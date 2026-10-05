#include "process_audio_capture.h"

#include <audioclientactivationparams.h>
#include <functiondiscoverykeys_devpkey.h>
#include <propvarutil.h>

#include <chrono>
#include <cstring>
#include <sstream>

namespace {

constexpr DWORD ACTIVATION_TIMEOUT_MS = 10'000;

std::int64_t now_micros() {
    return std::chrono::duration_cast<std::chrono::microseconds>(
        std::chrono::steady_clock::now().time_since_epoch()
    ).count();
}

std::string hr_message(HRESULT hr, const char* prefix) {
    std::ostringstream out;
    out << prefix << " (HRESULT 0x" << std::hex
        << static_cast<unsigned long>(hr) << ")";
    return out.str();
}

} // namespace

ProcessAudioCapture::~ProcessAudioCapture() {
    stop();
}

bool ProcessAudioCapture::start(
    DWORD process_id,
    AudioCallback callback,
    std::string& error
) {
    std::scoped_lock lock(mutex_);
    stop();

    callback_ = std::move(callback);

    activation_event_ = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    sample_event_ = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    stop_event_ = CreateEventW(nullptr, TRUE, FALSE, nullptr);

    if (!activation_event_ || !sample_event_ || !stop_event_) {
        error = "Unable to create process-audio capture events.";
        stop();
        return false;
    }

    AUDIOCLIENT_ACTIVATION_PARAMS params{};
    params.ActivationType = AUDIOCLIENT_ACTIVATION_TYPE_PROCESS_LOOPBACK;
    params.ProcessLoopbackParams.TargetProcessId = process_id;
    params.ProcessLoopbackParams.ProcessLoopbackMode =
        PROCESS_LOOPBACK_MODE_INCLUDE_TARGET_PROCESS_TREE;

    PROPVARIANT activate_params{};
    activate_params.vt = VT_BLOB;
    activate_params.blob.cbSize = sizeof(params);
    activate_params.blob.pBlobData = reinterpret_cast<BYTE*>(&params);

    activation_result_ = E_UNEXPECTED;
    activation_error_.clear();

    HRESULT hr = ActivateAudioInterfaceAsync(
        VIRTUAL_AUDIO_DEVICE_PROCESS_LOOPBACK,
        __uuidof(IAudioClient),
        &activate_params,
        this,
        activation_.ReleaseAndGetAddressOf()
    );

    if (FAILED(hr)) {
        error = hr_message(hr, "ActivateAudioInterfaceAsync failed");
        stop();
        return false;
    }

    DWORD wait = WaitForSingleObject(activation_event_, ACTIVATION_TIMEOUT_MS);
    if (wait != WAIT_OBJECT_0) {
        error = "Timed out while activating process-loopback audio.";
        stop();
        return false;
    }

    if (FAILED(activation_result_)) {
        error = activation_error_.empty()
            ? hr_message(activation_result_, "Process-loopback activation failed")
            : activation_error_;
        stop();
        return false;
    }

    hr = audio_client_->Start();
    if (FAILED(hr)) {
        error = hr_message(hr, "Unable to start process-loopback audio");
        stop();
        return false;
    }

    running_.store(true);
    worker_ = std::thread([this] { worker_loop(); });
    return true;
}

HRESULT ProcessAudioCapture::ActivateCompleted(
    IActivateAudioInterfaceAsyncOperation* operation
) {
    HRESULT activate_hr = E_UNEXPECTED;
    Microsoft::WRL::ComPtr<IUnknown> unknown;

    HRESULT hr = operation->GetActivateResult(
        &activate_hr,
        unknown.ReleaseAndGetAddressOf()
    );

    if (SUCCEEDED(hr)) {
        hr = activate_hr;
    }

    if (SUCCEEDED(hr)) {
        hr = unknown.As(&audio_client_);
    }

    if (SUCCEEDED(hr)) {
        format_.wFormatTag = WAVE_FORMAT_PCM;
        format_.nChannels = 2;
        format_.nSamplesPerSec = 44100;
        format_.wBitsPerSample = 16;
        format_.nBlockAlign =
            format_.nChannels * format_.wBitsPerSample / 8;
        format_.nAvgBytesPerSec =
            format_.nSamplesPerSec * format_.nBlockAlign;
        format_.cbSize = 0;

        hr = audio_client_->Initialize(
            AUDCLNT_SHAREMODE_SHARED,
            AUDCLNT_STREAMFLAGS_LOOPBACK
                | AUDCLNT_STREAMFLAGS_EVENTCALLBACK
                | AUDCLNT_STREAMFLAGS_AUTOCONVERTPCM,
            0,
            0,
            &format_,
            nullptr
        );
    }

    if (SUCCEEDED(hr)) {
        hr = audio_client_->GetService(
            __uuidof(IAudioCaptureClient),
            reinterpret_cast<void**>(
                capture_client_.ReleaseAndGetAddressOf()
            )
        );
    }

    if (SUCCEEDED(hr)) {
        hr = audio_client_->SetEventHandle(sample_event_);
    }

    activation_result_ = hr;
    if (FAILED(hr)) {
        activation_error_ = hr_message(
            hr,
            "Unable to initialize process-loopback audio"
        );
    }

    SetEvent(activation_event_);
    return S_OK;
}

void ProcessAudioCapture::worker_loop() {
    HANDLE events[2] = {stop_event_, sample_event_};

    while (running_.load()) {
        DWORD wait = WaitForMultipleObjects(
            2,
            events,
            FALSE,
            INFINITE
        );

        if (wait == WAIT_OBJECT_0) {
            break;
        }

        if (wait == WAIT_OBJECT_0 + 1) {
            drain_packets();
            continue;
        }

        break;
    }
}

void ProcessAudioCapture::drain_packets() {
    if (!capture_client_) {
        return;
    }

    while (running_.load()) {
        UINT32 frames = 0;
        HRESULT hr = capture_client_->GetNextPacketSize(&frames);
        if (FAILED(hr) || frames == 0) {
            return;
        }

        BYTE* data = nullptr;
        DWORD flags = 0;
        UINT64 device_position = 0;
        UINT64 qpc_position = 0;

        hr = capture_client_->GetBuffer(
            &data,
            &frames,
            &flags,
            &device_position,
            &qpc_position
        );

        if (FAILED(hr)) {
            return;
        }

        const std::size_t bytes =
            static_cast<std::size_t>(frames) * format_.nBlockAlign;

        std::vector<std::uint8_t> pcm(bytes);

        if ((flags & AUDCLNT_BUFFERFLAGS_SILENT) == 0 && data != nullptr) {
            std::memcpy(pcm.data(), data, bytes);
        }

        const std::int64_t timestamp = now_micros();
        AudioCallback callback = callback_;
        if (callback && !pcm.empty()) {
            callback(
                format_.nSamplesPerSec,
                format_.nChannels,
                timestamp,
                pcm
            );
        }

        capture_client_->ReleaseBuffer(frames);
    }
}

void ProcessAudioCapture::stop() {
    running_.store(false);

    if (stop_event_) {
        SetEvent(stop_event_);
    }

    if (worker_.joinable()
        && worker_.get_id() != std::this_thread::get_id()) {
        worker_.join();
    }

    if (audio_client_) {
        audio_client_->Stop();
    }

    capture_client_.Reset();
    audio_client_.Reset();
    activation_.Reset();
    callback_ = nullptr;

    if (activation_event_) {
        CloseHandle(activation_event_);
        activation_event_ = nullptr;
    }
    if (sample_event_) {
        CloseHandle(sample_event_);
        sample_event_ = nullptr;
    }
    if (stop_event_) {
        CloseHandle(stop_event_);
        stop_event_ = nullptr;
    }
}

bool ProcessAudioCapture::running() const noexcept {
    return running_.load();
}

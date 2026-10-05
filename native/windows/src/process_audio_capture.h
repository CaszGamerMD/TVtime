#pragma once

#include <windows.h>
#include <audioclient.h>
#include <mmdeviceapi.h>
#include <wrl.h>

#include <atomic>
#include <cstdint>
#include <functional>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

class ProcessAudioCapture final :
    public Microsoft::WRL::RuntimeClass<
        Microsoft::WRL::RuntimeClassFlags<Microsoft::WRL::ClassicCom>,
        Microsoft::WRL::FtmBase,
        IActivateAudioInterfaceCompletionHandler
    >
{
public:
    using AudioCallback = std::function<void(
        int sample_rate,
        int channels,
        std::int64_t timestamp_micros,
        const std::vector<std::uint8_t>& pcm_s16_le
    )>;

    ProcessAudioCapture() = default;
    ~ProcessAudioCapture();

    ProcessAudioCapture(const ProcessAudioCapture&) = delete;
    ProcessAudioCapture& operator=(const ProcessAudioCapture&) = delete;

    bool start(DWORD process_id, AudioCallback callback, std::string& error);
    void stop();
    bool running() const noexcept;

    STDMETHOD(ActivateCompleted)(
        IActivateAudioInterfaceAsyncOperation* operation
    ) override;

private:
    bool initialize_client(
        IActivateAudioInterfaceAsyncOperation* operation,
        std::string& error
    );
    void worker_loop();
    void drain_packets();

    std::mutex mutex_;
    std::atomic<bool> running_{false};
    AudioCallback callback_;

    Microsoft::WRL::ComPtr<IAudioClient> audio_client_;
    Microsoft::WRL::ComPtr<IAudioCaptureClient> capture_client_;
    Microsoft::WRL::ComPtr<IActivateAudioInterfaceAsyncOperation> activation_;

    HANDLE activation_event_{nullptr};
    HANDLE sample_event_{nullptr};
    HANDLE stop_event_{nullptr};

    HRESULT activation_result_{E_UNEXPECTED};
    std::string activation_error_;
    std::thread worker_;

    WAVEFORMATEX format_{};
};

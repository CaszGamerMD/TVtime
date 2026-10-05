#include "wgc_capture.h"

#include <dxgi1_2.h>
#include <windows.graphics.capture.interop.h>
#include <windows.graphics.directx.direct3d11.interop.h>

#include <winrt/base.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Graphics.Capture.h>
#include <winrt/Windows.Graphics.DirectX.h>
#include <winrt/Windows.Graphics.DirectX.Direct3D11.h>

#include <algorithm>
#include <chrono>
#include <cstring>
#include <sstream>

namespace {

constexpr int DEBUG_MAX_WIDTH = 128;
constexpr int DEBUG_MAX_HEIGHT = 72;
constexpr std::int64_t DEBUG_FRAME_INTERVAL_MICROS = 100'000; // 10 FPS

std::int64_t now_micros() {
    return std::chrono::duration_cast<std::chrono::microseconds>(
        std::chrono::steady_clock::now().time_since_epoch()
    ).count();
}

winrt::Windows::Graphics::DirectX::Direct3D11::IDirect3DDevice
create_direct3d_device(
    winrt::com_ptr<ID3D11Device>& device,
    winrt::com_ptr<ID3D11DeviceContext>& context
) {
    UINT flags = D3D11_CREATE_DEVICE_BGRA_SUPPORT;
    D3D_FEATURE_LEVEL feature_level{};

    winrt::check_hresult(D3D11CreateDevice(
        nullptr,
        D3D_DRIVER_TYPE_HARDWARE,
        nullptr,
        flags,
        nullptr,
        0,
        D3D11_SDK_VERSION,
        device.put(),
        &feature_level,
        context.put()
    ));

    auto dxgi_device = device.as<IDXGIDevice>();
    winrt::com_ptr<IInspectable> inspectable;

    winrt::check_hresult(CreateDirect3D11DeviceFromDXGIDevice(
        dxgi_device.get(),
        inspectable.put()
    ));

    return inspectable.as<
        winrt::Windows::Graphics::DirectX::Direct3D11::IDirect3DDevice
    >();
}

winrt::Windows::Graphics::Capture::GraphicsCaptureItem create_capture_item(HWND hwnd) {
    using namespace winrt::Windows::Graphics::Capture;

    auto interop = winrt::get_activation_factory<
        GraphicsCaptureItem,
        IGraphicsCaptureItemInterop
    >();

    GraphicsCaptureItem item{nullptr};
    winrt::check_hresult(interop->CreateForWindow(
        hwnd,
        winrt::guid_of<GraphicsCaptureItem>(),
        winrt::put_abi(item)
    ));

    return item;
}

std::string hresult_message(const winrt::hresult_error& error) {
    std::ostringstream out;
    out << "HRESULT 0x" << std::hex
        << static_cast<std::uint32_t>(error.code().value)
        << ": " << winrt::to_string(error.message());
    return out.str();
}

} // namespace

WgcCaptureSession::~WgcCaptureSession() {
    stop();
}

bool WgcCaptureSession::start(
    HWND hwnd,
    FrameCallback callback,
    std::string& error
) {
    std::scoped_lock lock(mutex_);
    stop();

    try {
        if (!IsWindow(hwnd)) {
            error = "The selected window handle is no longer valid.";
            return false;
        }

        frame_callback_ = std::move(callback);

        auto device = create_direct3d_device(d3d_device_, d3d_context_);
        item_ = create_capture_item(hwnd);

        const auto size = item_.Size();
        if (size.Width <= 0 || size.Height <= 0) {
            error = "The selected window has no capturable size.";
            item_ = nullptr;
            return false;
        }

        width_.store(size.Width);
        height_.store(size.Height);
        frame_count_.store(0);
        last_emit_micros_.store(0);

        using namespace winrt::Windows::Graphics::Capture;
        using namespace winrt::Windows::Graphics::DirectX;

        frame_pool_ = Direct3D11CaptureFramePool::CreateFreeThreaded(
            device,
            DirectXPixelFormat::B8G8R8A8UIntNormalized,
            2,
            size
        );

        frame_arrived_token_ = frame_pool_.FrameArrived(
            [this](
                Direct3D11CaptureFramePool const& sender,
                winrt::Windows::Foundation::IInspectable const&
            ) {
                try {
                    handle_frame(sender);
                } catch (...) {
                    // Capture teardown can race a pending callback.
                }
            }
        );
        frame_handler_registered_ = true;

        capture_session_ = frame_pool_.CreateCaptureSession(item_);
        capture_session_.StartCapture();
        running_.store(true);
        return true;
    } catch (const winrt::hresult_error& ex) {
        error = hresult_message(ex);
    } catch (const std::exception& ex) {
        error = ex.what();
    } catch (...) {
        error = "Unknown Windows Graphics Capture failure.";
    }

    stop();
    return false;
}

void WgcCaptureSession::handle_frame(
    winrt::Windows::Graphics::Capture::Direct3D11CaptureFramePool const& sender
) {
    auto frame = sender.TryGetNextFrame();
    if (!frame) return;

    const auto content_size = frame.ContentSize();
    width_.store(content_size.Width);
    height_.store(content_size.Height);
    frame_count_.fetch_add(1, std::memory_order_relaxed);

    const std::int64_t now = now_micros();
    const std::int64_t last = last_emit_micros_.load(std::memory_order_relaxed);
    if (now - last < DEBUG_FRAME_INTERVAL_MICROS) {
        return;
    }
    last_emit_micros_.store(now, std::memory_order_relaxed);

    auto access = frame.Surface().as<IDirect3DDxgiInterfaceAccess>();
    winrt::com_ptr<ID3D11Texture2D> source_texture;
    winrt::check_hresult(access->GetInterface(
        __uuidof(ID3D11Texture2D),
        source_texture.put_void()
    ));

    D3D11_TEXTURE2D_DESC desc{};
    source_texture->GetDesc(&desc);

    if (!staging_texture_
        || staging_width_ != desc.Width
        || staging_height_ != desc.Height) {
        D3D11_TEXTURE2D_DESC staging_desc = desc;
        staging_desc.BindFlags = 0;
        staging_desc.MiscFlags = 0;
        staging_desc.Usage = D3D11_USAGE_STAGING;
        staging_desc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;

        staging_texture_ = nullptr;
        winrt::check_hresult(d3d_device_->CreateTexture2D(
            &staging_desc,
            nullptr,
            staging_texture_.put()
        ));

        staging_width_ = desc.Width;
        staging_height_ = desc.Height;
    }

    d3d_context_->CopyResource(staging_texture_.get(), source_texture.get());

    D3D11_MAPPED_SUBRESOURCE mapped{};
    winrt::check_hresult(d3d_context_->Map(
        staging_texture_.get(),
        0,
        D3D11_MAP_READ,
        0,
        &mapped
    ));

    const int source_width = static_cast<int>(desc.Width);
    const int source_height = static_cast<int>(desc.Height);

    const double scale = std::min(
        1.0,
        std::min(
            static_cast<double>(DEBUG_MAX_WIDTH) / source_width,
            static_cast<double>(DEBUG_MAX_HEIGHT) / source_height
        )
    );

    const int output_width = std::max(1, static_cast<int>(source_width * scale));
    const int output_height = std::max(1, static_cast<int>(source_height * scale));

    std::vector<std::uint8_t> bgra(
        static_cast<std::size_t>(output_width)
        * static_cast<std::size_t>(output_height)
        * 4
    );

    const auto* base = static_cast<const std::uint8_t*>(mapped.pData);

    for (int y = 0; y < output_height; ++y) {
        const int source_y = std::min(
            source_height - 1,
            y * source_height / output_height
        );

        const auto* source_row =
            base + static_cast<std::size_t>(source_y) * mapped.RowPitch;

        for (int x = 0; x < output_width; ++x) {
            const int source_x = std::min(
                source_width - 1,
                x * source_width / output_width
            );

            const auto* source_pixel = source_row + source_x * 4;
            auto* output_pixel =
                bgra.data() + (static_cast<std::size_t>(y) * output_width + x) * 4;

            std::memcpy(output_pixel, source_pixel, 4);
        }
    }

    d3d_context_->Unmap(staging_texture_.get(), 0);

    FrameCallback callback = frame_callback_;
    if (callback) {
        callback(output_width, output_height, now, bgra);
    }
}

void WgcCaptureSession::stop() {
    running_.store(false);

    if (frame_handler_registered_ && frame_pool_) {
        frame_pool_.FrameArrived(frame_arrived_token_);
        frame_handler_registered_ = false;
    }

    if (capture_session_) {
        capture_session_.Close();
        capture_session_ = nullptr;
    }

    if (frame_pool_) {
        frame_pool_.Close();
        frame_pool_ = nullptr;
    }

    item_ = nullptr;
    staging_texture_ = nullptr;
    d3d_context_ = nullptr;
    d3d_device_ = nullptr;

    staging_width_ = 0;
    staging_height_ = 0;
    width_.store(0);
    height_.store(0);
    last_emit_micros_.store(0);
    frame_callback_ = nullptr;
}

bool WgcCaptureSession::running() const noexcept {
    return running_.load();
}

std::uint64_t WgcCaptureSession::frame_count() const noexcept {
    return frame_count_.load(std::memory_order_relaxed);
}

int WgcCaptureSession::width() const noexcept {
    return width_.load();
}

int WgcCaptureSession::height() const noexcept {
    return height_.load();
}

#include "wgc_capture.h"

#include <d3d11.h>
#include <dxgi1_2.h>
#include <windows.graphics.capture.interop.h>
#include <windows.graphics.directx.direct3d11.interop.h>

#include <winrt/base.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Graphics.Capture.h>
#include <winrt/Windows.Graphics.DirectX.h>
#include <winrt/Windows.Graphics.DirectX.Direct3D11.h>

#include <sstream>

namespace {

winrt::Windows::Graphics::DirectX::Direct3D11::IDirect3DDevice create_direct3d_device() {
    UINT flags = D3D11_CREATE_DEVICE_BGRA_SUPPORT;

    winrt::com_ptr<ID3D11Device> device;
    winrt::com_ptr<ID3D11DeviceContext> context;
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

bool WgcCaptureSession::start(HWND hwnd, std::string& error) {
    std::scoped_lock lock(mutex_);
    stop();

    try {
        if (!IsWindow(hwnd)) {
            error = "The selected window handle is no longer valid.";
            return false;
        }

        auto device = create_direct3d_device();
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

        using namespace winrt::Windows::Graphics::Capture;
        using namespace winrt::Windows::Graphics::DirectX;

        frame_pool_ = Direct3D11CaptureFramePool::CreateFreeThreaded(
            device,
            DirectXPixelFormat::B8G8R8A8UIntNormalized,
            2,
            size
        );

        frame_arrived_token_ = frame_pool_.FrameArrived(
            [this](Direct3D11CaptureFramePool const& sender, winrt::Windows::Foundation::IInspectable const&) {
                try {
                    auto frame = sender.TryGetNextFrame();
                    if (!frame) {
                        return;
                    }

                    const auto content_size = frame.ContentSize();
                    width_.store(content_size.Width);
                    height_.store(content_size.Height);
                    frame_count_.fetch_add(1, std::memory_order_relaxed);
                } catch (...) {
                    // Capture teardown can race a pending frame callback. The
                    // owning thread handles session lifetime and reports status.
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

    if (frame_handler_registered_ && frame_pool_) {
        frame_pool_.FrameArrived(frame_arrived_token_);
        frame_handler_registered_ = false;
    }

    capture_session_ = nullptr;
    if (frame_pool_) {
        frame_pool_.Close();
        frame_pool_ = nullptr;
    }
    item_ = nullptr;
    running_.store(false);
    return false;
}

void WgcCaptureSession::stop() {
    // Callers currently serialize start/stop from the command loop.
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
    width_.store(0);
    height_.store(0);
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

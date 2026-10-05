#pragma once

#include <windows.h>
#include <d3d11.h>

#include <atomic>
#include <cstdint>
#include <functional>
#include <mutex>
#include <string>
#include <vector>

#include <winrt/base.h>
#include <winrt/Windows.Graphics.Capture.h>

class WgcCaptureSession {
public:
    using FrameCallback = std::function<void(
        int width,
        int height,
        std::int64_t timestamp_micros,
        const std::vector<std::uint8_t>& bgra
    )>;

    WgcCaptureSession() = default;
    ~WgcCaptureSession();

    WgcCaptureSession(const WgcCaptureSession&) = delete;
    WgcCaptureSession& operator=(const WgcCaptureSession&) = delete;

    bool start(
        HWND hwnd,
        int max_width,
        int max_height,
        int max_fps,
        FrameCallback callback,
        std::string& error
    );
    void stop();

    bool running() const noexcept;
    std::uint64_t frame_count() const noexcept;
    int width() const noexcept;
    int height() const noexcept;

private:
    void handle_frame(
        winrt::Windows::Graphics::Capture::Direct3D11CaptureFramePool const& sender
    );

    std::mutex mutex_;
    std::atomic<bool> running_{false};
    std::atomic<std::uint64_t> frame_count_{0};
    std::atomic<int> width_{0};
    std::atomic<int> height_{0};
    std::atomic<std::int64_t> last_emit_micros_{0};
    std::atomic<int> max_width_{128};
    std::atomic<int> max_height_{72};
    std::atomic<std::int64_t> frame_interval_micros_{100'000};

    FrameCallback frame_callback_;

    winrt::com_ptr<ID3D11Device> d3d_device_;
    winrt::com_ptr<ID3D11DeviceContext> d3d_context_;
    winrt::com_ptr<ID3D11Texture2D> staging_texture_;
    UINT staging_width_{0};
    UINT staging_height_{0};

    winrt::Windows::Graphics::Capture::GraphicsCaptureItem item_{nullptr};
    winrt::Windows::Graphics::Capture::Direct3D11CaptureFramePool frame_pool_{nullptr};
    winrt::Windows::Graphics::Capture::GraphicsCaptureSession capture_session_{nullptr};
    winrt::event_token frame_arrived_token_{};
    bool frame_handler_registered_{false};
};

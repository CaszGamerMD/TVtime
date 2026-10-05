#pragma once

#include <windows.h>

#include <atomic>
#include <cstdint>
#include <mutex>

#include <winrt/Windows.Graphics.Capture.h>

class WgcCaptureSession {
public:
    WgcCaptureSession() = default;
    ~WgcCaptureSession();

    WgcCaptureSession(const WgcCaptureSession&) = delete;
    WgcCaptureSession& operator=(const WgcCaptureSession&) = delete;

    bool start(HWND hwnd, std::string& error);
    void stop();

    bool running() const noexcept;
    std::uint64_t frame_count() const noexcept;
    int width() const noexcept;
    int height() const noexcept;

private:
    std::mutex mutex_;
    std::atomic<bool> running_{false};
    std::atomic<std::uint64_t> frame_count_{0};
    std::atomic<int> width_{0};
    std::atomic<int> height_{0};

    winrt::Windows::Graphics::Capture::GraphicsCaptureItem item_{nullptr};
    winrt::Windows::Graphics::Capture::Direct3D11CaptureFramePool frame_pool_{nullptr};
    winrt::Windows::Graphics::Capture::GraphicsCaptureSession capture_session_{nullptr};
    winrt::event_token frame_arrived_token_{};
    bool frame_handler_registered_{false};
};

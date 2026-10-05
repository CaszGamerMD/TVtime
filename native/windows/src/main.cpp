#include <windows.h>
#include <winrt/base.h>
#include <wrl.h>
#include "wgc_capture.h"
#include "process_audio_capture.h"

#include <algorithm>
#include <array>
#include <cstdint>
#include <cstring>
#include <iostream>
#include <sstream>
#include <string>
#include <string_view>
#include <vector>
#include <mutex>
#include <charconv>

namespace {

std::mutex media_output_mutex;

void write_u32_le(std::uint8_t* target, std::uint32_t value) {
    target[0] = static_cast<std::uint8_t>(value & 0xff);
    target[1] = static_cast<std::uint8_t>((value >> 8) & 0xff);
    target[2] = static_cast<std::uint8_t>((value >> 16) & 0xff);
    target[3] = static_cast<std::uint8_t>((value >> 24) & 0xff);
}

void write_u64_le(std::uint8_t* target, std::uint64_t value) {
    for (int i = 0; i < 8; ++i) {
        target[i] = static_cast<std::uint8_t>((value >> (i * 8)) & 0xff);
    }
}

void write_all(HANDLE handle, const void* data, DWORD bytes) {
    const auto* cursor = static_cast<const std::uint8_t*>(data);
    DWORD remaining = bytes;

    while (remaining > 0) {
        DWORD written = 0;
        if (!WriteFile(handle, cursor, remaining, &written, nullptr) || written == 0) {
            return;
        }
        cursor += written;
        remaining -= written;
    }
}

void write_video_frame(
    int width,
    int height,
    std::int64_t timestamp_micros,
    const std::vector<std::uint8_t>& bgra
) {
    if (bgra.empty()) return;

    std::scoped_lock lock(media_output_mutex);
    HANDLE output = GetStdHandle(STD_ERROR_HANDLE);
    if (output == nullptr || output == INVALID_HANDLE_VALUE) return;

    std::array<std::uint8_t, 28> header{};
    header[0] = 'T';
    header[1] = 'V';
    header[2] = 'F';
    header[3] = '1';
    header[4] = 1; // video
    header[5] = 0; // BGRA8
    write_u32_le(header.data() + 8, static_cast<std::uint32_t>(bgra.size()));
    write_u64_le(
        header.data() + 12,
        static_cast<std::uint64_t>(timestamp_micros)
    );
    write_u32_le(header.data() + 20, static_cast<std::uint32_t>(width));
    write_u32_le(header.data() + 24, static_cast<std::uint32_t>(height));

    write_all(output, header.data(), static_cast<DWORD>(header.size()));
    write_all(output, bgra.data(), static_cast<DWORD>(bgra.size()));
}

void write_audio_chunk(
    int sample_rate,
    int channels,
    std::int64_t timestamp_micros,
    const std::vector<std::uint8_t>& pcm
) {
    if (pcm.empty()) return;

    std::scoped_lock lock(media_output_mutex);
    HANDLE output = GetStdHandle(STD_ERROR_HANDLE);
    if (output == nullptr || output == INVALID_HANDLE_VALUE) return;

    std::array<std::uint8_t, 28> header{};
    header[0] = 'T';
    header[1] = 'V';
    header[2] = 'F';
    header[3] = '1';
    header[4] = 2; // audio
    header[5] = 0; // S16_LE
    write_u32_le(header.data() + 8, static_cast<std::uint32_t>(pcm.size()));
    write_u64_le(
        header.data() + 12,
        static_cast<std::uint64_t>(timestamp_micros)
    );
    write_u32_le(header.data() + 20, static_cast<std::uint32_t>(sample_rate));
    write_u32_le(header.data() + 24, static_cast<std::uint32_t>(channels));

    write_all(output, header.data(), static_cast<DWORD>(header.size()));
    write_all(output, pcm.data(), static_cast<DWORD>(pcm.size()));
}

struct WindowInfo {
    std::uint64_t handle{};
    DWORD process_id{};
    std::wstring process_name;
    std::wstring title;
};

std::string utf8(const std::wstring& value) {
    if (value.empty()) return {};

    const int required = WideCharToMultiByte(
        CP_UTF8, WC_ERR_INVALID_CHARS, value.data(),
        static_cast<int>(value.size()), nullptr, 0, nullptr, nullptr
    );
    if (required <= 0) return {};

    std::string output(static_cast<std::size_t>(required), '\0');
    WideCharToMultiByte(
        CP_UTF8, WC_ERR_INVALID_CHARS, value.data(),
        static_cast<int>(value.size()), output.data(),
        required, nullptr, nullptr
    );
    return output;
}

std::string json_escape(std::string_view value) {
    std::ostringstream out;
    for (unsigned char ch : value) {
        switch (ch) {
            case '"': out << "\\\""; break;
            case '\\': out << "\\\\"; break;
            case '\b': out << "\\b"; break;
            case '\f': out << "\\f"; break;
            case '\n': out << "\\n"; break;
            case '\r': out << "\\r"; break;
            case '\t': out << "\\t"; break;
            default:
                if (ch < 0x20) {
                    constexpr char hex[] = "0123456789abcdef";
                    out << "\\u00" << hex[(ch >> 4) & 0x0f] << hex[ch & 0x0f];
                } else {
                    out << static_cast<char>(ch);
                }
        }
    }
    return out.str();
}

std::wstring window_title(HWND hwnd) {
    const int length = GetWindowTextLengthW(hwnd);
    if (length <= 0) return {};

    std::wstring title(static_cast<std::size_t>(length + 1), L'\0');
    const int copied = GetWindowTextW(hwnd, title.data(), length + 1);
    if (copied <= 0) return {};

    title.resize(static_cast<std::size_t>(copied));
    return title;
}

std::wstring process_name(DWORD process_id) {
    HANDLE process = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, FALSE, process_id);
    if (process == nullptr) return {};

    std::wstring buffer(32768, L'\0');
    DWORD size = static_cast<DWORD>(buffer.size());

    if (!QueryFullProcessImageNameW(process, 0, buffer.data(), &size)) {
        CloseHandle(process);
        return {};
    }

    CloseHandle(process);
    buffer.resize(size);

    const std::size_t slash = buffer.find_last_of(L"\\/");
    if (slash != std::wstring::npos) buffer.erase(0, slash + 1);
    return buffer;
}

BOOL CALLBACK enumerate_window(HWND hwnd, LPARAM param) {
    auto* windows = reinterpret_cast<std::vector<WindowInfo>*>(param);

    if (!IsWindowVisible(hwnd)) return TRUE;
    if (GetWindow(hwnd, GW_OWNER) != nullptr) return TRUE;

    const std::wstring title = window_title(hwnd);
    if (title.empty()) return TRUE;

    RECT rect{};
    if (!GetWindowRect(hwnd, &rect)) return TRUE;
    if (rect.right <= rect.left || rect.bottom <= rect.top) return TRUE;

    DWORD process_id = 0;
    GetWindowThreadProcessId(hwnd, &process_id);

    windows->push_back(WindowInfo{
        reinterpret_cast<std::uint64_t>(hwnd),
        process_id,
        process_name(process_id),
        title
    });

    return TRUE;
}

std::vector<WindowInfo> list_windows() {
    std::vector<WindowInfo> windows;
    EnumWindows(enumerate_window, reinterpret_cast<LPARAM>(&windows));
    std::sort(windows.begin(), windows.end(), [](const WindowInfo& a, const WindowInfo& b) {
        return a.title < b.title;
    });
    return windows;
}

void write_hello() {
    std::cout
        << "{\"type\":\"hello\",\"protocol\":1,"
        << "\"helper\":\"tvtime-capture\","
        << "\"captureBackend\":\"windows-graphics-capture\"}"
        << std::endl;
}

void write_windows() {
    const auto windows = list_windows();
    std::cout << "{\"type\":\"windows\",\"windows\":[";

    bool first = true;
    for (const auto& window : windows) {
        if (!first) std::cout << ',';
        first = false;

        std::cout
            << "{\"handle\":" << window.handle
            << ",\"processId\":" << window.process_id
            << ",\"processName\":\"" << json_escape(utf8(window.process_name)) << "\""
            << ",\"title\":\"" << json_escape(utf8(window.title)) << "\"}";
    }

    std::cout << "]}" << std::endl;
}

void write_error(std::string_view code, std::string_view message) {
    std::cout
        << "{\"type\":\"error\",\"code\":\"" << json_escape(code)
        << "\",\"message\":\"" << json_escape(message) << "\"}"
        << std::endl;
}

} // namespace

int main() {
    winrt::init_apartment(winrt::apartment_type::multi_threaded);
    SetConsoleOutputCP(CP_UTF8);

    WgcCaptureSession capture;
    auto audio_capture =
        Microsoft::WRL::Make<ProcessAudioCapture>();
    write_hello();

    std::string line;
    while (std::getline(std::cin, line)) {
        if (line == "PING") {
            std::cout << "{\"type\":\"pong\"}" << std::endl;
        } else if (line == "LIST_WINDOWS") {
            write_windows();
        } else if (line == "QUIT") {
            audio_capture->stop();
            capture.stop();
            return 0;
        } else if (line.rfind("START ", 0) == 0) {
            std::istringstream args(line.substr(6));

            std::uint64_t handle_value = 0;
            int max_width = 0;
            int max_height = 0;
            int max_fps = 0;
            int audio_flag = 0;

            if (!(args
                >> handle_value
                >> max_width
                >> max_height
                >> max_fps
                >> audio_flag)) {
                write_error(
                    "invalid_start",
                    "START requires: handle width height fps audioFlag."
                );
                continue;
            }

            std::string trailing;
            if (args >> trailing) {
                write_error(
                    "invalid_start",
                    "START received unexpected trailing arguments."
                );
                continue;
            }

            std::string error;
            const auto hwnd = reinterpret_cast<HWND>(
                static_cast<std::uintptr_t>(handle_value)
            );

            if (!capture.start(
                hwnd,
                max_width,
                max_height,
                max_fps,
                write_video_frame,
                error
            )) {
                write_error("capture_start_failed", error);
                continue;
            }

            DWORD process_id = 0;
            GetWindowThreadProcessId(hwnd, &process_id);

            std::string audio_error;
            const bool audio_requested = audio_flag != 0;
            const bool audio_started =
                audio_requested
                && process_id != 0
                && audio_capture->start(
                    process_id,
                    write_audio_chunk,
                    audio_error
                );

            std::cout
                << "{\"type\":\"capture_started\","
                << "\"handle\":" << handle_value << ","
                << "\"processId\":" << process_id << ","
                << "\"width\":" << capture.width() << ","
                << "\"height\":" << capture.height() << ","
                << "\"maxFps\":" << max_fps << ","
                << "\"audioRequested\":" << (audio_requested ? "true" : "false") << ","
                << "\"audio\":" << (audio_started ? "true" : "false");

            if (!audio_started && !audio_error.empty()) {
                std::cout
                    << ",\"audioError\":\""
                    << json_escape(audio_error)
                    << "\"";
            }

            std::cout << "}" << std::endl;
        } else if (line == "STOP") {
            audio_capture->stop();
            capture.stop();
            std::cout << "{\"type\":\"capture_stopped\"}" << std::endl;
        } else if (line == "STATUS") {
            std::cout
                << "{\"type\":\"status\","
                << "\"running\":" << (capture.running() ? "true" : "false") << ","
                << "\"frames\":" << capture.frame_count() << ","
                << "\"width\":" << capture.width() << ","
                << "\"height\":" << capture.height() << ","
                << "\"audio\":" << (audio_capture->running() ? "true" : "false")
                << "}" << std::endl;
        } else {
            write_error("unknown_command", "Unknown TVtime capture-helper command.");
        }
    }

    return 0;
}

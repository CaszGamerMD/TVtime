#include <windows.h>
#include <winrt/base.h>
#include "wgc_capture.h"

#include <algorithm>
#include <cstdint>
#include <iostream>
#include <sstream>
#include <string>
#include <string_view>
#include <vector>
#include <charconv>

namespace {

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
    write_hello();

    std::string line;
    while (std::getline(std::cin, line)) {
        if (line == "PING") {
            std::cout << "{\"type\":\"pong\"}" << std::endl;
        } else if (line == "LIST_WINDOWS") {
            write_windows();
        } else if (line == "QUIT") {
            capture.stop();
            return 0;
        } else if (line.rfind("START ", 0) == 0) {
            std::uint64_t handle_value = 0;
            const std::string_view text(line.data() + 6, line.size() - 6);
            const auto parsed = std::from_chars(
                text.data(),
                text.data() + text.size(),
                handle_value
            );

            if (parsed.ec != std::errc{} || parsed.ptr != text.data() + text.size()) {
                write_error("invalid_handle", "START requires a numeric native window handle.");
                continue;
            }

            std::string error;
            const auto hwnd = reinterpret_cast<HWND>(
                static_cast<std::uintptr_t>(handle_value)
            );

            if (!capture.start(hwnd, error)) {
                write_error("capture_start_failed", error);
                continue;
            }

            std::cout
                << "{\"type\":\"capture_started\","
                << "\"handle\":" << handle_value << ","
                << "\"width\":" << capture.width() << ","
                << "\"height\":" << capture.height()
                << "}" << std::endl;
        } else if (line == "STOP") {
            capture.stop();
            std::cout << "{\"type\":\"capture_stopped\"}" << std::endl;
        } else if (line == "STATUS") {
            std::cout
                << "{\"type\":\"status\","
                << "\"running\":" << (capture.running() ? "true" : "false") << ","
                << "\"frames\":" << capture.frame_count() << ","
                << "\"width\":" << capture.width() << ","
                << "\"height\":" << capture.height()
                << "}" << std::endl;
        } else {
            write_error("unknown_command", "Unknown TVtime capture-helper command.");
        }
    }

    return 0;
}

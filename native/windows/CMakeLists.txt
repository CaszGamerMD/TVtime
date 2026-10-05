cmake_minimum_required(VERSION 3.25)

project(tvtime_capture LANGUAGES CXX)

add_executable(tvtime-capture
    src/main.cpp
    src/wgc_capture.cpp
    src/process_audio_capture.cpp
)

target_compile_features(tvtime-capture PRIVATE cxx_std_20)
target_compile_definitions(tvtime-capture PRIVATE
    UNICODE
    _UNICODE
    WIN32_LEAN_AND_MEAN
    NOMINMAX
)

target_link_libraries(tvtime-capture PRIVATE
    user32
    kernel32
    d3d11
    dxgi
    windowsapp
    mmdevapi
    ole32
)

if(MSVC)
    target_compile_options(tvtime-capture PRIVATE /W4 /permissive- /EHsc)
endif()

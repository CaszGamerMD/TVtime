cmake_minimum_required(VERSION 3.25)

project(caszual_tv_time_capture LANGUAGES CXX)

add_executable(caszual-tv-time-capture
    src/main.cpp
    src/wgc_capture.cpp
    src/process_audio_capture.cpp
)

target_compile_features(caszual-tv-time-capture PRIVATE cxx_std_20)
target_compile_definitions(caszual-tv-time-capture PRIVATE
    UNICODE
    _UNICODE
    WIN32_LEAN_AND_MEAN
    NOMINMAX
)

target_link_libraries(caszual-tv-time-capture PRIVATE
    user32
    kernel32
    d3d11
    dxgi
    windowsapp
    mmdevapi
    ole32
)

if(MSVC)
    target_compile_options(caszual-tv-time-capture PRIVATE /W4 /permissive- /EHsc)
endif()

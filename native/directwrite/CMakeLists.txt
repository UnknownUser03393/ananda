cmake_minimum_required(VERSION 3.20)
project(directwritejni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(JNI REQUIRED)

add_library(directwritejni SHARED directwrite_jni.cpp)
target_include_directories(directwritejni PRIVATE ${JNI_INCLUDE_DIRS})
target_link_libraries(directwritejni PRIVATE d2d1 dwrite windowscodecs ole32)

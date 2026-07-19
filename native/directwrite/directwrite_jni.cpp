#include <jni.h>
#include <windows.h>
#include <d2d1.h>
#include <dwrite.h>
#include <wincodec.h>
#include <wrl/client.h>

#include <cstdint>
#include <string>
#include <vector>

using Microsoft::WRL::ComPtr;

namespace {
class ComScope {
public:
    ComScope() : initialized_(SUCCEEDED(CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED))) {}
    ~ComScope() {
        if (initialized_) {
            CoUninitialize();
        }
    }

private:
    bool initialized_;
};

std::wstring toWide(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return L"";
    }
    const jchar* chars = env->GetStringChars(value, nullptr);
    const jsize length = env->GetStringLength(value);
    std::wstring result(reinterpret_cast<const wchar_t*>(chars), static_cast<size_t>(length));
    env->ReleaseStringChars(value, chars);
    return result;
}

float channel(int argb, int shift) {
    return static_cast<float>((argb >> shift) & 0xff) / 255.0f;
}

D2D1_COLOR_F toD2DColor(int argb) {
    return D2D1::ColorF(channel(argb, 16), channel(argb, 8), channel(argb, 0), channel(argb, 24));
}

void throwJava(JNIEnv* env, const char* message, HRESULT hr = S_OK) {
    char buffer[512];
    if (FAILED(hr)) {
        sprintf_s(buffer, "%s HRESULT=0x%08X", message, static_cast<unsigned int>(hr));
    } else {
        sprintf_s(buffer, "%s", message);
    }
    jclass exceptionClass = env->FindClass("java/lang/IllegalStateException");
    env->ThrowNew(exceptionClass, buffer);
}
}

extern "C" JNIEXPORT jobject JNICALL
Java_dev_unknownuser_ananda_text_directwrite_DirectWriteNative_renderText(
    JNIEnv* env,
    jclass,
    jstring jText,
    jstring jFontFamily,
    jfloat fontSize,
    jint textArgb,
    jint backgroundArgb,
    jint width,
    jint height
) {
    if (width <= 0 || height <= 0) {
        throwJava(env, "DirectWrite render target size must be positive");
        return nullptr;
    }

    const ComScope com;
    const std::wstring text = toWide(env, jText);
    const std::wstring fontFamily = toWide(env, jFontFamily).empty() ? L"Segoe UI" : toWide(env, jFontFamily);

    ComPtr<IWICImagingFactory> wicFactory;
    HRESULT hr = CoCreateInstance(
        CLSID_WICImagingFactory,
        nullptr,
        CLSCTX_INPROC_SERVER,
        IID_PPV_ARGS(&wicFactory)
    );
    if (FAILED(hr)) {
        throwJava(env, "Failed to create WIC factory", hr);
        return nullptr;
    }

    ComPtr<ID2D1Factory> d2dFactory;
    hr = D2D1CreateFactory(
        D2D1_FACTORY_TYPE_SINGLE_THREADED,
        __uuidof(ID2D1Factory),
        nullptr,
        reinterpret_cast<void**>(d2dFactory.GetAddressOf())
    );
    if (FAILED(hr)) {
        throwJava(env, "Failed to create Direct2D factory", hr);
        return nullptr;
    }

    ComPtr<IDWriteFactory> dwriteFactory;
    hr = DWriteCreateFactory(
        DWRITE_FACTORY_TYPE_SHARED,
        __uuidof(IDWriteFactory),
        reinterpret_cast<IUnknown**>(dwriteFactory.GetAddressOf())
    );
    if (FAILED(hr)) {
        throwJava(env, "Failed to create DirectWrite factory", hr);
        return nullptr;
    }

    ComPtr<IWICBitmap> bitmap;
    hr = wicFactory->CreateBitmap(
        static_cast<UINT>(width),
        static_cast<UINT>(height),
        GUID_WICPixelFormat32bppPBGRA,
        WICBitmapCacheOnLoad,
        &bitmap
    );
    if (FAILED(hr)) {
        throwJava(env, "Failed to create WIC bitmap", hr);
        return nullptr;
    }

    const D2D1_RENDER_TARGET_PROPERTIES properties = D2D1::RenderTargetProperties(
        D2D1_RENDER_TARGET_TYPE_DEFAULT,
        D2D1::PixelFormat(DXGI_FORMAT_B8G8R8A8_UNORM, D2D1_ALPHA_MODE_PREMULTIPLIED),
        96.0f,
        96.0f
    );

    ComPtr<ID2D1RenderTarget> renderTarget;
    hr = d2dFactory->CreateWicBitmapRenderTarget(bitmap.Get(), properties, &renderTarget);
    if (FAILED(hr)) {
        throwJava(env, "Failed to create Direct2D WIC render target", hr);
        return nullptr;
    }

    renderTarget->SetTextAntialiasMode(D2D1_TEXT_ANTIALIAS_MODE_CLEARTYPE);
    renderTarget->SetAntialiasMode(D2D1_ANTIALIAS_MODE_PER_PRIMITIVE);

    ComPtr<IDWriteTextFormat> textFormat;
    hr = dwriteFactory->CreateTextFormat(
        fontFamily.c_str(),
        nullptr,
        DWRITE_FONT_WEIGHT_NORMAL,
        DWRITE_FONT_STYLE_NORMAL,
        DWRITE_FONT_STRETCH_NORMAL,
        fontSize,
        L"",
        &textFormat
    );
    if (FAILED(hr)) {
        throwJava(env, "Failed to create DirectWrite text format", hr);
        return nullptr;
    }
    textFormat->SetTextAlignment(DWRITE_TEXT_ALIGNMENT_LEADING);
    textFormat->SetParagraphAlignment(DWRITE_PARAGRAPH_ALIGNMENT_NEAR);

    ComPtr<ID2D1SolidColorBrush> textBrush;
    hr = renderTarget->CreateSolidColorBrush(toD2DColor(textArgb), &textBrush);
    if (FAILED(hr)) {
        throwJava(env, "Failed to create Direct2D text brush", hr);
        return nullptr;
    }

    renderTarget->BeginDraw();
    renderTarget->Clear(toD2DColor(backgroundArgb));
    renderTarget->DrawText(
        text.c_str(),
        static_cast<UINT32>(text.size()),
        textFormat.Get(),
        D2D1::RectF(0.0f, 0.0f, static_cast<float>(width), static_cast<float>(height)),
        textBrush.Get(),
        D2D1_DRAW_TEXT_OPTIONS_CLIP
    );
    hr = renderTarget->EndDraw();
    if (FAILED(hr)) {
        throwJava(env, "DirectWrite rendering failed", hr);
        return nullptr;
    }

    WICRect lockRect{0, 0, width, height};
    ComPtr<IWICBitmapLock> lock;
    hr = bitmap->Lock(&lockRect, WICBitmapLockRead, &lock);
    if (FAILED(hr)) {
        throwJava(env, "Failed to lock DirectWrite bitmap", hr);
        return nullptr;
    }

    UINT stride = 0;
    hr = lock->GetStride(&stride);
    if (FAILED(hr)) {
        throwJava(env, "Failed to read DirectWrite bitmap stride", hr);
        return nullptr;
    }

    UINT bufferSize = 0;
    BYTE* source = nullptr;
    hr = lock->GetDataPointer(&bufferSize, &source);
    if (FAILED(hr)) {
        throwJava(env, "Failed to read DirectWrite bitmap pixels", hr);
        return nullptr;
    }

    const int rowBytes = width * 4;
    std::vector<jbyte> pixels(static_cast<size_t>(rowBytes) * static_cast<size_t>(height));
    for (int y = 0; y < height; ++y) {
        memcpy(
            pixels.data() + static_cast<size_t>(y) * static_cast<size_t>(rowBytes),
            source + static_cast<size_t>(y) * static_cast<size_t>(stride),
            static_cast<size_t>(rowBytes)
        );
    }

    jbyteArray pixelArray = env->NewByteArray(static_cast<jsize>(pixels.size()));
    if (pixelArray == nullptr) {
        return nullptr;
    }
    env->SetByteArrayRegion(pixelArray, 0, static_cast<jsize>(pixels.size()), pixels.data());

    jclass rasterClass = env->FindClass("dev/unknownuser/ananda/text/directwrite/DirectWriteRaster");
    if (rasterClass == nullptr) {
        return nullptr;
    }
    jmethodID constructor = env->GetMethodID(rasterClass, "<init>", "([BIII)V");
    if (constructor == nullptr) {
        return nullptr;
    }
    return env->NewObject(rasterClass, constructor, pixelArray, width, height, rowBytes);
}

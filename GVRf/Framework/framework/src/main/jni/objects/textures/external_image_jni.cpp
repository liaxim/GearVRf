/***************************************************************************
 * JNI
 ***************************************************************************/

#include <engine/renderer/renderer.h>
#include "external_image.h"

#include "util/gvr_jni.h"

namespace gvr {
extern "C" {
JNIEXPORT jlong JNICALL
Java_org_gearvrf_NativeExternalRendererTexture_ctor(JNIEnv * env,
        jobject obj);
JNIEXPORT void JNICALL
Java_org_gearvrf_NativeExternalRendererTexture_setData(JNIEnv * env,
        jobject obj, jlong ptr, jlong data);
JNIEXPORT jlong JNICALL
Java_org_gearvrf_NativeExternalRendererTexture_getData(JNIEnv * env,
        jobject obj, jlong ptr);
}
;

JNIEXPORT jlong JNICALL
Java_org_gearvrf_NativeExternalRendererTexture_ctor(JNIEnv * env, jobject obj) {
    Texture* tex = Renderer::getInstance()->createTexture(Texture::TextureType::TEXTURE_EXTERNAL_RENDERER);
    std::shared_ptr<Texture>* result = new std::shared_ptr<Texture>(tex);
    return reinterpret_cast<jlong>(result);
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeExternalRendererTexture_setData(JNIEnv * env,
        jobject obj, jlong ptr, jlong data) {
    reinterpret_cast<ExternalImage*>(ptr)->setData(data);
}

JNIEXPORT jlong JNICALL
Java_org_gearvrf_NativeExternalRendererTexture_getData(JNIEnv * env,
        jobject obj, jlong ptr) {
    return reinterpret_cast<ExternalImage*>(ptr)->getData();
}

}

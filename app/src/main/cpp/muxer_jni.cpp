#include <jni.h>
#include <map>
#include <string>
#include "log.h"

#include "mux_stream.h"

using namespace std;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeInit(JNIEnv * env, jobject instance, jstring outputPath, jstring container) {
	const char *nativeString = env->GetStringUTFChars(outputPath, 0);
	const char *nativeString2 = env->GetStringUTFChars(container, 0);
	return reinterpret_cast<jlong>(init(nativeString, nativeString2));
}

JNIEXPORT void JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeStart(JNIEnv * env, jobject instance, jlong stream) {
    prepareStart((OutputStream *)stream);
}

JNIEXPORT void JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeStop(JNIEnv * env, jobject instance, jlong stream) {
    release((OutputStream *)stream);
}

JNIEXPORT void JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeWriteSampleData(JNIEnv * env, jobject instance, jlong stream, jint trackIndex, jobject byteBuffer, jint offset, jint size, jint flags, jlong presentationTimeUs) {
	jbyte* buf_in = (jbyte*)env->GetDirectBufferAddress(byteBuffer);
    writeFrame((OutputStream *)stream, trackIndex, buf_in, offset, size, flags, presentationTimeUs);
}

JNIEXPORT void JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeRegistVideoExtraData(JNIEnv * env, jobject instance, jlong stream, jint trackIndex, jbyteArray array, jint size) {
    jbyte* buf_in = (jbyte*)env->GetByteArrayElements(array, 0);
    jsize lengthOfArray = env->GetArrayLength( array);
    addPPSAndSPSBuffer((OutputStream *)stream, trackIndex, buf_in, size);
}


JNIEXPORT jint JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeAddTrack(JNIEnv * env, jobject instance, jlong stream, jobjectArray keys, jobjectArray values) {
	int index = -1;
    std::map<std::string, const char *> mymap;
    int stringCount = env->GetArrayLength(keys);
    for(int i = 0; i < stringCount; i++) {
    	jstring string_tmp = (jstring) (env->GetObjectArrayElement(keys, i));
    	const char *nativeStringKey = env->GetStringUTFChars(string_tmp, 0);
    	string_tmp = (jstring) (env->GetObjectArrayElement(values, i));
    	const char *nativeStringValue = env->GetStringUTFChars(string_tmp, 0);
    	std::string s(nativeStringKey);
    	mymap[s] = nativeStringValue;
    	LOGI("[key]-[Value] = [%s]-[%s]", nativeStringKey, nativeStringValue);

    }

    index = addTrack((OutputStream *)stream, mymap);
    return reinterpret_cast<jint>(index);
}
}

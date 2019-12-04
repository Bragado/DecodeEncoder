#include <jni.h>
#include <map>
#include <string>
#include "log.h"

#include "mux_stream.h"

using namespace std;

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeInit(JNIEnv * env, jobject instance, jstring outputPath) {
	const char *nativeString = env->GetStringUTFChars(outputPath, 0);
	init(nativeString);
}

JNIEXPORT void JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeStart(JNIEnv * env, jobject instance) {
    prepareStart(instance);
}

JNIEXPORT void JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeStop(JNIEnv * env, jobject instance) {
    release();
}

JNIEXPORT void JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeWriteSampleData(JNIEnv * env, jobject instance, jint trackIndex, jobject byteBuffer, jint offset, jint size, jint flags, jlong presentationTimeUs) {
	jbyte* buf_in = (jbyte*)env->GetDirectBufferAddress(byteBuffer);
	writeFrame(trackIndex, buf_in, offset, size, flags, presentationTimeUs);
}

JNIEXPORT jint JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeAddTrack(JNIEnv * env, jobject instance, jobjectArray keys,
        jobjectArray values) {
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

    index = addTrack(mymap);
    return static_cast<jint>(index);	
}
}

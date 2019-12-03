#include <jni.h>
#include <map>
#include<string>

#include "mux_stream.h"

extern "C" {
JNIEXPORT void JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeStart(JNIEnv *, jobject instance) {
    prepareStart(instance);
}

JNIEXPORT void JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeStop(JNIEnv *, jobject instance) {
    release(instance);
}

JNIEXPORT void JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeWriteSampleData(JNIEnv *, jobject instance, jint trackIndex, jobject byteBuffer, jint offset, jint size, jint flags, jlong presentationTimeUs) {
    writeFrame(trackIndex, byteBuffer, offset, size, flags, presentationTimeUs);
}

JNIEXPORT jint JNICALL
Java_com_example_decoderencoder_library_muxer_FFmpegMuxer_nativeAddTrack(JNIEnv *, jobject instance, jobjectArray keys,
        jobjectArray values) {
    std::map<std::string, std::string> mymap;
    int index = addTrack(mymap);
    return static_cast<jint>(index);	
}
}

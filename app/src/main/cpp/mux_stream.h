#ifndef ENCODERDECODER_MUX_STREAM_H
#define ENCODERDECODER_MUX_STREAM_H

#include <jni.h>
#include <map>
#include<string>


void prepareStart(jobject instance);

void release(jobject instance);

void writeFrame(jint trackIndex, jobject byteBuffer, jint offset, jint size, jint flags, jlong presentationTimeUs);

int addTrack(std::map<std::string, std::string> mymap);


#endif ENCODERDECODER_MUX_STREAM_H

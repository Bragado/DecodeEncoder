#ifndef ENCODERDECODER_MUX_STREAM_H
#define ENCODERDECODER_MUX_STREAM_H

#include <jni.h>
#include <map>
#include <string>

void init(const char * path);

void prepareStart(jobject instance);

void release();

void writeFrame(jint trackIndex, jbyte* framedata, jint offset, jint size, jint flags, jlong presentationTimeUs);

int addTrack(std::map<std::string, const char *> mymap);


#endif //ENCODERDECODER_MUX_STREAM_H

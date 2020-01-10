#ifndef ENCODERDECODER_MUX_STREAM_H
#define ENCODERDECODER_MUX_STREAM_H

#include <jni.h>
#include <map>
#include <string>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
}

typedef struct VideoStreamInfo {
    int trackIndex;
    uint8_t * sps_pps_data;
    int sps_pps_data_size;
} VideoStreamInfo;

typedef struct AudioStreamInfo {

    // to construct adts packets
    int trackIndex;
    int profile;
    int channel_count;
    int sample_rate;

} AudioStreamInfo;




typedef struct OutputStream{
	AVStream **out_streams;
	AVOutputFormat *of;
	AVFormatContext *ofmt_ctx;

	AVCodecContext ** avcodecs;
	AVRational *streamSourceTimeBase;

	int nbstreams;
	int allocation_size;		// FIXME : realloc
	const char * path;

	VideoStreamInfo ** videoStreamInfo;
	AudioStreamInfo ** audioStreamInfo;
	int numOfAudioStreams;
	int numOfVideoStreams;

} OutputStream;

OutputStream * init(const char * url, const char * container);

void prepareStart(OutputStream * video_st);

void release(OutputStream * video_st);

void writeFrame(OutputStream * video_st, jint trackIndex, jbyte* framedata, jint offset, jint size, jint flags, jlong presentationTimeUs);

int addTrack(OutputStream * video_st, std::map<std::string, const char *> & mymap);

void queueData2Mux(OutputStream * video_st, jint trackIndex, jbyte* framedata, jint offset, jint size, jint flags, jlong presentationTimeUs);

void addPPSAndSPSBuffer(OutputStream * video_st, jint trackIndex, jbyte* framedata, jint size);

#endif //ENCODERDECODER_MUX_STREAM_H

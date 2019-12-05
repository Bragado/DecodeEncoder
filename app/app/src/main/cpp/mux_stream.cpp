#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include "mux_stream.h"
#include "log.h"



#define DEFAULT_NO_STREAMS 10
#define DEBUG 1

/* local functions */
void add_stream(OutputStream * video_st, AVStream ** stream, std::map<std::string, const char *> format);
static void *thread_func(void*);
int start_logger(const char *app_name);

OutputStream * init(const char * path) {
	// alloc OutputStream space
	OutputStream * video_st = (OutputStream*)malloc(sizeof(OutputStream));
	video_st->allocation_size = DEFAULT_NO_STREAMS;
	video_st->nbstreams = 0;
	video_st->out_streams = (AVStream**)malloc(DEFAULT_NO_STREAMS*sizeof(AVStream*));
	video_st->path = path;
	start_logger("FFMPEG-IMP");
	// Create container
	avformat_alloc_output_context2(&video_st->ofmt_ctx, NULL, NULL, path);
	if(!video_st->ofmt_ctx) {
		LOGE("Could not create output context. Releasing all allocated resources\n");
		release(video_st);
	}
	video_st->of = video_st->ofmt_ctx->oformat;
	strcpy( video_st->ofmt_ctx->filename, video_st->path );
	return video_st;

}

void prepareStart(OutputStream * video_st) {
	if(DEBUG)
		LOGI("native prepareStart called");

	// Init container
	//av_set_parameters(video_st->ofmt_ctx, 0);
	//avformat_write_header(video_st->ofmt_ctx, 0);
	av_dump_format(video_st->ofmt_ctx, 0, video_st->path, 1);
}

void release(OutputStream * video_st) {
	if(DEBUG)
		LOGI("native release called");

	//avformat_free_context(video_st->ofmt_ctx);

	free(video_st->out_streams);
	free(video_st);
	video_st = 0;
}

void writeFrame(OutputStream * video_st, jint trackIndex, jbyte* framedata, jint offset, jint size, jint flags, jlong presentationTimeUs) {
	if(DEBUG)
		LOGI("native  writeFrame called");

	AVPacket packet;
	av_init_packet(&packet);
	if(DEBUG)
		LOGI("new packet created");
	packet.stream_index = (int)trackIndex;
	packet.data = (uint8_t*)framedata + offset;	// check this out
	packet.size = (int)size;
	packet.pts = (int64_t)presentationTimeUs;
	packet.dts = packet.pts;
	packet.flags |= AV_PKT_FLAG_KEY;
	LOGI("NativeWriteFrame: video_st: %p, trackIndex: %d, offset: %d, size: %d, presentationTime: %d", video_st, trackIndex, offset, size, presentationTimeUs);
	LOGI("NativeWriteFrame: path: %s", video_st->path);
	av_interleaved_write_frame( video_st->ofmt_ctx, &packet);
	if(DEBUG)
		LOGI("frame was written to output");
}

int addTrack(OutputStream * video_st, std::map<std::string, const char *> format) {
	LOGI("native addTrack called");
	int index = -1;
	if(video_st == 0) {
		LOGE("FFmpeg muxer was not initialized when trying to add a track");
	}

	// Add video stream
	AVStream *pst =  avformat_new_stream( video_st->ofmt_ctx, 0);
	if(!pst) {
		LOGE("Failed allocating output stream\n");
		return index;
	}

	index = pst->index;
	if(DEBUG)
		LOGI("New stream created with index %d", index);
	add_stream(video_st, &pst, format);

	return index;
}


void add_stream(OutputStream * video_st, AVStream ** stream, std::map<std::string, const char *> format) {

	AVStream *out_stream = *stream;
	AVCodecParameters *pcc = out_stream->codecpar;


	int streamType = atoi(format["streamType"]);		// FIXME
	switch(streamType) {
	case 0:
		//avcodec_get_context_defaults3( pcc, AVMEDIA_TYPE_VIDEO );
		pcc->codec_type = AVMEDIA_TYPE_VIDEO;
		break;
	case 1:
		//avcodec_get_context_defaults3( pcc, AVMEDIA_TYPE_AUDIO );
		pcc->codec_type = AVMEDIA_TYPE_AUDIO;
		break;
	case 2:
		//avcodec_get_context_defaults3( pcc, AVMEDIA_TYPE_SUBTITLE );
		pcc->codec_type = AVMEDIA_TYPE_SUBTITLE;
		break;
	default:
		//avcodec_get_context_defaults2( pcc, AVMEDIA_TYPE_UNKNOWN );
		pcc->codec_type = AVMEDIA_TYPE_UNKNOWN;
		break;
	}

	//pcc->codec_tag = atoi(format["codec_tag"]);
	pcc->codec_type = AVMEDIA_TYPE_VIDEO;		// FIXME
	pcc->codec_id = AV_CODEC_ID_H264; // FIXME
	pcc->bit_rate = atoi(format["bit_rate"]);
	pcc->width = atoi(format["width"]);
	pcc->height = atoi(format["height"]);

	video_st->out_streams[video_st->nbstreams++] = out_stream;

}


AVCodecID getCodecByID(int ID) {
	switch(ID) {
	case 0:
		return AV_CODEC_ID_H264;
	case 1:
		return AV_CODEC_ID_AAC;
	case 2:
		return AV_CODEC_ID_DVB_SUBTITLE;
	case 3:
		return AV_CODEC_ID_DVB_TELETEXT;
		break;
	case 4:
		break;
	}

}

static int pfd[2];
static pthread_t thr;
static const char *tag = "ffmpeg";

int start_logger(const char *app_name)
{
    tag = app_name;

    /* make stdout line-buffered and stderr unbuffered */
    setvbuf(stdout, 0, _IOLBF, 0);
    setvbuf(stderr, 0, _IONBF, 0);

    /* create the pipe and redirect stdout and stderr */
    pipe(pfd);
    dup2(pfd[1], 1);
    dup2(pfd[1], 2);

    /* spawn the logging thread */
    if(pthread_create(&thr, 0, thread_func, 0) == -1)
        return -1;
    pthread_detach(thr);
    return 0;
}

static void *thread_func(void*)
{
    ssize_t rdsz;
    char buf[128];
    while((rdsz = read(pfd[0], buf, sizeof buf - 1)) > 0) {
        if(buf[rdsz - 1] == '\n') --rdsz;
        buf[rdsz] = 0;  /* add null-terminator */
        __android_log_write(ANDROID_LOG_DEBUG, tag, buf);
    }
    return 0;
}


/**
 * Usefull information about ffmpeg:
 *
 * AVCodecParameters : https://ffmpeg.org/doxygen/trunk/structAVCodecParameters.html
 *
 *
 *
 */






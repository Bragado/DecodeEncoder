#include <stdio.h>
#include <stdlib.h>
#include <string.h>


#include "mux_stream.h"
#include "log.h"

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
}

#define DEFAULT_NO_STREAMS 10
#define DEBUG 1

typedef struct OutputStream{
	AVStream **out_streams;
	AVOutputFormat *of;
	AVFormatContext *ofmt_ctx;
	int nbstreams;
	int allocation_size;		// FIXME : realloc
	const char * path;


} OutputStream;


OutputStream * video_st = 0;

void init(const char * path) {
	// alloc OutputStream space
	video_st = (OutputStream*)malloc(sizeof(OutputStream));
	video_st->allocation_size = DEFAULT_NO_STREAMS;
	video_st->nbstreams = 0;
	video_st->out_streams = (AVStream**)malloc(DEFAULT_NO_STREAMS*sizeof(AVStream*));
	video_st->path = path;

	// Create container
	avformat_alloc_output_context2(&video_st->ofmt_ctx, NULL, NULL, path);
	if(!video_st->ofmt_ctx) {
		LOGE("Could not create output context. Releasing all allocated resources\n");
		release();
	}
	video_st->of = video_st->ofmt_ctx->oformat;
	strcpy( video_st->ofmt_ctx->filename, video_st->path );


}

void prepareStart(jobject instance) {
	if(DEBUG)
		LOGI("native prepareStart called");

	// Init container
	//av_set_parameters(video_st->ofmt_ctx, 0);
	//avformat_write_header(video_st->ofmt_ctx, 0);
	av_dump_format(video_st->ofmt_ctx, 0, video_st->path, 1);
}

void release() {
	if(DEBUG)
		LOGI("native release called");

	//avformat_free_context(video_st->ofmt_ctx);

	free(video_st->out_streams);
	free(video_st);
	video_st = 0;
}

void writeFrame(jint trackIndex, jbyte* framedata, jint offset, jint size, jint flags, jlong presentationTimeUs) {
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
	packet.flags |= AV_PKT_FLAG_KEY;
	av_interleaved_write_frame( video_st->ofmt_ctx, &packet);
	if(DEBUG)
		LOGI("frame was written to output");
}

int addTrack(std::map<std::string, const char *> format) {
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
	//add_stream(&pst);

	return index;
}


void add_stream(AVStream ** stream, std::map<std::string, const char *> format) {

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
	pcc->codec_tag = atoi(format["codec_tag"]);
	//pcc->codec_id = atoi(format["codec_id"]); // FIXME
	pcc->bit_rate = atoi(format["bit_rate"]);
	pcc->width = atoi(format["width"]);
	pcc->height = atoi(format["height"]);

	video_st->out_streams[video_st->nbstreams++] = out_stream;

}



/**
 * Usefull information about ffmpeg:
 *
 * AVCodecParameters : https://ffmpeg.org/doxygen/trunk/structAVCodecParameters.html
 *
 *
 *
 */






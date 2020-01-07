#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include <mutex>
#include <queue>
#include "mux_stream.h"
#include "log.h"



#define DEFAULT_NO_STREAMS 10
#define DEBUG 1

/* local functions */
int addVideoStream(OutputStream * video_st, std::map<std::string, const char *>& format);
int addAudioStream(OutputStream * video_st, std::map<std::string, const char *>& format);
int addSubtitleStream(OutputStream * video_st, const std::map<std::string, const char *>& format);
int addUnknownStream(OutputStream * video_st, const std::map<std::string, const char *>& format);
AVCodecID getCodecByID(int ID);
AVRational *videoSourceTimeBase;
AVRational * audioTime;
int64_t pts = 0;

static pthread_t muxThread;
void *muxerThread_func(void *);


/* TODO: erase the next 2 functions */
static void *thread_func(void*);
int start_logger(const char *app_name);

OutputStream * init(const char * url, const char * container) {
	
	start_logger("FFMPEG");

	avformat_network_init();
//	avcodec_register_all();

	// alloc OutputStream space
	OutputStream * video_st = (OutputStream*)malloc(sizeof(OutputStream));
	video_st->allocation_size = DEFAULT_NO_STREAMS;
	video_st->nbstreams = 0;
	video_st->out_streams = (AVStream**)malloc(DEFAULT_NO_STREAMS*sizeof(AVStream*));
	video_st->path = url;
	video_st->avcodecs = (AVCodecContext**)malloc(DEFAULT_NO_STREAMS*sizeof(AVCodec*));
	videoSourceTimeBase = (AVRational*)av_malloc(sizeof(AVRational));
	videoSourceTimeBase->num = 1;
	videoSourceTimeBase->den = 1000000;

	// Create container
	avformat_alloc_output_context2(&video_st->ofmt_ctx, NULL, container, url);
	if(!video_st->ofmt_ctx) {
		LOGE("Could not create output context. Releasing all allocated resources\n");
		release(video_st);
	}
	video_st->of = video_st->ofmt_ctx->oformat;
	video_st->ofmt_ctx->debug = 1;
//	strcpy( video_st->ofmt_ctx->filename, video_st->path );
	return video_st;
		
}

void prepareStart(OutputStream * video_st) {
	if(DEBUG)
		LOGI("native prepareStart called");
	int ret = 1;
	// Init container
	//av_set_parameters(video_st->ofmt_ctx, 0);
	av_dump_format(video_st->ofmt_ctx, 0, video_st->path, 1);

	if (!(video_st->of->flags & AVFMT_NOFILE)) {
		ret = avio_open(&video_st->ofmt_ctx->pb, video_st->path , AVIO_FLAG_WRITE);
		if (ret < 0) {
			LOGE("Could not open output file '%s'", video_st->path);
		}
	}
	ret = avformat_write_header(video_st->ofmt_ctx, NULL);
	if (ret < 0) {
		LOGE("Error occurred when opening output file\n");
	}
	if(pthread_create(&muxThread, 0, thread_func, 0) != 0)
		LOGE("Error occurred when trying to launch the muxer thread\n");
    pthread_detach(muxThread);
}

void release(OutputStream * video_st) {
	if(DEBUG)
		LOGI("native release called");

	//avformat_free_context(video_st->ofmt_ctx);
	av_write_trailer(video_st->ofmt_ctx);

	// close output
	avio_closep(&video_st->ofmt_ctx->pb);
    for(int i = 0; i < video_st->nbstreams; i++) {
        avcodec_close(video_st->avcodecs[i]);
    }


	avformat_free_context(video_st->ofmt_ctx);
	free(video_st->out_streams);
	free(video_st);
	video_st = NULL;
}

void writeFrame(OutputStream * video_st, jint trackIndex, jbyte* framedata, jint offset, jint size, jint flags, jlong presentationTimeUs) {
	if(DEBUG)
		LOGI("native  writeFrame called");
	int ret = -1;

	AVPacket packet;
	av_init_packet(&packet);
	if(DEBUG)
		LOGI("new packet created");
	packet.stream_index = (int)trackIndex;
	packet.data = (uint8_t*)framedata + offset;	// check this out
	packet.size = (int)size;
	packet.pts = (int64_t)presentationTimeUs;		// 90 khz
	packet.dts = AV_NOPTS_VALUE;
	packet.flags |= AV_PKT_FLAG_KEY;
	LOGI("NativeWriteFrame: video_st: %p, trackIndex: %d, offset: %d, size: %d, presentationTime: %lld", video_st, trackIndex, offset, size, (int64_t)presentationTimeUs);
	LOGI("NativeWriteFrame: path: %s", video_st->path);
	packet.pts = av_rescale_q(packet.pts, *videoSourceTimeBase, (video_st->ofmt_ctx->streams[packet.stream_index]->time_base));

    /*LOGI("PACKET PTS DIFF: %lld", packet.pts - pts);
    pts = packet.pts;
*/
   	ret = av_interleaved_write_frame( video_st->ofmt_ctx, &packet);
	if (ret < 0) {
		LOGE("Error muxing packet\n");
	}

	av_packet_unref(&packet);	// wipe the packet
	if(DEBUG)
		LOGI("frame was written to output");
}

int addTrack(OutputStream * video_st, std::map<std::string, const char *>& format) {
	LOGI("native addTrack called");
	if(video_st == 0) {
		LOGE("FFmpeg muxer was not initialized when trying to add a track");
	}
	int streamType = atoi(format["streamType"]);
	int streamIndex = -1;
	switch(streamType){
	case 0:		// video
        streamIndex = addVideoStream(video_st, format);
        break;
	case 1:		// audio
        streamIndex = addAudioStream(video_st, format);
        break;
	case 2:		// subtitle
        streamIndex = addSubtitleStream(video_st, format);
        break;
	default:
        streamIndex = addUnknownStream(video_st, format);
	    break;
	}
	video_st->nbstreams += 1;
    return streamIndex;
}

int addVideoStream(OutputStream * video_st, std::map<std::string, const char *>& format) {
	AVFormatContext *dest = video_st->ofmt_ctx;
	AVCodecContext *c;
	AVStream *st;
	AVCodec *codec;
	int streamIndex = -1;

	int bitrate = atoi(format["bit_rate"]);
	int width = atoi(format["width"]);
	int height = atoi(format["height"]);
	int fps = atoi(format["fps"]);

	LOGI("trying to add stream with: [bit_rate, width, height, fps] = [%d, %d, %d, %d]", bitrate, width, height, fps);

    AVCodecID codecId = getCodecByID(atoi(format["codecID"]));
	codec = avcodec_find_encoder(codecId);
	if(!codec) {
		LOGI("add_video_stream codec not found, as expected. No encoding necessary");
	}
	st = avformat_new_stream(dest, codec);
	if (!st) {
		LOGE("add_video_stream could not alloc stream");
	}
	streamIndex = st->index;   // ok
	LOGI("addVideoStream at index %d", streamIndex);
//	c = st->codec;
    c = avcodec_alloc_context3(codec);
	avcodec_get_context_defaults3(c, codec);        // this is a problem, is c being initialized?

	c->codec_id = codecId;

	/* Sample Parameters */
	c->width    = width;
	c->height   = height;
	c->time_base.den = fps;		// fps
	c->time_base.num = 1;

	c->pix_fmt = AV_PIX_FMT_YUV420P;
    video_st->avcodecs[streamIndex] =  c;
	/*if (dest->oformat->flags & AVFMT_GLOBALHEADER)
			c->flags |= CODEC_FLAG_GLOBAL_HEADER;*/

	return streamIndex;
}

int addAudioStream(OutputStream * video_st, std::map<std::string, const char *>& format) {
	AVFormatContext *formatContext = video_st->ofmt_ctx;
	AVCodecContext *c;
	AVStream *st;
	AVCodec *codec;
	AVCodecParameters * avCodecParameters;
    audioTime = (AVRational*)av_malloc(sizeof(AVRational));
    audioTime->num = 1;
    audioTime->den =  atoi(format["sampleRate"]);

	int audioStreamIndex = -1;

	/* find the audio encoder */
	AVCodecID codecId = getCodecByID(atoi(format["codecID"]));
	codec = avcodec_find_encoder(codecId);
	if (!codec) {
		LOGE("add_audio_stream codec not found");
	}
	st = avformat_new_stream(formatContext, codec);
	if (!st) {
		LOGE("add_audio_stream could not alloc stream");
	}
	audioStreamIndex = st->index;
    avCodecParameters = st->codecpar;
    avCodecParameters->codec_id = codecId;
    avCodecParameters->sample_rate =  atoi(format["sampleRate"]);
    avCodecParameters->channels = atoi(format["channels"]);;
    avCodecParameters->bit_rate = atoi(format["bitrate"]);


    st->time_base = *audioTime;
    c = st->codec;
    //c = avcodec_alloc_context3(codec);
	avcodec_get_context_defaults3(c, codec);

	c->strict_std_compliance = FF_COMPLIANCE_UNOFFICIAL;
	c->sample_fmt  = AV_SAMPLE_FMT_S16;
	c->time_base.den = atoi(format["sampleRate"]);
	c->time_base.num = 1;
    c->bit_rate = atoi(format["bitrate"]);
	c->sample_rate = atoi(format["sampleRate"]);
	c->channels    = atoi(format["channels"]);
	LOGI("addAudioStream sample_rate %d index %d", c->sample_rate, st->index);
    video_st->avcodecs[audioStreamIndex] =  c;
    if (formatContext->oformat->flags & AVFMT_GLOBALHEADER)
        c->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

	return audioStreamIndex;
}

int addSubtitleStream(OutputStream * video_st, const std::map<std::string, const char *>& format) {
	int streamIndex = -1;

	return streamIndex;
}

int addUnknownStream(OutputStream * video_st, const std::map<std::string, const char *>& format) {
	int streamIndex = -1;

	return streamIndex;
}




AVCodecID getCodecByID(int ID) {            // must be the same as FFmpegUtil.java
	switch(ID) {
	case 0:
		return AV_CODEC_ID_H264;
	case 1:
        return AV_CODEC_ID_H265;
	case 2:
        return AV_CODEC_ID_AAC;
	case 3:
        return AV_CODEC_ID_FLAC;
	case 6:
        break;
    case 7:
        return AV_CODEC_ID_DVB_TELETEXT;
    case 8:
        return AV_CODEC_ID_DVB_SUBTITLE;

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

struct SampleData {
	OutputStream ** video_st;
	jint trackIndex;
	jbyte ** framedata;
	jint offset; jint size;
	jint flags; jlong presentationTimeUs;
};



std::mutex m;
std::condition_variable cv;
std::queue<SampleData> data;
bool canceled = false;


void *muxerThread_func(void *) {
	std::unique_lock<std::mutex> lk(m);
	LOGI("muxerThread_func");
	while(!canceled) {
		// wait until there's data to be muxed
		cv.wait(lk, [] {return data.size() > 0;});
		LOGI("Frame Received");
		// lock to avoid race-conditions
		m.lock();
		SampleData sampleData = data.front();
		data.pop();
		m.unlock();

		// mux data
		writeFrame(*(sampleData.video_st), sampleData.trackIndex, *(sampleData.framedata), sampleData.offset, sampleData.size, sampleData.flags, sampleData.presentationTimeUs);
		free(*(sampleData.framedata));
	}
    pthread_exit(NULL);
}

void queueData2Mux(OutputStream * video_st, jint trackIndex, jbyte* framedata, jint offset, jint size, jint flags, jlong presentationTimeUs) {

	SampleData sampleData;
	sampleData.video_st = &video_st;
	sampleData.trackIndex = trackIndex;
	u_int8_t * encodedData = (u_int8_t  *)malloc((size - offset)*sizeof(u_int8_t));
	memcpy(encodedData, framedata + offset, (size_t)(size - offset));
	sampleData.framedata = (jbyte**) &encodedData;
	sampleData.offset = offset;
	sampleData.size = size;
	sampleData.flags = flags;
	sampleData.presentationTimeUs = presentationTimeUs;

	m.lock();
	data.push(sampleData);
	m.unlock();
}



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
#define DEBUG 0
#define ADTS_LENGTH 7

/* local functions */
int addVideoStream(OutputStream * video_st, std::map<std::string, const char *>& format);
int addAudioStream(OutputStream * video_st, std::map<std::string, const char *>& format);
int addSubtitleStream(OutputStream * video_st, const std::map<std::string, const char *>& format);
int addUnknownStream(OutputStream * video_st, const std::map<std::string, const char *>& format);
int maybeProcessPacket(OutputStream * video_st, AVPacket * packet, uint8_t * data, int size, int flags , int trackIndex);

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

    if(DEBUG)
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
	video_st->audioStreamInfo = (AudioStreamInfo**)malloc(DEFAULT_NO_STREAMS*sizeof(AudioStreamInfo*));
	video_st->videoStreamInfo = (VideoStreamInfo**)malloc(DEFAULT_NO_STREAMS*sizeof(VideoStreamInfo*));
	video_st->numOfAudioStreams = 0;
	video_st->numOfVideoStreams = 0;
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
	int numOfVideoStreams = video_st->numOfVideoStreams;
	for(int i = 0; i < numOfVideoStreams; i++) {
        free(video_st->videoStreamInfo[i]->sps_pps_data);
	    free(video_st->videoStreamInfo[i]);
	}
	int numOfAudioStreams = video_st->numOfAudioStreams;
	for(int i = 0; i < numOfAudioStreams; i++)
	    free(video_st->audioStreamInfo[i]);

	free(video_st->audioStreamInfo);
	free(video_st->videoStreamInfo);


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
	//packet.data = (uint8_t*)framedata + offset;	// check this out
	//packet.size = (int)size;

	// TODO: test this approach
	int new_buffer_allocated = maybeProcessPacket(video_st, &packet, (uint8_t*)framedata, size, flags , trackIndex);


    	packet.pts = (int64_t)presentationTimeUs;		// 90 khz
	packet.dts = AV_NOPTS_VALUE;
	packet.flags |= AV_PKT_FLAG_KEY;

	if(DEBUG)
	    LOGI("NativeWriteFrame: video_st: %p, trackIndex: %d, offset: %d, size: %d, presentationTime: %lld", video_st, trackIndex, offset, size, (int64_t)presentationTimeUs);
	packet.pts = av_rescale_q(packet.pts, *videoSourceTimeBase, (video_st->ofmt_ctx->streams[packet.stream_index]->time_base));

   	ret = av_interleaved_write_frame( video_st->ofmt_ctx, &packet);
	if (ret < 0) {
		LOGE("Error muxing packet\n");
	}
	if(new_buffer_allocated)
	    free(packet.data);

	av_packet_unref(&packet);	// wipe the packet
	//if(DEBUG)
		LOGI("frame was written to output");

}

void addPPSAndSPSBuffer(OutputStream * video_st, jint trackIndex, jbyte* framedata, jint size) {
	for(int i = 0; i < video_st->numOfVideoStreams; i++) {
		if(video_st->videoStreamInfo[i]->trackIndex == (int)trackIndex) {
			video_st->videoStreamInfo[i]->sps_pps_data = (uint8_t *)malloc(size*sizeof(uint8_t));

			video_st->videoStreamInfo[i]->sps_pps_data_size = size;
			memcpy(video_st->videoStreamInfo[i]->sps_pps_data, (uint8_t *)framedata, size);
			return;
		}
	}
}


int getFreqIndex(int sampleRate) {
	switch(sampleRate) {
		case 96000:
			return 0;
		case 88200:
			return 1;
		case 64000:
			return 2;
		case 48000:
			return 3;
		case 44100:
			return 4;
		case 32000:
			return 5;
		case 24000:
			return 6;
		case 22050:
			return 7;
		case 16000:
			return 8;
		case 12000:
			return 9;
		case 11025:
			return 10;
		case 8000:
			return 11;
		case 7350:
			return 12;
		default:
			return -1;

	}
}

uint8_t  * addAdtsPacket(OutputStream * video_st, uint8_t* data , int dataLen,  int trackIndex) {
	AudioStreamInfo ** audioStreamInfos = video_st->audioStreamInfo;
	AudioStreamInfo * audioStreamInfo;
	for(int i = 0; i < video_st->numOfAudioStreams; i++) {
		if(audioStreamInfos[i]->trackIndex == trackIndex) {
			audioStreamInfo = audioStreamInfos[i];
			break;
		}
	}
    unsigned char * adts_packet = (uint8_t *)malloc(sizeof(uint8_t)*(dataLen + ADTS_LENGTH));
	int freqIdx = getFreqIndex(audioStreamInfo->sample_rate);
    dataLen += ADTS_LENGTH;
	adts_packet[0] = 0xFF;
	adts_packet[1] = 0xF9;	// 1111 1 00 1  = syncword MPEG-2 Layer CRC
	adts_packet[2] = ((audioStreamInfo->profile-1)<<6) + (freqIdx<<2) +(audioStreamInfo->channel_count>>2);
	adts_packet[3] = ((audioStreamInfo->channel_count&3)<<6) + (dataLen>>11);
	adts_packet[4] = (dataLen&0x7FF) >> 3;
	adts_packet[5] = ((dataLen&7)<<5) + 0x1F;
	adts_packet[6] = 0xFC;

	memcpy(adts_packet + ADTS_LENGTH, data, dataLen);

	return adts_packet;
}

int maybeProcessPacket(OutputStream * video_st, AVPacket * packet, uint8_t * data, int size, int flags , int trackIndex) {
	AVStream * avStream = video_st->ofmt_ctx->streams[trackIndex];
	int ret = 0;
	switch(avStream->codecpar->codec_id) {
		case AV_CODEC_ID_AAC:
			packet->data = addAdtsPacket(video_st, data, size, trackIndex);
			packet->size = size + 7;
            ret = 1;
			break;
		case AV_CODEC_ID_H264:
		case AV_CODEC_ID_H265:
			if((flags & 1) != 0) {
				for(int i = 0; i < video_st->numOfVideoStreams; i++) {
					if(video_st->videoStreamInfo[i]->trackIndex == trackIndex) {
						packet->data = (uint8_t *)malloc(sizeof(uint8_t)*(size + video_st->videoStreamInfo[i]->sps_pps_data_size));
                        memcpy(packet->data, video_st->videoStreamInfo[i]->sps_pps_data, video_st->videoStreamInfo[i]->sps_pps_data_size);
						memcpy(packet->data + video_st->videoStreamInfo[i]->sps_pps_data_size, data, size);
						packet->size = size + video_st->videoStreamInfo[i]->sps_pps_data_size;

					}
				}
				ret = 1;
                break;
			}

		default:
			packet->data = data;
			packet->size = size;
			break;
	}
	return ret;
}



int addTrack(OutputStream * video_st, std::map<std::string, const char *>& format) {
    if(DEBUG)
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

    if(DEBUG)
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


    AVCodecParameters * avCodecParameters = st->codecpar;
    avCodecParameters->codec_id = codecId;
    avCodecParameters->width =  width;
    avCodecParameters->height = height;
    avCodecParameters->bit_rate = bitrate;

    // TODO: test if there's any improvement
   /* uint8_t * extradata = (uint8_t *)format["csd-0"];
    int extradata_size = atoi(format["csd-0_size"]);
    c->extradata_size = extradata_size;
    memcpy(c->extradata, extradata, extradata_size);*/

	/*if (dest->oformat->flags & AVFMT_GLOBALHEADER)
			c->flags |= CODEC_FLAG_GLOBAL_HEADER;*/
	video_st->videoStreamInfo[video_st->numOfVideoStreams] = (VideoStreamInfo *)malloc(sizeof(VideoStreamInfo));
	video_st->videoStreamInfo[video_st->numOfVideoStreams]->trackIndex = streamIndex;
	video_st->numOfVideoStreams += 1;

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

	AudioStreamInfo ** audioStreams = video_st->audioStreamInfo;
	audioStreams[video_st->numOfAudioStreams] = (AudioStreamInfo *)malloc(sizeof(AudioStreamInfo));
	audioStreams[video_st->numOfAudioStreams]->trackIndex = audioStreamIndex;

	audioStreams[video_st->numOfAudioStreams]->sample_rate = codecId == AV_CODEC_ID_AAC ? avCodecParameters->sample_rate : -1;
	audioStreams[video_st->numOfAudioStreams]->channel_count = codecId == AV_CODEC_ID_AAC ? avCodecParameters->channels : -1;
	audioStreams[video_st->numOfAudioStreams]->profile = codecId == AV_CODEC_ID_AAC ? atoi(format["profile"]) : -1;

	video_st->numOfAudioStreams += 1;
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
	OutputStream * video_st;
	jint trackIndex;
	jbyte * framedata;
	jint offset; jint size;
	jint flags; jlong presentationTimeUs;
};



std::mutex m;
std::condition_variable cv;
std::queue<SampleData *> data;
bool canceled = false;


void *muxerThread_func(void *) {
	std::unique_lock<std::mutex> lk(m);
	LOGI("muxerThread_func");
	while(!canceled) {
		// wait until there's data to be muxed
		cv.wait(lk, [] {return data.size() > 0;});
		LOGI("Frame Received");
		// lock to avoid race-conditions

		SampleData * sampleData = data.front();
		data.pop();
		m.unlock();

		// mux data
		writeFrame(sampleData->video_st, sampleData->trackIndex, sampleData->framedata, sampleData->offset, sampleData->size, sampleData->flags, sampleData->presentationTimeUs);
		free(sampleData->framedata);
		free(sampleData);
	}
    pthread_exit(NULL);
}

void queueData2Mux(OutputStream * video_st, jint trackIndex, jbyte* framedata, jint offset, jint size, jint flags, jlong presentationTimeUs) {

	SampleData* sampleData = (SampleData *)malloc(sizeof(SampleData));
	sampleData->video_st = video_st;
	sampleData->trackIndex = trackIndex;
	u_int8_t * encodedData = (u_int8_t  *)malloc((size - offset)*sizeof(u_int8_t));
	memcpy(encodedData, framedata + offset, (size_t)(size - offset));
	sampleData->framedata = (jbyte*) encodedData;
	sampleData->offset = 0;
	sampleData->size = size;
	sampleData->flags = flags;
	sampleData->presentationTimeUs = presentationTimeUs;

    std::lock_guard<std::mutex> lk(m);
	data.push(sampleData);
	m.unlock();
    cv.notify_one();
}



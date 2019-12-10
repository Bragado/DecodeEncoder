//
// Created by Alexander Berezhnoi on 24/03/19.
//

#include "video_file_config_builder.h"
#include "video_stream.h"
#include "utils.h"

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/bprint.h>
}

static jstring toJString(const char *cString) {
    jstring result = nullptr;
    if (cString != nullptr) {
        result = utils_get_env()->NewStringUTF(cString);
    }
    return result;
}

static jstring get_string(AVDictionary *metadata, const char *key) {
    jstring result = nullptr;
    AVDictionaryEntry *tag = av_dict_get(metadata, key, nullptr, 0);
    if (tag != nullptr) {
        result = utils_get_env()->NewStringUTF(tag->value);
    }
    return result;
}

static jstring get_title(AVDictionary *metadata) {
    return get_string(metadata, "title");
}

static jstring get_language(AVDictionary *metadata) {
    return get_string(metadata, "language");
}

static void onError(jobject jMediaFileBuilder) {
    utils_call_instance_method(jMediaFileBuilder,
                               fields.MediaFileBuilder.onErrorID);
}

static void onMediaFileFound(jobject jMediaFileBuilder, AVFormatContext *avFormatContext) {
    const char *fileFormatName = avFormatContext->iformat->long_name;

    jstring jFileFormatName = utils_get_env()->NewStringUTF(fileFormatName);

    utils_call_instance_method(jMediaFileBuilder,
                               fields.MediaFileBuilder.onMediaFileFoundID,
                               jFileFormatName);
}

static void onVideoStreamFound(jobject jMediaFileBuilder,
                               AVFormatContext *avFormatContext,
                               int index) {
    AVCodecParameters *parameters = avFormatContext->streams[index]->codecpar;

    auto *videoStream = (VideoStream *) malloc(sizeof(VideoStream));;
    videoStream->avFormatContext = avFormatContext;
    videoStream->parameters = parameters;
    videoStream->avVideoCodec = avcodec_find_decoder(parameters->codec_id);
    videoStream->videoStreamIndex = index;

    jstring jCodecName = utils_get_env()->NewStringUTF(videoStream->avVideoCodec->long_name);

    utils_call_instance_method(jMediaFileBuilder,
                               fields.MediaFileBuilder.onVideoStreamFoundID,
                               parameters->width,
                               parameters->height,
                               jCodecName,
                               video_stream_get_handle(videoStream));
}

static void onAudioStreamFound(jobject jMediaFileBuilder,
                               AVFormatContext *avFormatContext,
                               int index) {
    AVStream *stream = avFormatContext->streams[index];
    AVCodecParameters *parameters = stream->codecpar;

    auto codecDescriptor = avcodec_descriptor_get(parameters->codec_id);
    jstring jCodecName = utils_get_env()->NewStringUTF(codecDescriptor->long_name);

    auto avSampleFormat = static_cast<AVSampleFormat>(parameters->format);
    auto jSampleFormat = toJString(av_get_sample_fmt_name(avSampleFormat));

    jstring jChannelLayout = nullptr;
    if (parameters->channel_layout) {
        AVBPrint printBuffer;
        av_bprint_init(&printBuffer, 1, AV_BPRINT_SIZE_UNLIMITED);
        av_bprint_clear(&printBuffer);
        av_bprint_channel_layout(&printBuffer, parameters->channels, parameters->channel_layout);
        jChannelLayout = toJString(printBuffer.str);
        av_bprint_finalize(&printBuffer, NULL);
    }

    utils_call_instance_method(jMediaFileBuilder,
                               fields.MediaFileBuilder.onAudioStreamFoundID,
                               index,
                               jCodecName,
                               get_title(stream->metadata),
                               get_language(stream->metadata),
                               parameters->bit_rate,
                               jSampleFormat,
                               parameters->sample_rate,
                               parameters->channels,
                               jChannelLayout,
                               stream->disposition);
}

static void onSubtitleStreamFound(jobject jMediaFileBuilder,
                                  AVFormatContext *avFormatContext,
                                  int index) {

    AVStream *stream = avFormatContext->streams[index];
    AVCodecParameters *parameters = stream->codecpar;

    auto codecDescriptor = avcodec_descriptor_get(parameters->codec_id);
    jstring jCodecName = utils_get_env()->NewStringUTF(codecDescriptor->long_name);

    utils_call_instance_method(jMediaFileBuilder,
                               fields.MediaFileBuilder.onSubtitleStreamFoundID,
                               index,
                               jCodecName,
                               stream->disposition,
                               get_title(stream->metadata),
                               get_language(stream->metadata));
}

static int STREAM_VIDEO = 1;
static int STREAM_AUDIO = 1 << 1;
static int STREAM_SUBTITLE = 1 << 2;

static void video_file_config_build(jobject jMediaFileBuilder, const char *uri, int mediaStreamsMask, AVFormatContext *avFormatContext) {
    if (avformat_open_input(&avFormatContext, uri, nullptr, nullptr)) {
        onError(jMediaFileBuilder);
        return;
    }

    if (avformat_find_stream_info(avFormatContext, nullptr) < 0) {
        avformat_free_context(avFormatContext);
        onError(jMediaFileBuilder);
        return;
    };

    onMediaFileFound(jMediaFileBuilder, avFormatContext);

    for (int pos = 0; pos < avFormatContext->nb_streams; pos++) {
        AVCodecParameters *parameters = avFormatContext->streams[pos]->codecpar;
        AVMediaType type = parameters->codec_type;
        switch (type) {
            case AVMEDIA_TYPE_VIDEO:
                if (mediaStreamsMask & STREAM_VIDEO) {
                    onVideoStreamFound(jMediaFileBuilder, avFormatContext, pos);
                }
                break;
            case AVMEDIA_TYPE_AUDIO:
                if (mediaStreamsMask & STREAM_AUDIO) {
                    onAudioStreamFound(jMediaFileBuilder, avFormatContext, pos);
                }
                break;
            case AVMEDIA_TYPE_SUBTITLE:
                if (mediaStreamsMask & STREAM_SUBTITLE) {
                    onSubtitleStreamFound(jMediaFileBuilder, avFormatContext, pos);
                }
                break;
        }
    }
}

void video_file_config_build(jobject jMediaFileBuilder, const char *uri, int mediaStreamsMask) {
    video_file_config_build(jMediaFileBuilder, uri, mediaStreamsMask, nullptr);
}

void video_file_config_build(jobject jMediaFileBuilder, int fileDescriptor, int mediaStreamsMask) {
    char pipe[32];
    sprintf(pipe, "pipe:%d", fileDescriptor);

    video_file_config_build(jMediaFileBuilder, pipe, mediaStreamsMask, nullptr);
}

void video_file_config_build(jobject jMediaFileBuilder, int assetFileDescriptor, int64_t startOffset, const char *shortFormatName, int mediaStreamsMask) {
    char str[32];
    sprintf(str, "pipe:%d", assetFileDescriptor);

    AVFormatContext *predefinedContext = avformat_alloc_context();
    predefinedContext->skip_initial_bytes = startOffset;
    predefinedContext->iformat = av_find_input_format(shortFormatName);

    video_file_config_build(jMediaFileBuilder, str, mediaStreamsMask, predefinedContext);
}
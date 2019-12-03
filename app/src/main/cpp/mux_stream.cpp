#include "mux_stream.h"
#include "log.h"

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
}

class OutputStream {
	
	public :
		AVStream **in_streams;
		AVStream **out_streams;
		
		AVFormatContext *ifmt_ctx;
		AVFormatContext *ofmt_ctx;
		int nbstreams;
	
};


void prepareStart(jobject instance) {
	LOGI("native prepareStart called"); 
}

void release(jobject instance) {
	LOGI("native release called");
}

void writeFrame(jint trackIndex, jobject byteBuffer, jint offset, jint size, jint flags, jlong presentationTimeUs) {
	LOGI("native  writeFrame called");
}

int addTrack(std::map<std::string, std::string> mymap) {
	LOGI("native addTrack called");
				
		
	return 0;
}


int add_stream(OutputStream * ost, AVFormatContext *oc, AVStream *in_stream) {
	
	AVStream *out_stream;
	AVCodecParameters *in_codecpar = in_stream->codecpar;
	
	out_stream = avformat_new_stream(oc, NULL);	
	if (!out_stream) {		// TODO: free out_stream
        	LOGE("Failed allocating output stream\n");
        	return -1;
        }
	avcodec_parameters_copy(out_stream->codecpar, in_codecpar);
	if (ret < 0) {			// TODO: free out_stream
              	LOGE("Failed to copy codec parameters\n");
              	return -1;
        }	
	out_stream->codecpar->codec_tag = 0;
	ost->in_streams[ost->nstreams] = in_stream;
	ost->out_streams[ost->nstreams] = out_stream;
	ost->nbstreams += 1;
	return ost->nbstreams - 1;
	
}







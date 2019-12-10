package com.example.decoderencoder.Library;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AVMuxer {
    MediaMuxer mediaMuxer = null;
    int containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
    boolean mediaMuxerStarted = false;
    boolean is2useMediaMuxer = true;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public AVMuxer(String outputPath, int containerFormat) throws IOException {
        if( containerFormat > -1)
            this.containerFormat = containerFormat;
        this.mediaMuxer = new MediaMuxer(outputPath, containerFormat);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stop() {
        try {
            mediaMuxer.stop();
            mediaMuxer.release();
        }catch(Exception e) {

        }
        mediaMuxer = null;
        mediaMuxerStarted = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public int addTrack(MediaFormat newFormat) {
        return mediaMuxer.addTrack(newFormat);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void start() {
        mediaMuxer.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void writeSampleData(int mediaMuxerTrackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        mediaMuxer.writeSampleData(mediaMuxerTrackIndex, encodedData, bufferInfo);
    }
}

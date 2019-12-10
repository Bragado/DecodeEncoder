package com.example.decoderencoder.library.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.TimestampAdjuster;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaCodecMuxer implements MediaMuxer {

    android.media.MediaMuxer muxer;
    boolean started = false;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public MediaCodecMuxer(String path, int container) {
        try {
            this.muxer = new  android.media.MediaMuxer(path, container);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void stop() {
        muxer.stop();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public int addTrack(MediaFormat newFormat) {
        return muxer.addTrack(newFormat);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void start() {
        muxer.start();
        started = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void release() {
        muxer.release();

    }

    @Override
    public void sniff(int container) {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void writeSampleData(int trackIndex, ByteBuffer byteBuf, int offset, int size, int flags,  long presentationTimeUs) {
        MediaCodec.BufferInfo bf = new MediaCodec.BufferInfo();
        bf.flags = flags;
        bf.offset = offset;
        bf.presentationTimeUs = presentationTimeUs;
        if(bf.presentationTimeUs < 0) {
            return;
        }
        bf.size = size;
        muxer.writeSampleData(trackIndex, byteBuf, bf);
    }

}

package com.example.decoderencoder.library.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.source.SampleStream;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaCodecMuxer implements MediaMuxer, SampleOutput {

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

    @Override
    public void release() {

    }

    @Override
    public void sniff(int container) {

    }

    @Override
    public boolean isReady() {
        return started;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public int writeData(int trackId, byte[] data, Object info) {
        muxer.writeSampleData(trackId, ByteBuffer.wrap(data), (MediaCodec.BufferInfo)info);
        return 0;
    }
}

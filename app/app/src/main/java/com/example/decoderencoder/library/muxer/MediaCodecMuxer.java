package com.example.decoderencoder.library.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.encoder.EncoderBuffer;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.Log;
import com.example.decoderencoder.library.util.TimestampAdjuster;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;

public class MediaCodecMuxer implements MediaMuxer {

    private static final String TAG = "MediaCodecMuxer";

    android.media.MediaMuxer muxer;
    boolean started = false;
    long last_pts[] = new long[0];


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
        last_pts = Arrays.copyOf(last_pts, last_pts.length + 1);
        last_pts[last_pts.length - 1] = 0;

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
    public void writeSampleData(int trackIndex, EncoderBuffer encoderBuffer) {

        long presentationTimeUs = encoderBuffer.presentationTimeUs;
        if(presentationTimeUs < 0) {
            return;
        }
        if(presentationTimeUs < last_pts[trackIndex]) {
            Log.d(TAG, "Packet drop due to bad pts : [last_pts, current_pts] = " +  "["+ last_pts + ","+ presentationTimeUs +"]");
            return;
        }
        last_pts[trackIndex] = encoderBuffer.presentationTimeUs;
        MediaCodec.BufferInfo bf = new MediaCodec.BufferInfo();
        bf.flags = encoderBuffer.flags;
        bf.offset = encoderBuffer.offset;
        bf.presentationTimeUs = encoderBuffer.presentationTimeUs;
        bf.size = encoderBuffer.size;
        muxer.writeSampleData(trackIndex, encoderBuffer.data, bf);
    }

    @Override
    public void addConfigBuffer(int trackId, byte[] content, int size) {

    }



}

package com.example.decoderencoder.library.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public interface MediaMuxer {

    public void stop();

    public int addTrack(MediaFormat newFormat);

    public void start();

    public void release();

    public void sniff(int container);

    public void writeSampleData(int trackIndex, ByteBuffer byteBuf, int offset, int size, int flags, long presentationTimeUs);

}

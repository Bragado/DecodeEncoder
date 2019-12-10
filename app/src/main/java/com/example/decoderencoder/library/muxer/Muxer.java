package com.example.decoderencoder.library.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public interface Muxer {

    public void stop();

    public int addTrack(MediaFormat newFormat);

    public void start();

    public void writeSampleData(int mediaMuxerTrackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo);

    public void release();

}

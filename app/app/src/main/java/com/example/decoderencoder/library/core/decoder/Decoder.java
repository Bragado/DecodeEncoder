package com.example.decoderencoder.library.core.decoder;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Decoder {

    public boolean makeCodecReady(MediaFormat mediaFormat, Surface surface);

    public String getName();

    public int dequeueOutputBuffer(MediaCodec.BufferInfo info, long timeoutUs);

    public void releaseOutputBuffer(int index,  boolean render);

    public void signalEndOfInputStream();

    public ByteBuffer getOutputBuffer(int index);

    public ByteBuffer getInputBuffer(int index);

    public void queueInputBuffer(
            int index,
            int offset, int size, long presentationTimeUs, int flags);

    public void stop();

    public void release();

    public void start();

    public void configure(
                MediaFormat format,
                Surface surface, MediaCrypto crypto,
                int flags);


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    Decoder createDecoderByType(String type) throws IOException;

    int dequeueInputBuffer(int timeout);

    MediaCodec getCodec();

    ByteBuffer[] getInputBuffers();

    ByteBuffer[] getOutputBuffers();
}

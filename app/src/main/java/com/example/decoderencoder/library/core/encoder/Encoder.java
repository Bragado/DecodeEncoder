package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.view.Surface;

import java.nio.ByteBuffer;

public interface Encoder {

    public void queueInputBuffer(
            int index,
            int offset, int size, long presentationTimeUs, int flags);

    ByteBuffer getOutputBuffer(int outputIndex);

    boolean makeCodecReady(MediaFormat mediaFormat);

    ByteBuffer[] getInputBuffers();

    public interface Callback {
        public void onSurfaceReady(Object surface);
    }

    Surface createInputSurface();

    void start();

    void signalEndOfInputStream();

    ByteBuffer[] getOutputBuffers();

    ByteBuffer getInputBuffer(int index);

    int dequeueOutputBuffer(MediaCodec.BufferInfo info, long timeoutUs);

    int dequeueInputBuffer(int timeout);

    MediaFormat getOutputFormat();

    void releaseOutputBuffer(int index, boolean render);

    void stop();

    void release();

    void configure(MediaFormat format, Surface surface, MediaCrypto crypto, int flags);

}

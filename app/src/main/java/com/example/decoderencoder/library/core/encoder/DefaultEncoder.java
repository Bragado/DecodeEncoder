package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.Format;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DefaultEncoder implements Encoder {

    MediaCodec encoder;

    public DefaultEncoder() {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void queueInputBuffer(int index, int offset, int size, long presentationTimeUs, int flags) {
        encoder.queueInputBuffer(index, offset, size, presentationTimeUs, flags);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public ByteBuffer getOutputBuffer(int outputIndex) {
        return encoder.getOutputBuffer(outputIndex);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean makeCodecReady(MediaFormat mediaFormat) {
        if(encoder != null)
            return true;

        try {
            encoder = MediaCodec.createEncoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        encoder.configure(mediaFormat,  null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public ByteBuffer[] getInputBuffers() {
        return encoder.getInputBuffers();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public Surface createInputSurface() {
        return encoder.createInputSurface();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void start() {
        encoder.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void signalEndOfInputStream() {
        encoder.signalEndOfInputStream();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public ByteBuffer[] getOutputBuffers() {
        return encoder.getOutputBuffers();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public ByteBuffer getInputBuffer(int index) {
        return encoder.getInputBuffer(index);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int dequeueOutputBuffer(MediaCodec.BufferInfo info, long timeoutUs) {
        return encoder.dequeueOutputBuffer(info, timeoutUs);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int dequeueInputBuffer(int timeout) {
        return encoder.dequeueInputBuffer(timeout);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public MediaFormat getOutputFormat() {
        return encoder.getOutputFormat();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void releaseOutputBuffer(int index, boolean render) {
        encoder.releaseOutputBuffer(index, render);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void stop() {
        encoder.stop();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void release() {
        encoder.release();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void configure(MediaFormat format, Surface surface, MediaCrypto crypto, int flags) {
        encoder.configure(format, surface, crypto, flags);
    }
}

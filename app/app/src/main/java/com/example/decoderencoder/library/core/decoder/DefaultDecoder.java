package com.example.decoderencoder.library.core.decoder;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.source.TrackGroup;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DefaultDecoder implements Decoder {

    TrackGroup trackGroup;
    MediaCodec decoder;

    public DefaultDecoder(TrackGroup trackGroup) {
        this.trackGroup = trackGroup;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean makeCodecReady(MediaFormat mediaFormat, Surface surface) {
        if(decoder != null)
            return true;

        Format f = trackGroup.getFormat(0);
        try {

            decoder = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {       // TODO: passthrough or ffmeg decoder
            e.printStackTrace();
            return false;
        }
        decoder.configure(mediaFormat, surface != null ? surface : null, null, 0);
        return true;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public String getName() {
        return decoder.getName();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int dequeueOutputBuffer(MediaCodec.BufferInfo info, long timeoutUs) {
        return decoder.dequeueOutputBuffer(info, timeoutUs);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void releaseOutputBuffer(int index, boolean render) {
        decoder.releaseOutputBuffer(index, render);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void signalEndOfInputStream() {
        decoder.signalEndOfInputStream();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public ByteBuffer getOutputBuffer(int index) {
        return decoder.getOutputBuffer(index);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public ByteBuffer getInputBuffer(int index) {
        return decoder.getInputBuffer(index);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void queueInputBuffer(int index, int offset, int size, long presentationTimeUs, int flags) {
        decoder.queueInputBuffer(index, offset, size, presentationTimeUs, flags);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void stop() {
        decoder.stop();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void release() {
        decoder.release();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void start() {
        decoder.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void configure(MediaFormat format, Surface surface, MediaCrypto crypto, int flags) {
        decoder.configure(format, surface, crypto, flags);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Decoder createDecoderByType(String type) throws IOException {
        this.decoder = MediaCodec.createDecoderByType(trackGroup.getFormat(0).sampleMimeType);
        return this;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int dequeueInputBuffer(int timeout) {
        return decoder.dequeueInputBuffer(timeout);
    }

    @Override
    public MediaCodec getCodec() {
        return this.decoder;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public ByteBuffer[] getInputBuffers() {
        return decoder.getInputBuffers();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public ByteBuffer[] getOutputBuffers() {
        return decoder.getOutputBuffers();
    }
}

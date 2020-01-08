package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.output.MediaOutput;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Handles the writing to MuxerInput
 */
public abstract class BaseCodification implements Codification {

    Renderer renderer;
    Encoder encoder;
    MediaFormat format;
    MuxerInput muxerInput;
    MediaOutput mediaOutput;

    public BaseCodification(Renderer renderer, Encoder encoder, MediaFormat format, MediaOutput mediaOutput) {
        this.renderer = renderer;
        this.format = format;
        this.encoder = encoder;
        this.mediaOutput = mediaOutput;
    }


    @Override
    public int getTrackType() {
        return 0;
    }


    @Override
    public void stop() {
        encoder.stop();
        encoder.release();
        onStop();
    }

    @Override
    public void disable() {
        encoder.release();
        onRelease();
    }

    @Override
    public void reset() {

    }

    @Override
    public void enable(Renderer renderer) {
        this.renderer = renderer;
    }

    public abstract void onRelease();

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    final void onDataReady(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        onDataReady(new EncoderBuffer(outputBuffer, bufferInfo.offset, bufferInfo.size, bufferInfo.flags, bufferInfo.presentationTimeUs));
    }
    final void onDataReady(EncoderBuffer buffer) {
        mediaOutput.maybeStartMuxer();
        try {
            muxerInput.sampleData(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void addTrack(MediaFormat trackFormat) {
        this.muxerInput = mediaOutput.newTrackDiscovered(trackFormat);
    }

    @Override
    public boolean feedInputBuffer() {
        return false;
    }

    public abstract void onStop();

    protected void addConfigBuffer(byte[] content, int size) {
        muxerInput.addConfigBuffer(content, size);
    }
}

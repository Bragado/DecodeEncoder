package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.output.MediaOutput;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.RequiresApi;

/**
 * Handles the writing of samples to MuxerInput
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
    }       // FIXME


    /**
     * Stops the codec and propagates the signal to each child to release all kept resource
     */
    @Override
    public void stop() {
        if(encoder != null) {
            encoder.stop();
            encoder.release();
        }
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

    /**
     * Sets this Codification Renderer
     * @param renderer The corresponding renderer from wich the encoder will encode data.
     */
    @Override
    public void enable(Renderer renderer) {
        this.renderer = renderer;
    }

    public abstract void onRelease();

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    final void onDataReady(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        onDataReady(new EncoderBuffer(outputBuffer, bufferInfo.offset, bufferInfo.size, bufferInfo.flags, bufferInfo.presentationTimeUs));
    }

    /**
     * Tries to start muxing and sends an encoded sample to its assigned muxerInput.
     * In order to start muxing all codifications must call {@link #addTrack}
     * @param buffer
     */
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

    /**
     * Requests a muxerInput to write the encoded samples.
     * @param trackFormat samples track format
     */
    public void addTrack(MediaFormat trackFormat) {
        this.muxerInput = mediaOutput.newTrackDiscovered(trackFormat);
    }

    @Override
    public boolean feedInputBuffer() {
        return false;
    }

    public abstract void onStop();

    /**
     * Used to add codec extra data
     * @param content buffer returned by MediaCodec
     * @param size of the buffer
     */
    protected void addConfigBuffer(byte[] content, int size) {
        muxerInput.addConfigBuffer(content, size);
    }
}

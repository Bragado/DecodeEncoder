package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.muxer.MuxerInput;

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

    public BaseCodification(Renderer renderer, Encoder encoder, MediaFormat format, MuxerInput muxerInput) {
        this.renderer = renderer;
        this.format = format;
        this.encoder = encoder;
        this.muxerInput = muxerInput;
    }


    @Override
    public int getTrackType() {
        return 0;
    }


    @Override
    public void stop() {
        encoder.stop();
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
    final void onDataReady(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo, int trackId) {
       /* try {
            muxerInput.sampleData(new EncoderBuffer(outputBuffer, bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs), trackId);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

    }

}

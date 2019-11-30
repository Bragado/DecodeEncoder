package com.example.decoderencoder.library.core.encoder;

import android.media.MediaFormat;

import com.example.decoderencoder.library.core.decoder.Renderer;

/**
 * Handles the writing to MuxerInput
 */
public abstract class BaseCodification implements Codification {

    Renderer renderer;
    Encoder encoder;
    MediaFormat format;

    public BaseCodification(Renderer renderer, Encoder encoder, MediaFormat format) {
        this.renderer = renderer;
        this.format = format;
        this.encoder = encoder;
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

}

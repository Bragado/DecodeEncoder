package com.example.decoderencoder.library.core.encoder;

import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.core.decoder.Decoder;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.C;

public interface Codification {

    /**
     * Starts encoding in to a surface or buffer
     */
    public void start();

    /**
     * Returns the track type that the {@link Renderer} handles. For example, a video renderer will
     * return {@link C#TRACK_TYPE_VIDEO}, an audio renderer will return {@link C#TRACK_TYPE_AUDIO}, a
     * text renderer will return {@link C#TRACK_TYPE_TEXT}, and so on.
     *
     * @return One of the {@code TRACK_TYPE_*} constants defined in {@link C}.
     */
    public int getTrackType();

    /**
     * Enables the codification to consume from the specified {@link SampleStream}.
     * <p>
     *
     * @param renderer The corresponding renderer from wich the encoder will encode data.
     */
    public void enable(Renderer renderer);

    /**
     * Stops the codification.
     * <p>
     */
    public void stop();

    /**
     * Disable the codification.
     * <p>
     */
    public void disable();

    /**
     * Forces the codification to give up any resources (e.g. media decoders) that it may be holding. If
     * the renderer is not holding any resources, the call is a no-op.
     *
     */
    public void reset();


    boolean feedInputBuffer();

    boolean drainOutputBuffer();



}

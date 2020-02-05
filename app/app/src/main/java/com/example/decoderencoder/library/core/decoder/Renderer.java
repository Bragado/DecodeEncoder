package com.example.decoderencoder.library.core.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import com.example.decoderencoder.library.video.openGL.InputSurface;
import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.C;

import java.nio.ByteBuffer;

public interface Renderer {




    public interface Callback {
        /**
         * Called immediatly after the renderer is ready to start decoding
         */
        public void ready();
    }

    /**
     * Returns the {@link SampleStream} being consumed, or null if the renderer is disabled.
     */
    public SampleStream getStream();


    /**
     * Starts rendering
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
     * Enables the renderer to consume from the specified {@link SampleStream}.
     * <p>
     *
     * @param formats The enabled formats.
     * @param stream The {@link SampleStream} from which the renderer should consume.
     * @param positionUs The player's current position.
     */
    public void enable(Format[] formats, SampleStream stream, long positionUs, long offsetUs);


    /**
     * Returns whether the renderer has read the current {@link SampleStream} to the end.
     * <p>
     */
    public boolean hasReadStreamToEnd();

    /**
     * Stops the renderer.
     * <p>
      */
    public void stop();

    /**
     * Disable the renderer.
     * <p>
     */
    public void disable();

    /**
     * Forces the renderer to give up any resources (e.g. media decoders) that it may be holding. If
     * the renderer is not holding any resources, the call is a no-op.
     *
     */
    public void reset();


    public long getReadingPositionUs();

    /**
     * Sets the decoder to decode the available samples
     * <p>
     * @param decoder
     */
    public void setDecoder(Decoder decoder);

    /**
     * Signals end of stream
     */
    public void signalEndOfStream();

    /**
     * Feeds data into the decoder
     * @return  the pts value of the sample feed to the decoder
     */
    long feedInputBuffer(long stream_highest_pts);

    /**
     * Fetchs data into the decoder
     * @return  false if end of stream, true otherwise
     */
    int drainOutputBuffer();


    /**
     * Called by audio encoders to poll data
     */

    /**
     * Called by the encoder to pool the list of buffer infos
     * @return first buffer info not pooled
     */
    MediaCodec.BufferInfo pollBufferInfo();

    /**
     * Called by the encoder to pool the list of buffer indexs in order to read the decoder output buffer.
     * <p>
     * The encoder is responsible to call {@link Decoder#releaseOutputBuffer} to release the decoder output
     * @return first buffer index not pooled
     */
    int poolBufferIndex();


    /**
     * Called by video encoders to set the input or/and output surfaces
     */

    /**
     * Called by the encoder (Encoder's callback) when the decoder input and output surfaces have been created
     * @param inputSurface
     */
    void SurfaceCreated(InputSurface inputSurface);

    void SurfaceCreated(Surface surface);


    /**
     * Called by all encoders
     */

    /**
     * Gets the decoder associated with this renderer
     * @returns the instanciated decoder or null if it's passthrough
     */
    Decoder getDecoder();

    /**
     * Gets the stream original format
     * @return
     */
    MediaFormat getFormat();


    ByteBuffer pollFrameData();

}

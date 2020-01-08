package com.example.decoderencoder.library.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.example.decoderencoder.library.core.encoder.EncoderBuffer;

import java.nio.ByteBuffer;

public interface MediaMuxer {

    /**
     * Stops muxing data. Should only be called once endOfStream is encoded
     */
    public void stop();

    /**
     * Adds a new track to be muxed, should ideally be called before #start()
     * <p>
     * This call is originally called by the encoder once a new track is encoded
     * @param newFormat the format of the new track
     * @return the index of the track
     */
    public int addTrack(MediaFormat newFormat);

    /**
     * Starts muxing data, should be called once all tracks are added.
     */
    public void start();

    /**
     * Release all kept resources
     */
    public void release();

    /**
     * Evaluates if this muxer can support this specific container
      * @param container the id of the container, the value is declared in #C
     */
    public void sniff(int container);

    /**
     * Writes a given stream parcel to the output
     * @param trackIndex the track index given by addTrack
     * @param byteBuf   the frame data buffer
     * @param offset    the offset inside the frame data buffer
     * @param size      frame data buffer size
     * @param flags     mediacodec flags
     * @param presentationTimeUs pts
     */
    public void writeSampleData(int trackIndex, EncoderBuffer outputBuffer);

    void addConfigBuffer(int trackId, byte[] content, int size);
}

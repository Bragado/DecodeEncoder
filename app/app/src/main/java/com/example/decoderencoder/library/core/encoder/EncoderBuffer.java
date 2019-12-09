package com.example.decoderencoder.library.core.encoder;

import java.nio.ByteBuffer;

public class EncoderBuffer {

    /**
     * This indicates that the (encoded) buffer marked as such contains
     * the data for a key frame.
     *
     * @deprecated Use {@link #BUFFER_FLAG_KEY_FRAME} instead.
     */
    public static final int BUFFER_FLAG_SYNC_FRAME = 1;

    /**
     * This indicates that the (encoded) buffer marked as such contains
     * the data for a key frame.
     */
    public static final int BUFFER_FLAG_KEY_FRAME = 1;

    /**
     * This indicated that the buffer marked as such contains codec
     * initialization / codec specific data instead of media data.
     */
    public static final int BUFFER_FLAG_CODEC_CONFIG = 2;

    /**
     * This signals the end of stream, i.e. no buffers will be available
     * after this, unless of course, {@link #flush} follows.
     */
    public static final int BUFFER_FLAG_END_OF_STREAM = 4;

    /**
     * The buffer's data, or {@code null} if no data has been set.
     */
    public ByteBuffer data;


    /**
     * The start-offset of the data in the buffer.
     */
    public int offset;

    /**
     * The amount of data (in bytes) in the buffer.  If this is {@code 0},
     * the buffer has no data in it and can be discarded.  The only
     * use of a 0-size buffer is to carry the end-of-stream marker.
     */
    public int size;

    /**
     * The presentation timestamp in microseconds for the buffer.
     * This is derived from the presentation timestamp passed in
     * with the corresponding input buffer.  This should be ignored for
     * a 0-sized buffer.
     */
    public long presentationTimeUs;

    /**
     * Buffer flags associated with the buffer.  A combination of
     * {@link #BUFFER_FLAG_KEY_FRAME} and {@link #BUFFER_FLAG_END_OF_STREAM}.
     *
     * <p>Encoded buffers that are key frames are marked with
     * {@link #BUFFER_FLAG_KEY_FRAME}.
     *
     * <p>The last output buffer corresponding to the input buffer
     * marked with {@link #BUFFER_FLAG_END_OF_STREAM} will also be marked
     * with {@link #BUFFER_FLAG_END_OF_STREAM}. In some cases this could
     * be an empty buffer, whose sole purpose is to carry the end-of-stream
     * marker.
     */
    public int flags;


    public EncoderBuffer(ByteBuffer data, int offset, int size, int flags, long presentationTimeUs) {
        this.data = data;
        this.offset = offset;
        this.size = size;
        this.presentationTimeUs = presentationTimeUs;
        this.flags = flags;
    }


}

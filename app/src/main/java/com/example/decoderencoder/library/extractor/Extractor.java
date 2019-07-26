package com.example.decoderencoder.library.extractor;

import java.nio.ByteBuffer;

public interface Extractor {   // necessary to abstract the Extractor to the decoder
    /**
     *  Necessary to config the desired Extractor, called at most once.
     */
    void init();

    /**
     * Retrieve the current encoded sample and store it in the byte buffer
     * starting at the given offset.
     * @param inputBuffer the destination byte buffer
     * @param offset
     * @return the sample size (or -1 if no more samples are available).
     */
    int read(ByteBuffer inputBuffer, int offset);

    /**
     * Advance to the next sample. Returns false if no more sample data
     * is available (end of stream).
     */
    boolean advance();

    /**
     * @return an instance of the media extractor
     */
    Object getInstance();

    /**
     * Sets the data source of the media.
     * @param path
     */
    void setDataSource(Object path);

    /**
     * Evaluates if either a given Extractor instance can deal with a given format
     * @param path
     * @return
     */
    boolean evaluate(Object path);

    /**
     * Notifies the extractor that a seek has occurred.
     * <p>
     * @param position The byte offset in the stream from which data will be provided.
     * @param timeUs The seek time in microseconds.
     */
    void seek(long position, long timeUs);

    /**
     * Releases all kept resources.
     */
    void release();

}

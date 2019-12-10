package com.example.decoderencoder.library.muxer;


import com.example.decoderencoder.library.FormatHolder;
import com.example.decoderencoder.library.core.decoder.DecoderInputBuffer;

/**
 * Muxers will have this interface in order to fetch information
 */
public interface SampleOutput {

    /**
     * Returns whether data is available to be read.
     * @return Whether data is available to be read.
     */
    boolean isReady();


    /**
     * Reads some encoded track from the queue
     * @param data target array
     * @returns The number of bytes read
     */
    int writeData(int trackId, byte[] data, Object info);

}

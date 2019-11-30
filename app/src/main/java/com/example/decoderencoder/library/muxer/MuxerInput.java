package com.example.decoderencoder.library.muxer;

import com.example.decoderencoder.library.core.encoder.EncoderBuffer;
import com.example.decoderencoder.library.extractor.ExtractorInput;
import com.example.decoderencoder.library.util.C;

import java.io.EOFException;
import java.io.IOException;


public interface MuxerInput {

    /**
     * Called to write sample data to the output.
     *
     * @param outputBuffer An {@link EncoderBuffer} from the encoder to read the sample data.
     * @param length The maximum length to read from the input.
     * @return The number of bytes not appended.
     * @throws IOException If an error occurred reading from the input.
     * @throws InterruptedException If the thread was interrupted.
     */
    int sampleData(EncoderBuffer outputBuffer, int length)
            throws IOException, InterruptedException;
}

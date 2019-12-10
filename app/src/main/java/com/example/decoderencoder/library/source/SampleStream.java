package com.example.decoderencoder.library.source;

import java.io.IOException;

import com.example.decoderencoder.library.FormatHolder;
import com.example.decoderencoder.library.core.decoder.DecoderInputBuffer;
import com.example.decoderencoder.library.util.C;

public interface SampleStream {

    /**
     * Returns whether data is available to be read.
     * @return Whether data is available to be read.
     */
    boolean isReady();

    /**
     * Throws an error that's preventing data from being read. Does nothing if no such error exists.
     *
     * @throws IOException The underlying error.
     */
    void maybeThrowError() throws IOException;

    /**
     * Attempts to read from the stream.
     *
     * <p>If the stream has ended then {@link C#BUFFER_FLAG_END_OF_STREAM} flag is set on {@code
     * buffer} and {@link C#RESULT_BUFFER_READ} is returned. Else if no data is available then {@link
     * C#RESULT_NOTHING_READ} is returned. Else if the format of the media is changing or if {@code
     * formatRequired} is set then {@code formatHolder} is populated and {@link C#RESULT_FORMAT_READ}
     * is returned. Else {@code buffer} is populated and {@link C#RESULT_BUFFER_READ} is returned.
     *
     * @param formatHolder A {@link FormatHolder} to populate in the case of reading a format.
     * @param buffer A {@link DecoderInputBuffer} to populate in the case of reading a sample or the
     *     end of the stream. If the end of the stream has been reached, the {@link
     *     C#BUFFER_FLAG_END_OF_STREAM} flag will be set on the buffer. If a {@link
     *     DecoderInputBuffer#isFlagsOnly() flags-only} buffer is passed, then no {@link
     *     DecoderInputBuffer#data} will be read and the read position of the stream will not change,
     *     but the flags of the buffer will be populated.
     * @param formatRequired Whether the caller requires that the format of the stream be read even if
     *     it's not changing. A sample will never be read if set to true, however it is still possible
     *     for the end of stream or nothing to be read.
     * @return The result, which can be {@link C#RESULT_NOTHING_READ}, {@link C#RESULT_FORMAT_READ} or
     *     {@link C#RESULT_BUFFER_READ}.
     */
    int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean formatRequired);

}

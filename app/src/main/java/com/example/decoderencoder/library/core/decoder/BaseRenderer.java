package com.example.decoderencoder.library.core.decoder;

import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.FormatHolder;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.C;

import java.nio.ByteBuffer;

public abstract class BaseRenderer implements Renderer {

    private final int trackType;
    private int index;
    protected Format[] streamFormats;
    private SampleStream stream;
    private long readingPositionUs;
    protected boolean streamIsFinal = false;
    private long streamOffsetUs;
    private Decoder decoder;

    public BaseRenderer(int trackType) {
        this.trackType = trackType;
        readingPositionUs = C.TIME_END_OF_SOURCE;
    }

    // Renderer interface implementation:

    @Override
    public final int getTrackType() {
        return trackType;
    }           // FIXME : trackType is being instanciated incorrectly

    @Override
    public final void enable(Format[] formats, SampleStream stream, long positionUs, long offsetUs)
    {
        replaceStream(formats, stream, offsetUs);
        onEnabled();
    }

    public final void replaceStream(Format[] formats, SampleStream stream, long offsetUs)
    {
        this.stream = stream;
        streamOffsetUs = readingPositionUs = offsetUs;
        streamFormats = formats;
        
    }

    @Override
    public final void start() {
        onStarted();
    }

    @Override
    public final long getReadingPositionUs() {
        return readingPositionUs;
    }

    @Override
    public final void stop() {
        onStopped();
    }

    @Override
    public final void disable() {
        stream = null;
        streamFormats = null;
        onDisabled();
    }

    @Override
    public final void reset() {
        onReset();
    }

    @Override
    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public void signalEndOfStream() {
        this.streamIsFinal = true;
    }

    @Override
    public ByteBuffer pollFrameData() { return null; }

    // Methods to be overridden by subclasses.


    /**
     * Called when the renderer is enabled.
     * <p>
     * The default implementation is a no-op.
     */
    protected void onEnabled() {
        // Do nothing.
    }

    /**
     * Called when the renderer is started.
     * <p>
     * The default implementation is a no-op.
     *
     */
    protected abstract void onStarted();


    /**
     * Called when the renderer is stopped.
     * <p>
     * The default implementation is a no-op.
     *
     */
    protected void onStopped()  {
        // Do nothing.
    }

    /**
     * Called when the renderer is disabled.
     * <p>
     * The default implementation is a no-op.
     */
    protected void onDisabled() {
        // Do nothing.
    }

    /**
     * Called when the renderer is reset.
     *
     * <p>The default implementation is a no-op.
     */
    protected void onReset() {
        // Do nothing.
    }


    /**
     * Reads from the enabled upstream source. If the upstream source has been read to the end then
     * {@link C#RESULT_BUFFER_READ} is only returned if {@link #setCurrentStreamFinal()} has been
     * called. {@link C#RESULT_NOTHING_READ} is returned otherwise.
     *
     * @param formatHolder A {@link FormatHolder} to populate in the case of reading a format.
     * @param buffer A {@link DecoderInputBuffer} to populate in the case of reading a sample or the
     *     end of the stream. If the end of the stream has been reached, the
     *     {@link C#BUFFER_FLAG_END_OF_STREAM} flag will be set on the buffer.
     * @param formatRequired Whether the caller requires that the format of the stream be read even if
     *     it's not changing. A sample will never be read if set to true, however it is still possible
     *     for the end of stream or nothing to be read.
     * @return The result, which can be {@link C#RESULT_NOTHING_READ}, {@link C#RESULT_FORMAT_READ} or
     *     {@link C#RESULT_BUFFER_READ}.
     */
    protected final int readSource(FormatHolder formatHolder, DecoderInputBuffer buffer,
                                   boolean formatRequired) {
        int result = stream.readData(formatHolder, buffer, formatRequired);
        if (result == C.RESULT_BUFFER_READ) {
            if (buffer.isEndOfStream()) {
                readingPositionUs = C.TIME_END_OF_SOURCE;
                return streamIsFinal ? C.RESULT_BUFFER_READ : C.RESULT_NOTHING_READ;
            }
            buffer.timeUs += streamOffsetUs;
            readingPositionUs = Math.max(readingPositionUs, buffer.timeUs);
        } else if (result == C.RESULT_FORMAT_READ) {
            Format format = formatHolder.format;
            if (format.subsampleOffsetUs != Format.OFFSET_SAMPLE_RELATIVE) {
                format = format.copyWithSubsampleOffsetUs(format.subsampleOffsetUs + streamOffsetUs);
                formatHolder.format = format;
            }
        }
        return result;
    }







}

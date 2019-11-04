package com.example.decoderencoder.library.util;

import java.io.IOException;

/**
 * A seeker that supports seeking within a stream by searching for the target frame using binary
 * search.
 *
 * <p>This seeker operates on a stream that contains multiple frames (or samples). Each frame is
 * associated with some kind of timestamps, such as stream time, or frame indices. Given a target
 * seek time, the seeker will find the corresponding target timestamp, and perform a search
 * operation within the stream to identify the target frame and return the byte position in the
 * stream of the target frame.
 */
public class BinarySearchSeeker {

    protected final BinarySearchSeekMap seekMap;
    protected SeekOperationParams seekOperationParams;
    /**
     * Constructs an instance.
     *
     * @param seekTimestampConverter The {@link SeekTimestampConverter} that converts seek time in
     *     stream time into target timestamp.
     * @param timestampSeeker A {@link TimestampSeeker} that will be used to search for timestamps
     *     within the stream.
     * @param durationUs The duration of the stream in microseconds.
     * @param floorTimePosition The minimum timestamp value (inclusive) in the stream.
     * @param ceilingTimePosition The minimum timestamp value (exclusive) in the stream.
     * @param floorBytePosition The starting position of the frame with minimum timestamp value
     *     (inclusive) in the stream.
     * @param ceilingBytePosition The position after the frame with maximum timestamp value in the
     *     stream.
     * @param approxBytesPerFrame Approximated bytes per frame.
     * @param minimumSearchRange The minimum byte range that this binary seeker will operate on. If
     *     the remaining search range is smaller than this value, the search will stop, and the seeker
     *     will return the position at the floor of the range as the result.
     */
    @SuppressWarnings("initialization")
    protected BinarySearchSeeker(
            SeekTimestampConverter seekTimestampConverter,
            TimestampSeeker timestampSeeker,
            long durationUs,
            long floorTimePosition,
            long ceilingTimePosition,
            long floorBytePosition,
            long ceilingBytePosition,
            long approxBytesPerFrame,
            int minimumSearchRange) {
        this.timestampSeeker = timestampSeeker;
        this.minimumSearchRange = minimumSearchRange;
        this.seekMap =
                new BinarySearchSeekMap(
                        seekTimestampConverter,
                        durationUs,
                        floorTimePosition,
                        ceilingTimePosition,
                        floorBytePosition,
                        ceilingBytePosition,
                        approxBytesPerFrame);
    }

    /** Returns the seek map for the stream. */
    public final SeekMap getSeekMap() {
        return seekMap;
    }

    /** Returns whether the last operation set by {@link #setSeekTargetUs(long)} is still pending. */
    public final boolean isSeeking() {
        return seekOperationParams != null;
    }

    /**
     * A {@link SeekMap} implementation that returns the estimated byte location from {@link
     * SeekOperationParams#calculateNextSearchBytePosition(long, long, long, long, long, long)} for
     * each {@link #getSeekPoints(long)} query.
     */
    public static class BinarySearchSeekMap implements SeekMap {
        private final SeekTimestampConverter seekTimestampConverter;
        private final long durationUs;
        private final long floorTimePosition;
        private final long ceilingTimePosition;
        private final long floorBytePosition;
        private final long ceilingBytePosition;
        private final long approxBytesPerFrame;

        /** Constructs a new instance of this seek map. */
        public BinarySearchSeekMap(
                SeekTimestampConverter seekTimestampConverter,
                long durationUs,
                long floorTimePosition,
                long ceilingTimePosition,
                long floorBytePosition,
                long ceilingBytePosition,
                long approxBytesPerFrame) {
            this.seekTimestampConverter = seekTimestampConverter;
            this.durationUs = durationUs;
            this.floorTimePosition = floorTimePosition;
            this.ceilingTimePosition = ceilingTimePosition;
            this.floorBytePosition = floorBytePosition;
            this.ceilingBytePosition = ceilingBytePosition;
            this.approxBytesPerFrame = approxBytesPerFrame;
        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        public SeekPoints getSeekPoints(long timeUs) {
            long nextSearchPosition =
                    SeekOperationParams.calculateNextSearchBytePosition(
                            /* targetTimePosition= */ seekTimestampConverter.timeUsToTargetTime(timeUs),
                            /* floorTimePosition= */ floorTimePosition,
                            /* ceilingTimePosition= */ ceilingTimePosition,
                            /* floorBytePosition= */ floorBytePosition,
                            /* ceilingBytePosition= */ ceilingBytePosition,
                            /* approxBytesPerFrame= */ approxBytesPerFrame);
            return new SeekPoints(new SeekPoint(timeUs, nextSearchPosition));
        }

        @Override
        public long getDurationUs() {
            return durationUs;
        }

        /** @see SeekTimestampConverter#timeUsToTargetTime(long) */
        public long timeUsToTargetTime(long timeUs) {
            return seekTimestampConverter.timeUsToTargetTime(timeUs);
        }
    }

    /**
     * Continues to handle the pending seek operation. Returns one of the {@code RESULT_} values from
     * {@link Extractor}.
     *
     * @param input The {@link ExtractorInputBuffer} from which data should be read.
     * @param seekPositionHolder If {@link Extractor#RESULT_SEEK} is returned, this holder is updated
     *     to hold the position of the required seek.
     * @param outputFrameHolder If {@link Extractor#RESULT_CONTINUE} is returned, this holder may be
     *     updated to hold the extracted frame that contains the target sample. The caller needs to
     *     check the byte buffer limit to see if an extracted frame is available.
     * @return One of the {@code RESULT_} values defined in {@link Extractor}.
     * @throws IOException If an error occurred reading from the input.
     * @throws InterruptedException If the thread was interrupted.
     */
    public int handlePendingSeek(
            ExtractorInputBuffer input, PositionHolder seekPositionHolder, OutputFrameHolder outputFrameHolder)
            throws InterruptedException, IOException {
        TimestampSeeker timestampSeeker = Assertions.checkNotNull(this.timestampSeeker);
        while (true) {
            SeekOperationParams seekOperationParams = Assertions.checkNotNull(this.seekOperationParams);
            long floorPosition = seekOperationParams.getFloorBytePosition();
            long ceilingPosition = seekOperationParams.getCeilingBytePosition();
            long searchPosition = seekOperationParams.getNextSearchBytePosition();

            if (ceilingPosition - floorPosition <= minimumSearchRange) {
                // The seeking range is too small, so we can just continue from the floor position.
                markSeekOperationFinished(/* foundTargetFrame= */ false, floorPosition);
                return seekToPosition(input, floorPosition, seekPositionHolder);
            }
            if (!skipInputUntilPosition(input, searchPosition)) {
                return seekToPosition(input, searchPosition, seekPositionHolder);
            }

            input.resetPeekPosition();
            TimestampSearchResult timestampSearchResult =
                    timestampSeeker.searchForTimestamp(
                            input, seekOperationParams.getTargetTimePosition(), outputFrameHolder);

            switch (timestampSearchResult.type) {
                case TimestampSearchResult.TYPE_POSITION_OVERESTIMATED:
                    seekOperationParams.updateSeekCeiling(
                            timestampSearchResult.timestampToUpdate, timestampSearchResult.bytePositionToUpdate);
                    break;
                case TimestampSearchResult.TYPE_POSITION_UNDERESTIMATED:
                    seekOperationParams.updateSeekFloor(
                            timestampSearchResult.timestampToUpdate, timestampSearchResult.bytePositionToUpdate);
                    break;
                case TimestampSearchResult.TYPE_TARGET_TIMESTAMP_FOUND:
                    markSeekOperationFinished(
                            /* foundTargetFrame= */ true, timestampSearchResult.bytePositionToUpdate);
                    skipInputUntilPosition(input, timestampSearchResult.bytePositionToUpdate);
                    return seekToPosition(
                            input, timestampSearchResult.bytePositionToUpdate, seekPositionHolder);
                case TimestampSearchResult.TYPE_NO_TIMESTAMP:
                    // We can't find any timestamp in the search range from the search position.
                    // Give up, and just continue reading from the last search position in this case.
                    markSeekOperationFinished(/* foundTargetFrame= */ false, searchPosition);
                    return seekToPosition(input, searchPosition, seekPositionHolder);
                default:
                    throw new IllegalStateException("Invalid case");
            }
        }
    }
}

package com.example.decoderencoder.library.source;

import java.io.IOException;

public interface Media {
    /**
     * Returns the {@link TrackGroup}s exposed by the period.
     *
     * <p>This method is only called after the period has been prepared.
     *
     * @return The {@link TrackGroup}s.
     */
    TrackGroupArray getTrackGroups();

    interface Callback  {

        /**
         * Called when preparation completes. i.e. When all tracks are known
         *
         * @param source The prepared {@link MediaSource}.
         */
        void onPrepared(MediaSource source);

        /**
         * Called by the loader to indicate that it wishes for its {@link MediaSource#continueLoading(long)} method
         * to be called when it can continue to load data. Called on the playback thread.
         */
        void onContinueLoadingRequested(MediaSource source);
    }

    /**
     * Attempts to continue loading.
     *
     * <p>This method may be called both during and after the period has been prepared.
     *
     * <p>A period may call {@link Callback#onContinueLoadingRequested(MediaSource)} on the
     * {@link Callback} passed to {@link #prepare(Callback, long)} to request that this method be
     * called when the period is permitted to continue loading data. A period may do this both during
     * and after preparation.
     *
     * @param positionUs The current playback position in microseconds. If playback of this period has
     *     not yet started, the value will be the starting position in this period minus the duration
     *     of any media in previous periods still to be played.
     * @return True if progress was made, meaning that {@link #getNextLoadPositionUs()} will return a
     *     different value than prior to the call. False otherwise.
     */
    boolean continueLoading(long positionUs);


    /**
     * Prepares this media period asynchronously.
     *
     * <p>{@code callback.onPrepared} is called when preparation completes. If preparation fails,
     * {@link #maybeThrowPrepareError()} will throw an {@link IOException}.
     *
     *
     * @param callback Callback to receive updates from this period, including being notified when
     *     preparation completes.
     * @param positionUs The expected starting position, in microseconds.
     */
    void prepare(Callback callback, long positionUs);


    /**
     * Throws an error that's preventing the media source from becoming prepared. Does nothing if no such
     * error exists.
     *
     * <p>This method is only called before the media source has completed preparation.
     *
     * @throws IOException The underlying error.
     */
    void maybeThrowPrepareError() throws IOException;


    /**
     * Gets all the streams of a given media content
     *
     * <p>Should be called after receiving {@link Callback#onContinueLoadingRequested}
     *
     */
     SampleStream[] getSampleStreams();

}

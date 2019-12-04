package com.example.decoderencoder.library.core;

import android.media.MediaFormat;
import android.net.Uri;

import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.network.DataSource;
import com.example.decoderencoder.library.source.MediaSource;
import com.example.decoderencoder.library.source.TrackGroup;
import com.example.decoderencoder.library.source.TrackGroupArray;
import com.example.decoderencoder.library.util.Stats;

public interface Transcoder {

    enum State {
        INICIALIZED,
        PREPARED,
        RUNNING,
        STOPED
    }

    enum LoaderStatus {
        BLOCKED,
        RUNNING
    }

    public interface Callback {         // callbacks to UI THREAD
        /**
         * A callback performed by an instance of the transcoder with all available tracks
         * <p>
         * After this call, the transcoder instance expects a selectTrack() call to select the tracks
         * @param preparedState all tracks available
         */
        public void onPrepared(Transcoder transcoder, MediaSource.PreparedState preparedState);

        public void onTracksSelected(Transcoder transcoder);

        /**
         * A callback performed when requested by {@link #getStats}
         * @param stats
         */
        public void updateStats(Stats stats);

    }

    /**
     * Sets the transcoder dataSource, should be called immediately after the constructor
     * <p>
     * The caller should then wait for {@link Callback#onPrepared} callback from the transcdoer
     * @param dataSource
     * @return
     */
    public boolean setDataSource(DataSource dataSource);

    /**
     * Sets the transcoder MediaMuxer , should be called before start()
     * @param mediaMuxer
     * @return
     */
    public boolean setOutputSource(MediaMuxer mediaMuxer);

    /**
     * Tells the transcoder to prepare itself to perform all necessary operations to go to ready state
     * <p>
     * Should called after {@link #setDataSource}
     *
     * @return true can proceed (if mediaSource is set), false otherwise
     */
    public boolean prepare();

    /**
     * Starts transcoding the
     * @return
     */
    public boolean startTranscoder();

    /**
     * Sets the tracks to be transcoded, must be called before {@link #startTranscoder()}
     * @param selectedTracks an ordered array (by id) of the selected tracks
     * @param formats the desired output format,  format[i] refers to track selectedTracks.get(i)
     * @return false if it's not possible to select a track (the transcoder does not yet have information about the available tracks), true otherwise
     */
    public boolean setSelectedTracks(TrackGroup[] selectedTracks, MediaFormat[] formats);


    /**
     * Stops the transconder and releases all kept resources
     * @return
     */
    public boolean stopTranscoder();

    /*
     * Requests the current transcoder stats
     */
    public void getStats();






}

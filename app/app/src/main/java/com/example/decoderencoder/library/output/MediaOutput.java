package com.example.decoderencoder.library.output;

import android.media.MediaFormat;
import android.net.Uri;
import android.os.Handler;

import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.muxer.SampleOutput;

public interface MediaOutput {


    void prepare(Callback callback, Uri uri, Handler transcoderHandler);

    void prepare(Callback callback, String uri, Handler transcoderHandler);

    /**
     * Prepares all tracks to be muxed ordered following the formats array. Must be called before newTrackDiscovered;
     * It is up to the inteface's client to decide the order of the media formats,
     * ideally should be: 1º video, 2º to N-Mº audio, N-M+1 º to Nº subtitles (N total, M number of subs)
     * @param formats contains the format of all tracks
     */
    void prepareTracks(MediaFormat[] formats);

    void maybeStartMuxer();

    void stopMuxer();

    void release();

    interface Callback {

        public void onPrepared(MediaOutput mediaOutput);

        public void onContinueLoading(MediaOutput mediaOutput);
    }

    MuxerInput newTrackDiscovered(MediaFormat trackFormat);

    void setNumOfStreams(int streams);

    long getCurrentMaxPts();

    long getCurrentMinPts();


}

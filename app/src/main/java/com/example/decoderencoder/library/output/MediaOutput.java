package com.example.decoderencoder.library.output;

import android.media.MediaFormat;
import android.net.Uri;

import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.muxer.SampleOutput;

public interface MediaOutput {


    void prepare(Callback callback, Uri uri);

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

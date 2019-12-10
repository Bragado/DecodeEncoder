package com.example.decoderencoder.library.output;

import android.media.MediaFormat;

import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.muxer.SampleOutput;

public interface MediaOutput {


    void prepare(MediaMuxer mediaMuxer);

    interface Callback {

        public void onPrepared(MediaOutput mediaOutput);

        public void onContinueLoading(MediaOutput mediaOutput);
    }

    MuxerInput newTrackDiscovered(MediaFormat trackFormat);

    long getCurrentMaxPts();

    long getCurrentMinPts();


}

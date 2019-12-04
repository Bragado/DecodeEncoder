package com.example.decoderencoder.library.output;

import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.muxer.SampleOutput;

public interface MediaOutput {


    void prepare(MediaMuxer mediaMuxer);

    boolean continueLoading(long positionUs);

    interface Callback {

        public void onPrepared(MediaOutput mediaOutput);

        public void onContinueLoading(MediaOutput mediaOutput);
    }


    /**
     * Gets all the streams of a given media content
     *
     * <p>Should be called after receiving {@link Callback#onContinueLoadingRequested}
     *
     */
    MuxerInput[] getInputBuffers();






}

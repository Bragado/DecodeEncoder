package com.example.decoderencoder.library.core;


import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.decoder.DefaultRenderFactory;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.core.encoder.Codification;
import com.example.decoderencoder.library.core.encoder.MediaCodecCodification;
import com.example.decoderencoder.library.extractor.DefaultExtractorsFactory;
import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.network.Allocator;
import com.example.decoderencoder.library.network.DataSource;
import com.example.decoderencoder.library.network.DefaultAllocator;
import com.example.decoderencoder.library.network.DefaultLoadErrorHandlingPolicy;
import com.example.decoderencoder.library.network.LoadErrorHandlingPolicy;
import com.example.decoderencoder.library.output.MediaOutput;
import com.example.decoderencoder.library.source.Media;
import com.example.decoderencoder.library.source.MediaSource;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.source.TrackGroup;
import com.example.decoderencoder.library.source.TrackGroupArray;
import com.example.decoderencoder.library.util.C;
import com.example.decoderencoder.library.util.Log;

import java.util.Arrays;

/**
 * This class is responsible to create and manage all
 */
public class DefaultTranscoder  extends HandlerThread implements Transcoder, RenderersFactory.Callback {

    public static final String TAG = "DEFAULT_TRANSCODER";


    /*  Main Thread */
    private Transcoder.State currentState = Transcoder.State.INICIALIZED;
    private DataSource dataSource = null;
    private MediaMuxer mediaMuxer = null;
    private Uri inputUri = null;
    private Uri outputUri = null;
    private Transcoder.Callback callback;

    /*  Shared variables */
    Allocator allocator;
    LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    MediaSource mediaSource;
    MediaManagement mediaManagement;
    MediaOutput mediaOutput;
    OutputManagement outputManagement;

    /* DefaultTranscoder Variables */
    Handler transcoderHandler;
    Handler uiHandler;
    MediaSource.PreparedState preparedState = null;
    SampleStream[] sampleStreams = null;
    Renderer[] renderers = null;
    Codification[] codifications = null;
    RenderersFactory defaultRenderFactory = null;
    CodificationFactory defaultCodificationFactory = null;
    MediaFormat[] formats = null;

    /* Control Variables */
    boolean tracksSelected = false;
    boolean tracksSelectedUpdated = false;
    boolean prepareWhenReady = false;
    boolean transcoderRunning = false;

    /* Encoders/Decoders */
    DecodeEncodeStreams decodeEncodeStreams;



    public DefaultTranscoder (Transcoder.Callback callback, String inputPath, String outputPath) {     // Thread : UI
        super("DefaultTranscoder");
        this.allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
        this.loadErrorHandlingPolicy = new DefaultLoadErrorHandlingPolicy();
        this.inputUri = Uri.parse(inputPath);
        this.outputUri = Uri.parse(outputPath);
        this.uiHandler = new Handler();
        this.callback = callback;

    }

    @Override
    protected void onLooperPrepared() {  // Thread : DefaultTranscoder
        transcoderHandler = new Handler();
        mediaSource = new MediaSource(inputUri, dataSource, new DefaultExtractorsFactory().createExtractors(), allocator, MediaSource.DEFAULT_LOADING_CHECK_INTERVAL_BYTES, loadErrorHandlingPolicy);
        if(prepareWhenReady) {
            onPrepare();
        }
    }


    @Override
    public boolean setDataSource(DataSource dataSource) {    // Thread : UI
        this.dataSource = dataSource;
        return true;
    }

    @Override
    public boolean setOutputSource(MediaOutput mediaOutput) {  // Thread : UI
        //this.mediaMuxer = mediaMuxer;
        /*TODO:
        *
        * */
        this.mediaOutput = mediaOutput;
        mediaOutput.prepare(outputManagement, outputUri);
        return true;
    }

    @Override
    public boolean prepare() {   // Thread : UI
        if(mediaSource == null) {
            prepareWhenReady = true;
            return false;
        }
        transcoderHandler.post(() -> {
            onPrepare();
        });
        return true;
    }

    @Override
    public boolean startTranscoder() {  // Thread : UI
        if(!tracksSelected || this.mediaOutput == null)
            return false;
        else {
            transcoderHandler.post(() -> {
                int i = 0;
                for (Codification codification : codifications) {
                    i++;
                    if(codification != null)
                        codification.start();
                }
                for (Renderer renderer : renderers) {
                    if(renderer != null)
                        renderer.start();
                }
                reallyStartTranscoding();
            });
            return true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean setSelectedTracks(TrackGroup[] selectedTracks, TrackGroup[] discardedTracks, MediaFormat[] formats) {  // Thread : UI
        if(this.preparedState == null)
            return false;
        TrackGroupArray available_tracks = this.preparedState.tracks;
        int[] tracksToDiscard = new int[0];     // tracks to be discarded
        this.formats = formats;
        int j = 0;
        int k = 0;
        for(int i = 0; i < available_tracks.length; i++) {
            if(selectedTracks.length > j && selectedTracks[j].getFormat(0).id.equals(available_tracks.get(i).getFormat(0).id)) {    // which tracks are going to be transcoded
                this.preparedState.trackEnabledStates[i] = MediaSource.PreparedState.TRACKSTATE.SELECTED;
                j++;
            }else if(discardedTracks.length > k && discardedTracks[k].getFormat(0).id.equals(available_tracks.get(i).getFormat(0).id)) {    // which tracks are going to be discarded
                this.preparedState.trackEnabledStates[i] = MediaSource.PreparedState.TRACKSTATE.DISCARD;
                tracksToDiscard = Arrays.copyOf(tracksToDiscard, tracksToDiscard.length + 1);
                tracksToDiscard[tracksToDiscard.length-1] = Integer.parseInt(available_tracks.get(i).getFormat(0).id.split("/")[1]);
                k++;
            }else {         // which tracks are going to be passthrough
                this.preparedState.trackEnabledStates[i] = MediaSource.PreparedState.TRACKSTATE.PASSTROUGH;
            }
        }
        onSelectedTracks(tracksToDiscard);
        return true;
    }


    @Override
    public boolean stopTranscoder() {   // Thread : UI
        return false;
    }

    @Override
    public void getStats() {    // Thread : UI

    }

    // Internals
    /**
     *
     */

    public void onPrepare() {
        DefaultTranscoder.this.mediaManagement = new MediaManagement();
        DefaultTranscoder.this.outputManagement = new OutputManagement();
        mediaSource.prepare(DefaultTranscoder.this.mediaManagement,0);   // Thread : DefaultTranscoder
    }

    private void reallyStartTranscoding() {
        transcoderRunning = true;
        readyToStart();
        mediaSource.continueLoading(0);
    }


    /**
     * Inicialize the renderers and the encoders
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void onSelectedTracks(int[] tracksToDiscard) {        // Thread : UI
        tracksSelected = true;
        transcoderHandler.post(() -> {
            if(renderers == null) {
                /* 0. Discard tracks */
                mediaSource.discardTracks(true, tracksToDiscard);

                /* 1. Create Factories */
                DefaultTranscoder.this.defaultCodificationFactory = new DefaultCodificationFactory();
                DefaultTranscoder.this.defaultRenderFactory = new DefaultRenderFactory(DefaultTranscoder.this);

                /*2. Create Renderers */
                DefaultTranscoder.this.renderers = defaultRenderFactory.createRenderers(sampleStreams, preparedState);

                /* Create Codifications */
                DefaultTranscoder.this.codifications = defaultCodificationFactory.createCodification(DefaultTranscoder.this.formats, preparedState, DefaultTranscoder.this.renderers, mediaOutput);

                 /* Tell the mediaOutput how many streams there is*/
                 mediaOutput.setNumOfStreams(this.preparedState.tracks.length - tracksToDiscard.length);

            }else {         // TODO: change transcoding languages onDemand
                // 1. Evaluate which renderers will be maintained
                // 2. Evaluate which renderers can be reused
                // 3. Create the rest of the renderers

            }
            uiHandler.post(()-> {
                callback.onTracksSelected(DefaultTranscoder.this);
            });
        });

    }


    public void readyToStart() {
        /* Actually start feeding data to decoders/encoders */
        decodeEncodeStreams = new DecodeEncodeStreams("DecodeEncodeStreams", transcoderHandler, renderers, codifications);
        decodeEncodeStreams.start();
    }


    final class MediaManagement implements Media.Callback {         // Thread : DefaultTranscoder

        @Override
        public void onPrepared(MediaSource source) {        // talk with the UI thread to
            Log.d(TAG, "onPrepared");
            DefaultTranscoder.this.preparedState = source.getPreparedState();
            DefaultTranscoder.this.sampleStreams = source.getSampleStreams();
            uiHandler.post(() -> { callback.onPrepared(DefaultTranscoder.this, DefaultTranscoder.this.preparedState); });
        }

        @Override
        public void onContinueLoadingRequested(MediaSource source) {        // TODO
            if(transcoderRunning && allocator.getTotalBytesAllocated() < 42833536)
                source.continueLoading(0);

        }
    }

    final class OutputManagement implements MediaOutput.Callback {

        @Override
        public void onPrepared(MediaOutput mediaOutput) {

        }

        @Override
        public void onContinueLoading(MediaOutput mediaOutput) {

        }
    }


    final class DecodeEncodeStreams extends HandlerThread {

        Handler thisHandler;
        final Handler transcoderHandler;
        final Renderer[] renderers;
        final Codification[] codifications;

        boolean canceled;
        boolean isReady = false;

        public DecodeEncodeStreams(String name,
                            Handler transcoderHandler,
                            Renderer[] renderer,
                            Codification[] codification) {
            super(name);
            this.transcoderHandler = transcoderHandler;
            this.renderers = renderer;
            this.codifications = codification;
        }

        @Override
        protected void onLooperPrepared() {
            thisHandler = new Handler();
            isReady = true;
            startTranscoding();
        }

        /**
         * Tries to replace the sample stream for decoding/encoding. This method should be called when a sampleStream is changed and the renderer
         * is now free. Instead of creating a new renderer we try to check if it can handle any new selected sample stream
         * @param sampleStreams the sample streams without a renderer assigned
         * @return SampleStream that the renderer can handle
         */
        public SampleStream replaceSampleStream(SampleStream[] sampleStreams) {
             return null;
        }

        public void startTranscoding() {

            if (!isReady)
                return;
            boolean nothingToRead = false;
            /*  Basic Idea:
             *   1. Drain encoder
             *   2. Feed Decoder
             *   3. Drain decoder
             *   4. Feed Encoder
             */
            long counter = 0;
            while (counter++ < 15000) {
                for (int i = 0; i < renderers.length; i++) {
                    codifications[i].drainOutputBuffer();
                    codifications[i].feedInputBuffer();
                    renderers[i].drainOutputBuffer();
                    renderers[i].feedInputBuffer();
                    if(transcoderRunning && allocator.getTotalBytesAllocated() < 42833536)
                        DefaultTranscoder.this.mediaSource.continueLoading(0);

                }
            }

            for (int i = 0; i < renderers.length; i++) {
                renderers[i].signalEndOfStream();
            }

            while (counter++ < 15030) {
                for (int i = 0; i < renderers.length; i++) {
                    codifications[i].drainOutputBuffer();
                    codifications[i].feedInputBuffer();
                    renderers[i].drainOutputBuffer();
                    nothingToRead &= !(renderers[i].feedInputBuffer());
                }
            }
            mediaOutput.stopMuxer();
        }

        public void stopTranscoding() {
            canceled = true;
        }

        public void release() {
            quit();
        }


    }


}

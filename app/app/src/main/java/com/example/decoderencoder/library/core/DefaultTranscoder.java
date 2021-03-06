package com.example.decoderencoder.library.core;


import android.annotation.TargetApi;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.MainActivity;
import com.example.decoderencoder.library.core.decoder.DefaultRenderFactory;
import com.example.decoderencoder.library.core.decoder.EmptyRenderer;
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
import com.example.decoderencoder.library.util.Stats;

import java.util.Arrays;

/**
 * This class is responsible to create and manage all
 */
public class DefaultTranscoder  extends HandlerThread implements Transcoder, RenderersFactory.Callback {

    public static final String TAG = "DEFAULT_TRANSCODER";
    private static final int BUFFER_MAX_SIZE = 82833536;        // Maximum bytes in memory
    private static final int SAMPLE_PARSED = 1;
    private static final int NO_SAMPLE_OR_END_OF_STREAM = 0;

    /*  Main Thread */
    private Transcoder.State currentState = Transcoder.State.INICIALIZED;
    private DataSource dataSource = null;
    private MediaMuxer mediaMuxer = null;
    private Uri inputUri = null;
    private String outputUri = null;
    private Transcoder.Callback callback;

    /*  Shared variables */
    Allocator allocator;
    LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    MediaSource mediaSource;
    MediaManagement mediaManagement;
    MediaOutput mediaOutput = null;
    OutputManagement outputManagement;

    /* DefaultTranscoder Variables */
    Handler transcoderHandler = null;
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
    Boolean outputPrepared = false;

    /* Encoders/Decoders */
    DecodeEncodeStreams decodeEncodeStreams;



    /* Testing */
    float bufferCapacity = 0.0f;
    long numOfEncodedFrames = 0;
    long numOfDecodedFrames = 0;
    long timeElapsed = 0;


    public DefaultTranscoder (Transcoder.Callback callback, String inputPath, String outputPath) {     // Thread : UI
        super("DefaultTranscoder");
        this.allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
        this.loadErrorHandlingPolicy = new DefaultLoadErrorHandlingPolicy();
        this.inputUri = Uri.parse(inputPath);
        this.outputUri = outputPath;
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
        synchronized (outputPrepared) {
            if(transcoderHandler != null && !outputPrepared) {
                outputPrepared = true;
                mediaOutput.prepare(outputManagement, outputUri, transcoderHandler);
            }
        }
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
        onSelectedTracks(tracksToDiscard, formats);
        return true;
    }


    @Override
    public boolean stopTranscoder() {   // Thread : UI
        if(decodeEncodeStreams != null)
            decodeEncodeStreams.stopTranscoding();
        decodeEncodeStreams = null;
        return true;
    }

    @Override
    public Stats getStats() {    // Thread : UI
        float bufOccupancy = (float)allocator.getTotalBytesAllocated()/BUFFER_MAX_SIZE;
        return new Stats(bufOccupancy > 1.0f ? 1.0f : 1.0f - bufOccupancy,
                numOfEncodedFrames,
                numOfDecodedFrames,
                System.currentTimeMillis() - timeElapsed);
    }

    @Override
    public void release() {
        // release mediasource
    }

    // Internals


    public void releaseRenderersAndCodifications() {
        for(Renderer renderer : renderers)
            renderer.stop();
        for(Codification codification : codifications)
            codification.stop();
    }

    /**
     * Starts all renders and codifications
     */
    public void initCodecs() {
        for (Codification codification : codifications) {
            if(codification != null)
                codification.start();
        }
        for (Renderer renderer : renderers) {
            if(renderer != null)
                renderer.start();
        }
    }

    /**
     * Inicializes mediaSource and mediaOutput by calling its prepare() method. Since mediaOutput requires
     * DefaultTranscoder handler to process output, we must check if the handler is already inicialized, if it
     * is not, onLooperPrepared() will handle the mediaOutput.prepare call
     */
    public void onPrepare() {
        DefaultTranscoder.this.mediaManagement = new MediaManagement();
        DefaultTranscoder.this.outputManagement = new OutputManagement();
        mediaSource.prepare(DefaultTranscoder.this.mediaManagement,0);   // Thread : DefaultTranscoder
        synchronized (outputPrepared) {
            if(mediaOutput != null && !outputPrepared ) {
                outputPrepared = true;
                mediaOutput.prepare(outputManagement, outputUri, transcoderHandler);
            }
        }
    }


    private void reallyStartTranscoding() {
        transcoderRunning = true;
        readyToStart();
        mediaSource.continueLoading(0);
    }


    /**
     * Inicialize the renderers and the encoders
     */

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void onSelectedTracks(int[] tracksToDiscard, MediaFormat[] newFormats) {        // Thread : UI
        tracksSelected = true;
        transcoderHandler.post(() -> {
            if(renderers == null) {
                /* 0. Discard tracks */
                mediaSource.discardTracks(true, tracksToDiscard);

                /* 1. Create Factories */
                DefaultTranscoder.this.defaultCodificationFactory = new DefaultCodificationFactory();
                DefaultTranscoder.this.defaultRenderFactory = new DefaultRenderFactory(DefaultTranscoder.this);

                /*2. Create Renderers */
                DefaultTranscoder.this.renderers = defaultRenderFactory.createRenderers(sampleStreams, preparedState, DefaultTranscoder.this.formats);

                /* Create Codifications */
                DefaultTranscoder.this.codifications = defaultCodificationFactory.createCodification(DefaultTranscoder.this.formats, preparedState, DefaultTranscoder.this.renderers, mediaOutput);

                 /* Tell the mediaOutput how many streams there is*/
                 mediaOutput.setNumOfStreams(this.preparedState.tracks.length - tracksToDiscard.length);

                 MediaFormat[] mediaFormats = new MediaFormat[this.preparedState.tracks.length - tracksToDiscard.length];
                 int j, k;
                 j = k = 0;
                 for(int i = 0; i < preparedState.tracks.length; i++) {
                     if(preparedState.trackEnabledStates[i] == MediaSource.PreparedState.TRACKSTATE.PASSTROUGH) {
                         mediaFormats[j++] = EmptyRenderer.getFormat(preparedState.tracks.get(i).getFormat(0));
                     }else if(preparedState.trackEnabledStates[i] == MediaSource.PreparedState.TRACKSTATE.SELECTED) {
                         mediaFormats[j++] = newFormats[k++];
                     }
                 }
                 mediaOutput.prepareTracks(mediaFormats);

            }else {         // TODO: support update the tracks that are inteded to be transcoded
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

        /* Called by MediaSource when all tracks have been discovered */
        @Override
        public void onPrepared(MediaSource source) {        // talk with the UI thread to
            Log.d(TAG, "onPrepared");
            DefaultTranscoder.this.preparedState = source.getPreparedState();
            DefaultTranscoder.this.sampleStreams = source.getSampleStreams();
            uiHandler.post(() -> { callback.onPrepared(DefaultTranscoder.this, DefaultTranscoder.this.preparedState); });
        }

        /* Tells MediaSource to continue fetching data from the source */
        @Override
        public void onContinueLoadingRequested(MediaSource source) {        // TODO
            if(transcoderRunning && allocator.getTotalBytesAllocated() < BUFFER_MAX_SIZE)
                source.continueLoading(0);
            else if(!transcoderRunning)             // too high bitrate??
                source.continueLoading(0);

        }
    }

    // TODO: output load control
    final class OutputManagement implements MediaOutput.Callback {

        @Override
        public void onPrepared(MediaOutput mediaOutput) {

        }

        @Override
        public void onContinueLoading(MediaOutput mediaOutput) {

        }
    }

    /**
     * Controls the flow of the renderers and codifications
     */
    final class DecodeEncodeStreams extends Thread {
        final Handler transcoderHandler;
        final Renderer[] renderers;
        final Codification[] codifications;

        long highest_pts;
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
            this.highest_pts = 0;
        }

        @Override
        public void run() {
            isReady = true;
            initCodecs();
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
             *   2. Feed Encoder
             *   3. Feed Decoder
             *   4. Drain decoder
             */

            // for testing purposes only :
            if(MainActivity.TESTING)
                timeElapsed = System.currentTimeMillis();

            int decoder_response = NO_SAMPLE_OR_END_OF_STREAM;      // evaluates whether end of stream has been reached
            int encoder_response = SAMPLE_PARSED;
            int aux;
            boolean reachedEndOfStream = false;

            while (!canceled ) {
                decoder_response = NO_SAMPLE_OR_END_OF_STREAM;

                for (int i = 0; i < renderers.length; i++, encoder_response = SAMPLE_PARSED) {

                    // drain encoder output buffer before feeding new samples
                    while(encoder_response == SAMPLE_PARSED) {          // while samples are been read from output buffer
                        aux  = codifications[i].drainOutputBuffer();
                        decoder_response += aux;                        // if samples are being read from encoder we did not reached end of stream ,  0 means end of stream
                        encoder_response = encoder_response & aux;
                        if(aux == SAMPLE_PARSED)
                            numOfEncodedFrames++;
                    }

                    codifications[i].feedInputBuffer();
                    aux = renderers[i].drainOutputBuffer();

                    decoder_response += aux;                            // 0 means end of stream

                    if(aux == SAMPLE_PARSED)
                        numOfDecodedFrames++;

                    long tmp = renderers[i].feedInputBuffer(highest_pts);
                    highest_pts = tmp > highest_pts ? tmp : highest_pts;

                    if(transcoderRunning && allocator.getTotalBytesAllocated() < BUFFER_MAX_SIZE) {
                        DefaultTranscoder.this.mediaSource.continueLoading(0);
                    }
                }
                if(decoder_response == NO_SAMPLE_OR_END_OF_STREAM) {        // if decoders reached end of stream and encoders have no sample in its output buffer
                    reachedEndOfStream = true;
                    canceled = true;
                }

            }

            if(MainActivity.TESTING) {
                timeElapsed = System.currentTimeMillis() - timeElapsed;
                android.util.Log.d(TAG, "Time Elapsed For Transcoding: " + timeElapsed);
            }

            if(!reachedEndOfStream) // client demanded to stop
                signalEndOfStreamAndDrainEncoders();
            else
                drainEncoders();
        }

        /**
         * Response to stopTranscoding.
         * This functions signals the renderers of the end of stream and fetchs all the remain data from the encoder
         */
        public void signalEndOfStreamAndDrainEncoders() {
            for (int i = 0; i < renderers.length; i++) {
                renderers[i].signalEndOfStream();
            }
            int bool = 1;
            while (bool == 1) {   // FIXME: while(bool != 0)
                bool = 0;
                for (int i = 0; i < renderers.length; i++) {
                    bool += codifications[i].drainOutputBuffer();
                    codifications[i].feedInputBuffer();
                    renderers[i].drainOutputBuffer();
                    renderers[i].feedInputBuffer(highest_pts);
                }
            }
            mediaOutput.stopMuxer();
            mediaOutput.release();

            releaseRenderersAndCodifications();
            release();
        }

        /**
         * Response to endOfStream - when the end of the file has been reached
         * This method drains all the remain data from the encoder
         */
        public void drainEncoders() {
            int bool = 1;
            while(bool == 1) {
                bool = 0;
                for (int i = 0; i < codifications.length; i++)
                    bool += codifications[i].drainOutputBuffer();
            }
            mediaOutput.stopMuxer();
            mediaOutput.release();

            releaseRenderersAndCodifications();
            release();
        }

        public void stopTranscoding() {
            canceled = true;
        }

        public void release() {
            quit();
        }


    }


}

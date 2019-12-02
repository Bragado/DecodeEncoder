package com.example.decoderencoder;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.DefaultTranscoder;
import com.example.decoderencoder.library.core.Transcoder;
import com.example.decoderencoder.library.core.decoder.Decoder;
import com.example.decoderencoder.library.core.decoder.DefaultDecoder;
import com.example.decoderencoder.library.core.decoder.DefaultRenderFactory;
import com.example.decoderencoder.library.core.decoder.MediaCodecAudioRenderer;
import com.example.decoderencoder.library.core.decoder.MediaCodecVideoRenderer;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.extractor.DefaultExtractorsFactory;
import com.example.decoderencoder.library.network.Allocator;
import com.example.decoderencoder.library.network.DataSource;
import com.example.decoderencoder.library.network.DefaultAllocator;
import com.example.decoderencoder.library.network.DefaultDataSourceFactory;
import com.example.decoderencoder.library.network.DefaultLoadErrorHandlingPolicy;
import com.example.decoderencoder.library.network.LoadErrorHandlingPolicy;
import com.example.decoderencoder.library.network.Loader;
import com.example.decoderencoder.library.source.Media;
import com.example.decoderencoder.library.source.MediaSource;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.source.TrackGroup;
import com.example.decoderencoder.library.source.TrackGroupArray;
import com.example.decoderencoder.library.util.C;
import com.example.decoderencoder.library.util.Stats;

import java.io.IOException;

public class DecoderActivity  extends AppCompatActivity implements SurfaceHolder.Callback, Loader.Callback<DecoderActivity.DecodeLoadable>,
        Loader.ReleaseCallback {

    public final String TAG = "DECODER ACTIVITY";



    DataSource.Factory dataSourceFactory;
    DataSource dataSource;
    Allocator allocator;
    LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    MediaSource mediaSource = null;



    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Renderer renderer;
    String PATH = "/storage/emulated/0/Download/kika.ts";
    Surface surface;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);


        this.dataSourceFactory =
                new DefaultDataSourceFactory(this, "exoplayer-codelab");
        this.dataSource = dataSourceFactory.createDataSource();
        this.allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
        this.loadErrorHandlingPolicy = new DefaultLoadErrorHandlingPolicy();

    /*    Media.Callback callback = new Media.Callback() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onPrepared(MediaSource source) {
               Log.e(TAG, "onPrepared");
                SampleStream[] samples = source.getSampleStreams();
                TrackGroupArray tracks =  source.getTrackGroups();
                com.example.decoderencoder.library.util.Log.d(TAG, "number of samples: " + samples.length);
                source.continueLoading(0);
                MediaSource.PreparedState preparedState = source.getPreparedState();
                //DefaultRenderFactory defaultRenderFactory = new DefaultRenderFactory();
                //defaultRenderFactory.createRenderers(samples, source.getPreparedState());
                String tracksNames[] = new String[tracks.length];
                for(int i = 0; i < tracks.length; i++){
                    tracksNames[i] = tracks.get(i).getFormat(0).sampleMimeType;
                }
                RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
                TracksAdapter tracksAdapter = new TracksAdapter(DecoderActivity.this, tracksNames);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(DecoderActivity.this));
                mRecyclerView.setAdapter(tracksAdapter);

                DecodeLoadable decodeLoadable = new DecodeLoadable(DecoderActivity.this.surface,  tracks.get(0), samples[0]);
                Loader loader = new Loader("decoder");
                loader.startLoading(
                        decodeLoadable, DecoderActivity.this, loadErrorHandlingPolicy.getMinimumLoadableRetryCount(C.TRACK_TYPE_VIDEO));


            }

            @Override
            public void onContinueLoadingRequested(MediaSource source) {
                Log.e(TAG, "onContinueLoading: " + allocator.getTotalBytesAllocated());

                if( allocator.getTotalBytesAllocated() < 32833536)
                    source.continueLoading(0);

            }
        };
        mediaSource = new MediaSource(Uri.parse(PATH), dataSource, new DefaultExtractorsFactory().createExtractors(), allocator, MediaSource.DEFAULT_LOADING_CHECK_INTERVAL_BYTES, loadErrorHandlingPolicy);
        mediaSource.prepare(callback,0);*/
        Transcoder.Callback callback = new Transcoder.Callback() {


            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onPrepared(Transcoder transcoder, MediaSource.PreparedState preparedState) {
                TrackGroupArray tracks =  preparedState.tracks;
                String tracksNames[] = new String[tracks.length];
                for(int i = 0; i < tracks.length; i++){
                    tracksNames[i] = tracks.get(i).getFormat(0).sampleMimeType;
                }
                RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
                TracksAdapter tracksAdapter = new TracksAdapter(DecoderActivity.this, tracksNames);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(DecoderActivity.this));
                mRecyclerView.setAdapter(tracksAdapter);

                /* TODO: the user should be the one selecting this: */
                // VideoFormat:
                MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1280, 720);

                // Set some properties.  Failing to specify some of these can cause the MediaCodec
                // configure() call to throw an unhelpful exception.
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(MediaFormat.KEY_BIT_RATE, 5388608);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, 50);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);


                MediaFormat formatAudio = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2);
                formatAudio.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
                formatAudio.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,655360);

                TrackGroup[] trackGroups = new TrackGroup[] {tracks.get(0)/*, tracks.get(2)*/};
                MediaFormat[] mediaFormats = new MediaFormat[] {format/*, formatAudio*/};
                transcoder.setSelectedTracks(trackGroups, mediaFormats);

            }

            @Override
            public void onTracksSelected(Transcoder transcoder) {
                transcoder.startTranscoder();
            }

            @Override
            public void updateStats(Stats stats) {

            }
        };

        DefaultTranscoder df = new DefaultTranscoder(callback, PATH);
        df.start();
        df.setDataSource(dataSource);
        df.prepare();

        surfaceView = (SurfaceView) findViewById(R.id.decoderSurfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        Log.e(TAG, "Path : " + PATH);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.surface = holder.getSurface();


        /*if(renderer != null) {

            try {
                //render.start();
                Log.e(TAG, "Decoder-Encoder Finished");
            } catch (Exception e) {
                //render.stop();     // TODO: Fix me! Pause is not implemented :(
                //render.release();
                e.printStackTrace();
            }*/
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //render.stop();
        //render.release();
    }


    @Override
    public void onLoaderReleased() {

    }


    @Override
    public void onLoadCompleted(DecodeLoadable loadable, long elapsedRealtimeMs, long loadDurationMs) {

    }

    @Override
    public void onLoadCanceled(DecodeLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {

    }

    @Override
    public Loader.LoadErrorAction onLoadError(DecodeLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
        return null;
    }

    final class DecodeLoadable implements Loader.Loadable {

        Renderer renderer;
        Decoder decoder;
        TrackGroup trackGroup;
        SampleStream sampleStream;

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        public DecodeLoadable(Surface surface, TrackGroup trackGroup, SampleStream sampleStream) {
            //decoder = new DefaultDecoder(trackGroup, surface);
            //renderer = new MediaCodecVideoRenderer(C.TRACK_TYPE_VIDEO, decoder);
            this.trackGroup = trackGroup;
            this.sampleStream = sampleStream;

        }

        @Override
        public void cancelLoad() {

        }

        @Override
        public void load() throws IOException, InterruptedException {
            renderer.enable(trackGroup.getFormat(), sampleStream, 0,0);
            renderer.start();
        }
    }

}

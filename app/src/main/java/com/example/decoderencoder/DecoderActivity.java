package com.example.decoderencoder;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.audio.AacUtil;
import com.example.decoderencoder.library.core.DefaultTranscoder;
import com.example.decoderencoder.library.core.Transcoder;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.network.Allocator;
import com.example.decoderencoder.library.network.DataSource;
import com.example.decoderencoder.library.network.DefaultAllocator;
import com.example.decoderencoder.library.network.DefaultDataSourceFactory;
import com.example.decoderencoder.library.network.DefaultLoadErrorHandlingPolicy;
import com.example.decoderencoder.library.network.LoadErrorHandlingPolicy;
import com.example.decoderencoder.library.output.DefaultMediaOutput;
import com.example.decoderencoder.library.output.MediaOutput;
import com.example.decoderencoder.library.source.MediaSource;
import com.example.decoderencoder.library.source.TrackGroup;
import com.example.decoderencoder.library.source.TrackGroupArray;
import com.example.decoderencoder.library.util.C;
import com.example.decoderencoder.library.util.Stats;
import com.example.decoderencoder.ui.RunningTranscoder;
import com.example.decoderencoder.ui.SelectOutputFormats;
import com.example.decoderencoder.ui.TracksAdapter;
import com.example.decoderencoder.ui.UrisInsertation;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DecoderActivity  extends AppCompatActivity implements UrisInsertation.Callback, RunningTranscoder.Callback, SelectOutputFormats.Callback {

    public final String TAG = "DECODER ACTIVITY";



    DataSource.Factory dataSourceFactory;
    DataSource dataSource;
    Allocator allocator;
    LoadErrorHandlingPolicy loadErrorHandlingPolicy;

    //String PATH = "/storage/emulated/0/Download/kika.ts";
    //String PATH = "udp://239.192.1.103:1234";     // 3 sat hg
    String PATH = "udp://239.192.2.61:1234";      // mcm 4k


    DefaultTranscoder df = null;
    private MediaFormat formatVideo = null;
    private MediaFormat formatAudio = null;
    private String inputUri = null;
    private String outputUri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);
        if(!MainActivity.TESTING) {
            Fragment fragment = new UrisInsertation();
            loadFragment(fragment);
        }else {
            inicializeVariablesAndStartTranscoding();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void inicializeVariablesAndStartTranscoding() {
        inputUri = "/storage/emulated/0/Download/kika.ts";
        outputUri = "/storage/emulated/0/Download/kikats.ts";
        formatAudio = MediaFormat.createAudioFormat("audio/mp4a-latm", 48000, 2);   // AAC Low Overhead Audio Transport Multiplex
        formatAudio.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        formatAudio.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
        formatAudio.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,155360);


        ByteBuffer csd = ByteBuffer.allocate(2);
        csd.put(0, (byte) (2 << 3 | 3 >> 1));
        csd.put(1, (byte)((3 & 0x01) << 7 | 2 << 3));
        formatAudio.setByteBuffer("csd-0", csd);

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", 720, 480);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 2888608);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        formatVideo = format;
        startTranscoding();
    }

    private void startTranscoding() {
        this.dataSourceFactory =
                new DefaultDataSourceFactory(this, "exoplayer-codelab");
        this.dataSource = dataSourceFactory.createDataSource();
        this.allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
        this.loadErrorHandlingPolicy = new DefaultLoadErrorHandlingPolicy();

        Transcoder.Callback callback = new Transcoder.Callback() {


            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onPrepared(Transcoder transcoder, MediaSource.PreparedState preparedState) {
                TrackGroupArray tracks =  preparedState.tracks;
                String tracksNames[] = new String[tracks.length];
                for(int i = 0; i < tracks.length; i++){
                    tracksNames[i] = tracks.get(i).getFormat(0).sampleMimeType;
                }
                boolean audio_choosed = false;
                boolean video_choosed = false;

                TrackGroup[] trackGroups = new TrackGroup[0];
                TrackGroup[] discardTracks = new TrackGroup[0];
                MediaFormat[] mediaFormats = new MediaFormat[0];

                for(int i = 0; i < tracks.length; i++) {
                    if(!video_choosed && tracks.get(i).getFormat(0).sampleMimeType.contains("video") && formatVideo != null) {
                      trackGroups = Arrays.copyOf(trackGroups, trackGroups.length + 1);
                      trackGroups[trackGroups.length - 1] = tracks.get(i);
                      mediaFormats = Arrays.copyOf(mediaFormats, mediaFormats.length + 1);
                      mediaFormats[mediaFormats.length -1] = formatVideo;
                        video_choosed = true;
                    }else if(!audio_choosed && tracks.get(i).getFormat(0).sampleMimeType.contains("audio") && formatAudio != null && supportedAudioTrack(tracks.get(i).getFormat(0).sampleMimeType)) {
                        trackGroups = Arrays.copyOf(trackGroups, trackGroups.length + 1);
                        trackGroups[trackGroups.length - 1] = tracks.get(i);
                        mediaFormats = Arrays.copyOf(mediaFormats, mediaFormats.length + 1);
                        mediaFormats[mediaFormats.length -1] = formatAudio;
                        audio_choosed = true;
                    }else {
                        discardTracks = Arrays.copyOf(discardTracks, discardTracks.length + 1);
                        discardTracks[discardTracks.length - 1] = tracks.get(i);
                    }
                }
                transcoder.setSelectedTracks(trackGroups, discardTracks,  mediaFormats);

            }

            @Override
            public void onTracksSelected(Transcoder transcoder) {
                transcoder.startTranscoder();
            }

            @Override
            public void updateStats(Stats stats) {

            }
        };

        MediaOutput mediaOutput = new DefaultMediaOutput(allocator);

        DefaultTranscoder df = new DefaultTranscoder(callback, inputUri, outputUri); //"udp://239.239.239.239:1234?pkt_size=1316"
        df.start();
        df.setDataSource(dataSource);
        df.setOutputSource(mediaOutput);
        df.prepare();


        Log.e(TAG, "Path : " + inputUri);
    }

    private boolean supportedAudioTrack(String sampleMimeType) {
        switch (sampleMimeType) {
            case "audio/mpeg-L2":
                return true;
        }
        return false;
    }

    private void loadFragment(Fragment fragment) {
        // create a FragmentManager
        FragmentManager fm = getFragmentManager();
        // create a FragmentTransaction to begin the transaction and replace the Fragment
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        // replace the FrameLayout with new Fragment
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit(); // save the changes
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stopTranscoder() {
        if(df != null) {
            df.stopTranscoder();
        }
        finishAndRemoveTask();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void addVideoTrack(String codec_name, int width, int height, int bitrate, int fps) {
        this.formatVideo = MediaFormat.createVideoFormat(codec_name, width, height);
        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        this.formatVideo.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        this.formatVideo.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        this.formatVideo.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        this.formatVideo.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void addAudioTrack(String codec_name, int sample_rate, int channel_count, int bitrate) {

        if(codec_name != null ) {
            this.formatAudio = MediaFormat.createAudioFormat(codec_name, sample_rate, channel_count);   // AAC Low Overhead Audio Transport Multiplex
            this.formatAudio.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            this.formatAudio.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            this.formatAudio.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,155360);

            ByteBuffer csd = ByteBuffer.allocate(2);        // TODO
            csd.put(0, (byte) (MediaCodecInfo.CodecProfileLevel.AACObjectLC << 3 | AacUtil.getFreqIndex(formatAudio) >> 1));
            csd.put(1, (byte)((AacUtil.getFreqIndex(formatAudio) & 0x01) << 7 | channel_count << 3));
            this.formatAudio.setByteBuffer("csd-0", csd);
        }


        startTranscoding();
        Fragment fragment = new RunningTranscoder();
        loadFragment(fragment);
    }

    @Override
    public void urisSelected(String inputUri, String outputUri) {
        this.inputUri = inputUri;
        this.outputUri = outputUri;
        Fragment fragment = new SelectOutputFormats();
        loadFragment(fragment);
    }
}

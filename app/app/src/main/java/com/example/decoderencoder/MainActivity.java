package com.example.decoderencoder;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.core.DefaultTranscoder;
import com.example.decoderencoder.library.core.Transcoder;
import com.example.decoderencoder.library.core.decoder.MediaCodecAudioRenderer;
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
import com.example.decoderencoder.library.util.Assertions;
import com.example.decoderencoder.library.util.C;
import com.example.decoderencoder.library.util.Stats;
import com.example.decoderencoder.ui.Home;
import com.example.decoderencoder.ui.RunningTranscoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.RequiresApi;

public class MainActivity extends AppCompatActivity implements Home.Callback, RunningTranscoder.Callback {

    public static final boolean FORCE_GPU_RENDER = false;
    public static final boolean TESTING = false;
    public final String TAG = "DECODER ACTIVITY";

    enum TRACKS_SELECTION_MODE {
        AUTO_SELECT_AUDIO_AND_VIDEO,
        USER_SELECTS_TRACKS,
        SELECT_TRACKS_BY_PID
    }


    DataSource.Factory dataSourceFactory;
    DataSource dataSource;
    Allocator allocator;
    LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    Fragment current_fragment = null;


    DefaultTranscoder df = null;
    private MediaFormat formatVideo = null;
    private MediaFormat formatAudio = null;

    private TRACKS_SELECTION_MODE mode = TRACKS_SELECTION_MODE.AUTO_SELECT_AUDIO_AND_VIDEO;
    private final String Server_URL = "http://www.mocky.io/v2/5e32e8c7320000007994d26d";



    // Tracks Selection
    TrackGroup[] trackGroups = new TrackGroup[0];
    TrackGroup[] discardTracks = new TrackGroup[0];
    MediaFormat[] mediaFormats = new MediaFormat[0];
    String inputUri = null;
    String outputUri = null;

    // Server Selected tracks:
    String[] discardedTracksPIDs = new String[0];
    JSONArray formats = null;

    // User Selected tracks
    UserSelectedParams userSelectedParams = null;

    // stats
    TimerTask doAsynchronousTask;
    boolean tracks_not_selected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);

        this.userSelectedParams = restoreCurrentState();
        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey("user_parameters")) {
            this.userSelectedParams = null;
        }

         if(this.userSelectedParams == null) {
            Fragment fragment = new Home();
            loadFragment(fragment);
        }else {
            submitAudioAndVideo(userSelectedParams);
        }



    }

    /**
     * For testing only
     */
    private void inicializeVariablesAndStartTranscoding() {
        inputUri = "udp://239.192.2.61:1234";
        outputUri = "udp://239.239.239.238:1234?pkt_size=1316";
        //inputUri = "/storage/emulated/0/Download/HD.ts";
        //outputUri = "/storage/emulated/0/Download/output.ts";
        //outputUri = "/storage/emulated/0/Download/3satts.ts";
        formatAudio = MediaFormat.createAudioFormat("audio/mp4a-latm", 48000, 2);   // AAC Low Overhead Audio Transport Multiplex
        formatAudio.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        formatAudio.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
        formatAudio.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,255360);


        ByteBuffer csd = ByteBuffer.allocate(2);
        csd.put(0, (byte) (2 << 3 | 3 >> 1));
        csd.put(1, (byte)((3 & 0x01) << 7 | 2 << 3));
        formatAudio.setByteBuffer("csd-0", csd);

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1280, 720);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 9848510);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        format.setString(MediaFormat.KEY_TRACK_ID, "6601/01");      // mandatory
        formatVideo = format;
        //formatAudio = null;

        current_fragment = new RunningTranscoder();
        loadFragment(current_fragment);
        startTranscoding();
    }

    /**
     * Client Example
     */
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
                tracks_not_selected = false;
                TrackGroupArray tracks =  preparedState.tracks;
                String tracksNames[] = new String[tracks.length];
                for(int i = 0; i < tracks.length; i++){
                    tracksNames[i] = tracks.get(i).getFormat(0).sampleMimeType;
                }

                switch(mode) {
                    case AUTO_SELECT_AUDIO_AND_VIDEO:
                        autoSelectAudioAndVideoTracks(tracks);
                        break;
                    case USER_SELECTS_TRACKS:
                        userSelectsAudioAndVideoTracks(tracks );
                        break;
                    case SELECT_TRACKS_BY_PID:
                        try {
                            selectTracksByPID(tracks);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
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

        df = new DefaultTranscoder(callback, inputUri, outputUri); //"udp://239.239.239.239:1234?pkt_size=1316"
        df.start();
        df.setDataSource(dataSource);
        df.setOutputSource(mediaOutput);
        df.prepare();

        final Handler handler = new Handler();
        Timer timer = new Timer();
        this.doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            if(tracks_not_selected) {       // tracks should be selected already
                                Log.d(TAG, "tracks not yet selected, re-init transcoder");
                                df.stopTranscoder();
                                startTranscoding();
                                timer.cancel();
                                startTimer(doAsynchronousTask, timer);
                            }
                            updateStats();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
        startTimer(doAsynchronousTask, timer);

    }

    public void startTimer(TimerTask doAsynchronousTask, Timer timer) {
        timer.schedule(doAsynchronousTask, 1000*30, 1000*1);
    }

    /**
     * Only used for tests when #MainActivity.TESTING  is set to true
     * @param tracks
     */
    private void userSelectsAudioAndVideoTracks(TrackGroupArray tracks) {
        // TODO: ask the user wich tracks he wants to use
       // this.trackGroups = new TrackGroup[] {tracks.get(0)};
        // this.trackGroups = new TrackGroup[0];
        //this.discardTracks = new TrackGroup[] {/*tracks.get(0),*/ tracks.get(1)/*, tracks.get(2), tracks.get(3), tracks.get(4), tracks.get(5), tracks.get(6)*/};
        //this.mediaFormats = new MediaFormat[] { formatVideo,  formatAudio};


        Intent intent = new Intent(this, RecordActivity.class);
        userSelectedParams = new UserSelectedParams();
        userSelectedParams.output_address = "239.239.239.238";
        userSelectedParams.output_port = 1234;
        userSelectedParams.frame_rate = 25;
        userSelectedParams.video_bitrate = 15*1000*1000;
        userSelectedParams.width = 1920;
        userSelectedParams.height = 1080;
        userSelectedParams.video_codec = "video/avc";
        intent.putExtra("user_parameters", userSelectedParams);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void autoSelectAudioAndVideoTracks(TrackGroupArray tracks) {
        boolean audio_choosed = false;
        boolean video_choosed = false;

        for(int i = 0; i < tracks.length; i++) {
            if(!video_choosed && tracks.get(i).getFormat(0).sampleMimeType.contains("video")  ) {
                trackGroups = Arrays.copyOf(trackGroups, trackGroups.length + 1);
                trackGroups[trackGroups.length - 1] = tracks.get(i);
                mediaFormats = Arrays.copyOf(mediaFormats, mediaFormats.length + 1);
                mediaFormats[mediaFormats.length -1] = getVideoFormat(userSelectedParams.video_codec, userSelectedParams.width, userSelectedParams.height, userSelectedParams.video_bitrate, userSelectedParams.frame_rate);
                mediaFormats[mediaFormats.length -1].setString(MediaFormat.KEY_TRACK_ID, "0101");       // random ID, but it's necessary
                video_choosed = true;
            }else if(!audio_choosed && tracks.get(i).getFormat(0).sampleMimeType.contains("audio")  && supportedAudioTrack(tracks.get(i).getFormat(0).sampleMimeType)) {

                if(!userSelectedParams.audio_codec.equals("passthrough")) {
                    trackGroups = Arrays.copyOf(trackGroups, trackGroups.length + 1);
                    trackGroups[trackGroups.length - 1] = tracks.get(i);
                    mediaFormats = Arrays.copyOf(mediaFormats, mediaFormats.length + 1);
                    MediaFormat mediaFormat = MediaCodecAudioRenderer.getMediaFormat(tracks.get(i).getFormat(0),tracks.get(i).getFormat(0).sampleMimeType);
                    mediaFormat.setString(MediaFormat.KEY_MIME, userSelectedParams.audio_codec);
                    mediaFormats[mediaFormats.length -1] = mediaFormat;
                }// else passthrough
                audio_choosed = true;
            }else {
                discardTracks = Arrays.copyOf(discardTracks, discardTracks.length + 1);
                discardTracks[discardTracks.length - 1] = tracks.get(i);
            }
        }
    }

    /**
     * Audio MimeTypes supported by the devices
     * @param sampleMimeType
     * @return
     */
    private boolean supportedAudioTrack(String sampleMimeType) {
        switch (sampleMimeType) {
            case "audio/mpeg-L2":
                return true;
            case "audio/mp4a-latm":
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
        SharedPreferences preferences = getSharedPreferences("Transcoder", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("state", "").apply();
        editor.commit();
        loadFragment(new Home());
    }

    /**
     * GUI callback with user selected setting
     * @param userSelectedParams {@link UserSelectedParams}
     */
    @Override
    public void submitAudioAndVideo(UserSelectedParams userSelectedParams) {
        this.userSelectedParams = userSelectedParams;
        this.inputUri = "udp://" + userSelectedParams.input_address + ":" + userSelectedParams.input_port;
        this.outputUri = "udp://" + userSelectedParams.output_address  + ":" + userSelectedParams.output_port + "?pkt_size=1316";
        saveCurrentState(userSelectedParams);
        if(userSelectedParams.input_port == 0) {        // HDMI
            Intent intent = new Intent(this, RecordActivity.class);
            intent.putExtra("user_parameters", userSelectedParams);
            startActivity(intent);
            tracks_not_selected = false;
        }else {
            current_fragment = new RunningTranscoder();
            loadFragment(current_fragment);
            startTranscoding();
        }

    }

    private void saveCurrentState(UserSelectedParams userSelectedParams) {
        SharedPreferences preferences = getSharedPreferences("Transcoder", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("state", userSelectedParams.toJSON().toString()).apply();
        editor.commit();
    }

    private UserSelectedParams restoreCurrentState() {

        UserSelectedParams userSelectedParams = null;
        SharedPreferences preferences = getSharedPreferences("Transcoder", MODE_PRIVATE);
        String state = preferences.getString("state", null);
        if(state == null)
            return null;
        Log.d(TAG, "restoring previous state with: " + state);
        try {
            JSONObject json = new JSONObject(state);
            return new UserSelectedParams(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


    public MediaFormat getVideoFormat(String codec_name, int width, int height, int bitrate, int fps) {
        MediaFormat formatVideo = MediaFormat.createVideoFormat(codec_name, width, height);
        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        formatVideo.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        formatVideo.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        formatVideo.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        formatVideo.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        return formatVideo;
    }


    public void updateStats() {
        ((RunningTranscoder)current_fragment).updateStats(df.getStats());
    }

    @Override
    public void onBackPressed()
    {
        stopTranscoder();
    }


    /**
     * Server Client Example Below :
     */

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void selectTracksByPID(TrackGroupArray tracks) throws JSONException {

        int j = 0;
        int k = 0;
        int len;


        for(int i = 0; i < tracks.length; i++) {
            TrackGroup track = tracks.get(i);
            if(discardedTracksPIDs.length > j && track.getFormat(0).id.contains("/" + discardedTracksPIDs[j])) {
                len = discardTracks.length;
                this.discardTracks = Arrays.copyOf(discardTracks, len + 1);
                this.discardTracks[len] = track;
                j++;
            } else if(formats.length() > k && track.getFormat(0).id.contains("/" + formats.getJSONObject(k).getString("pid"))) {
                Assertions.checkArgument(trackGroups.length == mediaFormats.length);
                len = trackGroups.length;
                this.trackGroups = Arrays.copyOf(trackGroups, len + 1);
                this.trackGroups[len] = track;
                this.mediaFormats = Arrays.copyOf(mediaFormats, len + 1);
                this.mediaFormats[len] = parseMediaFormat(track.getFormat(0), formats.getJSONObject(k));;
                k++;
            }
        }
    }

    // http://www.mocky.io/v2/5e31d3a232000053ad888862
    private class GetServerConfiguration extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MainActivity.this,"Json Data is downloading",Toast.LENGTH_LONG).show();

        }

        @Override
        protected Void doInBackground(Void... voids) {
            HttpHandler sh = new HttpHandler();
            // Making a request to url and getting response

            String jsonStr = sh.makeServiceCall(Server_URL);

            Log.e(TAG, "Response from url: " + jsonStr);
            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);


                    // Getting JSON Array node
                    JSONArray discard_tracks = jsonObj.getJSONArray("discard_tracks");

                    inputUri = jsonObj.getString("inputUri");
                    outputUri = jsonObj.getString("outputUri");

                    formats = jsonObj.getJSONArray("formats");

                    for(int i = 0; i < discard_tracks.length(); i++) {
                        int len = discardedTracksPIDs.length;
                        discardedTracksPIDs = Arrays.copyOf(discardedTracksPIDs, len +1);
                        discardedTracksPIDs[len] = discard_tracks.getString(i);
                    }

                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                }

            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            return null;
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            startTranscoding();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private MediaFormat parseMediaFormat(Format trackFormat, JSONObject userFormat) throws JSONException {
        MediaFormat mediaFormat = null;
        String mime_type = userFormat.getString("mime_type");
        int bitrate = userFormat.getInt("bitrate");
        if(mime_type.startsWith("video/")) {
            int width =  userFormat.getString("width").equals("passthrough") ? trackFormat.width : Integer.parseInt(userFormat.getString("width"));
            int heigth = userFormat.getString("height").equals("passthrough") ? trackFormat.width : Integer.parseInt(userFormat.getString("height"));
            int frame_rate = userFormat.getInt("frame_rate");
            int key_frame_interval = userFormat.getInt("key_frame_interval");

            mediaFormat = MediaFormat.createVideoFormat(mime_type, width, heigth);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frame_rate);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, key_frame_interval);
            mediaFormat.setString(MediaFormat.KEY_TRACK_ID, userFormat.getString("pid"));

        }else if(userFormat.getString("mime_type").startsWith("audio/")) {
            int sampleRate = userFormat.getString("sample_rate").equals("passthrough") ? trackFormat.sampleRate : Integer.parseInt(userFormat.getString("sample_rate"));
            int channelCount = userFormat.getString("channel_count").equals("passthrough") ? trackFormat.channelCount : Integer.parseInt(userFormat.getString("channel_count"));
            formatAudio = MediaFormat.createAudioFormat(mime_type, sampleRate, channelCount);   // AAC Low Overhead Audio Transport Multiplex
            formatAudio.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            formatAudio.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            formatAudio.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,255360);
            formatAudio.setString(MediaFormat.KEY_LANGUAGE, trackFormat.language);
        }



        return mediaFormat;
    }

}

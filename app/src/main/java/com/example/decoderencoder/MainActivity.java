package com.example.decoderencoder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telecom.Call;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.decoderencoder.library.extractor.DefaultExtractorsFactory;
import com.example.decoderencoder.library.network.Allocator;
import com.example.decoderencoder.library.network.DataSource;
import com.example.decoderencoder.library.network.DefaultAllocator;
import com.example.decoderencoder.library.network.DefaultDataSourceFactory;
import com.example.decoderencoder.library.network.DefaultLoadErrorHandlingPolicy;
import com.example.decoderencoder.library.network.LoadErrorHandlingPolicy;
import com.example.decoderencoder.library.source.Media;
import com.example.decoderencoder.library.source.MediaSource;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.source.TrackGroupArray;
import com.example.decoderencoder.library.util.C;
import com.example.decoderencoder.library.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  {

    public final String TAG = "ACTIVITY MAIN";
    Context This;

    public static final boolean TESTING = false;
    public static final boolean FORCE_GPU_RENDER = false;


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //final ListView listView = (ListView) findViewById(R.id.list);
       // Button decoderEncoder = findViewById(R.id.button3);
       /* final Intent intent = new Intent(this, DecoderEncoderActivity.class);
        final Intent decodeIntent = new Intent(this, DecoderActivity.class);


        This = this;

        decoderEncoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(intent);
            }
        });
*/


       final Intent decodeIntent = new Intent(this, DecoderActivity.class);
        Button decode = (Button) findViewById(R.id.decode_only);
        startActivity(decodeIntent);
        decode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(decodeIntent);
            }
        });
      /*  DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(this, "exoplayer-codelab");
        DataSource dataSource = dataSourceFactory.createDataSource();
        Allocator allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
        LoadErrorHandlingPolicy loadErrorHandlingPolicy = new DefaultLoadErrorHandlingPolicy();
        MediaSource mediaSource = null;
        Media.Callback callback = new Media.Callback() {
            @Override
            public void onPrepared(MediaSource source) {
                Log.d(TAG, "onPrepared");
            }

            @Override
            public void onContinueLoadingRequested(MediaSource source) {
                Log.d(TAG, "onContinueLoading");
                SampleStream[] samples = source.getSampleStreams();
                TrackGroupArray tracks =  source.getTrackGroups();
                Log.d(TAG, "number of samples: " + samples.length);
            }
        };

        mediaSource = new MediaSource(Uri.parse("/storage/emulated/0/Download/kika.ts"), dataSource, new DefaultExtractorsFactory().createExtractors(), allocator, MediaSource.DEFAULT_LOADING_CHECK_INTERVAL_BYTES, loadErrorHandlingPolicy);
        mediaSource.prepare(callback,0);*/



    }


}

package com.example.encoderdecoder;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public final String TAG = "ACTIVITY MAIN";

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Decoder decoder ;
    String PATH = "/storage/emulated/0/Download/The Simpsons Movie - Trailer.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);


        decoder = new Decoder();


        Log.e(TAG, "Path : " + PATH);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if(decoder != null) {
                try {
                    if(decoder.DecodeVideoFile(PATH, holder.getSurface()))
                        decoder.start();
                } catch (Exception e) {
                    decoder = null;
                    e.printStackTrace();
                }

            }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(decoder != null)
            decoder.close();
    }
}

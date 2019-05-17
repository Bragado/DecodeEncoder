package com.example.decoderencoder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.decoderencoder.Library.DecoderEncoder;
import com.example.decoderencoder.Library.VCodec;

public class DecoderActivity  extends AppCompatActivity implements SurfaceHolder.Callback {

    public final String TAG = "DECODER ACTIVITY";

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    DecoderEncoder decoder ;
    String PATH = "/storage/emulated/0/Download/The Simpsons Movie - Trailer.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);

        surfaceView = findViewById(R.id.decoderSurfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        Log.e(TAG, "Path : " + PATH);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        VCodec vCodecDecoder = new VCodec(holder.getSurface(), PATH);
        decoder = new DecoderEncoder(null, vCodecDecoder);

        if(decoder != null) {

            try {
                decoder.start();
                Log.e(TAG, "Decoder-Encoder Finished");
            } catch (Exception e) {
                decoder.pause();     // TODO: Fix me! Pause is not implemented :(
                decoder.release();
                e.printStackTrace();
            }
}
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        decoder.pause();
        decoder.release();
    }
}

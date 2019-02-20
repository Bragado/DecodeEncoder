package com.example.encoderdecoder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.encoderdecoder.Library.Decoder;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public final String TAG = "ACTIVITY MAIN";

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Decoder decoder ;
    String PATH2 = "/storage/emulated/0/Download/The Simpsons Movie - Trailer.mp4";
    //String PATH2 = "http://playready.directtaps.net/smoothstreaming/SSWSS720H264/SuperSpeedway_720_1427.ismv";
    //String PATH3 = "http://playready.directtaps.net/smoothstreaming/TTLSS720VC1/To_The_Limit_720_1427.ismv";
    //String PATH2 = "https://streaming-ondemand.rtp.pt/nas2.share/h264/512x384/p5488/asset-audio=1024-video=1024-9.ts?tlm=hls&streams=p5488_1_201902120000000981.mp4.m3u8:1024&AliasPass=streaming-ondemand.rtp.pt&aa=11";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);


        decoder = new Decoder();


        Log.e(TAG, "Path : " + PATH2);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if(decoder != null) {
                try {
                    if(decoder.DecodeVideoFile(PATH2, holder.getSurface()))
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

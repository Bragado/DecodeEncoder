package com.example.encoderdecoder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.encoderdecoder.Library.VCodec;

public class DecoderEncoderActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public final String TAG = "DecoderEncoder Activity";

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    com.example.encoderdecoder.Library.DecoderEncoder decoderEncoder;
    //String PATH2 = "/storage/emulated/0/Download/The Simpsons Movie - Trailer.mp4";
    //String PATH2 = "http://playready.directtaps.net/smoothstreaming/SSWSS720H264/SuperSpeedway_720_1427.ismv";
    String PATH2 = "http://playready.directtaps.net/smoothstreaming/TTLSS720VC1/To_The_Limit_720_1427.ismv";
    //String PATH2 = "https://streaming-ondemand.rtp.pt/nas2.share/h264/512x384/p5488/asset-audio=1024-video=1024-9.ts?tlm=hls&streams=p5488_1_201902120000000981.mp4.m3u8:1024&AliasPass=streaming-ondemand.rtp.pt&aa=11";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder_encoder);

        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);


        //decoder = new Decoder();


        Log.e(TAG, "Path : " + PATH2);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


        VCodec vCodecDecoder = new VCodec(holder.getSurface(), PATH2);
        VCodec vCodecEncoder = new VCodec("/storage/emulated/0/Download/testX.mp4");
        decoderEncoder = new com.example.encoderdecoder.Library.DecoderEncoder(vCodecEncoder, vCodecDecoder) ;

        if(decoderEncoder != null) {

            try {
                decoderEncoder.start();
                Log.e(TAG, "Decoder-Encoder Finished");
            } catch (Exception e) {
                decoderEncoder.pause();     // TODO: Fix me! Pause is not implemented :(
                decoderEncoder.release();
                e.printStackTrace();
            }

        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        decoderEncoder.pause();
        decoderEncoder.release();
    }
}

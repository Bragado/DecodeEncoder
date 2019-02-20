package com.example.encoderdecoder.Library;

import android.media.MediaCodec;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;


/* Surface to Surface Decoder-Encoder or Encoder-Decoder */

public abstract class DecoderEncoder {

    public final String TAG = "DECODER-ENCODER";
    private CodecWorkerThread worker;  // it should not be the responsible of the main class to create a thread
    private VCodec vCodec;          // all needed configuration for encoding or decoding


    public DecoderEncoder (VCodec vCodec) {
        this.vCodec = vCodec;
    }


    public abstract void start();

    public void stop()  {
        worker.setDisabled();
    }

    protected void onSurfaceCreated(Surface surface) {
    }

    protected void onSurfaceDestroyed(Surface surface) {
    }

    protected void onDecodedSample(MediaCodec.BufferInfo info, ByteBuffer data) {
    }

    protected void onEncodedSample(MediaCodec.BufferInfo info, ByteBuffer data) {
    }

    public abstract class CodecWorkerThread extends Thread {

        private boolean enabled = false;
        MediaCodec mediaCodec;



        public void setEnabled() {
            this.enabled = true;
        }
        public void setDisabled() {
            this.enabled = false;
        }

         @Override
        public void run() {

            configure();
            try {
                encodeOrDecode();
            }catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG,e.toString());
            }finally {
                shutdown();
            }
        }

        public abstract void configure();

        public abstract void encodeOrDecode() throws Exception;

        public void shutdown() {
            mediaCodec.stop();
            mediaCodec.release();
        }


    }



}

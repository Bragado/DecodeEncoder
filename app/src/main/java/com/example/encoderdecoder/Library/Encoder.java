package com.example.encoderdecoder.Library;


import android.media.MediaCodec;
import android.view.Surface;

import java.nio.ByteBuffer;

public class Encoder extends DecoderEncoder {

    private MyWorkerThread worker;  // it should not be the responsible of the main class to create a thread


    public Encoder(VCodec vCodec) {
        super(vCodec);
    }


    public void start()  {
        if(worker == null)
            stop();
        worker = new MyWorkerThread();
        worker.setEnabled();
        worker.start();

    }





    private class MyWorkerThread extends CodecWorkerThread {

        @Override
        public void run(){

        }

        @Override
        public void configure() {

        }

        @Override
        public void encodeOrDecode() throws Exception {

        }



    }



}

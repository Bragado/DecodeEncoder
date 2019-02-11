package com.example.encoderdecoder;

import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Decoder extends  Thread {
    private static final String TAG = "Decoder";

    private MediaCodec codec = null;
    private MediaExtractor extractor;
    private boolean initialized = false;
    private long timeoutUs = 10000;

    private boolean SAVEFILE = true;
    private static final String DEBUG_FILE_NAME_BASE = "/storage/emulated/0/Download/Decoded.mp4";


    public boolean DecodeVideoFile(String path, Surface surface) throws Exception {
        this.extractor = new MediaExtractor();
        String mimeType = "";


        try {
            // Read the file with a MediaExtractor
            this.extractor.setDataSource(path);

            Utils.configCodecWithMimeType("video/", this.extractor, this.codec, surface);
            codec.start();

            if(this.codec == null)
                return false;

        } catch (IOException e) {
            e.printStackTrace();
            //Toast.makeText(this, "Video File Could not be found", Toast.LENGTH_LONG).show();
            throw new FileNotFoundException("Video File Could not be found");

        } catch (IllegalStateException e) {
            Log.e(TAG, "codec '" + mimeType + "' failed configuration. " + e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Could not create codec");
        } finally {
            if(extractor == null)
                throw new Exception("Could not initialize Media Extractor");
        }

        initialized = true;
        return true;

    }


    @Override
    public void run() {
        if(!initialized)
            return;

        ByteBuffer[] outputBuffers = codec.getOutputBuffers();

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        //codec.getOutputBuffers();

        boolean ready = false;

        for(;;)  {

            if(!ready) {
                int inputBufferId = codec.dequeueInputBuffer(this.timeoutUs);
                if( inputBufferId >= 0) {

                    ByteBuffer inputBuffer = inputBuffers[inputBufferId];

                    int sampleSize = extractor.readSampleData(inputBuffer, 0);

                    if(extractor.advance() && sampleSize > 0) {
                        codec.queueInputBuffer(inputBufferId, 0, sampleSize, extractor.getSampleTime(), 0);
                    }else {
                        Log.e(TAG, "End of stream");
                        codec.queueInputBuffer(inputBufferId, 0,0,0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        ready = true;
                    }
                }
            }

            int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, this.timeoutUs);
            switch (outputBufferIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");

                    outputBuffers = codec.getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
				    Log.d(TAG, "INFO_TRY_AGAIN_LATER");
                    break;
                default:            // in case index is correct


                    //outputBuffers = codec.getOutputBuffers();

                    codec.releaseOutputBuffer(outputBufferIndex, true );
                    break;


            }



            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }


        }

        codec.stop();
        codec.release();
        extractor.release();

    }

    public void close() {
        initialized = false;
    }


}

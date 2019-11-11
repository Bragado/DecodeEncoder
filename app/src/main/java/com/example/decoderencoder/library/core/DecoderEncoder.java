package com.example.decoderencoder.library.core;


import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.example.decoderencoder.Library.AVMuxer;
import com.example.decoderencoder.library.support.VCodec;
import com.example.decoderencoder.OpenGL.InputSurface;
import com.example.decoderencoder.OpenGL.OutputSurface;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;


/* Surface to Surface Decoder-Encoder or Encoder-Decoder */

// TODO: difference between shutdown and stop ?? one should do the effect of pause and de other stop! the release of resources should be this class' responsible whenever the user calls stop
// TODO: run lint
// TODO: use API 23


public class DecoderEncoder {

    public final String TAG = "DECODER-ENCODER";
    public boolean DEBUG = true;
    //private CodecWorkerThread worker;  // it should not be the responsible of the main class to create a thread
    private VCodec vCodecEncoder = null;          // all needed configuration for encoding or decoding
    private VCodec vCodecDecoder = null;
    private InputSurfaceCallback inputSurface = null ;



    public DecoderEncoder (VCodec vCodecEncoder, VCodec vCodecDecoder) {
        this.vCodecEncoder = vCodecEncoder;
        this.vCodecDecoder = vCodecDecoder;
       // worker = new CodecWorkerThread();
       // worker.start();
    }


    /**
     *  Enables de decoder/encoder process
     */
    public void start() {
       // worker.setEnabled();

    }

    /**
     *  Pauses decoder/encoder process
     */
    public void pause()  {

       // worker.setDisabled();
    }

    public void Continue() {

    }


    /**
     *  Stops decoder/encoder process
     */
    public void release()  {

        //worker.shutdown();
    }



    public interface InputSurfaceCallback   {

        /**
         * The encoder's input surface cannot be specified unless we use API lvl > 23, this interface must be implemented by the object that wants to encode some raw data in order to receive the appropriated stream
         * @param surface   generated Input Surface
         */
        public void onSurfaceCreated(Surface surface);
    }



/*    private class CodecWorkerThread extends Thread {

        private boolean enabled = false;
        private boolean pause = false;              // TODO: implement it
        private boolean endOfStream = true;


        // TODO: Test
        InputSurface mInputSurface;
        OutputSurface mOutputSurface;
        boolean decodeEncode = false;


        MediaCodec decoder = null;
        MediaCodec encoder = null;

        /* Decoder */
 /*       AVDemuxer extractor = null;

        public void setEnabled() {
            this.enabled = true;
        }
        public void setDisabled() { this.enabled = false; }


        /**
         * Generic function to decode, encode or decode-encode
         * Configures the encoder or decoder or both and runs the necessary methods to process the data stream.
         */
  /*       @Override
        public void run() {
             Surface surface = null;

             // TODO: Test
             if(vCodecEncoder != null && vCodecDecoder != null)
                 decodeEncode = true;


            // < Encoder and decoder configurations >
             if(vCodecEncoder != null)  {
                 try {
                     surface = configureEncoder(vCodecEncoder);
                     Log.d(TAG, "Encoder Configured");
                 }catch (Exception e) {
                     e.printStackTrace();
                     Log.e(TAG, "InputSurface interface not implemented");
                     return;
                 }

                 if(vCodecDecoder != null) { // since we cannot do setInputSurface in the encoder in API 18 we must configure the decoder with the result input surface of the encoder otherwise this if statement wouldn't be necessary
                     vCodecDecoder.setDecoderOutputSurface(surface);
                     Log.d(TAG, "Setting Decoder Input Surface");
                 }

             }if(vCodecDecoder != null ) {
                try {
                    configureDecoder(vCodecDecoder);
                    Log.d(TAG, "Decoder Configured");
                }catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

            }

             // </ Encoder and decoder configurations >

            enabled = true;
            endOfStream = false;

            try {
                if(decoder == null) {
                    Log.d(TAG, "Starting encoder");
                    encode(vCodecEncoder, true);
                }

                else if(encoder == null) {
                    Log.d(TAG, "Starting decoder");
                    decode(vCodecDecoder, true);
                }

                else{
                    Log.d(TAG, "Starting decoder-encoder");
                    decodeAndEncode();
                }

            }catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG,e.toString());
            }finally {
                Log.d(TAG, "Shutdown");
                shutdown();

            }
        }

        /**
         *  Creates the encoder given its parameters in VCodec, if no input surface is presented one will be created and published transmitted with InputSurface interface method callback
         * @param vCodec encoder parameters container
         * @throws Exception e when no input surface is passed to the encoder and InputSurface interface was not not implemented by the client
         */
   /*     @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        public Surface configureEncoder(VCodec vCodec)  throws  Exception{

            MediaFormat format = vCodec.getEncoderFormat();

            try{
                this.encoder = MediaCodec.createEncoderByType(vCodec.getEncoderVideoFormat());
            }catch(IOException e) {
                throw new RuntimeException(e);
            }

            Surface surface = null;
            this.encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            try {
                if(!decodeEncode) {
                    surface = this.encoder.createInputSurface();
                    vCodecEncoder.setEncoderInputSurface(surface);
                }else {
                    surface = this.encoder.createInputSurface();
                    mInputSurface = new InputSurface( surface );
                    mInputSurface.makeCurrent();
                }

                if(inputSurface != null) // for decoding-encoding there's no need for a call
                    inputSurface.onSurfaceCreated(surface);

            }catch(Exception e)  {
                throw e;
            }

            this.encoder.start();
            return surface;
        }


        /**
         *  Method responsible to create a decoder, starts by creating the extractor with the data path, selects the track from the stream and finally creates the codec
         * @param vCodec container of parameters necessary to initialize de decoder
         * @throws Exception when: a codec cannot be found; data path is not correct ; internal error due to precedences of calls to mediacodec api
         */
   /*     @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void configureDecoder(VCodec vCodec) throws Exception{
            this.extractor = new AVDemuxer();
            String mimeType = "";

            try {
                // Read the file with a MediaExtractor
                this.extractor.setDataSource(vCodec.getURLForDecoder());

                if(decodeEncode ) {
                    Log.d(TAG, "Getting DecoderConfiguration");
                    this.mOutputSurface = new OutputSurface();
                    this.decoder = VCodec.configCodecWithMimeType("video/", (MediaExtractor) this.extractor.getInstance(), this.mOutputSurface.getSurface());
                }else
                    this.decoder = VCodec.configCodecWithMimeType("video/", (MediaExtractor)this.extractor.getInstance(), vCodecDecoder.getDecoderSurface());

                if(this.decoder == null)
                    throw new Exception("Could not create decoder codec");
                decoder.start();


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

        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void encode(VCodec vCodec, boolean render) throws Exception{


            if(endOfStream) {
                this.encoder.signalEndOfInputStream();
                if(DEBUG) Log.e(TAG, "sending end of stream to encoder");
            }

            AVMuxer mediaMuxer = vCodecEncoder.getMediaMuxer();
            boolean mediaMuxerStarted = false;
            int mediaMuxerTrackIndex = -1;


            ByteBuffer[] encoderOutputBuffers = this.encoder.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (true) {
                int encoderStatus = this.encoder.dequeueOutputBuffer(bufferInfo, vCodecEncoder.getTimeOUT());

                switch(encoderStatus) {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        encoderOutputBuffers = this.encoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        MediaFormat newFormat = this.encoder.getOutputFormat();
                        if(mediaMuxerStarted) {
                            Log.e(TAG, "Media Muxer has already started, this should have not happened");
                        }
                        mediaMuxerTrackIndex = mediaMuxer.addTrack(newFormat);
                        mediaMuxer.start();
                        mediaMuxerStarted = true;

                        break;
                    default:

                        if(encoderStatus < 0) {
                            Log.e(TAG, "Unexpected resulted while encoding");
                            break;
                        }


                        ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                        if(encodedData == null) {
                            Log.e(TAG, "Encoded data is null, something went wrong");
                        }

                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) { bufferInfo.size = 0; } // ignore data

                        if (bufferInfo.size != 0) {
                            if (!mediaMuxerStarted) {
                                Log.e(TAG, "MediaMuxer should have been started, something went wrong");
                            }
                        }

                        encodedData.position(bufferInfo.offset);
                        encodedData.limit(bufferInfo.offset + bufferInfo.size);
                        mediaMuxer.writeSampleData(mediaMuxerTrackIndex, encodedData, bufferInfo);


                        break;
                }
                encoder.releaseOutputBuffer(encoderStatus, false);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.e(TAG, "reached end of stream unexpectedly");
                    } else {
                        Log.e(TAG, "end of stream reached");
                        break;
                    }
                }

            }

        }

        /**
         *  Decodes a data stream into VCodec.getDecoderSurface
         * @param vCodec container with the parameters
         * @param render whether the decoded video should be rendered
         */
 /*       @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void decode(VCodec vCodec, boolean render) {

            if(!enabled)
                return;

            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = decoder.getInputBuffers();

            while(enabled)  {

                int inputBufferId = decoder.dequeueInputBuffer(vCodecDecoder.getTimeOUT());
                if( inputBufferId >= 0) {

                    ByteBuffer inputBuffer = inputBuffers[inputBufferId];

                    int sampleSize = extractor.readSampleData(inputBuffer, 0);

                    if(extractor.advance() && sampleSize > 0 && !endOfStream) {
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, extractor.getSampleTime(), 0);
                    }else {
                        Log.e(TAG, "End of stream");
                        decoder.queueInputBuffer(inputBufferId, 0,0,0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                    }
                }

                int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, vCodecDecoder.getTimeOUT());
                switch (outputBufferIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");

                        outputBuffers = decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d(TAG, "INFO_TRY_AGAIN_LATER");
                        break;
                    default:            // in case index is correct
                        decoder.releaseOutputBuffer(outputBufferIndex, vCodecDecoder.isSupposedToRender() );
                        break;
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }
        }



        private void endOfStream() {
            endOfStream = true;
        }





        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void shutdown() {
            endOfStream();

            if(encoder != null) {
                vCodecEncoder.stopMediaMuxer();
                encoder.stop();
                encoder.release();
            }if(decoder != null) {
                decoder.stop();
                decoder.release();
            }
        }

        // 1 - Decode the generated stream with MediaCodec, using an output Surface.
        // 2 - Encode the frame with MediaCodec, using an input Surface.

        /**
         *  The decoder must be configured with an output surface, and the encoder with an input surface. We used the inputSurface returned by the encoder to create que output surface of the decoder
         * @throws Exception
         */


        // TODO: this method is too long
 /*       @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void decodeAndEncode() throws Exception{

            Log.e(TAG, "Transcoder not implemented");

            ByteBuffer[] outputBuffers = encoder.getOutputBuffers();
            ByteBuffer[] inputBuffers = decoder.getInputBuffers();

            MediaCodec.BufferInfo encoderBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo decoderBufferInfo = new MediaCodec.BufferInfo();

            AVMuxer mediaMuxer = vCodecEncoder.getMediaMuxer();
            boolean mediaMuxerStarted = false;
            int mediaMuxerTrackIndex = -1;
            boolean decoderEnabled = true;
            long i = 0L;
            while(enabled) {

                /* < Decoder > */
  /*              if(decoderEnabled) { // feeds information to the decoder
                    int inputBufferId = decoder.dequeueInputBuffer(vCodecDecoder.getTimeOUT());

                    if( inputBufferId >= 0) {

                        ByteBuffer inputBuffer = inputBuffers[inputBufferId];

                        int sampleSize = extractor.readSampleData(inputBuffer, 0);

                        if(extractor.advance() && sampleSize > 0 && !endOfStream) {
                            decoder.queueInputBuffer(inputBufferId, 0, sampleSize, extractor.getSampleTime(), 0);
                        }else {
                            Log.e(TAG, "End of stream");
                            decoder.queueInputBuffer(inputBufferId, 0,0,0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            decoderEnabled = false;
                            encoder.signalEndOfInputStream();
                        }
                    }
                }


                /* < /Decoder > */

                /* < Encoder > */
 /*               boolean encoderDone = false;

                while(!encoderDone) {   // to ensure no information is lost, let's assume the decoder is always full

                    int encoderStatus = MediaCodec.INFO_TRY_AGAIN_LATER;
                    //try {
                        encoderStatus = this.encoder.dequeueOutputBuffer(encoderBufferInfo, vCodecEncoder.getTimeOUT());
                    //}catch(Exception e) {
                     //   e.printStackTrace();
                      //  continue;
                    //}


                    // TODO: check all possible outcomes of encoderStatus
                    if(encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        outputBuffers = this.encoder.getOutputBuffers();
                    }else if(encoderStatus ==  MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = this.encoder.getOutputFormat();
                        if(mediaMuxerStarted) {
                            Log.e(TAG, "Media Muxer has already started, this should have not happened");

                        }
                        mediaMuxerTrackIndex = mediaMuxer.addTrack(newFormat);
                        mediaMuxer.start();
                        mediaMuxerStarted = true;

                    }else if(encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        Log.d(TAG, "Encoder status try again");
                        encoderDone = true;      // the decoder output buffer was drain
                    }else if(encoderStatus >= 0) { // success encoder status >= 0 ; let's catch the information from the encoder
                        Log.d(TAG, "Encoder Status success");
                        ByteBuffer encodedData = outputBuffers[encoderStatus];
                        if (encodedData == null) {
                            Log.e(TAG, "Encoded data is null, something went wrong");
                        }

                        if ((encoderBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            encoderBufferInfo.size = 0;
                        } // ignore data

                        if (encoderBufferInfo.size != 0) {
                            if (!mediaMuxerStarted) {
                                Log.e(TAG, "MediaMuxer should have been started, something went wrong");
                            }
                            // MediaMuxer to save track as mp4
                            encodedData.position(encoderBufferInfo.offset);
                            encodedData.limit(encoderBufferInfo.offset + encoderBufferInfo.size);
                            mediaMuxer.writeSampleData(mediaMuxerTrackIndex, encodedData, encoderBufferInfo);
                        }

                        encoder.releaseOutputBuffer(encoderStatus, false);

                    }
                    if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        Log.e(TAG, "Continue");
                        continue;
                    }
                    if(encoderDone) {   // TODO: try a if(true)    // no information in encoder input surface, so let's get him new data throw decoder output surface
                        Log.e(TAG, "Information was pass to the decoder");
                        // catches information from the decoder
                        int outputBufferIndex = decoder.dequeueOutputBuffer(decoderBufferInfo, vCodecDecoder.getTimeOUT());
                        if(outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            encoderDone = true; // TODO: check this out
                        }else if( outputBufferIndex >= 0) {


                            boolean render = (decoderBufferInfo.size != 0);
                            decoder.releaseOutputBuffer(outputBufferIndex, render);

                            if(render) {
                                // This waits for the image and renders it after it arrives.
                                this.mOutputSurface.awaitNewImage();
                                this.mOutputSurface.drawImage();
                                // Send it to the encoder.
                                i++;
                                Log.i(TAG, "Presentation Time: " + decoderBufferInfo.presentationTimeUs * 1000);
                                //Log.i(TAG, "i: " + i);
                                //mInputSurface.setPresentationTime(decoderBufferInfo.presentationTimeUs * 1000);
                                mInputSurface.setPresentationTime(41709000 * i);    // check if this is a samsung problem
                                mInputSurface.swapBuffers();
                            }

                        }

                    }
                }
                /* < /Encoder > */

                /* < End of stream > */
  /*              if(endOfStream) {
                    decoder.signalEndOfInputStream();
                    /*encoder.signalEndOfInputStream();*/ //TODO : is there a problem?
 /*               }else if ((encoderBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;

                }else if ((decoderBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");

                }
                /* </ End of stream > */

  /*          }
        }

    }
*/


}

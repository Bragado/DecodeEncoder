package com.example.decoderencoder.library.output;

import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.encoder.EncoderBuffer;
import com.example.decoderencoder.library.muxer.FFmpegMuxer;
import com.example.decoderencoder.library.muxer.MediaCodecMuxer;
import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.network.Allocator;
import com.example.decoderencoder.library.network.DataOutput;
import com.example.decoderencoder.library.source.Media;
import com.example.decoderencoder.library.source.MediaSource;
import com.example.decoderencoder.library.util.ConditionVariable;
import com.example.decoderencoder.library.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;


/**
 * Main Class to Control muxing
 * Instead of being in a loop waiting for information the thread will block when all current data is muxed. The queue will however be available
 * to save
 *
 */
public class DefaultMediaOutput implements MediaOutput {

    private static final String TAG = "DEFAULTMEDIAOUTPUT";
    Handler transcoderHandler;

    final Allocator allocator;
    MediaOutput.Callback callback;

    MediaMuxer mediaMuxer;
    ArrayList<MuxerInput> muxerInputs = new ArrayList<>();
    ArrayList<MediaFormat> mediaFormats = new ArrayList<>();        // TODO: do we need this?


    long maxPTS = Long.MIN_VALUE;
    long minPTS = Long.MAX_VALUE;

    boolean muxerStarted = false;
    int numOfMuxingStreams = -1;     // FIXME
    int currentNumOfStreams = 0;
    //final ConditionVariable loadCondition;



    public DefaultMediaOutput(
                              Allocator allocator

    ) {
        this.allocator = allocator;
        this.transcoderHandler = transcoderHandler;
        //loadCondition = new ConditionVariable();
        //this.transcoderHandler = transcoderHandler;
    }


    @Override
    public MuxerInput newTrackDiscovered(MediaFormat trackFormat) {
        mediaFormats.add(trackFormat);
        int trackId = mediaMuxer.addTrack(trackFormat);
        EncoderOutput encoderOutput = new EncoderOutput(trackId);
        muxerInputs.add(encoderOutput);
        currentNumOfStreams++;
        return encoderOutput;
    }

    @Override
    public void setNumOfStreams(int streams) {
        numOfMuxingStreams = streams;
    }

    @Override
    public long getCurrentMaxPts() {
        return maxPTS;
    }

    @Override
    public long getCurrentMinPts() {
        return minPTS;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void prepare(Callback callback, Uri uri, Handler transcoderHandler) {
        //loadCondition.open();
        //this.mediaMuxer = mediaMuxer;/* new MediaCodecMuxer(uri.getPath(),  android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4); */
        this.callback = callback;
        this.transcoderHandler = transcoderHandler;
        /**
         * TODO:
         * mediamuxer factory
         * based on the uri, check which muxer can handle it
         */
        //this.mediaMuxer = new MediaCodecMuxer(uri.getPath(),  android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        //this.mediaMuxer = new FFmpegMuxer(uri);
    }

    @Override
    public void prepare(Callback callback, String uri, Handler transcoderHandler) {
        this.transcoderHandler = transcoderHandler;
        this.callback = callback;
        this.mediaMuxer = new FFmpegMuxer(uri);
    }

    @Override
    public void maybeStartMuxer() {
        if(numOfMuxingStreams < 0) {
            Log.d(TAG, "Cannot start muxing because the number of streams was not defined");
        }

        if(!muxerStarted && numOfMuxingStreams == currentNumOfStreams) {
            this.mediaMuxer.start();
            muxerStarted = true;
        }
    }

    @Override
    public void release() {
        mediaMuxer.release();
        muxerInputs = null;
        mediaFormats = null;
    }

    @Override
    public void stopMuxer() {
        mediaMuxer.stop();
        muxerStarted = false;
    }


    // Internals

    public class EncoderOutput implements MuxerInput {
        int trackId;
        LinkedList<EncoderBuffer> pendingEncoderOutputBuffers = new LinkedList<EncoderBuffer>();


        public EncoderOutput(int trackId) {
            this.trackId = trackId;
        }

        @Override
        public int sampleData(EncoderBuffer outputBuffer) throws IOException, InterruptedException {


                if(numOfMuxingStreams > currentNumOfStreams) {
                    pendingEncoderOutputBuffers.add(outputBuffer);
                }else {
                    while(pendingEncoderOutputBuffers.size() > 0) {                 // TODO
                        EncoderBuffer bf = pendingEncoderOutputBuffers.poll();
                        mediaMuxer.writeSampleData(trackId, outputBuffer);
                    }
                    mediaMuxer.writeSampleData(trackId, outputBuffer);
                }

            return 1;
        }
    }




}

package com.example.decoderencoder.library.output;

import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.encoder.EncoderBuffer;
import com.example.decoderencoder.library.muxer.MediaCodecMuxer;
import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.network.Allocator;
import com.example.decoderencoder.library.network.DataOutput;
import com.example.decoderencoder.library.source.Media;
import com.example.decoderencoder.library.source.MediaSource;
import com.example.decoderencoder.library.util.ConditionVariable;

import java.io.IOException;
import java.util.ArrayList;


/**
 * Main Class to Control muxing
 * Instead of being in a loop waiting for information the thread will block when all current data is muxed. The queue will however be available
 * to save
 *
 */
public class MediaOutputImpl extends HandlerThread implements MediaOutput {

    Handler mediaOutputHandler;
    Handler transcoderHandler;

    final Allocator allocator;
    final MediaOutput.Callback callback;

    MediaMuxer mediaMuxer;
    ArrayList<MuxerInput> muxerInputs = new ArrayList<>();
    ArrayList<MediaFormat> mediaFormats = new ArrayList<>();


    long maxPTS = Long.MIN_VALUE;
    long minPTS = Long.MAX_VALUE;

    //final ConditionVariable loadCondition;



    public MediaOutputImpl(Handler transcoderHandler,
                           Allocator allocator,
                           Callback callback
    ) {
        super("MediaOutput");
        this.allocator = allocator;
        this.callback = callback;
        //loadCondition = new ConditionVariable();
        this.transcoderHandler = transcoderHandler;
    }


    @Override
    protected void onLooperPrepared() {
        mediaOutputHandler = new Handler();
    }

    @Override
    public MuxerInput newTrackDiscovered(MediaFormat trackFormat) {
        mediaFormats.add(trackFormat);
        int trackId = mediaMuxer.addTrack(trackFormat);
        EncoderOutput encoderOutput = new EncoderOutput(trackId);
        muxerInputs.add(encoderOutput);
        return encoderOutput;
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
    public void prepare(MediaMuxer mediaMuxer) {
        //loadCondition.open();
        this.mediaMuxer = mediaMuxer;/* new MediaCodecMuxer(uri.getPath(),  android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4); */
    }



    // Internals
    public void load() {
       /* loadCondition.block();
        while(!loadCancelead) {
            boolean info_available = false;
            for(int i = 0; i < muxerInputBuffers.length; i++) {
                if(muxerInputBuffers[i].isDataAvailable() > 0) {
                    this.sampleOutput.writeData();
                    info_available = true;
                }
            }
            if(!info_available)
                loadCondition.close();
        }*/

    }


    public class EncoderOutput implements MuxerInput {
        int trackId;

        public EncoderOutput(int trackId) {
            this.trackId = trackId;
        }

        @Override
        public int sampleData(EncoderBuffer outputBuffer) throws IOException, InterruptedException {
            mediaMuxer.writeSampleData(trackId, outputBuffer.data, outputBuffer.offset, outputBuffer.size, outputBuffer.flags, outputBuffer.presentationTimeUs);
            return 1;
        }
    }




}

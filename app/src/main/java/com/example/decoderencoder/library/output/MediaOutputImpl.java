package com.example.decoderencoder.library.output;

import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.muxer.MediaCodecMuxer;
import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.muxer.SampleOutput;
import com.example.decoderencoder.library.network.Allocator;
import com.example.decoderencoder.library.network.DataOutput;
import com.example.decoderencoder.library.source.Media;
import com.example.decoderencoder.library.source.MediaSource;
import com.example.decoderencoder.library.util.ConditionVariable;


/**
 * Main Class to Control muxing
 * Instead of being in a loop waiting for information the thread will block when all current data is muxed. The queue will however be available
 * to save
 *
 */
public class MediaOutputImpl extends HandlerThread implements MediaOutput {

    Handler mediaOutputHandler;
    Handler transcoderHandler;
    Uri uri;
    final Allocator allocator;
    DataOutput dataOutput;
    MediaFormat[] mediaFormats;
    final MediaOutput.Callback callback;
    final ConditionVariable loadCondition;
    final MediaSource.PreparedState preparedState;

    MuxerInput[] muxerInputBuffers;
    SampleOutput sampleOutput;
    MediaMuxer mediaMuxer;

    public MediaOutputImpl(Handler transcoderHandler,
                           Uri uri,
                           Allocator allocator,
                           DataOutput dataOutput,
                           MediaFormat[] mediaFormat,
                           Callback callback,
                           MediaSource.PreparedState preparedState
    ) {
        super("MediaOutput");
        this.uri = uri;
        this.allocator = allocator;
        this.dataOutput = dataOutput;
        this.mediaFormats = mediaFormat;
        this.callback = callback;
        loadCondition = new ConditionVariable();
        this.transcoderHandler = transcoderHandler;
        this.preparedState = preparedState;
    }


    @Override
    protected void onLooperPrepared() {
        mediaOutputHandler = new Handler();
    }

    @Override
    public MuxerInput[] getInputBuffers() {
        if(this.muxerInputBuffers != null)
            return this.muxerInputBuffers;
        else {
            int noTracks = this.preparedState.tracks.length;
            this.muxerInputBuffers = new MuxerInput[this.preparedState.tracks.length];
            for(int i = 0; i < noTracks; i++) {
                this.muxerInputBuffers[i] = new TrackQueue(this.preparedState.tracks.get(i), allocator);
            }
        }
        return this.muxerInputBuffers;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void prepare(MediaMuxer mediaMuxer) {
        loadCondition.open();
        this.mediaMuxer = mediaMuxer;/* new MediaCodecMuxer(uri.getPath(),  android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4); */
        for(MediaFormat f : mediaFormats) {
            mediaMuxer.addTrack(f);
        }
    }

    @Override
    public boolean continueLoading(long positionUs) {
        loadCondition.open();
        return false;
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




}

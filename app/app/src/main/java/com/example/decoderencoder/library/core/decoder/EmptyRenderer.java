package com.example.decoderencoder.library.core.decoder;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.util.Log;
import com.example.decoderencoder.library.video.openGL.InputSurface;
import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.FormatHolder;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.C;

import java.nio.ByteBuffer;
import java.util.LinkedList;

// For passthrough
public class EmptyRenderer extends BaseRenderer {

    private static final String TAG = "EMPTYRENDERER";
    private static final long[] MAX_PTS_OFFSET = {600000, 600000, 600000, 6000000}; // in ms

    MediaFormat mediaFormat = null;
    LinkedList<ByteBuffer> pendingDecoderOutputBuffers;
    LinkedList<MediaCodec.BufferInfo> pendingDecoderOutputBufferInfos;
    FormatHolder formatHolder;
    DecoderInputBuffer inputBuffer;
    int endofstream = 1;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public EmptyRenderer(int trackType) {
        super(trackType);
        pendingDecoderOutputBuffers = new LinkedList<ByteBuffer> ();
        pendingDecoderOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();
        formatHolder = new FormatHolder();
    }

    @Override
    protected void onStarted() {

    }

    @Override
    public SampleStream getStream() {
        return null;
    }


    @Override
    public boolean hasReadStreamToEnd() {
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public long feedInputBuffer(long stream_highest_pts) {
        if(this.streamIsFinal)
            return 0;
        if(inputBuffer == null) {
            inputBuffer = new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT);
            int result = readSource(formatHolder, inputBuffer, false);
            if (result == C.RESULT_NOTHING_READ) {
                Log.d(TAG, "RESULT_NOTHING_READ");
                return 2;
            }
            if (result == C.RESULT_FORMAT_READ) {
                Log.d(TAG, "RESULT_FORMAT_READ");

                inputBuffer.clear();
                this.mediaFormat = getFormat(formatHolder.format);
                inputBuffer = null;
                return 1;
            }

             if(inputBuffer.getFlag(C.BUFFER_FLAG_DECODE_ONLY) ) {
                inputBuffer.data = null;
                inputBuffer = null;
                return 2;
            }
            if(inputBuffer.isEndOfStream()) {
                endofstream = 0;
                streamIsFinal = true;
            }
        }

        long presentationTimeUs = inputBuffer.timeUs;

        if(presentationTimeUs > stream_highest_pts + MAX_PTS_OFFSET[getTrackType()])
            return -1;          // FIXME: return -1

        pendingDecoderOutputBuffers.add(inputBuffer.data);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.presentationTimeUs = presentationTimeUs;
        if(inputBuffer.data == null) {
            inputBuffer = null;
            return 1;
        }
        bufferInfo.size = inputBuffer.data.limit();
        bufferInfo.flags = inputBuffer.getFlag();
        pendingDecoderOutputBufferInfos.add(bufferInfo);
        Log.i(TAG, "Empty renderer processed stream " + formatHolder.format.sampleMimeType);
        inputBuffer = null;
        return 1;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static MediaFormat getFormat(Format format) {
       if(format.sampleMimeType.startsWith("audio/")) {
           return MediaCodecAudioRenderer.getMediaFormat(format, format.sampleMimeType);
       }else if(format.sampleMimeType.startsWith("video/")) {
           return MediaCodecVideoRenderer.getMediaFormat(format, format.sampleMimeType, false);
       }else if(format.sampleMimeType.startsWith("subtitle/") || format.sampleMimeType.equals("application/dvbsubs")) {
           MediaFormat mediaFormat = MediaFormat.createSubtitleFormat("application/dvbsubs", format.language);
           mediaFormat.setString(MediaFormat.KEY_TRACK_ID, format.id);
           return mediaFormat;
       }
       return null;
    }

    @Override
    public int drainOutputBuffer() {
        return endofstream;
    }

    @Override
    public MediaCodec.BufferInfo pollBufferInfo() {
        return pendingDecoderOutputBufferInfos.poll();
    }

    @Override
    public ByteBuffer pollFrameData() { return pendingDecoderOutputBuffers.poll(); }

    @Override
    public int poolBufferIndex() {
        return 0;
    }

    @Override
    public void SurfaceCreated(InputSurface inputSurface) {

    }

    @Override
    public void SurfaceCreated(Surface surface) {

    }

    @Override
    public Decoder getDecoder() {
        return null;
    }

    @Override
    public MediaFormat getFormat() {
        return this.mediaFormat;
    }
}

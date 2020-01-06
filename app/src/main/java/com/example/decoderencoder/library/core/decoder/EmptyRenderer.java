package com.example.decoderencoder.library.core.decoder;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.openGL.InputSurface;
import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.FormatHolder;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.C;

import java.nio.ByteBuffer;
import java.util.LinkedList;

// For passthrough
public class EmptyRenderer extends BaseRenderer {

    private static final String TAG = "EMPTYRENDERER";
    MediaFormat mediaFormat = null;
    LinkedList<ByteBuffer> pendingDecoderOutputBuffers;
    LinkedList<MediaCodec.BufferInfo> pendingDecoderOutputBufferInfos;
    FormatHolder formatHolder;
    DecoderInputBuffer inputBuffer;

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
    public boolean feedInputBuffer() {
        if(this.streamIsFinal)
            return false;

        inputBuffer = new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT);
        int result = readSource(formatHolder, inputBuffer, false);
        if (result == C.RESULT_NOTHING_READ) {
            Log.d(TAG, "RESULT_NOTHING_READ");
            return false;
        }
        if (result == C.RESULT_FORMAT_READ) {
            Log.d(TAG, "RESULT_FORMAT_READ");

            inputBuffer.clear();
            setFormat(formatHolder.format);
            return true;
        }

        if(inputBuffer.getFlag(C.BUFFER_FLAG_DECODE_ONLY) ) {
            inputBuffer.data = null;
            inputBuffer = null;
            return true;
        }

        /*if(inputBuffer.data)
*/
        long presentationTimeUs = inputBuffer.timeUs;


        pendingDecoderOutputBuffers.add(inputBuffer.data);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.presentationTimeUs = presentationTimeUs;
        bufferInfo.size = inputBuffer.data.limit();
        bufferInfo.flags = inputBuffer.getFlag();
        pendingDecoderOutputBufferInfos.add(bufferInfo);
        Log.i(TAG, "Empty renderer processed stream " + formatHolder.format.sampleMimeType);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setFormat(Format format) {
       if(format.sampleMimeType.startsWith("audio/")) {
           this.mediaFormat = MediaCodecAudioRenderer.getMediaFormat(format, format.sampleMimeType);
       }else if(format.sampleMimeType.startsWith("video/")) {
           this.mediaFormat = MediaCodecVideoRenderer.getMediaFormat(format, format.sampleMimeType, false);
       }else if(format.sampleMimeType.startsWith("subtitle/")) {
           mediaFormat = MediaFormat.createSubtitleFormat(MediaFormat.MIMETYPE_TEXT_CEA_608, format.language);
       }
    }

    @Override
    public int drainOutputBuffer() {
        return 0;
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

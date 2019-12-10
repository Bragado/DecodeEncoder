package com.example.decoderencoder.library.core.decoder;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.OpenGL.InputSurface;
import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.FormatHolder;
import com.example.decoderencoder.library.source.SampleStream;

import java.nio.ByteBuffer;

// For passthrough
public class EmptyRenderer extends BaseRenderer {

    final DecoderInputBuffer buffer;
    FormatHolder formatHolder = null;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public EmptyRenderer(int trackType, ByteBuffer output) {
        super(trackType);
        buffer = DecoderInputBuffer.newFlagsOnlyInstance();
        buffer.data = output;
        this.formatHolder = new FormatHolder();
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

    @Override
    public boolean feedInputBuffer() {
        return false;
    }

    @Override
    public boolean drainOutputBuffer() {
        //readSource(formatHolder, buffer, false);
        return false;
    }

    @Override
    public MediaCodec.BufferInfo pollBufferInfo() {
        return null;
    }

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
        return null;
    }
}

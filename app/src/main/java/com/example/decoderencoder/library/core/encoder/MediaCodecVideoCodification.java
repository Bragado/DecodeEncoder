package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.OpenGL.InputSurface;
import com.example.decoderencoder.library.core.decoder.MediaCodecVideoRenderer;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.source.Media;

import java.io.IOException;

public class MediaCodecVideoCodification extends MediaCodecCodification {

    InputSurface inputSurface;


    public MediaCodecVideoCodification(Renderer renderer, Encoder encoder, MediaFormat mediaFormat, MediaMuxer mediaMuxer, MuxerInput muxerInput) {
        super(renderer, encoder, mediaFormat, muxerInput, mediaMuxer);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    protected void init() {
        this.encoder.makeCodecReady(this.format);
        this.encoderSurface = this.encoder.createInputSurface();

        if(openGLSurfaceIsNecessary()) {        // openGL adds an overload, it is necessary only when we need to change frame resolution
            // inicialize inputSurface
            this.inputSurface = new InputSurface(this.encoderSurface);
            this.inputSurface.makeCurrent();
            renderer.SurfaceCreated(this.inputSurface);
        }else {
            renderer.SurfaceCreated(encoderSurface);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean openGLSurfaceIsNecessary() {
        MediaFormat decoderFormat = renderer.getFormat();
        return !(decoderFormat.getInteger(MediaFormat.KEY_WIDTH) == this.format.getInteger(MediaFormat.KEY_WIDTH) &&  decoderFormat.getInteger(MediaFormat.KEY_HEIGHT) == this.format.getInteger(MediaFormat.KEY_HEIGHT));
    }


    @Override
    public boolean feedInputBuffer() {
        return true;
    }
}

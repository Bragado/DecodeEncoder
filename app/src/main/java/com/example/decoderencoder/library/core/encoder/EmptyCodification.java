package com.example.decoderencoder.library.core.encoder;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.core.decoder.MediaCodecAudioRenderer;
import com.example.decoderencoder.library.core.decoder.MediaCodecVideoRenderer;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.output.MediaOutput;

public class EmptyCodification extends BaseCodification {

    MediaMuxer mediaMuxer;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public EmptyCodification(Renderer renderer, Format format, MediaOutput mediaOutput) {
        super(renderer, null, null, mediaOutput);          // TODO: set the format
        /* set the format */
        setFormat(format);
    }

    @Override
    public void onRelease() {

    }

    @Override
    public void start() {

    }

    @Override
    public boolean feedInputBuffer() {
        return false;
    }

    @Override
    public boolean drainOutputBuffer() {
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void setFormat(Format format) {
        String mimeType = format.sampleMimeType;
        if(mimeType.startsWith("video/")) {
            this.format = MediaCodecVideoRenderer.getMediaFormat(format, format.sampleMimeType, false);
        }else if(mimeType.startsWith("audio/")) {
            this.format = MediaCodecAudioRenderer.getMediaFormat(format, format.sampleMimeType);
        }else if(mimeType.startsWith("application/cea")) {

        }else if(mimeType.startsWith("application/dvbsubs")) {

        }else {

        }
    }

}

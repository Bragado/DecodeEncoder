package com.example.decoderencoder.library.core;

import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.CodificationFactory;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.core.encoder.Codification;
import com.example.decoderencoder.library.core.encoder.DefaultEncoder;
import com.example.decoderencoder.library.core.encoder.EmptyCodification;
import com.example.decoderencoder.library.core.encoder.Encoder;
import com.example.decoderencoder.library.core.encoder.EncoderBuffer;
import com.example.decoderencoder.library.core.encoder.MediaCodecAudioCodification;
import com.example.decoderencoder.library.core.encoder.MediaCodecVideoCodification;
import com.example.decoderencoder.library.output.MediaOutput;
import com.example.decoderencoder.library.source.MediaSource;
import com.example.decoderencoder.library.source.TrackGroupArray;

public class DefaultCodificationFactory implements CodificationFactory {

    Surface encoderInputSurface;
    Codification[] codifications;
    Encoder[] encoders;


    public DefaultCodificationFactory() {};




    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public Codification[] createCodification(MediaFormat[] formats, MediaSource.PreparedState preparedState, Renderer[] renderers, MediaOutput mediaOutput) {
        TrackGroupArray tracks = preparedState.tracks;
        codifications = new Codification[tracks.length];
        encoders = new Encoder[tracks.length];
        boolean[] tracks_enabled = preparedState.trackEnabledStates;
        int j = 0;
        for(int i = 0; i < tracks.length; i++){
            if(tracks_enabled[i]) {
                String mimeType = preparedState.tracks.get(i).getFormat(0).sampleMimeType;
                if(mimeType.startsWith("video/")) {
                    encoders[i] = new DefaultEncoder();
                    codifications[i] = new MediaCodecVideoCodification(renderers[i], encoders[i], formats[j++], mediaOutput);
                }else if(mimeType.startsWith("audio/")) {
                    encoders[i] = new DefaultEncoder();
                    codifications[i] = new MediaCodecAudioCodification(renderers[i], encoders[i], formats[j++], mediaOutput);
                }else {     // should not be here
                    codifications[i] = new EmptyCodification(renderers[i],  tracks.get(i).getFormat(0), mediaOutput);
                }
            }else {
                codifications[i] = new EmptyCodification(renderers[i], tracks.get(i).getFormat(0), mediaOutput);
            }
        }

        return codifications;
    }

    @Override
    public Surface getEncoderInputSurface() {
        return this.encoderInputSurface;
    }

}

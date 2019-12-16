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

import java.util.Arrays;

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
        MediaSource.PreparedState.TRACKSTATE[] tracks_enabled = preparedState.trackEnabledStates;
        int j = 0;
        int k = 0;
        int numOfCodifications = 0;
        for(int i = 0; i < tracks.length; i++){
            if(tracks_enabled[i] == MediaSource.PreparedState.TRACKSTATE.SELECTED) {

                String mimeType = preparedState.tracks.get(i).getFormat(0).sampleMimeType;
                if(mimeType.startsWith("video/")) {
                    encoders[i] = new DefaultEncoder();
                    codifications[numOfCodifications] = new MediaCodecVideoCodification(renderers[k++], encoders[i], formats[j++], mediaOutput);
                }else if(mimeType.startsWith("audio/")) {
                    encoders[i] = new DefaultEncoder();
                    codifications[numOfCodifications] = new MediaCodecAudioCodification(renderers[k++], encoders[i], formats[j++], mediaOutput);
                }else {     // should not be here
                    codifications[numOfCodifications] = new EmptyCodification(renderers[k++],  mediaOutput);
                }
                numOfCodifications++;
            }else if(tracks_enabled[i] == MediaSource.PreparedState.TRACKSTATE.PASSTROUGH) {
                codifications[numOfCodifications] = new EmptyCodification(renderers[k++],  mediaOutput);
                numOfCodifications++;
            }
        }
        codifications = Arrays.copyOf(codifications, numOfCodifications);
        return codifications;
    }

    @Override
    public Surface getEncoderInputSurface() {
        return this.encoderInputSurface;
    }

}

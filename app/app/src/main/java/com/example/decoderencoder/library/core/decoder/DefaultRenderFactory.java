package com.example.decoderencoder.library.core.decoder;

import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.RenderersFactory;
import com.example.decoderencoder.library.core.encoder.EncoderBuffer;
import com.example.decoderencoder.library.source.MediaSource;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.C;
import com.example.decoderencoder.library.util.Log;

public class DefaultRenderFactory implements RenderersFactory, Renderer.Callback {

    public static final String TAG = "RENDERERFACTORY";

    Surface surface = null;
    Decoder decoders[] = null;
    RenderersFactory.Callback callback;


    public DefaultRenderFactory(RenderersFactory.Callback callback) {
        this.callback = callback;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Renderer[] createRenderers(SampleStream[] sampleStreams, MediaSource.PreparedState preparedState) {

        Log.d(TAG, "createRenderers");
        Renderer[] renderers = new Renderer[sampleStreams.length];
        String mimeType;
        decoders = new Decoder[sampleStreams.length];

        for(int i = 0; i < preparedState.tracks.length; i++) {
            mimeType = preparedState.tracks.get(i).getFormat(0).sampleMimeType;
            if(mimeType.startsWith("video/") && preparedState.trackEnabledStates[i]) {
                decoders[i] = new DefaultDecoder(preparedState.tracks.get(i));
                renderers[i] = new MediaCodecVideoRenderer(this, C.TRACK_TYPE_VIDEO, decoders[i]);
                renderers[i].enable(preparedState.tracks.get(i).getFormat(), sampleStreams[i], 0,0);
            }else if(mimeType.startsWith("audio/") && preparedState.trackEnabledStates[i]) {
                decoders[i] = new DefaultDecoder(preparedState.tracks.get(i));
                renderers[i] = new MediaCodecAudioRenderer(C.TRACK_TYPE_AUDIO, decoders[i]);
                renderers[i].enable(preparedState.tracks.get(i).getFormat(), sampleStreams[i], 0,0);
            }else {
                renderers[i] = new EmptyRenderer(C.TRACK_TYPE_UNKNOWN, null);       // FIXME: Renderer output
            }
        }
        return renderers;
    }

    @Override
    public Renderer[] createRenderers(SampleStream[] sampleStreams, MediaSource.PreparedState preparedState, Renderer[] currentRenderers) {
        return new Renderer[0];
    }

    @Override
    public void ready() {
        callback.readyToStart();
    }
}

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

import java.util.Arrays;

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

        int numOfRenderers = 0;
        for(int i = 0; i < preparedState.tracks.length; i++) {
            mimeType = preparedState.tracks.get(i).getFormat(0).sampleMimeType;
            if(preparedState.trackEnabledStates[i] == MediaSource.PreparedState.TRACKSTATE.SELECTED) {

                if(mimeType.startsWith("video/")) {
                    decoders[i] = new DefaultDecoder(preparedState.tracks.get(i));
                    renderers[numOfRenderers] = new MediaCodecVideoRenderer(this, C.TRACK_TYPE_VIDEO, decoders[i]);
                    renderers[numOfRenderers].enable(preparedState.tracks.get(i).getFormat(), sampleStreams[i], 0,0);
                }else if(mimeType.startsWith("audio/")) {
                    decoders[i] = new DefaultDecoder(preparedState.tracks.get(i));
                    renderers[numOfRenderers] = new MediaCodecAudioRenderer(C.TRACK_TYPE_AUDIO, decoders[i]);
                    renderers[numOfRenderers].enable(preparedState.tracks.get(i).getFormat(), sampleStreams[i], 0,0);
                }else {     // passthrough, only audio and video are supported
                    renderers[numOfRenderers] = new EmptyRenderer(C.TRACK_TYPE_UNKNOWN);       // FIXME: Renderer output
                    renderers[numOfRenderers].enable(preparedState.tracks.get(i).getFormat(), sampleStreams[i], 0,0);
                }
                numOfRenderers++;
            }else if(preparedState.trackEnabledStates[i] == MediaSource.PreparedState.TRACKSTATE.PASSTROUGH ) {
                renderers[numOfRenderers] = new EmptyRenderer(C.TRACK_TYPE_UNKNOWN);       // FIXME: Renderer output
                renderers[numOfRenderers].enable(preparedState.tracks.get(i).getFormat(), sampleStreams[i], 0,0);
                numOfRenderers++;
            }else {
                // discard, do nothing
            }
        }
        // realloc renderers
        renderers = Arrays.copyOf(renderers, numOfRenderers);

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

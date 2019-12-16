package com.example.decoderencoder.library.core;

import android.media.MediaFormat;
import android.view.Surface;

import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.core.encoder.Codification;
import com.example.decoderencoder.library.core.encoder.EncoderBuffer;
import com.example.decoderencoder.library.output.MediaOutput;
import com.example.decoderencoder.library.source.MediaSource;

public interface CodificationFactory {

    /**
     * Builds the {@link Codification} instances for a {@link }.
     *
     * @param formats All the formats necessary to configure the encoder.
     * @param preparedState All the information about the tracks and the ones selected
     * @return The {@link Codification} instances.
     */
    Codification[] createCodification(MediaFormat[] formats, MediaSource.PreparedState preparedState, Renderer[] renderers, MediaOutput mediaOutput);

    Surface getEncoderInputSurface();

}

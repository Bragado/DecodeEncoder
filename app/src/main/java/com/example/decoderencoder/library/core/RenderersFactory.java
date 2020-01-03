/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.decoderencoder.library.core;

import android.media.MediaFormat;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.source.MediaSource;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.source.TrackGroupArray;

/**
 * Builds {@link Renderer} instances for use by a {@link }.
 */
public interface RenderersFactory {


  public interface Callback {
    public void readyToStart();
  }

  /**
   * Builds the {@link Renderer} instances for a {@link }.
   *
   * @param sampleStreams All streams available.
   * @param preparedState All the information about the tracks and the ones selected
   * @param formats
   * @return The {@link Renderer} instances.
   */
  Renderer[] createRenderers(SampleStream[] sampleStreams, MediaSource.PreparedState preparedState, MediaFormat[] formats);

  /**
   * Builds the {@link Renderer} instances for a {@link com.example.decoderencoder.library.core.decoder.Decoder} by trying to reuse the ones in currentRenderers
   * @param sampleStreams All streams available
   * @param preparedState All the information about the tracks and the ones selected
   * @param currentRenderers prepared
   * @return The {@link Renderer} instances.
   */
  Renderer[] createRenderers(SampleStream[] sampleStreams, MediaSource.PreparedState preparedState, Renderer[] currentRenderers);




}

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
package com.example.decoderencoder.library.extractor.ts;

import com.example.decoderencoder.library.util.C;
import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.extractor.ExtractorOutput;
import com.example.decoderencoder.library.extractor.TrackOutput;
import com.example.decoderencoder.library.extractor.ts.TsPayloadReader.TrackIdGenerator;
import com.example.decoderencoder.library.text.cea.CeaUtil;
import com.example.decoderencoder.library.util.MimeTypes;
import com.example.decoderencoder.library.util.ParsableByteArray;

import java.util.List;

/**
 * Consumes SEI buffers, outputting contained CEA-608 messages to a {@link TrackOutput}.
 */
/* package */ public final class SeiReader {

  private final List<Format> closedCaptionFormats;
  private final TrackOutput[] outputs;
  private boolean keepTrack = true;

  /**
   * @param closedCaptionFormats A list of formats for the closed caption channels to expose.
   */
  public SeiReader(List<Format> closedCaptionFormats) {
    this.closedCaptionFormats = closedCaptionFormats;
    outputs = new TrackOutput[closedCaptionFormats.size()];
  }

  public void createTracks(ExtractorOutput extractorOutput, TrackIdGenerator idGenerator) {
    for (int i = 0; i < outputs.length; i++) {
      idGenerator.generateNewId();
      TrackOutput output = extractorOutput.track(this, idGenerator.getTrackId(), C.TRACK_TYPE_TEXT);
      Format channelFormat = closedCaptionFormats.get(i);
      String channelMimeType = channelFormat.sampleMimeType;
      if(!(MimeTypes.APPLICATION_CEA608.equals(channelMimeType) || MimeTypes.APPLICATION_CEA708.equals(channelMimeType))) {
        throw new IllegalArgumentException();
      }

      String formatId = channelFormat.id != null ? channelFormat.id : idGenerator.getFormatId();
      output.format(
          Format.createTextSampleFormat(
              formatId,
              channelMimeType,
              /* codecs= */ null,
              /* bitrate= */ Format.NO_VALUE,
              channelFormat.selectionFlags,
              channelFormat.language,
              channelFormat.accessibilityChannel,
              /* drmInitData= */ null,
              Format.OFFSET_SAMPLE_RELATIVE,
              channelFormat.initializationData));
      outputs[i] = output;
    }
  }

  public void consume(long pesTimeUs, ParsableByteArray seiBuffer) {
    if(keepTrack)
    CeaUtil.consume(pesTimeUs, seiBuffer, outputs);
  }

  public void discardStream(boolean discard) {
    this.keepTrack = discard;
  }
}

/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.example.decoderencoder.library.testutil;

import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.FormatHolder;
import com.example.decoderencoder.library.core.decoder.DecoderInputBuffer;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.C;
import com.example.decoderencoder.library.util.EventDispatcher;

import java.io.IOException;

import androidx.annotation.Nullable;

/**
 * Fake {@link SampleStream} that outputs a given {@link Format}, an optional sample containing a
 * single zero byte, then end of stream.
 */
public final class FakeSampleStream implements SampleStream {

  private final Format format;
  private final @Nullable
  EventDispatcher eventDispatcher;

  private boolean notifiedDownstreamFormat;
  private boolean readFormat;
  private boolean readSample;

  /**
   * Creates fake sample stream which outputs the given {@link Format}, optionally one sample with
   * zero bytes, then end of stream.
   *
   * @param format The {@link Format} to output.
   * @param eventDispatcher An {@link EventDispatcher} to notify of read events.
   * @param shouldOutputSample Whether the sample stream should output a sample.
   */
  public FakeSampleStream(
      Format format, @Nullable EventDispatcher eventDispatcher, boolean shouldOutputSample) {
    this.format = format;
    this.eventDispatcher = eventDispatcher;
    readSample = !shouldOutputSample;
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer,
      boolean formatRequired) {

    if (formatRequired || !readFormat) {
      formatHolder.format = format;
      readFormat = true;
      return C.RESULT_FORMAT_READ;
    } else if (!readSample) {
      buffer.timeUs = 0;
      buffer.ensureSpaceForWrite(1);
      buffer.data.put((byte) 0);
      buffer.flip();
      readSample = true;
      return C.RESULT_BUFFER_READ;
    } else {
      buffer.setFlags(C.BUFFER_FLAG_END_OF_STREAM);
      return C.RESULT_BUFFER_READ;
    }
  }

  @Override
  public void maybeThrowError() throws IOException {
    // Do nothing.
  }


}

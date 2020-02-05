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
package com.example.decoderencoder.extractor;

import com.example.decoderencoder.library.extractor.DefaultExtractorsFactory;
import com.example.decoderencoder.library.extractor.Extractor;
import com.example.decoderencoder.library.extractor.amr.AmrExtractor;
import com.example.decoderencoder.library.extractor.flv.FlvExtractor;
import com.example.decoderencoder.library.extractor.mkv.MatroskaExtractor;
import com.example.decoderencoder.library.extractor.mp3.Mp3Extractor;
import com.example.decoderencoder.library.extractor.mp4.FragmentedMp4Extractor;
import com.example.decoderencoder.library.extractor.mp4.Mp4Extractor;
import com.example.decoderencoder.library.extractor.ogg.OggExtractor;
import com.example.decoderencoder.library.extractor.ts.Ac3Extractor;
import com.example.decoderencoder.library.extractor.ts.Ac4Extractor;
import com.example.decoderencoder.library.extractor.ts.AdtsExtractor;
import com.example.decoderencoder.library.extractor.ts.PsExtractor;
import com.example.decoderencoder.library.extractor.ts.TsExtractor;
import com.example.decoderencoder.library.extractor.wav.WavExtractor;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.google.common.truth.Truth.assertThat;

/** Unit test for {@link DefaultExtractorsFactory}. */
@RunWith(AndroidJUnit4.class)
public final class DefaultExtractorsFactoryTest {

  @Test
  public void testCreateExtractors_returnExpectedClasses() {
    DefaultExtractorsFactory defaultExtractorsFactory = new DefaultExtractorsFactory();

    Extractor[] extractors = defaultExtractorsFactory.createExtractors();
    List<Class> listCreatedExtractorClasses = new ArrayList<>();
    for (Extractor extractor : extractors) {
      listCreatedExtractorClasses.add(extractor.getClass());
    }

    Class[] expectedExtractorClassses =
        new Class[] {
          MatroskaExtractor.class,
          FragmentedMp4Extractor.class,
          Mp4Extractor.class,
          Mp3Extractor.class,
          AdtsExtractor.class,
          Ac3Extractor.class,
          TsExtractor.class,
          FlvExtractor.class,
          OggExtractor.class,
          PsExtractor.class,
          WavExtractor.class,
          AmrExtractor.class,
          Ac4Extractor.class
        };

    assertThat(listCreatedExtractorClasses).containsNoDuplicates();
    assertThat(listCreatedExtractorClasses).containsExactlyElementsIn(expectedExtractorClassses);
  }
}

/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.example.decoderencoder.extractor.ts;

import com.example.decoderencoder.library.extractor.ts.Ac4Extractor;
import com.example.decoderencoder.library.testutil.ExtractorAsserts;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

/** Unit test for {@link Ac4Extractor}. */
@RunWith(AndroidJUnit4.class)
public final class Ac4ExtractorTest {

  @Test
  public void testAc4Sample() throws Exception {
    ExtractorAsserts.assertBehavior(Ac4Extractor::new, "ts/sample.ac4");
  }
}

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
package com.example.decoderencoder.metadata.id3;

import android.os.Parcel;

import com.example.decoderencoder.library.metadata.id3.ChapterTocFrame;
import com.example.decoderencoder.library.metadata.id3.Id3Frame;
import com.example.decoderencoder.library.metadata.id3.TextInformationFrame;
import com.example.decoderencoder.library.metadata.id3.UrlLinkFrame;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.google.common.truth.Truth.assertThat;

/** Test for {@link ChapterTocFrame}. */
@RunWith(AndroidJUnit4.class)
public final class ChapterTocFrameTest {

  @Test
  public void testParcelable() {
    String[] children = new String[] {"child0", "child1"};
    Id3Frame[] subFrames = new Id3Frame[] {
        new TextInformationFrame("TIT2", null, "title"),
        new UrlLinkFrame("WXXX", "description", "url")
    };
    ChapterTocFrame chapterTocFrameToParcel = new ChapterTocFrame("id", false, true, children,
        subFrames);

    Parcel parcel = Parcel.obtain();
    chapterTocFrameToParcel.writeToParcel(parcel, 0);
    parcel.setDataPosition(0);

    ChapterTocFrame chapterTocFrameFromParcel = ChapterTocFrame.CREATOR.createFromParcel(parcel);
    assertThat(chapterTocFrameFromParcel).isEqualTo(chapterTocFrameToParcel);

    parcel.recycle();
  }

}

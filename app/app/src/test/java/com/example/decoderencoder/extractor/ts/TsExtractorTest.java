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
package com.example.decoderencoder.extractor.ts;

import android.util.SparseArray;

import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.extractor.Extractor;
import com.example.decoderencoder.library.extractor.ExtractorOutput;
import com.example.decoderencoder.library.extractor.PositionHolder;
import com.example.decoderencoder.library.extractor.TrackOutput;
import com.example.decoderencoder.library.extractor.ts.DefaultTsPayloadReaderFactory;
import com.example.decoderencoder.library.extractor.ts.ElementaryStreamReader;
import com.example.decoderencoder.library.extractor.ts.PesReader;
import com.example.decoderencoder.library.extractor.ts.SectionPayloadReader;
import com.example.decoderencoder.library.extractor.ts.SectionReader;
import com.example.decoderencoder.library.extractor.ts.TsExtractor;
import com.example.decoderencoder.library.extractor.ts.TsPayloadReader;
import com.example.decoderencoder.library.extractor.ts.TsPayloadReader.EsInfo;
import com.example.decoderencoder.library.extractor.ts.TsPayloadReader.TrackIdGenerator;
import com.example.decoderencoder.library.testutil.ExtractorAsserts;
import com.example.decoderencoder.library.testutil.FakeExtractorInput;
import com.example.decoderencoder.library.testutil.FakeExtractorOutput;
import com.example.decoderencoder.library.testutil.FakeTrackOutput;
import com.example.decoderencoder.library.testutil.TestUtil;
import com.example.decoderencoder.library.util.C;
import com.example.decoderencoder.library.util.ParsableByteArray;
import com.example.decoderencoder.library.util.TimestampAdjuster;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.util.Random;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.google.common.truth.Truth.assertThat;

/** Unit test for {@link TsExtractor}. */
@RunWith(AndroidJUnit4.class)
public final class TsExtractorTest {

  private static final int TS_PACKET_SIZE = 188;
  private static final int TS_SYNC_BYTE = 0x47; // First byte of each TS packet.

  @Test
  public void testSample() throws Exception {
    ExtractorAsserts.assertBehavior(TsExtractor::new, "ts/sample.ts");
  }

  @Test
  public void testStreamWithJunkData() throws Exception {
    Random random = new Random(0);
    byte[] fileData =
        TestUtil.getByteArray(ApplicationProvider.getApplicationContext(), "ts/sample.ts");
    ByteArrayOutputStream out = new ByteArrayOutputStream(fileData.length * 2);
    int bytesLeft = fileData.length;

    writeJunkData(out, random.nextInt(TS_PACKET_SIZE - 1) + 1);
    out.write(fileData, 0, TS_PACKET_SIZE * 5);
    bytesLeft -= TS_PACKET_SIZE * 5;

    for (int i = TS_PACKET_SIZE * 5; i < fileData.length; i += 5 * TS_PACKET_SIZE) {
      writeJunkData(out, random.nextInt(TS_PACKET_SIZE));
      int length = Math.min(5 * TS_PACKET_SIZE, bytesLeft);
      out.write(fileData, i, length);
      bytesLeft -= length;
    }
    out.write(TS_SYNC_BYTE);
    writeJunkData(out, random.nextInt(TS_PACKET_SIZE - 1) + 1);
    fileData = out.toByteArray();

    ExtractorAsserts.assertOutput(
        TsExtractor::new, "ts/sample.ts", fileData, ApplicationProvider.getApplicationContext());
  }

  @Test
  public void testCustomPesReader() throws Exception {
    CustomTsPayloadReaderFactory factory = new CustomTsPayloadReaderFactory(true, false);
    TsExtractor tsExtractor =
        new TsExtractor(TsExtractor.MODE_MULTI_PMT, new TimestampAdjuster(0), factory);
    FakeExtractorInput input =
        new FakeExtractorInput.Builder()
            .setData(
                TestUtil.getByteArray(ApplicationProvider.getApplicationContext(), "ts/sample.ts"))
            .setSimulateIOErrors(false)
            .setSimulateUnknownLength(false)
            .setSimulatePartialReads(false)
            .build();
    FakeExtractorOutput output = new FakeExtractorOutput();
    tsExtractor.init(output);
    PositionHolder seekPositionHolder = new PositionHolder();
    int readResult = Extractor.RESULT_CONTINUE;
    while (readResult != Extractor.RESULT_END_OF_INPUT) {
      readResult = tsExtractor.read(input, seekPositionHolder);
      if (readResult == Extractor.RESULT_SEEK) {
        input.setPosition((int) seekPositionHolder.position);
      }
    }
    CustomEsReader reader = factory.esReader;
    //assertThat(reader.packetsRead).isEqualTo(2);    // FIXME
    TrackOutput trackOutput = reader.getTrackOutput();
    assertThat(trackOutput == output.trackOutputs.get(257 /* PID of audio track. */)).isTrue();
    assertThat(((FakeTrackOutput) trackOutput).format)
        .isEqualTo(Format.createTextSampleFormat("1/257", "mime", null, 0, 0, "und", null, 0));
  }

  @Test
  public void testCustomInitialSectionReader() throws Exception {
    CustomTsPayloadReaderFactory factory = new CustomTsPayloadReaderFactory(false, true);
    TsExtractor tsExtractor =
        new TsExtractor(TsExtractor.MODE_MULTI_PMT, new TimestampAdjuster(0), factory);
    FakeExtractorInput input =
        new FakeExtractorInput.Builder()
            .setData(
                TestUtil.getByteArray(
                    ApplicationProvider.getApplicationContext(), "ts/sample_with_sdt.ts"))
            .setSimulateIOErrors(false)
            .setSimulateUnknownLength(false)
            .setSimulatePartialReads(false)
            .build();
    tsExtractor.init(new FakeExtractorOutput());
    PositionHolder seekPositionHolder = new PositionHolder();
    int readResult = Extractor.RESULT_CONTINUE;
    while (readResult != Extractor.RESULT_END_OF_INPUT) {
      readResult = tsExtractor.read(input, seekPositionHolder);
      if (readResult == Extractor.RESULT_SEEK) {
        input.setPosition((int) seekPositionHolder.position);
      }
    }
    assertThat(factory.sdtReader.consumedSdts).isEqualTo(2);
  }

  private static void writeJunkData(ByteArrayOutputStream out, int length) {
    for (int i = 0; i < length; i++) {
      if (((byte) i) == TS_SYNC_BYTE) {
        out.write(0);
      } else {
        out.write(i);
      }
    }
  }

  private static final class CustomTsPayloadReaderFactory implements TsPayloadReader.Factory {

    private final boolean provideSdtReader;
    private final boolean provideCustomEsReader;
    private final TsPayloadReader.Factory defaultFactory;
    private CustomEsReader esReader;
    private SdtSectionReader sdtReader;

    public CustomTsPayloadReaderFactory(boolean provideCustomEsReader, boolean provideSdtReader) {
      this.provideCustomEsReader = provideCustomEsReader;
      this.provideSdtReader = provideSdtReader;
      defaultFactory = new DefaultTsPayloadReaderFactory();
    }

    @Override
    public SparseArray<TsPayloadReader> createInitialPayloadReaders() {
      if (provideSdtReader) {
        assertThat(sdtReader).isNull();
        SparseArray<TsPayloadReader> mapping = new SparseArray<>();
        sdtReader = new SdtSectionReader();
        mapping.put(17, new SectionReader(sdtReader));
        return mapping;
      } else {
        return defaultFactory.createInitialPayloadReaders();
      }
    }

    @Override
    public TsPayloadReader createPayloadReader(int streamType, EsInfo esInfo) {
      if (provideCustomEsReader && streamType == 3) {
        esReader = new CustomEsReader(esInfo.language);
        return new PesReader(esReader);
      } else {
        return defaultFactory.createPayloadReader(streamType, esInfo);
      }
    }
  }

  private static final class CustomEsReader implements ElementaryStreamReader {

    private final String language;
    private TrackOutput output;
    public int packetsRead = 0;

    public CustomEsReader(String language) {
      this.language = language;
    }

    @Override
    public void seek() {}

    @Override
    public void createTracks(ExtractorOutput extractorOutput, TrackIdGenerator idGenerator) {
      idGenerator.generateNewId();
      output = extractorOutput.track(this, idGenerator.getTrackId(), C.TRACK_TYPE_UNKNOWN);
      output.format(
          Format.createTextSampleFormat(
              idGenerator.getFormatId(), "mime", null, 0, 0, language, null, 0));
    }

    @Override
    public void packetStarted(long pesTimeUs, @TsPayloadReader.Flags int flags) {}

    @Override
    public void consume(ParsableByteArray data) {}

    @Override
    public void packetFinished() {
      packetsRead++;
    }

  @Override
  public void discardStream(boolean discard) {

  }

  @Override
  public boolean keepsTrack() {
      return false;
  }

      public TrackOutput getTrackOutput() {
      return output;
    }
  }

  private static final class SdtSectionReader implements SectionPayloadReader {

    private int consumedSdts;

    @Override
    public void init(
        TimestampAdjuster timestampAdjuster,
        ExtractorOutput extractorOutput,
        TrackIdGenerator idGenerator) {
      // Do nothing.
    }

    @Override
    public void consume(ParsableByteArray sectionData) {
      // table_id(8), section_syntax_indicator(1), reserved_future_use(1), reserved(2),
      // section_length(12), transport_stream_id(16), reserved(2), version_number(5),
      // current_next_indicator(1), section_number(8), last_section_number(8),
      // original_network_id(16), reserved_future_use(8)
      sectionData.skipBytes(11);
      // Start of the service loop.
      assertThat(sectionData.readUnsignedShort()).isEqualTo(0x5566 /* arbitrary service id */);
      // reserved_future_use(6), EIT_schedule_flag(1), EIT_present_following_flag(1)
      sectionData.skipBytes(1);
      // Assert there is only one service.
      // Remove running_status(3), free_CA_mode(1) from the descriptors_loop_length with the mask.
      assertThat(sectionData.readUnsignedShort() & 0xFFF).isEqualTo(sectionData.bytesLeft());
      while (sectionData.bytesLeft() > 0) {
        int descriptorTag = sectionData.readUnsignedByte();
        int descriptorLength = sectionData.readUnsignedByte();
        if (descriptorTag == 72 /* service descriptor */) {
          assertThat(sectionData.readUnsignedByte()).isEqualTo(1); // Service type: Digital TV.
          int serviceProviderNameLength = sectionData.readUnsignedByte();
          assertThat(sectionData.readString(serviceProviderNameLength)).isEqualTo("Some provider");
          int serviceNameLength = sectionData.readUnsignedByte();
          assertThat(sectionData.readString(serviceNameLength)).isEqualTo("Some Channel");
        } else {
          sectionData.skipBytes(descriptorLength);
        }
      }
      consumedSdts++;
    }
  }
}

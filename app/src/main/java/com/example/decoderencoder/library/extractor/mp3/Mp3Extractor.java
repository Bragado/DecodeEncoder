package com.example.decoderencoder.library.extractor.mp3;

import com.example.decoderencoder.library.extractor.Extractor;
import com.example.decoderencoder.library.extractor.ExtractorInput;
import com.example.decoderencoder.library.extractor.ExtractorOutput;
import com.example.decoderencoder.library.extractor.PositionHolder;

import java.io.IOException;

public class Mp3Extractor implements Extractor {
    public static final int FLAG_ENABLE_CONSTANT_BITRATE_SEEKING = 0;

    int flag = 0;

    public Mp3Extractor(int flags) {
        this.flag = flags;
    }


    @Override
    public boolean sniff(ExtractorInput input) throws IOException, InterruptedException {
        return false;
    }

    @Override
    public void init(ExtractorOutput output) {

    }

    @Override
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException, InterruptedException {
        return 0;
    }

    @Override
    public void seek(long position, long timeUs) {

    }

    @Override
    public void release() {

    }
}


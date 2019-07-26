package com.example.decoderencoder.library.extractor;

import android.media.MediaExtractor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AVDemuxer implements Extractor {

    MediaExtractor mediaExtractor;
    String source = "";


    public AVDemuxer() {                                                // PRIVATE

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setDataSource(String fileId) throws IOException {       // DEPRECATED
        this.mediaExtractor = new MediaExtractor();
        this.source = fileId;
        this.mediaExtractor.setDataSource(fileId);
    }

    public void setDataSource(Uri uri) {

    }

    public Object getInstance() {
        return mediaExtractor;
    }

    @Override
    public void setDataSource(Object path) {

    }

    @Override
    public boolean evaluate(Object path) {
        return false;
    }

    @Override
    public void seek(long position, long timeUs) {

    }

    @Override
    public void release() {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public int readSampleData(ByteBuffer inputBuffer, int i) {
        return mediaExtractor.readSampleData(inputBuffer, i);
    }

    @Override
    public void init() {

    }

    @Override
    public int read(ByteBuffer inputBuffer, int offset) {
        return 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public boolean advance() {
        return mediaExtractor.advance();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public long getSampleTime() {
        return mediaExtractor.getSampleTime();
    }
}

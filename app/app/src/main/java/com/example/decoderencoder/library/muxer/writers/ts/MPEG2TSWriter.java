package com.example.decoderencoder.library.muxer.writers.ts;

import com.example.decoderencoder.library.muxer.writers.MediaWriter;
import com.example.decoderencoder.library.source.MediaSource;

public class MPEG2TSWriter implements MediaWriter {
    enum State  {
        kWhatStart, kWhatRead
    };


    @Override
    public boolean addSource(MediaSource source) {
        return false;
    }

    @Override
    public boolean reachedEOS() {
        return false;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean pause() {
        return false;
    }

    @Override
    public void setMaxFileSize(long bytes) {

    }

    @Override
    public void release() {

    }

    @Override
    public void setStartTimeOffsetMs(long ms) {

    }

    @Override
    public long getStartTimeOffsetMs() {
        return 0;
    }


    public class SourceInfo {

    }

}

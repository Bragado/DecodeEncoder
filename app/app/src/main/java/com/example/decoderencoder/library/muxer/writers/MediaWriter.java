package com.example.decoderencoder.library.muxer.writers;

import com.example.decoderencoder.library.source.MediaSource;

public interface MediaWriter {
/*
    private final long mMaxFileSizeLimitBytes;
    private final long mMaxFileDurationLimitUs;

    public MediaWriter() {
        this.mMaxFileSizeLimitBytes = 0;
        this.mMaxFileDurationLimitUs = 0;
    }
*/
    public boolean addSource(MediaSource source);

    public boolean reachedEOS();

    public boolean start();

    public boolean stop();

    public boolean pause();

    public void setMaxFileSize(long bytes);

    public void release();

    public void setStartTimeOffsetMs(long ms);

    public long getStartTimeOffsetMs();





}

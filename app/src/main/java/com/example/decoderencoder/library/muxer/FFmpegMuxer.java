package com.example.decoderencoder.library.muxer;

import android.media.MediaFormat;
import android.net.Uri;

import java.nio.ByteBuffer;

public class FFmpegMuxer implements MediaMuxer {

    Uri uri;

    public FFmpegMuxer(Uri output) {
        this.uri = uri;
    }

    @Override
    public void stop() {
        nativeStop();
    }

    @Override
    public int addTrack(MediaFormat newFormat) {
        return nativeAddTrack(new String[0], new String[0]);
    }

    @Override
    public void start() {
        nativeStart(uri.getPath());
    }

    @Override
    public void release() {
        nativeStop();
    }

    @Override
    public void sniff(int container) {

    }

    @Override
    public void writeSampleData(int trackIndex, ByteBuffer byteBuf, int offset, int size, int flags, long presentationTimeUs) {
        nativeWriteSampleData(trackIndex, byteBuf, offset, size, flags, presentationTimeUs);
    }


    private native int nativeAddTrack(String[] keys, String[] values);

    private native void nativeStart(String path);

    private native void nativeStop();

    private native void nativeWriteSampleData(int trackIndex, ByteBuffer byteBuf, int offset, int size, int flags, long presentationTimeUs);

    static{
        System.loadLibrary("avutil");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("swscale");
        System.loadLibrary("media-file");
    }

}

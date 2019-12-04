package com.example.decoderencoder.library.muxer;

import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.RequiresApi;

public class FFmpegMuxer implements MediaMuxer {

    Uri uri;

    public FFmpegMuxer(Uri output) {
        this.uri = output;
        nativeInit(uri.getPath());
    }

    @Override
    public void stop() {
        nativeStop();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int addTrack(MediaFormat newFormat) {
        // Convert the MediaFormat into key-value pairs and send to the native.
        String[] keys = new String[5];
        String[] values = new String[5];
        keys[0] = "mimeType";
        values[0] = newFormat.getString(MediaFormat.KEY_MIME);
        keys[1] = "bit_rate";
        values[1] = newFormat.getInteger(MediaFormat.KEY_BIT_RATE) + "";
        keys[2] = "width";
        values[2] = newFormat.getInteger(MediaFormat.KEY_WIDTH) + "";
        keys[3] = "height";
        values[3] = newFormat.getInteger(MediaFormat.KEY_HEIGHT) + "";
        keys[4] = "fps";
        values[4] = newFormat.getInteger(MediaFormat.KEY_FRAME_RATE) + "";

        return nativeAddTrack(keys, values);
    }

    @Override
    public void start() {
        nativeStart();
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
        if(size > 0)
        nativeWriteSampleData(trackIndex, byteBuf, offset, size, flags, presentationTimeUs);
    }

    private native int nativeInit(String path);

    private native int nativeAddTrack(String[] keys, String[] values);

    private native void nativeStart();

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

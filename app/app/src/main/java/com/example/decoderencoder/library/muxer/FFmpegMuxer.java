package com.example.decoderencoder.library.muxer;

import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;

public class FFmpegMuxer implements MediaMuxer {

    Uri uri;

    @Keep
    private Long nativePointer = new Long(0);

    public FFmpegMuxer(Uri output) {
        this.uri = output;
        this.nativePointer = nativeInit(uri.getPath());
    }

    @Override
    public void stop() {
        nativeStop(nativePointer);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int addTrack(MediaFormat newFormat) {
        // Convert the MediaFormat into key-value pairs and send to the native.
        // Video keys: streamType ; codec_tag ; codec_id ; bit_rate ; width ; height ; fps
        String[] keys = null;
        String[] values = null;

        if(newFormat.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
            keys = new String[6];
            values = new String[6];
            keys[0] = "streamType";
            values[0] = "0";
            keys[1] = "bit_rate";
            keys[1] = "bit_rate";
            values[1] = newFormat.getInteger(MediaFormat.KEY_BIT_RATE) + "";
            keys[2] = "width";
            values[2] = newFormat.getInteger(MediaFormat.KEY_WIDTH) + "";
            keys[3] = "height";
            values[3] = newFormat.getInteger(MediaFormat.KEY_HEIGHT) + "";
            keys[4] = "fps";
            values[4] = 50/* newFormat.getInteger(MediaFormat.KEY_FRAME_RATE) */ + ""; // FIXME
            keys[5] = "mimeType";
            values[5] = newFormat.getString(MediaFormat.KEY_MIME);
        }else if(newFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
            keys[0] = "streamType";
            values[0] = "1";

        }else if(newFormat.getString(MediaFormat.KEY_MIME).startsWith("text/")) {
            keys[0] = "streamType";
            values[0] = "2";
        }else {
            keys[0] = "streamType";
            values[0] = "-1";
        }



        return nativeAddTrack(nativePointer, keys, values);
    }

    @Override
    public void start() {
        nativeStart(nativePointer);
    }

    @Override
    public void release() {
        nativeStop(nativePointer);
    }

    @Override
    public void sniff(int container) {

    }



    @Override
    public void writeSampleData(int trackIndex, ByteBuffer byteBuf, int offset, int size, int flags, long presentationTimeUs) {
        if(size > 0)
        nativeWriteSampleData(nativePointer, trackIndex, byteBuf, offset, size, flags, presentationTimeUs);
    }

    private native long nativeInit(String path);

    private native int nativeAddTrack(long nativePointer, String[] keys, String[] values);

    private native void nativeStart(long nativePointer);

    private native void nativeStop(long nativePointer);

    private native void nativeWriteSampleData(long nativePointer, int trackIndex, ByteBuffer byteBuf, int offset, int size, int flags, long presentationTimeUs);

    static{
        System.loadLibrary("avutil");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("swscale");
        System.loadLibrary("media-file");
    }

}

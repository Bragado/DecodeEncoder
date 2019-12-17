package com.example.decoderencoder.library.muxer;

import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.encoder.EncoderBuffer;
import com.example.decoderencoder.library.source.Media;

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
            keys = new String[7];
            values = new String[7];
            keys[6] = "streamType";
            values[6] = "0";
            keys[0] = "codecID";
            values[0] = "0";
            keys[1] = "bit_rate";
            values[1] = newFormat.getInteger(MediaFormat.KEY_BIT_RATE) + "";
            keys[2] = "width";
            values[2] = newFormat.getInteger(MediaFormat.KEY_WIDTH) + "";
            keys[3] = "height";
            values[3] = newFormat.getInteger(MediaFormat.KEY_HEIGHT) + "";
            keys[4] = "fps";
            values[4] =  newFormat.getInteger(MediaFormat.KEY_FRAME_RATE) + ""; // FIXME: sometimes this is null, what to do in those cases ?? passthrough the configuration??
            keys[5] = "mimeType";
            values[5] = newFormat.getString(MediaFormat.KEY_MIME);

        }else if(newFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
            keys = new String[5];
            values = new String[5];
            keys[3] = "streamType";
            values[3] = "1";
            keys[0] = "codecID";
            values[0] = "1";
            keys[1] = "sampleRate";
            values[1] = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) + "";
            keys[2] = "channels";
            values[2] = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) + "";
            keys[4] = "bitrate";
            values[4] = newFormat.getInteger(MediaFormat.KEY_BIT_RATE) + "";
        }else if(newFormat.getString(MediaFormat.KEY_MIME).startsWith("text/")) {
            keys = new String[3];
            values = new String[3];
            keys[2] = "streamType";
            values[2] = "2";
            keys[0] = "codecID";
            values[0] = "2";
            keys[1] = "language";
            values[1] = newFormat.getString(MediaFormat.KEY_LANGUAGE);
        }else {     // what to do ??
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
    public void writeSampleData(int trackIndex, EncoderBuffer bf) {
        if(bf.size > 0)
        nativeWriteSampleData(nativePointer, trackIndex, bf.data, bf.offset, bf.size, bf.flags, bf.presentationTimeUs);
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

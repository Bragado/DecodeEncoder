package com.example.encoderdecoder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;

public class Utils {
    public static String TAG = "Utils";

    public static void configCodecWithMimeType(String type, MediaExtractor extractor, MediaCodec codec, Surface surface) throws IOException {
        String mimeType = "";
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            mimeType = format.getString(MediaFormat.KEY_MIME);
            Log.e(TAG, "MimeType found : " + mimeType);
            if(mimeType.startsWith(type)) {
                extractor.selectTrack(i);
                codec = MediaCodec.createDecoderByType(mimeType);
                codec.configure(format, surface, null, 0);


            }
        }
    }


}

package com.example.decoderencoder.library.support;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

public class Codec {

    public static final String TAG = "CODEC";

    String MIME_TYPE;
    String decoderInputPath;
    String encoderOutputPath;
    boolean isSupposedToRender = true;
    boolean mediaMuxerStarted = false;

    public Codec() {}

    public void setDecoderInputPath(String decoderInputPath) {
        this.decoderInputPath = decoderInputPath;
    }

    public void setEncoderOutputPath(String encoderOutputPath) {
        this.encoderOutputPath = encoderOutputPath;
    }

    public String getDecoderInputPath() {
        return this.decoderInputPath;
    }

    public String getEncoderOutputPath() {
        return this.encoderOutputPath;
    }

    public void setDecoderRenderOption(boolean isToRender) {
        this.isSupposedToRender = isToRender;
    }

    public boolean isSupposedToRender() {
        return isSupposedToRender;
    }


    /**
     *  Creates a MediaFormat necessary to configure the encoder
     * @return MediaFormat instance
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static MediaFormat getEncoderVideoFormat(String MIME_TYPE, int mWidth, int mHeight, int mBitRate, int FRAME_RATE, int IFRAME_INTERVAL) {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        return format;
    }

    public static MediaFormat getEncoderAudioFormat(String MIME_TYPE) {
        return null;
    }

    /**
     *
     * @param type whether is audio or video track
     * @param extractor Object initialized with the data to be decoded
     * @param surface   Decoders' output surface
     * @return A MediaCodec compatible with the track from extractor specified in type
     * @throws IOException thrown by createDecoderByType(...)
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static MediaCodec configCodecWithMimeType(String type, MediaExtractor extractor, Surface surface) throws IOException {
        String mimeType = "";
        MediaCodec codec = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            Log.e(TAG, "Bit Rate: " + format.toString());

            mimeType = format.getString(MediaFormat.KEY_MIME);
            Log.e(TAG, "MimeType found : " + mimeType);
            if(mimeType.startsWith(type)) {
                extractor.selectTrack(i);
                codec = MediaCodec.createDecoderByType(mimeType);
                codec.configure(format, surface, null, 0);


            }
        }
        return codec;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static MediaFormat getTrackFormat(String type, MediaExtractor extractor, Surface surface) throws IOException {
        String mimeType = "";
        MediaCodec codec = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            Log.e(TAG, "Bit Rate: " + format.toString());

            mimeType = format.getString(MediaFormat.KEY_MIME);
            Log.e(TAG, "MimeType found : " + mimeType);
            if(mimeType.startsWith(type)) {
                return format;
            }
        }
        return null;
    }

    public boolean isMediaMuxerStarted() {
        return this.mediaMuxerStarted;
    }

    public void setMediaMuxerStarted(boolean started) {
        this.mediaMuxerStarted = started;
    }





}

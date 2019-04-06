package com.example.encoderdecoder.Library;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;


// TODO: extend VCodec into 2 different classes to distinguish between encoder or decoder constructor

/**
 *    This class encapsulates all needed parameters and support in order to encode, decode and decode-encode methods present in DecoderEncoder.java,
 *    that is, all the parameters necessary to implement those methods with MediaCodec API.
 *
 */
public class VCodec {

    public static final String TAG = "VCODEC";
    private long TIMEOUT = 10000;


    // parameters for the decoder
    private Surface decoderSurface = null;                   // decoder output surface
    private String decoderInputPath;                         // needed for extractor
    private boolean isSupposedToRender = true;               // needed for release buffers
    private boolean isSupposedToSave;                        // needed for debugging purposes - save a copy of decoded data
    private String decoderOutputPath;                        // output path for decoded data


    // parameters for the encoder
    private String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private int FRAME_RATE = 25;               // 24fps
    private int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private int mWidth = 600;
    private int mHeight = 280;
    private int mBitRate = 4221440;             // 2000000
    private Surface encoderSurface;
    private String encoderOutputPath = "/storage/emulated/0/Download/testX.mp4";
    private MediaMuxer mediaMuxer = null;
    private boolean mediaMuxerStarted = false;


    // < Decoder specific methods >

    /**
     *  Creates a VCodec instance appropriated to decoders
     * @param surface decoder output surface
     * @param path data path to be decoded
     */
    public VCodec(Surface surface, String path) {
        this.decoderSurface = surface;
        this.decoderInputPath= path;
    }

    public VCodec(String path) {
        this.encoderOutputPath = path;
    }


    public void setDecoderOutputSurface(Surface surface) {
        this.decoderSurface = surface;
    }



    public long getTimeOUT() {
        return TIMEOUT;
    }

    public void setTIMEOUT(long timeout)  {
        this.TIMEOUT = timeout;
    }

    public Surface getDecoderSurface() {
        return decoderSurface;
    }

    public String getURLForDecoder() {
        return decoderInputPath;
    }

    // </ Decoder specific methods >


    // < Encoder specific methods >

    /**
     *  Creates a MediaFormat necessary to configure the encoder
     * @return MediaFormat instance
     */
    public MediaFormat getEncoderFormat() {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        return format;
    }

    public MediaMuxer getMediaMuxer() throws Exception {
        if(this.mediaMuxer == null) {
            this.mediaMuxer = new MediaMuxer(this.encoderOutputPath,  MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            this.mediaMuxerStarted = false;
        }
        return this.mediaMuxer;
    }


    public String getEncoderVideoFormat() {
        return MIME_TYPE;
    }

    public void setEncoderInputSurface(Surface surface) {
        this.encoderSurface = surface;
    }

    public Surface getEncoderInputSurface() {
        return this.encoderSurface;
    }

    // </ Encoder specific methods >


    // < Static functions to support DecoderEncoder >
    /**
     *
     * @param type whether is audio or video track
     * @param extractor Object initialized with the data to be decoded
     * @param surface   Decoders' output surface
     * @return A MediaCodec compatible with the track from extractor specified in type
     * @throws IOException thrown by createDecoderByType(...)
     */
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

    // < /Static functions to support DecoderEncoder >





    public boolean isMediaMuxerStarted() {
        return this.mediaMuxerStarted;
    }

    public void setMediaMuxerStarted(boolean started) {
        this.mediaMuxerStarted = started;
    }


    public boolean isSupposedToRender() {
        return isSupposedToRender;
    }


    public void stopMediaMuxer() {
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaMuxer = null;
        mediaMuxerStarted = false;
    }
}

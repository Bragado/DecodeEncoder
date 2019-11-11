package com.example.decoderencoder.library.support;

import android.annotation.TargetApi;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.view.Surface;

import com.example.decoderencoder.Library.AVMuxer;


// TODO: extend VCodec into 2 different classes to distinguish between encoder or decoder constructor

/**
 *    This class encapsulates all needed parameters and support in order to encode, decode and decode-encode methods present in DecoderEncoder.java,
 *    that is, all the parameters necessary to implement those methods with MediaCodec API.
 *
 */
public class VCodec extends Codec{

    public static final String TAG = "VCODEC";
    private long TIMEOUT =  0;                              // the main reason that can cause delay,


    // parameters for the decoder
    private Surface decoderSurface = null;                   // decoder output surface
    private String decoderInputPath;                         // needed for extractor
    private boolean isSupposedToRender = true;               // needed for release buffers
    private boolean isSupposedToSave;                        // needed for debugging purposes - save a copy of decoded data
    private String decoderOutputPath;                        // output path for decoded data


    // parameters for the encoder
    private String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private int FRAME_RATE = 24;               // 24fps
    private int IFRAME_INTERVAL = 8;          // 10 seconds between I-frames
    private int mWidth = 1280;
    private int mHeight = 544;
    private int mBitRate = 221440;             // 12581768
    private Surface encoderSurface;
    private String encoderOutputPath = "/storage/emulated/0/Download/testX.mp4";
    private AVMuxer mediaMuxer = null;

    // < Decoder specific methods >
    /**
     *  Creates a VCodec instance appropriated to decoders
     * @param surface decoder output surface
     * @param path data path to be decoded
     */
    public VCodec(Surface surface, String path) {
        super();
        this.decoderSurface = surface;
        this.decoderInputPath= path;
    }

    public VCodec(String path) {
        super();
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

    /**
     *  Creates a MediaFormat necessary to configure the encoder
     * @return MediaFormat instance
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public MediaFormat getEncoderFormat() {
        return super.getEncoderVideoFormat(MIME_TYPE, mWidth, mHeight, mBitRate, FRAME_RATE, IFRAME_INTERVAL);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public AVMuxer getMediaMuxer() throws Exception {
        if(this.mediaMuxer == null) {
            this.mediaMuxer = new AVMuxer(this.encoderOutputPath,  MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
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

    public boolean isSupposedToRender() {
        return isSupposedToRender;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stopMediaMuxer() {
        try {
            mediaMuxer.stop();
        }catch(Exception e) {

        }
        mediaMuxer = null;
        mediaMuxerStarted = false;
    }
}

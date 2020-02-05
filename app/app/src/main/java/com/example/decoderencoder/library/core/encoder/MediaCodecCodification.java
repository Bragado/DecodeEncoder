package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import com.example.decoderencoder.library.core.decoder.Decoder;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.output.MediaOutput;
import com.example.decoderencoder.library.util.Log;
import com.example.decoderencoder.library.util.Util;

import java.nio.ByteBuffer;

import androidx.annotation.RequiresApi;

/**
 * Handles audio and video generic operations for enconding
 */
public abstract class MediaCodecCodification extends BaseCodification {

    public static String TAG = "MediaCodecCodification";

    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    protected Decoder decoder;
    protected EncoderBuffer extraData = null;
    protected boolean endOfCodification = false;


    Surface encoderSurface;

    long pts = 0;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public MediaCodecCodification(Renderer renderer, Encoder encoder, MediaFormat format, MediaOutput mediaOutput) {
        super(renderer, encoder, format, mediaOutput);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void start() {
        if(this.initCodec()) {
            encoder.start();
            getCodecBuffers(encoder);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public int drainOutputBuffer() {
        int ret = 2;
        if(this.endOfCodification)
            return ret;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        int encoderStatus = this.encoder.dequeueOutputBuffer(bufferInfo, 0);
        switch(encoderStatus) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                ret = 0;
                break;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                mayUpdateOutputBuffer();
                break;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                MediaFormat newFormat = this.encoder.getOutputFormat();
                onFormatChange(this.format);
                break;
            default:
                if(encoderStatus < 0)
                    return ret;
                ret = 1;
                Log.i(TAG, "DATA WAS READ FROM ENCODER for stream: " + format.getString(MediaFormat.KEY_MIME));
                ByteBuffer outputBuffer = getOutputBuffer(encoderStatus);
                if(outputBuffer == null) {
                    Log.e(TAG, "Encoded data is null, something went wrong");
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                      // if video:      h264 / h265 requires sps and pps values, those values can be obtained here
                    //onCodecConfigAvailable(outputBuffer, bufferInfo);
                    byte[] content = new byte[bufferInfo.size];
                    outputBuffer.get(content);
                    encoder.releaseOutputBuffer(encoderStatus, false);
                    super.addConfigBuffer(content, content.length);

                    return ret;
                }

                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

               /* ByteBuffer frameData = maybeProccessOutputData(outputBuffer, bufferInfo);

                if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
                    onKeyFrameReady(extraData, frameData, bufferInfo);
                }
              else {*/
                    onDataReady(outputBuffer, bufferInfo);
                //}

                encoder.releaseOutputBuffer(encoderStatus, false);
        }
        if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            this.endOfCodification = true;
            return 0;
        }

        return ret;
    }

    protected ByteBuffer maybeProccessOutputData(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        return outputBuffer;
    }


    protected void mayUpdateOutputBuffer() {
        if (Util.SDK_INT <= 21) {
            this.outputBuffers = encoder.getOutputBuffers();
        }
    }

    @Override
    public void onRelease() {}

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onFormatChange(MediaFormat mediaFormat) {
        super.addTrack(mediaFormat);
    }

    public void onStop() {
        inputBuffers = null;
        outputBuffers = null;
        extraData = null;
    }

    protected ByteBuffer getInputBuffer(int inputIndex) {
        if(inputIndex < 0)
            return null;
        if (Util.SDK_INT >= 21) {
            return this.encoder.getInputBuffer(inputIndex);
        } else {
            return inputBuffers[inputIndex];        // TODO: not instanciated
        }
    }

    private ByteBuffer getOutputBuffer(int outputIndex) {
        if (Util.SDK_INT >= 21) {
            return encoder.getOutputBuffer(outputIndex);
        } else {
            return outputBuffers[outputIndex];
        }
    }

    protected void getCodecBuffers(Encoder codec) {
        if (Util.SDK_INT < 21) {
            inputBuffers = codec.getInputBuffers();
            outputBuffers = codec.getOutputBuffers();
        }
    }

    protected boolean initCodec() {
        init();
        return encoder.makeCodecReady(this.format);
    }

    protected abstract void init();


    protected void onKeyFrameReady(EncoderBuffer extraData, ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        // no-op, only necessary for video muxers
    }

    protected void onCodecConfigAvailable(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        // no-op, only necessary for video muxers
    }

}

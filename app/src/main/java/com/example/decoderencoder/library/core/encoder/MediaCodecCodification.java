package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.decoder.Decoder;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.util.Util;

import java.nio.ByteBuffer;

/**
 * Handles audio and video generic operations for enconding
 */
public abstract class MediaCodecCodification extends BaseCodification {

    public static String TAG = "MediaCodecCodification";

    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    protected Decoder decoder;


    Surface encoderSurface;

    public MediaCodecCodification(Renderer renderer, Encoder encoder, MediaFormat format) {
        super(renderer, encoder, format);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void start() {
        if(this.initCodec()) {
            encoder.start();
            getCodecBuffers(encoder);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean drainOutputBuffer() {

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        int encoderStatus = this.encoder.dequeueOutputBuffer(bufferInfo, 0);
        switch(encoderStatus) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                break;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                mayUpdateOutputBuffer();
                break;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                MediaFormat newFormat = this.encoder.getOutputFormat();
                onFormatChange(newFormat);
                break;
            default:
                Log.i(TAG, "DATA WAS READ FROM ENCODER for stream: " + format.getString(MediaFormat.KEY_MIME));
                if(encoderStatus < 0)
                    return true;
                ByteBuffer outputBuffer = getOutputBuffer(encoderStatus);
                if(outputBuffer == null) {
                    Log.e(TAG, "Encoded data is null, something went wrong");
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) { bufferInfo.size = 0; } // ignore data
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                onDataReady(outputBuffer, bufferInfo);
                encoder.releaseOutputBuffer(encoderStatus, false);
        }

        return true;
    }

    private void onDataReady(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
    }

    public abstract boolean feedInputBuffer();

    protected void mayUpdateOutputBuffer() {
        if (Util.SDK_INT <= 21) {
            this.outputBuffers = encoder.getOutputBuffers();
        }
    }

    @Override
    public void onRelease() {}

    public void onFormatChange(MediaFormat mediaFormat) {
        // TODO: tell the muxer the format changed
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


}

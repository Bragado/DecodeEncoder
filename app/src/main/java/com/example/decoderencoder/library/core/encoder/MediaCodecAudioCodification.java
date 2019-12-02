package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.muxer.MuxerInput;

import java.nio.ByteBuffer;

public class MediaCodecAudioCodification extends MediaCodecCodification {

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean feedInputBuffer() {          // only for audio, for video streams we use encoders' surface to feed it data
        if(decoder == null) {                   // TODO: ensure decoder is initialized and get this out of here
            decoder = renderer.getDecoder();
        }

        int decoderIndex = renderer.poolBufferIndex();
        if(decoderIndex < 0)
            return true;
        MediaCodec.BufferInfo decoderBufferInfo = renderer.pollBufferInfo();

        int index = this.encoder.dequeueInputBuffer(0);
        ByteBuffer encoderInputBuffer = getInputBuffer(index);
        if(encoderInputBuffer == null)
            return true;

        int size = decoderBufferInfo.size;
        int encodeSize = encoderInputBuffer.capacity();
        long presentationTime = decoderBufferInfo.presentationTimeUs;

        if(size >= 0) {
            ByteBuffer decoderOutputBuffer = decoder.getOutputBuffer(decoderIndex).duplicate();
            decoderOutputBuffer.position(decoderBufferInfo.offset);
            decoderOutputBuffer.limit(decoderBufferInfo.offset + size);
            encoderInputBuffer.position(0);
            encoderInputBuffer.put(decoderOutputBuffer); // BufferOverflowException

            this.encoder.queueInputBuffer(
                    index,
                    0,
                    size,
                    presentationTime,
                    decoderBufferInfo.flags);
        }
        renderer.getDecoder().releaseOutputBuffer(decoderIndex, false);
        return true;
    }

    public MediaCodecAudioCodification(Renderer renderer, Encoder encoder, MediaFormat format, MuxerInput muxerInput) {
        super(renderer, encoder, format, muxerInput);

    }

    @Override
    protected void init() {
        // do nothing
    }
}

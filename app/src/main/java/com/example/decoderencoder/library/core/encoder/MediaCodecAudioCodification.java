package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.audio.AacUtil;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.output.MediaOutput;

import java.nio.ByteBuffer;

public class MediaCodecAudioCodification extends MediaCodecCodification {

    int index = -1;
    ByteBuffer encoderInputBuffer;

    // For aac encoders
    byte[] audioPacket = null;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public MediaCodecAudioCodification(Renderer renderer, Encoder encoder, MediaFormat format, MediaOutput mediaOutput) {
        super(renderer, encoder, format, mediaOutput);

    }

    @Override
    protected void init() {
        // do nothing
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean feedInputBuffer() {          // only for audio, for video streams we use encoders' surface to feed it data
        if(super.endOfCodification)
            return false;

        if(decoder == null) {                   // TODO: ensure decoder is initialized and get this out of here
            decoder = renderer.getDecoder();
        }
        if(index < 0) {
            index = this.encoder.dequeueInputBuffer(0);
            encoderInputBuffer = getInputBuffer(index);
        }

        if(encoderInputBuffer == null)
            return true;
        int decoderIndex = renderer.poolBufferIndex();
        if(decoderIndex < 0)
            return true;
        MediaCodec.BufferInfo decoderBufferInfo = renderer.pollBufferInfo();

        int size = decoderBufferInfo.size;
        long presentationTime = decoderBufferInfo.presentationTimeUs;

        if(size >= 0) {
            ByteBuffer decoderOutputBuffer = decoder.getOutputBuffer(decoderIndex).duplicate();
            decoderOutputBuffer.position(decoderBufferInfo.offset);
            decoderOutputBuffer.limit(decoderBufferInfo.offset + size);
            encoderInputBuffer.position(0);
            encoderInputBuffer.put(decoderOutputBuffer); // FIXME: BufferOverflowException if we do not set max buffer size in MediaFormat

            this.encoder.queueInputBuffer(
                    index,
                    0,
                    size,
                    presentationTime,
                    decoderBufferInfo.flags);
            index = -1;
        }
        renderer.getDecoder().releaseOutputBuffer(decoderIndex, false);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected  ByteBuffer maybeProccessOutputData(ByteBuffer outputBuffer,  MediaCodec.BufferInfo bufferInfo) {
        // TODO: check if it's aac, let's assume for now it is because that's our default encoder

        int outSize = bufferInfo.size;
        int outPacketSize = outSize + AacUtil.ADTS_LENGTH;
        ByteBuffer ret = ByteBuffer.allocateDirect(outPacketSize);
        audioPacket = new byte[outPacketSize];
        AacUtil.addADTStoPacket(audioPacket, outPacketSize, format);
        outputBuffer.get(audioPacket, AacUtil.ADTS_LENGTH, outSize);
        bufferInfo.size = outPacketSize;
        ret.put(audioPacket, 0, outPacketSize);
        return ret;
    }


}

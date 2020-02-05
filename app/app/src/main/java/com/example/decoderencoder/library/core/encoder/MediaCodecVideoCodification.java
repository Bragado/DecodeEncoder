package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.MainActivity;
import com.example.decoderencoder.library.video.openGL.InputSurface;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.output.MediaOutput;

import java.nio.ByteBuffer;

public class MediaCodecVideoCodification extends MediaCodecCodification {

    InputSurface inputSurface;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public MediaCodecVideoCodification(Renderer renderer, Encoder encoder, MediaFormat mediaFormat, MediaOutput mediaOutput) {
        super(renderer, encoder, mediaFormat, mediaOutput);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    protected void init() {
        this.encoder.makeCodecReady(this.format);
        this.encoderSurface = this.encoder.createInputSurface();

        if(MainActivity.FORCE_GPU_RENDER || openGLSurfaceIsNecessary()) {        // openGL adds an overload, it is necessary only when we need to change frame resolution
            // inicialize inputSurface
            this.inputSurface = new InputSurface(this.encoderSurface);
            this.inputSurface.makeCurrent();
            renderer.SurfaceCreated(this.inputSurface);
        }else {
            renderer.SurfaceCreated(encoderSurface);
        }
    }

    /**
     * Check if it's necessary the of OpenGL to render the frames into the surface
     * @return true if the transcoded frame resolution is different that the origial (thus requiring opengl)
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean openGLSurfaceIsNecessary() {
        MediaFormat decoderFormat = renderer.getFormat();
        boolean ret = !(decoderFormat.getInteger(MediaFormat.KEY_WIDTH) == this.format.getInteger(MediaFormat.KEY_WIDTH) &&  decoderFormat.getInteger(MediaFormat.KEY_HEIGHT) == this.format.getInteger(MediaFormat.KEY_HEIGHT));
        return (ret || MainActivity.FORCE_GPU_RENDER);
    }

    /**
     * Before each keyframe we must write the codec extra data (this hack took me a while to figure out)
     * @param encoderBuffer contains the extra data for the muxer, built in {@link #onCodecConfigAvailable}
     */
    @Override
    public void onKeyFrameReady(EncoderBuffer encoderBuffer, ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        ByteBuffer pps_sps_data = ByteBuffer.allocateDirect(encoderBuffer.size + bufferInfo.size);
        encoderBuffer.data.position(0);
        pps_sps_data.put(encoderBuffer.data);
        pps_sps_data.put(outputBuffer);

        bufferInfo.size += encoderBuffer.size;
        bufferInfo.offset = 0;
        super.onDataReady(pps_sps_data, bufferInfo);
    }


    /**
     * Collects the extra data for the codec, i.e. PPS and SPS
     * @param outputBuffer  encoder output buffer with pps and sps starting with (0x00000001)
     * @param bufferInfo describes the output buffer (size, offset, etc)
     */
    @Override
    protected void onCodecConfigAvailable(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        ByteBuffer videoSPSandPPS;
        videoSPSandPPS = ByteBuffer.allocateDirect(bufferInfo.size);
        byte[] videoConfig = new byte[bufferInfo.size];
        outputBuffer.get(videoConfig, 0, bufferInfo.size);
        outputBuffer.position(bufferInfo.offset);
        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
        videoSPSandPPS.put(videoConfig, 0, bufferInfo.size);
        super.extraData = new EncoderBuffer(videoSPSandPPS, bufferInfo.offset, bufferInfo.size, bufferInfo.flags, bufferInfo.presentationTimeUs);
    }


}

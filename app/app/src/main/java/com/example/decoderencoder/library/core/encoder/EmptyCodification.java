package com.example.decoderencoder.library.core.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.core.decoder.MediaCodecAudioRenderer;
import com.example.decoderencoder.library.core.decoder.MediaCodecVideoRenderer;
import com.example.decoderencoder.library.core.decoder.Renderer;
import com.example.decoderencoder.library.muxer.MediaMuxer;
import com.example.decoderencoder.library.muxer.MuxerInput;
import com.example.decoderencoder.library.output.DefaultMediaOutput;
import com.example.decoderencoder.library.output.MediaOutput;

import java.nio.ByteBuffer;

public class EmptyCodification extends BaseCodification {

    boolean format_registered = false;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public EmptyCodification(Renderer renderer, MediaOutput mediaOutput) {
        super(renderer, null, null, mediaOutput);          // TODO: set the format
    }

    @Override
    public void onRelease() {
        format_registered = false;
    }

    @Override
    public void onStop() {

    }

    @Override
    public void start() {

    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int drainOutputBuffer() {
        if(!format_registered) {
            // let's add the stream
            MediaFormat mediaFormat = renderer.getFormat();
            if(mediaFormat != null) {
                addTrack(mediaFormat);
                format_registered = true;
            }else {
                return 0;
            }
        }
        ByteBuffer data = renderer.pollFrameData();
        MediaCodec.BufferInfo bf = renderer.pollBufferInfo();
        if(data == null || bf == null)
            return 0;
      //  byte[] data_array = new byte[bf.size];
     //           data.get(data_array);
        data.position(0);
        // jni_data_compatible = ByteBuffer.allocateDirect(bf.size);
        //jni_data_compatible.put(data);
       // jni_data_compatible.position(0);

        onDataReady(data, bf);
        return 1;

    }


}

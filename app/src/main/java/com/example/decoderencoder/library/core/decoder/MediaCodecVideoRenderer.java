package com.example.decoderencoder.library.core.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.Util;

public class MediaCodecVideoRenderer extends MediaCodecRenderer {

    MediaCodecRenderer.Callback callback;

    /* In order to reduce frame resolution */
    int intended_fps = -1;
    int observed_fps = -1;
    int racio = -1;
    long last_observed_pts = -1;
    long decodedFrames = 0;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public MediaCodecVideoRenderer(MediaCodecRenderer.Callback callback, int trackType, Decoder decoder, int intended_fps) {
        super(trackType);
        this.decoder = decoder;
        this.callback = callback;
        this.intended_fps = intended_fps;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public MediaCodecVideoRenderer(MediaCodecRenderer.Callback callback, int trackType, Decoder decoder) {
        this(callback, trackType, decoder, -1);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected boolean initCodec() {
        Format f = streamFormats[0];
        return decoder.makeCodecReady(getMediaFormat(f, f.sampleMimeType, false), surface == null ? outputSurface.getSurface() : surface) ;
    }

    @Override
    protected void onQueueInputBuffer(DecoderInputBuffer buffer) {

    }

    @Override
    protected void onInputFormatChanged(Format format) {

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onReleaseOutputBuffer(MediaCodec.BufferInfo info, int outputBufferIndex) {
        if(intended_fps > 0) {
            if(last_observed_pts < 0) {
                last_observed_pts = info.presentationTimeUs;
            }else {
                observed_fps = (int) (1000000 / (info.presentationTimeUs - last_observed_pts));
                float aux = (float)intended_fps/observed_fps;
                aux = 1 - aux;
                if( aux < 0.1) {
                    racio = 1;
                }else {
                    aux = 1/aux;
                    racio = (int)aux;
                }
                intended_fps = -1;
            }
        }

        if(racio <= 1 || decodedFrames++ % racio == 0) {
            decoder.releaseOutputBuffer(outputBufferIndex, true);
        }else {
            decoder.releaseOutputBuffer(outputBufferIndex, false);
            return;
        }

        if(inputSurface != null && outputSurface != null) {
            /* If we are using OpenGL surfaces, there's some extra work */
            try {
                inputSurface.makeCurrent();
                outputSurface.awaitNewImage();
                outputSurface.drawImage();
                inputSurface.setPresentationTime(info.presentationTimeUs*1000);
                inputSurface.swapBuffers();
            }catch (Exception e) {
                e.printStackTrace();
            }

        }

    }


    @Override
    public SampleStream getStream() {
        return null;
    }

    @Override
    public boolean hasReadStreamToEnd() {
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int drainOutputBuffer() {
        return super.drainOutputBuffer(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Decoder getDecoder() {
        return this.decoder;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public MediaFormat getFormat() {
        return getMediaFormat(streamFormats[0], streamFormats[0].sampleMimeType, false);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static MediaFormat getMediaFormat(
            Format format,
            String codecMimeType,
            boolean deviceNeedsNoPostProcessWorkaround) {
        MediaFormat mediaFormat = new MediaFormat();
        // Set format parameters that should always be set.
        mediaFormat.setString(MediaFormat.KEY_MIME, codecMimeType);
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, format.width);
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, format.height);
        MediaFormatUtil.setCsdBuffers(mediaFormat, format.initializationData);
        // Set format parameters that may be unset.
        MediaFormatUtil.maybeSetFloat(mediaFormat, MediaFormat.KEY_FRAME_RATE, format.frameRate);
        MediaFormatUtil.maybeSetInteger(mediaFormat, MediaFormat.KEY_ROTATION, format.rotationDegrees);
        MediaFormatUtil.maybeSetColorInfo(mediaFormat, format.colorInfo);
       /* if (MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType)) {
            // Some phones require the profile to be set on the codec.
            // See https://github.com/google/ExoPlayer/pull/5438.
            Pair<Integer, Integer> codecProfileAndLevel =
                    MediaCodecUtil.getCodecProfileAndLevel(format.codecs);
            if (codecProfileAndLevel != null) {
                MediaFormatUtil.maybeSetInteger(
                        mediaFormat, MediaFormat.KEY_PROFILE, codecProfileAndLevel.first);
            }
        }*/
        // Set codec max values.
        mediaFormat.setInteger(MediaFormat.KEY_MAX_WIDTH, format.width);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_HEIGHT, format.height);
        MediaFormatUtil.maybeSetInteger(
                mediaFormat, MediaFormat.KEY_MAX_INPUT_SIZE, getMaxInputSize(format));
        // Set codec configuration values.
        if (Util.SDK_INT >= 23) {
            mediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0 /* realtime priority */);
        }
        if (deviceNeedsNoPostProcessWorkaround) {
            mediaFormat.setInteger("no-post-process", 1);
            mediaFormat.setInteger("auto-frc", 0);
        }
        return mediaFormat;
    }


    private static int getMaxInputSize(Format format) {
        // The format defines an explicit maximum input size. Add the total size of initialization
        // data buffers, as they may need to be queued in the same input buffer as the largest sample.
        int totalInitializationDataSize = 0;
        int initializationDataCount = format.initializationData.size();
        for (int i = 0; i < initializationDataCount; i++) {
            totalInitializationDataSize += format.initializationData.get(i).length;
        }
        return format.maxInputSize + totalInitializationDataSize;

    }
}

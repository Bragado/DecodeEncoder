package com.example.decoderencoder.library.core.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.MimeTypes;
import com.example.decoderencoder.library.util.Util;

import java.util.LinkedList;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class MediaCodecAudioRenderer extends MediaCodecRenderer {

    public MediaCodecAudioRenderer(int trackType, Decoder decoder) {
        super(trackType);
        this.decoder = decoder;
        this.pendingDecoderOutputBufferIndices = new LinkedList<Integer>();
        this.pendingDecoderOutputBufferInfos = new LinkedList<MediaCodec.BufferInfo>();
    }

    @Override
    protected void onStarted() {
        if(this.initCodec()) {
            decoder.start();
            getCodecBuffers(decoder);
        }
    }

    @Override
    protected boolean initCodec() {
        Format f = streamFormats[0];
        return decoder.makeCodecReady(getMediaFormat(f, f.sampleMimeType), null) ;
    }

    @Override
    protected void onQueueInputBuffer(DecoderInputBuffer buffer) {

    }

    @Override
    protected void onInputFormatChanged(Format format) {

    }

    @Override
    protected void onReleaseOutputBuffer(MediaCodec.BufferInfo info, int outputBufferIndex) {
        //decoder.releaseOutputBuffer(outputBufferIndex, false);        // do not release this buffer, the encoder will do it
        pendingDecoderOutputBufferIndices.add(outputBufferIndex);       /* The encoder will peek this to feed its input buffer */
        pendingDecoderOutputBufferInfos.add(info);
    }


    @Override
    public SampleStream getStream() {
        return null;
    }

    @Override
    public boolean hasReadStreamToEnd() {
        return false;
    }

    @Override
    public int drainOutputBuffer() {
        return super.drainOutputBuffer(false);
    }

    @Override
    public Decoder getDecoder() {
        return this.decoder;
    }
    @Override
    public MediaFormat getFormat() {
        return getMediaFormat(streamFormats[0], streamFormats[0].sampleMimeType);
    }

    public static MediaFormat getMediaFormat(
            Format format, String codecMimeType) {
        MediaFormat mediaFormat = new MediaFormat();
        // Set format parameters that should always be set.
        mediaFormat.setString(MediaFormat.KEY_MIME, codecMimeType);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, format.channelCount);
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, format.sampleRate);
        MediaFormatUtil.setCsdBuffers(mediaFormat, format.initializationData);
        MediaFormatUtil.maybeSetInteger(
                mediaFormat, MediaFormat.KEY_MAX_INPUT_SIZE, 20000);        // FIXME - This problem was seen in the amlogic s905x
        // Set codec max values.
        // Set codec configuration values.
        if (Util.SDK_INT >= 23) {
            mediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0 /* realtime priority */);
        }
        if (Util.SDK_INT <= 28 && MimeTypes.AUDIO_AC4.equals(format.sampleMimeType)) {
            // On some older builds, the AC-4 decoder expects to receive samples formatted as raw frames
            // not sync frames. Set a format key to override this.
            mediaFormat.setInteger("ac4-is-sync", 1);
        }
        mediaFormat.setString(MediaFormat.KEY_TRACK_ID, format.id);
        mediaFormat.setString(MediaFormat.KEY_LANGUAGE, format.language);
        return mediaFormat;
    }
}

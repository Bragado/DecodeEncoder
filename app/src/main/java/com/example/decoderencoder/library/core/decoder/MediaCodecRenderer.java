package com.example.decoderencoder.library.core.decoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.example.decoderencoder.OpenGL.InputSurface;
import com.example.decoderencoder.OpenGL.OutputSurface;
import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.FormatHolder;
import com.example.decoderencoder.library.source.SampleStream;
import com.example.decoderencoder.library.util.C;
import com.example.decoderencoder.library.util.Util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public abstract class MediaCodecRenderer extends BaseRenderer {

    private static final String TAG = "MediaCodecRenderer";
    protected Decoder decoder;
    private Format codecFormat;
    private final DecoderInputBuffer buffer;
    private int inputIndex = -1;
    private ByteBuffer outputBuffer;
    private MediaCrypto mediaCrypto;
    private ByteBuffer[] inputBuffers;
    protected ByteBuffer[] outputBuffers;
    protected final FormatHolder formatHolder;
    private boolean waitingForFirstSyncSample = false;
    private final ArrayList<Long> decodeOnlyPresentationTimestamps;
    private long largestQueuedPresentationTimeUs;
    private boolean reconfigurationStateWritePending = false;
    private boolean endOfStreamSignaled = false;


    /* Audio Renderers */
    LinkedList<Integer> pendingDecoderOutputBufferIndices;
    LinkedList<MediaCodec.BufferInfo> pendingDecoderOutputBufferInfos;

    /* Video Renderers */
    InputSurface inputSurface = null;
    OutputSurface outputSurface = null;
    Surface surface = null;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public MediaCodecRenderer(int trackType) {
        this(trackType, false);
    }



    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public MediaCodecRenderer(int trackType, boolean decodeOnly) {
        super(trackType);
        buffer = new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DISABLED);
        this.formatHolder = new FormatHolder();
        decodeOnlyPresentationTimestamps = new ArrayList<>();
        largestQueuedPresentationTimeUs = C.TIME_UNSET;
    }


    protected abstract boolean initCodec();


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onDisabled() {
        // TODO: make everything null
        onReset();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onReset() {
        releaseCodec();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onStarted() {
        if(this.initCodec()) {
            decoder.start();
            getCodecBuffers(decoder);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    protected void releaseCodec() {
        codecFormat = null;
        streamIsFinal = false;
        resetInputBuffer();
        resetOutputBuffer();
        resetCodecBuffers();
        try {
            if (decoder != null) {
                try {
                    decoder.stop();
                } finally {
                    decoder.release();
                }
            }
        } finally {
            decoder = null;
            try {
                if (mediaCrypto != null) {
                    mediaCrypto.release();
                }
            } finally {
                mediaCrypto = null;
            }
        }
    }

    @Override
    public MediaCodec.BufferInfo pollBufferInfo() {
        return pendingDecoderOutputBufferInfos.poll();
    }

    @Override
    public int poolBufferIndex() {
        if(pendingDecoderOutputBufferIndices.isEmpty())
            return -1;
        return pendingDecoderOutputBufferIndices.poll();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void SurfaceCreated(InputSurface inputSurface) {
        this.inputSurface = inputSurface;
        this.outputSurface = new OutputSurface();
    }

    public void SurfaceCreated(Surface surface) {
        this.surface = surface;
    }

    protected void onStopped()  {
        decoder.stop();
        decoder.release();
        pendingDecoderOutputBufferIndices = null;
        pendingDecoderOutputBufferInfos = null;
        inputBuffers = null;
        outputBuffers = null;
        inputSurface = null;
        outputSurface = null;
        surface = null;
    }


    private void resetCodecBuffers() {
        if (Util.SDK_INT < 21) {
            inputBuffers = null;
            outputBuffers = null;
        }
    }

    private void resetInputBuffer() {
        inputIndex = C.INDEX_UNSET;
        buffer.data = null;
    }

    private void resetOutputBuffer() {
        outputBuffer = null;
    }

    protected void getCodecBuffers(Decoder codec) {
         if (Util.SDK_INT < 21) {
            inputBuffers = codec.getInputBuffers();
            outputBuffers = codec.getOutputBuffers();
         }
    }

    protected void maybeUpdateCodecBuffers(Decoder codec) {
        if(Util.SDK_INT < 21) {
            outputBuffers = codec.getOutputBuffers();
        }
    }

    private ByteBuffer getInputBuffer(int inputIndex) {
        if(inputIndex < 0)
            return null;
        if (Util.SDK_INT >= 21) {
            return decoder.getInputBuffer(inputIndex);
        } else {
            return inputBuffers[inputIndex];
        }
    }

    public ByteBuffer getOutputBuffer(int outputIndex) {
        if (Util.SDK_INT >= 21) {
            return decoder.getOutputBuffer(outputIndex);
        } else {
            return outputBuffers[outputIndex];
        }
    }
    /**
     * @return Whether it may be possible to feed more input data.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public boolean feedInputBuffer() {
        if(this.endOfStreamSignaled) {
            return false;
        }

        if(!initCodec()) {
            return true;
        }
        if(inputIndex < 0) {
            inputIndex = decoder.dequeueInputBuffer(10);
            buffer.data = getInputBuffer(inputIndex);
            buffer.clear();
        }

        if(buffer.data == null) {
            Log.d(TAG, "Codec Input Buffer is null");
            return true;
        }
        // For adaptive reconfiguration OMX decoders expect all reconfiguration data to be supplied
        // at the start of the buffer that also contains the first frame in the new format.
        if (reconfigurationStateWritePending) {
            reconfigurationStateWritePending = false;
            for (int i = 0; i < codecFormat.initializationData.size(); i++) {
                byte[] data = codecFormat.initializationData.get(i);
                buffer.data.put(data);
            }
        }

        if (this.streamIsFinal) {           // Default transcoder asked to stop this stream
            if(!this.endOfStreamSignaled) {
                decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                resetInputBuffer();
                this.endOfStreamSignaled = true;
            }
            return false;
        }

        int result = readSource(formatHolder, buffer, false);
        if(buffer.getFlag(C.BUFFER_FLAG_DECODE_ONLY) ) {
            return true;
        }

        if (result == C.RESULT_NOTHING_READ) {
            Log.d(TAG, "RESULT_NOTHING_READ");
            return false;
        }
        if (result == C.RESULT_FORMAT_READ) {
            Log.d(TAG, "RESULT_FORMAT_READ");
            reconfigurationStateWritePending = true;
            // We received two formats in a row. Clear the current buffer of any reconfiguration data
            // associated with the first format.
            buffer.clear();
            codecFormat = formatHolder.format;
            return true;
        }


        if (waitingForFirstSyncSample && !buffer.isKeyFrame()) {
            buffer.clear();
            return true;
        }
        waitingForFirstSyncSample = false;

        try {
            long presentationTimeUs = buffer.timeUs;
            /*if (buffer.isDecodeOnly()) {      // FIXME : this is used to regiter negative pts, check if we need this
                decodeOnlyPresentationTimestamps.add(presentationTimeUs);
            }*/
            if(buffer.isEndOfStream()) {
                decoder.queueInputBuffer(inputIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                resetInputBuffer();
            }

            largestQueuedPresentationTimeUs =
                    Math.max(largestQueuedPresentationTimeUs, presentationTimeUs);

            buffer.flip();
            onQueueInputBuffer(buffer);

            decoder.queueInputBuffer(inputIndex, 0, buffer.data.limit(), presentationTimeUs, 0);
            Log.i(TAG, "DATA WAS WRITTEN TO DECODER for stream: " + codecFormat.sampleMimeType);
            resetInputBuffer();
        } catch (MediaCodec.CryptoException e) {
            throw e;
        }
        resetInputBuffer();
        return true;
    }

    /**
     *
     * @return Whether it may be possible to drain more output data.
     */
    protected boolean drainOutputBuffer(boolean render) {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);

        switch (outputBufferIndex) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                maybeUpdateCodecBuffers(decoder);
                break;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                break;
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                break;
            default:            // in case index is correct
                onReleaseOutputBuffer(bufferInfo, outputBufferIndex);           // let the child of this class handle the data
                Log.i(TAG, "DATA WAS READ FROM DECODER for stream: " + codecFormat.sampleMimeType);
                break;
        }
        boolean isEndOfStream = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
        if(isEndOfStream) {
            return false;
        }
        return true;
    }

    /* Methods called  by MediaCodecRenderer before execute the action */

    protected abstract void onQueueInputBuffer(DecoderInputBuffer buffer);

    protected abstract void onInputFormatChanged(Format format);

    protected abstract void onReleaseOutputBuffer(MediaCodec.BufferInfo info, int outputBufferIndex);






}

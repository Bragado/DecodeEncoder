package com.example.decoderencoder.library.source;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.example.decoderencoder.library.Format;
import com.example.decoderencoder.library.FormatHolder;
import com.example.decoderencoder.library.core.decoder.DecoderInputBuffer;
import com.example.decoderencoder.library.extractor.DefaultExtractorInput;
import com.example.decoderencoder.library.extractor.Extractor;
import com.example.decoderencoder.library.extractor.ExtractorInput;
import com.example.decoderencoder.library.extractor.ExtractorOutput;
import com.example.decoderencoder.library.extractor.PositionHolder;
import com.example.decoderencoder.library.extractor.SeekMap;
import com.example.decoderencoder.library.extractor.TrackOutput;
import com.example.decoderencoder.library.extractor.ts.ElementaryStreamReader;
import com.example.decoderencoder.library.extractor.ts.SeiReader;
import com.example.decoderencoder.library.metadata.Metadata;
import com.example.decoderencoder.library.metadata.icy.IcyHeaders;
import com.example.decoderencoder.library.network.Allocator;
import com.example.decoderencoder.library.network.DataSource;
import com.example.decoderencoder.library.network.DataSpec;
import com.example.decoderencoder.library.network.LoadErrorHandlingPolicy;
import com.example.decoderencoder.library.network.Loader;
import com.example.decoderencoder.library.network.StatsDataSource;
import com.example.decoderencoder.library.util.Assertions;
import com.example.decoderencoder.library.util.C;
import com.example.decoderencoder.library.util.ConditionVariable;
import com.example.decoderencoder.library.util.Log;
import com.example.decoderencoder.library.util.MimeTypes;
import com.example.decoderencoder.library.util.Util;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

public class MediaSource implements ExtractorOutput, SampleQueue.UpstreamFormatChangedListener, Media,
        Loader.Callback<MediaSource.ExtractingLoadable>,
        Loader.ReleaseCallback {

    public static final String TAG = "MEDIASOURCE";
    public static final int DEFAULT_LOADING_CHECK_INTERVAL_BYTES = 1024 * 1024;

    private final Uri uri;
    private final DataSource dataSource;
    private final Allocator allocator;
    private final long continueLoadingCheckIntervalBytes;
    private final ExtractorHolder extractorHolder;
    private final Handler handler;
    private final Runnable maybeFinishPrepareRunnable;
    private final Runnable onContinueLoadingRequestedRunnable;
    private Media.Callback callback = null;     // FIXME

    private TrackId[] sampleQueueTrackIds;
    private SampleQueue[] sampleQueues;
    private Object[] readers;
    private boolean sampleQueuesBuilt;
    private boolean loadingFinished;
    private boolean notifyDiscontinuity;
    private long pendingResetPositionUs;
    private long lastSeekPositionUs;
    private PreparedState preparedState;
    private boolean released = false;
    private final ConditionVariable loadCondition;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private boolean prepared;
    private final Loader loader;
    private SampleStream[] sampleStreams = null;

    PositionHolder positionHolder = new PositionHolder();
    DataSpec dataSpec;
    boolean loadCanceled = false;
    private long length = C.LENGTH_UNSET;
    private final ExtractorOutput extractorOutput = this;
    private int dataType;
    private SeekMap seekMap;
    private boolean loadCompleted = false;

    public MediaSource(
            Uri uri,
            DataSource dataSource,
            Extractor[] extractors,
            Allocator allocator,
            int continueLoadingCheckIntervalBytes,
            LoadErrorHandlingPolicy loadErrorHandlingPolicy
            ) {
        this.uri = uri;
        this.dataSource = dataSource;
        this.allocator = allocator;
        this.continueLoadingCheckIntervalBytes = continueLoadingCheckIntervalBytes;
        extractorHolder = new ExtractorHolder(extractors);
        maybeFinishPrepareRunnable = this::maybeFinishPrepare;
        onContinueLoadingRequestedRunnable =
                () -> {
                    if (!released) {
                        Assertions.checkNotNull(callback)
                                .onContinueLoadingRequested(MediaSource.this);
                    }
                };
        handler = new Handler();
        sampleQueueTrackIds = new TrackId[0];
        sampleQueues = new SampleQueue[0];
        this.readers = new Object[0];
        pendingResetPositionUs = C.TIME_UNSET;
        loader = new Loader("Loader:MediaSource");
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        loadCondition = new ConditionVariable();
        dataType = C.DATA_TYPE_MEDIA;
    }


    // ExtractorOutput Interface Implementation
    @Override
    public TrackOutput track(Object reader, int id, int type) {
        return prepareTrackOutput(new TrackId(id, /* isIcyTrack= */ false), reader);
    }

    @Override
    public void endTracks() {       // when all tracks are identified
        sampleQueuesBuilt = true;
        handler.post(maybeFinishPrepareRunnable);
    }

    @Override
    public void seekMap(SeekMap seekMap) {
        this.seekMap = seekMap != null ? seekMap : new SeekMap.Unseekable(/* durationUs */ C.TIME_UNSET);
        handler.post(maybeFinishPrepareRunnable);
    }

    // Media Interface implementation
    @Override
    public TrackGroupArray getTrackGroups() {
        return preparedState.tracks;
    }

    @Override
    public boolean continueLoading(long positionUs) {
        if (loadingFinished
                || loader.hasFatalError()
                /*|| !prepared */) {
            return false;
        }
        boolean continuedLoading = loadCondition.open();
        if (!loader.isLoading()) {
            //startLoading();
            //continuedLoading = true;
        }
        return continuedLoading;
    }

    @Override
    public void prepare(Callback callback, long positionUs) {
        this.callback = callback;
        loadCondition.open();
        startLoading();
    }

    private void startLoading() {       // starts extracting data from the network/file
        ExtractingLoadable loadable =
                new ExtractingLoadable(
                        uri, dataSource, extractorHolder, /* extractorOutput= */ this, loadCondition);
        /*if (prepared) {
            SeekMap seekMap = preparedState.seekMap;
            Assertions.checkState(isPendingReset());

            loadable.setLoadPosition(
                    seekMap.getSeekPoints(pendingResetPositionUs).first.position, pendingResetPositionUs);
            pendingResetPositionUs = C.TIME_UNSET;
        }*/
        long elapsedRealtimeMs =
                loader.startLoading(
                        loadable, this, loadErrorHandlingPolicy.getMinimumLoadableRetryCount(dataType));

    }

    @Override
    public void maybeThrowPrepareError() throws IOException {

    }

    @Override
    public SampleStream[] getSampleStreams() {
        if(this.sampleStreams != null)
            return this.sampleStreams;

        PreparedState preparedState = this.preparedState;
        TrackGroupArray tracks = preparedState.tracks;

        this.sampleStreams = new SampleStream[tracks.length];
        for(int i = 0; i < tracks.length; i++) {
            this.sampleStreams[i] = new SampleStreamImpl(i);
        }

        return this.sampleStreams;
    }

    @Override
    public void discardTracks(boolean discard, int[] tracksID) {
        int k = 0;
        for(int i = 0; i < sampleQueues.length; i++) {
            if(tracksID.length > k) {
                if(tracksID[k] == sampleQueueTrackIds[i].id) {
                    if(readers[i] != null) {
                        if(readers[i] instanceof ElementaryStreamReader) {
                            ((ElementaryStreamReader)readers[i]).discardStream(discard);
                        }else if(readers[i] instanceof SeiReader) {
                            ((SeiReader)readers[i]).discardStream(discard);
                        }
                    }else {
                        Log.e(TAG, "This stream with ID " + tracksID[i] + " will not be discarded because you must implemented it yet. Follow the previous examples");
                    }
                    k++;
                }
            }
        }
    }

    // Loader Callbacks implementation
    @Override
    public void onLoadCompleted(ExtractingLoadable loadable, long elapsedRealtimeMs, long loadDurationMs) {     // End of the stream callback (ex. reached the end of the file)
        Log.d(TAG, "onLoadCompleted");
        loadCompleted = true;
    }

    @Override
    public void onLoadCanceled(ExtractingLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {

    }

    @Override
    public Loader.LoadErrorAction onLoadError(ExtractingLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
        return null;
    }

    @Override
    public void onLoaderReleased() {            // when all kept resources by loader are released

    }

    // Internals
    private TrackOutput prepareTrackOutput(TrackId id, Object reader) {            // Loader Thread
        int trackCount = sampleQueues.length;
        for (int i = 0; i < trackCount; i++) {
            if (id.equals(sampleQueueTrackIds[i])) {
                return sampleQueues[i];
            }
        }
        SampleQueue trackOutput = new SampleQueue(allocator);
        trackOutput.setUpstreamFormatChangeListener(this);
        TrackId[] sampleQueueTrackIds = Arrays.copyOf(this.sampleQueueTrackIds, trackCount + 1);
        sampleQueueTrackIds[trackCount] = id;
        this.sampleQueueTrackIds = Util.castNonNullTypeArray(sampleQueueTrackIds);

        Object[] readers = Arrays.copyOf(this.readers, trackCount + 1);
        readers[trackCount] = reader;
        this.readers = readers;
        SampleQueue[] sampleQueues = Arrays.copyOf(this.sampleQueues, trackCount + 1);
        sampleQueues[trackCount] = trackOutput;
        this.sampleQueues = Util.castNonNullTypeArray(sampleQueues);

        return trackOutput;
    }

    private void maybeFinishPrepare() {         // parent thread
        SeekMap seekMap = this.seekMap;
        if (released || prepared || !sampleQueuesBuilt) {
            return;
        }
        for (SampleQueue sampleQueue : sampleQueues) {
            if (sampleQueue.getUpstreamFormat() == null) {
                return;
            }
        }
        loadCondition.close();
        int trackCount = sampleQueues.length;
        TrackGroup[] trackArray = new TrackGroup[trackCount];
        boolean[] trackIsAudioVideoFlags = new boolean[trackCount];
        for (int i = 0; i < trackCount; i++) {
            Format trackFormat = sampleQueues[i].getUpstreamFormat();
            String mimeType = trackFormat.sampleMimeType;
            boolean isAudio = MimeTypes.isAudio(mimeType);
            boolean isAudioVideo = isAudio || MimeTypes.isVideo(mimeType);
            trackIsAudioVideoFlags[i] = isAudioVideo;
            trackArray[i] = new TrackGroup(trackFormat);
        }
        dataType =
                length == C.LENGTH_UNSET && seekMap.getDurationUs() == C.TIME_UNSET
                        ? C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE
                        : C.DATA_TYPE_MEDIA;
        preparedState =
                new PreparedState(seekMap, new TrackGroupArray(trackArray), trackIsAudioVideoFlags);
        prepared = true;
        callback.onPrepared(this);

    }

    public void setLoadStatus(boolean status) {
        loadCanceled = status;
    }

    private DataSpec buildDataSpec(long position) {
        // Disable caching if the content length cannot be resolved, since this is indicative of a
        // progressive live stream.
        return new DataSpec(
                uri,
                position,
                C.LENGTH_UNSET,
                "",
                DataSpec.FLAG_ALLOW_ICY_METADATA
                        | DataSpec.FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN
                        | DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION);
    }

    @Override
    public void onUpstreamFormatChanged(Format format) {        // called when the actual metadata (about wich tracks are available) is ready
        handler.post(maybeFinishPrepareRunnable);
    }


    // Loadable implementation for extracting information

    final class ExtractingLoadable implements Loader.Loadable {     // Loading Thread only

        private final StatsDataSource dataSource;
        private final ExtractorHolder extractorHolder;
        private final ExtractorOutput extractorOutput;
        private final ConditionVariable loadCondition;
        private final PositionHolder positionHolder;

        private volatile boolean loadCanceled;


        private DataSpec dataSpec;
        private long length;

        @SuppressWarnings("method.invocation.invalid")
        public ExtractingLoadable(
                Uri uri,
                DataSource dataSource,
                ExtractorHolder extractorHolder,
                ExtractorOutput extractorOutput,
                ConditionVariable loadCondition) {

            this.dataSource = new StatsDataSource(dataSource);
            this.extractorHolder = extractorHolder;
            this.extractorOutput = extractorOutput;
            this.loadCondition = loadCondition;
            this.positionHolder = new PositionHolder();
            this.length = C.LENGTH_UNSET;
            dataSpec = buildDataSpec(/* position= */ 0);
        }

        @Override
        public void cancelLoad() {
            loadCanceled = true;
        }

        @Override
        public void load() throws IOException, InterruptedException {
            int result = Extractor.RESULT_CONTINUE;
            while (result == Extractor.RESULT_CONTINUE && !loadCanceled) {
                ExtractorInput input = null;
                try {
                    long position = positionHolder.position;
                    dataSpec = buildDataSpec(position);
                    length = dataSource.open(dataSpec);
                    if (length != C.LENGTH_UNSET) {
                        length += position;
                    }
                    Uri uri = Assertions.checkNotNull(dataSource.getUri());
                    DataSource extractorDataSource = dataSource;
                    input = new DefaultExtractorInput(extractorDataSource, position, length);
                    Extractor extractor = extractorHolder.selectExtractor(input, extractorOutput, uri);
                    while (result == Extractor.RESULT_CONTINUE && !loadCanceled) {
                        loadCondition.block();
                        result = extractor.read(input, positionHolder);
                        if (input.getPosition() > position + continueLoadingCheckIntervalBytes) {
                            position = input.getPosition();
                            loadCondition.close();
                            handler.post(onContinueLoadingRequestedRunnable);
                        }
                    }
                } finally {
                    if (result == Extractor.RESULT_SEEK) {
                        result = Extractor.RESULT_CONTINUE;
                    } else if (input != null) {
                        positionHolder.position = input.getPosition();
                    }
                    Util.closeQuietly(dataSource);
                }
            }
        }

        private void setLoadPosition(long position, long timeUs) {
            positionHolder.position = position;
        }
    }


    // SampleStream Implementation

    private final class SampleStreamImpl implements SampleStream {

        private final int track;

        public SampleStreamImpl(int track) {
            this.track = track;
        }

        @Override
        public boolean isReady() {
            return MediaSource.this.isReady(track);
        }

        @Override
        public void maybeThrowError() throws IOException {
            MediaSource.this.maybeThrowError();
        }

        @Override
        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean formatRequired) {
            return MediaSource.this.readData(track, formatHolder, buffer, formatRequired);
        }

    }

    private void maybeThrowError() {
    }

    private int readData(int track, FormatHolder formatHolder, DecoderInputBuffer buffer,
                 boolean formatRequired) {
        if (suppressRead()) {
            return C.RESULT_NOTHING_READ;
        }
        //maybeNotifyDownstreamFormat(track); FIXME
        int result =
                sampleQueues[track].read(
                        formatHolder, buffer, formatRequired, loadingFinished, lastSeekPositionUs);
        if (result == C.RESULT_NOTHING_READ && loadCompleted) {
            result = C.RESULT_END_OF_INPUT;
            //maybeStartDeferredRetry(track);
        }
        return result;
    }

    private void maybeStartDeferredRetry(int track) {
       /* boolean[] trackIsAudioVideoFlags = getPreparedState().trackIsAudioVideoFlags;
        if (!pendingDeferredRetry
                || !trackIsAudioVideoFlags[track]
                || sampleQueues[track].hasNextSample()) {
            return;
        }
        pendingResetPositionUs = 0;
        pendingDeferredRetry = false;
        notifyDiscontinuity = true;
        lastSeekPositionUs = 0;
        extractedSamplesCountAtStartOfLoad = 0;
        for (SampleQueue sampleQueue : sampleQueues) {
            sampleQueue.reset();
        }
        Assertions.checkNotNull(callback).onContinueLoadingRequested(this);*/
    }

    public PreparedState getPreparedState() {
        return this.preparedState;
    }

    private boolean isReady(int track) {
        return !suppressRead() && (loadingFinished || sampleQueues[track].hasNextSample());
    }

    private boolean suppressRead() {
        return notifyDiscontinuity || isPendingReset();
    }

    private boolean isPendingReset() {
        return pendingResetPositionUs != C.TIME_UNSET;
    }



    /** Stores a list of extractors and a selected extractor when the format has been detected. */
    private static final class ExtractorHolder {

        private final Extractor[] extractors;

        private Extractor extractor;

        /**
         * Creates a holder that will select an extractor and initialize it using the specified output.
         *
         * @param extractors One or more extractors to choose from.
         */
        public ExtractorHolder(Extractor[] extractors) {
            this.extractors = extractors;
        }

        /**
         * Returns an initialized extractor for reading {@code input}, and returns the same extractor on
         * later calls.
         *
         * @param input The {@link ExtractorInput} from which data should be read.
         * @param output The {@link ExtractorOutput} that will be used to initialize the selected
         *     extractor.
         * @param uri The {@link Uri} of the data.
         * @return An initialized extractor for reading {@code input}.
         * @throws IOException Thrown if the input could not be read.
         * @throws InterruptedException Thrown if the thread was interrupted.
         */
        public Extractor selectExtractor(ExtractorInput input, ExtractorOutput output, Uri uri)
                throws IOException, InterruptedException {
            if (extractor != null) {
                return extractor;
            }
            if (extractors.length == 1) {
                this.extractor = extractors[0];
            } else {
                for (Extractor extractor : extractors) {
                    try {
                        if (extractor.sniff(input)) {
                            this.extractor = extractor;
                            break;
                        }
                    } catch (EOFException e) {
                        // Do nothing.
                    } finally {
                        input.resetPeekPosition();
                    }
                }
                if (extractor == null) {
                    throw new InterruptedException(
                            "None of the available extractors ("
                                    + Util.getCommaDelimitedSimpleClassNames(extractors)
                                    + ") could read the stream." +
                            uri);
                }
            }
            extractor.init(output);
            return extractor;
        }

        public void release() {
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
        }
    }

    /** Stores state that is initialized when preparation completes. */
    public static final class PreparedState {

        public enum TRACKSTATE {
            SELECTED,
            PASSTROUGH,
            DISCARD
        }

        public final SeekMap seekMap;
        public final TrackGroupArray tracks;
        public final boolean[] trackIsAudioVideoFlags;
        public final TRACKSTATE[] trackEnabledStates;
        public final boolean[] trackNotifiedDownstreamFormats;

        public PreparedState(
                SeekMap seekMap, TrackGroupArray tracks, boolean[] trackIsAudioVideoFlags) {
            this.seekMap = seekMap;
            this.tracks = tracks;
            this.trackIsAudioVideoFlags = trackIsAudioVideoFlags;
            this.trackEnabledStates = new TRACKSTATE[tracks.length];
            this.trackNotifiedDownstreamFormats = new boolean[tracks.length];
        }
    }

    /** Identifies a track. */
    private static final class TrackId {

        public final int id;
        public final boolean isIcyTrack;

        public TrackId(int id, boolean isIcyTrack) {
            this.id = id;
            this.isIcyTrack = isIcyTrack;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MediaSource.TrackId other = (MediaSource.TrackId) obj;
            return id == other.id && isIcyTrack == other.isIcyTrack;
        }

        @Override
        public int hashCode() {
            return 31 * id + (isIcyTrack ? 1 : 0);
        }
    }
}

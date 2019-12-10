package com.example.decoderencoder.library.source;

import android.net.Uri;

import com.example.decoderencoder.library.extractor.DefaultExtractorsFactory;
import com.example.decoderencoder.library.extractor.ExtractorsFactory;
import com.example.decoderencoder.library.network.DataSource;
import com.example.decoderencoder.library.network.TransferListener;

public class ProgressiveMediaSource {

    private final Uri uri;
    private final DataSource.Factory dataSourceFactory;
    private final ExtractorsFactory extractorsFactory;
    private TransferListener transferListener;              // FIXME
    private boolean isCreateCalled = false;

    public final class Factory {
        private final DataSource.Factory dataSourceFactory;
        private ExtractorsFactory extractorsFactory;
        public Factory(DataSource.Factory dataSourceFactory) {
            this(dataSourceFactory, new DefaultExtractorsFactory());
        }

        public Factory(DataSource.Factory dataSourceFactory, ExtractorsFactory extractorsFactory) {
            this.dataSourceFactory = dataSourceFactory;
            this.extractorsFactory = extractorsFactory;
        }

        public ProgressiveMediaSource createMediaSource(Uri uri) {
            isCreateCalled = true;
            return new ProgressiveMediaSource(
                    uri,
                    dataSourceFactory,
                    extractorsFactory);
        }
    }


    ProgressiveMediaSource(Uri uri, DataSource.Factory dataSourceFactory, ExtractorsFactory extractorsFactory) {
        this.uri = uri;
        this.dataSourceFactory = dataSourceFactory;
        this.extractorsFactory = extractorsFactory;
    }





}

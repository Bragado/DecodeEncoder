package com.example.decoderencoder.library.network.utils;

import android.net.Uri;

public class StreamDescriptor {

    private Uri uri;

    public StreamDescriptor(Uri uri) {
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }

    public StreamDescriptor.SCHEMES getStreamType() {         //TODO
        return null;
    }

    public enum SCHEMES {
        UDP_STREAM
    }
}

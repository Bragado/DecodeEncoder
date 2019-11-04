package com.example.decoderencoder.library.network;

import com.example.decoderencoder.library.network.utils.StreamDescriptor;
import com.example.decoderencoder.library.network.utils.TransferListener;

import java.net.SocketException;

public class HttpSource extends GenericSource {

    public HttpSource() {
        super();
    }

    public HttpSource(TransferListener transferListener) {
        super();
        super.addListener(transferListener);
    }

    @Override
    public boolean open(StreamDescriptor streamDescriptor) throws SocketException {
        return false;
    }

    @Override
    public DataSource getInstance() {
        return this;
    }

    @Override
    public boolean close() {
        return false;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) {
        return 0;
    }
}

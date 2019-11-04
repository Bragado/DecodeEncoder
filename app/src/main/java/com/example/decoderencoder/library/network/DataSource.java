package com.example.decoderencoder.library.network;

import com.example.decoderencoder.library.network.utils.StreamDescriptor;
import com.example.decoderencoder.library.network.utils.TransferListener;

import java.net.SocketException;

public interface DataSource {

    public boolean open(StreamDescriptor streamDescriptor) throws Exception;

    public DataSource getInstance();

    public boolean close();

    public int read(byte[] buffer, int offset, int readLength);

    public void addListener(TransferListener transferListener);

}

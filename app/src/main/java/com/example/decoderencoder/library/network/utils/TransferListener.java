package com.example.decoderencoder.library.network.utils;

import com.example.decoderencoder.library.network.DataSource;

public interface TransferListener {

    public void onTransferStarted(DataSource dataSource, StreamDescriptor streamDescriptor);

    public void onTransferEnded(DataSource dataSource, StreamDescriptor streamDescriptor);

    public void onTransferReceived(DataSource dataSource, StreamDescriptor streamDescriptor, int bytes);

}

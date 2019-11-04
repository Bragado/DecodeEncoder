package com.example.decoderencoder.library.network;

import com.example.decoderencoder.library.network.utils.StreamDescriptor;
import com.example.decoderencoder.library.network.utils.TransferListener;

import java.util.ArrayList;

public abstract class GenericSource implements DataSource {

    public static final int BUFFER_MAX_SIZE = 500;
    private int size = BUFFER_MAX_SIZE;
    private ArrayList<TransferListener> listeners = new ArrayList<TransferListener>();

    protected StreamDescriptor streamDescriptor = null;

    public boolean setStreamDescriptor(StreamDescriptor streamDescriptor) {
        boolean ret = streamDescriptor != null ? false : true;
        this.streamDescriptor = streamDescriptor;
        return ret;
    }

    public void addListener(TransferListener transferListener) {
        listeners.add(transferListener);
    }

    public void transferStarted() {
        for(TransferListener transferListener : listeners) {
            transferListener.onTransferStarted(this, streamDescriptor);
        }
    }

    public void transferEnded()  {
        for(TransferListener transferListener : listeners) {
            transferListener.onTransferEnded(this, streamDescriptor);
        }
    }

    public void bytesTransfered(int bytesReceived) {
        for(TransferListener transferListener : listeners) {
            transferListener.onTransferReceived(this, streamDescriptor, bytesReceived);
        }
    }

}

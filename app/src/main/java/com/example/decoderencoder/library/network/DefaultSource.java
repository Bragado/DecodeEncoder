package com.example.decoderencoder.library.network;

import android.content.Context;

import com.example.decoderencoder.library.network.utils.StreamDescriptor;
import com.example.decoderencoder.library.network.utils.TransferListener;

import java.net.SocketException;

import static com.example.decoderencoder.library.network.utils.StreamDescriptor.SCHEMES.UDP_STREAM;

/**
 * This class is the default source for all kinds of streams
 * This is the class that represents all the available sources supported
 */
public class DefaultSource implements DataSource{

    public static final String TAG = "Default Source";

    private DataSource dataSource = null;
    private Context context;


    public DefaultSource(Context context) {
        super();
        this.context = context;
    }

    @Override
    public boolean open(StreamDescriptor streamDescriptor) throws Exception {
        StreamDescriptor.SCHEMES streamType = streamDescriptor.getStreamType();
        switch(streamType) {
            case UDP_STREAM:
                maybeInitializeUdpSource();
                break;
            default:
                throw new Exception("Stream Type not Implemented");

        }


        return dataSource.open(streamDescriptor);
    }

    private void maybeInitializeUdpSource() {
        if(dataSource != null && !(dataSource instanceof UdpSource)) {
            dataSource.close();
            dataSource = null;
            dataSource = new UdpSource();
        }else if(dataSource == null) {
            dataSource = new UdpSource();
        }

    }

    @Override
    public DataSource getInstance() {
        return dataSource.getInstance();
    }

    @Override
    public boolean close() {
        return dataSource.close();
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) {
        return dataSource.read(buffer, offset, readLength);
    }

    @Override
    public void addListener(TransferListener transferListener) {        // TODO

    }
}

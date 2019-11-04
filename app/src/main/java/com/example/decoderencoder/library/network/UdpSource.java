package com.example.decoderencoder.library.network;

import android.net.Uri;

import com.example.decoderencoder.library.network.utils.StreamDescriptor;
import com.example.decoderencoder.library.network.utils.TransferListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Arrays;

public class UdpSource extends GenericSource {

    public static int PACKET_MAX_SIZE = 2000;
    public static int SOCKET_TIMOUT = 0;

    DatagramSocket channel = null;
    InetAddress inetAddress = null;
    InetSocketAddress inetSocketAddress;
    MulticastSocket multicastSocket = null;
    DatagramPacket datagramPacket = null;
    byte[] buffer = new byte[PACKET_MAX_SIZE];
    int port = -1;
    Uri uri;

    public UdpSource() {
        super();
        DatagramPacket datagramPacket = new DatagramPacket(buffer, PACKET_MAX_SIZE);
    }

    public UdpSource(TransferListener transferListener) {
        super();
        addListener(transferListener);
        DatagramPacket datagramPacket = new DatagramPacket(buffer, PACKET_MAX_SIZE);
    }

    @Override
    public boolean open(StreamDescriptor streamDescriptor) throws SocketException {
        channel = new DatagramSocket();
        uri = streamDescriptor.getUri();
        String host = uri.getHost();
        port = uri.getPort();

        try {
            inetAddress = InetAddress.getByName(host);
            inetSocketAddress = new InetSocketAddress(inetAddress, port);
            if (inetAddress.isMulticastAddress()) {
                multicastSocket = new MulticastSocket(inetSocketAddress);
                multicastSocket.joinGroup(inetAddress);
                channel = multicastSocket;
            } else {
                channel = new DatagramSocket(inetSocketAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            channel.setSoTimeout(SOCKET_TIMOUT);
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
        transferStarted();
        return true;
    }

    @Override
    public DataSource getInstance() {
        return this;
    }

    @Override
    public boolean close() {
        transferEnded();
        if(multicastSocket != null) {
            try {
                multicastSocket.leaveGroup(inetAddress);
                multicastSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        if(channel != null) {
            channel.close();
        }
        inetAddress = null;
        inetSocketAddress = null;
        return true;
    }

    @Override
    public int read(byte[] buf, int offset, int readLength) {
        int bytes_read = 0;
        try {
            channel.receive(datagramPacket);
            bytes_read = datagramPacket.getLength();
            bytesTransfered(bytes_read);
            buf = Arrays.copyOf(buffer, bytes_read);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes_read;
    }
}

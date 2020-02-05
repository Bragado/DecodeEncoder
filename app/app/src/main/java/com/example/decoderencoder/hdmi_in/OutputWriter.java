package com.example.decoderencoder.hdmi_in;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import com.realtek.hardware.RtkHDMIRxManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class OutputWriter {
    private static final String TAG = "OutputWriter";

    private static final int ERROR = 0;
    private static final int ERRORTIME = 1000;

    private Context mContext = null;
    private RecorderClass mRecorderClass = null;


    private Handler mHandler = null;

    //188 bytes per TS package. 7 packages max for an MTU of 1500.
    private static final int UDP_Packet_Size = 188 * 7;
    private InetAddress mAddress = null;
    private int mPort = 0;

    private ParcelFileDescriptor mReadPipe = null;
    private ParcelFileDescriptor mUDP_WritePipe = null;
    private DisplayState mDesireState = null;
    private OutputStream mFileOutputStream = null;
    private long mLocalFileSize = 0;
    private DatagramSocket udpSocket = null;
    private final int TOAST_DURATION = Toast.LENGTH_SHORT;

    //100MB
    private static final long LOCAL_COPY_MIN_FREE_DISK = 100L * 1024L * 1024L;
    //1.5GB
    private static final long LOCAL_COPY_MAX_FILE_SIZE = 1536L * 1024L * 1024L;

    public OutputWriter(Context context, RecorderClass recorderClass) {
        this.mContext = context;
        this.mRecorderClass = recorderClass;
        initHandler();
    }

    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case ERROR:
                        if (((Integer) message.obj).intValue() == 1) {
                            //Toast.makeText(RecordActivity.this, "multicase_ip_error", TOAST_DURATION).show();
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
    }

    public ParcelFileDescriptor prepareIO(DisplayState desireState) {
        mDesireState = desireState;
        try {
            ParcelFileDescriptor[] createPipe = ParcelFileDescriptor.createPipe();
            if (createPipe == null) {
                return null;
            }
            mReadPipe = createPipe[0];
            mUDP_WritePipe = createPipe[1];
            mAddress = InetAddress.getByName(desireState.UDP_Target_IP);
            Log.i(TAG, "mIp = " + desireState.UDP_Target_IP + "  mPort = " + desireState.UDP_Target_Port + "  Multicast = " + mAddress.isMulticastAddress());
            mPort = desireState.UDP_Target_Port;
            try {
                if (desireState.HdmiVideoStream) {
                    if (mAddress.isMulticastAddress() == false) {
                        udpSocket = new DatagramSocket(null);
                        udpSocket.setReuseAddress(true);
                        udpSocket.bind(null);
                    } else {
                        udpSocket = new MulticastSocket(desireState.UDP_Target_Port);
                        udpSocket.setBroadcast(true);
                        udpSocket.setReuseAddress(true);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.v(TAG, "e1", e);
                mUDP_WritePipe.close();
                mUDP_WritePipe = null;
                mReadPipe.close();
                mReadPipe = null;
                return null;
            }
            StartWritingOutput();
            return this.mUDP_WritePipe;
        } catch (IOException e3) {
            Log.v(TAG, "e3", e3);
            return null;
        }
    }

    private void StartWritingOutput() {
        new Thread(new Runnable() {
            public void run() {
                Log.i(TAG, "start WritingOutput --- ");
                InputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(mReadPipe);
                byte[] bArr = new byte[UDP_Packet_Size];
                Object obj = 1;
                int counterTillNextCheckOfFreeDiskSpace = 0;
                boolean counterIncreased = false;
                while (mRecorderClass.IsHdmiVideoRecording()) {
                    try {

                            if (!counterIncreased) {
                                counterTillNextCheckOfFreeDiskSpace = counterTillNextCheckOfFreeDiskSpace + 1;
                                counterIncreased = true;
                            }
                            int read = autoCloseInputStream.read(bArr);
                            //Log.d(TAG, "Read " + read +" bit from encoder.");
                            if (read > 0) {
                                if (udpSocket != null) {
                                    DatagramPacket datagramPacket = new DatagramPacket(bArr, 0, read, mAddress, mPort);
                                    udpSocket.send(datagramPacket);
                                }
                                obj = 1;
                                if (mFileOutputStream != null) {
                                    mFileOutputStream.write(bArr, 0, read);
                                    mLocalFileSize = mLocalFileSize + read;
                                }
                            }

                    } catch (Exception e2) {
                        e2.printStackTrace();
                        if (obj != null) {
                            Log.v(TAG, "udp StartWritingOutput error", e2);
                        }
                        obj = null;
                    }
                }
                Log.i(TAG, "end WritingOutput --- ");
                if (mFileOutputStream != null) {
                    try {
                        mFileOutputStream.flush();
                        mFileOutputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "LocalOutputStream.flush failed.", e);
                    }
                }
                try {
                    Log.i(TAG, "stop StartWritingOutput --- ");
                    mUDP_WritePipe.close();
                    mUDP_WritePipe = null;
                    mReadPipe.close();
                    mReadPipe = null;
                    autoCloseInputStream.close();
                } catch (Exception e22) {
                    e22.printStackTrace();
                }
                try {
                    if (udpSocket != null) {
                        udpSocket.close();
                        udpSocket = null;
                        Log.i(TAG, "stop udpSocket close --- ");
                    }
                } catch (Exception e222) {
                    e222.printStackTrace();
                }
            }
        }).start();
    }
}

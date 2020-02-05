package com.example.decoderencoder.hdmi_in;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.example.decoderencoder.UserSelectedParams;

import java.util.ArrayList;

/**
 * Created by Developer on 29/07/2017.
 */

public class RecorderServiceBindTool {
    private static final String TAG = "RecorderServiceBindTool";
    private static RecorderServiceBindTool mBindRecorderServiceTool = null;
    private Context mContext = null;
    private RecorderService mRecorderService = null;
    private ServiceConnection mServiceConnection = null;
    private ArrayList<RecorderServiceListener> mServiceListenerList = new ArrayList();

    public interface RecorderServiceListener {
        void service(RecorderService recorderService);
    }

    public RecorderServiceBindTool(Context context, UserSelectedParams userSelectedParams) {
        this.mContext = context;
        startService();
        bindService(userSelectedParams);
    }

    private void bindService(UserSelectedParams userSelectedParams) {
        this.mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(TAG, "bindService RecorderServiceBindTool onServiceConnected");
                RecorderServiceBindTool.this.mRecorderService = ((RecorderService.LocalBinder) service).getService();
                int size = RecorderServiceBindTool.this.mServiceListenerList.size();
                for (int i = 0; i < size; i++) {
                    ((RecorderServiceListener) RecorderServiceBindTool.this.mServiceListenerList.get(i)).service(RecorderServiceBindTool.this.mRecorderService);
                }
                RecorderServiceBindTool.this.mServiceListenerList.clear();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "bindService RecorderService onServiceDisconnected");
                RecorderServiceBindTool.this.mRecorderService = null;
            }
        };
        Intent service = new Intent(this.mContext, RecorderService.class);
        service.putExtra("user_parameters", userSelectedParams);
        this.mContext.bindService(service, this.mServiceConnection, mContext.BIND_AUTO_CREATE);
    }

    public static RecorderServiceBindTool getInstance(Context context, UserSelectedParams userSelectedParams) {
        if (mBindRecorderServiceTool == null) {
            mBindRecorderServiceTool = new RecorderServiceBindTool(context, userSelectedParams);
        }
        return mBindRecorderServiceTool;
    }

    private void startService() {
        this.mContext.startService(new Intent(this.mContext, RecorderService.class));
    }

    public void initService(RecorderServiceListener recorderServiceListener) {
        Log.d(TAG, "initService");
        if (recorderServiceListener == null) {
            return;
        }
        if (this.mRecorderService != null) {
            recorderServiceListener.service(this.mRecorderService);
        } else {
            this.mServiceListenerList.add(recorderServiceListener);
        }
    }

    public void release() {
        try {
            mBindRecorderServiceTool = null;
            this.mRecorderService.release();
            this.mContext.unbindService(this.mServiceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

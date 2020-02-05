package com.example.decoderencoder;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.decoderencoder.hdmi_in.IHdmiStateChange;
import com.example.decoderencoder.hdmi_in.RecorderClass;
import com.example.decoderencoder.hdmi_in.RecorderService;
import com.example.decoderencoder.hdmi_in.RecorderServiceBindTool;

import androidx.annotation.RequiresApi;

public class RecordActivity extends Activity {
    private static final String TAG = "RecordActivity";
    boolean exit = false;
    private UserSelectedParams userSelectedParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        userSelectedParams = getIntent().getExtras().getParcelable("user_parameters");
        setContentView(R.layout.activity_record);

        initService();
    }

    private RecorderServiceBindTool mRecorderServiceBindTool = null;
    private RecorderClass mRecorderClass;
    private IHdmiStateChange StateChange = new IHdmiStateChange() {
        private final int TOAST_DURATION = Toast.LENGTH_SHORT;

        @Override
        public void StartRecording() {
            //Toast.makeText(RecordActivity.this, "Start Recording", TOAST_DURATION).show();
        }

        @Override
        public void StopRecording() {
            //Toast.makeText(RecordActivity.this, "Stop Recording", TOAST_DURATION).show();
        }

        @Override
        public void StartPreview() {
            View HdmiNoSignaleView = findViewById(R.id.home_ac_video_hdmi_nosignale);
            if (HdmiNoSignaleView != null) {
                HdmiNoSignaleView.setVisibility(View.GONE);
            }
        }

        @Override
        public void StopPreview() {
            View HdmiNoSignaleView = findViewById(R.id.home_ac_video_hdmi_nosignale);
            if (HdmiNoSignaleView != null) {
                HdmiNoSignaleView.setVisibility(View.VISIBLE);
            }
        }
    };

    private void initService() {
        mRecorderServiceBindTool = RecorderServiceBindTool.getInstance(this, userSelectedParams);
        mRecorderServiceBindTool.initService(new RecorderServiceBindTool.RecorderServiceListener() {

            @Override
            public void service(RecorderService recorderService) {
                RecordActivity.this.mRecorderClass = recorderService.getRecorderInterface();
                ViewGroup rootView = (ViewGroup) findViewById(R.id.home_ac_video_hdmi_textureView);
                RecordActivity.this.mRecorderClass.SetRootView(rootView);
                RecordActivity.this.mRecorderClass.SetStateChange(StateChange);
                RecordActivity.this.mRecorderClass.startDisplay();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        try {
            getWindow().getDecorView().setSystemUiVisibility(5894);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mRecorderClass != null) {
            mRecorderClass.startDisplay();
        }
        super.onResume();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBackPressed()             // FIXME
    {
        if (mRecorderServiceBindTool != null) {
           // mRecorderServiceBindTool.release();
        }
        if (mRecorderClass != null) {
            mRecorderClass.stopRecord();
            mRecorderClass.onDestroy();
        }
        SharedPreferences preferences = getSharedPreferences("Transcoder", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("state", "").apply();
        editor.commit();

        exit = true;
        System.exit(0);


    }

}

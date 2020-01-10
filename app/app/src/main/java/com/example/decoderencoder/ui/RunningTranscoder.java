package com.example.decoderencoder.ui;


import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.decoderencoder.R;
import com.example.decoderencoder.library.util.Stats;

/**
 * A simple {@link Fragment} subclass.
 */
public class RunningTranscoder extends Fragment {

    Callback callback;
    Button button;
    TextView textView;
    public RunningTranscoder() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            callback = (RunningTranscoder.Callback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement UrisSelected");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_running_transcoder, container, false);
        textView = (TextView)view.findViewById(R.id.stats);
        button = (Button)view.findViewById(R.id.buttonStop);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.stopTranscoder();
            }
        });
        return view;
    }


    public void updateStats(Stats stats) {
        String s = "BufferCapacity: " + stats.bufferCapacity*100 + "\nNumber Of Encoded Frames: " + stats.numOfEncodedFrames + "\nNumber Of Decoded Frames: " + stats.numOfDecodedFrames + "\nTimeElapsed: " + stats.timeElapsed;
        textView.setText(s);
    }

    public interface Callback {
        public void stopTranscoder();
    }

}

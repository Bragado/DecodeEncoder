package com.example.decoderencoder.ui;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.decoderencoder.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class RunningTranscoder extends Fragment {

    UrisSelected mCallback;

    public RunningTranscoder() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (UrisSelected) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement UrisSelected");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_running_transcoder, container, false);
    }


    public interface UrisSelected {
        public void userSelectedUris(String input, String output);
    }

}

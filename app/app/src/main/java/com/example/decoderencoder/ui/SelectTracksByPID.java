package com.example.decoderencoder.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.decoderencoder.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class SelectTracksByPID extends Fragment {


    public SelectTracksByPID() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_select_tracks_by_pid, container, false);
    }

}

package com.example.decoderencoder.ui;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.decoderencoder.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class UrisInsertation extends Fragment {

    EditText inputUri;
    EditText outputUri;
    Callback callback;
    View view;
    Button button;

    public UrisInsertation() {
        // Required empty public constructor
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            this.callback = (Callback) context;
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_uris_insertation, container, false);
        button = (Button) view.findViewById(R.id.button_uri_continue);
        inputUri = (EditText)view.findViewById(R.id.input_uri);
        outputUri = (EditText)view.findViewById(R.id.output_uri);

        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String iuri = inputUri.getText().toString();
                String ouri = outputUri.getText().toString();
                if(!iuri.equals("") && !ouri.equals("")) {
                    callback.urisSelected(iuri, ouri);
                }
            }
        });

        return view;
    }

    public interface Callback {
        void urisSelected(String inputUri, String outputUri);
    }

}

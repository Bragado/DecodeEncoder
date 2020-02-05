package com.example.decoderencoder.ui;


import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.example.decoderencoder.R;
import com.example.decoderencoder.UserSelectedParams;

/**
 * A simple {@link Fragment} subclass.
 */
public class Home extends Fragment {

    Callback callback;

    Spinner video_codec, audio_codec, video_bitrate, video_size, input_type;
    ArrayAdapter<CharSequence> video_codec_adapter, audio_codec_adapter, video_bitrate_adapter, video_size_adapter, input_type_adapter;
    EditText iuri1, iuri2, iuri3, iuri4, iport, ouri1, ouri2, ouri3, ouri4, oport;
    Button startTranscoder;
    Context context;
    LinearLayout inputUris = null;


    public Home() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        configSpinners(view);

        configUris(view);

        startTranscoder = (Button)view.findViewById(R.id.startTranscoding);
        startTranscoder.setOnClickListener(new View.OnClickListener() {
                                               @Override
                                               public void onClick(View v) {
                                                   callback.submitAudioAndVideo(getParams());
                                               }
        });

        return view;
    }

    private void configUris(View view) {
        iuri1 = (EditText)view.findViewById(R.id.input_uri1);
        iuri2 = (EditText)view.findViewById(R.id.input_uri2);
        iuri3 = (EditText)view.findViewById(R.id.input_uri3);
        iuri4 = (EditText)view.findViewById(R.id.input_uri4);
        iport = (EditText)view.findViewById(R.id.input_uri5);

        ouri1 = (EditText)view.findViewById(R.id.output_uri1);
        ouri2 = (EditText)view.findViewById(R.id.output_uri2);
        ouri3 = (EditText)view.findViewById(R.id.output_uri3);
        ouri4 = (EditText)view.findViewById(R.id.output_uri4);
        oport = (EditText)view.findViewById(R.id.output_uri5);
        inputUris = (LinearLayout)view.findViewById(R.id.input_uris);


    }

    private void configSpinners(View view) {
        video_codec = (Spinner) view.findViewById(R.id.video_codec_spinner);
        audio_codec = (Spinner) view.findViewById(R.id.audio_codec_spinner);
        video_bitrate = (Spinner) view.findViewById(R.id.video_bitrate_spinner);
        video_size = (Spinner) view.findViewById(R.id.video_size_spinner);
        input_type = (Spinner) view.findViewById(R.id.input_type);

        video_codec_adapter = ArrayAdapter.createFromResource(context,
                R.array.video_codecs, android.R.layout.simple_spinner_item);
        audio_codec_adapter = ArrayAdapter.createFromResource(context,
                R.array.audio_codecs, android.R.layout.simple_spinner_item);
        video_bitrate_adapter = ArrayAdapter.createFromResource(context,
                R.array.video_bitrates, android.R.layout.simple_spinner_item);
        video_size_adapter = ArrayAdapter.createFromResource(context,
                R.array.video_res, android.R.layout.simple_spinner_item);
        input_type_adapter = ArrayAdapter.createFromResource(context,
                R.array.input_sources, android.R.layout.simple_spinner_item);


        video_codec_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        audio_codec_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        video_bitrate_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        video_size_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        input_type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        video_codec.setAdapter(video_codec_adapter);
        audio_codec.setAdapter(audio_codec_adapter);
        video_bitrate.setAdapter(video_bitrate_adapter);
        video_size.setAdapter(video_size_adapter);
        input_type.setAdapter(input_type_adapter);

        input_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(parent.getItemAtPosition(position).toString().equals("UDP")) {
                    inputUris.setVisibility(View.VISIBLE);
                }else {
                    inputUris.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        try {
            callback = (Home.Callback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement UrisSelected");
        }
    }

    public UserSelectedParams getParams() {

        UserSelectedParams userSelectedParams = new UserSelectedParams();

        String video_res_s;

        if(input_type.getSelectedItem().toString().equals("UDP")) {
            userSelectedParams.input_address =  iuri1.getText().toString() + "." +
                    iuri2.getText().toString() + "." + iuri3.getText().toString() +
                    "."+ iuri4.getText().toString();
            userSelectedParams.input_port = Integer.parseInt(iport.getText().toString());
        }else {
            userSelectedParams.input_port = 0;
        }
        userSelectedParams.output_address =  ouri1.getText().toString() + "." +
                ouri2.getText().toString() + "." + ouri3.getText().toString() +
                "."+ ouri4.getText().toString();
        userSelectedParams.output_port = Integer.parseInt(oport.getText().toString());
        userSelectedParams.video_codec = video_codec.getSelectedItem().toString();
        userSelectedParams.audio_codec = audio_codec.getSelectedItem().toString();
        userSelectedParams.video_bitrate = Integer.parseInt(video_bitrate.getSelectedItem().toString()) * 1024*1024;
        video_res_s = video_size.getSelectedItem().toString();

        switch (video_res_s) {
            case "1920x1080 30Hz":
            case "1920x1080 25Hz":
                userSelectedParams.width = 1920;
                userSelectedParams.height = 1080;
                userSelectedParams.frame_rate =  video_res_s.contains("25Hz") ? 25 : 30;
                break;
            case "1280x720 30Hz":
            case "1280x720 25Hz":
                userSelectedParams.width = 1280;
                userSelectedParams.height = 720;
                userSelectedParams.frame_rate =  video_res_s.contains("25Hz") ? 25 : 30;
                break;
            case "720x480 30Hz":
            case "720x480 25Hz":
                userSelectedParams.width = 720;
                userSelectedParams.height = 480;
                userSelectedParams.frame_rate =  video_res_s.contains("25Hz") ? 25 : 30;
                break;
        }
        return userSelectedParams;
    }

    /**
     * Used to submit the user selected options
     */
    public interface Callback {
        void submitAudioAndVideo(UserSelectedParams userSelectedParams);
    }


}

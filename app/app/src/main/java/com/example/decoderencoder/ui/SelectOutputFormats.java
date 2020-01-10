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
public class SelectOutputFormats extends Fragment {

    Callback callback;
    Button button_continue;
    EditText video_codec, audio_codec, video_bitrate, audio_bitrate, width, height, fps, sample_rate, channel_count;


    public SelectOutputFormats() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            this.callback = (SelectOutputFormats.Callback) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_select_output_formats, container, false);
        width = (EditText)view.findViewById(R.id.width);
        video_codec = (EditText)view.findViewById(R.id.codec_video);
        audio_codec = (EditText)view.findViewById(R.id.codec_audio);
        video_bitrate = (EditText)view.findViewById(R.id.bitrate);
        audio_bitrate = (EditText)view.findViewById(R.id.bitrate_audio);

        height = (EditText)view.findViewById(R.id.height);
        fps = (EditText)view.findViewById(R.id.fps);
        sample_rate = (EditText)view.findViewById(R.id.sampleRate);
        channel_count = (EditText)view.findViewById(R.id.channel_count);
        button_continue = (Button)view.findViewById(R.id.continue_transcoding);

        button_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String vcodec = video_codec.getText().toString();
                String vbitrate = video_bitrate.getText().toString();
                String w = width.getText().toString();
                String h = height.getText().toString();
                String f = fps.getText().toString();

                if(!vcodec.equals("") && !vbitrate.equals("") && !w.equals("") && !h.equals("") && !f.equals("")) {
                    callback.addVideoTrack(vcodec, Integer.parseInt(w), Integer.parseInt(h),  Integer.parseInt(vbitrate), Integer.parseInt(f));
                }

                String acodec = audio_codec.getText().toString();
                String abitrate = audio_bitrate.getText().toString();
                String srate = sample_rate.getText().toString();
                String cc = channel_count.getText().toString();
                if(!acodec.equals("") && !abitrate.equals("") && !srate.equals("") && !cc.equals("")) {
                    callback.addAudioTrack(acodec, Integer.parseInt(srate), Integer.parseInt(cc), Integer.parseInt(abitrate));
                }else {
                    callback.addAudioTrack(null, 0, 0, 0);
                }

            }
        });


        return view;
    }

    public interface Callback  {
        void addVideoTrack(String codec_name, int width, int height, int bitrate, int fps);
        void addAudioTrack(String codec_name, int sample_rate, int channel_count, int bitrate);
    }
}

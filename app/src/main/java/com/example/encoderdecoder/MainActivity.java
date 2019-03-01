package com.example.encoderdecoder;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  {

    public final String TAG = "ACTIVITY MAIN";
    Context This;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button decoderEncoder = findViewById(R.id.button3);
        final Intent intent = new Intent(this, DecoderEncoderActivity.class);
        final ListView listView = (ListView) findViewById(R.id.list);

        This = this;

        decoderEncoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(intent);
            }
        });


        Button listCodecs = findViewById(R.id.button2);

        listCodecs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int numCodecs = MediaCodecList.getCodecCount();
                ArrayList<String> codecs = new ArrayList<>();

                for (int i = 0; i < numCodecs; i++) {
                    MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);


                    String name =  "" ;


                    if (codecInfo.isEncoder()) {
                        name = "encoder : " + codecInfo.getName();
                    }else
                        name = "decoder : "  + codecInfo.getName();

                    codecs.add(name);
                }

                String[] codecsArr = new String[codecs.size()];
                codecsArr = codecs.toArray(codecsArr);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(This,
                        android.R.layout.simple_list_item_1, android.R.id.text1, codecsArr);

                listView.setAdapter(adapter);
            }
        });




    }


}

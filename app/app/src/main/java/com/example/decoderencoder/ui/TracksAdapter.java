package com.example.decoderencoder.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.decoderencoder.R;

import androidx.annotation.NonNull;

public class TracksAdapter extends  RecyclerView.Adapter<TracksAdapter.TrackViewHolder>{
    Context context;
    String[] tracks;

    public TracksAdapter(Context context, String[] tracks) {

        this.context = context;
        this.tracks = tracks;
    }


    @NonNull
    @Override
    public TracksAdapter.TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_track_item,
                parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TrackViewHolder holder, int position) {
        holder.track_name.setText(tracks[position]);
    }

    @Override
    public int getItemCount() {
        return tracks.length;
    }


    class TrackViewHolder extends  RecyclerView.ViewHolder {

        TextView track_name ;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            track_name = (TextView) itemView.findViewById(R.id.textView);
        }
    }


}
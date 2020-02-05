package com.example.decoderencoder;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;


public class UserSelectedParams implements Parcelable {

    /* Input & Output */
    public String input_address;
    public int input_port;
    public String output_address;
    public int output_port;

    /* Audio Parameters */
    public String audio_codec;

    /* Video Parameters */
    public String video_codec;
    public int video_bitrate;
    public int width;
    public int height;
    public int frame_rate;

    public static final Creator<UserSelectedParams> CREATOR = new Creator<UserSelectedParams>() {
        @Override
        public UserSelectedParams createFromParcel(Parcel in) {
            return new UserSelectedParams(in);
        }

        @Override
        public UserSelectedParams[] newArray(int size) {
            return new UserSelectedParams[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(input_address);
        dest.writeInt(input_port);
        dest.writeString(output_address);
        dest.writeInt(output_port);

        dest.writeString(audio_codec);
        dest.writeString(video_codec);

        dest.writeInt(video_bitrate);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeInt(frame_rate);
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("input_address", input_address);
            if(input_address == null)
                jsonObject.put("input_address", "no-op");
            jsonObject.put("input_port", input_port);
            jsonObject.put("output_address", output_address);
            jsonObject.put("output_port", output_port);
            jsonObject.put("audio_codec", audio_codec);
            jsonObject.put("video_codec", video_codec);
            jsonObject.put("video_bitrate", video_bitrate);
            jsonObject.put("width", width);
            jsonObject.put("height", height);
            jsonObject.put("frame_rate", frame_rate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public UserSelectedParams (JSONObject info) throws JSONException {
        input_address = info.getString("input_address");
        input_port = info.getInt("input_port");
        output_address = info.getString("output_address");
        output_port = info.getInt("output_port");
        audio_codec = info.getString("audio_codec");
        video_codec = info.getString("video_codec");

        video_bitrate = info.getInt("video_bitrate");
        width = info.getInt("width");
        height = info.getInt("height");
        frame_rate = info.getInt("frame_rate");

    }

    public UserSelectedParams(Parcel in) {
        input_address = in.readString();
        input_port = in.readInt();
        output_address = in.readString();
        output_port = in.readInt();

        audio_codec = in.readString();
        video_codec = in.readString();

        video_bitrate = in.readInt();
        width = in.readInt();
        height = in.readInt();
        frame_rate = in.readInt();
    }

    public UserSelectedParams() {

    }
}
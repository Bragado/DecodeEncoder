package com.example.decoderencoder.library.audio;

import android.media.MediaFormat;
import android.os.Build;

import com.example.decoderencoder.library.source.Media;

import androidx.annotation.RequiresApi;

// refer to https://wiki.multimedia.cx/index.php?title=ADTS
public class AacUtil {
    public static final int ADTS_LENGTH = 7;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static void addADTStoPacket(byte[] packet, int packetLen, MediaFormat mediaFormat) {
        int profile = getProfileNo(mediaFormat);
        int freqIdx = getFreqIndex(mediaFormat);
        int chanCfg = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);        // Channel Configuration = channelCount
        // fill in ADTS data
        packet[0] = (byte)0xFF;	// 11111111  	= syncword
        packet[1] = (byte)0xF9;	// 1111 1 00 1  = syncword MPEG-2 Layer CRC
        packet[2] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
        packet[3] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));
        packet[4] = (byte)((packetLen&0x7FF) >> 3);
        packet[5] = (byte)(((packetLen&7)<<5) + 0x1F);
        packet[6] = (byte)0xFC;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private static int getFreqIndex(MediaFormat mediaFormat) {
        int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        switch(sampleRate) {
            case 96000:
                return 0;
            case 88200:
                return 1;
            case 64000:
                return 2;
            case 48000:
                return 3;
            case 44100:
                return 4;
            case 32000:
                return 5;
            case 24000:
                return 6;
            case 22050:
                return 7;
            case 16000:
                return 8;
            case 12000:
                return 9;
            case 11025:
                return 10;
            case 8000:
                return 11;
            case 7350:
                return 12;
            default:
                return -1;

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static int getProfileNo(MediaFormat mediaFormat) {
        return mediaFormat.getInteger(MediaFormat.KEY_AAC_PROFILE);
    }

}

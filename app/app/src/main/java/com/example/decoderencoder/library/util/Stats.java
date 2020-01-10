package com.example.decoderencoder.library.util;

public class Stats {

    public float bufferCapacity = 0.0f;
    public long numOfEncodedFrames = 0;
    public long numOfDecodedFrames = 0;
    public long timeElapsed = 0;

    public Stats(float bufferCapacity, long numOfEncodedFrames, long numOfDecodedFrames, long timeElapsed ) {
        this.bufferCapacity = bufferCapacity;
        this.numOfEncodedFrames = numOfEncodedFrames;
        this.numOfDecodedFrames = numOfDecodedFrames;
        this.timeElapsed = timeElapsed;
    }


}

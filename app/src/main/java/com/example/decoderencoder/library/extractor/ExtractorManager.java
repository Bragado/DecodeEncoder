package com.example.decoderencoder.library.extractor;

public class ExtractorManager {

    static enum MediaType {MP4, UDP_MPEGTS, DASH, HLS };

    /**
     * Returns the best Extractor for the media type
     * @param mediaType media path
     * @return
     */
    public static Extractor getExtractor(String mediaType) {
        return null;
    }

    /**
     * Evaluates a given media's format
     * @param media
     * @return
     */
    private static MediaType evaluateMedia(String media) {
        return null;
    }

}

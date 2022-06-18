package de.vkoop.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class Response {
    @JsonProperty("translations")
    public List<Translation> translations;

    public static class Translation {
        @JsonProperty("detected_source_language")
        public String detectedSourceLanguage;
        @JsonProperty("text")
        public String text;
    }
}

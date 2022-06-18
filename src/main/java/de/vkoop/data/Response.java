package de.vkoop.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class Response {
    @JsonProperty("translations")
    public List<Translation> translations;

    @RegisterForReflection
    public static class Translation {
        @JsonProperty("detected_source_language")
        public String detectedSourceLanguage;
        @JsonProperty("text")
        public String text;
    }
}

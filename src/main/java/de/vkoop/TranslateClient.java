package de.vkoop;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class TranslateClient {

    private final String authKey;
    private final ObjectMapper objectMapper;

    public TranslateClient(String authKey) {
        this.authKey = authKey;
        this.objectMapper = new ObjectMapper();
    }

    public Response translate(String text, SourceLanguages sourceLanguage, TargetLanguages targetLanguage) throws URISyntaxException, IOException, InterruptedException {
        var client = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder()
                                .uri(new URI(String.format("https://api-free.deepl.com/v2/translate?" +
                                        "auth_key=%s&text=%s" +
                                        "&target_lang=%s" +
                                        "&source_lang=%s", authKey, URLEncoder.encode(text, StandardCharsets.UTF_8), targetLanguage, sourceLanguage)))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

        final String responseBody = client.body();

        return objectMapper.readValue(responseBody, Response.class);
    }

    public static class Response {
        @JsonProperty("translations")
        public List<Translation> translations;

        public static class Translation {
            @JsonProperty("detected_source_language")
            public String detectedSourceLanguage;
            @JsonProperty("text")
            public String text;
        }
    }

    public enum SourceLanguages {
        BG,
        CS,
        DA,
        DE,
        EL,
        EN,
        ES,
        ET,
        FI,
        FR,
        HU,
        IT,
        JA,
        LT,
        LV,
        NL,
        PL,
        PT,
        RO,
        RU,
        SK,
        SL,
        SV,
        ZH
    }

    public enum TargetLanguages {
        BG,
        CS,
        DA,
        DE,
        EL,
        EN_GB("EN-GB"),
        EN_US("EN-US"),
        EN,
        ES,
        ET,
        FI,
        FR,
        HU,
        IT,
        JA,
        LT,
        LV,
        NL,
        PL,
        PT_PT("PT-PT"),
        PT_BR("PT-BR"),
        PT,
        RO,
        RU,
        SK,
        SL,
        SV,
        ZH;

        private String value;

        TargetLanguages() {

        }

        TargetLanguages(String s) {
            this.value = s;
        }

        @Override
        public String toString() {
            return Objects.requireNonNullElseGet(this.value, this::name);
        }
    }

}

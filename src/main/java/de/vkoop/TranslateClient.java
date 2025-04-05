package de.vkoop;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vkoop.data.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class TranslateClient {

    public static final Set<String> SUPPORTED_SOURCE_LANGUAGES = Set.of(
            "AR",
            "BG",
            "CS",
            "DA",
            "DE",
            "EL",
            "EN",
            "ES",
            "ET",
            "FI",
            "FR",
            "HU",
            "ID",
            "IT",
            "JA",
            "KO",
            "LT",
            "LV",
            "NB",
            "NL",
            "PL",
            "PT",
            "RO",
            "RU",
            "SK",
            "SL",
            "SV",
            "TR",
            "UK",
            "ZH"
    );
    public static final Set<String> SUPPORTED_TARGET_LANGUAGES = Set.of(
            "AR",
            "BG",
            "CS",
            "DA",
            "DE",
            "EL",
            "EN",
            "EN-GB",
            "EN-US",
            "ES",
            "ET",
            "FI",
            "FR",
            "HU",
            "ID",
            "IT",
            "JA",
            "KO",
            "LT",
            "LV",
            "NB",
            "NL",
            "PL",
            "PT",
            "PT-BR",
            "PT-PT",
            "RO",
            "RU",
            "SK",
            "SL",
            "SV",
            "TR",
            "UK",
            "ZH",
            "ZH-HANS",
            "ZH-HANT"
    );


    private final String authKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient;


    public TranslateClient(String authKey) {
        this.authKey = authKey;
    }

    public Response translate(String text, String sourceLanguage, String targetLanguage) {
        try {

            final URI uri = new URI(String.format("https://api-free.deepl.com/v2/translate?" +
                    "auth_key=%s&text=%s" +
                    "&target_lang=%s" +
                    "&source_lang=%s", authKey, URLEncoder.encode(text, StandardCharsets.UTF_8), targetLanguage, sourceLanguage));

            var client = getHttpClient()
                    .send(
                            HttpRequest.newBuilder()
                                    .uri(uri)
                                    .header("Accept", "application/json")
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            var responseBody = client.body();

            return objectMapper.readValue(responseBody, Response.class);
        } catch (InterruptedException | URISyntaxException | IOException e) {
            System.out.println(e);
            return null;
        }
    }

    protected HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = createHttpClient();
        }
        return httpClient;
    }

    protected HttpClient createHttpClient() {
        return HttpClient.newHttpClient();
    }
}

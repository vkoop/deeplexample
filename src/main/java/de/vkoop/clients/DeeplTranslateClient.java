package de.vkoop.clients;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vkoop.data.Response;
import de.vkoop.interfaces.TranslateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class DeeplTranslateClient implements TranslateClient {
    private static final Logger logger = LoggerFactory.getLogger(DeeplTranslateClient.class);

    private static final Set<String> SUPPORTED_SOURCE_LANGUAGES = new HashSet<>(
            Set.of("AR",
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
            "ZH")
    );
    
    private static final Set<String> SUPPORTED_TARGET_LANGUAGES = new HashSet<>(
            Set.of("AR",
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
            "ZH-HANT")
    );


    private String authKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient;

    public DeeplTranslateClient() {
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public Response translate(String text, String sourceLanguage, String targetLanguage) {
        try {

            final URI uri = new URI(String.format("https://api-free.deepl.com/v2/translate?" +
                    "auth_key=%s&text=%s" +
                    "&target_lang=%s" +
                    "&source_lang=%s",
                    URLEncoder.encode(authKey, StandardCharsets.UTF_8),
                    URLEncoder.encode(text, StandardCharsets.UTF_8),
                    URLEncoder.encode(targetLanguage, StandardCharsets.UTF_8),
                    URLEncoder.encode(sourceLanguage, StandardCharsets.UTF_8)));

            var client = getHttpClient()
                    .send(
                            HttpRequest.newBuilder()
                                    .uri(uri)
                                    .header("Accept", "application/json")
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            var responseBody = client.body();

            int statusCode = client.statusCode();
            if (statusCode != 200) {
                logger.error("DeepL API returned error code {}: {}", statusCode, responseBody);
                // Try to extract error message from response if possible
                try {
                    var errorNode = objectMapper.readTree(responseBody);
                    if (errorNode.has("message")) {
                        var errorMessage = errorNode.get("message").asText();
                        logger.error("Error message: {}", errorMessage);
                    }
                } catch (Exception e) {
                    logger.error("Could not parse error response", e);
                }
                return null;
            }

            return objectMapper.readValue(responseBody, Response.class);
        } catch (InterruptedException | URISyntaxException | IOException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("Exception during translation", e);
            return null;
        }
    }
    
    @Override
    public Set<String> getSupportedSourceLanguages() {
        return Collections.unmodifiableSet(SUPPORTED_SOURCE_LANGUAGES);
    }
    
    @Override
    public Set<String> getSupportedTargetLanguages() {
        return Collections.unmodifiableSet(SUPPORTED_TARGET_LANGUAGES);
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

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }
}

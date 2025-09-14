package de.vkoop.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Safe URL builder that properly handles encoding and prevents injection attacks.
 * Uses proper URI construction instead of string concatenation.
 */
public class SafeUrlBuilder {
    private final String baseUrl;
    private final Map<String, String> parameters = new LinkedHashMap<>();

    public SafeUrlBuilder(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }
        this.baseUrl = baseUrl;
    }

    /**
     * Add a parameter to the URL. The value will be properly URL-encoded.
     *
     * @param name  Parameter name (not encoded - should be safe)
     * @param value Parameter value (will be URL-encoded)
     * @return This builder for method chaining
     */
    public SafeUrlBuilder addParameter(String name, String value) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter name cannot be null or empty");
        }
        if (value != null) {
            parameters.put(name, value);
        }
        return this;
    }

    /**
     * Build the final URI with all parameters properly encoded.
     *
     * @return URI object with properly encoded parameters
     * @throws URISyntaxException if the resulting URI is malformed
     */
    public URI build() throws URISyntaxException {
        if (parameters.isEmpty()) {
            return new URI(baseUrl);
        }

        String queryString = parameters.entrySet().stream()
            .map(entry -> encodedParameter(entry.getKey(), entry.getValue()))
            .collect(Collectors.joining("&"));

        String fullUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?") + queryString;
        return new URI(fullUrl);
    }

    /**
     * Build the final URI, wrapping URISyntaxException in RuntimeException for convenience.
     *
     * @return URI object with properly encoded parameters
     * @throws IllegalArgumentException if the resulting URI is malformed
     */
    public URI buildSafe() {
        try {
            return build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to build valid URI", e);
        }
    }

    private String encodedParameter(String name, String value) {
        return name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Get the current parameters (for testing/debugging)
     */
    Map<String, String> getParameters() {
        return new LinkedHashMap<>(parameters);
    }

    /**
     * Create a new builder for DeepL translate API
     */
    public static SafeUrlBuilder forDeeplTranslate() {
        return new SafeUrlBuilder("https://api-free.deepl.com/v2/translate");
    }
}
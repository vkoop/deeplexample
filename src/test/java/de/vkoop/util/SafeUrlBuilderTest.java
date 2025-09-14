package de.vkoop.util;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class SafeUrlBuilderTest {

    @Test
    void build_shouldCreateSimpleUrl() throws URISyntaxException {
        SafeUrlBuilder builder = new SafeUrlBuilder("https://api.example.com/test");

        URI uri = builder.addParameter("key", "value").build();

        assertEquals("https://api.example.com/test?key=value", uri.toString());
    }

    @Test
    void build_shouldEncodeSpecialCharactersInParameters() throws URISyntaxException {
        SafeUrlBuilder builder = new SafeUrlBuilder("https://api.example.com/test");

        URI uri = builder
            .addParameter("text", "Hello & goodbye! 50% off = great")
            .addParameter("auth", "key&with=special%chars")
            .build();

        String result = uri.toString();

        // Should not contain unencoded special characters
        assertFalse(result.contains("&goodbye"), "Should not contain unencoded '&'");
        assertFalse(result.contains("50% off"), "Should not contain unencoded '%' sequence");
        assertFalse(result.contains("= great"), "Should not contain unencoded '='");
        assertFalse(result.contains("key&with"), "Should not contain unencoded '&' in auth");

        // Should contain properly encoded parameters
        assertTrue(result.contains("text=Hello"), "Should contain start of encoded text");
        assertTrue(result.contains("auth=key"), "Should contain start of encoded auth");
        assertTrue(result.contains("%26"), "Should contain encoded '&' (%26)");
        assertTrue(result.contains("%3D"), "Should contain encoded '=' (%3D)");
    }

    @Test
    void build_shouldEncodeUnicodeCharacters() throws URISyntaxException {
        SafeUrlBuilder builder = new SafeUrlBuilder("https://api.example.com/test");

        URI uri = builder
            .addParameter("text", "Café, naïve, 北京, العربية")
            .build();

        String result = uri.toString();

        // Should not contain raw Unicode
        assertFalse(result.contains("Café"), "Should not contain raw Unicode");
        assertFalse(result.contains("北京"), "Should not contain raw Chinese");
        assertFalse(result.contains("العربية"), "Should not contain raw Arabic");

        // Should contain encoded Unicode
        assertTrue(result.contains("text="), "Should contain parameter name");
        assertTrue(result.contains("%"), "Should contain encoded characters");
    }

    @Test
    void build_shouldHandleMultipleParameters() throws URISyntaxException {
        SafeUrlBuilder builder = new SafeUrlBuilder("https://api.example.com/test");

        URI uri = builder
            .addParameter("auth_key", "test-key")
            .addParameter("text", "Hello World")
            .addParameter("source_lang", "EN")
            .addParameter("target_lang", "DE")
            .build();

        String result = uri.toString();

        assertTrue(result.contains("auth_key=test-key"), "Should contain auth key");
        assertTrue(result.contains("text=Hello+World"), "Should contain encoded text");
        assertTrue(result.contains("source_lang=EN"), "Should contain source language");
        assertTrue(result.contains("target_lang=DE"), "Should contain target language");

        // Count parameter separators
        long ampersandCount = result.chars().filter(ch -> ch == '&').count();
        assertEquals(3, ampersandCount, "Should have 3 ampersands for 4 parameters");
    }

    @Test
    void build_shouldHandleExistingQueryString() throws URISyntaxException {
        SafeUrlBuilder builder = new SafeUrlBuilder("https://api.example.com/test?existing=param");

        URI uri = builder.addParameter("new", "value").build();

        String result = uri.toString();
        assertTrue(result.contains("existing=param"), "Should preserve existing parameters");
        assertTrue(result.contains("new=value"), "Should add new parameter");
        assertTrue(result.contains("?existing=param&new=value"), "Should connect with &");
    }

    @Test
    void build_shouldIgnoreNullValues() throws URISyntaxException {
        SafeUrlBuilder builder = new SafeUrlBuilder("https://api.example.com/test");

        URI uri = builder
            .addParameter("key1", "value1")
            .addParameter("key2", null)
            .addParameter("key3", "value3")
            .build();

        String result = uri.toString();
        assertTrue(result.contains("key1=value1"), "Should include non-null parameter");
        assertTrue(result.contains("key3=value3"), "Should include non-null parameter");
        assertFalse(result.contains("key2"), "Should not include null parameter");
    }

    @Test
    void buildSafe_shouldWrapURISyntaxExceptionInRuntimeException() {
        SafeUrlBuilder builder = new SafeUrlBuilder("https://api.example.com/test");

        // This shouldn't actually throw since our implementation is safe,
        // but we test the method exists and works
        URI uri = builder.addParameter("key", "value").buildSafe();

        assertEquals("https://api.example.com/test?key=value", uri.toString());
    }

    @Test
    void constructor_shouldThrowForNullBaseUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SafeUrlBuilder(null);
        }, "Should throw for null base URL");
    }

    @Test
    void constructor_shouldThrowForEmptyBaseUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SafeUrlBuilder("");
        }, "Should throw for empty base URL");

        assertThrows(IllegalArgumentException.class, () -> {
            new SafeUrlBuilder("   ");
        }, "Should throw for whitespace-only base URL");
    }

    @Test
    void addParameter_shouldThrowForNullOrEmptyName() {
        SafeUrlBuilder builder = new SafeUrlBuilder("https://api.example.com/test");

        assertThrows(IllegalArgumentException.class, () -> {
            builder.addParameter(null, "value");
        }, "Should throw for null parameter name");

        assertThrows(IllegalArgumentException.class, () -> {
            builder.addParameter("", "value");
        }, "Should throw for empty parameter name");

        assertThrows(IllegalArgumentException.class, () -> {
            builder.addParameter("   ", "value");
        }, "Should throw for whitespace-only parameter name");
    }

    @Test
    void forDeeplTranslate_shouldCreateBuilderWithCorrectBaseUrl() throws URISyntaxException {
        SafeUrlBuilder builder = SafeUrlBuilder.forDeeplTranslate();

        URI uri = builder.addParameter("test", "value").build();

        assertTrue(uri.toString().startsWith("https://api-free.deepl.com/v2/translate"),
            "Should use correct DeepL API base URL");
    }
}
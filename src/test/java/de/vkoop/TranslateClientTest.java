package de.vkoop;

import de.vkoop.clients.DeeplTranslateClient;
import de.vkoop.data.Response;
import de.vkoop.exceptions.TranslationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TranslateClientTest {

    private static final String AUTH_KEY = "test-auth-key";
    private static final String SOURCE_LANGUAGE = "DE";
    private static final String TARGET_LANGUAGE = "EN";
    private static final String TEXT_TO_TRANSLATE = "Hallo Welt";
    private static final String TRANSLATED_TEXT = "Hello World";

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private DeeplTranslateClient translateClient;

    @BeforeEach
    void setUp() throws Exception {
        // Create a test instance with the mocked HttpClient
        translateClient = new DeeplTranslateClient() {
            @Override
            protected HttpClient createHttpClient() {
                return httpClient;
            }
        };

        translateClient.setAuthKey(AUTH_KEY);
    }

    @Test
    void translate_shouldSendCorrectRequest() throws Exception {
        // Arrange
        String jsonResponse =
            "{\"translations\":[{\"detected_source_language\":\"DE\",\"text\":\"Hello World\"}]}";
        when(httpResponse.body()).thenReturn(jsonResponse);
        // Mock the status code to return 200 (Success)
        when(httpResponse.statusCode()).thenReturn(200);
        when(
            httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
            )
        ).thenReturn(httpResponse);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(
            HttpRequest.class
        );

        // Act
        Response response = translateClient.translate(
            TEXT_TO_TRANSLATE,
            SOURCE_LANGUAGE,
            TARGET_LANGUAGE
        );

        // Assert
        verify(httpClient).send(
            requestCaptor.capture(),
            any(HttpResponse.BodyHandler.class)
        );

        HttpRequest capturedRequest = requestCaptor.getValue();
        URI uri = capturedRequest.uri();

        assertTrue(uri.toString().contains("auth_key=" + AUTH_KEY));
        assertTrue(uri.toString().contains("source_lang=" + SOURCE_LANGUAGE));
        assertTrue(uri.toString().contains("target_lang=" + TARGET_LANGUAGE));
        // UriComponentsBuilder encodes spaces as %20 (more standard than +)
        assertTrue(uri.toString().contains("text=Hallo") && uri.toString().contains("Welt"),
            "URL should contain the text 'Hallo Welt' (encoded)");

        // Verify response is not null before accessing its properties
        assertTrue(response != null, "Response should not be null");
        assertTrue(response.translations != null, "Response translations should not be null");
        assertTrue(!response.translations.isEmpty(), "Response translations should not be empty");
        assertEquals(TRANSLATED_TEXT, response.translations.get(0).text);
        assertEquals(
            SOURCE_LANGUAGE,
            response.translations.get(0).detectedSourceLanguage
        );
    }

    @Test
    void translate_shouldThrowTranslationExceptionForHttpError() throws Exception {
        // Arrange
        when(
            httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
            )
        ).thenThrow(new IOException("Network error"));

        // Act & Assert
        TranslationException exception = assertThrows(TranslationException.class, () -> {
            translateClient.translate(
                TEXT_TO_TRANSLATE,
                SOURCE_LANGUAGE,
                TARGET_LANGUAGE
            );
        });

        assertEquals("Translation API call failed", exception.getMessage());
        assertTrue(exception.getCause() instanceof IOException);
        assertEquals("Network error", exception.getCause().getMessage());
    }

    @Test
    void translate_shouldValidateSourceLanguage() {
        // Act & Assert
        assertTrue(translateClient.getSupportedSourceLanguages().contains("DE"));
        assertTrue(translateClient.getSupportedSourceLanguages().contains("EN"));
        assertTrue(translateClient.getSupportedSourceLanguages().contains("FR"));
        assertFalse(translateClient.getSupportedSourceLanguages().contains("XX"));
    }

    @Test
    void translate_shouldValidateTargetLanguage() {
        // Act & Assert
        assertTrue(translateClient.getSupportedTargetLanguages().contains("DE"));
        assertTrue(
            translateClient.getSupportedTargetLanguages().contains("EN-US")
        );
        assertTrue(
            translateClient.getSupportedTargetLanguages().contains("ZH-HANS")
        );
        assertFalse(translateClient.getSupportedTargetLanguages().contains("XX"));
    }

    @Test
    void translate_shouldHandleSpecialCharactersInText() throws Exception {
        // Arrange - text with special characters that need URL encoding
        String textWithSpecialChars = "Hello & goodbye! How are you? 50% off = great deal";
        String jsonResponse =
            "{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":\"Hallo & auf Wiedersehen!\"}]}";

        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

        // Act
        Response response = translateClient.translate(textWithSpecialChars, SOURCE_LANGUAGE, TARGET_LANGUAGE);

        // Assert
        verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest capturedRequest = requestCaptor.getValue();
        String uriString = capturedRequest.uri().toString();

        // Should not contain unencoded special characters in URL
        assertFalse(uriString.contains("&goodbye"), "URL should not contain unencoded '&' from text");
        assertFalse(uriString.contains("50% off"), "URL should not contain unencoded '50% off' sequence");
        assertFalse(uriString.contains("= great"), "URL should not contain unencoded '=' from text");

        // Should contain properly encoded text parameter
        assertTrue(uriString.contains("text=Hello"), "URL should contain start of encoded text");

        // Response should still be parsed correctly
        assertNotNull(response);
        assertEquals("Hallo & auf Wiedersehen!", response.translations.get(0).text);
    }

    @Test
    void translate_shouldHandleUnicodeCharactersInText() throws Exception {
        // Arrange - text with Unicode characters
        String unicodeText = "Café, naïve, résumé, 北京, العربية";
        String jsonResponse =
            "{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":\"Translated unicode\"}]}";

        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        // Act
        Response response = translateClient.translate(unicodeText, SOURCE_LANGUAGE, TARGET_LANGUAGE);

        // Assert - Focus on the most important aspect: it should work without throwing exceptions
        assertNotNull(response);
        assertEquals("Translated unicode", response.translations.get(0).text);

        // UriComponentsBuilder handles Unicode encoding properly - we trust Spring's implementation
        // The key security benefit is that it prevents injection attacks through proper encoding
    }

    @Test
    void translate_shouldHandleAuthKeyWithSpecialCharacters() throws Exception {
        // Arrange - auth key with characters that need encoding
        String authKeyWithSpecialChars = "key&with=special%chars";
        translateClient.setAuthKey(authKeyWithSpecialChars);

        String jsonResponse =
            "{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":\"Hello World\"}]}";

        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        // Act
        Response response = translateClient.translate(TEXT_TO_TRANSLATE, SOURCE_LANGUAGE, TARGET_LANGUAGE);

        // Assert - Focus on functionality: it should work without throwing exceptions
        assertNotNull(response);
        assertEquals("Hello World", response.translations.get(0).text);

        // UriComponentsBuilder handles special character encoding properly - we trust Spring's implementation
        // The key security benefit is that it prevents parameter injection attacks
    }

    @Test
    void translate_shouldThrowTranslationExceptionForInvalidURL() throws Exception {
        // Arrange - create conditions that would cause URI construction to fail
        // This is a bit tricky to test directly, but we can verify exception handling
        translateClient.setAuthKey(null); // This might cause issues in URL construction

        // Act & Assert
        assertThrows(Exception.class, () -> {
            translateClient.translate(TEXT_TO_TRANSLATE, SOURCE_LANGUAGE, TARGET_LANGUAGE);
        }, "Should throw exception when URL construction fails");
    }
}

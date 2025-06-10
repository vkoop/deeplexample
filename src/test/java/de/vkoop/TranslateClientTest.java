package de.vkoop;

import de.vkoop.data.Response;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertTrue(uri.toString().contains("text=Hallo+Welt"));

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
    void translate_shouldHandleHttpError() throws Exception {
        // Arrange
        when(
            httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
            )
        ).thenThrow(new IOException("Network error"));

        // Act
        Response response = translateClient.translate(
            TEXT_TO_TRANSLATE,
            SOURCE_LANGUAGE,
            TARGET_LANGUAGE
        );

        // Assert
        assertNull(response);
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
}

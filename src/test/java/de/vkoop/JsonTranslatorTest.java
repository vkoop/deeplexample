package de.vkoop;

import de.vkoop.data.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JsonTranslatorTest {

    private static final String SOURCE_LANGUAGE = "DE";
    private static final String TARGET_LANGUAGE = "EN";

    @Mock
    private TranslateClient translateClient;

    private JsonTranslator jsonTranslator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jsonTranslator = new JsonTranslator(translateClient);
    }

    @Test
    void parseAsMap_shouldParseJsonFile() throws IOException {
        // Arrange
        String jsonContent = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        Path jsonFile = createTempJsonFile(jsonContent);

        // Act
        Map<String, Object> result = jsonTranslator.parseAsMap(jsonFile.toString());

        // Assert
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    void translateJsonFile_shouldTranslateAllStringValues() throws IOException {
        // Arrange
        String jsonContent = "{\"key1\":\"value1\",\"nested\":{\"key2\":\"value2\"}}";
        Path jsonFile = createTempJsonFile(jsonContent);

        // Mock translate client responses
        Response response1 = createMockResponse("TRANSLATED_VALUE1");
        Response response2 = createMockResponse("TRANSLATED_VALUE2");

        when(translateClient.translate(eq("value1"), eq(SOURCE_LANGUAGE), eq(TARGET_LANGUAGE)))
                .thenReturn(response1);
        when(translateClient.translate(eq("value2"), eq(SOURCE_LANGUAGE), eq(TARGET_LANGUAGE)))
                .thenReturn(response2);

        // Act
        Map<String, Object> result = jsonTranslator.translateJsonFile(jsonFile.toString(), SOURCE_LANGUAGE, TARGET_LANGUAGE);

        // Assert
        assertEquals("TRANSLATED_VALUE1", result.get("key1"));
        Map<String, Object> nestedResult = (Map<String, Object>) result.get("nested");
        assertEquals("TRANSLATED_VALUE2", nestedResult.get("key2"));
    }

    @Test
    void translateJsonFile_shouldHandleEmptyResponse() throws IOException {
        // Arrange
        String jsonContent = "{\"key1\":\"value1\"}";
        Path jsonFile = createTempJsonFile(jsonContent);

        // Mock translate client to return null
        when(translateClient.translate(anyString(), eq(SOURCE_LANGUAGE), eq(TARGET_LANGUAGE)))
                .thenReturn(null);

        // Act
        Map<String, Object> result = jsonTranslator.translateJsonFile(jsonFile.toString(), SOURCE_LANGUAGE, TARGET_LANGUAGE);

        // Assert
        assertEquals("", result.get("key1"));
    }

    private Path createTempJsonFile(String content) throws IOException {
        Path filePath = tempDir.resolve("test.json");
        Files.writeString(filePath, content);
        return filePath;
    }

    private Response createMockResponse(String translatedText) {
        Response response = new Response();
        Response.Translation translation = new Response.Translation();
        translation.text = translatedText;
        response.translations = Collections.singletonList(translation);
        return response;
    }
} 
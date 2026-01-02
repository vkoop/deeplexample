package de.vkoop.clients;

import de.vkoop.data.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@Testcontainers
@Tag("integration")
class DeeplIntegrationTest {

    @Container
    public static MockServerContainer mockServer = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:5.15.0"));

    @Autowired
    private DeeplTranslateClient deeplClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("translate.client", () -> "deepl");
        registry.add("translate.deepl.url",
                () -> "http://" + mockServer.getHost() + ":" + mockServer.getServerPort() + "/v2/translate");
    }

    @Test
    void translate_shouldReturnTranslation_whenMockServerResponds() {
        // Arrange
        String text = "Hello";
        String sourceLang = "EN";
        String targetLang = "DE";
        String authKey = "fake-key";

        deeplClient.setAuthKey(authKey);

        try (var mockClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort())) {
            mockClient
                    .when(
                            request()
                                    .withMethod("GET")
                                    .withPath("/v2/translate")
                                    .withQueryStringParameter("auth_key", authKey)
                                    .withQueryStringParameter("text", text)
                                    .withQueryStringParameter("source_lang", sourceLang)
                                    .withQueryStringParameter("target_lang", targetLang))
                    .respond(
                            response()
                                    .withStatusCode(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(
                                            "{ \"translations\": [{ \"detected_source_language\": \"EN\", \"text\": \"Hallo\" }] }"));

            // Act
            Response response = deeplClient.translate(text, sourceLang, targetLang);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.translations.size());
            assertEquals("Hallo", response.translations.get(0).text);
            assertEquals("EN", response.translations.get(0).detectedSourceLanguage);
        }
    }
}

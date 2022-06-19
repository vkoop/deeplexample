package de.vkoop;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class JsonTranslator {
    ObjectMapper objectMapper = new ObjectMapper();

    private final TranslateClient translateClient;

    public JsonTranslator(TranslateClient translateClient) {
        this.translateClient = translateClient;
    }

    public Map<String, Object> parseAsMap(String filePath) throws IOException {
        return objectMapper.readValue(new File(filePath), Map.class);
    }

    public Map<String, Object> translateJsonFile(String filename, String sourceLang, String targetLang) throws IOException {
        final Map<String, Object> stringObjectMap = parseAsMap(filename);

        return MapUtils.map(stringObjectMap, value -> {
            var response =  translateClient.translate(value, sourceLang, targetLang);
            return response.translations.get(0).text;
        });
    }

}

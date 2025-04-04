package de.vkoop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Command(
        name = "json",
        description = "Translate JSON files using DeepL API"
)
class JsonCommand extends BaseCommand {
    @Option(names = "--json-file", required = true)
    String jsonFile;

    @Option(names = "--json-target-file")
    String jsonTargetFile;

    @Option(names = "--output-folder")
    Optional<String> outputFolder;
    
    // Package-private for testing
    JsonTranslator jsonTranslator;

    @Override
    public void run() {
        loadConfigFromFile();
        validateLanguages();

        if (jsonTranslator == null) {
            jsonTranslator = new JsonTranslator(getTranslateClient());
        }
        
        targetLanguages.stream().parallel()
                .forEach(targetLanguage -> translateSingleLanguage(jsonTranslator, targetLanguage));
    }

    private void translateSingleLanguage(JsonTranslator jsonParser, String targetLanguage) {
        try {
            final Map<String, Object> stringObjectMap = jsonParser.translateJsonFile(jsonFile, sourceLanguage, targetLanguage);
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

            final File resultFile = getFile(targetLanguage);
            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(resultFile, stringObjectMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getFile(String targetLanguage) throws IOException {
        final File resultFile;
        String targetLanguageLowerCase = targetLanguage.toLowerCase().replace('-', '_');
        if (outputFolder.isPresent() && jsonTargetFile == null) {
            final File parentFolder = new File(outputFolder.get());
            if (!parentFolder.exists()) {
                parentFolder.mkdirs();
            }
            resultFile = new File(parentFolder, targetLanguageLowerCase + ".json");
        } else {
            resultFile = new File(Objects.requireNonNullElseGet(jsonTargetFile, () -> targetLanguageLowerCase + ".json"));
        }
        resultFile.createNewFile();
        return resultFile;
    }
}

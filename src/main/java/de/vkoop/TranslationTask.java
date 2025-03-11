package de.vkoop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@Command(
    name = "translate",
    description = "Translate text or JSON files using DeepL API"
)
public class TranslationTask implements Runnable {

    @Option(names = "-c")
    File configurationFile;

    @Option(names = "-f", description = "load configuration from home. E.g. ~/.transcli.properties")
    boolean loadConfigFromHome;

    @Option(names = "-k")
    String authKey;

    @Option(names = "-s")
    String sourceLanguage;

    @Option(names = "-t", split = ",")
    List<String> targetLanguages;

    @Option(names = "--json-file")
    String jsonFile;

    @Option(names = "--json-target-file")
    String jsonTargetFile;

    @Option(names = "--text")
    Optional<String> text;

    @Option(names = "--output-folder")
    Optional<String> outputFolder;

    private TranslateClient translateClient;

    @Override
    public void run() {
        loadConfigFromFile();

        if (!TranslateClient.SUPPORTED_SOURCE_LANGUAGES.contains(sourceLanguage)) {
            System.err.println("Unsupported source language: " + sourceLanguage);
        }

        for (String targetLanguage : targetLanguages) {
            if (!TranslateClient.SUPPORTED_TARGET_LANGUAGES.contains(targetLanguage)) {
                System.err.println("Unsupported target language: " + targetLanguage);
                System.exit(1);
            }
        }

        if (jsonFile == null) {
            translateTextInput();
        } else {
            translateJsonFile();
        }
    }

    private void translateJsonFile() {
        JsonTranslator jsonParser = new JsonTranslator(getTranslateClient());

        targetLanguages.stream().parallel()
                .forEach(targetLanguage -> {
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
                });
    }

    private File getFile(String targetLanguage) throws IOException {
        final File resultFile;
        // if parent folder is defined create the file in the parentfolder
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

    private void translateTextInput() {
        var translatedCsvLine = targetLanguages.stream()
                .parallel()
                .map(targetLanguage -> getTranslateClient().translate(text.get(), sourceLanguage, targetLanguage))
                .filter(Objects::nonNull)
                .map(response -> response.translations)
                .map(translations -> translations.isEmpty() ? "" : translations.get(0).text)
                .map(translation -> "\"" + translation + "\"")
                .collect(Collectors.joining(";"));

        System.out.println(translatedCsvLine);
    }

    private void loadConfigFromFile() {
        if (authKey == null && configurationFile == null && !loadConfigFromHome) {
            System.err.println("Not authentication provided will exit.");
            System.exit(1);
        } else if (loadConfigFromHome || configurationFile != null) {
            Properties properties = new Properties();
            try (FileInputStream inStream = (loadConfigFromHome)
                    ? new FileInputStream(System.getProperty("user.home") + File.separator + ".transcli.properties")
                    : new FileInputStream(configurationFile)) {

                properties.load(inStream);
                Optional.ofNullable(properties.getProperty("authKey"))
                        .ifPresent(value -> this.authKey = value);
                Optional.ofNullable(properties.getProperty("sourceLanguage"))
                        .ifPresent(value -> this.sourceLanguage = value);
                Optional.ofNullable(properties.getProperty("targetLanguages"))
                        .map(value -> List.of(value.split(",")))
                        .ifPresent(value -> this.targetLanguages = value);
            } catch (IOException e) {
                System.err.println("Failed to load file: " + configurationFile);
                System.exit(1);
            }
        }
    }

    public TranslateClient getTranslateClient() {
        if (null == translateClient) {
            translateClient = new TranslateClient(authKey);
        }

        return translateClient;
    }

    public void setTranslateClient(TranslateClient translateClient) {
        this.translateClient = translateClient;
    }
}

package de.vkoop;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@Command
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

        if(jsonFile == null){
            var translatedCsvLine = targetLanguages.stream()
                    .parallel()
                    .map(targetLanguage -> getTranslateClient().translate(text.get(), sourceLanguage, targetLanguage))
                    .filter(Objects::nonNull)
                    .map(response -> response.translations)
                    .map(translations -> translations.isEmpty() ? "" : translations.get(0).text)
                    .map(translation -> "\"" + translation + "\"")
                    .collect(Collectors.joining(";"));

            System.out.println(translatedCsvLine);
        } else {
            JsonTranslator jsonParser = new JsonTranslator(getTranslateClient());

            targetLanguages.stream().parallel()
                    .forEach(targetLanguage -> {
                        try {
                            final Map<String, Object> stringObjectMap = jsonParser.translateJsonFile(jsonFile, sourceLanguage, targetLanguage);
                            final ObjectMapper objectMapper = new ObjectMapper();
                            final File resultFile = new File(jsonTargetFile + targetLanguage + ".json");
                            resultFile.createNewFile();
                            objectMapper.writeValue(resultFile, stringObjectMap);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

        }
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

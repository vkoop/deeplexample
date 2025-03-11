package de.vkoop;

import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

abstract class BaseCommand implements Runnable {
    @CommandLine.Option(names = "-c")
    File configurationFile;

    @CommandLine.Option(names = "-f", description = "load configuration from home. E.g. ~/.transcli.properties")
    boolean loadConfigFromHome;

    @CommandLine.Option(names = "-k")
    String authKey;

    @CommandLine.Option(names = "-s")
    String sourceLanguage;

    @CommandLine.Option(names = "-t", split = ",")
    List<String> targetLanguages;

    private TranslateClient translateClient;

    protected void validateLanguages() {
        if (!TranslateClient.SUPPORTED_SOURCE_LANGUAGES.contains(sourceLanguage)) {
            System.err.println("Unsupported source language: " + sourceLanguage);
            System.exit(1);
        }

        for (String targetLanguage : targetLanguages) {
            if (!TranslateClient.SUPPORTED_TARGET_LANGUAGES.contains(targetLanguage)) {
                System.err.println("Unsupported target language: " + targetLanguage);
                System.exit(1);
            }
        }
    }

    protected void loadConfigFromFile() {
        if (authKey == null && configurationFile == null && !loadConfigFromHome) {
            System.err.println("No authentication provided will exit.");
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

    protected TranslateClient getTranslateClient() {
        if (null == translateClient) {
            translateClient = new TranslateClient(authKey);
        }
        return translateClient;
    }

    public void setTranslateClient(TranslateClient translateClient) {
        this.translateClient = translateClient;
    }
}

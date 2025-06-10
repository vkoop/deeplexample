package de.vkoop;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Objects;
import java.util.stream.Collectors;

@Command(name = "text", description = "Translate text using DeepL API")
class TextCommand extends BaseCommand {

    @Option(names = "--text", required = true)
    String text;

    @Override
    public void run() {
        loadConfigFromFile();
        validateLanguages();

        var translatedCsvLine = targetLanguages
            .stream()
            .parallel()
            .map(targetLanguage ->
                getTranslateClient()
                    .translate(text, sourceLanguage, targetLanguage)
            )
            .filter(Objects::nonNull)
            .map(response -> response.translations)
            .map(translations ->
                translations.isEmpty() ? "" : translations.get(0).text
            )
            .map(translation -> "\"" + translation + "\"")
            .collect(Collectors.joining(";"));

        System.out.println(translatedCsvLine);
    }
}

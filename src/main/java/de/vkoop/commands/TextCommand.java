package de.vkoop.commands;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Command(name = "text", description = "Translate text using DeepL API")
public class TextCommand extends BaseCommand {

    @Option(names = "--text", required = true)
    public String text;

    @Override
    public void run() {
        loadConfigFromFile();
        validateLanguages();

        var translatedCsvLine = targetLanguages
                .stream()
                // .parallel()
                .map(targetLanguage ->
                        translateClient
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

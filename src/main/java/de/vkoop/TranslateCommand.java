package de.vkoop;

import picocli.CommandLine.Command;

@Command(
        name = "translate",
        description = "Translation commands using DeepL API",
        subcommands = {
                TextCommand.class,
                JsonCommand.class
        }
)
public class TranslateCommand {
}
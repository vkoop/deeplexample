package de.vkoop.commands;


import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(
    name = "translate",
    description = "Translation commands using DeepL API",
    subcommands = { TextCommand.class, JsonCommand.class }
)
public class TranslateCommand {}

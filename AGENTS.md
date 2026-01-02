# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot CLI application for translating text and JSON files using translation APIs (DeepL and Ollama). It uses Picocli for command-line interface and Spring Boot for dependency injection.

## Build & Run Commands

- **Build project**: `./gradlew build`
- **Run application**: `./gradlew bootRun --args="[command arguments]"`
- **Run tests**: `./gradlew test`
- **Create distribution**: `./gradlew distZip` (creates package in `build/distributions/`)
- **Show help**: `./gradlew bootRun --args="--help"`

## Architecture

### Core Components

- **AppStarter.java**: Main Spring Boot application entry point with Picocli integration
- **Command Structure**: Hierarchical command pattern with `TranslateCommand` as parent to `TextCommand` and `JsonCommand`
- **Translation Clients**: Strategy pattern with `TranslateClient` interface and implementations:
  - `DeeplTranslateClient` (DeepL API)
  - `OllamaTranslateClient` (Ollama local AI)
- **BaseCommand**: Shared functionality for configuration loading, authentication, and language validation

### Key Patterns

- **Spring Boot + Picocli Integration**: Uses `CommandLineRunner` and `IFactory` for dependency injection in commands
- **Configuration Management**: Supports config files (`-c`), home directory config (`-f ~/.transcli.properties`), and command-line options
- **Language Validation**: Each client validates supported source/target languages before translation
- **JSON Processing**: `JsonTranslator` preserves JSON structure while translating string values

### Dependencies

- Java 17 (configured via Gradle toolchain)
- Spring Boot 3.5.3 with Spring AI (Ollama integration)
- Picocli 4.7.7 for CLI
- Jackson for JSON processing
- JUnit 5 for testing

### Package Structure

- `de.vkoop.commands.*`: CLI command implementations
- `de.vkoop.clients.*`: Translation service implementations
- `de.vkoop.interfaces.*`: Abstract interfaces (TranslateClient)
- `de.vkoop.*`: Core utilities (JsonTranslator, MapUtils, ConfigGeneratorTask)

## Common Development Tasks

### Adding New Translation Client
1. Implement `TranslateClient` interface in `clients/` package
2. Add Spring `@Component` annotation
3. Configure client selection logic if needed

### Adding New Command
1. Extend `BaseCommand` for shared configuration functionality
2. Add to appropriate parent command's `subcommands` array
3. Use `@Component` for Spring integration

### Configuration Properties
- Version properties defined in `gradle.properties`
- Application properties in `application.properties`
- User config typically in `~/.transcli.properties`
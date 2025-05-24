# DeepL Translations CLI

A command-line tool for translating text and JSON files using the DeepL API.

## Overview

- Translate text strings to multiple target languages
- Translate JSON files while preserving structure
- Support for configuration files and interactive config generation
- Parallel processing for multiple languages

## Requirements

- Java 17 or higher
- DeepL API key (get one at [DeepL API](https://www.deepl.com/pro-api))

## Project Structure

- `src/main/java/de/vkoop/`: Core implementation
  - `AppStarter.java`: Application entry point
  - `TranslateClient.java`: DeepL API client
  - `JsonTranslator.java`: JSON processing utilities
  - Command classes for subcommands
- `src/test/`: Unit and integration tests
- `gradle/`: Build configuration

## Quick Start

1. Build the project: `./gradlew build`
2. Generate a config file: `./gradlew run --args="generate-config"`
3. Run a translation:
   ```
   ./gradlew run --args="translate text --text 'Hello world' -f"
   ```

For complete usage instructions:
```
./gradlew run --args="--help"
./gradlew run --args="translate --help"
```

## Distribution

Create a distributable package:
```
./gradlew distZip
```

The distribution will be created in `build/distributions/`.

## Supported Languages

**Source Languages:**
AR, BG, CS, DA, DE, EL, EN, ES, ET, FI, FR, HU, ID, IT, JA, KO, LT, LV, NB, NL, PL, PT, RO, RU, SK, SL, SV, TR, UK, ZH

**Target Languages:**
All source languages plus: EN-GB, EN-US, PT-BR, PT-PT, ZH-HANS, ZH-HANT

## Development

Run tests: `./gradlew test`
# DeepL Translations CLI

A command-line tool for translating text and JSON files using the DeepL API.

## Features

- Translate single text strings to multiple languages
- Translate entire JSON files while preserving structure
- Support for configuration files
- Parallel processing for multiple target languages

## Installation

### Prerequisites

- Java 23 or higher
- Gradle (optional, wrapper included)
- DeepL API key (get one at [DeepL API](https://www.deepl.com/pro-api))

### Building from Source

```bash
./gradlew build
```

## Usage

### Basic Text Translation

```bash
./gradlew run --args="-k YOUR_API_KEY -s DE -t EN,FR --text 'Hallo Welt'"
```

### JSON File Translation

```bash
./gradlew run --args="-k YOUR_API_KEY -s DE -t EN,FR --json-file input.json --output-folder translations"
```

### Configuration File

Create a `.transcli.properties` file in your home directory:

```properties
authKey=YOUR_API_KEY
sourceLanguage=DE
targetLanguages=EN,FR
```

Then run with:

```bash
./gradlew run --args="-f"
```

### Configuration File Generator

To generate a `.transcli.properties` file interactively, use the generate-config command:

```bash
# On Unix-like systems
./bin/deeplclient generate-config

# On Windows
.\bin\deeplclient.bat generate-config
```

The generator will:
1. Ask for your DeepL API key (required)
2. Ask for default source language (optional)
3. Ask for default target languages (optional)

Example interaction:
```
DeepL Translations CLI Configuration Generator
-------------------------------------------
This will create a configuration file at: /home/user/.transcli.properties
Press Enter to skip optional fields.

Enter your DeepL API key (required): your-api-key-here
Enter default source language (optional, e.g., DE): DE
Enter default target languages (optional, comma-separated, e.g., EN,FR): EN,FR

Configuration file created successfully at: /home/user/.transcli.properties

Content:
----------------------------------------
authKey=your-api-key-here
sourceLanguage=DE
targetLanguages=EN,FR
----------------------------------------

You can now use the CLI with the -f flag to use this configuration.
```

## Command Line Options

- `-k`: DeepL API key
- `-s`: Source language code
- `-t`: Comma-separated list of target language codes
- `--text`: Text to translate
- `--json-file`: Input JSON file path
- `--json-target-file`: Output JSON file path
- `--output-folder`: Output folder for translations
- `-f`: Load configuration from ~/transcli.properties
- `-c`: Specify custom configuration file path

## Supported Languages

### Source Languages
AR, BG, CS, DA, DE, EL, EN, ES, ET, FI, FR, HU, ID, IT, JA, KO, LT, LV, NB, NL, PL, PT, RO, RU, SK, SL, SV, TR, UK, ZH

### Target Languages
All source languages plus: EN-GB, EN-US, PT-BR, PT-PT, ZH-HANS, ZH-HANT

## Distribution

### Creating the Distribution

You can create distributable packages using Gradle:

```bash
# Create ZIP distribution
./gradlew distZip

# Create TAR distribution
./gradlew distTar
```

The distributions will be created in `build/distributions/` directory.

### Installing the Distribution

1. Download or create the distribution package
2. Extract the archive:
   ```bash
   # For ZIP
   unzip deeplclient-1.0-SNAPSHOT.zip
   
   # For TAR
   tar -xf deeplclient-1.0-SNAPSHOT.tar
   ```
3. Navigate to the extracted directory:
   ```bash
   cd deeplclient-1.0-SNAPSHOT
   ```

### Running from Distribution

After installation, you can run the application using the provided scripts in the `bin` directory:

```bash
# On Unix-like systems
./bin/deeplclient -k YOUR_API_KEY -s DE -t EN --text 'Hallo Welt'

# On Windows
.\bin\deeplclient.bat -k YOUR_API_KEY -s DE -t EN --text 'Hallo Welt'
```

### Directory Structure

The distribution package contains:
- `bin/` - Executable scripts (Unix and Windows)
- `lib/` - Required JAR files
- `README` and `LICENSE` files

## Development

### Project Structure

- `src/main/java/de/vkoop/`: Core implementation
- `src/test/`: Test cases
- `gradle/`: Gradle wrapper and configuration

### Running Tests

```bash
./gradlew test
```



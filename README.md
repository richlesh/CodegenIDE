# CodegenIDE

A lightweight desktop IDE for the [Codegen](https://glowingcat.com) programming language. Codegen is a language-agnostic source language that generates equivalent programs in C++, Java, JavaScript, Perl, Python, Rust and Swift.

## Features

### Code Editor
- Syntax highlighting for Codegen source files (`.src`)
- Find and replace with regex support
- Undo/redo with deep history (200 levels)
- Block indent/outdent (Cmd/Ctrl+[ and ])
- Tab-to-spaces conversion on file open
- Monospaced font selection and sizing

### Code Generation
- Generate target language source from Codegen: C++, Java, JavaScript, Perl, Python, Swift
- Generate all languages at once
- Automatic regeneration when running if source has changed

### Run & Test
- Compile and run generated code directly from the IDE
- Built-in terminal emulator with ANSI color support, scrollback, and text selection
- Command-line arguments field for passing args to programs
- Keyboard interrupt support (Ctrl+C)

### AI Assistant
- Integrated AI chat panel for coding assistance
- Multi-vendor LLM support: OpenAI, Anthropic, Google, DeepSeek, Alibaba, Ollama
- Context-aware: sends current source code and console output to the LLM
- Codegen grammar and built-in function reference included in system prompt
- Code suggestions with Accept/Reject workflow
- Styled message rendering (bold, italic, code blocks, headers)
- Configurable chat bubble colors and fonts

### Settings
- Customizable syntax highlighting colors (9 categories)
- Console foreground/background colors
- AI chat font and size (any installed font)
- LLM vendor, model, and API key configuration with live model list fetching
- User prompt and AI response bubble colors
- Window size and layout persisted between sessions

### Multi-Platform Packaging
- GitHub Actions workflow for automated native packaging
- macOS: signed and notarized DMG (ARM64)
- Windows: MSI installer with code signing (x64, ARM64)
- Linux: DEB and RPM packages (x64, ARM64)
- File association for `.src` files with custom document icon

### Other
- Multiple windows (File → New)
- Native look and feel on each platform
- macOS integration (system menu bar, About/Preferences/Quit handlers)
- Donation splash screen and optional license key system

## Requirements

- Java 17 or later
- Maven (for building)

## Building

```bash
mvn clean package
```

## Running

```bash
./run.sh
```

Or directly:

```bash
java -jar target/CodegenIDE-1.0.0.jar
```

The `lib/` directory contains runtime dependencies (Codegen compiler, Utils, ANTLR) that must be on the classpath. The default classpath can be overridden with `-Dcodegen.classpath=...`.

## License

GNU General Public License v3.0 — see [LICENSE](LICENSE) for details.

© 2026 Richard Lesh

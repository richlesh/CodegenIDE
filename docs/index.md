<link rel="stylesheet" href="styles.css">

# CodegenIDE User Guide

CodegenIDE is a lightweight desktop IDE for the Codegen programming language. This guide covers how to use the IDE's features effectively.

## Getting Started

### Launching the IDE

Run CodegenIDE using the provided script:

```bash
./run.sh
```

Or directly:

```bash
java -jar target/CodegenIDE-1.0.0.jar
```

On Linux, macOS or Windows, you can also install the packaged application and launch using it.

### Opening Files

There are several ways to open a Codegen source file (`.src`):

- **File → Open** (Cmd+O / Ctrl+O) — opens a file dialog filtered to `.src` files
- **Double-click** a `.src` file in your file manager — opens a new CodegenIDE window with the file loaded
- **Drag and drop** a `.src` file onto the editor pane — opens the file in the current window
- **Command-line argument** — pass a `.src` file path as the first argument when launching

When opening a file, any tab characters are automatically converted to spaces (4-space tab stops).

---

## The Editor

The main editor pane provides a full-featured code editor with:

- Syntax highlighting for Codegen source files
- Line numbers in the left gutter
- Monospaced font (configurable in Settings)
- Caret width of 2 pixels for visibility

### Editing

| Action | Shortcut |
|--------|----------|
| Undo | Cmd/Ctrl+Z |
| Redo | Cmd/Ctrl+Shift+Z |
| Cut | Cmd/Ctrl+X |
| Copy | Cmd/Ctrl+C |
| Paste | Cmd/Ctrl+V |
| Shift Selection Right | Cmd/Ctrl+] |
| Shift Selection Left | Cmd/Ctrl+[ |

The editor maintains 200 levels of undo history. Each text change is recorded as a full snapshot, so undo/redo always restores the complete editor state.

### Block Indent and Outdent

Select one or more lines and press Cmd/Ctrl+] to indent them by 4 spaces, or Cmd/Ctrl+[ to remove up to 4 leading spaces. This works on the entire selection.

---

## Find and Replace

Open with **Edit → Find...** (Cmd/Ctrl+F).

### Search Options

- **Case Sensitive** — match exact case
- **Entire Word** — only match whole words (adds word-boundary matching)
- **Grep** — treat the search pattern as a regular expression; replacement supports backreferences
- **Selected Text Only** — restrict search to the current selection
- **Wrap Around** — when reaching the end of the document, continue from the beginning (enabled by default)

### Actions

| Button | Action |
|--------|--------|
| Next | Find the next occurrence from the cursor position |
| Previous | Find the previous occurrence before the cursor |
| First | Jump to the first occurrence in the document |
| Replace | Replace the current selection with the replacement text |
| Replace All | Replace all occurrences |
| Replace and Find | Replace the current match and advance to the next |

Press Enter to trigger "Next" (the default action). The dialog is non-modal, so you can continue editing while it's open.

---

## Code Generation

CodegenIDE generates target language source code from your Codegen `.src` file. Generated files are placed in a `gen/` subdirectory next to your source file.

### Generating Code

Use the **Generate** menu to generate code for a specific language:

- C++ (`.cpp`)
- Java (`.java`)
- JavaScript (`.js`)
- Perl (`.pl`)
- Python (`.py`)
- Rust (`.rs`)
- Swift (`.swift`)

Select **Generate All** to generate all seven languages at once.

The IDE automatically saves your file before generating. Any required utility library files (e.g., `Utils.hpp`, `Utils.jar`) are copied into the `gen/` directory automatically.

### Preview Panel

The right side of the editor shows a read-only preview of the generated code for a selected language. Use the language dropdown at the top of the preview panel to switch between languages.

The preview panel features:
- Syntax highlighting appropriate for the selected language
- Line numbers
- Synchronized scrolling with the main editor (proportional vertical position)
- Automatic refresh after code generation

---

## Running Programs

Use the **Run** menu to compile and execute your program in any supported language.

### Supported Languages

| Language | Compiler/Runtime | Notes |
|----------|-----------------|-------|
| C++ | g++ (C++20) | Links fmt library |
| Java | javac + java | Assertions enabled (-ea), Utils.jar on classpath |
| JavaScript | Node.js | |
| Perl | perl | PERL5LIB=. set for module resolution |
| Python | python3 | |
| Rust | cargo | Creates a Cargo project structure |
| Swift | swiftc | DYLD_LIBRARY_PATH set for dynamic libraries |

### Auto-Regeneration

When you run a program, the IDE checks if your source file is newer than the generated file. If so, it automatically regenerates the code before compiling and running.

### Command-Line Arguments

Enter arguments in the **Args** field in the console toolbar. Arguments are split on whitespace and passed to your program.

### Stopping a Program

Use **Run → Stop** (Cmd/Ctrl+.) to terminate a running process. You can also press Ctrl+C in the console to send a keyboard interrupt.

---

## The Console

The built-in console displays program output and accepts input.

### Features

- Full ANSI color support (8 colors + bright variants)
- Text styling: bold, dim, italic, underline, strikethrough, reverse
- 10,000-line scrollback buffer
- Mouse text selection (click and drag)
- Emoji and wide character rendering
- Progress bar support (carriage return handling)

### Keyboard Input

When a program is waiting for input:
- Type characters (displayed in white)
- Press Enter to send the line to the program
- Press Backspace to delete characters
- Press Ctrl+D (Unix) or Ctrl+Z (Windows) to send end-of-file

### Copying from the Console

Select text with the mouse, then press Cmd/Ctrl+C.

---

## AI Assistant

The integrated AI assistant provides coding help with context awareness.

### Showing/Hiding the Panel

Toggle the AI panel via **Help → AI Assistant** (checkbox menu item). The panel appears on the right side of the window.

### Using the Chat

1. Type your question or request in the input area at the bottom
2. Press Enter to send (Shift+Enter for a newline)
3. The AI response appears in the chat area above

Press the **Clear** button to reset the conversation history.

### Context Awareness

The AI automatically receives:
- Your current source code from the editor
- The generated code shown in the preview panel (if any)
- Recent console output

This allows it to understand your program and provide relevant suggestions.

### Code Suggestions

When the AI suggests code changes, a collapsible code block appears with:
- **Show/Hide** toggle to expand or collapse the code
- **Allow** button — applies the suggested code to your editor
- **Reject** button — discards the suggestion

### Canceling a Request

While the AI is "thinking" (pulsing icon animation), click the **✕** button to cancel the request.

### Status Bar

The bottom of the AI panel shows character counts for the context being sent:
- LLM System Prompt size
- Current Program size
- Preview Program size
- Current Output size

### Supported LLM Vendors

Configure your preferred AI vendor and model in Settings:

| Vendor | Models | Notes |
|--------|--------|-------|
| OpenAI | GPT-4o, GPT-4, etc. | Default vendor |
| Anthropic | Claude family | Uses Anthropic Messages API |
| Google | Gemini family | |
| DeepSeek | DeepSeek models | |
| Alibaba | Qwen models | |
| Ollama | Local models | Runs on localhost:11434, no API key needed |

---

## Settings

Open Settings via:

- **CodegenIDE → Settings**

### Fonts

| Setting | Description |
|---------|-------------|
| Code Font | Monospaced font for the code editor, code preview and console (only monospaced fonts are listed) |
| Code Size | Font size for the code editor, code preview and console |
| AI Font | Font for the AI chat panel (any installed font) |
| AI Size | Font size for the AI chat panel |

### LLM Settings

| Setting | Description |
|---------|-------------|
| Vendor | AI service provider |
| Model | Specific model (fetched live from vendor API) |
| API Key | Your API key (click "Get API key..." for the vendor's key page) |
| Endpoint URL | The required endpoint URL (Generic vendor only) |

The model list updates automatically when you change vendor or API key.

### Syntax Colors

Nine configurable color categories for syntax highlighting:

| Category | What It Highlights |
|----------|-------------------|
| Normal | Regular identifiers and text |
| Keywords | Language keywords |
| Directives | Preprocessor/compiler directives |
| Comments | Single-line and multi-line comments |
| Strings | String and character literals |
| Numbers | Numeric literals |
| Built-ins | Built-in functions |
| Types | Type names |
| Type Keywords | Type-related keywords |

Click a color swatch to open a color picker, or type a hex color code directly.

### Console Colors

- **Foreground** — default text color in the console
- **Background** — console background color

### Chat Colors

- **User Prompt** — background color for your chat bubbles
- **AI Response** — background color for AI response bubbles

### Developer Tools

Configure paths to compilers and runtimes. Leave a field empty to use auto-detection. Use the **Browse** button to select a specific executable.

| Tool | Used For |
|------|----------|
| C++ (g++) | Compiling C++ programs |
| Java (javac) | Compiling Java programs |
| JavaScript (node) | Running JavaScript programs |
| Perl | Running Perl programs |
| Python | Running Python programs |
| Rust (rustc) | Building Rust programs |
| Swift (swiftc) | Compiling Swift programs |

---

## Window Management

### Multiple Windows

- **File → New** (Cmd/Ctrl+N) — opens a new empty CodegenIDE window
- Each window is independent with its own editor, console, and preview

### Window Menu

| Item | Shortcut | Action |
|------|----------|--------|
| Minimize | Cmd/Ctrl+M | Minimize the current window |
| Zoom | — | Toggle between maximized and normal size |
| Previous | Cmd/Ctrl+Shift+` | Switch to the previous window |
| Next | Cmd/Ctrl+` | Switch to the next window |
| Cascade All | — | Arrange all windows in an overlapping cascade |
| Tile All | — | Tile all windows in a grid to fill the screen |

Below the separator, all open windows are listed. The current window is shown with a checkmark and bold text. Click any window name to bring it to the front.

### Layout Persistence

Window size and all divider positions (editor/console split, editor/preview split, AI panel width) are saved automatically when you close a window and restored on next launch.

---

## File Management

### Saving

| Action | Shortcut | Description |
|--------|----------|-------------|
| Save | Cmd/Ctrl+S | Save to current file (disabled if no changes) |
| Save As... | Cmd/Ctrl+Shift+S | Save to a new file (auto-appends .src if needed) |

### Modified File Indicator

The Save menu item is only enabled when the file has unsaved changes. When you close a window or open a different file with unsaved changes, you'll be prompted to save.

### Generated Files

Generated source files are stored in a `gen/` subdirectory relative to your `.src` file:

```
myproject/
├── hello.src          ← Your Codegen source
└── gen/
    ├── hello.cpp
    ├── hello.java
    ├── hello.js
    ├── hello.pl
    ├── hello.py
    ├── hello.rs
    ├── hello.swift
    ├── Utils.hpp      ← Utility libraries (copied automatically)
    ├── Utils.jar
    └── ...
```

---

## Keyboard Shortcuts Reference

| Action | macOS | Windows/Linux |
|--------|-------|---------------|
| New Window | Cmd+N | Ctrl+N |
| Open | Cmd+O | Ctrl+O |
| Close Window | Cmd+W | Ctrl+W |
| Save | Cmd+S | Ctrl+S |
| Save As | Cmd+Shift+S | Ctrl+Shift+S |
| Undo | Cmd+Z | Ctrl+Z |
| Redo | Cmd+Shift+Z | Ctrl+Shift+Z |
| Cut | Cmd+X | Ctrl+X |
| Copy | Cmd+C | Ctrl+C |
| Paste | Cmd+V | Ctrl+V |
| Find/Replace | Cmd+F | Ctrl+F |
| Indent | Cmd+] | Ctrl+] |
| Outdent | Cmd+[ | Ctrl+[ |
| Stop Program | Cmd+. | Ctrl+. |
| Minimize | Cmd+M | Ctrl+M |
| Next Window | Cmd+` | Ctrl+` |
| Previous Window | Cmd+Shift+` | Ctrl+Shift+` |
| Quit | Cmd+Q | Ctrl+Q |

---

## Platform Notes

### macOS
- Uses the native system menu bar
- About, Settings, and Quit are in the application menu
- Double-clicking `.src` files in Finder opens them in a new CodegenIDE window
- Input method auto-correction is disabled in the editor

### Windows
- Application menu items (About, Settings, License Key, Quit) appear in a "CodegenIDE" menu
- MSI installer registers `.src` file association

### Linux
- Requires X11 (XWayland supported)
- WM_CLASS is set for proper taskbar integration
- DEB/RPM packages register `.src` file association

©2026 Richard Lesh.  All rights reserved.

[Codgen Language](https://pureprogrammer.org/Codegen/docs/) | [Pure Programmer](https://pureprogrammer.org) | [Glowing Cat Software](https://glowingcat.com)
import java.awt.Color;
import java.awt.Font;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class AppSettings {
    private static final Path SETTINGS_FILE =
        Path.of(System.getProperty("user.home"), ".codegenide-settings.json");

    public String fontName = "Monospaced";
    public int fontSize = 14;
    public String aiFontName = detectAIFont();
    public int aiFontSize = 14;

    private static String detectAIFont() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String[] candidates;
        if (os.contains("linux")) candidates = new String[]{"DejaVu Sans", "Arial", "Helvetica", "SansSerif"};
        else if (os.contains("win")) candidates = new String[]{"Calibri", "Arial", "Helvetica", "SansSerif"};
        else candidates = new String[]{"Arial", "Helvetica", "SansSerif"};
        for (String name : candidates) {
            Font f = new Font(name, Font.PLAIN, 14);
            if (!f.getFamily().equals("Dialog")) return name;
        }
        return "SansSerif";
    }

    public String licenseEmail = null;
    public String licenseKey = null;
    public String llmVendor = "OpenAI";
    public String llmModel = "gpt-4o";
    public String llmApiKey = null;
    public Color userPromptColor = new Color(0x99, 0xCC, 0xFF);
    public Color aiResponseColor = new Color(0x00, 0x33, 0x99);
    public Color consoleFg = new Color(0xBB, 0xBB, 0xBB);
    public Color consoleBg = new Color(0, 0, 0);
    public boolean aiVisible = false;
    public int windowWidth = 1024;
    public int windowHeight = 768;
    public int mainDivider = -1;    // editor/terminal vertical split
    public int aiDivider = -1;      // main/AI horizontal split
    public int editorPreviewDivider = -1;  // editor/preview horizontal split
    // Developer tool paths (empty string means auto-detect)
    public String cppPath = "";
    public String javaPath = "";
    public String nodePath = "";
    public String perlPath = "";
    public String pythonPath = "";
    public String rustPath = "";
    public String swiftPath = "";

    public Color[] colors = {
        Color.BLACK,              // normal
        new Color(0, 80, 255),    // keyword
        new Color(255, 0, 255),   // directive
        new Color(128, 128, 128), // comment
        new Color(255, 0 , 0),    // string
        new Color(255, 148, 0),   // number
        new Color(0,229, 190 ),   // built-in
        new Color(75, 229, 0),    // type
        new Color(127, 0, 255)    // type keyword
    };

    public void save() {
        try {
            StringBuilder sb = new StringBuilder("{\n");
            sb.append("  \"fontName\": \"").append(escape(fontName)).append("\",\n");
            sb.append("  \"fontSize\": ").append(fontSize).append(",\n");
            sb.append("  \"aiFontName\": \"").append(escape(aiFontName)).append("\",\n");
            sb.append("  \"aiFontSize\": ").append(aiFontSize).append(",\n");
            if (licenseEmail != null) sb.append("  \"licenseEmail\": \"").append(escape(licenseEmail)).append("\",\n");
            if (licenseKey != null) sb.append("  \"licenseKey\": \"").append(escape(licenseKey)).append("\",\n");
            if (llmVendor != null) sb.append("  \"llmVendor\": \"").append(escape(llmVendor)).append("\",\n");
            if (llmModel != null) sb.append("  \"llmModel\": \"").append(escape(llmModel)).append("\",\n");
            if (llmApiKey != null) sb.append("  \"llmApiKey\": \"").append(escape(llmApiKey)).append("\",\n");
            sb.append("  \"userPromptColor\": \"").append(colorToHex(userPromptColor)).append("\",\n");
            sb.append("  \"aiResponseColor\": \"").append(colorToHex(aiResponseColor)).append("\",\n");
            sb.append("  \"consoleFg\": \"").append(colorToHex(consoleFg)).append("\",\n");
            sb.append("  \"consoleBg\": \"").append(colorToHex(consoleBg)).append("\",\n");
            sb.append("  \"aiVisible\": ").append(aiVisible).append(",\n");
            sb.append("  \"windowWidth\": ").append(windowWidth).append(",\n");
            sb.append("  \"windowHeight\": ").append(windowHeight).append(",\n");
            sb.append("  \"mainDivider\": ").append(mainDivider).append(",\n");
            sb.append("  \"aiDivider\": ").append(aiDivider).append(",\n");
            sb.append("  \"editorPreviewDivider\": ").append(editorPreviewDivider).append(",\n");
            if (!cppPath.isEmpty()) sb.append("  \"cppPath\": \"").append(escape(cppPath)).append("\",\n");
            if (!javaPath.isEmpty()) sb.append("  \"javaPath\": \"").append(escape(javaPath)).append("\",\n");
            if (!nodePath.isEmpty()) sb.append("  \"nodePath\": \"").append(escape(nodePath)).append("\",\n");
            if (!perlPath.isEmpty()) sb.append("  \"perlPath\": \"").append(escape(perlPath)).append("\",\n");
            if (!pythonPath.isEmpty()) sb.append("  \"pythonPath\": \"").append(escape(pythonPath)).append("\",\n");
            if (!rustPath.isEmpty()) sb.append("  \"rustPath\": \"").append(escape(rustPath)).append("\",\n");
            if (!swiftPath.isEmpty()) sb.append("  \"swiftPath\": \"").append(escape(swiftPath)).append("\",\n");
            sb.append("  \"colors\": [");
            for (int i = 0; i < colors.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(colorToHex(colors[i])).append("\"");
            }
            sb.append("]\n");
            sb.append("}");
            Files.writeString(SETTINGS_FILE, sb.toString());
        } catch (IOException e) {
            // silently ignore save errors
        }
    }

    public static AppSettings load() {
        AppSettings s = new AppSettings();
        try {
            if (!Files.exists(SETTINGS_FILE)) return s;
            String json = Files.readString(SETTINGS_FILE);

            String font = extractString(json, "fontName");
            if (font != null) s.fontName = font;

            Integer size = extractInt(json, "fontSize");
            if (size != null) s.fontSize = size;

            String aiFont = extractString(json, "aiFontName");
            if (aiFont != null) s.aiFontName = aiFont;

            Integer aiSize = extractInt(json, "aiFontSize");
            if (aiSize != null) s.aiFontSize = aiSize;

            s.licenseEmail = extractString(json, "licenseEmail");
            s.licenseKey = extractString(json, "licenseKey");

            String vendor = extractString(json, "llmVendor");
            if (vendor != null) s.llmVendor = vendor;
            String model = extractString(json, "llmModel");
            if (model != null) s.llmModel = model;
            s.llmApiKey = extractString(json, "llmApiKey");

            String upc = extractString(json, "userPromptColor");
            if (upc != null) s.userPromptColor = hexToColor(upc);
            String arc = extractString(json, "aiResponseColor");
            if (arc != null) s.aiResponseColor = hexToColor(arc);

            String cfg = extractString(json, "consoleFg");
            if (cfg != null) s.consoleFg = hexToColor(cfg);
            String cbg = extractString(json, "consoleBg");
            if (cbg != null) s.consoleBg = hexToColor(cbg);

            Matcher aiVisMatcher = Pattern.compile("\"aiVisible\"\\s*:\\s*(true|false)").matcher(json);
            if (aiVisMatcher.find()) s.aiVisible = Boolean.parseBoolean(aiVisMatcher.group(1));

            Integer ww = extractInt(json, "windowWidth");
            if (ww != null) s.windowWidth = ww;
            Integer wh = extractInt(json, "windowHeight");
            if (wh != null) s.windowHeight = wh;
            Integer md = extractInt(json, "mainDivider");
            if (md != null) s.mainDivider = md;
            Integer ad = extractInt(json, "aiDivider");
            if (ad != null) s.aiDivider = ad;
            Integer epd = extractInt(json, "editorPreviewDivider");
            if (epd != null) s.editorPreviewDivider = epd;

            String cpp = extractString(json, "cppPath");
            if (cpp != null) s.cppPath = cpp;
            String java = extractString(json, "javaPath");
            if (java != null) s.javaPath = java;
            String node = extractString(json, "nodePath");
            if (node != null) s.nodePath = node;
            String perl = extractString(json, "perlPath");
            if (perl != null) s.perlPath = perl;
            String python = extractString(json, "pythonPath");
            if (python != null) s.pythonPath = python;
            String rust = extractString(json, "rustPath");
            if (rust != null) s.rustPath = rust;
            String swift = extractString(json, "swiftPath");
            if (swift != null) s.swiftPath = swift;

            List<String> colorList = extractArray(json, "colors");
            if (colorList != null) {
                for (int i = 0; i < Math.min(colorList.size(), s.colors.length); i++) {
                    s.colors[i] = hexToColor(colorList.get(i));
                }
            }
        } catch (Exception e) {
            // return defaults on any parse error
        }
        return s;
    }

    private static String colorToHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static Color hexToColor(String hex) {
        hex = hex.trim().replace("\"", "");
        return Color.decode(hex);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractString(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static Integer extractInt(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private static List<String> extractArray(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]+)]").matcher(json);
        if (!m.find()) return null;
        String[] items = m.group(1).split(",");
        List<String> result = new ArrayList<>();
        for (String item : items) {
            result.add(item.trim().replace("\"", ""));
        }
        return result;
    }

    /**
     * Detect the path for a given tool by searching standard locations.
     * Returns the absolute path if found, or empty string if not found.
     */
    public static String detectToolPath(String toolName) {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> candidates = new ArrayList<>();

        if (os.contains("mac")) {
            // macOS: Homebrew (ARM + Intel), Xcode, system
            switch (toolName) {
                case "g++" -> {
                    candidates.add("/opt/homebrew/bin/g++");
                    candidates.add("/usr/local/bin/g++");
                    candidates.add("/usr/bin/g++");
                }
                case "javac" -> {
                    candidates.add("/opt/homebrew/bin/javac");
                    candidates.add("/usr/local/bin/javac");
                    candidates.add("/usr/bin/javac");
                }
                case "node" -> {
                    candidates.add("/opt/homebrew/bin/node");
                    candidates.add("/usr/local/bin/node");
                }
                case "perl" -> {
                    candidates.add("/opt/homebrew/bin/perl");
                    candidates.add("/usr/local/bin/perl");
                    candidates.add("/usr/bin/perl");
                }
                case "python3" -> {
                    candidates.add("/opt/homebrew/bin/python3");
                    candidates.add("/usr/local/bin/python3");
                    candidates.add("/usr/bin/python3");
                }
                case "rustc" -> {
                    candidates.add(System.getProperty("user.home") + "/.cargo/bin/rustc");
                    candidates.add("/opt/homebrew/bin/rustc");
                    candidates.add("/usr/local/bin/rustc");
                }
                case "swiftc" -> {
                    candidates.add("/usr/bin/swiftc");
                    candidates.add("/opt/homebrew/bin/swiftc");
                }
            }
        } else if (os.contains("win")) {
            // Windows: common install locations
            switch (toolName) {
                case "g++" -> {
                    candidates.add("C:\\msys64\\ucrt64\\bin\\g++.exe");
                    candidates.add("C:\\msys64\\mingw64\\bin\\g++.exe");
                    candidates.add("C:\\MinGW\\bin\\g++.exe");
                }
                case "javac" -> {
                    candidates.add("C:\\Program Files\\Java\\jdk-21\\bin\\javac.exe");
                    candidates.add("C:\\Program Files\\Eclipse Adoptium\\jdk-21\\bin\\javac.exe");
                }
                case "node" -> {
                    candidates.add("C:\\Program Files\\nodejs\\node.exe");
                }
                case "perl" -> {
                    candidates.add("C:\\Strawberry\\perl\\bin\\perl.exe");
                    candidates.add("C:\\Perl\\bin\\perl.exe");
                }
                case "python3" -> {
                    candidates.add(System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python312\\python.exe");
                    candidates.add(System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python311\\python.exe");
                    candidates.add("C:\\Python312\\python.exe");
                    candidates.add("C:\\Python311\\python.exe");
                }
                case "rustc" -> {
                    candidates.add(System.getProperty("user.home") + "\\.cargo\\bin\\rustc.exe");
                }
                case "swiftc" -> {
                    candidates.add("C:\\Library\\Developer\\Toolchains\\unknown-Asserts-development.xctoolchain\\usr\\bin\\swiftc.exe");
                }
            }
        } else {
            // Linux
            switch (toolName) {
                case "g++" -> {
                    candidates.add("/usr/bin/g++");
                    candidates.add("/usr/local/bin/g++");
                }
                case "javac" -> {
                    candidates.add("/usr/bin/javac");
                    candidates.add("/usr/local/bin/javac");
                }
                case "node" -> {
                    candidates.add("/usr/bin/node");
                    candidates.add("/usr/local/bin/node");
                }
                case "perl" -> {
                    candidates.add("/usr/bin/perl");
                    candidates.add("/usr/local/bin/perl");
                }
                case "python3" -> {
                    candidates.add("/usr/bin/python3");
                    candidates.add("/usr/local/bin/python3");
                }
                case "rustc" -> {
                    candidates.add(System.getProperty("user.home") + "/.cargo/bin/rustc");
                    candidates.add("/usr/bin/rustc");
                    candidates.add("/usr/local/bin/rustc");
                }
                case "swiftc" -> {
                    candidates.add("/usr/bin/swiftc");
                    candidates.add("/usr/local/bin/swiftc");
                }
            }
        }

        for (String path : candidates) {
            if (Files.isExecutable(Path.of(path))) {
                return path;
            }
        }
        return "";
    }

    /**
     * Get the effective path for a tool. If the user has configured a path, use it.
     * Otherwise, try to auto-detect it. If that fails, return the bare command name
     * (which will rely on PATH resolution at runtime).
     */
    public String getEffectiveToolPath(String toolName) {
        String configured = switch (toolName) {
            case "g++" -> cppPath;
            case "javac" -> javaPath;
            case "node" -> nodePath;
            case "perl" -> perlPath;
            case "python3" -> pythonPath;
            case "rustc" -> rustPath;
            case "swiftc" -> swiftPath;
            default -> "";
        };
        if (configured != null && !configured.isEmpty()) return configured;
        String detected = detectToolPath(toolName);
        if (!detected.isEmpty()) return detected;
        return toolName; // fallback to bare command name
    }
}

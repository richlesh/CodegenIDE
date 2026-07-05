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
    public Color[] colors = {
        Color.BLACK,              // normal
        new Color(0, 0, 180),     // keyword
        new Color(128, 0, 128),   // directive
        new Color(0x99, 0x99, 0x99),   // comment
        new Color(163, 21, 21),   // string
        new Color(180, 100, 0),   // number
        new Color(200, 0, 100),   // built-in
        new Color(0, 100, 100),   // type
        new Color(100, 0, 150)    // type keyword
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
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
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
}

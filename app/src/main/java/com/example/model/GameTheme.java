package com.example.model;

import android.content.Context;
import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data-driven theme model loaded from assets/themes.json.
 * Supports adding more themes in themes.json while preserving the exact
 * bg, accent, text, and price values of each supplied theme.
 */
public final class GameTheme {

    public static final GameTheme DEFAULT = new GameTheme(
            "DEFAULT", 0, "DEFAULT", 0, "#1e1e2f", "#4cafef", "#ffffff"
    );

    private static volatile List<GameTheme> loadedThemes = Collections.singletonList(DEFAULT);
    private static volatile boolean initialized = false;

    public final String id;
    public final int ordinalIndex;
    public final String displayName;
    public final int price;
    public final int backgroundColor;
    public final int surfaceColor;
    public final int boardColor;
    public final int tileColor;
    public final int primaryButtonColor;
    public final int textPrimaryColor;
    public final int textSecondaryColor;
    public final int previewColor;
    public final boolean isLightTheme;

    private GameTheme(String id, int ordinalIndex, String displayName, int price,
                      String bgHex, String accentHex, String textHex) {
        this.id = id;
        this.ordinalIndex = ordinalIndex;
        this.displayName = displayName;
        this.price = price;

        int bg = Color.parseColor(bgHex);
        int accent = Color.parseColor(accentHex);
        int txt = Color.parseColor(textHex);

        this.isLightTheme = ColorUtils.calculateLuminance(bg) > 0.5;
        int shiftTarget = isLightTheme ? Color.BLACK : Color.WHITE;

        this.backgroundColor = bg;
        this.surfaceColor = ColorUtils.blendARGB(bg, shiftTarget, 0.06f);
        this.boardColor = ColorUtils.blendARGB(bg, shiftTarget, 0.04f);
        this.tileColor = ColorUtils.blendARGB(bg, shiftTarget, 0.11f);
        this.primaryButtonColor = accent;
        this.textPrimaryColor = txt;
        this.textSecondaryColor = ColorUtils.blendARGB(txt, bg, 0.30f);
        this.previewColor = accent;
    }

    public static void ensureInitialized(Context context) {
        if (initialized || context == null) return;
        synchronized (GameTheme.class) {
            if (initialized) return;
            try (InputStream is = context.getApplicationContext().getAssets().open("themes.json")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                String json = new String(out.toByteArray(), StandardCharsets.UTF_8);
                JSONArray arr = new JSONArray(json);
                List<GameTheme> list = new ArrayList<>(arr.length());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String id = obj.getString("id");
                    String name = obj.getString("name");
                    String bg = obj.getString("bg");
                    String accent = obj.getString("accent");
                    String text = obj.getString("text");
                    int price = obj.optInt("price", 0);
                    if (i == 0 && "DEFAULT".equals(id)) {
                        list.add(DEFAULT);
                    } else {
                        list.add(new GameTheme(id, i, name, price, bg, accent, text));
                    }
                }
                if (!list.isEmpty()) {
                    loadedThemes = Collections.unmodifiableList(list);
                }
                initialized = true;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load themes.json from assets", e);
            }
        }
    }

    public static GameTheme[] values() {
        List<GameTheme> snapshot = loadedThemes;
        return snapshot.toArray(new GameTheme[0]);
    }

    public static GameTheme fromName(String name) {
        if (name == null) return DEFAULT;
        String trimmed = name.trim();
        String normalized = trimmed.replace(' ', '_');
        List<GameTheme> snapshot = loadedThemes;
        for (GameTheme t : snapshot) {
            if (t.id.equalsIgnoreCase(trimmed) || t.id.equalsIgnoreCase(normalized)) {
                return t;
            }
        }
        for (GameTheme t : snapshot) {
            if (t.displayName.equalsIgnoreCase(trimmed)) {
                return t;
            }
        }
        return DEFAULT;
    }

    public String name() {
        return id;
    }

    public int ordinal() {
        return ordinalIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameTheme)) return false;
        GameTheme other = (GameTheme) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return displayName;
    }
}

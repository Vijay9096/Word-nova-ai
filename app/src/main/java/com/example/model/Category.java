package com.example.model;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven Category model loaded from assets/words/categories.json.
 * Allows the app creator to introduce new categories cleanly via assets.
 */
public final class Category {

    public static final Category GENERAL = new Category("GENERAL", 0, "GENERAL", "GENERAL.txt");
    public static final Category PROFESSION = new Category("PROFESSION", 1, "PROFESSION", "PROFESSION.txt");
    public static final Category ANIMAL = new Category("ANIMAL", 2, "ANIMAL", "ANIMAL.txt");
    public static final Category FRUITS = new Category("FRUITS", 3, "FRUITS", "FRUITS.txt");
    public static final Category VEHICLES = new Category("VEHICLES", 4, "VEHICLES", "VEHICLES.txt");
    public static final Category COLORS = new Category("COLORS", 5, "COLORS", "COLORS.txt");
    public static final Category SPORTS = new Category("SPORTS", 6, "SPORTS", "SPORTS.txt");
    public static final Category FLOWERS = new Category("FLOWERS", 7, "FLOWERS", "FLOWERS.txt");
    public static final Category FOODS = new Category("FOODS", 8, "FOODS", "FOODS.txt");
    public static final Category DRINKS = new Category("DRINKS", 9, "DRINKS", "DRINKS.txt");

    private static final Map<String, Category> BUILT_INS = new LinkedHashMap<>();
    static {
        for (Category c : Arrays.asList(
                GENERAL, PROFESSION, ANIMAL, FRUITS, VEHICLES,
                COLORS, SPORTS, FLOWERS, FOODS, DRINKS
        )) {
            BUILT_INS.put(c.id, c);
        }
    }

    private static volatile List<Category> loadedCategories =
            Collections.unmodifiableList(new ArrayList<>(BUILT_INS.values()));
    private static volatile boolean initialized = false;

    public final String id;
    public final int ordinalIndex;
    public final String displayName;
    public final String fileName;

    private Category(String id, int ordinalIndex, String displayName, String fileName) {
        this.id = id;
        this.ordinalIndex = ordinalIndex;
        this.displayName = displayName;
        this.fileName = fileName;
    }

    public static void ensureInitialized(Context context) {
        if (initialized || context == null) return;
        synchronized (Category.class) {
            if (initialized) return;
            try (InputStream is = context.getApplicationContext().getAssets().open("words/categories.json")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[2048];
                int n;
                while ((n = is.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                String json = new String(out.toByteArray(), StandardCharsets.UTF_8);
                JSONArray arr = new JSONArray(json);
                List<Category> list = new ArrayList<>(arr.length());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String id = obj.getString("id");
                    String displayName = obj.optString("displayName", id);
                    String file = obj.optString("file", id + ".txt");
                    Category existing = BUILT_INS.get(id);
                    if (existing != null && existing.ordinalIndex == i
                            && existing.displayName.equals(displayName)
                            && existing.fileName.equals(file)) {
                        list.add(existing);
                    } else {
                        list.add(new Category(id, i, displayName, file));
                    }
                }
                if (!list.isEmpty()) {
                    loadedCategories = Collections.unmodifiableList(list);
                }
                initialized = true;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load words/categories.json from assets", e);
            }
        }
    }

    public static Category[] values() {
        List<Category> snapshot = loadedCategories;
        return snapshot.toArray(new Category[0]);
    }

    public static Category fromName(String name) {
        if (name == null) return GENERAL;
        String trimmed = name.trim();
        for (Category c : loadedCategories) {
            if (c.displayName.equalsIgnoreCase(trimmed) || c.id.equalsIgnoreCase(trimmed)) {
                return c;
            }
        }
        return GENERAL;
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
        if (!(o instanceof Category)) return false;
        Category other = (Category) o;
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

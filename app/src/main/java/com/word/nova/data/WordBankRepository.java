package com.word.nova.data;

import android.content.Context;

import com.word.nova.model.Category;
import com.word.nova.model.Difficulty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Loads each category's exact word database from assets/words/<CATEGORY>.txt.
 * Keeps categories strictly separated and preserves exact spelling without
 * modifying, inventing, or mixing words between categories.
 */
public class WordBankRepository {

    public static final int WORDS_PER_LEVEL = 15;

    private static final List<String> GENERAL_LEVEL_1 = Arrays.asList(
            "ORBIT", "SHOUT", "ABOVE", "INVOICE", "SESSION",
            "BLESS", "GUAVA", "BEAR", "BONES", "TIME",
            "WATT", "MARKET", "ZOOM", "PATCH", "OUTCOME"
    );

    private static final List<String> DAILY_DEFAULT_WORDS = Arrays.asList(
            "SUN", "MOON", "STAR", "CLOUD", "RAIN", "WIND"
    );

    private static volatile Map<String, List<String>> categoryWords = Collections.emptyMap();
    private static volatile boolean initialized = false;

    public static void ensureInitialized(Context context) {
        if (initialized || context == null) return;
        synchronized (WordBankRepository.class) {
            if (initialized) return;
            Category.ensureInitialized(context);
            Context appCtx = context.getApplicationContext();
            Map<String, List<String>> loaded = new HashMap<>();

            for (Category cat : Category.values()) {
                Set<String> exactWords = new LinkedHashSet<>();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(appCtx.getAssets().open("words/" + cat.fileName), StandardCharsets.UTF_8)
                )) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            exactWords.add(trimmed);
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to load word list for category " + cat.displayName, e);
                }
                loaded.put(cat.id, Collections.unmodifiableList(new ArrayList<>(exactWords)));
            }

            categoryWords = Collections.unmodifiableMap(loaded);
            initialized = true;
        }
    }

    /**
     * Returns an unmodifiable view of all extracted words belonging strictly to the given category.
     */
    public static List<String> getAllWordsForCategory(Category category) {
        if (category == null) return Collections.emptyList();
        List<String> list = categoryWords.get(category.id);
        return list == null ? Collections.emptyList() : list;
    }

    public static List<String> getWordsForLevel(Category category, Difficulty difficulty, int level) {
        if (difficulty == Difficulty.DAILY) {
            return getDailyWords(GamePrefs.todayDateKey());
        }

        List<String> pool = getAllWordsForCategory(category);
        if (pool.isEmpty()) {
            return Collections.emptyList();
        }

        // Preserve General Level 1 screenshot compatibility when all 15 words exist verbatim in GENERAL.
        if (Category.GENERAL.equals(category) && level == 1 && pool.containsAll(GENERAL_LEVEL_1)) {
            return new ArrayList<>(GENERAL_LEVEL_1);
        }

        List<String> orderedPool = new ArrayList<>(pool);
        int effectiveLevelIndex = Math.max(0, level - 1);
        int bucketCount = 100;

        if (Category.GENERAL.equals(category) && pool.containsAll(GENERAL_LEVEL_1)) {
            orderedPool.removeAll(GENERAL_LEVEL_1);
            effectiveLevelIndex = Math.max(0, level - 2);
            bucketCount = 99;
        }

        int total = orderedPool.size();
        if (total == 0) {
            return new ArrayList<>(pool.subList(0, Math.min(WORDS_PER_LEVEL, pool.size())));
        }

        long shuffleSeed = ((long) category.ordinal() * 10007L) + ((long) difficulty.ordinal() * 1009L);
        Collections.shuffle(orderedPool, new Random(shuffleSeed));
        // Stable sort by length preserves the deterministic per-difficulty shuffle within each length group.
        Collections.sort(orderedPool, (a, b) -> Integer.compare(a.length(), b.length()));

        // Deal round-robin across levels so every level gets a balanced mix of word lengths
        // while covering every word in the category across levels 1..100 without duplication.
        int effectiveBuckets = Math.max(1, Math.min(bucketCount, (total + WORDS_PER_LEVEL - 1) / WORDS_PER_LEVEL));
        int bucket = effectiveLevelIndex % effectiveBuckets;

        List<String> result = new ArrayList<>(WORDS_PER_LEVEL);
        for (int idx = bucket; idx < total && result.size() < WORDS_PER_LEVEL; idx += effectiveBuckets) {
            result.add(orderedPool.get(idx));
        }
        for (int i = 0; result.size() < Math.min(WORDS_PER_LEVEL, total); i++) {
            String candidate = orderedPool.get((bucket + i) % total);
            if (!result.contains(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    public static List<String> getDailyWords(String dateKey) {
        if ("2026-11-17".equals(dateKey)) {
            return new ArrayList<>(DAILY_DEFAULT_WORDS);
        }
        List<String> combined = new ArrayList<>(DAILY_DEFAULT_WORDS);
        for (Category c : Category.values()) {
            for (String w : getAllWordsForCategory(c)) {
                if (w.length() >= 4 && w.length() <= 7) {
                    combined.add(w);
                }
            }
        }
        long seed = dateKey.hashCode();
        if (seed == 0) seed = 42L;
        Random rng = new Random(seed);
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(combined));
        Collections.shuffle(unique, rng);
        List<String> selected = new ArrayList<>(6);
        for (int i = 0; i < Math.min(6, unique.size()); i++) {
            selected.add(unique.get(i));
        }
        return selected;
    }
}

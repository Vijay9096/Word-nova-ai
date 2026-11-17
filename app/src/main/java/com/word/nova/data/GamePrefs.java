package com.word.nova.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.word.nova.model.Achievement;
import com.word.nova.model.Category;
import com.word.nova.model.Difficulty;
import com.word.nova.model.GameTheme;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class GamePrefs {
    private static final String PREF_NAME = "word_nova_prefs";

    private static final String KEY_COINS = "coins";
    private static final String KEY_HINTS = "hints";
    private static final String KEY_PLAYER_NAME = "player_name";
    private static final String KEY_CURRENT_THEME = "current_theme";
    private static final String KEY_UNLOCKED_THEMES = "unlocked_themes";
    private static final String KEY_UNLOCKED_ACHIEVEMENTS = "unlocked_achievements";
    private static final String KEY_LEVELS_COMPLETED = "levels_completed";
    private static final String KEY_WORDS_FOUND = "words_found";
    private static final String KEY_HARD_COMPLETED = "hard_completed";
    private static final String KEY_DAILY_COMPLETED_COUNT = "daily_completed_count";
    private static final String KEY_SPINS_COUNT = "spins_count";
    private static final String KEY_DAILY_PUZZLE_DATE = "daily_puzzle_date";
    private static final String KEY_DAILY_REWARD_LAST_DATE = "daily_reward_last_date";
    private static final String KEY_DAILY_REWARD_STREAK = "daily_reward_streak";
    private static final String KEY_LAST_FREE_SPIN_DATE = "last_free_spin_date";
    private static final String KEY_LAST_FREE_SPIN_TS = "last_free_spin_ts";

    private static final String KEY_COLORBLIND = "setting_colorblind";
    private static final String KEY_PERFORMANCE = "setting_performance";
    private static final String KEY_MUSIC = "setting_music";
    private static final String KEY_SFX = "setting_sfx";
    // TODO(TESTING): Temporary key for Unlock All Levels testing feature — remove before production release
    private static final String KEY_TEST_ALL_LEVELS_UNLOCKED = "testing_all_levels_unlocked";

    private static final int DEFAULT_COINS = 100;
    private static final int DEFAULT_HINTS = 2;
    private static final long FREE_SPIN_COOLDOWN_MS = 24L * 60L * 60L * 1000L;

    public interface OnEconomyChangeListener {
        void onEconomyChanged(int coins, int hints);
    }

    public static class SavedLevelState {
        public final PuzzleGenerator.PuzzleBoard board;
        public final int nextColorIdx;
        public final int hintsUsed;
        public final int hintedWordIdx;
        public final int remainingSeconds;
        public final boolean completed;

        public SavedLevelState(
                PuzzleGenerator.PuzzleBoard board,
                int nextColorIdx,
                int hintsUsed,
                int hintedWordIdx,
                int remainingSeconds,
                boolean completed
        ) {
            this.board = board;
            this.nextColorIdx = nextColorIdx;
            this.hintsUsed = hintsUsed;
            this.hintedWordIdx = hintedWordIdx;
            this.remainingSeconds = remainingSeconds;
            this.completed = completed;
        }
    }

    private final SharedPreferences prefs;
    private final CopyOnWriteArrayList<OnEconomyChangeListener> economyListeners = new CopyOnWriteArrayList<>();

    public GamePrefs(Context context) {
        GameTheme.ensureInitialized(context);
        Category.ensureInitialized(context);
        WordBankRepository.ensureInitialized(context);
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureDefaults();
    }

    public void addEconomyChangeListener(OnEconomyChangeListener listener) {
        if (listener != null && !economyListeners.contains(listener)) {
            economyListeners.add(listener);
        }
    }

    public void removeEconomyChangeListener(OnEconomyChangeListener listener) {
        economyListeners.remove(listener);
    }

    public void notifyEconomyChanged() {
        int c = getCoins();
        int h = getHints();
        for (OnEconomyChangeListener l : economyListeners) {
            l.onEconomyChanged(c, h);
        }
    }

    private void ensureDefaults() {
        if (!prefs.contains(KEY_COINS)) {
            prefs.edit()
                    .putInt(KEY_COINS, DEFAULT_COINS)
                    .putInt(KEY_HINTS, DEFAULT_HINTS)
                    .putString(KEY_PLAYER_NAME, "Player")
                    .putString(KEY_CURRENT_THEME, GameTheme.DEFAULT.name())
                    .putBoolean(KEY_MUSIC, true)
                    .putBoolean(KEY_SFX, true)
                    .apply();
        }
        Set<String> themes = getUnlockedThemeNames();
        if (!themes.contains(GameTheme.DEFAULT.name())) {
            themes.add(GameTheme.DEFAULT.name());
            prefs.edit().putStringSet(KEY_UNLOCKED_THEMES, themes).apply();
        }
    }

    public static String todayDateKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String todayDisplayDate() {
        return new SimpleDateFormat("EEE MMM d yyyy", Locale.US).format(new Date());
    }

    // --- Economy ---
    public int getCoins() {
        return Math.max(0, prefs.getInt(KEY_COINS, DEFAULT_COINS));
    }

    public void addCoins(int amount) {
        if (amount <= 0) return;
        long updated = (long) getCoins() + amount;
        prefs.edit().putInt(KEY_COINS, (int) Math.min(Integer.MAX_VALUE, updated)).apply();
        notifyEconomyChanged();
    }

    public boolean spendCoins(int amount) {
        if (amount <= 0) return true;
        int current = getCoins();
        if (current < amount) return false;
        prefs.edit().putInt(KEY_COINS, current - amount).apply();
        notifyEconomyChanged();
        return true;
    }

    public int getHints() {
        return Math.max(0, prefs.getInt(KEY_HINTS, DEFAULT_HINTS));
    }

    public void addHints(int amount) {
        if (amount <= 0) return;
        long updated = (long) getHints() + amount;
        prefs.edit().putInt(KEY_HINTS, (int) Math.min(Integer.MAX_VALUE, updated)).apply();
        notifyEconomyChanged();
    }

    public boolean consumeHint() {
        int current = getHints();
        if (current <= 0) return false;
        prefs.edit().putInt(KEY_HINTS, current - 1).apply();
        notifyEconomyChanged();
        return true;
    }

    // --- Player Profile ---
    public String getPlayerName() {
        String name = prefs.getString(KEY_PLAYER_NAME, "Player");
        return (name == null || name.trim().isEmpty()) ? "Player" : name.trim();
    }

    public void setPlayerName(String name) {
        if (name == null || name.trim().isEmpty()) return;
        prefs.edit().putString(KEY_PLAYER_NAME, name.trim()).apply();
    }

    public int getLevelsCompletedCount() {
        return Math.max(0, prefs.getInt(KEY_LEVELS_COMPLETED, 0));
    }

    public int getWordsFoundCount() {
        return Math.max(0, prefs.getInt(KEY_WORDS_FOUND, 0));
    }

    public void incrementWordsFound(int count) {
        if (count <= 0) return;
        prefs.edit().putInt(KEY_WORDS_FOUND, getWordsFoundCount() + count).apply();
    }

    public int getHardLevelsCompletedCount() {
        return Math.max(0, prefs.getInt(KEY_HARD_COMPLETED, 0));
    }

    public int getDailyCompletedCount() {
        return Math.max(0, prefs.getInt(KEY_DAILY_COMPLETED_COUNT, 0));
    }

    public int getSpinsCount() {
        return Math.max(0, prefs.getInt(KEY_SPINS_COUNT, 0));
    }

    public void incrementSpinsCount() {
        prefs.edit().putInt(KEY_SPINS_COUNT, getSpinsCount() + 1).apply();
    }

    // --- Level Progression ("${currentDifficulty}-${currentCat}-level${currentLevel}") ---
    public static String levelKey(Difficulty difficulty, Category category, int level) {
        return difficulty.keyPrefix + "-" + category.displayName + "-level" + level;
    }

    public static String levelRewardClaimedKey(Difficulty difficulty, Category category, int level) {
        if (difficulty == Difficulty.DAILY) {
            return "reward_claimed_daily_" + todayDateKey();
        }
        return "reward_claimed_" + levelKey(difficulty, category, level);
    }

    public static String levelStateKey(Difficulty difficulty, Category category, int level) {
        if (difficulty == Difficulty.DAILY) {
            return "level_state_daily_" + todayDateKey();
        }
        return "level_state_" + levelKey(difficulty, category, level);
    }

    public boolean isLevelCompleted(Difficulty difficulty, Category category, int level) {
        if (difficulty == Difficulty.DAILY) {
            return todayDateKey().equals(prefs.getString(KEY_DAILY_PUZZLE_DATE, ""));
        }
        return prefs.getBoolean(levelKey(difficulty, category, level), false);
    }

    public boolean isLevelRewardClaimed(Difficulty difficulty, Category category, int level) {
        return prefs.getBoolean(levelRewardClaimedKey(difficulty, category, level), false)
                || isLevelCompleted(difficulty, category, level);
    }

    // TODO(TESTING): Temporary testing helpers — remove before production release
    public boolean isTestingAllLevelsUnlocked() {
        return prefs.getBoolean(KEY_TEST_ALL_LEVELS_UNLOCKED, false);
    }

    public void unlockAllLevelsForTesting() {
        prefs.edit().putBoolean(KEY_TEST_ALL_LEVELS_UNLOCKED, true).apply();
    }

    public boolean isLevelUnlocked(Difficulty difficulty, Category category, int level) {
        if (level <= 1) return true;
        if (isTestingAllLevelsUnlocked()) return true;
        return isLevelCompleted(difficulty, category, level - 1) || isLevelCompleted(difficulty, category, level);
    }

    public int getHighestUnlockedLevel(Difficulty difficulty, Category category) {
        for (int lvl = 1; lvl <= 100; lvl++) {
            if (!isLevelCompleted(difficulty, category, lvl)) {
                return lvl;
            }
        }
        return 100;
    }

    public boolean markLevelCompleted(Difficulty difficulty, Category category, int level) {
        boolean alreadyRewarded = isLevelRewardClaimed(difficulty, category, level);
        String rewardKey = levelRewardClaimedKey(difficulty, category, level);

        if (difficulty == Difficulty.DAILY) {
            String today = todayDateKey();
            boolean firstTimeToday = !today.equals(prefs.getString(KEY_DAILY_PUZZLE_DATE, ""));
            SharedPreferences.Editor editor = prefs.edit()
                    .putString(KEY_DAILY_PUZZLE_DATE, today)
                    .putBoolean(rewardKey, true);
            if (firstTimeToday) {
                editor.putInt(KEY_DAILY_COMPLETED_COUNT, getDailyCompletedCount() + 1);
            }
            editor.apply();
            return firstTimeToday && !alreadyRewarded;
        }
        String key = levelKey(difficulty, category, level);
        boolean wasCompleted = prefs.getBoolean(key, false);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, true);
        editor.putBoolean(rewardKey, true);
        if (!wasCompleted) {
            editor.putInt(KEY_LEVELS_COMPLETED, getLevelsCompletedCount() + 1);
            if (difficulty == Difficulty.HARD) {
                editor.putInt(KEY_HARD_COMPLETED, getHardLevelsCompletedCount() + 1);
            }
        }
        editor.apply();
        return !wasCompleted && !alreadyRewarded;
    }

    public void saveLevelState(
            Difficulty difficulty,
            Category category,
            int level,
            PuzzleGenerator.PuzzleBoard board,
            int nextColorIdx,
            int hintsUsed,
            int hintedWordIdx,
            int remainingSeconds,
            boolean completed
    ) {
        if (board == null || board.grid == null || board.grid.length == 0) return;
        try {
            JSONObject root = new JSONObject();
            JSONArray gridArr = new JSONArray();
            for (char[] row : board.grid) {
                gridArr.put(new String(row));
            }
            root.put("grid", gridArr);

            JSONArray wordsArr = new JSONArray();
            for (PuzzleGenerator.PlacedWord pw : board.placedWords) {
                JSONObject wObj = new JSONObject();
                wObj.put("w", pw.word);
                wObj.put("sr", pw.startRow);
                wObj.put("sc", pw.startCol);
                wObj.put("dr", pw.dRow);
                wObj.put("dc", pw.dCol);
                wObj.put("f", pw.found);
                wObj.put("c", pw.colorIndex);
                wordsArr.put(wObj);
            }
            root.put("words", wordsArr);
            root.put("nextColorIdx", nextColorIdx);
            root.put("hintsUsed", hintsUsed);
            root.put("hintedWordIdx", hintedWordIdx);
            root.put("remainingSeconds", remainingSeconds);
            root.put("completed", completed);

            prefs.edit().putString(levelStateKey(difficulty, category, level), root.toString()).apply();
        } catch (Exception ignored) {}
    }

    public SavedLevelState loadLevelState(Difficulty difficulty, Category category, int level) {
        String raw = prefs.getString(levelStateKey(difficulty, category, level), null);
        if (raw == null || raw.isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(raw);
            JSONArray gridArr = root.getJSONArray("grid");
            int n = gridArr.length();
            if (n <= 0) return null;
            char[][] grid = new char[n][n];
            for (int r = 0; r < n; r++) {
                String rowStr = gridArr.getString(r);
                if (rowStr.length() != n) return null;
                grid[r] = rowStr.toCharArray();
            }

            JSONArray wordsArr = root.getJSONArray("words");
            List<PuzzleGenerator.PlacedWord> placedWords = new ArrayList<>(wordsArr.length());
            for (int i = 0; i < wordsArr.length(); i++) {
                JSONObject wObj = wordsArr.getJSONObject(i);
                PuzzleGenerator.PlacedWord pw = new PuzzleGenerator.PlacedWord(
                        wObj.getString("w"),
                        wObj.getInt("sr"),
                        wObj.getInt("sc"),
                        wObj.getInt("dr"),
                        wObj.getInt("dc")
                );
                pw.found = wObj.optBoolean("f", false);
                pw.colorIndex = wObj.optInt("c", -1);
                placedWords.add(pw);
            }

            PuzzleGenerator.PuzzleBoard board = new PuzzleGenerator.PuzzleBoard(grid, placedWords);
            int nextColorIdx = root.optInt("nextColorIdx", 0);
            int hintsUsed = root.optInt("hintsUsed", 0);
            int hintedWordIdx = root.optInt("hintedWordIdx", -1);
            int remainingSeconds = root.optInt("remainingSeconds", difficulty.timeLimitSeconds);
            boolean completed = root.optBoolean("completed", false);
            return new SavedLevelState(board, nextColorIdx, hintsUsed, hintedWordIdx, remainingSeconds, completed);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isDailyPuzzleCompletedToday() {
        return todayDateKey().equals(prefs.getString(KEY_DAILY_PUZZLE_DATE, ""));
    }

    // --- Themes ---
    public GameTheme getCurrentTheme() {
        return GameTheme.fromName(prefs.getString(KEY_CURRENT_THEME, GameTheme.DEFAULT.name()));
    }

    public void setCurrentTheme(GameTheme theme) {
        if (theme == null) return;
        prefs.edit().putString(KEY_CURRENT_THEME, theme.name()).apply();
    }

    public Set<String> getUnlockedThemeNames() {
        Set<String> stored = prefs.getStringSet(KEY_UNLOCKED_THEMES, null);
        Set<String> copy = stored == null ? new HashSet<>() : new HashSet<>(stored);
        copy.add(GameTheme.DEFAULT.name());
        return copy;
    }

    public boolean isThemeUnlocked(GameTheme theme) {
        if (theme == null) return false;
        if (GameTheme.DEFAULT.equals(theme)) return true;
        Set<String> unlocked = getUnlockedThemeNames();
        if (unlocked.contains(theme.name())) return true;
        String primaryId = theme.displayName.replace(' ', '_');
        return theme.name().equals(primaryId) && unlocked.contains(theme.displayName);
    }

    public void unlockTheme(GameTheme theme) {
        Set<String> themes = getUnlockedThemeNames();
        themes.add(theme.name());
        prefs.edit().putStringSet(KEY_UNLOCKED_THEMES, themes).apply();
    }

    // --- Settings ---
    public boolean isColorblindMode() {
        return prefs.getBoolean(KEY_COLORBLIND, false);
    }

    public void setColorblindMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_COLORBLIND, enabled).apply();
    }

    public boolean isPerformanceMode() {
        return prefs.getBoolean(KEY_PERFORMANCE, false);
    }

    public void setPerformanceMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_PERFORMANCE, enabled).apply();
    }

    public boolean isMusicEnabled() {
        return prefs.getBoolean(KEY_MUSIC, true);
    }

    public void setMusicEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MUSIC, enabled).apply();
    }

    public boolean isSfxEnabled() {
        return prefs.getBoolean(KEY_SFX, true);
    }

    public void setSfxEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SFX, enabled).apply();
    }

    // --- Daily Rewards ---
    public int getCurrentDailyRewardDay() {
        int streak = prefs.getInt(KEY_DAILY_REWARD_STREAK, 0);
        if (streak < 0 || streak >= 7) {
            return 1;
        }
        return streak + 1;
    }

    public boolean canClaimDailyRewardToday() {
        String last = prefs.getString(KEY_DAILY_REWARD_LAST_DATE, "");
        return !todayDateKey().equals(last);
    }

    public int claimDailyReward() {
        if (!canClaimDailyRewardToday()) return -1;
        int day = getCurrentDailyRewardDay();
        int nextStreak = (day >= 7) ? 0 : day;
        prefs.edit()
                .putString(KEY_DAILY_REWARD_LAST_DATE, todayDateKey())
                .putInt(KEY_DAILY_REWARD_STREAK, nextStreak)
                .apply();
        return day;
    }

    // --- Lucky Spin ---
    public boolean canUseFreeSpin() {
        String lastDate = prefs.getString(KEY_LAST_FREE_SPIN_DATE, "");
        long lastTs = prefs.getLong(KEY_LAST_FREE_SPIN_TS, 0L);
        long now = System.currentTimeMillis();
        if (todayDateKey().equals(lastDate)) {
            return false;
        }
        return (now - lastTs) >= FREE_SPIN_COOLDOWN_MS || lastTs == 0L;
    }

    public long getRemainingFreeSpinCooldownMs() {
        if (canUseFreeSpin()) return 0L;
        long lastTs = prefs.getLong(KEY_LAST_FREE_SPIN_TS, 0L);
        long remaining = FREE_SPIN_COOLDOWN_MS - (System.currentTimeMillis() - lastTs);
        return Math.max(0L, remaining);
    }

    public void recordFreeSpinUsed() {
        prefs.edit()
                .putString(KEY_LAST_FREE_SPIN_DATE, todayDateKey())
                .putLong(KEY_LAST_FREE_SPIN_TS, System.currentTimeMillis())
                .apply();
    }

    // --- Achievements ---
    public static List<Achievement> getAllAchievements() {
        List<Achievement> list = new ArrayList<>();
        list.add(new Achievement("first_steps", "First Steps", "Complete your first level", 15));
        list.add(new Achievement("word_hunter", "Word Hunter", "Find 25 hidden words", 20));
        list.add(new Achievement("puzzle_master", "Puzzle Master", "Complete 5 levels", 25));
        list.add(new Achievement("word_wizard", "Word Wizard", "Find 100 hidden words", 30));
        list.add(new Achievement("brainiac", "Brainiac", "Find 200 hidden words", 40));
        list.add(new Achievement("speed_runner", "Speed Runner", "Beat a Hard mode level before time runs out", 50));
        list.add(new Achievement("puzzle_champion", "Puzzle Champion", "Complete 25 levels", 60));
        list.add(new Achievement("letter_legend", "Letter Legend", "Find 500 hidden words", 75));
        list.add(new Achievement("vocabulary_king", "Vocabulary King", "Find 1,000 hidden words", 100));
        list.add(new Achievement("ultimate_solver", "Ultimate Solver", "Complete 50 levels", 120));
        list.add(new Achievement("ultimate_champion", "Ultimate Champion", "Complete 100 levels", 200));
        list.add(new Achievement("level_10", "Level 10 Complete", "Complete 10 levels", 30));
        list.add(new Achievement("level_20", "Level 20 Complete", "Complete 20 levels", 40));
        list.add(new Achievement("level_30", "Level 30 Complete", "Complete 30 levels", 50));
        list.add(new Achievement("level_40", "Level 40 Complete", "Complete 40 levels", 60));
        list.add(new Achievement("level_50", "Level 50 Complete", "Complete 50 levels", 70));
        list.add(new Achievement("level_60", "Level 60 Complete", "Complete 60 levels", 80));
        list.add(new Achievement("level_70", "Level 70 Complete", "Complete 70 levels", 90));
        list.add(new Achievement("level_80", "Level 80 Complete", "Complete 80 levels", 100));
        list.add(new Achievement("level_90", "Level 90 Complete", "Complete 90 levels", 125));
        list.add(new Achievement("level_100", "Level 100 Complete", "Complete 100 levels", 150));
        list.add(new Achievement("daily_challenger", "Daily Challenger", "Complete a Daily Puzzle", 30));
        list.add(new Achievement("theme_collector", "Theme Collector", "Unlock 3 or more themes", 40));
        list.add(new Achievement("lucky_winner", "Lucky Winner", "Spin the Lucky Wheel", 20));
        return Collections.unmodifiableList(list);
    }

    public Set<String> getUnlockedAchievementIds() {
        Set<String> stored = prefs.getStringSet(KEY_UNLOCKED_ACHIEVEMENTS, null);
        return stored == null ? new HashSet<>() : new HashSet<>(stored);
    }

    public boolean isAchievementUnlocked(String id) {
        return getUnlockedAchievementIds().contains(id);
    }

    public List<Achievement> evaluateAndUnlockAchievements() {
        Set<String> unlocked = getUnlockedAchievementIds();
        List<Achievement> newlyUnlocked = new ArrayList<>();

        int levels = getLevelsCompletedCount();
        int words = getWordsFoundCount();
        int hard = getHardLevelsCompletedCount();
        int daily = getDailyCompletedCount();
        int themes = getUnlockedThemeNames().size();
        int spins = getSpinsCount();

        for (Achievement a : getAllAchievements()) {
            if (unlocked.contains(a.id)) continue;
            boolean met = false;
            switch (a.id) {
                case "first_steps": met = levels >= 1; break;
                case "word_hunter": met = words >= 25; break;
                case "puzzle_master": met = levels >= 5; break;
                case "word_wizard": met = words >= 100; break;
                case "brainiac": met = words >= 200; break;
                case "speed_runner": met = hard >= 1; break;
                case "puzzle_champion": met = levels >= 25; break;
                case "letter_legend": met = words >= 500; break;
                case "vocabulary_king": met = words >= 1000; break;
                case "ultimate_solver": met = levels >= 50; break;
                case "ultimate_champion": met = levels >= 100; break;
                case "level_10": met = levels >= 10; break;
                case "level_20": met = levels >= 20; break;
                case "level_30": met = levels >= 30; break;
                case "level_40": met = levels >= 40; break;
                case "level_50": met = levels >= 50; break;
                case "level_60": met = levels >= 60; break;
                case "level_70": met = levels >= 70; break;
                case "level_80": met = levels >= 80; break;
                case "level_90": met = levels >= 90; break;
                case "level_100": met = levels >= 100; break;
                case "daily_challenger": met = daily >= 1; break;
                case "theme_collector": met = themes >= 3; break;
                case "lucky_winner": met = spins >= 1; break;
            }
            if (met) {
                unlocked.add(a.id);
                newlyUnlocked.add(a);
                addCoins(a.rewardCoins);
            }
        }
        if (!newlyUnlocked.isEmpty()) {
            prefs.edit().putStringSet(KEY_UNLOCKED_ACHIEVEMENTS, unlocked).apply();
        }
        return newlyUnlocked;
    }

    // --- Reset & Clear Storage ---
    /**
     * Resets level progression ONLY, preserving coins, hints, purchased themes, and settings.
     */
    public void resetLevelProgressPreserveEconomy() {
        SharedPreferences.Editor editor = prefs.edit();
        for (Difficulty d : new Difficulty[]{Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD}) {
            for (Category c : Category.values()) {
                for (int lvl = 1; lvl <= 100; lvl++) {
                    editor.remove(levelKey(d, c, lvl));
                    editor.remove(levelStateKey(d, c, lvl));
                }
            }
        }
        editor.remove(levelStateKey(Difficulty.DAILY, Category.GENERAL, 1));
        editor.remove(KEY_LEVELS_COMPLETED);
        editor.remove(KEY_HARD_COMPLETED);
        editor.remove(KEY_DAILY_PUZZLE_DATE);
        editor.remove(KEY_TEST_ALL_LEVELS_UNLOCKED);
        editor.apply();
    }

    /**
     * Clears all local storage completely and restores fresh defaults.
     */
    public void clearAllLocalStorage() {
        prefs.edit().clear().apply();
        ensureDefaults();
        notifyEconomyChanged();
    }
}

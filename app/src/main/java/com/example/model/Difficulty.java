package com.example.model;

public enum Difficulty {
    EASY("Easy", "easy", 10, true, 0),
    NORMAL("Normal", "normal", 20, false, 0),
    HARD("Hard", "hard", 35, false, 120),
    DAILY("Daily", "daily", 50, false, 0);

    public final String displayName;
    public final String keyPrefix;
    public final int rewardCoins;
    public final boolean hintsAllowed;
    public final int timeLimitSeconds;

    Difficulty(String displayName, String keyPrefix, int rewardCoins, boolean hintsAllowed, int timeLimitSeconds) {
        this.displayName = displayName;
        this.keyPrefix = keyPrefix;
        this.rewardCoins = rewardCoins;
        this.hintsAllowed = hintsAllowed;
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public static Difficulty fromKey(String key) {
        if (key == null) return EASY;
        for (Difficulty d : values()) {
            if (d.keyPrefix.equalsIgnoreCase(key) || d.name().equalsIgnoreCase(key)) {
                return d;
            }
        }
        return EASY;
    }
}

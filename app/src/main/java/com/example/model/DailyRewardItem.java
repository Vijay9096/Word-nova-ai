package com.example.model;

public class DailyRewardItem {
    public final int day;
    public final int coins;
    public final int hints;
    public final String subtitle;
    public final boolean isBonusChest;

    public DailyRewardItem(int day, int coins, int hints, String subtitle, boolean isBonusChest) {
        this.day = day;
        this.coins = coins;
        this.hints = hints;
        this.subtitle = subtitle;
        this.isBonusChest = isBonusChest;
    }
}

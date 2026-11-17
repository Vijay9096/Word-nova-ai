package com.word.nova.model;

public class Achievement {
    public final String id;
    public final String title;
    public final String description;
    public final int rewardCoins;

    public Achievement(String id, String title, String description, int rewardCoins) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.rewardCoins = rewardCoins;
    }
}

package com.word.nova.model;

import android.graphics.Color;

public class SpinSegment {
    public final String label;
    public final boolean isHint;
    public final int amount;
    public final int color;

    public SpinSegment(String label, boolean isHint, int amount, String colorHex) {
        this.label = label;
        this.isHint = isHint;
        this.amount = amount;
        this.color = Color.parseColor(colorHex);
    }
}

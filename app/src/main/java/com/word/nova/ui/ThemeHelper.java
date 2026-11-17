package com.word.nova.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.word.nova.model.GameTheme;

public class ThemeHelper {

    public static int dp(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    public static int contrastTextOn(int backgroundColor) {
        return ColorUtils.calculateLuminance(backgroundColor) > 0.52
                ? Color.parseColor("#111827")
                : Color.WHITE;
    }

    public static void styleButton(Button btn, int fillColor, int textColor, int cornerDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(fillColor);
        shape.setCornerRadius(dp(btn.getContext(), cornerDp));

        RippleDrawable ripple = new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#44FFFFFF")),
                shape,
                null
        );
        btn.setBackground(ripple);
        btn.setTextColor(textColor);
        btn.setAllCaps(false);
        btn.setElevation(dp(btn.getContext(), 4));
    }

    public static void stylePrimaryButton(Button btn, GameTheme theme) {
        styleButton(btn, theme.primaryButtonColor, contrastTextOn(theme.primaryButtonColor), 12);
    }

    public static void styleCard(View view, int fillColor, int cornerDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(fillColor);
        shape.setCornerRadius(dp(view.getContext(), cornerDp));
        view.setBackground(shape);
    }

    public static void styleOutlinedCard(View view, int fillColor, int strokeColor, int strokeDp, int cornerDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(fillColor);
        shape.setStroke(dp(view.getContext(), strokeDp), strokeColor);
        shape.setCornerRadius(dp(view.getContext(), cornerDp));
        view.setBackground(shape);
    }

    public static void applyRootTheme(View root, GameTheme theme) {
        if (root != null) {
            root.setBackgroundColor(theme.backgroundColor);
        }
    }

    public static void styleHeaderText(TextView tv, GameTheme theme) {
        if (tv != null) {
            tv.setTextColor(theme.textPrimaryColor);
        }
    }

    public static void styleSecondaryText(TextView tv, GameTheme theme) {
        if (tv != null) {
            tv.setTextColor(theme.textSecondaryColor);
        }
    }
}

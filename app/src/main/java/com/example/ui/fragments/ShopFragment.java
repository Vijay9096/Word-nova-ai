package com.example.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.MainActivity;
import com.example.R;
import com.example.data.GamePrefs;
import com.example.model.GameTheme;
import com.example.ui.GamePopupDialog;
import com.example.ui.ThemeHelper;

import java.util.Random;

public class ShopFragment extends Fragment {

    private TextView tvCoins;
    private TextView tvHints;
    private GridLayout gridThemes;
    private final Random random = new Random();
    private GamePrefs prefsRef;
    private GamePrefs.OnEconomyChangeListener economyListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GamePrefs prefs = act.getGamePrefs();
        GameTheme theme = prefs.getCurrentTheme();

        this.prefsRef = prefs;
        applyThemeStyling(view, theme);

        tvCoins = view.findViewById(R.id.tv_shop_coins);
        tvHints = view.findViewById(R.id.tv_shop_hints);
        gridThemes = view.findViewById(R.id.grid_shop_themes);

        economyListener = (coins, hints) -> {
            if (isAdded()) {
                refreshHeader(prefs);
            }
        };
        prefs.addEconomyChangeListener(economyListener);

        refreshHeader(prefs);

        // Buy Hints
        Button bH1 = view.findViewById(R.id.btn_buy_hint_1);
        Button bH5 = view.findViewById(R.id.btn_buy_hint_5);
        Button bH10 = view.findViewById(R.id.btn_buy_hint_10);
        ThemeHelper.stylePrimaryButton(bH1, theme);
        ThemeHelper.stylePrimaryButton(bH5, theme);
        ThemeHelper.stylePrimaryButton(bH10, theme);

        bH1.setOnClickListener(v -> buyHints(act, prefs, 1, 40));
        bH5.setOnClickListener(v -> buyHints(act, prefs, 5, 180));
        bH10.setOnClickListener(v -> buyHints(act, prefs, 10, 306));

        // Bonus Chests
        Button bCommon = view.findViewById(R.id.btn_chest_common);
        Button bRare = view.findViewById(R.id.btn_chest_rare);
        Button bLeg = view.findViewById(R.id.btn_chest_legendary);
        ThemeHelper.styleButton(bCommon, Color.parseColor("#22C55E"), Color.WHITE, 12);
        ThemeHelper.styleButton(bRare, Color.parseColor("#42A5F5"), Color.WHITE, 12);
        ThemeHelper.styleButton(bLeg, Color.parseColor("#FBBF24"), Color.parseColor("#1F2937"), 12);

        bCommon.setOnClickListener(v -> openChest(act, prefs, "Common Chest", 100, 50, 160, 1, 2));
        bRare.setOnClickListener(v -> openChest(act, prefs, "Rare Chest", 250, 150, 380, 2, 5));
        bLeg.setOnClickListener(v -> openChest(act, prefs, "Legendary Chest", 500, 380, 850, 5, 10));

        Button btnBack = view.findViewById(R.id.btn_back);
        ThemeHelper.stylePrimaryButton(btnBack, theme);
        btnBack.setOnClickListener(v -> act.navigateBack());

        populateThemesGrid(act, prefs, view);
    }

    private void applyThemeStyling(View view, GameTheme theme) {
        ThemeHelper.applyRootTheme(view.findViewById(R.id.shop_root), theme);
        ((TextView) view.findViewById(R.id.tv_shop_title)).setTextColor(theme.primaryButtonColor);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_shop_coins), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_shop_hints), theme);
        ThemeHelper.styleCard(view.findViewById(R.id.card_shop_hints), theme.surfaceColor, 16);
        ThemeHelper.styleCard(view.findViewById(R.id.card_shop_chests), theme.surfaceColor, 16);
        ThemeHelper.styleCard(view.findViewById(R.id.card_shop_themes), theme.surfaceColor, 16);
    }

    private void refreshHeader(GamePrefs prefs) {
        tvCoins.setText("Coins: " + prefs.getCoins());
        tvHints.setText("Hints: " + prefs.getHints());
    }

    private void buyHints(MainActivity act, GamePrefs prefs, int count, int cost) {
        if (!prefs.spendCoins(cost)) {
            act.getSoundManager().playError();
            Toast.makeText(requireContext(),
                    "Insufficient coins! Need " + cost + " ⭐", Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.addHints(count);
        act.getSoundManager().playLevelComplete();
        refreshHeader(prefs);
        Toast.makeText(requireContext(),
                "Purchased +" + count + " 💡 Hint(s)!", Toast.LENGTH_SHORT).show();
    }

    private void openChest(MainActivity act, GamePrefs prefs, String name, int cost,
                           int minCoins, int maxCoins, int minHints, int maxHints) {
        if (!prefs.spendCoins(cost)) {
            act.getSoundManager().playError();
            Toast.makeText(requireContext(),
                    "Need " + cost + " ⭐ coins to open " + name + "!", Toast.LENGTH_SHORT).show();
            return;
        }
        int coinsWon = minCoins + random.nextInt(maxCoins - minCoins + 1);
        int hintsWon = minHints + random.nextInt(maxHints - minHints + 1);
        prefs.addCoins(coinsWon);
        prefs.addHints(hintsWon);
        act.getSoundManager().playLevelComplete();
        refreshHeader(prefs);

        new GamePopupDialog.Builder(requireContext(), prefs.getCurrentTheme())
                .setTitle("💎 " + name + " Opened!")
                .setMessage("You received:\n• +" + coinsWon + " ⭐ Coins\n• +" + hintsWon + " 💡 Hints")
                .setPositiveButton("Collect", null)
                .show();
    }

    private void populateThemesGrid(MainActivity act, GamePrefs prefs, View rootView) {
        gridThemes.removeAllViews();
        GameTheme active = prefs.getCurrentTheme();
        int margin = ThemeHelper.dp(requireContext(), 5);

        for (GameTheme t : GameTheme.values()) {
            if (GameTheme.DEFAULT.equals(t)) continue;
            Button btn = new Button(requireContext());
            btn.setTextSize(14);
            btn.setTypeface(btn.getTypeface(), android.graphics.Typeface.BOLD);
            btn.setMinimumHeight(ThemeHelper.dp(requireContext(), 50));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(margin, margin, margin, margin);
            btn.setLayoutParams(lp);

            boolean unlocked = prefs.isThemeUnlocked(t);
            if (t.equals(active)) {
                btn.setText("✅ " + t.displayName);
                ThemeHelper.styleButton(btn, Color.parseColor("#22C55E"), Color.WHITE, 12);
            } else if (unlocked) {
                btn.setText("✓ " + t.displayName + " (Unlocked)");
                ThemeHelper.stylePrimaryButton(btn, active);
            } else {
                btn.setText(t.displayName + "–" + t.price + "⭐");
                ThemeHelper.stylePrimaryButton(btn, active);
            }

            btn.setOnClickListener(v -> {
                if (prefs.isThemeUnlocked(t)) {
                    act.getSoundManager().playClick();
                    prefs.setCurrentTheme(t);
                    act.applyGlobalTheme();
                    applyThemeStyling(rootView, t);
                    populateThemesGrid(act, prefs, rootView);
                    Toast.makeText(requireContext(), t.displayName + " equipped!", Toast.LENGTH_SHORT).show();
                } else if (prefs.spendCoins(t.price)) {
                    prefs.unlockTheme(t);
                    prefs.setCurrentTheme(t);
                    act.getSoundManager().playLevelComplete();
                    act.applyGlobalTheme();
                    act.checkAndNotifyAchievements();
                    refreshHeader(prefs);
                    applyThemeStyling(rootView, t);
                    populateThemesGrid(act, prefs, rootView);
                    Toast.makeText(requireContext(), t.displayName + " unlocked & equipped!", Toast.LENGTH_SHORT).show();
                } else {
                    act.getSoundManager().playError();
                    Toast.makeText(requireContext(),
                            "Insufficient coins! Need " + t.price + " ⭐", Toast.LENGTH_SHORT).show();
                }
            });

            gridThemes.addView(btn);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (prefsRef != null && economyListener != null) {
            prefsRef.removeEconomyChangeListener(economyListener);
        }
    }
}

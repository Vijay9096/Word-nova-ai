package com.example.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.MainActivity;
import com.example.R;
import com.example.data.GamePrefs;
import com.example.model.GameTheme;
import com.example.ui.GamePopupDialog;
import com.example.ui.ThemeHelper;

public class ProfileFragment extends Fragment {

    private TextView tvName;
    private TextView tvCoins;
    private GamePrefs prefsRef;
    private GamePrefs.OnEconomyChangeListener economyListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GamePrefs prefs = act.getGamePrefs();
        GameTheme theme = prefs.getCurrentTheme();

        this.prefsRef = prefs;
        ThemeHelper.applyRootTheme(view.findViewById(R.id.profile_root), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_profile_title), theme);
        ThemeHelper.styleCard(view.findViewById(R.id.card_profile), theme.surfaceColor, 16);

        tvName = view.findViewById(R.id.tv_profile_name);
        TextView tvLevels = view.findViewById(R.id.tv_profile_levels);
        TextView tvWords = view.findViewById(R.id.tv_profile_words);
        TextView tvAch = view.findViewById(R.id.tv_profile_achievements);
        TextView tvTheme = view.findViewById(R.id.tv_profile_theme);
        tvCoins = view.findViewById(R.id.tv_profile_coins);

        TextView[] labels = {tvName, tvLevels, tvWords, tvAch, tvTheme, tvCoins};
        for (TextView tv : labels) {
            ThemeHelper.styleHeaderText(tv, theme);
        }

        tvName.setText("👦 Name: " + prefs.getPlayerName());
        tvLevels.setText("🏁 Levels Completed: " + prefs.getLevelsCompletedCount());
        tvWords.setText("🔤 Words Found: " + prefs.getWordsFoundCount());
        tvAch.setText("🏆 Achievements Unlocked: " + prefs.getUnlockedAchievementIds().size());
        tvTheme.setText("🎨 Current Theme: " + theme.displayName);
        tvCoins.setText("Coins: " + prefs.getCoins());

        economyListener = (coins, hints) -> {
            if (isAdded() && tvCoins != null) {
                tvCoins.setText("Coins: " + coins);
            }
        };
        prefs.addEconomyChangeListener(economyListener);

        tvName.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            new GamePopupDialog.Builder(requireContext(), theme)
                    .setTitle("Edit Player Name")
                    .setInput(prefs.getPlayerName(), entered -> {
                        prefs.setPlayerName(entered);
                        tvName.setText("👦 Name: " + prefs.getPlayerName());
                    })
                    .setPositiveButton("Save", null)
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        Button btnBack = view.findViewById(R.id.btn_back);
        ThemeHelper.stylePrimaryButton(btnBack, theme);
        btnBack.setOnClickListener(v -> act.navigateBack());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (prefsRef != null && economyListener != null) {
            prefsRef.removeEconomyChangeListener(economyListener);
        }
    }
}

package com.word.nova.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.word.nova.MainActivity;
import com.word.nova.R;
import com.word.nova.data.GamePrefs;
import com.word.nova.model.GameTheme;
import com.word.nova.ui.ThemeHelper;

public class SettingsFragment extends Fragment {

    private Button btnColorblind;
    private Button btnPerformance;
    private Button btnMusic;
    private Button btnSfx;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GamePrefs prefs = act.getGamePrefs();
        GameTheme theme = prefs.getCurrentTheme();

        ThemeHelper.applyRootTheme(view.findViewById(R.id.settings_root), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_settings_title), theme);

        Button btnThemes = view.findViewById(R.id.btn_themes);
        btnColorblind = view.findViewById(R.id.btn_colorblind);
        btnPerformance = view.findViewById(R.id.btn_performance);
        btnMusic = view.findViewById(R.id.btn_music);
        btnSfx = view.findViewById(R.id.btn_sfx);
        Button btnAbout = view.findViewById(R.id.btn_about);
        Button btnBack = view.findViewById(R.id.btn_back);

        ThemeHelper.stylePrimaryButton(btnThemes, theme);
        ThemeHelper.stylePrimaryButton(btnColorblind, theme);
        ThemeHelper.stylePrimaryButton(btnPerformance, theme);
        ThemeHelper.stylePrimaryButton(btnMusic, theme);
        ThemeHelper.stylePrimaryButton(btnSfx, theme);
        ThemeHelper.stylePrimaryButton(btnAbout, theme);
        ThemeHelper.stylePrimaryButton(btnBack, theme);

        refreshLabels(prefs);

        btnThemes.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            act.navigateTo(new ThemesFragment(), true);
        });

        btnColorblind.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            prefs.setColorblindMode(!prefs.isColorblindMode());
            refreshLabels(prefs);
        });

        btnPerformance.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            prefs.setPerformanceMode(!prefs.isPerformanceMode());
            refreshLabels(prefs);
        });

        btnMusic.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            prefs.setMusicEnabled(!prefs.isMusicEnabled());
            act.getSoundManager().refreshMusicState();
            refreshLabels(prefs);
        });

        btnSfx.setOnClickListener(v -> {
            prefs.setSfxEnabled(!prefs.isSfxEnabled());
            if (prefs.isSfxEnabled()) act.getSoundManager().playClick();
            refreshLabels(prefs);
        });

        btnAbout.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            act.navigateTo(new AboutFragment(), true);
        });

        btnBack.setOnClickListener(v -> act.navigateBack());
    }

    private void refreshLabels(GamePrefs prefs) {
        btnColorblind.setText("🎨 Colorblind: " + (prefs.isColorblindMode() ? "ON" : "OFF"));
        btnPerformance.setText("⚡ Performance: " + (prefs.isPerformanceMode() ? "ON" : "OFF"));
        btnMusic.setText("🎹 Music: " + (prefs.isMusicEnabled() ? "ON" : "OFF"));
        btnSfx.setText("🔊 Sound Effects: " + (prefs.isSfxEnabled() ? "ON" : "OFF"));
    }
}

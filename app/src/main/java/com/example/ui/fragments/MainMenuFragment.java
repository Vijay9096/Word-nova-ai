package com.example.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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

public class MainMenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GamePrefs prefs = act.getGamePrefs();
        GameTheme theme = prefs.getCurrentTheme();

        ThemeHelper.applyRootTheme(view.findViewById(R.id.main_menu_root), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_main_title), theme);

        ImageButton btnMenu = view.findViewById(R.id.btn_hamburger);
        btnMenu.setOnClickListener(v -> act.openSideMenu());

        Button btnPlay = view.findViewById(R.id.btn_play);
        Button btnClear = view.findViewById(R.id.btn_clear_storage);
        Button btnReset = view.findViewById(R.id.btn_reset_progress);

        ThemeHelper.stylePrimaryButton(btnPlay, theme);
        ThemeHelper.stylePrimaryButton(btnClear, theme);
        ThemeHelper.stylePrimaryButton(btnReset, theme);

        btnPlay.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            act.navigateTo(new DifficultyFragment(), true);
        });

        btnClear.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            new GamePopupDialog.Builder(requireContext(), prefs.getCurrentTheme())
                    .setTitle("Clear Local Storage")
                    .setMessage("This will erase ALL saved data including coins, hints, themes, achievements, and level progress. Continue?")
                    .setPositiveButton("Clear All", () -> {
                        prefs.clearAllLocalStorage();
                        act.applyGlobalTheme();
                        act.navigateTo(new MainMenuFragment(), false);
                        Toast.makeText(requireContext(), "All local storage cleared.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnReset.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            new GamePopupDialog.Builder(requireContext(), prefs.getCurrentTheme())
                    .setTitle("Reset Progress")
                    .setMessage("Reset all completed levels back to Level 1? Your coins, hints, and purchased themes will be preserved.")
                    .setPositiveButton("Reset", () -> {
                        prefs.resetLevelProgressPreserveEconomy();
                        Toast.makeText(requireContext(), "Level progress reset. Coins & hints preserved!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}

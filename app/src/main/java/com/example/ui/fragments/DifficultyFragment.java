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
import com.example.model.Difficulty;
import com.example.model.GameTheme;
import com.example.ui.ThemeHelper;

public class DifficultyFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_difficulty, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GameTheme theme = act.getGamePrefs().getCurrentTheme();

        ThemeHelper.applyRootTheme(view.findViewById(R.id.difficulty_root), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_difficulty_title), theme);

        view.findViewById(R.id.btn_hamburger).setOnClickListener(v -> act.openSideMenu());

        Button btnEasy = view.findViewById(R.id.btn_diff_easy);
        Button btnNormal = view.findViewById(R.id.btn_diff_normal);
        Button btnHard = view.findViewById(R.id.btn_diff_hard);
        Button btnBack = view.findViewById(R.id.btn_back);

        ThemeHelper.stylePrimaryButton(btnEasy, theme);
        ThemeHelper.stylePrimaryButton(btnNormal, theme);
        ThemeHelper.stylePrimaryButton(btnHard, theme);
        ThemeHelper.stylePrimaryButton(btnBack, theme);

        btnEasy.setOnClickListener(v -> openCategory(act, Difficulty.EASY));
        btnNormal.setOnClickListener(v -> openCategory(act, Difficulty.NORMAL));
        btnHard.setOnClickListener(v -> openCategory(act, Difficulty.HARD));
        btnBack.setOnClickListener(v -> act.navigateBack());
    }

    private void openCategory(MainActivity act, Difficulty difficulty) {
        act.getSoundManager().playClick();
        act.navigateTo(CategoryFragment.newInstance(difficulty), true);
    }
}

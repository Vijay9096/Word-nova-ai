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
import com.word.nova.model.Category;
import com.word.nova.model.Difficulty;
import com.word.nova.model.GameTheme;
import com.word.nova.ui.ThemeHelper;

public class DailyPuzzleIntroFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_intro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GamePrefs prefs = act.getGamePrefs();
        GameTheme theme = prefs.getCurrentTheme();

        ThemeHelper.applyRootTheme(view.findViewById(R.id.daily_intro_root), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_daily_title), theme);

        TextView tvDate = view.findViewById(R.id.tv_daily_date);
        TextView tvStatus = view.findViewById(R.id.tv_daily_status);
        ThemeHelper.styleHeaderText(tvDate, theme);
        ThemeHelper.styleHeaderText(tvStatus, theme);

        tvDate.setText(GamePrefs.todayDisplayDate());
        boolean done = prefs.isDailyPuzzleCompletedToday();
        tvStatus.setText(done ? "✅ Today’s puzzle completed! Play again?" : "🕹️ Ready to play today’s puzzle!");

        Button btnPlay = view.findViewById(R.id.btn_play_now);
        Button btnBack = view.findViewById(R.id.btn_back);
        ThemeHelper.stylePrimaryButton(btnPlay, theme);
        ThemeHelper.stylePrimaryButton(btnBack, theme);

        btnPlay.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            act.navigateTo(GameFragment.newInstance(Difficulty.DAILY, Category.GENERAL, 1), true);
        });
        btnBack.setOnClickListener(v -> act.navigateBack());
    }
}

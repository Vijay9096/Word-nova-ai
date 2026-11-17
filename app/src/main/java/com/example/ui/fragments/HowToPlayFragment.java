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
import com.example.model.GameTheme;
import com.example.ui.ThemeHelper;

public class HowToPlayFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_how_to_play, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GameTheme theme = act.getGamePrefs().getCurrentTheme();

        ThemeHelper.applyRootTheme(view.findViewById(R.id.how_to_play_root), theme);
        ThemeHelper.styleCard(view.findViewById(R.id.card_how_to_play), theme.surfaceColor, 16);

        TextView tvTitle = view.findViewById(R.id.tv_how_title);
        tvTitle.setTextColor(theme.primaryButtonColor);

        int[] headerIds = {R.id.tv_goal_header, R.id.tv_controls_header, R.id.tv_rewards_header, R.id.tv_bonus_header};
        for (int id : headerIds) {
            ((TextView) view.findViewById(id)).setTextColor(theme.primaryButtonColor);
        }

        int[] bodyIds = {R.id.tv_goal_body, R.id.tv_controls_body, R.id.tv_rewards_body, R.id.tv_bonus_body};
        for (int id : bodyIds) {
            ThemeHelper.styleHeaderText((TextView) view.findViewById(id), theme);
        }

        view.findViewById(R.id.btn_hamburger).setOnClickListener(v -> act.openSideMenu());

        Button btnBack = view.findViewById(R.id.btn_back);
        ThemeHelper.stylePrimaryButton(btnBack, theme);
        btnBack.setOnClickListener(v -> act.navigateBack());
    }
}

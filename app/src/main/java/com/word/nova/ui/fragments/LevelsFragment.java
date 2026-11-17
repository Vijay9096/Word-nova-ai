package com.word.nova.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.word.nova.MainActivity;
import com.word.nova.R;
import com.word.nova.data.GamePrefs;
import com.word.nova.model.Category;
import com.word.nova.model.Difficulty;
import com.word.nova.model.GameTheme;
import com.word.nova.ui.ThemeHelper;

public class LevelsFragment extends Fragment {

    private static final String ARG_DIFFICULTY = "arg_difficulty";
    private static final String ARG_CATEGORY = "arg_category";

    private Difficulty difficulty;
    private Category category;
    private RecyclerView rvLevels;

    public static LevelsFragment newInstance(Difficulty difficulty, Category category) {
        LevelsFragment f = new LevelsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DIFFICULTY, difficulty.keyPrefix);
        args.putString(ARG_CATEGORY, category.displayName);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_levels, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GamePrefs prefs = act.getGamePrefs();
        GameTheme theme = prefs.getCurrentTheme();

        difficulty = Difficulty.fromKey(getArguments() != null ? getArguments().getString(ARG_DIFFICULTY) : "easy");
        category = Category.fromName(getArguments() != null ? getArguments().getString(ARG_CATEGORY) : "GENERAL");

        ThemeHelper.applyRootTheme(view.findViewById(R.id.levels_root), theme);
        TextView tvTitle = view.findViewById(R.id.tv_levels_title);
        tvTitle.setText(category.displayName + " Levels");
        ThemeHelper.styleHeaderText(tvTitle, theme);

        view.findViewById(R.id.btn_hamburger).setOnClickListener(v -> act.openSideMenu());

        Button btnBack = view.findViewById(R.id.btn_back);
        ThemeHelper.stylePrimaryButton(btnBack, theme);
        btnBack.setOnClickListener(v -> act.navigateBack());

        rvLevels = view.findViewById(R.id.rv_levels);
        rvLevels.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        bindAdapter(act, prefs, theme);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (rvLevels != null && rvLevels.getAdapter() != null) {
            rvLevels.getAdapter().notifyDataSetChanged();
        }
    }

    private void bindAdapter(MainActivity act, GamePrefs prefs, GameTheme theme) {
        final int completedColor = Color.parseColor("#22C55E");
        final int lockedBg = Color.parseColor("#3A3F54");
        final int lockedText = Color.parseColor("#7B8199");

        rvLevels.setAdapter(new RecyclerView.Adapter<LevelVH>() {
            @NonNull
            @Override
            public LevelVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View item = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_grid_button, parent, false);
                return new LevelVH(item);
            }

            @Override
            public void onBindViewHolder(@NonNull LevelVH holder, int position) {
                int level = position + 1;
                boolean completed = prefs.isLevelCompleted(difficulty, category, level);
                boolean unlocked = prefs.isLevelUnlocked(difficulty, category, level);

                holder.btn.setText("Level " + level);
                if (completed) {
                    ThemeHelper.styleButton(holder.btn, completedColor, Color.WHITE, 12);
                } else if (unlocked) {
                    ThemeHelper.styleButton(holder.btn, theme.primaryButtonColor, Color.WHITE, 12);
                } else {
                    ThemeHelper.styleButton(holder.btn, lockedBg, lockedText, 12);
                }

                holder.btn.setOnClickListener(v -> {
                    if (unlocked) {
                        act.getSoundManager().playClick();
                        act.navigateTo(GameFragment.newInstance(difficulty, category, level), true);
                    } else {
                        act.getSoundManager().playError();
                        Toast.makeText(requireContext(),
                                "Complete Level " + (level - 1) + " first!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return 100;
            }
        });
    }

    static class LevelVH extends RecyclerView.ViewHolder {
        final Button btn;
        LevelVH(View itemView) {
            super(itemView);
            btn = itemView.findViewById(R.id.btn_grid_item);
        }
    }
}

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.word.nova.MainActivity;
import com.word.nova.R;
import com.word.nova.data.GamePrefs;
import com.word.nova.model.Achievement;
import com.word.nova.model.GameTheme;
import com.word.nova.ui.ThemeHelper;

import java.util.List;

public class AchievementsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_achievements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GamePrefs prefs = act.getGamePrefs();
        GameTheme theme = prefs.getCurrentTheme();

        ThemeHelper.applyRootTheme(view.findViewById(R.id.achievements_root), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_achievements_title), theme);

        Button btnBack = view.findViewById(R.id.btn_back);
        ThemeHelper.stylePrimaryButton(btnBack, theme);
        btnBack.setOnClickListener(v -> act.navigateBack());

        List<Achievement> list = GamePrefs.getAllAchievements();
        RecyclerView rv = view.findViewById(R.id.rv_achievements);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new RecyclerView.Adapter<AchVH>() {
            @NonNull
            @Override
            public AchVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View item = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_achievement, parent, false);
                return new AchVH(item);
            }

            @Override
            public void onBindViewHolder(@NonNull AchVH holder, int position) {
                Achievement ach = list.get(position);
                boolean unlocked = prefs.isAchievementUnlocked(ach.id);
                if (unlocked) {
                    holder.tv.setText(ach.title + " — 🟢 UNLOCKED");
                    ThemeHelper.styleCard(holder.tv, theme.primaryButtonColor, 10);
                    holder.tv.setTextColor(Color.WHITE);
                } else {
                    holder.tv.setText(ach.title + " — 🔒 LOCKED");
                    ThemeHelper.styleCard(holder.tv, Color.parseColor("#2D4C6E"), 10);
                    holder.tv.setTextColor(Color.parseColor("#9DB2C9"));
                }
                holder.tv.setOnClickListener(v -> {
                    act.getSoundManager().playClick();
                    Toast.makeText(requireContext(),
                            ach.description + " (Reward: +" + ach.rewardCoins + "⭐)",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public int getItemCount() {
                return list.size();
            }
        });
    }

    static class AchVH extends RecyclerView.ViewHolder {
        final TextView tv;
        AchVH(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tv_achievement_row);
        }
    }
}

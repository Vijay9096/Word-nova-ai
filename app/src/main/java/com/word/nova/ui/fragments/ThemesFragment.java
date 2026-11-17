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
import com.word.nova.model.GameTheme;
import com.word.nova.ui.ThemeHelper;

public class ThemesFragment extends Fragment {

    private RecyclerView rvThemes;
    private GameTheme previewedLockedTheme = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_themes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GamePrefs prefs = act.getGamePrefs();
        GameTheme active = prefs.getCurrentTheme();
        previewedLockedTheme = null;

        applyLocalThemePreview(view, active);

        Button btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> act.navigateBack());

        rvThemes = view.findViewById(R.id.rv_themes);
        rvThemes.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        bindAdapter(act, prefs, view);
    }

    private void applyLocalThemePreview(View rootView, GameTheme theme) {
        ThemeHelper.applyRootTheme(rootView.findViewById(R.id.themes_root), theme);
        ThemeHelper.styleHeaderText((TextView) rootView.findViewById(R.id.tv_themes_title), theme);
        ThemeHelper.stylePrimaryButton((Button) rootView.findViewById(R.id.btn_back), theme);
    }

    private void bindAdapter(MainActivity act, GamePrefs prefs, View rootView) {
        GameTheme[] themes = GameTheme.values();
        rvThemes.setAdapter(new RecyclerView.Adapter<ThemeVH>() {
            @NonNull
            @Override
            public ThemeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View item = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_grid_button, parent, false);
                return new ThemeVH(item);
            }

            @Override
            public void onBindViewHolder(@NonNull ThemeVH holder, int position) {
                GameTheme itemTheme = themes[position];
                GameTheme current = prefs.getCurrentTheme();
                boolean unlocked = prefs.isThemeUnlocked(itemTheme);
                boolean isSelected = unlocked && itemTheme == current;
                boolean isPreviewing = !unlocked && itemTheme == previewedLockedTheme;

                if (isSelected) {
                    holder.btn.setText("✅ " + itemTheme.displayName);
                    ThemeHelper.styleButton(
                            holder.btn,
                            itemTheme.primaryButtonColor,
                            ThemeHelper.contrastTextOn(itemTheme.primaryButtonColor),
                            12
                    );
                } else if (unlocked) {
                    holder.btn.setText("✓ " + itemTheme.displayName);
                    ThemeHelper.styleButton(
                            holder.btn,
                            itemTheme.previewColor,
                            ThemeHelper.contrastTextOn(itemTheme.previewColor),
                            12
                    );
                } else if (isPreviewing) {
                    holder.btn.setText("👁 " + itemTheme.displayName + " (Locked)");
                    ThemeHelper.styleButton(
                            holder.btn,
                            itemTheme.primaryButtonColor,
                            ThemeHelper.contrastTextOn(itemTheme.primaryButtonColor),
                            12
                    );
                } else {
                    holder.btn.setText("🔒 " + itemTheme.displayName + " (Locked)");
                    ThemeHelper.styleButton(
                            holder.btn,
                            itemTheme.previewColor,
                            ThemeHelper.contrastTextOn(itemTheme.previewColor),
                            12
                    );
                }

                holder.btn.setOnClickListener(v -> {
                    if (prefs.isThemeUnlocked(itemTheme)) {
                        previewedLockedTheme = null;
                        act.getSoundManager().playClick();
                        prefs.setCurrentTheme(itemTheme);
                        act.applyGlobalTheme();
                        applyLocalThemePreview(rootView, itemTheme);
                        notifyDataSetChanged();
                        Toast.makeText(requireContext(), itemTheme.displayName + " theme selected!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Locked themes can only be previewed here; purchasing is exclusively in the Shop.
                        previewedLockedTheme = itemTheme;
                        act.getSoundManager().playClick();
                        applyLocalThemePreview(rootView, itemTheme);
                        notifyDataSetChanged();
                        Toast.makeText(requireContext(),
                                "Previewing " + itemTheme.displayName + " (Locked — unlock in Shop)",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return themes.length;
            }
        });
    }

    static class ThemeVH extends RecyclerView.ViewHolder {
        final Button btn;
        ThemeVH(View itemView) {
            super(itemView);
            btn = itemView.findViewById(R.id.btn_grid_item);
        }
    }
}

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.word.nova.MainActivity;
import com.word.nova.R;
import com.word.nova.model.Category;
import com.word.nova.model.Difficulty;
import com.word.nova.model.GameTheme;
import com.word.nova.ui.ThemeHelper;

public class CategoryFragment extends Fragment {

    private static final String ARG_DIFFICULTY = "arg_difficulty";

    public static CategoryFragment newInstance(Difficulty difficulty) {
        CategoryFragment f = new CategoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DIFFICULTY, difficulty.keyPrefix);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GameTheme theme = act.getGamePrefs().getCurrentTheme();
        Difficulty difficulty = Difficulty.fromKey(
                getArguments() != null ? getArguments().getString(ARG_DIFFICULTY) : "easy");

        ThemeHelper.applyRootTheme(view.findViewById(R.id.category_root), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_category_title), theme);

        view.findViewById(R.id.btn_hamburger).setOnClickListener(v -> act.openSideMenu());

        Button btnBack = view.findViewById(R.id.btn_back);
        ThemeHelper.stylePrimaryButton(btnBack, theme);
        btnBack.setOnClickListener(v -> act.navigateBack());

        RecyclerView rv = view.findViewById(R.id.rv_categories);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        Category[] categories = Category.values();
        rv.setAdapter(new RecyclerView.Adapter<CategoryVH>() {
            @NonNull
            @Override
            public CategoryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View item = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_grid_button, parent, false);
                return new CategoryVH(item);
            }

            @Override
            public void onBindViewHolder(@NonNull CategoryVH holder, int position) {
                Category cat = categories[position];
                holder.btn.setText(cat.displayName);
                ThemeHelper.stylePrimaryButton(holder.btn, theme);
                holder.btn.setOnClickListener(v -> {
                    act.getSoundManager().playClick();
                    act.navigateTo(LevelsFragment.newInstance(difficulty, cat), true);
                });
            }

            @Override
            public int getItemCount() {
                return categories.length;
            }
        });
    }

    static class CategoryVH extends RecyclerView.ViewHolder {
        final Button btn;
        CategoryVH(View itemView) {
            super(itemView);
            btn = itemView.findViewById(R.id.btn_grid_item);
        }
    }
}

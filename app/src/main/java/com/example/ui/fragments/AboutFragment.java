package com.example.ui.fragments;

import android.content.Intent;
import android.net.Uri;
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

import com.example.MainActivity;
import com.example.R;
import com.example.model.GameTheme;
import com.example.ui.GamePopupDialog;
import com.example.ui.ThemeHelper;

public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GameTheme theme = act.getGamePrefs().getCurrentTheme();

        ThemeHelper.applyRootTheme(view.findViewById(R.id.about_root), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_about_title), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_about_intro), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_about_dev), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_about_ver), theme);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_about_engine), theme);

        view.findViewById(R.id.btn_hamburger).setOnClickListener(v -> act.openSideMenu());

        Button btnRate = view.findViewById(R.id.btn_rate_game);
        Button btnFeedback = view.findViewById(R.id.btn_send_feedback);
        Button btnBack = view.findViewById(R.id.btn_back);

        ThemeHelper.stylePrimaryButton(btnRate, theme);
        ThemeHelper.stylePrimaryButton(btnFeedback, theme);
        ThemeHelper.stylePrimaryButton(btnBack, theme);

        btnRate.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            new GamePopupDialog.Builder(requireContext(), theme)
                    .setTitle("⭐ Rate Word Nova")
                    .setMessage("Thank you for playing Word Nova! We appreciate your support.")
                    .setPositiveButton("5 Stars!", () ->
                            Toast.makeText(requireContext(), "Thank you for rating Word Nova!", Toast.LENGTH_SHORT).show())
                    .setNegativeButton("Later", null)
                    .show();
        });

        btnFeedback.setOnClickListener(v -> {
            act.getSoundManager().playClick();
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@wordnova.app"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Word Nova Feedback");
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Feedback email: support@wordnova.app", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> act.navigateBack());
    }
}

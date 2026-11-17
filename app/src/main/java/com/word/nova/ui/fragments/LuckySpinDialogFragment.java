package com.word.nova.ui.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.word.nova.MainActivity;
import com.word.nova.R;
import com.word.nova.data.GamePrefs;
import com.word.nova.model.GameTheme;
import com.word.nova.model.SpinSegment;
import com.word.nova.ui.SpinWheelView;
import com.word.nova.ui.ThemeHelper;

import java.util.Locale;

public class LuckySpinDialogFragment extends DialogFragment {

    private static final int PAID_SPIN_COST = 30;

    private SpinWheelView wheelView;
    private Button btnFreeSpin;
    private Button btnBuySpin;
    private TextView tvResult;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_lucky_spin, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90f);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GamePrefs prefs = act.getGamePrefs();
        GameTheme theme = prefs.getCurrentTheme();

        ThemeHelper.styleCard(view.findViewById(R.id.lucky_spin_card), theme.surfaceColor, 18);
        ThemeHelper.styleHeaderText((TextView) view.findViewById(R.id.tv_spin_title), theme);

        wheelView = view.findViewById(R.id.spin_wheel_view);
        btnFreeSpin = view.findViewById(R.id.btn_free_spin);
        btnBuySpin = view.findViewById(R.id.btn_buy_spin);
        tvResult = view.findViewById(R.id.tv_spin_result);
        Button btnClose = view.findViewById(R.id.btn_close_spin);

        ThemeHelper.stylePrimaryButton(btnBuySpin, theme);
        ThemeHelper.stylePrimaryButton(btnClose, theme);
        refreshFreeSpinButton(prefs, theme);

        btnFreeSpin.setOnClickListener(v -> {
            if (wheelView.isSpinning()) return;
            if (!prefs.canUseFreeSpin()) {
                act.getSoundManager().playError();
                Toast.makeText(requireContext(),
                        "Free spin already used today! Buy a spin for 30⭐.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.recordFreeSpinUsed();
            refreshFreeSpinButton(prefs, theme);
            triggerSpin(act, prefs, theme);
        });

        btnBuySpin.setOnClickListener(v -> {
            if (wheelView.isSpinning()) return;
            if (!prefs.spendCoins(PAID_SPIN_COST)) {
                act.getSoundManager().playError();
                Toast.makeText(requireContext(),
                        "Not enough coins! Need 30⭐.", Toast.LENGTH_SHORT).show();
                return;
            }
            triggerSpin(act, prefs, theme);
        });

        btnClose.setOnClickListener(v -> {
            if (!wheelView.isSpinning()) {
                act.getSoundManager().playClick();
                dismiss();
            }
        });
    }

    private void refreshFreeSpinButton(GamePrefs prefs, GameTheme theme) {
        if (prefs.canUseFreeSpin()) {
            btnFreeSpin.setText("🎁 Free Spin");
            ThemeHelper.stylePrimaryButton(btnFreeSpin, theme);
        } else {
            long remMs = prefs.getRemainingFreeSpinCooldownMs();
            long hrs = remMs / (1000L * 60L * 60L);
            long mins = (remMs / (1000L * 60L)) % 60L;
            btnFreeSpin.setText(String.format(Locale.US, "⏳ %dh %02dm", hrs, mins));
            ThemeHelper.styleButton(btnFreeSpin, Color.parseColor("#3A3F54"), Color.parseColor("#9AA0B8"), 12);
        }
    }

    private void triggerSpin(MainActivity act, GamePrefs prefs, GameTheme theme) {
        act.getSoundManager().playClick();
        tvResult.setVisibility(View.GONE);
        wheelView.spin(prefs.isPerformanceMode(), seg -> {
            if (!isAdded()) return;
            if (seg.isHint) {
                prefs.addHints(seg.amount);
                tvResult.setText("🎉 Won +" + seg.amount + " 💡 Hints!");
            } else {
                prefs.addCoins(seg.amount);
                tvResult.setText("🎉 Won +" + seg.amount + " ⭐ Coins!");
            }
            tvResult.setVisibility(View.VISIBLE);
            prefs.incrementSpinsCount();
            act.getSoundManager().playLevelComplete();
            act.checkAndNotifyAchievements();
            refreshFreeSpinButton(prefs, theme);
        });
    }
}

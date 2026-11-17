package com.word.nova.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.word.nova.MainActivity;
import com.word.nova.R;
import com.word.nova.data.GamePrefs;
import com.word.nova.model.DailyRewardItem;
import com.word.nova.model.GameTheme;
import com.word.nova.ui.GamePopupDialog;
import com.word.nova.ui.ThemeHelper;

import java.util.Arrays;
import java.util.List;

public class DailyRewardsFragment extends Fragment {

    private static final List<DailyRewardItem> REWARD_ITEMS = Arrays.asList(
            new DailyRewardItem(1, 50, 0, "Claim to keep streak", false),
            new DailyRewardItem(2, 70, 1, "Small boost", false),
            new DailyRewardItem(3, 100, 0, "Nice!", false),
            new DailyRewardItem(4, 140, 4, "Lucky 140", false),
            new DailyRewardItem(5, 190, 0, "Keep it up", false),
            new DailyRewardItem(6, 250, 8, "Almost there", false),
            new DailyRewardItem(7, 320, 12, "Lucky Day 7! Your weekly treat!", false),
            new DailyRewardItem(7, 0, 0, "Unlocks Legendary Chest", true)
    );

    private GridLayout grid;
    private Button btnClaim;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_rewards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GamePrefs prefs = act.getGamePrefs();
        GameTheme theme = prefs.getCurrentTheme();

        ThemeHelper.applyRootTheme(view.findViewById(R.id.daily_rewards_root), theme);
        TextView tvTitle = view.findViewById(R.id.tv_daily_rewards_title);
        tvTitle.setTextColor(theme.primaryButtonColor);

        view.findViewById(R.id.btn_hamburger).setOnClickListener(v -> act.openSideMenu());

        grid = view.findViewById(R.id.grid_daily_rewards);
        btnClaim = view.findViewById(R.id.btn_claim_reward);
        Button btnBack = view.findViewById(R.id.btn_back);

        ThemeHelper.stylePrimaryButton(btnBack, theme);
        btnBack.setOnClickListener(v -> act.navigateBack());

        renderCards(prefs, theme);
        refreshClaimButton(act, prefs, theme);
    }

    private void renderCards(GamePrefs prefs, GameTheme theme) {
        grid.removeAllViews();
        int currentDay = prefs.getCurrentDailyRewardDay();
        boolean canClaim = prefs.canClaimDailyRewardToday();
        int margin = ThemeHelper.dp(requireContext(), 6);
        int pad = ThemeHelper.dp(requireContext(), 12);

        for (DailyRewardItem item : REWARD_ITEMS) {
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(pad, pad, pad, pad);
            card.setMinimumHeight(ThemeHelper.dp(requireContext(), 112));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(margin, margin, margin, margin);
            card.setLayoutParams(lp);

            boolean isDay7 = item.day == 7;
            boolean isCurrent = item.day == currentDay && canClaim;

            if (isDay7) {
                card.setBackgroundResource(R.drawable.bg_day7_gold);
            } else if (isCurrent) {
                ThemeHelper.styleOutlinedCard(card, theme.surfaceColor, theme.primaryButtonColor, 2, 12);
            } else {
                ThemeHelper.styleCard(card, theme.surfaceColor, 12);
            }

            TextView tvDay = new TextView(requireContext());
            tvDay.setText("Day " + item.day);
            tvDay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvDay.setTypeface(tvDay.getTypeface(), android.graphics.Typeface.BOLD);
            tvDay.setTextColor(theme.primaryButtonColor);
            tvDay.setGravity(Gravity.CENTER);

            TextView tvReward = new TextView(requireContext());
            if (item.isBonusChest) {
                tvReward.setText("Legendary Chest");
            } else if (item.hints > 0) {
                tvReward.setText(item.coins + " ⭐ +" + item.hints + " 💡");
            } else {
                tvReward.setText(item.coins + " ⭐");
            }
            tvReward.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvReward.setTypeface(tvReward.getTypeface(), android.graphics.Typeface.BOLD);
            tvReward.setTextColor(isDay7 ? Color.parseColor("#111827") : theme.textPrimaryColor);
            tvReward.setGravity(Gravity.CENTER);
            tvReward.setPadding(0, ThemeHelper.dp(requireContext(), 6), 0, ThemeHelper.dp(requireContext(), 6));

            TextView tvSub = new TextView(requireContext());
            tvSub.setText(item.subtitle);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvSub.setTextColor(isDay7 ? Color.parseColor("#1F2937") : theme.textSecondaryColor);
            tvSub.setGravity(Gravity.CENTER);

            card.addView(tvDay);
            card.addView(tvReward);
            card.addView(tvSub);
            grid.addView(card);
        }
    }

    private void refreshClaimButton(MainActivity act, GamePrefs prefs, GameTheme theme) {
        int day = prefs.getCurrentDailyRewardDay();
        boolean canClaim = prefs.canClaimDailyRewardToday();
        if (canClaim) {
            btnClaim.setText("Claim Day " + day + " Reward");
            ThemeHelper.stylePrimaryButton(btnClaim, theme);
        } else {
            btnClaim.setText("Already Claimed Today ✓");
            ThemeHelper.styleButton(btnClaim, Color.parseColor("#3A3F54"), Color.parseColor("#9AA0B8"), 12);
        }

        btnClaim.setOnClickListener(v -> {
            if (!prefs.canClaimDailyRewardToday()) {
                act.getSoundManager().playError();
                Toast.makeText(requireContext(),
                        "Today's reward was already claimed! Come back tomorrow.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            int claimedDay = prefs.claimDailyReward();
            if (claimedDay <= 0) return;

            act.getSoundManager().playLevelComplete();
            int coinsWon = 0;
            int hintsWon = 0;
            StringBuilder msg = new StringBuilder();

            for (DailyRewardItem item : REWARD_ITEMS) {
                if (item.day == claimedDay) {
                    coinsWon += item.coins;
                    hintsWon += item.hints;
                }
            }
            if (claimedDay == 7) {
                // Legendary chest bonus
                coinsWon += 350;
                hintsWon += 5;
                msg.append("Day 7 + Legendary Chest unlocked!\n");
            }
            prefs.addCoins(coinsWon);
            prefs.addHints(hintsWon);
            msg.append("Received +").append(coinsWon).append(" ⭐");
            if (hintsWon > 0) {
                msg.append(" and +").append(hintsWon).append(" 💡");
            }

            renderCards(prefs, theme);
            refreshClaimButton(act, prefs, theme);

            new GamePopupDialog.Builder(requireContext(), prefs.getCurrentTheme())
                    .setTitle("🎁 Day " + claimedDay + " Claimed!")
                    .setMessage(msg.toString())
                    .setPositiveButton("Awesome!", null)
                    .show();
        });
    }
}

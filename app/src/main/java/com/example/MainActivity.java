package com.example;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.audio.SoundManager;
import com.example.data.GamePrefs;
import com.example.model.Achievement;
import com.example.model.Category;
import com.example.model.Difficulty;
import com.example.model.GameTheme;
import com.example.ui.ThemeHelper;
import com.example.ui.fragments.AchievementsFragment;
import com.example.ui.fragments.DailyPuzzleIntroFragment;
import com.example.ui.fragments.DailyRewardsFragment;
import com.example.ui.fragments.HowToPlayFragment;
import com.example.ui.fragments.LuckySpinDialogFragment;
import com.example.ui.fragments.MainMenuFragment;
import com.example.ui.fragments.ProfileFragment;
import com.example.ui.fragments.SettingsFragment;
import com.example.ui.fragments.ShopFragment;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private GamePrefs gamePrefs;
    private SoundManager soundManager;
    private FrameLayout rootContainer;
    private View sideMenuOverlay;
    private View menuCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        gamePrefs = new GamePrefs(this);
        soundManager = new SoundManager(gamePrefs);

        rootContainer = findViewById(R.id.root_main_container);
        sideMenuOverlay = findViewById(R.id.side_menu_overlay);
        menuCard = findViewById(R.id.menu_card);

        ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        setupSideMenu();
        applyGlobalTheme();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MainMenuFragment())
                    .commit();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (sideMenuOverlay.getVisibility() == View.VISIBLE) {
                    closeSideMenu();
                    return;
                }
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupSideMenu() {
        sideMenuOverlay.setOnClickListener(v -> closeSideMenu());
        menuCard.setOnClickListener(v -> { /* consume clicks inside menu card */ });

        findViewById(R.id.menu_btn_settings).setOnClickListener(v -> {
            soundManager.playClick();
            closeSideMenu();
            navigateTo(new SettingsFragment(), true);
        });
        findViewById(R.id.menu_btn_achievements).setOnClickListener(v -> {
            soundManager.playClick();
            closeSideMenu();
            navigateTo(new AchievementsFragment(), true);
        });
        findViewById(R.id.menu_btn_profile).setOnClickListener(v -> {
            soundManager.playClick();
            closeSideMenu();
            navigateTo(new ProfileFragment(), true);
        });
        findViewById(R.id.menu_btn_daily_puzzle).setOnClickListener(v -> {
            soundManager.playClick();
            closeSideMenu();
            navigateTo(new DailyPuzzleIntroFragment(), true);
        });
        findViewById(R.id.menu_btn_how_to_play).setOnClickListener(v -> {
            soundManager.playClick();
            closeSideMenu();
            navigateTo(new HowToPlayFragment(), true);
        });
        findViewById(R.id.menu_btn_lucky_spin).setOnClickListener(v -> {
            soundManager.playClick();
            closeSideMenu();
            showLuckySpin();
        });
        findViewById(R.id.menu_btn_daily_rewards).setOnClickListener(v -> {
            soundManager.playClick();
            closeSideMenu();
            navigateTo(new DailyRewardsFragment(), true);
        });
        findViewById(R.id.menu_btn_shop).setOnClickListener(v -> {
            soundManager.playClick();
            closeSideMenu();
            navigateTo(new ShopFragment(), true);
        });
        findViewById(R.id.menu_btn_close).setOnClickListener(v -> {
            soundManager.playClick();
            closeSideMenu();
        });
    }

    public void openSideMenu() {
        soundManager.playClick();
        GameTheme theme = gamePrefs.getCurrentTheme();
        ThemeHelper.styleCard(menuCard, theme.boardColor, 16);
        int[] btnIds = {
                R.id.menu_btn_settings, R.id.menu_btn_achievements, R.id.menu_btn_profile,
                R.id.menu_btn_daily_puzzle, R.id.menu_btn_how_to_play, R.id.menu_btn_lucky_spin,
                R.id.menu_btn_daily_rewards, R.id.menu_btn_shop, R.id.menu_btn_close
        };
        for (int id : btnIds) {
            Button b = findViewById(id);
            ThemeHelper.stylePrimaryButton(b, theme);
        }
        sideMenuOverlay.setVisibility(View.VISIBLE);
    }

    public void closeSideMenu() {
        sideMenuOverlay.setVisibility(View.GONE);
    }

    public void showLuckySpin() {
        LuckySpinDialogFragment dialog = new LuckySpinDialogFragment();
        dialog.show(getSupportFragmentManager(), "lucky_spin");
    }

    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        var tx = getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            tx.addToBackStack(fragment.getClass().getSimpleName());
        }
        tx.commit();
    }

    public void navigateBack() {
        soundManager.playClick();
        if (sideMenuOverlay.getVisibility() == View.VISIBLE) {
            closeSideMenu();
            return;
        }
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }
    }

    public void applyGlobalTheme() {
        GameTheme theme = gamePrefs.getCurrentTheme();
        rootContainer.setBackgroundColor(theme.backgroundColor);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(theme.isLightTheme);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(theme.isLightTheme);
    }

    public void checkAndNotifyAchievements() {
        List<Achievement> unlocked = gamePrefs.evaluateAndUnlockAchievements();
        for (Achievement a : unlocked) {
            Toast.makeText(this,
                    "🏆 Achievement Unlocked: " + a.title + " (+" + a.rewardCoins + "⭐)",
                    Toast.LENGTH_LONG).show();
        }
    }

    public GamePrefs getGamePrefs() {
        return gamePrefs;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    @Override
    protected void onResume() {
        super.onResume();
        soundManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        soundManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundManager.release();
    }
}

package com.example;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.audio.SoundManager;
import com.example.data.GamePrefs;
import com.example.model.Achievement;
import com.example.model.Category;
import com.example.model.Difficulty;
import com.example.model.GameTheme;
import com.example.ui.GameAnimations;
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
    private boolean isMenuClosing = false;

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
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
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
        if (sideMenuOverlay.getVisibility() == View.VISIBLE && !isMenuClosing) {
            return;
        }
        isMenuClosing = false;
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
        boolean reduceMotion = GameAnimations.shouldReduceMotion(this, gamePrefs);
        GameAnimations.triggerMenuAnimation(sideMenuOverlay, menuCard, true, reduceMotion, null);
    }

    public void closeSideMenu() {
        if (sideMenuOverlay.getVisibility() != View.VISIBLE || isMenuClosing) {
            return;
        }
        isMenuClosing = true;
        boolean reduceMotion = GameAnimations.shouldReduceMotion(this, gamePrefs);
        GameAnimations.triggerMenuAnimation(sideMenuOverlay, menuCard, false, reduceMotion,
                () -> isMenuClosing = false);
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
        ensureSystemBarsVisible(theme);
        ViewCompat.requestApplyInsets(rootContainer);
    }

    @SuppressWarnings("deprecation")
    private void ensureSystemBarsVisible(GameTheme theme) {
        if (getWindow() == null || theme == null) return;
        View decorView = getWindow().getDecorView();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT < 35) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().setStatusBarColor(theme.backgroundColor);
            int navColor = (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && theme.isLightTheme)
                    ? Color.parseColor("#1E1E2F")
                    : theme.backgroundColor;
            getWindow().setNavigationBarColor(navColor);
        }
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), decorView);
        if (controller != null) {
            controller.show(WindowInsetsCompat.Type.systemBars());
            controller.setAppearanceLightStatusBars(theme.isLightTheme);
            controller.setAppearanceLightNavigationBars(theme.isLightTheme);
        }
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
        ensureSystemBarsVisible(gamePrefs.getCurrentTheme());
        soundManager.onResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && gamePrefs != null) {
            ensureSystemBarsVisible(gamePrefs.getCurrentTheme());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        soundManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sideMenuOverlay != null) sideMenuOverlay.animate().cancel();
        if (menuCard != null) menuCard.animate().cancel();
        soundManager.release();
    }
}

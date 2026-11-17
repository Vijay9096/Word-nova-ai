package com.example;

import static org.junit.Assert.assertNotNull;

import androidx.fragment.app.Fragment;

import com.example.model.Category;
import com.example.model.Difficulty;
import com.example.ui.fragments.AboutFragment;
import com.example.ui.fragments.AchievementsFragment;
import com.example.ui.fragments.CategoryFragment;
import com.example.ui.fragments.DailyPuzzleIntroFragment;
import com.example.ui.fragments.DailyRewardsFragment;
import com.example.ui.fragments.DifficultyFragment;
import com.example.ui.fragments.GameFragment;
import com.example.ui.fragments.HowToPlayFragment;
import com.example.ui.fragments.LevelsFragment;
import com.example.ui.fragments.ProfileFragment;
import com.example.ui.fragments.SettingsFragment;
import com.example.ui.fragments.ShopFragment;
import com.example.ui.fragments.ThemesFragment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class MainActivityLaunchTest {

    @Test
    public void testMainActivityAndAllScreensRenderCleanly() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        assertNotNull(activity);

        Fragment mainMenu = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        assertNotNull("MainMenuFragment should be attached", mainMenu);

        activity.openSideMenu();
        activity.closeSideMenu();

        Fragment[] screens = new Fragment[]{
                new DifficultyFragment(),
                CategoryFragment.newInstance(Difficulty.EASY),
                LevelsFragment.newInstance(Difficulty.EASY, Category.GENERAL),
                GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1),
                GameFragment.newInstance(Difficulty.HARD, Category.ANIMAL, 1),
                GameFragment.newInstance(Difficulty.DAILY, Category.GENERAL, 1),
                new SettingsFragment(),
                new ThemesFragment(),
                new AboutFragment(),
                new AchievementsFragment(),
                new ProfileFragment(),
                new DailyPuzzleIntroFragment(),
                new HowToPlayFragment(),
                new DailyRewardsFragment(),
                new ShopFragment()
        };

        for (Fragment screen : screens) {
            activity.navigateTo(screen, true);
            activity.getSupportFragmentManager().executePendingTransactions();
            Fragment active = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            assertNotNull("Screen failed to attach: " + screen.getClass().getSimpleName(), active);
        }

        activity.showLuckySpin();
        activity.getSupportFragmentManager().executePendingTransactions();
        assertNotNull(activity.getSupportFragmentManager().findFragmentByTag("lucky_spin"));

        controller.pause().stop().destroy();
    }
}

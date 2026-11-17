package com.word.nova;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.word.nova.data.GamePrefs;
import com.word.nova.data.WordBankRepository;
import com.word.nova.model.Category;
import com.word.nova.model.Difficulty;
import com.word.nova.ui.WordFlowLayout;
import com.word.nova.ui.fragments.GameFragment;
import com.word.nova.ui.fragments.MainMenuFragment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w360dp-h640dp-mdpi")
public class WordNovaLayoutAndTestingFeatureTest {

    private static void layoutFragmentView(View root, int widthPx, int heightPx) {
        root.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        );
        root.layout(0, 0, widthPx, heightPx);
    }

    @Test
    public void testStatusBarVisibleAndNotFullscreenOnGameplayScreen() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();

        int flags = activity.getWindow().getAttributes().flags;
        assertEquals("Activity window must not enable fullscreen hiding status bar",
                0, flags & WindowManager.LayoutParams.FLAG_FULLSCREEN);

        GameFragment game = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1);
        activity.navigateTo(game, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        int gameFlags = activity.getWindow().getAttributes().flags;
        assertEquals("Gameplay screen must keep status bar visible",
                0, gameFlags & WindowManager.LayoutParams.FLAG_FULLSCREEN);

        controller.pause().stop().destroy();
    }

    @Test
    public void testClearLevelButtonSingleLineNoClippingAndCompactSpacingAcrossWordCounts() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        activity.getGamePrefs().clearAllLocalStorage();

        // Test both standard (GENERAL 1), long-word (PROFESSION 10), and short-word-list (DAILY)
        Difficulty[] diffs = {Difficulty.EASY, Difficulty.HARD, Difficulty.DAILY};
        Category[] cats = {Category.GENERAL, Category.PROFESSION, Category.GENERAL};
        int[] levels = {1, 10, 1};

        for (int i = 0; i < diffs.length; i++) {
            GameFragment game = GameFragment.newInstance(diffs[i], cats[i], levels[i]);
            activity.navigateTo(game, true);
            activity.getSupportFragmentManager().executePendingTransactions();

            View root = game.requireView();
            // Measure on compact 360x640 (at mdpi 1dp = 1px) and even tighter 320x568
            layoutFragmentView(root, 320, 568);
            ShadowLooper.idleMainLooper();
            layoutFragmentView(root, 320, 568);

            Button btnBack = root.findViewById(R.id.btn_back);
            Button btnClear = root.findViewById(R.id.btn_clear_level);
            View topBar = root.findViewById(R.id.game_top_bar);
            View boardContainer = root.findViewById(R.id.board_container);
            View wordScroll = root.findViewById(R.id.word_scroll);
            View bottomButtons = root.findViewById(R.id.bottom_buttons_container);
            WordFlowLayout flow = root.findViewById(R.id.word_flow_layout);

            assertEquals("Clear This Level", btnClear.getText().toString());
            assertTrue("Back button height >= 48", btnBack.getHeight() >= 48);
            assertTrue("Clear button height >= 48", btnClear.getHeight() >= 48);
            assertEquals("Buttons must have identical width", btnBack.getWidth(), btnClear.getWidth());
            assertEquals("Buttons must have identical height", btnBack.getHeight(), btnClear.getHeight());
            assertEquals("Buttons must be horizontally aligned", btnBack.getTop(), btnClear.getTop());
            assertTrue("Top bar must have comfortable breathing room below status bar", topBar.getTop() >= 18);

            // Verify single-line non-clipped rendering
            if (btnClear.getLayout() != null) {
                assertEquals("Clear This Level must stay on 1 line", 1, btnClear.getLineCount());
                assertEquals("Clear This Level must not ellipsize",
                        0, btnClear.getLayout().getEllipsisCount(0));
            }

            // Verify balanced compact spacing: board -> word list gap and word list -> buttons gap
            int gridToWordsGap = wordScroll.getTop() - boardContainer.getBottom();
            int wordsToButtonsGap = bottomButtons.getTop() - wordScroll.getBottom();
            assertTrue("Grid-to-words gap must be compact (" + gridToWordsGap + ")",
                    gridToWordsGap >= 0 && gridToWordsGap <= 12);
            assertTrue("Words-to-buttons gap must be compact (" + wordsToButtonsGap + ")",
                    wordsToButtonsGap >= 0 && wordsToButtonsGap <= 16);

            // Both bottom buttons must remain fully inside the viewport height without overlapping or clipping
            View contentArea = root.findViewById(R.id.game_content_area);
            assertTrue("Bottom buttons must stay visible within content area ("
                            + bottomButtons.getBottom() + " vs " + contentArea.getHeight() + ")",
                    bottomButtons.getBottom() <= contentArea.getHeight());
            assertTrue("Word chips must be populated", flow.getChildCount() > 0);
        }

        // Also test taller 412x915 phone to ensure grid/word list/buttons shift downward evenly without huge internal gaps
        GameFragment daily = GameFragment.newInstance(Difficulty.DAILY, Category.GENERAL, 1);
        activity.navigateTo(daily, true);
        activity.getSupportFragmentManager().executePendingTransactions();
        View dailyRoot = daily.requireView();
        layoutFragmentView(dailyRoot, 412, 915);
        View dailyBoard = dailyRoot.findViewById(R.id.board_container);
        View dailyScroll = dailyRoot.findViewById(R.id.word_scroll);
        View dailyButtons = dailyRoot.findViewById(R.id.bottom_buttons_container);
        Button dailyBack = dailyRoot.findViewById(R.id.btn_back);
        Button dailyClear = dailyRoot.findViewById(R.id.btn_clear_level);
        int dailyGap = dailyButtons.getTop() - dailyScroll.getBottom();
        assertEquals("Daily Puzzle Back & Clear buttons must match width", dailyBack.getWidth(), dailyClear.getWidth());
        assertEquals("Daily Puzzle Back & Clear buttons must match height", dailyBack.getHeight(), dailyClear.getHeight());
        assertTrue("Daily Puzzle grid should sit comfortably downward from title", dailyBoard.getTop() > 0);
        assertTrue("Daily Puzzle must not leave huge gap between words and buttons (" + dailyGap + ")",
                dailyGap <= 16);

        controller.pause().stop().destroy();
    }

    @Test
    public void testUnlockAllLevelsRemovedAndNormalProgressionRestored() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        GamePrefs prefs = activity.getGamePrefs();
        prefs.clearAllLocalStorage();

        // Simulate existing player progress and an obsolete leftover testing flag from earlier builds
        prefs.addCoins(50);
        prefs.addHints(3);
        prefs.markLevelCompleted(Difficulty.EASY, Category.GENERAL, 1);
        activity.getSharedPreferences("word_nova_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean("testing_all_levels_unlocked", true)
                .apply();

        // Re-initializing GamePrefs cleans up obsolete testing key WITHOUT deleting player progress
        GamePrefs refreshedPrefs = new GamePrefs(activity);
        assertEquals(150, refreshedPrefs.getCoins());
        assertEquals(5, refreshedPrefs.getHints());
        assertTrue(refreshedPrefs.isLevelCompleted(Difficulty.EASY, Category.GENERAL, 1));

        // Normal progression restored: level 1 unlocked, level 2 unlocked via completion of 1,
        // while uncompleted later levels remain locked
        assertTrue(refreshedPrefs.isLevelUnlocked(Difficulty.EASY, Category.GENERAL, 1));
        assertTrue(refreshedPrefs.isLevelUnlocked(Difficulty.EASY, Category.GENERAL, 2));
        assertFalse(refreshedPrefs.isLevelUnlocked(Difficulty.EASY, Category.GENERAL, 3));
        assertFalse(refreshedPrefs.isLevelUnlocked(Difficulty.EASY, Category.PROFESSION, 50));
        assertFalse(refreshedPrefs.isLevelUnlocked(Difficulty.NORMAL, Category.ANIMAL, 25));

        // Verify Unlock All Levels button is completely removed from MainMenuFragment
        MainMenuFragment menu = (MainMenuFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        assertNotNull(menu);
        int removedId = activity.getResources().getIdentifier("btn_unlock_all_levels", "id", activity.getPackageName());
        assertEquals("btn_unlock_all_levels ID should be removed", 0, removedId);
        assertNotNull(menu.requireView().findViewById(R.id.btn_play));
        assertNotNull(menu.requireView().findViewById(R.id.btn_clear_storage));
        assertNotNull(menu.requireView().findViewById(R.id.btn_reset_progress));

        controller.pause().stop().destroy();
    }
}

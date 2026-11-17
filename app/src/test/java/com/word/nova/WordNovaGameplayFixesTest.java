package com.word.nova;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.os.Build;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.word.nova.data.GamePrefs;
import com.word.nova.data.PuzzleGenerator;
import com.word.nova.model.Category;
import com.word.nova.model.Difficulty;
import com.word.nova.ui.GamePopupDialog;
import com.word.nova.ui.WordFlowLayout;
import com.word.nova.ui.WordGridView;
import com.word.nova.ui.fragments.GameFragment;
import com.word.nova.ui.fragments.LuckySpinDialogFragment;
import com.word.nova.ui.fragments.ShopFragment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowVibrator;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WordNovaGameplayFixesTest {

    private static Vibrator getVibrator(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getDefaultVibrator() : null;
        }
        return (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private static float cellCenterX(int col, int viewWidth, int gridSize) {
        float padding = viewWidth * 0.025f;
        float gap = viewWidth * 0.011f;
        float usable = viewWidth - (2f * padding) - ((gridSize - 1) * gap);
        float cellSize = usable / gridSize;
        return padding + col * (cellSize + gap) + cellSize / 2f;
    }

    private static float cellCenterY(int row, int viewWidth, int gridSize) {
        return cellCenterX(row, viewWidth, gridSize);
    }

    private static void dragCells(WordGridView gridView, int gridSize, int r0, int c0, int r1, int c1) {
        int size = 1000;
        gridView.measure(
                View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
        );
        gridView.layout(0, 0, size, size);

        float x0 = cellCenterX(c0, size, gridSize);
        float y0 = cellCenterY(r0, size, gridSize);
        float x1 = cellCenterX(c1, size, gridSize);
        float y1 = cellCenterY(r1, size, gridSize);

        long now = android.os.SystemClock.uptimeMillis();
        gridView.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x0, y0, 0));
        gridView.dispatchTouchEvent(MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_MOVE, x1, y1, 0));
        gridView.dispatchTouchEvent(MotionEvent.obtain(now, now + 20, MotionEvent.ACTION_UP, x1, y1, 0));
    }

    private static void dragPlacedWord(WordGridView gridView, PuzzleGenerator.PuzzleBoard board, PuzzleGenerator.PlacedWord pw) {
        int endRow = pw.startRow + (pw.word.length() - 1) * pw.dRow;
        int endCol = pw.startCol + (pw.word.length() - 1) * pw.dCol;
        dragCells(gridView, board.grid.length, pw.startRow, pw.startCol, endRow, endCol);
    }

    @Test
    public void testWrongWordVibrationAndCorrectWordNoVibration() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        activity.getGamePrefs().clearAllLocalStorage();

        GameFragment game = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1);
        activity.navigateTo(game, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        WordGridView gridView = game.requireView().findViewById(R.id.word_grid_view);
        GamePrefs.SavedLevelState state = activity.getGamePrefs().loadLevelState(Difficulty.EASY, Category.GENERAL, 1);
        assertNotNull(state);
        PuzzleGenerator.PuzzleBoard board = state.board;

        Vibrator vibrator = getVibrator(activity);
        assertNotNull(vibrator);
        ShadowVibrator shadowVibrator = Shadows.shadowOf(vibrator);

        // 1. Valid word submission must NOT trigger wrong-word vibration
        dragPlacedWord(gridView, board, board.placedWords.get(0));
        assertFalse("Correct word must NOT trigger vibration", shadowVibrator.isVibrating());

        // 2. Wrong 2-cell drag (all placed words are >= 3 chars) MUST trigger vibration
        dragCells(gridView, board.grid.length, 0, 0, 0, 1);
        assertTrue("Incorrect word submission MUST trigger vibration", shadowVibrator.isVibrating());

        controller.pause().stop().destroy();
    }

    @Test
    public void testInstantProgressSavingSurvivesExitAndReopen() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        GamePrefs prefs = activity.getGamePrefs();
        prefs.clearAllLocalStorage();

        GameFragment game = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1);
        activity.navigateTo(game, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        WordGridView gridView = game.requireView().findViewById(R.id.word_grid_view);
        Button btnHint = game.requireView().findViewById(R.id.btn_hint);
        assertEquals("💡(2)", btnHint.getText().toString());

        // Use 1 hint -> immediately persisted and UI updated
        btnHint.performClick();
        assertEquals(1, prefs.getHints());
        assertEquals("💡(1)", btnHint.getText().toString());
        assertEquals(1, game.getHintsUsedForCurrentLevel());

        // Find 2 words -> immediately persisted
        GamePrefs.SavedLevelState initial = prefs.loadLevelState(Difficulty.EASY, Category.GENERAL, 1);
        assertNotNull(initial);
        PuzzleGenerator.PlacedWord w0 = initial.board.placedWords.get(0);
        PuzzleGenerator.PlacedWord w1 = initial.board.placedWords.get(1);
        dragPlacedWord(gridView, initial.board, w0);
        dragPlacedWord(gridView, initial.board, w1);

        GamePrefs.SavedLevelState savedAfter = prefs.loadLevelState(Difficulty.EASY, Category.GENERAL, 1);
        assertNotNull(savedAfter);
        assertTrue(savedAfter.board.placedWords.get(0).found);
        assertTrue(savedAfter.board.placedWords.get(1).found);
        assertEquals(1, savedAfter.hintsUsed);
        assertFalse(savedAfter.completed);

        // Exit level and reopen
        activity.navigateBack();
        activity.getSupportFragmentManager().executePendingTransactions();

        GameFragment reopened = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1);
        activity.navigateTo(reopened, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        // Remaining hints stay 1, used hints stay 1, previously found words stay found & struck through
        Button reopenedHint = reopened.requireView().findViewById(R.id.btn_hint);
        assertEquals("💡(1)", reopenedHint.getText().toString());
        assertEquals(1, prefs.getHints());
        assertEquals(1, reopened.getHintsUsedForCurrentLevel());

        WordFlowLayout flow = reopened.requireView().findViewById(R.id.word_flow_layout);
        TextView chip0 = (TextView) flow.getChildAt(0);
        TextView chip1 = (TextView) flow.getChildAt(1);
        TextView chip2 = (TextView) flow.getChildAt(2);
        assertTrue((chip0.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) != 0);
        assertTrue((chip1.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) != 0);
        assertEquals(0, chip2.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG);

        controller.pause().stop().destroy();
    }

    @Test
    public void testCompletedLevelPreservationClearReplayAndOnceOnlyCoins() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        GamePrefs prefs = activity.getGamePrefs();
        prefs.clearAllLocalStorage();
        ShadowDialog.reset();

        int startingCoins = prefs.getCoins(); // 100
        GameFragment game = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1);
        activity.navigateTo(game, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        WordGridView gridView = game.requireView().findViewById(R.id.word_grid_view);
        TextView coinPill = game.requireView().findViewById(R.id.tv_coin_pill);
        GamePrefs.SavedLevelState state = prefs.loadLevelState(Difficulty.EASY, Category.GENERAL, 1);
        assertNotNull(state);

        // Complete Level 1 for the first time
        for (PuzzleGenerator.PlacedWord pw : state.board.placedWords) {
            dragPlacedWord(gridView, state.board, pw);
        }

        // First completion awards Easy level coins (10) + First Steps achievement bonus (15) = 25
        int expectedCoinsAfterFirstClear = startingCoins + Difficulty.EASY.rewardCoins + 15;
        assertEquals(expectedCoinsAfterFirstClear, prefs.getCoins());
        assertEquals("⭐ " + expectedCoinsAfterFirstClear, coinPill.getText().toString());
        assertTrue(prefs.isLevelCompleted(Difficulty.EASY, Category.GENERAL, 1));

        // Custom popup (GamePopupDialog, NOT AlertDialog) appears after completion celebration sequence
        ShadowLooper.idleMainLooper(1200, TimeUnit.MILLISECONDS);
        Dialog completionDialog = ShadowDialog.getLatestDialog();
        assertTrue(completionDialog instanceof GamePopupDialog);
        assertFalse(completionDialog instanceof AlertDialog);
        completionDialog.findViewById(R.id.btn_popup_negative).performClick(); // Back
        activity.getSupportFragmentManager().executePendingTransactions();

        // Spend 1 hint on another context to verify remaining hints stay preserved
        prefs.consumeHint(); // now 1 hint left
        int hintsBeforeReopen = prefs.getHints();

        // Reopen completed Level 1
        ShadowDialog.reset();
        GameFragment reopened = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1);
        activity.navigateTo(reopened, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        // Should show custom popup asking whether to clear before playing again
        Dialog entryPopup = ShadowDialog.getLatestDialog();
        assertTrue(entryPopup instanceof GamePopupDialog);
        TextView msg = entryPopup.findViewById(R.id.tv_popup_message);
        assertEquals("This level is already completed. Do you want to clear this level before playing again?",
                msg.getText().toString());

        // Test Cancel -> keeps all words found and completed state intact
        entryPopup.findViewById(R.id.btn_popup_negative).performClick();
        GamePrefs.SavedLevelState afterCancel = prefs.loadLevelState(Difficulty.EASY, Category.GENERAL, 1);
        assertNotNull(afterCancel);
        assertTrue(afterCancel.board.allFound());
        assertEquals(expectedCoinsAfterFirstClear, prefs.getCoins());

        // Reopen again and choose Clear Level
        activity.navigateBack();
        activity.getSupportFragmentManager().executePendingTransactions();
        ShadowDialog.reset();

        GameFragment reopened2 = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1);
        activity.navigateTo(reopened2, true);
        activity.getSupportFragmentManager().executePendingTransactions();
        Dialog entryPopup2 = ShadowDialog.getLatestDialog();
        assertTrue(entryPopup2 instanceof GamePopupDialog);
        entryPopup2.findViewById(R.id.btn_popup_positive).performClick(); // Clear Level

        // Verify word-finding progress cleared, coins and hints unchanged
        assertEquals(expectedCoinsAfterFirstClear, prefs.getCoins());
        assertEquals(hintsBeforeReopen, prefs.getHints());
        GamePrefs.SavedLevelState afterClear = prefs.loadLevelState(Difficulty.EASY, Category.GENERAL, 1);
        assertNotNull(afterClear);
        assertFalse(afterClear.board.allFound());
        for (PuzzleGenerator.PlacedWord pw : afterClear.board.placedWords) {
            assertFalse(pw.found);
        }

        // Complete the replayed level -> Word Hunter achievement (+20 coins) may trigger at 25 words,
        // but completing the replayed level itself must award 0 completion coins.
        ShadowDialog.reset();
        WordGridView replayGrid = reopened2.requireView().findViewById(R.id.word_grid_view);
        int lastIdx = afterClear.board.placedWords.size() - 1;
        for (int i = 0; i < lastIdx; i++) {
            dragPlacedWord(replayGrid, afterClear.board, afterClear.board.placedWords.get(i));
        }
        int coinsRightBeforeReplayCompletion = prefs.getCoins();
        dragPlacedWord(replayGrid, afterClear.board, afterClear.board.placedWords.get(lastIdx));
        assertEquals("Completing a replayed level must issue 0 level completion coins",
                coinsRightBeforeReplayCompletion, prefs.getCoins());
        assertEquals("Only Word Hunter achievement bonus (+20 at 25 words) may be added, never level reward",
                expectedCoinsAfterFirstClear + 20, prefs.getCoins());

        ShadowLooper.idleMainLooper(1200, TimeUnit.MILLISECONDS);
        Dialog replayDialog = ShadowDialog.getLatestDialog();
        assertTrue(replayDialog instanceof GamePopupDialog);
        Button nextBtn = replayDialog.findViewById(R.id.btn_popup_positive);
        assertEquals("Next", nextBtn.getText().toString());
        nextBtn.performClick();
        activity.getSupportFragmentManager().executePendingTransactions();

        // Next navigated cleanly to Level 2
        GameFragment nextLevelFragment = (GameFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        assertNotNull(nextLevelFragment);
        TextView titleView = nextLevelFragment.requireView().findViewById(R.id.tv_game_title);
        assertTrue(titleView.getText().toString().contains("Level 2"));

        controller.pause().stop().destroy();
    }

    @Test
    public void testLegacyCompletedLevelWithoutSavedStateShowsAllWordsFoundAndPromptsClear() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        GamePrefs prefs = activity.getGamePrefs();
        prefs.clearAllLocalStorage();
        ShadowDialog.reset();

        // Simulate a legacy completed level that existed before per-level state JSON saving
        prefs.markLevelCompleted(Difficulty.EASY, Category.GENERAL, 2);

        GameFragment game = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 2);
        activity.navigateTo(game, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        GamePrefs.SavedLevelState restored = prefs.loadLevelState(Difficulty.EASY, Category.GENERAL, 2);
        assertNotNull(restored);
        assertTrue(restored.completed);
        assertTrue(restored.board.allFound());

        Dialog dialog = ShadowDialog.getLatestDialog();
        assertTrue(dialog instanceof GamePopupDialog);
        TextView msg = dialog.findViewById(R.id.tv_popup_message);
        assertEquals("This level is already completed. Do you want to clear this level before playing again?",
                msg.getText().toString());

        controller.pause().stop().destroy();
    }

    @Test
    public void testLuckySpinAndShopSynchronizeCoinAndHintDisplaysImmediately() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        GamePrefs prefs = activity.getGamePrefs();
        prefs.clearAllLocalStorage();

        // Start on GameFragment so coin pill & hint button are visible
        GameFragment game = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1);
        activity.navigateTo(game, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        TextView coinPill = game.requireView().findViewById(R.id.tv_coin_pill);
        Button btnHint = game.requireView().findViewById(R.id.btn_hint);
        assertEquals("⭐ 100", coinPill.getText().toString());
        assertEquals("💡(2)", btnHint.getText().toString());

        // Open Lucky Spin over GameFragment
        activity.showLuckySpin();
        activity.getSupportFragmentManager().executePendingTransactions();
        LuckySpinDialogFragment spinDialog = (LuckySpinDialogFragment) activity.getSupportFragmentManager()
                .findFragmentByTag("lucky_spin");
        assertNotNull(spinDialog);

        // Buy spin (costs 30 coins) -> coin pill updates IMMEDIATELY without dismissing dialog
        Button btnBuySpin = spinDialog.requireView().findViewById(R.id.btn_buy_spin);
        btnBuySpin.performClick();
        assertEquals(70, prefs.getCoins());
        assertEquals("⭐ 70", coinPill.getText().toString());

        // Advance main looper so spin finishes and awards coins or hints
        int coinsBeforeReward = prefs.getCoins();
        int hintsBeforeReward = prefs.getHints();
        ShadowLooper.idleMainLooper(5000, TimeUnit.MILLISECONDS);

        assertTrue("Spin must award either coins or hints",
                prefs.getCoins() > coinsBeforeReward || prefs.getHints() > hintsBeforeReward);
        assertEquals("Coin pill must sync immediately after Lucky Spin reward",
                "⭐ " + prefs.getCoins(), coinPill.getText().toString());
        assertEquals("Hint button must sync immediately after Lucky Spin reward",
                "💡(" + prefs.getHints() + ")", btnHint.getText().toString());

        spinDialog.dismiss();
        activity.getSupportFragmentManager().executePendingTransactions();

        // Also test ShopFragment immediate synchronization
        ShopFragment shop = new ShopFragment();
        activity.navigateTo(shop, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        TextView shopCoins = shop.requireView().findViewById(R.id.tv_shop_coins);
        TextView shopHints = shop.requireView().findViewById(R.id.tv_shop_hints);
        assertEquals("Coins: " + prefs.getCoins(), shopCoins.getText().toString());
        assertEquals("Hints: " + prefs.getHints(), shopHints.getText().toString());

        if (prefs.getCoins() < 40) {
            prefs.addCoins(100);
        }
        int coinsBeforeBuy = prefs.getCoins();
        int hintsBeforeBuy = prefs.getHints();
        shop.requireView().findViewById(R.id.btn_buy_hint_1).performClick();
        assertEquals(coinsBeforeBuy - 40, prefs.getCoins());
        assertEquals(hintsBeforeBuy + 1, prefs.getHints());
        assertEquals("Coins: " + prefs.getCoins(), shopCoins.getText().toString());
        assertEquals("Hints: " + prefs.getHints(), shopHints.getText().toString());

        controller.pause().stop().destroy();
    }

    @Test
    public void testCustomPopupsReplaceStandardAlertDialogsOnMenu() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        ShadowDialog.reset();

        activity.findViewById(R.id.btn_reset_progress).performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertTrue("Main menu confirmation must use GamePopupDialog", dialog instanceof GamePopupDialog);
        assertFalse(dialog instanceof AlertDialog);
        dialog.findViewById(R.id.btn_popup_negative).performClick();
        assertNull(ShadowDialog.getLatestDialog().isShowing() ? ShadowDialog.getLatestDialog() : null);

        controller.pause().stop().destroy();
    }
}

package com.word.nova;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.MotionEvent;
import android.view.View;

import com.word.nova.data.GamePrefs;
import com.word.nova.data.PuzzleGenerator;
import com.word.nova.model.Category;
import com.word.nova.model.Difficulty;
import com.word.nova.ui.EffectsOverlayView;
import com.word.nova.ui.GameAnimations;
import com.word.nova.ui.GamePopupDialog;
import com.word.nova.ui.WordGridView;
import com.word.nova.ui.fragments.GameFragment;

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
public class WordNovaAnimationsTest {

    private static Vibrator getVibrator(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getDefaultVibrator() : null;
        }
        return (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private static void layoutForDrag(WordGridView gridView) {
        int size = 1000;
        gridView.measure(
                View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
        );
        gridView.layout(0, 0, size, size);
    }

    private static void dragPlacedWord(WordGridView gridView, PuzzleGenerator.PuzzleBoard board, PuzzleGenerator.PlacedWord pw) {
        layoutForDrag(gridView);
        int n = board.grid.length;
        float padding = 1000f * 0.025f;
        float gap = 1000f * 0.011f;
        float cellSize = (1000f - 2f * padding - (n - 1) * gap) / n;

        int endRow = pw.startRow + (pw.word.length() - 1) * pw.dRow;
        int endCol = pw.startCol + (pw.word.length() - 1) * pw.dCol;

        float x0 = padding + pw.startCol * (cellSize + gap) + cellSize / 2f;
        float y0 = padding + pw.startRow * (cellSize + gap) + cellSize / 2f;
        float x1 = padding + endCol * (cellSize + gap) + cellSize / 2f;
        float y1 = padding + endRow * (cellSize + gap) + cellSize / 2f;

        long now = android.os.SystemClock.uptimeMillis();
        gridView.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x0, y0, 0));
        gridView.dispatchTouchEvent(MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_MOVE, x1, y1, 0));
        gridView.dispatchTouchEvent(MotionEvent.obtain(now, now + 20, MotionEvent.ACTION_UP, x1, y1, 0));
    }

    private static void dragInvalidWord(WordGridView gridView, int gridSize) {
        layoutForDrag(gridView);
        float padding = 1000f * 0.025f;
        float gap = 1000f * 0.011f;
        float cellSize = (1000f - 2f * padding - (gridSize - 1) * gap) / gridSize;

        float x0 = padding + cellSize / 2f;
        float y0 = padding + cellSize / 2f;
        float x1 = padding + (cellSize + gap) + cellSize / 2f;
        float y1 = padding + cellSize / 2f;

        long now = android.os.SystemClock.uptimeMillis();
        gridView.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x0, y0, 0));
        gridView.dispatchTouchEvent(MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_MOVE, x1, y1, 0));
        gridView.dispatchTouchEvent(MotionEvent.obtain(now, now + 20, MotionEvent.ACTION_UP, x1, y1, 0));
    }

    @Test
    public void testIncorrectWordVibrationPatternAndCorrectWordParticles() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        GamePrefs prefs = activity.getGamePrefs();
        prefs.clearAllLocalStorage();

        GameFragment game = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1);
        activity.navigateTo(game, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        WordGridView gridView = game.requireView().findViewById(R.id.word_grid_view);
        EffectsOverlayView overlay = game.requireView().findViewById(R.id.effects_overlay);
        GamePrefs.SavedLevelState state = prefs.loadLevelState(Difficulty.EASY, Category.GENERAL, 1);
        assertNotNull(state);

        // Start-level toast appears upon entering a new level
        assertTrue("Level start toast banner must appear on level start", overlay.isStartToastVisible());

        // Clear initial particles/ripples
        overlay.clearAllEffects();
        Vibrator vibrator = getVibrator(activity);
        ShadowVibrator shadowVibrator = Shadows.shadowOf(vibrator);

        // 1. Correct word triggers sparkles + ripples + reveal blast without wrong-word vibration
        dragPlacedWord(gridView, state.board, state.board.placedWords.get(0));
        assertFalse("Valid word must NOT vibrate", shadowVibrator.isVibrating());
        assertTrue("Valid word must emit sparkles & blast particles", overlay.getActiveParticleCount() > 0);
        assertTrue("Valid word must emit ripple & reveal shockwave", overlay.getActiveRippleCount() > 0);

        // Dragging already-found word again must NOT re-trigger animations
        overlay.clearAllEffects();
        dragPlacedWord(gridView, state.board, state.board.placedWords.get(0));
        assertEquals(0, overlay.getActiveParticleCount());
        assertEquals(0, overlay.getActiveRippleCount());

        // 2. Incorrect word triggers exact waveform pattern {0, 200, 100, 60, 40, 60}
        dragInvalidWord(gridView, state.board.grid.length);
        assertTrue("Incorrect word must trigger vibration", shadowVibrator.isVibrating());
        assertArrayEquals("Must match navigator.vibrate([200, 100, 60, 40, 60]) timing waveform",
                new long[]{0L, 200L, 100L, 60L, 40L, 60L},
                shadowVibrator.getPattern());

        controller.pause().stop().destroy();
    }

    @Test
    public void testIdleGridBreathAndHintPulseStartOnInactivityAndStopImmediatelyOnTouch() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        activity.getGamePrefs().clearAllLocalStorage();

        GameFragment game = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1);
        activity.navigateTo(game, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        WordGridView gridView = game.requireView().findViewById(R.id.word_grid_view);
        GameAnimations animations = game.getAnimations();

        // Advance past intro (360ms) + idle delays (4000ms breath, 5500ms hint pulse)
        ShadowLooper.idleMainLooper(6500, TimeUnit.MILLISECONDS);
        assertTrue("GridBreath must start after idle delay", animations.getGridBreath().isRunning());
        assertTrue("Hint button pulse must start after idle delay when hints > 0", animations.isHintPulsing());

        // Touching grid stops both immediately!
        layoutForDrag(gridView);
        long now = android.os.SystemClock.uptimeMillis();
        gridView.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 100f, 100f, 0));
        assertFalse("GridBreath must stop immediately on touch down", animations.getGridBreath().isRunning());
        assertFalse("Hint pulse must stop immediately on touch down", animations.isHintPulsing());

        controller.pause().stop().destroy();
    }

    @Test
    public void testLevelCompletionRibbonConfettiFireworksAndMenuCloseTiming() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        GamePrefs prefs = activity.getGamePrefs();
        prefs.clearAllLocalStorage();
        ShadowDialog.reset();

        // Verify Side Menu opens and hides ONLY AFTER closing animation completes
        View menuOverlay = activity.findViewById(R.id.side_menu_overlay);
        assertEquals(View.GONE, menuOverlay.getVisibility());
        activity.openSideMenu();
        assertEquals(View.VISIBLE, menuOverlay.getVisibility());
        ShadowLooper.idleMainLooper(300, TimeUnit.MILLISECONDS);

        activity.closeSideMenu();
        assertEquals("Menu must remain visible while closing animation runs",
                View.VISIBLE, menuOverlay.getVisibility());
        ShadowLooper.idleMainLooper(250, TimeUnit.MILLISECONDS);
        assertEquals("Menu must become GONE after closing animation finishes",
                View.GONE, menuOverlay.getVisibility());

        // Launch gameplay and verify completion effects do not fire early
        GameFragment game = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 1);
        activity.navigateTo(game, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        WordGridView gridView = game.requireView().findViewById(R.id.word_grid_view);
        EffectsOverlayView overlay = game.requireView().findViewById(R.id.effects_overlay);
        GamePrefs.SavedLevelState state = prefs.loadLevelState(Difficulty.EASY, Category.GENERAL, 1);
        assertNotNull(state);

        int count = state.board.placedWords.size();
        for (int i = 0; i < count - 1; i++) {
            dragPlacedWord(gridView, state.board, state.board.placedWords.get(i));
        }
        assertFalse("Completion ribbon must NOT appear before all words are found", overlay.isRibbonVisible());

        // Complete final word -> spawns center confetti & ribbon immediately, then burst/fireworks/popup
        dragPlacedWord(gridView, state.board, state.board.placedWords.get(count - 1));
        assertTrue("Level complete ribbon must appear on completion", overlay.isRibbonVisible());
        assertTrue("Center confetti must spawn on completion", overlay.getActiveParticleCount() > 0);

        ShadowLooper.idleMainLooper(1200, TimeUnit.MILLISECONDS);
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertTrue("Completion popup appears cleanly after outro/confetti sequence", dialog instanceof GamePopupDialog);

        // Verify Performance Mode respects reduced motion (completes with zero lingering particle storm)
        dialog.findViewById(R.id.btn_popup_negative).performClick();
        prefs.setPerformanceMode(true);
        GameFragment perfGame = GameFragment.newInstance(Difficulty.EASY, Category.GENERAL, 2);
        activity.navigateTo(perfGame, true);
        activity.getSupportFragmentManager().executePendingTransactions();
        EffectsOverlayView perfOverlay = perfGame.requireView().findViewById(R.id.effects_overlay);
        assertFalse("Start toast decorative animation skipped in Performance Mode", perfOverlay.isStartToastVisible());

        controller.pause().stop().destroy();
    }
}

package com.word.nova.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.word.nova.data.GamePrefs;
import com.word.nova.data.PuzzleGenerator;
import com.word.nova.model.GameTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized coordinator for Word Nova gameplay, idle, completion, and menu animations.
 * Respects both in-app Performance Mode and system reduced-motion settings, avoids duplicate
 * triggers, and cleans up all active animators and delayed callbacks when cancelled.
 */
public class GameAnimations {

    public static final long[] WRONG_WORD_VIBRATE_PATTERN = {0L, 200L, 100L, 60L, 40L, 60L};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Runnable> pendingRunnables = new ArrayList<>();

    private ObjectAnimator shakeAnimator;
    private ObjectAnimator introTitleAnimator;
    private AnimatorSet introBoardAnimator;
    private AnimatorSet wordListSlideAnimator;
    private AnimatorSet outroAnimator;
    private AnimatorSet hintPulseAnimator;
    private boolean hintPulsing = false;
    private final GridBreath gridBreath = new GridBreath();

    public static boolean shouldReduceMotion(Context context, GamePrefs prefs) {
        if (prefs != null && prefs.isPerformanceMode()) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()) {
            return true;
        }
        if (context != null) {
            try {
                float scale = Settings.Global.getFloat(
                        context.getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE,
                        1.0f
                );
                if (scale == 0f) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private static float dp(View v, float dpVal) {
        return dpVal * v.getResources().getDisplayMetrics().density;
    }

    private void postTracked(Runnable r, long delayMs) {
        pendingRunnables.add(r);
        handler.postDelayed(r, delayMs);
    }

    public void triggerWrongShake(View boardContainer, boolean reduceMotion) {
        if (boardContainer == null) return;
        if (shakeAnimator != null) {
            shakeAnimator.cancel();
            shakeAnimator = null;
        }
        boardContainer.setTranslationX(0f);
        if (reduceMotion) return;

        float amp1 = dp(boardContainer, 12f);
        float amp2 = dp(boardContainer, 9f);
        float amp3 = dp(boardContainer, 5f);
        shakeAnimator = ObjectAnimator.ofFloat(
                boardContainer,
                View.TRANSLATION_X,
                0f, -amp1, amp1, -amp2, amp2, -amp3, amp3, 0f
        );
        shakeAnimator.setDuration(420L);
        shakeAnimator.setInterpolator(new DecelerateInterpolator());
        shakeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                boardContainer.setTranslationX(0f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                boardContainer.setTranslationX(0f);
            }
        });
        shakeAnimator.start();
    }

    public void triggerWordPop(View chipView, boolean reduceMotion) {
        if (chipView == null || reduceMotion) return;
        chipView.animate().cancel();
        chipView.setScaleX(1f);
        chipView.setScaleY(1f);
        chipView.animate()
                .scaleX(1.18f)
                .scaleY(1.18f)
                .setDuration(140L)
                .setInterpolator(new OvershootInterpolator(2.0f))
                .withEndAction(() -> chipView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150L)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();
    }

    public void emitWordSparkles(EffectsOverlayView overlay, float cx, float cy,
                                 float spreadW, float spreadH, int color, boolean reduceMotion) {
        if (overlay == null || reduceMotion) return;
        overlay.emitWordSparkles(cx, cy, spreadW, spreadH, color);
    }

    public void triggerRipple(EffectsOverlayView overlay, float cx, float cy, int color, boolean reduceMotion) {
        if (overlay == null || reduceMotion) return;
        overlay.triggerRipple(cx, cy, color);
    }

    public void triggerCellPulse(WordGridView gridView, List<int[]> cells, int color) {
        if (gridView == null) return;
        gridView.triggerCellPulse(cells, color);
    }

    public void triggerCellRipple(WordGridView gridView, List<int[]> cells, int color) {
        if (gridView == null) return;
        gridView.triggerCellRipple(cells, color);
    }

    public void triggerWordRevealBlast(WordGridView gridView, EffectsOverlayView overlay,
                                       List<int[]> cells, float cx, float cy, int color, boolean reduceMotion) {
        if (reduceMotion) return;
        if (gridView != null) {
            gridView.triggerWordRevealBlast(cells, color);
        }
        if (overlay != null) {
            overlay.triggerWordRevealBlast(cx, cy, color);
        }
    }

    public void triggerLevelIntro(View titleView, View boardContainer, boolean reduceMotion, Runnable onDone) {
        if (introTitleAnimator != null) {
            introTitleAnimator.cancel();
            introTitleAnimator = null;
        }
        if (introBoardAnimator != null) {
            introBoardAnimator.cancel();
            introBoardAnimator = null;
        }

        if (reduceMotion || boardContainer == null) {
            if (titleView != null) {
                titleView.setAlpha(1f);
                titleView.setTranslationY(0f);
            }
            if (boardContainer != null) {
                boardContainer.setAlpha(1f);
                boardContainer.setScaleX(1f);
                boardContainer.setScaleY(1f);
                boardContainer.setTranslationY(0f);
            }
            if (onDone != null) onDone.run();
            return;
        }

        if (titleView != null) {
            titleView.setAlpha(0f);
            introTitleAnimator = ObjectAnimator.ofFloat(titleView, View.ALPHA, 0f, 1f);
            introTitleAnimator.setDuration(260L);
            introTitleAnimator.start();
        }

        boardContainer.setAlpha(0f);
        boardContainer.setScaleX(0.93f);
        boardContainer.setScaleY(0.93f);
        boardContainer.setTranslationY(dp(boardContainer, 14f));

        ObjectAnimator alpha = ObjectAnimator.ofFloat(boardContainer, View.ALPHA, 0f, 1f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(boardContainer, View.SCALE_X, 0.93f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(boardContainer, View.SCALE_Y, 0.93f, 1f);
        ObjectAnimator ty = ObjectAnimator.ofFloat(boardContainer, View.TRANSLATION_Y,
                dp(boardContainer, 14f), 0f);

        introBoardAnimator = new AnimatorSet();
        introBoardAnimator.playTogether(alpha, sx, sy, ty);
        introBoardAnimator.setDuration(360L);
        introBoardAnimator.setInterpolator(new DecelerateInterpolator());
        introBoardAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                boardContainer.setAlpha(1f);
                boardContainer.setScaleX(1f);
                boardContainer.setScaleY(1f);
                boardContainer.setTranslationY(0f);
                if (onDone != null) onDone.run();
            }
        });
        introBoardAnimator.start();
    }

    public void showStartLevelToast(EffectsOverlayView overlay, String text, GameTheme theme, boolean reduceMotion) {
        if (overlay == null || reduceMotion) return;
        overlay.showStartLevelToast(text, theme);
    }

    public void triggerWordListSlideIn(WordFlowLayout wordFlow, boolean reduceMotion) {
        if (wordFlow == null) return;
        if (wordListSlideAnimator != null) {
            wordListSlideAnimator.cancel();
            wordListSlideAnimator = null;
        }
        if (reduceMotion) {
            wordFlow.setAlpha(1f);
            wordFlow.setTranslationY(0f);
            return;
        }

        wordFlow.setAlpha(0f);
        wordFlow.setTranslationY(dp(wordFlow, 22f));
        ObjectAnimator alpha = ObjectAnimator.ofFloat(wordFlow, View.ALPHA, 0f, 1f);
        ObjectAnimator ty = ObjectAnimator.ofFloat(wordFlow, View.TRANSLATION_Y, dp(wordFlow, 22f), 0f);
        wordListSlideAnimator = new AnimatorSet();
        wordListSlideAnimator.playTogether(alpha, ty);
        wordListSlideAnimator.setStartDelay(90L);
        wordListSlideAnimator.setDuration(340L);
        wordListSlideAnimator.setInterpolator(new DecelerateInterpolator());
        wordListSlideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                wordFlow.setAlpha(1f);
                wordFlow.setTranslationY(0f);
            }
        });
        wordListSlideAnimator.start();
    }

    public void dropConfettiFromCenterAllDir(EffectsOverlayView overlay, View boardContainer, boolean reduceMotion) {
        if (overlay == null || reduceMotion) return;
        float[] center = resolveCenterInRoot(boardContainer, overlay);
        overlay.dropConfettiFromCenterAllDir(center[0], center[1]);
    }

    public void dropBurstConfetti(EffectsOverlayView overlay, boolean reduceMotion) {
        if (overlay == null || reduceMotion) return;
        overlay.dropBurstConfetti();
    }

    public void triggerFireworks(EffectsOverlayView overlay, boolean reduceMotion) {
        if (overlay == null || reduceMotion) return;
        overlay.triggerFireworks();
    }

    public void showLevelCompleteRibbon(EffectsOverlayView overlay, String text, GameTheme theme, boolean reduceMotion) {
        if (overlay == null || reduceMotion) return;
        overlay.showLevelCompleteRibbon(text, theme);
    }

    public void triggerLevelOutro(View boardContainer, View wordScroll, boolean reduceMotion, Runnable onDone) {
        if (outroAnimator != null) {
            outroAnimator.cancel();
            outroAnimator = null;
        }
        if (reduceMotion || boardContainer == null) {
            if (onDone != null) onDone.run();
            return;
        }
        ObjectAnimator sx = ObjectAnimator.ofFloat(boardContainer, View.SCALE_X, 1f, 1.03f, 0.99f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(boardContainer, View.SCALE_Y, 1f, 1.03f, 0.99f, 1f);
        outroAnimator = new AnimatorSet();
        if (wordScroll != null) {
            ObjectAnimator wa = ObjectAnimator.ofFloat(wordScroll, View.ALPHA, 1f, 0.86f, 1f);
            outroAnimator.playTogether(sx, sy, wa);
        } else {
            outroAnimator.playTogether(sx, sy);
        }
        outroAnimator.setDuration(460L);
        outroAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        outroAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                boardContainer.setScaleX(1f);
                boardContainer.setScaleY(1f);
                if (wordScroll != null) wordScroll.setAlpha(1f);
                if (onDone != null) onDone.run();
            }
        });
        outroAnimator.start();
    }

    public void runLevelCompletionSequence(
            View boardContainer,
            View wordScroll,
            EffectsOverlayView overlay,
            String ribbonText,
            GameTheme theme,
            boolean reduceMotion,
            Runnable showDialogAction
    ) {
        if (reduceMotion) {
            if (showDialogAction != null) showDialogAction.run();
            return;
        }
        dropConfettiFromCenterAllDir(overlay, boardContainer, false);
        showLevelCompleteRibbon(overlay, ribbonText, theme, false);

        postTracked(() -> dropBurstConfetti(overlay, false), 180L);
        postTracked(() -> triggerFireworks(overlay, false), 220L);
        postTracked(() -> triggerLevelOutro(boardContainer, wordScroll, false, null), 360L);
        if (showDialogAction != null) {
            postTracked(showDialogAction, 900L);
        }
    }

    // --- Idle Hint Button Pulse ---
    public void startHintPulse(View btnHint, boolean reduceMotion) {
        if (btnHint == null || reduceMotion || hintPulsing) return;
        hintPulsing = true;
        btnHint.setScaleX(1f);
        btnHint.setScaleY(1f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(btnHint, View.SCALE_X, 1f, 1.12f, 1f);
        sx.setRepeatCount(ValueAnimator.INFINITE);
        sx.setRepeatMode(ValueAnimator.RESTART);
        ObjectAnimator sy = ObjectAnimator.ofFloat(btnHint, View.SCALE_Y, 1f, 1.12f, 1f);
        sy.setRepeatCount(ValueAnimator.INFINITE);
        sy.setRepeatMode(ValueAnimator.RESTART);

        hintPulseAnimator = new AnimatorSet();
        hintPulseAnimator.playTogether(sx, sy);
        hintPulseAnimator.setDuration(1200L);
        hintPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        hintPulseAnimator.start();
    }

    public void stopHintPulse(View btnHint) {
        hintPulsing = false;
        if (hintPulseAnimator != null) {
            hintPulseAnimator.cancel();
            hintPulseAnimator = null;
        }
        if (btnHint != null) {
            btnHint.setScaleX(1f);
            btnHint.setScaleY(1f);
        }
    }

    public boolean isHintPulsing() {
        return hintPulsing;
    }

    // --- Idle Grid Breathing (GridBreath) ---
    public GridBreath getGridBreath() {
        return gridBreath;
    }

    public static class GridBreath {
        private ValueAnimator breathAnimator;
        private View boardTarget;
        private WordGridView gridTarget;
        private boolean running = false;

        public void start(View boardContainer, WordGridView gridView, boolean reduceMotion) {
            if (boardContainer == null || reduceMotion || running) return;
            running = true;
            boardTarget = boardContainer;
            gridTarget = gridView;

            breathAnimator = ValueAnimator.ofFloat(0f, 1f);
            breathAnimator.setDuration(2400L);
            breathAnimator.setRepeatCount(ValueAnimator.INFINITE);
            breathAnimator.setRepeatMode(ValueAnimator.REVERSE);
            breathAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            breathAnimator.addUpdateListener(anim -> {
                float v = (float) anim.getAnimatedValue();
                float scale = 1f + v * 0.015f;
                if (boardTarget != null) {
                    boardTarget.setScaleX(scale);
                    boardTarget.setScaleY(scale);
                }
                if (gridTarget != null) {
                    gridTarget.setGridBreathPhase(v);
                }
            });
            breathAnimator.start();
        }

        public void stop() {
            running = false;
            if (breathAnimator != null) {
                breathAnimator.cancel();
                breathAnimator = null;
            }
            if (boardTarget != null) {
                boardTarget.setScaleX(1f);
                boardTarget.setScaleY(1f);
            }
            if (gridTarget != null) {
                gridTarget.setGridBreathPhase(0f);
            }
        }

        public boolean isRunning() {
            return running;
        }
    }

    // --- Side Menu Open / Close Animation ---
    public static void triggerMenuAnimation(View scrimOverlay, View menuCard,
                                            boolean open, boolean reduceMotion,
                                            Runnable onClosed) {
        if (scrimOverlay == null || menuCard == null) {
            if (!open && onClosed != null) onClosed.run();
            return;
        }
        scrimOverlay.animate().cancel();
        menuCard.animate().cancel();

        if (reduceMotion) {
            scrimOverlay.setAlpha(1f);
            menuCard.setAlpha(1f);
            menuCard.setTranslationX(0f);
            menuCard.setScaleX(1f);
            menuCard.setScaleY(1f);
            scrimOverlay.setVisibility(open ? View.VISIBLE : View.GONE);
            if (!open && onClosed != null) {
                onClosed.run();
            }
            return;
        }

        float offsetPx = dp(menuCard, 56f);
        if (open) {
            scrimOverlay.setAlpha(0f);
            scrimOverlay.setVisibility(View.VISIBLE);
            menuCard.setAlpha(0f);
            menuCard.setTranslationX(offsetPx);
            menuCard.setScaleX(0.94f);
            menuCard.setScaleY(0.94f);

            scrimOverlay.animate()
                    .alpha(1f)
                    .setDuration(220L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            menuCard.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(260L)
                    .setInterpolator(new OvershootInterpolator(0.85f))
                    .start();
        } else {
            scrimOverlay.animate()
                    .alpha(0f)
                    .setDuration(200L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            menuCard.animate()
                    .alpha(0f)
                    .translationX(offsetPx)
                    .scaleX(0.94f)
                    .scaleY(0.94f)
                    .setDuration(200L)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        scrimOverlay.setVisibility(View.GONE);
                        scrimOverlay.setAlpha(1f);
                        menuCard.setAlpha(1f);
                        menuCard.setTranslationX(0f);
                        menuCard.setScaleX(1f);
                        menuCard.setScaleY(1f);
                        if (onClosed != null) {
                            onClosed.run();
                        }
                    })
                    .start();
        }
    }

    public static float[] resolveCenterInRoot(View target, View ancestor) {
        if (ancestor == null) return new float[]{0f, 0f};
        if (target == null || target.getWidth() <= 0 || target.getHeight() <= 0) {
            return new float[]{ancestor.getWidth() / 2f, ancestor.getHeight() * 0.44f};
        }
        float x = target.getWidth() / 2f;
        float y = target.getHeight() / 2f;
        View cur = target;
        while (cur != null && cur != ancestor) {
            x += cur.getLeft() - cur.getScrollX();
            y += cur.getTop() - cur.getScrollY();
            if (cur.getParent() instanceof View) {
                cur = (View) cur.getParent();
            } else {
                break;
            }
        }
        return new float[]{x, y};
    }

    public static void resolveGridPointInOverlay(WordGridView grid, EffectsOverlayView overlay,
                                                 PuzzleGenerator.PlacedWord word, float[] outXYWH) {
        if (outXYWH == null || outXYWH.length < 4) return;
        if (grid == null || overlay == null) {
            outXYWH[0] = 180f;
            outXYWH[1] = 280f;
            outXYWH[2] = 80f;
            outXYWH[3] = 40f;
            return;
        }
        grid.computeWordLocalCenterAndSpan(word, outXYWH);
        View cur = grid;
        while (cur != null && cur.getParent() instanceof View) {
            outXYWH[0] += cur.getLeft() - cur.getScrollX();
            outXYWH[1] += cur.getTop() - cur.getScrollY();
            View parent = (View) cur.getParent();
            if (parent.getId() == com.word.nova.R.id.game_root) {
                break;
            }
            cur = parent;
        }
    }

    public void stopAll(View btnHint, EffectsOverlayView overlay) {
        for (Runnable r : pendingRunnables) {
            handler.removeCallbacks(r);
        }
        pendingRunnables.clear();
        gridBreath.stop();
        stopHintPulse(btnHint);
        if (shakeAnimator != null) {
            shakeAnimator.cancel();
            shakeAnimator = null;
        }
        if (introTitleAnimator != null) {
            introTitleAnimator.cancel();
            introTitleAnimator = null;
        }
        if (introBoardAnimator != null) {
            introBoardAnimator.cancel();
            introBoardAnimator = null;
        }
        if (wordListSlideAnimator != null) {
            wordListSlideAnimator.cancel();
            wordListSlideAnimator = null;
        }
        if (outroAnimator != null) {
            outroAnimator.cancel();
            outroAnimator = null;
        }
        if (overlay != null) {
            overlay.clearAllEffects();
        }
    }
}

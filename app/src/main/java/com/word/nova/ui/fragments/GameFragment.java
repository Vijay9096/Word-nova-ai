package com.word.nova.ui.fragments;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;

import com.word.nova.MainActivity;
import com.word.nova.R;
import com.word.nova.data.GamePrefs;
import com.word.nova.data.PuzzleGenerator;
import com.word.nova.data.WordBankRepository;
import com.word.nova.model.Category;
import com.word.nova.model.Difficulty;
import com.word.nova.model.GameTheme;
import com.word.nova.ui.EffectsOverlayView;
import com.word.nova.ui.GameAnimations;
import com.word.nova.ui.GamePopupDialog;
import com.word.nova.ui.ThemeHelper;
import com.word.nova.ui.WordFlowLayout;
import com.word.nova.ui.WordGridView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GameFragment extends Fragment {

    private static final String ARG_DIFFICULTY = "arg_difficulty";
    private static final String ARG_CATEGORY = "arg_category";
    private static final String ARG_LEVEL = "arg_level";
    private static final long IDLE_BREATH_DELAY_MS = 4000L;
    private static final long IDLE_HINT_DELAY_MS = 5500L;

    private Difficulty difficulty;
    private Category category;
    private int level;

    private TextView tvTitle;
    private View boardContainer;
    private View wordScroll;
    private EffectsOverlayView effectsOverlay;
    private WordGridView wordGridView;
    private WordFlowLayout wordFlowLayout;
    private Button btnHint;
    private TextView tvCoinPill;
    private TextView tvTimerPill;

    private PuzzleGenerator.PuzzleBoard board;
    private final Map<String, TextView> wordChips = new HashMap<>();
    private CountDownTimer hardTimer;
    private boolean levelFinished = false;

    private int hintsUsed = 0;
    private int hintedWordIdx = -1;
    private int remainingSeconds = 0;

    private GamePrefs prefsRef;
    private GameTheme currentTheme;
    private GamePrefs.OnEconomyChangeListener economyListener;

    private final GameAnimations animations = new GameAnimations();
    private final Handler idleHandler = new Handler(Looper.getMainLooper());
    private final Runnable startBreathRunnable = () -> {
        if (!isAdded() || levelFinished || isReduceMotion()) return;
        animations.getGridBreath().start(boardContainer, wordGridView, false);
    };
    private final Runnable startHintPulseRunnable = () -> {
        if (!isAdded() || levelFinished || isReduceMotion()) return;
        if (difficulty != null && difficulty.hintsAllowed && prefsRef != null && prefsRef.getHints() > 0) {
            animations.startHintPulse(btnHint, false);
        }
    };

    public static GameFragment newInstance(Difficulty difficulty, Category category, int level) {
        GameFragment f = new GameFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DIFFICULTY, difficulty.keyPrefix);
        args.putString(ARG_CATEGORY, category.displayName);
        args.putInt(ARG_LEVEL, level);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity act = (MainActivity) requireActivity();
        GamePrefs prefs = act.getGamePrefs();
        this.prefsRef = prefs;
        GameTheme theme = prefs.getCurrentTheme();
        this.currentTheme = theme;

        Bundle args = getArguments();
        difficulty = Difficulty.fromKey(args != null ? args.getString(ARG_DIFFICULTY) : "easy");
        category = Category.fromName(args != null ? args.getString(ARG_CATEGORY) : "GENERAL");
        level = args != null ? args.getInt(ARG_LEVEL, 1) : 1;

        ThemeHelper.applyRootTheme(view.findViewById(R.id.game_root), theme);
        boardContainer = view.findViewById(R.id.board_container);
        wordScroll = view.findViewById(R.id.word_scroll);
        effectsOverlay = view.findViewById(R.id.effects_overlay);
        ThemeHelper.styleCard(boardContainer, theme.boardColor, 14);

        btnHint = view.findViewById(R.id.btn_hint);
        tvCoinPill = view.findViewById(R.id.tv_coin_pill);
        tvTimerPill = view.findViewById(R.id.tv_timer_pill);
        tvTitle = view.findViewById(R.id.tv_game_title);
        wordGridView = view.findViewById(R.id.word_grid_view);
        wordFlowLayout = view.findViewById(R.id.word_flow_layout);
        Button btnBack = view.findViewById(R.id.btn_back);
        Button btnClearLevel = view.findViewById(R.id.btn_clear_level);
        View contentArea = view.findViewById(R.id.game_content_area);
        View bottomButtons = view.findViewById(R.id.bottom_buttons_container);

        ThemeHelper.styleHeaderText(tvTitle, theme);
        ThemeHelper.styleCard(tvCoinPill, theme.surfaceColor, 12);
        tvCoinPill.setTextColor(theme.textPrimaryColor);
        ThemeHelper.styleCard(tvTimerPill, theme.surfaceColor, 12);
        ThemeHelper.stylePrimaryButton(btnBack, theme);
        ThemeHelper.stylePrimaryButton(btnClearLevel, theme);

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                btnBack, 10, 14, 1, TypedValue.COMPLEX_UNIT_SP);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                btnClearLevel, 10, 14, 1, TypedValue.COMPLEX_UNIT_SP);

        if (contentArea != null && boardContainer != null && bottomButtons != null) {
            contentArea.addOnLayoutChangeListener((v, left, top, right, bottom,
                                                   oldLeft, oldTop, oldRight, oldBottom) ->
                    adjustBoardSizeToFit(contentArea, boardContainer, bottomButtons));
        }

        if (difficulty == Difficulty.DAILY) {
            tvTitle.setText("🌞 Daily Puzzle");
        } else {
            tvTitle.setText(category.displayName + " - Level " + level);
        }

        if (difficulty.hintsAllowed) {
            ThemeHelper.stylePrimaryButton(btnHint, theme);
        } else {
            ThemeHelper.styleButton(btnHint, Color.parseColor("#3A3F54"), Color.parseColor("#9AA0B8"), 12);
        }

        view.findViewById(R.id.btn_hamburger).setOnClickListener(v -> {
            stopIdleAnimationsImmediately();
            act.openSideMenu();
        });

        btnHint.setOnClickListener(v -> {
            stopIdleAnimationsImmediately();
            handleHintTap(act, prefs, theme);
            scheduleIdleAnimations();
        });
        btnBack.setOnClickListener(v -> {
            stopIdleAnimationsImmediately();
            act.navigateBack();
        });
        btnClearLevel.setOnClickListener(v -> {
            stopIdleAnimationsImmediately();
            act.getSoundManager().playClick();
            clearLevelForReplay(act, prefs, theme, true);
        });

        economyListener = (coins, hints) -> {
            if (isAdded()) {
                refreshTopBar(prefs);
            }
        };
        prefs.addEconomyChangeListener(economyListener);

        setupPuzzle(act, prefs, theme);
        refreshTopBar(prefs);
    }

    private boolean isReduceMotion() {
        return GameAnimations.shouldReduceMotion(getContext(), prefsRef);
    }

    private void stopIdleAnimationsImmediately() {
        idleHandler.removeCallbacks(startBreathRunnable);
        idleHandler.removeCallbacks(startHintPulseRunnable);
        animations.getGridBreath().stop();
        animations.stopHintPulse(btnHint);
    }

    private void scheduleIdleAnimations() {
        stopIdleAnimationsImmediately();
        if (!isAdded() || levelFinished || isReduceMotion()) return;
        idleHandler.postDelayed(startBreathRunnable, IDLE_BREATH_DELAY_MS);
        if (difficulty != null && difficulty.hintsAllowed && prefsRef != null && prefsRef.getHints() > 0) {
            idleHandler.postDelayed(startHintPulseRunnable, IDLE_HINT_DELAY_MS);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity act = (MainActivity) getActivity();
            GamePrefs prefs = act.getGamePrefs();
            GameTheme theme = prefs.getCurrentTheme();
            this.currentTheme = theme;
            refreshTopBar(prefs);
            if (wordGridView != null) {
                wordGridView.setTheme(theme);
                wordGridView.setColorblindMode(prefs.isColorblindMode());
                wordGridView.setPerformanceMode(prefs.isPerformanceMode());
                wordGridView.setReducedMotion(GameAnimations.shouldReduceMotion(act, prefs));
            }
            if (!levelFinished) {
                scheduleIdleAnimations();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopIdleAnimationsImmediately();
        if (prefsRef != null && board != null) {
            saveCurrentState(prefsRef, board.allFound());
        }
    }

    private void refreshTopBar(GamePrefs prefs) {
        if (btnHint != null) {
            btnHint.setText("💡(" + prefs.getHints() + ")");
        }
        if (tvCoinPill != null) {
            tvCoinPill.setText("⭐ " + prefs.getCoins());
        }
    }

    private void setupPuzzle(MainActivity act, GamePrefs prefs, GameTheme theme) {
        GamePrefs.SavedLevelState saved = prefs.loadLevelState(difficulty, category, level);
        if (saved != null && saved.board != null && !saved.board.placedWords.isEmpty()) {
            board = saved.board;
            hintsUsed = saved.hintsUsed;
            hintedWordIdx = saved.hintedWordIdx;
            remainingSeconds = saved.remainingSeconds > 0
                    ? saved.remainingSeconds : difficulty.timeLimitSeconds;
            wordGridView.setBoard(board);
            wordGridView.setNextColorIdx(saved.nextColorIdx);
        } else {
            List<String> words = WordBankRepository.getWordsForLevel(category, difficulty, level);
            board = PuzzleGenerator.generate(category, difficulty, level, words);
            hintsUsed = 0;
            hintedWordIdx = -1;
            if (prefs.isLevelCompleted(difficulty, category, level)) {
                // Upgrade path: pre-existing completed level without saved board state
                for (int i = 0; i < board.placedWords.size(); i++) {
                    PuzzleGenerator.PlacedWord pw = board.placedWords.get(i);
                    pw.found = true;
                    pw.colorIndex = i;
                }
                remainingSeconds = 0;
                wordGridView.setBoard(board);
                saveCurrentState(prefs, true);
            } else {
                remainingSeconds = difficulty.timeLimitSeconds;
                wordGridView.setBoard(board);
                saveCurrentState(prefs, false);
            }
        }

        boolean reduce = isReduceMotion();
        wordGridView.setTheme(theme);
        wordGridView.setColorblindMode(prefs.isColorblindMode());
        wordGridView.setPerformanceMode(prefs.isPerformanceMode());
        wordGridView.setReducedMotion(reduce);
        wordGridView.setInteractionListener(new WordGridView.OnGridInteractionListener() {
            @Override
            public void onUserTouchStart() {
                stopIdleAnimationsImmediately();
            }

            @Override
            public void onUserTouchEnd() {
                if (!levelFinished) {
                    scheduleIdleAnimations();
                }
            }
        });
        wordGridView.setListener(new WordGridView.OnWordGridListener() {
            @Override
            public void onWordFound(PuzzleGenerator.PlacedWord word) {
                act.getSoundManager().playWordFound();
                stopIdleAnimationsImmediately();
                if (hintedWordIdx >= 0 && hintedWordIdx < board.placedWords.size()
                        && board.placedWords.get(hintedWordIdx) == word) {
                    hintedWordIdx = -1;
                }
                boolean nowAllFound = board.allFound();
                saveCurrentState(prefs, nowAllFound);
                prefs.incrementWordsFound(1);
                act.checkAndNotifyAchievements();
                refreshTopBar(prefs);
                updateWordChipFound(word.word, theme);

                boolean reduceNow = isReduceMotion();
                int wordColor = wordGridView.getWordHighlightColor(word);
                float[] gridPos = new float[4];
                GameAnimations.resolveGridPointInOverlay(wordGridView, effectsOverlay, word, gridPos);
                TextView chipView = wordChips.get(word.word);
                animations.triggerWordPop(chipView, reduceNow);
                animations.emitWordSparkles(effectsOverlay,
                        gridPos[0], gridPos[1], gridPos[2], gridPos[3], wordColor, reduceNow);
                if (effectsOverlay != null) {
                    float[] chipCenter = GameAnimations.resolveCenterInRoot(chipView, requireView());
                    animations.triggerRipple(effectsOverlay, chipCenter[0], chipCenter[1], wordColor, reduceNow);
                    animations.emitWordSparkles(effectsOverlay,
                            chipCenter[0], chipCenter[1], 72f, 28f, wordColor, reduceNow);
                }
                animations.triggerWordRevealBlast(
                        null, effectsOverlay, word.getCells(), gridPos[0], gridPos[1], wordColor, reduceNow);

                if (!levelFinished && nowAllFound) {
                    onLevelCompleted(act, prefs, theme);
                } else if (!nowAllFound) {
                    scheduleIdleAnimations();
                }
            }

            @Override
            public void onInvalidSelection() {
                stopIdleAnimationsImmediately();
                act.getSoundManager().playError();
                animations.triggerWrongShake(boardContainer, isReduceMotion());
                vibrateWrongWord(act);
                scheduleIdleAnimations();
            }
        });

        buildWordChips(theme);
        if (hintedWordIdx >= 0 && hintedWordIdx < board.placedWords.size()) {
            PuzzleGenerator.PlacedWord hinted = board.placedWords.get(hintedWordIdx);
            if (!hinted.found) {
                wordGridView.showHintForWord(hinted);
            } else {
                hintedWordIdx = -1;
            }
        }

        if (board.allFound()) {
            levelFinished = true;
            stopTimer();
            stopIdleAnimationsImmediately();
            if (tvTimerPill != null) {
                tvTimerPill.setVisibility(View.GONE);
            }
            showCompletedLevelEntryPopup(act, prefs, theme);
        } else {
            levelFinished = false;
            startTimerIfHardMode(act, prefs, theme, remainingSeconds);
            startLevelEntryAnimations(theme);
        }
    }

    private void startLevelEntryAnimations(GameTheme theme) {
        if (board == null) return;
        boolean reduce = isReduceMotion();
        animations.triggerLevelIntro(tvTitle, boardContainer, reduce, this::scheduleIdleAnimations);
        animations.triggerWordListSlideIn(wordFlowLayout, reduce);
        String toastText = (difficulty == Difficulty.DAILY)
                ? "🌞 Daily Puzzle — Find " + board.placedWords.size() + " Words!"
                : "✨ " + category.displayName + " Level " + level + " — Find " + board.placedWords.size() + " Words!";
        animations.showStartLevelToast(effectsOverlay, toastText, theme, reduce);
    }

    private void showCompletedLevelEntryPopup(MainActivity act, GamePrefs prefs, GameTheme theme) {
        new GamePopupDialog.Builder(requireContext(), theme)
                .setTitle(difficulty == Difficulty.DAILY ? "🌞 Daily Puzzle Completed" : "✅ Level Completed")
                .setMessage("This level is already completed. Do you want to clear this level before playing again?")
                .setPositiveButton("Clear Level", () -> clearLevelForReplay(act, prefs, theme, false))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearLevelForReplay(MainActivity act, GamePrefs prefs, GameTheme theme, boolean showToast) {
        stopTimer();
        stopIdleAnimationsImmediately();
        if (effectsOverlay != null) {
            effectsOverlay.clearAllEffects();
        }
        levelFinished = false;
        hintsUsed = 0;
        hintedWordIdx = -1;
        remainingSeconds = difficulty.timeLimitSeconds;

        if (board != null) {
            for (PuzzleGenerator.PlacedWord pw : board.placedWords) {
                pw.found = false;
                pw.colorIndex = -1;
            }
            wordGridView.setBoard(board);
            wordGridView.setNextColorIdx(0);
            saveCurrentState(prefs, false);
            buildWordChips(theme);
        }
        startTimerIfHardMode(act, prefs, theme, remainingSeconds);
        refreshTopBar(prefs);
        startLevelEntryAnimations(theme);
        if (showToast) {
            Toast.makeText(requireContext(), "Level reset!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCurrentState(GamePrefs prefs, boolean completed) {
        if (prefs == null || board == null) return;
        int nextIdx = wordGridView != null ? wordGridView.getNextColorIdx() : 0;
        prefs.saveLevelState(
                difficulty,
                category,
                level,
                board,
                nextIdx,
                hintsUsed,
                hintedWordIdx,
                remainingSeconds,
                completed
        );
    }

    private void adjustBoardSizeToFit(View contentArea, View boardContainer, View bottomButtons) {
        if (!isAdded() || wordFlowLayout == null) return;
        int availW = contentArea.getWidth();
        int availH = contentArea.getHeight();
        if (availW <= 0 || availH <= 0) return;

        wordFlowLayout.measure(
                View.MeasureSpec.makeMeasureSpec(availW, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int wordsH = wordFlowLayout.getMeasuredHeight();
        int buttonsH = bottomButtons.getHeight();
        if (buttonsH <= 0) {
            bottomButtons.measure(
                    View.MeasureSpec.makeMeasureSpec(availW, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            buttonsH = bottomButtons.getMeasuredHeight();
        }
        int verticalMargins = ThemeHelper.dp(requireContext(), 28);
        int maxBoardForFit = availH - wordsH - buttonsH - verticalMargins;
        int minBoardSize = Math.round(availW * 0.72f);
        int desiredWidth = (maxBoardForFit > 0 && maxBoardForFit < availW)
                ? Math.max(minBoardSize, maxBoardForFit)
                : ViewGroup.LayoutParams.MATCH_PARENT;

        ViewGroup.LayoutParams lp = boardContainer.getLayoutParams();
        if (lp != null && lp.width != desiredWidth) {
            lp.width = desiredWidth;
            boardContainer.post(() -> {
                if (isAdded() && boardContainer.getLayoutParams() != null) {
                    boardContainer.setLayoutParams(lp);
                }
            });
        }
    }

    private void buildWordChips(GameTheme theme) {
        wordFlowLayout.removeAllViews();
        wordChips.clear();
        int padH = ThemeHelper.dp(requireContext(), 9);
        int padV = ThemeHelper.dp(requireContext(), 4);

        for (PuzzleGenerator.PlacedWord pw : board.placedWords) {
            TextView chip = new TextView(requireContext());
            chip.setText(pw.word);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
            chip.setPadding(padH, padV, padH, padV);
            chip.setTextColor(theme.textPrimaryColor);
            ThemeHelper.styleCard(chip, theme.tileColor, 8);
            wordFlowLayout.addView(chip);
            wordChips.put(pw.word, chip);
            if (pw.found) {
                updateWordChipFound(pw.word, theme);
            }
        }
    }

    private void updateWordChipFound(String word, GameTheme theme) {
        TextView chip = wordChips.get(word);
        if (chip != null) {
            chip.setTextColor(theme.primaryButtonColor);
            chip.setPaintFlags(chip.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    private void handleHintTap(MainActivity act, GamePrefs prefs, GameTheme theme) {
        if (!difficulty.hintsAllowed) {
            act.getSoundManager().playError();
            new GamePopupDialog.Builder(requireContext(), theme)
                    .setTitle("💡 Hints Disabled")
                    .setMessage("Hints are disabled in Normal, Hard, and Daily modes!")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        if (board == null || board.allFound()) return;
        if (prefs.getHints() <= 0) {
            act.getSoundManager().playError();
            new GamePopupDialog.Builder(requireContext(), theme)
                    .setTitle("💡 No Hints Left")
                    .setMessage("No hints remaining! You can buy more hints in the Shop.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        for (int i = 0; i < board.placedWords.size(); i++) {
            PuzzleGenerator.PlacedWord pw = board.placedWords.get(i);
            if (!pw.found) {
                if (prefs.consumeHint()) {
                    act.getSoundManager().playClick();
                    hintsUsed++;
                    hintedWordIdx = i;
                    saveCurrentState(prefs, false);
                    refreshTopBar(prefs);
                    wordGridView.showHintForWord(pw);
                    Toast.makeText(requireContext(),
                            "Hint: Look at the highlighted letters for " + pw.word + "!",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void startTimerIfHardMode(MainActivity act, GamePrefs prefs, GameTheme theme, int initialSeconds) {
        stopTimer();
        if (difficulty.timeLimitSeconds <= 0) {
            tvTimerPill.setVisibility(View.GONE);
            return;
        }
        int startSec = initialSeconds > 0 ? initialSeconds : difficulty.timeLimitSeconds;
        remainingSeconds = startSec;
        tvTimerPill.setVisibility(View.VISIBLE);
        updateTimerText(startSec);
        hardTimer = new CountDownTimer(startSec * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isAdded()) return;
                remainingSeconds = (int) Math.ceil(millisUntilFinished / 1000.0);
                updateTimerText(remainingSeconds);
            }

            @Override
            public void onFinish() {
                if (!isAdded() || levelFinished) return;
                remainingSeconds = 0;
                updateTimerText(0);
                onHardModeTimeUp(act, prefs, theme);
            }
        }.start();
    }

    private void updateTimerText(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        tvTimerPill.setText(String.format(Locale.US, "⏱ %02d:%02d", min, sec));
    }

    private void stopTimer() {
        if (hardTimer != null) {
            hardTimer.cancel();
            hardTimer = null;
        }
    }

    private void onHardModeTimeUp(MainActivity act, GamePrefs prefs, GameTheme theme) {
        levelFinished = true;
        act.getSoundManager().playError();
        new GamePopupDialog.Builder(requireContext(), theme)
                .setTitle("⏰ Time's Up!")
                .setMessage("Hard mode 120-second timer expired before all words were found.")
                .setCancelable(false)
                .setPositiveButton("Retry", () -> clearLevelForReplay(act, prefs, theme, false))
                .setNegativeButton("Back", act::navigateBack)
                .show();
    }

    private void onLevelCompleted(MainActivity act, GamePrefs prefs, GameTheme theme) {
        if (levelFinished) return;
        levelFinished = true;
        stopTimer();
        stopIdleAnimationsImmediately();
        act.getSoundManager().playLevelComplete();

        boolean alreadyRewarded = prefs.isLevelRewardClaimed(difficulty, category, level);
        boolean firstClear = prefs.markLevelCompleted(difficulty, category, level);
        saveCurrentState(prefs, true);

        boolean grantReward = firstClear && !alreadyRewarded;
        int reward = grantReward ? difficulty.rewardCoins : 0;
        if (grantReward && reward > 0) {
            prefs.addCoins(reward);
        }
        refreshTopBar(prefs);
        act.checkAndNotifyAchievements();

        String ribbonText = (difficulty == Difficulty.DAILY) ? "🌞 DAILY COMPLETE!" : "🎉 LEVEL COMPLETE!";
        animations.runLevelCompletionSequence(
                boardContainer,
                wordScroll,
                effectsOverlay,
                ribbonText,
                theme,
                isReduceMotion(),
                () -> showCompletionDialog(act, theme, grantReward, reward)
        );
    }

    private void showCompletionDialog(MainActivity act, GameTheme theme, boolean grantReward, int reward) {
        if (!isAdded()) return;
        String title = (difficulty == Difficulty.DAILY) ? "🌞 Daily Puzzle Complete!" : "🎉 Level Complete!";
        if (grantReward) {
            String message = "Great job! You found all words and earned +" + reward + " ⭐ coins!";
            String nextLabel = (difficulty != Difficulty.DAILY && level < 100) ? "Next Level" : "Awesome!";
            new GamePopupDialog.Builder(requireContext(), theme)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(nextLabel, () -> {
                        if (difficulty != Difficulty.DAILY && level < 100) {
                            act.navigateTo(GameFragment.newInstance(difficulty, category, level + 1), false);
                        } else {
                            act.navigateBack();
                        }
                    })
                    .setNegativeButton("Back", act::navigateBack)
                    .show();
        } else {
            new GamePopupDialog.Builder(requireContext(), theme)
                    .setTitle(title)
                    .setMessage("Great job! You completed this level again.\n(Completion reward already claimed)")
                    .setCancelable(false)
                    .setPositiveButton("Next", () -> {
                        if (difficulty != Difficulty.DAILY && level < 100) {
                            act.navigateTo(GameFragment.newInstance(difficulty, category, level + 1), false);
                        } else {
                            act.navigateBack();
                        }
                    })
                    .setNegativeButton("Back", act::navigateBack)
                    .show();
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrateWrongWord(Context context) {
        if (context == null) return;
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = (vm != null) ? vm.getDefaultVibrator() : null;
            } else {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(
                            GameAnimations.WRONG_WORD_VIBRATE_PATTERN, -1));
                } else {
                    vibrator.vibrate(GameAnimations.WRONG_WORD_VIBRATE_PATTERN, -1);
                }
            }
        } catch (Throwable ignored) {}
    }

    public int getHintsUsedForCurrentLevel() {
        return hintsUsed;
    }

    public GameAnimations getAnimations() {
        return animations;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        idleHandler.removeCallbacksAndMessages(null);
        animations.stopAll(btnHint, effectsOverlay);
        if (prefsRef != null && economyListener != null) {
            prefsRef.removeEconomyChangeListener(economyListener);
        }
        stopTimer();
    }
}

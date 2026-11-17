package com.word.nova.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.word.nova.model.Category;
import com.word.nova.model.GameTheme;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs genuine startup initialization on a background worker thread and reports
 * real milestone percentages (1..100) back to the main thread for SplashActivity.
 */
public class GameInitializer {

    public interface Callback {
        void onProgress(int percent, @NonNull String statusMessage);

        void onComplete();

        void onError(@NonNull Throwable error);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WordNova-Init");
        t.setDaemon(true);
        return t;
    });
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public void start(@NonNull Context context, @NonNull Callback callback) {
        final Context appCtx = context.getApplicationContext();
        cancelled.set(false);
        executor.execute(() -> {
            try {
                if (cancelled.get()) return;
                dispatchProgress(callback, 4, "Loading Word Nova...");

                // 1. Parse all supplied themes from assets/themes.json
                GameTheme.ensureInitialized(appCtx);
                if (GameTheme.values().length == 0) {
                    throw new IllegalStateException("No themes loaded from assets.");
                }
                if (cancelled.get()) return;
                dispatchProgress(callback, 14, "Loading themes...");

                // 2. Parse word category definitions from assets/words/categories.json
                Category.ensureInitialized(appCtx);
                if (Category.values().length == 0) {
                    throw new IllegalStateException("No word categories loaded from assets.");
                }
                if (cancelled.get()) return;
                dispatchProgress(callback, 22, "Loading categories...");

                // 3. Load and index each category's word database (22% -> 86%)
                WordBankRepository.ensureInitialized(appCtx, (loadedCount, totalCount, categoryName) -> {
                    if (cancelled.get() || totalCount <= 0) return;
                    int wordProgress = 22 + Math.round((loadedCount * 64f) / totalCount);
                    dispatchProgress(callback, Math.min(86, wordProgress),
                            "Loading " + categoryName.toLowerCase() + " words...");
                });
                if (cancelled.get()) return;

                // 4. Initialize player preferences, active theme, coins, hints, and saved progress
                GamePrefs prefs = new GamePrefs(appCtx);
                prefs.getCurrentTheme();
                if (cancelled.get()) return;
                dispatchProgress(callback, 93, "Restoring progress...");

                // 5. Warm daily puzzle pool and verify level-1 word bank readiness
                List<String> dailyWords = WordBankRepository.getDailyWords(GamePrefs.todayDateKey());
                List<String> generalSample = WordBankRepository.getAllWordsForCategory(Category.GENERAL);
                if (dailyWords.isEmpty() || generalSample.isEmpty()) {
                    throw new IllegalStateException("Word database verification failed.");
                }
                if (cancelled.get()) return;
                dispatchProgress(callback, 100, "Ready!");

                mainHandler.post(() -> {
                    if (!cancelled.get()) {
                        callback.onComplete();
                    }
                });
            } catch (Throwable t) {
                mainHandler.post(() -> {
                    if (!cancelled.get()) {
                        callback.onError(t);
                    }
                });
            }
        });
    }

    private void dispatchProgress(@NonNull Callback callback, int percent, @NonNull String message) {
        final int clamped = Math.max(1, Math.min(100, percent));
        mainHandler.post(() -> {
            if (!cancelled.get()) {
                callback.onProgress(clamped, message);
            }
        });
    }

    public void cancel() {
        cancelled.set(true);
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
    }
}

package com.word.nova;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.word.nova.data.GameInitializer;
import com.word.nova.ui.ThemeHelper;

public class SplashActivity extends AppCompatActivity {

    private static final long STEP_INTERVAL_MS = 10L;

    private ProgressBar progressBar;
    private TextView tvPercent;
    private TextView tvStatus;
    private View errorContainer;
    private TextView tvError;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private GameInitializer initializer;

    private int displayedPercent = 1;
    private int targetPercent = 1;
    private boolean initializationReady = false;
    private boolean hasError = false;
    private boolean navigatedToMain = false;
    private boolean tickerScheduled = false;
    private boolean isForeground = false;

    private final Runnable progressStepRunnable = new Runnable() {
        @Override
        public void run() {
            tickerScheduled = false;
            if (isFinishing() || isDestroyed() || hasError || navigatedToMain) {
                return;
            }
            if (displayedPercent < targetPercent) {
                displayedPercent = Math.min(100, displayedPercent + 1);
                renderProgress(displayedPercent);
            }
            if (initializationReady && displayedPercent >= 100) {
                transitionToMainScreen();
            } else if (displayedPercent < targetPercent) {
                scheduleNextStep();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_splash);
        configureCosmicSystemBars();

        View bottomContainer = findViewById(R.id.splash_bottom_container);
        final int baseBottomPad = bottomContainer.getPaddingBottom();
        final int baseLeftPad = bottomContainer.getPaddingLeft();
        final int baseRightPad = bottomContainer.getPaddingRight();
        final int baseTopPad = bottomContainer.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(bottomContainer, (v, windowInsets) -> {
            Insets navInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.navigationBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(
                    baseLeftPad + navInsets.left,
                    baseTopPad,
                    baseRightPad + navInsets.right,
                    baseBottomPad + navInsets.bottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(bottomContainer);

        progressBar = findViewById(R.id.pb_splash_loading);
        tvPercent = findViewById(R.id.tv_splash_percent);
        tvStatus = findViewById(R.id.tv_splash_status);
        errorContainer = findViewById(R.id.splash_error_container);
        tvError = findViewById(R.id.tv_splash_error);
        Button btnRetry = findViewById(R.id.btn_splash_retry);

        ThemeHelper.styleButton(btnRetry, Color.parseColor("#2563EB"), Color.WHITE, 12);
        btnRetry.setOnClickListener(v -> startInitialization());

        startInitialization();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;
        if (initializationReady && displayedPercent >= 100 && !navigatedToMain && !hasError) {
            transitionToMainScreen();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isForeground = false;
    }

    @SuppressWarnings("deprecation")
    private void configureCosmicSystemBars() {
        if (getWindow() == null) return;
        int splashColor = Color.parseColor("#040715");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT < 35) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().setStatusBarColor(splashColor);
            getWindow().setNavigationBarColor(splashColor);
        }
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.show(WindowInsetsCompat.Type.systemBars());
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }
    }

    private void startInitialization() {
        if (initializer != null) {
            initializer.cancel();
        }
        uiHandler.removeCallbacks(progressStepRunnable);
        tickerScheduled = false;
        hasError = false;
        initializationReady = false;
        navigatedToMain = false;
        displayedPercent = 1;
        targetPercent = 1;

        errorContainer.setVisibility(View.GONE);
        tvStatus.setText(R.string.splash_loading_label);
        renderProgress(displayedPercent);

        initializer = new GameInitializer();
        initializer.start(getApplicationContext(), new GameInitializer.Callback() {
            @Override
            public void onProgress(int percent, @NonNull String statusMessage) {
                if (isFinishing() || isDestroyed() || hasError || navigatedToMain) return;
                int clamped = Math.max(1, Math.min(100, percent));
                if (clamped > targetPercent) {
                    targetPercent = clamped;
                }
                tvStatus.setText(statusMessage);
                if (displayedPercent < targetPercent) {
                    scheduleNextStep();
                }
            }

            @Override
            public void onComplete() {
                if (isFinishing() || isDestroyed() || hasError || navigatedToMain) return;
                initializationReady = true;
                targetPercent = 100;
                tvStatus.setText("Ready!");
                if (displayedPercent >= 100) {
                    transitionToMainScreen();
                } else {
                    scheduleNextStep();
                }
            }

            @Override
            public void onError(@NonNull Throwable error) {
                handleInitializationError(error);
            }
        });
    }

    void handleInitializationError(@NonNull Throwable error) {
        if (isFinishing() || isDestroyed() || navigatedToMain) return;
        hasError = true;
        uiHandler.removeCallbacks(progressStepRunnable);
        tickerScheduled = false;
        String detail = error.getMessage();
        tvStatus.setText("Initialization failed");
        tvError.setText(detail != null && !detail.trim().isEmpty()
                ? getString(R.string.splash_error_message) + "\n(" + detail + ")"
                : getString(R.string.splash_error_message));
        errorContainer.setVisibility(View.VISIBLE);
    }

    private void scheduleNextStep() {
        if (tickerScheduled || hasError || navigatedToMain) return;
        tickerScheduled = true;
        uiHandler.postDelayed(progressStepRunnable, STEP_INTERVAL_MS);
    }

    private void renderProgress(int percent) {
        int clamped = Math.max(1, Math.min(100, percent));
        if (progressBar != null) {
            progressBar.setProgress(clamped);
        }
        if (tvPercent != null) {
            tvPercent.setText(clamped + "%");
        }
    }

    @SuppressWarnings("deprecation")
    private void transitionToMainScreen() {
        if (navigatedToMain || isFinishing() || isDestroyed() || !isForeground) return;
        navigatedToMain = true;
        uiHandler.removeCallbacks(progressStepRunnable);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacksAndMessages(null);
        if (initializer != null) {
            initializer.cancel();
        }
    }
}

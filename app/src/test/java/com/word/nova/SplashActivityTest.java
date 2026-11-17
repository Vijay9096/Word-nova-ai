package com.word.nova;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class SplashActivityTest {

    @Test
    public void testSplashActivityIsLauncherEntryPointWithUndistortedArtwork() {
        ActivityController<SplashActivity> controller = Robolectric.buildActivity(SplashActivity.class);
        SplashActivity splash = controller.create().start().resume().visible().get();

        PackageManager pm = splash.getPackageManager();
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setPackage(splash.getPackageName());
        List<ResolveInfo> resolved = pm.queryIntentActivities(launchIntent, 0);
        assertFalse("Launcher activity should resolve", resolved.isEmpty());
        assertEquals(SplashActivity.class.getName(), resolved.get(0).activityInfo.name);

        ImageView art = splash.findViewById(R.id.iv_splash_art);
        assertNotNull(art);
        assertNotNull(art.getDrawable());
        assertEquals("Splash artwork must use FIT_CENTER to avoid distortion/cropping",
                ImageView.ScaleType.FIT_CENTER, art.getScaleType());

        controller.pause().stop().destroy();
    }

    @Test
    public void testSplashLoadingIncrementsSequentiallySyncedAndTransitionsToMainActivity() throws Exception {
        ActivityController<SplashActivity> controller = Robolectric.buildActivity(SplashActivity.class);
        SplashActivity splash = controller.create().get();

        ProgressBar progressBar = splash.findViewById(R.id.pb_splash_loading);
        TextView tvPercent = splash.findViewById(R.id.tv_splash_percent);
        assertNotNull(progressBar);
        assertNotNull(tvPercent);
        assertEquals(100, progressBar.getMax());
        assertEquals(1, progressBar.getProgress());
        assertEquals("1%", tvPercent.getText().toString());

        List<Integer> observedPercentages = new ArrayList<>();
        observedPercentages.add(progressBar.getProgress());

        // Observe every rendered text update to verify strict 1%, 2%, 3%, ... 99%, 100% progression
        // synchronized 1:1 with ProgressBar progress.
        tvPercent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int pbVal = progressBar.getProgress();
                assertEquals("Percent text must stay synchronized with progress bar", pbVal + "%", s.toString());
                assertTrue("Progress must never exceed 100%", pbVal <= 100);
                int last = observedPercentages.get(observedPercentages.size() - 1);
                if (pbVal != last) {
                    assertEquals("Percentage must increment sequentially by +1%", last + 1, pbVal);
                    observedPercentages.add(pbVal);
                }
            }
        });

        controller.start().resume().visible();

        ShadowActivity shadowSplash = Shadows.shadowOf(splash);
        long deadline = System.currentTimeMillis() + 5000L;
        while (!splash.isFinishing() && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS);
            if (!splash.isFinishing()) {
                Thread.sleep(5);
            }
        }

        assertTrue("Splash must finish cleanly once loaded", splash.isFinishing());
        assertEquals("Progress must reach 100%", Integer.valueOf(100),
                observedPercentages.get(observedPercentages.size() - 1));
        assertEquals("All sequential percentages 1..100 must be rendered", 100, observedPercentages.size());

        Intent nextIntent = shadowSplash.getNextStartedActivity();
        assertNotNull("Splash must transition to MainActivity", nextIntent);
        assertEquals(MainActivity.class.getName(), nextIntent.getComponent().getClassName());

        controller.pause().stop().destroy();
    }

    @Test
    public void testSplashHandlesLoadingErrorGracefullyAndRecoversOnRetry() throws Exception {
        ActivityController<SplashActivity> controller = Robolectric.buildActivity(SplashActivity.class);
        SplashActivity splash = controller.create().start().resume().visible().get();

        View errorContainer = splash.findViewById(R.id.splash_error_container);
        Button btnRetry = splash.findViewById(R.id.btn_splash_retry);
        ProgressBar progressBar = splash.findViewById(R.id.pb_splash_loading);
        assertEquals(View.GONE, errorContainer.getVisibility());

        splash.handleInitializationError(new IllegalStateException("Asset read error"));
        assertEquals(View.VISIBLE, errorContainer.getVisibility());
        assertFalse("Splash must not finish in error state", splash.isFinishing());

        // Clicking Retry recovers cleanly and completes loading to MainActivity
        btnRetry.performClick();
        assertEquals(View.GONE, errorContainer.getVisibility());

        ShadowActivity shadowSplash = Shadows.shadowOf(splash);
        long deadline = System.currentTimeMillis() + 5000L;
        while (!splash.isFinishing() && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS);
            if (!splash.isFinishing()) {
                Thread.sleep(5);
            }
        }

        assertTrue("Splash must finish after successful retry", splash.isFinishing());
        assertEquals(100, progressBar.getProgress());
        Intent nextIntent = shadowSplash.getNextStartedActivity();
        assertNotNull("Splash must transition to MainActivity after retry", nextIntent);
        assertEquals(MainActivity.class.getName(), nextIntent.getComponent().getClassName());

        controller.pause().stop().destroy();
    }
}

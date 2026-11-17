package com.word.nova.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.word.nova.model.GameTheme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Lightweight overlay View for Word Nova gameplay visual effects:
 * word sparkles, reveal blasts, ripples, center & burst confetti, fireworks,
 * level-start toast banner, and level-complete celebratory ribbon.
 */
public class EffectsOverlayView extends View {

    private static final int KIND_SPARKLE = 0;
    private static final int KIND_CONFETTI = 1;
    private static final int KIND_BLAST = 2;
    private static final int KIND_FIREWORK = 3;

    private static class Particle {
        int kind;
        float x, y;
        float vx, vy;
        float gravity;
        float drag;
        float size;
        float widthFactor;
        float rotation;
        float rotSpeed;
        int color;
        long startTimeMs;
        long durationMs;
        float alpha = 1f;
        float currentScale = 1f;
    }

    private static class RippleWave {
        float cx, cy;
        float maxRadius;
        int color;
        long startTimeMs;
        long durationMs;
        float progress;
    }

    private static class FloatingBanner {
        String text;
        int fillColor;
        int accentColor;
        int textColor;
        long startTimeMs;
        long durationMs;
        boolean isRibbon;
    }

    private static final int[] CELEBRATION_COLORS = {
            Color.parseColor("#FBBF24"), // Gold
            Color.parseColor("#EC407A"), // Pink
            Color.parseColor("#42A5F5"), // Sky Blue
            Color.parseColor("#2ECC71"), // Emerald
            Color.parseColor("#AB47BC"), // Violet
            Color.parseColor("#FF7043"), // Coral
            Color.parseColor("#26C6DA")  // Cyan
    };

    private final List<Particle> particles = new ArrayList<>();
    private final List<RippleWave> ripples = new ArrayList<>();
    private final List<FloatingBanner> banners = new ArrayList<>();
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Runnable> scheduledCallbacks = new ArrayList<>();

    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bannerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bannerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path starPath = new Path();
    private final Path ribbonPath = new Path();
    private final RectF bannerRect = new RectF();

    private ValueAnimator frameAnimator;
    private long lastTickMs = 0L;

    public EffectsOverlayView(Context context) {
        super(context);
        init();
    }

    public EffectsOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EffectsOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(false);
        setFocusable(false);
        strokePaint.setStyle(Paint.Style.STROKE);
        bannerStrokePaint.setStyle(Paint.Style.STROKE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void ensureLoopRunning() {
        if (particles.isEmpty() && ripples.isEmpty() && banners.isEmpty()) {
            return;
        }
        if (frameAnimator != null && frameAnimator.isRunning()) {
            return;
        }
        lastTickMs = android.os.SystemClock.uptimeMillis();
        frameAnimator = ValueAnimator.ofFloat(0f, 1f);
        frameAnimator.setDuration(1000L);
        frameAnimator.setRepeatCount(ValueAnimator.INFINITE);
        frameAnimator.setInterpolator(new LinearInterpolator());
        frameAnimator.addUpdateListener(anim -> stepSimulation());
        frameAnimator.start();
    }

    private void stopLoopIfIdle() {
        if (particles.isEmpty() && ripples.isEmpty() && banners.isEmpty()) {
            if (frameAnimator != null) {
                frameAnimator.cancel();
                frameAnimator = null;
            }
        }
    }

    private void stepSimulation() {
        long now = android.os.SystemClock.uptimeMillis();
        float dt = Math.min(0.05f, Math.max(0.004f, (now - lastTickMs) / 1000f));
        lastTickMs = now;

        Iterator<Particle> pIt = particles.iterator();
        while (pIt.hasNext()) {
            Particle p = pIt.next();
            long elapsed = now - p.startTimeMs;
            if (elapsed >= p.durationMs) {
                pIt.remove();
                continue;
            }
            float t = (float) elapsed / p.durationMs;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += p.gravity * dt;
            float damping = (float) Math.pow(p.drag, dt * 60f);
            p.vx *= damping;
            p.vy *= damping;
            p.rotation += p.rotSpeed * dt;

            if (p.kind == KIND_SPARKLE) {
                p.currentScale = (float) Math.sin(t * Math.PI);
                p.alpha = 1f - (t * t);
            } else if (p.kind == KIND_BLAST) {
                p.currentScale = 1f - t * 0.55f;
                p.alpha = Math.max(0f, 1f - t);
            } else {
                p.currentScale = 1f;
                p.alpha = t > 0.72f ? Math.max(0f, (1f - t) / 0.28f) : 1f;
            }
        }

        Iterator<RippleWave> rIt = ripples.iterator();
        while (rIt.hasNext()) {
            RippleWave rw = rIt.next();
            long elapsed = now - rw.startTimeMs;
            if (elapsed >= rw.durationMs) {
                rIt.remove();
                continue;
            }
            rw.progress = (float) elapsed / rw.durationMs;
        }

        Iterator<FloatingBanner> bIt = banners.iterator();
        while (bIt.hasNext()) {
            FloatingBanner fb = bIt.next();
            long elapsed = now - fb.startTimeMs;
            if (elapsed >= fb.durationMs) {
                bIt.remove();
            }
        }

        invalidate();
        stopLoopIfIdle();
    }

    public void emitWordSparkles(float cx, float cy, float spreadW, float spreadH, int baseColor) {
        long now = android.os.SystemClock.uptimeMillis();
        float halfW = Math.max(dp(24), spreadW * 0.55f);
        float halfH = Math.max(dp(18), spreadH * 0.65f);
        for (int i = 0; i < 16; i++) {
            Particle p = new Particle();
            p.kind = KIND_SPARKLE;
            p.x = cx + (random.nextFloat() * 2f - 1f) * halfW;
            p.y = cy + (random.nextFloat() * 2f - 1f) * halfH;
            double angle = random.nextDouble() * Math.PI * 2.0;
            float speed = dp(28 + random.nextFloat() * 52);
            p.vx = (float) Math.cos(angle) * speed;
            p.vy = (float) Math.sin(angle) * speed - dp(14);
            p.gravity = dp(20);
            p.drag = 0.96f;
            p.size = dp(4.5f + random.nextFloat() * 4.5f);
            p.rotation = random.nextFloat() * 360f;
            p.rotSpeed = (random.nextFloat() - 0.5f) * 180f;
            p.color = (i % 3 == 0) ? Color.parseColor("#FBBF24")
                    : (i % 3 == 1 ? Color.WHITE : ColorUtils.blendARGB(baseColor, Color.WHITE, 0.35f));
            p.startTimeMs = now + (i * 12L);
            p.durationMs = 560L + random.nextInt(180);
            particles.add(p);
        }
        ensureLoopRunning();
        invalidate();
    }

    public void triggerWordRevealBlast(float cx, float cy, int baseColor) {
        long now = android.os.SystemClock.uptimeMillis();
        RippleWave shockwave = new RippleWave();
        shockwave.cx = cx;
        shockwave.cy = cy;
        shockwave.maxRadius = dp(92);
        shockwave.color = ColorUtils.blendARGB(baseColor, Color.WHITE, 0.30f);
        shockwave.startTimeMs = now;
        shockwave.durationMs = 460L;
        ripples.add(shockwave);

        for (int i = 0; i < 18; i++) {
            Particle p = new Particle();
            p.kind = KIND_BLAST;
            p.x = cx;
            p.y = cy;
            double angle = (2.0 * Math.PI * i / 18.0) + (random.nextFloat() - 0.5f) * 0.25;
            float speed = dp(120 + random.nextFloat() * 140);
            p.vx = (float) Math.cos(angle) * speed;
            p.vy = (float) Math.sin(angle) * speed;
            p.gravity = dp(40);
            p.drag = 0.92f;
            p.size = dp(3.5f + random.nextFloat() * 3.5f);
            p.rotation = (float) Math.toDegrees(angle);
            p.rotSpeed = 0f;
            p.color = (i % 2 == 0) ? baseColor : Color.parseColor("#FDE68A");
            p.startTimeMs = now;
            p.durationMs = 480L + random.nextInt(140);
            particles.add(p);
        }
        ensureLoopRunning();
        invalidate();
    }

    public void triggerRipple(float cx, float cy, int color) {
        long now = android.os.SystemClock.uptimeMillis();
        for (int i = 0; i < 2; i++) {
            RippleWave rw = new RippleWave();
            rw.cx = cx;
            rw.cy = cy;
            rw.maxRadius = dp(58 + i * 24);
            rw.color = color;
            rw.startTimeMs = now + i * 70L;
            rw.durationMs = 420L;
            ripples.add(rw);
        }
        ensureLoopRunning();
        invalidate();
    }

    public void dropConfettiFromCenterAllDir(float cx, float cy) {
        long now = android.os.SystemClock.uptimeMillis();
        for (int i = 0; i < 48; i++) {
            Particle p = new Particle();
            p.kind = KIND_CONFETTI;
            p.x = cx + (random.nextFloat() - 0.5f) * dp(18);
            p.y = cy + (random.nextFloat() - 0.5f) * dp(18);
            double angle = (2.0 * Math.PI * i / 48.0) + (random.nextDouble() - 0.5) * 0.3;
            float speed = dp(130 + random.nextFloat() * 210);
            p.vx = (float) Math.cos(angle) * speed;
            p.vy = (float) Math.sin(angle) * speed - dp(60);
            p.gravity = dp(280);
            p.drag = 0.97f;
            p.size = dp(6 + random.nextFloat() * 4);
            p.widthFactor = 0.55f + random.nextFloat() * 0.45f;
            p.rotation = random.nextFloat() * 360f;
            p.rotSpeed = (random.nextFloat() - 0.5f) * 420f;
            p.color = CELEBRATION_COLORS[i % CELEBRATION_COLORS.length];
            p.startTimeMs = now;
            p.durationMs = 1300L + random.nextInt(400);
            particles.add(p);
        }
        ensureLoopRunning();
        invalidate();
    }

    public void dropBurstConfetti() {
        long now = android.os.SystemClock.uptimeMillis();
        float w = getWidth() > 0 ? getWidth() : dp(360);
        float h = getHeight() > 0 ? getHeight() : dp(640);
        float[] originX = {w * 0.22f, w * 0.50f, w * 0.78f};
        float originY = h * 0.14f;

        for (int b = 0; b < originX.length; b++) {
            for (int i = 0; i < 15; i++) {
                Particle p = new Particle();
                p.kind = KIND_CONFETTI;
                p.x = originX[b];
                p.y = originY;
                double angle = Math.toRadians(35 + random.nextDouble() * 110);
                float speed = dp(110 + random.nextFloat() * 190);
                p.vx = (float) Math.cos(angle) * speed * (random.nextBoolean() ? 1f : -0.6f);
                p.vy = -dp(90 + random.nextFloat() * 90);
                p.gravity = dp(340);
                p.drag = 0.98f;
                p.size = dp(6 + random.nextFloat() * 4);
                p.widthFactor = 0.55f + random.nextFloat() * 0.45f;
                p.rotation = random.nextFloat() * 360f;
                p.rotSpeed = (random.nextFloat() - 0.5f) * 440f;
                p.color = CELEBRATION_COLORS[(b * 5 + i) % CELEBRATION_COLORS.length];
                p.startTimeMs = now + b * 90L;
                p.durationMs = 1400L + random.nextInt(350);
                particles.add(p);
            }
        }
        ensureLoopRunning();
        invalidate();
    }

    public void triggerFireworks() {
        float w = getWidth() > 0 ? getWidth() : dp(360);
        float h = getHeight() > 0 ? getHeight() : dp(640);
        float[][] bursts = {
                {w * 0.26f, h * 0.28f},
                {w * 0.74f, h * 0.24f},
                {w * 0.50f, h * 0.20f}
        };
        for (int b = 0; b < bursts.length; b++) {
            final float bx = bursts[b][0];
            final float by = bursts[b][1];
            final int burstColor = CELEBRATION_COLORS[(b * 2) % CELEBRATION_COLORS.length];
            Runnable burstRun = () -> spawnFireworkBurst(bx, by, burstColor);
            if (b == 0) {
                burstRun.run();
            } else {
                scheduledCallbacks.add(burstRun);
                handler.postDelayed(burstRun, b * 220L);
            }
        }
    }

    private void spawnFireworkBurst(float cx, float cy, int primaryColor) {
        long now = android.os.SystemClock.uptimeMillis();
        RippleWave flash = new RippleWave();
        flash.cx = cx;
        flash.cy = cy;
        flash.maxRadius = dp(74);
        flash.color = primaryColor;
        flash.startTimeMs = now;
        flash.durationMs = 420L;
        ripples.add(flash);

        for (int i = 0; i < 24; i++) {
            Particle p = new Particle();
            p.kind = KIND_FIREWORK;
            p.x = cx;
            p.y = cy;
            double angle = 2.0 * Math.PI * i / 24.0;
            float speed = dp(95 + (i % 2) * 55 + random.nextFloat() * 35);
            p.vx = (float) Math.cos(angle) * speed;
            p.vy = (float) Math.sin(angle) * speed;
            p.gravity = dp(110);
            p.drag = 0.94f;
            p.size = dp(3.5f + random.nextFloat() * 2.5f);
            p.color = (i % 3 == 0) ? Color.WHITE : primaryColor;
            p.startTimeMs = now;
            p.durationMs = 680L + random.nextInt(180);
            particles.add(p);
        }
        ensureLoopRunning();
        invalidate();
    }

    public void showStartLevelToast(String text, GameTheme theme) {
        banners.removeIf(b -> !b.isRibbon);
        FloatingBanner fb = new FloatingBanner();
        fb.text = text;
        fb.fillColor = theme != null ? theme.surfaceColor : Color.parseColor("#262A3E");
        fb.accentColor = theme != null ? theme.primaryButtonColor : Color.parseColor("#42A5F5");
        fb.textColor = theme != null ? theme.textPrimaryColor : Color.WHITE;
        fb.startTimeMs = android.os.SystemClock.uptimeMillis();
        fb.durationMs = 1750L;
        fb.isRibbon = false;
        banners.add(fb);
        ensureLoopRunning();
        invalidate();
    }

    public void showLevelCompleteRibbon(String text, GameTheme theme) {
        banners.removeIf(b -> b.isRibbon);
        FloatingBanner fb = new FloatingBanner();
        fb.text = text;
        fb.fillColor = theme != null ? theme.primaryButtonColor : Color.parseColor("#42A5F5");
        fb.accentColor = Color.parseColor("#FBBF24");
        fb.textColor = theme != null ? ThemeHelper.contrastTextOn(fb.fillColor) : Color.WHITE;
        fb.startTimeMs = android.os.SystemClock.uptimeMillis();
        fb.durationMs = 2200L;
        fb.isRibbon = true;
        banners.add(fb);
        ensureLoopRunning();
        invalidate();
    }

    public void clearAllEffects() {
        for (Runnable r : scheduledCallbacks) {
            handler.removeCallbacks(r);
        }
        scheduledCallbacks.clear();
        particles.clear();
        ripples.clear();
        banners.clear();
        if (frameAnimator != null) {
            frameAnimator.cancel();
            frameAnimator = null;
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearAllEffects();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = android.os.SystemClock.uptimeMillis();

        // 1. Ripples & Reveal shockwaves
        for (RippleWave rw : ripples) {
            long elapsed = now - rw.startTimeMs;
            if (elapsed < 0 || elapsed >= rw.durationMs) continue;
            float p = (float) elapsed / rw.durationMs;
            float eased = 1f - (1f - p) * (1f - p);
            float radius = rw.maxRadius * eased;
            int alpha = Math.round((1f - p) * 190f);

            glowPaint.setColor(ColorUtils.setAlphaComponent(rw.color, alpha / 3));
            canvas.drawCircle(rw.cx, rw.cy, radius * 0.92f, glowPaint);

            strokePaint.setStrokeWidth(Math.max(dp(1.5f), dp(6f) * (1f - p)));
            strokePaint.setColor(ColorUtils.setAlphaComponent(rw.color, alpha));
            canvas.drawCircle(rw.cx, rw.cy, radius, strokePaint);
        }

        // 2. Particles
        for (Particle p : particles) {
            if (now < p.startTimeMs) continue;
            int alphaByte = Math.max(0, Math.min(255, Math.round(p.alpha * 255f)));
            if (alphaByte <= 0) continue;
            particlePaint.setColor(ColorUtils.setAlphaComponent(p.color, alphaByte));

            canvas.save();
            canvas.translate(p.x, p.y);
            canvas.rotate(p.rotation);

            if (p.kind == KIND_SPARKLE) {
                float r = Math.max(dp(1.5f), p.size * p.currentScale);
                drawFourPointStar(canvas, r);
            } else if (p.kind == KIND_CONFETTI) {
                float hw = p.size * 0.5f;
                float hh = p.size * 0.5f * p.widthFactor;
                canvas.drawRoundRect(-hw, -hh, hw, hh, dp(1.5f), dp(1.5f), particlePaint);
            } else {
                float r = Math.max(dp(1.5f), p.size * p.currentScale);
                canvas.drawCircle(0f, 0f, r, particlePaint);
            }
            canvas.restore();
        }

        // 3. Banners (Start-Level Toast Pill & Level-Complete Ribbon)
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        for (FloatingBanner fb : banners) {
            long elapsed = now - fb.startTimeMs;
            if (elapsed < 0 || elapsed >= fb.durationMs) continue;
            float t = (float) elapsed / fb.durationMs;

            float alphaFactor;
            if (t < 0.15f) {
                alphaFactor = t / 0.15f;
            } else if (t > 0.80f) {
                alphaFactor = (1f - t) / 0.20f;
            } else {
                alphaFactor = 1f;
            }
            alphaFactor = Math.max(0f, Math.min(1f, alphaFactor));

            if (fb.isRibbon) {
                drawLevelCompleteRibbonBanner(canvas, w, h, fb, t, alphaFactor);
            } else {
                drawStartToastBanner(canvas, w, h, fb, t, alphaFactor);
            }
        }
    }

    private void drawStartToastBanner(Canvas canvas, float w, float h,
                                      FloatingBanner fb, float t, float alphaFactor) {
        textPaint.setTextSize(dp(14.5f));
        float textW = textPaint.measureText(fb.text);
        float pillW = Math.min(w * 0.86f, textW + dp(40));
        float pillH = dp(40);
        float slideOffset = (t < 0.15f) ? dp(14) * (1f - t / 0.15f) : 0f;
        float top = h * 0.14f + slideOffset;
        float left = (w - pillW) / 2f;

        int alpha = Math.round(alphaFactor * 245);
        bannerRect.set(left, top, left + pillW, top + pillH);
        bannerPaint.setShader(null);
        bannerPaint.setColor(ColorUtils.setAlphaComponent(fb.fillColor, alpha));
        canvas.drawRoundRect(bannerRect, dp(20), dp(20), bannerPaint);

        bannerStrokePaint.setStrokeWidth(dp(2f));
        bannerStrokePaint.setColor(ColorUtils.setAlphaComponent(fb.accentColor, alpha));
        canvas.drawRoundRect(bannerRect, dp(20), dp(20), bannerStrokePaint);

        textPaint.setColor(ColorUtils.setAlphaComponent(fb.textColor, alpha));
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float cy = bannerRect.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(fb.text, w / 2f, cy, textPaint);
    }

    private void drawLevelCompleteRibbonBanner(Canvas canvas, float w, float h,
                                               FloatingBanner fb, float t, float alphaFactor) {
        float enterScale = (t < 0.18f)
                ? 0.72f + 0.28f * (t / 0.18f)
                : 1.0f;
        float ribbonW = Math.min(w * 0.88f, dp(310));
        float ribbonH = dp(52);
        float tailDepth = dp(14);
        float cx = w / 2f;
        float cy = h * 0.28f;

        int alpha = Math.round(alphaFactor * 255);
        int cStart = ColorUtils.setAlphaComponent(fb.fillColor, alpha);
        int cEnd = ColorUtils.setAlphaComponent(
                ColorUtils.blendARGB(fb.fillColor, fb.accentColor, 0.35f), alpha);

        canvas.save();
        canvas.scale(enterScale, enterScale, cx, cy);

        float l = cx - ribbonW / 2f;
        float r = cx + ribbonW / 2f;
        float top = cy - ribbonH / 2f;
        float bottom = cy + ribbonH / 2f;

        ribbonPath.reset();
        ribbonPath.moveTo(l, top);
        ribbonPath.lineTo(r, top);
        ribbonPath.lineTo(r - tailDepth, cy);
        ribbonPath.lineTo(r, bottom);
        ribbonPath.lineTo(l, bottom);
        ribbonPath.lineTo(l + tailDepth, cy);
        ribbonPath.close();

        bannerPaint.setShader(new LinearGradient(l, top, r, bottom, cStart, cEnd, Shader.TileMode.CLAMP));
        canvas.drawPath(ribbonPath, bannerPaint);
        bannerPaint.setShader(null);

        bannerStrokePaint.setStrokeWidth(dp(2.5f));
        bannerStrokePaint.setColor(ColorUtils.setAlphaComponent(fb.accentColor, alpha));
        canvas.drawPath(ribbonPath, bannerStrokePaint);

        textPaint.setTextSize(dp(17f));
        textPaint.setColor(ColorUtils.setAlphaComponent(fb.textColor, alpha));
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float ty = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(fb.text, cx, ty, textPaint);

        canvas.restore();
    }

    private void drawFourPointStar(Canvas canvas, float r) {
        float inner = r * 0.32f;
        starPath.reset();
        starPath.moveTo(0, -r);
        starPath.lineTo(inner, -inner);
        starPath.lineTo(r, 0);
        starPath.lineTo(inner, inner);
        starPath.lineTo(0, r);
        starPath.lineTo(-inner, inner);
        starPath.lineTo(-r, 0);
        starPath.lineTo(-inner, -inner);
        starPath.close();
        canvas.drawPath(starPath, particlePaint);
    }

    // --- Inspection helpers for verification ---
    public int getActiveParticleCount() {
        return particles.size();
    }

    public int getActiveRippleCount() {
        return ripples.size();
    }

    public boolean isStartToastVisible() {
        for (FloatingBanner fb : banners) {
            if (!fb.isRibbon) return true;
        }
        return false;
    }

    public boolean isRibbonVisible() {
        for (FloatingBanner fb : banners) {
            if (fb.isRibbon) return true;
        }
        return false;
    }
}

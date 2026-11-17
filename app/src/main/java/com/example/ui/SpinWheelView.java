package com.example.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import com.example.model.SpinSegment;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SpinWheelView extends View {

    public interface OnSpinCompleteListener {
        void onSpinComplete(SpinSegment winningSegment);
    }

    // Exact 12 segments matching the Lucky Spin screenshot clockwise from top-right
    public static final List<SpinSegment> SEGMENTS = Arrays.asList(
            new SpinSegment("+30", true, 30, "#C084FC"),
            new SpinSegment("1000", false, 1000, "#FB7144"),
            new SpinSegment("+80", true, 80, "#C084FC"),
            new SpinSegment("50", false, 50, "#FDE047"),
            new SpinSegment("+5", true, 5, "#99E2EA"),
            new SpinSegment("100", false, 100, "#F59E0B"),
            new SpinSegment("+10", true, 10, "#0EA5E9"),
            new SpinSegment("200", false, 200, "#A7D3A6"),
            new SpinSegment("+20", true, 20, "#C084FC"),
            new SpinSegment("500", false, 500, "#FB7144"),
            new SpinSegment("+50", true, 50, "#06F2F2"),
            new SpinSegment("300", false, 300, "#38BDF8")
    );

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF wheelRect = new RectF();

    private float rotationDeg = 0f;
    private boolean isSpinning = false;
    private final Random random = new Random();

    public SpinWheelView(Context context) {
        super(context);
        init();
    }

    public SpinWheelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpinWheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.parseColor("#11131C"));
        borderPaint.setStrokeWidth(4f);

        textPaint.setColor(Color.parseColor("#11131C"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        iconPaint.setTextAlign(Paint.Align.CENTER);
    }

    public boolean isSpinning() {
        return isSpinning;
    }

    public void spin(boolean reducedAnimations, OnSpinCompleteListener listener) {
        if (isSpinning) return;
        isSpinning = true;

        int targetIndex = random.nextInt(SEGMENTS.size());
        float sweep = 360f / SEGMENTS.size(); // 30 degrees

        // Segment i starts at (-90 + i * sweep) degrees when rotationDeg == 0.
        // Center of segment i is at (-90 + i * sweep + sweep/2).
        // Top pointer is at -90 degrees. So to bring segment i center to -90,
        // finalRotation mod 360 should equal (360 - (i * sweep + sweep / 2)).
        float desiredMod = (360f - (targetIndex * sweep + sweep / 2f)) % 360f;
        if (desiredMod < 0) desiredMod += 360f;

        float currentMod = rotationDeg % 360f;
        if (currentMod < 0) currentMod += 360f;
        float deltaMod = desiredMod - currentMod;
        if (deltaMod <= 0) deltaMod += 360f;

        int extraSpins = reducedAnimations ? 2 : 5;
        float endRotation = rotationDeg + (extraSpins * 360f) + deltaMod;

        ValueAnimator animator = ValueAnimator.ofFloat(rotationDeg, endRotation);
        animator.setDuration(reducedAnimations ? 1200L : 3400L);
        animator.setInterpolator(new DecelerateInterpolator(2.8f));
        animator.addUpdateListener(a -> {
            rotationDeg = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isSpinning = false;
                rotationDeg = endRotation % 360f;
                if (listener != null) {
                    listener.onSpinComplete(SEGMENTS.get(targetIndex));
                }
            }
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int size = (h > 0 && MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED)
                ? Math.min(w, h) : w;
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float dim = Math.min(w, h);
        float pad = dim * 0.03f;
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = (dim - 2f * pad) / 2f;
        wheelRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        float sweep = 360f / SEGMENTS.size();
        textPaint.setTextSize(radius * 0.115f);
        iconPaint.setTextSize(radius * 0.135f);
        borderPaint.setStrokeWidth(Math.max(3f, radius * 0.018f));

        canvas.save();
        canvas.rotate(rotationDeg, cx, cy);

        for (int i = 0; i < SEGMENTS.size(); i++) {
            SpinSegment seg = SEGMENTS.get(i);
            float startAngle = -90f + (i * sweep);

            slicePaint.setColor(seg.color);
            canvas.drawArc(wheelRect, startAngle, sweep, true, slicePaint);
            canvas.drawArc(wheelRect, startAngle, sweep, true, borderPaint);

            // Draw label and icon along segment bisector
            float midAngle = startAngle + sweep / 2f;
            canvas.save();
            canvas.rotate(midAngle + 90f, cx, cy);
            canvas.drawText(seg.label, cx, cy - radius * 0.67f, textPaint);
            canvas.drawText(seg.isHint ? "💡" : "⭐", cx, cy - radius * 0.46f, iconPaint);
            canvas.restore();
        }

        // Outer rim border
        borderPaint.setStrokeWidth(Math.max(5f, radius * 0.03f));
        canvas.drawCircle(cx, cy, radius, borderPaint);
        canvas.restore();
    }
}

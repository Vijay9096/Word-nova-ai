package com.word.nova.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight centered wrapping layout for word chips, matching the centered word rows in screenshots.
 */
public class WordFlowLayout extends ViewGroup {

    private int horizontalSpacing;
    private int verticalSpacing;

    public WordFlowLayout(Context context) {
        super(context);
        init(context);
    }

    public WordFlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WordFlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        horizontalSpacing = Math.round(5 * density);
        verticalSpacing = Math.round(5 * density);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        if (maxWidth <= 0) maxWidth = 1000;

        int childWidthSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        int lineWidth = 0;
        int lineHeight = 0;
        int totalHeight = getPaddingTop() + getPaddingBottom();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            child.measure(childWidthSpec, childHeightSpec);
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();

            if (lineWidth > 0 && lineWidth + horizontalSpacing + cw > maxWidth) {
                totalHeight += lineHeight + verticalSpacing;
                lineWidth = cw;
                lineHeight = ch;
            } else {
                lineWidth = (lineWidth == 0) ? cw : lineWidth + horizontalSpacing + cw;
                lineHeight = Math.max(lineHeight, ch);
            }
        }
        if (count > 0) {
            totalHeight += lineHeight;
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), totalHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int availableWidth = (r - l) - getPaddingLeft() - getPaddingRight();
        List<List<View>> rows = new ArrayList<>();
        List<Integer> rowWidths = new ArrayList<>();
        List<Integer> rowHeights = new ArrayList<>();

        List<View> currentRow = new ArrayList<>();
        int lineWidth = 0;
        int lineHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();

            if (!currentRow.isEmpty() && lineWidth + horizontalSpacing + cw > availableWidth) {
                rows.add(currentRow);
                rowWidths.add(lineWidth);
                rowHeights.add(lineHeight);
                currentRow = new ArrayList<>();
                lineWidth = cw;
                lineHeight = ch;
            } else {
                lineWidth = currentRow.isEmpty() ? cw : lineWidth + horizontalSpacing + cw;
                lineHeight = Math.max(lineHeight, ch);
            }
            currentRow.add(child);
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
            rowWidths.add(lineWidth);
            rowHeights.add(lineHeight);
        }

        int y = getPaddingTop();
        for (int rIdx = 0; rIdx < rows.size(); rIdx++) {
            List<View> row = rows.get(rIdx);
            int rw = rowWidths.get(rIdx);
            int rh = rowHeights.get(rIdx);
            int x = getPaddingLeft() + Math.max(0, (availableWidth - rw) / 2);
            for (View child : row) {
                int cw = child.getMeasuredWidth();
                int ch = child.getMeasuredHeight();
                int topOffset = (rh - ch) / 2;
                child.layout(x, y + topOffset, x + cw, y + topOffset + ch);
                x += cw + horizontalSpacing;
            }
            y += rh + verticalSpacing;
        }
    }
}

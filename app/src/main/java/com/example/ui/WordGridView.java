package com.example.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;

import com.example.data.PuzzleGenerator;
import com.example.model.GameTheme;

import java.util.ArrayList;
import java.util.List;

public class WordGridView extends View {

    public interface OnWordGridListener {
        void onWordFound(PuzzleGenerator.PlacedWord word);
        void onInvalidSelection();
    }

    private static final int[] STANDARD_WORD_COLORS = {
            Color.parseColor("#EC407A"), // Pink/Magenta
            Color.parseColor("#7E57C2"), // Purple
            Color.parseColor("#E67E22"), // Orange
            Color.parseColor("#2ECC71"), // Emerald
            Color.parseColor("#26C6DA"), // Cyan
            Color.parseColor("#3B82F6"), // Blue
            Color.parseColor("#65C430"), // Lime Green
            Color.parseColor("#AB47BC"), // Violet
            Color.parseColor("#EF5350"), // Coral Red
            Color.parseColor("#26A69A"), // Teal
            Color.parseColor("#5C6BC0"), // Indigo
            Color.parseColor("#D4AC0D"), // Amber Gold
            Color.parseColor("#42A5F5"), // Sky Blue
            Color.parseColor("#8D6E63"), // Warm Mocha
            Color.parseColor("#EC4899")  // Rose
    };

    // Okabe-Ito inspired high-contrast colorblind-safe palette
    private static final int[] COLORBLIND_WORD_COLORS = {
            Color.parseColor("#E69F00"), // Orange
            Color.parseColor("#56B4E9"), // Sky Blue
            Color.parseColor("#009E73"), // Bluish Green
            Color.parseColor("#0072B2"), // Deep Blue
            Color.parseColor("#D55E00"), // Vermilion
            Color.parseColor("#CC79A7"), // Reddish Purple
            Color.parseColor("#C9B000"), // Ochre Yellow
            Color.parseColor("#3399CC"), // Cerulean
            Color.parseColor("#117733"), // Forest
            Color.parseColor("#882255"), // Wine
            Color.parseColor("#44AA99"), // Teal
            Color.parseColor("#999933"), // Olive
            Color.parseColor("#AA4499"), // Purple
            Color.parseColor("#6699CC"), // Steel Blue
            Color.parseColor("#CC6677")  // Rose
    };

    private PuzzleGenerator.PuzzleBoard board;
    private GameTheme theme = GameTheme.DEFAULT;
    private boolean colorblindMode = false;
    private boolean performanceMode = false;
    private OnWordGridListener listener;

    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cellRect = new RectF();

    private float cellSize;
    private float padding;
    private float gap;

    private boolean isDragging = false;
    private int startRow = -1;
    private int startCol = -1;
    private final List<int[]> activeCells = new ArrayList<>();

    private final List<int[]> hintedCells = new ArrayList<>();
    private float popScale = 1f;
    private final List<int[]> popCells = new ArrayList<>();
    private int nextColorIdx = 0;

    public WordGridView(Context context) {
        super(context);
        init();
    }

    public WordGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WordGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        borderPaint.setStyle(Paint.Style.STROKE);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setListener(OnWordGridListener listener) {
        this.listener = listener;
    }

    public void setTheme(GameTheme theme) {
        this.theme = theme;
        invalidate();
    }

    public void setColorblindMode(boolean colorblindMode) {
        this.colorblindMode = colorblindMode;
        invalidate();
    }

    public void setPerformanceMode(boolean performanceMode) {
        this.performanceMode = performanceMode;
    }

    private int gridSize() {
        return (board != null && board.grid != null && board.grid.length > 0)
                ? board.grid.length : PuzzleGenerator.GRID_SIZE;
    }

    private void recomputeDimensions(int w, int h) {
        if (w <= 0 || h <= 0) return;
        int n = gridSize();
        float dim = Math.min(w, h);
        padding = dim * 0.025f;
        gap = dim * 0.011f;
        float usable = dim - (2f * padding) - ((n - 1) * gap);
        cellSize = usable / n;
        textPaint.setTextSize(cellSize * 0.50f);
        linePaint.setStrokeWidth(cellSize * 0.62f);
        borderPaint.setStrokeWidth(Math.max(3f, dim * 0.008f));
    }

    public void setBoard(PuzzleGenerator.PuzzleBoard board) {
        this.board = board;
        int maxColor = -1;
        if (board != null) {
            for (PuzzleGenerator.PlacedWord pw : board.placedWords) {
                if (pw.found && pw.colorIndex > maxColor) {
                    maxColor = pw.colorIndex;
                }
            }
        }
        this.nextColorIdx = maxColor + 1;
        this.activeCells.clear();
        this.hintedCells.clear();
        this.popCells.clear();
        this.isDragging = false;
        recomputeDimensions(getWidth(), getHeight());
        invalidate();
    }

    public int getNextColorIdx() {
        return nextColorIdx;
    }

    public void setNextColorIdx(int nextColorIdx) {
        this.nextColorIdx = Math.max(0, nextColorIdx);
    }

    public void showHintForWord(PuzzleGenerator.PlacedWord word) {
        hintedCells.clear();
        if (word != null) {
            hintedCells.add(new int[]{word.startRow, word.startCol});
            // Also highlight end cell subtly or whole word first 2 letters
            if (word.word.length() > 1) {
                hintedCells.add(new int[]{word.startRow + word.dRow, word.startCol + word.dCol});
            }
        }
        invalidate();
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recomputeDimensions(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (board == null || cellSize <= 0f) return;

        int n = gridSize();
        int[] palette = colorblindMode ? COLORBLIND_WORD_COLORS : STANDARD_WORD_COLORS;

        // Determine per-cell found color (later found words layer cleanly)
        int[][] cellColors = new int[n][n];
        for (PuzzleGenerator.PlacedWord pw : board.placedWords) {
            if (pw.found) {
                int c = palette[Math.abs(pw.colorIndex) % palette.length];
                for (int[] cell : pw.getCells()) {
                    if (cell[0] >= 0 && cell[0] < n && cell[1] >= 0 && cell[1] < n) {
                        cellColors[cell[0]][cell[1]] = c;
                    }
                }
            }
        }

        float corner = cellSize * 0.16f;
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textOffset = -(fm.ascent + fm.descent) / 2f;

        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                float left = padding + c * (cellSize + gap);
                float top = padding + r * (cellSize + gap);
                cellRect.set(left, top, left + cellSize, top + cellSize);

                boolean isActive = containsCell(activeCells, r, c);
                boolean isHinted = containsCell(hintedCells, r, c);
                boolean isPop = containsCell(popCells, r, c);

                int bgColor = cellColors[r][c] != 0 ? cellColors[r][c] : theme.tileColor;
                if (isActive) {
                    bgColor = theme.primaryButtonColor;
                }

                canvas.save();
                if (isPop && popScale != 1f) {
                    canvas.scale(popScale, popScale, cellRect.centerX(), cellRect.centerY());
                }

                tilePaint.setColor(bgColor);
                canvas.drawRoundRect(cellRect, corner, corner, tilePaint);

                if (isHinted && cellColors[r][c] == 0) {
                    borderPaint.setColor(Color.parseColor("#FBBF24"));
                    canvas.drawRoundRect(cellRect, corner, corner, borderPaint);
                }

                int letterColor = Color.WHITE;
                if (theme.isLightTheme && cellColors[r][c] == 0 && !isActive) {
                    letterColor = theme.textPrimaryColor;
                }
                textPaint.setColor(letterColor);
                canvas.drawText(String.valueOf(board.grid[r][c]),
                        cellRect.centerX(), cellRect.centerY() + textOffset, textPaint);

                canvas.restore();
            }
        }

        // Draw translucent drag band connecting first and last active cells
        if (isDragging && activeCells.size() > 1) {
            int[] first = activeCells.get(0);
            int[] last = activeCells.get(activeCells.size() - 1);
            float x1 = padding + first[1] * (cellSize + gap) + cellSize / 2f;
            float y1 = padding + first[0] * (cellSize + gap) + cellSize / 2f;
            float x2 = padding + last[1] * (cellSize + gap) + cellSize / 2f;
            float y2 = padding + last[0] * (cellSize + gap) + cellSize / 2f;
            linePaint.setColor(Color.argb(85, 255, 255, 255));
            canvas.drawLine(x1, y1, x2, y2, linePaint);
        }
    }

    private boolean containsCell(List<int[]> list, int r, int c) {
        for (int[] cell : list) {
            if (cell[0] == r && cell[1] == c) return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (board == null || cellSize <= 0f) return false;

        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                int[] start = pointToCell(x, y);
                if (start != null) {
                    isDragging = true;
                    startRow = start[0];
                    startCol = start[1];
                    activeCells.clear();
                    activeCells.add(start);
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (!isDragging) return false;
                int[] cur = pointToCellClamped(x, y);
                updateSelectionLine(startRow, startCol, cur[0], cur[1]);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isDragging) return false;
                isDragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                if (action == MotionEvent.ACTION_UP) {
                    evaluateSelection();
                }
                activeCells.clear();
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int[] pointToCell(float x, float y) {
        int n = gridSize();
        float stride = cellSize + gap;
        int c = (int) ((x - padding) / stride);
        int r = (int) ((y - padding) / stride);
        if (r >= 0 && r < n && c >= 0 && c < n) {
            return new int[]{r, c};
        }
        return null;
    }

    private int[] pointToCellClamped(float x, float y) {
        int n = gridSize();
        float stride = cellSize + gap;
        int c = Math.round((x - padding - cellSize / 2f) / stride);
        int r = Math.round((y - padding - cellSize / 2f) / stride);
        r = Math.max(0, Math.min(n - 1, r));
        c = Math.max(0, Math.min(n - 1, c));
        return new int[]{r, c};
    }

    private void updateSelectionLine(int r0, int c0, int r1, int c1) {
        activeCells.clear();
        int n = gridSize();
        int dr = r1 - r0;
        int dc = c1 - c0;
        if (dr == 0 && dc == 0) {
            activeCells.add(new int[]{r0, c0});
            return;
        }

        // Snap angle to nearest 45-degree octant
        double angle = Math.atan2(dr, dc);
        int octant = (int) Math.round(angle / (Math.PI / 4.0));
        int stepR = (int) Math.round(Math.sin(octant * Math.PI / 4.0));
        int stepC = (int) Math.round(Math.cos(octant * Math.PI / 4.0));
        if (stepR == 0 && stepC == 0) return;

        int steps = Math.max(Math.abs(dr), Math.abs(dc));
        for (int i = 0; i <= steps; i++) {
            int nr = r0 + i * stepR;
            int nc = c0 + i * stepC;
            if (nr < 0 || nr >= n || nc < 0 || nc >= n) {
                break;
            }
            activeCells.add(new int[]{nr, nc});
        }
    }

    private void evaluateSelection() {
        if (activeCells.isEmpty() || board == null) return;
        StringBuilder sb = new StringBuilder(activeCells.size());
        for (int[] cell : activeCells) {
            sb.append(board.grid[cell[0]][cell[1]]);
        }
        String forward = sb.toString();
        String backward = sb.reverse().toString();

        PuzzleGenerator.PlacedWord matched = null;
        for (PuzzleGenerator.PlacedWord pw : board.placedWords) {
            if (!pw.found && (pw.word.equals(forward) || pw.word.equals(backward))) {
                matched = pw;
                break;
            }
        }

        if (matched != null) {
            matched.found = true;
            matched.colorIndex = nextColorIdx++;
            // Clear hint if it belonged to this word
            hintedCells.clear();
            if (!performanceMode) {
                animatePop(matched.getCells());
            }
            if (listener != null) {
                listener.onWordFound(matched);
            }
        } else if (!board.allFound() && activeCells.size() >= 2 && listener != null) {
            listener.onInvalidSelection();
        }
    }

    private void animatePop(List<int[]> cells) {
        popCells.clear();
        popCells.addAll(cells);
        ValueAnimator anim = ValueAnimator.ofFloat(1.0f, 1.14f, 1.0f);
        anim.setDuration(260);
        anim.setInterpolator(new OvershootInterpolator());
        anim.addUpdateListener(a -> {
            popScale = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }
}

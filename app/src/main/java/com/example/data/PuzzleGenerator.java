package com.example.data;

import com.example.model.Category;
import com.example.model.Difficulty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PuzzleGenerator {

    public static final int GRID_SIZE = 10;

    public static class PlacedWord {
        public final String word;
        public final int startRow;
        public final int startCol;
        public final int dRow;
        public final int dCol;
        public boolean found;
        public int colorIndex;

        public PlacedWord(String word, int startRow, int startCol, int dRow, int dCol) {
            this.word = word;
            this.startRow = startRow;
            this.startCol = startCol;
            this.dRow = dRow;
            this.dCol = dCol;
            this.found = false;
            this.colorIndex = -1;
        }

        public List<int[]> getCells() {
            List<int[]> cells = new ArrayList<>(word.length());
            for (int i = 0; i < word.length(); i++) {
                cells.add(new int[]{startRow + i * dRow, startCol + i * dCol});
            }
            return cells;
        }
    }

    public static class PuzzleBoard {
        public final char[][] grid;
        public final List<PlacedWord> placedWords;

        public PuzzleBoard(char[][] grid, List<PlacedWord> placedWords) {
            this.grid = grid;
            this.placedWords = placedWords;
        }

        public boolean allFound() {
            for (PlacedWord pw : placedWords) {
                if (!pw.found) return false;
            }
            return true;
        }
    }

    private static final int[][] DIRECTIONS_EASY = {
            {0, 1}, {1, 0}, {1, 1}, {0, -1}, {-1, 0}, {1, -1}
    };

    private static final int[][] DIRECTIONS_ALL = {
            {0, 1}, {0, -1}, {1, 0}, {-1, 0},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public static PuzzleBoard generate(Category category, Difficulty difficulty, int level, List<String> wordsInput) {
        long baseSeed = ((long) category.ordinal() * 10007L)
                + ((long) difficulty.ordinal() * 1009L)
                + (level * 37L)
                + (difficulty == Difficulty.DAILY ? GamePrefs.todayDateKey().hashCode() : 0L);

        int maxWordLen = 0;
        for (String w : wordsInput) {
            if (w != null && w.length() > maxWordLen) {
                maxWordLen = w.length();
            }
        }

        List<String> words = new ArrayList<>(wordsInput);
        // Place longer words first to maximize success rate
        Collections.sort(words, (a, b) -> Integer.compare(b.length(), a.length()));

        int[][] dirs = (difficulty == Difficulty.EASY) ? DIRECTIONS_EASY : DIRECTIONS_ALL;

        int minGrid = Math.max(GRID_SIZE, maxWordLen);
        int maxGrid = Math.max(12, minGrid);
        for (int gridSize = minGrid; gridSize <= maxGrid; gridSize++) {
            int attempts = (gridSize == GRID_SIZE) ? 60 : (gridSize == 11 ? 80 : 120);
            PuzzleBoard solved = tryGenerateForGridSize(gridSize, baseSeed, words, wordsInput, dirs, attempts);
            if (solved != null) {
                return solved;
            }
        }

        // Fallback with expanded grid if needed
        int fallbackSize = Math.max(14, maxWordLen);
        Random rng = new Random(baseSeed + 999L);
        char[][] grid = new char[fallbackSize][fallbackSize];
        List<PlacedWord> placed = new ArrayList<>();
        for (String w : words) {
            PlacedWord pw = tryPlaceWord(grid, fallbackSize, w, DIRECTIONS_ALL, rng);
            if (pw != null) {
                placed.add(pw);
            }
        }
        fillEmpty(grid, fallbackSize, rng);
        return new PuzzleBoard(grid, reorderPlaced(placed, wordsInput));
    }

    private static PuzzleBoard tryGenerateForGridSize(
            int gridSize,
            long baseSeed,
            List<String> sortedWords,
            List<String> originalOrder,
            int[][] dirs,
            int attempts
    ) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            Random rng = new Random(baseSeed + attempt * 131L + gridSize * 31L);
            List<String> attemptWords = new ArrayList<>(sortedWords);
            if (attempt >= 12) {
                // Slightly perturb order of equal/similar-length words to escape local greedy dead-ends
                for (int i = 1; i < attemptWords.size(); i++) {
                    if (rng.nextInt(3) == 0) {
                        Collections.swap(attemptWords, i - 1, i);
                    }
                }
            }
            char[][] grid = new char[gridSize][gridSize];
            List<PlacedWord> placed = new ArrayList<>();
            boolean allOk = true;

            for (String w : attemptWords) {
                PlacedWord pw = tryPlaceWord(grid, gridSize, w, dirs, rng);
                if (pw == null) {
                    pw = tryPlaceWord(grid, gridSize, w, DIRECTIONS_ALL, rng);
                }
                if (pw == null) {
                    allOk = false;
                    break;
                }
                placed.add(pw);
            }

            if (allOk) {
                fillEmpty(grid, gridSize, rng);
                return new PuzzleBoard(grid, reorderPlaced(placed, originalOrder));
            }
        }
        return null;
    }

    private static List<PlacedWord> reorderPlaced(List<PlacedWord> placed, List<String> originalOrder) {
        List<PlacedWord> ordered = new ArrayList<>(placed.size());
        for (String orig : originalOrder) {
            for (PlacedWord pw : placed) {
                if (pw.word.equals(orig)) {
                    ordered.add(pw);
                    break;
                }
            }
        }
        return ordered;
    }

    private static PlacedWord tryPlaceWord(char[][] grid, int gridSize, String word, int[][] dirs, Random rng) {
        int len = word.length();
        List<int[]> positions = new ArrayList<>();
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                for (int[] d : dirs) {
                    int endR = r + (len - 1) * d[0];
                    int endC = c + (len - 1) * d[1];
                    if (endR >= 0 && endR < gridSize && endC >= 0 && endC < gridSize) {
                        positions.add(new int[]{r, c, d[0], d[1]});
                    }
                }
            }
        }
        if (positions.isEmpty()) return null;
        Collections.shuffle(positions, rng);

        // Scan all valid placements to find maximum intersection density.
        int[] best = null;
        int bestScore = -1;
        for (int i = 0; i < positions.size(); i++) {
            int[] pos = positions.get(i);
            int score = checkFitScore(grid, word, pos[0], pos[1], pos[2], pos[3]);
            if (score > bestScore) {
                bestScore = score;
                best = pos;
            }
        }

        if (best == null) return null;
        for (int i = 0; i < len; i++) {
            grid[best[0] + i * best[2]][best[1] + i * best[3]] = word.charAt(i);
        }
        return new PlacedWord(word, best[0], best[1], best[2], best[3]);
    }

    private static int checkFitScore(char[][] grid, String word, int r, int c, int dr, int dc) {
        int overlaps = 0;
        for (int i = 0; i < word.length(); i++) {
            char existing = grid[r + i * dr][c + i * dc];
            char ch = word.charAt(i);
            if (existing != 0 && existing != ch) {
                return -1;
            }
            if (existing == ch) {
                overlaps++;
            }
        }
        return overlaps;
    }

    private static void fillEmpty(char[][] grid, int gridSize, Random rng) {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (grid[r][c] == 0) {
                    grid[r][c] = alphabet.charAt(rng.nextInt(alphabet.length()));
                }
            }
        }
    }
}

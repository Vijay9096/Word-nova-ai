package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Color;

import androidx.test.core.app.ApplicationProvider;

import com.example.data.GamePrefs;
import com.example.data.PuzzleGenerator;
import com.example.data.WordBankRepository;
import com.example.model.Category;
import com.example.model.Difficulty;
import com.example.model.GameTheme;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WordNovaCoreTest {

    @Before
    public void initAssets() {
        Context ctx = ApplicationProvider.getApplicationContext();
        GameTheme.ensureInitialized(ctx);
        Category.ensureInitialized(ctx);
        WordBankRepository.ensureInitialized(ctx);
    }

    @Test
    public void testAllExtractedCategoriesHaveExact1500WordsAndNoCrossCategoryMixing() {
        Category[] categories = Category.values();
        assertEquals(10, categories.length);

        for (Category c : categories) {
            List<String> fullPool = WordBankRepository.getAllWordsForCategory(c);
            assertEquals("Category " + c.displayName + " must contain exactly 1500 extracted words",
                    1500, fullPool.size());
            Set<String> poolSet = new HashSet<>(fullPool);
            assertEquals("Category " + c.displayName + " should have 1500 unique words",
                    1500, poolSet.size());

            // Check levels across the range pull strictly from this category and place all 15 words
            for (int lvl : new int[]{1, 25, 50, 75, 100}) {
                List<String> words = WordBankRepository.getWordsForLevel(c, Difficulty.NORMAL, lvl);
                assertEquals("Category " + c + " level " + lvl + " should have 15 words", 15, words.size());
                assertEquals("Level words must not duplicate", 15, new HashSet<>(words).size());
                for (String w : words) {
                    assertTrue("Word " + w + " must belong strictly to " + c.displayName, poolSet.contains(w));
                }

                PuzzleGenerator.PuzzleBoard board = PuzzleGenerator.generate(c, Difficulty.NORMAL, lvl, words);
                assertEquals("Board must place all 15 words for " + c + " level " + lvl,
                        15, board.placedWords.size());
                int n = board.grid.length;
                assertTrue("Grid size should stay compact (10..12)", n >= PuzzleGenerator.GRID_SIZE && n <= 12);
                if (Category.GENERAL.equals(c)) {
                    assertEquals("GENERAL levels must preserve 10x10 layout", PuzzleGenerator.GRID_SIZE, n);
                }
                for (PuzzleGenerator.PlacedWord pw : board.placedWords) {
                    for (int idx = 0; idx < pw.word.length(); idx++) {
                        int rr = pw.startRow + idx * pw.dRow;
                        int cc = pw.startCol + idx * pw.dCol;
                        assertEquals("Grid cell must match placed word char",
                                pw.word.charAt(idx), board.grid[rr][cc]);
                    }
                }
                for (int r = 0; r < n; r++) {
                    assertEquals(n, board.grid[r].length);
                    for (int col = 0; col < n; col++) {
                        char ch = board.grid[r][col];
                        assertTrue("Grid char must be A-Z", ch >= 'A' && ch <= 'Z');
                    }
                }
            }
        }

        // Verify 10-letter words were preserved verbatim
        assertTrue(WordBankRepository.getAllWordsForCategory(Category.PROFESSION).contains("JOURNALIST"));
        assertTrue(WordBankRepository.getAllWordsForCategory(Category.GENERAL).contains("LIGHTHOUSE"));
        assertTrue(WordBankRepository.getAllWordsForCategory(Category.FRUITS).contains("WATERMELON"));
    }

    @Test
    public void testAll174SuppliedThemesLoadedFaithfully() {
        GameTheme[] themes = GameTheme.values();
        assertEquals(174, themes.length);
        assertEquals(GameTheme.DEFAULT, themes[0]);
        assertEquals(0, themes[0].price);
        assertEquals(Color.parseColor("#1e1e2f"), themes[0].backgroundColor);
        assertEquals(Color.parseColor("#4cafef"), themes[0].primaryButtonColor);

        GameTheme dark = GameTheme.fromName("DARK");
        assertNotNull(dark);
        assertEquals(120, dark.price);
        assertEquals(Color.parseColor("#121212"), dark.backgroundColor);

        GameTheme mint1 = GameTheme.fromName("MINT");
        GameTheme mint2 = GameTheme.fromName("MINT_2");
        assertEquals(130, mint1.price);
        assertEquals(280, mint2.price);
        assertEquals(Color.parseColor("#98FFDE"), mint1.backgroundColor);
        assertEquals(Color.parseColor("#D9FFF8"), mint2.backgroundColor);

        GameTheme hazePurple = GameTheme.fromName("HAZE PURPLE");
        assertEquals(300, hazePurple.price);

        GameTheme last = GameTheme.fromName("LIGHT BLUE");
        assertEquals(430, last.price);
        assertEquals(Color.parseColor("#3DD9BA"), last.backgroundColor);
    }

    @Test
    public void testProgressKeyAndResetPreservesEconomy() {
        Context ctx = ApplicationProvider.getApplicationContext();
        GamePrefs prefs = new GamePrefs(ctx);
        prefs.clearAllLocalStorage();

        GameTheme ice = GameTheme.fromName("ICE");
        int initialCoins = prefs.getCoins();
        prefs.addCoins(250);
        prefs.addHints(3);
        prefs.unlockTheme(ice);

        assertEquals("hard-ANIMAL-level5", GamePrefs.levelKey(Difficulty.HARD, Category.ANIMAL, 5));
        assertTrue(prefs.isLevelUnlocked(Difficulty.HARD, Category.ANIMAL, 1));
        assertFalse(prefs.isLevelUnlocked(Difficulty.HARD, Category.ANIMAL, 2));

        prefs.markLevelCompleted(Difficulty.HARD, Category.ANIMAL, 1);
        assertTrue(prefs.isLevelCompleted(Difficulty.HARD, Category.ANIMAL, 1));
        assertTrue(prefs.isLevelUnlocked(Difficulty.HARD, Category.ANIMAL, 2));

        // Reset level progress MUST preserve coins, hints, and purchased themes
        prefs.resetLevelProgressPreserveEconomy();
        assertFalse(prefs.isLevelCompleted(Difficulty.HARD, Category.ANIMAL, 1));
        assertEquals(initialCoins + 250, prefs.getCoins());
        assertEquals(5, prefs.getHints());
        assertTrue(prefs.isThemeUnlocked(ice));
    }

    @Test
    public void testThemesScreenCannotPurchaseOrSelectLockedThemes() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        GamePrefs prefs = activity.getGamePrefs();
        prefs.clearAllLocalStorage();
        prefs.addCoins(500); // Plenty of coins, yet Themes screen must NOT allow purchase
        int coinsBefore = prefs.getCoins();

        GameTheme dark = GameTheme.fromName("DARK");
        GameTheme ice = GameTheme.fromName("ICE");

        com.example.ui.fragments.ThemesFragment themesFragment = new com.example.ui.fragments.ThemesFragment();
        activity.navigateTo(themesFragment, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        androidx.recyclerview.widget.RecyclerView rv = themesFragment.requireView().findViewById(R.id.rv_themes);
        rv.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY)
        );
        rv.layout(0, 0, 1080, 1920);

        // Position 1 is DARK (locked by default, price 120)
        androidx.recyclerview.widget.RecyclerView.ViewHolder vhLocked = rv.findViewHolderForAdapterPosition(1);
        android.widget.Button lockedBtn = vhLocked.itemView.findViewById(R.id.btn_grid_item);
        assertTrue(lockedBtn.getText().toString().contains("Locked"));

        // Tapping a locked theme only previews it: no dialog, no coin deduction, no unlock, no equip
        lockedBtn.performClick();
        assertFalse("Themes screen must never unlock locked themes", prefs.isThemeUnlocked(dark));
        assertEquals("Themes screen must never deduct coins", coinsBefore, prefs.getCoins());
        assertEquals("Locked theme must not become selected", GameTheme.DEFAULT, prefs.getCurrentTheme());

        // Unlocked theme can be selected
        prefs.unlockTheme(ice);
        themesFragment = new com.example.ui.fragments.ThemesFragment();
        activity.navigateTo(themesFragment, true);
        activity.getSupportFragmentManager().executePendingTransactions();
        rv = themesFragment.requireView().findViewById(R.id.rv_themes);
        rv.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY)
        );
        rv.layout(0, 0, 1080, 1920);

        int icePos = ice.ordinal();
        androidx.recyclerview.widget.RecyclerView.ViewHolder vhUnlocked = rv.findViewHolderForAdapterPosition(icePos);
        android.widget.Button unlockedBtn = vhUnlocked.itemView.findViewById(R.id.btn_grid_item);
        unlockedBtn.performClick();
        assertEquals(ice, prefs.getCurrentTheme());

        controller.pause().stop().destroy();
    }

    @Test
    public void testShopScreenPurchasesThemeAndPreventsInsufficientCoins() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().resume().visible().get();
        GamePrefs prefs = activity.getGamePrefs();
        prefs.clearAllLocalStorage();

        GameTheme dark = GameTheme.fromName("DARK");

        // Default coins = 100, DARK theme costs 120 -> cannot buy yet
        com.example.ui.fragments.ShopFragment shopFragment = new com.example.ui.fragments.ShopFragment();
        activity.navigateTo(shopFragment, true);
        activity.getSupportFragmentManager().executePendingTransactions();

        android.widget.GridLayout grid = shopFragment.requireView().findViewById(R.id.grid_shop_themes);
        android.widget.Button darkBtn = (android.widget.Button) grid.getChildAt(0); // DARK is first after DEFAULT
        darkBtn.performClick();
        assertFalse(prefs.isThemeUnlocked(dark));
        assertEquals(100, prefs.getCoins());

        // Add enough coins and purchase
        prefs.addCoins(50); // total 150
        darkBtn.performClick();
        assertTrue(prefs.isThemeUnlocked(dark));
        assertEquals(150 - dark.price, prefs.getCoins());
        assertEquals(dark, prefs.getCurrentTheme());

        // Reload preferences to verify permanent unlock
        GamePrefs reloaded = new GamePrefs(activity);
        assertTrue(reloaded.isThemeUnlocked(dark));

        controller.pause().stop().destroy();
    }
}

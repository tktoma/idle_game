package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.pages.SkinShelf;
import idle.ui.pages.SkinsPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** L'onglet Skins par catégorie (version 0.249), sous Xvfb : les sous-onglets, les sections et les cartes suivent les paliers. */
class PageDesSkinsParCategorieTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    @Test
    void lesSousOngletsLesSectionsEtLesCartesSuiventLesPaliers() {
        FxTestSupport.fx(() -> {
            Game game = new Game(5);
            game.start();
            game.testFormUniverse();
            game.state().setSkinsOpen(true);
            SkinsPage page = new SkinsPage(game);
            assertNull(page.selected(), "la collection d'abord");
            for (SkinShelf.Category category : SkinShelf.Category.values()) {
                assertEquals(SkinShelf.shown(game, category), page.categoryTab(category).isVisible(), category.label());
            }
            for (SkinVisual visual : SkinVisual.values()) assertEquals(SkinShelf.shown(game, visual), page.sectionShown(visual), visual.name());
            for (Skin skin : Skin.values()) assertEquals(SkinShelf.shown(game, skin), page.cardShown(skin), skin.name());
            SkinShelf.Category open = null;
            for (SkinShelf.Category category : SkinShelf.Category.values()) if (open == null && SkinShelf.shown(game, category)) open = category;
            page.select(open);
            assertEquals(open, page.selected());
            int[] counts = SkinShelf.counts(game, open);
            assertTrue(page.categoryTab(open).getText().endsWith("(" + counts[0] + "/" + counts[1] + ")"), page.categoryTab(open).getText());
            // Tout débloqué : chaque catégorie et chaque carte se montrent.
            game.testAllSkins(true);
            page.refresh();
            for (SkinShelf.Category category : SkinShelf.Category.values()) assertTrue(page.categoryTab(category).isVisible(), category.label());
            for (Skin skin : Skin.values()) assertTrue(page.cardShown(skin), skin.name());
            return null;
        });
    }
}

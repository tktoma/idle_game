package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.ui.common.ElementText;
import idle.ui.pages.SkinsPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.209 : l'onglet Skins n'écrit plus le bonus sur chaque carte ; une carte en tête donne le nombre de skins
 * débloqués, puis le bonus cumulé, sans plafond.
 */
class CollectionDesSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    @Test
    void laCarteDeLaCollectionDitLeNombrePuisLeBonusCumule() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.testFormUniverse();
            game.state().setSkinsOpen(true);
            int counted = 0;
            for (Skin skin : Skin.values()) {
                if (skin.isFree() || counted >= 150) continue;
                game.state().unlockSkin(skin.id());
                counted++;
            }
            game.tick(1);
            SkinsPage page = new SkinsPage(game);
            page.refresh();
            assertEquals(150, game.skinsCounted(), "plus de cent : le plafond est parti");
            assertEquals(4.0, game.skinBoost(), 1e-9);
            String text = page.collectionCard().text();
            int number = text.indexOf(game.skinsUnlocked() + " / " + Skin.values().length + " skins débloqués");
            int bonus = text.indexOf("Bonus cumulé : ×" + ElementText.number(4.0) + " (+" + ElementText.percent(3.0) + ")");
            assertTrue(number >= 0 && bonus > number, "le nombre, puis le bonus : " + text);
            for (Skin skin : Skin.values()) {
                if (!game.hasSkin(skin) || skin.isFree()) continue;
                String card = page.card(skin).text();
                assertFalse(card.contains("à toutes les ressources") || card.contains("+" + ElementText.percent(Skin.BONUS)), card);
                assertTrue(card.contains(skin.source()), "la carte dit comment le skin s'obtient : " + card);
            }
            assertFalse(page.summaryText().contains("jusqu'à 100"), page.summaryText());
            assertTrue(page.summaryText().contains("sans plafond"), page.summaryText());
            return null;
        });
    }
}

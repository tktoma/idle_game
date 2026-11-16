package idle.ui.darkmatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.darkmatter.DarkUpgrade;
import idle.ui.FxTestSupport;
import idle.ui.common.Card;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * La case qui compte le plus (Noyaux sombres) se pose seule en haut de l'arbre : sous la racine, au milieu, au-dessus
 * de toutes les autres cases, dans une fenêtre large comme dans une étroite (branches sur deux étages).
 */
class ArbreCaseCleTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    @Test
    void laCaseQuiCompteLePlusEstEnHautAuMilieu() {
        FxTestSupport.fx(() -> {
            Game game = new Game(42L);
            game.start();
            game.state().setExplosions(1);
            DarkMatterPage page = new DarkMatterPage(game);
            DarkMatterTreePane tree = page.treePane();
            page.step(1);
            tree.refresh();
            for (double width : new double[] {1400, 600}) {
                tree.resize(width, 600);
                tree.layout();
                Card key = tree.card(Game.KEY_DARK_UPGRADE);
                double keyCenter = key.getLayoutX() + key.getWidth() / 2;
                assertEquals(width / 2, keyCenter, 1.5, "au milieu, à la largeur " + width);
                double keyBottom = key.getLayoutY() + key.getHeight();
                assertTrue(key.getLayoutY() > 40, "sous la racine");
                for (DarkUpgrade dark : game.darkUpgrades()) {
                    if (dark.isLink() || dark.id().equals(Game.KEY_DARK_UPGRADE)) continue;
                    Card other = tree.card(dark.id());
                    assertTrue(other.getLayoutY() > keyBottom, dark.id() + " sous la case qui compte le plus, à la largeur " + width);
                }
            }
            return null;
        });
    }
}

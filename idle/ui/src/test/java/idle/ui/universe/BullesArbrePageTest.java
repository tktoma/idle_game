package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.ui.FxTestSupport;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.142 : les bulles de l'arbre sur la page de la graine, sous Xvfb. Elles naissent avec les paliers, se
 * cliquent pour s'acheter avec l'énergie du vide accumulée, suivent leurs liens ; le sous-onglet « Vide » est parti.
 */
class BullesArbrePageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static Game at(int stage, double accumulated) {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(stage));
        if (accumulated > 0) game.state().noteVoidHistory("3", accumulated);
        return game;
    }

    private static UniversePage page(Game game) {
        UniversePage page = new UniversePage(game);
        new Scene(page, 1200, 760);
        page.select(UniversePage.SEED);
        page.refresh();
        page.applyCss();
        page.layout();
        for (int i = 0; i < 10; i++) page.frame(0.016, true);
        return page;
    }

    @Test
    void leSousOngletDuVideEstParti() {
        FxTestSupport.fx(() -> {
            UniversePage page = page(at(4, TreeBubble.SEED.cost()));
            for (Node tab : page.subTabs().getChildren()) assertFalse(((Button) tab).getText().contains("Vide"));
            assertEquals(3, page.subTabs().getChildren().size());
            return null;
        });
    }

    @Test
    void laBulleDeLaBaseSAcheteDUnClic() {
        FxTestSupport.fx(() -> {
            double base = TreeBubble.SEED.cost();
            Game game = at(4, base * 2.5);
            UniversePage page = page(game);
            SeedView view = page.seedView();
            assertEquals(1, view.bubbleCount(), "au palier 4, une bulle");
            assertTrue(page.voidCumulLabel().getText().contains("bulles de l'arbre"), page.voidCumulLabel().getText());
            double[] at = view.bubblePoint(0);
            view.hover(at[0], at[1]);
            assertEquals(0, view.hoveredBubble(), "l'étiquette de la bulle s'affiche au survol");
            assertTrue(view.clickBubble(at[0], at[1]));
            assertTrue(game.isTreeBubbleBought(TreeBubble.SEED));
            assertEquals(base * 1.5, game.voidAvailable(), 1e-9);
            assertTrue(page.voidCumulLabel().getText().contains("dépenser pour les bulles"), page.voidCumulLabel().getText());
            assertFalse(view.clickBubble(5, 5), "à côté, rien");
            return null;
        });
    }

    @Test
    void lesBullesSuiventLeursLiensJusquALaCime() {
        FxTestSupport.fx(() -> {
            Game game = at(6, 1e12);
            UniversePage page = page(game);
            SeedView view = page.seedView();
            assertEquals(6, view.bubbleCount(), "la base, le feuillage, la cime");
            // La cime, avant la base : rien.
            double[] crown = view.bubblePoint(4);
            view.clickBubble(crown[0], crown[1]);
            assertFalse(game.isTreeBubbleBought(TreeBubble.CROWN_MIDDLE));
            double[] base = view.bubblePoint(0), left = view.bubblePoint(1);
            assertTrue(base[1] > left[1], "la base est sous le feuillage");
            assertTrue(crown[1] < left[1], "la cime au-dessus");
            view.clickBubble(base[0], base[1]);
            page.frame(0.016, true);
            left = view.bubblePoint(1);
            view.clickBubble(left[0], left[1]);
            page.frame(0.016, true);
            crown = view.bubblePoint(4);
            view.clickBubble(crown[0], crown[1]);
            assertTrue(game.isTreeBubbleBought(TreeBubble.LEAVES_LEFT));
            assertTrue(game.isTreeBubbleBought(TreeBubble.CROWN_MIDDLE), "reliée à la bulle de gauche, achetée");
            return null;
        });
    }

    /**
     * Le survol d'une bulle affiche son étiquette (nom et prix, dessinés par-dessus le reste) ; la quitter l'efface ; un clic
     * la survole aussi. Le texte lui-même (« Bulle de la graine », « Prix : 1000 d'énergie du vide ») n'a pas d'accesseur :
     * on vérifie quelle bulle est survolée.
     */
    @Test
    void leSurvolDUneBulleMontreSonEtiquetteEtLaQuitterLEfface() {
        FxTestSupport.fx(() -> {
            Game game = at(5, TreeBubble.SEED.cost());
            UniversePage page = page(game);
            SeedView view = page.seedView();
            assertEquals(3, view.bubbleCount(), "la base et les deux du bonsaï");
            assertEquals(-1, view.hoveredBubble(), "rien de survolé au départ");
            double[] base = view.bubblePoint(TreeBubble.SEED.ordinal());
            view.hover(base[0], base[1]);
            assertEquals(TreeBubble.SEED.ordinal(), view.hoveredBubble(), "la base : son étiquette s'affiche");
            page.frame(0.016, true);
            assertEquals(TreeBubble.SEED.ordinal(), view.hoveredBubble(), "une image de plus ne la perd pas");
            double[] left = view.bubblePoint(TreeBubble.LEAVES_LEFT.ordinal());
            view.hover(left[0], left[1]);
            assertEquals(TreeBubble.LEAVES_LEFT.ordinal(), view.hoveredBubble(), "une bulle fermée se survole aussi : son étiquette dit ce qu'il faut d'abord");
            view.hover(5, 5);
            assertEquals(-1, view.hoveredBubble(), "à côté, plus d'étiquette");
            assertTrue(view.clickBubble(base[0], base[1]));
            assertEquals(TreeBubble.SEED.ordinal(), view.hoveredBubble(), "un clic survole la bulle cliquée");
            assertTrue(game.isTreeBubbleBought(TreeBubble.SEED));
            page.frame(0.016, true);
            assertEquals(TreeBubble.SEED.ordinal(), view.hoveredBubble(), "achetée, elle reste survolée : l'étiquette dit « Achetée »");
            return null;
        });
    }

    /** Version 0.143 : à l'arbre-monde, les dix-neuf bulles du schéma, toutes dans la vue ; les racines sous la graine. */
    @Test
    void lArbreMondePorteToutLeReseau() {
        FxTestSupport.fx(() -> {
            Game game = at(11, 1e12);
            UniversePage page = page(game);
            SeedView view = page.seedView();
            assertEquals(TreeBubble.values().length, view.bubbleCount());
            double width = view.getWidth(), height = view.getHeight();
            for (int i = 0; i < view.bubbleCount(); i++) {
                double[] at = view.bubblePoint(i);
                assertTrue(at[0] > 0 && at[0] < width && at[1] > 0 && at[1] < height, "bulle " + i + " dans la vue");
            }
            double base = view.bubblePoint(TreeBubble.SEED.ordinal())[1];
            double rootsY = view.bubblePoint(TreeBubble.ROOTS.ordinal())[1];
            assertTrue(rootsY > base, "les racines sous la graine");
            // Version 0.144 : les deux bulles du bas s'écartent, et les racines ne touchent plus le bas de la vue.
            assertTrue(rootsY - base >= view.bubbleRadius() * 3.4, "écart " + (rootsY - base));
            // Les bulles flottent de ±0,12 rayon : la marge en tient compte.
            assertTrue(rootsY <= height - view.bubbleRadius() * 1.85, "racines à " + rootsY + " sur " + height);
            for (int i = 0; i < view.bubbleCount(); i++) {
                assertTrue(view.bubblePoint(TreeBubble.SUMMIT.ordinal())[1] <= view.bubblePoint(i)[1] + 1, "le sommet en haut");
            }
            // Le réseau s'achète de proche en proche, par la base puis les racines.
            double[] seed = view.bubblePoint(TreeBubble.SEED.ordinal());
            view.clickBubble(seed[0], seed[1]);
            page.frame(0.016, true);
            double[] roots = view.bubblePoint(TreeBubble.ROOTS.ordinal());
            // Version 0.158 : les racines attendent l'amélioration de graine qui les ouvre.
            assertEquals("Ouverte par l'amélioration de graine Bulle des racines", view.bubble(TreeBubble.ROOTS.ordinal()).price());
            view.clickBubble(roots[0], roots[1]);
            assertFalse(game.isTreeBubbleBought(TreeBubble.ROOTS));
            game.state().setSeedUpgradeLevel(Game.SeedUpgrade.ROOT_BUBBLE.ordinal(), 1);
            page.refresh();
            page.frame(0.016, true);
            view.clickBubble(roots[0], roots[1]);
            assertTrue(game.isTreeBubbleBought(TreeBubble.ROOTS));
            return null;
        });
    }
}

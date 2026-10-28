package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.TreeBubble;
import idle.core.math.BigNum;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.142 : les bulles de l'arbre. Au palier qui ouvre l'univers 4, une bulle à la base, sur la graine ; au palier
 * suivant, deux sur le feuillage, qui attendent celle de la base. Elles se paient avec l'énergie du vide accumulée (ce
 * qu'il restait à la formation de chaque univers) ; leurs effets (version 0.148) ont leur test, BullesEffetsTest.
 */
class BullesArbreTest {

    /** L'univers formé, la graine à ce palier, et de l'énergie du vide accumulée. */
    private static Game at(int stage, double accumulated) {
        Game game = GameTestSupport.realGame();
        game.testFormUniverse();
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(stage));
        if (accumulated > 0) game.state().noteVoidHistory("3", accumulated);
        return game;
    }

    @Test
    void lesBullesNaissentAuxPaliersQuatreCinqEtSix() {
        assertEquals(List.of(), at(3, 0).treeBubblesShown(), "avant la fin de la dimension 3, pas de bulle");
        assertEquals(List.of(TreeBubble.SEED), at(4, 0).treeBubblesShown(), "une, à la base");
        assertEquals(List.of(TreeBubble.SEED, TreeBubble.LEAVES_LEFT, TreeBubble.LEAVES_RIGHT), at(5, 0).treeBubblesShown(),
                "puis deux sur le feuillage");
        assertEquals(6, at(6, 0).treeBubblesShown().size(), "puis trois à la cime");
        // Version 0.143 : les schémas des paliers 7 à 11.
        int[] counts = {1, 3, 6, 8, 10, 12, 16, 19};
        for (int stage = 4; stage <= 11; stage++) {
            assertEquals(counts[stage - 4], at(stage, 0).treeBubblesShown().size(), "palier " + stage);
        }
    }

    /** Version 0.143 : les prix des dix-neuf bulles, de 1 000 à 30 000, environ ×1,5 par palier, et leurs noms. */
    @Test
    void lesPrixEtLesNomsDesDixNeufBulles() {
        assertEquals(19, TreeBubble.values().length);
        double[] byStage = {1_000, 2_000, 4_000, 6_000, 9_000, 13_000, 20_000, 30_000};
        for (TreeBubble bubble : TreeBubble.values()) {
            assertEquals(byStage[bubble.stage() - 4], bubble.cost(), 0, bubble + " : le prix de son palier");
        }
        assertEquals("Bulle de la graine", TreeBubble.SEED.label());
        assertEquals("Bulle du bonsaï, à gauche", TreeBubble.LEAVES_LEFT.label());
        assertEquals("Bulle de l'arbuste, au milieu", TreeBubble.CROWN_MIDDLE.label());
        assertEquals("Bulle du jeune arbre, à droite", TreeBubble.YOUNG_RIGHT.label());
        assertEquals("Bulle de l'arbre mûr, à gauche", TreeBubble.MATURE_LEFT.label());
        assertEquals("Bulle de l'arbre ancien, à droite", TreeBubble.ANCIENT_RIGHT.label());
        assertEquals("Bulle de l'arbre géant, à gauche, en bas", TreeBubble.GIANT_LEFT_LOW.label());
        assertEquals("Bulle des racines", TreeBubble.ROOTS.label());
        assertEquals("Bulle de l'arbre-monde, au cœur", TreeBubble.HEART.label());
        assertEquals("Bulle de l'arbre-monde, au sommet", TreeBubble.SUMMIT.label());
        // Chaque nom dit son palier ; d'un palier au suivant, le prix monte d'un tiers à deux fois.
        for (TreeBubble bubble : TreeBubble.values()) {
            assertTrue(bubble.label().startsWith("Bulle "), bubble.label());
            if (bubble.stage() > 4) {
                double previous = byStage[bubble.stage() - 5];
                assertTrue(bubble.cost() >= previous * 1.33 && bubble.cost() <= previous * 2, bubble + " : " + bubble.cost() + " après " + previous);
            }
        }
        // Le prix se paie en énergie du vide accumulée : à 999, la base attend ; à 1 000, elle s'achète et tout est dépensé.
        Game game = at(4, 999);
        assertFalse(game.canBuyTreeBubble(TreeBubble.SEED));
        game = at(4, 1_000);
        assertTrue(game.buyTreeBubble(TreeBubble.SEED));
        assertEquals(0, game.voidAvailable(), 1e-9);
        assertEquals(1_000, game.state().voidSpent(), 1e-9);
        // La dix-neuvième bulle (le sommet, bit 18) se sauvegarde comme les autres.
        game.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
        GameState back = GameTestSupport.reread(game.state());
        assertTrue(back.treeBubbleBought(TreeBubble.SEED.ordinal()) && back.treeBubbleBought(TreeBubble.SUMMIT.ordinal()));
        assertFalse(back.treeBubbleBought(TreeBubble.HEART.ordinal()));
    }

    /** Les liens des schémas 7 à 11, tels que dessinés. */
    @Test
    void lesLiensSuiventLesSchemas() {
        assertEquals(List.of(TreeBubble.CROWN_LEFT, TreeBubble.CROWN_MIDDLE), TreeBubble.YOUNG_LEFT.below());
        assertEquals(List.of(TreeBubble.CROWN_RIGHT, TreeBubble.YOUNG_RIGHT), TreeBubble.MATURE_RIGHT.below());
        assertEquals(List.of(TreeBubble.YOUNG_LEFT, TreeBubble.MATURE_LEFT), TreeBubble.ANCIENT_LEFT.below());
        assertEquals(List.of(TreeBubble.MATURE_LEFT, TreeBubble.GIANT_LEFT_HIGH), TreeBubble.GIANT_LEFT_LOW.below());
        assertEquals(List.of(TreeBubble.SEED, TreeBubble.GIANT_LEFT_LOW, TreeBubble.GIANT_RIGHT_LOW), TreeBubble.ROOTS.below());
        assertEquals(List.of(TreeBubble.ANCIENT_LEFT, TreeBubble.ANCIENT_RIGHT, TreeBubble.HEART), TreeBubble.SUMMIT.below());
        // Chaque lien vaut dans les deux sens.
        assertTrue(TreeBubble.SEED.links().containsAll(List.of(TreeBubble.LEAVES_LEFT, TreeBubble.LEAVES_RIGHT, TreeBubble.ROOTS)));
        for (TreeBubble bubble : TreeBubble.values()) {
            for (TreeBubble linked : bubble.links()) assertTrue(linked.links().contains(bubble), bubble + " – " + linked);
            for (TreeBubble under : bubble.below()) assertTrue(under.stage() <= bubble.stage(), bubble + " après " + under);
        }
    }

    /** Version 0.143 : une bulle s'ouvre par n'importe quelle bulle reliée, aussi par une bulle d'un palier d'après. */
    @Test
    void uneBulleSOuvreParNImporteQuelLien() {
        Game game = at(11, 1e12);
        assertFalse(game.isTreeBubbleOpen(TreeBubble.ROOTS), "rien d'acheté : seule la base s'ouvre");
        assertTrue(game.isTreeBubbleOpen(TreeBubble.SEED));
        assertTrue(game.buyTreeBubble(TreeBubble.SEED));
        assertTrue(game.buyTreeBubble(TreeBubble.ROOTS), "les racines, reliées à la base");
        assertTrue(game.buyTreeBubble(TreeBubble.GIANT_LEFT_LOW), "puis l'arbre géant, par les racines");
        assertTrue(game.buyTreeBubble(TreeBubble.MATURE_LEFT), "puis l'arbre mûr, par le lien d'en haut");
        assertTrue(game.isTreeBubbleOpen(TreeBubble.CROWN_LEFT), "l'arbuste s'ouvre par l'arbre mûr");
        assertFalse(game.isTreeBubbleOpen(TreeBubble.CROWN_RIGHT));
    }

    @Test
    void lesBullesSePaientAvecLEnergieDuVideAccumulee() {
        double base = TreeBubble.SEED.cost(), stock = base * 2.5;
        Game game = at(4, stock);
        assertEquals(stock, game.voidAvailable(), 1e-9);
        assertTrue(game.buyTreeBubble(TreeBubble.SEED));
        assertTrue(game.isTreeBubbleBought(TreeBubble.SEED));
        assertEquals(stock - base, game.voidAvailable(), 1e-9, "le prix de la bulle dépensé");
        assertEquals(stock, game.voidAccumulated(), 1e-9, "le cumul affiché reste celui des univers formés");
        assertFalse(game.buyTreeBubble(TreeBubble.SEED), "une bulle s'achète une fois");
        assertFalse(game.canBuyTreeBubble(TreeBubble.LEAVES_LEFT), "le feuillage attend son palier");
        assertFalse(at(4, base / 2).canBuyTreeBubble(TreeBubble.SEED), "la moitié du prix ne suffit pas");
    }

    @Test
    void ilFautLaBulleDeLaBasePourAcheterLesAutres() {
        Game game = at(5, 1e12);
        assertTrue(game.isTreeBubbleShown(TreeBubble.LEAVES_LEFT));
        assertFalse(game.isTreeBubbleOpen(TreeBubble.LEAVES_LEFT));
        assertFalse(game.buyTreeBubble(TreeBubble.LEAVES_RIGHT), "sans la bulle de la base");
        assertTrue(game.buyTreeBubble(TreeBubble.SEED));
        assertTrue(game.buyTreeBubble(TreeBubble.LEAVES_LEFT));
        assertTrue(game.buyTreeBubble(TreeBubble.LEAVES_RIGHT));
        assertEquals(1e12 - TreeBubble.SEED.cost() - TreeBubble.LEAVES_LEFT.cost() - TreeBubble.LEAVES_RIGHT.cost(),
                game.voidAvailable(), 1e-3);
    }

    /** L'arbre du schéma : chaque bulle de la cime s'ouvre par une des bulles du feuillage reliées à elle. */
    @Test
    void lesBullesDeLaCimeSuiventLesLiens() {
        Game game = at(6, 1e12);
        assertTrue(game.buyTreeBubble(TreeBubble.SEED));
        assertTrue(game.buyTreeBubble(TreeBubble.LEAVES_LEFT));
        assertTrue(game.isTreeBubbleOpen(TreeBubble.CROWN_LEFT));
        assertTrue(game.isTreeBubbleOpen(TreeBubble.CROWN_MIDDLE), "le milieu est relié aux deux : une suffit");
        assertFalse(game.isTreeBubbleOpen(TreeBubble.CROWN_RIGHT), "la droite attend la bulle de droite");
        assertFalse(game.buyTreeBubble(TreeBubble.CROWN_RIGHT));
        assertTrue(game.buyTreeBubble(TreeBubble.CROWN_MIDDLE));
        assertTrue(game.buyTreeBubble(TreeBubble.LEAVES_RIGHT));
        assertTrue(game.buyTreeBubble(TreeBubble.CROWN_RIGHT));
        assertEquals(List.of(TreeBubble.LEAVES_LEFT, TreeBubble.LEAVES_RIGHT), TreeBubble.CROWN_MIDDLE.below());
    }

    @Test
    void lesBullesEtLaDepenseTraversentLesUnivers() {
        double base = TreeBubble.SEED.cost();
        Game game = at(4, base * 5);
        assertTrue(game.buyTreeBubble(TreeBubble.SEED));
        assertTrue(game.startNextUniverse());
        game.state().setVoidOpen(true);
        game.state().setVoidEnergy(BigNum.of(base * 3));
        game.testFormUniverse();
        assertTrue(game.isTreeBubbleBought(TreeBubble.SEED));
        assertEquals(base * 8, game.voidAccumulated(), 1e-6, "ce qui restait s'ajoute au cumul d'avant");
        assertEquals(base * 7, game.voidAvailable(), 1e-6, "moins le prix de la bulle");
        game.testResetUniverses();
        assertFalse(game.isTreeBubbleBought(TreeBubble.SEED));
        assertEquals(0, game.voidAvailable(), 1e-9);
    }
}

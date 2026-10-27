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
 * qu'il restait à la formation de chaque univers) ; pour l'instant, sans autre effet.
 */
class BullesArbreTest {

    /** L'univers formé, la graine à ce palier, et de l'énergie du vide accumulée. */
    private static Game at(int stage, double accumulated) {
        Game game = new Game();
        game.start();
        DeuxiemeUniversTest.formUniverse(game);
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
        assertEquals(6, at(11, 0).treeBubblesShown().size());
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
        DeuxiemeUniversTest.formUniverse(game);
        assertTrue(game.isTreeBubbleBought(TreeBubble.SEED));
        assertEquals(base * 8, game.voidAccumulated(), 1e-6, "ce qui restait s'ajoute au cumul d'avant");
        assertEquals(base * 7, game.voidAvailable(), 1e-6, "moins le prix de la bulle");
        game.testResetUniverses();
        assertFalse(game.isTreeBubbleBought(TreeBubble.SEED));
        assertEquals(0, game.voidAvailable(), 1e-9);
    }
}

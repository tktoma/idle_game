package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.LineUpgrade;
import org.junit.jupiter.api.Test;

/**
 * La ligne, première dimension (version 0.101, ouverte avec le premier univers formé) : des boules qui vont et viennent
 * sur une ligne graduée, un dixième de segment par trait touché ; cinq améliorations (densité, graduation, élan, boules,
 * lignes) avec leurs prix ; le seuil des 250 000 segments gagnés (version 0.136).
 */
class LigneTest {

    /** Une partie au premier univers formé, la ligne remise au départ, sans segment. */
    private static Game line() {
        Game game = GameTestSupport.universe(1);
        game.state().setLinePhase(0);
        game.state().setDimensionShards(0);
        return game;
    }

    @Test
    void laLigneDonneUnDixiemeDeSegmentParTraitTouche() {
        Game before = GameTestSupport.realGame();
        before.tick(100);
        assertFalse(before.isLineOpen(), "avant l'univers, pas de dimension");
        assertEquals(0, before.state().dimensionShards(), 0);
        Game game = line();
        assertTrue(game.isLineOpen());
        assertEquals(2, game.lineTicks(), "un trait au début, un à la fin");
        game.tick(Game.LINE_HALF_TRIP * 0.5);
        assertEquals(0, game.state().dimensionShards(), 1e-9, "à mi-chemin, aucun trait touché");
        assertEquals(0.5, game.linePosition(), 1e-9);
        game.tick(Game.LINE_HALF_TRIP * 0.5);
        assertEquals(0.1, game.state().dimensionShards(), 1e-9, "le bout : 0,1 segment");
        game.tick(Game.LINE_HALF_TRIP);
        assertEquals(0.2, game.state().dimensionShards(), 1e-9, "retour au début : 0,1 de plus");
    }

    @Test
    void unGrandPasCompteAutantQueBeaucoupDePetits() {
        Game big = line(), small = line();
        big.tick(100);
        for (int k = 0; k < 1000; k++) small.tick(0.1);
        assertEquals(big.state().dimensionShards(), small.state().dimensionShards(), 1e-6);
        // 100 s à 2,4 s l'aller : 41 bouts touchés, 0,1 chacun.
        assertEquals(4.1, big.state().dimensionShards(), 1e-6);
    }

    @Test
    void laDensiteEtLaGraduation() {
        Game game = line();
        game.state().setDimensionShards(0.5);
        assertFalse(game.canBuyLineYield(), "il faut 1 segment");
        game.state().setDimensionShards(1);
        assertTrue(game.buyLineYield());
        assertEquals(0, game.state().dimensionShards(), 1e-9);
        assertEquals(0.2, game.lineShardsPerTouch(), 1e-9, "0,1 de plus par trait");
        assertEquals(3, game.lineYieldCost(), 1e-9, "×3 par niveau");
        game.state().setDimensionShards(2);
        assertTrue(game.buyLineTicks());
        assertEquals(3, game.lineTicks(), "un trait au milieu");
        assertEquals(6, game.lineTicksCost(), 1e-9, "2 puis 6 segments");
        game.state().setDimensionShards(0);
        game.state().setLinePhase(0);
        game.tick(Game.LINE_HALF_TRIP * 0.5);
        assertEquals(0.2, game.state().dimensionShards(), 1e-9, "le trait du milieu, à égale distance des bouts");
        game.tick(Game.LINE_HALF_TRIP * 0.5);
        assertEquals(0.4, game.state().dimensionShards(), 1e-9);
        assertEquals(2 * 0.2 / Game.LINE_HALF_TRIP, game.lineShardsPerSecond(), 1e-12);
        game.state().setLineTicksLevel(Game.LINE_MAX_EXTRA_TICKS);
        game.state().setDimensionShards(1e9);
        assertFalse(game.canBuyLineTicks(), "quarante traits au plus");
        assertEquals(Double.POSITIVE_INFINITY, game.lineTicksCost());
        // Les segments et les améliorations traversent les univers.
        game.state().setDimensionShards(5);
        assertTrue(game.startNextUniverse());
        assertEquals(5, game.state().dimensionShards(), 1e-9);
        assertEquals(1, game.state().lineYieldLevel());
    }

    @Test
    void elanBoulesEtLignesMultiplientLesSegments() {
        Game game = line();
        double base = game.lineShardsPerSecond();
        assertEquals(0.1 / 2.4, base, 1e-12, "un dixième tous les 2,4 s");
        game.state().setDimensionShards(1e6);
        assertTrue(game.buyLine(LineUpgrade.SPEED));
        assertEquals(1.25, game.lineSpeed(), 1e-12, "+25 % de vitesse");
        assertEquals(2.4 / 1.25, game.lineHalfTrip(), 1e-12);
        assertTrue(game.buyLine(LineUpgrade.BALLS));
        assertEquals(2, game.lineBalls());
        assertTrue(game.buyLine(LineUpgrade.LINES));
        assertEquals(2, game.lineCount());
        assertEquals(base * 1.25 * 2 * 2, game.lineShardsPerSecond(), 1e-12);
        // Le compte exact suit le débit annoncé, sur un long moment.
        game.state().setDimensionShards(0);
        game.state().setLinePhase(0);
        game.tick(600);
        assertEquals(game.lineShardsPerSecond() * 600, game.state().dimensionShards(), 0.5);
        // Les boules d'une ligne sont réparties : la deuxième est au bout quand la première part.
        game.state().setLinePhase(0);
        assertEquals(0, game.linePosition(0, 0), 1e-12);
        assertEquals(1, game.linePosition(0, 1), 1e-12);
        game.state().setLineCountLevel(Game.LINE_MAX_EXTRA_LINES);
        assertFalse(game.canBuyLine(LineUpgrade.LINES), "cinq lignes au plus");
    }

    @Test
    void lesPrixDesCinqAmeliorationsEtLeurCroissance() {
        Game game = line();
        assertEquals(1, game.lineCost(LineUpgrade.YIELD), 1e-9, "densité : 1 segment");
        assertEquals(2, game.lineCost(LineUpgrade.TICKS), 1e-9, "graduation : 2");
        assertEquals(5, game.lineCost(LineUpgrade.SPEED), 1e-9, "élan : 5");
        assertEquals(10, game.lineCost(LineUpgrade.BALLS), 1e-9, "boules : 10");
        assertEquals(25, game.lineCost(LineUpgrade.LINES), 1e-9, "lignes : 25");
        game.state().setDimensionShards(4.9);
        assertFalse(game.canBuyLine(LineUpgrade.SPEED), "4,9 segments : l'élan attend");
        game.state().setDimensionShards(5);
        assertTrue(game.buyLine(LineUpgrade.SPEED));
        assertEquals(0, game.state().dimensionShards(), 1e-9, "le prix est pris");
        assertEquals(15, game.lineCost(LineUpgrade.SPEED), 1e-9, "élan ×3 par niveau");
        game.state().setLineSpeedLevel(2);
        assertEquals(45, game.lineCost(LineUpgrade.SPEED), 1e-9);
        game.state().setLineBallsLevel(1);
        assertEquals(40, game.lineCost(LineUpgrade.BALLS), 1e-9, "boules ×4 par niveau");
        game.state().setLineBallsLevel(3);
        assertEquals(640, game.lineCost(LineUpgrade.BALLS), 1e-9);
        game.state().setLineCountLevel(1);
        assertEquals(125, game.lineCost(LineUpgrade.LINES), 1e-9, "lignes ×5 par niveau");
        game.state().setLineCountLevel(3);
        assertEquals(3125, game.lineCost(LineUpgrade.LINES), 1e-9);
        // Au plus haut : plus de prix.
        game.state().setLineSpeedLevel(Game.LINE_MAX_SPEED);
        assertEquals(Double.POSITIVE_INFINITY, game.lineCost(LineUpgrade.SPEED));
        game.state().setLineBallsLevel(Game.LINE_MAX_EXTRA_BALLS);
        assertEquals(Double.POSITIVE_INFINITY, game.lineCost(LineUpgrade.BALLS));
        assertEquals(10, game.lineBalls(), "dix boules par ligne au plus");
        assertEquals(6, game.lineSpeed(), 1e-12, "vitesse ×6 au plus (vingt niveaux de +25 %)");
        // Fermée, la ligne ne vend rien.
        Game closed = GameTestSupport.realGame();
        closed.state().setDimensionShards(1e9);
        assertFalse(closed.canBuyLine(LineUpgrade.YIELD));
        assertFalse(closed.buyLine(LineUpgrade.YIELD));
    }

    @Test
    void leSeuilDeLaLigneCompteLesSegmentsGagnes() {
        Game game = line();
        assertEquals(250_000, Game.LINE_THRESHOLD, 0, "version 0.136 : 250 000 segments gagnés");
        assertFalse(game.isLineThresholdReached());
        game.state().setDimensionShardsEarned(249_999.9);
        game.tick(Game.LINE_HALF_TRIP);
        assertTrue(game.isLineThresholdReached(), "le dixième de segment qui manquait");
        assertTrue(game.isDimensionThresholdReached(1));
        game.state().setDimensionShards(1);
        assertTrue(game.buyLineYield());
        assertTrue(game.isLineThresholdReached(), "dépenser ne fait pas redescendre sous le seuil");
    }
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.SeedUpgrade;
import idle.core.save.SaveCodec;
import org.junit.jupiter.api.Test;

/**
 * Version 0.139 : les améliorations de la graine, payées avec la ressource des dimensions (segments, facettes, sphères,
 * instants), et le chrono qui fait passer tout le jeu plus vite.
 */
class GraineAmeliorationsTest {

    /** Quatre univers : les quatre dimensions sont ouvertes, l'arrosoir débloqué, de quoi tout payer. */
    private static Game rich() {
        Game game = GameTestSupport.universe(4);
        assertTrue(game.unlockWateringCan());
        game.state().setDimensionShards(1e12);
        game.state().setPlaneShards(1e12);
        game.state().setVolumeShards(1e12);
        game.state().setDurationShards(1e15);
        return game;
    }

    @Test
    void chaqueAmeliorationSePaieAvecLaRessourceDeSaDimension() {
        Game game = rich();
        double segments = game.dimensionResource(1);
        assertTrue(game.buySeedUpgrade(SeedUpgrade.WATERING));
        assertEquals(segments - 25_000, game.dimensionResource(1), 1e-3, "le débit se paie en segments");
        double facettes = game.dimensionResource(2);
        assertTrue(game.buySeedUpgrade(SeedUpgrade.HOSE));
        assertEquals(facettes - 350, game.dimensionResource(2), 1e-3, "le tuyau, en facettes");
        assertEquals(350 * 2.5, game.seedUpgradeCost(SeedUpgrade.HOSE), 1e-9);
        assertTrue(game.buySeedUpgrade(SeedUpgrade.HOSE));
        assertEquals(2188, game.seedUpgradeCost(SeedUpgrade.HOSE), 1e-9, "350 × 2,5² = 2 187,5, arrondi au-dessus");
        double spheres = game.dimensionResource(3);
        assertTrue(game.buySeedUpgrade(SeedUpgrade.SOIL));
        assertEquals(spheres - 800, game.dimensionResource(3), 1e-3, "la terre fertile, en sphères");
        assertTrue(game.buySeedUpgrade(SeedUpgrade.CHRONO));
        assertFalse(game.canBuySeedUpgrade(SeedUpgrade.CHRONO), "un seul étage");
        assertEquals("segments", Game.dimensionResourceName(1));
        assertEquals("instants", Game.dimensionResourceName(4));
    }

    @Test
    void lesAmeliorationsAttendentLeurDimensionLArrosoirEtLEtageDuChrono() {
        Game game = GameTestSupport.universe(1);
        game.state().setDimensionShards(1e12);
        game.state().setPlaneShards(1e12);
        assertFalse(game.isSeedUpgradeOpen(SeedUpgrade.WATERING), "pas d'arrosoir");
        assertTrue(game.unlockWateringCan());
        assertTrue(game.buySeedUpgrade(SeedUpgrade.WATERING));
        assertFalse(game.canBuySeedUpgrade(SeedUpgrade.HOSE), "le plan n'est pas ouvert");
        assertTrue(game.testAddUniverse());
        assertTrue(game.buySeedUpgrade(SeedUpgrade.HOSE));
        assertFalse(game.isSeedUpgradeOpen(SeedUpgrade.CHRONO_FILL), "pas d'étage du chrono");
    }

    @Test
    void leDebitLeTuyauEtLaTerreFertileArrosentPlus() {
        Game game = rich();
        game.state().setSeedSprouted(true);
        assertEquals(1, game.wateringCanRate(), 1e-12);
        assertTrue(game.buySeedUpgrade(SeedUpgrade.WATERING));
        assertEquals(2, game.wateringCanRate(), 1e-12, "+1 arrosage par seconde");
        game.setWatering(true);
        game.tick(3);
        assertEquals(6, game.state().seedWaterings(), "2 par seconde, 3 secondes");
        game.setWatering(false);
        // Le tuyau verse seul 10 % de l'arrosoir par niveau.
        assertTrue(game.buySeedUpgrade(SeedUpgrade.HOSE));
        assertEquals(0.2, game.hoseRate(), 1e-12);
        game.tick(10);
        assertEquals(8, game.state().seedWaterings(), "10 s de tuyau à 0,2 par seconde : 2 arrosages, sans tenir l'arrosoir");
        // La terre fertile : chaque arrosage compte double, puis ×2 encore.
        assertTrue(game.buySeedUpgrade(SeedUpgrade.SOIL));
        assertTrue(game.buySeedUpgrade(SeedUpgrade.SOIL));
        assertEquals(4, game.seedSoilFactor());
        assertTrue(game.tendSeed());
        assertEquals(12, game.state().seedWaterings());
    }

    @Test
    void leChronoSeRemplitPuisAccelereToutLeJeuAvecDesPertes() {
        Game game = rich();
        assertTrue(game.buySeedUpgrade(SeedUpgrade.CHRONO));
        assertEquals(0.1, game.chronoFillRate(), 1e-12);
        assertEquals(300, game.chronoCapacity(), 1e-12);
        assertEquals(2, game.chronoMaxSpeed(), 1e-12);
        game.tick(100);
        assertEquals(10, game.chronoStored(), 1e-9, "0,1 s par seconde");
        game.tickAway(100);
        assertEquals(20, game.chronoStored(), 1e-9, "il se remplit aussi en absence");
        game.setChronoSpeed(5);
        assertEquals(2, game.chronoSpeed(), 1e-12, "pas plus que l'accélération la plus forte");
        assertEquals(0.1, game.chronoLoss(2), 1e-12, "×2 : 10 % perdus");
        double before = game.state().timePlayed();
        game.tick(1);
        assertEquals(2, game.state().timePlayed() - before, 1e-9, "une seconde réelle en vaut deux");
        assertEquals(20 + 0.1 - 1 / 0.9, game.chronoStored(), 1e-9, "la seconde de plus coûte 1 ÷ 0,9 s au chrono");
        before = game.state().timePlayed();
        game.tickAway(10);
        assertEquals(10, game.state().timePlayed() - before, 1e-9, "une absence n'est pas accélérée");
        // Vide, il ne donne que ce qui reste.
        game.state().setChronoStored(0.9);
        before = game.state().timePlayed();
        game.tick(10);
        assertEquals(10 + (0.9 + 1.0) * 0.9, game.state().timePlayed() - before, 1e-9);
        assertEquals(0, game.chronoStored(), 1e-9);
        assertFalse(game.isChronoRunning(), "vide, il n'accélère plus");
    }

    @Test
    void lesAmeliorationsDuChrono() {
        Game game = rich();
        assertTrue(game.buySeedUpgrade(SeedUpgrade.CHRONO));
        for (SeedUpgrade upgrade : new SeedUpgrade[] {SeedUpgrade.CHRONO_FILL, SeedUpgrade.CHRONO_CAPACITY,
                SeedUpgrade.CHRONO_SPEED, SeedUpgrade.CHRONO_LOSS}) assertTrue(game.buySeedUpgrade(upgrade), upgrade.name());
        assertEquals(0.2, game.chronoFillRate(), 1e-12);
        assertEquals(600, game.chronoCapacity(), 1e-12);
        assertEquals(3, game.chronoMaxSpeed(), 1e-12);
        assertEquals(0.1 * 2 * 0.85, game.chronoLoss(3), 1e-12, "×3 : 20 % de pertes, ×0,85 par niveau");
        game.state().setSeedUpgradeLevel(SeedUpgrade.CHRONO_LOSS.ordinal(), 0);
        game.state().setSeedUpgradeLevel(SeedUpgrade.CHRONO_SPEED.ordinal(), 20);
        assertEquals(0.9, game.chronoLoss(22), 1e-12, "90 % au plus");
    }

    @Test
    void toutSeSauvegardeEtTraverseLesUnivers() throws Exception {
        Game game = rich();
        assertTrue(game.buySeedUpgrade(SeedUpgrade.SOIL));
        assertTrue(game.buySeedUpgrade(SeedUpgrade.CHRONO));
        game.state().setChronoStored(42);
        game.setChronoSpeed(1.5);
        GameState copy = SaveCodec.read(SaveCodec.write(game.state(), 1)).state();
        assertEquals(1, copy.seedUpgradeLevel(SeedUpgrade.SOIL.ordinal()));
        assertEquals(1, copy.seedUpgradeLevel(SeedUpgrade.CHRONO.ordinal()));
        assertEquals(42, copy.chronoStored(), 1e-9);
        assertEquals(1.5, copy.chronoSpeed(), 1e-9);
        String text = SaveCodec.write(game.state(), 1).replaceAll("(?m)^(seedUpgrade[0-9]+|chrono[A-Za-z]*)=.*\\R", "");
        assertFalse(text.contains("seedUpgrade0=") || text.contains("chronoStored="), "les clés de la 0.139 sont retirées");
        GameState old = SaveCodec.read(text).state();
        assertEquals(0, old.seedUpgradeLevel(SeedUpgrade.SOIL.ordinal()), "une sauvegarde d'avant repart sans amélioration");
        assertEquals(0, old.chronoStored(), 1e-12);
        assertEquals(1, old.chronoSpeed(), 1e-12);
        game.testFormUniverse();
        assertEquals(1, game.seedUpgradeLevel(SeedUpgrade.SOIL), "former l'univers suivant ne les reprend pas");
        assertEquals(42, game.chronoStored(), 1e-9);
        game.testResetUniverses();
        assertEquals(0, game.seedUpgradeLevel(SeedUpgrade.SOIL));
        assertEquals(0, game.chronoStored(), 1e-12);
    }
}

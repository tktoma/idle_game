package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.SeedUpgrade;
import idle.core.save.SaveCodec;
import org.junit.jupiter.api.Test;

/**
 * Version 0.179 : la graine se paie en temps de jeu des dimensions. Les prix des améliorations dérivent du seuil de leur
 * dimension (un quart d'heure de jeu appliqué), les paliers demandent de plus en plus d'arrosages, la terre fertile
 * compte un arrosage de plus par niveau, et une sauvegarde d'avant garde son palier.
 */
class Graine179Test {

    @Test
    void chaqueAmeliorationCouteUnCinquiemeDuSeuilDeSaDimensionPuisUnVirguleSixParNiveau() {
        for (SeedUpgrade upgrade : SeedUpgrade.values()) {
            double threshold = Game.dimensionThreshold(upgrade.dimension());
            assertTrue(threshold > 0 && threshold < Double.POSITIVE_INFINITY, upgrade + " : sa dimension a un seuil");
            if (upgrade.max() == 1) {
                assertEquals(threshold * SeedUpgrade.SEED_SINGLE_SHARE, upgrade.baseCost(), 1e-9,
                        upgrade + " : un achat unique vaut un tiers du seuil, cinq minutes de jeu");
            } else {
                assertEquals(threshold * SeedUpgrade.SEED_PRICE_SHARE, upgrade.baseCost(), 1e-9,
                        upgrade + " : le premier niveau vaut un cinquième du seuil, trois minutes de jeu");
                assertEquals(SeedUpgrade.SEED_PRICE_GROWTH, upgrade.growth(), 1e-12, upgrade + " : ×1,6 par niveau");
            }
        }
        assertEquals(50_000, SeedUpgrade.WATERING.baseCost(), 1e-9, "250 000 segments / 5");
        assertEquals(700, SeedUpgrade.HOSE.baseCost(), 1e-9, "3 500 facettes / 5");
        assertEquals(1_600, SeedUpgrade.SOIL.baseCost(), 1e-9, "8 000 sphères / 5");
        assertEquals(3_000_000, SeedUpgrade.CHRONO_LOSS.baseCost(), 1e-9, "15 000 000 instants / 5");
        assertEquals(5_000_000, SeedUpgrade.CHRONO.baseCost(), 1e-9, "l'étage du chrono : 15 000 000 / 3");
        assertEquals(1_000, SeedUpgrade.ASTRAL_LIGHT.baseCost(), 1e-9, "5 000 ondes / 5");
        // Dix niveaux de terre fertile coûtent ensemble 1 600 × (1,6¹⁰ − 1) / 0,6 : moins de six heures de volume au rythme de départ.
        double soil = 0;
        for (int level = 0; level < SeedUpgrade.SOIL.max(); level++) soil += SeedUpgrade.SOIL.cost(level);
        assertTrue(soil < 8_000 * 40, "dix niveaux de terre fertile : " + soil + " sphères, moins de dix heures de volume au rythme de départ");
        // Un prix est toujours entier, arrondi au-dessus : 700 × 1,6² = 1 792.
        Game game = GameTestSupport.universe(2);
        game.state().setPlaneShards(1e9);
        assertTrue(game.buySeedUpgrade(SeedUpgrade.HOSE) && game.buySeedUpgrade(SeedUpgrade.HOSE));
        assertEquals(1_792, game.seedUpgradeCost(SeedUpgrade.HOSE), 1e-9);
    }

    @Test
    void lesPaliersDemandentDePlusEnPlusDArrosagesEtLaTerreFertileCompteUnDePlusParNiveau() {
        long[] steps = {30, 60, 900, 2_700, 5_400, 10_800, 16_200, 21_600, 32_400, 43_200};
        long total = 0;
        for (int stage = 2; stage <= Game.SEED_STAGES; stage++) {
            total += steps[stage - 2];
            assertEquals(total, Game.seedStageWaterings(stage), "palier " + stage);
            if (stage > 2) assertTrue(steps[stage - 2] > steps[stage - 3], "chaque palier demande plus que le précédent");
        }
        assertEquals(0, Game.seedStageWaterings(1));
        assertEquals(990, Game.seedStageWaterings(4), "le bonsaï et ses premières bulles : un quart d'heure d'arrosoir");
        assertEquals(133_290, Game.seedStageWaterings(Game.SEED_STAGES), "l'arbre-monde : douze heures d'arrosoir seul");
        Game game = GameTestSupport.universe(3);
        game.state().setSeedSprouted(true);
        game.state().setVolumeShards(1e9);
        assertEquals(1, game.seedSoilFactor());
        for (int level = 1; level <= SeedUpgrade.SOIL.max(); level++) {
            assertTrue(game.buySeedUpgrade(SeedUpgrade.SOIL));
            assertEquals(1 + level, game.seedSoilFactor(), "niveau " + level);
        }
        assertFalse(game.canBuySeedUpgrade(SeedUpgrade.SOIL), "dix niveaux au plus");
        long before = game.state().seedWaterings();
        assertTrue(game.tendSeed());
        assertEquals(before + 11, game.state().seedWaterings(), "au dixième niveau, chaque arrosage compte pour onze");
    }

    @Test
    void uneSauvegardeDAvantGardeSonPalierEtSaPartVersLeSuivant() throws Exception {
        // Avant la 0.179 : trente arrosages par palier. 165 arrosages = palier 6 (150) et la moitié du chemin vers le 7.
        Game old = GameTestSupport.universe(4);
        old.state().setSeedSprouted(true);
        old.state().setSeedWaterings(165);
        String text = sansMarque(SaveCodec.write(old.state(), 0));
        assertFalse(text.contains("seedStages179"), "la marque de la 0.179 est retirée : une sauvegarde d'avant");
        GameState loaded = SaveCodec.read(text).state();
        Game game = new Game(loaded, idle.core.upgrades.Upgrades.DEFAULT, idle.core.upgrades.Automations.DEFAULT);
        assertEquals(6, game.seedStage(), "le palier atteint est gardé");
        long stage6 = Game.seedStageWaterings(6), stage7 = Game.seedStageWaterings(7);
        assertEquals(stage6 + (stage7 - stage6) * 15 / 30, loaded.seedWaterings(), "la moitié du chemin vers le palier 7, en proportion");
        // Une sauvegarde de la 0.179 se relit telle quelle.
        String fresh = SaveCodec.write(game.state(), 0);
        assertTrue(fresh.contains("seedStages179=1"));
        assertEquals(loaded.seedWaterings(), SaveCodec.read(fresh).state().seedWaterings());
        // Une sauvegarde d'avant où la graine n'a pas germé ne change pas.
        Game bare = GameTestSupport.universe(1);
        assertEquals(0, SaveCodec.read(sansMarque(SaveCodec.write(bare.state(), 0))).state().seedWaterings());
    }

    @Test
    void laGraineNePrendPasPlusDeDeuxPaliersDAvanceSurLesUnivers() {
        Game game = GameTestSupport.universe(1);
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(Game.SEED_STAGES));
        assertEquals(3, game.seedStage(), "un univers formé : le palier 3 au plus, même avec les arrosages de l'arbre-monde");
        assertEquals(3, game.seedStageCap());
        assertTrue(game.isSeedStageCapped());
        assertTrue(game.canTendSeed(), "elle boit quand même : les arrosages comptent pour la suite");
        Game later = GameTestSupport.universe(4);
        later.state().setSeedSprouted(true);
        later.state().setSeedWaterings(Game.seedStageWaterings(Game.SEED_STAGES));
        assertEquals(6, later.seedStage(), "quatre univers : l'arbuste, deux paliers d'avance");
        later.state().setSeedWaterings(Game.seedStageWaterings(5));
        assertEquals(5, later.seedStage(), "les arrosages manquent : le bonsaï, pas encore l'arbuste");
        assertFalse(later.isSeedStageCapped());
        Game end = GameTestSupport.universe(9);
        end.state().setSeedSprouted(true);
        end.state().setSeedWaterings(Game.seedStageWaterings(Game.SEED_STAGES));
        assertEquals(Game.SEED_STAGES, end.seedStage(), "neuf univers : l'arbre-monde");
        assertFalse(end.isSeedStageCapped());
    }

    /** La sauvegarde sans la marque de la 0.179 : celle qu'écrivait la version d'avant. */
    private static String sansMarque(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n")) if (!line.startsWith("seedStages179=")) out.append(line).append('\n');
        return out.toString();
    }
}

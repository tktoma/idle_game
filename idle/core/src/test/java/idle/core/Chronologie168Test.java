package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.history.Achats;
import idle.core.history.Pace;
import idle.core.history.PurchaseHistory;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.168 : la chronologie de la partie pour l'onglet Statistiques — le journal des univers formés et de leurs
 * phases, les moments de la partie secondaire (dimensions, graine, défi final), le temps passé dans chaque dimension — et
 * la valeur durable d'un achat, à côté de son gain immédiat.
 */
class Chronologie168Test {

    @Test
    void chaqueUniversFormeEntreAuJournalAvecSonNumeroEtSesEtapes() {
        Game game = GameTestSupport.realGame();
        game.state().setTimePlayed(3_600);
        game.testFormUniverse();
        List<GameStats.UniverseRecord> log = game.state().stats().universeLog();
        assertEquals(1, log.size());
        GameStats.UniverseRecord first = log.get(0);
        assertEquals(1, first.number());
        assertEquals(0, first.start(), 1e-9);
        assertEquals(3_600, first.formed(), 1e-9);
        assertEquals(3_600, first.duration(), 1e-9);
        assertTrue(Double.isNaN(first.phases()[0]), "pas d'explosion notée : la phase est inconnue");
        // L'univers suivant commence à la formation du premier, et porte le numéro 2.
        game.state().setTimePlayed(5_400);
        game.testFormUniverse();
        GameStats.UniverseRecord second = game.state().stats().universeLog().get(1);
        assertEquals(2, second.number());
        assertEquals(3_600, second.start(), 1e-9);
        assertEquals(1_800, second.duration(), 1e-9);
    }

    @Test
    void lesPhasesDUnUniversSontLesEcartsEntreSesEtapes() {
        GameStats.UniverseRecord r = new GameStats.UniverseRecord(4, 100, 400, 1_000, 5_000, 6_000, 9_000);
        assertEquals(List.of(300.0, 600.0, 4_000.0, 1_000.0, 3_000.0), java.util.Arrays.stream(r.phases()).boxed().toList());
        assertEquals(8_900, r.duration(), 1e-9);
        assertEquals(5, GameStats.UniverseRecord.PHASES.size());
    }

    @Test
    void lesMomentsDeLaPartieSecondaireSeNotentUneFoisAvecLeurDate() {
        Game game = GameTestSupport.universe(3);
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(2));
        double t = game.state().timePlayed();
        game.tick(2);
        GameStats stats = game.state().stats();
        for (int d = 1; d <= 3; d++) {
            assertTrue(stats.momentKnown("dimension." + d + ".ouverte"), "la dimension " + d);
            assertTrue(stats.moment("dimension." + d + ".ouverte") >= t, "datée");
        }
        assertFalse(stats.momentKnown("dimension.4.ouverte"));
        assertTrue(stats.momentKnown("graine.1") && stats.momentKnown("graine.2"));
        assertFalse(stats.momentKnown("graine.3"));
        assertFalse(stats.momentKnown("defiFinal.ouvert"));
        double first = stats.moment("dimension.1.ouverte");
        game.tick(5);
        assertEquals(first, stats.moment("dimension.1.ouverte"), 1e-9, "seule la première fois compte");
    }

    @Test
    void leTempsPasseDansUneDimensionSAdditionneParPetitsPas() {
        Game game = GameTestSupport.universe(2);
        game.noteDimensionPlayed(2, 0.25);
        game.noteDimensionPlayed(2, 0.25);
        game.noteDimensionPlayed(2, 600);
        game.noteDimensionPlayed(11, 5);
        game.noteDimensionPlayed(2, Double.NaN);
        assertEquals(1.5, game.state().stats().dimensionPlayTime(2), 1e-9, "une image de plus d'une seconde compte pour une seconde");
        assertEquals(0, game.state().stats().dimensionPlayTime(1), 1e-9);
    }

    @Test
    void chaqueAchatDitJusquOuIlTient() {
        Game game = GameTestSupport.realGame();
        String particle = game.upgrades(Resource.PARTICLES).get(0).id();
        assertEquals(Achats.Tenue.FUSION, Achats.tenue("upgradeLevels", particle));
        assertEquals(Achats.Tenue.EXPLOSION, Achats.tenue("upgradeLevels", "atom_double"));
        assertEquals(Achats.Tenue.EXPLOSION, Achats.tenue("ownedAutomations", "auto_generator"));
        assertEquals(Achats.Tenue.BIG_BANG, Achats.tenue("darkUpgradeLevels", "x"));
        assertEquals(Achats.Tenue.UNIVERS, Achats.tenue("automationBatchLevels", "auto_generator"));
        assertEquals(Achats.Tenue.UNIVERS, Achats.tenue("spaceUpgrades", null));
        assertEquals(Achats.Tenue.PARTIE, Achats.tenue("treeBubbles", null));
        assertEquals(Achats.Tenue.PARTIE, Achats.tenue("dimensionAutomations", "PLANE_AIM"));
        assertEquals(Achats.Tenue.INCONNUE, Achats.tenue("pulses", null));
        assertEquals("toute la partie", Achats.Tenue.PARTIE.label());
    }

    @Test
    void laValeurDurableSuitLaDureeDeVieDeLAchat() {
        Game game = GameTestSupport.gameWithParticles(1e6);
        PurchaseHistory history = new PurchaseHistory();
        Pace pace = new Pace();
        pace.observe(game);
        assertTrue(Double.isNaN(PurchaseHistory.dureeDeVie(game, Achats.Tenue.FUSION)), "pas encore de fusion : pas de durée de vie");
        game.state().stats().noteFusion(idle.core.math.BigNum.ONE, 600);
        assertEquals(600, PurchaseHistory.dureeDeVie(game, Achats.Tenue.FUSION), 1e-9);
        assertTrue(Double.isNaN(PurchaseHistory.dureeDeVie(game, Achats.Tenue.PARTIE)), "toute la partie : pas de fin à mesurer");
        history.before(game);
        game.buy(game.upgrades(Resource.PARTICLES).get(0).id());
        PurchaseHistory.Entry e = history.after(game, pace).get(0);
        assertEquals("jusqu'à la prochaine fusion", e.tenue());
        assertEquals(600, e.dureeSecondes(), 1e-9);
        assertTrue(e.durableSecondes() >= 0 && e.durableSecondes() <= 600, "au plus la durée de vie : " + e.durableSecondes());
    }

    @Test
    void ceQuiManqueAuCosmosSeDitConditionParCondition() {
        // Version 0.168 : pour le bot analyste, l'attente d'une seule condition de la galaxie dit laquelle.
        Game game = GameTestSupport.realGame();
        List<String> galaxy = game.cosmosMissing(idle.core.sky.Cosmos.GALAXY);
        assertEquals(game.cosmosConditions(idle.core.sky.Cosmos.GALAXY), galaxy.size(), "chaque astre, et les systèmes solaires");
        assertTrue(galaxy.contains("Systèmes solaires complets (0 / " + Game.SYSTEMS_FOR_GALAXY + ")"), galaxy.toString());
        assertTrue(galaxy.get(0).endsWith(" : pas encore formé"), galaxy.toString());
        List<String> cluster = game.cosmosMissing(idle.core.sky.Cosmos.CLUSTER);
        assertTrue(cluster.get(0).endsWith(" : pas encore formé"), "l'échelle d'avant d'abord : " + cluster);
        assertTrue(cluster.stream().anyMatch(m -> m.contains(" rassemblé (0 / ")), cluster.toString());
    }
}

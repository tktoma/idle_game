package idle.core;

import idle.core.state.GameState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.TreeBubble;
import idle.core.math.BigNum;
import idle.core.save.SaveCodec;
import idle.core.universe.DimensionAutomation;
import org.junit.jupiter.api.Test;

/**
 * L'automatisation des dimensions. Dans chaque dimension, les automatismes s'achètent l'un après l'autre.
 * Le plan automatique ramasse ses éclats aussi par grands pas (une absence rejouée par pas de 10 s), les
 * dimensions étant découpées en pas d'au plus 0,05 s. Payés avec la ressource de leur dimension ; ceux
 * qui achètent d'abord, puis ceux qui jouent, en cinq paliers de cadence.
 */
class AutomatisationDimensionsTest {
    /** Quatre dimensions ouvertes aux automatismes (cinq univers, l'arbre-monde ; version 0.182), et de quoi payer. */
    private static Game four() {
        Game game = GameTestSupport.universe(4);
        GameTestSupport.openDimensionAutomations(game, 4);
        game.state().setUniverseSeeds(BigNum.of(100));
        game.state().setDimensionShards(1e9);
        game.state().setPlaneShards(1e9);
        game.state().setVolumeShards(1e9);
        game.state().setDurationShards(1e9);
        return game;
    }

    private static void buyAll(Game game, int dimension) {
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() == dimension) assertTrue(game.buyDimensionAutomation(automation), automation.toString());
        }
    }

    /** Achète, dans l'ordre, les automatismes de sa dimension jusqu'à celui-ci compris. */
    static void buyUpTo(Game game, DimensionAutomation last) {
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() != last.dimension()) continue;
            if (!game.ownsDimensionAutomation(automation)) assertTrue(game.buyDimensionAutomation(automation), automation.toString());
            if (automation == last) return;
        }
    }

    @Test
    void lesAutomatismesSuiventLesDimensionsEtLeursAmeliorations() {
        int[] counts = new int[11];
        for (DimensionAutomation automation : DimensionAutomation.values()) counts[automation.dimension()]++;
        assertEquals(5, counts[1], "la ligne : ses cinq améliorations, rien à jouer");
        assertEquals(10, counts[2], "le plan : ses cinq améliorations, puis la visée en cinq paliers");
        assertEquals(10, counts[3]);
        assertEquals(9, counts[4]);
        assertEquals(11, counts[5], "la boucle (version 0.153) : ses six améliorations, puis le rythme");
        assertEquals(10, counts[6], "la brane (version 0.154) : ses cinq améliorations, puis la tirée");
        assertEquals(13, counts[7], "le clicker (version 0.155) : ses huit améliorations, puis les clics");
        assertEquals(11, counts[8], "la grille (version 0.156) : ses six améliorations, puis les essais");
        assertEquals(10, counts[9], "le miroir (version 0.157) : ses cinq améliorations, puis le jeu");
        assertEquals(16, counts[10], "le mélange (refait à la 0.159) : ses onze améliorations, puis le mélange automatique");
        assertEquals(105, DimensionAutomation.values().length);
        // Ceux qui jouent en dernier, du plus lent au plus rapide ; l'ancien nom garde le dernier.
        assertNull(DimensionAutomation.PLANE_VALUE.previous(), "les améliorations d'abord");
        assertEquals(DimensionAutomation.PLANE_SIZE, DimensionAutomation.PLANE_AIM_5S.previous());
        assertEquals(DimensionAutomation.PLANE_AIM_05S, DimensionAutomation.PLANE_AIM.previous());
        double[] periods = {5, 2, 1, 0.5, DimensionAutomation.INSTANT_PERIOD};
        DimensionAutomation[] plane = {DimensionAutomation.PLANE_AIM_5S, DimensionAutomation.PLANE_AIM_2S, DimensionAutomation.PLANE_AIM_1S,
            DimensionAutomation.PLANE_AIM_05S, DimensionAutomation.PLANE_AIM};
        for (int i = 0; i < plane.length; i++) {
            assertEquals(DimensionAutomation.Kind.PLAY, plane[i].kind());
            assertEquals(periods[i], plane[i].playPeriod(), 0, plane[i].toString());
        }
        assertTrue(DimensionAutomation.PLANE_AIM.isInstant() && !DimensionAutomation.PLANE_AIM_05S.isInstant());
        assertEquals("Visée automatique, toutes les 5 s", DimensionAutomation.PLANE_AIM_5S.title());
        assertEquals("Visée automatique, chaque seconde", DimensionAutomation.PLANE_AIM_1S.title());
        assertEquals("Visée automatique, toutes les 0,5 s", DimensionAutomation.PLANE_AIM_05S.title());
        assertEquals("Visée automatique, sans attente", DimensionAutomation.PLANE_AIM.title());
        assertEquals(Double.POSITIVE_INFINITY, DimensionAutomation.PLANE_VALUE.playPeriod());
        // Les prix : avec la ressource de la dimension, en part de son seuil, plus chers à chaque automatisme.
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            assertEquals(automation.dimension(), automation.paidWith(), automation.toString());
            DimensionAutomation previous = automation.previous();
            if (previous != null) assertTrue(automation.cost() > previous.cost(), automation.toString());
        }
        assertEquals("segments", Game.dimensionAutomationCurrency(DimensionAutomation.LINE_YIELD));
        assertEquals("facettes", Game.dimensionAutomationCurrency(DimensionAutomation.PLANE_AIM));
        assertEquals("instants", Game.dimensionAutomationCurrency(DimensionAutomation.DURATION_HOLD));
    }

    @Test
    void ilFautLePalierDeLaGraineEtLesUniversPuisLOrdre() {
        Game game = four();
        long waterings = game.state().seedWaterings();
        game.state().setSeedWaterings(Game.seedStageWaterings(Game.dimensionAutomationStage(1)) - 1);
        assertFalse(game.canBuyDimensionAutomation(DimensionAutomation.LINE_YIELD), "un palier de moins : fermé");
        game.state().setSeedWaterings(waterings);
        assertFalse(game.canBuyDimensionAutomation(DimensionAutomation.LINE_TICKS), "pas avant celui d'avant");
        double segments = game.state().dimensionShards();
        assertTrue(game.buyDimensionAutomation(DimensionAutomation.LINE_YIELD));
        assertEquals(segments - DimensionAutomation.LINE_YIELD.cost(), game.state().dimensionShards(), 1e-6, "payé en segments");
        assertFalse(game.buyDimensionAutomation(DimensionAutomation.LINE_YIELD), "une seule fois");
        assertTrue(game.canBuyDimensionAutomation(DimensionAutomation.LINE_TICKS));
        assertFalse(game.canBuyDimensionAutomation(DimensionAutomation.PLANE_AIM), "le jeu vient après les améliorations");
        double facets = game.state().planeShards();
        buyUpTo(game, DimensionAutomation.PLANE_AIM);
        double paid = 0;
        for (DimensionAutomation automation : DimensionAutomation.values()) if (automation.dimension() == 2) paid += automation.cost();
        assertEquals(facets - paid, game.state().planeShards(), 1e-6, "payé en facettes");
        // Une dimension pas encore ouverte aux automatismes n'a pas les siens : il faut un univers de plus qu'elle.
        Game two = GameTestSupport.universe(2);
        GameTestSupport.openDimensionAutomations(two, 1);
        two.state().setDimensionShards(1e9);
        two.state().setPlaneShards(1e9);
        two.state().setVolumeShards(1e9);
        assertEquals(2, two.universes());
        assertTrue(two.isDimensionAutomationAvailable(DimensionAutomation.LINE_YIELD), "la ligne : deux univers et l'arbre-monde");
        assertFalse(two.isDimensionAutomationAvailable(DimensionAutomation.PLANE_VALUE), "le plan attend le troisième univers");
        assertTrue(two.testAddUniverse());
        assertTrue(two.isDimensionAutomationAvailable(DimensionAutomation.PLANE_VALUE));
        assertFalse(two.isDimensionAutomationAvailable(DimensionAutomation.VOLUME_TIME), "trois univers : pas le volume");
        // La bulle du sommet fait moitié prix.
        assertEquals(DimensionAutomation.PLANE_VALUE.cost(), two.dimensionAutomationCost(DimensionAutomation.PLANE_VALUE), 1e-9);
        two.state().setTreeBubbleBought(TreeBubble.SUMMIT.ordinal(), true);
        assertEquals(DimensionAutomation.PLANE_VALUE.cost() / 2, two.dimensionAutomationCost(DimensionAutomation.PLANE_VALUE), 1e-9);
        double before = two.state().planeShards();
        assertTrue(two.buyDimensionAutomation(DimensionAutomation.PLANE_VALUE));
        assertEquals(before - DimensionAutomation.PLANE_VALUE.cost() / 2, two.state().planeShards(), 1e-6, "payé moitié prix");
    }

    @Test
    void lesAchatsAutomatiquesPrennentLesAmeliorations() {
        Game game = four();
        buyAll(game, 1);
        int before = game.lineLevel(Game.LineUpgrade.SPEED);
        game.tick(0.1);
        assertTrue(game.lineLevel(Game.LineUpgrade.SPEED) > before, "l'élan s'achète seul");
        assertEquals(game.lineMaxLevel(Game.LineUpgrade.BALLS), game.lineLevel(Game.LineUpgrade.BALLS), "les boules jusqu'au plus haut");
        // Coupé à part, il n'achète plus ; « Tout couper » les arrête tous.
        game.state().setDimensionShards(1e12);
        assertTrue(game.setDimensionAutomationEnabled(DimensionAutomation.LINE_YIELD, false));
        int yield = game.lineLevel(Game.LineUpgrade.YIELD);
        game.tick(0.1);
        assertEquals(yield, game.lineLevel(Game.LineUpgrade.YIELD));
        assertTrue(game.setDimensionAutomationEnabled(DimensionAutomation.LINE_YIELD, true));
        game.setAutomationPaused(true);
        game.tick(0.1);
        assertEquals(yield, game.lineLevel(Game.LineUpgrade.YIELD), "tout coupé");
        game.setAutomationPaused(false);
        game.tick(0.1);
        assertTrue(game.lineLevel(Game.LineUpgrade.YIELD) > yield);
    }

    @Test
    void laViseeDuPlanRamasseSeule() {
        Game game = four();
        buyUpTo(game, DimensionAutomation.PLANE_AIM);
        double before = game.state().planeShardsEarned();
        for (int i = 0; i < 1_200; i++) game.tick(0.05);
        assertTrue(game.state().planeShardsEarned() > before + 5, "une minute : " + (game.state().planeShardsEarned() - before));
        Game idle = four();
        buyUpTo(idle, DimensionAutomation.PLANE_SIZE);
        double still = idle.state().planeShardsEarned();
        for (int i = 0; i < 1_200; i++) idle.tick(0.05);
        assertTrue(idle.state().planeShardsEarned() - still <= 1, "sans elle, le point ne bouge pas");
    }

    @Test
    void laPlongeeDuVolumeCueilleSeule() {
        Game game = four();
        buyUpTo(game, DimensionAutomation.VOLUME_DIVE);
        game.state().setVolumeShards(0);   // ses achats automatiques n'allongent pas la plongée pendant la mesure
        double before = game.state().volumeShardsEarned();
        for (int i = 0; i < 1_200; i++) game.tick(0.05);
        assertTrue(game.volumeDives() >= 2, "elle replonge : " + game.volumeDives() + " plongées en une minute");
        assertTrue(game.state().volumeShardsEarned() > before + 10, "des sphères : " + (game.state().volumeShardsEarned() - before));
    }

    @Test
    void lAppuiDeLaDureeSeTientEtSeRelacheSeul() {
        Game game = four();
        buyUpTo(game, DimensionAutomation.DURATION_HOLD);
        game.state().setDurationShards(0);   // ses achats automatiques ne changent pas le temps au plus pendant la mesure
        double max = game.durationMaxSeconds();
        double before = game.state().durationShardsEarned();
        game.tick(0.05);
        assertTrue(game.isDurationHeld());
        game.tick((max + DimensionAutomation.INSTANT_PERIOD) * 3);
        assertEquals(3, game.durationReleases(), "un gros pas fait ses trois relâchements d'un coup");
        assertEquals(before + 3 * game.durationRewardFor(max), game.state().durationShardsEarned(), 1e-6);
        // Le bouton reste tenu : ses améliorations s'achètent quand même, à la main comme par leurs automatismes.
        assertTrue(game.isDurationHeld());
        game.state().setDurationShards(1e9);
        assertTrue(game.canBuyDuration(Game.DurationUpgrade.VALUE));
        int value = game.durationLevel(Game.DurationUpgrade.VALUE);
        game.state().setDurationShards(1e30);
        game.tick(0.05);
        assertTrue(game.durationLevel(Game.DurationUpgrade.VALUE) > value, "la valeur s'achète pendant l'appui");
    }

    @Test
    void ilsTraversentLesUniversEtSeSauvent() throws Exception {
        Game game = four();
        assertTrue(game.buyDimensionAutomation(DimensionAutomation.LINE_YIELD));
        buyUpTo(game, DimensionAutomation.PLANE_AIM);
        game.setDimensionAutomationEnabled(DimensionAutomation.PLANE_AIM, false);
        assertTrue(game.universeTransition(true).kept().stream().anyMatch(line -> line.startsWith("Les automatismes des dimensions (11")));
        game.state().setReplaying(false);
        assertTrue(game.startNextUniverse());
        assertTrue(game.ownsDimensionAutomation(DimensionAutomation.LINE_YIELD) && game.ownsDimensionAutomation(DimensionAutomation.PLANE_AIM));
        assertFalse(game.isDimensionAutomationEnabled(DimensionAutomation.PLANE_AIM), "coupé, il le reste");
        GameState back = GameTestSupport.reread(game.state());
        assertEquals(game.state().dimensionAutomations(), back.dimensionAutomations());
        assertEquals(game.state().dimensionAutomationsOff(), back.dimensionAutomationsOff());
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(game.state(), 0).split("\n")) if (!line.startsWith("dimensionAutomations")) old.append(line).append('\n');
        assertTrue(SaveCodec.read(old.toString()).state().dimensionAutomations().isEmpty(), "une sauvegarde d'avant n'en a pas");
        game.reset();
        assertTrue(game.state().dimensionAutomations().isEmpty(), "« Recommencer de zéro » les reprend");
    }

    /** Version 0.155 : chaque automatisme s'achète ; version 0.209 : payé avec la ressource de sa dimension. */
    @Test
    void tousLesAutomatismesSAchetentAvecLaBonneRessource() {
        Game game = GameTestSupport.universe(10);
        GameTestSupport.openDimensionAutomations(game, 10);   // onze univers et l'arbre-monde
        game.state().setUniverseSeeds(BigNum.of(1_000));
        game.state().setDimensionShards(1e30);
        game.state().setPlaneShards(1e30);
        game.state().setVolumeShards(1e30);
        game.state().setDurationShards(1e30);
        game.state().setLoopShards(1e30);
        game.state().setBraneShards(1e30);
        game.state().setClickerShards(1e30);
        game.state().setGridShards(1e30);
        game.state().setMirrorShards(1e30);
        game.state().setMixShards(1e30);
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            double before = game.dimensionAutomationBalance(automation);
            assertTrue(game.buyDimensionAutomation(automation), automation.toString());
            assertEquals(before - automation.cost(), game.dimensionAutomationBalance(automation), before * 1e-12 + 1e-9, automation.toString());
        }
        assertEquals(DimensionAutomation.values().length, game.dimensionAutomationsOwned());
    }

    @Test
    void parGrandsPasLePlanAutomatiqueRamasseCommeImageParImage() {
        Game fine = four(), coarse = four();
        for (Game game : new Game[] {fine, coarse}) buyUpTo(game, DimensionAutomation.PLANE_AIM);
        double start = fine.state().planeShardsEarned();
        for (int i = 0; i < 6000; i++) fine.tick(0.05);
        for (int i = 0; i < 30; i++) coarse.tick(10);
        double a = fine.state().planeShardsEarned() - start, b = coarse.state().planeShardsEarned() - start;
        assertTrue(a > 10, "le plan automatique ramasse : " + a);
        assertTrue(Math.abs(a - b) <= 0.05 * a, "par pas de 10 s comme par pas de 0,05 s : " + a + " et " + b);
    }

    /** Version 0.209 : un palier ancien dans une sauvegarde donne tous ceux, plus lents, d'avant lui. */
    @Test
    void uneAncienneSauvegardeQuiAvaitLaViseeLaGardeSansAttente() {
        Game game = four();
        game.state().addDimensionAutomation(DimensionAutomation.PLANE_AIM.name());
        GameState back = GameTestSupport.reread(game.state());
        for (DimensionAutomation tier : new DimensionAutomation[] {DimensionAutomation.PLANE_AIM_5S, DimensionAutomation.PLANE_AIM_2S,
            DimensionAutomation.PLANE_AIM_1S, DimensionAutomation.PLANE_AIM_05S, DimensionAutomation.PLANE_AIM}) {
            assertTrue(back.dimensionAutomations().contains(tier.name()), tier.toString());
        }
        assertFalse(back.dimensionAutomations().contains(DimensionAutomation.PLANE_VALUE.name()), "les achats, eux, restent à acheter");
    }

    /** Le plus rapide des paliers en marche joue ; la cadence d'un palier lent se voit dans ce qu'il ramasse. */
    @Test
    void lePlusRapideDesPaliersJoueEtLesLentsRamassentMoins() {
        Game slow = four(), fast = four();
        buyUpTo(slow, DimensionAutomation.PLANE_AIM_5S);
        buyUpTo(fast, DimensionAutomation.PLANE_AIM);
        assertEquals(5, slow.dimensionPlayPeriod(2), 0);
        assertEquals(DimensionAutomation.INSTANT_PERIOD, fast.dimensionPlayPeriod(2), 0);
        double a = slow.state().planeShardsEarned(), b = fast.state().planeShardsEarned();
        for (int i = 0; i < 2_400; i++) {
            slow.tick(0.05);
            fast.tick(0.05);
        }
        a = slow.state().planeShardsEarned() - a;
        b = fast.state().planeShardsEarned() - b;
        assertTrue(b > a, "sans attente : " + b + " ; toutes les 5 s : " + a);
        fast.setDimensionAutomationEnabled(DimensionAutomation.PLANE_AIM, false);
        assertEquals(0.5, fast.dimensionPlayPeriod(2), 0, "le palier coupé laisse jouer le suivant");
        assertEquals(DimensionAutomation.PLANE_AIM_05S, fast.dimensionPlayTier(2));
    }

    /** Version 0.209 : ceux qui achètent gardent de quoi payer l'automatisme suivant de la dimension. */
    @Test
    void ceuxQuiAchetentGardentLePrixDuSuivant() {
        Game game = four();
        buyUpTo(game, DimensionAutomation.PLANE_SIZE);
        double reserve = game.dimensionAutomationReserve(2);
        assertEquals(DimensionAutomation.PLANE_AIM_5S.cost(), reserve, 1e-9);
        game.state().setPlaneShards(reserve * 1.5);
        game.tick(0.05);
        assertTrue(game.state().planeShards() >= reserve - 1e-6, "la réserve reste : " + game.state().planeShards());
        assertTrue(game.buyDimensionAutomation(DimensionAutomation.PLANE_AIM_5S));
    }

    /** Version 0.209 : la main d'un bot joue une dimension comme son automatisme, sans rien acheter ni sauvegarder. */
    @Test
    void laMainDUnBotJoueUneDimension() {
        Game game = four();
        assertFalse(game.isDimensionPlayed(2));
        game.playDimensionByHand(2, 1);
        assertTrue(game.isDimensionPlayed(2));
        assertFalse(game.isDimensionAutoPlayed(2), "aucun palier acheté");
        assertEquals(2, game.dimensionPlayedByHand());
        double before = game.state().planeShardsEarned();
        for (int i = 0; i < 1_200; i++) game.tick(0.05);
        assertTrue(game.state().planeShardsEarned() > before, "elle ramasse");
        game.playDimensionByHand(0, 0);
        assertFalse(game.isDimensionPlayed(2));
        assertEquals(0, game.dimensionPlayedByHand());
    }
}

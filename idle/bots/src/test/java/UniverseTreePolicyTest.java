import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import idle.core.math.BigNum;
import idle.core.universe.DimensionAutomation;
import java.util.Map;
import java.util.Random;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.152 : à partir de l'univers 4, les bots arrosent la graine, achètent les bulles de l'arbre, lient les hadrons
 * et achètent les automatismes des dimensions ({@link UniverseTreePolicy}) ; rien avant. Des états préparés par les
 * outils d'essai du moteur : quelques millisecondes chacun.
 */
class UniverseTreePolicyTest {

    /** Un hasard qui refuse d'être tiré. */
    private static final class NoRandom extends Random {
        private static final long serialVersionUID = 1L;

        @Override
        protected int next(int bits) {
            throw new AssertionError("aucun hasard ne devait être tiré");
        }
    }

    /** {@code formed} univers formés ; avec {@code replaying}, la partie qui mène au suivant est lancée. */
    private static Game universes(int formed, boolean replaying) {
        Game game = Bots.fresh(42);
        game.testFormUniverse();
        for (int u = 1; u < formed; u++) {
            game.startNextUniverse();
            game.testFormUniverse();
        }
        if (replaying) game.startNextUniverse();
        assertEquals(formed, game.universes());
        return game;
    }

    /** La graine à son dernier palier, l'arbre-monde : toutes les bulles sont nées. */
    private static void worldTree(Game game) {
        game.state().setSeedSprouted(true);
        // Version 0.179 : l'arbre-monde demande neuf univers formés (deux paliers d'avance au plus).
        while (game.universes() + Game.SEED_STAGES_AHEAD < Game.SEED_STAGES) game.testAddUniverse();
        game.state().setSeedWaterings(Game.seedStageWaterings(Game.SEED_STAGES));
        assertEquals(Game.SEED_STAGES, game.seedStage());
    }

    private static void own(Game game, TreeBubble... bubbles) {
        for (TreeBubble bubble : bubbles) game.state().setTreeBubbleLevel(bubble.ordinal(), 1);
    }

    @Test
    void rienAvantLUniversQuatreSaufLArrosoir() {
        UniverseTreePolicy policy = new UniverseTreePolicy(new UniverseTreePolicy.Style(0.5, UniverseTreePolicy.Plan.CHEAPEST), new NoRandom());
        // Avant le premier univers formé, rien du tout : pas de graine à arroser, et aucun hasard tiré.
        Game fresh = Bots.fresh(42);
        assertEquals(0, policy.play(fresh), "aucun univers formé : aucun geste");
        assertFalse(fresh.isWateringCanUnlocked());
        // Version 0.179 : dès l'univers 1 formé, l'arrosoir est débloqué et tenu (les paliers demandent des heures) ; les
        // bulles et les hadrons attendent toujours trois univers formés, et les univers 2 et 3 se lancent sans attendre.
        UniverseTreePolicy expert = new UniverseTreePolicy(UniverseTreePolicy.Style.EXPERT, new java.util.Random(1));
        for (int formed = 1; formed < UniverseTreePolicy.FROM_UNIVERSES; formed++) {
            Game game = universes(formed, true);
            game.state().noteVoidHistory("1", 1e6);
            assertTrue(expert.play(game) > 0, formed + " univers formés : l'arrosoir");
            assertTrue(game.isWateringCanUnlocked() && game.isWatering(), "l'arrosoir est débloqué et penché");
            assertTrue(expert.readyToLaunch(game), "les univers 2 et 3 se lancent sans attendre la graine, comme avant");
            assertFalse(expert.savingForHadrons(game));
            assertEquals(0, game.treeBubblesShown().stream().filter(game::isTreeBubbleBought).count());
        }
        Game third = universes(2, false);
        assertTrue(expert.readyToLaunch(third), "l'univers 3 se lance dès l'univers 2 formé");
    }

    @Test
    void lUniversQuatreAttendLePalierDeLaGraineEtLArrosoirLyMene() {
        Game game = universes(3, false);
        RobotCosmosPlanner bot = new RobotCosmosPlanner(game, 20);
        bot.targetUniverses = 5;
        assertFalse(bot.universeProgress(), "trois univers sur cinq");
        assertFalse(bot.launchedNextUniverse, "la graine n'a pas son palier 4 : l'univers 4 attend");
        assertTrue(game.isWateringCanUnlocked() && game.isWatering(), "l'arrosoir est débloqué et penché");
        // Le palier 4 (version 0.179) : 990 arrosages, plus celui qui fait germer, à un par seconde.
        game.tick(600);
        bot.universeProgress();
        assertFalse(bot.launchedNextUniverse);
        game.tick(400);
        assertEquals(4, game.seedStage());
        bot.universeProgress();
        assertTrue(bot.launchedNextUniverse, "le palier 4 ouvre l'univers 4");
        assertTrue(game.isReplaying());
        // Dans la partie de l'univers 4, il garde l'arrosoir penché : il vise l'arbre-monde (version 0.179), et la graine
        // boit même au plafond de deux paliers d'avance (les arrosages comptent pour la suite).
        bot.tree.play(game);
        assertTrue(game.isWatering());
        game.state().setSeedWaterings(Game.seedStageWaterings(Game.SEED_STAGES));
        assertEquals(5, game.seedStage(), "trois univers formés : le palier 5 au plus");
        bot.tree.play(game);
        assertTrue(game.isWatering(), "il arrose encore : l'arbre-monde n'est pas là");
        while (game.universes() + Game.SEED_STAGES_AHEAD < Game.SEED_STAGES) game.testAddUniverse();
        bot.tree.play(game);
        assertFalse(game.isWatering(), "l'arbre-monde atteint, l'arrosoir est repris");
    }

    @Test
    void lExpertAcheteSesBullesDansLOrdreEtEconomisePourLaSuivante() {
        Game game = universes(3, true);
        worldTree(game);
        // Version 0.166 : les prix suivent l'énergie gardée ; le test se règle sur eux.
        game.state().noteVoidHistory("3", TreeBubble.SEED.cost() + TreeBubble.LEAVES_RIGHT.cost() + TreeBubble.LEAVES_LEFT.costAt(0)
                + TreeBubble.CROWN_LEFT.cost() + 500);
        UniverseTreePolicy expert = new UniverseTreePolicy(UniverseTreePolicy.Style.EXPERT, new NoRandom());
        expert.play(game);
        // Version 0.174 : la base, la bulle sans délai, ×2 sur le vide, puis l'arbuste à gauche (le lien vers l'arbre mûr).
        assertTrue(game.isTreeBubbleBought(TreeBubble.SEED));
        assertTrue(game.isTreeBubbleBought(TreeBubble.LEAVES_RIGHT));
        assertEquals(1, game.treeBubbleLevel(TreeBubble.LEAVES_LEFT));
        assertTrue(game.isTreeBubbleBought(TreeBubble.CROWN_LEFT));
        assertFalse(game.isTreeBubbleBought(TreeBubble.CROWN_RIGHT), "500 : pas assez");
        assertFalse(game.isTreeBubbleBought(TreeBubble.ANCIENT_RIGHT), "il garde pour la suivante de sa liste, pas pour une autre");
        assertEquals(500, game.voidAvailable(), 1e-9);
        assertEquals(TreeBubble.CROWN_RIGHT, UniverseTreePolicy.nextBest(game).bubble());
        game.state().noteVoidHistory("4", TreeBubble.CROWN_RIGHT.cost() + TreeBubble.MATURE_LEFT.cost() + TreeBubble.MATURE_RIGHT.cost() - 500);
        expert.play(game);
        // L'arbuste à droite, puis l'arbre mûr à gauche et à droite.
        assertTrue(game.isTreeBubbleBought(TreeBubble.CROWN_RIGHT));
        assertTrue(game.isTreeBubbleBought(TreeBubble.MATURE_LEFT));
        assertTrue(game.isTreeBubbleBought(TreeBubble.MATURE_RIGHT));
        assertEquals(7, expert.bubbles);
    }

    @Test
    void lOrdreDeLExpertMetEnDernierCeQuiNeChangeRien() {
        List<TreeBubble> order = new java.util.ArrayList<>();
        for (UniverseTreePolicy.Step step : UniverseTreePolicy.BEST_ORDER) order.add(step.bubble());
        int youngRight = order.indexOf(TreeBubble.YOUNG_RIGHT);
        assertTrue(youngRight > order.indexOf(TreeBubble.SUMMIT) && youngRight > order.lastIndexOf(TreeBubble.CROWN_RIGHT));
        assertTrue(order.indexOf(TreeBubble.GIANT_LEFT_HIGH) > youngRight);
        assertTrue(order.indexOf(TreeBubble.MATURE_LEFT) < order.indexOf(TreeBubble.CROWN_MIDDLE), "l'arbre mûr à gauche tôt");
        // Les mêmes bulles et niveaux que l'ordre d'avant, gardé pour les mesures.
        assertEquals(UniverseTreePolicy.ORDER_0173.size(), UniverseTreePolicy.BEST_ORDER.size());
        assertTrue(UniverseTreePolicy.BEST_ORDER.containsAll(UniverseTreePolicy.ORDER_0173));
    }

    @Test
    void lesBullesNeNaissentQuAvecLeurPalierDeGraine() {
        Game game = universes(3, true);
        game.state().noteVoidHistory("3", 1e6);
        UniverseTreePolicy expert = new UniverseTreePolicy(UniverseTreePolicy.Style.EXPERT, new NoRandom());
        // La graine au palier 5 : la base (palier 4) et le bonsaï (palier 5) seulement.
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(5));
        expert.buyBubbles(game);
        for (TreeBubble bubble : TreeBubble.values()) {
            // Version 0.167 : la bulle sans délai s'achète aussi.
            assertEquals(bubble.stage() <= 5, game.isTreeBubbleBought(bubble), bubble.name());
        }
        assertEquals(5, game.treeBubbleLevel(TreeBubble.LEAVES_LEFT), "le bonsaï à gauche, ses cinq niveaux");
    }

    @Test
    void leJoueurMoinsAppliquePrendLaBulleLaMoinsChere() {
        Game game = universes(3, true);
        worldTree(game);
        own(game, TreeBubble.SEED, TreeBubble.LEAVES_LEFT, TreeBubble.LEAVES_RIGHT);
        double cheapest = Math.min(TreeBubble.LEAVES_LEFT.costAt(1), TreeBubble.CROWN_LEFT.cost());
        game.state().noteVoidHistory("3", cheapest);
        UniverseTreePolicy policy = new UniverseTreePolicy(new UniverseTreePolicy.Style(1, UniverseTreePolicy.Plan.CHEAPEST), new NoRandom());
        policy.buyBubbles(game);
        // Ouvertes : ×2 sur le vide (niveau 2) et les trois bulles de l'arbuste : la moins chère, une seule.
        assertEquals(1, policy.bubbles);
        assertEquals(0, game.voidAvailable(), 1e-9);
        assertTrue(UniverseTreePolicy.cheapestBubble(game) == null);
        assertFalse(game.isTreeBubbleBought(TreeBubble.ROOTS));
    }

    @Test
    void lOngletDesHadronsSOuvreEtLesQuarksEnTropSeLient() {
        Game game = universes(3, true);
        worldTree(game);
        own(game, TreeBubble.SEED, TreeBubble.LEAVES_LEFT, TreeBubble.CROWN_LEFT, TreeBubble.CROWN_MIDDLE);
        assertTrue(game.isColliderUnlocked() && game.isHadronsOffered());
        UniverseTreePolicy expert = new UniverseTreePolicy(UniverseTreePolicy.Style.EXPERT, new NoRandom());
        assertFalse(expert.savingForHadrons(game), "0.174 : pas pour un prix loin de la production");
        game.state().setColliderEnergy(game.hadronsPrice());
        assertTrue(expert.savingForHadrons(game), "le collisionneur se garde pour l'onglet");
        game.state().setColliderEnergy(2e9);
        game.state().setQuarks(Quark.UP, 3_000);
        game.state().setQuarks(Quark.DOWN, 3_000);
        expert.hadrons(game);
        assertTrue(game.isHadronsUnlocked(), "ouvert contre au moins 1e9 d'énergie");
        assertEquals(1, expert.hadronsOpened);
        assertFalse(expert.savingForHadrons(game));
        assertTrue(game.hadrons(Hadron.PROTON) > 0 && game.hadrons(Hadron.NEUTRON) > 0, "les deux nucléons");
        assertTrue(Math.abs(game.hadrons(Hadron.PROTON) - game.hadrons(Hadron.NEUTRON)) <= game.hadrons(Hadron.PROTON),
                "au même ordre de grandeur");
        Map<Quark, Integer> keep = UniverseTreePolicy.reserve(game);
        assertTrue(keep.getOrDefault(Quark.UP, 0) > 0, "la chaîne du collisionneur demande des quarks haut");
        for (Quark quark : new Quark[] {Quark.UP, Quark.DOWN}) {
            assertTrue(game.state().quarks(quark) >= keep.getOrDefault(quark, 0), "la réserve du collisionneur reste : " + quark);
        }
        assertTrue(game.gluons() >= 1_000, "assez de gluons pour le premier palier : " + game.gluons());
        assertTrue(game.isGluonMilestoneReached(idle.core.collider.GluonMilestone.UPGRADES));
        assertEquals(0, game.hadrons(Hadron.PION), "les exotiques attendent le cœur de l'arbre-monde");
    }

    @Test
    void lesAutomatismesDesDimensionsSAchetentAvecLaBulleDuSommet() {
        Game game = universes(3, true);
        worldTree(game);
        game.testAddUniverseSeeds(BigNum.of(20));
        UniverseTreePolicy expert = new UniverseTreePolicy(UniverseTreePolicy.Style.EXPERT, new NoRandom());
        expert.line(game);
        assertEquals(0, game.dimensionAutomationsOwned(), "pas sans la bulle du sommet");
        own(game, TreeBubble.SUMMIT);
        double seeds = game.state().universeSeeds().toDouble();
        expert.line(game);
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            assertEquals(automation.dimension() == 1, game.ownsDimensionAutomation(automation), automation.name());
        }
        assertEquals(seeds - 9, game.state().universeSeeds().toDouble(), 1e-9, "1 + 1 + 2 + 2 + 3 graines");
        assertEquals(5, expert.automations);
        // Avec les segments du premier automatisme du plan, la visée automatique suit.
        game.state().setDimensionShards(DimensionAutomation.PLANE_AIM.cost());
        expert.line(game);
        assertTrue(game.ownsDimensionAutomation(DimensionAutomation.PLANE_AIM));
    }

    @Test
    void laLigneSAcheteAuMeilleurRapport() {
        Game game = universes(3, true);
        game.state().setDimensionShards(100);
        UniverseTreePolicy expert = new UniverseTreePolicy(UniverseTreePolicy.Style.EXPERT, new NoRandom());
        expert.line(game);
        assertTrue(expert.lineBought > 0);
        assertTrue(game.state().dimensionShards() < 100);
    }

    @Test
    void laBulleDesRacinesAttendLOnziemeUnivers() {
        Game ten = universes(10, true), eleven = universes(11, false);
        assertTrue(UniverseTreePolicy.skipped(ten, TreeBubble.ROOTS), "avant l'univers 11 formé, pas de défi final");
        assertFalse(UniverseTreePolicy.skipped(eleven, TreeBubble.ROOTS), "après, le bot la prend (version 0.163)");
        assertFalse(UniverseTreePolicy.skipped(eleven, TreeBubble.LEAVES_RIGHT), "la bulle sans délai s'achète (version 0.167)");
        assertFalse(UniverseTreePolicy.skipped(ten, TreeBubble.LEAVES_RIGHT));
    }

    @Test
    void lOrdreDeLExpertFiniIlPrendLaBulleQuiReste() {
        Game game = universes(11, false);
        worldTree(game);
        for (TreeBubble bubble : TreeBubble.values()) {
            if (bubble != TreeBubble.ROOTS) game.state().setTreeBubbleLevel(bubble.ordinal(), bubble.maxLevel());
        }
        game.state().setSeedUpgradeLevel(Game.SeedUpgrade.ROOT_BUBBLE.ordinal(), 1);
        game.state().noteVoidHistory("11", 1e6);
        assertTrue(UniverseTreePolicy.allBestDone(game));
        assertTrue(game.isTreeBubbleOpen(TreeBubble.ROOTS));
        UniverseTreePolicy expert = new UniverseTreePolicy(UniverseTreePolicy.Style.EXPERT, new NoRandom());
        expert.buyBubbles(game);
        assertTrue(game.isTreeBubbleBought(TreeBubble.ROOTS), "la bulle des racines, hors de la liste de l'expert");
        assertTrue(game.isFinalChallengeOpen());
    }
}

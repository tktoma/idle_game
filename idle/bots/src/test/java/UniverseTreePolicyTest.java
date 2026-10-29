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
        game.state().setSeedWaterings(Game.seedStageWaterings(Game.SEED_STAGES));
        assertEquals(Game.SEED_STAGES, game.seedStage());
    }

    private static void own(Game game, TreeBubble... bubbles) {
        for (TreeBubble bubble : bubbles) game.state().setTreeBubbleLevel(bubble.ordinal(), 1);
    }

    @Test
    void rienAvantLUniversQuatre() {
        UniverseTreePolicy policy = new UniverseTreePolicy(new UniverseTreePolicy.Style(0.5, UniverseTreePolicy.Plan.CHEAPEST), new NoRandom());
        for (int formed = 0; formed < UniverseTreePolicy.FROM_UNIVERSES; formed++) {
            Game game = formed == 0 ? Bots.fresh(42) : universes(formed, true);
            game.state().noteVoidHistory("1", 1e6);
            assertEquals(0, policy.play(game), formed + " univers formés : aucun geste");
            assertTrue(policy.readyToLaunch(game), "les univers 2 et 3 se lancent sans attendre la graine, comme avant");
            assertFalse(policy.savingForHadrons(game));
            assertFalse(game.isWateringCanUnlocked());
            assertEquals(0, game.treeBubblesShown().stream().filter(game::isTreeBubbleBought).count());
        }
        Game third = universes(2, false);
        assertTrue(policy.readyToLaunch(third), "l'univers 3 se lance dès l'univers 2 formé");
        assertFalse(third.isWatering());
    }

    @Test
    void lUniversQuatreAttendLePalierDeLaGraineEtLArrosoirLyMene() {
        Game game = universes(3, false);
        RobotCosmosPlanner bot = new RobotCosmosPlanner(game, 20);
        bot.targetUniverses = 5;
        assertFalse(bot.universeProgress(), "trois univers sur cinq");
        assertFalse(bot.launchedNextUniverse, "la graine n'a pas son palier 4 : l'univers 4 attend");
        assertTrue(game.isWateringCanUnlocked() && game.isWatering(), "l'arrosoir est débloqué et penché");
        // Le palier 4 : 90 arrosages, plus celui qui fait germer, à un par seconde.
        game.tick(60);
        bot.universeProgress();
        assertFalse(bot.launchedNextUniverse);
        game.tick(40);
        assertEquals(4, game.seedStage());
        bot.universeProgress();
        assertTrue(bot.launchedNextUniverse, "le palier 4 ouvre l'univers 4");
        assertTrue(game.isReplaying());
        // Dans la partie de l'univers 4, il vise le palier 5, celui de l'univers suivant, et pas au-delà.
        bot.tree.play(game);
        assertTrue(game.isWatering());
        game.tick(40);
        assertEquals(5, game.seedStage());
        bot.tree.play(game);
        assertFalse(game.isWatering(), "le palier 5 atteint, l'arrosoir est repris");
    }

    @Test
    void lExpertAcheteSesBullesDansLOrdreEtEconomisePourLaSuivante() {
        Game game = universes(3, true);
        worldTree(game);
        game.state().noteVoidHistory("3", 7_500);
        UniverseTreePolicy expert = new UniverseTreePolicy(UniverseTreePolicy.Style.EXPERT, new NoRandom());
        expert.play(game);
        // La base 1 000, ×2 sur le vide 2 000 puis ×4 4 000 ; l'arbuste au milieu (4 000) attend.
        assertTrue(game.isTreeBubbleBought(TreeBubble.SEED));
        assertEquals(2, game.treeBubbleLevel(TreeBubble.LEAVES_LEFT));
        assertFalse(game.isTreeBubbleBought(TreeBubble.LEAVES_RIGHT), "la bulle sans délai n'est pas achetée");
        assertFalse(game.isTreeBubbleBought(TreeBubble.CROWN_MIDDLE));
        assertFalse(game.isTreeBubbleBought(TreeBubble.CROWN_LEFT), "il garde pour la suivante de sa liste, pas pour une autre");
        assertEquals(500, game.voidAvailable(), 1e-9);
        assertEquals(TreeBubble.CROWN_MIDDLE, UniverseTreePolicy.nextBest(game).bubble());
        game.state().noteVoidHistory("4", 9_500);
        expert.play(game);
        // L'arbuste au milieu 4 000, le jeune arbre à droite 6 000 : il rouvre l'arbuste à droite, qui passe devant.
        assertTrue(game.isTreeBubbleBought(TreeBubble.CROWN_MIDDLE));
        assertTrue(game.isTreeBubbleBought(TreeBubble.YOUNG_RIGHT));
        assertEquals(TreeBubble.CROWN_RIGHT, UniverseTreePolicy.nextBest(game).bubble());
        assertEquals(5, expert.bubbles);
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
            assertEquals(bubble.stage() <= 5 && bubble != TreeBubble.LEAVES_RIGHT, game.isTreeBubbleBought(bubble), bubble.name());
        }
        assertEquals(5, game.treeBubbleLevel(TreeBubble.LEAVES_LEFT), "le bonsaï à gauche, ses cinq niveaux");
    }

    @Test
    void leJoueurMoinsAppliquePrendLaBulleLaMoinsChere() {
        Game game = universes(3, true);
        worldTree(game);
        own(game, TreeBubble.SEED, TreeBubble.LEAVES_LEFT, TreeBubble.LEAVES_RIGHT);
        game.state().noteVoidHistory("3", 4_000);
        UniverseTreePolicy policy = new UniverseTreePolicy(new UniverseTreePolicy.Style(1, UniverseTreePolicy.Plan.CHEAPEST), new NoRandom());
        policy.buyBubbles(game);
        // Ouvertes : ×2 sur le vide (niveau 2, 4 000) et les trois bulles de l'arbuste (4 000) : la première trouvée.
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
}

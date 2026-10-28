package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.TreeBubble;
import idle.core.bigbang.BigBangMilestone;
import idle.core.collider.ColliderGenerator;
import idle.core.math.BigNum;
import idle.core.save.SaveCodec;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.148 : les effets des bulles de l'arbre. Chaque bulle achetée change une règle, pour tous les univers
 * suivants ; deux se rachètent cinq fois. Celle du défi final s'achète déjà, son effet est à venir ; celles des hadrons
 * (version 0.149) et de l'automatisation des dimensions (version 0.150) ont leur test, HadronsTest et
 * AutomatisationDimensionsTest.
 */
class BullesEffetsTest {

    /** L'univers formé, la graine à ce palier, de l'énergie du vide accumulée. */
    private static Game at(int stage, double accumulated) {
        Game game = GameTestSupport.realGame();
        game.testFormUniverse();
        game.state().setSeedSprouted(true);
        game.state().setSeedWaterings(Game.seedStageWaterings(stage));
        if (accumulated > 0) game.state().noteVoidHistory("3", accumulated);
        return game;
    }

    /** Le premier univers formé, puis un univers neuf lancé, avec ces bulles posées. */
    private static Game nextUniverseWith(TreeBubble... bubbles) {
        Game game = GameTestSupport.realGame();
        game.testFormUniverse();
        for (TreeBubble bubble : bubbles) game.state().setTreeBubbleBought(bubble.ordinal(), true);
        return game;
    }

    private static void give(Game game, TreeBubble bubble) {
        game.state().setTreeBubbleBought(bubble.ordinal(), true);
    }

    @Test
    void deuxBullesSeRachetentCinqFoisEtCoutentDeuxFoisPlusAChaqueNiveau() throws Exception {
        assertEquals(5, TreeBubble.LEAVES_LEFT.maxLevel());
        assertEquals(5, TreeBubble.GIANT_LEFT_HIGH.maxLevel());
        for (TreeBubble bubble : TreeBubble.values()) {
            if (bubble != TreeBubble.LEAVES_LEFT && bubble != TreeBubble.GIANT_LEFT_HIGH) assertEquals(1, bubble.maxLevel(), bubble.toString());
            assertFalse(bubble.effect().isBlank(), bubble + " dit ce qu'elle fait");
        }
        Game game = at(5, 1e9);
        assertTrue(game.buyTreeBubble(TreeBubble.SEED));
        assertFalse(game.buyTreeBubble(TreeBubble.SEED), "la base ne s'achète qu'une fois");
        double spent = game.state().voidSpent();
        for (int level = 1; level <= 5; level++) {
            assertEquals(2_000 * Math.pow(2, level - 1), game.treeBubbleCost(TreeBubble.LEAVES_LEFT), 1e-9, "niveau " + level);
            assertTrue(game.buyTreeBubble(TreeBubble.LEAVES_LEFT), "niveau " + level);
            assertEquals(level, game.treeBubbleLevel(TreeBubble.LEAVES_LEFT));
        }
        assertTrue(game.isTreeBubbleMaxed(TreeBubble.LEAVES_LEFT));
        assertFalse(game.canBuyTreeBubble(TreeBubble.LEAVES_LEFT), "pas de sixième niveau");
        assertEquals(spent + 2_000 * 31, game.state().voidSpent(), 1e-6, "2 000 + 4 000 + … + 32 000");
        // Le niveau se sauvegarde ; une sauvegarde d'avant (le champ de bits seul) relit chaque bulle au niveau 1.
        GameState back = GameTestSupport.reread(game.state());
        assertEquals(5, back.treeBubbleLevel(TreeBubble.LEAVES_LEFT.ordinal()));
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(game.state(), 0).split("\n")) {
            if (!line.startsWith("treeBubbleLevel") && !line.startsWith("voidTrickled")) old.append(line).append('\n');
        }
        GameState older = SaveCodec.read(old.toString()).state();
        assertEquals(1, older.treeBubbleLevel(TreeBubble.LEAVES_LEFT.ordinal()));
        assertEquals(1, older.treeBubbleLevel(TreeBubble.SEED.ordinal()));
    }

    @Test
    void lesBullesDontLEffetEstAVenirLeDisent() {
        // Version 0.150 : les hadrons et l'automatisation des dimensions sont là ; reste le défi final.
        List<TreeBubble> soon = List.of(TreeBubble.ROOTS);
        for (TreeBubble bubble : TreeBubble.values()) assertEquals(soon.contains(bubble), bubble.isComingSoon(), bubble.toString());
    }

    @Test
    void laGraineDonneUnePlaceDeMaturationParDimension() {
        Game game = GameTestSupport.universe(3);
        int before = game.maturationPlaces();
        give(game, TreeBubble.SEED);
        assertEquals(before + 3, game.maturationPlaces(), "trois dimensions ouvertes");
    }

    @Test
    void leBonsaiDeGaucheDoubleLeGainDuVideParNiveau() {
        Game game = GameTestSupport.universe(3);
        double before = game.voidPairGain();
        game.state().setTreeBubbleLevel(TreeBubble.LEAVES_LEFT.ordinal(), 3);
        assertEquals(before * 8, game.voidPairGain(), 1e-9);
        game.state().setTreeBubbleLevel(TreeBubble.LEAVES_LEFT.ordinal(), 9);
        assertEquals(before * 32, game.voidPairGain(), 1e-9, "cinq niveaux au plus, même d'une sauvegarde trafiquée");
    }

    @Test
    void leBonsaiDeDroiteRendLesAutomatismesInstantanes() {
        Game game = GameTestSupport.realGame();
        game.state().addAutomation("auto_speed");
        game.state().setAutomationEnabled("auto_speed", true);
        game.state().setParticles(BigNum.of(1e12));
        Game slow = GameTestSupport.realGame();
        slow.state().addAutomation("auto_speed");
        slow.state().setAutomationEnabled("auto_speed", true);
        slow.state().setParticles(BigNum.of(1e12));
        give(game, TreeBubble.LEAVES_RIGHT);
        assertTrue(game.isAutomationInstant());
        assertFalse(slow.isAutomationInstant());
        game.tick(0.05);
        slow.tick(0.05);
        assertTrue(game.levelOf("speed") > 20, "tout ce que les particules permettent : " + game.levelOf("speed"));
        assertTrue(slow.levelOf("speed") <= 1, "à sa cadence : " + slow.levelOf("speed"));
    }

    @Test
    void lArbusteDeGaucheOuvreLeCollisionneurDesLeDebut() {
        Game plain = nextUniverseWith();
        assertTrue(plain.startNextUniverse());
        assertFalse(plain.isColliderUnlocked(), "sans la bulle, il attend la première explosion");
        Game game = nextUniverseWith(TreeBubble.CROWN_LEFT);
        assertTrue(game.startNextUniverse());
        assertTrue(game.isColliderUnlocked());
        assertEquals(0, game.state().colliderGeneratorsBought(ColliderGenerator.BEAM), "ouvert, mais repris de zéro");
    }

    @Test
    void lArbreMurDeGaucheGardeLeCollisionneur() {
        Game game = nextUniverseWith(TreeBubble.MATURE_LEFT);
        game.state().setColliderEnergy(12_345);
        game.state().setColliderGenerators(ColliderGenerator.BEAM, 7);
        assertTrue(game.startNextUniverse());
        assertTrue(game.isColliderUnlocked());
        assertEquals(12_345, game.state().colliderEnergy(), 1e-9);
        assertEquals(7, game.state().colliderGenerators(ColliderGenerator.BEAM), 1e-9);
        assertTrue(game.universeTransition(true).kept().stream().anyMatch(line -> line.startsWith("Le collisionneur")));
    }

    @Test
    void lArbusteDeDroiteDonneLaPersistanceEtDesAtomesSansFusion() {
        Game game = GameTestSupport.realGame();
        give(game, TreeBubble.CROWN_RIGHT);
        assertEquals(0, game.levelOf("atom_keep"));
        game.tick(1);
        assertEquals(1, game.levelOf("atom_keep"), "Persistance offerte");
        assertTrue(game.keepsUpgradesOnFusion());
        assertTrue(game.bubbleAtomsPerSecond().sign() > 0, "un générateur suffit pour une part");
        BigNum before = game.state().atoms();
        game.tick(60);
        assertTrue(game.state().atoms().gt(before), "des atomes sans fusionner");
        assertEquals(0, game.stats().fusions());
        // La part : le premier générateur seul, un dixième des générateurs d'une fusion.
        BigNum expected = game.atomsPerFusion().multiply((double) game.generatorCount() / game.generatorsPerAtom() / Game.BUBBLE_FUSION_SECONDS);
        assertEquals(expected.toDouble(), game.bubbleAtomsPerSecond().toDouble(), expected.toDouble() * 1e-9);
    }

    @Test
    void leJeuneArbreDeDroiteLeveLePlafondDesGenerateurs() {
        Game game = GameTestSupport.realGame();
        int cap = game.maxGeneratorCount();
        give(game, TreeBubble.YOUNG_RIGHT);
        assertEquals(Game.BUBBLE_GENERATOR_LIMIT, game.maxGeneratorCount());
        assertEquals(cap, game.generatorCap(), "l'ancien plafond reste le repère du prix");
        // Jusqu'à l'ancien plafond, les prix ne bougent pas ; au-delà, ils grimpent de 10^(0,01 n²).
        Game plain = GameTestSupport.realGame();
        game.state().setLevel("generator", cap - 2);
        plain.state().setLevel("generator", cap - 2);
        assertEquals(plain.costOf("generator").log10(), game.costOf("generator").log10(), 1e-9, "le dernier sous l'ancien plafond");
        game.state().setLevel("generator", cap - 1 + 9);
        double raw = game.upgrade("generator").costAt(cap - 1 + 9).log10();
        assertTrue(game.costOf("generator").log10() > raw + 0.9, "le dixième au-delà coûte dix fois plus : " + game.costOf("generator"));
    }

    @Test
    void lArbreMurDeDroiteGardeLaMatiereNoireEtSesDefis() {
        Game game = nextUniverseWith(TreeBubble.MATURE_RIGHT);
        game.state().setDarkMatter(BigNum.of(777));
        game.state().setExplosions(4);
        game.state().setDarkLevel("dark_density", 3);
        game.state().addCompletedChallenge("challenge_rust");
        assertTrue(game.startNextUniverse());
        assertEquals(777, game.state().darkMatter().toDouble(), 1e-9);
        assertEquals(4, game.state().explosions());
        assertEquals(3, game.state().darkLevelOf("dark_density"));
        assertTrue(game.state().completedChallenges().contains("challenge_rust"));
        assertTrue(game.isDarkMatterUnlocked());
        // Sans la bulle, tout repart.
        Game plain = nextUniverseWith();
        plain.state().setDarkMatter(BigNum.of(777));
        assertTrue(plain.startNextUniverse());
        assertEquals(0, plain.state().darkMatter().toDouble(), 0);
    }

    @Test
    void lArbreAncienDeDroiteSupprimeLaMaturation() {
        Game game = GameTestSupport.realGame();
        game.state().addBody("rocky_rubble");
        game.state().setMaturing("rocky_rubble", 500);
        assertTrue(game.maturationSeconds(game.body("rocky_rubble")) > 0);
        give(game, TreeBubble.ANCIENT_RIGHT);
        game.bubbleBought(TreeBubble.ANCIENT_RIGHT);
        assertEquals(0, game.maturationSeconds(game.body("rocky_rubble")), 0);
        assertTrue(game.isBodyMature("rocky_rubble"), "celui qui mûrissait est mûr");
        assertTrue(game.maturingBodies().isEmpty());
    }

    @Test
    void leGeantToutAGaucheVerseUnePartDuVideAuCumul() {
        // Le vide qui produit, comme dans les statistiques de l'univers (StatsUniversePageTest).
        Game game = GameTestSupport.universe(3);
        game.state().setBigBangs(1);
        game.state().setColliderEnergy(game.voidPrice() + 1e6);
        assertTrue(game.openVoid());
        game.state().setSpace(BigNum.of(1e6));
        game.state().setColliderGenerators(ColliderGenerator.BEAM, 7);
        game.state().setTreeBubbleLevel(TreeBubble.GIANT_LEFT_HIGH.ordinal(), 2);
        assertEquals(0.10, game.voidTrickleShare(), 1e-12);
        double before = game.voidAccumulated();
        game.tick(5);
        double gain = game.voidGainPerSecond();
        assertTrue(gain > 0, "le vide produit");
        assertTrue(game.voidAccumulated() > before, "le cumul monte pendant qu'il produit");
        GameState back = GameTestSupport.reread(game.state());
        assertEquals(game.state().voidTrickled(), back.voidTrickled(), 1e-9);
    }

    @Test
    void leGeantDeGaucheEnBasGardeLeVideOuvert() {
        Game game = nextUniverseWith();
        assertFalse(game.isVoidUnlocked());
        give(game, TreeBubble.GIANT_LEFT_LOW);
        game.bubbleBought(TreeBubble.GIANT_LEFT_LOW);
        assertTrue(game.isVoidUnlocked() && game.state().voidOpen(), "ouvert sans le payer");
        game.state().setVoidLevel(Game.VoidUpgrade.RATE.ordinal(), 3);
        game.state().setVoidEnergy(BigNum.of(5_000));
        assertTrue(game.startNextUniverse());
        assertTrue(game.isVoidUnlocked());
        assertEquals(3, game.voidLevel(Game.VoidUpgrade.RATE));
        assertEquals(5_000, game.voidEnergy().toDouble(), 1e-9);
        assertFalse(game.isVoidOffered(), "déjà ouvert : il ne se propose plus");
    }

    @Test
    void leGeantToutADroiteDonneDeLaMatiereNoireSansExploser() {
        Game game = GameTestSupport.realGame();
        game.state().setAtoms(BigNum.of(1e6));
        assertEquals(0, game.bubbleDarkMatterPerSecond().toDouble(), 0);
        give(game, TreeBubble.GIANT_RIGHT_HIGH);
        BigNum rate = game.bubbleDarkMatterPerSecond();
        assertEquals(game.darkMatterPerExplosion().toDouble() * Math.log10(1 + 1e6) / Game.BUBBLE_EXPLOSION_SECONDS, rate.toDouble(), 1e-9);
        assertFalse(game.isDarkMatterUnlocked());
        game.tick(10);
        assertTrue(game.state().darkMatter().sign() > 0);
        assertEquals(0, game.state().explosions());
        assertTrue(game.isDarkMatterUnlocked(), "la matière noire existe sans explosion");
    }

    @Test
    void leGeantDeDroiteEnBasOuvreTousLesPaliersEtGardeLesDefisDuBigBang() {
        Game game = nextUniverseWith(TreeBubble.GIANT_RIGHT_LOW);
        game.state().addCompletedBangChallenge("VOID");
        assertTrue(game.startNextUniverse());
        assertEquals(0, game.bigBangs());
        for (BigBangMilestone milestone : game.bigBangMilestones()) {
            assertTrue(game.isBigBangMilestoneReached(milestone), milestone.name() + " : ouvert sans Big Bang ni prix");
        }
        assertTrue(game.isProbesUnlocked() && game.isExoticUnlocked());
        assertTrue(game.state().completedBangChallenges().contains("VOID"));
        Game plain = nextUniverseWith();
        plain.state().addCompletedBangChallenge("VOID");
        assertTrue(plain.startNextUniverse());
        assertFalse(plain.state().completedBangChallenges().contains("VOID"));
        assertFalse(plain.isProbesUnlocked());
    }
}

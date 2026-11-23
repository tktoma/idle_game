package idle.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.ui.TestTools.Section;
import idle.ui.TestTools.Tool;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Les outils du profil de test, sans affichage : le catalogue ({@link TestTools}) a une section par onglet et chacun de
 * ses outils se lance sans casser la partie ; les actions ({@link TestActions}) ouvrent, remplissent et remettent à zéro
 * ce qu'elles disent, paliers secondaires et dimensions compris.
 */
class OutilsDeTestTest {

    private static Game partie() {
        Game game = new Game(1L);
        game.start();
        return game;
    }

    private static List<Tool> outils(Game game, TestTools.Hooks hooks, List<String> notes) {
        TestActions actions = new TestActions(game);
        return TestTools.build(game, actions, null, hooks, notes::add);
    }

    @Test
    void chaqueOngletDuJeuASaSectionEtChaqueSectionSesOutils() {
        for (GameAppBase.Tab tab : GameAppBase.Tab.values()) {
            boolean expected = !List.of("PROFILE", "STATS", "SETTINGS").contains(tab.name());
            assertEquals(expected, Section.ofTab(tab.name()) != null, tab + " : une section le suit");
        }
        assertNull(Section.ofTab(null));
        List<Tool> tools = outils(partie(), new TestTools.Hooks() { }, new ArrayList<>());
        for (Section section : Section.values()) {
            assertTrue(tools.stream().anyMatch(tool -> tool.section() == section), section + " : au moins un outil");
            Set<String> labels = new HashSet<>();
            for (Tool tool : tools) if (tool.section() == section) assertTrue(labels.add(tool.label()), "deux fois « " + tool.label() + " »");
        }
        assertTrue(tools.size() > 200, "presque tout se teste : " + tools.size() + " outils");
    }

    @Test
    void chaqueOutilSeLanceSansCasserLaPartie() {
        // Sur une partie neuve chacun, puis tous à la suite sur la même : rien ne lève d'exception.
        List<String> notes = new ArrayList<>();
        for (Tool tool : outils(partie(), new TestTools.Hooks() { }, notes)) {
            if (!tool.section().headless()) continue;
            Game game = partie();
            Tool fresh = outils(game, new TestTools.Hooks() { }, notes).stream()
                    .filter(each -> each.section() == tool.section() && each.label().equals(tool.label())).findFirst().orElseThrow();
            assertDoesNotThrow(fresh.action()::run, "sur une partie neuve : " + tool.label());
            assertDoesNotThrow(() -> game.tick(1), "le jeu repart après " + tool.label());
        }
        Game game = partie();
        for (Tool tool : outils(game, new TestTools.Hooks() { }, notes)) {
            if (!tool.section().headless() || tool.label().startsWith("Nouvelle partie")) continue;
            assertDoesNotThrow(tool.action()::run, "à la suite : " + tool.label());
            assertDoesNotThrow(() -> game.tick(1), "le jeu repart après " + tool.label());
        }
    }

    @Test
    void lesSonsEtLesAnimationsPassentParLApplicationSansToucherLaPartie() {
        Game game = partie();
        List<String> heard = new ArrayList<>();
        Set<TestTools.TestHum> forced = EnumSet.noneOf(TestTools.TestHum.class);
        TestTools.Hooks hooks = new TestTools.Hooks() {
            @Override public void sound(Sound.Cue cue) { heard.add("son " + cue); }
            @Override public void particle(int stage) { heard.add("particule " + stage); }
            @Override public void gesture(int dimension) { heard.add("geste " + dimension); }
            @Override public void clicker(int stack) { heard.add("clicker " + stack); }
            @Override public void gain(int dimension) { heard.add("points " + dimension); }
            @Override public void piano(int note) { heard.add("piano " + note); }
            @Override public boolean toggleHum(TestTools.TestHum hum) {
                if (!forced.remove(hum)) forced.add(hum);
                return forced.contains(hum);
            }
            @Override public void explosion() { heard.add("explosion"); }
            @Override public void fusion() { heard.add("fusion"); }
            @Override public void newAtom() { heard.add("atome"); }
        };
        List<String> notes = new ArrayList<>();
        int explosions = game.state().explosions();
        for (Tool tool : outils(game, hooks, notes)) {
            if (tool.section() == Section.SOUNDS || tool.section() == Section.ANIMATIONS) tool.action().run();
        }
        for (Sound.Cue cue : Sound.Cue.values()) assertTrue(heard.contains("son " + cue), "chaque son s'écoute : " + cue);
        assertTrue(heard.contains("particule 13") && heard.contains("geste 10") && heard.contains("points 1"));
        assertTrue(heard.contains("piano 15") && heard.contains("piano " + SoundEvents.WRONG), "le piano du miroir, et la faute");
        assertTrue(heard.contains("clicker 1") && heard.contains("clicker 4"), "les accords du clicker, d'une à quatre zones");
        assertEquals(EnumSet.allOf(TestTools.TestHum.class), forced, "chaque fond se fait jouer partout");
        assertTrue(heard.containsAll(List.of("explosion", "fusion", "atome")));
        assertEquals(explosions, game.state().explosions(), "rejouer l'explosion n'explose pas");
        assertFalse(game.isUniverseFormed());
        assertTrue(notes.stream().anyMatch(note -> note.contains("Il faut un univers formé")), "ce qui ne peut pas se jouer le dit");
    }

    @Test
    void laRechercheIgnoreAccentsEtMajuscules() {
        Tool tool = new Tool(Section.COLLIDER, "Énergie ×1 000", () -> { });
        assertTrue(tool.matches("energie"));
        assertTrue(tool.matches("ÉNERG"));
        assertTrue(tool.matches("collisionneur"), "le nom de la section compte aussi");
        assertTrue(tool.matches("  "));
        assertFalse(tool.matches("graine"));
        assertEquals("Figé", TestTools.speedName(0));
        assertEquals("×0.25", TestTools.speedName(0.25));
        assertEquals("×1000", TestTools.speedName(1000));
    }

    @Test
    void lesDimensionsSOuvrentSeRemplissentEtRepartentDeZero() {
        Game game = partie();
        TestActions act = new TestActions(game);
        act.openDimension(4);
        assertEquals(4, game.dimensionsUnlocked(), "ouvrir la durée forme l'univers et en ajoute trois");
        act.addDimensionShards(2, Game.PLANE_THRESHOLD / 10);
        assertEquals(Game.PLANE_THRESHOLD / 10, game.state().planeShards(), 1e-9);
        assertEquals(Game.PLANE_THRESHOLD / 10, game.state().planeShardsEarned(), 1e-9);
        act.reachDimensionThreshold(3);
        assertTrue(game.isVolumeThresholdReached());
        assertEquals(0, game.state().volumeShards(), 1e-9, "le seuil n'ajoute rien à dépenser");
        act.dimensionLevels(1, true);
        for (Game.LineUpgrade u : Game.LineUpgrade.values()) assertEquals(game.lineMaxLevel(u), game.lineLevel(u));
        act.resetDimension(1);
        for (Game.LineUpgrade u : Game.LineUpgrade.values()) assertEquals(0, game.lineLevel(u));
        act.resetDimension(2);
        assertEquals(0, game.state().planeShards(), 1e-9);
        assertEquals(0, game.state().planeShardsEarned(), 1e-9);
        assertEquals(4, game.dimensionsUnlocked(), "remise à zéro : la dimension reste ouverte");
        act.openDimension(5);
        act.dimensionLevels(5, true);
        assertEquals(Game.MAX_RINGS, game.loopRings());
        assertEquals(10, game.loopLevel(Game.LoopUpgrade.GAIN), "le gain n'a pas de plafond : dix niveaux");
        act.resetAllDimensions();
        for (Game.LoopUpgrade u : Game.LoopUpgrade.values()) assertEquals(0, game.loopLevel(u));
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) assertEquals(0, act.dimensionEarned(d), 1e-9, "dimension " + d);
    }

    @Test
    void lesPaliersSecondairesRepartentDeZero() {
        Game game = partie();
        TestActions act = new TestActions(game);
        act.seedUpgrades(true);
        act.fullChrono();
        act.water();
        assertTrue(game.isWateringCanUnlocked());
        assertTrue(game.chronoStored() > 0);
        act.resetSeed();
        for (Game.SeedUpgrade u : Game.SeedUpgrade.values()) assertEquals(0, game.seedUpgradeLevel(u));
        assertEquals(0, game.chronoStored(), 1e-9);
        assertEquals(0, game.state().seedWaterings());
        assertFalse(game.isWateringCanUnlocked());
        act.openVoid();
        act.addVoidEnergy(idle.core.math.BigNum.of(1000));
        act.voidUpgrades(true);
        act.resetVoid();
        assertFalse(game.state().voidOpen());
        assertTrue(game.state().voidEnergy().isZero());
        for (Game.VoidUpgrade u : Game.VoidUpgrade.values()) assertEquals(0, game.state().voidLevel(u.ordinal()));
        act.openFinalChallenge();
        assertTrue(game.isFinalChallengeOpen());
        act.closeFinalChallenge();
        assertFalse(game.isFinalChallengeOpen());
        act.openDimension(3);
        act.resetUniverses();
        assertEquals(1, game.universes(), "la partie secondaire revient à un seul univers");
    }

    @Test
    void lesActesRepartentDeZero() {
        Game game = partie();
        TestActions act = new TestActions(game);
        act.addParticles(idle.core.math.BigNum.of(1e6));
        act.addAtoms(idle.core.math.BigNum.of(10));
        act.addDarkMatter(5);
        act.resetAct1();
        assertTrue(game.state().atoms().isZero(), "acte 1 : plus d'atomes");
        assertFalse(game.state().darkMatter().isZero(), "acte 1 : la matière noire reste");
        act.resetAct2();
        assertTrue(game.state().darkMatter().isZero(), "acte 2 : plus de matière noire");
        act.colliderEnergy();
        act.resetCollider();
        assertEquals(0, game.state().colliderEnergy(), 1e-9);
        assertNotNull(TestTools.cueName(Sound.Cue.NEW_ATOM));
    }
}

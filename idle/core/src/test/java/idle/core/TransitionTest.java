package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import idle.core.universe.UniverseTransition;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.127 : le guide de transition entre univers dit vrai. Chaque test lit le guide, fait la vraie remise à
 * zéro, puis vérifie que ce qu'il annonce perdu l'est, et que ce qu'il annonce gardé l'est.
 */
class TransitionTest {

    private static boolean mentions(List<String> lines, String words) {
        return lines.stream().anyMatch(line -> line.contains(words));
    }

    /** Une partie avancée, au seuil de l'univers. */
    private static Game advanced() {
        Game game = GameTestSupport.realGame();
        GameTestSupport.bangs(game, 8);
        game.state().setParticles(BigNum.of(1, 500));
        game.state().setAtoms(BigNum.of(1, 20));
        game.state().setDarkMatter(BigNum.of(500));
        game.state().setColliderEnergy(1e9);
        game.state().setTimePlayed(100_000);
        game.state().setRecord(GameStats.Step.GALAXY.name(), 90_000);
        game.achievements().stream().limit(12).forEach(achievement -> game.state().addAchievement(achievement.id()));
        game.state().setAutomationPaused(true);
        game.state().setChallengeTime("c1", 500);
        return game;
    }

    @Test
    void laFormationDeLUniversFaitCeQueDitLeGuide() {
        Game game = advanced();
        UniverseTransition guide = game.universeTransition(false);
        assertFalse(guide.launching());
        assertTrue(mentions(guide.lost(), "8 Big Bangs"));
        assertTrue(mentions(guide.lost(), "Matière noire"));
        assertTrue(mentions(guide.lost(), "collisionneur"));
        assertTrue(mentions(guide.lost(), "records des défis"));
        assertTrue(mentions(guide.kept(), "records des étapes"));
        assertTrue(mentions(guide.kept(), "Les succès (12)"));
        assertTrue(mentions(guide.gained(), "+1 graine d'univers"));
        assertTrue(mentions(guide.gained(), "L'onglet Univers s'ouvre"), "première formation : la partie secondaire est à venir");
        assertFalse(mentions(guide.kept(), "skins"), "pas de skins à garder avant le premier univers");
        int achievements = game.achievementCount();

        game.testFormUniverse();

        // Perdu.
        assertEquals(0, game.bigBangs());
        assertTrue(game.state().particles().isZero());
        assertTrue(game.state().darkMatter().isZero());
        assertEquals(0, game.state().colliderEnergy(), 1e-9);
        assertTrue(game.state().probes().isEmpty());
        assertTrue(game.state().challengeTimes().isEmpty(), "les records des défis repartent");
        // Gardé.
        assertTrue(game.achievementCount() >= achievements);
        assertEquals(100_000, game.state().timePlayed(), 1e-9);
        assertEquals(90_000, game.recordOf(GameStats.Step.GALAXY), 1e-9);
        assertTrue(game.state().automationPaused(), "les réglages de jeu restent");
        // Gagné.
        assertEquals(BigNum.of(Game.SEEDS_PER_UNIVERSE), game.state().universeSeeds());
        assertEquals(1, game.universes());
    }

    @Test
    void leLancementDeLUniversSuivantGardeLaPartieSecondaire() {
        Game game = GameTestSupport.realGame();
        game.testFormUniverse();
        // La partie secondaire : graines, énergie du vide, ligne, graine, skins, épreuves.
        game.state().setUniverseSeeds(BigNum.of(3));
        game.state().noteVoidHistory("1", 1250);
        game.state().setDimensionShards(42);
        game.state().setLineSpeedLevel(2);
        game.state().unlockSkin(idle.core.universe.Skin.values()[0].id());
        UniverseTransition guide = game.universeTransition(true);
        assertTrue(guide.launching());
        assertTrue(mentions(guide.kept(), "graines d'univers (3)"));
        assertTrue(mentions(guide.kept(), "cumul du vide (1 250)"));
        assertTrue(mentions(guide.kept(), "Les skins (1 débloqués)"));
        assertTrue(mentions(guide.kept(), "dimensions"));
        assertTrue(mentions(guide.kept(), "La graine : palier"));

        assertTrue(game.startNextUniverse());

        assertEquals(BigNum.of(3), game.state().universeSeeds());
        assertEquals(1250, game.state().voidAccumulated(), 1e-9);
        assertEquals(42, game.state().dimensionShards(), 1e-9);
        assertEquals(2, game.state().lineSpeedLevel());
        assertTrue(game.state().unlockedSkins().contains(idle.core.universe.Skin.values()[0].id()));
        assertTrue(game.isReplaying(), "une partie neuve, celle de l'univers suivant");
    }
}

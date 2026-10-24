import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameState;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.save.SaveCodec;
import idle.core.universe.CycleChoice;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Version 0.130 : les bots jouent l'univers suivant. La politique du deuxième univers ne fait rien dans le premier,
 * l'univers visé décide quand s'arrêter, et les profils comme le bot V2 enchaînent sur l'univers 2.
 */
class NextUniverseTest {

    /** La partie que le Speedrunner (graine 201) laisse en formant le premier univers : le départ de l'univers 2. */
    static final Path UNIVERSE_ONE = Path.of("sauvegardes", "reference", "checkpoint_universe1_speedrunner.save");

    private static Game fresh() {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        game.start();
        return game;
    }

    private static Game load(Path save) throws Exception {
        GameState state = SaveCodec.read(Files.readString(save)).state();
        return new Game(state, Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT, Achievements.DEFAULT,
                new Random(42));
    }

    @Test
    void rienNeChangeDansLePremierUnivers() {
        Game game = fresh();
        game.state().setRegimeOpen(true);
        game.state().setBigBangs(1);
        for (int z = 1; z <= 12; z++) game.state().setElementCount(z, 1);
        NextUniversePolicy policy = new NextUniversePolicy(CycleChoice.FUSION);
        assertEquals(0, policy.play(game));
        assertEquals(0, game.state().regimeLeft(), 1e-9);
        assertEquals(0, policy.regimes + policy.cycles + policy.families + policy.freeElements);
    }

    @Test
    void lUniversViseDecideQuandLancerLeSuivant() {
        Game game = fresh();
        assertFalse(NextUniversePolicy.reached(game, 1));
        assertFalse(NextUniversePolicy.launchNext(game, 2), "rien à lancer avant le premier univers");
        game.testFormUniverse();
        assertTrue(NextUniversePolicy.reached(game, 1));
        assertFalse(NextUniversePolicy.launchNext(game, 1), "un seul univers visé : on s'arrête");
        assertFalse(NextUniversePolicy.reached(game, 2));
        assertTrue(NextUniversePolicy.launchNext(game, 2));
        assertTrue(game.isReplaying());
        assertEquals(0, game.bigBangs());
        assertFalse(NextUniversePolicy.reached(game, 2));
        assertFalse(NextUniversePolicy.reached(game, 1), "la partie qui mène au suivant n'est pas finie");
        game.testFormUniverse();
        assertEquals(2, game.universes());
        assertTrue(NextUniversePolicy.reached(game, 2));
    }

    @Test
    void leDeuxiemeUniversJoueRegimeCycleEtElementsOfferts() {
        Game game = fresh();
        game.testFormUniverse();
        game.startNextUniverse();
        game.state().setRegimeOpen(true);
        game.state().setBigBangs(1);
        for (int z = 1; z <= 10; z++) game.state().setElementCount(z, 1);
        NextUniversePolicy policy = new NextUniversePolicy(CycleChoice.SYNTHESIS);
        assertTrue(policy.play(game) >= 3);
        assertEquals(1, policy.regimes);
        assertTrue(game.state().regimeLeft() > 0, "le régime rapide est lancé");
        assertEquals(CycleChoice.SYNTHESIS, game.cycleChoice());
        assertEquals(1, policy.freeElements);
        assertEquals(0, game.freeElementsAvailable());
        assertEquals(11, game.discoveredElements(), "l'élément offert est un élément nouveau");
        // À la décision suivante, rien à refaire : le régime tourne, le cycle est choisi.
        assertEquals(0, policy.play(game));
    }

    @Test
    void laFuiteNeRetientPasLeBigBang() {
        Game game = fresh();
        game.testFormUniverse();
        game.startNextUniverse();
        game.state().setExplosions(1);   // la matière noire existe : les défis se montrent
        for (var challenge : idle.core.darkmatter.Challenges.DEFAULT) game.state().addCompletedChallenge(challenge.id());
        assertTrue(game.baseChallengesDone());
        assertTrue(game.challenges().size() > game.completedChallenges(), "la fuite s'est montrée, pas encore réussie");
        // La politique des bots compte les dix défis de toujours : la fuite manquante ne retient pas la préparation.
        game.state().setBigBangs(1);
        game.state().setDarkMatter(idle.core.math.BigNum.of(1, 6));
        game.state().setParticles(idle.core.math.BigNum.of(1, 200_000));
        game.achievements().stream().limit(Game.BIG_BANG_ACHIEVEMENTS).forEach(a -> game.state().addAchievement(a.id()));
        assertTrue(game.isBigBangConditionMet(idle.core.bigbang.BigBangCondition.PARTICLES));
        assertTrue(FirstUniversePolicy.finalBangPreparation(game), "le Big Bang se prépare sans attendre la fuite");
    }

    @Test
    void unDefiAbandonneSeRetenteDansLUniversSuivant() {
        Game game = fresh();
        game.testFormUniverse();
        RobotCosmosPlanner bot = new RobotCosmosPlanner(game, 20);
        bot.targetUniverses = 2;
        bot.failedChallenges.put("challenge_x", 500);
        assertFalse(bot.universeProgress());
        assertTrue(bot.launchedNextUniverse);
        assertTrue(bot.failedChallenges.isEmpty(), "les explosions repartent de zéro : l'attente aussi");
    }

    @Test
    void lesProfilsEnchainentSurLUniversDeux(@TempDir Path out) throws Exception {
        IdleProfiles24.main(new String[] {"1", UNIVERSE_ONE.toString(), "0.2", "20", out.toString(), "201", "late", "2"});
        assertTrue(Files.exists(out.resolve("01_Speedrunner_late.csv")), "l'historique de l'univers 2 est écrit");
        assertFalse(Files.exists(out.resolve("01_Speedrunner_winner.save")), "l'univers 2 n'est pas fini en 12 minutes");
        // Le bilan de la partie : relancée dans l'univers 2, avec ses gestes.
        Game started = load(UNIVERSE_ONE);
        assertTrue(NextUniversePolicy.reached(started, 1), "la sauvegarde de référence est l'univers 1 formé");
        IdleProfiles24 bot = new IdleProfiles24(started, IdleProfiles24.PROFILES[0], 201, 20);
        bot.targetUniverses = 2;
        bot.execute(0.5, out.resolve("direct"));
        assertTrue(started.isReplaying(), "l'univers 2 est lancé");
        assertTrue(started.fusionsSinceUniverse() > 0, "l'univers 2 se joue : des fusions depuis son lancement");
        assertTrue(started.isRegimeUnlocked());
        assertTrue(bot.nextUniverse.regimes > 0, "le régime rapide sert");
    }

    @Test
    void leBotV2EnchaineAussi(@TempDir Path out) throws Exception {
        AdvancedLab.main(new String[] {"DEPENDANCES", UNIVERSE_ONE.toString(), "0.1", "20", "42", out.toString(), "2"});
        String result = Files.readString(out.resolve("result.csv"));
        assertTrue(result.lines().findFirst().orElse("").endsWith("univers_formes,univers_h"));
        assertTrue(Files.readString(out.resolve("events.csv")).contains("UNIVERS_SUIVANT"));
    }
}

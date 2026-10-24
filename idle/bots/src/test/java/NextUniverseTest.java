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
        assertTrue(result.lines().findFirst().orElse("").endsWith("univers_formes,univers_h,anomalies"));
        assertTrue(Files.readString(out.resolve("events.csv")).contains("UNIVERS_SUIVANT"));
    }

    /** Un deuxième univers où tout s'offre : régime prêt, un Big Bang (choix de cycle), dix éléments découverts. */
    private static Game everythingOffered() {
        Game game = fresh();
        game.testFormUniverse();
        game.startNextUniverse();
        game.state().setRegimeOpen(true);
        game.state().setBigBangs(1);
        for (int z = 1; z <= 10; z++) game.state().setElementCount(z, 1);
        return game;
    }

    @Test
    void chaqueProfilASaFaconDeJouerLUniversDeux() {
        assertEquals(IdleProfiles24.PROFILES.length, IdleProfiles24.STYLES.length, "une façon de jouer par profil");
        // Le débutant oublie le régime et prend le premier élément offert ; le choix de cycle, il le fait (60 %).
        Game beginner = everythingOffered();
        NextUniversePolicy policy = new NextUniversePolicy(IdleProfiles24.STYLES[15], new Random(1));
        for (int i = 0; i < 20; i++) policy.play(beginner);
        assertEquals(0, policy.regimes, "le débutant ne pense jamais au régime rapide");
        assertTrue(beginner.cycleChoice() == null || beginner.cycleChoice() == CycleChoice.FUSION, "le premier choix proposé");
        int chosen = 0;
        for (int seed = 0; seed < 20; seed++) {
            Game other = everythingOffered();
            new NextUniversePolicy(IdleProfiles24.STYLES[15], mixed(seed)).play(other);
            if (other.cycleChoice() != null) {
                chosen++;
                assertEquals(CycleChoice.FUSION, other.cycleChoice(), "le premier choix proposé");
            }
        }
        assertTrue(chosen > 0 && chosen < 20, "il y pense une fois sur deux à peu près : " + chosen + "/20");
        assertTrue(beginner.elementCount(11) > 0, "le premier élément qui manque : le sodium");
        // Le thésauriseur met en réserve ; le chimiste prend la synthèse.
        Game hoarder = everythingOffered();
        NextUniversePolicy hoarding = new NextUniversePolicy(IdleProfiles24.STYLES[11], new Random(1));
        for (int i = 0; i < 20; i++) hoarding.play(hoarder);
        assertEquals(CycleChoice.RESERVE, hoarder.cycleChoice());
        Game chemist = everythingOffered();
        new NextUniversePolicy(IdleProfiles24.STYLES[6], new Random(1)).play(chemist);
        assertEquals(CycleChoice.SYNTHESIS, chemist.cycleChoice());
        // L'expert relance le régime, prend l'élément le plus utile (ici le plus lourd) et choisit la fusion.
        Game expert = everythingOffered();
        NextUniversePolicy best = new NextUniversePolicy(IdleProfiles24.STYLES[0], new Random(1));
        best.play(expert);
        assertEquals(1, best.regimes);
        assertEquals(CycleChoice.FUSION, expert.cycleChoice());
        assertTrue(expert.elementCount(118) > 0);
        // Le cliqueur au hasard choisit quand même un cycle, au hasard.
        Game random = everythingOffered();
        NextUniversePolicy clicker = new NextUniversePolicy(IdleProfiles24.STYLES[14], new Random(3));
        for (int i = 0; i < 20; i++) clicker.play(random);
        assertTrue(random.cycleChoice() != null);
    }

    @Test
    void lesBotsSaisissentLesAnomaliesDuCiel() {
        Game game = fresh();
        game.state().setBigBangs(1);
        game.state().setSkyEvent(idle.core.sky.SkyEvent.PARTICLE_WIND.name());
        game.state().setSkyEventVisible(100);
        RobotCosmosPlanner bot = new RobotCosmosPlanner(game, 20);
        assertTrue(bot.catchSkyEvent());
        assertEquals(1, bot.skyEventsCaught);
        assertEquals(Game.WIND_FACTOR, game.windMultiplier(), 1e-9);
        assertFalse(bot.catchSkyEvent(), "plus rien à saisir");
        // Sans anomalie visible, rien ; la propriété idle.sansAnomalies les fait ignorer.
        game.state().setSkyEvent(idle.core.sky.SkyEvent.PARTICLE_WIND.name());
        game.state().setSkyEventVisible(100);
        System.setProperty("idle.sansAnomalies", "true");
        try {
            assertFalse(bot.catchSkyEvent());
        } finally {
            System.clearProperty("idle.sansAnomalies");
        }
        assertTrue(bot.catchSkyEvent());
    }

    /** Un hasard par graine, mélangé : les premiers tirages de graines voisines se ressemblent trop. */
    private static Random mixed(int seed) {
        return new Random(new Random(seed * 0x9E3779B97F4A7C15L).nextLong());
    }

    /** Un hasard qui refuse d'être tiré : l'expert ne doit en consommer aucun. */
    private static final class NoRandom extends Random {
        private static final long serialVersionUID = 1L;

        @Override
        protected int next(int bits) {
            throw new AssertionError("l'expert ne tire pas de hasard");
        }
    }

    @Test
    void lExpertNeTireAucunHasard() {
        Game game = everythingOffered();
        NextUniversePolicy expert = new NextUniversePolicy(NextUniversePolicy.Style.EXPERT, new NoRandom());
        for (int i = 0; i < 30; i++) {
            expert.play(game);
            game.tick(20);
        }
        assertTrue(expert.regimes > 0 && expert.cycles == 1 && expert.freeElements > 0);
    }

    @Test
    void leDistraitOublieLeCycleEntier() {
        // Avec 50 % d'attention au choix de cycle, un oubli vaut pour tout le cycle, pas pour une décision.
        int forgotten = 0;
        for (int seed = 0; seed < 40; seed++) {
            Game game = everythingOffered();
            NextUniversePolicy forgetful = new NextUniversePolicy(IdleProfiles24.STYLES[22], mixed(seed));
            for (int i = 0; i < 50; i++) forgetful.play(game);
            if (game.cycleChoice() == null) forgotten++;
        }
        assertTrue(forgotten > 5 && forgotten < 35, "à peu près un cycle sur deux oublié : " + forgotten + "/40");
    }

    @Test
    void laFamilleChoisieUneFoisNeChangePlus() {
        Game game = everythingOffered();
        game.state().setPreferredFamily(idle.core.elements.ElementCategory.NOBLE_GAS.name());
        NextUniversePolicy once = new NextUniversePolicy(IdleProfiles24.STYLES[16], new Random(1));
        for (int i = 0; i < 30; i++) once.play(game);
        assertEquals(0, once.families, "AFKOpen n'y revient pas");
    }
}

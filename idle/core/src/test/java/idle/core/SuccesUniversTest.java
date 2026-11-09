package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.bigbang.BangChallenge;
import idle.core.universe.Trial;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.174 : les nouveautés du deuxième univers s'ouvrent par leurs succès, dans l'ordre des actes, et une vingtaine
 * de succès jalonnent la suite jusqu'à la fin de la partie.
 */
class SuccesUniversTest {

    /** Tous les succès du deuxième univers : aucun ne peut tomber dans le premier. */
    private static final List<String> UNIVERSE_STEPS = List.of(Achievements.UNIVERSE_SKINS, Achievements.UNIVERSE_REGIME,
            Achievements.UNIVERSE_SYNERGY, Achievements.UNIVERSE_PERFECT, Achievements.UNIVERSE_FREE, Achievements.UNIVERSE_TRIALS,
            Achievements.UNIVERSE_DENSE, Achievements.UNIVERSE_PREFERENCE, Achievements.UNIVERSE_COLLIDER_TRIALS,
            Achievements.UNIVERSE_LEAK, Achievements.UNIVERSE_CYCLE, Achievements.UNIVERSE_PLANETS);

    @Test
    void chaqueSuccesDuDeuxiemeUniversEstAuCatalogueEtDitCeQuIlOuvre() {
        for (String id : UNIVERSE_STEPS) {
            Achievement achievement = Achievements.byId(id);
            assertNotNull(achievement, id);
            assertTrue(achievement.description().contains("Ouvre"), id);
            assertFalse(achievement.hasBonus(), "son bonus, c'est ce qu'il ouvre : " + id);
        }
        for (BangChallenge challenge : BangChallenge.values()) assertNotNull(Achievements.byId(Achievements.universeBangId(challenge)));
        // Dans l'ordre des actes : l'onglet Skins d'abord, le petit arbre du cycle au premier Big Bang.
        List<Achievement> all = Achievements.DEFAULT;
        assertTrue(all.indexOf(Achievements.byId(Achievements.UNIVERSE_SKINS)) < all.indexOf(Achievements.byId(Achievements.UNIVERSE_TRIALS)));
        assertTrue(all.indexOf(Achievements.byId(Achievements.UNIVERSE_TRIALS)) < all.indexOf(Achievements.byId(Achievements.UNIVERSE_CYCLE)));
    }

    @Test
    void rienDuDeuxiemeUniversAvantLaFormationDuPremier() {
        Game game = GameTestSupport.realGame();
        game.state().setLevel("generator", game.generatorsPerAtom() - 1);
        game.state().setExplosions(3);
        GameTestSupport.bangs(game, 2);
        game.tick(1);
        for (String id : UNIVERSE_STEPS) assertFalse(game.hasAchievement(id), id);
        assertFalse(game.isCycleChoiceUnlocked());
        assertFalse(game.universeStep(Achievements.UNIVERSE_CYCLE));
    }

    @Test
    void lesNouveautesDeLActeUnTombentAvecLeursSucces() {
        Game game = DeuxiemeUniversTest.second();
        game.tick(1.1);
        assertFalse(game.hasAchievement(Achievements.UNIVERSE_SYNERGY));
        assertFalse(game.isSynergyUnlocked());
        game.state().setLevel("generator", game.generatorsPerAtom() - 1);
        game.tick(1.1);
        assertTrue(game.hasAchievement(Achievements.UNIVERSE_REGIME));
        assertTrue(game.hasAchievement(Achievements.UNIVERSE_SYNERGY));
        assertTrue(game.isRegimeUnlocked() && game.isSynergyUnlocked());
        assertFalse(game.isSkinsUnlocked(), "l'onglet Skins attend cinq fusions");
        game.testAddFusions(GameUniverse.SKINS_FUSIONS);
        game.tick(1.1);
        assertTrue(game.hasAchievement(Achievements.UNIVERSE_SKINS));
        assertTrue(game.isSkinsUnlocked());
    }

    @Test
    void lePetitArbreDuCycleResteOuvertParSonSuccesMaisAttendLeBigBangDeChaquePartie() {
        Game game = DeuxiemeUniversTest.second();
        assertFalse(game.isCycleChoiceUnlocked());
        GameTestSupport.bangs(game, 1);
        game.tick(1.1);
        assertTrue(game.hasAchievement(Achievements.UNIVERSE_CYCLE));
        assertTrue(game.isCycleChoiceUnlocked());
        // Un nouvel univers : le succès reste, le Big Bang de la partie est à refaire.
        game.testFormUniverse();
        assertTrue(game.startNextUniverse());
        assertTrue(game.hasAchievement(Achievements.UNIVERSE_CYCLE));
        assertFalse(game.isCycleChoiceUnlocked(), "pas encore de Big Bang dans cette partie");
        GameTestSupport.bangs(game, 1);
        assertTrue(game.isCycleChoiceUnlocked());
    }

    @Test
    void leNoyauDenseAttendUneExplosionEtLesEpreuvesAussi() {
        Game game = SkinsObtenusTest.skins();
        game.state().stats().noteFusion(GameUniverse.DENSE_UNLOCK_ATOMS, 1);
        game.tick(1.1);
        assertFalse(game.isDenseResearchUnlocked(), "acte 2 : il faut une explosion");
        assertFalse(game.isTrialUnlocked(Trial.LONE_GENERATOR));
        game.state().setExplosions(1);
        game.tick(1.1);
        assertTrue(game.hasAchievement(Achievements.UNIVERSE_DENSE) && game.isDenseResearchUnlocked());
        assertTrue(game.hasAchievement(Achievements.UNIVERSE_TRIALS) && game.isTrialUnlocked(Trial.LONE_GENERATOR));
    }

    @Test
    void lesBonusDUniversDUnDefiComptentParSonSuccesTantQuIlEstReussi() {
        Game game = DeuxiemeUniversTest.second();
        assertEquals(0, game.universeBangChallenges());
        game.state().addCompletedBangChallenge(BangChallenge.VOID.name());
        // La condition remplie suffit, avant même que le succès soit noté au pas suivant.
        assertEquals(1, game.universeBangChallenges());
        assertEquals(2, game.universeBangFactor(Game.BANG_UNIVERSE_PARTICLES), 1e-9);
        game.tick(1.1);
        assertTrue(game.hasAchievement(Achievements.universeBangId(BangChallenge.VOID)));
        // Un nouvel univers, sans la bulle qui garde les défis : ses bonus s'arrêtent, le succès reste.
        game.testFormUniverse();
        assertTrue(game.startNextUniverse());
        assertFalse(game.isBangChallengeCompleted(BangChallenge.VOID));
        assertEquals(0, game.universeBangChallenges());
        assertTrue(game.hasAchievement(Achievements.universeBangId(BangChallenge.VOID)));
    }

    @Test
    void lesPlanetesDansDeuxSystemesAvecLesTroisDefisDOrigine() {
        Game game = DeuxiemeUniversTest.second();
        game.state().addCompletedBangChallenge(BangChallenge.VOID.name());
        game.state().addCompletedBangChallenge(BangChallenge.FORGOTTEN.name());
        game.tick(1.1);
        assertFalse(game.planetsTwice());
        game.state().addCompletedBangChallenge(BangChallenge.BY_HAND.name());
        game.tick(1.1);
        assertTrue(game.hasAchievement(Achievements.UNIVERSE_PLANETS));
        assertTrue(game.planetsTwice());
    }

    @Test
    void uneVieilleSauvegardeGardeCeQuiEtaitOuvertEtRecoitSesSucces() {
        Game game = DeuxiemeUniversTest.second();
        game.state().setSkinsOpen(true);
        game.state().setRegimeOpen(true);
        game.state().setSynergyOpen(true);
        assertTrue(game.isSkinsUnlocked() && game.isRegimeUnlocked() && game.isSynergyUnlocked());
        Game reread = new Game(GameTestSupport.reread(game.state()), idle.core.upgrades.Upgrades.DEFAULT,
                idle.core.upgrades.Automations.DEFAULT, idle.core.darkmatter.DarkUpgrades.DEFAULT, Achievements.DEFAULT,
                new java.util.Random(42L));
        assertFalse(reread.hasAchievement(Achievements.UNIVERSE_SKINS), "une sauvegarde d'avant la 0.174");
        reread.tick(1.1);
        assertTrue(reread.isSkinsUnlocked() && reread.isRegimeUnlocked() && reread.isSynergyUnlocked());
        assertTrue(reread.hasAchievement(Achievements.UNIVERSE_SKINS));
        assertTrue(reread.hasAchievement(Achievements.UNIVERSE_REGIME));
        assertTrue(reread.hasAchievement(Achievements.UNIVERSE_SYNERGY));
        // Sauvegardés comme les autres succès : relus tels quels.
        assertTrue(GameTestSupport.reread(reread.state()).achievements().contains(Achievements.UNIVERSE_SKINS));
    }

    @Test
    void lesSuccesDeLaFinDePartie() {
        Game game = GameTestSupport.universe(10);
        game.tick(1.1);
        assertTrue(game.hasAchievement("late_ten_universes"));
        assertTrue(game.hasAchievement("step_universe_cluster"));
        assertFalse(game.hasAchievement("late_end"));
        long late = Achievements.DEFAULT.stream().filter(a -> a.id().startsWith("late_")).count();
        assertEquals(15, late);
        long withBonus = Achievements.DEFAULT.stream().filter(a -> a.id().startsWith("late_") && a.hasBonus()).count();
        assertEquals(6, withBonus, "quelques petits bonus, comme avant");
    }

    @Test
    void laPuissanceDesSuccesResteCelleDAvantDansLePremierUnivers() {
        boolean off = GameParticles.powersOff;
        GameParticles.powersOff = false;
        try {
            Game game = GameTestSupport.realGame();
            assertEquals(97, game.achievements().size());                 // 0.178 : les deux succès de collection sont partis
            // Un tiers des 67 succès d'avant : la même puissance qu'en 0.173, pas un tiers de 97.
            for (int i = 0; i < 22; i++) game.state().addAchievement(Achievements.DEFAULT.get(i).id());
            assertEquals(Game.POWER_ACHIEVEMENTS * 22 / 67.0, game.achievementPower(), 1e-12);
            // Pleine à 67, et pas au-delà avec les succès de la 0.174.
            for (Achievement achievement : Achievements.DEFAULT) game.state().addAchievement(achievement.id());
            assertEquals(97, game.achievementCount());
            assertEquals(Game.POWER_ACHIEVEMENTS, game.achievementPower(), 1e-12);
        } finally {
            GameParticles.powersOff = off;
        }
    }
}

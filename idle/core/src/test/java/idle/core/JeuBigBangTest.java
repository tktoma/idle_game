package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.bigbang.BigBangCondition;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.elements.Element;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : le Big Bang et l'expansion de la matière.
 */
class JeuBigBangTest extends GameTestSupport {


    @Nested
    class BigBang {

        /** Le vrai jeu, démarré, avec ses catalogues complets. */
        private Game fullGame() {
            Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                    Achievements.DEFAULT, new Random(42));
            game.start();
            return game;
        }

        /** Réunit exactement ce que le Big Bang demande, pas davantage. */
        private Game readyGame() {
            Game game = fullGame();
            game.state().setExplosions(1);
            game.state().setParticles(Game.BIG_BANG_PARTICLES);
            game.state().setAtoms(Game.BIG_BANG_ATOMS);
            game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER);
            giveAchievements(game, Game.BIG_BANG_ACHIEVEMENTS);
            for (Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
            return game;
        }

        private void giveAchievements(Game game, int count) {
            int given = 0;
            for (Achievement achievement : game.achievements()) {
                if (given++ < count) game.state().addAchievement(achievement.id());
            }
        }

        @Test
        void unePartieNeuveNeRemplitAucuneCondition() {
            Game game = fullGame();
            assertEquals(0, game.bigBangConditionsMet());
            assertFalse(game.canBigBang());
            assertFalse(game.bigBang());
            assertEquals(0, game.bigBangs());
        }

        @Test
        void lesCinqConditionsReuniesOuvrentLeBigBang() {
            Game game = readyGame();
            for (BigBangCondition condition : BigBangCondition.values()) {
                assertTrue(game.isBigBangConditionMet(condition), "condition " + condition);
            }
            assertEquals(5, game.bigBangConditionsMet());
            assertTrue(game.canBigBang());
        }

        @Test
        void ilManqueDesParticules() {
            Game game = readyGame();
            game.state().setParticles(Game.BIG_BANG_PARTICLES.divide(BigNum.of(10)));
            assertFalse(game.isBigBangConditionMet(BigBangCondition.PARTICLES));
            assertEquals(4, game.bigBangConditionsMet());
            assertFalse(game.canBigBang());
            assertFalse(game.bigBang());
        }

        @Test
        void ilManqueDesAtomes() {
            Game game = readyGame();
            game.state().setAtoms(Game.BIG_BANG_ATOMS.divide(BigNum.of(2)));
            assertFalse(game.isBigBangConditionMet(BigBangCondition.ATOMS));
            assertFalse(game.canBigBang());
        }

        @Test
        void laMatiereNoireDepenseeNeComptePlus() {
            Game game = readyGame();
            // Tout a été gagné, mais une partie est dépensée : il n'en reste plus assez en réserve.
            game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER.subtract(BigNum.ONE));
            game.state().setDarkMatterSpent(BigNum.of(1000));
            assertTrue(game.darkMatterEarned().gte(Game.BIG_BANG_DARK_MATTER));
            assertFalse(game.isBigBangConditionMet(BigBangCondition.DARK_MATTER));
            assertFalse(game.canBigBang());
        }

        @Test
        void trenteNeufSuccesNeSuffisentPas() {
            Game game = fullGame();
            giveAchievements(game, Game.BIG_BANG_ACHIEVEMENTS - 1);
            assertFalse(game.isBigBangConditionMet(BigBangCondition.ACHIEVEMENTS));
            giveAchievements(game, Game.BIG_BANG_ACHIEVEMENTS);
            assertTrue(game.isBigBangConditionMet(BigBangCondition.ACHIEVEMENTS));
            giveAchievements(game, 50);
            assertTrue(game.isBigBangConditionMet(BigBangCondition.ACHIEVEMENTS));
        }

        @Test
        void unDefiManquantSuffitARefuser() {
            Game game = fullGame();
            game.state().setExplosions(1);
            List<Challenge> challenges = game.challenges();
            for (int index = 0; index < challenges.size() - 1; index++) {
                game.state().addCompletedChallenge(challenges.get(index).id());
            }
            assertFalse(game.isBigBangConditionMet(BigBangCondition.CHALLENGES));
            game.state().addCompletedChallenge(challenges.get(challenges.size() - 1).id());
            assertTrue(game.isBigBangConditionMet(BigBangCondition.CHALLENGES));
        }

        @Test
        void pasDeBigBangPendantUnDefi() {
            Game game = readyGame();
            game.state().setActiveChallenge(game.challenges().get(0).id());
            assertEquals(5, game.bigBangConditionsMet());
            assertFalse(game.canBigBang());
            assertFalse(game.bigBang());
            game.state().setActiveChallenge(null);
            assertTrue(game.canBigBang());
        }

        @Test
        void pasDeBigBangAvantLePremierGenerateur() {
            Game game = readyGame();
            game.state().setStarted(false);
            assertFalse(game.canBigBang());
        }

        @Test
        void leBigBangEffaceLesTroisPremiersActes() {
            Game game = readyGame();
            game.state().setLevel("speed", 12);
            game.state().addAutomation("auto_speed");
            game.state().setElementCount(1, 3);
            game.state().setTotalAtoms(BigNum.of(1, 13));
            game.state().setDarkMatterSpent(BigNum.of(77));
            game.state().setDarkMatterSize(BigNum.of(1, 30));
            game.state().setExplosions(60);
            game.state().setTableWeightLevel(9);
            game.state().setDarkLevel("dark_density", 4);
            game.state().setDarkLevel("dark_overflow", 1);
            game.state().setDarkAutomationEnabled("dark_auto_explosion", true);
            game.state().setHoldLocked(true);

            assertFalse(game.isBigBangUnlocked());
            assertTrue(game.bigBang());

            assertEquals(1, game.bigBangs());
            assertTrue(game.isBigBangUnlocked());
            assertTrue(game.isStarted());
            assertEquals(1, game.generatorCount());
            assertTrue(game.state().particles().isZero());
            assertTrue(game.state().atoms().isZero());
            assertTrue(game.state().totalAtoms().isZero());
            assertEquals(0, game.levelOf("speed"));
            assertFalse(game.ownsAutomation("auto_speed"));
            assertEquals(0, game.discoveredElements());
            assertTrue(game.state().darkMatter().isZero());
            assertTrue(game.darkMatterEarned().isZero());
            assertValue(Game.DARK_MATTER_START_SIZE.toDouble(), game.state().darkMatterSize());
            assertEquals(0, game.state().explosions());
            assertFalse(game.isDarkMatterUnlocked());
            assertEquals(0, game.state().tableWeightLevel());
            assertValue(1, game.tableWeight());
            assertEquals(0, game.darkLevelOf("dark_density"));
            assertFalse(game.isAtomCapLifted());
            // Les réglages du joueur restent, mais ne servent plus tant que l'arbre ne les a pas rouverts.
            assertTrue(game.state().isDarkAutomationEnabled("dark_auto_explosion"));
            assertFalse(game.isDarkAutomationEnabled("dark_auto_explosion"));
            assertTrue(game.state().holdLocked());
            assertFalse(game.isHoldLocked());
            assertEquals(0, game.completedChallenges());
            assertFalse(game.canBigBang());
        }

        @Test
        void leBigBangGardeLesSuccesLesRecordsEtLeTemps() {
            Game game = readyGame();
            String challenge = game.challenges().get(0).id();
            game.state().setChallengeTime(challenge, 321);
            game.state().setTimePlayed(98765);

            assertTrue(game.bigBang());

            assertEquals(Game.BIG_BANG_ACHIEVEMENTS, game.achievementCount());
            assertTrue(game.isBigBangConditionMet(BigBangCondition.ACHIEVEMENTS));
            assertEquals(321.0, game.challengeBestTime(challenge), 1e-9);
            assertFalse(game.isChallengeCompleted(challenge));
            assertEquals(98765.0, game.state().timePlayed(), 1e-9);
            assertEquals(0.0, game.stats().runTime(), 1e-9);
        }

        @Test
        void apresLeBigBangLaPartieSeRejoue() {
            Game game = readyGame();
            assertTrue(game.bigBang());
            // Le jeu tourne de nouveau depuis un seul générateur, et un deuxième Big Bang se mérite.
            game.tick(60);
            assertTrue(game.state().particles().gt(BigNum.ZERO));
            assertFalse(game.bigBang());
            assertEquals(1, game.bigBangs());
        }

        @Test
        void lesBigBangsSeComptent() {
            Game game = readyGame();
            assertTrue(game.bigBang());
            game.state().setExplosions(1);
            game.state().setParticles(Game.BIG_BANG_PARTICLES);
            game.state().setAtoms(Game.BIG_BANG_ATOMS);
            game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER);
            for (Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
            assertTrue(game.bigBang());
            assertEquals(2, game.bigBangs());
        }

        @Test
        void leBigBangLaisseDansLeReleveLaMatiereNoireQuIlReprend() {
            Game game = readyGame();
            game.state().setElementCount(1, 3);
            int copies = game.ownedCopies();
            assertTrue(game.bigBang());
            assertTrue(game.state().darkMatter().isZero());
            game.tick(StatsHistory.FIRST_INTERVAL * 2);
            List<StatsHistory.Sample> samples = game.stats().history().samples();
            StatsHistory.Sample last = samples.get(samples.size() - 1);
            assertEquals(Game.BIG_BANG_DARK_MATTER.log10(), last.value(StatsHistory.Stat.DARK_MATTER), 1e-9);
            assertEquals(Game.BIG_BANG_ATOMS.log10(), last.value(StatsHistory.Stat.ATOMS), 1e-9);
            assertEquals(Game.BIG_BANG_PARTICLES.log10(), last.value(StatsHistory.Stat.PARTICLES), 1e-6);
            assertEquals(copies, last.value(StatsHistory.Stat.ELEMENT_COPIES));
        }

        @Test
        void leBigBangNoteSaDureeEtSesExplosions() {
            Game game = readyGame();
            GameStats stats = game.stats();
            assertEquals(0, stats.timedBigBangs());
            assertEquals(0, stats.lastBigBangTime());
            assertEquals(0, stats.fastestBigBangTime());
            assertEquals(0, stats.lastBigBangExplosions());
            assertTrue(stats.bigBangTimes().isEmpty());
            assertFalse(stats.reached(GameStats.Step.FIRST_BIG_BANG));
            assertTrue(Double.isNaN(stats.reachedAt(GameStats.Step.FIRST_BIG_BANG)));

            // Avant le premier Big Bang, sa durée court depuis le début du jeu.
            game.state().setExplosions(60);
            game.state().setTimePlayed(5000);
            double first = stats.bigBangTime();
            assertTrue(game.bigBang());
            assertTrue(stats == game.stats());                // les statistiques traversent le Big Bang
            assertEquals(1, stats.timedBigBangs());
            assertEquals(first, stats.lastBigBangTime(), 1e-9);
            assertEquals(first, stats.fastestBigBangTime(), 1e-9);
            assertEquals(60, stats.lastBigBangExplosions());
            assertEquals(0, stats.bigBangTime(), 1e-9);
            assertEquals(5000, stats.reachedAt(GameStats.Step.FIRST_BIG_BANG), 1e-9);

            // Le suivant se compte depuis le précédent ; le premier garde son instant.
            game.tick(40);
            assertEquals(40, stats.bigBangTime(), 1e-6);
            game.state().setExplosions(7);
            game.state().setParticles(Game.BIG_BANG_PARTICLES);
            game.state().setAtoms(Game.BIG_BANG_ATOMS);
            game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER);
            for (Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
            assertTrue(game.bigBang());
            assertEquals(2, stats.timedBigBangs());
            assertEquals(40, stats.lastBigBangTime(), 1e-6);
            assertEquals(first > 0 ? Math.min(first, 40) : 40, stats.fastestBigBangTime(), 1e-6);   // une durée nulle ne compte pas (0.65)
            assertEquals(7, stats.lastBigBangExplosions());
            assertEquals(2, stats.bigBangTimes().size());
            assertEquals(first, stats.bigBangTimes().get(0), 1e-9);
            assertEquals(40, stats.bigBangTimes().get(1), 1e-6);
            assertEquals(5000, stats.reachedAt(GameStats.Step.FIRST_BIG_BANG), 1e-9);

            // La remise à zéro du jeu oublie tout.
            game.reset();
            assertEquals(0, game.stats().timedBigBangs());
            assertFalse(game.stats().reached(GameStats.Step.FIRST_BIG_BANG));
        }

        @Test
        void avecLaSuperpositionLesVoiesEtLesLiaisonsRestentAuBigBang() {
            Game game = readyGame();
            game.state().setBigBangs(4);
            game.state().setDarkMatterSpent(BigNum.of(102));
            game.state().setDarkLevel("dark_overflow", 1);
            game.state().setDarkLevel("dark_gravity", 1);
            game.state().setDarkLevel("dark_harvest", 1);
            game.state().setDarkLevel("link_elements", 1);
            game.state().setColliderUpgradeLevel(idle.core.collider.ColliderUpgrade.SUPERPOSITION, 1);
            assertTrue(game.bigBang());
            assertEquals(1, game.darkLevelOf("dark_gravity"));
            assertEquals(1, game.darkLevelOf("dark_harvest"));
            assertEquals(1, game.darkLevelOf("link_elements"));
            assertValue(102, game.darkMatterEarned());
        }

        @Test
        void leCinquiemeBigBangGardeLArbreDeMatiereNoire() {
            Game game = readyGame();
            game.state().setBigBangs(4);
            game.state().setLevel("speed", 12);
            game.state().setElementCount(1, 3);
            game.state().setDarkMatterSpent(BigNum.of(102));   // dont 25 pour la liaison, rendus au Big Bang
            game.state().setDarkLevel("link_elements", 1);
            game.state().setDarkMatterSize(BigNum.of(1, 30));
            game.state().setExplosions(60);
            game.state().setTableWeightLevel(9);
            game.state().setDarkLevel("dark_density", 4);
            game.state().setDarkLevel("dark_overflow", 1);
            game.state().setDarkLevel("dark_gravity", 1);
            game.state().setDarkLevel("dark_horizon", 2);
            game.state().setDarkAutomationEnabled("dark_auto_explosion", true);
            game.state().setHoldLocked(true);

            // Le palier se gagne au cinquième Big Bang, et ce Big Bang-là en profite déjà.
            assertTrue(game.bigBangKeepsDarkTree());
            assertTrue(game.bigBang());
            assertEquals(5, game.bigBangs());

            // L'arbre est resté : ses cases, ce qu'elles ont coûté, ses automatismes, ses réglages.
            assertEquals(4, game.darkLevelOf("dark_density"));
            assertEquals(1, game.darkLevelOf("dark_overflow"));
            // Les bifurcations se rouvrent (0.69) : la voie prise est rendue, ce qui la suivait reste.
            assertEquals(0, game.darkLevelOf("dark_gravity"));
            assertEquals(2, game.darkLevelOf("dark_horizon"));
            assertTrue(game.darkForkRival("dark_harvest") == null);
            assertEquals(0, game.darkLevelOf("link_elements"));     // la liaison repart avec les voies (0.70)
            assertValue(77, game.darkMatterEarned());
            assertTrue(game.state().isDarkAutomationEnabled("dark_auto_explosion"));
            assertTrue(game.state().holdLocked());
            // La matière noire existe donc dès le début de la partie, sans attendre une explosion.
            assertTrue(game.isDarkMatterUnlocked());

            // Le reste repart : la réserve, la taille, la masse du tableau, les explosions, et toute la matière.
            assertTrue(game.state().darkMatter().isZero());
            assertValue(Game.DARK_MATTER_START_SIZE.toDouble(), game.state().darkMatterSize());
            assertEquals(0, game.state().explosions());
            assertEquals(0, game.state().tableWeightLevel());
            // Les défis, eux, restent réussis depuis le palier du troisième Big Bang.
            assertEquals(game.challenges().size(), game.completedChallenges());
            assertEquals(1, game.generatorCount());
            assertEquals(0, game.levelOf("speed"));
            assertEquals(0, game.discoveredElements());
            assertTrue(game.state().particles().isZero());
            assertFalse(game.canBigBang());
            // Et il en va de même à tous les Big Bangs suivants.
            assertTrue(game.bigBangKeepsDarkTree());
        }

        @Test
        void avantLeCinquiemeLeBigBangReprendLArbre() {
            Game game = readyGame();
            game.state().setBigBangs(3);
            game.state().setDarkMatterSpent(BigNum.of(77));
            game.state().setDarkLevel("dark_density", 4);
            assertFalse(game.bigBangKeepsDarkTree());
            assertTrue(game.bigBang());
            assertEquals(4, game.bigBangs());
            assertEquals(0, game.darkLevelOf("dark_density"));
            assertTrue(game.darkMatterEarned().isZero());
            assertFalse(game.isDarkMatterUnlocked());
            // Le suivant, lui, la gardera.
            assertTrue(game.bigBangKeepsDarkTree());
        }

        @Test
        void leTroisiemeBigBangGardeLesDefisReussis() {
            Game game = readyGame();
            game.state().setBigBangs(2);
            // Le palier se gagne au troisième Big Bang, et ce Big Bang-là en profite déjà.
            assertTrue(game.bigBangKeepsChallenges());
            assertFalse(game.bigBangKeepsDarkTree());
            assertTrue(game.bigBang());
            assertEquals(3, game.bigBangs());
            assertEquals(game.challenges().size(), game.completedChallenges());
            for (Challenge challenge : game.challenges()) assertTrue(game.isChallengeCompleted(challenge.id()), challenge.id());
            // L'arbre, lui, est encore repris : il attend le cinquième.
            assertFalse(game.isDarkMatterUnlocked());
            assertTrue(game.bigBangKeepsChallenges());
        }

        @Test
        void avantLeTroisiemeLeBigBangReprendLesDefis() {
            Game game = readyGame();
            assertFalse(game.bigBangKeepsChallenges());
            assertTrue(game.bigBang());
            assertEquals(0, game.completedChallenges());
            assertFalse(game.bigBangKeepsChallenges());             // le deuxième non plus
            game.state().setBigBangs(2);
            assertTrue(game.bigBangKeepsChallenges());
            // Recommencer de zéro oublie les défis, palier ou non.
            for (Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
            game.reset();
            assertEquals(0, game.completedChallenges());
        }

        @Test
        void laRemiseAZeroEffaceAussiLesBigBangs() {
            Game game = readyGame();
            assertTrue(game.bigBang());
            game.reset();
            assertEquals(0, game.bigBangs());
            assertFalse(game.isBigBangUnlocked());
            assertEquals(0, game.achievementCount());
            assertFalse(game.isStarted());
        }

        @Test
        void unNombreDeBigBangsNegatifEstRefuse() {
            assertThrows(IllegalArgumentException.class, () -> new GameState().setBigBangs(-1));
        }
    }

    @Nested
    class ExpansionDeLaMatiere {

        private Game game(int bigBangs) {
            Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                    Achievements.DEFAULT, new Random(42));
            game.start();
            game.state().setBigBangs(bigBangs);
            return game;
        }

        @Test
        void pasDEspaceAvantLePremierBigBang() {
            Game game = game(0);
            game.tick(3600);
            assertTrue(game.state().space().isZero());
            assertTrue(game.spacePerSecond().isZero());
        }

        @Test
        void unBigBangDonneUneUniteDEspaceParSeconde() {
            Game game = game(1);
            assertValue(1, game.spacePerSecond());
            game.tick(60);
            assertValue(60, game.state().space());
        }

        @Test
        void chaqueBigBangAccelereLExpansion() {
            Game game = game(1);
            assertValue(1, game.spacePerSecond());
            // Une unité par Big Bang, multipliée par les paliers : le deuxième double, le cinquième triple.
            game.state().setBigBangs(3);
            assertValue(3 * 2, game.spacePerSecond());
            game.tick(100);
            assertValue(600, game.state().space());
            game.state().setBigBangs(5);
            assertValue(5 * 2 * 3, game.spacePerSecond());
            game.state().setBigBangs(6);
            assertValue(6 * 2 * 3, game.spacePerSecond());
        }

        @Test
        void unGrosTickVautBeaucoupDePetits() {
            Game small = game(2);
            for (int index = 0; index < 600; index++) small.tick(0.1);
            Game big = game(2);
            big.tick(60);
            assertEquals(big.state().space().toDouble(), small.state().space().toDouble(), 1e-6);
            assertValue(240, big.state().space());        // deux Big Bangs, et le palier du deuxième
        }

        @Test
        void lEspaceNAttendPasLePremierGenerateur() {
            Game game = game(1);
            game.state().setStarted(false);
            game.tick(60);
            assertTrue(game.state().space().isZero());
        }

        @Test
        void lesMoleculesOccupentLEspaceSansRalentirLExpansion() {
            Game game = game(1);
            game.tick(200);
            game.state().setBigBangs(2);
            game.state().setMoleculeCount("NaCl", 1);
            assertValue(4, game.spacePerSecond());
            assertValue(200, game.state().space());
            assertValue(280, game.usedSpace());
            assertTrue(game.freeSpace().isZero());                    // 280 pour 200 d'espace : plus rien de libre
            game.tick(25);
            assertValue(300, game.state().space());
            assertValue(20, game.freeSpace());
        }

        @Test
        void lEspaceSurvitALExplosionEtAuBigBang() {
            Game game = game(1);
            game.tick(50);
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertValue(50, game.state().space());
            game.state().setParticles(Game.BIG_BANG_PARTICLES);
            game.state().setAtoms(Game.BIG_BANG_ATOMS);
            game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER);
            for (Achievement achievement : game.achievements()) game.state().addAchievement(achievement.id());
            for (Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
            assertTrue(game.bigBang());
            assertValue(50, game.state().space());
            game.tick(10);
            assertValue(90, game.state().space());     // deux Big Bangs : deux unités par seconde, doublées par le palier
        }

        @Test
        void laRemiseAZeroEffaceLEspace() {
            Game game = game(1);
            game.tick(50);
            game.reset();
            assertTrue(game.state().space().isZero());
        }

        @Test
        void unEspaceNegatifEstRefuse() {
            assertThrows(IllegalArgumentException.class, () -> new GameState().setSpace(BigNum.of(-1)));
        }
    }
}

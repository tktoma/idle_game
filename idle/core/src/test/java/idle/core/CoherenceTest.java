package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.math.BigNum;
import idle.core.save.SaveCodec;
import idle.core.sky.Cosmos;
import idle.core.universe.Trial;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Version 0.126 : les incohérences relevées par l'audit du moteur. Chaque test dit la règle telle qu'elle est
 * annoncée, et la vérifie là où elle ne tenait pas : au-delà du premier univers, pendant une épreuve, sur un
 * gros pas de temps, après une sauvegarde d'une autre version.
 */
class CoherenceTest {

    private static GameState reread(GameState state) {
        try {
            return SaveCodec.read(SaveCodec.write(state, 0)).state();
        } catch (Exception unreadable) {
            throw new AssertionError(unreadable);
        }
    }

    /** Le premier univers formé à ce temps de jeu, puis la partie suivante lancée à cet autre. */
    private static Game secondAt(double formedAt, double startedAt) {
        Game game = new Game();
        game.start();
        game.state().setTimePlayed(formedAt);
        DeuxiemeUniversTest.formUniverse(game);
        game.state().setTimePlayed(startedAt);
        assertTrue(game.startNextUniverse());
        return game;
    }

    @Nested
    class LesEtapesDeChaquePartie {

        @Test
        void leDelaiEntreLesEchellesDuCosmosVautAussiApresLePremierUnivers() {
            Game game = new Game();
            game.start();
            // Première partie : la galaxie à 10 000 s, l'amas attend deux heures.
            game.state().setTimePlayed(10_000);
            game.state().setCosmosLevel(1);
            game.noteStep(GameStats.Step.GALAXY);
            assertEquals(GameUniverse.COSMOS_SETTLE_SECONDS, game.cosmosSettleRemaining(Cosmos.CLUSTER), 1e-9);
            game.state().setTimePlayed(100_000);
            DeuxiemeUniversTest.formUniverse(game);
            assertTrue(game.startNextUniverse());
            // Partie suivante : sa galaxie à 400 000 s ; l'amas attend de nouveau deux heures, pas zéro.
            game.state().setTimePlayed(400_000);
            game.state().setCosmosLevel(1);
            game.noteStep(GameStats.Step.GALAXY);
            assertEquals(GameUniverse.COSMOS_SETTLE_SECONDS, game.cosmosSettleRemaining(Cosmos.CLUSTER), 1e-9);
            game.state().setTimePlayed(400_000 + GameUniverse.COSMOS_SETTLE_SECONDS);
            assertEquals(0, game.cosmosSettleRemaining(Cosmos.CLUSTER), 1e-9);
            // La toute première galaxie reste datée pour l'écran de fin et le journal.
            assertEquals(10_000, game.stats().reachedAt(GameStats.Step.GALAXY), 1e-9);
        }

        @Test
        void lesRecordsSeComptentDepuisLeDebutDeLaPartie() {
            Game game = new Game();
            game.start();
            game.state().setTimePlayed(5000);
            game.noteStep(GameStats.Step.FIRST_MOLECULE);
            assertEquals(5000, game.recordOf(GameStats.Step.FIRST_MOLECULE), 1e-9);
            game.state().setTimePlayed(90_000);
            DeuxiemeUniversTest.formUniverse(game);
            game.state().setTimePlayed(100_000);
            assertTrue(game.startNextUniverse());
            assertEquals(100_000, game.stats().universeStart(), 1e-9);
            // La nouvelle partie repart de la première étape, et son temps se compte de son début.
            assertEquals(GameStats.Step.FIRST_FUSION, game.nextStep());
            game.state().setTimePlayed(100_600);
            assertEquals(600, game.runTimePlayed(), 1e-9);
            game.takeNewRecords();
            game.state().setTimePlayed(103_000);
            game.noteStep(GameStats.Step.FIRST_MOLECULE);
            assertEquals(3000, game.recordOf(GameStats.Step.FIRST_MOLECULE), 1e-9, "3 000 s de partie battent 5 000 s");
            assertEquals(List.of(GameStats.Step.FIRST_MOLECULE), game.takeNewRecords());
            // Tout cela survit à la sauvegarde.
            GameState back = reread(game.state());
            assertEquals(100_000, back.stats().universeStart(), 1e-9);
            assertEquals(103_000, back.stats().reachedThisUniverseAt(GameStats.Step.FIRST_MOLECULE), 1e-9);
        }

        @Test
        void laFusionLExplosionEtLeBigBangDUnePartieSuivanteBattentLeursRecords() {
            // Par les vrais gestes du jeu, et non par noteStep : leurs appels testaient « la première fois du jeu entier ».
            Game game = new Game();
            game.start();
            for (GameStats.Step step : List.of(GameStats.Step.FIRST_FUSION, GameStats.Step.FIRST_EXPLOSION, GameStats.Step.FIRST_BIG_BANG)) {
                game.state().setRecord(step.name(), 50_000);
            }
            game.state().stats().noteFusion(BigNum.ONE, 1);         // la partie 1 a déjà fusionné, explosé
            game.state().stats().endRun();
            game.state().setTimePlayed(90_000);
            DeuxiemeUniversTest.formUniverse(game);
            game.state().setTimePlayed(100_000);
            assertTrue(game.startNextUniverse());
            game.takeNewRecords();
            // Fusion, à 100 s de partie.
            game.state().setTimePlayed(100_100);
            game.state().setLevel("generator", game.generatorsPerAtom() - 1);
            game.tick(0.01);
            assertTrue(game.fuse());
            assertTrue(game.stats().reachedThisUniverse(GameStats.Step.FIRST_FUSION));
            assertEquals(100.01, game.recordOf(GameStats.Step.FIRST_FUSION), 1e-6);
            // Explosion, à 200 s.
            game.state().setTimePlayed(100_200);
            for (int number = 1; number <= idle.core.elements.PeriodicTable.ELEMENTS.size(); number++) game.state().setElementCount(number, 1);
            assertTrue(game.explode());
            assertEquals(200, game.recordOf(GameStats.Step.FIRST_EXPLOSION), 1e-6);
            // Big Bang, à 500 s.
            game.state().setTimePlayed(100_500);
            game.state().setExplosions(Math.max(1, game.state().explosions()));
            game.state().setParticles(Game.BIG_BANG_PARTICLES);
            game.state().setAtoms(Game.BIG_BANG_ATOMS);
            game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER);
            int given = 0;
            for (idle.core.achievements.Achievement achievement : game.achievements()) {
                if (given++ < Game.BIG_BANG_ACHIEVEMENTS) game.state().addAchievement(achievement.id());
            }
            for (idle.core.darkmatter.Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
            assertTrue(game.bigBang());
            assertEquals(500, game.recordOf(GameStats.Step.FIRST_BIG_BANG), 1e-6);
            assertTrue(game.takeNewRecords().containsAll(List.of(GameStats.Step.FIRST_FUSION, GameStats.Step.FIRST_EXPLOSION,
                    GameStats.Step.FIRST_BIG_BANG)), "les trois sont annoncés");
        }

        @Test
        void uneSauvegardeDAvantNoteLesEtapesDeLaPremierePartieCommeAvant() {
            Game game = new Game();
            game.start();
            game.state().setTimePlayed(5000);
            game.noteStep(GameStats.Step.FIRST_MOLECULE);
            String text = SaveCodec.write(game.state(), 0).replaceAll("\nstats\\.universe(Steps|Start)=[^\n]*", "");
            GameState old;
            try {
                old = SaveCodec.read(text).state();
            } catch (Exception unreadable) {
                throw new AssertionError(unreadable);
            }
            assertTrue(old.stats().reachedThisUniverse(GameStats.Step.FIRST_MOLECULE));
            assertEquals(0, old.stats().universeStart(), 1e-9);
        }
    }

    @Test
    void lAutomatismeDesGenerateursRespecteLEpreuveDuGenerateurSeul() {
        Game game = SkinsObtenusTest.skins(), witness = SkinsObtenusTest.skins();
        for (Game each : List.of(game, witness)) {
            each.state().setExplosions(1);
            each.state().addAutomation("auto_generator");
            each.state().setAutomationEnabled("auto_generator", true);
            each.state().setParticles(BigNum.of(1, 9));
        }
        assertTrue(game.startTrial(Trial.LONE_GENERATOR));
        double wait = game.automationInterval("auto_generator") + 1;
        game.tick(wait);
        witness.tick(wait);
        assertTrue(witness.generatorCount() > 1, "sans l'épreuve, l'automatisme achète bien des générateurs");
        assertEquals(1, game.generatorCount(), "aucun générateur acheté pendant l'épreuve, même par l'automatisme");
        assertEquals(Trial.LONE_GENERATOR, game.activeTrial(), "l'épreuve continue");
    }

    @Test
    void toutLeTempsDAbsenceEstRejoueDansLesUniversSuivants() {
        Game game = secondAt(50_000, 60_000);
        assertFalse(game.hasCosmos(Cosmos.UNIVERSE), "la nouvelle partie n'a pas encore son univers");
        assertEquals(Game.OFFLINE_RATE_UNIVERSE, game.offlineRate(), 1e-12);
    }

    @Nested
    class LesReglagesDuJoueur {

        private void choose(Game game) {
            game.state().setAutomationPaused(true);
            game.state().setAutoGather(false);
            game.state().setAutoForm(false);
            game.state().setAutoHold(false);
            game.state().setFusionThreshold(37);
            game.state().setAutoMoleculeReserve(0.25);
            game.state().setAtomUpgradeOrder(List.of("double", "mass"));
            game.state().setColliderPaused(idle.core.collider.ColliderUpgrade.PARTICLES, true);
        }

        private void assertChosen(GameState state) {
            assertTrue(state.automationPaused());
            assertFalse(state.autoGather());
            assertFalse(state.autoForm());
            assertFalse(state.autoHold());
            assertEquals(37, state.fusionThreshold());
            assertEquals(0.25, state.autoMoleculeReserve(), 1e-12);
            assertEquals(List.of("double", "mass"), state.atomUpgradeOrder());
            assertTrue(state.isColliderPaused(idle.core.collider.ColliderUpgrade.PARTICLES));
        }

        @Test
        void ilsTraversentLaFormationDeLUniversEtLaPartieSuivante() {
            Game game = new Game();
            game.start();
            choose(game);
            DeuxiemeUniversTest.formUniverse(game);
            assertChosen(game.state());
            assertTrue(game.startNextUniverse());
            assertChosen(game.state());
            assertChosen(reread(game.state()));
        }

        @Test
        void seuleUneNouvellePartieLesEfface() {
            Game game = new Game();
            game.start();
            choose(game);
            game.reset();
            assertFalse(game.state().automationPaused());
            assertTrue(game.state().autoGather());
            assertEquals(0, game.state().fusionThreshold());
        }
    }

    @Test
    void rouvrirLesBifurcationsRendLaMatiereNoireDesLiaisons() {
        DarkUpgrade link = DarkUpgrades.DEFAULT.stream().filter(dark -> dark.isLink() && !dark.isAccord()).findFirst().orElseThrow();
        BigNum cost = link.costAt(0);
        Game game = atFork(link, cost);
        Game twin = atFork(null, BigNum.ZERO);
        BigNum spentBefore = game.state().darkMatterSpent();
        BigNum before = game.state().darkMatter(), twinBefore = twin.state().darkMatter();
        for (Game each : List.of(game, twin)) {
            each.setForkResetArmed(true);
            for (int number = 1; number <= idle.core.elements.PeriodicTable.ELEMENTS.size(); number++) each.state().setElementCount(number, 1);
            assertTrue(each.explode());
        }
        assertEquals(0, game.state().darkLevelOf(link.id()), "la liaison est défaite");
        assertEquals(cost, game.takeForkRefund(), "le remboursement est annoncé une fois");
        assertEquals(BigNum.ZERO, game.takeForkRefund());
        assertEquals(spentBefore.subtract(cost), game.state().darkMatterSpent());
        // La réserve reçoit l'explosion comme celle du jumeau sans liaison, plus le remboursement.
        BigNum gained = game.state().darkMatter().subtract(before), twinGained = twin.state().darkMatter().subtract(twinBefore);
        GameTestSupport.assertValue(twinGained.add(cost).toDouble(), gained);
    }

    /** Une partie à une bifurcation, avec une voie prise et, si demandé, cette liaison déjà payée en matière noire. */
    private static Game atFork(DarkUpgrade link, BigNum cost) {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        game.start();
        game.state().setDarkMatter(BigNum.of(12));
        game.state().setExplosions(12);
        for (String id : List.of("dark_nucleus", "dark_multisynthesis", "dark_luck")) game.state().setDarkLevel(id, 1);
        game.state().setAtoms(BigNum.of(5000));
        assertTrue(game.buyDark("dark_rich_fusion"));
        if (link != null) {
            game.state().setDarkLevel(link.id(), 1);
            game.state().setDarkMatterSpent(game.state().darkMatterSpent().add(cost));
        }
        return game;
    }

    @Test
    void uneCategorieDeSyntheseInconnueEstEcarteeSansPerdreLaPartie() throws Exception {
        Game game = new Game();
        game.start();
        game.state().setExplosions(3);
        String text = SaveCodec.write(game.state(), 0) + "synthesisTarget=FAMILLE_DISPARUE\n";
        GameState back = SaveCodec.read(text).state();
        assertNull(back.synthesisTarget());
        assertEquals(3, back.explosions());
    }

    @Nested
    class LesBonusMinutes {

        /** Une partie qui produit, avec cinq secondes de surchauffe. */
        private Game overheated() {
            Game game = new Game();
            game.start();
            game.state().setLevel("generator", 5);
            game.state().setOverheatLeft(5);
            return game;
        }

        @Test
        void unGrosPasDonneLaMemeSurchauffeQueBeaucoupDePetits() {
            Game big = overheated(), small = overheated();
            big.tick(12);
            for (int i = 0; i < 1200; i++) small.tick(0.01);
            assertTrue(big.state().overheatLeft() <= 0 && small.state().overheatLeft() <= 0, "la surchauffe est finie");
            double ratio = big.state().particles().toDouble() / small.state().particles().toDouble();
            assertEquals(1, ratio, 0.02, "même production à 2 % près (créations entières)");
        }

        @Test
        void leRegimeRapideSArreteAuBonMomentEtSonReposSuit() {
            Game game = DeuxiemeUniversTest.second();
            game.state().setRegimeOpen(true);
            assertTrue(game.startRegime());
            game.tick(30);
            assertTrue(game.state().regimeLeft() <= 0);
            assertEquals(GameUniverse.REGIME_REST_SECONDS - 10, game.state().regimeRest(), 1e-6);
        }

        @Test
        void leSursautDesSondesNeCompteQueSonTemps() {
            Game big = new Game(), small = new Game();
            for (Game game : List.of(big, small)) {
                game.start();
                GameTestSupport.bangs(game, 1);
                game.state().setProbeBoostLeft(5);
            }
            BigNum perSecond = big.steadySpacePerSecond().multiply(1 / big.probeBoostMultiplier());
            big.tick(20);
            for (int i = 0; i < 2000; i++) small.tick(0.01);
            // 5 s à ×2 puis 15 s à ×1 : 25 secondes d'expansion ordinaire.
            GameTestSupport.assertValue(perSecond.toDouble() * 25, big.state().space());
            double ratio = big.state().space().toDouble() / small.state().space().toDouble();
            assertEquals(1, ratio, 1e-6);
        }
    }
}

package idle.core;

import idle.core.state.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import idle.core.achievements.Achievements;
import idle.core.collider.*;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.elements.ElementCategory;
import idle.core.math.BigNum;
import idle.core.probes.*;
import idle.core.save.*;
import idle.core.sky.SolarSystem;
import idle.core.upgrades.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static idle.core.SaveTest.*;

/**
 * La suite de {@link SaveTest} (séparée en 0.293 pour rester sous 1 000 lignes) : une vraie partie sauvegardée et
 * relue, le retour après une absence, les courbes des statistiques.
 */
class SaveSuiteTest {
    @Nested
    class PartieReelle {
        private Game play(GameState state, long seed) {
            return new Game(state, Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(seed));
        }

        /** Joue quelque temps comme un joueur pressé : tout acheter, fusionner dès que possible. */
        private void rush(Game game, double seconds) {
            for (double t = 0; t < seconds; t += 0.5) {
                game.tick(0.5);
                game.buyAllWithParticles();
                if (game.canFuse()) game.fuse();
                for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
                    if (game.canBuy(upgrade.id())) game.buy(upgrade.id());
                }
            }
        }

        @Test
        void unePartieJoueeSeRelitChampParChamp() throws Exception {
            Game game = play(new GameState(), 1);
            game.start();
            rush(game, 3000);
            assertTrue(game.stats().fusions() > 0, "la partie d'essai doit avoir fusionné");
            assertTrue(game.stats().history().samples().size() > 50);
            assertEquals(List.of(), differences("partie", game.state(), copy(game.state()), false));
        }

        @Test
        void unePartieRelueContinueCommeLOriginale() throws Exception {
            Game game = play(new GameState(), 1);
            game.start();
            rush(game, 2000);
            Game reloaded = play(copy(game.state()), 1);
            assertEquals(0, reloaded.restored(), "rien d'inconnu dans une sauvegarde de cette version");
            assertEquals(game.productionPerSecond(), reloaded.productionPerSecond());
            assertEquals(game.atomsPerFusion(), reloaded.atomsPerFusion());
            assertEquals(game.generatorCount(), reloaded.generatorCount());
            rush(game, 600);
            rush(reloaded, 600);
            assertEquals(game.state().particles(), reloaded.state().particles());
            assertEquals(game.state().totalAtoms(), reloaded.state().totalAtoms());
            assertEquals(game.stats().fusions(), reloaded.stats().fusions());
            assertEquals(game.state().upgradeLevels(), reloaded.state().upgradeLevels());
            assertEquals(game.state().achievements(), reloaded.state().achievements());
        }

        @Test
        void chargerDansUnePartieOuverteOublieCeQuiEtaitCalculeDAvance() throws Exception {
            Game source = play(new GameState(), 1);
            source.start();
            rush(source, 2000);
            Game open = play(new GameState(), 1);
            open.start();
            rush(open, 50);
            BigNum before = open.productionPerSecond();
            SaveCodec.readInto(open.state(), SaveCodec.export(source.state(), 0));
            open.restored();
            assertFalse(before.equals(open.productionPerSecond()));
            assertEquals(source.productionPerSecond(), open.productionPerSecond());
            assertEquals(source.achievementCount(), open.achievementCount());
            assertEquals(source.achievementParticlesMultiplier(), open.achievementParticlesMultiplier());
            assertEquals(List.of(), open.takeNewAchievements());
        }

        @Test
        void ceQueLeJeuNeConnaitPasEstEcarteAuChargement() throws Exception {
            Game game = play(new GameState(), 1);
            game.start();
            rush(game, 600);
            GameState state = game.state();
            state.setLevel("amelioration_disparue", 3);
            state.addAutomation("automatisme_disparu");
            state.setAutomationEnabled("automatisme_disparu", true);
            state.setElementCount(999, 2);
            state.setDarkLevel("case_disparue", 1);
            state.setActiveChallenge("defi_disparu");
            state.addCompletedChallenge("defi_disparu");
            state.addAchievement("succes_disparu");
            state.addMolecules("XxYy", 5);
            state.addSubstance("XxYy");
            state.setMoleculeAutomated("XxYy", true);
            state.addAssembly("assemblage_disparu");
            state.addBody("astre_disparu");
            state.addSpaceUpgrade("espace_disparu");
            state.setMoleculeFavorite("XxYy", true);
            int known = state.upgradeLevels().size() - 1;

            Game reloaded = play(copy(state), 1);
            assertEquals(15, reloaded.restored());
            GameState kept = reloaded.state();
            assertEquals(known, kept.upgradeLevels().size());
            assertFalse(kept.ownsAutomation("automatisme_disparu"));
            assertEquals(0, kept.elementCount(999));
            assertEquals(0, kept.darkLevelOf("case_disparue"));
            assertEquals(null, kept.activeChallenge());
            assertTrue(kept.completedChallenges().isEmpty());
            assertFalse(kept.achievements().contains("succes_disparu"));
            assertEquals(0, kept.moleculeCount("XxYy"));
            assertTrue(kept.moleculeLog().isEmpty());
            assertFalse(kept.hasSubstance("XxYy"));
            assertFalse(kept.isMoleculeAutomated("XxYy"));
            assertFalse(kept.isMoleculeFavorite("XxYy"));
            assertFalse(kept.hasAssembly("assemblage_disparu"));
            assertFalse(kept.hasBody("astre_disparu"));
            assertFalse(kept.ownsSpaceUpgrade("espace_disparu"));
            // Et le jeu tourne : rien ne cherche plus ce qui a disparu.
            reloaded.tick(60);
            reloaded.activeChallenge();
            reloaded.moleculesCreated();
            reloaded.spacePerSecond();
        }
    }

    @Nested
    class RetourApresUneAbsence {
        private Game started() {
            Game game = GameTestSupport.realGame();
            return game;
        }

        @Test
        void laPartRejoueeGranditAvecLesActes() {
            Game game = started();
            assertEquals(0.10, game.offlineRate());
            game.state().setExplosions(1);
            assertEquals(0.40, game.offlineRate());
            GameTestSupport.bangs(game, 1);
            game.state().setExplosions(0);
            assertEquals(0.70, game.offlineRate());
            game.state().setCosmosLevel(2);
            assertEquals(0.70, game.offlineRate(), "l'amas de galaxies n'est pas encore la fin");
            game.state().setCosmosLevel(3);
            assertEquals(1.0, game.offlineRate());
        }

        @Test
        void lArbreAchetePuisLeBigBangNeFontPasRetomberLaPart() {
            Game game = started();
            // Toute la matière noire dépensée dans l'arbre, puis une explosion à zéro : toujours le deuxième acte.
            game.state().setDarkMatterSpent(BigNum.of(5));
            assertEquals(0.40, game.offlineRate());
        }

        @Test
        void uneAbsenceRejoueSaPartDuTemps() {
            Game game = started();
            double before = game.state().timePlayed();
            Absence absence = new Absence(game, 3600);
            assertEquals(3600, absence.away());
            assertEquals(0.10, absence.rate());
            assertEquals(360, absence.total(), 1e-9);
            assertFalse(absence.isDone());
            absence.advance(100);
            assertEquals(100, absence.done(), 1e-9);
            assertEquals(100.0 / 360, absence.progress(), 1e-9);
            absence.advance(1e9);
            assertTrue(absence.isDone());
            assertEquals(360, absence.done(), 1e-9);
            assertEquals(1, absence.progress());
            assertEquals(before + 360, game.state().timePlayed(), 1e-6);
            // Rien de plus une fois finie.
            absence.advance(50);
            assertEquals(before + 360, game.state().timePlayed(), 1e-6);
        }

        @Test
        void uneAbsenceDonneCeQueLeMemeTempsDeJeuAuraitDonne() {
            Game away = started();
            Game open = started();
            new Absence(away, 5000).advance(1e9);
            for (int i = 0; i < 50; i++) open.tick(10);
            assertEquals(open.state().particles(), away.state().particles());
            assertEquals(open.state().timePlayed(), away.state().timePlayed(), 1e-6);
        }

        @Test
        void uneAbsenceTropCourteNeComptePas() {
            Game game = started();
            Absence absence = new Absence(game, Absence.MIN_SECONDS - 1);
            assertEquals(0, absence.total());
            assertTrue(absence.isDone());
            assertTrue(absence.report().isEmpty());
            assertFalse(new Absence(game, Absence.MIN_SECONDS).isDone());
        }

        @Test
        void uneHorlogeReculeeOuIllisibleNeComptePas() {
            Game game = started();
            assertTrue(new Absence(game, -5000).isDone());
            assertEquals(0, new Absence(game, -5000).away());
            assertTrue(new Absence(game, Double.NaN).isDone());
        }

        @Test
        void rienNEstCompteAuDelaDUneJournee() {
            Game game = started();
            Absence absence = new Absence(game, 10 * 24 * 3600);
            assertEquals(Absence.MAX_SECONDS * 0.10, absence.total(), 1e-9);
            assertEquals(10 * 24 * 3600, absence.report().away());
            assertEquals(Absence.MAX_SECONDS, absence.report().counted());
        }

        @Test
        void unePartiePasCommenceeNeRattrapeRien() {
            Game game = new Game(42L);
            Absence absence = new Absence(game, 7200);
            assertTrue(absence.isDone());
            assertEquals(0, game.state().timePlayed());
        }

        @Test
        void leJoueurPeutPasserLaFin() {
            Game game = started();
            Absence absence = new Absence(game, 36000);
            absence.advance(600);
            absence.skip();
            assertTrue(absence.isDone());
            absence.advance(1e9);
            assertEquals(600, absence.done(), 1e-9);
            assertTrue(absence.report().skipped());
            assertEquals(600, absence.report().played(), 1e-9);
        }

        @Test
        void passerUneAbsenceFinieNEstPasLaPasser() {
            Game game = started();
            Absence absence = new Absence(game, 600);
            absence.advance(1e9);
            absence.skip();
            assertFalse(absence.report().skipped());
        }

        @Test
        void leRapportDitCeQueLAbsenceAAjoute() {
            Game game = started();
            game.tick(500);
            BigNum created = game.stats().particlesCreated();
            Absence absence = new Absence(game, 20000);
            absence.advance(1e9);
            Absence.Report report = absence.report();
            assertFalse(report.isEmpty());
            assertEquals(0.10, report.rate());
            assertEquals(2000, report.played(), 1e-9);
            assertEquals(game.stats().particlesCreated().subtract(created), report.particles());
            assertEquals(0, report.fusions());
            assertTrue(report.space().isZero());
        }

        @Test
        void leRattrapageParTranchesDeTempsReelFinitParToutRejouer() {
            Game game = started();
            Absence absence = new Absence(game, 3600);
            int rounds = 0;
            while (!absence.isDone() && rounds++ < 100_000) absence.advanceFor(1_000_000);
            assertTrue(absence.isDone());
            assertEquals(360, absence.done(), 1e-9);
        }
    }

    /** Les courbes gardées dans la sauvegarde : une colonne relue se réécrit à l'identique. */
    @Nested
    class Courbes {
        @Test
        void uneColonneRelueSeReecritALIdentique() {
            Random random = new Random(7);
            double[] column = new double[5_000];
            for (int i = 0; i < column.length; i++) {
                column[i] = switch (i % 5) {
                    case 0 -> random.nextDouble() * 3000;
                    case 1 -> Math.rint(random.nextDouble() * 3000) - 1e-9;   // un arrondi qui tombe sur un entier
                    case 2 -> random.nextGaussian() * 1e-40;
                    case 3 -> Double.NaN;
                    default -> random.nextDouble() * 1e30;
                };
            }
            String once = StatsHistory.pack(column);
            String twice = StatsHistory.pack(StatsHistory.unpack(once, column.length));
            assertEquals(once, twice);
        }
    }
}

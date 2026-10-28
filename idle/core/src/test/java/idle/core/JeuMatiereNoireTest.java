package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.darkmatter.DarkAutomation;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.Element;
import idle.core.elements.ElementEffect;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : les statistiques, l'historique, la remise à zéro, les améliorations payées
 * en matière noire et les automatismes de matière noire.
 */
class JeuMatiereNoireTest extends GameTestSupport {


    @Nested
    class Statistiques {

        @Test
        void unePartieNeuveNARienCompte() {
            GameStats stats = unstartedGame().stats();
            assertEquals(BigNum.ZERO, stats.particlesCreated());
            assertEquals(BigNum.ZERO, stats.bestProduction());
            assertEquals(0, stats.fusions());
            assertEquals(0, stats.fastestFusionTime());
            assertEquals(0, stats.syntheses());
            assertEquals(0, stats.runTime());
            assertEquals(0, stats.fastestExplosionTime());
        }

        @Test
        void lesParticulesCreeesEtDepenseesSontComptees() {
            Game game = newGame();
            game.tick(10 * ONE_PARTICLE);                     // dix créations : 10 particules
            assertValue(10, game.stats().particlesCreated());
            assertValue(10, game.stats().runParticlesCreated());
            assertEquals(10 * ONE_PARTICLE, game.stats().runTime(), 1e-9);
            BigNum cost = game.costOf("speed");
            assertTrue(game.buy("speed"));
            assertEquals(cost, game.stats().particlesSpent());
            assertEquals(1, game.stats().particleUpgradesBought());
            assertEquals(0, game.stats().generatorsBought());
            game.state().setParticles(BigNum.of(1e6));       // donnés, pas créés
            BigNum generator = game.costOf("gen");
            assertTrue(game.buy("gen"));
            assertEquals(1, game.stats().generatorsBought());
            assertEquals(2, game.stats().particleUpgradesBought());
            assertEquals(cost.add(generator), game.stats().particlesSpent());
            assertValue(10, game.stats().particlesCreated());
        }

        @Test
        void leRecordDeProductionEstReleveALaLecture() {
            Game game = newGame();
            assertValue(BASE, game.stats().bestProduction());
            game.state().setLevel("gen", 2);
            assertValue(3 * BASE, game.stats().bestProduction());
            game.state().setLevel("gen", 0);                  // la production retombe, pas le record
            assertValue(3 * BASE, game.stats().bestProduction());
        }

        @Test
        void chaqueFusionNoteSaDureeEtSesAtomes() {
            Game game = gameReadyToFuse();
            game.tick(30 - game.stats().runTime());           // la partie dure 30 s en tout
            assertTrue(game.fuse());
            GameStats stats = game.stats();
            assertEquals(1, stats.fusions());
            assertEquals(1, stats.runFusions());
            assertValue(1, stats.atomsCreated());
            assertValue(1, stats.bestAtomsPerFusion());
            assertEquals(30, stats.lastFusionTime(), 1e-9);
            assertEquals(30, stats.fastestFusionTime(), 1e-9);
            assertValue(6 * BASE, stats.bestProduction());    // trois générateurs à vitesse doublée, relevée avant la remise à zéro

            fuseAgain(game);                                  // tout de suite : une partie de zéro seconde
            assertEquals(2, stats.fusions());
            assertEquals(0, stats.lastFusionTime(), 1e-9);
            assertEquals(30, stats.fastestFusionTime(), 1e-9); // une durée nulle n'est pas un record (0.65)
            game.tick(50);
            fuseAgain(game);
            assertEquals(50, stats.lastFusionTime(), 1e-9);
            assertEquals(30, stats.fastestFusionTime(), 1e-9); // le record reste
            assertValue(3, stats.atomsCreated());
        }

        @Test
        void lesAtomesDepensesSontComptesQuelQueSoitLAchat() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(100));
            game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
            assertTrue(game.buy("keep"));                     // 1 atome
            assertEquals(1, game.stats().atomUpgradesBought());
            assertTrue(game.buyAutomation("autoSlow"));       // 1 atome
            assertTrue(game.speedUpAutomation("autoSlow"));   // 2 atomes
            assertValue(4, game.stats().atomsSpent());
            BigNum synthesis = game.synthesisCost();
            assertFalse(game.synthesize().isEmpty());
            assertValue(4 + synthesis.toDouble(), game.stats().atomsSpent());
            assertEquals(1, game.stats().atomUpgradesBought());   // seules les améliorations comptent ici
        }

        @Test
        void lesSynthesesEtLeursElementsSontComptes() {
            Game game = newGame();
            unlock(game);
            for (int synthesis = 0; synthesis < 3; synthesis++) {
                game.state().setAtoms(Game.MAX_ATOMS);
                game.synthesize();
            }
            assertEquals(3, game.stats().syntheses());
            assertEquals(3, game.stats().elementsObtained());  // aucune chance de tirage double au départ
            assertEquals(0, game.stats().doubleDraws());
            game.state().setElementCount(35, 100);             // brome : tirage double assuré
            game.state().setAtoms(Game.MAX_ATOMS);
            assertEquals(2, game.synthesize().size());
            assertEquals(4, game.stats().syntheses());
            assertEquals(5, game.stats().elementsObtained());
            assertEquals(1, game.stats().doubleDraws());
        }

        @Test
        void lesActionsDesAutomatismesSontComptees() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(100));
            game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
            game.buyAutomation("autoSlow");                   // achète la vitesse toutes les 4 s
            game.state().setParticles(BigNum.of(1e9));
            game.tick(8.5);
            assertEquals(2, game.stats().automationActions());
            assertEquals(2, game.levelOf("speed"));
            assertEquals(2, game.stats().particleUpgradesBought());   // l'automatisme achète pour le joueur
            assertEquals(0, game.stats().darkAutomationActions());
        }

        @Test
        void seulLAppuiDuJoueurCompteCommeTempsDAppui() {
            Game game = gameReadyToFuse();
            game.state().setDarkMatter(BigNum.of(3));
            game.state().setExplosions(1);
            assertTrue(game.growDarkMatter(2.5));
            assertEquals(2.5, game.stats().holdTime(), 1e-12);
            game.state().setDarkLevel("dark_wave", 1);        // l'onde de fusion fait grossir sans appui
            BigNum size = game.state().darkMatterSize();
            assertTrue(game.fuse());
            assertTrue(game.state().darkMatterSize().gt(size));
            assertEquals(2.5, game.stats().holdTime(), 1e-12);
            game.state().setDarkLevel("dark_spontaneous", 1); // l'expansion spontanée non plus
            game.tick(10);
            assertEquals(2.5, game.stats().holdTime(), 1e-12);
        }

        @Test
        void lExplosionNoteSaDureeEtRemetLesCompteursDeLaPartieAZero() {
            Game game = gameReadyToFuse();
            game.tick(100 - game.stats().runTime());
            game.fuse();
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            BigNum created = game.stats().particlesCreated();
            assertTrue(created.gt(BigNum.ZERO));
            assertTrue(game.explode());
            GameStats stats = game.stats();
            assertEquals(100, stats.lastExplosionTime(), 1e-9);
            assertEquals(100, stats.fastestExplosionTime(), 1e-9);
            assertEquals(0, stats.runTime(), 1e-9);
            assertEquals(0, stats.runFusions());
            assertEquals(BigNum.ZERO, stats.runParticlesCreated());
            assertEquals(1, stats.fusions());                 // les compteurs du jeu entier restent
            assertEquals(created, stats.particlesCreated());
            assertValue(1, stats.atomsCreated());

            game.tick(250);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertEquals(250, stats.lastExplosionTime(), 1e-9);
            assertEquals(100, stats.fastestExplosionTime(), 1e-9);    // la plus rapide reste la première
        }
    }

    @Nested
    class HistoriqueEtDeblocages {

        private void fillTable(Game game) {
            GameTestSupport.fillTableToMax(game);
        }

        @Test
        void unReleveEstPrisAIntervallesReguliers() {
            Game game = unstartedGame();
            game.tick(60);
            assertTrue(game.stats().history().samples().isEmpty());     // rien avant le premier générateur
            game.start();
            game.tick(0.1);                                             // le premier relevé, tout de suite
            assertEquals(1, game.stats().history().samples().size());
            for (int i = 0; i < 102; i++) game.tick(0.1);               // dix secondes : deux relevés de plus
            List<StatsHistory.Sample> samples = game.stats().history().samples();
            assertEquals(3, samples.size());
            assertEquals(StatsHistory.FIRST_INTERVAL, samples.get(1).time() - samples.get(0).time(), 0.11);
            StatsHistory.Sample first = samples.get(0);
            assertEquals(Math.log10(BASE), first.production(), 1e-9);   // un générateur, à la vitesse de départ
            assertEquals(0, first.atomsPerFusion(), 1e-9);              // un atome par fusion
            assertTrue(Double.isNaN(first.fusionTime()));               // aucune fusion encore
            assertEquals(0, first.elementsShare());
            assertEquals(-15, first.darkMatterSize(), 1e-9);
        }

        @Test
        void lHistoriqueSuitLeJeu() {
            Game game = gameReadyToFuse();
            game.tick(30);
            game.fuse();
            game.state().setElementCount(18, 1);
            game.state().setElementCount(26, 3);
            game.tick(StatsHistory.FIRST_INTERVAL);
            List<StatsHistory.Sample> samples = game.stats().history().samples();
            StatsHistory.Sample last = samples.get(samples.size() - 1);
            assertEquals(30 + ONE_PARTICLE / 8, last.fusionTime(), 1e-9);   // gameReadyToFuse a déjà joué un instant
            assertEquals(2.0 / 118, last.elementsShare(), 1e-12);
            assertEquals(4.0 / Game.maxCopies(), last.copiesShare(), 1e-12);
            assertEquals(Math.log10(1.15), last.atomsPerFusion(), 1e-9);    // l'argon
        }

        @Test
        void unReleveGardeUneValeurParStatistique() {
            Game game = newGame();
            game.tick(StatsHistory.FIRST_INTERVAL);
            StatsHistory.Sample first = game.stats().history().samples().get(0);
            assertEquals(StatsHistory.Stat.values().length, first.values().length);
            assertEquals(1, first.value(StatsHistory.Stat.GENERATORS));
            assertEquals(0, first.value(StatsHistory.Stat.FUSIONS));
            assertEquals(0, first.value(StatsHistory.Stat.EXPLOSIONS));
            assertEquals(Math.log10(BASE), first.value(StatsHistory.Stat.SPEED), 1e-9);
            assertEquals(1, first.value(StatsHistory.Stat.FUSION_YIELD), 1e-9);
            // Ce qui n'a pas encore de sens n'a pas de valeur : la courbe ne commence qu'ensuite.
            assertTrue(Double.isNaN(first.value(StatsHistory.Stat.ATOMS_CREATED)));
            assertTrue(Double.isNaN(first.value(StatsHistory.Stat.DARK_GROWTH)));
            for (StatsHistory.Stat stat : StatsHistory.Stat.values()) {
                assertFalse(Double.isInfinite(first.value(stat)), stat::name);
            }
        }

        @Test
        void lesCompteursDuReleveSuiventLeJeu() {
            Game game = gameReadyToFuse();
            game.tick(30);
            game.fuse();
            game.state().setElementCount(18, 1);
            game.tick(StatsHistory.FIRST_INTERVAL);
            List<StatsHistory.Sample> samples = game.stats().history().samples();
            StatsHistory.Sample last = samples.get(samples.size() - 1);
            assertEquals(1, last.value(StatsHistory.Stat.FUSIONS));
            assertEquals(game.stats().atomsCreated().log10(), last.value(StatsHistory.Stat.ATOMS_CREATED), 1e-9);
            assertEquals(game.stats().particlesCreated().log10(), last.value(StatsHistory.Stat.PARTICLES_CREATED), 1e-9);
            assertEquals(game.synthesisCost().log10(), last.value(StatsHistory.Stat.SYNTHESIS_COST), 1e-9);
            assertEquals(Math.log10(game.elementMultiplier(ElementEffect.Stat.ATOMS)),
                    last.value(StatsHistory.Stat.ELEMENT_ATOMS), 1e-9);
        }

        @Test
        void leReleveGardeLeSommetDeCeQueLeJoueurAvaitEnMain() {
            Game game = gameReadyToFuse();
            game.tick(StatsHistory.FIRST_INTERVAL);
            List<StatsHistory.Sample> samples = game.stats().history().samples();
            StatsHistory.Sample before = samples.get(samples.size() - 1);
            // Sans remise à zéro, le relevé donne ce que le joueur a en main à cet instant.
            assertEquals(game.state().particles().log10(), before.value(StatsHistory.Stat.PARTICLES), 1e-9);
            assertTrue(Double.isNaN(before.value(StatsHistory.Stat.ATOMS)));           // pas encore d'atome
            assertTrue(Double.isNaN(before.value(StatsHistory.Stat.DARK_MATTER)));
            assertEquals(0, before.value(StatsHistory.Stat.ELEMENT_COPIES));

            // Une fusion reprend les particules : le relevé suivant garde le sommet d'avant, pas le creux d'après.
            double held = game.state().particles().log10();
            assertTrue(game.fuse());
            assertTrue(game.state().particles().log10() < held);
            game.tick(StatsHistory.FIRST_INTERVAL);
            samples = game.stats().history().samples();
            StatsHistory.Sample after = samples.get(samples.size() - 1);
            assertTrue(after.value(StatsHistory.Stat.PARTICLES) >= held - 1e-9);
            assertEquals(game.state().atoms().log10(), after.value(StatsHistory.Stat.ATOMS), 1e-9);
            // Le sommet ne sert qu'une fois : le relevé d'après retrouve la valeur de l'instant.
            game.tick(StatsHistory.FIRST_INTERVAL);
            samples = game.stats().history().samples();
            assertEquals(game.state().particles().log10(), samples.get(samples.size() - 1).value(StatsHistory.Stat.PARTICLES), 1e-9);

            // Seules les quantités qu'une remise à zéro fait retomber retiennent un sommet.
            assertTrue(StatsHistory.Stat.PARTICLES.peaks() && StatsHistory.Stat.ATOMS.peaks() && StatsHistory.Stat.DARK_MATTER.peaks()
                    && StatsHistory.Stat.ELEMENT_COPIES.peaks() && StatsHistory.Stat.PRODUCTION.peaks());
            assertFalse(StatsHistory.Stat.SPACE.peaks() || StatsHistory.Stat.MOLECULES.peaks() || StatsHistory.Stat.FUSIONS.peaks());
        }

        @Test
        void leDelaiDUnAutomatismeNEstReleveQueSIlEstPossede() {
            Game game = newGame();
            createEnoughAtoms(game);
            game.tick(StatsHistory.FIRST_INTERVAL);
            List<StatsHistory.Sample> samples = game.stats().history().samples();
            assertTrue(Double.isNaN(samples.get(samples.size() - 1).delay("autoSlow")));
            game.state().setAtoms(BigNum.of(50));
            assertTrue(game.buyAutomation("autoSlow"));
            game.tick(StatsHistory.FIRST_INTERVAL + 0.5);         // un peu plus : avec un automatisme, le temps avance par petits pas
            samples = game.stats().history().samples();
            assertEquals(game.automationInterval("autoSlow"), samples.get(samples.size() - 1).delay("autoSlow"), 1e-9);
            assertTrue(Double.isNaN(samples.get(samples.size() - 1).delay("autoGen")));
        }

        @Test
        void leReleveGardeLeSommetDeProductionDAvantLaFusion() {
            Game game = gameReadyToFuse();                        // trois générateurs à vitesse doublée
            game.tick(StatsHistory.FIRST_INTERVAL);
            assertTrue(game.fuse());                              // la production retombe à celle d'un générateur
            assertValue(BASE, game.productionPerSecond());
            game.tick(StatsHistory.FIRST_INTERVAL);
            List<StatsHistory.Sample> samples = game.stats().history().samples();
            assertEquals(Math.log10(6 * BASE), samples.get(samples.size() - 1).production(), 1e-9);
            game.tick(StatsHistory.FIRST_INTERVAL);               // plus de fusion depuis : le relevé suivant dit la production du moment
            samples = game.stats().history().samples();
            assertEquals(Math.log10(BASE), samples.get(samples.size() - 1).production(), 1e-9);
        }

        @Test
        void pleinIlGardeUnReleveSurDeuxEtEspaceLesSuivants() {
            Game game = newGame();
            double step = StatsHistory.FIRST_INTERVAL;
            for (int i = 0; i < StatsHistory.CAPACITY + 10; i++) game.tick(step);
            StatsHistory history = game.stats().history();
            assertEquals(2 * step, history.interval());
            assertTrue(history.samples().size() <= StatsHistory.CAPACITY / 2 + 6, () -> "taille " + history.samples().size());
            assertTrue(history.samples().size() >= StatsHistory.CAPACITY / 2);
            assertEquals(step, history.samples().get(0).time(), 1e-9);     // le tout premier relevé est toujours là
            double end = game.state().timePlayed();
            assertTrue(end - history.samples().get(history.samples().size() - 1).time() <= 2 * step);
            for (int i = 1; i < history.samples().size(); i++) {           // toujours dans l'ordre du temps
                assertTrue(history.samples().get(i).time() > history.samples().get(i - 1).time());
            }
        }

        @Test
        void lExplosionVideLHistoriqueDeLaPartiePasCeluiDuJeu() {
            Game game = newGame();
            game.tick(100);
            game.tick(100);
            int whole = game.stats().history().samples().size();
            assertEquals(whole, game.stats().runHistory().samples().size());
            fillTable(game);
            assertTrue(game.explode());
            assertTrue(game.stats().runHistory().samples().isEmpty());
            assertEquals(whole, game.stats().history().samples().size());
            game.tick(10);
            assertEquals(1, game.stats().runHistory().samples().size());
            assertEquals(10, game.stats().runHistory().samples().get(0).time(), 1e-9);  // compté depuis l'explosion
            assertEquals(210, game.stats().history().samples().get(whole).time(), 1e-9);
        }

        @Test
        void laDureeDeChaqueExplosionEstGardee() {
            Game game = newGame();
            for (double duration : new double[]{300, 120, 45}) {
                game.tick(duration);
                fillTable(game);
                assertTrue(game.explode());
            }
            assertEquals(List.of(300.0, 120.0, 45.0), game.stats().explosionTimes());
            assertEquals(3, game.stats().timedExplosions());
        }

        @Test
        void unAutomatismeDejaDebloqueResteConnuApresLExplosion() {
            Game game = newGame();
            assertFalse(game.wasAutomationEverAvailable("autoGen"));
            createEnoughAtoms(game);
            game.tick(0.1);
            assertTrue(game.wasAutomationEverAvailable("autoGen"));
            assertFalse(game.wasAutomationEverAvailable("autoSynth"));  // il lui faut un élément unique
            fillTable(game);
            game.tick(0.1);
            assertTrue(game.wasAutomationEverAvailable("autoSynth"));
            assertTrue(game.explode());
            assertFalse(game.isAutomationAvailable("autoGen"));         // reverrouillé…
            assertFalse(game.isAutomationAvailable("autoSynth"));
            assertTrue(game.wasAutomationEverAvailable("autoGen"));     // … mais pas oublié
            assertTrue(game.wasAutomationEverAvailable("autoSynth"));
            game.reset();                                               // recommencer de zéro efface tout
            assertFalse(game.wasAutomationEverAvailable("autoGen"));
            assertThrows(IllegalArgumentException.class, () -> game.wasAutomationEverAvailable("inconnu"));
        }
    }

    @Nested
    class RemiseAZero {

        @Test
        void toutEstEffaceJusquALaMatiereNoireEtAuxStatistiques() {
            Game game = gameReadyToFuse();
            game.fuse();
            game.state().setAtoms(BigNum.of(50));
            game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
            game.buy("keep");
            game.buyAutomation("autoSlow");
            game.state().setElementCount(18, 1);
            game.state().setDarkMatter(BigNum.of(7));
            game.state().setDarkMatterSpent(BigNum.of(2));
            game.state().setExplosions(3);
            game.state().setDarkLevel("dark_nucleus", 2);
            game.setDarkAutomationEnabled("dark_auto_start", true);
            game.growDarkMatter(100);

            game.reset();
            GameState state = game.state();
            assertFalse(game.isStarted());
            assertEquals(0, game.generatorCount());
            assertEquals(BigNum.ZERO, state.particles());
            assertEquals(BigNum.ZERO, state.atoms());
            assertEquals(BigNum.ZERO, state.totalAtoms());
            assertEquals(0, game.levelOf("keep"));
            assertFalse(game.ownsAutomation("autoSlow"));
            assertEquals(0, game.discoveredElements());
            assertEquals(BigNum.ZERO, state.darkMatter());
            assertEquals(BigNum.ZERO, game.darkMatterEarned());
            assertEquals(0, state.explosions());
            assertEquals(Game.DARK_MATTER_START_SIZE, state.darkMatterSize());
            assertEquals(0, game.darkLevelOf("dark_nucleus"));
            assertFalse(game.isDarkAutomationEnabled("dark_auto_start"));
            assertEquals(0, state.timePlayed());
            assertEquals(0, game.stats().fusions());
            assertEquals(0, game.stats().holdTime());
            assertValue(1, game.atomsPerFusion());            // plus aucun bonus d'élément

            game.start();                                     // et la partie redémarre normalement
            game.tick(10 * ONE_PARTICLE);
            assertValue(10, state.particles());
        }
    }

    @Nested
    class AmeliorationsPayeesEnMatiereNoire {

        /** Partie qui a {@code darkMatter} matières noires à dépenser, après une explosion. */
        private Game gameWithDarkMatter(int darkMatter) {
            Game game = gameReadyToFuse();
            game.state().setDarkMatter(BigNum.of(darkMatter));
            game.state().setExplosions(1);
            return game;
        }

        @Test
        void ellesSontSixEtHorsDeLArbre() {
            Game game = gameWithDarkMatter(1);
            List<DarkUpgrade> shop = game.darkUpgrades(DarkUpgrade.Branch.DARK_MATTER);
            assertEquals(6, shop.size());
            for (DarkUpgrade dark : shop) {
                assertFalse(dark.branch().inTree());
                assertEquals(null, dark.requires());
                assertTrue(game.isDarkAvailable(dark.id()));   // dès la première matière noire
            }
            assertEquals(19, game.darkUpgrades(DarkUpgrade.Branch.PARTICLES).size());   // six cases du seuil du Big Bang en 0.097
        }

        @Test
        void laMatiereNoireEstDepenseeMaisResteGagnee() {
            Game game = gameWithDarkMatter(5);
            assertValue(1, game.darkCostOf("dark_shop_atoms"));
            assertTrue(game.buyDark("dark_shop_atoms"));
            assertValue(4, game.state().darkMatter());
            assertValue(1, game.state().darkMatterSpent());
            assertValue(5, game.darkMatterEarned());
            assertEquals(1, game.darkLevelOf("dark_shop_atoms"));
            assertValue(2, game.darkCostOf("dark_shop_atoms"));   // le prix double à chaque niveau
            assertTrue(game.buyDark("dark_shop_atoms"));
            assertTrue(game.buyDark("dark_shop_cadence"));
            assertValue(1, game.state().darkMatter());
            assertFalse(game.canBuyDark("dark_shop_synthesis"));  // 2 matières noires, il n'en reste qu'une
            assertValue(5, game.darkMatterEarned());
        }

        @Test
        void sansMatiereNoireOnNAchetePas() {
            Game game = gameWithDarkMatter(0);
            assertFalse(game.buyDark("dark_shop_atoms"));
            Game before = gameReadyToFuse();                      // aucune explosion : rien n'est accessible
            before.state().setDarkMatter(BigNum.of(5));
            assertFalse(before.buyDark("dark_shop_atoms"));
        }

        @Test
        void uneSecondeDeProductionPaieLaBrancheDesParticules() {
            Game game = gameWithDarkMatter(3);                    // trois générateurs à vitesse doublée
            game.state().setParticles(BigNum.ZERO);
            assertValue(6 * BASE, game.darkBalance(DarkUpgrade.Branch.PARTICLES));
            game.state().setParticles(BigNum.of(19));
            assertValue(Math.max(19, 6 * BASE), game.darkBalance(DarkUpgrade.Branch.PARTICLES));   // les particules en main, si elles valent plus
            assertFalse(game.canBuyDark("dark_density"));         // 20 particules : ni l'un ni l'autre n'y suffit
            game.state().setParticles(BigNum.of(20));
            assertTrue(game.canBuyDark("dark_density"));
        }

        @Test
        void depenserNeRefermeRien() {
            Game game = gameWithDarkMatter(3);
            double momentum = game.darkMatterMomentum();
            assertTrue(game.isDarkAutomationUnlocked("dark_auto_start"));
            assertTrue(game.buyDark("dark_shop_synthesis"));      // 2 matières noires
            assertTrue(game.buyDark("dark_shop_atoms"));          // et la dernière : tout y passe
            assertValue(0, game.state().darkMatter());
            assertTrue(game.isDarkAutomationUnlocked("dark_auto_start"));
            assertEquals(momentum, game.darkMatterMomentum(), 1e-12);
            assertTrue(game.growDarkMatter(1));
        }

        @Test
        void noyauxSombresMultiplientLesAtomesSelonLaMatiereNoireGagnee() {
            Game game = gameWithDarkMatter(5);
            assertValue(1, game.atomsPerFusion());
            game.state().setDarkLevel("dark_shop_atoms", 1);
            assertValue(2, game.atomsPerFusion());                // +20 % × 5 matières noires
            game.state().setDarkLevel("dark_shop_atoms", 2);
            assertValue(3, game.atomsPerFusion());
            game.state().setDarkMatter(BigNum.of(10));
            assertValue(5, game.atomsPerFusion());
            game.state().setDarkMatter(BigNum.ZERO);              // dépensée ou pas, elle compte
            game.state().setDarkMatterSpent(BigNum.of(10));
            assertValue(5, game.atomsPerFusion());
            assertTrue(game.fuse());
            assertValue(5, game.state().atoms());
        }

        @Test
        void rouagesSombresAccelerentLesAutomatismes() {
            Game game = GameTestSupport.realGame();
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(5));
            assertEquals(Automation.DEFAULT_INTERVAL, game.automationInterval("auto_fusion"));
            game.state().setDarkLevel("dark_shop_cadence", 1);
            assertEquals(1.5, game.darkAutomationDivisor(), 1e-12);
            assertEquals(Automation.DEFAULT_INTERVAL / 1.5, game.automationInterval("auto_fusion"), 1e-12);
            game.state().setDarkLevel("dark_shop_cadence", 3);
            assertEquals(Automation.DEFAULT_INTERVAL / 2.5, game.automationInterval("auto_generator"), 1e-12);
        }

        @Test
        void syntheseSombreBaisseLePrixDeLaSynthese() {
            Game game = gameWithDarkMatter(5);
            game.state().setSynthesisCount(20);                   // prix au plafond
            assertValue(118, game.synthesisCost());
            game.state().setDarkLevel("dark_shop_synthesis", 2);  // diviseur 1 + 0,1 × 2 × 5
            assertValue(59, game.synthesisCost());
        }

        @Test
        void departLanceCompteDesAtomesCommeCreesDesLAchat() {
            Game game = gameWithDarkMatter(3);
            assertValue(0, game.darkHeadStartAtoms());
            assertTrue(game.buyDark("dark_shop_start"));          // 2 matières noires
            assertValue(15, game.darkHeadStartAtoms());           // 5 × 3 gagnées
            assertValue(15, game.state().totalAtoms());
            assertValue(0, game.state().atoms());                 // comptés comme créés, pas à dépenser
            assertFalse(game.isAutomationUnlocked());
            game.state().setTotalAtoms(BigNum.of(20));            // il n'enlève rien à qui a déjà plus
            game.state().setDarkLevel("dark_shop_start", 1);
            assertValue(20, game.state().totalAtoms());
        }

        @Test
        void departLanceOuvreLAutomatisationEtLeTableauApresLExplosion() {
            Game game = gameWithDarkMatter(9);
            game.state().setDarkLevel("dark_shop_start", 1);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertValue(Game.UNLOCK_TOTAL_ATOMS.toDouble(), game.darkHeadStartAtoms());   // 50, plafonné à 30
            assertValue(Game.UNLOCK_TOTAL_ATOMS.toDouble(), game.state().totalAtoms());
            assertTrue(game.isAutomationUnlocked());
            assertTrue(game.isPeriodicTableUnlocked());
            assertValue(0, game.state().atoms());
        }
    }

    @Nested
    class AutomatismesDeMatiereNoire {

        /** Partie démarrée avec {@code darkMatter} matière noire. */
        private Game gameWithDarkMatter(int darkMatter) {
            Game game = newGame();
            game.state().setDarkMatter(BigNum.of(darkMatter));
            game.state().setExplosions(1);
            return game;
        }

        @Test
        void ilsSeDebloquentAvecLaMatiereNoireSansLaDepenser() {
            Game game = gameWithDarkMatter(1);
            assertEquals(5, game.darkAutomations().size());
            for (DarkAutomation automation : game.darkAutomations()) {
                assertFalse(game.isDarkAutomationUnlocked(automation.id()));
                assertFalse(game.setDarkAutomationEnabled(automation.id(), true));
                assertFalse(game.isDarkAutomationEnabled(automation.id()));
            }
            game.state().setDarkMatter(BigNum.of(3));
            assertTrue(game.isDarkAutomationUnlocked("dark_auto_start"));
            assertFalse(game.isDarkAutomationUnlocked("dark_auto_atoms"));
            assertFalse(game.isDarkAutomationEnabled("dark_auto_start"));   // débloqué, mais coupé au départ
            assertTrue(game.setDarkAutomationEnabled("dark_auto_start", true));
            assertTrue(game.isDarkAutomationEnabled("dark_auto_start"));
            assertValue(3, game.state().darkMatter());
            game.state().setDarkMatter(BigNum.of(14));
            for (DarkAutomation automation : game.darkAutomations()) {
                // Celui qui rachète l'arbre attend un premier Big Bang.
                assertEquals(automation.kind() != DarkAutomation.Kind.DARK_TREE, game.isDarkAutomationUnlocked(automation.id()), automation.id());
            }
            GameTestSupport.bangs(game, 1);
            assertTrue(game.isDarkAutomationUnlocked("dark_auto_tree"));
            GameTestSupport.bangs(game, 0);
            assertThrows(IllegalArgumentException.class, () -> game.isDarkAutomationUnlocked("inconnu"));
        }

        @Test
        void lesAutomatismesOrdinairesSontOffertsDesLeDebutDeLaPartie() {
            Game game = gameWithDarkMatter(3);
            assertFalse(game.isAutomationUnlocked());         // aucun atome créé : rien n'est débloqué
            game.setDarkAutomationEnabled("dark_auto_start", true);
            game.tick(0.9);
            assertFalse(game.ownsAutomation("autoSpeed"));    // pas encore une seconde
            game.tick(0.2);
            assertTrue(game.ownsAutomation("autoSpeed"));     // un par seconde, dans l'ordre du catalogue
            assertFalse(game.ownsAutomation("autoGen"));
            game.tick(3);
            for (String id : List.of("autoSpeed", "autoGen", "autoFusion", "autoSlow")) {
                assertTrue(game.ownsAutomation(id), () -> id);
                assertTrue(game.isAutomationEnabled(id), () -> id);       // offerts en marche
                assertTrue(game.isAutomationAvailable(id), () -> id);
            }
            assertFalse(game.ownsAutomation("autoSynth"));    // la synthèse automatique attend son heure
            assertTrue(game.isAutomationUnlocked());
            assertValue(0, game.state().atoms());             // rien n'a été payé
            assertValue(0, game.stats().atomsSpent());
        }

        @Test
        void ceSontLesAutomatismesOrdinairesQuiJouentPasUnDouble() {
            Game game = gameWithDarkMatter(3);
            game.setDarkAutomationEnabled("dark_auto_start", true);
            game.tick(4.5);                                   // les quatre sont offerts
            game.state().setParticles(BigNum.of(1e9));
            game.tick(1);                                     // ils achètent les générateurs et fusionnent tout seuls
            assertTrue(game.state().totalAtoms().gte(BigNum.ONE));
            assertTrue(game.stats().automationActions() > 0);
            // L'automatisme de matière noire n'a fait qu'offrir : quatre actions, une par automatisme.
            assertEquals(4, game.stats().darkAutomationActions());
            game.setAutomationEnabled("autoGen", false);      // coupé, un automatisme offert le reste : personne ne joue à sa place
            game.setAutomationEnabled("autoFusion", false);
            int generators = game.generatorCount();
            game.state().setParticles(BigNum.of(1e9));
            game.tick(5);
            assertEquals(generators, game.generatorCount());
            assertEquals(4, game.stats().darkAutomationActions());
        }

        @Test
        void laSyntheseAutomatiqueEstOfferteUneFoisLesAutresAuMaximumEtLeTableauOuvert() {
            Game game = gameWithDarkMatter(3);
            game.setDarkAutomationEnabled("dark_auto_start", true);
            game.tick(4.5);
            createEnoughAtoms(game);                          // tableau ouvert, mais « autoSlow » n'est pas à sa cadence maximale
            game.state().setAtoms(BigNum.of(50));
            game.tick(3);
            assertFalse(game.ownsAutomation("autoSynth"));
            assertEquals(0, game.state().synthesisCount());   // les atomes du joueur ne sont pas dépensés dans son dos
            assertTrue(game.speedUpAutomation("autoSlow"));
            assertTrue(game.speedUpAutomation("autoSlow"));   // tous au maximum
            game.setAutomationEnabled("autoSlow", false);
            game.tick(1.05);
            assertTrue(game.ownsAutomation("autoSynth"));     // offerte sans élément unique
            assertFalse(game.isSynthesisAutomationUnlocked());
            assertTrue(game.isAutomationAvailable("autoSynth"));
            game.tick(2.05);                                  // et elle synthétise, comme si elle avait été achetée
            assertEquals(1, game.state().synthesisCount());
        }

        @Test
        void sansTableauOuvertLaSyntheseAutomatiqueNEstPasOfferte() {
            Game game = gameWithDarkMatter(3);
            game.setDarkAutomationEnabled("dark_auto_start", true);
            game.tick(4.5);
            game.state().setAtoms(BigNum.of(50));
            game.speedUpAutomation("autoSlow");
            game.speedUpAutomation("autoSlow");
            game.state().setTotalAtoms(BigNum.ZERO);          // pas assez d'atomes créés : le tableau est fermé
            game.tick(3);
            assertFalse(game.isPeriodicTableUnlocked());
            assertFalse(game.ownsAutomation("autoSynth"));
        }

        @Test
        void ilsSontRedonnesApresChaqueExplosion() {
            Game game = gameWithDarkMatter(3);
            game.setDarkAutomationEnabled("dark_auto_start", true);
            game.tick(4.5);
            game.state().setAtoms(BigNum.of(10));
            game.speedUpAutomation("autoSlow");
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertFalse(game.ownsAutomation("autoSpeed"));    // l'explosion les a détruits…
            game.tick(4.5);
            assertTrue(game.ownsAutomation("autoSpeed"));     // … ils reviennent, sans leur cadence
            assertTrue(game.ownsAutomation("autoSlow"));
            assertEquals(0, game.automationSpeedLevel("autoSlow"));
        }

        @Test
        void ilNOffrePasCeQueLeJoueurPossedeDeja() {
            Game game = gameWithDarkMatter(3);
            createEnoughAtoms(game);
            game.state().setAtoms(BigNum.of(10));
            assertTrue(game.buyAutomation("autoGen"));
            game.setAutomationEnabled("autoGen", false);      // acheté puis coupé par le joueur
            game.setDarkAutomationEnabled("dark_auto_start", true);
            game.tick(4.5);
            assertFalse(game.isAutomationEnabled("autoGen")); // il n'y touche pas
            assertTrue(game.ownsAutomation("autoSpeed"));
            assertEquals(3, game.stats().darkAutomationActions());
        }

        @Test
        void lesAmeliorationsEnAtomesSAchetentDeLaMoinsChereALaPlusChere() {
            Game game = gameWithDarkMatter(5);
            game.state().setAtoms(BigNum.of(4));
            game.setDarkAutomationEnabled("dark_auto_atoms", true);
            game.tick(1.05);
            assertValue(3, game.state().atoms());             // une seule action : une amélioration à 1 atome
            game.tick(10);
            assertEquals(1, game.levelOf("double"));          // quatre achats à 1 atome, dans l'ordre du catalogue
            assertEquals(1, game.levelOf("patience"));
            assertEquals(2, game.levelOf("boost"));           // son deuxième niveau coûte encore 1 atome
            assertValue(0, game.state().atoms());             // il a tout dépensé, les moins chères d'abord
            assertEquals(0, game.levelOf("mass"));            // 2 atomes : elle passait après celles à 1 atome
        }

        @Test
        void lesAutomatismesOrdinairesSAchetentTousSeuls() {
            Game game = gameWithDarkMatter(8);
            game.state().setAtoms(BigNum.of(100));
            game.buy("keep");
            createEnoughAtoms(game);
            game.setDarkAutomationEnabled("dark_auto_machines", true);
            game.tick(1.05);
            assertTrue(game.ownsAutomation("autoSlow"));      // le moins cher : 1 atome
            game.tick(20);
            for (Automation automation : game.automations()) {
                if (automation.kind() == Automation.Kind.SYNTHESIS) continue;   // elle attend un élément unique
                assertTrue(game.ownsAutomation(automation.id()), () -> automation.name());
                assertTrue(game.isAutomationMaxed(automation.id()));
            }
            assertTrue(game.isPeriodicTableUnlocked());
        }

        @Test
        void sansPersistanceIlNAchetePasDAutomatisme() {
            Game game = gameWithDarkMatter(8);
            game.state().setAtoms(BigNum.of(100));
            game.setDarkAutomationEnabled("dark_auto_machines", true);
            game.tick(5);
            assertValue(100, game.state().atoms());
        }

        @Test
        void lExplosionAutomatiqueAttendUnTableauComplet() {
            Game game = gameWithDarkMatter(14);
            game.setDarkAutomationEnabled("dark_auto_explosion", true);
            game.tick(5);
            assertEquals(1, game.state().explosions());       // rien à faire exploser
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            game.tick(1.05);
            assertEquals(2, game.state().explosions());
            assertValue(15, game.state().darkMatter());
            assertEquals(0, game.discoveredElements());
            assertTrue(game.isDarkAutomationEnabled("dark_auto_explosion"));   // l'explosion ne le coupe pas
        }

        @Test
        void coupeIlNeFaitRien() {
            Game game = gameWithDarkMatter(14);
            game.setDarkAutomationEnabled("dark_auto_start", true);
            game.setDarkAutomationEnabled("dark_auto_start", false);
            game.tick(10);
            assertFalse(game.ownsAutomation("autoSpeed"));
            assertFalse(game.isAutomationUnlocked());
        }

        @Test
        void automatismeDeMatiereNoireInvalideRefuse() {
            assertThrows(IllegalArgumentException.class,
                    () -> new DarkAutomation("a", "A", DarkAutomation.Kind.EXPLOSION, BigNum.ONE, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> new DarkAutomation(" ", "A", DarkAutomation.Kind.EXPLOSION, BigNum.ONE, 1));
        }
    }
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameTest {

    /** Vitesse de départ d'un point de création, en particules par seconde. */
    private static final double BASE = Game.BASE_SPEED.toDouble();

    /** Temps nécessaire pour former une particule au départ, en secondes. */
    private static final double ONE_PARTICLE = 1 / BASE;

    // Catalogue fixe pour les tests : ils ne cassent pas quand le vrai catalogue change.
    private static final Upgrade SPEED = new Upgrade("speed", "Vitesse", Resource.PARTICLES,
            BigNum.of(10), 1.5, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(2));
    private static final Upgrade GENERATOR = new Upgrade("gen", "Point de création", Resource.PARTICLES,
            BigNum.of(100), 10, 2, new Effect.AddGenerator());
    private static final Upgrade HUGE = new Upgrade("huge", "Énorme", Resource.PARTICLES,
            BigNum.of(1), 2, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(1e30));

    // Améliorations payées en atomes.
    private static final Upgrade DOUBLE = new Upgrade("double", "Dédoublement", Resource.ATOMS,
            BigNum.of(1), 3, Upgrade.NO_LIMIT, new Effect.MultiplyParticles(2));
    private static final Upgrade MASS = new Upgrade("mass", "Masse", Resource.ATOMS,
            BigNum.of(2), 1, 1, new Effect.MultiplyByAtoms(0.5));
    private static final Upgrade PATIENCE = new Upgrade("patience", "Patience", Resource.ATOMS,
            BigNum.of(1), 1, 1, new Effect.MultiplyByRunTime(1));

    private static final Upgrade BOOST = new Upgrade("boost", "Catalyseur", Resource.ATOMS,
            BigNum.of(1), 1, 3, new Effect.StrengthenSpeed(0.5));
    private static final Upgrade DISCOUNT = new Upgrade("discount", "Compression", Resource.ATOMS,
            BigNum.of(1), 1, 3, new Effect.DiscountGenerators(0.5));
    private static final Upgrade KEEP = new Upgrade("keep", "Persistance", Resource.ATOMS,
            BigNum.of(1), 1, 1, new Effect.KeepUpgradesOnFusion());

    // Automatismes, payés en atomes. Les trois premiers sont au délai minimal (0,1 s) dès l'achat,
    // sans niveau de cadence à acheter, pour tester les achats eux-mêmes.
    private static final Automation AUTO_SPEED =
            Automation.buying("autoSpeed", "Vitesse", BigNum.of(2), "speed").withCadence(0, 0, BigNum.ONE, 1);
    private static final Automation AUTO_GEN =
            Automation.buying("autoGen", "Générateur", BigNum.of(3), "gen").withCadence(0, 0, BigNum.ONE, 1);
    private static final Automation AUTO_FUSION =
            Automation.fusing("autoFusion", "Fusion", BigNum.of(5)).withCadence(0, 0, BigNum.ONE, 1);
    /** Une action toutes les 4 s, puis 2 s, puis 1 s au maximum ; la cadence coûte 2 puis 6 atomes. */
    private static final Automation AUTO_SLOW =
            Automation.buying("autoSlow", "Lent", BigNum.of(1), "speed").withCadence(4, 2, BigNum.of(2), 3);
    /** Synthèse automatique, une action toutes les 2 s. */
    private static final Automation AUTO_SYNTH =
            Automation.synthesizing("autoSynth", "Synthèse", BigNum.of(4)).withCadence(2, 0, BigNum.ONE, 1);

    /** Partie pas encore démarrée : aucun générateur. */
    private static Game unstartedGame() {
        return new Game(new GameState(),
                List.of(SPEED, GENERATOR, HUGE, DOUBLE, MASS, PATIENCE, BOOST, DISCOUNT, KEEP),
                List.of(AUTO_SPEED, AUTO_GEN, AUTO_FUSION, AUTO_SLOW, AUTO_SYNTH), new Random(42));
    }

    /** Partie démarrée : le premier générateur est créé. */
    private static Game newGame() {
        Game game = unstartedGame();
        game.start();
        return game;
    }

    private static Game gameWithParticles(double particles) {
        Game game = newGame();
        game.state().setParticles(BigNum.of(particles));
        return game;
    }

    private static void assertValue(double expected, BigNum actual) {
        assertEquals(expected, actual.toDouble(), Math.abs(expected) * 1e-9 + 1e-9);
    }

    /** Matière possédée plus particules en cours de formation. */
    private static double total(Game game) {
        double total = game.state().particles().toDouble();
        for (double formation : game.state().formations()) total += formation;
        return total;
    }

    @Nested
    class Demarrage {

        @Test
        void unePartieNeuveNAAucunGenerateur() {
            Game game = unstartedGame();
            assertFalse(game.isStarted());
            assertEquals(0, game.generatorCount());
            assertEquals(BigNum.ZERO, game.productionPerSecond());
        }

        @Test
        void rienNeSeCreeAvantLeDemarrage() {
            Game game = unstartedGame();
            game.tick(3600);
            assertEquals(BigNum.ZERO, game.state().particles());
            assertTrue(game.state().formations().isEmpty());
        }

        @Test
        void aucunAchatAvantLeDemarrage() {
            Game game = unstartedGame();
            game.state().setParticles(BigNum.of(1e6));
            assertFalse(game.canBuy("speed"));
            assertFalse(game.buy("gen"));
            assertEquals(0, game.levelOf("gen"));
        }

        @Test
        void demarrerCreeLePremierGenerateur() {
            Game game = unstartedGame();
            game.start();
            assertTrue(game.isStarted());
            assertEquals(1, game.generatorCount());
            game.tick(ONE_PARTICLE);
            assertValue(1, game.state().particles());
        }

        @Test
        void demarrerDeuxFoisNeChangeRien() {
            Game game = newGame();
            game.tick(ONE_PARTICLE / 2);
            game.start();
            assertEquals(1, game.generatorCount());
            assertEquals(0.5, game.state().formation(0), 1e-9);
        }
    }

    @Nested
    class Temps {

        @Test
        void unePartieNeuveCommenceAZero() {
            Game game = newGame();
            assertEquals(BigNum.ZERO, game.state().particles());
            assertEquals(0, game.state().formation(0));
            assertEquals(0, game.state().timePlayed());
            assertEquals(1, game.generatorCount());
            assertValue(BASE, game.speed());
            assertValue(BASE, game.productionPerSecond());
        }

        @Test
        void uneParticuleSeFormeProgressivement() {
            Game game = newGame();
            game.tick(ONE_PARTICLE / 2);
            assertEquals(BigNum.ZERO, game.state().particles());
            assertEquals(0.5, game.state().formation(0), 1e-9);

            game.tick(ONE_PARTICLE / 2);
            assertValue(1, game.state().particles());
            assertEquals(0, game.state().formation(0), 1e-9);
        }

        @Test
        void laMatiereResteEnParticulesEntieres() {
            Game game = newGame();
            game.tick(ONE_PARTICLE * 10.25);
            assertValue(10, game.state().particles());
            assertEquals(0.25, game.state().formation(0), 1e-9);
            assertEquals(ONE_PARTICLE * 10.25, game.state().timePlayed(), 1e-9);
        }

        @Test
        void unTickDeZeroNeChangeRien() {
            Game game = gameWithParticles(42);
            game.tick(0);
            assertValue(42, game.state().particles());
            assertEquals(0, game.state().formation(0));
        }

        @Test
        void dureeInvalideRefusee() {
            Game game = newGame();
            assertThrows(IllegalArgumentException.class, () -> game.tick(-1));
            assertThrows(IllegalArgumentException.class, () -> game.tick(Double.NaN));
            assertThrows(IllegalArgumentException.class, () -> game.tick(Double.POSITIVE_INFINITY));
        }

        @Test
        void unGrosTickVautPlusieursPetits() {
            Game small = gameWithParticles(100);
            small.buy("gen");
            for (int i = 0; i < 20 * 3600; i++) small.tick(0.05); // 1 h à 20 ticks/s
            Game big = gameWithParticles(100);
            big.buy("gen");
            big.tick(3600);                                        // 1 h hors-ligne
            assertEquals(total(big), total(small), 1e-6);
        }

        @Test
        void vitesseGigantesque() {
            Game game = gameWithParticles(1);
            game.buy("huge");   // vitesse × 1e30
            game.tick(2);
            assertEquals(30 + Math.log10(BASE * 2), game.state().particles().log10(), 1e-9);
        }
    }

    @Nested
    class Achats {

        @Test
        void acheterRetireLeCoutEtMonteLeNiveau() {
            Game game = gameWithParticles(25);
            assertTrue(game.buy("speed"));
            assertValue(15, game.state().particles());
            assertEquals(1, game.levelOf("speed"));
        }

        @Test
        void achatRefuseSansAssezDeMatiere() {
            Game game = gameWithParticles(9);
            assertFalse(game.canBuy("speed"));
            assertFalse(game.buy("speed"));
            assertValue(9, game.state().particles());
            assertEquals(0, game.levelOf("speed"));
        }

        @Test
        void achatPossibleAvecExactementLeCout() {
            Game game = gameWithParticles(10);
            assertTrue(game.canBuy("speed"));
            assertTrue(game.buy("speed"));
            assertEquals(BigNum.ZERO, game.state().particles());
        }

        @Test
        void acheterNeTouchePasALaParticuleEnCours() {
            Game game = gameWithParticles(10);
            game.state().setFormation(0, 0.4);
            game.buy("speed");
            assertEquals(0.4, game.state().formation(0));
        }

        @Test
        void leCoutAugmenteEtResteEntier() {
            Game game = gameWithParticles(1000);
            assertValue(10, game.costOf("speed"));
            game.buy("speed");
            assertValue(15, game.costOf("speed"));
            game.buy("speed");
            assertValue(23, game.costOf("speed")); // 22,5 arrondi à la particule supérieure
        }

        @Test
        void lesTresGrosCoutsNeSontPasArrondis() {
            Upgrade costly = new Upgrade("x", "X", Resource.PARTICLES, BigNum.of(1, 100), 2, Upgrade.NO_LIMIT, SPEED.effect());
            assertEquals(BigNum.of(1, 100), costly.costAt(0));
        }

        @Test
        void niveauMaximal() {
            Game game = gameWithParticles(1e9);
            assertFalse(game.isMaxed("gen"));
            assertTrue(game.buy("gen"));
            assertTrue(game.buy("gen"));
            assertTrue(game.isMaxed("gen"));

            BigNum before = game.state().particles();
            assertFalse(game.canBuy("gen"));
            assertFalse(game.buy("gen"));
            assertEquals(2, game.levelOf("gen"));
            assertEquals(before, game.state().particles());
        }

        @Test
        void sansLimiteOnPeutToujoursAcheter() {
            Game game = gameWithParticles(1e300);
            for (int i = 0; i < 50; i++) assertTrue(game.buy("speed"));
            assertFalse(game.isMaxed("speed"));
        }

        @Test
        void ameliorationInconnue() {
            Game game = newGame();
            assertThrows(IllegalArgumentException.class, () -> game.buy("inconnue"));
            assertThrows(IllegalArgumentException.class, () -> game.costOf("inconnue"));
        }

        @Test
        void catalogueAvecDoublonRefuse() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Game(new GameState(), List.of(SPEED, SPEED)));
        }
    }

    @Nested
    class Vitesse {

        @Test
        void chaqueNiveauMultiplieLaVitesse() {
            Game game = gameWithParticles(1000);
            game.buy("speed");
            game.buy("speed");
            assertValue(BASE * 2 * 2, game.speed());
            assertValue(BASE * 2 * 2, game.productionPerSecond());
        }

        @Test
        void laNouvelleVitesseSAppliqueAuTickSuivant() {
            Game game = gameWithParticles(10);
            game.buy("speed");   // matière : 0, vitesse : BASE × 2
            game.tick(ONE_PARTICLE);
            assertEquals(2, total(game), 1e-9);
        }
    }

    @Nested
    class Generateurs {

        @Test
        void chaqueNiveauAjouteUnGenerateur() {
            Game game = gameWithParticles(1e6);
            assertEquals(1, game.generatorCount());
            game.buy("gen");
            assertEquals(2, game.generatorCount());
            game.buy("gen");
            assertEquals(3, game.generatorCount());
            assertEquals(3, game.maxGeneratorCount());
        }

        @Test
        void laProductionEstLaSommeDesGenerateurs() {
            Game game = gameWithParticles(1e6);
            game.buy("gen");
            game.buy("speed");
            assertValue(BASE * 2, game.speed());
            assertValue(BASE * 2 * 2, game.productionPerSecond());
        }

        @Test
        void unNouveauGenerateurPartDeZeroEtAvanceEnParallele() {
            Game game = gameWithParticles(100);
            game.tick(ONE_PARTICLE / 2);   // le point de départ est à moitié
            game.buy("gen");
            assertEquals(0, game.state().formation(1));

            game.tick(ONE_PARTICLE / 4);
            assertEquals(0.75, game.state().formation(0), 1e-9);
            assertEquals(0.25, game.state().formation(1), 1e-9);
            assertValue(0, game.state().particles());

            game.tick(ONE_PARTICLE / 4);   // seul le point de départ termine sa particule
            assertEquals(0, game.state().formation(0), 1e-9);
            assertEquals(0.5, game.state().formation(1), 1e-9);
            assertValue(1, game.state().particles());
        }

        @Test
        void chaqueGenerateurCreeSesParticulesEntieres() {
            Game game = gameWithParticles(1100);
            game.buy("gen");
            game.buy("gen");               // 3 points, matière : 0
            game.tick(ONE_PARTICLE * 2);
            assertValue(6, game.state().particles());
        }

        @Test
        void unGenerateurPasEncoreAcheteNAvancePas() {
            Game game = newGame();
            game.tick(ONE_PARTICLE / 2);
            assertEquals(0, game.state().formation(1));
            assertEquals(1, game.state().formations().size());
        }
    }

    @Nested
    class Fusion {

        @Test
        void impossibleTantQuIlManqueDesGenerateurs() {
            Game game = gameWithParticles(1e9);
            game.buy("gen");
            assertFalse(game.canFuse());
            assertFalse(game.fuse());
            assertEquals(BigNum.ZERO, game.state().atoms());
            assertEquals(2, game.generatorCount());
            assertFalse(unstartedGame().canFuse());
        }

        @Test
        void possibleQuandTousLesGenerateursSontDebloques() {
            assertTrue(gameReadyToFuse().canFuse());
        }

        @Test
        void fusionnerDonneUnAtomeEtRepartDUnSeulGenerateur() {
            Game game = gameReadyToFuse();
            assertTrue(game.fuse());

            assertValue(1, game.state().atoms());
            assertValue(1, game.state().totalAtoms());
            assertTrue(game.isStarted());
            assertEquals(1, game.generatorCount());
            assertEquals(0, game.levelOf("gen"));
            assertEquals(0, game.levelOf("speed"));
            assertEquals(BigNum.ZERO, game.state().particles());
            assertTrue(game.state().formations().isEmpty());
            assertValue(BASE, game.speed());
            assertValue(10, game.costOf("speed"));
            assertFalse(game.canFuse());
        }

        @Test
        void unAtomeSeulNeChangeRienTantQuIlNEstPasDepense() {
            Game game = gameReadyToFuse();
            game.fuse();
            assertValue(1, game.particlesPerCreation());
            assertValue(BASE, game.productionPerSecond());
        }

        @Test
        void lesAtomesSeCumulentDeFusionEnFusion() {
            Game game = gameReadyToFuse();
            game.fuse();
            fuseAgain(game);
            assertValue(2, game.state().atoms());
            assertValue(2, game.state().totalAtoms());
        }

        @Test
        void leTempsDepuisLaFusionRepartDeZero() {
            Game game = gameReadyToFuse();
            game.tick(100);
            double played = game.state().timePlayed();
            assertTrue(game.state().timeSinceFusion() >= 100);

            game.fuse();
            assertEquals(0, game.state().timeSinceFusion());
            assertEquals(played, game.state().timePlayed());

            game.tick(30);
            assertEquals(30, game.state().timeSinceFusion(), 1e-9);
        }
    }

    @Nested
    class PlafondDAtomes {

        private static final double MAX = Game.MAX_ATOMS.toDouble();

        @Test
        void lePlafondEstLeNombreDElementsDuTableauPeriodique() {
            assertValue(118, Game.MAX_ATOMS);
        }

        @Test
        void auPlafondLaFusionEstBloquee() {
            Game game = gameReadyToFuse();
            game.state().setAtoms(BigNum.of(MAX));
            assertTrue(game.isAtomCapReached());
            assertTrue(game.hasAllGenerators());
            assertFalse(game.canFuse());
            assertFalse(game.fuse());
            assertValue(MAX, game.state().atoms());
            assertEquals(3, game.generatorCount());   // rien n'a été remis à zéro
        }

        @Test
        void justeSousLePlafondOnPeutEncoreFusionner() {
            Game game = gameReadyToFuse();
            game.state().setAtoms(BigNum.of(MAX - 1));
            assertFalse(game.isAtomCapReached());
            assertTrue(game.fuse());
            assertValue(MAX, game.state().atoms());
            assertTrue(game.isAtomCapReached());
        }

        @Test
        void depenserUnAtomeDebloqueLaFusion() {
            Game game = gameReadyToFuse();
            game.state().setAtoms(BigNum.of(MAX));
            assertFalse(game.canFuse());
            game.buy("double");                       // coûte 1 atome
            assertValue(MAX - 1, game.state().atoms());
            assertTrue(game.canFuse());
        }

        @Test
        void laFusionAutomatiqueSArreteAuPlafond() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(MAX - 2 + 1 + 3 + 5));
            game.buy("keep");
            game.buyAutomation("autoGen");
            game.buyAutomation("autoFusion");         // reste MAX − 2 atomes
            assertValue(MAX - 2, game.state().atoms());
            for (int i = 0; i < 10; i++) {
                game.state().setParticles(BigNum.of(1e9));
                game.tick(1);
            }
            assertValue(MAX, game.state().atoms());
            assertTrue(game.hasAllGenerators());      // les générateurs attendent, sans fusionner
        }
    }

    @Nested
    class AmeliorationsEnAtomes {

        /** Partie qui vient de fusionner {@code fusions} fois, sans avoir rien dépensé. */
        private Game gameWithAtoms(int fusions) {
            Game game = gameReadyToFuse();
            game.fuse();
            for (int i = 1; i < fusions; i++) fuseAgain(game);
            return game;
        }

        @Test
        void lesAmeliorationsSontTrieesParRessource() {
            Game game = newGame();
            assertEquals(List.of(SPEED, GENERATOR, HUGE), game.upgrades(Resource.PARTICLES));
            assertEquals(List.of(DOUBLE, MASS, PATIENCE, BOOST, DISCOUNT, KEEP), game.upgrades(Resource.ATOMS));
        }

        @Test
        void ellesSePaientEnAtomesPasEnParticules() {
            Game game = gameWithAtoms(1);
            game.state().setParticles(BigNum.of(500));
            assertTrue(game.buy("double"));
            assertEquals(BigNum.ZERO, game.state().atoms());
            assertValue(500, game.state().particles());
            assertEquals(1, game.levelOf("double"));
        }

        @Test
        void sansAtomeOnNePeutRienAcheter() {
            Game game = gameWithParticles(1e9);
            assertFalse(game.canBuy("double"));
            assertFalse(game.buy("double"));
            assertEquals(0, game.levelOf("double"));
        }

        @Test
        void depenserNeDiminuePasLeTotalCree() {
            Game game = gameWithAtoms(3);
            game.buy("mass");
            assertValue(1, game.state().atoms());
            assertValue(3, game.state().totalAtoms());
        }

        @Test
        void ellesSontConserveesApresUneFusion() {
            Game game = gameWithAtoms(1);
            game.buy("double");
            game.state().setParticles(BigNum.of(1e9));
            game.buy("speed");
            fuseAgain(game);
            assertEquals(1, game.levelOf("double"));
            assertEquals(0, game.levelOf("speed"));
        }

        @Test
        void dedoublementDoubleLesParticulesAChaqueNiveau() {
            Game game = gameWithAtoms(4);
            assertValue(1, game.costOf("double"));
            game.buy("double");
            assertValue(2, game.particlesPerCreation());
            assertValue(BASE * 2, game.productionPerSecond());
            game.tick(ONE_PARTICLE);
            assertValue(2, game.state().particles());

            assertValue(3, game.costOf("double"));
            game.buy("double");
            assertValue(4, game.particlesPerCreation());
            game.tick(ONE_PARTICLE);
            assertValue(6, game.state().particles());
        }

        @Test
        void masseMultiplieSelonLesAtomesCrees() {
            Game game = gameWithAtoms(2);
            assertValue(1, game.particlesPerCreation());
            game.buy("mass");                               // 1 + 0,5 × 2 atomes créés
            assertValue(2, game.particlesPerCreation());
            assertEquals(BigNum.ZERO, game.state().atoms());

            fuseAgain(game);                                // 3 atomes créés, même après dépense
            assertValue(2.5, game.particlesPerCreation());
            assertTrue(game.isMaxed("mass"));
            assertFalse(game.buy("mass"));
        }

        @Test
        void masseEstPlafonneeCommeLesAtomes() {
            Game game = gameWithAtoms(2);
            game.buy("mass");
            game.state().setTotalAtoms(Game.MAX_ATOMS);
            BigNum atCap = game.particlesPerCreation();       // 1 + 0,5 × 118
            assertValue(60, atCap);
            game.state().setTotalAtoms(BigNum.of(1e6));        // des milliers de fusions automatiques plus tard
            assertEquals(atCap, game.particlesPerCreation());
        }

        @Test
        void patienceGranditAvecLeTempsDepuisLaFusion() {
            Game game = gameWithAtoms(1);
            game.buy("patience");
            assertValue(1, game.particlesPerCreation());   // juste après la fusion : aucun bonus
            game.tick(60);                                  // 1 + √1 minute
            assertValue(2, game.particlesPerCreation());
            game.tick(180);                                 // 1 + √4 minutes
            assertValue(3, game.particlesPerCreation());

            game.state().setParticles(BigNum.of(1e9));
            fuseAgain(game);
            assertValue(1, game.particlesPerCreation());   // le bonus repart de zéro
        }

        @Test
        void lesBonusSeMultiplientEntreEux() {
            Game game = gameWithAtoms(4);
            game.buy("double");     // ×2
            game.buy("mass");       // ×(1 + 0,5 × 4) = ×3
            game.buy("patience");
            game.tick(60);          // ×2 après une minute
            assertValue(2 * 3 * 2, game.particlesPerCreation());
        }

        @Test
        void particlesMultiplierDonneLEffetAvantLAchat() {
            Game game = gameWithAtoms(2);
            assertValue(1, game.particlesMultiplier("mass", 0));
            assertValue(2, game.particlesMultiplier("mass", 1));
            assertValue(8, game.particlesMultiplier("double", 3));
            assertValue(1, game.particlesMultiplier("speed", 5));
        }

        @Test
        void unGrosTickVautPlusieursPetitsAvecPatience() {
            Game small = gameWithAtoms(1);
            small.buy("patience");
            for (int i = 0; i < 20 * 600; i++) small.tick(0.05);   // 10 min à 20 ticks/s
            Game big = gameWithAtoms(1);
            big.buy("patience");
            big.tick(600);                                          // 10 min hors-ligne
            assertEquals(total(small), total(big), total(small) * 0.01);
        }
    }

    @Nested
    class AmeliorationsDesAmeliorations {

        /** Partie démarrée avec des atomes et des particules à dépenser, sans rien d'acheté. */
        private Game richGame() {
            Game game = gameWithParticles(1e9);
            game.state().setAtoms(BigNum.of(10));
            game.state().setTotalAtoms(BigNum.of(10));
            return game;
        }

        @Test
        void catalyseurRenforceChaqueNiveauDeVitesse() {
            Game game = richGame();
            game.buy("speed");
            game.buy("speed");
            assertValue(BASE * 2 * 2, game.speed());

            game.buy("boost");                              // chaque niveau : ×2 devient ×2,5
            assertEquals(0.5, game.speedExtraPerLevel(), 1e-12);
            assertValue(BASE * 2.5 * 2.5, game.speed());

            game.buy("boost");                              // puis ×3
            assertValue(BASE * 3 * 3, game.speed());
        }

        @Test
        void catalyseurSansNiveauDeVitesseNeChangeRien() {
            Game game = richGame();
            game.buy("boost");
            assertValue(BASE, game.speed());
        }

        @Test
        void compressionBaisseLeCoutDesGenerateursSeulement() {
            Game game = richGame();
            assertValue(100, game.costOf("gen"));
            game.buy("discount");
            assertEquals(0.5, game.generatorCostFactor(), 1e-12);
            assertValue(50, game.costOf("gen"));
            assertValue(10, game.costOf("speed"));

            game.buy("gen");                                // le niveau suivant coûtait 1000
            assertValue(500, game.costOf("gen"));
        }

        @Test
        void leCoutReduitEstArrondiALUniteSuperieure() {
            Game game = richGame();
            game.buy("discount");
            game.buy("discount");
            game.buy("discount");                           // 100 × 0,5³ = 12,5
            assertValue(13, game.costOf("gen"));
            assertTrue(game.isMaxed("discount"));
        }

        @Test
        void onPaieBienLeCoutReduit() {
            Game game = richGame();
            game.buy("discount");
            game.state().setParticles(BigNum.of(50));
            assertTrue(game.buy("gen"));
            assertEquals(BigNum.ZERO, game.state().particles());
        }

        @Test
        void sansPersistanceLaFusionEffaceLaVitesse() {
            Game game = richGame();
            game.buy("speed");
            game.buy("gen");
            game.buy("gen");
            game.fuse();
            assertEquals(0, game.levelOf("speed"));
        }

        @Test
        void persistanceConserveLaVitesseMaisPasLesGenerateurs() {
            Game game = richGame();
            game.buy("keep");
            assertTrue(game.keepsUpgradesOnFusion());
            game.buy("speed");
            game.buy("speed");
            game.buy("gen");
            game.buy("gen");
            assertTrue(game.fuse());

            assertEquals(2, game.levelOf("speed"));
            assertValue(BASE * 2 * 2, game.speed());
            assertEquals(0, game.levelOf("gen"));
            assertEquals(1, game.generatorCount());
            assertEquals(BigNum.ZERO, game.state().particles());
            assertFalse(game.canFuse());
        }

        @Test
        void cesAmeliorationsNeMultiplientPasLesParticules() {
            Game game = richGame();
            game.buy("boost");
            game.buy("discount");
            game.buy("keep");
            assertValue(1, game.particlesPerCreation());
        }
    }

    @Nested
    class Automatisation {

        /** Partie avec la persistance achetée et {@code atoms} atomes à dépenser, sans particule. */
        private Game gameWithAutomation(int atoms) {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(atoms + 1));
            game.buy("keep");
            return game;
        }

        @Test
        void verrouilleeSansPersistance() {
            Game game = gameWithParticles(1e9);
            game.state().setAtoms(BigNum.of(100));
            assertFalse(game.isAutomationUnlocked());
            assertFalse(game.canBuyAutomation("autoSpeed"));
            assertFalse(game.buyAutomation("autoSpeed"));
            assertFalse(game.ownsAutomation("autoSpeed"));
            assertValue(100, game.state().atoms());
        }

        @Test
        void unAutomatismeSAcheteEnAtomes() {
            Game game = gameWithAutomation(5);
            assertTrue(game.isAutomationUnlocked());
            assertTrue(game.canBuyAutomation("autoSpeed"));
            assertTrue(game.buyAutomation("autoSpeed"));
            assertValue(3, game.state().atoms());
            assertTrue(game.ownsAutomation("autoSpeed"));
            assertTrue(game.isAutomationEnabled("autoSpeed"));   // mis en marche à l'achat
        }

        @Test
        void sansAssezDAtomesLAchatEstRefuse() {
            Game game = gameWithAutomation(2);
            assertFalse(game.canBuyAutomation("autoGen"));       // coûte 3
            assertFalse(game.buyAutomation("autoGen"));
            assertValue(2, game.state().atoms());
            assertFalse(game.ownsAutomation("autoGen"));
        }

        @Test
        void onNePaieQuUneFois() {
            Game game = gameWithAutomation(10);
            assertTrue(game.buyAutomation("autoSpeed"));
            assertFalse(game.canBuyAutomation("autoSpeed"));
            assertFalse(game.buyAutomation("autoSpeed"));
            assertValue(8, game.state().atoms());
        }

        @Test
        void unAutomatismeNonAcheteNePeutPasEtreActive() {
            Game game = gameWithAutomation(10);
            assertFalse(game.setAutomationEnabled("autoSpeed", true));
            assertFalse(game.isAutomationEnabled("autoSpeed"));
            game.state().setParticles(BigNum.of(1e9));
            game.tick(5);
            assertEquals(0, game.levelOf("speed"));
        }

        @Test
        void unAutomatismeAchetePeutEtreCoupePuisRelance() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoSpeed");
            assertTrue(game.setAutomationEnabled("autoSpeed", false));
            assertFalse(game.isAutomationEnabled("autoSpeed"));
            game.state().setParticles(BigNum.of(1e9));
            game.tick(5);
            assertEquals(0, game.levelOf("speed"));

            assertTrue(game.setAutomationEnabled("autoSpeed", true));
            game.tick(1);
            assertTrue(game.levelOf("speed") > 5);
        }

        @Test
        void acheteToutSeulDesQueCEstPossible() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoSpeed");
            game.tick(ONE_PARTICLE * 9);    // 9 particules : pas assez pour le premier niveau à 10
            assertEquals(0, game.levelOf("speed"));
            game.tick(ONE_PARTICLE + 0.3);  // la dixième arrive : achat dans les dixièmes de seconde suivants
            assertEquals(1, game.levelOf("speed"));
            assertEquals(BigNum.ZERO, game.state().particles());
        }

        @Test
        void neTouchePasAuxAutresAmeliorations() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoSpeed");
            game.state().setParticles(BigNum.of(1e9));
            game.tick(1);
            assertTrue(game.levelOf("speed") > 5);
            assertEquals(0, game.levelOf("gen"));
            assertEquals(0, game.levelOf("double"));
        }

        @Test
        void laFusionAutomatiqueCreeDesAtomesSansClic() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoGen");
            game.buyAutomation("autoFusion");                    // reste 10 − 3 − 5 = 2 atomes
            game.state().setParticles(BigNum.of(1e9));
            game.tick(1);                   // achète les deux générateurs, puis fusionne
            assertValue(3, game.state().atoms());
            assertEquals(1, game.generatorCount());
            assertEquals(BigNum.ZERO, game.state().particles());
        }

        @Test
        void sansFusionAutomatiqueLeJoueurGardeLaMain() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoGen");
            game.state().setParticles(BigNum.of(1e9));
            game.tick(1);
            assertTrue(game.canFuse());
            assertValue(7, game.state().atoms());
        }

        @Test
        void lesAutomatismesSontConservesApresUneFusion() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoGen");
            game.state().setParticles(BigNum.of(1e9));
            game.tick(1);
            game.fuse();
            assertTrue(game.ownsAutomation("autoGen"));
            assertTrue(game.isAutomationEnabled("autoGen"));
            assertEquals(0, game.levelOf("gen"));
        }

        @Test
        void leJeuTourneSeulAvecTousLesAutomatismes() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoSpeed");
            game.buyAutomation("autoGen");
            game.buyAutomation("autoFusion");                    // reste 0 atome
            for (int i = 0; i < 20 * 600; i++) game.tick(0.05); // 10 minutes sans aucune action
            assertTrue(game.state().atoms().gte(BigNum.of(3)),
                    "atomes créés en 10 minutes : " + game.state().atoms().format());
        }

        @Test
        void auDelaiMinimalDixAchatsParSeconde() {
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoSpeed");
            assertEquals(Game.MIN_AUTOMATION_INTERVAL, game.automationInterval("autoSpeed"));
            game.state().setParticles(BigNum.of(1, 30));
            game.tick(2.5);
            int level = game.levelOf("speed");
            assertTrue(level >= 24 && level <= 26, "achats en 2,5 s : " + level);
        }

        @Test
        void unGrosTickVautPlusieursPetits() {
            Game small = gameWithAutomation(10);
            small.buyAutomation("autoGen");
            for (int i = 0; i < 20 * 600; i++) small.tick(0.05);   // 10 min à 20 ticks/s
            Game big = gameWithAutomation(10);
            big.buyAutomation("autoGen");
            big.tick(600);                                          // 10 min hors-ligne
            assertEquals(1, small.levelOf("gen"));
            assertEquals(1, big.levelOf("gen"));
            assertEquals(total(small), total(big), total(small) * 0.02);
        }

        @Test
        void catalogueIncoherentRefuse() {
            assertThrows(IllegalArgumentException.class, () -> game("autoInconnu"));
            assertThrows(IllegalArgumentException.class, () -> new Game(new GameState(),
                    List.of(SPEED), List.of(AUTO_SPEED, AUTO_SPEED)));
            assertThrows(IllegalArgumentException.class, () -> new Game(new GameState(),
                    List.of(SPEED), List.of(AUTO_GEN)));             // vise « gen », absent du catalogue
            // un automatisme d'achat doit désigner une amélioration, les autres non
            assertThrows(IllegalArgumentException.class, () -> new Automation("x", "X",
                    Automation.Kind.UPGRADE, BigNum.ONE, null, 1, 0, BigNum.ONE, 1));
            assertThrows(IllegalArgumentException.class, () -> new Automation("x", "X",
                    Automation.Kind.FUSION, BigNum.ONE, "speed", 1, 0, BigNum.ONE, 1));
        }

        private void game(String automationId) {
            newGame().buyAutomation(automationId);
        }
    }

    @Nested
    class CadenceDesAutomatismes {

        /** Partie avec l'automatisme lent acheté, {@code atoms} atomes restants et beaucoup de particules. */
        private Game gameWithSlowAutomation(int atoms) {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(atoms + 2));
            game.buy("keep");
            game.buyAutomation("autoSlow");
            game.state().setParticles(BigNum.of(1e9));
            return game;
        }

        @Test
        void leDelaiSeDiviseParDeuxJusquAuPlafond() {
            assertEquals(4, AUTO_SLOW.intervalAt(0));
            assertEquals(2, AUTO_SLOW.intervalAt(1));
            assertEquals(1, AUTO_SLOW.intervalAt(2));
            assertEquals(1, AUTO_SLOW.intervalAt(7));     // au-delà du dernier niveau, plus rien ne change
        }

        @Test
        void lesReglagesParDefaut() {
            Automation automation = Automation.fusing("f", "Fusion", BigNum.of(1));
            assertEquals(4, automation.intervalAt(0));
            assertEquals(0.5, automation.intervalAt(3));
            assertEquals(0.5, automation.intervalAt(9));
            assertValue(4, automation.speedCostAt(0));
            assertValue(12, automation.speedCostAt(1));
            assertValue(36, automation.speedCostAt(2));
        }

        @Test
        void unSeulAchatParDelai() {
            Game game = gameWithSlowAutomation(0);
            assertEquals(4, game.automationInterval("autoSlow"));
            game.tick(3.8);
            assertEquals(0, game.levelOf("speed"));   // le délai n'est pas écoulé
            game.tick(0.3);
            assertEquals(1, game.levelOf("speed"));
            game.tick(4);
            assertEquals(2, game.levelOf("speed"));
            game.tick(12);                            // trois délais d'un coup
            assertEquals(5, game.levelOf("speed"));
        }

        @Test
        void unAutomatismePretAttendDePouvoirPayer() {
            Game game = gameWithSlowAutomation(0);
            game.state().setParticles(BigNum.ZERO);
            game.tick(30);                            // prêt depuis longtemps, mais sans particules
            assertEquals(0, game.levelOf("speed"));
            game.state().setParticles(BigNum.of(1e9));
            game.tick(0.1);                           // achète tout de suite…
            assertEquals(1, game.levelOf("speed"));
            game.tick(3);                             // …puis le délai repart
            assertEquals(1, game.levelOf("speed"));
            game.tick(1.2);
            assertEquals(2, game.levelOf("speed"));
        }

        @Test
        void accelererCouteDesAtomesEtReduitLeDelai() {
            Game game = gameWithSlowAutomation(10);
            assertFalse(game.isAutomationMaxed("autoSlow"));
            assertValue(2, game.automationSpeedCost("autoSlow"));
            assertTrue(game.canSpeedUpAutomation("autoSlow"));
            assertTrue(game.speedUpAutomation("autoSlow"));
            assertValue(8, game.state().atoms());
            assertEquals(1, game.automationSpeedLevel("autoSlow"));
            assertEquals(2, game.automationInterval("autoSlow"));

            game.tick(4.2);
            assertEquals(2, game.levelOf("speed"));   // deux achats en 4 s au lieu d'un
        }

        @Test
        void auDernierNiveauLeDelaiNeDisparaitPas() {
            Game game = gameWithSlowAutomation(10);
            game.speedUpAutomation("autoSlow");       // 2 atomes
            assertValue(6, game.automationSpeedCost("autoSlow"));
            game.speedUpAutomation("autoSlow");       // 6 atomes
            assertValue(2, game.state().atoms());
            assertTrue(game.isAutomationMaxed("autoSlow"));
            assertEquals(1, game.automationInterval("autoSlow"));   // plafonné, pas instantané
            assertFalse(game.canSpeedUpAutomation("autoSlow"));
            assertFalse(game.speedUpAutomation("autoSlow"));
            assertValue(2, game.state().atoms());

            game.tick(3.05);
            assertEquals(3, game.levelOf("speed"));   // un achat par seconde, pas plus
        }

        @Test
        void onNAccelerePasCeQuOnNePossedePas() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(50));
            game.buy("keep");
            assertFalse(game.canSpeedUpAutomation("autoSlow"));
            assertFalse(game.speedUpAutomation("autoSlow"));
            assertEquals(0, game.automationSpeedLevel("autoSlow"));
        }

        @Test
        void sansAssezDAtomesOnNAccelerePas() {
            Game game = gameWithSlowAutomation(1);
            assertFalse(game.canSpeedUpAutomation("autoSlow"));
            assertFalse(game.speedUpAutomation("autoSlow"));
            assertValue(1, game.state().atoms());
        }

        @Test
        void lesElementsAccelerentAuDelaDuPlafondMaisJamaisSousLeMinimum() {
            Game game = gameWithSlowAutomation(10);
            game.speedUpAutomation("autoSlow");
            game.speedUpAutomation("autoSlow");       // plafond des atomes : 1 s
            game.state().setElementCount(36, 1);      // krypton : tous les automatismes deux fois plus rapides
            assertEquals(0.5, game.automationInterval("autoSlow"), 1e-12);
            game.state().setElementCount(87, 1_000_000);   // une quantité absurde de francium
            assertEquals(Game.MIN_AUTOMATION_INTERVAL, game.automationInterval("autoSlow"));
        }
    }

    @Nested
    class RythmeDesAutomatismes {

        /** Partie où la fusion et l'achat de générateurs sont automatiques, au délai minimal. */
        private Game fullyAutomatedGame() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(1 + 3 + 5));
            game.buy("keep");
            game.buyAutomation("autoGen");
            game.buyAutomation("autoFusion");
            return game;
        }

        @Test
        void auPlusDixActionsParSecondeParAutomatisme() {
            // Deux générateurs à racheter par partie, un achat par dixième de seconde :
            // même avec des particules illimitées, cinq fusions par seconde au mieux.
            Game game = fullyAutomatedGame();
            for (int i = 0; i < 100; i++) {          // 5 secondes à 20 ticks/s
                game.state().setParticles(BigNum.of(1e9));
                game.tick(0.05);
            }
            double atoms = game.state().atoms().toDouble();
            assertTrue(atoms >= 20 && atoms <= 26, "fusions en 5 s : " + atoms);
        }

        @Test
        void leRythmeNeDependPasDuNombreDImagesParSeconde() {
            Game slow = fullyAutomatedGame();        // 20 images par seconde
            Game fast = fullyAutomatedGame();        // 200 images par seconde
            for (int i = 0; i < 100; i++) {
                slow.state().setParticles(BigNum.of(1e9));
                slow.tick(0.05);
            }
            for (int i = 0; i < 1000; i++) {
                fast.state().setParticles(BigNum.of(1e9));
                fast.tick(0.005);
            }
            assertEquals(slow.state().atoms().toDouble(), fast.state().atoms().toDouble(), 3);
        }
    }

    @Nested
    class TableauPeriodique {

        @Test
        void leTableauCompte118ElementsNumerotesDansLOrdre() {
            assertEquals(118, PeriodicTable.ELEMENTS.size());
            for (int number = 1; number <= 118; number++) {
                assertEquals(number, PeriodicTable.element(number).number());
                assertTrue(PeriodicTable.element(number).effect() != null);
            }
            assertEquals(118, PeriodicTable.ELEMENTS.stream().map(Element::symbol).distinct().count());
            assertEquals(118, PeriodicTable.ELEMENTS.stream().map(Element::name).distinct().count());
            assertThrows(IllegalArgumentException.class, () -> PeriodicTable.element(0));
            assertThrows(IllegalArgumentException.class, () -> PeriodicTable.element(119));
        }

        @Test
        void quelquesElementsConnus() {
            assertEquals("Hydrogène", PeriodicTable.element(1).name());
            assertEquals("Fe", PeriodicTable.element(26).symbol());
            assertEquals(ElementCategory.NONMETAL, PeriodicTable.element(1).category());
            assertEquals(ElementCategory.TRANSITION_METAL, PeriodicTable.element(26).category());
            assertEquals(ElementCategory.NOBLE_GAS, PeriodicTable.element(2).category());
            assertEquals(ElementCategory.ALKALI_METAL, PeriodicTable.element(11).category());
            assertEquals(ElementCategory.HALOGEN, PeriodicTable.element(17).category());
            assertEquals(ElementCategory.LANTHANIDE, PeriodicTable.element(64).category());
            assertEquals(ElementCategory.ACTINIDE, PeriodicTable.element(92).category());
            assertEquals("Og", PeriodicTable.element(118).symbol());
        }

        @Test
        void chaqueFamilleASonNombreDElements() {
            Map<ElementCategory, Integer> expected = new EnumMap<>(ElementCategory.class);
            expected.put(ElementCategory.TRANSITION_METAL, 38);
            expected.put(ElementCategory.POST_TRANSITION_METAL, 12);
            expected.put(ElementCategory.NONMETAL, 7);
            expected.put(ElementCategory.ALKALI_METAL, 6);
            expected.put(ElementCategory.ALKALINE_EARTH_METAL, 6);
            expected.put(ElementCategory.METALLOID, 6);
            expected.put(ElementCategory.HALOGEN, 6);
            expected.put(ElementCategory.LANTHANIDE, 15);
            expected.put(ElementCategory.NOBLE_GAS, 7);
            expected.put(ElementCategory.ACTINIDE, 15);
            for (ElementCategory category : ElementCategory.values()) {
                assertEquals(expected.get(category).intValue(), PeriodicTable.elements(category).size());
            }
        }

        @Test
        void lesProbabilitesFont100PourCent() {
            double total = 0;
            for (ElementCategory category : ElementCategory.values()) total += category.chance();
            assertEquals(1, total, 1e-12);
        }

        @Test
        void lesDeuxFamillesLesPlusRaresSontUniques() {
            for (ElementCategory category : ElementCategory.values()) {
                boolean rarest = category == ElementCategory.NOBLE_GAS || category == ElementCategory.ACTINIDE;
                assertEquals(rarest, category.unique());
                if (!rarest) {
                    assertTrue(category.chance() > ElementCategory.NOBLE_GAS.chance());
                }
            }
        }

        @Test
        void chaqueMetalDeTransitionAmelioreLeGenerateurDeSaColonne() {
            // 4ᵉ période : particules ; 5ᵉ : vitesse ; 6ᵉ et 7ᵉ : pareil, en plus fort.
            for (int generator = 1; generator <= 10; generator++) {
                assertEquals(new ElementEffect.GeneratorBoost(generator, ElementEffect.Stat.PARTICLES, 1.5),
                        PeriodicTable.element(20 + generator).effect());
                assertEquals(new ElementEffect.GeneratorBoost(generator, ElementEffect.Stat.SPEED, 1.25),
                        PeriodicTable.element(38 + generator).effect());
            }
            for (int generator = 2; generator <= 10; generator++) {
                assertEquals(new ElementEffect.GeneratorBoost(generator, ElementEffect.Stat.PARTICLES, 2),
                        PeriodicTable.element(70 + generator).effect());
                assertEquals(new ElementEffect.GeneratorBoost(generator, ElementEffect.Stat.SPEED, 1.5),
                        PeriodicTable.element(102 + generator).effect());
            }
        }
    }

    @Nested
    class Synthese {

        private static final double MAX = Game.MAX_ATOMS.toDouble();

        /** Partie où le tableau périodique est débloqué, sans atome ni automatisme en marche. */
        private Game unlockedGame() {
            Game game = newGame();
            unlock(game);
            return game;
        }

        /** Vide la collection, pour que chaque tirage se fasse avec les probabilités de base. */
        private void forget(Game game, int... keep) {
            for (int number : List.copyOf(game.state().elements().keySet())) {
                boolean kept = false;
                for (int k : keep) kept |= k == number;
                if (!kept) game.state().setElementCount(number, 0);
            }
        }

        @Test
        void verrouilleTantQueLesAutomatismesNeSontPasAuMaximum() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(100));
            assertFalse(game.isPeriodicTableUnlocked());      // rien d'acheté

            game.buy("keep");
            for (Automation automation : game.automations()) game.buyAutomation(automation.id());
            assertFalse(game.isPeriodicTableUnlocked());      // « autoSlow » a encore des niveaux de cadence

            game.speedUpAutomation("autoSlow");
            assertFalse(game.isPeriodicTableUnlocked());

            game.speedUpAutomation("autoSlow");
            assertTrue(game.isPeriodicTableUnlocked());       // la synthèse automatique n'est pas exigée
            assertFalse(game.ownsAutomation("autoSynth"));
        }

        @Test
        void ilManqueUnSeulAutomatismeEtLeTableauResteVerrouille() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(100));
            game.buy("keep");
            game.buyAutomation("autoSpeed");
            game.buyAutomation("autoGen");
            game.buyAutomation("autoSlow");
            game.speedUpAutomation("autoSlow");
            game.speedUpAutomation("autoSlow");
            assertFalse(game.isPeriodicTableUnlocked());      // « autoFusion » n'est pas acheté
        }

        @Test
        void tableauVerrouillePasDeSyntheseMemeAvecLesAtomes() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(MAX));
            assertFalse(game.canSynthesize());
            assertTrue(game.synthesize().isEmpty());
            assertValue(MAX, game.state().atoms());
            assertEquals(0, game.state().synthesisCount());
        }

        @Test
        void couperUnAutomatismeNeReverrouillePasLeTableau() {
            Game game = unlockedGame();                       // tous les automatismes sont coupés
            game.state().setAtoms(BigNum.of(2));
            assertTrue(game.canSynthesize());
        }

        @Test
        void lePrixDoubleAChaqueSyntheseJusquAuPlafond() {
            Game game = unlockedGame();
            double[] expected = {2, 4, 8, 16, 32, 64, MAX, MAX, MAX};
            for (double cost : expected) {
                assertValue(cost, game.synthesisCost());
                game.state().setAtoms(BigNum.of(MAX));
                game.synthesize();
                forget(game);                                 // sans élément, aucune réduction de prix
            }
            assertEquals(expected.length, game.state().synthesisCount());
            game.state().setSynthesisCount(5000);
            assertValue(MAX, game.synthesisCost());
        }

        @Test
        void impossibleSansAssezDAtomes() {
            Game game = unlockedGame();
            game.state().setAtoms(BigNum.of(1));              // la première synthèse coûte 2
            assertFalse(game.canSynthesize());
            assertTrue(game.synthesize().isEmpty());
            assertValue(1, game.state().atoms());
            assertEquals(0, game.discoveredElements());
            assertEquals(0, game.state().synthesisCount());
        }

        @Test
        void laSyntheseConsommeSonPrixEtDonneUnElement() {
            Game game = unlockedGame();
            game.state().setAtoms(BigNum.of(5));
            List<Element> obtained = game.synthesize();
            assertEquals(1, obtained.size());
            assertValue(3, game.state().atoms());             // 5 − 2
            assertEquals(1, game.elementCount(obtained.get(0).number()));
            assertEquals(1, game.discoveredElements());
            assertFalse(game.canSynthesize());                // la suivante coûte 4
        }

        @Test
        void laSyntheseNeToucheNiAuxAmeliorationsNiAuxGenerateurs() {
            Game game = gameReadyToFuse();
            unlock(game);
            game.state().setAtoms(BigNum.of(MAX));
            game.buy("double");                               // 1 atome
            game.state().setAtoms(BigNum.of(MAX));
            double created = game.state().totalAtoms().toDouble();
            game.synthesize();
            assertEquals(1, game.levelOf("double"));
            assertEquals(1, game.levelOf("speed"));
            assertEquals(3, game.generatorCount());
            assertValue(created, game.state().totalAtoms());
            assertTrue(game.canFuse());                       // le plafond est libéré
        }

        @Test
        void unDoublonAjouteUnExemplaire() {
            Game game = unlockedGame();
            game.state().setElementCount(26, 2);
            game.state().setAtoms(BigNum.of(MAX));
            int before = game.state().elements().values().stream().mapToInt(Integer::intValue).sum();
            int obtained = game.synthesize().size();
            int after = game.state().elements().values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(before + obtained, after);
        }

        @Test
        void lesTiragesSuiventLesProbabilitesDesFamilles() {
            Game game = unlockedGame();                       // hasard reproductible (graine 42)
            game.state().setElementCount(118, 1);             // un élément unique, pour écarter la garantie
            Map<ElementCategory, Integer> draws = new EnumMap<>(ElementCategory.class);
            int total = 40_000;
            for (int i = 0; i < total; i++) {
                game.state().setAtoms(BigNum.of(MAX));
                List<Element> obtained = game.synthesize();
                assertEquals(1, obtained.size());             // sans élément de tirage double, un seul à la fois
                draws.merge(obtained.get(0).category(), 1, Integer::sum);
                forget(game, 118);
            }
            for (ElementCategory category : ElementCategory.values()) {
                double frequency = draws.getOrDefault(category, 0) / (double) total;
                assertEquals(category.chance(), frequency, 0.01,
                        () -> category.label() + " : " + frequency);
            }
        }

        @Test
        void chaqueElementSArreteASonMaximumPuisLeTableauEstComplet() {
            Game game = unlockedGame();
            assertFalse(game.isPeriodicTableComplete());
            for (int i = 0; i < 3_000 && !game.isPeriodicTableComplete(); i++) {
                game.state().setAtoms(BigNum.of(MAX));
                assertFalse(game.synthesize().isEmpty());
            }
            assertTrue(game.isPeriodicTableComplete());
            assertEquals(118, game.discoveredElements());
            for (Element element : PeriodicTable.ELEMENTS) {
                assertEquals(element.category().maxCopies(), game.elementCount(element.number()),
                        () -> element.name() + " devrait être à son maximum");
                assertTrue(game.isElementMaxed(element.number()));
            }
            assertEquals(Game.maxCopies(), game.ownedCopies());

            // Plus rien à synthétiser : les atomes ne sont pas consommés et plus aucune famille ne sort.
            game.state().setAtoms(BigNum.of(MAX));
            int count = game.state().synthesisCount();
            assertFalse(game.canSynthesize());
            assertTrue(game.synthesize().isEmpty());
            assertValue(MAX, game.state().atoms());
            assertEquals(count, game.state().synthesisCount());
            for (ElementCategory category : ElementCategory.values()) {
                assertEquals(0, game.categoryChance(category));
            }
        }

        @Test
        void leNombreMaximalDExemplairesDependDeLaFamille() {
            assertEquals(9, ElementCategory.TRANSITION_METAL.maxCopies());
            assertEquals(4, ElementCategory.LANTHANIDE.maxCopies());
            assertEquals(1, ElementCategory.NOBLE_GAS.maxCopies());
            assertEquals(1, ElementCategory.ACTINIDE.maxCopies());
            for (ElementCategory category : ElementCategory.values()) {
                // Des carrés : la force d'un élément au maximum est un nombre entier.
                double strength = Math.sqrt(category.maxCopies());
                assertEquals(Math.rint(strength), strength, 1e-12, () -> category.label());
                assertEquals(category.maxCopies() == 1, category.unique());
            }
            // 38 + 12 + 7 + 6 + 6 + 6 + 6 éléments à 9 exemplaires, 15 lanthanides à 4, 22 uniques.
            assertEquals(81 * 9 + 15 * 4 + 22, Game.maxCopies());
        }

        @Test
        void unElementAuMaximumNeSortPlus() {
            Game game = unlockedGame();
            game.state().setElementCount(118, 1);             // un élément unique, pour écarter la garantie
            game.state().setElementCount(26, 8);              // fer : encore un exemplaire possible
            assertFalse(game.isElementMaxed(26));
            game.state().setElementCount(26, 9);
            assertTrue(game.isElementMaxed(26));
            for (int i = 0; i < 3_000; i++) {
                game.state().setAtoms(BigNum.of(MAX));
                for (Element element : game.synthesize()) {
                    assertTrue(element.number() != 26 && element.number() != 118, "tiré : " + element.name());
                }
                forget(game, 26, 118);
                game.state().setElementCount(26, 9);
                game.state().setElementCount(118, 1);
            }
        }

        @Test
        void uneFamilleEpuiseeLaisseSaChanceAuxAutres() {
            Game game = unlockedGame();
            for (Element element : PeriodicTable.elements(ElementCategory.LANTHANIDE)) {
                game.state().setElementCount(element.number(), 4);
            }
            assertEquals(0, game.categoryChance(ElementCategory.LANTHANIDE));
            double total = 0;
            for (ElementCategory category : ElementCategory.values()) total += game.categoryChance(category);
            assertEquals(1, total, 1e-12);
            // Les lanthanides possédés ne donnent pas de chance : les autres familles se partagent leurs 6 %.
            assertEquals(0.30 / 0.94, game.categoryChance(ElementCategory.TRANSITION_METAL), 1e-12);
        }

        @Test
        void leTirageDoubleNeDonneQuUnElementSIlNEnRestequUn() {
            Game game = unlockedGame();
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            game.state().setElementCount(26, 8);              // il ne manque qu'un exemplaire de fer
            assertEquals(0.85, game.doubleDrawChance(), 1e-12);   // tableau presque complet : 85 % de tirage double
            game.state().setElementCount(35, 10_000);         // on force le tirage double à 100 %
            assertEquals(1, game.doubleDrawChance());
            assertEquals(Game.maxCopies() - 1, game.ownedCopies());
            game.state().setAtoms(BigNum.of(MAX));
            List<Element> obtained = game.synthesize();
            assertEquals(1, obtained.size());
            assertEquals(26, obtained.get(0).number());
            assertTrue(game.isPeriodicTableComplete());
        }

        @Test
        void lesExemplairesAuDelaDuMaximumNeComptentPasDansLeTotal() {
            Game game = unlockedGame();
            game.state().setElementCount(26, 500);            // une sauvegarde trafiquée, par exemple
            assertEquals(9, game.ownedCopies());
        }

        @Test
        void sansElementUniqueLaDouziemeSyntheseEnDonneUnACoupSur() {
            assertEquals(12, Game.GUARANTEED_UNIQUE_SYNTHESIS);
            Game game = unlockedGame();
            for (int synthesis = 1; synthesis <= 11; synthesis++) {
                game.state().setAtoms(BigNum.of(MAX));
                game.synthesize();
                forget(game);                                 // on fait comme si rien d'unique n'était sorti
            }
            assertFalse(game.isSynthesisAutomationUnlocked());
            game.state().setAtoms(BigNum.of(MAX));
            List<Element> obtained = game.synthesize();       // la douzième
            assertTrue(obtained.get(0).category().unique());
            assertTrue(game.isSynthesisAutomationUnlocked());
        }

        @Test
        void laGarantieNeJouePlusUneFoisUnElementUniquePossede() {
            Game game = unlockedGame();
            game.state().setElementCount(92, 1);              // uranium
            game.state().setSynthesisCount(500);
            int uniques = 0;
            for (int i = 0; i < 200; i++) {
                game.state().setAtoms(BigNum.of(MAX));
                if (game.synthesize().get(0).category().unique()) uniques++;
                forget(game, 92);
            }
            assertTrue(uniques < 40, "trop d'éléments uniques : " + uniques);   // environ 5 %, pas 100 %
        }

        @Test
        void sansElementLesChancesSontCellesDeBase() {
            Game game = unlockedGame();
            for (ElementCategory category : ElementCategory.values()) {
                assertEquals(category.chance(), game.categoryChance(category), 1e-12);
            }
            assertEquals(0, game.doubleDrawChance());
        }

        @Test
        void laChanceRendLesFamillesRaresPlusFrequentes() {
            Game game = unlockedGame();
            game.state().setElementCount(53, 1);              // iode : familles rares +10 %
            double total = 0.89 + 0.11 * 1.10;                // 89 % de familles communes, 11 % de rares
            assertEquals(0.06 * 1.10 / total, game.categoryChance(ElementCategory.LANTHANIDE), 1e-12);
            assertEquals(0.03 * 1.10 / total, game.categoryChance(ElementCategory.NOBLE_GAS), 1e-12);
            assertEquals(0.30 / total, game.categoryChance(ElementCategory.TRANSITION_METAL), 1e-12);
        }

        @Test
        void leTirageDoubleDonneParfoisDeuxElements() {
            Game game = unlockedGame();
            game.state().setElementCount(86, 1);              // radon : 25 % de tirage double
            assertEquals(0.25, game.doubleDrawChance(), 1e-12);
            int doubles = 0;
            int total = 8000;
            for (int i = 0; i < total; i++) {
                game.state().setAtoms(BigNum.of(MAX));
                List<Element> obtained = game.synthesize();
                assertTrue(obtained.size() == 1 || obtained.size() == 2);
                if (obtained.size() == 2) doubles++;
                forget(game, 86);
            }
            assertEquals(0.25, doubles / (double) total, 0.02);
        }

        @Test
        void laChanceDeTirageDoubleNeDepassePas100PourCent() {
            Game game = unlockedGame();
            game.state().setElementCount(35, 10_000);         // brome : 10 % × 100
            assertEquals(1, game.doubleDrawChance());
        }
    }

    @Nested
    class EffetsDesElements {

        /** Partie avec trois générateurs et rien d'autre : ni particule, ni amélioration de vitesse. */
        private Game threeGenerators() {
            Game game = gameWithParticles(1e9);
            game.buy("gen");
            game.buy("gen");
            game.state().setParticles(BigNum.ZERO);
            return game;
        }

        /** Partie dont les quatre automatismes agissent toutes les 4 secondes, pour comparer leurs délais. */
        private Game cadenceGame() {
            Game game = new Game(new GameState(), List.of(SPEED, GENERATOR), List.of(
                    Automation.buying("s", "Vitesse", BigNum.ONE, "speed").withCadence(4, 0, BigNum.ONE, 1),
                    Automation.buying("g", "Générateur", BigNum.ONE, "gen").withCadence(4, 0, BigNum.ONE, 1),
                    Automation.fusing("f", "Fusion", BigNum.ONE).withCadence(4, 0, BigNum.ONE, 1),
                    Automation.synthesizing("y", "Synthèse", BigNum.ONE).withCadence(4, 0, BigNum.ONE, 1)));
            game.start();
            return game;
        }

        @Test
        void sansElementAucunBonus() {
            Game game = threeGenerators();
            for (ElementEffect.Stat stat : ElementEffect.Stat.values()) {
                assertEquals(1, game.elementMultiplier(stat));
            }
            for (ElementEffect.CostTarget target : ElementEffect.CostTarget.values()) {
                assertEquals(1, game.elementCostDivisor(target));
            }
            for (int generator = 0; generator < 3; generator++) {
                assertEquals(1, game.generatorSpeedMultiplier(generator));
                assertEquals(1, game.generatorParticlesMultiplier(generator));
            }
        }

        @Test
        void unMetalDeTransitionNAmelioreQueLesParticulesDeSonGenerateur() {
            Game game = threeGenerators();
            game.state().setElementCount(22, 1);              // titane : générateur 2, particules ×1,5
            assertValue(1, game.particlesPerCreation(0));
            assertValue(1.5, game.particlesPerCreation(1));
            assertValue(1, game.particlesPerCreation(2));
            assertValue(BASE * 3.5, game.productionPerSecond());

            game.tick(ONE_PARTICLE);                          // chaque générateur termine une création
            assertValue(3.5, game.state().particles());
        }

        @Test
        void unMetalDeTransitionNAmelioreQueLaVitesseDeSonGenerateur() {
            Game game = threeGenerators();
            game.state().setElementCount(41, 1);              // niobium : générateur 3, vitesse ×1,25
            assertValue(BASE, game.speed(0));
            assertValue(BASE, game.speed(1));
            assertValue(BASE * 1.25, game.speed(2));

            game.tick(ONE_PARTICLE / 2);
            assertEquals(0.5, game.state().formation(0), 1e-9);
            assertEquals(0.625, game.state().formation(2), 1e-9);
        }

        @Test
        void lesDeuxPeriodesDUneMemeColonneSeMultiplient() {
            Game game = threeGenerators();
            game.state().setElementCount(22, 1);              // titane : générateur 2, ×1,5
            game.state().setElementCount(72, 1);              // hafnium : générateur 2, ×2
            assertValue(3, game.particlesPerCreation(1));
            game.state().setElementCount(40, 1);              // zirconium : générateur 2, vitesse ×1,25
            game.state().setElementCount(104, 1);             // rutherfordium : générateur 2, vitesse ×1,5
            assertValue(BASE * 1.875, game.speed(1));
            assertValue(BASE * (1 + 3 * 1.875 + 1), game.productionPerSecond());
        }

        @Test
        void lesDoublonsRapportentDeMoinsEnMoins() {
            Game game = threeGenerators();
            game.state().setElementCount(22, 4);              // quatre titanes : ×1,5 deux fois, pas quatre
            assertValue(2.25, game.particlesPerCreation(1));
            game.state().setElementCount(22, 9);              // neuf : trois fois
            assertValue(3.375, game.particlesPerCreation(1));
        }

        @Test
        void lesAlcalinoTerreuxDonnentPlusDAtomes() {
            Game game = gameReadyToFuse();
            assertValue(1, game.atomsPerFusion());
            game.state().setElementCount(4, 1);               // béryllium : +3 %
            game.state().setElementCount(88, 1);              // radium : +10 %
            assertValue(1.13, game.atomsPerFusion());
            game.fuse();
            assertValue(1.13, game.state().atoms());
            assertValue(1.13, game.state().totalAtoms());
        }

        @Test
        void lesLanthanidesSeMultiplientEntreEux() {
            Game game = threeGenerators();
            game.state().setElementCount(57, 1);              // lanthane : particules ×1,3
            game.state().setElementCount(58, 1);              // cérium : particules ×1,3
            assertEquals(1.69, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
            assertValue(1.69, game.particlesPerCreation());

            game.state().setElementCount(62, 1);              // samarium : vitesse ×1,12
            game.state().setElementCount(67, 1);              // holmium : atomes ×1,06
            assertValue(BASE * 1.12, game.speed());
            assertValue(1.06, game.atomsPerFusion());
        }

        @Test
        void unLanthanideEnDoubleCompteCommeUnExposant() {
            Game game = threeGenerators();
            game.state().setElementCount(57, 4);              // 1,3 à la puissance √4
            assertEquals(1.69, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
        }

        @Test
        void lesGazNoblesApportentUnBonusMajeur() {
            Game game = threeGenerators();
            game.state().setElementCount(2, 1);               // hélium : particules ×2
            game.state().setElementCount(10, 1);              // néon : vitesse ×1,5
            game.state().setElementCount(18, 1);              // argon : atomes ×1,3
            assertValue(2, game.particlesPerCreation());
            assertValue(BASE * 1.5, game.speed());
            assertValue(1.3, game.atomsPerFusion());
            game.state().setElementCount(118, 1);             // oganesson : ×1,25 partout
            assertValue(2.5, game.particlesPerCreation());
            assertValue(BASE * 1.875, game.speed());
            assertValue(1.625, game.atomsPerFusion());
        }

        @Test
        void lesActinidesDoublentParticulesEtVitesse() {
            Game game = threeGenerators();
            game.state().setElementCount(92, 1);              // uranium : particules ×2 et vitesse ×2
            assertValue(BASE * 3 * 4, game.productionPerSecond());
            game.state().setElementCount(94, 1);              // plutonium : pareil
            assertEquals(4, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
            assertEquals(4, game.elementMultiplier(ElementEffect.Stat.SPEED), 1e-12);
            assertEquals(1, game.elementMultiplier(ElementEffect.Stat.ATOMS), 1e-12);
            assertValue(4, game.particlesPerCreation());
            assertValue(BASE * 4, game.speed());
            assertValue(BASE * 3 * 16, game.productionPerSecond());
            assertValue(1, game.atomsPerFusion());
        }

        @Test
        void additifsEtMultiplicatifsSeCombinent() {
            Game game = gameReadyToFuse();
            game.state().setElementCount(4, 1);               // béryllium : atomes +3 %
            game.state().setElementCount(18, 1);              // argon : atomes ×1,3
            assertValue(1.03 * 1.3, game.atomsPerFusion());
        }

        @Test
        void lesMetauxPauvresReduisentChacunUnPrix() {
            Game game = threeGenerators();
            game.state().setLevel("gen", 0);
            assertValue(100, game.costOf("gen"));
            assertValue(10, game.costOf("speed"));

            game.state().setElementCount(113, 1);             // nihonium : vitesse, diviseur 1,2
            assertValue(Math.ceil(10 / 1.2), game.costOf("speed"));
            assertValue(100, game.costOf("gen"));             // les générateurs ne sont pas concernés
            game.state().setElementCount(13, 1);              // aluminium en plus : diviseur 1,25
            assertValue(8, game.costOf("speed"));

            assertValue(2, game.costOf("mass"));
            game.state().setElementCount(114, 25);            // flérovium : améliorations en atomes, diviseur 1 + 0,2 × 5
            assertValue(1, game.costOf("mass"));
            assertValue(1, game.costOf("double"));            // jamais moins d'un atome
            game.state().setElementCount(115, 100);           // moscovium en plus : diviseur 3
            game.state().setLevel("double", 3);               // prix de base 27
            assertValue(9, game.costOf("double"));
        }

        @Test
        void lePrixDeLaSynthesePeutBaisser() {
            Game game = threeGenerators();
            game.state().setSynthesisCount(20);               // prix au plafond
            assertValue(118, game.synthesisCost());
            game.state().setElementCount(84, 1);              // polonium : diviseur 1,1
            assertValue(Math.ceil(118 / 1.1), game.synthesisCost());
            game.state().setElementCount(54, 1);              // xénon : diviseur +1
            assertValue(Math.ceil(118 / 2.1), game.synthesisCost());
        }

        @Test
        void chaqueMetalAlcalinAccelereSonAutomatisme() {
            Game game = cadenceGame();
            for (String id : List.of("s", "g", "f", "y")) assertEquals(4, game.automationInterval(id));

            game.state().setElementCount(3, 1);               // lithium : achat de la vitesse
            assertEquals(3.2, game.automationInterval("s"), 1e-12);
            assertEquals(4, game.automationInterval("g"));

            game.state().setElementCount(11, 1);              // sodium : achat des générateurs
            game.state().setElementCount(19, 1);              // potassium : fusion
            game.state().setElementCount(37, 1);              // rubidium : synthèse
            for (String id : List.of("s", "g", "f", "y")) assertEquals(3.2, game.automationInterval(id), 1e-12);
        }

        @Test
        void cesiumEtFranciumAccelerentTousLesAutomatismes() {
            Game game = cadenceGame();
            game.state().setElementCount(55, 1);              // césium : tous, +15 %
            for (String id : List.of("s", "g", "f", "y")) {
                assertEquals(4 / 1.15, game.automationInterval(id), 1e-12);
            }
            game.state().setElementCount(3, 1);               // lithium en plus : les bonus s'additionnent
            assertEquals(4 / 1.40, game.automationInterval("s"), 1e-12);
            assertEquals(4 / 1.15, game.automationInterval("g"), 1e-12);
        }

        @Test
        void lHydrogeneRenforceLeDedoublement() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(10));
            game.buy("double");
            game.buy("double");
            assertValue(4, game.particlesPerCreation());
            game.state().setElementCount(1, 1);               // ×2 par niveau devient ×2,1
            assertValue(2.1 * 2.1, game.particlesPerCreation());
        }

        @Test
        void leCarboneRenforceLaMasseAtomique() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(10));
            game.state().setTotalAtoms(BigNum.of(10));
            game.buy("mass");                                 // 1 + 0,5 × 10
            assertValue(6, game.particlesPerCreation());
            game.state().setElementCount(6, 1);               // +0,01 par atome
            assertValue(1 + 0.51 * 10, game.particlesPerCreation());
        }

        @Test
        void lAzoteRenforceLaPatience() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(10));
            game.buy("patience");
            game.tick(60);                                    // 1 + 1 × √1 minute
            assertValue(2, game.particlesPerCreation());
            game.state().setElementCount(7, 1);               // facteur +0,05
            assertValue(2.05, game.particlesPerCreation());
        }

        @Test
        void lOxygeneRenforceLeCatalyseur() {
            Game game = gameWithParticles(1e9);
            game.state().setAtoms(BigNum.of(10));
            game.buy("speed");
            game.buy("boost");                                // chaque niveau de vitesse : ×2 devient ×2,5
            assertValue(BASE * 2.5, game.speed());
            game.state().setElementCount(8, 1);               // le Catalyseur donne 0,002 de plus par niveau
            assertValue(BASE * 2.502, game.speed());
        }

        @Test
        void leSoufreRenforceLaVitesseDeCreation() {
            Game game = gameWithParticles(1e9);
            game.buy("speed");
            game.buy("speed");
            assertValue(BASE * 4, game.speed());
            game.state().setElementCount(16, 1);              // ×2 par niveau devient ×2,005
            assertValue(BASE * 2.005 * 2.005, game.speed());
        }

        @Test
        void lesElementsNAjoutentPasPlusDeDixPointsALaVitesse() {
            Game game = gameWithParticles(1e9);
            game.buy("speed");
            game.state().setElementCount(16, 100);            // soufre : 0,005 × 10
            assertEquals(0.05, game.speedExtraPerLevel(), 1e-12);
            game.state().setElementCount(16, 1_000_000);      // une quantité absurde de soufre
            assertEquals(Game.MAX_ELEMENT_SPEED_EXTRA, game.speedExtraPerLevel(), 1e-12);
            assertValue(BASE * 2.1, game.speed());

            game.state().setAtoms(BigNum.of(10));
            game.buy("boost");                                // le Catalyseur, lui, s'ajoute toujours en entier
            game.state().setElementCount(8, 1_000_000);       // et l'oxygène ne fait pas sauter le plafond
            assertEquals(0.5 + Game.MAX_ELEMENT_SPEED_EXTRA, game.speedExtraPerLevel(), 1e-12);
            assertValue(BASE * 2.6, game.speed());
        }

        @Test
        void leBoreGranditAvecLaCollection() {
            Game game = threeGenerators();
            game.state().setElementCount(5, 1);               // +2 % de particules par élément différent
            assertEquals(1.02, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
            game.state().setElementCount(3, 1);
            game.state().setElementCount(11, 1);              // trois éléments différents
            assertEquals(1.06, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
            game.state().setElementCount(11, 50);             // un doublon ne compte pas
            assertEquals(1.06, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
        }

        @Test
        void leSiliciumGranditAvecLesNiveauxDeVitesse() {
            Game game = gameWithParticles(1e9);
            game.state().setElementCount(14, 1);              // +1 % de particules par niveau de vitesse
            assertValue(1, game.particlesPerCreation());
            game.buy("speed");
            game.buy("speed");
            game.buy("speed");
            assertValue(1.03, game.particlesPerCreation());
        }

        @Test
        void leGermaniumEtLeSeleniumGrandissentAvecLesGenerateurs() {
            Game game = gameWithParticles(1e9);
            game.state().setElementCount(32, 1);              // +5 % de particules par générateur
            game.state().setElementCount(34, 1);              // +2 % de vitesse par générateur
            assertValue(1.05, game.particlesPerCreation());
            assertValue(BASE * 1.02, game.speed());
            game.buy("gen");
            game.buy("gen");
            assertValue(1.15, game.particlesPerCreation());
            assertValue(BASE * 1.06, game.speed());
        }

        @Test
        void lArsenicRecompenseLesAtomesGardes() {
            Game game = newGame();
            game.state().setElementCount(33, 1);              // +0,5 % de vitesse par atome disponible
            assertValue(BASE, game.speed());
            game.state().setAtoms(BigNum.of(40));
            assertValue(BASE * 1.2, game.speed());
            game.buy("double");                               // dépenser un atome fait baisser le bonus
            assertValue(BASE * 1.195, game.speed());
        }

        @Test
        void lAntimoineGranditAvecLesAutomatismes() {
            Game game = newGame();
            game.state().setElementCount(51, 1);              // +10 % de particules par automatisme possédé
            game.state().setAtoms(BigNum.of(20));
            game.buy("keep");
            assertValue(1, game.particlesPerCreation());
            game.buyAutomation("autoSpeed");
            game.buyAutomation("autoGen");
            game.setAutomationEnabled("autoSpeed", false);
            game.setAutomationEnabled("autoGen", false);
            assertValue(1.2, game.particlesPerCreation());
        }

        @Test
        void leTellureDonneDesAtomesSelonLaCollection() {
            Game game = gameReadyToFuse();
            game.state().setElementCount(52, 1);              // +0,2 % d'atomes par élément différent
            game.state().setElementCount(26, 1);
            assertValue(1.004, game.atomsPerFusion());
        }

        @Test
        void uneSynergieSeMultiplieAvecLesAutresBonus() {
            Game game = threeGenerators();
            game.state().setElementCount(32, 1);              // germanium : +5 % par générateur, soit +15 %
            game.state().setElementCount(2, 1);               // hélium : ×2
            assertValue(2 * 1.15, game.particlesPerCreation());
        }
    }

    @Nested
    class Rendement {

        /** +50 % d'atomes par fusion pour chaque ×10 de production au-delà de 100 particules par seconde. */
        private static final Upgrade YIELD = new Upgrade("yield", "Rendement", Resource.ATOMS,
                BigNum.of(3), 1, 1, new Effect.MultiplyAtomsByProduction(100, 0.5));
        /** Vitesse ×10 par niveau, pour choisir la production à la puissance de dix près. */
        private static final Upgrade TENFOLD = new Upgrade("tenfold", "×10", Resource.PARTICLES,
                BigNum.ONE, 1, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(10));

        /** Deux générateurs à 0,25 création par seconde : 0,5 particule par seconde une fois tous débloqués. */
        private Game yieldGame() {
            Game game = new Game(new GameState(),
                    List.of(TENFOLD, new Upgrade("gen", "Générateur", Resource.PARTICLES, BigNum.ONE, 1, 1,
                            new Effect.AddGenerator()), DOUBLE, KEEP, YIELD),
                    List.of(), new Random(42));
            game.start();
            game.state().setAtoms(BigNum.of(10));
            return game;
        }

        /** Porte la production à la fusion à {@code 0,5 × 10^levels} particules par seconde. */
        private void setSpeedLevels(Game game, int levels) {
            game.state().setLevel("tenfold", levels);
        }

        @Test
        void sansLAmeliorationLaProductionNeChangeRienAuxAtomes() {
            Game game = yieldGame();
            setSpeedLevels(game, 20);
            assertEquals(1, game.fusionYield());
            assertValue(1, game.atomsPerFusion());
        }

        @Test
        void sousLeSeuilAucunEffet() {
            Game game = yieldGame();
            game.buy("yield");
            setSpeedLevels(game, 2);                          // 50 particules par seconde, seuil à 100
            assertEquals(1, game.fusionYield());
            assertValue(1, game.atomsPerFusion());
        }

        @Test
        void chaqueFoisDixDeProductionAjouteUnBonus() {
            Game game = yieldGame();
            game.buy("yield");
            game.buy("double");                               // particules ×2 : 1 × 10^n par seconde
            setSpeedLevels(game, 2);                          // 100 : pile au seuil
            assertEquals(1, game.fusionYield(), 1e-9);
            setSpeedLevels(game, 3);                          // 1 000 : une puissance de dix au-dessus
            assertEquals(1.5, game.fusionYield(), 1e-9);
            setSpeedLevels(game, 6);                          // 1 000 000 : quatre
            assertEquals(3, game.fusionYield(), 1e-9);
            assertValue(3, game.atomsPerFusion());
            setSpeedLevels(game, 302);                        // bien au-delà de ce que contient un double
            assertEquals(151, game.fusionYield(), 1e-6);
        }

        @Test
        void cEstLaProductionAvecTousLesGenerateursQuiCompte() {
            Game game = yieldGame();
            game.buy("yield");
            game.buy("double");
            setSpeedLevels(game, 3);
            assertEquals(1, game.generatorCount());           // un seul générateur pour l'instant
            assertValue(500, game.productionPerSecond());
            assertValue(1000, game.productionAtFusion());     // mais deux au moment de fusionner
            assertEquals(1.5, game.fusionYield(), 1e-9);
        }

        @Test
        void laFusionRapporteLesAtomesMultiplies() {
            Game game = yieldGame();
            game.buy("yield");                                // 3 atomes
            game.buy("double");                               // 1 atome
            setSpeedLevels(game, 4);                          // 10 000 par seconde : +50 % deux fois
            game.state().setParticles(BigNum.of(10));
            game.buy("gen");
            assertTrue(game.fuse());
            assertValue(6 + 2, game.state().atoms());
        }

        @Test
        void seMultiplieAvecLesElements() {
            Game game = yieldGame();
            game.buy("yield");
            game.buy("double");
            setSpeedLevels(game, 4);                          // rendement ×2
            game.state().setElementCount(18, 1);              // argon : atomes ×1,3
            assertValue(2.6, game.atomsPerFusion());
        }

        @Test
        void lePhosphoreRenforceLeRendement() {
            Game game = yieldGame();
            game.buy("yield");
            game.buy("double");
            setSpeedLevels(game, 4);                          // deux puissances de dix au-dessus du seuil
            assertEquals(2, game.fusionYield(), 1e-9);
            game.state().setElementCount(15, 1);              // +50 % devient +51 %
            assertEquals(2.02, game.fusionYield(), 1e-9);
            game.state().setElementCount(15, 100);            // cent exemplaires : +60 %
            assertEquals(2.2, game.fusionYield(), 1e-9);
        }

        @Test
        void lesElementsQuiAugmententLaProductionAugmententLeRendement() {
            Game game = yieldGame();
            game.buy("yield");
            game.buy("double");
            setSpeedLevels(game, 3);                          // 1 000 par seconde
            assertEquals(1.5, game.fusionYield(), 1e-9);
            game.state().setElementCount(92, 1);              // uranium : particules ×2 et vitesse ×2
            assertEquals(1 + 0.5 * Math.log10(40), game.fusionYield(), 1e-9);
        }

        @Test
        void seuilEtBonusDoiventEtreValides() {
            assertThrows(IllegalArgumentException.class, () -> new Effect.MultiplyAtomsByProduction(0, 0.5));
            assertThrows(IllegalArgumentException.class, () -> new Effect.MultiplyAtomsByProduction(100, -1));
        }

        @Test
        void leRendementDuJeuDemarreAMilleParticulesParSeconde() {
            Game game = new Game();
            game.start();
            Upgrade upgrade = game.upgrades().stream().filter(u -> u.id().equals("atom_yield")).findFirst().orElseThrow();
            assertEquals(Resource.ATOMS, upgrade.resource());
            assertEquals(new Effect.MultiplyAtomsByProduction(1_000, 0.3), upgrade.effect());
        }
    }

    @Nested
    class SyntheseAutomatique {

        /** Partie où le tableau est débloqué, avec {@code atoms} atomes et aucun automatisme en marche. */
        private Game unlockedGame(int atoms) {
            Game game = newGame();
            unlock(game);
            game.state().setAtoms(BigNum.of(atoms));
            return game;
        }

        @Test
        void verrouilleeTantQuAucunElementUniqueNEstObtenu() {
            Game game = unlockedGame(100);
            game.state().setElementCount(26, 3);              // du fer, même en plusieurs exemplaires, ne suffit pas
            assertFalse(game.isSynthesisAutomationUnlocked());
            assertFalse(game.isAutomationAvailable("autoSynth"));
            assertFalse(game.buyAutomation("autoSynth"));
            assertValue(100, game.state().atoms());
        }

        @Test
        void unGazNobleOuUnActinideLaDebloque() {
            Game noble = unlockedGame(100);
            noble.state().setElementCount(10, 1);             // néon
            assertTrue(noble.isSynthesisAutomationUnlocked());
            assertTrue(noble.isAutomationAvailable("autoSynth"));

            Game actinide = unlockedGame(100);
            actinide.state().setElementCount(92, 1);          // uranium
            assertTrue(actinide.isAutomationAvailable("autoSynth"));
            assertTrue(actinide.buyAutomation("autoSynth"));
            assertValue(96, actinide.state().atoms());
            assertTrue(actinide.isAutomationEnabled("autoSynth"));
        }

        @Test
        void lesAutresAutomatismesNOntPasBesoinDElement() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(10));
            game.buy("keep");
            assertTrue(game.isAutomationAvailable("autoSpeed"));
            assertFalse(game.isAutomationAvailable("autoSynth"));
        }

        @Test
        void elleSynthetiseSansClicASonRythme() {
            Game game = unlockedGame(100);
            game.state().setElementCount(92, 1);
            game.buyAutomation("autoSynth");                  // une action toutes les 2 secondes
            game.state().setAtoms(BigNum.of(100));
            game.tick(1.8);
            assertEquals(0, game.state().synthesisCount());
            game.tick(0.4);
            assertEquals(1, game.state().synthesisCount());
            assertValue(98, game.state().atoms());            // la première synthèse coûte 2
            game.tick(4);
            assertEquals(3, game.state().synthesisCount());   // puis 4, puis 8
            assertValue(86, game.state().atoms());
        }

        @Test
        void elleSArreteQuandLeTableauEstComplet() {
            Game game = unlockedGame(100);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            game.buyAutomation("autoSynth");
            game.state().setAtoms(BigNum.of(100));
            game.tick(30);
            assertEquals(0, game.state().synthesisCount());
            assertValue(100, game.state().atoms());
        }

        @Test
        void elleAttendDAvoirAssezDAtomes() {
            Game game = unlockedGame(100);
            game.state().setElementCount(92, 1);
            game.buyAutomation("autoSynth");
            game.state().setAtoms(BigNum.of(1));              // pas de quoi payer 2 atomes
            game.tick(10);
            assertEquals(0, game.state().synthesisCount());
            assertValue(1, game.state().atoms());
        }
    }

    @Nested
    class Simulation {

        /** Joue en achetant tout dès que possible, jusqu'à pouvoir fusionner. Renvoie la durée en minutes. */
        private double minutesUntilFusion(Game game) {
            double dt = 0.05;
            double seconds = 0;
            while (!game.canFuse() && seconds < 3 * 3600) {
                game.tick(dt);
                seconds += dt;
                for (Upgrade upgrade : game.upgrades(Resource.PARTICLES)) {
                    while (game.buy(upgrade.id())) { /* achète tant que possible */ }
                }
            }
            return seconds / 60;
        }

        /**
         * Garde-fou d'équilibrage : un joueur qui achète tout dès que possible doit
         * débloquer le dixième générateur en 30 à 45 minutes avec le catalogue du jeu.
         * Si ce test échoue après un changement dans {@link Upgrades}, c'est que la durée
         * de la phase 1 a bougé : ajuster les coûts, ou la fourchette si c'est voulu.
         */
        @Test
        void laPremierePartieDureEntre30Et45Minutes() {
            Game game = new Game();
            game.start();
            double minutes = minutesUntilFusion(game);

            assertEquals(Upgrades.MAX_GENERATORS, game.maxGeneratorCount());
            assertTrue(game.canFuse());
            assertTrue(minutes >= 30 && minutes <= 45, "durée de la première partie : " + minutes + " min");
            // L'animation n'est lisible que sous 6 créations par seconde et par générateur.
            assertTrue(game.speed().toDouble() < 6, "vitesse finale trop élevée : " + game.speed().format());
        }

        /**
         * Garde-fou sur l'arrivée de l'automatisation : un joueur qui achète les trois premières
         * améliorations en atomes puis économise pour Persistance doit l'obtenir entre 2 h et 3 h 15,
         * et pouvoir aussitôt enchaîner sur les automatismes. Plus tôt, le début perd son intérêt ;
         * plus tard, il faut racheter les améliorations à la main des dizaines de fois.
         */
        @Test
        void persistanceArriveEntre2hEt3h15() {
            Game game = new Game();
            game.start();
            double minutes = 0;
            String[] order = {"atom_double", "atom_patience", "atom_mass", "atom_keep"};
            int next = 0;
            while (next < order.length && minutes < 10 * 60) {
                minutes += minutesUntilFusion(game);
                assertTrue(game.fuse());
                while (next < order.length && game.buy(order[next])) next++;
            }
            assertTrue(game.isAutomationUnlocked());
            assertTrue(minutes >= 120 && minutes <= 195, "Persistance obtenue après " + minutes + " min");
        }

        /**
         * Garde-fou sur les atomes : en dépensant ses atomes dès que possible, les parties
         * raccourcissent, mais les dix premières durent encore plus de trois minutes.
         */
        @Test
        void lesAtomesRaccourcissentLesPartiesSansLesRendreInstantanees() {
            Game game = new Game();
            game.start();
            double first = minutesUntilFusion(game);
            double previous = first;
            for (int fusion = 1; fusion <= 9; fusion++) {
                assertTrue(game.fuse());
                for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
                    while (game.buy(upgrade.id())) { /* dépense tant que possible */ }
                }
                double minutes = minutesUntilFusion(game);
                assertTrue(minutes <= previous + 0.5,
                        "la partie " + (fusion + 1) + " est plus longue que la précédente : " + minutes + " min");
                assertTrue(minutes > 3, "la partie " + (fusion + 1) + " est trop courte : " + minutes + " min");
                previous = minutes;
            }
            assertTrue(previous < first / 3, "les atomes n'accélèrent pas assez : " + previous + " min");
        }
    }

    /** Partie avec tous les générateurs du catalogue de test débloqués (trois). */
    private static Game gameReadyToFuse() {
        Game game = gameWithParticles(1e9);
        game.buy("gen");
        game.buy("gen");
        game.buy("speed");
        game.tick(ONE_PARTICLE / 8);
        return game;
    }

    /** Achète Persistance et tous les automatismes disponibles, les porte à leur cadence maximale, puis les coupe. */
    private static void unlock(Game game) {
        BigNum atoms = game.state().atoms();
        game.state().setAtoms(BigNum.of(100));
        game.buy("keep");
        for (Automation automation : game.automations()) {
            game.buyAutomation(automation.id());
            while (game.speedUpAutomation(automation.id())) { /* jusqu'à la cadence maximale */ }
            game.setAutomationEnabled(automation.id(), false);
        }
        game.state().setAtoms(atoms);
        assertTrue(game.isPeriodicTableUnlocked());
    }

    /** Redébloque tous les générateurs puis fusionne. */
    private static void fuseAgain(Game game) {
        game.state().setParticles(BigNum.of(1e9));
        game.buy("gen");
        game.buy("gen");
        assertTrue(game.fuse());
    }
}

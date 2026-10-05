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
            createEnoughAtoms(game);
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

        /** Partie où l'automatisation est débloquée, avec {@code atoms} atomes à dépenser, sans particule. */
        private Game gameWithAutomation(int atoms) {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(atoms + 1));
            game.buy("keep");
            createEnoughAtoms(game);
            return game;
        }

        @Test
        void verrouilleeAvantTrenteAtomesCrees() {
            assertValue(30, Game.UNLOCK_TOTAL_ATOMS);
            Game game = gameWithParticles(1e9);
            game.state().setAtoms(BigNum.of(100));
            game.state().setTotalAtoms(BigNum.of(29));
            game.buy("keep");                                 // la persistance ne débloque plus rien
            assertFalse(game.isAutomationUnlocked());
            assertFalse(game.canBuyAutomation("autoSpeed"));
            assertFalse(game.buyAutomation("autoSpeed"));
            assertFalse(game.ownsAutomation("autoSpeed"));
            assertValue(99, game.state().atoms());
        }

        @Test
        void seDebloqueATrenteAtomesCreesMemeDepenses() {
            Game game = newGame();
            game.state().setTotalAtoms(BigNum.of(30));
            game.state().setAtoms(BigNum.of(3));              // les 27 autres ont été dépensés
            assertTrue(game.isAutomationUnlocked());
            assertEquals(0, game.levelOf("keep"));            // sans persistance
            assertTrue(game.buyAutomation("autoSpeed"));
            assertValue(1, game.state().atoms());
        }

        @Test
        void laFusionQuiCreeLeTrentiemeAtomeDebloqueToutDeSuite() {
            Game game = gameReadyToFuse();
            game.state().setTotalAtoms(BigNum.of(29));
            assertFalse(game.isAutomationUnlocked());
            assertFalse(game.isPeriodicTableUnlocked());
            assertTrue(game.fuse());
            assertTrue(game.isAutomationUnlocked());
            assertTrue(game.isPeriodicTableUnlocked());
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
            createEnoughAtoms(game);
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
            assertEquals(16, automation.intervalAt(0));
            assertEquals(2, automation.intervalAt(3));
            assertEquals(2, automation.intervalAt(9));
            assertValue(5, automation.speedCostAt(0));
            assertValue(20, automation.speedCostAt(1));
            assertValue(80, automation.speedCostAt(2));
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
            createEnoughAtoms(game);
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
            game.state().setElementCount(36, 1);      // krypton : tous les automatismes 50 % plus rapides
            assertEquals(1 / 1.5, game.automationInterval("autoSlow"), 1e-12);
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
            createEnoughAtoms(game);
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
        void leTableauSOuvreATrenteAtomesCreesSansRienAcheter() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(100));
            game.state().setTotalAtoms(BigNum.of(29.9));
            assertFalse(game.isPeriodicTableUnlocked());

            game.state().setTotalAtoms(BigNum.of(30));
            assertTrue(game.isPeriodicTableUnlocked());       // ni persistance ni automatisme exigés
            assertEquals(0, game.levelOf("keep"));
            assertFalse(game.ownsAutomation("autoSpeed"));
            assertTrue(game.canSynthesize());
        }

        @Test
        void sansAssezDAtomesCreesNiPersistanceNiAutomatismesNOuvrentLeTableau() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(100));
            game.state().setTotalAtoms(BigNum.of(10));
            game.buy("keep");
            // Des automatismes déjà possédés, comme après une explosion qui les conserve.
            for (Automation automation : game.automations()) {
                game.state().addAutomation(automation.id());
                game.state().setAutomationSpeedLevel(automation.id(), automation.maxSpeedLevel());
            }
            assertTrue(game.isAutomationUnlocked());
            assertFalse(game.isPeriodicTableUnlocked());
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
        void sansElementUniqueLaHuitiemeSyntheseEnDonneUnACoupSur() {
            assertEquals(8, Game.GUARANTEED_UNIQUE_SYNTHESIS);
            Game game = unlockedGame();
            for (int synthesis = 1; synthesis <= 7; synthesis++) {
                game.state().setAtoms(BigNum.of(MAX));
                game.synthesize();
                forget(game);                                 // on fait comme si rien d'unique n'était sorti
            }
            assertFalse(game.isSynthesisAutomationUnlocked());
            game.state().setAtoms(BigNum.of(MAX));
            List<Element> obtained = game.synthesize();       // la huitième
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
            game.state().setElementCount(4, 1);               // béryllium : +1,5 %
            game.state().setElementCount(88, 1);              // radium : +5 %
            assertValue(1.065, game.atomsPerFusion());
            game.fuse();
            assertValue(1.065, game.state().atoms());
            assertValue(1.065, game.state().totalAtoms());
        }

        @Test
        void lesLanthanidesSeMultiplientEntreEux() {
            Game game = threeGenerators();
            game.state().setElementCount(57, 1);              // lanthane : particules ×1,3
            game.state().setElementCount(58, 1);              // cérium : particules ×1,3
            assertEquals(1.69, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
            assertValue(1.69, game.particlesPerCreation());

            game.state().setElementCount(62, 1);              // samarium : vitesse ×1,12
            game.state().setElementCount(67, 1);              // holmium : atomes ×1,03
            assertValue(BASE * 1.12, game.speed());
            assertValue(1.03, game.atomsPerFusion());
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
            game.state().setElementCount(18, 1);              // argon : atomes ×1,15
            assertValue(2, game.particlesPerCreation());
            assertValue(BASE * 1.5, game.speed());
            assertValue(1.15, game.atomsPerFusion());
            game.state().setElementCount(118, 1);             // oganesson : ×1,25 partout
            assertValue(2.5, game.particlesPerCreation());
            assertValue(BASE * 1.875, game.speed());
            assertValue(1.15 * 1.25, game.atomsPerFusion());
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
            game.state().setElementCount(4, 1);               // béryllium : atomes +1,5 %
            game.state().setElementCount(18, 1);              // argon : atomes ×1,15
            assertValue(1.015 * 1.15, game.atomsPerFusion());
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
            game.state().setElementCount(84, 1);              // polonium : diviseur 1,05
            assertValue(Math.ceil(118 / 1.05), game.synthesisCost());
            game.state().setElementCount(54, 1);              // xénon : diviseur +0,5
            assertValue(Math.ceil(118 / 1.55), game.synthesisCost());
        }

        @Test
        void chaqueMetalAlcalinAccelereSonAutomatisme() {
            Game game = cadenceGame();
            for (String id : List.of("s", "g", "f", "y")) assertEquals(4, game.automationInterval(id));

            game.state().setElementCount(3, 1);               // lithium : achat de la vitesse
            assertEquals(4 / 1.12, game.automationInterval("s"), 1e-12);
            assertEquals(4, game.automationInterval("g"));

            game.state().setElementCount(11, 1);              // sodium : achat des générateurs
            game.state().setElementCount(19, 1);              // potassium : fusion
            game.state().setElementCount(37, 1);              // rubidium : synthèse
            for (String id : List.of("s", "g", "f", "y")) assertEquals(4 / 1.12, game.automationInterval(id), 1e-12);
        }

        @Test
        void cesiumEtFranciumAccelerentTousLesAutomatismes() {
            Game game = cadenceGame();
            game.state().setElementCount(55, 1);              // césium : tous, +8 %
            for (String id : List.of("s", "g", "f", "y")) {
                assertEquals(4 / 1.08, game.automationInterval(id), 1e-12);
            }
            game.state().setElementCount(3, 1);               // lithium en plus : les bonus s'additionnent
            assertEquals(4 / 1.20, game.automationInterval("s"), 1e-12);
            assertEquals(4 / 1.08, game.automationInterval("g"), 1e-12);
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
            createEnoughAtoms(game);
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
            game.state().setElementCount(52, 1);              // +0,1 % d'atomes par élément différent
            game.state().setElementCount(26, 1);
            assertValue(1.002, game.atomsPerFusion());
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
            game.state().setElementCount(18, 1);              // argon : atomes ×1,15
            assertValue(2.3, game.atomsPerFusion());
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
        void leRendementDuJeuDemarreADixMilleParticulesParSeconde() {
            Game game = new Game();
            game.start();
            Upgrade upgrade = game.upgrades().stream().filter(u -> u.id().equals("atom_yield")).findFirst().orElseThrow();
            assertEquals(Resource.ATOMS, upgrade.resource());
            assertEquals(new Effect.MultiplyAtomsByProduction(10_000, 0.15), upgrade.effect());
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
            createEnoughAtoms(game);
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
    class Explosion {

        /** Remplit le tableau périodique : chaque élément à son maximum d'exemplaires. */
        private void fillTable(Game game) {
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
        }

        /** Partie bien avancée : améliorations, automatismes, atomes, particules, tableau débloqué. */
        private Game advancedGame() {
            Game game = gameReadyToFuse();
            game.state().setAtoms(BigNum.of(50));
            game.state().setTotalAtoms(BigNum.of(300));
            game.buy("double");
            game.buy("mass");
            unlock(game);
            game.setAutomationEnabled("autoSpeed", true);
            game.state().setSynthesisCount(40);
            game.state().setParticles(BigNum.of(1234));
            return game;
        }

        @Test
        void impossibleTantQueLeTableauNEstPasComplet() {
            Game game = advancedGame();
            assertFalse(game.canExplode());
            fillTable(game);
            game.state().setElementCount(26, 8);              // il manque un exemplaire de fer
            assertFalse(game.canExplode());
            assertFalse(game.explode());
            assertEquals(0, game.state().explosions());
            assertValue(0, game.state().darkMatter());
            assertEquals(8, game.elementCount(26));           // rien n'a été effacé
            assertFalse(game.isDarkMatterUnlocked());
        }

        @Test
        void impossibleAvantLeDemarrage() {
            Game game = unstartedGame();
            fillTable(game);
            assertFalse(game.canExplode());
            assertFalse(game.explode());
        }

        @Test
        void leTableauCompletPeutExploser() {
            Game game = advancedGame();
            fillTable(game);
            assertTrue(game.isPeriodicTableComplete());
            assertTrue(game.canExplode());
        }

        @Test
        void lExplosionDonneDeLaMatiereNoire() {
            Game game = advancedGame();
            fillTable(game);
            assertTrue(game.explode());
            assertValue(1, game.state().darkMatter());
            assertEquals(1, game.state().explosions());
            assertTrue(game.isDarkMatterUnlocked());
        }

        @Test
        void lExplosionEffaceTouteLaMatiere() {
            Game game = advancedGame();
            fillTable(game);
            double played = game.state().timePlayed();
            assertTrue(game.explode());

            assertValue(0, game.state().particles());
            assertValue(0, game.state().atoms());
            assertValue(0, game.state().totalAtoms());
            assertEquals(0, game.state().timeSinceFusion());
            assertEquals(0, game.discoveredElements());
            assertEquals(0, game.ownedCopies());
            assertEquals(0, game.state().synthesisCount());
            for (Upgrade upgrade : game.upgrades()) {
                assertEquals(0, game.levelOf(upgrade.id()), () -> upgrade.name());
            }
            for (Automation automation : game.automations()) {
                assertFalse(game.ownsAutomation(automation.id()));
                assertFalse(game.isAutomationEnabled(automation.id()));
                assertEquals(0, game.automationSpeedLevel(automation.id()));
            }
            assertFalse(game.isAutomationUnlocked());
            assertFalse(game.isPeriodicTableUnlocked());
            assertFalse(game.canExplode());

            // La partie repart comme au tout début : un générateur, la vitesse de base, aucun bonus.
            assertTrue(game.isStarted());
            assertEquals(1, game.generatorCount());
            assertValue(BASE, game.speed());
            assertValue(1, game.particlesPerCreation());
            assertValue(BASE, game.productionPerSecond());
            assertValue(1, game.atomsPerFusion());
            assertEquals(played, game.state().timePlayed());  // le temps de jeu, lui, n'est pas effacé
        }

        @Test
        void apresLExplosionLeJeuSeRejoueNormalement() {
            Game game = advancedGame();
            fillTable(game);
            game.explode();
            game.tick(ONE_PARTICLE);                          // aucun automatisme : une particule, pas plus
            assertValue(1, game.state().particles());
            game.state().setParticles(BigNum.of(1e9));
            assertTrue(game.buy("gen"));
            assertTrue(game.buy("gen"));
            assertTrue(game.fuse());
            assertValue(1, game.state().atoms());
            assertValue(1, game.state().darkMatter());        // la fusion ne touche pas à la matière noire
        }

        @Test
        void chaqueExplosionAjouteDeLaMatiereNoire() {
            Game game = advancedGame();
            fillTable(game);
            game.explode();
            assertFalse(game.explode());                      // le tableau est vide : pas de seconde explosion
            fillTable(game);
            assertTrue(game.explode());
            assertValue(2, game.state().darkMatter());
            assertEquals(2, game.state().explosions());
        }

        @Test
        void laMatiereNoireEtLesExplosionsNePeuventPasEtreNegatives() {
            GameState state = new GameState();
            assertThrows(IllegalArgumentException.class, () -> state.setDarkMatter(BigNum.of(-1)));
            assertThrows(IllegalArgumentException.class, () -> state.setExplosions(-1));
        }
    }

    @Nested
    class TailleDeLaMatiereNoire {

        private static final double START = Game.DARK_MATTER_START_SIZE.toDouble();
        /** L'élan qu'apporte une matière noire, par seconde d'appui. */
        private static final double UNIT = Game.DARK_MATTER_GROWTH_PER_UNIT;

        /** Partie qui possède {@code darkMatter} unités de matière noire, après autant d'explosions. */
        private Game gameWithDarkMatter(int darkMatter) {
            Game game = newGame();
            game.state().setDarkMatter(BigNum.of(darkMatter));
            game.state().setExplosions(darkMatter);
            return game;
        }

        @Test
        void elleDemarreALaTailleDUnProton() {
            Game game = newGame();
            assertValue(1e-15, game.state().darkMatterSize());
            assertEquals("proton", SizeScale.reached(game.state().darkMatterSize()).name());
        }

        @Test
        void sansMatiereNoireRienNeGrossit() {
            Game game = newGame();
            assertFalse(game.growDarkMatter(10));
            assertValue(START, game.state().darkMatterSize());
            assertValue(1, game.darkMatterGrowthPerSecond());
        }

        @Test
        void enDebutDePartieUneSecondeDAppuiAjouteTroisPourMille() {
            Game game = gameWithDarkMatter(1);
            assertEquals(0.003, UNIT);
            assertEquals(1, game.darkMatterProgressFactor()); // moins d'une particule par seconde, aucun atome
            assertEquals(UNIT, game.darkMatterMomentum(), 1e-12);
            assertValue(1, game.darkMatterResistance());      // taille d'un proton : aucune résistance
            assertValue(Math.exp(UNIT), game.darkMatterGrowthPerSecond());
            assertTrue(game.growDarkMatter(1));
            // La résistance gagne 0,1 × l'élan par seconde ; la taille est sa puissance dix.
            assertValue(START * Math.pow(1 + 0.1 * UNIT, 10), game.state().darkMatterSize());
            game.growDarkMatter(1);
            assertValue(START * Math.pow(1 + 0.2 * UNIT, 10), game.state().darkMatterSize());
        }

        @Test
        void plusElleEstGrandePlusElleGrossitLentement() {
            Game game = gameWithDarkMatter(1);
            game.state().setDarkMatterSize(Game.DARK_MATTER_START_SIZE.multiply(1000));
            assertEquals(1.995, game.darkMatterResistance().toDouble(), 0.001);   // ×1 000 de taille : deux fois plus lent
            assertValue(Math.exp(UNIT / Math.pow(1000, 0.1)), game.darkMatterGrowthPerSecond());
            game.state().setDarkMatterSize(Game.DARK_MATTER_START_SIZE.multiply(1e30));
            assertValue(1000, game.darkMatterResistance());   // au million de milliards de mètres : mille fois plus lent
        }

        @Test
        void doublerLElanOuLeTempsDAppuiRevientAuMeme() {
            Game slow = gameWithDarkMatter(1);
            slow.growDarkMatter(2000);
            Game fast = gameWithDarkMatter(2);
            fast.growDarkMatter(1000);
            assertEquals(slow.state().darkMatterSize().log10(), fast.state().darkMatterSize().log10(), 1e-9);
        }

        @Test
        void laCroissanceSuitLaProductionDeParticules() {
            Game game = gameWithDarkMatter(1);
            game.state().setLevel("gen", 2);                  // trois générateurs : 0,75 particule par seconde
            assertEquals(1, game.darkMatterProgressFactor());
            game.state().setLevel("huge", 1);                 // vitesse ×1e30 : 7,5e29 particules par seconde
            assertEquals(1 + Math.log10(7.5e29), game.darkMatterProgressFactor(), 1e-9);
            assertEquals(UNIT * (1 + Math.log10(7.5e29)), game.darkMatterMomentum(), 1e-12);
        }

        @Test
        void laCroissanceSuitLesAtomesCrees() {
            Game game = gameWithDarkMatter(1);
            game.state().setTotalAtoms(BigNum.of(1000));
            assertEquals(4, game.darkMatterProgressFactor(), 1e-9);   // 1 + 3 ordres de grandeur
            assertEquals(4 * UNIT, game.darkMatterMomentum(), 1e-12);
            game.state().setAtoms(BigNum.ZERO);               // dépenser ses atomes ne ralentit rien
            assertEquals(4, game.darkMatterProgressFactor(), 1e-9);
        }

        @Test
        void laCroissanceRepartDeZeroApresUneExplosion() {
            Game game = gameWithDarkMatter(1);
            game.state().setTotalAtoms(BigNum.of(1000));
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.darkMatterProgressFactor() > 4);  // les éléments gonflent la production
            game.explode();
            assertEquals(1, game.darkMatterProgressFactor()); // plus d'atomes, production de départ
        }

        @Test
        void laTailleSeLitAussiEnAnneesLumiere() {
            Game game = gameWithDarkMatter(1);
            game.state().setDarkMatterSize(BigNum.of(9.46, 18));
            assertValue(1000, game.darkMatterLightYears());
            assertValue(9.46e15, SizeScale.LIGHT_YEAR);
        }

        @Test
        void beaucoupDePetitsAppuisValentUnLong() {
            Game longPress = gameWithDarkMatter(1);
            longPress.growDarkMatter(3);
            Game frames = gameWithDarkMatter(1);
            for (int i = 0; i < 180; i++) frames.growDarkMatter(1.0 / 60);
            assertEquals(longPress.state().darkMatterSize().toDouble(), frames.state().darkMatterSize().toDouble(),
                    START * 1e-9);                            // le calcul est exact : même résultat
        }

        @Test
        void plusIlYADeMatiereNoirePlusElleGrossitVite() {
            Game one = gameWithDarkMatter(1);
            Game four = gameWithDarkMatter(4);
            assertEquals(4 * UNIT, four.darkMatterMomentum(), 1e-12);
            one.growDarkMatter(1);
            four.growDarkMatter(1);
            assertValue(START * Math.pow(1 + 0.1 * 4 * UNIT, 10), four.state().darkMatterSize());
            assertTrue(four.state().darkMatterSize().gt(one.state().darkMatterSize()));
        }

        @Test
        void sansAppuiLaTailleNeBougePas() {
            Game game = gameWithDarkMatter(2);
            game.growDarkMatter(5);
            BigNum size = game.state().darkMatterSize();
            game.tick(3600);                                  // le temps passe, personne n'appuie
            assertFalse(game.growDarkMatter(0));
            assertEquals(size, game.state().darkMatterSize());
        }

        @Test
        void elleDepasseLUniversSansProbleme() {
            Game game = gameWithDarkMatter(1_000_000);        // un élan de 3 000
            game.growDarkMatter(100);
            assertEquals(10 * Math.log10(1 + 0.1 * UNIT * 1e6 * 100) - 15, game.state().darkMatterSize().log10(), 1e-6);
            assertEquals("univers observable", SizeScale.reached(game.state().darkMatterSize()).name());
            assertEquals(null, SizeScale.next(game.state().darkMatterSize()));
        }

        @Test
        void lExplosionNeLaRemetPasAZero() {
            Game game = gameWithDarkMatter(1);
            game.growDarkMatter(10);
            BigNum size = game.state().darkMatterSize();
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertEquals(size, game.state().darkMatterSize());
            assertEquals(2 * UNIT, game.darkMatterMomentum(), 1e-12); // deux matières noires maintenant
        }

        @Test
        void dureeInvalideRefusee() {
            Game game = gameWithDarkMatter(1);
            assertThrows(IllegalArgumentException.class, () -> game.growDarkMatter(-1));
            assertThrows(IllegalArgumentException.class, () -> game.growDarkMatter(Double.NaN));
            assertThrows(IllegalArgumentException.class, () -> game.state().setDarkMatterSize(BigNum.ZERO));
        }

        @Test
        void lEchelleDesGrandeursEstRangeeDuPlusPetitAuPlusGrand() {
            List<Landmark> landmarks = SizeScale.LANDMARKS;
            assertEquals(Game.DARK_MATTER_START_SIZE, landmarks.get(0).size());
            for (int i = 1; i < landmarks.size(); i++) {
                Landmark previous = landmarks.get(i - 1);
                Landmark current = landmarks.get(i);
                assertTrue(current.size().gt(previous.size()), () -> current.name() + " après " + previous.name());
            }
            assertEquals(landmarks.size(), landmarks.stream().map(Landmark::name).distinct().count());
        }

        @Test
        void repereAtteintEtRepereSuivant() {
            assertEquals(null, SizeScale.reached(BigNum.of(1, -20)));
            assertEquals("proton", SizeScale.next(BigNum.of(1, -20)).name());
            BigNum threeNanometers = BigNum.of(3, -9);
            assertEquals("hélice d'ADN", SizeScale.reached(threeNanometers).name());
            assertEquals("virus", SizeScale.next(threeNanometers).name());
            assertEquals("être humain", SizeScale.reached(BigNum.of(1.7)).name());   // pile sur un repère : atteint
            assertEquals("baleine bleue", SizeScale.next(BigNum.of(1.7)).name());
            assertThrows(IllegalArgumentException.class, () -> new Landmark("rien", BigNum.ZERO));
            assertThrows(IllegalArgumentException.class, () -> new Landmark(" ", BigNum.ONE));
        }
    }

    @Nested
    class ArbreDeMatiereNoire {

        private static final double START = Game.DARK_MATTER_START_SIZE.toDouble();

        /** Partie démarrée, avec assez de matière noire (20) pour que toutes les cases soient accessibles. */
        private Game darkGame() {
            Game game = newGame();
            game.state().setDarkMatter(BigNum.of(20));
            game.state().setExplosions(1);
            return game;
        }

        /** Donne directement {@code level} niveaux d'une amélioration de matière noire. */
        private void give(Game game, String id, int level) {
            game.state().setDarkLevel(id, level);
        }

        private void fillTable(Game game) {
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
        }

        // ----- L'arbre lui-même -----

        @Test
        void troisBranchesDontChaqueCaseDemandeCelleDuDessus() {
            Map<DarkUpgrade.Branch, Integer> count = new EnumMap<>(DarkUpgrade.Branch.class);
            Map<DarkUpgrade.Branch, String> previous = new EnumMap<>(DarkUpgrade.Branch.class);
            for (DarkUpgrade dark : DarkUpgrades.DEFAULT) {
                count.merge(dark.branch(), 1, Integer::sum);
                if (!dark.branch().inTree()) continue;         // celles payées en matière noire ne sont pas dans l'arbre
                assertEquals(previous.get(dark.branch()), dark.requires());   // la case du dessus
                if (dark.requires() != null) {                 // plus on descend, plus il faut de matière noire
                    int above = DarkUpgrades.DEFAULT.stream().filter(d -> d.id().equals(dark.requires()))
                            .findFirst().orElseThrow().darkMatter();
                    assertTrue(dark.darkMatter() >= above, () -> dark.name());
                } else {
                    assertEquals(1, dark.darkMatter());        // la première case de chaque branche : dès la première explosion
                }
                previous.put(dark.branch(), dark.id());
            }
            assertEquals(8, count.get(DarkUpgrade.Branch.PARTICLES).intValue());
            assertEquals(8, count.get(DarkUpgrade.Branch.ATOMS).intValue());
            assertEquals(6, count.get(DarkUpgrade.Branch.SIZE).intValue());
            assertEquals(4, count.get(DarkUpgrade.Branch.DARK_MATTER).intValue());
            assertEquals(26, DarkUpgrades.DEFAULT.stream().map(DarkUpgrade::id).distinct().count());
            assertEquals(26, DarkUpgrades.DEFAULT.stream().map(DarkUpgrade::name).distinct().count());
        }

        @Test
        void fermeAvantLaPremiereExplosion() {
            Game game = gameWithParticles(1e9);
            assertFalse(game.isDarkAvailable("dark_density"));
            assertFalse(game.canBuyDark("dark_density"));
            assertFalse(game.buyDark("dark_density"));
            assertValue(1e9, game.state().particles());
        }

        @Test
        void uneCaseDemandeCelleDuDessus() {
            Game game = darkGame();
            game.state().setParticles(BigNum.of(1, 30));
            assertTrue(game.isDarkAvailable("dark_density"));
            assertFalse(game.isDarkAvailable("dark_swarm"));
            assertFalse(game.buyDark("dark_swarm"));
            assertTrue(game.buyDark("dark_density"));
            assertTrue(game.isDarkAvailable("dark_swarm"));
            assertTrue(game.buyDark("dark_swarm"));
        }

        @Test
        void uneCaseDemandeAssezDeMatiereNoire() {
            Game game = darkGame();
            game.state().setDarkMatter(BigNum.ONE);           // juste après la première explosion
            game.state().setParticles(BigNum.of(1, 30));
            assertTrue(game.buyDark("dark_density"));          // la première case : 1 matière noire suffit
            assertFalse(game.hasDarkMatterFor("dark_swarm"));  // la deuxième en demande 2
            assertFalse(game.isDarkAvailable("dark_swarm"));
            assertFalse(game.buyDark("dark_swarm"));
            game.state().setDarkMatter(BigNum.of(2));
            assertTrue(game.buyDark("dark_swarm"));
            assertEquals(2, game.state().darkMatter().toDouble()); // la matière noire n'est pas dépensée
        }

        @Test
        void lesParticulesSontDepenseesEtLePrixMonte() {
            Game game = darkGame();
            game.state().setParticles(BigNum.of(2.5, 3));
            assertValue(1e3, game.darkCostOf("dark_density"));
            assertTrue(game.buyDark("dark_density"));
            assertValue(1.5e3, game.state().particles());
            assertEquals(1, game.darkLevelOf("dark_density"));
            assertValue(1e43, game.darkCostOf("dark_density"));
            assertFalse(game.canBuyDark("dark_density"));
        }

        @Test
        void lesAtomesSontDepenses() {
            Game game = darkGame();
            game.state().setAtoms(BigNum.of(30));
            assertTrue(game.buyDark("dark_nucleus"));          // 10 atomes
            assertValue(20, game.state().atoms());
            assertValue(25, game.darkCostOf("dark_nucleus"));
            assertFalse(game.buyDark("dark_nucleus"));
        }

        @Test
        void laTailleEstUnSeuilPasUnPrix() {
            Game game = darkGame();
            assertFalse(game.canBuyDark("dark_inflation"));   // un proton, il faut 1 nanomètre
            game.state().setDarkMatterSize(BigNum.of(2, -9));
            assertTrue(game.buyDark("dark_inflation"));
            assertValue(2e-9, game.state().darkMatterSize()); // la matière noire n'a pas rétréci
            assertValue(1e-3, game.darkCostOf("dark_inflation"));
            assertFalse(game.canBuyDark("dark_inflation"));
        }

        @Test
        void uneCaseAUnNombreMaximalDeNiveaux() {
            Game game = darkGame();
            game.state().setDarkMatterSize(BigNum.of(1, 40));
            int bought = 0;
            while (game.buyDark("dark_inflation")) bought++;
            assertEquals(5, bought);
            assertTrue(game.isDarkMaxed("dark_inflation"));
            assertFalse(game.canBuyDark("dark_inflation"));
        }

        @Test
        void lExplosionNeReprendPasLesCasesAcquises() {
            Game game = darkGame();
            give(game, "dark_density", 2);
            give(game, "dark_inflation", 3);
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(2, game.darkLevelOf("dark_density"));
            assertEquals(3, game.darkLevelOf("dark_inflation"));
        }

        @Test
        void catalogueIncoherentRefuse() {
            DarkUpgrade orphan = new DarkUpgrade("a", "A", DarkUpgrade.Branch.ATOMS, BigNum.ONE, 1, 1, "absent", 0,
                    new DarkEffect.UncapAtoms());
            assertThrows(IllegalArgumentException.class,
                    () -> new Game(new GameState(), List.of(SPEED), List.of(), List.of(orphan), new Random(1)));
            DarkUpgrade root = new DarkUpgrade("a", "A", DarkUpgrade.Branch.ATOMS, BigNum.ONE, 1, 1, null, 0,
                    new DarkEffect.UncapAtoms());
            assertThrows(IllegalArgumentException.class,
                    () -> new Game(new GameState(), List.of(SPEED), List.of(), List.of(root, root), new Random(1)));
            assertThrows(IllegalArgumentException.class, () -> new DarkUpgrade("a", "A", DarkUpgrade.Branch.ATOMS,
                    BigNum.ZERO, 1, 1, null, 0, new DarkEffect.UncapAtoms()));
            assertThrows(IllegalArgumentException.class, () -> newGame().buyDark("inconnue"));
        }

        @Test
        void lesPrixEnParticulesEtEnAtomesSArrondissentMaisPasLesTailles() {
            DarkUpgrade multi = DarkUpgrades.DEFAULT.stream().filter(d -> d.id().equals("dark_multisynthesis")).findFirst().orElseThrow();
            assertValue(266, multi.costAt(2));                // 118 × 1,5² = 265,5
            DarkUpgrade inflation = DarkUpgrades.DEFAULT.stream().filter(d -> d.id().equals("dark_inflation")).findFirst().orElseThrow();
            assertValue(1e-9, inflation.costAt(0));
        }

        // ----- Branche des atomes -----

        @Test
        void noyauLourdAjouteUnAtomeParFusionEtParNiveau() {
            Game game = gameReadyToFuse();
            assertValue(1, game.atomsPerFusion());
            give(game, "dark_nucleus", 1);
            assertValue(2, game.atomsPerFusion());
            give(game, "dark_nucleus", 4);
            assertValue(5, game.atomsPerFusion());
            game.state().setElementCount(18, 1);              // argon ×1,15 : il multiplie aussi les atomes ajoutés
            assertValue(5.75, game.atomsPerFusion());
            assertTrue(game.fuse());
            assertValue(5.75, game.state().atoms());
        }

        @Test
        void syntheseMultipleTirePlusieursElements() {
            Game game = newGame();
            unlock(game);
            game.state().setAtoms(BigNum.of(100));
            assertEquals(1, game.elementsPerSynthesis());
            give(game, "dark_multisynthesis", 2);
            assertEquals(3, game.elementsPerSynthesis());
            List<Element> obtained = game.synthesize();
            assertEquals(3, obtained.size());
            assertEquals(3, game.ownedCopies());
            assertValue(98, game.state().atoms());            // un seul prix pour les trois
            assertEquals(1, game.state().synthesisCount());
        }

        @Test
        void syntheseMultipleSArreteQuandLeTableauEstComplet() {
            Game game = newGame();
            unlock(game);
            fillTable(game);
            game.state().setElementCount(26, 7);              // il manque deux exemplaires de fer
            give(game, "dark_multisynthesis", 4);
            game.state().setAtoms(BigNum.of(118));
            assertEquals(2, game.synthesize().size());
            assertTrue(game.isPeriodicTableComplete());
        }

        @Test
        void memoireDesMachinesGardeLesAutomatismes() {
            Game game = newGame();
            unlock(game);                                     // tous achetés, à la cadence maximale, coupés
            game.setAutomationEnabled("autoSpeed", true);
            give(game, "dark_machines", 1);
            fillTable(game);
            assertTrue(game.explode());

            assertTrue(game.ownsAutomation("autoSlow"));
            assertEquals(2, game.automationSpeedLevel("autoSlow"));
            assertTrue(game.isAutomationEnabled("autoSpeed"));
            assertFalse(game.isAutomationEnabled("autoGen")); // coupé avant, coupé après
            assertEquals(0, game.levelOf("keep"));            // les améliorations, elles, sont perdues
            assertTrue(game.isAutomationUnlocked());          // mais l'onglet reste, puisqu'il y a des automatismes
            assertFalse(game.isPeriodicTableUnlocked());      // le tableau, lui, attend de nouveau 30 atomes créés
        }

        @Test
        void unAutomatismeGardeContinueDAgirApresLExplosion() {
            Game game = newGame();
            unlock(game);
            game.setAutomationEnabled("autoSpeed", true);
            give(game, "dark_machines", 1);
            fillTable(game);
            game.explode();
            game.state().setParticles(BigNum.of(10));         // le prix d'un niveau de vitesse
            game.tick(0.2);
            assertEquals(1, game.levelOf("speed"));
        }

        @Test
        void memoireDeLaMatiereGardeLesAmeliorationsSaufLesGenerateurs() {
            Game game = gameReadyToFuse();                    // 3 générateurs, vitesse niveau 1
            game.state().setAtoms(BigNum.of(50));
            game.buy("double");
            game.buy("double");
            game.buy("keep");
            give(game, "dark_memory", 1);
            fillTable(game);
            assertTrue(game.explode());

            assertEquals(1, game.levelOf("speed"));
            assertEquals(2, game.levelOf("double"));
            assertEquals(1, game.levelOf("keep"));
            assertEquals(0, game.levelOf("gen"));
            assertEquals(1, game.generatorCount());
            assertValue(0, game.state().atoms());             // les atomes et les éléments, eux, ont disparu
            assertEquals(0, game.discoveredElements());
            assertFalse(game.ownsAutomation("autoSpeed"));    // et les automatismes aussi, sans l'autre case
        }

        @Test
        void coupDePouceAvanceLElementUniqueGaranti() {
            Game game = newGame();
            unlock(game);
            assertEquals(8, game.guaranteedUniqueSynthesis());
            give(game, "dark_luck", 1);
            assertEquals(3, game.guaranteedUniqueSynthesis());
            for (int synthesis = 1; synthesis <= 2; synthesis++) {
                game.state().setAtoms(BigNum.of(118));
                game.synthesize();
                for (int number : List.copyOf(game.state().elements().keySet())) game.state().setElementCount(number, 0);
            }
            game.state().setAtoms(BigNum.of(118));
            assertTrue(game.synthesize().get(0).category().unique());   // la troisième
        }

        @Test
        void tableauEntameGardeLesElementsUniques() {
            Game game = darkGame();
            give(game, "dark_relics", 1);
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(22, game.discoveredElements());      // 7 gaz nobles et 15 actinides
            for (int number : game.state().elements().keySet()) {
                assertTrue(PeriodicTable.element(number).category().unique());
                assertEquals(1, game.elementCount(number));
            }
            assertTrue(game.isSynthesisAutomationUnlocked());
            assertValue(1.15, BigNum.of(game.elementMultiplier(ElementEffect.Stat.ATOMS) / 1.25));  // argon et oganesson agissent encore
        }

        @Test
        void tableauEntameNeGardeQueCeQuiEtaitObtenu() {
            Game game = darkGame();
            give(game, "dark_relics", 1);
            give(game, "dark_chain", 1);
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertEquals(22, game.discoveredElements());
            assertEquals(0, game.elementCount(26));           // le fer, lui, a disparu
        }

        @Test
        void reactionEnChainePermetDExploserDesLes118ElementsDecouverts() {
            Game game = darkGame();
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertFalse(game.isPeriodicTableComplete());
            assertFalse(game.canExplode());
            give(game, "dark_chain", 1);
            assertTrue(game.canExplode());
            game.state().setElementCount(26, 0);              // il manque un élément : plus possible
            assertFalse(game.canExplode());
            game.state().setElementCount(26, 1);
            assertTrue(game.explode());
        }

        @Test
        void isotopesRepousseLeMaximumDExemplaires() {
            Game game = newGame();
            unlock(game);
            assertEquals(9, game.maxCopiesOf(ElementCategory.TRANSITION_METAL));
            assertEquals(811, game.maxTotalCopies());
            give(game, "dark_isotopes", 1);
            assertEquals(16, game.maxCopiesOf(ElementCategory.TRANSITION_METAL));
            assertEquals(9, game.maxCopiesOf(ElementCategory.LANTHANIDE));
            assertEquals(1, game.maxCopiesOf(ElementCategory.NOBLE_GAS));   // un élément unique le reste
            assertEquals(81 * 16 + 15 * 9 + 22, game.maxTotalCopies());
            give(game, "dark_isotopes", 2);
            assertEquals(25, game.maxCopiesOf(ElementCategory.HALOGEN));
            assertEquals(16, game.maxCopiesOf(ElementCategory.LANTHANIDE));
        }

        @Test
        void avecIsotopesUnTableauPleinAncienneManiereNEstPlusComplet() {
            Game game = newGame();
            unlock(game);
            fillTable(game);                                  // 9 exemplaires de chaque
            assertTrue(game.isPeriodicTableComplete());
            give(game, "dark_isotopes", 1);
            assertFalse(game.isPeriodicTableComplete());
            assertFalse(game.isElementMaxed(26));
            assertFalse(game.canExplode());
            game.state().setAtoms(BigNum.of(118));
            int obtained = game.synthesize().size();          // on peut de nouveau synthétiser
            assertTrue(obtained >= 1);
            assertEquals(811 + obtained, game.ownedCopies());
        }

        // ----- Branche des particules -----

        @Test
        void densiteDoubleLesParticulesDeBaseAChaqueNiveau() {
            Game game = darkGame();
            assertValue(1, game.particlesPerCreation());
            give(game, "dark_density", 1);
            assertValue(2, game.particlesPerCreation());
            give(game, "dark_density", 3);
            assertValue(8, game.particlesPerCreation());
            game.tick(ONE_PARTICLE);
            assertValue(8, game.state().particles());
        }

        @Test
        void sansEssaimLeNombreDeGenerateursResteLimite() {
            Game game = gameWithParticles(1e30);
            while (game.buy("gen")) { /* autant que possible */ }
            assertEquals(3, game.generatorCount());
            assertEquals(3, game.maxGeneratorCount());
            assertTrue(game.isMaxed("gen"));
            assertEquals(1, game.fusionGroups());
        }

        @Test
        void essaimLeveLaLimiteDesGenerateurs() {
            Game game = gameWithParticles(1e30);
            give(game, "dark_swarm", 1);
            assertEquals(3, game.generatorsPerAtom());        // il en faut toujours trois pour fusionner
            assertEquals(100, game.maxGeneratorCount());
            for (int i = 0; i < 6; i++) assertTrue(game.buy("gen"));
            assertEquals(7, game.generatorCount());
            assertFalse(game.isMaxed("gen"));
            assertValue(BASE * 7, game.productionPerSecond());
            game.tick(ONE_PARTICLE);                          // les sept générateurs produisent
            assertEquals(7, total(game) - game.state().particles().toDouble() + 7, 1e-6);
        }

        @Test
        void avecEssaimLaFusionRapporteUnAtomeParGroupeDeGenerateurs() {
            Game game = gameWithParticles(1e30);
            give(game, "dark_swarm", 1);
            for (int i = 0; i < 2; i++) game.buy("gen");      // 3 générateurs : un groupe
            assertEquals(1, game.fusionGroups());
            assertValue(1, game.atomsPerFusion());
            for (int i = 0; i < 4; i++) game.buy("gen");      // 7 générateurs : deux groupes complets
            assertEquals(2, game.fusionGroups());
            assertValue(2, game.atomsPerFusion());
            assertTrue(game.fuse());
            assertValue(2, game.state().atoms());
            assertEquals(1, game.generatorCount());           // tous consommés
        }

        @Test
        void essaimSArreteASaLimite() {
            Game game = gameWithParticles(1e300);
            give(game, "dark_swarm", 1);
            game.state().setLevel("gen", 99);
            assertEquals(100, game.generatorCount());
            assertTrue(game.isMaxed("gen"));
            assertFalse(game.buy("gen"));
            assertEquals(33, game.fusionGroups());
        }

        @Test
        void germesFaitDemarrerAvecPlusDeGenerateurs() {
            Game game = darkGame();
            assertEquals(1, game.generatorCount());
            give(game, "dark_seeds", 1);
            assertEquals(1, game.startingGenerators());
            assertEquals(2, game.generatorCount());
            game.state().setParticles(BigNum.of(1e9));
            assertTrue(game.buy("gen"));                      // il n'en reste qu'un à acheter
            assertEquals(3, game.generatorCount());
            assertTrue(game.isMaxed("gen"));
            assertFalse(game.buy("gen"));
            assertTrue(game.fuse());
            assertEquals(2, game.generatorCount());           // la partie repart avec deux générateurs
        }

        @Test
        void germesNeDonneJamaisPlusQuIlNEnFautPourFusionner() {
            Game game = darkGame();
            give(game, "dark_seeds", 9);
            assertEquals(2, game.startingGenerators());       // trois générateurs au plus dans ce catalogue
            assertEquals(3, game.generatorCount());
            assertTrue(game.canFuse());                       // on peut fusionner d'entrée
            assertTrue(game.fuse());
            assertTrue(game.canFuse());
        }

        @Test
        void germesNExistePasAvantLeDemarrage() {
            Game game = unstartedGame();
            give(game, "dark_seeds", 2);
            assertEquals(0, game.generatorCount());
        }

        @Test
        void resonanceMultiplieLesParticulesParLeCarreDesAnneesLumiere() {
            Game game = darkGame();
            give(game, "dark_resonance", 1);
            assertValue(1, game.particlesPerCreation());      // bien moins d'une année-lumière : aucun effet
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(0.5));
            assertValue(1, game.particlesPerCreation());
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(10));
            assertValue(100, game.particlesPerCreation());
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(1e6));
            assertValue(1e12, game.particlesPerCreation());
            give(game, "dark_density", 1);                    // se multiplie avec la densité
            assertValue(2e12, game.particlesPerCreation());
        }

        @Test
        void sansResonanceLaTailleNeChangeRienAuxParticules() {
            Game game = darkGame();
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(1e6));
            assertValue(1, game.particlesPerCreation());
        }

        @Test
        void sansSeuilLaFusionAutomatiqueFusionneDesQuePossible() {
            Game game = gameWithParticles(1e30);
            give(game, "dark_swarm", 1);
            assertFalse(game.isFusionThresholdUnlocked());
            assertFalse(game.setFusionThreshold(6));
            assertEquals(3, game.fusionThreshold());
        }

        @Test
        void leSeuilFaitAttendreLaFusionAutomatique() {
            Game game = gameWithParticles(1e30);
            game.state().setAtoms(BigNum.of(100));
            game.buy("keep");
            createEnoughAtoms(game);
            game.buyAutomation("autoFusion");                 // agit toutes les 0,1 s
            give(game, "dark_swarm", 1);
            give(game, "dark_threshold", 1);
            assertTrue(game.setFusionThreshold(6));
            assertEquals(6, game.fusionThreshold());
            double atoms = game.state().atoms().toDouble();

            for (int i = 0; i < 4; i++) game.buy("gen");      // 5 générateurs : assez pour fusionner, pas pour le seuil
            game.tick(1);
            assertEquals(5, game.generatorCount());
            assertValue(atoms, game.state().atoms());

            game.buy("gen");                                  // le sixième : deux groupes de trois
            game.tick(0.2);
            assertEquals(1, game.generatorCount());
            assertValue(atoms + 2, game.state().atoms());
        }

        @Test
        void leSeuilNEmpechePasDeFusionnerALaMain() {
            Game game = gameReadyToFuse();
            give(game, "dark_swarm", 1);
            give(game, "dark_threshold", 1);
            game.setFusionThreshold(50);
            assertTrue(game.canFuse());
            assertTrue(game.fuse());
        }

        @Test
        void leSeuilResteEntreLeMinimumEtLaLimiteDeGenerateurs() {
            Game game = darkGame();
            give(game, "dark_threshold", 1);
            assertEquals(3, game.fusionThreshold());          // pas encore réglé
            game.setFusionThreshold(1);
            assertEquals(3, game.fusionThreshold());
            game.setFusionThreshold(500);
            assertEquals(3, game.fusionThreshold());          // sans l'Essaim, la limite est de trois
            give(game, "dark_swarm", 1);
            game.setFusionThreshold(500);
            assertEquals(100, game.fusionThreshold());
        }

        @Test
        void amorcageOffreDesNiveauxDeVitesse() {
            // Un catalogue avec une seule amélioration de vitesse, comme le vrai jeu.
            Game game = new Game(new GameState(), List.of(SPEED, GENERATOR), List.of(), new Random(1));
            game.start();
            assertEquals(0, game.startingSpeedLevels());
            give(game, "dark_priming", 1);
            assertEquals(10, game.startingSpeedLevels());
            assertValue(BASE * 1024, game.speed());           // dix niveaux à ×2 dans ce catalogue
            assertValue(10, game.costOf("speed"));            // le prix, lui, ne bouge pas
            game.state().setParticles(BigNum.of(10));
            game.buy("speed");
            assertValue(BASE * 2048, game.speed());
            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen");
            game.buy("gen");
            game.fuse();                                      // la fusion efface le niveau acheté, pas les niveaux offerts
            assertValue(BASE * 1024, game.speed());
        }

        @Test
        void masseSombreDoubleLesParticulesPourChaqueMatiereNoire() {
            Game game = darkGame();
            game.state().setDarkMatter(BigNum.ONE);
            give(game, "dark_mass", 1);
            assertValue(2, game.particlesPerCreation());      // 1 matière noire
            game.state().setDarkMatter(BigNum.of(5));
            assertValue(32, game.particlesPerCreation());
            game.state().setDarkMatter(BigNum.of(1, 30));     // une quantité absurde reste calculable
            assertTrue(game.particlesPerCreation().gt(BigNum.of(1, 100)));
        }

        @Test
        void automatismesVifsAbaisseLeDelaiMinimal() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(100));
            game.buy("keep");
            createEnoughAtoms(game);
            game.buyAutomation("autoSpeed");                  // au délai minimal dès l'achat
            assertEquals(0.1, game.minAutomationInterval());
            assertEquals(0.1, game.automationInterval("autoSpeed"));
            give(game, "dark_reflexes", 1);
            assertEquals(0.05, game.minAutomationInterval());
            assertEquals(0.05, game.automationInterval("autoSpeed"));

            game.state().setParticles(BigNum.of(1e12));
            game.tick(0.5);                                   // dix actions en une demi-seconde, au lieu de cinq
            assertEquals(10, game.levelOf("speed"));
        }

        // ----- Branche de la taille -----

        @Test
        void inflationDoubleLaCroissanceAChaqueNiveau() {
            Game game = darkGame();
            game.state().setDarkMatter(BigNum.ONE);
            double unit = Game.DARK_MATTER_GROWTH_PER_UNIT;
            assertEquals(unit, game.darkMatterMomentum(), 1e-12);
            give(game, "dark_inflation", 1);
            assertEquals(2 * unit, game.darkMatterMomentum(), 1e-12);
            give(game, "dark_inflation", 5);
            assertEquals(32 * unit, game.darkMatterMomentum(), 1e-12);
            game.growDarkMatter(1);
            assertValue(START * Math.pow(1 + 0.1 * 32 * unit, 10), game.state().darkMatterSize());
        }

        @Test
        void sansExpansionSpontaneeLeTempsNeFaitRienGrossir() {
            Game game = darkGame();
            game.tick(100_000);
            assertValue(START, game.state().darkMatterSize());
            assertEquals(0, game.darkAutoExpansionShare());
        }

        @Test
        void expansionSpontaneeGrossitAUnMilliemeDeLaVitesseDAppui() {
            Game held = darkGame();
            held.growDarkMatter(1);
            Game idle = darkGame();
            give(idle, "dark_spontaneous", 1);
            assertEquals(0.001, idle.darkAutoExpansionShare());
            idle.tick(1000);                                  // mille secondes sans appuyer = une seconde d'appui
            assertEquals(held.state().darkMatterSize().toDouble(), idle.state().darkMatterSize().toDouble(), START * 1e-9);
        }

        @Test
        void expansionSpontaneeEstDixFoisPlusForteAChaqueNiveau() {
            Game game = darkGame();
            give(game, "dark_spontaneous", 2);
            assertEquals(0.01, game.darkAutoExpansionShare(), 1e-12);
            give(game, "dark_spontaneous", 3);
            assertEquals(0.1, game.darkAutoExpansionShare(), 1e-12);
        }

        @Test
        void leVerrouSeDebloqueDansLArbre() {
            Game game = darkGame();
            assertFalse(game.isHoldLockUnlocked());
            give(game, "dark_lock", 1);
            assertTrue(game.isHoldLockUnlocked());
        }

        @Test
        void ondeDeFusionFaitGrossirLaMatiereNoireAChaqueFusion() {
            Game game = gameReadyToFuse();
            game.state().setDarkMatter(BigNum.ONE);
            game.state().setExplosions(1);
            assertEquals(0, game.darkFusionPulse());
            assertTrue(game.fuse());
            assertValue(START, game.state().darkMatterSize()); // sans l'amélioration, rien ne bouge

            // Deux parties identiques : l'une fusionne avec l'onde, l'autre reçoit 0,2 s d'appui à la main.
            Game pulsed = gameReadyToFuse();
            Game pressed = gameReadyToFuse();
            for (Game each : List.of(pulsed, pressed)) {
                each.state().setDarkMatter(BigNum.ONE);
                each.state().setExplosions(1);
            }
            give(pulsed, "dark_wave", 1);
            assertEquals(0.2, pulsed.darkFusionPulse());
            pressed.growDarkMatter(0.2);                      // avec la production d'avant la fusion
            assertTrue(pulsed.fuse());
            assertTrue(pulsed.state().darkMatterSize().gt(Game.DARK_MATTER_START_SIZE));
            assertEquals(pressed.state().darkMatterSize(), pulsed.state().darkMatterSize());
        }

        @Test
        void condensationAjouteDeLaMatiereNoireAChaqueExplosion() {
            Game game = darkGame();
            assertValue(1, game.darkMatterPerExplosion());
            give(game, "dark_condensation", 2);
            assertValue(3, game.darkMatterPerExplosion());
            fillTable(game);
            assertTrue(game.explode());
            assertValue(20 + 3, game.state().darkMatter());
            assertEquals(2, game.state().explosions());
        }

        @Test
        void debordementLeveLePlafondDAtomes() {
            Game game = gameReadyToFuse();
            game.state().setAtoms(Game.MAX_ATOMS);
            assertTrue(game.isAtomCapReached());
            assertFalse(game.canFuse());
            give(game, "dark_overflow", 1);
            assertTrue(game.isAtomCapLifted());
            assertFalse(game.isAtomCapReached());
            assertTrue(game.fuse());
            assertValue(119, game.state().atoms());
        }

        @Test
        void sansDebordementLesAtomesRestentPlafonnes() {
            Game game = gameReadyToFuse();
            give(game, "dark_nucleus", 50);                   // 51 atomes par fusion
            game.state().setAtoms(BigNum.of(100));
            assertTrue(game.fuse());
            assertValue(118, game.state().atoms());
            assertValue(51, game.state().totalAtoms());
        }

        @Test
        void lePrixDeLaSyntheseRestePlafonneMemeSansPlafondDAtomes() {
            Game game = newGame();
            unlock(game);
            give(game, "dark_overflow", 1);
            game.state().setSynthesisCount(50);
            assertValue(118, game.synthesisCost());
        }
    }

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
            game.tick(40);                                    // 0,25 par seconde : 10 particules
            assertValue(10, game.stats().particlesCreated());
            assertValue(10, game.stats().runParticlesCreated());
            assertEquals(40, game.stats().runTime(), 1e-9);
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
            assertValue(0.25, game.stats().bestProduction());
            game.state().setLevel("gen", 2);
            assertValue(0.75, game.stats().bestProduction());
            game.state().setLevel("gen", 0);                  // la production retombe, pas le record
            assertValue(0.75, game.stats().bestProduction());
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
            assertValue(1.5, stats.bestProduction());         // trois générateurs à vitesse doublée, relevée avant la remise à zéro

            fuseAgain(game);                                  // tout de suite : une partie de zéro seconde
            assertEquals(2, stats.fusions());
            assertEquals(0, stats.lastFusionTime(), 1e-9);
            assertEquals(0, stats.fastestFusionTime(), 1e-9);
            game.tick(50);
            fuseAgain(game);
            assertEquals(50, stats.lastFusionTime(), 1e-9);
            assertEquals(0, stats.fastestFusionTime(), 1e-9); // le record reste
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
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
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
            assertEquals(Math.log10(0.25), first.production(), 1e-9);   // un générateur, 0,25 particule par seconde
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
            assertEquals(30.5, last.fusionTime(), 1e-9);
            assertEquals(2.0 / 118, last.elementsShare(), 1e-12);
            assertEquals(4.0 / Game.maxCopies(), last.copiesShare(), 1e-12);
            assertEquals(Math.log10(1.15), last.atomsPerFusion(), 1e-9);    // l'argon
        }

        @Test
        void leReleveGardeLeSommetDeProductionDAvantLaFusion() {
            Game game = gameReadyToFuse();                        // trois générateurs à vitesse doublée : 1,5 par seconde
            game.tick(StatsHistory.FIRST_INTERVAL);
            assertTrue(game.fuse());                              // la production retombe à 0,25
            assertValue(0.25, game.productionPerSecond());
            game.tick(StatsHistory.FIRST_INTERVAL);
            List<StatsHistory.Sample> samples = game.stats().history().samples();
            assertEquals(Math.log10(1.5), samples.get(samples.size() - 1).production(), 1e-9);
            game.tick(StatsHistory.FIRST_INTERVAL);               // plus de fusion depuis : le relevé suivant dit la production du moment
            samples = game.stats().history().samples();
            assertEquals(Math.log10(0.25), samples.get(samples.size() - 1).production(), 1e-9);
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
            game.tick(40);
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
        void ellesSontQuatreEtHorsDeLArbre() {
            Game game = gameWithDarkMatter(1);
            List<DarkUpgrade> shop = game.darkUpgrades(DarkUpgrade.Branch.DARK_MATTER);
            assertEquals(4, shop.size());
            for (DarkUpgrade dark : shop) {
                assertFalse(dark.branch().inTree());
                assertEquals(null, dark.requires());
                assertTrue(game.isDarkAvailable(dark.id()));   // dès la première matière noire
            }
            assertEquals(8, game.darkUpgrades(DarkUpgrade.Branch.PARTICLES).size());
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
        void depenserNeRefermeRien() {
            Game game = gameWithDarkMatter(2);
            double momentum = game.darkMatterMomentum();
            assertTrue(game.isDarkAutomationUnlocked("dark_auto_start"));
            assertTrue(game.hasDarkMatterFor("dark_swarm"));
            assertTrue(game.buyDark("dark_shop_synthesis"));      // tout y passe
            assertValue(0, game.state().darkMatter());
            assertTrue(game.isDarkAutomationUnlocked("dark_auto_start"));
            assertTrue(game.hasDarkMatterFor("dark_swarm"));
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
            Game game = new Game();
            game.start();
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
            assertEquals(4, game.darkAutomations().size());
            for (DarkAutomation automation : game.darkAutomations()) {
                assertFalse(game.isDarkAutomationUnlocked(automation.id()));
                assertFalse(game.setDarkAutomationEnabled(automation.id(), true));
                assertFalse(game.isDarkAutomationEnabled(automation.id()));
            }
            game.state().setDarkMatter(BigNum.of(2));
            assertTrue(game.isDarkAutomationUnlocked("dark_auto_start"));
            assertFalse(game.isDarkAutomationUnlocked("dark_auto_atoms"));
            assertFalse(game.isDarkAutomationEnabled("dark_auto_start"));   // débloqué, mais coupé au départ
            assertTrue(game.setDarkAutomationEnabled("dark_auto_start", true));
            assertTrue(game.isDarkAutomationEnabled("dark_auto_start"));
            assertValue(2, game.state().darkMatter());
            game.state().setDarkMatter(BigNum.of(6));
            for (DarkAutomation automation : game.darkAutomations()) {
                assertTrue(game.isDarkAutomationUnlocked(automation.id()));
            }
            assertThrows(IllegalArgumentException.class, () -> game.isDarkAutomationUnlocked("inconnu"));
        }

        @Test
        void lesAutomatismesOrdinairesSontOffertsDesLeDebutDeLaPartie() {
            Game game = gameWithDarkMatter(2);
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
            Game game = gameWithDarkMatter(2);
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
            Game game = gameWithDarkMatter(2);
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
            Game game = gameWithDarkMatter(2);
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
            Game game = gameWithDarkMatter(2);
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
            Game game = gameWithDarkMatter(2);
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
            Game game = gameWithDarkMatter(3);
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
            Game game = gameWithDarkMatter(4);
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
            Game game = gameWithDarkMatter(4);
            game.state().setAtoms(BigNum.of(100));
            game.setDarkAutomationEnabled("dark_auto_machines", true);
            game.tick(5);
            assertValue(100, game.state().atoms());
        }

        @Test
        void lExplosionAutomatiqueAttendUnTableauComplet() {
            Game game = gameWithDarkMatter(6);
            game.setDarkAutomationEnabled("dark_auto_explosion", true);
            game.tick(5);
            assertEquals(1, game.state().explosions());       // rien à faire exploser
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            game.tick(1.05);
            assertEquals(2, game.state().explosions());
            assertValue(7, game.state().darkMatter());
            assertEquals(0, game.discoveredElements());
            assertTrue(game.isDarkAutomationEnabled("dark_auto_explosion"));   // l'explosion ne le coupe pas
        }

        @Test
        void coupeIlNeFaitRien() {
            Game game = gameWithDarkMatter(6);
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
         * Garde-fou sur l'arrivée de l'automatisation et du tableau périodique : un joueur qui
         * dépense ses atomes au fur et à mesure dans les améliorations les moins chères (sans
         * économiser pour Persistance) doit créer son trentième atome entre 2 h 45 et 3 h 45.
         * Plus tôt, le début perd son intérêt ; plus tard, il faut tout racheter à la main des
         * dizaines de fois.
         */
        @Test
        void automatisationEtTableauArriventEntre2h45Et3h45() {
            Game game = new Game();
            game.start();
            double minutes = 0;
            while (!game.isAutomationUnlocked() && minutes < 10 * 60) {
                minutes += minutesUntilFusion(game);
                assertTrue(game.fuse());
                boolean bought = true;
                while (bought) {
                    Upgrade cheapest = null;
                    for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
                        if (upgrade.id().equals("atom_keep") || !game.canBuy(upgrade.id())) continue;
                        if (cheapest == null || game.costOf(upgrade.id()).lt(game.costOf(cheapest.id()))) cheapest = upgrade;
                    }
                    bought = cheapest != null && game.buy(cheapest.id());
                }
            }
            assertTrue(game.isAutomationUnlocked());
            assertTrue(game.isPeriodicTableUnlocked());       // les deux se débloquent ensemble
            assertEquals(0, game.levelOf("atom_keep"));
            assertValue(25, game.costOf("atom_keep"));
            assertTrue(minutes >= 165 && minutes <= 225, "trentième atome créé après " + minutes + " min");
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

    /** Fait comme si la partie avait déjà créé les atomes qui débloquent l'automatisation et le tableau périodique. */
    private static void createEnoughAtoms(Game game) {
        game.state().setTotalAtoms(game.state().totalAtoms().max(Game.UNLOCK_TOTAL_ATOMS));
    }

    /** Débloque le tableau, achète Persistance et tous les automatismes disponibles, les porte à leur cadence maximale, puis les coupe. */
    private static void unlock(Game game) {
        BigNum atoms = game.state().atoms();
        game.state().setAtoms(BigNum.of(100));
        game.buy("keep");
        createEnoughAtoms(game);
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

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
            game.state().setElementCount(36, 1);      // krypton : tous les automatismes 40 % plus rapides
            assertEquals(1 / 1.4, game.automationInterval("autoSlow"), 1e-12);
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
            // Les autres familles se partagent leurs 6 %. L'ensemble des lanthanides, complet, double la
            // chance des deux familles rares qui restent : 3 % et 2 % comptent pour 6 % et 4 %.
            assertEquals(0.30 / 0.99, game.categoryChance(ElementCategory.TRANSITION_METAL), 1e-12);
            assertEquals(0.06 / 0.99, game.categoryChance(ElementCategory.NOBLE_GAS), 1e-12);
        }

        @Test
        void leTirageDoubleNeDonneQuUnElementSIlNEnRestequUn() {
            Game game = unlockedGame();
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            game.state().setElementCount(26, 8);              // il ne manque qu'un exemplaire de fer
            // Tableau presque complet : 85 % de tirage double avec les halogènes, 10 % de plus avec leur ensemble.
            assertEquals(0.95, game.doubleDrawChance(), 1e-12);
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
            // Quatre gaz nobles sur sept : la moitié de leur ensemble, soit le quart de son ×1,25 partout.
            double half = Math.pow(1.25, ElementSet.HALF_STRENGTH);
            assertValue(2.5 * half, game.particlesPerCreation());
            assertValue(BASE * 1.875 * half, game.speed());
            assertValue(1.15 * 1.25 * half, game.atomsPerFusion());
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
            assertEquals(4 / 1.09, game.automationInterval("s"), 1e-12);
            assertEquals(4, game.automationInterval("g"));

            game.state().setElementCount(11, 1);              // sodium : achat des générateurs
            assertEquals(4 / 1.09, game.automationInterval("g"), 1e-12);
            game.state().setElementCount(19, 1);              // potassium : fusion
            game.state().setElementCount(37, 1);              // rubidium : synthèse
            // Quatre alcalins sur six : la moitié de leur ensemble, soit le quart de ses +20 % pour tous.
            for (String id : List.of("s", "g", "f", "y")) assertEquals(4 / 1.14, game.automationInterval(id), 1e-12);
        }

        @Test
        void cesiumEtFranciumAccelerentTousLesAutomatismes() {
            Game game = cadenceGame();
            game.state().setElementCount(55, 1);              // césium : tous, +6 %
            for (String id : List.of("s", "g", "f", "y")) {
                assertEquals(4 / 1.06, game.automationInterval(id), 1e-12);
            }
            game.state().setElementCount(3, 1);               // lithium en plus : les bonus s'additionnent
            assertEquals(4 / 1.15, game.automationInterval("s"), 1e-12);
            assertEquals(4 / 1.06, game.automationInterval("g"), 1e-12);
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
        void leRendementDuJeuDemarreACinquanteMillionsDeParticulesParSeconde() {
            Game game = new Game();
            game.start();
            Upgrade upgrade = game.upgrades().stream().filter(u -> u.id().equals("atom_yield")).findFirst().orElseThrow();
            assertEquals(Resource.ATOMS, upgrade.resource());
            assertEquals(new Effect.MultiplyAtomsByProduction(50_000_000, 0.09), upgrade.effect());
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
        void laReserveEstVideParDefautEtNEstJamaisNegative() {
            Game game = unlockedGame(100);
            assertEquals(BigNum.ZERO, game.synthesisReserve());
            game.setSynthesisReserve(BigNum.of(25));
            assertValue(25, game.synthesisReserve());
            game.setSynthesisReserve(BigNum.of(-3));
            assertEquals(BigNum.ZERO, game.synthesisReserve());
        }

        @Test
        void elleNEntamePasLaReserve() {
            Game game = unlockedGame(100);
            game.state().setElementCount(92, 1);
            game.buyAutomation("autoSynth");                  // une action toutes les 2 secondes
            game.state().setAtoms(BigNum.of(10));
            game.setSynthesisReserve(BigNum.of(9));           // 2 atomes de prix + 9 de réserve : il en faut 11
            game.tick(10);
            assertEquals(0, game.state().synthesisCount());
            assertValue(10, game.state().atoms());

            game.setSynthesisReserve(BigNum.of(8));           // il en faut 10 : la synthèse part
            game.tick(0.2);
            assertEquals(1, game.state().synthesisCount());
            assertValue(8, game.state().atoms());
            game.tick(10);                                    // la suivante coûte 4 : 8 atomes ne suffisent plus
            assertEquals(1, game.state().synthesisCount());
        }

        @Test
        void laSyntheseALaMainIgnoreLaReserve() {
            Game game = unlockedGame(10);
            game.setSynthesisReserve(BigNum.of(100));
            assertFalse(game.synthesize().isEmpty());
            assertValue(8, game.state().atoms());
        }

        @Test
        void uneReserveTropHauteBloqueLaSyntheseSousLePlafondDAtomes() {
            Game game = unlockedGame(100);
            game.setSynthesisReserve(BigNum.of(100));         // 2 + 100 atomes : possible
            assertFalse(game.isSynthesisReserveBlocking());
            game.setSynthesisReserve(BigNum.of(117));         // 2 + 117 dépassent le plafond de 118
            assertTrue(game.isSynthesisReserveBlocking());
            game.state().setDarkLevel("dark_overflow", 1);    // plafond levé : plus de blocage
            assertFalse(game.isSynthesisReserveBlocking());
        }

        @Test
        void laReserveSurvitALExplosionMaisPasALaRemiseAZero() {
            Game game = unlockedGame(100);
            game.setSynthesisReserve(BigNum.of(30));
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertValue(30, game.synthesisReserve());
            game.reset();
            assertEquals(BigNum.ZERO, game.synthesisReserve());
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
        void impossibleTantQuIlManqueUnElement() {
            Game game = advancedGame();
            assertFalse(game.canExplode());
            fillTable(game);
            game.state().setElementCount(26, 0);              // il manque le fer
            assertFalse(game.canExplode());
            assertFalse(game.explode());
            assertEquals(0, game.state().explosions());
            assertValue(0, game.state().darkMatter());
            assertEquals(9, game.elementCount(29));           // rien n'a été effacé
            assertFalse(game.isDarkMatterUnlocked());
        }

        @Test
        void unExemplaireDeChaqueElementSuffit() {
            Game game = advancedGame();
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertFalse(game.isPeriodicTableComplete());      // il reste des exemplaires à obtenir
            assertTrue(game.canExplode());
            assertTrue(game.explode());
            assertValue(1, game.state().darkMatter());
        }

        @Test
        void ilManqueDesExemplairesMaisPasDElement() {
            Game game = advancedGame();
            fillTable(game);
            game.state().setElementCount(26, 8);              // il manque un exemplaire de fer : sans importance
            assertTrue(game.canExplode());
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
        void chaqueExplosionAlourditLeTableau() {
            Game game = advancedGame();
            assertValue(1, game.tableWeight());
            assertValue(118, game.atomCap());
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(1, game.state().tableWeightLevel());
            assertValue(Game.TABLE_WEIGHT_GROWTH, game.tableWeight());
            assertValue(118 * Game.TABLE_WEIGHT_GROWTH, game.atomCap());
            fillTable(game);
            assertTrue(game.explode());
            assertValue(118 * Game.TABLE_WEIGHT_GROWTH * Game.TABLE_WEIGHT_GROWTH, game.atomCap());
        }

        @Test
        void unTableauPlusLourdReleveLePlafondDAtomesEtLePrixDesSyntheses() {
            Game game = advancedGame();
            game.state().setTableWeightLevel(1);              // comme après une explosion
            BigNum cap = game.atomCap();
            game.state().setAtoms(BigNum.of(118));
            assertFalse(game.isAtomCapReached());             // 118 n'est plus le plafond
            game.state().setAtoms(cap);
            assertTrue(game.isAtomCapReached());
            game.state().setSynthesisCount(40);               // le prix a doublé bien au-delà du plafond
            assertValue(cap.toDouble(), game.synthesisCost());
            game.state().setSynthesisCount(3);                // en dessous, il double comme avant
            assertValue(16, game.synthesisCost());
        }

        @Test
        void leTableauCesseDeSAlourdirApresUnCertainNombreDExplosions() {
            Game game = advancedGame();
            game.state().setTableWeightLevel(Game.TABLE_WEIGHT_MAX_LEVEL);
            BigNum heaviest = game.atomCap();
            game.state().setTableWeightLevel(Game.TABLE_WEIGHT_MAX_LEVEL + 5);
            assertValue(heaviest.toDouble(), game.atomCap());
            assertValue(Math.pow(Game.TABLE_WEIGHT_GROWTH, Game.TABLE_WEIGHT_MAX_LEVEL), game.tableWeight());
        }

        @Test
        void chaqueMatiereNoireOuvreQuelqueChoseJusquALaCondensation() {
            // Tant qu'une explosion ne rapporte qu'une matière noire, aucune ne doit rester sans rien ouvrir.
            int condensation = DarkUpgrades.DEFAULT.stream()
                    .filter(dark -> dark.effect() instanceof DarkEffect.AddDarkMatterPerExplosion)
                    .mapToInt(DarkUpgrade::darkMatter).findFirst().orElseThrow();
            for (int earned = 1; earned <= condensation; earned++) {
                int threshold = earned;
                boolean opens = DarkUpgrades.DEFAULT.stream().anyMatch(dark -> dark.branch().inTree() && dark.darkMatter() == threshold)
                        || DarkAutomations.DEFAULT.stream().anyMatch(automation -> automation.darkMatter().toDouble() == threshold)
                        || Challenges.DEFAULT.stream().anyMatch(challenge -> challenge.darkMatter() == threshold);
                assertTrue(opens, "rien ne s'ouvre a " + threshold + " matieres noires");
            }
        }

        @Test
        void laRemiseAZeroRendAuTableauSaMasseDuDebut() {
            Game game = advancedGame();
            fillTable(game);
            assertTrue(game.explode());
            game.reset();
            assertEquals(0, game.state().tableWeightLevel());
            assertValue(118, game.atomCap());
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
            // Le premier palier de taille (le noyau d'uranium) double les particules : l'avancement en tient compte.
            assertValue(Math.exp(UNIT * game.darkMatterProgressFactor() / Math.pow(1000, 0.1)), game.darkMatterGrowthPerSecond());
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
            assertEquals(7, count.get(DarkUpgrade.Branch.ATOMS).intValue());
            assertEquals(6, count.get(DarkUpgrade.Branch.SIZE).intValue());
            assertEquals(5, count.get(DarkUpgrade.Branch.DARK_MATTER).intValue());
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
            // L'argon (×1,15) et l'oganesson (×1,25) agissent encore, l'ensemble des gaz nobles (×1,25) est resté
            // complet, et la 7ᵉ période est à moitié réunie : le quart de son ×1,5.
            assertValue(1.15 * 1.25 * Math.pow(1.5, ElementSet.HALF_STRENGTH),
                    BigNum.of(game.elementMultiplier(ElementEffect.Stat.ATOMS) / 1.25));
            assertEquals(2, game.completedSets());            // les gaz nobles et les actinides
            assertEquals(1, game.halfSets());                 // la 7ᵉ période : 15 actinides et l'oganesson sur 32
        }

        @Test
        void tableauEntameNeGardeQueCeQuiEtaitObtenu() {
            Game game = darkGame();
            give(game, "dark_relics", 1);
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertEquals(22, game.discoveredElements());
            assertEquals(0, game.elementCount(26));           // le fer, lui, a disparu
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
            assertTrue(game.canExplode());                    // tous découverts : l'explosion n'attend pas les exemplaires
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
            assertValue(2.2, game.atomsPerFusion());          // deux groupes, plus la prime de 10 % du second
            assertTrue(game.fuse());
            assertValue(2.2, game.state().atoms());
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
            // Les paliers de taille multiplient eux aussi les particules : on les met de côté.
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(0.5));
            assertValue(1, game.particlesPerCreation().divide(game.landmarkParticlesMultiplier()));
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(10));
            assertValue(100, game.particlesPerCreation().divide(game.landmarkParticlesMultiplier()));
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(1e6));
            assertValue(1e12, game.particlesPerCreation().divide(game.landmarkParticlesMultiplier()));
            give(game, "dark_density", 1);                    // se multiplie avec la densité
            assertValue(2e12, game.particlesPerCreation().divide(game.landmarkParticlesMultiplier()));
        }

        @Test
        void sansResonanceLaTailleNeChangeRienAuxParticules() {
            Game game = darkGame();
            game.state().setDarkMatterSize(SizeScale.LIGHT_YEAR.multiply(1e6));
            // Seuls les paliers de taille comptent : la taille elle-même ne multiplie rien.
            assertValue(1, game.particlesPerCreation().divide(game.landmarkParticlesMultiplier()));
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
            assertValue(atoms + 2.2, game.state().atoms());   // deux groupes et leur prime
        }

        @Test
        void laPrimeDeGroupeGranditAvecLeNombreDeGroupes() {
            Game game = gameWithParticles(1e300);
            assertEquals(1, game.fusionGroups());
            assertEquals(1, game.fusionGroupBonus(), 1e-12);  // un seul groupe : pas de prime
            give(game, "dark_swarm", 1);
            game.state().setLevel("gen", 99);                 // 100 générateurs : 33 groupes de trois
            assertEquals(33, game.fusionGroups());
            assertEquals(1 + Game.FUSION_GROUP_BONUS * 32, game.fusionGroupBonus(), 1e-12);
            assertValue(33 * 4.2, game.atomsPerFusion());
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
        void unReleveGardeUneValeurParStatistique() {
            Game game = newGame();
            game.tick(StatsHistory.FIRST_INTERVAL);
            StatsHistory.Sample first = game.stats().history().samples().get(0);
            assertEquals(StatsHistory.Stat.values().length, first.values().length);
            assertEquals(1, first.value(StatsHistory.Stat.GENERATORS));
            assertEquals(0, first.value(StatsHistory.Stat.FUSIONS));
            assertEquals(0, first.value(StatsHistory.Stat.EXPLOSIONS));
            assertEquals(Math.log10(0.25), first.value(StatsHistory.Stat.SPEED), 1e-9);
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
        void ellesSontCinqEtHorsDeLArbre() {
            Game game = gameWithDarkMatter(1);
            List<DarkUpgrade> shop = game.darkUpgrades(DarkUpgrade.Branch.DARK_MATTER);
            assertEquals(5, shop.size());
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
        void uneSecondeDeProductionPaieLaBrancheDesParticules() {
            Game game = gameWithDarkMatter(3);                    // trois générateurs à vitesse doublée : 1,5 par seconde
            game.state().setParticles(BigNum.ZERO);
            assertValue(1.5, game.darkBalance(DarkUpgrade.Branch.PARTICLES));
            game.state().setParticles(BigNum.of(40));
            assertValue(40, game.darkBalance(DarkUpgrade.Branch.PARTICLES));   // les particules en main, si elles valent plus
            assertFalse(game.canBuyDark("dark_density"));         // 1 000 particules : ni l'un ni l'autre n'y suffit
        }

        @Test
        void depenserNeRefermeRien() {
            Game game = gameWithDarkMatter(3);
            double momentum = game.darkMatterMomentum();
            assertTrue(game.isDarkAutomationUnlocked("dark_auto_start"));
            assertTrue(game.hasDarkMatterFor("dark_swarm"));
            assertTrue(game.buyDark("dark_shop_synthesis"));      // 2 matières noires
            assertTrue(game.buyDark("dark_shop_atoms"));          // et la dernière : tout y passe
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
            game.state().setDarkMatter(BigNum.of(3));
            assertTrue(game.isDarkAutomationUnlocked("dark_auto_start"));
            assertFalse(game.isDarkAutomationUnlocked("dark_auto_atoms"));
            assertFalse(game.isDarkAutomationEnabled("dark_auto_start"));   // débloqué, mais coupé au départ
            assertTrue(game.setDarkAutomationEnabled("dark_auto_start", true));
            assertTrue(game.isDarkAutomationEnabled("dark_auto_start"));
            assertValue(3, game.state().darkMatter());
            game.state().setDarkMatter(BigNum.of(14));
            for (DarkAutomation automation : game.darkAutomations()) {
                assertTrue(game.isDarkAutomationUnlocked(automation.id()));
            }
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

    @Nested
    class PaliersDeVitesse {

        /** Une vitesse qui n'accélère rien, mais triple les particules tous les cinq niveaux. */
        private final Upgrade stepped = new Upgrade("speed", "Vitesse", Resource.PARTICLES,
                BigNum.of(1), 1, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(1, 5, 3));

        private Game steppedGame() {
            Game game = new Game(new GameState(), List.of(stepped, GENERATOR, KEEP), List.of(AUTO_FUSION), new Random(42));
            game.start();
            game.state().setParticles(BigNum.of(1e6));
            return game;
        }

        @Test
        void rienAvantLePremierPalier() {
            Game game = steppedGame();
            assertEquals(4, game.buy("speed", 4));
            assertEquals(0, game.speedMilestones());
            assertEquals(BigNum.ONE, game.speedMilestoneMultiplier());
            assertValue(1, game.particlesPerCreation());
            assertEquals(5, game.nextSpeedMilestone("speed"));
        }

        @Test
        void chaquePalierMultiplieLesParticulesEtPasLaVitesse() {
            Game game = steppedGame();
            game.buy("speed", 5);
            assertEquals(1, game.speedMilestones());
            assertValue(3, game.particlesPerCreation());
            assertValue(BASE, game.speed());                  // l'animation ne s'emballe pas
            assertValue(BASE * 3, game.productionPerSecond());
            assertEquals(10, game.nextSpeedMilestone("speed"));

            game.buy("speed", 6);                             // niveau 11 : deux paliers
            assertEquals(2, game.speedMilestones());
            assertValue(9, game.particlesPerCreation());
            assertEquals(15, game.nextSpeedMilestone("speed"));
        }

        @Test
        void lesNiveauxOffertsParLaMatiereNoireComptent() {
            Game game = steppedGame();
            game.state().setDarkLevel("dark_priming", 1);     // dix niveaux de vitesse offerts
            assertEquals(10, game.speedLevels("speed"));
            assertEquals(2, game.speedMilestones());
            assertValue(9, game.particlesPerCreation());
            game.buy("speed", 5);
            assertEquals(3, game.speedMilestones());
        }

        @Test
        void laFusionLesReprendSaufAvecPersistance() {
            Game game = steppedGame();
            game.buy("speed", 5);
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertEquals(0, game.speedMilestones());

            Game keeper = steppedGame();
            keeper.state().setAtoms(BigNum.of(1));
            keeper.buy("keep");
            keeper.buy("speed", 5);
            keeper.buy("gen", 2);
            assertTrue(keeper.fuse());
            assertEquals(1, keeper.speedMilestones());
        }

        @Test
        void uneVitesseSansPalierNEnAJamais() {
            Game game = gameWithParticles(1e30);
            game.buy("speed", 60);
            assertEquals(0, game.speedMilestones());
            assertEquals(BigNum.ONE, game.speedMilestoneMultiplier());
            assertEquals(0, game.nextSpeedMilestone("speed"));
            assertEquals(0, game.nextSpeedMilestone("gen"));   // pas une amélioration de vitesse
        }

        @Test
        void unPalierMalDefiniEstRefuse() {
            assertThrows(IllegalArgumentException.class, () -> new Effect.MultiplySpeed(1.1, -1, 2));
            assertThrows(IllegalArgumentException.class, () -> new Effect.MultiplySpeed(1.1, 25, 0.5));
            assertFalse(new Effect.MultiplySpeed(1.1).hasMilestones());
            assertEquals(3, new Effect.MultiplySpeed(1.1, 25, 2).milestonesAt(80));
        }

        @Test
        void dansLeJeuLesParticulesDoublentTousLes25Niveaux() {
            Game game = new Game();
            game.start();
            Upgrade speed = game.upgrades().stream().filter(u -> u.id().equals("speed")).findFirst().orElseThrow();
            assertEquals(new Effect.MultiplySpeed(1.10, Upgrades.SPEED_MILESTONE_EVERY, 2), speed.effect());
            assertEquals(25, game.nextSpeedMilestone("speed"));
            game.state().setLevel("speed", 24);
            assertValue(1, game.particlesPerCreation());
            game.state().setLevel("speed", 25);
            assertValue(2, game.particlesPerCreation());
            game.state().setLevel("speed", 75);
            assertValue(8, game.particlesPerCreation());
        }
    }

    @Nested
    class Couplage {

        /** +50 % de particules par niveau et par autre générateur. */
        private final Upgrade coupling = new Upgrade("coupling", "Couplage", Resource.PARTICLES,
                BigNum.of(1), 1, Upgrade.NO_LIMIT, new Effect.MultiplyByGenerators(0.5));

        private Game coupledGame() {
            Game game = new Game(new GameState(), List.of(GENERATOR, coupling, KEEP),
                    List.of(Automation.buying("autoCoupling", "Couplage", BigNum.of(2), "coupling").withCadence(0, 0, BigNum.ONE, 1)),
                    new Random(42));
            game.start();
            game.state().setParticles(BigNum.of(1e6));
            return game;
        }

        @Test
        void sansEffetAvecUnSeulGenerateur() {
            Game game = coupledGame();
            assertEquals(2, game.buy("coupling", 2));
            assertValue(1, game.particlesPerCreation());
            assertValue(1, game.particlesMultiplier("coupling", 2));
            assertValue(BASE, game.productionPerSecond());
        }

        @Test
        void chaqueAutreGenerateurAjouteSaPart() {
            Game game = coupledGame();
            game.buy("coupling", 2);
            game.buy("gen");                                  // 2 générateurs : 1 + 0,5 × 2 × 1
            assertValue(2, game.particlesPerCreation());
            game.buy("gen");                                  // 3 générateurs : 1 + 0,5 × 2 × 2
            assertValue(3, game.particlesPerCreation());
            assertValue(3, game.particlesMultiplier("coupling", 2));
            assertValue(4, game.particlesMultiplier("coupling", 3));   // ce que donnerait un niveau de plus
            assertValue(BASE * 3 * 3, game.productionPerSecond());
        }

        @Test
        void laProductionAuMomentDeFusionnerCompteTousLesGenerateurs() {
            Game game = coupledGame();
            game.buy("coupling", 2);                          // un seul générateur pour l'instant
            assertValue(BASE, game.productionPerSecond());
            assertValue(BASE * 3 * 3, game.productionAtFusion());   // comme s'ils étaient déjà trois
        }

        @Test
        void laFusionLeReprendSaufAvecPersistance() {
            Game game = coupledGame();
            game.buy("coupling", 3);
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertEquals(0, game.levelOf("coupling"));

            Game keeper = coupledGame();
            keeper.state().setAtoms(BigNum.of(1));
            keeper.buy("keep");
            keeper.buy("coupling", 3);
            keeper.buy("gen", 2);
            assertTrue(keeper.fuse());
            assertEquals(3, keeper.levelOf("coupling"));
        }

        @Test
        void sonAutomatismeLAcheteToutSeul() {
            Game game = coupledGame();
            game.state().setAtoms(BigNum.of(10));
            createEnoughAtoms(game);
            assertTrue(game.buyAutomation("autoCoupling"));
            game.tick(0.35);                                  // une action tous les 0,1 s
            assertEquals(3, game.levelOf("coupling"));
        }

        @Test
        void unBonusNegatifEstRefuse() {
            assertThrows(IllegalArgumentException.class, () -> new Effect.MultiplyByGenerators(-0.1));
        }

        @Test
        void leJeuASonCouplageEtSonAutomatisme() {
            Game game = new Game();
            game.start();
            Upgrade upgrade = game.upgrades(Resource.PARTICLES).stream()
                    .filter(u -> u.id().equals("coupling")).findFirst().orElseThrow();
            assertEquals(new Effect.MultiplyByGenerators(0.02), upgrade.effect());
            Automation automation = game.automations().stream()
                    .filter(a -> a.id().equals("auto_coupling")).findFirst().orElseThrow();
            assertEquals("coupling", automation.upgradeId());
            // Trois améliorations en particules : la vitesse, le couplage et les générateurs.
            assertEquals(3, game.upgrades(Resource.PARTICLES).size());
        }
    }

    @Nested
    class AchatsGroupes {

        /** Vitesse à 10, 15, 23, 34, 51… particules ; générateur à 100 puis 1000, deux au plus. */
        private Game simpleGame(double particles) {
            Game game = new Game(new GameState(), List.of(SPEED, GENERATOR), List.of(AUTO_FUSION), new Random(42));
            game.start();
            game.state().setParticles(BigNum.of(particles));
            return game;
        }

        @Test
        void leCoutDePlusieursNiveauxEstLaSommeDesCouts() {
            Game game = simpleGame(0);
            assertValue(10, game.costOf("speed", 1));
            assertValue(10 + 15 + 23, game.costOf("speed", 3));
            game.state().setLevel("speed", 1);
            assertValue(15 + 23, game.costOf("speed", 2));
        }

        @Test
        void leCoutSArreteAuMaximumDeLAmelioration() {
            Game game = simpleGame(0);
            assertValue(100 + 1000, game.costOf("gen", 50));  // deux générateurs à acheter, pas cinquante
            game.state().setLevel("gen", 2);
            assertValue(10000, game.costOf("gen", 50));       // au maximum : le prix qu'aurait le suivant
        }

        @Test
        void onCompteCeQueLeJoueurPeutPayer() {
            Game game = simpleGame(50);                       // 10 + 15 + 23 = 48
            assertEquals(3, game.affordableLevels("speed", 10));
            assertEquals(2, game.affordableLevels("speed", 2));
            assertEquals(3, game.affordableLevels("speed", Integer.MAX_VALUE));
            assertEquals(0, game.affordableLevels("gen", 10));
            game.state().setParticles(BigNum.of(1e9));
            assertEquals(2, game.affordableLevels("gen", 10)); // limité par le maximum
        }

        @Test
        void unAchatGroupePrendCeQuiEstAPortee() {
            Game game = simpleGame(50);
            assertEquals(3, game.buy("speed", 10));
            assertEquals(3, game.levelOf("speed"));
            assertValue(2, game.state().particles());
            assertEquals(0, game.buy("speed", 10));           // plus rien à portée
        }

        @Test
        void unAchatGroupeNeDepassePasLeNombreDemande() {
            Game game = simpleGame(1e9);
            assertEquals(2, game.buy("speed", 2));
            assertEquals(2, game.levelOf("speed"));
            assertEquals(2, game.buy("gen", Integer.MAX_VALUE));
            assertEquals(3, game.generatorCount());
        }

        @Test
        void rienNeSAcheteAvantLeDemarrage() {
            Game game = unstartedGame();
            game.state().setParticles(BigNum.of(1e9));
            assertEquals(0, game.affordableLevels("speed", 10));
            assertEquals(0, game.buy("speed", 10));
            assertEquals(0, game.buyAllWithParticles());
        }

        @Test
        void toutAcheterPrendChaqueFoisLeMoinsCher() {
            Game game = simpleGame(130);                      // vitesse : 10, 15, 23, 34 = 82 ; la suivante vaut 51
            assertEquals(4, game.buyAllWithParticles());
            assertEquals(4, game.levelOf("speed"));
            assertEquals(1, game.generatorCount());           // le générateur à 100 n'a jamais été le moins cher payable
            assertValue(48, game.state().particles());
        }

        @Test
        void toutAcheterVaJusquAuBout() {
            Game game = simpleGame(1000);
            int bought = game.buyAllWithParticles();
            assertEquals(bought, game.levelOf("speed") + game.levelOf("gen"));
            assertEquals(2, game.generatorCount());           // le premier générateur (100) est passé, pas le second (1000)
            assertFalse(game.canBuy("speed"));
            assertFalse(game.canBuy("gen"));
            assertEquals(0, game.buyAllWithParticles());
        }

        @Test
        void lesAchatsGroupesComptentDansLesStatistiques() {
            Game game = simpleGame(50);
            game.buy("speed", 10);
            assertEquals(3, game.stats().particleUpgradesBought());
        }
    }

    @Nested
    class TempsRestant {

        @Test
        void leTempsAvantUnAchatSuitLaProduction() {
            Game game = newGame();                            // 0,25 particule par seconde
            assertEquals(40, game.secondsUntilParticles(BigNum.of(10)), 1e-6);
            game.state().setParticles(BigNum.of(5));
            assertEquals(20, game.secondsUntilParticles(BigNum.of(10)), 1e-6);
            game.state().setParticles(BigNum.of(10));
            assertEquals(0, game.secondsUntilParticles(BigNum.of(10)), 0);
        }

        @Test
        void lAttenteDiminueAvecLeTempsEtPasSeulementAChaqueParticule() {
            Game game = newGame();                            // une particule toutes les 4 secondes
            assertEquals(4, game.secondsUntilParticles(BigNum.of(1)), 1e-6);
            game.tick(1);                                     // un quart de la création est fait
            assertEquals(3, game.secondsUntilParticles(BigNum.of(1)), 1e-6);
            game.tick(2.5);
            assertEquals(0.5, game.secondsUntilParticles(BigNum.of(1)), 1e-6);
            assertEquals(4.5, game.secondsUntilParticles(BigNum.of(2)), 1e-6);   // la suivante, quatre secondes plus tard
            game.tick(0.5);                                   // la particule arrive : rien ne saute
            assertEquals(0, game.secondsUntilParticles(BigNum.of(1)), 0);
            assertEquals(4, game.secondsUntilParticles(BigNum.of(2)), 1e-6);
        }

        @Test
        void lAttenteCompteLesCreationsDeChaqueGenerateur() {
            Game game = gameWithParticles(1100);
            game.buy("gen", 2);                               // trois générateurs, plus rien en poche
            game.tick(1);
            // Trois créations en cours, toutes au quart : les trois particules arrivent ensemble dans 3 s,
            // alors que la moyenne (2 particules manquantes à 0,75 par seconde) annoncerait 2,7 s.
            assertEquals(3, game.secondsUntilParticles(BigNum.of(2)), 1e-6);
            assertEquals(7, game.secondsUntilParticles(BigNum.of(4)), 1e-6);   // puis la fournée suivante
        }

        @Test
        void auxGrandsNombresLAttenteEstLaMoyenne() {
            Game game = gameWithParticles(0);
            game.state().setLevel("huge", 1);                  // une vitesse gigantesque
            BigNum cost = game.productionPerSecond().multiply(90);
            assertEquals(90, game.secondsUntilParticles(cost), 1e-6);
        }

        @Test
        void sansProductionLAttenteEstInfinie() {
            Game game = unstartedGame();
            assertEquals(Double.POSITIVE_INFINITY, game.secondsUntilParticles(BigNum.of(10)), 0);
        }

        @Test
        void lesFusionsAvantUnAchatSuiventLesAtomesParFusion() {
            Game game = newGame();                            // 1 atome par fusion
            game.state().setAtoms(BigNum.of(1));
            assertEquals(0, game.fusionsUntilAtoms(BigNum.of(1)));
            assertEquals(3, game.fusionsUntilAtoms(BigNum.of(4)));
            game.state().setAtoms(BigNum.of(1.5));
            assertEquals(3, game.fusionsUntilAtoms(BigNum.of(4)));   // 2,5 atomes manquent : trois fusions
        }

        @Test
        void unPrixAuDessusDuPlafondDAtomesEstHorsDePortee() {
            Game game = newGame();
            assertEquals(-1, game.fusionsUntilAtoms(BigNum.of(200)));
            game.state().setDarkLevel("dark_overflow", 1);    // plafond levé
            assertEquals(200, game.fusionsUntilAtoms(BigNum.of(200)));
        }
    }

    @Nested
    class Surcharge {

        private final Upgrade overload = new Upgrade("over", "Surcharge", Resource.ATOMS,
                BigNum.of(1), 1, Upgrade.NO_LIMIT, new Effect.Overload(1.5));

        private Game overloadGame() {
            Game game = new Game(new GameState(), List.of(SPEED, GENERATOR, DOUBLE, overload), List.of(AUTO_FUSION), new Random(42));
            game.start();
            game.state().setAtoms(BigNum.of(50));
            return game;
        }

        @Test
        void chaqueNiveauMultiplieLesParticules() {
            Game game = overloadGame();
            game.buy("over");
            assertValue(1.5, game.particlesPerCreation());
            game.buy("over");
            assertValue(2.25, game.particlesPerCreation());
            assertValue(2.25, game.particlesMultiplier("over", 2));
            assertValue(48, game.state().atoms());
        }

        @Test
        void lesElementsNeLaRenforcentPas() {
            Game game = overloadGame();
            game.buy("over");
            game.buy("double");
            game.state().setElementCount(1, 1);               // l'hydrogène renforce le Dédoublement
            assertTrue(game.particlesMultiplier("double", 1).gt(BigNum.of(2)));
            assertValue(1.5, game.particlesMultiplier("over", 1));
        }

        @Test
        void laFusionLaConserve() {
            Game game = overloadGame();
            game.buy("over");
            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertEquals(1, game.levelOf("over"));
        }

        @Test
        void uneSurchargeQuiReduitEstRefusee() {
            assertThrows(IllegalArgumentException.class, () -> new Effect.Overload(0.9));
        }

        @Test
        void dansLeJeuElleResteSousLePlafondDAtomesPendantTreizeNiveaux() {
            Game game = new Game();
            game.start();
            Upgrade upgrade = game.upgrades(Resource.ATOMS).stream()
                    .filter(u -> u.id().equals("atom_overload")).findFirst().orElseThrow();
            assertEquals(new Effect.Overload(1.3), upgrade.effect());
            assertFalse(upgrade.hasLimit());
            assertValue(12, game.costOf("atom_overload"));
            game.state().setLevel("atom_overload", 12);
            assertTrue(game.costOf("atom_overload").lte(Game.MAX_ATOMS));
            game.state().setLevel("atom_overload", 13);
            assertTrue(game.costOf("atom_overload").gt(Game.MAX_ATOMS));
            assertEquals(-1, game.fusionsUntilAtoms(game.costOf("atom_overload")));
        }
    }

    @Nested
    class Ensembles {

        private Game tableGame() {
            Game game = newGame();
            unlock(game);
            return game;
        }

        private void giveAll(Game game, ElementSet set) {
            for (Element element : set.members()) game.state().setElementCount(element.number(), 1);
        }

        private ElementSet family(Game game, ElementCategory category) {
            return game.elementSets().stream().filter(set -> set.category() == category).findFirst().orElseThrow();
        }

        private ElementSet period(Game game, int period) {
            return game.elementSets().stream().filter(set -> set.period() == period).findFirst().orElseThrow();
        }

        @Test
        void ilYADixFamillesEtSeptPeriodes() {
            Game game = tableGame();
            assertEquals(17, game.elementSets().size());
            assertEquals(10, game.elementSets().stream().filter(ElementSet::isFamily).count());
            assertEquals(17, game.elementSets().stream().map(ElementSet::id).distinct().count());
            assertEquals(0, game.completedSets());
        }

        @Test
        void chaqueElementAUnePeriode() {
            assertEquals(1, PeriodicTable.period(1));
            assertEquals(1, PeriodicTable.period(2));
            assertEquals(2, PeriodicTable.period(3));
            assertEquals(3, PeriodicTable.period(18));
            assertEquals(6, PeriodicTable.period(57));         // le lanthane
            assertEquals(6, PeriodicTable.period(71));
            assertEquals(7, PeriodicTable.period(89));         // l'actinium
            assertEquals(7, PeriodicTable.period(118));
            int[] sizes = {2, 8, 8, 18, 18, 32, 32};
            int total = 0;
            for (int p = 1; p <= PeriodicTable.PERIODS; p++) {
                assertEquals(sizes[p - 1], PeriodicTable.elementsOfPeriod(p).size());
                total += PeriodicTable.elementsOfPeriod(p).size();
            }
            assertEquals(118, total);
            assertThrows(IllegalArgumentException.class, () -> PeriodicTable.period(0));
            assertThrows(IllegalArgumentException.class, () -> PeriodicTable.elementsOfPeriod(8));
        }

        @Test
        void unEnsembleSeRemplitElementParElement() {
            Game game = tableGame();
            ElementSet halogens = family(game, ElementCategory.HALOGEN);
            assertEquals(6, halogens.members().size());
            assertEquals(0, game.setProgress(halogens));
            game.state().setElementCount(9, 1);                // le fluor
            game.state().setElementCount(17, 5);               // cinq exemplaires de chlore : un seul élément
            assertEquals(2, game.setProgress(halogens));
            assertFalse(game.isSetComplete(halogens));
            giveAll(game, halogens);
            assertEquals(6, game.setProgress(halogens));
            assertTrue(game.isSetComplete(halogens));
            assertEquals(1, game.completedSets());
        }

        @Test
        void unEnsembleCompletAjouteSonBonus() {
            Game game = tableGame();
            ElementSet earths = family(game, ElementCategory.ALKALINE_EARTH_METAL);
            List<Element> members = earths.members();
            double sum = 0;
            for (Element element : members.subList(0, members.size() - 1)) {
                game.state().setElementCount(element.number(), 1);
                sum += ((ElementEffect.Add) element.effect()).bonus();
            }
            // Il en manque un : seulement le quart du bonus, celui de la moitié réunie.
            assertEquals((1 + sum) * Math.pow(1.15, ElementSet.HALF_STRENGTH),
                    game.elementMultiplier(ElementEffect.Stat.ATOMS), 1e-12);

            Element last = members.get(members.size() - 1);
            game.state().setElementCount(last.number(), 1);
            sum += ((ElementEffect.Add) last.effect()).bonus();
            assertEquals((1 + sum) * 1.15, game.elementMultiplier(ElementEffect.Stat.ATOMS), 1e-12);
        }

        @Test
        void laMoitieDUnEnsembleDonneLeQuartDeSonBonus() {
            Game game = tableGame();
            ElementSet earths = family(game, ElementCategory.ALKALINE_EARTH_METAL);   // six éléments, ×1,15 d'atomes
            assertEquals(3, earths.halfSize());
            List<Element> members = earths.members();
            double sum = 0;
            for (Element element : members.subList(0, 2)) {
                game.state().setElementCount(element.number(), 1);
                sum += ((ElementEffect.Add) element.effect()).bonus();
            }
            assertEquals(0, game.setStrength(earths), 0);
            assertEquals(1 + sum, game.elementMultiplier(ElementEffect.Stat.ATOMS), 1e-12);

            game.state().setElementCount(members.get(2).number(), 1);          // le troisième : la moitié
            sum += ((ElementEffect.Add) members.get(2).effect()).bonus();
            assertEquals(ElementSet.HALF_STRENGTH, game.setStrength(earths), 0);
            assertEquals(1, game.halfSets());
            assertEquals(0, game.completedSets());
            assertEquals((1 + sum) * Math.pow(1.15, ElementSet.HALF_STRENGTH),
                    game.elementMultiplier(ElementEffect.Stat.ATOMS), 1e-12);
        }

        @Test
        void laMoitieSArronditAuDessus() {
            Game game = tableGame();
            assertEquals(2, period(game, 1).halfSize());       // deux éléments : pas de premier palier
            game.state().setElementCount(2, 1);
            assertEquals(0, game.setStrength(period(game, 1)), 0);
            assertEquals(4, family(game, ElementCategory.NONMETAL).halfSize());        // sept
            assertEquals(8, family(game, ElementCategory.LANTHANIDE).halfSize());      // quinze
            assertEquals(19, family(game, ElementCategory.TRANSITION_METAL).halfSize());
        }

        @Test
        void unePeriodeCompleteAjouteSonBonus() {
            Game game = tableGame();
            game.state().setElementCount(2, 1);                // l'hélium seul
            assertEquals(2, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-9);
            game.state().setElementCount(1, 1);                // avec l'hydrogène, la première période est complète
            assertTrue(game.isSetComplete(period(game, 1)));
            assertEquals(2 * 3, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-9);
        }

        @Test
        void unElementCompteALaFoisPourSaFamilleEtSaPeriode() {
            Game game = tableGame();
            giveAll(game, period(game, 2));                    // du lithium au néon
            assertEquals(1, game.completedSets());
            assertEquals(0, game.halfSets());                  // un élément par famille : aucune n'est à moitié
            assertEquals(1, game.setProgress(family(game, ElementCategory.NOBLE_GAS)));   // le néon
            assertEquals(1, game.setProgress(family(game, ElementCategory.HALOGEN)));     // le fluor
        }

        @Test
        void lExplosionVideLesEnsembles() {
            Game game = tableGame();
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertEquals(17, game.completedSets());
            assertTrue(game.explode());
            assertEquals(0, game.completedSets());
        }

        @Test
        void unEnsembleReunitUneFamilleOuUnePeriode() {
            ElementEffect effect = new ElementEffect.Luck(1);
            assertThrows(IllegalArgumentException.class, () -> new ElementSet("x", "X", null, 0, effect));
            assertThrows(IllegalArgumentException.class, () -> new ElementSet("x", "X", ElementCategory.HALOGEN, 2, effect));
            assertThrows(IllegalArgumentException.class, () -> new ElementSet("x", "X", null, 8, effect));
            assertTrue(ElementSet.family(ElementCategory.HALOGEN, effect).isFamily());
            assertFalse(ElementSet.period(3, effect).isFamily());
        }
    }

    @Nested
    class SyntheseCiblee {

        private static final double MAX = Game.MAX_ATOMS.toDouble();

        /** Tableau débloqué, plein d'atomes, sans automatisme en marche, synthèse ciblée achetée. */
        private Game tableGame() {
            Game game = newGame();
            unlock(game);
            game.state().setAtoms(BigNum.of(MAX));
            game.state().setDarkLevel("dark_shop_target", 1);
            return game;
        }

        @Test
        void elleSAcheteAvecDeLaMatiereNoire() {
            Game game = newGame();
            unlock(game);
            game.state().setAtoms(BigNum.of(MAX));
            assertFalse(game.isTargetedSynthesisUnlocked());
            assertFalse(game.setSynthesisTarget(ElementCategory.HALOGEN));   // verrouillée : la synthèse reste au hasard
            assertEquals(null, game.synthesisTarget());
            assertEquals(1, game.synthesize().size());

            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(2));
            assertTrue(game.buyDark("dark_shop_target"));
            assertValue(0, game.state().darkMatter());
            assertTrue(game.isTargetedSynthesisUnlocked());
            assertTrue(game.setSynthesisTarget(ElementCategory.HALOGEN));
            assertEquals(ElementCategory.HALOGEN, game.synthesisTarget());
        }

        @Test
        void sansCibleLaSyntheseTireAuHasard() {
            Game game = tableGame();
            assertEquals(null, game.synthesisTarget());
            assertEquals(0, game.synthesisTriesLeft());
            assertEquals(1, game.synthesize().size());
            assertEquals(0, game.synthesisTries());
        }

        @Test
        void onNeViseRienAvantLOuvertureDuTableau() {
            Game game = newGame();
            assertFalse(game.setSynthesisTarget(ElementCategory.HALOGEN));
            assertEquals(null, game.synthesisTarget());
        }

        @Test
        void lesPremieresSynthesesSontPayeesMaisNeDonnentRien() {
            Game game = tableGame();
            assertTrue(game.setSynthesisTarget(ElementCategory.TRANSITION_METAL));   // trois synthèses
            assertEquals(3, game.synthesisTriesLeft());

            assertTrue(game.synthesize().isEmpty());
            assertValue(MAX - 2, game.state().atoms());        // payée au prix normal
            assertEquals(1, game.state().synthesisCount());    // et le prix monte comme pour les autres
            assertEquals(2, game.synthesisTriesLeft());
            assertTrue(game.synthesize().isEmpty());
            assertValue(MAX - 2 - 4, game.state().atoms());
            assertEquals(0, game.discoveredElements());

            List<Element> obtained = game.synthesize();
            assertEquals(1, obtained.size());
            assertEquals(ElementCategory.TRANSITION_METAL, obtained.get(0).category());
            assertValue(MAX - 2 - 4 - 8, game.state().atoms());
            assertEquals(0, game.synthesisTries());
            assertEquals(3, game.synthesisTriesLeft());        // la cible reste : la suivante repart de zéro

            assertEquals(3, game.stats().syntheses());
            assertEquals(2, game.stats().targetTries());
            assertEquals(1, game.stats().targetedSyntheses());
        }

        @Test
        void elleDonneDAbordUnElementQueLeJoueurNAPas() {
            for (long seed = 1; seed <= 20; seed++) {
                Game game = new Game(new GameState(), List.of(SPEED, GENERATOR, KEEP), List.of(AUTO_FUSION), new Random(seed));
                game.start();
                createEnoughAtoms(game);
                game.state().setDarkLevel("dark_shop_target", 1);
                for (Element element : PeriodicTable.elements(ElementCategory.HALOGEN)) {
                    game.state().setElementCount(element.number(), 1);
                }
                game.state().setElementCount(85, 0);           // il ne manque que l'astate
                game.setSynthesisTarget(ElementCategory.HALOGEN);
                List<Element> obtained = List.of();
                for (int i = 0; i < ElementCategory.HALOGEN.targetTries(); i++) {
                    game.state().setAtoms(BigNum.of(MAX));
                    obtained = game.synthesize();
                }
                assertEquals(85, obtained.get(0).number());
            }
        }

        @Test
        void sinonUnElementQuiPeutEncoreGagnerUnExemplaire() {
            Game game = tableGame();
            for (Element element : PeriodicTable.elements(ElementCategory.HALOGEN)) {
                game.state().setElementCount(element.number(), 9);
            }
            game.state().setElementCount(17, 3);               // seul le chlore n'est pas à son maximum
            game.setSynthesisTarget(ElementCategory.HALOGEN);
            List<Element> obtained = List.of();
            for (int i = 0; i < ElementCategory.HALOGEN.targetTries(); i++) {
                game.state().setAtoms(BigNum.of(MAX));
                obtained = game.synthesize();
            }
            assertEquals(17, obtained.get(0).number());
            assertEquals(4, game.elementCount(17));
        }

        @Test
        void uneFamilleEpuiseeNePeutPlusEtreVisee() {
            Game game = tableGame();
            assertTrue(game.setSynthesisTarget(ElementCategory.HALOGEN));
            for (Element element : PeriodicTable.elements(ElementCategory.HALOGEN)) {
                game.state().setElementCount(element.number(), 9);
            }
            assertEquals(null, game.synthesisTarget());        // la cible s'efface d'elle-même
            assertFalse(game.setSynthesisTarget(ElementCategory.HALOGEN));
            assertFalse(game.synthesize().isEmpty());          // la synthèse redevient ordinaire : elle donne tout de suite
        }

        @Test
        void changerDeCibleGardeLesSynthesesDejaPayees() {
            Game game = tableGame();
            game.setSynthesisTarget(ElementCategory.ACTINIDE); // dix synthèses
            game.synthesize();
            game.synthesize();
            assertEquals(8, game.synthesisTriesLeft());
            game.setSynthesisTarget(ElementCategory.TRANSITION_METAL);   // trois : il n'en reste qu'une
            assertEquals(1, game.synthesisTriesLeft());
            game.state().setAtoms(BigNum.of(MAX));
            List<Element> obtained = game.synthesize();
            assertEquals(ElementCategory.TRANSITION_METAL, obtained.get(0).category());
        }

        @Test
        void retirerLaCibleRendLaSyntheseOrdinaire() {
            Game game = tableGame();
            game.setSynthesisTarget(ElementCategory.ACTINIDE);
            game.synthesize();
            assertTrue(game.setSynthesisTarget(null));
            assertEquals(0, game.synthesisTriesLeft());
            assertEquals(1, game.synthesize().size());
            assertEquals(1, game.synthesisTries());            // l'essai payé attend une prochaine cible
        }

        @Test
        void laGarantieDUnElementUniquePasseAvantLaCible() {
            Game game = tableGame();
            game.setSynthesisTarget(ElementCategory.ACTINIDE); // dix synthèses, la garantie tombe à la huitième
            for (int i = 1; i < Game.GUARANTEED_UNIQUE_SYNTHESIS; i++) {
                game.state().setAtoms(BigNum.of(MAX));
                assertTrue(game.synthesize().isEmpty());
            }
            game.state().setAtoms(BigNum.of(MAX));
            List<Element> guaranteed = game.synthesize();
            assertEquals(1, guaranteed.size());
            assertTrue(guaranteed.get(0).category().unique());
            assertEquals(Game.GUARANTEED_UNIQUE_SYNTHESIS - 1, game.synthesisTries());   // la cible n'a pas avancé
        }

        @Test
        void laSyntheseAutomatiqueSuitLaCible() {
            Game game = tableGame();
            game.state().setElementCount(92, 1);               // un élément unique : la synthèse automatique existe
            game.buyAutomation("autoSynth");                   // une action toutes les 2 secondes
            game.state().setAtoms(BigNum.of(MAX));
            game.setSynthesisTarget(ElementCategory.TRANSITION_METAL);
            game.tick(4.1);                                    // deux synthèses payées, rien d'obtenu
            assertEquals(2, game.state().synthesisCount());
            assertEquals(1, game.discoveredElements());
            game.tick(2);                                      // la troisième aboutit
            assertEquals(3, game.state().synthesisCount());
            assertEquals(2, game.discoveredElements());
        }

        @Test
        void lExplosionEffaceLaCible() {
            Game game = tableGame();
            game.setSynthesisTarget(ElementCategory.ACTINIDE);
            game.synthesize();
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertEquals(null, game.state().synthesisTarget());
            assertEquals(0, game.synthesisTries());
        }

        @Test
        void plusLaFamilleEstRarePlusLaCibleCoute() {
            assertEquals(3, ElementCategory.TRANSITION_METAL.targetTries());
            assertEquals(5, ElementCategory.HALOGEN.targetTries());
            assertEquals(8, ElementCategory.NOBLE_GAS.targetTries());
            assertEquals(10, ElementCategory.ACTINIDE.targetTries());
            ElementCategory previous = null;
            for (ElementCategory category : ElementCategory.values()) {
                assertTrue(category.targetTries() >= 3);       // toujours plus cher que le double
                if (previous != null) assertTrue(category.targetTries() >= previous.targetTries());
                previous = category;
            }
        }
    }

    @Nested
    class PaliersDeTaille {

        private Game darkGame() {
            Game game = newGame();
            game.state().setDarkMatter(BigNum.of(1));
            game.state().setExplosions(1);
            return game;
        }

        @Test
        void leProtonNEstPasUnPalier() {
            assertEquals(Game.DARK_MATTER_START_SIZE, SizeScale.LANDMARKS.get(0).size());
            assertEquals(SizeScale.LANDMARKS.size() - 1, SizeScale.MILESTONES.size());
            assertEquals(26, SizeScale.MILESTONES.size());
            Game game = darkGame();
            assertEquals(0, game.landmarksReached());          // à sa taille de départ
        }

        @Test
        void aucunPalierAvantLaPremiereExplosion() {
            Game game = newGame();
            game.state().setDarkMatterSize(BigNum.of(1, 30));
            assertEquals(0, game.landmarksReached());
            assertValue(1, game.particlesPerCreation());
        }

        @Test
        void lesTroisBonusAlternent() {
            assertEquals(SizeScale.Bonus.PARTICLES, SizeScale.bonusOf(0));
            assertEquals(SizeScale.Bonus.ATOMS, SizeScale.bonusOf(1));
            assertEquals(SizeScale.Bonus.EXPANSION, SizeScale.bonusOf(2));
            assertEquals(SizeScale.Bonus.PARTICLES, SizeScale.bonusOf(3));
            assertThrows(IllegalArgumentException.class, () -> SizeScale.bonusOf(26));
        }

        @Test
        void chaquePalierAtteintDonneSonBonus() {
            Game game = darkGame();
            game.state().setDarkMatterSize(BigNum.of(2, -14));   // au-delà du noyau d'uranium : un palier
            assertEquals(1, game.landmarksReached());
            assertValue(2, game.particlesPerCreation());
            assertValue(1, game.atomsPerFusion());

            game.state().setDarkMatterSize(BigNum.of(2, -10));   // l'atome d'hydrogène : deux paliers
            assertEquals(2, game.landmarksReached());
            assertValue(1.05, game.atomsPerFusion());
            assertEquals(1, game.landmarkExpansionMultiplier(), 0);

            game.state().setDarkMatterSize(BigNum.of(1, -8));    // l'hélice d'ADN : trois paliers
            assertEquals(Game.LANDMARK_EXPANSION, game.landmarkExpansionMultiplier(), 1e-12);
        }

        @Test
        void auBoutDeLEchelleLesBonusSeCumulent() {
            Game game = darkGame();
            game.state().setDarkMatterSize(BigNum.of(1, 30));
            assertEquals(26, game.landmarksReached());
            assertEquals(9, game.landmarksReached(SizeScale.Bonus.PARTICLES));
            assertEquals(9, game.landmarksReached(SizeScale.Bonus.ATOMS));
            assertEquals(8, game.landmarksReached(SizeScale.Bonus.EXPANSION));
            assertValue(512, game.landmarkParticlesMultiplier());
            assertEquals(1.45, game.landmarkAtomsMultiplier(), 1e-12);
            assertEquals(Math.pow(Game.LANDMARK_EXPANSION, 8), game.landmarkExpansionMultiplier(), 1e-9);
        }

        @Test
        void lesPaliersDeCroissanceAccelerentLaMatiereNoire() {
            Game small = darkGame();
            Game large = darkGame();
            large.state().setDarkMatterSize(BigNum.of(1, -8));   // un palier de croissance atteint
            double ratio = large.darkMatterMomentum() / small.darkMatterMomentum();
            // L'élan suit aussi la production, que le premier palier a doublée.
            assertEquals(Game.LANDMARK_EXPANSION * large.darkMatterProgressFactor() / small.darkMatterProgressFactor(), ratio, 1e-9);
        }
    }

    @Nested
    class VerrouDeLAppui {

        private Game darkGame() {
            Game game = newGame();
            game.state().setDarkMatter(BigNum.of(5));
            game.state().setExplosions(1);
            return game;
        }

        @Test
        void sansLaCaseDeLArbreRienNeSeVerrouille() {
            Game game = darkGame();
            assertFalse(game.setHoldLocked(true));
            assertFalse(game.isHoldLocked());
            game.tick(60);
            assertEquals(Game.DARK_MATTER_START_SIZE, game.state().darkMatterSize());
        }

        @Test
        void verrouilleeElleGrossitAChaqueTick() {
            Game locked = darkGame();
            locked.state().setDarkLevel("dark_lock", 1);
            assertTrue(locked.setHoldLocked(true));
            assertTrue(locked.isHoldLocked());
            locked.tick(30);

            Game held = darkGame();
            held.growDarkMatter(30 * Game.HOLD_LOCK_SHARE);    // un verrou vaut 60 % d'un appui tenu à la main
            assertEquals(held.state().darkMatterSize().log10(), locked.state().darkMatterSize().log10(), 1e-9);
            assertEquals(30 * Game.HOLD_LOCK_SHARE, locked.stats().holdTime(), 1e-9);   // compté comme un appui
        }

        @Test
        void leVerrouSeLibereALaDemande() {
            Game game = darkGame();
            game.state().setDarkLevel("dark_lock", 1);
            game.setHoldLocked(true);
            assertTrue(game.setHoldLocked(false));
            game.tick(30);
            assertEquals(Game.DARK_MATTER_START_SIZE, game.state().darkMatterSize());
        }

        @Test
        void lExplosionLibereLeVerrou() {
            Game game = darkGame();
            game.state().setDarkLevel("dark_lock", 1);
            game.setHoldLocked(true);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertFalse(game.isHoldLocked());
        }
    }

    @Nested
    class NiveauxSansPlafond {

        @Test
        void noyauxEtRouagesSombresNOntPlusDeMaximum() {
            Game game = newGame();
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(1e6));
            for (String id : List.of("dark_shop_atoms", "dark_shop_cadence")) {
                for (int level = 0; level < 12; level++) {
                    assertValue(Math.pow(2, level), game.darkCostOf(id));   // 1, 2, 4, 8…
                    assertTrue(game.buyDark(id));
                }
                assertEquals(12, game.darkLevelOf(id));
                assertFalse(game.isDarkMaxed(id));
            }
            // Douze niveaux à +20 % par matière noire gagnée : l'effet s'additionne, il ne s'emballe pas.
            assertEquals(1 + 0.2 * 12 * 1e6, game.darkAtomsMultiplier(), 1);
        }

        @Test
        void lesRouagesNeFontJamaisPasserSousLeDelaiMinimal() {
            Game game = newGame();
            unlock(game);
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(1e6));
            for (int level = 0; level < 12; level++) game.buyDark("dark_shop_cadence");
            assertEquals(game.minAutomationInterval(), game.automationInterval("autoSlow"), 0);
        }
    }

    @Nested
    class Defis {

        /** Partie après quelques explosions : {@code darkMatter} matières noires gagnées, tableau débloqué. */
        private Game darkGame(double darkMatter) {
            Game game = newGame();
            unlock(game);
            game.state().setDarkMatter(BigNum.of(darkMatter));
            game.state().setExplosions(3);
            return game;
        }

        private void fillTable(Game game) {
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
        }

        @Test
        void ilYASixDefisChacunAvecSaContrainteEtSaRecompense() {
            Game game = darkGame(100);
            assertEquals(6, game.challenges().size());
            assertEquals(6, game.challenges().stream().map(Challenge::id).distinct().count());
            assertEquals(6, game.challenges().stream().map(Challenge::rule).distinct().count());
            assertEquals(6, game.challenges().stream().map(Challenge::reward).distinct().count());
            int previous = 0;
            for (Challenge challenge : game.challenges()) {    // du plus accessible au plus exigeant
                assertTrue(challenge.darkMatter() >= previous);
                previous = challenge.darkMatter();
            }
            assertEquals(null, game.activeChallenge());
            assertEquals(0, game.completedChallenges());
        }

        @Test
        void unDefiDemandeAssezDeMatiereNoireGagnee() {
            Game game = darkGame(2);
            assertFalse(game.isChallengeUnlocked("challenge_rust"));     // il en faut 3
            assertFalse(game.startChallenge("challenge_rust"));
            game.state().setDarkMatter(BigNum.of(2));
            game.state().setDarkMatterSpent(BigNum.of(1));     // dépensée, mais gagnée
            assertTrue(game.isChallengeUnlocked("challenge_rust"));
            assertFalse(game.isChallengeUnlocked("challenge_decay"));
        }

        @Test
        void pasDeDefiAvantLaPremiereExplosion() {
            Game game = newGame();
            game.state().setDarkMatter(BigNum.of(50));         // sans explosion, la matière noire n'existe pas encore
            assertFalse(game.isChallengeUnlocked("challenge_rust"));
        }

        @Test
        void commencerUnDefiEffaceLaPartieSansRienRapporter() {
            Game game = darkGame(30);
            game.state().setParticles(BigNum.of(500));
            game.state().setElementCount(26, 3);
            assertTrue(game.startChallenge("challenge_rust"));
            assertEquals("challenge_rust", game.activeChallenge().id());
            assertEquals(BigNum.ZERO, game.state().particles());
            assertEquals(BigNum.ZERO, game.state().atoms());
            assertEquals(0, game.discoveredElements());
            assertEquals(0, game.levelOf("keep"));
            assertValue(30, game.state().darkMatter());        // ni gagnée ni perdue
            assertEquals(3, game.state().explosions());
            assertEquals(0, game.stats().runTime(), 0);
        }

        @Test
        void unDefiSeJoueSansLesMemoiresDeLaMatiereNoire() {
            Game game = darkGame(30);
            game.state().setDarkLevel("dark_seeds", 2);        // deux générateurs offerts
            game.state().setDarkLevel("dark_priming", 1);      // dix niveaux de vitesse offerts
            game.state().setDarkLevel("dark_machines", 1);     // les automatismes survivent à l'explosion
            game.state().setDarkLevel("dark_relics", 1);       // les éléments uniques aussi
            game.state().setDarkLevel("dark_shop_start", 1);   // et le départ est lancé
            game.state().setDarkLevel("dark_density", 3);      // particules ×8 : un multiplicateur, lui, reste
            game.state().setElementCount(2, 1);
            game.setDarkAutomationEnabled("dark_auto_start", true);
            assertEquals(3, game.generatorCount());
            assertTrue(game.ownsAutomation("autoSlow"));

            assertTrue(game.startChallenge("challenge_rust"));
            assertEquals(1, game.generatorCount());
            assertEquals(0, game.startingSpeedLevels());
            assertFalse(game.ownsAutomation("autoSlow"));
            assertEquals(0, game.discoveredElements());
            assertEquals(BigNum.ZERO, game.state().totalAtoms());
            assertValue(8, game.particlesPerCreation());
            game.tick(30);
            assertFalse(game.ownsAutomation("autoSpeed"));     // rien n'est offert pendant un défi

            game.abandonChallenge();                           // la partie ordinaire retrouve tout
            assertEquals(3, game.generatorCount());
            assertEquals(10, game.startingSpeedLevels());
            assertTrue(game.state().totalAtoms().sign() > 0);
            game.tick(2);
            assertTrue(game.ownsAutomation("autoSpeed"));
        }

        @Test
        void unSeulDefiALaFois() {
            Game game = darkGame(30);
            assertTrue(game.startChallenge("challenge_rust"));
            assertFalse(game.startChallenge("challenge_heavy"));
            assertEquals("challenge_rust", game.activeChallenge().id());
        }

        @Test
        void abandonnerEffaceLaPartieEtLeveLaContrainte() {
            Game game = darkGame(30);
            assertFalse(game.abandonChallenge());              // rien à abandonner
            game.startChallenge("challenge_heavy");
            game.state().setParticles(BigNum.of(500));
            assertTrue(game.abandonChallenge());
            assertEquals(null, game.activeChallenge());
            assertEquals(BigNum.ZERO, game.state().particles());
            assertFalse(game.isChallengeCompleted("challenge_heavy"));
            assertEquals(3, game.generatorsPerAtom());
        }

        @Test
        void lExplosionReussitLeDefiEtNoteSonTemps() {
            Game game = darkGame(30);
            game.startChallenge("challenge_heavy");
            game.tick(120);
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(null, game.activeChallenge());
            assertTrue(game.isChallengeCompleted("challenge_heavy"));
            assertEquals(1, game.completedChallenges());
            assertEquals(120, game.challengeBestTime("challenge_heavy"), 1e-6);
            assertValue(31, game.state().darkMatter());        // l'explosion rapporte comme d'habitude
            assertEquals(-1, game.challengeBestTime("challenge_rust"), 0);
        }

        @Test
        void unDefiSeJoueAvecUnTableauDeMasseUn() {
            Game game = darkGame(30);
            game.state().setTableWeightLevel(4);
            assertTrue(game.atomCap().gt(BigNum.of(118)));
            game.startChallenge("challenge_heavy");
            assertValue(1, game.tableWeight());
            assertValue(118, game.atomCap());
            game.abandonChallenge();
            assertTrue(game.atomCap().gt(BigNum.of(118)));   // la partie ordinaire retrouve son poids
        }

        @Test
        void laPremiereReussiteAlourditLeTableauPasLesSuivantes() {
            Game game = darkGame(30);
            game.startChallenge("challenge_heavy");
            assertFalse(game.isChallengeReplay());
            assertValue(1, game.nextExplosionDarkMatter());
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(1, game.state().tableWeightLevel());
            assertValue(31, game.state().darkMatter());
            int explosions = game.state().explosions();

            game.startChallenge("challenge_heavy");           // rejoué : il ne compte que pour son temps
            assertTrue(game.isChallengeReplay());
            assertValue(0, game.nextExplosionDarkMatter());
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(1, game.state().tableWeightLevel());
            assertValue(31, game.state().darkMatter());
            assertEquals(explosions + 1, game.state().explosions());
            assertFalse(game.isChallengeReplay());            // le défi est terminé
        }

        @Test
        void rejouerUnDefiNeGardeQueLeMeilleurTemps() {
            Game game = darkGame(30);
            game.startChallenge("challenge_heavy");
            game.tick(120);
            fillTable(game);
            game.explode();
            game.startChallenge("challenge_heavy");
            game.tick(300);
            fillTable(game);
            game.explode();
            assertEquals(120, game.challengeBestTime("challenge_heavy"), 1e-6);
            game.startChallenge("challenge_heavy");
            game.tick(60);
            fillTable(game);
            game.explode();
            assertEquals(60, game.challengeBestTime("challenge_heavy"), 1e-6);
            assertEquals(1, game.completedChallenges());
        }

        @Test
        void uneExplosionOrdinaireNeReussitRien() {
            Game game = darkGame(30);
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(0, game.completedChallenges());
        }

        @Test
        void laRemiseAZeroEffaceLesDefis() {
            Game game = darkGame(30);
            game.startChallenge("challenge_heavy");
            fillTable(game);
            game.explode();
            game.startChallenge("challenge_rust");
            game.reset();
            assertEquals(null, game.state().activeChallenge());
            assertEquals(0, game.state().completedChallenges().size());
        }

        // ----- Les contraintes -----

        @Test
        void rouilleRalentitTousLesAutomatismes() {
            Game game = darkGame(30);
            game.startChallenge("challenge_rust");
            game.state().setAtoms(BigNum.of(100));
            createEnoughAtoms(game);
            game.buyAutomation("autoSlow");                    // 4 secondes d'ordinaire
            assertEquals(4 * Game.RUSTY_FACTOR, game.automationInterval("autoSlow"), 1e-12);
            game.buyAutomation("autoSpeed");                   // au minimum d'ordinaire : une demi-seconde ici
            assertEquals(Game.RUSTY_MIN_INTERVAL, game.automationInterval("autoSpeed"), 1e-12);
            assertEquals(Game.RUSTY_FACTOR, game.darkAutomationInterval("dark_auto_start"), 1e-12);
            game.abandonChallenge();
            assertEquals(1, game.darkAutomationInterval("dark_auto_start"), 1e-12);
        }

        @Test
        void fusionLourdeDemandeDeuxFoisPlusDeGenerateurs() {
            Game game = darkGame(30);
            assertEquals(3, game.generatorsPerAtom());
            game.startChallenge("challenge_heavy");
            assertEquals(6, game.generatorsPerAtom());
            assertEquals(6, game.maxGeneratorCount());         // la limite suit
            game.state().setParticles(BigNum.of(1e30));
            assertEquals(2, game.buy("gen", 2));
            assertFalse(game.canFuse());                       // trois générateurs ne suffisent plus
            assertEquals(3, game.buy("gen", 10));
            assertEquals(6, game.generatorCount());
            assertTrue(game.canFuse());
            assertEquals(1, game.fusionGroups());
            assertTrue(game.fuse());
            assertValue(1, game.state().atoms());
        }

        @Test
        void particulesVolatilesFaitFondreLesParticules() {
            Game game = darkGame(30);
            game.startChallenge("challenge_volatile");
            game.state().setParticles(BigNum.of(1000));
            double before = total(game);
            game.tick(1);
            // La moitié de ce que le joueur possédait a disparu ; le peu créé entre-temps ne change presque rien.
            assertEquals(500, game.state().particles().toDouble(), 1);
            assertTrue(total(game) < before);
            game.tick(3);
            assertEquals(1000 * Math.pow(0.5, 4), game.state().particles().toDouble(), 2);
        }

        @Test
        void particulesVolatilesLaisseUnEquilibreEntreCreationEtPerte() {
            Game game = darkGame(30);
            game.startChallenge("challenge_volatile");
            game.state().setLevel("huge", 1);                  // une production énorme et régulière
            double production = game.productionPerSecond().toDouble();
            for (int i = 0; i < 2000; i++) game.tick(0.1);
            // À l'équilibre, ce qui s'évapore chaque seconde vaut ce qui se crée : le stock vaut la production / taux.
            double rate = -Math.log(1 - Game.VOLATILE_LOSS_PER_SECOND);
            assertEquals(production / rate, game.state().particles().toDouble(), production / rate * 1e-6);
        }

        @Test
        void amnesieRemetAZeroLesAmeliorationsEnAtomesAChaqueFusion() {
            Game game = darkGame(30);
            game.startChallenge("challenge_amnesia");
            game.state().setAtoms(BigNum.of(100));
            createEnoughAtoms(game);
            game.buy("double");
            game.buy("keep");
            game.buyAutomation("autoSlow");
            game.state().setParticles(BigNum.of(1e30));
            game.buy("speed", 3);
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertEquals(0, game.levelOf("double"));
            assertEquals(0, game.levelOf("keep"));
            assertEquals(3, game.levelOf("speed"));            // Persistance a protégé la vitesse une dernière fois
            assertTrue(game.ownsAutomation("autoSlow"));       // les automatismes ne sont pas des améliorations
        }

        @Test
        void elementsInertesCoupeTousLesBonusDuTableau() {
            Game game = darkGame(30);
            game.startChallenge("challenge_inert");
            game.state().setElementCount(2, 1);                // l'hélium doublerait les particules
            game.state().setElementCount(1, 1);                // et la première période les triplerait
            assertEquals(1, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 0);
            assertValue(1, game.particlesPerCreation());
            assertEquals(2, game.discoveredElements());        // les éléments sont bien là, ils ne font rien
            assertTrue(game.isSynthesisAutomationUnlocked());  // et un élément unique reste un élément unique
            game.abandonChallenge();
            game.state().setElementCount(2, 1);
            assertEquals(2, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
        }

        @Test
        void desintegrationFaitDisparaitreLesExemplaires() {
            Game game = darkGame(30);
            game.startChallenge("challenge_decay");
            game.state().setElementCount(26, 9);
            game.state().setElementCount(29, 9);
            game.state().setElementCount(47, 7);               // 25 exemplaires : un demi-exemplaire perdu par seconde
            game.tick(1.9);
            assertEquals(25, game.ownedCopies());              // pas encore un exemplaire entier
            game.tick(0.2);
            assertEquals(24, game.ownedCopies());
            game.tick(100);                                    // un seul très long tick
            assertTrue(game.ownedCopies() <= 4);               // 25 × e^(−0,02 × 100) ≈ 3,4… à un ou deux près
        }

        @Test
        void sansDefiRienNeSeDesintegre() {
            Game game = darkGame(30);
            game.state().setElementCount(26, 9);
            game.tick(10_000);
            assertEquals(9, game.elementCount(26));
        }

        // ----- Les récompenses -----

        private Game rewarded(String challengeId) {
            Game game = darkGame(100);
            game.state().addCompletedChallenge(challengeId);
            return game;
        }

        @Test
        void rouilleLaisseDesAutomatismesPlusVifs() {
            Game game = rewarded("challenge_rust");
            game.state().setAtoms(BigNum.of(100));
            game.buyAutomation("autoSlow");
            game.state().setAutomationSpeedLevel("autoSlow", 0);
            assertEquals(4 / Game.REWARD_AUTOMATION_DIVISOR, game.automationInterval("autoSlow"), 1e-12);
        }

        @Test
        void fusionLourdeDoubleLaPrimeDeGroupe() {
            Game game = rewarded("challenge_heavy");
            assertEquals(Game.REWARD_GROUP_BONUS, game.fusionGroupBonusPerGroup(), 0);
            game.state().setDarkLevel("dark_swarm", 1);
            game.state().setLevel("gen", 5);                   // six générateurs : deux groupes
            assertEquals(1.2, game.fusionGroupBonus(), 1e-12);
        }

        @Test
        void particulesVolatilesRenforceLesPaliersDeVitesse() {
            Game plain = new Game();
            plain.start();
            plain.state().setLevel("speed", 50);
            assertValue(4, plain.speedMilestoneMultiplier());   // deux paliers à ×2
            assertEquals(2, plain.speedMilestoneFactor("speed"), 0);

            Game game = new Game();
            game.start();
            game.state().addCompletedChallenge("challenge_volatile");
            game.state().setLevel("speed", 50);
            assertEquals(3, game.speedMilestoneFactor("speed"), 0);
            assertValue(9, game.speedMilestoneMultiplier());    // deux paliers à ×3
        }

        @Test
        void amnesieDiviseParDeuxLePrixDesAmeliorationsEnAtomes() {
            Game game = rewarded("challenge_amnesia");
            assertValue(1, game.costOf("mass"));               // 2 atomes d'ordinaire
            game.state().setLevel("double", 3);
            assertValue(14, game.costOf("double"));            // 27 d'ordinaire, arrondi au-dessus
            assertValue(10, game.costOf("speed"));             // les particules ne sont pas concernées
        }

        @Test
        void elementsInertesRenforceTousLesElements() {
            Game game = rewarded("challenge_inert");
            game.state().setElementCount(2, 1);                // l'hélium : ×2 d'ordinaire
            assertEquals(Math.pow(2, Game.REWARD_ELEMENT_STRENGTH), game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
        }

        @Test
        void desintegrationDonneUnElementDePlusParSynthese() {
            Game game = rewarded("challenge_decay");
            assertEquals(2, game.elementsPerSynthesis());
            game.state().setAtoms(BigNum.of(100));
            assertTrue(game.synthesize().size() >= 2);
        }

        @Test
        void unDefiMalDefiniEstRefuse() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Challenge("", "X", Challenge.Rule.DECAY, 1, Challenge.Reward.EXTRA_ELEMENT));
            assertThrows(IllegalArgumentException.class,
                    () -> new Challenge("x", "X", Challenge.Rule.DECAY, 0, Challenge.Reward.EXTRA_ELEMENT));
            assertThrows(IllegalArgumentException.class, () -> newGame().isChallengeUnlocked("inconnu"));
        }
    }

    @Nested
    class Succes {

        /** Partie de test (vitesse à 10, générateur à 100 puis 1000) avec le vrai catalogue de succès. */
        private Game gameWithAchievements() {
            Game game = new Game(new GameState(),
                    List.of(SPEED, GENERATOR, HUGE, DOUBLE, MASS, PATIENCE, BOOST, DISCOUNT, KEEP),
                    List.of(AUTO_SPEED, AUTO_GEN, AUTO_FUSION, AUTO_SLOW, AUTO_SYNTH),
                    DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(42));
            game.start();
            return game;
        }

        @Test
        void leCatalogueCompteCinquanteSuccesDontAuMoinsVingtAvecUnBonusPropre() {
            assertEquals(50, Achievements.DEFAULT.size());
            assertEquals(50, Achievements.DEFAULT.stream().map(Achievement::id).distinct().count());
            assertEquals(50, Achievements.DEFAULT.stream().map(Achievement::name).distinct().count());
            long withBonus = Achievements.DEFAULT.stream().filter(Achievement::hasBonus).count();
            assertTrue(withBonus >= 20, "succès d'action : " + withBonus);
            for (Achievement achievement : Achievements.DEFAULT) {
                assertFalse(achievement.description().isBlank());
                assertTrue(achievement.summary().length() <= 30, achievement.summary());   // un résumé tient sur une tuile
                if (achievement.hasBonus()) assertTrue(achievement.amount() > 0 && achievement.amount() <= 0.05);   // de petits bonus
            }
        }

        @Test
        void unePartieSansCatalogueNAAucunSucces() {
            Game game = gameWithParticles(1e9);
            game.buy("gen", 2);
            game.tick(5);
            assertTrue(game.achievements().isEmpty());
            assertEquals(0, game.achievementCount());
            assertEquals(BigNum.ONE, game.achievementParticlesMultiplier());
            assertValue(1, game.particlesPerCreation());
        }

        @Test
        void lesSuccesDEtapeTombentTousSeuls() {
            Game game = gameWithAchievements();
            assertEquals(0, game.achievementCount());
            game.tick(ONE_PARTICLE);                           // une particule, et plus d'une seconde de jeu
            assertTrue(game.hasAchievement("step_first_particle"));
            assertFalse(game.hasAchievement("step_all_generators"));
            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            game.tick(1);
            assertTrue(game.hasAchievement("step_all_generators"));
            assertTrue(game.fuse());
            game.tick(1);
            assertTrue(game.hasAchievement("step_first_atom"));
        }

        @Test
        void chaqueSuccesAjouteUnPourCentDeParticules() {
            Game game = gameWithAchievements();
            game.state().addAchievement("step_first_particle");
            game.state().addAchievement("step_first_atom");
            assertEquals(2, game.achievementCount());
            assertValue(1 + 2 * Game.ACHIEVEMENT_PARTICLES, game.achievementParticlesMultiplier());
            assertValue(1.02, game.particlesPerCreation());
        }

        @Test
        void unSuccesInconnuDuCatalogueNeComptePas() {
            Game game = gameWithAchievements();
            game.state().addAchievement("vieux_succes_supprime");
            assertEquals(0, game.achievementCount());
            assertValue(1, game.particlesPerCreation());
        }

        @Test
        void lesBonusPropresSAdditionnentParSorte() {
            Game game = gameWithAchievements();
            game.state().addAchievement(Achievements.EXACT_CHANGE);      // atomes +0,5 %
            game.state().addAchievement("feat_full_atoms");              // atomes +0,5 %
            game.state().addAchievement(Achievements.BARE_FUSION);       // particules +2 %
            assertEquals(0.01, game.achievementBonus(Achievement.Bonus.ATOMS), 1e-12);
            assertEquals(0.02, game.achievementBonus(Achievement.Bonus.PARTICLES), 1e-12);
            assertEquals(0, game.achievementBonus(Achievement.Bonus.EXPANSION), 0);
            assertValue(1.01, game.atomsPerFusion());
            assertValue((1 + 3 * Game.ACHIEVEMENT_PARTICLES) * 1.02, game.particlesPerCreation());
        }

        @Test
        void lesBonusAgissentLaOuIlsLeDisent() {
            Game game = gameWithAchievements();
            unlock(game);
            double interval = game.automationInterval("autoSlow");
            BigNum generator = game.costOf("gen");
            game.state().addAchievement(Achievements.FAST_FUSION);       // cadence +1 %
            game.state().addAchievement(Achievements.BULK_PURCHASE);     // générateurs −2 %
            game.state().addAchievement(Achievements.BUY_EVERYTHING);    // générateurs −2 %
            assertEquals(interval / 1.01, game.automationInterval("autoSlow"), 1e-12);
            assertValue(generator.toDouble() * 0.96, game.costOf("gen"));
        }

        @Test
        void lesNouveauxSuccesSontAnnoncesUneSeuleFois() {
            Game game = gameWithAchievements();
            game.tick(ONE_PARTICLE);
            List<Achievement> fresh = game.takeNewAchievements();
            assertEquals(1, fresh.size());
            assertEquals("step_first_particle", fresh.get(0).id());
            assertTrue(game.takeNewAchievements().isEmpty());
            game.tick(5);
            assertTrue(game.takeNewAchievements().isEmpty()); // déjà obtenu : pas de seconde annonce
        }

        @Test
        void lExplosionGardeLesSuccesMaisPasLaRemiseAZero() {
            Game game = gameWithAchievements();
            unlock(game);
            game.tick(ONE_PARTICLE);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertTrue(game.hasAchievement("step_first_particle"));
            game.reset();
            assertEquals(0, game.state().achievements().size());
        }

        // ----- Les actions -----

        @Test
        void fusionnerSansVitesseNiCouplage() {
            Game game = gameWithAchievements();
            game.state().setParticles(BigNum.of(1e9));
            game.buy("speed");
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertFalse(game.hasAchievement(Achievements.BARE_FUSION));   // un niveau de vitesse : raté
            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertTrue(game.hasAchievement(Achievements.BARE_FUSION));
        }

        @Test
        void fusionnerApresUneLongueAttenteOuTresVite() {
            Game game = gameWithAchievements();
            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            game.tick(10);
            assertTrue(game.fuse());                           // la première fusion de la partie n'est pas un sprint
            assertFalse(game.hasAchievement(Achievements.FAST_FUSION));
            assertFalse(game.hasAchievement(Achievements.PATIENT_FUSION));

            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            game.tick(5);
            assertTrue(game.fuse());
            assertTrue(game.hasAchievement(Achievements.FAST_FUSION));

            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            game.tick(Achievements.PATIENT_SECONDS + 1);
            assertTrue(game.fuse());
            assertTrue(game.hasAchievement(Achievements.PATIENT_FUSION));
        }

        @Test
        void acheterEnGrosEtToutAcheter() {
            Game game = gameWithAchievements();
            game.state().setParticles(BigNum.of(1e30));
            assertEquals(24, game.buy("speed", 24));
            assertFalse(game.hasAchievement(Achievements.BULK_PURCHASE));
            assertEquals(25, game.buy("speed", 25));
            assertTrue(game.hasAchievement(Achievements.BULK_PURCHASE));
            assertFalse(game.hasAchievement(Achievements.BUY_EVERYTHING));
            assertTrue(game.buyAllWithParticles() >= Achievements.BUY_EVERYTHING_LEVELS);
            assertTrue(game.hasAchievement(Achievements.BUY_EVERYTHING));
        }

        @Test
        void payerExactementCeQuOnA() {
            Game game = gameWithAchievements();
            game.state().setLevel("double", 3);                // le niveau suivant coûte 27 atomes
            game.state().setAtoms(BigNum.of(28));
            game.buy("double");
            assertFalse(game.hasAchievement(Achievements.EXACT_CHANGE));   // il reste un atome
            game.state().setLevel("double", 3);
            game.state().setAtoms(BigNum.of(27));
            game.buy("double");
            assertTrue(game.hasAchievement(Achievements.EXACT_CHANGE));
        }

        @Test
        void unPetitAchatTombantJusteNeComptePas() {
            Game game = gameWithAchievements();
            game.state().setAtoms(BigNum.of(1));
            game.buy("double");                                // 1 atome : trop facile
            assertFalse(game.hasAchievement(Achievements.EXACT_CHANGE));
        }

        @Test
        void lesSuccesDeLaSynthese() {
            Game game = gameWithAchievements();
            unlock(game);
            game.state().setAtoms(Game.MAX_ATOMS);
            game.state().setDarkLevel("dark_shop_target", 1);
            game.setSynthesisTarget(ElementCategory.TRANSITION_METAL);
            for (int i = 0; i < 3; i++) {
                game.state().setAtoms(Game.MAX_ATOMS);
                game.synthesize();
            }
            assertTrue(game.hasAchievement(Achievements.TARGETED));
            assertFalse(game.hasAchievement(Achievements.TARGETED_ACTINIDE));
            game.state().setElementCount(92, 1);               // un élément unique : la garantie ne s'en mêle plus
            game.setSynthesisTarget(ElementCategory.ACTINIDE);
            for (int i = 0; i < 10; i++) {
                game.state().setAtoms(Game.MAX_ATOMS);
                game.synthesize();
            }
            assertTrue(game.hasAchievement(Achievements.TARGETED_ACTINIDE));

            game.setSynthesisTarget(null);
            game.state().setElementCount(35, 10_000);          // tirage double assuré
            game.state().setAtoms(Game.MAX_ATOMS);
            assertEquals(2, game.synthesize().size());
            assertTrue(game.hasAchievement(Achievements.DOUBLE_DRAW));
            assertFalse(game.hasAchievement(Achievements.TRIPLE_DRAW));
            game.state().setDarkLevel("dark_multisynthesis", 1);
            game.state().setAtoms(Game.MAX_ATOMS);
            assertEquals(3, game.synthesize().size());
            assertTrue(game.hasAchievement(Achievements.TRIPLE_DRAW));
        }

        @Test
        void lesSuccesDesElements() {
            Game game = gameWithAchievements();
            unlock(game);
            game.state().setElementCount(79, 1);               // l'or
            game.state().setElementCount(1, 1);
            game.state().setElementCount(8, 1);                // hydrogène et oxygène
            game.state().setElementCount(11, 1);               // du sodium, mais pas de chlore
            game.state().setElementCount(26, 8);
            game.tick(1);
            assertTrue(game.hasAchievement("feat_gold"));
            assertTrue(game.hasAchievement("feat_water"));
            assertFalse(game.hasAchievement("feat_salt"));
            assertFalse(game.hasAchievement("feat_nine_copies"));
            game.state().setElementCount(17, 1);
            game.state().setElementCount(26, 9);
            game.tick(1);
            assertTrue(game.hasAchievement("feat_salt"));
            assertTrue(game.hasAchievement("feat_nine_copies"));
        }

        @Test
        void lesSuccesDeLaMatiereNoire() {
            Game game = gameWithAchievements();
            unlock(game);
            game.state().setDarkMatter(BigNum.of(5));
            game.state().setExplosions(2);
            game.state().setDarkLevel("dark_lock", 1);
            game.setHoldLocked(true);
            assertTrue(game.hasAchievement(Achievements.HOLD_LOCK));

            game.startChallenge("challenge_rust");
            game.abandonChallenge();
            assertTrue(game.hasAchievement(Achievements.ABANDON));

            game.tick(30);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());                        // trente secondes après le début de la partie
            assertTrue(game.hasAchievement(Achievements.FAST_EXPLOSION));
        }

        @Test
        void laToutePremiereExplosionNEstPasUneExplosionRapide() {
            Game game = gameWithAchievements();
            unlock(game);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertFalse(game.hasAchievement(Achievements.FAST_EXPLOSION));
        }

        @Test
        void unSuccesMalDefiniEstRefuse() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Achievement("", "X", "x", "x", Achievement.Bonus.NONE, 0, null));
            assertThrows(IllegalArgumentException.class,
                    () -> new Achievement("x", "X", "", "x", Achievement.Bonus.NONE, 0, null));        // sans résumé
            assertThrows(IllegalArgumentException.class,
                    () -> new Achievement("x", "X", "x", "x", Achievement.Bonus.ATOMS, 0, null));    // un bonus sans taille
            assertThrows(IllegalArgumentException.class,
                    () -> new Achievement("x", "X", "x", "x", Achievement.Bonus.NONE, 0.1, null));   // une taille sans bonus
            Achievement twice = Achievement.step("x", "X", "x", "x", game -> true);
            assertThrows(IllegalArgumentException.class, () -> new Game(new GameState(), List.of(SPEED), List.of(),
                    DarkUpgrades.DEFAULT, List.of(twice, twice), new Random(1)));
        }

        @Test
        void leJeuCompletASesSucces() {
            Game game = new Game();
            assertEquals(50, game.achievements().size());
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
            assertFalse(game.state().isDarkAutomationEnabled("dark_auto_explosion"));
            assertFalse(game.state().holdLocked());
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
    class LesMolecules {

        private static final int H = 1;
        private static final int C = 6;
        private static final int O = 8;
        private static final String WATER = "H2O";
        private static final String SALT = "NaCl";
        private static final String GLUCOSE = "C6H12O6";

        /** Le vrai jeu, après un premier Big Bang, tableau plein et de l'espace à revendre. */
        private Game gameAfterBigBang() {
            Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                    Achievements.DEFAULT, new Random(42));
            game.start();
            game.state().setBigBangs(1);
            game.state().setSpace(BigNum.of(1, 9));
            refill(game);
            return game;
        }

        /** Remet chaque élément à son nombre maximal d'exemplaires. */
        private void refill(Game game) {
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), game.maxCopiesOf(element.category()));
            }
        }

        @Test
        void uneFormuleSeLit() {
            Molecule water = Molecule.of(Molecule.Kind.SIMPLE, "Eau", "H2O");
            assertEquals("H2O", water.id());
            assertEquals("H₂O", water.formula());
            assertEquals(Map.of(H, 2, O, 1), water.recipe());
            assertEquals(List.of(H, O), List.copyOf(water.recipe().keySet()));
            assertEquals(3, water.atoms());
            assertEquals(10, water.protons());
        }

        @Test
        void lesParenthesesMultiplientCeQuEllesContiennent() {
            assertEquals(Map.of(20, 3, 15, 2, O, 8), Molecule.of(Molecule.Kind.SALT, "x", "Ca3(PO4)2").recipe());
            assertEquals("Ca₃(PO₄)₂", Molecule.of(Molecule.Kind.SALT, "x", "Ca3(PO4)2").formula());
            assertEquals(Map.of(29, 3, C, 2, O, 8, H, 2), Molecule.of(Molecule.Kind.MINERAL, "x", "Cu3(CO3)2(OH)2").recipe());
            assertEquals(Map.of(26, 1, O, 2, H, 1), Molecule.of(Molecule.Kind.MINERAL, "x", "FeO(OH)").recipe());
            assertEquals(Map.of(26, 7, C, 18, 7, 18), Molecule.of(Molecule.Kind.MATERIAL, "x", "Fe7(CN)18").recipe());
            assertEquals(Map.of(82, 1, C, 8, H, 20), Molecule.of(Molecule.Kind.MATERIAL, "x", "Pb(C2H5)4").recipe());
        }

        @Test
        void unElementEcritPlusieursFoisEstAdditionne() {
            Molecule acid = Molecule.of(Molecule.Kind.ACID, "Acide acétique", "CH3COOH");
            assertEquals(Map.of(C, 2, H, 4, O, 2), acid.recipe());
            assertEquals(List.of(C, H, O), List.copyOf(acid.recipe().keySet()));
            assertEquals(8, acid.atoms());
        }

        @Test
        void uneFormuleIllisibleEstRefusee() {
            for (String formula : new String[] {"", "h2o", "H2O)", "(H2O", "Xx2", "H0", "2H", "H2 O"}) {
                assertThrows(IllegalArgumentException.class, () -> Molecule.of(Molecule.Kind.SIMPLE, "x", formula));
            }
            assertThrows(IllegalArgumentException.class, () -> Molecule.of(Molecule.Kind.SIMPLE, " ", "H2O"));
            assertThrows(IllegalArgumentException.class, () -> Molecule.of(null, "x", "H2O"));
        }

        @Test
        void leCatalogueTientDebout() {
            List<Molecule> molecules = Molecules.DEFAULT;
            assertEquals(430, molecules.size());
            assertEquals(430, molecules.stream().map(Molecule::id).distinct().count());
            assertEquals(430, molecules.stream().map(Molecule::name).distinct().count());
            for (Molecule.Kind kind : Molecule.Kind.values()) {
                assertTrue(molecules.stream().anyMatch(molecule -> molecule.kind() == kind), "rayon vide : " + kind);
            }
            for (Molecule molecule : molecules) {
                for (int number : molecule.recipe().keySet()) {
                    // Un élément unique ne peut pas quitter le tableau : aucune formule n'en demande.
                    assertFalse(PeriodicTable.element(number).category().unique(), molecule.id() + " demande un element unique");
                }
            }
        }

        @Test
        void chaqueMoleculeEstFaisableAuMoinsUneFoisArbreComplet() {
            Game game = gameAfterBigBang();
            game.state().setDarkLevel("dark_isotopes", 2);
            for (Molecule molecule : game.molecules()) {
                assertTrue(game.isMoleculeWithinReach(molecule.id()), molecule.id() + " demande trop d'exemplaires");
            }
            // Et une bonne moitié du catalogue l'est sans les Isotopes, avec neuf exemplaires par élément.
            game.state().setDarkLevel("dark_isotopes", 0);
            long reachable = game.molecules().stream().filter(molecule -> game.isMoleculeWithinReach(molecule.id())).count();
            assertTrue(reachable > 300 && reachable < 430, "a portee sans Isotopes : " + reachable);
            assertFalse(game.isMoleculeWithinReach(GLUCOSE));
        }

        @Test
        void lesPaliersDEspaceVontCroissant() {
            Molecule.Kind[] kinds = Molecule.Kind.values();
            assertEquals(0.0, Molecule.Kind.SIMPLE.space(), 0);
            for (int index = 1; index < kinds.length; index++) {
                assertTrue(kinds[index].space() > kinds[index - 1].space(), kinds[index] + " devrait demander plus d'espace");
            }
        }

        @Test
        void seulesLesPetitesMoleculesSontLaAuPremierBigBang() {
            Game game = gameAfterBigBang();
            game.state().setSpace(BigNum.ZERO);
            assertTrue(game.isMoleculeKindUnlocked(Molecule.Kind.SIMPLE));
            assertEquals(1, game.moleculeKindsUnlocked());
            assertEquals(Molecule.Kind.ACID, game.nextMoleculeKind());
            for (Molecule.Kind kind : Molecule.Kind.values()) {
                if (kind != Molecule.Kind.SIMPLE) assertFalse(game.isMoleculeKindUnlocked(kind), kind + " ne devrait pas etre ouvert");
            }
        }

        @Test
        void aucunRayonAvantLePremierBigBang() {
            Game game = gameAfterBigBang();
            game.state().setBigBangs(0);
            assertEquals(0, game.moleculeKindsUnlocked());
            assertFalse(game.isMoleculeKindUnlocked(Molecule.Kind.SIMPLE));
            assertEquals(Molecule.Kind.SIMPLE, game.nextMoleculeKind());
        }

        @Test
        void chaquePalierDEspaceOuvreSonRayon() {
            Game game = gameAfterBigBang();
            Molecule.Kind[] kinds = Molecule.Kind.values();
            for (int index = 1; index < kinds.length; index++) {
                game.state().setSpace(BigNum.of(kinds[index].space() - 1));
                assertFalse(game.isMoleculeKindUnlocked(kinds[index]), kinds[index] + " ouvert trop tot");
                assertEquals(index, game.moleculeKindsUnlocked());
                assertEquals(kinds[index], game.nextMoleculeKind());
                game.state().setSpace(BigNum.of(kinds[index].space()));
                assertTrue(game.isMoleculeKindUnlocked(kinds[index]), kinds[index] + " devrait etre ouvert");
                assertEquals(index + 1, game.moleculeKindsUnlocked());
            }
            assertTrue(game.nextMoleculeKind() == null);
        }

        @Test
        void unRayonFermeNeSeCreePas() {
            Game game = gameAfterBigBang();
            // Le sel occupe 280 unités : il y a la place et les éléments, mais son rayon attend 10 000 d'espace créé.
            game.state().setSpace(BigNum.of(9_999));
            assertTrue(game.hasElementsForMolecule(SALT));
            assertTrue(game.hasSpaceForMolecule(SALT));
            assertFalse(game.canCreateMolecule(SALT));
            assertFalse(game.createMolecule(SALT));
            assertTrue(game.canCreateMolecule(WATER));
            game.state().setSpace(BigNum.of(10_000));
            assertTrue(game.createMolecule(SALT));
        }

        @Test
        void remplirLEspaceNeRefermeAucunRayon() {
            Game game = gameAfterBigBang();
            game.state().setSpace(BigNum.of(2_000));
            game.state().setMoleculeCount("S8", 1);
            game.state().setMoleculeCount("I2", 1);     // 1 280 + 1 060 : l'espace est plein
            assertTrue(game.freeSpace().isZero());
            assertTrue(game.isMoleculeKindUnlocked(Molecule.Kind.ACID));
        }

        @Test
        void lExpansionOuvreLeDeuxiemeRayonEnUneDemiHeure() {
            Game game = gameAfterBigBang();
            game.state().setSpace(BigNum.ZERO);
            assertEquals(2_000.0, game.secondsUntilSpace(Molecule.Kind.ACID.space()), 1e-9);
            game.tick(1_999);
            assertFalse(game.isMoleculeKindUnlocked(Molecule.Kind.ACID));
            assertEquals(1.0, game.secondsUntilSpace(Molecule.Kind.ACID.space()), 1e-6);
            game.tick(1);
            assertTrue(game.isMoleculeKindUnlocked(Molecule.Kind.ACID));
            assertEquals(0.0, game.secondsUntilSpace(Molecule.Kind.ACID.space()), 0);
            // Deux Big Bangs : deux fois plus vite.
            game.state().setBigBangs(2);
            assertEquals(4_000.0, game.secondsUntilSpace(Molecule.Kind.SALT.space()), 1e-6);
        }

        @Test
        void pasDeMoleculeAvantLePremierBigBang() {
            Game game = gameAfterBigBang();
            game.state().setBigBangs(0);
            assertFalse(game.canCreateMolecule(WATER));
            assertFalse(game.createMolecule(WATER));
            assertEquals(0, game.moleculeCount(WATER));
            assertEquals(9, game.elementCount(H));
        }

        @Test
        void uneMoleculeDEauPrendDeuxHydrogenesEtUnOxygene() {
            Game game = gameAfterBigBang();
            assertTrue(game.createMolecule(WATER));
            assertEquals(1, game.moleculeCount(WATER));
            assertEquals(7, game.elementCount(H));
            assertEquals(8, game.elementCount(O));
            assertEquals(9, game.elementCount(C));
            assertEquals(1, game.moleculesCreated());
            assertEquals(1, game.moleculeKindsCreated());
        }

        @Test
        void unExemplaireDeChaqueElementResteDansLeTableau() {
            Game game = gameAfterBigBang();
            game.state().setElementCount(H, 2);
            assertFalse(game.hasElementsForMolecule(WATER));
            assertFalse(game.canCreateMolecule(WATER));
            game.state().setElementCount(H, 3);
            game.state().setElementCount(O, 1);
            assertFalse(game.canCreateMolecule(WATER));
            game.state().setElementCount(O, 2);
            assertTrue(game.createMolecule(WATER));
            assertEquals(1, game.elementCount(H));
            assertEquals(1, game.elementCount(O));
            assertEquals(118, game.discoveredElements());
            assertTrue(game.canExplode());
        }

        @Test
        void chaqueMoleculeDoubleLePrixDeLaSuivante() {
            Game game = gameAfterBigBang();
            assertEquals(Map.of(H, 2, O, 1), game.nextMoleculeCost(WATER));
            assertTrue(game.createMolecule(WATER));
            assertEquals(Map.of(H, 4, O, 2), game.nextMoleculeCost(WATER));
            refill(game);
            assertTrue(game.createMolecule(WATER));
            assertEquals(5, game.elementCount(H));
            assertEquals(7, game.elementCount(O));
            assertEquals(Map.of(H, 8, O, 4), game.nextMoleculeCost(WATER));
            // Le prix des autres molécules n'a pas bougé.
            assertEquals(Map.of(11, 1, 17, 1), game.nextMoleculeCost(SALT));
        }

        @Test
        void neufExemplairesDonnentTroisMoleculesDEau() {
            Game game = gameAfterBigBang();
            int created = 0;
            while (game.isMoleculeWithinReach(WATER) && created < 50) {
                refill(game);
                assertTrue(game.createMolecule(WATER));
                created++;
            }
            assertEquals(3, created);
            refill(game);
            assertFalse(game.canCreateMolecule(WATER));     // la quatrième demande seize hydrogènes
            game.state().setDarkLevel("dark_isotopes", 2);    // vingt-cinq exemplaires
            refill(game);
            assertTrue(game.createMolecule(WATER));
            assertFalse(game.isMoleculeWithinReach(WATER));   // la cinquième en demande trente-deux
        }

        @Test
        void leGlucoseAttendLesIsotopes() {
            Game game = gameAfterBigBang();
            assertFalse(game.isMoleculeWithinReach(GLUCOSE));
            game.state().setDarkLevel("dark_isotopes", 1);     // seize exemplaires par élément commun
            assertTrue(game.isMoleculeWithinReach(GLUCOSE));
            assertFalse(game.canCreateMolecule(GLUCOSE));      // le tableau n'en a encore que neuf
            refill(game);
            assertTrue(game.createMolecule(GLUCOSE));
            assertEquals(4, game.elementCount(H));
            assertEquals(10, game.elementCount(C));
            assertEquals(10, game.elementCount(O));
            assertFalse(game.isMoleculeWithinReach(GLUCOSE));   // la deuxième demande vingt-quatre hydrogènes
            game.state().setDarkLevel("dark_isotopes", 2);
            assertTrue(game.isMoleculeWithinReach(GLUCOSE));
        }

        @Test
        void uneMoleculeOccupeDixUnitesDEspaceParProton() {
            Game game = gameAfterBigBang();
            assertValue(100, game.moleculeVolume(WATER));
            assertValue(280, game.moleculeVolume(SALT));
            assertValue(960, game.moleculeVolume(GLUCOSE));
        }

        @Test
        void plusUneMoleculeEstComplexeOuLourdePlusElleDemande() {
            Game game = gameAfterBigBang();
            String[] growing = {"H2", WATER, SALT, GLUCOSE, "C20H24N2O2", "Pb5(VO4)3Cl"};
            for (int index = 1; index < growing.length; index++) {
                assertTrue(game.moleculeVolume(growing[index]).gt(game.moleculeVolume(growing[index - 1])),
                        growing[index] + " devrait occuper plus que " + growing[index - 1]);
            }
            // À nombre d'atomes égal, des éléments plus lourds prennent plus de place.
            assertEquals(game.molecule("PbS").atoms(), game.molecule("CO").atoms());
            assertTrue(game.moleculeVolume("PbS").gt(game.moleculeVolume("CO")));
        }

        @Test
        void sansEspaceLibrePasDeMolecule() {
            Game game = gameAfterBigBang();
            game.state().setSpace(BigNum.of(99));
            assertTrue(game.hasElementsForMolecule(WATER));
            assertFalse(game.hasSpaceForMolecule(WATER));
            assertFalse(game.createMolecule(WATER));
            assertEquals(9, game.elementCount(H));
            game.state().setSpace(BigNum.of(100));
            assertTrue(game.createMolecule(WATER));
            assertValue(100, game.occupiedSpace());
            assertTrue(game.freeSpace().isZero());
            refill(game);
            assertFalse(game.canCreateMolecule(WATER));      // l'espace est plein
            assertFalse(game.canCreateMolecule("H2"));
            game.state().setSpace(BigNum.of(120));
            assertTrue(game.canCreateMolecule("H2"));         // deux protons : vingt unités
        }

        @Test
        void lEspaceOccupeSAdditionneEtNeSeLiberePas() {
            Game game = gameAfterBigBang();
            assertTrue(game.occupiedSpace().isZero());
            assertTrue(game.createMolecule(WATER));
            assertTrue(game.createMolecule(SALT));
            assertTrue(game.createMolecule(WATER));
            assertValue(480, game.occupiedSpace());
            assertValue(1e9 - 480, game.freeSpace());
            assertTrue(game.explode());
            assertValue(480, game.occupiedSpace());
        }

        @Test
        void lEspaceLibreNEstJamaisNegatif() {
            Game game = gameAfterBigBang();
            game.state().setMoleculeCount(GLUCOSE, 3);
            game.state().setSpace(BigNum.of(50));
            assertValue(2880, game.occupiedSpace());
            assertTrue(game.freeSpace().isZero());
        }

        @Test
        void seulesLesPetitesMoleculesDonnentQuelqueChose() {
            for (Molecule molecule : Molecules.DEFAULT) {
                assertEquals(molecule.kind() == Molecule.Kind.SIMPLE, molecule.hasBonus(), molecule.id());
            }
        }

        @Test
        void lesBonusSontLegers() {
            for (Molecule molecule : Molecules.DEFAULT) {
                if (molecule.bonus() instanceof Molecule.Boost boost) {
                    assertTrue(boost.perMolecule() >= 0.02 && boost.perMolecule() <= 0.10, molecule.id() + " : " + boost.perMolecule());
                }
            }
            for (Molecule.Stat stat : Molecule.Stat.values()) {
                assertTrue(Molecules.DEFAULT.stream().anyMatch(molecule -> molecule.bonus() instanceof Molecule.Boost boost
                        && boost.stat() == stat), "aucune molecule pour " + stat);
            }
        }

        @Test
        void unPlafondReleveEstToujoursCeluiDUnElementDeLaFormule() {
            java.util.Set<Integer> raised = new java.util.HashSet<>();
            for (Molecule molecule : Molecules.DEFAULT) {
                if (molecule.bonus() instanceof Molecule.Uncap uncap) {
                    assertTrue(molecule.recipe().containsKey(uncap.element()), molecule.id());
                    assertTrue(raised.add(uncap.element()), "deux molecules relevent le plafond de l'element " + uncap.element());
                }
            }
            assertEquals(15, raised.size());
            assertTrue(raised.contains(H) && raised.contains(C) && raised.contains(O));
        }

        @Test
        void unBonusMalDecritEstRefuse() {
            Molecule water = Molecule.of(Molecule.Kind.SIMPLE, "Eau", "H2O");
            assertFalse(water.hasBonus());
            assertTrue(water.uncapping("O").hasBonus());
            assertThrows(IllegalArgumentException.class, () -> water.uncapping("Na"));     // pas dans la formule
            assertThrows(IllegalArgumentException.class, () -> water.uncapping("Xx"));
            assertThrows(IllegalArgumentException.class, () -> water.boosting(Molecule.Stat.SPACE, 0));
            assertThrows(IllegalArgumentException.class, () -> water.boosting(null, 0.05));
        }

        @Test
        void sansMoleculeAucunBonus() {
            Game game = gameAfterBigBang();
            for (Molecule.Stat stat : Molecule.Stat.values()) assertEquals(1.0, game.moleculeBoost(stat), 0);
            for (Element element : PeriodicTable.ELEMENTS) assertEquals(0, game.elementUncap(element.number()));
        }

        @Test
        void lesBonusDUneMemeGrandeurSAdditionnent() {
            Game game = gameAfterBigBang();
            double before = game.particlesPerCreation().log10();
            game.state().setMoleculeCount(WATER, 2);       // +5 % chacune
            game.state().setMoleculeCount("SO3", 1);       // +8 %
            assertEquals(1.18, game.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            assertEquals(before + Math.log10(1.18), game.particlesPerCreation().log10(), 1e-9);
            assertEquals(1.0, game.moleculeBoost(Molecule.Stat.ATOMS), 0);
        }

        @Test
        void leDioxydeDeCarboneAccelereUnPeuLExpansion() {
            Game game = gameAfterBigBang();
            assertValue(1, game.spacePerSecond());
            game.state().setMoleculeCount("CO2", 1);
            assertValue(1.05, game.spacePerSecond());
            game.state().setMoleculeCount("SF6", 2);
            assertValue(1.25, game.spacePerSecond());
            BigNum space = game.state().space();
            game.tick(100);
            assertValue(space.toDouble() + 125, game.state().space());
        }

        @Test
        void lAmmoniacAugmenteUnPeuLesAtomes() {
            Game game = gameAfterBigBang();
            game.state().setLevel("generator", 9);
            double before = game.atomsPerFusion().toDouble();
            game.state().setMoleculeCount("NH3", 3);
            assertEquals(1.09, game.moleculeBoost(Molecule.Stat.ATOMS), 1e-12);
            assertTrue(game.atomsPerFusion().toDouble() >= before, "atomes par fusion : " + before + " -> " + game.atomsPerFusion());
        }

        @Test
        void leProtoxydeDAzoteFaitGrossirLaMatiereNoireUnPeuPlusVite() {
            Game game = gameAfterBigBang();
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(3));
            double before = game.darkMatterMomentum();
            game.state().setMoleculeCount("N2O", 2);
            assertEquals(before * 1.10, game.darkMatterMomentum(), before * 1e-9);
        }

        @Test
        void lesMoleculesDesAutresRayonsNeDonnentRien() {
            Game game = gameAfterBigBang();
            double particles = game.particlesPerCreation().log10();
            double synthesis = game.synthesisCost().toDouble();
            int copies = game.maxTotalCopies();
            for (Molecule molecule : game.molecules()) {
                if (molecule.kind() != Molecule.Kind.SIMPLE) game.state().setMoleculeCount(molecule.id(), 2);
            }
            assertEquals(particles, game.particlesPerCreation().log10(), 1e-12);
            assertEquals(synthesis, game.synthesisCost().toDouble(), 1e-9);
            assertValue(1, game.spacePerSecond());
            assertEquals(copies, game.maxTotalCopies());
        }

        @Test
        void chaqueDihydrogeneReleveLePlafondDeLHydrogene() {
            Game game = gameAfterBigBang();
            Element hydrogen = PeriodicTable.element(H);
            int total = game.maxTotalCopies();
            assertEquals(9, game.maxCopiesOf(hydrogen));
            assertTrue(game.isElementMaxed(H));
            assertTrue(game.isPeriodicTableComplete());
            assertTrue(game.createMolecule("H2"));
            assertEquals(1, game.elementUncap(H));
            assertEquals(10, game.maxCopiesOf(hydrogen));
            // Les autres éléments de la famille, et la famille elle-même, n'ont pas bougé.
            assertEquals(9, game.maxCopiesOf(PeriodicTable.element(C)));
            assertEquals(9, game.maxCopiesOf(ElementCategory.NONMETAL));
            assertEquals(total + 1, game.maxTotalCopies());
            // Neuf hydrogènes ne suffisent plus à remplir le tableau : la synthèse peut en redonner.
            refill(game);
            assertFalse(game.isElementMaxed(H));
            assertFalse(game.isPeriodicTableComplete());
            game.state().setElementCount(H, 10);
            assertTrue(game.isElementMaxed(H));
            assertTrue(game.isPeriodicTableComplete());
            assertEquals(total + 1, game.ownedCopies());
        }

        @Test
        void unPlafondReleveMetDautresMoleculesAPortee() {
            Game game = gameAfterBigBang();
            // Le butane demande dix hydrogènes, plus celui qui reste : hors de portée avec neuf exemplaires.
            assertFalse(game.isMoleculeWithinReach("C4H10"));
            game.state().setMoleculeCount("H2", 2);
            assertEquals(11, game.maxCopiesOf(PeriodicTable.element(H)));
            assertTrue(game.isMoleculeWithinReach("C4H10"));
        }

        @Test
        void lePlafondReleveSAjouteAuxIsotopes() {
            Game game = gameAfterBigBang();
            game.state().setMoleculeCount("O2", 3);
            assertEquals(12, game.maxCopiesOf(PeriodicTable.element(O)));
            game.state().setDarkLevel("dark_isotopes", 1);
            assertEquals(19, game.maxCopiesOf(PeriodicTable.element(O)));
            assertEquals(16, game.maxCopiesOf(PeriodicTable.element(C)));
            // Un élément unique reste unique, quoi qu'il arrive.
            assertEquals(1, game.maxCopiesOf(PeriodicTable.element(2)));
        }

        @Test
        void lePlafondReleveSurvitALExplosionEtAuBigBang() {
            Game game = gameAfterBigBang();
            assertTrue(game.createMolecule("H2"));
            assertTrue(game.explode());
            assertEquals(10, game.maxCopiesOf(PeriodicTable.element(H)));
            game.reset();
            assertEquals(9, game.maxCopiesOf(PeriodicTable.element(H)));
        }

        @Test
        void lOrdreDeCreationEstGarde() {
            Game game = gameAfterBigBang();
            assertTrue(game.createMolecule(SALT));
            assertTrue(game.createMolecule(WATER));
            assertTrue(game.createMolecule(SALT));
            assertEquals(List.of(SALT, WATER, SALT), game.state().moleculeLog());
            assertEquals(2, game.moleculeCount(SALT));
            assertEquals(2, game.moleculeKindsCreated());
            // Retirer une molécule retire la dernière créée de sa sorte.
            game.state().setMoleculeCount(SALT, 1);
            assertEquals(List.of(SALT, WATER), game.state().moleculeLog());
            game.state().setMoleculeCount(WATER, 0);
            assertEquals(List.of(SALT), game.state().moleculeLog());
            assertEquals(1, game.moleculeKindsCreated());
        }

        @Test
        void lesMoleculesSurviventALExplosionEtAuBigBang() {
            Game game = gameAfterBigBang();
            assertTrue(game.createMolecule(WATER));
            assertTrue(game.explode());
            assertEquals(1, game.moleculeCount(WATER));
            assertEquals(0, game.discoveredElements());
            game.state().setParticles(Game.BIG_BANG_PARTICLES);
            game.state().setAtoms(Game.BIG_BANG_ATOMS);
            game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER);
            for (Achievement achievement : game.achievements()) game.state().addAchievement(achievement.id());
            for (Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
            assertTrue(game.bigBang());
            assertEquals(2, game.bigBangs());
            assertEquals(1, game.moleculeCount(WATER));
            assertEquals(List.of(WATER), game.state().moleculeLog());
        }

        @Test
        void laRemiseAZeroEffaceLesMolecules() {
            Game game = gameAfterBigBang();
            assertTrue(game.createMolecule(WATER));
            game.reset();
            assertEquals(0, game.moleculeCount(WATER));
            assertEquals(0, game.moleculesCreated());
            assertTrue(game.state().hasNoMolecule());
            assertTrue(game.occupiedSpace().isZero());
        }

        @Test
        void uneMoleculeInconnueEstRefusee() {
            Game game = gameAfterBigBang();
            assertThrows(IllegalArgumentException.class, () -> game.createMolecule("phlogistique"));
            assertThrows(IllegalArgumentException.class, () -> game.moleculeCount("phlogistique"));
            assertThrows(IllegalArgumentException.class, () -> game.moleculeVolume("phlogistique"));
            assertThrows(IllegalArgumentException.class, () -> game.state().setMoleculeCount(WATER, -1));
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
            Game game = game(3);
            assertValue(3, game.spacePerSecond());
            game.tick(100);
            assertValue(300, game.state().space());
        }

        @Test
        void unGrosTickVautBeaucoupDePetits() {
            Game small = game(2);
            for (int index = 0; index < 600; index++) small.tick(0.1);
            Game big = game(2);
            big.tick(60);
            assertEquals(big.state().space().toDouble(), small.state().space().toDouble(), 1e-6);
            assertValue(120, big.state().space());
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
            Game game = game(2);
            game.tick(100);
            game.state().setMoleculeCount("H2O", 1);
            assertValue(2, game.spacePerSecond());
            assertValue(200, game.state().space());
            assertValue(100, game.freeSpace());
            game.tick(50);
            assertValue(300, game.state().space());
            assertValue(200, game.freeSpace());
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
            assertValue(70, game.state().space());     // deux Big Bangs : deux unités par seconde
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

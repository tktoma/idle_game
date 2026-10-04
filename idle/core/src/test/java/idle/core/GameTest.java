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

    // Automatismes, payés en atomes. Les trois premiers sont sans délai, pour tester les achats eux-mêmes.
    private static final Automation AUTO_SPEED =
            Automation.buying("autoSpeed", "Vitesse", BigNum.of(2), "speed").instant();
    private static final Automation AUTO_GEN =
            Automation.buying("autoGen", "Générateur", BigNum.of(3), "gen").instant();
    private static final Automation AUTO_FUSION =
            Automation.fusing("autoFusion", "Fusion", BigNum.of(5)).instant();
    /** Une action toutes les 4 s, puis 2 s, puis sans délai ; la cadence coûte 2 puis 6 atomes. */
    private static final Automation AUTO_SLOW =
            new Automation("autoSlow", "Lent", BigNum.of(1), "speed", 4, 2, BigNum.of(2), 3);

    /** Partie pas encore démarrée : aucun générateur. */
    private static Game unstartedGame() {
        return new Game(new GameState(),
                List.of(SPEED, GENERATOR, HUGE, DOUBLE, MASS, PATIENCE, BOOST, DISCOUNT, KEEP),
                List.of(AUTO_SPEED, AUTO_GEN, AUTO_FUSION, AUTO_SLOW), new Random(42));
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
        void unCatalogueQuiSEmballeNeFigePasLeJeu() {
            // Dans le catalogue de test, la vitesse double à chaque niveau alors que son coût
            // ne monte que de 50 % : l'achat automatique doit rester borné à chaque tick.
            Game game = gameWithAutomation(10);
            game.buyAutomation("autoSpeed");
            game.state().setParticles(BigNum.of(1, 30));
            for (int i = 0; i < 50; i++) game.tick(0.05);
            int level = game.levelOf("speed");
            assertTrue(level > 300, "l'emballement devrait acheter beaucoup de niveaux : " + level);
            assertTrue(level <= 50 * 100, "au plus 100 niveaux par tick : " + level);
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
        void leDelaiSeDiviseParDeuxPuisDisparait() {
            assertEquals(4, AUTO_SLOW.intervalAt(0));
            assertEquals(2, AUTO_SLOW.intervalAt(1));
            assertEquals(0, AUTO_SLOW.intervalAt(2));
            assertEquals(0, AUTO_SLOW.intervalAt(7));
        }

        @Test
        void lesReglagesParDefaut() {
            Automation automation = Automation.fusing("f", "Fusion", BigNum.of(1));
            assertEquals(4, automation.intervalAt(0));
            assertEquals(0.5, automation.intervalAt(3));
            assertEquals(0, automation.intervalAt(4));
            assertValue(2, automation.speedCostAt(0));
            assertValue(16, automation.speedCostAt(3));
        }

        @Test
        void unSeulAchatParDelai() {
            Game game = gameWithSlowAutomation(0);
            assertEquals(4, game.automationInterval("autoSlow"));
            game.tick(3.9);
            assertEquals(0, game.levelOf("speed"));   // le délai n'est pas écoulé
            game.tick(0.2);
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
            game.tick(0.05);                          // achète tout de suite…
            assertEquals(1, game.levelOf("speed"));
            game.tick(3);                             // …puis le délai repart
            assertEquals(1, game.levelOf("speed"));
            game.tick(1.1);
            assertEquals(2, game.levelOf("speed"));
        }

        @Test
        void accelererCouteDesAtomesEtReduitLeDelai() {
            Game game = gameWithSlowAutomation(10);
            assertFalse(game.isAutomationInstant("autoSlow"));
            assertValue(2, game.automationSpeedCost("autoSlow"));
            assertTrue(game.canSpeedUpAutomation("autoSlow"));
            assertTrue(game.speedUpAutomation("autoSlow"));
            assertValue(8, game.state().atoms());
            assertEquals(1, game.automationSpeedLevel("autoSlow"));
            assertEquals(2, game.automationInterval("autoSlow"));

            game.tick(4.1);
            assertEquals(2, game.levelOf("speed"));   // deux achats en 4 s au lieu d'un
        }

        @Test
        void auDernierNiveauIlNYAPlusDeDelai() {
            Game game = gameWithSlowAutomation(10);
            game.speedUpAutomation("autoSlow");       // 2 atomes
            assertValue(6, game.automationSpeedCost("autoSlow"));
            game.speedUpAutomation("autoSlow");       // 6 atomes
            assertValue(2, game.state().atoms());
            assertTrue(game.isAutomationInstant("autoSlow"));
            assertEquals(0, game.automationInterval("autoSlow"));
            assertFalse(game.canSpeedUpAutomation("autoSlow"));
            assertFalse(game.speedUpAutomation("autoSlow"));
            assertValue(2, game.state().atoms());

            game.tick(0.2);
            assertTrue(game.levelOf("speed") > 5);    // tout ce qui est payable, d'un coup
        }

        @Test
        void onNAcceleresPasCeQuOnNePossedePas() {
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
    }

    @Nested
    class AutomatismesSansDelai {

        /** Partie où la fusion et l'achat de générateurs sont automatiques et sans délai. */
        private Game fullyAutomatedGame() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(1 + 3 + 5));
            game.buy("keep");
            game.buyAutomation("autoGen");
            game.buyAutomation("autoFusion");
            return game;
        }

        @Test
        void auPlusDixFusionsParSeconde() {
            Game game = fullyAutomatedGame();
            for (int i = 0; i < 100; i++) {          // 5 secondes à 20 ticks/s, particules illimitées
                game.state().setParticles(BigNum.of(1e9));
                game.tick(0.05);
            }
            double atoms = game.state().atoms().toDouble();
            assertTrue(atoms >= 40 && atoms <= 50, "fusions en 5 s : " + atoms);
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

        private static final double MAX = Game.MAX_ATOMS.toDouble();

        private Game gameAtCap() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(MAX));
            return game;
        }

        @Test
        void leTableauCompte118ElementsNumerotesDansLOrdre() {
            assertEquals(118, PeriodicTable.ELEMENTS.size());
            for (int number = 1; number <= 118; number++) {
                assertEquals(number, PeriodicTable.element(number).number());
            }
            assertEquals(118, PeriodicTable.ELEMENTS.stream().map(Element::symbol).distinct().count());
            assertEquals(118, PeriodicTable.ELEMENTS.stream().map(Element::name).distinct().count());
            assertThrows(IllegalArgumentException.class, () -> PeriodicTable.element(0));
            assertThrows(IllegalArgumentException.class, () -> PeriodicTable.element(119));
        }

        @Test
        void quelquesElementsConnus() {
            assertEquals(new Element(1, "H", "Hydrogène", ElementCategory.NONMETAL), PeriodicTable.element(1));
            assertEquals(new Element(26, "Fe", "Fer", ElementCategory.TRANSITION_METAL), PeriodicTable.element(26));
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
        void lesProbabilitesFont100PourCentEtLeRareRapportePlus() {
            double total = 0;
            for (ElementCategory category : ElementCategory.values()) total += category.chance();
            assertEquals(1, total, 1e-12);
            // Deux familles qui améliorent le même aspect : la plus rare donne plus par exemplaire.
            assertTrue(ElementCategory.METALLOID.bonusPerCopy() > ElementCategory.TRANSITION_METAL.bonusPerCopy());
            assertTrue(ElementCategory.LANTHANIDE.bonusPerCopy() > ElementCategory.NONMETAL.bonusPerCopy());
            assertTrue(ElementCategory.NOBLE_GAS.bonusPerCopy() > ElementCategory.HALOGEN.bonusPerCopy());
        }

        @Test
        void lePrixDoubleAChaqueSyntheseJusquAuPlafond() {
            Game game = newGame();
            double[] expected = {2, 4, 8, 16, 32, 64, MAX, MAX, MAX};
            for (double cost : expected) {
                assertValue(cost, game.synthesisCost());
                game.state().setAtoms(BigNum.of(MAX));
                game.synthesize();
            }
            assertEquals(expected.length, game.state().synthesisCount());
        }

        @Test
        void lePrixResteAuPlafondMemeApresDesCentainesDeSyntheses() {
            Game game = newGame();
            game.state().setSynthesisCount(5000);
            assertValue(MAX, game.synthesisCost());
        }

        @Test
        void impossibleSansAssezDAtomes() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(1));              // la première synthèse coûte 2
            assertFalse(game.canSynthesize());
            assertEquals(null, game.synthesize());
            assertValue(1, game.state().atoms());
            assertEquals(0, game.discoveredElements());
            assertEquals(0, game.state().synthesisCount());
        }

        @Test
        void laSyntheseConsommeSonPrixEtDonneUnElement() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(5));
            assertTrue(game.canSynthesize());
            Element element = game.synthesize();
            assertTrue(element != null);
            assertValue(3, game.state().atoms());             // 5 − 2
            assertEquals(1, game.elementCount(element.number()));
            assertEquals(1, game.discoveredElements());
            assertFalse(game.canSynthesize());                // la suivante coûte 4
        }

        @Test
        void laSyntheseNeToucheNiAuxAmeliorationsNiAuxGenerateurs() {
            Game game = gameReadyToFuse();
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
            Game game = gameAtCap();
            game.state().setElementCount(26, 2);
            int before = game.state().elements().values().stream().mapToInt(Integer::intValue).sum();
            game.synthesize();
            int after = game.state().elements().values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(before + 1, after);
        }

        @Test
        void lesTiragesSuiventLesProbabilitesDesFamilles() {
            Game game = newGame();                            // hasard reproductible (graine 42)
            Map<ElementCategory, Integer> draws = new EnumMap<>(ElementCategory.class);
            int total = 40_000;
            for (int i = 0; i < total; i++) {
                game.state().setAtoms(BigNum.of(MAX));
                draws.merge(game.synthesize().category(), 1, Integer::sum);
            }
            for (ElementCategory category : ElementCategory.values()) {
                double frequency = draws.getOrDefault(category, 0) / (double) total;
                assertEquals(category.chance(), frequency, 0.01,
                        () -> category.label() + " : " + frequency);
            }
            assertEquals(118, game.discoveredElements());     // tous sortent, même les plus rares
        }

        @Test
        void sansElementAucunBonus() {
            Game game = newGame();
            for (Aspect aspect : Aspect.values()) assertEquals(1, game.elementPower(aspect));
        }

        @Test
        void lesExemplairesSAdditionnent() {
            Game game = newGame();
            game.state().setElementCount(26, 3);              // fer, métal de transition : +2 % chacun
            game.state().setElementCount(14, 1);              // silicium, métalloïde : +8 %
            assertEquals(1 + 3 * 0.02 + 0.08, game.elementPower(Aspect.PARTICLES), 1e-12);
            assertEquals(1, game.elementPower(Aspect.SPEED));
            assertValue(1.14, game.particlesPerCreation());
        }

        @Test
        void lesNonMetauxAccelerentLaCreation() {
            Game game = newGame();
            game.state().setElementCount(8, 5);               // oxygène : +2 % chacun
            assertValue(BASE * 1.10, game.speed());
        }

        @Test
        void lesMetauxPauvresBaissentLeCoutDesGenerateurs() {
            Game game = newGame();
            assertValue(100, game.costOf("gen"));
            game.state().setElementCount(13, 10);             // aluminium : puissance 1,30
            assertValue(Math.ceil(100 / 1.30), game.costOf("gen"));
            assertValue(10, game.costOf("speed"));
        }

        @Test
        void lesAlcalinsBaissentLeCoutDeLaVitesse() {
            Game game = newGame();
            game.state().setElementCount(11, 25);             // sodium : puissance 2
            assertValue(5, game.costOf("speed"));
            assertValue(100, game.costOf("gen"));
        }

        @Test
        void lesHalogenesEtGazNoblesDonnentPlusDAtomes() {
            Game game = gameReadyToFuse();
            assertValue(1, game.atomsPerFusion());
            game.state().setElementCount(17, 2);              // chlore : +5 % chacun
            game.state().setElementCount(2, 1);               // hélium : +15 %
            assertValue(1.25, game.atomsPerFusion());
            game.fuse();
            assertValue(1.25, game.state().atoms());
            assertValue(1.25, game.state().totalAtoms());
        }

        @Test
        void lesAlcalinoTerreuxRaccourcissentLeDelaiDesAutomatismes() {
            Game game = newGame();
            game.state().setAtoms(BigNum.of(2));
            game.buy("keep");
            game.buyAutomation("autoSlow");
            assertEquals(4, game.automationInterval("autoSlow"));
            game.state().setElementCount(12, 20);             // magnésium : puissance 2
            assertEquals(2, game.automationInterval("autoSlow"), 1e-12);
        }

        @Test
        void lesActinidesAmeliorentTout() {
            Game game = newGame();
            game.state().setElementCount(92, 2);              // uranium : +10 % chacun, partout
            for (Aspect aspect : Aspect.values()) {
                assertEquals(1.2, game.elementPower(aspect), 1e-12);
            }
            assertValue(1.2, game.particlesPerCreation());
            assertValue(BASE * 1.2, game.speed());
            assertValue(1.2, game.atomsPerFusion());
            assertValue(Math.ceil(100 / 1.2), game.costOf("gen"));
            assertValue(Math.ceil(10 / 1.2), game.costOf("speed"));
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

    /** Redébloque tous les générateurs puis fusionne. */
    private static void fuseAgain(Game game) {
        game.state().setParticles(BigNum.of(1e9));
        game.buy("gen");
        game.buy("gen");
        assertTrue(game.fuse());
    }
}

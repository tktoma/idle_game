package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import idle.core.upgrades.Upgrade;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : le démarrage, le temps, les achats, les générateurs, la fusion,
 * le plafond d'atomes et les améliorations en atomes.
 */
class JeuParticulesTest extends GameTestSupport {


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
            for (int i = 0; i < 20 * 300; i++) small.tick(0.05);  // 5 min à 20 ticks/s
            Game big = gameWithParticles(100);
            big.buy("gen");
            big.tick(300);                                         // 5 min hors-ligne
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
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameTest {

    /** Vitesse de départ d'un point de création, en particules par seconde. */
    private static final double BASE = Game.BASE_SPEED.toDouble();

    /** Temps nécessaire pour former une particule au départ, en secondes. */
    private static final double ONE_PARTICLE = 1 / BASE;

    // Catalogue fixe pour les tests : ils ne cassent pas quand le vrai catalogue change.
    private static final Upgrade SPEED = new Upgrade("speed", "Vitesse",
            BigNum.of(10), 1.5, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(2));
    private static final Upgrade GENERATOR = new Upgrade("gen", "Point de création",
            BigNum.of(100), 10, 2, new Effect.AddGenerator());
    private static final Upgrade HUGE = new Upgrade("huge", "Énorme",
            BigNum.of(1), 2, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(1e30));

    /** Partie pas encore démarrée : aucun générateur. */
    private static Game unstartedGame() {
        return new Game(new GameState(), List.of(SPEED, GENERATOR, HUGE));
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
            Upgrade costly = new Upgrade("x", "X", BigNum.of(1, 100), 2, Upgrade.NO_LIMIT, SPEED.effect());
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

        /** Partie avec tous les générateurs du catalogue de test débloqués (trois). */
        private Game gameReadyToFuse() {
            Game game = gameWithParticles(1e9);
            game.buy("gen");
            game.buy("gen");
            game.buy("speed");
            game.tick(ONE_PARTICLE / 8);
            return game;
        }

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
        void chaqueAtomeAjouteUneParticuleParCreation() {
            Game game = gameReadyToFuse();
            assertValue(1, game.particlesPerCreation());
            game.fuse();
            assertValue(2, game.particlesPerCreation());
            assertValue(BASE * 2, game.productionPerSecond());

            game.tick(ONE_PARTICLE);       // une création terminée
            assertValue(2, game.state().particles());
            game.tick(ONE_PARTICLE * 3);   // trois de plus
            assertValue(8, game.state().particles());
        }

        @Test
        void lesAtomesSeCumulentDeFusionEnFusion() {
            Game game = gameReadyToFuse();
            game.fuse();
            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen");
            game.buy("gen");
            assertTrue(game.fuse());
            assertValue(2, game.state().atoms());
            assertValue(3, game.particlesPerCreation());
        }

        @Test
        void leTempsDeJeuNEstPasRemisAZero() {
            Game game = gameReadyToFuse();
            double before = game.state().timePlayed();
            game.fuse();
            assertEquals(before, game.state().timePlayed());
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
                for (Upgrade upgrade : game.upgrades()) {
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

        /** Chaque atome doit raccourcir la partie suivante, sans la rendre instantanée. */
        @Test
        void chaqueFusionRaccourcitLaPartieSuivante() {
            Game game = new Game();
            game.start();
            double previous = minutesUntilFusion(game);
            for (int fusion = 1; fusion <= 4; fusion++) {
                assertTrue(game.fuse());
                double minutes = minutesUntilFusion(game);
                assertTrue(minutes < previous, "la partie " + (fusion + 1) + " n'est pas plus courte : " + minutes + " min");
                assertTrue(minutes > 5, "la partie " + (fusion + 1) + " est trop courte : " + minutes + " min");
                previous = minutes;
            }
        }
    }
}
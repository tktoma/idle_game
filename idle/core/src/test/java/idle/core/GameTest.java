package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameTest {

    // Catalogue fixe pour les tests : ils ne cassent pas quand le vrai catalogue change.
    private static final Upgrade ADD = new Upgrade("add", "Ajout",
            BigNum.of(10), 1.5, new Effect.AddProduction(BigNum.of(2)));
    private static final Upgrade MUL = new Upgrade("mul", "Multiplicateur",
            BigNum.of(100), 10, new Effect.MultiplyProduction(3));

    private static Game newGame() {
        return new Game(new GameState(), List.of(ADD, MUL));
    }

    private static Game gameWithMatter(double matter) {
        Game game = newGame();
        game.state().setMatter(BigNum.of(matter));
        return game;
    }

    private static void assertValue(double expected, BigNum actual) {
        assertEquals(expected, actual.toDouble(), Math.abs(expected) * 1e-9 + 1e-9);
    }

    @Nested
    class Temps {

        @Test
        void unePartieNeuveCommenceAZero() {
            Game game = newGame();
            assertEquals(BigNum.ZERO, game.state().matter());
            assertEquals(0, game.state().timePlayed());
            assertValue(1, game.productionPerSecond());
        }

        @Test
        void laMatiereAugmenteAvecLeTemps() {
            Game game = newGame();
            game.tick(10);
            assertValue(10, game.state().matter());
            assertEquals(10, game.state().timePlayed(), 1e-9);
        }

        @Test
        void unTickDeZeroNeChangeRien() {
            Game game = gameWithMatter(42);
            game.tick(0);
            assertValue(42, game.state().matter());
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
            Game small = newGame();
            for (int i = 0; i < 20 * 3600; i++) small.tick(0.05); // 1 h à 20 ticks/s
            Game big = newGame();
            big.tick(3600);                                        // 1 h hors-ligne
            assertEquals(big.state().matter().toDouble(), small.state().matter().toDouble(), 1e-6);
        }
    }

    @Nested
    class Achats {

        @Test
        void acheterRetireLeCoutEtMonteLeNiveau() {
            Game game = gameWithMatter(25);
            assertTrue(game.buy("add"));
            assertValue(15, game.state().matter());
            assertEquals(1, game.levelOf("add"));
        }

        @Test
        void achatRefuseSansAssezDeMatiere() {
            Game game = gameWithMatter(9.99);
            assertFalse(game.canBuy("add"));
            assertFalse(game.buy("add"));
            assertValue(9.99, game.state().matter());
            assertEquals(0, game.levelOf("add"));
        }

        @Test
        void achatPossibleAvecExactementLeCout() {
            Game game = gameWithMatter(10);
            assertTrue(game.canBuy("add"));
            assertTrue(game.buy("add"));
            assertEquals(BigNum.ZERO, game.state().matter());
        }

        @Test
        void leCoutAugmenteAChaqueNiveau() {
            Game game = gameWithMatter(1000);
            assertValue(10, game.costOf("add"));
            game.buy("add");
            assertValue(15, game.costOf("add"));
            game.buy("add");
            assertValue(22.5, game.costOf("add"));
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
                    () -> new Game(new GameState(), List.of(ADD, ADD)));
        }
    }

    @Nested
    class Production {

        @Test
        void effetAdditif() {
            Game game = gameWithMatter(1000);
            game.buy("add");
            game.buy("add");
            assertValue(1 + 2 * 2, game.productionPerSecond());
        }

        @Test
        void effetMultiplicatif() {
            Game game = gameWithMatter(1e6);
            game.buy("mul");
            game.buy("mul");
            assertValue(1 * 3 * 3, game.productionPerSecond());
        }

        @Test
        void lesAjoutsSontMultiplies() {
            Game game = gameWithMatter(1e6);
            game.buy("add");
            game.buy("mul");
            assertValue((1 + 2) * 3, game.productionPerSecond());
        }

        @Test
        void laNouvelleProductionSAppliqueAuTickSuivant() {
            Game game = gameWithMatter(10);
            game.buy("add");   // matière : 0, production : 3/s
            game.tick(4);
            assertValue(12, game.state().matter());
        }
    }

    @Nested
    class Simulation {

        /** Joue 10 heures en achetant tout ce qui est achetable : sert de base à l'équilibrage. */
        @Test
        void dixHeuresDeJeuAvecLeCatalogueParDefaut() {
            Game game = new Game();
            double dt = 0.05;
            for (int i = 0; i < 20 * 3600 * 10; i++) {
                game.tick(dt);
                for (Upgrade upgrade : game.upgrades()) {
                    while (game.buy(upgrade.id())) { /* achète tant que possible */ }
                }
            }
            assertEquals(36_000, game.state().timePlayed(), 1e-3);
            assertTrue(game.state().matter().sign() >= 0);
            assertTrue(game.productionPerSecond().gt(BigNum.of(1000)),
                    () -> "production trop faible : " + game.productionPerSecond().format());
        }
    }
}
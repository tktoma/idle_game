package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le rythme du jeu : le tempo qui a ramené la première explosion de vingt-quatre heures à trois,
 * l'automatisme des générateurs qui les achète tous d'un coup, et le garde-fou du hasard à la
 * synthèse. Les durées elles-mêmes sont gardées par les essais de simulation de {@link GameTest}.
 */
class RythmeTest {

    private static final Upgrade TENFOLD = new Upgrade("tenfold", "×10", Resource.PARTICLES,
            BigNum.ONE, 1, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(10));
    private static final Upgrade SPEED = new Upgrade("speed", "Vitesse", Resource.PARTICLES,
            BigNum.of(10), 1.5, Upgrade.NO_LIMIT, new Effect.MultiplySpeed(2));
    /** Quatre générateurs de plus : 100, 1 000, 10 000 puis 100 000 particules. */
    private static final Upgrade GENERATOR = new Upgrade("gen", "Générateur", Resource.PARTICLES,
            BigNum.of(100), 10, 4, new Effect.AddGenerator());
    private static final Upgrade KEEP = new Upgrade("keep", "Persistance", Resource.ATOMS,
            BigNum.of(1), 1, 1, new Effect.KeepUpgradesOnFusion());
    /** Une action toutes les 4 secondes : de quoi compter les actions une à une. */
    private static final Automation AUTO_GEN =
            Automation.buying("autoGen", "Générateur", BigNum.of(1), "gen").withCadence(4, 0, BigNum.ONE, 1);
    private static final Automation AUTO_SPEED =
            Automation.buying("autoSpeed", "Vitesse", BigNum.of(1), "speed").withCadence(4, 0, BigNum.ONE, 1);

    /** Partie d'essai dont les deux automatismes sont achetés. */
    private static Game automated() {
        Game game = new Game(new GameState(), List.of(TENFOLD, SPEED, GENERATOR, KEEP), List.of(AUTO_GEN, AUTO_SPEED),
                new Random(42));
        game.start();
        game.state().setAtoms(BigNum.of(3));
        game.buy("keep");
        game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        assertTrue(game.buyAutomation("autoGen"));
        assertTrue(game.buyAutomation("autoSpeed"));
        game.setAutomationEnabled("autoSpeed", false);
        return game;
    }

    /** Le vrai jeu, tableau périodique ouvert, avec de quoi payer n'importe quelle synthèse. */
    private static Game chemist(long seed) {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(seed));
        game.start();
        game.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        rich(game);
        return game;
    }

    private static void rich(Game game) {
        game.state().setAtoms(game.atomCap());
    }

    @Nested
    class Tempo {

        @Test
        void laVitesseDeDepartEstCelleDAvantMultiplieeParLeTempo() {
            assertEquals(0.25 * Game.TEMPO, Game.BASE_SPEED.toDouble(), 1e-12);
            Game game = new Game();
            game.start();
            assertEquals(Game.BASE_SPEED.toDouble(), game.productionPerSecond().toDouble(), 1e-12);
        }

        @Test
        void laMatiereNoireCompteLaProductionAPartirDuTempo() {
            // Sans cela, la vitesse de départ multipliée aurait aussi gonflé l'élan de la matière noire.
            Game game = new Game(new GameState(), List.of(TENFOLD), List.of(), new Random(42));
            game.start();
            assertEquals(1, game.darkMatterProgressFactor(), 1e-12);       // un générateur de départ : rien encore
            game.state().setLevel("tenfold", 1);                           // 2,5 fois le tempo
            assertEquals(1 + Math.log10(2.5), game.darkMatterProgressFactor(), 1e-9);
            game.state().setLevel("tenfold", 13);                          // douze ordres de grandeur de plus
            assertEquals(13 + Math.log10(2.5), game.darkMatterProgressFactor(), 1e-9);
        }

        @Test
        void leSeuilDuRendementDeFusionAGrandiCommeLaVitesse() {
            Upgrade yield = Upgrades.DEFAULT.stream().filter(u -> u.id().equals("atom_yield")).findFirst().orElseThrow();
            Effect.MultiplyAtomsByProduction effect = (Effect.MultiplyAtomsByProduction) yield.effect();
            assertEquals(50_000_000 * Game.TEMPO, effect.threshold(), 1e-3);
        }
    }

    @Nested
    class AchatGroupeDesGenerateurs {

        @Test
        void uneSeuleActionAcheteTousLesGenerateursPayables() {
            Game game = automated();
            game.state().setParticles(BigNum.of(1e9));
            game.tick(4.1);                                   // le délai d'une action, pas de deux
            assertEquals(4, game.levelOf("gen"));
            assertEquals(5, game.generatorCount());
        }

        @Test
        void elleSArreteAuPremierGenerateurTropCher() {
            Game game = automated();
            game.tick(4.1);                                   // l'automatisme est prêt, sans rien pouvoir payer
            assertEquals(0, game.levelOf("gen"));
            game.state().setParticles(BigNum.of(1_150));      // 100 + 1 000 : deux générateurs, pas le troisième
            game.tick(0.1);
            assertEquals(2, game.levelOf("gen"));
            assertTrue(game.state().particles().lt(BigNum.of(10_000)));
        }

        @Test
        void lesAutresAutomatismesAchetentToujoursUnNiveauParAction() {
            Game game = automated();
            game.setAutomationEnabled("autoGen", false);
            game.setAutomationEnabled("autoSpeed", true);
            game.state().setParticles(BigNum.of(1e9));
            game.tick(4.1);
            assertEquals(1, game.levelOf("speed"));
            game.tick(4);
            assertEquals(2, game.levelOf("speed"));
        }

        @Test
        void lAchatGroupeCouteExactementAutantQueLesAchatsUnAUn() {
            // Avec une réduction des générateurs, les prix arrondis doivent être ceux des achats pièce par pièce,
            // et l'action doit prendre tout ce qui est payable, pas un de moins.
            Upgrade discount = new Upgrade("discount", "Compression", Resource.ATOMS, BigNum.ONE, 1, 3,
                    new Effect.DiscountGenerators(0.37));
            for (double particles : new double[] {99, 150, 1_234, 11_500, 123_456, 1e9}) {
                Game bulk = new Game(new GameState(), List.of(TENFOLD, SPEED, GENERATOR, KEEP, discount),
                        List.of(AUTO_GEN, AUTO_SPEED), new Random(42));
                Game twin = new Game(new GameState(), List.of(TENFOLD, SPEED, GENERATOR, KEEP, discount),
                        List.of(AUTO_GEN, AUTO_SPEED), new Random(42));
                for (Game game : List.of(bulk, twin)) {
                    game.start();
                    game.state().setLevel("discount", 2);
                }
                bulk.state().setAtoms(BigNum.of(3));
                bulk.buy("keep");
                bulk.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
                bulk.buyAutomation("autoGen");
                bulk.tick(3.99);                                  // l'automatisme est prêt, sans rien pouvoir payer
                assertEquals(0, bulk.levelOf("gen"));
                bulk.state().setParticles(BigNum.of(particles));
                BigNum spentBefore = bulk.stats().particlesSpent();
                bulk.tick(0.02);
                int bought = bulk.levelOf("gen");
                double spent = bulk.stats().particlesSpent().subtract(spentBefore).toDouble();
                assertEquals(bought, bulk.stats().generatorsBought(), "générateurs comptés avec " + particles);
                if (bought == 0) {
                    assertTrue(twin.costOf("gen").toDouble() > particles, "rien de payable avec " + particles);
                    continue;
                }
                assertEquals(twin.costOf("gen", bought).toDouble(), spent, 1e-6, "dépense avec " + particles);
                assertTrue(spent <= particles + 2, "payé avec " + particles);   // + ce que crée la production pendant le pas
                if (bought < 4) {
                    assertTrue(twin.costOf("gen", bought + 1).toDouble() > particles + 2, "un de plus était payable avec " + particles);
                }
            }
        }

        @Test
        void aLaMainUnClicAcheteUnSeulGenerateur() {
            Game game = automated();
            game.setAutomationEnabled("autoGen", false);
            game.state().setParticles(BigNum.of(1e9));
            assertTrue(game.buy("gen"));
            assertEquals(1, game.levelOf("gen"));
        }
    }

    @Nested
    class GardeFouDuHasard {

        /** Tout le tableau connu en un exemplaire, sauf le fer. */
        private Game allButIron(long seed) {
            Game game = chemist(seed);
            for (Element element : PeriodicTable.ELEMENTS) {
                if (element.number() != 26) game.state().setElementCount(element.number(), 1);
            }
            return game;
        }

        @Test
        void apresDeuxSynthesesSansRienDeNouveauLaSuivanteDonneUnElementInconnu() {
            assertEquals(2, Game.LUCK_FLOOR);
            // Le fer est un élément parmi 118 : au hasard seul, il se ferait attendre des dizaines de synthèses.
            for (long seed = 1; seed <= 40; seed++) {
                Game game = allButIron(seed);
                int syntheses = 0;
                while (game.state().elementCount(26) == 0) {
                    assertEquals(game.state().drySyntheses() >= Game.LUCK_FLOOR, game.isNextSynthesisNew());
                    rich(game);
                    assertFalse(game.synthesize().isEmpty());
                    syntheses++;
                    assertTrue(syntheses <= Game.LUCK_FLOOR + 1, "tirage " + seed + " : " + syntheses + " synthèses sans le fer");
                }
                assertEquals(0, game.state().drySyntheses());         // un élément nouveau : le compte repart
                assertFalse(game.isNextSynthesisNew());
            }
        }

        @Test
        void unElementNouveauRemetLeCompteAZero() {
            Game game = chemist(7);
            game.state().setDrySyntheses(1);
            game.synthesize();                                        // tableau vide : forcément nouveau
            assertEquals(0, game.state().drySyntheses());
        }

        @Test
        void unDoublonFaitMonterLeCompte() {
            Game game = allButIron(3);
            game.state().setElementCount(26, 1);                      // plus rien à découvrir
            game.synthesize();
            assertEquals(1, game.state().drySyntheses());
            rich(game);
            game.synthesize();
            assertEquals(2, game.state().drySyntheses());
            assertFalse(game.isNextSynthesisNew());                   // rien d'inconnu à promettre
            rich(game);
            assertFalse(game.synthesize().isEmpty());                 // et la synthèse reste un tirage ordinaire
        }

        @Test
        void lExplosionRemetLeCompteAZero() {
            Game game = chemist(5);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            game.state().setDrySyntheses(2);
            assertTrue(game.explode());
            assertEquals(0, game.state().drySyntheses());
        }

        @Test
        void leCompteEstSauvegarde() throws SaveCodec.Unreadable {
            Game game = chemist(5);
            game.state().setDrySyntheses(2);
            GameState back = SaveCodec.read(SaveCodec.write(game.state(), 0)).state();
            assertEquals(2, back.drySyntheses());
        }

        @Test
        void unCompteNegatifEstRameneAZero() {
            GameState state = new GameState();
            state.setDrySyntheses(-3);
            assertEquals(0, state.drySyntheses());
        }
    }
}

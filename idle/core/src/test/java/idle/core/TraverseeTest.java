package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.TreeBubble;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.175 : la bulle de l'arbre mûr, à gauche, fait traverser les univers au collisionneur, et lui donne 20 %
 * d'énergie et de hadrons de plus par univers formé. Chaque univers va plus vite que le précédent.
 */
class TraverseeTest {

    /** Le troisième univers, le collisionneur en marche, les hadrons ouverts et des quarks pour les assembler. */
    private static Game avecHadrons() {
        Game game = VideTest.third();
        game.state().setColliderGenerators(ColliderGenerator.BEAM, 9);
        game.state().setHadronsOpen(true);
        game.state().setQuarks(Quark.UP, 2_000);
        game.state().setQuarks(Quark.DOWN, 1_000);
        return game;
    }

    private static void acheteLaBulle(Game game) {
        game.state().setTreeBubbleLevel(TreeBubble.MATURE_LEFT.ordinal(), 1);
    }

    @Test
    void sansLaBulleRienNeChange() {
        Game game = avecHadrons();
        assertEquals(1, game.crossingGain(), 0);
        assertEquals(100, game.bindHadrons(Hadron.PROTON, 100), "100 assemblés, 100 venus");
        assertTrue(Multipliers.of(game, Multipliers.Quantity.COLLIDER_ENERGY).stream()
                .noneMatch(factor -> factor.label().startsWith("Arbre mûr")), "pas de facteur sans la bulle");
    }

    @Test
    void vingtPourCentDEnergieParUniversForme() {
        Game game = avecHadrons();
        double before = game.colliderEnergyPerSecond();
        acheteLaBulle(game);
        int universes = game.universes();
        assertTrue(universes >= 2, "au moins deux univers formés : " + universes);
        assertEquals(1 + 0.2 * universes, game.crossingGain(), 1e-12);
        assertEquals(before * (1 + 0.2 * universes), game.colliderEnergyPerSecond(), before * 1e-12);
        assertTrue(game.testAddUniverse());
        assertEquals(1 + 0.2 * (universes + 1), game.crossingGain(), 1e-12, "un univers de plus, 20 % de plus");
    }

    /** Le garde-fou du rythme : à production égale, le même seuil d'énergie produite vient de plus en plus vite. */
    @Test
    void chaqueUniversAtteintLeSeuilPlusVite() {
        Game game = avecHadrons();
        acheteLaBulle(game);
        double threshold = 1e15;
        double previous = Double.MAX_VALUE;
        int start = game.universes();
        for (int n = 0; n < 6; n++) {
            game.state().setColliderGenerators(ColliderGenerator.BEAM, 9);
            double seconds = threshold / game.colliderEnergyPerSecond();
            assertTrue(seconds < previous, "univers " + game.universes() + " : " + seconds + " s, avant " + previous + " s");
            previous = seconds;
            assertTrue(game.testAddUniverse());
        }
        // Six univers de plus : +120 %, et le même seuil prend d'autant moins de temps.
        assertEquals(start + 6, game.universes());
        assertEquals(1 + 0.2 * (start + 6), game.crossingGain(), 1e-12);
    }

    @Test
    void lesHadronsAssemblesDUnCoupGagnentLeurPart() {
        Game game = avecHadrons();
        acheteLaBulle(game);
        double gain = game.crossingGain();
        long came = game.bindHadrons(Hadron.PROTON, 100);
        assertEquals((long) Math.floor(100 * gain + 1e-9), came, "gain ×" + gain);
        assertEquals(came, game.hadrons(Hadron.PROTON));
        assertEquals(2_000 - 200, game.state().quarks(Quark.UP), "les quarks ne paient que les 100 assemblés");
        assertEquals(1_000 - 100, game.state().quarks(Quark.DOWN));
    }

    @Test
    void unAUnLeMemeNombreQueDUnCoup() {
        Game game = avecHadrons();
        acheteLaBulle(game);
        double gain = game.crossingGain();
        for (int i = 0; i < 100; i++) {
            assertTrue(game.bindHadron(Hadron.PROTON), "le bouton « un » marche même quand il en vient deux");
        }
        assertEquals((long) Math.floor(100 * gain + 1e-9), game.hadrons(Hadron.PROTON), "un à un comme d'un coup, gain ×" + gain);
    }

    /** Des hadrons gardés d'un univers où le gain était plus petit : le nouvel assemblage prend le gain d'aujourd'hui. */
    @Test
    void desHadronsGardesPrennentLeGainDuMoment() {
        Game game = avecHadrons();
        game.state().setHadrons(Hadron.PROTON, 500);
        acheteLaBulle(game);
        double gain = game.crossingGain();
        long came = game.bindHadrons(Hadron.PROTON, 100);
        assertTrue(came >= 100, "jamais moins que les assemblés : " + came);
        assertEquals(100 * gain, came, 1.5, "à peu près 100 × " + gain);
    }

    @Test
    void laDecompositionMontreLeGain() {
        Game game = VideTest.open();
        acheteLaBulle(game);
        game.tick(1);
        MultipliersTest.assertAddsUp(game);
        List<Multipliers.Factor> collider = Multipliers.of(game, Multipliers.Quantity.COLLIDER_ENERGY);
        assertTrue(collider.stream().anyMatch(factor -> factor.label().equals("Arbre mûr, à gauche : " + game.universes() + " univers formés")
                && factor.aspect() == Multipliers.Aspect.UNIVERSE
                && Math.abs(factor.log10() - Math.log10(game.crossingGain())) < 1e-12), collider.toString());
    }

    @Test
    void leTexteDeLaBulleDitLeGain() {
        assertTrue(TreeBubble.MATURE_LEFT.effect().contains("20 % d'énergie et de hadrons par univers formé"),
                TreeBubble.MATURE_LEFT.effect());
        assertEquals(0.20, Game.CROSSING_GAIN_PER_UNIVERSE, 0);
    }
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Le détail des multiplicateurs (version 0.123) : ses facteurs redonnent toujours la grandeur. */
class MultipliersTest {

    /** La somme des facteurs, en ordres de grandeur, est la grandeur elle-même. */
    static void assertAddsUp(Game game) {
        for (Multipliers.Quantity quantity : Multipliers.Quantity.values()) {
            if (!Multipliers.isAvailable(game, quantity)) continue;
            List<Multipliers.Factor> factors = Multipliers.of(game, quantity);
            double sum = 0;
            for (Multipliers.Factor factor : factors) sum += factor.log10();
            double total = Multipliers.total(game, quantity);
            assertEquals(total, sum, 1e-6 * Math.max(1, Math.abs(total)), quantity + " : " + factors);
            double shares = 0;
            List<Multipliers.Share> byAspect = Multipliers.byAspect(factors);
            for (Multipliers.Share share : byAspect) shares += Multipliers.shareOf(share, byAspect);
            if (byAspect.stream().anyMatch(share -> share.log10() > 0)) assertEquals(1, shares, 1e-9, quantity + " : les parts font le tout");
        }
    }

    @Test
    void uneNouvellePartieSeDecompose() {
        Game game = new Game(42L);
        assertFalse(Multipliers.isAvailable(game, Multipliers.Quantity.PRODUCTION));
        game.start();
        assertTrue(Multipliers.isAvailable(game, Multipliers.Quantity.PRODUCTION));
        for (int second = 0; second < 600; second++) {
            game.tick(1);
            game.buyAllWithParticles();
        }
        assertAddsUp(game);
    }

    @Test
    void unePartieRicheSeDecompose() {
        Game game = new Game(42L);
        try {
            idle.core.save.SaveCodec.readInto(game.state(), idle.core.save.SaveCodec.write(SaveTest.richState(), 0));
        } catch (Exception unreadable) {
            throw new AssertionError(unreadable);
        }
        game.restored();
        assertAddsUp(game);
        assertTrue(Multipliers.of(game, Multipliers.Quantity.PRODUCTION).size() > 5);
    }

    @Test
    void lesFreinsVontALaFin() {
        Game game = GameTestSupport.realGame();
        // Sous Gradle, le premier élan est coupé (idle.sansPremierElan) : on le remet pour avoir un frein à ranger.
        game.setFirstRunPace(Game.FIRST_RUN_PACE);
        game.tick(5);
        assertEquals(13, game.firstRunSlowdown(), 0, "la première partie crée treize fois moins vite");
        List<Multipliers.Factor> factors = Multipliers.of(game, Multipliers.Quantity.PRODUCTION);
        Multipliers.Factor brake = factors.stream().filter(factor -> factor.aspect() == Multipliers.Aspect.BRAKE).findFirst().orElseThrow();
        assertEquals("Premier élan", brake.label());
        assertEquals(-Math.log10(13), brake.log10(), 1e-12, "un frein : des ordres de grandeur en moins");
        List<Multipliers.Share> shares = Multipliers.byAspect(factors);
        assertTrue(shares.size() >= 2, "la base et le frein au moins : " + shares);
        Multipliers.Share last = shares.get(shares.size() - 1);
        assertEquals(Multipliers.Aspect.BRAKE, last.aspect(), "les freins ferment la liste");
        assertEquals(0, Multipliers.shareOf(last, shares), "un frein n'a pas de part des ordres de grandeur gagnés");
        for (int i = 0; i < shares.size() - 1; i++) assertTrue(shares.get(i).log10() > 0, shares.get(i) + " n'est pas un frein");
        assertAddsUp(game);
        assertTrue(game.productionPerSecond().gt(BigNum.ZERO));
    }

    /** Version 0.144 : l'énergie du collisionneur, l'énergie du vide et les segments de la ligne se décomposent aussi. */
    @Test
    void leCollisionneurLeVideEtLaLigneSeDecomposent() {
        Game fresh = GameTestSupport.realGame();
        for (Multipliers.Quantity quantity : List.of(Multipliers.Quantity.COLLIDER_ENERGY, Multipliers.Quantity.VOID_ENERGY,
                Multipliers.Quantity.LINE_SHARDS)) {
            assertFalse(Multipliers.isAvailable(fresh, quantity), quantity + " : rien avant son ouverture");
        }
        Game game = VideTest.open();
        game.state().setColliderGenerators(idle.core.collider.ColliderGenerator.BEAM, 7);
        game.state().setColliderUpgradeLevel(idle.core.collider.ColliderUpgrade.ENERGY, 3);
        game.state().setVoidLevel(Game.VoidUpgrade.RATE.ordinal(), 4);
        game.state().setVoidLevel(Game.VoidUpgrade.GAIN.ordinal(), 2);
        game.state().setLineTicksLevel(5);
        game.state().setLineYieldLevel(2);
        game.state().setLineBallsLevel(3);
        game.state().setLineCountLevel(1);
        game.state().setLineSpeedLevel(4);
        game.tick(1);
        assertTrue(Multipliers.isAvailable(game, Multipliers.Quantity.COLLIDER_ENERGY));
        assertTrue(Multipliers.isAvailable(game, Multipliers.Quantity.VOID_ENERGY), "des paires naissent : " + game.voidGainPerSecond());
        assertTrue(Multipliers.isAvailable(game, Multipliers.Quantity.LINE_SHARDS));
        assertAddsUp(game);
        List<Multipliers.Factor> collider = Multipliers.of(game, Multipliers.Quantity.COLLIDER_ENERGY);
        assertTrue(collider.stream().anyMatch(factor -> factor.label().equals("8 faisceaux croisés")), collider.toString());
        assertTrue(collider.stream().anyMatch(factor -> factor.label().endsWith("3 niveaux") && Math.abs(factor.log10() - 3 * Math.log10(2)) < 1e-12),
                "énergie ×2 par niveau : " + collider);
        List<Multipliers.Factor> line = Multipliers.of(game, Multipliers.Quantity.LINE_SHARDS);
        assertTrue(line.stream().anyMatch(factor -> factor.label().equals("4 boules par ligne")), line.toString());
        assertTrue(line.stream().anyMatch(factor -> factor.label().equals("2 lignes")), line.toString());
        // Le vide : la borne d'énergie ou d'espace est un frein, nommé d'après ce qui borne ; sans borne, il n'y a pas de frein.
        List<Multipliers.Factor> vide = Multipliers.of(game, Multipliers.Quantity.VOID_ENERGY);
        boolean bounded = game.voidRate() < game.voidBaseRate() - 1e-9;
        assertEquals(bounded, vide.stream().anyMatch(factor -> factor.aspect() == Multipliers.Aspect.BRAKE), vide.toString());
        if (bounded) assertTrue(vide.stream().anyMatch(factor -> factor.label().contains(game.voidLimit())), vide.toString());
        assertTrue(vide.stream().anyMatch(factor -> factor.label().equals("Gain par paire : 2 niveaux")), vide.toString());
    }
}

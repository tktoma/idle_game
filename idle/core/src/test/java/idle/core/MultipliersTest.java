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
        Game game = new Game();
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
        Game game = new Game();
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
        Game game = new Game();
        game.start();
        game.tick(5);
        List<Multipliers.Share> shares = Multipliers.byAspect(Multipliers.of(game, Multipliers.Quantity.PRODUCTION));
        if (game.firstRunSlowdown() > 1) {
            Multipliers.Share last = shares.get(shares.size() - 1);
            assertEquals(Multipliers.Aspect.BRAKE, last.aspect());
            assertEquals(0, Multipliers.shareOf(last, shares));
        }
        assertTrue(game.productionPerSecond().gt(BigNum.ZERO));
    }
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import java.util.List;
import org.junit.jupiter.api.Test;

/** La maturation des astres (version 0.53) : un astre formé mûrit avant de compter pleinement. */
class MaturationTest {

    private static Game game() {
        Game game = new Game();
        game.start();
        game.state().setBigBangs(5);
        return game;
    }

    @Test
    void unAstreFormeMuritPuisCompte() {
        Game game = game();
        game.state().addBody("rocky_rubble");
        game.state().setMaturing("rocky_rubble", 120);
        assertTrue(game.hasBody("rocky_rubble"));
        assertFalse(game.isBodyMature("rocky_rubble"));
        assertEquals(0, game.bodiesMature());
        for (int i = 0; i < 119; i++) game.tick(1);
        assertTrue(game.isBodyMaturing("rocky_rubble"));
        assertTrue(game.takeMaturedBodies().isEmpty());
        game.tick(2);
        assertTrue(game.isBodyMature("rocky_rubble"));
        assertEquals(List.of("rocky_rubble"), game.takeMaturedBodies());
        assertEquals(1, game.bodiesMature());
    }

    @Test
    void lesDureesGrandissentAvecLEchelle() {
        Game game = game();
        double previous = 0;
        for (Body.Tier tier : Body.Tier.values()) {
            Body body = game.bodies().stream().filter(each -> each.tier() == tier).findFirst().orElseThrow();
            assertTrue(game.maturationSeconds(body) > previous, tier.name());
            previous = game.maturationSeconds(body);
        }
        assertEquals(2 * 3600, previous, 1e-9, "le trou noir supermassif : deux heures");
    }

    @Test
    void troisPlacesAuPlus() {
        Game game = game();
        for (String id : List.of("rocky_rubble", "icy_rubble", "metal_rubble")) {
            game.state().addBody(id);
            game.state().setMaturing(id, 600);
        }
        assertFalse(game.hasMaturationPlace());
        game.state().setMaturing("icy_rubble", 0);
        assertTrue(game.hasMaturationPlace());
    }

    @Test
    void uneImpulsionAvanceDUneMinute() {
        Game game = game();
        game.state().addBody("rocky_rubble");
        game.state().setMaturing("rocky_rubble", 90);
        assertFalse(game.hastenMaturation("rocky_rubble"), "sans charge, rien");
        game.state().setPulseCharges(2);
        assertTrue(game.hastenMaturation("rocky_rubble"));
        assertEquals(30, game.maturationLeft("rocky_rubble"), 1e-9);
        assertEquals(1, game.state().pulseCharges(), 1e-9);
        assertTrue(game.hastenMaturation("rocky_rubble"));
        assertTrue(game.isBodyMature("rocky_rubble"));
        assertEquals(List.of("rocky_rubble"), game.takeMaturedBodies());
        assertFalse(game.hastenMaturation("rocky_rubble"), "déjà mûr");
    }

    @Test
    void laGalaxieAttendQueToutSoitMur() {
        Game game = game();
        for (Body body : game.bodies()) game.state().addBody(body.id());
        int all = game.bodies().size();
        game.state().setMaturing("supermassive_black_hole", 10);
        assertEquals(all - 1, game.cosmosConditionsMet(Cosmos.GALAXY), "un astre qui mûrit ne compte pas encore");
        assertFalse(game.canFormGalaxy());
        game.state().setMaturing("supermassive_black_hole", 0);
        assertEquals(all, game.cosmosConditionsMet(Cosmos.GALAXY));
    }
}

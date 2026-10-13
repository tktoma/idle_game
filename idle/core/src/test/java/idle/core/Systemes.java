package idle.core;

import idle.core.sky.SolarSystem;
import java.util.List;

/** Pour les essais : trois systèmes solaires complets, faits d'astres posés d'office, pour que la galaxie ne demande plus que ses astres. */
final class Systemes {

    static void trois(Game game) {
        GameState state = game.state();
        for (String id : List.of("yellow_sun", "red_dwarf", "blue_giant", "rocky_planet", "ocean_planet", "lava_planet",
                "ice_planet", "gas_giant_planet", "ice_giant")) {
            state.addBody(id);
        }
        state.setSolarSystems(List.of(new SolarSystem("yellow_sun", List.of("rocky_planet", "ocean_planet"), List.of()),
                new SolarSystem("red_dwarf", List.of("lava_planet", "ice_planet"), List.of()),
                new SolarSystem("blue_giant", List.of("gas_giant_planet", "ice_giant"), List.of())));
    }

    private Systemes() {}
}

package idle.core;

import idle.core.sky.SolarSystem;
import java.util.List;

/**
 * Pour les essais : trois systèmes solaires complets, faits d'astres posés d'office, pour que la galaxie ne demande plus
 * que ses astres ; et une partie avec de quoi fonder des systèmes (deux étoiles, quatre planètes, deux lunes).
 */
final class Systemes {

    /** Le vrai jeu après ce nombre de Big Bangs, huit astres mûrs posés d'office : deux étoiles, quatre planètes, deux lunes. */
    static Game huitAstres(int bigBangs) {
        Game game = GameTestSupport.realGame();
        GameTestSupport.bangs(game, bigBangs);
        for (String id : List.of("yellow_sun", "blue_giant", "rocky_planet", "ocean_planet", "ice_planet", "gas_giant_planet",
                "icy_moon", "rocky_moon")) {
            game.state().addBody(id);
        }
        return game;
    }

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

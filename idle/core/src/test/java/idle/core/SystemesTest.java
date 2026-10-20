package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.bigbang.BigBangCondition;
import idle.core.bigbang.BigBangMilestones;
import idle.core.molecules.Molecule;
import idle.core.save.SaveCodec;
import idle.core.sky.Body;
import idle.core.sky.SolarSystem;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Les paliers six à huit (version 0.054) : systèmes solaires, places de plus, matières exotiques. */
class SystemesTest {

    private static Game game(int bigBangs) {
        Game game = new Game();
        game.start();
        game.state().setBigBangs(bigBangs);
        for (String id : List.of("yellow_sun", "blue_giant", "rocky_planet", "ocean_planet", "ice_planet", "gas_giant_planet",
                "icy_moon", "rocky_moon")) {
            game.state().addBody(id);
        }
        return game;
    }

    @Test
    void lesPaliersSixAHuit() {
        assertEquals(11, BigBangMilestones.DEFAULT.size());
        assertFalse(game(5).isSolarSystemsUnlocked());
        assertTrue(game(6).isSolarSystemsUnlocked());
        assertEquals(Game.MATURATION_PLACES, game(6).maturationPlaces());
        assertEquals(Game.MATURATION_PLACES + 2, game(7).maturationPlaces());
        assertEquals(2, game(7).probeSlots());
        assertFalse(game(7).isExoticUnlocked());
        assertTrue(game(8).isExoticUnlocked());
        assertTrue(game(5).isBangChallengesUnlocked(), "les défis de Big Bang s'ouvrent toujours au cinquième");
    }

    @Test
    void unSystemeSeFondeSeRempliEtSeDefait() {
        Game game = game(6);
        assertFalse(game.foundSystem("rocky_planet"), "une planète ne fonde pas de système");
        assertTrue(game.foundSystem("yellow_sun"));
        assertFalse(game.foundSystem("yellow_sun"), "une étoile n'a qu'un système");
        assertTrue(game.addPlanet(0, "ocean_planet"));
        assertFalse(game.addMoon(0, "icy_moon") && game.addMoon(0, "rocky_moon"), "une lune par planète au plus");
        assertFalse(game.isSystemComplete(game.state().solarSystems().get(0)));
        assertTrue(game.addPlanet(0, "rocky_planet"));
        assertTrue(game.isSystemComplete(game.state().solarSystems().get(0)));
        assertEquals(List.of("ocean_planet", "rocky_planet"), game.state().solarSystems().get(0).planets());
        assertTrue(game.movePlanet(0, "rocky_planet", -1));
        assertEquals(List.of("rocky_planet", "ocean_planet"), game.state().solarSystems().get(0).planets());
        assertFalse(game.addPlanet(0, "rocky_planet"), "déjà placée");
        assertTrue(game.removeFromSystem(0, "ocean_planet"));
        assertTrue(game.state().solarSystems().get(0).moons().size() <= 1);
        assertTrue(game.dissolveSystem(0));
        assertTrue(game.state().solarSystems().isEmpty());
        assertFalse(game.isInSystem("rocky_planet"));
    }

    @Test
    void lHarmonieRecompenseUnSystemeBienRange() {
        Game game = game(6);
        SolarSystem good = new SolarSystem("yellow_sun", List.of("rocky_planet", "ocean_planet", "gas_giant_planet"), List.of("icy_moon"));
        SolarSystem bad = new SolarSystem("yellow_sun", List.of("gas_giant_planet", "rocky_planet", "ocean_planet"), List.of());
        // Bien rangé : trois planètes à leur place, l'océan dans la zone habitable (+2), une lune : 6.
        assertEquals(6, game.systemHarmony(good));
        // Mal rangé : la géante devant, les rocheuses derrière elle ; l'océan reste en zone habitable.
        assertEquals(2, game.systemHarmony(bad));
        SolarSystem giant = new SolarSystem("blue_giant", List.of("ice_planet", "gas_giant_planet"), List.of());
        assertEquals(3, game.systemHarmony(giant), "deux planètes au loin, une étoile géante");
    }

    @Test
    void rangerAuMieuxDonneLeMeilleurOrdre() {
        Game game = game(6);
        SolarSystem bad = new SolarSystem("yellow_sun", List.of("gas_giant_planet", "ocean_planet", "rocky_planet"), List.of());
        assertFalse(game.isPlanetWellPlaced(bad, 0), "une gazeuse devant des telluriques");
        assertTrue(game.isPlanetWellPlaced(bad, 2) == false && game.isTelluric("rocky_planet"));
        SolarSystem best = game.bestArrangement(bad);
        assertEquals(List.of("rocky_planet", "ocean_planet", "gas_giant_planet"), best.planets());
        assertEquals(5, game.systemHarmony(best), "trois planètes à leur place, l'océan en zone habitable");
        for (int orbit = 0; orbit < 3; orbit++) assertTrue(game.isPlanetWellPlaced(best, orbit));
        assertTrue(Game.isHabitableOrbit(best, 1) && !Game.isHabitableOrbit(best, 0));
        game.foundSystem("yellow_sun");
        game.addPlanet(0, "gas_giant_planet");
        game.addPlanet(0, "ocean_planet");
        game.addPlanet(0, "rocky_planet");
        assertTrue(game.canArrangeSystem(0));
        assertTrue(game.arrangeSystem(0));
        assertEquals(best.planets(), game.state().solarSystems().get(0).planets());
        assertFalse(game.canArrangeSystem(0), "déjà au mieux");
        assertFalse(game.arrangeSystem(0));
    }

    @Test
    void unSystemeCompletAugmenteToutesLesGrandeurs() {
        Game game = game(6);
        double before = game.moleculeBoost(Molecule.Stat.ATOMS);
        game.foundSystem("blue_giant");
        game.addPlanet(0, "ice_planet");
        assertEquals(before, game.moleculeBoost(Molecule.Stat.ATOMS), 1e-9, "incomplet : rien");
        game.addPlanet(0, "gas_giant_planet");
        assertEquals(3 * Game.SYSTEM_GAIN, game.moleculeBoost(Molecule.Stat.ATOMS) - before, 1e-9);
    }

    @Test
    void lesSystemesSeSauvent() throws Exception {
        Game game = game(6);
        game.foundSystem("yellow_sun");
        game.addPlanet(0, "rocky_planet");
        game.addMoon(0, "icy_moon");
        GameState copy = new GameState();
        SaveCodec.readInto(copy, SaveCodec.write(game.state(), 0));
        assertEquals(game.state().solarSystems(), copy.solarSystems());
    }

    @Test
    void uneMatiereExotiqueAttendLeHuitiemeBigBang() {
        Game game = game(7);
        Body pulsar = game.body("pulsar");
        assertEquals(Body.Tier.EXOTIC, pulsar.tier());
        assertTrue(pulsar.assemblies().isEmpty());
        assertFalse(game.canFormBody("pulsar"));
    }

    @Test
    void lesBigBangsSixAHuitDemandentDesAstresMurs() {
        Game game = new Game();
        game.start();
        game.state().setBigBangs(4);
        assertEquals(0, game.bigBangSkyRequirement());
        assertFalse(game.bigBangConditions().contains(BigBangCondition.SKY));
        game.state().setBigBangs(5);
        assertEquals(18, game.bigBangSkyRequirement());
        assertTrue(game.bigBangConditions().contains(BigBangCondition.SKY));
        assertFalse(game.isBigBangConditionMet(BigBangCondition.SKY));
        for (Body body : game.bodies().subList(0, 18)) game.state().addBody(body.id());
        assertTrue(game.isBigBangConditionMet(BigBangCondition.SKY));
        game.state().setMaturing(game.bodies().get(0).id(), 60);
        assertFalse(game.isBigBangConditionMet(BigBangCondition.SKY), "un astre qui mûrit ne compte pas");
        game.state().setBigBangs(7);
        assertEquals(28, game.bigBangSkyRequirement());
        game.state().setBigBangs(8);
        assertEquals(0, game.bigBangSkyRequirement());
        // Jamais plus que les astres hors matières exotiques, qui attendent justement le huitième Big Bang.
        long ladder = game.bodies().stream().filter(b -> b.tier() != Body.Tier.EXOTIC).count();
        for (int need : Game.BIG_BANG_SKY) assertTrue(need <= ladder, need + " sur " + ladder);
    }
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.bigbang.BigBangMilestone;
import idle.core.bigbang.BigBangMilestones;
import idle.core.math.BigNum;
import org.junit.jupiter.api.Test;

/**
 * Version 0.124 : les paliers de Big Bang se paient, la vitesse s'essouffle au-delà de cent niveaux,
 * et les puissances du tableau, du ciel, du collisionneur et des succès élèvent la production.
 */
class PaliersPayantsTest {

    private static BigBangMilestone named(String name) {
        return BigBangMilestones.DEFAULT.stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void chaquePalierAUnPrixEnParticulesOuEnAtomes() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            assertTrue(milestone.hasPrice(), milestone.name());
            assertTrue(milestone.resource() == Resource.PARTICLES || milestone.resource() == Resource.ATOMS, milestone.name());
        }
    }

    @Test
    void unPalierOuvertNAgitQuUneFoisPaye() {
        Game game = new Game();
        game.start();
        BigBangMilestone first = named("Matière noire primordiale");
        assertFalse(game.isBigBangMilestoneOpen(first));
        game.state().setBigBangs(1);
        assertTrue(game.isBigBangMilestoneOpen(first));
        assertFalse(game.isBigBangMilestoneReached(first), "ouvert, pas encore payé");
        assertEquals(first, game.nextMilestoneToBuy());
        assertEquals(0, game.bigBangMilestonesReached());

        game.state().setParticles(first.price().divide(BigNum.of(10)));
        assertFalse(game.canBuyBigBangMilestone(first));
        assertFalse(game.buyBigBangMilestone(first));

        game.state().setParticles(first.price().multiply(BigNum.of(3)));
        assertTrue(game.buyBigBangMilestone(first));
        assertTrue(game.isBigBangMilestoneReached(first));
        assertEquals(2, game.state().particles().divide(first.price()).toDouble(), 1e-9, "le prix est dépensé");
        assertFalse(game.buyBigBangMilestone(first), "un palier ne se paie qu'une fois");
        assertNull(game.nextMilestoneToBuy());
    }

    @Test
    void lesSondesSePaientEnAtomes() {
        Game game = new Game();
        game.start();
        BigBangMilestone probes = named("Sondes spatiales");
        assertEquals(Resource.ATOMS, probes.resource());
        game.state().setBigBangs(3);
        game.state().setParticles(BigNum.of(1, 5000));
        assertFalse(game.canBuyBigBangMilestone(probes), "des particules ne paient pas un palier en atomes");
        game.state().setAtoms(probes.price());
        assertTrue(game.buyBigBangMilestone(probes));
        assertEquals(0, game.state().atoms().toDouble(), 0);
    }

    @Test
    void lesPaliersPayesTraversentLeBigBangMaisPasLUnivers() {
        Game game = new Game();
        game.start();
        game.state().setBigBangs(2);
        game.state().setParticles(BigNum.of(1, 1000));
        assertEquals(3, game.buyAllBigBangMilestones());
        assertTrue(game.isBigBangMilestoneReached(named("Création automatique")));
        try {
            Game copy = new Game();
            idle.core.save.SaveCodec.readInto(copy.state(), idle.core.save.SaveCodec.write(game.state(), 0));
            copy.restored();
            assertTrue(copy.isBigBangMilestoneReached(named("Expansion accélérée")), "la sauvegarde garde les paliers payés");
        } catch (Exception unreadable) {
            throw new AssertionError(unreadable);
        }
        game.state().reset();
        assertTrue(game.state().boughtMilestones().isEmpty(), "une nouvelle partie repart sans paliers");
    }

    @Test
    void laVitesseSEssouffleAuDelaDeCentNiveaux() {
        Game game = new Game();
        game.start();
        game.state().setLevel("speed", 50);
        double early = game.costOf("speed").log10();
        game.state().setLevel("speed", 51);
        double earlyStep = game.costOf("speed").log10() - early;
        game.state().setLevel("speed", 300);
        double late = game.costOf("speed").log10();
        game.state().setLevel("speed", 301);
        double lateStep = game.costOf("speed").log10() - late;
        // Au niveau 300, un niveau de plus coûte en plus 10^(1e-5 × (201² − 200²)) = 10^0,00401.
        double extra = Game.SPEED_SCALING * (201 * 201 - 200 * 200);
        assertEquals(earlyStep + extra, lateStep, 1e-6);
    }

    @Test
    void laMaturiteDuTableauSeRefaitAChaqueUnivers() {
        Game game = new Game();
        game.start();
        for (int i = 0; i < 150; i++) game.state().stats().endRun();
        assertEquals(0.5, game.tableMaturity(), 1e-12);
        game.state().setExplosionsAtUniverse(150);
        assertEquals(0, game.tableMaturity(), 0, "un nouvel univers repart des explosions qu'il fait lui-même");
        for (int i = 0; i < 600; i++) game.state().stats().endRun();
        assertEquals(1, game.tableMaturity(), 0);
    }

    @Test
    void lesPuissancesElevantLaProductionEtSeDetaillent() {
        boolean was = GameParticles.powersOff;
        GameParticles.powersOff = false;
        try {
            Game game = new Game();
            idle.core.save.SaveCodec.readInto(game.state(), idle.core.save.SaveCodec.write(SaveTest.richState(), 0));
            game.restored();
            double power = game.systemsPower();
            assertTrue(power > 0, "la partie riche a des astres, de l'énergie et des succès");
            assertEquals(game.tablePower() + game.skyPower() + game.colliderPower() + game.achievementPower(), power, 1e-12);
            assertTrue(game.tablePower() <= Game.POWER_TABLE && game.skyPower() <= Game.POWER_SKY
                    && game.colliderPower() <= Game.POWER_COLLIDER && game.achievementPower() <= Game.POWER_ACHIEVEMENTS);
            MultipliersTest.assertAddsUp(game);
            GameParticles.powersOff = true;
            assertEquals(0, game.systemsPower(), 0);
        } catch (Exception unreadable) {
            throw new AssertionError(unreadable);
        } finally {
            GameParticles.powersOff = was;
        }
    }
}

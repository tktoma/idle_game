package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.GluonMilestone;
import idle.core.collider.Hadron;
import idle.core.math.BigNum;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.151 : ce que font les hadrons. Chaque sorte renforce une grandeur ou baisse un prix, ×(1 + 0,25 × log₁₀(1 + n)) ;
 * chaque quark lié compte un gluon, et les paliers de gluons font garder à l'explosion et au Big Bang.
 */
class HadronsBonusTest {

    private static double boost(long count) {
        return 1 + Game.HADRON_BOOST_STEP * Math.log10(1 + (double) count);
    }

    @Test
    void leBonusGranditAvecLeLogarithme() {
        Game game = GameTestSupport.realGame();
        assertEquals(1, game.hadronBoost(Hadron.PROTON), 0, "sans proton, rien");
        game.state().setHadrons(Hadron.PROTON, 999);
        assertEquals(1.75, game.hadronBoost(Hadron.PROTON), 1e-9, "mille protons : ×1,75");
        for (Hadron hadron : Hadron.values()) assertFalse(hadron.bonus().isBlank(), hadron.toString());
        assertEquals(List.of(Hadron.J_PSI, Hadron.B_MESON, Hadron.UPSILON),
                java.util.Arrays.stream(Hadron.values()).filter(Hadron::lowersCost).toList());
    }

    @Test
    void chaqueSorteRenforceSaGrandeur() {
        Game game = GameTestSupport.universe(4);
        game.state().setBigBangs(1);
        game.state().setColliderGenerators(ColliderGenerator.BEAM, 5);
        double atoms = game.atomsPerFusion().log10(), dark = game.darkMatterPerExplosion().log10();
        double collider = game.colliderEnergyPerSecond(), space = game.steadySpacePerSecond().log10();
        double particles = game.particlesPerCreation().log10();
        double line = game.lineShardsPerTouch(), plane = game.planeValue(), volume = game.volumeValue(), duration = game.durationValue();
        double pair = game.voidPairGain();
        for (Hadron hadron : Hadron.values()) game.state().setHadrons(hadron, 99);
        double k = boost(99);
        assertEquals(atoms + Math.log10(k), game.atomsPerFusion().log10(), 1e-9, "protons : atomes");
        assertEquals(dark + Math.log10(k), game.darkMatterPerExplosion().log10(), 1e-9, "neutrons : matière noire");
        assertEquals(collider * k, game.colliderEnergyPerSecond(), collider * 1e-9, "pions : collisionneur");
        assertEquals(space + Math.log10(k), game.steadySpacePerSecond().log10(), 1e-9, "kaons : espace");
        assertEquals(particles + Math.log10(k), game.particlesPerCreation().log10(), 1e-6, "lambdas : particules");
        assertEquals(line * k, game.lineShardsPerTouch(), 1e-9, "omégas : la ligne");
        assertEquals(plane * k, game.planeValue(), 1e-9);
        assertEquals(volume * k, game.volumeValue(), 1e-9);
        assertEquals(duration * k, game.durationValue(), 1e-9);
        assertEquals(pair * k, game.voidPairGain(), 1e-9, "mésons D : le vide");
    }

    @Test
    void troisSortesBaissentDesPrix() {
        Game game = GameTestSupport.universe(4);
        double upgrade = game.colliderUpgradeCost(ColliderUpgrade.ENERGY);
        double generator = game.colliderGeneratorCost(ColliderGenerator.BEAM);
        double factor = game.generatorCostFactor();
        game.state().setHadrons(Hadron.J_PSI, 9_999);
        game.state().setHadrons(Hadron.UPSILON, 9_999);
        game.state().setHadrons(Hadron.B_MESON, 9_999);
        double k = boost(9_999);
        assertEquals(upgrade / k, game.colliderUpgradeCost(ColliderUpgrade.ENERGY), upgrade * 1e-9, "J/ψ");
        assertEquals(generator / k, game.colliderGeneratorCost(ColliderGenerator.BEAM), generator * 1e-9, "upsilons");
        assertEquals(factor / k, game.generatorCostFactor(), 1e-12, "mésons B");
    }

    @Test
    void lesGluonsComptentLesQuarksLies() {
        Game game = GameTestSupport.realGame();
        assertEquals(0, game.gluons());
        game.state().setHadrons(Hadron.PROTON, 10);
        game.state().setHadrons(Hadron.PION, 5);
        assertEquals(10 * 3 + 5 * 2, game.gluons());
        long previous = 0;
        for (GluonMilestone milestone : GluonMilestone.values()) {
            assertTrue(milestone.gluons() > previous, milestone + " : de plus en plus loin");
            previous = milestone.gluons();
        }
        assertFalse(game.isGluonMilestoneReached(GluonMilestone.UPGRADES));
        game.state().setHadrons(Hadron.PROTON, GluonMilestone.UPGRADES.gluons() / 3);
        assertTrue(game.isGluonMilestoneReached(GluonMilestone.UPGRADES));
    }

    @Test
    void lesPaliersDeGluonsFontGarderALExplosion() {
        Game game = GameTestSupport.realGame();
        GameTestSupport.fillTableToMax(game);
        game.state().setLevel("coupling", 4);
        game.state().addAutomation("auto_speed");
        game.state().setAutomationEnabled("auto_speed", true);
        game.state().setHadrons(Hadron.PROTON, GluonMilestone.AUTOMATIONS.gluons());
        assertTrue(game.explode());
        assertEquals(4, game.levelOf("coupling"), "les améliorations en particules restent");
        assertTrue(game.state().ownedAutomations().contains("auto_speed"), "les automatismes restent");
        Game plain = GameTestSupport.realGame();
        GameTestSupport.fillTableToMax(plain);
        plain.state().setLevel("coupling", 4);
        assertTrue(plain.explode());
        assertEquals(0, plain.levelOf("coupling"));
    }

    @Test
    void lesPaliersDeGluonsFontGarderAuBigBang() {
        Game game = GameTestSupport.realGame();
        assertFalse(game.bigBangKeepsDarkTree() || game.bigBangKeepsChallenges());
        game.state().setHadrons(Hadron.OMEGA, GluonMilestone.CHALLENGES.gluons());
        assertTrue(game.bigBangKeepsDarkTree() && game.bigBangKeepsChallenges());
    }

    @Test
    void lesMultiplicateursMontrentLesHadrons() {
        Game game = GameTestSupport.universe(4);
        game.state().stats().noteFusion(BigNum.ONE, 10);
        game.state().setExplosions(1);
        game.state().setDarkMatter(BigNum.of(5));
        game.state().setHadrons(Hadron.PROTON, 99);
        List<Multipliers.Factor> atoms = Multipliers.of(game, Multipliers.Quantity.ATOMS);
        assertTrue(atoms.stream().anyMatch(f -> f.aspect() == Multipliers.Aspect.HADRONS && f.label().contains("Proton")), atoms.toString());
        double sum = atoms.stream().mapToDouble(Multipliers.Factor::log10).sum();
        assertEquals(Multipliers.total(game, Multipliers.Quantity.ATOMS), sum, 1e-9, "les facteurs font le tout");
        game.state().setHadrons(Hadron.NEUTRON, 99);
        List<Multipliers.Factor> dark = Multipliers.of(game, Multipliers.Quantity.DARK_MATTER);
        assertEquals(Multipliers.total(game, Multipliers.Quantity.DARK_MATTER), dark.stream().mapToDouble(Multipliers.Factor::log10).sum(), 1e-9);
        assertTrue(dark.stream().anyMatch(f -> f.aspect() == Multipliers.Aspect.HADRONS));
    }
}

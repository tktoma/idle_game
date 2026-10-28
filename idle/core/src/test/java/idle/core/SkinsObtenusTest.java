package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.collider.ColliderUpgrade;
import idle.core.math.BigNum;
import idle.core.universe.Skin;
import idle.core.universe.Trial;
import org.junit.jupiter.api.Test;

/** Version 0.088 : comment s'obtiennent les skins — record, recherches, paliers et épreuves. */
class SkinsObtenusTest {

    /** Le deuxième univers, l'onglet Skins ouvert. */
    static Game skins() {
        Game game = DeuxiemeUniversTest.second();
        game.state().setSkinsOpen(true);
        assertTrue(game.isSkinsUnlocked());
        return game;
    }

    @Test
    void lesPaliersDeMatiereNoireEtDuBigBangDonnentLeursSkins() {
        Game game = skins();
        game.tick(0.1);
        assertFalse(game.hasSkin(Skin.DARK_OBSIDIAN));
        game.state().setDarkMatter(BigNum.of(1, 3));
        GameTestSupport.bangs(game, 1);
        game.tick(0.1);
        assertEquals(2, game.darkSkinTiers());
        assertTrue(game.hasSkin(Skin.DARK_OBSIDIAN) && game.hasSkin(Skin.DARK_VORTEX));
        assertFalse(game.hasSkin(Skin.DARK_NEBULA));
        assertTrue(game.hasSkin(Skin.BANG_AURORA) && !game.hasSkin(Skin.BANG_SUPERNOVA));
    }

    @Test
    void leRecordDeReconstructionEnMoinsDUneMinute() {
        Game game = skins();
        game.state().setLevel("generator", game.generatorsPerAtom() - 1);
        game.tick(0.01);
        assertTrue(game.fuse());
        assertTrue(game.state().rebuildPending());
        game.tick(30);
        assertFalse(game.hasSkin(Skin.FUSION_FLASH), "pas encore dix générateurs");
        game.state().setLevel("generator", game.generatorsPerAtom() - 1);
        game.tick(0.01);
        assertTrue(game.hasSkin(Skin.FUSION_FLASH));
        assertTrue(game.rebuildRecord() > 29 && game.rebuildRecord() < 31, "record : " + game.rebuildRecord());
    }

    @Test
    void lesDeuxRecherches() {
        Game game = skins();
        assertFalse(game.canBuyPrismResearch());
        game.state().setParticles(Game.PRISM_COST.multiply(2));
        assertTrue(game.buyPrismResearch());
        assertFalse(game.buyPrismResearch(), "une seule fois");
        assertTrue(game.hasSkin(Skin.FUSION_PRISM));
        // Le noyau dense : un million d'atomes créés, puis quatre paliers payés en atomes.
        assertFalse(game.isDenseResearchUnlocked());
        game.state().stats().noteFusion(Game.DENSE_UNLOCK_ATOMS, 1);
        assertTrue(game.isDenseResearchUnlocked());
        game.state().setAtoms(BigNum.of(1, 9));
        for (int tier = 0; tier < 4; tier++) assertTrue(game.buyDenseTier(), "palier " + (tier + 1));
        assertFalse(game.buyDenseTier());
        assertEquals(4, game.denseTiers());
        assertTrue(game.hasSkin(Skin.ATOM_DENSE) && game.hasSkin(Skin.TABLE_LEAD)
                && game.hasSkin(Skin.ATOM_STELLAR) && game.hasSkin(Skin.TABLE_GOLD));
    }

    @Test
    void lEpreuveDuPremierGenerateurSeul() {
        Game game = skins();
        assertFalse(game.isTrialUnlocked(Trial.LONE_GENERATOR), "avant une explosion");
        game.state().setExplosions(1);
        // Avec deux générateurs, elle ne commence pas : il faut n'avoir que le premier.
        game.state().setLevel("generator", 1);
        assertEquals(2, game.generatorCount());
        assertTrue(game.isTrialUnlocked(Trial.LONE_GENERATOR));
        assertFalse(game.canStartTrial(Trial.LONE_GENERATOR), "deux générateurs : refusée");
        assertFalse(game.startTrial(Trial.LONE_GENERATOR));
        game.state().setLevel("generator", 0);
        assertEquals(1, game.generatorCount());
        assertEquals(0, game.trialLeft(), 0, "sans épreuve, rien à attendre");
        assertEquals(0, game.trialProgress(), 0);
        assertTrue(game.canStartTrial(Trial.LONE_GENERATOR));
        assertTrue(game.startTrial(Trial.LONE_GENERATOR));
        assertFalse(game.startTrial(Trial.COLD_BANG), "une à la fois");
        assertEquals(Double.POSITIVE_INFINITY, game.trialLeft(), "sans limite de temps");
        game.state().setParticles(BigNum.of(1, 40));
        assertFalse(game.canBuy("generator"), "aucun autre générateur");
        game.state().setParticles(BigNum.of(1, 6));
        assertEquals(0.5, game.trialProgress(), 1e-12, "1e6 particules : la moitié des douze ordres de grandeur");
        game.state().setParticles(BigNum.of(1, 12));
        game.tick(0.01);
        assertNull(game.activeTrial());
        assertTrue(game.isTrialCompleted(Trial.LONE_GENERATOR) && game.hasSkin(Skin.FUSION_LONE));
    }

    @Test
    void lAutomatismeDesGenerateursRespecteLEpreuveDuGenerateurSeul() {
        Game game = skins(), witness = skins();
        for (Game each : java.util.List.of(game, witness)) {
            each.state().setExplosions(1);
            each.state().addAutomation("auto_generator");
            each.state().setAutomationEnabled("auto_generator", true);
            each.state().setParticles(BigNum.of(1, 9));
        }
        assertTrue(game.startTrial(Trial.LONE_GENERATOR));
        double wait = game.automationInterval("auto_generator") + 1;
        game.tick(wait);
        witness.tick(wait);
        assertTrue(witness.generatorCount() > 1, "sans l'épreuve, l'automatisme achète bien des générateurs");
        assertEquals(1, game.generatorCount(), "aucun générateur acheté pendant l'épreuve, même par l'automatisme");
        assertEquals(Trial.LONE_GENERATOR, game.activeTrial(), "l'épreuve continue");
    }

    @Test
    void laContractionDouceEtLeBigBangAFroid() {
        Game game = skins();
        game.state().setExplosions(1);
        assertTrue(game.startTrial(Trial.SOFT_CONTRACTION));
        game.state().setParticles(BigNum.of(1000));
        game.state().setLevel("generator", 0);
        double before = game.state().particles().toDouble();
        game.tick(10);
        assertTrue(game.state().particles().toDouble() < before * 1.5, "les particules fondent");
        game.state().setTrialCount(30);
        assertEquals(Trial.CONTRACTION_MAX, game.contractionAtomsMultiplier(), 1e-12, "×2 au plus");
        assertTrue(game.abandonTrial());
        assertFalse(game.isTrialCompleted(Trial.SOFT_CONTRACTION));
        // Le Big Bang à froid : l'expansion divisée par deux.
        GameTestSupport.bangs(game, 1);
        double space = game.steadySpacePerSecond().toDouble();
        assertTrue(game.startTrial(Trial.COLD_BANG));
        assertEquals(space * Trial.COLD_EXPANSION, game.steadySpacePerSecond().toDouble(), space * 1e-9);
    }

    @Test
    void lesEpreuvesDuCollisionneur() {
        Game game = skins();
        game.state().setExplosions(1);
        assertTrue(game.isColliderUnlocked());
        assertFalse(game.isTrialUnlocked(Trial.DETECTOR), "le détecteur élargi au niveau 3");
        game.state().setColliderUpgradeLevel(ColliderUpgrade.WINDOW, 3);
        assertTrue(game.isTrialUnlocked(Trial.DETECTOR));
        assertTrue(game.startTrial(Trial.NO_OVERHEAT));
        game.state().setOverheatLeft(5);
        game.tick(0.1);
        assertNull(game.activeTrial(), "la surchauffe la fait échouer");
        assertTrue(game.startTrial(Trial.LONE_COLLIDER));
        game.state().setTrialCount(2);
        assertEquals(0.25, game.trialProgress(), 1e-12, "deux croisements captés sur huit");
        game.state().setTrialCount(Trial.LONE_COLLIDER.goal());
        game.tick(0.1);
        assertTrue(game.hasSkin(Skin.COLLIDER_ICE));
        // Le temps écoulé fait échouer une épreuve limitée.
        assertTrue(game.startTrial(Trial.DETECTOR));
        assertEquals(60, game.trialLeft(), 1e-9, "soixante secondes");
        game.state().setTimePlayed(game.state().timePlayed() + 20);
        assertEquals(40, game.trialLeft(), 1e-6, "vingt secondes plus tard, quarante restent");
        game.tick(Trial.DETECTOR.seconds() + 1);
        assertNull(game.activeTrial());
        assertFalse(game.isTrialCompleted(Trial.DETECTOR));
    }

    @Test
    void lesEpreuvesEtLesRecherchesTraversentLUnivers() {
        Game game = skins();
        game.state().setParticles(Game.PRISM_COST);
        game.buyPrismResearch();
        game.state().addCompletedTrial(Trial.COLD_BANG.name());
        game.state().setRebuildRecord(42);
        game.testFormUniverse();
        assertTrue(game.hasPrismResearch());
        assertTrue(game.isTrialCompleted(Trial.COLD_BANG));
        assertEquals(42, game.rebuildRecord(), 0);
        assertNull(game.activeTrial());
    }
}

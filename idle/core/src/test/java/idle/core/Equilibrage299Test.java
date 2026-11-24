package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.save.SaveCodec;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.core.state.GameState;
import idle.core.upgrades.Upgrade;
import idle.core.upgrades.Upgrades;
import org.junit.jupiter.api.Test;

/**
 * Version 0.299, l'équilibrage de la partie 4 : le couplage plafonné, la matière du cosmos
 * baissée, le palier du mélange au défi final, et la bulle du rassemblement accéléré.
 */
class Equilibrage299Test {
    /** Une partie où la galaxie vient d'être formée : l'amas de galaxies attend son délai. */
    private static Game galaxyJustFormed(boolean bubble) {
        Game game = GameTestSupport.universe(8);
        GameTestSupport.bangs(game, 1);
        game.state().setSpace(idle.core.math.BigNum.of(1, 12));
        GameTestSupport.openSpace(game);
        for (Body body : game.bodies()) game.state().addBody(body.id());
        Systemes.trois(game);
        assertTrue(game.formGalaxy());
        if (bubble) game.state().setTreeBubbleBought(Game.TreeBubble.GATHER.ordinal(), true);
        return game;
    }

    @Test
    void leCouplageSArreteAMilleNiveaux() {
        Game game = GameTestSupport.realGame();
        Upgrade coupling = game.upgrade("coupling");
        assertEquals(Upgrades.COUPLING_MAX_LEVEL, coupling.maxLevel());
        assertEquals(1000, Upgrades.COUPLING_MAX_LEVEL);
        // Une ancienne sauvegarde au-delà du plafond : le bonus est celui du plafond.
        game.state().setLevel("coupling", Upgrades.COUPLING_MAX_LEVEL);
        double atCap = game.particlesMultiplier(coupling, Upgrades.COUPLING_MAX_LEVEL, 10).toDouble();
        assertEquals(1 + 0.02 * 1000 * 9, atCap, 1e-9);
        assertEquals(atCap, game.particlesMultiplier(coupling, 130_000_000, 10).toDouble(), 1e-9);
        assertTrue(game.isMaxed("coupling"));
    }

    @Test
    void laMatiereDuCosmosBaisseDeTrenteCinqPourCent() {
        assertEquals(39_000_000, Cosmos.CLUSTER.matter().values().stream().mapToInt(Integer::intValue).max().getAsInt());
        assertEquals(156_000_000, Cosmos.UNIVERSE.matter().values().stream().mapToInt(Integer::intValue).max().getAsInt());
        assertEquals(3e6, Game.finalGoal(10), 0);
    }

    @Test
    void tenirLeClicAccelereLeRassemblement() {
        Game game = galaxyJustFormed(true);
        assertEquals(Cosmos.CLUSTER, game.settlingCosmos());
        double before = game.cosmosSettleRemaining(Cosmos.CLUSTER);
        assertTrue(before > 0);
        assertTrue(game.canHoldSettle());
        // Dix secondes tenues : trente secondes de délai en plus du temps (qui ne passe pas ici).
        assertEquals(30, game.holdSettle(10), 1e-9);
        assertEquals(before - 30, game.cosmosSettleRemaining(Cosmos.CLUSTER), 1e-9);
        // Le reste d'un coup : on ne gagne pas plus que ce qui reste, et le délai est fini.
        assertEquals(before - 30, game.holdSettle(1e9), 1e-6);
        assertEquals(0, game.cosmosSettleRemaining(Cosmos.CLUSTER), 1e-9);
        assertNull(game.settlingCosmos());
        assertFalse(game.canHoldSettle());
        assertEquals(0, game.holdSettle(10), 0);
    }

    @Test
    void sansLaBulleTenirNeFaitRien() {
        Game game = galaxyJustFormed(false);
        assertFalse(game.canHoldSettle());
        double before = game.cosmosSettleRemaining(Cosmos.CLUSTER);
        assertEquals(0, game.holdSettle(60), 0);
        assertEquals(before, game.cosmosSettleRemaining(Cosmos.CLUSTER), 0);
    }

    @Test
    void leRassemblementTenuSeSauvegardeEtRepartAZeroALEchelleSuivante() throws Exception {
        Game game = galaxyJustFormed(true);
        game.holdSettle(100);
        String save = SaveCodec.write(game.state(), 0);
        GameState back = SaveCodec.read(save).state();
        assertEquals(300, back.settleHeld(), 1e-9);
        // L'amas formé (matière, sortes et délai réunis), le compte repart de zéro pour l'univers.
        game.holdSettle(1e9);
        for (var need : game.cosmosMatter(Cosmos.CLUSTER).entrySet()) {
            var molecule = game.molecules().stream().filter(m -> m.state() == need.getKey()).findFirst().orElseThrow();
            game.state().addSubstance(molecule.id());
            game.state().setMoleculeCount(molecule.id(), need.getValue());
        }
        for (var need : Cosmos.CLUSTER.sorts().entrySet()) {
            game.molecules().stream().filter(m -> m.state() == need.getKey()).limit(need.getValue())
                    .forEach(m -> game.state().addSubstance(m.id()));
        }
        assertTrue(game.formCosmos(Cosmos.CLUSTER));
        assertEquals(0, game.state().settleHeld(), 0);
    }

    @Test
    void laBulleNaitALArbreMurEtSOuvreParLArbusteDuMilieu() {
        assertEquals(8, Game.TreeBubble.GATHER.stage());
        assertEquals(java.util.List.of(Game.TreeBubble.CROWN_MIDDLE), Game.TreeBubble.GATHER.below());
        assertEquals(Game.TreeBubble.values().length - 1, Game.TreeBubble.GATHER.ordinal(), "ajoutée à la fin : les sauvegardes gardent leurs bits");
    }
}

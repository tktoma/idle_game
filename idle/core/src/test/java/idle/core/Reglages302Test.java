package idle.core;

import idle.core.collider.ColliderUpgrade;
import idle.core.sky.Cosmos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Version 0.302 : la chaîne du collisionneur multiplie (particules ×10, atomes ×1.3, matière noire ×1.3, espace ×1.5
 * par niveau), le rassemblement de l'amas et de l'univers a un délai de base par univers, et le vide le raccourcit
 * de 70 % au plus en sept niveaux.
 */
class Reglages302Test {
    @Test
    void laChaineMultiplieNiveauParNiveau() {
        Game game = GameTestSupport.realGame();
        for (int k = 0; k <= 10; k++) {
            game.state().setColliderUpgradeLevel(ColliderUpgrade.PARTICLES, k);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.ATOMS, k);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.DARK_MATTER, k);
            game.state().setColliderUpgradeLevel(ColliderUpgrade.SPACE, Math.min(k, 4));
            assertEquals(Math.pow(10, k), game.colliderParticlesMultiplier(), Math.pow(10, k) * 1e-12, "particules, niveau " + k);
            assertEquals(Math.pow(1.3, k), game.colliderAtomsMultiplier(), 1e-9, "atomes, niveau " + k);
            assertEquals(Math.pow(1.3, k), game.colliderDarkGrowthMultiplier(), 1e-9, "matière noire, niveau " + k);
            assertEquals(Math.pow(1.5, Math.min(k, 4)), game.colliderSpaceMultiplier(), 1e-9, "espace, niveau " + k);
        }
    }

    @Test
    void auNiveauZeroLaChaineNeChangeRien() {
        Game game = GameTestSupport.realGame();
        assertEquals(1, game.colliderParticlesMultiplier(), 0);
        assertEquals(1, game.colliderAtomsMultiplier(), 0);
        assertEquals(1, game.colliderDarkGrowthMultiplier(), 0);
        assertEquals(1, game.colliderSpaceMultiplier(), 0);
    }

    @Test
    void leDelaiDeBaseSuitLUnivers() {
        double[] attendu = {123, 158, 160, 117, 110, 115, 165, 173, 183, 165, 145};
        assertEquals(attendu.length, GameUniverseBase.SETTLE_MINUTES.length, "un délai par univers, défi final compris");
        Game premier = GameTestSupport.realGame();
        assertEquals(attendu[0] * 60, premier.cosmosSettleSeconds(), 1e-9, "premier univers");
        for (int formes = 1; formes <= 13; formes++) {
            Game game = GameTestSupport.realGame();
            game.state().setUniverseFormed(true);
            game.state().setExtraUniverses(formes - 1);
            assertEquals(formes, game.universes());
            assertEquals(attendu[Math.min(formes, attendu.length - 1)] * 60, game.cosmosSettleSeconds(), 1e-9, formes + " univers formés");
        }
    }

    @Test
    void chaqueDelaiResteEntreUneHeureEtTroisHeuresVingt() {
        for (double minutes : GameUniverseBase.SETTLE_MINUTES) assertTrue(minutes >= 60 && minutes <= 200, minutes + " min");
    }

    @Test
    void leVideRaccourcitLeDelaiDeLUniversEnCours() {
        Game game = GameTestSupport.realGame();
        game.state().setUniverseFormed(true);
        game.state().setExtraUniverses(9);
        game.state().setVoidLevel(Game.VoidUpgrade.SETTLE.ordinal(), Game.VoidUpgrade.SETTLE.max());
        assertEquals(145 * 60 * 0.3, game.cosmosSettleSeconds() * game.settleShare(), 1e-6, "dix univers formés : le onzième, vide au plafond");
    }

    @Test
    void leRassemblementDuVideVaJusquaSeptNiveaux() {
        Game.VoidUpgrade settle = Game.VoidUpgrade.SETTLE;
        assertEquals(7, settle.max());
        assertEquals(500, settle.cost(0), 1e-6, "le premier niveau ne change pas");
        assertEquals(20_000_000, settle.cost(6), 1, "le dernier niveau");
        assertTrue(Double.isInfinite(settle.cost(7)), "plus rien au-delà");
        for (int k = 1; k < 7; k++) assertTrue(settle.cost(k) > settle.cost(k - 1), "prix croissant, niveau " + k);
    }

    @Test
    void leDelaiCourtDepuisLaGalaxieDeCetUnivers() {
        Game game = GameTestSupport.realGame();
        assertEquals(0, game.cosmosSettleRemaining(Cosmos.GALAXY), 0, "la galaxie n'attend rien");
    }
}

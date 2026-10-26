package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.save.SaveCodec;
import org.junit.jupiter.api.Test;

/**
 * Version 0.137 : l'arrosoir remplace le bouton de la graine. Il se débloque sur son étagère ; penché sur la graine,
 * il verse un arrosage par seconde, le premier la fait germer ; relâché, il ne verse plus.
 */
class ArrosoirTest {

    /** Le premier univers formé, l'objectif de la dimension 1 atteint : la graine peut être arrosée. */
    private static Game ready() {
        Game game = new Game();
        game.start();
        DeuxiemeUniversTest.formUniverse(game);
        game.state().setDimensionShardsEarned(Game.LINE_THRESHOLD);
        assertTrue(game.canTendSeed());
        return game;
    }

    @Test
    void lArrosoirSeDebloqueUneFoisLUniversForme() {
        Game fresh = new Game();
        fresh.start();
        assertFalse(fresh.canUnlockWateringCan(), "pas d'univers, pas de graine, pas d'arrosoir");
        assertFalse(fresh.unlockWateringCan());
        Game game = ready();
        assertFalse(game.isWateringCanUnlocked());
        assertTrue(game.canUnlockWateringCan());
        assertTrue(game.unlockWateringCan());
        assertTrue(game.isWateringCanUnlocked());
        assertFalse(game.canUnlockWateringCan(), "une fois suffit");
        assertFalse(game.unlockWateringCan());
    }

    @Test
    void sansArrosoirRienNeCoule() {
        Game game = ready();
        game.setWatering(true);
        assertFalse(game.isWatering(), "l'arrosoir n'est pas débloqué");
        game.tick(5);
        assertFalse(game.state().seedSprouted());
    }

    @Test
    void penchePendantTroisSecondesEtDemieIlFaitGermerPuisArroseDeuxFois() {
        Game game = ready();
        assertTrue(game.unlockWateringCan());
        game.setWatering(true);
        game.tick(0.5);
        assertFalse(game.state().seedSprouted(), "une demi-seconde : pas encore");
        assertEquals(0.5, game.wateringPoured(), 1e-9);
        game.tick(0.5);
        assertTrue(game.state().seedSprouted(), "la première seconde fait germer la graine");
        assertEquals(0, game.state().seedWaterings());
        game.tick(2.5);
        assertEquals(2, game.state().seedWaterings(), Game.WATERING_CAN_PER_SECOND + " arrosage par seconde");
        game.setWatering(false);
        assertEquals(0, game.wateringPoured(), "relâché, ce qui était à moitié versé se perd");
        game.tick(10);
        assertEquals(2, game.state().seedWaterings(), "relâché, il ne verse plus");
    }

    @Test
    void uneLongueImageVerseToutCeQuElleDoitEtUneAbsenceLeRelache() {
        Game game = ready();
        assertTrue(game.unlockWateringCan());
        game.setWatering(true);
        game.tick(5);
        assertTrue(game.state().seedSprouted());
        assertEquals(4, game.state().seedWaterings(), "cinq secondes d'un coup : la germination et quatre arrosages");
        game.tickAway(60);
        assertFalse(game.isWatering(), "une absence repose l'arrosoir");
        assertEquals(4, game.state().seedWaterings());
    }

    @Test
    void laGraineQuiAttendUnSeuilNeBoitPas() {
        Game game = ready();
        game.state().setDimensionShardsEarned(0);
        assertFalse(game.canTendSeed());
        assertTrue(game.unlockWateringCan());
        game.setWatering(true);
        game.tick(10);
        assertFalse(game.state().seedSprouted(), "l'objectif de la dimension 1 manque");
        assertEquals(0, game.wateringPoured(), "rien ne s'accumule en attendant");
    }

    @Test
    void lArrosoirSeSauvegardeEtTraverseLesUnivers() throws Exception {
        Game game = ready();
        assertTrue(game.unlockWateringCan());
        GameState copy = SaveCodec.read(SaveCodec.write(game.state(), 1)).state();
        assertTrue(copy.wateringCan());
        GameState old = SaveCodec.read(SaveCodec.write(new GameState(), 1)).state();
        assertFalse(old.wateringCan(), "une partie sans arrosoir le garde verrouillé");
        DeuxiemeUniversTest.formUniverse(game);
        assertTrue(game.isWateringCanUnlocked(), "former l'univers suivant ne le reprend pas");
        game.testResetUniverses();
        assertFalse(game.isWateringCanUnlocked(), "l'outil d'essai qui remet la partie secondaire à zéro le reprend");
    }
}

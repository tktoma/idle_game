import static org.junit.jupiter.api.Assertions.*;

import idle.core.Game;
import idle.core.universe.Trial;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Version 0.294 : les mécaniques que les bots ne jouaient pas (le calme du Big Bang, les épreuves, le rond de croissance)
 * sont jouées par tous les profils sauf le Speedrunner, la référence de l'équilibrage, qui ne change pas.
 */
class MecaniquesAjouteesTest {
    private static final Path UNIVERSE1 = Path.of("sauvegardes/reference/checkpoint_universe1_speedrunner.save");

    private static IdleProfiles24 bot(int id) throws Exception {
        Game game = Bots.load(UNIVERSE1, 42);
        return new IdleProfiles24(game, IdleProfiles24.PROFILES[id - 1], 42, 20);
    }

    @Test
    void leSpeedrunnerResteLaReference() throws Exception {
        IdleProfiles24 speedrunner = bot(1);
        assertTrue(speedrunner.reference());
        speedrunner.extras();
        assertFalse(speedrunner.calm, "la référence n'attend jamais le calme");
        assertNull(speedrunner.g.activeTrial(), "ni ne lance d'épreuve");
        assertEquals(0, speedrunner.trialsStarted + speedrunner.bloomsCaught + speedrunner.calmWaits);
        for (int id = 2; id <= IdleProfiles24.PROFILES.length; id++) assertFalse(bot(id).reference(), "profil " + id);
    }

    @Test
    void uneEpreuveSeTenteUneFoisParUnivers() throws Exception {
        // Le Vétéran a toute son attention aux à-côtés : la première épreuve qui peut commencer commence.
        IdleProfiles24 veteran = bot(2);
        Trial startable = null;
        for (Trial trial : Trial.values()) if (veteran.g.canStartTrial(trial)) { startable = trial; break; }
        veteran.trials();
        if (startable == null) {
            assertNull(veteran.g.activeTrial(), "aucune épreuve ne peut commencer : rien ne se lance");
            return;
        }
        assertEquals(startable, veteran.g.activeTrial());
        assertEquals(1, veteran.trialsStarted);
        // Abandonnée, elle ne se retente pas dans le même univers.
        veteran.g.abandonTrial();
        veteran.trials();
        assertNotEquals(startable, veteran.g.activeTrial());
    }

    @Test
    void sansLaFuiteRienNAttendLeCalme() throws Exception {
        IdleProfiles24 regular = bot(3);
        if (!regular.g.state().completedChallenges().contains(idle.core.darkmatter.Challenges.LEAK.id())) {
            assertFalse(regular.calmWanted(), "sans la récompense de la Fuite, aucun calme à attendre");
        }
        regular.sansMecaniques.add("calme");
        assertFalse(regular.calmWanted(), "coupé pour la mesure");
    }
}

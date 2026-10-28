import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;

import org.junit.jupiter.api.Test;

/**
 * Version 0.136 : un bot par dimension, et les seuils réglés pour qu'un joueur appliqué mette environ un quart d'heure
 * à mettre le rond vert sur chacune. Garde-fou de l'équilibrage : entre 12 et 18 minutes pour chaque dimension.
 */
class DimensionBotsTest {

    private static void quarterOfAnHour(String dimension) throws Exception {
        DimensionBots.Result result = DimensionBots.measure(dimension, 60 * 60);
        assertTrue(result.reached(), dimension + " : seuil pas atteint en une heure");
        double minutes = result.seconds() / 60;
        assertTrue(minutes >= 12 && minutes <= 18, dimension + " : " + result.minutes() + " au lieu d'environ 15");
        assertTrue(result.purchases().size() >= 10, dimension + " : le bot achète ses améliorations");
    }

    @Test
    void laLigne() throws Exception {
        quarterOfAnHour("ligne");
    }

    @Test
    void lePlan() throws Exception {
        quarterOfAnHour("plan");
    }

    @Test
    void leVolume() throws Exception {
        quarterOfAnHour("volume");
    }

    @Test
    void laDuree() throws Exception {
        quarterOfAnHour("duree");
    }

    /**
     * Version 0.140 : le joueur appliqué joue aussi une partie déjà en cours, pas à pas (les outils « Bots dimension » du
     * profil de test). Les quatre dimensions ouvertes, chacun gagne des éclats en deux minutes et achète.
     */
    @Test
    void leJoueurJoueUnePartieEnCoursPasAPas() throws Exception {
        Game game = DimensionBots.open(4);
        for (String name : java.util.List.of("ligne", "plan", "volume", "duree")) {
            DimensionBots.Player player = DimensionBots.player(game, name);
            double before = player.earned();
            for (int step = 0; step < 2400; step++) {     // deux minutes, en pas de 0,05 s
                assertTrue(player.decide(DimensionBots.STEP), name + " : la dimension est ouverte");
                game.tick(DimensionBots.STEP);
            }
            assertTrue(player.earned() > before, name + " : des éclats gagnés");
            assertFalse(player.purchases().isEmpty(), name + " : des achats");
            assertEquals(120, player.t, 1e-6);
        }
    }

    /** Une dimension fermée : le joueur n'y fait rien et le dit. */
    @Test
    void leJoueurNeJouePasUneDimensionFermee() throws Exception {
        Game game = DimensionBots.open(1);
        DimensionBots.Player plane = DimensionBots.player(game, "plan");
        assertFalse(plane.isOpen());
        assertFalse(plane.decide(DimensionBots.STEP));
        assertTrue(DimensionBots.player(game, "ligne").decide(DimensionBots.STEP));
    }
}

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Version 0.136 : un bot par dimension, et les seuils réglés pour qu'un joueur appliqué mette environ un quart d'heure
 * à mettre le rond vert sur chacune. Garde-fou de l'équilibrage : entre 12 et 18 minutes pour chaque dimension.
 */
class DimensionBotsTest {

    private static void quarterOfAnHour(String dimension) throws Exception {
        DimensionBots.Result result = DimensionBots.bot(dimension).run(60 * 60);
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
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameUniverse.PlaneUpgrade;
import org.junit.jupiter.api.Test;

/**
 * Le plan, deuxième dimension (version 0.104, ouverte avec le deuxième univers) : un point qu'on dirige au clavier
 * sur un carré, des éclats posés qu'on ramasse en passant dessus, chacun donnant des facettes ; cinq améliorations
 * (valeur, apparition, nombre, vitesse, taille) avec leurs prix ; le seuil des 3 500 facettes gagnées (version 0.136).
 */
class PlanTest {

    /** Une partie au deuxième univers formé : le plan est ouvert. */
    private static Game plane() {
        return GameTestSupport.universe(2);
    }

    @Test
    void lePlanSeJoueAuClavierEtRamasseSesEclats() {
        Game game = GameTestSupport.universe(1);
        assertFalse(game.isPlaneOpen(), "un seul univers : la deuxième dimension est fermée");
        game.testAddUniverse();
        assertTrue(game.isPlaneOpen());
        game.tick(Game.PLANE_SPAWN_SECONDS * 10);
        assertEquals(3, game.planeShardsOnBoard().size(), "trois éclats au plus");
        // Le point va à droite, et ne sort pas du plan.
        game.setPlanePosition(0, 0);
        game.setPlaneInput(1, 0);
        for (int k = 0; k < 200; k++) game.tick(0.1);
        assertEquals(1 - 0.025, game.planeX(), 1e-9, "arrêté au bord, à son rayon");
        assertEquals(0, game.planeY(), 1e-9);
        game.setPlaneInput(0, 0);
        // Passer sur un éclat le ramasse : un éclat du plan, propre à cette dimension.
        double line = game.state().dimensionShards();
        game.state().setPlaneShards(0);
        int before = game.planeShardsOnBoard().size();
        long collected = game.planeCollected();
        Game.PlaneShard shard = game.planeShardsOnBoard().get(0);
        game.setPlanePosition(shard.x(), shard.y());
        game.tick(0.01);
        assertEquals(1, game.state().planeShards(), 1e-9, "un éclat ramassé : une facette");
        assertEquals(1, game.state().planeShardsEarned(), 1e-9);
        assertEquals(before - 1, game.planeShardsOnBoard().size());
        assertEquals(collected + 1, game.planeCollected());
        assertEquals(line, game.state().dimensionShards(), 1e-6, "les segments de la ligne ne bougent pas");
    }

    @Test
    void lesEclatsApparaissentUnParTroisSecondesJusquATrois() {
        Game game = plane();
        assertEquals(0, game.planeShardsOnBoard().size(), "au départ, le plan est nu");
        game.tick(Game.PLANE_SPAWN_SECONDS - 0.01);
        assertEquals(0, game.planeShardsOnBoard().size(), "pas encore trois secondes");
        game.tick(0.02);
        assertEquals(1, game.planeShardsOnBoard().size(), "le premier à 3 s");
        game.tick(Game.PLANE_SPAWN_SECONDS * 5);
        assertEquals(3, game.planeShardsOnBoard().size(), "jamais plus de trois");
        for (Game.PlaneShard shard : game.planeShardsOnBoard()) {
            assertTrue(Math.abs(shard.x()) <= 1 - 0.06 && Math.abs(shard.y()) <= 1 - 0.06, "posé dans le plan, à deux rayons du bord");
        }
    }

    @Test
    void lePointAvanceASaVitesseEtSePartageEnDiagonale() {
        Game game = plane();
        game.setPlanePosition(0, 0);
        game.setPlaneInput(0, -1);
        for (int k = 0; k < 5; k++) game.tick(0.1);
        assertEquals(-0.3, game.planeY(), 1e-9, "0,6 par seconde vers le haut");
        assertEquals(0, game.planeX(), 1e-9);
        game.setPlaneInput(1, 1);
        for (int k = 0; k < 5; k++) game.tick(0.1);
        assertEquals(0.3 / Math.sqrt(2), game.planeX(), 1e-9, "en diagonale, la vitesse se partage");
        assertEquals(-0.3 + 0.3 / Math.sqrt(2), game.planeY(), 1e-9);
        game.setPlaneInput(0, 0);
        game.tick(10);
        assertEquals(0.3 / Math.sqrt(2), game.planeX(), 1e-9, "sans direction, le point reste");
        // Un gros pas ne fait pas sauter le point plus loin qu'un quart de seconde de course.
        game.setPlanePosition(0, 0);
        game.setPlaneInput(1, 0);
        game.tick(100);
        assertEquals(0.15, game.planeX(), 1e-9, "une image de 100 s ne vaut que 0,25 s de course");
    }

    @Test
    void lesCinqAmeliorationsDuPlan() {
        Game game = plane();
        game.state().setPlaneShards(1e6);
        for (PlaneUpgrade upgrade : PlaneUpgrade.values()) assertTrue(game.buyPlane(upgrade), upgrade.name());
        assertEquals(2, game.planeValue(), 1e-12, "deux facettes par éclat");
        assertEquals(3 * 0.85, game.planeSpawnSeconds(), 1e-12, "apparition ×0,85");
        assertEquals(4, game.planeMaxShards(), "un éclat de plus sur le plan");
        assertEquals(0.6 * 1.2, game.planeSpeed(), 1e-12, "+20 % de vitesse");
        assertEquals(0.025 * 1.25, game.planePlayerRadius(), 1e-12, "+25 % de taille");
        game.state().setPlaneLevel(PlaneUpgrade.COUNT.ordinal(), PlaneUpgrade.COUNT.max());
        assertFalse(game.canBuyPlane(PlaneUpgrade.COUNT), "vingt éclats au plus");
        assertEquals(20, game.planeMaxShards());
        game.state().setPlaneShards(7);
        assertTrue(game.startNextUniverse());
        assertEquals(7, game.state().planeShards(), 1e-9, "les facettes traversent les univers");
        assertEquals(1, game.planeLevel(PlaneUpgrade.VALUE));
    }

    @Test
    void lesPrixDesCinqAmeliorationsEtLeurCroissance() {
        Game game = plane();
        assertEquals(5, game.planeCost(PlaneUpgrade.VALUE), 1e-9, "valeur : 5 facettes");
        assertEquals(3, game.planeCost(PlaneUpgrade.SPAWN), 1e-9, "apparition : 3");
        assertEquals(4, game.planeCost(PlaneUpgrade.COUNT), 1e-9, "nombre : 4");
        assertEquals(3, game.planeCost(PlaneUpgrade.SPEED), 1e-9, "vitesse : 3");
        assertEquals(4, game.planeCost(PlaneUpgrade.SIZE), 1e-9, "taille : 4");
        game.state().setPlaneShards(4.9);
        assertFalse(game.canBuyPlane(PlaneUpgrade.VALUE), "4,9 facettes : la valeur attend");
        assertTrue(game.canBuyPlane(PlaneUpgrade.SPAWN));
        game.state().setPlaneShards(5);
        assertTrue(game.buyPlane(PlaneUpgrade.VALUE));
        assertEquals(0, game.state().planeShards(), 1e-9, "le prix est pris");
        assertEquals(15, game.planeCost(PlaneUpgrade.VALUE), 1e-9, "valeur ×3 par niveau");
        game.state().setPlaneLevel(PlaneUpgrade.VALUE.ordinal(), 3);
        assertEquals(135, game.planeCost(PlaneUpgrade.VALUE), 1e-9);
        game.state().setPlaneLevel(PlaneUpgrade.SPAWN.ordinal(), 2);
        assertEquals(18.75, game.planeCost(PlaneUpgrade.SPAWN), 1e-9, "apparition ×2,5 par niveau");
        game.state().setPlaneLevel(PlaneUpgrade.COUNT.ordinal(), 2);
        assertEquals(36, game.planeCost(PlaneUpgrade.COUNT), 1e-9, "nombre ×3 par niveau");
        game.state().setPlaneLevel(PlaneUpgrade.SPEED.ordinal(), 3);
        assertEquals(46.875, game.planeCost(PlaneUpgrade.SPEED), 1e-9, "vitesse ×2,5 par niveau");
        game.state().setPlaneLevel(PlaneUpgrade.SIZE.ordinal(), 1);
        assertEquals(10, game.planeCost(PlaneUpgrade.SIZE), 1e-9, "taille ×2,5 par niveau");
        // Les plafonds : 15 niveaux d'apparition, de vitesse et de taille, 17 de nombre, la valeur sans fin.
        game.state().setPlaneLevel(PlaneUpgrade.SPAWN.ordinal(), 15);
        assertEquals(Double.POSITIVE_INFINITY, game.planeCost(PlaneUpgrade.SPAWN));
        game.state().setPlaneLevel(PlaneUpgrade.SPEED.ordinal(), 15);
        assertEquals(Double.POSITIVE_INFINITY, game.planeCost(PlaneUpgrade.SPEED));
        game.state().setPlaneLevel(PlaneUpgrade.SIZE.ordinal(), 15);
        assertEquals(Double.POSITIVE_INFINITY, game.planeCost(PlaneUpgrade.SIZE));
        game.state().setPlaneLevel(PlaneUpgrade.VALUE.ordinal(), 100);
        assertTrue(Double.isFinite(game.planeCost(PlaneUpgrade.VALUE)), "la valeur n'a pas de plafond : au centième niveau, elle a encore un prix");
        // Fermé, le plan ne vend rien.
        Game closed = GameTestSupport.universe(1);
        closed.state().setPlaneShards(1e9);
        assertFalse(closed.canBuyPlane(PlaneUpgrade.VALUE));
        assertFalse(closed.buyPlane(PlaneUpgrade.VALUE));
    }

    @Test
    void leSeuilDuPlanCompteLesFacettesGagnees() {
        Game game = plane();
        assertEquals(3_500, Game.PLANE_THRESHOLD, 0, "version 0.136 : 3 500 facettes gagnées");
        assertFalse(game.isPlaneThresholdReached());
        game.state().setPlaneShardsEarned(3_499);
        assertFalse(game.isPlaneThresholdReached());
        game.state().setPlaneShardsEarned(3_500);
        assertTrue(game.isPlaneThresholdReached());
        assertTrue(game.isDimensionThresholdReached(2));
        game.state().setPlaneShards(0);
        assertTrue(game.isPlaneThresholdReached(), "ce qui est dépensé ne compte pas : les facettes gagnées restent");
    }
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import idle.core.sky.SolarSystem;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Version 0.084 : les plans de l'arbre et des systèmes solaires, en code texte, et la réouverture des bifurcations. */
class PlansTest {

    /** Une partie où la bifurcation « matière » est ouverte, ses deux voies payables. */
    private static Game atFork() {
        return GameTestSupport.atFork();
    }

    private static GameState reread(GameState state) {
        return GameTestSupport.reread(state);
    }

    @Test
    void leCodeDeLArbreSeCopieEtSeRecolle() {
        Game game = atFork();
        assertEquals("arbre:", game.darkTreeCode());
        assertTrue(game.buyDark("dark_fine_synthesis"));
        String code = game.darkTreeCode();
        assertEquals("arbre:dark_fine_synthesis", code);
        // Dans une autre partie : le plan prend la voie tout de suite, elle est payable.
        Game other = atFork();
        Game.PlanLoad load = other.loadDarkPlan(code + ",inconnue,dark_nucleus");
        assertEquals(1, load.known());
        assertEquals(2, load.unknown(), "un identifiant inconnu, et une case qui n'est ni une voie ni une liaison");
        assertTrue(other.state().darkLevelOf("dark_fine_synthesis") > 0);
        assertEquals(Game.PlanStep.TAKEN, other.darkPlanStep(other.darkPlan().get(0)));
        assertTrue(other.loadDarkPlan("rien de lisible").empty());
        assertEquals(List.of("dark_fine_synthesis"), other.state().darkPlan(), "un code illisible ne change pas le plan");
    }

    @Test
    void lePlanAttendCeQuiNEstPasPayableEtSeReprendApresLeBigBang() {
        Game game = atFork();
        game.state().setAtoms(BigNum.ZERO);
        game.loadDarkPlan("arbre:dark_rich_fusion");
        assertEquals(0, game.state().darkLevelOf("dark_rich_fusion"));
        assertEquals(Game.PlanStep.WAITING, game.darkPlanStep(game.darkPlan().get(0)));
        game.state().setAtoms(BigNum.of(5000));
        game.tick(Game.PLAN_CHECK_SECONDS);
        assertTrue(game.state().darkLevelOf("dark_rich_fusion") > 0, "payable : prise au passage suivant du plan");
        // La voie rivale : le plan, qui veut l'autre, la dit bloquée.
        game.loadDarkPlan("arbre:dark_fine_synthesis");
        assertEquals(Game.PlanStep.BLOCKED, game.darkPlanStep(game.darkPlan().get(0)));
        game.forgetDarkPlan();
        assertTrue(game.darkPlan().isEmpty());
        assertTrue(game.state().darkLevelOf("dark_rich_fusion") > 0, "oublier le plan ne rend rien");
    }

    @Test
    void sauvegarderEtChargerLePlanDeLArbre() {
        Game game = atFork();
        assertTrue(game.buyDark("dark_rich_fusion"));
        game.saveDarkPlan();
        assertEquals("arbre:dark_rich_fusion", game.state().savedDarkPlan());
        GameState back = reread(game.state());
        assertEquals("arbre:dark_rich_fusion", back.savedDarkPlan());
        Game other = atFork();
        other.state().setSavedDarkPlan(back.savedDarkPlan());
        assertEquals(1, other.loadSavedDarkPlan().known());
        assertTrue(other.state().darkLevelOf("dark_rich_fusion") > 0);
        assertEquals(List.of("dark_rich_fusion"), reread(other.state()).darkPlan());
    }

    @Test
    void lesBifurcationsSeRouvrentALaProchaineExplosion() {
        Game game = atFork();
        assertFalse(game.canArmForkReset(), "aucune voie prise : rien à rouvrir");
        assertTrue(game.buyDark("dark_rich_fusion"));
        assertTrue(game.canArmForkReset());
        game.setForkResetArmed(true);
        assertTrue(game.state().forkResetArmed());
        assertTrue(reread(game.state()).forkResetArmed());
        GameTestSupport.fillTable(game, 1);
        assertTrue(game.canExplode());
        assertTrue(game.explode());
        assertEquals(0, game.state().darkLevelOf("dark_rich_fusion"), "la voie est rendue");
        assertTrue(game.darkForkRival("dark_fine_synthesis") == null, "l'autre voie se reprend");
        assertFalse(game.state().forkResetArmed(), "une seule fois");
        assertTrue(game.state().darkLevelOf("dark_nucleus") > 0, "le reste de l'arbre ne bouge pas");
    }

    private static Game systems() {
        return Systemes.huitAstres(6);
    }

    @Test
    void leCodeDesSystemesSeCopieEtSeRecolle() {
        Game game = systems();
        assertTrue(game.foundSystem("yellow_sun"));
        assertTrue(game.addPlanet(0, "rocky_planet"));
        assertTrue(game.addPlanet(0, "ocean_planet"));
        assertTrue(game.addMoon(0, "icy_moon"));
        assertTrue(game.foundSystem("blue_giant"));
        assertTrue(game.addPlanet(1, "gas_giant_planet"));
        String code = game.systemsCode();
        assertEquals("systemes:yellow_sun|rocky_planet,ocean_planet|icy_moon;blue_giant|gas_giant_planet|", code);
        Game other = systems();
        // Le joueur avait déjà mis l'océan dans l'autre système : il y reste.
        assertTrue(other.foundSystem("blue_giant"));
        assertTrue(other.addPlanet(0, "ocean_planet"));
        Game.PlanLoad load = other.loadSystemPlan(code);
        assertEquals(6, load.known());
        assertEquals(0, load.unknown());
        int sun = other.systemIndexOf("yellow_sun"), giant = other.systemIndexOf("blue_giant");
        assertEquals(List.of("rocky_planet"), other.state().solarSystems().get(sun).planets());
        assertEquals(List.of("icy_moon"), other.state().solarSystems().get(sun).moons());
        assertEquals(List.of("gas_giant_planet", "ocean_planet"), other.state().solarSystems().get(giant).planets(),
                "les planètes du plan d'abord, celle du joueur ensuite");
        int[] progress = other.systemPlanProgress();
        assertEquals(5, progress[0]);
        assertEquals(6, progress[1]);
        // L'océan libéré, le plan le place à son orbite, au passage suivant.
        other.removeFromSystem(giant, "ocean_planet");
        other.tick(Game.PLAN_CHECK_SECONDS);
        assertEquals(List.of("rocky_planet", "ocean_planet"), other.state().solarSystems().get(sun).planets());
        assertEquals(6, other.systemPlanProgress()[0]);
    }

    @Test
    void unCodeDeSystemesEcarteCeQuIlNeConnaitPas() {
        Game game = systems();
        Game.PlanLoad load = game.loadSystemPlan("systemes:rocky_planet|ocean_planet|;yellow_sun|ocean_planet,icy_moon,martienne|rocky_moon,icy_moon");
        assertEquals(3, load.known(), "le soleil, l'océan, une lune pour une planète");
        assertEquals(4, load.unknown(), "une planète pour étoile, une lune pour planète, une inconnue, une lune de trop");
        assertEquals(List.of(new SolarSystem("yellow_sun", List.of("ocean_planet"), List.of("rocky_moon"))), game.systemPlan());
        assertTrue(game.loadSystemPlan("").empty());
        game.saveSystemPlan();
        assertEquals(game.systemsCode(), reread(game.state()).savedSystemPlan());
        assertEquals(game.systemPlan(), reread(game.state()).systemPlan());
        game.forgetSystemPlan();
        assertTrue(game.systemPlan().isEmpty());
    }

    @Test
    void cinqEmplacementsDeSauvegardeParPlan() {
        Game game = atFork();
        assertTrue(game.buyDark("dark_rich_fusion"));
        game.saveDarkPlan(0);
        game.state().setDarkLevel("dark_rich_fusion", 0);
        assertTrue(game.buyDark("dark_fine_synthesis"));
        game.saveDarkPlan(3);
        assertEquals("arbre:dark_rich_fusion", game.state().savedDarkPlan(0));
        assertEquals("arbre:dark_fine_synthesis", game.state().savedDarkPlan(3));
        assertEquals("", game.state().savedDarkPlan(1));
        assertEquals("", game.state().savedDarkPlan(GameState.PLAN_SLOTS), "hors des emplacements : rien");
        GameState back = reread(game.state());
        assertEquals("arbre:dark_fine_synthesis", back.savedDarkPlan(3));
        assertEquals("arbre:dark_rich_fusion", back.savedDarkPlan(0));
        assertTrue(game.loadSavedDarkPlan(1).empty(), "un emplacement vide ne charge rien");
        assertEquals(1, game.loadSavedDarkPlan(3).known());
        assertEquals(List.of("dark_fine_synthesis"), game.state().darkPlan());
        assertEquals(1, game.describeTreeCode(game.state().savedDarkPlan(3))[0]);
        // Les systèmes aussi.
        game.state().setSavedSystemPlan(4, "systemes:yellow_sun|rocky_planet,ocean_planet|icy_moon;blue_giant||");
        assertEquals(2, game.describeSystemsCode(game.state().savedSystemPlan(4))[0]);
        assertEquals(3, game.describeSystemsCode(game.state().savedSystemPlan(4))[1]);
        assertEquals(game.state().savedSystemPlan(4), reread(game.state()).savedSystemPlan(4));
    }

    @Test
    void uneSauvegardeDeLa085GardeSonPlanDansLePremierEmplacement() {
        // L'ancienne clé, à un seul emplacement, sans la nouvelle.
        idle.core.save.SaveData data = new idle.core.save.SaveData();
        data.putList("savedDarkPlan", List.of("arbre:dark_rich_fusion"));
        data.putList("savedSystemPlan", List.of("systemes:yellow_sun||"));
        GameState back = new GameState();
        back.load(data);
        assertEquals("arbre:dark_rich_fusion", back.savedDarkPlan(0));
        assertEquals("systemes:yellow_sun||", back.savedSystemPlan(0));
        assertEquals("", back.savedDarkPlan(1));
    }

    @Test
    void lesCodesGardesSurviventALaRemiseAZeroDeLUnivers() {
        Game game = atFork();
        game.state().setSavedDarkPlan("arbre:dark_rich_fusion");
        game.state().setSavedSystemPlan("systemes:yellow_sun||");
        game.state().setDarkPlan(List.of("dark_rich_fusion"));
        game.state().startUniverse();
        assertEquals("arbre:dark_rich_fusion", game.state().savedDarkPlan());
        assertEquals("systemes:yellow_sun||", game.state().savedSystemPlan());
        assertTrue(game.state().darkPlan().isEmpty(), "le plan suivi, lui, repart de zéro");
    }
}

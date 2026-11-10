import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.upgrades.Automation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.202 : la sobriété des bots. Quand plus rien dans l'univers ne demande d'éléments (ni la liste de courses, ni
 * la matière ou la variété d'une échelle du cosmos à venir), après le dernier Big Bang, sans palier ni case finie de
 * l'arbre à payer, le bot éteint la boucle des particules, des atomes et des éléments ; il la rallume telle quelle dès
 * qu'un besoin revient. L'univers où elle agit finit à la même seconde ; le hasard de la synthèse, décalé, déplace les
 * univers suivants d'une minute environ (voir le README). Ici, les cas où elle ne doit pas s'éteindre, et l'extinction
 * qui rend exactement ce qu'elle a pris.
 */
class SobrieteTest {

    private static RobotCosmosPlanner planner(Game game) {
        return new RobotCosmosPlanner(game, 20);
    }

    @Test
    void rienNeSEteintAvantLeDernierBigBang() {
        Game game = Bots.fresh(201);
        assertFalse(planner(game).loopIdle(), "partie neuve");
        game.state().setBigBangs(Bots.LAST_BIG_BANG - 1);
        assertFalse(planner(game).loopIdle(), "avant-dernier Big Bang : le suivant demande tout");
    }

    @Test
    void rienNeSEteintTantQueLeCosmosAttendDeLaMatiere() throws Exception {
        // Juste après le huitième Big Bang : la galaxie, l'amas et l'univers attendent encore leur matière.
        Game game = Bots.load(Path.of("sauvegardes", "reference", "checkpoint_after_eighth.save"), 201);
        assertTrue(game.bigBangs() >= Bots.LAST_BIG_BANG, "la sauvegarde est après le dernier Big Bang");
        RobotCosmosPlanner robot = planner(game);
        assertTrue(robot.elementsWanted(), "l'amas et l'univers demandent de la matière, donc des éléments");
        assertFalse(robot.loopIdle());
    }

    @Test
    void laSobrieteSeCoupeParUnePropriete() {
        String was = System.getProperty("idle.sansSobriete");
        try {
            System.setProperty("idle.sansSobriete", "true");
            assertFalse(RobotCosmosPlanner.sober());
            System.clearProperty("idle.sansSobriete");
            assertTrue(RobotCosmosPlanner.sober(), "allumée par défaut");
        } finally {
            if (was == null) System.clearProperty("idle.sansSobriete");
            else System.setProperty("idle.sansSobriete", was);
        }
    }

    @Test
    void lExtinctionRendExactementCeQuElleAPris() {
        Game game = Bots.fresh(201);
        List<String> allumes = new ArrayList<>();
        int k = 0;
        for (Automation a : game.automations()) {
            // Un automatisme sur deux allumé : seuls ceux-là doivent revenir.
            boolean on = k++ % 2 == 0;
            game.state().addAutomation(a.id());
            game.state().setAutomationEnabled(a.id(), on);
            if (on) allumes.add(a.id());
        }
        RobotCosmosPlanner robot = planner(game);
        robot.applySobriety(true);
        assertEquals(allumes, robot.sobrietyOff, "elle note ce qu'elle éteint");
        for (Automation a : game.automations()) assertFalse(game.isAutomationEnabled(a.id()), "éteint : " + a.id());
        robot.applySobriety(true);
        assertEquals(allumes, robot.sobrietyOff, "rien de plus pris la seconde fois");
        robot.applySobriety(false);
        for (Automation a : game.automations()) {
            assertEquals(allumes.contains(a.id()), game.isAutomationEnabled(a.id()), "rendu tel quel : " + a.id());
        }
        assertTrue(robot.sobrietyOff.isEmpty());
    }
}

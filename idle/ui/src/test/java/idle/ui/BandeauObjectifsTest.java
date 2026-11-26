package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.history.Pace;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.309 : les objectifs du bandeau, une ligne courte chacun, le texte de toujours au survol. Sans affichage
 * JavaFX.
 */
class BandeauObjectifsTest {
    @Test
    void chaqueLigneCourteGardeSonTexteCompletAuSurvol() {
        Game game = new Game(1L);
        game.start();
        List<Goals.Line> lines = Goals.lines(game, part -> true, new Pace());
        assertTrue(!lines.isEmpty());
        Goals.Line goal = lines.get(0);
        assertTrue(goal.brief().startsWith("Objectif : "), goal.brief());
        assertTrue(goal.full().startsWith("Objectif : " + Goals.next(game)), goal.full());
        for (Goals.Line line : lines) {
            assertTrue(line.brief().length() <= line.full().length(), "le texte court l'est vraiment : " + line);
            assertTrue(line.brief().length() <= 40, "quelques mots : " + line.brief());
        }
    }

    @Test
    void rienDeMontreRienDAffiche() {
        Game game = new Game(1L);
        game.start();
        assertEquals(List.of(), Goals.lines(game, part -> false, new Pace()));
        assertEquals(1, Goals.lines(game, part -> part == Goals.Part.GOAL, new Pace()).size());
    }

    @Test
    void lObjectifEnQuelquesMots() {
        Game game = new Game(1L);
        assertEquals("un premier générateur", Goals.brief(game), "avant de commencer");
        game.start();
        assertEquals("les " + game.generatorsPerAtom() + " générateurs", Goals.brief(game));
    }

    @Test
    void lActeTroisEnQuelquesMots() {
        Game game = new Game(1L);
        assertEquals("un amas rocheux", Goals.briefAct(game, "un amas rocheux (3/5 conditions), assez d'espace dans 12 min environ"));
        assertEquals("autres assemblages", Goals.briefAct(game, "former d'autres assemblages"));
        assertEquals("autres molécules", Goals.briefAct(game, "créer d'autres molécules"));
        assertEquals("créer une première molécule", Goals.briefAct(game, "créer une première molécule"));
    }
}

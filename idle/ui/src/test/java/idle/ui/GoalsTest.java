package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.sky.*;
import org.junit.jupiter.api.Test;

/** Version 0.126 : l'article de chaque astre du catalogue. Sans affichage JavaFX. */
class GoalsTest {
    /** Les noms féminins du catalogue, par leur premier mot. */
    private static final Set<String> FEMININE = Set.of("comète", "lune", "planète", "géante", "naine", "étoile", "nébuleuse");

    @Test
    void chaqueAstreALeBonArticle() {
        for (Body body : Bodies.DEFAULT) {
            String text = Goals.article(body.name());
            String lower = body.name().toLowerCase(Locale.ROOT);
            String expected = body.name().equals("Soleil") ? "le Soleil"
                    : (FEMININE.contains(lower.split("[ ']")[0]) ? "une " : "un ") + lower;
            assertEquals(expected, text, body.name());
        }
        assertTrue(Goals.article("Nébuleuse planétaire").startsWith("une "));
    }
}

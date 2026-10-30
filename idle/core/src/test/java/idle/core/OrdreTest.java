package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.162 : les copies que l'état donne à lire (molécules, niveaux, automatismes) se parcourent dans l'ordre
 * alphabétique. Avant, {@code Map.copyOf} et {@code Set.copyOf} les parcouraient dans un ordre tiré au sort à chaque
 * lancement du jeu ; le rassemblement automatique, qui parcourt les molécules, ne faisait pas les mêmes choix d'une
 * session à l'autre, et deux parties jouées pareil divergeaient (mesuré avec le bot analyste : 76:06:20 et 76:12:25).
 * Et le nombre d'exemplaires des éléments, lu dans un tableau pour aller plus vite, reste d'accord avec la table.
 */
class OrdreTest {

    private static final List<String> ORDRE = List.of("zeta", "alpha", "mu", "beta", "omega", "kappa", "delta");

    private static List<String> trie() {
        List<String> sorted = new ArrayList<>(ORDRE);
        sorted.sort(null);
        return sorted;
    }

    @Test
    void lesMoleculesSeParcourentDansLOrdreAlphabetique() {
        GameState state = new GameState();
        for (String id : ORDRE) state.addMolecules(id, 3);
        assertEquals(trie(), new ArrayList<>(state.molecules().keySet()));
        assertEquals(3, state.molecules().get("mu"));
    }

    @Test
    void lesNiveauxEtLesAutomatismesAussi() {
        GameState state = new GameState();
        for (String id : ORDRE) {
            state.setLevel(id, 2);
            state.setAutomationEnabled(id, true);
        }
        assertEquals(trie(), new ArrayList<>(state.upgradeLevels().keySet()));
        assertEquals(trie(), new ArrayList<>(state.enabledAutomations()));
    }

    @Test
    void cesCopiesRestentEnLectureSeule() {
        GameState state = new GameState();
        state.addMolecules("alpha", 1);
        assertThrows(UnsupportedOperationException.class, () -> state.molecules().put("beta", 1));
        assertThrows(UnsupportedOperationException.class, () -> state.enabledAutomations().add("beta"));
    }

    @Test
    void leTableauDesElementsSuitLaTable() {
        GameState state = new GameState();
        state.setElementCount(26, 4);
        state.setElementCount(1, 2);
        assertEquals(4, state.elementCount(26));
        state.setElementCount(26, 0);
        assertEquals(0, state.elementCount(26));
        assertEquals(2, state.elementCount(1));
        assertEquals(0, state.elementCount(500), "hors du tableau : la table répond");
        state.clearMatter();
        assertEquals(0, state.elementCount(1), "remis à zéro avec la matière");
        state.setElementCount(8, 3);
        GameState copy = GameTestSupport.reread(state);
        assertEquals(3, copy.elementCount(8), "relu d'une sauvegarde");
    }
}

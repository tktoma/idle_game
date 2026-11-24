package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.296 : l'historique des versions, dans le jeu, est complet. La 0.133 y manquait (reconstituée) ; une note
 * oubliée à une livraison, ou perdue à une fusion, se voit ici. Sans affichage JavaFX.
 */
class HistoriqueDesVersionsTest {
    private static int number(String version) { return Integer.parseInt(version.substring(2)); }

    @Test
    void chaqueVersionALaSienneDansLOrdre() {
        List<String> wrong = new ArrayList<>();
        List<Version.Note> notes = Version.NOTES;
        for (int i = 1; i < notes.size(); i++) {
            int before = number(notes.get(i - 1).number()), after = number(notes.get(i).number());
            if (before != after + 1) wrong.add(notes.get(i - 1).number() + " puis " + notes.get(i).number());
        }
        assertEquals(List.of(), wrong, "les notes vont de la plus récente à la plus ancienne, sans trou ni doublon");
        assertEquals(Version.CURRENT, notes.get(0).number());
        assertEquals("0.034", notes.get(notes.size() - 1).number(), "la première note de l'historique");
    }

    @Test
    void chaqueNoteDitQuelqueChose() {
        for (Version.Note note : Version.NOTES) {
            assertFalse(note.title().isBlank(), note.number());
            assertFalse(note.changes().isEmpty(), note.number());
            for (String change : note.changes()) assertFalse(change.isBlank(), note.number());
        }
    }
}

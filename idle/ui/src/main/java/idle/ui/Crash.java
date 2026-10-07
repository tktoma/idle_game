package idle.ui;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * La mémoire des erreurs imprévues : la première qui survient est gardée, avec le nombre de
 * celles qui ont suivi, pour être jointe au rapport de bug ({@link BugReport}).
 *
 * <p>Une erreur dans l'affichage se répète à chaque image : en garder une seule suffit, et c'est
 * la première qui dit ce qui a cassé.
 */
final class Crash {

    /** Nombre de lignes de la trace gardées : le haut de la pile, là où l'erreur est née. */
    private static final int MAX_LINES = 25;

    private static String first = "";
    private static long count = 0;

    private Crash() {
    }

    /** Retient une erreur imprévue. */
    static synchronized void note(Throwable error) {
        count++;
        if (!first.isEmpty() || error == null) return;
        StringWriter text = new StringWriter();
        error.printStackTrace(new PrintWriter(text));
        String[] lines = text.toString().split("\\R");
        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, MAX_LINES); i++) kept.append(lines[i].stripTrailing()).append('\n');
        if (lines.length > MAX_LINES) kept.append("… (").append(lines.length - MAX_LINES).append(" lignes de plus)\n");
        first = kept.toString();
    }

    /** La première erreur survenue depuis le lancement, ou un texte vide. */
    static synchronized String first() {
        return first;
    }

    /** Nombre d'erreurs survenues depuis le lancement. */
    static synchronized long count() {
        return count;
    }

    /** Oublie tout : pour les essais. */
    static synchronized void clear() {
        first = "";
        count = 0;
    }
}

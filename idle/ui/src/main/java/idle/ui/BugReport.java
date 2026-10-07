package idle.ui;

import idle.core.Game;
import idle.core.SaveCodec;

/**
 * Le rapport de bug : tout ce qu'il faut pour reproduire un problème, en un seul texte à coller.
 * La version du jeu, la machine, où en est la partie, la première erreur survenue s'il y en a
 * eu une ({@link Crash}), et la partie elle-même, exportée.
 *
 * <p>Rien n'est envoyé nulle part : le texte va dans le presse-papiers, et c'est le joueur qui
 * décide à qui le donner.
 */
final class BugReport {

    private BugReport() {
    }

    /** Le rapport pour cette partie, à l'instant présent. */
    static String of(Game game, SaveStore store) {
        StringBuilder text = new StringBuilder();
        text.append("Idle ").append(Version.CURRENT).append(" — rapport de bug\n");
        text.append("Java ").append(property("java.version")).append(" · ").append(property("os.name")).append(' ')
                .append(property("os.version")).append(" · ").append(property("os.arch")).append('\n');
        text.append("Temps de jeu : ").append(Format.duration(game.state().timePlayed()))
                .append(" · Big Bangs : ").append(game.bigBangs())
                .append(" · explosions : ").append(game.state().explosions())
                .append(" · échelles du cosmos : ").append(game.cosmosFormed()).append('\n');
        text.append("Sauvegarde : ").append(store.status()).append('\n');
        text.append('\n');
        if (Crash.count() == 0) {
            text.append("Aucune erreur depuis le lancement.\n");
        } else {
            text.append("Erreurs depuis le lancement : ").append(Crash.count()).append(". La première :\n").append(Crash.first());
        }
        text.append("\nPartie :\n").append(SaveCodec.export(game.state(), System.currentTimeMillis())).append('\n');
        return text.toString();
    }

    private static String property(String name) {
        try {
            return System.getProperty(name, "?");
        } catch (RuntimeException hidden) {
            return "?";
        }
    }
}

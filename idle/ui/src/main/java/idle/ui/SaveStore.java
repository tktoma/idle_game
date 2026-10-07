package idle.ui;

import idle.core.Game;
import idle.core.SaveCodec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Le fichier de sauvegarde : où il est, quand il s'écrit, et comment il se relit.
 *
 * <p>La partie est écrite toutes les {@link #EVERY_SECONDS} secondes et à la fermeture du jeu,
 * dans le dossier des données de l'utilisateur (sous Windows, {@code %APPDATA%\Idle}). Le texte
 * est celui de {@link SaveCodec} : lisible, une ligne par champ.
 *
 * <p>Trois précautions :
 * <ul>
 *   <li>le fichier est d'abord écrit à côté, puis mis à la place de l'ancien d'un seul geste : une
 *       coupure de courant au mauvais moment laisse l'ancienne sauvegarde intacte ;</li>
 *   <li>à chaque lancement réussi, la sauvegarde relue est recopiée en {@code .bak} : si le
 *       fichier devient illisible, c'est cette copie qui est reprise ;</li>
 *   <li>un fichier illisible n'est jamais écrasé : il est mis de côté sous le nom
 *       {@code .illisible}, pour pouvoir être examiné.</li>
 * </ul>
 *
 * <p>Arguments du jeu qui le concernent : {@code --sans-sauvegarde} ne lit ni n'écrit rien ;
 * {@code --dossier=chemin} range la sauvegarde ailleurs ; {@code --test} (le profil de test) écrit
 * dans un fichier à part, pour que les ressources gratuites n'abîment jamais la vraie partie.
 */
final class SaveStore {

    /** Délai entre deux sauvegardes automatiques, en secondes de temps réel. */
    static final double EVERY_SECONDS = 30;

    /**
     * Ce qu'a donné la lecture au lancement.
     *
     * @param found   vrai si une partie a été reprise
     * @param savedAt l'instant de sa sauvegarde, en millisecondes depuis 1970
     * @param notice  ce qu'il faut dire au joueur, ou un texte vide quand tout s'est bien passé
     */
    record Loaded(boolean found, long savedAt, String notice) {
    }

    private final boolean enabled;
    private final Path folder;
    private final Path file;
    private final Path backup;
    private long savedAt = 0;
    private String problem = "";

    private SaveStore(boolean enabled, Path folder, String name) {
        this.enabled = enabled;
        this.folder = folder;
        this.file = folder.resolve(name + ".sav");
        this.backup = folder.resolve(name + ".bak");
    }

    /** Le fichier de sauvegarde que désignent les arguments du jeu. */
    static SaveStore of(List<String> arguments) {
        Path folder = defaultFolder();
        for (String argument : arguments) {
            if (argument.startsWith("--dossier=")) folder = Path.of(argument.substring("--dossier=".length()));
        }
        return new SaveStore(!arguments.contains("--sans-sauvegarde"), folder, arguments.contains("--test") ? "essai" : "partie");
    }

    /** Le dossier des données de l'utilisateur : {@code %APPDATA%\Idle} sous Windows, {@code ~/.idle} ailleurs. */
    private static Path defaultFolder() {
        try {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) return Path.of(appData, "Idle");
        } catch (RuntimeException unavailable) {
            // pas d'accès à l'environnement : on se rabat sur le dossier personnel
        }
        return Path.of(System.getProperty("user.home", "."), ".idle");
    }

    /** Vrai si le jeu sauvegarde : faux seulement quand il a été lancé avec {@code --sans-sauvegarde}. */
    boolean enabled() {
        return enabled;
    }

    /** Le fichier où s'écrit la partie. */
    Path file() {
        return file;
    }

    /** L'instant de la dernière sauvegarde écrite ou relue, en millisecondes depuis 1970 ; 0 s'il n'y en a pas. */
    long savedAt() {
        return savedAt;
    }

    /** Ce qui a empêché la dernière sauvegarde, ou un texte vide si elle s'est bien passée. */
    String problem() {
        return problem;
    }

    /**
     * Reprend la partie sauvegardée, dans la partie en cours. Le fichier d'abord ; s'il est
     * illisible, sa copie de secours. Si rien ne se relit, la partie en cours n'est pas touchée.
     */
    Loaded load(Game game) {
        if (!enabled) return new Loaded(false, 0, "");
        String reason = "";
        for (Path candidate : List.of(file, backup)) {
            if (!Files.isRegularFile(candidate)) continue;
            try {
                SaveCodec.Save save = SaveCodec.readInto(game.state(), Files.readString(candidate, StandardCharsets.UTF_8));
                int dropped = game.restored();
                savedAt = save.savedAt();
                boolean rescued = candidate.equals(backup);
                if (!rescued) copyQuietly(file, backup);
                return new Loaded(true, save.savedAt(), rescued
                        ? "La sauvegarde était illisible (" + reason + ") : la copie de secours a été reprise."
                        : dropped > 0 ? "Sauvegarde d'une autre version du jeu : " + dropped
                                + (dropped > 1 ? " choses que ce jeu ne connaît pas ont été écartées." : " chose que ce jeu ne connaît pas a été écartée.")
                        : "");
            } catch (IOException | SaveCodec.Unreadable | RuntimeException broken) {
                if (reason.isEmpty()) reason = broken.getMessage() == null ? broken.getClass().getSimpleName() : broken.getMessage();
                if (candidate.equals(file)) setAside(file);
            }
        }
        return new Loaded(false, 0, reason.isEmpty() ? ""
                : "La sauvegarde n'a pas pu être relue (" + reason + "). Elle est gardée à côté, sous le nom "
                        + aside(file).getFileName() + ", et une partie neuve commence.");
    }

    /**
     * Écrit la partie dans le fichier.
     *
     * @return vrai si elle est écrite ; sinon {@link #problem()} dit pourquoi
     */
    boolean save(Game game) {
        if (!enabled) return false;
        try {
            long now = System.currentTimeMillis();
            String text = SaveCodec.write(game.state(), now);
            Files.createDirectories(folder);
            Path draft = folder.resolve(file.getFileName() + ".tmp");
            Files.writeString(draft, text, StandardCharsets.UTF_8);
            try {
                Files.move(draft, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(draft, file, StandardCopyOption.REPLACE_EXISTING);
            }
            savedAt = now;
            problem = "";
            return true;
        } catch (IOException | RuntimeException failed) {
            problem = failed.getMessage() == null ? failed.getClass().getSimpleName() : failed.getMessage();
            return false;
        }
    }

    /** La partie sur une ligne, pour le presse-papiers. */
    String export(Game game) {
        return SaveCodec.export(game.state(), System.currentTimeMillis());
    }

    /**
     * Remplace la partie en cours par celle d'un texte exporté, puis l'écrit dans le fichier. Si
     * le texte est illisible, rien ne change.
     *
     * @return le nombre de choses que ce jeu ne connaît pas et qui ont été écartées
     */
    int importText(Game game, String text) throws SaveCodec.Unreadable {
        SaveCodec.readInto(game.state(), text);
        int dropped = game.restored();
        save(game);
        return dropped;
    }

    /** L'état de la sauvegarde, en une phrase : où, et depuis combien de temps. */
    String status() {
        if (!enabled) return "Jeu lancé sans sauvegarde : rien n'est écrit, la partie s'arrête avec la fenêtre.";
        if (!problem.isEmpty()) return "La dernière sauvegarde a échoué : " + problem + ". Fichier : " + file;
        if (savedAt == 0) return "Pas encore de sauvegarde. Fichier : " + file;
        double ago = Math.max(0, (System.currentTimeMillis() - savedAt) / 1000.0);
        return "Sauvegardée il y a " + Format.duration(ago) + ". Fichier : " + file;
    }

    private static Path aside(Path path) {
        return path.resolveSibling(path.getFileName() + ".illisible");
    }

    private static void setAside(Path path) {
        try {
            Files.move(path, aside(path), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException stuck) {
            // tant pis : le fichier reste où il est, et sera remplacé à la prochaine sauvegarde
        }
    }

    private static void copyQuietly(Path from, Path to) {
        try {
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException failed) {
            // la copie de secours est un confort : la sauvegarde elle-même est intacte
        }
    }
}

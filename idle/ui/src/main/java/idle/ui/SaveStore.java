package idle.ui;

import idle.core.Game;
import idle.core.save.SaveCodec;
import idle.ui.common.Format;
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
 * <p>La partie est écrite au rythme choisi dans les réglages ({@link Settings#saveDelay()}) et à la fermeture du jeu,
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
 * <p>Les points de reprise (version 0.127) : {@link #MAX_CHECKPOINTS} emplacements nommés, à côté de la sauvegarde,
 * que le joueur remplit et reprend à la main. Ils s'écrivent avec les mêmes précautions. Reprendre un point met
 * d'abord la partie en cours de côté, dans un emplacement à part ({@link #UNDO_SLOT}) : rien ne se perd par erreur.
 *
 * <p>Arguments du jeu qui le concernent : {@code --sans-sauvegarde} ne lit ni n'écrit rien ;
 * {@code --dossier=chemin} range la sauvegarde ailleurs ; {@code --test} (le profil de test) écrit
 * dans un fichier à part, pour que les ressources gratuites n'abîment jamais la vraie partie.
 */
public final class SaveStore {

    /**
     * Ce qu'a donné la lecture au lancement.
     *
     * @param found   vrai si une partie a été reprise
     * @param savedAt l'instant de sa sauvegarde, en millisecondes depuis 1970
     * @param notice  ce qu'il faut dire au joueur, ou un texte vide quand tout s'est bien passé
     */
    record Loaded(boolean found, long savedAt, String notice) {
    }

    /** Nombre de points de reprise que le joueur peut nommer. */
    static final int MAX_CHECKPOINTS = 5;
    /** L'emplacement où la partie en cours est mise de côté avant la reprise d'un point : il ne compte pas dans les cinq. */
    static final int UNDO_SLOT = 0;

    /**
     * Un point de reprise, tel qu'il se lit sur le disque.
     *
     * @param slot    son numéro : 1 à {@link #MAX_CHECKPOINTS}, ou {@link #UNDO_SLOT}
     * @param preview ce qu'il contient, ou {@code null} s'il est vide ou illisible
     * @param problem pourquoi il ne se lit pas, ou un texte vide
     */
    record Checkpoint(int slot, SaveCodec.Preview preview, String problem) {
        boolean used() {
            return preview != null;
        }
    }

    private final boolean enabled;
    private final Path folder;
    private final Path file;
    private final Path backup;
    private final Path checkpoints;
    private long savedAt = 0;
    private String problem = "";
    private String checkpointProblem = "";

    private SaveStore(boolean enabled, Path folder, String name) {
        this.enabled = enabled;
        this.folder = folder;
        this.file = folder.resolve(name + ".sav");
        this.backup = folder.resolve(name + ".bak");
        // Le profil de test a ses propres points : ses ressources gratuites ne passent jamais dans la vraie partie.
        this.checkpoints = folder.resolve("points-" + name);
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

    /**
     * Le dossier des données, où s'écrivent la sauvegarde et ses points de reprise : le profil y copie aussi l'image
     * que le joueur choisit pour son avatar (version 0.147).
     */
    public Path folder() {
        return folder;
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
     * Pourquoi le dernier point de reprise n'a pas pu s'écrire ou s'effacer, ou un texte vide (version 0.127). À part
     * de {@link #problem()} : un point qui échoue ne fait pas croire que la sauvegarde ordinaire a échoué, et l'inverse.
     */
    String checkpointProblem() {
        return checkpointProblem;
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
            writeAtomically(file, SaveCodec.write(game.state(), now, new SaveCodec.Meta("", Version.CURRENT)));
            savedAt = now;
            problem = "";
            return true;
        } catch (IOException | RuntimeException failed) {
            problem = failed.getMessage() == null ? failed.getClass().getSimpleName() : failed.getMessage();
            return false;
        }
    }

    /** Écrit un fichier à côté, puis le met à la place de l'ancien d'un seul geste. */
    private static void writeAtomically(Path target, String text) throws IOException {
        Files.createDirectories(target.getParent());
        Path draft = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(draft, text, StandardCharsets.UTF_8);
        try {
            Files.move(draft, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(draft, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** La partie sur une ligne, pour le presse-papiers, avec la version du jeu qui l'exporte (version 0.127). */
    String export(Game game) {
        return SaveCodec.export(game.state(), System.currentTimeMillis(), new SaveCodec.Meta("", Version.CURRENT));
    }

    // ------------------------------------------------------------------
    // Les points de reprise (version 0.127)
    // ------------------------------------------------------------------

    private Path checkpointFile(int slot) {
        return checkpoints.resolve(slot == UNDO_SLOT ? "avant-reprise.sav" : "point-" + slot + ".sav");
    }

    private static boolean validSlot(int slot) {
        return slot == UNDO_SLOT || (slot >= 1 && slot <= MAX_CHECKPOINTS);
    }

    /** Les points déjà lus, avec la date et la taille de leur fichier : la page des réglages les redemande à chaque image. */
    private final java.util.Map<Integer, Object[]> checkpointCache = new java.util.HashMap<>();

    /** Le point de cet emplacement, lu sur le disque (ou en mémoire, tant que son fichier n'a pas changé). */
    Checkpoint checkpoint(int slot) {
        if (!enabled || !validSlot(slot)) return new Checkpoint(slot, null, "");
        Path path = checkpointFile(slot);
        if (!Files.isRegularFile(path)) {
            checkpointCache.remove(slot);
            return new Checkpoint(slot, null, "");
        }
        long modified, size;
        try {
            modified = Files.getLastModifiedTime(path).toMillis();
            size = Files.size(path);
        } catch (IOException | RuntimeException unreadable) {
            return new Checkpoint(slot, null, unreadable.getMessage() == null ? "fichier inaccessible" : unreadable.getMessage());
        }
        Object[] cached = checkpointCache.get(slot);
        if (cached != null && (long) cached[0] == modified && (long) cached[1] == size) return (Checkpoint) cached[2];
        Checkpoint point;
        try {
            point = new Checkpoint(slot, SaveCodec.preview(Files.readString(path, StandardCharsets.UTF_8)), "");
        } catch (IOException | SaveCodec.Unreadable | RuntimeException broken) {
            point = new Checkpoint(slot, null, broken.getMessage() == null ? broken.getClass().getSimpleName() : broken.getMessage());
        }
        checkpointCache.put(slot, new Object[] {modified, size, point});
        return point;
    }

    /**
     * Compare deux numéros de version du jeu, partie par partie : « 0.99 » vient avant « 0.127 ». Un numéro illisible
     * compte comme le plus ancien.
     */
    static int compareVersions(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int left = i < x.length ? number(x[i]) : 0, right = i < y.length ? number(y[i]) : 0;
            if (left != right) return Integer.compare(left, right);
        }
        return 0;
    }

    private static int number(String part) {
        try {
            return Integer.parseInt(part.trim());
        } catch (NumberFormatException unreadable) {
            return -1;
        }
    }

    /**
     * Écrit la partie en cours dans cet emplacement, sous ce nom (vide : « Point n »). Un point déjà rempli est remplacé.
     *
     * @return vrai s'il est écrit ; sinon {@link #checkpointProblem()} dit pourquoi
     */
    boolean saveCheckpoint(Game game, int slot, String name) {
        if (!enabled || !validSlot(slot)) return false;
        String label = name == null || name.isBlank() ? (slot == UNDO_SLOT ? "Avant la dernière reprise" : "Point " + slot) : name;
        checkpointCache.remove(slot);
        try {
            writeAtomically(checkpointFile(slot), SaveCodec.write(game.state(), System.currentTimeMillis(),
                    new SaveCodec.Meta(label, Version.CURRENT)));
            checkpointProblem = "";
            return true;
        } catch (IOException | RuntimeException failed) {
            checkpointProblem = failed.getMessage() == null ? failed.getClass().getSimpleName() : failed.getMessage();
            return false;
        }
    }

    /**
     * Reprend la partie de cet emplacement. La partie en cours est d'abord mise de côté dans {@link #UNDO_SLOT} ; quand
     * c'est lui qu'on reprend, les deux parties s'échangent (il est lu avant d'être réécrit). Seule la dernière partie
     * quittée est gardée ainsi. Si le point ne se relit pas, rien ne change.
     *
     * @return le nombre de choses que ce jeu ne connaît pas et qui ont été écartées
     */
    int loadCheckpoint(Game game, int slot) throws SaveCodec.Unreadable {
        if (!enabled || !validSlot(slot)) throw new SaveCodec.Unreadable("Cet emplacement n'existe pas.", null);
        String text;
        try {
            text = Files.readString(checkpointFile(slot), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException missing) {
            throw new SaveCodec.Unreadable("Ce point de reprise est vide.", missing);
        }
        SaveCodec.read(text);   // relu à part d'abord : illisible, rien ne bouge
        if (!saveCheckpoint(game, UNDO_SLOT, "")) {
            throw new SaveCodec.Unreadable("La partie en cours n'a pas pu être mise de côté (" + checkpointProblem
                    + ") : rien n'a changé.", null);
        }
        return importText(game, text);
    }

    /**
     * Ce que contient une sauvegarde, en une ligne : « « Avant le Big Bang » · 23 oct. 2026, 21:14 · version 0.127 ·
     * 12 h 30 de jeu · 3 Big Bangs · 40 succès ».
     */
    static String describe(SaveCodec.Preview preview) {
        StringBuilder text = new StringBuilder();
        if (!preview.meta().label().isEmpty()) text.append("« ").append(preview.meta().label()).append(" » · ");
        if (preview.savedAt() > 0) {
            text.append(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", java.util.Locale.FRENCH)
                    .format(java.time.Instant.ofEpochMilli(preview.savedAt()).atZone(java.time.ZoneId.systemDefault()))).append(" · ");
        }
        text.append(preview.meta().gameVersion().isEmpty() ? "version d'avant la 0.127" : "version " + preview.meta().gameVersion());
        text.append(" · ").append(Format.duration(preview.timePlayed())).append(" de jeu");
        if (preview.universes() > 0) text.append(" · ").append(preview.universes()).append(preview.universes() > 1 ? " univers" : " univers formé");
        text.append(" · ").append(preview.bigBangs()).append(preview.bigBangs() > 1 ? " Big Bangs" : " Big Bang");
        text.append(" · ").append(preview.achievements()).append(" succès");
        return text.toString();
    }

    /** Efface le point de cet emplacement. */
    boolean deleteCheckpoint(int slot) {
        if (!enabled || !validSlot(slot)) return false;
        checkpointCache.remove(slot);
        try {
            boolean deleted = Files.deleteIfExists(checkpointFile(slot));
            checkpointProblem = "";
            return deleted;
        } catch (IOException | RuntimeException stuck) {
            checkpointProblem = stuck.getMessage() == null ? stuck.getClass().getSimpleName() : stuck.getMessage();
            return false;
        }
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

    /**
     * L'état de la sauvegarde, sans le chemin du fichier : pour le rapport de bug, qui n'a pas à dire où le joueur
     * range ses fichiers (version 0.127). Le profil (partie ou essai) suffit.
     */
    String statusWithoutPath() {
        if (!enabled) return "jeu lancé sans sauvegarde";
        String profile = " (fichier " + (file == null ? "?" : file.getFileName()) + ")";
        if (!problem.isEmpty()) return "la dernière a échoué : " + problem + profile;
        if (savedAt == 0) return "pas encore de sauvegarde" + profile;
        double ago = Math.max(0, (System.currentTimeMillis() - savedAt) / 1000.0);
        return "il y a " + Format.duration(ago) + profile;
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

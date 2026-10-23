package idle.core.save;

import idle.core.GameState;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * La partie en texte, et retour : c'est ce qui s'écrit dans le fichier de sauvegarde, et ce que
 * le joueur copie pour emporter sa partie ailleurs.
 *
 * <p>Deux formes du même contenu :
 * <ul>
 *   <li>{@link #write} : le texte lisible, une ligne {@code clé=valeur} par champ, précédé d'une
 *       ligne d'en-tête qui porte le numéro de version du format. C'est le fichier ;</li>
 *   <li>{@link #export} : ce même texte compressé puis écrit en lettres et chiffres, sur une
 *       seule ligne, pour le presse-papiers.</li>
 * </ul>
 * {@link #read} relit les deux.
 *
 * <p>La sauvegarde ne contient que la partie ({@link GameState}, statistiques et historiques
 * compris). Les réglages de la fenêtre (affichage, sons, touches) n'en font pas partie : ils restent sur sa machine.
 * Ceux qui règlent le jeu lui-même (coupure des automatismes, ordres d'achat, plans…) sont dans la partie.
 */
public final class SaveCodec {

    /** Premier mot de toute sauvegarde lisible. */
    public static final String HEADER = "idle-save";
    /** Numéro du format écrit par cette version du jeu. Une sauvegarde d'un format plus récent est refusée. */
    public static final int VERSION = 1;
    /** Premier mot d'une partie exportée pour le presse-papiers. */
    public static final String EXPORT_PREFIX = "idle1:";

    private SaveCodec() {
    }

    /**
     * Une sauvegarde relue.
     *
     * @param state   la partie
     * @param savedAt l'instant où elle a été écrite, en millisecondes depuis 1970 ; sert à compter l'absence du joueur
     * @param version le numéro de format de la sauvegarde relue
     * @param meta    son nom et la version du jeu qui l'a écrite (version 0.127), vides pour une sauvegarde plus ancienne
     */
    public record Save(GameState state, long savedAt, int version, Meta meta) {

        public Save(GameState state, long savedAt, int version) {
            this(state, savedAt, version, Meta.NONE);
        }
    }

    /**
     * Ce qu'une sauvegarde dit d'elle-même, en plus de la partie (version 0.127) : le nom que le joueur lui a donné
     * (un point de reprise), et la version du jeu qui l'a écrite. Une version plus ancienne du jeu les ignore : le
     * format ne change pas.
     *
     * @param label       son nom, vide pour la sauvegarde ordinaire ; {@link #MAX_LABEL} caractères au plus
     * @param gameVersion la version du jeu, « 0.127 » par exemple ; vide si elle n'est pas connue
     */
    public record Meta(String label, String gameVersion) {
        /** Rien de dit : la sauvegarde ordinaire d'une version qui n'écrivait pas ces renseignements. */
        public static final Meta NONE = new Meta("", "");
        /** La longueur la plus grande d'un nom. */
        public static final int MAX_LABEL = 40;

        public Meta {
            label = clean(label, MAX_LABEL);
            gameVersion = clean(gameVersion, 16);
        }

        private static String clean(String text, int max) {
            if (text == null) return "";
            String flat = text.replaceAll("\\p{Cntrl}", " ").strip();
            return flat.length() > max ? flat.substring(0, max).strip() : flat;
        }
    }

    /**
     * Ce qu'une sauvegarde contient, en quelques chiffres, sans la charger (version 0.127) : pour la montrer avant de
     * l'importer ou de la reprendre.
     *
     * @param format         le numéro de format
     * @param meta           son nom et la version du jeu qui l'a écrite
     * @param savedAt        l'instant de son écriture, en millisecondes depuis 1970
     * @param timePlayed     le temps de jeu, en secondes
     * @param bigBangs       les Big Bangs de la partie en cours
     * @param explosions     les explosions depuis le dernier Big Bang
     * @param achievements   les succès obtenus
     * @param universes      les univers formés
     */
    public record Preview(int format, Meta meta, long savedAt, double timePlayed, int bigBangs, int explosions,
                          int achievements, int universes) {
    }

    /** La sauvegarde n'a pas pu être relue : son message dit pourquoi, en une phrase pour le joueur. */
    public static final class Unreadable extends Exception {
        private static final long serialVersionUID = 1L;

        public Unreadable(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * La partie en texte lisible.
     *
     * @param savedAt l'instant de la sauvegarde, en millisecondes depuis 1970
     */
    public static String write(GameState state, long savedAt) {
        return write(state, savedAt, Meta.NONE);
    }

    /**
     * La partie en texte lisible, avec ce que la sauvegarde dit d'elle-même (version 0.127).
     *
     * @param savedAt l'instant de la sauvegarde, en millisecondes depuis 1970
     * @param meta    son nom et la version du jeu
     */
    public static String write(GameState state, long savedAt, Meta meta) {
        SaveData data = new SaveData();
        data.put("savedAt", savedAt);
        if (meta != null && !meta.label().isEmpty()) data.put("label", SaveData.escape(meta.label()));
        if (meta != null && !meta.gameVersion().isEmpty()) data.put("gameVersion", SaveData.escape(meta.gameVersion()));
        state.save(data);
        StringBuilder text = new StringBuilder(1 << 16);
        text.append(HEADER).append(' ').append(VERSION).append('\n');
        data.writeTo(text);
        return text.toString();
    }

    /** La partie compressée, sur une seule ligne de lettres et de chiffres : pour le presse-papiers. */
    public static String export(GameState state, long savedAt) {
        return export(state, savedAt, Meta.NONE);
    }

    /** La partie compressée, sur une seule ligne, avec ce qu'elle dit d'elle-même (version 0.127). */
    public static String export(GameState state, long savedAt, Meta meta) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream zip = new GZIPOutputStream(bytes)) {
                zip.write(write(state, savedAt, meta).getBytes(StandardCharsets.UTF_8));
            }
            return EXPORT_PREFIX + Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);   // rien ne s'écrit ailleurs qu'en mémoire
        }
    }

    /**
     * Relit une sauvegarde, sous l'une ou l'autre forme : le texte lisible du fichier, ou la ligne
     * exportée. Les espaces et retours à la ligne ajoutés autour par un copier-coller sont ignorés.
     *
     * @throws Unreadable si le texte n'est pas une sauvegarde, s'il est abîmé, ou s'il vient d'une
     *                    version plus récente du jeu
     */
    public static Save read(String text) throws Unreadable {
        if (text == null || text.isBlank()) throw new Unreadable("Il n'y a rien à relire.", null);
        String trimmed = text.strip();
        if (trimmed.startsWith(EXPORT_PREFIX)) trimmed = unzip(trimmed.substring(EXPORT_PREFIX.length()));
        if (!trimmed.startsWith(HEADER + " ")) throw new Unreadable("Ce texte n'est pas une sauvegarde du jeu.", null);
        String[] lines = trimmed.split("\\R");
        int version;
        try {
            version = Integer.parseInt(lines[0].substring(HEADER.length() + 1).trim());
        } catch (NumberFormatException broken) {
            throw new Unreadable("L'en-tête de la sauvegarde est abîmé.", broken);
        }
        if (version > VERSION) {
            throw new Unreadable("Cette sauvegarde vient d'une version plus récente du jeu (format " + version
                    + ", ce jeu lit jusqu'au format " + VERSION + ").", null);
        }
        SaveData data = new SaveData();
        for (int i = 1; i < lines.length; i++) data.readLine(lines[i]);
        GameState state = new GameState();
        try {
            state.load(data);
            Meta meta = new Meta(SaveData.unescape(data.text("label", "")), SaveData.unescape(data.text("gameVersion", "")));
            return new Save(state, data.count("savedAt", 0), version, meta);
        } catch (RuntimeException broken) {
            throw new Unreadable("La sauvegarde est abîmée : " + broken.getMessage(), broken);
        }
    }

    /**
     * Ce qu'une sauvegarde contient, sans rien charger dans la partie en cours (version 0.127).
     *
     * @throws Unreadable si le texte ne se relit pas
     */
    public static Preview preview(String text) throws Unreadable {
        Save save = read(text);
        GameState state = save.state();
        int universes = state.universeFormed() || state.cosmosLevel() >= 3 ? 1 + state.extraUniverses() : 0;
        return new Preview(save.version(), save.meta(), save.savedAt(), state.timePlayed(), state.bigBangs(),
                state.explosions(), state.achievements().size(), universes);
    }

    /**
     * Remplace une partie par celle d'une sauvegarde. Le texte est d'abord relu à part : s'il est
     * illisible, la partie en cours n'est pas touchée.
     *
     * @return la sauvegarde relue ; son état est une copie, celui qui compte est {@code target}
     */
    public static Save readInto(GameState target, String text) throws Unreadable {
        Save save = read(text);
        // Relue sans erreur une première fois, elle se relit de même dans la partie en cours.
        SaveData data = new SaveData();
        for (String line : write(save.state(), save.savedAt()).split("\\R")) data.readLine(line);
        target.load(data);
        return save;
    }

    private static String unzip(String encoded) throws Unreadable {
        try {
            byte[] zipped = Base64.getMimeDecoder().decode(encoded.replaceAll("\\s", ""));
            try (GZIPInputStream zip = new GZIPInputStream(new ByteArrayInputStream(zipped))) {
                return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException | IllegalArgumentException broken) {
            throw new Unreadable("Le texte collé est incomplet ou abîmé.", broken);
        }
    }
}

package idle.core;

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
 * compris). Les réglages du joueur n'en font pas partie : ils restent sur sa machine.
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
     */
    public record Save(GameState state, long savedAt, int version) {
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
        SaveData data = new SaveData();
        data.put("savedAt", savedAt);
        state.save(data);
        StringBuilder text = new StringBuilder(1 << 16);
        text.append(HEADER).append(' ').append(VERSION).append('\n');
        data.writeTo(text);
        return text.toString();
    }

    /** La partie compressée, sur une seule ligne de lettres et de chiffres : pour le presse-papiers. */
    public static String export(GameState state, long savedAt) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream zip = new GZIPOutputStream(bytes)) {
                zip.write(write(state, savedAt).getBytes(StandardCharsets.UTF_8));
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
            return new Save(state, data.count("savedAt", 0), version);
        } catch (RuntimeException broken) {
            throw new Unreadable("La sauvegarde est abîmée : " + broken.getMessage(), broken);
        }
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

package idle.ui;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.IntSupplier;

/**
 * Le fichier des images par seconde du profil test UI (version 0.253) : une ligne par combinaison de skins mesurée
 * (sur tous ses états montrés), ajoutée au fichier
 * à mesure, pour les comparer après (un tableur l'ouvre : séparateur « ; », virgule décimale). Le fichier grandit d'un
 * passage à l'autre ; chaque ligne dit sa date, sa version et le plafond d'images par seconde réglé.
 */
final class UiTestFpsFile {
    /** Le nom du fichier, dans le dossier des données du jeu. */
    static final String NAME = "test-ui-fps.csv";
    static final String HEADER = "date;version;onglet;mode;combinaison n°;combinaison des skins;états mesurés;état le plus lent;images;"
            + "durée (s);images par seconde (moyenne);images par seconde (plus bas);pire image (ms);calcul par image (ms);"
            + "mise en place (ms);plafond (images par seconde)";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path file;
    private final IntSupplier cap;
    private String problem = "";

    /**
     * @param file le fichier où écrire
     * @param cap  le plafond d'images par seconde réglé, au moment de chaque ligne
     */
    UiTestFpsFile(Path file, IntSupplier cap) {
        this.file = file;
        this.cap = cap;
    }

    Path file() { return file; }

    /** Ce qui a empêché d'écrire la dernière fois ; vide si tout va bien. */
    String problem() { return problem; }

    /** Ajoute la ligne d'une combinaison (et l'en-tête si le fichier est neuf). Une erreur d'écriture est gardée, pas lancée. */
    void write(UiTestRun.Fps fps) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            boolean fresh = !Files.exists(file) || Files.size(file) == 0;
            try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (fresh) out.write("﻿" + HEADER + "\n");
                out.write(line(fps, LocalDateTime.now(), cap.getAsInt()));
                out.write("\n");
            }
            problem = "";
        } catch (IOException | RuntimeException failed) {
            problem = failed.getClass().getSimpleName() + (failed.getMessage() == null ? "" : " : " + failed.getMessage());
        }
    }

    /** La ligne d'une combinaison, sans retour à la ligne. */
    static String line(UiTestRun.Fps fps, LocalDateTime when, int cap) {
        return String.join(";", DATE.format(when), Version.CURRENT, clean(fps.group()), clean(fps.mode()), String.valueOf(fps.combination() + 1),
                clean(fps.skins()), String.valueOf(fps.states()), clean(fps.slowestState()), String.valueOf(fps.frames()), number(fps.seconds(), 2),
                number(fps.average(), 1), number(fps.lowest(), 1), number(fps.worstMs(), 1), number(fps.workMs(), 2),
                number(fps.setupMs(), 1), String.valueOf(cap));
    }

    /** Un texte sans séparateur ni retour à la ligne : il reste dans sa colonne. */
    private static String clean(String text) { return text.replace(';', ',').replace('\n', ' ').replace('\r', ' '); }

    private static String number(double value, int decimals) {
        return Double.isFinite(value) ? String.format(Locale.FRANCE, "%." + decimals + "f", value) : "";
    }
}

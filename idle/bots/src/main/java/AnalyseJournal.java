import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Le journal du bot analyste (version 0.161) : tout ce qu'il a fait et vu, horodaté à la seconde de jeu. Il s'écrit en
 * texte brut (journal.txt), en tableaux pour un tableur (achats.csv, paliers.csv, estimations.csv) et sert de matière à
 * la page HTML du rapport ({@link AnalyseRapport}).
 */
final class AnalyseJournal {

    /** Ce qu'est une ligne du journal. */
    enum Sorte {
        PALIER("Palier"), ACHAT("Achat"), OBTENU("Obtenu"), CHOIX("Choix"), AUTOMATISME("Automatisme"), DEFI("Défi"),
        SUCCES("Succès"), ESTIMATION("Estimation"), BLOCAGE("Blocage"), INFO("Info");

        final String label;

        Sorte(String label) {
            this.label = label;
        }
    }

    /** Une ligne : quand (en secondes de jeu), quoi, et le détail. */
    record Ligne(double t, int univers, Sorte sorte, String titre, List<String> details) {}

    /**
     * Un achat : quand, quoi, combien de niveaux, ce qu'il a coûté (la ressource, le montant, sa part de la réserve), son
     * poids sur chaque grandeur (en ordres de grandeur), et le temps qu'il fait gagner selon l'estimation du moment.
     */
    record Achat(double t, int univers, String groupe, String chose, double quantite, String paye, String montant,
            double part, double montantLog, String poids, double poidsMax, double gainSecondes) {}

    /** Un palier atteint : quand, lequel, le temps depuis le précédent, et le moment que la dernière estimation annonçait (NaN sans). */
    record Palier(double t, int univers, String nom, double depuis, double annonce) {}

    /** Un choix de façon de jouer (version 0.162) : quand, pour quel palier, laquelle, et le temps de chacune (en secondes, −1 : pas atteint). */
    record Choix(double t, String vise, String retenue, List<String> temps) {}

    /** Une estimation : quand, le palier visé, le temps estimé (négatif : au-delà de l'horizon), le goulot. */
    record Estimation(double t, String vise, double secondes, String goulot) {}

    final List<Ligne> lignes = new ArrayList<>();
    final List<Achat> achats = new ArrayList<>();
    final List<Palier> paliers = new ArrayList<>();
    final List<Estimation> estimations = new ArrayList<>();
    final List<Choix> choix = new ArrayList<>();

    void ajoute(double t, int univers, Sorte sorte, String titre, List<String> details) {
        lignes.add(new Ligne(t, univers, sorte, titre, List.copyOf(details)));
    }

    void ajoute(double t, int univers, Sorte sorte, String titre) {
        ajoute(t, univers, sorte, titre, List.of());
    }

    /** 82:45:13 : des heures (au-delà de 24), des minutes et des secondes. */
    static String chrono(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds)) return "—";
        long s = Math.round(Math.max(0, seconds));
        return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    /** Une durée lisible : « 1 h 12 min 05 s », « 42 s ». */
    static String duree(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds < 0) return "—";
        long s = Math.round(seconds);
        if (s < 60) return s + " s";
        if (s < 3600) return String.format(Locale.ROOT, "%d min %02d s", s / 60, s % 60);
        return String.format(Locale.ROOT, "%d h %02d min %02d s", s / 3600, (s / 60) % 60, s % 60);
    }

    private static String csv(String value) {
        return "\"" + String.valueOf(value).replace("\"", "\"\"") + "\"";
    }

    void ecrire(Path dossier) throws Exception {
        Files.createDirectories(dossier);
        StringBuilder txt = new StringBuilder();
        for (Ligne l : lignes) {
            txt.append('[').append(chrono(l.t())).append("] u").append(l.univers()).append(' ')
                    .append(String.format(Locale.ROOT, "%-11s", l.sorte().label)).append(' ').append(l.titre()).append('\n');
            for (String d : l.details()) txt.append("             ").append(d).append('\n');
        }
        Files.writeString(dossier.resolve("journal.txt"), txt.toString());
        StringBuilder a = new StringBuilder("chrono,secondes,univers,groupe,chose,quantite,paye,montant,part_reserve,montant_log10,poids,poids_max_log10,gain_estime_s\n");
        for (Achat x : achats) {
            a.append(String.format(Locale.ROOT, "%s,%.0f,%d,%s,%s,%.0f,%s,%s,%.4f,%.4f,%s,%.5f,%.1f%n", chrono(x.t()), x.t(), x.univers(),
                    csv(x.groupe()), csv(x.chose()), x.quantite(), csv(x.paye()), csv(x.montant()), x.part(), x.montantLog(), csv(x.poids()),
                    x.poidsMax(), x.gainSecondes()));
        }
        Files.writeString(dossier.resolve("achats.csv"), a.toString());
        StringBuilder p = new StringBuilder("chrono,secondes,univers,palier,depuis_precedent_s,annonce_pour_s,ecart_s\n");
        for (Palier x : paliers) {
            String annonce = Double.isNaN(x.annonce()) ? "," // pas d'annonce pour ce palier : deux cases vides
                    : String.format(Locale.ROOT, "%.0f,%.0f", x.annonce(), x.t() - x.annonce());
            p.append(String.format(Locale.ROOT, "%s,%.0f,%d,%s,%.0f,%s%n", chrono(x.t()), x.t(), x.univers(), csv(x.nom()), x.depuis(), annonce));
        }
        Files.writeString(dossier.resolve("paliers.csv"), p.toString());
        StringBuilder e = new StringBuilder("chrono,secondes,palier_vise,estime_s,goulot\n");
        for (Estimation x : estimations) {
            e.append(String.format(Locale.ROOT, "%s,%.0f,%s,%.0f,%s%n", chrono(x.t()), x.t(), csv(x.vise()), x.secondes(), csv(x.goulot())));
        }
        Files.writeString(dossier.resolve("estimations.csv"), e.toString());
        StringBuilder c = new StringBuilder("chrono,secondes,palier_vise,retenue,essais\n");
        for (Choix x : choix) {
            c.append(String.format(Locale.ROOT, "%s,%.0f,%s,%s,%s%n", chrono(x.t()), x.t(), csv(x.vise()), csv(x.retenue()), csv(String.join(" ; ", x.temps()))));
        }
        Files.writeString(dossier.resolve("choix.csv"), c.toString());
    }
}

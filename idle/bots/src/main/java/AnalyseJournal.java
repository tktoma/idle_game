import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Le journal du bot analyste (version 0.161) : tout ce qu'il a fait et vu, horodaté à la seconde de jeu. Il s'écrit en
 * texte brut (journal.txt), en tableaux pour un tableur (achats.csv, paliers.csv, estimations.csv, et depuis la 0.162
 * classement.csv et etsi.csv) et sert de matière à la page HTML du rapport ({@link AnalyseRapport}).
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
     * Version 0.162 : sa rentabilité. {@code coutSecondes} est le temps de production qu'il a coûté (le montant divisé par
     * ce que la ressource rapportait par seconde à ce moment) ; {@code rentabilite} est le temps gagné divisé par ce coût
     * (au-dessus de 1, l'achat rend plus de temps qu'il n'en a coûté). NaN quand l'un des deux est inconnu.
     */
    record Achat(double t, int univers, String groupe, String chose, double quantite, String paye, String montant,
            double part, double montantLog, String poids, double poidsMax, double gainSecondes, double coutSecondes, double rentabilite) {

        /** Ce qui a été acheté, en une ligne : « Amélioration — Couplage ». */
        String quoi() {
            return groupe + (chose.isEmpty() ? "" : " — " + chose);
        }
    }

    /**
     * Version 0.162 : le classement d'une chose achetée sur toute la partie. Combien de fois, le temps de production
     * qu'elle a coûté en tout, le temps qu'elle a fait gagner en tout, et le rapport des deux (les achats sans coût ou
     * sans gain connus ne comptent pas dans les sommes).
     */
    record Classement(String quoi, int achats, double coutSecondes, double gainSecondes) {
        double rentabilite() {
            return coutSecondes > 0 ? gainSecondes / coutSecondes : Double.NaN;
        }
    }

    /**
     * Version 0.162 : « et si » à un palier principal. Depuis ce palier, la suite jouée sur une copie de la partie
     * jusqu'au palier principal suivant, au rythme du bot (5 s), d'un joueur qui passe une fois par minute, et d'un
     * joueur qui passe toutes les dix minutes ; puis le temps qu'il a vraiment fallu (rempli au palier suivant).
     * Une durée négative : pas atteint dans l'horizon.
     */
    record EtSi(double t, int univers, String depuis, String vise, double parMinute, double parDixMinutes, double reel) {}

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
    final List<EtSi> etsi = new ArrayList<>();

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

    /** Le classement de tout ce qui a été acheté, du plus rentable au moins rentable (les choses sans coût connu à la fin). */
    List<Classement> classement() {
        Map<String, double[]> sums = new LinkedHashMap<>();
        for (Achat a : achats) {
            double[] s = sums.computeIfAbsent(a.quoi(), k -> new double[3]);
            s[0]++;
            if (Double.isFinite(a.coutSecondes()) && Double.isFinite(a.gainSecondes())) {
                s[1] += a.coutSecondes();
                s[2] += a.gainSecondes();
            }
        }
        List<Classement> out = new ArrayList<>();
        sums.forEach((k, v) -> out.add(new Classement(k, (int) v[0], v[1], v[2])));
        out.sort((x, y) -> {
            double rx = x.rentabilite(), ry = y.rentabilite();
            if (Double.isNaN(rx) != Double.isNaN(ry)) return Double.isNaN(rx) ? 1 : -1;
            return Double.isNaN(rx) ? Integer.compare(y.achats(), x.achats()) : Double.compare(ry, rx);
        });
        return out;
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
        StringBuilder a = new StringBuilder("chrono,secondes,univers,groupe,chose,quantite,paye,montant,part_reserve,montant_log10,poids,poids_max_log10,"
                + "gain_estime_s,cout_en_production_s,rentabilite\n");
        for (Achat x : achats) {
            a.append(String.format(Locale.ROOT, "%s,%.0f,%d,%s,%s,%.0f,%s,%s,%.4f,%.4f,%s,%.5f,%.1f,%s,%s%n", chrono(x.t()), x.t(), x.univers(),
                    csv(x.groupe()), csv(x.chose()), x.quantite(), csv(x.paye()), csv(x.montant()), x.part(), x.montantLog(), csv(x.poids()),
                    x.poidsMax(), x.gainSecondes(), nombre(x.coutSecondes()), nombre(x.rentabilite())));
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
        StringBuilder k = new StringBuilder("rang,quoi,achats,cout_en_production_s,gain_estime_s,rentabilite\n");
        int rang = 0;
        for (Classement x : classement()) {
            k.append(String.format(Locale.ROOT, "%d,%s,%d,%.1f,%.1f,%s%n", ++rang, csv(x.quoi()), x.achats(), x.coutSecondes(), x.gainSecondes(),
                    nombre(x.rentabilite())));
        }
        Files.writeString(dossier.resolve("classement.csv"), k.toString());
        StringBuilder w = new StringBuilder("chrono,secondes,univers,depuis,vise,reel_s,une_fois_par_minute_s,toutes_les_dix_minutes_s\n");
        for (EtSi x : etsi) {
            w.append(String.format(Locale.ROOT, "%s,%.0f,%d,%s,%s,%s,%s,%s%n", chrono(x.t()), x.t(), x.univers(), csv(x.depuis()), csv(x.vise()),
                    nombre(x.reel()), nombre(x.parMinute()), nombre(x.parDixMinutes())));
        }
        Files.writeString(dossier.resolve("etsi.csv"), w.toString());
    }

    /** Un nombre pour le CSV : vide s'il est inconnu (NaN) ou infini, négatif s'il n'a pas été atteint. */
    private static String nombre(double value) {
        if (!Double.isFinite(value)) return "";
        return Math.abs(value) >= 100 ? String.format(Locale.ROOT, "%.0f", value) : String.format(Locale.ROOT, "%.4g", value);
    }
}

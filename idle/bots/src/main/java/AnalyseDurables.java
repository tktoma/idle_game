import idle.core.history.Noms;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ce que vaut chaque amélioration durable du premier univers (version 0.166), mesuré et non estimé. Le gain estimé par
 * achat ne regarde que la grandeur qui mène au palier suivant : il rate ce qu'une amélioration garde d'une fusion à
 * l'autre (la Persistance : 1 min 07 estimée, 4 h 47 mesurées en 0.164). Ici, le bot analyste joue le premier univers une
 * fois avec tout, puis une fois sans chacune ; l'écart à chaque grand palier est son apport.
 *
 * <p>Les améliorations durables : celles qui se paient en atomes, que la fusion ne remet pas à zéro.
 *
 * <p>Usage : {@code AnalyseDurables <sortie> [id,id…]} ; sans liste, les {@link #DURABLES}. Chaque partie s'écrit dans son
 * dossier (reference, sans-atom_keep…), sans les « et si » ; le résumé dans durables.txt et durables.csv.
 */
final class AnalyseDurables {

    private AnalyseDurables() {}

    /** Les améliorations payées en atomes : la fusion les garde. */
    static final List<String> DURABLES = List.of("atom_double", "atom_mass", "atom_patience", "atom_boost", "atom_discount", "atom_keep",
            "atom_overload", "atom_yield", "atom_bulk");

    /** Les grands paliers du premier univers, dans l'ordre. */
    static final List<String> PALIERS = List.of("Univers 1 : acte 1 : première explosion", "Univers 1 : acte 2 : premier Big Bang",
            "Univers 1 : galaxie", "Univers 1 formé (acte 3)");

    /** L'apport d'une amélioration : les moments de chaque palier sans elle (NaN : pas atteint). */
    record Apport(String id, double[] sans) {}

    static void ecrire(double[] reference, List<Apport> apports, Path sortie) throws Exception {
        StringBuilder txt = new StringBuilder("Ce que vaut chaque amélioration durable du premier univers (mesuré : une partie sans elle)\n\n");
        txt.append(String.format(Locale.ROOT, "%-24s", "Avec tout"));
        for (double r : reference) txt.append(String.format(Locale.ROOT, " %10s", AnalyseJournal.chrono(r)));
        txt.append("\n\n");
        StringBuilder csv = new StringBuilder("id,amelioration");
        for (int i = 0; i < PALIERS.size(); i++) csv.append(",sans_").append(i + 1).append("_s,apport_").append(i + 1).append("_s");
        csv.append('\n');
        for (Apport a : apports) {
            String nom = Noms.chose(a.id());
            txt.append(String.format(Locale.ROOT, "%-24s", nom));
            csv.append(a.id()).append(",\"").append(nom.replace("\"", "\"\"")).append('"');
            for (int i = 0; i < PALIERS.size(); i++) {
                double s = a.sans()[i], d = s - reference[i];
                txt.append(String.format(Locale.ROOT, " %10s", Double.isNaN(s) ? "pas atteint" : (d >= 0 ? "+" : "−") + AnalyseJournal.duree(Math.abs(d))));
                csv.append(String.format(Locale.ROOT, ",%s,%s", Double.isNaN(s) ? "" : String.format(Locale.ROOT, "%.0f", s),
                        Double.isNaN(s) ? "" : String.format(Locale.ROOT, "%.0f", d)));
            }
            txt.append('\n');
            csv.append('\n');
        }
        txt.append("\nColonnes : ").append(String.join(" ; ", PALIERS)).append(". Un écart positif : sans elle, le palier vient plus tard.\n");
        Files.createDirectories(sortie);
        Files.writeString(sortie.resolve("durables.txt"), txt.toString());
        Files.writeString(sortie.resolve("durables.csv"), csv.toString());
    }

    /** Les moments des grands paliers d'une partie jouée (NaN : pas atteint). */
    static double[] moments(Path dossier) throws Exception {
        Map<String, Double> paliers = AnalyseComparaison.paliers(dossier);
        double[] out = new double[PALIERS.size()];
        for (int i = 0; i < out.length; i++) out[i] = paliers.getOrDefault(PALIERS.get(i), Double.NaN);
        return out;
    }

    /** Une partie du premier univers, sans « et si », une estimation par heure. */
    static void jouer(Path dossier, String sans) throws Exception {
        BotAnalyste.main(new String[] {dossier.toString(), "1", "60", "201", "5", "60", "false", "false", "", sans});
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage : AnalyseDurables <sortie> [id,id…]");
            System.exit(2);
        }
        Path sortie = Path.of(args[0]);
        List<String> ids = args.length > 1 && !args[1].isBlank() ? List.copyOf(BotAnalyste.sans(args[1])) : DURABLES;
        Path ref = sortie.resolve("reference");
        if (!Files.exists(ref.resolve("paliers.csv"))) jouer(ref, "");
        double[] reference = moments(ref);
        List<Apport> apports = new ArrayList<>();
        for (String id : ids) {
            Path d = sortie.resolve("sans-" + id);
            if (!Files.exists(d.resolve("paliers.csv"))) jouer(d, id);
            apports.add(new Apport(id, moments(d)));
            // Le résumé s'écrit à chaque partie : une série interrompue garde ce qu'elle a mesuré.
            ecrire(reference, apports, sortie);
        }
        System.out.println("DURABLES," + apports.size() + " améliorations," + sortie.toAbsolutePath());
    }
}

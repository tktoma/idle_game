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
 *
 * <p>Version 0.168 : {@code AnalyseDurables <sortie> arbre} mesure de même les cases de l'arbre de matière noire les plus
 * achetées dans l'univers 1 ({@link #ARBRE}) ; {@code AnalyseDurables <sortie> bulles <sauvegarde> <univers> [bulle:NOM,…]}
 * les bulles de l'arbre ({@link #BULLES}), depuis une sauvegarde et jusqu'à l'univers donné, univers formé par univers formé.
 */
final class AnalyseDurables {

    private AnalyseDurables() {}

    /** Les améliorations payées en atomes : la fusion les garde. */
    static final List<String> DURABLES = List.of("atom_double", "atom_mass", "atom_patience", "atom_boost", "atom_discount", "atom_keep",
            "atom_overload", "atom_yield", "atom_bulk");

    /** Version 0.168 : les cases de l'arbre de matière noire les plus achetées dans l'univers 1 (Speedrunner 0.167). */
    static final List<String> ARBRE = List.of("dark_nucleus", "dark_singularity", "dark_seeds", "dark_priming", "dark_shop_cadence",
            "dark_shop_atoms", "dark_condensation", "dark_inflation");

    /** Version 0.168 : les bulles de l'arbre mesurées « sans », dans l'ordre de l'expert. */
    static final List<String> BULLES = List.of("bulle:LEAVES_LEFT", "bulle:LEAVES_RIGHT", "bulle:CROWN_RIGHT", "bulle:CROWN_MIDDLE",
            "bulle:YOUNG_RIGHT", "bulle:MATURE_RIGHT", "bulle:ANCIENT_RIGHT");

    /** Les grands paliers du premier univers, dans l'ordre. */
    static final List<String> PALIERS = List.of("Univers 1 : acte 1 : première explosion", "Univers 1 : acte 2 : premier Big Bang",
            "Univers 1 : galaxie", "Univers 1 formé (acte 3)");

    /** L'apport d'une amélioration : les moments de chaque palier sans elle (NaN : pas atteint). */
    record Apport(String id, double[] sans) {}

    static void ecrire(double[] reference, List<Apport> apports, Path sortie) throws Exception {
        ecrire(reference, apports, sortie, PALIERS);
    }

    static void ecrire(double[] reference, List<Apport> apports, Path sortie, List<String> paliers) throws Exception {
        StringBuilder txt = new StringBuilder(paliers == PALIERS ? "Ce que vaut chaque amélioration durable du premier univers (mesuré : une partie sans elle)\n\n"
                : "Ce que vaut chaque bulle de l'arbre (mesuré : une partie sans elle), univers formé par univers formé\n\n");
        txt.append(String.format(Locale.ROOT, "%-24s", "Avec tout"));
        for (double r : reference) txt.append(String.format(Locale.ROOT, " %10s", AnalyseJournal.chrono(r)));
        txt.append("\n\n");
        StringBuilder csv = new StringBuilder("id,amelioration");
        for (int i = 0; i < paliers.size(); i++) csv.append(",sans_").append(i + 1).append("_s,apport_").append(i + 1).append("_s");
        csv.append('\n');
        for (Apport a : apports) {
            String nom = BotAnalyste.nomSans(a.id());
            txt.append(String.format(Locale.ROOT, "%-24s", nom));
            csv.append(a.id()).append(",\"").append(nom.replace("\"", "\"\"")).append('"');
            for (int i = 0; i < paliers.size(); i++) {
                double s = a.sans()[i], d = s - reference[i];
                txt.append(String.format(Locale.ROOT, " %10s", Double.isNaN(s) ? "pas atteint" : (d >= 0 ? "+" : "−") + AnalyseJournal.duree(Math.abs(d))));
                csv.append(String.format(Locale.ROOT, ",%s,%s", Double.isNaN(s) ? "" : String.format(Locale.ROOT, "%.0f", s),
                        Double.isNaN(s) ? "" : String.format(Locale.ROOT, "%.0f", d)));
            }
            txt.append('\n');
            csv.append('\n');
        }
        txt.append("\nColonnes : ").append(String.join(" ; ", paliers)).append(". Un écart positif : sans elle, le palier vient plus tard.\n");
        Files.createDirectories(sortie);
        Files.writeString(sortie.resolve("durables.txt"), txt.toString());
        Files.writeString(sortie.resolve("durables.csv"), csv.toString());
    }

    /** Les moments des grands paliers d'une partie jouée (NaN : pas atteint). */
    static double[] moments(Path dossier) throws Exception {
        return moments(dossier, PALIERS);
    }

    static double[] moments(Path dossier, List<String> noms) throws Exception {
        Map<String, Double> paliers = AnalyseComparaison.paliers(dossier);
        double[] out = new double[noms.size()];
        for (int i = 0; i < out.length; i++) out[i] = paliers.getOrDefault(noms.get(i), Double.NaN);
        return out;
    }

    /** Les univers formés après la sauvegarde, jusqu'à l'univers {@code univers} : les paliers d'une mesure des bulles. */
    static List<String> universFormes(int depuis, int univers) {
        List<String> out = new ArrayList<>();
        for (int k = depuis + 1; k <= univers; k++) out.add("Univers " + k + " formé (acte 3)");
        return out;
    }

    /** Une partie depuis une sauvegarde jusqu'à l'univers {@code univers}, sans « et si », une estimation par heure. */
    static void jouer(Path dossier, String sans, String depart, int univers) throws Exception {
        BotAnalyste.main(new String[] {dossier.toString(), String.valueOf(univers), "400", "201", "5", "60", "false", "false", depart, sans});
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
        if (args.length > 3 && args[1].equals("bulles")) {
            bulles(sortie, args[2], Integer.parseInt(args[3]), args.length > 4 ? List.copyOf(BotAnalyste.sans(args[4])) : BULLES);
            return;
        }
        List<String> ids = args.length > 1 && args[1].equals("arbre") ? ARBRE
                : args.length > 1 && !args[1].isBlank() ? List.copyOf(BotAnalyste.sans(args[1])) : DURABLES;
        Path ref = sortie.resolve("reference");
        if (!Files.exists(ref.resolve("fin.save"))) jouer(ref, "");
        double[] reference = moments(ref);
        List<Apport> apports = new ArrayList<>();
        // Version 0.168 : une partie compte comme jouée une fois son fin.save écrit ; une partie coupée en cours (son
        // paliers.csv seul) se rejoue, au lieu de passer pour une amélioration dont le palier n'est « pas atteint ».
        for (String id : ids) {
            Path d = sortie.resolve("sans-" + id);
            if (!Files.exists(d.resolve("fin.save"))) jouer(d, id);
            apports.add(new Apport(id, moments(d)));
            // Le résumé s'écrit à chaque partie : une série interrompue garde ce qu'elle a mesuré.
            ecrire(reference, apports, sortie);
        }
        System.out.println("DURABLES," + apports.size() + " améliorations," + sortie.toAbsolutePath());
    }

    /** Les bulles : depuis la sauvegarde, une partie avec tout, puis une sans chacune, jusqu'à l'univers {@code univers}. */
    static void bulles(Path sortie, String depart, int univers, List<String> ids) throws Exception {
        int depuis = Bots.load(Path.of(depart), 201).universes();
        List<String> paliers = universFormes(depuis, univers);
        Path ref = sortie.resolve("reference");
        if (!Files.exists(ref.resolve("fin.save"))) jouer(ref, "", depart, univers);
        double[] reference = moments(ref, paliers);
        List<Apport> apports = new ArrayList<>();
        for (String id : ids) {
            Path d = sortie.resolve("sans-" + id.replace(':', '-'));
            if (!Files.exists(d.resolve("fin.save"))) jouer(d, id, depart, univers);
            apports.add(new Apport(id, moments(d, paliers)));
            ecrire(reference, apports, sortie, paliers);
        }
        System.out.println("BULLES," + apports.size() + " bulles," + sortie.toAbsolutePath());
    }
}

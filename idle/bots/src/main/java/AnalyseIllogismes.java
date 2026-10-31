import idle.core.history.Noms;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.upgrades.Upgrade;
import idle.core.upgrades.Upgrades;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Les détecteurs d'illogismes du bot analyste (version 0.164) : à la fin d'une partie, ils relisent ce que le bot a vu
 * et signalent ce qui semble ne pas tourner rond dans le jeu (ou dans la façon de jouer du bot). Chacun a un numéro
 * stable, une règle simple et des seuils écrits ici ; un signalement n'est pas une preuve, c'est un endroit où regarder.
 *
 * <ol>
 *   <li>un joueur moins présent va plus vite que le bot sur une étape (les « et si ») ;</li>
 *   <li>un achat jamais rentable : acheté souvent, il rend moins d'un dixième du temps de production qu'il coûte ;</li>
 *   <li>une amélioration du catalogue (ordinaire ou de l'arbre) que le bot n'achète jamais ;</li>
 *   <li>un défi trivial, réussi en moins d'une minute ;</li>
 *   <li>une rafale de succès, cinq ou plus au même instant ;</li>
 *   <li>une étape éclair (moins de 30 s) ou interminable (plus de 6 h) ;</li>
 *   <li>une estimation ratée : l'annonce se trompe de plus d'un quart du temps de l'étape, et d'au moins 5 min ;</li>
 *   <li>un automatisme que le bot coupe sans cesse (plus de 20 fois) ;</li>
 *   <li>une ressource gagnée, jamais dépensée par un achat ;</li>
 *   <li>un gros multiplicateur sans effet : trois achats ou plus à ×1 000 ou plus, et pas une seconde gagnée en tout.</li>
 * </ol>
 */
final class AnalyseIllogismes {

    private AnalyseIllogismes() {}

    /** À regarder de près, ou simplement à savoir. */
    enum Gravite {
        CREUSER("à creuser"), NOTER("à noter");

        final String label;

        Gravite(String label) {
            this.label = label;
        }
    }

    /** Un signalement : quel détecteur, sa gravité, ce qu'il a vu, et le détail. */
    record Illogisme(int numero, String detecteur, Gravite gravite, String titre, List<String> details) {}

    static final String[] NOMS = {"", "Joueur moins présent plus rapide", "Achat jamais rentable", "Amélioration jamais achetée",
        "Défi trivial", "Rafale de succès", "Étape éclair ou interminable", "Estimation ratée", "Automatisme en va-et-vient",
        "Ressource jamais dépensée", "Gros multiplicateur sans effet"};

    /** Tous les détecteurs, sur ce que le bot a vu. */
    static List<Illogisme> detecter(BotAnalyste bot) {
        AnalyseJournal j = bot.journal;
        List<Illogisme> out = new ArrayList<>();
        // 1. Les « et si » : un joueur moins présent plus rapide que le bot.
        for (AnalyseJournal.EtSi e : j.etsi) {
            if (!(e.reel() > 300)) continue;
            double best = Math.min(pos(e.parMinute()), pos(e.parDixMinutes()));
            if (best < 0.8 * e.reel()) {
                out.add(new Illogisme(1, NOMS[1], Gravite.CREUSER, e.depuis() + " → " + e.vise(), List.of(
                        "Le bot : " + AnalyseJournal.duree(e.reel()) + " ; une fois par minute : " + duree(e.parMinute())
                                + " ; toutes les dix minutes : " + duree(e.parDixMinutes()) + ".",
                        "Jouer moins souvent ne devrait pas aller plus vite : la façon de jouer du bot (ou une mécanique qui punit les "
                                + "achats fréquents) perd du temps ici.")));
            }
        }
        // 2. Les achats jamais rentables.
        for (AnalyseJournal.Classement c : j.classement()) {
            if (c.achats() >= 5 && Double.isFinite(c.rentabilite()) && c.rentabilite() < 0.1 && c.coutSecondes() >= 60) {
                out.add(new Illogisme(2, NOMS[2], Gravite.NOTER, c.quoi(), List.of(String.format(Locale.FRANCE,
                        "%d achats, %s de production pour %s gagnées vers les paliers suivants : ×%.3f.", c.achats(),
                        AnalyseJournal.duree(c.coutSecondes()), AnalyseJournal.duree(c.gainSecondes()), c.rentabilite()),
                        "Le gain est une estimation qui ne regarde que le palier suivant : l'achat sert peut-être plus loin.")));
            }
        }
        // 3. Les améliorations jamais achetées.
        Set<String> achetes = new HashSet<>();
        for (AnalyseJournal.Achat a : j.achats) achetes.add(a.chose());
        List<String> jamais = new ArrayList<>();
        for (Upgrade u : Upgrades.DEFAULT) if (!achetes.contains(u.name())) jamais.add("Amélioration — " + u.name());
        for (DarkUpgrade d : DarkUpgrades.DEFAULT) if (!achetes.contains(d.name())) jamais.add("Arbre de matière noire — " + d.name());
        for (String quoi : jamais) {
            out.add(new Illogisme(3, NOMS[3], Gravite.NOTER, quoi, List.of("Le bot ne l'a jamais achetée de toute la partie : "
                    + "inutile, trop chère, ou laissée de côté par un choix de branche ?")));
        }
        // 4. Les défis triviaux.
        Map<String, Integer> triviaux = new LinkedHashMap<>();
        Map<String, Double> plusLong = new LinkedHashMap<>();
        for (AnalyseJournal.Defi d : j.defis) {
            if (!d.reussi() || d.duree() >= 60) continue;
            triviaux.merge(d.nom(), 1, Integer::sum);
            plusLong.merge(d.nom(), d.duree(), Math::max);
        }
        triviaux.forEach((nom, fois) -> out.add(new Illogisme(4, NOMS[4], Gravite.CREUSER, nom, List.of(
                "Réussi " + fois + " fois en moins d'une minute (au plus " + AnalyseJournal.duree(plusLong.get(nom)) + ") : "
                        + "une contrainte qui ne contraint plus, pour un joueur aussi avancé."))));
        // 5. Les rafales de succès.
        Map<Long, Integer> parInstant = new LinkedHashMap<>();
        for (AnalyseJournal.Ligne l : j.lignes) {
            if (l.sorte() == AnalyseJournal.Sorte.SUCCES && l.titre().startsWith("Succès")) parInstant.merge(Math.round(l.t()), 1, Integer::sum);
        }
        parInstant.forEach((t, n) -> {
            if (n >= 5) out.add(new Illogisme(5, NOMS[5], Gravite.NOTER, n + " succès à " + AnalyseJournal.chrono(t), List.of(
                    "Obtenus au même instant : ils ne marquent pas des moments différents du jeu.")));
        });
        // 6. Les étapes éclair ou interminables (les paliers principaux seulement).
        for (AnalyseJournal.Palier p : j.paliers) {
            boolean principal = p.nom().contains("acte") || p.nom().contains("galaxie") || p.nom().contains("amas") || p.nom().contains("dernier Big Bang");
            if (!principal || p.univers() > 11) continue;
            if (p.depuis() < 30) {
                out.add(new Illogisme(6, NOMS[6], Gravite.NOTER, p.nom() + " en " + AnalyseJournal.duree(p.depuis()), List.of(
                        "Une étape franchie en moins de 30 secondes : elle ne se joue pas.")));
            } else if (p.depuis() > 6 * 3600) {
                out.add(new Illogisme(6, NOMS[6], Gravite.CREUSER, p.nom() + " en " + AnalyseJournal.duree(p.depuis()), List.of(
                        "Plus de six heures pour une étape, même en jouant sans faute.")));
            }
        }
        // 7. Les estimations ratées.
        for (AnalyseJournal.Palier p : j.paliers) {
            if (Double.isNaN(p.annonce()) || p.depuis() <= 0) continue;
            double ecart = Math.abs(p.t() - p.annonce());
            if (ecart >= 300 && ecart > 0.25 * p.depuis()) {
                out.add(new Illogisme(7, NOMS[7], Gravite.NOTER, p.nom(), List.of("Annoncé vers " + AnalyseJournal.chrono(p.annonce())
                        + ", atteint à " + AnalyseJournal.chrono(p.t()) + " : " + AnalyseJournal.duree(ecart) + " d'écart pour une étape de "
                        + AnalyseJournal.duree(p.depuis()) + ". La copie de la partie n'a pas joué comme le bot.")));
            }
        }
        // 8. Les automatismes en va-et-vient.
        bot.coupures.forEach((nom, fois) -> {
            if (fois > 20) out.add(new Illogisme(8, NOMS[8], Gravite.NOTER, nom + " : coupé " + fois + " fois", List.of(
                    "Le bot le coupe et le remet sans cesse : il gêne ce que le bot prépare (une réserve, un défi), "
                            + "ou il lui manque un réglage pour ne pas tout dépenser.")));
        });
        // 9. Les ressources jamais dépensées.
        Set<String> payees = new HashSet<>();
        for (AnalyseJournal.Achat a : j.achats) payees.add(a.paye());
        for (String key : bot.revenuLog.keySet()) {
            String nom = Noms.cle(key);
            if (!payees.contains(nom)) {
                out.add(new Illogisme(9, NOMS[9], Gravite.NOTER, nom, List.of("Le bot l'a gagnée, mais ne l'a jamais dépensée "
                        + "dans un achat qu'il a fait lui-même (un automatisme a pu la dépenser, ou elle sert seulement de condition).")));
            }
        }
        // 10. Les gros multiplicateurs sans effet : la chose, tous ses achats réunis, n'a jamais fait gagner une seconde.
        Map<String, Integer> gros = new LinkedHashMap<>();
        Map<String, Double> gagne = new LinkedHashMap<>();
        for (AnalyseJournal.Achat a : j.achats) {
            if (Double.isFinite(a.gainSecondes())) gagne.merge(a.quoi(), a.gainSecondes(), Double::sum);
            if (a.poidsMax() >= 3) gros.merge(a.quoi(), 1, Integer::sum);
        }
        gros.forEach((quoi, fois) -> {
            if (fois >= 3 && gagne.getOrDefault(quoi, 0.0) < 1) out.add(new Illogisme(10, NOMS[10], Gravite.NOTER, quoi, List.of(fois
                    + " achats qui multiplient une grandeur par mille ou plus, et, tous achats réunis, pas une seconde gagnée vers les "
                    + "paliers : la grandeur qu'ils multiplient ne mène à rien ici.")));
        });
        return out;
    }

    private static double pos(double seconds) {
        return seconds < 0 || Double.isNaN(seconds) ? Double.POSITIVE_INFINITY : seconds;
    }

    private static String duree(double seconds) {
        return seconds < 0 ? "pas atteint en 24 h" : AnalyseJournal.duree(seconds);
    }

    /** Les signalements en texte et en tableau : illogismes.txt et illogismes.csv. */
    static void ecrire(List<Illogisme> illogismes, Path dossier) throws Exception {
        StringBuilder txt = new StringBuilder("Illogismes repérés par le bot analyste (version 0.164) : " + illogismes.size() + "\n\n");
        StringBuilder csv = new StringBuilder("numero,detecteur,gravite,titre,detail\n");
        for (int n = 1; n < NOMS.length; n++) {
            final int numero = n;
            List<Illogisme> these = illogismes.stream().filter(i -> i.numero() == numero).toList();
            txt.append(n).append(". ").append(NOMS[n]).append(" : ").append(these.size()).append('\n');
            for (Illogisme i : these) {
                txt.append("   [").append(i.gravite().label).append("] ").append(i.titre()).append('\n');
                for (String d : i.details()) txt.append("      ").append(d).append('\n');
                csv.append(n).append(',').append(q(i.detecteur())).append(',').append(q(i.gravite().label)).append(',').append(q(i.titre()))
                        .append(',').append(q(String.join(" ", i.details()))).append('\n');
            }
            txt.append('\n');
        }
        Files.writeString(dossier.resolve("illogismes.txt"), txt.toString());
        Files.writeString(dossier.resolve("illogismes.csv"), csv.toString());
    }

    private static String q(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

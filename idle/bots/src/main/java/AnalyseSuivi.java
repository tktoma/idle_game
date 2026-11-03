import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.core.bigbang.BigBangCondition;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.sky.Cosmos;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ce que le bot analyste suit en continu (version 0.166), une fois par minute de jeu :
 * <ul>
 *   <li>les réserves qui dorment : une ressource plus de {@value #RESERVE_FACTEUR} fois au-dessus de ce qu'elle peut
 *       payer, pendant plus d'une heure (la matière noire, l'énergie du vide gardée pour les bulles) ;</li>
 *   <li>les paliers bloqués par une seule condition : le Big Bang, ou une échelle du cosmos, à qui il ne manque qu'une
 *       condition pendant plus d'une heure ;</li>
 *   <li>la dernière condition remplie de chaque Big Bang : ce qui l'a débloqué ;</li>
 *   <li>l'avancée des dimensions et de la graine, pour donner à leurs améliorations le même traitement qu'aux autres :
 *       le temps qu'elles font gagner vers le seuil de leur dimension (ou le palier du défi final) et vers le palier
 *       suivant de la graine, mesuré sur le rythme des cinq minutes d'avant et des cinq minutes d'après.</li>
 * </ul>
 */
final class AnalyseSuivi {

    /** Une réserve dort quand elle dépasse ce multiple de ce qu'elle peut payer. */
    static final double RESERVE_FACTEUR = 10;
    /** Pendant au moins ce temps de jeu, pour être signalée ; de même pour un palier bloqué par une seule condition. */
    static final double DUREE_SIGNAL = 3600;
    /** La fenêtre du rythme d'une dimension ou de la graine, avant et après un achat. */
    static final double FENETRE = 300;
    /** Un relevé par minute de jeu. */
    static final double RELEVE = 60;

    /** Un temps passé dans un état (une réserve qui dort, une seule condition qui manque) : de quand à quand, et pourquoi. */
    record Episode(int univers, String quoi, double debut, double fin, String detail) {
        double duree() {
            return fin - debut;
        }
    }

    /** Un achat de dimension ou de graine qui attend son rythme d'après. */
    private record Attente(int index, double t0, String cible, double valeur0, double reste, double rythmeAvant, int dimPaye,
            double montantLog, int lot) {}

    final List<Episode> reserves = new ArrayList<>();
    final List<Episode> uneCondition = new ArrayList<>();

    private final Map<String, double[]> enCours = new java.util.LinkedHashMap<>();
    private final Map<BigBangCondition, Double> rempliesDepuis = new EnumMap<>(BigBangCondition.class);
    /** Les relevés : temps, ressource gagnée de chaque dimension (1 à 10), arrosages de la graine. */
    private final ArrayDeque<double[]> releves = new ArrayDeque<>();
    private final List<Attente> attentes = new ArrayList<>();
    private double prochain = Double.NEGATIVE_INFINITY;

    /** À chaque pas : les conditions du Big Bang (pour la dernière remplie) ; une fois par minute, le reste. */
    void observe(Game g, double t, int univers, AnalyseJournal journal) {
        conditions(g, t);
        if (t < prochain) return;
        prochain = t + RELEVE;
        reserve(g, t, univers, "Matière noire", g.state().darkMatter().log10(), besoinMatiereNoire(g));
        reserve(g, t, univers, "Énergie du vide pour les bulles", Math.log10(Math.max(1e-300, g.voidAvailable())), besoinBulles(g));
        seuleCondition(g, t, univers);
        double[] row = new double[12];
        row[0] = t;
        for (int d = 1; d <= 10; d++) row[d] = g.dimensionEarned(d);
        row[11] = g.state().seedWaterings();
        releves.addLast(row);
        while (releves.size() > 2 && t - releves.peekFirst()[0] > 2 * FENETRE + RELEVE) releves.removeFirst();
        evaluer(g, t, journal);
    }

    // ------------------------------------------------------------------
    // Les réserves qui dorment
    // ------------------------------------------------------------------

    /** Ce que la matière noire peut payer : la réserve du Big Bang, ou la case de l'arbre la moins chère payée en matière noire. */
    static double besoinMatiereNoire(Game g) {
        double besoin = g.bigBangs() < Bots.LAST_BIG_BANG ? g.bigBangDarkMatter().log10() : Double.NEGATIVE_INFINITY;
        double moinsChere = Double.POSITIVE_INFINITY;
        for (DarkUpgrade d : DarkUpgrades.DEFAULT) {
            if (d.branch() != DarkUpgrade.Branch.DARK_MATTER || !g.isDarkAvailable(d.id()) || g.isDarkMaxed(d.id())) continue;
            moinsChere = Math.min(moinsChere, g.darkCostOf(d.id()).log10());
        }
        double plusHaut = Math.max(besoin, Double.isFinite(moinsChere) ? moinsChere : Double.NEGATIVE_INFINITY);
        return Double.isFinite(plusHaut) ? plusHaut : Double.NaN;
    }

    /** Ce que l'énergie du vide gardée peut payer : la bulle ouverte la moins chère ; NaN sans bulle ouverte à acheter. */
    static double besoinBulles(Game g) {
        double moinsChere = Double.POSITIVE_INFINITY;
        for (TreeBubble b : TreeBubble.values()) {
            if (g.isTreeBubbleOpen(b) && !g.isTreeBubbleMaxed(b)) moinsChere = Math.min(moinsChere, g.treeBubbleCost(b));
        }
        return Double.isFinite(moinsChere) && moinsChere > 0 ? Math.log10(moinsChere) : Double.NaN;
    }

    private void reserve(Game g, double t, int univers, String quoi, double avoirLog, double besoinLog) {
        boolean dort = Double.isFinite(avoirLog) && Double.isFinite(besoinLog) && avoirLog - besoinLog >= Math.log10(RESERVE_FACTEUR);
        String cle = "reserve|" + quoi;
        double[] e = enCours.get(cle);
        if (dort && e == null) enCours.put(cle, new double[] {t, avoirLog - besoinLog, univers});
        else if (dort) e[1] = Math.max(e[1], avoirLog - besoinLog);
        else if (e != null) {
            enCours.remove(cle);
            if (t - e[0] >= DUREE_SIGNAL) reserves.add(new Episode((int) e[2], quoi, e[0], t, "jusqu'à " + fois(e[1]) + " fois ce qu'elle peut payer"));
        }
    }

    // ------------------------------------------------------------------
    // Les paliers bloqués par une seule condition, et la dernière condition remplie
    // ------------------------------------------------------------------

    private void conditions(Game g, double t) {
        if (g.bigBangs() >= Bots.LAST_BIG_BANG) {
            rempliesDepuis.clear();
            return;
        }
        for (BigBangCondition c : g.bigBangConditions()) {
            if (g.isBigBangConditionMet(c)) rempliesDepuis.putIfAbsent(c, t);
            else rempliesDepuis.remove(c);
        }
    }

    /**
     * La dernière condition remplie du Big Bang qui vient d'être lancé, et le temps qu'elle a fait attendre seule ; à
     * appeler au palier du Big Bang. Vide si les conditions n'ont pas été suivies.
     */
    String derniereCondition(Game g) {
        if (rempliesDepuis.isEmpty()) return "";
        BigBangCondition derniere = null;
        double t = Double.NEGATIVE_INFINITY, avant = Double.NEGATIVE_INFINITY;
        for (Map.Entry<BigBangCondition, Double> e : rempliesDepuis.entrySet()) {
            if (e.getValue() > t) {
                avant = t;
                t = e.getValue();
                derniere = e.getKey();
            } else {
                avant = Math.max(avant, e.getValue());
            }
        }
        rempliesDepuis.clear();
        if (derniere == null) return "";
        return "dernière condition remplie : " + BotAnalyste.conditionNom(derniere)
                + (Double.isFinite(avant) ? " (seule à manquer pendant " + AnalyseJournal.duree(t - avant) + ")" : "");
    }

    private void seuleCondition(Game g, double t, int univers) {
        String bloque = null, detail = null;
        if (g.bigBangs() < Bots.LAST_BIG_BANG && !g.canBigBang()) {
            List<BigBangCondition> manque = new ArrayList<>();
            int n = 0;
            for (BigBangCondition c : g.bigBangConditions()) {
                n++;
                if (!g.isBigBangConditionMet(c)) manque.add(c);
            }
            if (n > 1 && manque.size() == 1) {
                bloque = "Big Bang n° " + (g.bigBangs() + 1);
                detail = "il ne manque que " + BotAnalyste.conditionNom(manque.get(0));
            }
        } else if (g.bigBangs() >= Bots.LAST_BIG_BANG && g.nextCosmos() != null) {
            Cosmos scale = g.nextCosmos();
            int n = g.cosmosConditions(scale), met = g.cosmosConditionsMet(scale);
            if (n > 1 && met == n - 1) {
                bloque = scale.label();
                // Version 0.168 : laquelle (quel astre, quelle matière), sans les nombres qui bougent à chaque pas.
                List<String> manque = g.cosmosMissing(scale);
                detail = "il ne manque qu'une condition sur " + n
                        + (manque.size() == 1 ? " : " + manque.get(0).replaceAll(" \\([^)]*\\)$", "") : "");
            }
        }
        String cle = "seule|" + bloque + "|" + detail;
        // Ferme ce qui ne tient plus.
        for (var it = enCours.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            if (!e.getKey().startsWith("seule|") || e.getKey().equals(cle)) continue;
            double[] v = e.getValue();
            String[] parts = e.getKey().split("\\|", 3);
            if (t - v[0] >= DUREE_SIGNAL) uneCondition.add(new Episode((int) v[2], parts[1], v[0], t, parts[2]));
            it.remove();
        }
        if (bloque != null) enCours.putIfAbsent(cle, new double[] {t, 0, univers});
    }

    /** « 20 », « 2 240 », « 10^6,3 » : un facteur lisible à partir de ses ordres de grandeur. */
    static String fois(double log10) {
        return log10 < 6 ? String.format(Locale.FRANCE, "%,.0f", Math.pow(10, log10)) : String.format(Locale.FRANCE, "10^%.1f", log10);
    }

    /** À la fin de la partie : ferme ce qui est encore en cours. */
    void fin(double t) {
        for (var e : enCours.entrySet()) {
            double[] v = e.getValue();
            if (t - v[0] < DUREE_SIGNAL) continue;
            String[] parts = e.getKey().split("\\|", 3);
            if (parts[0].equals("reserve")) reserves.add(new Episode((int) v[2], parts[1], v[0], t,
                    "jusqu'à " + fois(v[1]) + " fois ce qu'elle peut payer (encore à la fin)"));
            else uneCondition.add(new Episode((int) v[2], parts[1], v[0], t, parts[2] + " (encore à la fin)"));
        }
        enCours.clear();
    }

    // ------------------------------------------------------------------
    // Les améliorations des dimensions et de la graine
    // ------------------------------------------------------------------

    /** Les ressources des dimensions, dans l'ordre des dimensions (1 à 10), comme {@code Achats.monnaie} les nomme. */
    static final List<String> RESSOURCES = List.of("dimensionShards", "planeShards", "volumeShards", "durationShards", "loopShards",
            "braneShards", "clickerShards", "gridShards", "mirrorShards", "mixShards");

    /** La dimension (1 à 10) qu'une ressource paie, 0 si ce n'en est pas une. */
    static int dimension(String monnaie) {
        return monnaie == null ? 0 : RESSOURCES.indexOf(monnaie) + 1;
    }

    /**
     * Ce que vise un achat de dimension ou de graine : « graine », ou le numéro de la dimension ; null pour les autres
     * achats. Les améliorations de la graine se paient dans la ressource d'une dimension : c'est leur groupe qui les
     * distingue.
     */
    static String cible(String groupe, String monnaie) {
        if (groupe.startsWith("Graine")) return "graine";
        int d = dimension(monnaie);
        return d > 0 ? String.valueOf(d) : null;
    }

    /** Le nom du palier que vise une amélioration de dimension ou de graine, ou "" s'il n'y en a plus. */
    static String palierVise(Game g, String cible) {
        if (cible.equals("graine")) {
            int stage = g.seedStage();
            return stage < Game.SEED_STAGES ? "Graine : palier " + (stage + 1) : "";
        }
        int d = Integer.parseInt(cible);
        if (!g.isDimensionThresholdReached(d)) return "Seuil de la dimension " + d + " atteint";
        return g.isFinalChallengeOpen() ? "Défi final : la dimension " + d : "";
    }

    /** Où en est la cible, et ce qu'il lui faut pour son palier (NaN : plus de palier). */
    static double[] avancee(Game g, String cible) {
        if (cible.equals("graine")) {
            int stage = g.seedStage();
            double besoin = stage < Game.SEED_STAGES ? Game.seedStageWaterings(stage + 1) : Double.NaN;
            return new double[] {g.state().seedWaterings(), besoin};
        }
        int d = Integer.parseInt(cible);
        double besoin = !g.isDimensionThresholdReached(d) ? seuil(d) : g.isFinalChallengeOpen() ? g.finalGoalFor(d) : Double.NaN;
        return new double[] {g.dimensionEarned(d), besoin};
    }

    /** Le seuil de la dimension {@code d}, en ressource gagnée. */
    static double seuil(int d) {
        return switch (d) {
            case 1 -> Game.LINE_THRESHOLD;
            case 2 -> Game.PLANE_THRESHOLD;
            case 3 -> Game.VOLUME_THRESHOLD;
            case 4 -> Game.DURATION_THRESHOLD;
            case 5 -> Game.LOOP_THRESHOLD;
            case 6 -> Game.BRANE_THRESHOLD;
            case 7 -> Game.CLICKER_THRESHOLD;
            case 8 -> Game.GRID_THRESHOLD;
            case 9 -> Game.MIRROR_THRESHOLD;
            default -> Game.MIX_THRESHOLD;
        };
    }

    /** Le rythme de la cible (par seconde) sur les {@value #FENETRE} secondes avant {@code t}, d'après les relevés ; NaN sans. */
    private double rythmeAvant(String cible, double t, double valeur) {
        int col = cible.equals("graine") ? 11 : Integer.parseInt(cible);
        double[] debut = null;
        for (double[] r : releves) {
            if (t - r[0] <= FENETRE + 1e-6) {
                debut = r;
                break;
            }
        }
        if (debut == null || t - debut[0] < RELEVE - 1e-6) return Double.NaN;
        return (valeur - debut[col]) / (t - debut[0]);
    }

    /**
     * Note un achat de dimension ou de graine (son indice dans {@code journal.achats}) : son gain se mesurera
     * {@value #FENETRE} secondes plus tard, sur le rythme d'après. Rend vrai s'il est suivi.
     */
    boolean achat(Game g, double t, int index, String groupe, String monnaie, double montantLog, int lot) {
        String cible = cible(groupe, monnaie);
        if (cible == null) return false;
        double[] a = avancee(g, cible);
        if (Double.isNaN(a[1])) return false;
        double avant = rythmeAvant(cible, t, a[0]);
        attentes.add(new Attente(index, t, cible, a[0], Math.max(0, a[1] - a[0]), avant, dimension(monnaie), montantLog, lot));
        return true;
    }

    private void evaluer(Game g, double t, AnalyseJournal journal) {
        for (var it = attentes.iterator(); it.hasNext();) {
            Attente w = it.next();
            if (t - w.t0() < FENETRE - 1e-6) continue;
            it.remove();
            double valeur = avancee(g, w.cible())[0];
            if (valeur < w.valeur0()) continue; // une remise à zéro entre-temps : rien à mesurer
            double apres = (valeur - w.valeur0()) / (t - w.t0());
            double gain = Double.NaN;
            if (w.reste() <= 0) gain = 0;
            else if (w.rythmeAvant() > 0 && apres > 0) gain = Math.max(0, w.reste() / w.rythmeAvant() - w.reste() / apres) / w.lot();
            // Le coût en temps de production : le prix, au rythme de la dimension qui paie, avant l'achat.
            double cout = Double.NaN;
            if (w.dimPaye() > 0 && Double.isFinite(w.montantLog())) {
                double rythmePaye = rythmeAvant(String.valueOf(w.dimPaye()), w.t0(), valeurAu(w.dimPaye(), w.t0()));
                if (rythmePaye > 0) cout = Math.pow(10, w.montantLog()) / rythmePaye;
            }
            if (w.index() >= journal.achats.size()) continue;
            AnalyseJournal.Achat old = journal.achats.get(w.index());
            double rentabilite = Double.isFinite(cout) && cout > 0 && Double.isFinite(gain) ? gain / cout : Double.NaN;
            journal.achats.set(w.index(), new AnalyseJournal.Achat(old.t(), old.univers(), old.groupe(), old.chose(), old.quantite(), old.paye(),
                    old.montant(), old.part(), old.montantLog(), old.poids(), old.poidsMax(), Double.isFinite(gain) ? gain : 0, cout, rentabilite));
        }
    }

    /** La ressource gagnée de la dimension {@code d} au relevé le plus proche de {@code t}. */
    private double valeurAu(int d, double t) {
        double[] best = null;
        for (double[] r : releves) if (best == null || Math.abs(r[0] - t) < Math.abs(best[0] - t)) best = r;
        return best == null ? Double.NaN : best[d];
    }
}

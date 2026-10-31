package idle.core.history;

import idle.core.Game;
import idle.core.Multipliers;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * L'historique des achats du joueur (version 0.165), comme le bot analyste les note : quoi, combien, payé en quoi et
 * combien, le poids sur chaque grandeur, et le temps que l'achat fait gagner vers la prochaine étape.
 *
 * <p>L'interface prend une photo de la partie juste avant une action du joueur ({@link #before}), et une autre juste
 * avant le pas de temps suivant ({@link #after}) : entre les deux, le temps ne passe pas, la différence est exactement
 * ce que le joueur a fait. Ce que les automatismes achètent pendant le temps de jeu n'y est pas. Le temps gagné vient du
 * compteur « prochaine étape dans… » ({@link Pace}) et de ce que l'achat multiplie la grandeur qui y mène :
 * reste × (1 − 10^−Δ), une estimation haute. L'historique vit le temps de la session : il ne s'écrit pas dans la
 * sauvegarde.
 */
public final class PurchaseHistory {

    /** Le nombre d'achats gardés : les plus anciens s'en vont. */
    public static final int KEPT = 500;

    /**
     * Un achat : quand (secondes de jeu), quoi, combien de niveaux, payé en quoi et combien, la part de la réserve (NaN si
     * inconnue), le poids sur les grandeurs en mots et le plus grand en ordres de grandeur, le temps gagné estimé (NaN sans
     * estimation) et l'étape qu'il rapproche.
     */
    public record Entry(double t, String quoi, double quantite, String paye, String montant, double part, String poids, double poidsMax,
            double gainSecondes, String vers) {}

    /** Une chose achetée, sur toute la session : combien de fois, et le temps gagné en tout. */
    public record Total(String quoi, int achats, double gainSecondes) {}

    private final ArrayDeque<Entry> entries = new ArrayDeque<>();
    private final Map<String, double[]> totals = new LinkedHashMap<>();
    private Photo avant;
    private Map<Multipliers.Quantity, Double> grandeursAvant;

    /** Vrai entre {@link #before} et {@link #after} : une photo attend sa comparaison. */
    public boolean isOpen() {
        return avant != null;
    }

    /** Juste avant une action du joueur : la photo d'avant, une seule jusqu'à {@link #after}. */
    public void before(Game game) {
        if (avant != null) return;
        avant = Photo.of(game.state());
        grandeursAvant = grandeurs(game);
    }

    /** Juste avant le pas de temps suivant : ce que le joueur a acheté depuis {@link #before} ; rien sans photo d'avant. */
    public List<Entry> after(Game game, Pace pace) {
        if (avant == null) return List.of();
        Photo apres = Photo.of(game.state());
        List<Achats.Achat> achats = Achats.lire(Photo.diff(avant, apres));
        Map<Multipliers.Quantity, Double> before = grandeursAvant;
        avant = null;
        grandeursAvant = null;
        if (achats.isEmpty()) return List.of();
        Map<Multipliers.Quantity, Double> now = grandeurs(game);
        StringBuilder poids = new StringBuilder();
        double poidsMax = 0;
        for (Map.Entry<Multipliers.Quantity, Double> e : now.entrySet()) {
            Double b = before.get(e.getKey());
            if (b == null) continue;
            double delta = e.getValue() - b;
            if (Math.abs(delta) < 1e-6) continue;
            if (poids.length() > 0) poids.append(" ; ");
            poids.append(e.getKey().label()).append(" ×").append(facteur(delta));
            poidsMax = Math.max(poidsMax, delta);
        }
        Multipliers.Quantity engine = Pace.engine(pace.step());
        double delta = before.containsKey(engine) && now.containsKey(engine) ? now.get(engine) - before.get(engine) : 0;
        double reste = pace.secondsLeft();
        double gain = Double.isNaN(reste) ? Double.NaN : delta <= 0 ? 0 : reste * (1 - Math.pow(10, -delta));
        int lot = achats.size();
        String vers = pace.step() == null ? "" : pace.step().label();
        List<Entry> added = new ArrayList<>();
        for (Achats.Achat a : achats) {
            String texte = (lot > 1 ? "(lot de " + lot + " achats) " : "") + (poids.length() == 0 ? "aucun effet mesuré sur les grandeurs" : poids);
            Entry entry = new Entry(game.state().timePlayed(), a.quoi(), a.quantite(), a.paye(), a.montant(), a.part(), texte, poidsMax / lot,
                    gain / lot, vers);
            added.add(entry);
            entries.addLast(entry);
            if (entries.size() > KEPT) entries.removeFirst();
            double[] sum = totals.computeIfAbsent(a.quoi(), k -> new double[2]);
            sum[0]++;
            if (Double.isFinite(entry.gainSecondes())) sum[1] += entry.gainSecondes();
        }
        return added;
    }

    /** Les derniers achats, du plus récent au plus ancien. */
    public List<Entry> recent() {
        List<Entry> out = new ArrayList<>(entries);
        java.util.Collections.reverse(out);
        return out;
    }

    /** Chaque chose achetée pendant la session, celle qui a fait gagner le plus de temps d'abord. */
    public List<Total> totals() {
        List<Total> out = new ArrayList<>();
        totals.forEach((quoi, v) -> out.add(new Total(quoi, (int) v[0], v[1])));
        out.sort((x, y) -> x.gainSecondes() != y.gainSecondes() ? Double.compare(y.gainSecondes(), x.gainSecondes())
                : Integer.compare(y.achats(), x.achats()));
        return out;
    }

    /** Oublie tout : une nouvelle session, ou le joueur qui vide l'historique. */
    public void clear() {
        entries.clear();
        totals.clear();
        avant = null;
        grandeursAvant = null;
    }

    private static Map<Multipliers.Quantity, Double> grandeurs(Game game) {
        Map<Multipliers.Quantity, Double> out = new EnumMap<>(Multipliers.Quantity.class);
        for (Multipliers.Quantity q : Multipliers.Quantity.values()) {
            if (!Multipliers.isAvailable(game, q)) continue;
            double v = Multipliers.total(game, q);
            if (Double.isFinite(v)) out.put(q, v);
        }
        return out;
    }

    /** « 1,250 » ou « 10^4.20 » : un facteur, écrit en clair ou en puissance de dix. */
    public static String facteur(double log10) {
        if (Math.abs(log10) < 3) return String.format(Locale.FRANCE, "%.3f", Math.pow(10, log10));
        return String.format(Locale.ROOT, "10^%.2f", log10);
    }
}

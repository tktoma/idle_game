import idle.core.save.SaveData;
import idle.core.GameState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Une photo de la partie pour le bot analyste (version 0.161) : la sauvegarde, clé par clé, lue en nombres (gardés en
 * ordres de grandeur, pour les très grands), en tables « nom:valeur », en ensembles de noms ou en texte. Deux photos
 * prises juste avant et juste après les décisions d'un pas, sans que le temps passe entre elles, disent exactement ce
 * que le joueur a fait : ce qui est monté (achats, niveaux, choses obtenues) et ce qui a baissé (ce qu'il a payé).
 */
final class AnalyseEtat {

    /** Un nombre, gardé en signe et en ordres de grandeur : 4,2e27316531 se compare encore. */
    record Nombre(int signe, double log10) {
        static final Nombre ZERO = new Nombre(0, Double.NEGATIVE_INFINITY);

        static Nombre of(double value) {
            if (value == 0 || Double.isNaN(value)) return ZERO;
            return new Nombre(value > 0 ? 1 : -1, Math.log10(Math.abs(value)));
        }

        /** Lit « 12 », « 3.5E7 » ou « 4.18e27316531 » ; {@code null} si ce n'est pas un nombre. */
        static Nombre parse(String text) {
            String t = text.trim();
            if (t.isEmpty()) return null;
            int e = Math.max(t.indexOf('e'), t.indexOf('E'));
            try {
                if (e < 0) return of(Double.parseDouble(t));
                double mantissa = Double.parseDouble(t.substring(0, e));
                double exponent = Double.parseDouble(t.substring(e + 1));
                if (mantissa == 0) return ZERO;
                return new Nombre(mantissa > 0 ? 1 : -1, Math.log10(Math.abs(mantissa)) + exponent);
            } catch (NumberFormatException notNumber) {
                return null;
            }
        }

        double toDouble() {
            return signe == 0 ? 0 : signe * (log10 > 308 ? Double.POSITIVE_INFINITY : Math.pow(10, log10));
        }

        boolean greater(Nombre other) {
            return compareTo(other) > 0;
        }

        int compareTo(Nombre other) {
            if (signe != other.signe) return Integer.compare(signe, other.signe);
            if (signe == 0) return 0;
            int c = Double.compare(log10, other.log10);
            return signe > 0 ? c : -c;
        }

        /** La différence this − other, pour deux nombres positifs, this le plus grand. */
        Nombre minus(Nombre other) {
            if (other.signe <= 0) return this;
            if (signe <= 0) return ZERO;
            double gap = other.log10 - log10;
            if (gap >= 0) return ZERO;
            double rest = 1 - Math.pow(10, gap);
            return rest <= 0 ? ZERO : new Nombre(1, log10 + Math.log10(rest));
        }

        /** Écrit pour un humain : 1 234, 3,5 M, 4,2e27316531. */
        String texte() {
            if (signe == 0) return "0";
            String sign = signe < 0 ? "−" : "";
            if (log10 < 6) {
                double v = Math.pow(10, log10);
                if (Math.abs(v - Math.rint(v)) < 1e-6 * Math.max(1, v)) v = Math.rint(v);
                return sign + (v >= 100 || v == Math.rint(v) ? String.format(Locale.FRANCE, "%,.0f", v) : String.format(Locale.FRANCE, "%.2f", v));
            }
            double exponent = Math.floor(log10);
            return sign + String.format(Locale.ROOT, "%.2fe%.0f", Math.pow(10, log10 - exponent), exponent);
        }
    }

    /** Ce qui a changé entre deux photos, pour une clé (et une sous-clé pour les tables). */
    record Changement(String cle, String sousCle, Nombre avant, Nombre apres, String ajoute, String retire) {
        boolean monte() {
            return avant != null && apres != null && apres.greater(avant);
        }

        boolean baisse() {
            return avant != null && apres != null && avant.greater(apres);
        }
    }

    /** Les clés qui bougent sans que le joueur y soit pour rien : le temps, les statistiques, le ciel. */
    private static final Set<String> IGNOREES = Set.of("savedAt", "timePlayed", "timeSinceFusion", "upgradeCalm", "bangCalm",
            "cometWait", "skyEventWait", "automationTimers", "darkAutomationTimers", "linePhase", "colliderPhase");

    final Map<String, String> brut = new LinkedHashMap<>();

    private AnalyseEtat() {}

    static AnalyseEtat of(GameState state) {
        AnalyseEtat etat = new AnalyseEtat();
        // Version 0.162 : la sauvegarde sans les statistiques, lue clé par clé sans passer par le texte : deux fois
        // plus rapide (l'historique des statistiques était la première dépense de la partie analysée).
        SaveData data = new SaveData();
        state.save(data, false);
        for (String key : data.keys("")) {
            if (key.startsWith("stats.") || key.startsWith("found.") || IGNOREES.contains(key)) continue;
            etat.brut.put(key, data.text(key, ""));
        }
        return etat;
    }

    /** Une valeur lue en table « nom:nombre » : {@code null} si ce n'en est pas une. */
    private static Map<String, Nombre> table(String value) {
        if (!value.contains(":")) return null;
        Map<String, Nombre> map = new LinkedHashMap<>();
        for (String part : value.split(",")) {
            if (part.isEmpty()) continue;
            int colon = part.lastIndexOf(':');
            if (colon <= 0) return null;
            Nombre n = Nombre.parse(part.substring(colon + 1));
            if (n == null) return null;
            map.put(part.substring(0, colon), n);
        }
        return map;
    }

    private static Set<String> ensemble(String value) {
        Set<String> set = new LinkedHashSet<>();
        for (String part : value.split(",")) if (!part.isBlank()) set.add(part);
        return set;
    }

    /** Ce qui a changé d'une photo à l'autre. */
    static List<Changement> diff(AnalyseEtat avant, AnalyseEtat apres) {
        List<Changement> out = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>(avant.brut.keySet());
        keys.addAll(apres.brut.keySet());
        for (String key : keys) {
            String a = avant.brut.getOrDefault(key, ""), b = apres.brut.getOrDefault(key, "");
            if (a.equals(b)) continue;
            Nombre na = Nombre.parse(a), nb = Nombre.parse(b);
            if (na != null && nb != null) {
                out.add(new Changement(key, null, na, nb, null, null));
                continue;
            }
            Map<String, Nombre> ta = a.isEmpty() ? new LinkedHashMap<>() : table(a), tb = b.isEmpty() ? new LinkedHashMap<>() : table(b);
            if (ta != null && tb != null) {
                Set<String> sub = new LinkedHashSet<>(ta.keySet());
                sub.addAll(tb.keySet());
                for (String s : sub) {
                    Nombre x = ta.getOrDefault(s, Nombre.ZERO), y = tb.getOrDefault(s, Nombre.ZERO);
                    if (x.compareTo(y) != 0) out.add(new Changement(key, s, x, y, null, null));
                }
                continue;
            }
            Set<String> sa = ensemble(a), sb = ensemble(b);
            for (String s : sb) if (!sa.contains(s)) out.add(new Changement(key, s, null, null, s, null));
            for (String s : sa) if (!sb.contains(s)) out.add(new Changement(key, s, null, null, null, s));
        }
        return out;
    }
}

package idle.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le contenu d'une sauvegarde, avant d'être écrit ou après avoir été relu : des lignes
 * {@code clé=valeur}, dans l'ordre où elles ont été posées.
 *
 * <p>Chaque classe qui porte une part de la partie ({@link GameState}, {@link GameStats},
 * {@link StatsHistory}) y range ses propres champs et les y relit : les champs restent privés,
 * et un champ ajouté plus tard s'écrit à côté de sa déclaration.
 *
 * <p>Deux règles rendent une sauvegarde lisible d'une version du jeu à l'autre :
 * <ul>
 *   <li>une clé absente prend la valeur par défaut donnée à la lecture : une vieille sauvegarde
 *       se relit dans un jeu qui a gagné des champs ;</li>
 *   <li>une clé inconnue est ignorée : rien ne casse si un champ a disparu.</li>
 * </ul>
 *
 * <p>Les listes séparent leurs éléments par des virgules, les tables écrivent {@code clé:valeur}.
 * Les caractères qui servent de séparateurs sont échappés ({@code %2C} pour une virgule), si
 * bien qu'un identifiant peut contenir n'importe quoi.
 */
final class SaveData {

    private final Map<String, String> values = new LinkedHashMap<>();

    // ------------------------------------------------------------------
    // Écriture
    // ------------------------------------------------------------------

    /** Pose une valeur telle quelle. Une valeur {@code null} n'est pas écrite : la clé restera absente. */
    void put(String key, String value) {
        if (value != null) values.put(key, value);
    }

    void put(String key, boolean value) {
        values.put(key, value ? "1" : "0");
    }

    void put(String key, long value) {
        values.put(key, Long.toString(value));
    }

    void put(String key, double value) {
        values.put(key, number(value));
    }

    void put(String key, BigNum value) {
        values.put(key, value.toString());
    }

    /** Une liste d'identifiants, dans son ordre. */
    void putList(String key, Collection<String> items) {
        StringBuilder text = new StringBuilder();
        boolean first = true;
        for (String item : items) {
            if (!first) text.append(',');
            first = false;
            text.append(escape(item));
        }
        values.put(key, text.toString());
    }

    /** Une table identifiant → nombre entier. */
    void putInts(String key, Map<String, Integer> map) {
        StringBuilder text = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (!first) text.append(',');
            first = false;
            text.append(escape(entry.getKey())).append(':').append(entry.getValue());
        }
        values.put(key, text.toString());
    }

    /** Une table identifiant → nombre à virgule. */
    void putDoubles(String key, Map<String, Double> map) {
        StringBuilder text = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            if (!first) text.append(',');
            first = false;
            text.append(escape(entry.getKey())).append(':').append(number(entry.getValue()));
        }
        values.put(key, text.toString());
    }

    /** Une suite de nombres à virgule, dans son ordre. */
    void putDoubleList(String key, List<Double> list) {
        StringBuilder text = new StringBuilder();
        boolean first = true;
        for (double value : list) {
            if (!first) text.append(';');
            first = false;
            text.append(number(value));
        }
        values.put(key, text.toString());
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    boolean has(String key) {
        return values.containsKey(key);
    }

    String text(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    boolean flag(String key, boolean fallback) {
        String value = values.get(key);
        return value == null ? fallback : value.equals("1");
    }

    int whole(String key, int fallback) {
        String value = values.get(key);
        return value == null ? fallback : Integer.parseInt(value);
    }

    long count(String key, long fallback) {
        String value = values.get(key);
        return value == null ? fallback : Long.parseLong(value);
    }

    double real(String key, double fallback) {
        String value = values.get(key);
        return value == null ? fallback : parse(value);
    }

    BigNum big(String key, BigNum fallback) {
        String value = values.get(key);
        return value == null ? fallback : BigNum.parse(value);
    }

    List<String> list(String key) {
        List<String> items = new ArrayList<>();
        String value = values.get(key);
        if (value == null || value.isEmpty()) return items;
        for (String item : value.split(",", -1)) items.add(unescape(item));
        return items;
    }

    Map<String, Integer> ints(String key) {
        Map<String, Integer> map = new LinkedHashMap<>();
        String value = values.get(key);
        if (value == null || value.isEmpty()) return map;
        for (String entry : value.split(",", -1)) {
            int colon = entry.lastIndexOf(':');
            map.put(unescape(entry.substring(0, colon)), Integer.parseInt(entry.substring(colon + 1)));
        }
        return map;
    }

    Map<String, Double> doubles(String key) {
        Map<String, Double> map = new LinkedHashMap<>();
        String value = values.get(key);
        if (value == null || value.isEmpty()) return map;
        for (String entry : value.split(",", -1)) {
            int colon = entry.lastIndexOf(':');
            map.put(unescape(entry.substring(0, colon)), parse(entry.substring(colon + 1)));
        }
        return map;
    }

    List<Double> doubleList(String key) {
        List<Double> list = new ArrayList<>();
        String value = values.get(key);
        if (value == null || value.isEmpty()) return list;
        for (String item : value.split(";", -1)) list.add(parse(item));
        return list;
    }

    /** Les clés qui commencent par ce préfixe, dans l'ordre où elles ont été posées. */
    List<String> keys(String prefix) {
        List<String> keys = new ArrayList<>();
        for (String key : values.keySet()) {
            if (key.startsWith(prefix)) keys.add(key);
        }
        return keys;
    }

    // ------------------------------------------------------------------
    // Texte
    // ------------------------------------------------------------------

    /** Toutes les lignes {@code clé=valeur}, une par ligne. */
    void writeTo(StringBuilder text) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            text.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
    }

    /** Range une ligne {@code clé=valeur} relue ; une ligne sans {@code =} est ignorée. */
    void readLine(String line) {
        int equals = line.indexOf('=');
        if (equals > 0) values.put(line.substring(0, equals), line.substring(equals + 1));
    }

    /**
     * Un nombre à virgule, sans perte : {@code 12} pour 12.0, le texte de {@link Double#toString}
     * sinon. Une valeur sans sens ({@code NaN}) s'écrit par un texte vide.
     */
    static String number(double value) {
        if (Double.isNaN(value)) return "";
        if (value == Math.rint(value) && Math.abs(value) < 1e15 && !(value == 0 && 1 / value < 0)) return Long.toString((long) value);
        return Double.toString(value);
    }

    /** Relit ce qu'a écrit {@link #number(double)}. */
    static double parse(String text) {
        return text.isEmpty() ? Double.NaN : Double.parseDouble(text);
    }

    /** Remplace les caractères qui servent de séparateurs par {@code %XX}. */
    static String escape(String text) {
        StringBuilder escaped = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean special = c == '%' || c == ',' || c == ':' || c == ';' || c == '=' || c == '|' || c == '*' || c == '\n' || c == '\r';
            if (special && escaped == null) escaped = new StringBuilder(text.length() + 8).append(text, 0, i);
            if (escaped == null) continue;
            if (special) escaped.append('%').append(String.format("%02X", (int) c));
            else escaped.append(c);
        }
        return escaped == null ? text : escaped.toString();
    }

    static String unescape(String text) {
        if (text.indexOf('%') < 0) return text;
        StringBuilder plain = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '%' && i + 2 < text.length()) {
                plain.append((char) Integer.parseInt(text.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                plain.append(c);
            }
        }
        return plain.toString();
    }
}

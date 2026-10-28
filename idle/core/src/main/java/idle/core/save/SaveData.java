package idle.core.save;

import idle.core.math.BigNum;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * Le contenu d'une sauvegarde, avant d'être écrit ou après avoir été relu : des lignes
 * {@code clé=valeur}, dans l'ordre où elles ont été posées.
 *
 * <p>Chaque classe qui porte une part de la partie ({@link idle.core.GameState}, {@link idle.core.GameStats},
 * {@link idle.core.StatsHistory}) y range ses propres champs et les y relit : les champs restent privés,
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
public final class SaveData {

    private final Map<String, String> values = new LinkedHashMap<>();

    // ------------------------------------------------------------------
    // Écriture
    // ------------------------------------------------------------------

    /** Pose une valeur telle quelle. Une valeur {@code null} n'est pas écrite : la clé restera absente. */
    public void put(String key, String value) {
        if (value != null) values.put(key, value);
    }

    public void put(String key, boolean value) {
        values.put(key, value ? "1" : "0");
    }

    public void put(String key, long value) {
        values.put(key, Long.toString(value));
    }

    public void put(String key, double value) {
        values.put(key, number(value));
    }

    public void put(String key, BigNum value) {
        values.put(key, value.toString());
    }

    /** Une liste d'identifiants, dans son ordre. */
    public void putList(String key, Collection<String> items) {
        putJoined(key, ",", items, SaveData::escape);
    }

    /** Une table identifiant → nombre entier. */
    public void putInts(String key, Map<String, Integer> map) {
        putJoined(key, ",", map.entrySet(), entry -> escape(entry.getKey()) + ":" + entry.getValue());
    }

    /** Une table identifiant → nombre à virgule. */
    public void putDoubles(String key, Map<String, Double> map) {
        putJoined(key, ",", map.entrySet(), entry -> escape(entry.getKey()) + ":" + number(entry.getValue()));
    }

    /** Une suite de nombres à virgule, dans son ordre. */
    public void putDoubleList(String key, List<Double> list) {
        putJoined(key, ";", list, SaveData::number);
    }

    /** Pose les éléments écrits par {@code toText}, séparés par {@code separator}. */
    private <T> void putJoined(String key, String separator, Iterable<T> items, Function<T, String> toText) {
        StringJoiner text = new StringJoiner(separator);
        for (T item : items) text.add(toText.apply(item));
        values.put(key, text.toString());
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public String text(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public boolean flag(String key, boolean fallback) {
        String value = values.get(key);
        return value == null ? fallback : value.equals("1");
    }

    public int whole(String key, int fallback) {
        String value = values.get(key);
        return value == null ? fallback : Integer.parseInt(value);
    }

    public long count(String key, long fallback) {
        String value = values.get(key);
        return value == null ? fallback : Long.parseLong(value);
    }

    public double real(String key, double fallback) {
        String value = values.get(key);
        return value == null ? fallback : parse(value);
    }

    public BigNum big(String key, BigNum fallback) {
        String value = values.get(key);
        return value == null ? fallback : BigNum.parse(value);
    }

    public List<String> list(String key) {
        List<String> items = new ArrayList<>();
        String value = values.get(key);
        if (value == null || value.isEmpty()) return items;
        for (String item : value.split(",", -1)) items.add(unescape(item));
        return items;
    }

    public Map<String, Integer> ints(String key) {
        return table(key, Integer::parseInt);
    }

    public Map<String, Double> doubles(String key) {
        return table(key, SaveData::parse);
    }

    /** Relit une table {@code clé:valeur} dont {@code parseValue} lit les valeurs ; vide si la clé est absente. */
    private <V> Map<String, V> table(String key, Function<String, V> parseValue) {
        Map<String, V> map = new LinkedHashMap<>();
        String value = values.get(key);
        if (value == null || value.isEmpty()) return map;
        for (String entry : value.split(",", -1)) {
            int colon = entry.lastIndexOf(':');
            map.put(unescape(entry.substring(0, colon)), parseValue.apply(entry.substring(colon + 1)));
        }
        return map;
    }

    public List<Double> doubleList(String key) {
        List<Double> list = new ArrayList<>();
        String value = values.get(key);
        if (value == null || value.isEmpty()) return list;
        for (String item : value.split(";", -1)) list.add(parse(item));
        return list;
    }

    /** Les clés qui commencent par ce préfixe, dans l'ordre où elles ont été posées. */
    public List<String> keys(String prefix) {
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
    public void writeTo(StringBuilder text) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            text.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
    }

    /** Range une ligne {@code clé=valeur} relue ; une ligne sans {@code =} est ignorée. */
    public void readLine(String line) {
        int equals = line.indexOf('=');
        if (equals > 0) values.put(line.substring(0, equals), line.substring(equals + 1));
    }

    /**
     * Un nombre à virgule, sans perte : {@code 12} pour 12.0, le texte de {@link Double#toString}
     * sinon. Une valeur sans sens ({@code NaN}) s'écrit par un texte vide.
     */
    public static String number(double value) {
        if (Double.isNaN(value)) return "";
        if (value == Math.rint(value) && Math.abs(value) < 1e15 && !(value == 0 && 1 / value < 0)) return Long.toString((long) value);
        return Double.toString(value);
    }

    /** Relit ce qu'a écrit {@link #number(double)}. */
    public static double parse(String text) {
        return text.isEmpty() ? Double.NaN : Double.parseDouble(text);
    }

    /** Remplace les caractères qui servent de séparateurs par {@code %XX}. */
    public static String escape(String text) {
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

    public static String unescape(String text) {
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

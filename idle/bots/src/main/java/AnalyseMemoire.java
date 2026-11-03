import idle.core.Game;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * La mémoire d'un bot écrite à côté d'une sauvegarde (version 0.168), pour qu'une partie jouée en plusieurs tranches de
 * calcul reste la même partie. Une sauvegarde du jeu ne garde que la partie : reprise, le bot repartait avec une mémoire
 * neuve et un autre hasard, et jouait autrement (en 0.166, 90 h 40 au lieu de 110 h 44). Ici, champ par champ comme
 * {@link AnalyseCopie}, la mémoire de jeu du bot (les champs de {@link IdleProfiles24} et de ses parents, ses politiques
 * et leurs compteurs, ses hasards) s'écrit dans un fichier {@code .bot}, avec le hasard de la partie elle-même ; reprise,
 * elle se relit dans le bot neuf. Ce qui sert à l'analyse ({@link BotAnalyste} : journal, relevés) n'en fait pas partie :
 * chaque tranche écrit ses propres fichiers.
 *
 * <p>Ce qui ne s'écrit pas (un objet qui n'est pas sérialisable, un journal texte) garde sa valeur de départ : la reprise
 * reste jouable, un peu moins fidèle.
 */
final class AnalyseMemoire {

    private AnalyseMemoire() {}

    /** La clé du hasard de la partie elle-même, à côté des champs du bot. */
    static final String HASARD_DU_JEU = "jeu.random";

    /** Le fichier de mémoire qui accompagne une sauvegarde : le même nom, en {@code .bot}. */
    static Path fichier(Path sauvegarde) {
        String nom = sauvegarde.getFileName().toString();
        return sauvegarde.resolveSibling((nom.endsWith(".save") ? nom.substring(0, nom.length() - 5) : nom) + ".bot");
    }

    /** La mémoire de jeu du bot, et le hasard de sa partie : chemin du champ → valeur sérialisée. */
    static Map<String, byte[]> lire(IdleProfiles24 bot) {
        Map<String, byte[]> out = new LinkedHashMap<>();
        parcourir(bot, IdleProfiles24.class, "", out, 0);
        Object hasard = hasardDuJeu(bot.g);
        byte[] h = hasard == null ? null : octets(hasard);
        if (h != null) out.put(HASARD_DU_JEU, h);
        return out;
    }

    private static void parcourir(Object objet, Class<?> depuis, String prefixe, Map<String, byte[]> out, int profondeur) {
        if (profondeur > 6) return;
        for (Class<?> type = depuis; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || field.isSynthetic()) continue;
                Class<?> t = field.getType();
                if (Game.class.isAssignableFrom(t) || StringBuilder.class.isAssignableFrom(t)) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(objet);
                    String chemin = prefixe + type.getName() + "#" + field.getName();
                    if (value == null) continue;
                    Object code = coder(value);
                    byte[] b = code == INCONNU ? null : octets(code);
                    if (b != null) out.put(chemin, b);
                    else if (!(value instanceof Collection<?>) && !(value instanceof Map<?, ?>) && !t.isArray()) {
                        parcourir(value, value.getClass(), chemin + "/", out, profondeur + 1);
                    }
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    // Un champ illisible garde sa valeur de départ à la reprise.
                }
            }
        }
    }

    /** Ce que {@link #coder} ne sait pas écrire : un objet du bot, qu'on parcourt champ par champ. */
    private static final Object INCONNU = new Object();

    /** Un grand nombre du jeu, par son texte. */
    private record Grand(String texte) implements Serializable {}

    /** Un enregistrement (record) : sa classe et ses composants, codés. */
    private record Enregistrement(String classe, Object[] valeurs) implements Serializable {}

    /** Une liste ou un ensemble, ses éléments codés. */
    private record Suite(boolean ensemble, List<Object> elements) implements Serializable {}

    /** Une table, clés et valeurs codées. */
    private record Table(List<Object> cles, List<Object> valeurs) implements Serializable {}

    /**
     * Une valeur sous une forme sérialisable : telle quelle si elle l'est ; un grand nombre par son texte ; un
     * enregistrement, une liste, un ensemble ou une table par leurs composants codés. {@link #INCONNU} pour le reste.
     */
    static Object coder(Object v) {
        if (v == null || v instanceof String || v instanceof Number || v instanceof Boolean || v instanceof Character || v instanceof Enum<?>
                || v instanceof Random) return v;
        if (v instanceof idle.core.math.BigNum b) return new Grand(b.toString());
        if (v instanceof Game) return INCONNU;
        try {
            if (v.getClass().isRecord()) {
                java.lang.reflect.RecordComponent[] rc = v.getClass().getRecordComponents();
                Object[] vals = new Object[rc.length];
                for (int i = 0; i < rc.length; i++) {
                    rc[i].getAccessor().setAccessible(true);
                    vals[i] = coder(rc[i].getAccessor().invoke(v));
                    if (vals[i] == INCONNU) return INCONNU;
                }
                return new Enregistrement(v.getClass().getName(), vals);
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            return INCONNU;
        }
        if (v instanceof Collection<?> c) {
            List<Object> el = new ArrayList<>();
            for (Object x : c) {
                Object y = coder(x);
                if (y == INCONNU) return INCONNU;
                el.add(y);
            }
            return new Suite(v instanceof java.util.Set<?>, el);
        }
        if (v instanceof Map<?, ?> m) {
            List<Object> k = new ArrayList<>(), w = new ArrayList<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                Object a = coder(e.getKey()), b = coder(e.getValue());
                if (a == INCONNU || b == INCONNU) return INCONNU;
                k.add(a);
                w.add(b);
            }
            return new Table(k, w);
        }
        if (v.getClass().isArray() && v.getClass().getComponentType().isPrimitive()) return v;
        return INCONNU;
    }

    /** L'inverse de {@link #coder}. */
    static Object decoder(Object c) throws ReflectiveOperationException {
        if (c instanceof Grand g) return idle.core.math.BigNum.parse(g.texte());
        if (c instanceof Enregistrement e) {
            Class<?> type = Class.forName(e.classe());
            java.lang.reflect.RecordComponent[] rc = type.getRecordComponents();
            Class<?>[] types = new Class<?>[rc.length];
            Object[] vals = new Object[rc.length];
            for (int i = 0; i < rc.length; i++) {
                types[i] = rc[i].getType();
                vals[i] = decoder(e.valeurs()[i]);
            }
            var constructor = type.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return constructor.newInstance(vals);
        }
        if (c instanceof Suite s) {
            Collection<Object> out = s.ensemble() ? new java.util.LinkedHashSet<>() : new ArrayList<>();
            for (Object x : s.elements()) out.add(decoder(x));
            return out;
        }
        if (c instanceof Table t) {
            Map<Object, Object> out = new LinkedHashMap<>();
            for (int i = 0; i < t.cles().size(); i++) out.put(decoder(t.cles().get(i)), decoder(t.valeurs().get(i)));
            return out;
        }
        return c;
    }

    /** La valeur sérialisée, ou {@code null} si elle ne l'est pas. */
    private static byte[] octets(Object value) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(value);
            }
            return bytes.toByteArray();
        } catch (Exception notSerializable) {
            return null;
        }
    }

    private static Object objet(byte[] bytes) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return in.readObject();
        }
    }

    /** Remet la mémoire lue dans le bot (et le hasard dans sa partie) ; rend le nombre de champs remis. */
    static int remettre(IdleProfiles24 bot, Map<String, byte[]> memoire) {
        int remis = 0;
        for (Map.Entry<String, byte[]> e : memoire.entrySet()) {
            try {
                Object value = decoder(objet(e.getValue()));
                if (e.getKey().equals(HASARD_DU_JEU)) {
                    if (poserHasardDuJeu(bot.g, value)) remis++;
                    continue;
                }
                Object owner = bot;
                String[] pas = e.getKey().split("/");
                Field field = null;
                for (int i = 0; i < pas.length; i++) {
                    int diese = pas[i].indexOf('#');
                    Field f = Class.forName(pas[i].substring(0, diese)).getDeclaredField(pas[i].substring(diese + 1));
                    f.setAccessible(true);
                    if (i < pas.length - 1) {
                        owner = f.get(owner);
                        if (owner == null) break;
                    } else {
                        field = f;
                    }
                }
                if (field == null || owner == null) continue;
                // Une liste, une table ou un tableau déjà en place se remplit sur place (une table d'énumérations reste
                // une table d'énumérations) ; le reste se pose, champ final compris une fois rendu accessible.
                if (!remplacerFinal(field, owner, value)) {
                    try {
                        field.set(owner, value);
                    } catch (IllegalAccessException | IllegalArgumentException refused) {
                        continue;
                    }
                }
                remis++;
            } catch (Exception failure) {
                // Une valeur d'une autre version du bot : le champ garde sa valeur de départ.
            }
        }
        return remis;
    }

    /**
     * Un champ final (une liste, une table, un tableau du bot) : son contenu est remplacé sur place quand c'est possible.
     * Vrai si c'est fait.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean remplacerFinal(Field field, Object owner, Object value) throws IllegalAccessException {
        Object existing = field.get(owner);
        try {
            if (existing instanceof Collection c && value instanceof Collection v) {
                c.clear();
                c.addAll(v);
                return true;
            }
            if (existing instanceof Map m && value instanceof Map v) {
                m.clear();
                m.putAll(v);
                return true;
            }
        } catch (UnsupportedOperationException | ClassCastException immutable) {
            // Une liste ou une table qu'on ne peut pas changer : le champ entier sera remplacé.
            return false;
        }
        if (existing != null && existing.getClass().isArray() && value != null && value.getClass() == existing.getClass()) {
            int n = java.lang.reflect.Array.getLength(existing);
            if (n != java.lang.reflect.Array.getLength(value)) return false;
            System.arraycopy(value, 0, existing, 0, n);
            return true;
        }
        return false;
    }

    private static Field champHasard(Game game) {
        for (Class<?> type = game.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field f = type.getDeclaredField("random");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException next) {
                // La classe parente l'a peut-être.
            }
        }
        return null;
    }

    private static Object hasardDuJeu(Game game) {
        try {
            Field f = champHasard(game);
            Object value = f == null ? null : f.get(game);
            return value instanceof Serializable ? value : null;
        } catch (IllegalAccessException failure) {
            return null;
        }
    }

    /** Le hasard de la partie (un champ final de la partie) : remplacé par celui qui a été lu, s'ils sont de même sorte. */
    private static boolean poserHasardDuJeu(Game game, Object value) {
        try {
            Field f = champHasard(game);
            Object existing = f == null ? null : f.get(game);
            if (f == null || !(value instanceof java.util.random.RandomGenerator)) return false;
            if (existing != null && existing.getClass() != value.getClass()) return false;
            f.set(game, value);
            return true;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            // Le hasard de la partie repart de la graine : la reprise reste jouable.
        }
        return false;
    }

    /** Écrit la mémoire du bot à côté de la sauvegarde. */
    static void ecrire(IdleProfiles24 bot, Path sauvegarde) throws Exception {
        Map<String, byte[]> memoire = lire(bot);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(new LinkedHashMap<>(memoire));
        }
        Files.write(fichier(sauvegarde), bytes.toByteArray());
    }

    /** Relit la mémoire écrite à côté de cette sauvegarde ; vide s'il n'y en a pas. */
    @SuppressWarnings("unchecked")
    static Map<String, byte[]> relire(Path sauvegarde) throws Exception {
        Path f = fichier(sauvegarde);
        if (!Files.exists(f)) return Map.of();
        return (Map<String, byte[]>) objet(Files.readAllBytes(f));
    }

    /** Les chemins des champs écrits : pour les vérifications. */
    static List<String> chemins(Map<String, byte[]> memoire) {
        return new ArrayList<>(memoire.keySet());
    }
}

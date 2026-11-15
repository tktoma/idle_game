package idle.core;

import java.util.*;
import java.util.function.Function;

/**
 * Ce que le jeu faisait à plusieurs endroits avec ses énumérations, écrit une fois. Relire un nom
 * sauvegardé sans planter sur celui d'une autre version ({@link #parse}), relire une liste de noms ({@link #parseAll}),
 * et retrouver une constante par son identifiant sans parcourir toutes les autres ({@link #index}).
 */
public final class Enums {
    private Enums() {
    }

    /** La constante de {@code type} que nomme {@code name}, ou {@code null} : nom absent, vide ou d'une autre version. */
    public static <E extends Enum<E>> E parse(Class<E> type, String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return Enum.valueOf(type, name.trim());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Comme {@link #parse(Class, String)}, avec {@code fallback} à la place de {@code null}. */
    public static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback) {
        E value = parse(type, name);
        return value == null ? fallback : value;
    }

    /** Les constantes nommées dans un texte séparé par des virgules, dans l'ordre ; les noms inconnus sont écartés. */
    public static <E extends Enum<E>> List<E> parseAll(Class<E> type, String text) {
        List<E> values = new ArrayList<>();
        if (text == null) return values;
        for (String name : text.split(",")) {
            E value = parse(type, name);
            if (value != null) values.add(value);
        }
        return values;
    }

    /**
     * Une table identifiant → constante, à garder dans un champ statique : la recherche ne parcourt plus toutes les
     * constantes (ni ne recopie leur tableau, comme le fait {@code values()} à chaque appel). Deux constantes au même
     * identifiant sont une erreur du catalogue.
     */
    public static <E, K> Map<K, E> index(E[] values, Function<? super E, ? extends K> key) {
        Map<K, E> byKey = new HashMap<>(values.length * 2);
        for (E value : values) {
            K k = key.apply(value);
            if (byKey.put(k, value) != null) throw new IllegalStateException("Identifiant en double : " + k);
        }
        return Collections.unmodifiableMap(byKey);
    }
}

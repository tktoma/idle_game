import idle.core.Game;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * La copie d'un bot pour une partie d'essai (version 0.161). Copier la partie ne suffit pas : le bot a sa mémoire (une
 * fusion qu'il a coupée lui-même, une réserve qu'il gère, ses politiques et leurs compteurs, son hasard), et un bot
 * neuf sur une partie en cours se trompe (mesuré : sans elle, la copie n'explose plus). Ici, champ par champ, la
 * mémoire du bot passe à un bot neuf construit sur la copie de la partie : les nombres et les textes tels quels, le
 * hasard dupliqué (la suite des tirages est la même), les politiques recopiées de la même façon, les listes et tables
 * dupliquées. La partie elle-même ({@link Game}) n'est jamais recopiée par ce chemin : chaque bot garde la sienne.
 */
final class AnalyseCopie {

    private AnalyseCopie() {}

    /** Recopie la mémoire de {@code source} dans {@code cible}, de la même classe ou d'une classe parente commune. */
    static void memoire(Object source, Object cible) {
        Class<?> type = source.getClass();
        while (type != null && type != Object.class) {
            if (type.isAssignableFrom(cible.getClass())) {
                for (Field field : type.getDeclaredFields()) copie(field, source, cible);
            }
            type = type.getSuperclass();
        }
    }

    private static void copie(Field field, Object source, Object cible) {
        int mod = field.getModifiers();
        if (Modifier.isStatic(mod) || field.isSynthetic()) return;
        Class<?> t = field.getType();
        if (Game.class.isAssignableFrom(t)) return;
        try {
            field.setAccessible(true);
            Object value = field.get(source);
            if (value == null || t.isPrimitive() || immuable(value)) {
                field.set(cible, value);
            } else if (value instanceof Random random) {
                field.set(cible, duplique(random));
            } else if (value instanceof StringBuilder sb) {
                field.set(cible, new StringBuilder(sb));
            } else if (value instanceof List<?> list) {
                field.set(cible, new ArrayList<>(list));
            } else if (value instanceof java.util.Set<?> set) {
                field.set(cible, new LinkedHashSet<>(set));
            } else if (value instanceof Map<?, ?> map) {
                field.set(cible, new HashMap<>(map));
            } else if (value instanceof Collection<?>) {
                field.set(cible, value);
            } else {
                Object existing = field.get(cible);
                if (existing != null && existing.getClass() == value.getClass()) memoire(value, existing);
                else field.set(cible, value);
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            // Un champ qu'on ne peut pas recopier garde sa valeur de départ : la copie reste jouable.
        }
    }

    private static boolean immuable(Object value) {
        return value instanceof Number || value instanceof String || value instanceof Boolean || value instanceof Character
                || value instanceof Enum<?> || value.getClass().isRecord() || value instanceof idle.core.math.BigNum;
    }

    /** Un hasard qui tirera la même suite que l'original, sans toucher à l'original. */
    static Random duplique(Random random) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(random);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (Random) in.readObject();
            }
        } catch (Exception failure) {
            return new Random(random.nextLong());
        }
    }
}

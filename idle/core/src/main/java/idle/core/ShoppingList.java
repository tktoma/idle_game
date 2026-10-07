package idle.core;

import java.util.List;
import java.util.Map;

/**
 * La liste de courses du troisième acte : ce qu'il manque pour le prochain pas, molécule par
 * molécule ({@link Game#shoppingList()}).
 *
 * <p>Le prochain pas est, dans l'ordre : l'astre à portée le plus avancé (ses astres plus petits
 * sont formés) ; avant que les astres soient ouverts, l'assemblage le plus avancé ; une fois tous
 * les astres formés, la prochaine échelle du cosmos.
 *
 * @param kind       ce qu'est le prochain pas
 * @param target     son nom (« Comète glacée », « l'amas de galaxies »), vide s'il n'y en a pas
 * @param ready      vrai si tout est réuni : il n'y a plus qu'à le former
 * @param items      les molécules qui manquent ou qu'il reste à rassembler, dans l'ordre où le pas les demande
 * @param matter     par état de la matière, ce qu'il manque de molécules rassemblées ; vide quand rien ne manque
 * @param assemblies les assemblages du pas qui peuvent être formés tout de suite
 * @param sorts      par état de la matière, le nombre de sortes rassemblées qu'il manque ; vide quand rien ne manque,
 *                   et pour tout ce qui n'est pas une échelle du cosmos
 */
public record ShoppingList(Kind kind, String target, boolean ready, List<Item> items, Map<Molecule.State, Integer> matter,
                           List<Assembly> assemblies, Map<Molecule.State, Integer> sorts) {

    /** Une liste sans variété demandée : tout sauf les échelles du cosmos. */
    public ShoppingList(Kind kind, String target, boolean ready, List<Item> items, Map<Molecule.State, Integer> matter,
                        List<Assembly> assemblies) {
        this(kind, target, ready, items, matter, assemblies, Map.of());
    }

    /** Ce qu'est le prochain pas. */
    public enum Kind {
        /** Rien à viser : le troisième acte n'a pas commencé, ou l'univers est formé. */
        NONE,
        ASSEMBLY,
        BODY,
        COSMOS
    }

    /**
     * Une sorte de molécule qu'il faut avoir en plus grand nombre, ou rassembler.
     *
     * @param molecule la sorte
     * @param owned    combien le joueur en a créé
     * @param needed   combien il lui en faut en tout pour ce pas : au moins {@code owned}
     * @param gather   vrai si la sorte doit être rassemblée et ne l'est pas encore
     * @param purpose  à quoi elle sert dans ce pas : le nom d'un assemblage, ou un texte vide quand l'astre la demande lui-même
     */
    public record Item(Molecule molecule, int owned, int needed, boolean gather, String purpose) {

        /** Molécules qu'il reste à créer. */
        public int missing() {
            return Math.max(0, needed - owned);
        }
    }

    /** La liste vide : il n'y a rien à viser. */
    public static final ShoppingList NONE = new ShoppingList(Kind.NONE, "", false, List.of(), Map.of(), List.of());

    public ShoppingList {
        items = List.copyOf(items);
        // Dans l'ordre des états : gaz, liquides, solides, cristaux, métaux.
        Map<Molecule.State, Integer> ordered = new java.util.EnumMap<>(Molecule.State.class);
        ordered.putAll(matter);
        matter = java.util.Collections.unmodifiableMap(ordered);
        assemblies = List.copyOf(assemblies);
        Map<Molecule.State, Integer> kinds = new java.util.EnumMap<>(Molecule.State.class);
        kinds.putAll(sorts);
        sorts = java.util.Collections.unmodifiableMap(kinds);
    }

    /** Vrai si cette sorte est utile au prochain pas : il en manque, elle reste à rassembler, ou son état manque de matière. */
    public boolean wants(Molecule molecule) {
        for (Item item : items) {
            if (item.molecule().id().equals(molecule.id())) return true;
        }
        return molecule.hasState() && (matter.containsKey(molecule.state()) || sorts.containsKey(molecule.state()));
    }
}

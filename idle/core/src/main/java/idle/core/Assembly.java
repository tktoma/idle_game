package idle.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un assemblage : une matière du monde minéral faite de plusieurs sortes de molécules, comme une
 * roche granitique (quartz, orthose, albite), des rubis et saphirs (corindon et des traces de
 * chrome, de fer et de titane) ou une eau salée (eau et sels). Rien de vivant : ce sont des
 * mélanges et des roches, pas des organismes. Rien de fabriqué non plus : seulement ce qu'on
 * trouve dans la nature, sur Terre ou ailleurs, sans verre, sans alliage, sans batterie.
 *
 * <p>Les assemblages portent des noms généraux : « Minerai de cuivre » plutôt qu'une mine en
 * particulier, « Eau salée » plutôt que chaque mer. Chacun réunit les molécules de sa
 * famille de matière, de trois à huit sortes.
 *
 * <p>Un assemblage se forme avec {@link Game#formAssembly(String)}, une seule fois. Il demande
 * beaucoup de molécules, des centaines, pour que sa taille reste comparable à ce qu'il
 * représente à côté d'une molécule seule : chaque ingrédient doit être rassemblé
 * ({@link Game#formSubstance(String)}), et le joueur doit en posséder le nombre demandé. Les
 * molécules ne sont pas consommées : elles quittent l'amas de leur sorte pour entrer dans le bloc
 * de l'assemblage, où elles restent ; deux assemblages ne se partagent donc pas les mêmes molécules.
 *
 * <p>Les proportions sont celles de la matière réelle, à peu près : 78 % de diazote dans l'air,
 * une trace d'oxyde de chrome dans un rubis. Elles viennent de mémoire.
 *
 * @param id          identifiant stable (sert de clé dans la sauvegarde) : « granitic_rock »
 * @param name        nom affiché
 * @param family      la famille de matière, qui décide de la grandeur augmentée
 * @param tint        la couleur du bloc dans la vue de l'espace, « #rrggbb »
 * @param ingredients ce qu'il demande : identifiant de la molécule → nombre de molécules
 */
public record Assembly(String id, String name, Family family, String tint, Map<String, Integer> ingredients) {

    /** Nombre de molécules d'un assemblage qui valent un bonus de 100 %. */
    public static final double MOLECULES_PER_WHOLE = 200;

    /** Les familles d'assemblages, chacune avec la grandeur qu'elle augmente. */
    public enum Family {
        ROCK("Roches", Molecule.Stat.ATOMS),
        ORE("Minerais", Molecule.Stat.PARTICLES),
        GEM("Pierres précieuses", Molecule.Stat.DARK_GROWTH),
        WATER("Eaux et glaces", Molecule.Stat.PARTICLES),
        AIR("Gaz", Molecule.Stat.SPACE),
        FUEL("Hydrocarbures", Molecule.Stat.ATOMS);

        private final String label;
        private final Molecule.Stat stat;

        Family(String label, Molecule.Stat stat) {
            this.label = label;
            this.stat = stat;
        }

        /** Nom affiché au joueur. */
        public String label() {
            return label;
        }

        /** La grandeur que les assemblages de cette famille augmentent. */
        public Molecule.Stat stat() {
            return stat;
        }
    }

    public Assembly {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Assemblage sans identifiant");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Assemblage sans nom : " + id);
        if (family == null) throw new IllegalArgumentException("Assemblage sans famille : " + id);
        if (tint == null || !tint.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Couleur invalide pour " + id + " : " + tint);
        if (ingredients == null) throw new IllegalArgumentException("Assemblage sans ingrédients : " + id);
        for (Map.Entry<String, Integer> ingredient : ingredients.entrySet()) {
            if (ingredient.getValue() <= 0) throw new IllegalArgumentException("Nombre de molécules invalide dans " + id);
        }
        ingredients = Collections.unmodifiableMap(new LinkedHashMap<>(ingredients));
    }

    /** Un assemblage sans ingrédient encore : ils s'ajoutent avec {@link #with(String, int)}. */
    public static Assembly of(Family family, String id, String name, String tint) {
        return new Assembly(id, name, family, tint, Map.of());
    }

    /**
     * Le même assemblage, avec {@code count} molécules de plus de la sorte {@code moleculeId}.
     *
     * @throws IllegalArgumentException si la molécule y est déjà, ou si le nombre n'est pas positif
     */
    public Assembly with(String moleculeId, int count) {
        Map<String, Integer> more = new LinkedHashMap<>(ingredients);
        if (more.put(moleculeId, count) != null) throw new IllegalArgumentException(moleculeId + " en double dans " + id);
        return new Assembly(id, name, family, tint, more);
    }

    /** Nombre de molécules de l'assemblage, tous ingrédients confondus : la taille de son bloc. */
    public int size() {
        int size = 0;
        for (int count : ingredients.values()) size += count;
        return size;
    }

    /**
     * Ce que donne l'assemblage une fois formé : la grandeur de sa famille, augmentée de 100 % par
     * {@link #MOLECULES_PER_WHOLE} molécules assemblées. Une roche granitique de 270 molécules ajoute 135 %.
     * C'est un premier réglage.
     */
    public Molecule.Boost boost() {
        return new Molecule.Boost(family.stat(), size() / MOLECULES_PER_WHOLE);
    }
}

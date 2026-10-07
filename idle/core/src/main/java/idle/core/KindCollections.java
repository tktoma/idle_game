package idle.core;

import java.util.List;

/**
 * Les collections des huit rayons. Cinq portent sur l'espace, la grandeur qui décide de la durée
 * du troisième acte : ×1,1 à la moitié, ×1,25 complète. Réunies, les cinq moitiés valent ×1,61 et
 * les cinq collections complètes ×3,05. Les trois autres portent sur les atomes et sur la croissance
 * de la matière noire, où un facteur plus gros reste raisonnable : ×1,5 puis ×3.
 *
 * <p>Dans la partie mesurée, le joueur qui ne crée que ce que demandent les astres finit avec la
 * moitié de trois rayons (petites molécules, minéraux, et de peu les acides) : les collections
 * récompensent celui qui va chercher le reste.
 */
public final class KindCollections {

    public static final List<KindCollection> DEFAULT = List.of(
            new KindCollection(Molecule.Kind.SIMPLE, Molecule.Stat.SPACE, 1.1, 1.25),
            new KindCollection(Molecule.Kind.ACID, Molecule.Stat.ATOMS, 1.5, 3),
            new KindCollection(Molecule.Kind.SALT, Molecule.Stat.SPACE, 1.1, 1.25),
            new KindCollection(Molecule.Kind.MINERAL, Molecule.Stat.ATOMS, 1.5, 3),
            new KindCollection(Molecule.Kind.MATERIAL, Molecule.Stat.SPACE, 1.1, 1.25),
            new KindCollection(Molecule.Kind.ORGANIC, Molecule.Stat.DARK_GROWTH, 1.5, 3),
            new KindCollection(Molecule.Kind.LIFE, Molecule.Stat.SPACE, 1.1, 1.25),
            new KindCollection(Molecule.Kind.RARE, Molecule.Stat.SPACE, 1.1, 1.25));

    private KindCollections() {}
}

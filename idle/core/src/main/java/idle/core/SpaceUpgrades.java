package idle.core;

import java.util.List;

/**
 * Catalogue des améliorations liées à l'espace : chacune se prend quand l'expansion a créé assez
 * d'espace en tout, sans rien dépenser.
 *
 * <p>La première ouvre le rassemblement des molécules en gaz, liquides, solides, cristaux et métaux
 * ({@link Game#formSubstance(String)}), et la deuxième, à sa suite, les assemblages de plusieurs
 * sortes de molécules ({@link Game#formAssembly(String)}), puis la troisième les astres
 * ({@link Game#formBody(String)}). Les sept autres ouvrent un à un les rayons du catalogue
 * de molécules, chacune après la précédente : au premier Big Bang, seules les petites molécules
 * sont là.
 *
 * <p>À une unité d'espace par seconde, le deuxième rayon s'ouvre après une demi-heure, le rassemblement
 * après cinquante minutes, le quatrième rayon après huit heures ; les derniers supposent plusieurs
 * Big Bangs, puisque chacun accélère l'expansion. C'est un premier réglage, pas encore passé par
 * la simulation.
 */
public final class SpaceUpgrades {

    public static final List<SpaceUpgrade> DEFAULT = List.of(
            new SpaceUpgrade("space_states", "États de la matière", BigNum.of(3_000), null,
                    new SpaceUpgrade.OpenStates()),
            new SpaceUpgrade("space_assemblies", "Assemblages", BigNum.of(20_000), "space_states",
                    new SpaceUpgrade.OpenAssemblies()),
            new SpaceUpgrade("space_bodies", "Astres", BigNum.of(100_000), "space_assemblies",
                    new SpaceUpgrade.OpenBodies()),
            shelf("space_acid", Molecule.Kind.ACID, 2_000, null),
            shelf("space_salt", Molecule.Kind.SALT, 10_000, "space_acid"),
            shelf("space_mineral", Molecule.Kind.MINERAL, 30_000, "space_salt"),
            shelf("space_material", Molecule.Kind.MATERIAL, 80_000, "space_mineral"),
            shelf("space_organic", Molecule.Kind.ORGANIC, 200_000, "space_material"),
            shelf("space_life", Molecule.Kind.LIFE, 500_000, "space_organic"),
            shelf("space_rare", Molecule.Kind.RARE, 1_000_000, "space_life"));

    private static SpaceUpgrade shelf(String id, Molecule.Kind kind, double space, String requires) {
        return new SpaceUpgrade(id, kind.label(), BigNum.of(space), requires, new SpaceUpgrade.OpenKind(kind));
    }

    private SpaceUpgrades() {}
}

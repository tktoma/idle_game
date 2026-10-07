package idle.core;

import java.util.List;

/**
 * Catalogue des améliorations liées à l'espace : chacune est acquise d'elle-même quand l'expansion a créé assez
 * d'espace en tout, sans rien dépenser.
 *
 * <p>Quatre d'entre elles multiplient une grandeur pour de bon ({@link SpaceUpgrade.Boost}). Les
 * deux premières viennent dans les vingt minutes qui suivent un Big Bang : dix fois plus d'atomes,
 * dix fois plus de particules. Après un Big Bang tout repart d'un seul générateur, et sans elles
 * le joueur passerait une vingtaine d'heures à refaire le chemin avant de revoir un tableau
 * périodique garni, donc avant sa première molécule ; avec elles, deux à trois heures. Les deux
 * inflations doublent chacune l'expansion, à 4 000 et 30 000 d'espace ; ensuite ce sont les paliers
 * de Big Bang qui l'accélèrent ({@link BigBangMilestones}).
 *
 * <p>Trois autres ouvrent, l'une après l'autre, le rassemblement des molécules en gaz, liquides,
 * solides, cristaux et métaux ({@link Game#formSubstance(String)}), les assemblages de plusieurs
 * sortes de molécules ({@link Game#formAssembly(String)}), puis les astres
 * ({@link Game#formBody(String)}). Une donne l'appui automatique sur la matière noire. Les sept
 * dernières ouvrent un à un les rayons du catalogue de molécules, chacune après la précédente : au
 * premier Big Bang, seules les petites molécules sont là.
 *
 * <p>Ces seuils sont passés par la simulation du troisième acte : un joueur présent voit le
 * rassemblement après cinquante minutes, les assemblages et les astres vers cinq heures, le
 * dernier rayon vers dix heures, avec deux Big Bangs de plus en route.
 */
public final class SpaceUpgrades {

    public static final List<SpaceUpgrade> DEFAULT = List.of(
            // Ce que le Big Bang laisse derrière lui : de quoi refaire vite le chemin jusqu'au suivant.
            new SpaceUpgrade("space_primordial_atoms", "Matière primordiale", BigNum.of(300), null,
                    new SpaceUpgrade.Boost(Molecule.Stat.ATOMS, 10)),
            new SpaceUpgrade("space_primordial_particles", "Lumière primordiale", BigNum.of(1_200), null,
                    new SpaceUpgrade.Boost(Molecule.Stat.PARTICLES, 10)),
            // L'inflation : l'expansion elle-même accélère, deux fois. La suite vient des paliers de Big Bang.
            new SpaceUpgrade("space_inflation_1", "Inflation", BigNum.of(4_000), null,
                    new SpaceUpgrade.Boost(Molecule.Stat.SPACE, 2)),
            new SpaceUpgrade("space_inflation_2", "Inflation II", BigNum.of(30_000), "space_inflation_1",
                    new SpaceUpgrade.Boost(Molecule.Stat.SPACE, 2)),
            // Deux inflations tardives, pour la fin de partie : l'espace n'y achète plus que des molécules.
            new SpaceUpgrade("space_inflation_3", "Inflation III", BigNum.of(100_000_000), "space_inflation_2",
                    new SpaceUpgrade.Boost(Molecule.Stat.SPACE, 2)),
            new SpaceUpgrade("space_inflation_4", "Inflation IV", BigNum.of(5_000_000_000L), "space_inflation_3",
                    new SpaceUpgrade.Boost(Molecule.Stat.SPACE, 2)),
            new SpaceUpgrade("space_states", "États de la matière", BigNum.of(3_000), null,
                    new SpaceUpgrade.OpenStates()),
            new SpaceUpgrade("space_assemblies", "Assemblages", BigNum.of(40_000), "space_states",
                    new SpaceUpgrade.OpenAssemblies()),
            new SpaceUpgrade("space_bodies", "Astres", BigNum.of(60_000), "space_assemblies",
                    new SpaceUpgrade.OpenBodies()),
            new SpaceUpgrade("space_auto_hold", "Appui automatique", BigNum.of(6_000), null,
                    new SpaceUpgrade.AutoHold()),
            // Le confort : créer en tenant le clic, puis tout rassembler, assembler ou former d'un geste.
            new SpaceUpgrade("space_hold_create", "Création continue", BigNum.of(800), null,
                    new SpaceUpgrade.HoldCreate()),
            new SpaceUpgrade("space_bulk_form", "Gestes groupés", BigNum.of(15_000), "space_states",
                    new SpaceUpgrade.BulkForm()),
            // Puis ce qui se fait tout seul : rassembler, créer plus souvent, assembler et former les astres.
            new SpaceUpgrade("space_auto_gather", "Rassemblement automatique", BigNum.of(100_000), "space_bulk_form",
                    new SpaceUpgrade.AutoGather()),
            new SpaceUpgrade("space_creation_pace", "Cadence de création", BigNum.of(250_000), null,
                    new SpaceUpgrade.CreationPace(2)),
            new SpaceUpgrade("space_auto_form", "Formation automatique", BigNum.of(300_000), "space_auto_gather",
                    new SpaceUpgrade.AutoForm()),
            new SpaceUpgrade("space_creation_pace_2", "Cadence de création II", BigNum.of(20_000_000), "space_creation_pace",
                    new SpaceUpgrade.CreationPace(1)),
            shelf("space_acid", Molecule.Kind.ACID, 2_000, null),
            shelf("space_salt", Molecule.Kind.SALT, 10_000, "space_acid"),
            shelf("space_mineral", Molecule.Kind.MINERAL, 20_000, "space_salt"),
            shelf("space_material", Molecule.Kind.MATERIAL, 80_000, "space_mineral"),
            shelf("space_organic", Molecule.Kind.ORGANIC, 200_000, "space_material"),
            shelf("space_life", Molecule.Kind.LIFE, 500_000, "space_organic"),
            shelf("space_rare", Molecule.Kind.RARE, 1_000_000, "space_life"));

    private static SpaceUpgrade shelf(String id, Molecule.Kind kind, double space, String requires) {
        return new SpaceUpgrade(id, kind.label(), BigNum.of(space), requires, new SpaceUpgrade.OpenKind(kind));
    }

    private SpaceUpgrades() {}
}

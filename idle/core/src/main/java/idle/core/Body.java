package idle.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Un astre : un corps du ciel, de l'amas de roches à la planète. C'est l'étage au-dessus des
 * assemblages : un astre se forme en cumulant ce que le joueur a déjà fait de plus petit.
 *
 * <p>Les astres vont par échelles ({@link Tier}), chacune avec plusieurs types : un amas peut être
 * rocheux, glacé, ferreux ou carboné ; une planète, rocheuse, océan, de lave, glacée ou géante.
 * Un astre se forme avec {@link Game#formBody(String)}, une seule fois, quand le joueur réunit
 * tout ce qu'il demande :
 * <ul>
 *   <li>des astres de l'échelle d'en dessous, déjà formés ({@link #bodies()}) : une comète part
 *       d'un amas, une planète de lunes et d'astéroïdes ;</li>
 *   <li>des assemblages déjà formés ({@link #assemblies()}) ;</li>
 *   <li>de la matière : un nombre de molécules rassemblées dans un état donné, toutes sortes
 *       confondues ({@link #matter()}) ;</li>
 *   <li>des molécules précises, en nombre ({@link #molecules()}).</li>
 * </ul>
 * Rien n'est consommé ni réservé : ce sont des seuils. Deux astres peuvent demander le même
 * assemblage ou le même amas.
 *
 * @param id         identifiant stable (sert de clé dans la sauvegarde) : « icy_comet »
 * @param name       nom affiché
 * @param tier       son échelle
 * @param look       l'aspect de sa surface, pour son dessin
 * @param tint       sa couleur principale, « #rrggbb »
 * @param accent     la couleur de ses détails, « #rrggbb »
 * @param stat       la grandeur qu'il augmente une fois formé
 * @param bodies     les astres qu'il faut avoir formés
 * @param assemblies les assemblages qu'il faut avoir formés
 * @param matter     la matière demandée : état → nombre de molécules rassemblées dans cet état
 * @param molecules  les molécules demandées : identifiant → nombre de molécules créées
 */
public record Body(String id, String name, Tier tier, Look look, String tint, String accent, Molecule.Stat stat,
                   List<String> bodies, List<String> assemblies, Map<Molecule.State, Integer> matter,
                   Map<String, Integer> molecules) {

    /** Les échelles d'astres, de la plus petite à la plus grande, avec ce que donne un astre de chacune. */
    public enum Tier {
        RUBBLE("Amas de roches", 2),
        COMET("Comètes", 4),
        ASTEROID("Astéroïdes", 8),
        MOON("Lunes", 16),
        PLANET("Planètes", 32);

        private final String label;
        private final double gain;

        Tier(String label, double gain) {
            this.label = label;
            this.gain = gain;
        }

        /** Nom affiché au joueur. */
        public String label() {
            return label;
        }

        /** Ce qu'un astre de cette échelle ajoute à sa grandeur (2 = +200 %) : le double à chaque échelle. */
        public double gain() {
            return gain;
        }
    }

    /** L'aspect de la surface d'un astre : ce qui distingue à l'œil deux types d'une même échelle. */
    public enum Look {
        /** De la roche nue, marquée de cratères. */
        ROCKY,
        /** De la glace, parcourue de fissures. */
        ICY,
        /** Du métal ou de la lave : sombre, avec des veines qui luisent. */
        MOLTEN,
        /** Sombre et mat, riche en carbone. */
        DARK,
        /** Une surface cachée sous une brume épaisse. */
        HAZY,
        /** Des mers et des nuages. */
        OCEAN,
        /** Des bandes de gaz. */
        BANDED,
        /** Des bandes de gaz, et des anneaux. */
        RINGED
    }

    public Body {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Astre sans identifiant");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Astre sans nom : " + id);
        if (tier == null || look == null || stat == null) throw new IllegalArgumentException("Astre incomplet : " + id);
        if (tint == null || !tint.matches("#[0-9a-fA-F]{6}") || accent == null || !accent.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Couleur invalide pour " + id);
        }
        for (int count : matter.values()) {
            if (count <= 0) throw new IllegalArgumentException("Quantité de matière invalide dans " + id);
        }
        for (int count : molecules.values()) {
            if (count <= 0) throw new IllegalArgumentException("Nombre de molécules invalide dans " + id);
        }
        bodies = List.copyOf(bodies);
        assemblies = List.copyOf(assemblies);
        matter = matter.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(matter));
        molecules = Collections.unmodifiableMap(new LinkedHashMap<>(molecules));
    }

    /** Un astre qui ne demande encore rien : ses conditions s'ajoutent avec les méthodes ci-dessous. */
    public static Body of(Tier tier, String id, String name, Look look, String tint, String accent, Molecule.Stat stat) {
        return new Body(id, name, tier, look, tint, accent, stat, List.of(), List.of(), Map.of(), Map.of());
    }

    /** Le même astre, qui demande en plus d'avoir formé ces astres. */
    public Body from(String... bodyIds) {
        return new Body(id, name, tier, look, tint, accent, stat, List.of(bodyIds), assemblies, matter, molecules);
    }

    /** Le même astre, qui demande en plus d'avoir formé ces assemblages. */
    public Body made(String... assemblyIds) {
        return new Body(id, name, tier, look, tint, accent, stat, bodies, List.of(assemblyIds), matter, molecules);
    }

    /** Le même astre, qui demande en plus {@code count} molécules rassemblées dans cet état. */
    public Body matter(Molecule.State state, int count) {
        Map<Molecule.State, Integer> more = new EnumMap<>(Molecule.State.class);
        more.putAll(matter);
        if (more.put(state, count) != null) throw new IllegalArgumentException(state + " en double dans " + id);
        return new Body(id, name, tier, look, tint, accent, stat, bodies, assemblies, more, molecules);
    }

    /** Le même astre, qui demande en plus {@code count} molécules de la sorte {@code moleculeId}. */
    public Body molecules(String moleculeId, int count) {
        Map<String, Integer> more = new LinkedHashMap<>(molecules);
        if (more.put(moleculeId, count) != null) throw new IllegalArgumentException(moleculeId + " en double dans " + id);
        return new Body(id, name, tier, look, tint, accent, stat, bodies, assemblies, matter, more);
    }

    /** Nombre de conditions à réunir, toutes sortes confondues. */
    public int conditions() {
        return bodies.size() + assemblies.size() + matter.size() + molecules.size();
    }

    /** Ce que donne l'astre une fois formé : sa grandeur, augmentée de ce que vaut son échelle ({@link Tier#gain()}). */
    public Molecule.Boost boost() {
        return new Molecule.Boost(stat, tier.gain());
    }
}

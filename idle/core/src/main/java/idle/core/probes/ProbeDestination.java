package idle.core.probes;

import idle.core.molecules.Molecule;

/**
 * Où va une sonde. Le vide intersidéral rapporte de l'espace et des sursauts d'expansion ; chaque
 * autre destination rapporte surtout les éléments d'un rayon du catalogue, ce qui rejoint ce que
 * l'indicateur dit manquer. Une destination de rayon s'ouvre avec son rayon.
 */
public enum ProbeDestination {
    VOID("Vide intersidéral", null),
    // Version 0.178 : une destination par rayon, quatre rayons ; la lune salée, le reste d'étoile, la planète
    // océan et le reste de supernova sont partis avec leurs rayons.
    GAS_CLOUD("Nuage de gaz", Molecule.Kind.SIMPLE),
    SALT_MOON("Lune aux lacs salés", Molecule.Kind.ACID),
    ASTEROIDS("Ceinture d'astéroïdes", Molecule.Kind.MINERAL),
    NEBULA("Nébuleuse moléculaire", Molecule.Kind.ORGANIC);

    private final String label;
    private final Molecule.Kind kind;

    ProbeDestination(String label, Molecule.Kind kind) {
        this.label = label;
        this.kind = kind;
    }

    public String label() {
        return label;
    }

    /** Le rayon dont elle rapporte les éléments, ou {@code null} pour le vide. */
    public Molecule.Kind kind() {
        return kind;
    }
}

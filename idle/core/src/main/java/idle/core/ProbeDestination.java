package idle.core;

/**
 * Où va une sonde. Le vide intersidéral rapporte de l'espace et des sursauts d'expansion ; chaque
 * autre destination rapporte surtout les éléments d'un rayon du catalogue, ce qui rejoint ce que
 * l'indicateur dit manquer. Une destination de rayon s'ouvre avec son rayon.
 */
public enum ProbeDestination {
    VOID("Vide intersidéral", null),
    GAS_CLOUD("Nuage de gaz", Molecule.Kind.SIMPLE),
    COMET("Comète", Molecule.Kind.ACID),
    SALT_MOON("Lune aux lacs salés", Molecule.Kind.SALT),
    ASTEROIDS("Ceinture d'astéroïdes", Molecule.Kind.MINERAL),
    DEAD_STAR("Reste d'étoile", Molecule.Kind.MATERIAL),
    NEBULA("Nébuleuse moléculaire", Molecule.Kind.ORGANIC),
    OCEAN_WORLD("Planète océan", Molecule.Kind.LIFE),
    SUPERNOVA("Reste de supernova", Molecule.Kind.RARE);

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

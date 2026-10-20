package idle.core.universe;

/**
 * Les améliorations de l'onglet Univers (version 0.081), payées plus tard en graines d'univers.
 * Pour l'instant elles n'ont que leur nom et une phrase : leur niveau monte avec un bouton d'essai,
 * sans rien coûter ni rien changer.
 */
public enum UniverseUpgrade {
    BRANE("brane", "Brane", "Une membrane où l'univers est posé, comme une feuille dans un livre."),
    BULK("bulk", "Bulk", "L'espace plus vaste où flottent les branes, et que leurs univers ne voient pas."),
    LANDSCAPE("landscape", "Paysage", "Toutes les façons dont un univers peut être fait, côte à côte, comme des vallées."),
    OMNIVERSE("omniverse", "Omnivers", "Tout ce qui peut exister, tous les univers de tous les paysages réunis.");

    private final String id;
    private final String label;
    private final String flavor;

    UniverseUpgrade(String id, String label, String flavor) {
        this.id = id;
        this.label = label;
        this.flavor = flavor;
    }

    /** L'identifiant gardé dans la sauvegarde. */
    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    /** Une phrase qui dit ce que c'est. */
    public String flavor() {
        return flavor;
    }
}

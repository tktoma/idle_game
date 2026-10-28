package idle.ui;

/**
 * Les catégories des notifications (version 0.146) : chacune se coupe à part dans les réglages. Une notification coupée
 * ne s'affiche pas et ne joue pas de son ; le journal, lui, garde tout.
 *
 * <p>Elles sont rangées par endroit du jeu, dans l'ordre où la partie les découvre.
 */
public enum Notice {
    START("Premier élan", "La création qui prend de l'élan, au tout début."),
    ELEMENTS("Éléments et ensembles", "Un élément nouveau, un ensemble à moitié ou tout réuni."),
    AUTOMATION("Automatismes", "Le tableau et l'automatisation ouverts, la synthèse automatique, un automatisme offert ou de matière noire."),
    EXPLOSION("Explosions", "Le tableau qui peut exploser, et ce qu'une explosion a rapporté."),
    MILESTONES("Paliers de vitesse et de taille", "Les paliers de vitesse des particules et de taille de la matière noire."),
    CHALLENGES("Défis de matière noire", "Un défi qui s'ouvre, un défi réussi."),
    COLLIDER("Collisionneur", "Le quark top, les améliorations qui s'ouvrent, l'énergie des trous noirs, les hadrons qui se proposent."),
    BIG_BANG("Big Bang et ses défis", "Chaque Big Bang, et les défis de Big Bang commencés ou réussis."),
    MOLECULES("Molécules, espace et collections", "Un rayon de molécules qui s'ouvre, une amélioration d'espace, une collection."),
    SKY("Astres et jalons", "Les astres qui mûrissent, les jalons du cosmos."),
    AUTO_SKY("Rassemblement et formation automatiques", "Ce que le rassemblement et la formation automatiques font sans vous."),
    COMETS("Comètes", "Une comète qui traverse l'expansion, quelques secondes pour la saisir."),
    SKY_EVENTS("Anomalies du ciel", "Les petites anomalies de l'expansion (les masquer dans le jeu les tait aussi)."),
    PROBES("Sondes", "Une sonde revenue, son butin à prendre."),
    VOID("Le vide", "Le vide qui se propose, son trou noir, ses améliorations au plus haut."),
    ACHIEVEMENTS("Succès", "Les succès, ceux du collisionneur et ceux du Big Bang."),
    TITLES("Titres du profil", "Un titre obtenu, à porter dans l'onglet Profil."),
    RECORDS("Records", "Un record de temps battu sur une étape."),
    COSMETICS("Skins, thèmes et notations", "Un skin, un thème ou une notation débloqué."),
    GAME("Partie et sauvegarde", "Ce que dit une partie chargée ou réparée.");

    private final String label;
    private final String detail;

    Notice(String label, String detail) {
        this.label = label;
        this.detail = detail;
    }

    public String label() {
        return label;
    }

    /** Ce que la catégorie annonce, pour la bulle d'aide de son bouton. */
    public String detail() {
        return detail;
    }
}

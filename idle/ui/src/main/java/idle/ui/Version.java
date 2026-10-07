package idle.ui;

import java.util.List;

/**
 * Le numéro de version du jeu et ses notes : ce qui a changé, version après version, de la plus
 * récente à la plus ancienne. Affichés au bas des réglages ({@link SettingsPage}) et joints au
 * rapport de bug ({@link BugReport}).
 *
 * <p>À chaque livraison : ajouter une entrée en tête de {@link #NOTES}, et {@link #CURRENT} suit.
 */
final class Version {

    /**
     * Une version et ce qu'elle a apporté.
     *
     * @param number  son numéro
     * @param title   ce qu'elle apporte, en quelques mots
     * @param changes ses changements, une phrase chacun
     */
    record Note(String number, String title, List<String> changes) {
    }

    /** Les notes de version, de la plus récente à la plus ancienne. */
    static final List<Note> NOTES = List.of(
            new Note("0.40", "Sauvegarde", List.of(
                    "La partie est sauvegardée toute seule : toutes les 30 secondes et à la fermeture.",
                    "Au retour, une part du temps d'absence est rejouée : 10 % au premier acte, 40 % après la première "
                            + "explosion, 70 % après le premier Big Bang, 100 % une fois l'univers formé. "
                            + "Un résumé dit ce que l'absence a rapporté.",
                    "Export et import de la partie en texte, par le presse-papiers.",
                    "La fenêtre retrouve sa taille, sa place et son onglet au lancement.",
                    "Rapport de bug en un clic, et ces notes de version.")),
            new Note("0.39", "Amas de galaxies et univers dans l'expansion", List.of(
                    "La vue de l'expansion de la matière peut reculer jusqu'à l'amas de galaxies, puis jusqu'à l'univers.")),
            new Note("0.38", "Amas de galaxies et univers", List.of(
                    "Deux échelles après la galaxie : l'amas de galaxies, puis l'univers.",
                    "Le sous-onglet « Galaxie » devient « Univers ».")),
            new Note("0.37", "Graphique de toutes les ressources", List.of(
                    "Un graphique des statistiques trace toutes les ressources ensemble, en échelle logarithmique.")),
            new Note("0.36", "Statistiques du Big Bang", List.of(
                    "La page de statistiques du Big Bang est complète : durées, paliers, expansion, molécules, astres.")),
            new Note("0.35", "Cinquième palier", List.of(
                    "À partir du cinquième Big Bang, l'arbre de matière noire est gardé.")),
            new Note("0.34", "Énergie sombre et création automatique", List.of(
                    "Le palier « Énergie sombre » fait dépendre l'expansion de la matière noire en réserve.",
                    "Le palier « Création automatique » entretient les amas de molécules choisis.")));

    /** Le numéro de la version en cours. */
    static final String CURRENT = NOTES.get(0).number();

    private Version() {
    }

    /** Les notes de version en texte : une version par paragraphe, ses changements en dessous. */
    static String notes() {
        StringBuilder text = new StringBuilder();
        for (Note note : NOTES) {
            if (text.length() > 0) text.append("\n\n");
            text.append(note.number()).append(" — ").append(note.title());
            for (String change : note.changes()) text.append("\n· ").append(change);
        }
        return text.toString();
    }
}

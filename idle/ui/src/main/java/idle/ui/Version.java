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
            new Note("0.44", "Troisième acte et après", List.of(
                    "Prime de vitesse : exploser en moins de 90 % du temps de la dernière explosion primée rapporte une "
                            + "matière noire de plus.",
                    "Collections : la moitié d'un rayon de molécules, puis le rayon entier, multiplient une grandeur.",
                    "L'amas de galaxies et l'univers demandent aussi de la variété : un nombre de sortes rassemblées par état.",
                    "Une comète traverse l'expansion de temps en temps : saisie, elle double l'espace créé pendant deux minutes.",
                    "Trois améliorations d'espace de fin de partie : Inflation III et IV, Cadence de création II.",
                    "Records par étape, gardés d'une partie à l'autre, et mode chrono dans la ligne d'objectif.",
                    "Seize succès pour le troisième acte, de la première molécule à l'univers.",
                    "Défis de Big Bang, une fois tous les paliers atteints : trois contraintes, trois récompenses qui restent.")),
            new Note("0.43", "Lisibilité", List.of(
                    "Liste de courses au-dessus des assemblages, des astres et de l'univers : ce qu'il manque pour le "
                            + "prochain pas, avec un bouton « Créer » ou « Rassembler » sur chaque ligne.",
                    "Meilleur achat : chaque molécule dit ce qu'elle rapporte pour l'espace qu'elle prend, une ligne "
                            + "nomme la meilleure, et un bouton range les cartes par rendement.",
                    "Sous les onglets : le temps d'expansion qu'il reste avant le prochain astre, et ce qu'il manque "
                            + "aux cinq conditions du prochain Big Bang.",
                    "La case du Big Bang dit le palier qu'il donnera ; le bouton d'explosion, ce que sa matière noire ouvrira.",
                    "Au survol d'un compteur, une bulle dit de quoi il est fait : production, atomes par fusion, "
                            + "matière noire par explosion, espace par seconde.",
                    "Signes d'état devant le nom des cartes, et un signe par état de la matière : tout se reconnaît "
                            + "sans la couleur. Réglable.",
                    "Journal des cent dernières annonces, dans les statistiques.")),
            new Note("0.42", "Automatisation", List.of(
                    "Ordre d'achat au choix pour « Améliorations en atomes » et « Achat des automatismes » : le moins cher "
                            + "d'abord, ou votre liste, rangée par deux flèches.",
                    "Nouvel automatisme de matière noire, après le premier Big Bang : il rachète l'arbre tout seul.",
                    "Nouveau palier au troisième Big Bang, « Mémoire des défis » : les défis réussis le restent.",
                    "Création automatique : une réserve d'espace réglable, un bouton « Tout confier », et l'amélioration "
                            + "d'espace « Cadence de création » (une création toutes les 2 secondes au lieu de 5).",
                    "Deux améliorations d'espace : « Rassemblement automatique » et « Formation automatique » des "
                            + "assemblages et des astres. La galaxie, l'amas de galaxies et l'univers restent à vous.")),
            new Note("0.41", "Confort de jeu", List.of(
                    "Aide « Comment jouer », par sa touche (H) ou son bouton sous les onglets.",
                    "Un raccourci pour presque tout : synthèse, explosion, Big Bang, coupure des automatismes, "
                            + "flèches pour changer d'onglet et de sous-onglet. Chaque touche se change dans les réglages.",
                    "Confirmations réglables une par une : explosion, Big Bang, défis.",
                    "Achat par dix ou « max » des améliorations en atomes, avec « Achat groupé » (1 matière noire).",
                    "Un bouton et une touche coupent ou relancent tous les automatismes d'un geste.",
                    "Les améliorations d'espace sont acquises d'elles-mêmes dès leur seuil. Deux nouvelles : "
                            + "« Création continue » (tenir le clic répète la création) et « Gestes groupés » "
                            + "(tout rassembler, tout assembler, former les astres prêts).",
                    "Une pastille signale l'onglet et le sous-onglet où quelque chose est prêt.",
                    "Molécules favorites, et deux filtres : créables maintenant, utiles au prochain astre.",
                    "Le verrou de l'appui, le seuil de fusion et les automatismes de matière noire gardent leur réglage "
                            + "à travers les explosions et les Big Bangs.")),
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

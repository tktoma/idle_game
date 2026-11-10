package idle.ui.pages;

import idle.core.universe.SkinVisual;

/**
 * Version 0.203 : où se voit chaque visuel de l'onglet Skins, en une phrase écrite sous son nom. Plusieurs skins semblaient
 * ne rien faire parce qu'ils ne changent qu'un détail, ou qu'ils ne se voient qu'à un endroit précis du jeu (les signes des
 * états de la matière, le cadre des mini-jeux, la galaxie, le tic du métronome).
 */
final class SkinPlaces {

    private SkinPlaces() {
    }

    /** Où regarder pour voir ce visuel, et ce qu'il change. */
    static String of(SkinVisual visual) {
        return switch (visual) {
            case PARTICLE_FUSION -> "Onglet Particules : l'animation du centre, à chaque fusion.";
            case ATOM -> "Onglet Atomes : le dessin de l'atome.";
            case TABLE -> "Onglet Atomes : les cases du tableau périodique.";
            case DARK_BALL -> "Onglet Matière noire : la boule qui grossit.";
            case DARK_ANIMATION -> "L'animation plein écran de chaque explosion, et ce qui tombe autour de la boule de matière noire.";
            case BIG_BANG -> "L'animation plein écran du Big Bang, et la vue « Expansion de la matière » de l'onglet Big Bang.";
            case COLLIDER_BEAM -> "Onglet Collisionneur : les deux faisceaux qui tournent dans l'anneau.";
            case COLLIDER -> "Onglet Collisionneur : l'anneau et son fond.";
            case TREE -> "Onglet Univers, sous-page Graine : l'essence de l'arbre (écorce, feuilles, silhouette).";
            case BODY -> "Onglet Big Bang : les astres, dans leur sous-page, dans l'espace et au cœur de la galaxie.";
            case LINE, PLANE, VOLUME, DURATION, LOOP, BRANE, CLICKER, GRID, MIRROR, MIX ->
                    "La fenêtre de cette dimension (onglet Univers, carte des dimensions) : le décor et l'habillage du mini-jeu, jamais sa règle.";
            case MOLECULE -> "Onglet Big Bang : le dessin des molécules, sur leurs cartes et dans l'espace.";
            case AVATAR_FRAME -> "Le cadre autour de l'avatar, dans l'en-tête et dans l'onglet Profil.";
            case AMBIENT -> "Le fond de toute la fenêtre, derrière les pages.";
            case TABS -> "La barre des onglets, en haut ou à gauche.";
            case CARD -> "Toutes les cartes du jeu : améliorations, automatismes, défis, skins.";
            case TAB_ICONS -> "Les petites icônes animées des onglets.";
            case CARD_SIGNS -> "Le signe d'état écrit sur les cartes : prête, en attente, acquise, fermée.";
            case MATTER_SIGNS -> "Seulement le petit signe écrit devant « Gaz », « Liquides » et « Cristaux » (∴ ≈ ◆) : onglet Big Bang, sur les "
                    + "filtres des molécules, les États de la matière et la légende de l'espace.";
            case TOOLTIP -> "Les info-bulles qui détaillent une production.";
            case AMBIENT_DUST -> "Onglet Particules : les poussières qui dérivent derrière les générateurs.";
            case GENERATOR_FUSION -> "Onglet Particules : l'animation des générateurs qui fusionnent.";
            case TABLE_PAGE -> "Onglet Atomes : la page du tableau périodique, autour de ses cases.";
            case DARK_RULER -> "Onglet Matière noire : l'échelle des grandeurs, autour de la boule.";
            case DARK_TREE -> "Onglet Matière noire, sous-page Arbre : ses traits, ses vignettes, ses en-têtes et son fond.";
            case CHALLENGES -> "Onglet Matière noire, sous-page Défis : les cartes des défis.";
            case MOLECULE_PALETTE -> "Les couleurs des atomes dans les dessins des molécules.";
            case CLUMPS -> "Onglet Big Bang, « Expansion de la matière » : les amas des sortes rassemblées.";
            case BLOCKS -> "Onglet Big Bang, « Expansion de la matière » : les blocs des assemblages.";
            case GAS -> "Onglet Big Bang, « Expansion de la matière » : la matière vue de loin, fondue en gaz.";
            case SKY -> "Onglet Big Bang, sous-page Astres : le ciel derrière eux.";
            case GALAXY -> "Onglet Big Bang, sous-page Univers : la galaxie, seule puis au milieu de l'amas (sans skin d'amas) ; "
                    + "une fois l'univers formé, la toile la remplace et elle n'est plus qu'un point.";
            case GALAXY_CLUSTER -> "Onglet Big Bang, sous-page Univers : l'amas de galaxies, de sa formation à celle de l'univers.";
            case COSMIC_WEB -> "Onglet Big Bang, sous-page Univers : la toile, une fois l'univers formé.";
            case SYSTEM -> "Onglet Big Bang, sous-page Astres : les systèmes solaires.";
            case PROBE -> "Onglet Big Bang : les sondes et leur hangar.";
            case DETECTOR -> "Onglet Collisionneur : le détecteur, là où les faisceaux se croisent.";
            case LIGHTNING -> "Onglet Collisionneur : l'éclair d'un croisement capté.";
            case SPARKS -> "Onglet Collisionneur : les étincelles d'une collision.";
            case QUARK_FLIGHT -> "Onglet Collisionneur : les quarks qui s'envolent d'une collision.";
            case QUARKS -> "Onglet Hadrons : les quarks de la liaison.";
            case GLUONS -> "Onglet Hadrons : les gluons.";
            case HADRON_BACKGROUND -> "Onglet Hadrons : le fond de la liaison.";
            case VOID_FOAM -> "Onglet Le vide : l'écume du fond.";
            case VOID_PAIRS -> "Onglet Le vide : les paires de particules virtuelles.";
            case VOID_HOLE -> "Onglet Le vide : son trou noir.";
            case UNIVERSE_BUBBLE -> "Onglet Univers : les univers formés, en bulles.";
            case UNIVERSE_BIRTH -> "Onglet Univers : l'animation de la naissance d'un univers.";
            case UNIVERSE_LINKS -> "Onglet Univers : les liens entre les bulles.";
            case LAUNCH -> "Onglet Univers : l'animation du lancement de l'univers suivant.";
            case SEED -> "Onglet Univers, sous-page Graine : la graine, avant qu'elle germe.";
            case WATERING_CAN -> "Onglet Univers, sous-page Graine : l'arrosoir, sur l'étagère puis en main.";
            case SHELF -> "Onglet Univers, sous-page Graine : l'étagère de l'arrosoir et du chrono.";
            case HOSE -> "Onglet Univers, sous-page Graine : le tuyau d'arrosage, une fois acheté.";
            case CHRONO -> "Onglet Univers, sous-page Graine : le chrono, sur l'étage du haut de l'étagère.";
            case GROWTH_RING -> "Onglet Univers, sous-page Graine : le rond de croissance à cueillir.";
            case WEATHER -> "Onglet Univers, sous-page Graine : la météo, en haut à gauche de la vue.";
            case SEASONS -> "Onglet Univers, sous-page Graine : les saisons qui passent.";
            case TREE_SEASON -> "Onglet Univers, sous-page Graine : la saison figée du feuillage, quelle que soit l'essence.";
            case TREE_FANTASY -> "Onglet Univers, sous-page Graine : la matière de l'arbre, quelle que soit l'essence.";
            case TREE_GROUND -> "Onglet Univers, sous-page Graine : le sol au pied de l'arbre.";
            case TREE_BUBBLES -> "Onglet Univers, sous-page Graine : les bulles de l'arbre.";
            case TREE_BUBBLE_LINKS -> "Onglet Univers, sous-page Graine : les liens entre les bulles de l'arbre.";
            case DIMENSIONS_MAP -> "Onglet Univers, sous-page Dimensions : la carte des dix dimensions.";
            case FINALE_FILL -> "Onglet Univers, sous-page Dimensions : ce qui remplit la carte pendant le défi final.";
            case DIMENSION_FRAME -> "Purement décoratif : un cadre autour de la vue du mini-jeu, dans la fenêtre de chaque dimension. "
                    + "Il ne se voit nulle part ailleurs et ne change rien au jeu.";
            case DIMENSION_PALETTE -> "Les couleurs des dix dimensions, partout où elles servent.";
            case AVATAR_STYLE -> "Onglet Profil : le style de dessin des avatars.";
            case ALBUM -> "Onglet Profil : les cases des albums.";
            case ACHIEVEMENT_TILE -> "Onglet Succès : les tuiles des succès obtenus.";
            case ACHIEVEMENT_LOCKED -> "Onglet Succès : les tuiles des succès pas encore obtenus.";
            case CHART -> "Onglet Statistiques : le fond et le trait des courbes.";
            case CHART_PALETTE -> "Onglet Statistiques : les couleurs des courbes.";
            case TIMELINE -> "Onglet Statistiques : la frise des univers.";
            case TOAST -> "Les notifications qui passent en bas à droite de la fenêtre.";
            case PANEL -> "Les panneaux qui recouvrent la fenêtre : l'aide (touche H), le retour d'une absence, la fin.";
            case TRANSITION -> "L'effet plein écran du passage à l'univers suivant.";
            case ENDING -> "L'écran de fin de la partie.";
            case START_BUTTON -> "Le bouton qui lance une partie neuve, au milieu de l'onglet Particules.";
            case CURSOR -> "Le curseur de la souris, sur tous les onglets ; au-dessus d'un bouton, la main du système reste.";
            case SOUND_BANK -> "Le timbre de tous les sons du jeu ; il se fait entendre dès qu'on le choisit (sons allumés dans les réglages).";
            case BIG_BANG_SOUND -> "Le son d'un Big Bang ; il se fait entendre dès qu'on le choisit.";
            case METRONOME -> "Le tic du métronome de la boucle (dimension 5) ; le choisir le fait entendre et allume le métronome, éteint au départ.";
        };
    }
}

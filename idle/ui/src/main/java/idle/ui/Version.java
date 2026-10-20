package idle.ui;

import java.util.List;

/**
 * Le numéro de version du jeu et ses notes : ce qui a changé, version après version, de la plus
 * récente à la plus ancienne. Affichés au bas des réglages ({@link SettingsPage}) et joints au
 * rapport de bug ({@link BugReport}).
 *
 * <p>À chaque livraison : ajouter une entrée en tête de {@link #NOTES}, et {@link #CURRENT} suit.
 */
public final class Version {

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
            new Note("0.97", "Une matière noire plus étalée", List.of(
                    "Avant le premier Big Bang, chaque explosion donne ×0,8 de matière noire : le deuxième acte dure six heures "
                            + "environ au lieu de cinq. La matière noire s'affiche avec son dixième.",
                    "Les huit premiers défis se jouent sur un tableau de masse 5 000 au lieu de 1 : quelques minutes chacun au "
                            + "lieu de quelques secondes.",
                    "Le seuil du Big Bang : huit cases nouvelles au bout de l'arbre, six dans la branche des particules "
                            + "(Proto-étoiles, Nuage primordial, Seuil, Point zéro, Fluctuation primordiale, Veille du Big Bang, de "
                            + "10¹⁴⁰⁰⁰ à 10³⁴⁰⁰⁰ particules) et deux dans celle de la taille (Fond diffus, Mur de Planck). Elles "
                            + "jalonnent la montée vers le premier Big Bang, qui demande maintenant 10³⁸⁰⁰⁰ particules.")),
            new Note("0.96", "Le premier élan par paliers", List.of(
                    "Dans la toute première partie, la création prend son élan par paliers : 13 fois moins vite jusqu'au "
                            + "premier atome (1 h), 9 fois jusqu'au cinquième (2 h), 2,8 fois jusqu'au trentième (vers 2 h 40), où "
                            + "s'ouvrent l'automatisation et le tableau. Chaque palier s'annonce.",
                    "Persistance vers 2 h 55, 118 éléments et première explosion vers 5 h 15. Rien ne change après la "
                            + "première explosion.")),
            new Note("0.95", "Le premier élan", List.of(
                    "Jusqu'au tout premier atome d'une partie neuve, la création va 13 fois moins vite : ce début dure environ "
                            + "une heure au lieu de cinq minutes. Le détail des particules l'indique (« dont ÷13, premier élan »).",
                    "Dès le premier atome, la création prend son élan pour de bon, et le jeu l'annonce. Rien ne ralentit plus "
                            + "ensuite : ni les fusions suivantes, ni après une explosion, ni dans l'univers suivant.",
                    "La suite ne bouge pas : 30 atomes vers 1 h 23, Persistance vers 1 h 40, 118 éléments et première "
                            + "explosion vers 4 h (3 h avant).")),
            new Note("0.94", "Fenêtre en Petit", List.of(
                    "À la première ouverture, la fenêtre prend le format Petit (1280 × 800), recentrée sur l'écran et ramenée "
                            + "à sa taille s'il est plus petit. Ensuite, elle se rouvre à la place et à la taille de la dernière fois, ou en Petit si cette place n'est plus sur un écran.")),
            new Note("0.93", "Formats de fenêtre", List.of(
                    "Les formats de fenêtre deviennent Petit (1280 × 800), Moyen (1600 × 900) et Grand (1920 × 1080) ; "
                            + "Compacte disparaît.",
                    "Deux plein écrans : Plein écran fenêtré, la fenêtre agrandie au maximum, et Plein écran, tout l'écran "
                            + "sans barre, dont Échap fait sortir.")),
            new Note("0.92", "Taille de la fenêtre", List.of(
                    "La fenêtre ne se rétrécit plus en dessous de 820 × 600, où tout reste utilisable ; la limite grandit avec "
                            + "la taille de l'interface (984 × 720 en Grande).",
                    "Dans Réglages, Affichage : des formats prédéfinis, Compacte (1024 × 680), Moyenne (1280 × 800), Grande "
                            + "(1600 × 900), Très grande (1920 × 1080) et Plein écran. Un clic redimensionne et recentre la fenêtre ; "
                            + "un format plus grand que l'écran est grisé.")),
            new Note("0.91", "Correctifs", List.of(
                    "L'expansion de la matière ne plante plus quand une place de l'espace reste sans molécule : la place prend "
                            + "une sorte réellement possédée.",
                    "Les étiquettes des cartes n'écrivent plus une couleur invalide (transparence écrite deux fois), qui faisait "
                            + "râler JavaFX dans la console.")),
            new Note("0.90", "Skins plus marqués", List.of(
                    "Faisceau doré : chaque faisceau devient un ruban d'or massif qui remplit l'anneau jusqu'à une grande étoile "
                            + "dorée, qui sème des paillettes ; les arcs du croisement sont dorés.",
                    "Vortex et Pulsar habillent aussi l'onglet Matière noire en permanence : un tourbillon violet qui alimente le "
                            + "point, ou deux faisceaux qui balaient la vue. L'explosion sous Vortex assombrit tout l'écran au lieu de le blanchir.",
                    "Aurore, Supernova et Cristal redessinent tout l'espace du Big Bang : le ciel entier, l'espace créé et son bord.",
                    "Les animations plein écran prennent toute la fenêtre, quelle que soit sa taille, et sont plus longues et plus fortes.")),
            new Note("0.89", "Skins redessinés et fenêtres d'explication", List.of(
                    "Chaque skin est un vrai dessin à lui, plus un simple changement de couleurs : éclairs, cristal à facettes ou "
                            + "lune solitaire pour la fusion ; noyau de protons ou petit soleil et ses planètes pour l'atome ; plaques "
                            + "de plomb ou lingots d'or pour le tableau ; pierre taillée, nuage de gaz ou cœur contracté pour la "
                            + "matière noire ; néon, plasma ou glace pour le collisionneur, et une comète dorée pour le faisceau.",
                    "Vortex et Pulsar remplacent l'onde de l'explosion par une spirale ou deux faisceaux qui balaient l'écran. "
                            + "Aurore, Supernova et Cristal jouent leur animation à chaque Big Bang et habillent le fond de l'espace.",
                    "Chaque skin débloqué s'annonce, avec le bonus total : +2 % par skin, cumulés, qu'il soit choisi ou non.",
                    "À l'ouverture d'un onglet, une fenêtre dit en quelques phrases ce qu'est sa ressource et comment elle marche. "
                            + "Dans le deuxième univers, chaque nouveauté des trois actes s'annonce de même au moment où elle s'ouvre.",
                    "Chaque fenêtre ne s'affiche qu'une fois ; une partie déjà commencée ne revoit pas ce qu'elle connaît.")),
            new Note("0.88", "Les skins à obtenir", List.of(
                    "Dans l'onglet Skins, les dix-neuf skins s'obtiennent : des paliers de matière noire (100, 1 000, 10 000, 100 000 "
                            + "gagnés) et de Big Bangs (1 et 3), qui se débloquent seuls, et un record de reconstruction : de la fusion "
                            + "aux dix générateurs en une minute au plus.",
                    "Deux recherches facultatives : « Fusion prismatique », 1e50 particules ; le noyau dense, ouvert à un million "
                            + "d'atomes créés, quatre paliers payés en atomes (100, 1 000, 100 000, 1e8).",
                    "Sept épreuves volontaires, une à la fois, qu'on peut abandonner sans rien perdre : premier générateur seul, "
                            + "contraction douce, Big Bang à froid, et quatre au collisionneur (détecteur, sans surchauffe, pleine "
                            + "charge, un seul générateur). Chacune donne son skin.",
                    "Un skin choisi change la palette de ce qu'il habille : la fusion, l'atome, le tableau, la boule de matière "
                            + "noire, l'onde de l'explosion, le Big Bang, le collisionneur et son faisceau.",
                    "Les épreuves réussies, les recherches et le record traversent l'univers suivant.")),
            new Note("0.87", "Le deuxième univers", List.of(
                    "Dans l'onglet Univers, « Rouvrir les paliers » : les anciens onglets reviennent pour une partie neuve, celle "
                            + "de l'univers suivant. Former l'univers à la fin de cette partie le crée : une remise à zéro et une "
                            + "graine de plus. Tout ce qui suit ne s'ouvre qu'à partir du deuxième univers.",
                    "L'onglet Skins, sous les réglages, s'ouvre à la cinquième fusion du deuxième univers. Chaque skin débloqué "
                            + "donne +2 % à toutes les ressources d'en dessous de l'univers ; les choisir ne change que l'apparence.",
                    "Régime des générateurs, dès trois générateurs : +15 % de cadence pendant 20 s, puis 40 s de repos, toujours à "
                            + "la main. Synergie par rang, dès le dixième : +0,5 % au plus à chaque générateur.",
                    "Prime de cycle parfait, une fois Persistance achetée : trois fusions de suite sans le plafond d'atomes donnent "
                            + "+10 % de particules pendant une minute, sans se cumuler.",
                    "Dès le premier Big Bang, un petit arbre de trois choix pour le cycle : fusion, synthèse ou réserve. Un seul, "
                            + "rendu gratuitement par le Big Bang suivant.",
                    "Avec la synthèse ciblée, une famille préférée dont les chances montent à chaque tirage au hasard, jusqu'à ×2. "
                            + "Et tous les dix éléments découverts, un élément gratuit : un clic sur un élément pas encore découvert le prend.")),
            new Note("0.86", "Sondes : l'urgence et l'absence", List.of(
                    "Dès le troisième Big Bang, l'onde d'urgence chimique : un voyage de 20 minutes, sans destination, qui ramène "
                            + "un petit lot de l'élément déjà découvert que l'astre visé demande le plus. Jamais un élément inconnu.",
                    "Pendant votre absence, une sonde arrivée vide son butin et repart pour le même voyage : les sondes servent "
                            + "à qui s'absente, sans rien demander à qui reste. Le bilan du retour les compte.",
                    "Les plans de l'arbre et des systèmes ont cinq emplacements de sauvegarde, à choisir dans une liste qui "
                            + "dit ce que chacun contient.",
                    "Profil de test : les bots partis de zéro font maintenant les succès et les défis (44 succès et les 10 "
                            + "défis vers 11 h de jeu, au lieu de 37 et aucun).")),
            new Note("0.85", "Planifier la matière", List.of(
                    "Dès cinq astres formés, l'étoile ☆ d'une carte d'astre l'ajoute au plan, trois au plus. Le plan réunit "
                            + "leurs besoins : ce qui manque d'abord, ce qui est déjà réuni ensuite. Un seuil demandé par plusieurs "
                            + "astres en même temps ne compte qu'une fois ; les ingrédients de leurs assemblages s'ajoutent.",
                    "Plafond par état, dans l'automatisation : la création automatique ne donne pas plus de la part choisie de "
                            + "l'espace créé aux gaz, aux liquides, aux solides, aux cristaux ou aux métaux (de 90 % à 5 %).",
                    "Dès le premier assemblage, la réservation par recette : chaque carte de molécule dit ce qu'il en faut "
                            + "encore pour le prochain assemblage, et prévient quand en créer plus prendrait l'espace qui manque au reste.",
                    "Dès le premier Big Bang, le tableau périodique peut éclairer les éléments utiles au prochain astre et "
                            + "estomper les autres.",
                    "Profil de test : la politique du bot se choisit dans une liste.")),
            new Note("0.84", "Plans de l'arbre et des systèmes", List.of(
                    "Sous l'arbre de matière noire et dans les systèmes solaires, une barre de plan : « Sauvegarder » garde les "
                            + "voies prises (ou la disposition des systèmes), « Charger » la reprend, « Copier le code » et « Coller "
                            + "un code » la font passer en texte. Un plan chargé se suit seul : ce qu'il demande se prend dès que "
                            + "c'est payable ou mûr, et se reprend après chaque Big Bang, jusqu'à « Oublier le plan ».",
                    "« Rouvrir les bifurcations à la prochaine explosion » : un clic, et l'explosion suivante rend les voies "
                            + "prises et leurs liaisons, comme un Big Bang, pour changer de chemin sans attendre.",
                    "Un clic sur une voie, même trop chère ou fermée, surligne sa bifurcation et toutes les cases qu'il "
                            + "faut au-dessus d'elle ; sur une liaison, ses voies et leurs chemins. Un second clic l'éteint.",
                    "Profil de test : le groupe « Bots » fait jouer le bot V2 sur la partie en cours, en direct ou en avance "
                            + "rapide (10 min, 1 h, 6 h, jusqu'au prochain Big Bang), avec la politique de son choix.")),
            new Note("0.83", "L'univers remet tout à zéro", List.of(
                    "Former l'univers remet à zéro tout ce qui y a mené : particules, atomes, matière noire, collisionneur, "
                            + "Big Bangs, molécules, astres et cosmos. Restent les succès, le temps de jeu, les statistiques et "
                            + "tout ce qui appartient à l'onglet Univers.",
                    "Former l'univers donne une graine d'univers. Elle a son sous-onglet, « Graine d'univers » : la graine "
                            + "dorée qui respire, sa pousse qui grandit, et les graines suivantes qui tournent autour. Elle ne "
                            + "sert encore à rien.",
                    "L'écran de fin garde le bilan de la partie d'avant la remise à zéro. Une sauvegarde déjà dans l'univers "
                            + "se remet à zéro à son premier chargement.")),
            new Note("0.82", "Plus léger, rien de changé", List.of(
                    "L'onglet Statistiques ne redessine plus son graphique à chaque image, seulement quand ses points, sa "
                            + "taille ou l'écriture des nombres changent : il coûtait le cinquième de chaque image, à peine "
                            + "plus que les autres onglets maintenant.",
                    "Le jeu ne recompte plus ses succès et ne relit plus les bonus de chaque générateur dans des tables à "
                            + "chaque instant. Des parties entières rejouées avant et après donnent les mêmes sauvegardes : "
                            + "rien ne change dans le jeu.")),
            new Note("0.81", "La partie secondaire : univers, dimensions, Vide", List.of(
                    "L'univers formé, tous les anciens onglets disparaissent, particules et collisionneur compris : restent "
                            + "l'Univers, les succès, les statistiques et les réglages. Leurs raccourcis et leurs annonces se taisent.",
                    "Chaque univers est une bulle ronde, un nuage coloré qui dérive dedans. Les suivants viennent autour, plus "
                            + "petits, reliés par des lignes semi-transparentes ; chaque nouvel univers rejoue la naissance.",
                    "Dès le deuxième univers, le sous-onglet « Dimensions » : dix points autour d'un onzième qui les relie, une "
                            + "dimension de plus par univers. Chacune s'ouvre dans sa fenêtre, avec un bouton « Retour ».",
                    "Les graines d'univers, l'énergie du vide et le Vide, deuxième acte de la partie secondaire ; quatre "
                            + "améliorations, Brane, Bulk, Paysage et Omnivers ; un succès, « Grappe d'univers », à cinq univers.",
                    "Rien de tout cela n'a encore de logique : des outils d'essai créent des univers, des graines, de l'énergie "
                            + "du vide, et font revenir les anciens onglets.")),
            new Note("0.80", "Le quatrième acte : l'onglet Univers", List.of(
                    "Quand l'univers se forme, un nouvel onglet « Univers » s'ouvre, et l'on y va. Sa naissance s'y joue : "
                            + "un point de lumière, l'éclair blanc, une onde qui part du centre, puis les amas de galaxies et leurs "
                            + "filaments qui s'allument à mesure que l'expansion les atteint. L'écran de fin vient après.",
                    "L'onglet montre ensuite la toile de l'univers, qui tourne doucement, et un bouton pour revoir sa naissance. "
                            + "Il n'y a encore rien à y faire : c'est le début du quatrième acte.",
                    "Un réglage « Animations de l'univers » coupe le mouvement ; la naissance passe alors d'un coup.")),
            new Note("0.79", "Une sorte pleine le dit", List.of(
                    "Une sorte de molécule arrivée à un milliard affiche « Pleine : un milliard au plus par sorte », au lieu de "
                            + "réclamer de l'espace qui ne manquait pas, et ne promet plus de gain.",
                    "Les grands comptes de molécules s'écrivent comme les autres grands nombres : sur les cartes (« ×1.00e9 ») "
                            + "et dans ce que demande un astre.")),
            new Note("0.78", "Des boules en feu et de l'électricité", List.of(
                    "Dans l'anneau d'énergie, les boules des faisceaux prennent feu quand elles tournent une fois et demie plus "
                            + "vite qu'au départ (« Faisceaux rapides » niveau 3) : une flamme qui traîne derrière elles, du jaune au "
                            + "rouge, qui vacille et grandit jusqu'au niveau 5.",
                    "À chaque croisement, de l'électricité crépite entre les deux boules et jaillit du détecteur ; plus d'arcs, "
                            + "et plus longs, quand elles sont en feu. Le réglage « Étincelles du collisionneur » la coupe aussi.")),
            new Note("0.77", "Des copies de l'atome au-delà du tableau", List.of(
                    "Le grand atome reste comme avant, à sa taille. Passé 118 atomes, des copies de lui, complètes (noyau, "
                            + "couches et 118 orbes), viennent autour : de tailles variées, jamais plus grandes que lui, vues de biais.",
                    "Une copie dès 119 atomes, quatre de plus par ordre de grandeur, jusqu'à quarante vers cent milliards : la vue, "
                            + "qui prend maintenant toute la largeur, est alors pleine. Les plus petites sont plus pâles et passent derrière.",
                    "Les anneaux de petits atomes de la 0.75 disparaissent.")),
            new Note("0.76", "Plus de plafond à deux milliards de molécules", List.of(
                    "Les molécules ne s'arrêtent plus à deux milliards en tout : les comptes passent en entiers longs, et la "
                            + "sauvegarde les relit. Seule une même sorte reste plafonnée à un milliard.",
                    "Le compte des molécules s'écrit comme les autres grands nombres : « 2.00e9 molécules » au lieu de « 2000000000 ».")),
            new Note("0.75", "Des anneaux d'atomes au-delà du tableau", List.of(
                    "Passé 118 atomes, de petits atomes tournent autour du grand, sur des anneaux inclinés vus de biais, de tailles "
                            + "variées, les plus gros avec un petit noyau doré.",
                    "Un anneau de plus à chaque fois que les atomes en trop sont multipliés par seize environ, jusqu'à neuf : vers "
                            + "quatre milliards d'atomes, l'écran est plein. Le grand atome se resserre pour leur faire place.",
                    "Les anneaux passent devant et derrière le grand atome ; ce qui est derrière est plus pâle.")),
            new Note("0.74", "Des défis Présence et Absence corsés", List.of(
                    "« Présence » et « Absence » s'ouvrent à 300 et 350 matières noires, quand l'arbre a toutes ses voies. De tout "
                            + "l'arbre, seules y agissent les voies et les accords du style, et le tableau pèse trois millions de fois plus.",
                    "Avec les voies du bon style, ils prennent une ou deux minutes ; avec les autres, vingt à quarante. « Branche coupée » "
                            + "et « Tronc nu » s'ouvrent maintenant avant eux, à 24 et 27 matières noires.",
                    "Pour garder un deuxième acte de cinq heures, le premier Big Bang demande 10³²⁰⁰⁰ particules au lieu de 10³⁷⁰⁰⁰.")),
            new Note("0.73", "Un deuxième acte de cinq heures", List.of(
                    "Le deuxième acte dure maintenant 5 h en jouant parfaitement, au lieu d'une heure et demie. Le premier Big Bang demande "
                            + "10³²⁰⁰⁰ particules ; les cases du bas de l'arbre s'étalent jusque-là : Singularité en trente niveaux, "
                            + "Réacteur en huit, Horizon en dix, et les dernières bifurcations bien plus loin.",
                    "Les voies de présence sont plus fortes, exprès : « Croissance continue » et « Poussée » renforcent les paliers de "
                            + "vitesse, ce qui fait grandir une partie qu'on laisse tourner. Les accords de présence rapportent 1 puis 2 "
                            + "matières noires par explosion.",
                    "Les voies d'absence sont plus lentes, mais moins fatigantes : pendant une absence, une bifurcation où vous n'avez "
                            + "pas choisi prend sa voie d'absence, et l'accord d'absence y tente les défis. Le bilan du retour dit "
                            + "désormais les voies prises et les défis lancés, et le défi en cours.",
                    "La Superposition s'ouvre au cinquième Big Bang, au milieu du troisième acte, au lieu de l'amas de galaxies.",
                    "L'amas de galaxies puis l'univers demandent chacun 45 minutes de rassemblement après l'échelle d'avant : la fin "
                            + "ne passe plus en vingt minutes pour qui va très vite.")),
            new Note("0.72", "Une planche claire pour les liaisons", List.of(
                    "Les liaisons et les accords quittent l'intérieur de l'arbre : leurs traits traversaient les colonnes et se "
                            + "lisaient mal. Ils ont leur planche sous la case du Big Bang, un rang par rang de bifurcations, "
                            + "liaisons d'abord, accords ensuite.",
                    "Chaque carte dit ses voies une à une : ✓ prise, ○ à prendre, × fermée jusqu'au prochain Big Bang ; et ce "
                            + "qu'il manque : l'une des deux voies, les trois, ou rien à faire avant le prochain Big Bang.",
                    "L'arbre retrouve sa hauteur d'avant : plus de rangées vides entre les cases. En mode détails, chaque voie "
                            + "dit quelles liaisons et quels accords elle aide à ouvrir.")),
            new Note("0.71", "Deux styles de jeu, des accords, dix défis", List.of(
                    "Dans chaque bifurcation, la voie de gauche est celle de la présence, la voie de droite celle de l'absence : "
                            + "chaque voie d'absence prise rend 5 points de plus du temps passé jeu fermé. La bonne voie dépend "
                            + "maintenant de la façon de jouer.",
                    "Quatre accords, entre les branches : ils demandent trois voies du même style à la fois. Ceux de présence "
                            + "rapportent 2 puis 3 matières noires de plus à chaque explosion. Le premier d'absence fait tenter les "
                            + "défis ouverts pendant l'absence ; le second rend 10 points de plus du temps passé jeu fermé.",
                    "Quatre défis de plus, dix en tout : « Présence » et « Absence » éteignent les voies de l'autre style, « Branche "
                            + "coupée » la branche des particules, « Tronc nu » toutes les voies et liaisons. Leur difficulté dépend "
                            + "du passage pris dans l'arbre. Le Big Bang demande les dix.",
                    "La Superposition du collisionneur attend l'amas de galaxies : jusque-là, les choix de l'arbre comptent.")),
            new Note("0.70", "Liaisons, Superposition, et un arbre sans seuils", List.of(
                    "Trois bifurcations de plus : « Poussée » ou « Écho sombre » (particules, après la Résonance), « Noyau dense » "
                            + "ou « Synthèse double » (atomes, après la Mémoire de la matière), « Abîme » ou « Moisson profonde » "
                            + "(taille, tout au bout). Sept en tout, et toutes nettement plus fortes qu'avant.",
                    "Six liaisons entre les branches, payées en matière noire : chacune s'ouvre dès que l'une des deux voies "
                            + "voisines est prise. Elles se posent dans une rangée sous les bifurcations, à cheval entre deux branches, "
                            + "et repartent au Big Bang avec les voies.",
                    "Une nouvelle amélioration du collisionneur, « Superposition », après la saveur top : les deux voies de chaque "
                            + "bifurcation se prennent, et voies et liaisons restent d'un Big Bang à l'autre.",
                    "Plus aucune case de l'arbre n'attend un nombre de matières noires gagnées : seuls les prix rythment l'arbre, "
                            + "et ils ont été relevés pour qu'il s'étale sur une vingtaine d'explosions.",
                    "Aucune voie ne rapporte plus de matière noire par explosion : « Grandes explosions » devient « Cadence sombre », "
                            + "« Moisson » donne des atomes. Au robot, toutes les voies de gauche ou toutes celles de droite finissent "
                            + "le jeu à cinq minutes près.")),
            new Note("0.69", "Un arbre de matière noire à bifurcations", List.of(
                    "Quatre bifurcations dans l'arbre, deux voies côte à côte : « Croissance continue » ou « Grandes explosions » "
                            + "(particules), « Fusion riche » ou « Synthèse fine » (atomes), « Croissance soutenue » ou « Onde "
                            + "amplifiée » (taille), « Gravité » ou « Moisson » (au-delà du Débordement). Prendre une voie ferme "
                            + "l'autre ; la case d'après demande l'une ou l'autre.",
                    "Le choix se refait à chaque Big Bang, même quand l'arbre est gardé : la voie prise est rendue, ce qui la suit reste.",
                    "Trois cases de plus au bout des branches, au-delà de 70 matières noires : Singularité, Réacteur, Horizon. "
                            + "L'arbre passe de 20 à 31 cases.",
                    "L'automatisme de l'arbre ne prend jamais une bifurcation : c'est au joueur de choisir sa voie.")),
            new Note("0.68", "Une vraie fin de partie", List.of(
                    "La galaxie, l'amas de galaxies et l'univers multiplient enfin : ×4, puis ×2, puis ×2. Avant, ils ajoutaient "
                            + "1 024, 2 048 et 4 096 à des bonus qui valaient déjà plus de cent mille : leur effet réel était d'environ 1 %.",
                    "L'amas demande dix fois plus de matière, et l'univers quatre fois celle de l'amas : la réserve accumulée en "
                            + "attendant les derniers astres ne paie plus tout en quelques minutes.",
                    "Dix jalons : chaque état de la matière réuni pour l'amas, puis pour l'univers, ajoute 10 % à toutes les grandeurs, "
                            + "s'annonce et se marque sur la carte. L'objectif dit l'état le plus en retard.",
                    "Un écran de fin, une fois par partie : le temps de jeu, comparé au record, la durée de chaque acte et quelques "
                            + "chiffres de la partie. Le jeu continue derrière.")),
            new Note("0.67", "Quatre effets de plus, coupés tout de suite", List.of(
                    "Quatre effets visuels de plus à couper : la création de particules (l'onde et les poussières), la "
                            + "matière noire animée, les animations du Big Bang (espace, ciel, systèmes, galaxie) et celles du "
                            + "collisionneur (ondes, envol des quarks, scintillement ; les faisceaux restent, ils servent à viser). "
                            + "Douze en tout, rangés onglet par onglet.",
                    "Un effet coupé s'arrête dès l'image suivante : plus de poussières, d'ondes, d'étincelles ou d'allure qui "
                            + "finissent de s'éteindre, ni en arrivant sur l'onglet. Le fond animé coupé s'efface au lieu de rester figé.")),
            new Note("0.66", "Le record de la fusion la plus rapide se débloque", List.of(
                    "Une fusion de zéro seconde (tout de suite après la précédente, quand tout est gardé) inscrivait un record "
                            + "de 0, que l'écran lit comme « pas encore » et qu'aucune fusion ne pouvait battre. Une durée nulle "
                            + "ne compte plus, et un record à 0 relu d'une sauvegarde est battu par la prochaine fusion. Pareil "
                            + "pour l'explosion et le Big Bang les plus rapides.")),
            new Note("0.65", "Courbes tout de suite, nombres en notation, outils des thèmes", List.of(
                    "Les courbes ont toujours un point de l'instant : une courbe toute neuve (comme celles du collisionneur "
                            + "sur une partie déjà longue, dont les relevés s'espacent de plusieurs minutes) n'est plus vide en "
                            + "attendant le prochain relevé ; elle se remplit à partir de maintenant.",
                    "Collisionneur : tous les nombres (énergie, générateurs, achetés, quarks, prix en quarks, production) suivent "
                            + "la notation choisie au-delà de mille.",
                    "Effets : l'atome de l'onglet Atomes a son propre réglage, « Atome qui tourne », et l'icône de l'onglet se "
                            + "fige avec les autres icônes.",
                    "Thème « Contraste fort » adouci : les tons sombres (orbites de l'atome, cadres) ne disparaissent plus.",
                    "Profil de test : un groupe « Thèmes et notations », pour tout débloquer, tout refermer, ou ouvrir et refermer chacun.")),
            new Note("0.64", "Courbes du collisionneur, records complets, collisionneur plus sobre", List.of(
                    "Statistiques : l'énergie du collisionneur rejoint le graphique de toutes les ressources, et sa courbe "
                            + "d'énergie produite la vue d'ensemble. Le sous-onglet « Collisionneur » a cinq courbes : énergie "
                            + "produite, énergie par seconde, énergie en réserve, collisions, quarks.",
                    "Records : chaque record a sa ligne dès le début, « pas encore » tant qu'il n'est pas tenu (fusion la plus "
                            + "rapide, explosion la plus rapide, chaque défi…).",
                    "Collisionneur : les cartes d'amélioration disent l'essentiel en quelques mots, l'explication complète passe "
                            + "en mode détails, comme les saveurs fermées, le quark top et les annonces.",
                    "Les courbes relues d'une sauvegarde se réécrivent à l'identique (un arrondi pouvait changer d'un chiffre).")),
            new Note("0.63", "Thèmes, notations secrètes, réglages rangés, records", List.of(
                    "Neuf thèmes de plus : Nuit, Aurore, Monochrome et Contraste fort tout de suite ; Sépia, Phosphore, "
                            + "Infrarouge, Nébuleuse et Or à débloquer. Deux notations de plus (en mots, en puissances), et trois à "
                            + "débloquer (Alphabet, Logarithme, Romaine). Chaque déblocage demande quelque chose de très précis : "
                            + "un clic sur un cadenas en donne l'indice.",
                    "Les réglages se rangent en cinq sous-onglets : Affichage, Effets et sons, Jeu, Clavier, Partie. Les "
                            + "effets visuels se coupent un par un (éclair, fond animé, onglet qui bat, icônes, allure des "
                            + "générateurs, étincelles du collisionneur, chute de matière noire).",
                    "La sauvegarde automatique se règle : toutes les 10 s, 30 s, 1 min ou 5 min. Le temps hors ligne peut "
                            + "être coupé : le jeu reprend alors là où il en était.",
                    "Les statistiques gagnent deux sous-onglets : « Collisionneur » (énergie, croisements, collisions, quarks) "
                            + "et « Records », acte par acte (étapes, fusion, explosion et Big Bang les plus rapides, défis).",
                    "Profil de test : deux groupes d'outils de plus, « Collisionneur » et « Actes » (refaire un acte, vider "
                            + "le collisionneur, ouvrir ou refermer les cosmétiques).",
                    "Les textes d'aide, d'objectifs et de détail qui parlaient encore des hadrons ou du plasma sont remis à jour.")),
            new Note("0.62", "Plus de hadrons : l'énergie ouvre les trous noirs", List.of(
                    "Les hadrons disparaissent, avec leurs bonus, leurs paliers et la capture : les quarks servent désormais à "
                            + "payer la chaîne, chaque étape dans sa saveur (u, u, d, s, c, b), en plus de l'énergie.",
                    "Les deux trous noirs demandent un palier d'énergie produite au collisionneur depuis le début : "
                            + "mille milliards pour le trou noir, un million de milliards pour le trou noir supermassif.",
                    "Le sous-onglet « Hadrons » devient « Chaîne » ; les sondes rapportent des quarks c et b dès la saveur charmée.")),
            new Note("0.61", "Particules vivantes, succès rangés, chaîne dans les hadrons", List.of(
                    "Les générateurs de particules suivent leur vitesse : lents, des poussières bleues et un noyau calme ; plus "
                            + "vite, des traînées, des spirales et des anneaux d'accrétion qui tournent ; très vite, une couronne "
                            + "de rayons, des arcs d'énergie, et la couleur qui passe du bleu au violet puis à l'or.",
                    "Le fond de l'onglet des particules file et se teinte avec la production ; l'icône des particules et celle "
                            + "du collisionneur tournent avec leur vitesse ; la matière noire attire en spirale de la matière qui "
                            + "tombe, d'autant plus fort qu'elle grossit vite.",
                    "Les succès du collisionneur passent dans l'onglet « Succès », avec la matière noire ; quatorze succès du "
                            + "Big Bang s'y ajoutent au premier Big Bang (Big Bangs, molécules, astres, galaxie, systèmes, sondes, "
                            + "comètes), chacun renforçant l'espace, les particules, les atomes ou la matière noire.",
                    "La chaîne énergie → particules → atomes → matière noire → espace → molécules prend la place des cartes "
                            + "du sous-onglet « Hadrons » ; les hadrons se forment désormais d'eux-mêmes.")),
            new Note("0.60", "Collisionneur par améliorations, succès du collisionneur, onglets à glisser", List.of(
                    "Les collisions ne viennent plus d'office : l'amélioration « Collisions », première du sous-onglet ouvert "
                            + "par le huitième générateur, donne aux croisements leur chance ; les saveurs s'ouvrent ensuite une à "
                            + "une, avec l'énergie : étrange, charmée, belle, top.",
                    "Les générateurs coûtent moins : les huit arrivent en trois heures environ, au lieu de deux jours.",
                    "Les améliorations se rangent en quatre familles : collisions et saveurs ; faisceaux (vitesse, détecteur, "
                            + "déclencheur) ; générateurs (production ×2 par niveau, un automate par générateur, ouvert un à un) ; "
                            + "et la chaîne énergie → particules → atomes → matière noire → vitesse de l'espace → dédoublement des molécules.",
                    "Nouveau sous-onglet « Succès » : dix-neuf succès du collisionneur, gagnés en achetant des générateurs ; "
                            + "chacun double une production, divise des prix par deux, ou renforce l'énergie ou la chance.",
                    "Un appui long sur un onglet le laisse glisser à une autre place, en haut comme à gauche ; l'ordre est "
                            + "gardé, et un réglage remet l'ordre d'origine.")),
            new Note("0.59", "Collisions au hasard, améliorations du collisionneur, systèmes lisibles", List.of(
                    "Le sous-onglet « Collisions » disparaît : chaque croisement des faisceaux a désormais 15 % de chances "
                            + "de donner une collision, donc un quark, qui s'envole du détecteur à la couleur de sa saveur.",
                    "Un clic au moment du croisement capte trois secondes d'énergie et assure une collision au butin doublé.",
                    "Les saveurs plus lourdes s'ouvrent avec un hadron et un générateur : l'étrange avec un proton et la "
                            + "cavité accélératrice, le charmé avec un kaon et l'anneau de stockage, et ainsi de suite.",
                    "Avec le huitième générateur s'ouvre le sous-onglet « Améliorations » : une seule au début, chacune en "
                            + "ouvrant d'autres (automates des générateurs, chance, saveurs lourdes, capture de hadrons, "
                            + "déclencheur automatique, énergie, espace, top stable).",
                    "Les plasma et matière étrange changent : 20 % de chance de collision, et un quark de plus une fois sur quatre.",
                    "Les systèmes solaires se dessinent : l'étoile, une orbite par planète, verte quand la planète est à sa "
                            + "place et orange sinon, la zone habitable ; trois pas à suivre en haut, et « Ranger au mieux ».",
                    "Chaque onglet a son icône animée et une allure selon son rôle : cadre plein pour les actes, pointillés "
                            + "pour ce qui tourne à côté, sans cadre pour les pages pour s'y retrouver.",
                    "À gauche, les onglets ne bougent plus au clic : la barre du haut garde sa hauteur, et la colonne descend "
                            + "jusqu'en bas de la fenêtre.",
                    "Le lanceur de tests n'emploie plus ce que Gradle 10 refusera.")),
            new Note("0.58", "Paquets thématiques et lanceur de tests", List.of(
                    "Le code est rangé en paquets par thème : particules, atomes, matière noire, collisionneur, Big Bang, "
                            + "molécules, ciel, sondes, sauvegarde… pour s'y retrouver plus vite.",
                    "Un seul clic lance tous les essais, ceux d'aujourd'hui comme ceux à venir : tester-tout.bat, "
                            + "tester-tout.sh, la configuration « Tous les tests » d'IntelliJ ou « gradlew toutTester ».")),
            new Note("0.57", "Collisionneur à part, et onglets à gauche", List.of(
                    "Le collisionneur a son propre onglet, ouvert à la première explosion, avec trois sous-onglets : "
                            + "l'énergie, les collisions, les hadrons.",
                    "L'énergie ne vient plus de la production de particules : les faisceaux croisés la donnent, et huit "
                            + "générateurs s'achètent avec elle, chacun produisant le précédent, de l'aimant supraconducteur "
                            + "au collisionneur de Planck.",
                    "Un clic quand les faisceaux se croisent, sur le grand anneau, capte trois secondes de production.",
                    "Les collisions coûtent désormais 100, 10 000, 100 millions, mille milliards et dix millions de milliards d'énergie.",
                    "Nouveau réglage « Disposition des onglets » : en haut comme avant, ou à gauche, la barre du haut "
                            + "montrant alors les sous-onglets de l'onglet affiché.")),
            new Note("0.56", "Collisionneur et sondes en images", List.of(
                    "Le collisionneur se dessine : un anneau par place, deux faisceaux qui partent de l'injecteur et se "
                            + "croisent dans le détecteur. Un clic sur l'anneau quand ils se croisent tente la résonance ; "
                            + "réussie, des quarks jaillissent.",
                    "Les sondes ont leur carte du ciel : le hangar, une trajectoire par sonde, et la sonde qui avance "
                            + "jusqu'à sa destination.",
                    "Arrivée, la sonde attend : un clic sur elle ouvre son butin au premier plan, une carte à la fois. "
                            + "Chaque clic ouvre la suivante, « Tout ouvrir » ouvre le reste d'un coup. La sonde rentre au "
                            + "hangar une fois tout ouvert.")),
            new Note("0.55", "Réglage de la route vers la galaxie", List.of(
                    "Les Big Bangs six, sept et huit demandent aussi 18, 24 puis 28 astres mûrs : le ciel avance entre les "
                            + "derniers paliers.",
                    "Les astres, à partir des lunes, demandent 50 % de matière et de molécules en plus ; les matières "
                            + "exotiques aussi.",
                    "Maturation un peu plus longue : 4 min pour un amas de roches, 25 pour une planète, 50 pour une étoile ; "
                            + "toujours 2 h pour le trou noir supermassif.",
                    "Collisionneur : l'énergie croît avec le logarithme de la production au lieu de s'emballer (environ 2 à 5 "
                            + "par seconde) ; haute énergie à 5 000, très haute à 40 000, extrême à 250 000.")),
            new Note("0.54", "Paliers 6 à 8 : systèmes solaires, places, matières exotiques", List.of(
                    "Trois paliers de plus, aux Big Bangs six, sept et huit : refaire des Big Bangs jusqu'au huitième a "
                            + "désormais un sens.",
                    "6e Big Bang : la sous-page « Systèmes solaires ». Autour d'une étoile, des planètes rangées de la plus "
                            + "proche à la plus lointaine, et des lunes ; l'harmonie d'un système complet augmente toutes les "
                            + "grandeurs. La galaxie demande trois systèmes complets.",
                    "7e Big Bang : deux places de maturation de plus, et une sonde de plus en vol.",
                    "8e Big Bang : les matières exotiques, cinq astres (pulsar, magnétar, deux nébuleuses, quasar) faits "
                            + "d'astres déjà formés et de beaucoup de matière. La galaxie en compte désormais 35.",
                    "Les défis de Big Bang s'ouvrent toujours au cinquième Big Bang.")),
            new Note("0.53", "Maturation des astres", List.of(
                    "Un astre formé mûrit avant de compter en entier : 3 min pour un amas de roches, 20 min pour une "
                            + "planète, 45 min pour une étoile, jusqu'à 2 h pour le trou noir supermassif.",
                    "En attendant, il donne le quart de son bonus ; il ne compte ni pour les astres qui partent de lui, ni "
                            + "pour la galaxie.",
                    "Trois astres mûrissent à la fois : on choisit lesquels former avant de s'absenter.",
                    "« Accélérer » dépense une charge d'impulsion pour gagner une minute. Une annonce dit quand un astre est mûr.")),
            new Note("0.52", "Sondes", List.of(
                    "Un nouveau palier au troisième Big Bang ouvre la sous-page « Sondes » du Big Bang.",
                    "Une sonde part 15 minutes, 1 heure, 4 heures ou 8 heures, vers le vide intersidéral ou vers la "
                            + "destination d'un rayon ouvert. Plus le voyage est long, plus le butin est riche par heure.",
                    "Au retour, chaque récompense se réclame d'un clic (ou de la touche R), puis la suivante apparaît : "
                            + "éléments du rayon, espace d'un coup, sursaut d'expansion (espace par seconde ×2), échantillons.",
                    "Les échantillons paient cinq améliorations : récompenses plus riches, voyages plus courts, sursauts "
                            + "plus longs, une récompense de plus, une sonde de plus en vol.",
                    "Avec le palier du charme, les voyages d'une heure et plus rapportent aussi des quarks c et b.")),
            new Note("0.51", "Paliers du collisionneur", List.of(
                    "Quatre paliers, gagnés avec les hadrons, au bas de la page du collisionneur.",
                    "Plasma quark-gluon (10 protons et 10 neutrons) : la collision basse énergie se relance d'elle-même "
                            + "dans une place libre ; un bouton la coupe. Elle ne s'annonce plus à chaque fois.",
                    "Matière étrange (5 kaons ou lambdas) : une troisième place de collision.",
                    "Charme (3 J/psi) : les sondes, à venir, rapporteront aussi des quarks c et b.",
                    "Cœur de galaxie (1 upsilon) : le trou noir supermassif le demande. Sans lui, pas de galaxie.")),
            new Note("0.50", "Collisionneur de quarks", List.of(
                    "Une nouvelle sous-page de la matière noire, ouverte à la première explosion : le collisionneur. "
                            + "La production donne de l'énergie, les collisions donnent des quarks, les quarks forment des hadrons.",
                    "Cinq énergies de collision, de 30 secondes à 8 heures, deux à la fois. Chacune s'ouvre avec le hadron "
                            + "de la précédente.",
                    "Les hadrons renforcent la partie pour toujours : particules (protons), atomes par fusion (neutrons), "
                            + "matière noire (kaons, lambdas), espace (J/psi). Au-delà de dix d'une même sorte, chacun compte un peu moins.",
                    "Après un Big Bang, l'onglet de la matière noire reste ouvert pour le collisionneur, même avant la "
                            + "première explosion de la partie.",
                    "La résonance : un clic au bon moment double le butin d'une collision. Le quark top se désintègre en "
                            + "une minute ; lancé à temps, il donne dix minutes de surchauffe, particules ×10.")),
            new Note("0.49", "Achat simple", List.of(
                    "Au-dessus des molécules, une ligne dit ce que demande le prochain pas, rayon par rayon : "
                            + "« Pour Comète glacée : 3 à créer en Sels, 2 en Minéraux · 4 solides à rassembler ».",
                    "Chaque rayon dit combien de ses molécules il faut encore créer.",
                    "« Au hasard dans ce rayon » crée une molécule du rayon, prise d'abord parmi celles que le prochain pas demande : "
                            + "plus besoin de choisir la sorte.",
                    "La création automatique suit la liste de courses d'office : elle crée ce que demande le prochain pas, "
                            + "sans amas à confier. Réglable dans l'onglet Automatisation.")),
            new Note("0.48", "Impulsion", List.of(
                    "Après le premier Big Bang, l'impulsion : chaque clic donne d'un coup dix secondes d'expansion. Une "
                            + "charge revient toutes les trois secondes, quarante au plus en réserve.",
                    "Deux boutons, sur la page Expansion et au-dessus des molécules, et le raccourci I à tenir. "
                            + "En cliquant sans arrêt, l'espace vient jusqu'à quatre fois plus vite ; sans cliquer, rien ne change.")),
            new Note("0.47", "Retours de jeu", List.of(
                    "Plusieurs raccourcis tenus à la fois se répètent ensemble : M et F achètent et fusionnent en boucle.",
                    "Le bouton d'explosion garde sa place entre deux explosions : la fenêtre ne saute plus, et les "
                            + "sous-onglets restent sous la souris quand tout va vite.",
                    "« Automatismes offerts » agit aussi pendant les défis de matière noire.",
                    "L'automatisme de l'arbre monte aussi les cases sans fin (Densité, Noyau lourd), quand un niveau "
                            + "coûte au plus 1 % de ce que possède la branche.",
                    "La liste de courses et le filtre « Utiles au prochain astre » suivent les assemblages et les astres formés.",
                    "Molécules : le bouton « Créer » du meilleur achat reste au même endroit, et « Créer le maximum » crée "
                            + "tout ce que permettent les molécules filtrées ou favorites.",
                    "La page des astres défile d'un bloc, ciel compris : la liste reste accessible dans une petite fenêtre.",
                    "Plus tôt et moins cher : l'achat groupé en atomes s'achète aussi pour 8 atomes, la création continue "
                            + "coûte 100 d'espace et les gestes groupés 1 500.")),
            new Note("0.46", "Technique", List.of(
                    "Des sons, tous fabriqués par le jeu : succès, élément nouveau, explosion, Big Bang, comète, défi réussi. "
                            + "Volume réglable et coupure dans les réglages ; un même son ne revient pas avant une seconde.",
                    "Économie d'énergie : 60, 30 ou 15 images par seconde au choix, 4 quand la fenêtre est derrière une autre, "
                            + "1 quand elle est réduite. Le jeu avance au même rythme.",
                    "Le retour après une longue absence se rejoue environ deux fois plus vite en fin de partie.")),
            new Note("0.45", "Rythme", List.of(
                    "Tout va plus vite : les générateurs créent huit fois plus de particules par seconde dès le départ. "
                            + "La première explosion vient en trois heures environ, au lieu de vingt-quatre.",
                    "L'automatisme « Nouveau générateur » achète d'un coup tous les générateurs que les particules "
                            + "permettent : les parties automatiques ne sont plus freinées par neuf achats un à un.",
                    "Moins de malchance à la synthèse : après deux synthèses sans rien de nouveau, la suivante donne "
                            + "à coup sûr un élément inconnu. La carte de synthèse l'annonce.",
                    "La croissance de la matière noire et le seuil du rendement de fusion ont été recalés sur la "
                            + "nouvelle vitesse : ils gardent l'allure qu'ils avaient.")),
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
    public static final String CURRENT = NOTES.get(0).number();

    private Version() {
    }

    /** Les notes de version en texte : une version par paragraphe, ses changements en dessous. */
    public static String notes() {
        StringBuilder text = new StringBuilder();
        for (Note note : NOTES) {
            if (text.length() > 0) text.append("\n\n");
            text.append(note.number()).append(" — ").append(note.title());
            for (String change : note.changes()) text.append("\n· ").append(change);
        }
        return text.toString();
    }
}

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

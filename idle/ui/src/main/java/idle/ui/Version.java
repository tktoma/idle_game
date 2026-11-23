package idle.ui;

import java.util.List;

/**
 * Le numéro de version du jeu et ses notes : ce qui a changé, version après version, de la plus récente à la plus
 * ancienne. Affichés au bas des réglages ({@link SettingsPage}) et joints au rapport de bug ({@link BugReport}).
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
    static final List<Note> NOTES = joined(List.of(
            new Note("0.296", "L'interface relue de près", List.of(
                    "Mélange, étape 4 : l'aide de la charge donne le vrai plafond (×2.5 après un niveau de « Charge », et ainsi "
                            + "de suite) au lieu de toujours dire ×2.",
                    "Carte des dimensions : les noms et les progressions ne sortent plus de la carte ; trop longs, ils passent sur "
                            + "deux lignes, et la progression d'un point du haut ou du bas se place du côté où il reste de la place.",
                    "Boucle : les cercles s'étalent un peu plus (à sept cercles, 28.8 px d'écart au lieu de 25.6 sur une vue de 480 px).",
                    "Graine : la saison et l'effet du rond de croissance restent dans la vue sur un petit écran ; trop longue, la "
                            + "ligne de la saison passe sur deux lignes.",
                    "Les nombres à deux décimales s'arrondissent comme on l'attend : 0.455 s'affiche 0.46 (avant 0.45).",
                    "Big Bang : les sous-onglets se resserrent, puis prennent un nom court (Expansion, États, Systèmes ; le nom "
                            + "entier en bulle d'aide), pour tenir sur une ligne dès 1 100 px de large.",
                    "Fenêtre des dimensions : la vue garde au moins 35 % de la hauteur (200 px au moins) ; ce qui ne tient pas "
                            + "dessous défile. En 820 × 600, la vue du mélange tombait à 18 px de haut, celles de la grille et "
                            + "du miroir à 120 px ; à la taille par défaut, rien ne change.")),
            new Note("0.295", "Moins de dessin caché", List.of(
                    "La fenêtre d'une dimension recouvre tout le jeu : tant qu'elle est ouverte, ce qu'elle cache (le fond animé, "
                            + "les icônes des onglets, l'onglet en cours, la carte des dimensions ou la graine) ne se redessine "
                            + "plus. Rien ne change à l'écran ; la carte des dimensions au maximum coûtait à elle seule une dizaine "
                            + "de millisecondes de rendu par image, pour rien.",
                    "Les couleurs écrites en clair dans le dessin des skins (atome, arbre, univers, Big Bang, matière noire, "
                            + "collisionneur, espace) ne sont plus relues à chaque image. Les images sont identiques au pixel près.")),
            new Note("0.294", "Les bots jouent ce qu'ils oubliaient", List.of(
                    "Rien ne change en jeu. Les profils de bots, sauf le Speedrunner (la référence de l'équilibrage, inchangée), "
                            + "jouent maintenant le calme du Big Bang, les épreuves de l'onglet Skins et le rond de croissance de la graine.",
                    "Les épreuves qui ralentissent tout un acte ne sont tentées que par le chasseur de succès et le collectionneur ; "
                            + "celles du collisionneur seulement avec le déclencheur automatique au plus haut.",
                    "Univers 2, avec / sans : Régulier 26 h 41 / 27 h 17, Méthodique 33 h 22 / 33 h 22, chasseur de succès "
                            + "29 h 38 / 27 h 02, Astronome 25 h 53 / 25 h 53.",
                    "Les autres mécaniques notées comme oubliées étaient déjà jouées (accélérer la maturation, les recherches, "
                            + "la graine et le chrono, les planètes en double par le placement des systèmes).")),
            new Note("0.293", "Le code rangé, encore", List.of(
                    "Rien ne change en jeu. Plus aucun fichier ne dépasse 1 000 lignes : les quinze qui le faisaient sont "
                            + "découpés en chaînes de classes, une par thème, comme en 0.098 (la vue des dimensions en huit, "
                            + "l'univers du moteur en sept, la fenêtre des dimensions et la graine en quatre…). Le reste du jeu, "
                            + "les tests et les bots les appellent comme avant.",
                    "Les notes de version les plus anciennes, de la 0.199 à la 0.034, sont rangées dans deux fichiers à part.")),
            new Note("0.292", "Les décimales avec un point", List.of(
                    "Tous les nombres à virgule s'écrivent maintenant avec un point, comme la plupart l'étaient déjà : "
                            + "« ×1.5 », « 0.5 s », « 4.1 % ». Avant, certains textes écrivaient « ×1,5 » ou « 12,5 % » : "
                            + "aides, cartes, hadrons, vide, dimensions, statistiques, multiplicateurs, historique des achats, "
                            + "notes de version.",
                    "Seul le fichier des images par seconde du profil test UI garde la virgule, pour s'ouvrir tel quel dans "
                            + "un tableur réglé en français.")),
            new Note("0.291", "Le mode détails partout", List.of(
                    "La ligne et le plan ont enfin leur paragraphe de détails, comme les huit autres dimensions : le trajet "
                            + "des boules et ce que rapporte un trait, l'apparition des éclats du plan, ce qui traverse les univers "
                            + "et ce que leurs éclats paient dans la graine.",
                    "Des cartes sans explication en ont une : les améliorations de la graine (niveau sur le maximum, ce qui "
                            + "les paie, elles restent d'un univers à l'autre), celles des sondes, les paliers de gluons (d'où "
                            + "viennent les gluons, ce qu'un nouvel univers en fait), la fusion prismatique et la recherche sur le "
                            + "noyau dense (les skins de chaque palier), et les épreuves (une à la fois, l'abandon, l'échec au "
                            + "bout du temps, le skin, le rejeu).")),
            new Note("0.290", "Des noms plus justes", List.of(
                    "Des noms qui se répétaient d'un endroit à l'autre sont séparés : dans l'arbre de matière noire, "
                            + "« Inflation » devient « Inflation sombre », « Gravité » « Puits de gravité » et « Noyau dense » "
                            + "« Noyau compact » (l'inflation de l'espace, le palier « Gravité » et la recherche sur le noyau dense "
                            + "gardent leur nom) ; le succès « Horizon » devient « Horizon des événements ». Dans l'onglet Big Bang, "
                            + "la sous-page « Univers » devient « Cosmos », pour ne plus se confondre avec l'onglet Univers.",
                    "Deux épreuves du collisionneur changent de nom : « Défi du détecteur » devient « Cinq de suite », et "
                            + "« Un seul générateur » devient « Chaîne arrêtée », avec une description qui dit enfin ce qu'elle fait "
                            + "(seul le premier générateur produit). Les skins les appellent toutes « épreuve », plus « défi ».",
                    "Des skins quittent les noms de marque et l'anglais : Briques (ex-Lego), Pense-bête (ex-Post-it), "
                            + "Photo instantanée (ex-Polaroid), Jetons de lettres (ex-Lettres de Scrabble), Avis de recherche "
                            + "(ex-Wanted, l'affiche dit « RECHERCHÉ »), Plan d'architecte (ex-Blueprint), Planétaire mécanique et "
                            + "Globe planétaire (ex-Orrery), Émoji, Adapté aux daltoniens (ex-Daltonien sûr). Rien n'est perdu : "
                            + "un skin débloqué ou choisi le reste.",
                    "Dans l'onglet Skins, les visuels ont tous leur article (« L'atome », « Le collisionneur »…) ; « L'arbre » "
                            + "devient « L'essence de l'arbre » et « L'arbre de fantaisie » « La matière de l'arbre », comme le dit "
                            + "leur description.",
                    "Textes corrigés : l'aide des sons (plus de délai d'une seconde entre deux sons, les sons ajoutés depuis, "
                            + "les paires du vide), le succès « Razzia » (le raccourci « Tout acheter avec les particules »), le "
                            + "bouton « Lancer l'univers suivant » dans l'aide.")),
            new Note("0.289", "Relecture du code", List.of(
                    "Rien ne change en jeu. Le code des sons, passé au-dessus de 1 000 lignes avec les enregistrements, est "
                            + "coupé en deux : la fabrication des sons (lecture des fichiers, mise à niveau, synthèse) d'un côté, "
                            + "leur lecture de l'autre.",
                    "La base de la fenêtre repasse sous 1 000 lignes : les gestes de sauvegarde des réglages et les outils "
                            + "d'affichage du panneau de test rejoignent la partie « fenêtre et sauvegarde ».",
                    "Commentaires remis à jour (les sons ne sont plus tous synthétisés), imports inutiles retirés.")),
            new Note("0.288", "Le profil test UI saute les sous-onglets sans visuel", List.of(
                    "Un sous-onglet qui n'a aucun visuel de l'onglet (les améliorations du collisionneur, du vide et du Big "
                            + "Bang, la chaîne du collisionneur, les paliers des hadrons et du Big Bang) ne se passe plus : seuls "
                            + "les skins de la fenêtre y changent, et c'est le passage « Fenêtre » qui s'en occupe.",
                    "Moins d'étapes dans ces onglets, à la charge maximale aussi.")),
            new Note("0.287", "Le profil test UI ne croise plus les sous-onglets entre eux", List.of(
                    "Un visuel ne se croise plus qu'avec ce qui se voit en même temps que lui : les skins de l'atome ne se "
                            + "croisent plus avec ceux du tableau périodique, la boule de matière noire plus avec l'arbre ni les "
                            + "défis, les astres plus avec les sondes, et ainsi de suite dans chaque onglet.",
                    "Le passage se range par sous-onglet : tous les états de l'atome d'abord, puis ceux du tableau. Chaque "
                            + "part a ses combinaisons ; le thème, la notation et ce qui couvre toute la fenêtre se croisent dans "
                            + "chacune. Les animations plein écran (le Big Bang, la naissance d'un univers…) ne croisent que leurs skins.",
                    "Bien moins d'étapes : le Big Bang tout croisé passe de plusieurs années à quelques jours. Le panneau dit "
                            + "combien de parts compte l'onglet.")),
            new Note("0.286", "La charge maximale du profil test UI", List.of(
                    "Le profil test UI a une option « Charge maximale » : les états de l'onglet choisi sont remplacés par "
                            + "l'onglet au plus chargé que la partie peut atteindre, pour voir chaque skin et mesurer les images "
                            + "par seconde là où la vue a le plus à dessiner.",
                    "Par onglet : 30 générateurs au dernier palier de vitesse (et leur fusion) ; l'atome à 118 orbes et ses 40 "
                            + "copies, le tableau rempli de chaque exemplaire ; la plus grosse boule de matière noire, l'arbre "
                            + "rempli, chaque défi ; le collisionneur et le vide au maximum, le trou noir plein ; quarks et hadrons "
                            + "en masse ; tout le Big Bang formé ; plus d'univers que la vue n'en dessine ; l'arbre de la graine "
                            + "au maximum ; chaque dimension au maximum ; cinq notifications d'un coup.",
                    "Le fichier des images par seconde dit « charge maximale » pour ces passages.")),
            new Note("0.285", "Plus de mémoire pour les tests de l'interface", List.of(
                    "Les tests de l'interface ont 2 Go par JVM (1 Go ne suffisait pas sous Windows), une JVM neuve toutes les "
                            + "10 classes, et au plus deux JVM à la fois.")),
            new Note("0.284", "Les tests de l'interface ne manquent plus de mémoire", List.of(
                    "Les tests de l'interface repartent d'une JVM neuve toutes les 25 classes : ce qu'ils gardaient en mémoire "
                            + "d'un test à l'autre ne s'accumule plus (un des derniers tests manquait de mémoire en CI).")),
            new Note("0.283", "Le dernier test de l'interface", List.of(
                    "Le test du mélange automatique attend le texte d'aujourd'hui (« sans attente, il mène le point… »), qui dit "
                            + "le palier depuis les paliers de cadence.",
                    "Les tests de l'interface ont 1 Go de mémoire au lieu de 512 Mo : la CI en manquait.")),
            new Note("0.282", "Des tests qui disent pourquoi", List.of(
                    "Un test qui échoue écrit maintenant dans la console son nom, l'erreur, sa cause et sa pile : en CI, le "
                            + "journal ne disait que « There were failing tests ».")),
            new Note("0.281", "Tests de l'interface, suite", List.of(
                    "Quatre tests de l'interface suivent les changements récents : les paliers de celui qui joue sont sur une "
                            + "seule carte (brane), le compte des automatismes achetés compte aussi les améliorations (clicker, "
                            + "grille), et le texte d'un onglet n'est dans son bouton qu'une fois affiché (rond au clic).",
                    "Le test du mélange automatique dit maintenant ce qu'il voit quand il échoue, pour trouver la cause.")),
            new Note("0.280", "Tests de l'interface corrigés", List.of(
                    "Quatre tests des dimensions (brane, clicker, grille, miroir) coupaient seulement le palier le plus rapide de "
                            + "celui qui joue : les paliers plus lents jouaient encore. Ils coupent maintenant tous les paliers, "
                            + "comme le bouton « Couper ».",
                    "Le test de l'ordre des onglets attendait les particules juste après les statistiques ; les réglages et les "
                            + "skins s'y rangent d'abord, comme le jeu le veut.")),
            new Note("0.279", "Volumes et quark top", List.of(
                    "Le son d'un succès est moins fort (−6 dB), et celui du nouvel élément encore moins (−12 dB au lieu de −6).",
                    "Le quark top ne fait plus de son.",
                    "Le fond des paires du vide monte avec le remplissage du trou noir du vide : comme avant à vide, jusqu'à "
                            + "6 dB de plus (à peu près deux fois plus fort) une fois plein.")),
            new Note("0.278", "Hadrons, sondes et autres annonces", List.of(
                    "Les hadrons créés font le son choisi par le joueur (une montée de 2 secondes), deux au plus à la fois.",
                    "Chaque carte de récompense d'une sonde qu'on ouvre fait le son d'achat ; « Tout ouvrir » le joue une fois "
                            + "par carte, à la suite.",
                    "Les annonces qui n'avaient pas de son en ont un, choisi par le joueur (un court son doux), quand rien "
                            + "d'autre ne sonne ; il suit le réglage « Autres annonces ».")),
            new Note("0.277", "Les sons des jalons, des skins et des succès", List.of(
                    "Un jalon fait maintenant le son choisi par le joueur (un court son de bascule, 0.22 s), à la place des "
                            + "deux notes ; toutes les annonces de jalon le partagent (cosmos, ensembles, paliers de gluons, "
                            + "racines liées, nouveaux titres, premier atome…).",
                    "Un skin débloqué fait le son choisi par le joueur (deux déclics, l'un à gauche, l'autre à droite), à la "
                            + "place des trois notes.",
                    "Un succès fait le son choisi par le joueur (un court déclic), à la place des deux notes ; le palier du "
                            + "collisionneur et l'essai du volume dans les réglages, qui partageaient ce son, aussi.")),
            new Note("0.276", "Deux volumes retouchés", List.of(
                    "Le son du nouvel élément est moins fort (−6 dB, à peu près deux fois moins fort à l'oreille).",
                    "Le fond des paires du vide est un peu plus fort (+5 dB).")),
            new Note("0.275", "Les skins de la fusion suivent les paliers", List.of(
                    "Sous chaque skin de la fusion des particules, les ondes partent maintenant du bord et se resserrent sur "
                            + "le noyau, comme au classique : le son d'une particule tombe quand son onde le touche (avant, elles "
                            + "s'élargissaient depuis le centre).",
                    "Les skins montrent aussi le palier de vitesse : leurs traits s'épaississent à chaque palier, et un rond "
                            + "fin par palier passé entoure le noyau, dans la couleur du skin (en carrés pour le Pixel).")),
            new Note("0.274", "Le bong des particules", List.of(
                    "Une particule récupérée fait maintenant le « bong » choisi par le joueur (une note courte, 0.12 s), à la "
                            + "place de l'ancien fa ; comme avant, il est plus aigu aux paliers lents et plus grave aux paliers "
                            + "rapides, où il s'allonge un peu (jusqu'à 0.4 s).")),
            new Note("0.273", "Le son du nouvel élément", List.of(
                    "Un nouvel élément fait maintenant le son choisi par le joueur (1.5 s, en stéréo), à la place de la petite "
                            + "cloche ; l'élément unique et l'astre mûr, qui partageaient ce son, aussi.")),
            new Note("0.272", "Le fond des paires du vide", List.of(
                    "Sur le sous-onglet Paires du vide, le son choisi par le joueur tourne en boucle en fond (un souffle grave, "
                            + "toutes les 0.8 s) ; le son de chaque paire qui naît reste par-dessus.",
                    "Les collisions du collisionneur ne sonnent plus que sur son sous-onglet Énergie, comme son fond.")),
            new Note("0.271", "Les sons des défis", List.of(
                    "Un palier du défi final atteint fait maintenant le son choisi par le joueur (1.7 s), au volume des sons "
                            + "rares, à la place des deux notes synthétisées.",
                    "Un défi réussi (de matière noire ou de Big Bang) fait le son choisi par le joueur, un court accord de "
                            + "confirmation, à la place des trois notes ; un record battu aussi, qui partageait ce son.",
                    "Un défi qui s'ouvre (« Nouveau défi ») ou qui commence fait le son choisi par le joueur, un court son "
                            + "de goutte, à la place des deux notes.",
                    "Correction : le son du palier de vitesse des particules (0.264) ne partait pas, le jeu jouait encore "
                            + "le petit bip ; il part maintenant.")),
            new Note("0.270", "Le son de « Revoir la fin »", List.of(
                    "« Revoir la fin » rejoue aussi le son de la fin de la partie et l'élan de la musique, comme la première fois.")),
            new Note("0.269", "Un bouton pour couper les paliers", List.of(
                    "Sous la carte des paliers de celui qui joue une dimension, un bouton « Couper » met tous ses paliers en "
                            + "pause, même quand le palier suivant est payable ; en pause, il devient « Relancer ».")),
            new Note("0.268", "Les paliers sur une seule carte", List.of(
                    "Dans l'automatisation des dimensions, les cinq paliers de celui qui joue une dimension (5 s, 2 s, 1 s, "
                            + "0.5 s, sans attente) tiennent sur une seule carte : elle dit le palier acheté (« 2/5 ») et le prix du "
                            + "suivant. Payable, un clic l'achète ; sinon, un clic met en pause ou relance tous les paliers ensemble.",
                    "Un palier acheté pendant une pause reste en pause. Les sauvegardes gardent leurs paliers achetés.")),
            new Note("0.267", "Une fin à choisir", List.of(
                    "La fin de la partie a le son choisi par le joueur (9.6 s, il continue sur l'écran de fin).",
                    "Dans l'animation de la fin, la vue toute jaune tient une seconde avant l'écran de fin (7.5 s en tout).",
                    "L'écran de fin propose deux boutons : « Recommencer de zéro », qui efface toute la partie comme dans les "
                            + "réglages (un second clic le confirme), et « Continuer la sauvegarde », qui la garde.",
                    "Les skins de l'écran de fin (Générique qui défile, Certificat encadré, Photo souvenir) sont retirés : l'écran "
                            + "de fin suit le skin des panneaux. Une sauvegarde qui en avait un choisi revient au Classique.")),
            new Note("0.266", "La fin de la partie garde son accord", List.of(
                    "Le son du joueur ne sert qu'à la formation d'un univers : la fin de la partie (les dix paliers atteints) "
                            + "reprend l'accord d'avant, qui s'élargit.")),
            new Note("0.265", "Le son de l'univers formé", List.of(
                    "Former un univers fait maintenant le son choisi par le joueur (5 secondes, en stéréo), "
                            + "au volume des sons rares, à la place de l'accord synthétisé.")),
            new Note("0.264", "Le son du palier de vitesse", List.of(
                    "Un palier de vitesse des particules fait maintenant le son choisi par le joueur, au volume des sons rares, "
                            + "à la place du petit bip. Les annonces de collision gardent le petit bip, à part ; si les deux "
                            + "arrivent ensemble, c'est le palier qui sonne.")),
            new Note("0.263", "Le son de l'achat", List.of(
                    "Un achat (une carte, une amélioration) fait maintenant le son choisi par le joueur, un clic plus rond, à la "
                            + "place du petit bip ; au même volume que les autres sons répétés.")),
            new Note("0.262", "Un clic, un son, partout", List.of(
                    "Chaque clic fait le son du clic, qu'il tombe sur rien ou qu'il déclenche une action (une carte, un "
                            + "sous-onglet, un interrupteur…), dès l'appui. Seuls les onglets et les mini-jeux des dimensions, qui "
                            + "ont leur propre son à l'appui, ne le doublent pas ; un bouton actionné au clavier fait aussi le clic.")),
            new Note("0.261", "Le rond au clic", List.of(
                    "Chaque clic fait un petit rond qui s'élargit et s'efface là où il tombe, pour voir ses clics (Réglages, "
                            + "effets : « Rond au clic », allumé d'office).",
                    "Un clic sur rien (un fond, un texte) fait maintenant le son du clic ; les boutons, les cartes, les "
                            + "réglettes et les vues de jeu gardent leur propre son, sans doublon.")),
            new Note("0.260", "L'horloge du chrono", List.of(
                    "Le chrono joue le son du joueur, une vieille horloge un peu boiteuse, à la place du tic-tac synthétisé : "
                            + "une boucle de six tics sans coupure, toujours plus pressée quand le chrono accélère (jusqu'à ×3).")),
            new Note("0.259", "Le clic des zones monte", List.of(
                    "Dans les zones ×2 du clicker, le clic monte d'un demi-ton par zone superposée, jusqu'à huit zones : une "
                            + "montée légère qui suit le multiplicateur, en plus d'une note de l'arpège par zone (jusqu'à quatre). "
                            + "Le panneau de test (F9) joue les huit.")),
            new Note("0.258", "Le clic du joueur", List.of(
                    "Chaque bouton et chaque clic sur le clicker jouent le clic choisi par le joueur (Kenney, domaine public), "
                            + "à la place des deux clics synthétisés ; les onglets gardent leur son.",
                    "Dans une zone ×2, l'accord du clicker reprend ce clic, une note de plus par zone superposée ; le clic "
                            + "étant très bref, les notes partent l'une après l'autre (un arpège rapide), sinon l'accord ne "
                            + "s'entendait plus.")),
            new Note("0.257", "Un autre son pour le palier de la graine", List.of(
                    "Le palier de la graine joue le nouveau son du joueur, sans le passage trop fort de l'ancien : une "
                            + "montée qui culmine puis s'éteint, au même niveau que les autres sons rares.")),
            new Note("0.256", "Le palier de la graine se fait entendre", List.of(
                    "Le son d'un palier de la graine joue à chaque palier passé, d'où qu'il vienne : l'arrosoir, le tuyau, ou "
                            + "la graine qui rattrape ses arrosages quand un univers se forme. Il ne jouait que sous l'arrosoir "
                            + "tenu, et la graine, vite à son plafond, passait ses paliers ailleurs : on ne l'entendait presque "
                            + "jamais.")),
            new Note("0.255", "Le palier de la graine enregistré", List.of(
                    "Un palier de la graine (quand elle passe une étape sous l'arrosoir que tient le joueur) joue le son du "
                            + "joueur, en stéréo, à la place des quatre notes synthétisées ; son début calé, sa fin fondue.")),
            new Note("0.254", "La nuit de jungle à la place de la cigale", List.of(
                    "Le beau temps sur la graine joue le son du joueur, une nuit humide de jungle (oiseaux, insectes), à la "
                            + "place de la cigale : en stéréo, une boucle de 20 s sans coupure, toujours 8 dB sous les autres "
                            + "fonds (moins de la moitié de leur force).")),
            new Note("0.253", "Profil test UI : les images par seconde", List.of(
                    "Le profil test UI mesure les images par seconde de chaque combinaison de skins, sur tous ses états : la "
                            + "moyenne, le plus bas (la pire image, et l'état où elle tombe), le calcul du jeu par image, et à part "
                            + "la mise en place de chaque état. Son panneau montre le moment présent, la combinaison d'avant et la "
                            + "plus lente.",
                    "Chaque combinaison mesurée ajoute une ligne au fichier test-ui-fps.csv, dans le dossier des données du "
                            + "jeu (%APPDATA%\\Idle sous Windows, ~/.idle ailleurs), avec la date, la version et le plafond d'images "
                            + "par seconde ; un tableur l'ouvre. « Copier le chemin du fichier » le donne.")),
            new Note("0.252", "Profil test UI : tout ce qui touche l'onglet", List.of(
                    "Le panneau du profil test UI a une option par onglet : « Avec tout ce qui touche l'onglet ». Cochée, elle "
                            + "croise aussi, avec les visuels de l'onglet, ceux de la fenêtre qui s'y voient (fond, onglets et "
                            + "leurs icônes, cartes des améliorations et leurs signes, info-bulles, curseur, cadre de l'avatar, "
                            + "notifications, panneaux), le thème et la notation des nombres ; chacun a sa case.",
                    "Les thèmes et notations essayés ne s'écrivent pas dans les réglages du joueur : le profil test UI ne "
                            + "garde aucun réglage.")),
            new Note("0.251", "Plus de skins de son", List.of(
                    "Les skins de son sont retirés : les banques (Bois, Verre, 8 bits, Orchestre, Nature, Silence partiel), "
                            + "les sons du Big Bang (Carillon géant, Souffle de vent, Orgue) et les tics du métronome (Bois, Métal, "
                            + "Goutte d'eau, Battement de cœur). Chaque son a une seule voix ; l'onglet Skins n'a plus de "
                            + "catégorie Sons.",
                    "Une sauvegarde qui les avait les oublie sans erreur. Les treize skins en moins rangent les autres "
                            + "autrement entre les univers : quelques skins pas encore débloqués peuvent venir un univers plus "
                            + "tôt ou plus tard.")),
            new Note("0.250", "L'onglet Skins par catégorie", List.of(
                    "L'onglet Skins a des sous-onglets : Collection (la carte de la collection, les recherches, le record, les "
                            + "épreuves), puis Fenêtre, Particules et atomes, Matière noire, Collisionneur et hadrons, Le vide, Big "
                            + "Bang, Univers, Graine, Dimensions, Profil et statistiques, Sons ; chacun dit ses skins débloqués sur "
                            + "ceux montrés.",
                    "Un skin ne se montre qu'une fois son palier atteint : l'univers qu'il attend, et pour le skin d'une "
                            + "dimension, cette dimension ouverte. Un skin déjà débloqué se montre toujours ; un visuel ou une "
                            + "catégorie sans skin montré restent cachés.")),
            new Note("0.249", "Profil test UI : tous les onglets", List.of(
                    "Dans le profil test UI, tous les onglets sont ouverts, et la vue va d'elle-même à l'onglet et au "
                            + "sous-onglet de chaque étape : la partie préparée a son univers formé, et le jeu cachait les anciens "
                            + "onglets et ramenait la vue sur l'onglet Univers à chaque image.")),
            new Note("0.248", "Profil test UI ; Neige et Sablier retirés", List.of(
                    "Les animations de matière noire « Neige » et « Sablier » sont retirées ; restent Vortex et Pulsar. Une "
                            + "sauvegarde qui les avait les oublie sans erreur, et celle qui en avait choisi une revient au Classique.",
                    "Nouveau profil test UI (tâche Gradle runTestUi, ou profil-test-ui.bat / .sh) : une partie préparée (chaque "
                            + "acte ouvert, tous les skins), sans gains ni sauvegarde. Son panneau, à droite, passe chaque "
                            + "combinaison de skins d'un onglet par chacun de ses états (1, 2, 5, 10, 30 générateurs, la fusion, "
                            + "chaque sous-onglet, chaque dimension…), tout croisé ou par paires, en lecture automatique à "
                            + "l'allure choisie, et relève les erreurs (exception, vue vide, sous-onglet absent) dans un rapport à "
                            + "copier.")),
            new Note("0.247", "Touches de la boucle, vitesse du miroir, sons", List.of(
                    "La boucle : ses touches (S, D, F, Espace…) jouent de nouveau les cercles. Un champ de texte (la recherche "
                            + "du panneau de test, les chiffres de la grille) gardait le clavier ; un clic ailleurs le rend au jeu.",
                    "Le miroir : une barre « Vitesse du motif » dans sa fenêtre, de la mémorisation entière (à gauche) à "
                            + "0.3 s par case (à droite). Elle se garde dans les réglages.",
                    "La cigale du beau temps est 8 dB plus douce (moins de la moitié de son amplitude). La comète (et le "
                            + "quark top, les sondes revenues), le nouvel élément, le palier de vitesse (et l'annonce d'une "
                            + "collision) et le tic-tac du chrono sonnent une octave plus grave.",
                    "Les paires du vide ne sonnent que sur le sous-onglet « Paires ». Plus aucun son n'attend un délai avant "
                            + "de revenir ; seul le nombre de sons en même temps est borné (24).")),
            new Note("0.246", "L'animation du Big Bang", List.of(
                    "Le Big Bang sans skin a maintenant son animation plein écran (3.5 s) : la nuit tombe et la matière des "
                            + "quatre ressources tombe vers un point qui s'allume et tremble ; puis l'éclair, l'onde de choc et la "
                            + "matière projetée jusqu'aux bords, une boule de feu qui refroidit du blanc au violet, et les étoiles "
                            + "et les premières galaxies qui s'allument.",
                    "Elle se coupe avec le réglage « Éclair de l'explosion », comme celles des skins. Le panneau de test (F9) "
                            + "la joue avec « Big Bang (skin choisi, ou le classique) ».")),
            new Note("0.245", "Le clic du clicker", List.of(
                    "Choisi à l'écoute : chaque clic du joueur sur le clicker fait un tapotement doux, comme un doigt sur "
                            + "du bois, plus grave que l'essai.",
                    "Un clic dans une zone ×2 joue ce même tapotement en accord, une note de plus par zone superposée "
                            + "(tierce, quinte, octave, dixième) : le cumul s'entend. Les clics automatiques restent muets.")),
            new Note("0.244", "Le décollage des sondes", List.of(
                    "Choisi à l'écoute : une sonde lancée décolle dans un rugissement grave qui monte, puis s'éloigne en "
                            + "s'assourdissant (3 s, au niveau des grands moments). Les banques de sons des skins gardent le leur.")),
            new Note("0.243", "Les sons de la graine", List.of(
                    "Choisis à l'écoute, sur la graine qu'on voit : tant qu'on tient l'arrosoir, de grosses gouttes graves "
                            + "qui tombent dans une flaque ; tant que le tuyau arrose, un gargouillis sourd ; pendant l'orage, "
                            + "une pluie feutrée et un grondement très bas ; pendant le beau temps, une cigale qui chante par "
                            + "à-coups.",
                    "Un rond de croissance cueilli : une grosse bulle d'eau qui remonte. Une saison qui commence, sur "
                            + "l'onglet de l'univers : un accord de bois, un par saison.",
                    "La goutte jouée à chaque arrosage disparaît : l'eau est désormais un fond continu. Le palier de la "
                            + "graine sonne toujours s'il vient de l'arrosoir qu'on tient.")),
            new Note("0.242", "La corde de la brane", List.of(
                    "Choisis à l'écoute : tant qu'on tire la brane, des fibres qui craquent, de plus en plus serrées, aiguës "
                            + "et fortes avec la tirée ; au relâchement, la membrane qui claque (un coup sec, puis elle ondule), "
                            + "plus aiguë et plus forte selon la tirée.")),
            new Note("0.241", "Le mélange sonne de nouveau, le chrono tourne", List.of(
                    "Le mélange sonne à chaque éclat ramassé, par le point ou par l'onde de la brane. Aux étapes avancées, il "
                            + "se taisait : ce qu'un éclat rapporte va dans la charge, ou est trop petit pour bouger le total des "
                            + "fragments, sur lequel se réglait le son.",
                    "Le métronome de la boucle bat à chaque huitième de tour (un quart avant).",
                    "Le chrono fait entendre un tic-tac tant qu'il accélère le jeu, plus pressé avec l'accélération (×3 au "
                            + "plus) ; il se coupe avec sa catégorie, « Le chrono qui tourne ».")),
            new Note("0.240", "Des sons pour chaque dimension", List.of(
                    "Le métronome de la boucle bat de nouveau : un tic à chaque quart de tour du premier cercle. Il battait au "
                            + "centre des zones, mais une zone touchée part ailleurs, et le point n'en passait presque plus aucun.",
                    "La graine ne sonne que sous l'arrosoir qu'on tient : la goutte à chaque arrosage, le palier s'il arrive "
                            + "pendant qu'on arrose. Le tuyau, les gains et les outils d'essai arrosent en silence.",
                    "La brane : tant qu'on tire, une corde se tend, plus aiguë et plus forte avec la tirée ; relâchée, la "
                            + "membrane part d'autant plus fort et aigu que la tirée était forte.",
                    "La durée : pendant l'appui, une note tenue monte jusqu'au plus long appui possible.",
                    "Une sonde lancée a son son (catégorie « Sondes lancées »).",
                    "Le miroir joue au piano (Salamander Grand Piano V3, Alexander Holm, CC BY 3.0, voir l'aide, « Crédits ») : "
                            + "chaque case a sa note, la même que son reflet ; le motif se joue en s'allumant, se rejoue à l'entrée "
                            + "de la dimension s'il attend, puis chaque case reproduite rejoue sa note ; une faute, deux notes graves.",
                    "La grille : un clic hors de la boîte des chiffres (x, y, z) quitte le champ en cours.",
                    "Panneau de test : le piano du miroir, la corde de la brane et l'appui de la durée s'écoutent aussi.")),
            new Note("0.239", "Le panneau de test", List.of(
                    "Profil de test (runTest) : la barre du haut laisse place à un panneau à droite, qui s'ouvre et se ferme "
                            + "par sa poignée ou par F9. En haut, la vitesse du jeu (de figé à ×1 000, avec ×0.25 et ×0.5), "
                            + "une recherche dans tous les outils, le bot de la partie et le résumé ; dessous, une section par "
                            + "onglet du jeu (celle de l'onglet affiché passe en tête, ouverte), puis le temps et l'avance rapide "
                            + "(+10 s à +24 h, sans bloquer la fenêtre, annulable), les bots, les dimensions, les sons, les "
                            + "animations et les paliers.",
                    "Nouveaux outils : chaque son du jeu, chaque palier du son des particules, les gestes et les points de "
                            + "chaque dimension, chaque fond sonore à faire jouer partout ; les animations de la fusion et du "
                            + "nouvel atome ; les hadrons ; la vitesse des faisceaux ; l'arbre vide, les automatismes coupés ; "
                            + "les remises à zéro de chaque palier, secondaires compris (graine, vide, bulles de l'arbre, défi "
                            + "final, chaque dimension, toutes les dimensions, la partie secondaire, une nouvelle partie).",
                    "Chaque fenêtre de dimension a sa bande de test, en bas : sa ressource et son seuil, ses améliorations, "
                            + "sa remise à zéro, son bot (de la ligne à la durée) et ce qu'elle a en propre (les étapes du "
                            + "mélange, le rond de croissance, la météo).")),
            new Note("0.238", "Le fond de l'expansion", List.of(
                    "Le fond de la 0.237 change de place : il tourne sur le sous-onglet « Expansion de la matière » du Big "
                            + "Bang, plus sur Astres. Sa catégorie devient « L'expansion de la matière (son de fond) ».")),
            new Note("0.237", "Le fond du ciel", List.of(
                    "Sur le sous-onglet Astres du Big Bang, ton enregistrement tourne en boucle (4.1 s de sa partie tenue, "
                            + "la fin fondue dans le début), en stéréo, au niveau des sons répétitifs. Il entre et sort en "
                            + "fondu, et se coupe avec sa propre catégorie : « Le ciel (son de fond) ».")),
            new Note("0.236", "Les particules à l'heure", List.of(
                    "Le son d'une particule tombe avec l'onde qui atteint le noyau : sa montée du début (0.04 s, plus "
                            + "longue aux paliers graves) est coupée.",
                    "Les particules de générateurs différents se superposent : seules celles du même instant (à moins de "
                            + "0.05 s) n'en font qu'une. Au plus six sonnent en même temps.")),
            new Note("0.235", "La collision plus franche", List.of(
                    "Le fond du collisionneur ne joue plus que sur son sous-onglet Énergie, celui des faisceaux.",
                    "Le son d'une collision est un peu plus fort (4 dB au-dessus des autres sons répétitifs), et son choc "
                            + "tombe tout de suite : la montée presque inaudible du début (0.11 s) est coupée.")),
            new Note("0.234", "Le fond du collisionneur", List.of(
                    "Sur l'onglet du collisionneur, ton enregistrement tourne en boucle (1.5 s, sa fin fondue dans son début) "
                            + "tant que les faisceaux tournent, lu à leur vitesse : ×1 au départ, ×2 avec les cinq niveaux de "
                            + "« Faisceaux rapides », en glissant. Il se coupe avec la catégorie du collisionneur.",
                    "Un croisement capté au milieu sonne maintenant, même avant l'amélioration « Collisions » : il ne "
                            + "donnait pas de collision, donc pas de son.")),
            new Note("0.233", "Le son des collisions", List.of(
                    "Une collision réussie du collisionneur (un croisement qui donne un quark, ou capté au milieu) joue ton "
                            + "enregistrement, en stéréo, au niveau des sons répétitifs ; un croisement raté reste muet. "
                            + "Seulement sur l'onglet du collisionneur, comme avant ; les banques de sons des skins gardent le leur.")),
            new Note("0.232", "Le quark top et les sondes sonnent de nouveau", List.of(
                    "La 0.231 est annulée : les outils d'essai restent retirés de l'onglet Univers, comme en 0.229.",
                    "La 0.226 est annulée : un quark top du collisionneur et des sondes qui reviennent sonnent de nouveau, "
                            + "comme avant, quel que soit « Sons des automatismes ». Le son qui revenait sans cesse venait des "
                            + "achats automatiques, coupés depuis la 0.228.")),
            new Note("0.231", "Les outils d'essai reviennent", List.of(
                    "Le retrait de la 0.229 est annulé : le cadre des outils d'essai revient sous les bulles de l'onglet "
                            + "Univers, avec « Montrer les anciens onglets », qui permet de retrouver le collisionneur et les "
                            + "autres onglets dans la partie secondaire.")),
            new Note("0.230", "Le son de l'atome créé", List.of(
                    "Sur l'onglet Atomes, quand un atome est créé (le grand atome grossit), ton enregistrement sonne : en "
                            + "stéréo, au niveau des sons répétitifs, rangé avec « Fusions (atomes) » dans les réglages. Il "
                            + "sonne aussi pour une fusion automatique, comme les particules, mais seulement sur le grand atome. "
                            + "Les banques de sons des skins ont le leur.")),
            new Note("0.229", "Plus d'outils d'essai dans l'onglet Univers", List.of(
                    "Le cadre « Outils d'essai, sans logique » sous les bulles des univers est retiré (+1 / −1 univers, +10 "
                            + "graines, +10 énergie du vide, revoir la naissance, montrer les anciens onglets, tout remettre à "
                            + "zéro). Dans la partie secondaire, les anciens onglets restent cachés ; ils reviennent quand on "
                            + "lance l'univers suivant.")),
            new Note("0.228", "Les achats automatiques se taisent", List.of(
                    "Ce qu'amène un automatisme en marche ne sonne plus, sauf si « Sons des automatismes » est activé : un "
                            + "palier de vitesse atteint par l'achat automatique de la vitesse, un automatisme offert par la "
                            + "matière noire, un élément ou un ensemble de la synthèse automatique. Ils revenaient à chaque partie. "
                            + "Les notifications restent ; atteints à la main (automatisme coupé ou en pause), ils sonnent.")),
            new Note("0.227", "Des sons au même niveau", List.of(
                    "Les sons rares (Big Bang, univers formé, explosion du tableau, défis, succès, skins, comètes, éléments, "
                            + "jalons, paliers…) sonnent tous aussi fort ; les sons qui reviennent sans cesse (particules, fusion, "
                            + "collisions, hadrons, vide, arrosage, clics, onglets, achats, interrupteurs, gestes et points des "
                            + "dimensions, grondement de la matière noire) sonnent tous aussi fort entre eux, nettement plus bas.",
                    "Le Big Bang et l'explosion du tableau sont un peu moins forts qu'avant, la fusion bien moins forte ; les "
                            + "clics et les achats un peu plus audibles. Dans toutes les banques de sons. La musique et le "
                            + "métronome gardent leurs réglages.")),
            new Note("0.226", "Moins de sons qui reviennent sans cesse", List.of(
                    "Un quark top du collisionneur et des sondes qui reviennent ne sonnent plus, sauf si « Sons des automatismes » "
                            + "est activé : en fin de partie, ils faisaient retentir « Autres annonces » toutes les 45 secondes environ. "
                            + "Les comètes, qui se saisissent à la main, sonnent toujours.")),
            new Note("0.225", "Les sons des automatismes, au choix", List.of(
                    "Réglages, « Sons et musique » : « Sons des automatismes », coupé au départ. Coupé, une fusion automatique "
                            + "garde son animation sans son, et une explosion automatique reste muette ; activé, elles sonnent comme "
                            + "celles du joueur.")),
            new Note("0.224", "Les automatismes agissent en silence", List.of(
                    "Ce que font les automatismes ne lance plus de son : une fusion automatique garde son animation dans "
                            + "l'onglet des particules, mais sans son ; une explosion automatique (automatisme de matière noire) reste "
                            + "muette. La fusion, l'explosion et le Big Bang ne sonnent que quand le joueur les fait.")),
            new Note("0.223", "Les sons partent avec leurs animations", List.of(
                    "La fusion des générateurs a un nouveau son, enregistré, qui part quand les générateurs commencent à se "
                            + "regrouper (au bouton comme en fusion automatique).",
                    "L'explosion du tableau sonne au départ de l'onde, et non plus une seconde plus tard quand le tableau explose "
                            + "au plus fort de l'éclair ; le Big Bang, au moment même où il a lieu. Une explosion automatique, sans "
                            + "animation, sonne toujours.")),
            new Note("0.222", "Une vraie explosion pour le tableau", List.of(
                    "L'explosion du tableau périodique a un nouveau son : une explosion enregistrée, en stéréo, au niveau de "
                            + "l'ancienne. Les banques de sons des skins gardent la leur, et le silence partiel la coupe toujours.")),
            new Note("0.221", "Les paliers de vitesse des particules", List.of(
                    "Chaque générateur montre son palier de vitesse, quatorze en tout : de la vitesse de base à 10 par seconde, "
                            + "puis 100, 10⁴, 10⁸… (la vitesse élevée au carré à chaque palier) jusqu'à 10⁴⁰⁹⁶, la vitesse de la fin "
                            + "du premier univers. Le noyau glisse du presque blanc au noir et garde un rond par palier franchi ; le "
                            + "trait des poussières s'épaissit, et l'allure (spirale, anneaux, rayons, arcs) repart de zéro à chaque palier.",
                    "À chaque particule, une onde part du bord et se resserre sur le noyau ; quand elle l'atteint, un nouveau son "
                            + "joue, une note plus grave à chaque palier (gamme pentatonique de fa). Les banques de sons des skins "
                            + "gardent le leur.",
                    "L'animation suit maintenant la vraie vitesse, sans le plafond d'un milliard par seconde d'avant.")),
            new Note("0.220", "Les sons ne dépendent plus des notifications", List.of(
                    "Les sons des grands moments (Big Bang, explosion, univers formé, succès, défis…) partent de ce qui s'est passé, "
                            + "plus de sa notification : une catégorie de notifications coupée, ou une annonce fondue dans « … et N autres "
                            + "nouvelles », ne coupe plus son son. Seuls les réglages des sons décident. Ce que le deuxième univers cache "
                            + "des anciens onglets ne s'affiche toujours pas et ne sonne pas.")),
            new Note("0.219", "Le Big Bang se fait entendre", List.of(
                    "Correctif : quand un Big Bang arrivait avec plus de quatre annonces (records battus, défi réussi, skins…), "
                            + "il finissait dans « … et N autres nouvelles » et ne sonnait pas. Le son se choisit maintenant sur toutes "
                            + "les annonces.",
                    "Si la sortie audio refuse la stéréo, le Big Bang part en mono au lieu de couper tous les sons.")),
            new Note("0.218", "Un Big Bang qui explose", List.of(
                    "Le Big Bang a un nouveau son : une vraie explosion enregistrée, en stéréo, avec l'accord du Big Bang qui monte "
                            + "par-dessus. Les banques de sons et les sons de Big Bang choisis dans les skins gardent les leurs.")),
            new Note("0.217", "Noyaux sombres en haut de l'arbre", List.of(
                    "Arbre de la matière noire : ★ Noyaux sombres, la case qui compte le plus, se pose seule tout en haut, sous "
                            + "la racine, avant que l'arbre ne se divise en branches. La colonne de la matière noire commence "
                            + "ensuite par Rouages sombres.")),
            new Note("0.216", "La matière noire gronde", List.of(
                    "Sur l'onglet de la matière noire, tant qu'elle grossit (appui tenu, verrouillé ou automatique), un grondement "
                            + "tourne en boucle. Il va de plus en plus vite, et monte, avec l'élan de la croissance : trois fois plus vite "
                            + "à l'élan qu'elle a à la fin du premier univers, et pas plus ensuite. Il se tait en pause et au Big Bang.",
                    "Réglages : « Matière noire qui grossit » est une catégorie de sons à part, qui se garde ou se coupe d'un clic.")),
            new Note("0.215", "Un code plus court et mieux rangé", List.of(
                    "Rien ne change en jeu : les parties se jouent à l'identique, au même rythme. Le code du jeu a perdu "
                            + "environ 8 000 lignes sur 149 000 (méthodes inutilisées retirées, commentaires sans l'historique "
                            + "des versions, mise en page resserrée), et l'état de la partie a son propre paquet.",
                    "Les anciennes sauvegardes se relisent comme avant.")),
            new Note("0.214", "Un jeu un peu plus léger", List.of(
                    "Un pas de jeu en partie avancée prend 12 % de temps en moins, et l'onglet des particules calcule la "
                            + "production une fois par image au lieu de trois. Rien ne change dans les calculs : une partie se joue "
                            + "à l'identique.",
                    "Le skin choisi pour chaque partie de l'écran (une dizaine par image) se retrouve directement, sans passer "
                            + "les deux cents skins en revue ; les annonces de skins débloqués ne les repassent que s'il y en a un nouveau.",
                    "Les réglages, l'ordre des onglets, les titres et les avatars se relisent tous de la même façon : une valeur "
                            + "d'une autre version du jeu revient à celle d'origine, comme avant.")),
            new Note("0.213", "Des bruitages pour tout ce qui se passe, et le choix des sons à garder", List.of(
                    "Bruitages nouveaux : une particule créée et une fusion (sur leurs onglets), une collision du collisionneur, "
                            + "des hadrons créés, une paire du vide, un arrosage et un palier de la graine, chaque point gagné dans "
                            + "une dimension ouverte (une note par dimension), un clic sur un bouton, un changement d'onglet. Ce qui "
                            + "revient sans cesse ne sonne que sur l'onglet ou dans la fenêtre où on le voit, et pas trop souvent.",
                    "Réglages : chaque catégorie de sons se garde ou se coupe d'un clic (particules, fusions, explosions, Big "
                            + "Bangs, univers, dimensions, achats, clics, onglets, collisions, hadrons, vide, graine, autres annonces).",
                    "Onglet Univers : les quatre améliorations Brane, Bulk, Paysage et Omnivers, sans prix ni effet depuis la "
                            + "0.081, sont retirées. Leurs niveaux, dans une ancienne sauvegarde, sont ignorés.",
                    "Textes corrigés : l'automatisation des dimensions dit que chaque automatisme se paie avec la ressource de sa "
                            + "propre dimension (depuis la 0.209) ; la fenêtre de l'univers formé ne parle plus d'améliorations.")),
            new Note("0.212", "La musique choisie à l'écoute", List.of(
                    "La façon de jouer la musique a été choisie à l'écoute, parmi quatre séries de variantes : tempo fixe de 69 dans "
                            + "toutes les scènes (plus aucune accélération), une nouvelle scène qui attend la fin de la section (plus "
                            + "aucune phrase coupée), des cloches de verre plus basses (au plus le do à 523 Hz), une mélodie plus douce "
                            + "et plus grave, moins d'écho, et un fond très doux qui respire : chaque accord s'éteint deux temps avant "
                            + "le suivant.",
                    "Grésillement corrigé : une nappe encore tenue pouvait être coupée net quand trop de notes avaient été lancées "
                            + "(un claquement, et un trou dans l'accord) ; le froissement des contretemps est retiré. Plus de marge avant "
                            + "la saturation.")),
            new Note("0.211", "La musique réécrite : des grilles d'accords, des mélodies qui reviennent", List.of(
                    "La musique de fond est écrite comme un morceau : chaque scène a sa grille de quatre accords (I-V-vi-IV pour "
                            + "l'élan, i-VII-VI-VII pour un défi, i-VI-III-VII pour le défi final, IV-I pour la fin…), les nappes passent "
                            + "d'un accord au suivant par le plus court chemin en gardant les notes communes, la basse tient la "
                            + "fondamentale, l'arpège ne joue que les notes de l'accord.",
                    "La mélodie est faite de motifs qui reviennent (A, A, B, A), avance surtout par degrés, se pose sur l'accord à "
                            + "chaque temps fort, ne saute jamais de plus d'une quinte, respire en fin de phrase et se tait une section "
                            + "sur trois.",
                    "Plus rien ne glisse de hauteur : une nouvelle scène ou une nouvelle tonalité attend la mesure suivante. Une "
                            + "réverbération et un écho donnent l'espace ; les nappes ne battent plus, la basse et la pulsation sont "
                            + "plus discrètes, les cloches plus douces.")),
            new Note("0.210", "Une musique qui suit la partie, des sons pour tous les grands moments", List.of(
                    "Musique de fond, composée par le jeu pendant qu'on joue (aucun fichier) : nappes, basse, arpèges, cloches et "
                            + "scintillement. Chaque étape d'un univers a sa scène (l'aube avant la première explosion, l'élan, les Big "
                            + "Bangs, les astres, l'amas, la formation, l'univers formé), un défi la resserre, une dimension ouverte a son "
                            + "arpège, le défi final et la fin ont la leur ; chaque univers change de tonalité, et un Big Bang ou un "
                            + "univers formé la fait monter un moment. Les changements glissent sans couper.",
                    "Réglages : la musique a son interrupteur et son volume, à part des sons (activée, 30 % au départ).",
                    "Nouveaux sons : univers formé et fin de la partie, palier du défi final, skin débloqué, défi qui s'ouvre ou "
                            + "commence, matière noire (Noyaux sombres à portée, automatisme de matière noire, palier de taille), jalons "
                            + "(cosmos, gluons, racines, ensembles, titres, premier atome). Les skins de banque de sons les jouent dans "
                            + "leur timbre.",
                    "Sons d'interface, plus discrets : un achat ou un interrupteur sur une carte, et chaque geste dans le mini-jeu "
                            + "d'une dimension, un timbre par dimension (un tir dans le volume, un clic sur la surface, un sondage de la "
                            + "grille, la membrane relâchée…). Le silence partiel les coupe.",
                    "Tests : les systèmes du jeu combinés de toutes les façons (skins, galaxie, matière noire, univers, bulle, "
                            + "atomes, Big Bangs) gardent leurs règles ensemble.")),
            new Note("0.209", "Noyaux sombres d'abord, le bonus des skins sans plafond, des automatismes des dimensions à gagner", List.of(
                    "Arbre de matière noire : la colonne payée en matière noire passe en tête, et « Noyaux sombres » doit être "
                            + "prise avant tout autre achat en matière noire (accords et liaisons compris) ; les autres cartes disent "
                            + "« Après ★ Noyaux sombres ». L'automatisme de l'arbre prend son premier niveau. Les niveaux déjà pris "
                            + "restent.",
                    "Skins : le bonus de +2 % par skin n'a plus de plafond (cent skins, ×3, jusqu'ici). Il n'est plus écrit sur "
                            + "chaque carte : une carte « Collection », en tête de l'onglet, donne le nombre de skins débloqués puis "
                            + "le bonus cumulé.",
                    "Automatismes des dimensions : ils se paient avec la ressource de leur propre dimension (les segments pour la "
                            + "ligne, les facettes pour le plan…), au lieu de celle de la dimension d'avant et des graines d'univers : "
                            + "il faut jouer une dimension à la main avant de l'automatiser.",
                    "Ceux qui jouent la dimension à votre place viennent maintenant en dernier, les plus chers, en cinq paliers : "
                            + "une action toutes les 5 s, puis 2 s, 1 s, 0.5 s, et le dernier sans attente. Le plus rapide acheté "
                            + "joue ; aucune bulle ni la matière noire ne change leur cadence. Une partie qui avait l'automatisme de "
                            + "jeu d'une dimension garde les cinq paliers.",
                    "Ceux qui achètent les améliorations d'une dimension gardent de quoi payer son automatisme suivant.",
                    "Robots de mesure : ils achètent la bulle du jeune arbre, à droite, après les autres, sans son effet (pour "
                            + "le calcul) ; ils jouent les dimensions à la main, une à la fois. Prix réglés pour un joueur appliqué : "
                            + "le premier palier qui joue vers dix minutes de jeu à la main, le dernier vers une demi-heure. La "
                            + "partie de référence forme ses onze univers en 89 h 01 (89 h 07 en 0.208).")),
            new Note("0.208", "Rien de changé au jeu : des mesures trois fois plus rapides, onze univers mesurés", List.of(
                    "Le jeu se joue exactement comme en 0.207. Les robots de mesure n'achètent plus la bulle du jeune arbre, à "
                            + "droite : elle triplait leur temps de calcul pour 13 minutes gagnées sur 87 heures.",
                    "Mesuré sur les onze univers : les trois derniers se jouent en dix minutes, puis deux heures d'attente où "
                            + "rien de ce que fait le joueur ne compte. Une piste pour l'équilibrage.")),
            new Note("0.207", "Rien de changé au jeu : ce qui ne sert plus, mesuré univers par univers", List.of(
                    "Le jeu se joue exactement comme en 0.206. La mesure de ce qui ne sert plus est refaite avec le bot qui tient "
                            + "la touche M, et étendue aux six premiers univers.",
                    "Dès le deuxième univers, plus rien de ce que fait le joueur ne compte après la galaxie : la fin de chaque "
                            + "univers n'est que de l'attente. Une piste pour l'équilibrage.")),
            new Note("0.206", "Rien de changé au jeu : le bot de référence tient la touche M", List.of(
                    "Le jeu se joue exactement comme en 0.205. Le bot le plus rapide des mesures tient maintenant la touche M "
                            + "(tout acheter) entre deux décisions, comme un joueur attentif, et la lâche une fois l'amas formé.",
                    "Son premier univers se forme en 25 h au lieu de 35 h, pour le même temps de calcul. Les prochaines mesures "
                            + "d'équilibrage partent de cette partie-là.")),
            new Note("0.205", "Rien de changé au jeu : le poids de l'inutile dans une simulation, et la touche M", List.of(
                    "Le jeu se joue exactement comme en 0.204. Un second outil de mesure, du côté des bots, chronomètre le premier "
                            + "univers avec et sans ce que la 0.204 a mesuré inutile : 40 % de calcul en moins, pour deux minutes "
                            + "de jeu de retard.",
                    "Trouvé au passage : depuis la 0.167, un automatisme n'achète qu'un niveau par action. Le premier univers se "
                            + "forme en 25 h quand on tient la touche M (tout acheter), en 35 h quand on appuie une fois toutes "
                            + "les 20 s.")),
            new Note("0.204", "Rien de changé au jeu : une mesure de ce qui ne sert plus, automatismes, améliorations et mécaniques", List.of(
                    "Le jeu se joue exactement comme en 0.203 : mêmes règles, mêmes prix, mêmes textes. Cette version ajoute un "
                            + "outil de mesure pour l'équilibrage, du côté des bots.",
                    "L'outil rejoue la fin du premier univers en coupant une chose à la fois (un automatisme, une amélioration, "
                            + "une mécanique) depuis sept moments de la partie, et note à partir de quand la couper ne retarde plus "
                            + "rien. Les tableaux sont dans le README des bots.",
                    "Pour le permettre, le moteur sait tenir un automatisme éteint le temps d'une mesure. Ce réglage n'existe que "
                            + "dans les mesures : il ne s'enregistre pas dans la sauvegarde et le jeu ne s'en sert jamais.")),
            new Note("0.203", "Retours sur les skins : six retirés, plus de fonds clairs, le chrono rangé, des skins plus légers", List.of(
                    "Six skins retirés : l'Œuf, la Pluie et le Feu d'artifice de la matière noire ; le Ballon noué, la Planète-fruit "
                            + "et l'Œuf des bulles des univers. Une sauvegarde qui en avait un de choisi revient au Classique pour ce "
                            + "visuel. Il reste 363 skins ; la Boule à neige et le Globe d'orrery viennent aux univers 2 et 3. Le bonus "
                            + "des skins ne baisse que pour une partie qui comptait moins de cent skins hors de ces six.",
                    "Plus de grand fond clair dans une fenêtre sombre : l'Encre de Chine, le Papier plié et la Carte ancienne du Big "
                            + "Bang, l'Encre du collisionneur, la Carte au trésor du tableau et l'Empreinte digitale de la galaxie "
                            + "gardent leur dessin, en version nuit (papier noir, feuille bleu nuit, vélin sombre à l'encre d'or).",
                    "Le chrono ne flotte plus en bas à gauche, par-dessus les cartes et la colonne des onglets : il a sa ligne, sous "
                            + "celle de l'objectif, et la même dans l'en-tête de la fenêtre d'une dimension. Il ne recouvre plus rien.",
                    "La galaxie garde son skin au milieu de l'amas (elle redevenait la spirale classique dès l'amas formé). La Grappe "
                            + "de raisin est une vraie grappe : des grains serrés, une rafle, une feuille et une vrille.",
                    "L'arbre de matière noire : chaque skin habille aussi le fond de l'arbre et ses en-têtes, et ses vignettes sont "
                            + "plus grandes. Avant, seuls les traits changeaient.",
                    "Le plan en Feuille de cahier : la feuille est couchée et fuit vers le fond, comme la grille du Classique. Le "
                            + "Coquillage est une conque dont le canal finit là où l'eau sort ; la Fontaine a une borne et un bec, d'où "
                            + "part le jet.",
                    "Les curseurs Arrosoir et Loupe se montrent sur tous les onglets, comme les autres (ils ne se voyaient que sur "
                            + "l'Univers et le Big Bang). Un son choisi dans l'onglet Skins se fait entendre tout de suite ; choisir un "
                            + "tic allume le métronome de la boucle, éteint au départ.",
                    "Avec les onglets à gauche et le skin Pastilles, la colonne prend la largeur des pastilles au lieu de garder un "
                            + "grand bloc vide. L'onglet Skins écrit sous chaque visuel où il se voit et ce qu'il change (les signes "
                            + "des états de la matière, le cadre des mini-jeux, purement décoratif…).",
                    "Des skins plus légers à dessiner. Mesuré image par image : les cartes Étiquette triplaient presque le temps de "
                            + "rendu d'une page chargée (elles coûtent maintenant comme le Classique), le Kaléidoscope du mélange le "
                            + "doublait (+11 % à présent), le Pin coûtait moitié plus que les autres essences (+34 %).",
                    "Profil de test : les outils d'un groupe défilent dans une zone bornée (le groupe Univers ou Dimensions poussait "
                            + "le haut de la barre hors de l'écran, les autres groupes devenaient inaccessibles), une page plus haute "
                            + "que la fenêtre ne passe plus sur les onglets, et un groupe « Animations » rejoue l'explosion, le Big "
                            + "Bang, la naissance et le lancement d'un univers, la transition et la fin, dans le skin choisi.",
                    "Les 32 tests de l'interface qui échouaient depuis la 0.182 passent : la plupart vérifiaient une règle d'avant "
                            + "(les automatismes des dimensions), quatre montraient un vrai défaut (la neige de la Comète restait "
                            + "sous le Laser, les bulles de l'arbre Inversé étaient mal placées, le feuillage du Saule trop bas, "
                            + "le Baobab trop lourd).",
                    "Le rendu réel n'a pas été vu pendant le développement : seulement des captures prises sous un écran virtuel.")),
            new Note("0.202", "Un moteur 40 % plus rapide, les mêmes nombres", List.of(
                    "Rien ne change dans le jeu. Le bonus des skins se relit à chaque fusion : son compte se garde maintenant tant "
                            + "que les skins débloqués ne changent pas, au lieu de repasser sur les quelque 370 skins à chaque fois. "
                            + "La synthèse lit la chance des familles rares et le double tirage sans recalculer tous les bonus des "
                            + "éléments. Les mêmes nombres au bit près, de 40 % de calcul en moins en fin de partie : le jeu reste "
                            + "fluide plus longtemps quand le tableau se remplit vite.",
                    "Les bots : quand plus rien dans l'univers ne demande d'éléments, ils éteignent la fusion, la synthèse et "
                            + "les achats automatiques, et les rallument dès qu'un besoin revient (10 à 15 % de calcul en moins).")),
            new Note("0.201", "Correctif : le jeu ne se lançait plus depuis la 0.194", List.of(
                    "Au lancement, la page du Big Bang plantait (NullPointerException sur les dessins des cartes de molécules) : le "
                            + "constructeur de la classe mère dessinait les cartes avant que la liste qui garde leurs dessins soit créée. "
                            + "La liste se crée à la demande.",
                    "Un test construit désormais chaque page et chaque panneau de la fenêtre, sur une partie neuve et sur une partie "
                            + "avancée, comme le jeu le fait au démarrage (ConstructionDesPagesTest).",
                    "Le rendu réel n'a pas été vu pendant le développement.")),
            new Note("0.200", "Treize skins de sons, huit thèmes et six notations : la liste de SKINS.md est entièrement réalisée", List.of(
                    "Trois visuels de plus dans l'onglet Skins, tous synthétisés sans fichier audio. La banque de sons change le timbre de "
                            + "tous les sons : Bois (xylophone et claves) à une heure de jeu, Verre (carillon, une octave plus haut) à deux, "
                            + "8 bits (ondes carrées) à cinq, Orchestre (cordes en dents de scie, attaque lente, vibrato) à dix, Nature (gouttes, "
                            + "vent, pépiements pour la comète) à vingt ; Silence partiel, libre et sans bonus, ne laisse sonner que les Big Bangs.",
                    "Le son du Big Bang : Carillon géant (trois grandes cloches), Souffle de vent, Orgue (un accord de tuyaux) à 5, 10 et 20 "
                            + "Big Bangs ; le grondement d'avant reste le Classique. Le tic du métronome de la boucle : Bois (bloc de bois) au seuil "
                            + "de la boucle, Métal, Goutte d'eau et Battement de cœur (deux coups sourds) à six, sept et huit univers formés. Le son "
                            + "du Big Bang et le tic choisis passent devant la banque.",
                    "Huit thèmes de plus dans les réglages, en filtre comme les autres, avec un voile nouveau qui colore les noirs : Encre (noir "
                            + "et blanc tranché) à mille fusions, Cyanotype (bleu de Prusse #1a3a6b et blanc) au défi Rouille réussi, Vieux film "
                            + "(sépia sombre et contrasté ; sans grain, le filtre n'en fait pas) à un Big Bang fait entre minuit et six heures, "
                            + "Thermique (fausses couleurs) à trois systèmes solaires complets, Lune (gris bleuté) à dix comètes, Forêt (verts) à "
                            + "la galaxie formée, Brasier (rouges et oranges) à cent explosions ; chacun avec son énigme. Daltonien (saturation "
                            + "baissée, rouges et verts écartés, contraste monté) est libre : de l'accessibilité.",
                    "Six notations de plus, chacune par une énigme : Binaire (1.21×2^10, puis 2^282 361 pour les plus grands nombres) à "
                            + "exactement 1 024 particules, Hexadécimale (0x4D2 jusqu'à 1e15, puis ×16^…) à exactement 255 de matière noire, "
                            + "Chiffres japonais (万, 億, 兆… jusqu'au 無量大数, puis ×10^…) à dix mille molécules créées, Morse (la mantisse en "
                            + "points et traits, l'exposant en clair) à cent notifications reçues d'une traite, Préfixes SI (k, M, G… jusqu'au "
                            + "quetta, puis scientifique) à 1e30 particules, Battements de cœur (les nombres en scientifique, les durées en "
                            + "battements, un par seconde) au chrono plein.",
                    "La liste de SKINS.md est entièrement réalisée depuis la 0.185 : 369 skins, dix-neuf thèmes et quatorze notations.",
                    "Le rendu réel n'a pas été vu pendant le développement."))), VersionNotes100.NOTES, VersionNotes000.NOTES);

    /** Le numéro de la version en cours. */
    public static final String CURRENT = NOTES.get(0).number();

    private Version() {
    }

    /** Les notes récentes, puis les plus anciennes, rangées dans d'autres fichiers (0.293). */
    @SafeVarargs
    private static List<Note> joined(List<Note>... parts) {
        List<Note> all = new java.util.ArrayList<>();
        for (List<Note> part : parts) all.addAll(part);
        return List.copyOf(all);
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

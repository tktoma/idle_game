package idle.ui;

import idle.core.Absence;
import idle.core.Game;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * L'aide « Comment jouer », par-dessus la fenêtre : un paragraphe par étape du jeu, puis la liste
 * des raccourcis avec leurs touches du moment.
 *
 * <p>Seules les étapes que le joueur a atteintes sont expliquées : la suite se découvre en
 * jouant, et l'aide grandit avec la partie. Les nombres cités viennent des règles ({@link Game}),
 * pour ne jamais dire autre chose que le jeu.
 *
 * <p>Elle s'ouvre et se ferme par son raccourci ({@link Shortcuts.Action#HELP}), par le bouton
 * « Aide » sous les onglets, et se ferme aussi par Échap ou son bouton.
 */
final class HelpPane extends StackPane {

    private static final String TITLE_STYLE = "-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 10 0 0 0; -fx-text-fill: #d6dde6;";
    private static final String TEXT_STYLE = "-fx-font-size: 13px; -fx-text-fill: #b7c4d2;";
    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 18; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-text-fill: #0b0e14; -fx-background-color: #d6dde6; -fx-border-color: #ffffff;";

    /**
     * Un paragraphe de l'aide.
     *
     * @param title son titre
     * @param text  ce qu'il explique
     */
    record Section(String title, String text) {
    }

    private final Game game;
    private final Settings settings;
    private final VBox content = new VBox(6);
    private final Button close = new Button("Fermer");
    private boolean open = false;

    HelpPane(Game game, Settings settings) {
        this.game = game;
        this.settings = settings;
        Label title = new Label("Comment jouer");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e8f4ff;");
        close.setStyle(BUTTON_STYLE);
        close.setFocusTraversable(false);
        close.setOnAction(event -> close());
        content.setAlignment(Pos.TOP_LEFT);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox card = new VBox(10, title, scroll, close);
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-background-color: #16202e; -fx-background-radius: 10; -fx-border-radius: 10;"
                + " -fx-border-color: #3a4a5e; -fx-padding: 20 26;");
        card.setMaxWidth(640);
        setStyle("-fx-background-color: #0b0e14cc;");
        setPadding(new Insets(24));
        getChildren().add(card);
        setVisible(false);
    }

    /** Vrai tant que l'aide est affichée. */
    boolean isOpen() {
        return open;
    }

    /** Ouvre l'aide si elle est fermée, la ferme sinon. */
    void toggle() {
        if (open) close();
        else open();
    }

    /** Ouvre l'aide, écrite pour la partie telle qu'elle est en ce moment. */
    void open() {
        open = true;
        content.getChildren().clear();
        for (Section section : sections(game, settings)) {
            Label title = new Label(section.title());
            title.setStyle(TITLE_STYLE);
            Label text = new Label(section.text());
            text.setStyle(TEXT_STYLE);
            text.setWrapText(true);
            content.getChildren().add(title);
            content.getChildren().add(text);
        }
        setVisible(true);
    }

    void close() {
        open = false;
        setVisible(false);
    }

    /** Les paragraphes de l'aide pour cette partie : ceux des étapes atteintes, puis ce qui vaut toujours. */
    static List<Section> sections(Game game, Settings settings) {
        List<Section> sections = new ArrayList<>();
        int generators = game.generatorsPerAtom();
        sections.add(new Section("Particules", "Un générateur crée des particules. Elles paient trois améliorations : la "
                + "vitesse de création, le couplage (chaque générateur renforce les autres) et un générateur de plus. "
                + "Réunis, les " + generators + " générateurs fusionnent en atomes : il n'en reste qu'un, et les "
                + "particules repartent de zéro."));
        boolean atoms = game.state().totalAtoms().sign() > 0 || game.isDarkMatterUnlocked() || game.isBigBangUnlocked();
        if (atoms) {
            sections.add(new Section("Atomes", "Les atomes paient des améliorations que la fusion ne reprend pas (onglet "
                    + "Atomes)." + (game.isAtomCapLifted() ? "" : " On ne peut en garder que " + Format.count(game.atomCap())
                    + " à la fois : il faut les dépenser pour fusionner de nouveau.") + " À " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés s'ouvrent "
                    + "l'automatisation et le tableau périodique."));
        }
        if (game.isPeriodicTableUnlocked() || game.isDarkMatterUnlocked() || game.isBigBangUnlocked()) {
            sections.add(new Section("Automatisation", "Un automatisme s'achète une fois, en atomes, puis achète ou "
                    + "fusionne à votre place. Il se coupe et se relance d'un clic, et sa cadence s'accélère en atomes."
                    + (game.isDarkMatterUnlocked() || game.isBigBangUnlocked()
                            ? " Ceux de matière noire ne coûtent rien ; les deux qui achètent suivent le moins cher "
                                    + "d'abord, ou l'ordre que vous leur donnez sous leurs cartes." : "")
                    + (game.isBigBangUnlocked()
                            ? " L'espace et les Big Bangs en apportent d'autres : l'appui, la création, le rassemblement "
                                    + "et la formation automatiques." : "")));
            sections.add(new Section("Tableau périodique", "Une synthèse coûte des atomes et donne un élément. Chaque "
                    + "élément apporte un bonus, ses exemplaires le renforcent, et une famille réunie forme un ensemble. "
                    + "Quand les " + PeriodicTable.ELEMENTS.size() + " éléments sont découverts, le tableau peut exploser."));
        }
        if (game.isDarkMatterUnlocked() || game.isBigBangUnlocked()) {
            sections.add(new Section("Matière noire", "L'explosion remet toute la matière à zéro et laisse de la matière "
                    + "noire. Tenez le clic sur son point pour la faire grossir. Son arbre a trois branches : l'une se "
                    + "paie en particules, l'autre en atomes, la troisième s'ouvre avec la taille. Les défis sont des "
                    + "parties sous contrainte, avec une récompense qui reste. Au bout de l'arbre, cinq conditions "
                    + "réunies déclenchent le Big Bang. Exploser nettement plus vite que la fois d'avant (en moins de "
                    + ElementText.percent(Game.SPEED_PRIME_MARGIN) + " du temps) rapporte une prime de vitesse : une matière noire de plus."));
        }
        if (game.isBigBangUnlocked()) {
            sections.add(new Section("Big Bang", "Le Big Bang efface tout sauf les succès, et ouvre l'expansion : l'espace "
                    + "grandit seul. Une molécule se crée avec des exemplaires du tableau périodique et de l'espace libre, "
                    + "et reste pour toujours. À partir de " + Game.SUBSTANCE_MOLECULES + " molécules, une sorte se "
                    + "rassemble ; les sortes rassemblées forment des assemblages, puis des astres, jusqu'à la galaxie, "
                    + "l'amas de galaxies et l'univers. Chaque Big Bang refait donne un palier, jusqu'au cinquième."));
        } else {
            sections.add(new Section("La suite", "Elle se découvre en jouant : cette aide grandit avec la partie."));
        }
        if (game.isBigBangUnlocked()) {
            sections.add(new Section("S'y retrouver au troisième acte", "Au-dessus des assemblages, des astres et de "
                    + "l'univers, la liste de courses dit ce qu'il manque pour le prochain pas, avec un bouton sur chaque "
                    + "ligne. Sur la page des molécules, la ligne « Meilleur achat » nomme la création qui rapporte le plus "
                    + "pour l'espace qu'elle prend, et un bouton range les cartes par rendement."));
            sections.add(new Section("Collections, comètes et défis", "Créer au moins une molécule de la moitié des sortes "
                    + "d'un rayon, puis de toutes, multiplie une grandeur pour de bon. De temps en temps une comète traverse "
                    + "l'expansion de la matière : la saisir double l'espace créé pendant "
                    + (int) (Game.COMET_BOOST_SECONDS / 60) + " minutes ; la manquer ne coûte rien. L'amas de galaxies et "
                    + "l'univers demandent de la quantité et de la variété : un nombre de sortes rassemblées dans chaque état."
                    + (game.isBangChallengesUnlocked() ? " Au cinquième Big Bang, les défis de Big Bang s'ouvrent sous "
                            + "les paliers : un Big Bang sous contrainte, pour une récompense qui reste." : "")));
        }
        sections.add(new Section("Lire une carte", "Une carte dit l'essentiel : son niveau en haut, ce qu'elle fait, son "
                + "prix en bas. Tenez la touche " + Detail.keyName(settings.detailKey()) + " pour lire les explications "
                + "complètes, partout dans le jeu."
                + (settings.stateSigns() ? " Devant son nom, un signe dit son état sans la couleur : ● à portée, ○ en "
                        + "attente, ◐ entamée, ▶ en marche, □ coupée, ✓ acquise, × fermée." : "")));
        sections.add(new Section("Lire un nombre", "Laissez la souris sur la production, les atomes par fusion, la matière "
                + "noire ou l'espace par seconde : une bulle dit de quoi le nombre est fait, un facteur par ligne. Sous "
                + "les onglets, l'objectif rappelle le prochain pas"
                + (game.isDarkMatterUnlocked() ? ", puis ce qu'il manque au prochain Big Bang" : "")
                + ". Les annonces qui passent en haut de l'écran restent dans le journal, dernière sous-page des statistiques."));
        if (game.isColliderUnlocked()) sections.add(new Section("Collisionneur", "Un onglet à part, depuis la première "
                + "explosion, que ni l'explosion ni le Big Bang ne remettent à zéro. Les faisceaux croisés donnent de l'énergie, et "
                + "huit générateurs se produisent l'un l'autre, de l'aimant supraconducteur au collisionneur de Planck : comptez "
                + "trois heures environ pour les avoir tous. Un clic quand les faisceaux se croisent capte trois secondes de production. "
                + "Avec le huitième s'ouvrent les améliorations, payées en énergie : d'abord « Collisions », qui donne à chaque "
                + "croisement 15 % de chances de sortir un quark, puis les saveurs une à une (étrange, charmée, belle, top), les "
                + "automates des générateurs, la vitesse des faisceaux, la production d'énergie et des générateurs, et la chaîne qui "
                + "renforce le reste du jeu, dans son sous-onglet « Chaîne », payée en énergie et en quarks : particules, atomes, "
                + "matière noire, vitesse de l'espace, dédoublement des molécules. L'énergie produite depuis le début ouvre les deux "
                + "trous noirs : " + Format.count(BigNum.of(Game.BLACK_HOLE_ENERGY)) + " pour le trou noir, "
                + Format.count(BigNum.of(Game.SUPERMASSIVE_ENERGY)) + " pour le trou noir supermassif, donc pour la galaxie. Les "
                + "succès du collisionneur, dans l'onglet « Succès », se gagnent en achetant des générateurs : chacun double une "
                + "production ou divise des prix par deux."));
        if (game.bodiesFormed() > 0) sections.add(new Section("Maturation des astres", "Un astre formé mûrit avant de compter "
                + "en entier : de 4 minutes pour un amas de roches à 2 heures pour le trou noir supermassif. En attendant, il donne "
                + "le quart de son bonus, et ne compte ni pour les astres qui partent de lui ni pour la galaxie. Trois astres "
                + "mûrissent à la fois. Le bouton « Accélérer » dépense une charge d'impulsion pour gagner une minute."));
        if (game.isSolarSystemsUnlocked()) sections.add(new Section("Systèmes solaires", "Dans le Big Bang, depuis le sixième. "
                + "Autour d'une étoile mûre, on range des planètes, de la plus proche à la plus lointaine, et des lunes, une par "
                + "planète au plus. Un système est complet à " + Game.SYSTEM_MIN_PLANETS + " planètes ; la galaxie en demande "
                + Game.SYSTEMS_FOR_GALAXY + ". L'harmonie : un point par planète bien placée (telluriques près, glacées et gazeuses "
                + "au loin), par lune, pour une étoile géante, et deux pour une planète océan en deuxième ou troisième orbite d'un "
                + "Soleil ou d'une naine rouge. Chaque point d'un système complet ajoute " + ElementText.percent(Game.SYSTEM_GAIN)
                + " à toutes les grandeurs. Au septième Big Bang, deux places de maturation et une sonde de plus ; au huitième, "
                + "les matières exotiques : cinq astres sans assemblage, que la galaxie demande aussi."));
        if (game.isProbesUnlocked()) sections.add(new Section("Sondes", "Dans le Big Bang, depuis le troisième. On choisit une "
                + "destination, puis un voyage de 15 minutes à 8 heures ; plus il est long, plus le butin est riche par heure. À l'"
                + "arrivée, la sonde attend à destination : un clic sur elle (ou la touche R) ouvre son butin, une carte par clic, ou tout d'un coup : des éléments "
                + "du rayon visé, de l'espace d'un coup, un sursaut qui double l'espace par seconde un moment, des échantillons. "
                + "Les échantillons paient les améliorations : instruments, moteurs, réserves d'énergie, soute et hangar. "
                + "Rien ne se perd si l'on revient tard, ni au Big Bang."));
        sections.add(new Section("Sauvegarde et absence", "La partie s'écrit toute seule toutes les "
                + settings.saveDelay().label() + " (le rythme se choisit dans les réglages) et à la fermeture. Au retour, "
                + (settings.offline() ? "" : "si le temps hors ligne est rejoué (il est coupé en ce moment), ")
                + "une part du temps d'absence est rejouée : " + ElementText.percent(Game.OFFLINE_RATE_PARTICLES) + " au début, "
                + ElementText.percent(Game.OFFLINE_RATE_UNIVERSE) + " une fois l'univers formé, sur "
                + (int) (Absence.MAX_SECONDS / 3600) + " heures d'absence au plus. Les réglages permettent d'exporter la partie, de régler le volume des sons ou de les "
                + "couper, de limiter les images par seconde ou de couper les effets visuels un par un ; la fenêtre ralentit d'elle-même quand "
                + "elle n'est pas au premier plan. Des thèmes et des notations s'y ouvrent en faisant des choses très précises."));
        sections.add(new Section("Raccourcis clavier", shortcuts(settings)));
        return sections;
    }

    /** La liste des raccourcis avec leurs touches du moment, un par ligne. */
    static String shortcuts(Settings settings) {
        if (!settings.shortcuts()) return "Les raccourcis sont coupés dans les réglages.";
        StringBuilder text = new StringBuilder();
        for (Shortcuts.Action action : Shortcuts.Action.values()) {
            KeyCode key = settings.key(action);
            if (text.length() > 0) text.append('\n');
            text.append(key == null ? "(aucune touche)" : Shortcuts.name(key)).append("  —  ").append(action.label());
        }
        text.append('\n').append(Detail.keyName(settings.detailKey())).append("  —  Afficher les détails")
                .append("\nLes touches se changent dans les réglages.");
        return text.toString();
    }
}

package idle.ui;

import idle.core.Automation;
import idle.core.BigNum;
import idle.core.DarkAutomation;
import idle.core.Game;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Contenu de l'onglet « Automatisation » : tous les automatismes du jeu au même endroit, chacun
 * n'apparaissant qu'une fois débloqué pour la première fois. Ensuite il reste affiché : si une
 * explosion le reverrouille, sa carte dit ce qu'il faut pour le débloquer de nouveau.
 *
 * <p>D'abord les automatismes ordinaires, une ligne chacun. À gauche, sa carte : tant qu'il n'est
 * pas acheté, elle affiche son prix en atomes et sert à l'acheter ; ensuite elle devient un
 * interrupteur pour le mettre en marche ou le couper. À droite, sa cadence : le délai entre deux
 * actions, et le bouton pour le réduire en payant des atomes. La cadence achetée en atomes a un
 * maximum ; au-delà, seuls les éléments du tableau périodique réduisent encore le délai. Ils
 * apparaissent une fois assez d'atomes créés ({@link Game#UNLOCK_TOTAL_ATOMS}) ; la synthèse
 * automatique, elle, attend le premier élément unique du tableau périodique.
 *
 * <p>Ensuite les automatismes de matière noire, qui font ce que les ordinaires laissent au
 * joueur : chacun apparaît quand le joueur a gagné assez de matière noire, puis se met en marche
 * ou se coupe d'un clic. Ils ne coûtent rien, et aucun ne refait le travail d'un automatisme
 * ordinaire : le premier se contente de les offrir au début de chaque partie.
 *
 * <p>Sous chaque groupe, une ligne dit ce qui reste à découvrir, pour que le joueur sache qu'il
 * y a une suite.
 *
 * <p>L'onglet apparaît dès qu'un automatisme est débloqué, et reste là pour de bon une fois la
 * première matière noire obtenue.
 */
final class AutomationPage extends VBox {

    private static final String CARD_STYLE = "-fx-font-size: 14px; -fx-padding: 12 16; -fx-cursor: hand;"
            + " -fx-background-radius: 8; -fx-border-radius: 8;";
    /** Automatisme pas encore acheté : couleur des atomes, puisqu'il se paie en atomes. */
    private static final String TO_BUY_STYLE = CARD_STYLE
            + " -fx-text-fill: #ffe9c2; -fx-background-color: #2a2113; -fx-border-color: #6b5a33;";
    private static final String ON_STYLE = CARD_STYLE
            + " -fx-text-fill: #c8f7d0; -fx-background-color: #14261a; -fx-border-color: #9be7a8;";
    private static final String OFF_STYLE = CARD_STYLE
            + " -fx-text-fill: #8fa3b8; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;";
    private static final String DARK_ON_STYLE = CARD_STYLE + " -fx-text-fill: #1a0f2e; -fx-background-color: "
            + GameApp.DARK_MATTER_COLOR + "; -fx-border-color: #ffffff;";
    private static final String DARK_OFF_STYLE = CARD_STYLE + " -fx-text-fill: " + GameApp.DARK_MATTER_COLOR
            + "; -fx-background-color: #1c1630; -fx-border-color: " + GameApp.DARK_MATTER_COLOR + ";";
    private static final String HINT_STYLE = "-fx-font-size: 13px; -fx-text-fill: #8fa3b8;";

    private final Game game;
    private final Label balanceLabel = new Label();

    // Les automatismes ordinaires.
    private final Label ordinaryHint = new Label();
    private final Map<Automation, Button> cards = new LinkedHashMap<>();
    private final Map<Automation, Button> cadences = new LinkedHashMap<>();
    private final Map<Automation, HBox> rows = new LinkedHashMap<>();
    /** Ce qui reste à débloquer parmi les automatismes ordinaires. */
    private final Label ordinaryNext = new Label();
    // Le seuil de la fusion automatique, réglable une fois débloqué dans l'arbre de matière noire.
    private final Button lessGenerators = new Button("−");
    private final Button moreGenerators = new Button("+");
    private final Label thresholdLabel = new Label();
    private final HBox thresholdRow = new HBox(8, lessGenerators, thresholdLabel, moreGenerators);

    // Les automatismes de matière noire.
    private final Label darkTitle = new Label("Automatismes de matière noire");
    private final Label darkHint = new Label();
    private final Map<DarkAutomation, Button> darkCards = new LinkedHashMap<>();
    /** Ce qui reste à débloquer parmi les automatismes de matière noire. */
    private final Label darkNext = new Label();

    AutomationPage(Game game) {
        super(12);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(24));

        Label title = new Label("Automatismes");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #9be7a8;");
        ordinaryHint.setText("Chaque automatisme s'achète une fois, en atomes, puis s'active ou se coupe librement. "
                + "Il agit à intervalles réguliers : accélérez sa cadence en atomes jusqu'à son maximum ; "
                + "au-delà, seuls les éléments du tableau périodique réduisent encore le délai.");
        wrapped(ordinaryHint, HINT_STYLE);
        balanceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffd27f;");
        getChildren().add(title);
        getChildren().add(ordinaryHint);
        getChildren().add(balanceLabel);

        // Une ligne par automatisme du catalogue : en ajouter un dans core suffit à le faire apparaître.
        for (Automation automation : game.automations()) {
            Button card = new Button();
            card.setPrefWidth(400);
            card.setMinWidth(0);                 // la carte rétrécit avec la fenêtre : son texte passe à la ligne
            card.setMaxWidth(520);
            card.setMinHeight(84);
            card.setWrapText(true);
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setTextAlignment(TextAlignment.CENTER);
            // Un bouton qui garde le focus puis se grise le passe au suivant, et la page défile toute
            // seule jusqu'à lui : aucun bouton de cette page ne prend le focus au clic.
            card.setFocusTraversable(false);
            card.setOnAction(event -> {
                if (game.ownsAutomation(automation.id())) {
                    game.setAutomationEnabled(automation.id(), !game.isAutomationEnabled(automation.id()));
                } else {
                    game.buyAutomation(automation.id());
                }
                refresh();
            });
            cards.put(automation, card);

            Button cadence = new Button();
            cadence.setPrefWidth(250);
            cadence.setMinWidth(0);
            cadence.setMaxHeight(Double.MAX_VALUE);   // même hauteur que la carte, quelle qu'elle soit
            cadence.setMinHeight(84);
            cadence.setWrapText(true);
            cadence.setTextAlignment(TextAlignment.CENTER);
            cadence.setFocusTraversable(false);
            cadence.setOnAction(event -> {
                game.speedUpAutomation(automation.id());
                refresh();
            });
            cadences.put(automation, cadence);

            HBox row = new HBox(8, card, cadence);
            row.setAlignment(Pos.CENTER);
            row.setFillHeight(true);
            rows.put(automation, row);
            getChildren().add(row);
            // Le réglage du seuil vient juste sous l'automatisme de fusion.
            if (automation.kind() == Automation.Kind.FUSION) getChildren().add(thresholdRow);
        }
        thresholdRow.setAlignment(Pos.CENTER);
        thresholdLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #c9a6ff;");
        String stepStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 2 12; -fx-cursor: hand;"
                + " -fx-text-fill: #c9a6ff; -fx-background-color: #1c1630; -fx-background-radius: 6;"
                + " -fx-border-color: #c9a6ff; -fx-border-radius: 6;";
        lessGenerators.setStyle(stepStyle);
        moreGenerators.setStyle(stepStyle);
        lessGenerators.setFocusTraversable(false);
        moreGenerators.setFocusTraversable(false);
        // Un clic déplace le seuil d'un groupe de générateurs : 10, 20, 30…
        lessGenerators.setOnAction(event -> {
            game.setFusionThreshold(game.fusionThreshold() - game.generatorsPerAtom());
            refresh();
        });
        moreGenerators.setOnAction(event -> {
            game.setFusionThreshold(game.fusionThreshold() + game.generatorsPerAtom());
            refresh();
        });
        wrapped(ordinaryNext, "-fx-font-size: 13px; -fx-text-fill: #ffe9c2;");
        getChildren().add(ordinaryNext);

        // Les automatismes de matière noire : une carte chacun, qui sert d'interrupteur.
        darkTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 16 0 0 0; -fx-text-fill: "
                + GameApp.DARK_MATTER_COLOR + ";");
        darkHint.setText("Ils font ce que les automatismes ordinaires laissent au joueur, et ne coûtent rien : une "
                + "action par seconde. Aucun ne fait le travail d'un automatisme ordinaire : le premier les offre, les "
                + "autres achètent ou font exploser. Attention : l'explosion automatique n'attend pas que vous ayez "
                + "dépensé vos particules et vos atomes dans l'arbre.");
        wrapped(darkHint, HINT_STYLE);
        getChildren().add(darkTitle);
        getChildren().add(darkHint);
        for (DarkAutomation automation : game.darkAutomations()) {
            Button card = new Button();
            card.setPrefWidth(658);              // la largeur d'une carte ordinaire et de sa cadence réunies
            card.setMinWidth(0);
            card.setMaxWidth(778);
            card.setMinHeight(84);
            card.setWrapText(true);
            card.setTextAlignment(TextAlignment.CENTER);
            card.setFocusTraversable(false);
            card.setOnAction(event -> {
                game.setDarkAutomationEnabled(automation.id(), !game.isDarkAutomationEnabled(automation.id()));
                refresh();
            });
            darkCards.put(automation, card);
            getChildren().add(card);
        }
        wrapped(darkNext, "-fx-font-size: 13px; -fx-text-fill: #c9bfe0;");
        getChildren().add(darkNext);
    }

    private static void wrapped(Label label, String style) {
        label.setStyle(style);
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
    }

    /** Affiche ou cache un composant ; caché, il ne laisse pas de trou dans la page. */
    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /**
     * Vrai si l'onglet doit être affiché : un automatisme ordinaire ou de matière noire est
     * débloqué, ou la première matière noire a été obtenue (l'onglet ne disparaît alors plus).
     */
    static boolean hasContent(Game game) {
        if (game.isAutomationUnlocked() || game.isDarkMatterUnlocked()) return true;
        for (DarkAutomation automation : game.darkAutomations()) {
            if (game.isDarkAutomationUnlocked(automation.id())) return true;
        }
        return false;
    }

    /** Recopie l'état des automatismes dans les cartes, et n'affiche que ceux qui sont débloqués. */
    void refresh() {
        BigNum atoms = game.state().atoms();
        balanceLabel.setText(Format.count(atoms) + (atoms.gt(BigNum.ONE) ? " atomes disponibles" : " atome disponible"));

        boolean ordinary = game.isAutomationUnlocked();

        boolean threshold = ordinary && game.isFusionThresholdUnlocked();
        show(thresholdRow, threshold);
        if (threshold) {
            int generators = game.fusionThreshold();
            thresholdLabel.setText("La fusion automatique attend " + generators + " générateurs ("
                    + generators / game.generatorsPerAtom() + " fois les atomes)");
            lessGenerators.setDisable(generators <= game.generatorsPerAtom());
            moreGenerators.setDisable(generators + game.generatorsPerAtom() > game.maxGeneratorCount());
        }

        boolean anyShown = false;
        boolean someHidden = false;
        for (Automation automation : game.automations()) {
            boolean available = game.isAutomationAvailable(automation.id());
            // Déjà débloqué une fois : il reste affiché, avec ce qu'il faut pour le débloquer de nouveau.
            boolean known = available || game.wasAutomationEverAvailable(automation.id());
            show(rows.get(automation), known);
            if (!known) {
                someHidden = true;
                continue;
            }
            anyShown = true;
            if (available) {
                refreshCard(automation);
                refreshCadence(automation);
            } else {
                showLocked(automation);
            }
        }
        show(ordinaryHint, anyShown);
        show(balanceLabel, anyShown);
        // Ce que le joueur n'a encore jamais débloqué reste caché ; une ligne annonce la suite.
        ordinaryNext.setText(!ordinary && !anyShown
                ? "Les automatismes ordinaires se débloquent à " + Format.count(Game.UNLOCK_TOTAL_ATOMS)
                        + " atomes créés (vous : " + Format.count(game.state().totalAtoms()) + ")."
                : "Un autre automatisme apparaîtra avec votre premier élément unique ★ du tableau périodique.");
        show(ordinaryNext, someHidden);

        // Les automatismes de matière noire : seulement ceux que la matière noire gagnée a débloqués.
        boolean anyDark = false;
        DarkAutomation next = null;
        for (DarkAutomation automation : game.darkAutomations()) {
            Button card = darkCards.get(automation);
            boolean unlocked = game.isDarkAutomationUnlocked(automation.id());
            show(card, unlocked);
            if (!unlocked) {
                if (next == null) next = automation;
                continue;
            }
            anyDark = true;
            boolean on = game.isDarkAutomationEnabled(automation.id());
            card.setText(automation.name() + "\n" + describe(automation) + "\n" + (on ? "En marche" : "Coupé"));
            card.setStyle(on ? DARK_ON_STYLE : DARK_OFF_STYLE);
        }
        show(darkTitle, anyDark);
        show(darkHint, anyDark);
        // La suite n'est annoncée qu'à qui connaît déjà la matière noire.
        boolean announce = next != null && game.isDarkMatterUnlocked();
        show(darkNext, announce);
        if (announce) {
            darkNext.setText((anyDark ? "Prochain automatisme de matière noire à "
                    : "Un automatisme de matière noire apparaîtra à ")
                    + Format.count(next.darkMatter()) + " matières noires gagnées (vous : "
                    + Format.count(game.darkMatterEarned()) + ").");
        }
    }

    /** Ce que fait un automatisme ordinaire, en une phrase. */
    private static String describe(Automation automation) {
        return switch (automation.kind()) {
            case UPGRADE -> "Achète « " + automation.name() + " » dès que possible";
            case FUSION -> "Fusionne dès que tous les générateurs sont débloqués";
            case SYNTHESIS -> "Synthétise un élément dès qu'il y a assez d'atomes";
        };
    }

    /** Un automatisme déjà connu mais reverrouillé par une explosion : sa carte dit comment le débloquer. */
    private void showLocked(Automation automation) {
        Button card = cards.get(automation);
        card.setText(automation.name() + "\n" + describe(automation) + "\n" + condition(automation));
        card.setStyle(OFF_STYLE);
        card.setDisable(true);
        Button cadence = cadences.get(automation);
        cadence.setText("Verrouillé");
        cadence.setStyle(OFF_STYLE);
        cadence.setDisable(true);
    }

    /** Ce qu'il manque pour qu'un automatisme ordinaire soit de nouveau disponible. */
    private String condition(Automation automation) {
        boolean needsUnique = automation.kind() == Automation.Kind.SYNTHESIS && !game.isSynthesisAutomationUnlocked();
        if (!game.isAutomationUnlocked()) {
            return "Se débloque à " + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés (vous : "
                    + Format.count(game.state().totalAtoms()) + ")"
                    + (needsUnique ? ", avec un élément unique ★" : "");
        }
        return "Se débloque avec un élément unique ★ du tableau périodique"
                + (isGranting() ? ", ou sera offert une fois les autres automatismes à leur cadence maximale" : "");
    }

    /** Vrai si l'automatisme de matière noire qui offre les automatismes ordinaires est en marche. */
    private boolean isGranting() {
        for (DarkAutomation automation : game.darkAutomations()) {
            if (automation.kind() == DarkAutomation.Kind.GRANT_AUTOMATIONS && game.isDarkAutomationEnabled(automation.id())) {
                return true;
            }
        }
        return false;
    }

    private void refreshCard(Automation automation) {
        Button card = cards.get(automation);
        String what = describe(automation);
        if (!game.ownsAutomation(automation.id())) {
            card.setText(automation.name() + "\n" + what + "\nAcheter : " + Format.count(automation.cost())
                    + (automation.cost().gt(BigNum.ONE) ? " atomes" : " atome"));
            card.setStyle(TO_BUY_STYLE);
            card.setDisable(!game.canBuyAutomation(automation.id()));
        } else {
            boolean on = game.isAutomationEnabled(automation.id());
            card.setText(automation.name() + "\n" + what + "\n" + (on ? "En marche" : "Coupé"));
            card.setStyle(on ? ON_STYLE : OFF_STYLE);
            card.setDisable(false);
        }
    }

    private void refreshCadence(Automation automation) {
        Button cadence = cadences.get(automation);
        String delay = "Une action toutes les " + seconds(game.automationInterval(automation.id()));
        if (game.isAutomationMaxed(automation.id())) {
            boolean floor = game.automationInterval(automation.id()) <= game.minAutomationInterval();
            cadence.setText("Cadence maximale\n" + delay
                    + (floor ? "\n(le plus court possible)" : "\nLes éléments l'accélèrent"));
            cadence.setStyle(ON_STYLE);
            cadence.setDisable(true);
        } else {
            BigNum cost = game.automationSpeedCost(automation.id());
            cadence.setText(delay + "\nAccélérer : " + Format.count(cost)
                    + (cost.gt(BigNum.ONE) ? " atomes" : " atome"));
            cadence.setStyle(TO_BUY_STYLE);
            cadence.setDisable(!game.canSpeedUpAutomation(automation.id()));
        }
    }

    /** Ce que fait un automatisme de matière noire, en une phrase. */
    private static String describe(DarkAutomation automation) {
        return switch (automation.kind()) {
            case GRANT_AUTOMATIONS -> "Offre les automatismes ordinaires au début de chaque partie, sans attendre "
                    + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " atomes créés ni payer. La synthèse automatique suit, une fois "
                    + "les autres à leur cadence maximale et le tableau périodique ouvert, sans attendre un élément unique";
            case ATOM_UPGRADES -> "Achète les améliorations en atomes, la moins chère d'abord";
            case AUTOMATIONS -> "Achète les automatismes ordinaires et leur cadence, le moins cher d'abord";
            case EXPLOSION -> "Fait exploser le tableau périodique dès que c'est possible";
        };
    }

    /** 4.0 → « 4 s », 0.5 → « 0.5 s », 1.333 → « 1.33 s ». */
    static String seconds(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        return text.replaceAll("0+$", "").replaceAll("\\.$", "") + " s";
    }
}

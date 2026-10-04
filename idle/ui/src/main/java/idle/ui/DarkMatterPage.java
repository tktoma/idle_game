package idle.ui;

import idle.core.Automation;
import idle.core.BigNum;
import idle.core.DarkAutomation;
import idle.core.Game;
import idle.core.Landmark;
import idle.core.SizeScale;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Contenu de l'onglet « Matière noire ». En haut, la quantité possédée ; en dessous, deux sous-pages :
 * <ul>
 *   <li>« Matière noire » : le point ({@link DarkMatterView}). Tant que le joueur maintient le
 *       clic dessus (ou après un clic, une fois le verrou débloqué dans l'arbre), il grossit, d'autant plus vite que le joueur a de matière noire et que sa
 *       partie est avancée. La vue dézoome à mesure, et l'échelle des grandeurs situe sa taille :
 *       cercles des objets connus autour du point, règle graduée, repère dépassé et repère
 *       suivant. La vue occupe toute la place que laisse la fenêtre ;</li>
 *   <li>« Améliorations » : l'arbre d'améliorations ({@link DarkMatterTreePane}) ;</li>
 *   <li>« Automatisation » : les automatismes de matière noire, qui font ce que les automatismes
 *       ordinaires laissent au joueur. Chacun se débloque en possédant assez de matière noire,
 *       puis se met en marche ou se coupe d'un clic.</li>
 * </ul>
 *
 * <p>L'onglet n'apparaît qu'après la première explosion du tableau périodique.
 */
final class DarkMatterPage extends VBox {

    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 20; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";

    private final Game game;
    private final Label balanceLabel = new Label();

    private final Button pointTab = new Button("Matière noire");
    private final Button treeTab = new Button("Améliorations");
    private final Button automationTab = new Button("Automatisation");
    /** Sous-page affichée : 0 = le point, 1 = l'arbre d'améliorations, 2 = les automatismes. */
    private int selected = 0;

    // Sous-page « Matière noire »
    private final Label sizeLabel = new Label();
    private final Label landmarkLabel = new Label();
    private final Label speedLabel = new Label();
    private final DarkMatterView view = new DarkMatterView(460, 300);
    private final VBox pointPane = new VBox(8);
    /** Vrai tant que le clic est maintenu sur le point. */
    private boolean pressed = false;
    /** Vrai quand l'appui est verrouillé : il continue sans le bouton, tant que cette sous-page est affichée. */
    private boolean locked = false;

    // Sous-page « Améliorations »
    private final DarkMatterTreePane tree;
    private final VBox treePane = new VBox(10);
    private final ScrollPane treeScroll = new ScrollPane(treePane);

    // Sous-page « Automatisation »
    private static final String CARD_STYLE = "-fx-font-size: 14px; -fx-padding: 12 16; -fx-background-radius: 8;"
            + " -fx-border-radius: 8;";
    private final Map<DarkAutomation, Button> automationCards = new LinkedHashMap<>();
    private final VBox automationPane = new VBox(12);
    private final ScrollPane automationScroll = new ScrollPane(automationPane);

    DarkMatterPage(Game game) {
        super(8);
        this.game = game;
        this.tree = new DarkMatterTreePane(game);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 16, 24));

        balanceLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + GameApp.DARK_MATTER_COLOR + ";");
        sizeLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #efe4ff;");
        landmarkLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #c9bfe0;");
        landmarkLabel.setWrapText(true);
        landmarkLabel.setTextAlignment(TextAlignment.CENTER);
        speedLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        speedLabel.setWrapText(true);
        speedLabel.setTextAlignment(TextAlignment.CENTER);

        pointTab.setOnAction(event -> select(0));
        treeTab.setOnAction(event -> select(1));
        automationTab.setOnAction(event -> select(2));
        HBox subTabs = new HBox(pointTab, treeTab, automationTab);
        subTabs.setAlignment(Pos.CENTER);

        // Le clic doit commencer sur le point ; il compte ensuite jusqu'à ce qu'on relâche le bouton.
        view.setStyle("-fx-cursor: hand;");
        // Avec le verrou de l'arbre, un clic sur le point bloque l'appui ; un autre clic le libère.
        view.setOnMousePressed(event -> {
            boolean onPoint = view.isOnDarkMatter(event.getX(), event.getY());
            if (onPoint && game.isHoldLockUnlocked()) locked = !locked;
            pressed = onPoint;
        });
        view.setOnMouseReleased(event -> pressed = false);

        // La vue s'étire avec la fenêtre ; les textes gardent leur hauteur.
        CanvasPane viewPane = CanvasPane.filling(view);
        VBox.setVgrow(viewPane, Priority.ALWAYS);
        pointPane.setAlignment(Pos.TOP_CENTER);
        pointPane.getChildren().add(viewPane);
        pointPane.getChildren().add(sizeLabel);
        pointPane.getChildren().add(landmarkLabel);
        pointPane.getChildren().add(speedLabel);

        // L'arbre, qui défile s'il dépasse la fenêtre.
        Label mockNotice = new Label("Ces améliorations sont définitives : ni la fusion ni l'explosion ne les "
                + "reprennent. À gauche, elles se paient en particules ; au milieu, en atomes ; à droite, elles "
                + "se débloquent quand la matière noire atteint une taille, sans rien dépenser.");
        mockNotice.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        mockNotice.setWrapText(true);
        mockNotice.setTextAlignment(TextAlignment.CENTER);
        treePane.setAlignment(Pos.TOP_CENTER);
        treePane.setPadding(new Insets(10, 0, 10, 0));
        treePane.getChildren().add(mockNotice);
        treePane.getChildren().add(tree);
        treeScroll.setFitToWidth(true);
        treeScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        treeScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Les automatismes de matière noire : une carte chacun, qui sert d'interrupteur une fois débloquée.
        Label automationNotice = new Label("Ces automatismes font ce que ceux de l'onglet Automatisation laissent "
                + "au joueur : une action par seconde, sauf « Premiers pas » qui agit toutes les "
                + ElementText.number(Automation.DEFAULT_INTERVAL) + " s, comme un automatisme neuf. "
                + "Ils se débloquent en possédant assez de matière noire, sans la "
                + "dépenser. Attention : l'explosion automatique n'attend pas que vous ayez dépensé vos particules "
                + "et vos atomes dans l'arbre.");
        automationNotice.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        automationNotice.setWrapText(true);
        automationNotice.setTextAlignment(TextAlignment.CENTER);
        automationPane.setAlignment(Pos.TOP_CENTER);
        automationPane.setPadding(new Insets(10, 0, 10, 0));
        automationPane.getChildren().add(automationNotice);
        for (DarkAutomation automation : game.darkAutomations()) {
            Button card = new Button();
            card.setPrefWidth(520);
            card.setMinWidth(0);                 // la carte rétrécit avec la fenêtre : son texte passe à la ligne
            card.setMinHeight(84);
            card.setWrapText(true);
            card.setTextAlignment(TextAlignment.CENTER);
            card.setOnAction(event -> {
                game.setDarkAutomationEnabled(automation.id(), !game.isDarkAutomationEnabled(automation.id()));
                refresh();
            });
            automationCards.put(automation, card);
            automationPane.getChildren().add(card);
        }
        automationScroll.setFitToWidth(true);
        automationScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        automationScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Les sous-pages sont empilées au même endroit ; une seule est visible à la fois.
        StackPane pages = new StackPane(pointPane, treeScroll, automationScroll);
        VBox.setVgrow(pages, Priority.ALWAYS);

        getChildren().add(balanceLabel);
        getChildren().add(subTabs);
        getChildren().add(pages);
        select(0);
    }

    /** Affiche une sous-page : 0 = le point, 1 = l'arbre d'améliorations, 2 = les automatismes. */
    private void select(int index) {
        selected = index;
        pressed = false;
        pointPane.setVisible(index == 0);
        treeScroll.setVisible(index == 1);
        automationScroll.setVisible(index == 2);
        pointTab.setStyle(subTabStyle(index == 0));
        treeTab.setStyle(subTabStyle(index == 1));
        automationTab.setStyle(subTabStyle(index == 2));
    }

    private static String subTabStyle(boolean selected) {
        return SUB_TAB_STYLE + (selected
                ? " -fx-text-fill: " + GameApp.DARK_MATTER_COLOR + "; -fx-border-color: transparent transparent "
                        + GameApp.DARK_MATTER_COLOR + " transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;");
    }

    /**
     * Fait grossir la matière noire si le clic est maintenu, puis avance l'animation.
     *
     * @param elapsed    secondes écoulées depuis l'image précédente
     * @param timeFactor accélération du temps (1 en jeu normal)
     */
    void frame(double elapsed, double timeFactor) {
        if (selected != 0) return;
        boolean growing = pressed || (locked && game.isHoldLockUnlocked());
        if (growing) game.growDarkMatter(elapsed * timeFactor);
        view.frame(elapsed, game.state().darkMatterSize(), growing);
    }

    /** L'onglet n'est plus affiché : un clic resté « maintenu » ne doit pas reprendre au retour. */
    void release() {
        pressed = false;
    }

    /** Recopie l'état du jeu dans les textes. */
    void refresh() {
        BigNum darkMatter = game.state().darkMatter();
        int explosions = game.state().explosions();
        balanceLabel.setText(Format.count(darkMatter) + " matière noire   (" + explosions
                + (explosions > 1 ? " explosions)" : " explosion)"));

        BigNum size = game.state().darkMatterSize();
        sizeLabel.setText("Taille : " + Format.length(size));
        Landmark reached = SizeScale.reached(size);
        Landmark next = SizeScale.next(size);
        landmarkLabel.setText((reached == null ? "" : "Dépassé : " + describe(reached))
                + (reached != null && next != null ? "   |   " : "")
                + (next == null ? "   |   Plus grand que tous les repères" : "Prochain repère : " + describe(next)));

        // La vitesse, et d'où elle vient : l'élan (matière noire, avancement, arbre) divisé par la résistance.
        BigNum growth = game.darkMatterGrowthPerSecond();
        String speed = growth.lt(BigNum.of(2))
                ? "+" + ElementText.percent(growth.toDouble() - 1)
                : Format.multiplier(growth);
        String how = !game.isHoldLockUnlocked() ? "Maintenez le clic sur le point pour le faire grossir : "
                : locked ? "Appui verrouillé (un clic sur le point le libère) : "
                : "Cliquez sur le point pour verrouiller l'appui : ";
        speedLabel.setText(how + speed + " par seconde d'appui.\n"
                + "Élan : " + ElementText.percent(Game.DARK_MATTER_GROWTH_PER_UNIT) + " × " + Format.count(darkMatter)
                + " matière noire × avancement de la partie " + ElementText.number(game.darkMatterProgressFactor())
                + (game.darkExpansionMultiplier().gt(BigNum.ONE)
                        ? " × " + Format.amount(game.darkExpansionMultiplier()) + " (arbre)" : "")
                + ". Résistance : ÷" + Format.amount(game.darkMatterResistance())
                + ", elle double chaque fois que la taille est multipliée par 1 000."
                + (game.darkAutoExpansionShare() > 0
                        ? " Elle grossit aussi seule, à " + ElementText.percent(game.darkAutoExpansionShare())
                                + " de cette vitesse." : ""));

        tree.refresh();

        automationCards.forEach((automation, card) -> {
            String what = switch (automation.kind()) {
                case PARTICLE_UPGRADES -> "Achète vitesse et générateurs, fusionne et synthétise, tant que "
                        + "l'automatisme ordinaire correspondant n'est pas en marche";
                case ATOM_UPGRADES -> "Achète les améliorations en atomes, la moins chère d'abord";
                case AUTOMATIONS -> "Achète les automatismes ordinaires et leur cadence, le moins cher d'abord";
                case EXPLOSION -> "Fait exploser le tableau périodique dès que c'est possible";
            };
            if (!game.isDarkAutomationUnlocked(automation.id())) {
                card.setText(automation.name() + "\n" + what + "\nVerrouillé : il faut "
                        + Format.count(automation.darkMatter()) + " matières noires (vous : " + Format.count(darkMatter) + ")");
                card.setStyle(CARD_STYLE + " -fx-text-fill: #8fa3b8; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;");
                card.setDisable(true);
            } else {
                boolean on = game.isDarkAutomationEnabled(automation.id());
                card.setText(automation.name() + "\n" + what + "\n" + (on ? "En marche" : "Coupé"));
                card.setStyle(CARD_STYLE + " -fx-cursor: hand;" + (on
                        ? " -fx-text-fill: #1a0f2e; -fx-background-color: " + GameApp.DARK_MATTER_COLOR + "; -fx-border-color: #ffffff;"
                        : " -fx-text-fill: " + GameApp.DARK_MATTER_COLOR + "; -fx-background-color: #1c1630; -fx-border-color: "
                                + GameApp.DARK_MATTER_COLOR + ";"));
                card.setDisable(false);
            }
        });
    }

    private static String describe(Landmark landmark) {
        return landmark.name() + " (" + Format.length(landmark.size()) + ")";
    }
}

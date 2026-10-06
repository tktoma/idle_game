package idle.ui;

import idle.core.BigNum;
import idle.core.Game;
import idle.core.Landmark;
import idle.core.SizeScale;
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
 * Contenu de l'onglet « Matière noire ». En haut, la quantité disponible et la quantité gagnée ;
 * en dessous, trois sous-pages :
 * <ul>
 *   <li>« Matière noire » : le point ({@link DarkMatterView}). Tant que le joueur maintient le
 *       clic dessus (ou après un clic, une fois le verrou débloqué dans l'arbre), il grossit, d'autant plus vite que le joueur a de matière noire et que sa
 *       partie est avancée. La vue dézoome à mesure, et l'échelle des grandeurs situe sa taille :
 *       cercles des objets connus autour du point, règle graduée, repère dépassé et repère
 *       suivant. La vue occupe toute la place que laisse la fenêtre ;</li>
 *   <li>« Arbre » : l'arbre d'améliorations ({@link DarkMatterTreePane}), toutes branches réunies.
 *       Trois se paient en particules, en atomes, ou se débloquent par la taille ; la quatrième
 *       se paie en matière noire. La matière noire dépensée reste comptée comme gagnée, et c'est
 *       avec la matière noire gagnée que l'effet de cette branche grandit.</li>
 *   <li>« Défis » : les défis d'explosion ({@link ChallengesPane}).</li>
 * </ul>
 *
 * <p>Le verrou de l'appui est un état du jeu, pas de cette page : une fois posé, la matière noire
 * grossit depuis n'importe quel onglet, jusqu'à la prochaine explosion.
 *
 * <p>Les automatismes de matière noire sont avec les autres, dans l'onglet « Automatisation ».
 *
 * <p>L'onglet n'apparaît qu'après la première explosion du tableau périodique.
 */
final class DarkMatterPage extends VBox {

    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 20; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";

    private final Game game;
    private final Label balanceLabel = new Label();

    private final Button pointTab = new Button("Matière noire");
    private final Button treeTab = new Button("Arbre");
    private final Button challengesTab = new Button("Défis");
    /** Sous-page affichée : 0 = le point, 1 = l'arbre, 2 = les défis. */
    private int selected = 0;

    // Sous-page « Matière noire »
    private final Label sizeLabel = new Label();
    private final Label landmarkLabel = new Label();
    private final Label milestoneLabel = new Label();
    private final Label speedLabel = new Label();
    private final DarkMatterView view = new DarkMatterView(460, 300);
    private final VBox pointPane = new VBox(8);
    /** Vrai tant que le clic est maintenu sur le point. */
    private boolean pressed = false;

    // Sous-page « Arbre »
    private final DarkMatterTreePane tree;
    /** Le nombre de Big Bangs au dernier affichage, pour remarquer qu'il vient d'y en avoir un. */
    private int bigBangs = 0;
    private final VBox treePane = new VBox(10);
    /** Le mode d'emploi de l'arbre : en mode détails seulement. */
    private final Label mockNotice = new Label();
    private final ScrollPane treeScroll = new ScrollPane(treePane);

    // Sous-page « Défis »
    private final ChallengesPane challenges;
    private final ScrollPane challengesScroll;

    DarkMatterPage(Game game) {
        super(8);
        this.game = game;
        this.tree = new DarkMatterTreePane(game);
        this.challenges = new ChallengesPane(game);
        this.challengesScroll = new ScrollPane(challenges);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(16, 24, 16, 24));

        balanceLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + GameApp.DARK_MATTER_COLOR + ";");
        sizeLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #efe4ff;");
        landmarkLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #c9bfe0;");
        landmarkLabel.setWrapText(true);
        landmarkLabel.setTextAlignment(TextAlignment.CENTER);
        milestoneLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7fe0d4;");
        milestoneLabel.setWrapText(true);
        milestoneLabel.setTextAlignment(TextAlignment.CENTER);
        speedLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        speedLabel.setWrapText(true);
        speedLabel.setTextAlignment(TextAlignment.CENTER);

        // Une sous-page n'est recopiée que lorsqu'elle est affichée : elle l'est donc dès qu'on la choisit.
        pointTab.setOnAction(event -> {
            select(0);
            refresh();
        });
        treeTab.setOnAction(event -> {
            select(1);
            refresh();
        });
        challengesTab.setOnAction(event -> {
            select(2);
            refresh();
        });
        HBox subTabs = new HBox(pointTab, treeTab, challengesTab);
        subTabs.setAlignment(Pos.CENTER);

        // Le clic doit commencer sur le point ; il compte ensuite jusqu'à ce qu'on relâche le bouton.
        view.setStyle("-fx-cursor: hand;");
        // Avec le verrou de l'arbre, un clic sur le point bloque l'appui ; un autre clic le libère.
        view.setOnMousePressed(event -> {
            boolean onPoint = view.isOnDarkMatter(event.getX(), event.getY());
            if (onPoint && game.isHoldLockUnlocked()) game.setHoldLocked(!game.isHoldLocked());
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
        pointPane.getChildren().add(milestoneLabel);
        pointPane.getChildren().add(speedLabel);

        // L'arbre, qui défile s'il dépasse la fenêtre.
        mockNotice.setText("Ces améliorations sont définitives : ni la fusion ni l'explosion ne les "
                + "reprennent. Chaque branche se paie avec ce qu'indique son en-tête ; la taille, elle, se débloque "
                + "en l'atteignant, sans rien dépenser. Pour les particules, une seconde de production compte comme "
                + "des particules en main. La matière noire dépensée reste comptée comme gagnée : "
                + "les cases qui demandent « N matières noires gagnées » et la croissance n'y perdent rien.");
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

        // Les sous-pages sont empilées au même endroit ; une seule est visible à la fois.
        challengesScroll.setFitToWidth(true);
        challengesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        challengesScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        StackPane pages = new StackPane(pointPane, treeScroll, challengesScroll);
        VBox.setVgrow(pages, Priority.ALWAYS);

        getChildren().add(balanceLabel);
        getChildren().add(subTabs);
        getChildren().add(pages);
        select(0);
    }

    /** Affiche une sous-page : 0 = le point, 1 = l'arbre, 2 = les défis. */
    private void select(int index) {
        selected = index;
        pressed = false;
        if (index != 1) tree.release();
        if (index != 2) challenges.release();
        pointPane.setVisible(index == 0);
        treeScroll.setVisible(index == 1);
        challengesScroll.setVisible(index == 2);
        pointTab.setStyle(subTabStyle(index == 0));
        treeTab.setStyle(subTabStyle(index == 1));
        challengesTab.setStyle(subTabStyle(index == 2));
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
        if (selected == 1) tree.frame(elapsed);
        if (selected == 2) challenges.frame(elapsed);
        if (selected != 0) return;
        // Verrouillée, elle grossit déjà toute seule à chaque tick du jeu : tenir le clic n'ajoute que la part manquante.
        boolean locked = game.isHoldLocked();
        // Avec l'appui automatique, le jeu tient déjà le clic en entier : le tenir soi-même n'ajoute rien.
        boolean held = game.isAutoHolding();
        if (pressed && !held) game.growDarkMatter(elapsed * timeFactor * (locked ? 1 - Game.HOLD_LOCK_SHARE : 1));
        view.frame(elapsed, game.state().darkMatterSize(), pressed || locked || held);
    }

    /** L'onglet n'est plus affiché : un clic resté « maintenu » ne doit pas reprendre au retour. */
    void release() {
        pressed = false;
        tree.release();
        challenges.release();
    }

    /** La partie recommence de zéro : plus d'appui, et retour à la première sous-page. */
    void reset() {
        select(0);
    }

    /** Recopie l'état du jeu dans les textes. */
    void refresh() {
        refresh(true);
    }

    /**
     * Recopie l'état du jeu dans la page : son en-tête, et la sous-page affichée.
     *
     * @param visible faux quand l'onglet « Matière noire » n'est pas à l'écran : rien n'est alors recopié,
     *                la page le sera à son retour. Elle ne cesse pas pour autant de guetter le Big Bang.
     */
    void refresh(boolean visible) {
        // Un Big Bang vient d'avoir lieu : la page reviendra sur sa première sous-page, à la prochaine explosion.
        if (game.bigBangs() != bigBangs) {
            bigBangs = game.bigBangs();
            release();
            reset();
        }
        if (!visible) return;
        BigNum darkMatter = game.state().darkMatter();
        BigNum earned = game.darkMatterEarned();
        int explosions = game.state().explosions();
        balanceLabel.setText(Format.count(darkMatter) + " matière noire"
                + (earned.gt(darkMatter) ? " disponible, " + Format.count(earned) + " gagnée" : "")
                + "   (" + explosions + (explosions > 1 ? " explosions)" : " explosion)"));

        mockNotice.setVisible(Detail.shown());
        mockNotice.setManaged(Detail.shown());
        challengesTab.setText("Défis (" + game.completedChallenges() + "/" + game.challenges().size() + ")");
        if (selected == 1) tree.refresh();
        if (selected == 2) challenges.refresh();
        if (selected != 0) return;

        BigNum size = game.state().darkMatterSize();
        sizeLabel.setText("Taille : " + Format.length(size));
        Landmark reached = SizeScale.reached(size);
        Landmark next = SizeScale.next(size);
        landmarkLabel.setText((reached == null || !Detail.shown() ? "" : "Dépassé : " + describe(reached) + "   |   ")
                + (next == null ? "Plus grand que tous les repères" : "Prochain repère : " + describe(next)));

        // La vitesse, et d'où elle vient : l'élan (matière noire, avancement, arbre) divisé par la résistance.
        BigNum growth = game.darkMatterGrowthPerSecond();
        String speed = growth.lt(BigNum.of(2))
                ? "+" + ElementText.percent(growth.toDouble() - 1)
                : Format.multiplier(growth);
        boolean locked = game.isHoldLocked();
        String share = ElementText.percent(Game.HOLD_LOCK_SHARE);
        // En quelques mots : quoi faire, et à quelle vitesse. Le calcul complet vient en mode détails.
        String how = game.isAutoHolding() ? "Appui automatique, sans rien tenir : "
                : !game.isHoldLockUnlocked() ? "Maintenez le clic sur le point : "
                : locked ? "Appui verrouillé à " + share + " : "
                : "Cliquez sur le point pour verrouiller l'appui : ";
        String more = game.isAutoHolding() ? " L'appui automatique, acquis avec l'espace, tient le clic pour vous depuis "
                        + "n'importe quel onglet ; il se coupe dans l'onglet Automatisation."
                : !game.isHoldLockUnlocked() ? ""
                : locked ? " Verrouillée, elle grossit seule à " + share + " de cette vitesse, depuis n'importe quel "
                        + "onglet, jusqu'à la prochaine explosion. Un clic sur le point la libère ; tenir le clic donne le reste."
                : " Verrouillé, l'appui vaut " + share + " de la vitesse, depuis n'importe quel onglet.";
        speedLabel.setText(how + speed + " par seconde" + Detail.only(" d'appui." + more + "\n"
                + "Élan : " + ElementText.percent(Game.DARK_MATTER_GROWTH_PER_UNIT) + " × " + Format.count(earned)
                + " matière noire gagnée × avancement de la partie " + ElementText.number(game.darkMatterProgressFactor())
                + (game.darkExpansionMultiplier().gt(BigNum.ONE)
                        ? " × " + Format.amount(game.darkExpansionMultiplier()) + " (arbre)" : "")
                + (game.landmarkExpansionMultiplier() > 1
                        ? " × " + ElementText.number(game.landmarkExpansionMultiplier()) + " (paliers)" : "")
                + ". Résistance : ÷" + Format.amount(game.darkMatterResistance())
                + ", elle double chaque fois que la taille est multipliée par 1 000."
                + (game.darkAutoExpansionShare() > 0
                        ? " Elle grossit aussi seule, à " + ElementText.percent(game.darkAutoExpansionShare())
                                + " de cette vitesse." : "")));

        milestoneLabel.setText(milestones(size));
    }

    /**
     * Les paliers de taille : combien sont atteints et ce que donnera le prochain ; en mode détails,
     * ce qu'ils donnent déjà en tout.
     */
    private String milestones(BigNum size) {
        int reached = game.landmarksReached();
        int total = SizeScale.MILESTONES.size();
        StringBuilder text = new StringBuilder("Paliers de taille : " + reached + " / " + total);
        if (reached < total) {
            String gain = switch (SizeScale.bonusOf(reached)) {
                case PARTICLES -> "particules ×" + ElementText.number(Game.LANDMARK_PARTICLES);
                case ATOMS -> "+" + ElementText.percent(Game.LANDMARK_ATOMS) + " d'atomes par fusion";
                case EXPANSION -> "croissance ×" + ElementText.number(Game.LANDMARK_EXPANSION);
            };
            text.append(", le prochain donne ").append(gain);
        }
        if (reached > 0 && Detail.shown()) {
            text.append("\nDéjà acquis : particules ").append(Format.multiplier(game.landmarkParticlesMultiplier()))
                    .append(", atomes ").append(Format.multiplier(BigNum.of(game.landmarkAtomsMultiplier())))
                    .append(", croissance ").append(Format.multiplier(BigNum.of(game.landmarkExpansionMultiplier())));
        }
        return text.toString();
    }

    private static String describe(Landmark landmark) {
        return landmark.name() + " (" + Format.length(landmark.size()) + ")";
    }
}

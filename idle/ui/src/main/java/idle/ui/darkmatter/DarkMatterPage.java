package idle.ui.darkmatter;

import idle.core.Game;
import idle.core.darkmatter.Landmark;
import idle.core.darkmatter.SizeScale;
import idle.core.math.BigNum;
import idle.ui.GameApp;
import idle.ui.Readiness;
import idle.ui.common.Breakdown;
import idle.ui.common.CanvasPane;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.StableHeight;
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
 * grossit depuis n'importe quel onglet, et le verrou tient d'une partie à l'autre.
 *
 * <p>Les automatismes de matière noire sont avec les autres, dans l'onglet « Automatisation ».
 *
 * <p>L'onglet n'apparaît qu'après la première explosion du tableau périodique.
 */
public final class DarkMatterPage extends VBox {

    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 20; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";

    private final Game game;
    private final Label balanceLabel = new Label();
    /** Au survol : d'où vient la matière noire d'une explosion. */
    private final Breakdown balanceBreakdown;

    private final Button pointTab = new Button("Matière noire");
    private final Button treeTab = new Button("Arbre");
    private final Button challengesTab = new Button("Défis");
    private final HBox subTabs;
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
    /** Le plan des voies (version 0.84), et la réouverture des bifurcations à la prochaine explosion. */
    private final idle.ui.common.PlanBar treePlan;
    private final javafx.scene.control.Button reopen = new javafx.scene.control.Button();

    // Sous-page « Défis »
    private final ChallengesPane challenges;
    private final ScrollPane challengesScroll;

    public DarkMatterPage(Game game) {
        super(8);
        this.game = game;
        this.tree = new DarkMatterTreePane(game);
        this.treePlan = new idle.ui.common.PlanBar(new idle.ui.common.PlanBar.Plan() {
            public String code() { return game.darkTreeCode(); }
            public Game.PlanLoad load(String code) { return game.loadDarkPlan(code); }
            public void save(int slot) { game.saveDarkPlan(slot); }
            public Game.PlanLoad loadSaved(int slot) { return game.loadSavedDarkPlan(slot); }
            public String saved(int slot) { return game.state().savedDarkPlan(slot); }
            public String describe(String code) {
                int[] n = game.describeTreeCode(code);
                return n[0] + (n[0] > 1 ? " voies" : " voie") + (n[1] > 0 ? ", " + n[1] + (n[1] > 1 ? " liaisons" : " liaison") : "");
            }
            public void forget() { game.forgetDarkPlan(); }
            public boolean active() { return !game.darkPlan().isEmpty(); }
            public String progress() { return treePlanProgress(); }
        }, "les voies et les liaisons", GameApp.DARK_MATTER_COLOR);
        this.balanceBreakdown = Breakdown.attach(balanceLabel, game, Breakdown.Of.DARK_MATTER);
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
        subTabs = new HBox(pointTab, treeTab, challengesTab);
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
                + "des particules en main. Les liaisons, entre deux branches, se paient en matière noire. La "
                + "matière noire dépensée reste comptée comme gagnée : la croissance n'y perd rien.");
        mockNotice.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        mockNotice.setWrapText(true);
        mockNotice.setTextAlignment(TextAlignment.CENTER);
        treePane.setAlignment(Pos.TOP_CENTER);
        treePane.setPadding(new Insets(10, 0, 10, 0));
        treePane.getChildren().add(mockNotice);
        reopen.setFocusTraversable(false);
        reopen.setOnAction(event -> {
            game.setForkResetArmed(!game.state().forkResetArmed());
            refresh();
        });
        treePane.getChildren().add(treePlan);
        treePane.getChildren().add(reopen);
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

        getChildren().add(StableHeight.of(balanceLabel));
        getChildren().add(subTabs);
        getChildren().add(pages);
        select(0);
    }

    /** Passe à la sous-page suivante ou précédente, en boucle. */
    public void step(int direction) {
        select(Math.floorMod(selected + direction, 3));
        refresh();
    }

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public HBox subTabs() {
        return subTabs;
    }

    /** La sous-page affichée : 0 le point, 1 l'arbre, 2 les défis. Pour les vérifications. */
    public int selected() {
        return selected;
    }

    /**
     * Le raccourci du Big Bang : la page montre l'arbre et presse sa dernière case, comme un clic.
     * Avec la confirmation, un premier appui l'arme et le second déclenche.
     */
    public void pressBigBang() {
        if (selected != 1) select(1);
        // L'arbre n'est recopié que lorsqu'il est affiché : sa case doit d'abord savoir que tout est réuni.
        tree.refresh();
        tree.bangCard().click();
        refresh();
    }

    /** Le plan des voies et le bouton de réouverture (version 0.84). */
    private void refreshPlan() {
        treePlan.refresh();
        boolean armed = game.state().forkResetArmed();
        boolean can = game.canArmForkReset();
        reopen.setVisible(can || armed);
        reopen.setManaged(can || armed);
        reopen.setText(armed ? "Les bifurcations se rouvriront à la prochaine explosion (annuler)"
                : "Rouvrir les bifurcations à la prochaine explosion");
        reopen.setStyle("-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + (armed ? " -fx-text-fill: #1a0f2e; -fx-background-color: " + GameApp.DARK_MATTER_COLOR + ";"
                        : " -fx-text-fill: " + GameApp.DARK_MATTER_COLOR + "; -fx-background-color: #141a26; -fx-border-color: "
                                + GameApp.DARK_MATTER_COLOR + "88;"));
    }

    /** Où en est le plan des voies : prises, en attente, bloquées. */
    private String treePlanProgress() {
        int taken = 0, waiting = 0, blocked = 0;
        for (idle.core.darkmatter.DarkUpgrade dark : game.darkPlan()) {
            switch (game.darkPlanStep(dark)) {
                case TAKEN -> taken++;
                case WAITING -> waiting++;
                case BLOCKED -> blocked++;
            }
        }
        int all = taken + waiting + blocked;
        return taken + " / " + all + " prises" + (waiting > 0 ? ", " + waiting + " en attente (prix ou case du dessus)" : "")
                + (blocked > 0 ? ", " + blocked + " fermée(s) par l'autre voie jusqu'à la réouverture des bifurcations" : "")
                + ". Elles se reprennent seules après chaque Big Bang.";
    }

    /** La barre du plan des voies : pour les vérifications. */
    public idle.ui.common.PlanBar treePlan() {
        return treePlan;
    }

    /** Le bouton de réouverture des bifurcations : pour les vérifications. */
    public javafx.scene.control.Button reopenButton() {
        return reopen;
    }

    /** L'arbre : pour les vérifications. */
    public DarkMatterTreePane treePane() {
        return tree;
    }

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
    public void frame(double elapsed, double timeFactor) {
        if (selected == 1) tree.frame(elapsed);
        if (selected == 2) challenges.frame(elapsed);
        if (selected != 0) return;
        // Verrouillée, elle grossit déjà toute seule à chaque tick du jeu : tenir le clic n'ajoute que la part manquante.
        boolean locked = game.isHoldLocked();
        // Avec l'appui automatique, le jeu tient déjà le clic en entier : le tenir soi-même n'ajoute rien.
        boolean held = game.isAutoHolding();
        if (pressed && !held) game.growDarkMatter(elapsed * timeFactor * (locked ? 1 - Game.HOLD_LOCK_SHARE : 1));
        view.frame(elapsed, game.state().darkMatterSize(), pressed || locked || held, game.darkMatterMomentum());
    }

    /** L'onglet n'est plus affiché : un clic resté « maintenu » ne doit pas reprendre au retour. */
    public void release() {
        pressed = false;
        tree.release();
        challenges.release();
    }

    /** La partie recommence de zéro : plus d'appui, et retour à la première sous-page. */
    public void reset() {
        select(0);
    }

    /** Recopie l'état du jeu dans les textes. */
    public void refresh() {
        refresh(true);
    }

    /**
     * Recopie l'état du jeu dans la page : son en-tête, et la sous-page affichée.
     *
     * @param visible faux quand l'onglet « Matière noire » n'est pas à l'écran : rien n'est alors recopié,
     *                la page le sera à son retour. Elle ne cesse pas pour autant de guetter le Big Bang.
     */
    public void refresh(boolean visible) {
        view.setSkin(game.chosenSkin(idle.core.universe.SkinVisual.DARK_BALL));
        view.setAnimation(game.chosenSkin(idle.core.universe.SkinVisual.DARK_ANIMATION));
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
        balanceLabel.setText(Format.darkMatter(darkMatter) + " matière noire"
                + (earned.gt(darkMatter) ? " disponible, " + Format.count(earned) + " gagnée" : "")
                + "   (" + explosions + (explosions > 1 ? " explosions)" : " explosion)"));
        balanceBreakdown.refresh();

        mockNotice.setVisible(Detail.shown());
        mockNotice.setManaged(Detail.shown());
        Readiness ready = Readiness.of(game);
        treeTab.setText("Arbre" + Readiness.dot(ready.tree()));
        challengesTab.setText("Défis (" + game.completedChallenges() + "/" + game.challenges().size() + ")" + Readiness.dot(ready.challenges()));
        if (selected == 1) {
            tree.refresh();
            refreshPlan();
        }
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
                        + "onglet, et le verrou tient d'une partie à l'autre. Un clic sur le point la libère ; tenir le clic donne le reste."
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

package idle.ui.collider;

import idle.core.Game;
import idle.core.collider.ColliderUpgrade;
import idle.ui.Readiness;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * L'onglet du collisionneur (version 0.057, refait en 0.059), ouvert à la première explosion : trois
 * sous-onglets : l'énergie (les faisceaux, leurs collisions et les huit générateurs), la chaîne
 * (énergie → molécules, payée en énergie et en quarks) et les améliorations, qui n'apparaissent
 * qu'avec le huitième générateur. Les hadrons ont disparu en 0.062.
 */
public final class ColliderPage extends VBox {

    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 20; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";

    private final Game game;
    private final ColliderPane pane;
    private final Button energyTab = new Button("Énergie");
    private final Button chainTab = new Button("Chaîne");
    private final Button upgradesTab = new Button("Améliorations");
    private final HBox subTabs = new HBox(energyTab, chainTab, upgradesTab);

    public ColliderPage(Game game) {
        super(6);
        this.game = game;
        this.pane = new ColliderPane(game, this::refresh);
        setAlignment(Pos.TOP_CENTER);
        Button[] tabs = {energyTab, chainTab, upgradesTab};
        for (int i = 0; i < tabs.length; i++) {
            int index = i;
            tabs[i].setFocusTraversable(false);
            tabs[i].setOnAction(event -> {
                select(index);
                refresh();
            });
        }
        subTabs.setAlignment(Pos.CENTER);
        ScrollPane scroll = new ScrollPane(pane);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().addAll(subTabs, scroll);
        select(0);
    }

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public HBox subTabs() {
        return subTabs;
    }

    public ColliderPane pane() {
        return pane;
    }

    public void select(int index) {
        if (index == ColliderPane.UPGRADES && !game.isColliderUpgradesUnlocked()) index = ColliderPane.ENERGY;
        pane.select(index);
        energyTab.setStyle(style(index == ColliderPane.ENERGY));
        chainTab.setStyle(style(index == ColliderPane.CHAIN));
        upgradesTab.setStyle(style(index == ColliderPane.UPGRADES));
    }

    public int selected() {
        return pane.section();
    }

    /** Passe au sous-onglet suivant ou précédent, en boucle. */
    public void step(int direction) {
        // Les sous-onglets affichés, dans l'ordre : les améliorations sautent tant qu'elles sont fermées.
        int[] shown = game.isColliderUpgradesUnlocked()
                ? new int[] {ColliderPane.ENERGY, ColliderPane.CHAIN, ColliderPane.UPGRADES}
                : new int[] {ColliderPane.ENERGY, ColliderPane.CHAIN};
        int at = 0;
        for (int i = 0; i < shown.length; i++) if (shown[i] == pane.section()) at = i;
        select(shown[Math.floorMod(at + direction, shown.length)]);
        refresh();
    }

    private static String style(boolean selected) {
        return SUB_TAB_STYLE + (selected
                ? " -fx-text-fill: " + ColliderPane.COLOR + "; -fx-border-color: transparent transparent " + ColliderPane.COLOR + " transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;");
    }

    /** Vrai s'il y a quelque chose à faire : un quark top, une amélioration à prendre. */
    public static boolean ready(Game game) {
        if (!game.isColliderUnlocked()) return false;
        return game.hasTop() || upgradeReady(game, true) || upgradeReady(game, false);
    }

    /** Vrai si une amélioration payable attend : celles de la chaîne (son sous-onglet), ou les autres. */
    private static boolean upgradeReady(Game game, boolean chain) {
        for (ColliderUpgrade upgrade : ColliderUpgrade.values()) {
            if ((upgrade.section() == ColliderUpgrade.Section.CHAIN) == chain && game.canBuyColliderUpgrade(upgrade)) return true;
        }
        return false;
    }

    public void refresh() {
        boolean upgrades = game.isColliderUpgradesUnlocked();
        upgradesTab.setVisible(upgrades);
        upgradesTab.setManaged(upgrades);
        energyTab.setText("Énergie" + Readiness.dot(game.hasTop()));
        chainTab.setText("Chaîne" + Readiness.dot(upgradeReady(game, true)));
        upgradesTab.setText("Améliorations" + Readiness.dot(upgradeReady(game, false)));
        pane.refresh();
    }

    public void frame(double elapsed) {
        pane.frame(elapsed);
    }
}

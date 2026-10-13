package idle.ui.collider;

import idle.core.Game;
import idle.core.collider.CollisionTier;
import idle.core.collider.Hadron;
import idle.ui.Readiness;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * L'onglet du collisionneur (version 0.57), ouvert à la première explosion : trois sous-onglets,
 * l'énergie et ses huit générateurs, les collisions, les hadrons et leurs paliers.
 */
public final class ColliderPage extends VBox {

    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 20; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";

    private final Game game;
    private final ColliderPane pane;
    private final Button energyTab = new Button("Énergie");
    private final Button collisionsTab = new Button("Collisions");
    private final Button hadronsTab = new Button("Hadrons");
    private final HBox subTabs = new HBox(energyTab, collisionsTab, hadronsTab);

    public ColliderPage(Game game) {
        super(6);
        this.game = game;
        this.pane = new ColliderPane(game, this::refresh);
        setAlignment(Pos.TOP_CENTER);
        Button[] tabs = {energyTab, collisionsTab, hadronsTab};
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
        pane.select(index);
        energyTab.setStyle(style(index == 0));
        collisionsTab.setStyle(style(index == 1));
        hadronsTab.setStyle(style(index == 2));
    }

    public int selected() {
        return pane.section();
    }

    /** Passe au sous-onglet suivant ou précédent, en boucle. */
    public void step(int direction) {
        select(Math.floorMod(pane.section() + direction, 3));
        refresh();
    }

    private static String style(boolean selected) {
        return SUB_TAB_STYLE + (selected
                ? " -fx-text-fill: " + ColliderPane.COLOR + "; -fx-border-color: transparent transparent " + ColliderPane.COLOR + " transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;");
    }

    /** Vrai s'il y a quelque chose à faire : une place de collision libre et payable, un quark top, un hadron à former. */
    public static boolean ready(Game game) {
        if (!game.isColliderUnlocked()) return false;
        boolean idleSlot = game.state().collisions().size() < game.colliderSlots() && game.canStartCollision(CollisionTier.LOW);
        return idleSlot || game.hasTop() || hadronReady(game);
    }

    private static boolean hadronReady(Game game) {
        for (Hadron hadron : Hadron.values()) if (game.canFormHadron(hadron)) return true;
        return false;
    }

    public void refresh() {
        boolean slot = game.state().collisions().size() < game.colliderSlots() && game.canStartCollision(CollisionTier.LOW);
        collisionsTab.setText("Collisions (" + game.state().collisions().size() + "/" + game.colliderSlots() + ")"
                + Readiness.dot(slot || game.hasTop()));
        hadronsTab.setText("Hadrons" + Readiness.dot(hadronReady(game)));
        pane.refresh();
    }

    public void frame(double elapsed) {
        pane.frame(elapsed);
    }
}

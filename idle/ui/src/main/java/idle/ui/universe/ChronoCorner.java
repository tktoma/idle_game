package idle.ui.universe;

import idle.core.Game;
import idle.ui.common.Format;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

/**
 * Le chrono dans un coin de la fenêtre (version 0.182) : une fois son étage acheté, il se règle depuis n'importe quel
 * onglet, sans revenir à la sous-page Graine, où il reste aussi ({@link SeedUpgradesPane}). Une petite carte en bas à
 * gauche : le temps gardé, l'accélération, et le curseur. Elle disparaît sur la sous-page Graine (le chrono y est déjà)
 * et tant que l'étage n'est pas acheté.
 */
public final class ChronoCorner extends VBox {

    private final Game game;
    private final Label line = new Label();
    private final Slider speed = new Slider(1, 2, 1);
    private boolean syncing;

    public ChronoCorner(Game game) {
        super(4);
        this.game = game;
        setAlignment(Pos.BOTTOM_LEFT);
        setPadding(new Insets(16));
        setMaxWidth(USE_PREF_SIZE);
        setMaxHeight(USE_PREF_SIZE);
        setPickOnBounds(false);
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: #16202eee; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #8fd3ff;"
                + " -fx-padding: 8 12;");
        Label title = new Label("Le chrono");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #8fd3ff;");
        line.setStyle("-fx-font-size: 12px; -fx-text-fill: #b8c8d8;");
        speed.setBlockIncrement(0.25);
        speed.setMajorTickUnit(0.25);
        speed.setMinorTickCount(0);
        speed.setSnapToTicks(true);
        speed.setFocusTraversable(false);
        speed.setPrefWidth(200);
        speed.valueProperty().addListener((obs, before, after) -> {
            if (syncing) return;
            game.setChronoSpeed(Math.round(after.doubleValue() * 4) / 4.0);
            refreshText();
        });
        card.getChildren().addAll(title, line, speed);
        getChildren().add(card);
        setVisible(false);
        setManaged(false);
    }

    /**
     * Recopie le chrono ; {@code seedShown} vrai quand la sous-page Graine est affichée : la carte se cache, le chrono y
     * est déjà. Vrai si la carte est visible.
     */
    public boolean refresh(boolean seedShown) {
        boolean shown = game.isChronoUnlocked() && !seedShown;
        setVisible(shown);
        setManaged(shown);
        if (!shown) return false;
        if (!speed.isValueChanging()) {
            syncing = true;
            speed.setMax(game.chronoMaxSpeed());
            speed.setValue(game.chronoSpeed());
            syncing = false;
        }
        refreshText();
        return true;
    }

    private void refreshText() {
        double chosen = game.chronoSpeed();
        line.setText("Gardé " + Format.duration(game.chronoStored()) + " sur " + Format.duration(game.chronoCapacity())
                + (chosen <= 1 + 1e-9 ? " · ×1, il se remplit"
                : " · ×" + Format.darkMatter(chosen) + (game.isChronoRunning() ? ", le jeu accélère" : ", vide : il attend")));
    }

    /** Pour les vérifications. */
    public Slider slider() {
        return speed;
    }

    public Label text() {
        return line;
    }
}

package idle.ui.universe;

import idle.core.Game;
import idle.ui.common.Format;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Le chrono hors de la sous-page Graine : une fois son étage acheté, il se règle depuis n'importe quel onglet, sans
 * revenir à la sous-page Graine, où il reste aussi ({@link SeedUpgradesPane}). Le temps gardé, l'accélération, et le
 * curseur. Il disparaît sur la sous-page Graine (le chrono y est déjà) et tant que l'étage n'est pas acheté.
 * <p> Une bande d'une ligne, rangée dans la mise en page (sous la ligne de l'objectif, et dans l'en-tête de la fenêtre
 * d'une dimension), au lieu d'une carte flottante en bas à gauche, qui passait par-dessus les cartes des pages, les
 * améliorations des dimensions et la colonne des onglets. Elle a sa place à elle : elle ne recouvre plus rien.
 */
public final class ChronoCorner extends VBox {
    private final Game game;
    private final Label line = new Label();
    private final Slider speed = new Slider(1, 2, 1);
    private boolean syncing;

    public ChronoCorner(Game game) {
        super(0);
        this.game = game;
        setAlignment(Pos.CENTER);
        setPadding(new Insets(0));
        setMaxHeight(USE_PREF_SIZE);
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: #0e131b; -fx-border-color: transparent transparent #1d2633 transparent; -fx-padding: 2 12;");
        Label title = new Label("Chrono");
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #8fd3ff;");
        title.setMinWidth(USE_PREF_SIZE);
        line.setStyle("-fx-font-size: 12px; -fx-text-fill: #b8c8d8;");
        speed.setBlockIncrement(0.25);
        speed.setMajorTickUnit(0.25);
        speed.setMinorTickCount(0);
        speed.setSnapToTicks(true);
        speed.setFocusTraversable(false);
        speed.setPrefWidth(180);
        speed.setMinWidth(120);
        speed.valueProperty().addListener((obs, before, after) -> {
            if (syncing) return;
            game.setChronoSpeed(Math.round(after.doubleValue() * 4) / 4.0);
            refreshText();
        });
        line.setTooltip(new Tooltip());
        // Version 0.309 : le titre, le curseur, puis l'accélération et le temps gardé ; la phrase entière au survol.
        card.getChildren().addAll(title, speed, line);
        getChildren().add(card);
        setVisible(false);
        setManaged(false);
    }

    /**
     * Recopie le chrono ; {@code hidden} vrai quand la sous-page affichée s'en passe : la Graine, qui a déjà le sien, et
     * depuis la version 0.315 l'Expansion du Big Bang. Vrai si la carte est visible.
     */
    public boolean refresh(boolean hidden) {
        boolean shown = game.isChronoUnlocked() && !hidden;
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
        line.setText("×" + Format.darkMatter(chosen) + "   " + Format.duration(game.chronoStored()));
        line.getTooltip().setText("Gardé " + Format.duration(game.chronoStored()) + " sur " + Format.duration(game.chronoCapacity())
                + (chosen <= 1 + 1e-9 ? " · ×1, il se remplit"
                : " · ×" + Format.darkMatter(chosen) + (game.isChronoRunning() ? ", le jeu accélère" : ", vide : il attend")));
    }

    /** Pour les vérifications. */
    public Slider slider() { return speed; }

    public Label text() { return line; }
}

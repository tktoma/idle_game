package idle.ui;

import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Les pastilles des grands pas, version 0.309 : sous le bandeau, à gauche, une pastille de couleur s'allume pour chaque
 * grand pas possible tout de suite (fusion, explosion, Big Bang, univers), sans texte. Elles gardent toujours le même
 * ordre, pour qu'une couleur garde sa place. Le nom au survol ; un clic mène où le pas se fait. Elles remplacent la
 * barre violette de l'explosion, qui était la seule de son genre.
 */
final class StepDots extends HBox {
    /** Les grands pas, dans l'ordre des actes. */
    enum Step {
        FUSION("#e0a030", "Fusion possible"),
        EXPLOSION("#7b4fd6", "Explosion possible"),
        BIG_BANG("#e05a3a", "Big Bang possible"),
        UNIVERSE("#2fb8a0", "Univers possible");

        final String color;
        final String name;

        Step(String color, String name) {
            this.color = color;
            this.name = name;
        }
    }

    /** Rayon d'une pastille, en pixels. */
    static final double RADIUS = 7;

    private final Map<Step, Circle> dots = new EnumMap<>(Step.class);
    private final Map<Step, Tooltip> tips = new EnumMap<>(Step.class);

    StepDots() {
        super(10);
        setAlignment(Pos.CENTER_LEFT);
        for (Step step : Step.values()) {
            Circle dot = new Circle(RADIUS, Color.web(step.color));
            DropShadow glow = new DropShadow(10, Color.web(step.color));
            glow.setSpread(0.25);
            dot.setEffect(glow);
            dot.setCursor(Cursor.HAND);
            Tooltip tip = new Tooltip(step.name);
            Tooltip.install(dot, tip);
            dot.setVisible(false);
            dot.setManaged(false);
            dots.put(step, dot);
            tips.put(step, tip);
            getChildren().add(dot);
        }
    }

    /** Ce que fait un clic sur la pastille de ce pas. */
    void setOnClick(Step step, Runnable action) { dots.get(step).setOnMouseClicked(event -> action.run()); }

    /** Allume ou éteint la pastille de ce pas ; {@code hint} : le texte au survol, sous son nom. */
    void show(Step step, boolean possible, String hint) {
        Circle dot = dots.get(step);
        if (dot.isVisible() != possible) {
            dot.setVisible(possible);
            dot.setManaged(possible);
        }
        String text = hint == null || hint.isEmpty() ? step.name : step.name + "\n" + hint;
        if (possible && !text.equals(tips.get(step).getText())) tips.get(step).setText(text);
    }

    /** Vrai si la pastille de ce pas est allumée : pour les vérifications. */
    boolean shown(Step step) { return dots.get(step).isVisible(); }
}

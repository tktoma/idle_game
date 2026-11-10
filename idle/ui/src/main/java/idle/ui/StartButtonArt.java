package idle.ui;

import idle.core.universe.Skin;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.paint.Color;

/**
 * Les quatre skins du bouton de démarrage (version 0.199), à côté de {@link GameAppWindow#showStartButton} : un style
 * et une petite icône dessinée au code, posée à gauche du texte. Le classique ne passe pas ici et garde son style au
 * caractère près.
 * <ul>
 * <li>« Interrupteur » : une pastille grise, un interrupteur à bascule dont le levier est encore sur « arrêt » ;</li>
 * <li>« Graine qu'on plante » : terre brune, une graine d'où sort une pousse ;</li>
 * <li>« Allumette » : fond de nuit, une allumette dont la tête rouge s'enflamme ;</li>
 * <li>« Détonateur » : boîtier rouge, un piston en T et la bande jaune d'avertissement.</li>
 * </ul>
 */
final class StartButtonArt {

    static final double ICON = 30;

    private StartButtonArt() {
    }

    /** Vrai pour les skins habillés ici. */
    static boolean handles(Skin skin) {
        return skin != null && skin.visual() == idle.core.universe.SkinVisual.START_BUTTON;
    }

    /** Habille le bouton de ce skin : son style et son icône. */
    static void dress(Button button, Skin skin) {
        if (!handles(skin)) return;
        button.setStyle(style(skin));
        button.setGraphic(icon(skin));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(12);
        button.setAlignment(Pos.CENTER);
    }

    /** Le style du bouton sous ce skin. */
    static String style(Skin skin) {
        String base = "-fx-font-size: 16px; -fx-padding: 12 28; -fx-cursor: hand;";
        return switch (skin) {
            case START_SWITCH -> base + " -fx-text-fill: #e8f4ff; -fx-background-color: #2a3242; -fx-background-radius: 24;"
                    + " -fx-border-color: #9fd0ff; -fx-border-radius: 24; -fx-border-width: 2;";
            case START_SEED -> base + " -fx-text-fill: #e9f7dc; -fx-font-family: 'Serif'; -fx-background-color: #3a2a16; -fx-background-radius: 16;"
                    + " -fx-border-color: #9be7a8; -fx-border-radius: 16; -fx-border-style: dashed;";
            case START_MATCH -> base + " -fx-text-fill: #ffe0b0; -fx-background-color: #1a1410; -fx-background-radius: 4;"
                    + " -fx-border-color: #ffb347; -fx-border-radius: 4;";
            case START_DETONATOR -> base + " -fx-text-fill: #fff3e0; -fx-font-weight: bold; -fx-background-color: #5a1a1a; -fx-background-radius: 2;"
                    + " -fx-border-color: #ff8a75; -fx-border-radius: 2; -fx-border-width: 3;";
            default -> base;
        };
    }

    /** L'icône de ce skin, dessinée au code. */
    static Canvas icon(Skin skin) {
        Canvas canvas = new Canvas(ICON, ICON);
        GraphicsContext g = canvas.getGraphicsContext2D();
        switch (skin) {
            case START_SWITCH -> {
                g.setFill(Color.web("#141a26"));
                g.fillRoundRect(3, 9, 24, 12, 12, 12);
                g.setStroke(Color.web("#9fd0ff"));
                g.setLineWidth(1.5);
                g.strokeRoundRect(3, 9, 24, 12, 12, 12);
                g.setFill(Color.web("#e8f4ff"));
                g.fillOval(5, 11, 8, 8);
                g.setFill(Color.web("#ff8a75"));
                g.fillOval(20, 13, 4, 4);
            }
            case START_SEED -> {
                g.setFill(Color.web("#5a3a1e"));
                g.fillRect(2, 20, 26, 8);
                g.setFill(Color.web("#8a5a2e"));
                g.fillOval(10, 16, 10, 7);
                g.setStroke(Color.web("#9be7a8"));
                g.setLineWidth(2);
                g.strokeLine(15, 17, 15, 7);
                g.setFill(Color.web("#9be7a8"));
                g.fillOval(15, 5, 8, 5);
                g.fillOval(7, 9, 8, 5);
            }
            case START_MATCH -> {
                g.setStroke(Color.web("#d9b77a"));
                g.setLineWidth(3);
                g.strokeLine(9, 27, 18, 13);
                g.setFill(Color.web("#c0392b"));
                g.fillOval(15, 9, 7, 7);
                g.setFill(Color.color(1, 0.7, 0.2, 0.9));
                g.fillOval(13, 1, 11, 11);
                g.setFill(Color.web("#fff1a0"));
                g.fillOval(16, 4, 5, 6);
            }
            case START_DETONATOR -> {
                g.setFill(Color.web("#8a1c1c"));
                g.fillRect(4, 14, 22, 13);
                g.setFill(Color.web("#ffd23f"));
                g.fillRect(4, 22, 22, 3);
                g.setStroke(Color.web("#d0d0d0"));
                g.setLineWidth(3);
                g.strokeLine(15, 14, 15, 5);
                g.strokeLine(8, 4, 22, 4);
            }
            default -> { }
        }
        return canvas;
    }
}

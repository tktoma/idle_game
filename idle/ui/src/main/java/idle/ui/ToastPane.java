package idle.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Les notifications : de courts messages empilés en bas à droite de la fenêtre, qui s'effacent
 * d'eux-mêmes au bout de quelques secondes. La zone ne prend aucun clic : ce qui est dessous
 * reste cliquable.
 */
final class ToastPane extends VBox {

    /** Durée d'affichage d'un message, en secondes ; il s'estompe pendant la dernière. */
    private static final double SECONDS = 5;
    private static final double FADE_SECONDS = 1;
    /** Nombre de messages visibles à la fois : au-delà, le plus ancien laisse sa place. */
    private static final int MAX_VISIBLE = 4;
    private static final String STYLE = "-fx-font-size: 13px; -fx-padding: 8 14; -fx-text-fill: #e8f4ff;"
            + " -fx-background-color: #16202eee; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: ";

    private final List<Label> toasts = new ArrayList<>();
    private final List<Double> remaining = new ArrayList<>();

    ToastPane() {
        super(6);
        setAlignment(Pos.BOTTOM_RIGHT);
        setPadding(new Insets(16));
        setMouseTransparent(true);
    }

    /** Affiche un message, avec un liseré de la couleur donnée. */
    void show(String text, String color) {
        Label toast = new Label(text);
        toast.setStyle(STYLE + color + ";");
        toast.setWrapText(true);
        toast.setMaxWidth(440);
        toasts.add(toast);
        remaining.add(SECONDS);
        getChildren().add(toast);
        while (toasts.size() > MAX_VISIBLE) remove(0);
    }

    /** Nombre de messages affichés en ce moment. */
    int count() {
        return toasts.size();
    }

    /**
     * Fait passer le temps : les messages s'estompent puis disparaissent.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    void frame(double elapsed) {
        for (int i = toasts.size() - 1; i >= 0; i--) {
            double left = remaining.get(i) - elapsed;
            if (left <= 0) {
                remove(i);
            } else {
                remaining.set(i, left);
                toasts.get(i).setOpacity(Math.min(1, left / FADE_SECONDS));
            }
        }
    }

    /** Efface tous les messages : la partie recommence, ou les notifications viennent d'être coupées. */
    void clear() {
        while (!toasts.isEmpty()) remove(0);
    }

    private void remove(int index) {
        getChildren().remove(toasts.remove(index));
        remaining.remove(index);
    }
}

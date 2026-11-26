package idle.ui.common;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * La ressource d'un onglet au centre du bandeau (version 0.309) : son nom en couleur devant le nombre (« Particules :
 * 4.65e74897724 »), presque de la même taille, et une ligne plus petite dessous (le débit, ce que donne une fusion…).
 * Un peu plus petits quand le bandeau rapetisse.
 * <p>Version 0.315 : {@link #title} n'y met qu'un titre, sans nombre (l'onglet Automatisation).
 */
public final class ResourceHead extends VBox {
    private final Label title;
    private final Label value = new Label();
    private final Label sub = new Label();
    private final String color;
    private final boolean titleOnly;
    private boolean compact;

    /** {@code name} sans les deux points : « Particules », « Atomes ». */
    public ResourceHead(String name, String color) {
        this(name + " :", color, false);
    }

    /** Un titre seul, sans nombre ni ligne dessous. */
    public static ResourceHead title(String name, String color) {
        return new ResourceHead(name, color, true);
    }

    private ResourceHead(String text, String color, boolean titleOnly) {
        this.title = new Label(text);
        this.color = color;
        this.titleOnly = titleOnly;
        value.setVisible(!titleOnly);
        value.setManaged(!titleOnly);
        sub.setVisible(!titleOnly);
        sub.setManaged(!titleOnly);
        HBox line = new HBox(8, title, value);
        line.setAlignment(Pos.BASELINE_CENTER);
        sub.setTextAlignment(TextAlignment.CENTER);
        setAlignment(Pos.CENTER);
        getChildren().addAll(line, sub);
        style();
    }

    /** Le nombre, et la ligne du dessous. */
    public void set(String number, String below) {
        if (!number.equals(value.getText())) value.setText(number);
        if (!below.equals(sub.getText())) sub.setText(below);
    }

    /** En petit, quand le bandeau rapetisse. */
    public void setCompact(boolean small) {
        if (small == compact) return;
        compact = small;
        style();
    }

    /** Le titre, avec ses deux points s'il porte un nombre. */
    public String titleText() { return title.getText(); }

    /** Le nombre, pour y accrocher une bulle ou pour les vérifications. */
    public Label value() { return value; }

    /** La ligne du dessous, pour y accrocher une bulle ou pour les vérifications. */
    public Label sub() { return sub; }

    private void style() {
        int size = titleOnly ? (compact ? 18 : 24) : compact ? 16 : 22;
        title.setStyle("-fx-font-size: " + size + "px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        value.setStyle("-fx-font-size: " + (compact ? 17 : 24) + "px; -fx-font-weight: bold; -fx-text-fill: #e8f4ff;");
        sub.setStyle("-fx-font-size: " + (compact ? 10 : 13) + "px; -fx-text-fill: #8fa3b8;");
    }
}

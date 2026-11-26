package idle.ui;

import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Le bandeau du haut, version 0.309 : à gauche les objectifs, une ligne courte chacun ; au centre ce que l'onglet
 * affiché y met (le compteur des particules, pour commencer) ; à droite l'aide et les détails, et dessous ce que
 * l'onglet y range (la quantité par achat). Le centre reste au milieu de la fenêtre : les deux côtés se partagent
 * la place qui reste, et les objectifs trop longs se coupent (le texte complet au survol).
 * <p>Sous {@link #COMPACT_WIDTH}, tout rapetisse au lieu de passer à la ligne : le bandeau garde une seule hauteur ; la
 * ressource du centre ({@link idle.ui.common.ResourceHead}) aussi.
 */
final class PageHeader extends StackPane {
    /** En dessous de cette largeur, les textes et les boutons du bandeau rapetissent. */
    static final double COMPACT_WIDTH = 860;

    private final VBox goals = new VBox(1);
    private final StackPane center = new StackPane();
    private final HBox keys = new HBox(6);
    private final StackPane extra = new StackPane();
    private final VBox right = new VBox(5, keys, extra);
    private boolean compact;

    PageHeader(Node help, Node detail) {
        keys.getChildren().addAll(help, detail);
        keys.setAlignment(Pos.CENTER_RIGHT);
        extra.setAlignment(Pos.CENTER_RIGHT);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        goals.setAlignment(Pos.CENTER_LEFT);
        goals.setMaxHeight(USE_PREF_SIZE);
        goals.setMinWidth(0);
        center.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setAlignment(goals, Pos.CENTER_LEFT);
        setAlignment(right, Pos.CENTER_RIGHT);
        setAlignment(center, Pos.CENTER);
        getChildren().addAll(goals, right, center);
        // Les objectifs n'ont que la moitié de ce que le centre laisse : ils ne passent jamais dessous.
        goals.maxWidthProperty().bind(widthProperty().subtract(center.widthProperty()).divide(2).subtract(18));
        setPadding(new Insets(6, 14, 6, 14));
        setStyle("-fx-background-color: #0e131b; -fx-border-color: transparent transparent #1d2633 transparent;");
        widthProperty().addListener((obs, before, after) -> {
            boolean small = after.doubleValue() > 0 && after.doubleValue() < COMPACT_WIDTH;
            if (small == compact) return;
            compact = small;
            styleGoals();
            fitCenter();
        });
    }

    /** Vrai quand le bandeau est en petit. */
    boolean compact() { return compact; }

    /** Ce que l'onglet affiché met au centre ; {@code null} : rien. */
    void setCenter(Node node) {
        if (node == null) center.getChildren().clear();
        else if (center.getChildren().size() != 1 || center.getChildren().get(0) != node) center.getChildren().setAll(node);
        fitCenter();
    }

    /** La ressource du centre suit la taille du bandeau : en petit quand il rapetisse. */
    private void fitCenter() {
        for (Node node : center.getChildren()) if (node instanceof idle.ui.common.ResourceHead head) head.setCompact(compact);
    }

    /** Ce que l'onglet affiché range sous l'aide et les détails ; {@code null} : rien. */
    void setExtra(Node node) {
        if (node == null) extra.getChildren().clear();
        else if (extra.getChildren().size() != 1 || extra.getChildren().get(0) != node) extra.getChildren().setAll(node);
        extra.setVisible(node != null);
        extra.setManaged(node != null);
    }

    /** Les objectifs, une ligne chacun : le texte court, et au survol le texte complet. */
    void setGoals(List<Goals.Line> lines) {
        while (goals.getChildren().size() < lines.size()) {
            Label label = new Label();
            label.setTextOverrun(OverrunStyle.ELLIPSIS);
            label.setMinWidth(0);
            label.setTooltip(new Tooltip());
            goals.getChildren().add(label);
            styleGoals();
        }
        for (int i = 0; i < goals.getChildren().size(); i++) {
            Label label = (Label) goals.getChildren().get(i);
            boolean shown = i < lines.size();
            label.setVisible(shown);
            label.setManaged(shown);
            if (!shown) continue;
            Goals.Line line = lines.get(i);
            if (!line.brief().equals(label.getText())) label.setText(line.brief());
            if (!line.full().equals(label.getTooltip().getText())) label.getTooltip().setText(line.full());
        }
    }

    /** Les textes des objectifs affichés, pour les vérifications. */
    List<String> goalTexts() {
        return goals.getChildren().stream().filter(Node::isVisible).map(node -> ((Label) node).getText()).toList();
    }

    private void styleGoals() {
        String style = "-fx-font-size: " + (compact ? 10 : 11) + "px; -fx-text-fill: #8fa3b8;";
        for (Node node : goals.getChildren()) node.setStyle(style);
    }
}

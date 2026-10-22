package idle.ui.pages;

import idle.core.Game;
import idle.core.Multipliers;
import idle.core.math.BigNum;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Ce qui fait grandir chaque grandeur (version 0.123), comme le détail des multiplicateurs d'Antimatter
 * Dimensions : une barre partagée entre les aspects du jeu, au prorata des ordres de grandeur que chacun
 * apporte, puis une ligne par aspect, la plus lourde en haut. Un clic sur une ligne déplie ses facteurs.
 *
 * <p>Tout vient de {@link Multipliers} : la somme des parts fait la grandeur entière. Le contenu n'est
 * recomposé que deux fois par seconde, et seulement quand la sous-page est à l'écran.
 */
final class MultiplierPane extends VBox {

    static final double WIDTH = 640;
    private static final long EVERY_NANOS = 500_000_000L;

    private final Game game;
    private Multipliers.Quantity chosen = Multipliers.Quantity.PRODUCTION;
    private final Set<Multipliers.Aspect> open = EnumSet.noneOf(Multipliers.Aspect.class);
    private final Map<Multipliers.Quantity, Button> buttons = new EnumMap<>(Multipliers.Quantity.class);
    private final FlowPane choices = new FlowPane(8, 8);
    private final Label total = new Label();
    private final Label note = new Label();
    private final HBox bar = new HBox();
    private final VBox lines = new VBox(4);
    private long writtenAt = 0;
    /** Le texte des lignes au dernier dessin : pour les vérifications. */
    private final List<String> shown = new ArrayList<>();

    MultiplierPane(Game game) {
        super(8);
        this.game = game;
        setMaxWidth(WIDTH);
        choices.setAlignment(Pos.CENTER_LEFT);
        for (Multipliers.Quantity quantity : Multipliers.Quantity.values()) {
            Button button = new Button(quantity.label());
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                chosen = quantity;
                writtenAt = 0;
                refresh();
            });
            buttons.put(quantity, button);
            choices.getChildren().add(button);
        }
        total.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e8f4ff;");
        total.setWrapText(true);
        note.setStyle(StatsPageBase.NAME_STYLE);
        note.setWrapText(true);
        note.setMaxWidth(WIDTH);
        bar.setPrefHeight(18);
        bar.setMinHeight(18);
        bar.setMaxWidth(WIDTH);
        bar.setStyle("-fx-background-color: #121821; -fx-background-radius: 4;");
        getChildren().addAll(choices, total, bar, note, lines);
    }

    /** Recompose le détail, deux fois par seconde au plus. */
    void refresh() {
        long now = System.nanoTime();
        if (writtenAt != 0 && now - writtenAt < EVERY_NANOS) return;
        writtenAt = now;
        if (!Multipliers.isAvailable(game, chosen)) chosen = Multipliers.Quantity.PRODUCTION;
        for (Map.Entry<Multipliers.Quantity, Button> entry : buttons.entrySet()) {
            boolean available = Multipliers.isAvailable(game, entry.getKey());
            entry.getValue().setVisible(available);
            entry.getValue().setManaged(available);
            entry.getValue().setStyle(pill(entry.getKey() == chosen));
        }
        draw(Multipliers.byAspect(Multipliers.of(game, chosen)), Multipliers.total(game, chosen));
    }

    private void draw(List<Multipliers.Share> shares, double log10) {
        shown.clear();
        total.setText(chosen.label() + " : " + Format.amount(BigNum.pow10(log10)));
        double gained = 0;
        for (Multipliers.Share share : shares) if (share.log10() > 0) gained += share.log10();
        note.setText(gained <= 0 ? "Rien ne multiplie encore cette grandeur."
                : "Ordres de grandeur gagnés : " + orders(gained)
                + ". La barre les partage entre les aspects du jeu ; un clic sur une ligne montre ses facteurs.");

        bar.getChildren().clear();
        for (Multipliers.Share share : shares) {
            double part = Multipliers.shareOf(share, shares);
            if (part <= 0) continue;
            Region piece = new Region();
            double width = Math.max(1, part * WIDTH);
            piece.setPrefWidth(width);
            piece.setMinWidth(width);
            piece.setPrefHeight(18);
            piece.setStyle("-fx-background-color: " + share.aspect().color() + ";");
            bar.getChildren().add(piece);
        }

        lines.getChildren().clear();
        for (Multipliers.Share share : shares) {
            double part = Multipliers.shareOf(share, shares);
            boolean brake = share.log10() < 0;
            String value = brake ? "÷" + Format.amount(BigNum.pow10(-share.log10())) : Format.multiplier(BigNum.pow10(share.log10()));
            String percent = brake ? "frein" : part > 0 && part < 0.005 ? "< 1 %" : Math.round(part * 100) + " %";
            shown.add(share.aspect().label() + " | " + value + " | " + percent);
            lines.getChildren().add(line(share, value, percent, part, brake));
            if (!open.contains(share.aspect())) continue;
            for (Multipliers.Factor factor : share.factors()) {
                String each = factor.log10() < 0 ? "÷" + Format.amount(BigNum.pow10(-factor.log10()))
                        : Format.multiplier(BigNum.pow10(factor.log10()));
                lines.getChildren().add(detail(factor.label(), each));
            }
        }
    }

    private HBox line(Multipliers.Share share, String value, String percent, double part, boolean brake) {
        Region swatch = new Region();
        swatch.setMinSize(12, 12);
        swatch.setMaxSize(12, 12);
        swatch.setStyle("-fx-background-color: " + share.aspect().color() + "; -fx-background-radius: 3;");
        Label name = new Label((open.contains(share.aspect()) ? "▾ " : "▸ ") + share.aspect().label());
        name.setStyle(StatsPageBase.NAME_STYLE);
        Region gauge = new Region();
        gauge.setMinHeight(6);
        gauge.setMaxHeight(6);
        double width = Math.max(2, part * 160);
        gauge.setPrefWidth(width);
        gauge.setMaxWidth(width);
        gauge.setStyle("-fx-background-color: " + share.aspect().color() + "; -fx-background-radius: 3;");
        HBox gaugeBox = new HBox(gauge);
        gaugeBox.setAlignment(Pos.CENTER_LEFT);
        gaugeBox.setMinWidth(164);
        gaugeBox.setMaxWidth(164);
        Region spring = new Region();
        HBox.setHgrow(spring, Priority.ALWAYS);
        Label amount = new Label(value);
        amount.setStyle(StatsPageBase.VALUE_STYLE + (brake ? " -fx-text-fill: #ff8a8a;" : ""));
        Label share100 = new Label(percent);
        share100.setStyle(StatsPageBase.NAME_STYLE);
        share100.setMinWidth(48);
        share100.setAlignment(Pos.CENTER_RIGHT);
        HBox row = new HBox(10, swatch, name, spring, amount, gaugeBox, share100);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(WIDTH);
        row.setStyle(StatsPageBase.ROW_STYLE + " -fx-cursor: hand;");
        row.setOnMouseEntered(event -> row.setStyle(StatsPageBase.ROW_HOVER_STYLE));
        row.setOnMouseExited(event -> row.setStyle(StatsPageBase.ROW_STYLE + " -fx-cursor: hand;"));
        row.setOnMouseClicked(event -> {
            if (!open.remove(share.aspect())) open.add(share.aspect());
            writtenAt = 0;
            refresh();
        });
        return row;
    }

    private static HBox detail(String label, String value) {
        Label name = new Label(label);
        name.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        name.setWrapText(true);
        Region spring = new Region();
        HBox.setHgrow(spring, Priority.ALWAYS);
        Label amount = new Label(value);
        amount.setStyle("-fx-font-size: 12px; -fx-text-fill: #b9c7d6;");
        HBox row = new HBox(10, name, spring, amount);
        row.setPadding(new Insets(1, 12, 1, 40));
        row.setMaxWidth(WIDTH);
        return row;
    }

    private static String orders(double log10) {
        return log10 >= 100 ? Format.whole(Math.round(log10)) : String.format(java.util.Locale.FRANCE, "%.1f", log10);
    }

    private static String pill(boolean chosen) {
        return "-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 12; -fx-border-radius: 12;"
                + (chosen ? " -fx-text-fill: #0b0e14; -fx-background-color: " + StatsPageBase.STATS_COLOR + "; -fx-border-color: " + StatsPageBase.STATS_COLOR + ";"
                : " -fx-text-fill: #b9c7d6; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;");
    }

    // Pour les vérifications.
    List<String> shownLines() {
        return List.copyOf(shown);
    }

    void choose(Multipliers.Quantity quantity) {
        chosen = quantity;
        writtenAt = 0;
        refresh();
    }

    void toggle(Multipliers.Aspect aspect) {
        if (!open.remove(aspect)) open.add(aspect);
        writtenAt = 0;
        refresh();
    }

    int lineCount() {
        return lines.getChildren().size();
    }
}

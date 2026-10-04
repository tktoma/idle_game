package idle.ui;

import idle.core.Aspect;
import idle.core.BigNum;
import idle.core.Element;
import idle.core.ElementCategory;
import idle.core.Game;
import idle.core.PeriodicTable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Sous-page « Tableau périodique » de l'onglet Atomes.
 *
 * <p>Le bouton de synthèse dépense des atomes pour créer un élément tiré au sort ; son prix
 * double à chaque synthèse, jusqu'au maximum d'atomes. Le tableau montre les 118 éléments à leur place : ceux
 * qui sont possédés sont allumés dans la couleur de leur famille, avec leur nombre
 * d'exemplaires. Sous le tableau, les bonus cumulés et la légende des familles.
 */
final class PeriodicTablePage extends VBox {

    private static final double CELL_SIZE = 36;
    private static final String CELL_STYLE = "-fx-font-size: 11px; -fx-font-weight: bold;"
            + " -fx-background-radius: 4; -fx-border-radius: 4;";

    /** Une couleur par famille, comme sur les tableaux périodiques imprimés. */
    private static final Map<ElementCategory, String> COLORS = new EnumMap<>(ElementCategory.class);

    static {
        COLORS.put(ElementCategory.TRANSITION_METAL, "#f4a6a6");
        COLORS.put(ElementCategory.POST_TRANSITION_METAL, "#9fb4c7");
        COLORS.put(ElementCategory.NONMETAL, "#9be7a8");
        COLORS.put(ElementCategory.ALKALI_METAL, "#ffb86b");
        COLORS.put(ElementCategory.ALKALINE_EARTH_METAL, "#ffe08a");
        COLORS.put(ElementCategory.METALLOID, "#7fd6c2");
        COLORS.put(ElementCategory.HALOGEN, "#8fd0ff");
        COLORS.put(ElementCategory.LANTHANIDE, "#d8a6f4");
        COLORS.put(ElementCategory.NOBLE_GAS, "#c0a6ff");
        COLORS.put(ElementCategory.ACTINIDE, "#ff8fb8");
    }

    private final Game game;
    private final Button synthesizeButton = new Button();
    private final Label resultLabel = new Label();
    private final Label progressLabel = new Label();
    private final Label bonusLabel = new Label();
    private final Map<Integer, Label> cells = new HashMap<>();
    private final Map<Integer, Tooltip> tooltips = new HashMap<>();
    /** Dernier état affiché du tableau, pour ne le redessiner que lorsqu'il change. */
    private Map<Integer, Integer> shown = null;

    PeriodicTablePage(Game game) {
        super(10);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(12, 0, 12, 0));

        synthesizeButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand; -fx-text-fill: #ffd27f;"
                + " -fx-background-color: #2a2113; -fx-background-radius: 8;"
                + " -fx-border-color: #ffd27f; -fx-border-radius: 8;");
        synthesizeButton.setOnAction(event -> synthesize());
        resultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffe9c2;");
        progressLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        bonusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ffe9c2;");
        bonusLabel.setWrapText(true);
        bonusLabel.setTextAlignment(TextAlignment.CENTER);

        // Le tableau : sept périodes, puis les lanthanides et les actinides sur deux lignes à part.
        GridPane table = new GridPane();
        table.setHgap(2);
        table.setVgap(2);
        table.setAlignment(Pos.CENTER);
        for (Element element : PeriodicTable.ELEMENTS) {
            Label cell = new Label(element.symbol());
            cell.setMinSize(CELL_SIZE, CELL_SIZE);
            cell.setPrefSize(CELL_SIZE, CELL_SIZE);
            cell.setAlignment(Pos.CENTER);
            cell.setTextAlignment(TextAlignment.CENTER);
            Tooltip tooltip = new Tooltip();
            Tooltip.install(cell, tooltip);
            cells.put(element.number(), cell);
            tooltips.put(element.number(), tooltip);
            table.add(cell, column(element.number()), row(element.number()));
        }

        // La légende : chaque famille, sa chance d'être tirée et ce qu'apporte un exemplaire.
        FlowPane legend = new FlowPane(16, 4);
        legend.setAlignment(Pos.CENTER);
        for (ElementCategory category : ElementCategory.values()) {
            Label entry = new Label("■ " + category.label() + " : " + percent(category.chance()) + " de chance, "
                    + category.aspect().label().toLowerCase(Locale.ROOT) + " +" + percent(category.bonusPerCopy()));
            entry.setStyle("-fx-font-size: 12px; -fx-text-fill: " + COLORS.get(category) + ";");
            legend.getChildren().add(entry);
        }

        getChildren().add(synthesizeButton);
        getChildren().add(resultLabel);
        getChildren().add(progressLabel);
        getChildren().add(table);
        getChildren().add(bonusLabel);
        getChildren().add(legend);
    }

    /** Consomme les atomes, tire un élément et le met en avant dans le tableau. */
    private void synthesize() {
        Element element = game.synthesize();
        if (element == null) return;
        int copies = game.elementCount(element.number());
        resultLabel.setText((copies == 1 ? "Nouvel élément : " : "Exemplaire n° " + copies + " : ")
                + element.name() + " (" + element.symbol() + "), " + element.category().label().toLowerCase(Locale.ROOT));
        resultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLORS.get(element.category()) + ";");
        refresh();

        // La case de l'élément obtenu grossit un instant.
        ScaleTransition pop = new ScaleTransition(Duration.seconds(0.5), cells.get(element.number()));
        pop.setFromX(2.2);
        pop.setFromY(2.2);
        pop.setToX(1);
        pop.setToY(1);
        pop.play();
    }

    /** Recopie l'état du jeu dans le bouton, le tableau et le résumé des bonus. */
    void refresh() {
        BigNum cost = game.synthesisCost();
        String price = Format.count(cost) + (cost.gt(BigNum.ONE) ? " atomes" : " atome");
        synthesizeButton.setText(game.canSynthesize()
                ? "Synthétiser un élément (" + price + ")"
                : "Synthèse : il faut " + price + " (" + Format.count(game.state().atoms()) + " actuellement)");
        synthesizeButton.setDisable(!game.canSynthesize());
        progressLabel.setText(game.discoveredElements() + " / " + PeriodicTable.ELEMENTS.size() + " éléments découverts"
                + (cost.lt(Game.MAX_ATOMS)
                        ? "   |   le prix double à chaque synthèse, jusqu'à " + Format.count(Game.MAX_ATOMS) + " atomes"
                        : "   |   prix au maximum"));

        // Le tableau et les bonus ne changent qu'à la synthèse : inutile de tout réécrire à chaque image.
        Map<Integer, Integer> owned = game.state().elements();
        if (owned.equals(shown)) return;
        shown = new HashMap<>(owned);

        for (Element element : PeriodicTable.ELEMENTS) {
            int copies = game.elementCount(element.number());
            String color = COLORS.get(element.category());
            Label cell = cells.get(element.number());
            cell.setText(copies > 1 ? element.symbol() + "\n×" + copies : element.symbol());
            cell.setStyle(CELL_STYLE + (copies > 0
                    ? " -fx-text-fill: #ffffff; -fx-background-color: " + color + "55; -fx-border-color: " + color + ";"
                    : " -fx-text-fill: " + color + "88; -fx-background-color: #10151f; -fx-border-color: " + color + "44;"));
            tooltips.get(element.number()).setText(element.number() + "  " + element.name() + " (" + element.symbol() + ")\n"
                    + element.category().label() + "\n"
                    + element.category().aspect().label() + " : +" + percent(element.category().bonusPerCopy())
                    + " par exemplaire\n"
                    + (copies == 0 ? "Pas encore obtenu" : copies + (copies > 1 ? " exemplaires" : " exemplaire")));
        }

        StringBuilder bonuses = new StringBuilder();
        for (Aspect aspect : Aspect.values()) {
            double power = game.elementPower(aspect);
            if (aspect == Aspect.ALL || power == 1) continue;
            bonuses.append("   ").append(aspect.label()).append(' ').append(aspect.isReduction()
                    ? "−" + percent(1 - 1 / power)
                    : Format.multiplier(BigNum.of(power)));
        }
        bonusLabel.setText(bonuses.isEmpty()
                ? "Bonus des éléments : aucun pour l'instant"
                : "Bonus des éléments :" + bonuses);
    }

    /** Ligne d'un élément dans la grille : périodes 0 à 6, lanthanides en 8, actinides en 9. */
    static int row(int number) {
        if (number >= 57 && number <= 71) return 8;
        if (number >= 89 && number <= 103) return 9;
        if (number <= 2) return 0;
        if (number <= 10) return 1;
        if (number <= 18) return 2;
        if (number <= 36) return 3;
        if (number <= 54) return 4;
        if (number <= 86) return 5;
        return 6;
    }

    /** Colonne d'un élément dans la grille, de 0 à 17. */
    static int column(int number) {
        if (number == 1) return 0;
        if (number == 2) return 17;
        if (number <= 4) return number - 3;
        if (number <= 10) return number - 5 + 12;
        if (number <= 12) return number - 11;
        if (number <= 18) return number - 13 + 12;
        if (number <= 36) return number - 19;
        if (number <= 54) return number - 37;
        if (number <= 56) return number - 55;
        if (number <= 71) return number - 57 + 2;     // lanthanides, sous la troisième colonne
        if (number <= 86) return number - 72 + 3;
        if (number <= 88) return number - 87;
        if (number <= 103) return number - 89 + 2;    // actinides
        return number - 104 + 3;
    }

    /** 0.3 → « 30 % », 0.025 → « 2.5 % ». */
    private static String percent(double fraction) {
        String text = String.format(Locale.ROOT, "%.1f", fraction * 100);
        return (text.endsWith(".0") ? text.substring(0, text.length() - 2) : text) + " %";
    }
}

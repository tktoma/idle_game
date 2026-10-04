package idle.ui;

import idle.core.Automation;
import idle.core.BigNum;
import idle.core.Element;
import idle.core.ElementCategory;
import idle.core.ElementEffect;
import idle.core.Game;
import idle.core.PeriodicTable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Sous-page « Tableau périodique » de l'onglet Atomes.
 *
 * <p>Le bouton de synthèse dépense des atomes pour créer un élément tiré au sort ; son prix
 * double à chaque synthèse, jusqu'au maximum d'atomes. Le tableau montre les 118 éléments à
 * leur place : ceux qui sont possédés sont allumés dans la couleur de leur famille, avec leur
 * nombre d'exemplaires sur le maximum possible ; un élément au maximum est en couleur pleine,
 * et les éléments uniques portent une étoile.
 *
 * <p>Chaque élément a son propre effet : on le lit en survolant sa case, ou en cliquant dessus
 * pour l'afficher sous le tableau. Plus bas, le cumul des bonus et la légende des familles avec
 * leur chance actuelle d'être tirées.
 *
 * <p>La page suit l'état du jeu, pas les clics : un élément obtenu par la synthèse automatique
 * s'affiche exactement comme un élément obtenu à la main.
 */
final class PeriodicTablePage extends VBox {

    /** Le tableau fait 18 cases de large, séparées par un petit espace. */
    private static final int COLUMNS = 18;
    private static final double CELL_GAP = 2;
    /** Côté d'une case : il suit la largeur de la fenêtre, entre ces deux bornes. */
    private static final double MIN_CELL_SIZE = 20;
    private static final double MAX_CELL_SIZE = 64;
    private static final String CELL_STYLE = "-fx-font-weight: bold; -fx-cursor: hand;"
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
    private final Label automationLabel = new Label();
    private final Label detailLabel = new Label();
    private final Label bonusLabel = new Label();
    private final Map<Integer, Label> cells = new HashMap<>();
    private final Map<Integer, Tooltip> tooltips = new HashMap<>();
    private final Map<ElementCategory, Label> legend = new EnumMap<>(ElementCategory.class);
    /** Dernier état affiché du tableau, pour ne le redessiner que lorsqu'il change ; {@code null} avant le premier affichage. */
    private Map<Integer, Integer> shown = null;
    /** Maximum total d'exemplaires au dernier affichage. */
    private int shownMaxTotal = -1;
    /** Élément dont le détail est affiché sous le tableau, ou {@code null}. */
    private Element selected = null;
    /** Côté actuel d'une case, en pixels. */
    private double cellSize = 36;

    PeriodicTablePage(Game game) {
        super(10);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(12, 0, 12, 0));

        synthesizeButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 20; -fx-cursor: hand; -fx-text-fill: #ffd27f;"
                + " -fx-background-color: #2a2113; -fx-background-radius: 8;"
                + " -fx-border-color: #ffd27f; -fx-border-radius: 8;");
        synthesizeButton.setOnAction(event -> {
            game.synthesize();
            refresh();     // c'est l'affichage qui repère l'élément obtenu, comme pour une synthèse automatique
        });
        resultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffe9c2;");
        resultLabel.setWrapText(true);
        resultLabel.setTextAlignment(TextAlignment.CENTER);
        progressLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        progressLabel.setWrapText(true);
        progressLabel.setTextAlignment(TextAlignment.CENTER);
        automationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        automationLabel.setWrapText(true);
        automationLabel.setTextAlignment(TextAlignment.CENTER);
        detailLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #e8f4ff;");
        detailLabel.setWrapText(true);
        detailLabel.setTextAlignment(TextAlignment.CENTER);
        bonusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ffe9c2;");
        bonusLabel.setWrapText(true);
        bonusLabel.setTextAlignment(TextAlignment.CENTER);

        // Le tableau : sept périodes, puis les lanthanides et les actinides sur deux lignes à part.
        GridPane table = new GridPane();
        table.setHgap(CELL_GAP);
        table.setVgap(CELL_GAP);
        table.setAlignment(Pos.CENTER);
        for (Element element : PeriodicTable.ELEMENTS) {
            Label cell = new Label(element.symbol());
            cell.setMinSize(cellSize, cellSize);
            cell.setPrefSize(cellSize, cellSize);
            cell.setAlignment(Pos.CENTER);
            cell.setTextAlignment(TextAlignment.CENTER);
            cell.setOnMouseClicked(event -> {
                selected = element;
                showDetail();
            });
            Tooltip tooltip = new Tooltip();
            Tooltip.install(cell, tooltip);
            cells.put(element.number(), cell);
            tooltips.put(element.number(), tooltip);
            table.add(cell, column(element.number()), row(element.number()));
        }

        // La légende : une ligne par famille, avec sa chance actuelle et ce que font ses éléments.
        VBox legendBox = new VBox(3);
        legendBox.setAlignment(Pos.TOP_CENTER);
        for (ElementCategory category : ElementCategory.values()) {
            Label entry = new Label();
            entry.setStyle("-fx-font-size: 12px; -fx-text-fill: " + COLORS.get(category) + ";");
            entry.setWrapText(true);
            entry.setTextAlignment(TextAlignment.CENTER);
            legend.put(category, entry);
            legendBox.getChildren().add(entry);
        }
        Label legendHint = new Label("Un nouvel élément rapporte toujours plus qu'un doublon : quatre exemplaires "
                + "ne comptent que pour deux, neuf pour trois. Un élément arrivé à son maximum d'exemplaires ne sort "
                + "plus : les tirages vont aux autres. Les éléments marqués ★ sont uniques : "
                + "ils ne sortent qu'une fois, et le premier obtenu débloque la synthèse automatique.");
        legendHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        legendHint.setWrapText(true);
        legendHint.setTextAlignment(TextAlignment.CENTER);

        // Les cases grandissent et rétrécissent avec la fenêtre.
        widthProperty().addListener((observable, before, after) -> fitCells(after.doubleValue()));

        getChildren().add(synthesizeButton);
        getChildren().add(resultLabel);
        getChildren().add(progressLabel);
        getChildren().add(automationLabel);
        getChildren().add(table);
        getChildren().add(detailLabel);
        getChildren().add(bonusLabel);
        getChildren().add(legendBox);
        getChildren().add(legendHint);
    }

    /** Recopie l'état du jeu dans le bouton, le tableau, le résumé des bonus et la légende. */
    void refresh() {
        BigNum cost = game.synthesisCost();
        String price = Format.count(cost) + (cost.gt(BigNum.ONE) ? " atomes" : " atome");
        synthesizeButton.setText(game.isPeriodicTableComplete()
                ? "Tableau périodique complet : tous les éléments sont au maximum"
                : game.canSynthesize()
                ? "Synthétiser un élément (" + price + ")"
                : "Synthèse : il faut " + price + " (" + Format.count(game.state().atoms()) + " actuellement)");
        synthesizeButton.setDisable(!game.canSynthesize());

        int doubleDraw = (int) Math.round(game.doubleDrawChance() * 100);
        progressLabel.setText(game.discoveredElements() + " / " + PeriodicTable.ELEMENTS.size() + " éléments découverts"
                + "   |   " + game.ownedCopies() + " / " + game.maxTotalCopies() + " exemplaires"
                + "   |   " + game.state().synthesisCount() + (game.state().synthesisCount() > 1 ? " synthèses" : " synthèse")
                + (doubleDraw > 0 ? "   |   tirage double : " + doubleDraw + " %" : ""));
        automationLabel.setText(automationStatus());

        // Le tableau et les bonus ne changent qu'à la synthèse : inutile de tout réécrire à chaque image.
        // L'arbre de matière noire peut aussi repousser le maximum d'exemplaires : il faut alors repeindre.
        Map<Integer, Integer> owned = game.state().elements();
        int maxTotal = game.maxTotalCopies();
        if (owned.equals(shown) && maxTotal == shownMaxTotal) return;
        shownMaxTotal = maxTotal;
        if (shown != null) announce(owned);
        if (owned.isEmpty()) resultLabel.setText("");     // tableau vidé par une explosion
        shown = new HashMap<>(owned);

        paintCells();
        showDetail();
        bonusLabel.setText(bonusSummary());
        for (ElementCategory category : ElementCategory.values()) {
            double chance = game.categoryChance(category);
            legend.get(category).setText("■ " + category.label() + " ("
                    + (chance > 0 ? percent(chance) : "complet")
                    + (category.unique() ? "" : ", " + game.maxCopiesOf(category) + " exemplaires au plus")
                    + ") : " + category.description());
        }
    }

    /**
     * Donne aux cases la plus grande taille qui fait tenir les 18 colonnes dans la largeur
     * disponible, entre {@link #MIN_CELL_SIZE} et {@link #MAX_CELL_SIZE}.
     */
    private void fitCells(double width) {
        double room = width - 34 - (COLUMNS - 1) * CELL_GAP;   // un peu de marge de chaque côté
        double size = Math.max(MIN_CELL_SIZE, Math.min(MAX_CELL_SIZE, Math.floor(room / COLUMNS)));
        // On rétrécit dès qu'il le faut, mais on ne grandit que franchement : sinon l'apparition de la
        // barre de défilement, qui rogne un peu la largeur, ferait hésiter les cases entre deux tailles.
        if (size == cellSize || (size > cellSize && size < cellSize + 2)) return;
        cellSize = size;
        for (Label cell : cells.values()) {
            cell.setMinSize(size, size);
            cell.setPrefSize(size, size);
        }
        paintCells();    // la taille du texte suit celle de la case
    }

    /** Écrit et colore chaque case d'après ce que le joueur possède. */
    private void paintCells() {
        String font = "-fx-font-size: " + Math.max(8, Math.round(cellSize * 0.31)) + "px; ";
        for (Element element : PeriodicTable.ELEMENTS) {
            int copies = game.elementCount(element.number());
            int max = game.maxCopiesOf(element.category());
            boolean unique = element.category().unique();
            boolean maxed = game.isElementMaxed(element.number());
            String color = COLORS.get(element.category());
            Label cell = cells.get(element.number());
            cell.setText(unique ? element.symbol() + "\n★"
                    : copies > 0 ? element.symbol() + "\n" + Math.min(copies, max) + "/" + max : element.symbol());
            // Pas encore obtenu : éteint. Possédé : allumé. Au maximum : couleur pleine.
            cell.setStyle(font + CELL_STYLE + (maxed
                    ? " -fx-text-fill: #10151f; -fx-background-color: " + color + "; -fx-border-color: #ffffff;"
                    : copies > 0
                    ? " -fx-text-fill: #ffffff; -fx-background-color: " + color + "55; -fx-border-color: " + color + ";"
                    : " -fx-text-fill: " + color + "88; -fx-background-color: #10151f; -fx-border-color: " + color + "44;"));
            tooltips.get(element.number()).setText(detail(element, "\n"));
        }
    }

    /** Affiche les éléments gagnés depuis le dernier affichage et fait grossir leur case un instant. */
    private void announce(Map<Integer, Integer> owned) {
        List<Element> gained = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : owned.entrySet()) {
            if (entry.getValue() > shown.getOrDefault(entry.getKey(), 0)) {
                gained.add(PeriodicTable.element(entry.getKey()));
            }
        }
        if (gained.isEmpty()) return;
        // Plus de deux d'un coup : plusieurs synthèses automatiques entre deux images, on résume.
        if (gained.size() > 2) {
            resultLabel.setText(gained.size() + " éléments obtenus");
            resultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffe9c2;");
            return;
        }
        StringBuilder text = new StringBuilder(gained.size() == 2 ? "Tirage double ! " : "");
        for (Element element : gained) {
            int copies = owned.get(element.number());
            if (text.length() > 0 && element != gained.get(0)) text.append("\n");
            text.append(copies == 1 ? "Nouvel élément : " : "Exemplaire n° " + copies + " : ")
                    .append(element.name()).append(" (").append(element.symbol()).append(") — ")
                    .append(ElementText.describe(element.effect()));

            ScaleTransition pop = new ScaleTransition(Duration.seconds(0.5), cells.get(element.number()));
            pop.setFromX(2.2);
            pop.setFromY(2.2);
            pop.setToX(1);
            pop.setToY(1);
            pop.play();
        }
        resultLabel.setText(text.toString());
        resultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLORS.get(gained.get(0).category()) + ";");
    }

    /** Où en est la synthèse automatique : verrouillée, à acheter, en marche ou coupée. */
    private String automationStatus() {
        Automation synthesis = null;
        for (Automation automation : game.automations()) {
            if (automation.kind() == Automation.Kind.SYNTHESIS) synthesis = automation;
        }
        if (synthesis == null) return "";
        if (game.isPeriodicTableComplete()) {
            return "Tous les éléments sont au maximum : le tableau peut exploser, avec le bouton tout en haut de la fenêtre.";
        }
        if (game.canExplode()) {
            return "Les 118 éléments sont découverts : le tableau peut exploser, avec le bouton tout en haut de la fenêtre.";
        }
        if (!game.isSynthesisAutomationUnlocked()) {
            int left = game.guaranteedUniqueSynthesis() - game.state().synthesisCount();
            return "Synthèse automatique : se débloque avec le premier élément unique ★ (gaz noble ou actinide)"
                    + (left > 1 ? ", garanti au plus tard dans " + left + " synthèses"
                            : ", garanti à la prochaine synthèse");
        }
        if (!game.ownsAutomation(synthesis.id())) {
            return "Synthèse automatique débloquée : à acheter dans l'onglet Automatisation ("
                    + Format.count(synthesis.cost()) + " atomes)";
        }
        return game.isAutomationEnabled(synthesis.id())
                ? "Synthèse automatique en marche : une toutes les "
                        + ElementText.number(game.automationInterval(synthesis.id())) + " s, dès qu'il y a assez d'atomes"
                : "Synthèse automatique coupée (onglet Automatisation)";
    }

    /** Écrit le détail de l'élément sélectionné sous le tableau. */
    private void showDetail() {
        if (selected == null) {
            detailLabel.setText("Survolez un élément, ou cliquez dessus, pour voir son effet.");
            detailLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
            return;
        }
        detailLabel.setText(detail(selected, "   |   "));
        detailLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLORS.get(selected.category()) + ";");
    }

    /** Tout ce qu'il y a à savoir sur un élément : nom, famille, effet, exemplaires. */
    private String detail(Element element, String separator) {
        int copies = game.elementCount(element.number());
        String stacking = ElementText.stacking(element.effect(), copies);
        int max = game.maxCopiesOf(element.category());
        String owned = element.category().unique() ? (copies == 0 ? "Pas encore obtenu" : "Obtenu")
                : (copies == 0 ? "Pas encore obtenu, " + max + " exemplaires au plus"
                        : Math.min(copies, max) + " / " + max + (max > 1 ? " exemplaires" : " exemplaire")
                                + (copies >= max ? " (maximum)" : ""))
                        + (stacking.isEmpty() ? "" : " : " + stacking);
        return element.number() + "  " + element.name() + " (" + element.symbol() + ")" + separator
                + element.category().label() + (element.category().unique() ? " ★ unique" : "") + separator
                + ElementText.describe(element.effect()) + separator
                + owned;
    }

    /** Le cumul de ce que rapportent les éléments possédés. */
    private String bonusSummary() {
        List<String> parts = new ArrayList<>();
        addMultiplier(parts, "Particules", game.elementMultiplier(ElementEffect.Stat.PARTICLES));
        addMultiplier(parts, "Vitesse", game.elementMultiplier(ElementEffect.Stat.SPEED));
        addMultiplier(parts, "Atomes par fusion", game.elementMultiplier(ElementEffect.Stat.ATOMS));
        for (ElementEffect.CostTarget target : ElementEffect.CostTarget.values()) {
            double divisor = game.elementCostDivisor(target);
            if (divisor != 1) parts.add("Prix " + ElementText.of(target) + " ÷" + ElementText.number(divisor));
        }
        for (int generator = 0; generator < game.generatorsPerAtom(); generator++) {
            double production = game.generatorParticlesMultiplier(generator) * game.generatorSpeedMultiplier(generator);
            if (production != 1) {
                parts.add("Générateur " + (generator + 1) + " " + Format.multiplier(BigNum.of(production)));
            }
        }
        if (parts.isEmpty()) return "Bonus des éléments : aucun pour l'instant";
        return "Bonus des éléments :   " + String.join("   |   ", parts);
    }

    private static void addMultiplier(List<String> parts, String name, double value) {
        if (value != 1) parts.add(name + " " + Format.multiplier(BigNum.of(value)));
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

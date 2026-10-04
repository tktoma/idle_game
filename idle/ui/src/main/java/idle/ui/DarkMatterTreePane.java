package idle.ui;

import idle.core.BigNum;
import idle.core.DarkEffect;
import idle.core.DarkUpgrade;
import idle.core.Game;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;

/**
 * L'arbre d'améliorations de la matière noire : une colonne par branche (particules, atomes,
 * taille), et dans chaque colonne les cases de haut en bas, reliées par un trait à la case dont
 * elles dépendent. Une case est un bouton : un clic achète un niveau.
 *
 * <p>L'arbre est construit d'après le catalogue du jeu ({@link Game#darkUpgrades()}) : ajouter
 * une case dans core suffit à la faire apparaître ici. La zone place elle-même ses cases et ses
 * traits : ils suivent la largeur de la fenêtre.
 */
final class DarkMatterTreePane extends Pane {

    /** Une couleur par branche : celle de sa ressource dans le reste du jeu. */
    private static final Map<DarkUpgrade.Branch, String> COLORS = new EnumMap<>(DarkUpgrade.Branch.class);

    static {
        COLORS.put(DarkUpgrade.Branch.PARTICLES, "#9fd0ff");
        COLORS.put(DarkUpgrade.Branch.ATOMS, "#ffd27f");
        COLORS.put(DarkUpgrade.Branch.SIZE, GameApp.DARK_MATTER_COLOR);
    }

    private static final double ROW_GAP = 28;
    private static final double TOP = 8;
    private static final double ROOT_HEIGHT = 40;
    private static final String CARD_STYLE = "-fx-font-size: 12px; -fx-padding: 6 8; -fx-background-radius: 8;"
            + " -fx-border-radius: 8;";

    private final Game game;
    private final Label root = new Label("Matière noire");
    private final List<DarkUpgrade> upgrades = new ArrayList<>();
    private final List<Button> cards = new ArrayList<>();
    /** Pour chaque case : sa ligne dans sa colonne (0 en haut) et la case dont elle dépend (−1 : la racine). */
    private final List<Integer> rows = new ArrayList<>();
    private final List<Integer> parents = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private int rowCount = 0;
    private double cardHeight = 110;

    DarkMatterTreePane(Game game) {
        this.game = game;
        setMinSize(0, 0);

        Map<String, Integer> indexOf = new HashMap<>();
        Map<DarkUpgrade.Branch, Integer> filled = new EnumMap<>(DarkUpgrade.Branch.class);
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            int row = filled.merge(upgrade.branch(), 1, Integer::sum) - 1;
            indexOf.put(upgrade.id(), upgrades.size());
            upgrades.add(upgrade);
            rows.add(row);
            parents.add(upgrade.requires() == null ? -1 : indexOf.get(upgrade.requires()));
            rowCount = Math.max(rowCount, row + 1);
        }
        // Les traits d'abord : ils passent sous les cases.
        for (int i = 0; i < upgrades.size(); i++) {
            Line line = new Line();
            line.setStrokeWidth(2);
            lines.add(line);
            getChildren().add(line);
        }
        root.setAlignment(Pos.CENTER);
        root.setStyle(CARD_STYLE + " -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a0f2e;"
                + " -fx-background-color: " + GameApp.DARK_MATTER_COLOR + ";");
        getChildren().add(root);
        for (DarkUpgrade upgrade : upgrades) {
            Button card = new Button();
            card.setWrapText(true);
            card.setTextAlignment(TextAlignment.CENTER);
            card.setOnAction(event -> {
                game.buyDark(upgrade.id());
                refresh();
            });
            cards.add(card);
            getChildren().add(card);
        }
        setPrefSize(600, height());
    }

    /** Hauteur totale de l'arbre avec la hauteur de case actuelle. */
    private double height() {
        return TOP + ROOT_HEIGHT + rowCount * (cardHeight + ROW_GAP) + 8;
    }

    /** Recopie l'état du jeu dans les cases : texte, couleur, et bouton actif seulement si l'achat est possible. */
    void refresh() {
        for (int i = 0; i < upgrades.size(); i++) {
            DarkUpgrade upgrade = upgrades.get(i);
            int level = game.darkLevelOf(upgrade.id());
            boolean maxed = game.isDarkMaxed(upgrade.id());
            boolean available = game.isDarkAvailable(upgrade.id());
            boolean affordable = game.canBuyDark(upgrade.id());
            String color = COLORS.get(upgrade.branch());

            Button card = cards.get(i);
            card.setText(title(upgrade, level, maxed) + "\n" + DarkText.describe(upgrade.effect(), game)
                    + (maxed ? "" : "\n" + price(upgrade, available)));
            card.setDisable(!affordable);
            // Acquise : couleur pleine. Achetable : bordure vive. Trop chère ou verrouillée : éteinte.
            card.setStyle(CARD_STYLE + (maxed
                    ? " -fx-text-fill: #10151f; -fx-background-color: " + color + "; -fx-border-color: #ffffff; -fx-opacity: 1;"
                    : affordable
                    ? " -fx-cursor: hand; -fx-text-fill: #ffffff; -fx-background-color: " + color + "44; -fx-border-color: " + color + ";"
                    : level > 0
                    ? " -fx-text-fill: #ffffff; -fx-background-color: " + color + "33; -fx-border-color: " + color + "88; -fx-opacity: 1;"
                    : " -fx-text-fill: " + color + "; -fx-background-color: #10151f; -fx-border-color: " + color + "44;"));

            // Le trait vers la case du dessus s'allume quand cette case est accessible.
            lines.get(i).setStroke(Color.web(available ? color : "#3a3550"));
        }
    }

    private static String title(DarkUpgrade upgrade, int level, boolean maxed) {
        if (upgrade.maxLevel() == 1) return upgrade.name() + (maxed ? " (acquise)" : "");
        return upgrade.name() + " (niveau " + level + (upgrade.hasLimit() ? "/" + upgrade.maxLevel() : "") + ")";
    }

    /** Le prix du prochain niveau, avec ce que le joueur possède en face. */
    private String price(DarkUpgrade upgrade, boolean available) {
        if (!game.hasDarkMatterFor(upgrade.id())) {
            return "Demande " + upgrade.darkMatter() + " matières noires (vous : "
                    + Format.count(game.state().darkMatter()) + ")";
        }
        if (!available) return "Demande la case du dessus";
        BigNum cost = game.darkCostOf(upgrade.id());
        BigNum owned = game.darkBalance(upgrade.branch());
        return switch (upgrade.branch()) {
            case PARTICLES -> "Coût : " + Format.count(cost) + " particules (vous : " + Format.count(owned) + ")";
            case ATOMS -> "Coût : " + Format.count(cost) + " atomes (vous : " + Format.count(owned) + ")";
            case SIZE -> "Taille à atteindre : " + Format.length(cost) + " (actuelle : " + Format.length(owned) + ")";
        };
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        if (width <= 0) return;
        int columns = DarkUpgrade.Branch.values().length;
        double columnWidth = width / columns;
        double cardWidth = Math.max(110, Math.min(250, columnWidth - 16));
        // Case étroite : le texte passe à la ligne, il lui faut plus de hauteur.
        double wanted = cardWidth >= 220 ? 110 : cardWidth >= 170 ? 140 : 190;
        if (wanted != cardHeight) {
            cardHeight = wanted;
            setPrefSize(600, height());
        }
        double rootWidth = Math.min(220, width - 16);
        root.resizeRelocate(Math.floor((width - rootWidth) / 2), TOP, rootWidth, ROOT_HEIGHT);

        double firstRow = TOP + ROOT_HEIGHT + ROW_GAP;
        for (int i = 0; i < upgrades.size(); i++) {
            double centerX = (upgrades.get(i).branch().ordinal() + 0.5) * columnWidth;
            double top = firstRow + rows.get(i) * (cardHeight + ROW_GAP);
            cards.get(i).resizeRelocate(Math.floor(centerX - cardWidth / 2), top, cardWidth, cardHeight);

            // Le trait part du bas de la case du dessus (ou de la racine) et arrive en haut de celle-ci.
            Line line = lines.get(i);
            if (parents.get(i) < 0) {
                line.setStartX(width / 2);
                line.setStartY(TOP + ROOT_HEIGHT);
            } else {
                line.setStartX(centerX);
                line.setStartY(top - ROW_GAP);
            }
            line.setEndX(centerX);
            line.setEndY(top);
        }
    }
}

package idle.ui;

import idle.core.BigNum;
import idle.core.DarkUpgrade;
import idle.core.Game;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.TextAlignment;

/**
 * L'arbre d'améliorations de la matière noire, toutes branches réunies.
 *
 * <p>En haut, la racine : la matière noire. Elle se divise en quatre branches, une colonne
 * chacune, coiffée d'un en-tête qui dit avec quoi la branche se paie et ce que le joueur en a :
 * <ul>
 *   <li><b>particules</b> et <b>atomes</b> : les cases s'enchaînent, chacune demande celle du dessus ;</li>
 *   <li><b>taille</b> : les cases s'enchaînent aussi, mais se débloquent en atteignant une taille,
 *       sans rien dépenser ;</li>
 *   <li><b>matière noire</b> : les améliorations payées en matière noire. Elles ne dépendent pas
 *       les unes des autres : au lieu de s'enchaîner, elles partent toutes d'un même tronc.</li>
 * </ul>
 * Une case est un bouton : un clic achète un niveau. Un trait s'allume quand la case qu'il mène
 * est accessible.
 *
 * <p>L'arbre est construit d'après le catalogue du jeu ({@link Game#darkUpgrades()}) : ajouter
 * une case dans core suffit à la faire apparaître ici. La zone place elle-même ses cases et ses
 * traits : ils suivent la largeur de la fenêtre. Dans une fenêtre étroite, les branches ne
 * tiennent plus côte à côte : elles se rangent en deux étages de deux, reliés par le tronc.
 */
final class DarkMatterTreePane extends Pane {

    /** Une couleur par branche. */
    private static final Map<DarkUpgrade.Branch, String> COLORS = new EnumMap<>(DarkUpgrade.Branch.class);

    static {
        COLORS.put(DarkUpgrade.Branch.PARTICLES, "#9fd0ff");
        COLORS.put(DarkUpgrade.Branch.ATOMS, "#ffd27f");
        COLORS.put(DarkUpgrade.Branch.SIZE, "#7fe0d4");
        COLORS.put(DarkUpgrade.Branch.DARK_MATTER, GameApp.DARK_MATTER_COLOR);
    }

    private static final String OFF = "#3a3550";
    private static final double TOP = 8;
    private static final double ROOT_HEIGHT = 40;
    /** De la racine au trait horizontal qui distribue les branches, puis de ce trait aux en-têtes. */
    private static final double BUS_GAP = 16;
    private static final double HEADER_HEIGHT = 48;
    private static final double ROW_GAP = 20;
    /** En dessous de cette largeur, les branches se rangent en deux étages au lieu d'une seule rangée. */
    private static final double TWO_FLOORS_BELOW = 700;
    /** Retrait des cases d'une branche en éventail, pour laisser passer son tronc à leur gauche. */
    private static final double FAN_INDENT = 18;
    private static final String CARD_STYLE = "-fx-font-size: 12px; -fx-padding: 6 8; -fx-background-radius: 8;"
            + " -fx-border-radius: 8;";

    /** Une case de l'arbre : l'amélioration, son bouton, sa place et les traits qui y mènent. */
    private static final class Cell {
        final DarkUpgrade upgrade;
        final Button card = new Button();
        /** Ligne dans sa colonne, 0 en haut. */
        final int row;
        /** La case du dessus dont elle dépend, ou {@code null} si elle part de l'en-tête de sa branche. */
        Cell parent;
        /** Trait vertical : depuis la case du dessus, ou le long du tronc pour une branche en éventail. */
        final Line link = new Line();
        /** Petit trait horizontal du tronc à la case, pour une branche en éventail seulement. */
        final Line elbow = new Line();

        Cell(DarkUpgrade upgrade, int row) {
            this.upgrade = upgrade;
            this.row = row;
        }
    }

    private final Game game;
    private final Label root = new Label();
    /** Le tronc : il descend de la racine jusqu'au dernier étage de branches. */
    private final Line trunk = new Line();
    /** Un trait horizontal par étage, qui distribue ses branches. */
    private final List<Line> buses = new ArrayList<>();
    private final Map<DarkUpgrade.Branch, Integer> rowCounts = new EnumMap<>(DarkUpgrade.Branch.class);
    private final Map<DarkUpgrade.Branch, Label> headers = new EnumMap<>(DarkUpgrade.Branch.class);
    private final Map<DarkUpgrade.Branch, Line> drops = new EnumMap<>(DarkUpgrade.Branch.class);
    /** Les branches dont les cases ne s'enchaînent pas : elles partent toutes de l'en-tête. */
    private final Map<DarkUpgrade.Branch, Boolean> fans = new EnumMap<>(DarkUpgrade.Branch.class);
    private final List<DarkUpgrade.Branch> branches = new ArrayList<>();
    private final List<Cell> cells = new ArrayList<>();
    private double cardHeight = 136;
    private double totalHeight = 0;

    DarkMatterTreePane(Game game) {
        this.game = game;
        setMinSize(0, 0);

        Map<String, Cell> byId = new HashMap<>();
        Map<DarkUpgrade.Branch, Integer> filled = new EnumMap<>(DarkUpgrade.Branch.class);
        Map<DarkUpgrade.Branch, Integer> independent = new EnumMap<>(DarkUpgrade.Branch.class);
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            DarkUpgrade.Branch branch = upgrade.branch();
            if (!branches.contains(branch)) branches.add(branch);
            Cell cell = new Cell(upgrade, filled.merge(branch, 1, Integer::sum) - 1);
            cell.parent = upgrade.requires() == null ? null : byId.get(upgrade.requires());
            if (cell.parent == null) independent.merge(branch, 1, Integer::sum);
            byId.put(upgrade.id(), cell);
            cells.add(cell);
        }
        rowCounts.putAll(filled);
        // Plusieurs cases sans case du dessus dans une même branche : elles ne s'enchaînent pas.
        for (DarkUpgrade.Branch branch : branches) fans.put(branch, independent.getOrDefault(branch, 0) > 1);

        // Les traits d'abord : ils passent sous les cases.
        buses.add(trunk);
        for (int floor = 0; floor < branches.size(); floor++) buses.add(new Line());
        for (Line line : buses) {
            line.setStrokeWidth(2);
            line.setStroke(Color.web(GameApp.DARK_MATTER_COLOR));
            getChildren().add(line);
        }
        buses.remove(trunk);
        for (DarkUpgrade.Branch branch : branches) {
            Line drop = new Line();
            drop.setStrokeWidth(2);
            drop.setStroke(Color.web(COLORS.get(branch)));
            drops.put(branch, drop);
            getChildren().add(drop);
        }
        for (Cell cell : cells) {
            cell.link.setStrokeWidth(2);
            cell.elbow.setStrokeWidth(2);
            cell.elbow.setVisible(fans.get(cell.upgrade.branch()));
            getChildren().add(cell.link);
            getChildren().add(cell.elbow);
        }

        root.setAlignment(Pos.CENTER);
        root.setStyle(CARD_STYLE + " -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a0f2e;"
                + " -fx-background-color: " + GameApp.DARK_MATTER_COLOR + ";");
        getChildren().add(root);
        for (DarkUpgrade.Branch branch : branches) {
            Label header = new Label();
            header.setAlignment(Pos.CENTER);
            header.setTextAlignment(TextAlignment.CENTER);
            header.setWrapText(true);
            header.setStyle(CARD_STYLE + " -fx-font-weight: bold; -fx-text-fill: #10151f; -fx-background-color: "
                    + COLORS.get(branch) + ";");
            headers.put(branch, header);
            getChildren().add(header);
        }
        for (Cell cell : cells) {
            cell.card.setWrapText(true);
            cell.card.setTextAlignment(TextAlignment.CENTER);
            // Sans cela, un clic donne le focus à la case ; dès qu'elle se grise, le focus saute à la
            // case suivante du catalogue, souvent tout en haut, et la vue défile jusqu'à elle.
            cell.card.setFocusTraversable(false);
            cell.card.setOnAction(event -> {
                game.buyDark(cell.upgrade.id());
                refresh();
            });
            getChildren().add(cell.card);
        }
        setPrefSize(720, 600);
    }

    /**
     * Hauteur d'une case selon sa largeur : plus elle est étroite, plus son texte passe à la
     * ligne. Les valeurs viennent de mesures du plus long texte de l'arbre, avec une marge.
     */
    private static double cardHeightFor(double width) {
        return width >= 213 ? 136 : width >= 183 ? 152 : width >= 153 ? 170 : width >= 138 ? 204
                : width >= 123 ? 222 : width >= 108 ? 270 : width >= 93 ? 304 : 370;
    }

    /** Recopie l'état du jeu dans l'arbre : textes, couleurs, et boutons actifs seulement si l'achat est possible. */
    void refresh() {
        BigNum earned = game.darkMatterEarned();
        root.setText("Matière noire : " + Format.count(earned) + (earned.gt(BigNum.ONE) ? " gagnées" : " gagnée"));
        for (DarkUpgrade.Branch branch : branches) {
            BigNum owned = game.darkBalance(branch);
            headers.get(branch).setText(switch (branch) {
                case PARTICLES -> "Particules\n" + Format.count(owned) + " à dépenser";
                case ATOMS -> "Atomes\n" + Format.count(owned) + " à dépenser";
                case SIZE -> "Taille\n" + Format.length(owned) + " atteinte";
                case DARK_MATTER -> "Matière noire\n" + Format.count(owned) + " à dépenser";
            });
        }
        for (Cell cell : cells) {
            DarkUpgrade upgrade = cell.upgrade;
            int level = game.darkLevelOf(upgrade.id());
            boolean maxed = game.isDarkMaxed(upgrade.id());
            boolean available = game.isDarkAvailable(upgrade.id());
            boolean affordable = game.canBuyDark(upgrade.id());
            String color = COLORS.get(upgrade.branch());

            cell.card.setText(title(upgrade, level, maxed) + "\n" + DarkText.describe(upgrade.effect(), game)
                    + (maxed ? "" : "\n" + price(upgrade, available)));
            cell.card.setDisable(!affordable);
            // Acquise : couleur pleine. Achetable : bordure vive. Trop chère ou verrouillée : éteinte.
            cell.card.setStyle(CARD_STYLE + (maxed
                    ? " -fx-text-fill: #10151f; -fx-background-color: " + color + "; -fx-border-color: #ffffff; -fx-opacity: 1;"
                    : affordable
                    ? " -fx-cursor: hand; -fx-text-fill: #ffffff; -fx-background-color: " + color + "44; -fx-border-color: " + color + ";"
                    : level > 0
                    ? " -fx-text-fill: #ffffff; -fx-background-color: " + color + "33; -fx-border-color: " + color + "88; -fx-opacity: 1;"
                    : " -fx-text-fill: " + color + "; -fx-background-color: #10151f; -fx-border-color: " + color + "44;"));

            // Le trait qui mène à la case s'allume quand elle est accessible.
            Color stroke = Color.web(available ? color : OFF);
            cell.link.setStroke(stroke);
            cell.elbow.setStroke(stroke);
        }
    }

    private static String title(DarkUpgrade upgrade, int level, boolean maxed) {
        if (upgrade.maxLevel() == 1) return upgrade.name() + (maxed ? " (acquise)" : "");
        return upgrade.name() + " (niveau " + level + (upgrade.hasLimit() ? "/" + upgrade.maxLevel() : "") + ")";
    }

    /** Le prix du prochain niveau, avec ce que le joueur possède en face. */
    private String price(DarkUpgrade upgrade, boolean available) {
        if (!game.hasDarkMatterFor(upgrade.id())) {
            return "Demande " + upgrade.darkMatter() + " matières noires gagnées (vous : "
                    + Format.count(game.darkMatterEarned()) + ")";
        }
        if (!available) return "Demande la case du dessus";
        BigNum cost = game.darkCostOf(upgrade.id());
        BigNum owned = game.darkBalance(upgrade.branch());
        return switch (upgrade.branch()) {
            case PARTICLES -> "Coût : " + Format.count(cost) + " particules (vous : " + Format.count(owned) + ")";
            case ATOMS -> "Coût : " + Format.count(cost) + " atomes (vous : " + Format.count(owned) + ")";
            case SIZE -> "Taille à atteindre : " + Format.length(cost) + " (actuelle : " + Format.length(owned) + ")";
            case DARK_MATTER -> "Coût : " + Format.count(cost) + (cost.gt(BigNum.ONE) ? " matières noires" : " matière noire");
        };
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        if (width <= 0 || branches.isEmpty()) return;
        // Combien de branches côte à côte : toutes, ou deux par étage dans une fenêtre étroite.
        int perFloor = width < TWO_FLOORS_BELOW ? Math.min(2, branches.size()) : branches.size();
        int floors = (branches.size() + perFloor - 1) / perFloor;
        double columnWidth = width / perFloor;
        double cardWidth = Math.max(96, Math.min(250, columnWidth - 14));
        // Les cases en éventail, en retrait, sont les plus étroites : ce sont elles qui décident de la hauteur.
        cardHeight = cardHeightFor(fans.containsValue(true) ? cardWidth - FAN_INDENT : cardWidth);

        double rootWidth = Math.min(260, width - 16);
        root.resizeRelocate(Math.floor((width - rootWidth) / 2), TOP, rootWidth, ROOT_HEIGHT);

        Map<DarkUpgrade.Branch, Double> headerYs = new EnumMap<>(DarkUpgrade.Branch.class);
        Map<DarkUpgrade.Branch, Double> centers = new EnumMap<>(DarkUpgrade.Branch.class);
        double y = TOP + ROOT_HEIGHT;
        double lastBusY = y;
        for (int floor = 0; floor < buses.size(); floor++) {
            Line bus = buses.get(floor);
            bus.setVisible(floor < floors);
            if (floor >= floors) continue;
            int first = floor * perFloor;
            int last = Math.min(branches.size(), first + perFloor) - 1;
            // Le trait horizontal de l'étage, d'où descend chaque branche vers son en-tête.
            double busY = y + BUS_GAP;
            double headerY = busY + BUS_GAP;
            place(bus, Math.min(width / 2, columnWidth / 2), busY,
                    Math.max(width / 2, (last - first + 0.5) * columnWidth), busY);
            int rows = 0;
            for (int index = first; index <= last; index++) {
                DarkUpgrade.Branch branch = branches.get(index);
                double centerX = (index - first + 0.5) * columnWidth;
                place(drops.get(branch), centerX, busY, centerX, headerY);
                headers.get(branch).resizeRelocate(Math.floor(centerX - cardWidth / 2), headerY, cardWidth, HEADER_HEIGHT);
                headerYs.put(branch, headerY);
                centers.put(branch, centerX);
                rows = Math.max(rows, rowCounts.get(branch));
            }
            lastBusY = busY;
            y = headerY + HEADER_HEIGHT + rows * (ROW_GAP + cardHeight);
        }
        // Le tronc passe entre les colonnes du milieu, de la racine au dernier étage.
        place(trunk, width / 2, TOP + ROOT_HEIGHT, width / 2, lastBusY);

        for (Cell cell : cells) {
            DarkUpgrade.Branch branch = cell.upgrade.branch();
            double centerX = centers.get(branch);
            double headerBottom = headerYs.get(branch) + HEADER_HEIGHT;
            double left = Math.floor(centerX - cardWidth / 2);
            double top = headerBottom + ROW_GAP + cell.row * (cardHeight + ROW_GAP);
            if (fans.get(branch)) {
                // En éventail : un tronc descend de l'en-tête à gauche des cases, un coude rejoint chacune.
                double trunkX = left + FAN_INDENT / 2;
                double middle = top + cardHeight / 2;
                cell.card.resizeRelocate(left + FAN_INDENT, top, cardWidth - FAN_INDENT, cardHeight);
                place(cell.link, trunkX, headerBottom, trunkX, middle);
                place(cell.elbow, trunkX, middle, left + FAN_INDENT, middle);
            } else {
                // Enchaînées : le trait part du bas de la case du dessus (ou de l'en-tête) et arrive en haut de celle-ci.
                cell.card.resizeRelocate(left, top, cardWidth, cardHeight);
                place(cell.link, centerX, top - ROW_GAP, centerX, top);
            }
        }

        // La hauteur de l'arbre dépend de la largeur : la zone qui défile autour doit la connaître.
        if (y + 8 != totalHeight) {
            totalHeight = y + 8;
            setPrefSize(720, totalHeight);
        }
    }

    private static void place(Line line, double startX, double startY, double endX, double endY) {
        line.setStartX(startX);
        line.setStartY(startY);
        line.setEndX(endX);
        line.setEndY(endY);
    }
}

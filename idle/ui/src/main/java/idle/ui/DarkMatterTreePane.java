package idle.ui;

import idle.core.BigBangCondition;
import idle.core.BigNum;
import idle.core.DarkUpgrade;
import idle.core.Game;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
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
 * Une case est une {@link Card} : un clic achète un niveau. Elle dit son niveau, ce qu'elle
 * fait en quelques mots et son prix ; l'explication complète, avec la valeur actuelle, vient en
 * mode détails ({@link Detail}), et les cases grandissent alors toutes ensemble. Un trait
 * s'allume quand la case qu'il mène est accessible.
 *
 * <p>L'arbre est construit d'après le catalogue du jeu ({@link Game#darkUpgrades()}) : ajouter
 * une case dans core suffit à la faire apparaître ici. La zone place elle-même ses cases et ses
 * traits : ils suivent la largeur de la fenêtre. Dans une fenêtre étroite, les branches ne
 * tiennent plus côte à côte : elles se rangent en deux étages de deux, reliés par le tronc.
 *
 * <p>Tout en bas, là où les branches se rejoignent, la case du Big Bang ({@link Game#bigBang()}).
 * Elle se paie avec les trois ressources à la fois et demande des succès et tous les défis : elle
 * liste ses cinq conditions, avec ce que le joueur en a. Comme elle efface tout, elle demande un
 * second clic dans les cinq secondes.
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
    /** Hauteur minimale d'une case : même presque vide, elle garde l'allure d'une carte. */
    private static final double MIN_CARD_HEIGHT = 84;
    /** Largeur maximale de la case du Big Bang : plus large qu'une case, elle porte cinq lignes. */
    private static final double BANG_WIDTH = 380;
    /** Temps laissé pour le second clic sur le Big Bang, en secondes. */
    private static final double CONFIRM_SECONDS = 5;
    private static final String CARD_STYLE = "-fx-font-size: 12px; -fx-padding: 6 8; -fx-background-radius: 6;"
            + " -fx-border-radius: 6;";

    /** Une case de l'arbre : l'amélioration, sa carte, sa place et les traits qui y mènent. */
    private static final class Cell {
        final DarkUpgrade upgrade;
        final Card card;
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
            this.card = new Card(COLORS.get(upgrade.branch()));
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
    /** Hauteur commune des cases : celle de la plus haute, mesurée à chaque mise en page. */
    private double cardHeight = MIN_CARD_HEIGHT;
    /** La largeur de case et le mode (détails ou non) pour lesquels {@link #cardHeight} a été mesurée. */
    private double measuredWidth = -1;
    private boolean measuredDetail = false;
    private double totalHeight = 0;
    /** La case du Big Bang, et les traits par lesquels les branches du dernier étage y descendent. */
    private final Card bang = new Card(GameApp.BIG_BANG_COLOR);
    private final Map<DarkUpgrade.Branch, Line> tails = new EnumMap<>(DarkUpgrade.Branch.class);
    private final Line gather = new Line();
    private final Line stem = new Line();
    /** Secondes qu'il reste pour confirmer le Big Bang ; 0 quand aucun premier clic n'attend. */
    private double armedFor = 0;

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
        for (DarkUpgrade.Branch branch : branches) tails.put(branch, new Line());
        List<Line> toBang = new ArrayList<>(tails.values());
        toBang.add(gather);
        toBang.add(stem);
        for (Line line : toBang) {
            line.setStrokeWidth(2);
            getChildren().add(line);
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
            cell.card.setOnAction(() -> {
                game.buyDark(cell.upgrade.id());
                refresh();
            });
            getChildren().add(cell.card);
        }
        bang.setOnAction(this::clickBang);
        getChildren().add(bang);
        setPrefSize(720, 600);
    }

    /** Premier clic : la case s'arme. Second clic dans les cinq secondes : le Big Bang a lieu. */
    private void clickBang() {
        if (armedFor > 0) {
            armedFor = 0;
            game.bigBang();
        } else if (game.canBigBang()) {
            armedFor = CONFIRM_SECONDS;
        }
        refresh();
    }

    /**
     * Fait passer le temps de la confirmation du Big Bang : sans second clic, elle s'annule.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    void frame(double elapsed) {
        if (armedFor > 0) armedFor = Math.max(0, armedFor - elapsed);
    }

    /** L'arbre n'est plus affiché : une confirmation en attente est oubliée. */
    void release() {
        armedFor = 0;
    }

    /** La case du Big Bang : pour les vérifications. */
    Card bangCard() {
        return bang;
    }

    /** Recopie l'état du jeu dans l'arbre : textes, couleurs, et cases cliquables seulement si l'achat est possible. */
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
            boolean oneTime = upgrade.maxLevel() == 1;

            // Acquise : couleur pleine. À portée : éclairée. Entamée : teintée. Trop chère ou verrouillée : éteinte.
            cell.card.show(maxed ? Card.State.DONE : affordable ? Card.State.READY
                            : !available ? Card.State.LOCKED : level > 0 ? Card.State.STARTED : Card.State.WAITING,
                    oneTime ? "" : level + (upgrade.hasLimit() ? "/" + upgrade.maxLevel() : ""),
                    !maxed ? "" : oneTime ? "acquise" : "max",
                    upgrade.name(), DarkText.brief(upgrade.effect(), game),
                    DarkText.describe(upgrade.effect(), game) + ".",
                    maxed ? "" : price(upgrade, available), "");

            // Le trait qui mène à la case s'allume quand elle est accessible.
            Color stroke = Color.web(available ? color : OFF);
            cell.link.setStroke(stroke);
            cell.elbow.setStroke(stroke);
        }
        refreshBang();
    }

    /** La case du Big Bang : ses cinq conditions, et ce qu'un clic fera. */
    private void refreshBang() {
        boolean ready = game.canBigBang();
        if (!ready) armedFor = 0;     // une condition vient de tomber : une explosion a repris les atomes, par exemple
        int met = game.bigBangConditionsMet();
        int all = BigBangCondition.values().length;
        StringBuilder lines = new StringBuilder();
        for (BigBangCondition condition : BigBangCondition.values()) {
            if (lines.length() > 0) lines.append('\n');
            lines.append(game.isBigBangConditionMet(condition) ? "● " : "○ ").append(condition(condition));
        }
        String detail = "Le passage à l'acte suivant. Les cinq conditions doivent être réunies au même moment, hors "
                + "de tout défi : les particules et les atomes sont ceux de la partie en cours (une seconde de "
                + "production compte comme des particules en main), la matière noire est celle qui reste à "
                + "dépenser. Une explosion reprend particules et atomes : mieux vaut couper l'explosion "
                + "automatique le temps de les réunir. Le Big Bang efface tout, jusqu'à la matière noire, son "
                + "arbre, ses automatismes et les défis réussis ; il ne laisse que les succès, les records des "
                + "défis, le temps de jeu et les statistiques. L'acte qu'il ouvre n'existe pas encore.";
        int count = game.bigBangs();
        String aside = count == 0 ? "" : "déjà " + count + (count > 1 ? " déclenchés" : " déclenché");
        if (armedFor > 0) {
            bang.show(Card.State.ARMED, "", (int) Math.ceil(armedFor) + " s", "Big Bang", lines.toString(), detail,
                    "Cliquer encore : tout repart du premier générateur", aside);
        } else if (ready) {
            bang.show(Card.State.READY, "", "prêt", "Big Bang", lines.toString(), detail, "Déclencher", aside);
        } else {
            bang.show(met > 0 ? Card.State.STARTED : Card.State.WAITING, "", "", "Big Bang", lines.toString(), detail,
                    met == all ? "Pas pendant un défi" : met + (met > 1 ? " conditions sur " : " condition sur ") + all, aside);
        }
        Color stroke = Color.web(ready ? GameApp.BIG_BANG_COLOR : OFF);
        for (Line tail : tails.values()) tail.setStroke(stroke);
        gather.setStroke(stroke);
        stem.setStroke(stroke);
    }

    /** Une condition du Big Bang : ce qu'il faut, et entre parenthèses ce que le joueur a. */
    private String condition(BigBangCondition condition) {
        return switch (condition) {
            case PARTICLES -> Format.count(Game.BIG_BANG_PARTICLES) + " particules ("
                    + Format.count(game.darkBalance(DarkUpgrade.Branch.PARTICLES)) + ")";
            case ATOMS -> Format.count(Game.BIG_BANG_ATOMS) + " atomes (" + Format.count(game.state().atoms()) + ")";
            case DARK_MATTER -> Format.count(Game.BIG_BANG_DARK_MATTER) + " matières noires en réserve ("
                    + Format.count(game.state().darkMatter()) + ")";
            case ACHIEVEMENTS -> Game.BIG_BANG_ACHIEVEMENTS + " succès (" + game.achievementCount() + ")";
            case CHALLENGES -> "Les " + game.challenges().size() + " défis réussis (" + game.completedChallenges() + ")";
        };
    }

    /** Le prix du prochain niveau, ou ce qu'il faut pour que la case s'ouvre. Ce que le joueur possède est dans l'en-tête. */
    private String price(DarkUpgrade upgrade, boolean available) {
        if (!game.hasDarkMatterFor(upgrade.id())) {
            return "À " + upgrade.darkMatter() + " matières noires gagnées";
        }
        if (!available) return "Après la case du dessus";
        BigNum cost = game.darkCostOf(upgrade.id());
        return switch (upgrade.branch()) {
            case PARTICLES -> Format.count(cost) + " particules";
            case ATOMS -> Format.count(cost) + " atomes";
            case SIZE -> "Taille " + Format.length(cost);
            case DARK_MATTER -> Format.count(cost) + (cost.gt(BigNum.ONE) ? " matières noires" : " matière noire");
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
        // Toutes les cases ont la hauteur de la plus haute, mesurée à la largeur qu'elle aura. Tant que
        // ni la largeur ni le mode ne changent, la hauteur ne fait que grandir : un prix qui s'allonge
        // d'un chiffre ne doit pas faire trembler tout l'arbre.
        if (cardWidth != measuredWidth || Detail.shown() != measuredDetail) {
            measuredWidth = cardWidth;
            measuredDetail = Detail.shown();
            cardHeight = MIN_CARD_HEIGHT;
        }
        for (Cell cell : cells) {
            double room = fans.get(cell.upgrade.branch()) ? cardWidth - FAN_INDENT : cardWidth;
            cardHeight = Math.max(cardHeight, Math.ceil(cell.card.prefHeight(room)));
        }

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

        // Sous le dernier étage : ses branches descendent jusqu'à un trait commun, d'où part la case du Big Bang.
        double gatherY = y + ROW_GAP / 2;
        int firstOfLast = (floors - 1) * perFloor;
        for (int index = 0; index < branches.size(); index++) {
            DarkUpgrade.Branch branch = branches.get(index);
            Line tail = tails.get(branch);
            tail.setVisible(index >= firstOfLast);
            if (index < firstOfLast) continue;
            double centerX = centers.get(branch);
            double bottom = headerYs.get(branch) + HEADER_HEIGHT + rowCounts.get(branch) * (ROW_GAP + cardHeight);
            place(tail, centerX, bottom, centerX, gatherY);
        }
        double firstCenter = centers.get(branches.get(firstOfLast));
        double lastCenter = centers.get(branches.get(branches.size() - 1));
        place(gather, Math.min(firstCenter, width / 2), gatherY, Math.max(lastCenter, width / 2), gatherY);
        double bangTop = gatherY + ROW_GAP;
        place(stem, width / 2, gatherY, width / 2, bangTop);
        double bangWidth = Math.max(96, Math.min(BANG_WIDTH, width - 16));
        double bangHeight = Math.max(MIN_CARD_HEIGHT, Math.ceil(bang.prefHeight(bangWidth)));
        bang.resizeRelocate(Math.floor((width - bangWidth) / 2), bangTop, bangWidth, bangHeight);
        y = bangTop + bangHeight;

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

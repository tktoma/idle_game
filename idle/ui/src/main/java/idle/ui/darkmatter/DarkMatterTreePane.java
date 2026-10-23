package idle.ui.darkmatter;

import idle.core.Game;
import idle.core.bigbang.BigBangCondition;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.ui.Confirm;
import idle.ui.GameApp;
import idle.ui.NextReset;
import idle.ui.common.Card;
import idle.ui.common.DarkText;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
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
 * <p>Les liaisons et les accords (versions 0.070 et 0.071) s'ouvrent avec les voies des bifurcations.
 * Ils ont leur planche sous la case du Big Bang, un rang par rang de bifurcations, et chaque carte
 * dit, voie par voie, ce qui l'ouvre : des traits à travers tout l'arbre se lisaient mal.
 *
 * <p>Tout en bas, là où les branches se rejoignent, la case du Big Bang ({@link Game#bigBang()}).
 * Elle se paie avec les trois ressources à la fois et demande des succès et tous les défis : elle
 * liste ses conditions (cinq, six avec le ciel), avec ce que le joueur en a. Comme elle efface tout, elle demande un
 * second clic dans les cinq secondes.
 */
public final class DarkMatterTreePane extends Pane {

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
    /** L'écart entre les deux voies d'une bifurcation, en pixels. */
    private static final double FORK_GAP = 10;
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
        /** Petit trait horizontal du tronc à la case, pour une branche en éventail ; pour une voie de bifurcation, le trait qui rejoint la case d'après. */
        final Line elbow = new Line();
        /** Une voie de bifurcation (version 0.069) : 0 à gauche, 1 à droite ; −1 pour une case ordinaire. */
        final int side;
        /** Sa place à la dernière mise en page : les traits des liaisons en partent. */
        double x, y, width;

        Cell(DarkUpgrade upgrade, int row, int side) {
            this.upgrade = upgrade;
            this.row = row;
            this.side = side;
            this.card = new Card(COLORS.get(upgrade.branch()));
        }
    }

    /**
     * Une liaison ou un accord (versions 0.070 et 0.071) : sa carte, les voies qui l'ouvrent, et le rang
     * de bifurcations auquel elle appartient (le premier ou le second de chaque branche).
     */
    private static final class LinkCell {
        final DarkUpgrade upgrade;
        final Card card = new Card(GameApp.DARK_MATTER_COLOR);
        final List<Cell> options = new ArrayList<>();
        /** 1 pour le premier rang de bifurcations, 2 pour le second. */
        int level;

        LinkCell(DarkUpgrade upgrade) {
            this.upgrade = upgrade;
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
    private final List<LinkCell> links = new ArrayList<>();
    /** Le titre de la planche des liaisons, et celui de chacun de ses rangs. */
    private final Label linksTitle = new Label();
    private final Map<Integer, Label> levelTitles = new java.util.TreeMap<>();
    /** Pour chaque voie, les liaisons et accords qu'elle aide à ouvrir : son rappel, en mode détails. */
    private final Map<String, List<String>> opens = new HashMap<>();
    /** Les bifurcations suivies d'une case : les autres (la dernière d'une branche) n'ont pas de trait vers le bas. */
    private final java.util.Set<String> forksWithChild = new java.util.HashSet<>();
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
    /** Durée du surlignage du chemin d'une bifurcation, en secondes (version 0.084). */
    public static final double LIT_SECONDS = 6;
    /** Les cases surlignées : la voie ou la liaison cliquée, et tout ce qu'il faut au-dessus d'elle. */
    private final java.util.Set<Cell> lit = new java.util.LinkedHashSet<>();
    private final java.util.Set<LinkCell> litLinks = new java.util.HashSet<>();
    private double litFor = 0;
    /** Les deux voies de chaque bifurcation, par son nom. */
    private final Map<String, List<Cell>> forkCells = new HashMap<>();

    DarkMatterTreePane(Game game) {
        this.game = game;
        setMinSize(0, 0);

        Map<String, Cell> byId = new HashMap<>();
        Map<DarkUpgrade.Branch, Integer> filled = new EnumMap<>(DarkUpgrade.Branch.class);
        Map<DarkUpgrade.Branch, Integer> independent = new EnumMap<>(DarkUpgrade.Branch.class);
        Map<String, Cell> forks = new HashMap<>();
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            if (upgrade.isLink()) {
                links.add(new LinkCell(upgrade));
                continue;
            }
            DarkUpgrade.Branch branch = upgrade.branch();
            if (!branches.contains(branch)) branches.add(branch);
            // Les deux voies d'une bifurcation partagent une rangée, côte à côte.
            Cell first = upgrade.isFork() ? forks.get(upgrade.fork()) : null;
            Cell cell = first != null ? new Cell(upgrade, first.row, 1)
                    : new Cell(upgrade, filled.merge(branch, 1, Integer::sum) - 1, upgrade.isFork() ? 0 : -1);
            if (upgrade.isFork() && first == null) forks.put(upgrade.fork(), cell);
            cell.parent = upgrade.requires() == null ? null
                    : upgrade.requiredFork() != null ? forks.get(upgrade.requiredFork()) : byId.get(upgrade.requires());
            if (cell.parent == null) independent.merge(branch, 1, Integer::sum);
            if (upgrade.requiredFork() != null) forksWithChild.add(upgrade.requiredFork());
            byId.put(upgrade.id(), cell);
            cells.add(cell);
            if (upgrade.isFork()) forkCells.computeIfAbsent(upgrade.fork(), key -> new ArrayList<>()).add(cell);
        }
        rowCounts.putAll(filled);
        // Le rang d'une bifurcation : la première de sa branche, ou la deuxième. Une liaison prend le rang de ses voies.
        Map<String, Integer> forkLevel = new HashMap<>();
        Map<DarkUpgrade.Branch, Integer> forksInBranch = new EnumMap<>(DarkUpgrade.Branch.class);
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            if (upgrade.isFork() && !forkLevel.containsKey(upgrade.fork())) {
                forkLevel.put(upgrade.fork(), forksInBranch.merge(upgrade.branch(), 1, Integer::sum));
            }
        }
        for (LinkCell link : links) {
            for (String id : link.upgrade.linkedIds()) {
                Cell option = byId.get(id);
                if (option == null) continue;
                link.options.add(option);
                link.level = Math.max(link.level, forkLevel.getOrDefault(option.upgrade.fork(), 1));
                opens.computeIfAbsent(id, key -> new ArrayList<>()).add(link.upgrade.name());
            }
            levelTitles.computeIfAbsent(link.level, level -> new Label());
        }
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
            cell.elbow.setVisible(fans.get(cell.upgrade.branch()) || (cell.side >= 0 && forksWithChild.contains(cell.upgrade.fork())));
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
            // Une voie cliquée (version 0.084), prenable ou non : le chemin qui y mène s'allume.
            if (cell.upgrade.isFork()) cell.card.setOnLook(() -> {
                light(cell, null);
                refresh();
            });
            getChildren().add(cell.card);
        }
        linksTitle.setWrapText(true);
        linksTitle.setTextAlignment(TextAlignment.CENTER);
        linksTitle.setAlignment(Pos.CENTER);
        linksTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #c9b8ff;");
        if (!links.isEmpty()) getChildren().add(linksTitle);
        for (Label title : levelTitles.values()) {
            title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + GameApp.DARK_MATTER_COLOR + ";");
            getChildren().add(title);
        }
        for (LinkCell link : links) {
            link.card.setOnAction(() -> {
                game.buyDark(link.upgrade.id());
                refresh();
            });
            link.card.setOnLook(() -> {
                light(null, link);
                refresh();
            });
            getChildren().add(link.card);
        }
        bang.setOnAction(this::clickBang);
        getChildren().add(bang);
        setPrefSize(720, 600);
    }

    /**
     * Premier clic : la case s'arme. Second clic dans les cinq secondes : le Big Bang a lieu. Sans
     * la confirmation ({@link Confirm#bigBang()}), le premier clic suffit.
     */
    private void clickBang() {
        if (armedFor > 0 || (!Confirm.bigBang() && game.canBigBang())) {
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
    public void frame(double elapsed) {
        if (armedFor > 0) armedFor = Math.max(0, armedFor - elapsed);
        if (litFor > 0) {
            litFor = Math.max(0, litFor - elapsed);
            if (litFor == 0) {
                lit.clear();
                litLinks.clear();
                refresh();
            }
        }
    }

    /**
     * Surligne ce qu'il faut pour une voie (sa bifurcation, et chaque case au-dessus d'elle jusqu'à
     * l'en-tête : les deux voies d'une bifurcation du dessus, l'une ou l'autre suffisant), ou pour une
     * liaison (ses voies, et ce qu'il faut à chacune). Un second clic sur la même case l'éteint.
     */
    private void light(Cell cell, LinkCell link) {
        boolean same = litFor > 0 && (cell != null ? litLinks.isEmpty() && first() == cell : litLinks.contains(link));
        lit.clear();
        litLinks.clear();
        litFor = 0;
        if (same) return;
        if (link != null) {
            litLinks.add(link);
            for (Cell option : link.options) addPath(option);
        } else {
            addPath(cell);
        }
        litFor = LIT_SECONDS;
    }

    private Cell first() {
        return lit.isEmpty() ? null : lit.iterator().next();
    }

    private void addPath(Cell cell) {
        lit.add(cell);       // la case cliquée d'abord : elle mène le surlignage
        for (Cell at = cell; at != null; at = at.parent) {
            if (at.upgrade.isFork()) lit.addAll(forkCells.getOrDefault(at.upgrade.fork(), List.of(at)));
            else lit.add(at);
        }
    }

    /** Les cases surlignées, liaisons comprises : pour les vérifications. */
    public int litCount() {
        return lit.size() + litLinks.size();
    }

    /** Vrai si cette case est surlignée : pour les vérifications. */
    public boolean isLit(String darkUpgradeId) {
        for (Cell cell : lit) if (cell.upgrade.id().equals(darkUpgradeId)) return true;
        for (LinkCell link : litLinks) if (link.upgrade.id().equals(darkUpgradeId)) return true;
        return false;
    }

    /** Un clic sur la case de cette amélioration, comme le joueur : pour les vérifications. */
    public void clickCell(String darkUpgradeId) {
        for (Cell cell : cells) if (cell.upgrade.id().equals(darkUpgradeId)) cell.card.click();
        for (LinkCell link : links) if (link.upgrade.id().equals(darkUpgradeId)) link.card.click();
    }

    /** L'arbre n'est plus affiché : une confirmation en attente est oubliée. */
    void release() {
        armedFor = 0;
        lit.clear();
        litLinks.clear();
        litFor = 0;
    }

    /** La case du Big Bang : pour les vérifications. */
    public Card bangCard() {
        return bang;
    }

    /** Recopie l'état du jeu dans l'arbre : textes, couleurs, et cases cliquables seulement si l'achat est possible. */
    public void refresh() {
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
            DarkUpgrade rival = game.darkForkRival(upgrade.id());
            if (upgrade.isFork() && (rival != null || level == 0)) {
                // Une voie de bifurcation (version 0.069) : prise, ouverte au choix, ou fermée par l'autre jusqu'au prochain Big Bang.
                // Avec la Superposition du collisionneur (0.070), les deux voies se prennent.
                boolean both = game.hasSuperposition();
                boolean absence = game.darkStyle(upgrade) == Game.Style.ABSENCE;
                cell.card.show(rival != null ? Card.State.LOCKED : affordable ? Card.State.READY : !available ? Card.State.LOCKED : Card.State.WAITING,
                        "", rival != null ? "fermée" : planned(upgrade) ? "prévue" : both ? "les 2 voies" : absence ? "absence" : "présence",
                        upgrade.name(), DarkText.brief(upgrade.effect(), game) + (absence ? absenceBrief() : ""),
                        Detail.shown() ? DarkText.describe(upgrade.effect(), game) + style(upgrade) + opensText(upgrade) + (both
                                ? ". Une bifurcation, ouverte des deux côtés par la Superposition du collisionneur : les deux voies se prennent et restent au Big Bang."
                                : ". Une bifurcation : prendre cette voie ferme l'autre jusqu'au prochain Big Bang, où le choix se refait. "
                                        + "La Superposition du collisionneur ouvre les deux.") : "",
                        rival != null ? "« " + rival.name() + " » est prise, jusqu'au prochain Big Bang" : price(upgrade, available), "");
                Color stroke = Color.web(available ? color : OFF);
                cell.link.setStroke(stroke);
                cell.elbow.setStroke(Color.web(OFF));
                continue;
            }

            // Acquise : couleur pleine. À portée : éclairée. Entamée : teintée. Trop chère ou verrouillée : éteinte.
            cell.card.show(maxed ? Card.State.DONE : affordable ? Card.State.READY
                            : !available ? Card.State.LOCKED : level > 0 ? Card.State.STARTED : Card.State.WAITING,
                    oneTime ? "" : level + (upgrade.hasLimit() ? "/" + upgrade.maxLevel() : ""),
                    !maxed ? "" : oneTime ? "acquise" : "max",
                    upgrade.name(), DarkText.brief(upgrade.effect(), game),
                    Detail.shown() ? DarkText.describe(upgrade.effect(), game) + "." : "",
                    maxed ? "" : price(upgrade, available), "");

            // Le trait qui mène à la case s'allume quand elle est accessible.
            Color stroke = Color.web(available ? color : OFF);
            cell.link.setStroke(stroke);
            cell.elbow.setStroke(cell.side >= 0 && level == 0 ? Color.web(OFF) : stroke);
        }
        for (LinkCell link : links) refreshLink(link);
        showLit();
        linksTitle.setText("Liaisons et accords : ils s'ouvrent avec les voies des bifurcations. Une liaison demande l'une "
                + "de ses deux voies, un accord ses trois voies du même style.");
        for (Map.Entry<Integer, Label> title : levelTitles.entrySet()) {
            title.getValue().setText(title.getKey() == 1 ? "Premier rang de bifurcations" : "Second rang de bifurcations");
        }
        refreshBang();
    }

    /** Vrai si cette case attend dans le plan de l'arbre (version 0.084). */
    private boolean planned(DarkUpgrade upgrade) {
        return game.darkLevelOf(upgrade.id()) == 0 && game.state().darkPlan().contains(upgrade.id());
    }

    /** Le surlignage : un halo blanc autour des cases, leurs traits épaissis et blanchis. */
    private void showLit() {
        javafx.scene.effect.DropShadow halo = new javafx.scene.effect.DropShadow(16, Color.web("#ffffff"));
        halo.setSpread(0.45);
        for (Cell cell : cells) {
            boolean on = lit.contains(cell);
            cell.card.setEffect(on ? halo : null);
            cell.link.setStrokeWidth(on ? 4 : 2);
            if (on) cell.link.setStroke(Color.web("#ffffff"));
        }
        for (LinkCell link : links) link.card.setEffect(litLinks.contains(link) ? halo : null);
    }

    /** Les liaisons et accords que cette voie aide à ouvrir, rangés sous l'arbre. */
    private String opensText(DarkUpgrade upgrade) {
        List<String> names = opens.get(upgrade.id());
        return names == null ? "" : ". Ouvre, sous l'arbre : " + String.join(", ", names);
    }

    /**
     * Ce qu'une voie d'absence ajoute au temps rejoué, en une ligne. Version 0.126 : une fois tout le temps d'absence
     * rejoué (l'univers formé), elle n'ajoute plus rien, et la carte le dit.
     */
    private String absenceBrief() {
        return game.offlineRate() >= 1 ? "\nAbsence : déjà 100 % rejoué"
                : "\nAbsence +" + ElementText.number(Game.ABSENCE_PATH_BONUS * 100) + " points";
    }

    /** Ce que dit le style d'une voie (version 0.070), en mode détails. */
    private String style(DarkUpgrade upgrade) {
        return game.darkStyle(upgrade) == Game.Style.ABSENCE
                ? ". Voie d'absence : un peu moins forte tant que vous êtes là, mais elle rend "
                        + ElementText.number(Game.ABSENCE_PATH_BONUS * 100) + " points de plus du temps passé jeu fermé"
                        + (game.offlineRate() >= 1 ? " (sans effet en ce moment : tout le temps d'absence est déjà rejoué)" : "")
                : ". Voie de présence : la plus forte tant que vous êtes là, sans rien de plus en votre absence";
    }

    /**
     * Une liaison ou un accord : comme une case, mais la carte dit aussi, voie par voie, ce qui l'ouvre :
     * ✓ prise, ○ encore à prendre, × fermée par l'autre voie de sa bifurcation.
     */
    private void refreshLink(LinkCell link) {
        DarkUpgrade upgrade = link.upgrade;
        boolean maxed = game.isDarkMaxed(upgrade.id());
        boolean available = game.isDarkAvailable(upgrade.id());
        boolean affordable = game.canBuyDark(upgrade.id());
        boolean accord = upgrade.isAccord();
        List<String> needs = new ArrayList<>();
        int closed = 0;
        for (Cell option : link.options) {
            DarkUpgrade voie = option.upgrade;
            boolean shut = game.darkLevelOf(voie.id()) == 0 && game.darkForkRival(voie.id()) != null;
            if (shut) closed++;
            String mark = game.darkLevelOf(voie.id()) > 0 ? "✓ " : shut ? "× " : "○ ";
            needs.add(mark + voie.name() + " (" + branchName(voie.branch()) + ")");
        }
        // Fermée jusqu'au prochain Big Bang : un accord dont une voie est fermée, une liaison dont toutes le sont.
        boolean shutOut = accord ? closed > 0 : closed == link.options.size();
        String missing = shutOut ? "Fermée jusqu'au prochain Big Bang" : accord ? "Il faut les trois voies"
                : link.options.size() == 1 ? "Il faut cette voie" : "Il faut l'une des deux voies";
        String joined = String.join(accord ? "\n" : "\nou ", needs);
        link.card.show(maxed ? Card.State.DONE : affordable ? Card.State.READY : !available ? Card.State.LOCKED : Card.State.WAITING,
                "", maxed ? "acquise" : planned(upgrade) ? "prévue" : accord ? (game.darkStyle(upgrade) == Game.Style.ABSENCE ? "accord d'absence" : "accord de présence") : "liaison",
                upgrade.name(), DarkText.brief(upgrade.effect(), game) + "\n" + joined,
                Detail.shown() ? DarkText.describe(upgrade.effect(), game) + (accord
                        ? ". Un accord : il demande ses trois voies à la fois, une par branche, toutes du même style."
                        : ". Une liaison : l'une ou l'autre de ses voies suffit.")
                        + " Elle se paie en matière noire et repart au Big Bang avec les voies, sauf avec la Superposition du collisionneur." : "",
                maxed ? "" : available ? price(upgrade, true) : missing, "");
    }

    private static String branchName(DarkUpgrade.Branch branch) {
        return switch (branch) {
            case PARTICLES -> "particules";
            case ATOMS -> "atomes";
            case SIZE -> "taille";
            case DARK_MATTER -> "matière noire";
        };
    }

    /** La case du Big Bang : ses conditions (cinq, six avec le ciel), et ce qu'un clic fera. */
    private void refreshBang() {
        boolean ready = game.canBigBang();
        if (!ready) armedFor = 0;     // une condition vient de tomber : une explosion a repris les atomes, par exemple
        int met = game.bigBangConditionsMet();
        int all = game.bigBangConditions().size();
        StringBuilder lines = new StringBuilder();
        for (BigBangCondition condition : game.bigBangConditions()) {
            if (lines.length() > 0) lines.append('\n');
            lines.append(game.isBigBangConditionMet(condition) ? "● " : "○ ").append(condition(condition));
        }
        String detail = "Le passage à l'acte suivant. Les conditions doivent être réunies au même moment, hors "
                + "de tout défi : les particules et les atomes sont ceux de la partie en cours (une seconde de "
                + "production compte comme des particules en main), la matière noire est celle qui reste à "
                + "dépenser. Une explosion reprend particules et atomes : mieux vaut couper l'explosion "
                + "automatique le temps de les réunir. "
                + (game.bigBangKeepsDarkTree()
                        ? "Le Big Bang efface les particules, les atomes, les éléments, la matière noire en réserve "
                                + "et sa taille. Cet arbre, ses automatismes et les défis réussis restent : ce sont "
                                + "les paliers du troisième et du cinquième Big Bang. "
                        : game.bigBangKeepsChallenges()
                        ? "Le Big Bang efface tout, jusqu'à la matière noire, son arbre et ses automatismes. Les "
                                + "défis réussis restent : c'est le palier du troisième Big Bang. "
                        : "Le Big Bang efface tout, jusqu'à la matière noire, son arbre, ses automatismes et les "
                                + "défis réussis. ")
                + "Restent les succès, les records des défis, le temps de jeu, les statistiques, le collisionneur"
                + (game.bigBangs() == 0 ? ". Il ouvre l'onglet Big Bang." : ", et tout ce que contient l'onglet Big Bang.");
        // Ce qu'il rapportera, juste sous ses conditions : une remise à zéro se lit d'abord par ce qu'elle donne.
        lines.append('\n').append(NextReset.bigBangGives(game));
        int count = game.bigBangs();
        String aside = count == 0 ? "" : "déjà " + count + (count > 1 ? " déclenchés" : " déclenché");
        if (armedFor > 0) {
            bang.show(Card.State.ARMED, "", (int) Math.ceil(armedFor) + " s", "Big Bang", lines.toString(), detail,
                    game.bigBangKeepsDarkTree() ? "Cliquer encore : tout repart du premier générateur, sauf cet arbre"
                            : "Cliquer encore : tout repart du premier générateur", aside);
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
            case PARTICLES -> Format.count(game.bigBangParticles()) + " particules ("
                    + Format.count(game.darkBalance(DarkUpgrade.Branch.PARTICLES)) + ")"
                    // Le calme du Big Bang (version 0.114) : ce qu'il a déjà effacé du seuil, et ce qu'il reste à attendre.
                    + (game.bigBangCalmRelief() > 0 ? ", calme " + Math.round(100 * game.bigBangCalmRelief()) + " % : "
                            + (game.bigBangCalmRelief() >= 1 ? "plus rien à attendre"
                            // Version 0.126 : le compteur du calme lui-même, que les achats des automatismes rompent aussi.
                            : "encore " + Format.wait(Game.BIG_BANG_CALM_SECONDS - game.state().bangCalm()) + " sans aucun achat d'amélioration") : "");
            case ATOMS -> Format.count(game.bigBangAtoms()) + " atomes (" + Format.count(game.state().atoms()) + ")";
            case DARK_MATTER -> Format.count(game.bigBangDarkMatter()) + " matières noires en réserve ("
                    + Format.darkMatter(game.state().darkMatter()) + ")";
            case ACHIEVEMENTS -> Game.BIG_BANG_ACHIEVEMENTS + " succès (" + game.achievementCount() + ")";
            case CHALLENGES -> "Les " + idle.core.darkmatter.Challenges.DEFAULT.size() + " défis réussis (" + game.baseChallengesCompleted() + ")";
            case SKY -> game.bigBangSkyRequirement() + " astres mûrs (" + game.bodiesMature() + ")";
        };
    }

    /** Le prix du prochain niveau, ou ce qu'il faut pour que la case s'ouvre. Ce que le joueur possède est dans l'en-tête. */
    private String price(DarkUpgrade upgrade, boolean available) {
        if (!available) return "Après la case du dessus";
        BigNum cost = game.darkCostOf(upgrade.id());
        return switch (upgrade.branch()) {
            case PARTICLES -> Format.count(cost) + " particules";
            case ATOMS -> Format.count(cost) + " atomes";
            case SIZE -> "Taille " + Format.length(cost);
            case DARK_MATTER -> Format.count(cost) + (cost.gt(BigNum.ONE) ? " matières noires" : " matière noire");
        };
    }

    /** La place d'une rangée d'une branche. */
    private int slot(int row) {
        return row;
    }

    @Override
    public void layoutChildren() {
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
        double forkWidth = Math.floor((cardWidth - FORK_GAP) / 2);
        for (Cell cell : cells) {
            double room = cell.side >= 0 ? forkWidth : fans.get(cell.upgrade.branch()) ? cardWidth - FAN_INDENT : cardWidth;
            cardHeight = Math.max(cardHeight, Math.ceil(cell.card.prefHeight(room)));
        }
        double pitch = cardHeight + ROW_GAP;

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
            int slots = 0;
            for (int index = first; index <= last; index++) {
                DarkUpgrade.Branch branch = branches.get(index);
                double centerX = (index - first + 0.5) * columnWidth;
                place(drops.get(branch), centerX, busY, centerX, headerY);
                headers.get(branch).resizeRelocate(Math.floor(centerX - cardWidth / 2), headerY, cardWidth, HEADER_HEIGHT);
                headerYs.put(branch, headerY);
                centers.put(branch, centerX);
                slots = Math.max(slots, slot(rowCounts.get(branch) - 1) + 1);
            }
            lastBusY = busY;
            y = headerY + HEADER_HEIGHT + slots * pitch;
        }
        // Le tronc passe entre les colonnes du milieu, de la racine au dernier étage.
        place(trunk, width / 2, TOP + ROOT_HEIGHT, width / 2, lastBusY);

        for (Cell cell : cells) {
            DarkUpgrade.Branch branch = cell.upgrade.branch();
            double centerX = centers.get(branch);
            double headerBottom = headerYs.get(branch) + HEADER_HEIGHT;
            double left = Math.floor(centerX - cardWidth / 2);
            double top = headerBottom + ROW_GAP + slot(cell.row) * pitch;
            // Le bas de la case du dessus (ou de l'en-tête) : une rangée de liaisons peut s'intercaler entre les deux.
            double above = cell.parent == null ? headerBottom : headerBottom + ROW_GAP + slot(cell.parent.row) * pitch + cardHeight;
            if (fans.get(branch)) {
                // En éventail : un tronc descend de l'en-tête à gauche des cases, un coude rejoint chacune.
                double trunkX = left + FAN_INDENT / 2;
                double middle = top + cardHeight / 2;
                put(cell, left + FAN_INDENT, top, cardWidth - FAN_INDENT);
                place(cell.link, trunkX, headerBottom, trunkX, middle);
                place(cell.elbow, trunkX, middle, left + FAN_INDENT, middle);
            } else if (cell.side >= 0) {
                // Une voie de bifurcation : la moitié de la largeur, à gauche ou à droite. Un trait en biais y mène
                // depuis la case du dessus ; un autre en repart vers le trait qui descend à la case d'après.
                double cardLeft = left + cell.side * (forkWidth + FORK_GAP);
                double cardCenter = cardLeft + forkWidth / 2;
                put(cell, cardLeft, top, forkWidth);
                place(cell.link, centerX, above, cardCenter, top);
                place(cell.elbow, cardCenter, top + cardHeight, centerX, top + cardHeight + ROW_GAP / 2);
            } else {
                // Enchaînées : le trait part du bas de la case du dessus (ou de l'en-tête) et arrive en haut de celle-ci.
                // Après une bifurcation, il part du point où se rejoignent les traits de ses deux voies.
                double from = cell.parent != null && cell.parent.side >= 0 ? above + ROW_GAP / 2 : above;
                put(cell, left, top, cardWidth);
                place(cell.link, centerX, from, centerX, top);
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
            double bottom = headerYs.get(branch) + HEADER_HEIGHT + slot(rowCounts.get(branch) - 1) * pitch + pitch;
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

        y = layoutLinks(width, y);

        // La hauteur de l'arbre dépend de la largeur : la zone qui défile autour doit la connaître.
        if (y + 8 != totalHeight) {
            totalHeight = y + 8;
            setPrefSize(720, totalHeight);
        }
    }

    /** Largeur visée d'une carte de liaison, et l'écart entre deux. */
    private static final double LINK_WIDTH = 200;
    private static final double LINK_GAP = 10;

    /**
     * La planche des liaisons et des accords (version 0.071), sous la case du Big Bang : un titre, puis
     * un rang par rang de bifurcations, liaisons d'abord, accords ensuite, chacun sur une ligne qui
     * passe à la suivante quand la fenêtre est étroite. Pas de traits : chaque carte dit ses voies.
     *
     * @return le bas de ce qui a été placé
     */
    private double layoutLinks(double width, double bottom) {
        if (links.isEmpty()) return bottom;
        double inner = Math.max(120, width - 16);
        int columns = (int) Math.max(1, Math.floor((inner + LINK_GAP) / (LINK_WIDTH * 0.85 + LINK_GAP)));
        double y = bottom + 2 * ROW_GAP;
        double titleHeight = Math.ceil(linksTitle.prefHeight(Math.min(inner, 640)));
        linksTitle.resizeRelocate(Math.floor((width - Math.min(inner, 640)) / 2), y, Math.min(inner, 640), titleHeight);
        y += titleHeight + ROW_GAP / 2;
        for (Map.Entry<Integer, Label> level : levelTitles.entrySet()) {
            List<LinkCell> row = new ArrayList<>();
            for (LinkCell link : links) if (link.level == level.getKey()) row.add(link);
            int perLine = Math.min(columns, row.size());
            double cardW = Math.min(LINK_WIDTH, Math.floor((inner - (perLine - 1) * LINK_GAP) / perLine));
            double height = MIN_CARD_HEIGHT;
            for (LinkCell link : row) height = Math.max(height, Math.ceil(link.card.prefHeight(cardW)));
            Label title = level.getValue();
            title.resizeRelocate(8, y, inner, 18);
            y += 22;
            for (int i = 0; i < row.size(); i++) {
                int line = i / perLine, column = i % perLine;
                int onThisLine = Math.min(perLine, row.size() - line * perLine);
                double left = Math.floor((width - (onThisLine * cardW + (onThisLine - 1) * LINK_GAP)) / 2);
                row.get(i).card.resizeRelocate(left + column * (cardW + LINK_GAP), y + line * (height + LINK_GAP), cardW, height);
            }
            int lines = (row.size() + perLine - 1) / perLine;
            y += lines * (height + LINK_GAP) + ROW_GAP / 2;
        }
        return y;
    }

    private void put(Cell cell, double x, double y, double width) {
        cell.x = x;
        cell.y = y;
        cell.width = width;
        cell.card.resizeRelocate(x, y, width, cardHeight);
    }

    private static void place(Line line, double startX, double startY, double endX, double endY) {
        line.setStartX(startX);
        line.setStartY(startY);
        line.setEndX(endX);
        line.setEndY(endY);
    }
}

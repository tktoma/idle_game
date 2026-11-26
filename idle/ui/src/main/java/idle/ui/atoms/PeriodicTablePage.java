package idle.ui.atoms;

import idle.core.Game;
import idle.core.elements.Element;
import idle.core.elements.ElementCategory;
import idle.core.elements.ElementEffect;
import idle.core.elements.ElementSet;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import idle.ui.GameApp;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.TileGrid;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javafx.animation.ScaleTransition;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Sous-page « Tableau périodique » de l'onglet Atomes.
 *
 * <p>Le bouton de synthèse dépense des atomes pour créer un élément tiré au sort ; son prix double à chaque synthèse,
 * jusqu'au maximum d'atomes. Le tableau montre les 118 éléments à leur place : ceux qui sont possédés sont allumés dans
 * la couleur de leur famille, avec leur nombre d'exemplaires sur le maximum possible ; un élément au maximum est en
 * couleur pleine, et les éléments uniques portent une étoile.
 *
 * <p>Chaque élément a son propre effet : on le lit en survolant sa case, ou en cliquant dessus pour l'afficher sous le
 * tableau. Plus bas, le cumul des bonus et la légende des familles avec leur chance actuelle d'être tirées.
 *
 * <p>Sous la carte de synthèse, la synthèse ciblée : un bouton par famille, avec ce qu'elle coûte en synthèses. Elle
 * n'apparaît qu'une fois achetée dans l'arbre de matière noire ({@link Game#isTargetedSynthesisUnlocked()}) ; avant,
 * une ligne dit où la trouver à qui connaît déjà la matière noire. Sous le tableau, les ensembles (familles et
 * périodes), une tuile chacun, avec leur avancement et leur bonus.
 *
 * <p>Les explications (ce que coûte une cible, comment se cumulent les exemplaires, ce que fait chaque famille)
 * n'apparaissent qu'en mode détails ({@link Detail}).
 *
 * <p>La page suit l'état du jeu, pas les clics : un élément obtenu par la synthèse automatique s'affiche exactement
 * comme un élément obtenu à la main.
 */
public final class PeriodicTablePage extends VBox {
    /**
     * Le skin dont les cases sont peintes : {@code null} pour le classique. Les styles des cases sous un
     * skin sont dans {@link TableSkinStyles} ; « Minuscule » réduit aussi les cases de moitié, « Néon » et
     * « Carte au trésor » donnent un fond au tableau entier.
     */
    private idle.core.universe.Skin tableSkin;
    /** La famille visée au dernier affichage : sur la carte au trésor, en changer repeint les îles marquées. */
    private ElementCategory shownTarget;
    /**
     * Le skin de la page hors cases : « Médailles » sur les tuiles des ensembles, « Creuset » sur la carte
     * de synthèse, « Spectre » sur la légende des familles ; dessins dans {@link TablePageSkinArt}. {@code null} : le classique.
     */
    private idle.core.universe.Skin pageSkin;
    /** Les médailles des tuiles, faites à la première demande puis gardées. */
    private final Map<ElementSet, javafx.scene.canvas.Canvas> medals = new LinkedHashMap<>();
    /** Le creuset de la carte de synthèse, fait à la première demande ; {@code null} avant. */
    private javafx.scene.canvas.Canvas crucible = null;
    /** Le temps de la fumée du creuset. */
    private double time = 0;

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

    /** La couleur d'une famille d'éléments (« #f4a6a6 ») : celle de ses cases dans le tableau. */
    public static String familyColor(ElementCategory category) { return COLORS.get(category); }

    private final Game game;
    private final Card synthesizeCard = new Card(GameApp.ATOMS_COLOR);
    private final Label resultLabel = new Label();
    private final Label progressLabel = new Label();
    /**
     * Le bouton de l'explosion, à droite de la carte de synthèse (version 0.314) : la barre violette en haut de la fenêtre
     * est partie à la 0.309. Il garde sa place, éteint, entre deux explosions.
     */
    private final Button explodeButton = new Button();
    private Runnable onExplode = () -> { };
    /** Les secondes qu'il reste pour confirmer l'explosion d'un second clic ; 0 hors confirmation. */
    private java.util.function.DoubleSupplier armed = () -> 0;
    /** Ce que l'explosion rapporte et efface, au survol du bouton. */
    private java.util.function.Supplier<String> explosionHint = () -> "";
    /**
     * Le haut de la page (version 0.314) : en grand, le décompte, la carte de synthèse, l'explosion et le dernier tirage sur
     * une ligne ; en petit ({@link #NARROW_TOP}), la carte et l'explosion côte à côte, le décompte et le tirage dessous.
     */
    private final StackPane top = new StackPane();
    private boolean narrowTop;
    /** En dessous de cette largeur, le haut de la page passe sur trois lignes. */
    static final double NARROW_TOP = 760;
    private final Label automationLabel = new Label();
    private final Label detailLabel = new Label();
    private final Label bonusLabel = new Label();
    /** Le tableau des cases. */
    private final GridPane table = new GridPane();
    private final Map<Integer, Label> cells = new HashMap<>();
    private final Map<Integer, Tooltip> tooltips = new HashMap<>();
    private final Map<ElementCategory, Label> legend = new EnumMap<>(ElementCategory.class);
    // La synthèse ciblée : « au hasard », puis une famille par bouton.
    private final Button randomButton = new Button("Au hasard");
    private final Map<ElementCategory, Button> targetButtons = new EnumMap<>(ElementCategory.class);
    private final FlowPane targets = new FlowPane(6, 6);
    private final Label targetLabel = new Label();
    // Les ensembles : une tuile chacun.
    private final Label setsTitle = new Label();
    private final Map<ElementSet, Card> setCards = new LinkedHashMap<>();
    private final TileGrid setGrid = new TileGrid(170, 5, 6);
    /** Les explications sous la légende : en mode détails seulement. */
    private final VBox legendBox = new VBox(3);
    private final Label legendHint = new Label();
    /** Le mode (détails ou non) au dernier affichage : en changer redessine ce qui ne suit que les synthèses. */
    private boolean shownDetail = false;
    /** Nombre de synthèses au dernier affichage, pour repérer une étape de synthèse ciblée. */
    private int shownSynthesisCount = 0;
    /** Dernier état affiché du tableau, pour ne le redessiner que lorsqu'il change ; {@code null} avant le premier affichage. */
    private Map<Integer, Integer> shown = null;
    /** Maximum total d'exemplaires au dernier affichage. */
    private int shownMaxTotal = -1;
    /** Élément dont le détail est affiché sous le tableau, ou {@code null}. */
    private Element selected = null;
    /** Côté actuel d'une case, en pixels. */
    private double cellSize = 36;
    /** Le filtre « utiles au prochain astre », et les éléments qu'il éclaire au dernier affichage. */
    private final Button nextFilter = new Button();
    private boolean filterOn = false;
    /** La préférence de famille graduée, et les éléments gratuits, tous les dix découverts. */
    private final javafx.scene.control.ComboBox<String> preference = new javafx.scene.control.ComboBox<>();
    /** Ce que la préférence de famille fait à ses chances, au survol de la liste (une ligne à droite avant la 0.314). */
    private final Tooltip preferenceTip = new Tooltip();
    private final javafx.scene.layout.HBox preferenceRow = new javafx.scene.layout.HBox(8);
    private boolean fillingPreference = false;
    private java.util.Set<Integer> filtered = java.util.Set.of();

    PeriodicTablePage(Game game) {
        super(10);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(12, 0, 12, 0));

        synthesizeCard.setPrefWidth(340);
        synthesizeCard.setMaxWidth(340);
        synthesizeCard.setOnAction(() -> {
            game.synthesize();
            refresh();     // c'est l'affichage qui repère l'élément obtenu, comme pour une synthèse automatique
        });
        resultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffe9c2;");
        resultLabel.setWrapText(true);
        resultLabel.setMaxWidth(260);
        resultLabel.setTooltip(new Tooltip());
        progressLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fa3b8;");
        progressLabel.setTooltip(new Tooltip());
        explodeButton.setFocusTraversable(false);
        explodeButton.setTextAlignment(TextAlignment.CENTER);
        explodeButton.setTooltip(new Tooltip());
        explodeButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 18; -fx-cursor: hand;"
                + " -fx-text-fill: #efe4ff; -fx-background-color: #2a1850; -fx-background-radius: 8; -fx-border-color: #7b4fd6;"
                + " -fx-border-radius: 8; -fx-border-width: 2;");
        explodeButton.setOnAction(event -> onExplode.run());
        Labels.centered(automationLabel, "-fx-font-size: 13px; -fx-text-fill: #9be7a8;");
        Labels.centered(detailLabel, "-fx-font-size: 13px; -fx-text-fill: #e8f4ff;");
        Labels.centered(bonusLabel, "-fx-font-size: 13px; -fx-text-fill: #ffe9c2;");

        // La synthèse ciblée : choisir une famille coûte plusieurs synthèses.
        targets.setAlignment(Pos.CENTER);
        randomButton.setFocusTraversable(false);
        randomButton.setOnAction(event -> {
            game.setSynthesisTarget(null);
            refresh();
        });
        targets.getChildren().add(randomButton);
        for (ElementCategory category : ElementCategory.values()) {
            Button button = new Button(category.label() + " ×" + category.targetTries());
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                game.setSynthesisTarget(category);
                refresh();
            });
            targetButtons.put(category, button);
            targets.getChildren().add(button);
        }
        Labels.note(targetLabel);
        // Version 0.314 : ce que coûte une famille visée, au survol des boutons (une ligne dessous avant).
        Tooltip targetsTip = new Tooltip("Viser une famille coûte le nombre de synthèses indiqué (×3 à ×10). Les premières sont "
                + "payées sans rien donner ; la dernière donne un élément de la famille, en priorité un que vous n'avez pas. "
                + "La synthèse automatique suit la cible.");
        targetsTip.setWrapText(true);
        targetsTip.setMaxWidth(420);
        Tooltip.install(targets, targetsTip);

        // Les ensembles : dix familles et sept périodes, une tuile chacun. Une tuile ne se clique pas.
        setsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 0 0 0; -fx-text-fill: #ffe9c2;");
        for (ElementSet set : game.elementSets()) {
            Card tile = new Card(set.isFamily() ? COLORS.get(set.category()) : "#ffe9c2");
            setCards.put(set, tile);
            setGrid.add(tile);
        }

        // Le tableau : sept périodes, puis les lanthanides et les actinides sur deux lignes à part.
        table.setHgap(CELL_GAP);
        table.setVgap(CELL_GAP);
        table.setAlignment(Pos.CENTER);
        for (Element element : PeriodicTable.ELEMENTS) {
            Label cell = new Label(element.symbol());
            cell.setMinSize(cellSize, cellSize);
            cell.setPrefSize(cellSize, cellSize);
            cell.setAlignment(Pos.CENTER);
            cell.setTextAlignment(TextAlignment.CENTER);
            cell.setOnMouseClicked(event -> clickCell(element));
            Tooltip tooltip = new Tooltip();
            Tooltip.install(cell, tooltip);
            cells.put(element.number(), cell);
            tooltips.put(element.number(), tooltip);
            table.add(cell, column(element.number()), row(element.number()));
        }

        // La légende : une ligne par famille, avec sa chance actuelle et ce que font ses éléments.
        legendBox.setAlignment(Pos.TOP_CENTER);
        for (ElementCategory category : ElementCategory.values()) {
            Label entry = new Label();
            entry.setStyle("-fx-font-size: 12px; -fx-text-fill: " + COLORS.get(category) + ";");
            entry.setWrapText(true);
            entry.setTextAlignment(TextAlignment.CENTER);
            legend.put(category, entry);
            legendBox.getChildren().add(entry);
        }
        legendHint.setText("Un nouvel élément rapporte toujours plus qu'un doublon : quatre exemplaires "
                + "ne comptent que pour deux, neuf pour trois. Un élément arrivé à son maximum d'exemplaires ne sort "
                + "plus : les tirages vont aux autres. Les éléments marqués ★ sont uniques : "
                + "ils ne sortent qu'une fois, et le premier obtenu débloque la synthèse automatique. "
                + "Un ensemble compte les éléments différents, pas les exemplaires : un seul de chaque suffit.");
        Labels.note(legendHint);

        // Les cases grandissent et rétrécissent avec la fenêtre ; le haut de la page aussi.
        widthProperty().addListener((observable, before, after) -> {
            fitCells(after.doubleValue());
            arrangeTop(after.doubleValue() > 0 && after.doubleValue() < NARROW_TOP);
        });
        arrangeTop(false);

        getChildren().add(top);
        getChildren().add(automationLabel);
        getChildren().add(targets);
        getChildren().add(targetLabel);
        nextFilter.setFocusTraversable(false);
        nextFilter.setOnAction(event -> {
            filterOn = !filterOn;
            filtered = filterOn ? game.elementsForNextStep() : java.util.Set.of();
            paintCells();
            refresh();
        });
        // La préférence de famille : une liste, « aucune » d'abord.
        preference.getItems().add("Aucune");
        for (ElementCategory category : ElementCategory.values()) preference.getItems().add(category.label());
        preference.setValue("Aucune");
        preference.setVisibleRowCount(ElementCategory.values().length + 1);
        preference.setFocusTraversable(false);
        preference.setStyle("-fx-font-size: 12px;");
        preference.setOnAction(event -> {
            if (fillingPreference) return;
            int index = preference.getItems().indexOf(preference.getValue()) - 1;
            game.setPreferredFamily(index < 0 ? null : ElementCategory.values()[index]);
            refresh();
        });
        Label preferenceTitle = new Label("Famille préférée :");
        preferenceTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffe9c2;");
        preference.setTooltip(preferenceTip);
        preferenceRow.setAlignment(Pos.CENTER);
        // Version 0.314 : le filtre « utiles au prochain astre » à droite de la préférence, sur la même ligne.
        preferenceRow.getChildren().addAll(preferenceTitle, preference, nextFilter);
        getChildren().add(preferenceRow);
        getChildren().add(table);
        getChildren().add(detailLabel);
        getChildren().add(bonusLabel);
        getChildren().add(setsTitle);
        getChildren().add(setGrid);
        getChildren().add(legendBox);
        getChildren().add(legendHint);
    }

    /** Recopie l'état du jeu dans la carte de synthèse, le tableau, le résumé des bonus et la légende. */
    public void refresh() { refresh(true); }

    /**
     * Recopie l'état du jeu dans la page.
     *
     * @param visible faux quand la page n'est pas à l'écran : les textes qui se recopient tels quels à chaque image
     * attendent alors qu'elle y revienne. Ce qui suit les changements du tableau, lui, ne s'arrête jamais : l'annonce
     * du dernier élément obtenu, l'étape payée d'une synthèse ciblée, les tuiles. La page dit ainsi la même chose au
     * retour que si elle était restée affichée.
     */
    void refresh(boolean visible) {
        // Le skin du tableau : les cases se redessinent quand il change.
        idle.core.universe.Skin look = game.chosenSkin(idle.core.universe.SkinVisual.TABLE);
        boolean repaint = look != tableSkin;
        if (repaint) {
            tableSkin = look;
            table.setStyle(look == null ? "" : TableSkinStyles.tableStyle(look));
            double gap = look != null && TableSkinStyles.compact(look) ? CELL_GAP / 2 : CELL_GAP;
            table.setHgap(gap);
            table.setVgap(gap);
            sizeCells();
        }
        // Le skin de la page hors cases : ses ornements se posent ou se retirent quand il change.
        idle.core.universe.Skin page = game.chosenSkin(idle.core.universe.SkinVisual.TABLE_PAGE);
        if (page != pageSkin) {
            pageSkin = page;
            dressPage();
        }
        ElementCategory target = game.synthesisTarget();
        // Sur la carte au trésor, les îles de la famille visée portent une croix : changer de cible les repeint.
        if (tableSkin == idle.core.universe.Skin.TABLE_MAP && target != shownTarget) repaint = true;
        shownTarget = target;
        if (repaint) paintCells();
        int left = game.synthesisTriesLeft();
        int count = game.state().synthesisCount();
        if (visible) refreshSynthesis(target, left, count);

        // Une étape de synthèse ciblée a été payée sans rien donner : on le dit, sinon le clic semble perdu.
        if (count > shownSynthesisCount && target != null && game.state().elements().equals(shown)) {
            resultLabel.getTooltip().setText("");
            resultLabel.setText("Étape payée : encore " + left + (left > 1 ? " synthèses" : " synthèse")
                    + " avant l'élément visé (" + target.label().toLowerCase(Locale.ROOT) + ").");
            resultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLORS.get(target) + ";");
        }
        shownSynthesisCount = count;

        if (visible) {
            refreshProgress();
            refreshExplosion();
            String automation = automationStatus();
            automationLabel.setText(automation);
            automationLabel.setVisible(!automation.isEmpty());
            automationLabel.setManaged(!automation.isEmpty());
            legendBox.setVisible(Detail.shown());
            legendBox.setManaged(Detail.shown());
            legendHint.setVisible(Detail.shown());
            legendHint.setManaged(Detail.shown());
        }
        refreshTable();
    }

    /**
     * Pose ou retire les ornements du skin de la page : les médailles des tuiles, le creuset de la carte de
     * synthèse, les bandes de spectre de la légende. Sans skin, la légende reprend son style de toujours, au caractère près.
     */
    private void dressPage() {
        boolean medalled = pageSkin == idle.core.universe.Skin.PAGE_MEDALS;
        setCards.forEach((set, tile) -> {
            if (medalled && !medals.containsKey(set)) medals.put(set, new javafx.scene.canvas.Canvas(TablePageSkinArt.MEDAL_SIZE, TablePageSkinArt.MEDAL_SIZE));
            tile.setInlinePicture(medalled ? medals.get(set) : null);
        });
        if (medalled) paintMedals();
        boolean fuming = pageSkin == idle.core.universe.Skin.PAGE_CRUCIBLE;
        if (fuming && crucible == null) crucible = new javafx.scene.canvas.Canvas(TablePageSkinArt.CRUCIBLE_WIDTH, TablePageSkinArt.CRUCIBLE_HEIGHT);
        synthesizeCard.setPicture(fuming ? crucible : null);
        if (fuming) TablePageSkinArt.drawCrucible(crucible, time, game.canSynthesize());
        boolean spectrum = pageSkin == idle.core.universe.Skin.PAGE_SPECTRUM;
        legend.forEach((category, entry) -> entry.setStyle(spectrum ? TablePageSkinArt.spectrumStyle(COLORS.get(category))
                : "-fx-font-size: 12px; -fx-text-fill: " + COLORS.get(category) + ";"));
    }

    /** Dessine la médaille de chaque tuile d'après l'avancement de son ensemble. */
    private void paintMedals() { medals.forEach((set, medal) -> TablePageSkinArt.drawMedal(medal, medalTier(set))); }

    /** Le palier de la médaille d'un ensemble : 0 rien, 1 bronze dès un élément, 2 argent à la moitié, 3 or complet. */
    int medalTier(ElementSet set) {
        double strength = game.setStrength(set);
        return strength >= 1 ? 3 : strength > 0 ? 2 : game.setProgress(set) > 0 ? 1 : 0;
    }

    /** Fait fumer le creuset : à appeler à chaque image quand la page est à l'écran ; rien sous les autres skins. */
    public void frame(double dt) {
        if (pageSkin != idle.core.universe.Skin.PAGE_CRUCIBLE || crucible == null) return;
        time += Math.min(dt, 0.1);
        TablePageSkinArt.drawCrucible(crucible, time, game.canSynthesize());
    }

    /** Le creuset de la carte de synthèse, ou {@code null} hors du skin « Creuset » : pour les vérifications. */
    javafx.scene.Node crucible() { return pageSkin == idle.core.universe.Skin.PAGE_CRUCIBLE ? crucible : null; }

    /** Vrai si les tuiles portent une médaille : pour les vérifications. */
    boolean medalled() { return pageSkin == idle.core.universe.Skin.PAGE_MEDALS; }

    /** Le style de la ligne de la légende de cette famille : pour les vérifications. */
    String legendStyle(ElementCategory category) { return legend.get(category).getStyle(); }

    /** La carte de synthèse et la rangée de la synthèse ciblée : ce qui se recopie à chaque image. */
    private void refreshSynthesis(ElementCategory target, int left, int count) {
        BigNum cost = game.synthesisCost();
        String price = Format.count(cost) + (cost.gt(BigNum.ONE) ? " atomes" : " atome");
        if (game.isPeriodicTableComplete()) {
            synthesizeCard.show(Card.State.DONE, "", "complet", "Tableau périodique complet",
                    "Tous les éléments sont au maximum", "", "", "");
        } else {
            // Avec une cible, la carte dit où en est la synthèse ciblée : les premières étapes ne donnent rien.
            long fusions = game.fusionsUntilAtoms(cost);
            synthesizeCard.show(game.canSynthesize() ? Card.State.READY : Card.State.WAITING,
                    String.valueOf(count + 1), "",
                    target == null ? "Synthétiser un élément" : "Synthèse ciblée",
                    target == null ? game.isNextSynthesisNew() ? "Un élément nouveau, à coup sûr" : "Un élément tiré au sort"
                            : left > 1 ? "Étape " + (target.targetTries() - left + 1) + " sur " + target.targetTries()
                                    + ", sans élément"
                            : "Un élément parmi les " + target.label().toLowerCase(Locale.ROOT),
                    "Chaque synthèse dépense des atomes et double le prix de la suivante, jusqu'à "
                            + Format.count(game.atomCap()) + " atomes"
                            + (game.tableWeight().gt(BigNum.ONE) ? " (tableau " + Format.multiplier(game.tableWeight())
                                    + " plus lourd qu'au début)" : "")
                            + ". Les familles rares sortent moins souvent : leurs chances sont sous le tableau."
                            + " Après " + Game.LUCK_FLOOR + " synthèses sans rien de nouveau, la suivante donne un élément inconnu.",
                    price, fusions > 0 ? Format.whole(fusions) + (fusions > 1 ? " fusions" : " fusion") : "");
        }
        refreshTargets(target, left);
    }

    /** Les tuiles, les bonus et l'annonce du dernier élément obtenu : seulement quand le tableau a changé. */
    /**
     * Un clic sur une case : avec un élément gratuit à prendre, un élément pas encore
     * découvert est pris ; sinon, son détail s'affiche sous le tableau.
     */
    private void clickCell(Element element) {
        if (game.freeElementsAvailable() > 0 && game.elementCount(element.number()) == 0 && game.claimFreeElement(element.number())) {
            resultLabel.getTooltip().setText("");
            resultLabel.setText("Élément gratuit : " + element.name() + " (" + element.symbol() + ").");
            refresh();
            return;
        }
        selected = element;
        showDetail();
    }

    /** La préférence et les éléments gratuits : cachés hors du deuxième univers. */
    private void refreshSecondUniverse() {
        boolean open = game.isFamilyPreferenceUnlocked();
        preferenceRow.setVisible(open);
        preferenceRow.setManaged(open);
        if (open) {
            ElementCategory preferred = game.preferredFamily();
            String wanted = preferred == null ? "Aucune" : preferred.label();
            if (!wanted.equals(preference.getValue())) {
                fillingPreference = true;
                preference.setValue(wanted);
                fillingPreference = false;
            }
            preferenceTip.setText(preferred == null ? "Ses chances montent à chaque tirage au hasard, ×2 au plus."
                    : "Chances ×" + String.format(java.util.Locale.ROOT, "%.2f", game.preferenceMultiplier(preferred))
                            + " (+" + Math.round(Game.PREFERENCE_STEP * 100) + " % par tirage au hasard, ×2 au plus ; l'explosion la remet à ×1).");
        }
    }

    /**
     * Le décompte, à gauche de la carte de synthèse (version 0.314) : éléments, exemplaires, tirage double, et au deuxième
     * univers les éléments gratuits à prendre ; une ligne chacun en grand, une seule ligne en petit. Le survol dit comment
     * on gagne et on prend les éléments gratuits (c'était une ligne sous la préférence de famille).
     */
    private void refreshProgress() {
        int doubleDraw = (int) Math.round(game.doubleDrawChance() * 100);
        int free = game.freeElementsAvailable();
        List<String> parts = new ArrayList<>(List.of(game.discoveredElements() + " / " + PeriodicTable.ELEMENTS.size() + " éléments",
                game.ownedCopies() + " / " + game.maxTotalCopies() + " exemplaires"));
        if (doubleDraw > 0) parts.add("tirage double " + doubleDraw + " %");
        boolean second = game.isSecondUniverse();
        if (second && free > 0) parts.add(free + (free > 1 ? " éléments gratuits" : " élément gratuit"));
        String text = String.join(narrowTop ? "   |   " : "\n", parts);
        if (!text.equals(progressLabel.getText())) progressLabel.setText(text);
        progressLabel.getTooltip().setText(!second ? "Les éléments découverts, les exemplaires obtenus sur le maximum, et la chance "
                + "qu'une synthèse donne deux éléments."
                : free > 0 ? "Cliquez un élément pas encore découvert pour le prendre gratuitement."
                : "Éléments gratuits : un tous les " + Game.FREE_ELEMENT_EVERY + " éléments découverts (prochain à "
                        + ((game.discoveredElements() / Game.FREE_ELEMENT_EVERY + 1) * Game.FREE_ELEMENT_EVERY) + ").");
    }

    private void refreshTable() {
        refreshSecondUniverse();
        // Le filtre du prochain astre : caché avant le premier Big Bang, et repeint quand ce qu'il éclaire change.
        boolean filter = game.isNextBodyFilterUnlocked();
        nextFilter.setVisible(filter);
        nextFilter.setManaged(filter);
        if (!filter) filterOn = false;
        java.util.Set<Integer> now = filterOn ? game.elementsForNextStep() : java.util.Set.of();
        nextFilter.setText(filterOn ? "Utiles au prochain astre : " + now.size() + (now.size() > 1 ? " éléments éclairés" : " élément éclairé")
                + " (tout montrer)" : "Montrer les éléments utiles au prochain astre");
        nextFilter.setStyle("-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + (filterOn ? " -fx-text-fill: #10151f; -fx-background-color: #ffd27f;"
                        : " -fx-text-fill: #ffd27f; -fx-background-color: #141a26; -fx-border-color: #ffd27f88;"));
        if (!now.equals(filtered)) {
            filtered = now;
            paintCells();
        }
        // Le tableau et les bonus ne changent qu'à la synthèse : inutile de tout réécrire à chaque image.
        // L'arbre de matière noire peut aussi repousser le maximum d'exemplaires : il faut alors repeindre.
        // Passer en mode détails, ou en sortir, change aussi les textes des tuiles.
        Map<Integer, Integer> owned = game.state().elements();
        int maxTotal = game.maxTotalCopies();
        if (owned.equals(shown) && maxTotal == shownMaxTotal && Detail.shown() == shownDetail) return;
        shownMaxTotal = maxTotal;
        shownDetail = Detail.shown();
        if (shown != null) announce(owned);
        // Le tableau vidé par une explosion : plus de dernier tirage.
        if (owned.isEmpty()) {
            resultLabel.setText("");
            resultLabel.getTooltip().setText("");
        }
        shown = new HashMap<>(owned);

        paintCells();
        showDetail();
        bonusLabel.setText(bonusSummary());
        refreshSets();
        if (pageSkin == idle.core.universe.Skin.PAGE_MEDALS) paintMedals();
        for (ElementCategory category : ElementCategory.values()) {
            double chance = game.categoryChance(category);
            legend.get(category).setText("■ " + category.label() + " ("
                    + (chance > 0 ? percent(chance) : "complet")
                    + (category.unique() ? "" : ", " + game.maxCopiesOf(category) + " exemplaires au plus")
                    + ") : " + category.description());
        }
    }

    /**
     * La synthèse ciblée : cachée tant qu'elle n'est pas achetée dans l'arbre de matière noire.
     * Achetée, la rangée éclaire la famille visée, éteint celles qui n'ont plus rien à donner, et
     * chaque bouton rappelle la chance de sa famille quand on tire au hasard.
     */
    private void refreshTargets(ElementCategory target, int left) {
        boolean unlocked = game.isTargetedSynthesisUnlocked();
        targets.setVisible(unlocked);
        targets.setManaged(unlocked);
        // Avant l'achat, la ligne ne parle de la synthèse ciblée qu'à qui connaît déjà la matière noire.
        boolean hint = unlocked || game.isDarkMatterUnlocked();
        targetLabel.setVisible(hint);
        targetLabel.setManaged(hint);
        if (!unlocked) {
            targetLabel.setText("Synthèse ciblée : à acheter dans l'arbre de matière noire"
                    + Detail.only(" (branche matière noire). Elle permet de choisir la famille de l'élément à venir, "
                            + "contre plusieurs synthèses."));
            return;
        }
        String base = "-fx-font-size: 11px; -fx-padding: 3 8; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;";
        randomButton.setStyle(base + (target == null
                ? " -fx-text-fill: #10151f; -fx-background-color: #ffe9c2; -fx-border-color: #ffffff;"
                : " -fx-text-fill: #ffe9c2; -fx-background-color: #16202e; -fx-border-color: #6b5a33;"));
        targetButtons.forEach((category, button) -> {
            String color = COLORS.get(category);
            double chance = game.categoryChance(category);
            button.setText(category.label() + " ×" + category.targetTries()
                    + (Detail.shown() ? chance > 0 ? "  (" + percent(chance) + " au hasard)" : "  (complet)" : ""));
            button.setStyle(base + (category == target
                    ? " -fx-text-fill: #10151f; -fx-background-color: " + color + "; -fx-border-color: #ffffff;"
                    : " -fx-text-fill: " + color + "; -fx-background-color: #16202e; -fx-border-color: " + color + "66;"));
            button.setDisable(chance <= 0);   // famille complète : plus rien à viser
        });
        // Sans cible, rien sous les boutons : ce qu'elle coûte se lit à leur survol (version 0.314).
        targetLabel.setVisible(target != null);
        targetLabel.setManaged(target != null);
        targetLabel.setText(target == null ? ""
                : "Cible : " + target.label().toLowerCase(Locale.ROOT) + ", encore " + left
                        + (left > 1 ? " synthèses" : " synthèse")
                        + Detail.only(". Chaque élément visé coûte " + target.targetTries() + " synthèses ; un élément "
                                + "que vous n'avez pas sort en priorité."));
    }

    /** Remplit une tuile par ensemble : avancement, bonus, et ce qui en est déjà actif. */
    private void refreshSets() {
        // Version 0.314 : les complets seuls ; les tuiles disent le reste.
        setsTitle.setText("Ensembles : " + game.completedSets() + (game.completedSets() > 1 ? " complets" : " complet"));
        setCards.forEach((set, tile) -> {
            int found = game.setProgress(set);
            int size = set.members().size();
            double strength = game.setStrength(set);
            boolean hasHalf = set.halfSize() < size;
            String state = strength >= 1 ? "Complet : le bonus est entier."
                    : strength > 0 ? "À moitié : " + ElementText.percent(strength) + " du bonus est actif, le reste en le complétant."
                    : hasHalf ? ElementText.percent(ElementSet.HALF_STRENGTH) + " du bonus à " + set.halfSize()
                            + " éléments différents, le tout à " + size + "."
                    : "Le bonus vient en réunissant ses " + size + " éléments.";
            tile.show(strength >= 1 ? Card.State.DONE : strength > 0 ? Card.State.STARTED
                            : found > 0 ? Card.State.WAITING : Card.State.LOCKED,
                    found + "/" + size, strength >= 1 ? "complet" : strength > 0 ? "à moitié" : "",
                    set.name(), ElementText.describe(set.effect()), state, "", "");
        });
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
        sizeCells();
        paintCells();    // la taille du texte suit celle de la case
    }

    /** Donne à chaque case son côté : {@link #cellSize}, la moitié sous le skin « Minuscule ». */
    private void sizeCells() {
        double size = shownCellSize();
        for (Label cell : cells.values()) {
            cell.setMinSize(size, size);
            cell.setPrefSize(size, size);
        }
    }

    /** Le côté des cases à l'écran, en pixels : pour les vérifications. */
    double shownCellSize() { return tableSkin != null && TableSkinStyles.compact(tableSkin) ? cellSize / 2 : cellSize; }

    /** Le style de la case de l'élément de ce numéro : pour les vérifications. */
    String cellStyle(int number) { return cells.get(number).getStyle(); }

    /** Le texte de la case de l'élément de ce numéro : pour les vérifications. */
    String cellText(int number) { return cells.get(number).getText(); }

    /** Le style du fond du tableau : pour les vérifications. */
    String tableStyle() { return table.getStyle(); }

    /** Écrit et colore chaque case d'après ce que le joueur possède. */
    private void paintCells() {
        String font = "-fx-font-size: " + Math.max(8, Math.round(cellSize * 0.31)) + "px; ";
        boolean compact = tableSkin != null && TableSkinStyles.compact(tableSkin);
        boolean treasure = tableSkin == idle.core.universe.Skin.TABLE_MAP;
        ElementCategory target = treasure ? game.synthesisTarget() : null;
        for (Element element : PeriodicTable.ELEMENTS) {
            int copies = game.elementCount(element.number());
            int max = game.maxCopiesOf(element);
            boolean unique = element.category().unique();
            boolean maxed = game.isElementMaxed(element.number());
            String color = COLORS.get(element.category());
            Label cell = cells.get(element.number());
            cell.setText(compact ? element.symbol()     // minuscule : rien que les lettres
                    : treasure && maxed && !unique ? element.symbol() + "\n✕"     // le trésor déterré
                    : unique ? element.symbol() + "\n★"
                    : copies > 0 ? element.symbol() + "\n" + Math.min(copies, max) + "/" + max : element.symbol());
            // Pas encore obtenu : éteint. Possédé : allumé. Au maximum : couleur pleine.
            if (tableSkin != null) {
                String skinFont = compact ? "-fx-font-size: " + Math.max(7, Math.round(cellSize * 0.2)) + "px; " : font;
                boolean spot = treasure && copies == 0 && element.category() == target;
                cell.setStyle(skinFont + TableSkinStyles.cellStyle(tableSkin, color, copies > 0, maxed, spot)
                        + (!filterOn ? "" : filtered.contains(element.number()) ? " -fx-border-color: #ffd27f; -fx-border-width: 2;"
                                : " -fx-opacity: 0.3;"));
                tooltips.get(element.number()).setText(detail(element, "\n"));
                continue;
            }
            cell.setStyle(font + CELL_STYLE + (maxed
                    ? " -fx-text-fill: #10151f; -fx-background-color: " + color + "; -fx-border-color: #ffffff;"
                    : copies > 0
                    ? " -fx-text-fill: #ffffff; -fx-background-color: " + color + "55; -fx-border-color: " + color + ";"
                    : " -fx-text-fill: " + color + "88; -fx-background-color: #10151f; -fx-border-color: " + color + "44;")
                    // Le filtre du prochain astre : ce qu'il demande cerclé d'or, le reste estompé.
                    + (!filterOn ? "" : filtered.contains(element.number()) ? " -fx-border-color: #ffd27f; -fx-border-width: 2;"
                            : " -fx-opacity: 0.3;"));
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
            resultLabel.getTooltip().setText("");
            resultLabel.setText(gained.size() + " éléments obtenus");
            resultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffe9c2;");
            return;
        }
        StringBuilder text = new StringBuilder(gained.size() == 2 ? "Tirage double ! " : "");
        StringBuilder effects = new StringBuilder();
        for (Element element : gained) {
            int copies = owned.get(element.number());
            if (text.length() > 0 && element != gained.get(0)) text.append("\n");
            // Version 0.314 : l'élément seul ; son effet au survol.
            text.append(copies == 1 ? "Nouvel élément : " : "Exemplaire n° " + copies + " : ")
                    .append(element.name()).append(" (").append(element.symbol()).append(")");
            effects.append(effects.length() == 0 ? "" : "\n").append(element.name()).append(" : ").append(ElementText.describe(element.effect()));

            ScaleTransition pop = new ScaleTransition(Duration.seconds(0.5), cells.get(element.number()));
            pop.setFromX(2.2);
            pop.setFromY(2.2);
            pop.setToX(1);
            pop.setToY(1);
            pop.play();
        }
        resultLabel.setText(text.toString());
        resultLabel.getTooltip().setText(effects.toString());
        resultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLORS.get(gained.get(0).category()) + ";");
    }

    /**
     * Range le haut de la page : en grand, le décompte, la carte de synthèse, l'explosion et le dernier tirage sur une ligne ;
     * en petit, la carte et l'explosion côte à côte, à la même hauteur, le décompte et le tirage dessous.
     */
    private void arrangeTop(boolean narrow) {
        if (narrow == narrowTop && !top.getChildren().isEmpty()) return;
        narrowTop = narrow;
        progressLabel.setAlignment(narrow ? Pos.CENTER : Pos.CENTER_RIGHT);
        progressLabel.setTextAlignment(narrow ? TextAlignment.CENTER : TextAlignment.RIGHT);
        resultLabel.setTextAlignment(narrow ? TextAlignment.CENTER : TextAlignment.LEFT);
        synthesizeCard.setPrefWidth(narrow ? 260 : 340);
        synthesizeCard.setMaxWidth(narrow ? 260 : 340);
        HBox boxes = new HBox(14, synthesizeCard, explodeButton);
        boxes.setAlignment(Pos.CENTER);
        javafx.scene.Parent row;
        if (narrow) {
            VBox column = new VBox(6, boxes, progressLabel, resultLabel);
            column.setAlignment(Pos.CENTER);
            row = column;
        } else {
            HBox line = new HBox(18, progressLabel, boxes, resultLabel);
            line.setAlignment(Pos.CENTER);
            row = line;
        }
        top.getChildren().setAll(row);
        if (game != null) refreshProgress();
    }

    /** L'explosion : ce qu'elle fait au clic, ce que dure la confirmation, et ce qu'elle rapporte au survol. */
    public void setExplosion(Runnable action, java.util.function.DoubleSupplier armedSeconds, java.util.function.Supplier<String> hint) {
        onExplode = action;
        armed = armedSeconds;
        explosionHint = hint;
    }

    /**
     * Le bouton de l'explosion : allumé quand le tableau peut exploser ; sinon il garde sa place, éteint, une fois la matière
     * noire ouverte, et n'existe pas avant. Le second clic de confirmation s'y lit.
     */
    private void refreshExplosion() {
        boolean can = game.canExplode();
        boolean place = can || game.isDarkMatterUnlocked();
        explodeButton.setVisible(can);
        explodeButton.setManaged(place);
        if (!can) return;
        double seconds = armed.getAsDouble();
        String text = seconds > 0 ? "Confirmer l'explosion\n(" + (int) Math.ceil(seconds) + " s)"
                : "Explosion du tableau\n+" + Format.darkMatter(game.nextExplosionDarkMatter()) + " matière noire";
        if (!text.equals(explodeButton.getText())) explodeButton.setText(text);
        String hint = explosionHint.get();
        if (!hint.equals(explodeButton.getTooltip().getText())) explodeButton.getTooltip().setText(hint);
    }

    /** Le bouton de l'explosion : pour les vérifications. */
    Button explodeButton() { return explodeButton; }

    /** Le décompte, à gauche de la carte : pour les vérifications. */
    String progressText() { return progressLabel.getText(); }

    /** Où en est la synthèse automatique : verrouillée, à acheter, en marche ou coupée. */
    private String automationStatus() {
        Automation synthesis = null;
        for (Automation automation : game.automations()) {
            if (automation.kind() == Automation.Kind.SYNTHESIS) synthesis = automation;
        }
        if (synthesis == null) return "";
        // Le tableau peut exploser : le bouton est à droite de la carte de synthèse (version 0.314), rien à ajouter.
        if (game.isPeriodicTableComplete() || game.canExplode()) return "";
        if (!game.isSynthesisAutomationUnlocked()) {
            int left = game.guaranteedUniqueSynthesis() - game.state().synthesisCount();
            return "Synthèse automatique : avec le premier élément unique ★"
                    + Detail.only(" (gaz noble ou actinide)"
                            + (left > 1 ? ", garanti au plus tard dans " + left + " synthèses"
                                    : ", garanti à la prochaine synthèse"));
        }
        if (!game.ownsAutomation(synthesis.id())) {
            return "Synthèse automatique débloquée : " + Format.count(synthesis.cost()) + " atomes, onglet Automatisation";
        }
        return game.isAutomationEnabled(synthesis.id())
                ? "Synthèse automatique en marche, une toutes les "
                        + ElementText.number(game.automationInterval(synthesis.id())) + " s"
                : "Synthèse automatique coupée";
    }

    /** Écrit le détail de l'élément sélectionné sous le tableau. */
    private void showDetail() {
        if (selected == null) {
            // Version 0.314 : sans élément choisi, rien sous le tableau (« Survolez ou cliquez un élément… » avant).
            detailLabel.setText("");
            detailLabel.setVisible(false);
            detailLabel.setManaged(false);
            return;
        }
        detailLabel.setVisible(true);
        detailLabel.setManaged(true);
        detailLabel.setText(detail(selected, "   |   "));
        detailLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLORS.get(selected.category()) + ";");
    }

    /** Tout ce qu'il y a à savoir sur un élément : nom, famille, effet, exemplaires. */
    private String detail(Element element, String separator) {
        int copies = game.elementCount(element.number());
        String stacking = ElementText.stacking(element.effect(), copies);
        int max = game.maxCopiesOf(element);
        String owned = element.category().unique() ? (copies == 0 ? "Pas encore obtenu" : "Obtenu")
                : (copies == 0 ? "Pas encore obtenu, " + max + " exemplaires au plus"
                        : Math.min(copies, max) + " / " + max + (max > 1 ? " exemplaires" : " exemplaire")
                                + (copies >= max ? " (maximum)" : ""))
                        + (stacking.isEmpty() ? "" : " : " + stacking);
        // Un plafond relevé par des molécules : on dit de combien, pour qu'un maximum de 11 ne surprenne pas.
        int raised = game.elementUncap(element.number());
        if (raised > 0) owned += separator + "Maximum relevé de " + raised + " par ses molécules";
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

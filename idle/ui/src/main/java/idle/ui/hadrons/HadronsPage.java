package idle.ui.hadrons;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.core.collider.GluonMilestone;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import idle.ui.Readiness;
import idle.ui.common.CanvasPane;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.SubTabs;
import idle.ui.common.TileGrid;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * L'onglet des hadrons (version 0.149) : les gluons y lient les quarks du collisionneur en hadrons. Il se propose dans
 * le collisionneur avec la bulle de l'arbuste, au milieu, et s'y paie en énergie, comme le vide ; la bulle du jeune
 * arbre, à gauche, l'ouvre dès le début de l'univers.
 *
 * <p>En tête, sur les trois sous-onglets, les gluons (un par quark lié) et les paliers atteints. Puis :
 * <ul>
 *   <li>« Gluons » : les quarks en main ; les nucléons (proton, neutron), et les particules exotiques, que la bulle du
 *       cœur de l'arbre-monde ouvre. Chaque hadron a sa carte (nom, formule, phrase, recette, nombre possédé), sous elle
 *       son bonus (version 0.151) — ce qu'il vaut, ce qu'il vaudrait avec un de plus et avec « Lier tout » — et deux
 *       boutons, « Lier 1 » et « Lier tout » ;</li>
 *   <li>« Paliers » (version 0.151) : les cinq paliers de gluons, ce qu'ils font garder, atteints ou ce qu'il manque ;</li>
 *   <li>« Liaison » (version 0.151) : la vue dessinée ({@link BindingView}) où l'on voit les quarks se lier, et dessous
 *       un bouton par hadron ouvert, qui en lie un.</li>
 * </ul>
 *
 * <p>Les cartes et les boutons ne sont construits qu'une fois ; leurs textes ne se refont que si les quarks, les
 * hadrons ou l'affichage ont changé.
 */
public final class HadronsPage extends VBox {

    /** La couleur des hadrons : le rouge d'une charge de couleur, celle que portent les quarks liés par les gluons. */
    public static final String COLOR = "#ff8a7a";
    /** Les saveurs qui se gardent en main : le top se désintègre avant d'être lié. */
    static final Quark[] KEPT = {Quark.UP, Quark.DOWN, Quark.STRANGE, Quark.CHARM, Quark.BOTTOM};
    private static final String HEADING_STYLE = "-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 12 0 2 0; -fx-text-fill: " + COLOR + ";";
    private static final String BUTTON_STYLE = "-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-background-color: #24120f; -fx-text-fill: " + COLOR + "; -fx-border-color: " + COLOR + ";";
    private static final double MAX_WIDTH = 860;
    /** La largeur de la barre d'un palier de gluons. */
    private static final double GAUGE_WIDTH = 320;

    private final Game game;
    /** Les sous-onglets : les gluons, les paliers (version 0.151), la liaison (version 0.151). */
    public static final int GLUONS = 0, MILESTONES = 1, BINDING = 2;
    private final Button gluonsTab = new Button("Gluons");
    private final Button milestonesTab = new Button("Paliers");
    private final Button bindingTab = new Button("Liaison");
    private final HBox subTabs = new HBox(gluonsTab, milestonesTab, bindingTab);
    private final VBox[] panes = new VBox[3];
    private int selected = GLUONS;
    /** En tête des trois sous-onglets : les gluons et les paliers atteints. */
    private final Label gluonsLine = new Label();
    private final Map<Quark, Label> quarks = new EnumMap<>(Quark.class);
    private final Label effectNote = new Label();
    private final Label exoticNote = new Label();
    private final TileGrid exoticGrid = new TileGrid(250, 3, 10);
    private final Map<Hadron, Card> cards = new EnumMap<>(Hadron.class);
    private final Map<Hadron, Label> bonuses = new EnumMap<>(Hadron.class);
    private final Map<Hadron, Button> bindOne = new EnumMap<>(Hadron.class);
    private final Map<Hadron, Button> bindAll = new EnumMap<>(Hadron.class);
    private final Map<GluonMilestone, Card> milestoneCards = new EnumMap<>(GluonMilestone.class);
    private final Map<GluonMilestone, Region> gauges = new EnumMap<>(GluonMilestone.class);
    private final Label milestonesNote = new Label();
    private final BindingView view;
    private final Map<Hadron, Button> bindHere = new EnumMap<>(Hadron.class);
    private final Label bindingNote = new Label();
    /** Ce que la page montre (quarks, hadrons, ce qui se lie, affichage) : ses textes ne se refont que si cela change. */
    private String shownKey = "";

    public HadronsPage(Game game) {
        super(10);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(14, 16, 14, 16));
        gluonsLine.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");

        HBox quarkRow = new HBox(18);
        quarkRow.setAlignment(Pos.CENTER);
        for (Quark quark : KEPT) {
            Label label = new Label();
            label.setStyle("-fx-font-size: 14px; -fx-text-fill: #e8f4ff;");
            quarks.put(quark, label);
            quarkRow.getChildren().add(label);
        }
        Label quarksTitle = new Label("Quarks en main");
        quarksTitle.setStyle(HEADING_STYLE);

        Labels.note(effectNote, MAX_WIDTH);
        Labels.note(exoticNote, MAX_WIDTH);
        exoticNote.setText("La bulle du cœur de l'arbre-monde les ouvre : mésons, un quark et un antiquark, et baryons étranges.");

        TileGrid nucleonGrid = new TileGrid(250, 3, 10);
        for (Hadron hadron : Hadron.values()) (hadron.isExotic() ? exoticGrid : nucleonGrid).add(tile(hadron));
        nucleonGrid.setMaxWidth(MAX_WIDTH);
        exoticGrid.setMaxWidth(MAX_WIDTH);

        VBox gluons = new VBox(8, quarksTitle, quarkRow, effectNote, heading("Nucléons"), nucleonGrid,
                heading("Particules exotiques"), exoticNote, exoticGrid);
        gluons.setAlignment(Pos.TOP_CENTER);
        panes[GLUONS] = scrolled(gluons);

        // « Paliers » : une carte par palier, avec sa barre.
        Labels.note(milestonesNote, MAX_WIDTH);
        milestonesNote.setText("Chaque quark lié dans un hadron compte un gluon. Un palier atteint agit aussitôt, sans rien "
                + "payer : il fait garder quelque chose à l'explosion ou au Big Bang, en plus de ce que l'arbre de matière "
                + "noire et les paliers du Big Bang gardent déjà. Les gluons suivent les hadrons qu'on a : un nouvel univers "
                + "les reprend avec eux, sauf avec la bulle de l'arbre ancien, à gauche.");
        VBox milestones = new VBox(10, milestonesNote);
        milestones.setAlignment(Pos.TOP_CENTER);
        for (GluonMilestone milestone : GluonMilestone.values()) milestones.getChildren().add(buildMilestone(milestone));
        panes[MILESTONES] = scrolled(milestones);

        // « Liaison » : la vue, et un bouton par hadron ouvert.
        view = new BindingView(game);
        CanvasPane drawing = CanvasPane.filling(view);
        drawing.setMinHeight(220);
        drawing.setPrefHeight(340);
        drawing.setMaxWidth(MAX_WIDTH + 40);
        VBox.setVgrow(drawing, Priority.ALWAYS);
        FlowPane buttons = new FlowPane(8, 8);
        buttons.setAlignment(Pos.CENTER);
        buttons.setMaxWidth(MAX_WIDTH);
        for (Hadron hadron : Hadron.values()) {
            Button button = button("Lier : " + hadron.label() + " (" + hadron.formula() + ")");
            button.setOnAction(event -> bind(hadron, false));
            bindHere.put(hadron, button);
            buttons.getChildren().add(button);
        }
        Labels.note(bindingNote, MAX_WIDTH);
        bindingNote.setText("Les gluons à l'œuvre : les quarks en main flottent, chacun de la couleur de sa saveur (un "
                + "anneau pour un antiquark). Un hadron lié, ici ou dans « Gluons », se joue ici : ses quarks se rapprochent, "
                + "les gluons les relient, et il rejoint la rangée des hadrons formés.");
        panes[BINDING] = new VBox(10, drawing, buttons, bindingNote);
        panes[BINDING].setAlignment(Pos.TOP_CENTER);

        StackPane pages = new StackPane(panes);
        pages.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(pages, Priority.ALWAYS);

        SubTabs.wire(true, this::select, this::refresh, gluonsTab, milestonesTab, bindingTab);
        subTabs.setAlignment(Pos.CENTER);
        getChildren().addAll(subTabs, gluonsLine, pages);
        select(GLUONS);
        refresh();
    }

    private static VBox scrolled(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        Labels.transparent(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return new VBox(scroll);
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.setStyle(HEADING_STYLE);
        return label;
    }

    private static Button button(String text) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        button.setStyle(BUTTON_STYLE);
        return button;
    }

    /** La tuile d'un hadron : sa carte, qui montre, son bonus, et dessous ses deux boutons, qui lient. */
    private VBox tile(Hadron hadron) {
        Card card = new Card(COLOR);
        Button one = button("Lier 1");
        Button all = button("");
        one.setOnAction(event -> bind(hadron, false));
        all.setOnAction(event -> bind(hadron, true));
        Label bonus = new Label();
        bonus.setWrapText(true);
        bonus.setMinHeight(Region.USE_PREF_SIZE);
        bonus.setStyle("-fx-font-size: 12px; -fx-text-fill: " + COLOR + ";");
        cards.put(hadron, card);
        bonuses.put(hadron, bonus);
        bindOne.put(hadron, one);
        bindAll.put(hadron, all);
        HBox buttons = new HBox(8, one, all);
        buttons.setAlignment(Pos.CENTER);
        VBox tile = new VBox(6, card, bonus, buttons);
        VBox.setVgrow(card, Priority.ALWAYS);
        return tile;
    }

    /** La carte d'un palier de gluons, avec sa barre posée entre l'en-tête et le nom. */
    private Card buildMilestone(GluonMilestone milestone) {
        Card card = new Card(COLOR);
        Region fill = new Region();
        fill.setMinHeight(8);
        fill.setMaxHeight(8);
        fill.setStyle("-fx-background-color: " + COLOR + "; -fx-background-radius: 4;");
        HBox track = new HBox(fill);
        track.setMinSize(GAUGE_WIDTH, 8);
        track.setMaxSize(GAUGE_WIDTH, 8);
        track.setStyle("-fx-background-color: #24120f; -fx-background-radius: 4;");
        card.setPicture(track);
        card.setMaxWidth(640);
        milestoneCards.put(milestone, card);
        gauges.put(milestone, fill);
        return card;
    }

    /** Lie un hadron, ou tous ceux que les quarks permettent ; la liaison se joue dans la vue. */
    private void bind(Hadron hadron, boolean all) {
        long made = all ? game.bindAllHadrons(hadron) : game.bindHadron(hadron) ? 1 : 0;
        if (made > 0) view.bind(hadron, made);
        refresh();
    }

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public HBox subTabs() {
        return subTabs;
    }

    /** Choisit un sous-onglet : {@link #GLUONS}, {@link #MILESTONES} ou {@link #BINDING}. */
    public void select(int index) {
        selected = index == MILESTONES || index == BINDING ? index : GLUONS;
        Button[] tabs = {gluonsTab, milestonesTab, bindingTab};
        for (int i = 0; i < tabs.length; i++) {
            panes[i].setVisible(i == selected);
            tabs[i].setStyle(SubTabs.style(i == selected, COLOR));
        }
    }

    public int selected() {
        return selected;
    }

    /** Passe au sous-onglet suivant ou précédent, en boucle. */
    public void step(int direction) {
        select(Math.floorMod(selected + direction, panes.length));
        refresh();
    }

    /** Une image : la vue de la liaison n'avance que si son sous-onglet est à l'écran. */
    public void frame(double elapsed, boolean motion) {
        if (selected == BINDING) view.frame(elapsed, motion);
    }

    /** Vrai s'il y a quelque chose à faire : des quarks assez pour lier un hadron ouvert. */
    public static boolean ready(Game game) {
        if (!game.isHadronsUnlocked()) return false;
        for (Hadron hadron : Hadron.values()) if (game.canBindHadron(hadron)) return true;
        return false;
    }

    /** « 2 u + 1 d » : les quarks qu'un hadron prend, saveur par saveur. */
    public static String recipe(Hadron hadron) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Quark, Integer> need : hadron.recipe().entrySet()) {
            text.append(text.length() == 0 ? "" : " + ").append(need.getValue()).append(' ').append(need.getKey().symbol());
        }
        return text.toString();
    }

    /** « 2 quarks haut et 1 quark bas » : la même recette, en toutes lettres. */
    private static String recipeWords(Hadron hadron) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (Map.Entry<Quark, Integer> need : hadron.recipe().entrySet()) {
            parts.add(need.getValue() + (need.getValue() > 1 ? " quarks " : " quark ") + need.getKey().label());
        }
        return parts.size() == 1 ? parts.get(0)
                : String.join(", ", parts.subList(0, parts.size() - 1)) + " et " + parts.get(parts.size() - 1);
    }

    /** Le bonus de {@code count} hadrons d'une sorte, comme le moteur le calcule ({@link Game#hadronBoost}). */
    static double boostOf(long count) {
        return count <= 0 ? 1 : 1 + Game.HADRON_BOOST_STEP * Math.log10(1 + (double) count);
    }

    /**
     * Ce que vaut une sorte et ce qu'elle vaudrait : « Atomes par fusion : ×1,08 → ×1,12 avec un de plus, ×1,4 avec
     * « Lier tout » » ; « ÷ » pour ceux qui baissent un prix. Assez de décimales (jusqu'à quatre) pour que la hausse se lise.
     */
    public static String bonusText(Hadron hadron, long owned, long bindable) {
        double now = boostOf(owned), next = boostOf(owned + 1), all = boostOf(owned + bindable);
        int decimals = 2;
        while (decimals < 4 && boost(now, decimals).equals(boost(next, decimals))) decimals++;
        String sign = hadron.lowersCost() ? "÷" : "×";
        String bonus = hadron.bonus();
        return Character.toUpperCase(bonus.charAt(0)) + bonus.substring(1) + " : " + sign + boost(now, decimals)
                + " → " + sign + boost(next, decimals) + " avec un de plus"
                + (bindable > 1 ? ", " + sign + boost(all, decimals) + " avec « Lier tout »" : "");
    }

    /** « 1 », « 1,08 » : un bonus, en décimales à la française ; le bonus nul (×1) sans virgule. */
    private static String boost(double value, int decimals) {
        return value == 1 ? "1" : String.format(Locale.FRANCE, "%." + decimals + "f", value);
    }

    /** Recopie la partie : les gluons, les quarks en main, chaque hadron, les paliers, seulement si quelque chose a changé. */
    public void refresh() {
        gluonsTab.setText("Gluons" + Readiness.dot(Readiness.Kind.HADRONS, ready(game)));
        boolean exotic = game.isExoticHadronsUnlocked();
        boolean kept = game.isTreeBubbleBought(TreeBubble.ANCIENT_LEFT);
        StringBuilder key = new StringBuilder().append(Detail.shown()).append(Card.signsShown()).append(exotic).append(kept);
        for (Quark quark : KEPT) key.append('/').append(game.state().quarks(quark));
        for (Hadron hadron : Hadron.values()) key.append('/').append(game.hadrons(hadron)).append(':').append(game.hadronsBindable(hadron));
        if (key.toString().equals(shownKey)) return;
        shownKey = key.toString();

        long gluons = game.gluons();
        int reached = 0;
        for (GluonMilestone milestone : GluonMilestone.values()) if (game.isGluonMilestoneReached(milestone)) reached++;
        gluonsLine.setText("Gluons : " + Format.whole(gluons) + "   |   paliers atteints : " + reached + " / " + GluonMilestone.values().length);
        for (Quark quark : KEPT) {
            quarks.get(quark).setText(quark.symbol() + "  " + quark.label() + " : " + Format.whole(game.state().quarks(quark)));
        }
        effectNote.setText("Les gluons lient les quarks du collisionneur en hadrons : chacun prend ses quarks, un antiquark "
                + "(la barre de la formule) coûte un quark de sa saveur. Chaque sorte renforce une grandeur, ou divise un prix, "
                + "d'autant plus qu'on en a : ×(1 + " + boost(Game.HADRON_BOOST_STEP, 2) + " × log₁₀(1 + n)) pour n hadrons, "
                + "×" + boost(boostOf(9), 2) + " à 9, ×" + boost(boostOf(99), 2) + " à 99. Chaque quark lié compte un gluon, pour les "
                + "paliers. " + (kept ? "La bulle de l'arbre ancien, à gauche, les garde d'un univers à l'autre."
                        : "Un nouvel univers les reprend, sauf avec la bulle de l'arbre ancien, à gauche."));
        exoticNote.setVisible(!exotic);
        exoticNote.setManaged(!exotic);
        exoticGrid.setVisible(exotic);
        exoticGrid.setManaged(exotic);
        for (Hadron hadron : Hadron.values()) {
            long owned = game.hadrons(hadron), bindable = game.hadronsBindable(hadron);
            Card.State state = bindable > 0 ? Card.State.READY : owned > 0 ? Card.State.STARTED : Card.State.WAITING;
            cards.get(hadron).show(state, hadron.formula(), "", hadron.label(), hadron.detail(),
                    "Il prend " + recipeWords(hadron) + ". Lier tout en assemble autant que les quarks en main le permettent.",
                    recipe(hadron), Format.whole(owned) + (owned > 1 ? " possédés" : " possédé"));
            bonuses.get(hadron).setText(bonusText(hadron, owned, bindable));
            bindOne.get(hadron).setDisable(bindable <= 0);
            bindAll.get(hadron).setDisable(bindable <= 0);
            bindAll.get(hadron).setText("Lier tout (" + Format.whole(bindable) + ")");
            Button here = bindHere.get(hadron);
            boolean open = game.isHadronAvailable(hadron);
            here.setVisible(open);
            here.setManaged(open);
            here.setDisable(bindable <= 0);
        }
        for (GluonMilestone milestone : GluonMilestone.values()) {
            boolean done = game.isGluonMilestoneReached(milestone);
            double share = Math.min(1, gluons / (double) milestone.gluons());
            Card.State state = done ? Card.State.DONE : gluons > 0 ? Card.State.STARTED : Card.State.WAITING;
            long missing = milestone.gluons() - gluons;
            milestoneCards.get(milestone).show(state, Format.whole(milestone.gluons()) + " gluons",
                    done ? (Card.signsShown() ? "atteint" : "✓ atteint") : "", milestone.label(), milestone.effect(), "",
                    done ? "" : "Il manque " + Format.whole(missing) + (missing > 1 ? " gluons" : " gluon"),
                    done ? "" : Math.round(share * 100) + " %");
            gauges.get(milestone).setPrefWidth(share * GAUGE_WIDTH);
            gauges.get(milestone).setMinWidth(share * GAUGE_WIDTH);
        }
    }

    /** Les quarks d'une saveur en main, comme la page les écrit : pour les vérifications. */
    public String quarkText(Quark quark) {
        Label label = quarks.get(quark);
        return label == null ? "" : label.getText();
    }

    /** La phrase qui dit ce que font les hadrons : pour les vérifications. */
    public Label effectNote() {
        return effectNote;
    }

    /** La phrase des exotiques fermées : pour les vérifications. */
    public Label exoticNote() {
        return exoticNote;
    }

    /** Les cartes des exotiques : cachées tant qu'elles sont fermées. */
    public TileGrid exoticGrid() {
        return exoticGrid;
    }

    /** En tête de page : les gluons et les paliers atteints. */
    public Label gluonsLine() {
        return gluonsLine;
    }

    public Button gluonsTab() {
        return gluonsTab;
    }

    public Button milestonesTab() {
        return milestonesTab;
    }

    public Button bindingTab() {
        return bindingTab;
    }

    public Card card(Hadron hadron) {
        return cards.get(hadron);
    }

    /** Le bonus écrit sous la carte d'un hadron : pour les vérifications. */
    public String bonusText(Hadron hadron) {
        return bonuses.get(hadron).getText();
    }

    public Button bindOneButton(Hadron hadron) {
        return bindOne.get(hadron);
    }

    public Button bindAllButton(Hadron hadron) {
        return bindAll.get(hadron);
    }

    /** La carte d'un palier de gluons. */
    public Card milestoneCard(GluonMilestone milestone) {
        return milestoneCards.get(milestone);
    }

    /** La largeur de la barre remplie d'un palier, sur {@link #GAUGE_WIDTH} : pour les vérifications. */
    public double gaugeShare(GluonMilestone milestone) {
        return gauges.get(milestone).getPrefWidth() / GAUGE_WIDTH;
    }

    /** Le bouton du sous-onglet « Liaison » qui lie un hadron. */
    public Button bindHereButton(Hadron hadron) {
        return bindHere.get(hadron);
    }

    /** La vue de la liaison. */
    public BindingView view() {
        return view;
    }
}

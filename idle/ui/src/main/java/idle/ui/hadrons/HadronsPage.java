package idle.ui.hadrons;

import idle.core.Game;
import idle.core.GameUniverse.TreeBubble;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import idle.ui.Readiness;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.SubTabs;
import idle.ui.common.TileGrid;
import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * L'onglet des hadrons (version 0.149) : les gluons y lient les quarks du collisionneur en hadrons. Il se propose dans
 * le collisionneur avec la bulle de l'arbuste, au milieu, et s'y paie en énergie, comme le vide ; la bulle du jeune
 * arbre, à gauche, l'ouvre dès le début de l'univers.
 *
 * <p>Un seul sous-onglet pour l'instant, « Gluons » : en haut, les quarks en main ; puis les nucléons (proton,
 * neutron), et les particules exotiques, que la bulle du cœur de l'arbre-monde ouvre. Chaque hadron a sa carte (nom,
 * formule, phrase, recette, nombre possédé) et deux boutons, « Lier 1 » et « Lier tout ». Les hadrons n'ont pas encore
 * d'effet : on les assemble, on les compte.
 *
 * <p>Les cartes et les boutons ne sont construits qu'une fois ; leurs textes ne se refont que si les quarks, les
 * hadrons ou l'affichage ont changé.
 */
public final class HadronsPage extends VBox {

    /** La couleur des hadrons : le rouge d'une charge de couleur, celle que portent les quarks liés par les gluons. */
    public static final String COLOR = "#ff8a7a";
    /** Les saveurs qui se gardent en main : le top se désintègre avant d'être lié. */
    private static final Quark[] KEPT = {Quark.UP, Quark.DOWN, Quark.STRANGE, Quark.CHARM, Quark.BOTTOM};
    private static final String HEADING_STYLE = "-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 12 0 2 0; -fx-text-fill: " + COLOR + ";";
    private static final String BUTTON_STYLE = "-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6;"
            + " -fx-border-radius: 6; -fx-background-color: #24120f; -fx-text-fill: " + COLOR + "; -fx-border-color: " + COLOR + ";";
    private static final double MAX_WIDTH = 860;

    private final Game game;
    /** Les sous-onglets : les gluons, seuls pour l'instant. */
    public static final int GLUONS = 0;
    private final Button gluonsTab = new Button("Gluons");
    private final HBox subTabs = new HBox(gluonsTab);
    private final Map<Quark, Label> quarks = new EnumMap<>(Quark.class);
    private final Label effectNote = new Label();
    private final Label exoticNote = new Label();
    private final TileGrid exoticGrid = new TileGrid(250, 3, 10);
    private final Map<Hadron, Card> cards = new EnumMap<>(Hadron.class);
    private final Map<Hadron, Button> bindOne = new EnumMap<>(Hadron.class);
    private final Map<Hadron, Button> bindAll = new EnumMap<>(Hadron.class);
    /** Ce que la page montre (quarks, hadrons, ce qui se lie, affichage) : ses textes ne se refont que si cela change. */
    private String shownKey = "";

    public HadronsPage(Game game) {
        super(10);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(14, 16, 14, 16));

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
        ScrollPane scroll = new ScrollPane(gluons);
        Labels.transparent(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        SubTabs.wire(true, this::select, this::refresh, gluonsTab);
        subTabs.setAlignment(Pos.CENTER);
        getChildren().addAll(subTabs, scroll);
        select(GLUONS);
        refresh();
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.setStyle(HEADING_STYLE);
        return label;
    }

    /** La tuile d'un hadron : sa carte, qui montre, et dessous ses deux boutons, qui lient. */
    private VBox tile(Hadron hadron) {
        Card card = new Card(COLOR);
        Button one = new Button("Lier 1");
        Button all = new Button();
        for (Button button : new Button[] {one, all}) {
            button.setFocusTraversable(false);
            button.setStyle(BUTTON_STYLE);
        }
        one.setOnAction(event -> {
            game.bindHadron(hadron);
            refresh();
        });
        all.setOnAction(event -> {
            game.bindAllHadrons(hadron);
            refresh();
        });
        cards.put(hadron, card);
        bindOne.put(hadron, one);
        bindAll.put(hadron, all);
        HBox buttons = new HBox(8, one, all);
        buttons.setAlignment(Pos.CENTER);
        VBox tile = new VBox(6, card, buttons);
        VBox.setVgrow(card, Priority.ALWAYS);
        return tile;
    }

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public HBox subTabs() {
        return subTabs;
    }

    /** Choisit un sous-onglet : il n'y a que les gluons pour l'instant. */
    public void select(int index) {
        gluonsTab.setStyle(SubTabs.style(true, COLOR));
    }

    public int selected() {
        return GLUONS;
    }

    /** Passe au sous-onglet suivant ou précédent, en boucle : avec un seul, il reste choisi. */
    public void step(int direction) {
        select(GLUONS);
        refresh();
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

    /** Recopie la partie : les quarks en main, puis chaque hadron, seulement si quelque chose a changé. */
    public void refresh() {
        gluonsTab.setText("Gluons" + Readiness.dot(Readiness.Kind.HADRONS, ready(game)));
        boolean exotic = game.isExoticHadronsUnlocked();
        boolean kept = game.isTreeBubbleBought(TreeBubble.ANCIENT_LEFT);
        StringBuilder key = new StringBuilder().append(Detail.shown()).append(Card.signsShown()).append(exotic).append(kept);
        for (Quark quark : KEPT) key.append('/').append(game.state().quarks(quark));
        for (Hadron hadron : Hadron.values()) key.append('/').append(game.hadrons(hadron)).append(':').append(game.hadronsBindable(hadron));
        if (key.toString().equals(shownKey)) return;
        shownKey = key.toString();

        for (Quark quark : KEPT) {
            quarks.get(quark).setText(quark.symbol() + "  " + quark.label() + " : " + Format.whole(game.state().quarks(quark)));
        }
        effectNote.setText("Les gluons lient les quarks du collisionneur en hadrons : chacun prend ses quarks, un antiquark "
                + "(la barre de la formule) coûte un quark de sa saveur. Les hadrons n'ont pas encore d'effet : on les assemble, "
                + "on les compte. " + (kept ? "La bulle de l'arbre ancien, à gauche, les garde d'un univers à l'autre."
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
            bindOne.get(hadron).setDisable(bindable <= 0);
            bindAll.get(hadron).setDisable(bindable <= 0);
            bindAll.get(hadron).setText("Lier tout (" + Format.whole(bindable) + ")");
        }
    }

    /** Les quarks d'une saveur en main, comme la page les écrit : pour les vérifications. */
    public String quarkText(Quark quark) {
        Label label = quarks.get(quark);
        return label == null ? "" : label.getText();
    }

    /** La phrase qui dit que les hadrons n'ont pas encore d'effet : pour les vérifications. */
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

    public Button gluonsTab() {
        return gluonsTab;
    }

    public Card card(Hadron hadron) {
        return cards.get(hadron);
    }

    public Button bindOneButton(Hadron hadron) {
        return bindOne.get(hadron);
    }

    public Button bindAllButton(Hadron hadron) {
        return bindAll.get(hadron);
    }
}

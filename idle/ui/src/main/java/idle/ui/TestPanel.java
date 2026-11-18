package idle.ui;

import idle.core.Game;
import idle.core.state.GameState;
import idle.ui.TestTools.Section;
import idle.ui.TestTools.Tool;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Le panneau du profil de test (argument {@code --test}, tâche Gradle {@code runTest}) : un tiroir à droite de la
 * fenêtre, qui s'ouvre et se ferme par sa poignée ou par {@value #KEY_NAME}. Il modifie directement la partie, à côté
 * des règles : c'est un outil de développement, pas du gameplay.
 *
 * <p>En haut, la vitesse du jeu (de figé à ×1 000), une recherche dans tous les outils et le résumé de la partie ;
 * dessous, les sections du catalogue ({@link TestTools}). La section de l'onglet affiché passe en tête, ouverte ; les
 * autres se déplient d'un clic. Une recherche montre tous les outils qui lui répondent, dans toutes les sections. Chaque
 * fenêtre de dimension a en plus sa bande ({@link DimensionTestBar}).
 */
final class TestPanel extends HBox {
    /** La touche qui ouvre et ferme le panneau. */
    static final String KEY_NAME = "F9";
    /** Les vitesses du jeu proposées, de figé à ×1 000. */
    static final double[] SPEEDS = {0, 0.25, 0.5, 1, 2, 5, 10, 100, 1000};
    static final double WIDTH = 360;

    static final String ACCENT = "#ff8a80";
    private static final String BUTTON = "-fx-font-size: 12px; -fx-padding: 3 9; -fx-cursor: hand; -fx-background-radius: 4; -fx-border-radius: 4;";
    static final String TOOL_STYLE = BUTTON + " -fx-text-fill: #ffd9d4; -fx-background-color: #3a1c1f; -fx-border-color: #ff8a8066;";
    private static final String ON_STYLE = BUTTON + " -fx-text-fill: #2a1416; -fx-background-color: " + ACCENT + "; -fx-border-color: #ffffff;";
    private static final String HEAD_STYLE = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-cursor: hand;"
            + " -fx-background-radius: 4; -fx-alignment: center-left; -fx-text-fill: " + ACCENT + "; -fx-background-color: #2a1416;";
    private static final String TEXT = "-fx-font-size: 11px; -fx-text-fill: #ffd9d4;";

    private final Game game;
    private final TestActions actions;
    private final TestBots bots;
    private final List<Tool> tools;
    private final Button handle = new Button("TEST");
    private final VBox body = new VBox(8);
    private final TextField search = new TextField();
    private final Label status = new Label();
    private final Label note = new Label();
    private final Button liveBot = new Button();
    private final ComboBox<String> botMode = new ComboBox<>();
    private final List<Button> speedButtons = new ArrayList<>();
    private final VBox sectionsBox = new VBox(4);
    private final Map<Section, VBox> sections = new EnumMap<>(Section.class);
    private final Map<Section, Button> heads = new EnumMap<>(Section.class);
    private final Map<Section, FlowPane> lines = new EnumMap<>(Section.class);
    /** Les sections dépliées à la main ; celle de l'onglet affiché l'est d'office. */
    private final Map<Section, Boolean> unfolded = new EnumMap<>(Section.class);
    private int speed = indexOf(1);
    private Section tabSection;

    TestPanel(Game game, TestTools.Hooks hooks) {
        super(0);
        this.game = game;
        this.actions = new TestActions(game);
        this.bots = new TestBots(game, this::note);
        this.tools = TestTools.build(game, actions, bots, hooks, this::note);
        bots.setOnChange(this::refreshBots);

        handle.setStyle(ON_STYLE + " -fx-rotate: -90; -fx-font-weight: bold;");
        handle.setFocusTraversable(false);
        handle.setOnAction(event -> toggle());
        Label handleHint = new Label(KEY_NAME);
        handleHint.setStyle(TEXT);
        VBox handleBox = new VBox(28, handleHint, new javafx.scene.Group(handle));
        handleBox.setAlignment(Pos.CENTER);
        handleBox.setPadding(new Insets(0, 2, 0, 2));
        handleBox.setStyle("-fx-background-color: #2a1416; -fx-border-color: " + ACCENT + "; -fx-border-width: 0 1 0 1;");

        Label title = new Label("PROFIL DE TEST");
        title.setStyle("-fx-text-fill: " + ACCENT + "; -fx-font-weight: bold;");
        search.setPromptText("Chercher un outil…");
        // Il ne prend pas le clavier tout seul à l'ouverture du jeu : seulement quand on clique dedans.
        search.setFocusTraversable(false);
        search.setStyle("-fx-font-size: 12px;");
        search.textProperty().addListener((observable, before, text) -> layoutSections());

        FlowPane speedRow = new FlowPane(4, 4);
        Label speedTitle = new Label("Vitesse :");
        speedTitle.setStyle(TEXT);
        speedRow.getChildren().add(speedTitle);
        for (int i = 0; i < SPEEDS.length; i++) {
            int index = i;
            Button button = new Button(TestTools.speedName(SPEEDS[i]));
            button.setFocusTraversable(false);
            button.setOnAction(event -> setSpeed(index));
            speedButtons.add(button);
            speedRow.getChildren().add(button);
        }

        // Les bots : le choix de la politique et le direct restent en vue, au-dessus des sections.
        botMode.getItems().setAll(BotBridge.modes());
        botMode.setValue(bots.mode());
        botMode.setVisibleRowCount(12);
        botMode.setFocusTraversable(false);
        botMode.setStyle("-fx-font-size: 12px;");
        botMode.setOnAction(event -> bots.setMode(botMode.getValue()));
        liveBot.setFocusTraversable(false);
        liveBot.setOnAction(event -> bots.toggleLive());
        Label botTitle = new Label("Bot :");
        botTitle.setStyle(TEXT);
        HBox botRow = new HBox(6, botTitle, botMode, liveBot);
        botRow.setAlignment(Pos.CENTER_LEFT);

        status.setStyle(TEXT);
        status.setWrapText(true);
        note.setStyle(TEXT + " -fx-font-style: italic;");
        note.setWrapText(true);

        for (Section section : Section.values()) buildSection(section);
        ScrollPane scroll = new ScrollPane(sectionsBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #1f0f11; -fx-background-color: #1f0f11;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        body.getChildren().addAll(title, search, speedRow, botRow, status, note, scroll);
        body.setPadding(new Insets(10));
        body.setPrefWidth(WIDTH);
        body.setMinWidth(WIDTH);
        body.setMaxWidth(WIDTH);
        body.setStyle("-fx-background-color: #1f0f11;");
        getChildren().addAll(handleBox, body);
        setMaxHeight(Double.MAX_VALUE);
        setSpeed(speed);
        refreshBots();
        layoutSections();
        refresh();
    }

    private static int indexOf(double factor) {
        for (int i = 0; i < SPEEDS.length; i++) if (SPEEDS[i] == factor) return i;
        return 0;
    }

    /** Une section : sa tête, qui la déplie, et ses outils. */
    private void buildSection(Section section) {
        FlowPane line = new FlowPane(5, 5);
        line.setPrefWrapLength(WIDTH - 30);
        Button head = new Button();
        head.setStyle(HEAD_STYLE);
        head.setMaxWidth(Double.MAX_VALUE);
        head.setFocusTraversable(false);
        head.setOnAction(event -> {
            unfolded.put(section, !isUnfolded(section));
            layoutSections();
        });
        VBox box = new VBox(4, head, line);
        sections.put(section, box);
        heads.put(section, head);
        lines.put(section, line);
    }

    private boolean isUnfolded(Section section) { return unfolded.getOrDefault(section, section == tabSection); }

    /** Place les sections : celle de l'onglet en tête ; avec une recherche, seulement les outils qui y répondent. */
    private void layoutSections() {
        String wanted = search.getText() == null ? "" : search.getText();
        boolean searching = !wanted.isBlank();
        List<Section> order = new ArrayList<>();
        if (tabSection != null) order.add(tabSection);
        for (Section section : Section.values()) if (section != tabSection) order.add(section);
        sectionsBox.getChildren().clear();
        for (Section section : order) {
            List<Tool> shown = new ArrayList<>();
            for (Tool tool : tools) if (tool.section() == section && tool.matches(wanted)) shown.add(tool);
            if (searching && shown.isEmpty()) continue;
            boolean open = searching || isUnfolded(section);
            heads.get(section).setText((open ? "▾ " : "▸ ") + section.title + (section == tabSection ? "  (onglet affiché)" : "")
                    + "  · " + shown.size());
            FlowPane line = lines.get(section);
            line.getChildren().clear();
            if (open) for (Tool tool : shown) line.getChildren().add(button(tool));
            line.setVisible(open);
            line.setManaged(open);
            sectionsBox.getChildren().add(sections.get(section));
        }
    }

    private Button button(Tool tool) {
        Button button = new Button(tool.label());
        button.setStyle(TOOL_STYLE);
        button.setFocusTraversable(false);     // sinon Espace, pressé plus tard, rejouerait le dernier outil
        button.setOnAction(event -> run(tool));
        return button;
    }

    /** Lance un outil : sans effet tant que la partie n'a pas commencé (le premier générateur). */
    void run(Tool tool) {
        if (!game.isStarted()) {
            note("La partie n'a pas commencé : créez le premier générateur.");
            return;
        }
        try {
            tool.action().run();
        } catch (RuntimeException error) {
            note("« " + tool.label() + " » a échoué : " + error);
        }
        refresh();
    }

    // ------------------------------------------------------------------

    /** Ouvre ou ferme le tiroir ; la poignée reste. */
    void toggle() {
        boolean shown = !body.isVisible();
        body.setVisible(shown);
        body.setManaged(shown);
        if (shown) layoutSections();
    }

    boolean isOpen() { return body.isVisible(); }

    /** L'onglet affiché a changé : sa section passe en tête, dépliée. */
    void followTab(String tab) {
        Section section = Section.ofTab(tab);
        if (section == tabSection) return;
        tabSection = section;
        if (isOpen()) layoutSections();
    }

    private void setSpeed(int index) {
        speed = index;
        for (int i = 0; i < speedButtons.size(); i++) speedButtons.get(i).setStyle(i == index ? ON_STYLE : TOOL_STYLE);
    }

    /** Le facteur du temps choisi : 1 = vitesse normale, 0 = figé. */
    double timeFactor() { return SPEEDS[speed]; }

    void note(String text) { note.setText(text); }

    private void refreshBots() {
        liveBot.setText(bots.live() ? "En direct : en marche" : "En direct : coupé");
        liveBot.setStyle(bots.live() ? ON_STYLE : TOOL_STYLE);
        if (!BotBridge.available()) note("Les bots ne sont pas là : lancez le jeu avec le module « bots » (runTest l'ajoute).");
    }

    /** Le résumé de la partie, sous la recherche. */
    void refresh() {
        GameState state = game.state();
        status.setText("jeu " + Format.duration(state.timePlayed()) + "  |  partie " + Format.duration(game.stats().runTime())
                + "  |  " + state.explosions() + (state.explosions() > 1 ? " explosions" : " explosion")
                + (game.bigBangs() > 0 ? "  |  " + game.bigBangs() + " Big Bang" + (game.bigBangs() > 1 ? "s" : "") : "")
                + (game.universes() > 0 ? "  |  " + game.universes() + " univers, " + game.dimensionsUnlocked() + " dimension(s)" : "")
                + (bots.isFastForwarding() ? "  |  avance rapide en cours" : ""));
    }

    /** Les bots et l'avance rapide, partagés avec la bande des dimensions. */
    TestBots bots() { return bots; }

    TestActions actions() { return actions; }

    /** Tous les outils : pour les vérifications. */
    List<Tool> tools() { return tools; }
}

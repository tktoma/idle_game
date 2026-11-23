package idle.ui;

import idle.core.universe.SkinVisual;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Le panneau du profil test UI (argument {@code --test-ui}, tâche Gradle {@code runTestUi}, version 0.248), à droite de
 * la fenêtre : l'onglet à passer, les visuels à croiser, tout croisé ou par paires, la lecture automatique et son allure,
 * l'étape d'avant ou d'après, et les erreurs relevées, à copier.
 *
 * <p>Le jeu montre chaque étape ({@code show}) et vérifie, un peu avant de passer à la suivante, que la vue n'est pas
 * vide ({@code check}).
 */
final class UiTestPanel extends VBox {
    static final double WIDTH = 340;
    private static final String ACCENT = "#7fd1ff";
    private static final String BUTTON = "-fx-font-size: 12px; -fx-padding: 3 9; -fx-cursor: hand; -fx-background-radius: 4; -fx-border-radius: 4;";
    private static final String TOOL_STYLE = BUTTON + " -fx-text-fill: #d4ecff; -fx-background-color: #132433; -fx-border-color: #7fd1ff66;";
    private static final String ON_STYLE = BUTTON + " -fx-text-fill: #0b1620; -fx-background-color: " + ACCENT + "; -fx-border-color: #ffffff;";
    private static final String TEXT = "-fx-font-size: 11px; -fx-text-fill: #d4ecff;";
    private static final String HEAD = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";";

    private final List<UiTestPlan.Group> groups = UiTestPlan.groups();
    private final ComboBox<String> groupBox = new ComboBox<>();
    private final Button crossButton = new Button(UiTestPlan.Mode.CROSS.label);
    private final Button pairsButton = new Button(UiTestPlan.Mode.PAIRS.label);
    private final VBox visualsBox = new VBox(3);
    /** Les cases des axes affichés (visuels de l'onglet, puis ce qui le touche), et leur axe. */
    private final Map<UiTestPlan.Axis, CheckBox> checks = new LinkedHashMap<>();
    /** Version 0.252 : croiser aussi tout ce qui touche l'onglet (la fenêtre, le curseur, les cartes, le thème, la notation). */
    private final CheckBox around = new CheckBox("Avec tout ce qui touche l'onglet : fenêtre, onglets, cartes, curseur, "
            + "info-bulles, avatar, notifications, panneaux, thème, notation");
    /**
     * Version 0.286 : la charge maximale. Les états de l'onglet sont remplacés par l'onglet au plus chargé que la partie
     * peut atteindre (l'atome à 118 orbes et 40 copies, 30 générateurs au dernier palier de vitesse…).
     */
    private final CheckBox heaviest = new CheckBox("Charge maximale : l'onglet au plus chargé que la partie peut atteindre "
            + "(atome à 118 orbes et 40 copies, 30 générateurs au dernier palier, boule de matière noire au plus gros…)");
    /** Les axes des réglages (le thème, la notation), fournis par la fenêtre. */
    private final List<UiTestPlan.Axis> settingsAxes;
    private final Label size = new Label();
    private final Slider pace = new Slider(UiTestRun.FASTEST, UiTestRun.SLOWEST, 1);
    private final Label paceText = new Label();
    private final Button play = new Button("Lecture");
    private final Label position = new Label();
    private final Label combination = new Label();
    private final Label state = new Label();
    private final Label errorsText = new Label();
    /** Version 0.253 : les images par seconde, en ce moment, de la combinaison d'avant, de la plus lente, et leur fichier. */
    private final Label fpsNow = new Label();
    private final Label fpsLast = new Label();
    private final Label fpsSlowest = new Label();
    private final Label fpsFileText = new Label();
    private final UiTestFpsFile fpsFile;
    /** Le temps depuis le dernier affichage des images par seconde : deux fois par seconde suffit. */
    private double sinceFps;
    private final TextArea errorsArea = new TextArea();
    private final BiConsumer<UiTestPlan.Run, Long> show;
    private final BiConsumer<UiTestPlan.Run, UiTestRun> check;
    private UiTestPlan.Mode mode = UiTestPlan.Mode.CROSS;
    private UiTestRun run;
    private long errorsShown = -1;
    /** Faux tant que le jeu n'est pas prêt : rien n'est montré avant {@link #begin()}. */
    private boolean begun;

    /**
     * @param show  montre l'étape : les skins de sa combinaison, son onglet et son état
     * @param check vérifie la vue de l'étape montrée, et relève ce qui ne va pas
     */
    UiTestPanel(BiConsumer<UiTestPlan.Run, Long> show, BiConsumer<UiTestPlan.Run, UiTestRun> check, List<UiTestPlan.Axis> settingsAxes,
                UiTestFpsFile fpsFile) {
        super(8);
        this.fpsFile = fpsFile;
        this.show = show;
        this.check = check;
        this.settingsAxes = List.copyOf(settingsAxes);
        Label title = new Label("Profil test UI");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";");
        Label intro = new Label("Chaque combinaison des choix cochés (skins, et avec l'option, thème et notation) passe par chaque état de l'onglet. La partie ne "
                + "gagne rien : le temps ne passe que sur les états qui bougent.");
        intro.setWrapText(true);
        intro.setStyle(TEXT);

        for (UiTestPlan.Group group : groups) groupBox.getItems().add(group.name());
        groupBox.setValue(groups.get(0).name());
        groupBox.setFocusTraversable(false);
        groupBox.setMaxWidth(Double.MAX_VALUE);
        groupBox.setOnAction(event -> fillVisuals());
        around.setWrapText(true);
        around.setStyle(TEXT);
        around.setFocusTraversable(false);
        around.setOnAction(event -> fillVisuals());
        heaviest.setWrapText(true);
        heaviest.setStyle(TEXT);
        heaviest.setFocusTraversable(false);
        heaviest.setOnAction(event -> rebuild());

        crossButton.setOnAction(event -> setMode(UiTestPlan.Mode.CROSS));
        pairsButton.setOnAction(event -> setMode(UiTestPlan.Mode.PAIRS));
        Button all = tool("Tout cocher", () -> checkAll(true));
        Button none = tool("Tout décocher", () -> checkAll(false));
        FlowPane modeRow = new FlowPane(6, 6, crossButton, pairsButton, all, none);
        for (Button button : List.of(crossButton, pairsButton)) button.setFocusTraversable(false);

        pace.setFocusTraversable(false);
        pace.valueProperty().addListener((observable, before, seconds) -> {
            run.setSecondsPerStep(seconds.doubleValue());
            refresh();
        });
        play.setFocusTraversable(false);
        play.setOnAction(event -> {
            run.setPlaying(!run.playing());
            refresh();
        });
        Button previous = tool("◀ Avant", () -> { if (run.previous()) showNow(); });
        Button next = tool("Après ▶", () -> { if (run.next()) showNow(); });
        Button restart = tool("Recommencer", () -> {
            run.goTo(0);
            showNow();
        });
        HBox controls = new HBox(6, previous, play, next, restart);
        controls.setAlignment(Pos.CENTER_LEFT);

        errorsArea.setEditable(false);
        errorsArea.setWrapText(true);
        errorsArea.setPrefRowCount(8);
        errorsArea.setFocusTraversable(false);
        errorsArea.setStyle("-fx-font-size: 11px;");
        Button copy = tool("Copier le rapport", () -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(run.report());
            Clipboard.getSystemClipboard().setContent(content);
        });
        Button clear = tool("Effacer", () -> {
            run.clearErrors();
            refresh();
        });

        Button copyPath = tool("Copier le chemin du fichier", () -> {
            if (fpsFile == null) return;
            ClipboardContent content = new ClipboardContent();
            content.putString(fpsFile.file().toString());
            Clipboard.getSystemClipboard().setContent(content);
        });
        for (Label label : List.of(size, paceText, position, combination, state, errorsText, fpsNow, fpsLast, fpsSlowest, fpsFileText)) {
            label.setWrapText(true);
            label.setStyle(TEXT);
        }
        position.setStyle(HEAD);
        ScrollPane visualsScroll = new ScrollPane(visualsBox);
        visualsScroll.setFitToWidth(true);
        visualsScroll.setPrefViewportHeight(170);
        visualsScroll.setStyle("-fx-background: #0e1a24; -fx-background-color: #0e1a24;");
        getChildren().addAll(title, intro, head("Onglet"), groupBox, around, heaviest, modeRow, head("Visuels croisés"), visualsScroll, size,
                head("Lecture"), paceText, pace, controls, position, state, combination,
                head("Images par seconde"), fpsNow, fpsLast, fpsSlowest, fpsFileText, copyPath,
                head("Erreurs"), errorsText, errorsArea, new HBox(6, copy, clear));
        VBox.setVgrow(errorsArea, Priority.ALWAYS);
        setPadding(new Insets(10));
        setPrefWidth(WIDTH);
        setMinWidth(WIDTH);
        setMaxWidth(WIDTH);
        setStyle("-fx-background-color: #0e1a24;");
        run = new UiTestRun(currentRun(groups.get(0)));
        if (fpsFile != null) run.setFpsSink(fpsFile::write);
        fillVisuals();
    }

    private static Label head(String text) {
        Label label = new Label(text);
        label.setStyle(HEAD);
        return label;
    }

    private static Button tool(String text, Runnable action) {
        Button button = new Button(text);
        button.setStyle(TOOL_STYLE);
        button.setFocusTraversable(false);
        button.setOnAction(event -> action.run());
        return button;
    }

    private UiTestPlan.Group group() {
        for (UiTestPlan.Group group : groups) if (group.name().equals(groupBox.getValue())) return group;
        return groups.get(0);
    }

    /**
     * Les cases des axes de l'onglet choisi, toutes cochées : ses visuels, puis, l'option cochée, ce qui le touche (les
     * visuels de la fenêtre qu'il n'a pas déjà, le thème et la notation).
     */
    private void fillVisuals() {
        visualsBox.getChildren().clear();
        checks.clear();
        List<UiTestPlan.Axis> axes = new ArrayList<>(UiTestPlan.axes(group()));
        if (around.isSelected()) {
            for (SkinVisual visual : UiTestPlan.around(group())) axes.add(UiTestPlan.Axis.of(visual));
            axes.addAll(settingsAxes);
        }
        for (UiTestPlan.Axis axis : axes) {
            CheckBox box = new CheckBox(axis.label() + " (" + axis.options().size() + ")");
            box.setSelected(true);
            box.setStyle(TEXT);
            box.setFocusTraversable(false);
            box.setOnAction(event -> rebuild());
            checks.put(axis, box);
            visualsBox.getChildren().add(box);
        }
        rebuild();
    }

    private void checkAll(boolean on) {
        for (CheckBox box : checks.values()) box.setSelected(on);
        rebuild();
    }

    private void setMode(UiTestPlan.Mode chosen) {
        mode = chosen;
        rebuild();
    }

    private UiTestPlan.Run currentRun(UiTestPlan.Group group) {
        List<UiTestPlan.Axis> crossed = new ArrayList<>();
        for (Map.Entry<UiTestPlan.Axis, CheckBox> entry : checks.entrySet()) if (entry.getValue().isSelected()) crossed.add(entry.getKey());
        return new UiTestPlan.Run(group, crossed, mode, heaviest.isSelected());
    }

    /** Un autre passage (onglet, visuels ou façon de combiner) : la lecture s'arrête, on repart de la première étape. */
    private void rebuild() {
        run.setPlaying(false);
        run.setRun(currentRun(group()));
        showNow();
    }

    private void showNow() {
        if (begun) show.accept(run.run(), run.step());
        refresh();
    }

    /** Le jeu est prêt : la première étape se montre. */
    void begin() {
        begun = true;
        showNow();
    }

    /** Une erreur du jeu pendant le passage : elle est relevée à l'étape montrée. */
    void error(Throwable problem) {
        run.error(problem.getClass().getSimpleName() + (problem.getMessage() == null ? "" : " : " + problem.getMessage()));
        refresh();
    }

    /** Un problème sans exception (un sous-onglet absent…) : relevé à l'étape montrée. */
    void problem(String what) {
        run.error(what);
        refresh();
    }

    /** Vrai si l'état montré veut que le temps du jeu passe. */
    boolean live() { return run.live(); }

    /** La lecture avance ; un peu avant de passer à l'étape suivante, la vue est vérifiée. */
    /**
     * Une image vient d'être montrée : {@code dt} secondes après la précédente, et {@code workSeconds} de calcul du jeu.
     * Elle compte pour la combinaison montrée ; l'affichage suit deux fois par seconde.
     */
    void measure(double dt, double workSeconds) {
        if (!begun) return;
        run.measure(dt, workSeconds);
        sinceFps += dt;
        if (sinceFps >= 0.5) {
            sinceFps = 0;
            showFps();
        }
    }

    /** La mesure en cours part au fichier : la fenêtre se ferme. */
    void close() { run.flushFps(); }

    private void showFps() {
        fpsNow.setText(String.format(java.util.Locale.ROOT, "En ce moment : %.0f images par seconde · calcul %.1f ms par image",
                run.liveFps(), run.liveWorkMs()));
        UiTestRun.Fps last = run.lastFps();
        fpsLast.setText(last == null ? "Combinaison d'avant : pas encore mesurée." : String.format(java.util.Locale.ROOT,
                "Combinaison d'avant (n° %s) : %.0f en moyenne, %.0f au plus bas (pire image %.0f ms, état « %s »), mise en place %.0f ms",
                Format.whole(last.combination() + 1), last.average(), last.lowest(), last.worstMs(), last.slowestState(), last.setupMs()));
        UiTestRun.Fps slow = run.slowestFps();
        fpsSlowest.setText(slow == null ? "" : String.format(java.util.Locale.ROOT, "La plus lente : n° %s, %.0f en moyenne : %s",
                Format.whole(slow.combination() + 1), slow.average(), slow.skins()));
        if (fpsFile == null) fpsFileText.setText("Pas de fichier.");
        else fpsFileText.setText((fpsFile.problem().isEmpty() ? "Une ligne par combinaison dans " : "Écriture impossible (" + fpsFile.problem() + ") : ")
                + fpsFile.file());
    }

    void frame(double dt) {
        if (run.frame(dt)) showNow();
        if (run.takeCheck()) {
            check.accept(run.run(), run);
            refresh();
        }
    }

    private void refresh() {
        UiTestPlan.Run current = run.run();
        crossButton.setStyle(mode == UiTestPlan.Mode.CROSS ? ON_STYLE : TOOL_STYLE);
        pairsButton.setStyle(mode == UiTestPlan.Mode.PAIRS ? ON_STYLE : TOOL_STYLE);
        long combinations = current.combinations(), total = current.total();
        // Version 0.287 : chaque visuel ne se croise que sur son sous-onglet ; les états se rangent en parts.
        int parts = current.parts();
        size.setText((combinations == Long.MAX_VALUE ? "Plus de 9 × 10¹⁸" : Format.whole(combinations)) + " combinaisons sur "
                + current.steps().size() + (current.heaviest ? " états à la charge maximale" : " états")
                + (parts > 1 ? " (" + parts + " parts, une par sous-onglet)" : "") + " = " + (total == Long.MAX_VALUE ? "trop" : Format.whole(total))
                + " étapes ; il reste " + run.remaining() + " à cette allure.");
        paceText.setText(String.format(java.util.Locale.ROOT, "%.1f s par étape", run.secondsPerStep()));
        play.setText(run.playing() ? "Pause" : "Lecture");
        play.setStyle(run.playing() ? ON_STYLE : TOOL_STYLE);
        position.setText("Étape " + Format.whole(run.step() + 1) + " / " + (total == Long.MAX_VALUE ? "…" : Format.whole(total)));
        UiTestPlan.Step step = current.stepOf(run.step());
        state.setText("État : " + step.label() + (step.live() ? " (le temps passe)" : ""));
        combination.setText(current.describe(run.step()));
        errorsText.setText(run.errorCount() == 0 ? "Aucune erreur." : Format.whole(run.errorCount())
                + (run.errorCount() > 1 ? " erreurs relevées." : " erreur relevée."));
        if (run.errorCount() != errorsShown) {
            errorsShown = run.errorCount();
            StringBuilder lines = new StringBuilder();
            List<UiTestRun.Error> errors = run.errors();
            for (int i = Math.max(0, errors.size() - 50); i < errors.size(); i++) lines.append(errors.get(i).line()).append('\n');
            errorsArea.setText(lines.toString());
        }
    }

    /** Pour les vérifications : le passage en cours. */
    UiTestRun testRun() { return run; }
}

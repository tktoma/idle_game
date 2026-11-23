package idle.ui;

import idle.core.universe.SkinVisual;
import idle.ui.universe.Dimension;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

/**
 * Ce que le profil test UI (version 0.248) fait dans la fenêtre du jeu : chaque étape met les skins de sa combinaison
 * (les visuels non croisés au Classique), affiche l'onglet, puis l'état ; un peu avant l'étape suivante, la vue est
 * vérifiée (pas vide). Les erreurs vont au panneau ({@link UiTestPanel}).
 */
final class UiTestStage implements UiTestPlan.Driver {
    private final GameAppBase app;
    private final TestActions actions;
    private final TestTools.Hooks hooks;
    private UiTestPanel panel;

    UiTestStage(GameAppBase app, TestTools.Hooks hooks) {
        this.app = app;
        this.actions = new TestActions(app.game);
        this.hooks = hooks;
    }

    void setPanel(UiTestPanel panel) { this.panel = panel; }

    /** Montre l'étape {@code step} du passage {@code run}. */
    void show(UiTestPlan.Run run, long step) {
        try {
            // Ce qu'une étape d'avant a pu ouvrir se referme : chaque étape part de la même fenêtre.
            app.helpPane.close();
            app.endPane.close();
            app.hideStartButton();
            app.dimensionWindow.close();
            app.toasts.clear();
            app.testSpeedLog = Double.NaN;
            long combination = run.combinationOf(step);
            for (SkinVisual visual : SkinVisual.values()) app.game.chooseSkin(visual, run.skin(combination, visual));
            // Le thème et la notation : ceux d'origine (Sombre, Scientifique) s'ils ne sont pas croisés (0.252).
            Settings.Theme theme = Settings.Theme.values()[run.option(combination, UiTestPlan.THEME)];
            Settings.Notation notation = Settings.Notation.values()[run.option(combination, UiTestPlan.NOTATION)];
            if (app.settings.notation() != notation) app.settings.setNotation(notation);
            if (app.settings.theme() != theme) {
                app.settings.setTheme(theme);
                app.applyAppearance();
            }
            tab(run.group.tab());
            run.stepOf(step).action().accept(this);
            app.refresh();
        } catch (RuntimeException problem) {
            if (panel != null) panel.error(problem);
        }
    }

    /** Les axes des réglages qui changent l'allure de tout onglet : le thème, puis la notation des nombres (0.252). */
    java.util.List<UiTestPlan.Axis> settingsAxes() {
        java.util.List<String> themes = new java.util.ArrayList<>(), notations = new java.util.ArrayList<>();
        for (Settings.Theme theme : Settings.Theme.values()) themes.add(theme.label());
        for (Settings.Notation notation : Settings.Notation.values()) notations.add(notation.label());
        return java.util.List.of(new UiTestPlan.Axis(UiTestPlan.THEME, "Thème", themes),
                new UiTestPlan.Axis(UiTestPlan.NOTATION, "Notation", notations));
    }

    /** Vérifie la vue montrée : une image d'une seule couleur est relevée comme vide (rien ne s'est dessiné). */
    void check(UiTestPlan.Run run, UiTestRun progress) {
        try {
            Node view = app.dimensionWindow.isVisible() ? app.dimensionWindow : app.pages;
            if (view == null || view.getLayoutBounds().getWidth() < 2 || view.getLayoutBounds().getHeight() < 2) return;
            WritableImage image = view.snapshot(null, null);
            if (uniform(image)) progress.error("vue vide : une seule couleur partout");
        } catch (RuntimeException problem) {
            progress.error(problem.getClass().getSimpleName() + " à la vérification : " + problem.getMessage());
        }
    }

    /** Vrai si l'image n'a qu'une couleur (relevée un point sur six dans chaque sens). */
    static boolean uniform(WritableImage image) {
        PixelReader pixels = image.getPixelReader();
        int width = (int) image.getWidth(), height = (int) image.getHeight();
        if (pixels == null || width == 0 || height == 0) return true;
        int first = pixels.getArgb(0, 0);
        for (int y = 0; y < height; y += 6) {
            for (int x = 0; x < width; x += 6) if (pixels.getArgb(x, y) != first) return false;
        }
        return true;
    }

    @Override
    public void tab(String tab) {
        app.selectTab(GameAppBase.Tab.valueOf(tab));
        app.refresh();
    }

    @Override
    public boolean subTab(String label) {
        for (Button button : subTabButtons()) {
            if (button.getText() != null && (button.getText().startsWith(label)
                    || button.getUserData() instanceof String name && name.startsWith(label))) {
                button.fire();
                return true;
            }
        }
        missing("le sous-onglet « " + label + " » n'est pas là");
        return false;
    }

    /** Ce qu'un état demande et que la fenêtre n'a pas : relevé comme une erreur à l'étape montrée. */
    private void missing(String what) { if (panel != null) panel.problem(what); }

    @Override
    public boolean subTabAt(int index) {
        java.util.List<Button> buttons = subTabButtons();
        if (index < 0 || index >= buttons.size()) {
            missing("pas de sous-onglet " + (index + 1) + " (" + buttons.size() + " en tout)");
            return false;
        }
        buttons.get(index).fire();
        return true;
    }

    /** Les sous-onglets visibles de l'onglet affiché, dans l'ordre. */
    private java.util.List<Button> subTabButtons() {
        java.util.List<Button> buttons = new java.util.ArrayList<>();
        for (GameAppBase.SubTabDock dock : app.docks) {
            if (dock.tab() != app.selectedTab) continue;
            for (Node node : dock.bar().getChildren()) if (node instanceof Button button && button.isVisible()) buttons.add(button);
        }
        return buttons;
    }

    @Override
    public void act(Consumer<TestActions> change) { change.accept(actions); }

    @Override
    public void hook(Consumer<TestTools.Hooks> animation) { animation.accept(hooks); }

    @Override
    public void dimension(int number) {
        app.selectTab(GameAppBase.Tab.UNIVERSE);
        app.dimensionWindow.open(Dimension.of(number));
    }

    @Override
    public void toast(String text) { app.toasts.show(text, GameAppBase.ACHIEVEMENTS_COLOR); }

    @Override
    public void help() { app.helpPane.open(); }

    @Override
    public void ending() { app.endPane.show(); }

    @Override
    public void startButton() { app.showStartButton(); }

    @Override
    public void particleSpeed(double log) { app.testSpeedLog = log; }
}

package idle.ui;

import idle.core.Game;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import idle.ui.automation.AutomationPage;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import java.util.List;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.util.Duration;

/**
 * L'application : la fenêtre et sa taille, la sauvegarde, la remise à zéro, le clavier et le jeu en tâche de fond.
 */
public abstract class GameAppWindow extends GameAppBase {

    /** Seul {@link GameApp} prolonge la chaîne. */
    GameAppWindow() {
    }

    // Ce que fournissent les parties suivantes.
    abstract void stepSubTab(int direction);


    /** Le jeu se ferme : la partie et la place de la fenêtre sont écrites une dernière fois. */
    @Override
    public void stop() {
        closing();
    }

    void closing() {
        if (closed) return;
        closed = true;
        save();
        settings.setLastTab(selectedTab.name());
        if (stage == null) return;
        // Agrandie au maximum, la fenêtre garde en mémoire la place qu'elle avait avant.
        double[] before = settings.window();
        if (stage.isMaximized() && before != null) {
            settings.setWindow(before[0], before[1], before[2], before[3], true);
        } else {
            settings.setWindow(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight(), stage.isMaximized());
        }
    }

    /**
     * La plus petite fenêtre (version 0.092) : en dessous, plus rien ne tient. Elle suit la taille de
     * l'interface, sans dépasser l'écran ; une fenêtre déjà plus petite est agrandie jusqu'à elle.
     */
    void applyMinWindow() {
        if (stage == null) return;
        double[] least = Settings.minWindow(settings.scale());
        javafx.geometry.Rectangle2D screen = screenBounds();
        if (screen != null) {
            least[0] = Math.min(least[0], screen.getWidth());
            least[1] = Math.min(least[1], screen.getHeight());
        }
        stage.setMinWidth(least[0]);
        stage.setMinHeight(least[1]);
        if (!stage.isMaximized()) {
            if (stage.getWidth() > 0 && stage.getWidth() < least[0]) stage.setWidth(least[0]);
            if (stage.getHeight() > 0 && stage.getHeight() < least[1]) stage.setHeight(least[1]);
        }
    }

    /** La place utilisable de l'écran où est la fenêtre, ou {@code null} sans écran à interroger. */
    private javafx.geometry.Rectangle2D screenBounds() {
        try {
            List<Screen> screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), Math.max(1, stage.getWidth()), Math.max(1, stage.getHeight()));
            return (screens.isEmpty() ? Screen.getPrimary() : screens.get(0)).getVisualBounds();
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    /** Vrai si ce format tient sur l'écran. */
    boolean windowSizeFits(Settings.WindowSize size) {
        if (size.width() <= 0) return true;
        javafx.geometry.Rectangle2D screen = screenBounds();
        return screen == null || (size.width() <= screen.getWidth() && size.height() <= screen.getHeight());
    }

    /** Le format de la fenêtre en ce moment, ou {@code null} si elle a été tirée à la main. */
    Settings.WindowSize currentWindowSize() {
        if (stage == null) return null;
        if (stage.isFullScreen()) return Settings.WindowSize.FULL;
        if (stage.isMaximized()) return Settings.WindowSize.MAXIMIZED;
        for (Settings.WindowSize size : Settings.WindowSize.values()) {
            if (size.width() > 0 && Math.abs(stage.getWidth() - size.width()) < 2
                    && Math.abs(stage.getHeight() - size.height()) < 2) return size;
        }
        return null;
    }

    /** Donne à la fenêtre un format prédéfini, recentrée sur son écran (version 0.092, plein écrans en 0.093). */
    void applyWindowSize(Settings.WindowSize size) {
        if (stage == null) return;
        if (size == Settings.WindowSize.FULL) {
            stage.setFullScreenExitHint("Échap pour quitter le plein écran");
            stage.setFullScreen(true);
            return;
        }
        stage.setFullScreen(false);
        if (size == Settings.WindowSize.MAXIMIZED) {
            stage.setMaximized(true);
            return;
        }
        stage.setMaximized(false);
        javafx.geometry.Rectangle2D screen = screenBounds();
        double width = size.width(), height = size.height();
        if (screen != null) {
            width = Math.min(width, screen.getWidth());
            height = Math.min(height, screen.getHeight());
        }
        stage.setWidth(width);
        stage.setHeight(height);
        if (screen != null) {
            stage.setX(screen.getMinX() + (screen.getWidth() - width) / 2);
            stage.setY(screen.getMinY() + (screen.getHeight() - height) / 2);
        }
    }

    /** Remet la fenêtre où elle était à la dernière fermeture, si cet endroit est encore sur un écran. */
    void placeWindow() {
        double[] place = settings.window();
        // Première ouverture (version 0.094) : le format Petit, recentré et ramené à l'écran s'il est plus petit.
        if (place == null) {
            applyWindowSize(Settings.WindowSize.SMALL);
            return;
        }
        try {
            if (Screen.getScreensForRectangle(place[0], place[1], place[2], place[3]).isEmpty()) {
                applyWindowSize(Settings.WindowSize.SMALL);     // l'ancienne place n'est plus sur un écran
                return;
            }
        } catch (RuntimeException unavailable) {
            return;     // pas d'écran à interroger : on garde la place par défaut
        }
        stage.setX(place[0]);
        stage.setY(place[1]);
        double[] least = Settings.minWindow(settings.scale());
        stage.setWidth(Math.max(least[0], place[2]));
        stage.setHeight(Math.max(least[1], place[3]));
        if (place[4] == 1) stage.setMaximized(true);
    }

    /** Écrit la partie dans son fichier. */
    void save() {
        sinceSave = 0;
        store.save(game);
    }

    /**
     * Une image du rattrapage d'une absence : le jeu rejoue autant de temps qu'il peut sans figer
     * la fenêtre. Ce qui se passe pendant ce temps n'est pas annoncé message par message : le
     * panneau en fait le bilan à la fin.
     */
    void catchUp() {
        absencePane.absence().advanceFor(CATCH_UP_NANOS);
        notifier.poll();
        // Arrivé au bout, le panneau fait écrire la partie, et se ferme s'il n'a rien à annoncer.
        absencePane.refresh();
        refresh();
    }

    /**
     * La partie vient d'être remplacée par une autre (importée) : la fenêtre oublie ce qu'elle
     * avait retenu de la précédente, comme après une remise à zéro.
     */
    void afterLoad() {
        toasts.clear();
        popupPane.forget();
        absencePane.close();
        endPane.close();
        transitionPane.close();
        exploding = false;
        explosionArmed = 0;
        blast.setVisible(false);
        lastTotalAtoms = -1;
        lastBigBangs = -1;
        hadUniverse = -1;
        particlesTabPop = -1;
        particlesTab.setScaleX(1);
        particlesTab.setScaleY(1);
        universePage.forgetOldTabs();
        dimensionWindow.close();
        generators.setCount(0);
        darkMatterPage.reset();
        bigBangPage.reset();
        notifier.forget();
        profilePage.forget();
        EventLog.of(game).clear();
        if (game.isStarted()) {
            if (startButton != null) center.getChildren().remove(startButton);
            startButton = null;
            controls.setOpacity(1);
            controls.setVisible(true);
        } else {
            controls.setVisible(false);
            showStartButton();
        }
        refresh();
    }

    /**
     * Bouton de l'écran de démarrage : il crée le premier générateur, s'efface,
     * puis le compteur et les améliorations apparaissent.
     */
    void showStartButton() {
        if (startButton != null) return;
        Button button = new Button("Créer le premier générateur");
        startButton = button;
        button.setStyle("-fx-font-size: 16px; -fx-padding: 12 28; -fx-text-fill: #e8f4ff;"
                + " -fx-background-color: #16202e; -fx-background-radius: 8;"
                + " -fx-border-color: #9fd0ff; -fx-border-radius: 8; -fx-cursor: hand;");

        // Le bouton « respire » pour attirer l'œil.
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.1), button);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.06);
        pulse.setToY(1.06);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        button.setOnAction(event -> {
            pulse.stop();
            button.setDisable(true);
            game.start(); // le générateur apparaît à l'image suivante, avec son animation de naissance

            FadeTransition hideButton = new FadeTransition(Duration.seconds(0.3), button);
            hideButton.setToValue(0);
            hideButton.setOnFinished(done -> {
                center.getChildren().remove(button);
                if (startButton == button) startButton = null;
            });
            hideButton.play();

            // Les commandes arrivent une fois le générateur allumé.
            controls.setOpacity(0);
            controls.setVisible(true);
            FadeTransition showControls = new FadeTransition(Duration.seconds(0.8), controls);
            showControls.setDelay(Duration.seconds(1.2));
            showControls.setToValue(1);
            showControls.play();
        });
        center.getChildren().add(button);
    }

    /**
     * Le joueur a confirmé qu'il recommence de zéro : le jeu efface tout, et la fenêtre revient à
     * l'écran de démarrage.
     */
    void resetGame() {
        if (debugBar != null) debugBar.cancelFastForward();
        game.reset();
        game.restored();
        save();
        absencePane.close();
        endPane.close();
        transitionPane.close();
        helpPane.close();
        popupPane.forget();
        toasts.clear();
        forgetOldPages();
        profilePage.forget();
        hadUniverse = 0;
        universePage.forgetOldTabs();
        dimensionWindow.close();
        controls.setVisible(false);
        showStartButton();
        selectTab(Tab.PARTICLES);
        refresh();
    }

    /**
     * Les anciens onglets oublient ce qu'ils montraient : la partie vient de repartir de zéro, par
     * « Recommencer » ou par la formation de l'univers (version 0.083).
     */
    void forgetOldPages() {
        exploding = false;
        explosionArmed = 0;
        blast.setVisible(false);
        lastTotalAtoms = -1;
        generators.setCount(0);
        darkMatterPage.reset();
        bigBangPage.reset();
        lastBigBangs = 0;
    }

    /** Le régime, la synergie et la prime de cycle parfait : cachés tant que rien n'est ouvert. */
    void refreshSecondUniverse() {
        boolean regime = game.isRegimeUnlocked();
        boolean synergy = game.isSynergyUnlocked();
        boolean perfect = game.isPerfectCycleUnlocked();
        secondRow.setVisible(regime || synergy || perfect);
        secondRow.setManaged(regime || synergy || perfect);
        regimeButton.setVisible(regime);
        regimeButton.setManaged(regime);
        double left = game.state().regimeLeft(), rest = game.state().regimeRest();
        regimeButton.setText(left > 0 ? "Régime rapide : +" + Math.round(Game.REGIME_GAIN * 100) + " % encore " + Format.wait(left)
                : rest > 0 ? "Régime : repos " + Format.wait(rest) : "Régime rapide : +" + Math.round(Game.REGIME_GAIN * 100) + " % pendant "
                        + Math.round(Game.REGIME_SECONDS) + " s");
        regimeButton.setDisable(left <= 0 && !game.canStartRegime());     // en cours : éclairé, pas grisé
        regimeButton.setStyle("-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + (left > 0 ? " -fx-text-fill: #0b0e14; -fx-background-color: " + PARTICLES_COLOR + ";"
                        : " -fx-text-fill: " + PARTICLES_COLOR + "; -fx-background-color: #101c2b; -fx-border-color: " + PARTICLES_COLOR + "88;"));
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (synergy) parts.add("Synergie des rangs : +" + ElementText.percent(game.synergyMultiplier() - 1) + " à chaque générateur");
        if (perfect) parts.add(game.perfectCycleMultiplier() > 1
                ? "Cycle parfait : particules +" + Math.round(Game.PERFECT_GAIN * 100) + " % encore " + Format.wait(game.state().perfectLeft())
                : "Cycle parfait : " + game.state().perfectStreak() + " / " + Game.PERFECT_FUSIONS + " fusions sans le plafond d'atomes");
        secondLabel.setText(String.join("   |   ", parts));
    }

    /**
     * Carte de fusion : elle n'apparaît que lorsque tous les générateurs sont débloqués.
     * Les générateurs se regroupent au centre, puis le jeu applique la fusion.
     */
    Card fuseCard() {
        fuseCard.setPrefWidth(FUSE_WIDTH);
        fuseCard.setMaxWidth(FUSE_WIDTH);
        fuseCard.setVisible(false); // sa place reste réservée pour que rien ne bouge à son apparition

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.1), fuseCard);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.04);
        pulse.setToY(1.04);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        fuseCard.setOnAction(this::fuse);
        return fuseCard;
    }

    /** La fusion demandée par le joueur, au bouton ou au clavier : les générateurs se regroupent, puis le jeu fusionne. */
    private void fuse() {
        if (!game.canFuse() || generators.isFusing()) return;
        if (selectedTab != Tab.PARTICLES) {
            game.fuse();     // l'onglet n'est pas affiché : personne ne verrait l'animation
            refresh();
            return;
        }
        generators.fuse(() -> {
            game.fuse(); // un seul générateur renaît à l'image suivante, là où les autres ont fusionné
            refresh();   // fait apparaître les onglets si c'est le premier atome
        });
        refresh();
    }

    /** Le raccourci qui achète cette amélioration, ou {@code null} si elle n'en a pas. */
    static Shortcuts.Action actionOf(Upgrade upgrade) {
        return switch (upgrade.effect()) {
            case Effect.MultiplySpeed speed -> Shortcuts.Action.SPEED;
            case Effect.MultiplyByGenerators coupling -> Shortcuts.Action.COUPLING;
            case Effect.AddGenerator generator -> Shortcuts.Action.GENERATOR;
            default -> null;
        };
    }

    /** Touche du clavier qui achète cette amélioration, ou {@code null} si elle n'en a pas. */
    KeyCode keyOf(Upgrade upgrade) {
        Shortcuts.Action action = actionOf(upgrade);
        return action == null ? null : settings.key(action);
    }

    /**
     * Une touche vient d'être enfoncée. Dans l'ordre : la page des réglages l'attend peut-être
     * pour en faire la touche de détail ; sinon c'est la touche de détail elle-même ; sinon un
     * raccourci.
     *
     * @return vrai si la touche a servi à l'interface et ne doit pas aller plus loin
     */
    /**
     * Vrai si le clavier est dans une zone de saisie visible : une barre de recherche. Une zone
     * cachée avec sa page ne compte pas, même si la fenêtre lui a laissé le clavier.
     */
    static boolean typing(Scene scene) {
        javafx.scene.Node owner = scene.getFocusOwner();
        if (!(owner instanceof javafx.scene.control.TextInputControl)) return false;
        for (javafx.scene.Node node = owner; node != null; node = node.getParent()) {
            if (!node.isVisible()) return false;
        }
        return true;
    }

    boolean keyPressed(KeyCode code) {
        // Le plan (version 0.104) : Z Q S D et les flèches déplacent son point, avant tout raccourci.
        if (dimensionWindow.key(code, true)) return true;
        if (settingsPage.isCapturing()) {
            settingsPage.capture(code);
            return true;
        }
        // L'aide ouverte prend le clavier : Échap ou son raccourci la ferment, le reste attend.
        if (helpPane.isOpen() && code != settings.detailKey()) {
            if (code == KeyCode.ESCAPE || code == settings.key(Shortcuts.Action.HELP)) {
                helpPane.close();
                refresh();
            }
            return true;
        }
        // Le guide de transition (version 0.127) prend le clavier : Échap le ferme sans rien changer, le reste attend.
        if (transitionPane.isOpen()) {
            if (code == KeyCode.ESCAPE) transitionPane.close();
            // Entrée confirme, comme le bouton mis en avant : le guide se lit et se valide au clavier.
            else if (code == KeyCode.ENTER) transitionPane.confirm();
            return true;
        }
        if (code == settings.detailKey()) {
            // Tenue, la touche se répète : seul le premier appui compte.
            if (!detailKeyDown) {
                detailKeyDown = true;
                Detail.set(settings.detailToggle() ? !Detail.shown() : true);
                refresh();
            }
            return true;
        }
        // Une touche tenue se répète : le système renvoie des appuis, mais c'est la fenêtre qui fixe le
        // rythme, pour toutes les touches tenues à la fois (M et F ensemble, par exemple).
        Shortcuts.Action action = settings.actionOf(code);
        if (action != null && heldKeys.containsKey(code)) return settings.shortcuts();
        boolean used = onKey(code);
        if (used) heldKeys.put(code, REPEATABLE.contains(action) ? REPEAT_DELAY : Double.POSITIVE_INFINITY);
        return used;
    }

    /** Une touche vient d'être relâchée : elle cesse de se répéter ; si c'est la touche de détail tenue, les détails se referment. */
    boolean keyReleased(KeyCode code) {
        if (dimensionWindow.key(code, false)) return true;
        if (heldKeys.remove(code) != null) return true;
        if (code != settings.detailKey()) return false;
        releaseDetailKey();
        return true;
    }
    /** Attente avant la première répétition, puis intervalle entre deux répétitions, en secondes. */
    static final double REPEAT_DELAY = 0.3;
    static final double REPEAT_INTERVAL = 0.08;
    /** Les raccourcis qui se répètent tant que la touche est tenue ; les autres (exploser, couper…) ne jouent qu'une fois. */
    static final java.util.Set<Shortcuts.Action> REPEATABLE = java.util.EnumSet.of(Shortcuts.Action.SPEED,
            Shortcuts.Action.COUPLING, Shortcuts.Action.GENERATOR, Shortcuts.Action.BUY_ALL, Shortcuts.Action.FUSE,
            Shortcuts.Action.SYNTHESIZE, Shortcuts.Action.PULSE);

    /** Répète les raccourcis tenus dont le délai est écoulé. */
    void repeatHeldKeys(double dt) {
        if (heldKeys.isEmpty() || !settings.shortcuts() || exploding || absencePane.isOpen() || helpPane.isOpen()) return;
        boolean any = false;
        for (java.util.Map.Entry<KeyCode, Double> entry : heldKeys.entrySet()) {
            double wait = entry.getValue();
            if (Double.isInfinite(wait)) continue;
            wait -= dt;
            Shortcuts.Action action = settings.actionOf(entry.getKey());
            // Au plus quelques répétitions par image : une image en retard ne déclenche pas une rafale.
            for (int burst = 0; wait <= 0 && burst < 4; burst++) {
                if (action != null && game.isStarted()) play(action);
                wait += REPEAT_INTERVAL;
                any = true;
            }
            entry.setValue(Math.max(wait, 0));
        }
        if (any) refresh();
    }

    void releaseDetailKey() {
        detailKeyDown = false;
        if (!settings.detailToggle() && Detail.shown()) {
            Detail.set(false);
            refresh();
        }
    }

    /**
     * Les raccourcis clavier ({@link Shortcuts}), actifs depuis n'importe quel onglet, avec les
     * touches que le joueur leur a données ({@link Settings#key}). Un raccourci dont l'action
     * n'est pas possible en ce moment ne fait rien.
     *
     * @return vrai si la touche portait un raccourci : elle ne va alors pas plus loin
     */
    private boolean onKey(KeyCode code) {
        Shortcuts.Action action = settings.actionOf(code);
        if (action == null || !settings.shortcuts() || exploding || absencePane.isOpen()) return false;
        switch (action) {
            case PAUSE -> {
                settings.setPaused(!settings.paused());
                settingsPage.refresh();
            }
            case HELP -> helpPane.toggle();
            case PREVIOUS_TAB -> stepTab(-1);
            case NEXT_TAB -> stepTab(1);
            case PREVIOUS_SUB_TAB -> stepSubTab(-1);
            case NEXT_SUB_TAB -> stepSubTab(1);
            default -> {
                // Dans la partie secondaire, les anciens onglets sont partis : leurs raccourcis ne jouent plus.
                if (game.isStarted() && !universePhase()) play(action);
            }
        }
        refresh();
        return true;
    }

    /** Les raccourcis qui jouent : acheter, fusionner, synthétiser, exploser, couper les automatismes. */
    private void play(Shortcuts.Action action) {
        switch (action) {
            case FUSE -> fuse();
            case BUY_ALL -> game.buyAllWithParticles();
            case SYNTHESIZE -> game.synthesize();
            case PULSE -> game.pulse();
            case CLAIM -> {
                // Le butin ouvert : la carte suivante ; sinon, celui de la première sonde arrivée.
                if (lootPane.isOpen()) lootPane.next();
                else if (!lootPane.openFirst()) game.claimProbeReward();
            }
            case EXPLODE -> {
                if (game.canExplode()) requestExplosion();
            }
            case BIG_BANG -> {
                // Le Big Bang se déclenche sur sa case, au bout de l'arbre : le raccourci y mène et la presse.
                if (game.canBigBang()) {
                    selectTab(Tab.DARK_MATTER);
                    darkMatterPage.pressBigBang();
                }
            }
            case AUTOMATION -> {
                if (AutomationPage.anyToPause(game)) game.setAutomationPaused(!game.isAutomationPaused());
            }
            default -> {
                Upgrade upgrade = upgradeActions.get(action);
                if (upgrade != null) game.buy(upgrade.id(), settings.buyAmount().count());
            }
        }
    }

    /** Passe à l'onglet visible suivant ou précédent, en boucle. */
    private void stepTab(int direction) {
        // Dans l'ordre affiché, celui que le joueur a pu choisir en glissant les onglets.
        int index = tabOrder.indexOf(selectedTab);
        for (int tries = 0; tries < tabOrder.size(); tries++) {
            index = Math.floorMod(index + direction, tabOrder.size());
            Tab tab = tabOrder.get(index);
            if (tabButtons[tab.ordinal()].isVisible()) {
                selectTab(tab);
                return;
            }
        }
    }
}

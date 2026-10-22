package idle.ui;

import idle.core.Game;
import idle.core.upgrades.Effect;
import idle.ui.collider.ColliderPane;
import idle.ui.common.TabIcon;
import idle.ui.universe.UniversePage;
import idle.ui.particles.ParticleView;
import idle.ui.probes.ProbesPane;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * L'application : l'ordre et le style des onglets, l'explosion, les fenêtres d'explication, les annonces et l'animation.
 */
public abstract class GameAppTabs extends GameAppWindow {

    /** Seul {@link GameApp} prolonge la chaîne. */
    GameAppTabs() {
    }


    // ------------------------------------------------------------------
    // L'ordre des onglets, qu'on change en les glissant (version 0.060)
    // ------------------------------------------------------------------

    /** Temps d'appui avant qu'un onglet ne se laisse glisser, en nanosecondes : un clic bref reste un clic. */
    private static final long HOLD_NANOS = 300_000_000L;

    /** L'ordre des onglets lu dans les réglages ; les onglets qu'il ne cite pas suivent, dans l'ordre d'origine. */
    static List<Tab> readTabOrder(String saved) {
        List<Tab> order = new java.util.ArrayList<>();
        for (String name : saved.split(",")) {
            try {
                Tab tab = Tab.valueOf(name.trim());
                if (!order.contains(tab)) order.add(tab);
            } catch (IllegalArgumentException unknown) {
                // un onglet d'une autre version, ou rien : écarté
            }
        }
        // Un onglet que l'ordre ne cite pas (un nouveau, comme l'univers en 0.080) se range après celui qui le
        // précède d'habitude, pas au bout : les pages pour s'y retrouver restent à part.
        Tab[] natural = Tab.values();
        for (int k = 0; k < natural.length; k++) {
            if (order.contains(natural[k])) continue;
            int at = 0;
            for (int before = k - 1; before >= 0; before--) {
                int place = order.indexOf(natural[before]);
                if (place >= 0) {
                    at = place + 1;
                    break;
                }
            }
            order.add(at, natural[k]);
        }
        return order;
    }

    /**
     * Range les boutons des onglets dans leur barre, dans l'ordre choisi. Les pages pour s'y retrouver
     * (succès, statistiques, réglages) restent à part, après un écart, tant qu'elles sont toutes au bout.
     */
    void layoutTabs() {
        javafx.scene.layout.Pane holder = tabsLeftApplied ? sideBar : tabBar;
        holder.getChildren().clear();
        int firstMeta = tabOrder.size();
        while (firstMeta > 0 && roleOf(tabOrder.get(firstMeta - 1)) == TabRole.META) firstMeta--;
        for (int i = 0; i < tabOrder.size(); i++) {
            if (i == firstMeta && firstMeta > 0) holder.getChildren().add(tabsLeftApplied ? sideSpacer : tabGap);
            holder.getChildren().add(tabButtons[tabOrder.get(i).ordinal()]);
        }
    }

    /** Branche le glisser-déposer sur le bouton d'un onglet : un appui long, puis on le déplace. */
    void makeDraggable(Tab tab) {
        Button button = tabButtons[tab.ordinal()];
        button.setOnMousePressed(event -> {
            pressedAt = System.nanoTime();
            justDragged = false;
        });
        button.setOnMouseDragged(event -> {
            if (draggedTab == null) {
                if (System.nanoTime() - pressedAt < HOLD_NANOS) return;
                startTabDrag(tab);
            }
            dragTab(tabsLeftApplied ? event.getY() : event.getX());
        });
        button.setOnMouseReleased(event -> endTabDrag());
    }

    /** Un onglet est saisi : il pâlit, et suivra la souris. */
    void startTabDrag(Tab tab) {
        draggedTab = tab;
        justDragged = true;
        tabButtons[tab.ordinal()].setOpacity(0.6);
    }

    /**
     * L'onglet saisi suit la souris : {@code offset} est la position de la souris le long de la
     * barre, comptée depuis le bord de l'onglet. Passé la moitié de son voisin, il prend sa place.
     */
    void dragTab(double offset) {
        if (draggedTab == null) return;
        Button button = tabButtons[draggedTab.ordinal()];
        int at = tabOrder.indexOf(draggedTab);
        Tab next = neighbour(at, 1), previous = neighbour(at, -1);
        double size = tabsLeftApplied ? button.getHeight() : button.getWidth();
        if (next != null && offset > size + extent(next) / 2) {
            moveTab(draggedTab, tabOrder.indexOf(next));
        } else if (previous != null && offset < -extent(previous) / 2) {
            moveTab(draggedTab, tabOrder.indexOf(previous));
        }
    }

    /** L'onglet lâché : son nouvel ordre est gardé dans les réglages. */
    void endTabDrag() {
        if (draggedTab == null) return;
        tabButtons[draggedTab.ordinal()].setOpacity(1);
        draggedTab = null;
        StringBuilder saved = new StringBuilder();
        for (Tab tab : tabOrder) saved.append(saved.length() == 0 ? "" : ",").append(tab.name());
        settings.setTabOrder(saved.toString());
    }

    /** Le premier onglet affiché dans ce sens, ou {@code null} au bout de la barre. */
    private Tab neighbour(int from, int direction) {
        for (int i = from + direction; i >= 0 && i < tabOrder.size(); i += direction) {
            if (tabButtons[tabOrder.get(i).ordinal()].isManaged()) return tabOrder.get(i);
        }
        return null;
    }

    private double extent(Tab tab) {
        Button button = tabButtons[tab.ordinal()];
        return tabsLeftApplied ? button.getHeight() : button.getWidth();
    }

    /** Déplace un onglet à cette place de l'ordre, et refait la barre. */
    void moveTab(Tab tab, int to) {
        tabOrder.remove(tab);
        tabOrder.add(Math.max(0, Math.min(to, tabOrder.size())), tab);
        layoutTabs();
    }

    /** Remet les onglets dans leur ordre d'origine. */
    void resetTabOrder() {
        tabOrder.clear();
        tabOrder.addAll(List.of(Tab.values()));
        settings.setTabOrder("");
        layoutTabs();
    }

    /** L'ordre des onglets : pour les vérifications. */
    List<String> tabOrderNames() {
        List<String> names = new java.util.ArrayList<>();
        for (Tab tab : tabOrder) names.add(tab.name());
        return names;
    }

    /** Passe au sous-onglet suivant ou précédent de l'onglet affiché, s'il en a. */
    void stepSubTab(int direction) {
        switch (selectedTab) {
            case ATOMS -> atomsPage.step(direction);
            case DARK_MATTER -> darkMatterPage.step(direction);
            case COLLIDER -> colliderPage.step(direction);
            case BIG_BANG -> bigBangPage.step(direction);
            case UNIVERSE -> universePage.step(direction);
            case STATS -> statsPage.step(direction);
            default -> { }
        }
    }

    /**
     * Un clic sur le bouton d'explosion. Avec la confirmation des réglages, le premier clic arme
     * le bouton et c'est le second, dans les secondes qui suivent, qui déclenche l'explosion.
     */
    void requestExplosion() {
        if (!game.canExplode() || exploding) return;
        if (settings.confirmExplosion() && explosionArmed <= 0) {
            explosionArmed = EXPLOSION_CONFIRM_SECONDS;
            refresh();
            return;
        }
        explode();
    }

    /**
     * L'explosion du tableau périodique : une onde part du centre et la fenêtre vire au blanc ;
     * au plus fort de l'éclair, le jeu remet tout à zéro, puis l'écran revient sur l'onglet
     * « Matière noire ». La fenêtre ne réagit à aucun clic tant que l'explosion n'est pas finie.
     */
    private void explode() {
        if (exploding || !game.canExplode()) return;
        explosionArmed = 0;
        if (!settings.effect(Settings.Effect.FLASH)) {
            // Sans l'éclair : pas d'onde ni d'éclair, l'explosion a lieu tout de suite.
            game.explode();
            selectTab(Tab.DARK_MATTER);
            refresh();
            return;
        }
        exploding = true;
        blast.setVisible(true);
        flash.setOpacity(0);
        // Le skin de l'animation (version 0.089) : une spirale ou un pulsar à la place de l'onde, et l'éclair à sa couleur.
        idle.core.universe.Skin look = game.chosenSkin(idle.core.universe.SkinVisual.DARK_ANIMATION);
        boolean skinned = look == idle.core.universe.Skin.DARK_VORTEX || look == idle.core.universe.Skin.DARK_PULSAR;
        flash.setStyle("-fx-background-color: " + (look == idle.core.universe.Skin.DARK_VORTEX ? "#12061f"
                : look == idle.core.universe.Skin.DARK_PULSAR ? "#e8fdff" : "#ffffff") + ";");
        shockwave.setVisible(!skinned);
        if (skinned) burst.play(look, 2.4);

        ScaleTransition grow = new ScaleTransition(Duration.seconds(1.1), shockwave);
        grow.setFromX(0.1);
        grow.setFromY(0.1);
        grow.setToX(60);
        grow.setToY(60);
        grow.setInterpolator(Interpolator.EASE_IN); // départ lent, arrivée rapide
        FadeTransition whiten = new FadeTransition(Duration.seconds(0.7), flash);
        whiten.setDelay(Duration.seconds(0.4));
        whiten.setFromValue(0);
        whiten.setToValue(1);
        ParallelTransition bang = new ParallelTransition();
        bang.getChildren().add(grow);
        bang.getChildren().add(whiten);
        bang.setOnFinished(done -> {
            game.explode();
            shockwave.setVisible(false);
            selectTab(Tab.DARK_MATTER);
            refresh();

            FadeTransition clear = new FadeTransition(Duration.seconds(1.4), flash);
            clear.setFromValue(1);
            clear.setToValue(0);
            clear.setOnFinished(end -> {
                blast.setVisible(false);
                exploding = false;
            });
            clear.play();
        });
        bang.play();
    }

    /** Applique la taille de l'interface, le thème et la disposition des onglets choisis dans les réglages. */
    void applyAppearance() {
        if (scaled == null) return;      // la fenêtre n'est pas encore construite
        scaled.setScale(settings.scale().factor());
        applyMinWindow();
        themeFilter.apply(settings.theme());
        applyTabLayout();
        // L'ordre des onglets a pu être remis à l'origine dans les réglages.
        List<Tab> wanted = readTabOrder(settings.tabOrder());
        if (!wanted.equals(tabOrder)) {
            tabOrder.clear();
            tabOrder.addAll(wanted);
            layoutTabs();
        }
    }

    /** Retient une barre de sous-onglets et sa place dans sa page. */
    void dock(Tab tab, javafx.scene.layout.Pane page, javafx.scene.layout.Pane bar) {
        docks.add(new SubTabDock(tab, page, bar, page.getChildren().indexOf(bar)));
    }

    /**
     * Range les onglets selon le réglage. À gauche : les onglets en colonne, et la barre du haut
     * prend les sous-onglets de l'onglet affiché. En haut : la disposition d'avant, chaque barre
     * de sous-onglets revenant à sa place dans sa page.
     */
    private void applyTabLayout() {
        boolean left = settings.tabsLeft();
        if (left == tabsLeftApplied || topBox == null) return;
        tabsLeftApplied = left;
        tabBar.getChildren().clear();
        sideBar.getChildren().clear();
        layoutTabs();
        if (left) {
            int at = topBox.getChildren().indexOf(tabBar);
            topBox.getChildren().set(at, subTabHolder);
            for (SubTabDock dock : docks) {
                dock.page().getChildren().remove(dock.bar());
                subTabHolder.getChildren().add(dock.bar());
            }
            // La colonne des onglets descend du haut au bas de la fenêtre : ni le bouton d'explosion ni la
            // barre du haut ne la font bouger.
            root.setTop(null);
            root.setCenter(null);
            mainColumn.setTop(topBox);
            mainColumn.setCenter(pages);
            root.setCenter(mainColumn);
            root.setLeft(sideBar);
        } else {
            int at = topBox.getChildren().indexOf(subTabHolder);
            topBox.getChildren().set(at, tabBar);
            for (SubTabDock dock : docks) {
                subTabHolder.getChildren().remove(dock.bar());
                dock.page().getChildren().add(Math.min(dock.index(), dock.page().getChildren().size()), dock.bar());
                dock.bar().setVisible(true);
                dock.bar().setManaged(true);
            }
            root.setLeft(null);
            mainColumn.setTop(null);
            mainColumn.setCenter(null);
            root.setCenter(pages);
            root.setTop(topBox);
        }
        showSubTabs();
        styleTabs();
    }

    /** Dans la disposition à gauche, seule la barre de l'onglet affiché se montre en haut. */
    private void showSubTabs() {
        if (!tabsLeftApplied) return;
        boolean any = false;
        for (SubTabDock dock : docks) {
            boolean shown = dock.tab() == selectedTab;
            dock.bar().setVisible(shown);
            dock.bar().setManaged(shown);
            any |= shown;
        }
        // Sans sous-onglets, la barre garde sa place et montre le nom de l'onglet.
        tabTitle.setVisible(!any);
        tabTitle.setText(tabName(selectedTab));
        tabTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + tabColor(selectedTab) + ";");
    }

    /**
     * Vrai dans la partie secondaire (version 0.081) : l'univers est formé, et les anciens onglets sont
     * cachés (sauf si l'outil d'essai les a fait revenir).
     */
    /** L'onglet Particules réapparaît (version 0.111) : il grandit, dépasse un peu, revient ; puis on y va. */
    void popParticlesTab(double dt) {
        particlesTabPop += Math.min(Math.max(0, dt), 0.1);
        double t = Math.min(1, particlesTabPop / PARTICLES_TAB_POP_SECONDS);
        // Une montée qui dépasse de 15 % avant de se poser.
        double scale = t < 0.6 ? 0.2 + (1.15 - 0.2) * (t / 0.6) : 1.15 - 0.15 * ((t - 0.6) / 0.4);
        particlesTab.setScaleX(scale);
        particlesTab.setScaleY(scale);
        if (t >= 1) {
            particlesTabPop = -1;
            particlesTab.setScaleX(1);
            particlesTab.setScaleY(1);
            selectTab(Tab.PARTICLES);
            refresh();
        }
    }

    boolean universePhase() {
        return game.hasCosmos(idle.core.sky.Cosmos.UNIVERSE) && !universePage.showsOldTabs();
    }

    /**
     * Les fenêtres d'explication (version 0.089) : chaque onglet qui s'ouvre, puis les nouveautés du
     * deuxième univers. Elles attendent qu'aucune autre fenêtre n'ait la main.
     */
    void checkPopups() {
        if (popupEntries == null) {
            List<PopupPane.Entry> entries = new java.util.ArrayList<>();
            for (Tab tab : Tab.values()) {
                String text = PopupPane.tabText(tab.name());
                if (text == null) continue;
                Button button = tabButtons[tab.ordinal()];
                entries.add(new PopupPane.Entry("tab:" + tab.name(), "Nouvel onglet", tabName(tab), text, tabColor(tab),
                        () -> game.isStarted() && button.isVisible()));
            }
            entries.addAll(PopupPane.secondUniverse(game));
            popupEntries = List.copyOf(entries);
        }
        boolean busy = absencePane.isVisible() || endPane.isOpen() || exploding
                || (selectedTab == Tab.UNIVERSE && universePage.isBirthPlaying()) || darkMatterPage.isRevealing();
        popupPane.check(popupEntries, busy);
    }

    /** Les fenêtres d'explication : pour les vérifications. */
    PopupPane popupPane() {
        return popupPane;
    }

    /** Le nom d'un onglet, sans compteur. */
    private static String tabName(Tab tab) {
        return switch (tab) {
            case PARTICLES -> "Particules";
            case ATOMS -> "Atomes";
            case AUTOMATION -> "Automatisation";
            case DARK_MATTER -> "Matière noire";
            case COLLIDER -> "Collisionneur";
            case BIG_BANG -> "Big Bang";
            case UNIVERSE -> "Univers";
            case VOID -> "Le vide";
            case ACHIEVEMENTS -> "Succès";
            case STATS -> "Statistiques";
            case SETTINGS -> "Réglages";
            case SKINS -> "Skins";
        };
    }

    private static String tabColor(Tab tab) {
        return switch (tab) {
            case PARTICLES -> PARTICLES_COLOR;
            case ATOMS -> ATOMS_COLOR;
            case AUTOMATION -> AUTOMATION_COLOR;
            case DARK_MATTER -> DARK_MATTER_COLOR;
            case COLLIDER -> ColliderPane.COLOR;
            case BIG_BANG -> BIG_BANG_COLOR;
            case UNIVERSE -> UniversePage.COLOR;
            case VOID -> idle.ui.vacuum.VoidPage.COLOR;
            case ACHIEVEMENTS -> ACHIEVEMENTS_COLOR;
            case STATS -> STATS_COLOR;
            case SETTINGS -> SETTINGS_COLOR;
            case SKINS -> idle.ui.pages.SkinsPage.COLOR;
        };
    }

    /** Le nom de l'onglet affiché en haut, dans la disposition à gauche : pour les vérifications. */
    String tabTitleText() {
        return tabTitle.isVisible() ? tabTitle.getText() : "";
    }

    /** Vrai si les onglets sont à gauche : pour les vérifications. */
    boolean tabsOnLeft() {
        return tabsLeftApplied;
    }

    /**
     * Annonce ce qui vient de se passer dans le jeu, et fait vieillir les messages déjà affichés.
     * Coupées dans les réglages, les notifications sont tout de même relevées, pour ne pas toutes
     * tomber d'un coup le jour où on les rallume.
     */
    void announce(double dt) {
        List<String> news = notifier.poll();
        // Dans la partie secondaire, ce qui se passe dans les anciens onglets, cachés, n'est plus annoncé :
        // seuls les succès le sont encore. Le journal, lui, garde tout.
        if (universePhase()) {
            for (String text : news) if (!text.startsWith("Succès")) EventLog.of(game).add(game, text);
            news = news.stream().filter(text -> text.startsWith("Succès")).toList();
        }
        sound.announce(news, settings.sound(), settings.volume());
        // Le journal garde tout, que les notifications soient affichées ou non.
        for (String text : news) EventLog.of(game).add(game, text);
        if (settings.notifications()) {
            for (String text : news) toasts.show(text, noticeColor(text));
        } else if (toasts.count() > 0) {
            toasts.clear();
        }
        toasts.frame(dt);
    }

    /** La couleur du liseré d'une notification : celle de l'onglet dont elle parle. */
    private static String noticeColor(String text) {
        if (text.startsWith("Big Bang") || text.startsWith("Molécules") || text.startsWith("Espace") || text.startsWith("Astre mûr") || text.contains("astres mûrs")
                || text.startsWith("Rassemblement automatique") || text.startsWith("Formation automatique")) return BIG_BANG_COLOR;
        if (text.startsWith("Succès du Big Bang")) return BIG_BANG_COLOR;
        if (text.startsWith("Succès") && !text.startsWith("Succès du collisionneur")) return ACHIEVEMENTS_COLOR;
        if (text.startsWith("Skin débloqué")) return idle.ui.pages.SkinsPage.COLOR;
        if (text.startsWith("Sonde") || text.contains("sondes revenues")) return ProbesPane.COLOR;
        if (text.startsWith("Collision") || text.startsWith("Palier du collisionneur") || text.startsWith("Succès du collisionneur")) return ColliderPane.COLOR;
        if (text.startsWith("Explosion") || text.startsWith("Défi") || text.startsWith("Nouveau défi")
                || text.contains("matière noire") || text.startsWith("Palier de taille") || text.contains("paliers de taille")
                || text.contains("exploser")) return DARK_MATTER_COLOR;
        if (text.contains("utomatis")) return AUTOMATION_COLOR;
        if (text.startsWith("Palier de vitesse")) return PARTICLES_COLOR;
        return ATOMS_COLOR;
    }

    static String lowerFirst(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    /** Un clic sur un onglet : il s'affiche, et son contenu est recopié aussitôt, puisqu'il ne l'était plus tant qu'il était caché. */
    void open(Tab tab) {
        // L'appui a servi à glisser l'onglet : on ne l'ouvre pas en le lâchant.
        if (justDragged) {
            justDragged = false;
            return;
        }
        selectTab(tab);
        refresh();
    }

    /** Affiche un onglet et masque les autres. */
    void selectTab(Tab tab) {
        selectedTab = tab;
        // Le profil de test (version 0.122) : la barre d'outils ouvre le groupe de l'onglet affiché.
        if (debugBar != null) debugBar.followTab(tab.name());
        if (tab != Tab.DARK_MATTER) darkMatterPage.release();
        if (tab != Tab.SETTINGS) settingsPage.release();
        particlesPage.setVisible(tab == Tab.PARTICLES);
        atomsPage.setVisible(tab == Tab.ATOMS);
        automationScroll.setVisible(tab == Tab.AUTOMATION);
        darkMatterPage.setVisible(tab == Tab.DARK_MATTER);
        colliderPage.setVisible(tab == Tab.COLLIDER);
        bigBangPage.setVisible(tab == Tab.BIG_BANG);
        universePage.setVisible(tab == Tab.UNIVERSE);
        voidPage.setVisible(tab == Tab.VOID);
        achievementsScroll.setVisible(tab == Tab.ACHIEVEMENTS);
        statsPage.setVisible(tab == Tab.STATS);
        settingsScroll.setVisible(tab == Tab.SETTINGS);
        skinsScroll.setVisible(tab == Tab.SKINS);
        showSubTabs();
        styleTabs();
    }

    /** Colore les onglets : celui qui est affiché est éclairé, et tous se resserrent dans une fenêtre étroite. */
    void styleTabs() {
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            tabButtons[i].setStyle(tabStyle(tabColor(tabs[i]), selectedTab == tabs[i], roleOf(tabs[i])));
        }
        for (TabIcon icon : tabIcons) icon.setBright(false);
        TabIcon shown = iconOf(selectedTab);
        if (shown != null) shown.setBright(true);
        // Sept ou huit onglets ne tiennent pas en toutes lettres dans une fenêtre étroite : en haut, ils
        // perdent alors leur icône et se serrent. À gauche, ils ont toujours la place.
        statsTab.setText(narrow ? "Stats" : "Statistiques");
        boolean icons = tabsLeftApplied || !narrow;
        javafx.scene.Node[] graphics = {particlesIcon, atomsTabIcon, automationIcon, darkMatterIcon, colliderIcon, bigBangIcon, universeIcon, voidIcon, achievementsIcon, statsIcon, settingsIcon, skinsIcon};
        for (int i = 0; i < tabButtons.length; i++) tabButtons[i].setGraphic(icons ? graphics[i] : null);
        tabBar.setSpacing(narrow ? 1 : 4);
        tabGap.setMinWidth(narrow ? 6 : 18);
    }

    private static TabRole roleOf(Tab tab) {
        return switch (tab) {
            case PARTICLES, ATOMS, DARK_MATTER, BIG_BANG, UNIVERSE, VOID -> TabRole.ACT;
            case AUTOMATION, COLLIDER -> TabRole.SIDE;
            case ACHIEVEMENTS, STATS, SETTINGS, SKINS -> TabRole.META;
        };
    }

    private TabIcon iconOf(Tab tab) {
        return switch (tab) {
            case PARTICLES -> particlesIcon;
            case ATOMS -> null;
            case AUTOMATION -> automationIcon;
            case DARK_MATTER -> darkMatterIcon;
            case COLLIDER -> colliderIcon;
            case BIG_BANG -> bigBangIcon;
            case UNIVERSE -> universeIcon;
            case VOID -> voidIcon;
            case ACHIEVEMENTS -> achievementsIcon;
            case STATS -> statsIcon;
            case SETTINGS -> settingsIcon;
            case SKINS -> skinsIcon;
        };
    }

    /**
     * L'allure d'un onglet selon son rôle (version 0.059). Les actes ont un cadre plein dans leur
     * couleur ; ce qui tourne à côté, un cadre en pointillés ; les pages pour s'y retrouver, aucun
     * cadre tant qu'elles ne sont pas affichées. L'onglet affiché est rempli d'un voile de sa couleur
     * et brille. L'épaisseur du cadre, les marges et la police ne changent jamais d'un état à
     * l'autre : rien ne bouge au clic.
     */
    private String tabStyle(String color, boolean selected, TabRole role) {
        String base = "-fx-cursor: hand; -fx-border-width: 1.5;" + (tabsLeftApplied
                ? " -fx-font-size: 14px; -fx-padding: 8 12; -fx-alignment: center-left; -fx-min-width: 176; -fx-max-width: 176;"
                        + " -fx-background-radius: 8; -fx-border-radius: 8;"
                : (narrow ? NARROW_TAB : WIDE_TAB) + " -fx-background-radius: 8 8 0 0; -fx-border-radius: 8 8 0 0;");
        String border = switch (role) {
            case ACT -> " -fx-border-style: solid; -fx-border-color: " + fade(color, selected ? 1 : 0.45) + ";";
            case SIDE -> " -fx-border-style: segments(5, 4); -fx-border-color: " + fade(color, selected ? 1 : 0.5) + ";";
            case META -> " -fx-border-style: solid; -fx-border-color: " + (selected ? fade(color, 0.8) : "transparent") + ";";
        };
        String fill = selected
                ? " -fx-text-fill: " + color + "; -fx-background-color: " + fade(color, 0.14) + ";"
                        + " -fx-effect: dropshadow(gaussian, " + fade(color, 0.55) + ", 12, 0.2, 0, 0);"
                : " -fx-text-fill: " + (role == TabRole.META ? "#7d8fa3" : "#a9bacb") + "; -fx-background-color: "
                        + (role == TabRole.META ? "transparent" : "#111824") + "; -fx-effect: null;";
        return base + border + fill;
    }

    /** Une couleur « #rrggbb » avec une transparence : « rgba(…) » pour le style. */
    private static String fade(String color, double alpha) {
        Color parsed = Color.web(color);
        return "rgba(" + Math.round(parsed.getRed() * 255) + ", " + Math.round(parsed.getGreen() * 255) + ", "
                + Math.round(parsed.getBlue() * 255) + ", " + alpha + ")";
    }

    /** Un atome vient d'être créé : l'onglet « Atomes » bat quelques fois pour attirer l'œil. */
    void celebrateNewAtom() {
        if (!settings.effect(Settings.Effect.ATOM_PULSE)) return;
        ScaleTransition notice = new ScaleTransition(Duration.seconds(0.35), atomsTab);
        notice.setFromX(1);
        notice.setFromY(1);
        notice.setToX(1.15);
        notice.setToY(1.15);
        notice.setAutoReverse(true);
        notice.setCycleCount(6);
        notice.play();
        atomsPage.celebrate();
    }

    /** Affiche les générateurs existants et fait avancer les animations de l'onglet visible. */
    void animate(double dt) {
        // Moins de générateurs dans le jeu qu'à l'écran : une fusion automatique vient d'avoir lieu.
        // On joue la même animation que pour une fusion manuelle, puis les nouveaux générateurs naissent.
        if (Math.min(game.generatorCount(), MAX_VISIBLE_GENERATORS) < generators.views().size()
                && !generators.isFusing()) {
            generators.fuse(() -> { });
        }
        // Au-delà d'un certain nombre, les générateurs tournent sans être dessinés : ils deviendraient illisibles.
        generators.setCount(Math.min(game.generatorCount(), MAX_VISIBLE_GENERATORS));
        if (windowPane != null) burst.frame(dt, windowPane.getWidth(), windowPane.getHeight());
        idle.ui.particles.ParticleView.setSkin(game.chosenSkin(idle.core.universe.SkinVisual.PARTICLE_FUSION));
        // L'atome de l'onglet tourne comme les autres icônes : immobile quand elles le sont.
        atomsTabIcon.frame(settings.effect(Settings.Effect.TAB_ICONS) ? dt : 0);
        // Les icônes suivent ce qu'elles montrent : les particules tournent avec la production, les
        // faisceaux du collisionneur avec leur vitesse.
        double production = Math.min(game.productionPerSecond().toDouble(), 1e12);
        particlesIcon.setSpeed(1 + 3 * ParticleView.intensity(production));
        colliderIcon.setSpeed(Game.RESONANCE_PERIOD / game.colliderPeriod());
        for (TabIcon icon : tabIcons) icon.frame(dt, settings.effect(Settings.Effect.TAB_ICONS));
        if (selectedTab == Tab.ATOMS) {
            atomsPage.frame(dt);
        }
        if (selectedTab == Tab.DARK_MATTER) {
            darkMatterPage.frame(dt, timeFactor());
        }
        if (selectedTab == Tab.COLLIDER) {
            colliderPage.frame(dt);
        }
        if (selectedTab == Tab.BIG_BANG) {
            bigBangPage.frame(dt);
        }
        if (selectedTab == Tab.UNIVERSE || universePage.launchView().isPlaying()) {
            universePage.frame(dt, settings.effect(Settings.Effect.UNIVERSE_MOTION));
        }
        if (particlesTabPop >= 0) popParticlesTab(dt);
        if (selectedTab == Tab.VOID) {
            voidPage.frame(dt, settings.effect(Settings.Effect.UNIVERSE_MOTION));
        }
        dimensionWindow.frame(dt, settings.effect(Settings.Effect.UNIVERSE_MOTION));
        lootPane.frame(dt);
        if (selectedTab != Tab.PARTICLES) {
            return;
        }
        if (settings.effect(Settings.Effect.AMBIENT)) generators.animateBackground(dt, Math.min(game.productionPerSecond().toDouble(), 1e12));
        else generators.clearBackground();
        // Chaque générateur a sa propre vitesse : les éléments du tableau périodique en accélèrent certains.
        List<ParticleView> views = generators.views();
        for (int i = 0; i < views.size(); i++) {
            double speed = Math.min(game.speed(i).toDouble() * timeFactor(), 1e9);
            views.get(i).frame(dt, game.state().formation(i), speed);
        }
    }

    /** Marge sur la durée d'une image : un écran à 60 Hz n'arrive pas pile à 1/60 s, et le plafond de 60 ne doit sauter aucune image. */
    static final double FRAME_TOLERANCE = 0.004;
    /** Images par seconde quand la fenêtre est derrière une autre, et quand elle est réduite, avec l'économie d'énergie. */
    public static final int BACKGROUND_FPS = 4;
    public static final int ICONIFIED_FPS = 1;

    /** Images par seconde visées en ce moment : le réglage, ou moins si la fenêtre n'est pas au premier plan. */
    int framesPerSecond() {
        int chosen = settings.frameRate().perSecond();
        if (!settings.saveEnergy() || stage == null) return chosen;
        if (stage.isIconified()) return Math.min(chosen, ICONIFIED_FPS);
        if (!stage.isFocused()) return Math.min(chosen, BACKGROUND_FPS);
        return chosen;
    }

    /** Accélération du temps : toujours 1 en jeu normal, réglable dans le profil de test. */
    double timeFactor() {
        return debugBar == null ? 1 : debugBar.timeFactor();
    }
}

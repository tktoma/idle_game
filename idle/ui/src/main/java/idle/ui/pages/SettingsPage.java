package idle.ui.pages;

import idle.core.math.BigNum;
import idle.ui.Cosmetic;
import idle.ui.GameApp;
import idle.ui.Settings;
import idle.ui.Shortcuts;
import idle.ui.Version;
import idle.ui.common.Detail;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Contenu de l'onglet « Réglages » : un bloc par réglage, rangés depuis la version 0.063 en cinq
 * sous-onglets ({@link Section}).
 * <ul>
 *   <li><b>Affichage</b> : la notation des grands nombres (dont trois à débloquer), le thème (dont
 *       cinq à débloquer), la taille de l'interface, la disposition des onglets, les notifications,
 *       l'objectif du moment, les signes d'état ;</li>
 *   <li><b>Effets et sons</b> : les effets visuels, ensemble ou un par un, les images par seconde, les sons ;</li>
 *   <li><b>Jeu</b> : la pause, les confirmations, le temps hors ligne, le chrono et les records ;</li>
 *   <li><b>Clavier</b> : les raccourcis et leurs touches, la touche de détail ;</li>
 *   <li><b>Partie</b> : la sauvegarde et son rythme, l'export et l'import, recommencer de zéro, le
 *       rapport de bug, la version du jeu et ses notes.</li>
 * </ul>
 * Un thème ou une notation à débloquer ({@link Cosmetic}) montre, tant qu'il est fermé, un
 * cadenas et un indice ; une fois ouvert, ce qu'il fallait faire.
 * Chaque réglage s'applique tout de suite. Les règles du jeu ne changent pas : les réglages sont
 * dans {@link Settings}, pas dans core.
 *
 * <p>Comme partout dans le jeu, les explications ne s'affichent qu'en mode détails
 * ({@link Detail}) : le reste du temps, chaque réglage tient en un titre et un bouton.
 */
public final class SettingsPage extends VBox {

    /** Les sous-onglets des réglages (version 0.063). */
    public enum Section {
        DISPLAY("Affichage"),
        EFFECTS("Effets et sons"),
        GAME("Jeu"),
        KEYS("Clavier"),
        SAVE("Partie");

        private final String label;

        Section(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final String SUB_TAB_STYLE = "-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand;"
            + " -fx-background-radius: 0; -fx-border-width: 0 0 2 0; -fx-background-color: transparent;";

    private static final String SETTINGS_COLOR = "#d6dde6";
    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 18; -fx-cursor: hand;"
            + " -fx-background-radius: 6; -fx-border-radius: 6;";
    private static final String ON_STYLE = BUTTON_STYLE
            + " -fx-text-fill: #0b0e14; -fx-background-color: " + SETTINGS_COLOR + "; -fx-border-color: #ffffff;";
    private static final String OFF_STYLE = BUTTON_STYLE
            + " -fx-text-fill: " + SETTINGS_COLOR + "; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;";
    /** Un thème ou une notation encore fermé : grisé, en pointillés. */
    private static final String LOCKED_STYLE = BUTTON_STYLE
            + " -fx-text-fill: #6f7f92; -fx-background-color: #10161f; -fx-border-color: #3a4a5e; -fx-border-style: segments(4, 3);";
    private static final String DANGER_STYLE = BUTTON_STYLE
            + " -fx-text-fill: #ffb4a8; -fx-background-color: #2a1614; -fx-border-color: #b8584a;";
    private static final String DANGER_ARMED_STYLE = BUTTON_STYLE
            + " -fx-text-fill: #1a0a08; -fx-background-color: #ff8a75; -fx-border-color: #ffffff;";
    /** Temps laissé pour le second clic de la remise à zéro, en secondes. */
    private static final double RESET_CONFIRM_SECONDS = 5;
    /** Nombres d'exemple pour montrer une notation. */
    private static final BigNum[] SAMPLES = {BigNum.of(1.2345, 7), BigNum.of(4.56, 21), BigNum.of(7.891, 44)};

    /**
     * Ce que la page demande à la fenêtre pour la sauvegarde. Chaque geste rend la phrase à
     * afficher sous les boutons : ce qu'il a donné, ou pourquoi il n'a rien donné.
     */
    public interface Saves {
        /** Où en est la sauvegarde : depuis combien de temps, dans quel fichier. */
        String status();

        /** Écrit la partie tout de suite. */
        String saveNow();

        /** Copie la partie, sur une ligne, dans le presse-papiers. */
        String copyExport();

        /** Remplace la partie en cours par celle du presse-papiers. */
        String pasteImport();

        /** Copie le rapport de bug dans le presse-papiers. */
        String copyBugReport();

        /** Les records du joueur, en une phrase : combien d'étapes en ont un. */
        String records();

        /** Oublie tous les records. */
        void clearRecords();
    }

    private final Settings settings;
    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    private final javafx.scene.layout.FlowPane subTabs = new javafx.scene.layout.FlowPane();
    private final Map<Section, Button> sectionTabs = new EnumMap<>(Section.class);
    private final Map<Section, VBox> sections = new EnumMap<>(Section.class);
    private Section selected = Section.DISPLAY;
    /** Le sous-onglet que remplit la construction de la page. */
    private VBox target;
    private final Label themeNote = new Label();
    /** L'indice du dernier thème fermé cliqué, ou rien. */
    private String themeHint = "";
    private String notationHint = "";
    private static final long LOCKED_THEMES = java.util.Arrays.stream(Settings.Theme.values()).filter(t -> t.unlock() != null).count();
    private static final long LOCKED_NOTATIONS = java.util.Arrays.stream(Settings.Notation.values()).filter(n -> n.unlock() != null).count();
    private final Label notationNote = new Label();
    private final Map<Settings.Effect, Button> effectButtons = new EnumMap<>(Settings.Effect.class);
    private final javafx.scene.layout.FlowPane effectRow = new javafx.scene.layout.FlowPane(8, 8);
    private final Map<Settings.SaveDelay, Button> saveDelayButtons = new EnumMap<>(Settings.SaveDelay.class);
    private final Button offlineButton = new Button();
    private final Runnable onReset;
    private final Runnable onAppearance;
    private final Saves saves;
    private final Label saveStatus = new Label();
    private final Button saveButton = new Button("Sauvegarder maintenant");
    private final Button exportButton = new Button("Exporter (copier)");
    private final Button importButton = new Button();
    /** Ce qu'a donné le dernier geste de sauvegarde, d'export ou d'import ; vide tant qu'il n'y en a pas eu. */
    private String saveMessage = "";
    private final Label saveNote = new Label();
    /** Secondes restantes pour confirmer l'import ; 0 quand il n'est pas demandé. */
    private double importArmed = 0;
    private final Button bugButton = new Button("Copier le rapport de bug");
    private String bugMessage = "";
    private final Label bugNote = new Label();
    private final Button notesButton = new Button();
    private final Label notesLabel = new Label(Version.notes());
    private boolean notesShown = false;
    private final Map<Settings.Theme, Button> themeButtons = new EnumMap<>(Settings.Theme.class);
    private final Map<Settings.Scale, Button> scaleButtons = new EnumMap<>(Settings.Scale.class);
    private final Map<Settings.WindowSize, Button> sizeButtons = new EnumMap<>(Settings.WindowSize.class);
    private java.util.function.Consumer<Settings.WindowSize> windowSize = size -> { };
    private java.util.function.Predicate<Settings.WindowSize> sizeFits = size -> true;
    private java.util.function.Supplier<Settings.WindowSize> sizeNow = () -> null;
    private final Button notificationsButton = new Button();
    private final Button goalButton = new Button();
    private final Button signsButton = new Button();
    private final Button chronoButton = new Button();
    private final Button clearRecordsButton = new Button("Effacer les records");
    private final Label recordsLabel = new Label();
    private final Map<Settings.Notation, Button> notationButtons = new EnumMap<>(Settings.Notation.class);
    private final Label notationExample = new Label();
    private final Button effectsButton = new Button();
    private final Button soundButton = new Button();
    private final Button tabsTopButton = new Button("En haut (la disposition actuelle)");
    private final Button tabsLeftButton = new Button("À gauche (la nouvelle)");
    private final Button tabOrderButton = new Button("Remettre les onglets dans l'ordre d'origine");
    private final Button quieterButton = new Button("−");
    private final Button louderButton = new Button("+");
    private final Label volumeLabel = new Label();
    private final java.util.Map<Settings.FrameRate, Button> frameRateButtons = new java.util.EnumMap<>(Settings.FrameRate.class);
    private final Button energyButton = new Button();
    /** Joue un son d'essai au volume réglé : branché par la fenêtre, qui possède les sons. */
    private Runnable soundTest = () -> { };
    private final Button confirmButton = new Button();
    private final Button confirmBigBangButton = new Button();
    private final Button confirmChallengeButton = new Button();
    private final Button shortcutsButton = new Button();
    /** Une ligne par raccourci : ce qu'il fait, et le bouton qui porte sa touche. */
    private final Map<Shortcuts.Action, Button> keyButtons = new EnumMap<>(Shortcuts.Action.class);
    private final VBox keyList = new VBox(4);
    private final Button resetKeysButton = new Button("Remettre les touches d'origine");
    private final Label keyNote = new Label();
    /** Le raccourci dont la page attend la nouvelle touche ; {@code null} quand c'est la touche de détail qu'elle attend. */
    private Shortcuts.Action capturingAction = null;
    private final Button detailKeyButton = new Button();
    private final Button detailModeButton = new Button();
    /** Vrai tant que la page attend la touche qui deviendra la touche de détail. */
    private boolean capturing = false;
    /** Ce que la dernière touche proposée avait de travers, à dire sous le bouton ; vide sinon. */
    private String captureProblem = "";
    private final Label detailKeyNote = new Label();
    /** Les explications des réglages : elles ne s'affichent qu'en mode détails. */
    private final List<Label> explanations = new ArrayList<>();
    private final Button pauseButton = new Button();
    private final Button resetButton = new Button();
    /** Secondes restantes pour confirmer la remise à zéro ; 0 quand elle n'est pas demandée. */
    private double resetArmed = 0;

    /**
     * @param settings les réglages à afficher et à modifier
     * @param onReset      ce qu'il faut faire quand le joueur a confirmé qu'il recommence de zéro
     * @param onAppearance ce qu'il faut faire quand le thème ou la taille de l'interface change
     * @param saves        ce que la fenêtre sait faire de la sauvegarde
     */
    public SettingsPage(Settings settings, Runnable onReset, Runnable onAppearance, Saves saves) {
        super(10);
        this.settings = settings;
        this.onReset = onReset;
        this.onAppearance = onAppearance;
        this.saves = saves;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(24));

        Label title = new Label("Réglages");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + SETTINGS_COLOR + ";");
        getChildren().add(title);
        subTabs.setAlignment(Pos.CENTER);
        getChildren().add(subTabs);
        for (Section section : Section.values()) {
            Button tab = new Button(section.label());
            tab.setFocusTraversable(false);
            tab.setOnAction(event -> select(section));
            sectionTabs.put(section, tab);
            subTabs.getChildren().add(tab);
            VBox box = new VBox(10);
            box.setAlignment(Pos.TOP_CENTER);
            sections.put(section, box);
            getChildren().add(box);
        }
        use(Section.DISPLAY);

        // Notation des grands nombres : un bouton par notation, celui choisi est éclairé.
        javafx.scene.layout.FlowPane notationFlow = new javafx.scene.layout.FlowPane(8, 8);
        notationFlow.setAlignment(Pos.CENTER);
        notationFlow.setMaxWidth(640);
        for (Settings.Notation notation : Settings.Notation.values()) {
            Button button = new Button(notation.label());
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                notationHint = settings.isOpen(notation) ? "" : "Fermée. Indice : " + notation.unlock().hint();
                if (settings.isOpen(notation)) settings.setNotation(notation);
                refresh();
            });
            notationButtons.put(notation, button);
            notationFlow.getChildren().add(button);
        }
        block("Notation des grands nombres",
                "Comment s'écrivent les nombres à partir d'un million, partout dans le jeu. Celles marquées d'un "
                        + "cadenas s'ouvrent en faisant quelque chose de très précis : un clic dessus donne un indice.");
        target.getChildren().add(notationFlow);
        note(notationExample);
        note(notationNote);

        // Le thème et la taille : ils s'appliquent tout de suite à toute la fenêtre.
        javafx.scene.layout.FlowPane themes = new javafx.scene.layout.FlowPane(8, 8);
        themes.setAlignment(Pos.CENTER);
        themes.setMaxWidth(640);
        for (Settings.Theme theme : Settings.Theme.values()) {
            Button button = new Button(theme.label());
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                themeHint = settings.isOpen(theme) ? "" : "Fermé. Indice : " + theme.unlock().hint();
                if (settings.isOpen(theme)) {
                    settings.setTheme(theme);
                    this.onAppearance.run();
                }
                refresh();
            });
            themeButtons.put(theme, button);
            themes.getChildren().add(button);
        }
        block("Thème", "Chaque thème filtre les couleurs du thème sombre. Le thème clair les inverse : le fond devient clair, "
                + "les textes foncés, et chaque couleur garde sa teinte. Les autres tournent les teintes, les éteignent ou "
                + "les teintent. Ceux marqués d'un cadenas s'ouvrent en faisant quelque chose de très précis : un clic "
                + "dessus donne un indice.");
        target.getChildren().add(themes);
        note(themeNote);

        HBox scales = new HBox(8);
        scales.setAlignment(Pos.CENTER);
        for (Settings.Scale scale : Settings.Scale.values()) {
            Button button = new Button(scale.label());
            button.setOnAction(event -> {
                settings.setScale(scale);
                this.onAppearance.run();
                refresh();
            });
            scaleButtons.put(scale, button);
            scales.getChildren().add(button);
        }
        block("Taille de l'interface", "Agrandit ou réduit tout ensemble : les textes, les boutons et les dessins.");
        target.getChildren().add(scales);

        // Les formats de fenêtre (version 0.092) : un clic redimensionne et recentre la fenêtre.
        javafx.scene.layout.FlowPane sizes = new javafx.scene.layout.FlowPane(8, 8);
        sizes.setAlignment(Pos.CENTER);
        sizes.setMaxWidth(760);
        for (Settings.WindowSize size : Settings.WindowSize.values()) {
            Button button = new Button(size.text());
            button.setOnAction(event -> {
                windowSize.accept(size);
                refresh();
            });
            sizeButtons.put(size, button);
            sizes.getChildren().add(button);
        }
        block("Taille de la fenêtre", "Un format prédéfini, recentré sur l'écran. La fenêtre se redimensionne toujours à la main, "
                + "mais pas en dessous d'une taille où tout reste utilisable ; cette limite grandit avec la taille de l'interface.");
        target.getChildren().add(sizes);

        block("Notifications", "Un message de quelques secondes, en bas à droite, quand il se passe quelque chose : "
                + "nouvel élément, ensemble réuni, succès, déblocage, explosion possible.");
        notificationsButton.setOnAction(event -> {
            settings.setNotifications(!settings.notifications());
            refresh();
        });
        target.getChildren().add(notificationsButton);

        block("Objectif du moment", "Une ligne sous les onglets rappelle le prochain grand pas de la partie, et "
                + "le temps qu'il demande quand le jeu peut l'estimer.");
        goalButton.setOnAction(event -> {
            settings.setShowGoal(!settings.showGoal());
            refresh();
        });
        target.getChildren().add(goalButton);

        block("Signes d'état", "Chaque carte porte devant son nom le signe de son état, pour le reconnaître sans la "
                + "couleur : ● à portée, ○ en attente, ◐ entamée, ▶ en marche, □ coupée, ✓ acquise, × fermée. Les "
                + "états de la matière ont aussi le leur : ∴ gaz, ≈ liquides, ■ solides, ◆ cristaux, ▲ métaux.");
        signsButton.setOnAction(event -> {
            settings.setStateSigns(!settings.stateSigns());
            refresh();
        });
        target.getChildren().add(signsButton);

        use(Section.DISPLAY);

        block("Disposition des onglets",
                "En haut, comme jusqu'ici : les onglets en ligne, et les sous-onglets dans chaque page. À gauche : les "
                        + "onglets en colonne sur le côté, et la barre du haut montre les sous-onglets de l'onglet affiché. "
                        + "Dans les deux, un appui long sur un onglet permet de le glisser à une autre place.");
        tabsTopButton.setOnAction(event -> {
            settings.setTabsLeft(false);
            onAppearance.run();
            refresh();
        });
        tabsLeftButton.setOnAction(event -> {
            settings.setTabsLeft(true);
            onAppearance.run();
            refresh();
        });
        tabOrderButton.setOnAction(event -> {
            settings.setTabOrder("");
            onAppearance.run();
            refresh();
        });
        HBox tabsRow = new HBox(8, tabsTopButton, tabsLeftButton, tabOrderButton);
        tabsRow.setAlignment(Pos.CENTER);
        for (Button button : List.of(tabsTopButton, tabsLeftButton, tabOrderButton)) button.setFocusTraversable(false);
        target.getChildren().add(tabsRow);

        use(Section.EFFECTS);
        block("Effets visuels",
                "Le grand interrupteur les coupe tous ; sinon, chacun se règle à part. Sans l'éclair, l'explosion a lieu "
                        + "tout de suite : à préférer si les flashs vous gênent.");
        effectsButton.setOnAction(event -> {
            settings.setEffects(!settings.effects());
            refresh();
        });
        target.getChildren().add(effectsButton);
        effectRow.setAlignment(Pos.CENTER);
        effectRow.setMaxWidth(640);
        for (Settings.Effect effect : Settings.Effect.values()) {
            Button button = new Button(effect.label());
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                settings.setEffect(effect, !settings.effectChosen(effect));
                refresh();
            });
            effectButtons.put(effect, button);
            effectRow.getChildren().add(button);
        }
        target.getChildren().add(effectRow);

        block("Sons",
                "Un son accompagne les grands moments : succès, élément nouveau, explosion, Big Bang, comète, défi "
                        + "réussi. Tous sont fabriqués par le jeu, sans fichier. Un même son ne revient pas avant une "
                        + "seconde, même quand les automatismes enchaînent tout. Changer le volume fait entendre un essai.");
        soundButton.setOnAction(event -> {
            settings.setSound(!settings.sound());
            if (settings.sound()) soundTest.run();
            refresh();
        });
        quieterButton.setOnAction(event -> changeVolume(-VOLUME_STEP));
        louderButton.setOnAction(event -> changeVolume(VOLUME_STEP));
        for (Button button : List.of(quieterButton, louderButton)) {
            button.setStyle(OFF_STYLE);
            button.setFocusTraversable(false);
        }
        volumeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #c9d1d9; -fx-min-width: 110; -fx-alignment: center;");
        HBox soundRow = new HBox(8, soundButton, quieterButton, volumeLabel, louderButton);
        soundRow.setAlignment(Pos.CENTER);
        target.getChildren().add(soundRow);

        block("Images par seconde",
                "Moins d'images, c'est moins de calcul et moins de batterie ; le jeu avance au même rythme, seule "
                        + "l'animation est moins fluide. Avec l'économie d'énergie, la fenêtre passe à "
                        + GameApp.BACKGROUND_FPS + " images par seconde quand une autre est au premier plan, et à "
                        + GameApp.ICONIFIED_FPS + " quand elle est réduite.");
        HBox rateRow = new HBox(8);
        rateRow.setAlignment(Pos.CENTER);
        for (Settings.FrameRate rate : Settings.FrameRate.values()) {
            Button button = new Button();
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                settings.setFrameRate(rate);
                refresh();
            });
            frameRateButtons.put(rate, button);
            rateRow.getChildren().add(button);
        }
        energyButton.setOnAction(event -> {
            settings.setSaveEnergy(!settings.saveEnergy());
            refresh();
        });
        rateRow.getChildren().add(energyButton);
        target.getChildren().add(rateRow);

        use(Section.GAME);
        block("Pause", "En pause, le temps ne passe plus : rien ne se crée, aucun automatisme n'agit. "
                + "Le jeu repart toujours en marche au lancement.");
        pauseButton.setOnAction(event -> {
            settings.setPaused(!settings.paused());
            refresh();
        });
        target.getChildren().add(pauseButton);

        block("Confirmations",
                "L'explosion, le Big Bang et les défis remettent la partie à zéro. Avec la confirmation, il faut cliquer "
                        + "deux fois : le premier clic arme, le second, dans les cinq secondes, déclenche. Chacune se "
                        + "règle à part.");
        confirmButton.setOnAction(event -> {
            settings.setConfirmExplosion(!settings.confirmExplosion());
            refresh();
        });
        confirmBigBangButton.setOnAction(event -> {
            settings.setConfirmBigBang(!settings.confirmBigBang());
            refresh();
        });
        confirmChallengeButton.setOnAction(event -> {
            settings.setConfirmChallenge(!settings.confirmChallenge());
            refresh();
        });
        HBox confirmRow = new HBox(8, confirmButton, confirmBigBangButton, confirmChallengeButton);
        confirmRow.setAlignment(Pos.CENTER);
        target.getChildren().add(confirmRow);

        block("Temps hors ligne", "Au retour, une part du temps passé jeu fermé est rejouée : 10 % avant la première "
                + "explosion, 40 % ensuite, 70 % après le premier Big Bang, 100 % une fois l'univers formé (vingt-quatre "
                + "heures d'absence au plus). Coupé, le jeu reprend là où il en était, sans rien rattraper.");
        offlineButton.setOnAction(event -> {
            settings.setOffline(!settings.offline());
            refresh();
        });
        target.getChildren().add(offlineButton);

        use(Section.KEYS);
        block("Raccourcis clavier",
                "Ils marchent depuis n'importe quel onglet. Les trois premiers achètent la quantité choisie dans l'onglet "
                        + "Particules (×1, ×10 ou max). Un clic sur une touche permet de la changer : appuyez ensuite sur "
                        + "la nouvelle, ou sur Échap pour annuler. Deux raccourcis ne peuvent pas partager une touche.");
        shortcutsButton.setOnAction(event -> {
            settings.setShortcuts(!settings.shortcuts());
            refresh();
        });
        target.getChildren().add(shortcutsButton);
        keyList.setAlignment(Pos.CENTER);
        for (Shortcuts.Action action : Shortcuts.Action.values()) {
            Label name = new Label(action.label());
            name.setStyle("-fx-font-size: 13px; -fx-text-fill: #b7c4d2;");
            name.setMinWidth(300);
            name.setPrefWidth(300);
            Button key = new Button();
            key.setMinWidth(150);
            key.setOnAction(event -> {
                // Un second clic sur la même touche annule l'attente.
                boolean same = capturing && capturingAction == action;
                capturing = !same;
                capturingAction = same ? null : action;
                captureProblem = "";
                refresh();
            });
            keyButtons.put(action, key);
            HBox row = new HBox(10, name, key);
            row.setAlignment(Pos.CENTER);
            keyList.getChildren().add(row);
        }
        target.getChildren().add(keyList);
        note(keyNote);
        resetKeysButton.setStyle(OFF_STYLE);
        resetKeysButton.setOnAction(event -> {
            settings.resetKeys();
            capturing = false;
            capturingAction = null;
            captureProblem = "";
            refresh();
        });
        target.getChildren().add(resetKeysButton);

        block("Touche de détail", "Les cartes et les pages s'en tiennent à l'essentiel. Cette touche fait apparaître "
                + "leurs explications complètes : ce que fait chaque amélioration, d'où vient chaque nombre. "
                + "On peut la tenir enfoncée, ou la faire basculer d'un appui.");
        detailKeyButton.setOnAction(event -> {
            capturing = !(capturing && capturingAction == null);
            capturingAction = null;
            captureProblem = "";
            refresh();
        });
        detailModeButton.setOnAction(event -> {
            settings.setDetailToggle(!settings.detailToggle());
            Detail.set(false);
            refresh();
        });
        HBox detailRow = new HBox(8, detailKeyButton, detailModeButton);
        detailRow.setAlignment(Pos.CENTER);
        target.getChildren().add(detailRow);
        note(detailKeyNote);

        use(Section.SAVE);
        block("Sauvegarde", "La partie s'écrit toute seule au rythme choisi ci-dessous (toutes les trente secondes "
                + "d'origine) et à la fermeture du jeu. Exporter copie toute la partie dans le presse-papiers, sur une ligne : "
                + "pour la garder ailleurs ou la reprendre sur une autre machine. Importer remplace la partie en cours par "
                + "celle du presse-papiers. Le temps hors ligne se règle dans « Jeu ».");
        note(saveStatus);
        HBox delayRow = new HBox(8);
        delayRow.setAlignment(Pos.CENTER);
        Label delayLabel = new Label("Sauvegarde automatique toutes les");
        delayLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #c9d1d9;");
        delayRow.getChildren().add(delayLabel);
        for (Settings.SaveDelay delay : Settings.SaveDelay.values()) {
            Button button = new Button(delay.label());
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                settings.setSaveDelay(delay);
                refresh();
            });
            saveDelayButtons.put(delay, button);
            delayRow.getChildren().add(button);
        }
        target.getChildren().add(delayRow);
        saveButton.setStyle(OFF_STYLE);
        saveButton.setOnAction(event -> {
            saveMessage = saves.saveNow();
            refresh();
        });
        exportButton.setStyle(OFF_STYLE);
        exportButton.setOnAction(event -> {
            saveMessage = saves.copyExport();
            refresh();
        });
        importButton.setOnAction(event -> {
            if (importArmed > 0) {
                importArmed = 0;
                saveMessage = saves.pasteImport();
            } else {
                importArmed = RESET_CONFIRM_SECONDS;
            }
            refresh();
        });
        HBox saveRow = new HBox(8, saveButton, exportButton, importButton);
        saveRow.setAlignment(Pos.CENTER);
        target.getChildren().add(saveRow);
        note(saveNote);

        use(Section.GAME);
        block("Chrono et records", "Le jeu retient le temps de jeu le plus court auquel chaque étape a été atteinte : "
                + "première fusion, première explosion, premier Big Bang, puis les étapes du troisième acte. Avec le chrono, "
                + "la ligne d'objectif dit l'avance ou le retard sur le record de la prochaine étape. Les records "
                + "traversent la remise à zéro du jeu : c'est à la partie suivante qu'ils servent.");
        chronoButton.setOnAction(event -> {
            settings.setChrono(!settings.chrono());
            refresh();
        });
        clearRecordsButton.setStyle(OFF_STYLE);
        clearRecordsButton.setFocusTraversable(false);
        clearRecordsButton.setOnAction(event -> {
            saves.clearRecords();
            refresh();
        });
        HBox recordsRow = new HBox(8, chronoButton, clearRecordsButton);
        recordsRow.setAlignment(Pos.CENTER);
        target.getChildren().add(recordsRow);
        note(recordsLabel);

        use(Section.SAVE);
        block("Recommencer de zéro",
                "Efface toute la partie : particules, atomes, tableau périodique, matière noire, arbre et statistiques, "
                        + "sauvegarde comprise. Les réglages et les records sont gardés. C'est définitif : exportez d'abord "
                        + "la partie si vous voulez pouvoir y revenir.");
        resetButton.setOnAction(event -> {
            if (resetArmed > 0) {
                resetArmed = 0;
                this.onReset.run();
            } else {
                resetArmed = RESET_CONFIRM_SECONDS;
            }
            refresh();
        });
        target.getChildren().add(resetButton);

        block("Rapport de bug", "Copie dans le presse-papiers de quoi reproduire un problème : la version du jeu, "
                + "celle de Java et du système, la première erreur survenue s'il y en a eu une, et la partie elle-même. "
                + "Rien n'est envoyé : c'est vous qui le collez où vous voulez.");
        bugButton.setStyle(OFF_STYLE);
        bugButton.setOnAction(event -> {
            bugMessage = saves.copyBugReport();
            refresh();
        });
        target.getChildren().add(bugButton);
        note(bugNote);

        block("Version " + Version.CURRENT, "Ce qui a changé, version après version.");
        notesButton.setOnAction(event -> {
            notesShown = !notesShown;
            refresh();
        });
        target.getChildren().add(notesButton);
        note(notesLabel);
        notesLabel.setTextAlignment(TextAlignment.LEFT);

        Label footer = new Label("Les réglages sont gardés d'un lancement à l'autre, sur cette machine. La partie est "
                + "sauvegardée à part : l'exporter l'emporte, les réglages non.");
        target = null;
        note(footer);
        footer.setStyle("-fx-font-size: 12px; -fx-padding: 18 0 0 0; -fx-text-fill: #8fa3b8;");
        select(Section.DISPLAY);
    }

    /** Les blocs qui suivent vont dans ce sous-onglet. */
    private void use(Section section) {
        target = sections.get(section);
    }

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public javafx.scene.layout.FlowPane subTabs() {
        return subTabs;
    }

    /** Affiche un sous-onglet, et cache les autres. */
    public void select(Section section) {
        selected = section;
        sections.forEach((each, box) -> {
            box.setVisible(each == section);
            box.setManaged(each == section);
        });
        refresh();
    }

    /** Le sous-onglet affiché. */
    public Section selected() {
        return selected;
    }

    /** Titre et explication d'un réglage ; l'explication n'apparaît qu'en mode détails. */
    private void block(String name, String explanation) {
        Label header = new Label(name);
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 14 0 0 0; -fx-text-fill: " + SETTINGS_COLOR + ";");
        target.getChildren().add(header);
        Label text = new Label(explanation);
        note(text);
        explanations.add(text);
    }

    /** Pas du volume à chaque clic sur − ou + : dix crans de 0 à 100. */
    static final int VOLUME_STEP = 10;

    private void changeVolume(int step) {
        settings.setVolume(settings.volume() + step);
        soundTest.run();
        refresh();
    }

    /** Branche le son d'essai, joué quand le joueur règle le volume ou rallume les sons. */
    public void setSoundTest(Runnable test) {
        soundTest = test == null ? () -> { } : test;
    }

    /** Vrai tant que la page attend la prochaine touche du clavier pour en faire la touche de détail. */
    public boolean isCapturing() {
        return capturing;
    }

    /**
     * La touche pressée pendant que la page en attendait une, pour la touche de détail ou pour un
     * raccourci. Échap annule ; une touche déjà prise est refusée, et la page continue d'attendre.
     */
    public void capture(KeyCode key) {
        if (!capturing) return;
        Shortcuts.Action holder = settings.actionOf(key);
        if (key == KeyCode.ESCAPE) {
            capturing = false;
            captureProblem = "";
        } else if (capturingAction == null) {
            // La touche de détail : n'importe laquelle, sauf celle d'un raccourci.
            if (holder != null) {
                captureProblem = "« " + Shortcuts.name(key) + " » sert déjà à : " + holder.label().toLowerCase() + ". Choisissez une autre touche.";
            } else {
                settings.setDetailKey(key);
                Detail.set(false);
                capturing = false;
                captureProblem = "";
            }
        } else if (key == settings.detailKey()) {
            captureProblem = "« " + Shortcuts.name(key) + " » est la touche de détail. Choisissez une autre touche.";
        } else if (!Shortcuts.bindable(key)) {
            captureProblem = "« " + Shortcuts.name(key) + " » ne peut pas porter un raccourci. Choisissez une autre touche.";
        } else if (holder != null && holder != capturingAction) {
            captureProblem = "« " + Shortcuts.name(key) + " » sert déjà à : " + holder.label().toLowerCase() + ". Choisissez une autre touche.";
        } else {
            settings.setKey(capturingAction, key);
            capturing = false;
            capturingAction = null;
            captureProblem = "";
        }
        refresh();
    }

    private void note(Label label) {
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(560);
        (target == null ? getChildren() : target.getChildren()).add(label);
    }

    /**
     * Fait passer le temps de la confirmation de remise à zéro : sans second clic, elle s'annule.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    public void frame(double elapsed) {
        if (resetArmed <= 0 && importArmed <= 0) return;
        resetArmed = Math.max(0, resetArmed - elapsed);
        importArmed = Math.max(0, importArmed - elapsed);
        refresh();
    }

    /** L'onglet n'est plus affiché : une remise à zéro ou une touche demandées mais pas confirmées sont oubliées. */
    public void release() {
        resetArmed = 0;
        importArmed = 0;
        saveMessage = "";
        bugMessage = "";
        capturing = false;
        capturingAction = null;
        captureProblem = "";
        themeHint = "";
        notationHint = "";
    }

    /** Recopie les réglages dans les boutons. */
    public void refresh() {
        sectionTabs.forEach((section, tab) -> tab.setStyle(SUB_TAB_STYLE + (section == selected
                ? " -fx-text-fill: " + SETTINGS_COLOR + "; -fx-border-color: transparent transparent " + SETTINGS_COLOR + " transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;")));
        notationButtons.forEach((notation, button) -> {
            boolean open = settings.isOpen(notation);
            button.setText(open ? notation.label() : "🔒 " + notation.label());
            button.setStyle(notation == settings.notation() ? ON_STYLE : open ? OFF_STYLE : LOCKED_STYLE);
            tip(button, open && notation.unlock() != null ? "Débloquée en : " + notation.unlock().condition() : open ? "" : notation.unlock().hint());
        });
        StringBuilder example = new StringBuilder("Exemples : ");
        for (int i = 0; i < SAMPLES.length; i++) {
            example.append(i == 0 ? "" : "   ·   ").append(Format.big(SAMPLES[i]));
        }
        notationExample.setText(example.toString());

        themeButtons.forEach((theme, button) -> {
            boolean open = settings.isOpen(theme);
            button.setText(open ? theme.label() : "🔒 " + theme.label());
            button.setStyle(theme == settings.theme() ? ON_STYLE : open ? OFF_STYLE : LOCKED_STYLE);
            tip(button, open && theme.unlock() != null ? "Débloqué en : " + theme.unlock().condition() : open ? "" : theme.unlock().hint());
        });
        int themesOpen = 0, notationsOpen = 0;
        for (Settings.Theme theme : Settings.Theme.values()) if (theme.unlock() != null && settings.isOpen(theme)) themesOpen++;
        for (Settings.Notation notation : Settings.Notation.values()) if (notation.unlock() != null && settings.isOpen(notation)) notationsOpen++;
        themeNote.setText(!themeHint.isEmpty() ? themeHint : "Thèmes à débloquer : " + themesOpen + " / " + LOCKED_THEMES);
        notationNote.setText(!notationHint.isEmpty() ? notationHint : "Notations à débloquer : " + notationsOpen + " / " + LOCKED_NOTATIONS);
        effectButtons.forEach((effect, button) -> {
            boolean on = settings.effectChosen(effect);
            button.setText((on ? "✓ " : "✗ ") + effect.label());
            button.setStyle(on && settings.effects() ? ON_STYLE : OFF_STYLE);
            button.setDisable(!settings.effects());
            tip(button, effect.description());
        });
        saveDelayButtons.forEach((delay, button) -> button.setStyle(delay == settings.saveDelay() ? ON_STYLE : OFF_STYLE));
        offlineButton.setText(settings.offline() ? "Temps hors ligne : rejoué au retour" : "Temps hors ligne : ignoré");
        offlineButton.setStyle(settings.offline() ? ON_STYLE : OFF_STYLE);
        scaleButtons.forEach((scale, button) -> button.setStyle(scale == settings.scale() ? ON_STYLE : OFF_STYLE));
        Settings.WindowSize current = sizeNow.get();
        sizeButtons.forEach((size, button) -> {
            boolean fits = sizeFits.test(size);
            button.setDisable(!fits);
            button.setStyle(size == current ? ON_STYLE : OFF_STYLE);
        });
        notificationsButton.setText(settings.notifications() ? "Notifications : affichées" : "Notifications : coupées");
        notificationsButton.setStyle(settings.notifications() ? ON_STYLE : OFF_STYLE);
        goalButton.setText(settings.showGoal() ? "Objectif : affiché" : "Objectif : caché");
        goalButton.setStyle(settings.showGoal() ? ON_STYLE : OFF_STYLE);
        signsButton.setText(settings.stateSigns() ? "Signes d'état : affichés" : "Signes d'état : cachés");
        signsButton.setStyle(settings.stateSigns() ? ON_STYLE : OFF_STYLE);
        chronoButton.setText(settings.chrono() ? "Chrono : affiché" : "Chrono : caché");
        chronoButton.setStyle(settings.chrono() ? ON_STYLE : OFF_STYLE);
        recordsLabel.setText(saves.records());
        effectsButton.setText(settings.effects() ? "Effets visuels : affichés" : "Effets visuels : coupés");
        tabsTopButton.setStyle(settings.tabsLeft() ? OFF_STYLE : ON_STYLE);
        tabsLeftButton.setStyle(settings.tabsLeft() ? ON_STYLE : OFF_STYLE);
        tabOrderButton.setStyle(OFF_STYLE);
        tabOrderButton.setDisable(settings.tabOrder().isEmpty());
        soundButton.setText(settings.sound() ? "Sons : activés" : "Sons : coupés");
        soundButton.setStyle(settings.sound() ? ON_STYLE : OFF_STYLE);
        volumeLabel.setText("Volume : " + settings.volume() + " %");
        quieterButton.setDisable(!settings.sound() || settings.volume() <= 0);
        louderButton.setDisable(!settings.sound() || settings.volume() >= 100);
        for (Map.Entry<Settings.FrameRate, Button> entry : frameRateButtons.entrySet()) {
            entry.getValue().setText(entry.getKey().label() + " images/s");
            entry.getValue().setStyle(entry.getKey() == settings.frameRate() ? ON_STYLE : OFF_STYLE);
        }
        energyButton.setText(settings.saveEnergy() ? "Économie d'énergie : oui" : "Économie d'énergie : non");
        energyButton.setStyle(settings.saveEnergy() ? ON_STYLE : OFF_STYLE);
        effectsButton.setStyle(settings.effects() ? ON_STYLE : OFF_STYLE);
        confirmButton.setText(settings.confirmExplosion() ? "Explosion : demandée" : "Explosion : non demandée");
        confirmButton.setStyle(settings.confirmExplosion() ? ON_STYLE : OFF_STYLE);
        confirmBigBangButton.setText(settings.confirmBigBang() ? "Big Bang : demandée" : "Big Bang : non demandée");
        confirmBigBangButton.setStyle(settings.confirmBigBang() ? ON_STYLE : OFF_STYLE);
        confirmChallengeButton.setText(settings.confirmChallenge() ? "Défis : demandée" : "Défis : non demandée");
        confirmChallengeButton.setStyle(settings.confirmChallenge() ? ON_STYLE : OFF_STYLE);
        // La liste des raccourcis : chaque touche est un bouton, éclairé pendant qu'il attend la nouvelle.
        keyList.setVisible(settings.shortcuts());
        keyList.setManaged(settings.shortcuts());
        keyButtons.forEach((action, button) -> {
            boolean waiting = capturing && capturingAction == action;
            KeyCode bound = settings.key(action);
            button.setText(waiting ? "Appuyez sur une touche…" : bound == null ? "(aucune)" : Shortcuts.name(bound));
            button.setStyle(waiting ? ON_STYLE : OFF_STYLE);
        });
        boolean custom = settings.shortcuts() && settings.hasCustomKeys();
        resetKeysButton.setVisible(custom);
        resetKeysButton.setManaged(custom);
        show(keyNote, capturingAction != null ? captureProblem : "");
        shortcutsButton.setText(settings.shortcuts() ? "Raccourcis clavier : actifs" : "Raccourcis clavier : coupés");
        shortcutsButton.setStyle(settings.shortcuts() ? ON_STYLE : OFF_STYLE);
        String key = Detail.keyName(settings.detailKey());
        boolean detailWaiting = capturing && capturingAction == null;
        detailKeyButton.setText(detailWaiting ? "Appuyez sur une touche… (Échap annule)" : "Touche : " + key + " (changer)");
        detailKeyButton.setStyle(detailWaiting ? ON_STYLE : OFF_STYLE);
        detailModeButton.setText(settings.detailToggle() ? "Un appui bascule" : "À tenir enfoncée");
        detailModeButton.setStyle(OFF_STYLE);
        show(detailKeyNote, capturingAction == null ? captureProblem : "");
        // Les explications, comme partout : seulement en mode détails.
        for (Label explanation : explanations) {
            explanation.setVisible(Detail.shown());
            explanation.setManaged(Detail.shown());
        }
        pauseButton.setText(settings.paused() ? "En pause : cliquer pour reprendre" : "Mettre en pause");
        pauseButton.setStyle(settings.paused() ? ON_STYLE : OFF_STYLE);
        resetButton.setText(resetArmed > 0
                ? "Cliquer encore pour tout effacer (" + (int) Math.ceil(resetArmed) + " s)"
                : "Recommencer de zéro");
        resetButton.setStyle(resetArmed > 0 ? DANGER_ARMED_STYLE : DANGER_STYLE);
        saveStatus.setText(saves.status());
        importButton.setText(importArmed > 0
                ? "Cliquer encore : la partie en cours sera remplacée (" + (int) Math.ceil(importArmed) + " s)"
                : "Importer (coller)");
        importButton.setStyle(importArmed > 0 ? DANGER_ARMED_STYLE : DANGER_STYLE);
        show(saveNote, saveMessage);
        show(bugNote, bugMessage);
        notesButton.setText(notesShown ? "Cacher les notes de version" : "Notes de version");
        notesButton.setStyle(notesShown ? ON_STYLE : OFF_STYLE);
        notesLabel.setVisible(notesShown);
        notesLabel.setManaged(notesShown);
    }

    /** L'info-bulle d'un bouton, ou aucune. */
    private static void tip(Button button, String text) {
        if (text.isEmpty()) {
            if (button.getTooltip() != null) button.setTooltip(null);
            return;
        }
        if (button.getTooltip() != null && text.equals(button.getTooltip().getText())) return;
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(360);
        button.setTooltip(tooltip);
    }

    /** Affiche une phrase sous un bouton, ou retire sa place quand il n'y a rien à dire. */
    private static void show(Label label, String text) {
        label.setText(text);
        label.setVisible(!text.isEmpty());
        label.setManaged(!text.isEmpty());
    }

    /**
     * Ce que font les boutons des formats de fenêtre (version 0.092) : redimensionner, dire si un format
     * tient sur l'écran, et lequel est en cours.
     */
    public void setWindowSizes(java.util.function.Consumer<Settings.WindowSize> apply,
            java.util.function.Predicate<Settings.WindowSize> fits, java.util.function.Supplier<Settings.WindowSize> current) {
        windowSize = apply;
        sizeFits = fits;
        sizeNow = current;
        refresh();
    }

    /** Le bouton d'un format de fenêtre : pour les vérifications. */
    public Button sizeButton(Settings.WindowSize size) {
        return sizeButtons.get(size);
    }
}

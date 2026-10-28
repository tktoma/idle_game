package idle.ui.pages;

import idle.core.math.BigNum;
import idle.ui.GameApp;
import idle.ui.Settings;
import idle.ui.Shortcuts;
import idle.ui.Version;
import idle.ui.common.Detail;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.SubTabs;
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
 * Contenu de l'onglet « Réglages » : un bloc par réglage, rangés depuis la version 0.063 en six
 * sous-onglets ({@link Section}).
 * <ul>
 *   <li><b>Affichage</b> : la notation des grands nombres (dont trois à débloquer), le thème (dont
 *       cinq à débloquer), la taille de l'interface et de la fenêtre, la disposition des onglets ;</li>
 *   <li><b>Notifications et signes</b> (version 0.146) : les notifications par catégorie, les morceaux de la ligne
 *       d'objectif (chrono compris), les signes d'état des cartes et les pastilles des onglets par sorte ;</li>
 *   <li><b>Effets et sons</b> : les effets visuels, ensemble ou un par un, les images par seconde, les sons ;</li>
 *   <li><b>Jeu</b> : la pause, les confirmations, le temps hors ligne, les records ;</li>
 *   <li><b>Clavier</b> : les raccourcis et leurs touches, la touche de détail ;</li>
 *   <li><b>Partie</b> : la sauvegarde et son rythme, l'export et l'import, recommencer de zéro, le
 *       rapport de bug, la version du jeu et ses notes.</li>
 * </ul>
 * Un thème ou une notation à débloquer ({@link idle.ui.Cosmetic}) montre, tant qu'il est fermé, un
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
        /** Version 0.146 : les notifications, l'objectif et les signes d'état, réunis et réglés par catégorie. */
        NOTICES("Notifications et signes"),
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

        /** Ce que contient la partie du presse-papiers, sans rien changer (version 0.127) ; ou pourquoi elle ne se lit pas. */
        String previewImport();

        /** Le nombre de points de reprise nommés (version 0.127). */
        int checkpointSlots();

        /** Ce que contient ce point de reprise, en une ligne ; « vide » s'il n'y a rien. Le numéro 0 est la mise de côté. */
        String checkpointLine(int slot);

        /** Vrai si ce point de reprise contient une partie. */
        boolean checkpointUsed(int slot);

        /** Écrit la partie en cours dans ce point, sous ce nom. */
        String saveCheckpoint(int slot, String name);

        /** Reprend la partie de ce point, après avoir mis la partie en cours de côté. */
        String loadCheckpoint(int slot);

        /** Efface ce point. */
        String deleteCheckpoint(int slot);

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
    /** Les points de reprise (version 0.127) : un nom à donner, et une ligne par emplacement. */
    private final javafx.scene.control.TextField checkpointName = new javafx.scene.control.TextField();
    private final List<Label> checkpointLabels = new ArrayList<>();
    private final List<Button> checkpointSave = new ArrayList<>();
    private final List<Button> checkpointLoad = new ArrayList<>();
    private final List<Button> checkpointDelete = new ArrayList<>();
    /** Le geste armé sur un point, attendant son second clic : « save:2 », « load:0 »… et le temps qu'il lui reste. */
    private String checkpointArmed = "";
    private double checkpointArmedFor = 0;
    private String checkpointMessage = "";
    private final Label checkpointNote = new Label();
    /** Ce que contient la partie du presse-papiers, montré au premier clic sur « Importer ». */
    private String importPreview = "";
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
    /** Les catégories de la section « Notifications et signes » (version 0.146) : un bouton par catégorie. */
    private final Map<idle.ui.Notice, Button> noticeButtons = new EnumMap<>(idle.ui.Notice.class);
    private final Map<idle.ui.Goals.Part, Button> goalPartButtons = new EnumMap<>(idle.ui.Goals.Part.class);
    private final Map<idle.ui.Readiness.Kind, Button> dotButtons = new EnumMap<>(idle.ui.Readiness.Kind.class);
    private final Button allNoticesButton = new Button("Toutes affichées");
    private final Button noNoticesButton = new Button("Toutes coupées");
    private final Button skyEventsButton = new Button();
    private final Button clearRecordsButton = new Button("Effacer les records");
    private final Label recordsLabel = new Label();
    private final Map<Settings.Notation, Button> notationButtons = new EnumMap<>(Settings.Notation.class);
    private final Label notationExample = new Label();
    /** La notation dont les exemples sont écrits : ils ne se recalculent que si elle change. */
    private Settings.Notation exampleNotation;
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
        choiceRow(notationFlow, Settings.Notation.values(), Settings.Notation::label, notation -> {
            notationHint = settings.isOpen(notation) ? "" : "Fermée. Indice : " + notation.unlock().hint();
            if (settings.isOpen(notation)) settings.setNotation(notation);
        }, notationButtons, true);
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
        choiceRow(themes, Settings.Theme.values(), Settings.Theme::label, theme -> {
            themeHint = settings.isOpen(theme) ? "" : "Fermé. Indice : " + theme.unlock().hint();
            if (settings.isOpen(theme)) {
                settings.setTheme(theme);
                this.onAppearance.run();
            }
        }, themeButtons, true);
        block("Thème", "Chaque thème filtre les couleurs du thème sombre. Le thème clair les inverse : le fond devient clair, "
                + "les textes foncés, et chaque couleur garde sa teinte. Les autres tournent les teintes, les éteignent ou "
                + "les teintent. Ceux marqués d'un cadenas s'ouvrent en faisant quelque chose de très précis : un clic "
                + "dessus donne un indice.");
        target.getChildren().add(themes);
        note(themeNote);

        HBox scales = new HBox(8);
        scales.setAlignment(Pos.CENTER);
        choiceRow(scales, Settings.Scale.values(), Settings.Scale::label, scale -> {
            settings.setScale(scale);
            this.onAppearance.run();
        }, scaleButtons, false);
        block("Taille de l'interface", "Agrandit ou réduit tout ensemble : les textes, les boutons et les dessins.");
        target.getChildren().add(scales);

        // Les formats de fenêtre (version 0.092) : un clic redimensionne et recentre la fenêtre.
        javafx.scene.layout.FlowPane sizes = new javafx.scene.layout.FlowPane(8, 8);
        sizes.setAlignment(Pos.CENTER);
        sizes.setMaxWidth(760);
        // Le receveur se branche après (setWindowSize) : on le relit au clic.
        choiceRow(sizes, Settings.WindowSize.values(), Settings.WindowSize::text, size -> windowSize.accept(size), sizeButtons, false);
        block("Taille de la fenêtre", "Un format prédéfini, recentré sur l'écran. La fenêtre se redimensionne toujours à la main, "
                + "mais pas en dessous d'une taille où tout reste utilisable ; cette limite grandit avec la taille de l'interface.");
        target.getChildren().add(sizes);

        // Version 0.146 : les notifications, l'objectif et les signes d'état ont leur sous-onglet, et chacun se règle par
        // catégorie sous son grand interrupteur, comme les effets visuels.
        use(Section.NOTICES);
        block("Notifications", "Un message de quelques secondes, en bas à droite, quand il se passe quelque chose. Le grand "
                + "interrupteur les coupe toutes ; sinon, chaque catégorie se coupe à part (survolez-la pour voir ce qu'elle "
                + "annonce). Une catégorie coupée ne s'affiche pas et ne joue pas de son ; le journal des statistiques, lui, "
                + "garde tout.");
        notificationsButton.setOnAction(event -> {
            settings.setNotifications(!settings.notifications());
            refresh();
        });
        target.getChildren().add(notificationsButton);
        target.getChildren().add(categoryRow(idle.ui.Notice.values(), idle.ui.Notice::label,
                kind -> settings.setNotice(kind, !settings.noticeChosen(kind)), noticeButtons));
        for (Button all : new Button[] {allNoticesButton, noNoticesButton}) {
            all.setFocusTraversable(false);
            all.setStyle(OFF_STYLE);
        }
        allNoticesButton.setOnAction(event -> {
            for (idle.ui.Notice kind : idle.ui.Notice.values()) settings.setNotice(kind, true);
            refresh();
        });
        noNoticesButton.setOnAction(event -> {
            for (idle.ui.Notice kind : idle.ui.Notice.values()) settings.setNotice(kind, false);
            refresh();
        });
        HBox allRow = new HBox(8, allNoticesButton, noNoticesButton);
        allRow.setAlignment(Pos.CENTER);
        target.getChildren().add(allRow);

        block("Objectif du moment", "Une ligne sous les onglets rappelle le prochain grand pas de la partie. Chacun de ses "
                + "morceaux se cache à part : l'objectif, le temps qu'il demande, le prochain Big Bang, le troisième acte, et "
                + "le chrono, l'avance ou le retard sur le record de la prochaine étape.");
        goalButton.setOnAction(event -> {
            settings.setShowGoal(!settings.showGoal());
            refresh();
        });
        target.getChildren().add(goalButton);
        target.getChildren().add(categoryRow(idle.ui.Goals.Part.values(), idle.ui.Goals.Part::label,
                part -> settings.setGoalPart(part, !settings.goalPartChosen(part)), goalPartButtons));

        block("Signes d'état", "Chaque carte porte devant son nom le signe de son état, pour le reconnaître sans la "
                + "couleur : ● à portée, ○ en attente, ◐ entamée, ▶ en marche, □ coupée, ✓ acquise, × fermée. Les "
                + "états de la matière ont aussi le leur, toujours écrits : ∴ gaz, ≈ liquides, ■ solides, ◆ cristaux, ▲ métaux.");
        signsButton.setOnAction(event -> {
            settings.setStateSigns(!settings.stateSigns());
            refresh();
        });
        target.getChildren().add(signsButton);

        block("Pastilles des onglets", "Un ● au bout du nom d'un onglet ou d'un sous-onglet dit que quelque chose y attend : "
                + "une case de l'arbre, un défi, un astre à former, une comète… Chaque sorte se cache à part ; cachée, elle "
                + "ne s'allume ni sur son sous-onglet ni sur l'onglet qui le porte.");
        target.getChildren().add(categoryRow(idle.ui.Readiness.Kind.values(), idle.ui.Readiness.Kind::label,
                kind -> settings.setDot(kind, !settings.dot(kind)), dotButtons));

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
        choiceRow(effectRow, Settings.Effect.values(), Settings.Effect::label,
                effect -> settings.setEffect(effect, !settings.effectChosen(effect)), effectButtons, true);
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
        choiceRow(rateRow, Settings.FrameRate.values(), rate -> "", settings::setFrameRate, frameRateButtons, true);
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
        choiceRow(delayRow, Settings.SaveDelay.values(), Settings.SaveDelay::label, settings::setSaveDelay, saveDelayButtons, true);
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
            String seen = importPreview;
            if (importArmed > 0 && saves.previewImport().equals(seen)) {
                importArmed = 0;
                importPreview = "";
                saveMessage = saves.pasteImport();
            } else {
                // Le presse-papiers a changé depuis l'aperçu : on montre le nouveau, et c'est lui que le clic suivant importe.
                // Version 0.127 : le premier clic montre ce que contient le presse-papiers, le second l'importe.
                importPreview = saves.previewImport();
                saveMessage = importPreview;
                importArmed = importPreview.startsWith("Rien") ? 0 : RESET_CONFIRM_SECONDS;
            }
            refresh();
        });
        HBox saveRow = new HBox(8, saveButton, exportButton, importButton);
        saveRow.setAlignment(Pos.CENTER);
        target.getChildren().add(saveRow);
        note(saveNote);

        block("Points de reprise", "Jusqu'à " + saves.checkpointSlots() + " parties gardées à part, chacune sous un nom : "
                + "avant un Big Bang risqué, avant de former l'univers… Reprendre un point remplace la partie en cours, mais "
                + "celle-ci est d'abord mise de côté, dans « Avant la dernière reprise » : une reprise par erreur se défait "
                + "(seule la dernière partie quittée y reste). "
                + "Chaque geste demande un second clic.");
        checkpointName.setPromptText("Nom du point (facultatif)");
        checkpointName.setPrefWidth(320);
        checkpointName.setMaxWidth(320);
        checkpointName.setFocusTraversable(false);
        HBox nameRow = new HBox(8, checkpointName);
        nameRow.setAlignment(Pos.CENTER);
        target.getChildren().add(nameRow);
        for (int slot = 0; slot <= saves.checkpointSlots(); slot++) {
            final int index = slot;
            Label line = new Label();
            line.setStyle("-fx-font-size: 12px; -fx-text-fill: #c9d1d9;");
            line.setWrapText(true);
            line.setMaxWidth(460);
            Button store = new Button("Enregistrer ici");
            Button load = new Button("Reprendre");
            Button erase = new Button("Effacer");
            for (Button button : List.of(store, load, erase)) button.setFocusTraversable(false);
            store.setOnAction(event -> checkpointGesture("save", index));
            load.setOnAction(event -> checkpointGesture("load", index));
            erase.setOnAction(event -> checkpointGesture("delete", index));
            checkpointLabels.add(line);
            checkpointSave.add(store);
            checkpointLoad.add(load);
            checkpointDelete.add(erase);
            // La mise de côté ne s'écrit pas à la main : elle se reprend ou s'efface seulement.
            HBox row = slot == 0 ? new HBox(8, line, load, erase) : new HBox(8, line, store, load, erase);
            row.setAlignment(Pos.CENTER);
            target.getChildren().add(row);
        }
        note(checkpointNote);

        use(Section.GAME);
        block("Records", "Le jeu retient le temps de jeu le plus court auquel chaque étape a été atteinte : "
                + "première fusion, première explosion, premier Big Bang, puis les étapes du troisième acte. Le chrono de la "
                + "ligne d'objectif (il se règle dans « Notifications et signes ») dit l'avance ou le retard sur le record de "
                + "la prochaine étape. Les records traversent la remise à zéro du jeu : c'est à la partie suivante qu'ils servent.");
        clearRecordsButton.setStyle(OFF_STYLE);
        clearRecordsButton.setFocusTraversable(false);
        clearRecordsButton.setOnAction(event -> {
            saves.clearRecords();
            refresh();
        });
        HBox recordsRow = new HBox(8, clearRecordsButton);
        recordsRow.setAlignment(Pos.CENTER);
        target.getChildren().add(recordsRow);
        note(recordsLabel);

        block("Événements du ciel", "Après le premier Big Bang, de petites anomalies traversent parfois l'expansion, "
                + "en plus des comètes : un vent de particules, une fenêtre d'exploration pour les sondes en vol. Elles "
                + "restent quelques minutes ; les saisir donne un petit coup de pouce passager, les manquer ne coûte rien, "
                + "et rien ne s'achète ni ne se débloque avec elles. Masquées, elles passent sans se montrer.");
        skyEventsButton.setOnAction(event -> {
            settings.setSkyEvents(!settings.skyEvents());
            refresh();
        });
        target.getChildren().add(skyEventsButton);

        use(Section.SAVE);
        block("Recommencer de zéro",
                "Efface toute la partie : particules, atomes, tableau périodique, matière noire, arbre et statistiques, "
                        + "sauvegarde comprise, et les réglages de jeu (coupure des automatismes, ordres d'achat, seuil de "
                        + "fusion, verrou de l'appui, favoris). Les réglages de cette page et les records "
                        + "sont gardés. C'est définitif : exportez d'abord "
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
                + "celle de Java et du système, la graine du hasard de la session, l'onglet ouvert, où en est la partie "
                + "(étape, paliers payés, défi en cours), le début de la première erreur s'il y en a eu une, et la partie "
                + "elle-même. Ni chemin de fichier ni nom de compte. Rien n'est envoyé : c'est vous qui le collez où vous voulez.");
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

    /** Un geste sur un point de reprise : le premier clic l'arme, le second le fait. */
    private void checkpointGesture(String gesture, int slot) {
        String key = gesture + ":" + slot;
        if (!key.equals(checkpointArmed) || checkpointArmedFor <= 0) {
            checkpointArmed = key;
            checkpointArmedFor = RESET_CONFIRM_SECONDS;
            checkpointMessage = "";
            refresh();
            return;
        }
        checkpointArmed = "";
        checkpointArmedFor = 0;
        checkpointMessage = switch (gesture) {
            case "save" -> saves.saveCheckpoint(slot, checkpointName.getText());
            case "load" -> saves.loadCheckpoint(slot);
            default -> saves.deleteCheckpoint(slot);
        };
        if (gesture.equals("save")) checkpointName.clear();
        refresh();
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
        Labels.note(label, 560);
        (target == null ? getChildren() : target.getChildren()).add(label);
    }

    /**
     * Une rangée de catégories (version 0.146) : un bouton par catégorie, qui la montre ou la coupe, puis recopie la page.
     * Comme les effets visuels : « ✓ » ou « ✗ » devant son nom.
     */
    private <E> javafx.scene.layout.FlowPane categoryRow(E[] values, java.util.function.Function<E, String> label,
            java.util.function.Consumer<E> flip, Map<E, Button> buttons) {
        javafx.scene.layout.FlowPane row = new javafx.scene.layout.FlowPane(8, 8);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(720);
        choiceRow(row, values, label, flip, buttons, true);
        return row;
    }

    /** Une catégorie : « ✓ » si elle est montrée, éclairée seulement si son grand interrupteur est en marche. */
    private static void category(Button button, String label, String detail, boolean on, boolean master) {
        button.setText((on ? "✓ " : "✗ ") + label);
        button.setStyle(on && master ? ON_STYLE : OFF_STYLE);
        button.setDisable(!master);
        tip(button, detail);
    }

    /** Un bouton à deux états : son texte selon l'état, éclairé quand il est actif. */
    private static void toggle(Button button, boolean on, String onText, String offText) {
        button.setText(on ? onText : offText);
        button.setStyle(on ? ON_STYLE : OFF_STYLE);
    }

    /**
     * Une rangée de choix : un bouton par valeur, qui l'applique puis recopie la page. {@code focusless} retire les
     * boutons du parcours au clavier. Leurs couleurs (celui qui est choisi est éclairé) se posent dans {@link #refresh()}.
     */
    private <E> void choiceRow(javafx.scene.layout.Pane row, E[] values, java.util.function.Function<E, String> label,
            java.util.function.Consumer<E> choose, Map<E, Button> buttons, boolean focusless) {
        for (E value : values) {
            Button button = new Button(label.apply(value));
            if (focusless) button.setFocusTraversable(false);
            button.setOnAction(event -> {
                choose.accept(value);
                refresh();
            });
            buttons.put(value, button);
            row.getChildren().add(button);
        }
    }

    /**
     * Fait passer le temps de la confirmation de remise à zéro : sans second clic, elle s'annule.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    public void frame(double elapsed) {
        if (resetArmed <= 0 && importArmed <= 0 && checkpointArmedFor <= 0) return;
        resetArmed = Math.max(0, resetArmed - elapsed);
        importArmed = Math.max(0, importArmed - elapsed);
        checkpointArmedFor = Math.max(0, checkpointArmedFor - elapsed);
        if (checkpointArmedFor <= 0) checkpointArmed = "";
        refresh();
    }

    /** L'onglet n'est plus affiché : une remise à zéro ou une touche demandées mais pas confirmées sont oubliées. */
    public void release() {
        resetArmed = 0;
        importArmed = 0;
        importPreview = "";
        checkpointArmed = "";
        checkpointArmedFor = 0;
        checkpointMessage = "";
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
        sectionTabs.forEach((section, tab) -> tab.setStyle(SubTabs.style(SubTabs.NARROW_STYLE, section == selected, SETTINGS_COLOR)));
        notationButtons.forEach((notation, button) -> {
            boolean open = settings.isOpen(notation);
            button.setText(open ? notation.label() : "🔒 " + notation.label());
            button.setStyle(notation == settings.notation() ? ON_STYLE : open ? OFF_STYLE : LOCKED_STYLE);
            tip(button, open && notation.unlock() != null ? "Débloquée en : " + notation.unlock().condition() : open ? "" : notation.unlock().hint());
        });
        if (exampleNotation != Format.notation()) {
            exampleNotation = Format.notation();
            StringBuilder example = new StringBuilder("Exemples : ");
            for (int i = 0; i < SAMPLES.length; i++) {
                example.append(i == 0 ? "" : "   ·   ").append(Format.big(SAMPLES[i]));
            }
            notationExample.setText(example.toString());
        }

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
        toggle(offlineButton, settings.offline(), "Temps hors ligne : rejoué au retour", "Temps hors ligne : ignoré");
        scaleButtons.forEach((scale, button) -> button.setStyle(scale == settings.scale() ? ON_STYLE : OFF_STYLE));
        Settings.WindowSize current = sizeNow.get();
        sizeButtons.forEach((size, button) -> {
            boolean fits = sizeFits.test(size);
            button.setDisable(!fits);
            button.setStyle(size == current ? ON_STYLE : OFF_STYLE);
        });
        toggle(notificationsButton, settings.notifications(), "Notifications : affichées", "Notifications : coupées");
        toggle(goalButton, settings.showGoal(), "Objectif : affiché", "Objectif : caché");
        toggle(signsButton, settings.stateSigns(), "Signes d'état : affichés", "Signes d'état : cachés");
        // Les catégories (version 0.146) : éclairées si elles s'affichent, grisées sous un grand interrupteur coupé.
        noticeButtons.forEach((kind, button) -> category(button, kind.label(), kind.detail(), settings.noticeChosen(kind),
                settings.notifications()));
        allNoticesButton.setDisable(!settings.notifications());
        noNoticesButton.setDisable(!settings.notifications());
        goalPartButtons.forEach((part, button) -> category(button, part.label(), part.detail(), settings.goalPartChosen(part),
                settings.showGoal()));
        dotButtons.forEach((kind, button) -> category(button, kind.label(), kind.detail(), settings.dot(kind), true));
        toggle(skyEventsButton, settings.skyEvents(), "Événements du ciel : montrés", "Événements du ciel : masqués");
        recordsLabel.setText(saves.records());
        toggle(effectsButton, settings.effects(), "Effets visuels : affichés", "Effets visuels : coupés");
        tabsTopButton.setStyle(settings.tabsLeft() ? OFF_STYLE : ON_STYLE);
        tabsLeftButton.setStyle(settings.tabsLeft() ? ON_STYLE : OFF_STYLE);
        tabOrderButton.setStyle(OFF_STYLE);
        tabOrderButton.setDisable(settings.tabOrder().isEmpty());
        toggle(soundButton, settings.sound(), "Sons : activés", "Sons : coupés");
        volumeLabel.setText("Volume : " + settings.volume() + " %");
        quieterButton.setDisable(!settings.sound() || settings.volume() <= 0);
        louderButton.setDisable(!settings.sound() || settings.volume() >= 100);
        for (Map.Entry<Settings.FrameRate, Button> entry : frameRateButtons.entrySet()) {
            entry.getValue().setText(entry.getKey().label() + " images/s");
            entry.getValue().setStyle(entry.getKey() == settings.frameRate() ? ON_STYLE : OFF_STYLE);
        }
        toggle(energyButton, settings.saveEnergy(), "Économie d'énergie : oui", "Économie d'énergie : non");
        toggle(confirmButton, settings.confirmExplosion(), "Explosion : demandée", "Explosion : non demandée");
        toggle(confirmBigBangButton, settings.confirmBigBang(), "Big Bang : demandée", "Big Bang : non demandée");
        toggle(confirmChallengeButton, settings.confirmChallenge(), "Défis : demandée", "Défis : non demandée");
        // La liste des raccourcis : chaque touche est un bouton, éclairé pendant qu'il attend la nouvelle.
        keyList.setVisible(settings.shortcuts());
        keyList.setManaged(settings.shortcuts());
        keyButtons.forEach((action, button) -> {
            boolean waiting = capturing && capturingAction == action;
            KeyCode bound = settings.key(action);
            toggle(button, waiting, "Appuyez sur une touche…", bound == null ? "(aucune)" : Shortcuts.name(bound));
        });
        boolean custom = settings.shortcuts() && settings.hasCustomKeys();
        resetKeysButton.setVisible(custom);
        resetKeysButton.setManaged(custom);
        show(keyNote, capturingAction != null ? captureProblem : "");
        toggle(shortcutsButton, settings.shortcuts(), "Raccourcis clavier : actifs", "Raccourcis clavier : coupés");
        String key = Detail.keyName(settings.detailKey());
        boolean detailWaiting = capturing && capturingAction == null;
        toggle(detailKeyButton, detailWaiting, "Appuyez sur une touche… (Échap annule)", "Touche : " + key + " (changer)");
        detailModeButton.setText(settings.detailToggle() ? "Un appui bascule" : "À tenir enfoncée");
        detailModeButton.setStyle(OFF_STYLE);
        show(detailKeyNote, capturingAction == null ? captureProblem : "");
        // Les explications, comme partout : seulement en mode détails.
        for (Label explanation : explanations) {
            explanation.setVisible(Detail.shown());
            explanation.setManaged(Detail.shown());
        }
        toggle(pauseButton, settings.paused(), "En pause : cliquer pour reprendre", "Mettre en pause");
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
        for (int slot = 0; slot < checkpointLabels.size(); slot++) {
            boolean used = saves.checkpointUsed(slot);
            checkpointLabels.get(slot).setText((slot == 0 ? "Mise de côté · " : slot + ". ") + saves.checkpointLine(slot));
            armedLook(checkpointSave.get(slot), "save:" + slot, "Enregistrer ici", used ? "Remplacer ce point ?" : "Enregistrer ?", true);
            armedLook(checkpointLoad.get(slot), "load:" + slot, "Reprendre", "Remplacer la partie en cours ?", used);
            armedLook(checkpointDelete.get(slot), "delete:" + slot, "Effacer", "Effacer ce point ?", used);
        }
        show(checkpointNote, checkpointMessage);
        toggle(notesButton, notesShown, "Cacher les notes de version", "Notes de version");
        notesLabel.setVisible(notesShown);
        notesLabel.setManaged(notesShown);
    }

    /** Un bouton de point de reprise : son texte, armé ou non, et s'il sert à quelque chose. */
    private void armedLook(Button button, String key, String idle, String armed, boolean usable) {
        boolean on = key.equals(checkpointArmed) && checkpointArmedFor > 0;
        button.setText(on ? armed + " (" + (int) Math.ceil(checkpointArmedFor) + " s)" : idle);
        button.setStyle(on ? DANGER_ARMED_STYLE : OFF_STYLE);
        button.setDisable(!usable);
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

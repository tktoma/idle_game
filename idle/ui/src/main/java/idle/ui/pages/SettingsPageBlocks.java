package idle.ui.pages;

import idle.core.math.BigNum;
import idle.core.Game;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import idle.ui.GameApp;
import idle.ui.Goals;
import idle.ui.Notice;
import idle.ui.Readiness;
import idle.ui.Settings;
import idle.ui.Shortcuts;
import idle.ui.SoundKind;
import idle.ui.Version;
import idle.ui.common.Detail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

/** Une partie de {@link SettingsPage} (découpée en 0.293 pour rester sous 1 000 lignes) : ses champs et la construction des blocs de réglages. */
public abstract class SettingsPageBlocks extends VBox {
    // Ce que fournissent les parties suivantes.
    abstract <E> FlowPane categoryRow(E[] values, Function<E, String> label, Consumer<E> flip, Map<E, Button> buttons);
    abstract <E> void choiceRow(Pane row, E[] values, Function<E, String> label, Consumer<E> choose, Map<E, Button> buttons, boolean focusless);
    abstract public void refresh();
    abstract public void select(Section section);
    abstract void block(String name, String explanation);
    abstract void changeMusicVolume(int step);
    abstract void changeVolume(int step);
    abstract void note(Label label);
    abstract void use(Section section);

    /** Les sous-onglets des réglages. */
    public enum Section {
        DISPLAY("Affichage"),
        /** Version 0.146 : les notifications, l'objectif et les signes d'état, réunis et réglés par catégorie. */
        NOTICES("Notifications et signes"),
        EFFECTS("Effets et sons"),
        GAME("Jeu"),
        KEYS("Clavier"),
        SAVE("Partie");

        final String label;

        Section(String label) { this.label = label; }

        public String label() { return label; }
    }

    static final String SETTINGS_COLOR = "#d6dde6";

    static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 18; -fx-cursor: hand;"
            + " -fx-background-radius: 6; -fx-border-radius: 6;";

    static final String ON_STYLE = BUTTON_STYLE
            + " -fx-text-fill: #0b0e14; -fx-background-color: " + SETTINGS_COLOR + "; -fx-border-color: #ffffff;";

    static final String OFF_STYLE = BUTTON_STYLE
            + " -fx-text-fill: " + SETTINGS_COLOR + "; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;";

    /** Un thème ou une notation encore fermé : grisé, en pointillés. */
    static final String LOCKED_STYLE = BUTTON_STYLE
            + " -fx-text-fill: #6f7f92; -fx-background-color: #10161f; -fx-border-color: #3a4a5e; -fx-border-style: segments(4, 3);";

    static final String DANGER_STYLE = BUTTON_STYLE
            + " -fx-text-fill: #ffb4a8; -fx-background-color: #2a1614; -fx-border-color: #b8584a;";

    static final String DANGER_ARMED_STYLE = BUTTON_STYLE
            + " -fx-text-fill: #1a0a08; -fx-background-color: #ff8a75; -fx-border-color: #ffffff;";

    /** Temps laissé pour le second clic de la remise à zéro, en secondes. */
    static final double RESET_CONFIRM_SECONDS = 5;

    /** Nombres d'exemple pour montrer une notation. */
    static final BigNum[] SAMPLES = {BigNum.of(1.2345, 7), BigNum.of(4.56, 21), BigNum.of(7.891, 44)};

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

        /** Ce que contient la partie du presse-papiers, sans rien changer ; ou pourquoi elle ne se lit pas. */
        String previewImport();

        /** Le nombre de points de reprise nommés. */
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

    final Settings settings;

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    final FlowPane subTabs = new FlowPane();

    final Map<Section, Button> sectionTabs = new EnumMap<>(Section.class);

    final Map<Section, VBox> sections = new EnumMap<>(Section.class);

    Section selected = Section.DISPLAY;

    /** Le sous-onglet que remplit la construction de la page. */
    VBox target;

    final Label themeNote = new Label();

    /** L'indice du dernier thème fermé cliqué, ou rien. */
    String themeHint = "";

    String notationHint = "";

    static final long LOCKED_THEMES = Arrays.stream(Settings.Theme.values()).filter(t -> t.unlock() != null).count();

    static final long LOCKED_NOTATIONS = Arrays.stream(Settings.Notation.values()).filter(n -> n.unlock() != null).count();

    final Label notationNote = new Label();

    final Map<Settings.Effect, Button> effectButtons = new EnumMap<>(Settings.Effect.class);

    /** Les catégories de sons, une par bouton : gardée ou coupée. */
    final Map<SoundKind, Button> soundKindButtons = new EnumMap<>(SoundKind.class);

    final FlowPane soundKindRow = new FlowPane(8, 8);

    final FlowPane effectRow = new FlowPane(8, 8);

    final Map<Settings.SaveDelay, Button> saveDelayButtons = new EnumMap<>(Settings.SaveDelay.class);

    final Button offlineButton = new Button();

    final Runnable onReset;

    final Runnable onAppearance;

    final Saves saves;

    final Label saveStatus = new Label();

    final Button saveButton = new Button("Sauvegarder maintenant");

    final Button exportButton = new Button("Exporter (copier)");

    final Button importButton = new Button();

    /** Ce qu'a donné le dernier geste de sauvegarde, d'export ou d'import ; vide tant qu'il n'y en a pas eu. */
    String saveMessage = "";

    final Label saveNote = new Label();

    /** Secondes restantes pour confirmer l'import ; 0 quand il n'est pas demandé. */
    double importArmed = 0;

    /** Les points de reprise : un nom à donner, et une ligne par emplacement. */
    final TextField checkpointName = new TextField();

    final List<Label> checkpointLabels = new ArrayList<>();

    final List<Button> checkpointSave = new ArrayList<>();

    final List<Button> checkpointLoad = new ArrayList<>();

    final List<Button> checkpointDelete = new ArrayList<>();

    /** Le geste armé sur un point, attendant son second clic : « save:2 », « load:0 »… et le temps qu'il lui reste. */
    String checkpointArmed = "";

    double checkpointArmedFor = 0;

    String checkpointMessage = "";

    final Label checkpointNote = new Label();

    /** Ce que contient la partie du presse-papiers, montré au premier clic sur « Importer ». */
    String importPreview = "";

    final Button bugButton = new Button("Copier le rapport de bug");

    String bugMessage = "";

    final Label bugNote = new Label();

    final Button notesButton = new Button();

    final Label notesLabel = new Label(Version.notes());

    boolean notesShown = false;

    final Map<Settings.Theme, Button> themeButtons = new EnumMap<>(Settings.Theme.class);

    final Map<Settings.Scale, Button> scaleButtons = new EnumMap<>(Settings.Scale.class);

    final Map<Settings.WindowSize, Button> sizeButtons = new EnumMap<>(Settings.WindowSize.class);

    Consumer<Settings.WindowSize> windowSize = size -> { };

    Predicate<Settings.WindowSize> sizeFits = size -> true;

    Supplier<Settings.WindowSize> sizeNow = () -> null;

    final Button notificationsButton = new Button();

    final Button goalButton = new Button();

    final Button signsButton = new Button();

    /** Les catégories de la section « Notifications et signes » : un bouton par catégorie. */
    final Map<Notice, Button> noticeButtons = new EnumMap<>(Notice.class);

    final Map<Goals.Part, Button> goalPartButtons = new EnumMap<>(Goals.Part.class);

    final Map<Readiness.Kind, Button> dotButtons = new EnumMap<>(Readiness.Kind.class);

    final Button allNoticesButton = new Button("Toutes affichées");

    final Button noNoticesButton = new Button("Toutes coupées");

    final Button skyEventsButton = new Button();

    final Button clearRecordsButton = new Button("Effacer les records");

    final Label recordsLabel = new Label();

    final Map<Settings.Notation, Button> notationButtons = new EnumMap<>(Settings.Notation.class);

    final Label notationExample = new Label();

    /** La notation dont les exemples sont écrits : ils ne se recalculent que si elle change. */
    Settings.Notation exampleNotation;

    final Button effectsButton = new Button();

    final Button soundButton = new Button();

    final Button tabsTopButton = new Button("En haut (la disposition actuelle)");

    final Button tabsLeftButton = new Button("À gauche (la nouvelle)");

    final Button tabOrderButton = new Button("Remettre les onglets dans l'ordre d'origine");

    final Button quieterButton = new Button("−");

    final Button louderButton = new Button("+");

    final Label volumeLabel = new Label();

    /** La musique de fond : son interrupteur et son volume, sur leur ligne. */
    final Button musicButton = new Button();

    final Button automationSoundsButton = new Button();

    final Button musicQuieterButton = new Button("−");

    final Button musicLouderButton = new Button("+");

    final Label musicVolumeLabel = new Label();

    final Map<Settings.FrameRate, Button> frameRateButtons = new EnumMap<>(Settings.FrameRate.class);

    final Button energyButton = new Button();

    /** Joue un son d'essai au volume réglé : branché par la fenêtre, qui possède les sons. */
    Runnable soundTest = () -> { };

    final Button confirmButton = new Button();

    final Button confirmBigBangButton = new Button();

    final Button confirmChallengeButton = new Button();

    final Button shortcutsButton = new Button();

    /** Une ligne par raccourci : ce qu'il fait, et le bouton qui porte sa touche. */
    final Map<Shortcuts.Action, Button> keyButtons = new EnumMap<>(Shortcuts.Action.class);

    final VBox keyList = new VBox(4);

    final Button resetKeysButton = new Button("Remettre les touches d'origine");

    final Label keyNote = new Label();

    /**
     * Le raccourci dont la page attend la nouvelle touche ; {@code null} quand c'est la touche de détail ou celle d'un
     * cercle de la boucle qu'elle attend.
     */
    Shortcuts.Action capturingAction = null;

    /** Le cercle de la boucle dont la page attend la nouvelle touche, ou −1. */
    int capturingRing = -1;

    /** Une ligne par cercle de la boucle : le bouton qui porte sa touche. */
    final List<Button> loopKeyButtons = new ArrayList<>();

    final Button resetLoopKeysButton = new Button("Remettre les touches d'origine des cercles");

    final Label loopKeyNote = new Label();

    final Button detailKeyButton = new Button();

    final Button detailModeButton = new Button();

    /** Vrai tant que la page attend la touche qui deviendra la touche de détail. */
    boolean capturing = false;

    /** Ce que la dernière touche proposée avait de travers, à dire sous le bouton ; vide sinon. */
    String captureProblem = "";

    final Label detailKeyNote = new Label();

    /** Les explications des réglages : elles ne s'affichent qu'en mode détails. */
    final List<Label> explanations = new ArrayList<>();

    final Button pauseButton = new Button();

    final Button resetButton = new Button();

    /** Secondes restantes pour confirmer la remise à zéro ; 0 quand elle n'est pas demandée. */
    double resetArmed = 0;

    /**
     * @param settings les réglages à afficher et à modifier
     * @param onReset      ce qu'il faut faire quand le joueur a confirmé qu'il recommence de zéro
     * @param onAppearance ce qu'il faut faire quand le thème ou la taille de l'interface change
     * @param saves        ce que la fenêtre sait faire de la sauvegarde
     */
    public SettingsPageBlocks(Settings settings, Runnable onReset, Runnable onAppearance, Saves saves) {
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
        FlowPane notationFlow = new FlowPane(8, 8);
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
        FlowPane themes = new FlowPane(8, 8);
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

        // Les formats de fenêtre : un clic redimensionne et recentre la fenêtre.
        FlowPane sizes = new FlowPane(8, 8);
        sizes.setAlignment(Pos.CENTER);
        sizes.setMaxWidth(760);
        // Le receveur se branche après (setWindowSize) : on le relit au clic.
        choiceRow(sizes, Settings.WindowSize.values(), Settings.WindowSize::text, size -> windowSize.accept(size), sizeButtons, false);
        block("Taille de la fenêtre", "Un format prédéfini, recentré sur l'écran. La fenêtre se redimensionne toujours à la main, "
                + "mais pas en dessous d'une taille où tout reste utilisable ; cette limite grandit avec la taille de l'interface.");
        target.getChildren().add(sizes);

        // Les notifications, l'objectif et les signes d'état ont leur sous-onglet, et chacun se règle par
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
        target.getChildren().add(categoryRow(Notice.values(), Notice::label,
                kind -> settings.setNotice(kind, !settings.noticeChosen(kind)), noticeButtons));
        for (Button all : new Button[] {allNoticesButton, noNoticesButton}) {
            all.setFocusTraversable(false);
            all.setStyle(OFF_STYLE);
        }
        allNoticesButton.setOnAction(event -> {
            for (Notice kind : Notice.values()) settings.setNotice(kind, true);
            refresh();
        });
        noNoticesButton.setOnAction(event -> {
            for (Notice kind : Notice.values()) settings.setNotice(kind, false);
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
        target.getChildren().add(categoryRow(Goals.Part.values(), Goals.Part::label,
                part -> settings.setGoalPart(part, !settings.goalPartChosen(part)), goalPartButtons));

        block("Signes d'état", "Chaque carte porte devant son nom le signe de son état, pour le reconnaître sans la "
                + "couleur : ● à portée, ○ en attente, ◐ entamée, ▶ en marche, □ coupée, ✓ acquise, × fermée. Les "
                + "états de la matière ont aussi le leur, toujours écrits : ∴ gaz, ≈ liquides, ◆ cristaux.");
        signsButton.setOnAction(event -> {
            settings.setStateSigns(!settings.stateSigns());
            refresh();
        });
        target.getChildren().add(signsButton);

        block("Pastilles des onglets", "Un ● au bout du nom d'un onglet ou d'un sous-onglet dit que quelque chose y attend : "
                + "une case de l'arbre, un défi, un astre à former, une comète… Chaque sorte se cache à part ; cachée, elle "
                + "ne s'allume ni sur son sous-onglet ni sur l'onglet qui le porte.");
        target.getChildren().add(categoryRow(Readiness.Kind.values(), Readiness.Kind::label,
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

        block("Sons et musique",
                "Un son accompagne les grands moments : succès, élément nouveau, explosion, Big Bang, univers formé, fin de la "
                        + "partie, palier du défi final, défi qui s'ouvre et défi réussi, skin débloqué, palier de vitesse, matière "
                        + "noire, comète, jalon ; les autres annonces ont un son commun, plus léger. Un achat (et chaque carte de "
                        + "récompense d'une sonde), un interrupteur et chaque geste dans le mini-jeu d'une dimension ont aussi le leur, "
                        + "plus discret, comme les bruitages de ce qui se passe sous vos yeux : une particule récupérée (plus grave à "
                        + "chaque palier de vitesse), une fusion, un atome créé (sur le grand atome de l'onglet Atomes), une collision "
                        + "réussie (sur Collisionneur › Énergie), des hadrons (deux à la fois au plus), une sonde lancée, la graine "
                        + "(l'eau de l'arrosoir qu'on tient, le tuyau qui arrose, l'orage et le beau temps quand on la voit, un "
                        + "rond de croissance cueilli, une saison qui commence), un point gagné dans une dimension, la membrane de la brane (sa "
                        + "corde se tend avec la tirée, puis part d'autant plus fort), l'appui de la durée (une note qui monte "
                        + "jusqu'au plus long appui), le piano du miroir, chaque éclat ramassé du mélange, le tic-tac du chrono tant qu'il "
                        + "accélère le jeu, un clic, un changement d'onglet. Des fonds en boucle accompagnent certaines vues : le grondement de la matière noire "
                        + "qui grossit et le collisionneur (sur Énergie) accélèrent avec elles ; l'expansion de la matière a le sien, et les "
                        + "paires du vide (sur Le vide › Paires) le leur, plus fort à mesure que le trou noir se remplit. Chaque sorte de "
                        + "son se garde ou se coupe à part, en bas de ce bloc. La "
                        + "fusion, l'explosion et le Big Bang partent avec leur animation ; ce que le jeu fait seul (une fusion "
                        + "ou une explosion automatiques, un palier de vitesse atteint par l'achat automatique, un automatisme offert, un élément "
                        + "de la synthèse automatique) reste muet, sauf si « Sons des automatismes » est activé. Aucun son n'attend "
                        + "avant de revenir : seul le nombre de sons joués en même temps est borné. Changer le volume fait entendre un essai. Le métronome de la boucle, qui se branche "
                        + "dans sa fenêtre, suit ce réglage et ce volume. La musique, composée par le jeu pendant qu'on joue, suit "
                        + "la partie : chaque étape d'un univers a la sienne (l'aube, l'élan, les Big Bangs, les astres, l'amas, la "
                        + "formation), un défi la resserre, une dimension ouverte a son arpège, et chaque univers change de tonalité. "
                        + "Elle a son interrupteur et son volume.");
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
        musicButton.setOnAction(event -> {
            settings.setMusic(!settings.music());
            refresh();
        });
        musicQuieterButton.setOnAction(event -> changeMusicVolume(-VOLUME_STEP));
        musicLouderButton.setOnAction(event -> changeMusicVolume(VOLUME_STEP));
        for (Button button : List.of(musicQuieterButton, musicLouderButton)) {
            button.setStyle(OFF_STYLE);
            button.setFocusTraversable(false);
        }
        musicVolumeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #c9d1d9; -fx-min-width: 110; -fx-alignment: center;");
        HBox musicRow = new HBox(8, musicButton, musicQuieterButton, musicVolumeLabel, musicLouderButton);
        musicRow.setAlignment(Pos.CENTER);
        target.getChildren().add(musicRow);
        automationSoundsButton.setOnAction(event -> {
            settings.setAutomationSounds(!settings.automationSounds());
            refresh();
        });
        HBox automationRow = new HBox(8, automationSoundsButton);
        automationRow.setAlignment(Pos.CENTER);
        target.getChildren().add(automationRow);
        // note() pose le libellé sur la page : l'y ajouter une seconde fois fait planter JavaFX au lancement.
        note(new Label("Les sons à garder, un par un :"));
        soundKindRow.setAlignment(Pos.CENTER);
        soundKindRow.setMaxWidth(640);
        choiceRow(soundKindRow, SoundKind.values(), SoundKind::label,
                kind -> settings.setSoundKind(kind, !settings.soundKind(kind)), soundKindButtons, true);
        target.getChildren().add(soundKindRow);

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
                capturingRing = -1;
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
            capturingRing = -1;
            captureProblem = "";
            refresh();
        });
        target.getChildren().add(resetKeysButton);

        block("Touche de détail", "Les cartes et les pages s'en tiennent à l'essentiel. Cette touche fait apparaître "
                + "leurs explications complètes : ce que fait chaque amélioration, d'où vient chaque nombre. "
                + "On peut la tenir enfoncée, ou la faire basculer d'un appui.");
        detailKeyButton.setOnAction(event -> {
            capturing = !(capturing && capturingAction == null && capturingRing < 0);
            capturingAction = null;
            capturingRing = -1;
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

        // Une touche par cercle de la boucle, la cinquième dimension.
        block("Touches de la boucle", "Dans la fenêtre de la boucle, la cinquième dimension, chaque cercle se joue d'un clic "
                + "ou de sa touche, jusqu'à sept. Elles ne servent que fenêtre de la boucle ouverte : une touche peut être "
                + "à la fois celle d'un cercle et celle d'un raccourci, qui attend que la fenêtre se ferme. Deux cercles ne "
                + "peuvent pas partager une touche, ni prendre la touche de détail.");
        VBox loopKeyList = new VBox(4);
        loopKeyList.setAlignment(Pos.CENTER);
        for (int ring = 0; ring < Game.MAX_RINGS; ring++) {
            int number = ring;
            Label name = new Label("Cercle " + (ring + 1) + (ring == 0 ? " (le premier, dehors)" : ""));
            name.setStyle("-fx-font-size: 13px; -fx-text-fill: #b7c4d2;");
            name.setMinWidth(300);
            name.setPrefWidth(300);
            Button key = new Button();
            key.setMinWidth(150);
            key.setFocusTraversable(false);
            key.setOnAction(event -> {
                boolean same = capturing && capturingRing == number;
                capturing = !same;
                capturingAction = null;
                capturingRing = same ? -1 : number;
                captureProblem = "";
                refresh();
            });
            loopKeyButtons.add(key);
            HBox row = new HBox(10, name, key);
            row.setAlignment(Pos.CENTER);
            loopKeyList.getChildren().add(row);
        }
        target.getChildren().add(loopKeyList);
        note(loopKeyNote);
        resetLoopKeysButton.setStyle(OFF_STYLE);
        resetLoopKeysButton.setFocusTraversable(false);
        resetLoopKeysButton.setOnAction(event -> {
            settings.resetLoopKeys();
            capturing = false;
            capturingRing = -1;
            captureProblem = "";
            refresh();
        });
        target.getChildren().add(resetLoopKeysButton);

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
                // Le premier clic montre ce que contient le presse-papiers, le second l'importe.
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
    void checkpointGesture(String gesture, int slot) {
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

    /** Pas du volume à chaque clic sur − ou + : dix crans de 0 à 100. */
    static final int VOLUME_STEP = 10;

    /** Une catégorie : « ✓ » si elle est montrée, éclairée seulement si son grand interrupteur est en marche. */
    static void category(Button button, String label, String detail, boolean on, boolean master) {
        button.setText((on ? "✓ " : "✗ ") + label);
        button.setStyle(on && master ? ON_STYLE : OFF_STYLE);
        button.setDisable(!master);
        tip(button, detail);
    }

    /** Un bouton à deux états : son texte selon l'état, éclairé quand il est actif. */
    static void toggle(Button button, boolean on, String onText, String offText) {
        button.setText(on ? onText : offText);
        button.setStyle(on ? ON_STYLE : OFF_STYLE);
    }

    /** L'info-bulle d'un bouton, ou aucune. */
    static void tip(Button button, String text) {
        if (text.isEmpty()) {
            if (button.getTooltip() != null) button.setTooltip(null);
            return;
        }
        if (button.getTooltip() != null && text.equals(button.getTooltip().getText())) return;
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(360);
        button.setTooltip(tooltip);
    }

    /** Affiche une phrase sous un bouton, ou retire sa place quand il n'y a rien à dire. */
    static void show(Label label, String text) {
        label.setText(text);
        label.setVisible(!text.isEmpty());
        label.setManaged(!text.isEmpty());
    }
}

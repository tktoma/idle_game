package idle.ui;

import idle.core.BigNum;
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
 * Contenu de l'onglet « Réglages » : les réglages de base, un bloc chacun.
 * <ul>
 *   <li><b>Notation</b> des grands nombres : scientifique, ingénieur ou lettres ;</li>
 *   <li><b>Thème</b> sombre ou clair, et <b>taille de l'interface</b> ;</li>
 *   <li><b>Notifications</b> et <b>objectif du moment</b> : affichés ou non ;</li>
 *   <li><b>Effets visuels</b> : l'éclair de l'explosion, le fond animé, l'onglet qui bat ;</li>
 *   <li><b>Confirmations</b> : avant l'explosion, avant le Big Bang, avant un défi, chacune à part ;</li>
 *   <li><b>Raccourcis clavier</b> : actifs ou coupés, avec leur liste ; un clic sur une touche la change ;</li>
 *   <li><b>Touche de détail</b> : laquelle, et s'il faut la tenir ou si elle bascule ;</li>
 *   <li><b>Pause</b> : le temps ne passe plus ;</li>
 *   <li><b>Sauvegarde</b> : où elle en est, sauvegarder tout de suite, exporter la partie dans le
 *       presse-papiers, en importer une (en deux clics : elle remplace la partie en cours) ;</li>
 *   <li><b>Recommencer</b> : efface toute la partie, en deux clics ;</li>
 *   <li><b>Rapport de bug</b> : copie de quoi reproduire un problème ;</li>
 *   <li><b>Version</b> du jeu, et ses notes.</li>
 * </ul>
 * Chaque réglage s'applique tout de suite. Les règles du jeu ne changent pas : les réglages sont
 * dans {@link Settings}, pas dans core.
 *
 * <p>Comme partout dans le jeu, les explications ne s'affichent qu'en mode détails
 * ({@link Detail}) : le reste du temps, chaque réglage tient en un titre et un bouton.
 */
final class SettingsPage extends VBox {

    private static final String SETTINGS_COLOR = "#d6dde6";
    private static final String BUTTON_STYLE = "-fx-font-size: 13px; -fx-padding: 8 18; -fx-cursor: hand;"
            + " -fx-background-radius: 6; -fx-border-radius: 6;";
    private static final String ON_STYLE = BUTTON_STYLE
            + " -fx-text-fill: #0b0e14; -fx-background-color: " + SETTINGS_COLOR + "; -fx-border-color: #ffffff;";
    private static final String OFF_STYLE = BUTTON_STYLE
            + " -fx-text-fill: " + SETTINGS_COLOR + "; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;";
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
    interface Saves {
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
    private final Button notificationsButton = new Button();
    private final Button goalButton = new Button();
    private final Button signsButton = new Button();
    private final Button chronoButton = new Button();
    private final Button clearRecordsButton = new Button("Effacer les records");
    private final Label recordsLabel = new Label();
    private final Map<Settings.Notation, Button> notationButtons = new EnumMap<>(Settings.Notation.class);
    private final Label notationExample = new Label();
    private final Button effectsButton = new Button();
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
    SettingsPage(Settings settings, Runnable onReset, Runnable onAppearance, Saves saves) {
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

        // Notation des grands nombres : un bouton par notation, celui choisi est éclairé.
        HBox notations = new HBox(8);
        notations.setAlignment(Pos.CENTER);
        for (Settings.Notation notation : Settings.Notation.values()) {
            Button button = new Button(notation.label());
            button.setOnAction(event -> {
                settings.setNotation(notation);
                refresh();
            });
            notationButtons.put(notation, button);
            notations.getChildren().add(button);
        }
        block("Notation des grands nombres",
                "Comment s'écrivent les nombres à partir d'un million, partout dans le jeu.");
        getChildren().add(notations);
        note(notationExample);

        // Le thème et la taille : ils s'appliquent tout de suite à toute la fenêtre.
        HBox themes = new HBox(8);
        themes.setAlignment(Pos.CENTER);
        for (Settings.Theme theme : Settings.Theme.values()) {
            Button button = new Button(theme.label());
            button.setOnAction(event -> {
                settings.setTheme(theme);
                this.onAppearance.run();
                refresh();
            });
            themeButtons.put(theme, button);
            themes.getChildren().add(button);
        }
        block("Thème", "Le thème clair reprend les couleurs du thème sombre en les inversant : le fond devient clair, "
                + "les textes foncés, et chaque couleur garde sa teinte.");
        getChildren().add(themes);

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
        getChildren().add(scales);

        block("Notifications", "Un message de quelques secondes, en bas à droite, quand il se passe quelque chose : "
                + "nouvel élément, ensemble réuni, succès, déblocage, explosion possible.");
        notificationsButton.setOnAction(event -> {
            settings.setNotifications(!settings.notifications());
            refresh();
        });
        getChildren().add(notificationsButton);

        block("Objectif du moment", "Une ligne sous les onglets rappelle le prochain grand pas de la partie, et "
                + "le temps qu'il demande quand le jeu peut l'estimer.");
        goalButton.setOnAction(event -> {
            settings.setShowGoal(!settings.showGoal());
            refresh();
        });
        getChildren().add(goalButton);

        block("Signes d'état", "Chaque carte porte devant son nom le signe de son état, pour le reconnaître sans la "
                + "couleur : ● à portée, ○ en attente, ◐ entamée, ▶ en marche, □ coupée, ✓ acquise, × fermée. Les "
                + "états de la matière ont aussi le leur : ∴ gaz, ≈ liquides, ■ solides, ◆ cristaux, ▲ métaux.");
        signsButton.setOnAction(event -> {
            settings.setStateSigns(!settings.stateSigns());
            refresh();
        });
        getChildren().add(signsButton);

        block("Effets visuels",
                "L'éclair blanc de l'explosion, le fond animé derrière les générateurs et l'onglet qui bat à chaque "
                        + "nouvel atome. Coupés, l'explosion a lieu sans éclair : à préférer si les flashs vous gênent.");
        effectsButton.setOnAction(event -> {
            settings.setEffects(!settings.effects());
            refresh();
        });
        getChildren().add(effectsButton);

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
        getChildren().add(confirmRow);

        block("Raccourcis clavier",
                "Ils marchent depuis n'importe quel onglet. Les trois premiers achètent la quantité choisie dans l'onglet "
                        + "Particules (×1, ×10 ou max). Un clic sur une touche permet de la changer : appuyez ensuite sur "
                        + "la nouvelle, ou sur Échap pour annuler. Deux raccourcis ne peuvent pas partager une touche.");
        shortcutsButton.setOnAction(event -> {
            settings.setShortcuts(!settings.shortcuts());
            refresh();
        });
        getChildren().add(shortcutsButton);
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
        getChildren().add(keyList);
        note(keyNote);
        resetKeysButton.setStyle(OFF_STYLE);
        resetKeysButton.setOnAction(event -> {
            settings.resetKeys();
            capturing = false;
            capturingAction = null;
            captureProblem = "";
            refresh();
        });
        getChildren().add(resetKeysButton);

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
        getChildren().add(detailRow);
        note(detailKeyNote);

        block("Pause", "En pause, le temps ne passe plus : rien ne se crée, aucun automatisme n'agit. "
                + "Le jeu repart toujours en marche au lancement.");
        pauseButton.setOnAction(event -> {
            settings.setPaused(!settings.paused());
            refresh();
        });
        getChildren().add(pauseButton);

        block("Sauvegarde", "La partie s'écrit toute seule toutes les " + (int) SaveStore.EVERY_SECONDS + " secondes et à la "
                + "fermeture du jeu. Au retour, une part du temps d'absence est rejouée : 10 % avant la première explosion, "
                + "40 % ensuite, 70 % après le premier Big Bang, 100 % une fois l'univers formé (vingt-quatre heures "
                + "d'absence au plus). Exporter copie toute la partie dans le presse-papiers, sur une ligne : pour la "
                + "garder ailleurs ou la reprendre sur une autre machine. Importer remplace la partie en cours par celle du "
                + "presse-papiers.");
        note(saveStatus);
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
        getChildren().add(saveRow);
        note(saveNote);

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
        getChildren().add(recordsRow);
        note(recordsLabel);

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
        getChildren().add(resetButton);

        block("Rapport de bug", "Copie dans le presse-papiers de quoi reproduire un problème : la version du jeu, "
                + "celle de Java et du système, la première erreur survenue s'il y en a eu une, et la partie elle-même. "
                + "Rien n'est envoyé : c'est vous qui le collez où vous voulez.");
        bugButton.setStyle(OFF_STYLE);
        bugButton.setOnAction(event -> {
            bugMessage = saves.copyBugReport();
            refresh();
        });
        getChildren().add(bugButton);
        note(bugNote);

        block("Version " + Version.CURRENT, "Ce qui a changé, version après version.");
        notesButton.setOnAction(event -> {
            notesShown = !notesShown;
            refresh();
        });
        getChildren().add(notesButton);
        note(notesLabel);
        notesLabel.setTextAlignment(TextAlignment.LEFT);

        Label footer = new Label("Les réglages sont gardés d'un lancement à l'autre, sur cette machine. La partie est "
                + "sauvegardée à part : l'exporter l'emporte, les réglages non.");
        note(footer);
        footer.setStyle("-fx-font-size: 12px; -fx-padding: 18 0 0 0; -fx-text-fill: #8fa3b8;");
        refresh();
    }

    /** Titre et explication d'un réglage ; l'explication n'apparaît qu'en mode détails. */
    private void block(String name, String explanation) {
        Label header = new Label(name);
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 14 0 0 0; -fx-text-fill: " + SETTINGS_COLOR + ";");
        getChildren().add(header);
        Label text = new Label(explanation);
        note(text);
        explanations.add(text);
    }

    /** Vrai tant que la page attend la prochaine touche du clavier pour en faire la touche de détail. */
    boolean isCapturing() {
        return capturing;
    }

    /**
     * La touche pressée pendant que la page en attendait une, pour la touche de détail ou pour un
     * raccourci. Échap annule ; une touche déjà prise est refusée, et la page continue d'attendre.
     */
    void capture(KeyCode key) {
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
        getChildren().add(label);
    }

    /**
     * Fait passer le temps de la confirmation de remise à zéro : sans second clic, elle s'annule.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    void frame(double elapsed) {
        if (resetArmed <= 0 && importArmed <= 0) return;
        resetArmed = Math.max(0, resetArmed - elapsed);
        importArmed = Math.max(0, importArmed - elapsed);
        refresh();
    }

    /** L'onglet n'est plus affiché : une remise à zéro ou une touche demandées mais pas confirmées sont oubliées. */
    void release() {
        resetArmed = 0;
        importArmed = 0;
        saveMessage = "";
        bugMessage = "";
        capturing = false;
        capturingAction = null;
        captureProblem = "";
    }

    /** Recopie les réglages dans les boutons. */
    void refresh() {
        notationButtons.forEach((notation, button) ->
                button.setStyle(notation == settings.notation() ? ON_STYLE : OFF_STYLE));
        StringBuilder example = new StringBuilder("Exemples : ");
        for (int i = 0; i < SAMPLES.length; i++) {
            example.append(i == 0 ? "" : "   ·   ").append(Format.big(SAMPLES[i]));
        }
        notationExample.setText(example.toString());

        themeButtons.forEach((theme, button) -> button.setStyle(theme == settings.theme() ? ON_STYLE : OFF_STYLE));
        scaleButtons.forEach((scale, button) -> button.setStyle(scale == settings.scale() ? ON_STYLE : OFF_STYLE));
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

    /** Affiche une phrase sous un bouton, ou retire sa place quand il n'y a rien à dire. */
    private static void show(Label label, String text) {
        label.setText(text);
        label.setVisible(!text.isEmpty());
        label.setManaged(!text.isEmpty());
    }
}

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
 *   <li><b>Confirmation</b> avant l'explosion du tableau périodique ;</li>
 *   <li><b>Raccourcis clavier</b> : actifs ou coupés, avec leur liste ;</li>
 *   <li><b>Touche de détail</b> : laquelle, et s'il faut la tenir ou si elle bascule ;</li>
 *   <li><b>Pause</b> : le temps ne passe plus ;</li>
 *   <li><b>Recommencer</b> : efface toute la partie, en deux clics.</li>
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

    private final Settings settings;
    private final Runnable onReset;
    private final Runnable onAppearance;
    private final Map<Settings.Theme, Button> themeButtons = new EnumMap<>(Settings.Theme.class);
    private final Map<Settings.Scale, Button> scaleButtons = new EnumMap<>(Settings.Scale.class);
    private final Button notificationsButton = new Button();
    private final Button goalButton = new Button();
    private final Map<Settings.Notation, Button> notationButtons = new EnumMap<>(Settings.Notation.class);
    private final Label notationExample = new Label();
    private final Button effectsButton = new Button();
    private final Button confirmButton = new Button();
    private final Button shortcutsButton = new Button();
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
     */
    SettingsPage(Settings settings, Runnable onReset, Runnable onAppearance) {
        super(10);
        this.settings = settings;
        this.onReset = onReset;
        this.onAppearance = onAppearance;
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

        block("Effets visuels",
                "L'éclair blanc de l'explosion, le fond animé derrière les générateurs et l'onglet qui bat à chaque "
                        + "nouvel atome. Coupés, l'explosion a lieu sans éclair : à préférer si les flashs vous gênent.");
        effectsButton.setOnAction(event -> {
            settings.setEffects(!settings.effects());
            refresh();
        });
        getChildren().add(effectsButton);

        block("Confirmation avant l'explosion",
                "L'explosion remet toute la partie à zéro. Avec la confirmation, il faut cliquer deux fois sur son bouton.");
        confirmButton.setOnAction(event -> {
            settings.setConfirmExplosion(!settings.confirmExplosion());
            refresh();
        });
        getChildren().add(confirmButton);

        block("Raccourcis clavier",
                "V : vitesse de création · C : couplage · G : nouveau générateur · F : fusion · M : tout acheter avec "
                        + "les particules · P : pause. Les trois premiers achètent la quantité choisie dans l'onglet "
                        + "Particules (×1, ×10 ou max). Ils marchent depuis n'importe quel onglet.");
        shortcutsButton.setOnAction(event -> {
            settings.setShortcuts(!settings.shortcuts());
            refresh();
        });
        getChildren().add(shortcutsButton);

        block("Touche de détail", "Les cartes et les pages s'en tiennent à l'essentiel. Cette touche fait apparaître "
                + "leurs explications complètes : ce que fait chaque amélioration, d'où vient chaque nombre. "
                + "On peut la tenir enfoncée, ou la faire basculer d'un appui.");
        detailKeyButton.setOnAction(event -> {
            capturing = !capturing;
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

        block("Recommencer de zéro",
                "Efface toute la partie : particules, atomes, tableau périodique, matière noire, arbre et statistiques. "
                        + "Les réglages sont gardés. C'est définitif.");
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

        Label footer = new Label("Les réglages sont gardés d'un lancement à l'autre. La partie, elle, n'est pas "
                + "sauvegardée : elle repart du début à chaque lancement du jeu.");
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
     * La touche pressée pendant que la page en attendait une. Échap annule ; une touche déjà prise
     * par un raccourci est refusée, et la page continue d'attendre.
     *
     * @param taken vrai si la touche sert déjà de raccourci
     */
    void capture(KeyCode key, boolean taken) {
        if (!capturing) return;
        if (key == KeyCode.ESCAPE) {
            capturing = false;
            captureProblem = "";
        } else if (taken) {
            captureProblem = "« " + Detail.keyName(key) + " » est déjà un raccourci : choisissez une autre touche.";
        } else {
            settings.setDetailKey(key);
            Detail.set(false);
            capturing = false;
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
        if (resetArmed <= 0) return;
        resetArmed = Math.max(0, resetArmed - elapsed);
        refresh();
    }

    /** L'onglet n'est plus affiché : une remise à zéro ou une touche demandées mais pas confirmées sont oubliées. */
    void release() {
        resetArmed = 0;
        capturing = false;
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
        effectsButton.setText(settings.effects() ? "Effets visuels : affichés" : "Effets visuels : coupés");
        effectsButton.setStyle(settings.effects() ? ON_STYLE : OFF_STYLE);
        confirmButton.setText(settings.confirmExplosion() ? "Confirmation : demandée" : "Confirmation : non demandée");
        confirmButton.setStyle(settings.confirmExplosion() ? ON_STYLE : OFF_STYLE);
        shortcutsButton.setText(settings.shortcuts() ? "Raccourcis clavier : actifs" : "Raccourcis clavier : coupés");
        shortcutsButton.setStyle(settings.shortcuts() ? ON_STYLE : OFF_STYLE);
        String key = Detail.keyName(settings.detailKey());
        detailKeyButton.setText(capturing ? "Appuyez sur une touche… (Échap annule)" : "Touche : " + key + " (changer)");
        detailKeyButton.setStyle(capturing ? ON_STYLE : OFF_STYLE);
        detailModeButton.setText(settings.detailToggle() ? "Un appui bascule" : "À tenir enfoncée");
        detailModeButton.setStyle(OFF_STYLE);
        detailKeyNote.setText(captureProblem);
        detailKeyNote.setVisible(!captureProblem.isEmpty());
        detailKeyNote.setManaged(!captureProblem.isEmpty());
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
    }
}

package idle.ui.pages;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import idle.ui.Settings;
import idle.ui.Shortcuts;
import idle.ui.common.Detail;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.SubTabs;
import java.util.Map;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

/**
 * Contenu de l'onglet « Réglages » : un bloc par réglage, rangés depuis la version 0.063 en six sous-onglets ({@link
 * Section}).
 * <ul>
 * <li><b>Affichage</b> : la notation des grands nombres (dont trois à débloquer), le thème (dont cinq à débloquer), la
 * taille de l'interface et de la fenêtre, la disposition des onglets ;</li>
 * <li><b>Notifications et signes</b> : les notifications par catégorie, les morceaux de la ligne d'objectif (chrono
 * compris), les signes d'état des cartes et les pastilles des onglets par sorte ;</li>
 * <li><b>Effets et sons</b> : les effets visuels, ensemble ou un par un, les images par seconde, les sons ;</li>
 * <li><b>Jeu</b> : la pause, les confirmations, le temps hors ligne, les records ;</li>
 * <li><b>Clavier</b> : les raccourcis et leurs touches, la touche de détail, les touches des cercles de la boucle
 * ;</li>
 * <li><b>Partie</b> : la sauvegarde et son rythme, l'export et l'import, recommencer de zéro, le rapport de bug, la
 * version du jeu et ses notes.</li>
 * </ul> Un thème ou une notation à débloquer ({@link idle.ui.Cosmetic}) montre, tant qu'il est fermé, un cadenas et un
 * indice ; une fois ouvert, ce qu'il fallait faire. Chaque réglage s'applique tout de suite. Les règles du jeu ne
 * changent pas : les réglages sont dans {@link Settings}, pas dans core.
 *
 * <p>Comme partout dans le jeu, les explications ne s'affichent qu'en mode détails ({@link Detail}) : le reste du
 * temps, chaque réglage tient en un titre et un bouton.
 */
public final class SettingsPage extends SettingsPageBlocks {
    public SettingsPage(Settings settings, Runnable onReset, Runnable onAppearance, Saves saves) {
        super(settings, onReset, onAppearance, saves);
    }

    /** Les blocs qui suivent vont dans ce sous-onglet. */
    void use(Section section) { target = sections.get(section); }

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public FlowPane subTabs() { return subTabs; }

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
    public Section selected() { return selected; }

    /** Titre et explication d'un réglage ; l'explication n'apparaît qu'en mode détails. */
    void block(String name, String explanation) {
        Label header = new Label(name);
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 14 0 0 0; -fx-text-fill: " + SETTINGS_COLOR + ";");
        target.getChildren().add(header);
        Label text = new Label(explanation);
        note(text);
        explanations.add(text);
    }

    void changeVolume(int step) {
        settings.setVolume(settings.volume() + step);
        soundTest.run();
        refresh();
    }

    /** Le volume de la musique : elle joue déjà, le changement s'entend sans essai. */
    void changeMusicVolume(int step) {
        settings.setMusicVolume(settings.musicVolume() + step);
        refresh();
    }

    /** Branche le son d'essai, joué quand le joueur règle le volume ou rallume les sons. */
    public void setSoundTest(Runnable test) {
        soundTest = test == null ? () -> { } : test;
    }

    /** Vrai tant que la page attend la prochaine touche du clavier pour en faire la touche de détail. */
    public boolean isCapturing() { return capturing; }

    /**
     * La touche pressée pendant que la page en attendait une, pour la touche de détail, pour un raccourci ou pour un
     * cercle de la boucle. Échap annule ; une touche déjà prise est refusée, et la page continue d'attendre.
     */
    public void capture(KeyCode key) {
        if (!capturing) return;
        Shortcuts.Action holder = settings.actionOf(key);
        int ring = settings.loopRingOf(key);
        if (key == KeyCode.ESCAPE) {
            capturing = false;
            capturingRing = -1;
            captureProblem = "";
        } else if (capturingRing >= 0) {
            captureLoopKey(key, ring);
        } else if (capturingAction == null) {
            // La touche de détail : n'importe laquelle, sauf celle d'un raccourci ou d'un cercle de la boucle.
            if (holder != null) {
                captureProblem = "« " + Shortcuts.name(key) + " » sert déjà à : " + holder.label().toLowerCase() + ". Choisissez une autre touche.";
            } else if (ring >= 0) {
                captureProblem = "« " + Shortcuts.name(key) + " » est la touche du cercle " + (ring + 1) + " de la boucle. Choisissez une autre touche.";
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

    /**
     * La nouvelle touche d'un cercle de la boucle : refusée si c'est la touche de détail, une touche qui ne peut rien
     * porter, ou celle d'un autre cercle ; celle d'un raccourci est acceptée, les deux ne servent pas au même moment.
     */
    void captureLoopKey(KeyCode key, int holder) {
        if (key == settings.detailKey()) {
            captureProblem = "« " + Shortcuts.name(key) + " » est la touche de détail. Choisissez une autre touche.";
        } else if (!Shortcuts.bindable(key)) {
            captureProblem = "« " + Shortcuts.name(key) + " » ne peut pas porter un cercle. Choisissez une autre touche.";
        } else if (holder >= 0 && holder != capturingRing) {
            captureProblem = "« " + Shortcuts.name(key) + " » est déjà la touche du cercle " + (holder + 1) + ". Choisissez une autre touche.";
        } else {
            settings.setLoopKey(capturingRing, key);
            capturing = false;
            capturingRing = -1;
            captureProblem = "";
        }
    }

    void note(Label label) {
        Labels.note(label, 560);
        (target == null ? getChildren() : target.getChildren()).add(label);
    }

    /**
     * Une rangée de catégories : un bouton par catégorie, qui la montre ou la coupe, puis recopie la page.
     * Comme les effets visuels : « ✓ » ou « ✗ » devant son nom.
     */
    <E> FlowPane categoryRow(E[] values, Function<E, String> label, Consumer<E> flip, Map<E, Button> buttons) {
        FlowPane row = new FlowPane(8, 8);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(720);
        choiceRow(row, values, label, flip, buttons, true);
        return row;
    }

    /**
     * Une rangée de choix : un bouton par valeur, qui l'applique puis recopie la page. {@code focusless} retire les
     * boutons du parcours au clavier. Leurs couleurs (celui qui est choisi est éclairé) se posent dans {@link #refresh()}.
     */
    <E> void choiceRow(Pane row, E[] values, Function<E, String> label,
            Consumer<E> choose, Map<E, Button> buttons, boolean focusless) {
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
        capturingRing = -1;
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
        soundKindButtons.forEach((kind, button) -> {
            boolean on = settings.soundKind(kind);
            button.setText((on ? "✓ " : "✗ ") + kind.label());
            button.setStyle(on && settings.sound() ? ON_STYLE : OFF_STYLE);
            button.setDisable(!settings.sound());
        });
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
        // Les catégories : éclairées si elles s'affichent, grisées sous un grand interrupteur coupé.
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
        toggle(musicButton, settings.music(), "Musique : activée", "Musique : coupée");
        toggle(automationSoundsButton, settings.automationSounds(), "Sons des automatismes : activés", "Sons des automatismes : coupés");
        musicVolumeLabel.setText("Musique : " + settings.musicVolume() + " %");
        musicQuieterButton.setDisable(!settings.music() || settings.musicVolume() <= 0);
        musicLouderButton.setDisable(!settings.music() || settings.musicVolume() >= 100);
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
        for (int ring = 0; ring < loopKeyButtons.size(); ring++) {
            KeyCode bound = settings.loopKey(ring);
            toggle(loopKeyButtons.get(ring), capturing && capturingRing == ring, "Appuyez sur une touche…",
                    bound == null ? "(aucune)" : Shortcuts.name(bound));
        }
        resetLoopKeysButton.setVisible(settings.hasCustomLoopKeys());
        resetLoopKeysButton.setManaged(settings.hasCustomLoopKeys());
        show(loopKeyNote, capturingRing >= 0 ? captureProblem : "");
        toggle(shortcutsButton, settings.shortcuts(), "Raccourcis clavier : actifs", "Raccourcis clavier : coupés");
        String key = Detail.keyName(settings.detailKey());
        boolean detailWaiting = capturing && capturingAction == null && capturingRing < 0;
        toggle(detailKeyButton, detailWaiting, "Appuyez sur une touche… (Échap annule)", "Touche : " + key + " (changer)");
        detailModeButton.setText(settings.detailToggle() ? "Un appui bascule" : "À tenir enfoncée");
        detailModeButton.setStyle(OFF_STYLE);
        show(detailKeyNote, capturingAction == null && capturingRing < 0 ? captureProblem : "");
        // Les explications, comme partout : seulement en mode détails.
        for (Label explanation : explanations) {
            explanation.setVisible(Detail.shown());
            explanation.setManaged(Detail.shown());
        }
        toggle(pauseButton, settings.paused(), "En pause : cliquer pour reprendre", "Mettre en pause");
        resetButton.setText(resetArmed > 0 ? "Cliquer encore pour tout effacer (" + (int) Math.ceil(resetArmed) + " s)"
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
    void armedLook(Button button, String key, String idle, String armed, boolean usable) {
        boolean on = key.equals(checkpointArmed) && checkpointArmedFor > 0;
        button.setText(on ? armed + " (" + (int) Math.ceil(checkpointArmedFor) + " s)" : idle);
        button.setStyle(on ? DANGER_ARMED_STYLE : OFF_STYLE);
        button.setDisable(!usable);
    }

    /**
     * Ce que font les boutons des formats de fenêtre : redimensionner, dire si un format
     * tient sur l'écran, et lequel est en cours.
     */
    public void setWindowSizes(Consumer<Settings.WindowSize> apply,
            Predicate<Settings.WindowSize> fits, Supplier<Settings.WindowSize> current) {
        windowSize = apply;
        sizeFits = fits;
        sizeNow = current;
        refresh();
    }
}

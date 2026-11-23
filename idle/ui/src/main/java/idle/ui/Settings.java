package idle.ui;

import idle.ui.common.*;
import idle.core.*;
import idle.ui.bigbang.BigBangPage;
import idle.ui.collider.EnergyView;
import idle.ui.darkmatter.DarkMatterView;
import idle.ui.particles.ParticleView;
import java.util.*;
import java.util.prefs.Preferences;
import javafx.scene.input.KeyCode;

/**
 * Les réglages du joueur : ils ne changent rien aux règles, seulement à la façon dont le jeu s'affiche et se pilote.
 * Ils sont gardés d'un lancement à l'autre dans les préférences de l'utilisateur ({@link Preferences}), avec la place
 * de la fenêtre et l'onglet ouvert ; la partie elle-même est sauvegardée à part, dans un fichier ({@link SaveStore}).
 *
 * <p>La pause n'est pas gardée : le jeu repart toujours en marche.
 */
public final class Settings extends SettingsBase {
    Settings(Preferences store) {
        super(store);
    }

    public void setEffect(Effect effect, boolean on) {
        if (on) effectsOff.remove(effect);
        else effectsOff.add(effect);
        save(EFFECTS_OFF, joined(effectsOff));
        applyEffects();
    }

    /** Les dessins lisent leurs effets dans un drapeau commun : le recopier après chaque changement. */
    void applyEffects() {
        ParticleView.setSpeedLook(effect(Effect.PARTICLE_SPEED));
        EnergyView.setSparks(effect(Effect.COLLIDER_SPARKS));
        DarkMatterView.setFall(effect(Effect.DARK_FALL));
        ParticleView.setCreationLook(effect(Effect.PARTICLE_CREATION));
        DarkMatterView.setMotion(effect(Effect.DARK_MATTER_MOTION));
        BigBangPage.setMotion(effect(Effect.BIG_BANG_MOTION));
        EnergyView.setMotion(effect(Effect.COLLIDER_MOTION));
    }

    public SaveDelay saveDelay() { return saveDelay; }

    public void setSaveDelay(SaveDelay delay) {
        this.saveDelay = delay;
        save(SAVE_DELAY, delay.name());
    }

    /** Vrai si le temps passé jeu fermé est rejoué au retour ; faux pour repartir d'où on était. */
    public boolean offline() { return offline; }

    public void setOffline(boolean offline) {
        this.offline = offline;
        save(OFFLINE, String.valueOf(offline));
    }

    /** Vrai si ce thème ou cette notation à débloquer l'est déjà. */
    public boolean hasCosmetic(Cosmetic cosmetic) { return cosmetics.contains(cosmetic); }

    /** Vrai si ce thème peut être choisi : offert, ou débloqué. */
    public boolean isOpen(Theme theme) { return theme.unlock() == null || cosmetics.contains(theme.unlock()); }

    public boolean isOpen(Notation notation) {
        return notation.unlock() == null || cosmetics.contains(notation.unlock());
    }

    /**
     * Débloque un thème ou une notation.
     *
     * @return vrai s'il ne l'était pas encore
     */
    public boolean unlock(Cosmetic cosmetic) {
        if (!cosmetics.add(cosmetic)) return false;
        save(COSMETICS, joined(cosmetics));
        return true;
    }

    /** Referme un cosmétique (outil de test) : le thème ou la notation qu'il ouvrait revient à celui d'origine s'il était choisi. */
    public void lock(Cosmetic cosmetic) {
        if (!cosmetics.remove(cosmetic)) return;
        save(COSMETICS, joined(cosmetics));
        if (theme.unlock() == cosmetic) setTheme(Theme.DARK);
        if (notation.unlock() == cosmetic) setNotation(Notation.SCIENTIFIC);
    }

    /** Referme tous les cosmétiques (outil de test) : le thème et la notation reviennent à ceux d'origine s'il le faut. */
    public void lockCosmetics() {
        cosmetics.clear();
        save(COSMETICS, "");
        if (theme.unlock() != null) setTheme(Theme.DARK);
        if (notation.unlock() != null) setNotation(Notation.SCIENTIFIC);
    }

    /**
     * Ne garde plus rien de ce qui change ensuite : le profil test UI (version 0.252) change de thème et de notation à
     * chaque étape, sans toucher aux réglages du joueur.
     */
    void forget() { store = null; }

    public Notation notation() { return notation; }

    public void setNotation(Notation notation) {
        this.notation = notation;
        Format.setNotation(notation);
        save(NOTATION, notation.name());
    }

    /** Vrai si les effets visuels sont affichés : éclair de l'explosion, fond animé, onglet qui bat. */
    public boolean effects() { return effects; }

    public void setEffects(boolean effects) {
        this.effects = effects;
        save(EFFECTS, String.valueOf(effects));
        applyEffects();
    }

    /** Vrai si l'explosion demande un second clic de confirmation. */
    public boolean confirmExplosion() { return confirmExplosion; }

    public void setConfirmExplosion(boolean confirmExplosion) {
        this.confirmExplosion = confirmExplosion;
        save(CONFIRM_EXPLOSION, String.valueOf(confirmExplosion));
    }

    /** Vrai si le Big Bang demande un second clic de confirmation. */
    public boolean confirmBigBang() { return confirmBigBang; }

    public void setConfirmBigBang(boolean confirm) {
        confirmBigBang = confirm;
        Confirm.set(confirmBigBang, confirmChallenge);
        save(CONFIRM_BIG_BANG, String.valueOf(confirm));
    }

    /** Vrai si commencer ou abandonner un défi demande un second clic de confirmation. */
    public boolean confirmChallenge() { return confirmChallenge; }

    public void setConfirmChallenge(boolean confirm) {
        confirmChallenge = confirm;
        Confirm.set(confirmBigBang, confirmChallenge);
        save(CONFIRM_CHALLENGE, String.valueOf(confirm));
    }

    /** La touche d'un raccourci, ou {@code null} s'il n'en a pas (la sienne a été donnée à un autre). */
    public KeyCode key(Shortcuts.Action action) { return keys.get(action); }

    /** Le raccourci que porte cette touche, ou {@code null}. */
    public Shortcuts.Action actionOf(KeyCode key) {
        for (Map.Entry<Shortcuts.Action, KeyCode> entry : keys.entrySet()) {
            if (entry.getValue() == key) return entry.getKey();
        }
        return null;
    }

    /**
     * Donne une touche à un raccourci.
     *
     * @return faux, sans rien changer, si la touche ne peut pas porter de raccourci, si c'est la
     *         touche de détail, ou si un autre raccourci l'a déjà
     */
    public boolean setKey(Shortcuts.Action action, KeyCode key) {
        if (!Shortcuts.bindable(key) || key == detailKey) return false;
        Shortcuts.Action holder = actionOf(key);
        if (holder != null && holder != action) return false;
        keys.put(action, key);
        save(KEY_PREFIX + action.name(), key.name());
        return true;
    }

    /** Vrai si au moins un raccourci n'a plus sa touche d'origine. */
    public boolean hasCustomKeys() {
        for (Shortcuts.Action action : Shortcuts.Action.values()) {
            if (keys.get(action) != action.defaultKey()) return true;
        }
        return false;
    }

    /** Rend à chaque raccourci sa touche d'origine, sauf à celui dont la touche sert de touche de détail. */
    public void resetKeys() {
        keys.clear();
        for (Shortcuts.Action action : Shortcuts.Action.values()) {
            if (action.defaultKey() != detailKey) keys.put(action, action.defaultKey());
            if (store == null) continue;
            try {
                store.remove(KEY_PREFIX + action.name());
            } catch (RuntimeException unwritable) {
                // tant pis : la touche d'origine vaut au moins jusqu'à la fermeture du jeu
            }
        }
    }

    /** Quantité achetée à chaque clic sur une amélioration payée en particules. */
    public BuyAmount buyAmount() { return buyAmount; }

    public void setBuyAmount(BuyAmount buyAmount) {
        this.buyAmount = buyAmount;
        save(BUY_AMOUNT, buyAmount.name());
    }

    /** Vrai si les raccourcis clavier sont actifs. */
    public boolean shortcuts() { return shortcuts; }

    public void setShortcuts(boolean shortcuts) {
        this.shortcuts = shortcuts;
        save(SHORTCUTS, String.valueOf(shortcuts));
    }

    public Theme theme() { return theme; }

    public void setTheme(Theme theme) {
        this.theme = theme;
        save(THEME, theme.name());
    }

    public Scale scale() { return scale; }

    public void setScale(Scale scale) {
        this.scale = scale;
        save(SCALE, scale.name());
    }

    /** Vrai si les notifications s'affichent : nouvel élément, succès, déblocage… */
    public boolean notifications() { return notifications; }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
        save(NOTIFICATIONS, String.valueOf(notifications));
    }

    /** Vrai si les notifications de cette catégorie s'affichent : les notifications en marche, et elle pas coupée. */
    public boolean notice(Notice kind) { return notifications && !noticesOff.contains(kind); }

    /** Vrai si cette catégorie est en marche dans son propre réglage, quel que soit le grand interrupteur. */
    public boolean noticeChosen(Notice kind) { return !noticesOff.contains(kind); }

    /** Vrai si cette catégorie de sons est gardée : toutes le sont au départ. */
    public boolean soundKind(SoundKind kind) { return !soundsOff.contains(kind); }

    public void setSoundKind(SoundKind kind, boolean on) {
        if (on) soundsOff.remove(kind);
        else soundsOff.add(kind);
        save(SOUNDS_OFF, joined(soundsOff));
    }

    public void setNotice(Notice kind, boolean on) {
        if (on) noticesOff.remove(kind);
        else noticesOff.add(kind);
        save(NOTICES_OFF, joined(noticesOff));
    }

    /**
     * Vrai si ce morceau de la ligne d'objectif s'affiche : l'objectif en marche, et lui pas caché. Le
     * chrono garde son réglage d'avant.
     */
    public boolean goalPart(Goals.Part part) { return showGoal && goalPartChosen(part); }

    public boolean goalPartChosen(Goals.Part part) {
        return part == Goals.Part.CHRONO ? chrono : !goalPartsOff.contains(part);
    }

    public void setGoalPart(Goals.Part part, boolean on) {
        if (part == Goals.Part.CHRONO) {
            setChrono(on);
            return;
        }
        if (on) goalPartsOff.remove(part);
        else goalPartsOff.add(part);
        save(GOAL_PARTS_OFF, joined(goalPartsOff));
    }

    /** Vrai si cette sorte de pastille s'allume sur les onglets. */
    public boolean dot(Readiness.Kind kind) { return !dotsOff.contains(kind); }

    public void setDot(Readiness.Kind kind, boolean on) {
        if (on) dotsOff.remove(kind);
        else dotsOff.add(kind);
        Readiness.setHidden(dotsOff);
        save(DOTS_OFF, joined(dotsOff));
    }

    /** Vrai si l'objectif du moment est rappelé sous les onglets. */
    public boolean showGoal() { return showGoal; }

    public void setShowGoal(boolean showGoal) {
        this.showGoal = showGoal;
        save(SHOW_GOAL, String.valueOf(showGoal));
    }

    /** L'ordre des onglets : leurs noms, séparés par des virgules ; vide pour l'ordre d'origine. */
    public String tabOrder() { return tabOrder; }

    public void setTabOrder(String order) {
        this.tabOrder = order == null ? "" : order;
        save(TAB_ORDER, this.tabOrder);
    }

    /** Vrai si les onglets sont rangés en colonne, à gauche. */
    public boolean tabsLeft() { return tabsLeft; }

    public void setTabsLeft(boolean left) {
        this.tabsLeft = left;
        save(TABS_LEFT, String.valueOf(left));
    }

    public boolean sound() { return sound; }

    public void setSound(boolean sound) {
        this.sound = sound;
        save(SOUND, String.valueOf(sound));
    }

    /** Volume des sons, de 0 à 100. */
    public int volume() { return volume; }

    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
        save(VOLUME, String.valueOf(this.volume));
    }

    /** Vrai si la musique de fond joue. */
    public boolean music() { return music; }

    /** Vrai si ce que font les automatismes (une fusion, une explosion automatiques) sonne ; coupé par défaut. */
    public boolean automationSounds() { return automationSounds; }

    public void setAutomationSounds(boolean on) {
        automationSounds = on;
        save(AUTOMATION_SOUNDS, String.valueOf(on));
    }

    public void setMusic(boolean music) {
        this.music = music;
        save(MUSIC, String.valueOf(music));
    }

    /** Volume de la musique, de 0 à 100. */
    public int musicVolume() { return musicVolume; }

    public void setMusicVolume(int volume) {
        this.musicVolume = Math.max(0, Math.min(100, volume));
        save(MUSIC_VOLUME, String.valueOf(this.musicVolume));
    }

    public FrameRate frameRate() { return frameRate; }

    public void setFrameRate(FrameRate frameRate) {
        this.frameRate = frameRate;
        save(FRAME_RATE, frameRate.name());
    }

    /** Vrai si la fenêtre ralentit quand elle n'est pas au premier plan : quatre images par seconde, une si elle est réduite. */
    public boolean saveEnergy() { return saveEnergy; }

    public void setSaveEnergy(boolean saveEnergy) {
        this.saveEnergy = saveEnergy;
        save(SAVE_ENERGY, String.valueOf(saveEnergy));
    }

    /** Vrai si la ligne d'objectif dit l'écart au record sur la prochaine étape, quand il y a un record. */
    public boolean chrono() { return chrono; }

    public void setChrono(boolean chrono) {
        this.chrono = chrono;
        save(CHRONO, String.valueOf(chrono));
    }

    /**
     * Vrai si les anomalies du ciel (version 0.127 : vent de particules, fenêtre d'exploration) sont montrées et
     * annoncées. Coupées, elles passent sans se montrer : elles sont facultatives, rien n'en dépend.
     */
    public boolean skyEvents() { return skyEvents; }

    public void setSkyEvents(boolean shown) {
        this.skyEvents = shown;
        save(SKY_EVENTS, String.valueOf(shown));
    }

    /**
     * Vrai si la caméra du volume est libre : dans le cube, le regard suit la souris sans qu'on tienne
     * le clic gauche, et une croix au centre vise. Faux : on regarde autour de soi clic gauche tenu.
     */
    public boolean freeCamera() { return freeCamera; }

    public void setFreeCamera(boolean free) {
        this.freeCamera = free;
        save(FREE_CAMERA, String.valueOf(free));
    }

    /** Vrai si les cartes écrivent le signe de leur état devant leur nom, pour le reconnaître sans la couleur. */
    public boolean stateSigns() { return stateSigns; }

    public void setStateSigns(boolean stateSigns) {
        this.stateSigns = stateSigns;
        Card.setSigns(stateSigns);
        save(STATE_SIGNS, String.valueOf(stateSigns));
    }

    /** La touche qui fait apparaître les explications complètes des cartes et des pages. */
    public KeyCode detailKey() { return detailKey; }

    public void setDetailKey(KeyCode detailKey) {
        this.detailKey = detailKey;
        save(DETAIL_KEY, detailKey.name());
    }

    /**
     * Vrai si la touche de détail bascule (un appui affiche les détails, un autre les cache) ;
     * faux s'il faut la tenir enfoncée.
     */
    public boolean detailToggle() { return detailToggle; }

    public void setDetailToggle(boolean detailToggle) {
        this.detailToggle = detailToggle;
        save(DETAIL_TOGGLE, String.valueOf(detailToggle));
    }

    /** Vrai si le jeu est en pause : le temps ne passe plus, rien ne se crée. */
    public boolean paused() { return paused; }

    public void setPaused(boolean paused) { this.paused = paused; }

    /**
     * La place de la fenêtre à la dernière fermeture : {@code x, y, largeur, hauteur}, puis 1 si elle était agrandie au
     * maximum, 0 sinon. {@code null} tant que le jeu n'a jamais été fermé, ou si ce qui est gardé est illisible.
     */
    double[] window() {
        if (store == null) return null;
        try {
            String[] parts = store.get(WINDOW, "").split(",");
            if (parts.length != 5) return null;
            double[] place = new double[5];
            for (int i = 0; i < 5; i++) {
                place[i] = Double.parseDouble(parts[i]);
                if (Double.isNaN(place[i]) || Double.isInfinite(place[i])) return null;
            }
            return place[2] >= 100 && place[3] >= 100 ? place : null;
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    void setWindow(double x, double y, double width, double height, boolean maximized) {
        save(WINDOW, x + "," + y + "," + width + "," + height + "," + (maximized ? 1 : 0));
    }

    /** Le nom de l'onglet affiché à la dernière fermeture, ou un texte vide. */
    String lastTab() {
        if (store == null) return "";
        try {
            return store.get(LAST_TAB, "");
        } catch (RuntimeException unreadable) {
            return "";
        }
    }

    void setLastTab(String name) { save(LAST_TAB, name); }

    void save(String key, String value) {
        if (store == null) return;
        try {
            store.put(key, value);
        } catch (RuntimeException unwritable) {
            // tant pis : le réglage vaut au moins jusqu'à la fermeture du jeu
        }
    }
}

package idle.ui;

import idle.ui.common.Card;
import idle.ui.common.Format;
import java.util.prefs.Preferences;
import javafx.scene.input.KeyCode;

/**
 * Les réglages du joueur : ils ne changent rien aux règles, seulement à la façon dont le jeu
 * s'affiche et se pilote. Ils sont gardés d'un lancement à l'autre dans les préférences de
 * l'utilisateur ({@link Preferences}), avec la place de la fenêtre et l'onglet ouvert ; la partie
 * elle-même est sauvegardée à part, dans un fichier ({@link SaveStore}).
 *
 * <p>La pause n'est pas gardée : le jeu repart toujours en marche.
 */
public final class Settings {

    /** Façon d'écrire les grands nombres. */
    public enum Notation {
        /** 1.23e7 : une mantisse entre 1 et 10, et la puissance de dix. */
        SCIENTIFIC("Scientifique"),
        /** 12.3e6 : la puissance de dix avance de trois en trois, comme les milliers. */
        ENGINEERING("Ingénieur"),
        /** 12.3 M : une abréviation par millier (K, M, B, T, Qa…), puis scientifique au-delà de 1e36. */
        LETTERS("Lettres");

        private final String label;

        Notation(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Combien de niveaux achète un clic sur une amélioration payée en particules. */
    public enum BuyAmount {
        ONE("×1", 1),
        TEN("×10", 10),
        /** Tout ce que le joueur peut payer. */
        MAX("max", Integer.MAX_VALUE);

        private final String label;
        private final int count;

        BuyAmount(String label, int count) {
            this.label = label;
            this.count = count;
        }

        public String label() {
            return label;
        }

        /** Nombre de niveaux demandés à chaque clic ; l'achat s'arrête à ce que le joueur peut payer. */
        public int count() {
            return count;
        }
    }

    /** Apparence générale de la fenêtre. */
    public enum Theme {
        /** Fond sombre : celui pour lequel le jeu est dessiné. */
        DARK("Sombre"),
        /** Fond clair : les couleurs du thème sombre, inversées. */
        LIGHT("Clair");

        private final String label;

        Theme(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Nombre d'images par seconde au plus : moins d'images, moins de calcul et moins de batterie. */
    public enum FrameRate {
        HIGH("60", 60),
        MEDIUM("30", 30),
        LOW("15", 15);

        private final String label;
        private final int perSecond;

        FrameRate(String label, int perSecond) {
            this.label = label;
            this.perSecond = perSecond;
        }

        public String label() {
            return label;
        }

        int perSecond() {
            return perSecond;
        }
    }

    /** Taille de l'interface : tout grandit ou rétrécit ensemble, textes, boutons et dessins. */
    public enum Scale {
        SMALL("Petite", 0.85),
        NORMAL("Normale", 1.0),
        LARGE("Grande", 1.2);

        private final String label;
        private final double factor;

        Scale(String label, double factor) {
            this.label = label;
            this.factor = factor;
        }

        public String label() {
            return label;
        }

        /** Facteur d'agrandissement : 1 pour la taille normale. */
        double factor() {
            return factor;
        }
    }

    private static final String NOTATION = "notation";
    private static final String THEME = "theme";
    private static final String SCALE = "scale";
    private static final String NOTIFICATIONS = "notifications";
    private static final String SHOW_GOAL = "showGoal";
    private static final String STATE_SIGNS = "stateSigns";
    private static final String CHRONO = "chrono";
    private static final String SOUND = "sound";
    private static final String TABS_LEFT = "tabsLeft";
    private static final String VOLUME = "volume";
    private static final String FRAME_RATE = "frameRate";
    private static final String SAVE_ENERGY = "saveEnergy";
    /** Volume de départ, sur 100 : assez bas pour qu'un premier son ne surprenne pas. */
    static final int DEFAULT_VOLUME = 40;
    private static final String BUY_AMOUNT = "buyAmount";
    private static final String SHORTCUTS = "shortcuts";
    private static final String EFFECTS = "effects";
    private static final String CONFIRM_EXPLOSION = "confirmExplosion";
    private static final String DETAIL_KEY = "detailKey";
    private static final String DETAIL_TOGGLE = "detailToggle";
    private static final String CONFIRM_BIG_BANG = "confirmBigBang";
    private static final String CONFIRM_CHALLENGE = "confirmChallenge";
    /** Préfixe des touches choisies par le joueur : une préférence par raccourci. */
    private static final String KEY_PREFIX = "key.";
    private static final String WINDOW = "window";
    private static final String LAST_TAB = "lastTab";
    /**
     * La touche de détail d'origine. Pas Maj, pourtant habituelle pour cet usage : sous Windows,
     * cinq appuis de suite ouvrent la fenêtre des « touches rémanentes ».
     */
    static final KeyCode DEFAULT_DETAIL_KEY = KeyCode.CONTROL;

    /** {@code null} si les préférences ne sont pas accessibles : les réglages ne durent alors que le temps du lancement. */
    private final Preferences store;
    private Notation notation = Notation.SCIENTIFIC;
    private boolean effects = true;
    private boolean confirmExplosion = true;
    private boolean paused = false;
    private BuyAmount buyAmount = BuyAmount.ONE;
    private boolean shortcuts = true;
    private Theme theme = Theme.DARK;
    private Scale scale = Scale.NORMAL;
    private boolean notifications = true;
    private boolean showGoal = true;
    private boolean stateSigns = true;
    private boolean chrono = true;
    private boolean sound = true;
    /** Les onglets à gauche, et les sous-onglets en haut à leur place (version 0.57) ; sinon, la disposition d'avant. */
    private boolean tabsLeft = false;
    private int volume = DEFAULT_VOLUME;
    private FrameRate frameRate = FrameRate.HIGH;
    private boolean saveEnergy = true;
    private KeyCode detailKey = DEFAULT_DETAIL_KEY;
    private boolean detailToggle = false;
    private boolean confirmBigBang = true;
    private boolean confirmChallenge = true;
    private final java.util.Map<Shortcuts.Action, KeyCode> keys = new java.util.EnumMap<>(Shortcuts.Action.class);

    /** Réglages relus dans les préférences de l'utilisateur, ou ceux par défaut si elles sont illisibles. */
    static Settings load() {
        Preferences store;
        try {
            store = Preferences.userNodeForPackage(Settings.class);
        } catch (RuntimeException unavailable) {
            store = null;
        }
        Settings settings = new Settings(store);
        if (store != null) {
            try {
                settings.notation = Notation.valueOf(store.get(NOTATION, Notation.SCIENTIFIC.name()));
            } catch (RuntimeException unknown) {
                settings.notation = Notation.SCIENTIFIC;   // valeur inconnue ou préférences illisibles
            }
            try {
                settings.buyAmount = BuyAmount.valueOf(store.get(BUY_AMOUNT, BuyAmount.ONE.name()));
            } catch (RuntimeException unknown) {
                settings.buyAmount = BuyAmount.ONE;
            }
            try {
                settings.theme = Theme.valueOf(store.get(THEME, Theme.DARK.name()));
            } catch (RuntimeException unknown) {
                settings.theme = Theme.DARK;
            }
            try {
                settings.scale = Scale.valueOf(store.get(SCALE, Scale.NORMAL.name()));
            } catch (RuntimeException unknown) {
                settings.scale = Scale.NORMAL;
            }
            try {
                settings.frameRate = FrameRate.valueOf(store.get(FRAME_RATE, FrameRate.HIGH.name()));
            } catch (RuntimeException unknown) {
                settings.frameRate = FrameRate.HIGH;
            }
            try {
                settings.sound = store.getBoolean(SOUND, true);
                settings.tabsLeft = store.getBoolean(TABS_LEFT, false);
                settings.volume = Math.max(0, Math.min(100, store.getInt(VOLUME, DEFAULT_VOLUME)));
                settings.saveEnergy = store.getBoolean(SAVE_ENERGY, true);
            } catch (RuntimeException unreadable) {
                // on garde les valeurs par défaut
            }
            try {
                settings.detailKey = KeyCode.valueOf(store.get(DETAIL_KEY, DEFAULT_DETAIL_KEY.name()));
            } catch (RuntimeException unknown) {
                settings.detailKey = DEFAULT_DETAIL_KEY;
            }
            try {
                settings.detailToggle = store.getBoolean(DETAIL_TOGGLE, false);
                settings.notifications = store.getBoolean(NOTIFICATIONS, true);
                settings.showGoal = store.getBoolean(SHOW_GOAL, true);
                settings.stateSigns = store.getBoolean(STATE_SIGNS, true);
                settings.chrono = store.getBoolean(CHRONO, true);
                settings.shortcuts = store.getBoolean(SHORTCUTS, true);
                settings.effects = store.getBoolean(EFFECTS, true);
                settings.confirmExplosion = store.getBoolean(CONFIRM_EXPLOSION, true);
                settings.confirmBigBang = store.getBoolean(CONFIRM_BIG_BANG, true);
                settings.confirmChallenge = store.getBoolean(CONFIRM_CHALLENGE, true);
            } catch (RuntimeException unreadable) {
                // on garde les valeurs par défaut
            }
            for (Shortcuts.Action action : Shortcuts.Action.values()) {
                try {
                    KeyCode key = KeyCode.valueOf(store.get(KEY_PREFIX + action.name(), action.defaultKey().name()));
                    // Une touche déjà prise (préférences modifiées à la main) : le raccourci garde celle d'origine, ou aucune.
                    if (Shortcuts.bindable(key) && key != settings.detailKey && !settings.keys.containsValue(key)) settings.keys.put(action, key);
                } catch (RuntimeException unknown) {
                    // touche inconnue : celle d'origine sera reprise ci-dessous si elle est libre
                }
            }
        }
        for (Shortcuts.Action action : Shortcuts.Action.values()) {
            KeyCode key = action.defaultKey();
            if (!settings.keys.containsKey(action) && key != settings.detailKey && !settings.keys.containsValue(key)) settings.keys.put(action, key);
        }
        Confirm.set(settings.confirmBigBang, settings.confirmChallenge);
        Card.setSigns(settings.stateSigns);
        Format.setNotation(settings.notation);
        return settings;
    }

    private Settings(Preferences store) {
        this.store = store;
    }

    public Notation notation() {
        return notation;
    }

    public void setNotation(Notation notation) {
        this.notation = notation;
        Format.setNotation(notation);
        save(NOTATION, notation.name());
    }

    /** Vrai si les effets visuels sont affichés : éclair de l'explosion, fond animé, onglet qui bat. */
    public boolean effects() {
        return effects;
    }

    public void setEffects(boolean effects) {
        this.effects = effects;
        save(EFFECTS, String.valueOf(effects));
    }

    /** Vrai si l'explosion demande un second clic de confirmation. */
    public boolean confirmExplosion() {
        return confirmExplosion;
    }

    public void setConfirmExplosion(boolean confirmExplosion) {
        this.confirmExplosion = confirmExplosion;
        save(CONFIRM_EXPLOSION, String.valueOf(confirmExplosion));
    }

    /** Vrai si le Big Bang demande un second clic de confirmation. */
    public boolean confirmBigBang() {
        return confirmBigBang;
    }

    public void setConfirmBigBang(boolean confirm) {
        confirmBigBang = confirm;
        Confirm.set(confirmBigBang, confirmChallenge);
        save(CONFIRM_BIG_BANG, String.valueOf(confirm));
    }

    /** Vrai si commencer ou abandonner un défi demande un second clic de confirmation. */
    public boolean confirmChallenge() {
        return confirmChallenge;
    }

    public void setConfirmChallenge(boolean confirm) {
        confirmChallenge = confirm;
        Confirm.set(confirmBigBang, confirmChallenge);
        save(CONFIRM_CHALLENGE, String.valueOf(confirm));
    }

    /** La touche d'un raccourci, ou {@code null} s'il n'en a pas (la sienne a été donnée à un autre). */
    public KeyCode key(Shortcuts.Action action) {
        return keys.get(action);
    }

    /** Le raccourci que porte cette touche, ou {@code null}. */
    public Shortcuts.Action actionOf(KeyCode key) {
        for (java.util.Map.Entry<Shortcuts.Action, KeyCode> entry : keys.entrySet()) {
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
    public BuyAmount buyAmount() {
        return buyAmount;
    }

    public void setBuyAmount(BuyAmount buyAmount) {
        this.buyAmount = buyAmount;
        save(BUY_AMOUNT, buyAmount.name());
    }

    /** Vrai si les raccourcis clavier sont actifs. */
    public boolean shortcuts() {
        return shortcuts;
    }

    public void setShortcuts(boolean shortcuts) {
        this.shortcuts = shortcuts;
        save(SHORTCUTS, String.valueOf(shortcuts));
    }

    public Theme theme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
        save(THEME, theme.name());
    }

    public Scale scale() {
        return scale;
    }

    public void setScale(Scale scale) {
        this.scale = scale;
        save(SCALE, scale.name());
    }

    /** Vrai si les notifications s'affichent : nouvel élément, succès, déblocage… */
    public boolean notifications() {
        return notifications;
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
        save(NOTIFICATIONS, String.valueOf(notifications));
    }

    /** Vrai si l'objectif du moment est rappelé sous les onglets. */
    public boolean showGoal() {
        return showGoal;
    }

    public void setShowGoal(boolean showGoal) {
        this.showGoal = showGoal;
        save(SHOW_GOAL, String.valueOf(showGoal));
    }

    /** Vrai si le jeu joue ses sons. */
    public boolean tabsLeft() {
        return tabsLeft;
    }

    public void setTabsLeft(boolean left) {
        this.tabsLeft = left;
        save(TABS_LEFT, String.valueOf(left));
    }

    public boolean sound() {
        return sound;
    }

    public void setSound(boolean sound) {
        this.sound = sound;
        save(SOUND, String.valueOf(sound));
    }

    /** Volume des sons, de 0 à 100. */
    public int volume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
        save(VOLUME, String.valueOf(this.volume));
    }

    public FrameRate frameRate() {
        return frameRate;
    }

    public void setFrameRate(FrameRate frameRate) {
        this.frameRate = frameRate;
        save(FRAME_RATE, frameRate.name());
    }

    /** Vrai si la fenêtre ralentit quand elle n'est pas au premier plan : quatre images par seconde, une si elle est réduite. */
    public boolean saveEnergy() {
        return saveEnergy;
    }

    public void setSaveEnergy(boolean saveEnergy) {
        this.saveEnergy = saveEnergy;
        save(SAVE_ENERGY, String.valueOf(saveEnergy));
    }

    /** Vrai si la ligne d'objectif dit l'écart au record sur la prochaine étape, quand il y a un record. */
    public boolean chrono() {
        return chrono;
    }

    public void setChrono(boolean chrono) {
        this.chrono = chrono;
        save(CHRONO, String.valueOf(chrono));
    }

    /** Vrai si les cartes écrivent le signe de leur état devant leur nom, pour le reconnaître sans la couleur. */
    public boolean stateSigns() {
        return stateSigns;
    }

    public void setStateSigns(boolean stateSigns) {
        this.stateSigns = stateSigns;
        Card.setSigns(stateSigns);
        save(STATE_SIGNS, String.valueOf(stateSigns));
    }

    /** La touche qui fait apparaître les explications complètes des cartes et des pages. */
    public KeyCode detailKey() {
        return detailKey;
    }

    public void setDetailKey(KeyCode detailKey) {
        this.detailKey = detailKey;
        save(DETAIL_KEY, detailKey.name());
    }

    /**
     * Vrai si la touche de détail bascule (un appui affiche les détails, un autre les cache) ;
     * faux s'il faut la tenir enfoncée.
     */
    public boolean detailToggle() {
        return detailToggle;
    }

    public void setDetailToggle(boolean detailToggle) {
        this.detailToggle = detailToggle;
        save(DETAIL_TOGGLE, String.valueOf(detailToggle));
    }

    /** Vrai si le jeu est en pause : le temps ne passe plus, rien ne se crée. */
    public boolean paused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * La place de la fenêtre à la dernière fermeture : {@code x, y, largeur, hauteur}, puis 1 si
     * elle était agrandie au maximum, 0 sinon. {@code null} tant que le jeu n'a jamais été fermé,
     * ou si ce qui est gardé est illisible.
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

    void setLastTab(String name) {
        save(LAST_TAB, name);
    }

    private void save(String key, String value) {
        if (store == null) return;
        try {
            store.put(key, value);
        } catch (RuntimeException unwritable) {
            // tant pis : le réglage vaut au moins jusqu'à la fermeture du jeu
        }
    }
}

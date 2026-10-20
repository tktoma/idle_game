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
        LETTERS("Lettres"),
        /** 12.3 millions : le nom du millier en toutes lettres, jusqu'au décilliard (1e63). */
        WORDS("En mots"),
        /** 1.23×10⁷ : la puissance de dix en exposant, comme au tableau. */
        POWERS("Puissances"),
        /** 12.3 c : une lettre par millier, a, b… z, puis aa, ab… sans fin. À débloquer. */
        ALPHABET("Alphabet", Cosmetic.ALPHABET),
        /** 10^7.09 : le nombre écrit par son logarithme. À débloquer. */
        LOG("Logarithme", Cosmetic.LOG),
        /** 1.23 ×X^VII : la puissance de dix en chiffres romains. À débloquer. */
        ROMAN("Romaine", Cosmetic.ROMAN);

        private final String label;
        private final Cosmetic unlock;

        Notation(String label) {
            this(label, null);
        }

        Notation(String label, Cosmetic unlock) {
            this.label = label;
            this.unlock = unlock;
        }

        public String label() {
            return label;
        }

        /** Ce qui l'ouvre, ou {@code null} si elle est offerte d'emblée. */
        public Cosmetic unlock() {
            return unlock;
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

    /**
     * Apparence générale de la fenêtre. Chaque thème est un filtre posé sur le thème sombre
     * ({@link ThemeFilter}) : l'inverser, tourner ses teintes, le désaturer, le teinter. Les cinq
     * derniers sont à débloquer ({@link Cosmetic}).
     */
    public enum Theme {
        /** Fond sombre : celui pour lequel le jeu est dessiné. */
        DARK("Sombre", false, 0, 0, 0, 0, null, null),
        /** Fond clair : les couleurs du thème sombre, inversées. */
        LIGHT("Clair", true, 1.0, 0, 0, 0, null, null),
        /** Nuit : plus sombre, moins saturé, voilé de bleu. */
        NIGHT("Nuit", false, 0, -0.35, -0.12, 0, "#a9b8ff", null),
        /** Aurore : les teintes tournées vers le vert et le rose des aurores polaires. */
        AURORA("Aurore", false, -0.35, 0.25, 0, 0.05, null, null),
        /** Monochrome : sans couleur, en gris. */
        MONOCHROME("Monochrome", false, 0, -1, 0, 0.1, null, null),
        /** Contraste fort : des couleurs plus vives et plus tranchées, sans noyer les tons sombres (adouci en 0.64). */
        CONTRAST("Contraste fort", false, 0, 0.35, 0.12, 0.25, null, null),
        /** Sépia : une vieille photographie. */
        SEPIA("Sépia", false, 0, -1, 0.05, 0, "#f0cf9c", Cosmetic.SEPIA),
        /** Phosphore : le vert d'un vieil écran. */
        PHOSPHOR("Phosphore", false, 0, -1, 0.1, 0.2, "#6dff8f", Cosmetic.PHOSPHOR),
        /** Infrarouge : la chaleur en couleurs. */
        INFRARED("Infrarouge", false, 0.7, 0.4, 0, 0.25, null, Cosmetic.INFRARED),
        /** Nébuleuse : un voile violet et rose. */
        NEBULA("Nébuleuse", false, 0.15, 0.2, 0, 0, "#e3a8ff", Cosmetic.NEBULA),
        /** Or : tout en reflets dorés. */
        GOLD("Or", false, 0, -1, 0.12, 0.2, "#ffd36a", Cosmetic.GOLD);

        private final String label;
        private final boolean invert;
        private final double hue;
        private final double saturation;
        private final double brightness;
        private final double contrast;
        private final String tint;
        private final Cosmetic unlock;

        Theme(String label, boolean invert, double hue, double saturation, double brightness, double contrast, String tint, Cosmetic unlock) {
            this.label = label;
            this.invert = invert;
            this.hue = hue;
            this.saturation = saturation;
            this.brightness = brightness;
            this.contrast = contrast;
            this.tint = tint;
            this.unlock = unlock;
        }

        public String label() {
            return label;
        }

        /** Vrai si le thème part de l'image inversée (le thème clair). */
        public boolean invert() {
            return invert;
        }

        /** Rotation des teintes, de −1 à 1 (1 : un demi-tour). */
        public double hue() {
            return hue;
        }

        /** Saturation ajoutée, de −1 (gris) à 1. */
        public double saturation() {
            return saturation;
        }

        public double brightness() {
            return brightness;
        }

        public double contrast() {
            return contrast;
        }

        /** La couleur qui teinte toute l'image (multipliée), ou {@code null}. */
        public String tint() {
            return tint;
        }

        /** Vrai si le thème ne change rien : le thème sombre. */
        public boolean isPlain() {
            return !invert && hue == 0 && saturation == 0 && brightness == 0 && contrast == 0 && tint == null;
        }

        /** Ce qui l'ouvre, ou {@code null} s'il est offert d'emblée. */
        public Cosmetic unlock() {
            return unlock;
        }
    }

    /**
     * Les effets visuels qu'on peut couper un par un (version 0.63). Le grand interrupteur des
     * effets les coupe tous ; sinon, chacun suit son propre réglage.
     */
    public enum Effect {
        FLASH("Éclair de l'explosion", "l'onde et l'écran blanc de l'explosion"),
        AMBIENT("Fond animé", "la poussière qui dérive derrière les générateurs"),
        PARTICLE_SPEED("Allure des générateurs", "anneaux, traînées, couronne et arcs quand la production monte"),
        PARTICLE_CREATION("Création de particules", "l'onde et les poussières à chaque particule créée"),
        ATOM_PULSE("Onglet qui bat", "l'onglet des atomes qui bat à chaque atome créé"),
        ATOM_MODEL("Atome qui tourne", "l'atome de l'onglet Atomes, ses orbites et ses orbes qui tournent"),
        TAB_ICONS("Icônes animées", "les petites icônes des onglets qui bougent"),
        DARK_MATTER_MOTION("Matière noire animée", "la matière noire qui palpite et le fond qui scintille"),
        DARK_FALL("Chute de matière noire", "la spirale de matière qui tombe dans la matière noire"),
        COLLIDER_MOTION("Animations du collisionneur", "les ondes des collisions, les quarks qui s'envolent, l'anneau qui scintille ; les faisceaux restent, ils servent à viser"),
        COLLIDER_SPARKS("Étincelles du collisionneur", "les gerbes au détecteur à chaque croisement"),
        BIG_BANG_MOTION("Animations du Big Bang", "l'espace, le ciel, les systèmes solaires et la galaxie qui tournent"),
        UNIVERSE_MOTION("Animations de l'univers", "la toile de l'univers qui tourne et sa naissance");

        private final String label;
        private final String description;

        Effect(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }
    }

    /** Toutes les combien de secondes la partie s'écrit d'elle-même. */
    public enum SaveDelay {
        TEN("10 s", 10),
        THIRTY("30 s", 30),
        MINUTE("1 min", 60),
        FIVE_MINUTES("5 min", 300);

        private final String label;
        private final double seconds;

        SaveDelay(String label, double seconds) {
            this.label = label;
            this.seconds = seconds;
        }

        public String label() {
            return label;
        }

        public double seconds() {
            return seconds;
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

    /**
     * Les formats de fenêtre prédéfinis (version 0.93), en pixels d'écran, puis le plein écran fenêtré
     * (la fenêtre agrandie) et le vrai plein écran. Un format plus grand que l'écran est grisé.
     */
    public enum WindowSize {
        SMALL("Petit", 1280, 800),
        MEDIUM("Moyen", 1600, 900),
        LARGE("Grand", 1920, 1080),
        /** La fenêtre agrandie au maximum, avec sa barre de titre et la barre des tâches. */
        MAXIMIZED("Plein écran fenêtré", 0, 0),
        /** Tout l'écran, sans barre : Échap en sort. */
        FULL("Plein écran", 0, 0);

        private final String label;
        private final double width;
        private final double height;

        WindowSize(String label, double width, double height) {
            this.label = label;
            this.width = width;
            this.height = height;
        }

        public String label() {
            return label;
        }

        public double width() {
            return width;
        }

        public double height() {
            return height;
        }

        /** Le texte du bouton : son nom et ses dimensions. */
        public String text() {
            return width <= 0 ? label : label + " (" + (int) width + " × " + (int) height + ")";
        }
    }

    /** La plus petite fenêtre où tout reste utilisable, à la taille normale de l'interface (version 0.92). */
    public static final double MIN_WINDOW_WIDTH = 820;
    public static final double MIN_WINDOW_HEIGHT = 600;

    /** La plus petite fenêtre pour cette taille de l'interface : elle grandit avec elle. */
    public static double[] minWindow(Scale scale) {
        return new double[] {Math.round(MIN_WINDOW_WIDTH * scale.factor()), Math.round(MIN_WINDOW_HEIGHT * scale.factor())};
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
    private static final String TAB_ORDER = "tabOrder";
    private static final String EFFECTS_OFF = "effectsOff";
    private static final String SAVE_DELAY = "saveDelay";
    private static final String OFFLINE = "offline";
    private static final String COSMETICS = "cosmetics";
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
    /** L'ordre des onglets choisi en les glissant (version 0.60) : leurs noms, séparés par des virgules ; vide pour l'ordre d'origine. */
    private String tabOrder = "";
    /** Les effets coupés un par un (version 0.63). */
    private final java.util.EnumSet<Effect> effectsOff = java.util.EnumSet.noneOf(Effect.class);
    private SaveDelay saveDelay = SaveDelay.THIRTY;
    /** Vrai si le temps passé jeu fermé est rejoué au retour. */
    private boolean offline = true;
    /** Les thèmes et notations débloqués (version 0.63). */
    private final java.util.EnumSet<Cosmetic> cosmetics = java.util.EnumSet.noneOf(Cosmetic.class);
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
                settings.tabOrder = store.get(TAB_ORDER, "");
                settings.volume = Math.max(0, Math.min(100, store.getInt(VOLUME, DEFAULT_VOLUME)));
                settings.saveEnergy = store.getBoolean(SAVE_ENERGY, true);
            } catch (RuntimeException unreadable) {
                // on garde les valeurs par défaut
            }
            try {
                settings.saveDelay = SaveDelay.valueOf(store.get(SAVE_DELAY, SaveDelay.THIRTY.name()));
            } catch (RuntimeException unknown) {
                settings.saveDelay = SaveDelay.THIRTY;
            }
            try {
                settings.offline = store.getBoolean(OFFLINE, true);
                settings.effectsOff.addAll(names(Effect.class, store.get(EFFECTS_OFF, "")));
                settings.cosmetics.addAll(names(Cosmetic.class, store.get(COSMETICS, "")));
            } catch (RuntimeException unreadable) {
                // on garde les valeurs par défaut
            }
            // Un thème ou une notation à débloquer, gardé sans être débloqué (préférences touchées à la main) : celui d'origine.
            if (settings.theme.unlock() != null && !settings.cosmetics.contains(settings.theme.unlock())) settings.theme = Theme.DARK;
            if (settings.notation.unlock() != null && !settings.cosmetics.contains(settings.notation.unlock())) settings.notation = Notation.SCIENTIFIC;
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
        settings.applyEffects();
        return settings;
    }

    /** Les valeurs d'une énumération écrites dans un texte, séparées par des virgules ; les noms inconnus sont ignorés. */
    private static <E extends Enum<E>> java.util.List<E> names(Class<E> type, String text) {
        java.util.List<E> values = new java.util.ArrayList<>();
        for (String name : text.split(",")) {
            try {
                if (!name.isBlank()) values.add(Enum.valueOf(type, name.trim()));
            } catch (IllegalArgumentException unknown) {
                // une valeur d'une autre version du jeu
            }
        }
        return values;
    }

    private static <E extends Enum<E>> String joined(java.util.Set<E> values) {
        StringBuilder text = new StringBuilder();
        for (E value : values) text.append(text.length() == 0 ? "" : ",").append(value.name());
        return text.toString();
    }

    /** Vrai si cet effet est affiché : les effets en marche, et lui pas coupé. */
    public boolean effect(Effect effect) {
        return effects && !effectsOff.contains(effect);
    }

    /** Vrai si cet effet est en marche dans son propre réglage, quel que soit le grand interrupteur. */
    public boolean effectChosen(Effect effect) {
        return !effectsOff.contains(effect);
    }

    public void setEffect(Effect effect, boolean on) {
        if (on) effectsOff.remove(effect);
        else effectsOff.add(effect);
        save(EFFECTS_OFF, joined(effectsOff));
        applyEffects();
    }

    /** Les dessins lisent leurs effets dans un drapeau commun : le recopier après chaque changement. */
    private void applyEffects() {
        idle.ui.particles.ParticleView.setSpeedLook(effect(Effect.PARTICLE_SPEED));
        idle.ui.collider.EnergyView.setSparks(effect(Effect.COLLIDER_SPARKS));
        idle.ui.darkmatter.DarkMatterView.setFall(effect(Effect.DARK_FALL));
        idle.ui.particles.ParticleView.setCreationLook(effect(Effect.PARTICLE_CREATION));
        idle.ui.darkmatter.DarkMatterView.setMotion(effect(Effect.DARK_MATTER_MOTION));
        idle.ui.bigbang.BigBangPage.setMotion(effect(Effect.BIG_BANG_MOTION));
        idle.ui.collider.EnergyView.setMotion(effect(Effect.COLLIDER_MOTION));
    }

    public SaveDelay saveDelay() {
        return saveDelay;
    }

    public void setSaveDelay(SaveDelay delay) {
        this.saveDelay = delay;
        save(SAVE_DELAY, delay.name());
    }

    /** Vrai si le temps passé jeu fermé est rejoué au retour ; faux pour repartir d'où on était. */
    public boolean offline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
        save(OFFLINE, String.valueOf(offline));
    }

    /** Vrai si ce thème ou cette notation à débloquer l'est déjà. */
    public boolean hasCosmetic(Cosmetic cosmetic) {
        return cosmetics.contains(cosmetic);
    }

    /** Vrai si ce thème peut être choisi : offert, ou débloqué. */
    public boolean isOpen(Theme theme) {
        return theme.unlock() == null || cosmetics.contains(theme.unlock());
    }

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
        applyEffects();
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

    /** L'ordre des onglets : leurs noms, séparés par des virgules ; vide pour l'ordre d'origine. */
    public String tabOrder() {
        return tabOrder;
    }

    public void setTabOrder(String order) {
        this.tabOrder = order == null ? "" : order;
        save(TAB_ORDER, this.tabOrder);
    }

    /** Vrai si les onglets sont rangés en colonne, à gauche. */
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

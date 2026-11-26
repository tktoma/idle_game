package idle.ui;

import idle.ui.common.*;
import idle.core.*;
import idle.ui.universe.DimensionWindow;
import java.util.*;
import java.util.prefs.Preferences;
import javafx.scene.input.KeyCode;

/** Une partie de {@link Settings} (découpée en 0.293 pour rester sous 1 000 lignes) : les réglages, leurs valeurs par défaut et leur lecture. */
public abstract class SettingsBase {
    // Ce que fournissent les parties suivantes.
    abstract public boolean unlock(Cosmetic cosmetic);
    abstract void save(String key, String value);

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
        ROMAN("Romaine", Cosmetic.ROMAN),
        // Six notations de plus (SKINS.md, section 8), toutes à débloquer.
        /** 1.21×2^10 : une mantisse entre 1 et 2, et la puissance de deux. */
        BINARY("Binaire", Cosmetic.BINARY),
        /** 0x4D2 jusqu'à 1e15, puis 1.23×16^5 : la puissance de seize. */
        HEX("Hexadécimale", Cosmetic.HEX),
        /** 1234万 : les unités japonaises, de quatre chiffres en quatre chiffres, jusqu'au 無量大数 (1e68) ; au-delà, ×10^…. */
        JAPANESE("Chiffres japonais", Cosmetic.JAPANESE),
        /** ·−−−− ·−·−·− ··−−− ···−− e6 : la mantisse en points et traits, l'exposant en clair. */
        MORSE("Morse", Cosmetic.MORSE),
        /** 12.3 G : les préfixes du Système international, k, M, G… jusqu'au quetta (1e30), puis scientifique. */
        SI("Préfixes SI", Cosmetic.SI),
        /** Les nombres en scientifique ; les durées en battements de cœur : « 3 600 battements » (un par seconde, à 60 par minute). */
        HEARTBEATS("Battements de cœur", Cosmetic.HEARTBEATS);

        final String label;
        final Cosmetic unlock;

        Notation(String label) { this(label, null); }

        Notation(String label, Cosmetic unlock) {
            this.label = label;
            this.unlock = unlock;
        }

        public String label() { return label; }

        /** Ce qui l'ouvre, ou {@code null} si elle est offerte d'emblée. */
        public Cosmetic unlock() { return unlock; }
    }

    /** Combien de niveaux achète un clic sur une amélioration payée en particules. */
    public enum BuyAmount {
        ONE("×1", 1),
        TEN("×10", 10),
        /** Tout ce que le joueur peut payer. */
        MAX("max", Integer.MAX_VALUE);

        final String label;
        final int count;

        BuyAmount(String label, int count) {
            this.label = label;
            this.count = count;
        }

        public String label() { return label; }

        /** Nombre de niveaux demandés à chaque clic ; l'achat s'arrête à ce que le joueur peut payer. */
        public int count() { return count; }
    }

    /**
     * Apparence générale de la fenêtre. Chaque thème est un filtre posé sur le thème sombre
     * ({@link ThemeFilter}) : l'inverser, tourner ses teintes, le désaturer, le teinter, et depuis la version 0.200
     * éclaircir ses noirs d'une couleur (le voile). Douze sont à débloquer ({@link Cosmetic}) ; « Daltonien » est libre.
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
        /** Contraste fort : des couleurs plus vives et plus tranchées, sans noyer les tons sombres (adouci en 0.064). */
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
        GOLD("Or", false, 0, -1, 0.12, 0.2, "#ffd36a", Cosmetic.GOLD),
        // Huit thèmes de plus (SKINS.md, section 8), tous des filtres.
        /** Encre : noir et blanc tranché, le contraste au plus fort. */
        INK("Encre", false, 0, -1, 0.05, 1.0, null, null, Cosmetic.INK),
        /** Cyanotype : du bleu de Prusse au blanc, les noirs voilés de bleu. */
        CYANOTYPE("Cyanotype", false, 0, -1, 0.05, 0.1, null, "#1a3a6b", Cosmetic.CYANOTYPE),
        /** Vieux film : un sépia plus sombre et plus contrasté que « Sépia », les noirs voilés de brun. */
        OLD_FILM("Vieux film", false, 0, -1, -0.05, 0.3, "#d9b78a", "#2a1a08", Cosmetic.OLD_FILM),
        /** Thermique : les teintes retournées, saturées à bloc, en fausses couleurs. */
        THERMAL("Thermique", false, -0.5, 1.0, 0.05, 0.4, null, "#1a0030", Cosmetic.THERMAL),
        /** Daltonien (libre) : les rouges et les verts écartés vers l'orange et le bleu, la saturation baissée, le contraste monté. */
        COLORBLIND("Daltonien", false, 0.08, -0.3, 0.02, 0.15, null, null, null),
        /** Lune : un gris bleuté, les noirs voilés d'ardoise. */
        MOON("Lune", false, 0, -1, 0.05, 0.05, "#b9c4d6", "#101520", Cosmetic.MOON),
        /** Forêt : tout en verts, du sous-bois sombre à la feuille claire. */
        FOREST("Forêt", false, 0, -1, 0.05, 0.1, "#86c46d", "#06180a", Cosmetic.FOREST),
        /** Brasier : rouges et oranges, les noirs voilés de braise. */
        BLAZE("Brasier", false, 0, -1, 0.1, 0.2, "#ff7a2a", "#3a0a00", Cosmetic.BLAZE);

        final String label;
        final boolean invert;
        final double hue;
        final double saturation;
        final double brightness;
        final double contrast;
        final String tint;
        final String shade;
        final Cosmetic unlock;

        Theme(String label, boolean invert, double hue, double saturation, double brightness, double contrast, String tint, Cosmetic unlock) {
            this(label, invert, hue, saturation, brightness, contrast, tint, null, unlock);
        }

        /** Version 0.200 : {@code shade} est le voile qui éclaircit les noirs (mélange « écran »), ou {@code null}. */
        Theme(String label, boolean invert, double hue, double saturation, double brightness, double contrast, String tint, String shade, Cosmetic unlock) {
            this.label = label;
            this.invert = invert;
            this.hue = hue;
            this.saturation = saturation;
            this.brightness = brightness;
            this.contrast = contrast;
            this.tint = tint;
            this.shade = shade;
            this.unlock = unlock;
        }

        public String label() { return label; }

        /** Vrai si le thème part de l'image inversée (le thème clair). */
        public boolean invert() { return invert; }

        /** Rotation des teintes, de −1 à 1 (1 : un demi-tour). */
        public double hue() { return hue; }

        /** Saturation ajoutée, de −1 (gris) à 1. */
        public double saturation() { return saturation; }

        public double brightness() { return brightness; }

        public double contrast() { return contrast; }

        /** La couleur qui teinte toute l'image (multipliée), ou {@code null}. */
        public String tint() { return tint; }

        /** Le voile qui éclaircit les noirs (mélange « écran » : le blanc reste blanc), ou {@code null}. */
        public String shade() { return shade; }

        /** Vrai si le thème ne change rien : le thème sombre. */
        public boolean isPlain() {
            return !invert && hue == 0 && saturation == 0 && brightness == 0 && contrast == 0 && tint == null && shade == null;
        }

        /** Ce qui l'ouvre, ou {@code null} s'il est offert d'emblée. */
        public Cosmetic unlock() { return unlock; }
    }

    /**
     * Les effets visuels qu'on peut couper un par un. Le grand interrupteur des
     * effets les coupe tous ; sinon, chacun suit son propre réglage.
     */
    public enum Effect {
        FLASH("Éclair de l'explosion", "l'onde et l'écran blanc de l'explosion, et les grandes animations qui suivent"),
        AMBIENT("Fond animé", "la poussière qui dérive derrière les générateurs"),
        PARTICLE_SPEED("Allure des générateurs", "les paliers de vitesse : couleur du noyau, anneaux, traînées, couronne et arcs"),
        PARTICLE_CREATION("Création de particules", "les poussières, et l'onde qui se resserre sur le noyau à chaque particule"),
        ATOM_PULSE("Onglet qui bat", "l'onglet des atomes qui bat à chaque atome créé"),
        ATOM_MODEL("Atome qui tourne", "l'atome de l'onglet Atomes, ses orbites et ses orbes qui tournent"),
        TAB_ICONS("Icônes animées", "les petites icônes des onglets qui bougent"),
        DARK_MATTER_MOTION("Matière noire animée", "la matière noire qui palpite et le fond qui scintille"),
        DARK_FALL("Chute de matière noire", "la spirale de matière qui tombe dans la matière noire"),
        COLLIDER_MOTION("Animations du collisionneur", "les ondes des collisions, les quarks qui s'envolent, l'anneau qui scintille, la liaison des hadrons ; les faisceaux restent, ils servent à viser"),
        COLLIDER_SPARKS("Étincelles du collisionneur", "les gerbes au détecteur à chaque croisement"),
        BIG_BANG_MOTION("Animations du Big Bang", "l'espace et le ciel qui tournent"),
        UNIVERSE_MOTION("Animations de l'univers", "la toile de l'univers qui tourne et sa naissance"),
        CLICK_RING("Rond au clic", "un rond qui s'élargit et s'efface là où l'on clique, pour voir chaque clic");

        final String label;
        final String description;

        Effect(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String label() { return label; }

        public String description() { return description; }
    }

    /** Toutes les combien de secondes la partie s'écrit d'elle-même. */
    public enum SaveDelay {
        TEN("10 s", 10),
        THIRTY("30 s", 30),
        MINUTE("1 min", 60),
        FIVE_MINUTES("5 min", 300);

        final String label;
        final double seconds;

        SaveDelay(String label, double seconds) {
            this.label = label;
            this.seconds = seconds;
        }

        public String label() { return label; }

        public double seconds() { return seconds; }
    }

    /** Nombre d'images par seconde au plus : moins d'images, moins de calcul et moins de batterie. */
    public enum FrameRate {
        HIGH("60", 60),
        MEDIUM("30", 30),
        LOW("15", 15);

        final String label;
        final int perSecond;

        FrameRate(String label, int perSecond) {
            this.label = label;
            this.perSecond = perSecond;
        }

        public String label() { return label; }

        int perSecond() { return perSecond; }
    }

    /** Taille de l'interface : tout grandit ou rétrécit ensemble, textes, boutons et dessins. */
    public enum Scale {
        SMALL("Petite", 0.85),
        NORMAL("Normale", 1.0),
        LARGE("Grande", 1.2);

        final String label;
        final double factor;

        Scale(String label, double factor) {
            this.label = label;
            this.factor = factor;
        }

        public String label() { return label; }

        /** Facteur d'agrandissement : 1 pour la taille normale. */
        double factor() { return factor; }
    }

    /**
     * Les formats de fenêtre prédéfinis, en pixels d'écran, puis le plein écran fenêtré
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

        final String label;
        final double width;
        final double height;

        WindowSize(String label, double width, double height) {
            this.label = label;
            this.width = width;
            this.height = height;
        }

        public String label() { return label; }

        public double width() { return width; }

        public double height() { return height; }

        /** Le texte du bouton : son nom et ses dimensions. */
        public String text() { return width <= 0 ? label : label + " (" + (int) width + " × " + (int) height + ")"; }
    }

    /** La plus petite fenêtre où tout reste utilisable, à la taille normale de l'interface. */
    public static final double MIN_WINDOW_WIDTH = 820;

    public static final double MIN_WINDOW_HEIGHT = 600;

    /** La plus petite fenêtre pour cette taille de l'interface : elle grandit avec elle. */
    public static double[] minWindow(Scale scale) {
        return new double[] {Math.round(MIN_WINDOW_WIDTH * scale.factor()), Math.round(MIN_WINDOW_HEIGHT * scale.factor())};
    }

    static final String NOTATION = "notation";

    static final String THEME = "theme";

    static final String SCALE = "scale";

    static final String NOTIFICATIONS = "notifications";

    static final String SHOW_GOAL = "showGoal";

    static final String STATE_SIGNS = "stateSigns";

    static final String CHRONO = "chrono";

    static final String SKY_EVENTS = "skyEvents";

    static final String FREE_CAMERA = "freeCamera";

    static final String SOUND = "sound";

    static final String TABS_LEFT = "tabsLeft";

    static final String TAB_ORDER = "tabOrder";

    static final String EFFECTS_OFF = "effectsOff";

    /** Les catégories coupées : des notifications, des morceaux de l'objectif, des pastilles. */
    static final String NOTICES_OFF = "noticesOff";

    static final String GOAL_PARTS_OFF = "goalPartsOff";

    static final String DOTS_OFF = "dotsOff";

    static final String SAVE_DELAY = "saveDelay";

    static final String OFFLINE = "offline";

    static final String COSMETICS = "cosmetics";

    /** Ce que le joueur a déjà vu de la partie (version 0.318) : {@link Seen}. */
    static final String SEEN = "seen";

    static final String VOLUME = "volume";

    /** La musique de fond : son interrupteur et son volume, à part des sons. */
    static final String MUSIC = "music";

    static final String AUTOMATION_SOUNDS = "automationSounds";

    static final String MUSIC_VOLUME = "musicVolume";

    /** Les catégories de sons coupées. */
    static final String SOUNDS_OFF = "soundsOff";

    static final String FRAME_RATE = "frameRate";

    static final String SAVE_ENERGY = "saveEnergy";

    /** Volume de départ, sur 100 : assez bas pour qu'un premier son ne surprenne pas. */
    static final int DEFAULT_VOLUME = 40;

    /** Volume de départ de la musique : un fond, sous les sons. */
    static final int DEFAULT_MUSIC_VOLUME = 30;

    static final String BUY_AMOUNT = "buyAmount";

    static final String SHORTCUTS = "shortcuts";

    static final String EFFECTS = "effects";

    static final String CONFIRM_EXPLOSION = "confirmExplosion";

    static final String DETAIL_KEY = "detailKey";

    static final String DETAIL_TOGGLE = "detailToggle";

    static final String CONFIRM_BIG_BANG = "confirmBigBang";

    static final String CONFIRM_CHALLENGE = "confirmChallenge";

    /** Préfixe des touches choisies par le joueur : une préférence par raccourci. */
    static final String KEY_PREFIX = "key.";

    /** Les touches des cercles de la boucle : une préférence par cercle, de 0 à 6, et le métronome. */
    static final String LOOP_KEY_PREFIX = "loopKey.";

    static final String LOOP_METRONOME = "loopMetronome";

    static final String MIRROR_PACE = "mirrorPace";

    static final String WINDOW = "window";

    static final String LAST_TAB = "lastTab";

    /**
     * La touche de détail d'origine. Pas Maj, pourtant habituelle pour cet usage : sous Windows,
     * cinq appuis de suite ouvrent la fenêtre des « touches rémanentes ».
     */
    static final KeyCode DEFAULT_DETAIL_KEY = KeyCode.CONTROL;

    /** {@code null} si les préférences ne sont pas accessibles : les réglages ne durent alors que le temps du lancement. */
    Preferences store;

    Notation notation = Notation.SCIENTIFIC;

    boolean effects = true;

    boolean confirmExplosion = true;

    boolean paused = false;

    BuyAmount buyAmount = BuyAmount.ONE;

    boolean shortcuts = true;

    Theme theme = Theme.DARK;

    Scale scale = Scale.NORMAL;

    boolean notifications = true;

    boolean showGoal = true;

    boolean stateSigns = true;

    boolean chrono = true;

    boolean skyEvents = true;

    /** La caméra libre du volume : le regard suit la souris sans clic. Coupée au départ. */
    boolean freeCamera = false;

    boolean sound = true;

    /** Les onglets à gauche, et les sous-onglets en haut à leur place ; sinon, la disposition d'avant. */
    boolean tabsLeft = false;

    /** L'ordre des onglets choisi en les glissant : leurs noms, séparés par des virgules ; vide pour l'ordre d'origine. */
    String tabOrder = "";

    /** Les effets coupés un par un. */
    final EnumSet<Effect> effectsOff = EnumSet.noneOf(Effect.class);

    /** Les notifications coupées par catégorie, les morceaux de l'objectif cachés et les pastilles cachées. */
    final EnumSet<Notice> noticesOff = EnumSet.noneOf(Notice.class);

    final EnumSet<SoundKind> soundsOff = EnumSet.noneOf(SoundKind.class);

    final EnumSet<Goals.Part> goalPartsOff = EnumSet.noneOf(Goals.Part.class);

    final EnumSet<Readiness.Kind> dotsOff = EnumSet.noneOf(Readiness.Kind.class);

    SaveDelay saveDelay = SaveDelay.THIRTY;

    /** Vrai si le temps passé jeu fermé est rejoué au retour. */
    boolean offline = true;

    /** Les thèmes et notations débloqués. */
    final EnumSet<Cosmetic> cosmetics = EnumSet.noneOf(Cosmetic.class);

    /** Ce que le joueur a déjà vu de la partie, gardé même quand elle le referme ou repart de zéro. */
    final EnumSet<Seen> seen = EnumSet.noneOf(Seen.class);

    int volume = DEFAULT_VOLUME;

    boolean music = true;

    /** Faux par défaut : ce que font les automatismes (fusion, explosion) ne sonne pas. */
    boolean automationSounds = false;

    int musicVolume = DEFAULT_MUSIC_VOLUME;

    FrameRate frameRate = FrameRate.HIGH;

    boolean saveEnergy = true;

    KeyCode detailKey = DEFAULT_DETAIL_KEY;

    boolean detailToggle = false;

    boolean confirmBigBang = true;

    boolean confirmChallenge = true;

    final Map<Shortcuts.Action, KeyCode> keys = new EnumMap<>(Shortcuts.Action.class);

    /**
     * La touche de chaque cercle de la boucle, {@code null} s'il n'en a pas. Elles ne servent que dans la fenêtre de la
     * boucle : elles peuvent reprendre la touche d'un raccourci, pas celle d'un autre cercle ni la touche de détail.
     */
    final KeyCode[] loopKeys = new KeyCode[Game.MAX_RINGS];

    /** Le métronome de la boucle : coupé au départ. */
    boolean loopMetronome = false;

    /** La vitesse du motif du miroir : 0 le temps entier de la mémorisation, 1 le plus rapide. */
    double mirrorPace = 0;

    /** Réglages relus dans les préférences de l'utilisateur, ou ceux par défaut si elles sont illisibles. */
    static Settings load() {
        Preferences store;
        try {
            store = Preferences.userNodeForPackage(Settings.class);
        } catch (RuntimeException unavailable) {
            store = null;
        }
        return load(store);
    }

    /** Réglages relus dans ce stockage ({@code null} : ceux par défaut, pour le temps du lancement) ; les tests en donnent un en mémoire. */
    static Settings load(Preferences store) {
        Settings settings = new Settings(store);
        if (store != null) {
            settings.notation = stored(store, NOTATION, Notation.SCIENTIFIC);
            settings.buyAmount = stored(store, BUY_AMOUNT, BuyAmount.ONE);
            settings.theme = stored(store, THEME, Theme.DARK);
            settings.scale = stored(store, SCALE, Scale.NORMAL);
            settings.frameRate = stored(store, FRAME_RATE, FrameRate.HIGH);
            try {
                settings.sound = store.getBoolean(SOUND, true);
                settings.tabsLeft = store.getBoolean(TABS_LEFT, false);
                settings.tabOrder = store.get(TAB_ORDER, "");
                settings.volume = Math.max(0, Math.min(100, store.getInt(VOLUME, DEFAULT_VOLUME)));
                settings.music = store.getBoolean(MUSIC, true);
                settings.automationSounds = store.getBoolean(AUTOMATION_SOUNDS, false);
                settings.musicVolume = Math.max(0, Math.min(100, store.getInt(MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME)));
                settings.saveEnergy = store.getBoolean(SAVE_ENERGY, true);
            } catch (RuntimeException unreadable) {
                // on garde les valeurs par défaut
            }
            settings.saveDelay = stored(store, SAVE_DELAY, SaveDelay.THIRTY);
            try {
                settings.offline = store.getBoolean(OFFLINE, true);
                settings.effectsOff.addAll(names(Effect.class, store.get(EFFECTS_OFF, "")));
                // Absentes d'avant, tout est affiché.
                settings.noticesOff.addAll(names(Notice.class, store.get(NOTICES_OFF, "")));
                settings.soundsOff.addAll(names(SoundKind.class, store.get(SOUNDS_OFF, "")));
                settings.goalPartsOff.addAll(names(Goals.Part.class, store.get(GOAL_PARTS_OFF, "")));
                settings.dotsOff.addAll(names(Readiness.Kind.class, store.get(DOTS_OFF, "")));
                settings.cosmetics.addAll(names(Cosmetic.class, store.get(COSMETICS, "")));
                settings.seen.addAll(names(Seen.class, store.get(SEEN, "")));
            } catch (RuntimeException unreadable) {
                // on garde les valeurs par défaut
            }
            // Un thème ou une notation à débloquer, gardé sans être débloqué (préférences touchées à la main) : celui d'origine.
            if (settings.theme.unlock() != null && !settings.cosmetics.contains(settings.theme.unlock())) settings.theme = Theme.DARK;
            if (settings.notation.unlock() != null && !settings.cosmetics.contains(settings.notation.unlock())) settings.notation = Notation.SCIENTIFIC;
            settings.detailKey = stored(store, DETAIL_KEY, DEFAULT_DETAIL_KEY);
            try {
                settings.detailToggle = store.getBoolean(DETAIL_TOGGLE, false);
                settings.notifications = store.getBoolean(NOTIFICATIONS, true);
                settings.showGoal = store.getBoolean(SHOW_GOAL, true);
                settings.stateSigns = store.getBoolean(STATE_SIGNS, true);
                settings.chrono = store.getBoolean(CHRONO, true);
                settings.skyEvents = store.getBoolean(SKY_EVENTS, true);
                settings.freeCamera = store.getBoolean(FREE_CAMERA, false);
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
        settings.loadLoopKeys();
        Confirm.set(settings.confirmBigBang, settings.confirmChallenge);
        Card.setSigns(settings.stateSigns);
        Readiness.setHidden(settings.dotsOff);
        Format.setNotation(settings.notation);
        settings.applyEffects();
        return settings;
    }

    /**
     * Relit les touches des cercles et le métronome : une touche illisible, déjà prise par un cercle d'avant ou par la
     * touche de détail (préférences touchées à la main) laisse la place à celle d'origine si elle est libre, sinon à aucune.
     */
    void loadLoopKeys() {
        Arrays.fill(loopKeys, null);
        for (int ring = 0; ring < loopKeys.length; ring++) {
            if (store == null) break;
            try {
                KeyCode key = KeyCode.valueOf(store.get(LOOP_KEY_PREFIX + ring, defaultLoopKey(ring).name()));
                if (Shortcuts.bindable(key) && key != detailKey && loopRingOf(key) < 0) loopKeys[ring] = key;
            } catch (RuntimeException unknown) {
                // touche inconnue : celle d'origine sera reprise ci-dessous si elle est libre
            }
        }
        for (int ring = 0; ring < loopKeys.length; ring++) {
            KeyCode key = defaultLoopKey(ring);
            if (loopKeys[ring] == null && key != detailKey && loopRingOf(key) < 0) loopKeys[ring] = key;
        }
        try {
            if (store != null) loopMetronome = store.getBoolean(LOOP_METRONOME, false);
        } catch (RuntimeException unreadable) {
            loopMetronome = false;
        }
        try {
            double pace = store == null ? 0 : store.getDouble(MIRROR_PACE, 0);
            mirrorPace = Double.isFinite(pace) ? Math.max(0, Math.min(1, pace)) : 0;
        } catch (RuntimeException unreadable) {
            mirrorPace = 0;
        }
    }

    /** La touche d'origine du cercle {@code ring} : S D F Espace J K L. */
    static KeyCode defaultLoopKey(int ring) { return DimensionWindow.DEFAULT_LOOP_KEYS.get(ring); }

    /** La touche du cercle {@code ring} de la boucle (0 à 6), ou {@code null} s'il n'en a pas. */
    public KeyCode loopKey(int ring) { return ring >= 0 && ring < loopKeys.length ? loopKeys[ring] : null; }

    /** Le cercle de la boucle qui porte cette touche, ou −1. */
    public int loopRingOf(KeyCode key) {
        for (int ring = 0; ring < loopKeys.length; ring++) if (key != null && loopKeys[ring] == key) return ring;
        return -1;
    }

    /**
     * Donne une touche à un cercle de la boucle.
     *
     * @return faux, sans rien changer, si la touche ne peut pas porter de raccourci, si c'est la touche de détail, ou si
     *         un autre cercle l'a déjà
     */
    public boolean setLoopKey(int ring, KeyCode key) {
        if (ring < 0 || ring >= loopKeys.length || !Shortcuts.bindable(key) || key == detailKey) return false;
        int holder = loopRingOf(key);
        if (holder >= 0 && holder != ring) return false;
        loopKeys[ring] = key;
        save(LOOP_KEY_PREFIX + ring, key.name());
        return true;
    }

    /** Vrai si au moins un cercle n'a plus sa touche d'origine. */
    public boolean hasCustomLoopKeys() {
        for (int ring = 0; ring < loopKeys.length; ring++) if (loopKeys[ring] != defaultLoopKey(ring)) return true;
        return false;
    }

    /** Rend à chaque cercle sa touche d'origine, sauf à celui dont la touche sert de touche de détail. */
    public void resetLoopKeys() {
        for (int ring = 0; ring < loopKeys.length; ring++) {
            loopKeys[ring] = defaultLoopKey(ring) == detailKey ? null : defaultLoopKey(ring);
            if (store == null) continue;
            try {
                store.remove(LOOP_KEY_PREFIX + ring);
            } catch (RuntimeException unwritable) {
                // tant pis : la touche d'origine vaut au moins jusqu'à la fermeture du jeu
            }
        }
    }

    /** Vrai si le métronome de la boucle bat : un tic à chaque huitième de tour du point du premier cercle. */
    public boolean loopMetronome() { return loopMetronome; }

    public void setLoopMetronome(boolean on) {
        loopMetronome = on;
        save(LOOP_METRONOME, String.valueOf(on));
    }

    /** La vitesse du motif du miroir choisie à sa barre : 0 le temps entier de la mémorisation, 1 le plus rapide. */
    public double mirrorPace() { return mirrorPace; }

    public void setMirrorPace(double pace) {
        mirrorPace = Double.isFinite(pace) ? Math.max(0, Math.min(1, pace)) : 0;
        save(MIRROR_PACE, String.valueOf(mirrorPace));
    }

    /** Les valeurs d'une énumération écrites dans un texte, séparées par des virgules ; les noms inconnus (d'une autre version) sont ignorés. */
    static <E extends Enum<E>> List<E> names(Class<E> type, String text) { return Enums.parseAll(type, text); }

    /**
     * Une constante relue dans les préférences (huit blocs pareils avant). Un nom inconnu, d'une autre
     * version, ou des préférences illisibles donnent {@code fallback}.
     */
    static <E extends Enum<E>> E stored(Preferences store, String key, E fallback) {
        try {
            return Enums.parse(fallback.getDeclaringClass(), store.get(key, fallback.name()), fallback);
        } catch (RuntimeException unreadable) {
            return fallback;
        }
    }

    static <E extends Enum<E>> String joined(Set<E> values) {
        StringBuilder text = new StringBuilder();
        for (E value : values) text.append(text.length() == 0 ? "" : ",").append(value.name());
        return text.toString();
    }

    /** Vrai si cet effet est affiché : les effets en marche, et lui pas coupé. */
    public boolean effect(Effect effect) { return effects && !effectsOff.contains(effect); }

    /** Vrai si cet effet est en marche dans son propre réglage, quel que soit le grand interrupteur. */
    public boolean effectChosen(Effect effect) { return !effectsOff.contains(effect); }

    SettingsBase(Preferences store) { this.store = store; }
}

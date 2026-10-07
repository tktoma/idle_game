package idle.ui;

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
final class Settings {

    /** Façon d'écrire les grands nombres. */
    enum Notation {
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

        String label() {
            return label;
        }
    }

    /** Combien de niveaux achète un clic sur une amélioration payée en particules. */
    enum BuyAmount {
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

        String label() {
            return label;
        }

        /** Nombre de niveaux demandés à chaque clic ; l'achat s'arrête à ce que le joueur peut payer. */
        int count() {
            return count;
        }
    }

    /** Apparence générale de la fenêtre. */
    enum Theme {
        /** Fond sombre : celui pour lequel le jeu est dessiné. */
        DARK("Sombre"),
        /** Fond clair : les couleurs du thème sombre, inversées. */
        LIGHT("Clair");

        private final String label;

        Theme(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    /** Taille de l'interface : tout grandit ou rétrécit ensemble, textes, boutons et dessins. */
    enum Scale {
        SMALL("Petite", 0.85),
        NORMAL("Normale", 1.0),
        LARGE("Grande", 1.2);

        private final String label;
        private final double factor;

        Scale(String label, double factor) {
            this.label = label;
            this.factor = factor;
        }

        String label() {
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
    private static final String BUY_AMOUNT = "buyAmount";
    private static final String SHORTCUTS = "shortcuts";
    private static final String EFFECTS = "effects";
    private static final String CONFIRM_EXPLOSION = "confirmExplosion";
    private static final String DETAIL_KEY = "detailKey";
    private static final String DETAIL_TOGGLE = "detailToggle";
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
    private KeyCode detailKey = DEFAULT_DETAIL_KEY;
    private boolean detailToggle = false;

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
                settings.detailKey = KeyCode.valueOf(store.get(DETAIL_KEY, DEFAULT_DETAIL_KEY.name()));
            } catch (RuntimeException unknown) {
                settings.detailKey = DEFAULT_DETAIL_KEY;
            }
            try {
                settings.detailToggle = store.getBoolean(DETAIL_TOGGLE, false);
                settings.notifications = store.getBoolean(NOTIFICATIONS, true);
                settings.showGoal = store.getBoolean(SHOW_GOAL, true);
                settings.shortcuts = store.getBoolean(SHORTCUTS, true);
                settings.effects = store.getBoolean(EFFECTS, true);
                settings.confirmExplosion = store.getBoolean(CONFIRM_EXPLOSION, true);
            } catch (RuntimeException unreadable) {
                // on garde les valeurs par défaut
            }
        }
        Format.setNotation(settings.notation);
        return settings;
    }

    private Settings(Preferences store) {
        this.store = store;
    }

    Notation notation() {
        return notation;
    }

    void setNotation(Notation notation) {
        this.notation = notation;
        Format.setNotation(notation);
        save(NOTATION, notation.name());
    }

    /** Vrai si les effets visuels sont affichés : éclair de l'explosion, fond animé, onglet qui bat. */
    boolean effects() {
        return effects;
    }

    void setEffects(boolean effects) {
        this.effects = effects;
        save(EFFECTS, String.valueOf(effects));
    }

    /** Vrai si l'explosion demande un second clic de confirmation. */
    boolean confirmExplosion() {
        return confirmExplosion;
    }

    void setConfirmExplosion(boolean confirmExplosion) {
        this.confirmExplosion = confirmExplosion;
        save(CONFIRM_EXPLOSION, String.valueOf(confirmExplosion));
    }

    /** Quantité achetée à chaque clic sur une amélioration payée en particules. */
    BuyAmount buyAmount() {
        return buyAmount;
    }

    void setBuyAmount(BuyAmount buyAmount) {
        this.buyAmount = buyAmount;
        save(BUY_AMOUNT, buyAmount.name());
    }

    /** Vrai si les raccourcis clavier sont actifs. */
    boolean shortcuts() {
        return shortcuts;
    }

    void setShortcuts(boolean shortcuts) {
        this.shortcuts = shortcuts;
        save(SHORTCUTS, String.valueOf(shortcuts));
    }

    Theme theme() {
        return theme;
    }

    void setTheme(Theme theme) {
        this.theme = theme;
        save(THEME, theme.name());
    }

    Scale scale() {
        return scale;
    }

    void setScale(Scale scale) {
        this.scale = scale;
        save(SCALE, scale.name());
    }

    /** Vrai si les notifications s'affichent : nouvel élément, succès, déblocage… */
    boolean notifications() {
        return notifications;
    }

    void setNotifications(boolean notifications) {
        this.notifications = notifications;
        save(NOTIFICATIONS, String.valueOf(notifications));
    }

    /** Vrai si l'objectif du moment est rappelé sous les onglets. */
    boolean showGoal() {
        return showGoal;
    }

    void setShowGoal(boolean showGoal) {
        this.showGoal = showGoal;
        save(SHOW_GOAL, String.valueOf(showGoal));
    }

    /** La touche qui fait apparaître les explications complètes des cartes et des pages. */
    KeyCode detailKey() {
        return detailKey;
    }

    void setDetailKey(KeyCode detailKey) {
        this.detailKey = detailKey;
        save(DETAIL_KEY, detailKey.name());
    }

    /**
     * Vrai si la touche de détail bascule (un appui affiche les détails, un autre les cache) ;
     * faux s'il faut la tenir enfoncée.
     */
    boolean detailToggle() {
        return detailToggle;
    }

    void setDetailToggle(boolean detailToggle) {
        this.detailToggle = detailToggle;
        save(DETAIL_TOGGLE, String.valueOf(detailToggle));
    }

    /** Vrai si le jeu est en pause : le temps ne passe plus, rien ne se crée. */
    boolean paused() {
        return paused;
    }

    void setPaused(boolean paused) {
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

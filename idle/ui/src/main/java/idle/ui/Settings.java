package idle.ui;

import java.util.prefs.Preferences;

/**
 * Les réglages du joueur : ils ne changent rien aux règles, seulement à la façon dont le jeu
 * s'affiche et se pilote. Ils sont gardés d'un lancement à l'autre dans les préférences de
 * l'utilisateur ({@link Preferences}) ; la partie elle-même n'est pas sauvegardée ici.
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

    private static final String NOTATION = "notation";
    private static final String EFFECTS = "effects";
    private static final String CONFIRM_EXPLOSION = "confirmExplosion";

    /** {@code null} si les préférences ne sont pas accessibles : les réglages ne durent alors que le temps du lancement. */
    private final Preferences store;
    private Notation notation = Notation.SCIENTIFIC;
    private boolean effects = true;
    private boolean confirmExplosion = true;
    private boolean paused = false;

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

    /** Vrai si le jeu est en pause : le temps ne passe plus, rien ne se crée. */
    boolean paused() {
        return paused;
    }

    void setPaused(boolean paused) {
        this.paused = paused;
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

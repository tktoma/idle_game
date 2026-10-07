package idle.ui;

import javafx.scene.input.KeyCode;

/**
 * Les raccourcis clavier du jeu : ce que chacun fait, et sa touche d'origine. La touche que le
 * joueur a choisie pour chacun est dans {@link Settings#key(Action)} ; c'est {@link GameApp} qui
 * exécute.
 *
 * <p>La touche de détail n'en fait pas partie : elle se tient enfoncée, les raccourcis se frappent.
 */
final class Shortcuts {

    /** Une chose que le clavier peut déclencher. */
    enum Action {
        SPEED("Acheter la vitesse de création", KeyCode.V),
        COUPLING("Acheter le couplage", KeyCode.C),
        GENERATOR("Acheter un générateur", KeyCode.G),
        BUY_ALL("Tout acheter avec les particules", KeyCode.M),
        FUSE("Fusionner", KeyCode.F),
        SYNTHESIZE("Synthétiser un élément", KeyCode.S),
        EXPLODE("Faire exploser le tableau", KeyCode.E),
        BIG_BANG("Déclencher le Big Bang", KeyCode.B),
        AUTOMATION("Couper ou relancer tous les automatismes", KeyCode.A),
        PAUSE("Mettre en pause ou reprendre", KeyCode.P),
        HELP("Ouvrir ou fermer l'aide", KeyCode.H),
        PREVIOUS_TAB("Onglet précédent", KeyCode.LEFT),
        NEXT_TAB("Onglet suivant", KeyCode.RIGHT),
        PREVIOUS_SUB_TAB("Sous-onglet précédent", KeyCode.UP),
        NEXT_SUB_TAB("Sous-onglet suivant", KeyCode.DOWN);

        private final String label;
        private final KeyCode key;

        Action(String label, KeyCode key) {
            this.label = label;
            this.key = key;
        }

        /** Ce que fait le raccourci, en quelques mots. */
        String label() {
            return label;
        }

        /** Sa touche tant que le joueur n'en a pas choisi une autre. */
        KeyCode defaultKey() {
            return key;
        }
    }

    private Shortcuts() {
    }

    /** Le nom d'une touche tel qu'il s'écrit sur une carte ou dans une liste : « V », « ← », « Espace ». */
    static String name(KeyCode key) {
        if (key == null) return "";
        return switch (key) {
            case LEFT -> "←";
            case RIGHT -> "→";
            case UP -> "↑";
            case DOWN -> "↓";
            case ENTER -> "Entrée";
            case BACK_SPACE -> "Retour arrière";
            case DELETE -> "Suppr";
            default -> Detail.keyName(key);
        };
    }

    /** Vrai si cette touche peut porter un raccourci : ni une touche de majuscule ou de contrôle, ni Échap, qui annule. */
    static boolean bindable(KeyCode key) {
        return key != null && !key.isModifierKey() && key != KeyCode.ESCAPE && key != KeyCode.UNDEFINED;
    }
}

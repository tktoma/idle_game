package idle.ui;

import javafx.scene.input.KeyCode;

/**
 * Le mode « détails » : tant que le joueur tient la touche de détail, les cartes et les pages
 * affichent leurs explications complètes. Le reste du temps, elles s'en tiennent à l'essentiel.
 * La touche se choisit dans les réglages ({@link Settings#detailKey()}), où l'on peut aussi la
 * faire basculer d'un appui au lieu de la tenir. C'est {@link GameApp} qui écoute le clavier.
 *
 * <p>C'est un état de l'interface, commun à toutes les pages, comme la notation des nombres.
 */
final class Detail {

    private static boolean shown = false;

    /** Vrai tant que les détails sont demandés : touche de détail enfoncée, ou bascule activée. */
    static boolean shown() {
        return shown;
    }

    static void set(boolean detail) {
        shown = detail;
    }

    /** Le texte long si les détails sont demandés, sinon rien : pour les explications qu'on peut taire. */
    static String only(String text) {
        return shown ? text : "";
    }

    /** Nom d'une touche tel qu'on le lit sur un clavier français : « Maj », « Ctrl », « Espace »… */
    static String keyName(KeyCode key) {
        return switch (key) {
            case SHIFT -> "Maj";
            case CONTROL -> "Ctrl";
            case ALT -> "Alt";
            case TAB -> "Tab";
            case SPACE -> "Espace";
            default -> key.getName();
        };
    }

    private Detail() {}
}

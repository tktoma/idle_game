package idle.core;

import java.util.List;

/**
 * Catalogue des automatismes du jeu. Ils s'achètent chacun une fois, en atomes.
 *
 * <p>Les trois premiers se débloquent avec l'amélioration « Persistance ». Tous partent d'une
 * action toutes les 16 secondes ; chaque niveau de cadence (5, 20 puis 80 atomes) divise ce délai
 * par deux, jusqu'à 2 secondes. Au-delà, seuls les éléments du tableau périodique les accélèrent
 * encore. C'est cette phase, de Persistance à l'ouverture du tableau périodique, qui occupe le
 * joueur de la 3ᵉ à la 7ᵉ heure environ.
 *
 * <p>Le quatrième, la synthèse automatique, se débloque en obtenant un élément unique
 * (gaz noble ou actinide).
 */
public final class Automations {

    public static final List<Automation> DEFAULT = List.of(
            Automation.buying("auto_speed", "Vitesse de création", BigNum.of(3), "speed"),
            Automation.buying("auto_generator", "Nouveau générateur", BigNum.of(5), "generator"),
            // Avec lui, le jeu enchaîne les parties et crée des atomes sans aucun clic.
            Automation.fusing("auto_fusion", "Fusion des générateurs", BigNum.of(15)),
            // Débloqué par le premier élément unique obtenu.
            Automation.synthesizing("auto_synthesis", "Synthèse d'éléments", BigNum.of(30)));

    private Automations() {}
}

package idle.core;

import java.util.List;

/**
 * Catalogue des automatismes du jeu. Ils se débloquent avec l'amélioration « Persistance »
 * et s'achètent chacun une fois, en atomes.
 *
 * <p>Tous partent d'une action toutes les 4 secondes. Chaque niveau de cadence divise ce
 * délai par deux (2 s, 1 s, 0,5 s) et le quatrième le supprime ; ils coûtent 2, 4, 8 puis 16 atomes.
 */
public final class Automations {

    public static final List<Automation> DEFAULT = List.of(
            Automation.buying("auto_speed", "Vitesse de création", BigNum.of(5), "speed"),
            Automation.buying("auto_generator", "Nouveau générateur", BigNum.of(10), "generator"),
            // Le plus cher : avec lui, le jeu enchaîne les parties et crée des atomes sans aucun clic.
            Automation.fusing("auto_fusion", "Fusion des générateurs", BigNum.of(30)));

    private Automations() {}
}

package idle.core;

import java.util.List;

/**
 * Catalogue des automatismes de matière noire : ils prennent en charge ce que les automatismes
 * ordinaires laissent au joueur. Chacun se débloque en possédant assez de matière noire. Les
 * trois qui achètent ou font exploser agissent une fois par seconde ; « Premiers pas », qui joue
 * à la place des automatismes ordinaires, va au rythme d'un automatisme neuf
 * ({@link Automation#DEFAULT_INTERVAL}) : il ne doit jamais faire mieux qu'eux.
 *
 * <p>Avec les quatre en marche, et les automatismes ordinaires, le jeu enchaîne les explosions
 * sans aucun clic ; seules restent au joueur la croissance de la matière noire et son arbre.
 */
public final class DarkAutomations {

    public static final List<DarkAutomation> DEFAULT = List.of(
            // Le début de chaque partie, avant d'avoir racheté Persistance et les automatismes, et les
            // premières synthèses, avant d'avoir un élément unique.
            new DarkAutomation("dark_auto_start", "Premiers pas", DarkAutomation.Kind.PARTICLE_UPGRADES,
                    BigNum.of(2), Automation.DEFAULT_INTERVAL),
            new DarkAutomation("dark_auto_atoms", "Améliorations en atomes", DarkAutomation.Kind.ATOM_UPGRADES,
                    BigNum.of(3), 1),
            new DarkAutomation("dark_auto_machines", "Achat des automatismes", DarkAutomation.Kind.AUTOMATIONS,
                    BigNum.of(4), 1),
            // Le dernier : avec lui, les explosions s'enchaînent toutes seules.
            new DarkAutomation("dark_auto_explosion", "Explosion", DarkAutomation.Kind.EXPLOSION,
                    BigNum.of(6), 1));

    private DarkAutomations() {}
}

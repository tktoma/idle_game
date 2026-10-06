package idle.core;

import java.util.List;

/**
 * Catalogue des automatismes de matière noire : ils prennent en charge ce que les automatismes
 * ordinaires laissent au joueur. Chacun se débloque une fois assez de matière noire gagnée, et
 * agit une fois par seconde.
 *
 * <p>Aucun ne fait double emploi avec un automatisme ordinaire. Le premier ne joue pas à leur
 * place : il les offre, dès le début de chaque partie. Les suivants achètent ce que les
 * automatismes ordinaires n'achètent pas (améliorations en atomes, cadences), et le dernier
 * déclenche l'explosion.
 *
 * <p>Avec les quatre en marche, le jeu enchaîne les explosions sans aucun clic ; seules restent
 * au joueur la croissance de la matière noire et ses améliorations.
 */
public final class DarkAutomations {

    public static final List<DarkAutomation> DEFAULT = List.of(
            // Les automatismes ordinaires, offerts dès le début de chaque partie ; la synthèse
            // automatique, une fois les autres à leur cadence maximale et le tableau ouvert.
            new DarkAutomation("dark_auto_start", "Automatismes offerts", DarkAutomation.Kind.GRANT_AUTOMATIONS,
                    BigNum.of(3), 1),
            new DarkAutomation("dark_auto_atoms", "Améliorations en atomes", DarkAutomation.Kind.ATOM_UPGRADES,
                    BigNum.of(5), 1),
            new DarkAutomation("dark_auto_machines", "Achat des automatismes", DarkAutomation.Kind.AUTOMATIONS,
                    BigNum.of(8), 1),
            // Le dernier : avec lui, les explosions s'enchaînent toutes seules.
            new DarkAutomation("dark_auto_explosion", "Explosion", DarkAutomation.Kind.EXPLOSION,
                    BigNum.of(14), 1));

    private DarkAutomations() {}
}

import idle.core.Game;
import idle.core.Resource;
import idle.core.achievements.Achievements;
import idle.core.bigbang.BigBangCondition;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.upgrades.Upgrade;

/**
 * Décisions économiques communes aux robots du PREMIER univers.
 *
 * Cette politique ne touche pas à l'état directement : chaque achat passe par
 * les préconditions et les méthodes publiques du vrai moteur. Elle n'utilise
 * aucune amélioration, épreuve ou cycle du second univers.
 */
final class FirstUniversePolicy {
    private FirstUniversePolicy() {}

    static boolean needsPersistence(Game game) {
        return game.state().totalAtoms().gte(Game.UNLOCK_TOTAL_ATOMS)
                && !game.keepsUpgradesOnFusion();
    }

    static boolean skyReady(Game game) {
        int needed = game.bigBangSkyRequirement();
        return needed == 0 || game.isBigBangConditionMet(BigBangCondition.SKY);
    }

    /** Une partie encore en train de construire son ciel doit continuer à synthétiser. */
    static boolean finalBangPreparation(Game game) {
        return game.bigBangs() < 8
                && game.completedChallenges() == game.challenges().size()
                && game.achievementCount() >= Game.BIG_BANG_ACHIEVEMENTS
                && game.state().darkMatter().gte(Game.BIG_BANG_DARK_MATTER)
                && game.isBigBangConditionMet(BigBangCondition.PARTICLES)
                && skyReady(game);
    }

    /** L'achat unique de Persistance passe avant toute dépense d'atomes, automatisme compris. */
    static int atoms(Game game, int limit) {
        int count = 0;
        if (needsPersistence(game)) {
            if (game.canBuy("atom_keep") && game.buy("atom_keep")) count++;
            if (needsPersistence(game)) return count;
        }
        for (int n = 0; n < limit; n++) {
            Upgrade choice = null;
            // Tant qu'il manque un succès d'achat précis, prendre l'occasion sans dépenser davantage.
            if (!game.hasAchievement(Achievements.EXACT_CHANGE)) {
                for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
                    if (game.canBuy(upgrade.id())
                            && game.costOf(upgrade.id()).gte(BigNum.of(Achievements.EXACT_CHANGE_COST))
                            && game.costOf(upgrade.id()).equals(game.state().atoms())) {
                        choice = upgrade;
                        break;
                    }
                }
            }
            // Priorités qui rendent la progression stable avant de financer les répétitions.
            if (choice == null) {
                for (String id : new String[]{"atom_double", "atom_mass", "atom_patience", "atom_boost",
                        "atom_discount", "atom_yield", "atom_bulk"}) {
                    for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
                        if (upgrade.id().equals(id) && game.canBuy(id)) {
                            choice = upgrade;
                            break;
                        }
                    }
                    if (choice != null) break;
                }
            }
            // Les achats répétables et les nouveaux catalogues suivent le prix le plus bas.
            if (choice == null) for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
                if (upgrade.id().equals("atom_keep") || !game.canBuy(upgrade.id())) continue;
                if (choice == null || game.costOf(upgrade.id()).lt(game.costOf(choice.id())))
                    choice = upgrade;
            }
            if (choice == null || !game.buy(choice.id())) break;
            count++;
        }
        return count;
    }

    /**
     * Liaisons et premiers niveaux de l'arbre, puis boutique à niveaux limités,
     * enfin améliorations répétables, en plafonnant chaque dépense.
     */
    static int dark(Game game, int limit) {
        boolean bank = finalBangPreparation(game);
        int count = 0;
        for (int n = 0; n < limit; n++) {
            DarkUpgrade chosen = null;
            // Une liaison ou un nouveau nœud doit pouvoir passer avant Densité à l'infini.
            for (DarkUpgrade upgrade : game.darkUpgrades()) {
                if (!upgrade.inTree() || (!upgrade.hasLimit() && game.darkLevelOf(upgrade.id()) > 0))
                    continue;
                if (affordableDark(game, upgrade, bank)) { chosen = upgrade; break; }
            }
            // Toutes les améliorations à niveau limité de la boutique ont leur place.
            if (chosen == null) for (DarkUpgrade upgrade : game.darkUpgrades()) {
                if (upgrade.inTree() || (!upgrade.hasLimit() && game.darkLevelOf(upgrade.id()) > 0))
                    continue;
                if (affordableDark(game, upgrade, bank)) { chosen = upgrade; break; }
            }
            // Répartir les niveaux répétables de la boutique sans vider la matière noire.
            if (chosen == null) for (DarkUpgrade upgrade : game.darkUpgrades()) {
                if (upgrade.inTree() || upgrade.hasLimit() || game.darkLevelOf(upgrade.id()) == 0
                        || !affordableDark(game, upgrade, bank)
                        || game.darkCostOf(upgrade.id()).gt(game.state().darkMatter().multiply(0.05)))
                    continue;
                if (chosen == null || game.darkLevelOf(upgrade.id()) < game.darkLevelOf(chosen.id()))
                    chosen = upgrade;
            }
            if (chosen == null) for (DarkUpgrade upgrade : game.darkUpgrades()) {
                if (!upgrade.inTree() || upgrade.hasLimit() || game.darkLevelOf(upgrade.id()) == 0
                        || !affordableDark(game, upgrade, bank)
                        || game.darkCostOf(upgrade.id()).gt(
                                game.darkBalance(upgrade.branch()).multiply(Game.TREE_REPEAT_SHARE)))
                    continue;
                chosen = upgrade;
                break;
            }
            if (chosen == null || !game.buyDark(chosen.id())) break;
            count++;
        }
        return count;
    }

    private static boolean affordableDark(Game game, DarkUpgrade upgrade, boolean bank) {
        if (!game.canBuyDark(upgrade.id())) return false;
        if (upgrade.branch() != DarkUpgrade.Branch.DARK_MATTER) return true;
        return DarkMatterReservePolicy.allowPurchase(
                bank, game.state().darkMatter(), game.darkCostOf(upgrade.id()),
                Game.BIG_BANG_DARK_MATTER);
    }

    /** La sélection pour les amas suit les mêmes contrôles de fabricabilité que le moteur. */
    static Molecule matterTarget(Game game, idle.core.elements.ElementCategory state) {
        Molecule ready = null, fallback = null;
        double bestVolume = Double.POSITIVE_INFINITY, fallbackVolume = Double.POSITIVE_INFINITY;
        for (Molecule molecule : game.molecules()) {
            if (!molecule.hasState() || molecule.state() != state
                    || !game.hasSubstance(molecule.id())
                    || !game.isMoleculeKindUnlocked(molecule.kind())
                    || !game.isMoleculeWithinReach(molecule.id())
                    || game.isMoleculeKindFull(molecule.id())) continue;
            double size = game.moleculeVolume(molecule.id()).toDouble();
            if (game.canCreateMolecule(molecule.id())) {
                if (size < bestVolume) { bestVolume = size; ready = molecule; }
            } else if (size < fallbackVolume) {
                fallbackVolume = size;
                fallback = molecule;
            }
        }
        return ready == null ? fallback : ready;
    }
}

import idle.core.Game;
import idle.core.Resource;
import idle.core.achievements.Achievements;
import idle.core.bigbang.BigBangCondition;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.core.elements.ElementCategory;
import idle.core.elements.PeriodicTable;
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
        // Réserver les 500 matières noires dès que les autres prérequis
        // sont assurés, et non seulement après avoir déjà atteint 500.
        boolean bank = game.bigBangs() < 8
                && game.completedChallenges() == game.challenges().size()
                && game.achievementCount() >= Game.BIG_BANG_ACHIEVEMENTS;
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
        if (upgrade.branch() != DarkUpgrade.Branch.DARK_MATTER && !upgrade.isLink()) return true;
        return DarkMatterReservePolicy.allowPurchase(
                bank, game.state().darkMatter(), game.darkCostOf(upgrade.id()),
                Game.BIG_BANG_DARK_MATTER);
    }

    /**
     * Cible la famille d'éléments qui rationne le plus la fabrication des grandes
     * réserves de matière. La synthèse aléatoire disperse les tirages sur 118
     * éléments : pour un amas massif, elle n'approvisionne pas assez ses recettes.
     */
    static ElementCategory chemistryTarget(Game game) {
        if (!game.isTargetedSynthesisUnlocked()) return null;
        Molecule.State state = null;
        double largestShortfall = 0;
        for (var e : game.shoppingList().matter().entrySet()) {
            if (e.getValue() <= 0) continue;
            double deficit = e.getValue();
            if (deficit > largestShortfall) {
                largestShortfall = deficit;
                state = e.getKey();
            }
        }
        if (state == null) return null;
        Molecule molecule = matterTarget(game, state);
        if (molecule == null) return null;
        ElementCategory category = null;
        double worstStock = Double.POSITIVE_INFINITY;
        for (var e : molecule.recipe().entrySet()) {
            // Les derniers exemplaires ne sont jamais consommés.
            double supply = Math.max(0, game.elementCount(e.getKey()) - 1.0) / e.getValue();
            if (supply < worstStock) {
                worstStock = supply;
                category = PeriodicTable.element(e.getKey()).category();
            }
        }
        return category;
    }

    /** Concentre la matière sur un seul objectif massif au lieu de disperser
     * les éléments limitants entre cinq recettes simultanées.
     * L'état suivant devient prioritaire dès que le précédent est complet.
     */
    static Molecule.State focusMatterState(Game game) {
        Molecule.State state = null;
        long largest = 0;
        for (var entry : game.shoppingList().matter().entrySet()) {
            long missing = entry.getValue();
            if (missing <= largest) continue;
            if (matterTarget(game, entry.getKey()) == null) continue;
            largest = missing;
            state = entry.getKey();
        }
        return state;
    }

    /** La sélection pour les amas suit les mêmes contrôles de fabricabilité que le moteur. */
    static Molecule matterTarget(Game game, Molecule.State state) {
        Molecule best = null;
        double smallestVolume = Double.POSITIVE_INFINITY;
        // La synthèse regarnit les éléments temporairement absents. Changer
        // d'amas à chaque manque brise l'accrétion en racine du nombre déjà créé.
        // Filtrer ici seulement les obstacles structurels (rayon, espace, plafond).
        for (Molecule molecule : game.molecules()) {
            if (!molecule.hasState() || molecule.state() != state
                    || !game.hasSubstance(molecule.id())
                    || !game.isMoleculeKindUnlocked(molecule.kind())
                    || !game.isMoleculeWithinReach(molecule.id())
                    || game.isMoleculeKindFull(molecule.id())) continue;
            double volume = game.moleculeVolume(molecule.id()).toDouble();
            if (volume < smallestVolume) {
                best = molecule;
                smallestVolume = volume;
            }
        }
        return best;
    }
}

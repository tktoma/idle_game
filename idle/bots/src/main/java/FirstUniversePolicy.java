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

    /** Persistance n'est rentable qu'après les automatismes qui accélèrent les fusions (modèle 0.123, §14.1). */
    static boolean readyForPersistence(Game game) {
        if (!needsPersistence(game)) return false;
        // Ne pas pénaliser les sauvegardes avancées qui ont déjà dépassé cette phase.
        if (game.bigBangs() > 0 || game.state().explosions() > 0) return true;
        return game.ownsAutomation("auto_generator")
                && game.ownsAutomation("auto_fusion")
                && game.automationSpeedLevel("auto_generator") >= 2;
    }

    /** Garantit le générateur automatique, la fusion automatique et deux niveaux de cadence. */
    static int bootstrapAutomations(Game game) {
        if (!needsPersistence(game) || readyForPersistence(game)) return 0;
        int bought = 0;
        if (game.canBuyAutomation("auto_generator") && game.buyAutomation("auto_generator")) bought++;
        if (game.canBuyAutomation("auto_fusion") && game.buyAutomation("auto_fusion")) bought++;
        while (game.ownsAutomation("auto_generator")
                && game.automationSpeedLevel("auto_generator") < 2
                && game.canSpeedUpAutomation("auto_generator")
                && game.speedUpAutomation("auto_generator")) bought++;
        return bought;
    }

    /** Réserver Persistance seulement après l'amorçage des automatismes. */
    static int atoms(Game game, int limit) {
        int count = 0;
        if (readyForPersistence(game)) {
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
            // Ouverture recommandée par le modèle : dédoublement, patience, masse,
            // deuxième dédoublement, catalyseur, compression. Ensuite, acheter au prix.
            if (choice == null && game.state().explosions() == 0 && game.bigBangs() == 0) {
                String[] opening = {"atom_double", "atom_patience", "atom_mass",
                        "atom_double", "atom_boost", "atom_discount"};
                int[] minLevels = {1, 1, 1, 2, 1, 1};
                for (int i = 0; i < opening.length; i++) {
                    if (game.levelOf(opening[i]) >= minLevels[i]) continue;
                    if (game.canBuy(opening[i])) {
                        for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
                            if (upgrade.id().equals(opening[i])) { choice = upgrade; break; }
                        }
                    }
                    break; // préserver l'ordre ; si inaccessible, laisser acheter au prix.
                }
            }
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
        if (!game.isTargetedSynthesisUnlocked() || game.bigBangs() == 0) return null;
        // Le document §15 confirme qu'une recette requiert chaque élément + 1.
        // Approvisionner d'abord la première recette bloquée de la liste.
        for (var item : game.shoppingList().items()) {
            if (item.missing() <= 0 || !game.isMoleculeWithinReach(item.molecule().id())
                    || game.hasElementsForMolecule(item.molecule().id())) continue;
            ElementCategory scarce = scarceFamily(game, item.molecule());
            if (scarce != null) return scarce;
        }
        Molecule.State state = focusMatterState(game);
        if (state == null) return null;
        Molecule molecule = matterTarget(game, state);
        if (molecule == null) return null;
        return scarceFamily(game, molecule);
    }

    private static ElementCategory scarceFamily(Game game, Molecule molecule) {
        ElementCategory category = null;
        double worstStock = Double.POSITIVE_INFINITY;
        // nextMoleculeCost tient compte de la recette restante ; protéger le dernier exemplaire.
        for (var e : game.nextMoleculeCost(molecule.id()).entrySet()) {
            if (e.getValue() <= 0) continue;
            double supply = Math.max(0, game.elementCount(e.getKey()) - 1.0) / e.getValue();
            if (supply < worstStock) {
                worstStock = supply;
                category = PeriodicTable.element(e.getKey()).category();
            }
        }
        return category;
    }

    /** Compare les déficits en proportion du besoin, pas en nombres bruts.
     * Avec 5 millions de gaz manquants sur 400 millions et 3 millions de métaux
     * sur 4 millions, il faut d'abord produire les métaux. L'ancienne priorité
     * sur les nombres bruts empêchait l'univers de se former en 25 heures.
     */
    static Molecule.State focusMatterState(Game game) {
        java.util.Map<Molecule.State,Integer> needs = game.shoppingList().matter();
        java.util.Map<Molecule.State,Integer> objectives = java.util.Map.of();
        if (game.nextCosmos() != null) objectives = game.nextCosmos().matter();
        else if (game.nextBody() != null) objectives = game.nextBody().matter();
        return focusMatterState(needs, objectives);
    }

    static Molecule.State focusMatterState(
            java.util.Map<Molecule.State,Integer> missing,
            java.util.Map<Molecule.State,Integer> objectives) {
        Molecule.State choice = null;
        double highestRatio = 0;
        for (var entry : missing.entrySet()) {
            long lack = Math.max(0, entry.getValue());
            if (lack == 0) continue;
            int target = objectives.getOrDefault(entry.getKey(), 0);
            double ratio = target > 0 ? (double) lack / target : lack;
            if (ratio > highestRatio) {
                highestRatio = ratio;
                choice = entry.getKey();
            }
        }
        return choice;
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

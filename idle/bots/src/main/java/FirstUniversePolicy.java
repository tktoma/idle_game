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
 * Décisions économiques communes aux robots, les trois actes de chaque univers.
 *
 * Cette politique ne touche pas à l'état directement : chaque achat passe par
 * les préconditions et les méthodes publiques du vrai moteur. Ce que le deuxième
 * univers ajoute (régime, cycle, famille, éléments gratuits) est dans
 * {@link NextUniversePolicy} (version 0.130).
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
        // Les dix défis que demande le Big Bang : la fuite, onzième défi du deuxième univers, n'y entre pas (0.130).
        return game.bigBangs() < 8
                && game.baseChallengesDone()
                && game.achievementCount() >= Game.BIG_BANG_ACHIEVEMENTS
                && game.state().darkMatter().gte(Game.BIG_BANG_DARK_MATTER)
                && game.isBigBangConditionMet(BigBangCondition.PARTICLES)
                && skyReady(game);
    }

    /**
     * Modèle 0.123, §8.3 et §15 : au troisième acte, les molécules consomment les exemplaires des éléments, et
     * chaque explosion efface tout le tableau. Le bot (et l'explosion automatique) explosait encore plusieurs fois
     * par seconde après le 5e Big Bang : le carbone ne dépassait jamais 6 exemplaires, le Bitume (C14H10) ne se
     * créait pas, et le ciel restait à 3 astres mûrs pendant plus de 7 h. On n'explose donc plus tant que la
     * matière noire en main suffit au Big Bang et que le prochain pas du ciel ou du cosmos demande des molécules.
     */
    static boolean holdExplosions(Game game) {
        if (game.bigBangs() == 0 || game.inChallenge()) return false;
        if (game.state().darkMatter().lt(Game.BIG_BANG_DARK_MATTER)) return false;
        var list = game.shoppingList();
        for (var item : list.items())
            if (item.missing() > 0 || item.gather()) return true;
        for (var need : list.matter().values()) if (need > 0) return true;
        for (var need : list.sorts().values()) if (need > 0) return true;
        return false;
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

    /** Une étape de l'ordre d'ouverture : niveau d'amélioration, automatisme possédé ou niveau de cadence. */
    record Step(Kind kind, String id, int level) {
        enum Kind { UPGRADE, AUTOMATION, CADENCE }
    }

    /**
     * L'ordre d'achat en atomes « mesuré le meilleur » (modèle 0.123, §14.1) : Dédoublement, Patience,
     * Masse atomique, Dédoublement 2, Catalyseur, Compression ; puis automatismes générateurs et fusion,
     * deux niveaux de cadence des générateurs, Persistance, automatisme de vitesse, cadence de la
     * fusion, 3e cadence des générateurs, Rendement. Ensuite seulement, le moins cher.
     */
    static final Step[] OPENING = {
            new Step(Step.Kind.UPGRADE, "atom_double", 1),
            new Step(Step.Kind.UPGRADE, "atom_patience", 1),
            new Step(Step.Kind.UPGRADE, "atom_mass", 1),
            new Step(Step.Kind.UPGRADE, "atom_double", 2),
            new Step(Step.Kind.UPGRADE, "atom_boost", 1),
            new Step(Step.Kind.UPGRADE, "atom_discount", 1),
            new Step(Step.Kind.AUTOMATION, "auto_generator", 1),
            new Step(Step.Kind.AUTOMATION, "auto_fusion", 1),
            new Step(Step.Kind.CADENCE, "auto_generator", 2),
            new Step(Step.Kind.UPGRADE, "atom_keep", 1),
            new Step(Step.Kind.AUTOMATION, "auto_speed", 1),
            new Step(Step.Kind.CADENCE, "auto_fusion", 1),
            new Step(Step.Kind.CADENCE, "auto_generator", 3),
            new Step(Step.Kind.UPGRADE, "atom_yield", 1),
    };

    static boolean done(Game game, Step step) {
        return switch (step.kind()) {
            case UPGRADE -> game.levelOf(step.id()) >= step.level() || game.isMaxed(step.id());
            case AUTOMATION -> game.ownsAutomation(step.id());
            case CADENCE -> game.automationSpeedLevel(step.id()) >= step.level()
                    || (game.ownsAutomation(step.id()) && game.isAutomationMaxed(step.id()));
        };
    }

    /** Prix de l'étape, ou {@code null} tant qu'elle n'est pas encore ouverte (automatismes avant 30 atomes créés). */
    static BigNum costOf(Game game, Step step) {
        return switch (step.kind()) {
            case UPGRADE -> game.costOf(step.id());
            case AUTOMATION -> {
                if (!game.isAutomationAvailable(step.id())) yield null;
                for (idle.core.upgrades.Automation a : game.automations())
                    if (a.id().equals(step.id())) yield a.cost();
                yield null;
            }
            case CADENCE -> game.ownsAutomation(step.id()) ? game.automationSpeedCost(step.id()) : null;
        };
    }

    static boolean buy(Game game, Step step) {
        return switch (step.kind()) {
            case UPGRADE -> game.canBuy(step.id()) && game.buy(step.id());
            case AUTOMATION -> game.canBuyAutomation(step.id()) && game.buyAutomation(step.id());
            case CADENCE -> game.canSpeedUpAutomation(step.id()) && game.speedUpAutomation(step.id());
        };
    }

    /** Première étape de l'ouverture pas encore faite, ou {@code null} quand l'ouverture est terminée. */
    static Step nextStep(Game game) {
        for (Step step : OPENING) {
            if (done(game, step)) continue;
            // Les automatismes s'ouvrent avec le tableau : après, une étape fermée (défi, règle) ne doit pas tout bloquer.
            if (costOf(game, step) == null && game.isPeriodicTableUnlocked()) continue;
            return step;
        }
        return null;
    }

    /** Vrai quand tout l'ordre du §14.1 est acheté : le reste des atomes suit alors le prix le plus bas. */
    static boolean openingDone(Game game) {
        return nextStep(game) == null;
    }

    /**
     * Atomes à garder pour la prochaine étape de l'ouverture (§14.2 : « garder une réserve d'atomes
     * avant une amélioration chère »). Zéro si l'étape n'est pas encore ouverte ou dépasse le plafond.
     */
    static BigNum openingReserve(Game game) {
        Step step = nextStep(game);
        if (step == null) return BigNum.ZERO;
        BigNum cost = costOf(game, step);
        if (cost == null || (!game.isAtomCapLifted() && cost.gt(game.atomCap()))) return BigNum.ZERO;
        return cost;
    }

    /** Sous le plafond de 118 atomes, la Surcharge se limite à 13 niveaux (§14.1). */
    static final int OVERLOAD_UNDER_CAP = 13;

    /**
     * Une amélioration répétable, une fois l'ouverture faite, « se pèse contre une synthèse » (§14.1) :
     * tant que le tableau n'est pas complet, elle ne passe que si elle coûte moins qu'une synthèse.
     */
    static boolean worthAgainstSynthesis(Game game, String upgradeId) {
        if (upgradeId.equals("atom_overload") && !game.isAtomCapLifted()
                && game.levelOf(upgradeId) >= OVERLOAD_UNDER_CAP) return false;
        if (!game.isPeriodicTableUnlocked() || game.isPeriodicTableComplete()) return true;
        return game.costOf(upgradeId).lte(game.synthesisCost());
    }

    /** Persistance d'abord une fois prête, puis l'ordre du §14.1, puis le moins cher pesé contre une synthèse. */
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
            if (choice == null) {
                Step step = nextStep(game);
                if (step != null) {
                    // Respecter l'ordre : une étape pas encore payable ou pas encore ouverte retient les atomes.
                    if (!buy(game, step)) break;
                    count++;
                    continue;
                }
            }
            // Les achats répétables et les nouveaux catalogues suivent le prix le plus bas.
            if (choice == null) for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
                if (upgrade.id().equals("atom_keep") || !game.canBuy(upgrade.id())
                        || !worthAgainstSynthesis(game, upgrade.id())) continue;
                if (choice == null || game.costOf(upgrade.id()).lt(game.costOf(choice.id())))
                    choice = upgrade;
            }
            if (choice == null || !game.buy(choice.id())) break;
            count++;
        }
        return count;
    }

    /**
     * Synthèses à la main, dès l'ouverture du tableau (§4), sans toucher aux atomes que réclame la
     * prochaine étape de l'ouverture ni à la réserve réglée dans le jeu.
     */
    static int synthesize(Game game, int limit) {
        int count = 0;
        BigNum keep = game.synthesisReserve().max(openingReserve(game));
        for (int n = 0; n < limit && game.canSynthesize(); n++) {
            if (game.state().atoms().subtract(game.synthesisCost()).lt(keep)) break;
            game.synthesize();
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
                && game.baseChallengesDone()
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

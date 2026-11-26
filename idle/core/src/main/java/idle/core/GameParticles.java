package idle.core;

import idle.core.state.*;
import idle.core.bigbang.*;
import idle.core.collider.Hadron;
import idle.core.universe.*;
import idle.core.achievements.*;
import idle.core.darkmatter.*;
import idle.core.elements.*;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.upgrades.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Les particules : la production, la fusion, les améliorations, l'impulsion et la comète. */
public abstract class GameParticles extends GameProduction {
    GameParticles(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** La puissance du tableau périodique : au prorata de son remplissage (éléments différents et exemplaires), nulle sous « Éléments inertes ». */
    public double tablePower() {
        if (powersOff) return 0;
        if (ruled(Challenge.Rule.INERT_ELEMENTS)) return 0;
        if (copiesCacheVersion != state.elementsVersion()) {
            int max = maxTotalCopies();
            double copies = max <= 0 ? 0 : Math.min(1, ownedCopies() / (double) max);
            int distinct = 0;
            for (int count : state.elements().values()) if (count > 0) distinct++;
            double mix = TABLE_DISTINCT_WEIGHT;
            copiesShare = mix * Math.min(1, distinct / (double) PeriodicTable.ELEMENTS.size()) + (1 - mix) * copies;
            copiesCacheVersion = state.elementsVersion();
        }
        return POWER_TABLE * copiesShare * tableMaturity();
    }

    /** La maturité du tableau : sa puissance grandit avec les explosions faites dans cet univers, de 0 à 1. */
    public double tableMaturity() {
        if (TABLE_POWER_EXPLOSIONS <= 0) return 1;
        double done = Math.max(0, state.stats().timedExplosions() - state.explosionsAtUniverse());
        double needed = state.universeFormed() ? TABLE_POWER_EXPLOSIONS_LATER : TABLE_POWER_EXPLOSIONS;
        return Math.min(1, done / needed);
    }

    /**
     * La puissance du ciel : les astres mûrs, les assemblages (un quart d'astre chacun) et les sortes de molécules
     * (un quarantième), rapportés à {@link #SKY_POWER_FULL}. Moitié moindre dans l'univers vide.
     */
    public double skyPower() {
        if (powersOff) return 0;
        if (!isBigBangUnlocked()) return 0;
        double progress = bodiesMature() + assembliesFormed() / 4.0 + moleculeKindsCreated() / 40.0;
        // L'univers vide la réduit de moitié : nulle, elle faisait durer ce défi plus de huit heures.
        double voidFactor = bangRuled(BangChallenge.VOID) ? VOID_SKY_POWER : 1;
        return POWER_SKY * voidFactor * Math.min(1, progress / SKY_POWER_FULL);
    }

    /** La puissance du collisionneur : selon les ordres de grandeur de l'énergie produite, pleine à {@link #COLLIDER_POWER_DECADES}. */
    public double colliderPower() {
        if (powersOff) return 0;
        if (!isColliderUnlocked()) return 0;
        return POWER_COLLIDER * Math.min(1, Math.log10(1 + colliderEnergyProduced()) / COLLIDER_POWER_DECADES);
    }

    /**
     * La puissance des succès : au prorata des succès obtenus, pleine à {@link #POWER_ACHIEVEMENTS_FULL} succès (ou au
     * catalogue entier s'il est plus petit). Les succès ajoutés en 0.174 y comptent, sans la dépasser.
     */
    public double achievementPower() {
        if (powersOff) return 0;
        int full = Math.min(achievements().size(), POWER_ACHIEVEMENTS_FULL);
        return full == 0 ? 0 : POWER_ACHIEVEMENTS * Math.min(1, (double) achievementCount() / full);
    }

    /** Toutes les puissances ensemble. */
    public double systemsPower() { return tablePower() + skyPower() + colliderPower() + achievementPower(); }

    /** Les ordres de grandeur sur lesquels les puissances agissent en ce moment : vitesse × particules par création, sans elles. */
    public double powerBase() { return Math.max(0, speed().log10() + rawParticlesWith(generatorCount()).log10()); }

    BigNum rawParticlesWith(int generators) {
        BigNum result = BigNum.of(elementMultiplier(ElementEffect.Stat.PARTICLES))
                .multiply(darkParticlesMultiplier())
                .multiply(landmarkParticlesMultiplier())
                .multiply(achievementParticlesMultiplier())
                .multiply(moleculeBoost(Molecule.Stat.PARTICLES))
                .multiply(speedMilestoneMultiplier())
                .multiply(colliderParticlesMultiplier())
                .multiply(hadronBoost(Hadron.LAMBDA))
                .multiply(bangAchievementMultiplier(BangAchievement.Reward.PARTICLES))
                .multiply(perfectCycleMultiplier())
                .multiply(universeBangFactor(BANG_UNIVERSE_PARTICLES));
        for (Upgrade upgrade : upgrades.values()) {
            result = result.multiply(particlesMultiplier(upgrade, state.levelOf(upgrade.id()), generators));
        }
        return result;
    }

    /** Particules obtenues à chaque création d'un générateur précis (numéroté à partir de 0). */
    public BigNum particlesPerCreation(int generator) {
        return particlesPerCreation().multiply(generatorParticlesMultiplier(generator));
    }

    /**
     * Multiplicateur de particules qu'apporte une amélioration à un niveau donné, dans la situation actuelle (atomes
     * créés, temps écoulé, générateurs, éléments qui la renforcent). Vaut 1 au niveau 0, et toujours 1 pour une
     * amélioration qui n'agit pas sur les particules par création.
     *
     * <p>L'interface s'en sert pour afficher l'effet actuel d'une amélioration, ou ce qu'elle donnerait une fois
     * achetée.
     */
    public BigNum particlesMultiplier(String upgradeId, int level) {
        return particlesMultiplier(upgrade(upgradeId), level, generatorCount());
    }

    BigNum particlesMultiplier(Upgrade upgrade, int level, int generators) {
        if (level <= 0) return BigNum.ONE;
        ElementBonuses bonuses = bonuses();
        return switch (upgrade.effect()) {
            case Effect.MultiplyParticles multiply ->
                    BigNum.of(multiply.perLevel() + bonuses.upgradeBoost(ElementEffect.UpgradeTarget.DOUBLING)).pow(level);
            // Plafonné comme les atomes eux-mêmes : sans cela, le bonus grandirait sans fin
            // une fois les fusions automatisées.
            case Effect.MultiplyByAtoms byAtoms -> BigNum.ONE.add(state.totalAtoms().min(MAX_ATOMS).multiply(
                    byAtoms.perAtom() + bonuses.upgradeBoost(ElementEffect.UpgradeTarget.MASS))).pow(level);
            case Effect.MultiplyByRunTime byTime -> BigNum.of(1
                    + (byTime.factor() + bonuses.upgradeBoost(ElementEffect.UpgradeTarget.PATIENCE))
                    * Math.sqrt(state.timeSinceFusion() / 60)).pow(level);
            case Effect.Overload overload -> BigNum.of(overload.perLevel()).pow(level);
            // Additif, pas exponentiel : chaque niveau ajoute la même part par autre générateur.
            // Borné au plafond de la 0.299 : une ancienne sauvegarde peut en porter bien plus, jusqu'à la fusion suivante.
            case Effect.MultiplyByGenerators coupling ->
                    BigNum.of(1 + coupling.perGenerator() * Math.min(level, upgrade.maxLevel()) * (inTrial(Trial.LONE_GENERATOR)
                            ? 2 * (generatorsPerAtom() - 1) : Math.max(0, generators - 1)));
            // Les paliers de vitesse sont comptés à part : ils tiennent compte des niveaux offerts.
            case Effect.MultiplySpeed speed -> BigNum.ONE;
            case Effect.AddGenerator generator -> BigNum.ONE;
            case Effect.StrengthenSpeed strengthen -> BigNum.ONE;
            case Effect.DiscountGenerators discount -> BigNum.ONE;
            case Effect.KeepUpgradesOnFusion keep -> BigNum.ONE;
            case Effect.MultiplyAtomsByProduction byProduction -> BigNum.ONE;
        };
    }

    /** Particules créées par seconde, tous générateurs confondus. */
    public BigNum productionPerSecond() { return production(generatorCount()); }

    /**
     * Particules créées par seconde une fois tous les générateurs débloqués : la production
     * au moment d'une fusion. C'est elle que regarde {@link #fusionYield()}.
     */
    public BigNum productionAtFusion() { return production(Math.max(generatorCount(), generatorsPerAtom())); }

    /** Production des {@code generators} premiers générateurs, avec les bonus actuels. */
    BigNum production(int generators) {
        if (generators <= 0) return BigNum.ZERO;
        // Calculés une fois : seuls les bonus propres à chaque générateur diffèrent.
        BigNum common = runningSpeed().multiply(particlesWith(generators));
        double individual = 0;
        for (int generator = 0; generator < generators; generator++) {
            individual = Math.min(1e300, individual + generatorSpeedMultiplier(generator) * generatorParticlesMultiplier(generator));
        }
        return common.multiply(individual);
    }

    // ------------------------------------------------------------------
    // La fusion
    // ------------------------------------------------------------------

    /**
     * Atomes gagnés à la prochaine fusion : {@link #ATOMS_PER_FUSION} plus ceux de l'arbre de matière noire, multipliés
     * par le bonus des éléments, par le {@linkplain #fusionYield() rendement de fusion}, par le nombre de groupes de
     * générateurs ({@link #fusionGroups()}) et par leur {@linkplain #fusionGroupBonus() prime}. Le résultat peut être
     * fractionnaire : les fractions s'accumulent d'une fusion à l'autre.
     */
    public BigNum atomsPerFusion() {
        return ATOMS_PER_FUSION.add(BigNum.of(darkAtomsPerFusion()))
                .multiply(elementMultiplier(ElementEffect.Stat.ATOMS))
                .multiply(darkAtomsMultiplier())
                .multiply(landmarkAtomsMultiplier())
                .multiply(1 + achievementBonus(Achievement.Bonus.ATOMS))
                .multiply(moleculeBoost(Molecule.Stat.ATOMS))
                .multiply(colliderAtomsMultiplier())
                .multiply(bangAchievementMultiplier(BangAchievement.Reward.ATOMS))
                .multiply(fusionYield())
                .multiply(fusionGroups() * fusionGroupBonus())
                .multiply(cycleChoice() == CycleChoice.FUSION ? 1 + CycleChoice.FUSION_GAIN : 1)
                .multiply(contractionAtomsMultiplier())
                .multiply(hadronBoost(Hadron.PROTON));
    }

    /**
     * La prime de groupe : une fusion de plusieurs groupes de générateurs rapporte plus que la somme des groupes,
     * {@link #FUSION_GROUP_BONUS} de plus par groupe au-delà du premier (×1,1 avec deux groupes, ×1,9 avec dix). C'est
     * ce qui rend l'attente payante quand le nombre de générateurs n'est plus limité à dix.
     */
    public double fusionGroupBonus() { return 1 + fusionGroupBonusPerGroup() * (fusionGroups() - 1); }

    /** Ce que chaque groupe au-delà du premier ajoute à la fusion : {@link #FUSION_GROUP_BONUS}, ou plus après le défi réussi. */
    public double fusionGroupBonusPerGroup() {
        return rewarded(Challenge.Reward.GROUP_BONUS) ? REWARD_GROUP_BONUS : FUSION_GROUP_BONUS;
    }

    /**
     * Nombre de groupes complets de générateurs qu'une fusion consommerait maintenant : un tant
     * que leur nombre est limité, deux avec vingt générateurs, etc. Au moins un.
     */
    public int fusionGroups() { return Math.max(1, generatorCount() / generatorsPerAtom()); }

    /**
     * Ce que la production de particules ajoute aux atomes d'une fusion, grâce aux améliorations
     * {@link Effect.MultiplyAtomsByProduction} : un multiplicateur, 1 sans elles ou sous leur seuil.
     */
    public double fusionYield() {
        double multiplier = 1;
        BigNum production = null;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplyAtomsByProduction byProduction
                    && state.levelOf(upgrade.id()) > 0) {
                if (production == null) production = productionAtFusion();
                double decades = production.divide(byProduction.threshold()).log10();
                if (decades > 0) {
                    multiplier *= 1 + fusionYieldPerDecade(byProduction) * state.levelOf(upgrade.id()) * decades;
                }
            }
        }
        return multiplier;
    }

    /** Ce que rapporte chaque ×10 de production à une amélioration de rendement, éléments compris (0.5 = +50 %). */
    public double fusionYieldPerDecade(Effect.MultiplyAtomsByProduction byProduction) {
        return byProduction.perDecade() + bonuses().upgradeBoost(ElementEffect.UpgradeTarget.YIELD);
    }

    /**
     * Vrai quand tous les générateurs sont débloqués et qu'il reste de la place pour un atome : ils peuvent fusionner.
     */
    public boolean canFuse() { return hasAllGenerators() && !isAtomCapReached(); }

    /** Vrai quand tous les générateurs sont débloqués, que la fusion soit possible ou non. */
    public boolean hasAllGenerators() { return state.started() && generatorCount() >= generatorsPerAtom(); }

    /**
     * Vrai quand le joueur possède déjà {@link #MAX_ATOMS} atomes : aucune fusion tant qu'il n'en
     * dépense pas. Toujours faux une fois le plafond levé par l'arbre de matière noire.
     */
    public boolean isAtomCapReached() { return !isAtomCapLifted() && state.atoms().gte(atomCap()); }

    /**
     * Fusionne tous les générateurs en un atome.
     *
     * <p>Impossible tant qu'il manque des générateurs, ou si le joueur possède déjà {@link #MAX_ATOMS} atomes. Sinon il
     * gagne {@link #atomsPerFusion()} et repart du début : un seul générateur, plus aucune particule, plus aucune
     * amélioration payée en particules, sauf la part que garde Persistance ({@link #fusionKeepShare()}). Les
     * améliorations payées en atomes sont toujours conservées.
     *
     * @return {@code true} si la fusion a eu lieu
     */
    public boolean fuse() {
        if (!canFuse()) return false;
        // L'onde de fusion : la matière noire grossit un peu, avec la production d'avant la remise à zéro.
        double pulse = darkFusionPulse();
        if (pulse > 0) expandDarkMatter(pulse);
        BigNum production = productionPerSecond();
        noteHeldPeaks();
        state.stats().noteProduction(production);
        // La production va retomber : l'historique retient le sommet qu'elle vient d'atteindre.
        state.stats().history().notePeak(production.log10());
        state.stats().runHistory().notePeak(production.log10());
        BigNum gained = atomsPerFusion();
        noteFusionFeats();
        state.stats().noteFusion(gained, state.timeSinceFusion());
        // La première fusion de la partie (version 0.126 : de chaque partie, plus seulement du jeu entier) ; une partie
        // reprise d'une ancienne sauvegarde ne la date pas après coup.
        if (state.stats().fusions() - state.fusionsAtUniverse() == 1) noteStep(GameStats.Step.FIRST_FUSION);
        BigNum atoms = state.atoms().add(gained);
        state.setAtoms(isAtomCapLifted() ? atoms : atoms.min(atomCap()));
        notePerfectCycle(!isAtomCapLifted() && atoms.gte(atomCap()));
        // Les skins : la reconstruction se chronomètre, la contraction compte ses fusions.
        if (isSkinsUnlocked()) state.setRebuildPending(true);
        if (inTrial(Trial.SOFT_CONTRACTION)) state.setTrialCount(state.trialCount() + 1);
        state.setTotalAtoms(state.totalAtoms().add(gained));
        state.setParticles(BigNum.ZERO);
        state.clearFormations();
        state.setTimeSinceFusion(0);
        double keep = fusionKeepShare();
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.resource() != Resource.PARTICLES) continue;
            // Les générateurs sont consommés par la fusion, quoi qu'il arrive ; les autres gardent la part de Persistance.
            boolean fused = upgrade.effect() instanceof Effect.AddGenerator;
            int level = state.levelOf(upgrade.id());
            int kept = fused ? 0 : (int) Math.floor(level * keep + 1e-9);
            if (kept != level) state.setLevel(upgrade.id(), kept);
        }
        if (ruled(Challenge.Rule.AMNESIA)) {
            // Le défi : la fusion reprend aussi tout ce qui a été acheté en atomes, Persistance comprise.
            for (Upgrade upgrade : upgrades.values()) {
                if (upgrade.resource() == Resource.ATOMS) state.setLevel(upgrade.id(), 0);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Les améliorations
    // ------------------------------------------------------------------

    /** Toutes les améliorations, dans l'ordre du catalogue. */
    public Collection<Upgrade> upgrades() { return upgrades.values(); }

    /** Les améliorations qui se paient avec une ressource donnée, dans l'ordre du catalogue. */
    public List<Upgrade> upgrades(Resource resource) {
        return upgrades.values().stream().filter(upgrade -> upgrade.resource() == resource).toList();
    }

    public int levelOf(String upgradeId) { return state.levelOf(upgrade(upgradeId).id()); }

    /** Vrai quand l'amélioration a atteint son niveau maximal. */
    public boolean isMaxed(String upgradeId) {
        Upgrade upgrade = upgrade(upgradeId);
        // Les générateurs : la limite est celle du moment, que la matière noire peut repousser.
        if (upgrade.effect() instanceof Effect.AddGenerator) return generatorCount() >= maxGeneratorCount();
        return state.levelOf(upgrade.id()) >= upgrade.maxLevel();
    }

    /** Coût du prochain niveau de cette amélioration, dans sa ressource, toutes réductions comprises. */
    public BigNum costOf(String upgradeId) {
        Upgrade upgrade = upgrade(upgradeId);
        return costAt(upgrade, state.levelOf(upgrade.id()));
    }

    /**
     * Le prix de catalogue d'un niveau ({@link Upgrade#costAt(int)}), gardé : les mêmes niveaux se rachètent à chaque
     * fusion, et la puissance se recalculait à chaque fois (0.298). Le prix d'un niveau ne change jamais ; au-delà de
     * {@link #COST_CACHE_LEVELS} niveaux, il se calcule sans être gardé.
     */
    private BigNum baseCostAt(Upgrade upgrade, int level) {
        if (level < 0 || level >= COST_CACHE_LEVELS) return upgrade.costAt(level);
        BigNum[] costs = baseCosts.computeIfAbsent(upgrade, u -> new BigNum[COST_CACHE_LEVELS]);
        BigNum cost = costs[level];
        if (cost == null) costs[level] = cost = upgrade.costAt(level);
        return cost;
    }

    private static final int COST_CACHE_LEVELS = 4096;

    /** Les prix de catalogue déjà calculés, par amélioration (le catalogue ne change pas pendant une partie). */
    private final Map<Upgrade, BigNum[]> baseCosts = new IdentityHashMap<>();

    /** Coût du niveau acheté quand on en possède {@code level}, toutes réductions comprises. */
    BigNum costAt(Upgrade upgrade, int level) {
        BigNum cost = baseCostAt(upgrade, level);
        // La vitesse s'essouffle : au-delà de SPEED_SCALING_FROM niveaux, chaque niveau coûte de plus en plus.
        if (upgrade.effect() instanceof Effect.MultiplySpeed && level > SPEED_SCALING_FROM) {
            double over = level - SPEED_SCALING_FROM;
            cost = cost.multiply(BigNum.pow10(SPEED_SCALING * over * over));
        }
        // La bulle du jeune arbre, à droite : au-delà de l'ancien plafond, le prix grimpe très vite.
        if (upgrade.effect() instanceof Effect.AddGenerator && hasBubble(GameUniverse.TreeBubble.YOUNG_RIGHT)) {
            int over = 1 + level - (generatorCap() - 1);
            if (over > 0) cost = cost.multiply(BigNum.pow10(BUBBLE_GENERATOR_SCALING * over * over));
        }
        // Les bonus des éléments ne se lisent que pour les prix qu'ils touchent (ils se recalculent après
        // chaque synthèse, et l'achat sans délai demande des milliers de prix par seconde de jeu).
        double factor = 1;
        if (upgrade.effect() instanceof Effect.AddGenerator) {
            factor = generatorCostFactor();
        } else if (upgrade.effect() instanceof Effect.MultiplySpeed) {
            factor = 1 / bonuses().costDivisor(ElementEffect.CostTarget.SPEED_UPGRADES);
        } else if (upgrade.resource() == Resource.ATOMS) {
            factor = 1 / bonuses().costDivisor(ElementEffect.CostTarget.ATOM_UPGRADES);
            if (rewarded(Challenge.Reward.CHEAPER_ATOM_UPGRADES)) factor /= REWARD_ATOM_UPGRADE_DIVISOR;
        }
        if (factor != 1) cost = Upgrade.roundUp(cost.multiply(factor)).max(BigNum.ONE);
        return cost;
    }

    /** Niveaux qu'il reste à acheter avant le maximum de cette amélioration ({@link #MAX_BULK} au plus). */
    int levelsLeft(Upgrade upgrade) {
        // Les générateurs : la limite est celle du moment, que la matière noire peut repousser.
        if (upgrade.effect() instanceof Effect.AddGenerator) {
            return state.started() ? Math.max(0, maxGeneratorCount() - generatorCount()) : 0;
        }
        return Math.min(MAX_BULK, Math.max(0, upgrade.maxLevel() - state.levelOf(upgrade.id())));
    }

    /**
     * Nombre de niveaux de cette amélioration que le joueur peut payer d'un coup, sans dépasser
     * {@code limit} ni le maximum de l'amélioration. C'est ce qu'achèterait {@link #buy(String, int)}.
     */
    public int affordableLevels(String upgradeId, int limit) {
        Upgrade upgrade = upgrade(upgradeId);
        if (!state.started()) return 0;
        int most = Math.min(Math.min(limit, MAX_BULK), levelsLeft(upgrade));
        int level = state.levelOf(upgrade.id());
        BigNum left = balance(upgrade.resource());
        int count = 0;
        while (count < most) {
            BigNum cost = costAt(upgrade, level + count);
            if (left.lt(cost)) break;
            left = left.subtract(cost);
            count++;
        }
        return count;
    }

    /**
     * Coût total des {@code count} prochains niveaux de cette amélioration, toutes réductions
     * comprises. Les niveaux au-delà du maximum ne sont pas comptés.
     */
    public BigNum costOf(String upgradeId, int count) {
        Upgrade upgrade = upgrade(upgradeId);
        int level = state.levelOf(upgrade.id());
        int levels = Math.min(Math.min(count, MAX_BULK), Math.max(1, levelsLeft(upgrade)));
        BigNum total = BigNum.ZERO;
        for (int i = 0; i < levels; i++) {
            total = total.add(costAt(upgrade, level + i));
        }
        return total;
    }

    public boolean canBuy(String upgradeId) { return priceIfBuyable(upgradeId) != null; }

    /**
     * Le prix du prochain niveau s'il peut s'acheter maintenant, sinon {@code null} : les règles de {@link #canBuy(String)},
     * avec un seul calcul du prix pour la vérification et l'achat.
     */
    BigNum priceIfBuyable(String upgradeId) {
        Upgrade upgrade = upgrade(upgradeId);
        // L'épreuve du premier générateur seul : pas d'autre générateur.
        if (upgrade.effect() instanceof Effect.AddGenerator && inTrial(Trial.LONE_GENERATOR)) return null;
        if (!state.started() || isMaxed(upgradeId)) return null;
        BigNum cost = costOf(upgradeId);
        return balance(upgrade.resource()).gte(cost) ? cost : null;
    }

    /**
     * Achète un niveau si la partie a démarré, que le joueur a de quoi payer et que le maximum n'est pas atteint.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean buy(String upgradeId) {
        BigNum cost = priceIfBuyable(upgradeId);
        if (cost == null) return false;
        Upgrade upgrade = upgrade(upgradeId);
        // max(0) : la soustraction de deux double proches peut donner -1e-15 au lieu de 0
        BigNum left = balance(upgrade.resource()).subtract(cost).max(BigNum.ZERO);
        switch (upgrade.resource()) {
            case PARTICLES -> {
                state.setParticles(left);
                state.stats().noteParticleUpgrade(cost, upgrade.effect() instanceof Effect.AddGenerator);
            }
            case ATOMS -> {
                state.setAtoms(left);
                state.stats().noteAtomUpgrade(cost);
                if (left.isZero() && cost.toDouble() >= Achievements.EXACT_CHANGE_COST) award(Achievements.EXACT_CHANGE);
            }
        }
        state.setLevel(upgradeId, state.levelOf(upgradeId) + 1);
        // La fuite et le calme du Big Bang repartent de zéro à chaque amélioration achetée par le
        // joueur ; celles des automatismes ne comptent pas : on peut s'absenter sans couper l'automatisation.
        if (!automating) state.setUpgradeCalm(0);
        // Le calme du Big Bang : tout achat le rompt, celui des automatismes aussi.
        state.setBangCalm(0);
        return true;
    }

    /**
     * Achète jusqu'à {@code count} niveaux d'une amélioration : autant que le joueur peut en
     * payer, sans dépasser son maximum. Pour tout acheter, passer {@link Integer#MAX_VALUE}.
     *
     * @return le nombre de niveaux achetés, 0 si aucun n'était à portée
     */
    public int buy(String upgradeId, int count) {
        int bought = 0;
        while (bought < count && bought < MAX_BULK && buy(upgradeId)) bought++;
        if (bought >= Achievements.BULK_LEVELS) award(Achievements.BULK_PURCHASE);
        return bought;
    }

    /**
     * Achète tout ce qui peut l'être avec les particules, en prenant chaque fois l'amélioration
     * la moins chère : c'est elle qui retarde le moins les suivantes.
     *
     * @return le nombre de niveaux achetés
     */
    public int buyAllWithParticles() {
        int bought = 0;
        while (bought < MAX_BULK) {
            Upgrade cheapest = null;
            for (Upgrade upgrade : upgrades.values()) {
                if (upgrade.resource() != Resource.PARTICLES || !canBuy(upgrade.id())) continue;
                if (cheapest == null || costOf(upgrade.id()).lt(costOf(cheapest.id()))) cheapest = upgrade;
            }
            if (cheapest == null || !buy(cheapest.id())) break;
            bought++;
        }
        if (bought >= Achievements.BUY_EVERYTHING_LEVELS) award(Achievements.BUY_EVERYTHING);
        return bought;
    }

    /**
     * Secondes à attendre avant de pouvoir payer {@code cost} particules : 0 si le joueur les a
     * déjà, l'infini si rien ne produit.
     *
     * <p>Le calcul suit les créations une à une : il tient compte de celle que chaque générateur
     * a déjà commencée. L'attente annoncée diminue donc d'une seconde par seconde, au lieu de
     * rester figée entre deux particules puis de sauter. Ce n'est qu'une estimation au sens où
     * elle suppose que rien ne change d'ici là (ni achat, ni bonus qui grandit avec le temps).
     */
    public double secondsUntilParticles(BigNum cost) {
        BigNum missing = cost.subtract(state.particles());
        if (missing.sign() <= 0) return 0;
        int generators = generatorCount();
        BigNum production = productionPerSecond();
        if (generators == 0 || production.sign() <= 0) return Double.POSITIVE_INFINITY;
        double average = missing.divide(production).toDouble();

        // Les particules se comptent en « créations de base » : seuls les bonus propres à chaque
        // générateur les distinguent. Au-delà d'un grand nombre de créations, ou à très grande
        // vitesse, une création de plus ou de moins ne se voit plus : la moyenne suffit.
        BigNum commonSpeed = runningSpeed();
        BigNum needed = missing.divide(particlesPerCreation());
        if (commonSpeed.exponent() >= 6 || needed.exponent() >= 9) return average;
        double need = needed.toDouble();
        double[] speeds = new double[generators];
        double[] weights = new double[generators];
        double longest = 0;
        for (int generator = 0; generator < generators; generator++) {
            speeds[generator] = commonSpeed.toDouble() * generatorSpeedMultiplier(generator);
            weights[generator] = generatorParticlesMultiplier(generator);
            if (speeds[generator] > 0) longest = Math.max(longest, 1 / speeds[generator]);
        }
        // Le plus petit instant où assez de créations sont terminées. Au plus tard : l'attente
        // moyenne, plus la durée d'une création du générateur le plus lent.
        double low = 0;
        double high = average + longest;
        for (int i = 0; i < 60; i++) {
            double middle = (low + high) / 2;
            double made = 0;
            for (int generator = 0; generator < generators; generator++) {
                made += Math.floor(state.formation(generator) + speeds[generator] * middle + 1e-9) * weights[generator];
            }
            if (made >= need - 1e-9) high = middle; else low = middle;
        }
        return high;
    }

    /**
     * Nombre de fusions qu'il faut encore, au rythme actuel, avant de pouvoir payer {@code cost}
     * atomes : 0 si le joueur les a déjà, −1 si le prix dépasse le plafond d'atomes et ne peut
     * donc pas être atteint tant que ce plafond n'est pas levé.
     */
    public long fusionsUntilAtoms(BigNum cost) {
        BigNum missing = cost.subtract(state.atoms());
        if (missing.sign() <= 0) return 0;
        if (!isAtomCapLifted() && cost.gt(atomCap())) return -1;
        // La petite marge absorbe les erreurs d'arrondi : 3 atomes manquants à 1 par fusion font 3 fusions.
        return (long) Math.min(1e15, Math.ceil(missing.divide(atomsPerFusion()).toDouble() - 1e-9));
    }

    /** Quantité disponible d'une ressource. */
    public BigNum balance(Resource resource) {
        return switch (resource) {
            case PARTICLES -> state.particles();
            case ATOMS -> state.atoms();
        };
    }

    /**
     * Les statistiques de la partie, à jour : le record de production est relevé au passage, pour
     * qu'il tienne compte de la production du moment et pas seulement de celle des fusions.
     */
    public GameStats stats() {
        if (state.started()) state.stats().noteProduction(productionPerSecond());
        return state.stats();
    }

    public GameState state() { return state; }

    Automation automation(String automationId) { return require(automations, automationId, "Automatisme inconnu : "); }

    DarkAutomation darkAutomation(String darkAutomationId) {
        return require(darkAutomations, darkAutomationId, "Automatisme de matière noire inconnu : ");
    }

    DarkUpgrade darkUpgrade(String darkUpgradeId) {
        return require(darkUpgrades, darkUpgradeId, "Amélioration de matière noire inconnue : ");
    }

    Upgrade upgrade(String upgradeId) { return require(upgrades, upgradeId, "Amélioration inconnue : "); }
}

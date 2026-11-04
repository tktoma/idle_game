package idle.core;

import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.DarkAutomation;
import idle.core.darkmatter.DarkEffect;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.Element;
import idle.core.elements.ElementBonuses;
import idle.core.elements.ElementCategory;
import idle.core.elements.ElementEffect;
import idle.core.elements.ElementSet;
import idle.core.elements.ElementSets;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Les atomes : l'automatisation, le tableau périodique, les ensembles, les succès et les records.
 */
public abstract class GameAtoms extends GamePlans {

    GameAtoms(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    public abstract boolean isAutoHoldUnlocked();
    public abstract boolean isMoleculeAutomationUnlocked();
    public abstract boolean isAutoGatherUnlocked();
    public abstract boolean isAutoFormUnlocked();
    public abstract idle.core.universe.CycleChoice cycleChoice();
    public abstract ElementCategory preferredFamily();
    public abstract double preferenceMultiplier(ElementCategory category);

    /** Met à jour ce que donnent les molécules (dont {@link GameBase#elementUncaps}), défini plus bas dans la chaîne. */
    abstract void refreshMoleculeBonuses();
    public abstract boolean isDarkAutomationUnlocked(String darkAutomationId);
    public abstract boolean isDarkAutomationEnabled(String darkAutomationId);
    public abstract double generatorCostFactor();
    public abstract int generatorCount();
    public abstract int generatorCap();
    public abstract int fusionGroups();
    public abstract boolean fuse();
    public abstract boolean buy(String upgradeId);
    public abstract boolean canBuy(String upgradeId);
    abstract Automation automation(String automationId);


    // ------------------------------------------------------------------
    // L'automatisation
    // ------------------------------------------------------------------

    /** Tous les automatismes, dans l'ordre du catalogue. */
    public Collection<Automation> automations() {
        return automations.values();
    }

    /**
     * Vrai une fois {@link #UNLOCK_TOTAL_ATOMS} atomes créés depuis le début de la partie : les
     * automatismes deviennent achetables.
     */
    public boolean isAutomationUnlocked() {
        // Des automatismes conservés après une explosion restent utilisables tout de suite.
        return hasCreatedEnoughAtoms() || !state.ownedAutomations().isEmpty();
    }

    /** Vrai une fois {@link #UNLOCK_TOTAL_ATOMS} atomes créés depuis le début de la partie. */
    boolean hasCreatedEnoughAtoms() {
        return state.totalAtoms().gte(UNLOCK_TOTAL_ATOMS);
    }

    /** Vrai une fois un élément unique obtenu : la synthèse automatique devient achetable. */
    public boolean isSynthesisAutomationUnlocked() {
        for (int number : state.elements().keySet()) {
            if (PeriodicTable.element(number).category().unique()) return true;
        }
        return false;
    }

    /**
     * Vrai si cet automatisme peut être acheté : assez d'atomes créés pour tous, et en plus un
     * élément unique pour la synthèse automatique.
     */
    public boolean isAutomationAvailable(String automationId) {
        Automation automation = automation(automationId);
        // Un automatisme possédé (offert par la matière noire, ou gardé après une explosion) est disponible d'office.
        if (state.ownsAutomation(automation.id())) return true;
        if (!isAutomationUnlocked()) return false;
        return automation.kind() != Automation.Kind.SYNTHESIS || isSynthesisAutomationUnlocked();
    }

    /** Vrai si le joueur a acheté cet automatisme. */
    public boolean ownsAutomation(String automationId) {
        return state.ownsAutomation(automation(automationId).id());
    }

    public boolean canBuyAutomation(String automationId) {
        Automation automation = automation(automationId);
        return isAutomationAvailable(automationId) && !state.ownsAutomation(automation.id())
                && state.atoms().gte(automation.cost());
    }

    /**
     * Achète un automatisme avec des atomes. Il est mis en marche aussitôt.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean buyAutomation(String automationId) {
        if (!canBuyAutomation(automationId)) return false;
        Automation automation = automation(automationId);
        state.setAtoms(state.atoms().subtract(automation.cost()).max(BigNum.ZERO));
        state.stats().addAtomsSpent(automation.cost());
        state.addAutomation(automation.id());
        state.setAutomationEnabled(automation.id(), true);
        return true;
    }

    /** Vrai si cet automatisme est acheté et en marche. */
    public boolean isAutomationEnabled(String automationId) {
        Automation automation = automation(automationId);
        return state.ownsAutomation(automation.id()) && state.isAutomationEnabled(automation.id());
    }

    /**
     * Met en marche ou coupe un automatisme déjà acheté.
     *
     * @return {@code true} si le réglage a été pris en compte
     */
    public boolean setAutomationEnabled(String automationId, boolean enabled) {
        Automation automation = automation(automationId);
        if (!state.ownsAutomation(automation.id())) return false;
        state.setAutomationEnabled(automation.id(), enabled);
        return true;
    }

    /**
     * Vrai tant que le joueur a coupé d'un geste tous les automatismes : ceux de l'onglet
     * Automatisation, ceux de matière noire, l'appui automatique et la création automatique.
     * Aucun n'agit, mais chacun garde son propre réglage et reprend quand la coupure est levée.
     * Le verrou de l'appui n'est pas un automatisme : il continue.
     */
    public boolean isAutomationPaused() {
        return state.automationPaused();
    }

    /** Coupe ou relance d'un geste tous les automatismes. Aucune remise à zéro ne change ce réglage. */
    public void setAutomationPaused(boolean paused) {
        state.setAutomationPaused(paused);
    }

    /** Vrai si le joueur a au moins un automatisme, de quelque sorte que ce soit : il y a alors quelque chose à couper. */
    public boolean hasAnyAutomation() {
        if (!state.ownedAutomations().isEmpty() || isAutoHoldUnlocked() || isMoleculeAutomationUnlocked()
                || isAutoGatherUnlocked() || isAutoFormUnlocked()) return true;
        for (DarkAutomation automation : darkAutomations.values()) {
            if (isDarkAutomationUnlocked(automation.id())) return true;
        }
        return false;
    }

    boolean hasActiveAutomation() {
        if (state.automationPaused()) return false;
        for (Automation automation : automations.values()) {
            if (isAutomationEnabled(automation.id())) return true;
        }
        for (DarkAutomation automation : darkAutomations.values()) {
            if (isDarkAutomationEnabled(automation.id())) return true;
        }
        return false;
    }

    /**
     * Vrai quand les automatismes agissent sans délai et achètent tout ce qu'ils peuvent (version 0.148, bulle du
     * bonsaï, à droite) ; pas pendant le défi de la rouille, qui les ralentit exprès.
     */
    public boolean isAutomationInstant() {
        return hasBubble(GameUniverse.TreeBubble.LEAVES_RIGHT) && !ruled(Challenge.Rule.RUSTY_AUTOMATIONS);
    }

    /** Niveau de cadence acheté pour cet automatisme. */
    public int automationSpeedLevel(String automationId) {
        return state.automationSpeedLevel(automation(automationId).id());
    }

    /**
     * Délai actuel entre deux actions de cet automatisme, en secondes : celui de son niveau de
     * cadence, divisé par le bonus des éléments, sans jamais descendre sous {@link #minAutomationInterval()}.
     */
    public double automationInterval(String automationId) {
        Automation automation = automation(automationId);
        double interval = automation.intervalAt(state.automationSpeedLevel(automation.id()))
                / bonuses().automationDivisor(target(automation)) / darkAutomationDivisor();
        if (rewarded(Challenge.Reward.FASTER_AUTOMATIONS)) interval /= REWARD_AUTOMATION_DIVISOR;
        interval /= 1 + achievementBonus(Achievement.Bonus.AUTOMATION);
        interval = Math.max(minAutomationInterval(), interval);
        // La rouille s'applique en dernier : elle ralentit même ce qui était au minimum.
        return ruled(Challenge.Rule.RUSTY_AUTOMATIONS) ? Math.max(RUSTY_MIN_INTERVAL, interval * RUSTY_FACTOR) : interval;
    }

    /**
     * Délai en dessous duquel aucun automatisme ne descend : {@link #MIN_AUTOMATION_INTERVAL},
     * ou moins si l'arbre de matière noire l'a abaissé.
     */
    public double minAutomationInterval() {
        return darkMemo("minInterval", () -> {
            double min = MIN_AUTOMATION_INTERVAL;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.FasterAutomations faster && effectLevel(dark) > 0) {
                    min = Math.min(min, faster.minInterval());
                }
            }
            return min;
        });
    }

    /** Vrai quand tous les niveaux de cadence de cet automatisme sont achetés. */
    public boolean isAutomationMaxed(String automationId) {
        Automation automation = automation(automationId);
        return state.automationSpeedLevel(automation.id()) >= automation.maxSpeedLevel();
    }

    /** Prix du prochain niveau de cadence de cet automatisme, en atomes. */
    public BigNum automationSpeedCost(String automationId) {
        Automation automation = automation(automationId);
        return automation.speedCostAt(state.automationSpeedLevel(automation.id()));
    }

    public boolean canSpeedUpAutomation(String automationId) {
        Automation automation = automation(automationId);
        return state.ownsAutomation(automation.id()) && !isAutomationMaxed(automationId)
                && state.atoms().gte(automationSpeedCost(automationId));
    }

    /**
     * Achète un niveau de cadence pour un automatisme déjà possédé : son délai est divisé par deux.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean speedUpAutomation(String automationId) {
        if (!canSpeedUpAutomation(automationId)) return false;
        Automation automation = automation(automationId);
        BigNum cost = automationSpeedCost(automationId);
        state.setAtoms(state.atoms().subtract(cost).max(BigNum.ZERO));
        state.stats().addAtomsSpent(cost);
        state.setAutomationSpeedLevel(automation.id(), state.automationSpeedLevel(automation.id()) + 1);
        return true;
    }

    // ------------------------------------------------------------------
    // L'achat en lot (version 0.167)
    // ------------------------------------------------------------------

    /** Niveaux d'achat en lot d'un automatisme : ×2, ×4… jusqu'à ×64. */
    public static final int AUTOMATION_BATCH_LEVELS = 6;

    /** Prix du premier niveau d'achat en lot, en matière noire, et ce que chaque niveau coûte de plus : 2, 8, 32… 2 048. */
    public static final double AUTOMATION_BATCH_BASE_COST = 2, AUTOMATION_BATCH_COST_GROWTH = 4;

    /**
     * Vrai si cet automatisme peut acheter en lot : ceux qui achètent des améliorations, une fois la matière noire
     * obtenue (l'entrée dans l'acte 2). Chacun a son propre niveau.
     */
    public boolean isAutomationBatchAvailable(String automationId) {
        return isDarkMatterUnlocked() && automation(automationId).kind() == Automation.Kind.UPGRADE;
    }

    /** Niveau d'achat en lot de cet automatisme, de 0 à {@link #AUTOMATION_BATCH_LEVELS}. */
    public int automationBatchLevel(String automationId) {
        return Math.min(AUTOMATION_BATCH_LEVELS, state.automationBatchLevel(automation(automationId).id()));
    }

    /** Ce qu'une action de cet automatisme achète au plus : 1, puis ×2 par niveau d'achat en lot, jusqu'à 64. */
    public int automationBatch(String automationId) {
        return 1 << automationBatchLevel(automationId);
    }

    /** Vrai quand cet automatisme achète déjà par 64. */
    public boolean isAutomationBatchMaxed(String automationId) {
        return automationBatchLevel(automationId) >= AUTOMATION_BATCH_LEVELS;
    }

    /** Prix du prochain niveau d'achat en lot de cet automatisme, en matière noire. */
    public BigNum automationBatchCost(String automationId) {
        return BigNum.of(AUTOMATION_BATCH_BASE_COST * Math.pow(AUTOMATION_BATCH_COST_GROWTH, automationBatchLevel(automationId)));
    }

    public boolean canBuyAutomationBatch(String automationId) {
        return isAutomationBatchAvailable(automationId) && !isAutomationBatchMaxed(automationId)
                && state.darkMatter().gte(automationBatchCost(automationId));
    }

    /**
     * Double ce qu'une action de cet automatisme achète (version 0.167), payé en matière noire : elle est dépensée, mais
     * toujours comptée comme gagnée. Le niveau tient tout l'univers, explosions et Big Bangs compris ; il ne part qu'avec
     * l'univers suivant.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean buyAutomationBatch(String automationId) {
        if (!canBuyAutomationBatch(automationId)) return false;
        BigNum cost = automationBatchCost(automationId);
        state.setDarkMatter(state.darkMatter().subtract(cost).max(BigNum.ZERO));
        state.setDarkMatterSpent(state.darkMatterSpent().add(cost));
        Automation automation = automation(automationId);
        state.setAutomationBatchLevel(automation.id(), state.automationBatchLevel(automation.id()) + 1);
        return true;
    }

    /** L'automatisme vu par les éléments qui accélèrent les automatismes. */
    private ElementEffect.AutomationTarget target(Automation automation) {
        return switch (automation.kind()) {
            case FUSION -> ElementEffect.AutomationTarget.FUSION;
            case SYNTHESIS -> ElementEffect.AutomationTarget.SYNTHESIS;
            case UPGRADE -> upgrades.get(automation.upgradeId()).effect() instanceof Effect.AddGenerator
                    ? ElementEffect.AutomationTarget.GENERATORS
                    : ElementEffect.AutomationTarget.SPEED_UPGRADES;
        };
    }

    /**
     * Fait agir, dans l'ordre du catalogue, les automatismes en marche dont le délai est écoulé :
     * une action par délai. Un automatisme prêt qui n'a rien à faire (pas assez de particules)
     * reste prêt : il agira dès que ce sera possible, puis son délai repartira.
     */
    void runAutomation(double dt) {
        boolean instant = isAutomationInstant();
        for (Automation automation : automations.values()) {
            if (!isAutomationEnabled(automation.id())) continue;
            if (instant) {
                // La bulle du bonsaï, à droite (version 0.148) : sans délai, autant d'actions qu'il y a à faire.
                for (int actions = 0; actions < MAX_AUTO_ACTIONS_PER_STEP && act(automation); actions++) {
                    state.stats().noteAutomationAction();
                }
                state.setAutomationTimer(automation.id(), 0);
                continue;
            }
            double interval = automationInterval(automation.id());
            double waited = state.automationTimer(automation.id()) + dt;
            int actions = 0;
            // La petite marge absorbe les erreurs d'arrondi : dix pas de 0,1 s font bien 1 s.
            while (waited >= interval - 1e-9 && actions < MAX_AUTO_ACTIONS_PER_STEP) {
                if (!act(automation)) {
                    waited = interval;
                    break;
                }
                waited = Math.max(0, waited - interval);
                actions++;
                state.stats().noteAutomationAction();
            }
            state.setAutomationTimer(automation.id(), waited);
        }
    }

    /** Fait faire à l'automatisme une action ; faux s'il ne peut rien faire pour l'instant. */
    private boolean act(Automation automation) {
        boolean was = automating;
        automating = true;
        try {
            return actNow(automation);
        } finally {
            automating = was;
        }
    }

    private boolean actNow(Automation automation) {
        return switch (automation.kind()) {
            case UPGRADE -> bulkBuy(automation);
            case FUSION -> generatorCount() >= fusionThreshold() && fuse();
            // La réserve du joueur : la synthèse automatique n'entame pas les atomes mis de côté.
            case SYNTHESIS -> state.atoms().gte(synthesisCost().add(state.synthesisReserve())) && paySynthesis();
        };
    }

    /**
     * L'achat d'un automatisme (version 0.167) : un niveau par action, générateurs compris (avant, celui des générateurs
     * prenait d'un coup tous ceux que les particules permettaient), ou le lot que la matière noire a payé pour lui
     * ({@link #automationBatch(String)}, jusqu'à 64). La cadence fixe donc le rythme des fusions : neuf achats de
     * générateurs avant chacune, un par action sans lot. La bulle du bonsaï, à droite, rend l'achat « max » : sans délai,
     * celui des générateurs prend d'un coup tous ceux que les particules permettent ({@link #buysInBulk(Automation)}).
     */
    private boolean bulkBuy(Automation automation) {
        int most = buysInBulk(automation) ? MAX_GENERATORS_PER_ACTION : automationBatch(automation.id());
        Upgrade upgrade = upgrades.get(automation.upgradeId());
        if (most <= 1) return buy(upgrade.id());
        if (!(upgrade.effect() instanceof Effect.AddGenerator) || upgrade.resource() != Resource.PARTICLES || !state.started()) {
            boolean any = false;
            for (int bought = 0; bought < most && buy(upgrade.id()); bought++) any = true;
            return any;
        }
        // Version 0.126 : les mêmes règles qu'un achat un à un (l'épreuve du premier générateur seul, entre autres),
        // vérifiées une fois avant de calculer le lot.
        if (!canBuy(upgrade.id())) return false;
        // Le même résultat qu'autant d'achats un à un, sans refaire chaque fois tous les calculs :
        // la réduction des générateurs ne dépend pas de leur nombre, elle se calcule une fois. En fin de
        // partie, une action achète des dizaines de générateurs, vingt fois par seconde de jeu.
        // Au-delà de l'ancien plafond (bulle du jeune arbre, version 0.148), le prix n'a plus la forme simple du calcul
        // ci-dessous : ceux-là s'achètent un à un, avec leur vrai prix.
        if (generatorCount() >= generatorCap()) {
            boolean any = false;
            for (int bought = 0; bought < most && buy(upgrade.id()); bought++) any = true;
            return any;
        }
        int room = Math.min(most, Math.min(maxGeneratorCount(), generatorCap()) - generatorCount());
        double factor = generatorCostFactor();
        int level = state.levelOf(upgrade.id());
        BigNum particles = state.particles();
        int bought = 0;
        // Le prix brut du suivant, multiplié par la croissance à chaque achat plutôt que recalculé par une puissance.
        BigNum growth = BigNum.of(upgrade.costGrowth());
        BigNum raw = room > 0 ? upgrade.baseCost().multiply(growth.pow(level)) : BigNum.ZERO;
        while (bought < room) {
            BigNum cost = Upgrade.roundUp(raw);
            raw = raw.multiply(growth);
            if (factor != 1) cost = Upgrade.roundUp(cost.multiply(factor)).max(BigNum.ONE);
            if (particles.lt(cost)) break;
            particles = particles.subtract(cost).max(BigNum.ZERO);
            state.stats().noteParticleUpgrade(cost, true);
            bought++;
        }
        if (bought == 0) return false;
        state.setBangCalm(0);
        state.setParticles(particles);
        state.setLevel(upgrade.id(), level + bought);
        return true;
    }

    /**
     * Vrai pour l'automatisme qui achète les générateurs quand les automatismes agissent sans délai (la bulle du bonsaï, à
     * droite, version 0.167) : une action prend tous ceux que les particules permettent. Sans la bulle, un par action.
     */
    public boolean buysInBulk(Automation automation) {
        return isAutomationInstant() && automation.kind() == Automation.Kind.UPGRADE
                && upgrades.get(automation.upgradeId()).effect() instanceof Effect.AddGenerator;
    }

    /** Garde-fou : le plus grand nombre de générateurs que leur automatisme achète en une action. */
    private static final int MAX_GENERATORS_PER_ACTION = 1_000;

    /**
     * Fait une synthèse et dit si elle a eu lieu : une étape de synthèse ciblée ne donne aucun
     * élément, mais c'est bien une action, payée comme les autres.
     */
    private boolean paySynthesis() {
        int before = state.synthesisCount();
        synthesize();
        return state.synthesisCount() > before;
    }

    /** Atomes que la synthèse automatique laisse toujours au joueur (0 par défaut). */
    public BigNum synthesisReserve() {
        return state.synthesisReserve();
    }

    /**
     * Règle la réserve : la synthèse automatique n'agit que s'il reste au moins {@code atoms}
     * atomes après elle. Les synthèses faites à la main n'en tiennent pas compte. Une valeur
     * négative est ramenée à zéro.
     */
    public void setSynthesisReserve(BigNum atoms) {
        state.setSynthesisReserve(atoms.max(BigNum.ZERO));
    }

    /**
     * Vrai quand la réserve empêche à elle seule la synthèse automatique d'agir un jour : le prix
     * plus la réserve dépassent le plafond d'atomes, que rien ne permet d'atteindre.
     */
    public boolean isSynthesisReserveBlocking() {
        return state.synthesisReserve().sign() > 0 && !isAtomCapLifted() && !isPeriodicTableComplete()
                && synthesisCost().add(state.synthesisReserve()).gt(atomCap());
    }

    // ------------------------------------------------------------------
    // Le tableau périodique
    // ------------------------------------------------------------------

    /**
     * Masse du tableau périodique : 1 avant la première explosion, puis multipliée par
     * {@link #TABLE_WEIGHT_GROWTH} à chaque explosion qui compte, {@link #TABLE_WEIGHT_MAX_LEVEL}
     * fois au plus. Un défi se joue sur un tableau de masse {@link #CHALLENGE_WEIGHT} (1 avant la version 0.097).
     */
    public BigNum tableWeight() {
        if (ruled(Challenge.Rule.PRESENCE_ONLY) || ruled(Challenge.Rule.ABSENCE_ONLY)) return STYLE_CHALLENGE_WEIGHT;
        return inChallenge() ? CHALLENGE_WEIGHT
                : BigNum.of(TABLE_WEIGHT_GROWTH).pow(Math.min(state.tableWeightLevel(), TABLE_WEIGHT_MAX_LEVEL));
    }

    /**
     * Nombre maximal d'atomes qu'on peut posséder en même temps, et prix maximal d'une synthèse :
     * {@link #MAX_ATOMS} fois la masse du tableau. Chaque explosion laisse donc un tableau plus
     * lourd à remplir : il faut produire plus que la fois d'avant pour exploser de nouveau.
     */
    public BigNum atomCap() {
        return Upgrade.roundUp(MAX_ATOMS.multiply(tableWeight()));
    }

    /**
     * Prix de la prochaine synthèse, en atomes : {@link #SYNTHESIS_BASE_COST}, doublé à chaque
     * synthèse déjà faite, plafonné par {@link #atomCap()}, puis réduit par les éléments qui
     * baissent ce prix.
     */
    public BigNum synthesisCost() {
        BigNum cost = SYNTHESIS_BASE_COST.multiply(BigNum.of(2).pow(state.synthesisCount())).min(atomCap());
        double divisor = bonuses().costDivisor(ElementEffect.CostTarget.SYNTHESIS) * darkSynthesisDivisor()
                * (1 + achievementBonus(Achievement.Bonus.SYNTHESIS_COST))
                / (cycleChoice() == idle.core.universe.CycleChoice.SYNTHESIS ? 1 - idle.core.universe.CycleChoice.SYNTHESIS_SAVING : 1);
        return divisor == 1 ? cost : Upgrade.roundUp(cost.divide(divisor)).max(BigNum.ONE);
    }

    /**
     * Vrai une fois {@link #UNLOCK_TOTAL_ATOMS} atomes créés depuis le début de la partie : le
     * tableau périodique devient accessible, en même temps que l'automatisation. Le compte des
     * atomes créés ne redescend qu'à l'explosion : d'ici là, le tableau reste débloqué.
     */
    public boolean isPeriodicTableUnlocked() {
        return hasCreatedEnoughAtoms();
    }

    /** Vrai quand le tableau périodique est débloqué et que le joueur a de quoi payer la prochaine synthèse. */
    public boolean canSynthesize() {
        return state.started() && isPeriodicTableUnlocked() && state.atoms().gte(synthesisCost())
                && !isPeriodicTableComplete();
    }

    /**
     * Vrai quand tous les éléments sont à leur nombre maximal d'exemplaires
     * ({@link #maxCopiesOf(Element)}) : il n'y a plus rien à synthétiser.
     */
    public boolean isPeriodicTableComplete() {
        for (ElementCategory category : ElementCategory.values()) {
            if (anyAvailable(category)) return false;
        }
        return true;
    }

    /** Vrai quand cet élément a atteint son nombre maximal d'exemplaires : il ne sortira plus. */
    public boolean isElementMaxed(int atomicNumber) {
        Element element = PeriodicTable.element(atomicNumber);
        return state.elementCount(element.number()) >= maxCopiesOf(element);
    }

    /** Nombre total d'exemplaires possédés, tous éléments confondus, sans dépasser le maximum de chacun. */
    public int ownedCopies() {
        int copies = 0;
        for (Map.Entry<Integer, Integer> entry : state.elements().entrySet()) {
            copies += Math.min(entry.getValue(), maxCopiesOf(PeriodicTable.element(entry.getKey())));
        }
        return copies;
    }

    /** Nombre total d'exemplaires du tableau de base, avant ce que l'arbre de matière noire y ajoute ({@link #maxTotalCopies()}). */
    public static int maxCopies() {
        int copies = 0;
        for (Element element : PeriodicTable.ELEMENTS) copies += element.category().maxCopies();
        return copies;
    }

    /**
     * Consomme {@link #synthesisCost()} atomes pour créer un élément du tableau périodique tiré au
     * sort : d'abord la famille, selon {@link #categoryChance(ElementCategory)}, puis un élément de
     * cette famille. Un élément déjà possédé gagne un exemplaire ; un élément qui a atteint son
     * maximum d'exemplaires (un seul pour un élément unique) ne peut pas retomber. Avec de la
     * chance ({@link #doubleDrawChance()}), un second élément est tiré, s'il en reste à obtenir.
     * Si aucun élément unique n'est sorti avant, la synthèse n° {@link #guaranteedUniqueSynthesis()}
     * en donne un à coup sûr.
     * Rien d'autre n'est perdu : améliorations et automatismes sont conservés.
     *
     * <p>Avec une famille visée ({@link #setSynthesisTarget(ElementCategory)}), la synthèse est
     * ciblée : elle coûte plusieurs synthèses ({@link ElementCategory#targetTries()}). Les
     * premières sont payées comme les autres, font monter le prix comme les autres, mais ne
     * donnent rien ; la dernière donne un élément de la famille, en priorité un que le joueur
     * n'a pas encore.
     *
     * @return les éléments obtenus (un, parfois deux, davantage avec la matière noire), ou une
     *         liste vide si la synthèse est impossible (tableau verrouillé, tableau complet ou pas
     *         assez d'atomes) ou si elle n'était qu'une étape d'une synthèse ciblée
     */
    public List<Element> synthesize() {
        if (!canSynthesize()) return List.of();
        BigNum cost = synthesisCost();
        state.setAtoms(state.atoms().subtract(cost).max(BigNum.ZERO));
        state.stats().addAtomsSpent(cost);
        state.setSynthesisCount(state.synthesisCount() + 1);

        double doubleChance = doubleDrawChance();   // la chance d'avant le tirage, pas celle qu'il donnerait
        boolean guaranteed = !isSynthesisAutomationUnlocked()
                && state.synthesisCount() >= guaranteedUniqueSynthesis();
        // La garantie d'un élément unique passe avant la cible : cette synthèse-là est ordinaire.
        ElementCategory target = guaranteed ? null : synthesisTarget();
        if (target != null) {
            state.setSynthesisTries(state.synthesisTries() + 1);
            if (state.synthesisTries() < target.targetTries()) {
                state.stats().noteSynthesisTry();
                return List.of();
            }
            state.setSynthesisTries(0);
            state.stats().noteTargetedSynthesis();
            award(Achievements.TARGETED);
            if (target == ElementCategory.ACTINIDE) award(Achievements.TARGETED_ACTINIDE);
        }
        List<Element> obtained = new ArrayList<>();
        int known = discoveredElements();
        // Le garde-fou du hasard : après trop de synthèses sans rien de nouveau, celle-ci donne un élément inconnu.
        boolean rescue = target == null && !guaranteed && LUCK_FLOOR > 0 && state.drySyntheses() >= LUCK_FLOOR;
        obtained.add(target != null ? drawTargeted(target) : rescue ? drawUnknown() : drawElement(guaranteed));
        // La préférence graduée (version 0.087) : chaque tirage au hasard la renforce un peu.
        if (target == null && preferredFamily() != null) state.setPreferenceDraws(state.preferenceDraws() + 1);
        // Les éléments en plus de la matière noire, tant qu'il en reste à obtenir.
        for (int extra = elementsPerSynthesis() - 1; extra > 0 && !isPeriodicTableComplete(); extra--) {
            obtained.add(drawElement(false));
        }
        boolean doubleDraw = random.nextDouble() < doubleChance && !isPeriodicTableComplete();
        if (doubleDraw) obtained.add(drawElement(false));
        state.stats().noteSynthesis(obtained.size(), doubleDraw);
        // Rien de nouveau si le tableau compte autant d'éléments connus qu'avant le tirage.
        state.setDrySyntheses(discoveredElements() > known ? 0 : state.drySyntheses() + 1);
        if (doubleDraw) award(Achievements.DOUBLE_DRAW);
        if (obtained.size() >= 3) award(Achievements.TRIPLE_DRAW);
        return obtained;
    }

    /**
     * Famille visée par la synthèse ciblée, ou {@code null} si la synthèse tire au hasard. Une
     * famille dont tous les éléments sont à leur maximum d'exemplaires ne peut plus être visée :
     * la cible s'efface d'elle-même.
     */
    public ElementCategory synthesisTarget() {
        ElementCategory target = state.synthesisTarget();
        if (target != null && !isTargetedSynthesisUnlocked()) return null;
        if (target != null && !anyAvailable(target)) {
            state.setSynthesisTarget(null);
            return null;
        }
        return target;
    }

    /**
     * Vise une famille pour les prochaines synthèses, à la main comme automatiques, ou revient
     * au hasard avec {@code null}. Les synthèses déjà payées pour une cible restent acquises si
     * la cible change : elles comptent pour la suivante.
     *
     * @return {@code true} si la cible a été retenue ; faux si la synthèse ciblée n'est pas
     *         débloquée ({@link #isTargetedSynthesisUnlocked()}), si le tableau périodique est
     *         verrouillé ou si la famille n'a plus rien à donner
     */
    public boolean setSynthesisTarget(ElementCategory category) {
        if (category != null && (!isTargetedSynthesisUnlocked() || !isPeriodicTableUnlocked()
                || !anyAvailable(category))) return false;
        state.setSynthesisTarget(category);
        return true;
    }

    /** Vrai une fois la synthèse ciblée achetée avec de la matière noire : d'ici là, la synthèse tire toujours au hasard. */
    public boolean isTargetedSynthesisUnlocked() {
        return hasDark(DarkEffect.TargetedSynthesis.class);
    }

    /** Vrai une fois acquis l'achat groupé des améliorations payées en atomes : par dix, ou tout ce qui est à portée. */
    public boolean isBulkAtomBuyUnlocked() {
        if (hasDark(DarkEffect.BulkAtomUpgrades.class)) return true;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.BulkAtomBuy && state.levelOf(upgrade.id()) > 0) return true;
        }
        return false;
    }

    /** Synthèses déjà payées pour la synthèse ciblée en cours. */
    public int synthesisTries() {
        return state.synthesisTries();
    }

    /**
     * Synthèses qu'il reste à payer, la prochaine comprise, avant que la synthèse ciblée donne
     * son élément ; 0 sans cible. Les essais déjà payés au-delà de ce que demande la cible ne
     * sont pas perdus : la prochaine synthèse aboutit.
     */
    public int synthesisTriesLeft() {
        ElementCategory target = synthesisTarget();
        return target == null ? 0 : Math.max(1, target.targetTries() - state.synthesisTries());
    }

    /**
     * Tire un élément de la famille visée et l'ajoute à la collection : un élément que le joueur
     * ne possède pas encore s'il en reste, sinon un de ceux qui peuvent encore gagner un exemplaire.
     */
    private Element drawTargeted(ElementCategory target) {
        // Version 0.170 : le même tirage, sans construire les listes (la synthèse en fait des milliers par heure de jeu).
        int cap = maxCopiesOf(target);
        int unknown = availableCount(target, cap, true);
        boolean among = unknown > 0;
        int size = among ? unknown : availableCount(target, cap, false);
        Element element = availableAt(target, cap, among, random.nextInt(size));
        state.setElementCount(element.number(), state.elementCount(element.number()) + 1);
        return element;
    }

    /**
     * Nombre de synthèses de suite sans élément nouveau au bout duquel la suivante en donne un à
     * coup sûr, tant qu'il en reste à découvrir. C'est ce qui borne la malchance : le dernier
     * élément du tableau ne se fait plus attendre des dizaines de synthèses.
     */
    public static final int LUCK_FLOOR = 2;

    /**
     * Vrai si la prochaine synthèse ordinaire donnera à coup sûr un élément que le joueur n'a pas :
     * les {@link #LUCK_FLOOR} dernières n'ont rien apporté de nouveau, aucune famille n'est visée
     * et il reste des éléments à découvrir.
     */
    public boolean isNextSynthesisNew() {
        return LUCK_FLOOR > 0 && state.drySyntheses() >= LUCK_FLOOR && isPeriodicTableUnlocked()
                && synthesisTarget() == null && discoveredElements() < PeriodicTable.ELEMENTS.size();
    }

    /**
     * Tire un élément que le joueur n'a pas encore, avec les mêmes poids de familles que le tirage
     * ordinaire, parmi les familles qui en ont. S'il n'en reste aucun à découvrir, c'est un tirage ordinaire.
     */
    private Element drawUnknown() {
        // Version 0.170 : les poids et les comptes se calculent une fois par famille, sans construire de listes ;
        // les tirages au sort sont les mêmes qu'avant (une famille au poids, puis un élément parmi ses inconnus).
        ElementCategory[] categories = ElementCategory.values();
        double[] weights = new double[categories.length];
        int[] unknown = new int[categories.length];
        double total = 0;
        int families = 0;
        for (int i = 0; i < categories.length; i++) {
            double weight = drawWeight(categories[i]);
            if (weight <= 0) continue;
            int count = availableCount(categories[i], maxCopiesOf(categories[i]), true);
            if (count == 0) continue;
            weights[i] = weight;
            unknown[i] = count;
            total += weight;
            families++;
        }
        if (families == 0) return drawElement(false);
        double roll = random.nextDouble() * total;
        int chosen = -1;
        for (int i = 0; i < categories.length; i++) {
            if (unknown[i] == 0) continue;
            chosen = i;
            roll -= weights[i];
            if (roll < 0) break;
        }
        ElementCategory category = categories[chosen];
        Element element = availableAt(category, maxCopiesOf(category), true, random.nextInt(unknown[chosen]));
        state.setElementCount(element.number(), 1);
        return element;
    }

    /**
     * Tire un élément et l'ajoute à la collection.
     *
     * @param uniqueOnly vrai pour ne tirer que parmi les familles uniques
     */
    private Element drawElement(boolean uniqueOnly) {
        // Version 0.167 : chaque poids se calcule une fois par tirage (deux avant), pour la synthèse sans délai.
        ElementCategory[] categories = ElementCategory.values();
        double[] weights = new double[categories.length];
        double total = 0;
        for (int i = 0; i < categories.length; i++) {
            if (!uniqueOnly || categories[i].unique()) {
                weights[i] = drawWeight(categories[i]);
                total += weights[i];
            }
        }
        double roll = random.nextDouble() * total;
        ElementCategory chosen = null;
        for (int i = 0; i < categories.length; i++) {
            ElementCategory category = categories[i];
            double weight = weights[i];
            if (weight <= 0) continue;
            chosen = category;
            roll -= weight;
            if (roll < 0) break;
        }
        int cap = maxCopiesOf(chosen);
        Element element = availableAt(chosen, cap, false, random.nextInt(availableCount(chosen, cap, false)));
        state.setElementCount(element.number(), state.elementCount(element.number()) + 1);
        return element;
    }

    // ------------------------------------------------------------------
    // Les ensembles
    // ------------------------------------------------------------------

    /** Tous les ensembles du tableau périodique : les familles, puis les périodes. */
    public List<ElementSet> elementSets() {
        return ElementSets.DEFAULT;
    }

    /** Nombre d'éléments de cet ensemble que le joueur possède. */
    public int setProgress(ElementSet set) {
        return set.progress(state.elements());
    }

    /** Vrai si le joueur possède tous les éléments de cet ensemble : son bonus est actif. */
    public boolean isSetComplete(ElementSet set) {
        return set.isComplete(state.elements());
    }

    /**
     * Force du bonus de cet ensemble en ce moment : 0, {@link ElementSet#HALF_STRENGTH} à partir
     * de la moitié réunie, 1 quand il est complet.
     */
    public double setStrength(ElementSet set) {
        return set.strength(state.elements());
    }

    /** Nombre d'ensembles complets. */
    public int completedSets() {
        return bonuses().completedSets();
    }

    /** Nombre d'ensembles à moitié réunis, sans être complets. */
    public int halfSets() {
        return bonuses().halfSets();
    }

    /** Les familles, dans l'ordre de l'énumération, chacune par numéro atomique croissant : lues à chaque tirage. */
    private static final List<List<Element>> FAMILIES = java.util.Arrays.stream(ElementCategory.values())
            .map(PeriodicTable::elements).toList();

    /**
     * Le maximum d'exemplaires d'un élément, comme {@link #maxCopiesOf(Element)}, quand celui de sa famille
     * ({@code cap}) est déjà connu et que les bonus des molécules sont à jour : la lecture d'une case du tableau.
     */
    private int capOf(Element element, int cap) {
        return element.category().unique() ? 1 : cap + elementUncaps[element.number()];
    }

    /**
     * Combien d'éléments d'une famille peuvent encore sortir (ceux qui n'ont pas atteint leur maximum d'exemplaires),
     * ou, avec {@code unknownOnly}, combien le joueur n'en possède pas encore. Sans construire de liste : la synthèse
     * automatique tire des milliers de fois par heure de jeu (version 0.170).
     */
    private int availableCount(ElementCategory category, int cap, boolean unknownOnly) {
        refreshMoleculeBonuses();
        int count = 0;
        for (Element element : FAMILIES.get(category.ordinal())) {
            int owned = state.elementCount(element.number());
            if (unknownOnly ? owned == 0 : owned < capOf(element, cap)) count++;
        }
        return count;
    }

    /** Le {@code index}-ième (depuis 0) des éléments que compte {@link #availableCount}, par numéro atomique croissant. */
    private Element availableAt(ElementCategory category, int cap, boolean unknownOnly, int index) {
        refreshMoleculeBonuses();
        for (Element element : FAMILIES.get(category.ordinal())) {
            int owned = state.elementCount(element.number());
            if (unknownOnly ? owned == 0 : owned < capOf(element, cap)) {
                if (index == 0) return element;
                index--;
            }
        }
        throw new IllegalStateException("Plus d'élément disponible dans la famille " + category);
    }

    /** Vrai si un élément de la famille peut encore sortir : son nombre d'exemplaires est sous son maximum. */
    private boolean anyAvailable(ElementCategory category) {
        refreshMoleculeBonuses();
        int cap = maxCopiesOf(category);
        for (Element element : FAMILIES.get(category.ordinal())) {
            if (state.elementCount(element.number()) < capOf(element, cap)) return true;
        }
        return false;
    }

    /** Poids d'une famille dans le tirage : sa chance de base, augmentée si elle est rare, nulle si elle est épuisée. */
    private double drawWeight(ElementCategory category) {
        if (!anyAvailable(category)) return 0;
        return category.chance() * (category.rare() ? bonuses().luckMultiplier() : 1) * preferenceMultiplier(category);
    }

    /** Probabilité actuelle qu'une synthèse tombe dans cette famille, chance et familles épuisées comprises. */
    public double categoryChance(ElementCategory category) {
        double total = 0;
        for (ElementCategory each : ElementCategory.values()) total += drawWeight(each);
        return total > 0 ? drawWeight(category) / total : 0;   // tableau complet : plus rien ne sort
    }

    /** Chance qu'une synthèse donne un second élément, entre 0 et 1. */
    public double doubleDrawChance() {
        return bonuses().doubleDrawChance();
    }

    /** Nombre d'exemplaires possédés d'un élément. */
    public int elementCount(int atomicNumber) {
        return state.elementCount(PeriodicTable.element(atomicNumber).number());
    }

    /** Nombre d'éléments différents possédés, sur 118. */
    public int discoveredElements() {
        return state.elements().size();
    }

    /** Le cumul des effets des éléments possédés, recalculé seulement quand la collection change. */
    ElementBonuses bonuses() {
        if (ruled(Challenge.Rule.INERT_ELEMENTS)) return NO_BONUSES;
        double strength = rewarded(Challenge.Reward.STRONGER_ELEMENTS) ? REWARD_ELEMENT_STRENGTH : 1;
        if (elementBonusesVersion != state.elementsVersion() || elementBonusesStrength != strength) {
            elementBonuses = ElementBonuses.of(state.elementCounts, strength);
            elementBonusesVersion = state.elementsVersion();
            elementBonusesStrength = strength;
        }
        return elementBonuses;
    }

    /**
     * Ce que les éléments apportent à une grandeur, synergies comprises : le nombre par lequel
     * elle est multipliée (1 sans élément).
     */
    public double elementMultiplier(ElementEffect.Stat stat) {
        double synergy = 0;
        for (ElementBonuses.SynergyTerm term : bonuses().synergies()) {
            if (term.stat() == stat) synergy += term.perUnit() * synergyUnits(term.source());
        }
        // Borné (version 0.117) : avec les récompenses des sondes ×1 000 et plus, les exemplaires d'éléments peuvent pousser
        // ce produit au-delà de ce qu'un double sait écrire.
        double value = bonuses().stat(stat) * (1 + synergy);
        return Double.isNaN(value) ? 1 : Math.min(value, ELEMENT_MULTIPLIER_CAP);
    }

    /** Le plus grand multiplicateur que les éléments peuvent donner : de quoi rester calculable. */
    public static final double ELEMENT_MULTIPLIER_CAP = 1e300;

    /** Ce qu'une synergie compte en ce moment. */
    private double synergyUnits(ElementEffect.SynergySource source) {
        return switch (source) {
            case DISTINCT_ELEMENTS -> state.elements().size();
            case GENERATORS -> generatorCount();
            case AVAILABLE_ATOMS -> Math.floor(state.atoms().min(MAX_ATOMS).toDouble() + 1e-9);
            case AUTOMATIONS -> state.ownedAutomations().size();
            case SPEED_LEVELS -> {
                int levels = 0;
                for (Upgrade upgrade : upgrades.values()) {
                    if (upgrade.effect() instanceof Effect.MultiplySpeed) levels += state.levelOf(upgrade.id());
                }
                yield levels;
            }
        };
    }

    /** Ce que les éléments ajoutent à la vitesse d'un générateur précis (numéroté à partir de 0). */
    public double generatorSpeedMultiplier(int generator) {
        return capped(bonuses().generatorSpeed(generator + 1));
    }

    /** Ce que les éléments ajoutent aux particules d'un générateur précis (numéroté à partir de 0). */
    public double generatorParticlesMultiplier(int generator) {
        return capped(bonuses().generatorParticles(generator + 1));
    }

    /** Un bonus propre à un générateur, borné (version 0.117) : le produit de deux reste un double. */
    private static double capped(double value) {
        return Double.isNaN(value) ? 1 : Math.min(value, 1e150);
    }

    /** Diviseur qu'appliquent les éléments à un prix (1 sans élément). */
    public double elementCostDivisor(ElementEffect.CostTarget target) {
        return bonuses().costDivisor(target);
    }

    // ------------------------------------------------------------------
    // Les succès
    // ------------------------------------------------------------------

    /** Tous les succès du catalogue, dans son ordre. Vide pour une partie créée sans catalogue de succès. */
    public Collection<Achievement> achievements() {
        return achievements.values();
    }

    /** Vrai si ce succès est obtenu. */
    public boolean hasAchievement(String achievementId) {
        return state.achievements().contains(achievementId);
    }

    /** Nombre de succès obtenus parmi ceux du catalogue. */
    public int achievementCount() {
        // Lu à chaque instant par plusieurs bonus : le compte n'est refait que si les succès ont changé.
        if (achievementCountVersion != state.achievementsVersion()) {
            int count = 0;
            for (String id : state.achievements()) {
                if (achievements.containsKey(id)) count++;
            }
            achievementCountCache = count;
            achievementCountVersion = state.achievementsVersion();
        }
        return achievementCountCache;
    }

    private int achievementCountCache = 0;
    private int achievementCountVersion = -1;

    /**
     * Les succès obtenus depuis le dernier appel, dans l'ordre : l'interface s'en sert pour les
     * annoncer. La liste est vidée à chaque lecture.
     */
    public List<Achievement> takeNewAchievements() {
        List<Achievement> taken = List.copyOf(newAchievements);
        newAchievements.clear();
        return taken;
    }

    /** Ce que tous les succès obtenus multiplient dans les particules : le bonus commun, puis les bonus propres. */
    public BigNum achievementParticlesMultiplier() {
        int count = achievementCount();
        if (count == 0) return BigNum.ONE;
        return BigNum.of((1 + ACHIEVEMENT_PARTICLES * count) * (1 + achievementBonus(Achievement.Bonus.PARTICLES)));
    }

    /** Somme des bonus propres d'une sorte, pour les succès obtenus (0.02 = 2 %). */
    public double achievementBonus(Achievement.Bonus bonus) {
        if (achievements.isEmpty()) return 0;
        int count = state.achievements().size();
        if (achievementBonusesFor != count) {
            achievementBonuses.clear();
            for (String id : state.achievements()) {
                Achievement achievement = achievements.get(id);
                if (achievement != null && achievement.hasBonus()) {
                    achievementBonuses.merge(achievement.bonus(), achievement.amount(), Double::sum);
                }
            }
            achievementBonusesFor = count;
        }
        return achievementBonuses.getOrDefault(bonus, 0.0);
    }

    /** Accorde un succès du catalogue, s'il existe et n'est pas déjà obtenu. */
    void award(String achievementId) {
        Achievement achievement = achievements.get(achievementId);
        if (achievement != null && state.addAchievement(achievementId)) newAchievements.add(achievement);
    }

    /** Accorde les succès dont la condition est remplie. */
    void checkAchievements() {
        if (achievements.isEmpty() || !state.started()) return;
        for (Achievement achievement : achievements.values()) {
            if (achievement.condition() != null && !state.achievements().contains(achievement.id())
                    && achievement.condition().test(self())) {
                award(achievement.id());
            }
        }
    }

    /** Les succès qui se jouent au moment d'une fusion, avant qu'elle ne remette la partie à zéro. */
    void noteFusionFeats() {
        if (achievements.isEmpty()) return;
        boolean bare = startingSpeedLevels() == 0;
        for (Upgrade upgrade : upgrades.values()) {
            boolean generator = upgrade.effect() instanceof Effect.AddGenerator;
            if (upgrade.resource() == Resource.PARTICLES && !generator && state.levelOf(upgrade.id()) > 0) bare = false;
        }
        if (bare) award(Achievements.BARE_FUSION);
        if (state.timeSinceFusion() >= Achievements.PATIENT_SECONDS) award(Achievements.PATIENT_FUSION);
        // Un sprint se mesure d'une fusion à l'autre : la première de la partie n'en est pas un.
        if (state.stats().runFusions() >= 1 && state.timeSinceFusion() < Achievements.FAST_FUSION_SECONDS) {
            award(Achievements.FAST_FUSION);
        }
        if (fusionGroups() >= Achievements.MASS_FUSION_GROUPS) award(Achievements.MASS_FUSION);
    }

    // ------------------------------------------------------------------
    // Les records : le meilleur temps de chaque étape, d'une partie à l'autre
    // ------------------------------------------------------------------

    final List<GameStats.Step> newRecords = new ArrayList<>();

    /** Note qu'une étape vient d'être atteinte ; la première fois de la partie, son temps est comparé au record. */
    void noteStep(GameStats.Step step) {
        boolean first = !state.stats().reachedThisUniverse(step);
        state.stats().noteStep(step, state.timePlayed());
        if (first) noteRecord(step);
    }

    /**
     * Temps de jeu de la partie en cours, en secondes : depuis le début du jeu pour la première, depuis le début
     * de la nouvelle partie ensuite (version 0.126). {@code NaN} si ce début n'est pas connu (sauvegarde ancienne).
     */
    public double runTimePlayed() {
        return state.timePlayed() - state.stats().universeStart();
    }

    /** Retient le temps de la partie comme record de cette étape s'il bat le précédent, ou s'il n'y en avait pas. */
    void noteRecord(GameStats.Step step) {
        double time = runTimePlayed();
        if (Double.isNaN(time)) return;
        Double best = state.records().get(step.name());
        if (best == null || time < best) {
            // Un premier record n'est pas une nouvelle : il n'y avait rien à battre.
            if (best != null) newRecords.add(step);
            state.setRecord(step.name(), time);
        }
    }

    /**
     * Le record d'une étape : le temps de partie le plus court auquel elle a été atteinte, toutes
     * parties confondues, en secondes. {@code NaN} si elle ne l'a jamais été. La remise à zéro du
     * jeu garde les records : ce sont eux qu'on cherche à battre à la partie suivante.
     */
    public double recordOf(GameStats.Step step) {
        return state.records().getOrDefault(step.name(), Double.NaN);
    }

    /** La prochaine étape datée que cette partie n'a pas encore atteinte, ou {@code null} quand elles le sont toutes. */
    public GameStats.Step nextStep() {
        // Une sauvegarde d'avant la 0.126, au-delà de la première partie, n'a pas daté le début de celle-ci : on ne sait
        // pas ce qu'elle a déjà atteint. Rien n'est annoncé jusqu'à la partie suivante.
        if (Double.isNaN(state.stats().universeStart())) return null;
        for (GameStats.Step step : GameStats.Step.values()) {
            if (state.stats().reachedThisUniverse(step)) continue;
            // Une sauvegarde d'avant ces deux étapes les a passées sans les dater : elles ne sont plus « à venir ».
            if (step == GameStats.Step.FIRST_FUSION && state.stats().fusions() > state.fusionsAtUniverse()) continue;
            if (step == GameStats.Step.FIRST_EXPLOSION && state.stats().timedExplosions() > state.explosionsAtUniverse()) continue;
            return step;
        }
        return null;
    }

    /**
     * L'écart au record sur la prochaine étape, en secondes : négatif tant que le joueur est en
     * avance sur son record, positif une fois qu'il l'a dépassé. {@code NaN} quand il n'y a pas de
     * prochaine étape, ou pas encore de record pour elle.
     */
    public double recordGap() {
        GameStats.Step next = nextStep();
        if (next == null) return Double.NaN;
        double record = recordOf(next);
        return Double.isNaN(record) ? Double.NaN : runTimePlayed() - record;
    }

    /** Oublie tous les records. */
    public void clearRecords() {
        state.clearRecords();
    }

    /** Les étapes dont le record vient d'être battu depuis le dernier appel : pour l'annoncer. Vidé à chaque lecture. */
    public List<GameStats.Step> takeNewRecords() {
        List<GameStats.Step> taken = new ArrayList<>(newRecords);
        newRecords.clear();
        return taken;
    }
}

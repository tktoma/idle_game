package idle.core;

import idle.core.state.*;
import idle.core.universe.CycleChoice;
import idle.core.achievements.*;
import idle.core.darkmatter.*;
import idle.core.elements.*;
import idle.core.math.BigNum;
import idle.core.upgrades.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Une partie de {@link GameAtoms} (découpée en 0.293 pour rester sous 1 000 lignes) : l'automatisation et l'achat en lot. */
public abstract class GameAutomationBuy extends GamePlans {
    // Ce que fournissent les parties suivantes.
    abstract ElementBonuses bonuses();
    abstract public BigNum atomCap();
    abstract public BigNum synthesisCost();
    abstract public List<Element> synthesize();
    abstract public boolean isPeriodicTableComplete();
    abstract public double achievementBonus(Achievement.Bonus bonus);

    GameAutomationBuy(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    public abstract boolean isAutoHoldUnlocked();

    public abstract boolean isMoleculeAutomationUnlocked();

    public abstract boolean isAutoGatherUnlocked();

    public abstract boolean isAutoFormUnlocked();

    public abstract CycleChoice cycleChoice();

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
    public Collection<Automation> automations() { return automations.values(); }

    /**
     * Vrai une fois {@link #UNLOCK_TOTAL_ATOMS} atomes créés depuis le début de la partie : les
     * automatismes deviennent achetables.
     */
    public boolean isAutomationUnlocked() {
        // Des automatismes conservés après une explosion restent utilisables tout de suite.
        return hasCreatedEnoughAtoms() || state.ownedAutomationCount() > 0;
    }

    /** Vrai une fois {@link #UNLOCK_TOTAL_ATOMS} atomes créés depuis le début de la partie. */
    boolean hasCreatedEnoughAtoms() { return state.totalAtoms().gte(UNLOCK_TOTAL_ATOMS); }

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
    public boolean ownsAutomation(String automationId) { return state.ownsAutomation(automation(automationId).id()); }

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
     * Vrai tant que le joueur a coupé d'un geste tous les automatismes : ceux de l'onglet Automatisation, ceux de
     * matière noire, l'appui automatique et la création automatique. Aucun n'agit, mais chacun garde son propre réglage
     * et reprend quand la coupure est levée. Le verrou de l'appui n'est pas un automatisme : il continue.
     */
    public boolean isAutomationPaused() { return state.automationPaused(); }

    /** Coupe ou relance d'un geste tous les automatismes. Aucune remise à zéro ne change ce réglage. */
    public void setAutomationPaused(boolean paused) { state.setAutomationPaused(paused); }

    /** Vrai si le joueur a au moins un automatisme, de quelque sorte que ce soit : il y a alors quelque chose à couper. */
    public boolean hasAnyAutomation() {
        if (state.ownedAutomationCount() > 0 || isAutoHoldUnlocked() || isMoleculeAutomationUnlocked()
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
     * Vrai quand les automatismes agissent sans délai et achètent tout ce qu'ils peuvent (bulle du
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
    // L'achat en lot
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
    public int automationBatch(String automationId) { return 1 << automationBatchLevel(automationId); }

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
     * Double ce qu'une action de cet automatisme achète, payé en matière noire : elle est dépensée, mais
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
    ElementEffect.AutomationTarget target(Automation automation) {
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
                // La bulle du bonsaï, à droite : sans délai, autant d'actions qu'il y a à faire.
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
    boolean act(Automation automation) {
        boolean was = automating;
        automating = true;
        try {
            return actNow(automation);
        } finally {
            automating = was;
        }
    }

    boolean actNow(Automation automation) {
        return switch (automation.kind()) {
            case UPGRADE -> bulkBuy(automation);
            case FUSION -> generatorCount() >= fusionThreshold() && fuse();
            // La réserve du joueur : la synthèse automatique n'entame pas les atomes mis de côté.
            case SYNTHESIS -> state.atoms().gte(synthesisCost().add(state.synthesisReserve())) && paySynthesis();
        };
    }

    /**
     * L'achat d'un automatisme : un niveau par action, générateurs compris (avant, celui des générateurs
     * prenait d'un coup tous ceux que les particules permettaient), ou le lot que la matière noire a payé pour lui
     * ({@link #automationBatch(String)}, jusqu'à 64). La cadence fixe donc le rythme des fusions : neuf achats de
     * générateurs avant chacune, un par action sans lot. La bulle du bonsaï, à droite, rend l'achat « max » : sans délai,
     * celui des générateurs prend d'un coup tous ceux que les particules permettent ({@link #buysInBulk(Automation)}).
     */
    boolean bulkBuy(Automation automation) {
        int most = buysInBulk(automation) ? MAX_GENERATORS_PER_ACTION : automationBatch(automation.id());
        Upgrade upgrade = upgrades.get(automation.upgradeId());
        if (most <= 1) return buy(upgrade.id());
        if (!(upgrade.effect() instanceof Effect.AddGenerator) || upgrade.resource() != Resource.PARTICLES || !state.started()) {
            boolean any = false;
            for (int bought = 0; bought < most && buy(upgrade.id()); bought++) any = true;
            return any;
        }
        // Les mêmes règles qu'un achat un à un (l'épreuve du premier générateur seul, entre autres),
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
    static final int MAX_GENERATORS_PER_ACTION = 1_000;

    /**
     * Fait une synthèse et dit si elle a eu lieu : une étape de synthèse ciblée ne donne aucun
     * élément, mais c'est bien une action, payée comme les autres.
     */
    boolean paySynthesis() {
        int before = state.synthesisCount();
        synthesize();
        return state.synthesisCount() > before;
    }

    /** Atomes que la synthèse automatique laisse toujours au joueur (0 par défaut). */
    public BigNum synthesisReserve() { return state.synthesisReserve(); }

    /**
     * Règle la réserve : la synthèse automatique n'agit que s'il reste au moins {@code atoms} atomes après elle. Les
     * synthèses faites à la main n'en tiennent pas compte. Une valeur négative est ramenée à zéro.
     */
    public void setSynthesisReserve(BigNum atoms) { state.setSynthesisReserve(atoms.max(BigNum.ZERO)); }

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
        // Un défi se corse d'un facteur par univers formé ({@link #challengeUniverseFactor()}).
        if (ruled(Challenge.Rule.PRESENCE_ONLY) || ruled(Challenge.Rule.ABSENCE_ONLY)) return STYLE_CHALLENGE_WEIGHT.multiply(challengeUniverseFactor());
        return inChallenge() ? CHALLENGE_WEIGHT.multiply(challengeUniverseFactor())
                : BigNum.of(TABLE_WEIGHT_GROWTH).pow(Math.min(state.tableWeightLevel(), TABLE_WEIGHT_MAX_LEVEL));
    }

    /** Nombre total d'exemplaires du tableau de base, avant ce que l'arbre de matière noire y ajoute ({@link #maxTotalCopies()}). */
    public static int maxCopies() {
        int copies = 0;
        for (Element element : PeriodicTable.ELEMENTS) copies += element.category().maxCopies();
        return copies;
    }

    /**
     * Nombre de synthèses de suite sans élément nouveau au bout duquel la suivante en donne un à
     * coup sûr, tant qu'il en reste à découvrir. C'est ce qui borne la malchance : le dernier
     * élément du tableau ne se fait plus attendre des dizaines de synthèses.
     */
    public static final int LUCK_FLOOR = 2;

    /** Les familles, dans l'ordre de l'énumération, chacune par numéro atomique croissant : lues à chaque tirage. */
    static final List<List<Element>> FAMILIES = Arrays.stream(ElementCategory.values())
            .map(PeriodicTable::elements).toList();

    /** Le plus grand multiplicateur que les éléments peuvent donner : de quoi rester calculable. */
    public static final double ELEMENT_MULTIPLIER_CAP = 1e300;

    /** Un bonus propre à un générateur, borné : le produit de deux reste un double. */
    static double capped(double value) { return Double.isNaN(value) ? 1 : Math.min(value, 1e150); }

    int achievementCountCache = 0;

    int achievementCountVersion = -1;

    // ------------------------------------------------------------------
    // Les records : le meilleur temps de chaque étape, d'une partie à l'autre
    // ------------------------------------------------------------------

    final List<GameStats.Step> newRecords = new ArrayList<>();
}

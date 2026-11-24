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

/** Les atomes : l'automatisation, le tableau périodique, les ensembles, les succès et les records. */
public abstract class GameAtoms extends GameAutomationBuy {
    GameAtoms(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /**
     * Nombre maximal d'atomes qu'on peut posséder en même temps, et prix maximal d'une synthèse :
     * {@link #MAX_ATOMS} fois la masse du tableau. Chaque explosion laisse donc un tableau plus
     * lourd à remplir : il faut produire plus que la fois d'avant pour exploser de nouveau.
     */
    public BigNum atomCap() { return Upgrade.roundUp(MAX_ATOMS.multiply(tableWeight())); }

    /**
     * Prix de la prochaine synthèse, en atomes : {@link #SYNTHESIS_BASE_COST}, doublé à chaque synthèse déjà faite,
     * plafonné par {@link #atomCap()}, puis réduit par les éléments qui baissent ce prix.
     */
    public BigNum synthesisCost() {
        BigNum cost = SYNTHESIS_BASE_COST.multiply(BigNum.of(2).pow(state.synthesisCount())).min(atomCap());
        double divisor = bonuses().costDivisor(ElementEffect.CostTarget.SYNTHESIS) * darkSynthesisDivisor()
                * (1 + achievementBonus(Achievement.Bonus.SYNTHESIS_COST))
                / (cycleChoice() == CycleChoice.SYNTHESIS ? 1 - CycleChoice.SYNTHESIS_SAVING : 1);
        return divisor == 1 ? cost : Upgrade.roundUp(cost.divide(divisor)).max(BigNum.ONE);
    }

    /**
     * Vrai une fois {@link #UNLOCK_TOTAL_ATOMS} atomes créés depuis le début de la partie : le
     * tableau périodique devient accessible, en même temps que l'automatisation. Le compte des
     * atomes créés ne redescend qu'à l'explosion : d'ici là, le tableau reste débloqué.
     */
    public boolean isPeriodicTableUnlocked() { return hasCreatedEnoughAtoms(); }

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

    /**
     * Consomme {@link #synthesisCost()} atomes pour créer un élément du tableau périodique tiré au sort : d'abord la
     * famille, selon {@link #categoryChance(ElementCategory)}, puis un élément de cette famille. Un élément déjà
     * possédé gagne un exemplaire ; un élément qui a atteint son maximum d'exemplaires (un seul pour un élément unique)
     * ne peut pas retomber. Avec de la chance ({@link #doubleDrawChance()}), un second élément est tiré, s'il en reste
     * à obtenir. Si aucun élément unique n'est sorti avant, la synthèse n° {@link #guaranteedUniqueSynthesis()} en
     * donne un à coup sûr. Rien d'autre n'est perdu : améliorations et automatismes sont conservés.
     *
     * <p>Avec une famille visée ({@link #setSynthesisTarget(ElementCategory)}), la synthèse est ciblée : elle coûte
     * plusieurs synthèses ({@link ElementCategory#targetTries()}). Les premières sont payées comme les autres, font
     * monter le prix comme les autres, mais ne donnent rien ; la dernière donne un élément de la famille, en priorité
     * un que le joueur n'a pas encore.
     *
     * @return les éléments obtenus (un, parfois deux, davantage avec la matière noire), ou une liste vide si la
     * synthèse est impossible (tableau verrouillé, tableau complet ou pas assez d'atomes) ou si elle n'était qu'une
     * étape d'une synthèse ciblée
     */
    public List<Element> synthesize() {
        if (!canSynthesize()) return List.of();
        BigNum cost = synthesisCost();
        state.setAtoms(state.atoms().subtract(cost).max(BigNum.ZERO));
        state.stats().addAtomsSpent(cost);
        state.setSynthesisCount(state.synthesisCount() + 1);

        double doubleChance = doubleDrawChance();   // la chance d'avant le tirage, pas celle qu'il donnerait
        boolean guaranteed = !isSynthesisAutomationUnlocked() && state.synthesisCount() >= guaranteedUniqueSynthesis();
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
        // La préférence graduée : chaque tirage au hasard la renforce un peu.
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
     * Famille visée par la synthèse ciblée, ou {@code null} si la synthèse tire au hasard. Une famille dont tous les
     * éléments sont à leur maximum d'exemplaires ne peut plus être visée : la cible s'efface d'elle-même.
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
     * Vise une famille pour les prochaines synthèses, à la main comme automatiques, ou revient au hasard avec {@code
     * null}. Les synthèses déjà payées pour une cible restent acquises si la cible change : elles comptent pour la
     * suivante.
     *
     * @return {@code true} si la cible a été retenue ; faux si la synthèse ciblée n'est pas débloquée ({@link
     * #isTargetedSynthesisUnlocked()}), si le tableau périodique est verrouillé ou si la famille n'a plus rien à donner
     */
    public boolean setSynthesisTarget(ElementCategory category) {
        if (category != null && (!isTargetedSynthesisUnlocked() || !isPeriodicTableUnlocked()
                || !anyAvailable(category))) return false;
        state.setSynthesisTarget(category);
        return true;
    }

    /** Vrai une fois la synthèse ciblée achetée avec de la matière noire : d'ici là, la synthèse tire toujours au hasard. */
    public boolean isTargetedSynthesisUnlocked() { return hasDark(DarkEffect.TargetedSynthesis.class); }

    /** Vrai une fois acquis l'achat groupé des améliorations payées en atomes : par dix, ou tout ce qui est à portée. */
    public boolean isBulkAtomBuyUnlocked() {
        if (hasDark(DarkEffect.BulkAtomUpgrades.class)) return true;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.BulkAtomBuy && state.levelOf(upgrade.id()) > 0) return true;
        }
        return false;
    }

    /** Synthèses déjà payées pour la synthèse ciblée en cours. */
    public int synthesisTries() { return state.synthesisTries(); }

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
    Element drawTargeted(ElementCategory target) {
        // Le même tirage, sans construire les listes (la synthèse en fait des milliers par heure de jeu).
        int cap = maxCopiesOf(target);
        int unknown = availableCount(target, cap, true);
        boolean among = unknown > 0;
        int size = among ? unknown : availableCount(target, cap, false);
        Element element = availableAt(target, cap, among, random.nextInt(size));
        state.setElementCount(element.number(), state.elementCount(element.number()) + 1);
        return element;
    }

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
    Element drawUnknown() {
        // Les poids et les comptes se calculent une fois par famille, sans construire de listes ;
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
    Element drawElement(boolean uniqueOnly) {
        // Chaque poids se calcule une fois par tirage (deux avant), pour la synthèse sans délai.
        ElementCategory[] categories = ElementCategory.values();
        double[] weights = new double[categories.length];
        double total = 0;
        // La famille préférée se lit une fois par tirage (0.298 ; avant, une fois par famille).
        ElementCategory preferred = preferredFamily();
        for (int i = 0; i < categories.length; i++) {
            if (!uniqueOnly || categories[i].unique()) {
                weights[i] = drawWeight(categories[i], preferred);
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
    public List<ElementSet> elementSets() { return ElementSets.DEFAULT; }

    /** Nombre d'éléments de cet ensemble que le joueur possède. */
    public int setProgress(ElementSet set) { return set.progress(state.elements()); }

    /** Vrai si le joueur possède tous les éléments de cet ensemble : son bonus est actif. */
    public boolean isSetComplete(ElementSet set) { return set.isComplete(state.elements()); }

    /**
     * Force du bonus de cet ensemble en ce moment : 0, {@link ElementSet#HALF_STRENGTH} à partir
     * de la moitié réunie, 1 quand il est complet.
     */
    public double setStrength(ElementSet set) { return set.strength(state.elements()); }

    /** Nombre d'ensembles complets. */
    public int completedSets() { return bonuses().completedSets(); }

    /** Nombre d'ensembles à moitié réunis, sans être complets. */
    public int halfSets() { return bonuses().halfSets(); }

    /**
     * Le maximum d'exemplaires d'un élément, comme {@link #maxCopiesOf(Element)}, quand celui de sa famille
     * ({@code cap}) est déjà connu et que les bonus des molécules sont à jour : la lecture d'une case du tableau.
     */
    int capOf(Element element, int cap) {
        return element.category().unique() ? 1 : cap + elementUncaps[element.number()];
    }

    /**
     * Combien d'éléments d'une famille peuvent encore sortir (ceux qui n'ont pas atteint leur maximum d'exemplaires),
     * ou, avec {@code unknownOnly}, combien le joueur n'en possède pas encore. Sans construire de liste : la synthèse
     * automatique tire des milliers de fois par heure de jeu.
     */
    int availableCount(ElementCategory category, int cap, boolean unknownOnly) {
        refreshMoleculeBonuses();
        int count = 0;
        for (Element element : FAMILIES.get(category.ordinal())) {
            int owned = state.elementCount(element.number());
            if (unknownOnly ? owned == 0 : owned < capOf(element, cap)) count++;
        }
        return count;
    }

    /** Le {@code index}-ième (depuis 0) des éléments que compte {@link #availableCount}, par numéro atomique croissant. */
    Element availableAt(ElementCategory category, int cap, boolean unknownOnly, int index) {
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
    boolean anyAvailable(ElementCategory category) {
        refreshMoleculeBonuses();
        // Le même test que capOf, sans passer par chaque élément : une famille unique plafonne à 1 (0.298).
        boolean unique = category.unique();
        int cap = unique ? 1 : maxCopiesOf(category);
        for (int number : FAMILY_NUMBERS[category.ordinal()]) {
            if (state.elementCount(number) < (unique ? 1 : cap + elementUncaps[number])) return true;
        }
        return false;
    }

    /** Poids d'une famille dans le tirage : sa chance de base, augmentée si elle est rare, nulle si elle est épuisée. */
    double drawWeight(ElementCategory category) { return drawWeight(category, preferredFamily()); }

    /** Le même poids, la famille préférée ({@code null} : aucune) déjà lue. */
    double drawWeight(ElementCategory category, ElementCategory preferred) {
        if (!anyAvailable(category)) return 0;
        return category.chance() * (category.rare() ? luckAndDoubleDraw()[0] : 1) * preferenceMultiplier(category, preferred);
    }

    /** Probabilité actuelle qu'une synthèse tombe dans cette famille, chance et familles épuisées comprises. */
    public double categoryChance(ElementCategory category) {
        double total = 0;
        for (ElementCategory each : ElementCategory.values()) total += drawWeight(each);
        return total > 0 ? drawWeight(category) / total : 0;   // tableau complet : plus rien ne sort
    }

    /** Chance qu'une synthèse donne un second élément, entre 0 et 1. */
    public double doubleDrawChance() { return Math.min(1, luckAndDoubleDraw()[1]); }

    /**
     * {@code {1 + chance des familles rares, double tirage non borné}} : les mêmes nombres que
     * {@link #bonuses()}, sans recalculer les autres bonus à chaque tirage.
     */
    double[] luckAndDoubleDraw() {
        if (ruled(Challenge.Rule.INERT_ELEMENTS)) return new double[] {NO_BONUSES.luckMultiplier(), NO_BONUSES.doubleDrawChance()};
        double strength = rewarded(Challenge.Reward.STRONGER_ELEMENTS) ? REWARD_ELEMENT_STRENGTH : 1;
        if (elementLuckVersion != state.elementsVersion() || elementLuckStrength != strength) {
            elementLuck = ElementBonuses.luckAndDoubleDraw(state.elementCounts, strength);
            elementLuckVersion = state.elementsVersion();
            elementLuckStrength = strength;
        }
        return elementLuck;
    }

    /** Nombre d'exemplaires possédés d'un élément. */
    public int elementCount(int atomicNumber) {
        return state.elementCount(PeriodicTable.element(atomicNumber).number());
    }

    /** Nombre d'éléments différents possédés, sur 118. */
    public int discoveredElements() { return state.elements().size(); }

    /** Nombre de familles dont le joueur possède au moins un élément, sur dix (version 0.192 : le skin « Spectre »). */
    public int familiesOwned() {
        int owned = 0;
        for (List<Element> family : FAMILIES) {
            for (Element element : family) {
                if (state.elementCount(element.number()) > 0) {
                    owned++;
                    break;
                }
            }
        }
        return owned;
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
        // Borné : avec les récompenses des sondes ×1 000 et plus, les exemplaires d'éléments peuvent pousser
        // ce produit au-delà de ce qu'un double sait écrire.
        double value = bonuses().stat(stat) * (1 + synergy);
        return Double.isNaN(value) ? 1 : Math.min(value, ELEMENT_MULTIPLIER_CAP);
    }

    /** Ce qu'une synergie compte en ce moment. */
    double synergyUnits(ElementEffect.SynergySource source) {
        return switch (source) {
            case DISTINCT_ELEMENTS -> state.elements().size();
            case GENERATORS -> generatorCount();
            case AVAILABLE_ATOMS -> Math.floor(state.atoms().min(MAX_ATOMS).toDouble() + 1e-9);
            case AUTOMATIONS -> state.ownedAutomationCount();
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
    public double generatorSpeedMultiplier(int generator) { return capped(bonuses().generatorSpeed(generator + 1)); }

    /** Ce que les éléments ajoutent aux particules d'un générateur précis (numéroté à partir de 0). */
    public double generatorParticlesMultiplier(int generator) {
        return capped(bonuses().generatorParticles(generator + 1));
    }

    /** Diviseur qu'appliquent les éléments à un prix (1 sans élément). */
    public double elementCostDivisor(ElementEffect.CostTarget target) { return bonuses().costDivisor(target); }

    // ------------------------------------------------------------------
    // Les succès
    // ------------------------------------------------------------------

    /** Tous les succès du catalogue, dans son ordre. Vide pour une partie créée sans catalogue de succès. */
    public Collection<Achievement> achievements() { return achievements.values(); }

    /** Vrai si ce succès est obtenu. */
    public boolean hasAchievement(String achievementId) { return state.achievements().contains(achievementId); }

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

    /** Note qu'une étape vient d'être atteinte ; la première fois de la partie, son temps est comparé au record. */
    void noteStep(GameStats.Step step) {
        boolean first = !state.stats().reachedThisUniverse(step);
        state.stats().noteStep(step, state.timePlayed());
        if (first) noteRecord(step);
    }

    /**
     * Temps de jeu de la partie en cours, en secondes : depuis le début du jeu pour la première, depuis le début
     * de la nouvelle partie ensuite. {@code NaN} si ce début n'est pas connu (sauvegarde ancienne).
     */
    public double runTimePlayed() { return state.timePlayed() - state.stats().universeStart(); }

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
    public double recordOf(GameStats.Step step) { return state.records().getOrDefault(step.name(), Double.NaN); }

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
    public void clearRecords() { state.clearRecords(); }

    /** Les étapes dont le record vient d'être battu depuis le dernier appel : pour l'annoncer. Vidé à chaque lecture. */
    public List<GameStats.Step> takeNewRecords() {
        List<GameStats.Step> taken = new ArrayList<>(newRecords);
        newRecords.clear();
        return taken;
    }
}

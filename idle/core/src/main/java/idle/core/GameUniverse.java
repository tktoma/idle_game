package idle.core;

import idle.core.achievements.Achievement;
import idle.core.collider.ColliderUpgrade;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.ElementCategory;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Cosmos;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Les univers : la partie secondaire, le deuxième univers et les skins à obtenir.
 */
public abstract class GameUniverse extends GameAtoms {

    GameUniverse(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    abstract void forgetDerived();
    public abstract BigNum darkMatterEarned();
    public abstract long gatheredInState(Molecule.State matter);
    public abstract boolean isGalaxyUnlocked();
    public abstract boolean canFormGalaxy();
    public abstract boolean keepsUpgradesOnFusion();


    // ------------------------------------------------------------------
    // Le cosmos : la galaxie, l'amas de galaxies, l'univers
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // La partie secondaire (version 0.81) : les univers, sans logique pour l'instant
    // ------------------------------------------------------------------

    /** Nombre de dimensions à ouvrir, une par univers jusqu'au dixième. */
    public static final int MAX_DIMENSIONS = 10;

    /** Univers créés : 0 avant l'univers, 1 une fois formé, plus ceux d'après (créés pour l'instant par les outils d'essai). */
    public int universes() {
        return isUniverseFormed() ? 1 + state.extraUniverses() : 0;
    }

    /** Vrai une fois un univers formé, même pendant la partie qui mène au suivant (version 0.87). */
    public boolean isUniverseFormed() {
        return state.universeFormed() || state.cosmosLevel() >= Cosmos.values().length;
    }

    /** Vrai à partir du deuxième univers : la sous-page des dimensions s'ouvre. */
    public boolean isDimensionsUnlocked() {
        return universes() >= 2;
    }

    /** Dimensions ouvertes : les deux premières dès le deuxième univers, puis une par univers, dix au plus. */
    public int dimensionsUnlocked() {
        return isDimensionsUnlocked() ? Math.min(MAX_DIMENSIONS, universes()) : 0;
    }

    /** Les améliorations de l'univers, dans l'ordre. */
    public List<idle.core.universe.UniverseUpgrade> universeUpgrades() {
        return List.of(idle.core.universe.UniverseUpgrade.values());
    }

    /** Niveau d'une amélioration de l'univers. */
    public int universeUpgradeLevel(idle.core.universe.UniverseUpgrade upgrade) {
        return state.universeUpgradeLevel(upgrade.id());
    }

    /** Outil d'essai : un univers de plus, une fois le premier formé. Faux s'il n'y en a pas encore. */
    public boolean testAddUniverse() {
        if (!isUniverseFormed()) return false;
        state.setExtraUniverses(state.extraUniverses() + 1);
        return true;
    }

    /** Outil d'essai : un univers de moins, sans descendre sous le premier. */
    public boolean testRemoveUniverse() {
        if (state.extraUniverses() <= 0) return false;
        state.setExtraUniverses(state.extraUniverses() - 1);
        return true;
    }

    /** Outil d'essai : des graines d'univers en plus. */
    public void testAddUniverseSeeds(BigNum amount) {
        state.setUniverseSeeds(state.universeSeeds().add(amount));
    }

    /** Outil d'essai : de l'énergie du vide en plus. */
    public void testAddVoidEnergy(BigNum amount) {
        state.setVoidEnergy(state.voidEnergy().add(amount));
    }

    /** Outil d'essai : un niveau de plus à une amélioration de l'univers, sans coût ni effet. */
    public void testRaiseUniverseUpgrade(idle.core.universe.UniverseUpgrade upgrade) {
        state.setUniverseUpgradeLevel(upgrade.id(), state.universeUpgradeLevel(upgrade.id()) + 1);
    }

    /** Outil d'essai : la partie secondaire revient à son début (un seul univers, rien en réserve). */
    public void testResetUniverses() {
        state.setExtraUniverses(0);
        state.setUniverseSeeds(BigNum.ZERO);
        state.setVoidEnergy(BigNum.ZERO);
        for (idle.core.universe.UniverseUpgrade upgrade : idle.core.universe.UniverseUpgrade.values()) {
            state.setUniverseUpgradeLevel(upgrade.id(), 0);
        }
    }

    /** Vrai si cette échelle du cosmos est formée. */
    public boolean hasCosmos(Cosmos scale) {
        // L'univers reste formé après la remise à zéro qui l'accompagne (version 0.83), quand le cosmos repart de rien.
        // Pendant la partie du deuxième univers (version 0.87), seul compte ce que cette partie a formé.
        if (scale == Cosmos.UNIVERSE && state.universeFormed() && !state.replaying()) return true;
        return state.cosmosLevel() > scale.ordinal();
    }

    /** Nombre d'échelles du cosmos formées : 0 avant la galaxie, 3 une fois l'univers formé. */
    public int cosmosFormed() {
        return state.cosmosLevel();
    }

    /** La prochaine échelle du cosmos à former, ou {@code null} une fois l'univers formé. */
    public Cosmos nextCosmos() {
        return state.cosmosLevel() >= Cosmos.values().length ? null : Cosmos.values()[state.cosmosLevel()];
    }

    /**
     * Vrai une fois cette échelle en vue : la galaxie à la première étoile ({@link #isGalaxyUnlocked()}),
     * les suivantes dès que la précédente est formée.
     */
    public boolean isCosmosUnlocked(Cosmos scale) {
        return scale == Cosmos.GALAXY ? isGalaxyUnlocked() : hasCosmos(scale.previous());
    }

    /**
     * Vrai si cette échelle peut être formée maintenant : la précédente l'est, elle ne l'est pas
     * encore, et ce qu'elle demande est réuni. La galaxie demande tous les astres
     * ({@link #canFormGalaxy()}) ; les suivantes, assez de matière rassemblée dans chaque état
     * ({@link Cosmos#matter()}).
     */
    public boolean canFormCosmos(Cosmos scale) {
        if (scale == Cosmos.GALAXY) return canFormGalaxy();
        if (!state.started() || hasCosmos(scale) || !hasCosmos(scale.previous())) return false;
        if (cosmosSettleRemaining(scale) > 0) return false;
        for (Map.Entry<Molecule.State, Integer> need : scale.matter().entrySet()) {
            if (gatheredInState(need.getKey()) < need.getValue()) return false;
        }
        for (Map.Entry<Molecule.State, Integer> need : scale.sorts().entrySet()) {
            if (sortsInState(need.getKey()) < need.getValue()) return false;
        }
        return true;
    }

    /**
     * Temps de jeu qu'il faut laisser à l'échelle d'avant pour se rassembler avant de former l'amas,
     * puis l'univers (version 0.73) : 45 minutes chacun. Un joueur qui réunit la matière plus vite
     * attend ; l'infatigable, qui met déjà près d'une heure à chaque échelle, ne le voit pas.
     */
    public static final double COSMOS_SETTLE_SECONDS = 45 * 60;

    /** Secondes de jeu qu'il reste avant que cette échelle puisse se former, 0 si le délai est passé. */
    public double cosmosSettleRemaining(Cosmos scale) {
        if (scale == Cosmos.GALAXY || !hasCosmos(scale.previous())) return 0;
        double since = state.stats().reachedAt(scale.previous() == Cosmos.GALAXY ? GameStats.Step.GALAXY : GameStats.Step.CLUSTER);
        if (Double.isNaN(since)) return 0;
        return Math.max(0, since + COSMOS_SETTLE_SECONDS - state.timePlayed());
    }

    private final Map<Molecule.State, Integer> sortsByState = new java.util.EnumMap<>(Molecule.State.class);
    int sortsByStateVersion = -1;

    /** Nombre de sortes de molécules rassemblées dans cet état : la variété que demandent l'amas de galaxies et l'univers ({@link Cosmos#sorts()}). */
    public int sortsInState(Molecule.State matter) {
        if (sortsByStateVersion != state.moleculesVersion()) {
            sortsByStateVersion = state.moleculesVersion();
            sortsByState.clear();
            for (String id : state.substances()) {
                Molecule molecule = molecules.get(id);
                if (molecule != null && molecule.hasState()) sortsByState.merge(molecule.state(), 1, Integer::sum);
            }
        }
        return sortsByState.getOrDefault(matter, 0);
    }

    /** Nombre de conditions de cette échelle déjà réunies, sur {@link #cosmosConditions(Cosmos)}. */
    public int cosmosConditionsMet(Cosmos scale) {
        if (scale == Cosmos.GALAXY) {
            return hasGalaxy() ? bodies.size() + 1 : bodiesMature() + (solarSystemsComplete() >= SYSTEMS_FOR_GALAXY ? 1 : 0);
        }
        int met = hasCosmos(scale.previous()) ? 1 : 0;
        for (Map.Entry<Molecule.State, Integer> need : scale.matter().entrySet()) {
            if (gatheredInState(need.getKey()) >= need.getValue()) met++;
        }
        for (Map.Entry<Molecule.State, Integer> need : scale.sorts().entrySet()) {
            if (sortsInState(need.getKey()) >= need.getValue()) met++;
        }
        return met;
    }

    /** Nombre de conditions de cette échelle : les astres du catalogue pour la galaxie ; sinon l'échelle d'avant, plus un état de la matière par ligne. */
    public int cosmosConditions(Cosmos scale) {
        return scale == Cosmos.GALAXY ? bodies.size() + 1 : 1 + scale.matter().size() + scale.sorts().size();
    }

    /**
     * Forme cette échelle du cosmos. C'est un achat unique, qui ne consomme rien : la matière reste
     * rassemblée, les astres restent formés. Elle multiplie par {@link Cosmos#factor()} chacune des
     * grandeurs que la matière augmente, et rien ne la défait, ni l'explosion ni le Big Bang.
     *
     * @return {@code true} si elle a été formée
     */
    public boolean formCosmos(Cosmos scale) {
        if (!canFormCosmos(scale)) return false;
        state.setCosmosLevel(scale.ordinal() + 1);
        noteStep(switch (scale) {
            case GALAXY -> GameStats.Step.GALAXY;
            case CLUSTER -> GameStats.Step.CLUSTER;
            case UNIVERSE -> GameStats.Step.UNIVERSE;
        });
        if (scale == Cosmos.UNIVERSE) formUniverse();
        return true;
    }

    // ------------------------------------------------------------------
    // Le deuxième univers (version 0.87)
    // ------------------------------------------------------------------

    /** Fusions du deuxième univers qu'il faut pour ouvrir l'onglet Skins. */
    public static final int SKINS_FUSIONS = 5;
    /** Générateurs qu'il faut pour le régime des générateurs. */
    public static final int REGIME_GENERATORS = 3;
    /** Ce que le régime rapide ajoute à la cadence des générateurs, et combien de temps. */
    public static final double REGIME_GAIN = 0.15;
    public static final double REGIME_SECONDS = 20;
    /** Le repos qui suit le régime rapide, avant de pouvoir le relancer. */
    public static final double REGIME_REST_SECONDS = 40;
    /** Générateurs qu'il faut pour la synergie par rang. */
    public static final int SYNERGY_GENERATORS = 10;
    /** Ce que la synergie ajoute, au plus, à chaque générateur : +0,5 %. */
    public static final double SYNERGY_MAX = 0.005;
    /** Fusions de suite sans le plafond d'atomes pour la prime de cycle parfait, sa durée et son gain. */
    public static final int PERFECT_FUSIONS = 3;
    public static final double PERFECT_SECONDS = 60;
    public static final double PERFECT_GAIN = 0.10;
    /** Ce que chaque tirage ajoute à la famille préférée, et le nombre de tirages qui comptent au plus (×2). */
    public static final double PREFERENCE_STEP = 0.05;
    public static final int PREFERENCE_MAX_DRAWS = 20;
    /** Un élément gratuit à choisir tous les tant d'éléments découverts. */
    public static final int FREE_ELEMENT_EVERY = 10;

    /**
     * Vrai dans le deuxième univers et au-delà : un univers est déjà formé. C'est là que s'ouvre
     * tout ce que la liste des idées range sous « tout se débloque lors du deuxième univers ».
     */
    public boolean isSecondUniverse() {
        return state.universeFormed();
    }

    /** Vrai pendant la partie qui mène à l'univers suivant : les anciens onglets et leurs paliers sont revenus. */
    public boolean isReplaying() {
        return state.universeFormed() && state.replaying();
    }

    /** Vrai si l'on peut rouvrir les paliers pour jouer l'univers suivant. */
    public boolean canStartNextUniverse() {
        return state.universeFormed() && !state.replaying();
    }

    /**
     * Rouvre les paliers (version 0.87) : les anciens onglets reviennent, pour une partie neuve, celle
     * de l'univers suivant. La partie a déjà été remise à zéro par la formation de l'univers. Former
     * l'univers à la fin de cette partie crée l'univers suivant, avec une remise à zéro et une graine de plus.
     */
    public boolean startNextUniverse() {
        if (!canStartNextUniverse()) return false;
        state.setReplaying(true);
        return true;
    }

    /** Fusions faites depuis que le dernier univers s'est formé. */
    public long fusionsSinceUniverse() {
        return Math.max(0, state.stats().fusions() - state.fusionsAtUniverse());
    }

    /** Vrai une fois l'onglet Skins ouvert : cinq fusions dans le deuxième univers. */
    public boolean isSkinsUnlocked() {
        return state.skinsOpen();
    }

    /** Vrai si ce skin est débloqué. */
    public boolean hasSkin(idle.core.universe.Skin skin) {
        return state.unlockedSkins().contains(skin.id());
    }

    /** Nombre de skins débloqués. */
    public int skinsUnlocked() {
        int count = 0;
        for (idle.core.universe.Skin skin : idle.core.universe.Skin.values()) if (hasSkin(skin)) count++;
        return count;
    }

    /** Débloque un skin ; vrai s'il ne l'était pas. */
    public boolean unlockSkin(idle.core.universe.Skin skin) {
        if (hasSkin(skin)) return false;
        state.unlockSkin(skin.id());
        moleculeBonusesVersion = -1;
        return true;
    }

    /** Le skin choisi pour ce visuel, ou {@code null} pour l'apparence classique. */
    public idle.core.universe.Skin chosenSkin(idle.core.universe.SkinVisual visual) {
        idle.core.universe.Skin skin = idle.core.universe.Skin.byId(state.chosenSkins().get(visual.name()));
        return skin != null && skin.visual() == visual && hasSkin(skin) ? skin : null;
    }

    /** Choisit un skin débloqué pour son visuel ; {@code null} revient à l'apparence classique de ce visuel. */
    public boolean chooseSkin(idle.core.universe.SkinVisual visual, idle.core.universe.Skin skin) {
        if (skin != null && (skin.visual() != visual || !hasSkin(skin))) return false;
        state.chooseSkin(visual.name(), skin == null ? null : skin.id());
        return true;
    }

    /** Ce que les skins débloqués multiplient, sur toutes les ressources d'en dessous de l'univers. */
    public double skinBoost() {
        return 1 + idle.core.universe.Skin.BONUS * skinsUnlocked();
    }

    public boolean isRegimeUnlocked() {
        return state.regimeOpen();
    }

    /** Vrai si le régime rapide peut être lancé : ouvert, ni en cours ni au repos. */
    public boolean canStartRegime() {
        return isRegimeUnlocked() && state.regimeLeft() <= 0 && state.regimeRest() <= 0;
    }

    /** Lance le régime rapide : +15 % de cadence pendant 20 s, puis 40 s de repos. Un geste à la main, jamais automatique. */
    public boolean startRegime() {
        if (!canStartRegime()) return false;
        state.setRegimeLeft(REGIME_SECONDS);
        return true;
    }

    /** La cadence du régime rapide : ×1,15 pendant qu'il dure. */
    public double regimeMultiplier() {
        return state.regimeLeft() > 0 ? 1 + REGIME_GAIN : 1;
    }

    public boolean isSynergyUnlocked() {
        return state.synergyOpen();
    }

    /** La synergie par rang : +0,5 % au plus à chaque générateur, selon les autres rangs possédés dans le cycle. */
    public double synergyMultiplier() {
        if (!isSynergyUnlocked()) return 1;
        int others = Math.max(0, generatorCount() - 1);
        return 1 + SYNERGY_MAX * Math.min(1, others / (double) (SYNERGY_GENERATORS - 1));
    }

    /** Vrai une fois la prime de cycle parfait ouverte : Persistance achetée, dans le deuxième univers. */
    public boolean isPerfectCycleUnlocked() {
        return isSecondUniverse() && keepsUpgradesOnFusion();
    }

    /** Ce que la prime de cycle parfait multiplie dans les particules, pendant qu'elle dure : elle ne se cumule pas. */
    public double perfectCycleMultiplier() {
        return state.perfectLeft() > 0 ? 1 + PERFECT_GAIN : 1;
    }

    void notePerfectCycle(boolean capped) {
        if (!isPerfectCycleUnlocked()) return;
        if (capped) {
            state.setPerfectStreak(0);
            return;
        }
        state.setPerfectStreak(state.perfectStreak() + 1);
        if (state.perfectStreak() >= PERFECT_FUSIONS) {
            state.setPerfectStreak(0);
            state.setPerfectLeft(PERFECT_SECONDS);
        }
    }

    public boolean isCycleChoiceUnlocked() {
        return isSecondUniverse() && state.bigBangs() > 0;
    }

    /** Le choix du petit arbre pour ce cycle, ou {@code null}. */
    public idle.core.universe.CycleChoice cycleChoice() {
        if (!isCycleChoiceUnlocked() || state.cycleChoice() == null) return null;
        try {
            return idle.core.universe.CycleChoice.valueOf(state.cycleChoice());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Fait le choix du cycle : un seul, jusqu'au prochain Big Bang. */
    public boolean chooseCycle(idle.core.universe.CycleChoice choice) {
        if (!isCycleChoiceUnlocked() || choice == null || cycleChoice() != null) return false;
        state.setCycleChoice(choice.name());
        return true;
    }

    public boolean isFamilyPreferenceUnlocked() {
        return isSecondUniverse() && isTargetedSynthesisUnlocked();
    }

    /** La famille préférée, ou {@code null}. */
    public ElementCategory preferredFamily() {
        if (!isFamilyPreferenceUnlocked() || state.preferredFamily() == null) return null;
        try {
            return ElementCategory.valueOf(state.preferredFamily());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Choisit la famille préférée ({@code null} : aucune) ; la préférence repart de zéro. */
    public boolean setPreferredFamily(ElementCategory family) {
        if (!isFamilyPreferenceUnlocked()) return false;
        state.setPreferredFamily(family == null ? null : family.name());
        state.setPreferenceDraws(0);
        return true;
    }

    /** Ce par quoi la préférence multiplie les chances de sa famille : +5 % par tirage au hasard, ×2 au plus. */
    public double preferenceMultiplier(ElementCategory category) {
        ElementCategory preferred = preferredFamily();
        if (preferred == null || preferred != category) return 1;
        return 1 + PREFERENCE_STEP * Math.min(PREFERENCE_MAX_DRAWS, state.preferenceDraws());
    }

    /** Éléments gratuits à choisir maintenant : un tous les dix éléments découverts, dans le deuxième univers. */
    public int freeElementsAvailable() {
        if (!isSecondUniverse()) return 0;
        return Math.max(0, discoveredElements() / FREE_ELEMENT_EVERY - state.freeElementsUsed());
    }

    /** Prend gratuitement un élément pas encore découvert. */
    public boolean claimFreeElement(int atomicNumber) {
        if (freeElementsAvailable() <= 0 || elementCount(atomicNumber) > 0) return false;
        state.setElementCount(PeriodicTable.element(atomicNumber).number(), 1);
        state.setFreeElementsUsed(state.freeElementsUsed() + 1);
        return true;
    }

    /** Outil d'essai : forme l'univers sur-le-champ, comme si ses conditions étaient réunies (remise à zéro comprise). */
    public void testFormUniverse() {
        state.setCosmosLevel(Cosmos.values().length);
        noteStep(GameStats.Step.UNIVERSE);
        formUniverse();
    }

    /** Outil d'essai : compte des fusions de plus dans les statistiques, sans rien changer d'autre. */
    public void testAddFusions(int count) {
        for (int i = 0; i < count; i++) state.stats().noteFusion(BigNum.ONE, 1);
    }

    /** Outil d'essai : débloque tous les skins, ou les reverrouille tous. */
    public void testAllSkins(boolean unlocked) {
        for (idle.core.universe.Skin skin : idle.core.universe.Skin.values()) {
            if (unlocked) state.unlockSkin(skin.id());
            else state.lockSkin(skin.id());
        }
        moleculeBonusesVersion = -1;
    }

    /** Ce qui s'ouvre et s'écoule dans le deuxième univers, à chaque instant. */
    void runSecondUniverse(double dt) {
        if (!isSecondUniverse()) return;
        if (!state.skinsOpen() && fusionsSinceUniverse() >= SKINS_FUSIONS) state.setSkinsOpen(true);
        if (!state.regimeOpen() && generatorCount() >= REGIME_GENERATORS) state.setRegimeOpen(true);
        if (!state.synergyOpen() && generatorCount() >= SYNERGY_GENERATORS) state.setSynergyOpen(true);
        if (dt <= 0) return;
        if (state.regimeLeft() > 0) {
            double left = state.regimeLeft() - dt;
            state.setRegimeLeft(left);
            if (left <= 0) state.setRegimeRest(REGIME_REST_SECONDS + Math.min(0, left));
        } else if (state.regimeRest() > 0) {
            state.setRegimeRest(state.regimeRest() - dt);
        }
        if (state.perfectLeft() > 0) state.setPerfectLeft(state.perfectLeft() - dt);
        noteSkinUnlocks();
        runTrial(dt);
    }

    // ------------------------------------------------------------------
    // Les skins à obtenir (version 0.88)
    // ------------------------------------------------------------------

    /** Le record de reconstruction qui donne le skin « Éclair » : de la fusion au dixième générateur en une minute au plus. */
    public static final double REBUILD_FAST_SECONDS = 60;
    /** Le prix de la recherche « Fusion prismatique », en particules. */
    public static final BigNum PRISM_COST = BigNum.of(1, 50);
    /** Atomes créés qu'il faut pour la recherche sur le noyau dense, et le prix de ses quatre paliers, en atomes. */
    public static final BigNum DENSE_UNLOCK_ATOMS = BigNum.of(1, 6);
    public static final BigNum[] DENSE_COSTS = {BigNum.of(100), BigNum.of(1, 3), BigNum.of(1, 5), BigNum.of(1, 8)};
    private static final idle.core.universe.Skin[] DENSE_SKINS = {idle.core.universe.Skin.ATOM_DENSE, idle.core.universe.Skin.TABLE_LEAD,
            idle.core.universe.Skin.ATOM_STELLAR, idle.core.universe.Skin.TABLE_GOLD};
    /** Les paliers de la matière noire (gagnée dans le deuxième univers) et leurs skins. */
    public static final BigNum[] DARK_TIERS = {BigNum.of(100), BigNum.of(1, 3), BigNum.of(1, 4), BigNum.of(1, 5)};
    private static final idle.core.universe.Skin[] DARK_SKINS = {idle.core.universe.Skin.DARK_OBSIDIAN, idle.core.universe.Skin.DARK_VORTEX,
            idle.core.universe.Skin.DARK_NEBULA, idle.core.universe.Skin.DARK_PULSAR};
    /** Les paliers du Big Bang (Big Bangs du deuxième univers) et leurs skins. */
    public static final int[] BANG_TIERS = {1, 3};
    private static final idle.core.universe.Skin[] BANG_SKINS = {idle.core.universe.Skin.BANG_AURORA, idle.core.universe.Skin.BANG_SUPERNOVA};

    /** Les paliers de la matière noire atteints, de 0 à 4. */
    public int darkSkinTiers() {
        int tiers = 0;
        for (BigNum tier : DARK_TIERS) if (darkMatterEarned().gte(tier)) tiers++;
        return tiers;
    }

    /** Les paliers du Big Bang atteints, de 0 à 2. */
    public int bangSkinTiers() {
        int tiers = 0;
        for (int tier : BANG_TIERS) if (state.bigBangs() >= tier) tiers++;
        return tiers;
    }

    /** Les skins des paliers et du record se débloquent seuls, dès que l'onglet Skins est ouvert. */
    private void noteSkinUnlocks() {
        if (!isSkinsUnlocked()) return;
        for (int i = 0; i < darkSkinTiers(); i++) unlockSkin(DARK_SKINS[i]);
        for (int i = 0; i < bangSkinTiers(); i++) unlockSkin(BANG_SKINS[i]);
        if (state.rebuildPending() && generatorCount() >= generatorsPerAtom()) {
            double time = state.timeSinceFusion();
            state.setRebuildPending(false);
            if (state.rebuildRecord() <= 0 || time < state.rebuildRecord()) state.setRebuildRecord(time);
            if (time <= REBUILD_FAST_SECONDS) unlockSkin(idle.core.universe.Skin.FUSION_FLASH);
        }
    }

    /** Le record de reconstruction, en secondes ; 0 sans record. */
    public double rebuildRecord() {
        return state.rebuildRecord();
    }

    /** Vrai si la recherche « Fusion prismatique » est faite. */
    public boolean hasPrismResearch() {
        return state.researches().contains("prism");
    }

    public boolean canBuyPrismResearch() {
        return isSkinsUnlocked() && !hasPrismResearch() && state.particles().gte(PRISM_COST);
    }

    /** La recherche facultative en particules : elle donne le skin « Prisme » de la fusion. */
    public boolean buyPrismResearch() {
        if (!canBuyPrismResearch()) return false;
        state.setParticles(state.particles().subtract(PRISM_COST).max(BigNum.ZERO));
        state.stats().addParticlesSpent(PRISM_COST);
        state.addResearch("prism");
        unlockSkin(idle.core.universe.Skin.FUSION_PRISM);
        return true;
    }

    /** Vrai une fois la recherche sur le noyau dense ouverte : un million d'atomes créés, dans le deuxième univers. */
    public boolean isDenseResearchUnlocked() {
        return isSkinsUnlocked() && state.stats().atomsCreated().gte(DENSE_UNLOCK_ATOMS);
    }

    /** Paliers de la recherche sur le noyau dense déjà faits, de 0 à 4. */
    public int denseTiers() {
        int tiers = 0;
        while (tiers < DENSE_COSTS.length && state.researches().contains("dense" + (tiers + 1))) tiers++;
        return tiers;
    }

    public boolean canBuyDenseTier() {
        int next = denseTiers();
        return isDenseResearchUnlocked() && next < DENSE_COSTS.length && state.atoms().gte(DENSE_COSTS[next]);
    }

    /** Le palier suivant de la recherche sur le noyau dense : un skin d'atome ou de tableau à chaque palier. */
    public boolean buyDenseTier() {
        if (!canBuyDenseTier()) return false;
        int next = denseTiers();
        state.setAtoms(state.atoms().subtract(DENSE_COSTS[next]).max(BigNum.ZERO));
        state.stats().addAtomsSpent(DENSE_COSTS[next]);
        state.addResearch("dense" + (next + 1));
        unlockSkin(DENSE_SKINS[next]);
        return true;
    }

    /** L'épreuve en cours, ou {@code null}. */
    public idle.core.universe.Trial activeTrial() {
        if (state.activeTrial() == null) return null;
        try {
            return idle.core.universe.Trial.valueOf(state.activeTrial());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Vrai pendant cette épreuve. */
    public boolean inTrial(idle.core.universe.Trial trial) {
        return trial.name().equals(state.activeTrial());
    }

    /** Vrai si cette épreuve a déjà été réussie. */
    public boolean isTrialCompleted(idle.core.universe.Trial trial) {
        return state.completedTrials().contains(trial.name());
    }

    /** Vrai si cette épreuve est ouverte : l'onglet Skins, et ce qu'elle demande. */
    public boolean isTrialUnlocked(idle.core.universe.Trial trial) {
        if (!isSkinsUnlocked()) return false;
        return switch (trial) {
            case LONE_GENERATOR -> state.explosions() > 0;
            case SOFT_CONTRACTION -> isDarkMatterUnlocked();
            case COLD_BANG -> state.bigBangs() > 0;
            case DETECTOR -> isColliderUnlocked() && colliderUpgradeLevel(ColliderUpgrade.WINDOW) >= 3;
            case NO_OVERHEAT, FULL_CHARGE, LONE_COLLIDER -> isColliderUnlocked();
        };
    }

    /** Vrai si l'épreuve peut commencer : ouverte, aucune autre en cours, et pour le premier générateur, un seul générateur. */
    public boolean canStartTrial(idle.core.universe.Trial trial) {
        if (!isTrialUnlocked(trial) || activeTrial() != null) return false;
        return trial != idle.core.universe.Trial.LONE_GENERATOR || generatorCount() == 1;
    }

    /** Commence une épreuve. */
    public boolean startTrial(idle.core.universe.Trial trial) {
        if (!canStartTrial(trial)) return false;
        state.setActiveTrial(trial.name());
        state.setTrialStart(state.timePlayed());
        state.setTrialCount(0);
        state.setTrialBase(trial == idle.core.universe.Trial.FULL_CHARGE
                ? colliderEnergyProduced() + colliderEnergyPerSecond() * idle.core.universe.Trial.FULL_CHARGE_SECONDS : 0);
        return true;
    }

    /** Abandonne l'épreuve en cours : rien n'est perdu, rien n'est gagné. */
    public boolean abandonTrial() {
        if (activeTrial() == null) return false;
        state.setActiveTrial(null);
        return true;
    }

    /** Secondes qu'il reste à l'épreuve en cours ; l'infini sans limite, 0 sans épreuve. */
    public double trialLeft() {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return 0;
        if (trial.seconds() <= 0) return Double.POSITIVE_INFINITY;
        return Math.max(0, trial.seconds() - (state.timePlayed() - state.trialStart()));
    }

    /** Où en est l'épreuve en cours, de 0 à 1. */
    public double trialProgress() {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return 0;
        return switch (trial) {
            case LONE_GENERATOR -> Math.min(1, Math.max(0, state.particles().log10() / idle.core.universe.Trial.LONE_GOAL_LOG10));
            case SOFT_CONTRACTION, COLD_BANG -> 0;
            case FULL_CHARGE -> state.trialBase() <= 0 ? 0 : Math.min(1, colliderEnergyProduced() / state.trialBase());
            default -> Math.min(1, state.trialCount() / (double) trial.goal());
        };
    }

    /** Ce que la contraction douce ajoute aux atomes de chaque fusion : +5 % par fusion du cycle, ×2 au plus. */
    public double contractionAtomsMultiplier() {
        if (!inTrial(idle.core.universe.Trial.SOFT_CONTRACTION)) return 1;
        return Math.min(idle.core.universe.Trial.CONTRACTION_MAX, 1 + idle.core.universe.Trial.CONTRACTION_FUSION_GAIN * state.trialCount());
    }

    void completeTrial() {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return;
        state.setActiveTrial(null);
        state.addCompletedTrial(trial.name());
        unlockSkin(trial.skin());
    }

    void failTrial() {
        state.setActiveTrial(null);
    }

    /** L'épreuve en cours : sa contrainte au fil du temps, son but, sa limite. */
    private void runTrial(double dt) {
        idle.core.universe.Trial trial = activeTrial();
        if (trial == null) return;
        if (trial == idle.core.universe.Trial.SOFT_CONTRACTION && dt > 0) {
            state.setParticles(state.particles().multiply(Math.pow(1 - idle.core.universe.Trial.CONTRACTION_LOSS, dt / idle.core.universe.Trial.CONTRACTION_EVERY)));
        }
        if (trial == idle.core.universe.Trial.NO_OVERHEAT && state.overheatLeft() > 0) {
            failTrial();
            return;
        }
        boolean done = switch (trial) {
            case LONE_GENERATOR -> state.particles().log10() >= idle.core.universe.Trial.LONE_GOAL_LOG10;
            case SOFT_CONTRACTION, COLD_BANG -> false;
            case FULL_CHARGE -> state.trialBase() > 0 && colliderEnergyProduced() >= state.trialBase();
            default -> state.trialCount() >= trial.goal();
        };
        if (done) {
            completeTrial();
            return;
        }
        if (trial.seconds() > 0 && state.timePlayed() - state.trialStart() >= trial.seconds()) failTrial();
    }

    /** Graines d'univers que donne la formation de l'univers (version 0.83). */
    public static final int SEEDS_PER_UNIVERSE = 1;

    /**
     * L'univers se forme (version 0.83) : tout ce qui y a mené est remis à zéro
     * ({@link GameState#startUniverse()}), et le joueur reçoit {@link #SEEDS_PER_UNIVERSE} graine
     * d'univers. Les succès gagnés par la formation sont relevés avant.
     */
    private void formUniverse() {
        checkAchievements();
        resetForUniverse();
        forgetDerived();
    }

    /** La remise à zéro de l'univers, son bilan relevé avant, et sa graine. */
    void resetForUniverse() {
        // Un univers de plus (version 0.87) : celui-ci était déjà formé, la partie rejouée en forme un autre.
        boolean another = state.universeFormed();
        state.stats().noteUniverseTally(state.bigBangs(), moleculesCreated(), state.collisionRuns(), state.cometsCaught());
        state.startUniverse();
        if (another) state.setExtraUniverses(state.extraUniverses() + 1);
        state.setUniverseSeeds(state.universeSeeds().add(BigNum.of(SEEDS_PER_UNIVERSE)));
    }
}

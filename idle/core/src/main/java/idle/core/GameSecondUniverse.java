package idle.core;

import idle.core.state.*;
import idle.core.achievements.*;
import idle.core.collider.*;
import idle.core.darkmatter.*;
import idle.core.universe.*;
import idle.core.elements.*;
import idle.core.math.BigNum;
import idle.core.sky.*;
import idle.core.upgrades.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Une partie de {@link GameUniverse} (découpée en 0.293 pour rester sous 1 000 lignes) : le deuxième univers et ses nouveautés. */
public abstract class GameSecondUniverse extends GameChrono {
    GameSecondUniverse(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /**
     * Vrai dans le deuxième univers et au-delà : un univers est déjà formé. C'est là que s'ouvre
     * tout ce que la liste des idées range sous « tout se débloque lors du deuxième univers ».
     */
    public boolean isSecondUniverse() { return state.universeFormed(); }

    /** Vrai pendant la partie qui mène à l'univers suivant : les anciens onglets et leurs paliers sont revenus. */
    public boolean isReplaying() { return state.universeFormed() && state.replaying(); }

    /** Vrai si l'on peut rouvrir les paliers pour jouer l'univers suivant. */
    public boolean canStartNextUniverse() {
        // Un univers atteint par l'échelle du cosmos seule (outil d'essai, ancienne partie) compte aussi.
        return isUniverseFormed() && !state.replaying();
    }

    /**
     * Rouvre les paliers : les anciens onglets reviennent, pour une partie neuve, celle
     * de l'univers suivant. La partie a déjà été remise à zéro par la formation de l'univers. Former
     * l'univers à la fin de cette partie crée l'univers suivant, avec une remise à zéro et une graine de plus.
     */
    public boolean startNextUniverse() {
        if (!canStartNextUniverse()) return false;
        // Les trois actes repartent vraiment de zéro, même si quelque chose s'est joué depuis la
        // formation de l'univers (les anciens onglets montrés, une partie d'avant). La partie secondaire reste :
        // univers, graine, dimensions, skins, succès et temps de jeu.
        boolean twig = isSeedTwig();
        boolean bySeed = canOpenNextUniverse();
        // Une bulle qui garde, achetée depuis la formation, garde sa part pour cet univers-ci.
        state.startUniverseKeeping();
        forgetDerived();
        state.setReplaying(true);
        if (twig) state.setTwigOpened(true);
        if (bySeed) state.setSeedUniversesOpened(state.seedUniversesOpened() + 1);
        return true;
    }

    /** Fusions faites depuis que le dernier univers s'est formé. */
    public long fusionsSinceUniverse() { return Math.max(0, state.stats().fusions() - state.fusionsAtUniverse()); }

    /** Vrai une fois l'onglet Skins ouvert : cinq fusions dans le deuxième univers. */
    public boolean isSkinsUnlocked() { return state.skinsOpen(); }

    /** Vrai si ce skin est débloqué. */
    public boolean hasSkin(Skin skin) { return skin.isFree() || state.unlockedSkins().contains(skin.id()); }

    /** Nombre de skins débloqués, les skins libres compris. */
    public int skinsUnlocked() { return state.skinsUnlockedCount(false); }

    /**
     * Les skins débloqués qui comptent pour le bonus, sans les libres. Tous, sans plafond
     * (cent au plus de la 0.192 à la 0.208) ; depuis la 0.206 ils s'étalent jusqu'au onzième univers.
     */
    public int skinsCounted() {
        // Le compte se garde tant que les skins débloqués ne changent pas (il se relit à chaque fusion).
        return state.skinsUnlockedCount(true);
    }

    /**
     * Débloque un skin ; vrai s'il ne l'était pas. Un skin attend son univers
     * ({@link idle.core.universe.Skin#minUniverses()}) en plus de sa condition, pour que la collection s'étale jusqu'à la
     * fin de la partie ; une sauvegarde qui l'avait déjà le garde.
     */
    public boolean unlockSkin(Skin skin) {
        if (hasSkin(skin) || universes() < skin.minUniverses()) return false;
        state.unlockSkin(skin.id());
        moleculeBonusesVersion = -1;
        return true;
    }

    /** Le skin choisi pour ce visuel, ou {@code null} pour l'apparence classique. */
    public Skin chosenSkin(SkinVisual visual) {
        Skin skin = Skin.byId(state.chosenSkins().get(visual.name()));
        return skin != null && skin.visual() == visual && hasSkin(skin) ? skin : null;
    }

    /** Choisit un skin débloqué pour son visuel ; {@code null} revient à l'apparence classique de ce visuel. */
    public boolean chooseSkin(SkinVisual visual, Skin skin) {
        if (skin != null && (skin.visual() != visual || !hasSkin(skin))) return false;
        state.chooseSkin(visual.name(), skin == null ? null : skin.id());
        return true;
    }

    /** Ce que les skins débloqués multiplient, sur toutes les ressources d'en dessous de l'univers : +2 % par skin compté ({@link #skinsCounted()}). */
    public double skinBoost() { return 1 + Skin.BONUS * skinsCounted(); }

    public boolean isRegimeUnlocked() { return state.regimeOpen(); }

    /** Vrai si le régime rapide peut être lancé : ouvert, ni en cours ni au repos. */
    public boolean canStartRegime() { return isRegimeUnlocked() && state.regimeLeft() <= 0 && state.regimeRest() <= 0; }

    /** Lance le régime rapide : +15 % de cadence pendant 20 s, puis 40 s de repos. Un geste à la main, jamais automatique. */
    public boolean startRegime() {
        if (!canStartRegime()) return false;
        state.setRegimeLeft(REGIME_SECONDS);
        return true;
    }

    /** La cadence du régime rapide : ×1,15 pendant qu'il dure. */
    public double regimeMultiplier() { return state.regimeLeft() > 0 ? 1 + REGIME_GAIN : 1; }

    public boolean isSynergyUnlocked() { return state.synergyOpen(); }

    /** La synergie par rang : +0,5 % au plus à chaque générateur, selon les autres rangs possédés dans le cycle. */
    public double synergyMultiplier() {
        if (!isSynergyUnlocked()) return 1;
        int others = Math.max(0, generatorCount() - 1);
        return 1 + SYNERGY_MAX * Math.min(1, others / (double) (SYNERGY_GENERATORS - 1));
    }

    /** Vrai une fois la prime de cycle parfait ouverte : son succès, et la Persistance achetée. */
    public boolean isPerfectCycleUnlocked() {
        return isSecondUniverse() && universeStep(Achievements.UNIVERSE_PERFECT) && keepsUpgradesOnFusion();
    }

    /** Ce que la prime de cycle parfait multiplie dans les particules, pendant qu'elle dure : elle ne se cumule pas. */
    public double perfectCycleMultiplier() { return state.perfectLeft() > 0 ? 1 + PERFECT_GAIN : 1; }

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

    /** Vrai une fois le petit arbre du cycle ouvert : son succès, et un Big Bang dans cette partie. */
    public boolean isCycleChoiceUnlocked() {
        return isSecondUniverse() && universeStep(Achievements.UNIVERSE_CYCLE) && state.bigBangs() > 0;
    }

    /** Le choix du petit arbre pour ce cycle, ou {@code null}. */
    public CycleChoice cycleChoice() {
        if (!isCycleChoiceUnlocked() || state.cycleChoice() == null) return null;
        return Enums.parse(CycleChoice.class, state.cycleChoice());
    }

    /** Fait le choix du cycle : un seul, jusqu'au prochain Big Bang. */
    public boolean chooseCycle(CycleChoice choice) {
        if (!isCycleChoiceUnlocked() || choice == null || cycleChoice() != null) return false;
        state.setCycleChoice(choice.name());
        return true;
    }

    /** Vrai une fois la famille préférée ouverte : son succès, et la synthèse ciblée. */
    public boolean isFamilyPreferenceUnlocked() {
        return isSecondUniverse() && universeStep(Achievements.UNIVERSE_PREFERENCE) && isTargetedSynthesisUnlocked();
    }

    /** La famille préférée, ou {@code null}. */
    public ElementCategory preferredFamily() {
        if (!isFamilyPreferenceUnlocked() || state.preferredFamily() == null) return null;
        return Enums.parse(ElementCategory.class, state.preferredFamily());
    }

    /** Choisit la famille préférée ({@code null} : aucune) ; la préférence repart de zéro. */
    public boolean setPreferredFamily(ElementCategory family) {
        if (!isFamilyPreferenceUnlocked()) return false;
        state.setPreferredFamily(family == null ? null : family.name());
        state.setPreferenceDraws(0);
        return true;
    }

    /** Ce par quoi la préférence multiplie les chances de sa famille : +5 % par tirage au hasard, ×2 au plus. */
    public double preferenceMultiplier(ElementCategory category) { return preferenceMultiplier(category, preferredFamily()); }

    /** Le même multiplicateur, la famille préférée ({@code null} : aucune) déjà lue. */
    double preferenceMultiplier(ElementCategory category, ElementCategory preferred) {
        if (preferred == null || preferred != category) return 1;
        return 1 + PREFERENCE_STEP * Math.min(PREFERENCE_MAX_DRAWS, state.preferenceDraws());
    }

    /** Éléments gratuits à choisir maintenant : un tous les dix éléments découverts, dans le deuxième univers. */
    public int freeElementsAvailable() {
        // Ils s'ouvrent par leur succès, dix éléments découverts dans un nouvel univers.
        if (!isSecondUniverse() || !universeStep(Achievements.UNIVERSE_FREE)) return 0;
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
    public void testAddFusions(int count) { for (int i = 0; i < count; i++) state.stats().noteFusion(BigNum.ONE, 1); }

    /** Outil d'essai : débloque tous les skins, ou les reverrouille tous. */
    public void testAllSkins(boolean unlocked) {
        for (Skin skin : Skin.values()) {
            if (unlocked) state.unlockSkin(skin.id());
            else state.lockSkin(skin.id());
        }
        moleculeBonusesVersion = -1;
    }

    /** Ce qui s'ouvre et s'écoule dans le deuxième univers, à chaque instant. */
    void runSecondUniverse(double dt) {
        if (!isSecondUniverse()) return;
        // Chacune s'ouvre par son succès du deuxième univers (cinq fusions, trois et dix générateurs).
        if (!state.skinsOpen() && universeStep(Achievements.UNIVERSE_SKINS)) state.setSkinsOpen(true);
        if (!state.regimeOpen() && universeStep(Achievements.UNIVERSE_REGIME)) state.setRegimeOpen(true);
        if (!state.synergyOpen() && universeStep(Achievements.UNIVERSE_SYNERGY)) state.setSynergyOpen(true);
        if (dt <= 0) return;
        // Le régime rapide et la prime de cycle parfait s'écoulent avec la production qu'ils multiplient :
        // voir Game#runTimedBonuses.
        noteSkinUnlocks();
        runTrial(dt);
    }
}

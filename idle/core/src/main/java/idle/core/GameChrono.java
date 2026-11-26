package idle.core;

import idle.core.state.*;
import idle.core.achievements.*;
import idle.core.collider.*;
import idle.core.darkmatter.*;
import idle.core.universe.*;
import idle.core.elements.*;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.*;
import idle.core.upgrades.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Une partie de {@link GameUniverse} (découpée en 0.293 pour rester sous 1 000 lignes) : les améliorations de la graine et le chrono. */
public abstract class GameChrono extends GameSeed {
    GameChrono(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** La réserve de la ressource de la dimension {@code number} (1 à 10) ; 0 pour les autres. */
    public double dimensionResource(int number) {
        return switch (number) {
            case 1 -> state.dimensionShards();
            case 2 -> state.planeShards();
            case 3 -> state.volumeShards();
            case 4 -> state.durationShards();
            case 5 -> state.loopShards();
            case 6 -> state.braneShards();
            case 7 -> state.clickerShards();
            case 8 -> state.gridShards();
            case 9 -> state.mirrorShards();
            case 10 -> state.mixShards();
            default -> 0;
        };
    }

    /** Retire {@code amount} de la ressource de la dimension {@code number} (1 à 10), sans descendre sous zéro. */
    void spendDimensionResource(int number, double amount) {
        switch (number) {
            case 1 -> state.setDimensionShards(Math.max(0, state.dimensionShards() - amount));
            case 2 -> state.setPlaneShards(Math.max(0, state.planeShards() - amount));
            case 3 -> state.setVolumeShards(Math.max(0, state.volumeShards() - amount));
            case 4 -> state.setDurationShards(Math.max(0, state.durationShards() - amount));
            case 5 -> state.setLoopShards(Math.max(0, state.loopShards() - amount));
            case 6 -> state.setBraneShards(Math.max(0, state.braneShards() - amount));
            case 7 -> state.setClickerShards(Math.max(0, state.clickerShards() - amount));
            case 8 -> state.setGridShards(Math.max(0, state.gridShards() - amount));
            case 9 -> state.setMirrorShards(Math.max(0, state.mirrorShards() - amount));
            case 10 -> state.setMixShards(Math.max(0, state.mixShards() - amount));
            default -> throw new IllegalArgumentException("Pas de ressource pour la dimension " + number);
        }
    }

    public int seedUpgradeLevel(SeedUpgrade upgrade) { return state.seedUpgradeLevel(upgrade.ordinal()); }

    /** Prix du prochain niveau, dans la ressource de sa dimension ; infini une fois au plus haut. */
    public double seedUpgradeCost(SeedUpgrade upgrade) {
        // Arrondi à l'unité au-dessus : les croissances ×2,5 ne laissent pas de prix à virgule.
        return Math.ceil(upgrade.cost(seedUpgradeLevel(upgrade)) - 1e-9);
    }

    /**
     * Vrai si l'amélioration est ouverte : sa dimension existe, l'arrosoir est débloqué (pour son débit), l'étage du
     * chrono est là (pour celles du chrono).
     */
    public boolean isSeedUpgradeOpen(SeedUpgrade upgrade) {
        if (!isUniverseFormed() || dimensionsUnlocked() < upgrade.dimension) return false;
        if (upgrade == SeedUpgrade.WATERING && !state.wateringCan()) return false;
        return !upgrade.chrono || isChronoUnlocked();
    }

    public boolean canBuySeedUpgrade(SeedUpgrade upgrade) {
        return isSeedUpgradeOpen(upgrade) && seedUpgradeLevel(upgrade) < upgrade.max()
                && dimensionResource(upgrade.dimension) >= seedUpgradeCost(upgrade) - 1e-9;
    }

    public boolean buySeedUpgrade(SeedUpgrade upgrade) {
        if (!canBuySeedUpgrade(upgrade)) return false;
        spendDimensionResource(upgrade.dimension, seedUpgradeCost(upgrade));
        state.setSeedUpgradeLevel(upgrade.ordinal(), seedUpgradeLevel(upgrade) + 1);
        return true;
    }

    /** Arrosages par seconde de l'arrosoir tenu : 1, et 1 de plus par niveau de débit. */
    public double wateringCanRate() {
        return WATERING_CAN_PER_SECOND * (1 + seedUpgradeLevel(SeedUpgrade.WATERING)) * wateringBoost();
    }

    /** Ce qui accélère l'arrosage pour un temps : le rond de croissance ; 1 sans lui. */
    public double wateringBoost() { return 1; }

    /** Vrai une fois le seuil de la boucle atteint : {@link #LOOP_THRESHOLD} nœuds gagnés en tout. */
    public boolean isLoopThresholdReached() { return state.loopShardsEarned() >= LOOP_THRESHOLD - 1e-9; }

    /** Ce que verse le tuyau, en part de ce que verse l'arrosoir : 10 % par niveau, 0 sans tuyau. */
    public double hoseShare() { return 0.1 * seedUpgradeLevel(SeedUpgrade.HOSE); }

    /** Arrosages par seconde du tuyau, qui verse seul. */
    public double hoseRate() { return hoseShare() * wateringCanRate(); }

    public boolean hasHose() { return seedUpgradeLevel(SeedUpgrade.HOSE) > 0; }

    /** Ce que compte chaque arrosage grâce à la terre fertile : 1, puis un de plus par niveau (version 0.179 : ×2 par niveau avant). */
    public long seedSoilFactor() { return 1L + seedUpgradeLevel(SeedUpgrade.SOIL); }

    public boolean isChronoUnlocked() { return seedUpgradeLevel(SeedUpgrade.CHRONO) > 0; }

    /** Secondes gardées par seconde : 0,1, et 0,1 de plus par niveau de remplissage. */
    public double chronoFillRate() { return CHRONO_FILL * (1 + seedUpgradeLevel(SeedUpgrade.CHRONO_FILL)); }

    /** Le temps que garde le chrono au plus : 5 minutes, et 5 de plus par niveau de réserve. */
    public double chronoCapacity() { return CHRONO_CAPACITY * (1 + seedUpgradeLevel(SeedUpgrade.CHRONO_CAPACITY)); }

    /** L'accélération la plus forte : ×2, et +1 par niveau. */
    public double chronoMaxSpeed() { return CHRONO_MAX_SPEED + seedUpgradeLevel(SeedUpgrade.CHRONO_SPEED); }

    /** La part perdue du temps du chrono à l'accélération {@code speed} : plus on va vite, plus on perd. */
    public double chronoLoss(double speed) {
        double extra = Math.max(0, speed - 1);
        return Math.min(CHRONO_LOSS_MAX, CHRONO_LOSS_STEP * extra * Math.pow(CHRONO_LOSS_CUT, seedUpgradeLevel(SeedUpgrade.CHRONO_LOSS)));
    }

    /** Le temps gardé dans le chrono, en secondes. */
    public double chronoStored() { return state.chronoStored(); }

    /** L'accélération choisie, de ×1 (arrêté) à {@link #chronoMaxSpeed()}. */
    public double chronoSpeed() { return Math.max(1, Math.min(chronoMaxSpeed(), state.chronoSpeed())); }

    /** Règle l'accélération, entre ×1 et le plus fort ; ×1 l'arrête. */
    public void setChronoSpeed(double speed) {
        state.setChronoSpeed(Double.isFinite(speed) ? Math.max(1, Math.min(chronoMaxSpeed(), speed)) : 1);
    }

    /** Vrai tant que le chrono accélère le jeu : réglé au-dessus de ×1, avec du temps gardé. */
    public boolean isChronoRunning() {
        return isChronoUnlocked() && chronoSpeed() > 1 + 1e-9 && state.chronoStored() > 1e-9;
    }

    /**
     * Le chrono pendant {@code dt} secondes réelles : il se remplit, et s'il accélère, il rend le temps de jeu à jouer.
     * À l'accélération k, une seconde réelle en vaut k ; les k − 1 de plus se prennent dans le chrono, plus ce qui
     * s'en perd ({@link #chronoLoss(double)}). Sans assez de temps gardé, l'accélération se réduit à ce qui reste.
     * Une absence remplit le chrono sans rien accélérer.
     */
    double runChrono(double dt, boolean accelerate) {
        if (!isChronoUnlocked() || dt <= 0) return dt;
        double stored = Math.min(chronoCapacity(), state.chronoStored() + dt * chronoFillRate());
        double played = dt;
        double speed = chronoSpeed();
        if (accelerate && speed > 1) {
            double keep = 1 - chronoLoss(speed);
            double extra = (speed - 1) * dt, cost = extra / keep;
            if (cost > stored) {
                extra = stored * keep;
                cost = stored;
            }
            stored -= cost;
            played += extra;
        }
        state.setChronoStored(Math.max(0, stored));
        return played;
    }

    /** Outil d'essai : de l'énergie du vide en plus. */
    public void testAddVoidEnergy(BigNum amount) { state.setVoidEnergy(state.voidEnergy().add(amount)); }

    /** Outil d'essai : la partie secondaire revient à son début (un seul univers, rien en réserve). */
    public void testResetUniverses() {
        state.setExtraUniverses(0);
        state.setUniverseSeeds(BigNum.ZERO);
        state.setVoidEnergy(BigNum.ZERO);
        state.setVoidOpen(false);
        state.setVoidHoleOpen(false);
        state.setVoidSacrificed(BigNum.ZERO);
        for (int i = 0; i < VoidUpgrade.values().length; i++) state.setVoidLevel(i, 0);
        state.clearVoidHistory();
        state.setDimensionShards(0);
        state.setLinePhase(0);
        for (int i = 0; i < LineUpgrade.values().length; i++) state.setLineLevel(i, 0);
        state.setDimensionShardsEarned(0);
        state.setSeedSprouted(false);
        state.setSeedWaterings(0);
        state.setWateringCan(false);
        setWatering(false);
        for (int i = 0; i < SeedUpgrade.values().length; i++) state.setSeedUpgradeLevel(i, 0);
        state.setChronoStored(0);
        state.setChronoSpeed(1);
        state.clearTreeBubbles();
        state.setVoidSpent(0);
        state.setVoidTrickled(0);
        state.setVoidCarried(0);
        state.setHadronsOpen(false);
        state.setHadronsPrice(0);
        state.clearDimensionAutomations();
        for (Hadron hadron : Hadron.values()) state.setHadrons(hadron, 0);
        state.setPlaneShards(0);
        state.setPlaneShardsEarned(0);
        for (int i = 0; i < PlaneUpgrade.values().length; i++) state.setPlaneLevel(i, 0);
        state.setVolumeShards(0);
        state.setVolumeShardsEarned(0);
        for (int i = 0; i < VolumeUpgrade.values().length; i++) state.setVolumeLevel(i, 0);
        state.setDurationShards(0);
        state.setDurationShardsEarned(0);
        for (int i = 0; i < DurationUpgrade.values().length; i++) state.setDurationLevel(i, 0);
        state.setLoopShards(0);
        state.setLoopShardsEarned(0);
        for (int i = 0; i < GameStateBase.LOOP_UPGRADES; i++) state.setLoopLevel(i, 0);
        state.setBraneShards(0);
        state.setBraneShardsEarned(0);
        for (int i = 0; i < GameStateBase.BRANE_UPGRADES; i++) state.setBraneLevel(i, 0);
        state.setClickerShards(0);
        state.setClickerShardsEarned(0);
        for (int i = 0; i < GameStateBase.CLICKER_UPGRADES; i++) state.setClickerLevel(i, 0);
        state.setGridShards(0);
        state.setGridShardsEarned(0);
        for (int i = 0; i < GameStateBase.GRID_UPGRADES; i++) state.setGridLevel(i, 0);
        state.setMirrorShards(0);
        state.setMirrorShardsEarned(0);
        for (int i = 0; i < GameStateBase.MIRROR_UPGRADES; i++) state.setMirrorLevel(i, 0);
        state.setMirrorWins(0);
        state.setMixShards(0);
        state.setMixShardsEarned(0);
        for (int i = 0; i < GameStateBase.MIX_UPGRADES; i++) state.setMixLevel(i, 0);
        state.setMixStage(1);
        state.setMixStageEarned(0);
        dropDuration();
        leaveVolume();
    }

    /** Vrai si cette échelle du cosmos est formée. */
    public boolean hasCosmos(Cosmos scale) {
        // L'univers reste formé après la remise à zéro qui l'accompagne, quand le cosmos repart de rien.
        // Pendant la partie du deuxième univers, seul compte ce que cette partie a formé.
        if (scale == Cosmos.UNIVERSE && state.universeFormed() && !state.replaying()) return true;
        return state.cosmosLevel() > scale.ordinal();
    }

    /** Nombre d'échelles du cosmos formées : 0 avant la galaxie, 3 une fois l'univers formé. */
    public int cosmosFormed() { return state.cosmosLevel(); }

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
     * Vrai si cette échelle peut être formée maintenant : la précédente l'est, elle ne l'est pas encore, et ce qu'elle
     * demande est réuni. La galaxie demande tous les astres ({@link #canFormGalaxy()}) ; les suivantes, assez de
     * matière rassemblée dans chaque état ({@link Cosmos#matter()}).
     */
    public boolean canFormCosmos(Cosmos scale) {
        if (scale == Cosmos.GALAXY) return canFormGalaxy();
        if (!state.started() || hasCosmos(scale) || !hasCosmos(scale.previous())) return false;
        if (cosmosSettleRemaining(scale) > 0) return false;
        for (Map.Entry<Molecule.State, Integer> need : cosmosMatter(scale).entrySet()) {
            if (gatheredInState(need.getKey()) < need.getValue()) return false;
        }
        for (Map.Entry<Molecule.State, Integer> need : scale.sorts().entrySet()) {
            if (sortsInState(need.getKey()) < need.getValue()) return false;
        }
        return true;
    }

    /** Secondes de jeu qu'il reste avant que cette échelle puisse se former, 0 si le délai est passé. */
    public double cosmosSettleRemaining(Cosmos scale) {
        if (scale == Cosmos.GALAXY || !hasCosmos(scale.previous())) return 0;
        // La date de l'échelle d'avant dans cette partie, pas la toute première fois qu'elle s'est formée.
        double since = state.stats().reachedThisUniverseAt(scale.previous() == Cosmos.GALAXY ? GameStats.Step.GALAXY : GameStats.Step.CLUSTER);
        if (Double.isNaN(since)) return 0;
        return Math.max(0, since + cosmosSettleSeconds() * settleShare() - state.timePlayed() - state.settleHeld());
    }

    // ------------------------------------------------------------------
    // Le rassemblement accéléré (version 0.299)
    // ------------------------------------------------------------------

    /** Chaque seconde de clic tenu sur l'échelle qui se rassemble en vaut celle-ci de plus : quatre en tout. */
    public static final double SETTLE_HOLD_EXTRA = 3;

    /** L'échelle du cosmos dont le délai court en ce moment (l'amas ou l'univers), ou {@code null}. */
    public Cosmos settlingCosmos() {
        for (Cosmos scale : Cosmos.values()) {
            if (!hasCosmos(scale) && cosmosSettleRemaining(scale) > 0) return scale;
        }
        return null;
    }

    /** Vrai quand tenir le clic avance le rassemblement : la bulle achetée et un délai qui court. */
    public boolean canHoldSettle() { return isTreeBubbleBought(TreeBubble.GATHER) && settlingCosmos() != null; }

    /**
     * Le joueur tient le clic {@code seconds} secondes sur l'échelle qui se rassemble : son délai avance de
     * {@link #SETTLE_HOLD_EXTRA} fois ce temps en plus du temps qui passe, sans aller au-delà de ce qui reste.
     *
     * @return les secondes de délai gagnées
     */
    public double holdSettle(double seconds) {
        if (seconds <= 0 || !canHoldSettle()) return 0;
        double gain = Math.min(seconds * SETTLE_HOLD_EXTRA, cosmosSettleRemaining(settlingCosmos()));
        state.setSettleHeld(state.settleHeld() + gain);
        return gain;
    }

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
        for (Map.Entry<Molecule.State, Integer> need : cosmosMatter(scale).entrySet()) {
            if (gatheredInState(need.getKey()) >= need.getValue()) met++;
        }
        for (Map.Entry<Molecule.State, Integer> need : scale.sorts().entrySet()) {
            if (sortsInState(need.getKey()) >= need.getValue()) met++;
        }
        return met;
    }

    /**
     * Ce qui manque encore à cette échelle du cosmos, en mots, une condition par ligne : pour la galaxie,
     * chaque astre pas formé ou qui mûrit encore, et les systèmes solaires complets ; pour l'amas et l'univers, l'échelle
     * d'avant, puis chaque état de la matière dont la quantité rassemblée ou la variété ne suffit pas. Vide si tout est
     * réuni. Les nombres qui bougent (minutes de maturation, quantités) sont entre parenthèses, à la fin. Le délai de
     * rassemblement n'en est pas une condition.
     */
    public List<String> cosmosMissing(Cosmos scale) {
        List<String> missing = new ArrayList<>();
        if (scale == Cosmos.GALAXY) {
            if (hasGalaxy()) return missing;
            for (idle.core.sky.Body body : bodies.values()) {
                if (isBodyMature(body.id())) continue;
                missing.add(state.hasBody(body.id())
                        ? body.name() + " : mûrit encore (" + Math.round(Math.ceil(state.maturingLeft(body.id()) / 60)) + " min)"
                        : body.name() + " : pas encore formé");
            }
            if (solarSystemsComplete() < SYSTEMS_FOR_GALAXY) {
                missing.add("Systèmes solaires complets (" + solarSystemsComplete() + " / " + SYSTEMS_FOR_GALAXY + ")");
            }
            return missing;
        }
        if (!hasCosmos(scale.previous())) missing.add(scale.previous().label() + " : pas encore formé");
        for (Map.Entry<Molecule.State, Integer> need : cosmosMatter(scale).entrySet()) {
            long have = gatheredInState(need.getKey());
            if (have < need.getValue()) missing.add(need.getKey().label() + " rassemblé (" + have + " / " + need.getValue() + ")");
        }
        for (Map.Entry<Molecule.State, Integer> need : scale.sorts().entrySet()) {
            int have = sortsInState(need.getKey());
            if (have < need.getValue()) missing.add(need.getKey().label() + " : sortes de molécules (" + have + " / " + need.getValue() + ")");
        }
        return missing;
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
        // Le rassemblement tenu servait à cette échelle : la suivante repart de zéro.
        state.setSettleHeld(0);
        noteStep(switch (scale) {
            case GALAXY -> GameStats.Step.GALAXY;
            case CLUSTER -> GameStats.Step.CLUSTER;
            case UNIVERSE -> GameStats.Step.UNIVERSE;
        });
        if (scale == Cosmos.UNIVERSE) formUniverse();
        return true;
    }
}

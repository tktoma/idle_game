package idle.core;

import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.bigbang.BangAchievement;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.DarkAutomation;
import idle.core.darkmatter.DarkEffect;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.ElementBonuses;
import idle.core.elements.ElementEffect;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.Collection;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Les particules : la production, la fusion, les améliorations, l'impulsion et la comète.
 */
public abstract class GameParticles extends GameMatterAutomation {

    GameParticles(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    abstract void noteHeldPeaks();
    public abstract BigNum landmarkParticlesMultiplier();
    public abstract double landmarkAtomsMultiplier();
    abstract boolean expandDarkMatter(double seconds);


    // ------------------------------------------------------------------
    // L'impulsion : le clic du joueur actif (version 0.048)
    // ------------------------------------------------------------------

    /** Charges d'impulsion au plus : deux minutes d'avance, pour qui revient un instant. */
    public static final int PULSE_MAX_CHARGES = 40;
    /** Une charge revient toutes les trois secondes de jeu. */
    public static final double PULSE_REFILL_SECONDS = 3;
    /** Une impulsion donne d'un coup l'espace de dix secondes d'expansion. */
    public static final double PULSE_SECONDS = 10;

    /**
     * Vrai une fois le premier Big Bang passé : l'impulsion pousse l'expansion. Au milieu du
     * troisième acte, c'est l'espace qui manque ; le joueur présent l'accélère d'un clic (jusqu'à
     * quatre fois plus vite en cliquant sans arrêt), celui qui laisse tourner n'y perd rien.
     */
    public boolean isPulseUnlocked() {
        return state.started() && isBigBangUnlocked();
    }

    /** Charges entières disponibles. */
    public int pulseCharges() {
        return (int) Math.floor(state.pulseCharges() + 1e-9);
    }

    /** Espace que donnerait une impulsion maintenant. */
    public BigNum pulseGain() {
        return isPulseUnlocked() ? steadySpacePerSecond().multiply(PULSE_SECONDS) : BigNum.ZERO;
    }

    /** Secondes avant la prochaine charge (0 si la réserve est pleine). */
    public double secondsUntilPulse() {
        double charges = state.pulseCharges();
        if (charges >= PULSE_MAX_CHARGES) return 0;
        return (Math.floor(charges + 1e-9) + 1 - charges) * PULSE_REFILL_SECONDS;
    }

    /** Dépense une charge pour dix secondes d'expansion d'un coup. Faux sans charge ou avant le premier Big Bang. */
    public boolean pulse() {
        if (!isPulseUnlocked() || state.pulseCharges() < 1 - 1e-9) return false;
        state.setPulseCharges(state.pulseCharges() - 1);
        state.setSpace(state.space().add(pulseGain()));
        state.setPulses(state.pulses() + 1);
        return true;
    }

    /** Impulsions données depuis le début du jeu. */
    public long pulses() {
        return state.pulses();
    }

    void refillPulses(double dt) {
        if (dt <= 0 || !isPulseUnlocked()) return;
        state.setPulseCharges(Math.min(PULSE_MAX_CHARGES, state.pulseCharges() + dt / PULSE_REFILL_SECONDS));
    }

    // ------------------------------------------------------------------
    // La comète : un passage à saisir
    // ------------------------------------------------------------------

    /** Attente la plus courte et la plus longue entre deux comètes, en secondes de jeu. */
    public static final double COMET_MIN_WAIT = 8 * 60;
    public static final double COMET_MAX_WAIT = 16 * 60;
    /** Temps pendant lequel une comète reste à saisir, en secondes. */
    public static final double COMET_VISIBLE_SECONDS = 45;
    /** Durée du sillage d'une comète saisie, en secondes, et ce par quoi il multiplie l'expansion. */
    public static final double COMET_BOOST_SECONDS = 120;
    public static final double COMET_BOOST = 2;
    boolean cometAppeared = false;

    /**
     * Fait passer le temps de la comète. Après le premier Big Bang, une comète traverse l'expansion
     * de temps en temps ; elle reste {@link #COMET_VISIBLE_SECONDS} secondes, puis s'en va. Saisie
     * ({@link #catchComet()}), elle laisse un sillage qui double l'expansion deux minutes. Qui
     * n'est pas là n'y perd rien : elle n'enlève jamais quoi que ce soit.
     */
    void runComet(double dt) {
        if (dt <= 0 || !state.started() || !isBigBangUnlocked()) return;
        state.setCometBoost(state.cometBoost() - dt);
        double left = dt;
        while (left > 1e-12) {
            if (state.cometVisible() > 0) {
                double used = Math.min(left, state.cometVisible());
                state.setCometVisible(state.cometVisible() - used);
                left -= used;
                if (state.cometVisible() <= 1e-9) {
                    state.setCometVisible(0);
                    state.setCometWait(nextCometWait());
                }
            } else {
                if (state.cometWait() <= 0) state.setCometWait(nextCometWait());
                double used = Math.min(left, state.cometWait());
                state.setCometWait(state.cometWait() - used);
                left -= used;
                if (state.cometWait() <= 1e-9) {
                    state.setCometWait(0);
                    state.setCometVisible(COMET_VISIBLE_SECONDS);
                    cometAppeared = true;
                }
            }
        }
    }

    /**
     * L'attente avant la prochaine comète, entre {@link #COMET_MIN_WAIT} et {@link #COMET_MAX_WAIT}.
     * Elle se déduit du temps de jeu et du nombre de comètes saisies, sans toucher au hasard du
     * jeu : les tirages de la synthèse ne dépendent pas de ce que fait la comète.
     */
    private double nextCometWait() {
        double mix = Math.sin(state.timePlayed() * 12.9898 + state.cometsCaught() * 78.233) * 43758.5453;
        double share = mix - Math.floor(mix);
        return COMET_MIN_WAIT + (COMET_MAX_WAIT - COMET_MIN_WAIT) * share;
    }

    /** Vrai tant qu'une comète traverse l'expansion et peut être saisie. */
    public boolean isCometVisible() {
        return isBigBangUnlocked() && state.cometVisible() > 0;
    }

    /** Secondes pendant lesquelles la comète reste encore à saisir ; 0 s'il n'y en a pas. */
    public double cometVisibleFor() {
        return isCometVisible() ? state.cometVisible() : 0;
    }

    /** Secondes de jeu avant la prochaine comète ; 0 pendant qu'une comète est là, ou avant le premier Big Bang. */
    public double cometWait() {
        return isBigBangUnlocked() && state.cometVisible() <= 0 ? state.cometWait() : 0;
    }

    /**
     * Saisit la comète : son sillage multiplie l'expansion par {@link #COMET_BOOST} pendant
     * {@link #COMET_BOOST_SECONDS} secondes. Une comète saisie pendant un sillage le relance pour sa durée entière.
     *
     * @return {@code true} s'il y avait une comète à saisir
     */
    public boolean catchComet() {
        if (!isCometVisible()) return false;
        state.setCometVisible(0);
        state.setCometsCaught(state.cometsCaught() + 1);
        state.setCometBoost(COMET_BOOST_SECONDS);
        state.setCometWait(nextCometWait());
        return true;
    }

    // ------------------------------------------------------------------
    // Les anomalies du ciel (version 0.127)
    // ------------------------------------------------------------------

    /** Attente entre deux anomalies du ciel, en secondes de jeu : de dix à vingt-cinq minutes. */
    public static final double SKY_EVENT_MIN_WAIT = 10 * 60;
    public static final double SKY_EVENT_MAX_WAIT = 25 * 60;
    /** Temps pendant lequel une anomalie reste à saisir : trois minutes, de quoi la voir sans la guetter. */
    public static final double SKY_EVENT_VISIBLE_SECONDS = 3 * 60;
    /** Le vent de particules : ce par quoi il multiplie les particules, et combien de temps. */
    public static final double WIND_FACTOR = 2;
    public static final double WIND_SECONDS = 120;
    /** La fenêtre d'exploration : ce qu'elle fait gagner aux sondes en vol, en secondes de voyage. */
    public static final double EXPLORATION_SECONDS = 10 * 60;
    boolean skyEventAppeared = false;

    /**
     * Fait passer le temps des anomalies du ciel. Après le premier Big Bang, une anomalie apparaît de temps en temps
     * ({@link #SKY_EVENT_MIN_WAIT} à {@link #SKY_EVENT_MAX_WAIT}), reste {@link #SKY_EVENT_VISIBLE_SECONDS} secondes,
     * puis s'en va. La fenêtre d'exploration ne vient que quand une sonde est en vol ; sinon, c'est un vent de
     * particules. Rien ne se perd à la manquer. Le vent en cours s'écoule avec la production ({@code Game#runTimedBonuses}).
     */
    void runSkyEvents(double dt) {
        if (dt <= 0 || !state.started() || !isBigBangUnlocked()) return;
        double left = dt;
        while (left > 1e-12) {
            if (state.skyEvent() != null) {
                double used = Math.min(left, state.skyEventVisible());
                state.setSkyEventVisible(state.skyEventVisible() - used);
                left -= used;
                if (state.skyEventVisible() <= 1e-9) {
                    state.setSkyEvent(null);
                    state.setSkyEventVisible(0);
                    state.setSkyEventWait(nextSkyEventWait());
                }
            } else {
                if (state.skyEventWait() <= 0) state.setSkyEventWait(nextSkyEventWait());
                double used = Math.min(left, state.skyEventWait());
                state.setSkyEventWait(state.skyEventWait() - used);
                left -= used;
                if (state.skyEventWait() <= 1e-9) {
                    state.setSkyEventWait(0);
                    state.setSkyEvent(nextSkyEvent().name());
                    state.setSkyEventVisible(SKY_EVENT_VISIBLE_SECONDS);
                    skyEventAppeared = true;
                }
            }
        }
    }

    /** Un nombre entre 0 et 1 qui se déduit du temps de jeu et des anomalies saisies, comme l'attente de la comète. */
    private double skyMix(double salt) {
        double mix = Math.sin(state.timePlayed() * 7.137 + state.skyEventsCaught() * 51.77 + salt) * 24634.6345;
        return mix - Math.floor(mix);
    }

    private double nextSkyEventWait() {
        return SKY_EVENT_MIN_WAIT + (SKY_EVENT_MAX_WAIT - SKY_EVENT_MIN_WAIT) * skyMix(0);
    }

    /** La prochaine anomalie : une chance sur deux d'une fenêtre d'exploration quand une sonde est en vol. */
    private idle.core.sky.SkyEvent nextSkyEvent() {
        boolean flying = state.probes().stream().anyMatch(probe -> !probe.landed());
        return flying && skyMix(3.3) < 0.5 ? idle.core.sky.SkyEvent.EXPLORATION_WINDOW : idle.core.sky.SkyEvent.PARTICLE_WIND;
    }

    /** L'anomalie du ciel à saisir en ce moment, ou {@code null}. */
    public idle.core.sky.SkyEvent skyEvent() {
        if (!isBigBangUnlocked() || state.skyEvent() == null) return null;
        for (idle.core.sky.SkyEvent event : idle.core.sky.SkyEvent.values()) {
            if (event.name().equals(state.skyEvent())) return event;
        }
        return null;
    }

    /** Secondes pendant lesquelles l'anomalie reste à saisir ; 0 s'il n'y en a pas. */
    public double skyEventVisibleFor() {
        return skyEvent() == null ? 0 : state.skyEventVisible();
    }

    /**
     * Saisit l'anomalie du ciel. Le vent de particules multiplie les particules par {@link #WIND_FACTOR} pendant
     * {@link #WIND_SECONDS} secondes (relancé pour sa durée entière s'il soufflait déjà). La fenêtre d'exploration
     * fait avancer chaque sonde en vol de {@link #EXPLORATION_SECONDS} secondes ; si plus aucune n'est en vol, elle
     * laisse passer un vent de particules à la place.
     *
     * @return l'anomalie saisie, ou {@code null} s'il n'y en avait pas
     */
    public idle.core.sky.SkyEvent catchSkyEvent() {
        idle.core.sky.SkyEvent event = skyEvent();
        if (event == null) return null;
        boolean explored = false;
        if (event == idle.core.sky.SkyEvent.EXPLORATION_WINDOW) {
            List<idle.core.probes.Probe> moved = new java.util.ArrayList<>();
            for (idle.core.probes.Probe probe : state.probes()) {
                if (probe.landed()) {
                    moved.add(probe);
                } else {
                    explored = true;
                    // Jamais tout à fait zéro : l'arrivée et son butin passent par le cours normal du voyage.
                    moved.add(new idle.core.probes.Probe(probe.destination(), probe.trip(),
                            Math.max(1e-6, probe.left() - EXPLORATION_SECONDS), probe.total(), probe.id()));
                }
            }
            state.setProbes(moved);
        }
        if (!explored) state.setWindLeft(WIND_SECONDS);
        state.setSkyEvent(null);
        state.setSkyEventVisible(0);
        state.setSkyEventsCaught(state.skyEventsCaught() + 1);
        state.setSkyEventWait(nextSkyEventWait());
        return explored ? event : idle.core.sky.SkyEvent.PARTICLE_WIND;
    }

    /** Ce par quoi le vent de particules multiplie les particules en ce moment : {@link #WIND_FACTOR}, ou 1. */
    public double windMultiplier() {
        return state.windLeft() > 0 ? WIND_FACTOR : 1;
    }

    /** Vrai si une anomalie du ciel vient d'apparaître depuis la dernière lecture : pour l'annoncer. */
    public boolean takeSkyEventAppeared() {
        boolean appeared = skyEventAppeared;
        skyEventAppeared = false;
        return appeared;
    }

    /** Ce par quoi le sillage d'une comète multiplie l'expansion en ce moment : {@link #COMET_BOOST}, ou 1. */
    public double cometBoost() {
        return state.cometBoost() > 0 ? COMET_BOOST : 1;
    }

    /** Secondes de sillage qu'il reste. */
    public double cometBoostLeft() {
        return state.cometBoost();
    }

    /** Nombre de comètes saisies depuis le début du jeu. */
    public int cometsCaught() {
        return state.cometsCaught();
    }

    /** Vrai si une comète vient d'apparaître depuis le dernier appel : pour l'annoncer. Remis à faux à chaque lecture. */
    public boolean takeCometAppeared() {
        boolean appeared = cometAppeared;
        cometAppeared = false;
        return appeared;
    }

    // ------------------------------------------------------------------
    // La production
    // ------------------------------------------------------------------

    /** Vitesse commune à tous les générateurs, en créations par seconde, avant leurs bonus individuels. */
    public BigNum speed() {
        double extra = speedExtraPerLevel();
        BigNum speed = BASE_SPEED.multiply(elementMultiplier(ElementEffect.Stat.SPEED));
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplySpeed multiply) {
                speed = speed.multiply(BigNum.of(multiply.perLevel() + extra)
                        .pow(state.levelOf(upgrade.id()) + startingSpeedLevels()));
            }
        }
        double slowdown = firstRunSlowdown();
        if (slowdown > 1) speed = speed.divide(slowdown);
        return speed;
    }

    /** Pour les tests de rythme : remet (ou coupe, avec 1) le premier élan, quelle que soit la propriété système. */
    void setFirstRunPace(double pace) {
        firstRunPace = Math.max(1, pace);
    }

    /**
     * Ce par quoi la création est divisée en ce moment par le premier élan : {@link #FIRST_RUN_PACE}
     * avant le premier atome, {@link #FIRST_ATOMS_PACE} jusqu'au cinquième, {@link #FIRST_GAME_PACE}
     * jusqu'au trentième, 1 ensuite et dès qu'une explosion a eu lieu.
     */
    public double firstRunSlowdown() {
        if (firstRunPace <= 1 || state.stats().timedExplosions() > 0 || state.explosions() > 0) return 1;
        if (isFirstRun()) return FIRST_RUN_PACE;
        if (state.stats().atomsCreated().lt(BigNum.of(FIRST_ATOMS_UNTIL))) return FIRST_ATOMS_PACE;
        if (!hasCreatedEnoughAtoms()) return FIRST_GAME_PACE;
        return 1;
    }

    /** Vrai avant la toute première fusion de la partie : la création n'a pas encore son élan (version 0.095). */
    public boolean isFirstRun() {
        return state.stats().fusions() == 0;
    }

    /** Vitesse d'un générateur (numéroté à partir de 0), en créations par seconde. */
    public BigNum speed(int generator) {
        return runningSpeed().multiply(generatorSpeedMultiplier(generator));
    }

    /**
     * La vitesse commune qui fait vraiment tourner les générateurs : {@link #speed()}, avec le régime rapide et
     * la synergie des rangs du deuxième univers. Avant la version 0.123, ces deux-là ne pressaient que
     * l'animation : la production ne les voyait pas.
     */
    public BigNum runningSpeed() {
        double second = regimeMultiplier() * synergyMultiplier();
        return second == 1 ? speed() : speed().multiply(second);
    }

    /**
     * Niveaux de vitesse qui comptent pour une amélioration de vitesse : ceux achetés, plus ceux
     * offerts par l'arbre de matière noire.
     */
    public int speedLevels(String upgradeId) {
        return state.levelOf(upgrade(upgradeId).id()) + startingSpeedLevels();
    }

    /** Nombre de paliers de vitesse atteints, toutes améliorations de vitesse confondues. */
    public int speedMilestones() {
        int milestones = 0;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplySpeed speed) {
                milestones += speed.milestonesAt(speedLevels(upgrade.id()));
            }
        }
        return milestones;
    }

    /**
     * Ce que les paliers de vitesse multiplient dans les particules de chaque création : ×2 tous
     * les 25 niveaux de « Vitesse de création » dans le jeu. Vaut 1 avant le premier palier.
     */
    public BigNum speedMilestoneMultiplier() {
        BigNum result = BigNum.ONE;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.MultiplySpeed speed && speed.hasMilestones()) {
                result = result.multiply(BigNum.of(speedMilestoneFactor(upgrade.id())).pow(speed.milestonesAt(speedLevels(upgrade.id()))));
            }
        }
        return result;
    }

    /**
     * Ce que multiplie chaque palier de cette amélioration de vitesse : le facteur du catalogue,
     * plus ce qu'y ajoute le défi réussi ; 1 pour une amélioration sans palier.
     */
    public double speedMilestoneFactor(String upgradeId) {
        if (!(upgrade(upgradeId).effect() instanceof Effect.MultiplySpeed speed) || !speed.hasMilestones()) return 1;
        return speed.milestoneFactor() + (rewarded(Challenge.Reward.STRONGER_MILESTONES) ? REWARD_MILESTONE_EXTRA : 0)
                + darkMilestoneExtra();
    }

    /** Ce que l'arbre ajoute au facteur de chaque palier de vitesse (version 0.073) : les voies de présence de l'élan. */
    public double darkMilestoneExtra() {
        return this.<Double>darkMemo("milestoneExtra", () -> {
            double extra = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.StrongerMilestones more) extra += more.extra() * effectLevel(dark);
            }
            return extra;
        });
    }

    /**
     * Niveau (offerts compris) auquel cette amélioration de vitesse atteindra son prochain palier,
     * ou 0 si elle n'a pas de palier.
     */
    public int nextSpeedMilestone(String upgradeId) {
        if (!(upgrade(upgradeId).effect() instanceof Effect.MultiplySpeed speed) || !speed.hasMilestones()) return 0;
        return (speed.milestonesAt(speedLevels(upgradeId)) + 1) * speed.milestoneEvery();
    }

    /**
     * Ce qui s'ajoute au gain de chaque niveau de vitesse (0.02 = +2 points), grâce aux
     * améliorations en atomes et aux éléments.
     */
    public double speedExtraPerLevel() {
        double fromUpgrades = 0;
        double fromElements = bonuses().upgradeBoost(ElementEffect.UpgradeTarget.SPEED);
        double catalystBoost = bonuses().upgradeBoost(ElementEffect.UpgradeTarget.CATALYST);
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.StrengthenSpeed strengthen) {
                fromUpgrades += strengthen.extraPerLevel() * state.levelOf(upgrade.id());
                fromElements += catalystBoost * state.levelOf(upgrade.id());
            }
        }
        return fromUpgrades + Math.min(fromElements, MAX_ELEMENT_SPEED_EXTRA);
    }

    /** Facteur appliqué au coût des générateurs par les améliorations en atomes (0.64 = −36 %). */
    public double generatorCostFactor() {
        double factor = 1;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.DiscountGenerators discount) {
                factor *= Math.pow(discount.factorPerLevel(), state.levelOf(upgrade.id()));
            }
        }
        return factor * Math.max(0.5, 1 - achievementBonus(Achievement.Bonus.GENERATOR_COST));
    }

    /** Vrai si la fusion conserve les améliorations payées en particules (hors générateurs). */
    public boolean keepsUpgradesOnFusion() {
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.KeepUpgradesOnFusion && state.levelOf(upgrade.id()) > 0) {
                return true;
            }
        }
        return false;
    }

    /** Nombre de générateurs actifs : aucun avant {@link #start()}, puis le premier plus ceux achetés. */
    public int generatorCount() {
        if (!state.started()) return 0;
        int count = 1 + startingGenerators();
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.AddGenerator) {
                count += state.levelOf(upgrade.id());
            }
        }
        return Math.min(count, maxGeneratorCount());
    }

    /**
     * Générateurs offerts au départ de chaque partie par l'arbre de matière noire, en plus du
     * premier. Jamais plus qu'il n'en faut pour fusionner.
     */
    public int startingGenerators() {
        if (inChallenge()) return 0;
        int bonus = this.<Integer>darkMemo("startingGenerators", () -> {
            int sum = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.StartingGenerators start) {
                    sum += start.perLevel() * effectLevel(dark);
                }
            }
            return sum;
        });
        return Math.min(bonus, generatorsPerAtom() - 1);
    }

    /**
     * Nombre maximal de générateurs : celui qu'il faut pour fusionner, ou la limite repoussée par
     * l'arbre de matière noire.
     */
    public int maxGeneratorCount() {
        int uncapped = this.<Integer>darkMemo("maxGenerators", () -> {
            int limit = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (dark.effect() instanceof DarkEffect.UncapGenerators uncap && effectLevel(dark) > 0) {
                    limit = Math.max(limit, uncap.limit());
                }
            }
            return limit;
        });
        return Math.max(generatorsPerAtom(), uncapped);
    }

    /** Générateurs du catalogue (le premier compris) : le catalogue ne change pas, on ne les compte qu'une fois. */
    private int catalogGenerators = 0;

    /**
     * Nombre de générateurs qu'il faut pour fusionner, et que consomme chaque atome de base :
     * le premier, plus tous ceux des améliorations du catalogue (dix dans le jeu).
     */
    public int generatorsPerAtom() {
        if (catalogGenerators == 0) {
            int count = 1;
            for (Upgrade upgrade : upgrades.values()) {
                if (upgrade.effect() instanceof Effect.AddGenerator && upgrade.hasLimit()) {
                    count += upgrade.maxLevel();
                }
            }
            catalogGenerators = count;
        }
        int count = catalogGenerators;
        return ruled(Challenge.Rule.HEAVY_FUSION) ? count * HEAVY_FUSION_FACTOR : count;
    }

    /**
     * Particules obtenues à chaque création, avant les bonus propres à chaque générateur :
     * une de base, multipliée par tous les bonus achetés, par les paliers de vitesse et par les
     * bonus des éléments.
     */
    public BigNum particlesPerCreation() {
        return particlesWith(generatorCount());
    }

    /** Particules par création s'il y avait {@code generators} générateurs : seul le couplage en dépend. */
    private BigNum particlesWith(int generators) {
        BigNum raw = rawParticlesWith(generators);
        double power = systemsPower();
        // Le vent de particules (version 0.127) agit après les puissances : il double vraiment la création, sans
        // s'élever avec elles.
        if (power <= 0) return raw.multiply(windMultiplier());
        double base = speed().log10() + raw.log10();
        return (base > 0 ? raw.multiply(BigNum.pow10(power * base)) : raw).multiply(windMultiplier());
    }

    // ------------------------------------------------------------------
    // La puissance des grands systèmes (version 0.124)
    // ------------------------------------------------------------------


    /**
     * Ce que chaque grand système ajoute, au plus, à l'exposant de la production (version 0.124) : la production
     * devient {@code (vitesse × particules par création)^(1 + puissances)}. C'est ce qui donne au tableau
     * périodique, au ciel, au collisionneur et aux succès une part réelle de la production, et qui la garde quand
     * la partie grandit : une puissance pèse toujours la même part des ordres de grandeur.
     */
    public static final double POWER_TABLE = 2.0, POWER_SKY = 4.5,
            POWER_COLLIDER = 0.35, POWER_ACHIEVEMENTS = 0.1;
    /** Ce qui reste de la puissance du ciel dans l'univers vide (version 0.124). */
    public static final double VOID_SKY_POWER = 0.5;

    /** Les repères de la puissance du ciel et du collisionneur. */
    public static final double SKY_POWER_FULL = 12, COLLIDER_POWER_DECADES = 20;

    private int copiesCacheVersion = -1;
    private double copiesShare = 0;

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
            copiesShare = mix * Math.min(1, distinct / (double) idle.core.elements.PeriodicTable.ELEMENTS.size()) + (1 - mix) * copies;
            copiesCacheVersion = state.elementsVersion();
        }
        return POWER_TABLE * copiesShare * tableMaturity();
    }

    /**
     * La part des éléments différents possédés dans le remplissage du tableau (version 0.125), le reste venant des
     * exemplaires : un joueur qui explose dès que les 118 sont là, ou dont les molécules consomment les exemplaires,
     * garde ainsi l'essentiel de la puissance du tableau.
     */
    public static final double TABLE_DISTINCT_WEIGHT = 0.8;
    /** Explosions (dans l'univers en cours) qu'il faut pour que la puissance du tableau soit entière ; 0 : tout de suite. */
    public static final double TABLE_POWER_EXPLOSIONS = 300;
    /**
     * Les mêmes, dans les univers suivants : le tableau y mûrit plus vite. À 300, le deuxième acte du deuxième univers
     * durait deux heures de plus que dans la version 0.123 ; mûr d'emblée, il ne durait plus que dix minutes.
     */
    public static final double TABLE_POWER_EXPLOSIONS_LATER = 200;

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
        double voidFactor = bangRuled(idle.core.bigbang.BangChallenge.VOID) ? VOID_SKY_POWER : 1;
        return POWER_SKY * voidFactor * Math.min(1, progress / SKY_POWER_FULL);
    }

    /** La puissance du collisionneur : selon les ordres de grandeur de l'énergie produite, pleine à {@link #COLLIDER_POWER_DECADES}. */
    public double colliderPower() {
        if (powersOff) return 0;
        if (!isColliderUnlocked()) return 0;
        return POWER_COLLIDER * Math.min(1, Math.log10(1 + colliderEnergyProduced()) / COLLIDER_POWER_DECADES);
    }

    /** La puissance des succès : au prorata des succès obtenus. */
    public double achievementPower() {
        if (powersOff) return 0;
        int all = achievements().size();
        return all == 0 ? 0 : POWER_ACHIEVEMENTS * achievementCount() / all;
    }

    /**
     * L'essoufflement de la vitesse (version 0.124) : le prix d'un niveau au-delà de {@link #SPEED_SCALING_FROM} est
     * multiplié par {@code 10^(SPEED_SCALING × (niveau − SPEED_SCALING_FROM)²)}. Sans lui, chaque niveau rapportait presque
     * ce qu'il coûtait : l'automatisme en rachetait sans fin, et tout autre multiplicateur était amplifié douze fois au profit
     * de la vitesse. Version 0.125 : à partir de 300 niveaux (jamais atteints au premier acte), 10⁻³ au lieu de 10⁻⁵ ; à
     * 10⁻⁵, le Speedrunner montait la vitesse à 150 000 niveaux.
     */
    public static final int SPEED_SCALING_FROM = 300;
    public static final double SPEED_SCALING = 1e-3;

    /**
     * Les tests qui vérifient une règle sans les puissances les coupent avec la propriété système
     * {@code idle.sansPuissances} ; le jeu, les robots et les tests des puissances les gardent.
     */
    static boolean powersOff = Boolean.getBoolean("idle.sansPuissances");

    /** Toutes les puissances ensemble. */
    public double systemsPower() {
        return tablePower() + skyPower() + colliderPower() + achievementPower();
    }

    /** Les ordres de grandeur sur lesquels les puissances agissent en ce moment : vitesse × particules par création, sans elles. */
    public double powerBase() {
        return Math.max(0, speed().log10() + rawParticlesWith(generatorCount()).log10());
    }

    private BigNum rawParticlesWith(int generators) {
        BigNum result = BigNum.of(elementMultiplier(ElementEffect.Stat.PARTICLES))
                .multiply(darkParticlesMultiplier())
                .multiply(landmarkParticlesMultiplier())
                .multiply(achievementParticlesMultiplier())
                .multiply(moleculeBoost(Molecule.Stat.PARTICLES))
                .multiply(speedMilestoneMultiplier())
                .multiply(colliderParticlesMultiplier())
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
     * Multiplicateur de particules qu'apporte une amélioration à un niveau donné, dans la
     * situation actuelle (atomes créés, temps écoulé, générateurs, éléments qui la renforcent).
     * Vaut 1 au niveau 0, et toujours 1 pour une amélioration qui n'agit pas sur les particules
     * par création.
     *
     * <p>L'interface s'en sert pour afficher l'effet actuel d'une amélioration, ou ce
     * qu'elle donnerait une fois achetée.
     */
    public BigNum particlesMultiplier(String upgradeId, int level) {
        return particlesMultiplier(upgrade(upgradeId), level, generatorCount());
    }

    private BigNum particlesMultiplier(Upgrade upgrade, int level, int generators) {
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
            case Effect.MultiplyByGenerators coupling ->
                    BigNum.of(1 + coupling.perGenerator() * level * (inTrial(idle.core.universe.Trial.LONE_GENERATOR)
                            ? 2 * (generatorsPerAtom() - 1) : Math.max(0, generators - 1)));
            // Les paliers de vitesse sont comptés à part : ils tiennent compte des niveaux offerts.
            case Effect.MultiplySpeed speed -> BigNum.ONE;
            case Effect.AddGenerator generator -> BigNum.ONE;
            case Effect.StrengthenSpeed strengthen -> BigNum.ONE;
            case Effect.DiscountGenerators discount -> BigNum.ONE;
            case Effect.KeepUpgradesOnFusion keep -> BigNum.ONE;
            case Effect.BulkAtomBuy bulk -> BigNum.ONE;
            case Effect.MultiplyAtomsByProduction byProduction -> BigNum.ONE;
        };
    }

    /** Particules créées par seconde, tous générateurs confondus. */
    public BigNum productionPerSecond() {
        return production(generatorCount());
    }

    /**
     * Particules créées par seconde une fois tous les générateurs débloqués : la production
     * au moment d'une fusion. C'est elle que regarde {@link #fusionYield()}.
     */
    public BigNum productionAtFusion() {
        return production(Math.max(generatorCount(), generatorsPerAtom()));
    }

    /** Production des {@code generators} premiers générateurs, avec les bonus actuels. */
    private BigNum production(int generators) {
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
     * Atomes gagnés à la prochaine fusion : {@link #ATOMS_PER_FUSION} plus ceux de l'arbre de
     * matière noire, multipliés par le bonus des éléments, par le
     * {@linkplain #fusionYield() rendement de fusion}, par le nombre de groupes de générateurs
     * ({@link #fusionGroups()}) et par leur {@linkplain #fusionGroupBonus() prime}. Le résultat peut être fractionnaire : les fractions s'accumulent
     * d'une fusion à l'autre.
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
                .multiply(cycleChoice() == idle.core.universe.CycleChoice.FUSION ? 1 + idle.core.universe.CycleChoice.FUSION_GAIN : 1)
                .multiply(contractionAtomsMultiplier());
    }

    /**
     * La prime de groupe : une fusion de plusieurs groupes de générateurs rapporte plus que la
     * somme des groupes, {@link #FUSION_GROUP_BONUS} de plus par groupe au-delà du premier
     * (×1,1 avec deux groupes, ×1,9 avec dix). C'est ce qui rend l'attente payante quand le
     * nombre de générateurs n'est plus limité à dix.
     */
    public double fusionGroupBonus() {
        return 1 + fusionGroupBonusPerGroup() * (fusionGroups() - 1);
    }

    /** Ce que chaque groupe au-delà du premier ajoute à la fusion : {@link #FUSION_GROUP_BONUS}, ou plus après le défi réussi. */
    public double fusionGroupBonusPerGroup() {
        return rewarded(Challenge.Reward.GROUP_BONUS) ? REWARD_GROUP_BONUS : FUSION_GROUP_BONUS;
    }

    /**
     * Nombre de groupes complets de générateurs qu'une fusion consommerait maintenant : un tant
     * que leur nombre est limité, deux avec vingt générateurs, etc. Au moins un.
     */
    public int fusionGroups() {
        return Math.max(1, generatorCount() / generatorsPerAtom());
    }

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
     * Vrai quand tous les générateurs sont débloqués et qu'il reste de la place pour un atome :
     * ils peuvent fusionner.
     */
    public boolean canFuse() {
        return hasAllGenerators() && !isAtomCapReached();
    }

    /** Vrai quand tous les générateurs sont débloqués, que la fusion soit possible ou non. */
    public boolean hasAllGenerators() {
        return state.started() && generatorCount() >= generatorsPerAtom();
    }

    /**
     * Vrai quand le joueur possède déjà {@link #MAX_ATOMS} atomes : aucune fusion tant qu'il n'en
     * dépense pas. Toujours faux une fois le plafond levé par l'arbre de matière noire.
     */
    public boolean isAtomCapReached() {
        return !isAtomCapLifted() && state.atoms().gte(atomCap());
    }

    /**
     * Fusionne tous les générateurs en un atome.
     *
     * <p>Impossible tant qu'il manque des générateurs, ou si le joueur possède déjà
     * {@link #MAX_ATOMS} atomes. Sinon il gagne {@link #atomsPerFusion()} et repart du début : un
     * seul générateur, plus aucune particule, plus aucune amélioration payée en particules, sauf
     * s'il possède une amélioration {@link Effect.KeepUpgradesOnFusion}. Les améliorations payées
     * en atomes sont toujours conservées.
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
        // Les skins (version 0.088) : la reconstruction se chronomètre, la contraction compte ses fusions.
        if (isSkinsUnlocked()) state.setRebuildPending(true);
        if (inTrial(idle.core.universe.Trial.SOFT_CONTRACTION)) state.setTrialCount(state.trialCount() + 1);
        state.setTotalAtoms(state.totalAtoms().add(gained));
        state.setParticles(BigNum.ZERO);
        state.clearFormations();
        state.setTimeSinceFusion(0);
        boolean keep = keepsUpgradesOnFusion();
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.resource() != Resource.PARTICLES) continue;
            // Les générateurs sont consommés par la fusion, quoi qu'il arrive.
            boolean fused = upgrade.effect() instanceof Effect.AddGenerator;
            if (fused || !keep) {
                state.setLevel(upgrade.id(), 0);
            }
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
    public Collection<Upgrade> upgrades() {
        return upgrades.values();
    }

    /** Les améliorations qui se paient avec une ressource donnée, dans l'ordre du catalogue. */
    public List<Upgrade> upgrades(Resource resource) {
        return upgrades.values().stream().filter(upgrade -> upgrade.resource() == resource).toList();
    }

    public int levelOf(String upgradeId) {
        return state.levelOf(upgrade(upgradeId).id());
    }

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

    /** Coût du niveau acheté quand on en possède {@code level}, toutes réductions comprises. */
    private BigNum costAt(Upgrade upgrade, int level) {
        BigNum cost = upgrade.costAt(level);
        // La vitesse s'essouffle (version 0.124) : au-delà de SPEED_SCALING_FROM niveaux, chaque niveau coûte de plus en plus.
        if (upgrade.effect() instanceof Effect.MultiplySpeed && level > SPEED_SCALING_FROM) {
            double over = level - SPEED_SCALING_FROM;
            cost = cost.multiply(BigNum.pow10(SPEED_SCALING * over * over));
        }
        ElementBonuses bonuses = bonuses();
        double factor = 1;
        if (upgrade.effect() instanceof Effect.AddGenerator) {
            factor = generatorCostFactor();
        } else if (upgrade.effect() instanceof Effect.MultiplySpeed) {
            factor = 1 / bonuses.costDivisor(ElementEffect.CostTarget.SPEED_UPGRADES);
        } else if (upgrade.resource() == Resource.ATOMS) {
            factor = 1 / bonuses.costDivisor(ElementEffect.CostTarget.ATOM_UPGRADES);
            if (rewarded(Challenge.Reward.CHEAPER_ATOM_UPGRADES)) factor /= REWARD_ATOM_UPGRADE_DIVISOR;
        }
        if (factor != 1) {
            cost = Upgrade.roundUp(cost.multiply(factor)).max(BigNum.ONE);
        }
        return cost;
    }

    /** Niveaux qu'il reste à acheter avant le maximum de cette amélioration ({@link #MAX_BULK} au plus). */
    private int levelsLeft(Upgrade upgrade) {
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

    public boolean canBuy(String upgradeId) {
        Upgrade upgrade = upgrade(upgradeId);
        // L'épreuve du premier générateur seul (version 0.088) : pas d'autre générateur.
        if (upgrade.effect() instanceof Effect.AddGenerator && inTrial(idle.core.universe.Trial.LONE_GENERATOR)) return false;
        return state.started() && !isMaxed(upgradeId) && balance(upgrade.resource()).gte(costOf(upgradeId));
    }

    /**
     * Achète un niveau si la partie a démarré, que le joueur a de quoi payer
     * et que le maximum n'est pas atteint.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean buy(String upgradeId) {
        if (!canBuy(upgradeId)) return false;
        Upgrade upgrade = upgrade(upgradeId);
        // max(0) : la soustraction de deux double proches peut donner -1e-15 au lieu de 0
        BigNum cost = costOf(upgradeId);
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
        // La fuite et le calme du Big Bang (version 0.114) repartent de zéro à chaque amélioration achetée par le
        // joueur ; celles des automatismes ne comptent pas : on peut s'absenter sans couper l'automatisation.
        if (!automating) state.setUpgradeCalm(0);
        // Le calme du Big Bang (version 0.124) : tout achat le rompt, celui des automatismes aussi.
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
        while (bought < count && bought < MAX_BULK && buy(upgradeId)) {
            bought++;
        }
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

    public GameState state() {
        return state;
    }

    Automation automation(String automationId) {
        Automation automation = automations.get(automationId);
        if (automation == null) throw new IllegalArgumentException("Automatisme inconnu : " + automationId);
        return automation;
    }

    DarkAutomation darkAutomation(String darkAutomationId) {
        DarkAutomation automation = darkAutomations.get(darkAutomationId);
        if (automation == null) throw new IllegalArgumentException("Automatisme de matière noire inconnu : " + darkAutomationId);
        return automation;
    }

    DarkUpgrade darkUpgrade(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrades.get(darkUpgradeId);
        if (dark == null) throw new IllegalArgumentException("Amélioration de matière noire inconnue : " + darkUpgradeId);
        return dark;
    }

    Upgrade upgrade(String upgradeId) {
        Upgrade upgrade = upgrades.get(upgradeId);
        if (upgrade == null) throw new IllegalArgumentException("Amélioration inconnue : " + upgradeId);
        return upgrade;
    }
}

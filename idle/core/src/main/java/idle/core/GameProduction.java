package idle.core;

import idle.core.state.*;
import idle.core.bigbang.*;
import idle.core.collider.Hadron;
import idle.core.probes.Probe;
import idle.core.sky.SkyEvent;
import idle.core.universe.*;
import idle.core.achievements.*;
import idle.core.darkmatter.*;
import idle.core.elements.*;
import idle.core.math.BigNum;
import idle.core.upgrades.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Une partie de {@link GameParticles} (découpée en 0.293 pour rester sous 1 000 lignes) : l'impulsion, la comète, les anomalies du ciel et la production. */
public abstract class GameProduction extends GameMatterAutomation {
    // Ce que fournissent les parties suivantes.
    abstract BigNum rawParticlesWith(int generators);
    abstract Upgrade upgrade(String upgradeId);
    abstract public BigNum particlesPerCreation(int generator);
    abstract public double systemsPower();

    GameProduction(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    abstract void noteHeldPeaks();

    public abstract BigNum landmarkParticlesMultiplier();

    public abstract double landmarkAtomsMultiplier();

    abstract boolean expandDarkMatter(double seconds);

    // ------------------------------------------------------------------
    // L'impulsion : le clic du joueur actif
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
    public boolean isPulseUnlocked() { return state.started() && isBigBangUnlocked(); }

    /** Charges entières disponibles. */
    public int pulseCharges() { return (int) Math.floor(state.pulseCharges() + 1e-9); }

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
    public long pulses() { return state.pulses(); }

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
    double nextCometWait() {
        double mix = Math.sin(state.timePlayed() * 12.9898 + state.cometsCaught() * 78.233) * 43758.5453;
        double share = mix - Math.floor(mix);
        return COMET_MIN_WAIT + (COMET_MAX_WAIT - COMET_MIN_WAIT) * share;
    }

    /** Vrai tant qu'une comète traverse l'expansion et peut être saisie. */
    public boolean isCometVisible() { return isBigBangUnlocked() && state.cometVisible() > 0; }

    /** Secondes pendant lesquelles la comète reste encore à saisir ; 0 s'il n'y en a pas. */
    public double cometVisibleFor() { return isCometVisible() ? state.cometVisible() : 0; }

    /** Secondes de jeu avant la prochaine comète ; 0 pendant qu'une comète est là, ou avant le premier Big Bang. */
    public double cometWait() { return isBigBangUnlocked() && state.cometVisible() <= 0 ? state.cometWait() : 0; }

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
    // Les anomalies du ciel
    // ------------------------------------------------------------------

    /** Attente entre deux anomalies du ciel, en secondes de jeu : de dix à vingt-cinq minutes. */
    static final double SKY_EVENT_MIN_WAIT = 10 * 60;

    static final double SKY_EVENT_MAX_WAIT = 25 * 60;

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
    double skyMix(double salt) {
        double mix = Math.sin(state.timePlayed() * 7.137 + state.skyEventsCaught() * 51.77 + salt) * 24634.6345;
        return mix - Math.floor(mix);
    }

    double nextSkyEventWait() {
        return SKY_EVENT_MIN_WAIT + (SKY_EVENT_MAX_WAIT - SKY_EVENT_MIN_WAIT) * skyMix(0);
    }

    /** La prochaine anomalie : une chance sur deux d'une fenêtre d'exploration quand une sonde est en vol. */
    SkyEvent nextSkyEvent() {
        boolean flying = state.probes().stream().anyMatch(probe -> !probe.landed());
        return flying && skyMix(3.3) < 0.5 ? SkyEvent.EXPLORATION_WINDOW : SkyEvent.PARTICLE_WIND;
    }

    /** L'anomalie du ciel à saisir en ce moment, ou {@code null}. */
    public SkyEvent skyEvent() {
        if (!isBigBangUnlocked() || state.skyEvent() == null) return null;
        for (SkyEvent event : SkyEvent.values()) {
            if (event.name().equals(state.skyEvent())) return event;
        }
        return null;
    }

    /** Secondes pendant lesquelles l'anomalie reste à saisir ; 0 s'il n'y en a pas. */
    public double skyEventVisibleFor() { return skyEvent() == null ? 0 : state.skyEventVisible(); }

    /**
     * Saisit l'anomalie du ciel. Le vent de particules multiplie les particules par {@link #WIND_FACTOR} pendant
     * {@link #WIND_SECONDS} secondes (relancé pour sa durée entière s'il soufflait déjà). La fenêtre d'exploration
     * fait avancer chaque sonde en vol de {@link #EXPLORATION_SECONDS} secondes ; si plus aucune n'est en vol, elle
     * laisse passer un vent de particules à la place.
     *
     * @return l'anomalie saisie, ou {@code null} s'il n'y en avait pas
     */
    public SkyEvent catchSkyEvent() {
        SkyEvent event = skyEvent();
        if (event == null) return null;
        boolean explored = false;
        if (event == SkyEvent.EXPLORATION_WINDOW) {
            List<Probe> moved = new ArrayList<>();
            for (Probe probe : state.probes()) {
                if (probe.landed()) {
                    moved.add(probe);
                } else {
                    explored = true;
                    // Jamais tout à fait zéro : l'arrivée et son butin passent par le cours normal du voyage.
                    moved.add(new Probe(probe.destination(), probe.trip(),
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
        return explored ? event : SkyEvent.PARTICLE_WIND;
    }

    /** Ce par quoi le vent de particules multiplie les particules en ce moment : {@link #WIND_FACTOR}, ou 1. */
    public double windMultiplier() { return state.windLeft() > 0 ? WIND_FACTOR : 1; }

    /** Vrai si une anomalie du ciel vient d'apparaître depuis la dernière lecture : pour l'annoncer. */
    public boolean takeSkyEventAppeared() {
        boolean appeared = skyEventAppeared;
        skyEventAppeared = false;
        return appeared;
    }

    /** Ce par quoi le sillage d'une comète multiplie l'expansion en ce moment : {@link #COMET_BOOST}, ou 1. */
    public double cometBoost() { return state.cometBoost() > 0 ? COMET_BOOST : 1; }

    /** Secondes de sillage qu'il reste. */
    public double cometBoostLeft() { return state.cometBoost(); }

    /** Nombre de comètes saisies depuis le début du jeu. */
    public int cometsCaught() { return state.cometsCaught(); }

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
    void setFirstRunPace(double pace) { firstRunPace = Math.max(1, pace); }

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

    /** Vrai avant la toute première fusion de la partie : la création n'a pas encore son élan. */
    boolean isFirstRun() { return state.stats().fusions() == 0; }

    /** Vitesse d'un générateur (numéroté à partir de 0), en créations par seconde. */
    public BigNum speed(int generator) { return runningSpeed().multiply(generatorSpeedMultiplier(generator)); }

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
    public int speedLevels(String upgradeId) { return state.levelOf(upgrade(upgradeId).id()) + startingSpeedLevels(); }

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

    /** Ce que l'arbre ajoute au facteur de chaque palier de vitesse : les voies de présence de l'élan. */
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
        // Les mésons B baissent le prix des générateurs.
        return factor * Math.max(0.5, 1 - achievementBonus(Achievement.Bonus.GENERATOR_COST)) / hadronBoost(Hadron.B_MESON);
    }

    /** Vrai si la fusion conserve au moins une part des améliorations payées en particules (hors générateurs). */
    public boolean keepsUpgradesOnFusion() { return fusionKeepShare() > 0; }

    /** La part des améliorations payées en particules que la fusion garde, de 0 à 1, selon le niveau de Persistance. */
    public double fusionKeepShare() {
        double share = 0;
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.KeepUpgradesOnFusion keep) share = Math.max(share, keep.shareAt(state.levelOf(upgrade.id())));
        }
        return share;
    }

    /** Nombre de générateurs actifs : aucun avant {@link #start()}, puis le premier plus ceux achetés. */
    public int generatorCount() {
        if (!state.started()) return 0;
        int count = 1 + startingGenerators();
        for (Upgrade upgrade : upgrades.values()) {
            if (upgrade.effect() instanceof Effect.AddGenerator) count += state.levelOf(upgrade.id());
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
        // La bulle du jeune arbre, à droite : plus de plafond, le prix s'en charge (bubbleGeneratorScaling).
        if (hasBubble(GameUniverse.TreeBubble.YOUNG_RIGHT)) return BUBBLE_GENERATOR_LIMIT;
        return generatorCap();
    }

    /** Le plafond « sans fin » de la bulle du jeune arbre : un garde-fou, que le prix rend hors d'atteinte. */
    public static final int BUBBLE_GENERATOR_LIMIT = 1_000_000;

    /**
     * Au-delà de l'ancien plafond ({@link #generatorCap()}), le prix d'un générateur est multiplié par
     * {@code 10^(BUBBLE_GENERATOR_SCALING × n²)}, n les générateurs au-delà : ×10 au dixième, ×10¹⁰⁰ au centième.
     */
    public static final double BUBBLE_GENERATOR_SCALING = 0.01;

    /** Le plafond de générateurs sans la bulle du jeune arbre : celui du catalogue, ou celui de l'arbre de matière noire. */
    public int generatorCap() {
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
    int catalogGenerators = 0;

    /**
     * Nombre de générateurs qu'il faut pour fusionner, et que consomme chaque atome de base :
     * le premier, plus tous ceux des améliorations du catalogue (dix dans le jeu).
     */
    public int generatorsPerAtom() {
        if (catalogGenerators == 0) {
            int count = 1;
            for (Upgrade upgrade : upgrades.values()) {
                if (upgrade.effect() instanceof Effect.AddGenerator && upgrade.hasLimit()) count += upgrade.maxLevel();
            }
            catalogGenerators = count;
        }
        int count = catalogGenerators;
        return ruled(Challenge.Rule.HEAVY_FUSION) ? count * HEAVY_FUSION_FACTOR : count;
    }

    /**
     * Particules obtenues à chaque création, avant les bonus propres à chaque générateur : une de base, multipliée par
     * tous les bonus achetés, par les paliers de vitesse et par les bonus des éléments.
     */
    public BigNum particlesPerCreation() { return particlesWith(generatorCount()); }

    /** Particules par création s'il y avait {@code generators} générateurs : seul le couplage en dépend. */
    BigNum particlesWith(int generators) {
        BigNum raw = rawParticlesWith(generators);
        double power = systemsPower();
        // Le vent de particules agit après les puissances : il double vraiment la création, sans s'élever avec elles.
        if (power <= 0) return raw.multiply(windMultiplier());
        double base = speed().log10() + raw.log10();
        return (base > 0 ? raw.multiply(BigNum.pow10(power * base)) : raw).multiply(windMultiplier());
    }

    // ------------------------------------------------------------------
    // La puissance des grands systèmes
    // ------------------------------------------------------------------

    /**
     * Ce que chaque grand système ajoute, au plus, à l'exposant de la production : la production
     * devient {@code (vitesse × particules par création)^(1 + puissances)}. C'est ce qui donne au tableau
     * périodique, au ciel, au collisionneur et aux succès une part réelle de la production, et qui la garde quand
     * la partie grandit : une puissance pèse toujours la même part des ordres de grandeur.
     */
    public static final double POWER_TABLE = 2.0, POWER_SKY = 4.5, POWER_COLLIDER = 0.35, POWER_ACHIEVEMENTS = 0.1;

    /** Ce qui reste de la puissance du ciel dans l'univers vide. */
    static final double VOID_SKY_POWER = 0.5;

    /** Les repères de la puissance du ciel et du collisionneur. */
    public static final double SKY_POWER_FULL = 12, COLLIDER_POWER_DECADES = 20;

    int copiesCacheVersion = -1;

    double copiesShare = 0;

    /**
     * La part des éléments différents possédés dans le remplissage du tableau, le reste venant des
     * exemplaires : un joueur qui explose dès que les 118 sont là, ou dont les molécules consomment les exemplaires,
     * garde ainsi l'essentiel de la puissance du tableau.
     */
    static final double TABLE_DISTINCT_WEIGHT = 0.8;

    /** Explosions (dans l'univers en cours) qu'il faut pour que la puissance du tableau soit entière ; 0 : tout de suite. */
    static final double TABLE_POWER_EXPLOSIONS = 300;

    /**
     * Les mêmes, dans les univers suivants : le tableau y mûrit plus vite. À 300, le deuxième acte du deuxième univers
     * durait deux heures de plus que dans la version 0.123 ; mûr d'emblée, il ne durait plus que dix minutes.
     */
    static final double TABLE_POWER_EXPLOSIONS_LATER = 200;

    /**
     * Les succès qu'il faut pour la pleine puissance des succès : les 67 du catalogue d'avant. Les 32
     * succès ajoutés en 0.174 ne tombent pas dans le premier univers ; rapportée aux 99 du catalogue, la puissance y
     * baissait d'un tiers, et le Speedrunner formait son premier univers 37 min plus tard (26:36 au lieu de 25:59).
     */
    public static final int POWER_ACHIEVEMENTS_FULL = 67;

    /**
     * L'essoufflement de la vitesse : le prix d'un niveau au-delà de {@link #SPEED_SCALING_FROM} est
     * multiplié par {@code 10^(SPEED_SCALING × (niveau − SPEED_SCALING_FROM)²)}. Sans lui, chaque niveau rapportait presque
     * ce qu'il coûtait : l'automatisme en rachetait sans fin, et tout autre multiplicateur était amplifié douze fois au profit
     * de la vitesse. À partir de 300 niveaux (jamais atteints au premier acte), 10⁻³ au lieu de 10⁻⁵ ; à
     * 10⁻⁵, le Speedrunner montait la vitesse à 150 000 niveaux.
     */
    public static final int SPEED_SCALING_FROM = 300;

    public static final double SPEED_SCALING = 1e-3;

    /**
     * Les tests qui vérifient une règle sans les puissances les coupent avec la propriété système
     * {@code idle.sansPuissances} ; le jeu, les robots et les tests des puissances les gardent.
     */
    static boolean powersOff = Boolean.getBoolean("idle.sansPuissances");

    /** L'entrée du catalogue sous cet identifiant ; {@link IllegalArgumentException} s'il n'y en a pas, avec {@code message} suivi de l'identifiant. */
    static <T> T require(Map<String, T> catalog, String id, String message) {
        T found = catalog.get(id);
        if (found == null) throw new IllegalArgumentException(message + id);
        return found;
    }
}

package idle.core;

import idle.core.achievements.Achievement;
import idle.core.bigbang.BigBangMilestone;
import idle.core.bigbang.BigBangMilestones;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Quark;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.ShoppingList;
import idle.core.probes.Probe;
import idle.core.probes.ProbeDestination;
import idle.core.probes.ProbeReward;
import idle.core.probes.ProbeTrip;
import idle.core.probes.ProbeUpgrade;
import idle.core.sky.Body;
import idle.core.sky.SolarSystem;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Les sondes, la maturation des astres et les systèmes solaires.
 */
public abstract class GameSystems extends GameBase {

    GameSystems(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    public abstract boolean isBigBangMilestoneReached(BigBangMilestone milestone);
    public abstract boolean isBigBangUnlocked();
    public abstract double bangReward(idle.core.bigbang.BangChallenge.Reward reward);
    public abstract List<Molecule> molecules();
    public abstract boolean isMoleculeKindUnlocked(Molecule.Kind kind);
    public abstract ShoppingList shoppingList();
    public abstract Body body(String bodyId);
    public abstract BigNum steadySpacePerSecond();
    public abstract int colliderUpgradeLevel(ColliderUpgrade upgrade);

    // ------------------------------------------------------------------
    // Les sondes (version 0.052) : des voyages dont le butin se réclame d'un clic
    // ------------------------------------------------------------------

    /** Secondes d'expansion que vaut une récompense d'espace de valeur 1 (un saut court). */
    public static final double PROBE_SPACE_SECONDS = 120;
    /** Exemplaires d'élément que vaut une récompense d'éléments de valeur 1. */
    public static final double PROBE_ELEMENTS = 3;
    /** Durée d'un sursaut d'expansion de valeur 1, avant les réserves d'énergie. */
    public static final double PROBE_BOOST_SECONDS = 300;
    /** Ce par quoi un sursaut multiplie l'espace par seconde. */
    public static final double PROBE_BOOST_FACTOR = 2;

    private final List<ProbeReward> returnedProbes = new ArrayList<>();

    /** Vrai une fois les sondes ouvertes : au palier du troisième Big Bang. */
    public boolean isProbesUnlocked() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.Probes) return true;
            }
        }
        return false;
    }

    /** Sondes en vol à la fois : une, plus le hangar. */
    public int probeSlots() {
        int slots = 1 + state.probeUpgradeLevel(ProbeUpgrade.HANGAR);
        for (BigBangMilestone.MorePlaces more : reachedMorePlaces()) slots += more.probes();
        return slots;
    }

    /** Vrai si cette destination est ouverte : le vide toujours, les autres avec leur rayon. */
    public boolean isProbeDestinationOpen(ProbeDestination destination) {
        return destination.kind() == null || isMoleculeKindUnlocked(destination.kind());
    }

    /** Durée d'un voyage, moteurs compris. */
    public double probeDuration(ProbeTrip trip) {
        return trip.seconds() * Math.pow(0.9, state.probeUpgradeLevel(ProbeUpgrade.ENGINE)) / universeBangFactor(BANG_UNIVERSE_PROBE_SPEED)
                // Version 0.126 : la valeur du catalogue des récompenses (BANG_REWARDS), et non un 2 écrit ici.
                / bangReward(idle.core.bigbang.BangChallenge.Reward.PROBES);
    }

    /** Nombre de récompenses que rapporte ce voyage, soute comprise ; une de plus avec le charme, à partir d'une heure. */
    public int probeRewardCount(ProbeTrip trip) {
        if (trip.isUrgent()) return 1;      // un seul petit lot, ni soute ni charme
        return trip.rewards() + state.probeUpgradeLevel(ProbeUpgrade.CARGO)
                + (colliderUpgradeLevel(ColliderUpgrade.FLAVOR_CHARM) > 0 && trip.ordinal() >= ProbeTrip.HOUR.ordinal() ? 1 : 0);
    }

    /** Ce que valent les récompenses de ce voyage : sa valeur, instruments compris. */
    public double probeValue(ProbeTrip trip) {
        return trip.value() * (1 + 0.25 * state.probeUpgradeLevel(ProbeUpgrade.LOOT)) * universeBangFactor(BANG_UNIVERSE_PROBE_REWARDS);
    }

    public boolean canLaunchProbe(ProbeDestination destination, ProbeTrip trip) {
        if (trip.isUrgent()) return canLaunchUrgentWave();
        return isProbesUnlocked() && isProbeDestinationOpen(destination) && state.probes().size() < probeSlots();
    }

    /** Big Bangs qu'il faut pour l'onde d'urgence chimique. */
    public static final int URGENT_WAVE_BIG_BANGS = 3;

    /** Vrai une fois l'onde d'urgence chimique ouverte : les sondes, et le troisième Big Bang. */
    public boolean isUrgentWaveUnlocked() {
        return isProbesUnlocked() && state.bigBangs() >= URGENT_WAVE_BIG_BANGS;
    }

    /** Vrai si l'onde d'urgence peut partir : ouverte, une place libre, et un élément découvert à rapporter. */
    public boolean canLaunchUrgentWave() {
        return isUrgentWaveUnlocked() && state.probes().size() < probeSlots() && urgentElement() > 0;
    }

    /** Envoie l'onde d'urgence chimique. */
    public boolean launchUrgentWave() {
        return launchProbe(ProbeDestination.VOID, ProbeTrip.URGENT);
    }

    /**
     * L'élément que l'astre visé demande le plus : pour chaque molécule qui lui manque, ses
     * exemplaires de chaque élément, fois le nombre de molécules qui manquent. Seuls comptent les
     * éléments déjà découverts ; à égalité, le plus petit numéro. 0 s'il n'y en a pas.
     */
    public int urgentElement() {
        Map<Integer, Long> demand = new java.util.TreeMap<>();
        for (ShoppingList.Item item : shoppingList().items()) {
            long missing = Math.max(item.missing(), item.gather() ? 1 : 0);
            if (missing <= 0) continue;
            for (Map.Entry<Integer, Integer> part : item.molecule().recipe().entrySet()) {
                if (state.elementCount(part.getKey()) > 0) demand.merge(part.getKey(), missing * part.getValue(), Long::sum);
            }
        }
        int best = 0;
        long most = 0;
        for (Map.Entry<Integer, Long> entry : demand.entrySet()) {
            if (entry.getValue() > most) {
                best = entry.getKey();
                most = entry.getValue();
            }
        }
        return best;
    }

    /** Envoie une sonde. */
    public boolean launchProbe(ProbeDestination destination, ProbeTrip trip) {
        if (!canLaunchProbe(destination, trip)) return false;
        List<Probe> flying = new ArrayList<>(state.probes());
        double seconds = probeDuration(trip);
        long id = state.probeLaunches();
        state.setProbeLaunches(id + 1);
        flying.add(new Probe(destination, trip, seconds, seconds, id));
        state.setProbes(flying);
        return true;
    }

    /** Les sondes arrivées dont le butin attend, dans l'ordre de leur départ. */
    public List<Probe> landedProbes() {
        List<Probe> landed = new ArrayList<>();
        for (Probe probe : state.probes()) if (probe.landed()) landed.add(probe);
        return landed;
    }

    /** Le butin qu'il reste à ouvrir de cette sonde, dans l'ordre. */
    public List<ProbeReward> probeRewardsOf(long probe) {
        List<ProbeReward> rewards = new ArrayList<>();
        for (ProbeReward reward : state.probeRewards()) if (reward.probe() == probe) rewards.add(reward);
        return rewards;
    }

    /** Ouvre la prochaine récompense de cette sonde ; la dernière ouverte, la sonde rentre au hangar. */
    public ProbeReward claimProbeReward(long probe) {
        for (int index = 0; index < state.probeRewards().size(); index++) {
            if (state.probeRewards().get(index).probe() == probe) return claimProbeRewardAt(index);
        }
        dropEmptyProbes();
        return null;
    }

    /** Ouvre d'un coup tout le butin de cette sonde : les récompenses ouvertes, dans l'ordre. */
    public List<ProbeReward> claimAllProbeRewards(long probe) {
        List<ProbeReward> opened = new ArrayList<>();
        for (ProbeReward reward = claimProbeReward(probe); reward != null; reward = claimProbeReward(probe)) opened.add(reward);
        return opened;
    }

    /** Les sondes arrivées dont tout le butin est ouvert rentrent au hangar : leur place se libère. */
    private void dropEmptyProbes() {
        boolean any = false;
        for (Probe probe : state.probes()) if (probe.landed() && probeRewardsOf(probe.id()).isEmpty()) any = true;
        if (!any) return;
        List<Probe> kept = new ArrayList<>();
        for (Probe probe : state.probes()) if (!probe.landed() || !probeRewardsOf(probe.id()).isEmpty()) kept.add(probe);
        state.setProbes(kept);
    }

    void runProbes(double dt) {
        if (dt <= 0) return;
        if (state.probeBoostLeft() > 0) state.setProbeBoostLeft(state.probeBoostLeft() - dt);
        if (state.probes().isEmpty()) return;
        List<Probe> flying = new ArrayList<>();
        List<ProbeReward> waiting = new ArrayList<>(state.probeRewards());
        boolean landed = false;
        for (Probe probe : state.probes()) {
            if (probe.landed()) {
                // Arrivée : elle attend qu'on ouvre son butin. Sans numéro (une sauvegarde d'avant), elle rentre aussitôt.
                if (probe.id() >= 0) flying.add(probe);
                continue;
            }
            double left = probe.left() - dt;
            if (left > 0) {
                flying.add(new Probe(probe.destination(), probe.trip(), left, probe.total(), probe.id()));
            } else {
                List<ProbeReward> loot = probeLoot(probe);
                waiting.addAll(loot);
                returnedProbes.add(loot.isEmpty() ? null : loot.get(0));
                landed = true;
                if (probe.id() >= 0) flying.add(new Probe(probe.destination(), probe.trip(), 0, probe.total(), probe.id()));
            }
        }
        state.setProbes(flying);
        if (landed) state.setProbeRewards(waiting);
    }

    /** Le butin d'une sonde qui revient : fixé par le numéro du voyage, sans toucher au hasard du jeu. */
    private List<ProbeReward> probeLoot(Probe probe) {
        long run = state.probeRuns();
        state.setProbeRuns(run + 1);
        double value = probeValue(probe.trip());
        ProbeDestination where = probe.destination();
        // L'onde d'urgence (version 0.086) : un seul lot, de l'élément le plus demandé à son retour ; rien s'il n'y en a plus.
        if (probe.trip().isUrgent()) {
            int element = urgentElement();
            return element > 0 ? List.of(new ProbeReward(ProbeReward.Type.ELEMENTS, String.valueOf(element), value, where, probe.id()))
                    : List.of(new ProbeReward(ProbeReward.Type.SAMPLES, "", value, where, probe.id()));
        }
        List<Integer> elements = probeElements(where);
        int count = probeRewardCount(probe.trip());
        boolean quarks = colliderUpgradeLevel(ColliderUpgrade.FLAVOR_CHARM) > 0 && probe.trip().ordinal() >= ProbeTrip.HOUR.ordinal();
        List<ProbeReward> loot = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (i == count - 1) {
                loot.add(new ProbeReward(ProbeReward.Type.SAMPLES, "", value, where, probe.id()));
            } else if (quarks && i == count - 2) {
                loot.add(new ProbeReward(ProbeReward.Type.QUARKS, "", value, where, probe.id()));
            } else if (i == 0) {
                loot.add(elements.isEmpty() ? new ProbeReward(ProbeReward.Type.SPACE, "", value, where, probe.id())
                        : new ProbeReward(ProbeReward.Type.ELEMENTS, String.valueOf(elements.get(Game.between(run, 10, 0, elements.size() - 1))), value, where, probe.id()));
            } else {
                int roll = Game.between(run, 20 + i, 0, elements.isEmpty() ? 1 : 2);
                if (roll == 2) {
                    loot.add(new ProbeReward(ProbeReward.Type.ELEMENTS, String.valueOf(elements.get(Game.between(run, 30 + i, 0, elements.size() - 1))), value, where, probe.id()));
                } else {
                    loot.add(new ProbeReward(roll == 0 ? ProbeReward.Type.SPACE : ProbeReward.Type.BOOST, "", value, where, probe.id()));
                }
            }
        }
        return loot;
    }

    /**
     * Les éléments que peut rapporter une destination : ceux des molécules de son rayon, déjà
     * découverts dans la partie, par ordre de numéro. Aucun pour le vide.
     */
    public List<Integer> probeElements(ProbeDestination destination) {
        if (destination.kind() == null) return List.of();
        java.util.TreeSet<Integer> found = new java.util.TreeSet<>();
        for (Molecule molecule : molecules()) {
            if (molecule.kind() != destination.kind()) continue;
            for (Integer element : molecule.recipe().keySet()) {
                if (state.elementCount(element) > 0) found.add(element);
            }
        }
        return new ArrayList<>(found);
    }

    /** La prochaine récompense à réclamer, ou {@code null}. */
    public ProbeReward nextProbeReward() {
        return state.probeRewards().isEmpty() ? null : state.probeRewards().get(0);
    }

    /** Exemplaires d'élément que donne cette récompense. */
    public int probeElementsOf(ProbeReward reward) {
        return (int) Math.ceil(PROBE_ELEMENTS * reward.value());
    }

    /** Espace que donne cette récompense, à l'expansion du moment. */
    public BigNum probeSpaceOf(ProbeReward reward) {
        return steadySpacePerSecond().multiply(PROBE_SPACE_SECONDS * reward.value());
    }

    /** Secondes de sursaut que donne cette récompense, réserves d'énergie comprises. */
    public double probeBoostOf(ProbeReward reward) {
        return PROBE_BOOST_SECONDS * Math.sqrt(reward.value()) * (1 + 0.5 * state.probeUpgradeLevel(ProbeUpgrade.BOOST));
    }

    /** Échantillons que donne cette récompense. */
    public long probeSamplesOf(ProbeReward reward) {
        return Math.max(1, Math.round(2 * Math.pow(reward.value(), 0.8)));
    }

    /** Quarks charmés que donne cette récompense ; les quarks beaux sont la moitié, arrondie au-dessous. */
    public int probeCharmOf(ProbeReward reward) {
        return Math.max(1, (int) Math.round(Math.pow(reward.value(), 0.5)));
    }

    /**
     * Réclame la prochaine récompense : elle s'applique, et la suivante prend sa place. Un élément
     * qui n'est plus découvert (un Big Bang a eu lieu entre-temps) devient de l'espace.
     *
     * @return la récompense réclamée, ou {@code null} s'il n'y en avait pas
     */
    public ProbeReward claimProbeReward() {
        if (state.probeRewards().isEmpty()) {
            dropEmptyProbes();
            return null;
        }
        return claimProbeRewardAt(0);
    }

    private ProbeReward claimProbeRewardAt(int index) {
        ProbeReward reward = state.probeRewards().get(index);
        List<ProbeReward> waiting = new ArrayList<>(state.probeRewards());
        waiting.remove(index);
        state.setProbeRewards(waiting);
        dropEmptyProbes();
        ProbeReward.Type type = reward.type();
        if (type == ProbeReward.Type.ELEMENTS) {
            int element = Integer.parseInt(reward.target());
            if (state.elementCount(element) > 0) {
                state.setElementCount(element, state.elementCount(element) + probeElementsOf(reward));
                return reward;
            }
            reward = new ProbeReward(ProbeReward.Type.SPACE, "", reward.value(), reward.destination());
            type = ProbeReward.Type.SPACE;
        }
        switch (type) {
            case SPACE -> {
                if (isBigBangUnlocked()) state.setSpace(state.space().add(probeSpaceOf(reward)));
            }
            case BOOST -> state.setProbeBoostLeft(state.probeBoostLeft() + probeBoostOf(reward));
            case SAMPLES -> state.setProbeSamples(state.probeSamples() + probeSamplesOf(reward));
            case QUARKS -> {
                int charm = probeCharmOf(reward);
                state.setQuarks(Quark.CHARM, state.quarks(Quark.CHARM) + charm);
                state.setQuarks(Quark.BOTTOM, state.quarks(Quark.BOTTOM) + charm / 2);
            }
            default -> { }
        }
        return reward;
    }

    /** Ce par quoi un sursaut d'expansion multiplie l'espace par seconde. */
    public double probeBoostMultiplier() {
        return state.probeBoostLeft() > 0 ? PROBE_BOOST_FACTOR : 1;
    }

    public long probeUpgradeCost(ProbeUpgrade upgrade) {
        return upgrade.cost(state.probeUpgradeLevel(upgrade));
    }

    public boolean canBuyProbeUpgrade(ProbeUpgrade upgrade) {
        int level = state.probeUpgradeLevel(upgrade);
        return isProbesUnlocked() && level < upgrade.maxLevel() && state.probeSamples() >= upgrade.cost(level);
    }

    public boolean buyProbeUpgrade(ProbeUpgrade upgrade) {
        if (!canBuyProbeUpgrade(upgrade)) return false;
        state.setProbeSamples(state.probeSamples() - probeUpgradeCost(upgrade));
        state.setProbeUpgradeLevel(upgrade, state.probeUpgradeLevel(upgrade) + 1);
        return true;
    }

    /** Les sondes revenues depuis le dernier appel, par leur première récompense : pour les annoncer une fois. */
    public List<ProbeReward> takeReturnedProbes() {
        List<ProbeReward> taken = new ArrayList<>(returnedProbes);
        returnedProbes.clear();
        return taken;
    }
    // ------------------------------------------------------------------
    // La maturation des astres (version 0.053) : un astre formé mûrit avant de compter pleinement
    // ------------------------------------------------------------------

    /** Part de son bonus que donne un astre pendant qu'il mûrit. */
    public static final double MATURING_SHARE = 0.25;
    /** Places de maturation : autant d'astres qui mûrissent à la fois. */
    public static final int MATURATION_PLACES = 3;
    /** Ce qu'une charge d'impulsion fait gagner à une maturation. */
    public static final double MATURATION_HASTE_SECONDS = 18;

    private final List<String> maturedBodies = new ArrayList<>();

    /**
     * Durée de maturation d'un astre, selon son échelle : 12 minutes pour un amas de roches, 75
     * pour une planète, 150 pour une étoile, 6 heures pour le trou noir supermassif (version 0.125 : trois fois
     * celles d'avant ; une impulsion n'en retire plus que 18 secondes, au lieu de 60).
     */
    public double maturationSeconds(Body body) {
        double minutes = switch (body.tier()) {
            case RUBBLE -> 12;
            case COMET -> 18;
            case ASTEROID -> 30;
            case MOON -> 45;
            case PLANET -> 75;
            case STAR -> 150;
            case REMNANT -> 210;
            case BLACK_HOLE -> 270;
            case EXOTIC -> 300;
            case CORE -> 360;
        };
        // Version 0.126 : la valeur du catalogue des récompenses (BANG_REWARDS), et non un 2 écrit ici.
        return minutes * 60 / universeBangFactor(BANG_UNIVERSE_MATURATION) / bangReward(idle.core.bigbang.BangChallenge.Reward.MATURATION)
                * maturationShare();
    }

    /** Ce que chaque niveau de l'amélioration de maturation du vide retire à la durée : 7 % depuis la version 0.132 (3 % en 0.129), dix niveaux. */
    public static final double VOID_MATURE_STEP = 0.07;

    /** La part de la durée de maturation qui reste avec le vide : 1 sans l'amélioration, 0,3 au plus fort. */
    public double maturationShare() {
        GameUniverse.VoidUpgrade mature = GameUniverse.VoidUpgrade.MATURE;
        return 1 - VOID_MATURE_STEP * Math.min(mature.max(), state.voidLevel(mature.ordinal()));
    }

    /** Vrai si cet astre est formé et a fini de mûrir : il compte pour les astres plus gros et pour la galaxie. */
    public boolean isBodyMature(String bodyId) {
        String id = body(bodyId).id();
        return state.hasBody(id) && state.maturingLeft(id) <= 0;
    }

    /** Vrai si cet astre est formé mais mûrit encore. */
    public boolean isBodyMaturing(String bodyId) {
        return state.maturingLeft(body(bodyId).id()) > 0;
    }

    /** Secondes de maturation qu'il reste à cet astre. */
    public double maturationLeft(String bodyId) {
        return state.maturingLeft(body(bodyId).id());
    }

    /** Nombre d'astres mûrs. */
    public int bodiesMature() {
        return state.bodies().size() - state.maturing().size();
    }

    /** Places de maturation. */
    public int maturationPlaces() {
        int places = MATURATION_PLACES;
        for (BigBangMilestone.MorePlaces more : reachedMorePlaces()) places += more.maturation();
        return places;
    }

    /** Les paliers « des places de plus » atteints. */
    private List<BigBangMilestone.MorePlaces> reachedMorePlaces() {
        List<BigBangMilestone.MorePlaces> found = new ArrayList<>();
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.MorePlaces more) found.add(more);
            }
        }
        return found;
    }

    /** Vrai si un palier atteint donne cet effet. */
    private boolean milestoneGives(Class<? extends BigBangMilestone.Effect> kind) {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (kind.isInstance(effect)) return true;
            }
        }
        return false;
    }

    /** Vrai une fois les matières exotiques ouvertes, au huitième Big Bang : leurs astres peuvent se former. */
    public boolean isExoticUnlocked() {
        return milestoneGives(BigBangMilestone.ExoticMatter.class);
    }

    /** Vrai s'il reste une place pour faire mûrir un astre de plus. */
    public boolean hasMaturationPlace() {
        return state.maturing().size() < maturationPlaces();
    }

    /** Les astres qui mûrissent, dans l'ordre de leur formation. */
    public List<String> maturingBodies() {
        return List.copyOf(state.maturing().keySet());
    }

    /** Dépense une charge d'impulsion pour avancer la maturation de cet astre de {@link #MATURATION_HASTE_SECONDS} secondes. */
    public boolean hastenMaturation(String bodyId) {
        String id = body(bodyId).id();
        if (state.maturingLeft(id) <= 0 || state.pulseCharges() < 1 - 1e-9) return false;
        state.setPulseCharges(state.pulseCharges() - 1);
        state.setPulses(state.pulses() + 1);
        state.setMaturing(id, state.maturingLeft(id) - MATURATION_HASTE_SECONDS);
        if (state.maturingLeft(id) <= 0) maturedBodies.add(id);
        return true;
    }

    void runMaturation(double dt) {
        if (dt <= 0 || state.maturing().isEmpty()) return;
        for (String id : List.copyOf(state.maturing().keySet())) {
            state.setMaturing(id, state.maturingLeft(id) - dt);
            if (state.maturingLeft(id) <= 0) maturedBodies.add(id);
        }
    }

    /** Les astres qui ont fini de mûrir depuis le dernier appel : pour les annoncer une fois. */
    public List<String> takeMaturedBodies() {
        List<String> taken = List.copyOf(maturedBodies);
        maturedBodies.clear();
        return taken;
    }
    // ------------------------------------------------------------------
    // Les systèmes solaires (version 0.054) : une étoile, ses planètes, ses lunes
    // ------------------------------------------------------------------

    /** Systèmes solaires complets que demande la galaxie. */
    public static final int SYSTEMS_FOR_GALAXY = 3;
    /** Planètes qu'il faut à un système pour être complet. */
    public static final int SYSTEM_MIN_PLANETS = 2;
    /** Ce que chaque point d'harmonie d'un système complet ajoute à toutes les grandeurs (8 = +800 %). */
    public static final double SYSTEM_GAIN = 8;

    /** Vrai une fois les systèmes solaires ouverts, au sixième Big Bang. */
    public boolean isSolarSystemsUnlocked() {
        return milestoneGives(BigBangMilestone.SolarSystems.class);
    }

    /** Vrai si ce système a assez de planètes. */
    public boolean isSystemComplete(SolarSystem system) {
        return system.planets().size() >= SYSTEM_MIN_PLANETS;
    }

    /** Nombre de systèmes complets. */
    public int solarSystemsComplete() {
        int count = 0;
        for (SolarSystem system : state.solarSystems()) if (isSystemComplete(system)) count++;
        return count;
    }

    /** Vrai si cet astre est déjà dans un système. */
    public boolean isInSystem(String bodyId) {
        for (SolarSystem system : state.solarSystems()) {
            if (system.star().equals(bodyId) || system.planets().contains(bodyId) || system.moons().contains(bodyId)) return true;
        }
        return false;
    }

    /** Systèmes où se trouve cette planète. */
    public int planetPlacements(String planetId) {
        int count = 0;
        for (SolarSystem system : state.solarSystems()) if (system.planets().contains(planetId)) count++;
        return count;
    }

    /** Vrai si cette planète peut encore entrer dans un système : une fois, ou deux une fois les trois défis de Big Bang réussis (version 0.116). */
    public boolean canPlaceAgain(String planetId) {
        return planetPlacements(planetId) < (planetsTwice() ? 2 : 1);
    }

    /** Les astres mûrs de cette échelle qui ne sont dans aucun système : ce qu'on peut encore y placer. */
    public List<String> freeForSystems(Body.Tier tier) {
        List<String> free = new ArrayList<>();
        for (Body body : bodies.values()) {
            if (body.tier() == tier && isBodyMature(body.id()) && (tier == Body.Tier.PLANET ? canPlaceAgain(body.id()) : !isInSystem(body.id()))) free.add(body.id());
        }
        return free;
    }

    /** Fonde un système autour de cette étoile, mûre et libre. */
    public boolean foundSystem(String starId) {
        Body star = body(starId);
        if (!isSolarSystemsUnlocked() || star.tier() != Body.Tier.STAR || !isBodyMature(star.id()) || isInSystem(star.id())) return false;
        List<SolarSystem> systems = new ArrayList<>(state.solarSystems());
        systems.add(new SolarSystem(star.id(), List.of(), List.of()));
        state.setSolarSystems(systems);
        return true;
    }

    /** Défait un système : ses astres redeviennent libres. */
    public boolean dissolveSystem(int index) {
        if (index < 0 || index >= state.solarSystems().size()) return false;
        List<SolarSystem> systems = new ArrayList<>(state.solarSystems());
        systems.remove(index);
        state.setSolarSystems(systems);
        return true;
    }

    /** Place une planète mûre et libre sur l'orbite la plus lointaine de ce système. */
    public boolean addPlanet(int index, String planetId) {
        Body planet = body(planetId);
        if (index < 0 || index >= state.solarSystems().size() || planet.tier() != Body.Tier.PLANET
                || !isBodyMature(planet.id()) || !canPlaceAgain(planet.id())) return false;
        SolarSystem system = state.solarSystems().get(index);
        // Deux fois au plus (version 0.116), mais jamais deux fois dans le même système.
        if (system.planets().contains(planet.id())) return false;
        List<String> planets = new ArrayList<>(system.planets());
        planets.add(planet.id());
        replaceSystem(index, system.withPlanets(planets));
        return true;
    }

    /** Ajoute une lune mûre et libre à ce système : une par planète au plus. */
    public boolean addMoon(int index, String moonId) {
        Body moon = body(moonId);
        if (index < 0 || index >= state.solarSystems().size() || moon.tier() != Body.Tier.MOON
                || !isBodyMature(moon.id()) || isInSystem(moon.id())) return false;
        SolarSystem system = state.solarSystems().get(index);
        if (system.moons().size() >= system.planets().size()) return false;
        List<String> moons = new ArrayList<>(system.moons());
        moons.add(moon.id());
        replaceSystem(index, system.withMoons(moons));
        return true;
    }

    /** Retire une planète ou une lune de ce système ; une lune de trop part avec la planète. */
    public boolean removeFromSystem(int index, String bodyId) {
        if (index < 0 || index >= state.solarSystems().size()) return false;
        SolarSystem system = state.solarSystems().get(index);
        List<String> planets = new ArrayList<>(system.planets());
        List<String> moons = new ArrayList<>(system.moons());
        if (!planets.remove(bodyId) && !moons.remove(bodyId)) return false;
        while (moons.size() > planets.size()) moons.remove(moons.size() - 1);
        replaceSystem(index, new SolarSystem(system.star(), planets, moons));
        return true;
    }

    /** Rapproche (−1) ou éloigne (+1) une planète de son étoile. */
    public boolean movePlanet(int index, String planetId, int direction) {
        if (index < 0 || index >= state.solarSystems().size()) return false;
        SolarSystem system = state.solarSystems().get(index);
        List<String> planets = new ArrayList<>(system.planets());
        int at = planets.indexOf(planetId), to = at + Integer.signum(direction);
        if (at < 0 || to < 0 || to >= planets.size()) return false;
        java.util.Collections.swap(planets, at, to);
        replaceSystem(index, system.withPlanets(planets));
        return true;
    }

    void replaceSystem(int index, SolarSystem system) {
        List<SolarSystem> systems = new ArrayList<>(state.solarSystems());
        systems.set(index, system);
        state.setSolarSystems(systems);
    }

    /** Une planète rocheuse : roche, lave ou océan ; sa place est près de l'étoile. */
    private static boolean telluric(Body planet) {
        return planet.look() == Body.Look.ROCKY || planet.look() == Body.Look.MOLTEN || planet.look() == Body.Look.OCEAN;
    }

    /** Vrai pour une planète tellurique (roche, lave, océan), dont la place est près de l'étoile. */
    public boolean isTelluric(String planetId) {
        return telluric(body(planetId));
    }

    /**
     * Vrai si la planète de cette orbite est bien placée : une tellurique n'a que des telluriques plus
     * près de l'étoile qu'elle, une glacée ou gazeuse n'a que des glacées ou gazeuses plus loin.
     */
    public boolean isPlanetWellPlaced(SolarSystem system, int orbit) {
        List<String> planets = system.planets();
        if (orbit < 0 || orbit >= planets.size()) return false;
        boolean placed = true;
        if (telluric(body(planets.get(orbit)))) {
            for (int inner = 0; inner < orbit; inner++) placed &= telluric(body(planets.get(inner)));
        } else {
            for (int outer = orbit + 1; outer < planets.size(); outer++) placed &= !telluric(body(planets.get(outer)));
        }
        return placed;
    }

    /** Vrai si l'étoile de ce système a une zone habitable : un Soleil ou une naine rouge, aux orbites 2 et 3. */
    public static boolean hasHabitableZone(SolarSystem system) {
        return system.star().equals("yellow_sun") || system.star().equals("red_dwarf");
    }

    /** Vrai si cette orbite (comptée depuis 0) est dans la zone habitable du système. */
    public static boolean isHabitableOrbit(SolarSystem system, int orbit) {
        return hasHabitableZone(system) && (orbit == 1 || orbit == 2);
    }

    /** Ce qui fait l'harmonie d'un système, une raison par point, pour l'afficher. */
    public List<String> systemHarmonyReasons(SolarSystem system) {
        List<String> reasons = new ArrayList<>();
        List<String> planets = system.planets();
        for (int orbit = 0; orbit < planets.size(); orbit++) {
            Body planet = body(planets.get(orbit));
            if (isPlanetWellPlaced(system, orbit)) {
                reasons.add(planet.name() + (telluric(planet) ? " : tellurique, près de l'étoile" : " : glacée ou gazeuse, au loin"));
            }
            if (planet.look() == Body.Look.OCEAN && isHabitableOrbit(system, orbit)) {
                reasons.add(planet.name() + " : dans la zone habitable (+2)");
                reasons.add("");
            }
        }
        for (String moon : system.moons()) reasons.add(body(moon).name() + " : une lune");
        if (system.star().equals("blue_giant") || system.star().equals("red_giant")) reasons.add(body(system.star()).name() + " : une étoile géante");
        return reasons;
    }

    /**
     * Le meilleur ordre des planètes de ce système (version 0.059) : celui qui donne le plus
     * d'harmonie, et à égalité celui qui bouge le moins de planètes. Les lunes ne changent pas.
     */
    public SolarSystem bestArrangement(SolarSystem system) {
        List<String> planets = system.planets();
        if (planets.size() > 8) return system;
        SolarSystem[] best = {system};
        int[] bestScore = {systemHarmony(system)};
        int[] bestMoves = {0};
        permute(new ArrayList<>(planets), 0, order -> {
            SolarSystem candidate = system.withPlanets(order);
            int score = systemHarmony(candidate);
            int moves = 0;
            for (int i = 0; i < order.size(); i++) if (!order.get(i).equals(planets.get(i))) moves++;
            if (score > bestScore[0] || (score == bestScore[0] && moves < bestMoves[0])) {
                best[0] = candidate;
                bestScore[0] = score;
                bestMoves[0] = moves;
            }
        });
        return best[0];
    }

    private static void permute(List<String> items, int from, java.util.function.Consumer<List<String>> each) {
        if (from >= items.size() - 1) {
            each.accept(List.copyOf(items));
            return;
        }
        for (int i = from; i < items.size(); i++) {
            java.util.Collections.swap(items, from, i);
            permute(items, from + 1, each);
            java.util.Collections.swap(items, from, i);
        }
    }

    /** Vrai si ranger autrement les planètes de ce système lui donnerait plus d'harmonie. */
    public boolean canArrangeSystem(int index) {
        if (index < 0 || index >= state.solarSystems().size()) return false;
        SolarSystem system = state.solarSystems().get(index);
        return systemHarmony(bestArrangement(system)) > systemHarmony(system);
    }

    /** Range les planètes de ce système dans leur meilleur ordre. */
    public boolean arrangeSystem(int index) {
        if (!canArrangeSystem(index)) return false;
        replaceSystem(index, bestArrangement(state.solarSystems().get(index)));
        return true;
    }

    /** Points d'harmonie d'un système : une planète bien placée, une lune, la zone habitable, une géante. */
    public int systemHarmony(SolarSystem system) {
        return systemHarmonyReasons(system).size();
    }
}

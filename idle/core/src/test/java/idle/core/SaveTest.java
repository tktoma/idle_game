package idle.core;

import idle.core.state.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievements;
import idle.core.collider.*;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.elements.ElementCategory;
import idle.core.math.BigNum;
import idle.core.probes.*;
import idle.core.save.*;
import idle.core.sky.SolarSystem;
import idle.core.upgrades.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.*;

/**
 * La sauvegarde ({@link SaveCodec}) et le retour après une absence ({@link Absence}).
 *
 * <p>Le test central compare une partie à sa copie relue <b>champ par champ, par réflexion</b> : un
 * champ ajouté plus tard à {@link GameState}, {@link GameStats} ou {@link StatsHistory} et oublié
 * dans la sauvegarde le fait échouer. Un second test vérifie que la partie d'essai a bien sorti
 * chaque champ de sa valeur de départ : sans cela, un champ oublié passerait inaperçu.
 */
class SaveTest {
    /** Ce qui se recalcule et ne fait pas partie de la sauvegarde. */
    private static final Set<String> DERIVED = Set.of("testBlocked", "elementsVersion", "achievementsVersion", "darkVersion", "challengesVersion", "completedChallengesView", "moleculesVersion", "substancesSeen",
            "assembliesSeen", "bodiesSeen", "moleculeLog",
            // Vrai seulement à la relecture d'une sauvegarde d'avant, le temps du premier relevé des moments.
            "momentsUndated",
            // Le compte des skins débloqués, gardé tant qu'ils ne changent pas.
            "skinsVersion", "skinsCountVersion", "skinsCountUnlocked", "skinsCountCounted");

    // ------------------------------------------------------------------
    // Une partie où tout a bougé
    // ------------------------------------------------------------------

    /** Une partie d'essai où chaque champ a quitté sa valeur de départ. Elle ne respecte aucune règle : seule la sauvegarde l'intéresse. */
    static GameState richState() {
        GameState state = new GameState();
        state.setStarted(true);
        state.setParticles(BigNum.of(1.234567890123, 345));
        state.setAtoms(BigNum.of(42));
        state.setTotalAtoms(BigNum.of(9.87, 14));
        state.setTimeSinceFusion(12.5);
        state.setFormation(0, 0.25);
        state.setFormation(3, 0.999);
        state.setTimePlayed(123456.789);
        state.setLevel("speed", 31);
        state.setLevel("coupling", 4);
        state.addAutomation("auto_speed");
        state.addAutomation("auto_fusion");
        state.setAutomationEnabled("auto_speed", true);
        state.setAutomationSpeedLevel("auto_speed", 7);
        state.setAutomationBatchLevel("auto_generator", 3);
        state.setAutomationTimer("auto_speed", 0.125);
        state.setElementCount(1, 3);
        state.setElementCount(26, 1);
        state.setElementCount(118, 2);
        state.setSynthesisCount(77);
        state.setDrySyntheses(1);
        state.setPulseCharges(12.5);
        state.setPulses(33);
        state.setColliderEnergy(1234.5);
        state.setColliderGenerators(ColliderGenerator.BEAM, 12.25);
        state.setColliderGeneratorsBought(ColliderGenerator.MAGNET, 3);
        state.setColliderPhase(0.375);
        state.setColliderCrossings(1234);
        state.setColliderCaught(1233);
        state.setColliderUpgradeLevel(ColliderUpgrade.LUCK, 4);
        state.setColliderPaused(ColliderUpgrade.AUTO_BEAM, true);
        state.setQuarks(Quark.UP, 7);
        state.setQuarks(Quark.CHARM, 2);
        state.setColliderEnergyProduced(4.5e9);
        state.setProbes(List.of(new Probe(ProbeDestination.SALT_MOON, ProbeTrip.HOUR, 1200.5, 3240, 8)));
        state.setProbeRewards(List.of(new ProbeReward(ProbeReward.Type.ELEMENTS, "26", 6.25, ProbeDestination.ASTEROIDS, 7),
                new ProbeReward(ProbeReward.Type.SPACE, "", 1, ProbeDestination.VOID)));
        state.setProbeUpgradeLevel(ProbeUpgrade.LOOT, 3);
        state.setProbeRuns(7);
        state.setProbeLaunches(9);
        state.setProbeSamples(42);
        state.setProbeBoostLeft(90.5);
        state.setCollisions(List.of(new Collision(CollisionTier.LOW, 12.25, true, true), new Collision(CollisionTier.HIGH, 900.5, false, false)));
        state.setCollisionRuns(41);
        state.setTopLeft(33.5);
        state.setOverheatLeft(120.25);
        state.setSynthesisTarget(ElementCategory.values()[2]);
        state.setSynthesisTries(3);
        state.setDarkMatter(BigNum.of(17));
        state.setDarkMatterSpent(BigNum.of(350));
        state.setDarkMatterSize(BigNum.of(3.3, 12));
        state.setExplosions(64);
        state.setTableWeightLevel(9);
        state.setDarkLevel("dark_a", 2);
        state.setDarkLevel("dark_b", 1);
        state.setFusionThreshold(12);
        state.setSynthesisReserve(BigNum.of(5, 6));
        state.setHoldLocked(true);
        state.setActiveChallenge("défi, avec : des = signes | bizarres % et *");
        state.addCompletedChallenge("challenge_a");
        state.addCompletedChallenge("challenge_b");
        state.setChallengeTime("challenge_a", 4321.5);
        state.setDecayDebt(0.75);
        state.addAchievement("second");
        state.addAchievement("first");
        state.setDarkAutomationEnabled("dark_auto", true);
        state.setDarkAutomationTimer("dark_auto", 1.5);
        state.setBigBangs(5);
        state.addBoughtMilestone("Gravité");
        state.addMolecules("H2O", 12);
        state.addMolecules("Ca5(PO4)3F", 3);
        state.addMolecules("H2O", 1_000_000);
        state.addMolecules("CO2", 1);
        state.addSubstance("H2O");
        state.addSubstance("CO2");
        state.addAssembly("ocean");
        state.addAssembly("atmosphere");
        state.addBody("planet");
        state.setMaturing("planet", 612.5);
        state.addBody("star");
        state.addBody("moon");
        state.setSolarSystems(List.of(new SolarSystem("star", List.of("planet"), List.of("moon"))));
        state.setCosmosLevel(2);
        state.setAutoHold(false);
        state.setAutoMolecules(false);
        state.setMoleculeAutomated("H2O", true);
        state.setMoleculeAutomated("CO2", true);
        state.setAutomationPaused(true);
        state.setAutoGather(false);
        state.setAutoForm(false);
        state.setAutoMoleculeReserve(0.25);
        state.setExplosionRecord(431.5);
        state.setCometWait(512.25);
        state.setCometVisible(12.5);
        state.setCometBoost(88.75);
        state.setCometsCaught(7);
        // Les anomalies du ciel.
        state.setSkyEvent("PARTICLE_WIND");
        state.setSkyEventVisible(42.5);
        state.setSkyEventWait(300.25);
        state.setWindLeft(17.5);
        state.setSkyEventsCaught(4);
        state.setEndSeen(true);
        state.setExtraUniverses(3);
        state.setUniverseFormed(true);
        state.setDarkPlan(List.of("dark_a", "dark_b"));
        state.setSavedDarkPlan("arbre:dark_a,dark_b");
        state.setSavedDarkPlan(3, "arbre:dark_c");
        state.setSystemPlan(List.of(new SolarSystem("etoile", List.of("p1", "p2"), List.of("l1"))));
        state.setSavedSystemPlan("systemes:etoile|p1,p2|l1;autre||");
        state.setForkResetArmed(true);
        state.setReplaying(true);
        state.setFusionsAtUniverse(42);
        state.setExplosionsAtUniverse(17);
        state.setSkinsOpen(true);
        state.unlockSkin("dark_vortex");
        state.chooseSkin("DARK_ANIMATION", "dark_vortex");
        state.setRegimeOpen(true);
        state.setRegimeLeft(12.5);
        state.setRegimeRest(30.25);
        state.setSynergyOpen(true);
        state.setPerfectStreak(2);
        state.setPerfectLeft(40.5);
        state.setCycleChoice("FUSION");
        state.setPreferredFamily("NOBLE_GAS");
        state.setPreferenceDraws(7);
        state.setFreeElementsUsed(3);
        state.setActiveTrial("DETECTOR");
        state.setTrialStart(1234.5);
        state.setTrialBase(99.25);
        state.setTrialCount(2);
        state.addCompletedTrial("COLD_BANG");
        state.setRebuildRecord(48.5);
        state.addSeenPopup("tab:ATOMS");
        state.setPopupsPrimed(true);
        state.setRebuildPending(true);
        state.addResearch("dense2");
        state.setPlannedBodies(List.of("icy_comet", "rocky_moon"));
        state.setStateCap("GAS", 0.7);
        state.stats().noteUniverseTally(8, 5_000, 300, 12);
        state.setUniverseSeeds(BigNum.of(7.5, 12));
        state.setDimensionShards(12.3);
        state.setLinePhase(1.25);
        state.setLineYieldLevel(2);
        state.setLineTicksLevel(3);
        state.setLineSpeedLevel(4);
        state.setLineBallsLevel(1);
        state.setLineCountLevel(2);
        state.setDimensionShardsEarned(140.5);
        state.setSeedSprouted(true);
        state.setSeedWaterings(17);
        state.setWateringCan(true);
        for (int i = 0; i < 17; i++) state.setSeedUpgradeLevel(i, i == 16 ? 1 : i + 1);
        state.setChronoStored(123.5);
        state.setChronoSpeed(2.5);
        state.setTreeBubbles(0b1000000000000000101);   // la base, la bulle de droite du bonsaï, et le sommet (la 19e, bit 18)
        state.setVoidSpent(456.25);
        // Une bulle au niveau 4, et ce que le géant de gauche a versé au cumul.
        state.setTreeBubbleLevel(1, 4);
        state.setVoidTrickled(78.5);
        state.setVoidCarried(321.5);
        // La boucle.
        state.setLoopShards(55.5);
        state.setLoopShardsEarned(4321.25);
        for (int i = 0; i < 6; i++) state.setLoopLevel(i, i + 2);
        // La brane.
        state.setBraneShards(66.5);
        state.setBraneShardsEarned(5432.25);
        for (int i = 0; i < 5; i++) state.setBraneLevel(i, i + 3);
        // Le clicker.
        state.setClickerShards(77.5);
        state.setClickerShardsEarned(6543.25);
        for (int i = 0; i < 8; i++) state.setClickerLevel(i, i == 2 ? 1 : i + 1);
        // La grille.
        state.setGridShards(88.5);
        state.setGridShardsEarned(7654.25);
        for (int i = 0; i < 6; i++) state.setGridLevel(i, i + 1);
        // Le miroir.
        state.setMirrorShards(99.5);
        state.setMirrorShardsEarned(8765.25);
        for (int i = 0; i < 5; i++) state.setMirrorLevel(i, i + 1);
        // Les réussites du miroir et le mélange.
        state.setMirrorWins(12);
        state.setMixShards(111.5);
        state.setMixShardsEarned(9876.25);
        for (int i = 0; i < 11; i++) state.setMixLevel(i, i + 1);
        // Le mélange par étapes.
        state.setMixStage(5);
        state.setMixStageEarned(321.5);
        // La fin de la partie.
        state.setGameFinished(true);
        // Les hadrons.
        state.setHadronsOpen(true);
        state.setHadronsPrice(2.5e9);
        state.setHadrons(Hadron.PROTON, 12);
        state.setHadrons(Hadron.J_PSI, 3);
        // L'univers formé que le jeu garde jusqu'au lancement, et les bulles déjà achetées à la formation.
        GameState held = new GameState();
        held.setHadronsOpen(true);
        held.setHadrons(Hadron.NEUTRON, 7);
        state.bubbleKeep = held;
        state.bubbleKeepMask = 0b1010_0000_0000;
        // Deux automatismes des dimensions, dont un coupé. Le palier de la visée sans
        // attente vient avec ceux, plus lents, d'avant lui (la relecture les ajoute s'ils manquent).
        state.addDimensionAutomation("LINE_YIELD");
        for (String tier : new String[] {"PLANE_AIM_5S", "PLANE_AIM_2S", "PLANE_AIM_1S", "PLANE_AIM_05S", "PLANE_AIM"}) state.addDimensionAutomation(tier);
        state.setDimensionAutomationOff("PLANE_AIM", true);
        state.setTwigOpened(true);
        state.setSeedUniversesOpened(1);
        state.setUpgradeCalm(123.5);
        state.setBangCalm(77.5);
        state.setPlaneShards(9.5);
        state.setPlaneShardsEarned(31);
        for (int i = 0; i < 5; i++) state.setPlaneLevel(i, i + 1);
        state.setVolumeShards(4.5);
        state.setVolumeShardsEarned(23);
        for (int i = 0; i < 5; i++) state.setVolumeLevel(i, i + 2);
        state.setDurationShards(6.5);
        state.setDurationShardsEarned(57);
        for (int i = 0; i < 4; i++) state.setDurationLevel(i, i + 3);
        state.setVoidEnergy(BigNum.of(42));
        state.setVoidOpen(true);
        state.setVoidHoleOpen(true);
        state.setVoidShare(0.4);
        state.setVoidPrice(3.5e12);
        state.setVoidSacrificed(BigNum.of(1, 50));
        for (int i = 0; i < GameState.VOID_UPGRADES; i++) state.setVoidLevel(i, i + 1);
        state.noteVoidHistory("3", 1234.5);
        state.setRecord("FIRST_FUSION", 2280.5);
        state.setRecord("GALAXY", 231_456.75);
        // Le profil et les albums, avec des caractères que la sauvegarde doit échapper.
        state.setPlayerName("Zoé ; « la » 100 % = 1|2");
        state.setAvatar("custom");
        state.setChosenTitle("STAR_SMITH");
        state.earnTitle("SPARK", 3.5);
        state.earnTitle("STAR_SMITH", 98_765.25);
        state.noteFound("molecules", "H2O", 41_000.5);
        state.noteFound("substances", "H2O", 42_000);
        state.noteFound("assemblies", "granitic_rock", 50_000);
        state.noteFound("bodies", "rocky_rubble", 60_000.75);
        state.setActiveBangChallenge("FORGOTTEN");
        state.addCompletedBangChallenge("VOID");
        state.setBangChallengeTime("VOID", 3120.5);
        state.setBangChallengeStarted(190_000.25);
        state.setAtomUpgradeOrder(List.of("atom_mass", "atom_double"));
        state.setAtomUpgradeOrdered(true);
        state.setAutomationOrder(List.of("auto_fusion", "auto_speed"));
        state.setAutomationOrdered(true);
        state.setMoleculeFavorite("CO2", true);
        state.setMoleculeFavorite("H2O", true);
        state.addSpaceUpgrade("space_b");
        state.addSpaceUpgrade("space_a");
        state.setSpace(BigNum.of(5.17, 10));

        GameStats stats = state.stats();
        stats.addParticles(BigNum.of(3, 400));
        stats.noteProduction(BigNum.of(2, 380));
        stats.noteParticleUpgrade(BigNum.of(1, 50), true);
        stats.noteParticleUpgrade(BigNum.of(1, 60), false);
        stats.noteAtomUpgrade(BigNum.of(25));
        stats.noteFusion(BigNum.of(8), 30);
        stats.noteFusion(BigNum.of(11), 12.25);
        stats.noteAutomationAction();
        stats.noteDarkAutomationAction();
        stats.noteSpeedPrime();
        stats.noteDarkAutomationAction();
        stats.noteSynthesis(2, true);
        stats.noteSynthesisTry();
        stats.noteTargetedSynthesis();
        stats.addHoldTime(456.5);
        stats.noteUnlocked("auto_speed");
        stats.noteUnlocked("auto_fusion");
        stats.addTime(900);
        stats.endRun();
        stats.addTime(700);
        stats.endRun();
        stats.addTime(1200);
        stats.noteBigBang(104, 5000);
        stats.addTime(800);
        stats.noteBigBang(23, 9000);
        stats.noteMoleculeCreation(120, 44, true);
        stats.noteMoleculeCreation(1, 3, false);
        stats.noteStep(GameStats.Step.FIRST_MOLECULE, 5100.5);
        stats.noteStep(GameStats.Step.UNIVERSE, 245040);
        // L'univers formé entre au journal, et la partie secondaire note ses moments.
        stats.archiveUniverse(1, 245040);
        stats.noteMoment("dimension.1.ouverte", 245040);
        stats.noteMoment("graine.1", 246000.5);
        stats.addDimensionPlay(1, 0.75);
        // Une nouvelle partie : ses étapes à elle se datent à part.
        stats.startUniverseSteps(250_000.5);
        stats.noteStep(GameStats.Step.FIRST_FUSION, 250_300.25);
        // Après les remises à zéro : ce qui court « depuis la dernière explosion » doit avoir une valeur aussi.
        stats.addTime(333.25);
        stats.addParticles(BigNum.of(7, 20));
        stats.noteFusion(BigNum.of(2), 40);
        fill(stats.history(), 11, 640);
        fill(stats.runHistory(), 23, 5);
        return state;
    }

    /** Remplit un historique jusqu'à ce qu'il ait dû s'éclaircir une fois, avec des valeurs de toutes sortes. */
    static void fill(StatsHistory history, long seed, double step) {
        Random random = new Random(seed);
        int stats = StatsHistory.Stat.values().length;
        for (int i = 0; i < StatsHistory.CAPACITY + 7; i++) {
            double[] values = new double[stats];
            for (int s = 0; s < stats; s++) {
                values[s] = switch (s % 6) {
                    case 0 -> Double.NaN;
                    case 1 -> i / 40;                              // une valeur qui se répète longtemps
                    case 2 -> random.nextDouble() * 3000;
                    case 3 -> 133_026_596L + i;                    // un grand entier, exact
                    case 4 -> i < 50 ? Double.NEGATIVE_INFINITY : -random.nextDouble();
                    default -> i % 9 == 0 ? Double.NaN : 1e-9 * random.nextDouble();
                };
            }
            Map<String, Double> delays = new HashMap<>();
            if (i > 20) delays.put("auto_speed", Math.max(0.1, 5.0 / (1 + i / 30)));
            if (i > 300) delays.put("auto: étrange, id", 0.5);
            history.add(new StatsHistory.Sample(i * step, values, delays));
        }
        history.notePeak(StatsHistory.Stat.PARTICLES, 123.456);
        history.notePeak(StatsHistory.Stat.DARK_MATTER, 7);
    }

    // ------------------------------------------------------------------
    // Comparaison champ par champ
    // ------------------------------------------------------------------

    /**
     * Les endroits où deux objets diffèrent, par réflexion, en descendant dans les classes du jeu,
     * les listes, les tables et les tableaux. Les valeurs des courbes ({@code values} et
     * {@code delays} d'un relevé) sont comparées à sept chiffres près : c'est la précision gardée.
     */
    static List<String> differences(String path, Object a, Object b, boolean loose) {
        List<String> found = new ArrayList<>();
        compare(path, a, b, loose, found);
        return found;
    }

    static void compare(String path, Object a, Object b, boolean loose, List<String> found) {
        if (a == null || b == null) {
            if (a != b) found.add(path + " : " + a + " ≠ " + b);
        } else if (a instanceof Double x && b instanceof Double y) {
            if (!sameNumber(x, y, loose)) found.add(path + " : " + x + " ≠ " + y);
        } else if (a instanceof double[] x && b instanceof double[] y) {
            if (x.length != y.length) found.add(path + " : longueurs " + x.length + " ≠ " + y.length);
            for (int i = 0; i < Math.min(x.length, y.length); i++) {
                if (!sameNumber(x[i], y[i], loose)) found.add(path + "[" + i + "] : " + x[i] + " ≠ " + y[i]);
            }
        } else if (a instanceof int[] x && b instanceof int[] y) {
            // Les niveaux des améliorations de la graine.
            if (!Arrays.equals(x, y)) found.add(path + " : " + Arrays.toString(x) + " ≠ " + Arrays.toString(y));
        } else if (a instanceof Map<?, ?> x && b instanceof Map<?, ?> y) {
            if (!x.keySet().equals(y.keySet())) found.add(path + " : clés " + x.keySet() + " ≠ " + y.keySet());
            for (Object key : x.keySet()) {
                if (y.containsKey(key)) compare(path + "{" + key + "}", x.get(key), y.get(key), loose, found);
            }
        } else if (a instanceof List<?> x && b instanceof List<?> y) {
            if (x.size() != y.size()) found.add(path + " : tailles " + x.size() + " ≠ " + y.size());
            for (int i = 0; i < Math.min(x.size(), y.size()); i++) compare(path + "[" + i + "]", x.get(i), y.get(i), loose, found);
        } else if (a instanceof LinkedHashSet<?> x && b instanceof LinkedHashSet<?> y) {
            compare(path, new ArrayList<Object>(x), new ArrayList<Object>(y), loose, found);   // l'ordre compte
        } else if (a instanceof Collection<?> x && b instanceof Collection<?> y) {
            if (!Set.copyOf(x).equals(Set.copyOf(y))) found.add(path + " : " + x + " ≠ " + y);
        } else if (a instanceof Record && !(a instanceof BigNum)) {
            for (RecordComponent component : a.getClass().getRecordComponents()) {
                try {
                    boolean curve = component.getName().equals("values") || component.getName().equals("delays");
                    compare(path + "." + component.getName(), component.getAccessor().invoke(a),
                            component.getAccessor().invoke(b), loose || curve, found);
                } catch (ReflectiveOperationException impossible) {
                    throw new AssertionError(impossible);
                }
            }
        } else if (a instanceof GameState || a instanceof GameStats || a instanceof StatsHistory) {
            for (Field field : fields(a.getClass())) {
                try {
                    Object x = field.get(a);
                    Object y = field.get(b);
                    if (field.getName().equals("logEnds")) {
                        // Seules comptent les suites réellement posées : le tableau est plus long que nécessaire.
                        int runs = ((GameState) a).moleculeLog().isEmpty() ? 0 : ((List<?>) read(a, "logKinds")).size();
                        x = Arrays.copyOf((long[]) x, runs);
                        y = Arrays.copyOf((long[]) y, Math.min(runs, ((long[]) y).length));
                        if (!Arrays.equals((long[]) x, (long[]) y)) found.add(path + ".logEnds");
                        continue;
                    }
                    compare(path + "." + field.getName(), x, y, loose, found);
                } catch (IllegalAccessException impossible) {
                    throw new AssertionError(impossible);
                }
            }
        } else if (!a.equals(b)) {
            found.add(path + " : " + a + " ≠ " + b);
        }
    }

    static boolean sameNumber(double x, double y, boolean loose) {
        if (Double.compare(x, y) == 0) return true;
        return loose && Math.abs(x - y) <= 1e-6 * Math.max(Math.abs(x), Math.abs(y));
    }

    /** Les champs d'une classe du jeu qui font partie de la sauvegarde. */
    static List<Field> fields(Class<?> type) {
        List<Field> kept = new ArrayList<>();
        // Une classe découpée en parties (GameState et ses couches) : les champs de toute la chaîne.
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || DERIVED.contains(field.getName())) continue;
                field.setAccessible(true);
                kept.add(field);
            }
        }
        return kept;
    }

    static Object read(Object owner, String name) {
        try {
            for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (!field.getName().equals(name)) continue;
                    field.setAccessible(true);
                    return field.get(owner);
                }
            }
            throw new NoSuchFieldException(name);
        } catch (ReflectiveOperationException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Les champs, en descendant dans les statistiques et les historiques, qui ont encore leur valeur de départ. */
    static List<String> untouched(String path, Object rich, Object fresh) {
        List<String> same = new ArrayList<>();
        for (Field field : fields(rich.getClass())) {
            try {
                Object x = field.get(rich);
                Object y = field.get(fresh);
                String here = path + "." + field.getName();
                if (x instanceof GameStats || x instanceof StatsHistory) same.addAll(untouched(here, x, y));
                else if (differences(here, x, y, false).isEmpty()) same.add(here);
            } catch (IllegalAccessException impossible) {
                throw new AssertionError(impossible);
            }
        }
        return same;
    }

    static GameState copy(GameState state) throws SaveCodec.Unreadable {
        return SaveCodec.read(SaveCodec.write(state, 1_700_000_000_000L)).state();
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Nested
    class Sauvegarde {
        @Test
        void laPartieDEssaiASortiChaqueChampDeSaValeurDeDepart() {
            assertEquals(List.of(), untouched("partie", richState(), new GameState()),
                    "ces champs ont encore leur valeur de départ dans richState() : un oubli dans la sauvegarde ne se verrait pas");
        }

        @Test
        void unePartieRelueEstLaMemeChampParChamp() throws Exception {
            GameState state = richState();
            assertEquals(List.of(), differences("partie", state, copy(state), false));
        }

        @Test
        void unePartieNeuveSeRelitAussi() throws Exception {
            assertEquals(List.of(), differences("partie", new GameState(), copy(new GameState()), false));
        }

        @Test
        void ecrireRelireEcrireDonneLeMemeTexte() throws Exception {
            String first = SaveCodec.write(richState(), 123);
            assertEquals(first, SaveCodec.write(SaveCodec.read(first).state(), 123));
        }

        @Test
        void lInstantDeLaSauvegardeEtLaVersionSontRendus() throws Exception {
            SaveCodec.Save save = SaveCodec.read(SaveCodec.write(new GameState(), 1_700_000_123_456L));
            assertEquals(1_700_000_123_456L, save.savedAt());
            assertEquals(SaveCodec.VERSION, save.version());
        }

        @Test
        void lesUniversSeRelisent() throws Exception {
            GameState state = new GameState();
            state.setExtraUniverses(4);
            state.setUniverseSeeds(BigNum.of(1.5, 40));
            state.setVoidEnergy(BigNum.of(12));
            GameState read = copy(state);
            assertEquals(4, read.extraUniverses());
            assertEquals(40.176, read.universeSeeds().log10(), 1e-3);
            assertEquals(12, read.voidEnergy().toDouble(), 1e-9);
        }

        /** Les quatre améliorations de l'univers (Brane, Bulk, Paysage, Omnivers) sont parties ; leurs niveaux, dans une ancienne sauvegarde, sont ignorés. */
        @Test
        void uneAncienneSauvegardeAvecDesAmeliorationsDeLUniversSeRelit() throws Exception {
            GameState state = new GameState();
            state.setExtraUniverses(2);
            String old = SaveCodec.write(state, 1_700_000_000_000L) + "\nuniverseUpgrades=brane:2,bulk:3\n";
            GameState read = SaveCodec.read(old).state();
            assertEquals(2, read.extraUniverses(), "le reste de la partie se relit");
            assertFalse(SaveCodec.write(read, 1_700_000_000_000L).contains("universeUpgrades"), "et la clé disparaît à la prochaine sauvegarde");
        }

        @Test
        void plusDeDeuxMilliardsDeMoleculesSeRelisent() throws Exception {
            GameState state = new GameState();
            state.addMolecules("H2O", 1_000_000_000);
            state.addMolecules("CO2", 1_000_000_000);
            state.addMolecules("CH4", 900_000_000);
            GameState read = copy(state);
            assertEquals(2_900_000_000L, read.moleculeTotal());
            assertEquals("CH4", read.moleculeAt(2_899_999_999L));
            assertEquals(1_000_000_000, read.moleculeCount("CO2"));
        }

        @Test
        void lesMoleculesGardentLOrdreDeLeurCreation() throws Exception {
            GameState state = richState();
            GameState read = copy(state);
            assertEquals(state.moleculeLog().size(), read.moleculeLog().size());
            assertEquals("H2O", read.moleculeLog().get(0));
            assertEquals("Ca5(PO4)3F", read.moleculeLog().get(12));
            assertEquals("H2O", read.moleculeLog().get(15));
            assertEquals("CO2", read.moleculeLog().get(read.moleculeLog().size() - 1));
            assertEquals(1_000_012, read.moleculeCount("H2O"));
            assertEquals(List.of("H2O", "CO2"), read.substances());
        }

        @Test
        void unIdentifiantPeutContenirLesSeparateurs() throws Exception {
            assertEquals("défi, avec : des = signes | bizarres % et *", copy(richState()).activeChallenge());
            assertEquals("a%2Cb%3Ac%3Dd%25e", SaveData.escape("a,b:c=d%e"));
            assertEquals("a,b:c=d%e", SaveData.unescape(SaveData.escape("a,b:c=d%e")));
        }

        @Test
        void lesNombresSeRelisentSansPerte() {
            for (double value : new double[] {0, 1, -1, 0.1, 1e-300, 1e300, 123456.789, Math.PI, 1e15, 12345678901234567.0,
                    Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                assertEquals(value, SaveData.parse(SaveData.number(value)), "valeur " + value);
            }
            assertTrue(Double.isNaN(SaveData.parse(SaveData.number(Double.NaN))));
            assertEquals("12", SaveData.number(12.0));
        }

        @Test
        void uneColonneDHistoriqueSeReplieEtSeDeplie() {
            double[] column = {Double.NaN, Double.NaN, 3, 3, 3, 1.5, Double.NEGATIVE_INFINITY, 133_026_596, 0, 0};
            String packed = StatsHistory.pack(column);
            assertEquals("*2;3*3;1.5;-Infinity;133026596;0*2", packed);
            double[] read = StatsHistory.unpack(packed, column.length);
            for (int i = 0; i < column.length; i++) assertEquals(column[i], read[i], "rang " + i);
            // Une valeur absente seule en tête, puis des valeurs : le séparateur ne doit pas se perdre.
            double[] holes = {Double.NaN, 4.5, Double.NaN, Double.NaN, 2, Double.NaN};
            assertEquals(";4.5;*2;2;", StatsHistory.pack(holes));
            double[] back = StatsHistory.unpack(StatsHistory.pack(holes), holes.length);
            for (int i = 0; i < holes.length; i++) assertEquals(holes[i], back[i], "rang " + i);
            // Une colonne absente de la sauvegarde : aucune valeur.
            for (double value : StatsHistory.unpack("", 4)) assertTrue(Double.isNaN(value));
        }

        @Test
        void uneListeQuiCommenceParUneValeurAbsenteSeRelit() {
            SaveData data = new SaveData();
            data.putDoubleList("liste", List.of(Double.NaN, 1.5, Double.NaN, 3.0));
            List<Double> read = data.doubleList("liste");
            assertEquals(4, read.size());
            assertTrue(Double.isNaN(read.get(0)) && read.get(1) == 1.5 && Double.isNaN(read.get(2)) && read.get(3) == 3.0);
        }

        @Test
        void lExportTientSurUneLigneEtSeRelit() throws Exception {
            GameState state = richState();
            String exported = SaveCodec.export(state, 55);
            assertTrue(exported.startsWith(SaveCodec.EXPORT_PREFIX));
            assertFalse(exported.contains("\n"));
            assertTrue(exported.matches("idle1:[A-Za-z0-9+/=]+"));
            SaveCodec.Save save = SaveCodec.read("  \n" + exported + "\r\n ");
            assertEquals(55, save.savedAt());
            assertEquals(List.of(), differences("partie", state, save.state(), false));
            // Un copier-coller qui a replié la ligne ne la casse pas.
            String folded = exported.substring(0, 80) + "\n" + exported.substring(80, 200) + " \r\n" + exported.substring(200);
            assertEquals(List.of(), differences("partie", state, SaveCodec.read(folded).state(), false));
        }

        @Test
        void lExportEstBienPlusCourtQueLeFichier() {
            GameState state = richState();
            assertTrue(SaveCodec.export(state, 0).length() < SaveCodec.write(state, 0).length());
        }

        @Test
        void ceQuiNEstPasUneSauvegardeEstRefuse() {
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read(null));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read("   "));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read("bonjour"));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read("idle1:pas du base64 !!"));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read("idle1:aGVsbG8="));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read("idle-save x\n"));
        }

        @Test
        void unExportTronqueEstRefuse() {
            String exported = SaveCodec.export(richState(), 0);
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read(exported.substring(0, exported.length() / 2)));
        }

        @Test
        void uneSauvegardeDUneVersionPlusRecenteEstRefusee() {
            String future = SaveCodec.write(new GameState(), 0).replaceFirst("idle-save " + SaveCodec.VERSION,
                    "idle-save " + (SaveCodec.VERSION + 1));
            SaveCodec.Unreadable refused = assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read(future));
            assertTrue(refused.getMessage().contains("plus récente"));
        }

        @Test
        void uneValeurImpossibleEstRefusee() {
            String text = SaveCodec.write(richState(), 0);
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read(text.replaceFirst("\nexplosions=64\n", "\nexplosions=-3\n")));
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.read(text.replaceFirst("\nexplosions=64\n", "\nexplosions=beaucoup\n")));
        }

        @Test
        void uneFamilleDeSyntheseInconnueNeRendPasLaSauvegardeIllisible() throws Exception {
            // Comme les autres noms d'une autre version, elle est écartée ; la synthèse repart sans cible.
            String text = SaveCodec.write(richState(), 0);
            String changed = text.replaceFirst("\nsynthesisTarget=[A-Z_]+\n", "\nsynthesisTarget=INCONNUE\n");
            assertNotEquals(text, changed, "la partie d'essai doit avoir une cible");
            GameState read = SaveCodec.read(changed).state();
            assertNull(read.synthesisTarget());
            assertEquals(64, read.explosions());           // le reste est bien là
        }

        @Test
        void uneCleAbsentePrendLaValeurDUnePartieNeuve() throws Exception {
            String text = SaveCodec.write(richState(), 0);
            GameState read = SaveCodec.read(text.replaceFirst("\nexplosions=64\n", "\n").replaceFirst("\nautoHold=0\n", "\n")).state();
            assertEquals(0, read.explosions());
            assertTrue(read.autoHold());
            assertEquals(9, read.tableWeightLevel());      // le reste est bien là
        }

        @Test
        void uneCleInconnueEstIgnoree() throws Exception {
            String text = SaveCodec.write(richState(), 0) + "champDUneAutreVersion=42\nligne sans signe egal\n\n";
            assertEquals(List.of(), differences("partie", richState(), SaveCodec.read(text).state(), false));
        }

        @Test
        void uneStatistiqueInconnueDeLHistoriqueEstIgnoreeEtUneAbsenteResteSansValeur() throws Exception {
            String text = SaveCodec.write(richState(), 0);
            String changed = text.replaceFirst("\nstats\\.history\\.v\\.PRODUCTION=", "\nstats.history.v.DISPARUE=");
            assertFalse(changed.equals(text));
            GameState read = SaveCodec.read(changed).state();
            assertEquals(richState().stats().history().samples().size(), read.stats().history().samples().size());
            for (StatsHistory.Sample sample : read.stats().history().samples()) {
                assertTrue(Double.isNaN(sample.value(StatsHistory.Stat.PRODUCTION)));
            }
        }

        @Test
        void lireDansUnePartieEnCoursLaRemplace() throws Exception {
            GameState target = new GameState();
            target.setStarted(true);
            target.setExplosions(3);
            target.addAchievement("ancien");
            GameStats before = target.stats();
            SaveCodec.Save save = SaveCodec.readInto(target, SaveCodec.export(richState(), 77));
            assertEquals(77, save.savedAt());
            assertEquals(List.of(), differences("partie", richState(), target, false));
            assertFalse(target.achievements().contains("ancien"));
            assertFalse(before == target.stats(), "les statistiques d'avant ne doivent plus servir");
        }

        @Test
        void unTexteIllisibleNeToucheParALaPartieEnCours() {
            GameState target = richState();
            assertThrows(SaveCodec.Unreadable.class, () -> SaveCodec.readInto(target, "n'importe quoi"));
            assertEquals(List.of(), differences("partie", richState(), target, false));
        }

        @Test
        void uneSauvegardeDAvantLesCinqNiveauxDePersistanceGardeTout() throws Exception {
            // Persistance a cinq niveaux ; le seul niveau d'avant gardait tout, il devient le cinquième.
            GameState state = new GameState();
            state.setLevel("atom_keep", 1);
            String text = SaveCodec.write(state, 0);
            assertTrue(text.contains("persistenceTiers="));
            assertEquals(1, SaveCodec.read(text).state().levelOf("atom_keep"), "une sauvegarde d'aujourd'hui garde son niveau");
            StringBuilder old = new StringBuilder();
            for (String line : text.split("\n")) if (!line.startsWith("persistenceTiers=")) old.append(line).append('\n');
            assertEquals(5, SaveCodec.read(old.toString()).state().levelOf("atom_keep"), "le seul niveau d'avant gardait tout");
            state.setLevel("atom_keep", 0);
            StringBuilder none = new StringBuilder();
            for (String line : SaveCodec.write(state, 0).split("\n")) if (!line.startsWith("persistenceTiers=")) none.append(line).append('\n');
            assertEquals(0, SaveCodec.read(none.toString()).state().levelOf("atom_keep"), "pas de Persistance, rien à relever");
        }

        @Test
        void laChronologieDesUniversEtDesDimensionsSeSauvegarde() throws Exception {
            // Le journal des univers, les moments de la partie secondaire et le temps passé dans chaque
            // dimension s'écrivent ; une sauvegarde d'avant n'a pas de moments, et ce qui est déjà atteint reste sans date.
            Game game = GameTestSupport.realGame();
            game.state().setTimePlayed(3_600);
            game.testFormUniverse();
            game.noteDimensionPlayed(1, 0.5);
            game.tick(2);
            GameState back = copy(game.state());
            assertEquals(1, back.stats().universeLog().size());
            assertEquals(game.state().stats().universeLog().get(0), back.stats().universeLog().get(0));
            assertEquals(game.state().stats().moment("dimension.1.ouverte"), back.stats().moment("dimension.1.ouverte"), 1e-9);
            assertEquals(0.5, back.stats().dimensionPlayTime(1), 1e-9);
            StringBuilder old = new StringBuilder();
            for (String line : SaveCodec.write(game.state(), 0).split("\n")) {
                if (!line.startsWith("stats.moments=") && !line.startsWith("stats.universeLog=")) old.append(line).append('\n');
            }
            Game legacy = new Game(SaveCodec.read(old.toString()).state(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                    Achievements.DEFAULT, new Random(1));
            assertTrue(legacy.state().stats().universeLog().isEmpty());
            legacy.tick(2);
            assertTrue(legacy.state().stats().momentKnown("dimension.1.ouverte"));
            assertTrue(Double.isNaN(legacy.state().stats().moment("dimension.1.ouverte")), "atteint avant la 0.168 : sans date");
        }

        @Test
        void laSauvegardeDUneLonguePartieResteLegere() {
            GameState state = richState();
            String text = SaveCodec.write(state, 0);
            String exported = SaveCodec.export(state, 0);
            assertTrue(text.length() < 400_000, "fichier de " + text.length() + " caractères");
            assertTrue(exported.length() < 200_000, "export de " + exported.length() + " caractères");
        }
    }
}

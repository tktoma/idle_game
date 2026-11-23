package idle.core;

import idle.core.state.*;
import idle.core.achievements.*;
import idle.core.collider.*;
import idle.core.darkmatter.*;
import idle.core.profile.Title;
import idle.core.universe.*;
import idle.core.elements.*;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.*;
import idle.core.upgrades.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Les univers : la partie secondaire, le deuxième univers et les skins à obtenir. */
public abstract class GameUniverse extends GameSecondUniverse {
    GameUniverse(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

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

    /** Les essences de l'arbre gagnées : une par univers formé à partir du quatrième, huit au plus. */
    public int treeSkinsEarned() {
        return Math.max(0, Math.min(Skin.TREES.length, universes() - Skin.FIRST_TREE_UNIVERSE + 1));
    }

    /** Les skins des paliers et du record se débloquent seuls, dès que l'onglet Skins est ouvert. */
    void noteSkinUnlocks() {
        if (!isSkinsUnlocked()) return;
        for (int i = 0; i < darkSkinTiers(); i++) unlockSkin(DARK_SKINS[i]);
        for (int i = 0; i < bangSkinTiers(); i++) unlockSkin(BANG_SKINS[i]);
        // Une essence de l'arbre par univers formé, à partir du quatrième.
        for (int i = 0; i < treeSkinsEarned(); i++) unlockSkin(Skin.TREES[i]);
        // Le skin de chaque mini-jeu, au seuil de sa dimension.
        for (int n = 1; n <= MAX_DIMENSIONS; n++) {
            if (isDimensionThresholdReached(n)) unlockSkin(Skin.DIMENSION_SKINS[n - 1]);
        }
        // Les molécules.
        if (moleculeKindsCreated() >= 50) unlockSkin(Skin.MOLECULE_SPACE_FILLING);
        if (moleculeKindsCreated() >= molecules().size()) unlockSkin(Skin.MOLECULE_SKELETON);
        if (substancesFormed() >= 20) unlockSkin(Skin.MOLECULE_PEARLS);
        if (moleculesCreated() >= 1_000) unlockSkin(Skin.MOLECULE_BRICKS);
        // Les cadres de l'avatar, un par titre.
        if (hasTitle(Title.ALCHEMIST)) unlockSkin(Skin.FRAME_ROUND);
        if (hasTitle(Title.TABLE_READER)) unlockSkin(Skin.FRAME_HEXAGON);
        if (hasTitle(Title.CHALLENGER)) unlockSkin(Skin.FRAME_SHIELD);
        if (hasTitle(Title.BANG_MAKER)) unlockSkin(Skin.FRAME_BAROQUE);
        if (hasTitle(Title.COMET_CHASER)) unlockSkin(Skin.FRAME_STAMP);
        if (hasTitle(Title.UNIVERSE_CREATOR)) unlockSkin(Skin.FRAME_INSTANT_PHOTO);
        // Le chrome.
        if (state.timePlayed() >= 3600) unlockSkin(Skin.AMBIENT_PAPER);
        if (discoveredElements() >= 50) unlockSkin(Skin.AMBIENT_SLATE);
        if (state.hasGalaxy()) unlockSkin(Skin.AMBIENT_NIGHT);
        if (state.explosions() >= 100) unlockSkin(Skin.AMBIENT_TERMINAL);
        if (assembliesFormed() >= 10) unlockSkin(Skin.AMBIENT_BLUEPRINT);
        if (isBigBangUnlocked()) unlockSkin(Skin.TABS_BINDER);
        if (state.timePlayed() >= 10 * 3600) unlockSkin(Skin.TABS_BUTTONS);
        if (universes() >= 1) unlockSkin(Skin.TABS_DOTS);
        if (universes() >= 2) unlockSkin(Skin.TABS_RIBBONS);
        if (state.stats().fusions() >= 100) unlockSkin(Skin.CARD_INDEX);
        if (hasSkin(Skin.BANG_CRYSTAL)) unlockSkin(Skin.CARD_GLASS);
        if (baseChallengesCompleted() >= 10) unlockSkin(Skin.CARD_PLAYING);
        if (state.probeRuns() >= 10) unlockSkin(Skin.CARD_TAG);
        // Des skins de plus pour les huit visuels d'origine.
        long fusions = state.stats().fusions();
        if (fusions >= 100) unlockSkin(Skin.FUSION_EMBER);
        if (fusions >= 1_000) unlockSkin(Skin.FUSION_INK);
        if (generatorCount() >= 100) unlockSkin(Skin.FUSION_SWARM);
        if (fusions >= 10_000) unlockSkin(Skin.FUSION_CLOCK);
        if (hasSkin(Skin.FUSION_PRISM)) unlockSkin(Skin.FUSION_GLASS);
        if (state.rebuildRecord() > 0 && state.rebuildRecord() <= 30) unlockSkin(Skin.FUSION_PIXEL);
        if (state.totalAtoms().gte(BigNum.of(50))) unlockSkin(Skin.ATOM_CHALK);
        if (state.totalAtoms().gte(BigNum.of(1_000))) unlockSkin(Skin.ATOM_CLOUD);
        if (isPeriodicTableComplete()) unlockSkin(Skin.ATOM_RINGS);
        if (state.synthesisCount() >= 1_000) unlockSkin(Skin.ATOM_GEARS);
        if (discoveredElements() >= 100) unlockSkin(Skin.ATOM_STAINED);
        if (discoveredElements() >= 50) unlockSkin(Skin.TABLE_WOOD);
        if (state.synthesisCount() >= 10_000) unlockSkin(Skin.TABLE_CRYSTAL);
        if (state.completedChallenges().contains("challenge_rust")) unlockSkin(Skin.TABLE_RUST);
        if (hasSkin(Skin.COLLIDER_NEON)) unlockSkin(Skin.TABLE_NEON);
        if (state.probeRuns() >= 10) unlockSkin(Skin.TABLE_MAP);
        if (discoveredElements() >= 1) unlockSkin(Skin.TABLE_TINY);
        int explosions = state.explosions();
        if (explosions >= 25) unlockSkin(Skin.DARK_MERCURY);
        if (explosions >= 10) unlockSkin(Skin.DARK_HOLE);
        if (explosions >= 200) unlockSkin(Skin.DARK_KNOT);
        if (explosions >= 500) unlockSkin(Skin.DARK_LANTERN);
        if (state.bigBangs() >= 5) unlockSkin(Skin.BANG_INK);
        if (solarSystemsComplete() >= 3) unlockSkin(Skin.BANG_STAINED);
        if (state.hasBody("ocean_planet")) unlockSkin(Skin.BANG_ABYSS);
        if (state.bigBangs() >= 10) unlockSkin(Skin.BANG_ORIGAMI);
        if (state.cometsCaught() >= 25) unlockSkin(Skin.BANG_MAP);
        if (state.bigBangs() >= 20) unlockSkin(Skin.BANG_NEON);
        if (state.cometsCaught() >= 50) unlockSkin(Skin.BEAM_COMET);
        if (state.collisionRuns() >= 1_000) unlockSkin(Skin.BEAM_INK);
        if (state.collisionRuns() >= 10_000) unlockSkin(Skin.BEAM_LASER);
        if (state.collisionRuns() >= 100) unlockSkin(Skin.BEAM_FIREFLIES);
        if (colliderGeneratorsBoughtTotal() >= 10) unlockSkin(Skin.COLLIDER_WOOD);
        if (colliderGeneratorsBoughtTotal() >= 100) unlockSkin(Skin.COLLIDER_CIRCUIT);
        if (colliderGeneratorsBoughtTotal() >= 1_000) unlockSkin(Skin.COLLIDER_STADIUM);
        if (hasSkin(Skin.COLLIDER_ICE)) unlockSkin(Skin.COLLIDER_MOBIUS);
        // Les icônes d'onglet, les signes, les info-bulles, le fond des particules, leur fusion, la page du tableau.
        if (state.totalAtoms().gte(BigNum.of(50))) unlockSkin(Skin.ICONS_CHALK);
        if (state.rebuildRecord() > 0 && state.rebuildRecord() <= REBUILD_FAST_SECONDS) unlockSkin(Skin.ICONS_PIXEL);
        if (state.hasGalaxy()) unlockSkin(Skin.ICONS_CONSTELLATION);
        if (hasBubble(TreeBubble.LEAVES_RIGHT)) unlockSkin(Skin.ICONS_EMOJI);
        if (hasAchievement("step_fusions_100")) unlockSkin(Skin.SIGNS_RUNES);
        if (hasAchievement("step_set")) unlockSkin(Skin.SIGNS_SHAPES);
        if (hasAchievement("step_explosions")) unlockSkin(Skin.SIGNS_LETTERS);
        int substances = substancesFormed();
        if (substances >= 20) unlockSkin(Skin.MATTER_LETTERS);
        if (substances >= 50) unlockSkin(Skin.MATTER_DROPS);
        if (substances >= molecules().size()) unlockSkin(Skin.MATTER_ALCHEMY);
        double played = state.timePlayed();
        if (played >= 3600) unlockSkin(Skin.TOOLTIP_PARCHMENT);
        if (played >= 5 * 3600) unlockSkin(Skin.TOOLTIP_STICKY_NOTE);
        if (played >= 10 * 3600) unlockSkin(Skin.TOOLTIP_TERMINAL);
        BigNum particlesCreated = state.stats().particlesCreated();
        for (int i = 0; i < Skin.DUSTS.length; i++) {
            int exponent = Skin.FIRST_DUST_EXPONENT + i * Skin.DUST_EXPONENT_STEP;
            if (particlesCreated.gte(BigNum.of(1, exponent))) unlockSkin(Skin.DUSTS[i]);
        }
        if (fusions >= 100) unlockSkin(Skin.GENERATORS_SWIRL);
        if (fusions >= 1_000) unlockSkin(Skin.GENERATORS_SUCTION);
        if (hasSkin(Skin.FUSION_FLASH)) unlockSkin(Skin.GENERATORS_SHATTER);
        if (completedSets() >= 1) unlockSkin(Skin.PAGE_MEDALS);
        if (state.synthesisCount() >= 100) unlockSkin(Skin.PAGE_CRUCIBLE);
        if (familiesOwned() >= ElementCategory.values().length) unlockSkin(Skin.PAGE_SPECTRUM);
        // La matière noire hors de la boule : l'échelle et la règle, l'arbre, les défis.
        if (explosions >= 10) unlockSkin(Skin.RULER_WOOD);
        if (explosions >= 20) unlockSkin(Skin.RULER_ARCHITECT);
        if (hasAchievement("step_landmarks")) unlockSkin(Skin.RULER_CELESTIAL);
        if (darkCellsBought() >= 30) unlockSkin(Skin.DARK_TREE_ROOTS);
        if (darkLevelsBought() >= 100) unlockSkin(Skin.DARK_TREE_CIRCUIT);
        if (state.hasGalaxy()) unlockSkin(Skin.DARK_TREE_CONSTELLATION);
        if (darkBranchesOpened() >= idle.core.darkmatter.DarkUpgrade.Branch.values().length) unlockSkin(Skin.DARK_TREE_METRO);
        int challengesDone = state.completedChallenges().size();
        if (challengesDone >= 3) unlockSkin(Skin.CHALLENGES_TAROT);
        if (challengesDone >= 6) unlockSkin(Skin.CHALLENGES_WANTED);
        if (baseChallengesCompleted() >= Challenges.DEFAULT.size()) unlockSkin(Skin.CHALLENGES_SEALS);
        // Le Big Bang au-delà des molécules : les amas, les blocs, le gaz, le ciel, le cosmos, les systèmes, les sondes.
        if (substances >= 5) unlockSkin(Skin.CLUMPS_MINERALS);
        if (substances >= 20) unlockSkin(Skin.CLUMPS_NEON);
        if (substances >= 50) unlockSkin(Skin.CLUMPS_PASTEL);
        for (int i = 0; i < Skin.BLOCKS.length; i++) {
            if (assembliesFormed() >= Skin.BLOCKS_FORMED[i]) unlockSkin(Skin.BLOCKS[i]);
        }
        long gas = gatheredInState(Molecule.State.GAS);
        if (gas >= 1_000) unlockSkin(Skin.GAS_HAZE);
        if (gas >= 10_000) unlockSkin(Skin.GAS_SMOKE);
        if (gas >= 100_000) unlockSkin(Skin.GAS_AURORA);
        if (bodiesFormed() >= 15) unlockSkin(Skin.SKY_PLATE);
        if (bodiesFormed() >= 25) unlockSkin(Skin.SKY_MOBILE);
        if (bodiesFormed() >= 35) unlockSkin(Skin.SKY_SHOWCASE);
        // La galaxie et l'amas de l'univers en cours, ou un univers déjà formé, qui les a eus : le premier skin ; les suivants, un par univers formé.
        int formedUniverses = universes();
        if (state.hasGalaxy() || formedUniverses >= 1) unlockSkin(Skin.GALAXIES[0]);
        if (state.cosmosLevel() >= 2 || formedUniverses >= 1) unlockSkin(Skin.CLUSTERS[0]);
        for (int i = 1; i < Skin.GALAXIES.length; i++) {
            if (formedUniverses >= i + 1) unlockSkin(Skin.GALAXIES[i]);
        }
        for (int i = 1; i < Skin.CLUSTERS.length; i++) {
            if (formedUniverses >= i + 1) unlockSkin(Skin.CLUSTERS[i]);
        }
        for (int i = 0; i < Skin.WEBS.length; i++) {
            if (formedUniverses >= i + 1) unlockSkin(Skin.WEBS[i]);
        }
        if (solarSystemsComplete() >= 1) unlockSkin(Skin.SYSTEM_ORRERY);
        if (solarSystemsComplete() >= 2) unlockSkin(Skin.SYSTEM_CRAYON);
        if (solarSystemsComplete() >= 3) unlockSkin(Skin.SYSTEM_RADAR);
        for (int i = 0; i < Skin.PROBES.length; i++) {
            if (state.probeRuns() >= Skin.PROBE_RUNS[i]) unlockSkin(Skin.PROBES[i]);
        }
        // Les skins des astres, par le nombre d'astres formés dans l'univers en cours.
        for (int i = 0; i < Skin.BODIES.length; i++) {
            if (bodiesFormed() >= Skin.BODIES_FORMED[i]) unlockSkin(Skin.BODIES[i]);
        }
        noteColliderHadronVoidSkins();
        noteUniverseSkins();
        noteTreeAndDimensionSkins();
        noteProfileAndStatsSkins();
        noteWindowSkins();
        if (state.rebuildPending() && generatorCount() >= generatorsPerAtom()) {
            double time = state.timeSinceFusion();
            state.setRebuildPending(false);
            if (state.rebuildRecord() <= 0 || time < state.rebuildRecord()) state.setRebuildRecord(time);
            if (time <= REBUILD_FAST_SECONDS) unlockSkin(Skin.FUSION_FLASH);
        }
    }

    /**
     * Les skins du reste du collisionneur, des hadrons et du vide. Le détecteur suit les collisions, les
     * éclairs les croisements, les étincelles trois épreuves du collisionneur, l'envol des quarks et les quarks de la
     * liaison les hadrons liés (le Puzzle : chaque sorte au moins une fois), les gluons leur compte, le fond des hadrons
     * trois paliers de gluons, l'écume le vide ouvert puis le plus haut niveau d'une amélioration du vide, les paires
     * celles nées depuis l'ouverture de la fenêtre, le trou noir du vide les univers qu'il compte une fois ouvert.
     */
    void noteColliderHadronVoidSkins() {
        for (int i = 0; i < Skin.DETECTORS.length; i++) {
            if (state.collisionRuns() >= Skin.DETECTOR_COLLISIONS[i]) unlockSkin(Skin.DETECTORS[i]);
        }
        for (int i = 0; i < Skin.LIGHTNINGS.length; i++) {
            if (state.colliderCrossings() >= Skin.LIGHTNING_CROSSINGS[i]) unlockSkin(Skin.LIGHTNINGS[i]);
        }
        if (isTrialCompleted(Trial.DETECTOR)) unlockSkin(Skin.SPARKS_PETALS);
        if (isTrialCompleted(Trial.NO_OVERHEAT)) unlockSkin(Skin.SPARKS_CONFETTI);
        if (isTrialCompleted(Trial.FULL_CHARGE)) unlockSkin(Skin.SPARKS_EMBERS);
        long hadrons = hadronsMade();
        for (int i = 0; i < Skin.QUARK_FLIGHTS.length; i++) {
            if (hadrons >= Skin.QUARK_FLIGHT_HADRONS[i]) unlockSkin(Skin.QUARK_FLIGHTS[i]);
        }
        for (int i = 0; i < Skin.QUARK_SKINS.length; i++) {
            if (hadrons >= Skin.QUARK_SKIN_HADRONS[i]) unlockSkin(Skin.QUARK_SKINS[i]);
        }
        boolean everyKind = true;
        for (Hadron hadron : Hadron.values()) if (state.hadrons(hadron) <= 0) everyKind = false;
        if (everyKind) unlockSkin(Skin.QUARKS_PUZZLE);
        long gluons = gluons();
        for (int i = 0; i < Skin.GLUON_SKINS.length; i++) {
            if (gluons >= Skin.GLUON_SKIN_GLUONS[i]) unlockSkin(Skin.GLUON_SKINS[i]);
        }
        if (isGluonMilestoneReached(GluonMilestone.UPGRADES)) unlockSkin(Skin.HADRON_BACK_BLACKBOARD);
        if (isGluonMilestoneReached(GluonMilestone.AUTOMATIONS)) unlockSkin(Skin.HADRON_BACK_LAB);
        if (isGluonMilestoneReached(GluonMilestone.ELEMENTS)) unlockSkin(Skin.HADRON_BACK_STORM);
        if (isVoidUnlocked()) {
            unlockSkin(Skin.FOAMS[0]);
            int highest = 0;
            for (VoidUpgrade upgrade : VoidUpgrade.values()) highest = Math.max(highest, voidLevel(upgrade));
            for (int i = 1; i < Skin.FOAMS.length; i++) if (highest >= i) unlockSkin(Skin.FOAMS[i]);
        }
        for (int i = 0; i < Skin.PAIR_SKINS.length; i++) {
            if (voidPairsBorn() >= Skin.PAIR_SKIN_BORN[i]) unlockSkin(Skin.PAIR_SKINS[i]);
        }
        if (isVoidUnlocked() && state.voidHoleOpen()) {
            for (int i = 0; i < Skin.VOID_HOLES.length; i++) {
                if (voidHoleUniverses() >= i + 1) unlockSkin(Skin.VOID_HOLES[i]);
            }
        }
    }

    /**
     * Les skins de l'univers. Les bulles suivent les univers formés (une par univers, du premier au troisième),
     * la naissance et le lancement les univers à partir du cinquième, les liens à partir du septième ; la graine ses paliers ;
     * l'arrosoir les arrosages de la graine en cours, le tuyau et la terre fertile achetés ; l'étagère l'étage du chrono
     * acheté et les univers formés à partir du troisième ; le tuyau ses niveaux ; le chrono plein, puis les univers formés ;
     * le rond de croissance les ronds cueillis, la météo les orages, les saisons leurs cycles complets, tous trois comptés
     * depuis l'ouverture de la fenêtre (les skins, eux, restent).
     */
    void noteUniverseSkins() {
        int formed = universes();
        for (int i = 0; i < Skin.UNIVERSE_BUBBLES.length; i++) {
            if (formed >= i + 1) unlockSkin(Skin.UNIVERSE_BUBBLES[i]);
        }
        for (int i = 0; i < Skin.BIRTHS.length; i++) {
            if (formed >= Skin.FIRST_BIRTH_UNIVERSE + i) unlockSkin(Skin.BIRTHS[i]);
        }
        for (int i = 0; i < Skin.LINKS.length; i++) {
            if (formed >= Skin.FIRST_LINKS_UNIVERSE + i) unlockSkin(Skin.LINKS[i]);
        }
        for (int i = 0; i < Skin.LAUNCHES.length; i++) {
            if (formed >= Skin.FIRST_LAUNCH_UNIVERSE + i) unlockSkin(Skin.LAUNCHES[i]);
        }
        if (isUniverseFormed()) {
            int stage = seedStage();
            for (int i = 0; i < Skin.SEEDS.length; i++) {
                if (stage >= i + 1) unlockSkin(Skin.SEEDS[i]);
            }
        }
        for (int i = 0; i < Skin.CANS.length; i++) {
            if (state.seedWaterings() >= Skin.CAN_WATERINGS[i]) unlockSkin(Skin.CANS[i]);
        }
        if (hasHose()) unlockSkin(Skin.CAN_BEAK);
        if (seedUpgradeLevel(SeedUpgrade.SOIL) > 0) unlockSkin(Skin.CAN_SHELL);
        if (isChronoUnlocked()) {
            for (int i = 0; i < Skin.SHELVES.length; i++) {
                if (formed >= Skin.FIRST_SHELF_UNIVERSE + i) unlockSkin(Skin.SHELVES[i]);
            }
            if (chronoStored() >= chronoCapacity() - 1e-6) {
                unlockSkin(Skin.CHRONOS[0]);
                for (int i = 1; i < Skin.CHRONOS.length; i++) {
                    if (formed >= Skin.FIRST_CHRONO_UNIVERSE + i - 1) unlockSkin(Skin.CHRONOS[i]);
                }
            }
        }
        int hose = seedUpgradeLevel(SeedUpgrade.HOSE);
        for (int i = 0; i < Skin.HOSES.length; i++) {
            if (hose >= Skin.HOSE_LEVELS[i]) unlockSkin(Skin.HOSES[i]);
        }
        for (int i = 0; i < Skin.RINGS.length; i++) {
            if (bloomsCaught() >= Skin.RING_CAUGHT[i]) unlockSkin(Skin.RINGS[i]);
        }
        for (int i = 0; i < Skin.WEATHERS.length; i++) {
            if (storms() >= Skin.WEATHER_STORMS[i]) unlockSkin(Skin.WEATHERS[i]);
        }
        for (int i = 0; i < Skin.SEASON_SKINS.length; i++) {
            if (seasonCycles() >= i + 1) unlockSkin(Skin.SEASON_SKINS[i]);
        }
    }

    /**
     * Les skins de l'arbre au-delà de ses essences, de ses bulles, de la carte des dimensions, du défi
     * final et des cadres des mini-jeux. Les saisons figées suivent les saisons débloquées puis les univers formés ; les
     * arbres de fantaisie le nombre de paliers de la graine dont une bulle est achetée ; les sols les paliers de la
     * graine ; les bulles et leurs liens le nombre de bulles achetées ; la carte et les cadres les dimensions ouvertes ;
     * le remplissage du défi final le défi fini, puis les univers formés. La palette des dimensions est libre.
     */
    void noteTreeAndDimensionSkins() {
        int formed = universes();
        if (areSeasonsUnlocked()) {
            for (int i = 0; i < Skin.TREE_SEASONS.length; i++) {
                if (formed >= Skin.FIRST_TREE_SEASON_UNIVERSE + i) unlockSkin(Skin.TREE_SEASONS[i]);
            }
        }
        int stages = treeBubbleStagesBought();
        for (int i = 0; i < Skin.TREE_FANTASIES.length; i++) {
            if (stages >= i + 1) unlockSkin(Skin.TREE_FANTASIES[i]);
        }
        if (isUniverseFormed()) {
            int stage = seedStage();
            for (int i = 0; i < Skin.GROUNDS.length; i++) {
                if (stage >= Skin.FIRST_GROUND_STAGE + i) unlockSkin(Skin.GROUNDS[i]);
            }
        }
        int bubbles = treeBubblesBought();
        for (int i = 0; i < Skin.TREE_BUBBLE_SKINS.length; i++) {
            if (bubbles >= Skin.TREE_BUBBLE_COUNTS[i]) unlockSkin(Skin.TREE_BUBBLE_SKINS[i]);
        }
        for (int i = 0; i < Skin.BUBBLE_LINK_SKINS.length; i++) {
            if (bubbles >= Skin.BUBBLE_LINK_COUNTS[i]) unlockSkin(Skin.BUBBLE_LINK_SKINS[i]);
        }
        int dimensions = dimensionsUnlocked();
        for (int i = 0; i < Skin.MAPS.length; i++) {
            if (dimensions >= Skin.MAP_DIMENSIONS[i]) unlockSkin(Skin.MAPS[i]);
        }
        for (int i = 0; i < Skin.FRAMES.length; i++) {
            if (dimensions >= Skin.FRAME_DIMENSIONS[i]) unlockSkin(Skin.FRAMES[i]);
        }
        if (state.gameFinished()) {
            unlockSkin(Skin.FINALES[0]);
            for (int i = 1; i < Skin.FINALES.length; i++) {
                if (formed >= Skin.FIRST_FINALE_UNIVERSE + i - 1) unlockSkin(Skin.FINALES[i]);
            }
        }
    }

    /**
     * Les skins du profil, des succès et des statistiques. Les styles des avatars, les tuiles des succès et
     * les succès non obtenus suivent les succès obtenus ; les albums les entrées trouvées puis les albums complets ; les
     * courbes les heures de jeu ; la frise les univers formés. La palette des courbes est libre.
     */
    void noteProfileAndStatsSkins() {
        for (int i = 0; i < Skin.AVATAR_STYLES.length; i++) {
            if (achievementsReach(Skin.AVATAR_STYLE_ACHIEVEMENTS[i])) unlockSkin(Skin.AVATAR_STYLES[i]);
        }
        for (int i = 0; i < Skin.TILES.length; i++) {
            if (achievementsReach(Skin.TILE_ACHIEVEMENTS[i])) unlockSkin(Skin.TILES[i]);
        }
        for (int i = 0; i < Skin.LOCKED_SKINS.length; i++) {
            if (achievementsReach(Skin.LOCKED_ACHIEVEMENTS[i])) unlockSkin(Skin.LOCKED_SKINS[i]);
        }
        if (albumEntriesFound() >= Skin.ALBUM_ENTRIES) unlockSkin(Skin.ALBUMS[0]);
        int complete = albumsComplete();
        for (int i = 1; i < Skin.ALBUMS.length; i++) {
            if (complete >= i) unlockSkin(Skin.ALBUMS[i]);
        }
        for (int i = 0; i < Skin.CHARTS.length; i++) {
            if (state.timePlayed() >= Skin.CHART_HOURS[i] * 3600.0) unlockSkin(Skin.CHARTS[i]);
        }
        for (int i = 0; i < Skin.TIMELINES.length; i++) {
            if (universes() >= Skin.TIMELINE_UNIVERSES[i]) unlockSkin(Skin.TIMELINES[i]);
        }
    }

    /**
     * Les skins de la fenêtre autour du jeu. Les notifications suivent celles reçues depuis l'ouverture du
     * jeu ({@link #noteNotificationsShown}), les panneaux les heures de jeu, la transition les univers formés, l'écran de
     * fin et le bouton de démarrage la partie finie puis les univers formés, le curseur un succès d'un onglet chacun.
     */
    void noteWindowSkins() {
        for (int i = 0; i < Skin.TOASTS.length; i++) {
            if (notificationsShown >= Skin.TOAST_NOTIFICATIONS[i]) unlockSkin(Skin.TOASTS[i]);
        }
        for (int i = 0; i < Skin.PANELS.length; i++) {
            if (state.timePlayed() >= Skin.PANEL_HOURS[i] * 3600.0) unlockSkin(Skin.PANELS[i]);
        }
        int formed = universes();
        for (int i = 0; i < Skin.TRANSITIONS.length; i++) {
            if (formed >= i + 1) unlockSkin(Skin.TRANSITIONS[i]);
        }
        if (state.gameFinished()) {
            unlockSkin(Skin.START_BUTTONS[0]);
            for (int i = 1; i < Skin.START_BUTTONS.length; i++) {
                if (formed >= Skin.FIRST_START_UNIVERSE + i - 1) unlockSkin(Skin.START_BUTTONS[i]);
            }
        }
        for (int i = 0; i < Skin.CURSORS.length; i++) {
            if (hasAchievement(Skin.CURSOR_ACHIEVEMENTS[i])) unlockSkin(Skin.CURSORS[i]);
        }
    }

    /**
     * La fenêtre dit combien de notifications elle a affichées depuis l'ouverture du jeu : les skins des
     * notifications s'en débloquent au pas suivant. Le compte ne descend jamais et n'est pas sauvegardé.
     */
    public void noteNotificationsShown(int total) { notificationsShown = Math.max(notificationsShown, total); }

    /** Les notifications affichées depuis l'ouverture du jeu. */
    public int notificationsShown() { return notificationsShown; }

    /** Vrai si {@code needed} succès du catalogue sont obtenus ; {@link idle.core.universe.Skin#ALL_ACHIEVEMENTS} les demande tous. */
    boolean achievementsReach(int needed) {
        if (needed == Skin.ALL_ACHIEVEMENTS) return !achievements().isEmpty() && achievementCount() >= achievements().size();
        return achievementCount() >= needed;
    }

    /** Les entrées trouvées dans les quatre albums du profil, ensemble. */
    public int albumEntriesFound() {
        return state.foundMolecules().size() + state.foundSubstances().size() + state.foundAssemblies().size() + state.foundBodies().size();
    }

    /** Les albums du profil complets, de 0 à 4 : les molécules, les sortes rassemblées, les assemblages, les astres. */
    public int albumsComplete() {
        int sorts = 0;
        for (Molecule molecule : molecules()) if (molecule.hasState()) sorts++;
        int complete = 0;
        if (albumComplete(state.foundMolecules(), molecules().size())) complete++;
        if (albumComplete(state.foundSubstances(), sorts)) complete++;
        if (albumComplete(state.foundAssemblies(), assemblies().size())) complete++;
        if (albumComplete(state.foundBodies(), bodies().size())) complete++;
        return complete;
    }

    /** Les bulles de l'arbre achetées, niveaux à part : de 0 à {@code TreeBubble.values().length}. */
    public int treeBubblesBought() {
        int bought = 0;
        for (TreeBubble bubble : TreeBubble.values()) if (isTreeBubbleBought(bubble)) bought++;
        return bought;
    }

    /** Les paliers de la graine dont une bulle au moins est achetée : de 0 à 8. */
    public int treeBubbleStagesBought() {
        Set<Integer> stages = new HashSet<>();
        for (TreeBubble bubble : TreeBubble.values()) if (isTreeBubbleBought(bubble)) stages.add(bubble.stage());
        return stages.size();
    }

    /** Les cases de l'arbre de matière noire achetées au moins une fois, liaisons comprises. */
    public int darkCellsBought() {
        int bought = 0;
        for (idle.core.darkmatter.DarkUpgrade dark : darkUpgrades()) if (state.darkLevelOf(dark.id()) > 0) bought++;
        return bought;
    }

    /** Les niveaux achetés dans l'arbre de matière noire, toutes cases confondues. */
    public int darkLevelsBought() {
        int levels = 0;
        for (idle.core.darkmatter.DarkUpgrade dark : darkUpgrades()) levels += state.darkLevelOf(dark.id());
        return levels;
    }

    /** Les branches de l'arbre de matière noire dont une case au moins est achetée, liaisons à part. */
    public int darkBranchesOpened() {
        EnumSet<idle.core.darkmatter.DarkUpgrade.Branch> opened = EnumSet.noneOf(idle.core.darkmatter.DarkUpgrade.Branch.class);
        for (idle.core.darkmatter.DarkUpgrade dark : darkUpgrades()) {
            if (!dark.isLink() && state.darkLevelOf(dark.id()) > 0) opened.add(dark.branch());
        }
        return opened.size();
    }

    /** Le record de reconstruction, en secondes ; 0 sans record. */
    public double rebuildRecord() { return state.rebuildRecord(); }

    /** Vrai si la recherche « Fusion prismatique » est faite. */
    public boolean hasPrismResearch() { return state.researches().contains("prism"); }

    public boolean canBuyPrismResearch() {
        return isSkinsUnlocked() && !hasPrismResearch() && state.particles().gte(PRISM_COST);
    }

    /** La recherche facultative en particules : elle donne le skin « Prisme » de la fusion. */
    public boolean buyPrismResearch() {
        if (!canBuyPrismResearch()) return false;
        state.setParticles(state.particles().subtract(PRISM_COST).max(BigNum.ZERO));
        state.stats().addParticlesSpent(PRISM_COST);
        state.addResearch("prism");
        unlockSkin(Skin.FUSION_PRISM);
        return true;
    }

    /**
     * Vrai une fois la recherche sur le noyau dense ouverte : son succès (version 0.174 : un million d'atomes créés et une
     * explosion dans un nouvel univers ; avant, le million suffisait, et elle s'ouvrait avec l'onglet Skins).
     */
    public boolean isDenseResearchUnlocked() { return isSkinsUnlocked() && universeStep(Achievements.UNIVERSE_DENSE); }

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
    public Trial activeTrial() {
        if (state.activeTrial() == null) return null;
        return Enums.parse(Trial.class, state.activeTrial());
    }

    /** Vrai pendant cette épreuve. */
    public boolean inTrial(Trial trial) { return trial.name().equals(state.activeTrial()); }

    /** Vrai si cette épreuve a déjà été réussie. */
    public boolean isTrialCompleted(Trial trial) { return state.completedTrials().contains(trial.name()); }

    /** Vrai si cette épreuve est ouverte : l'onglet Skins, et ce qu'elle demande. */
    public boolean isTrialUnlocked(Trial trial) {
        if (!isSkinsUnlocked()) return false;
        // Les épreuves s'ouvrent par les succès du deuxième univers (la première explosion, le
        // collisionneur rouvert, le premier Big Bang), puis selon ce qu'elles demandent dans la partie.
        return switch (trial) {
            case LONE_GENERATOR -> universeStep(Achievements.UNIVERSE_TRIALS) && state.explosions() > 0;
            case SOFT_CONTRACTION -> universeStep(Achievements.UNIVERSE_TRIALS) && isDarkMatterUnlocked();
            case COLD_BANG -> universeStep(Achievements.UNIVERSE_CYCLE) && state.bigBangs() > 0;
            case DETECTOR -> universeStep(Achievements.UNIVERSE_COLLIDER_TRIALS) && isColliderUnlocked()
                    && colliderUpgradeLevel(ColliderUpgrade.WINDOW) >= 3;
            case NO_OVERHEAT, FULL_CHARGE, LONE_COLLIDER -> universeStep(Achievements.UNIVERSE_COLLIDER_TRIALS) && isColliderUnlocked();
        };
    }

    /** Vrai si l'épreuve peut commencer : ouverte, aucune autre en cours, et pour le premier générateur, un seul générateur. */
    public boolean canStartTrial(Trial trial) {
        if (!isTrialUnlocked(trial) || activeTrial() != null) return false;
        return trial != Trial.LONE_GENERATOR || generatorCount() == 1;
    }

    /** Commence une épreuve. */
    public boolean startTrial(Trial trial) {
        if (!canStartTrial(trial)) return false;
        state.setActiveTrial(trial.name());
        state.setTrialStart(state.timePlayed());
        state.setTrialCount(0);
        state.setTrialBase(trial == Trial.FULL_CHARGE
                ? colliderEnergyProduced() + colliderEnergyPerSecond() * Trial.FULL_CHARGE_SECONDS : 0);
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
        Trial trial = activeTrial();
        if (trial == null) return 0;
        if (trial.seconds() <= 0) return Double.POSITIVE_INFINITY;
        return Math.max(0, trial.seconds() - (state.timePlayed() - state.trialStart()));
    }

    /** Où en est l'épreuve en cours, de 0 à 1. */
    public double trialProgress() {
        Trial trial = activeTrial();
        if (trial == null) return 0;
        return switch (trial) {
            case LONE_GENERATOR -> Math.min(1, Math.max(0, state.particles().log10() / Trial.LONE_GOAL_LOG10));
            case SOFT_CONTRACTION, COLD_BANG -> 0;
            case FULL_CHARGE -> state.trialBase() <= 0 ? 0 : Math.min(1, colliderEnergyProduced() / state.trialBase());
            default -> Math.min(1, state.trialCount() / (double) trial.goal());
        };
    }

    /** Ce que la contraction douce ajoute aux atomes de chaque fusion : +5 % par fusion du cycle, ×2 au plus. */
    public double contractionAtomsMultiplier() {
        if (!inTrial(Trial.SOFT_CONTRACTION)) return 1;
        return Math.min(Trial.CONTRACTION_MAX, 1 + Trial.CONTRACTION_FUSION_GAIN * state.trialCount());
    }

    void completeTrial() {
        Trial trial = activeTrial();
        if (trial == null) return;
        state.setActiveTrial(null);
        state.addCompletedTrial(trial.name());
        unlockSkin(trial.skin());
    }

    void failTrial() { state.setActiveTrial(null); }

    /** L'épreuve en cours : sa contrainte au fil du temps, son but, sa limite. */
    void runTrial(double dt) {
        Trial trial = activeTrial();
        if (trial == null) return;
        if (trial == Trial.SOFT_CONTRACTION && dt > 0) {
            state.setParticles(state.particles().multiply(Math.pow(1 - Trial.CONTRACTION_LOSS, dt / Trial.CONTRACTION_EVERY)));
        }
        if (trial == Trial.NO_OVERHEAT && state.overheatLeft() > 0) {
            failTrial();
            return;
        }
        boolean done = switch (trial) {
            case LONE_GENERATOR -> state.particles().log10() >= Trial.LONE_GOAL_LOG10;
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

    /**
     * Le guide de transition : ce que fera la prochaine remise à zéro de l'univers, avec les chiffres
     * de la partie en cours. Il suit {@link GameState#startUniverse()} : ce qu'elle garde, il le dit gardé, et
     * {@code TransitionTest} le vérifie.
     *
     * @param launching vrai pour « Lancer l'univers » ({@link #startNextUniverse()}), faux pour la formation de l'univers
     */
    public UniverseTransition universeTransition(boolean launching) {
        List<String> lost = new ArrayList<>();
        if (launching) {
            lost.add("Ce qui s'est joué dans les trois actes depuis la formation de l'univers : tout repart du premier générateur.");
        } else {
            lost.add("Particules, atomes, éléments, améliorations et automatismes.");
            // Les bulles de l'arbre en gardent une part : elle est dite gardée plus bas.
            if (!hasBubble(TreeBubble.MATURE_RIGHT)) {
                lost.add("Matière noire, son arbre et ses défis (" + state.completedChallenges().size() + " réussis).");
            }
            lost.add(state.bigBangs() + " Big Bang" + (state.bigBangs() > 1 ? "s" : "")
                    + (hasBubble(TreeBubble.GIANT_RIGHT_LOW) ? " et leurs paliers." : ", leurs paliers et leurs défis."));
            lost.add("Molécules, assemblages, astres (" + bodiesFormed() + " formés), systèmes solaires, galaxie et amas.");
            lost.add("L'espace et ses améliorations, les comètes et les anomalies du ciel.");
            lost.add(hasBubble(TreeBubble.MATURE_LEFT) ? "Les sondes."
                    : "Le collisionneur (énergie, générateurs, quarks, améliorations) et les sondes.");
            lost.add("Les plans suivis de l'arbre et des systèmes (leurs codes sauvegardés, eux, restent).");
            // Les hadrons : repris, sauf avec la bulle de l'arbre ancien, à gauche.
            if (state.hadronsOpen() && !hasBubble(TreeBubble.ANCIENT_LEFT)) {
                lost.add("Les hadrons (" + hadronCount() + " assemblés) et leur onglet, à rouvrir dans le collisionneur.");
            }
            // Le vide : sa réserve repart de zéro, ce qu'il en reste rejoint le cumul.
            if (state.voidOpen() && !hasBubble(TreeBubble.GIANT_LEFT_LOW)) {
                lost.add("Le vide, son trou noir et ses améliorations : les " + wholeText(state.voidEnergy())
                        + " d'énergie du vide qui restent s'ajoutent au cumul.");
            }
        }
        // Remis à zéro par les deux transitions, et pas rendus ensuite (GameState#startUniverse).
        lost.add("Les records des défis et des explosions (ceux des étapes restent).");
        if (state.universeFormed()) {
            lost.add("Ce qui s'ouvre en cours de partie : régime rapide, synergie, choix de cycle, famille préférée, "
                    + "éléments offerts.");
        }
        Trial trial = activeTrial();
        if (trial != null) lost.add("L'épreuve en cours, « " + trial.label() + " » : elle s'arrête sans être réussie.");
        List<String> kept = new ArrayList<>();
        kept.add("Les succès (" + achievementCount() + "), les records des étapes, le temps de jeu et les statistiques.");
        // La partie secondaire n'existe qu'une fois un univers formé : à la toute première formation, elle est à venir.
        boolean secondary = state.universeFormed();
        if (secondary) {
            kept.add("Les graines d'univers (" + wholeText(state.universeSeeds()) + ") et le cumul du vide ("
                    + wholeText(BigNum.of(state.voidAccumulated() + (!launching && state.voidOpen() ? state.voidEnergy().toDouble() : 0)))
                    + ").");
            // Le volume traverse les univers lui aussi. La durée traverse les univers elle aussi.
            kept.add(dimensionsUnlocked() >= 10 ? "Les dimensions : éclats de la ligne, du plan, du volume, de la durée, de la boucle, de la brane, du clicker, de la grille, du miroir et du mélange, et leurs améliorations."
                    : dimensionsUnlocked() >= 9 ? "Les dimensions : éclats de la ligne, du plan, du volume, de la durée, de la boucle, de la brane, du clicker, de la grille et du miroir, et leurs améliorations."
                    : dimensionsUnlocked() >= 8 ? "Les dimensions : éclats de la ligne, du plan, du volume, de la durée, de la boucle, de la brane, du clicker et de la grille, et leurs améliorations."
                    : dimensionsUnlocked() >= 7 ? "Les dimensions : éclats de la ligne, du plan, du volume, de la durée, de la boucle, de la brane et du clicker, et leurs améliorations."
                    : dimensionsUnlocked() >= 6 ? "Les dimensions : éclats de la ligne, du plan, du volume, de la durée, de la boucle et de la brane, et leurs améliorations."
                    : dimensionsUnlocked() >= 5 ? "Les dimensions : éclats de la ligne, du plan, du volume, de la durée et de la boucle, et leurs améliorations."
                    : isDurationOpen() ? "Les dimensions : éclats de la ligne, du plan, du volume et de la durée, et leurs améliorations."
                    : isVolumeOpen() ? "Les dimensions : éclats de la ligne, du plan et du volume, et leurs améliorations."
                    : "Les dimensions : éclats de la ligne et du plan, et leurs améliorations.");
            kept.add("La graine : palier " + seedStage() + ", et ses arrosages." + (launching && canOpenNextUniverse()
                    ? " L'univers d'après demandera le palier " + Math.min(SEED_STAGES, nextUniverseStage() + 1) + "."
                    : ""));
            kept.add("Les skins (" + state.unlockedSkins().size() + " débloqués) et ceux que vous avez choisis.");
            kept.add("Les améliorations d'univers, les épreuves réussies (" + state.completedTrials().size()
                    + ") et les recherches (" + state.researches().size() + ").");
        }
        kept.add("Vos réglages de jeu, les codes de plan sauvegardés et les fenêtres d'explication déjà vues.");
        // Les bulles de l'arbre qui font traverser les univers, comme GameState#startUniverse.
        // L'automatisation des dimensions traverse toujours les univers.
        if (!state.dimensionAutomations().isEmpty()) {
            kept.add("Les automatismes des dimensions (" + state.dimensionAutomations().size() + " achetés).");
        }
        if (hasBubble(TreeBubble.MATURE_LEFT)) kept.add("Le collisionneur, grâce à la bulle de l'arbre mûr, à gauche.");
        if (hasBubble(TreeBubble.GIANT_LEFT_LOW)) {
            kept.add("Le vide, son énergie (" + wholeText(state.voidEnergy()) + "), son trou noir et ses améliorations, grâce à la "
                    + "bulle du géant de gauche" + (launching ? "." : " ; ce qu'il a gagné depuis la dernière formation rejoint aussi le cumul."));
        }
        if (hasBubble(TreeBubble.MATURE_RIGHT)) {
            kept.add("La matière noire, son arbre, les explosions et les défis réussis (" + state.completedChallenges().size()
                    + "), grâce à la bulle de l'arbre mûr, à droite.");
        }
        if (hasBubble(TreeBubble.ANCIENT_LEFT) && (state.hadronsOpen() || hadronCount() > 0)) {
            kept.add("Les hadrons (" + hadronCount() + " assemblés), grâce à la bulle de l'arbre ancien, à gauche.");
        }
        if (hasBubble(TreeBubble.GIANT_RIGHT_LOW)) {
            kept.add("Les défis du Big Bang réussis (" + state.completedBangChallenges().size() + "), grâce à la bulle du géant de droite.");
        }
        // Ce que la formation a mis de côté attend les bulles qui gardent, jusqu'au lancement.
        if (launching && state.isHoldingForBubbles()) {
            kept.add("Mis de côté à la formation : une bulle qui garde (collisionneur, vide, matière noire, hadrons, défis du Big Bang), "
                    + "achetée avant de lancer, garde encore sa part de l'univers formé.");
        }
        List<String> gained = new ArrayList<>();
        if (launching) {
            gained.add("Une partie neuve, celle de l'univers suivant : les trois actes reviennent.");
        } else {
            gained.add("+" + SEEDS_PER_UNIVERSE + " graine d'univers.");
            if (!secondary) gained.add("L'onglet Univers s'ouvre : ce qui s'y gagne (graines, dimensions, skins) traverse "
                    + "ensuite chaque univers.");
            gained.add("Un univers de plus : la matière noire des explosions et sa croissance ×"
                    + (universes() + 2) + " pour la suite, au lieu de ×" + (universes() + 1) + ".");
        }
        return new UniverseTransition(launching, lost, kept, gained);
    }

    /** Les hadrons assemblés, toutes sortes, pour le guide. */
    long hadronCount() {
        long total = 0;
        for (Hadron hadron : Hadron.values()) total += state.hadrons(hadron);
        return total;
    }

    /**
     * L'univers se forme : tout ce qui y a mené est remis à zéro ({@link GameState#startUniverse()}), et le joueur
     * reçoit {@link #SEEDS_PER_UNIVERSE} graine d'univers. Les succès gagnés par la formation sont relevés avant.
     */
    void formUniverse() {
        checkAchievements();
        resetForUniverse();
        forgetDerived();
    }

    /** La remise à zéro de l'univers, son bilan relevé avant, et sa graine. */
    void resetForUniverse() {
        // Un univers de plus : celui-ci était déjà formé, la partie rejouée en forme un autre.
        boolean another = state.universeFormed();
        state.stats().noteUniverseTally(state.bigBangs(), moleculesCreated(), state.collisionRuns(), state.cometsCaught());
        // Le vide : ce qu'il en reste à la formation s'ajoute au cumul, gardé d'un univers à l'autre.
        if (state.voidOpen()) {
            // L'énergie que le vide gardé (bulle du géant de gauche, en bas) apportait d'un univers d'avant
            // est déjà dans le cumul ; seule celle gagnée depuis s'y ajoute. Elle était comptée à chaque formation.
            double stock = state.voidEnergy().toDouble();
            state.noteVoidHistory(String.valueOf(another ? 2 + state.extraUniverses() : 1), Math.max(0, stock - state.voidCarried()));
            state.setVoidCarried(hasBubble(TreeBubble.GIANT_LEFT_LOW) ? stock : 0);
        }
        // L'univers formé entre au journal des univers, avec ses étapes, avant leur remise à zéro.
        state.stats().archiveUniverse(another ? 2 + state.extraUniverses() : 1, state.timePlayed());
        // Ce que les bulles peuvent garder attend le lancement de l'univers suivant.
        state.startUniverseHolding();
        if (another) state.setExtraUniverses(state.extraUniverses() + 1);
        state.setUniverseSeeds(state.universeSeeds().add(BigNum.of(SEEDS_PER_UNIVERSE)));
    }
}

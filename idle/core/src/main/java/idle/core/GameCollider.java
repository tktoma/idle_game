package idle.core;

import idle.core.achievements.Achievement;
import idle.core.bigbang.BangAchievement;
import idle.core.collider.ColliderAchievement;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Collision;
import idle.core.collider.CollisionTier;
import idle.core.collider.Quark;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.sky.Body;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Le collisionneur de quarks.
 */
public abstract class GameCollider extends GameDarkTree {

    GameCollider(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    public abstract long moleculesCreated();
    public abstract int bodiesFormed();
    public abstract boolean hasGalaxy();
    public abstract idle.core.universe.Trial activeTrial();
    public abstract boolean inTrial(idle.core.universe.Trial trial);
    abstract void failTrial();


    // ------------------------------------------------------------------
    // Le collisionneur de quarks (version 0.050, refait en 0.059) : un jeu en parallèle, dont rien
    // ne se perd ni à l'explosion ni au Big Bang ; seule la remise à zéro le reprend
    // ------------------------------------------------------------------

    /** Période des faisceaux, en secondes : ils se croisent une fois par tour. */
    public static final double RESONANCE_PERIOD = 2;
    /** La part du tour pendant laquelle les faisceaux se croisent : le moment où un clic les capte. */
    public static final double RESONANCE_WINDOW = 0.16;
    /** Le quark top se désintègre une minute après son arrivée ; utilisé à temps, il donne dix minutes de surchauffe. */
    public static final double TOP_SECONDS = 60;
    public static final double OVERHEAT_SECONDS = 600;
    public static final double OVERHEAT_FACTOR = 10;
    /** Quarks beaux rendus quand le top se désintègre sans servir : rater le moment ne coûte rien. */
    public static final int TOP_CONSOLATION_BOTTOM = 3;
    /** Chance qu'un croisement donne une collision, sans les améliorations. */
    public static final double COLLISION_CHANCE = 0.15;
    /** Ce que chaque niveau de focalisation ajoute à cette chance. */
    public static final double LUCK_CHANCE = 0.03;
    /** Secondes de production que rapporte un croisement capté d'un clic. */
    public static final double CROSSING_SECONDS = 3;
    /** Au-delà de ce nombre de croisements d'un coup (une longue absence), le butin se compte en moyenne. */
    static final int CROSSINGS_ONE_BY_ONE = 400;
    /** Les collisions gardées pour le dessin des faisceaux. */
    private static final int RECENT_COLLISIONS = 24;

    /**
     * Ce qu'une collision a donné : une saveur et son nombre de quarks. {@code caught} : le croisement a été capté (d'un clic ou par le déclencheur), le butin
     * est doublé. {@code serial} : le numéro de la collision, pour que le dessin ne la montre qu'une fois.
     */
    public record CollisionResult(long serial, Quark quark, int count, boolean caught) {}

    final List<CollisionResult> finishedCollisions = new ArrayList<>();
    private final java.util.ArrayDeque<CollisionResult> recentCollisions = new java.util.ArrayDeque<>();

    /** Le collisionneur s'ouvre à la première explosion et ne se referme plus. */
    public boolean isColliderUnlocked() {
        return state.started() && (isDarkMatterUnlocked() || state.bigBangs() > 0 || state.collisionRuns() > 0
                || state.colliderGeneratorsBought(ColliderGenerator.BEAM) > 0);
    }

    /**
     * Énergie gagnée par seconde : les faisceaux croisés, un d'office et ceux qu'on achète ou que
     * produit l'aimant ({@link ColliderGenerator}), multipliés par la production d'énergie et les
     * succès du collisionneur. Le collisionneur est un jeu à part : il ne dépend pas de la production
     * de particules.
     */
    public double colliderEnergyPerSecond() {
        if (!isColliderUnlocked()) return 0;
        return (1 + state.colliderGenerators(ColliderGenerator.BEAM)) * ColliderGenerator.BEAM.power() * colliderEnergyMultiplier()
                * colliderOutputMultiplier(ColliderGenerator.BEAM);
    }

    /**
     * Ce par quoi un générateur multiplie sa production : son succès de production (×2), le succès
     * des 250 générateurs (×1,5), et pour ceux qui produisent un autre générateur, les générateurs
     * renforcés (×2 par niveau).
     */
    public double colliderOutputMultiplier(ColliderGenerator generator) {
        double multiplier = hasColliderAchievement(ColliderAchievement.of(generator, ColliderAchievement.Kind.OUTPUT)) ? 2 : 1;
        if (hasColliderAchievement(ColliderAchievement.QUARTER_THOUSAND)) multiplier *= 1.5;
        if (generator != ColliderGenerator.BEAM) multiplier *= Math.pow(2, colliderUpgradeLevel(ColliderUpgrade.GENERATOR_POWER));
        return multiplier;
    }

    // Les succès du Big Bang (version 0.061).

    /** Vrai une fois le premier Big Bang passé : les succès du Big Bang se montrent. */
    public boolean isBangAchievementsUnlocked() {
        return state.bigBangs() > 0;
    }

    /** Où en est ce succès du Big Bang : ce qui est réuni, borné à ce qu'il demande. */
    public int bangAchievementProgress(BangAchievement achievement) {
        long have = switch (achievement.stat()) {
            case BIG_BANGS -> state.bigBangs();
            case MOLECULES -> moleculesCreated();
            case BODIES -> bodiesFormed();
            case GALAXY -> hasGalaxy() ? 1 : 0;
            case SYSTEMS -> solarSystemsComplete();
            case PROBES -> state.probeRuns();
            case COMETS -> state.cometsCaught();
        };
        return (int) Math.min(achievement.threshold(), Math.max(0, have));
    }

    public boolean hasBangAchievement(BangAchievement achievement) {
        return bangAchievementProgress(achievement) >= achievement.threshold();
    }

    public int bangAchievements() {
        int count = 0;
        for (BangAchievement achievement : BangAchievement.values()) if (hasBangAchievement(achievement)) count++;
        return count;
    }

    /** Ce par quoi les succès du Big Bang gagnés multiplient cette grandeur : 1 + la somme de leurs bonus. */
    public double bangAchievementMultiplier(BangAchievement.Reward reward) {
        double sum = 0;
        for (BangAchievement achievement : BangAchievement.values()) {
            if (achievement.reward() == reward && hasBangAchievement(achievement)) sum += achievement.amount();
        }
        return 1 + sum;
    }

    // Les succès du collisionneur (version 0.060).

    /** Générateurs achetés en tout, toutes sortes confondues. */
    public int colliderGeneratorsBoughtTotal() {
        int total = 0;
        for (ColliderGenerator generator : ColliderGenerator.values()) total += state.colliderGeneratorsBought(generator);
        return total;
    }

    /** Où en est ce succès : ce qui est réuni (borné à ce qu'il demande). */
    public int colliderAchievementProgress(ColliderAchievement achievement) {
        long have;
        if (achievement.generator() != null) have = state.colliderGeneratorsBought(achievement.generator());
        else if (achievement.kind() == ColliderAchievement.Kind.CHANCE) have = state.collisionRuns();
        else have = colliderGeneratorsBoughtTotal();
        return (int) Math.min(achievement.threshold(), have);
    }

    /** Vrai si ce succès du collisionneur est gagné : il se déduit des achats et des collisions. */
    public boolean hasColliderAchievement(ColliderAchievement achievement) {
        return colliderAchievementProgress(achievement) >= achievement.threshold();
    }

    /** Nombre de succès du collisionneur gagnés. */
    public int colliderAchievements() {
        int count = 0;
        for (ColliderAchievement achievement : ColliderAchievement.values()) if (hasColliderAchievement(achievement)) count++;
        return count;
    }

    /** Ce par quoi les améliorations multiplient l'énergie. */
    public double colliderEnergyMultiplier() {
        double multiplier = Math.pow(2, colliderUpgradeLevel(ColliderUpgrade.ENERGY))
                * (hasColliderAchievement(ColliderAchievement.HUNDRED) ? 1.5 : 1);
        return multiplier;
    }

    /** Ce que produit un générateur du collisionneur par seconde : des exemplaires du précédent, ou de l'énergie pour le premier. */
    public double colliderGeneratorOutput(ColliderGenerator generator) {
        // L'épreuve « Un seul générateur » (version 0.088) : la chaîne s'arrête, seul le premier produit.
        if (generator.previous() != null && inTrial(idle.core.universe.Trial.LONE_COLLIDER)) return 0;
        return state.colliderGenerators(generator) * generator.power() * colliderOutputMultiplier(generator);
    }

    /** Prix du prochain exemplaire de ce générateur. */
    public double colliderGeneratorCost(ColliderGenerator generator) {
        double cost = generator.cost(state.colliderGeneratorsBought(generator));
        return hasColliderAchievement(ColliderAchievement.of(generator, ColliderAchievement.Kind.COST)) ? cost / 2 : cost;
    }

    /** Un générateur s'offre dès que le précédent existe : le premier tout de suite. */
    public boolean isColliderGeneratorOpen(ColliderGenerator generator) {
        ColliderGenerator previous = generator.previous();
        return isColliderUnlocked() && (previous == null || state.colliderGeneratorsBought(previous) > 0);
    }

    public boolean canBuyColliderGenerator(ColliderGenerator generator) {
        return isColliderGeneratorOpen(generator) && state.colliderEnergy() >= colliderGeneratorCost(generator) - 1e-9;
    }

    /** Achète un exemplaire de ce générateur avec l'énergie. */
    public boolean buyColliderGenerator(ColliderGenerator generator) {
        if (!canBuyColliderGenerator(generator)) return false;
        state.setColliderEnergy(state.colliderEnergy() - colliderGeneratorCost(generator));
        state.setColliderGeneratorsBought(generator, state.colliderGeneratorsBought(generator) + 1);
        state.setColliderGenerators(generator, state.colliderGenerators(generator) + 1);
        return true;
    }

    /** Achète autant d'exemplaires de ce générateur que l'énergie le permet. Rend le nombre achetés. */
    public int buyMaxColliderGenerator(ColliderGenerator generator) {
        int bought = 0;
        while (bought < 10_000 && buyColliderGenerator(generator)) bought++;
        return bought;
    }

    public double colliderEnergy() {
        return state.colliderEnergy();
    }

    /** Ajoute de l'énergie, et la compte dans l'énergie produite depuis le début. */
    private void addColliderEnergy(double gain) {
        if (!(gain > 0)) return;
        state.setColliderEnergy(state.colliderEnergy() + gain);
        state.setColliderEnergyProduced(state.colliderEnergyProduced() + gain);
    }

    /** L'énergie produite depuis le début : ce qui ouvre les deux trous noirs (version 0.062). */
    public double colliderEnergyProduced() {
        return state.colliderEnergyProduced();
    }

    // Les croisements et leur hasard.

    /** Secondes entre deux croisements : {@link #RESONANCE_PERIOD}, moins avec les faisceaux rapides. */
    public double colliderPeriod() {
        return RESONANCE_PERIOD / (1 + 0.2 * colliderUpgradeLevel(ColliderUpgrade.SPEED));
    }

    /** La part du tour où les faisceaux se croisent : {@link #RESONANCE_WINDOW}, plus avec le détecteur élargi. */
    public double colliderWindow() {
        return RESONANCE_WINDOW * (1 + 0.25 * colliderUpgradeLevel(ColliderUpgrade.WINDOW));
    }

    /** La phase des faisceaux, de 0 à 1 : ils se croisent dans le détecteur à 0,5. */
    public double colliderBeamPhase() {
        return state.colliderPhase();
    }

    /** Vrai si la phase tombe dans le moment du croisement, de la largeur de départ. */
    public static boolean inResonance(double phase) {
        return Math.abs(phase - 0.5) <= RESONANCE_WINDOW / 2;
    }

    /** Vrai pendant que les faisceaux se croisent, détecteur élargi compris. */
    public boolean isCrossing() {
        return Math.abs(state.colliderPhase() - 0.5) <= colliderWindow() / 2;
    }

    /** Le numéro du croisement en cours : celui qui arrive avant le milieu du tour, celui qui vient de passer après. */
    private long currentCrossing() {
        return state.colliderCrossings() + (state.colliderPhase() < 0.5 ? 1 : 0);
    }

    /** Vrai si le croisement en cours n'a pas encore été capté. */
    public boolean canCatchCrossing() {
        return isColliderUnlocked() && isCrossing() && state.colliderCaught() != currentCrossing();
    }

    /**
     * Un clic quand les faisceaux se croisent : {@link #CROSSING_SECONDS} secondes de production
     * d'énergie d'un coup, et une collision assurée au butin doublé. Un seul par croisement ; à
     * côté, rien.
     *
     * @return l'énergie gagnée, 0 si le clic tombe à côté
     */
    public double catchCrossing() {
        if (!canCatchCrossing()) return 0;
        long crossing = currentCrossing();
        state.setColliderCaught(crossing);
        catchStreak++;
        // Les épreuves du collisionneur (version 0.088) comptent les croisements captés à la main.
        if (activeTrial() != null && activeTrial().isCollider()) state.setTrialCount(state.trialCount() + 1);
        return caught(crossing);
    }

    /** Croisements captés à la main de suite, sans en laisser passer un : rien ne le garde, il repart de zéro au lancement. */
    private int catchStreak;

    /** Croisements captés à la main de suite, depuis le dernier manqué (ou le lancement du jeu). */
    public int catchStreak() {
        return catchStreak;
    }

    /** Ce que donne un croisement capté : l'énergie, et la collision assurée. */
    private double caught(long crossing) {
        double gain = colliderEnergyPerSecond() * CROSSING_SECONDS;
        addColliderEnergy(gain);
        if (isCollisionsUnlocked()) collide(crossing, true);
        return gain;
    }

    /** Vrai une fois l'amélioration « Collisions » prise : les croisements ont leur chance de collision. */
    public boolean isCollisionsUnlocked() {
        return isColliderUnlocked() && colliderUpgradeLevel(ColliderUpgrade.COLLISIONS) > 0;
    }

    /**
     * Chance qu'un croisement donne une collision : aucune avant l'amélioration « Collisions » ;
     * ensuite 15 %, plus la focalisation et le succès des mille collisions.
     */
    public double collisionChance() {
        if (!isCollisionsUnlocked()) return 0;
        double chance = COLLISION_CHANCE
                + LUCK_CHANCE * colliderUpgradeLevel(ColliderUpgrade.LUCK)
                + (hasColliderAchievement(ColliderAchievement.THOUSAND_COLLISIONS) ? 0.02 : 0);
        return Math.min(1, chance);
    }

    /** Vrai si cette énergie est ouverte : son amélioration est prise. */
    public boolean isCollisionTierOpen(CollisionTier tier) {
        return isColliderUnlocked() && colliderUpgradeLevel(tier.upgrade()) > 0;
    }

    /** L'énergie la plus haute ouverte. */
    public CollisionTier colliderTier() {
        CollisionTier top = CollisionTier.LOW;
        for (CollisionTier tier : CollisionTier.values()) if (isCollisionTierOpen(tier)) top = tier;
        return top;
    }

    /** Vrai si un croisement peut donner cette saveur. */
    public boolean isQuarkOpen(Quark quark) {
        return isCollisionTierOpen(CollisionTier.of(quark));
    }

    /** Le poids d'une saveur dans le tirage, 0 si elle n'est pas ouverte : les lourdes sont rares. */
    public double quarkWeight(Quark quark) {
        if (!isQuarkOpen(quark)) return 0;
        double heavy = Math.pow(1.4, colliderUpgradeLevel(ColliderUpgrade.HEAVY));
        return switch (quark) {
            case UP, DOWN -> 45;
            case STRANGE -> 14 * heavy;
            case CHARM -> 6 * heavy;
            case BOTTOM -> 2 * heavy;
            case TOP -> 0.15 * heavy;
        };
    }

    /** La part des collisions qui donne cette saveur, de 0 à 1. */
    public double quarkShare(Quark quark) {
        double total = 0;
        for (Quark each : Quark.values()) total += quarkWeight(each);
        return total <= 0 ? 0 : quarkWeight(quark) / total;
    }

    /** Un nombre de 0 à 1, toujours le même pour un croisement et un tirage donnés : le hasard du collisionneur ne touche pas à celui de la synthèse. */
    private static double roll(long crossing, int draw) {
        double mix = Math.sin(crossing * 12.9898 + draw * 78.233) * 43758.5453;
        return mix - Math.floor(mix);
    }

    /** Un croisement : la chance d'une collision, ou une collision assurée s'il est capté. */
    private void collide(long crossing, boolean caught) {
        if (!caught && roll(crossing, 1) >= collisionChance()) return;
        Quark quark = drawQuark(roll(crossing, caught ? 7 : 2));
        int count = caught ? 2 : 1;
        give(quark, count, caught);
    }

    /** Tire une saveur parmi celles qui sont ouvertes, selon leur poids. */
    private Quark drawQuark(double unit) {
        double total = 0;
        for (Quark quark : Quark.values()) total += quarkWeight(quark);
        double at = unit * total;
        for (Quark quark : Quark.values()) {
            at -= quarkWeight(quark);
            if (at < 0) return quark;
        }
        return Quark.UP;
    }

    /** Ajoute le butin d'une collision, et la retient pour les annonces et le dessin. */
    private void give(Quark quark, int count, boolean caught) {
        long serial = state.collisionRuns() + 1;
        state.setCollisionRuns(serial);
        if (quark == Quark.TOP) {
            state.setTopLeft(Math.max(state.topLeft(), topSeconds()));
        } else {
            state.setQuarks(quark, state.quarks(quark) + count);
        }
        CollisionResult result = new CollisionResult(serial, quark, quark == Quark.TOP ? 1 : count, caught);
        if (finishedCollisions.size() < 200) finishedCollisions.add(result);
        recentCollisions.addLast(result);
        while (recentCollisions.size() > RECENT_COLLISIONS) recentCollisions.removeFirst();
    }

    /** Les dernières collisions, la plus récente au bout : pour le dessin des faisceaux. */
    public List<CollisionResult> recentCollisions() {
        return List.copyOf(recentCollisions);
    }

    /** Les collisions depuis le dernier appel : pour les annoncer une fois. */
    public List<CollisionResult> takeFinishedCollisions() {
        List<CollisionResult> taken = List.copyOf(finishedCollisions);
        finishedCollisions.clear();
        return taken;
    }

    /** Tous les combien de croisements le déclencheur automatique capte : 5, 3 puis 1 ; 0 sans lui. */
    public int autoCatchEvery() {
        return switch (colliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH)) {
            case 0 -> 0;
            case 1 -> 5;
            case 2 -> 3;
            default -> 1;
        };
    }

    /**
     * Les croisements d'un pas de jeu. Un à un d'habitude ; au retour d'une longue absence, au-delà
     * de {@link #CROSSINGS_ONE_BY_ONE}, le butin se compte en moyenne pour ne pas ralentir le jeu.
     */
    private void runCrossings(long count) {
        if (count <= 0) return;
        long first = state.colliderCrossings() + 1;
        state.setColliderCrossings(state.colliderCrossings() + count);
        int every = autoCatchEvery();
        if (count <= CROSSINGS_ONE_BY_ONE) {
            for (long crossing = first; crossing < first + count; crossing++) {
                if (state.colliderCaught() == crossing) continue;      // déjà capté d'un clic, juste avant le milieu
                catchStreak = 0;
                if (inTrial(idle.core.universe.Trial.DETECTOR)) failTrial();      // un croisement laissé passer
                if (every > 0 && crossing % every == 0) {
                    state.setColliderCaught(crossing);
                    caught(crossing);
                } else {
                    collide(crossing, false);
                }
            }
            return;
        }
        // En moyenne : les captés du déclencheur, puis la chance sur les autres.
        catchStreak = 0;
        if (inTrial(idle.core.universe.Trial.DETECTOR)) failTrial();
        long autoCaught = every > 0 ? count / every : 0;
        double free = count - autoCaught;
        addColliderEnergy(colliderEnergyPerSecond() * CROSSING_SECONDS * autoCaught);
        if (!isCollisionsUnlocked()) return;
        double collisions = free * collisionChance() + autoCaught;
        double quarks = free * collisionChance() + 2.0 * autoCaught;
        for (Quark quark : Quark.values()) {
            double share = quarkShare(quark);
            if (share <= 0) continue;
            if (quark == Quark.TOP) {
                if (collisions * share >= 1 || roll(first, 11) < collisions * share) state.setTopLeft(Math.max(state.topLeft(), topSeconds()));
                continue;
            }
            double expected = quarks * share;
            int whole = (int) Math.min(Integer.MAX_VALUE / 4, Math.floor(expected) + (roll(first, 20 + quark.ordinal()) < expected - Math.floor(expected) ? 1 : 0));
            state.setQuarks(quark, (int) Math.min(Integer.MAX_VALUE / 2, (long) state.quarks(quark) + whole));
        }
        state.setCollisionRuns(state.collisionRuns() + (long) collisions);
    }

    // Les améliorations du collisionneur.

    /** Vrai quand les huit générateurs sont là : les améliorations du collisionneur s'ouvrent. */
    public boolean isColliderUpgradesUnlocked() {
        return isColliderUnlocked() && state.colliderGeneratorsBought(ColliderGenerator.PLANCK) > 0
                || !colliderUpgradesBought().isEmpty();
    }

    private List<ColliderUpgrade> colliderUpgradesBought() {
        List<ColliderUpgrade> bought = new ArrayList<>();
        for (ColliderUpgrade upgrade : ColliderUpgrade.values()) if (state.colliderUpgradeLevel(upgrade) > 0) bought.add(upgrade);
        return bought;
    }

    public int colliderUpgradeLevel(ColliderUpgrade upgrade) {
        return state.colliderUpgradeLevel(upgrade);
    }

    /** Vrai si cette amélioration s'offre : la première dès l'ouverture, les autres quand celle qui les ouvre a le niveau voulu. */
    public boolean isColliderUpgradeOpen(ColliderUpgrade upgrade) {
        if (!isColliderUpgradesUnlocked()) return false;
        // La Superposition attend aussi le cinquième Big Bang (version 0.073) : jusque-là, les choix de l'arbre comptent.
        if (upgrade == ColliderUpgrade.SUPERPOSITION && state.bigBangs() < SUPERPOSITION_BIG_BANGS) return false;
        return upgrade.requires() == null || state.colliderUpgradeLevel(upgrade.requires()) >= upgrade.requiredLevel();
    }

    /** Big Bangs qu'il faut avoir déclenchés pour que la Superposition s'offre : cinq, le milieu du troisième acte. */
    public static final int SUPERPOSITION_BIG_BANGS = 5;

    public double colliderUpgradeCost(ColliderUpgrade upgrade) {
        return upgrade.cost(state.colliderUpgradeLevel(upgrade));
    }

    /** Les quarks que demande le prochain niveau de cette amélioration (la chaîne, version 0.062) ; 0 pour les autres. */
    public int colliderUpgradeQuarks(ColliderUpgrade upgrade) {
        return upgrade.quarkCost(state.colliderUpgradeLevel(upgrade));
    }

    public boolean canBuyColliderUpgrade(ColliderUpgrade upgrade) {
        return isColliderUpgradeOpen(upgrade) && state.colliderUpgradeLevel(upgrade) < upgrade.maxLevel()
                && state.colliderEnergy() >= colliderUpgradeCost(upgrade) - 1e-9
                && (upgrade.quark() == null || state.quarks(upgrade.quark()) >= colliderUpgradeQuarks(upgrade));
    }

    /** Prend un niveau de cette amélioration, payé en énergie, et en quarks pour la chaîne. */
    public boolean buyColliderUpgrade(ColliderUpgrade upgrade) {
        if (!canBuyColliderUpgrade(upgrade)) return false;
        state.setColliderEnergy(state.colliderEnergy() - colliderUpgradeCost(upgrade));
        if (upgrade.quark() != null) state.setQuarks(upgrade.quark(), state.quarks(upgrade.quark()) - colliderUpgradeQuarks(upgrade));
        state.setColliderUpgradeLevel(upgrade, state.colliderUpgradeLevel(upgrade) + 1);
        return true;
    }

    /** Vrai si cet automate est acheté et que le joueur le laisse faire. */
    public boolean isColliderAutomationRunning(ColliderUpgrade upgrade) {
        return state.colliderUpgradeLevel(upgrade) > 0 && !state.isColliderPaused(upgrade);
    }

    /** Met en pause un automate, ou le relance. */
    public void toggleColliderAutomation(ColliderUpgrade upgrade) {
        if (state.colliderUpgradeLevel(upgrade) <= 0) return;
        state.setColliderPaused(upgrade, !state.isColliderPaused(upgrade));
    }

    /** Les automates : du générateur le plus haut au plus bas, chacun achète tout ce qu'il peut. */
    private void runColliderAutomations() {
        ColliderGenerator[] generators = ColliderGenerator.values();
        for (int k = generators.length - 1; k >= 0; k--) {
            ColliderUpgrade automation = ColliderUpgrade.automationOf(generators[k]);
            if (isColliderAutomationRunning(automation)) buyMaxColliderGenerator(generators[k]);
        }
    }

    /** L'énergie produite qu'il faut au trou noir (version 0.062), puis au trou noir supermassif, le dernier avant la galaxie. */
    public static final double BLACK_HOLE_ENERGY = 1e12;
    public static final double SUPERMASSIVE_ENERGY = 1e15;

    /**
     * L'énergie du collisionneur que demande cet astre : produite depuis le début, pour les deux trous
     * noirs ; 0 pour les autres. Le collisionneur fait ainsi partie du chemin vers la galaxie.
     */
    public double colliderEnergyNeeded(Body body) {
        return switch (body.tier()) {
            case BLACK_HOLE -> BLACK_HOLE_ENERGY;
            case CORE -> SUPERMASSIVE_ENERGY;
            default -> 0;
        };
    }

    /** Vrai si cet astre demande de l'énergie du collisionneur. */
    public boolean needsColliderEnergy(Body body) {
        return colliderEnergyNeeded(body) > 0;
    }

    /** Vrai si l'énergie produite suffit à cet astre. */
    public boolean hasColliderEnergyFor(Body body) {
        return state.colliderEnergyProduced() >= colliderEnergyNeeded(body) - 1e-6;
    }

    /** Nombre de conditions de cet astre : celles du catalogue, plus l'énergie du collisionneur s'il en demande. */
    public int bodyConditions(String bodyId) {
        Body body = body(bodyId);
        return body.conditions() + (needsColliderEnergy(body) ? 1 : 0);
    }

    /** Quarks en réserve, toutes saveurs ensemble. */
    public long totalQuarks() {
        long total = 0;
        for (Quark quark : Quark.values()) total += state.quarks(quark);
        return total;
    }

        public int quarks(Quark quark) {
        return state.quarks(quark);
    }

    /** Vrai tant qu'un quark top attend d'être utilisé. */
    public boolean hasTop() {
        return state.topLeft() > 0;
    }

    public double topLeft() {
        return state.topLeft();
    }

    /** Combien de temps le quark top attend : une minute, deux fois plus par niveau de top stable. */
    public double topSeconds() {
        return TOP_SECONDS * Math.pow(2, colliderUpgradeLevel(ColliderUpgrade.TOP_LIFE));
    }

    /** Durée de la surchauffe d'un quark top : dix minutes, 50 % de plus par niveau de top stable. */
    public double overheatSeconds() {
        return OVERHEAT_SECONDS * (1 + 0.5 * colliderUpgradeLevel(ColliderUpgrade.TOP_LIFE));
    }

    /** Utilise le quark top : la surchauffe, toutes les particules ×10. */
    public boolean useTop() {
        if (!hasTop()) return false;
        state.setTopLeft(0);
        state.setOverheatLeft(state.overheatLeft() + overheatSeconds());
        return true;
    }

    public double overheatLeft() {
        return state.overheatLeft();
    }

    /** Ce par quoi le collisionneur multiplie les particules : la chaîne, et la surchauffe d'un quark top. */
    public double colliderParticlesMultiplier() {
        return (state.overheatLeft() > 0 ? OVERHEAT_FACTOR : 1) * Math.pow(1.5, colliderUpgradeLevel(ColliderUpgrade.PARTICLES));
    }

    /** Ce par quoi la chaîne multiplie les atomes de chaque fusion. */
    public double colliderAtomsMultiplier() {
        return 1 + 0.1 * colliderUpgradeLevel(ColliderUpgrade.ATOMS);
    }

    /** Ce par quoi la chaîne multiplie la croissance de la matière noire. */
    public double colliderDarkGrowthMultiplier() {
        return 1 + 0.2 * colliderUpgradeLevel(ColliderUpgrade.DARK_MATTER);
    }

    /** Ce par quoi la chaîne multiplie l'espace par seconde. */
    public double colliderSpaceMultiplier() {
        return 1 + 0.25 * colliderUpgradeLevel(ColliderUpgrade.SPACE);
    }

    void runCollider(double dt) {
        if (dt <= 0 || !isColliderUnlocked()) return;
        finishOldCollisions();
        // La chaîne des générateurs : chacun produit le précédent, le premier l'énergie, au début de ce pas.
        double[] output = new double[ColliderGenerator.values().length];
        for (ColliderGenerator generator : ColliderGenerator.values()) output[generator.ordinal()] = colliderGeneratorOutput(generator);
        addColliderEnergy(colliderEnergyPerSecond() * dt);
        for (ColliderGenerator generator : ColliderGenerator.values()) {
            ColliderGenerator previous = generator.previous();
            if (previous != null && output[generator.ordinal()] > 0) {
                state.setColliderGenerators(previous, state.colliderGenerators(previous) + output[generator.ordinal()] * dt);
            }
        }
        // Les faisceaux avancent ; chaque passage au milieu du tour est un croisement.
        double before = state.colliderPhase();
        double after = before + dt / colliderPeriod();
        long crossings = (long) (Math.floor(after - 0.5) - Math.floor(before - 0.5));
        state.setColliderPhase(after);
        runCrossings(crossings);
        // La surchauffe s'écoule avec la production qu'elle multiplie : voir Game#runTimedBonuses (version 0.126).
        if (state.topLeft() > 0) {
            state.setTopLeft(state.topLeft() - dt);
            if (state.topLeft() <= 0) state.setQuarks(Quark.BOTTOM, state.quarks(Quark.BOTTOM) + TOP_CONSOLATION_BOTTOM);
        }
        runColliderAutomations();
    }

    /** Les collisions d'une sauvegarde d'avant la version 0.059 se terminent d'un coup, avec leur butin d'alors. */
    private void finishOldCollisions() {
        if (state.collisions().isEmpty()) return;
        for (Collision collision : state.collisions()) {
            int factor = collision.doubled() ? 2 : 1;
            switch (collision.tier()) {
                case LOW -> {
                    addQuarks(Quark.UP, 3 * factor);
                    addQuarks(Quark.DOWN, 3 * factor);
                }
                case MEDIUM -> {
                    addQuarks(Quark.STRANGE, factor);
                    addQuarks(Quark.UP, factor);
                    addQuarks(Quark.DOWN, factor);
                }
                case HIGH -> {
                    addQuarks(Quark.CHARM, factor);
                    addQuarks(Quark.STRANGE, factor);
                }
                case VERY_HIGH -> {
                    addQuarks(Quark.BOTTOM, factor);
                    addQuarks(Quark.CHARM, factor);
                }
                case EXTREME -> {
                    addQuarks(Quark.BOTTOM, 2 * factor);
                    state.setTopLeft(Math.max(state.topLeft(), topSeconds()));
                }
            }
        }
        state.setCollisions(List.of());
    }

    private void addQuarks(Quark quark, int count) {
        state.setQuarks(quark, state.quarks(quark) + count);
    }

    /** Un entier de {@code low} à {@code high}, toujours le même pour un tirage donné. */
    static int between(long run, int draw, int low, int high) {
        double mix = Math.sin(run * 12.9898 + draw * 78.233) * 43758.5453;
        double unit = mix - Math.floor(mix);
        return low + (int) Math.floor(unit * (high - low + 1));
    }
}

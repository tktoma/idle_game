package idle.core;

import idle.core.achievements.Achievement;
import idle.core.bigbang.BangAchievement;
import idle.core.bigbang.BangChallenge;
import idle.core.collider.ColliderAchievement;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.elements.ElementEffect;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.universe.CycleChoice;
import idle.core.universe.Trial;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * D'où vient chaque grandeur (version 0.123) : ses facteurs, un par ligne, chacun en ordres de grandeur, et
 * rangés par aspect du jeu. La somme des ordres de grandeur d'une grandeur redonne la grandeur elle-même :
 * {@code Σ log10(facteur) = log10(grandeur)}. C'est ce que montre l'onglet Statistiques, comme le détail des
 * multiplicateurs d'Antimatter Dimensions, et ce qu'un robot regarde pour savoir ce qui pèse le plus.
 *
 * <p>Rien n'est calculé ici que {@link Game} n'expose déjà : les facteurs sont lus un par un, dans l'ordre des
 * formules du jeu. Un facteur qui vaut 1 n'est pas listé.
 */
public final class Multipliers {

    private Multipliers() {}

    /** Les grandeurs décomposées. */
    public enum Quantity {
        PRODUCTION("Particules par seconde"),
        ATOMS("Atomes par fusion"),
        DARK_MATTER("Matière noire par explosion"),
        DARK_GROWTH("Élan de la matière noire"),
        SPACE("Espace par seconde"),
        /** Version 0.144 : les trois autres grandeurs qui se gagnent par seconde. */
        COLLIDER_ENERGY("Énergie du collisionneur par seconde"),
        VOID_ENERGY("Énergie du vide par seconde"),
        LINE_SHARDS("Segments de la ligne par seconde");

        private final String label;

        Quantity(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Les aspects du jeu auxquels revient chaque facteur. */
    public enum Aspect {
        BASE("Base", "#8fa3b8"),
        UPGRADES("Améliorations", "#9fd0ff"),
        MILESTONES("Paliers de vitesse", "#6fb6ff"),
        GENERATORS("Générateurs", "#7fe0d4"),
        PROGRESS("Avancement de la partie", "#b9c7d6"),
        ELEMENTS("Éléments", "#f0a8c0"),
        ACHIEVEMENTS("Succès", "#ffe27a"),
        DARK_MATTER("Matière noire", "#c9a8ff"),
        COLLIDER("Collisionneur", "#6fe3ff"),
        MATTER("Matière et ciel", "#9be7a8"),
        BIG_BANG("Big Bang", "#ff9f6e"),
        UNIVERSE("Univers", "#ffd27f"),
        BRAKE("Freins", "#ff6b6b");

        private final String label;
        private final String color;

        Aspect(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String label() {
            return label;
        }

        /** Sa couleur dans l'onglet Statistiques. */
        public String color() {
            return color;
        }
    }

    /** Un facteur : l'aspect auquel il revient, son nom, et ce qu'il vaut en ordres de grandeur (négatif : il divise). */
    public record Factor(Aspect aspect, String label, double log10) {
        /** Ce par quoi il multiplie, quand cela s'écrit encore en double ; l'infini au-delà. */
        public double value() {
            return log10 > 300 ? Double.POSITIVE_INFINITY : Math.pow(10, log10);
        }
    }

    /** Un aspect et ses facteurs, avec ce qu'il pèse en tout. */
    public record Share(Aspect aspect, double log10, List<Factor> factors) {}

    /** Vrai quand la grandeur existe dans la partie : avant, il n'y a rien à décomposer. */
    public static boolean isAvailable(Game game, Quantity quantity) {
        return switch (quantity) {
            case PRODUCTION -> game.isStarted() && game.generatorCount() > 0;
            case ATOMS -> game.isStarted() && game.stats().fusions() > 0;
            case DARK_MATTER, DARK_GROWTH -> game.isDarkMatterUnlocked() && game.darkMatterEarned().sign() > 0;
            case SPACE -> game.isBigBangUnlocked();
            case COLLIDER_ENERGY -> game.isColliderUnlocked();
            case VOID_ENERGY -> game.isVoidUnlocked() && game.voidGainPerSecond() > 0;
            case LINE_SHARDS -> game.isLineOpen();
        };
    }

    /** La grandeur elle-même, en ordres de grandeur : ce que redonne la somme des facteurs. */
    public static double total(Game game, Quantity quantity) {
        return switch (quantity) {
            case PRODUCTION -> game.productionPerSecond().log10();
            case ATOMS -> game.atomsPerFusion().log10();
            case DARK_MATTER -> game.darkMatterPerExplosion().log10();
            case DARK_GROWTH -> Math.log10(game.darkMatterMomentum());
            case SPACE -> game.spacePerSecond().log10();
            case COLLIDER_ENERGY -> log(game.colliderEnergyPerSecond());
            case VOID_ENERGY -> log(game.voidGainPerSecond());
            case LINE_SHARDS -> log(game.lineShardsPerSecond());
        };
    }

    /** Les facteurs d'une grandeur, dans l'ordre de sa formule ; ceux qui valent 1 n'y sont pas. */
    public static List<Factor> of(Game game, Quantity quantity) {
        List<Factor> factors = new ArrayList<>();
        if (!isAvailable(game, quantity)) return factors;
        switch (quantity) {
            case PRODUCTION -> production(game, factors);
            case ATOMS -> atoms(game, factors);
            case DARK_MATTER -> darkMatter(game, factors);
            case DARK_GROWTH -> darkGrowth(game, factors);
            case SPACE -> space(game, factors);
            case COLLIDER_ENERGY -> colliderEnergy(game, factors);
            case VOID_ENERGY -> voidEnergy(game, factors);
            case LINE_SHARDS -> lineShards(game, factors);
        }
        return factors;
    }

    /**
     * Les facteurs rangés par aspect, du plus lourd au plus léger ; les freins (ce qui divise) à la fin. Dans
     * chaque aspect, ses facteurs du plus lourd au plus léger.
     */
    public static List<Share> byAspect(List<Factor> factors) {
        Map<Aspect, List<Factor>> grouped = new EnumMap<>(Aspect.class);
        for (Factor factor : factors) grouped.computeIfAbsent(factor.aspect(), each -> new ArrayList<>()).add(factor);
        List<Share> shares = new ArrayList<>();
        for (Map.Entry<Aspect, List<Factor>> entry : grouped.entrySet()) {
            List<Factor> list = entry.getValue();
            list.sort((a, b) -> Double.compare(b.log10(), a.log10()));
            double sum = 0;
            for (Factor factor : list) sum += factor.log10();
            shares.add(new Share(entry.getKey(), sum, List.copyOf(list)));
        }
        shares.sort((a, b) -> {
            boolean aBrake = a.log10() < 0, bBrake = b.log10() < 0;
            if (aBrake != bBrake) return aBrake ? 1 : -1;
            return Double.compare(Math.abs(b.log10()), Math.abs(a.log10()));
        });
        return shares;
    }

    /** Ce qu'un aspect pèse parmi tout ce qui multiplie, de 0 à 1 : sa part des ordres de grandeur gagnés. Les freins n'en ont pas. */
    public static double shareOf(Share share, List<Share> all) {
        if (share.log10() <= 0) return 0;
        double gained = 0;
        for (Share each : all) if (each.log10() > 0) gained += each.log10();
        return gained > 0 ? share.log10() / gained : 0;
    }

    // ------------------------------------------------------------------
    // Les grandeurs, facteur par facteur
    // ------------------------------------------------------------------

    private static void add(List<Factor> factors, Aspect aspect, String label, double log10) {
        if (Double.isNaN(log10) || Math.abs(log10) < 1e-9) return;
        factors.add(new Factor(aspect, label, log10));
    }

    private static void add(List<Factor> factors, Aspect aspect, String label, BigNum value) {
        if (value.sign() > 0) add(factors, aspect, label, value.log10());
    }

    private static String exponent(double power) {
        return String.format(java.util.Locale.FRANCE, "%.3f", power);
    }

    private static double log(double value) {
        return value > 0 ? Math.log10(value) : 0;
    }

    /** Ce que la matière apporte à une grandeur, en ses cinq parts : {@link Game#moleculeBoost(Molecule.Stat)}. */
    private static void matter(Game game, List<Factor> factors, Molecule.Stat stat) {
        add(factors, Aspect.MATTER, "Molécules, astres et cosmos", log(game.matterBoost(stat)));
        add(factors, Aspect.MATTER, "Collections de rayon", log(game.collectionBoost(stat)));
        add(factors, Aspect.BIG_BANG, "Améliorations d'espace", log(game.spaceUpgradeBoost(stat)));
        add(factors, Aspect.BIG_BANG, "Paliers de Big Bang", log(game.bigBangMilestoneBoost(stat)));
        add(factors, Aspect.UNIVERSE, "Skins", log(game.skinBoost()));
    }

    private static void production(Game game, List<Factor> factors) {
        // La vitesse : la base, les niveaux de vitesse (achetés et offerts), les éléments, l'élan.
        add(factors, Aspect.BASE, "Vitesse de départ", Game.BASE_SPEED);
        double extra = game.speedExtraPerLevel();
        for (Upgrade upgrade : game.upgrades()) {
            if (!(upgrade.effect() instanceof Effect.MultiplySpeed speed)) continue;
            double perLevel = log(speed.perLevel() + extra);
            int bought = game.levelOf(upgrade.id());
            int offered = game.startingSpeedLevels();
            add(factors, Aspect.UPGRADES, upgrade.name() + " : " + bought + " niveaux", bought * perLevel);
            add(factors, Aspect.DARK_MATTER, upgrade.name() + " : " + offered + " niveaux offerts", offered * perLevel);
        }
        add(factors, Aspect.ELEMENTS, "Éléments : vitesse", log(game.elementMultiplier(ElementEffect.Stat.SPEED)));
        add(factors, Aspect.BRAKE, "Premier élan", -log(game.firstRunSlowdown()));
        add(factors, Aspect.UNIVERSE, "Régime rapide", log(game.regimeMultiplier()));
        add(factors, Aspect.UNIVERSE, "Synergie des rangs", log(game.synergyMultiplier()));
        // Les particules de chaque création.
        add(factors, Aspect.ELEMENTS, "Éléments : particules", log(game.elementMultiplier(ElementEffect.Stat.PARTICLES)));
        add(factors, Aspect.DARK_MATTER, "Arbre de matière noire", game.darkParticlesMultiplier());
        add(factors, Aspect.DARK_MATTER, "Paliers de taille", game.landmarkParticlesMultiplier());
        add(factors, Aspect.ACHIEVEMENTS, "Succès", game.achievementParticlesMultiplier());
        matter(game, factors, Molecule.Stat.PARTICLES);
        add(factors, Aspect.MILESTONES, "Paliers de vitesse (" + game.speedMilestones() + ")", game.speedMilestoneMultiplier());
        add(factors, Aspect.COLLIDER, "Collisionneur", log(game.colliderParticlesMultiplier()));
        add(factors, Aspect.ACHIEVEMENTS, "Succès du Big Bang", log(game.bangAchievementMultiplier(BangAchievement.Reward.PARTICLES)));
        add(factors, Aspect.UNIVERSE, "Cycle parfait", log(game.perfectCycleMultiplier()));
        add(factors, Aspect.BIG_BANG, "Vent de particules", log(game.windMultiplier()));
        add(factors, Aspect.UNIVERSE, "Défis de Big Bang", log(game.universeBangFactor(Game.BANG_UNIVERSE_PARTICLES)));
        for (Upgrade upgrade : game.upgrades()) {
            int level = game.levelOf(upgrade.id());
            if (level <= 0) continue;
            add(factors, Aspect.UPGRADES, upgrade.name() + (upgrade.hasLimit() && upgrade.maxLevel() == 1 ? "" : " : " + level + " niveaux"),
                    game.particlesMultiplier(upgrade.id(), level));
        }
        // Les puissances des grands systèmes (version 0.124) : chacune élève la base à sa part.
        double base = game.powerBase();
        add(factors, Aspect.ELEMENTS, "Puissance du tableau périodique (+" + exponent(game.tablePower()) + ")", game.tablePower() * base);
        add(factors, Aspect.MATTER, "Puissance du ciel (+" + exponent(game.skyPower()) + ")", game.skyPower() * base);
        add(factors, Aspect.COLLIDER, "Puissance du collisionneur (+" + exponent(game.colliderPower()) + ")", game.colliderPower() * base);
        add(factors, Aspect.ACHIEVEMENTS, "Puissance des succès (+" + exponent(game.achievementPower()) + ")", game.achievementPower() * base);
        // Les générateurs : leur nombre, puis ce que les métaux de transition donnent à chacun.
        int generators = game.generatorCount();
        double individual = 0;
        for (int generator = 0; generator < generators; generator++) {
            individual = Math.min(1e300, individual + game.generatorSpeedMultiplier(generator) * game.generatorParticlesMultiplier(generator));
        }
        add(factors, Aspect.GENERATORS, generators + " générateurs", log(generators));
        add(factors, Aspect.ELEMENTS, "Éléments : générateurs un à un", log(individual) - log(generators));
    }

    private static void atoms(Game game, List<Factor> factors) {
        add(factors, Aspect.BASE, "Atome de base", 0);
        add(factors, Aspect.DARK_MATTER, "Arbre : atomes en plus", log(1 + game.darkAtomsPerFusion()));
        add(factors, Aspect.ELEMENTS, "Éléments : atomes", log(game.elementMultiplier(ElementEffect.Stat.ATOMS)));
        add(factors, Aspect.DARK_MATTER, "Améliorations par matière noire", log(game.darkAtomsMultiplier()));
        add(factors, Aspect.DARK_MATTER, "Paliers de taille", log(game.landmarkAtomsMultiplier()));
        add(factors, Aspect.ACHIEVEMENTS, "Succès", log(1 + game.achievementBonus(Achievement.Bonus.ATOMS)));
        matter(game, factors, Molecule.Stat.ATOMS);
        add(factors, Aspect.COLLIDER, "Collisionneur", log(game.colliderAtomsMultiplier()));
        add(factors, Aspect.ACHIEVEMENTS, "Succès du Big Bang", log(game.bangAchievementMultiplier(BangAchievement.Reward.ATOMS)));
        add(factors, Aspect.UPGRADES, "Rendement", log(game.fusionYield()));
        add(factors, Aspect.GENERATORS, game.fusionGroups() + " groupes de générateurs", log(game.fusionGroups() * game.fusionGroupBonus()));
        add(factors, Aspect.UNIVERSE, "Choix de cycle : fusion", log(game.cycleChoice() == CycleChoice.FUSION ? 1 + CycleChoice.FUSION_GAIN : 1));
        add(factors, Aspect.UNIVERSE, "Contraction douce", log(game.contractionAtomsMultiplier()));
    }

    private static void darkMatter(Game game, List<Factor> factors) {
        double first = game.state().bigBangs() == 0 && game.firstRunPace > 1 ? Game.FIRST_CYCLE_DARK_MATTER : 1;
        double milestones = game.bigBangDarkMatterFactor();
        double challenges = game.bangReward(BangChallenge.Reward.DARK_MATTER);
        double reserve = game.cycleChoice() == CycleChoice.RESERVE ? 1 + CycleChoice.RESERVE_GAIN : 1;
        double universes = game.universeDarkMatterFactor();
        double base = game.darkMatterPerExplosion().log10() - log(first * milestones * challenges * reserve * universes);
        add(factors, Aspect.DARK_MATTER, "Base et arbre", base);
        add(factors, Aspect.BRAKE, "Premier cycle", log(first));
        add(factors, Aspect.BIG_BANG, "Paliers de Big Bang", log(milestones));
        add(factors, Aspect.BIG_BANG, "Défis de Big Bang", log(challenges));
        add(factors, Aspect.UNIVERSE, "Choix de cycle : réserve", log(reserve));
        add(factors, Aspect.UNIVERSE, "Univers formés", log(universes));
    }

    private static void darkGrowth(Game game, List<Factor> factors) {
        add(factors, Aspect.BASE, "Élan de base", log(Game.DARK_MATTER_GROWTH_PER_UNIT));
        add(factors, Aspect.DARK_MATTER, "Matière noire gagnée", log(game.darkMatterUnits()));
        add(factors, Aspect.PROGRESS, "Production et atomes de la partie", log(game.darkMatterProgressFactor()));
        add(factors, Aspect.DARK_MATTER, "Arbre : expansion", Math.min(100, game.darkExpansionMultiplier().log10()));
        add(factors, Aspect.DARK_MATTER, "Paliers de taille", log(game.landmarkExpansionMultiplier()));
        add(factors, Aspect.ACHIEVEMENTS, "Succès", log(1 + game.achievementBonus(Achievement.Bonus.EXPANSION)));
        matter(game, factors, Molecule.Stat.DARK_GROWTH);
        add(factors, Aspect.COLLIDER, "Collisionneur", log(game.colliderDarkGrowthMultiplier()));
        add(factors, Aspect.ACHIEVEMENTS, "Succès du Big Bang", log(game.bangAchievementMultiplier(BangAchievement.Reward.DARK_GROWTH)));
        add(factors, Aspect.UNIVERSE, "Univers formés", log(game.universeDarkMatterFactor()));
    }

    private static void space(Game game, List<Factor> factors) {
        add(factors, Aspect.BIG_BANG, game.bigBangs() + " Big Bangs", log(Game.SPACE_PER_SECOND * game.bigBangs()));
        matter(game, factors, Molecule.Stat.SPACE);
        add(factors, Aspect.BIG_BANG, "Énergie sombre", log(game.darkMatterSpaceBoost()));
        add(factors, Aspect.BRAKE, "Big Bang à froid", log(game.inTrial(Trial.COLD_BANG) ? Trial.COLD_EXPANSION : 1));
        add(factors, Aspect.BIG_BANG, "Défis de Big Bang", log(game.bangReward(BangChallenge.Reward.SPACE)));
        add(factors, Aspect.COLLIDER, "Collisionneur", log(game.colliderSpaceMultiplier()));
        add(factors, Aspect.BIG_BANG, "Sursaut des sondes", log(game.probeBoostMultiplier()));
        add(factors, Aspect.ACHIEVEMENTS, "Succès du Big Bang", log(game.bangAchievementMultiplier(BangAchievement.Reward.SPACE)));
        add(factors, Aspect.BIG_BANG, "Sillage de comète", log(game.cometBoost()));
    }

    /** L'énergie du collisionneur (version 0.144) : {@link Game#colliderEnergyPerSecond()}, facteur par facteur. */
    private static void colliderEnergy(Game game, List<Factor> factors) {
        ColliderGenerator beam = ColliderGenerator.BEAM;
        add(factors, Aspect.BASE, "Un faisceau croisé", log(beam.power()));
        double beams = 1 + game.state().colliderGenerators(beam);
        add(factors, Aspect.GENERATORS, Math.round(beams) + " faisceaux croisés", log(beams));
        int energy = game.colliderUpgradeLevel(ColliderUpgrade.ENERGY);
        add(factors, Aspect.UPGRADES, ColliderUpgrade.ENERGY.label() + " : " + energy + " niveaux", energy * log(2));
        add(factors, Aspect.ACHIEVEMENTS, "Succès « " + ColliderAchievement.HUNDRED.title() + " »",
                log(game.hasColliderAchievement(ColliderAchievement.HUNDRED) ? 1.5 : 1));
        ColliderAchievement output = ColliderAchievement.of(beam, ColliderAchievement.Kind.OUTPUT);
        add(factors, Aspect.ACHIEVEMENTS, "Succès « " + output.title() + " »", log(game.hasColliderAchievement(output) ? 2 : 1));
        add(factors, Aspect.ACHIEVEMENTS, "Succès « " + ColliderAchievement.QUARTER_THOUSAND.title() + " »",
                log(game.hasColliderAchievement(ColliderAchievement.QUARTER_THOUSAND) ? 1.5 : 1));
    }

    /**
     * L'énergie du vide (version 0.144) : {@link Game#voidGainPerSecond()}. Le rythme des paires est le rythme de base
     * borné par la part d'énergie du collisionneur et la part d'espace libre que le joueur laisse au vide ; ce que la
     * borne retire est un frein, nommé d'après ce qui borne ({@link Game#voidLimit()}).
     */
    private static void voidEnergy(Game game, List<Factor> factors) {
        add(factors, Aspect.BASE, "Une paire par seconde", log(Game.VOID_BASE_RATE));
        int rate = game.voidLevel(Game.VoidUpgrade.RATE);
        add(factors, Aspect.UPGRADES, "Rythme des paires : " + rate + " niveaux", rate * log(Game.VOID_RATE_STEP));
        add(factors, Aspect.BRAKE, "Part du collisionneur et de l'espace laissée au vide (" + game.voidLimit() + ")",
                log(game.voidRate()) - log(game.voidBaseRate()));
        int gain = game.voidLevel(Game.VoidUpgrade.GAIN);
        add(factors, Aspect.UPGRADES, "Gain par paire : " + gain + " niveaux", log(game.voidPairGain()));
    }

    /** Les segments de la ligne (version 0.144) : {@link Game#lineShardsPerSecond()}, facteur par facteur. */
    private static void lineShards(Game game, List<Factor> factors) {
        add(factors, Aspect.BASE, "Un dixième d'éclat par trait, un aller en " + String.format(java.util.Locale.FRANCE, "%.1f", Game.LINE_HALF_TRIP) + " s", log(Game.LINE_SHARD / Game.LINE_HALF_TRIP));
        add(factors, Aspect.UPGRADES, game.lineTicks() - 1 + " traits touchés par aller", log(game.lineTicks() - 1));
        add(factors, Aspect.UPGRADES, "Éclats par trait : " + game.state().lineYieldLevel() + " niveaux", log(1 + game.state().lineYieldLevel()));
        add(factors, Aspect.UPGRADES, game.lineBalls() + " boules par ligne", log(game.lineBalls()));
        add(factors, Aspect.UPGRADES, game.lineCount() + " lignes", log(game.lineCount()));
        add(factors, Aspect.UPGRADES, "Vitesse des boules : " + game.state().lineSpeedLevel() + " niveaux", log(game.lineSpeed()));
    }
}

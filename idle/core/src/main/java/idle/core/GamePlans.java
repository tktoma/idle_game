package idle.core;

import idle.core.achievements.Achievement;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.DarkEffect;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.core.sky.Body;
import idle.core.sky.SolarSystem;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Les plans de l'arbre de matière noire et des systèmes solaires.
 */
public abstract class GamePlans extends GameCollider {

    GamePlans(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    abstract boolean ruled(Challenge.Rule rule);
    public abstract BigNum productionPerSecond();


    // ------------------------------------------------------------------
    // Les plans de l'arbre et des systèmes solaires (version 0.084)
    // ------------------------------------------------------------------

    /** Le début d'un code de l'arbre. */
    public static final String TREE_CODE_PREFIX = "arbre:";
    /** Le début d'un code des systèmes solaires. */
    public static final String SYSTEMS_CODE_PREFIX = "systemes:";
    /** Intervalle, en secondes de jeu, entre deux passages des plans. */
    public static final double PLAN_CHECK_SECONDS = 1;

    private double planTimer;

    /** Ce qu'un code a donné : les identifiants reconnus, et ceux que le jeu ne connaît pas (écartés). */
    public record PlanLoad(int known, int unknown) {
        /** Vrai si le code ne contenait rien d'utilisable. */
        public boolean empty() {
            return known == 0;
        }
    }

    /** L'état d'une case du plan de l'arbre. */
    public enum PlanStep {
        /** Prise. */
        TAKEN,
        /** Pas encore payable, ou sa case du dessus pas encore prise : elle sera prise dès que possible. */
        WAITING,
        /** L'autre voie de sa bifurcation est prise : elle attend que les bifurcations se rouvrent. */
        BLOCKED
    }

    /**
     * Le code des voies et des liaisons prises dans l'arbre : « arbre: » puis leurs identifiants,
     * dans l'ordre de l'arbre. Il se recolle avec {@link #loadDarkPlan(String)}.
     */
    public String darkTreeCode() {
        List<String> ids = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if ((dark.isFork() || dark.isLink()) && state.darkLevelOf(dark.id()) > 0) ids.add(dark.id());
        }
        return TREE_CODE_PREFIX + String.join(",", ids);
    }

    /**
     * Fait d'un code de l'arbre le plan suivi : ses voies et ses liaisons se prennent dès qu'elles
     * sont payables, maintenant et après chaque Big Bang, jusqu'à ce que le joueur l'oublie. Un code
     * vide ou illisible ne change rien.
     */
    public PlanLoad loadDarkPlan(String code) {
        if (code == null) return new PlanLoad(0, 0);
        String text = code.trim();
        if (text.startsWith(TREE_CODE_PREFIX)) text = text.substring(TREE_CODE_PREFIX.length());
        java.util.Set<String> wanted = new java.util.LinkedHashSet<>();
        int unknown = 0;
        for (String id : text.split("[,;\\s]+")) {
            if (id.isBlank()) continue;
            DarkUpgrade dark = darkUpgrades.get(id);
            if (dark == null || !(dark.isFork() || dark.isLink())) unknown++;
            else wanted.add(id);
        }
        if (wanted.isEmpty()) return new PlanLoad(0, unknown);
        // Dans l'ordre de l'arbre : une liaison vient après les voies qu'elle relie.
        List<String> ordered = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) if (wanted.contains(dark.id())) ordered.add(dark.id());
        state.setDarkPlan(ordered);
        applyDarkPlan();
        return new PlanLoad(ordered.size(), unknown);
    }

    /** Les cases du plan de l'arbre, dans l'ordre ; vide sans plan. */
    public List<DarkUpgrade> darkPlan() {
        List<DarkUpgrade> plan = new ArrayList<>();
        for (String id : state.darkPlan()) {
            DarkUpgrade dark = darkUpgrades.get(id);
            if (dark != null) plan.add(dark);
        }
        return plan;
    }

    /** Où en est une case du plan de l'arbre. */
    public PlanStep darkPlanStep(DarkUpgrade dark) {
        if (state.darkLevelOf(dark.id()) > 0) return PlanStep.TAKEN;
        return darkForkRival(dark.id()) != null ? PlanStep.BLOCKED : PlanStep.WAITING;
    }

    /** Oublie le plan de l'arbre : rien de pris n'est rendu. */
    public void forgetDarkPlan() {
        state.setDarkPlan(List.of());
    }

    /** Garde le code de l'arbre actuel dans le premier emplacement. */
    public void saveDarkPlan() {
        saveDarkPlan(0);
    }

    /** Garde le code de l'arbre actuel dans cet emplacement (version 0.086), pour le recharger plus tard. */
    public void saveDarkPlan(int slot) {
        state.setSavedDarkPlan(slot, darkTreeCode());
    }

    /** Recharge le code du premier emplacement. */
    public PlanLoad loadSavedDarkPlan() {
        return loadSavedDarkPlan(0);
    }

    /** Recharge le code gardé dans cet emplacement. */
    public PlanLoad loadSavedDarkPlan(int slot) {
        return loadDarkPlan(state.savedDarkPlan(slot));
    }

    /** Ce que contient un code de l'arbre : {voies, liaisons et accords}, d'après le catalogue. */
    public int[] describeTreeCode(String code) {
        int[] counts = new int[2];
        if (code == null) return counts;
        String text = code.trim();
        if (text.startsWith(TREE_CODE_PREFIX)) text = text.substring(TREE_CODE_PREFIX.length());
        for (String id : text.split("[,;\\s]+")) {
            DarkUpgrade dark = darkUpgrades.get(id);
            if (dark == null) continue;
            if (dark.isFork()) counts[0]++;
            else if (dark.isLink()) counts[1]++;
        }
        return counts;
    }

    /** Ce que contient un code des systèmes : {systèmes, astres placés autour des étoiles}. */
    public int[] describeSystemsCode(String code) {
        int[] counts = new int[2];
        if (code == null) return counts;
        String text = code.trim();
        if (text.startsWith(SYSTEMS_CODE_PREFIX)) text = text.substring(SYSTEMS_CODE_PREFIX.length());
        for (String line : text.split("[;\\n]+")) {
            SolarSystem system = SolarSystem.parse(line);
            if (system == null) continue;
            counts[0]++;
            counts[1] += system.planets().size() + system.moons().size();
        }
        return counts;
    }

    /** Prend tout ce que le plan de l'arbre permet maintenant ; renvoie le nombre de cases prises. */
    public int applyDarkPlan() {
        int taken = 0;
        for (String id : state.darkPlan()) {
            if (!darkUpgrades.containsKey(id) || state.darkLevelOf(id) > 0) continue;
            if (buyDark(id)) taken++;
        }
        return taken;
    }

    /** Vrai si « Rouvrir les bifurcations à la prochaine explosion » a un sens : une voie prise, sans la Superposition. */
    public boolean canArmForkReset() {
        if (!isDarkMatterUnlocked() || hasSuperposition()) return false;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.isFork() && state.darkLevelOf(dark.id()) > 0) return true;
        }
        return false;
    }

    /** Arme ou désarme la réouverture des bifurcations à la prochaine explosion. */
    public void setForkResetArmed(boolean armed) {
        state.setForkResetArmed(armed && (canArmForkReset() || !armed));
    }

    /**
     * Le code de la disposition des systèmes solaires : « systemes: » puis un système par morceau,
     * séparés par « ; » ; chacun « étoile|planètes|lunes », les planètes de la plus proche à la plus lointaine.
     */
    public String systemsCode() {
        List<String> lines = new ArrayList<>();
        for (SolarSystem system : state.solarSystems()) lines.add(system.line());
        return SYSTEMS_CODE_PREFIX + String.join(";", lines);
    }

    /**
     * Fait d'un code des systèmes le plan suivi : chaque système se fonde, et chaque astre s'y place
     * à son orbite, dès qu'il est mûr et libre. Un astre déjà dans un autre système y reste. Les
     * identifiants inconnus, ou d'une autre échelle (une planète à la place d'une étoile), sont écartés.
     */
    public PlanLoad loadSystemPlan(String code) {
        if (code == null) return new PlanLoad(0, 0);
        String text = code.trim();
        if (text.startsWith(SYSTEMS_CODE_PREFIX)) text = text.substring(SYSTEMS_CODE_PREFIX.length());
        List<SolarSystem> plan = new ArrayList<>();
        java.util.Set<String> used = new java.util.HashSet<>();
        int known = 0, unknown = 0;
        for (String line : text.split("[;\\n]+")) {
            if (line.isBlank()) continue;
            SolarSystem read = SolarSystem.parse(line);
            if (read == null || !isTier(read.star(), Body.Tier.STAR) || !used.add(read.star())) {
                unknown++;
                continue;
            }
            known++;
            List<String> planets = new ArrayList<>(), moons = new ArrayList<>();
            for (String id : read.planets()) {
                if (isTier(id, Body.Tier.PLANET) && used.add(id)) {
                    planets.add(id);
                    known++;
                } else unknown++;
            }
            for (String id : read.moons()) {
                if (isTier(id, Body.Tier.MOON) && used.add(id) && moons.size() < planets.size()) {
                    moons.add(id);
                    known++;
                } else unknown++;
            }
            plan.add(new SolarSystem(read.star(), planets, moons));
        }
        if (plan.isEmpty()) return new PlanLoad(0, unknown);
        state.setSystemPlan(plan);
        applySystemPlan();
        return new PlanLoad(known, unknown);
    }

    private boolean isTier(String bodyId, Body.Tier tier) {
        Body body = bodies.get(bodyId);
        return body != null && body.tier() == tier;
    }

    /** Le plan des systèmes solaires ; vide sans plan. */
    public List<SolarSystem> systemPlan() {
        return state.systemPlan();
    }

    /** Astres du plan des systèmes déjà à leur place, et astres qu'il compte en tout (étoiles comprises). */
    public int[] systemPlanProgress() {
        int placed = 0, total = 0;
        for (SolarSystem plan : state.systemPlan()) {
            int index = systemIndexOf(plan.star());
            SolarSystem now = index < 0 ? null : state.solarSystems().get(index);
            total += 1 + plan.planets().size() + plan.moons().size();
            if (now == null) continue;
            placed++;
            for (String id : plan.planets()) if (now.planets().contains(id)) placed++;
            for (String id : plan.moons()) if (now.moons().contains(id)) placed++;
        }
        return new int[] {placed, total};
    }

    /** Oublie le plan des systèmes : rien de placé ne bouge. */
    public void forgetSystemPlan() {
        state.setSystemPlan(List.of());
    }

    /** Garde la disposition actuelle des systèmes dans le premier emplacement. */
    public void saveSystemPlan() {
        saveSystemPlan(0);
    }

    /** Garde la disposition actuelle des systèmes dans cet emplacement (version 0.086). */
    public void saveSystemPlan(int slot) {
        state.setSavedSystemPlan(slot, systemsCode());
    }

    /** Recharge la disposition du premier emplacement. */
    public PlanLoad loadSavedSystemPlan() {
        return loadSavedSystemPlan(0);
    }

    /** Recharge la disposition gardée dans cet emplacement. */
    public PlanLoad loadSavedSystemPlan(int slot) {
        return loadSystemPlan(state.savedSystemPlan(slot));
    }

    /** La place du système de cette étoile, ou −1. */
    public int systemIndexOf(String starId) {
        List<SolarSystem> systems = state.solarSystems();
        for (int i = 0; i < systems.size(); i++) if (systems.get(i).star().equals(starId)) return i;
        return -1;
    }

    /** Place tout ce que le plan des systèmes permet maintenant ; renvoie le nombre d'astres placés. */
    public int applySystemPlan() {
        if (state.systemPlan().isEmpty() || !isSolarSystemsUnlocked()) return 0;
        int placed = 0;
        for (SolarSystem plan : state.systemPlan()) {
            int index = systemIndexOf(plan.star());
            if (index < 0) {
                if (!foundSystem(plan.star())) continue;
                index = systemIndexOf(plan.star());
                placed++;
            }
            for (String planet : plan.planets()) {
                if (!state.solarSystems().get(index).planets().contains(planet) && addPlanet(index, planet)) placed++;
            }
            // Les planètes du plan à leurs orbites, dans son ordre ; celles que le joueur a ajoutées en plus, après.
            SolarSystem now = state.solarSystems().get(index);
            List<String> ordered = new ArrayList<>();
            for (String planet : plan.planets()) if (now.planets().contains(planet)) ordered.add(planet);
            for (String planet : now.planets()) if (!ordered.contains(planet)) ordered.add(planet);
            if (!ordered.equals(now.planets())) replaceSystem(index, now.withPlanets(ordered));
            for (String moon : plan.moons()) {
                if (!state.solarSystems().get(index).moons().contains(moon) && addMoon(index, moon)) placed++;
            }
        }
        return placed;
    }

    /** Les plans, une fois par seconde de jeu : ce qu'ils permettent se prend, joueur présent ou absent. */
    void runPlans(double dt) {
        if (state.darkPlan().isEmpty() && state.systemPlan().isEmpty()) return;
        planTimer += dt;
        if (planTimer < PLAN_CHECK_SECONDS) return;
        planTimer = 0;
        applyDarkPlan();
        applySystemPlan();
    }

    /** Les voies d'une bifurcation, dans l'ordre du catalogue. */
    public List<DarkUpgrade> darkForkOptions(String fork) {
        List<DarkUpgrade> options = new ArrayList<>();
        for (DarkUpgrade dark : darkUpgrades.values()) if (fork.equals(dark.fork())) options.add(dark);
        return options;
    }

    /** Prix du prochain niveau, dans l'unité de sa branche (particules, atomes, ou mètres à atteindre). */
    public BigNum darkCostOf(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return dark.costAt(state.darkLevelOf(dark.id()));
    }

    /**
     * Ce que le joueur a dans l'unité d'une branche : particules, atomes disponibles, taille de la
     * matière noire en mètres, ou matière noire qui reste à dépenser. Pour les particules, c'est
     * ce qu'il possède ou ce qu'il produit en une seconde, le plus grand des deux.
     */
    public BigNum darkBalance(DarkUpgrade.Branch branch) {
        return switch (branch) {
            // Une seconde de production vaut des particules en main : sans cela, une fusion automatique
            // rapide, qui remet les particules à zéro vingt fois par seconde, rendrait la branche inachetable.
            case PARTICLES -> state.particles().max(productionPerSecond());
            case ATOMS -> state.atoms();
            case SIZE -> state.darkMatterSize();
            case DARK_MATTER -> state.darkMatter();
        };
    }

    public boolean canBuyDark(String darkUpgradeId) {
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        return isDarkAvailable(darkUpgradeId) && !isDarkMaxed(darkUpgradeId)
                && darkBalance(dark.branch()).gte(darkCostOf(darkUpgradeId));
    }

    /**
     * Achète un niveau d'une amélioration de matière noire. Les particules, les atomes et la
     * matière noire sont dépensés ; une taille à atteindre ne coûte rien, la matière noire ne
     * rétrécit pas.
     *
     * @return {@code true} si l'achat a eu lieu
     */
    public boolean buyDark(String darkUpgradeId) {
        if (!canBuyDark(darkUpgradeId)) return false;
        DarkUpgrade dark = darkUpgrade(darkUpgradeId);
        BigNum cost = darkCostOf(darkUpgradeId);
        switch (dark.branch()) {
            case PARTICLES -> {
                state.setParticles(state.particles().subtract(cost).max(BigNum.ZERO));
                state.stats().addParticlesSpent(cost);
            }
            case ATOMS -> {
                state.setAtoms(state.atoms().subtract(cost).max(BigNum.ZERO));
                state.stats().addAtomsSpent(cost);
            }
            case SIZE -> { /* un seuil, pas un prix */ }
            case DARK_MATTER -> {
                // Dépensée, mais toujours comptée comme gagnée : rien de ce qu'elle ouvre ne se referme.
                state.setDarkMatter(state.darkMatter().subtract(cost).max(BigNum.ZERO));
                state.setDarkMatterSpent(state.darkMatterSpent().add(cost));
            }
        }
        state.setDarkLevel(dark.id(), state.darkLevelOf(dark.id()) + 1);
        applyHeadStart();
        return true;
    }

    /**
     * Le niveau qui agit (version 0.070) : celui que possède le joueur, éteint par certains défis
     * (« Présence », « Absence », « Branche coupée », « Tronc nu ») et doublé par certaines récompenses.
     * Tous les effets de l'arbre passent par lui ; les prix et l'accès suivent le niveau possédé.
     */
    int effectLevel(DarkUpgrade dark) {
        int level = state.darkLevelOf(dark.id());
        if (level == 0) return 0;
        Style style = darkStyle(dark);
        // « Présence » et « Absence » (corsés en 0.073) : seules agissent les voies et les accords du style, tout le
        // reste de l'arbre est éteint. Avec les bonnes voies, le défi reste court ; sans, c'est presque un premier acte.
        if (ruled(Challenge.Rule.PRESENCE_ONLY) && dark.branch() != DarkUpgrade.Branch.DARK_MATTER | dark.isLink()
                && style != Style.PRESENCE) return 0;
        if (ruled(Challenge.Rule.ABSENCE_ONLY) && dark.branch() != DarkUpgrade.Branch.DARK_MATTER | dark.isLink()
                && style != Style.ABSENCE) return 0;
        if (ruled(Challenge.Rule.CUT_BRANCH) && dark.branch() == DarkUpgrade.Branch.PARTICLES) return 0;
        if (ruled(Challenge.Rule.BARE_TREE) && (dark.isFork() || dark.isLink())) return 0;
        if (style == Style.PRESENCE && rewarded(Challenge.Reward.STRONGER_PRESENCE)) level *= 2;
        if (style == Style.ABSENCE && rewarded(Challenge.Reward.STRONGER_ABSENCE)) level *= 2;
        if (dark.isLink() && rewarded(Challenge.Reward.STRONGER_LINKS)) level *= 2;
        return level;
    }

    /**
     * Les deux styles de l'arbre (version 0.070). Dans chaque bifurcation, la voie de gauche est celle
     * de la présence : la plus forte tant que le joueur est là. Celle de droite est celle de l'absence :
     * un peu moins forte, mais chacune rend {@link #ABSENCE_VOIE_BONUS} de plus du temps passé jeu
     * fermé ({@link #offlineRate()}). Un accord a le style de ses voies.
     */
    public enum Style { PRESENCE, ABSENCE, NONE }

    /** Points de temps d'absence rejoué en plus que rend chaque voie d'absence prise : cinq. */
    public static final double ABSENCE_VOIE_BONUS = 0.05;

    /** Le style d'une case : celui de sa place dans sa bifurcation, ou de toutes les voies d'un accord. */
    public Style darkStyle(DarkUpgrade dark) {
        if (dark.isFork()) return darkForkOptions(dark.fork()).indexOf(dark) == 0 ? Style.PRESENCE : Style.ABSENCE;
        if (dark.isAccord()) {
            Style style = null;
            for (String id : dark.linkedIds()) {
                Style each = darkStyle(darkUpgrade(id));
                if (style == null) style = each;
                else if (style != each) return Style.NONE;
            }
            return style == null ? Style.NONE : style;
        }
        return Style.NONE;
    }

    /** Nombre de voies de ce style que possède le joueur. */
    public int darkVoiesOwned(Style style) {
        int count = 0;
        for (DarkUpgrade dark : darkUpgrades.values()) {
            if (dark.isFork() && darkStyle(dark) == style && state.darkLevelOf(dark.id()) > 0) count++;
        }
        return count;
    }

    /** Ce que l'arbre ajoute à la part du temps d'absence rejouée : les voies d'absence, et l'accord d'absence. */
    public double darkOfflineBonus() {
        return this.<Double>darkMemo("offlineBonus", () -> {
            double bonus = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                int level = effectLevel(dark);
                if (level == 0) continue;
                if (dark.isFork() && darkStyle(dark) == Style.ABSENCE) bonus += ABSENCE_VOIE_BONUS * level;
                if (dark.effect() instanceof DarkEffect.OfflineBonus more) bonus += more.points() * level;
            }
            return bonus;
        });
    }

    /**
     * Somme, sur les améliorations d'un type, de {@code par unité × niveau × matière noire gagnée} :
     * la forme commune des effets payés en matière noire.
     */
    private double darkMatterScaling(String key, java.util.function.ToDoubleFunction<DarkEffect> perUnit) {
        double total = this.<Double>darkMemo(key, () -> {
            double sum = 0;
            for (DarkUpgrade dark : darkUpgrades.values()) {
                int level = effectLevel(dark);
                if (level > 0) sum += perUnit.applyAsDouble(dark.effect()) * level;
            }
            return sum;
        });
        return total == 0 ? 0 : total * darkMatterUnits();
    }

    /** Ce par quoi les améliorations payées en matière noire multiplient les atomes de chaque fusion. */
    public double darkAtomsMultiplier() {
        return 1 + darkMatterScaling("atomsByDarkMatter", effect -> effect instanceof DarkEffect.AtomsByDarkMatter by ? by.perUnit() : 0);
    }

    /** Ce par quoi les améliorations payées en matière noire divisent le délai des automatismes ordinaires. */
    public double darkAutomationDivisor() {
        return 1 + darkMatterScaling("automationsByDarkMatter", effect -> effect instanceof DarkEffect.AutomationsByDarkMatter by ? by.perUnit() : 0);
    }

    /** Ce par quoi les améliorations payées en matière noire divisent le prix de la synthèse. */
    public double darkSynthesisDivisor() {
        return 1 + darkMatterScaling("synthesisByDarkMatter", effect -> effect instanceof DarkEffect.SynthesisByDarkMatter by ? by.perUnit() : 0);
    }

    /**
     * Atomes déjà comptés comme créés au départ de chaque partie, grâce aux améliorations payées
     * en matière noire : jamais plus que {@link #UNLOCK_TOTAL_ATOMS}.
     */
    public BigNum darkHeadStartAtoms() {
        if (inChallenge()) return BigNum.ZERO;
        double atoms = Math.floor(darkMatterScaling("headStart",
                effect -> effect instanceof DarkEffect.HeadStartByDarkMatter by ? by.perUnit() : 0));
        return BigNum.of(atoms).min(UNLOCK_TOTAL_ATOMS);
    }

    /** Porte le compte des atomes créés au niveau du départ lancé, s'il est en dessous. */
    void applyHeadStart() {
        BigNum headStart = darkHeadStartAtoms();
        if (state.totalAtoms().lt(headStart)) state.setTotalAtoms(headStart);
    }

    /** Vrai si le joueur possède au moins un niveau d'une amélioration de matière noire de ce type. */
    boolean hasDark(Class<? extends DarkEffect> type) {
        return darkMemo(type, () -> {
            for (DarkUpgrade dark : darkUpgrades.values()) {
                if (type.isInstance(dark.effect()) && effectLevel(dark) > 0) return true;
            }
            return false;
        });
    }

    /** Les cases possédées dont l'effet sur les particules suit la matière noire ou sa taille : peu nombreuses, parcourues à chaque calcul. */
    List<DarkUpgrade> darkScaled() {
        return darkMemo("scaledParticles", () -> darkUpgrades.values().stream()
                .filter(dark -> effectLevel(dark) > 0
                        && (dark.effect() instanceof DarkEffect.ParticlesByDarkMatter || dark.effect() instanceof DarkEffect.ParticlesByLightYears))
                .toList());
    }
}

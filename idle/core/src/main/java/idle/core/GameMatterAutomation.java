package idle.core;

import idle.core.achievements.Achievement;
import idle.core.bigbang.BangChallenge;
import idle.core.bigbang.BigBangMilestone;
import idle.core.bigbang.BigBangMilestones;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.ShoppingList;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * La matière en automatique : création, planification, rassemblement, favorites et liste de courses.
 */
public abstract class GameMatterAutomation extends GameMolecules {

    GameMatterAutomation(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    public abstract double bangReward(BangChallenge.Reward reward);
    abstract boolean ownsSpaceEffect(Class<? extends SpaceUpgrade.Effect> type);


    // ------------------------------------------------------------------
    // La création automatique des molécules
    // ------------------------------------------------------------------

    /** Secondes entre deux passages de la création automatique : à chaque passage, une création par amas choisi. */
    public static final double MOLECULE_AUTOMATION_SECONDS = 5;

    /** Nombre de passages qu'un seul appel à {@link #tick(double)} rattrape au plus : un long temps hors-ligne ne bloque pas le jeu. */
    private static final int MOLECULE_AUTOMATION_CATCH_UP = 20;

    double moleculeAutomationTimer = 0;
    int moleculeAutomationTurn = 0;

    /**
     * Vrai si le prochain Big Bang laissera les défis réussis ({@link BigBangMilestone.KeepChallenges}) :
     * le palier qui le donne est atteint, ou ce Big Bang l'atteindra. Ce Big Bang-là en profite donc déjà.
     */
    public boolean bigBangKeepsChallenges() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            // Le Big Bang qui ouvre le palier en profite déjà ; les suivants, une fois le palier payé (version 0.124).
            if (milestone.bigBangs() != state.bigBangs() + 1 && !isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.KeepChallenges) return true;
            }
        }
        return false;
    }

    /**
     * Vrai si le prochain Big Bang laissera l'arbre de matière noire en place
     * ({@link BigBangMilestone.KeepDarkTree}) : le palier qui le donne est atteint, ou ce Big Bang
     * l'atteindra. Ce Big Bang-là en profite donc déjà.
     */
    public boolean bigBangKeepsDarkTree() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            // Le Big Bang qui ouvre le palier en profite déjà ; les suivants, une fois le palier payé (version 0.124).
            if (milestone.bigBangs() != state.bigBangs() + 1 && !isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.KeepDarkTree) return true;
            }
        }
        return false;
    }

    /** Vrai une fois atteint le palier de Big Bang qui donne la création automatique ({@link BigBangMilestone.AutoMolecules}). */
    public boolean isMoleculeAutomationUnlocked() {
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.AutoMolecules) return true;
            }
        }
        return false;
    }

    /** Vrai si la création automatique est réglée sur « en marche » ; elle l'est par défaut dès qu'elle est acquise. */
    public boolean isMoleculeAutomationEnabled() {
        return state.autoMolecules();
    }

    /**
     * Met en marche ou coupe la création automatique.
     *
     * @return {@code true} si le réglage a été pris en compte (il faut l'avoir acquise)
     */
    public boolean setMoleculeAutomationEnabled(boolean enabled) {
        if (!isMoleculeAutomationUnlocked()) return false;
        state.setAutoMolecules(enabled);
        return true;
    }

    /** Vrai si la création automatique entretient cette sorte. */
    public boolean isMoleculeAutomated(String moleculeId) {
        return state.isMoleculeAutomated(molecule(moleculeId).id());
    }

    /**
     * Confie un amas à la création automatique, ou le lui retire. Seule une sorte rassemblée peut
     * lui être confiée : c'est là qu'une création ajoute plusieurs molécules.
     *
     * @return {@code true} si le choix a été pris en compte
     */
    public boolean setMoleculeAutomated(String moleculeId, boolean automated) {
        Molecule molecule = molecule(moleculeId);
        if (!isMoleculeAutomationUnlocked()) return false;
        if (automated && !state.hasSubstance(molecule.id())) return false;
        state.setMoleculeAutomated(molecule.id(), automated);
        return true;
    }

    /** Nombre d'amas confiés à la création automatique. */
    public int automatedMolecules() {
        return state.automatedMolecules().size();
    }

    /** Vrai si la création automatique agit en ce moment : acquise, en marche, et au moins un amas lui est confié. */
    public boolean isAutoCreatingMolecules() {
        return isMoleculeAutomationUnlocked() && state.autoMolecules()
                && (!state.automatedMolecules().isEmpty() || state.autoFollowList() || state.autoFollowMatter())
                && !state.automationPaused() && !bangRuled(BangChallenge.BY_HAND);
    }

    /** Vrai si la création automatique suit la liste de courses (le réglage par défaut). */
    public boolean isAutoFollowingList() {
        return state.autoFollowList();
    }

    /** Fait suivre la liste de courses à la création automatique, ou non ; faux tant qu'elle n'est pas acquise. */
    public boolean setAutoFollowList(boolean follow) {
        if (!isMoleculeAutomationUnlocked()) return false;
        state.setAutoFollowList(follow);
        return true;
    }

    /** Mode facultatif, éteint par défaut : entretient les substances utiles aux besoins de matière. */
    public boolean isAutoFollowingMatter() { return state.autoFollowMatter(); }

    public boolean setAutoFollowMatter(boolean enabled) {
        if (!isMoleculeAutomationUnlocked() || !isStatesUnlocked()) return false;
        state.setAutoFollowMatter(enabled);
        return true;
    }

    /**
     * Pour chaque état dont le prochain objectif demande plus de matière rassemblée, sélectionne
     * une substance existante peu encombrante. Ne découvre ni élément ni nouvelle substance ;
     * les choix manuels et la liste de courses restent prioritaires.
     */
    public List<String> autoMatterTargets() {
        if (!isStatesUnlocked() || !isMoleculeAutomationUnlocked()) return List.of();
        List<String> selected = new ArrayList<>();
        for (Map.Entry<Molecule.State, Integer> need : shoppingList().matter().entrySet()) {
            if (need.getValue() <= 0) continue;
            Molecule best = null;
            BigNum least = null;
            for (Molecule candidate : moleculeList) {
                if (!candidate.hasState() || candidate.state() != need.getKey()
                        || !state.hasSubstance(candidate.id()) || !isMoleculeKindUnlocked(candidate.kind())) continue;
                BigNum volume = moleculeVolume(candidate.id());
                if (best == null || volume.lt(least)) { best = candidate; least = volume; }
            }
            if (best != null) selected.add(best.id());
        }
        return selected;
    }

    /**
     * Fait passer le temps de la création automatique : toutes les {@link #MOLECULE_AUTOMATION_SECONDS}
     * secondes, une création dans chaque amas choisi qui peut en recevoir une, au même prix qu'à la
     * main ({@link #createMolecule(String)}). Quand l'espace manque pour tous, le premier servi
     * change à chaque passage : aucun amas n'est oublié.
     */
    void runMoleculeAutomation(double dt) {
        if (dt <= 0 || !state.started() || !isAutoCreatingMolecules()) {
            moleculeAutomationTimer = 0;
            return;
        }
        moleculeAutomationTimer += dt;
        double interval = moleculeAutomationInterval();
        int rounds = (int) Math.min(MOLECULE_AUTOMATION_CATCH_UP, Math.floor(moleculeAutomationTimer / interval + 1e-9));
        if (rounds <= 0) return;
        moleculeAutomationTimer = Math.max(0, Math.min(interval, moleculeAutomationTimer - rounds * interval));
        double reserve = state.autoMoleculeReserve();
        List<String> chosen = state.automatedMolecules();
        for (int round = 0; round < rounds; round++) {
            boolean created = false;
            // D'abord ce que demande le prochain pas : une création par sorte qui manque, dans l'ordre de la liste.
            if (state.autoFollowList()) {
                for (ShoppingList.Item item : shoppingList().items()) {
                    if (item.missing() <= 0) continue;
                    String id = item.molecule().id();
                    if (reserve > 0 && !leavesReserve(id, reserve)) continue;
                    if (!withinStateCap(id)) continue;
                    created |= createMolecule(id, true);
                }
            }
            // Répare les besoins de matière sans monopoliser les amas manuels, et sans les doubler.
            if (state.autoFollowMatter()) {
                for (String id : autoMatterTargets()) {
                    if (chosen.contains(id)) continue; // le joueur gère déjà cette substance lui-même
                    if (reserve > 0 && !leavesReserve(id, reserve)) continue;
                    if (!withinStateCap(id)) continue;
                    created |= createMolecule(id, true);
                }
            }
            if (chosen.isEmpty()) {
                if (!created) break;
                continue;
            }
            int first = Math.floorMod(moleculeAutomationTurn++, chosen.size());
            for (int offset = 0; offset < chosen.size(); offset++) {
                String id = chosen.get((first + offset) % chosen.size());
                if (!molecules.containsKey(id)) continue;
                // La réserve : la création n'a lieu que si elle laisse libre la part d'espace que le joueur veut garder.
                if (reserve > 0 && !leavesReserve(id, reserve)) continue;
                if (!withinStateCap(id)) continue;
                created |= createMolecule(id, true);
            }
            if (!created) break;
        }
    }

    // ------------------------------------------------------------------
    // Planifier la matière (version 0.085)
    // ------------------------------------------------------------------

    /** Astres formés qu'il faut pour planifier plusieurs astres à la fois. */
    public static final int BODY_PLANNING_BODIES = 5;
    /** Astres planifiés ensemble, au plus. */
    public static final int MAX_PLANNED_BODIES = 3;

    /** Vrai une fois {@link #BODY_PLANNING_BODIES} astres formés : la planification de plusieurs astres s'ouvre. */
    public boolean isBodyPlanningUnlocked() {
        return isBodiesUnlocked() && bodiesFormed() >= BODY_PLANNING_BODIES;
    }

    /** Les astres planifiés qui ne sont pas encore formés, dans l'ordre du choix. */
    public List<Body> plannedBodies() {
        List<Body> planned = new ArrayList<>();
        for (String id : state.plannedBodies()) {
            Body body = bodies.get(id);
            if (body != null && !state.hasBody(id)) planned.add(body);
        }
        return planned;
    }

    /** Vrai si cet astre est planifié. */
    public boolean isBodyPlanned(String bodyId) {
        return state.plannedBodies().contains(bodyId) && !state.hasBody(bodyId);
    }

    /**
     * Ajoute cet astre au plan, ou l'en retire. Faux si la planification n'est pas ouverte, si
     * l'astre est déjà formé, ou s'il y en a déjà {@link #MAX_PLANNED_BODIES}.
     */
    public boolean togglePlannedBody(String bodyId) {
        Body body = body(bodyId);
        List<String> planned = new ArrayList<>();
        for (Body kept : plannedBodies()) planned.add(kept.id());
        if (planned.remove(body.id())) {
            state.setPlannedBodies(planned);
            return true;
        }
        if (!isBodyPlanningUnlocked() || state.hasBody(body.id()) || planned.size() >= MAX_PLANNED_BODIES) return false;
        planned.add(body.id());
        state.setPlannedBodies(planned);
        return true;
    }

    /** Ce que demande le plan de plusieurs astres, une ligne par exigence. */
    public record PlanNeed(Kind kind, String id, long needed, long have, boolean gather, boolean gathered, List<String> bodies) {

        /** Ce qu'est l'exigence. */
        public enum Kind {
            /** Un astre plus petit, à avoir formé et mûri. */
            BODY,
            /** Un assemblage à former. */
            ASSEMBLY,
            /** Des molécules d'une sorte, en nombre, rassemblées ou non. */
            MOLECULE,
            /** De la matière rassemblée dans un état. */
            MATTER
        }

        public PlanNeed {
            bodies = List.copyOf(bodies);
        }

        /** Vrai si c'est déjà réuni. */
        public boolean satisfied() {
            return have >= needed && (!gather || gathered);
        }

        /** Vrai si plusieurs astres du plan la demandent en même temps : elle ne se compte qu'une fois, ou s'ajoute (assemblages). */
        public boolean shared() {
            return bodies.size() > 1;
        }

        /** Ce qu'il manque, en nombre. */
        public long missing() {
            return Math.max(0, needed - have);
        }
    }

    /**
     * Les besoins réunis des astres planifiés ({@link #plannedBodies()}) : les astres plus petits, les
     * assemblages, les molécules et la matière. Les seuils (molécules d'un astre, matière) servent à
     * tous les astres à la fois : le plus haut compte. Les ingrédients des assemblages s'ajoutent,
     * puisque deux assemblages ne se partagent pas leurs molécules. Les exigences déjà réunies
     * viennent après les autres.
     */
    public List<PlanNeed> plannedNeeds() {
        Map<String, Long> smaller = new LinkedHashMap<>(), assembled = new LinkedHashMap<>();
        Map<String, long[]> wanted = new LinkedHashMap<>();       // sorte → {ingrédients des assemblages, seuil de l'astre, à rassembler}
        Map<Molecule.State, Long> matter = new java.util.EnumMap<>(Molecule.State.class);
        Map<String, List<String>> askers = new java.util.HashMap<>();
        for (Body body : plannedBodies()) {
            for (String id : body.bodies()) {
                smaller.put(id, 1L);
                askers.computeIfAbsent("B" + id, key -> new ArrayList<>()).add(body.id());
            }
            for (String id : body.assemblies()) {
                assembled.put(id, 1L);
                askers.computeIfAbsent("A" + id, key -> new ArrayList<>()).add(body.id());
                if (state.hasAssembly(id)) continue;
                for (Map.Entry<String, Integer> ingredient : assemblies.get(id).ingredients().entrySet()) {
                    long[] want = wanted.computeIfAbsent(ingredient.getKey(), key -> new long[3]);
                    want[0] += ingredient.getValue();
                    want[2] = 1;
                    List<String> who = askers.computeIfAbsent("M" + ingredient.getKey(), key -> new ArrayList<>());
                    if (!who.contains(body.id())) who.add(body.id());
                }
            }
            for (Map.Entry<String, Integer> needed : bodyMolecules(body).entrySet()) {
                long[] want = wanted.computeIfAbsent(needed.getKey(), key -> new long[3]);
                want[1] = Math.max(want[1], needed.getValue());
                List<String> who = askers.computeIfAbsent("M" + needed.getKey(), key -> new ArrayList<>());
                if (!who.contains(body.id())) who.add(body.id());
            }
            for (Map.Entry<Molecule.State, Integer> needed : bodyMatter(body).entrySet()) {
                matter.merge(needed.getKey(), (long) needed.getValue(), Math::max);
                askers.computeIfAbsent("S" + needed.getKey().name(), key -> new ArrayList<>()).add(body.id());
            }
        }
        List<PlanNeed> needs = new ArrayList<>();
        for (String id : smaller.keySet()) {
            needs.add(new PlanNeed(PlanNeed.Kind.BODY, id, 1, isBodyMature(id) ? 1 : 0, false, false, askers.get("B" + id)));
        }
        for (String id : assembled.keySet()) {
            needs.add(new PlanNeed(PlanNeed.Kind.ASSEMBLY, id, 1, state.hasAssembly(id) ? 1 : 0, false, false, askers.get("A" + id)));
        }
        for (Map.Entry<String, long[]> want : wanted.entrySet()) {
            String id = want.getKey();
            if (!molecules.containsKey(id)) continue;
            long[] w = want.getValue();
            long needed = Math.max(w[1], w[0] > 0 ? assembledMolecules(id) + w[0] : 0);
            if (w[2] == 1) needed = Math.max(needed, SUBSTANCE_MOLECULES);
            needs.add(new PlanNeed(PlanNeed.Kind.MOLECULE, id, needed, state.moleculeCount(id), w[2] == 1, state.hasSubstance(id),
                    askers.get("M" + id)));
        }
        for (Map.Entry<Molecule.State, Long> need : matter.entrySet()) {
            needs.add(new PlanNeed(PlanNeed.Kind.MATTER, need.getKey().name(), need.getValue(), gatheredInState(need.getKey()), false, false,
                    askers.get("S" + need.getKey().name())));
        }
        List<PlanNeed> ordered = new ArrayList<>();
        for (PlanNeed need : needs) if (!need.satisfied()) ordered.add(need);
        for (PlanNeed need : needs) if (need.satisfied()) ordered.add(need);
        return ordered;
    }

    /** Vrai une fois les plafonds par état ouverts : la création automatique et les états de la matière sont acquis. */
    public boolean isStateCapUnlocked() {
        return isMoleculeAutomationUnlocked() && isStatesUnlocked();
    }

    /** La part de l'espace que la création automatique laisse aux molécules de cet état, de 0,05 à 1 (sans plafond). */
    public double stateCap(Molecule.State matter) {
        return state.stateCap(matter.name());
    }

    /** Règle le plafond d'un état, ramené entre 0,05 et 1 ; 1 l'efface. */
    public void setStateCap(Molecule.State matter, double cap) {
        state.setStateCap(matter.name(), Double.isNaN(cap) ? 1 : Math.max(0.05, Math.min(1, cap)));
    }

    private final Map<Molecule.State, Double> spaceByState = new java.util.EnumMap<>(Molecule.State.class);
    long spaceByStateVersion = -1;

    /** L'espace qu'occupent les molécules de cet état, rassemblées ou non. */
    public BigNum spaceInState(Molecule.State matter) {
        if (spaceByStateVersion != state.moleculesVersion()) {
            spaceByStateVersion = state.moleculesVersion();
            spaceByState.clear();
            for (Molecule molecule : moleculeList) {
                if (!molecule.hasState()) continue;
                int count = state.moleculeCount(molecule.id());
                if (count > 0) spaceByState.merge(molecule.state(), count * volumeOf(molecule), Double::sum);
            }
        }
        return BigNum.of(spaceByState.getOrDefault(matter, 0.0));
    }

    /** La part de l'espace créé qu'occupent les molécules de cet état, de 0 à 1. */
    public double stateShare(Molecule.State matter) {
        double space = state.space().toDouble();
        return space > 0 ? Math.min(1, spaceInState(matter).toDouble() / space) : 0;
    }

    /** Vrai si la création automatique peut créer cette molécule sans dépasser le plafond de son état. */
    private boolean withinStateCap(String moleculeId) {
        Molecule molecule = molecules.get(moleculeId);
        if (molecule == null || !molecule.hasState()) return true;
        double cap = stateCap(molecule.state());
        if (cap >= 1 || !isStateCapUnlocked()) return true;
        BigNum after = spaceInState(molecule.state()).add(moleculeVolume(moleculeId).multiply(moleculesNextCreation(moleculeId)));
        return after.lte(state.space().multiply(cap));
    }

    /** Vrai une fois le premier assemblage formé : la réservation par recette s'ouvre. */
    public boolean isRecipeReservationUnlocked() {
        return assembliesFormed() > 0;
    }

    /** Vrai si la réservation par recette est ouverte et allumée. */
    public boolean isRecipeReservationOn() {
        return isRecipeReservationUnlocked() && state.recipeReservation();
    }

    public void setRecipeReservation(boolean on) {
        state.setRecipeReservation(on);
    }

    /**
     * Le prochain assemblage à former : le premier de l'astre visé ({@link #nextBody()}) qui ne l'est
     * pas encore, ou celui de la liste de courses avant les astres. {@code null} s'il n'y en a pas.
     */
    public Assembly nextAssembly() {
        // Les cartes des molécules le demandent chacune : il n'est recalculé que lorsque la liste de courses peut changer.
        long key = shoppingKey();
        if (key != nextAssemblyKey) {
            nextAssemblyKey = key;
            nextAssembly = computeNextAssembly();
        }
        return nextAssembly;
    }

    private Assembly nextAssembly;
    long nextAssemblyKey = Long.MIN_VALUE;

    private Assembly computeNextAssembly() {
        Body body = nextBody();
        if (body != null) {
            for (String id : body.assemblies()) if (!state.hasAssembly(id)) return assemblies.get(id);
            return null;
        }
        ShoppingList list = shoppingList();
        if (list.kind() != ShoppingList.Kind.ASSEMBLY) return null;
        for (Assembly assembly : assemblies.values()) if (assembly.name().equals(list.target())) return assembly;
        return null;
    }

    /**
     * Molécules de cette sorte qu'il faut encore créer pour le prochain assemblage ; −1 si elle n'en
     * est pas un ingrédient. Celles des assemblages déjà formés n'y comptent pas.
     */
    public int recipeRemaining(String moleculeId) {
        Assembly next = nextAssembly();
        if (next == null) return -1;
        Integer count = next.ingredients().get(moleculeId);
        if (count == null) return -1;
        return Math.max(0, assembledMolecules(moleculeId) + count - state.moleculeCount(moleculeId));
    }

    /**
     * Vrai si créer cette sorte maintenant serait une production saturante inutile : la réservation
     * par recette est allumée, la sorte a déjà ce qu'il faut au prochain assemblage, et l'espace libre
     * ne suffirait plus à ce que demande encore la liste de courses.
     */
    public boolean isSaturating(String moleculeId) {
        if (!isRecipeReservationOn() || recipeRemaining(moleculeId) != 0) return false;
        return freeSpace().subtract(moleculeNextSpace(moleculeId)).lt(shoppingSpace());
    }

    /** Vrai une fois le premier Big Bang fait : le filtre « utiles au prochain astre » du tableau s'ouvre. */
    public boolean isNextBodyFilterUnlocked() {
        return state.bigBangs() > 0;
    }

    /**
     * Les éléments qu'il faut aux recettes des molécules utiles au prochain pas
     * ({@link ShoppingList#wants(Molecule)}), parmi celles que le jeu offre déjà : leurs numéros atomiques.
     */
    public java.util.Set<Integer> elementsForNextStep() {
        java.util.Set<Integer> numbers = new java.util.TreeSet<>();
        ShoppingList list = shoppingList();
        if (list.kind() == ShoppingList.Kind.NONE) return numbers;
        for (Molecule molecule : moleculeList) {
            if (!isMoleculeKindUnlocked(molecule.kind()) || !list.wants(molecule)) continue;
            // Une matière qui manque dans un état : seules les sortes déjà rassemblées comptent, sinon tout l'état passerait.
            boolean listed = false;
            for (ShoppingList.Item item : list.items()) listed |= item.molecule().id().equals(molecule.id());
            if (!listed && !state.hasSubstance(molecule.id())) continue;
            numbers.addAll(molecule.recipe().keySet());
        }
        return numbers;
    }

    /** Vrai si créer cette molécule maintenant laisserait libre au moins cette part de l'espace créé. */
    private boolean leavesReserve(String moleculeId, double share) {
        BigNum cost = moleculeVolume(moleculeId).multiply(moleculesNextCreation(moleculeId));
        return freeSpace().subtract(cost).gte(state.space().multiply(share));
    }

    /**
     * Délai entre deux passages de la création automatique, en secondes :
     * {@link #MOLECULE_AUTOMATION_SECONDS}, ou moins une fois acquise une amélioration d'espace qui
     * le raccourcit ({@link SpaceUpgrade.CreationPace}).
     */
    public double moleculeAutomationInterval() {
        double interval = MOLECULE_AUTOMATION_SECONDS;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.CreationPace pace && state.ownsSpaceUpgrade(upgrade.id())) {
                interval = Math.min(interval, pace.seconds());
            }
        }
        return interval;
    }

    /** Part de l'espace créé que la création automatique laisse toujours libre, de 0 à 1 : la création à la main n'en tient pas compte. */
    public double autoMoleculeReserve() {
        return state.autoMoleculeReserve();
    }

    /** Règle la part d'espace que la création automatique laisse libre ; ramenée entre 0 et 0,95. */
    public void setAutoMoleculeReserve(double share) {
        state.setAutoMoleculeReserve(Math.max(0, Math.min(0.95, Double.isNaN(share) ? 0 : share)));
    }

    /**
     * Confie à la création automatique toutes les sortes rassemblées, ou les lui retire toutes.
     *
     * @return le nombre de sortes dont le réglage a changé ; 0 tant que l'automatisme n'est pas acquis
     */
    public int setAllMoleculesAutomated(boolean automated) {
        if (!isMoleculeAutomationUnlocked()) return 0;
        int changed = 0;
        for (String id : automated ? state.substances() : state.automatedMolecules()) {
            if (!molecules.containsKey(id) || state.isMoleculeAutomated(id) == automated) continue;
            state.setMoleculeAutomated(id, automated);
            changed++;
        }
        return changed;
    }

    // ------------------------------------------------------------------
    // Le rassemblement et la formation automatiques
    // ------------------------------------------------------------------

    /** Délai entre deux passages du rassemblement et de la formation automatiques, en secondes. */
    public static final double AUTO_FORM_SECONDS = 1;
    double autoFormTimer = 0;
    int autoGathered = 0;
    final List<String> autoFormed = new ArrayList<>();

    /** Vrai une fois acquise l'amélioration d'espace « Rassemblement automatique » ({@link SpaceUpgrade.AutoGather}). */
    public boolean isAutoGatherUnlocked() {
        return ownsSpaceEffect(SpaceUpgrade.AutoGather.class);
    }

    /** Vrai si le rassemblement automatique est en marche (il ne sert qu'une fois acquis). */
    public boolean isAutoGatherEnabled() {
        return state.autoGather();
    }

    /** Met en marche ou coupe le rassemblement automatique ; faux s'il n'est pas acquis. */
    public boolean setAutoGatherEnabled(boolean enabled) {
        if (!isAutoGatherUnlocked()) return false;
        state.setAutoGather(enabled);
        return true;
    }

    /** Vrai si les sortes se rassemblent d'elles-mêmes en ce moment : l'automatisme est acquis, en marche, et rien n'est coupé. */
    public boolean isAutoGathering() {
        return isAutoGatherUnlocked() && state.autoGather() && !state.automationPaused() && !bangRuled(BangChallenge.BY_HAND);
    }

    /** Vrai une fois acquise l'amélioration d'espace « Formation automatique » ({@link SpaceUpgrade.AutoForm}). */
    public boolean isAutoFormUnlocked() {
        return ownsSpaceEffect(SpaceUpgrade.AutoForm.class);
    }

    /** Vrai si la formation automatique est en marche (elle ne sert qu'une fois acquise). */
    public boolean isAutoFormEnabled() {
        return state.autoForm();
    }

    /** Met en marche ou coupe la formation automatique ; faux si elle n'est pas acquise. */
    public boolean setAutoFormEnabled(boolean enabled) {
        if (!isAutoFormUnlocked()) return false;
        state.setAutoForm(enabled);
        return true;
    }

    /** Vrai si les assemblages et les astres se forment d'eux-mêmes en ce moment. */
    public boolean isAutoForming() {
        return isAutoFormUnlocked() && state.autoForm() && !state.automationPaused() && !bangRuled(BangChallenge.BY_HAND);
    }

    /**
     * Fait passer le temps du rassemblement et de la formation automatiques : une fois par
     * seconde, tout ce qui peut être rassemblé l'est, puis tout assemblage et tout astre dont les
     * conditions sont réunies est formé. Les échelles du cosmos restent au joueur.
     */
    void runAutoForm(double dt) {
        boolean gathering = isAutoGathering();
        boolean forming = isAutoForming();
        if (dt <= 0 || !state.started() || (!gathering && !forming)) {
            autoFormTimer = 0;
            return;
        }
        autoFormTimer += dt;
        if (autoFormTimer < AUTO_FORM_SECONDS) return;
        autoFormTimer = 0;
        if (gathering) {
            for (String id : state.molecules().keySet()) {
                if (molecules.containsKey(id) && formSubstance(id)) autoGathered++;
            }
        }
        if (!forming) return;
        boolean again;
        do {
            again = false;
            for (Assembly assembly : assemblies.values()) {
                if (formAssembly(assembly.id())) {
                    autoFormed.add(assembly.name());
                    again = true;
                }
            }
            for (Body body : bodies.values()) {
                if (formBody(body.id())) {
                    autoFormed.add(body.name());
                    again = true;
                }
            }
        } while (again);
    }

    /** Nombre de sortes que l'automatisme a rassemblées depuis le dernier appel : pour l'annoncer. Remis à zéro à chaque lecture. */
    public int takeAutoGathered() {
        int gathered = autoGathered;
        autoGathered = 0;
        return gathered;
    }

    /** Les noms des assemblages et des astres que l'automatisme a formés depuis le dernier appel, dans l'ordre. La liste est vidée à chaque lecture. */
    public List<String> takeAutoFormed() {
        List<String> formed = List.copyOf(autoFormed);
        autoFormed.clear();
        return formed;
    }

    /** Ce par quoi les paliers atteints multiplient la matière noire de chaque explosion ({@link BigBangMilestone.DarkMatter}). */
    public double bigBangDarkMatterFactor() {
        if (bangRuled(BangChallenge.FORGOTTEN)) return 1;
        double factor = 1;
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.DarkMatter dark) factor *= dark.factor();
            }
        }
        return factor;
    }

    /** Ce par quoi les paliers atteints multiplient ce qu'un amas attire à chaque création ({@link BigBangMilestone.Accretion}). */
    public double accretionFactor() {
        // La récompense d'un défi de Big Bang reste, même pendant celui qui fait taire les paliers.
        double factor = bangReward(BangChallenge.Reward.ACCRETION);
        if (bangRuled(BangChallenge.FORGOTTEN)) return factor;
        for (BigBangMilestone milestone : BigBangMilestones.DEFAULT) {
            if (!isBigBangMilestoneReached(milestone)) continue;
            for (BigBangMilestone.Effect effect : milestone.effects()) {
                if (effect instanceof BigBangMilestone.Accretion accretion) factor *= accretion.factor();
            }
        }
        return factor;
    }

    /** Vrai une fois le premier Big Bang déclenché : l'acte suivant existe. */
    public boolean isBigBangUnlocked() {
        return state.bigBangs() > 0;
    }

    /** Nombre de Big Bangs déclenchés depuis le début du jeu. */
    public int bigBangs() {
        return state.bigBangs();
    }

    // ------------------------------------------------------------------
    // Les molécules favorites et la liste de courses
    // ------------------------------------------------------------------

    /** Vrai si le joueur a épinglé cette molécule. */
    public boolean isMoleculeFavorite(String moleculeId) {
        return state.isMoleculeFavorite(molecule(moleculeId).id());
    }

    /** Épingle une molécule du catalogue, ou la retire des favorites. */
    public void setMoleculeFavorite(String moleculeId, boolean favorite) {
        state.setMoleculeFavorite(molecule(moleculeId).id(), favorite);
    }

    /** Les molécules épinglées, dans l'ordre où elles l'ont été. */
    public List<Molecule> favoriteMolecules() {
        List<Molecule> favorites = new ArrayList<>();
        for (String id : state.favoriteMolecules()) {
            Molecule molecule = molecules.get(id);
            if (molecule != null) favorites.add(molecule);
        }
        return favorites;
    }

    private ShoppingList shopping = ShoppingList.NONE;
    long shoppingVersion = Long.MIN_VALUE;

    /**
     * Ce qu'il manque pour le prochain pas du troisième acte ({@link ShoppingList}) : l'astre à
     * portée le plus avancé, l'assemblage le plus avancé tant que les astres ne sont pas ouverts,
     * ou la prochaine échelle du cosmos une fois tous les astres formés.
     */
    public ShoppingList shoppingList() {
        long key = shoppingKey();
        if (shoppingVersion != key) {
            shoppingVersion = key;
            shopping = computeShoppingList();
        }
        return shopping;
    }

    /**
     * Ce dont dépend la liste de courses. Les molécules ne suffisaient pas : un assemblage ou un astre
     * formé, un rassemblement, une explosion qui vide le tableau, une amélioration d'espace ou un Big
     * Bang la changent aussi. Avant la 0.047, elle pouvait rester sur un astre déjà formé et ne plus
     * rien montrer.
     */
    private long shoppingKey() {
        long key = state.moleculesVersion();
        for (int part : new int[] {state.elementsVersion(), assembliesFormed(), bodiesFormed(), substancesFormed(),
                state.spaceUpgradeCount(), state.cosmosLevel(), state.bigBangs(), state.started() ? 1 : 0}) {
            key = key * 1_000_003L + part;
        }
        return key;
    }

    private ShoppingList computeShoppingList() {
        if (!state.started() || !isBigBangUnlocked()) return ShoppingList.NONE;
        if (isBodiesUnlocked()) {
            Body best = nextBody();
            if (best != null) return shoppingFor(best);
        } else if (isAssembliesUnlocked()) {
            // L'assemblage entamé auquel il manque le moins d'ingrédients ; à défaut, le premier du catalogue.
            Assembly best = null;
            int bestMissing = Integer.MAX_VALUE;
            for (Assembly assembly : assemblies.values()) {
                if (state.hasAssembly(assembly.id())) continue;
                int missing = 0;
                boolean started = false;
                for (Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
                    if (spareMolecules(ingredient.getKey()) < ingredient.getValue()) missing++;
                    started |= state.moleculeCount(ingredient.getKey()) > 0;
                }
                if (best == null || (started && missing < bestMissing)) {
                    best = assembly;
                    bestMissing = started ? missing : Integer.MAX_VALUE;
                }
            }
            if (best != null) {
                Map<String, int[]> wanted = new LinkedHashMap<>();
                Map<String, String> purposes = new java.util.HashMap<>();
                wantAssembly(best, wanted, purposes);
                return new ShoppingList(ShoppingList.Kind.ASSEMBLY, best.name(), canFormAssembly(best.id()),
                        shoppingItems(wanted, purposes), Map.of(), canFormAssembly(best.id()) ? List.of(best) : List.of());
            }
        }
        Cosmos next = nextCosmos();
        if (next == null || !isCosmosUnlocked(next)) return ShoppingList.NONE;
        Map<Molecule.State, Integer> matter = new java.util.EnumMap<>(Molecule.State.class);
        for (Map.Entry<Molecule.State, Integer> need : cosmosMatter(next).entrySet()) {
            int missing = (int) Math.max(0, need.getValue() - gatheredInState(need.getKey()));
            if (missing > 0) matter.put(need.getKey(), missing);
        }
        Map<Molecule.State, Integer> sorts = new java.util.EnumMap<>(Molecule.State.class);
        for (Map.Entry<Molecule.State, Integer> need : next.sorts().entrySet()) {
            int missing = need.getValue() - sortsInState(need.getKey());
            if (missing > 0) sorts.put(need.getKey(), missing);
        }
        return new ShoppingList(ShoppingList.Kind.COSMOS, next.phrase(), canFormCosmos(next), List.of(), matter, List.of(), sorts);
    }

    /**
     * L'astre que le joueur a le plus de chances de former ensuite : parmi ceux qui sont à portée
     * (leurs astres plus petits sont formés), celui auquel il manque le moins de conditions ; à
     * égalité, le premier du catalogue. {@code null} avant l'ouverture des astres, ou une fois tous formés.
     */
    public Body nextBody() {
        if (!state.started() || !isBodiesUnlocked()) return null;
        Body best = null;
        int bestMissing = Integer.MAX_VALUE;
        for (Body body : bodies.values()) {
            if (state.hasBody(body.id())) continue;
            // Version 0.138 : une matière exotique n'est pas à portée avant son palier (huitième Big Bang). Sans cela,
            // une nébuleuse planétaire dont la matière était déjà réunie (zéro condition manquante) devenait la cible,
            // jamais formable : la liste de courses restait vide et le 28e astre qu'attend le huitième Big Bang
            // n'était plus visé (le Thésauriseur bloquait à 27 astres pendant 24 h).
            if (body.tier() == Body.Tier.EXOTIC && !isExoticUnlocked()) continue;
            boolean reachable = true;
            for (String smaller : body.bodies()) reachable &= isBodyMature(smaller);
            if (!reachable) continue;
            int missing = bodyConditions(body.id()) - bodyConditionsMet(body.id());
            if (missing < bestMissing) {
                best = body;
                bestMissing = missing;
            }
        }
        return best;
    }

    /**
     * Espace que demande encore la liste de courses ({@link #shoppingList()}) : le volume des
     * molécules qui manquent, celui des lieux de rassemblement à ouvrir, et, pour la matière qui
     * manque dans un état, le volume de la plus petite molécule de cet état que le joueur peut
     * créer. C'est une estimation par le bas : elle ne compte ni les exemplaires d'éléments à
     * regarnir, ni les molécules plus grosses que le joueur choisirait.
     */
    public BigNum shoppingSpace() {
        // Le calcul parcourt le catalogue : il n'est refait que lorsque les molécules ou les rayons ouverts ont changé.
        long key = shoppingKey();
        if (key == shoppingSpaceKey && shoppingSpace != null) return shoppingSpace;
        shoppingSpaceKey = key;
        shoppingSpace = computeShoppingSpace();
        return shoppingSpace;
    }

    private BigNum shoppingSpace = null;
    long shoppingSpaceKey = -1;

    private BigNum computeShoppingSpace() {
        ShoppingList list = shoppingList();
        double space = 0;
        Map<Molecule.State, Integer> covered = new java.util.EnumMap<>(Molecule.State.class);
        for (ShoppingList.Item item : list.items()) {
            Molecule molecule = item.molecule();
            double volume = volumeOf(molecule);
            space += item.missing() * volume;
            if (item.gather() && molecule.hasState()) space += volume * SUBSTANCE_MOLECULES;
            // Rassemblées, ces molécules compteront aussi dans la matière de leur état.
            if (molecule.hasState() && (item.gather() || state.hasSubstance(molecule.id()))) {
                covered.merge(molecule.state(), item.missing(), Integer::sum);
            }
        }
        for (Map.Entry<Molecule.State, Integer> lacking : list.matter().entrySet()) {
            int missing = lacking.getValue() - covered.getOrDefault(lacking.getKey(), 0);
            if (missing > 0) space += missing * smallestVolume(lacking.getKey());
        }
        // Une sorte de plus à rassembler : ses premières molécules, et son lieu, qui en vaut autant.
        for (Map.Entry<Molecule.State, Integer> lacking : list.sorts().entrySet()) {
            space += lacking.getValue() * 2.0 * SUBSTANCE_MOLECULES * smallestVolume(lacking.getKey());
        }
        return BigNum.of(space);
    }

    /** Le volume de la plus petite molécule de cet état dans les rayons ouverts ; d'abord parmi les sortes déjà rassemblées. */
    private double smallestVolume(Molecule.State matter) {
        double gathered = Double.POSITIVE_INFINITY;
        double any = Double.POSITIVE_INFINITY;
        for (Molecule molecule : molecules.values()) {
            if (!molecule.hasState() || molecule.state() != matter || !isMoleculeKindUnlocked(molecule.kind())) continue;
            double volume = volumeOf(molecule);
            any = Math.min(any, volume);
            if (state.hasSubstance(molecule.id())) gathered = Math.min(gathered, volume);
        }
        return Double.isInfinite(gathered) ? (Double.isInfinite(any) ? 0 : any) : gathered;
    }

    /**
     * Secondes d'expansion avant que l'espace libre suffise à la liste de courses
     * ({@link #shoppingSpace()}) : 0 s'il suffit déjà ou s'il n'y a rien à viser, l'infini si
     * l'espace ne grandit pas.
     */
    public double secondsUntilShopping() {
        BigNum needed = shoppingSpace();
        BigNum free = freeSpace();
        if (needed.isZero() || free.gte(needed)) return 0;
        double rate = spacePerSecond().toDouble();
        return rate > 0 ? needed.subtract(free).toDouble() / rate : Double.POSITIVE_INFINITY;
    }
}

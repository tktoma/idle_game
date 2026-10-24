package idle.core;

import idle.core.achievements.Achievement;
import idle.core.bigbang.BangChallenge;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.collider.ColliderUpgrade;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.Element;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.ShoppingList;
import idle.core.sky.Assemblies;
import idle.core.sky.Assembly;
import idle.core.sky.Bodies;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Les molécules et ce qu'elles forment : états de la matière, assemblages, astres, galaxie, cosmos.
 */
public abstract class GameMolecules extends GameUniverse {

    GameMolecules(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    // Ce que fournissent les parties suivantes.
    abstract boolean bangRuled(BangChallenge challenge);
    public abstract double bigBangMilestoneBoost(Molecule.Stat stat);
    public abstract double accretionFactor();
    public abstract double collectionBoost(Molecule.Stat stat);
    abstract void refreshMoleculeBonuses();
    abstract SpaceUpgrade spaceUpgrade(String spaceUpgradeId);
    public abstract boolean canBuySpaceUpgrade(String spaceUpgradeId);
    public abstract BigNum spacePerSecond();


    // ------------------------------------------------------------------
    // Les molécules
    // ------------------------------------------------------------------

    /** Toutes les molécules, dans l'ordre du catalogue. */
    public List<Molecule> molecules() {
        return moleculeList;
    }

    /** La molécule qui porte cet identifiant. */
    public Molecule molecule(String moleculeId) {
        Molecule molecule = molecules.get(moleculeId);
        if (molecule == null) throw new IllegalArgumentException("Molécule inconnue : " + moleculeId);
        return molecule;
    }

    /** Nombre de molécules de cette sorte créées. */
    public int moleculeCount(String moleculeId) {
        return state.moleculeCount(molecule(moleculeId).id());
    }

    /** Nombre de molécules créées, toutes sortes confondues. */
    public long moleculesCreated() {
        return state.moleculeTotal();
    }

    /** Nombre de sortes de molécules dont le joueur a créé au moins une. */
    public int moleculeKindsCreated() {
        return state.moleculeKinds();
    }

    /**
     * Ce que demande une création dans cette sorte : numéro atomique → exemplaires à y mettre, dans
     * l'ordre de la formule. C'est la formule, une fois, quel que soit le nombre de molécules déjà
     * créées : ce qui limite une sorte, c'est l'espace qu'elle occupe ({@link #moleculeVolume(String)}),
     * et le tableau périodique qu'il faut regarnir entre deux créations.
     */
    public Map<Integer, Integer> nextMoleculeCost(String moleculeId) {
        return molecule(moleculeId).recipe();
    }

    /**
     * Espace qu'occupe une molécule de cette sorte : {@link #SPACE_PER_PROTON} par proton, multiplié
     * par ce que demande son état ({@link Molecule.State#spaceFactor()}). Plus elle a d'atomes, et
     * plus ils sont lourds, plus elle prend de place ; et un gaz en prend vingt fois plus qu'un
     * cristal du même poids.
     */
    public BigNum moleculeVolume(String moleculeId) {
        return BigNum.of(volumeOf(molecule(moleculeId)));
    }

    static double volumeOf(Molecule molecule) {
        return SPACE_PER_PROTON * molecule.protons() * (molecule.hasState() ? molecule.state().spaceFactor() : 1);
    }

    /** Espace qu'occupent toutes les molécules créées. */
    public BigNum occupiedSpace() {
        if (occupiedSpaceVersion != state.moleculesVersion()) {
            double volume = 0;
            for (Molecule molecule : moleculeList) {
                int count = state.moleculeCount(molecule.id());
                if (count > 0) volume += count * volumeOf(molecule);
            }
            occupiedSpace = BigNum.of(volume);
            occupiedSpaceVersion = state.moleculesVersion();
        }
        return occupiedSpace;
    }

    /**
     * Espace que réservent les lieux de rassemblement ouverts : la somme de ce qu'a demandé chaque
     * gaz, liquide ou solide formé ({@link #substanceSpace(String)}).
     */
    public BigNum reservedSpace() {
        if (reservedSpaceVersion != state.moleculesVersion()) {
            BigNum reserved = BigNum.ZERO;
            for (String id : state.substances()) {
                Molecule molecule = molecules.get(id);
                if (molecule != null && molecule.hasState()) reserved = reserved.add(substanceSpace(id));
            }
            reservedSpace = reserved;
            reservedSpaceVersion = state.moleculesVersion();
        }
        return reservedSpace;
    }

    /** Espace utilisé : celui que les molécules occupent, et celui que réservent les lieux de rassemblement. */
    public BigNum usedSpace() {
        // Version 0.129 : les paires du vide tiennent leur espace tant qu'elles vivent.
        return occupiedSpace().add(reservedSpace()).add(voidHeldSpace());
    }

    /** Espace encore libre : celui que l'expansion a créé, moins celui qui est utilisé ({@link #usedSpace()}). Jamais négatif. */
    public BigNum freeSpace() {
        BigNum used = usedSpace();
        return state.space().gt(used) ? state.space().subtract(used) : BigNum.ZERO;
    }

    /** Vrai s'il reste au moins {@code amount} unités d'espace libre. */
    private boolean hasFreeSpace(BigNum amount) {
        // Comparé du côté des additions, avec une marge infime : 120 − 100 ne vaut pas tout à fait 20 en virgule flottante.
        return state.space().multiply(1 + 1e-9).gte(usedSpace().add(amount));
    }

    /**
     * Secondes de jeu avant que l'expansion ait créé {@code total} unités d'espace en tout : 0 si
     * c'est déjà fait, l'infini avant le premier Big Bang.
     */
    public double secondsUntilSpace(BigNum total) {
        if (hasCreatedSpace(total)) return 0;
        double missing = total.subtract(state.space()).toDouble();
        double rate = spacePerSecond().toDouble();
        return rate > 0 ? Math.max(0, missing) / rate : Double.POSITIVE_INFINITY;
    }

    /** Vrai si l'expansion a créé au moins {@code total} unités d'espace depuis le premier Big Bang, libres ou non. */
    boolean hasCreatedSpace(BigNum total) {
        // Même marge infime que pour l'espace libre : 2 000 secondes d'expansion doivent donner 2 000 d'espace.
        return state.space().multiply(1 + 1e-9).gte(total);
    }

    /**
     * Vrai si une molécule de cette sorte peut se payer un jour avec le nombre d'exemplaires par
     * élément que le joueur a le droit de posséder en ce moment ({@link #maxCopiesOf(Element)}).
     * Faux : il faut d'abord les Isotopes de l'arbre, ou relever le plafond d'un de ses éléments.
     */
    public boolean isMoleculeWithinReach(String moleculeId) {
        for (Map.Entry<Integer, Integer> atom : nextMoleculeCost(moleculeId).entrySet()) {
            if (atom.getValue() + 1 > maxCopiesOf(PeriodicTable.element(atom.getKey()))) return false;
        }
        return true;
    }

    /** Vrai si le tableau périodique contient ce que demande la prochaine molécule de cette sorte, plus un exemplaire de chaque élément. */
    public boolean hasElementsForMolecule(String moleculeId) {
        for (Map.Entry<Integer, Integer> atom : nextMoleculeCost(moleculeId).entrySet()) {
            if (state.elementCount(atom.getKey()) < atom.getValue() + 1) return false;
        }
        return true;
    }

    /** Vrai s'il reste assez d'espace libre pour une molécule de cette sorte. */
    public boolean hasSpaceForMolecule(String moleculeId) {
        return hasFreeSpace(moleculeVolume(moleculeId));
    }

    /** Vrai si cette sorte a atteint {@link #MAX_MOLECULES} molécules : on ne peut plus en créer, quoi qu'on ait. */
    public boolean isMoleculeKindFull(String moleculeId) {
        return state.moleculeCount(molecule(moleculeId).id()) >= MAX_MOLECULES;
    }

    /**
     * Vrai si une molécule de cette sorte peut être créée maintenant : son rayon est ouvert
     * ({@link #isMoleculeKindUnlocked}), le tableau périodique contient les exemplaires demandés (plus un de chaque élément,
     * qui doit y rester), et l'expansion a créé assez d'espace libre pour la loger.
     */
    public boolean canCreateMolecule(String moleculeId) {
        return state.started() && !bangRuled(BangChallenge.FROZEN) && isMoleculeKindUnlocked(molecule(moleculeId).kind())
                && state.moleculeCount(molecule(moleculeId).id()) < MAX_MOLECULES
                && hasElementsForMolecule(moleculeId) && hasSpaceForMolecule(moleculeId);
    }

    /**
     * Vrai si ce rayon du catalogue est ouvert : le premier Big Bang a eu lieu, et le joueur
     * possède l'amélioration d'espace qui l'ouvre ({@link SpaceUpgrade.OpenKind}). Les petites
     * molécules n'en demandent aucune.
     */
    public boolean isMoleculeKindUnlocked(Molecule.Kind kind) {
        if (!isBigBangUnlocked()) return false;
        if (kind == Molecule.Kind.SIMPLE) return true;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.OpenKind open && open.kind() == kind) {
                return state.ownsSpaceUpgrade(upgrade.id());
            }
        }
        return false;
    }

    /** Nombre de rayons du catalogue ouverts. */
    public int moleculeKindsUnlocked() {
        int unlocked = 0;
        for (Molecule.Kind kind : Molecule.Kind.values()) {
            if (isMoleculeKindUnlocked(kind)) unlocked++;
        }
        return unlocked;
    }

    /** Le premier rayon encore fermé, dans l'ordre du catalogue, ou {@code null} s'ils sont tous ouverts. */
    public Molecule.Kind nextMoleculeKind() {
        for (Molecule.Kind kind : Molecule.Kind.values()) {
            if (!isMoleculeKindUnlocked(kind)) return kind;
        }
        return null;
    }

    /**
     * Crée une molécule du rayon, tirée au hasard parmi celles qui peuvent l'être maintenant : d'abord
     * parmi celles que demande le prochain pas ({@link #shoppingList()}), sinon parmi toutes. C'est
     * l'achat simple : un clic par rayon, sans avoir à choisir la sorte.
     *
     * @return la molécule créée, ou {@code null} si aucune du rayon ne peut l'être
     */
    public Molecule createRandomMolecule(Molecule.Kind kind) {
        if (!isMoleculeKindUnlocked(kind)) return null;
        ShoppingList shopping = shoppingList();
        List<Molecule> wanted = new ArrayList<>();
        List<Molecule> any = new ArrayList<>();
        for (Molecule molecule : molecules.values()) {
            if (molecule.kind() != kind || !canCreateMolecule(molecule.id())) continue;
            any.add(molecule);
            if (shopping.wants(molecule)) wanted.add(molecule);
        }
        List<Molecule> pool = wanted.isEmpty() ? any : wanted;
        if (pool.isEmpty()) return null;
        Molecule chosen = pool.get(random.nextInt(pool.size()));
        return createMolecule(chosen.id()) ? chosen : null;
    }

    /** Vrai si au moins une molécule du rayon peut être créée maintenant. */
    public boolean canCreateInKind(Molecule.Kind kind) {
        if (!isMoleculeKindUnlocked(kind)) return false;
        for (Molecule molecule : molecules.values()) {
            if (molecule.kind() == kind && canCreateMolecule(molecule.id())) return true;
        }
        return false;
    }

    /**
     * Ce que le prochain pas demande, rayon par rayon : le nombre de molécules qu'il reste à créer
     * dans chaque rayon d'après la liste de courses. Les rayons dont rien ne manque n'y sont pas.
     */
    public Map<Molecule.Kind, Integer> missingByKind() {
        Map<Molecule.Kind, Integer> missing = new java.util.EnumMap<>(Molecule.Kind.class);
        for (ShoppingList.Item item : shoppingList().items()) {
            int count = item.missing();
            if (count > 0) missing.merge(item.molecule().kind(), count, Integer::sum);
        }
        return missing;
    }

    /**
     * Crée une molécule : les exemplaires que demande sa formule quittent le tableau périodique,
     * avec ce qu'ils y apportaient, et la molécule prend sa place dans l'espace. Il reste toujours
     * au moins un exemplaire de chaque élément : le tableau reste découvert, et l'explosion reste
     * possible. La molécule, elle, est définitive : ni l'explosion ni le Big Bang ne la reprennent,
     * et l'espace qu'elle occupe ne se libère pas.
     *
     * <p>Dans une sorte rassemblée, l'amas attire en plus de la matière : la création y ajoute
     * {@link #moleculesPerCreation(String)} molécules pour le même prix, dans la limite de l'espace
     * libre. C'est ce qui permet de réunir les milliers de molécules que demande un astre.
     *
     * @return {@code true} si la molécule a été créée
     */
    public boolean createMolecule(String moleculeId) {
        return createMolecule(moleculeId, false);
    }

    /** {@link #createMolecule(String)}, en notant pour les statistiques si c'est l'automatisme qui crée. */
    boolean createMolecule(String moleculeId, boolean automatic) {
        if (!canCreateMolecule(moleculeId)) return false;
        Molecule molecule = molecule(moleculeId);
        int created = moleculesNextCreation(moleculeId);
        created += moleculeDoubling(moleculeId, created);
        created += moleculeUniverseBonus(moleculeId, created);
        long elements = 0;
        for (Map.Entry<Integer, Integer> atom : nextMoleculeCost(moleculeId).entrySet()) {
            state.setElementCount(atom.getKey(), state.elementCount(atom.getKey()) - atom.getValue());
            elements += atom.getValue();
        }
        state.addMolecules(molecule.id(), created);
        state.stats().noteMoleculeCreation(created, elements, automatic);
        noteStep(GameStats.Step.FIRST_MOLECULE);
        return true;
    }

    /**
     * Les molécules de plus que donnent les défis de Big Bang du deuxième univers (version 0.118) : la création en donne
     * ×2 au premier défi réussi, ×4 au troisième, ×8 au cinquième (version 0.119 : plafonné à ×8), pour le même prix
     * en éléments. Chacune prend sa place : sans assez d'espace libre, ou au plafond de la sorte, il en vient moins.
     */
    private int moleculeUniverseBonus(String moleculeId, int created) {
        double factor = universeMoleculeFactor();
        if (factor <= 1) return 0;
        Molecule molecule = molecule(moleculeId);
        double wanted = created * (factor - 1);
        double room = state.space().multiply(1 + 1e-9).subtract(usedSpace()).toDouble() / volumeOf(molecule) - created;
        long caps = (long) MAX_MOLECULES - state.moleculeCount(molecule.id()) - created;
        return (int) Math.max(0, Math.min(wanted, Math.min(Math.floor(room), caps)));
    }

    /** Ce que les défis de Big Bang du deuxième univers multiplient aux molécules d'une création : ×2, ×4, ×8 aux défis 1, 3 et 5. */
    public double universeMoleculeFactor() {
        int done = universeBangChallenges();
        return done <= 0 ? 1 : Math.pow(BANG_UNIVERSE_MOLECULES, (done + 1) / 2);
    }

    /** Chance qu'une création de molécule en donne deux fois plus : le dédoublement du collisionneur (5 % par niveau). */
    public double moleculeDoublingChance() {
        return 0.05 * colliderUpgradeLevel(ColliderUpgrade.MOLECULES);
    }

    /**
     * Les molécules de plus que donne le dédoublement à cette création : autant qu'elle en crée,
     * dans la limite de l'espace libre et des plafonds ; 0 s'il ne joue pas cette fois. Le hasard est
     * tiré du nombre de créations, sans toucher à celui de la synthèse.
     */
    private int moleculeDoubling(String moleculeId, int created) {
        double chance = moleculeDoublingChance();
        if (chance <= 0) return 0;
        double mix = Math.sin(state.stats().moleculeCreations() * 12.9898 + 61 * 78.233) * 43758.5453;
        if (mix - Math.floor(mix) >= chance) return 0;
        Molecule molecule = molecule(moleculeId);
        double room = state.space().multiply(1 + 1e-9).subtract(usedSpace()).toDouble() / volumeOf(molecule) - created;
        long caps = (long) MAX_MOLECULES - state.moleculeCount(molecule.id()) - created;
        return (int) Math.max(0, Math.min(created, Math.min(Math.floor(room), caps)));
    }

    /**
     * Nombre de molécules qu'une création ajoute à cette sorte quand la place ne manque pas : une
     * seule tant que la sorte n'est pas rassemblée ; ensuite une, plus la racine carrée du nombre
     * de molécules de l'amas (4 pour un amas de 9, 11 pour un amas de 100, 101 pour un amas de
     * 10 000). Plus un amas est gros, plus il attire ; et le palier du troisième Big Bang double
     * cette racine ({@link #accretionFactor()}).
     */
    public int moleculesPerCreation(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        int count = state.moleculeCount(molecule.id());
        if (!molecule.hasState() || !state.hasSubstance(molecule.id())) return 1;
        int drawn = 1 + (int) Math.floor(accretionFactor() * Math.sqrt(count) + 1e-9);
        return Math.max(1, Math.min(drawn, MAX_MOLECULES - count));
    }

    /**
     * Nombre de molécules que la prochaine création ajouterait vraiment : {@link #moleculesPerCreation(String)},
     * ou moins si l'espace libre ne suffit pas à toutes. Au moins une, même quand il n'y a de place pour aucune :
     * c'est {@link #canCreateMolecule(String)} qui dit si la création est possible.
     */
    public int moleculesNextCreation(String moleculeId) {
        int drawn = moleculesPerCreation(moleculeId);
        if (drawn == 1) return 1;
        // Même marge infime que pour l'espace libre : ce qui tient tout juste doit tenir.
        double room = state.space().multiply(1 + 1e-9).subtract(usedSpace()).toDouble() / volumeOf(molecule(moleculeId));
        return (int) Math.max(1, Math.min(drawn, Math.floor(room)));
    }

    /**
     * Ce par quoi l'acte du Big Bang multiplie une grandeur : {@code 1 + la somme} des bonus des
     * molécules sur cette grandeur ({@link Molecule.Boost}), chaque sorte comptée pour
     * {@link #effectiveMolecules(String)}, plus ce que donnent leurs rassemblements, les
     * assemblages formés ({@link Assembly#boost()}) et les astres formés ({@link Body#boost()}) ;
     * le tout multiplié par les améliorations d'espace qui portent sur cette grandeur
     * ({@link #spaceUpgradeBoost(Molecule.Stat)}) et par les paliers de Big Bang atteints
     * ({@link #bigBangMilestoneBoost(Molecule.Stat)}). Vaut 1 sans molécule, amélioration ni palier.
     */
    public double moleculeBoost(Molecule.Stat stat) {
        refreshMoleculeBonuses();
        return matterBoost(stat) * upgradeBoosts.getOrDefault(stat, 1.0) * bigBangMilestoneBoost(stat) * collectionBoost(stat)
                * skinBoost();
    }

    /**
     * La part de {@link #moleculeBoost(Molecule.Stat)} qui vient de la matière elle-même : les
     * molécules créées, les assemblages, les astres et la galaxie, sans les améliorations d'espace
     * ni les paliers de Big Bang. Vaut 1 tant que rien n'est créé.
     */
    public double matterBoost(Molecule.Stat stat) {
        // Dans l'univers vide, la matière ne multiplie plus rien.
        if (bangRuled(BangChallenge.VOID)) return 1;
        refreshMoleculeBonuses();
        return (1 + moleculeBoosts.getOrDefault(stat, 0.0)) * cosmosFactor() * Math.pow(1 + COSMOS_MILESTONE_GAIN, cosmosMilestones());
    }

    /** Ce par quoi la galaxie, l'amas et l'univers formés multiplient toutes les grandeurs de la matière (version 0.068). */
    public double cosmosFactor() {
        double factor = 1;
        for (Cosmos scale : Cosmos.values()) if (hasCosmos(scale)) factor *= scale.factor();
        return factor;
    }

    /**
     * Ce qu'ajoute chaque jalon du cosmos ({@link #cosmosMilestones()}) à toutes les grandeurs de la
     * matière : +10 %, cumulés en multipliant. Toutes, et pas seulement l'espace : en fin de partie,
     * l'espace abonde, ce sont les éléments des explosions qui manquent.
     */
    public static final double COSMOS_MILESTONE_GAIN = 0.10;

    /**
     * Les jalons de la fin de partie (version 0.068) : chaque état de la matière réuni pour l'amas de
     * galaxies, puis pour l'univers, une fois l'échelle en vue. Une échelle formée compte tous les
     * siens. Dix en tout.
     */
    public int cosmosMilestones() {
        int count = 0;
        for (Cosmos scale : Cosmos.values()) {
            if (scale == Cosmos.GALAXY) continue;
            if (hasCosmos(scale)) {
                count += scale.matter().size();
                continue;
            }
            if (!isCosmosUnlocked(scale)) continue;
            for (Map.Entry<Molecule.State, Integer> need : cosmosMatter(scale).entrySet()) {
                if (gatheredInState(need.getKey()) >= need.getValue()) count++;
            }
        }
        return count;
    }

    /** Vrai si cet état de la matière est réuni pour cette échelle du cosmos : un jalon (version 0.068). */
    public boolean isCosmosMilestoneMet(Cosmos scale, Molecule.State matter) {
        Integer need = cosmosMatter(scale).get(matter);
        return need != null && (hasCosmos(scale) || isCosmosUnlocked(scale) && gatheredInState(matter) >= need);
    }

    // ------------------------------------------------------------------
    // Le meilleur achat : ce qu'une création rapporte pour l'espace qu'elle prend
    // ------------------------------------------------------------------

    /** Pour combien compterait cette sorte avec {@code count} molécules, rassemblée ou non comme elle l'est aujourd'hui. */
    private double effectiveAt(Molecule molecule, int count) {
        double doublings = doublings(count);
        if (!molecule.hasState() || !state.hasSubstance(molecule.id())) return doublings;
        return Math.pow(doublings, molecule.state().exponent());
    }

    /**
     * Ce que la prochaine création dans cette sorte ajouterait aux grandeurs du jeu, en proportion
     * de ce qu'elles valent : 0,02 pour « +2 % ». Les gains sur plusieurs grandeurs s'ajoutent (le
     * bonus de la molécule, et ce que son état ajoute une fois rassemblée). Une molécule qui relève
     * un plafond d'exemplaires ne multiplie rien : elle vaut 0 ici.
     */
    public double moleculeGain(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        if (isMoleculeKindFull(moleculeId)) return 0;     // une sorte pleine ne peut plus rien rapporter
        int count = state.moleculeCount(molecule.id());
        int after = count + moleculesPerCreation(moleculeId);
        double more = effectiveAt(molecule, after) - effectiveAt(molecule, count);
        if (more <= 0) return 0;
        refreshMoleculeBonuses();
        double gain = 0;
        if (molecule.bonus() instanceof Molecule.Boost boost) {
            gain += boost.perMolecule() * more / (1 + moleculeBoosts.getOrDefault(boost.stat(), 0.0));
        }
        if (molecule.hasState() && state.hasSubstance(molecule.id())) {
            Molecule.State matter = molecule.state();
            gain += matter.particles() * more / (1 + moleculeBoosts.getOrDefault(Molecule.Stat.PARTICLES, 0.0));
            gain += matter.atoms() * more / (1 + moleculeBoosts.getOrDefault(Molecule.Stat.ATOMS, 0.0));
        }
        return gain;
    }

    /** Espace que prendrait la prochaine création dans cette sorte, quand la place ne manque pas : le volume de toutes les molécules qu'elle ajoute. */
    public BigNum moleculeNextSpace(String moleculeId) {
        return BigNum.of(volumeOf(molecule(moleculeId)) * moleculesPerCreation(moleculeId));
    }

    /**
     * Le rendement d'une sorte : ce que sa prochaine création rapporte ({@link #moleculeGain(String)})
     * par unité d'espace qu'elle prend ({@link #moleculeNextSpace(String)}). C'est ce qui permet
     * de comparer deux molécules : la meilleure est celle dont le rendement est le plus haut.
     */
    public double moleculeYield(String moleculeId) {
        double space = moleculeNextSpace(moleculeId).toDouble();
        return space > 0 ? moleculeGain(moleculeId) / space : 0;
    }

    /**
     * Le meilleur achat du moment : parmi les molécules qui peuvent être créées maintenant, celle
     * dont le rendement ({@link #moleculeYield(String)}) est le plus haut. {@code null} si aucune
     * création possible ne rapporte quoi que ce soit.
     */
    public Molecule bestMolecule() {
        if (!state.started() || !isBigBangUnlocked()) return null;
        Molecule best = null;
        double bestYield = 0;
        for (Molecule molecule : molecules.values()) {
            if (!isMoleculeKindUnlocked(molecule.kind())) continue;
            double yield = moleculeYield(molecule.id());
            if (yield <= bestYield || !canCreateMolecule(molecule.id())) continue;
            best = molecule;
            bestYield = yield;
        }
        return best;
    }

    /** La liste de courses d'un astre : les ingrédients de ses assemblages pas encore formés, ses molécules, sa matière. */
    ShoppingList shoppingFor(Body body) {
        Map<String, int[]> wanted = new LinkedHashMap<>();       // sorte → {à avoir en tout, 1 s'il faut la rassembler}
        Map<String, String> purposes = new java.util.HashMap<>();
        List<Assembly> ready = new ArrayList<>();
        for (String id : body.assemblies()) {
            if (state.hasAssembly(id)) continue;
            Assembly assembly = assemblies.get(id);
            if (canFormAssembly(id)) ready.add(assembly);
            else wantAssembly(assembly, wanted, purposes);
        }
        for (Map.Entry<String, Integer> needed : bodyMolecules(body).entrySet()) {
            int[] want = wanted.computeIfAbsent(needed.getKey(), each -> new int[2]);
            want[0] = Math.max(want[0], needed.getValue());
            purposes.putIfAbsent(needed.getKey(), "");
        }
        Map<Molecule.State, Integer> matter = new java.util.EnumMap<>(Molecule.State.class);
        for (Map.Entry<Molecule.State, Integer> needed : bodyMatter(body).entrySet()) {
            int missing = (int) Math.max(0, needed.getValue() - gatheredInState(needed.getKey()));
            if (missing > 0) matter.put(needed.getKey(), missing);
        }
        return new ShoppingList(ShoppingList.Kind.BODY, body.name(), canFormBody(body.id()), shoppingItems(wanted, purposes), matter, ready);
    }

    /**
     * Ajoute à la liste ce que demande un assemblage : chaque ingrédient doit être rassemblé, et
     * il en faut le nombre demandé hors de tout autre assemblage, donc en plus de ce que les
     * assemblages déjà formés (et ceux déjà comptés dans cette liste) ont pris.
     */
    void wantAssembly(Assembly assembly, Map<String, int[]> wanted, Map<String, String> purposes) {
        for (Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
            String id = ingredient.getKey();
            int[] want = wanted.computeIfAbsent(id, each -> new int[] {assembledMolecules(each), 0});
            want[0] = Math.max(want[0], assembledMolecules(id)) + ingredient.getValue();
            want[0] = Math.max(want[0], SUBSTANCE_MOLECULES);
            want[1] = 1;
            purposes.merge(id, assembly.name(), (before, name) -> before.isEmpty() || before.contains(name) ? name : before + ", " + name);
        }
    }

    List<ShoppingList.Item> shoppingItems(Map<String, int[]> wanted, Map<String, String> purposes) {
        List<ShoppingList.Item> items = new ArrayList<>();
        for (Map.Entry<String, int[]> want : wanted.entrySet()) {
            Molecule molecule = molecules.get(want.getKey());
            if (molecule == null) continue;
            int owned = state.moleculeCount(molecule.id());
            boolean gather = want.getValue()[1] == 1 && !state.hasSubstance(molecule.id());
            if (owned >= want.getValue()[0] && !gather) continue;
            items.add(new ShoppingList.Item(molecule, owned, Math.max(owned, want.getValue()[0]), gather,
                    purposes.getOrDefault(molecule.id(), "")));
        }
        return items;
    }

    /**
     * Acquiert une amélioration d'espace dont le seuil est atteint. Le jeu le fait de lui-même à
     * chaque {@link #tick(double)} : cette méthode ne sert qu'à l'acquérir sans attendre. Rien n'est
     * dépensé : c'est l'espace gagné qui l'ouvre, et il reste entièrement disponible pour les
     * molécules et leurs rassemblements. Elle est définitive, ni l'explosion ni le Big Bang ne la
     * reprennent.
     *
     * @return {@code true} si l'amélioration vient d'être acquise
     */
    public boolean buySpaceUpgrade(String spaceUpgradeId) {
        if (!canBuySpaceUpgrade(spaceUpgradeId)) return false;
        state.addSpaceUpgrade(spaceUpgrade(spaceUpgradeId).id());
        return true;
    }

    // ------------------------------------------------------------------
    // Les états de la matière : gaz, liquides, solides
    // ------------------------------------------------------------------

    /** Vrai une fois acquise l'amélioration d'espace qui ouvre le rassemblement des molécules ({@link SpaceUpgrade.OpenStates}). */
    public boolean isStatesUnlocked() {
        if (!isBigBangUnlocked()) return false;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.OpenStates && state.ownsSpaceUpgrade(upgrade.id())) return true;
        }
        return false;
    }

    /** Vrai si les molécules de cette sorte sont rassemblées en leur substance : un gaz, un liquide ou un solide. */
    public boolean hasSubstance(String moleculeId) {
        return state.hasSubstance(molecule(moleculeId).id());
    }

    /**
     * Espace que réserve le lieu où se rassemblent les molécules de cette sorte : le volume des
     * {@link #SUBSTANCE_MOLECULES} molécules qu'il faut pour l'ouvrir ({@link #moleculeVolume(String)},
     * donc bien plus pour un gaz que pour un cristal). Il se paie une seule fois, et ne grandit pas
     * avec le nombre de molécules rassemblées.
     *
     * @throws IllegalArgumentException si ces molécules ne se rassemblent pas
     */
    public BigNum substanceSpace(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        if (!molecule.hasState()) throw new IllegalArgumentException(moleculeId + " ne forme pas de substance");
        return BigNum.of(volumeOf(molecule) * SUBSTANCE_MOLECULES);
    }

    /**
     * Vrai si les molécules de cette sorte peuvent être rassemblées maintenant : le rassemblement
     * est ouvert, elles ont un état, elles ne sont pas déjà rassemblées, le joueur en possède
     * {@link #SUBSTANCE_MOLECULES}, et l'espace libre suffit à leur lieu ({@link #substanceSpace(String)}).
     */
    public boolean canFormSubstance(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        if (!state.started() || !isStatesUnlocked() || !molecule.hasState()) return false;
        if (state.hasSubstance(molecule.id())) return false;
        if (state.moleculeCount(molecule.id()) < SUBSTANCE_MOLECULES) return false;
        return hasFreeSpace(substanceSpace(moleculeId));
    }

    /**
     * Rassemble les molécules d'une sorte en leur substance : un gaz, un liquide ou un solide,
     * selon la molécule ({@link Molecule#state()}). C'est un achat unique par sorte de molécule :
     * il réserve un lieu dans l'espace ({@link #substanceSpace(String)}), et toutes les molécules
     * de la sorte s'y rangent, celles d'aujourd'hui comme celles qui seront créées ensuite.
     *
     * <p>Aucune molécule n'est consommée : elles gardent leur place. Le rassemblement fait trois
     * choses. La sorte compte davantage dans les bonus, à l'exposant de son état
     * ({@link #effectiveMolecules(String)}) ; elle ajoute sa part de particules ou d'atomes ; et
     * son amas attire la matière : chaque création y ajoute plus d'une molécule
     * ({@link #moleculesPerCreation(String)}). Le rassemblement est définitif.
     *
     * @return {@code true} si les molécules ont été rassemblées
     */
    public boolean formSubstance(String moleculeId) {
        if (!canFormSubstance(moleculeId)) return false;
        state.addSubstance(molecule(moleculeId).id());
        noteStep(GameStats.Step.FIRST_GATHERING);
        return true;
    }

    /** Nombre de molécules de cette sorte rangées dans leur lieu de rassemblement : toutes si elles sont rassemblées, aucune sinon. */
    public int gatheredMolecules(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        return molecule.hasState() && state.hasSubstance(molecule.id()) ? state.moleculeCount(molecule.id()) : 0;
    }

    /**
     * Pour combien cette sorte compte dans les bonus : le nombre de fois qu'elle a doublé
     * ({@link #doublings(int)}), élevé à la puissance de son état quand elle est rassemblée. Trois
     * molécules d'un liquide (deux doublements, exposant 1,25) comptent pour 2,38 ; mille (dix
     * doublements), pour 17,7. Les premières molécules d'une sorte comptent donc beaucoup, les
     * suivantes de moins en moins : il vaut mieux beaucoup de sortes qu'une seule énorme.
     */
    public double effectiveMolecules(String moleculeId) {
        Molecule molecule = molecule(moleculeId);
        double doublings = doublings(state.moleculeCount(molecule.id()));
        if (!molecule.hasState() || !state.hasSubstance(molecule.id())) return doublings;
        return Math.pow(doublings, molecule.state().exponent());
    }

    /**
     * Le nombre de doublements d'un amas de {@code count} molécules : 1 pour une molécule, 2 pour
     * trois, 3 pour sept, 4 pour quinze, et ainsi de suite ; entre deux, la valeur glisse de l'un à
     * l'autre. C'est le logarithme en base deux de {@code count + 1}.
     */
    public static double doublings(int count) {
        return count <= 0 ? 0 : Math.log1p(count) / Math.log(2);
    }

    /** Nombre de sortes de molécules rassemblées dans le lieu de leur état. */
    public int substancesFormed() {
        return state.substances().size();
    }

    // ------------------------------------------------------------------
    // Les assemblages : roches, minerais, pierres précieuses, eaux, gaz, hydrocarbures
    // ------------------------------------------------------------------

    /** Vrai une fois acquise l'amélioration d'espace qui ouvre les assemblages ({@link SpaceUpgrade.OpenAssemblies}). */
    public boolean isAssembliesUnlocked() {
        if (!isBigBangUnlocked()) return false;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.OpenAssemblies && state.ownsSpaceUpgrade(upgrade.id())) return true;
        }
        return false;
    }

    /** Tous les assemblages, dans l'ordre du catalogue ({@link Assemblies}). */
    public List<Assembly> assemblies() {
        if (assemblyList == null) assemblyList = List.copyOf(assemblies.values());
        return assemblyList;
    }

    /**
     * L'assemblage de cet identifiant.
     *
     * @throws IllegalArgumentException si l'identifiant est inconnu
     */
    public Assembly assembly(String assemblyId) {
        Assembly assembly = assemblies.get(assemblyId);
        if (assembly == null) throw new IllegalArgumentException("Assemblage inconnu : " + assemblyId);
        return assembly;
    }

    /** Vrai si cet assemblage est formé. */
    public boolean hasAssembly(String assemblyId) {
        return state.hasAssembly(assembly(assemblyId).id());
    }

    /** Nombre d'assemblages formés. */
    public int assembliesFormed() {
        return state.assemblies().size();
    }

    /**
     * Nombre de molécules de cette sorte entrées dans des assemblages : la somme de ce qu'en
     * demande chaque assemblage formé. Elles ne sont plus dans l'amas de leur sorte, et aucun
     * autre assemblage ne peut compter dessus.
     */
    public int assembledMolecules(String moleculeId) {
        String id = molecule(moleculeId).id();
        int assembled = 0;
        for (String formed : state.assemblies()) {
            Assembly assembly = assemblies.get(formed);
            if (assembly != null) assembled += assembly.ingredients().getOrDefault(id, 0);
        }
        return assembled;
    }

    /**
     * Nombre de molécules rassemblées de cette sorte qui ne sont dans aucun assemblage : ce qui
     * reste dans leur amas, et ce qu'un nouvel assemblage peut prendre. Nul si elles ne sont pas rassemblées.
     */
    public int spareMolecules(String moleculeId) {
        return Math.max(0, gatheredMolecules(moleculeId) - assembledMolecules(moleculeId));
    }

    /**
     * Vrai si cet assemblage peut être formé maintenant : les assemblages sont ouverts, il n'est
     * pas déjà formé, et chacun de ses ingrédients est rassemblé, avec assez de molécules encore
     * hors de tout assemblage ({@link #spareMolecules(String)}).
     */
    public boolean canFormAssembly(String assemblyId) {
        Assembly assembly = assembly(assemblyId);
        if (!state.started() || !isAssembliesUnlocked() || state.hasAssembly(assembly.id())) return false;
        for (Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
            if (spareMolecules(ingredient.getKey()) < ingredient.getValue()) return false;
        }
        return true;
    }

    /**
     * Forme un assemblage : c'est un achat unique, qui ne consomme rien. Les molécules qu'il
     * demande quittent l'amas de leur sorte pour entrer dans son bloc ; elles comptent toujours
     * comme des molécules de leur sorte, avec leur bonus et celui de leur rassemblement. En plus,
     * l'assemblage augmente la grandeur de sa famille ({@link Assembly#boost()}). Il est
     * définitif : ni l'explosion ni le Big Bang ne le défont.
     *
     * @return {@code true} si l'assemblage a été formé
     */
    public boolean formAssembly(String assemblyId) {
        if (!canFormAssembly(assemblyId)) return false;
        state.addAssembly(assembly(assemblyId).id());
        noteStep(GameStats.Step.FIRST_ASSEMBLY);
        return true;
    }

    // ------------------------------------------------------------------
    // Les astres : amas de roches, comètes, astéroïdes, lunes, planètes
    // ------------------------------------------------------------------

    /** Vrai une fois acquise l'amélioration d'espace qui ouvre les astres ({@link SpaceUpgrade.OpenBodies}). */
    public boolean isBodiesUnlocked() {
        if (!isBigBangUnlocked()) return false;
        for (SpaceUpgrade upgrade : spaceUpgrades.values()) {
            if (upgrade.effect() instanceof SpaceUpgrade.OpenBodies && state.ownsSpaceUpgrade(upgrade.id())) return true;
        }
        return false;
    }

    /** Tous les astres, du plus petit au plus grand ({@link Bodies}). */
    public List<Body> bodies() {
        if (bodyList == null) bodyList = List.copyOf(bodies.values());
        return bodyList;
    }

    /**
     * L'astre de cet identifiant.
     *
     * @throws IllegalArgumentException si l'identifiant est inconnu
     */
    public Body body(String bodyId) {
        Body body = bodies.get(bodyId);
        if (body == null) throw new IllegalArgumentException("Astre inconnu : " + bodyId);
        return body;
    }

    /** Vrai si cet astre est formé. */
    public boolean hasBody(String bodyId) {
        return state.hasBody(body(bodyId).id());
    }

    /** Nombre d'astres formés. */
    public int bodiesFormed() {
        return state.bodies().size();
    }

    /**
     * L'astre dont cet assemblage fait partie : celui qui en est fait, dans le catalogue. Il n'y en
     * a qu'un, ou aucun ({@code null}). L'astre peut ne pas être formé encore ({@link #hasBody(String)}).
     */
    public Body bodyOf(String assemblyId) {
        return bodyByAssembly.get(assembly(assemblyId).id());
    }

    /**
     * Nombre de molécules rassemblées dans cet état, toutes sortes confondues, qu'elles soient
     * dans leur amas ou dans un assemblage : c'est la matière que demande un astre ({@link Body#matter()}).
     */
    public long gatheredInState(Molecule.State matter) {
        if (gatheredByStateVersion != state.moleculesVersion()) {
            gatheredByStateVersion = state.moleculesVersion();
            gatheredByState.clear();
            for (String id : state.substances()) {
                Molecule molecule = molecules.get(id);
                if (molecule != null && molecule.hasState()) gatheredByState.merge(molecule.state(), (long) state.moleculeCount(id), Long::sum);
            }
        }
        return gatheredByState.getOrDefault(matter, 0L);
    }

    /** Nombre de conditions de cet astre déjà réunies, sur {@link #bodyConditions(String)}. */
    public int bodyConditionsMet(String bodyId) {
        Body body = body(bodyId);
        int met = 0;
        for (String needed : body.bodies()) {
            if (isBodyMature(needed)) met++;
        }
        for (String needed : body.assemblies()) {
            if (state.hasAssembly(needed)) met++;
        }
        for (Map.Entry<Molecule.State, Integer> needed : bodyMatter(body).entrySet()) {
            if (gatheredInState(needed.getKey()) >= needed.getValue()) met++;
        }
        for (Map.Entry<String, Integer> needed : bodyMolecules(body).entrySet()) {
            if (state.moleculeCount(needed.getKey()) >= needed.getValue()) met++;
        }
        if (needsColliderEnergy(body) && hasColliderEnergyFor(body)) met++;
        return met;
    }

    /**
     * Vrai si cet astre peut être formé maintenant : les astres sont ouverts, il n'est pas déjà
     * formé, et toutes ses conditions sont réunies ({@link #bodyConditionsMet(String)}) : les
     * assemblages dont il est fait et ses astres plus petits sont formés, sa matière est rassemblée,
     * ses molécules sont créées.
     */
    public boolean canFormBody(String bodyId) {
        Body body = body(bodyId);
        return state.started() && !bangRuled(BangChallenge.FROZEN) && isBodiesUnlocked() && !state.hasBody(body.id())
                && bodyConditionsMet(bodyId) == bodyConditions(bodyId) && hasMaturationPlace()
                && (body.tier() != Body.Tier.EXOTIC || isExoticUnlocked());
    }

    /**
     * Forme un astre : c'est un achat unique. Ses assemblages entrent dans l'astre et en font le
     * corps ({@link #bodyOf(String)}) ; ils restent formés et gardent leur bonus. Le reste de ce
     * qu'il demande n'est ni consommé ni réservé, et peut servir à d'autres astres. Une fois formé,
     * il augmente sa grandeur de ce que vaut son échelle ({@link Body#boost()}). Il est définitif :
     * ni l'explosion ni le Big Bang ne le défont.
     *
     * @return {@code true} si l'astre a été formé
     */
    public boolean formBody(String bodyId) {
        if (!canFormBody(bodyId)) return false;
        state.addBody(body(bodyId).id());
        state.setMaturing(body(bodyId).id(), maturationSeconds(body(bodyId)));
        noteStep(GameStats.Step.FIRST_BODY);
        return true;
    }

    // ------------------------------------------------------------------
    // La galaxie : tous les astres, rassemblés autour du trou noir supermassif
    // ------------------------------------------------------------------

    /** Ce par quoi la galaxie multiplie chacune des grandeurs une fois formée ({@link Cosmos#factor()}, ×4 depuis la 0.068). */
    public static final double GALAXY_FACTOR = Cosmos.GALAXY.factor();

    /** Nombre d'astres de cette échelle dans le catalogue. */
    public int bodiesIn(Body.Tier tier) {
        int count = 0;
        for (Body body : bodies.values()) {
            if (body.tier() == tier) count++;
        }
        return count;
    }

    /** Nombre d'astres formés de cette échelle. */
    public int bodiesFormed(Body.Tier tier) {
        int count = 0;
        for (String id : state.bodies()) {
            Body body = bodies.get(id);
            if (body != null && body.tier() == tier) count++;
        }
        return count;
    }

    /**
     * Vrai une fois la galaxie en vue : les astres sont ouverts et une première étoile est formée,
     * ou la galaxie l'est déjà.
     */
    public boolean isGalaxyUnlocked() {
        if (!isBodiesUnlocked()) return false;
        if (state.hasGalaxy()) return true;
        for (String id : state.bodies()) {
            Body body = bodies.get(id);
            if (body != null && body.tier().ordinal() >= Body.Tier.STAR.ordinal()) return true;
        }
        return false;
    }

    /** Vrai si la galaxie est formée. */
    public boolean hasGalaxy() {
        return state.hasGalaxy();
    }

    /**
     * Vrai si la galaxie peut être formée maintenant : les astres sont ouverts, elle ne l'est pas
     * déjà, et tous les astres du catalogue sont formés, du premier amas de roches au trou noir
     * supermassif.
     */
    public boolean canFormGalaxy() {
        return state.started() && isBodiesUnlocked() && !state.hasGalaxy() && bodiesMature() == bodies.size()
                && solarSystemsComplete() >= SYSTEMS_FOR_GALAXY;
    }

    /**
     * Forme la galaxie : tout ce que le joueur a créé, molécules, amas, assemblages et astres, se
     * met à tourner autour du trou noir supermassif. C'est un achat unique, qui ne consomme rien :
     * les astres restent formés et gardent leur bonus. La galaxie multiplie par {@link #GALAXY_FACTOR}
     * chacune des grandeurs que les molécules augmentent, et rien ne la défait, ni l'explosion ni
     * le Big Bang.
     *
     * @return {@code true} si la galaxie a été formée
     */
    public boolean formGalaxy() {
        return formCosmos(Cosmos.GALAXY);
    }
}

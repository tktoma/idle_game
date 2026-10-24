package idle.core;

import idle.core.GameUniverse.VoidUpgrade;
import idle.core.achievements.Achievement;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Le vide (version 0.129), deuxième acte de la partie secondaire, qui se joue dans les anciens onglets à partir du
 * premier Big Bang du troisième univers.
 *
 * <p>Il se propose dans le collisionneur ({@link #isVoidOffered()}) et s'ouvre contre {@link #VOID_UNLOCK_COST}
 * d'énergie du collisionneur. Ouvert, le collisionneur y fait naître des paires de particules virtuelles : chacune
 * coûte de l'énergie du collisionneur et de l'espace libre, occupe cet espace tant qu'elle vit
 * ({@link #VOID_PAIR_LIFE} secondes), et donne de l'énergie du vide à sa naissance. L'énergie du vide fond de
 * {@link #VOID_DECAY} par seconde : elle tend vers un équilibre, le gain par seconde divisé par la fonte
 * ({@link #voidEquilibrium()}). La part du collisionneur et de l'espace libre que le vide peut prendre se règle
 * ({@link GameState#voidShare()}).
 *
 * <p>Le premier trou noir formé ouvre le trou noir du vide : les particules qu'on lui sacrifie lui donnent une masse
 * ({@link #voidMass()}) qui raccourcit la vie des paires (elles rendent leur espace plus vite) et ralentit la fonte.
 * L'énergie du vide paie sept améliorations ({@link VoidUpgrade}), dont l'allègement des besoins du ciel et la
 * maturation plus courte des astres. À la
 * formation de l'univers, ce qu'il en reste s'ajoute au cumul, gardé ; le vide, lui, repart de zéro.
 */
public abstract class GameVoid extends GameBigBang {

    GameVoid(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog,
            List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /**
     * Le prix d'ouverture du vide : {@link #VOID_UNLOCK_SECONDS} secondes de production du collisionneur, comptées quand
     * il se propose (au premier Big Bang de l'univers), et jamais moins que {@link #VOID_UNLOCK_COST}. Le collisionneur
     * explose d'une partie à l'autre (de 10⁶ à 10⁹ par seconde au premier Big Bang) : un prix fixe serait presque
     * gratuit dans l'une, hors d'atteinte dans l'autre.
     */
    public static final double VOID_UNLOCK_COST = 1e9;
    public static final double VOID_UNLOCK_SECONDS = 600;
    /** Combien de secondes vit une paire sans trou noir. */
    public static final double VOID_PAIR_LIFE = 6;
    /** Paires par seconde sans amélioration de rythme : chaque niveau en ajoute 25 %. */
    public static final double VOID_BASE_RATE = 1;
    public static final double VOID_RATE_STEP = 1.25;
    /** Ce que coûte une paire : une demi-seconde de collisionneur, et un cinquième de seconde d'expansion, chacun −15 % par niveau. */
    public static final double VOID_PAIR_ENERGY_SECONDS = 0.5;
    public static final double VOID_PAIR_SPACE_SECONDS = 0.2;
    public static final double VOID_COST_STEP = 0.85;
    /** La fonte : 0,2 % par seconde (moitié en près de six minutes), −10 % par niveau. */
    public static final double VOID_DECAY = 0.002;
    public static final double VOID_DECAY_STEP = 0.9;
    /** La masse du trou noir au plus. */
    public static final double VOID_MAX_MASS = 15;

    /** Les paires vivantes : un compte continu, qui n'est pas sauvegardé (il se refait en quelques secondes). */
    private double voidPairs = 0;
    /** Les paires nées, en tout, depuis l'ouverture de la fenêtre : l'écume les dessine à mesure. */
    private double voidBorn = 0;

    // ------------------------------------------------------------------
    // L'ouverture
    // ------------------------------------------------------------------

    /** Ce que coûte l'ouverture du vide : le prix fixé quand il s'est proposé, ou celui qu'il aurait maintenant. */
    public double voidPrice() {
        return state.voidPrice() > 0 ? state.voidPrice() : Math.max(VOID_UNLOCK_COST, VOID_UNLOCK_SECONDS * colliderEnergyPerSecond());
    }

    public boolean canOpenVoid() {
        return isVoidOffered() && state.started() && colliderEnergy() >= voidPrice() * (1 - 1e-12);
    }

    /** Ouvre le vide contre son prix ({@link #voidPrice()}) en énergie du collisionneur. */
    public boolean openVoid() {
        if (!canOpenVoid()) return false;
        state.setColliderEnergy(Math.max(0, colliderEnergy() - voidPrice()));
        state.setVoidOpen(true);
        voidPairs = 0;
        return true;
    }

    /** Vrai une fois le trou noir du vide ouvert : le vide est ouvert et un trou noir a été formé dans cet univers. */
    public boolean isVoidHoleOpen() {
        return isVoidUnlocked() && state.voidHoleOpen();
    }

    // ------------------------------------------------------------------
    // Les paires
    // ------------------------------------------------------------------

    /** Combien de temps vit une paire : {@link #VOID_PAIR_LIFE} ÷ (1 + m/10), m la masse du trou noir. */
    public double voidPairLife() {
        return VOID_PAIR_LIFE / (1 + voidMass() / 10);
    }

    /** Les paires par seconde que le rythme permet, avant les limites de l'énergie et de l'espace. */
    public double voidBaseRate() {
        return VOID_BASE_RATE * Math.pow(VOID_RATE_STEP, voidLevel(VoidUpgrade.RATE));
    }

    /** L'énergie du collisionneur que coûte une paire : une fraction de seconde de sa production. */
    public double voidPairEnergy() {
        return VOID_PAIR_ENERGY_SECONDS * Math.pow(VOID_COST_STEP, voidLevel(VoidUpgrade.ENERGY)) * colliderEnergyPerSecond();
    }

    /** L'espace qu'occupe une paire tant qu'elle vit : une fraction de seconde d'expansion. */
    public BigNum voidPairSpace() {
        return steadySpacePerSecond().multiply(VOID_PAIR_SPACE_SECONDS * Math.pow(VOID_COST_STEP, voidLevel(VoidUpgrade.SPACE)));
    }

    /** L'énergie du vide que donne une paire à sa naissance : 1, plus 1 par niveau de gain. */
    public double voidPairGain() {
        return 1 + voidLevel(VoidUpgrade.GAIN);
    }

    /** Ce que la part réglée laisse au vide de la production du collisionneur : en paires par seconde. */
    public double voidEnergyRateLimit() {
        double share = state.voidShare();
        if (share <= 0 || colliderEnergyPerSecond() <= 0) return 0;
        return share / (VOID_PAIR_ENERGY_SECONDS * Math.pow(VOID_COST_STEP, voidLevel(VoidUpgrade.ENERGY)));
    }

    /** L'espace que les molécules et les lieux de rassemblement laissent : celui où les paires peuvent naître. */
    private BigNum spaceLeftForPairs() {
        BigNum molecules = occupiedSpace().add(reservedSpace());
        return state.space().gt(molecules) ? state.space().subtract(molecules) : BigNum.ZERO;
    }

    /** Les paires vivantes que la part réglée de l'espace libre peut tenir. */
    public double voidSpaceLimit() {
        BigNum each = voidPairSpace();
        if (each.sign() <= 0) return 0;
        return spaceLeftForPairs().multiply(state.voidShare()).divide(each).toDouble();
    }

    /** Les paires par seconde que l'espace permet : celles qu'il peut tenir, renouvelées à chaque vie. */
    public double voidSpaceRateLimit() {
        return voidSpaceLimit() / voidPairLife();
    }

    /** Les paires qui naissent par seconde : le rythme, borné par la part d'énergie et la part d'espace. Nul fermé. */
    public double voidRate() {
        if (!isVoidUnlocked() || !state.started()) return 0;
        return Math.max(0, Math.min(voidBaseRate(), Math.min(voidEnergyRateLimit(), voidSpaceRateLimit())));
    }

    /** Ce qui borne le rythme en ce moment : « rythme », « énergie » ou « espace ». */
    public String voidLimit() {
        double base = voidBaseRate(), energy = voidEnergyRateLimit(), space = voidSpaceRateLimit();
        if (energy <= base && energy <= space) return "énergie";
        if (space <= base) return "espace";
        return "rythme";
    }

    /** Les paires vivantes en ce moment. */
    public double voidPairs() {
        return voidPairs;
    }

    /** Les paires nées depuis l'ouverture de la fenêtre : un compte qui ne fait que monter. */
    public double voidPairsBorn() {
        return voidBorn;
    }

    /** L'espace que tiennent les paires vivantes : il compte dans l'espace utilisé. */
    @Override
    public BigNum voidHeldSpace() {
        if (voidPairs <= 0) return BigNum.ZERO;
        return voidPairSpace().multiply(voidPairs);
    }

    // ------------------------------------------------------------------
    // L'énergie du vide
    // ------------------------------------------------------------------

    /** L'énergie du vide gagnée par seconde : les paires qui naissent, fois leur gain. */
    public double voidGainPerSecond() {
        return voidRate() * voidPairGain();
    }

    /** La fonte, par seconde : {@link #VOID_DECAY}, −10 % par niveau, divisée par (1 + m/5). */
    public double voidDecay() {
        return VOID_DECAY * Math.pow(VOID_DECAY_STEP, voidLevel(VoidUpgrade.DECAY)) / (1 + voidMass() / 5);
    }

    /** L'équilibre : là où la fonte reprend autant que les paires donnent. */
    public double voidEquilibrium() {
        return voidGainPerSecond() / voidDecay();
    }

    /** Ce qu'il reste d'énergie du vide de cet univers, et le cumul de ce qui restait à la formation des précédents. */
    public BigNum voidEnergy() {
        return state.voidEnergy();
    }

    public double voidAccumulated() {
        return state.voidAccumulated();
    }

    /**
     * Fait passer le temps du vide : les paires naissent au rythme permis, payent leur énergie, vivent et rendent
     * leur espace ; l'énergie du vide monte vers son équilibre. Exact quelle que soit la durée : une absence donne
     * ce que le jeu ouvert aurait donné.
     */
    void runVoid(double dt) {
        // Le prix se fixe la première fois que le vide se propose : il ne monte plus ensuite avec le collisionneur.
        if (isVoidOffered() && state.voidPrice() <= 0) state.setVoidPrice(voidPrice());
        if (isVoidUnlocked() && !state.voidHoleOpen() && hasBody("black_hole")) state.setVoidHoleOpen(true);
        if (!isVoidUnlocked() || !state.started() || dt <= 0) {
            voidPairs = 0;
            return;
        }
        double rate = voidRate();
        // L'énergie : payée naissance par naissance ; une réserve trop maigre ralentit les naissances.
        double cost = voidPairEnergy();
        if (cost > 0 && rate * cost * dt > colliderEnergy()) rate = colliderEnergy() / (cost * dt);
        if (cost > 0) state.setColliderEnergy(Math.max(0, colliderEnergy() - rate * cost * dt));
        double life = voidPairLife();
        double steady = rate * life;
        voidPairs = steady + (voidPairs - steady) * Math.exp(-dt / life);
        // Moins d'espace qu'avant (un Big Bang, des molécules) : les paires en trop s'annulent tout de suite.
        voidPairs = Math.max(0, Math.min(voidPairs, voidSpaceLimit()));
        voidBorn += rate * dt;
        double gain = rate * voidPairGain();
        double decay = voidDecay();
        double stock = state.voidEnergy().toDouble();
        double target = gain / decay;
        state.setVoidEnergy(BigNum.of(Math.max(0, target + (stock - target) * Math.exp(-decay * dt))));
    }

    // ------------------------------------------------------------------
    // Le trou noir
    // ------------------------------------------------------------------

    /** La masse du trou noir : 2 × log₁₀(1 + log₁₀(1 + S)), S les particules sacrifiées, au plus {@link #VOID_MAX_MASS}. */
    public double voidMass() {
        return voidMassOf(state.voidSacrificed());
    }

    /** La masse que donneraient ces particules sacrifiées. */
    public static double voidMassOf(BigNum sacrificed) {
        if (sacrificed.sign() <= 0) return 0;
        double digits = sacrificed.compareTo(BigNum.of(1e300)) > 0 ? sacrificed.log10() : Math.log10(1 + sacrificed.toDouble());
        return Math.min(VOID_MAX_MASS, 2 * Math.log10(1 + digits));
    }

    /** La masse qu'aurait le trou noir si on lui sacrifiait maintenant toutes les particules. */
    public double voidMassAfterSacrifice() {
        return voidMassOf(state.voidSacrificed().add(state.particles()));
    }

    public boolean canSacrificeParticles() {
        return isVoidHoleOpen() && state.particles().sign() > 0 && voidMass() < VOID_MAX_MASS;
    }

    /** Sacrifie toutes les particules au trou noir : elles s'ajoutent à sa masse. */
    public boolean sacrificeParticles() {
        if (!canSacrificeParticles()) return false;
        state.setVoidSacrificed(state.voidSacrificed().add(state.particles()));
        state.setParticles(BigNum.ZERO);
        return true;
    }

    // ------------------------------------------------------------------
    // Les améliorations
    // ------------------------------------------------------------------

    /** Le prix du prochain niveau, en énergie du vide. */
    public double voidCost(VoidUpgrade upgrade) {
        return upgrade.baseCost() * Math.pow(upgrade.growth(), voidLevel(upgrade));
    }

    public boolean canBuyVoid(VoidUpgrade upgrade) {
        return isVoidUnlocked() && voidLevel(upgrade) < upgrade.max() && state.voidEnergy().toDouble() >= voidCost(upgrade) * (1 - 1e-9);
    }

    public boolean buyVoid(VoidUpgrade upgrade) {
        if (!canBuyVoid(upgrade)) return false;
        state.setVoidEnergy(BigNum.of(Math.max(0, state.voidEnergy().toDouble() - voidCost(upgrade))));
        double before = maturationShare();
        state.setVoidLevel(upgrade.ordinal(), voidLevel(upgrade) + 1);
        // La maturation plus courte vaut aussi pour les astres qui mûrissent déjà : leur reste raccourcit d'autant.
        if (upgrade == VoidUpgrade.MATURE && before > 0) {
            double ratio = maturationShare() / before;
            for (String id : List.copyOf(state.maturing().keySet())) {
                if (state.maturingLeft(id) > 0) state.setMaturing(id, state.maturingLeft(id) * ratio);
            }
        }
        return true;
    }

    /** Règle la part du collisionneur et de l'espace libre que le vide peut prendre, de 0 à 1. */
    public void setVoidShare(double share) {
        state.setVoidShare(share);
    }

    /** Outils d'essai : ouvrir le vide sans payer, lui donner de la masse. */
    public void testOpenVoid() {
        state.setVoidOpen(true);
    }

    public void testOpenVoidHole() {
        state.setVoidOpen(true);
        state.setVoidHoleOpen(true);
    }
}

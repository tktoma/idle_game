package idle.core;

import idle.core.GameUniverse.TreeBubble;
import idle.core.achievements.Achievement;
import idle.core.collider.Hadron;
import idle.core.collider.Quark;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Les hadrons (version 0.149) : un onglet où les gluons lient les quarks du collisionneur en protons et en neutrons,
 * puis en particules exotiques ({@link Hadron}).
 *
 * <p>La bulle de l'arbuste, au milieu, le fait se proposer dans le collisionneur, comme le vide : il s'ouvre contre
 * {@link #HADRONS_UNLOCK_SECONDS} secondes de production du collisionneur, jamais moins que
 * {@link #HADRONS_UNLOCK_COST}. La bulle du jeune arbre, à gauche, l'ouvre dès le début de chaque univers ; celle de
 * l'arbre ancien, à gauche, garde les hadrons d'un univers à l'autre ; celle du cœur de l'arbre-monde ouvre les
 * exotiques. Sans ces bulles, un nouvel univers les reprend, comme tout le reste. Ils n'ont pas encore d'effet.
 */
public abstract class GameHadrons extends GameVoid {

    GameHadrons(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog,
            List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** Le prix d'ouverture, au moins : celui du vide. */
    public static final double HADRONS_UNLOCK_COST = 1e9;
    /** Le prix d'ouverture : dix minutes de production du collisionneur, comptées quand l'onglet se propose. */
    public static final double HADRONS_UNLOCK_SECONDS = 600;
    /** Garde-fou : le plus de hadrons d'une sorte qu'on a. */
    public static final long MAX_HADRONS = Long.MAX_VALUE / 4;

    /** Vrai une fois l'onglet ouvert : payé dans cet univers, ou offert par la bulle du jeune arbre, à gauche. */
    public boolean isHadronsUnlocked() {
        return state.hadronsOpen() || hasBubble(TreeBubble.YOUNG_LEFT);
    }

    /** Vrai quand il se propose dans le collisionneur : la bulle de l'arbuste, au milieu, le collisionneur ouvert. */
    public boolean isHadronsOffered() {
        return hasBubble(TreeBubble.CROWN_MIDDLE) && isColliderUnlocked() && !isHadronsUnlocked();
    }

    /** Vrai une fois les particules exotiques ouvertes : la bulle du cœur de l'arbre-monde. */
    public boolean isExoticHadronsUnlocked() {
        return isHadronsUnlocked() && hasBubble(TreeBubble.HEART);
    }

    /** Ce que coûte l'ouverture : le prix fixé quand il s'est proposé, ou celui qu'il aurait maintenant. */
    public double hadronsPrice() {
        return state.hadronsPrice() > 0 ? state.hadronsPrice()
                : Math.max(HADRONS_UNLOCK_COST, HADRONS_UNLOCK_SECONDS * colliderEnergyPerSecond());
    }

    public boolean canOpenHadrons() {
        return isHadronsOffered() && state.started() && colliderEnergy() >= hadronsPrice() * (1 - 1e-12);
    }

    /** Ouvre l'onglet des hadrons contre son prix en énergie du collisionneur. */
    public boolean openHadrons() {
        if (!canOpenHadrons()) return false;
        state.setColliderEnergy(Math.max(0, colliderEnergy() - hadronsPrice()));
        state.setHadronsOpen(true);
        return true;
    }

    /** Vrai si ce hadron s'assemble : l'onglet ouvert, et les exotiques avec leur bulle. */
    public boolean isHadronAvailable(Hadron hadron) {
        return isHadronsUnlocked() && (!hadron.isExotic() || isExoticHadronsUnlocked());
    }

    /** Les hadrons de cette sorte qu'on a. */
    public long hadrons(Hadron hadron) {
        return state.hadrons(hadron);
    }

    /** Tous les hadrons assemblés, de toutes les sortes. */
    public long hadronsMade() {
        long total = 0;
        for (Hadron hadron : Hadron.values()) total += state.hadrons(hadron);
        return total;
    }

    /** Combien de ce hadron les quarks en main permettent d'assembler ; 0 s'il n'est pas ouvert. */
    public long hadronsBindable(Hadron hadron) {
        if (!isHadronAvailable(hadron) || !state.started()) return 0;
        long most = MAX_HADRONS - state.hadrons(hadron);
        for (Map.Entry<Quark, Integer> need : hadron.recipe().entrySet()) {
            most = Math.min(most, state.quarks(need.getKey()) / need.getValue());
        }
        return Math.max(0, most);
    }

    public boolean canBindHadron(Hadron hadron) {
        return hadronsBindable(hadron) > 0;
    }

    /**
     * Assemble {@code count} hadrons de cette sorte, ou moins si les quarks manquent : chacun prend ses quarks.
     *
     * @return combien ont été assemblés
     */
    public long bindHadrons(Hadron hadron, long count) {
        long made = Math.min(Math.max(0, count), hadronsBindable(hadron));
        if (made <= 0) return 0;
        for (Map.Entry<Quark, Integer> need : hadron.recipe().entrySet()) {
            state.setQuarks(need.getKey(), (int) (state.quarks(need.getKey()) - made * need.getValue()));
        }
        state.setHadrons(hadron, state.hadrons(hadron) + made);
        return made;
    }

    /** Assemble un hadron ; vrai si c'est fait. */
    public boolean bindHadron(Hadron hadron) {
        return bindHadrons(hadron, 1) == 1;
    }

    /** Assemble tous ceux que les quarks permettent ; combien. */
    public long bindAllHadrons(Hadron hadron) {
        return bindHadrons(hadron, hadronsBindable(hadron));
    }

    /** Le prix se fixe la première fois que l'onglet se propose : il ne monte plus ensuite avec le collisionneur. */
    void runHadrons() {
        if (isHadronsOffered() && state.hadronsPrice() <= 0) state.setHadronsPrice(hadronsPrice());
    }
}

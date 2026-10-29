package idle.core;

import idle.core.achievements.Achievement;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Le miroir, neuvième dimension (version 0.157) : un plateau coupé en deux par un miroir vertical. Un motif s'allume,
 * case après case, sur la moitié gauche ; puis il s'éteint, et le joueur doit le reproduire de mémoire, en miroir, sur
 * la moitié droite, dans le même ordre. Réussi, il donne des reflets (la ressource), et le motif suivant est plus long
 * d'une case ; manqué, le motif raccourcit d'une case. Améliorations ({@link MirrorUpgrade}) : gain, multiplicateur de
 * longueur, temps de mémorisation, seconde chance (des erreurs permises), plateau plus grand.
 *
 * <p>Avec le miroir viennent les saisons de la graine : une amélioration payée en reflets, qui fait remplir le chrono
 * à chaque arrosage, plus ou moins selon la saison.
 */
public abstract class GameMirror extends GameGrid {

    GameMirror(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog,
            List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** Les améliorations du miroir, dans l'ordre de la sauvegarde. */
    public enum MirrorUpgrade implements Priced {
        /** Chaque motif réussi donne un reflet de plus par case, par niveau. */
        GAIN(5, 3, Integer.MAX_VALUE),
        /** Chaque case au-delà de trois multiplie le gain : ×1,1, et 0,05 de plus par niveau. */
        LENGTH(8, 2.8, 20),
        /** Chaque case du motif reste allumée plus longtemps : +0,15 s par niveau. */
        MEMORY(6, 2.5, 15),
        /** Une erreur permise de plus par motif : 3 au plus. */
        MISTAKES(30, 6, 3),
        /** Une colonne de plus de chaque côté et une ligne de plus : le gain ×1,5 par niveau. */
        BOARD(20, 4, 3);

        private final Price price;

        MirrorUpgrade(double cost, double growth, int max) {
            price = new Price(cost, growth, max);
        }

        @Override
        public Price price() {
            return price;
        }
    }

    /** Une case du plateau : sa colonne (0 à gauche) et sa ligne (0 en haut). */
    public record MirrorCell(int column, int row) {
    }

    /** Où en est le motif : il s'allume, le joueur le reproduit, ou une courte pause montre le résultat. */
    public enum MirrorPhase { SHOW, INPUT, PAUSE }

    /** Ce qu'a donné un clic sur la moitié droite. */
    public enum MirrorAnswer { RIGHT, WRONG, SUCCESS, FAIL, IGNORED }

    /** Trois cases au départ, vingt au plus. */
    public static final int MIRROR_START = 3, MIRROR_MAX = 20;
    /** Deux colonnes de chaque côté et quatre lignes au départ. */
    public static final int MIRROR_HALF = 2, MIRROR_ROWS = 4;
    /** Chaque case reste allumée 0,8 s, +0,15 s par niveau de mémorisation. */
    public static final double MIRROR_SHOW = 0.8, MIRROR_SHOW_STEP = 0.15;
    /** La pause qui montre le résultat avant le motif suivant. */
    public static final double MIRROR_PAUSE = 1;
    /** Le multiplicateur de longueur : ×1,1 par case au-delà de trois, +0,05 par niveau. */
    public static final double MIRROR_LENGTH = 1.1, MIRROR_LENGTH_STEP = 0.05;
    /** Un plateau plus grand : le gain ×1,5 par niveau. */
    public static final double MIRROR_BOARD = 0.5;
    /** Un motif manqué donne un quart de reflet par case juste. */
    public static final double MIRROR_FAIL_SHARE = 0.25;
    /** L'automatisme clique une case toutes les {@value} secondes, sans faute jusqu'à six cases. */
    public static final double MIRROR_AUTO_STEP = 0.4;
    public static final int MIRROR_AUTO_MAX = 6;

    private final java.util.Random mirrorRandom = new java.util.Random(9);
    private final List<MirrorCell> mirrorPattern = new ArrayList<>();
    private MirrorPhase mirrorPhase = MirrorPhase.PAUSE;
    private double mirrorTimer, mirrorAutoTimer, mirrorLastGain;
    private int mirrorLength = MIRROR_START, mirrorPosition, mirrorMistakes, mirrorBest;
    private long mirrorSuccesses, mirrorFails;
    private MirrorAnswer mirrorLastResult = MirrorAnswer.IGNORED;

    /** Vrai dès que la neuvième dimension est ouverte : au neuvième univers. */
    public boolean isMirrorOpen() {
        return dimensionsUnlocked() >= 9;
    }

    public int mirrorLevel(MirrorUpgrade upgrade) {
        return state.mirrorLevel(upgrade.ordinal());
    }

    /** Les colonnes de chaque côté du miroir. */
    public int mirrorHalf() {
        return MIRROR_HALF + mirrorLevel(MirrorUpgrade.BOARD);
    }

    /** Toutes les colonnes du plateau : les deux moitiés. */
    public int mirrorColumns() {
        return 2 * mirrorHalf();
    }

    public int mirrorRows() {
        return MIRROR_ROWS + mirrorLevel(MirrorUpgrade.BOARD);
    }

    /** Le temps où chaque case du motif reste allumée. */
    public double mirrorShowSeconds() {
        return MIRROR_SHOW + MIRROR_SHOW_STEP * mirrorLevel(MirrorUpgrade.MEMORY);
    }

    /** Les erreurs permises par motif. */
    public int mirrorAllowedMistakes() {
        return mirrorLevel(MirrorUpgrade.MISTAKES);
    }

    /** Ce que donne chaque case d'un motif réussi, avant la longueur : 1, plus 1 par niveau de gain, et les omégas. */
    public double mirrorValue() {
        return (1 + mirrorLevel(MirrorUpgrade.GAIN)) * hadronBoost(idle.core.collider.Hadron.OMEGA) * (1 + MIRROR_BOARD * mirrorLevel(MirrorUpgrade.BOARD));
    }

    public double mirrorLengthFactor() {
        return MIRROR_LENGTH + MIRROR_LENGTH_STEP * mirrorLevel(MirrorUpgrade.LENGTH);
    }

    /** Ce que donne un motif réussi de {@code length} cases. */
    public double mirrorReward(int length) {
        return mirrorValue() * length * Math.pow(mirrorLengthFactor(), Math.max(0, length - MIRROR_START));
    }

    /** Le reflet d'une case par le miroir : même ligne, colonne retournée. */
    public MirrorCell mirrored(MirrorCell cell) {
        return new MirrorCell(mirrorColumns() - 1 - cell.column(), cell.row());
    }

    public MirrorPhase mirrorPhase() {
        return mirrorPhase;
    }

    /** Le motif, sur la moitié gauche ; l'interface ne le montre que pendant qu'il s'allume. */
    public List<MirrorCell> mirrorPattern() {
        return Collections.unmodifiableList(mirrorPattern);
    }

    /** La case allumée en ce moment (rang dans le motif), ou −1 hors de l'allumage. */
    public int mirrorShown() {
        if (mirrorPhase != MirrorPhase.SHOW) return -1;
        return Math.min(mirrorPattern.size() - 1, (int) (mirrorTimer / mirrorShowSeconds() + 1e-9));
    }

    /** Les cases déjà reproduites du motif en cours. */
    public int mirrorPosition() {
        return mirrorPosition;
    }

    /** La longueur du motif en cours. */
    public int mirrorLength() {
        return mirrorLength;
    }

    public int mirrorMistakes() {
        return mirrorMistakes;
    }

    /** Le plus long motif réussi. */
    public int mirrorBest() {
        return mirrorBest;
    }

    public long mirrorSuccesses() {
        return mirrorSuccesses;
    }

    public long mirrorFails() {
        return mirrorFails;
    }

    public double mirrorLastGain() {
        return mirrorLastGain;
    }

    /** Le dernier motif : SUCCESS ou FAIL, IGNORED avant le premier. */
    public MirrorAnswer mirrorLastResult() {
        return mirrorLastResult;
    }

    /**
     * Un clic sur la case ({@code column}, {@code row}) : pendant la reproduction, sur la moitié droite, il doit être le
     * reflet de la case suivante du motif. Ailleurs, ou à un autre moment, il est sans effet.
     */
    public MirrorAnswer clickMirror(int column, int row) {
        if (!isMirrorOpen() || mirrorPhase != MirrorPhase.INPUT) return MirrorAnswer.IGNORED;
        if (column < mirrorHalf() || column >= mirrorColumns() || row < 0 || row >= mirrorRows()) return MirrorAnswer.IGNORED;
        MirrorCell expected = mirrored(mirrorPattern.get(mirrorPosition));
        if (expected.column() == column && expected.row() == row) {
            mirrorPosition++;
            if (mirrorPosition < mirrorPattern.size()) return MirrorAnswer.RIGHT;
            double gain = mirrorReward(mirrorLength);
            earnMirror(gain);
            mirrorSuccesses++;
            mirrorBest = Math.max(mirrorBest, mirrorLength);
            mirrorLength = Math.min(MIRROR_MAX, mirrorLength + 1);
            return endMirror(MirrorAnswer.SUCCESS);
        }
        mirrorMistakes++;
        if (mirrorMistakes <= mirrorAllowedMistakes()) return MirrorAnswer.WRONG;
        earnMirror(mirrorValue() * mirrorPosition * MIRROR_FAIL_SHARE);
        mirrorFails++;
        mirrorLength = Math.max(MIRROR_START, mirrorLength - 1);
        return endMirror(MirrorAnswer.FAIL);
    }

    private void earnMirror(double gain) {
        mirrorLastGain = gain;
        state.setMirrorShards(state.mirrorShards() + gain);
        state.setMirrorShardsEarned(state.mirrorShardsEarned() + gain);
    }

    private MirrorAnswer endMirror(MirrorAnswer result) {
        mirrorLastResult = result;
        mirrorPhase = MirrorPhase.PAUSE;
        mirrorTimer = 0;
        return result;
    }

    /** Un nouveau motif : des cases de la moitié gauche, deux de suite jamais pareilles, qui commencent à s'allumer. */
    private void newMirrorPattern() {
        mirrorPattern.clear();
        MirrorCell last = null;
        for (int n = 0; n < mirrorLength; n++) {
            MirrorCell cell;
            do {
                cell = new MirrorCell(mirrorRandom.nextInt(mirrorHalf()), mirrorRandom.nextInt(mirrorRows()));
            } while (cell.equals(last));
            mirrorPattern.add(cell);
            last = cell;
        }
        mirrorPosition = 0;
        mirrorMistakes = 0;
        mirrorPhase = MirrorPhase.SHOW;
        mirrorTimer = 0;
        mirrorAutoTimer = 0;
    }

    /** Outil d'essai : un motif choisi, prêt à reproduire. */
    public void testSetMirrorPattern(List<MirrorCell> pattern) {
        mirrorPattern.clear();
        mirrorPattern.addAll(pattern);
        mirrorLength = pattern.size();
        mirrorPosition = 0;
        mirrorMistakes = 0;
        mirrorPhase = MirrorPhase.INPUT;
        mirrorTimer = 0;
    }

    /**
     * Le miroir avance de {@code dt} secondes : le motif s'allume case après case, puis attend la reproduction ; après
     * le résultat, une courte pause, puis le motif suivant. L'automatisme reproduit une case toutes les
     * {@value #MIRROR_AUTO_STEP} secondes, sans faute jusqu'à {@value #MIRROR_AUTO_MAX} cases ; au-delà, il se trompe à
     * la septième. Un grand pas se découpe, pour que chaque motif se joue comme en jeu ouvert.
     */
    void runMirror(double dt) {
        if (!isMirrorOpen() || dt <= 0) return;
        double left = dt;
        for (int turn = 0; left > 1e-12 && turn < 200_000; turn++) {
            double step = Math.min(left, 0.1);
            left -= step;
            mirrorTimer += step;
            switch (mirrorPhase) {
                case PAUSE -> {
                    if (mirrorTimer >= MIRROR_PAUSE - 1e-9) {
                        double over = mirrorTimer - MIRROR_PAUSE;
                        newMirrorPattern();
                        mirrorTimer = Math.max(0, over);
                    }
                }
                case SHOW -> {
                    double show = mirrorShowSeconds() * mirrorPattern.size();
                    if (mirrorTimer >= show - 1e-9) {
                        mirrorPhase = MirrorPhase.INPUT;
                        mirrorTimer = Math.max(0, mirrorTimer - show);
                    }
                }
                case INPUT -> {
                    // Sans l'automatisme, le motif attend le joueur.
                    if (!isMirrorAutomatic()) return;
                    mirrorAutoTimer += step;
                    while (mirrorAutoTimer >= MIRROR_AUTO_STEP && mirrorPhase == MirrorPhase.INPUT) {
                        mirrorAutoTimer -= MIRROR_AUTO_STEP;
                        MirrorCell target = mirrored(mirrorPattern.get(mirrorPosition));
                        if (mirrorPosition >= MIRROR_AUTO_MAX) {
                            // Au-delà de six cases, il se trompe : la case voisine sur la même ligne (la première du côté droit au bord).
                            int column = target.column() + 1 < mirrorColumns() ? target.column() + 1 : mirrorHalf();
                            clickMirror(column, target.row());
                        } else {
                            clickMirror(target.column(), target.row());
                        }
                    }
                }
            }
        }
    }

    /** Vrai quand l'automatisme du miroir joue (automatisation des dimensions, version 0.157). */
    boolean isMirrorAutomatic() {
        return isDimensionAutomationRunning(idle.core.universe.DimensionAutomation.MIRROR_PLAY);
    }

    /** Prix du prochain niveau, en reflets ; infini une fois au plus haut. */
    public double mirrorCost(MirrorUpgrade upgrade) {
        return upgrade.cost(mirrorLevel(upgrade));
    }

    public boolean canBuyMirror(MirrorUpgrade upgrade) {
        return isMirrorOpen() && mirrorLevel(upgrade) < upgrade.max() && state.mirrorShards() >= mirrorCost(upgrade) - 1e-9;
    }

    /** Achète ; un plateau plus grand recommence le motif en cours, qui n'y tiendrait plus. */
    public boolean buyMirror(MirrorUpgrade upgrade) {
        if (!canBuyMirror(upgrade)) return false;
        state.setMirrorShards(Math.max(0, state.mirrorShards() - mirrorCost(upgrade)));
        state.setMirrorLevel(upgrade.ordinal(), mirrorLevel(upgrade) + 1);
        if (upgrade == MirrorUpgrade.BOARD) endMirror(mirrorLastResult);
        return true;
    }

    @Override
    boolean buyMirrorUpgrade(int index) {
        return buyMirror(MirrorUpgrade.values()[index]);
    }

    // ------------------------------------------------------------------
    // Les saisons, pour la graine
    // ------------------------------------------------------------------

    /** Les saisons de la graine : chacune remplit plus ou moins le chrono à chaque arrosage. */
    public enum Season {
        SPRING("Printemps", 1.5),
        SUMMER("Été", 1),
        AUTUMN("Automne", 1.25),
        WINTER("Hiver", 0.5);

        private final String label;
        private final double fill;

        Season(String label, double fill) {
            this.label = label;
            this.fill = fill;
        }

        public String label() {
            return label;
        }

        /** Ce qu'elle fait au remplissage par arrosage. */
        public double fill() {
            return fill;
        }
    }

    /** Une saison dure deux minutes. */
    public static final double SEASON_SECONDS = 120;
    /** Chaque arrosage remplit le chrono de 0,2 s par niveau des saisons, multiplié par la saison. */
    public static final double SEASON_FILL = 0.2;

    private double seasonClock;
    private double seasonFilled;

    /** Vrai une fois les saisons là : le miroir ouvert, l'univers formé (elles passent sur la graine). */
    public boolean areSeasonsUnlocked() {
        return isMirrorOpen() && isUniverseFormed();
    }

    public Season season() {
        return Season.values()[(int) (seasonClock / SEASON_SECONDS) % Season.values().length];
    }

    /** Ce qu'il reste à la saison, en secondes. */
    public double seasonLeft() {
        return SEASON_SECONDS - seasonClock % SEASON_SECONDS;
    }

    /** Ce qu'un arrosage verse au chrono, en secondes, en ce moment : 0 sans l'amélioration ou sans chrono. */
    public double seasonFillPerWatering() {
        if (!areSeasonsUnlocked() || !isChronoUnlocked()) return 0;
        return SEASON_FILL * seedUpgradeLevel(SeedUpgrade.SEASONS) * season().fill();
    }

    /** Le temps que les arrosages ont versé au chrono depuis le lancement. */
    public double seasonFilled() {
        return seasonFilled;
    }

    /** Chaque arrosage, de l'arrosoir ou du tuyau, remplit aussi le chrono, selon la saison. */
    @Override
    public boolean tendSeed() {
        if (!super.tendSeed()) return false;
        double fill = seasonFillPerWatering();
        if (fill > 0) {
            double before = state.chronoStored();
            state.setChronoStored(Math.min(chronoCapacity(), before + fill));
            seasonFilled += state.chronoStored() - before;
        }
        return true;
    }

    void runSeasons(double dt) {
        if (!areSeasonsUnlocked() || dt <= 0) return;
        seasonClock = (seasonClock + dt) % (SEASON_SECONDS * Season.values().length);
    }

    /** Outil d'essai : passe à la saison choisie, à son début. */
    public void testSetSeason(Season next) {
        seasonClock = next.ordinal() * SEASON_SECONDS;
    }
}

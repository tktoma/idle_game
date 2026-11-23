package idle.core;

import idle.core.state.*;
import idle.core.achievements.*;
import idle.core.collider.*;
import idle.core.darkmatter.*;
import idle.core.universe.*;
import idle.core.elements.*;
import idle.core.sky.*;
import idle.core.upgrades.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Une partie de {@link GameUniverse} (découpée en 0.293 pour rester sous 1 000 lignes) : la graine qui germe, l'arrosoir et les bulles de l'arbre. */
public abstract class GameSeed extends GameVolumeDuration {
    GameSeed(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        super(state, catalog, automationCatalog, darkCatalog, achievementCatalog, random);
    }

    /** Le palier atteint par la graine, de 1 (pas encore de tige) à {@link #SEED_STAGES}. */
    public int seedStage() {
        if (!state.seedSprouted()) return 1;
        int stage = 1;
        while (stage < seedStageCap() && state.seedWaterings() >= seedStageWaterings(stage + 1)) stage++;
        return stage;
    }

    /**
     * Le palier le plus haut que la graine peut atteindre : deux de plus que les univers formés. Les
     * arrosages font venir la prochaine phase de l'arbre plus tôt, pas plus loin : la mesure de la 0.179 sans ce plafond
     * donnait toutes les bulles au Speedrunner dès l'univers 4, et des univers de 2 h 26 à partir du cinquième. Les
     * arrosages versés au-delà restent comptés pour les paliers suivants.
     */
    public int seedStageCap() { return Math.min(SEED_STAGES, universes() + SEED_STAGES_AHEAD); }

    /** Vrai si la graine a atteint le palier que les univers formés lui permettent : elle attend l'univers suivant. */
    public boolean isSeedStageCapped() {
        return state.seedSprouted() && seedStage() >= seedStageCap() && seedStage() < SEED_STAGES;
    }

    /** Vrai une fois le germe devenu brindille : la graine a au moins son deuxième palier. */
    public boolean isSeedTwig() { return seedStage() >= 2; }

    /**
     * Vrai si l'univers suivant peut s'ouvrir depuis la page Univers. L'univers N attend le palier N
     * de la graine (la tige pour le deuxième, la graine craquelée pour le troisième…).
     */
    public boolean canOpenNextUniverse() {
        // Comptés par la graine elle-même : l'outil d'essai « +1 univers » ne change pas le palier demandé.
        return canStartNextUniverse() && seedStage() >= state.seedUniversesOpened() + 2;
    }

    /** Le palier de la graine qu'attend l'univers suivant : 2 pour le deuxième, 3 pour le troisième… */
    public int nextUniverseStage() { return Math.min(SEED_STAGES, state.seedUniversesOpened() + 2); }

    /**
     * Vrai si la brindille doit ouvrir le deuxième univers maintenant : elle est là, l'univers
     * est formé, aucune partie n'est en cours, et elle ne l'a pas déjà ouvert. Le nombre d'univers n'y entre
     * pas : l'outil d'essai « +1 univers » (pour ouvrir le plan) ne l'empêche plus.
     */
    public boolean twigOpensUniverse() { return canOpenNextUniverse() && !state.twigOpened(); }

    /** Ouvre le deuxième univers si la brindille le demande ; vrai si c'est fait. */
    public boolean openUniverseFromTwig() { return twigOpensUniverse() && startNextUniverse(); }

    /** Vrai une fois l'arrosoir débloqué. */
    public boolean isWateringCanUnlocked() { return state.wateringCan(); }

    /** L'arrosoir se débloque dès que la graine existe, c'est-à-dire dès le premier univers formé ; il est gratuit. */
    public boolean canUnlockWateringCan() { return isUniverseFormed() && !state.wateringCan(); }

    /** Débloque l'arrosoir, posé sur son étagère. */
    public boolean unlockWateringCan() {
        if (!canUnlockWateringCan()) return false;
        state.setWateringCan(true);
        return true;
    }

    /**
     * L'arrosoir est penché sur la graine ({@code true}) ou repris ({@code false}). Penché, il verse
     * {@link #WATERING_CAN_PER_SECOND} arrosage par seconde, tant que la graine peut être arrosée : le premier la fait
     * germer, comme le faisait le premier clic du bouton.
     */
    public void setWatering(boolean pouring) {
        boolean next = pouring && state.wateringCan();
        if (!next) wateringPoured = 0;
        watering = next;
    }

    public boolean isWatering() { return watering; }

    /** La part de l'arrosage en cours déjà versée, de 0 à 1. */
    public double wateringPoured() { return watering ? wateringPoured : 0; }

    /**
     * L'arrosoir tenu et le tuyau versent pendant {@code dt} secondes : un arrosage à chaque arrosage
     * plein, tant que la graine l'accepte. Le tuyau verse seul, sans qu'on le tienne.
     */
    void runWatering(double dt) {
        if (dt <= 0) return;
        if (watering && !state.wateringCan()) setWatering(false);
        if (!canTendSeed()) {
            wateringPoured = 0;
            hosePoured = 0;
            return;
        }
        if (watering) {
            wateringPoured += dt * wateringCanRate();
            while (wateringPoured >= 1 && canTendSeed()) {
                wateringPoured -= 1;
                tendSeed();
            }
        }
        double hose = hoseRate();
        if (hose > 0) {
            hosePoured += dt * hose;
            while (hosePoured >= 1 && canTendSeed()) {
                hosePoured -= 1;
                tendSeed();
            }
        }
        if (!canTendSeed()) {
            wateringPoured = 0;
            hosePoured = 0;
        }
    }

    /**
     * Le premier arrosage fait germer la graine ; les suivants arrosent son germe, qui grandit, et chacun compte pour
     * {@link #seedSoilFactor()} grâce à la terre fertile. Faux si fermé.
     */
    public boolean tendSeed() {
        if (!canTendSeed()) return false;
        if (!state.seedSprouted()) state.setSeedSprouted(true);
        else state.setSeedWaterings(state.seedWaterings() + seedSoilFactor());
        return true;
    }

    /**
     * L'énergie du vide qu'on peut dépenser : ce qu'il restait à la formation de chaque univers, additionné, moins ce
     * que les bulles ont déjà coûté.
     */
    public double voidAvailable() { return Math.max(0, state.voidAccumulated() - state.voidSpent()); }

    /** Vrai si la bulle est née : l'univers formé, la graine à son palier. */
    public boolean isTreeBubbleShown(TreeBubble bubble) { return isUniverseFormed() && seedStage() >= bubble.stage; }

    public boolean isTreeBubbleBought(TreeBubble bubble) { return state.treeBubbleBought(bubble.ordinal()); }

    /** Le niveau de la bulle : 0 pas achetée ; jusqu'à {@link TreeBubble#maxLevel()}. */
    public int treeBubbleLevel(TreeBubble bubble) {
        return Math.min(bubble.maxLevel(), state.treeBubbleLevel(bubble.ordinal()));
    }

    /** Vrai quand la bulle est à son dernier niveau. */
    public boolean isTreeBubbleMaxed(TreeBubble bubble) { return treeBubbleLevel(bubble) >= bubble.maxLevel(); }

    /** Le prix de son prochain niveau, en énergie du vide accumulée. */
    public double treeBubbleCost(TreeBubble bubble) { return bubble.costAt(treeBubbleLevel(bubble)); }

    /** Les bulles nées : pour l'affichage. */
    public List<TreeBubble> treeBubblesShown() {
        List<TreeBubble> shown = new ArrayList<>();
        for (TreeBubble bubble : TreeBubble.values()) if (isTreeBubbleShown(bubble)) shown.add(bubble);
        return shown;
    }

    /**
     * Vrai si la bulle est née et qu'une bulle reliée à elle est achetée (version 0.143 : d'un côté comme de l'autre du
     * lien). La base n'attend rien : c'est par elle que le réseau commence.
     */
    public boolean isTreeBubbleOpen(TreeBubble bubble) {
        if (!isTreeBubbleShown(bubble)) return false;
        if (bubble == TreeBubble.SEED) return true;
        for (TreeBubble linked : bubble.links()) if (isTreeBubbleBought(linked)) return true;
        return false;
    }

    public boolean canBuyTreeBubble(TreeBubble bubble) {
        return isTreeBubbleOpen(bubble) && !isTreeBubbleMaxed(bubble) && voidAvailable() >= treeBubbleCost(bubble) - 1e-9;
    }

    /** Achète un niveau de la bulle avec l'énergie du vide accumulée ; vrai si c'est fait. */
    public boolean buyTreeBubble(TreeBubble bubble) {
        if (!canBuyTreeBubble(bubble)) return false;
        state.setVoidSpent(state.voidSpent() + treeBubbleCost(bubble));
        state.setTreeBubbleLevel(bubble.ordinal(), treeBubbleLevel(bubble) + 1);
        bubbleBought(bubble);
        return true;
    }

    /** Ce qu'une bulle change tout de suite, à son achat : rien par défaut. */
    void bubbleBought(TreeBubble bubble) {
        switch (bubble) {
            // Le vide s'ouvre pour de bon : la suite le garde (state.startUniverse).
            case GIANT_LEFT_LOW -> state.setVoidOpen(true);
            // Plus de maturation : les astres qui mûrissaient sont mûrs.
            case ANCIENT_RIGHT -> {
                for (String id : List.copyOf(state.maturing().keySet())) state.setMaturing(id, 0);
            }
            default -> { }
        }
    }
}

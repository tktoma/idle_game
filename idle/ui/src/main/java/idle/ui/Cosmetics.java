package idle.ui;

import idle.core.Game;
import idle.core.collider.ColliderAchievement;
import idle.core.math.BigNum;
import java.util.ArrayList;
import java.util.List;

/**
 * Guette les conditions des thèmes et des notations à débloquer ({@link Cosmetic}), à chaque image.
 * Trois d'entre elles se comptent en temps réel, hors des règles du jeu : le temps passé sans
 * toucher à rien, le temps passé en pause, et (version 0.200) un Big Bang fait entre minuit et six
 * heures. Ces compteurs ne sont pas gardés : il faut tenir d'une traite, sans fermer le jeu.
 */
final class Cosmetics {

    /** Dix minutes sans toucher à rien. */
    static final double IDLE_SECONDS = 600;
    /** Une heure de pause d'affilée. */
    static final double PAUSE_SECONDS = 3600;
    /** Trente croisements captés à la main de suite. */
    static final int STREAK = 30;
    /** Une heure de surchauffe d'avance. */
    static final double OVERHEAT = 3600;
    private static final BigNum GOOGOL = BigNum.of(1, 100);
    /** Un quetta : 1e30 (version 0.200). */
    private static final BigNum QUETTA = BigNum.of(1, 30);
    /** Le dernier Big Bang fait pendant la nuit (version 0.200) : avant six heures du matin. */
    static final int NIGHT_END_HOUR = 6;

    private final Game game;
    private final Settings settings;
    private double idle;
    private double paused;
    /** Les Big Bangs vus à l'image d'avant (version 0.200), ou −1 avant la première ; vrai une fois l'un d'eux fait de nuit. */
    private int bigBangsSeen = -1;
    private boolean nightBigBang;
    /** L'heure qu'il est : l'horloge, ou une heure fixée pour les essais. */
    java.util.function.Supplier<java.time.LocalTime> clock = java.time.LocalTime::now;

    Cosmetics(Game game, Settings settings) {
        this.game = game;
        this.settings = settings;
    }

    /** Le joueur vient de toucher au clavier ou à la souris. */
    void input() {
        idle = 0;
    }

    /** Le temps passé sans toucher à rien, le jeu en marche. */
    double idle() {
        return idle;
    }

    /** Le temps passé en pause, d'affilée. */
    double paused() {
        return paused;
    }

    /**
     * Une image : avance les compteurs, puis débloque ce dont la condition est remplie.
     *
     * @param dt     le temps réel de l'image, en secondes
     * @param paused vrai si le jeu est en pause
     * @return ce qui vient d'être débloqué, dans l'ordre du catalogue
     */
    List<Cosmetic> frame(double dt, boolean paused) {
        if (paused) {
            this.paused += dt;
        } else {
            this.paused = 0;
            idle += dt;
        }
        List<Cosmetic> opened = new ArrayList<>();
        if (!game.isStarted()) return opened;
        // Un Big Bang de plus depuis l'image d'avant, de nuit : un seul à la fois, un chargement qui en ajoute plusieurs ne compte pas.
        int bigBangs = game.state().bigBangs();
        if (bigBangsSeen >= 0 && bigBangs == bigBangsSeen + 1 && clock.get().getHour() < NIGHT_END_HOUR) nightBigBang = true;
        bigBangsSeen = bigBangs;
        for (Cosmetic cosmetic : Cosmetic.values()) {
            if (!settings.hasCosmetic(cosmetic) && met(cosmetic) && settings.unlock(cosmetic)) opened.add(cosmetic);
        }
        return opened;
    }

    /** Vrai si la condition de ce cosmétique est remplie en ce moment. */
    boolean met(Cosmetic cosmetic) {
        return switch (cosmetic) {
            case SEPIA -> idle >= IDLE_SECONDS;
            case PHOSPHOR -> game.catchStreak() >= STREAK;
            case INFRARED -> game.overheatLeft() >= OVERHEAT;
            case NEBULA -> Math.abs(game.state().darkMatter().toDouble() - 42) < 1e-6;
            case GOLD -> game.colliderAchievements() >= ColliderAchievement.values().length;
            case ALPHABET -> whole(game.state().particles()) == 1111 && whole(game.state().atoms()) == 111;
            case LOG -> game.state().particles().gte(GOOGOL);
            case ROMAN -> paused >= PAUSE_SECONDS;
            case INK -> game.stats().fusions() >= 1_000;
            case CYANOTYPE -> game.state().completedChallenges().contains("challenge_rust");
            case OLD_FILM -> nightBigBang;
            case THERMAL -> game.solarSystemsComplete() >= 3;
            case MOON -> game.state().cometsCaught() >= 10;
            case FOREST -> game.state().hasGalaxy();
            case BLAZE -> game.state().explosions() >= 100;
            case BINARY -> whole(game.state().particles()) == 1024;
            case HEX -> Math.abs(game.state().darkMatter().toDouble() - 255) < 1e-6;
            case JAPANESE -> game.moleculesCreated() >= 10_000;
            case MORSE -> game.notificationsShown() >= 100;
            case SI -> game.state().particles().gte(QUETTA);
            case HEARTBEATS -> game.isChronoUnlocked() && game.chronoStored() >= game.chronoCapacity() - 1e-6;
        };
    }

    /** La partie entière, comme l'affiche le jeu ; −1 au-delà du million, où l'égalité n'a plus de sens. */
    private static long whole(BigNum value) {
        double number = value.toDouble();
        return number >= 1e6 ? -1 : (long) Math.floor(number + 1e-9);
    }

    /** Le texte annoncé au déblocage. */
    static String announcement(Cosmetic cosmetic) {
        return "Débloqué : " + cosmetic.label() + " (" + cosmetic.condition() + "). À choisir dans les réglages.";
    }
}

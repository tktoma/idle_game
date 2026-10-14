package idle.ui;

import idle.core.Game;
import idle.core.collider.ColliderAchievement;
import idle.core.math.BigNum;
import java.util.ArrayList;
import java.util.List;

/**
 * Guette les conditions des thèmes et des notations à débloquer ({@link Cosmetic}), à chaque image.
 * Deux d'entre elles se comptent en temps réel, hors des règles du jeu : le temps passé sans
 * toucher à rien, et le temps passé en pause. Ces compteurs ne sont pas gardés : il faut tenir
 * d'une traite, sans fermer le jeu.
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

    private final Game game;
    private final Settings settings;
    private double idle;
    private double paused;

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

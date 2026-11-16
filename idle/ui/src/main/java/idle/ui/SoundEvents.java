package idle.ui;

import idle.core.Game;
import idle.core.math.BigNum;
import java.util.*;

/**
 * Les bruitages de la partie : ce qui vient de se passer depuis l'image d'avant, relevé sur les compteurs
 * du jeu. Une particule créée, une fusion, une collision, des hadrons, une paire du vide, un arrosage, un palier de la
 * graine, un point gagné dans la dimension ouverte.
 *
 * <p>Ce qui revient sans cesse (les particules, les fusions, les collisions, les hadrons, le vide, les arrosages, les
 * points des dimensions) ne sonne que là où on le voit : sur son onglet, ou dans la fenêtre de sa dimension ouverte, qui
 * couvre les onglets. Un palier de la graine, rare, sonne partout. Les explosions, Big Bangs et univers formés sont relevés
 * par {@link Notifier} : ils sonnent même si leur notification est coupée ou fondue dans le résumé. Le rythme (un même son pas trop souvent) est celui de {@link Sound}.
 *
 * <p>La première image ne fait que relever les compteurs : une partie chargée ne sonne pas tout ce qu'elle a déjà fait.
 */
final class SoundEvents {
    /** Un bruitage à jouer : son son ; un point gagné n'en a pas ({@code null}), il a sa dimension (0 pour le reste). */
    record Heard(Sound.Cue cue, int dimension) {}

    private boolean primed;
    private BigNum particles = BigNum.ZERO;
    private long fusions, collisions, hadrons, waterings, voidPairs;
    private int seedStage, lastOpen;
    private final double[] earned = new double[Game.MAX_DIMENSIONS + 1];

    /**
     * Ce qui s'est passé depuis l'image d'avant, {@code tab} étant le nom de l'onglet affiché ({@code PARTICLES},
     * {@code COLLIDER}…) et {@code openDimension} la dimension dont la fenêtre est ouverte (0 sinon).
     */
    List<Heard> frame(Game game, String tab, int openDimension) {
        BigNum particlesNow = game.stats().particlesCreated();
        long fusionsNow = game.stats().fusions();
        long collisionsNow = game.state().collisionRuns();
        long hadronsNow = game.hadronsMade();
        long wateringsNow = game.state().seedWaterings();
        long pairsNow = (long) Math.floor(game.voidPairsBorn());
        int stageNow = game.seedStage();
        double earnedNow = openDimension > 0 ? game.dimensionEarned(openDimension) : 0;
        List<Heard> heard = new ArrayList<>();
        if (primed) {
            boolean onTabs = openDimension == 0;
            if (onTabs && tab.equals("PARTICLES") && particlesNow.gt(particles)) heard.add(new Heard(Sound.Cue.PARTICLE, 0));
            if (onTabs && (tab.equals("PARTICLES") || tab.equals("ATOMS")) && fusionsNow > fusions) heard.add(new Heard(Sound.Cue.ATOM, 0));
            if (onTabs && tab.equals("COLLIDER") && collisionsNow > collisions) heard.add(new Heard(Sound.Cue.COLLISION, 0));
            if (onTabs && tab.equals("HADRONS") && hadronsNow > hadrons) heard.add(new Heard(Sound.Cue.HADRON, 0));
            if (onTabs && tab.equals("VOID") && pairsNow > voidPairs) heard.add(new Heard(Sound.Cue.VOID, 0));
            if (onTabs && tab.equals("UNIVERSE") && wateringsNow > waterings) heard.add(new Heard(Sound.Cue.WATER, 0));
            if (stageNow > seedStage) heard.add(new Heard(Sound.Cue.SEED, 0));
            // Une fenêtre qui vient de s'ouvrir relève d'abord son compte : ce que l'automatisme a gagné avant ne sonne pas.
            if (openDimension > 0 && openDimension == lastOpen && earnedNow > earned[openDimension]) heard.add(new Heard(null, openDimension));
        }
        primed = true;
        particles = particlesNow;
        fusions = fusionsNow;
        collisions = collisionsNow;
        hadrons = hadronsNow;
        waterings = wateringsNow;
        voidPairs = pairsNow;
        seedStage = stageNow;
        if (openDimension > 0) earned[openDimension] = earnedNow;
        lastOpen = openDimension;
        return heard;
    }
}

package idle.ui;

import idle.core.Game;
import java.util.*;

/**
 * Les bruitages de la partie : ce qui vient de se passer depuis l'image d'avant, relevé sur les compteurs du jeu. Une
 * collision réussie (un croisement qui donne un quark, ou capté au milieu, même avant l'amélioration « Collisions »), des hadrons, une paire du vide, un rond de croissance cueilli, une saison qui commence, un palier de la graine, un point gagné dans la
 * dimension ouverte. Une particule récupérée sonne depuis son générateur ({@link idle.ui.particles.ParticleView}), quand
 * son onde atteint le noyau ; une fusion du joueur, au début de l'animation qui regroupe les générateurs ; un atome créé,
 * sur le grand atome de l'onglet Atomes ({@code GameApp}).
 *
 * <p>Ce qui revient sans cesse (les collisions, les hadrons, le vide, les
 * points des dimensions) ne sonne que là où on le voit : sur son onglet, ou dans la fenêtre de sa dimension ouverte, qui
 * couvre les onglets. Un palier de la graine, rare, sonne partout, d'où qu'il vienne (version 0.256 : l'arrosoir tenu, le
 * tuyau, ou le plafond de la graine qui monte quand un univers se forme et rattrape les arrosages déjà versés). L'eau de
 * l'arrosoir et du tuyau, et la météo, sont des fonds en boucle ({@link Hum}). L'explosion du tableau et le Big Bang du joueur sonnent au
 * début de leur animation ; ce que font les automatismes (fusion, explosion) reste muet. Les univers formés et les autres
 * annonces sonnent par ce que relève {@link Notifier}, même si la notification est coupée ou fondue dans le résumé. Le rythme (un même
 * son pas trop souvent) est celui de {@link Sound}.
 *
 * <p>La première image ne fait que relever les compteurs : une partie chargée ne sonne pas tout ce qu'elle a déjà fait.
 */
final class SoundEvents {
    /**
     * Un bruitage à jouer : son son ; un point gagné n'en a pas ({@code null}), il a sa dimension (0 pour le reste). Une
     * note du miroir ({@link Sound.Cue#MIRROR}) a sa note de piano, de 0 à {@link Sound#PIANO_NOTES} − 1 ({@link #WRONG}
     * pour une faute).
     */
    record Heard(Sound.Cue cue, int dimension, int note) {
        Heard(Sound.Cue cue, int dimension) { this(cue, dimension, 0); }
    }

    /** La note d'une faute dans le miroir. */
    static final int WRONG = -1;

    /**
     * La note de piano d'une case du miroir : sa ligne et sa distance à l'axe, sur la gamme pentatonique. Une case et son
     * reflet ont la même note : reproduire le motif rejoue sa mélodie.
     */
    static int mirrorNote(Game game, idle.core.GameMirror.MirrorCell cell) {
        int half = game.mirrorHalf();
        int distance = cell.column() < half ? half - 1 - cell.column() : cell.column() - half;
        int note = (game.mirrorRows() - 1 - cell.row()) * half + distance;
        return Math.floorMod(note, Sound.PIANO_NOTES);
    }

    /**
     * L'onglet où les bruitages se font entendre : le nom de l'onglet affiché, sauf le vide hors de son sous-onglet
     * « Paires » (version 0.247 : les paires ne sonnent que là où on les voit naître), et le collisionneur hors de son
     * sous-onglet « Énergie » (version 0.272 : les collisions ne sonnent que là où tournent les faisceaux).
     */
    static String heardTab(String tab, boolean pairsShown, boolean energyShown) {
        if ("VOID".equals(tab) && !pairsShown) return "VOID_UPGRADES";
        if ("COLLIDER".equals(tab) && !energyShown) return "COLLIDER_OTHER";
        return tab;
    }

    private boolean primed;
    private long collisions, caught, hadrons, voidPairs, blooms;
    private int seedStage, lastOpen;
    /** La saison relevée à l'image d'avant ; −1 sans saisons. */
    private int season = -1;
    private long probes, mirrorMistakes, mirrorFails, mirrorSuccesses, mixPickups;
    private int mirrorShown = -1, mirrorPosition;
    private boolean mirrorOpen;
    private final double[] earned = new double[Game.MAX_DIMENSIONS + 1];

    /**
     * Ce qui s'est passé depuis l'image d'avant, {@code tab} étant le nom de l'onglet affiché ({@code PARTICLES},
     * {@code COLLIDER}…) et {@code openDimension} la dimension dont la fenêtre est ouverte (0 sinon).
     */
    List<Heard> frame(Game game, String tab, int openDimension) {
        long collisionsNow = game.state().collisionRuns();
        long caughtNow = game.state().colliderCaught();
        long hadronsNow = game.hadronsMade();
        long bloomsNow = game.bloomsCaught();
        long pairsNow = (long) Math.floor(game.voidPairsBorn());
        int stageNow = game.seedStage();
        int seasonNow = game.areSeasonsUnlocked() ? game.season().ordinal() : -1;
        long probesNow = game.state().probeLaunches();
        long pickupsNow = game.mixPickups();
        double earnedNow = openDimension > 0 ? game.dimensionEarned(openDimension) : 0;
        List<Heard> heard = new ArrayList<>();
        if (primed) {
            boolean onTabs = openDimension == 0;
            if (onTabs && tab.equals("COLLIDER") && (collisionsNow > collisions || caughtNow != caught)) heard.add(new Heard(Sound.Cue.COLLISION, 0));
            if (onTabs && tab.equals("HADRONS") && hadronsNow > hadrons) heard.add(new Heard(Sound.Cue.HADRON, 0));
            if (onTabs && tab.equals("VOID") && pairsNow > voidPairs) heard.add(new Heard(Sound.Cue.VOID, 0));
            // Le palier de la graine sonne d'où qu'il vienne (0.256) : sous l'arrosoir seul, il ne sonnait presque jamais,
            // la graine arrivant vite à son plafond, puis passant ses paliers quand un univers se forme. L'eau elle-même
            // (l'arrosoir, le tuyau) et la météo sont des fonds en boucle ({@link Hum}).
            // Le rond de croissance cueilli ; une saison qui commence, sur l'onglet de l'univers.
            if (bloomsNow > blooms) heard.add(new Heard(Sound.Cue.BLOOM, 0));
            if (onTabs && tab.equals("UNIVERSE") && seasonNow >= 0 && season >= 0 && seasonNow != season)
                heard.add(new Heard(Sound.Cue.SEASON, 0, seasonNow));
            if (stageNow > seedStage) heard.add(new Heard(Sound.Cue.SEED, 0));
            // Une sonde qui part, où qu'on soit.
            if (probesNow > probes) heard.add(new Heard(Sound.Cue.PROBE, 0));
            // Une fenêtre qui vient de s'ouvrir relève d'abord son compte : ce que l'automatisme a gagné avant ne sonne pas.
            // Le mélange sonne à chaque éclat ramassé (version 0.241) : aux étapes avancées, ce qu'un éclat rapporte va
            // dans la charge, ou est trop petit pour bouger le total des fragments, qui ne sonnait plus.
            boolean gained = openDimension == 10 ? pickupsNow > mixPickups : earnedNow > earned[openDimension];
            if (openDimension > 0 && openDimension == lastOpen && gained) heard.add(new Heard(null, openDimension));
        }
        primed = true;
        collisions = collisionsNow;
        caught = caughtNow;
        hadrons = hadronsNow;
        blooms = bloomsNow;
        voidPairs = pairsNow;
        seedStage = stageNow;
        season = seasonNow;
        probes = probesNow;
        mixPickups = pickupsNow;
        mirror(game, openDimension, heard);
        if (openDimension > 0) earned[openDimension] = earnedNow;
        lastOpen = openDimension;
        return heard;
    }

    /**
     * Le miroir, fenêtre ouverte : chaque case qui s'allume joue sa note, puis chaque case reproduite la sienne (la même
     * que son reflet) ; une faute joue les notes graves ensemble.
     */
    private void mirror(Game game, int openDimension, List<Heard> heard) {
        boolean open = openDimension == 9 && game.isMirrorOpen();
        if (!open) {
            mirrorOpen = false;
            return;
        }
        int shown = game.mirrorShown(), position = game.mirrorPosition();
        long mistakes = game.mirrorMistakes(), fails = game.mirrorFails(), successes = game.mirrorSuccesses();
        var pattern = game.mirrorPattern();
        // À l'ouverture, seule la case qui s'allume déjà sonne : le reste de la partie n'est pas rejoué.
        if (!mirrorOpen) mirrorShown = -1;
        else if (successes > mirrorSuccesses && !pattern.isEmpty())
            heard.add(new Heard(Sound.Cue.MIRROR, 9, mirrorNote(game, pattern.get(pattern.size() - 1))));
        else if (position > mirrorPosition && position <= pattern.size())
            heard.add(new Heard(Sound.Cue.MIRROR, 9, mirrorNote(game, pattern.get(position - 1))));
        else if (mistakes > mirrorMistakes || fails > mirrorFails) heard.add(new Heard(Sound.Cue.MIRROR, 9, WRONG));
        if (shown >= 0 && shown != mirrorShown && shown < pattern.size()) heard.add(new Heard(Sound.Cue.MIRROR, 9, mirrorNote(game, pattern.get(shown))));
        mirrorOpen = true;
        mirrorShown = shown;
        mirrorPosition = position;
        mirrorMistakes = mistakes;
        mirrorFails = fails;
        mirrorSuccesses = successes;
    }
}

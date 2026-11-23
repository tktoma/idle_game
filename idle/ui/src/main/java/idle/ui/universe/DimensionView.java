package idle.ui.universe;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.ui.common.Format;
import javafx.geometry.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.scene.canvas.*;
import javafx.scene.text.*;

/**
 * Le visuel d'une dimension, dans sa fenêtre : une tentative, sans logique, pour
 * chacune des dix. De la ligne (un point qui va et vient) au point qui contient tout, en passant
 * par le plan, le cube qui tourne, ses instants mis bout à bout, la boucle, la brane, le clicker, la grille et le miroir.
 *
 * <p>La ligne a sa logique depuis la version 0.101 : la boule est celle du jeu ({@link Game#linePosition()}),
 * un trait à chaque bout et, avec l'amélioration, d'autres à égale distance entre les deux. Chaque trait
 * touché s'allume et laisse monter les éclats qu'il donne.
 *
 * <p>Le volume a la sienne depuis la version 0.128 : on clique le cube qui tourne, la vue plonge jusqu'en son
 * centre, et l'on regarde autour de soi (clic gauche tenu) pour cueillir les boules qui y apparaissent (clic
 * droit). À la fin du minuteur, le cube rejette le joueur : la vue ressort, et le cube tourne de nouveau.
 *
 * <p>La durée a la sienne depuis la version 0.134 : un rond, un bouton au milieu ; tant qu'on le tient, les spirales
 * avancent du bord vers le centre, et au relâchement leurs éclats montent du bouton.
 *
 * <p>Depuis la version 0.150, le jeu peut jouer seul le volume et la durée : la vue suit alors la partie. Une plongée
 * commencée sans clic fait plonger la vue, les boules cueillies seules laissent monter leur gain, et chaque relâchement
 * de l'appui automatique fait monter ses instants du bouton. Le joueur peut cueillir avec la plongée automatique ;
 * ses clics sur le bouton de la durée, eux, ne font rien tant que l'appui automatique le tient.
 *
 * <p>La boucle a la sienne depuis la version 0.153 : des cercles concentriques, jusqu'à sept, le premier dehors. Sur
 * chacun, un point tourne et des zones attendent, chacune en trois parts (le bord « mauvais », puis « bien », le centre
 * « excellent »). Un clic sur un cercle (ou sa touche, que la fenêtre transmet) le joue : le mot de ce qu'il a valu
 * monte du point, et un anneau autour du point se remplit tant que le cercle attend. Les clics du rythme automatique
 * font monter leurs nœuds du centre. Le métronome, s'il est branché ({@link #setOnBeat}), bat à chaque huitième de tour du
 * point du premier cercle ({@link LoopBeat}). Depuis la version 0.154, le rythme automatique joue clic par clic, fenêtre
 * ouverte : chacun de ses clics éclaire son cercle et fait monter son mot du point, comme un clic du joueur.
 *
 * <p>La brane a la sienne depuis la version 0.154 : la membrane, un carré tendu, quadrillé. On l'appuie en un point, on
 * glisse (la membrane se creuse vers la souris, et un cercle en pointillé montre jusqu'où ira l'onde), on relâche : une
 * onde part du point appuyé. Chaque onde est un cercle qui grandit et s'efface vers sa portée ; ses rebonds sur les bords
 * se dessinent comme les cercles des sources symétriques (les images), découpés au carré. Les points attendent sur la
 * membrane ; touchés par une onde qui court, ils changent de couleur, et chaque passage fait monter son gain (marqué
 * « rebond » après un bord). Une seule onde à la fois, qui rebondit plusieurs fois (les cent images de
 * {@code Game.images}) ; un front se colore et s'épaissit avec ses rebonds, un éclat marque le bord où il rebondit, le
 * gain dit ses rebonds (« +9, 2 rebonds »), et le point touché affiche ses passages jusqu'à partir avec l'onde.
 *
 * <p>Le clicker a la sienne depuis la version 0.155 : une surface où chaque clic donne des étincelles, plus vite on
 * clique, plus il rapporte (la cadence est écrite au-dessus). Les zones ×2 y apparaissent et s'y superposent : chaque
 * endroit prend la couleur du nombre de zones qui le couvrent, chaque zone a l'anneau du temps qu'il lui reste, et sous
 * la souris s'écrit le multiplicateur. Chaque clic, du joueur ou des clics automatiques, fait une onde et son gain monte.
 *
 * <p>La grille a la sienne depuis la version 0.156, refaite en 0.157 : un cube de mille cases par côté au départ, qui se
 * montre couche par couche (la molette, les flèches ou les boutons de la fenêtre changent de couche). La carte de la
 * couche montre toute la couche, ou, au zoom, la partie autour de la case visée ; un clic vise une case, que l'on essaie
 * du bouton de la fenêtre ; une grille réduite à {@value #GRID_CLICK_CELLS} cases par côté ou moins se montre case par
 * case, et un clic y essaie aussitôt. À côté, le cube vu de biais. Les cases essayées gardent leur couleur tant que
 * leur point est là (version 0.158 : parti, elles s'effacent) : neutre tant que l'affichage ne montre rien, puis du
 * froid au chaud selon la distance ; l'affichage montre ensuite la couche, puis la colonne du point visé par le dernier
 * essai, jamais plus. Un point trouvé fait un éclat doré, « Trouvé ! ×5 ». Les
 * essais de l'automatisme se montrent comme ceux du joueur.
 *
 * <p>Le miroir a la sienne depuis la version 0.157 : un plateau coupé par un trait de lumière. Le motif s'allume case
 * après case à gauche (seule la case allumée se voit) ; puis, de mémoire, on clique ses reflets à droite, et seules les
 * cases déjà reproduites s'y montrent. Les clics justes s'éclairent en vert, les erreurs en rouge ; ceux de
 * l'automatisme aussi. Depuis la version 0.158, le plateau s'agrandit seul toutes les trois réussites : un éclat le dit.
 *
 * <p>Le mélange a la sienne depuis la version 0.159 (en 0.158, sa fenêtre montrait plutôt les vues de mini-jeux tirés au
 * hasard) : une arène carrée où se mêlent, étape par étape, les mécaniques des neuf autres. La ligne et sa boule d'abord ;
 * puis les éclats et le point, mené par la souris (clic ou bouton tenu) ou le clavier ; leur profondeur, que la molette
 * change, et sa jauge ; la charge, que tient le bouton droit (ou la barre d'espace, que transmet la fenêtre) ; le cercle
 * du rythme ; les ondes et leurs rebonds ; les clics et les zones ×2 ; le halo du trésor caché ; l'axe du miroir et ses
 * paires. Les éclats ramassés s'éclairent, et les relâchements et les trésors font monter leur gain. Fermé, il reste un
 * visuel : les motifs des neuf autres se resserrent en un point.
 *
 * <p>Depuis la version 0.187, chaque mini-jeu a un skin ({@link idle.core.universe.Skin#DIMENSION_SKINS}, choisi dans
 * l'onglet Skins par {@link Game#chosenSkin}) : il change le fond, le décor, l'habillage des éléments et les couleurs,
 * jamais la logique ni la place de ce qu'on clique. La ligne devient des rails et des wagons ; le plan une feuille de
 * cahier où le point est une pointe de stylo ; le volume un aquarium où les boules sont des poissons ; la durée un vinyle
 * dont les spirales sont le sillon lu par l'aiguille ; la boucle une cible de tir à l'arc où le point est une flèche ; la
 * brane une toile d'araignée aux points de rosée ; le clicker des bulles de savon ; la grille un démineur aux cases
 * biseautées ; le miroir un vitrail ; le mélange un kaléidoscope qui répète le décor de l'arène six fois. Sans skin
 * (le Classique), rien ne change.
 */
public final class DimensionView extends DimensionViewGrid {
    public DimensionView(Game game) {
        super(game);
    }

    /** L'arène à l'écran : gauche, haut, côté. La boucle se dessine à sa gauche, la jauge de profondeur à sa droite. */
    public double[] mixArena() {
        double w = getWidth(), h = getHeight();
        double side = Math.max(40, Math.min(h * 0.9, w * 0.6));
        return new double[] {w / 2 - side / 2, h / 2 - side / 2, side};
    }

    /** Où se dessine ce point de l'arène (de 0 à 1, y vers le bas) : pour les vérifications. */
    public double[] mixScreen(double x, double y) {
        double[] a = mixArena();
        return new double[] {a[0] + x * a[2], a[1] + y * a[2]};
    }

    /** Le point de l'arène sous ce point de l'écran, ou {@code null} hors de l'arène. */
    public double[] mixAt(double sx, double sy) {
        double[] a = mixArena();
        double x = (sx - a[0]) / a[2], y = (sy - a[1]) / a[2];
        return x < 0 || x > 1 || y < 0 || y > 1 ? null : new double[] {x, y};
    }

    /**
     * Un bouton enfoncé sur le mélange : le droit tient la charge (dès la durée) ; le gauche, sur l'arène, mène le point
     * (dès le plan) et compte un clic (dès le clicker). Vrai si le geste a servi.
     */
    boolean pressMix(double x, double y, boolean secondary) {
        if (!game.isMixOpen()) return false;
        if (secondary) {
            if (!game.isMixing(4)) return false;
            mixHeld = true;
            game.holdMix(true);
            return true;
        }
        double[] at = mixAt(x, y);
        if (at == null || !game.isMixing(2)) return false;
        mixSteering = true;
        game.setMixTarget(at[0], at[1]);
        if (game.isMixing(7)) game.clickMix();
        return true;
    }

    /** La souris tenue glisse : le point la suit. Vrai si elle le menait. */
    boolean dragMix(double x, double y) {
        if (!mixSteering || dimension != Dimension.MIX || !game.isMixOpen()) return false;
        double[] a = mixArena();
        game.setMixTarget((x - a[0]) / a[2], (y - a[1]) / a[2]);
        return true;
    }

    /** Un bouton relâché : le droit verse la charge ; le gauche lâche le point. Vrai si la charge a été versée. */
    public boolean releaseMix(boolean secondary) {
        if (!secondary) {
            mixSteering = false;
            return false;
        }
        if (!mixHeld) return false;
        mixHeld = false;
        game.holdMix(false);
        return true;
    }

    /** Vrai tant que le bouton droit tient la charge : pour les vérifications. */
    public boolean mixHeld() { return mixHeld; }

    /** Le point plonge ({@code steps} positif) ou remonte d'autant de pas : la molette, ou les touches de la fenêtre. */
    public void shiftMixDepth(int steps) {
        if (!game.isMixOpen() || !game.isMixing(3) || steps == 0) return;
        // Tant que le point plonge vers la profondeur visée, les pas s'ajoutent à elle ; arrivé, ils partent de lui.
        double from = Double.isNaN(mixDepthAim) ? game.mixDepth() : mixDepthAim;
        mixDepthAim = Math.max(0, Math.min(1, from + steps * MIX_DEPTH_STEP));
        game.setMixDepth(mixDepthAim);
    }

    public int mixFlashesShown() { return mixFlashes.size(); }

    /** Le dernier gain qui monte (« Excellent ! +12 », « Trésor ! +40 »), ou une chaîne vide : pour les vérifications. */
    public String mixLastMark() { return mixMarks.isEmpty() ? "" : mixMarks.get(mixMarks.size() - 1).text; }

    /**
     * Une image du mélange : les gains et les éclats vieillissent ; les relâchements, les trésors trouvés et les éclats
     * ramassés (par le point ou une onde, du joueur ou du mélange automatique) en font de nouveaux. Changer d'étape vide
     * l'arène sans rien éclairer.
     */
    void runMix(double dt) {
        for (MixMark mark : mixMarks) mark.age += dt;
        mixMarks.removeIf(mark -> mark.age > MIX_MARK_SECONDS);
        for (MixMark flash : mixFlashes) flash.age += dt;
        mixFlashes.removeIf(flash -> flash.age > MIX_FLASH_SECONDS);
        if (!game.isMixOpen()) return;
        if (!Double.isNaN(mixDepthAim) && Math.abs(game.mixDepth() - mixDepthAim) < 1e-9) mixDepthAim = Double.NaN;
        boolean sameStage = lastMixStage == game.mixStage();
        lastMixStage = game.mixStage();
        Set<Long> present = new HashSet<>();
        for (Game.MixShard shard : game.mixShardList()) present.add(shard.id());
        if (sameStage) {
            for (Game.MixShard gone : mixSeen.values()) {
                if (present.contains(gone.id()) || mixFlashes.size() >= 24) continue;
                mixFlashes.add(mixMark(gone.x(), gone.y(), null, shardColor(gone.depth())));
            }
        }
        mixSeen.clear();
        for (Game.MixShard shard : game.mixShardList()) mixSeen.put(shard.id(), shard);
        long releases = game.mixReleases(), treasures = game.mixTreasures();
        if (sameStage && lastMixReleases >= 0 && releases > lastMixReleases && game.mixLastGain() > 0) {
            String word = rhythmWord(game.mixLastRhythm());
            mixMarks.add(mixMark(game.mixX(), game.mixY(), (word.isEmpty() ? "" : word + " ") + "+"
                    + Format.darkMatter(BigNum.of(game.mixLastGain())), color()));
        }
        if (sameStage && lastMixTreasures >= 0 && treasures > lastMixTreasures) {
            mixMarks.add(mixMark(game.mixX(), game.mixY(), "Trésor ! +"
                    + Format.darkMatter(BigNum.of(game.mixLastGain())), MIX_HOT));
        }
        while (mixMarks.size() > 12) mixMarks.remove(0);
        lastMixReleases = releases;
        lastMixTreasures = treasures;
    }

    /**
     * L'arène du mélange, avec toutes les mécaniques mêlées à l'étape en cours : la ligne et sa boule ; les éclats et le
     * point (le plan), leur profondeur (taille et couleur) et la jauge du point (le volume) ; la charge autour du point
     * (la durée) ; le cercle du rythme à gauche (la boucle) ; les ondes et leurs rebonds découpés à l'arène (la brane) ;
     * les zones ×2, colorées selon qu'elles se superposent (le clicker) ; une grille légère et le halo du point, du froid
     * au chaud, sans jamais montrer le trésor (la grille) ; l'axe du miroir et un trait entre les éclats d'une paire (le
     * miroir). Fermé, le mélange reste un visuel : les motifs des neuf autres se resserrent en un point.
     */
    void mixScene(GraphicsContext g, double w, double h, double cx, double cy, double u) {
        if (!game.isMixOpen()) {
            point(g, w, h, cx, cy, u);
            return;
        }
        Color c = color();
        double[] a = mixArena();
        double left = a[0], top = a[1], side = a[2];
        if (skinned) kaleidoscope(g, cx, cy, left, top, side, c);
        else mixArt(g, left, top, side, c, 1, true);
        for (MixMark flash : mixFlashes) {
            double t = flash.age / MIX_FLASH_SECONDS;
            double[] at = mixScreen(flash.x, flash.y);
            UniverseView.glow(g, at[0], at[1], side * (0.02 + 0.05 * t), flash.tint, 0.9 * (1 - t));
        }
        g.setTextAlign(TextAlignment.CENTER);
        for (MixMark mark : mixMarks) {
            double t = mark.age / MIX_MARK_SECONDS;
            double[] at = mixScreen(mark.x, mark.y);
            g.setGlobalAlpha(Math.max(0, 1 - t));
            g.setFill(mark.tint);
            g.setFont(Font.font(null, FontWeight.BOLD, 16));
            g.fillText(mark.text, at[0], at[1] - 18 - 40 * t);
        }
        if (game.isMixing(5)) mixRing(g, left, top, side);
        if (game.isMixing(3)) mixGauge(g, left, top, side);
        g.setGlobalAlpha(1);
    }

    /**
     * Ce qui se dessine dans l'arène, dans l'ordre : son fond, la grille, les zones ×2 et leurs cases, le cadre, l'axe du
     * miroir, la ligne, les ondes, les éclats et le point. {@code full} pour l'arène elle-même ; faux pour une copie du
     * kaléidoscope, qui n'a ni fond, ni cases colorées, ni textes, ni point, et dont {@code fade} atténue les couleurs.
     */
    void mixArt(GraphicsContext g, double left, double top, double side, Color c, double fade, boolean full) {
        if (full) {
            g.setGlobalAlpha(0.6);
            g.setFill(Color.web("#0b0e1a"));
            g.fillRoundRect(left, top, side, side, 10, 10);
        }
        // La grille : un quadrillage léger de 10 × 10.
        if (game.isMixing(8)) {
            g.setStroke(Color.web("#3b4670"));
            g.setLineWidth(1);
            g.setGlobalAlpha(0.35 * fade);
            for (int k = 1; k < Game.MIX_GRID; k++) {
                double at = side * k / Game.MIX_GRID;
                g.strokeLine(left + at, top, left + at, top + side);
                g.strokeLine(left, top + at, left + side, top + at);
            }
        }
        // Le clicker : chaque case prend la couleur du nombre de zones ×2 qui la couvrent.
        if (game.isMixing(7)) {
            List<Game.MixZone> zones = game.mixZones();
            double cell = side / MIX_ZONE_CELLS;
            for (int i = 0; i < MIX_ZONE_CELLS && full && !zones.isEmpty(); i++) {
                for (int j = 0; j < MIX_ZONE_CELLS; j++) {
                    double x = (i + 0.5) / MIX_ZONE_CELLS, y = (j + 0.5) / MIX_ZONE_CELLS;
                    int stack = 0;
                    for (Game.MixZone zone : zones) if (zone.contains(x, y)) stack++;
                    if (stack == 0) continue;
                    g.setGlobalAlpha(Math.min(0.75, 0.3 + 0.12 * stack));
                    g.setFill(stackColor(stack));
                    g.fillRect(left + i * cell, top + j * cell, cell + 0.5, cell + 0.5);
                }
            }
            for (Game.MixZone zone : zones) {
                double[] at = mixScreen(zone.x(), zone.y());
                double r = zone.radius() * side;
                g.setStroke(full ? stackColor(1) : c);
                g.setLineWidth(2);
                g.setGlobalAlpha(0.8 * fade);
                g.strokeArc(at[0] - r, at[1] - r, 2 * r, 2 * r, 90, 360 * zone.left() / Game.MIX_ZONE_SECONDS, ArcType.OPEN);
                if (!full) continue;
                g.setFill(Color.WHITE);
                g.setFont(Font.font(null, FontWeight.BOLD, 12));
                g.setTextAlign(TextAlignment.CENTER);
                g.fillText("×2", at[0], at[1] + 4);
            }
        }
        g.setStroke(c);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.6 * fade);
        g.strokeRoundRect(left, top, side, side, 10, 10);
        // Le miroir : son axe, au milieu.
        if (game.isMixing(9)) {
            double mx = left + side / 2;
            for (int k = 0; k < 5; k++) UniverseView.glow(g, mx, top + (k + 0.5) * side / 5, side * 0.06, full ? DimensionPalette.color(9) : c, (0.3 + 0.1 * Math.sin(time * 2 + k)) * fade);
            g.setStroke(Color.WHITE);
            g.setLineWidth(2);
            g.setGlobalAlpha(0.8 * fade);
            g.strokeLine(mx, top, mx, top + side);
        }
        mixLine(g, left, top, side, c, fade);
        if (game.isMixing(6)) mixWaves(g, left, top, side, fade);
        if (game.isMixing(2)) {
            mixShards(g, side, full ? null : c, fade);
            if (full) mixCursor(g, side);
        }
    }

    /**
     * Le kaléidoscope : la lunette, un cercle qui contient l'arène ; le décor de l'arène y est répété cinq fois de plus,
     * tourné de 60° en 60° et miroité une fois sur deux, en couleurs saturées et atténué, puis l'arène elle-même par-dessus,
     * à sa place (ce qu'on clique ne bouge pas) ; autour, le contour noir de la lunette.
     */
    void kaleidoscope(GraphicsContext g, double cx, double cy, double left, double top, double side, Color c) {
        double lens = side * 0.72;
        g.setGlobalAlpha(1);
        g.setFill(LENS);
        g.fillOval(cx - lens, cy - lens, lens * 2, lens * 2);
        // Plus de découpe ronde autour des copies. L'arène, carrée et centrée, tient dans la lunette quel
        // que soit l'angle (sa demi-diagonale vaut 0,707 côté, la lunette 0,72), et le contour noir, large de dix pixels,
        // couvre ce qui dépasserait d'une lueur. La découpe coûtait à elle seule la moitié du dessin : le kaléidoscope
        // prenait plus de deux fois le temps du Classique.
        g.save();
        for (int k = 1; k < 6; k++) {
            g.save();
            g.translate(cx, cy);
            g.rotate(60 * k);
            if (k % 2 == 1) g.scale(1, -1);
            g.translate(-cx, -cy);
            mixArt(g, left, top, side, KALEIDO[k - 1], 0.45, false);
            g.restore();
        }
        mixArt(g, left, top, side, c, 1, true);
        g.restore();
        g.setGlobalAlpha(1);
        g.setStroke(Color.BLACK);
        g.setLineWidth(10);
        g.strokeOval(cx - lens - 5, cy - lens - 5, lens * 2 + 10, lens * 2 + 10);
        g.setStroke(LENS_RIM);
        g.setLineWidth(1.5);
        g.strokeOval(cx - lens, cy - lens, lens * 2, lens * 2);
    }

    /**
     * La ligne : à mi-hauteur, ses graduations, et la boule qui fait l'aller-retour ; plus pâle dès qu'elle pose des éclats.
     * {@code fade} atténue le tout, pour les copies du kaléidoscope.
     */
    void mixLine(GraphicsContext g, double left, double top, double side, Color c, double fade) {
        double y = top + Game.MIX_LINE_Y * side;
        boolean first = !game.isMixing(2);
        g.setStroke(c);
        g.setLineWidth(first ? 3 : 1.5);
        g.setGlobalAlpha((first ? 0.9 : 0.35) * fade);
        g.strokeLine(left + side * 0.02, y, left + side * 0.98, y);
        for (int i = 0; i < game.mixTicks(); i++) {
            double x = left + game.mixTickX(i) * side;
            g.strokeLine(x, y - (first ? 14 : 8), x, y + (first ? 14 : 8));
        }
        double bx = left + game.mixBall() * side;
        UniverseView.glow(g, bx, y, first ? 26 : 14, c, (first ? 0.8 : 0.4) * fade);
        g.setGlobalAlpha((first ? 1 : 0.6) * fade);
        g.setFill(Color.WHITE);
        double r = first ? 7 : 4;
        g.fillOval(bx - r, y - r, 2 * r, 2 * r);
    }

    /**
     * Les éclats : plus petits et plus violets plus ils sont profonds ; un anneau autour de ceux que le point peut
     * ramasser à sa profondeur. Un trait léger relie les deux éclats d'une paire, de part et d'autre du miroir. Dans une
     * copie du kaléidoscope, tous prennent la couleur {@code tint} (sinon {@code null}), atténuée par {@code fade}.
     */
    void mixShards(GraphicsContext g, double side, Color tint, double fade) {
        List<Game.MixShard> shards = game.mixShardList();
        if (game.isMixing(9)) {
            Map<Long, Game.MixShard> byId = new HashMap<>();
            for (Game.MixShard shard : shards) byId.put(shard.id(), shard);
            g.setStroke(tint == null ? DimensionPalette.color(9) : tint);
            g.setLineWidth(1);
            g.setGlobalAlpha(0.4 * fade);
            for (Game.MixShard shard : shards) {
                Game.MixShard twin = byId.get(shard.twin());
                if (twin == null || twin.id() < shard.id()) continue;
                double[] p = mixScreen(shard.x(), shard.y()), q = mixScreen(twin.x(), twin.y());
                g.strokeLine(p[0], p[1], q[0], q[1]);
            }
        }
        boolean deep = game.isMixing(3);
        for (Game.MixShard shard : shards) {
            double[] at = mixScreen(shard.x(), shard.y());
            Color gem = tint != null ? tint : deep ? shardColor(shard.depth()) : SHARD;
            double r = side * 0.018 * (deep ? 1.3 - 0.6 * shard.depth() : 1);
            UniverseView.glow(g, at[0], at[1], r * 2.6, gem, 0.6 * fade);
            g.setGlobalAlpha(fade);
            g.setFill(gem);
            g.fillPolygon(new double[] {at[0], at[0] + r, at[0], at[0] - r}, new double[] {at[1] - r, at[1], at[1] + r, at[1]}, 4);
            if (deep && Math.abs(shard.depth() - game.mixDepth()) <= game.mixDepthTolerance() + 1e-9) {
                g.setStroke(Color.WHITE);
                g.setLineWidth(1.5);
                g.setGlobalAlpha(0.8 * fade);
                g.strokeOval(at[0] - r * 1.8, at[1] - r * 1.8, r * 3.6, r * 3.6);
            }
        }
    }

    /**
     * Le point : son rayon de ramassage ; sa couleur dit sa profondeur ; le halo dit la chaleur du trésor, du bleu froid
     * au rouge chaud ; la charge tenue l'entoure d'un anneau qui grossit avec son multiplicateur, et écrit ce qu'elle
     * porte.
     */
    void mixCursor(GraphicsContext g, double side) {
        double[] at = mixScreen(game.mixX(), game.mixY());
        if (game.isMixing(8)) {
            double heat = game.mixHeat();
            UniverseView.glow(g, at[0], at[1], side * (0.06 + 0.1 * heat), MIX_COLD.interpolate(MIX_HOT, heat), 0.5 + 0.4 * heat);
        }
        double reach = Game.MIX_REACH_RADIUS * side;
        Color tint = game.isMixing(3) ? shardColor(game.mixDepth()) : Color.WHITE;
        g.setStroke(tint);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.6);
        g.strokeOval(at[0] - reach, at[1] - reach, 2 * reach, 2 * reach);
        g.setGlobalAlpha(1);
        g.setFill(tint);
        g.fillOval(at[0] - 6, at[1] - 6, 12, 12);
        if (game.isMixing(4) && game.isMixHolding()) {
            double top = 1 + 1 + Game.MIX_CHARGE_STEP * game.mixLevel(Game.MixUpgrade.CHARGE);
            double share = (game.mixChargeMultiplier() - 1) / (top - 1);
            double r = reach + 6 + 14 * share;
            g.setStroke(DimensionPalette.color(4));
            g.setLineWidth(3);
            g.setGlobalAlpha(0.9);
            g.strokeOval(at[0] - r, at[1] - r, 2 * r, 2 * r);
            g.setFill(DimensionPalette.color(4));
            g.setFont(Font.font(13));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(mixChargeText(), at[0], at[1] + r + 16);
        }
    }

    /** « 12,5 ×1,40 » : la charge tenue et son multiplicateur, sous le point. */
    public String mixChargeText() {
        return Format.darkMatter(BigNum.of(game.mixPot())) + " " + Format.multiplier(game.mixChargeMultiplier());
    }

    /**
     * Les ondes : chacune est un cercle qui grandit jusqu'à sa portée ; ses rebonds sur les bords de l'arène sont les
     * cercles de ses images ({@code Game.images}, dans le repère de −1 à 1 de la brane), découpés à l'arène et colorés
     * selon leurs rebonds ; {@code fade} les atténue, pour les copies du kaléidoscope.
     */
    void mixWaves(GraphicsContext g, double left, double top, double side, double fade) {
        if (game.mixWaves().isEmpty()) return;
        g.save();
        g.beginPath();
        g.moveTo(left, top);
        g.lineTo(left + side, top);
        g.lineTo(left + side, top + side);
        g.lineTo(left, top + side);
        g.closePath();
        g.clip();
        for (Game.MixWave wave : game.mixWaves()) {
            double r = wave.radius();
            if (r <= 0) continue;
            double ebb = Math.max(0.15, 1 - r / wave.reach());
            for (double[] ix : Game.images(2 * wave.x() - 1)) {
                for (double[] iy : Game.images(2 * wave.y() - 1)) {
                    double x = (ix[0] + 1) / 2, y = (iy[0] + 1) / 2;
                    // Une image dont le cercle ne touche pas l'arène ne se dessine pas.
                    if (x + r < 0 || x - r > 1 || y + r < 0 || y - r > 1) continue;
                    int bounces = (int) (ix[1] + iy[1]);
                    g.setStroke(bounceColor(bounces));
                    g.setLineWidth(2 + Math.min(4, bounces));
                    g.setGlobalAlpha(0.85 * fade * ebb);
                    g.strokeOval(left + (x - r) * side, top + (y - r) * side, 2 * r * side, 2 * r * side);
                }
            }
        }
        g.restore();
        g.setGlobalAlpha(1);
    }

    /**
     * La boucle : un petit cercle à gauche de l'arène, sa zone (le centre « excellent » plus vif) et le point qui tourne ;
     * dessous, ce que donnerait un relâchement maintenant, et ce qu'a donné le dernier.
     */
    void mixRing(GraphicsContext g, double left, double top, double side) {
        double r = Math.min(56, Math.max(18, left * 0.3)), x = Math.max(r + 8, left - r - 24), y = top + side * 0.3;
        g.setStroke(Color.web("#3b4670"));
        g.setLineWidth(4);
        g.setGlobalAlpha(0.9);
        g.strokeOval(x - r, y - r, 2 * r, 2 * r);
        double half = game.mixRingHalfWidth(), zone = game.mixRingZone();
        g.setStroke(DimensionPalette.color(5));
        g.setLineWidth(6);
        g.strokeArc(x - r, y - r, 2 * r, 2 * r, 90 - 360 * (zone + half), 720 * half, ArcType.OPEN);
        g.setStroke(Color.WHITE);
        g.setLineWidth(7);
        g.strokeArc(x - r, y - r, 2 * r, 2 * r, 90 - 360 * (zone + 0.2 * half), 144 * half, ArcType.OPEN);
        double angle = 2 * Math.PI * game.mixRingPhase();
        double px = x + r * Math.sin(angle), py = y - r * Math.cos(angle);
        UniverseView.glow(g, px, py, 16, DimensionPalette.color(5), 0.8);
        g.setGlobalAlpha(1);
        g.setFill(Color.WHITE);
        g.fillOval(px - 5, py - 5, 10, 10);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font(null, FontWeight.BOLD, 13));
        g.setFill(DimensionPalette.color(5));
        g.fillText(rhythmWord(game.mixRhythmNow()), x, y + r + 20);
        if (game.mixLastRhythm() != Game.MixRhythm.NONE) {
            g.setFont(Font.font(11));
            g.setFill(Color.web("#c8d4ea"));
            g.fillText("Dernier : " + rhythmWord(game.mixLastRhythm()), x, y + r + 36);
        }
    }

    /**
     * Le volume : une jauge à droite de l'arène, de la surface (en haut) au fond ; la bande que le point ramasse autour
     * de sa profondeur, le point, et un trait par éclat à sa profondeur.
     */
    void mixGauge(GraphicsContext g, double left, double top, double side) {
        double x = left + side + Math.min(36, Math.max(10, (getWidth() - left - side) * 0.25)), width = 14;
        g.setGlobalAlpha(0.8);
        g.setFill(new LinearGradient(0, top, 0, top + side, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#3a3626")), new Stop(1, Color.web("#1d1a4a"))));
        g.fillRoundRect(x, top, width, side, 6, 6);
        double depth = game.mixDepth(), tolerance = game.mixDepthTolerance();
        double from = Math.max(0, depth - tolerance), to = Math.min(1, depth + tolerance);
        g.setGlobalAlpha(0.45);
        g.setFill(shardColor(depth));
        g.fillRect(x, top + from * side, width, (to - from) * side);
        g.setStroke(Color.web("#c8d4ea"));
        g.setLineWidth(1);
        g.setGlobalAlpha(0.7);
        for (Game.MixShard shard : game.mixShardList()) {
            double y = top + shard.depth() * side;
            g.strokeLine(x - 4, y, x, y);
        }
        double y = top + depth * side;
        g.setStroke(Color.WHITE);
        g.setLineWidth(3);
        g.setGlobalAlpha(1);
        g.strokeLine(x - 6, y, x + width + 6, y);
        g.setFill(Color.web("#c8d4ea"));
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText("surface", x + width / 2, top - 6);
        g.fillText("fond", x + width / 2, top + side + 14);
    }

    // 10. Le mélange, fermé : les motifs des neuf autres tournent en se resserrant en un point, et la lumière en sort.
    void point(GraphicsContext g, double w, double h, double cx, double cy, double u) {
        for (int k = 0; k < 48; k++) {
            double a = 2 * Math.PI * k / 48 + time * 0.05;
            g.setStroke(DimensionPalette.color(k % 10 + 1));
            g.setLineWidth(1);
            g.setGlobalAlpha(0.15 + 0.1 * Math.sin(time * 2 + k));
            g.strokeLine(cx, cy, cx + Math.cos(a) * Math.max(w, h), cy + Math.sin(a) * Math.max(w, h));
        }
        for (int k = 0; k < 9; k++) {
            double phase = (time * 0.12 + k / 9.0) % 1;
            double r = u * 0.42 * (1 - phase);
            double a = 2 * Math.PI * k / 9 + time * 0.4 + phase * 3;
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            Color c = DimensionPalette.color(k + 1);
            if (k == 2) cube(g, x, y, u * 0.025 * (1 - phase) + 2, time, time * 0.7, c, 1 - phase * 0.5);
            else UniverseView.glow(g, x, y, u * 0.03 * (1 - phase) + 3, c, 0.9 * (1 - phase * 0.5));
        }
        UniverseView.glow(g, cx, cy, u * (0.1 + 0.02 * Math.sin(time * 3)), color(), 1);
        g.setGlobalAlpha(1);
        g.setFill(Color.WHITE);
        g.fillOval(cx - 4, cy - 4, 8, 8);
    }
}

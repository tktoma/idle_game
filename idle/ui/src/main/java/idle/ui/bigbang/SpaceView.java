package idle.ui.bigbang;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * L'espace que l'expansion de la matière a créé, vu de dessus, avec les molécules qui l'occupent.
 *
 * <p>L'espace est un disque dont la surface vaut l'espace créé ({@code GameState.space()}) : son
 * bord recule à mesure que le temps passe. Les molécules s'y rangent en spirale depuis le centre,
 * à l'intérieur du cœur utilisé : le disque dont la surface vaut l'espace qu'elles occupent et
 * celui que réservent leurs lieux de rassemblement ({@link Game#usedSpace()}). Le cœur grossit
 * donc avec ce que fait le joueur, le bord avec le temps, et l'anneau vide entre les deux est
 * l'espace libre.
 *
 * <p>Les molécules rassemblées ({@link Game#formSubstance(String)}) occupent le centre, et celles
 * d'une même sorte y forment un seul amas : une surface pleine, sans fente ni trou entre ses
 * molécules, de la couleur de la molécule et cernée de la couleur de son état. Les états les plus serrés sont servis les premiers,
 * donc au plus près du centre : les métaux, puis les cristaux, les solides, les liquides, et les
 * gaz au bord, comme une planète de son noyau à son atmosphère. Les amas se cognent sans se
 * chevaucher : chacun s'arrête là où il touche son voisin, comme des bulles serrées les unes
 * contre les autres. Les molécules qui ne sont pas rassemblées restent au-delà, dans l'ordre de
 * leur création.
 *
 * <p>Au cœur de tout, les assemblages formés ({@link Game#formAssembly(String)}) : chacun est un
 * bloc de plusieurs centaines de molécules, bien plus gros qu'un amas. Il ne se dessine pas comme
 * eux : son corps est opaque, de la couleur de sa matière, sous un contour clair et épais ; ses
 * molécules de plusieurs sortes y sont mêlées, chacune réduite à un grain anguleux de sa couleur ;
 * et il porte son nom. On ne revoit ses molécules qu'en s'approchant tout près.
 *
 * <p>Et les astres formés ({@link Game#formBody(String)}), qui sont faits de leurs assemblages.
 * Chacun a une clairière ronde, de la taille de ce qu'il contient : l'astre lui-même
 * ({@link BodyArt}) en occupe le milieu, et les blocs de ses assemblages restent autour de lui, en
 * couronne, chacun avec sa couleur, son contour, ses grains et son nom. Une planète est un globe
 * au milieu de ses roches, de ses eaux et de ses airs ; une étoile, une boule de feu dans son
 * hydrogène. Le trou noir supermassif tient le centre de tout, les plus gros astres autour de
 * lui, et aucun ne touche ni un autre astre, ni un bloc, ni un amas. Un astre reste net de très
 * loin, quand tout le reste est devenu du gaz.
 *
 * <p>Enfin la galaxie ({@link Game#formGalaxy()}) : une fois formée, tout cela s'enroule en bras de
 * spirale ({@link Spiral}) autour du trou noir supermassif. Chaque molécule y devient un point, de
 * sa couleur mêlée à celle des étoiles ; les astres se rangent le long des bras, les plus gros
 * près du centre ; l'ensemble tourne, ses bras à la traîne. Le joueur peut toujours revenir à la
 * matière telle qu'elle était rangée ({@link #showGalaxy(boolean)}).
 *
 * <p>Le cœur est juste, les dessins ne le sont pas : les molécules s'y partagent la place à parts
 * égales, quelle que soit la taille de chacune. Une molécule lourde n'est donc pas dessinée
 * beaucoup plus grande qu'une légère, et aucun dessin ne dépasse du cœur, donc de l'espace.
 *
 * <p>La vue se règle comme une caméra :
 * <ul>
 *   <li>la molette, ou {@link #zoom(double)}, rapproche ou éloigne ;</li>
 *   <li>on ne peut pas reculer plus loin que ce qu'il faut pour voir tout l'espace : plus il y en
 *       a, plus on peut dézoomer ({@link #fit()} y va d'un coup) ;</li>
 *   <li>tirer avec la souris déplace la vue, sans sortir de l'espace.</li>
 * </ul>
 * De très loin, la matière est un gaz : chaque molécule n'est plus qu'une bouffée de sa couleur,
 * et les bouffées se fondent en nuages qui remuent lentement ({@link #mist()}). En s'approchant,
 * le gaz se dissipe et laisse voir les amas et leurs molécules : d'abord des points, puis leurs
 * atomes, leurs liaisons et leurs symboles ({@link MoleculeArt}). Tant que le joueur n'a pas rapproché la vue, elle suit
 * l'expansion et montre toujours tout l'espace.
 *
 * <p>L'ensemble tourne très lentement, et chaque molécule sur elle-même : c'est une image de
 * l'espace, pas une mesure.
 *
 * <p>Le code est rangé en parties (version 0.098) : une chaîne de classes abstraites, une par thème, dont
 * celle-ci est le bout ; le reste du programme ne voit que {@code SpaceView}. De la base au bout :
 * {@link SpaceViewBase}, {@link SpaceViewPlacement}, {@link SpaceViewPacking}. Quand une partie a besoin
 * d'une méthode définie plus loin dans la chaîne, elle la déclare abstraite en tête, sous « Ce que
 * fournissent les parties suivantes ».
 */
public final class SpaceView extends SpaceViewPacking {

    SpaceView(Game game) {
        super(game);
    }

    /**
     * Fait avancer le temps de la vue et la redessine.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    public void frame(double elapsed) {
        if (BigBangPage.moving()) time += elapsed;
        beat++;
        sincePlaced += elapsed;
        boolean stale = placedVersion != game.state().moleculesVersion();
        // La création automatique ajoute des molécules toutes les quelques secondes : une grosse vue ne
        // se remet en place que de loin en loin, sinon elle accrocherait à chaque passage.
        calm = stale && game.isAutoCreatingMolecules() && shapes.length >= CALM_FROM && sincePlaced < CALM_SECONDS;
        boolean changed = moved || getWidth() != drawnWidth || getHeight() != drawnHeight || (stale && !calm);
        Cosmos scale = cosmosShown();
        if (scale != null) {
            // L'amas de galaxies tourne lentement ; la toile de l'univers ne bouge pas.
            boolean still = scale == Cosmos.UNIVERSE && farDrawn;
            if (!moved && getWidth() == drawnWidth && getHeight() == drawnHeight && (still || beat % EVERY != 0)) return;
            redraw();
            return;
        }
        if (farDrawn) {
            // De retour dans l'espace : tout est à redessiner.
            farDrawn = false;
            changed = true;
        }
        if (!changed && beat % EVERY != 0) return;
        redraw();
        calm = false;
    }

    /** Nombre de points à partir duquel la vue attend, pendant la création automatique, avant de se remettre en place. */
    static final int CALM_FROM = 5_000;
    /** Secondes entre deux mises en place d'une grosse vue pendant la création automatique. */
    static final double CALM_SECONDS = 15;

    /** Dessine la vue telle qu'elle est à cet instant. */
    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;
        moved = false;
        drawnWidth = w;
        drawnHeight = h;
        // Au-delà de la galaxie, la vue ne montre plus l'espace : inutile d'y ranger les molécules.
        Cosmos beyond = cosmosShown();
        if (beyond != null) {
            farDrawn = true;
            drawFar(getGraphicsContext2D(), w, h, beyond);
            return;
        }
        place();
        double fit = fitScale();
        if (Double.isNaN(scale) || following) scale = fit;
        scale = Math.max(fit, Math.min(CLOSEST, scale));
        double radius = spaceRadius();
        // La vue ne sort pas de l'espace.
        double away = Math.hypot(centerX, centerY);
        if (away > radius && away > 0) {
            centerX *= radius / away;
            centerY *= radius / away;
        }

        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.setGlobalAlpha(1);
        idle.core.universe.Skin look = game.chosenSkin(idle.core.universe.SkinVisual.BIG_BANG);
        g.setFill(look == null ? VOID : skinVoid(look));
        g.fillRect(0, 0, w, h);
        double originX = w / 2 - centerX * scale;
        double originY = h / 2 - centerY * scale;
        // Le skin du Big Bang (version 0.089) : tout le fond de la vue, puis l'espace créé à sa façon.
        if (look != null) skinSky(g, w, h, look);

        // L'espace créé : un disque un peu plus clair que le vide. Son bord est tracé plus bas, par-dessus le gaz.
        double edge = radius * scale;
        if (edge > 0.5) {
            if (look != null) {
                skinSpace(g, originX, originY, edge, look);
            } else {
                g.setFill(SPACE);
                g.fillOval(originX - edge, originY - edge, 2 * edge, 2 * edge);
            }
        }
        // Les molécules se partagent le cœur utilisé à parts égales : la taille d'un dessin vient de sa
        // place, pas du volume de sa molécule, et rien ne dépasse du cœur, ni un dessin ni un amas.
        double heart = coreRadius();
        int count = shapes.length;
        double cell = count == 0 ? 0 : CELL_SHARE * heart / Math.sqrt(count);
        // Une fois la galaxie formée, tout est montré enroulé en bras, à moins que le joueur n'ait demandé à revoir la matière.
        swirled = asGalaxy && game.hasGalaxy() && swirlCos.length == count && count > 0;
        // La galaxie tourne dans le sens où ses bras traînent derrière elle, et plus vite que la matière ne dérivait.
        double turn = swirled ? -time * DRIFT * GALAXY_SPIN : time * DRIFT;
        // Le cosinus et le sinus de l'angle de chaque place sont connus : il ne reste qu'à les tourner tous du même angle.
        double cosTurn = Math.cos(turn);
        double sinTurn = Math.sin(turn);
        double[] cosOf = swirled ? swirlCos : cosines;
        double[] sinOf = swirled ? swirlSin : sines;
        reach = 0;
        for (int index = 0; index < count; index++) {
            double out = distances[index] * (heart - cell);
            if (!hidden[index]) reach = Math.max(reach, out + (swirled ? rooms[index] : states[index] != null ? blobs[index] : radii[index]) * cell);
            xs[index] = originX + out * (cosOf[index] * cosTurn - sinOf[index] * sinTurn) * scale;
            ys[index] = originY + out * (sinOf[index] * cosTurn + cosOf[index] * sinTurn) * scale;
        }
        double unit = cell * scale;
        double[] awayOf = swirled ? swirlAway : skyAway;
        double[] angleOf = swirled ? swirlAngle : skyAngle;
        shown = swirled ? swirlRadius : skyRadius;
        for (int body = 0; body < sky.length; body++) {
            reach = Math.max(reach, (awayOf[body] + shown[body]) * cell);
            skyX[body] = originX + awayOf[body] * unit * Math.cos(angleOf[body] + turn);
            skyY[body] = originY + awayOf[body] * unit * Math.sin(angleOf[body] + turn);
        }
        // De loin, la matière se fond en gaz : pas du tout au-dessus de MIST_FROM pixels par place, entièrement sous MIST_FULL.
        mist = count == 0 ? 0 : Math.max(0, Math.min(1, (MIST_FROM - unit) / (MIST_FROM - MIST_FULL)));

        // Les amas d'abord, sous les molécules : pour chaque sorte rassemblée, un rond par molécule, un
        // pont entre voisines et un triangle plein entre trois voisines, donc une seule surface. Elle est
        // dessinée deux fois : en entier dans la couleur de son état, puis un peu rétrécie dans sa couleur
        // à elle, ce qui lui laisse un liseré tout autour. De très loin, le gaz les remplace. Ronds et ponts ont été taillés pour ne recouvrir aucun autre amas ({@link #collide}).
        double widest = BLOB_SHARE * unit;
        if (swirled) {
            // La galaxie n'a plus ni amas ni blocs : sa lueur d'abord, sous ses points.
            double glow = heart * scale;
            if (glow > 1) {
                g.setFill(new javafx.scene.paint.RadialGradient(0, 0, originX, originY, glow, false,
                        javafx.scene.paint.CycleMethod.NO_CYCLE, GALAXY_GLOW));
                g.fillOval(originX - glow, originY - glow, 2 * glow, 2 * glow);
            }
        }
        if (unit >= 2 && mist < 1 && !swirled) {
            // Un pont s'arrête net au centre de ses deux ronds : avec un bout carré, ses coins en dépasseraient.
            g.setLineCap(StrokeLineCap.BUTT);
            for (int cluster = sky.length; cluster < clusters.length; cluster++) {
                if (clusters[cluster].length == 0) continue;
                // Un corps tout entier hors de la vue : aucun de ses ronds, de ses ponts ni de ses triangles n'y serait dessiné.
                double around = rounds[cluster] * unit;
                double midX = originX + (middleX[cluster] * cosTurn - middleY[cluster] * sinTurn) * unit;
                double midY = originY + (middleY[cluster] * cosTurn + middleX[cluster] * sinTurn) * unit;
                if (midX + around < 0 || midX - around > w || midY + around < 0 || midY - around > h) continue;
                Assembly block = blocks[cluster];
                // Un amas : sa couleur sous un fin liseré de la couleur de son état. Un bloc d'assemblage : la
                // couleur de sa matière sous un contour clair, deux fois plus épais. De loin, un contour plus
                // fin qu'un pixel n'est pas dessiné, et le corps est d'une seule couleur.
                double rim = block != null ? BLOCK_RIM : RIM;
                if (rim * unit < 0.75) rim = 0;
                for (int pass = rim > 0 ? 0 : 1; pass < 2; pass++) {
                    Color color = pass == 0 ? rims[cluster] : inks[cluster];
                    double inset = pass == 0 ? 0 : rim;
                    g.setFill(color);
                    g.setStroke(color);
                    for (int index : clusters[cluster]) {
                        double x = xs[index];
                        double y = ys[index];
                        double round = (blobs[index] - inset) * unit;
                        if (round <= 0 || x + round < 0 || x - round > w || y + round < 0 || y - round > h) continue;
                        g.fillOval(x - round, y - round, 2 * round, 2 * round);
                    }
                    int[] links = bridges[cluster];
                    for (int link = 0; link < links.length; link += 2) {
                        double width = widths[cluster][link / 2] - inset;
                        if (width <= 0) continue;
                        int from = links[link];
                        int to = links[link + 1];
                        if (Math.max(xs[from], xs[to]) + widest < 0 || Math.min(xs[from], xs[to]) - widest > w
                                || Math.max(ys[from], ys[to]) + widest < 0 || Math.min(ys[from], ys[to]) - widest > h) continue;
                        g.setLineWidth(2 * width * unit);
                        g.strokeLine(xs[from], ys[from], xs[to], ys[to]);
                    }
                    // Le creux entre trois voisines, rempli jusqu'à leurs centres : il est tout entier à
                    // l'intérieur de l'amas, et ne mord donc jamais sur le liseré.
                    int[] corners = fills[cluster];
                    for (int corner = 0; corner < corners.length; corner += 3) {
                        for (int point = 0; point < 3; point++) {
                            cornerX[point] = xs[corners[corner + point]];
                            cornerY[point] = ys[corners[corner + point]];
                        }
                        if (Math.max(cornerX[0], Math.max(cornerX[1], cornerX[2])) < 0
                                || Math.min(cornerX[0], Math.min(cornerX[1], cornerX[2])) > w
                                || Math.max(cornerY[0], Math.max(cornerY[1], cornerY[2])) < 0
                                || Math.min(cornerY[0], Math.min(cornerY[1], cornerY[2])) > h) continue;
                        g.fillPolygon(cornerX, cornerY, 3);
                    }
                }
            }
            g.setLineCap(StrokeLineCap.SQUARE);
        }

        // Le gaz : une bouffée par molécule, de sa couleur, qui s'écarte un peu de sa place et y revient.
        // Tout ce qui est dessiné jusqu'ici est ensuite flouté d'un coup, et les bouffées deviennent des nuages.
        if (mist > 0) {
            double puff = Math.max(1, (swirled ? SWIRL_PUFF : PUFF_SHARE) * unit);
            double churn = CHURN * unit * mist;
            g.setGlobalAlpha((swirled ? SWIRL_ALPHA : PUFF_ALPHA) * mist);
            // sin(a + b) et cos(a + b), où a ne dépend que du temps et b que de la place.
            double swayTimeCos = Math.cos(time * 0.35);
            double swayTimeSin = Math.sin(time * 0.35);
            double heaveTimeCos = Math.cos(time * 0.28);
            double heaveTimeSin = Math.sin(time * 0.28);
            for (int index = 0; index < count; index++) {
                double x = xs[index] + churn * (swayTimeSin * swayCos[index] + swayTimeCos * swaySin[index]);
                double y = ys[index] + churn * (heaveTimeCos * heaveCos[index] - heaveTimeSin * heaveSin[index]);
                if (hidden[index] || x + puff < 0 || x - puff > w || y + puff < 0 || y - puff > h) continue;
                g.setFill(swirled ? swirlTints[index] : tints[index] != null ? tints[index] : shapes[index].dominant());
                g.fillOval(x - puff, y - puff, 2 * puff, 2 * puff);
            }
            g.setGlobalAlpha(1);
            blur.setRadius(Math.min(63, Math.max(LEAST_BLUR, BLUR_SHARE * unit) * mist));
            g.applyEffect(blur);
        }

        // Ce qui reste net par-dessus le gaz : les étoiles, le cercle discret du cœur utilisé, le bord de l'espace.
        drawStars(g, w, h);
        double core = coreRadius() * scale;
        if (core > 4 && !swirled) {
            g.setStroke(CORE);
            g.setLineWidth(1);
            g.strokeOval(originX - core, originY - core, 2 * core, 2 * core);
        }
        if (edge > 0.5 && look != null) {
            skinEdge(g, originX, originY, edge, look);
        } else if (edge > 0.5) {
            g.setStroke(EDGE);
            g.setLineWidth(1.5);
            g.strokeOval(originX - edge, originY - edge, 2 * edge, 2 * edge);
        }

        // Les astres, nets même de très loin : chacun au milieu de sa clairière, aussi grand qu'elle le permet.
        for (int body = 0; body < sky.length; body++) {
            double round = shown[body] * unit;
            double x = skyX[body];
            double y = skyY[body];
            if (round <= 0 || x + round < 0 || x - round > w || y + round < 0 || y - round > h) continue;
            if (round < DOT_BELOW) {
                double dot = Math.max(0.75, round);
                g.setFill(skyTints[body]);
                g.fillOval(x - dot, y - dot, 2 * dot, 2 * dot);
                continue;
            }
            // Un petit astre n'occupe pas toute sa clairière : une planète reste plus grande qu'une lune, quoi qu'elles contiennent.
            // Dans la galaxie, ce rayon est déjà celui du dessin.
            double r = round * (swirled ? 1 : share(sky[body].tier())) / BodyArt.span(sky[body]);
            BodyArt.draw(g, sky[body], x - BodyArt.centerX(sky[body]) * r, y - BodyArt.centerY(sky[body]) * r, r);
        }

        // Puis les molécules, celles qui tombent dans la vue seulement : elles s'effacent à mesure que le gaz les remplace.
        if (mist < 1) {
            g.setGlobalAlpha(1 - mist);
            for (int index = 0; index < count && swirled; index++) {
                // Dans la galaxie, une molécule est un point de sa couleur ; de tout près, on la revoit, en petit.
                double x = xs[index];
                double y = ys[index];
                double dot = Math.max(0.8, rooms[index] * unit);
                if (hidden[index] || x + dot < 0 || x - dot > w || y + dot < 0 || y - dot > h) continue;
                if (dot >= DRAWN_FROM) {
                    shapes[index].draw(g, x, y, dot, time * (0.05 + 0.04 * ((index * 7) % 5)) * (index % 2 == 0 ? 1 : -1) + index);
                } else {
                    g.setFill(swirlTints[index]);
                    g.fillOval(x - dot, y - dot, 2 * dot, 2 * dot);
                }
            }
            for (int index = 0; index < count && !swirled; index++) {
                double x = xs[index];
                double y = ys[index];
                double size = radii[index] * cell * scale;
                if (hidden[index] || x + size < 0 || x - size > w || y + size < 0 || y - size > h) continue;
                // Dans un bloc d'assemblage, une molécule n'est qu'un grain anguleux et fixe, tant qu'on n'est pas tout près.
                if (assembled[index] && unit < GRAINS_BELOW) {
                    if (unit >= 2.5) grain(g, shapes[index].dominant(), x, y, GRAIN_SHARE * unit, index * 0.9, grainX, grainY);
                    continue;
                }
                // Chacune tourne sur elle-même, à son rythme et dans son sens.
                double spin = time * (0.05 + 0.04 * ((index * 7) % 5)) * (index % 2 == 0 ? 1 : -1) + index;
                shapes[index].draw(g, x, y, states[index] != null ? size * GATHERED_SHARE : size, spin);
            }
            if (!swirled) drawNames(g, originX, originY, cosTurn, sinTurn, unit, w, h);
            g.setGlobalAlpha(1);
        }
        drawBodyNames(g, unit, w, h);
        drawLegend(g);

        g.setFill(TEXT);
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.LEFT);
        double grown = magnification();
        g.fillText(following ? "Tout l'espace" : "Grossi " + (grown >= 10 ? String.valueOf(Math.round(grown))
                : ElementText.number(Math.round(grown * 10) / 10.0)) + " fois", 10, h - 10);
    }

    /**
     * Un grain : un petit losange plein, de travers. C'est ce que devient une molécule dans un bloc
     * d'assemblage, là où un amas montre ses atomes ronds.
     *
     * @param xs deux tableaux de quatre nombres, pour ne rien allouer
     */
    static void grain(GraphicsContext g, Color color, double x, double y, double size, double angle, double[] xs, double[] ys) {
        double cos = Math.cos(angle) * size;
        double sin = Math.sin(angle) * size;
        // Un losange étroit : sa petite diagonale fait les six dixièmes de la grande.
        xs[0] = x + cos;
        ys[0] = y + sin;
        xs[1] = x - 0.6 * sin;
        ys[1] = y + 0.6 * cos;
        xs[2] = x - cos;
        ys[2] = y - sin;
        xs[3] = x + 0.6 * sin;
        ys[3] = y - 0.6 * cos;
        g.setFill(color);
        g.fillPolygon(xs, ys, 4);
    }

    /**
     * Écrit le nom de chaque bloc d'assemblage en son milieu, dès qu'il est assez large à l'écran :
     * sur une pastille de sa couleur, en clair ou en sombre selon ce qui s'y lit le mieux.
     */
    private void drawNames(GraphicsContext g, double originX, double originY, double cosTurn, double sinTurn, double unit, double w, double h) {
        g.setTextAlign(TextAlignment.CENTER);
        for (int cluster = 0; cluster < clusters.length; cluster++) {
            Assembly block = blocks[cluster];
            int size = clusters[cluster].length;
            if (block == null || size == 0) continue;
            // Un bloc de n places fait à peu près 2,4 × √n rayons de place de large.
            double across = 2.4 * Math.sqrt(size) * unit;
            if (across < NAME_FROM) continue;
            double x = originX + (labelX[cluster] * cosTurn - labelY[cluster] * sinTurn) * unit;
            double y = originY + (labelY[cluster] * cosTurn + labelX[cluster] * sinTurn) * unit;
            double font = Math.max(11, Math.min(22, across / (0.62 * block.name().length() + 4)));
            double wide = 0.62 * font * block.name().length() + font;
            if (x + wide / 2 < 0 || x - wide / 2 > w || y + font < 0 || y - font > h) continue;
            Color tint = inks[cluster];
            boolean pale = 0.299 * tint.getRed() + 0.587 * tint.getGreen() + 0.114 * tint.getBlue() > 0.6;
            g.setFill(tint);
            g.fillRoundRect(x - wide / 2, y - 0.8 * font, wide, 1.6 * font, 1.6 * font, 1.6 * font);
            g.setFill(pale ? VOID : BLOCK_EDGE);
            g.setFont(Font.font(font));
            g.fillText(block.name(), x, y + 0.35 * font);
        }
        g.setTextAlign(TextAlignment.LEFT);
    }

    /**
     * Part de sa clairière qu'occupe le dessin d'un astre, selon son échelle : les clairières ont la
     * taille de ce que les astres contiennent, et une lune contient presque autant qu'une planète.
     */
    static double share(Body.Tier tier) {
        return switch (tier) {
            case RUBBLE -> 0.7;
            case ASTEROID -> 0.6;
            case MOON -> 0.72;
            case REMNANT -> 0.5;
            case COMET, PLANET, STAR, BLACK_HOLE, CORE, EXOTIC -> 1;
        };
    }

    /**
     * Écrit le nom de chaque astre au bas de sa clairière, dès qu'il est assez large à l'écran : en
     * clair sur une pastille sombre. Plus près encore, les assemblages dont il est fait sont écrits dessous.
     */
    private void drawBodyNames(GraphicsContext g, double unit, double w, double h) {
        g.setTextAlign(TextAlignment.CENTER);
        // Les noms déjà écrits, quatre nombres chacun : gauche, haut, droite, bas. Un nom qui en recouvrirait un autre n'est pas écrit.
        if (labels.length < 4 * sky.length) labels = new double[4 * sky.length];
        int written = 0;
        for (int body = 0; body < sky.length; body++) {
            double across = 2 * shown[body] * unit;
            if (across < NAME_FROM) continue;
            String name = sky[body].name();
            double font = Math.max(11, Math.min(18, across / (0.62 * name.length() + 4)));
            double wide = 0.62 * font * name.length() + font;
            String made = across >= MADE_FROM ? madeOf(sky[body]) : "";
            double small = 0.72 * font;
            // Les assemblages ne sont écrits que s'ils tiennent dans la largeur de l'astre.
            if (0.56 * small * made.length() + small > across) made = "";
            double widest = Math.max(wide, made.isEmpty() ? 0 : 0.56 * small * made.length() + small);
            double tall = made.isEmpty() ? 0 : 1.7 * small;
            // Au bas de la clairière ; si elle dépasse de la vue, au bas de ce qu'on en voit, tant que c'est encore sur elle.
            double x = Math.max(Math.min(widest / 2 + 6, w / 2), Math.min(Math.max(w - widest / 2 - 6, w / 2), skyX[body]));
            double y = Math.min(skyY[body] + across / 2 - 1.1 * font - tall, h - 1.4 * font - tall - 16);
            if (y - font < 0 || Math.hypot(x - skyX[body], y - skyY[body]) > across / 2 - 0.5 * font) continue;
            double left = x - widest / 2;
            double top = y - 0.8 * font;
            double right = x + widest / 2;
            double bottom = y + 0.8 * font + tall;
            // La légende, en haut à gauche, compte comme un nom déjà écrit.
            boolean covered = left < legendWidth() + 8 && top < 12 + 16 * legendLines();
            for (int other = 0; other < written && !covered; other += 4) {
                covered = left < labels[other + 2] + 4 && right > labels[other] - 4 && top < labels[other + 3] + 2 && bottom > labels[other + 1] - 2;
            }
            if (covered) continue;
            labels[written++] = left;
            labels[written++] = top;
            labels[written++] = right;
            labels[written++] = bottom;
            g.setFill(VOID);
            g.setGlobalAlpha(0.78);
            g.fillRoundRect(x - wide / 2, y - 0.8 * font, wide, 1.6 * font, 1.6 * font, 1.6 * font);
            if (!made.isEmpty()) {
                double below = 0.56 * small * made.length() + small;
                g.fillRoundRect(x - below / 2, y + 0.95 * font, below, 1.6 * small, 1.6 * small, 1.6 * small);
            }
            g.setGlobalAlpha(1);
            g.setFill(BLOCK_EDGE);
            g.setFont(Font.font(font));
            g.fillText(name, x, y + 0.35 * font);
            if (!made.isEmpty()) {
                g.setFill(TEXT);
                g.setFont(Font.font(small));
                g.fillText(made, x, y + 0.95 * font + 1.15 * small);
            }
        }
        g.setTextAlign(TextAlignment.LEFT);
    }

    /** « Fait de roche granitique, minerai de cuivre et atmosphère étrangère » : les assemblages d'un astre. */
    private String madeOf(Body body) {
        StringBuilder text = new StringBuilder("Fait de ");
        List<String> parts = body.assemblies();
        for (int index = 0; index < parts.size(); index++) {
            if (index > 0) text.append(index == parts.size() - 1 ? " et " : ", ");
            String name = game.assembly(parts.get(index)).name();
            text.append(Character.toLowerCase(name.charAt(0))).append(name, 1, name.length());
        }
        return text.toString();
    }

    /** Largeur du fond de la légende : un peu plus quand elle dit ce que vaut un point. */
    private double legendWidth() {
        return perDot > 1 ? 156 : 130;
    }

    /** Nombre de lignes de la légende : les astres, les assemblages, chaque état qui a des molécules rassemblées, et ce que vaut un point. */
    private int legendLines() {
        int lines = (blocks() > 0 ? 1 : 0) + (sky.length > 0 ? 1 : 0) + (perDot > 1 ? 1 : 0);
        for (long count : gathered) {
            if (count > 0) lines++;
        }
        return lines;
    }

    /**
     * En haut à gauche : le nombre d'astres, celui des assemblages qui ne sont dans aucun astre, puis
     * les états qui ont des molécules rassemblées hors des assemblages et combien, « Liquides : 5 »,
     * avec la couleur de leur liseré. Rien tant qu'il n'y a rien de tout cela.
     */
    private void drawLegend(GraphicsContext g) {
        int formed = blocks();
        int lines = legendLines();
        if (lines == 0) return;
        // Un fond sombre : de près, une molécule peut passer dessous.
        g.setFill(VOID);
        g.setGlobalAlpha(0.72);
        g.fillRoundRect(4, 4, legendWidth(), 8 + 16 * lines, 8, 8);
        g.setGlobalAlpha(1);
        g.setFont(Font.font(11));
        g.setTextAlign(TextAlignment.LEFT);
        double y = 18;
        if (sky.length > 0) {
            // Un petit globe : clair, avec son ombre.
            g.setFill(BLOCK_EDGE);
            g.fillOval(9, y - 9, 10, 10);
            g.setFill(VOID);
            g.setGlobalAlpha(0.5);
            g.fillOval(12, y - 6, 7, 7);
            g.setGlobalAlpha(1);
            g.setFill(TEXT);
            g.fillText(swirled ? "Galaxie : " + sky.length + " astres" : "Astres : " + sky.length, 24, y);
            y += 16;
        }
        if (formed > 0) {
            grain(g, BLOCK_EDGE, 14, y - 4, 5, 0.6, grainX, grainY);
            g.setFill(TEXT);
            g.fillText("Assemblages : " + formed, 24, y);
            y += 16;
        }
        for (int place = 0; place < PLACES.length; place++) {
            if (gathered[place] == 0) continue;
            // Chaque état a sa forme, pour se reconnaître sans la couleur : la même que son signe dans les pages.
            g.setFill(placeColor(PLACES[place]));
            stateShape(g, PLACES[place], 14, y - 4);
            g.setFill(TEXT);
            g.fillText(BigBangPage.placeTitle(PLACES[place]) + " : " + Format.count(BigNum.of(gathered[place])), 24, y);
            y += 16;
        }
        if (perDot > 1) {
            g.setFill(TEXT);
            g.fillText("1 point = " + perDot + " molécules", 10, y);
        }
    }

    /**
     * La forme d'un état dans la légende, centrée en ({@code x}, {@code y}) : trois points pour un
     * gaz, deux vagues pour un liquide, un carré pour un solide, un losange pour un cristal, un
     * triangle pour un métal ({@link MatterText#sign}). La couleur est celle du remplissage en cours.
     */
    static void stateShape(GraphicsContext g, Molecule.State state, double x, double y) {
        switch (state) {
            case GAS -> {
                g.fillOval(x - 1.5, y - 5, 3, 3);
                g.fillOval(x - 5, y + 1, 3, 3);
                g.fillOval(x + 2, y + 1, 3, 3);
            }
            case LIQUID -> {
                g.fillRect(x - 5, y - 3, 10, 2);
                g.fillRect(x - 5, y + 1, 10, 2);
            }
            case SOLID -> g.fillRect(x - 4, y - 4, 8, 8);
            case CRYSTAL -> g.fillPolygon(new double[] {x, x + 5, x, x - 5}, new double[] {y - 5, y, y + 5, y}, 4);
            case METAL -> g.fillPolygon(new double[] {x, x + 5, x - 5}, new double[] {y - 5, y + 4, y + 4}, 3);
        }
    }

    /** Un fond d'étoiles fixes, les mêmes d'une image à l'autre, qui scintillent à peine. */
    private void drawStars(GraphicsContext g, double w, double h) {
        // Le skin du Big Bang (version 0.089) habille aussi le fond de l'espace.
        idle.core.universe.Skin look = game.chosenSkin(idle.core.universe.SkinVisual.BIG_BANG);
        if (look != null) {
            skinStars(g, w, h, look);
            return;
        }
        g.setFill(STAR);
        for (int star = 0; star < STARS; star++) {
            double x = STAR_X[star] * w;
            double y = STAR_Y[star] * h;
            double twinkle = 0.25 + 0.2 * Math.sin(time * STAR_PACE[star] + star);
            g.setGlobalAlpha(Math.max(0.05, Math.min(1, twinkle)));
            double size = star % 9 == 0 ? 1.6 : 1;
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }
        g.setGlobalAlpha(1);
    }

    // ------------------------------------------------------------------
    // Les skins du Big Bang (version 0.089) : le vide, le ciel entier, l'espace créé et son bord.
    // ------------------------------------------------------------------

    private static Color skinVoid(idle.core.universe.Skin look) {
        return switch (look) {
            case BANG_AURORA -> Color.web("#020d14");
            case BANG_SUPERNOVA -> Color.web("#0d0306");
            default -> Color.web("#04081a");
        };
    }

    private static Color tint(Color c, double alpha) {
        return Color.color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(1, alpha)));
    }

    /**
     * Le ciel de toute la vue. Aurore : des rideaux de lumière verte, bleue et violette sur toute la
     * hauteur. Supernova : une nébuleuse de filaments rouges et orangés qui part d'un coin. Cristal :
     * un réseau de facettes sur toute la vue, des facettes allumées çà et là.
     */
    private void skinSky(GraphicsContext g, double w, double h, idle.core.universe.Skin look) {
        double small = Math.min(w, h);
        switch (look) {
            case BANG_AURORA -> {
                Color[] tints = {Color.web("#5dffa8"), Color.web("#6fc8ff"), Color.web("#b77bff"), Color.web("#5dffa8")};
                int steps = 90;
                for (int band = 0; band < tints.length; band++) {
                    double base = h * (0.1 + 0.22 * band);
                    for (int k = 0; k < steps; k++) {
                        double x = w * k / (steps - 1);
                        double top = base + Math.sin(x / w * 7 + time * 0.4 + band * 1.7) * h * 0.08
                                + Math.sin(x / w * 19 + band) * h * 0.02;
                        double tall = h * (0.35 + 0.12 * Math.sin(x / w * 11 + band * 2 + time * 0.3));
                        g.setGlobalAlpha(1);
                        g.setFill(new javafx.scene.paint.LinearGradient(0, top, 0, top + tall, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                                new javafx.scene.paint.Stop(0, tint(tints[band], 0)),
                                new javafx.scene.paint.Stop(0.25, tint(tints[band], 0.22)),
                                new javafx.scene.paint.Stop(1, tint(tints[band], 0))));
                        g.fillRect(x - w / steps, top, w / steps * 2, tall);
                    }
                }
            }
            case BANG_SUPERNOVA -> {
                double cx = w * 0.85, cy = h * 0.15;
                double far = Math.hypot(Math.max(cx, w - cx), Math.max(cy, h - cy));
                g.setFill(new javafx.scene.paint.RadialGradient(0, 0, cx, cy, far, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, tint(Color.web("#ff9a3c"), 0.35)),
                        new javafx.scene.paint.Stop(0.35, tint(Color.web("#a01e3a"), 0.22)),
                        new javafx.scene.paint.Stop(1, tint(Color.web("#2a0610"), 0))));
                g.setGlobalAlpha(1);
                g.fillRect(0, 0, w, h);
                // Les filaments : des traits qui rayonnent du reste de l'étoile jusqu'aux bords.
                for (int k = 0; k < 140; k++) {
                    double a = k * 2.39996;
                    double r0 = far * (0.05 + 0.25 * fraction(k * 1.31));
                    double r1 = r0 + far * (0.15 + 0.5 * fraction(k * 2.77));
                    double bend = 0.15 * Math.sin(k + time * 0.2);
                    g.setStroke(k % 3 == 0 ? Color.web("#ffb35a") : k % 3 == 1 ? Color.web("#ff4a5a") : Color.web("#d15aff"));
                    g.setLineWidth(0.6 + 1.6 * fraction(k * 4.1));
                    g.setGlobalAlpha(0.18 + 0.25 * fraction(k * 5.3));
                    g.strokePolyline(new double[] {cx + r0 * Math.cos(a), cx + (r0 + r1) / 2 * Math.cos(a + bend), cx + r1 * Math.cos(a + bend * 2)},
                            new double[] {cy + r0 * Math.sin(a), cy + (r0 + r1) / 2 * Math.sin(a + bend), cy + r1 * Math.sin(a + bend * 2)}, 3);
                }
                // Le reste de l'étoile : un pulsar minuscule qui bat.
                double beat = 0.6 + 0.4 * Math.sin(time * 6);
                g.setGlobalAlpha(1);
                g.setFill(new javafx.scene.paint.RadialGradient(0, 0, cx, cy, small * 0.08, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, tint(Color.WHITE, 0.9 * beat)), new javafx.scene.paint.Stop(1, tint(Color.web("#ff9a3c"), 0))));
                g.fillOval(cx - small * 0.08, cy - small * 0.08, small * 0.16, small * 0.16);
            }
            default -> {
                double cell = Math.max(28, small / 9);
                for (double y = 0; y < h + cell; y += cell * 0.866) {
                    int row = (int) Math.round(y / (cell * 0.866));
                    for (double x = row % 2 == 0 ? 0 : cell / 2; x < w + cell; x += cell) {
                        double[] xs = new double[6], ys = new double[6];
                        for (int k = 0; k < 6; k++) {
                            double a = Math.PI / 6 + k * Math.PI / 3;
                            xs[k] = x + cell * 0.5 * Math.cos(a);
                            ys[k] = y + cell * 0.5 * Math.sin(a);
                        }
                        double glint = fraction(x * 0.37 + y * 0.71);
                        double lit = Math.max(0, Math.sin(time * 0.8 + glint * 20));
                        g.setGlobalAlpha(0.03 + 0.12 * lit * glint);
                        g.setFill(glint > 0.5 ? Color.web("#9fd8ff") : Color.web("#d6c8ff"));
                        g.fillPolygon(xs, ys, 6);
                        g.setGlobalAlpha(0.12);
                        g.setStroke(Color.web("#9fd8ff"));
                        g.setLineWidth(0.7);
                        g.strokePolyline(new double[] {xs[0], xs[1], xs[2], xs[3], xs[4], xs[5], xs[0]},
                                new double[] {ys[0], ys[1], ys[2], ys[3], ys[4], ys[5], ys[0]}, 7);
                    }
                }
            }
        }
        g.setGlobalAlpha(1);
    }

    /** L'espace créé, rempli à la façon du skin : un voile d'aurore, une boule de feu, un cristal taillé. */
    private void skinSpace(GraphicsContext g, double x, double y, double r, idle.core.universe.Skin look) {
        g.setGlobalAlpha(1);
        switch (look) {
            case BANG_AURORA -> g.setFill(new javafx.scene.paint.RadialGradient(0, 0, x, y, r, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, Color.web("#0f3a3a")), new javafx.scene.paint.Stop(0.7, Color.web("#122a40")),
                    new javafx.scene.paint.Stop(1, Color.web("#2a1a4a"))));
            case BANG_SUPERNOVA -> g.setFill(new javafx.scene.paint.RadialGradient(0, 0, x, y, r, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, Color.web("#3a1a10")), new javafx.scene.paint.Stop(0.75, Color.web("#2a0c12")),
                    new javafx.scene.paint.Stop(1, Color.web("#5a1a10"))));
            default -> g.setFill(new javafx.scene.paint.RadialGradient(0, 0, x - r * 0.3, y - r * 0.3, r * 1.3, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, Color.web("#1c3050")), new javafx.scene.paint.Stop(1, Color.web("#0a1428"))));
        }
        if (look == idle.core.universe.Skin.BANG_CRYSTAL) {
            // Un cristal à douze faces au lieu d'un disque, et ses facettes.
            int sides = 12;
            double[] xs = new double[sides], ys = new double[sides];
            for (int k = 0; k < sides; k++) {
                double a = time * 0.05 + 2 * Math.PI * k / sides;
                xs[k] = x + r * Math.cos(a);
                ys[k] = y + r * Math.sin(a);
            }
            g.fillPolygon(xs, ys, sides);
            g.setStroke(Color.web("#9fd8ff"));
            g.setLineWidth(1);
            for (int k = 0; k < sides; k += 2) {
                g.setGlobalAlpha(0.18);
                g.strokeLine(x, y, xs[k], ys[k]);
            }
        } else {
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
        }
        g.setGlobalAlpha(1);
    }

    /** Le bord de l'espace : une lisière qui ondule, un front de flammes, ou les arêtes du cristal. */
    private void skinEdge(GraphicsContext g, double x, double y, double r, idle.core.universe.Skin look) {
        int steps = 120;
        double[] xs = new double[steps + 1], ys = new double[steps + 1];
        switch (look) {
            case BANG_AURORA -> {
                for (int band = 0; band < 2; band++) {
                    for (int k = 0; k <= steps; k++) {
                        double a = 2 * Math.PI * k / steps;
                        double rr = r * (1 + 0.025 * Math.sin(a * 9 + time * (1 + band) + band * 2));
                        xs[k] = x + rr * Math.cos(a);
                        ys[k] = y + rr * Math.sin(a);
                    }
                    g.setStroke(band == 0 ? Color.web("#5dffa8") : Color.web("#b77bff"));
                    g.setLineWidth(band == 0 ? 3 : 1.5);
                    g.setGlobalAlpha(band == 0 ? 0.7 : 0.9);
                    g.strokePolyline(xs, ys, steps + 1);
                }
            }
            case BANG_SUPERNOVA -> {
                for (int k = 0; k <= steps; k++) {
                    double a = 2 * Math.PI * k / steps;
                    double flame = 0.04 * (0.5 + 0.5 * Math.sin(a * 23 + time * 3)) + 0.03 * fraction(k * 7.7 + Math.floor(time * 4));
                    double rr = r * (1 + (k % 2 == 0 ? flame : 0));
                    xs[k] = x + rr * Math.cos(a);
                    ys[k] = y + rr * Math.sin(a);
                }
                g.setStroke(Color.web("#ff5a2a"));
                g.setLineWidth(4);
                g.setGlobalAlpha(0.45);
                g.strokePolyline(xs, ys, steps + 1);
                g.setStroke(Color.web("#ffd27f"));
                g.setLineWidth(1.5);
                g.setGlobalAlpha(0.95);
                g.strokePolyline(xs, ys, steps + 1);
            }
            default -> {
                int sides = 12;
                double[] cx = new double[sides + 1], cy = new double[sides + 1];
                for (int k = 0; k <= sides; k++) {
                    double a = time * 0.05 + 2 * Math.PI * k / sides;
                    cx[k] = x + r * Math.cos(a);
                    cy[k] = y + r * Math.sin(a);
                }
                g.setStroke(Color.web("#e8f8ff"));
                g.setLineWidth(2);
                g.setGlobalAlpha(0.9);
                g.strokePolyline(cx, cy, sides + 1);
                // Des éclats de lumière aux sommets.
                g.setFill(Color.WHITE);
                for (int k = 0; k < sides; k++) {
                    double glow = Math.max(0, Math.sin(time * 1.5 + k * 1.3));
                    double size = 2 + 5 * glow;
                    g.setGlobalAlpha(0.3 + 0.7 * glow);
                    g.fillPolygon(new double[] {cx[k], cx[k] + size / 3, cx[k], cx[k] - size / 3}, new double[] {cy[k] - size, cy[k], cy[k] + size, cy[k]}, 4);
                }
            }
        }
        g.setGlobalAlpha(1);
    }

    /** Les étoiles sous un skin du Big Bang : vertes et douces, à branches, ou en losange. */
    private void skinStars(GraphicsContext g, double w, double h, idle.core.universe.Skin look) {
        for (int star = 0; star < STARS; star++) {
            double x = STAR_X[star] * w, y = STAR_Y[star] * h;
            double twinkle = Math.max(0.05, Math.min(1, 0.3 + 0.25 * Math.sin(time * STAR_PACE[star] * 1.5 + star)));
            g.setGlobalAlpha(twinkle);
            switch (look) {
                case BANG_AURORA -> {
                    g.setFill(Color.web("#d8ffe9"));
                    g.fillOval(x - 0.6, y - 0.6, 1.2, 1.2);
                }
                case BANG_SUPERNOVA -> {
                    if (star % 6 == 0) {
                        g.setStroke(Color.web("#ffe6c2"));
                        g.setLineWidth(0.8);
                        g.strokeLine(x - 4, y, x + 4, y);
                        g.strokeLine(x, y - 4, x, y + 4);
                    } else {
                        g.setFill(Color.web("#ffd0b0"));
                        g.fillOval(x - 0.5, y - 0.5, 1, 1);
                    }
                }
                default -> {
                    double size = star % 9 == 0 ? 3.5 : 1.8;
                    g.setFill(Color.web("#e8f8ff"));
                    g.fillPolygon(new double[] {x, x + size / 2, x, x - size / 2}, new double[] {y - size, y, y + size, y}, 4);
                }
            }
        }
        g.setGlobalAlpha(1);
    }

    private static double fraction(double seed) {
        double value = Math.sin(seed) * 43758.5453;
        return value - Math.floor(value);
    }

    /** Une suite pseudo-aléatoire fixe, tirée une fois : la place d'une étoile et son rythme ne dépendent que de son rang. */
    private static final double[] STAR_X = new double[STARS];
    private static final double[] STAR_Y = new double[STARS];
    private static final double[] STAR_PACE = new double[STARS];

    static {
        for (int star = 0; star < STARS; star++) {
            STAR_X[star] = fraction(star * 12.9898);
            STAR_Y[star] = fraction(star * 78.233);
            STAR_PACE[star] = 0.4 + fraction(star * 3.7);
        }
    }
}

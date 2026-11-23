package idle.ui.bigbang;

import idle.core.Game;
import idle.core.universe.Skin;
import javafx.scene.paint.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.*;

/**
 * L'espace que l'expansion de la matière a créé, vu de dessus, avec les molécules qui l'occupent.
 *
 * <p>L'espace est un disque dont la surface vaut l'espace créé ({@code GameState.space()}) : son bord recule à mesure
 * que le temps passe. Les molécules s'y rangent en spirale depuis le centre, à l'intérieur du cœur utilisé : le disque
 * dont la surface vaut l'espace qu'elles occupent et celui que réservent leurs lieux de rassemblement ({@link
 * Game#usedSpace()}). Le cœur grossit donc avec ce que fait le joueur, le bord avec le temps, et l'anneau vide entre
 * les deux est l'espace libre.
 *
 * <p>Les molécules rassemblées ({@link Game#formSubstance(String)}) occupent le centre, et celles d'une même sorte y
 * forment un seul amas : une surface pleine, sans fente ni trou entre ses molécules, de la couleur de la molécule et
 * cernée de la couleur de son état. Les états les plus serrés sont servis les premiers, donc au plus près du centre :
 * les cristaux, puis les liquides, et les gaz au bord, comme une planète de son noyau à son atmosphère. Les amas se
 * cognent sans se chevaucher : chacun s'arrête là où il touche son voisin, comme des bulles serrées les unes contre les
 * autres. Les molécules qui ne sont pas rassemblées restent au-delà, dans l'ordre de leur création.
 *
 * <p>Au cœur de tout, les assemblages formés ({@link Game#formAssembly(String)}) : chacun est un bloc de plusieurs
 * centaines de molécules, bien plus gros qu'un amas. Il ne se dessine pas comme eux : son corps est opaque, de la
 * couleur de sa matière, sous un contour clair et épais ; ses molécules de plusieurs sortes y sont mêlées, chacune
 * réduite à un grain anguleux de sa couleur ; et il porte son nom. On ne revoit ses molécules qu'en s'approchant tout
 * près.
 *
 * <p>Et les astres formés ({@link Game#formBody(String)}), qui sont faits de leurs assemblages. Chacun a une clairière
 * ronde, de la taille de ce qu'il contient : l'astre lui-même ({@link BodyArt}) en occupe le milieu, et les blocs de
 * ses assemblages restent autour de lui, en couronne, chacun avec sa couleur, son contour, ses grains et son nom. Une
 * planète est un globe au milieu de ses roches, de ses eaux et de ses airs ; une étoile, une boule de feu dans son
 * hydrogène. Le trou noir supermassif tient le centre de tout, les plus gros astres autour de lui, et aucun ne touche
 * ni un autre astre, ni un bloc, ni un amas. Un astre reste net de très loin, quand tout le reste est devenu du gaz.
 *
 * <p>Enfin la galaxie ({@link Game#formGalaxy()}) : une fois formée, tout cela s'enroule en bras de spirale ({@link
 * Spiral}) autour du trou noir supermassif. Chaque molécule y devient un point, de sa couleur mêlée à celle des étoiles
 * ; les astres se rangent le long des bras, les plus gros près du centre ; l'ensemble tourne, ses bras à la traîne. Le
 * joueur peut toujours revenir à la matière telle qu'elle était rangée ({@link #showGalaxy(boolean)}).
 *
 * <p>Le cœur est juste, les dessins ne le sont pas : les molécules s'y partagent la place à parts égales, quelle que
 * soit la taille de chacune. Une molécule lourde n'est donc pas dessinée beaucoup plus grande qu'une légère, et aucun
 * dessin ne dépasse du cœur, donc de l'espace.
 *
 * <p>La vue se règle comme une caméra :
 * <ul>
 * <li>la molette, ou {@link #zoom(double)}, rapproche ou éloigne ;</li>
 * <li>on ne peut pas reculer plus loin que ce qu'il faut pour voir tout l'espace : plus il y en a, plus on peut
 * dézoomer ({@link #fit()} y va d'un coup) ;</li>
 * <li>tirer avec la souris déplace la vue, sans sortir de l'espace.</li>
 * </ul> De très loin, la matière est un gaz : chaque molécule n'est plus qu'une bouffée de sa couleur, et les bouffées
 * se fondent en nuages qui remuent lentement ({@link #mist()}). La structure reste pourtant lisible à travers eux :
 * par-dessus le gaz, net, chaque bloc d'assemblage est un disque plein de la couleur de sa matière sous son contour
 * clair, et chaque amas un anneau fin de la couleur de son état, avec une légère lueur de cette couleur qui monte vers
 * son bord. Ces marques apparaissent avec le gaz et s'effacent avec lui ({@link #drawMistBodies}) : de près, le dessin
 * des amas et des blocs reprend. En s'approchant, le gaz se dissipe et laisse voir les amas et leurs molécules :
 * d'abord des points, puis leurs atomes, leurs liaisons et leurs symboles ({@link MoleculeArt}). Tant que le joueur n'a
 * pas rapproché la vue, elle suit l'expansion et montre toujours tout l'espace.
 *
 * <p>L'ensemble tourne très lentement, et chaque molécule sur elle-même : c'est une image de l'espace, pas une mesure.
 *
 * <p>Le code est rangé en parties : une chaîne de classes abstraites, une par thème, dont celle-ci est le bout ; le
 * reste du programme ne voit que {@code SpaceView}. De la base au bout : {@link SpaceViewBase}, {@link
 * SpaceViewPlacement}, {@link SpaceViewPacking}. Quand une partie a besoin d'une méthode définie plus loin dans la
 * chaîne, elle la déclare abstraite en tête, sous « Ce que fournissent les parties suivantes ».
 */
public final class SpaceView extends SpaceViewScene {
    SpaceView(Game game) {
        super(game);
    }

    /** L'espace créé, rempli à la façon du skin : un voile d'aurore, une boule de feu, un cristal taillé. */
    void skinSpace(GraphicsContext g, double x, double y, double r, Skin look) {
        if (SpaceSkinArt.draws(look)) {
            SpaceSkinArt.space(g, x, y, r, look, time);
            return;
        }
        g.setGlobalAlpha(1);
        switch (look) {
            case BANG_AURORA -> g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#0f3a3a")), new Stop(0.7, Color.web("#122a40")),
                    new Stop(1, Color.web("#2a1a4a"))));
            case BANG_SUPERNOVA -> g.setFill(new RadialGradient(0, 0, x, y, r, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#3a1a10")), new Stop(0.75, Color.web("#2a0c12")),
                    new Stop(1, Color.web("#5a1a10"))));
            default -> g.setFill(new RadialGradient(0, 0, x - r * 0.3, y - r * 0.3, r * 1.3, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#1c3050")), new Stop(1, Color.web("#0a1428"))));
        }
        if (look == Skin.BANG_CRYSTAL) {
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
    void skinEdge(GraphicsContext g, double x, double y, double r, Skin look) {
        if (SpaceSkinArt.draws(look)) {
            SpaceSkinArt.edge(g, x, y, r, look, time);
            return;
        }
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
    void skinStars(GraphicsContext g, double w, double h, Skin look) {
        if (SpaceSkinArt.draws(look)) {
            SpaceSkinArt.stars(g, w, h, look, time, STAR_X, STAR_Y, STAR_PACE);
            return;
        }
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
}

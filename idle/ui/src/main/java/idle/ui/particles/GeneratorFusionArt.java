package idle.ui.particles;

import idle.core.universe.Skin;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.util.Duration;

/**
 * Les trois skins de la fusion des générateurs (version 0.192), à côté de {@link GeneratorPane} pour le garder
 * lisible. Chacun remplace le glissement classique vers le centre par un mouvement à lui, de même durée et qui
 * finit au même endroit, à la même taille :
 * <ul>
 * <li>« Tourbillon » : chaque générateur fait un tour complet autour du centre en s'y enroulant, de plus en plus
 * vite, et tourne sur lui-même ;</li>
 * <li>« Aspiration » : il s'étire vers le centre comme de la pâte, long et étroit dans le sens de sa course, puis
 * s'y engouffre ;</li>
 * <li>« Éclat » : il se brise, saute de son côté en tournant et en vacillant, puis les morceaux se rassemblent au
 * centre.</li>
 * </ul>
 */
final class GeneratorFusionArt {

    /** La part du temps où les éclats s'éloignent, avant de se rassembler. */
    static final double SHATTER_OUT = 0.4;

    private GeneratorFusionArt() {
    }

    /** Ajoute à {@code all} le mouvement de chaque vue vers le centre, sous ce skin. */
    static void animate(Skin skin, ParallelTransition all, List<ParticleView> views, double centerX, double centerY, double seconds, double endScale) {
        int index = 0;
        for (ParticleView view : views) {
            double fromX = view.getLayoutX() + view.getWidth() / 2;
            double fromY = view.getLayoutY() + view.getHeight() / 2;
            all.getChildren().add(new Move(skin, view, centerX - fromX, centerY - fromY, seconds, endScale, index++));
        }
    }

    /** Le mouvement d'une vue : la position, l'échelle, la rotation et l'opacité suivent l'avancement. */
    private static final class Move extends Transition {
        private final Skin skin;
        private final ParticleView view;
        /** Le chemin du centre de la vue jusqu'au centre de la zone, en pixels. */
        private final double toX;
        private final double toY;
        private final double endScale;
        /** Le côté et l'allure propres à cette vue : le sens du tourbillon, la direction de l'éclat. */
        private final int index;

        Move(Skin skin, ParticleView view, double toX, double toY, double seconds, double endScale, int index) {
            this.skin = skin;
            this.view = view;
            this.toX = toX;
            this.toY = toY;
            this.endScale = endScale;
            this.index = index;
            setCycleDuration(Duration.seconds(seconds));
            setInterpolator(Interpolator.LINEAR);
        }

        @Override
        protected void interpolate(double frac) {
            double eased = frac * frac;     // départ lent, arrivée rapide, comme le classique
            switch (skin) {
                case GENERATORS_SWIRL -> {
                    // Le rayon se réduit, l'angle fait un tour complet dans le sens de chaque vue.
                    double distance = Math.hypot(toX, toY);
                    double angle = Math.atan2(-toY, -toX) + (index % 2 == 0 ? 1 : -1) * 2 * Math.PI * eased;
                    double radius = distance * (1 - eased);
                    view.setTranslateX(toX + radius * Math.cos(angle));
                    view.setTranslateY(toY + radius * Math.sin(angle));
                    double scale = 1 - (1 - endScale) * eased;
                    view.setScaleX(scale);
                    view.setScaleY(scale);
                    view.setRotate((index % 2 == 0 ? 1 : -1) * 360 * eased);
                }
                case GENERATORS_SUCTION -> {
                    view.setTranslateX(toX * eased);
                    view.setTranslateY(toY * eased);
                    // Long dans le sens de la course, étroit en travers, puis tout rentre au centre.
                    double stretch = 1 + 1.1 * Math.sin(Math.PI * frac);
                    double shrink = 1 - (1 - endScale) * eased;
                    view.setRotate(Math.toDegrees(Math.atan2(toY, toX)) - 90);
                    view.setScaleX(shrink * (1 - 0.6 * Math.sin(Math.PI * frac)));
                    view.setScaleY(shrink * stretch);
                }
                default -> {
                    // L'éclat : d'abord un saut de côté en vacillant, puis les morceaux se rassemblent.
                    double away = 0.35 + 0.1 * (index % 3);
                    double side = (index % 2 == 0 ? 1 : -1) * 0.25;
                    double outX = -toX * away - toY * side, outY = -toY * away + toX * side;
                    if (frac < SHATTER_OUT) {
                        double t = frac / SHATTER_OUT;
                        double burst = Math.sin(Math.PI / 2 * t);
                        view.setTranslateX(outX * burst);
                        view.setTranslateY(outY * burst);
                        view.setScaleX(1 - 0.3 * t);
                        view.setScaleY(1 - 0.3 * t);
                        view.setOpacity(0.6 + 0.4 * Math.abs(Math.cos(t * 9 + index)));
                    } else {
                        double t = (frac - SHATTER_OUT) / (1 - SHATTER_OUT);
                        double gather = t * t;
                        view.setTranslateX(outX + (toX - outX) * gather);
                        view.setTranslateY(outY + (toY - outY) * gather);
                        double scale = 0.7 - (0.7 - endScale) * gather;
                        view.setScaleX(scale);
                        view.setScaleY(scale);
                        view.setOpacity(1);
                    }
                    view.setRotate((index % 2 == 0 ? 1 : -1) * 540 * frac);
                }
            }
        }
    }
}

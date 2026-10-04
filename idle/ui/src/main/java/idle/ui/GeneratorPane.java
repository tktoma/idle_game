package idle.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * Zone qui affiche les générateurs et les répartit dans l'espace disponible,
 * par-dessus un fond de poussières.
 *
 * <p>Les générateurs apparaissent au fur et à mesure, de gauche à droite.
 * À chaque changement (nouveau générateur, fenêtre redimensionnée), la grille est
 * recalculée pour leur donner la plus grande taille possible.
 *
 * <p>À la fusion, tous les générateurs glissent vers le centre en rétrécissant,
 * puis disparaissent : le générateur suivant naît à cet endroit.
 */
final class GeneratorPane extends Pane {

    /** Un générateur seul dans une grande fenêtre ne dépasse pas cette taille, en pixels. */
    private static final double MAX_VIEW_SIZE = 320;

    /** Durée du regroupement des générateurs pendant la fusion, en secondes. */
    private static final double FUSION_SECONDS = 1.4;
    /** Taille des générateurs à la fin du regroupement, par rapport à leur taille normale. */
    private static final double FUSION_SCALE = 0.35;

    private final AmbientView ambient = new AmbientView();
    private final List<ParticleView> views = new ArrayList<>();
    private boolean fusing = false;

    GeneratorPane() {
        setMinSize(0, 0);       // la zone peut rétrécir librement avec la fenêtre
        setPrefSize(800, 360);
        getChildren().add(ambient);
    }

    List<ParticleView> views() {
        return views;
    }

    /** Fait avancer le fond animé. */
    void animateBackground(double dt) {
        ambient.frame(dt);
    }

    /** Vrai pendant l'animation de fusion. */
    boolean isFusing() {
        return fusing;
    }

    /**
     * Joue l'animation de fusion : les générateurs convergent vers le centre et s'y confondent.
     *
     * @param onFinished appelé quand ils ont disparu ; c'est là que le jeu applique la fusion
     */
    void fuse(Runnable onFinished) {
        if (fusing) return;
        fusing = true;

        double centerX = getWidth() / 2;
        double centerY = getHeight() / 2;
        ParallelTransition all = new ParallelTransition();
        for (ParticleView view : views) {
            TranslateTransition move = new TranslateTransition(Duration.seconds(FUSION_SECONDS), view);
            move.setToX(centerX - (view.getLayoutX() + view.getWidth() / 2));
            move.setToY(centerY - (view.getLayoutY() + view.getHeight() / 2));
            move.setInterpolator(Interpolator.EASE_IN); // départ lent, arrivée rapide

            ScaleTransition shrink = new ScaleTransition(Duration.seconds(FUSION_SECONDS), view);
            shrink.setToX(FUSION_SCALE);
            shrink.setToY(FUSION_SCALE);
            shrink.setInterpolator(Interpolator.EASE_IN);

            all.getChildren().add(move);
            all.getChildren().add(shrink);
        }
        all.setOnFinished(event -> {
            getChildren().removeAll(views);
            views.clear();
            fusing = false;
            onFinished.run();
        });
        all.play();
    }

    /**
     * Ajoute des générateurs jusqu'à en afficher {@code count}.
     * Chaque nouveau générateur joue son animation de naissance.
     */
    void setCount(int count) {
        boolean changed = false;
        while (views.size() < count) {
            ParticleView view = new ParticleView();
            views.add(view);
            getChildren().add(view);
            changed = true;
        }
        if (changed) requestLayout();
    }

    @Override
    protected void layoutChildren() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        // Le fond occupe toute la zone.
        ambient.setWidth(getWidth());
        ambient.setHeight(getHeight());
        ambient.relocate(0, 0);

        int count = views.size();
        if (count == 0) return;

        int columns = bestColumns(count, getWidth(), getHeight());
        int rows = rowsFor(count, columns);
        double size = Math.floor(viewSize(count, columns, getWidth(), getHeight()));

        // Grille centrée dans la zone ; le premier générateur reste en haut à gauche de la grille.
        double left = Math.floor((getWidth() - size * columns) / 2);
        double top = Math.floor((getHeight() - size * rows) / 2);
        for (int i = 0; i < count; i++) {
            ParticleView view = views.get(i);
            view.setWidth(size);
            view.setHeight(size);
            view.relocate(left + (i % columns) * size, top + (i / columns) * size);
        }
    }

    /**
     * Nombre de colonnes qui donne les plus grands générateurs.
     * À taille égale, on préfère plus de colonnes : les nouveaux partent vers la droite.
     */
    static int bestColumns(int count, double width, double height) {
        int best = 1;
        double bestSize = 0;
        for (int columns = 1; columns <= count; columns++) {
            double size = viewSize(count, columns, width, height);
            if (size >= bestSize - 1e-9) {
                bestSize = size;
                best = columns;
            }
        }
        return best;
    }

    /** Côté d'un générateur (carré) si on les range sur {@code columns} colonnes. */
    private static double viewSize(int count, int columns, double width, double height) {
        return Math.min(MAX_VIEW_SIZE, Math.min(width / columns, height / rowsFor(count, columns)));
    }

    private static int rowsFor(int count, int columns) {
        return (count + columns - 1) / columns;
    }
}

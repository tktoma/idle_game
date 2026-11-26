package idle.ui.common;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

/**
 * Version 0.307 : le défilement des onglets. La molette va {@link WheelScroll#SPEED} fois plus loin à chaque cran, la
 * barre verticale de droite est bien visible (feuille « defilement.css »), sa poignée se fait glisser, et un clic sur
 * le rail y saute directement au lieu d'avancer d'une page.
 */
public final class Scrolling {
    private Scrolling() {}

    /** Installe tout cela pour toute la fenêtre. */
    public static void install(Scene scene) {
        var css = Scrolling.class.getResource("/idle/ui/defilement.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        scene.addEventFilter(ScrollEvent.SCROLL, Scrolling::wheel);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, Scrolling::trackClick);
    }

    /**
     * Un cran de molette fait défiler la page la plus proche du pointeur qui peut encore aller dans ce sens. Ce qui
     * a sa propre molette (le zoom de l'espace, d'une dimension) la garde.
     */
    private static void wheel(ScrollEvent event) {
        if (event.getDeltaY() == 0 || !(event.getTarget() instanceof Node target)) return;
        for (Node node = target; node != null; node = node.getParent()) {
            if (node.getOnScroll() != null || ownWheel(node)) return;
            if (node instanceof ScrollPane pane && pane.getContent() != null && pane.getVbarPolicy() != ScrollPane.ScrollBarPolicy.NEVER) {
                double content = pane.getContent().getLayoutBounds().getHeight();
                double viewport = pane.getViewportBounds().getHeight();
                double value = (pane.getVvalue() - pane.getVmin()) / Math.max(1e-9, pane.getVmax() - pane.getVmin());
                if (!WheelScroll.canMove(value, event.getDeltaY(), content, viewport)) continue;
                double next = WheelScroll.next(value, event.getDeltaY(), content, viewport);
                pane.setVvalue(pane.getVmin() + next * (pane.getVmax() - pane.getVmin()));
                event.consume();
                return;
            }
        }
    }

    /** Les contrôles qui défilent ou changent de valeur à la molette par eux-mêmes : une liste, un texte, un curseur. */
    private static boolean ownWheel(Node node) {
        return node instanceof javafx.scene.control.ListView<?> || node instanceof javafx.scene.control.TableView<?>
                || node instanceof javafx.scene.control.TreeView<?> || node instanceof javafx.scene.control.TextArea
                || node instanceof javafx.scene.control.Slider || node instanceof javafx.scene.control.Spinner<?>;
    }

    /** Un clic sur le rail d'une barre verticale y place la poignée (la poignée elle-même se fait glisser). */
    private static void trackClick(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || !(event.getTarget() instanceof Node target)) return;
        if (!target.getStyleClass().contains("track")) return;
        Parent parent = target.getParent();
        if (!(parent instanceof ScrollBar bar) || bar.getOrientation() != Orientation.VERTICAL) return;
        double ratio = WheelScroll.jump(event.getY(), target.getLayoutBounds().getHeight());
        bar.setValue(bar.getMin() + ratio * (bar.getMax() - bar.getMin()));
        event.consume();
    }
}

package idle.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Une grille de tuiles de même taille : autant de colonnes que la largeur le permet, toutes de
 * la même largeur, et dans une rangée toutes de la même hauteur.
 *
 * <p>Elle range ses tuiles en rangées, de gauche à droite, dans l'ordre où elles ont été
 * ajoutées. Quand la largeur change, le nombre de colonnes suit. Une tuile cachée
 * ({@link #show(Region, boolean)}) ne laisse pas de trou : les suivantes se resserrent.
 */
final class TileGrid extends VBox {

    /**
     * Marge qu'il faut avoir en plus pour gagner une colonne. Sans elle, une barre de défilement
     * qui apparaît puis disparaît ferait hésiter la grille entre deux nombres de colonnes.
     */
    private static final double WIDEN_MARGIN = 24;

    private final List<Region> tiles = new ArrayList<>();
    private final java.util.Set<Region> hidden = new java.util.HashSet<>();
    private final double tileWidth;
    private final int maxColumns;
    private final double gap;
    private int columns;

    /**
     * @param tileWidth  largeur minimale d'une tuile ; elle s'élargit pour remplir sa rangée
     * @param maxColumns nombre maximal de colonnes, quelle que soit la largeur
     * @param gap        espace entre deux tuiles, dans les deux sens
     */
    TileGrid(double tileWidth, int maxColumns, double gap) {
        super(gap);
        this.tileWidth = tileWidth;
        this.maxColumns = maxColumns;
        this.gap = gap;
        this.columns = maxColumns;
        setAlignment(Pos.TOP_CENTER);
        setMinWidth(0);
        widthProperty().addListener((observable, before, after) -> fit(after.doubleValue()));
    }

    /** Ajoute une tuile à la suite des autres. */
    void add(Region tile) {
        tile.setMinWidth(0);
        tile.setPrefWidth(tileWidth);          // même largeur de départ : la place en trop se partage alors à égalité
        tile.setMaxWidth(Double.MAX_VALUE);
        tiles.add(tile);
        arrange();
    }

    /** Affiche ou cache une tuile ; cachée, elle ne prend pas de place dans la grille. */
    void show(Region tile, boolean visible) {
        if (visible == !hidden.contains(tile)) return;
        if (visible) hidden.remove(tile); else hidden.add(tile);
        arrange();
    }

    /** Nombre de colonnes actuel : pour les vérifications. */
    int columns() {
        return columns;
    }

    /** Choisit le nombre de colonnes qui tient dans {@code width}. */
    private void fit(double width) {
        if (width <= 0) return;
        int fitting = clamp((int) Math.floor((width + gap) / (tileWidth + gap)));
        int roomy = clamp((int) Math.floor((width - WIDEN_MARGIN + gap) / (tileWidth + gap)));
        // On resserre dès qu'il le faut ; on n'élargit que s'il y a franchement la place.
        int wanted = fitting < columns ? fitting : Math.max(columns, roomy);
        if (wanted != columns) {
            columns = wanted;
            arrange();
        }
    }

    private int clamp(int value) {
        return Math.max(1, Math.min(maxColumns, value));
    }

    /** Refait les rangées : une {@link HBox} par rangée, complétée de cases vides pour garder les largeurs. */
    private void arrange() {
        getChildren().clear();
        HBox row = null;
        int inRow = 0;
        for (Region tile : tiles) {
            if (hidden.contains(tile)) continue;
            if (row == null || inRow == columns) {
                row = new HBox(gap);
                row.setFillHeight(true);
                getChildren().add(row);
                inRow = 0;
            }
            HBox.setHgrow(tile, Priority.ALWAYS);
            row.getChildren().add(tile);
            inRow++;
        }
        // La dernière rangée, incomplète, reçoit des cases vides : ses tuiles gardent la largeur des autres.
        // Une seule rangée, elle, reste telle quelle : ses tuiles se partagent toute la largeur.
        if (row != null && getChildren().size() > 1) {
            for (; inRow < columns; inRow++) {
                Region blank = new Region();
                blank.setMinWidth(0);
                blank.setPrefWidth(tileWidth);
                blank.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(blank, Priority.ALWAYS);
                row.getChildren().add(blank);
            }
        }
    }
}

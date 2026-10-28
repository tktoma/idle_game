package idle.ui.pages;

import idle.core.molecules.Molecule;
import idle.ui.common.Card;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.TileGrid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Un album du profil (version 0.147), façon Pokédex : tout ce qu'un catalogue contient, une case chacun, numérotée.
 * Ce qui a été obtenu au moins une fois montre son nom, ce qui le décrit et le temps de jeu de la première fois ; le
 * reste n'est qu'un « ??? » éteint. En tête, le compte « obtenus / total ».
 *
 * <p>Les cases ne sont créées qu'à la première visite, et ne se remplissent de nouveau que lorsque le nombre de
 * découvertes change : un album ne coûte rien tant que rien de neuf n'arrive. L'album des molécules se filtre par rayon.
 */
final class AlbumPane extends VBox {

    /**
     * Une case de l'album.
     *
     * @param id   l'identifiant du catalogue, celui des maps de découvertes
     * @param name le nom, montré une fois découvert
     * @param line ce qui le décrit en quelques mots : formule, état, famille ou échelle
     * @param kind le rayon d'une molécule, pour le filtre ; {@code null} ailleurs
     */
    record Entry(String id, String name, String line, Molecule.Kind kind) {
    }

    private static final String FILTER_STYLE = "-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand;"
            + " -fx-background-radius: 12; -fx-border-radius: 12;";

    private final String color;
    private final String what;
    private final Supplier<List<Entry>> catalog;
    private final Supplier<Map<String, Double>> found;
    private final boolean filtered;
    private final Label counter = new Label();
    private final Label note = new Label();
    private final FlowPane filters = new FlowPane(6, 6);
    private final TileGrid grid = new TileGrid(170, 5, 6);
    private final Map<Entry, Card> cards = new LinkedHashMap<>();
    private final Map<Molecule.Kind, Button> kindButtons = new LinkedHashMap<>();
    private final Button allButton = new Button("Tous");
    private List<Entry> entries;
    /** Le nombre de découvertes au dernier remplissage, et si les cartes écrivaient leur signe ; −1 : à refaire. */
    private int filledWith = -1;
    private boolean filledSigns;
    /** Le rayon montré, ou {@code null} pour tous. */
    private Molecule.Kind shownKind;

    /**
     * @param color    la couleur de l'album, celle de l'onglet Big Bang
     * @param what     ce que compte l'album, au singulier : « molécule créée au moins une fois »
     * @param catalog  tout ce que l'album peut contenir, dans l'ordre du jeu
     * @param found    identifiant → temps de jeu de la première fois
     * @param filtered vrai pour proposer le filtre par rayon (les molécules)
     */
    AlbumPane(String color, String what, Supplier<List<Entry>> catalog, Supplier<Map<String, Double>> found, boolean filtered) {
        super(8);
        this.color = color;
        this.what = what;
        this.catalog = catalog;
        this.found = found;
        this.filtered = filtered;
        setAlignment(Pos.TOP_CENTER);
        counter.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Labels.note(note, 760);
        filters.setAlignment(Pos.CENTER);
        filters.setMaxWidth(860);
        grid.setMaxWidth(920);
        getChildren().addAll(counter, note);
        if (filtered) {
            allButton.setFocusTraversable(false);
            allButton.setOnAction(event -> filter(null));
            filters.getChildren().add(allButton);
            for (Molecule.Kind kind : Molecule.Kind.values()) {
                Button button = new Button(kind.label());
                button.setFocusTraversable(false);
                button.setOnAction(event -> filter(kind));
                kindButtons.put(kind, button);
                filters.getChildren().add(button);
            }
            getChildren().add(filters);
            paintFilters();
        }
        getChildren().add(grid);
    }

    /** La partie vient d'être remplacée : les cases seront remplies de nouveau à la prochaine visite. */
    void forget() {
        filledWith = -1;
    }

    /** Recopie l'album, s'il a changé depuis la dernière fois. */
    void refresh() {
        if (entries == null) build();
        Map<String, Double> seen = found.get();
        if (seen.size() == filledWith && filledSigns == Card.signsShown()) return;
        filledWith = seen.size();
        filledSigns = Card.signsShown();
        int known = 0;
        int number = 0;
        for (Map.Entry<Entry, Card> each : cards.entrySet()) {
            Entry entry = each.getKey();
            number++;
            Double at = seen.get(entry.id());
            if (at == null) {
                each.getValue().show(Card.State.LOCKED, "n° " + number, "", "???", "", "", "", "");
            } else {
                known++;
                each.getValue().show(Card.State.DONE, "n° " + number, "", entry.name(), entry.line(), "",
                        "la première fois à " + Format.duration(at) + " de jeu", "");
            }
        }
        counter.setText(known + " / " + entries.size());
        note.setText(known == 0 ? "Rien encore : chaque " + what + " viendra prendre sa case."
                : known == entries.size() ? "L'album est complet." : "Encore " + (entries.size() - known) + " à trouver : « ??? ».");
    }

    /** Ne montre que les molécules d'un rayon ; {@code null} les montre toutes. */
    void filter(Molecule.Kind kind) {
        if (!filtered) return;
        shownKind = kind;
        if (entries == null) build();
        for (Map.Entry<Entry, Card> each : cards.entrySet()) grid.show(each.getValue(), kind == null || each.getKey().kind() == kind);
        paintFilters();
    }

    private void build() {
        entries = List.copyOf(catalog.get());
        for (Entry entry : entries) {
            Card card = new Card(color);
            cards.put(entry, card);
            grid.add(card);
        }
        if (shownKind != null) filter(shownKind);
    }

    private void paintFilters() {
        allButton.setStyle(filterStyle(shownKind == null));
        kindButtons.forEach((kind, button) -> button.setStyle(filterStyle(kind == shownKind)));
    }

    private String filterStyle(boolean chosen) {
        return FILTER_STYLE + (chosen ? " -fx-text-fill: #0b0e14; -fx-background-color: " + color + "; -fx-border-color: " + color + ";"
                : " -fx-text-fill: #b9c7d6; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;");
    }

    /** Le compte en tête, « 3 / 120 » : pour les vérifications. */
    String counterText() {
        return counter.getText();
    }

    /** Le texte des cases affichées, dans l'ordre : pour les vérifications. */
    List<String> shownTexts() {
        List<String> texts = new ArrayList<>();
        java.util.Set<javafx.scene.layout.Region> shown = new java.util.HashSet<>(grid.shownTiles());
        for (Card card : cards.values()) if (shown.contains(card)) texts.add(card.text());
        return texts;
    }

    /** Le texte de la case de cet identifiant, ou une chaîne vide : pour les vérifications. */
    String textOf(String id) {
        for (Map.Entry<Entry, Card> each : cards.entrySet()) if (each.getKey().id().equals(id)) return each.getValue().text();
        return "";
    }
}

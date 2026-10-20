package idle.ui.bigbang;

import idle.core.Game;
import idle.core.molecules.KindCollection;
import idle.core.molecules.Molecule;
import idle.ui.GameApp;
import idle.ui.Readiness;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.FilterBar;
import idle.ui.common.TileGrid;
import idle.ui.probes.ProbesPane;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;

/**
 * La page du Big Bang : le choix des sous-pages, les filtres et le tri des molécules, le meilleur achat et les cartes.
 */
public abstract class BigBangPageControls extends BigBangPageBase {

    BigBangPageControls(Game game) {
        super(game);
    }


    /** Le bouton « Tout confier » ou « Tout retirer » de la sous-page des états : pour les vérifications. */
    public Button entrustAllButton() {
        return entrustAll;
    }

    /** Prépare un bouton de geste groupé : il fait tout ce qui est possible, puis la page se recopie. */
    void bulk(Button button, Runnable action) {
        button.setStyle(ZOOM_STYLE);
        button.setFocusTraversable(false);
        button.setOnAction(event -> {
            action.run();
            Readiness.of(game).compute();
            refresh();
        });
    }

    /** Montre un bouton de geste groupé une fois les gestes groupés acquis, avec le nombre de choses prêtes. */
    static void showBulk(Button button, boolean unlocked, String what, int ready) {
        button.setVisible(unlocked);
        button.setManaged(unlocked);
        if (!unlocked) return;
        button.setText(ready == 0 ? what + " : rien de prêt" : what + " (" + ready + ")");
        button.setDisable(ready == 0);
    }

    /**
     * Passe à la sous-page visible suivante ou précédente, en boucle, dans l'ordre où elles sont
     * rangées à l'écran : les paliers viennent après les améliorations.
     */
    public void step(int direction) {
        int[] order = {0, 1, 2, 7, 8, 3, 4, 5, 9, 6};
        Button[] tabs = {moleculesTab, expansionTab, upgradesTab, milestonesTab, probesTab, statesTab, assembliesTab, bodiesTab, systemsTab, galaxyTab};
        int at = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == selected) at = i;
        }
        for (int tries = 0; tries < order.length; tries++) {
            at = Math.floorMod(at + direction, order.length);
            if (tabs[at].isVisible()) {
                select(order[at]);
                refresh();
                return;
            }
        }
    }

    /** Une zone qui défile en hauteur seulement, sans fond propre. */
    static void transparent(ScrollPane scroll) {
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
    }

    /** Une phrase d'explication : discrète, centrée, qui passe à la ligne. */
    static void note(Label label) {
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(760);
    }

    /**
     * Affiche une sous-page : 0 = les molécules, 1 = l'expansion de la matière, 2 = les améliorations,
     * 3 = les états de la matière, 4 = les assemblages, 5 = les astres, 6 = l'univers (galaxie, amas de galaxies, univers),
     * 7 = les paliers de Big Bang, 8 = les sondes, 9 = les systèmes solaires.
     */
    public void select(int index) {
        selected = index;
        // Une recherche en cours d'écriture ne garde pas le clavier d'une sous-page à l'autre.
        if (moleculeFilter != null) moleculeFilter.release();
        moleculesScroll.setVisible(index == 0);
        expansionPane.setVisible(index == 1);
        upgradesScroll.setVisible(index == 2);
        statesScroll.setVisible(index == 3);
        assembliesScroll.setVisible(index == 4);
        bodiesPane.setVisible(index == 5);
        bodiesScroll.setVisible(index == 5);
        galaxyPane.setVisible(index == 6);
        milestonesScroll.setVisible(index == 7);
        probesScroll.setVisible(index == 8);
        systemsScroll.setVisible(index == 9);
        systemsTab.setStyle(subTabStyle(index == 9));
        probesTab.setStyle(subTabStyle(index == 8));
        moleculesTab.setStyle(subTabStyle(index == 0));
        expansionTab.setStyle(subTabStyle(index == 1));
        upgradesTab.setStyle(subTabStyle(index == 2));
        statesTab.setStyle(subTabStyle(index == 3));
        assembliesTab.setStyle(subTabStyle(index == 4));
        bodiesTab.setStyle(subTabStyle(index == 5));
        galaxyTab.setStyle(subTabStyle(index == 6));
        milestonesTab.setStyle(subTabStyle(index == 7));
    }

    /** Les sondes : pour les vérifications et le raccourci. */
    public ProbesPane probesPane() {
        return probesPane;
    }

    /** La liste de courses : pour les vérifications. */
    public ShoppingPane shoppingPane() {
        return shoppingPane;
    }

    /** La sous-page affichée, numérotée comme dans {@link #select(int)}. */
    public int selected() {
        return selected;
    }

    private static String subTabStyle(boolean selected) {
        return SUB_TAB_STYLE + (selected
                ? " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-border-color: transparent transparent "
                        + GameApp.BIG_BANG_COLOR + " transparent;"
                : " -fx-text-fill: #8fa3b8; -fx-border-color: transparent;");
    }

    /** Affiche un rayon du catalogue ; ses cartes sont construites à la première visite. */
    public void selectKind(Molecule.Kind wanted) {
        kind = wanted;
        favoritesShown = false;
        TileGrid grid = grids.computeIfAbsent(wanted, this::build);
        gridHolder.getChildren().clear();
        gridHolder.getChildren().add(grid);
        bestAt = 0;
        sortShownGrid();
        paintChips();
    }

    /** Affiche les molécules épinglées, tous rayons confondus. */
    public void selectFavorites() {
        favoritesShown = true;
        gridHolder.getChildren().clear();
        gridHolder.getChildren().add(favoriteGrid);
        paintChips();
    }

    /** Vrai si la page montre les molécules épinglées plutôt qu'un rayon : pour les vérifications. */
    public boolean showsFavorites() {
        return favoritesShown;
    }

    private void paintChips() {
        kindChips.forEach((each, chip) -> chip.setStyle(chipStyle(each == kind && !favoritesShown)));
        favoritesChip.setStyle(chipStyle(favoritesShown));
    }

    private static String chipStyle(boolean chosen) {
        return CHIP_STYLE + (chosen
                ? " -fx-text-fill: #10151f; -fx-background-color: " + GameApp.BIG_BANG_COLOR + "; -fx-border-color: #ffffff;"
                : " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-background-color: #121923; -fx-border-color: "
                        + GameApp.BIG_BANG_COLOR + "66;");
    }

    /** Vrai si cette molécule passe les filtres : son état, ce qu'elle donne, et le texte cherché. */
    public boolean shown(Molecule molecule) {
        Molecule.State state = stateFilter.selected();
        if (state != null && molecule.state() != state) return false;
        Gives gives = givesFilter.selected();
        if (gives != null && gives != gives(molecule)) return false;
        Use use = useFilter.selected();
        if (use == Use.CREATABLE && !game.canCreateMolecule(molecule.id())) return false;
        if (use == Use.WANTED && !game.shoppingList().wants(molecule)) return false;
        return moleculeFilter.matches(searchTexts.computeIfAbsent(molecule,
                each -> FilterBar.normalized(each.name() + " " + each.formula() + " " + each.id())));
    }

    /** Ce que donne une molécule, pour le filtre ; {@code null} si elle ne donne rien. */
    private static Gives gives(Molecule molecule) {
        return switch (molecule.bonus()) {
            case Molecule.Boost boost -> switch (boost.stat()) {
                case SPACE -> Gives.SPACE;
                case PARTICLES -> Gives.PARTICLES;
                case ATOMS -> Gives.ATOMS;
                case DARK_GROWTH -> Gives.DARK_GROWTH;
            };
            case Molecule.Uncap uncap -> Gives.UNCAP;
            case null -> null;
        };
    }

    /** Nombre de molécules d'un rayon qui passent les filtres. */
    private int shownIn(Molecule.Kind wanted) {
        int found = 0;
        for (Molecule molecule : game.molecules()) {
            if (molecule.kind() == wanted && shown(molecule)) found++;
        }
        return found;
    }

    /**
     * Un filtre ou la recherche vient de changer. Si plus rien ne correspond dans le rayon affiché,
     * la page passe au premier rayon ouvert où quelque chose correspond : on cherche une molécule,
     * pas un rayon.
     */
    void moleculeFilterChanged() {
        if (useFilter == null) return;      // les pastilles se peignent pendant la construction de la page
        if (moleculeFilter.active() && !favoritesShown && shownIn(kind) == 0) {
            for (Molecule.Kind each : Molecule.Kind.values()) {
                if (game.isMoleculeKindUnlocked(each) && shownIn(each) > 0) {
                    selectKind(each);
                    break;
                }
            }
        }
        refresh();
    }

    /** La barre de recherche des molécules : pour les vérifications. */
    public FilterBar moleculeFilter() {
        return moleculeFilter;
    }

    /** Le rayon affiché : pour les vérifications. */
    public Molecule.Kind kind() {
        return kind;
    }

    /** Les cartes d'un rayon, dans l'ordre du catalogue, chacune avec le dessin de sa molécule. */
    private TileGrid build(Molecule.Kind wanted) {
        TileGrid grid = new TileGrid(200, 4, 8);
        for (Molecule molecule : game.molecules()) {
            if (molecule.kind() != wanted) continue;
            Card card = moleculeCard(molecule);
            cards.put(molecule, card);
            grid.add(card);
        }
        return grid;
    }

    /**
     * Range les cartes du rayon affiché : par rendement décroissant si le tri est demandé, sinon
     * dans l'ordre du catalogue. Le tri n'est refait qu'ici (au clic sur le bouton, à l'ouverture
     * d'un rayon) : refait à chaque image, il ferait glisser les cartes sous la souris.
     */
    void sortShownGrid() {
        TileGrid grid = grids.get(kind);
        if (grid == null) return;
        if (!sortedByYield) {
            grid.unsort();
            return;
        }
        Map<javafx.scene.layout.Region, Double> yields = new java.util.HashMap<>();
        cards.forEach((molecule, card) -> {
            if (molecule.kind() == kind) yields.put(card, game.moleculeYield(molecule.id()));
        });
        grid.sort((a, b) -> Double.compare(yields.getOrDefault(b, 0.0), yields.getOrDefault(a, 0.0)));
    }

    /** Vrai si les cartes des molécules sont rangées par rendement : pour les vérifications. */
    public boolean sortedByYield() {
        return sortedByYield;
    }

    /** Le bouton du tri et celui qui crée le meilleur achat : pour les vérifications. */
    /** Le bouton de la réservation par recette : pour les vérifications. */
    public Button reservationButton() {
        return reservationButton;
    }

    public Button sortButton() {
        return sortButton;
    }

    public Button bestButton() {
        return bestButton;
    }

    /** La ligne du meilleur achat, ou un texte vide quand elle est cachée : pour les vérifications. */
    public String bestText() {
        return best == null ? "" : bestLabel.getText();
    }

    /** Largeur de la barre du meilleur achat, et du bouton « Créer » : fixes pour que le bouton reste sous la souris. */
    static final double BEST_BAR_WIDTH = 760;
    static final double BEST_BUTTON_WIDTH = 96;

    /** Le meilleur achat du moment, tous rayons ouverts confondus : pour les vérifications. */
    public Molecule best() {
        return best;
    }

    /** Les molécules du rayon affiché, dans l'ordre où leurs cartes sont rangées : pour les vérifications. */
    public java.util.List<Molecule> shownOrder() {
        java.util.List<Molecule> order = new java.util.ArrayList<>();
        TileGrid grid = grids.get(kind);
        if (grid == null) return order;
        for (javafx.scene.layout.Region tile : grid.shownTiles()) {
            cards.forEach((molecule, card) -> {
                if (card == tile) order.add(molecule);
            });
        }
        return order;
    }

    /**
     * Cherche le meilleur achat ({@link Game#bestMolecule()}) et le meilleur du rayon affiché. Le
     * calcul parcourt tout le catalogue : il n'est refait que deux fois par seconde, ou dès qu'une
     * molécule a été créée.
     */
    void findBest() {
        long now = System.nanoTime();
        int version = game.state().moleculesVersion();
        // Le meilleur achat connu n'est plus créable (éléments ou espace dépensés ailleurs) : on cherche tout de suite.
        boolean stale = best != null && !game.canCreateMolecule(best.id());
        if (bestAt != 0 && now - bestAt < BEST_EVERY_NANOS && version == bestVersion && !stale) return;
        bestAt = now;
        bestVersion = version;
        best = game.bestMolecule();
        bestHere = null;
        double top = 0;
        for (Molecule molecule : game.molecules()) {
            if (molecule.kind() != kind) continue;
            double yield = game.moleculeYield(molecule.id());
            if (yield <= top || !game.canCreateMolecule(molecule.id())) continue;
            bestHere = molecule;
            top = yield;
        }
    }

    /** La comète et son bouton : pour les vérifications. */
    public Button cometButton() {
        return cometButton;
    }

    /** Les défis de Big Bang : pour les vérifications. */
    public BangChallengesPane bangChallenges() {
        return bangChallenges;
    }

    /** La ligne de la collection du rayon affiché, ou un texte vide : pour les vérifications. */
    public String collectionText() {
        return collectionLabel.isVisible() ? collectionLabel.getText() : "";
    }

    /**
     * Où en est la collection d'un rayon, et ce qu'elle rapporte : « Collection Sels : 25/61 sortes
     * créées. À 31 : espace ×1.1 ; complète : ×1.25. »
     */
    public String collectionText(Molecule.Kind shelf) {
        for (KindCollection collection : game.collections()) {
            if (collection.kind() != shelf) continue;
            int level = game.collectionLevel(shelf);
            int created = game.kindCreated(shelf);
            int size = game.kindSize(shelf);
            String what = lower(BigBangPage.what(collection.stat()));
            String head = "Collection " + shelf.label() + " : " + created + "/" + size + " sortes créées";
            String text = level >= 2 ? head + ", complète : " + what + " ×" + ElementText.number(collection.full()) + "."
                    : level == 1 ? head + ". Moitié réunie : " + what + " ×" + ElementText.number(collection.half()) + " ; complète : ×"
                            + ElementText.number(collection.full()) + "."
                    : head + ". À " + game.collectionHalf(shelf) + " : " + what + " ×" + ElementText.number(collection.half())
                            + " ; complète : ×" + ElementText.number(collection.full()) + ".";
            return text + Detail.only(" Une sorte compte dès sa première molécule. Les collections d'une même grandeur se "
                    + "multiplient entre elles, à côté des améliorations d'espace et des paliers.");
        }
        return "";
    }

    private static String lower(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    /** « +0.42 % » : ce qu'une création ajoute, en proportion ; « moins de 0.01 % » quand c'est trop peu pour s'écrire. */
    static String gainText(double gain) {
        return gain * 100 < 0.005 ? "moins de 0.01 %" : "+" + ElementText.percent(gain);
    }

    /** Une carte de molécule : son dessin, le clic qui la crée, et l'épingle qui la range parmi les favorites. */
    Card moleculeCard(Molecule molecule) {
        Card card = new Card(GameApp.BIG_BANG_COLOR);
        card.setPicture(picture(molecule));
        card.setOnAction(() -> {
            game.createMolecule(molecule.id());
            refresh();
        });
        card.setPin(() -> {
            game.setMoleculeFavorite(molecule.id(), !game.isMoleculeFavorite(molecule.id()));
            refresh();
        });
        return card;
    }

    /** La carte d'une molécule parmi les favorites, ou {@code null} si elle n'y est pas : pour les vérifications. */
    public Card favoriteCard(Molecule molecule) {
        return favoriteCards.get(molecule);
    }

    /** Le dessin d'une molécule, fait une fois pour toutes, centré dans la largeur de sa carte. */
    static HBox picture(Molecule molecule) {
        Canvas canvas = new Canvas(PICTURE_WIDTH, PICTURE_HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, PICTURE_WIDTH, PICTURE_HEIGHT);
        MoleculeArt.Shape shape = MoleculeArt.of(molecule);
        // Les petites molécules ne sont pas gonflées jusqu'au bord : leurs atomes gardent une taille raisonnable.
        double radius = Math.min(PICTURE_HEIGHT / 2 - 3, shape.extent() * 24);
        shape.draw(g, PICTURE_WIDTH / 2, PICTURE_HEIGHT / 2, radius, -Math.PI / 6);
        HBox box = new HBox(canvas);
        box.setAlignment(Pos.CENTER);
        box.setMouseTransparent(true);     // le clic va à la carte
        return box;
    }

    /** La carte d'une molécule, ou {@code null} si son rayon n'a pas encore été ouvert : pour les vérifications. */
    public Card card(Molecule molecule) {
        return cards.get(molecule);
    }

    /** La sous-page des astres : pour les vérifications. */
    public BodiesPane bodiesPane() {
        return bodiesPane;
    }

    /** La sous-page de la galaxie : pour les vérifications. */
    /** Le petit arbre du cycle : le choix fait éclairé, les autres fermés jusqu'au prochain Big Bang. */
    void refreshCycle() {
        boolean open = game.isCycleChoiceUnlocked();
        cycleRow.setVisible(open);
        cycleRow.setManaged(open);
        if (!open) return;
        idle.core.universe.CycleChoice chosen = game.cycleChoice();
        cycleTitle.setText(chosen == null ? "Choix du cycle, jusqu'au prochain Big Bang :" : "Choix du cycle : " + chosen.effect());
        cycleButtons.forEach((choice, button) -> {
            boolean on = choice == chosen;
            button.setDisable(chosen != null && !on);
            button.setText(choice.label() + (chosen == null ? " : " + choice.effect() : ""));
            button.setStyle("-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                    + (on ? " -fx-text-fill: #10151f; -fx-background-color: " + GameApp.BIG_BANG_COLOR + ";"
                            : " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-background-color: #121923; -fx-border-color: " + GameApp.BIG_BANG_COLOR + "66;"));
        });
    }

    /** Les boutons du petit arbre du cycle : pour les vérifications. */
    public Button cycleButton(idle.core.universe.CycleChoice choice) {
        return cycleButtons.get(choice);
    }

    /** Les systèmes solaires : pour les vérifications. */
    public SystemsPane systemsPane() {
        return systemsPane;
    }

    public GalaxyPane galaxyPane() {
        return galaxyPane;
    }

    /** La vue de l'espace : pour les vérifications. */
    public SpaceView spaceView() {
        return spaceView;
    }

    /** Le jeu recommence de zéro : retour à la première sous-page et au premier rayon. */
    public void reset() {
        selectKind(Molecule.Kind.SIMPLE);
        spaceView.fit();
        select(0);
    }

    /**
     * Fait avancer l'animation de la vue de l'espace, quand elle est affichée.
     *
     * @param elapsed secondes écoulées depuis l'image précédente
     */
    /** Faux si le joueur a coupé les animations du Big Bang : les vues se redessinent, figées. */
    private static boolean motion = true;

    /** Allume ou coupe les animations du Big Bang (réglage « Animations du Big Bang »). */
    public static void setMotion(boolean on) {
        motion = on;
    }

    /** Vrai si les vues du Big Bang font avancer leur temps. */
    static boolean moving() {
        return motion;
    }

    public void frame(double elapsed) {
        if (selected == 1) spaceView.frame(elapsed);
        if (selected == 5) bodiesPane.frame(elapsed);
        if (selected == 7) bangChallenges.frame(elapsed);
        if (selected == 9) systemsPane.frame(elapsed);
        if (selected == 6) galaxyPane.frame(elapsed);
        if (selected == 8) probesPane.frame(elapsed);
    }
}

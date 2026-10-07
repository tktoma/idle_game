package idle.ui;

import idle.core.Body;
import idle.core.Cosmos;
import idle.core.Game;
import idle.core.Molecule;
import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * La sous-page « Univers » de l'onglet Big Bang : les trois dernières échelles du jeu, formées
 * l'une après l'autre ({@link Cosmos}). La galaxie rassemble tous les astres autour du trou noir
 * supermassif ; l'amas de galaxies, puis l'univers, demandent de la matière rassemblée en bien plus
 * grand nombre.
 *
 * <p>En haut, une image de la plus grande échelle formée ({@link GalaxyView}). En dessous, une
 * carte par échelle : ce qu'elle donne, et où en est ce qu'elle demande. Une fois la galaxie
 * formée, un bouton mène à l'expansion de la matière, où tout ce que le joueur a créé tourne
 * désormais en bras de spirale.
 */
final class GalaxyPane extends VBox {

    private static final double VIEW_HEIGHT = 230;

    private final Game game;
    private final GalaxyView view;
    private final Label intro = new Label();
    private final Map<Cosmos, Card> cards = new EnumMap<>(Cosmos.class);
    private final TileGrid grid = new TileGrid(230, 3, 8);
    private final Button look = new Button("Voir la galaxie dans l'expansion de la matière");

    /**
     * @param changed ce qu'il faut refaire quand la galaxie vient d'être formée : rafraîchir toute la page
     * @param show    ce que fait le bouton : aller voir la galaxie dans l'expansion de la matière
     */
    GalaxyPane(Game game, Runnable changed, Runnable show) {
        super(10);
        this.game = game;
        this.view = new GalaxyView(game);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 0, 0));

        intro.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        intro.setWrapText(true);
        intro.setTextAlignment(TextAlignment.CENTER);
        intro.setMaxWidth(760);

        CanvasPane viewPane = CanvasPane.filling(view);
        viewPane.setMinHeight(VIEW_HEIGHT);
        viewPane.setPrefHeight(VIEW_HEIGHT);
        viewPane.setMaxHeight(VIEW_HEIGHT);
        viewPane.setMaxWidth(820);

        for (Cosmos scale : Cosmos.values()) {
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            card.setOnAction(() -> {
                game.formCosmos(scale);
                changed.run();
            });
            cards.put(scale, card);
            grid.add(card);
        }
        grid.setMaxWidth(820);
        look.setStyle("-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 4;"
                + " -fx-border-radius: 4; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-background-color: #121923;"
                + " -fx-border-color: " + GameApp.BIG_BANG_COLOR + "66;");
        look.setFocusTraversable(false);
        look.setOnAction(event -> show.run());

        getChildren().add(intro);
        getChildren().add(viewPane);
        getChildren().add(grid);
        getChildren().add(look);
    }

    /** L'image de la galaxie : pour les vérifications. */
    GalaxyView view() {
        return view;
    }

    /** La carte de la galaxie : pour les vérifications. */
    Card card() {
        return cards.get(Cosmos.GALAXY);
    }

    /** La carte d'une échelle du cosmos : pour les vérifications. */
    Card card(Cosmos scale) {
        return cards.get(scale);
    }

    /** Fait avancer l'image de la galaxie. */
    void frame(double elapsed) {
        view.frame(elapsed);
    }

    /** Recopie l'état du jeu dans les trois cartes. */
    void refresh() {
        galaxy();
        scale(Cosmos.CLUSTER, "La matière de plusieurs galaxies, rassemblée : la vôtre n'est plus qu'une parmi d'autres, tenues ensemble "
                + "par leur poids. Il faut la galaxie, et bien plus de matière rassemblée dans chaque état : un amas de molécules "
                + "attire d'autant plus qu'il est gros, et la galaxie a tout multiplié par mille. ");
        scale(Cosmos.UNIVERSE, "Tout ce qui existe : des amas de galaxies le long de filaments, et du vide entre eux. Il faut l'amas de "
                + "galaxies, et dix fois plus de matière encore. C'est la dernière échelle du jeu. ");
        boolean formed = game.hasGalaxy();
        look.setVisible(formed);
        look.setManaged(formed);
        look.setText("Voir " + (game.cosmosFormed() >= 3 ? "l'univers" : game.cosmosFormed() == 2 ? "l'amas de galaxies" : "la galaxie")
                + " dans l'expansion de la matière");
        Cosmos next = game.nextCosmos();
        intro.setText(next == null
                ? "L'univers est formé : votre amas de galaxies n'est plus qu'un nœud de sa toile. C'est la fin du chemin."
                : next == Cosmos.UNIVERSE
                ? "L'amas de galaxies est formé : votre galaxie, au milieu des autres. Il ne reste que l'univers."
                : next == Cosmos.CLUSTER
                ? "La galaxie est formée : tout ce que vous avez créé tourne autour du trou noir supermassif. Au-dessus d'elle : "
                        + "l'amas de galaxies, puis l'univers."
                : "Quand tous les astres seront formés, ils pourront se rassembler en une galaxie, autour du trou noir supermassif. "
                        + "Viendront ensuite l'amas de galaxies, puis l'univers."
                        + Detail.only(" Dans l'expansion de la matière, les molécules s'enroulent alors en bras de spirale, "
                                + "chacune réduite à un point de sa couleur, et les astres se rangent le long des bras, les "
                                + "plus gros près du centre. Un bouton y permet de revoir la matière telle qu'elle était rangée."));
    }

    /** La carte de la galaxie : une ligne par échelle d'astres, puisqu'il les faut tous. */
    private void galaxy() {
        boolean formed = game.hasGalaxy();
        boolean ready = game.canFormGalaxy();
        int all = game.bodies().size();
        int have = game.bodiesFormed();
        // Une ligne par échelle : un rond plein quand tous ses astres sont formés.
        StringBuilder lines = new StringBuilder("Particules, atomes, espace et matière noire +"
                + ElementText.percent(Game.GALAXY_GAIN));
        for (Body.Tier tier : Body.Tier.values()) {
            int done = game.bodiesFormed(tier);
            int wanted = game.bodiesIn(tier);
            lines.append('\n').append(formed || done == wanted ? "● " : "○ ").append(tier.label()).append(' ')
                    .append(formed ? wanted : done).append('/').append(wanted);
        }
        cards.get(Cosmos.GALAXY).show(formed ? Card.State.DONE : ready ? Card.State.READY : have > 0 ? Card.State.STARTED : Card.State.WAITING,
                formed ? "" : have + "/" + all, formed ? "formée" : ready ? "prête" : "", "Galaxie", lines.toString(),
                "Tous les astres, du premier amas de roches au trou noir supermassif, se mettent "
                        + "à tourner autour de lui, et avec eux toutes vos molécules. Un achat unique, qui ne consomme rien : "
                        + "les astres restent formés et gardent leur bonus. La galaxie augmente d'un coup les quatre "
                        + "grandeurs que la matière multiplie, du double de ce que donne le trou noir supermassif. Ni "
                        + "l'explosion ni le Big Bang ne la défont.",
                formed ? "" : ready ? "Former la galaxie" : "Il manque " + (all - have) + (all - have > 1 ? " astres" : " astre"), "");
    }

    /** La carte de l'amas de galaxies ou de l'univers : l'échelle d'avant, puis une ligne par état de la matière. */
    private void scale(Cosmos scale, String story) {
        boolean formed = game.hasCosmos(scale);
        boolean open = game.hasCosmos(scale.previous());
        boolean ready = game.canFormCosmos(scale);
        int all = game.cosmosConditions(scale);
        int met = game.cosmosConditionsMet(scale);
        StringBuilder lines = new StringBuilder("Particules, atomes, espace et matière noire +" + ElementText.percent(scale.gain()));
        lines.append('\n').append(open ? "● " : "○ ").append(scale.previous().label());
        for (Map.Entry<Molecule.State, Integer> need : scale.matter().entrySet()) {
            int have = Math.min(game.gatheredInState(need.getKey()), need.getValue());
            lines.append('\n').append(formed || have >= need.getValue() ? "● " : "○ ").append(MatterText.signed(need.getKey())).append(' ')
                    .append(Format.whole(formed ? need.getValue() : have)).append(" / ").append(Format.whole(need.getValue()));
        }
        // La variété : un nombre de sortes rassemblées dans chaque état, sur une seule ligne.
        if (!scale.sorts().isEmpty()) {
            boolean all5 = true;
            StringBuilder sorts = new StringBuilder();
            for (Map.Entry<Molecule.State, Integer> need : scale.sorts().entrySet()) {
                int have = Math.min(game.sortsInState(need.getKey()), need.getValue());
                all5 &= formed || have >= need.getValue();
                if (sorts.length() > 0) sorts.append("  ");
                sorts.append(MatterText.sign(need.getKey())).append(' ').append(formed ? need.getValue() : have).append('/').append(need.getValue());
            }
            lines.append('\n').append(all5 ? "● " : "○ ").append("Sortes rassemblées : ").append(sorts);
        }
        cards.get(scale).show(formed ? Card.State.DONE : ready ? Card.State.READY : open ? Card.State.STARTED : Card.State.LOCKED,
                formed ? "" : met + "/" + all, formed ? "formé" : ready ? "prêt" : "", scale.label(), lines.toString(),
                story + "Un achat unique, qui ne consomme rien, et que ni l'explosion ni le Big Bang ne défont. Il double ce que "
                        + "donnait l'échelle d'avant. Il demande de la quantité, état par état, et de la variété : un nombre de "
                        + "sortes rassemblées dans chaque état, chacune comptant dès qu'elle est rassemblée.",
                formed ? "" : ready ? "Former " + scale.phrase() : !open ? "Après " + scale.previous().phrase()
                        : "Il manque " + (all - met) + (all - met > 1 ? " conditions" : " condition"), "");
    }
}

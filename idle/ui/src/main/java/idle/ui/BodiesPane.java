package idle.ui;

import idle.core.Body;
import idle.core.Game;
import idle.core.Molecule;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * La sous-page « Astres » de l'onglet Big Bang : les corps du ciel que le joueur forme en cumulant
 * ce qu'il a fait de plus petit ({@link Game#formBody(String)}).
 *
 * <p>En haut, le ciel ({@link SkyView}) : les astres formés, côte à côte, du plus petit au plus
 * grand. En dessous, une section par échelle ({@link Body.Tier}), avec une carte par astre : son
 * dessin, ce qu'il donne, combien de ses conditions sont réunies et ce qui manque en premier. La
 * liste complète de ce qu'il demande n'apparaît qu'en mode détails.
 *
 * <p>Les amas de roches sont montrés d'emblée. Un astre plus gros n'a sa carte que lorsqu'un des
 * astres dont il part est formé : le joueur découvre les échelles l'une après l'autre.
 */
final class BodiesPane extends VBox {

    /** Hauteur du ciel, en pixels : de quoi montrer les petits astres sur une ligne et les planètes sur une autre. */
    private static final double SKY_HEIGHT = 330;
    private static final double PICTURE_WIDTH = 150;
    private static final double PICTURE_HEIGHT = 64;

    private final Game game;
    private final Runnable changed;
    private final SkyView sky;
    private final Label intro = new Label();
    /** Le geste groupé, une fois acquis : former d'un clic tous les astres prêts. */
    private final javafx.scene.control.Button formAll = new javafx.scene.control.Button();
    private final Map<Body.Tier, Label> tierTitles = new EnumMap<>(Body.Tier.class);
    private final Map<Body.Tier, TileGrid> tierGrids = new EnumMap<>(Body.Tier.class);
    private final Map<Body, Card> cards = new LinkedHashMap<>();

    /**
     * @param changed ce qu'il faut refaire quand un astre vient d'être formé : rafraîchir toute la page
     */
    BodiesPane(Game game, Runnable changed) {
        super(8);
        this.game = game;
        this.changed = changed;
        this.sky = new SkyView(game);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(10, 0, 0, 0));

        intro.setStyle("-fx-font-size: 12px; -fx-text-fill: #8fa3b8;");
        intro.setWrapText(true);
        intro.setTextAlignment(TextAlignment.CENTER);
        intro.setMaxWidth(760);

        CanvasPane skyPane = CanvasPane.filling(sky);
        skyPane.setMinHeight(SKY_HEIGHT);
        skyPane.setPrefHeight(SKY_HEIGHT);
        skyPane.setMaxHeight(SKY_HEIGHT);
        skyPane.setMaxWidth(820);

        VBox list = new VBox(10);
        list.setAlignment(Pos.TOP_CENTER);
        list.setPadding(new Insets(4, 0, 10, 0));
        for (Body.Tier tier : Body.Tier.values()) {
            Label title = new Label();
            title.setStyle("-fx-font-size: 14px; -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-padding: 8 0 0 0;");
            TileGrid grid = new TileGrid(230, 3, 8);
            grid.setMaxWidth(820);
            tierTitles.put(tier, title);
            tierGrids.put(tier, grid);
            list.getChildren().add(title);
            list.getChildren().add(grid);
        }
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        formAll.setStyle("-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand; -fx-background-radius: 4; -fx-border-radius: 4;"
                + " -fx-text-fill: " + GameApp.BIG_BANG_COLOR + "; -fx-background-color: #121923; -fx-border-color: " + GameApp.BIG_BANG_COLOR + "66;");
        formAll.setFocusTraversable(false);
        formAll.setOnAction(event -> {
            game.formAllBodies();
            Readiness.of(game).compute();
            this.changed.run();
        });
        getChildren().add(intro);
        getChildren().add(formAll);
        getChildren().add(skyPane);
        getChildren().add(scroll);
    }

    /** Le ciel : pour les vérifications. */
    SkyView sky() {
        return sky;
    }

    /** La carte d'un astre, ou {@code null} si elle n'a pas encore été montrée : pour les vérifications. */
    Card card(Body body) {
        return cards.get(body);
    }

    /** Fait avancer le ciel d'une image. */
    void frame(double elapsed) {
        sky.frame(elapsed);
    }

    /** Le bouton « Former les astres prêts » : pour les vérifications. */
    javafx.scene.control.Button formAllButton() {
        return formAll;
    }

    /** Recopie l'état du jeu dans les cartes. */
    void refresh() {
        BigBangPage.showBulk(formAll, game.isBulkFormUnlocked(), "Former les astres prêts", game.bodiesReady());
        Map<Body.Tier, int[]> tiers = new EnumMap<>(Body.Tier.class);     // par échelle : cartes montrées, astres formés
        for (Body body : game.bodies()) {
            boolean formed = game.hasBody(body.id());
            // Un amas est montré d'emblée ; un astre plus gros, dès qu'un de ceux dont il part est formé.
            boolean visible = formed || body.bodies().isEmpty();
            for (String smaller : body.bodies()) visible |= game.hasBody(smaller);
            Card card = visible ? cardFor(body) : cards.get(body);
            if (card != null) tierGrids.get(body.tier()).show(card, visible);
            if (!visible) continue;
            int[] tier = tiers.computeIfAbsent(body.tier(), each -> new int[2]);
            tier[0]++;
            if (formed) tier[1]++;
            boolean ready = game.canFormBody(body.id());
            int met = game.bodyConditionsMet(body.id());
            String gain = BigBangPage.what(body.boost().stat()) + " +" + ElementText.percent(body.boost().perMolecule());
            card.show(formed ? Card.State.DONE : ready ? Card.State.READY : met > 0 ? Card.State.STARTED : Card.State.WAITING,
                    formed ? "" : met + "/" + body.conditions(), formed ? "formé" : "", body.name(),
                    gain + "\n" + origin(body), Detail.shown() ? needs(body, formed) : "",
                    formed ? "" : ready ? "Former" : missing(body), "");
        }
        int formedAll = 0;
        for (Body.Tier tier : Body.Tier.values()) {
            int[] counts = tiers.getOrDefault(tier, new int[2]);
            formedAll += counts[1];
            Label title = tierTitles.get(tier);
            title.setText(tier.label() + (counts[1] == 0 ? "" : " : " + counts[1] + (counts[1] > 1 ? " formés" : " formé")));
            title.setVisible(counts[0] > 0);
            title.setManaged(counts[0] > 0);
        }
        intro.setText((formedAll == 0
                ? "Un astre est fait de ses assemblages, avec assez de matière rassemblée et de molécules autour : d'abord un amas de roches."
                : "Un astre est fait de ses assemblages et part d'astres plus petits : amas de roches, comètes, astéroïdes, lunes, "
                        + "planètes, puis étoiles, étoiles mortes et trous noirs.")
                + Detail.only(" Un achat unique. Ses assemblages entrent en lui : dans l'expansion de la matière, leurs blocs "
                        + "se rangent en couronne autour de l'astre, qui prend le milieu. Ils restent formés et gardent "
                        + "leur bonus, et chaque assemblage n'entre que dans un seul astre. Le reste, ce sont des seuils à "
                        + "atteindre, qui servent aussi aux autres astres : les astres plus petits, la matière, qui compte "
                        + "toutes les molécules rassemblées dans un état quelle que soit leur sorte, et les molécules. Un astre augmente une grandeur de 200 % pour un amas, et du double "
                        + "à chaque échelle : 3 200 % pour une planète, 6 400 % pour une étoile, jusqu'à 51 200 % pour le trou noir supermassif. Un astre plus gros n'est montré que lorsqu'un "
                        + "de ceux dont il part est formé."));
    }

    /** La carte d'un astre dans la section de son échelle, construite la première fois qu'on en a besoin, avec son dessin. */
    private Card cardFor(Body body) {
        return cards.computeIfAbsent(body, each -> {
            Card card = new Card(GameApp.BIG_BANG_COLOR);
            Canvas canvas = new Canvas(PICTURE_WIDTH, PICTURE_HEIGHT);
            canvas.getGraphicsContext2D().clearRect(0, 0, PICTURE_WIDTH, PICTURE_HEIGHT);
            // Le plus grand rayon qui laisse tenir tout l'astre, queue ou anneaux compris, dans le cadre.
            double r = Math.min((PICTURE_HEIGHT - 6) / (BodyArt.reachUp(body) + BodyArt.reachDown(body)),
                    (PICTURE_WIDTH - 6) / (BodyArt.reachLeft(body) + BodyArt.reachRight(body)));
            r = Math.min(r, 26);
            double x = PICTURE_WIDTH / 2 + (BodyArt.reachLeft(body) - BodyArt.reachRight(body)) * r / 2;
            double y = PICTURE_HEIGHT / 2 + (BodyArt.reachUp(body) - BodyArt.reachDown(body)) * r / 2;
            BodyArt.draw(canvas.getGraphicsContext2D(), body, x, y, r);
            HBox picture = new HBox(canvas);
            picture.setAlignment(Pos.CENTER);
            picture.setMouseTransparent(true);     // le clic va à la carte
            card.setPicture(picture);
            card.setOnAction(() -> {
                game.formBody(body.id());
                changed.run();
            });
            tierGrids.get(body.tier()).add(card);
            return card;
        });
    }

    /** De quoi un astre est fait, en une ligne : « Fait de roche sableuse » ou « Fait de glace de comète et eau de source ». */
    private String origin(Body body) {
        StringBuilder text = new StringBuilder("Fait de ");
        List<String> parts = body.assemblies();
        for (int index = 0; index < parts.size(); index++) {
            if (index > 0) text.append(index == parts.size() - 1 ? " et " : ", ");
            text.append(game.assembly(parts.get(index)).name().toLowerCase());
        }
        return text.toString();
    }

    /** Ce qui manque en premier à un astre, dans l'ordre de ses conditions : astres, assemblages, matière, molécules. */
    private String missing(Body body) {
        for (String smaller : body.bodies()) {
            if (!game.hasBody(smaller)) return "Il faut former : " + game.body(smaller).name().toLowerCase();
        }
        for (String assembly : body.assemblies()) {
            if (!game.hasAssembly(assembly)) return "Il faut assembler : " + game.assembly(assembly).name().toLowerCase();
        }
        for (Map.Entry<Molecule.State, Integer> matter : body.matter().entrySet()) {
            int lacking = matter.getValue() - game.gatheredInState(matter.getKey());
            if (lacking > 0) return "Il manque " + lacking + " " + matterName(matter.getKey());
        }
        for (Map.Entry<String, Integer> molecule : body.molecules().entrySet()) {
            int lacking = molecule.getValue() - game.moleculeCount(molecule.getKey());
            if (lacking > 0) return "Il manque " + lacking + " " + game.molecule(molecule.getKey()).formula();
        }
        return "";
    }

    /** Tout ce que demande un astre, pour le mode détails : un rond plein devant ce qui est réuni, vide devant le reste. */
    private String needs(Body body, boolean formed) {
        StringBuilder text = new StringBuilder();
        for (String smaller : body.bodies()) {
            line(text, formed || game.hasBody(smaller), game.body(smaller).name());
        }
        for (String assembly : body.assemblies()) {
            line(text, formed || game.hasAssembly(assembly), game.assembly(assembly).name());
        }
        for (Map.Entry<Molecule.State, Integer> matter : body.matter().entrySet()) {
            int have = game.gatheredInState(matter.getKey());
            line(text, formed || have >= matter.getValue(), matter.getValue() + " " + matterName(matter.getKey())
                    + (formed || have >= matter.getValue() ? "" : " (" + have + ")"));
        }
        for (Map.Entry<String, Integer> molecule : body.molecules().entrySet()) {
            int have = game.moleculeCount(molecule.getKey());
            line(text, formed || have >= molecule.getValue(), molecule.getValue() + " " + game.molecule(molecule.getKey()).formula()
                    + (formed || have >= molecule.getValue() ? "" : " (" + have + ")"));
        }
        return text.toString();
    }

    private static void line(StringBuilder text, boolean met, String what) {
        if (text.length() > 0) text.append('\n');
        text.append(met ? "● " : "○ ").append(what);
    }

    /** Un état, comme on compte sa matière : « gaz rassemblés », « cristaux rassemblés ». */
    private static String matterName(Molecule.State matter) {
        return MatterText.plural(matter) + " rassemblés";
    }
}

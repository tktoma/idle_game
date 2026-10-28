package idle.ui.vacuum;

import idle.core.Game;
import idle.core.GameUniverse.VoidUpgrade;
import idle.core.math.BigNum;
import idle.ui.common.CanvasPane;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.SubTabs;
import idle.ui.common.TileGrid;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * L'onglet du vide (version 0.099, jouable depuis la 0.129) : le deuxième acte de la partie secondaire, ouvert dans le
 * collisionneur au premier Big Bang du troisième univers. Le collisionneur y fait naître des paires de particules
 * virtuelles, qui coûtent de son énergie et de l'espace libre et donnent de l'énergie du vide ; celle-ci fond vers un
 * équilibre, affiché. Le réglage dit la part du collisionneur et de l'espace libre que le vide peut prendre. Le trou
 * noir, au premier trou noir formé, prend les particules qu'on lui sacrifie. Deux sous-onglets : « Paires » (tout ce
 * qui précède) et « Améliorations » (les huit, en énergie du vide) ; la réserve et son équilibre restent en haut des deux.
 */
public final class VoidPage extends VBox {

    /** La couleur du vide : celle de son sous-onglet dans la partie secondaire. */
    public static final String COLOR = idle.ui.universe.UniversePage.VOID_COLOR;
    /** La couleur du trou noir du vide : son texte ici, et sa nouveauté dans {@link idle.ui.PopupPane}. */
    public static final String HOLE_COLOR = "#ff9a3a";

    private final Game game;
    private final Label energy = new Label();
    private final Label pairsLine = new Label();
    private final Label costLine = new Label();
    private final Label shareLabel = new Label();
    private final Slider share = new Slider(0, 100, 25);
    private final Label holeLine = new Label();
    private final Button sacrifice = new Button();
    private final VBox holeBox;
    private final Label note = new Label();
    private final Map<VoidUpgrade, Card> cards = new EnumMap<>(VoidUpgrade.class);
    /** Ce que les cartes montrent (niveaux, achetables, détails, signes) : leurs longs textes ne se refont que si ça change. */
    private String cardsKey = "";
    /** Si la note du bas a été écrite avec le trou noir ouvert ; elle ne change qu'avec lui. */
    private Boolean notedHole;
    private final VoidView view = new VoidView();
    /** Les sous-onglets : les paires, et les améliorations. */
    public static final int PAIRS = 0, UPGRADES = 1;
    private final Button pairsTab = new Button("Paires");
    private final Button upgradesTab = new Button("Améliorations");
    private final HBox subTabs = new HBox(pairsTab, upgradesTab);
    private final Label upgradesNote = new Label();
    private VBox pairsPane, upgradesPane;
    private int selected = PAIRS;
    /** Vrai pendant que la page recopie la partie dans le curseur : ce n'est pas le joueur qui le bouge. */
    private boolean copying = false;

    public VoidPage(Game game) {
        super(10);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(14, 16, 14, 16));
        energy.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        for (Label label : new Label[] {pairsLine, costLine, shareLabel}) {
            Labels.centered(label, "-fx-font-size: 13px; -fx-text-fill: #c8d3e0;");
            label.setMaxWidth(860);
            // Le dessin prend la hauteur qui reste : les lignes gardent la leur, entières, sans « … ».
            label.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        }
        share.setMajorTickUnit(25);
        share.setMinorTickCount(4);
        share.setSnapToTicks(true);
        share.setBlockIncrement(5);
        share.setPrefWidth(260);
        share.setFocusTraversable(false);
        share.valueProperty().addListener((seen, before, now) -> {
            if (copying) return;
            game.setVoidShare(Math.round(now.doubleValue() / 5) * 5 / 100.0);
            refresh();
        });
        HBox shareRow = new HBox(12, shareLabel, share);
        shareRow.setAlignment(Pos.CENTER);

        CanvasPane pane = CanvasPane.filling(view);
        pane.setMinHeight(200);
        pane.setPrefHeight(260);
        pane.setMaxWidth(900);
        VBox.setVgrow(pane, Priority.ALWAYS);

        Labels.centered(holeLine, "-fx-font-size: 13px; -fx-text-fill: " + HOLE_COLOR + ";");
        holeLine.setMaxWidth(860);
        sacrifice.setFocusTraversable(false);
        sacrifice.setStyle("-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand; -fx-background-radius: 6;"
                + " -fx-background-color: #2a1a0c; -fx-text-fill: " + HOLE_COLOR + "; -fx-border-color: " + HOLE_COLOR + "; -fx-border-radius: 6;");
        sacrifice.setOnAction(event -> {
            game.sacrificeParticles();
            refresh();
        });
        holeBox = new VBox(6, holeLine, sacrifice);
        holeBox.setAlignment(Pos.CENTER);

        TileGrid upgrades = new TileGrid(220, 3, 10);
        for (VoidUpgrade upgrade : VoidUpgrade.values()) {
            Card card = new Card(COLOR);
            card.setOnAction(() -> {
                game.buyVoid(upgrade);
                refresh();
            });
            cards.put(upgrade, card);
            upgrades.add(card);
        }
        Labels.note(note, 860);

        Labels.note(upgradesNote, 860);
        upgradesNote.setText("Payées en énergie du vide, elles ne valent que pour cet univers : à sa formation, elles repartent "
                + "de zéro avec le vide. L'allègement du ciel et la maturation plus courte servent le reste de la partie.");

        // « Paires » : le rythme, les coûts, le réglage, l'écume, le trou noir.
        holeBox.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        note.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        pairsPane = new VBox(10, pairsLine, costLine, shareRow, pane, holeBox, note);
        pairsPane.setAlignment(Pos.TOP_CENTER);
        // « Améliorations » : les huit cartes.
        VBox upgradesBox = new VBox(10, upgrades, upgradesNote);
        upgradesBox.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(upgradesBox);
        Labels.transparent(scroll);
        upgradesPane = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        javafx.scene.layout.StackPane pages = new javafx.scene.layout.StackPane(pairsPane, upgradesPane);
        pages.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(pages, Priority.ALWAYS);

        SubTabs.wire(true, this::select, this::refresh, pairsTab, upgradesTab);
        subTabs.setAlignment(Pos.CENTER);
        getChildren().addAll(subTabs, energy, pages);
        select(PAIRS);
        refresh();
    }

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public HBox subTabs() {
        return subTabs;
    }

    public void select(int index) {
        selected = index == UPGRADES ? UPGRADES : PAIRS;
        pairsPane.setVisible(selected == PAIRS);
        upgradesPane.setVisible(selected == UPGRADES);
        pairsTab.setStyle(SubTabs.style(selected == PAIRS, COLOR));
        upgradesTab.setStyle(SubTabs.style(selected == UPGRADES, COLOR));
    }

    public int selected() {
        return selected;
    }

    /** Passe au sous-onglet suivant ou précédent, en boucle. */
    public void step(int direction) {
        select(Math.floorMod(selected + direction, 2));
        refresh();
    }

    public Button upgradesTab() {
        return upgradesTab;
    }

    /** La réserve et son équilibre, en haut des deux sous-onglets : pour les vérifications. */
    public Label energyLabel() {
        return energy;
    }

    /** Vrai s'il y a quelque chose à faire dans le vide : une amélioration à prendre, des particules à sacrifier. */
    public static boolean ready(Game game) {
        if (!game.isVoidUnlocked()) return false;
        for (VoidUpgrade upgrade : VoidUpgrade.values()) if (game.canBuyVoid(upgrade)) return true;
        return false;
    }

    /** Recopie la partie : la réserve et son équilibre, les paires, le réglage, le trou noir, les améliorations. */
    public void refresh() {
        energy.setText("Énergie du vide : " + Format.count(game.voidEnergy()) + "   |   équilibre : "
                + Format.count(BigNum.of(game.voidEquilibrium())));
        upgradesTab.setText("Améliorations" + idle.ui.Readiness.dot(idle.ui.Readiness.Kind.VOID, ready(game)));
        double rate = game.voidRate();
        pairsLine.setText(decimal(rate) + " paire" + (rate >= 2 ? "s" : "") + " par seconde (" + limitText(game.voidLimit()) + "), "
                + Format.count(BigNum.of(game.voidPairs())) + " vivantes, " + decimal(game.voidPairLife()) + " s chacune. "
                + "Gain : " + decimal(game.voidGainPerSecond()) + " par seconde ; fonte : " + percent(game.voidDecay()) + " par seconde.");
        costLine.setText("Une paire coûte " + amount(BigNum.of(game.voidPairEnergy())) + " énergie du collisionneur et tient "
                + amount(game.voidPairSpace()) + " d'espace tant qu'elle vit ; elle donne " + decimal(game.voidPairGain())
                + " énergie du vide à sa naissance.");
        copying = true;
        share.setValue(game.state().voidShare() * 100);
        copying = false;
        shareLabel.setText("Part du collisionneur et de l'espace libre : " + Math.round(game.state().voidShare() * 100) + " %");

        boolean hole = game.isVoidHoleOpen();
        holeBox.setVisible(hole);
        holeBox.setManaged(hole);
        if (hole) {
            double mass = game.voidMass();
            holeLine.setText("Trou noir : masse " + decimal(mass) + " (" + Format.count(game.state().voidSacrificed())
                    + " particules sacrifiées). Les paires vivent " + decimal(game.voidPairLife()) + " s au lieu de "
                    + decimal(Game.VOID_PAIR_LIFE) + " et rendent leur espace plus vite ; la fonte est divisée par "
                    + decimal(1 + mass / 5) + "."
                    + Detail.only(" Masse : 2 × log₁₀(1 + log₁₀(1 + particules sacrifiées)), au plus "
                    + (int) Game.VOID_MAX_MASS + ". Vie des paires : 6 ÷ (1 + masse/10) ; fonte ÷ (1 + masse/5)."));
            sacrifice.setDisable(!game.canSacrificeParticles());
            sacrifice.setText(mass >= Game.VOID_MAX_MASS ? "Le trou noir a sa masse la plus grande"
                    : "Sacrifier toutes les particules (" + Format.count(game.state().particles()) + ") : masse "
                    + decimal(game.voidMassAfterSacrifice()));
        }

        StringBuilder key = new StringBuilder().append(Detail.shown()).append(Card.signsShown());
        for (VoidUpgrade upgrade : cards.keySet()) key.append('/').append(game.voidLevel(upgrade)).append(game.canBuyVoid(upgrade) ? '+' : '-');
        if (!key.toString().equals(cardsKey)) {
            cardsKey = key.toString();
            for (Map.Entry<VoidUpgrade, Card> entry : cards.entrySet()) {
                VoidUpgrade upgrade = entry.getKey();
                int level = game.voidLevel(upgrade);
                boolean max = level >= upgrade.max();
                Card.State state = max ? Card.State.DONE : game.canBuyVoid(upgrade) ? Card.State.READY
                        : level > 0 ? Card.State.STARTED : Card.State.WAITING;
                entry.getValue().show(state, level > 0 ? String.valueOf(level) : "",
                        upgrade.max() == Integer.MAX_VALUE ? "" : level + " / " + upgrade.max(), title(upgrade), line(upgrade),
                        detail(upgrade), max ? "max" : Format.count(BigNum.of(game.voidCost(upgrade))) + " énergie du vide", "");
            }
        }
        if (notedHole == null || notedHole != hole) {
            notedHole = hole;
            note.setText("Les paires naissent du collisionneur et vivent quelques secondes : chacune coûte de son énergie et tient de "
                    + "l'espace libre, celui que les molécules n'ont pas pris. Le réglage borne ce que le vide en prend. L'énergie du vide "
                    + "fond de " + percent(Game.VOID_DECAY) + " par seconde : elle monte vers l'équilibre, le gain par seconde divisé par la fonte. "
                    + "À la formation de l'univers, ce qu'il en reste s'ajoute au cumul, gardé (sous-onglet Graine de l'onglet Univers), "
                    + "qui paie les bulles de l'arbre ; "
                    + "le vide, ses améliorations et son trou noir repartent de zéro dans l'univers suivant."
                    + (hole ? "" : " Le premier trou noir formé dans le ciel ouvre le trou noir du vide."));
        }
    }

    private static String limitText(String limit) {
        return switch (limit) {
            case "énergie" -> "bornées par la part du collisionneur";
            case "espace" -> "bornées par la part de l'espace libre";
            default -> "au rythme le plus haut";
        };
    }

    private static String title(VoidUpgrade upgrade) {
        return switch (upgrade) {
            case GAIN -> "Gain par paire";
            case RATE -> "Rythme";
            case ENERGY -> "Coût en énergie";
            case SPACE -> "Espace par paire";
            case DECAY -> "Fonte plus lente";
            case LIGHTEN -> "Allègement du ciel";
            case MATURE -> "Maturation plus courte";
            case SETTLE -> "Rassemblement plus court";
        };
    }

    private String line(VoidUpgrade upgrade) {
        return switch (upgrade) {
            case GAIN -> "+1 énergie du vide par paire (" + decimal(game.voidPairGain()) + ")";
            case RATE -> "+" + percent(Game.VOID_RATE_STEP - 1) + " de paires par seconde (" + decimal(game.voidBaseRate()) + " au plus)";
            case ENERGY -> "−15 % d'énergie du collisionneur par paire";
            case SPACE -> "−15 % d'espace par paire";
            case DECAY -> "−10 % de fonte";
            case LIGHTEN -> "−" + percent(Game.VOID_LIGHTEN_STEP) + " des besoins du ciel (−" + Math.round((1 - game.needShare()) * 100) + " %)";
            case MATURE -> "−" + percent(Game.VOID_MATURE_STEP) + " du temps de maturation des astres (−" + Math.round((1 - game.maturationShare()) * 100) + " %)";
            case SETTLE -> "−" + percent(Game.VOID_SETTLE_STEP) + " du rassemblement de l'amas et de l'univers (−" + Math.round((1 - game.settleShare()) * 100) + " %)";
        };
    }

    private static String detail(VoidUpgrade upgrade) {
        String price = " Prix ×" + ElementText.number(upgrade.growth()) + " par niveau.";
        return switch (upgrade) {
            case GAIN -> "Chaque paire donne 1 énergie du vide à sa naissance, plus 1 par niveau. Sans plafond." + price;
            case RATE -> "Le rythme le plus haut des paires : " + Math.round(Game.VOID_BASE_RATE) + " par seconde, ×"
                    + String.format(Locale.FRANCE, "%.2f", Game.VOID_RATE_STEP) + " par niveau, " + upgrade.max() + " niveaux. La part "
                    + "du collisionneur et celle de l'espace libre le bornent aussi." + price;
            case ENERGY -> "Une paire coûte une demi-seconde de production du collisionneur, ×0,85 par niveau, " + upgrade.max()
                    + " niveaux : la même part de collisionneur paie plus de paires." + price;
            case SPACE -> "Une paire tient un cinquième de seconde d'expansion tant qu'elle vit, ×0,85 par niveau, " + upgrade.max()
                    + " niveaux : la même part d'espace libre tient plus de paires." + price;
            case DECAY -> "La fonte, 0,2 % par seconde, ×0,9 par niveau, " + upgrade.max() + " niveaux : l'équilibre monte d'autant." + price;
            case LIGHTEN -> "Les molécules et la matière que demandent les astres, l'amas et l'univers baissent de "
                    + percent(Game.VOID_LIGHTEN_STEP) + " par niveau, arrondies au-dessus, jamais sous 1 ; " + upgrade.max()
                    + " niveaux, −" + percent(Game.VOID_LIGHTEN_STEP * upgrade.max()) + " au plus. La galaxie demande les astres : "
                    + "elle s'allège avec eux. Seulement dans cet univers." + price;
            case MATURE -> "Les astres mûrissent " + percent(Game.VOID_MATURE_STEP) + " plus vite par niveau, " + upgrade.max()
                    + " niveaux, −" + percent(Game.VOID_MATURE_STEP * upgrade.max()) + " du temps au plus : le trou noir supermassif "
                    + "passe de 6 h à " + hours(6 * (1 - Game.VOID_MATURE_STEP * upgrade.max())) + ". Les astres qui mûrissent déjà "
                    + "voient leur reste raccourcir d'autant. Seulement dans cet univers." + price;
            case SETTLE -> "Le rassemblement qu'attendent l'amas de galaxies et l'univers, deux heures chacun, raccourcit de "
                    + percent(Game.VOID_SETTLE_STEP) + " par niveau, " + upgrade.max() + " niveaux, −"
                    + percent(Game.VOID_SETTLE_STEP * upgrade.max()) + " au plus : " + minutesOf(Game.COSMOS_SETTLE_SECONDS
                    * (1 - Game.VOID_SETTLE_STEP * upgrade.max())) + " au lieu de deux heures. Seulement dans cet univers." + price;
        };
    }

    /**
     * Ce que le vide retire au troisième acte de cet univers (version 0.132), pour les statistiques : « besoins du ciel −9 %,
     * maturation −49 %, rassemblement −48 % » ; « rien encore » sans amélioration de ces trois-là.
     */
    public static String actThreeCuts(Game game) {
        java.util.List<String> cuts = new java.util.ArrayList<>();
        if (game.needShare() < 1) cuts.add("besoins du ciel −" + Math.round((1 - game.needShare()) * 100) + " %");
        if (game.maturationShare() < 1) cuts.add("maturation −" + Math.round((1 - game.maturationShare()) * 100) + " %");
        if (game.settleShare() < 1) cuts.add("rassemblement −" + Math.round((1 - game.settleShare()) * 100) + " %");
        return cuts.isEmpty() ? "rien encore" : String.join(", ", cuts);
    }

    /** Les niveaux d'amélioration du vide pris dans cet univers, toutes améliorations comprises. */
    public static int levelsOwned(Game game) {
        int levels = 0;
        for (VoidUpgrade upgrade : VoidUpgrade.values()) levels += game.voidLevel(upgrade);
        return levels;
    }

    /** « 1 h 48 » : des heures, en heures et minutes. */
    private static String hours(double hours) {
        long minutes = Math.round(hours * 60);
        return minutes / 60 + " h " + String.format(java.util.Locale.ROOT, "%02d", minutes % 60);
    }

    /** « 24 min » : des secondes, en minutes entières. */
    private static String minutesOf(double seconds) {
        return Math.round(seconds / 60) + " min";
    }

    /** Une quantité qui peut être petite (une fraction d'énergie ou d'espace) : avec ses décimales sous mille. */
    private static String amount(BigNum value) {
        return value.lt(BigNum.of(1000)) ? decimal(value.toDouble()) : Format.count(value);
    }

    private static String decimal(double value) {
        if (value >= 1000) return Format.count(BigNum.of(value));
        return value >= 100 ? String.valueOf(Math.round(value))
                : String.format(Locale.FRANCE, value >= 10 ? "%.1f" : "%.2f", value);
    }

    /** « 7 % », « 0,2 % » : une part, en pour cent, sans zéros inutiles. */
    private static String percent(double rate) {
        return String.format(Locale.FRANCE, "%.3f", rate * 100).replaceAll("0+$", "").replaceAll(",$", "") + " %";
    }

    /** Une image de l'écume : les paires de la partie, figées si les animations de l'univers sont coupées. */
    public void frame(double elapsed, boolean motion) {
        view.follow(game.voidPairsBorn(), game.voidPairs(), game.voidPairLife(), game.isVoidHoleOpen() ? game.voidMass() : -1);
        view.frame(elapsed, motion);
    }

    public VoidView view() {
        return view;
    }

    public Slider share() {
        return share;
    }

    public Button sacrificeButton() {
        return sacrifice;
    }

    public Card card(VoidUpgrade upgrade) {
        return cards.get(upgrade);
    }
}

package idle.ui.automation;

import idle.core.Game;
import idle.core.universe.DimensionAutomation;
import idle.ui.GameApp;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.ElementText;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.TileGrid;
import idle.ui.universe.Dimension;
import idle.ui.universe.DimensionWindow;
import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Le sous-onglet « Dimensions » de l'automatisation, que la bulle du sommet de l'arbre-monde ouvre : les automatismes
 * qui jouent les dimensions à la place du joueur et achètent leurs améliorations ({@link DimensionAutomation}).
 *
 * <p>En tête, ce qui paie chaque dimension, et le rappel que « Couper tous les automatismes » les arrête et qu'ils
 * traversent les univers ; le même bouton de coupure que le sous-onglet « Automatismes », pour s'en servir d'ici. Puis
 * une section par dimension ouverte (la ligne, le plan, le volume, la durée, la boucle depuis la version 0.153, la
 * brane depuis la version 0.154, le clicker depuis la version 0.155, la grille depuis la version 0.156, le miroir
 * depuis la version 0.157, le mélange depuis la version 0.158), qui dit ce que le joueur a pour payer, et une carte par
 * automatisme : son nom, ce qu'il fait, son prix et ce qui le paie. Pas encore achetable, la carte dit celui qu'il faut
 * avoir acheté avant (« Après : … ») ; à acheter, un clic l'achète ; acheté, elle sert d'interrupteur, en marche ou en
 * pause.
 *
 * <p>Version 0.268 : les cinq paliers de celui qui joue une dimension tiennent sur une seule carte ({@link #showPlay}).
 * Elle dit le palier acheté (« 2/5 ») et le prix du suivant : payable, un clic l'achète ; sinon, un clic met en pause
 * ou relance tous les paliers achetés ensemble. Tous achetés, elle n'est plus qu'un interrupteur. Version 0.269 : un bouton
 * « Couper » (ou « Relancer ») sous la carte, dès le premier palier acheté, fait la même pause à tout moment, même quand
 * le palier suivant est payable.
 *
 * <p>Les cartes ne se refont que si leur état a changé (acheté, en marche, payable, coupure, affichage) ; seules les
 * lignes des réserves se recopient à chaque fois.
 */
public final class DimensionAutomationPane extends VBox {
    private static final String HEADING_STYLE = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 14 0 0 0; -fx-text-fill: ";
    private static final double MAX_WIDTH = 820;

    private final Game game;
    private final Button pauseButton = new Button();
    private final Label intro = new Label();
    private final Map<DimensionAutomation, Card> cards = new EnumMap<>(DimensionAutomation.class);
    /** Par dimension : le bouton « Couper » sous la carte des paliers de celui qui joue (version 0.269). */
    private final Button[] playSwitches = new Button[Dimension.PLAYABLE + 1];
    /** Par dimension (1 à 10) : son titre, la ligne de ce qui la paie, ses cartes, et la boîte qui les tient. */
    private final Label[] headings = new Label[Dimension.PLAYABLE + 1];
    private final Label[] balances = new Label[Dimension.PLAYABLE + 1];
    private final VBox[] sections = new VBox[Dimension.PLAYABLE + 1];
    /** Ce que les cartes ont montré en dernier : elles ne se refont que si cela change. */
    private String shownKey = "";

    public DimensionAutomationPane(Game game) {
        super(10);
        this.game = game;
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(24));

        Label title = new Label("Automatisation des dimensions");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + GameApp.AUTOMATION_COLOR + ";");
        pauseButton.setFocusTraversable(false);
        pauseButton.setOnAction(event -> {
            game.setAutomationPaused(!game.isAutomationPaused());
            refresh();
        });
        Labels.note(intro, MAX_WIDTH);
        intro.setText("Ils jouent les dimensions à votre place et achètent leurs améliorations, l'un après l'autre dans "
                + "chaque dimension. Chacun se paie avec la ressource de sa propre dimension : ceux de la ligne en "
                + Game.dimensionResourceName(1) + ", ceux du plan en " + Game.dimensionResourceName(2) + ", et ainsi de suite "
                + "jusqu'au mélange. Ceux qui jouent la dimension viennent en dernier, les plus chers, en cinq paliers. « Couper tous les "
                + "automatismes » les arrête tous ; chacun se met aussi en pause d'un clic. Achetés, ils traversent les univers.");
        getChildren().addAll(title, pauseButton, intro);

        for (int number = 1; number <= Dimension.PLAYABLE; number++) {
            String color = DimensionWindow.color(number);
            Label heading = new Label("Dimension " + number + " : " + Dimension.of(number).label());
            heading.setStyle(HEADING_STYLE + color + ";");
            Label balance = new Label();
            balance.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + ";");
            TileGrid grid = new TileGrid(250, 3, 10);
            grid.setMaxWidth(MAX_WIDTH);
            Card playCard = null;
            for (DimensionAutomation automation : DimensionAutomation.values()) {
                if (automation.dimension() != number) continue;
                if (automation.kind() == DimensionAutomation.Kind.PLAY) {
                    // Une seule carte pour les cinq paliers : chacun y mène.
                    if (playCard == null) {
                        playCard = new Card(color);
                        int dimension = number;
                        Card chain = playCard;
                        playCard.setOnAction(() -> {
                            if (chain.state() == Card.State.READY) game.buyNextPlayTier(dimension);
                            else game.setDimensionPlayEnabled(dimension, !game.isDimensionPlayEnabled(dimension));
                            refresh();
                        });
                        Button cut = new Button();
                        cut.setFocusTraversable(false);
                        cut.setMaxWidth(Double.MAX_VALUE);
                        cut.setOnAction(event -> {
                            game.setDimensionPlayEnabled(dimension, !game.isDimensionPlayEnabled(dimension));
                            refresh();
                        });
                        playSwitches[number] = cut;
                        VBox tile = new VBox(4, playCard, cut);
                        grid.add(tile);
                    }
                    cards.put(automation, playCard);
                    continue;
                }
                Card card = new Card(color);
                card.setOnAction(() -> {
                    if (game.ownsDimensionAutomation(automation)) {
                        game.setDimensionAutomationEnabled(automation, !game.isDimensionAutomationEnabled(automation));
                    } else {
                        game.buyDimensionAutomation(automation);
                    }
                    refresh();
                });
                cards.put(automation, card);
                grid.add(card);
            }
            VBox section = new VBox(6, heading, balance, grid);
            section.setAlignment(Pos.TOP_CENTER);
            headings[number] = heading;
            balances[number] = balance;
            sections[number] = section;
            getChildren().add(section);
        }
        refresh();
    }

    /** Ce que fait un automatisme, en une ligne : celle de sa carte. Celui qui joue dit sa cadence. */
    public static String brief(DimensionAutomation automation) {
        if (automation.kind() == DimensionAutomation.Kind.UPGRADE) return "Achète « " + automation.label() + " » dès qu'il est payable";
        String cadence = automation.cadence();
        return switch (automation.dimension()) {
            case 2 -> "Mène le point vers l'éclat le plus proche quand vous ne tenez aucune touche ; reprend sa direction " + cadence;
            case 3 -> "Plonge seul et cueille une boule " + cadence;
            case 4 -> "Tient le bouton jusqu'au centre, relâche, rappuie " + cadence;
            case 5 -> "Clique « bien » au passage d'un point dans une zone, un clic " + cadence + " au plus";
            case 6 -> "Tire la membrane à fond sur le point qui en touche le plus, " + cadence;
            case 7 -> "Clique au meilleur endroit, " + cadence;
            case 8 -> "Essaie une case près d'un point, " + cadence;
            case 9 -> "Reproduit le motif, une case " + cadence;
            default -> "Joue le mélange (vise, plonge, charge, clique), une action " + cadence;
        };
    }

    /** Ce qu'il fait, en détail : seulement en mode détails. */
    private static String detail(DimensionAutomation automation) {
        if (automation.kind() == DimensionAutomation.Kind.UPGRADE) {
            return "Il achète tant que la ressource suffit, jusqu'au plus haut niveau s'il y en a un, en gardant de quoi payer "
                    + "l'automatisme suivant de la dimension.";
        }
        String tiers = " Cinq paliers sur cette carte, du plus lent au plus rapide (5 s, 2 s, 1 s, 0,5 s, sans attente), achetés "
                + "l'un après l'autre : le plus rapide joue, et aucune bulle ni la matière noire ne change sa cadence.";
        return switch (automation.dimension()) {
            case 2 -> "Dès que vous lâchez les touches, il reprend la main, fenêtre ouverte ou non." + tiers;
            case 3 -> "Il replonge dès que le cube le rejette ; les améliorations du volume s'achètent entre deux plongées. "
                    + "Vous pouvez cueillir avec lui, fenêtre ouverte." + tiers;
            case 4 -> "Chaque fois que les spirales atteignent le centre, il relâche et en donne les instants. Les améliorations "
                    + "de la durée s'achètent quand même, à la main ou par leurs automatismes." + tiers;
            case 5 -> "Il joue tous les cercles de la boucle, fenêtre ouverte ou non, sans jamais rater ni viser le centre ; la "
                    + "zone touchée part ailleurs, et vos clics comptent en plus." + tiers;
            case 6 -> "Il lance une onde de toute sa portée depuis le point d'où elle en touche le plus, fenêtre ouverte ou non, "
                    + "dès que l'onde d'avant s'est éteinte ; vos tirées comptent en plus." + tiers;
            case 7 -> "Il clique là où se superposent le plus de zones (sinon au milieu), fenêtre ouverte ou non ; ses clics "
                    + "comptent dans la cadence, et vos clics comptent en plus." + tiers;
            case 8 -> "Il essaie une case à au plus un huitième de la grille d'un point, sur chaque axe, fenêtre ouverte ou non : "
                    + "sur une grande grille, il ne trouve presque jamais ; réduite, souvent. Vos essais comptent en plus." + tiers;
            case 9 -> "Il refait chaque motif sans faute jusqu'à " + Game.MIRROR_AUTO_MAX + " cases, puis se trompe, fenêtre "
                    + "ouverte ou non ; vos clics comptent en plus." + tiers;
            default -> "Il mène le point vers l'éclat le plus proche et plonge à sa profondeur, tient la charge et la relâche "
                    + "une fois pleine quand le rythme est au moins « bien », clique et passe à l'étape suivante dès qu'elle est "
                    + "gagnée, fenêtre ouverte ou non. Vous pouvez y jouer en plus." + tiers;
        };
    }

    /** « 100 000 segments » : un prix, avec ce qui le paie (moitié prix avec la bulle du sommet, version 0.182). */
    public static String price(Game game, DimensionAutomation automation) {
        double cost = game.dimensionAutomationCost(automation);
        String currency = Game.dimensionAutomationCurrency(automation);
        return (cost < 100 ? ElementText.number(cost) : cost < 1e6 ? Format.whole((long) cost) : Format.darkMatter(cost)) + " " + currency;
    }

    /** Recopie la partie : la coupure, ce que le joueur a pour payer, puis les cartes, si leur état a changé. */
    public void refresh() {
        boolean paused = game.isAutomationPaused();
        pauseButton.setText(paused ? "Relancer tous les automatismes" : "Couper tous les automatismes");
        pauseButton.setStyle("-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + (paused ? " -fx-text-fill: #1a0a08; -fx-background-color: #ff8a75; -fx-border-color: #ffffff;"
                : " -fx-text-fill: #9be7a8; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;"));
        int open = game.dimensionsUnlocked();
        for (int number = 1; number <= Dimension.PLAYABLE; number++) {
            boolean shown = number <= open;
            sections[number].setVisible(shown);
            sections[number].setManaged(shown);
            sections[number].setOpacity(paused ? 0.45 : 1);
            if (!shown) continue;
            DimensionAutomation first = firstOf(number);
            double balance = game.dimensionAutomationBalance(first);
            balances[number].setText("Payés en " + Game.dimensionAutomationCurrency(first) + " : vous en avez " + Format.darkMatter(balance));
        }
        StringBuilder key = new StringBuilder().append(open).append(paused).append(Detail.shown()).append(Card.signsShown());
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            key.append('/').append(game.ownsDimensionAutomation(automation) ? 'a' : '-')
                    .append(game.isDimensionAutomationEnabled(automation) ? 'm' : '-')
                    .append(game.canBuyDimensionAutomation(automation) ? 'p' : '-')
                    .append(game.isDimensionAutomationAvailable(automation) ? 'o' : '-');
        }
        if (key.toString().equals(shownKey)) return;
        shownKey = key.toString();
        for (int number = 2; number <= Math.min(open, Dimension.PLAYABLE); number++) showPlay(number, paused);
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() > open || automation.kind() == DimensionAutomation.Kind.PLAY) continue;
            Card card = cards.get(automation);
            if (game.ownsDimensionAutomation(automation)) {
                boolean on = game.isDimensionAutomationEnabled(automation);
                card.show(on ? Card.State.ON : Card.State.OFF, "", !on ? "en pause" : paused ? "tout coupé" : "en marche",
                        automation.title(), brief(automation), detail(automation) + " Un clic " + (on ? "le met en pause." : "le relance."),
                        "acheté", "");
            } else if (game.isDimensionAutomationAvailable(automation)) {
                card.show(game.canBuyDimensionAutomation(automation) ? Card.State.READY : Card.State.WAITING, "", "",
                        automation.title(), brief(automation), detail(automation) + " S'achète une fois, puis se met en pause d'un clic.",
                        price(game, automation), "");
            } else {
                card.show(Card.State.LOCKED, "", "verrouillé", automation.title(), brief(automation), detail(automation),
                        price(game, automation), !game.isDimensionAutomationOpen(automation.dimension()) ? opening(automation.dimension())
                                : "Après : " + automation.previous().title());
            }
        }
    }

    /**
     * La carte des cinq paliers de celui qui joue cette dimension (version 0.268) : le palier acheté en coin (« 2/5 »),
     * sa cadence et s'il est en marche, le prix du suivant. Le suivant payable, la carte est à portée et un clic
     * l'achète ; sinon, achetée, elle sert d'interrupteur pour tous ses paliers ; rien d'acheté et rien à portée, elle
     * attend ou dit ce qu'il faut avant.
     */
    private void showPlay(int dimension, boolean paused) {
        java.util.List<DimensionAutomation> tiers = DimensionAutomation.playTiers(dimension);
        if (tiers.isEmpty()) return;
        Card card = cards.get(tiers.get(0));
        DimensionAutomation owned = game.ownedPlayTier(dimension), next = game.nextPlayTier(dimension);
        String corner = (owned == null ? 0 : tiers.indexOf(owned) + 1) + "/" + tiers.size();
        String label = tiers.get(0).label();
        if (owned == null) {
            showSwitch(dimension, false, false);
            if (game.isDimensionAutomationAvailable(next)) {
                card.show(game.canBuyDimensionAutomation(next) ? Card.State.READY : Card.State.WAITING, corner, "", label, brief(next),
                        detail(next) + " Un clic achète le palier suivant quand il est payable, sinon met en pause ou relance.",
                        price(game, next), next.cadence());
            } else {
                card.show(Card.State.LOCKED, corner, "verrouillé", label, brief(next), detail(next), price(game, next),
                        !game.isDimensionAutomationOpen(dimension) ? opening(dimension) : "Après : " + next.previous().title());
            }
            return;
        }
        boolean on = game.isDimensionPlayEnabled(dimension);
        showSwitch(dimension, true, on);
        // Le plus rapide en marche joue (une vieille sauvegarde a pu en couper un seul) ; en pause, le plus rapide acheté.
        DimensionAutomation shown = owned;
        for (DimensionAutomation tier : tiers) if (on && game.isDimensionAutomationEnabled(tier)) shown = tier;
        String mark = (!on ? "en pause" : paused ? "tout coupé" : "en marche") + ", " + shown.cadence();
        if (next == null) {
            card.show(on ? Card.State.ON : Card.State.OFF, corner, mark, label, brief(owned),
                    detail(owned) + " Tous les paliers sont achetés. Un clic " + (on ? "le met en pause." : "le relance."), "acheté", "");
        } else if (game.canBuyDimensionAutomation(next)) {
            card.show(Card.State.READY, corner, mark, label, brief(owned),
                    detail(owned) + " Un clic achète le palier suivant, " + next.cadence() + ".", price(game, next), "→ " + next.cadence());
        } else {
            card.show(on ? Card.State.ON : Card.State.OFF, corner, mark, label, brief(owned),
                    detail(owned) + " Un clic " + (on ? "le met en pause" : "le relance") + " ; le palier suivant, " + next.cadence()
                            + ", s'achète ici dès qu'il est payable.", price(game, next), "prochain : " + next.cadence());
        }
    }

    /** Le bouton sous la carte des paliers : montré dès le premier palier acheté, « Couper » en marche, « Relancer » en pause. */
    private void showSwitch(int dimension, boolean shown, boolean on) {
        Button cut = playSwitches[dimension];
        cut.setVisible(shown);
        cut.setManaged(shown);
        cut.setText(on ? "Couper" : "Relancer");
        cut.setStyle("-fx-font-size: 12px; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + (on ? " -fx-text-fill: #ffb4a8; -fx-background-color: #2a1614; -fx-border-color: #b8584a;"
                : " -fx-text-fill: #9be7a8; -fx-background-color: #16202e; -fx-border-color: #3a4a5e;"));
    }

    /** Le bouton « Couper » sous la carte des paliers d'une dimension (2 à 10) : pour les vérifications. */
    public Button playSwitch(int dimension) { return playSwitches[dimension]; }

    /** Ce qui ouvre les automatismes d'une dimension : le palier de la graine et les univers formés. */
    static String opening(int dimension) {
        int stage = Game.dimensionAutomationStage(dimension), universes = Game.dimensionAutomationUniverses(dimension);
        return "Avec " + Game.SEED_STAGE_NAMES.get(stage - 1) + " et " + universes + " univers formés";
    }

    /** Le premier automatisme d'une dimension : il dit ce qui la paie. */
    private static DimensionAutomation firstOf(int number) {
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() == number && automation.previous() == null) return automation;
        }
        throw new IllegalArgumentException("Pas d'automatisme pour la dimension " + number);
    }

    /** La carte d'un automatisme (pour un palier de celui qui joue, la carte commune à ses cinq paliers) : pour les vérifications. */
    public Card card(DimensionAutomation automation) { return cards.get(automation); }

    /** Vrai si la section de cette dimension (1 à 10) se montre. */
    public boolean sectionShown(int number) { return sections[number].isVisible(); }

    /** Le titre de la section d'une dimension : pour les vérifications. */
    public String headingText(int number) { return headings[number].getText(); }

    /** La ligne de ce qui paie une dimension : pour les vérifications. */
    public String balanceText(int number) { return balances[number].getText(); }

    public String introText() { return intro.getText(); }

    public Button pauseButton() { return pauseButton; }
}

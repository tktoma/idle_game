package idle.ui.automation;

import idle.core.Game;
import idle.core.universe.DimensionAutomation;
import idle.ui.GameApp;
import idle.ui.common.Card;
import idle.ui.common.Detail;
import idle.ui.common.Format;
import idle.ui.common.Labels;
import idle.ui.common.TileGrid;
import idle.ui.universe.Dimension;
import idle.ui.universe.DimensionWindow;
import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Le sous-onglet « Dimensions » de l'automatisation (version 0.150), que la bulle du sommet de l'arbre-monde ouvre :
 * les automatismes qui jouent les dimensions à la place du joueur et achètent leurs améliorations
 * ({@link DimensionAutomation}).
 *
 * <p>En tête, ce qui paie chaque dimension, et le rappel que « Couper tous les automatismes » les arrête et qu'ils
 * traversent les univers ; le même bouton de coupure que le sous-onglet « Automatismes », pour s'en servir d'ici. Puis
 * une section par dimension ouverte (la ligne, le plan, le volume, la durée, la boucle depuis la version 0.153, la brane
 * depuis la version 0.154), qui dit
 * ce que le joueur a pour payer, et
 * une carte par automatisme : son nom, ce qu'il fait, son prix et ce qui le paie. Pas encore achetable, la carte dit
 * celui qu'il faut avoir acheté avant (« Après : … ») ; à acheter, un clic l'achète ; acheté, elle sert d'interrupteur,
 * en marche ou en pause.
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
    /** Par dimension (1 à 6) : son titre, la ligne de ce qui la paie, ses cartes, et la boîte qui les tient. */
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
                + "chaque dimension. Ceux de la ligne se paient en graines d'univers, ceux du plan en "
                + Game.dimensionResourceName(1) + " de la ligne, ceux du volume en " + Game.dimensionResourceName(2)
                + " du plan, ceux de la durée en " + Game.dimensionResourceName(3) + " du volume, ceux de la boucle en "
                + Game.dimensionResourceName(4) + " de la durée, ceux de la brane en " + Game.dimensionResourceName(5)
                + " de la boucle. « Couper tous les "
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
            for (DimensionAutomation automation : DimensionAutomation.values()) {
                if (automation.dimension() != number) continue;
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

    /** Ce que fait un automatisme, en une ligne : celle de sa carte. */
    public static String brief(DimensionAutomation automation) {
        return switch (automation) {
            case PLANE_AIM -> "Le point file vers l'éclat le plus proche quand vous ne tenez aucune touche";
            case VOLUME_DIVE -> "Plonge seul et cueille une boule toutes les "
                    + String.format(java.util.Locale.FRANCE, "%.1f", Game.VOLUME_AUTO_SHOT_SECONDS) + " s";
            case DURATION_HOLD -> "Tient le bouton jusqu'au centre, relâche, recommence";
            case LOOP_PLAY -> "Clique « bien » à chaque passage d'un point dans une zone";
            case BRANE_PULL -> "Tire la membrane à fond au milieu des points toutes les "
                    + String.format(java.util.Locale.FRANCE, "%.0f", Game.BRANE_AUTO_SECONDS) + " s";
            default -> "Achète « " + automation.label() + " » dès qu'il est payable";
        };
    }

    /** Ce qu'il fait, en détail : seulement en mode détails. */
    private static String detail(DimensionAutomation automation) {
        return switch (automation.kind()) {
            case PLAY -> switch (automation) {
                case PLANE_AIM -> "Dès que vous lâchez les touches, il reprend la main, fenêtre ouverte ou non.";
                case VOLUME_DIVE -> "Il replonge dès que le cube le rejette ; les améliorations du volume s'achètent entre "
                        + "deux plongées. Vous pouvez cueillir avec lui, fenêtre ouverte.";
                case DURATION_HOLD -> "Chaque fois que les spirales atteignent le centre, il relâche et en donne les instants. Les "
                        + "améliorations de la durée s'achètent quand même, à la main ou par leurs automatismes.";
                case LOOP_PLAY -> "Il joue tous les cercles de la boucle, fenêtre ouverte ou non, clic par clic, sans jamais "
                        + "rater ni viser le centre ; la zone touchée part ailleurs, et vos clics comptent en plus. Les "
                        + "améliorations de la boucle s'achètent quand même.";
                default -> "Il lance une onde de toute sa portée depuis le milieu des points, fenêtre ouverte ou non, tant "
                        + "que moins de trois ondes courent ; vos tirées comptent en plus. Les améliorations de la brane "
                        + "s'achètent quand même.";
            };
            case UPGRADE -> "Il achète tant que la ressource suffit, jusqu'au plus haut niveau s'il y en a un.";
        };
    }

    /** « 1 graine d'univers », « 100 000 segments » : un prix, avec ce qui le paie. */
    public static String price(DimensionAutomation automation) {
        double cost = automation.cost();
        String currency = Game.dimensionAutomationCurrency(automation);
        if (cost < 2 && automation.paidWith() == 0) currency = "graine d'univers";
        return (cost < 1e6 ? Format.whole((long) cost) : Format.darkMatter(cost)) + " " + currency;
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
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() > open) continue;
            Card card = cards.get(automation);
            if (game.ownsDimensionAutomation(automation)) {
                boolean on = game.isDimensionAutomationEnabled(automation);
                card.show(on ? Card.State.ON : Card.State.OFF, "", !on ? "en pause" : paused ? "tout coupé" : "en marche",
                        automation.label(), brief(automation), detail(automation) + " Un clic " + (on ? "le met en pause." : "le relance."),
                        "acheté", "");
            } else if (game.isDimensionAutomationAvailable(automation)) {
                card.show(game.canBuyDimensionAutomation(automation) ? Card.State.READY : Card.State.WAITING, "", "",
                        automation.label(), brief(automation), detail(automation) + " S'achète une fois, puis se met en pause d'un clic.",
                        price(automation), "");
            } else {
                card.show(Card.State.LOCKED, "", "verrouillé", automation.label(), brief(automation), detail(automation),
                        price(automation), automation.previous() == null || !game.isDimensionAutomationUnlocked()
                                ? "Avec la bulle du sommet" : "Après : " + automation.previous().label());
            }
        }
    }

    /** Le premier automatisme d'une dimension : il dit ce qui la paie. */
    private static DimensionAutomation firstOf(int number) {
        for (DimensionAutomation automation : DimensionAutomation.values()) {
            if (automation.dimension() == number && automation.previous() == null) return automation;
        }
        throw new IllegalArgumentException("Pas d'automatisme pour la dimension " + number);
    }

    /** La carte d'un automatisme : pour les vérifications. */
    public Card card(DimensionAutomation automation) {
        return cards.get(automation);
    }

    /** Vrai si la section de cette dimension (1 à 6) se montre. */
    public boolean sectionShown(int number) {
        return sections[number].isVisible();
    }

    /** Le titre de la section d'une dimension : pour les vérifications. */
    public String headingText(int number) {
        return headings[number].getText();
    }

    /** La ligne de ce qui paie une dimension : pour les vérifications. */
    public String balanceText(int number) {
        return balances[number].getText();
    }

    public String introText() {
        return intro.getText();
    }

    public Button pauseButton() {
        return pauseButton;
    }
}

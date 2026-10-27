package idle.ui.universe;

import idle.core.Game;
import idle.core.GameUniverse.SeedUpgrade;
import idle.core.math.BigNum;
import idle.ui.common.Format;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * Les améliorations de la graine (version 0.139), à gauche de sa vue : une branche par dimension, payée avec sa
 * ressource (les segments de la ligne, les facettes du plan, les sphères du volume, les instants de la durée). Chaque
 * branche a une amélioration de l'arrosage ou de l'étagère, et une du chrono. Les branches des dimensions 5 à 10 sont
 * déjà là, verrouillées, avec des améliorations à venir : ces dimensions n'existent pas encore.
 *
 * <p>Sous les branches, le chrono une fois son étage acheté : le temps qu'il garde, et le réglage de l'accélération.
 */
public final class SeedUpgradesPane extends VBox {

    /** Les branches : une par dimension, de la ligne à la dixième. */
    public static final int BRANCHES = Game.MAX_DIMENSIONS;
    private static final String[] BRANCH_NAMES = {"Ligne", "Plan", "Volume", "Durée"};
    private static final String COLOR = "#e6d27a";
    /** La largeur du texte d'une carte, dans le panneau de 320 pixels. */
    private static final double CARD_TEXT_WIDTH = 285;

    private final Game game;
    private final List<Button> tabs = new ArrayList<>();
    private final Label resource = new Label();
    private final VBox cards = new VBox(6);
    private final Map<SeedUpgrade, Button> buttons = new EnumMap<>(SeedUpgrade.class);
    private final List<Button> placeholders = new ArrayList<>();
    private final VBox chronoBox = new VBox(4);
    private final Label chronoLine = new Label();
    private final Label chronoSpeedLine = new Label();
    private final Slider chronoSpeed = new Slider(1, 2, 1);
    private int branch = 1;
    private boolean syncing;

    public SeedUpgradesPane(Game game) {
        super(8);
        this.game = game;
        setPadding(new Insets(4, 8, 4, 0));
        setPrefWidth(320);
        setMinWidth(260);
        setMaxWidth(340);
        Label title = new Label("Améliorations de la graine");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + COLOR + ";");
        FlowPane tabRow = new FlowPane(4, 4);
        for (int d = 1; d <= BRANCHES; d++) {
            int number = d;
            Button tab = new Button(branchName(d));
            tab.setFocusTraversable(false);
            tab.setOnAction(event -> select(number));
            tabs.add(tab);
            tabRow.getChildren().add(tab);
        }
        resource.setWrapText(true);
        resource.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f0e6b8;");
        for (SeedUpgrade upgrade : SeedUpgrade.values()) {
            Button button = card();
            button.setOnAction(event -> {
                if (game.buySeedUpgrade(upgrade)) refresh();
            });
            buttons.put(upgrade, button);
        }
        for (int k = 0; k < 2; k++) placeholders.add(card());
        chronoLine.setWrapText(true);
        chronoLine.setStyle("-fx-font-size: 13px; -fx-text-fill: #8fd3ff;");
        chronoSpeedLine.setWrapText(true);
        chronoSpeedLine.setStyle("-fx-font-size: 12px; -fx-text-fill: #b8c8d8;");
        chronoSpeed.setBlockIncrement(0.25);
        // Le réglage va par quarts, et le curseur aussi (version 0.141) : il ne saute plus d'un cran pendant qu'on le glisse.
        chronoSpeed.setMajorTickUnit(0.25);
        chronoSpeed.setMinorTickCount(0);
        chronoSpeed.setSnapToTicks(true);
        chronoSpeed.setFocusTraversable(false);
        chronoSpeed.valueProperty().addListener((obs, before, after) -> {
            if (syncing) return;
            game.setChronoSpeed(Math.round(after.doubleValue() * 4) / 4.0);
            refreshChrono();
        });
        Label chronoTitle = new Label("Le chrono");
        chronoTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #8fd3ff;");
        chronoBox.getChildren().addAll(chronoTitle, chronoLine, chronoSpeed, chronoSpeedLine);
        chronoBox.setPadding(new Insets(6, 0, 0, 0));
        getChildren().addAll(title, tabRow, resource, cards, chronoBox);
        setAlignment(Pos.TOP_LEFT);
        select(1);
    }

    /**
     * Une carte : ce que fait le niveau suivant, puis son prix sur une ligne à part (version 0.140 : plus visible, même
     * format). Le texte du bouton reste le texte entier, deux lignes ; ce qui s'affiche, ce sont ces deux lignes en
     * couleur, selon l'état de la carte.
     */
    private static Button card() {
        Button button = new Button();
        // Deux textes repliés dans une largeur fixe : un Label replié, dans le dessin d'un bouton, se replie sur la
        // largeur qui reste à côté du dessin lui-même (quelques pixels) et demande alors plus de mille pixels de haut.
        Text what = new Text(), cost = new Text();
        what.setWrappingWidth(CARD_TEXT_WIDTH);
        cost.setWrappingWidth(CARD_TEXT_WIDTH);
        VBox lines = new VBox(3, what, cost);
        lines.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        button.setGraphic(lines);
        button.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setFocusTraversable(false);
        button.hoverProperty().addListener((obs, before, after) -> paint(button));
        return button;
    }

    /** L'état d'une carte, qui décide de ses couleurs. */
    enum CardState {
        /** Achetable maintenant : bordure dorée, prix en vert. */
        BUY,
        /** Ouverte, mais la ressource manque : prix en orange, avec ce qui manque. */
        SHORT,
        /** Fermée : il faut d'abord la dimension, l'arrosoir ou l'étage du chrono. */
        LOCKED,
        /** Au plus haut. */
        MAX
    }

    /** Remplit une carte : ses deux lignes et son état. */
    private static void fill(Button button, String what, String cost, CardState state) {
        button.setText(what + "\n" + cost);
        button.getProperties().put(CardState.class, state);
        VBox lines = (VBox) button.getGraphic();
        ((Text) lines.getChildren().get(0)).setText(what);
        ((Text) lines.getChildren().get(1)).setText(cost);
        button.setDisable(state != CardState.BUY);
        paint(button);
    }

    /** Les couleurs d'une carte selon son état, plus vives au survol quand elle s'achète. */
    private static void paint(Button button) {
        CardState state = state(button);
        boolean hover = button.isHover() && state == CardState.BUY;
        String background = switch (state) {
            case BUY -> hover ? "linear-gradient(#4a4128, #352e1b)" : "linear-gradient(#3b3420, #2a2416)";
            case SHORT -> "#202636";
            case LOCKED -> "#17191f";
            case MAX -> "#1b2a20";
        };
        String border = switch (state) {
            case BUY -> hover ? "#fff1a8" : COLOR;
            case SHORT -> "#6b6248";
            case LOCKED -> "#34363d";
            case MAX -> "#6fbf7a";
        };
        String text = state == CardState.LOCKED ? "#8d8a80" : "#f5efd8";
        String price = switch (state) {
            case BUY -> "#a8f0b0";
            case SHORT -> "#ffb48a";
            case LOCKED -> "#8d8a80";
            case MAX -> "#9be7a8";
        };
        // Fermée, une carte reste lisible : JavaFX éteint à 40 % ce qui est désactivé, la carte garde son opacité.
        // La bordure plus épaisse d'une carte achetable prend sur son retrait : la carte ne change pas de taille.
        button.setStyle("-fx-opacity: 1; -fx-padding: " + (state == CardState.BUY ? "7 9" : "8 10")
                + "; -fx-background-radius: 7; -fx-border-radius: 7;"
                + " -fx-border-width: " + (state == CardState.BUY ? 2 : 1) + "; -fx-background-color: " + background + ";"
                + " -fx-border-color: " + border + "; -fx-cursor: " + (state == CardState.BUY ? "hand" : "default") + ";"
                + (state == CardState.BUY ? " -fx-effect: dropshadow(gaussian, " + COLOR + "55, 8, 0.2, 0, 0);" : ""));
        VBox lines = (VBox) button.getGraphic();
        lines.getChildren().get(0).setStyle("-fx-font-size: 13px; -fx-fill: " + text + ";");
        lines.getChildren().get(1).setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-fill: " + price + ";");
    }

    /** L'état d'une carte : pour les couleurs, et pour les vérifications. */
    static CardState state(Button button) {
        Object state = button.getProperties().get(CardState.class);
        return state instanceof CardState known ? known : CardState.LOCKED;
    }

    /** Le nom d'une branche : celui de sa dimension, ou « Dim. N » pour celles qui n'existent pas encore. */
    public static String branchName(int dimension) {
        return dimension <= BRANCH_NAMES.length ? BRANCH_NAMES[dimension - 1] : "Dim. " + dimension;
    }

    /** Vrai si la branche a ses vraies améliorations : sa dimension existe dans le jeu et elle est ouverte. */
    public boolean isBranchOpen(int dimension) {
        return dimension <= BRANCH_NAMES.length && game.dimensionsUnlocked() >= dimension;
    }

    /** Montre la branche de cette dimension. */
    public void select(int dimension) {
        branch = Math.max(1, Math.min(BRANCHES, dimension));
        refresh();
    }

    public int branch() {
        return branch;
    }

    private static String amount(double value) {
        return Format.darkMatter(BigNum.of(value));
    }

    private static String percent(double share) {
        return String.format(Locale.ROOT, "%.0f %%", share * 100).replace('.', ',');
    }

    /** Ce que fait le niveau suivant de cette amélioration. */
    private String describe(SeedUpgrade upgrade) {
        int level = game.seedUpgradeLevel(upgrade);
        return switch (upgrade) {
            case WATERING -> "Débit de l'arrosoir (niveau " + level + ") : " + amount(game.wateringCanRate()) + " → "
                    + amount(game.wateringCanRate() + Game.WATERING_CAN_PER_SECOND) + " arrosages par seconde, arrosoir tenu";
            case HOSE -> level == 0 ? "Tuyau d'arrosage : il arrose seul, sans qu'on le tienne, 10 % de ce que verse l'arrosoir"
                    : "Tuyau d'arrosage (niveau " + level + ") : " + percent(game.hoseShare()) + " → " + percent(game.hoseShare() + 0.1)
                            + " de l'arrosoir, versés seul";
            case SOIL -> "Terre fertile (niveau " + level + ") : chaque arrosage compte pour " + game.seedSoilFactor() + " → "
                    + game.seedSoilFactor() * 2;
            case CHRONO -> "Étage du chrono : un étage de plus à l'étagère, et un chrono qui se remplit de temps tout seul ;"
                    + " ce temps fait passer tout le jeu plus vite";
            case CHRONO_FILL -> "Remplissage du chrono (niveau " + level + ") : " + amount(game.chronoFillRate()) + " → "
                    + amount(game.chronoFillRate() + Game.CHRONO_FILL) + " s gardée par seconde";
            case CHRONO_CAPACITY -> "Réserve du chrono (niveau " + level + ") : " + Format.duration(game.chronoCapacity()) + " → "
                    + Format.duration(game.chronoCapacity() + Game.CHRONO_CAPACITY) + " au plus";
            case CHRONO_SPEED -> "Accélération du chrono (niveau " + level + ") : jusqu'à ×" + amount(game.chronoMaxSpeed())
                    + " → ×" + amount(game.chronoMaxSpeed() + 1);
            case CHRONO_LOSS -> "Pertes du chrono (niveau " + level + ") : à ×2, " + percent(game.chronoLoss(2)) + " → "
                    + percent(Math.min(Game.CHRONO_LOSS_MAX, Game.CHRONO_LOSS_STEP * Math.pow(Game.CHRONO_LOSS_CUT, level + 1)))
                    + " du temps pris se perd";
        };
    }

    /** Ce qui manque pour acheter : la dimension, l'arrosoir, l'étage du chrono ; ou le prix, et ce qui manque. */
    private String price(SeedUpgrade upgrade) {
        if (game.seedUpgradeLevel(upgrade) >= upgrade.max()) return "Au maximum";
        if (game.dimensionsUnlocked() < upgrade.dimension()) return "S'ouvre avec la dimension " + upgrade.dimension();
        if (upgrade == SeedUpgrade.WATERING && !game.isWateringCanUnlocked()) return "Il faut d'abord l'arrosoir, sur l'étagère";
        if (upgrade.chrono() && !game.isChronoUnlocked()) return "Il faut d'abord l'étage du chrono (branche Durée)";
        double cost = game.seedUpgradeCost(upgrade), owned = game.dimensionResource(upgrade.dimension());
        String resource = Game.dimensionResourceName(upgrade.dimension());
        String line = "Prix : " + amount(cost) + " " + resource;
        return owned + 1e-9 >= cost ? line : line + " (il en manque " + amount(Math.ceil(cost - owned)) + ")";
    }

    /** L'état d'une carte d'amélioration. */
    private CardState state(SeedUpgrade upgrade) {
        if (game.seedUpgradeLevel(upgrade) >= upgrade.max()) return CardState.MAX;
        if (game.canBuySeedUpgrade(upgrade)) return CardState.BUY;
        return game.isSeedUpgradeOpen(upgrade) ? CardState.SHORT : CardState.LOCKED;
    }

    /** Vrai si une amélioration de cette branche s'achète maintenant : son onglet porte une pastille. */
    public boolean hasPurchase(int dimension) {
        for (SeedUpgrade upgrade : SeedUpgrade.values()) {
            if (upgrade.dimension() == dimension && game.canBuySeedUpgrade(upgrade)) return true;
        }
        return false;
    }

    public void refresh() {
        for (int d = 1; d <= BRANCHES; d++) {
            Button tab = tabs.get(d - 1);
            boolean open = isBranchOpen(d), chosen = d == branch;
            // Version 0.140 : une pastille sur l'onglet d'une branche où quelque chose s'achète.
            tab.setText(branchName(d) + (open && hasPurchase(d) ? " ●" : ""));
            tab.setStyle("-fx-font-size: 12px; -fx-padding: 4 8; -fx-background-radius: 5; -fx-border-radius: 5;"
                    + (chosen ? " -fx-background-color: " + COLOR + "; -fx-text-fill: #10151f; -fx-font-weight: bold;"
                    : open ? " -fx-background-color: #2e2c20; -fx-text-fill: " + COLOR + "; -fx-border-color: #6b6248;"
                    : " -fx-background-color: #1a1a1f; -fx-text-fill: #6c6a60;"));
        }
        // Version 0.141 : les cartes restent en place, seul leur contenu change. Avant, chaque rafraîchissement (quatre
        // par seconde) les retirait puis les remettait : un clic dont l'appui et le relâché tombaient de part et d'autre
        // était perdu, et beaucoup de cartes semblaient ne pas s'acheter.
        List<Button> shown = new ArrayList<>();
        if (branch <= BRANCH_NAMES.length) {
            resource.setText(capitalize(Game.dimensionResourceName(branch)) + " " + dimensionName(branch) + " : "
                    + (game.dimensionsUnlocked() >= branch ? amount(game.dimensionResource(branch)) : "pas encore ouverte"));
            for (SeedUpgrade upgrade : SeedUpgrade.values()) {
                if (upgrade.dimension() != branch) continue;
                Button button = buttons.get(upgrade);
                fill(button, describe(upgrade), price(upgrade), state(upgrade));
                shown.add(button);
            }
        } else {
            // Les dimensions 5 à 10 : leurs branches existent déjà, sans rien à acheter.
            resource.setText("Dimension " + branch + " : pas encore créée. Sa ressource paiera une branche de la graine "
                    + "et une du chrono.");
            fill(placeholders.get(0), "??? (branche de la graine)", "S'ouvrira avec la dimension " + branch, CardState.LOCKED);
            fill(placeholders.get(1), "??? (branche du chrono)", "S'ouvrira avec la dimension " + branch, CardState.LOCKED);
            shown.addAll(placeholders);
        }
        if (!cards.getChildren().equals(shown)) cards.getChildren().setAll(shown);
        refreshChrono();
    }

    private static String dimensionName(int number) {
        return switch (number) {
            case 1 -> "de la ligne";
            case 2 -> "du plan";
            case 3 -> "du volume";
            default -> "de la durée";
        };
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private void refreshChrono() {
        boolean shown = game.isChronoUnlocked();
        chronoBox.setVisible(shown);
        chronoBox.setManaged(shown);
        if (!shown) return;
        // Pendant qu'on le glisse, le curseur est à la main du joueur : le rafraîchissement ne le replace pas.
        if (!chronoSpeed.isValueChanging()) {
            syncing = true;
            chronoSpeed.setMax(game.chronoMaxSpeed());
            chronoSpeed.setValue(game.chronoSpeed());
            syncing = false;
        }
        chronoLine.setText("Gardé : " + Format.duration(game.chronoStored()) + " sur " + Format.duration(game.chronoCapacity())
                + " (+" + amount(game.chronoFillRate()) + " s par seconde, même en absence)");
        double speed = game.chronoSpeed();
        chronoSpeedLine.setText(speed <= 1 + 1e-9 ? "Accélération ×1 : le chrono se remplit sans rien accélérer."
                : "Accélération ×" + amount(speed) + " : tout le jeu va " + amount(speed) + " fois plus vite, "
                + percent(game.chronoLoss(speed)) + " du temps pris se perd"
                + (game.isChronoRunning() ? "." : ". Le chrono est vide : il attend de se remplir."));
    }

    /** Pour les vérifications. */
    public Button button(SeedUpgrade upgrade) {
        return buttons.get(upgrade);
    }

    public List<Button> placeholders() {
        return placeholders;
    }

    public Slider chronoSlider() {
        return chronoSpeed;
    }

    public Label chronoText() {
        return chronoLine;
    }

    public Label resourceText() {
        return resource;
    }
}

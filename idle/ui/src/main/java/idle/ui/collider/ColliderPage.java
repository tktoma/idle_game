package idle.ui.collider;

import idle.core.Game;
import idle.core.collider.ColliderUpgrade;
import idle.ui.Readiness;
import idle.ui.common.Labels;
import idle.ui.common.SubTabs;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * L'onglet du collisionneur (version 0.057, refait en 0.059), ouvert à la première explosion : trois
 * sous-onglets : l'énergie (les faisceaux, leurs collisions et les huit générateurs), la chaîne
 * (énergie → molécules, payée en énergie et en quarks) et les améliorations, qui n'apparaissent
 * qu'avec le huitième générateur. Les hadrons ont disparu en 0.062 ; ils reviennent en 0.149 dans leur propre onglet,
 * qui se propose ici, comme le vide.
 */
public final class ColliderPage extends VBox {

    private final Game game;
    private final ColliderPane pane;
    private final Button energyTab = new Button("Énergie");
    private final Button chainTab = new Button("Chaîne");
    private final Button upgradesTab = new Button("Améliorations");
    private final HBox subTabs = new HBox(energyTab, chainTab, upgradesTab);
    /** L'offre du vide (version 0.129) : au premier Big Bang du troisième univers, il s'ouvre ici, contre de l'énergie. */
    private final Label voidOffer = new Label();
    private final Button openVoid = new Button();
    private final VBox voidBox = new VBox(6, voidOffer, openVoid);
    /** L'offre des hadrons (version 0.149) : avec la bulle de l'arbuste, au milieu, leur onglet s'ouvre ici, contre de l'énergie. */
    private final Label hadronsOffer = new Label();
    private final Button openHadrons = new Button();
    private final VBox hadronsBox = new VBox(6, hadronsOffer, openHadrons);

    public ColliderPage(Game game) {
        super(6);
        this.game = game;
        this.pane = new ColliderPane(game, this::refresh);
        setAlignment(Pos.TOP_CENTER);
        SubTabs.wire(true, this::select, this::refresh, energyTab, chainTab, upgradesTab);
        subTabs.setAlignment(Pos.CENTER);
        ScrollPane scroll = new ScrollPane(pane);
        Labels.transparent(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        offerBox(voidBox, voidOffer, openVoid, idle.ui.vacuum.VoidPage.COLOR, "#0d1a24");
        openVoid.setOnAction(event -> {
            game.openVoid();
            refresh();
        });
        offerBox(hadronsBox, hadronsOffer, openHadrons, idle.ui.hadrons.HadronsPage.COLOR, "#1c0f0d");
        openHadrons.setOnAction(event -> {
            game.openHadrons();
            refresh();
        });
        getChildren().addAll(subTabs, voidBox, hadronsBox, scroll);
        select(0);
    }

    /** La barre des sous-onglets : la disposition à gauche la place en haut de la fenêtre. */
    public HBox subTabs() {
        return subTabs;
    }

    /** Le bouton qui ouvre le vide : pour les vérifications. */
    public Button openVoidButton() {
        return openVoid;
    }

    public VBox voidBox() {
        return voidBox;
    }

    /** Le bouton qui ouvre l'onglet des hadrons : pour les vérifications. */
    public Button openHadronsButton() {
        return openHadrons;
    }

    public VBox hadronsBox() {
        return hadronsBox;
    }

    /** Habille une offre : sa phrase centrée, son bouton et son cadre, dans la couleur de ce qu'elle ouvre. */
    private static void offerBox(VBox box, Label offer, Button open, String color, String background) {
        offer.setWrapText(true);
        offer.setMaxWidth(760);
        offer.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        offer.setStyle("-fx-font-size: 13px; -fx-text-fill: #c8d3e0;");
        open.setFocusTraversable(false);
        open.setStyle("-fx-font-size: 13px; -fx-padding: 6 18; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;"
                + " -fx-background-color: " + background + "; -fx-text-fill: " + color + "; -fx-border-color: " + color + ";");
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 8 12; -fx-border-color: " + color + "; -fx-border-radius: 8;"
                + " -fx-background-color: #0b141c; -fx-background-radius: 8;");
        box.setMaxWidth(820);
    }

    public ColliderPane pane() {
        return pane;
    }

    public void select(int index) {
        if (index == ColliderPane.UPGRADES && !game.isColliderUpgradesUnlocked()) index = ColliderPane.ENERGY;
        pane.select(index);
        energyTab.setStyle(SubTabs.style(index == ColliderPane.ENERGY, ColliderPane.COLOR));
        chainTab.setStyle(SubTabs.style(index == ColliderPane.CHAIN, ColliderPane.COLOR));
        upgradesTab.setStyle(SubTabs.style(index == ColliderPane.UPGRADES, ColliderPane.COLOR));
    }

    public int selected() {
        return pane.section();
    }

    /** Passe au sous-onglet suivant ou précédent, en boucle. */
    public void step(int direction) {
        // Les sous-onglets affichés, dans l'ordre : les améliorations sautent tant qu'elles sont fermées.
        int[] shown = game.isColliderUpgradesUnlocked()
                ? new int[] {ColliderPane.ENERGY, ColliderPane.CHAIN, ColliderPane.UPGRADES}
                : new int[] {ColliderPane.ENERGY, ColliderPane.CHAIN};
        int at = 0;
        for (int i = 0; i < shown.length; i++) if (shown[i] == pane.section()) at = i;
        select(shown[Math.floorMod(at + direction, shown.length)]);
        refresh();
    }

    /** Vrai s'il y a quelque chose à faire : un quark top, une amélioration à prendre, le vide ou les hadrons à ouvrir. */
    public static boolean ready(Game game) {
        if (!game.isColliderUnlocked()) return false;
        return game.hasTop() || upgradeReady(game, true) || upgradeReady(game, false) || game.canOpenVoid() || game.canOpenHadrons();
    }

    /** Vrai si une amélioration payable attend : celles de la chaîne (son sous-onglet), ou les autres. */
    private static boolean upgradeReady(Game game, boolean chain) {
        for (ColliderUpgrade upgrade : ColliderUpgrade.values()) {
            if ((upgrade.section() == ColliderUpgrade.Section.CHAIN) == chain && game.canBuyColliderUpgrade(upgrade)) return true;
        }
        return false;
    }

    public void refresh() {
        boolean offered = game.isVoidOffered();
        voidBox.setVisible(offered);
        voidBox.setManaged(offered);
        if (offered) {
            double price = game.voidPrice(), energy = game.colliderEnergy();
            voidOffer.setText("Le vide se propose. Le collisionneur peut y faire naître des paires de particules virtuelles, qui "
                    + "donnent l'énergie du vide, la ressource du deuxième acte de la partie secondaire. Il s'ouvre contre "
                    + "dix minutes de production du collisionneur, comptées à son arrivée, une fois par univers."
                    + (energy < price ? " Encore " + idle.ui.common.Format.count(idle.core.math.BigNum.of(price - energy)) + " énergie." : ""));
            openVoid.setText("Ouvrir le vide : " + idle.ui.common.Format.count(idle.core.math.BigNum.of(price)) + " énergie");
            openVoid.setDisable(!game.canOpenVoid());
        }
        boolean hadrons = game.isHadronsOffered();
        hadronsBox.setVisible(hadrons);
        hadronsBox.setManaged(hadrons);
        if (hadrons) {
            double price = game.hadronsPrice(), energy = game.colliderEnergy();
            hadronsOffer.setText("Les hadrons se proposent. Dans leur onglet, les gluons lient les quarks des collisions en "
                    + "protons et en neutrons, puis, avec la bulle du cœur de l'arbre-monde, en particules exotiques. Chaque "
                    + "sorte renforce une grandeur ou baisse un prix, et les gluons qui les lient font garder plus à "
                    + "l'explosion et au Big Bang. L'onglet s'ouvre contre dix minutes de production du collisionneur, comptées à son "
                    + "arrivée, une fois par univers."
                    + (energy < price ? " Encore " + idle.ui.common.Format.count(idle.core.math.BigNum.of(price - energy)) + " énergie." : ""));
            openHadrons.setText("Ouvrir les hadrons : " + idle.ui.common.Format.count(idle.core.math.BigNum.of(price)) + " énergie");
            openHadrons.setDisable(!game.canOpenHadrons());
        }
        boolean upgrades = game.isColliderUpgradesUnlocked();
        upgradesTab.setVisible(upgrades);
        upgradesTab.setManaged(upgrades);
        energyTab.setText("Énergie" + Readiness.dot(Readiness.Kind.COLLIDER, game.hasTop()));
        chainTab.setText("Chaîne" + Readiness.dot(Readiness.Kind.COLLIDER, upgradeReady(game, true)));
        upgradesTab.setText("Améliorations" + Readiness.dot(Readiness.Kind.COLLIDER, upgradeReady(game, false)));
        pane.refresh();
    }

    public void frame(double elapsed) {
        pane.frame(elapsed);
    }
}

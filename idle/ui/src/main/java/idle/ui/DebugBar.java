package idle.ui;

import idle.core.Automation;
import idle.core.BigNum;
import idle.core.Effect;
import idle.core.Element;
import idle.core.Game;
import idle.core.PeriodicTable;
import idle.core.Upgrade;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

/**
 * Barre d'outils du profil de test : accélérer le temps et s'ajouter des ressources,
 * pour essayer un changement sans attendre.
 *
 * <p>Elle n'existe que si le jeu est lancé avec l'argument {@code --test}
 * (tâche Gradle {@code runTest}). Elle modifie directement l'état du jeu,
 * sans passer par les règles : c'est un outil de développement, pas du gameplay.
 */
final class DebugBar extends FlowPane {

    /** Vitesses proposées par le bouton « Temps », dans l'ordre des clics. */
    private static final double[] TIME_FACTORS = {1, 10, 100};

    private final Game game;
    private int timeIndex = 0;

    DebugBar(Game game) {
        super(8, 6);   // les boutons passent à la ligne quand la fenêtre est étroite
        this.game = game;
        setAlignment(Pos.CENTER);
        setPadding(new Insets(8, 12, 8, 12));
        setStyle("-fx-background-color: #2a1416; -fx-border-color: #ff8a80; -fx-border-width: 0 0 1 0;");

        Label title = new Label("PROFIL DE TEST");
        title.setStyle("-fx-text-fill: #ff8a80; -fx-font-weight: bold;");

        Button time = new Button("Temps ×1");
        time.setOnAction(event -> {
            timeIndex = (timeIndex + 1) % TIME_FACTORS.length;
            time.setText("Temps ×" + (int) timeFactor());
        });

        getChildren().addAll(
                title,
                time,
                cheat("+1 000 particules", () ->
                        game.state().setParticles(game.state().particles().add(BigNum.of(1000)))),
                cheat("Particules ×10", () ->
                        game.state().setParticles(game.state().particles().multiply(10))),
                cheat("+1 générateur", this::addGenerator),
                cheat("+1 atome", () -> addAtoms(1)),
                cheat("+10 atomes", () -> addAtoms(10)),
                cheat("Tableau complet", this::fillPeriodicTable));
    }

    /** Facteur d'accélération du temps choisi : 1 = vitesse normale. */
    double timeFactor() {
        return TIME_FACTORS[timeIndex];
    }

    /** Bouton de triche, sans effet tant que le premier générateur n'est pas créé. */
    private Button cheat(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> {
            if (game.isStarted()) action.run();
        });
        return button;
    }

    private void addAtoms(int count) {
        BigNum atoms = game.state().atoms().add(BigNum.of(count));
        game.state().setAtoms(game.isAtomCapLifted() ? atoms : atoms.min(Game.MAX_ATOMS));
        game.state().setTotalAtoms(game.state().totalAtoms().add(BigNum.of(count)));
    }

    /**
     * Saute à la fin du jeu : Persistance, tous les automatismes à leur cadence maximale et
     * tous les éléments à leur maximum d'exemplaires. Sert à essayer l'explosion sans attendre.
     */
    private void fillPeriodicTable() {
        if (game.state().totalAtoms().isZero()) addAtoms(1);
        for (Upgrade upgrade : game.upgrades()) {
            if (upgrade.effect() instanceof Effect.KeepUpgradesOnFusion) game.state().setLevel(upgrade.id(), 1);
        }
        for (Automation automation : game.automations()) {
            game.state().addAutomation(automation.id());
            game.state().setAutomationEnabled(automation.id(), true);
            game.state().setAutomationSpeedLevel(automation.id(), automation.maxSpeedLevel());
        }
        for (Element element : PeriodicTable.ELEMENTS) {
            game.state().setElementCount(element.number(), game.maxCopiesOf(element.category()));
        }
    }

    /** Débloque gratuitement le générateur suivant, s'il en reste. */
    private void addGenerator() {
        for (Upgrade upgrade : game.upgrades()) {
            if (upgrade.effect() instanceof Effect.AddGenerator && !game.isMaxed(upgrade.id())) {
                game.state().setLevel(upgrade.id(), game.levelOf(upgrade.id()) + 1);
                return;
            }
        }
    }
}

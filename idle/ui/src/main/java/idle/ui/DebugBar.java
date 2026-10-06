package idle.ui;

import idle.core.Achievement;
import idle.core.Automation;
import idle.core.BigNum;
import idle.core.Challenge;
import idle.core.DarkUpgrade;
import idle.core.Effect;
import idle.core.Element;
import idle.core.Game;
import idle.core.GameState;
import idle.core.Molecule;
import idle.core.PeriodicTable;
import idle.core.Upgrade;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Barre d'outils du profil de test : accélérer ou sauter le temps, s'ajouter des ressources,
 * sauter à une étape de la partie, pour essayer un changement sans attendre.
 *
 * <p>Elle n'existe que si le jeu est lancé avec l'argument {@code --test}
 * (tâche Gradle {@code runTest}). Elle modifie directement l'état du jeu,
 * sans passer par les règles : c'est un outil de développement, pas du gameplay.
 *
 * <p>La première ligne porte la vitesse du temps, les groupes d'outils et un résumé de la partie. Un clic
 * sur un groupe déplie ses boutons sur la ligne du dessous ; un second clic les replie. Un seul
 * groupe est ouvert à la fois, pour que la barre ne mange pas la fenêtre :
 * <ul>
 *   <li><b>Temps</b> : avancer d'un coup de dix minutes ou d'une heure ;</li>
 *   <li><b>Ressources</b> : particules, générateurs, atomes ;</li>
 *   <li><b>Étapes</b> : ouvrir le tableau, tout automatiser, remplir ou vider le tableau ;</li>
 *   <li><b>Matière noire</b> : exploser, matière noire, taille, arbre, défis, masse du tableau ;</li>
 *   <li><b>Succès</b> : tous les accorder ;</li>
 *   <li><b>Big Bang</b> : réunir ses conditions, le déclencher, s'ajouter des molécules ou de l'espace.</li>
 * </ul>
 * Un outil qui ajoute de la matière noire ouvre l'onglet correspondant, comme le ferait une
 * première explosion.
 */
final class DebugBar extends VBox {

    /** Vitesses proposées par le bouton « Vitesse », dans l'ordre des clics. */
    private static final double[] TIME_FACTORS = {1, 10, 100, 1000};

    private static final String ACCENT = "#ff8a80";
    private static final String BUTTON_STYLE = "-fx-font-size: 12px; -fx-padding: 3 10; -fx-cursor: hand;"
            + " -fx-background-radius: 4; -fx-border-radius: 4;";
    private static final String TOOL_STYLE = BUTTON_STYLE
            + " -fx-text-fill: #ffd9d4; -fx-background-color: #3a1c1f; -fx-border-color: #ff8a8066;";
    private static final String GROUP_STYLE = BUTTON_STYLE
            + " -fx-text-fill: " + ACCENT + "; -fx-background-color: #2a1416; -fx-border-color: " + ACCENT + ";";
    private static final String OPEN_GROUP_STYLE = BUTTON_STYLE
            + " -fx-text-fill: #2a1416; -fx-background-color: " + ACCENT + "; -fx-border-color: #ffffff;";

    private final Game game;
    private final FlowPane header = new FlowPane(8, 6);
    private final Label status = new Label();
    /** Les groupes d'outils : le bouton qui déplie chacun, et sa ligne de boutons. */
    private final Map<Button, FlowPane> groups = new LinkedHashMap<>();
    private Button open = null;
    private int timeIndex = 0;

    DebugBar(Game game) {
        super(6);
        this.game = game;
        setPadding(new Insets(8, 12, 8, 12));
        setStyle("-fx-background-color: #2a1416; -fx-border-color: " + ACCENT + "; -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.CENTER);

        Label title = new Label("PROFIL DE TEST");
        title.setStyle("-fx-text-fill: " + ACCENT + "; -fx-font-weight: bold;");
        header.getChildren().add(title);

        // La vitesse du temps reste sur la première ligne : c'est l'outil qu'on touche le plus.
        Button time = new Button("Vitesse ×1");
        time.setStyle(TOOL_STYLE);
        time.setFocusTraversable(false);
        time.setOnAction(event -> {
            timeIndex = (timeIndex + 1) % TIME_FACTORS.length;
            time.setText("Vitesse ×" + (int) timeFactor());
        });
        header.getChildren().add(time);
        getChildren().add(header);

        group("Temps",
                tool("+10 min", () -> skip(600)),
                tool("+1 h", () -> skip(3600)));
        group("Ressources",
                tool("+1 000 particules", () -> game.state().setParticles(game.state().particles().add(BigNum.of(1000)))),
                tool("Particules ×10", () -> game.state().setParticles(game.state().particles().multiply(10))),
                tool("Particules ×1e10", () -> game.state().setParticles(
                        game.state().particles().max(BigNum.ONE).multiply(BigNum.of(1, 10)))),
                tool("+1 générateur", () -> addGenerators(1)),
                tool("Tous les générateurs", () -> addGenerators(Integer.MAX_VALUE)),
                tool("+1 atome", () -> addAtoms(BigNum.ONE)),
                tool("+10 atomes", () -> addAtoms(BigNum.of(10))),
                tool("Atomes au plafond", () -> addAtoms(game.atomCap())));
        group("Étapes",
                tool("30 atomes créés", this::unlockTable),
                tool("Améliorations d'atomes au maximum", this::maxAtomUpgrades),
                tool("Automatismes au maximum", this::maxAutomations),
                tool("Un élément unique", () -> give(2, 1)),
                tool("1 de chaque élément", () -> fillTable(false)),
                tool("Tableau complet", () -> fillTable(true)),
                tool("Vider le tableau", this::emptyTable));
        group("Matière noire",
                tool("Exploser maintenant", this::explodeNow),
                tool("+1 matière noire", () -> addDarkMatter(1)),
                tool("+10 matières noires", () -> addDarkMatter(10)),
                tool("Taille ×1 000", () -> growDarkMatter(3)),
                tool("Taille ×1e10", () -> growDarkMatter(10)),
                tool("Arbre complet", this::fillTree),
                tool("Défi suivant réussi", this::completeNextChallenge),
                tool("Tableau plus lourd", () -> game.state().setTableWeightLevel(game.state().tableWeightLevel() + 1)),
                tool("Tableau à sa masse du début", () -> game.state().setTableWeightLevel(0)));
        group("Succès",
                tool("Tous les succès", this::grantAchievements));
        group("Big Bang",
                tool("Conditions réunies", this::readyForBigBang),
                tool("Big Bang maintenant", () -> {
                    readyForBigBang();
                    game.bigBang();
                }),
                tool("Une molécule de chaque", () -> {
                    for (Molecule molecule : game.molecules()) {
                        game.state().setMoleculeCount(molecule.id(), game.moleculeCount(molecule.id()) + 1);
                    }
                }),
                tool("Aucune molécule", () -> {
                    for (Molecule molecule : game.molecules()) game.state().setMoleculeCount(molecule.id(), 0);
                }),
                tool("+1 000 d'espace", () -> game.state().setSpace(game.state().space().add(BigNum.of(1000)))),
                tool("Espace ×10", () -> game.state().setSpace(game.state().space().max(BigNum.of(100)).multiply(10))),
                tool("Améliorations d'espace", () -> {
                    for (idle.core.SpaceUpgrade upgrade : game.spaceUpgrades()) game.state().addSpaceUpgrade(upgrade.id());
                }),
                tool("Trois de chaque petite molécule", () -> {
                    for (Molecule molecule : game.molecules()) {
                        if (molecule.kind() == Molecule.Kind.SIMPLE) game.state().setMoleculeCount(molecule.id(), Game.SUBSTANCE_MOLECULES);
                    }
                }),
                tool("Tout rassembler", () -> {
                    for (Molecule molecule : game.molecules()) {
                        if (molecule.hasState() && game.moleculeCount(molecule.id()) > 0) game.state().addSubstance(molecule.id());
                    }
                }),
                tool("Assemblage suivant", this::formNextAssembly),
                tool("Tout assembler", () -> {
                    while (formNextAssembly()) { }
                }),
                tool("Astre suivant", this::formNextBody),
                tool("Tous les astres", () -> {
                    while (formNextBody()) { }
                }));

        status.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffd9d4;");
        header.getChildren().add(status);
        refresh();
    }

    /** Facteur d'accélération du temps choisi : 1 = vitesse normale. */
    double timeFactor() {
        return TIME_FACTORS[timeIndex];
    }

    /** Met à jour le résumé de la partie, à droite de la première ligne. */
    void refresh() {
        GameState state = game.state();
        status.setText("jeu " + Format.duration(state.timePlayed())
                + "   |   partie " + Format.duration(game.stats().runTime())
                + "   |   " + state.explosions() + (state.explosions() > 1 ? " explosions" : " explosion")
                + "   |   tableau " + Format.multiplier(game.tableWeight())
                + (game.bigBangs() > 0 ? "   |   " + game.bigBangs() + " Big Bang" + (game.bigBangs() > 1 ? "s" : "") : ""));
    }

    /** Ajoute un groupe : son bouton sur la première ligne, et ses outils sur une ligne repliée. */
    private void group(String name, Button... tools) {
        FlowPane line = new FlowPane(6, 6);    // les boutons passent à la ligne quand la fenêtre est étroite
        line.setAlignment(Pos.CENTER);
        line.getChildren().addAll(tools);
        line.setVisible(false);
        line.setManaged(false);
        Button toggle = new Button(name);
        toggle.setStyle(GROUP_STYLE);
        toggle.setFocusTraversable(false);
        toggle.setOnAction(event -> open(open == toggle ? null : toggle));
        groups.put(toggle, line);
        header.getChildren().add(toggle);
        getChildren().add(line);
    }

    /** Déplie un groupe et replie les autres ; {@code null} les replie tous. */
    private void open(Button group) {
        open = group;
        groups.forEach((toggle, line) -> {
            boolean shown = toggle == group;
            line.setVisible(shown);
            line.setManaged(shown);
            toggle.setStyle(shown ? OPEN_GROUP_STYLE : GROUP_STYLE);
        });
    }

    /** Un outil : sans effet tant que le premier générateur n'est pas créé. */
    private Button tool(String text, Runnable action) {
        Button button = new Button(text);
        button.setStyle(TOOL_STYLE);
        button.setFocusTraversable(false);     // sinon Espace, pressé plus tard, rejouerait le dernier outil
        button.setOnAction(event -> {
            if (!game.isStarted()) return;
            action.run();
            refresh();
        });
        return button;
    }

    // ------------------------------------------------------------------
    // Temps
    // ------------------------------------------------------------------

    /**
     * Fait passer {@code seconds} secondes de jeu d'un coup. Le jeu les rejoue pas à pas, comme il
     * le ferait en temps réel : une heure demande quelques secondes de calcul, fenêtre figée.
     */
    private void skip(double seconds) {
        // Par tranches d'une minute : chaque tranche laisse les automatismes et les relevés suivre.
        for (double done = 0; done < seconds; done += 60) game.tick(Math.min(60, seconds - done));
    }

    // ------------------------------------------------------------------
    // Ressources
    // ------------------------------------------------------------------

    private void addAtoms(BigNum count) {
        BigNum atoms = game.state().atoms().add(count);
        game.state().setAtoms(game.isAtomCapLifted() ? atoms : atoms.min(game.atomCap()));
        game.state().setTotalAtoms(game.state().totalAtoms().add(count));
    }

    /** Débloque gratuitement jusqu'à {@code count} générateurs, tant qu'il en reste. */
    private void addGenerators(int count) {
        for (Upgrade upgrade : game.upgrades()) {
            if (!(upgrade.effect() instanceof Effect.AddGenerator)) continue;
            while (count-- > 0 && !game.isMaxed(upgrade.id())) {
                game.state().setLevel(upgrade.id(), game.levelOf(upgrade.id()) + 1);
            }
            return;
        }
    }

    // ------------------------------------------------------------------
    // Étapes
    // ------------------------------------------------------------------

    /** Assez d'atomes créés pour que l'automatisation et le tableau périodique soient débloqués. */
    private void unlockTable() {
        if (game.state().totalAtoms().isZero()) addAtoms(BigNum.ONE);
        game.state().setTotalAtoms(game.state().totalAtoms().max(Game.UNLOCK_TOTAL_ATOMS));
    }

    /** Porte à leur maximum les améliorations payées en atomes qui en ont un, Persistance comprise. */
    private void maxAtomUpgrades() {
        unlockTable();
        for (Upgrade upgrade : game.upgrades(idle.core.Resource.ATOMS)) {
            if (upgrade.hasLimit()) game.state().setLevel(upgrade.id(), upgrade.maxLevel());
        }
    }

    /** Donne tous les automatismes ordinaires, en marche, à leur cadence maximale. */
    private void maxAutomations() {
        unlockTable();
        for (Automation automation : game.automations()) {
            game.state().addAutomation(automation.id());
            game.state().setAutomationEnabled(automation.id(), true);
            game.state().setAutomationSpeedLevel(automation.id(), automation.maxSpeedLevel());
        }
    }

    /** Donne au moins {@code copies} exemplaires d'un élément, sans dépasser son maximum. */
    private void give(int number, int copies) {
        unlockTable();
        Element element = PeriodicTable.element(number);
        int wanted = Math.min(copies, game.maxCopiesOf(element));
        if (game.elementCount(number) < wanted) game.state().setElementCount(number, wanted);
    }

    /**
     * Remplit le tableau : un exemplaire de chaque élément (de quoi exploser), ou tous les
     * exemplaires. Avec Persistance et tous les automatismes, comme en fin de partie.
     */
    private void fillTable(boolean everyCopy) {
        maxAutomations();
        for (Upgrade upgrade : game.upgrades()) {
            if (upgrade.effect() instanceof Effect.KeepUpgradesOnFusion) game.state().setLevel(upgrade.id(), 1);
        }
        for (Element element : PeriodicTable.ELEMENTS) {
            give(element.number(), everyCopy ? Integer.MAX_VALUE : 1);
        }
    }

    private void emptyTable() {
        for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 0);
        game.state().setSynthesisCount(0);
    }

    // ------------------------------------------------------------------
    // Matière noire
    // ------------------------------------------------------------------

    /** Découvre les éléments qui manquent, puis fait exploser le tableau par les règles du jeu. */
    private void explodeNow() {
        for (Element element : PeriodicTable.ELEMENTS) give(element.number(), 1);
        game.explode();
    }

    /** Ajoute de la matière noire ; sans explosion encore, en compte une, pour que l'onglet s'ouvre. */
    private void addDarkMatter(int count) {
        if (game.state().explosions() == 0) game.state().setExplosions(1);
        game.state().setDarkMatter(game.state().darkMatter().add(BigNum.of(count)));
    }

    /** Multiplie la taille de la matière noire par dix puissance {@code decades}. */
    private void growDarkMatter(int decades) {
        if (game.state().explosions() == 0) game.state().setExplosions(1);
        game.state().setDarkMatterSize(game.state().darkMatterSize().multiply(BigNum.of(1, decades)));
    }

    /** Porte à leur maximum toutes les améliorations de matière noire qui en ont un. */
    private void fillTree() {
        if (game.state().explosions() == 0) game.state().setExplosions(1);
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            if (upgrade.hasLimit()) game.state().setDarkLevel(upgrade.id(), upgrade.maxLevel());
        }
    }

    /** Marque comme réussi le premier défi qui ne l'est pas encore, sans le jouer. */
    private void completeNextChallenge() {
        if (game.inChallenge()) return;
        for (Challenge challenge : game.challenges()) {
            if (game.isChallengeCompleted(challenge.id())) continue;
            if (game.state().explosions() == 0) game.state().setExplosions(1);
            game.state().addCompletedChallenge(challenge.id());
            return;
        }
    }

    // ------------------------------------------------------------------
    // Succès
    // ------------------------------------------------------------------

    private void grantAchievements() {
        for (Achievement achievement : game.achievements()) game.state().addAchievement(achievement.id());
    }

    // ------------------------------------------------------------------
    // Big Bang
    // ------------------------------------------------------------------

    /**
     * Réunit les cinq conditions du Big Bang, sans rien retirer : ce qu'il manque de particules,
     * d'atomes et de matière noire, les succès et les défis. Un défi en cours est arrêté, sans
     * être compté comme réussi.
     */
    /**
     * Forme le premier assemblage du catalogue qui ne l'est pas encore, en donnant au joueur ce
     * qu'il demande : les améliorations d'espace, les molécules qui manquent, rassemblées, et la
     * place de les loger.
     *
     * @return {@code false} s'ils sont tous formés
     */
    private boolean formNextAssembly() {
        for (idle.core.Assembly assembly : game.assemblies()) {
            if (!game.hasAssembly(assembly.id())) return form(assembly);
        }
        return false;
    }

    /** Forme cet assemblage, en donnant au joueur ce qu'il demande. */
    private boolean form(idle.core.Assembly assembly) {
        GameState state = game.state();
        if (state.bigBangs() == 0) state.setBigBangs(1);
        for (idle.core.SpaceUpgrade upgrade : game.spaceUpgrades()) state.addSpaceUpgrade(upgrade.id());
        for (java.util.Map.Entry<String, Integer> ingredient : assembly.ingredients().entrySet()) {
            state.addSubstance(ingredient.getKey());
            int missing = ingredient.getValue() - game.spareMolecules(ingredient.getKey());
            if (missing > 0) state.setMoleculeCount(ingredient.getKey(), game.moleculeCount(ingredient.getKey()) + missing);
        }
        // Trois fois l'espace utilisé : de quoi voir le bloc au milieu d'un espace encore vide autour.
        state.setSpace(state.space().max(game.usedSpace().multiply(3)));
        return game.formAssembly(assembly.id());
    }

    /**
     * Forme le premier astre du catalogue qui ne l'est pas encore, en donnant au joueur ce qu'il
     * demande : ses assemblages, ses molécules, et de quoi faire le compte de sa matière. Le
     * catalogue va du plus petit au plus grand : les astres dont il part sont donc déjà formés.
     *
     * @return {@code false} s'ils sont tous formés
     */
    private boolean formNextBody() {
        GameState state = game.state();
        for (idle.core.Body body : game.bodies()) {
            if (game.hasBody(body.id())) continue;
            if (state.bigBangs() == 0) state.setBigBangs(1);
            for (idle.core.SpaceUpgrade upgrade : game.spaceUpgrades()) state.addSpaceUpgrade(upgrade.id());
            for (String assembly : body.assemblies()) {
                if (!game.hasAssembly(assembly)) form(game.assembly(assembly));
            }
            for (java.util.Map.Entry<String, Integer> molecule : body.molecules().entrySet()) {
                int missing = molecule.getValue() - game.moleculeCount(molecule.getKey());
                if (missing > 0) state.setMoleculeCount(molecule.getKey(), game.moleculeCount(molecule.getKey()) + missing);
                state.addSubstance(molecule.getKey());
            }
            for (java.util.Map.Entry<Molecule.State, Integer> matter : body.matter().entrySet()) {
                int missing = matter.getValue() - game.gatheredInState(matter.getKey());
                if (missing <= 0) continue;
                // De la matière dans un état : des molécules de plus de la première sorte du catalogue qui a cet état.
                for (Molecule molecule : game.molecules()) {
                    if (molecule.state() != matter.getKey()) continue;
                    state.addSubstance(molecule.id());
                    state.setMoleculeCount(molecule.id(), game.moleculeCount(molecule.id()) + missing);
                    break;
                }
            }
            state.setSpace(state.space().max(game.usedSpace().multiply(3)));
            return game.formBody(body.id());
        }
        return false;
    }

    private void readyForBigBang() {
        GameState state = game.state();
        if (state.explosions() == 0) state.setExplosions(1);
        state.setActiveChallenge(null);
        state.setParticles(state.particles().max(Game.BIG_BANG_PARTICLES));
        state.setAtoms(state.atoms().max(Game.BIG_BANG_ATOMS));
        state.setTotalAtoms(state.totalAtoms().max(state.atoms()));
        state.setDarkMatter(state.darkMatter().max(Game.BIG_BANG_DARK_MATTER));
        grantAchievements();
        for (Challenge challenge : game.challenges()) state.addCompletedChallenge(challenge.id());
    }
}

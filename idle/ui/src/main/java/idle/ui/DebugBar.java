package idle.ui;

import idle.core.Game;
import idle.core.GameState;
import idle.core.Resource;
import idle.core.achievements.Achievement;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Quark;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.Element;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.sky.Assembly;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Effect;
import idle.core.upgrades.Upgrade;
import idle.ui.common.Format;
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
 *   <li><b>Big Bang</b> : réunir ses conditions, le déclencher, s'ajouter des molécules ou de l'espace ;</li>
 *   <li><b>Collisionneur</b> : énergie, générateurs, améliorations, quarks, quark top, énergie des trous noirs ;</li>
 *   <li><b>Actes</b> : refaire un acte depuis son début, vider le collisionneur ;</li>
 *   <li><b>Thèmes et notations</b> : tout débloquer, tout refermer, ou ouvrir et refermer chacun.</li>
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
    /** Ce que la barre peut faire des thèmes et notations à débloquer, qui vivent dans les réglages ; branché par l'application. */
    interface CosmeticTools {
        void unlockAll();

        void lockAll();

        /** Ouvre ce thème ou cette notation s'il est fermé, le referme sinon. */
        void toggle(Cosmetic cosmetic);
    }

    private CosmeticTools cosmetics;

    /** Le nom court d'un cosmétique, pour son bouton : « Sépia », « Romaine ». */
    private static String cosmeticName(Cosmetic cosmetic) {
        String label = cosmetic.label();
        int open = label.lastIndexOf('«');
        return open < 0 ? label : "± " + label.substring(open + 1).replace("»", "").trim();
    }

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
                    for (SpaceUpgrade upgrade : game.spaceUpgrades()) game.state().addSpaceUpgrade(upgrade.id());
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
                }),
                tool("Former la galaxie", () -> {
                    while (formNextBody()) { }
                    threeSystems();
                    game.formGalaxy();
                }),
                tool("Échelle suivante du cosmos", () -> {
                    while (formNextBody()) { }
                    threeSystems();
                    Cosmos next = game.nextCosmos();
                    if (next != null) game.state().setCosmosLevel(next.ordinal() + 1);
                }));
        group("Collisionneur",
                tool("Énergie ×1 000", () -> {
                    unlockCollider();
                    game.state().setColliderEnergy(Math.max(game.state().colliderEnergy(), 1000) * 1000);
                }),
                tool("Les 8 générateurs", () -> {
                    unlockCollider();
                    for (ColliderGenerator generator : ColliderGenerator.values()) {
                        if (game.state().colliderGeneratorsBought(generator) == 0) {
                            game.state().setColliderGeneratorsBought(generator, 1);
                            game.state().setColliderGenerators(generator, Math.max(1, game.state().colliderGenerators(generator)));
                        }
                    }
                }),
                tool("Collisions et saveurs", () -> {
                    unlockCollider();
                    for (ColliderUpgrade upgrade : java.util.List.of(ColliderUpgrade.COLLISIONS, ColliderUpgrade.FLAVOR_STRANGE,
                            ColliderUpgrade.FLAVOR_CHARM, ColliderUpgrade.FLAVOR_BOTTOM, ColliderUpgrade.FLAVOR_TOP)) {
                        game.state().setColliderUpgradeLevel(upgrade, Math.max(1, game.colliderUpgradeLevel(upgrade)));
                    }
                }),
                tool("Toutes les améliorations", () -> {
                    unlockCollider();
                    for (ColliderUpgrade upgrade : ColliderUpgrade.values()) game.state().setColliderUpgradeLevel(upgrade, upgrade.maxLevel());
                }),
                tool("+100 quarks de chaque", () -> {
                    unlockCollider();
                    for (Quark quark : Quark.values()) game.state().setQuarks(quark, game.quarks(quark) + 100);
                }),
                tool("Un quark top", () -> {
                    unlockCollider();
                    game.state().setTopLeft(Game.TOP_SECONDS);
                }),
                tool("Énergie des trous noirs", () -> {
                    unlockCollider();
                    game.state().setColliderEnergyProduced(Math.max(game.state().colliderEnergyProduced(), Game.SUPERMASSIVE_ENERGY));
                }));
        group("Actes",
                tool("Refaire l'acte 1", () -> game.state().clearMatter()),
                tool("Refaire l'acte 2", () -> {
                    game.state().clearMatter();
                    game.state().clearDarkMatter();
                }),
                tool("Refaire l'acte 3", () -> game.state().clearBigBangAct()),
                tool("Vider le collisionneur", () -> game.state().clearCollider()));
        // Un bouton par thème et par notation à débloquer : un clic l'ouvre, un second le referme.
        java.util.List<Button> cosmeticTools = new java.util.ArrayList<>();
        cosmeticTools.add(tool("Tout débloquer", () -> { if (cosmetics != null) cosmetics.unlockAll(); }));
        cosmeticTools.add(tool("Tout refermer", () -> { if (cosmetics != null) cosmetics.lockAll(); }));
        for (idle.ui.Cosmetic cosmetic : idle.ui.Cosmetic.values()) {
            cosmeticTools.add(tool(cosmeticName(cosmetic), () -> { if (cosmetics != null) cosmetics.toggle(cosmetic); }));
        }
        group("Thèmes et notations", cosmeticTools.toArray(new Button[0]));

        status.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffd9d4;");
        header.getChildren().add(status);
        refresh();
    }

    /** Branche les outils qui ouvrent ou referment les cosmétiques, qui vivent dans les réglages de l'application. */
    void setCosmetics(CosmeticTools cosmetics) {
        this.cosmetics = cosmetics;
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
        for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
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
        for (Assembly assembly : game.assemblies()) {
            if (!game.hasAssembly(assembly.id())) return form(assembly);
        }
        return false;
    }

    /** Forme cet assemblage, en donnant au joueur ce qu'il demande. */
    private boolean form(Assembly assembly) {
        GameState state = game.state();
        if (state.bigBangs() == 0) state.setBigBangs(1);
        for (SpaceUpgrade upgrade : game.spaceUpgrades()) state.addSpaceUpgrade(upgrade.id());
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
        for (Body body : game.bodies()) {
            if (game.hasBody(body.id())) continue;
            if (state.bigBangs() == 0) state.setBigBangs(1);
            // Une matière exotique attend le huitième Big Bang : l'outil y va d'office.
            if (body.tier() == Body.Tier.EXOTIC && !game.isExoticUnlocked()) state.setBigBangs(8);
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) state.addSpaceUpgrade(upgrade.id());
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
            // L'outil de mise au point saute la maturation, et donne l'énergie produite qu'un trou noir demande.
            if (game.needsColliderEnergy(body)) state.setColliderEnergyProduced(Math.max(state.colliderEnergyProduced(), game.colliderEnergyNeeded(body)));
            boolean formed = game.formBody(body.id());
            if (formed) state.setMaturing(body.id(), 0);
            return formed;
        }
        return false;
    }

    /** Range d'office les étoiles et les planètes libres en systèmes solaires, jusqu'à ce que la galaxie en ait assez. */
    private void threeSystems() {
        if (game.state().bigBangs() < 6) game.state().setBigBangs(6);
        while (game.solarSystemsComplete() < Game.SYSTEMS_FOR_GALAXY) {
            java.util.List<String> stars = game.freeForSystems(Body.Tier.STAR);
            java.util.List<String> planets = game.freeForSystems(Body.Tier.PLANET);
            if (stars.isEmpty() || planets.size() < Game.SYSTEM_MIN_PLANETS) return;
            game.foundSystem(stars.get(0));
            int index = game.state().solarSystems().size() - 1;
            for (int i = 0; i < Game.SYSTEM_MIN_PLANETS; i++) game.addPlanet(index, planets.get(i));
        }
    }

    /** Le collisionneur s'ouvre à la première explosion : l'outil la compte, s'il n'y en a pas eu. */
    private void unlockCollider() {
        if (game.state().explosions() == 0 && game.bigBangs() == 0) game.state().setExplosions(1);
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

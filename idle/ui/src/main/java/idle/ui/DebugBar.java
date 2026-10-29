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
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
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
 *   <li><b>Dimensions</b> (version 0.140) : une rangée par dimension (l'ouvrir, son seuil, ses améliorations), la
 *       boucle comprise depuis la version 0.153, la brane depuis la version 0.154, le clicker depuis la version 0.155,
 *       la grille depuis la version 0.156, puis la graine, son chrono, le rond de croissance et la météo ;</li>
 *   <li><b>Bots dimension</b> (version 0.140) : le joueur appliqué d'une dimension (de la ligne à la durée) joue la
 *       partie en cours.</li>
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
    /** Les groupes des onglets, à gauche. */
    private final FlowPane header = new FlowPane(8, 6);
    /** À droite (version 0.122) : la vitesse, le temps, les bots, les paliers, le résumé de la partie. */
    private final FlowPane right = new FlowPane(8, 6);
    /** Le groupe de chaque onglet du jeu, par le nom de l'onglet : la barre suit l'onglet affiché. */
    private final Map<String, Button> tabGroups = new LinkedHashMap<>();
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

    // Les bots (version 0.084)
    private final javafx.scene.control.ComboBox<String> botMode = new javafx.scene.control.ComboBox<>();
    private final Button botLive = new Button();
    /** L'avance rapide procède par petites tranches sur le fil JavaFX et reste annulable. */
    private javafx.animation.Timeline botTimeline;
    private BotFastForwardJob fastForward;
    private final Button botCancel = new Button("Annuler l'avance rapide");
    private final Label botNote = new Label();
    /** La politique choisie : DEPENDANCES d'abord, la meilleure des campagnes. */
    private int modeIndex = Math.max(0, BotBridge.modes().indexOf("DEPENDANCES"));
    private BotBridge bot;
    private boolean live = false;
    private double sinceDecision = 0;

    // Les bots des dimensions (version 0.140)
    /** Les rangées des outils des dimensions : leur nom et leur ressource. */
    private static final String[] DIMENSION_ROWS = {"Ligne (segments)", "Plan (facettes)", "Volume (sphères)", "Durée (instants)",
            "Boucle (nœuds)", "Brane (ondes)", "Clicker (étincelles)", "Grille (repères)"};
    /** Pas joués par impulsion en avance rapide : 400 pas de 0,05 s, vingt secondes de jeu toutes les 25 ms. */
    static final int DIMENSION_BATCH = 400;
    private final javafx.scene.control.ComboBox<String> dimensionChoice = new javafx.scene.control.ComboBox<>();
    private final Button dimensionLive = new Button();
    private final Button dimensionCancel = new Button("Annuler l'avance rapide");
    private DimensionBotBridge dimensionBot;
    private boolean dimensionLiveOn = false;
    /** Vrai pendant une avance rapide d'un bot de dimension : l'annuler relâche ce qu'il tenait. */
    private boolean dimensionJob = false;

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

        // Version 0.122 : à gauche, un groupe d'outils par onglet du jeu, qui suit l'onglet affiché ; à droite, ce qui
        // sert partout : la vitesse, le temps, les bots, la remise à zéro des paliers, et le résumé de la partie.
        header.setAlignment(Pos.CENTER_LEFT);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(header, Priority.ALWAYS);
        HBox top = new HBox(10, title, header, right);
        top.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(top);

        Button time = new Button("Vitesse ×1");
        time.setStyle(TOOL_STYLE);
        time.setFocusTraversable(false);
        time.setOnAction(event -> {
            timeIndex = (timeIndex + 1) % TIME_FACTORS.length;
            time.setText("Vitesse ×" + (int) timeFactor());
        });
        right.getChildren().add(time);

        tabGroup("PARTICLES", "Particules",
                tool("+1 000 particules", () -> game.state().setParticles(game.state().particles().add(BigNum.of(1000)))),
                tool("Particules ×10", () -> game.state().setParticles(game.state().particles().multiply(10))),
                tool("Particules ×1e10", () -> game.state().setParticles(
                        game.state().particles().max(BigNum.ONE).multiply(BigNum.of(1, 10)))),
                tool("+1 générateur", () -> addGenerators(1)),
                tool("Tous les générateurs", () -> addGenerators(Integer.MAX_VALUE)),
                tool("Régime prêt", () -> {
                    game.state().setRegimeLeft(0);
                    game.state().setRegimeRest(0);
                }));
        tabGroup("ATOMS", "Atomes",
                tool("+1 atome", () -> addAtoms(BigNum.ONE)),
                tool("+10 atomes", () -> addAtoms(BigNum.of(10))),
                tool("Atomes au plafond", () -> addAtoms(game.atomCap())),
                tool("30 atomes créés", this::unlockTable),
                tool("Améliorations d'atomes au maximum", this::maxAtomUpgrades),
                tool("Un élément unique", () -> give(2, 1)),
                tool("1 de chaque élément", () -> fillTable(false)),
                tool("Tableau complet", () -> fillTable(true)),
                tool("Vider le tableau", this::emptyTable),
                tool("+1 élément gratuit", () -> game.state().setFreeElementsUsed(Math.max(0, game.state().freeElementsUsed() - 1))));
        tabGroup("AUTOMATION", "Automatisation",
                tool("Automatismes au maximum", this::maxAutomations));
        tabGroup("DARK_MATTER", "Matière noire",
                tool("Exploser maintenant", this::explodeNow),
                tool("+1 matière noire", () -> addDarkMatter(1)),
                tool("+10 matières noires", () -> addDarkMatter(10)),
                tool("Taille ×1 000", () -> growDarkMatter(3)),
                tool("Taille ×1e10", () -> growDarkMatter(10)),
                tool("Arbre complet", this::fillTree),
                tool("Défi suivant réussi", this::completeNextChallenge),
                tool("Les dix défis réussis", () -> {
                    for (idle.core.darkmatter.Challenge challenge : idle.core.darkmatter.Challenges.DEFAULT) game.state().addCompletedChallenge(challenge.id());
                }),
                tool("Fuite réussie", () -> game.state().addCompletedChallenge(idle.core.darkmatter.Challenges.LEAK.id())),
                tool("Calme du Big Bang complet", () -> game.state().setBangCalm(Game.BIG_BANG_CALM_SECONDS)),
                tool("Tableau plus lourd", () -> game.state().setTableWeightLevel(game.state().tableWeightLevel() + 1)),
                tool("Tableau à sa masse du début", () -> game.state().setTableWeightLevel(0)));
        tabGroup("COLLIDER", "Collisionneur",
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
                }),
                // Le vide (version 0.129) : l'ouvrir sans attendre le premier Big Bang du troisième univers ni payer.
                tool("Ouvrir le vide", () -> {
                    unlockCollider();
                    game.testOpenVoid();
                }));
        tabGroup("BIG_BANG", "Big Bang",
                tool("Conditions réunies", this::readyForBigBang),
                tool("Big Bang maintenant", () -> {
                    readyForBigBang();
                    game.bigBang();
                }),
                tool("Paliers ouverts payés", () -> bangsAtLeast(0)),
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
                    // L'univers (version 0.110) se forme pour de vrai, remise à zéro comprise : monter seulement
                    // l'échelle laissait un univers à moitié formé, que la brindille ne pouvait pas faire suivre.
                    if (next == Cosmos.UNIVERSE) game.testFormUniverse();
                    else if (next != null) game.state().setCosmosLevel(next.ordinal() + 1);
                }),
                tool("Défi de Big Bang suivant réussi", () -> {
                    for (idle.core.bigbang.BangChallenge challenge : idle.core.bigbang.BangChallenge.values()) {
                        if (!game.isBangChallengeCompleted(challenge)) { game.state().addCompletedBangChallenge(challenge.name()); break; }
                    }
                }),
                tool("Tous les défis de Big Bang", () -> {
                    for (idle.core.bigbang.BangChallenge challenge : idle.core.bigbang.BangChallenge.values()) game.state().addCompletedBangChallenge(challenge.name());
                }));
        tabGroup("UNIVERSE", "Univers",
                tool("Former l'univers", game::testFormUniverse),
                tool("Rouvrir les paliers", game::startNextUniverse),
                tool("+1 univers", game::testAddUniverse),
                tool("−1 univers", game::testRemoveUniverse),
                tool("Palier suivant de la graine", this::nextSeedStage),
                tool("+1 arrosage", () -> {
                    game.state().setSeedSprouted(true);
                    game.state().setSeedWaterings(game.state().seedWaterings() + 1);
                }),
                tool("Graine à zéro", () -> {
                    game.state().setSeedSprouted(false);
                    game.state().setSeedWaterings(0);
                }),
                // L'arrosoir (version 0.137) : le reprendre pour revoir le bouton de l'étagère.
                tool("Reprendre l'arrosoir", () -> {
                    game.setWatering(false);
                    game.state().setWateringCan(false);
                }),
                // Les bulles de l'arbre (version 0.142) : de quoi les payer, et les reprendre.
                tool("+1 000 d'énergie du vide accumulée", () -> {
                    String last = String.valueOf(Math.max(1, game.universes()));
                    game.state().noteVoidHistory(last, game.state().voidHistory().getOrDefault(last, 0.0) + 1000);
                }),
                tool("Bulles de l'arbre à zéro", () -> {
                    game.state().clearTreeBubbles();
                    game.state().setVoidSpent(0);
                }));
        // Version 0.140 : une rangée par dimension, avec sa ressource (l'ouvrir, s'approcher du seuil, l'atteindre, ses
        // améliorations au plus haut ou à zéro), puis la graine et son chrono.
        VBox dimensionRows = new VBox(4);
        // La boucle (version 0.153), la brane (version 0.154), le clicker (version 0.155) et la grille (version 0.156) ont
        // leur rangée ; ils n'ont pas encore de bot.
        for (int d = 1; d <= DIMENSION_ROWS.length; d++) {
            int number = d;
            dimensionRows.getChildren().add(row(DIMENSION_ROWS[d - 1],
                    tool("Ouvrir", () -> openDimension(number)),
                    tool("+10 % du seuil", () -> addDimensionShards(number, dimensionThreshold(number) / 10)),
                    tool("Seuil atteint", () -> reachDimensionThreshold(number)),
                    tool("Améliorations au maximum", () -> setDimensionLevels(number, true)),
                    tool("Améliorations à zéro", () -> setDimensionLevels(number, false))));
        }
        dimensionRows.getChildren().add(row("Graine et chrono",
                tool("Améliorations de la graine au maximum", () -> setSeedUpgrades(true)),
                tool("Améliorations de la graine à zéro", () -> setSeedUpgrades(false)),
                tool("Chrono plein", () -> {
                    if (!game.isChronoUnlocked()) game.state().setSeedUpgradeLevel(Game.SeedUpgrade.CHRONO.ordinal(), 1);
                    game.state().setChronoStored(game.chronoCapacity());
                }),
                tool("Chrono vide", () -> game.state().setChronoStored(0)),
                // Version 0.153 : le rond de croissance, tout de suite, s'il est là (la boucle ouverte).
                tool("Rond de croissance", () -> {
                    if (game.isBloomUnlocked()) game.testShowBloom();
                    else note("Le rond de croissance vient avec la boucle : « Ouvrir » dans sa rangée.");
                }),
                // Version 0.155 : la météo de la graine, tout de suite, si elle est là (le clicker ouvert).
                tool("Beau temps", () -> weather(Game.Weather.SUN)),
                tool("Orage", () -> weather(Game.Weather.STORM))));
        group("Dimensions", dimensionRows);
        // Version 0.140 : à côté des dimensions, leurs bots (DimensionBots, le joueur appliqué qui règle les seuils), qui
        // jouent la dimension choisie de la partie en cours, en direct ou en avance rapide.
        dimensionChoice.getItems().setAll(DimensionBotBridge.LABELS);
        dimensionChoice.setValue(DimensionBotBridge.LABELS.get(0));
        dimensionChoice.setFocusTraversable(false);
        dimensionChoice.setStyle("-fx-font-size: 12px;");
        dimensionChoice.setOnAction(event -> {
            stopDimensionLive();
            dimensionBot = null;
            refreshDimensionBots();
        });
        dimensionLive.setStyle(TOOL_STYLE);
        dimensionLive.setFocusTraversable(false);
        dimensionLive.setOnAction(event -> toggleDimensionLive());
        dimensionCancel.setStyle(TOOL_STYLE);
        dimensionCancel.setFocusTraversable(false);
        dimensionCancel.setOnAction(event -> cancelFastForward());
        dimensionCancel.setDisable(true);
        Label dimensionTitle = new Label("Bot de dimension :");
        dimensionTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffd9d4;");
        group("Bots dimension", dimensionTitle, dimensionChoice, dimensionLive, dimensionCancel,
                tool("+1 min avec le bot", () -> dimensionSkip(60, false)),
                tool("+5 min avec le bot", () -> dimensionSkip(300, false)),
                tool("Jusqu'au rond vert (30 min au plus)", () -> dimensionSkip(1800, true)));
        tabGroup("VOID", "Le vide",
                tool("+10 énergie du vide", () -> game.testAddVoidEnergy(BigNum.of(10))),
                tool("+1 000 énergie du vide", () -> game.testAddVoidEnergy(BigNum.of(1000))),
                tool("+1 million d'énergie du vide", () -> game.testAddVoidEnergy(BigNum.of(1e6))),
                tool("Ouvrir le trou noir", game::testOpenVoidHole),
                tool("Masse au maximum", () -> {
                    game.testOpenVoidHole();
                    game.state().setVoidSacrificed(BigNum.of(1, 40_000_000));
                }),
                tool("Améliorations au maximum", () -> {
                    // Le gain n'a pas de plafond : dix niveaux suffisent à l'essai.
                    for (Game.VoidUpgrade upgrade : Game.VoidUpgrade.values())
                        game.state().setVoidLevel(upgrade.ordinal(), upgrade == Game.VoidUpgrade.GAIN ? 10 : upgrade.max());
                }),
                tool("Améliorations à zéro", () -> {
                    for (Game.VoidUpgrade upgrade : Game.VoidUpgrade.values()) game.state().setVoidLevel(upgrade.ordinal(), 0);
                }));
        tabGroup("ACHIEVEMENTS", "Succès",
                tool("Tous les succès", this::grantAchievements));
        // Les skins, et avec eux les thèmes et les notations à débloquer (version 0.122 : plus de groupe à part).
        java.util.List<javafx.scene.Node> skinTools = new java.util.ArrayList<>(java.util.List.of(
                tool("+5 fusions", () -> game.testAddFusions(Game.SKINS_FUSIONS)),
                tool("Tous les skins", () -> game.testAllSkins(true)),
                tool("Aucun skin", () -> game.testAllSkins(false)),
                tool("Tous les thèmes et notations", () -> { if (cosmetics != null) cosmetics.unlockAll(); }),
                tool("Aucun thème ni notation", () -> { if (cosmetics != null) cosmetics.lockAll(); })));
        for (idle.ui.Cosmetic cosmetic : idle.ui.Cosmetic.values()) {
            skinTools.add(tool(cosmeticName(cosmetic), () -> { if (cosmetics != null) cosmetics.toggle(cosmetic); }));
        }
        tabGroup("SKINS", "Skins", skinTools.toArray(new javafx.scene.Node[0]));

        // À droite : le temps, les bots, la remise à zéro des paliers.
        groupRight("Temps",
                tool("+10 min", () -> skip(600)),
                tool("+1 h", () -> skip(3600)),
                tool("+6 h", () -> skip(6 * 3600)));
        // Les bots (version 0.084) : le bot V2 joue la partie en cours, en direct ou en avance rapide.
        // La politique se choisit dans une liste déroulante (version 0.085), plus en cliquant plusieurs fois.
        botMode.getItems().setAll(BotBridge.modes());
        botMode.setValue(BotBridge.modes().get(modeIndex));
        botMode.setVisibleRowCount(12);
        botMode.setFocusTraversable(false);
        botMode.setStyle("-fx-font-size: 12px;");
        botMode.setOnAction(event -> {
            int chosen = BotBridge.modes().indexOf(botMode.getValue());
            if (chosen >= 0 && chosen != modeIndex) {
                modeIndex = chosen;
                bot = null;
            }
            refreshBots();
        });
        botLive.setStyle(TOOL_STYLE);
        botLive.setFocusTraversable(false);
        botLive.setOnAction(event -> {
            if (!game.isStarted()) return;
            live = !live && bot() != null;
            sinceDecision = 0;
            refreshBots();
        });
        botNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffd9d4;");
        Label botTitle = new Label("Bot :");
        botTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffd9d4;");
        botCancel.setStyle(TOOL_STYLE);
        botCancel.setOnAction(event -> cancelFastForward());
        botCancel.setDisable(true);
        groupRight("Bots", botTitle, botMode, botLive, botCancel,
                tool("+10 min avec le bot", () -> skipWithBot(600)),
                tool("+1 h avec le bot", () -> skipWithBot(3600)),
                tool("+6 h avec le bot", () -> skipWithBot(6 * 3600)),
                tool("Jusqu'au prochain Big Bang (12 h au plus)", this::botToNextBigBang),
                tool("Une décision", () -> {
                    BotBridge b = bot();
                    if (b != null) note(b.act() ? "Le bot a joué une décision." : "Le bot a échoué : il ne suit plus le jeu.");
                }));

        groupRight("Paliers",
                tool("Refaire l'acte 1", () -> game.state().clearMatter()),
                tool("Refaire l'acte 2", () -> {
                    game.state().clearMatter();
                    game.state().clearDarkMatter();
                }),
                tool("Refaire l'acte 3", () -> game.state().clearBigBangAct()),
                tool("Vider le collisionneur", () -> game.state().clearCollider()),
                tool("Univers à zéro", game::testResetUniverses));

        status.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffd9d4;");
        right.getChildren().add(status);
        right.getChildren().add(botNote);
        refreshBots();
        refreshDimensionBots();
        refresh();
    }

    /** Le bot de la politique choisie, créé à la demande ; {@code null} si les bots ne sont pas là. */
    private BotBridge bot() {
        String mode = BotBridge.modes().get(modeIndex);
        if (bot == null || !bot.mode().equals(mode)) bot = BotBridge.of(game, mode);
        if (bot == null) note("Les bots ne sont pas là : lancez le jeu avec le module « bots » (runTest l'ajoute).");
        return bot;
    }

    private void note(String text) {
        botNote.setText(text);
    }

    private void refreshBots() {
        if (!BotBridge.modes().get(modeIndex).equals(botMode.getValue())) botMode.setValue(BotBridge.modes().get(modeIndex));
        botLive.setText(live ? "En direct : en marche" : "En direct : coupé");
        if (!BotBridge.available()) note("Les bots ne sont pas là : lancez le jeu avec le module « bots » (runTest l'ajoute).");
    }

    /** Le dernier message des bots : pour les vérifications. */
    String botNote() {
        return botNote.getText();
    }

    /** La liste des politiques du bot : pour les vérifications. */
    javafx.scene.control.ComboBox<String> botChoice() {
        return botMode;
    }

    /** Vrai si le bot joue en direct : pour les vérifications. */
    boolean botLive() {
        return live;
    }

    /**
     * Le temps de jeu qui vient de passer (vitesse comprise) : en direct, le bot décide toutes les
     * {@link BotBridge#STEP} secondes de jeu, comme dans ses campagnes.
     */
    void frame(double gameSeconds) {
        if (!game.isStarted()) return;
        if (dimensionLiveOn) liveDimension(gameSeconds);
        if (!live) return;
        sinceDecision += gameSeconds;
        int decisions = 0;
        while (sinceDecision >= BotBridge.STEP && decisions < 50) {
            sinceDecision -= BotBridge.STEP;
            decisions++;
            BotBridge b = bot();
            if (b == null || !b.act()) {
                live = false;
                note("Le bot s'est arrêté : il ne suit plus le jeu.");
                refreshBots();
                return;
            }
        }
        if (sinceDecision > BotBridge.STEP) sinceDecision = 0;      // une image très longue ne fait pas une rafale
    }

    /** Vrai tant que le jeu est avancé par tranches ; GameApp suspend son tick ordinaire. */
    boolean isFastForwarding() { return fastForward != null && fastForward.running(); }

    /** Arrête l'avance rapide et rend immédiatement la main. */
    void cancelFastForward() {
        if (fastForward != null) fastForward.cancel();
        if (botTimeline != null) { botTimeline.stop(); botTimeline = null; }
        if (fastForward != null) note("Avance rapide interrompue après " +
                Format.duration(fastForward.advancedSeconds()) + ".");
        fastForward = null;
        botCancel.setDisable(true);
        dimensionCancel.setDisable(true);
        if (dimensionJob && dimensionBot != null) releaseDimension(dimensionBot);
        dimensionJob = false;
    }

    /** Lance une avance JavaFX non bloquante, avec budget de 10 pas par impulsion. */
    private void startFastForward(double seconds, boolean untilBang) {
        BotBridge b = bot();
        if (b == null || !game.isStarted()) return;
        if (isFastForwarding()) cancelFastForward();
        live = false; // ne pas superposer les décisions du mode direct
        refreshBots();
        int startingBang = game.bigBangs();
        int explosions = game.state().explosions();
        runJob(new BotFastForwardJob(seconds, BotBridge.STEP, game::tick,
                b::act, () -> untilBang && game.bigBangs() != startingBang), 10, "Bot", (state, elapsed) -> {
            if (state == BotFastForwardJob.End.BOT_ERROR) note("Bot arrêté : décision impossible.");
            else if (state == BotFastForwardJob.End.TARGET_REACHED)
                note("Big Bang atteint en " + Format.duration(elapsed) + ".");
            else note("Avance de " + Format.duration(elapsed) + " terminée : +" +
                    (game.state().explosions() - explosions) + " explosion(s), +" +
                    (game.bigBangs() - startingBang) + " Big Bang(s).");
        });
    }

    /**
     * Une avance rapide sur le fil JavaFX (version 0.140 : commune au bot de la partie et à ceux des dimensions) :
     * {@code batch} pas à chaque impulsion de 25 ms, annulable ; {@code done} dit comment elle a fini.
     */
    private void runJob(BotFastForwardJob job, int batch, String who,
                        java.util.function.BiConsumer<BotFastForwardJob.End, Double> done) {
        fastForward = job;
        botCancel.setDisable(false);
        dimensionCancel.setDisable(false);
        botTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(25), event -> {
            if (fastForward == null) return;
            BotFastForwardJob.End state;
            try { state = fastForward.runBatch(batch); }
            catch (RuntimeException error) {
                cancelFastForward();
                note("Erreur pendant l'avance du bot : " + error.getMessage());
                return;
            }
            if (state == BotFastForwardJob.End.RUNNING) {
                note(who + " : " + Format.duration(fastForward.advancedSeconds()) + " / " +
                        Format.duration(fastForward.totalSeconds()) + " — Annuler reste disponible.");
                return;
            }
            double elapsed = fastForward.advancedSeconds();
            botTimeline.stop();
            botTimeline = null;
            fastForward = null;
            botCancel.setDisable(true);
            dimensionCancel.setDisable(true);
            done.accept(state, elapsed);
            refresh();
        }));
        botTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        botTimeline.play();
    }

    // ------------------------------------------------------------------
    // Les dimensions et leurs bots (version 0.140)
    // ------------------------------------------------------------------

    /** Une rangée d'outils avec son nom devant. */
    private HBox row(String name, javafx.scene.Node... tools) {
        Label label = new Label(name);
        label.setMinWidth(130);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";");
        HBox row = new HBox(6, label);
        row.getChildren().addAll(tools);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Impose le temps qu'il fait sur la graine, pour sa durée entière ; sans la météo, le dit. */
    private void weather(Game.Weather weather) {
        if (game.isWeatherUnlocked()) game.testSetWeather(weather);
        else note("La météo vient avec le clicker : « Ouvrir » dans sa rangée.");
    }

    /** Ouvre la dimension {@code number} : l'univers formé, puis autant d'univers qu'il en faut. */
    private void openDimension(int number) {
        if (!game.isUniverseFormed()) game.testFormUniverse();
        for (int guard = 0; guard < Game.MAX_DIMENSIONS && game.dimensionsUnlocked() < number; guard++) {
            if (!game.testAddUniverse()) break;
        }
    }

    private static double dimensionThreshold(int number) {
        return switch (number) {
            case 1 -> Game.LINE_THRESHOLD;
            case 2 -> Game.PLANE_THRESHOLD;
            case 3 -> Game.VOLUME_THRESHOLD;
            case 4 -> Game.DURATION_THRESHOLD;
            case 5 -> Game.LOOP_THRESHOLD;
            case 6 -> Game.BRANE_THRESHOLD;
            case 7 -> Game.CLICKER_THRESHOLD;
            default -> Game.GRID_THRESHOLD;
        };
    }

    /** Des éclats de la dimension, gagnés : la ressource à dépenser, et autant vers le seuil. */
    private void addDimensionShards(int number, double amount) {
        GameState state = game.state();
        switch (number) {
            case 1 -> {
                state.setDimensionShards(state.dimensionShards() + amount);
                state.setDimensionShardsEarned(state.dimensionShardsEarned() + amount);
            }
            case 2 -> {
                state.setPlaneShards(state.planeShards() + amount);
                state.setPlaneShardsEarned(state.planeShardsEarned() + amount);
            }
            case 3 -> {
                state.setVolumeShards(state.volumeShards() + amount);
                state.setVolumeShardsEarned(state.volumeShardsEarned() + amount);
            }
            case 4 -> {
                state.setDurationShards(state.durationShards() + amount);
                state.setDurationShardsEarned(state.durationShardsEarned() + amount);
            }
            case 5 -> {
                state.setLoopShards(state.loopShards() + amount);
                state.setLoopShardsEarned(state.loopShardsEarned() + amount);
            }
            case 6 -> {
                state.setBraneShards(state.braneShards() + amount);
                state.setBraneShardsEarned(state.braneShardsEarned() + amount);
            }
            case 7 -> {
                state.setClickerShards(state.clickerShards() + amount);
                state.setClickerShardsEarned(state.clickerShardsEarned() + amount);
            }
            default -> {
                state.setGridShards(state.gridShards() + amount);
                state.setGridShardsEarned(state.gridShardsEarned() + amount);
            }
        }
    }

    /** Le seuil atteint : les éclats gagnés qu'il faut, sans rien ajouter à la ressource. */
    private void reachDimensionThreshold(int number) {
        GameState state = game.state();
        double threshold = dimensionThreshold(number);
        switch (number) {
            case 1 -> state.setDimensionShardsEarned(Math.max(state.dimensionShardsEarned(), threshold));
            case 2 -> state.setPlaneShardsEarned(Math.max(state.planeShardsEarned(), threshold));
            case 3 -> state.setVolumeShardsEarned(Math.max(state.volumeShardsEarned(), threshold));
            case 4 -> state.setDurationShardsEarned(Math.max(state.durationShardsEarned(), threshold));
            case 5 -> state.setLoopShardsEarned(Math.max(state.loopShardsEarned(), threshold));
            case 6 -> state.setBraneShardsEarned(Math.max(state.braneShardsEarned(), threshold));
            case 7 -> state.setClickerShardsEarned(Math.max(state.clickerShardsEarned(), threshold));
            default -> state.setGridShardsEarned(Math.max(state.gridShardsEarned(), threshold));
        }
    }

    /**
     * Les améliorations de la dimension au plus haut, ou à zéro. La valeur du volume et de la durée, et le gain de la
     * boucle, de la brane, du clicker et de la grille, n'ont pas de plafond : dix niveaux.
     */
    private void setDimensionLevels(int number, boolean max) {
        GameState state = game.state();
        switch (number) {
            case 1 -> {
                state.setLineYieldLevel(max ? game.lineMaxLevel(Game.LineUpgrade.YIELD) : 0);
                state.setLineTicksLevel(max ? game.lineMaxLevel(Game.LineUpgrade.TICKS) : 0);
                state.setLineSpeedLevel(max ? game.lineMaxLevel(Game.LineUpgrade.SPEED) : 0);
                state.setLineBallsLevel(max ? game.lineMaxLevel(Game.LineUpgrade.BALLS) : 0);
                state.setLineCountLevel(max ? game.lineMaxLevel(Game.LineUpgrade.LINES) : 0);
            }
            case 2 -> {
                for (Game.PlaneUpgrade upgrade : Game.PlaneUpgrade.values())
                    state.setPlaneLevel(upgrade.ordinal(), max ? upgrade.max() : 0);
            }
            case 3 -> {
                for (Game.VolumeUpgrade upgrade : Game.VolumeUpgrade.values())
                    state.setVolumeLevel(upgrade.ordinal(), !max ? 0 : upgrade == Game.VolumeUpgrade.VALUE ? 10 : upgrade.max());
            }
            case 4 -> {
                for (Game.DurationUpgrade upgrade : Game.DurationUpgrade.values())
                    state.setDurationLevel(upgrade.ordinal(), !max ? 0 : upgrade == Game.DurationUpgrade.VALUE ? 10 : upgrade.max());
            }
            case 5 -> {
                for (Game.LoopUpgrade upgrade : Game.LoopUpgrade.values())
                    state.setLoopLevel(upgrade.ordinal(), !max ? 0 : upgrade == Game.LoopUpgrade.GAIN ? 10 : upgrade.max());
            }
            case 6 -> {
                for (Game.BraneUpgrade upgrade : Game.BraneUpgrade.values())
                    state.setBraneLevel(upgrade.ordinal(), !max ? 0 : upgrade == Game.BraneUpgrade.GAIN ? 10 : upgrade.max());
            }
            case 7 -> {
                for (Game.ClickerUpgrade upgrade : Game.ClickerUpgrade.values())
                    state.setClickerLevel(upgrade.ordinal(), !max ? 0 : upgrade == Game.ClickerUpgrade.GAIN ? 10 : upgrade.max());
            }
            default -> {
                for (Game.GridUpgrade upgrade : Game.GridUpgrade.values())
                    state.setGridLevel(upgrade.ordinal(), !max ? 0 : upgrade == Game.GridUpgrade.GAIN ? 10 : upgrade.max());
            }
        }
    }

    /**
     * Les améliorations de la graine au plus haut (avec l'arrosoir, que demande son débit), ou à zéro avec le chrono
     * vide et réglé à ×1.
     */
    private void setSeedUpgrades(boolean max) {
        for (Game.SeedUpgrade upgrade : Game.SeedUpgrade.values())
            game.state().setSeedUpgradeLevel(upgrade.ordinal(), max ? upgrade.max() : 0);
        if (max) game.state().setWateringCan(true);
        else {
            game.state().setChronoStored(0);
            game.setChronoSpeed(1);
        }
    }

    /** Le bot de la dimension choisie, créé à la demande ; {@code null} si les bots ne sont pas là. */
    private DimensionBotBridge dimensionBot() {
        int number = Math.max(1, DimensionBotBridge.LABELS.indexOf(dimensionChoice.getValue()) + 1);
        if (dimensionBot == null || dimensionBot.number() != number) dimensionBot = DimensionBotBridge.of(game, number);
        if (dimensionBot == null) note("Les bots ne sont pas là : lancez le jeu avec le module « bots » (runTest l'ajoute).");
        return dimensionBot;
    }

    /** Vrai si la dimension du bot est ouverte ; sinon la barre le dit. */
    private boolean dimensionOpen(DimensionBotBridge b) {
        if (game.dimensionsUnlocked() >= b.number()) return true;
        note("La dimension " + b.number() + " n'est pas ouverte : « Ouvrir » dans Dimensions, ou +1 univers.");
        return false;
    }

    private void refreshDimensionBots() {
        dimensionLive.setText(dimensionLiveOn ? "En direct : en marche" : "En direct : coupé");
    }

    private void toggleDimensionLive() {
        if (!game.isStarted()) return;
        if (dimensionLiveOn) stopDimensionLive();
        else {
            DimensionBotBridge b = dimensionBot();
            dimensionLiveOn = b != null && dimensionOpen(b);
            if (dimensionLiveOn) note("Le bot " + b.of() + " joue en direct : ouvrez sa fenêtre pour le voir.");
        }
        refreshDimensionBots();
    }

    /** Coupe le bot de dimension en direct, et relâche ce qu'il tenait. */
    private void stopDimensionLive() {
        if (dimensionLiveOn && dimensionBot != null) releaseDimension(dimensionBot);
        dimensionLiveOn = false;
        refreshDimensionBots();
    }

    /** En direct : le bot décide avant chaque image, sur le temps de jeu qui vient de passer. */
    private void liveDimension(double gameSeconds) {
        DimensionBotBridge b = dimensionBot();
        if (b == null || !b.decide(gameSeconds)) {
            stopDimensionLive();
            note("Le bot de dimension s'est arrêté : sa dimension n'est plus ouverte.");
        }
    }

    /** Le bot s'arrête : le point du plan s'immobilise, le bouton de la durée se relâche (il donne ce qu'il avait). */
    private void releaseDimension(DimensionBotBridge b) {
        if (b.number() == 2) game.setPlaneInput(0, 0);
        if (b.number() == 4 && game.isDurationHeld()) game.releaseDuration();
    }

    /** Le bot de la dimension choisie joue {@code seconds} secondes, ou jusqu'au rond vert, en avance rapide. */
    private void dimensionSkip(double seconds, boolean untilGreen) {
        DimensionBotBridge b = dimensionBot();
        if (b == null || !dimensionOpen(b)) return;
        if (untilGreen && b.reached()) {
            note("Le rond vert " + b.of() + " est déjà là.");
            return;
        }
        if (isFastForwarding()) cancelFastForward();
        stopDimensionLive();
        live = false;
        refreshBots();
        int bought = b.purchases();
        dimensionJob = true;
        runJob(new BotFastForwardJob(seconds, DimensionBotBridge.STEP, game::tick,
                () -> b.decide(DimensionBotBridge.STEP), () -> untilGreen && b.reached()),
                DIMENSION_BATCH, "Bot " + b.of(), (state, elapsed) -> {
            dimensionJob = false;
            releaseDimension(b);
            int count = b.purchases() - bought;
            if (state == BotFastForwardJob.End.BOT_ERROR) note("Bot " + b.of() + " arrêté : la dimension s'est fermée.");
            else if (state == BotFastForwardJob.End.TARGET_REACHED)
                note("Rond vert " + b.of() + " en " + Format.duration(elapsed) + " (" + count + " achats).");
            else note("Bot " + b.of() + " : " + Format.duration(elapsed) + " joués, " + count + " achats"
                    + (b.reached() ? ", rond vert là." : "."));
        });
    }

    /** Pour les vérifications : le choix de la dimension, le bouton du direct, et si le bot joue en direct. */
    javafx.scene.control.ComboBox<String> dimensionBotChoice() {
        return dimensionChoice;
    }

    Button dimensionLiveButton() {
        return dimensionLive;
    }

    boolean dimensionBotLive() {
        return dimensionLiveOn;
    }

    /** Fait avancer la partie sans bloquer l'interface JavaFX. */
    private void skipWithBot(double seconds) { startFastForward(seconds, false); }

    /** S'arrête au prochain Big Bang, ou après 12 heures simulées au maximum. */
    private void botToNextBigBang() { startFastForward(12 * 3600, true); }

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

    /** Un groupe d'outils lié à un onglet du jeu : il s'ouvre quand on va sur cet onglet. */
    private void tabGroup(String tab, String name, javafx.scene.Node... tools) {
        tabGroups.put(tab, group(name, tools));
    }

    /** Un groupe placé à droite, avec ce qui sert partout. */
    private void groupRight(String name, javafx.scene.Node... tools) {
        Button toggle = group(name, tools);
        header.getChildren().remove(toggle);
        right.getChildren().add(toggle);
    }

    /**
     * L'onglet affiché a changé (version 0.122) : la barre ouvre le groupe de cet onglet, sauf si l'on a ouvert à la main
     * un groupe de droite (le temps, les bots, les paliers) ou celui des dimensions.
     */
    void followTab(String tab) {
        Button group = tabGroups.get(tab);
        if (group == null || group == open) return;
        if (open == null || tabGroups.containsValue(open)) open(group);
    }

    /** Le groupe ouvert : pour les vérifications. */
    String openGroup() {
        return open == null ? "" : open.getText();
    }

    /** La graine passe au palier suivant : les arrosages qu'il faut. */
    private void nextSeedStage() {
        int stage = game.seedStage();
        if (stage >= Game.SEED_STAGES) return;
        if (!game.isUniverseFormed()) game.testFormUniverse();
        game.state().setSeedSprouted(true);
        // Version 0.139 : seuls les arrosages comptent, les seuils des dimensions ne retiennent plus la graine.
        game.state().setSeedWaterings(Math.max(game.state().seedWaterings(), Game.seedStageWaterings(stage + 1)));
    }

    /** Ajoute un groupe : son bouton sur la première ligne, et ses outils sur une ligne repliée. */
    private Button group(String name, javafx.scene.Node... tools) {
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
        return toggle;
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
        java.util.Set<String> forks = new java.util.HashSet<>();
        for (DarkUpgrade upgrade : game.darkUpgrades()) {
            // Une bifurcation : la première voie seulement, comme un joueur qui en choisit une.
            if (upgrade.isFork() && !forks.add(upgrade.fork())) continue;
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
        bangsAtLeast(1);
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
            bangsAtLeast(1);
            // Une matière exotique attend le huitième Big Bang : l'outil y va d'office.
            if (body.tier() == Body.Tier.EXOTIC && !game.isExoticUnlocked()) bangsAtLeast(8);
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) state.addSpaceUpgrade(upgrade.id());
            for (String assembly : body.assemblies()) {
                if (!game.hasAssembly(assembly)) form(game.assembly(assembly));
            }
            // Les besoins allégés par le vide (version 0.129) : ce que l'astre demande vraiment.
            for (java.util.Map.Entry<String, Integer> molecule : game.bodyMolecules(body).entrySet()) {
                int missing = molecule.getValue() - game.moleculeCount(molecule.getKey());
                if (missing > 0) state.setMoleculeCount(molecule.getKey(), game.moleculeCount(molecule.getKey()) + missing);
                state.addSubstance(molecule.getKey());
            }
            for (java.util.Map.Entry<Molecule.State, Integer> matter : game.bodyMatter(body).entrySet()) {
                int missing = (int) Math.max(0, matter.getValue() - game.gatheredInState(matter.getKey()));
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

    /** Au moins ce nombre de Big Bangs, et tous les paliers ouverts payés d'office (version 0.124 : un palier ne compte qu'une fois payé). */
    private void bangsAtLeast(int bigBangs) {
        GameState state = game.state();
        if (state.bigBangs() < bigBangs) state.setBigBangs(bigBangs);
        for (idle.core.bigbang.BigBangMilestone milestone : game.bigBangMilestones()) {
            if (game.isBigBangMilestoneOpen(milestone)) state.addBoughtMilestone(milestone.name());
        }
    }

    /** Range d'office les étoiles et les planètes libres en systèmes solaires, jusqu'à ce que la galaxie en ait assez. */
    private void threeSystems() {
        bangsAtLeast(6);
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

    /**
     * Réunit les conditions du Big Bang (cinq, six avec le ciel), sans rien retirer : ce qu'il manque de particules,
     * d'atomes et de matière noire, les succès et les défis. Un défi en cours est arrêté, sans
     * être compté comme réussi.
     */
    private void readyForBigBang() {
        GameState state = game.state();
        if (state.explosions() == 0) state.setExplosions(1);
        state.setActiveChallenge(null);
        state.setParticles(state.particles().max(game.bigBangParticles()));
        state.setAtoms(state.atoms().max(game.bigBangAtoms()));
        state.setTotalAtoms(state.totalAtoms().max(state.atoms()));
        state.setDarkMatter(state.darkMatter().max(game.bigBangDarkMatter()));
        grantAchievements();
        for (Challenge challenge : game.challenges()) state.addCompletedChallenge(challenge.id());
    }
}

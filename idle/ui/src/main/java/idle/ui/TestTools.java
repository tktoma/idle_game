package idle.ui;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.ui.particles.SpeedStages;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Le catalogue des outils du profil de test : chacun a sa section, son nom et son action. Le panneau ({@link TestPanel})
 * les montre et les cherche ; les essais les lancent tous, sans JavaFX, sur une partie neuve.
 *
 * <p>Une section par onglet du jeu (elle passe en tête quand on va sur cet onglet), puis ce qui sert partout : le temps
 * et l'avance rapide, les bots, les sons, les animations, les paliers et leurs remises à zéro, les dimensions.
 */
final class TestTools {

    /** Les sections, dans l'ordre du panneau ; {@code tab} est le nom de l'onglet du jeu qu'elles suivent, s'il y en a un. */
    enum Section {
        TIME("Temps et avance rapide", null),
        BOTS("Bots", null),
        PARTICLES("Particules", "PARTICLES"),
        ATOMS("Atomes et tableau", "ATOMS"),
        AUTOMATION("Automatisation", "AUTOMATION"),
        DARK_MATTER("Matière noire", "DARK_MATTER"),
        COLLIDER("Collisionneur", "COLLIDER"),
        HADRONS("Hadrons", "HADRONS"),
        BIG_BANG("Big Bang et ciel", "BIG_BANG"),
        UNIVERSE("Univers et graine", "UNIVERSE"),
        DIMENSIONS("Dimensions", null),
        VOID("Le vide", "VOID"),
        ACHIEVEMENTS("Succès", "ACHIEVEMENTS"),
        SKINS("Skins, thèmes et notations", "SKINS"),
        SOUNDS("Sons et fonds", null),
        ANIMATIONS("Animations", null),
        RESETS("Paliers et remises à zéro", null);

        final String title;
        final String tab;

        Section(String title, String tab) {
            this.title = title;
            this.tab = tab;
        }

        /** La section qui suit cet onglet du jeu, ou {@code null}. */
        static Section ofTab(String tab) {
            for (Section section : values()) if (tab != null && tab.equals(section.tab)) return section;
            return null;
        }

        /** Vrai si ses outils ne demandent pas JavaFX (l'avance rapide et les bots tournent sur son fil). */
        boolean headless() { return this != TIME && this != BOTS; }
    }

    /** Un outil : sa section, son nom, son action. */
    record Tool(Section section, String label, Runnable action) {
        /** Vrai si ce texte cherché (sans accents ni majuscules) est dans son nom ou celui de sa section. */
        boolean matches(String search) {
            String wanted = plain(search);
            return wanted.isBlank() || plain(label).contains(wanted) || plain(section.title).contains(wanted);
        }
    }

    /** Un fond sonore qu'on peut faire jouer partout, pour l'écouter. */
    enum TestHum {
        DARK_MATTER("Grondement de la matière noire"),
        COLLIDER("Fond du collisionneur"),
        EXPANSION("Fond de l'expansion de la matière"),
        BRANE("Corde de la brane"),
        DURATION("Appui de la durée"),
        CHRONO("Tic-tac du chrono");

        final String label;

        TestHum(String label) { this.label = label; }
    }

    /**
     * Ce que l'application fait pour les outils qui touchent à l'affichage ou au son. Par défaut rien : les essais s'en
     * passent. Les animations rendent faux quand elles ne peuvent pas se jouer (le skin classique n'en a pas, ou il
     * manque un univers formé).
     */
    interface Hooks {
        default void explosion() { }

        default boolean bigBang() { return false; }

        default boolean birth() { return false; }

        default boolean launch() { return false; }

        default boolean curtain() { return false; }

        default boolean finale() { return false; }

        default void fusion() { }

        default void newAtom() { }

        default void sound(Sound.Cue cue) { }

        default void particle(int stage) { }

        default void gesture(int dimension) { }

        default void gain(int dimension) { }

        /** Une note du piano du miroir (0 à 15), ou la faute (négatif). */
        default void piano(int note) { }

        /** Fait jouer ce fond partout, ou le rend à sa vue. @return vrai s'il joue maintenant partout */
        default boolean toggleHum(TestHum hum) { return false; }

        default void unlockCosmetics() { }

        default void lockCosmetics() { }

        default void toggleCosmetic(Cosmetic cosmetic) { }

        /** Une partie neuve, comme « Nouvelle partie » des réglages, sans confirmation. */
        default void newGame() { }
    }

    private final List<Tool> tools = new ArrayList<>();
    private final Game game;
    private final TestActions act;
    private final TestBots bots;
    private final Hooks hooks;
    private final Consumer<String> note;

    private TestTools(Game game, TestActions act, TestBots bots, Hooks hooks, Consumer<String> note) {
        this.game = game;
        this.act = act;
        this.bots = bots;
        this.hooks = hooks;
        this.note = note;
    }

    /** Tous les outils, section par section. */
    static List<Tool> build(Game game, TestActions act, TestBots bots, Hooks hooks, Consumer<String> note) {
        TestTools catalog = new TestTools(game, act, bots, hooks, note);
        catalog.time();
        catalog.bots();
        catalog.matter();
        catalog.darkMatter();
        catalog.collider();
        catalog.bigBang();
        catalog.universe();
        catalog.dimensions();
        catalog.voidAndMore();
        catalog.sounds();
        catalog.animations();
        catalog.resets();
        return List.copyOf(catalog.tools);
    }

    private void add(Section section, String label, Runnable action) { tools.add(new Tool(section, label, action)); }

    /** Le nom d'une vitesse du jeu : « Figé », « ×0,25 », « ×10 ». */
    static String speedName(double factor) {
        if (factor == 0) return "Figé";
        return "×" + (factor == Math.rint(factor) ? String.valueOf((long) factor) : String.valueOf(factor).replace('.', ','));
    }

    /** Le texte sans accents ni majuscules, pour chercher. */
    static String plain(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------

    private void time() {
        add(Section.TIME, "+10 s", () -> act.skip(10));
        add(Section.TIME, "+1 min", () -> bots.advance(60));
        add(Section.TIME, "+10 min", () -> bots.advance(600));
        add(Section.TIME, "+1 h", () -> bots.advance(3600));
        add(Section.TIME, "+6 h", () -> bots.advance(6 * 3600));
        add(Section.TIME, "+24 h", () -> bots.advance(24 * 3600));
        add(Section.TIME, "Annuler l'avance rapide", () -> bots.cancel());
    }

    private void bots() {
        add(Section.BOTS, "Bot en direct (marche / arrêt)", () -> bots.toggleLive());
        add(Section.BOTS, "Une décision du bot", () -> bots.decideOnce());
        add(Section.BOTS, "+10 min avec le bot", () -> bots.fastForward(600, false));
        add(Section.BOTS, "+1 h avec le bot", () -> bots.fastForward(3600, false));
        add(Section.BOTS, "+6 h avec le bot", () -> bots.fastForward(6 * 3600, false));
        add(Section.BOTS, "Jusqu'au prochain Big Bang (12 h au plus)", () -> bots.fastForward(12 * 3600, true));
        add(Section.BOTS, "Annuler l'avance rapide", () -> bots.cancel());
    }

    private void matter() {
        add(Section.PARTICLES, "+1 000 particules", () -> act.addParticles(BigNum.of(1000)));
        add(Section.PARTICLES, "Particules ×10", () -> act.multiplyParticles(BigNum.of(10)));
        add(Section.PARTICLES, "Particules ×1e10", () -> act.multiplyParticles(BigNum.of(1, 10)));
        add(Section.PARTICLES, "+1 générateur", () -> act.addGenerators(1));
        add(Section.PARTICLES, "Tous les générateurs", () -> act.addGenerators(Integer.MAX_VALUE));
        add(Section.PARTICLES, "Régime prêt", act::regimeReady);
        add(Section.ATOMS, "+1 atome", () -> act.addAtoms(BigNum.ONE));
        add(Section.ATOMS, "+10 atomes", () -> act.addAtoms(BigNum.of(10)));
        add(Section.ATOMS, "Atomes au plafond", () -> act.addAtoms(game.atomCap()));
        add(Section.ATOMS, "30 atomes créés (tableau ouvert)", act::unlockTable);
        add(Section.ATOMS, "Améliorations d'atomes au maximum", act::maxAtomUpgrades);
        add(Section.ATOMS, "Un élément unique", () -> act.give(2, 1));
        add(Section.ATOMS, "1 de chaque élément", () -> act.fillTable(false));
        add(Section.ATOMS, "Tableau complet", () -> act.fillTable(true));
        add(Section.ATOMS, "Vider le tableau", act::emptyTable);
        add(Section.ATOMS, "+1 élément gratuit", act::freeElement);
        add(Section.AUTOMATION, "Automatismes au maximum", act::maxAutomations);
        add(Section.AUTOMATION, "Automatismes coupés", act::noAutomations);
    }

    private void darkMatter() {
        add(Section.DARK_MATTER, "Exploser maintenant", act::explodeNow);
        add(Section.DARK_MATTER, "+1 matière noire", () -> act.addDarkMatter(1));
        add(Section.DARK_MATTER, "+10 matières noires", () -> act.addDarkMatter(10));
        add(Section.DARK_MATTER, "Taille ×1 000", () -> act.growDarkMatter(3));
        add(Section.DARK_MATTER, "Taille ×1e10", () -> act.growDarkMatter(10));
        add(Section.DARK_MATTER, "Arbre complet", act::fillTree);
        add(Section.DARK_MATTER, "Arbre vide", act::emptyTree);
        add(Section.DARK_MATTER, "Défi suivant réussi", act::completeNextChallenge);
        add(Section.DARK_MATTER, "Les dix défis réussis", act::completeAllChallenges);
        add(Section.DARK_MATTER, "Fuite réussie", act::leakCompleted);
        add(Section.DARK_MATTER, "Calme du Big Bang complet", act::bangCalmFull);
        add(Section.DARK_MATTER, "Tableau plus lourd", act::heavierTable);
        add(Section.DARK_MATTER, "Tableau à sa masse du début", act::tableAtStart);
    }

    private void collider() {
        add(Section.COLLIDER, "Énergie ×1 000", act::colliderEnergy);
        add(Section.COLLIDER, "Les 8 générateurs", act::colliderGenerators);
        add(Section.COLLIDER, "Collisions et saveurs", act::collisionsAndFlavors);
        add(Section.COLLIDER, "Toutes les améliorations", () -> act.colliderUpgrades(true));
        add(Section.COLLIDER, "Améliorations à zéro", () -> act.colliderUpgrades(false));
        add(Section.COLLIDER, "Faisceaux : vitesse suivante", act::nextBeamSpeed);
        add(Section.COLLIDER, "+100 quarks de chaque", () -> act.addQuarks(100));
        add(Section.COLLIDER, "Un quark top", act::topQuark);
        add(Section.COLLIDER, "Énergie des trous noirs", act::blackHoleEnergy);
        add(Section.COLLIDER, "Ouvrir le vide", act::openVoid);
        add(Section.HADRONS, "Ouvrir les hadrons", act::openHadrons);
        add(Section.HADRONS, "+100 de chaque hadron", () -> act.addHadrons(100));
        add(Section.HADRONS, "+10 000 de chaque hadron", () -> act.addHadrons(10_000));
    }

    private void bigBang() {
        add(Section.BIG_BANG, "Conditions réunies", act::readyForBigBang);
        add(Section.BIG_BANG, "Big Bang maintenant", act::bigBangNow);
        add(Section.BIG_BANG, "Paliers ouverts payés", () -> act.bangsAtLeast(0));
        add(Section.BIG_BANG, "Une molécule de chaque", act::oneOfEachMolecule);
        add(Section.BIG_BANG, "Aucune molécule", act::noMolecules);
        add(Section.BIG_BANG, "+1 000 d'espace", () -> act.addSpace(BigNum.of(1000)));
        add(Section.BIG_BANG, "Espace ×10", act::multiplySpace);
        add(Section.BIG_BANG, "Améliorations d'espace", act::spaceUpgrades);
        add(Section.BIG_BANG, "Trois de chaque petite molécule", act::threeOfEachSmallMolecule);
        add(Section.BIG_BANG, "Tout rassembler", act::gatherAll);
        add(Section.BIG_BANG, "Assemblage suivant", () -> { if (!act.formNextAssembly()) note.accept("Tous les assemblages sont formés."); });
        add(Section.BIG_BANG, "Tout assembler", act::formAllAssemblies);
        add(Section.BIG_BANG, "Astre suivant", () -> { if (!act.formNextBody()) note.accept("Tous les astres sont formés."); });
        add(Section.BIG_BANG, "Tous les astres", act::formAllBodies);
        add(Section.BIG_BANG, "Former la galaxie", act::formGalaxy);
        add(Section.BIG_BANG, "Échelle suivante du cosmos", act::nextCosmos);
        add(Section.BIG_BANG, "Défi de Big Bang suivant réussi", act::nextBangChallenge);
        add(Section.BIG_BANG, "Tous les défis de Big Bang", act::allBangChallenges);
    }

    private void universe() {
        add(Section.UNIVERSE, "Former l'univers", game::testFormUniverse);
        add(Section.UNIVERSE, "Rouvrir les paliers (univers suivant)", game::startNextUniverse);
        add(Section.UNIVERSE, "+1 univers", game::testAddUniverse);
        add(Section.UNIVERSE, "−1 univers", game::testRemoveUniverse);
        add(Section.UNIVERSE, "+10 graines d'univers", () -> game.testAddUniverseSeeds(BigNum.of(10)));
        add(Section.UNIVERSE, "Palier suivant de la graine", act::nextSeedStage);
        add(Section.UNIVERSE, "+1 arrosage", act::water);
        add(Section.UNIVERSE, "Reprendre l'arrosoir", act::takeWateringCan);
        add(Section.UNIVERSE, "Améliorations de la graine au maximum", () -> act.seedUpgrades(true));
        add(Section.UNIVERSE, "Améliorations de la graine à zéro", () -> act.seedUpgrades(false));
        add(Section.UNIVERSE, "Chrono plein", act::fullChrono);
        add(Section.UNIVERSE, "Chrono vide", () -> game.state().setChronoStored(0));
        add(Section.UNIVERSE, "Rond de croissance", () -> {
            if (game.isBloomUnlocked()) game.testShowBloom();
            else note.accept("Le rond de croissance vient avec la boucle (dimension 5).");
        });
        add(Section.UNIVERSE, "Beau temps", () -> { if (!act.weather(Game.Weather.SUN)) note.accept("La météo vient avec le clicker (dimension 7)."); });
        add(Section.UNIVERSE, "Orage", () -> { if (!act.weather(Game.Weather.STORM)) note.accept("La météo vient avec le clicker (dimension 7)."); });
        add(Section.UNIVERSE, "+1 000 d'énergie du vide accumulée", () -> act.addVoidHistory(1000));
    }

    private void dimensions() {
        add(Section.DIMENSIONS, "Ouvrir les dix dimensions", () -> act.openDimension(Game.MAX_DIMENSIONS));
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) {
            int number = d;
            String name = TestActions.DIMENSIONS[d - 1];
            add(Section.DIMENSIONS, name + " : ouvrir", () -> act.openDimension(number));
            add(Section.DIMENSIONS, name + " : seuil atteint", () -> act.reachDimensionThreshold(number));
        }
        add(Section.DIMENSIONS, "Toutes : améliorations au maximum", () -> { for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) act.dimensionLevels(d, true); });
        add(Section.DIMENSIONS, "Ouvrir le défi final", act::openFinalChallenge);
        add(Section.DIMENSIONS, "Défi final : tous les paliers", act::reachAllFinalTiers);
    }

    private void voidAndMore() {
        add(Section.VOID, "+10 énergie du vide", () -> act.addVoidEnergy(BigNum.of(10)));
        add(Section.VOID, "+1 000 énergie du vide", () -> act.addVoidEnergy(BigNum.of(1000)));
        add(Section.VOID, "+1 million d'énergie du vide", () -> act.addVoidEnergy(BigNum.of(1e6)));
        add(Section.VOID, "Ouvrir le trou noir", game::testOpenVoidHole);
        add(Section.VOID, "Masse au maximum", act::voidMass);
        add(Section.VOID, "Améliorations au maximum", () -> act.voidUpgrades(true));
        add(Section.VOID, "Améliorations à zéro", () -> act.voidUpgrades(false));
        add(Section.ACHIEVEMENTS, "Tous les succès", act::grantAchievements);
        add(Section.SKINS, "+5 fusions", () -> game.testAddFusions(Game.SKINS_FUSIONS));
        add(Section.SKINS, "Tous les skins", () -> game.testAllSkins(true));
        add(Section.SKINS, "Aucun skin", () -> game.testAllSkins(false));
        add(Section.SKINS, "Tous les thèmes et notations", hooks::unlockCosmetics);
        add(Section.SKINS, "Aucun thème ni notation", hooks::lockCosmetics);
        for (Cosmetic cosmetic : Cosmetic.values()) add(Section.SKINS, "± " + cosmetic.label(), () -> hooks.toggleCosmetic(cosmetic));
    }

    /** Le nom d'un son, pour son bouton. */
    static String cueName(Sound.Cue cue) {
        return switch (cue) {
            case BIG_BANG -> "Big Bang";
            case UNIVERSE -> "Univers formé";
            case FINALE -> "Palier du défi final";
            case EXPLOSION -> "Explosion du tableau";
            case VICTORY -> "Défi réussi, record";
            case ACHIEVEMENT -> "Succès";
            case SKIN -> "Skin débloqué";
            case CHALLENGE -> "Défi qui s'ouvre";
            case DARK -> "Matière noire (annonce)";
            case COMET -> "Comète, quark top, sondes";
            case ELEMENT -> "Nouvel élément";
            case MILESTONE -> "Jalon";
            case LEVEL -> "Palier de vitesse";
            case PURCHASE -> "Achat";
            case TOGGLE -> "Interrupteur";
            case PARTICLE -> "Particule (banque de sons)";
            case ATOM -> "Fusion des générateurs";
            case COLLISION -> "Collision";
            case HADRON -> "Hadrons";
            case VOID -> "Paire du vide";
            case NEW_ATOM -> "Atome créé";
            case PROBE -> "Sonde lancée";
            case MIRROR -> "Note du miroir";
            case SEED -> "Palier de la graine";
            case WATER -> "Arrosage";
            case CLICK -> "Clic";
            case TAB -> "Changement d'onglet";
            case BEAT -> "Tic du métronome";
        };
    }

    private void sounds() {
        for (Sound.Cue cue : Sound.Cue.values()) add(Section.SOUNDS, "Son : " + cueName(cue), () -> hooks.sound(cue));
        for (int stage = 0; stage < SpeedStages.COUNT; stage++) {
            int each = stage;
            add(Section.SOUNDS, "Particule, palier " + (stage + 1), () -> hooks.particle(each));
        }
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) {
            int number = d;
            String name = TestActions.DIMENSIONS[d - 1];
            add(Section.SOUNDS, "Geste : " + name, () -> hooks.gesture(number));
            add(Section.SOUNDS, "Points : " + name, () -> hooks.gain(number));
        }
        for (int note = 0; note < Sound.PIANO_NOTES; note++) {
            int each = note;
            add(Section.SOUNDS, "Piano du miroir, note " + (note + 1), () -> hooks.piano(each));
        }
        add(Section.SOUNDS, "Piano du miroir : une faute", () -> hooks.piano(SoundEvents.WRONG));
        for (TestHum hum : TestHum.values()) {
            add(Section.SOUNDS, hum.label + " partout (marche / arrêt)",
                    () -> note.accept(hum.label + (hooks.toggleHum(hum) ? " : joue partout." : " : seulement sur sa vue.")));
        }
    }

    private void animations() {
        add(Section.ANIMATIONS, "Fusion des générateurs", hooks::fusion);
        add(Section.ANIMATIONS, "Nouvel atome", hooks::newAtom);
        add(Section.ANIMATIONS, "Explosion (skin choisi)", hooks::explosion);
        add(Section.ANIMATIONS, "Big Bang (skin choisi)", () -> {
            if (!hooks.bigBang()) note.accept("Le Big Bang classique n'a pas d'animation : choisissez un skin du Big Bang.");
        });
        add(Section.ANIMATIONS, "Naissance d'un univers", () -> { if (!hooks.birth()) note.accept("Il faut un univers formé."); });
        add(Section.ANIMATIONS, "Lancement d'un univers", () -> { if (!hooks.launch()) note.accept("Il faut un univers formé."); });
        add(Section.ANIMATIONS, "Transition (skin choisi)", () -> {
            if (!hooks.curtain()) note.accept("La transition classique n'a pas d'effet : choisissez un skin de la transition.");
        });
        add(Section.ANIMATIONS, "Jouer la fin", () -> { if (!hooks.finale()) note.accept("La fin se joue sur la carte des dimensions : ouvrez une dimension."); });
    }

    private void resets() {
        add(Section.RESETS, "Refaire l'acte 1 (matière)", act::resetAct1);
        add(Section.RESETS, "Refaire l'acte 2 (matière noire)", act::resetAct2);
        add(Section.RESETS, "Refaire l'acte 3 (Big Bang)", act::resetAct3);
        add(Section.RESETS, "Collisionneur à zéro", act::resetCollider);
        add(Section.RESETS, "Graine à zéro", act::resetSeed);
        add(Section.RESETS, "Vide à zéro", act::resetVoid);
        add(Section.RESETS, "Bulles de l'arbre à zéro", act::resetTreeBubbles);
        add(Section.RESETS, "Défi final refermé", act::closeFinalChallenge);
        for (int d = 1; d <= Game.MAX_DIMENSIONS; d++) {
            int number = d;
            add(Section.RESETS, TestActions.DIMENSIONS[d - 1] + " à zéro", () -> act.resetDimension(number));
        }
        add(Section.RESETS, "Mélange : retour à l'étape 1", act::mixStageOne);
        add(Section.RESETS, "Toutes les dimensions à zéro", act::resetAllDimensions);
        add(Section.RESETS, "Partie secondaire à zéro (un seul univers)", act::resetUniverses);
        add(Section.RESETS, "Nouvelle partie (tout)", hooks::newGame);
    }
}

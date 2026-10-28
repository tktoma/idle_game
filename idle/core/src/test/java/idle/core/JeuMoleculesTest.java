package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.elements.Element;
import idle.core.elements.ElementCategory;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.Molecules;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : les molécules.
 */
class JeuMoleculesTest extends GameTestSupport {


    @Nested
    class LesMolecules {

        private static final int H = 1;
        private static final int C = 6;
        private static final int O = 8;
        private static final String WATER = "H2O";
        private static final String SALT = "NaCl";
        private static final String GLUCOSE = "C6H12O6";

        /**
         * Le vrai jeu, après un premier Big Bang, tableau plein, de l'espace à revendre, et toutes les
         * améliorations d'espace déjà là (rayons ouverts, fusion ouverte) sans qu'elles aient rien coûté.
         */
        private Game gameAfterBigBang() {
            Game game = bareGameAfterBigBang();
            GameTestSupport.openSpace(game);
            return game;
        }

        /** Le même, sans aucune amélioration d'espace : seules les petites molécules sont ouvertes. */
        private Game bareGameAfterBigBang() {
            Game game = GameTestSupport.realGame();
            GameTestSupport.bangs(game, 1);
            game.state().setSpace(BigNum.of(1, 9));
            refill(game);
            return game;
        }

        /** Remet chaque élément à son nombre maximal d'exemplaires. */
        private void refill(Game game) {
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), game.maxCopiesOf(element.category()));
            }
        }

        @Test
        void uneFormuleSeLit() {
            Molecule water = Molecule.of(Molecule.Kind.SIMPLE, "Eau", "H2O");
            assertEquals("H2O", water.id());
            assertEquals("H₂O", water.formula());
            assertEquals(Map.of(H, 2, O, 1), water.recipe());
            assertEquals(List.of(H, O), List.copyOf(water.recipe().keySet()));
            assertEquals(3, water.atoms());
            assertEquals(10, water.protons());
        }

        @Test
        void lesParenthesesMultiplientCeQuEllesContiennent() {
            assertEquals(Map.of(20, 3, 15, 2, O, 8), Molecule.of(Molecule.Kind.SALT, "x", "Ca3(PO4)2").recipe());
            assertEquals("Ca₃(PO₄)₂", Molecule.of(Molecule.Kind.SALT, "x", "Ca3(PO4)2").formula());
            assertEquals(Map.of(29, 3, C, 2, O, 8, H, 2), Molecule.of(Molecule.Kind.MINERAL, "x", "Cu3(CO3)2(OH)2").recipe());
            assertEquals(Map.of(26, 1, O, 2, H, 1), Molecule.of(Molecule.Kind.MINERAL, "x", "FeO(OH)").recipe());
            assertEquals(Map.of(26, 7, C, 18, 7, 18), Molecule.of(Molecule.Kind.MATERIAL, "x", "Fe7(CN)18").recipe());
            assertEquals(Map.of(82, 1, C, 8, H, 20), Molecule.of(Molecule.Kind.MATERIAL, "x", "Pb(C2H5)4").recipe());
        }

        @Test
        void unElementEcritPlusieursFoisEstAdditionne() {
            Molecule acid = Molecule.of(Molecule.Kind.ACID, "Acide acétique", "CH3COOH");
            assertEquals(Map.of(C, 2, H, 4, O, 2), acid.recipe());
            assertEquals(List.of(C, H, O), List.copyOf(acid.recipe().keySet()));
            assertEquals(8, acid.atoms());
        }

        @Test
        void uneFormuleIllisibleEstRefusee() {
            for (String formula : new String[] {"", "h2o", "H2O)", "(H2O", "Xx2", "H0", "2H", "H2 O"}) {
                assertThrows(IllegalArgumentException.class, () -> Molecule.of(Molecule.Kind.SIMPLE, "x", formula));
            }
            assertThrows(IllegalArgumentException.class, () -> Molecule.of(Molecule.Kind.SIMPLE, " ", "H2O"));
            assertThrows(IllegalArgumentException.class, () -> Molecule.of(null, "x", "H2O"));
        }

        @Test
        void leCatalogueTientDebout() {
            List<Molecule> molecules = Molecules.DEFAULT;
            assertEquals(430, molecules.size());
            assertEquals(430, molecules.stream().map(Molecule::id).distinct().count());
            assertEquals(430, molecules.stream().map(Molecule::name).distinct().count());
            for (Molecule.Kind kind : Molecule.Kind.values()) {
                assertTrue(molecules.stream().anyMatch(molecule -> molecule.kind() == kind), "rayon vide : " + kind);
            }
            for (Molecule molecule : molecules) {
                for (int number : molecule.recipe().keySet()) {
                    // Un élément unique ne peut pas quitter le tableau : aucune formule n'en demande.
                    assertFalse(PeriodicTable.element(number).category().unique(), molecule.id() + " demande un element unique");
                }
            }
        }

        @Test
        void chaqueMoleculeEstFaisableAuMoinsUneFoisArbreComplet() {
            Game game = gameAfterBigBang();
            game.state().setDarkLevel("dark_isotopes", 2);
            for (Molecule molecule : game.molecules()) {
                assertTrue(game.isMoleculeWithinReach(molecule.id()), molecule.id() + " demande trop d'exemplaires");
            }
            // Et une bonne moitié du catalogue l'est sans les Isotopes, avec neuf exemplaires par élément.
            game.state().setDarkLevel("dark_isotopes", 0);
            long reachable = game.molecules().stream().filter(molecule -> game.isMoleculeWithinReach(molecule.id())).count();
            assertTrue(reachable > 300 && reachable < 430, "a portee sans Isotopes : " + reachable);
            assertFalse(game.isMoleculeWithinReach(GLUCOSE));
        }

        @Test
        void seulesLesPetitesMoleculesSontLaAuPremierBigBang() {
            Game game = bareGameAfterBigBang();
            assertTrue(game.isMoleculeKindUnlocked(Molecule.Kind.SIMPLE));
            assertEquals(1, game.moleculeKindsUnlocked());
            assertEquals(Molecule.Kind.ACID, game.nextMoleculeKind());
            for (Molecule.Kind kind : Molecule.Kind.values()) {
                if (kind != Molecule.Kind.SIMPLE) assertFalse(game.isMoleculeKindUnlocked(kind), kind + " ne devrait pas etre ouvert");
            }
        }

        @Test
        void aucunRayonAvantLePremierBigBang() {
            Game game = gameAfterBigBang();
            GameTestSupport.bangs(game, 0);
            assertEquals(0, game.moleculeKindsUnlocked());
            assertFalse(game.isMoleculeKindUnlocked(Molecule.Kind.SIMPLE));
            assertEquals(Molecule.Kind.SIMPLE, game.nextMoleculeKind());
        }

        @Test
        void unRayonFermeNeSeCreePas() {
            Game game = bareGameAfterBigBang();
            // Le sel a la place et les éléments qu'il lui faut, mais son rayon n'est pas ouvert.
            assertTrue(game.hasElementsForMolecule(SALT));
            assertTrue(game.hasSpaceForMolecule(SALT));
            assertFalse(game.canCreateMolecule(SALT));
            assertFalse(game.createMolecule(SALT));
            assertTrue(game.canCreateMolecule(WATER));
            game.state().addSpaceUpgrade("space_acid");
            assertFalse(game.canCreateMolecule(SALT));     // les acides ne sont pas les sels
            game.state().addSpaceUpgrade("space_salt");
            assertTrue(game.createMolecule(SALT));
        }

        @Test
        void pasDeMoleculeAvantLePremierBigBang() {
            Game game = gameAfterBigBang();
            GameTestSupport.bangs(game, 0);
            assertFalse(game.canCreateMolecule(WATER));
            assertFalse(game.createMolecule(WATER));
            assertEquals(0, game.moleculeCount(WATER));
            assertEquals(9, game.elementCount(H));
        }

        @Test
        void uneMoleculeDEauPrendDeuxHydrogenesEtUnOxygene() {
            Game game = gameAfterBigBang();
            assertTrue(game.createMolecule(WATER));
            assertEquals(1, game.moleculeCount(WATER));
            assertEquals(7, game.elementCount(H));
            assertEquals(8, game.elementCount(O));
            assertEquals(9, game.elementCount(C));
            assertEquals(1, game.moleculesCreated());
            assertEquals(1, game.moleculeKindsCreated());
        }

        @Test
        void unExemplaireDeChaqueElementResteDansLeTableau() {
            Game game = gameAfterBigBang();
            game.state().setElementCount(H, 2);
            assertFalse(game.hasElementsForMolecule(WATER));
            assertFalse(game.canCreateMolecule(WATER));
            game.state().setElementCount(H, 3);
            game.state().setElementCount(O, 1);
            assertFalse(game.canCreateMolecule(WATER));
            game.state().setElementCount(O, 2);
            assertTrue(game.createMolecule(WATER));
            assertEquals(1, game.elementCount(H));
            assertEquals(1, game.elementCount(O));
            assertEquals(118, game.discoveredElements());
            assertTrue(game.canExplode());
        }

        @Test
        void unePremiereMoleculeNeChangePasLePrixDeLaSuivante() {
            Game game = gameAfterBigBang();
            assertEquals(Map.of(H, 2, O, 1), game.nextMoleculeCost(WATER));
            assertTrue(game.createMolecule(WATER));
            assertEquals(Map.of(H, 2, O, 1), game.nextMoleculeCost(WATER));
            assertTrue(game.createMolecule(WATER));
            assertEquals(5, game.elementCount(H));
            assertEquals(7, game.elementCount(O));
            assertEquals(Map.of(H, 2, O, 1), game.nextMoleculeCost(WATER));
            assertEquals(Map.of(11, 1, 17, 1), game.nextMoleculeCost(SALT));
        }

        @Test
        void leJournalGardeLOrdreDesCreationsParSuites() {
            GameState state = new GameState();
            state.addMolecules("H2O", 3);
            state.addMolecule("NaCl");
            state.addMolecules("H2O", 2);
            state.addMolecules("H2", 4);
            state.addMolecules("H2", 0);
            assertEquals(List.of("H2O", "H2O", "H2O", "NaCl", "H2O", "H2O", "H2", "H2", "H2", "H2"),
                    new java.util.ArrayList<>(state.moleculeLog()));
            assertEquals(10, state.moleculeLog().size());
            assertEquals("H2O", state.moleculeLog().get(0));
            assertEquals("NaCl", state.moleculeLog().get(3));
            assertEquals("H2O", state.moleculeLog().get(4));
            assertEquals("H2", state.moleculeLog().get(9));
            assertThrows(IndexOutOfBoundsException.class, () -> state.moleculeLog().get(10));
            assertThrows(UnsupportedOperationException.class, () -> state.moleculeLog().add("H2"));
            assertThrows(IllegalArgumentException.class, () -> state.addMolecules("H2", -1));
            // Les molécules en trop partent en commençant par les dernières créées.
            state.setMoleculeCount("H2O", 2);
            assertEquals(List.of("H2O", "H2O", "NaCl", "H2", "H2", "H2", "H2"), new java.util.ArrayList<>(state.moleculeLog()));
            state.setMoleculeCount("NaCl", 0);
            assertEquals(List.of("H2O", "H2O", "H2", "H2", "H2", "H2"), new java.util.ArrayList<>(state.moleculeLog()));
            assertEquals(0, state.moleculeCount("NaCl"));
            assertFalse(state.molecules().containsKey("NaCl"));
            state.setMoleculeCount("H2", 1);
            state.setMoleculeCount("H2O", 3);
            assertEquals(List.of("H2O", "H2O", "H2", "H2O"), new java.util.ArrayList<>(state.moleculeLog()));
            // Des millions de molécules ne coûtent qu'une suite.
            state.addMolecules("H2", 5_000_000);
            assertEquals(5_000_004, state.moleculeLog().size());
            assertEquals("H2", state.moleculeLog().get(5_000_003));
            assertEquals("H2O", state.moleculeLog().get(3));
            assertEquals(5_000_001, state.moleculeCount("H2"));
            // Beaucoup de suites : le journal grandit sans rien perdre.
            GameState many = new GameState();
            for (int round = 0; round < 100; round++) {
                many.addMolecules("H2O", 2);
                many.addMolecules("NaCl", 1);
            }
            assertEquals(300, many.moleculeLog().size());
            assertEquals("NaCl", many.moleculeLog().get(299));
            assertEquals("H2O", many.moleculeLog().get(297));
            int water = 0;
            for (String id : many.moleculeLog()) if (id.equals("H2O")) water++;
            assertEquals(200, water);
            many.reset();
            assertTrue(many.moleculeLog().isEmpty());
        }

        @Test
        void leTableauRegarniDonneAutantDeMoleculesQuOnVeut() {
            Game game = gameAfterBigBang();
            // Neuf exemplaires : quatre molécules d'eau avant de devoir regarnir le tableau (il reste un hydrogène).
            int created = 0;
            while (game.createMolecule(WATER)) created++;
            assertEquals(4, created);
            assertEquals(1, game.elementCount(H));
            assertEquals(5, game.elementCount(O));
            assertTrue(game.isMoleculeWithinReach(WATER));
            // Le tableau regarni, on en crée autant qu'on veut : le prix ne monte pas.
            for (int round = 0; round < 10; round++) {
                refill(game);
                assertTrue(game.createMolecule(WATER));
            }
            assertEquals(14, game.moleculeCount(WATER));
            assertEquals(Map.of(H, 2, O, 1), game.nextMoleculeCost(WATER));
        }

        @Test
        void leGlucoseAttendLesIsotopes() {
            Game game = gameAfterBigBang();
            assertFalse(game.isMoleculeWithinReach(GLUCOSE));
            game.state().setDarkLevel("dark_isotopes", 1);     // seize exemplaires par élément commun
            assertTrue(game.isMoleculeWithinReach(GLUCOSE));
            assertFalse(game.canCreateMolecule(GLUCOSE));      // le tableau n'en a encore que neuf
            refill(game);
            assertTrue(game.createMolecule(GLUCOSE));
            assertEquals(4, game.elementCount(H));
            assertEquals(10, game.elementCount(C));
            assertEquals(10, game.elementCount(O));
            // La deuxième demande la même chose : il suffit de regarnir le tableau.
            assertTrue(game.isMoleculeWithinReach(GLUCOSE));
            assertFalse(game.canCreateMolecule(GLUCOSE));
            refill(game);
            assertTrue(game.createMolecule(GLUCOSE));
            assertEquals(2, game.moleculeCount(GLUCOSE));
        }

        @Test
        void uneMoleculeOccupeDixUnitesDEspaceParProtonSelonSonEtat() {
            Game game = gameAfterBigBang();
            assertValue(280, game.moleculeVolume(SALT));          // un cristal : dix par proton
            assertValue(960, game.moleculeVolume(GLUCOSE));       // un solide : pareil
            assertValue(200, game.moleculeVolume(WATER));         // un liquide : le double
            assertValue(400, game.moleculeVolume("H2"));          // un gaz : vingt fois plus
            assertValue(375, game.moleculeVolume("NiTi"));        // un métal : les trois quarts
        }

        @Test
        void plusUneMoleculeEstComplexeOuLourdePlusElleDemande() {
            Game game = gameAfterBigBang();
            // Des solides et des cristaux, du plus léger au plus lourd.
            String[] growing = {"LiH", SALT, GLUCOSE, "C20H24N2O2", "Pb5(VO4)3Cl"};
            for (int index = 1; index < growing.length; index++) {
                assertTrue(game.moleculeVolume(growing[index]).gt(game.moleculeVolume(growing[index - 1])),
                        growing[index] + " devrait occuper plus que " + growing[index - 1]);
            }
            // À nombre d'atomes égal, des éléments plus lourds prennent plus de place.
            assertEquals(game.molecule("PbS").atoms(), game.molecule(SALT).atoms());
            assertTrue(game.moleculeVolume("PbS").gt(game.moleculeVolume(SALT)));
            // Et un gaz en prend bien plus qu'un cristal : le dihydrogène, deux protons, plus que le sel.
            assertTrue(game.moleculeVolume("H2").gt(game.moleculeVolume(SALT)));
            assertTrue(game.moleculeVolume("CO").gt(game.moleculeVolume("PbS")));
        }

        @Test
        void sansEspaceLibrePasDeMolecule() {
            Game game = gameAfterBigBang();
            game.state().setSpace(BigNum.of(199));
            assertTrue(game.hasElementsForMolecule(WATER));
            assertFalse(game.hasSpaceForMolecule(WATER));
            assertFalse(game.createMolecule(WATER));
            assertEquals(9, game.elementCount(H));
            game.state().setSpace(BigNum.of(200));
            assertTrue(game.createMolecule(WATER));
            assertValue(200, game.occupiedSpace());
            assertTrue(game.freeSpace().isZero());
            refill(game);
            assertFalse(game.canCreateMolecule(WATER));      // l'espace est plein
            assertFalse(game.canCreateMolecule(SALT));
            game.state().setSpace(BigNum.of(480));
            assertTrue(game.canCreateMolecule(SALT));         // vingt-huit protons : 280 unités
        }

        @Test
        void lEspaceOccupeSAdditionneEtNeSeLiberePas() {
            Game game = gameAfterBigBang();
            assertTrue(game.occupiedSpace().isZero());
            assertTrue(game.createMolecule(WATER));
            assertTrue(game.createMolecule(SALT));
            assertTrue(game.createMolecule(WATER));
            assertValue(680, game.occupiedSpace());
            assertValue(1e9 - 680, game.freeSpace());
            assertTrue(game.explode());
            assertValue(680, game.occupiedSpace());
        }

        @Test
        void lEspaceLibreNEstJamaisNegatif() {
            Game game = gameAfterBigBang();
            game.state().setMoleculeCount(GLUCOSE, 3);
            game.state().setSpace(BigNum.of(50));
            assertValue(2880, game.occupiedSpace());
            assertTrue(game.freeSpace().isZero());
        }

        @Test
        void chaqueMoleculeDonneQuelqueChose() {
            int boosts = 0, uncaps = 0;
            for (Molecule molecule : Molecules.DEFAULT) {
                assertTrue(molecule.hasBonus(), molecule.id());
                if (molecule.bonus() instanceof Molecule.Boost) boosts++;
                else uncaps++;
            }
            assertEquals(26 + 251, boosts);
            assertEquals(15 + 138, uncaps);
        }

        @Test
        void unBonusGranditAvecLaTailleDeLaMolecule() {
            // Dans un même rayon : 4 % sous 30 protons, puis 6, 8, 10, 12 et 15 %, plus un point par rayon.
            assertEquals(new Molecule.Boost(Molecule.Stat.PARTICLES, 0.04), molecule("HCl").bonus());       // 18 protons
            assertEquals(new Molecule.Boost(Molecule.Stat.PARTICLES, 0.06), molecule("H2SO4").bonus());     // 50
            assertEquals(new Molecule.Boost(Molecule.Stat.PARTICLES, 0.10), molecule("C6H8O7").bonus());    // 100
            assertEquals(new Molecule.Boost(Molecule.Stat.ATOMS, 0.17), molecule("C20H24N2O2").bonus());    // 174, au rayon du vivant
            assertEquals(new Molecule.Boost(Molecule.Stat.ATOMS, 0.20), molecule("C10H16N5O13P3").bonus()); // 260
        }

        private Molecule molecule(String id) {
            return Molecules.DEFAULT.stream().filter(each -> each.id().equals(id)).findFirst().orElseThrow();
        }

        @Test
        void lesBonusSontLegers() {
            for (Molecule molecule : Molecules.DEFAULT) {
                if (molecule.bonus() instanceof Molecule.Boost boost) {
                    // 2 à 10 % pour les petites molécules, jusqu'à 20 % pour les plus grosses des derniers rayons ;
                    // l'espace, qui s'entretient lui-même, donne trois fois moins : 1 à 6 %.
                    boolean space = boost.stat() == Molecule.Stat.SPACE;
                    double least = space ? 0.01 : 0.02;
                    double most = space ? 0.06 : molecule.kind() == Molecule.Kind.SIMPLE ? 0.10 : 0.20;
                    assertTrue(boost.perMolecule() >= least && boost.perMolecule() <= most + 1e-12, molecule.id() + " : " + boost.perMolecule());
                }
            }
            // Chaque grandeur a ses molécules, parmi les petites comme dans le reste du catalogue.
            for (Molecule.Stat stat : Molecule.Stat.values()) {
                for (boolean small : new boolean[] {true, false}) {
                    assertTrue(Molecules.DEFAULT.stream().anyMatch(molecule -> (molecule.kind() == Molecule.Kind.SIMPLE) == small
                            && molecule.bonus() instanceof Molecule.Boost boost && boost.stat() == stat), "aucune molecule pour " + stat);
                }
            }
        }

        @Test
        void unPlafondReleveEstToujoursCeluiDUnElementDeLaFormule() {
            Map<Molecule.Kind, java.util.Set<Integer>> raised = new EnumMap<>(Molecule.Kind.class);
            for (Molecule molecule : Molecules.DEFAULT) {
                if (molecule.bonus() instanceof Molecule.Uncap uncap) {
                    assertTrue(molecule.recipe().containsKey(uncap.element()), molecule.id());
                    assertFalse(PeriodicTable.element(uncap.element()).category().unique(), molecule.id());
                    // Dans un rayon, un élément n'a qu'une molécule qui relève son plafond.
                    assertTrue(raised.computeIfAbsent(molecule.kind(), each -> new java.util.HashSet<>()).add(uncap.element()),
                            molecule.kind() + " : deux molecules relevent le plafond de l'element " + uncap.element());
                    // Hors des petites molécules, c'est toujours un métal ou un métalloïde.
                    if (molecule.kind() != Molecule.Kind.SIMPLE) {
                        ElementCategory family = PeriodicTable.element(uncap.element()).category();
                        assertTrue(family != ElementCategory.NONMETAL && family != ElementCategory.HALOGEN, molecule.id());
                    }
                }
            }
            java.util.Set<Integer> simple = raised.get(Molecule.Kind.SIMPLE);
            assertEquals(15, simple.size());
            assertTrue(simple.contains(H) && simple.contains(C) && simple.contains(O));
            // La chimie du carbone et le vivant n'ont pas de métal : aucun plafond relevé.
            assertFalse(raised.containsKey(Molecule.Kind.ORGANIC));
            assertFalse(raised.containsKey(Molecule.Kind.LIFE));
            assertEquals(8, raised.get(Molecule.Kind.ACID).size());
            assertTrue(raised.get(Molecule.Kind.MINERAL).contains(24));    // la chromite, pour le chrome
        }

        @Test
        void unBonusMalDecritEstRefuse() {
            Molecule water = Molecule.of(Molecule.Kind.SIMPLE, "Eau", "H2O");
            assertFalse(water.hasBonus());
            assertTrue(water.uncapping("O").hasBonus());
            assertThrows(IllegalArgumentException.class, () -> water.uncapping("Na"));     // pas dans la formule
            assertThrows(IllegalArgumentException.class, () -> water.uncapping("Xx"));
            assertThrows(IllegalArgumentException.class, () -> water.boosting(Molecule.Stat.SPACE, 0));
            assertThrows(IllegalArgumentException.class, () -> water.boosting(null, 0.05));
        }

        @Test
        void sansMoleculeAucunBonus() {
            Game game = gameAfterBigBang();
            for (Molecule.Stat stat : Molecule.Stat.values()) assertEquals(1.0, game.moleculeBoost(stat), 0);
            for (Element element : PeriodicTable.ELEMENTS) assertEquals(0, game.elementUncap(element.number()));
        }

        @Test
        void lesBonusDUneMemeGrandeurSAdditionnent() {
            Game game = gameAfterBigBang();
            double before = game.particlesPerCreation().log10();
            game.state().setMoleculeCount(WATER, 1);       // +5 %
            game.state().setMoleculeCount("H2O2", 1);      // +5 %
            game.state().setMoleculeCount("SO3", 1);       // +8 %
            assertEquals(1.18, game.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            assertEquals(before + Math.log10(1.18), game.particlesPerCreation().log10(), 1e-9);
            assertEquals(1.0, game.moleculeBoost(Molecule.Stat.ATOMS), 0);
        }

        @Test
        void leDioxydeDeCarboneAccelereUnPeuLExpansion() {
            Game game = gameAfterBigBang();
            assertValue(1, game.spacePerSecond());
            game.state().setMoleculeCount("CO2", 1);                  // +1,5 %
            assertValue(1.015, game.spacePerSecond());
            game.state().setMoleculeCount("SF6", 3);                  // deux doublements, à 3 % chacun
            assertValue(1.075, game.spacePerSecond());
            BigNum space = game.state().space();
            game.tick(100);
            assertValue(space.toDouble() + 107.5, game.state().space());
        }

        @Test
        void lAmmoniacAugmenteUnPeuLesAtomes() {
            Game game = gameAfterBigBang();
            game.state().setLevel("generator", 9);
            double before = game.atomsPerFusion().toDouble();
            game.state().setMoleculeCount("NH3", 7);                  // trois doublements, à 3 % chacun
            assertEquals(1.09, game.moleculeBoost(Molecule.Stat.ATOMS), 1e-12);
            assertTrue(game.atomsPerFusion().toDouble() >= before, "atomes par fusion : " + before + " -> " + game.atomsPerFusion());
        }

        @Test
        void leProtoxydeDAzoteFaitGrossirLaMatiereNoireUnPeuPlusVite() {
            Game game = gameAfterBigBang();
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(3));
            double before = game.darkMatterMomentum();
            game.state().setMoleculeCount("N2O", 3);                  // deux doublements, à 5 % chacun
            assertEquals(before * 1.10, game.darkMatterMomentum(), before * 1e-9);
        }

        @Test
        void lesMoleculesDesAutresRayonsDonnentAussi() {
            Game game = gameAfterBigBang();
            int copies = game.maxTotalCopies();
            game.state().setMoleculeCount("H2SO4", 3);                // deux doublements
            assertEquals(1 + 2 * 0.06, game.moleculeBoost(Molecule.Stat.PARTICLES), 1e-12);
            game.state().setMoleculeCount("C20H24N2O2", 1);
            assertEquals(1 + 0.17, game.moleculeBoost(Molecule.Stat.ATOMS), 1e-12);
            // Le sel de table et la soude relèvent tous deux le plafond du sodium, chacun dans son rayon.
            game.state().setMoleculeCount("NaCl", 1);
            assertEquals(1, game.elementUncap(11));
            game.state().setMoleculeCount("NaOH", 3);
            assertEquals(3, game.elementUncap(11));
            assertEquals(12, game.maxCopiesOf(PeriodicTable.element(11)));
            assertEquals(copies + 3, game.maxTotalCopies());
            assertValue(1, game.spacePerSecond());
        }

        @Test
        void chaqueDihydrogeneReleveLePlafondDeLHydrogene() {
            Game game = gameAfterBigBang();
            Element hydrogen = PeriodicTable.element(H);
            int total = game.maxTotalCopies();
            assertEquals(9, game.maxCopiesOf(hydrogen));
            assertTrue(game.isElementMaxed(H));
            assertTrue(game.isPeriodicTableComplete());
            assertTrue(game.createMolecule("H2"));
            assertEquals(1, game.elementUncap(H));
            assertEquals(10, game.maxCopiesOf(hydrogen));
            // Les autres éléments de la famille, et la famille elle-même, n'ont pas bougé.
            assertEquals(9, game.maxCopiesOf(PeriodicTable.element(C)));
            assertEquals(9, game.maxCopiesOf(ElementCategory.NONMETAL));
            assertEquals(total + 1, game.maxTotalCopies());
            // Neuf hydrogènes ne suffisent plus à remplir le tableau : la synthèse peut en redonner.
            refill(game);
            assertFalse(game.isElementMaxed(H));
            assertFalse(game.isPeriodicTableComplete());
            game.state().setElementCount(H, 10);
            assertTrue(game.isElementMaxed(H));
            assertTrue(game.isPeriodicTableComplete());
            assertEquals(total + 1, game.ownedCopies());
        }

        @Test
        void unPlafondReleveMetDautresMoleculesAPortee() {
            Game game = gameAfterBigBang();
            // Le butane demande dix hydrogènes, plus celui qui reste : hors de portée avec neuf exemplaires.
            assertFalse(game.isMoleculeWithinReach("C4H10"));
            game.state().setMoleculeCount("H2", 2);                   // pas encore deux doublements
            assertEquals(10, game.maxCopiesOf(PeriodicTable.element(H)));
            assertFalse(game.isMoleculeWithinReach("C4H10"));
            game.state().setMoleculeCount("H2", 3);
            assertEquals(11, game.maxCopiesOf(PeriodicTable.element(H)));
            assertTrue(game.isMoleculeWithinReach("C4H10"));
        }

        @Test
        void lePlafondReleveSAjouteAuxIsotopes() {
            Game game = gameAfterBigBang();
            game.state().setMoleculeCount("O2", 7);                   // trois doublements
            assertEquals(12, game.maxCopiesOf(PeriodicTable.element(O)));
            game.state().setDarkLevel("dark_isotopes", 1);
            assertEquals(19, game.maxCopiesOf(PeriodicTable.element(O)));
            assertEquals(16, game.maxCopiesOf(PeriodicTable.element(C)));
            // Un élément unique reste unique, quoi qu'il arrive.
            assertEquals(1, game.maxCopiesOf(PeriodicTable.element(2)));
        }

        @Test
        void lePlafondReleveSurvitALExplosionEtAuBigBang() {
            Game game = gameAfterBigBang();
            assertTrue(game.createMolecule("H2"));
            assertTrue(game.explode());
            assertEquals(10, game.maxCopiesOf(PeriodicTable.element(H)));
            game.reset();
            assertEquals(9, game.maxCopiesOf(PeriodicTable.element(H)));
        }

        @Test
        void lOrdreDeCreationEstGarde() {
            Game game = gameAfterBigBang();
            assertTrue(game.createMolecule(SALT));
            assertTrue(game.createMolecule(WATER));
            assertTrue(game.createMolecule(SALT));
            assertEquals(List.of(SALT, WATER, SALT), game.state().moleculeLog());
            assertEquals(2, game.moleculeCount(SALT));
            assertEquals(2, game.moleculeKindsCreated());
            // Retirer une molécule retire la dernière créée de sa sorte.
            game.state().setMoleculeCount(SALT, 1);
            assertEquals(List.of(SALT, WATER), game.state().moleculeLog());
            game.state().setMoleculeCount(WATER, 0);
            assertEquals(List.of(SALT), game.state().moleculeLog());
            assertEquals(1, game.moleculeKindsCreated());
        }

        @Test
        void lesMoleculesSurviventALExplosionEtAuBigBang() {
            Game game = gameAfterBigBang();
            assertTrue(game.createMolecule(WATER));
            assertTrue(game.explode());
            assertEquals(1, game.moleculeCount(WATER));
            assertEquals(0, game.discoveredElements());
            GameTestSupport.readyForBigBang(game);
            assertTrue(game.bigBang());
            GameTestSupport.payMilestones(game);
            assertEquals(2, game.bigBangs());
            assertEquals(1, game.moleculeCount(WATER));
            assertEquals(List.of(WATER), game.state().moleculeLog());
        }

        @Test
        void laRemiseAZeroEffaceLesMolecules() {
            Game game = gameAfterBigBang();
            assertTrue(game.createMolecule(WATER));
            game.reset();
            assertEquals(0, game.moleculeCount(WATER));
            assertEquals(0, game.moleculesCreated());
            assertTrue(game.state().hasNoMolecule());
            assertTrue(game.occupiedSpace().isZero());
        }

        @Test
        void uneMoleculeInconnueEstRefusee() {
            Game game = gameAfterBigBang();
            assertThrows(IllegalArgumentException.class, () -> game.createMolecule("phlogistique"));
            assertThrows(IllegalArgumentException.class, () -> game.moleculeCount("phlogistique"));
            assertThrows(IllegalArgumentException.class, () -> game.moleculeVolume("phlogistique"));
            assertThrows(IllegalArgumentException.class, () -> game.state().setMoleculeCount(WATER, -1));
        }
    }
}

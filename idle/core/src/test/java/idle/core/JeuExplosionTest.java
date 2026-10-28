package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.darkmatter.DarkUpgrade;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.darkmatter.Landmark;
import idle.core.darkmatter.SizeScale;
import idle.core.elements.Element;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * L'explosion du tableau périodique (ce qu'elle demande, ce qu'elle donne, ce qu'elle efface) et la taille de la
 * matière noire ; puis, dans l'arbre de matière noire, ce que la matière noire ouvre : les bifurcations et leurs
 * liaisons, les accords, la superposition, et les voies d'absence.
 */
class JeuExplosionTest extends GameTestSupport {


    @Nested
    class Explosion {

        /** Remplit le tableau périodique : chaque élément à son maximum d'exemplaires. */
        private void fillTable(Game game) {
            GameTestSupport.fillTableToMax(game);
        }

        /** Partie bien avancée : améliorations, automatismes, atomes, particules, tableau débloqué. */
        private Game advancedGame() {
            Game game = gameReadyToFuse();
            game.state().setAtoms(BigNum.of(50));
            game.state().setTotalAtoms(BigNum.of(300));
            game.buy("double");
            game.buy("mass");
            unlock(game);
            game.setAutomationEnabled("autoSpeed", true);
            game.state().setSynthesisCount(40);
            game.state().setParticles(BigNum.of(1234));
            return game;
        }

        @Test
        void impossibleTantQuIlManqueUnElement() {
            Game game = advancedGame();
            assertFalse(game.canExplode());
            fillTable(game);
            game.state().setElementCount(26, 0);              // il manque le fer
            assertFalse(game.canExplode());
            assertFalse(game.explode());
            assertEquals(0, game.state().explosions());
            assertValue(0, game.state().darkMatter());
            assertEquals(9, game.elementCount(29));           // rien n'a été effacé
            assertFalse(game.isDarkMatterUnlocked());
        }

        @Test
        void unExemplaireDeChaqueElementSuffit() {
            Game game = advancedGame();
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertFalse(game.isPeriodicTableComplete());      // il reste des exemplaires à obtenir
            assertTrue(game.canExplode());
            assertTrue(game.explode());
            assertValue(1, game.state().darkMatter());
        }

        @Test
        void ilManqueDesExemplairesMaisPasDElement() {
            Game game = advancedGame();
            fillTable(game);
            game.state().setElementCount(26, 8);              // il manque un exemplaire de fer : sans importance
            assertTrue(game.canExplode());
        }

        @Test
        void impossibleAvantLeDemarrage() {
            Game game = unstartedGame();
            fillTable(game);
            assertFalse(game.canExplode());
            assertFalse(game.explode());
        }

        @Test
        void leTableauCompletPeutExploser() {
            Game game = advancedGame();
            fillTable(game);
            assertTrue(game.isPeriodicTableComplete());
            assertTrue(game.canExplode());
        }

        @Test
        void chaqueExplosionAlourditLeTableau() {
            Game game = advancedGame();
            assertValue(1, game.tableWeight());
            assertValue(118, game.atomCap());
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(1, game.state().tableWeightLevel());
            assertValue(Game.TABLE_WEIGHT_GROWTH, game.tableWeight());
            assertValue(118 * Game.TABLE_WEIGHT_GROWTH, game.atomCap());
            fillTable(game);
            assertTrue(game.explode());
            assertValue(118 * Game.TABLE_WEIGHT_GROWTH * Game.TABLE_WEIGHT_GROWTH, game.atomCap());
        }

        @Test
        void unTableauPlusLourdReleveLePlafondDAtomesEtLePrixDesSyntheses() {
            Game game = advancedGame();
            game.state().setTableWeightLevel(1);              // comme après une explosion
            BigNum cap = game.atomCap();
            game.state().setAtoms(BigNum.of(118));
            assertFalse(game.isAtomCapReached());             // 118 n'est plus le plafond
            game.state().setAtoms(cap);
            assertTrue(game.isAtomCapReached());
            game.state().setSynthesisCount(40);               // le prix a doublé bien au-delà du plafond
            assertValue(cap.toDouble(), game.synthesisCost());
            game.state().setSynthesisCount(3);                // en dessous, il double comme avant
            assertValue(16, game.synthesisCost());
        }

        @Test
        void leTableauCesseDeSAlourdirApresUnCertainNombreDExplosions() {
            Game game = advancedGame();
            game.state().setTableWeightLevel(Game.TABLE_WEIGHT_MAX_LEVEL);
            BigNum heaviest = game.atomCap();
            game.state().setTableWeightLevel(Game.TABLE_WEIGHT_MAX_LEVEL + 5);
            assertValue(heaviest.toDouble(), game.atomCap());
            assertValue(Math.pow(Game.TABLE_WEIGHT_GROWTH, Game.TABLE_WEIGHT_MAX_LEVEL), game.tableWeight());
        }

        @Test
        void lArbreNAttendAucuneMatiereNoire() {
            // Depuis la version 0.070, plus aucun seuil : une seule matière noire, et chaque case dont celle du
            // dessus est prise s'offre. Seuls les prix rythment l'arbre.
            Game game = newGame();
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.ONE);
            for (DarkUpgrade dark : DarkUpgrades.DEFAULT) {
                if (dark.requires() == null) assertTrue(game.isDarkAvailable(dark.id()), dark.id());
            }
            game.state().setDarkLevel("dark_overflow", 1);
            assertTrue(game.isDarkAvailable("dark_gravity"));            // autrefois à 30 matières noires
        }

        @Test
        void laRemiseAZeroRendAuTableauSaMasseDuDebut() {
            Game game = advancedGame();
            fillTable(game);
            assertTrue(game.explode());
            game.reset();
            assertEquals(0, game.state().tableWeightLevel());
            assertValue(118, game.atomCap());
        }

        @Test
        void lExplosionDonneDeLaMatiereNoire() {
            Game game = advancedGame();
            fillTable(game);
            assertTrue(game.explode());
            assertValue(1, game.state().darkMatter());
            assertEquals(1, game.state().explosions());
            assertTrue(game.isDarkMatterUnlocked());
        }

        @Test
        void lExplosionEffaceTouteLaMatiere() {
            Game game = advancedGame();
            fillTable(game);
            double played = game.state().timePlayed();
            assertTrue(game.explode());

            assertValue(0, game.state().particles());
            assertValue(0, game.state().atoms());
            assertValue(0, game.state().totalAtoms());
            assertEquals(0, game.state().timeSinceFusion());
            assertEquals(0, game.discoveredElements());
            assertEquals(0, game.ownedCopies());
            assertEquals(0, game.state().synthesisCount());
            for (Upgrade upgrade : game.upgrades()) {
                assertEquals(0, game.levelOf(upgrade.id()), () -> upgrade.name());
            }
            for (Automation automation : game.automations()) {
                assertFalse(game.ownsAutomation(automation.id()));
                assertFalse(game.isAutomationEnabled(automation.id()));
                assertEquals(0, game.automationSpeedLevel(automation.id()));
            }
            assertFalse(game.isAutomationUnlocked());
            assertFalse(game.isPeriodicTableUnlocked());
            assertFalse(game.canExplode());

            // La partie repart comme au tout début : un générateur, la vitesse de base, aucun bonus.
            assertTrue(game.isStarted());
            assertEquals(1, game.generatorCount());
            assertValue(BASE, game.speed());
            assertValue(1, game.particlesPerCreation());
            assertValue(BASE, game.productionPerSecond());
            assertValue(1, game.atomsPerFusion());
            assertEquals(played, game.state().timePlayed());  // le temps de jeu, lui, n'est pas effacé
        }

        @Test
        void apresLExplosionLeJeuSeRejoueNormalement() {
            Game game = advancedGame();
            fillTable(game);
            game.explode();
            game.tick(ONE_PARTICLE);                          // aucun automatisme : une particule, pas plus
            assertValue(1, game.state().particles());
            game.state().setParticles(BigNum.of(1e9));
            assertTrue(game.buy("gen"));
            assertTrue(game.buy("gen"));
            assertTrue(game.fuse());
            assertValue(1, game.state().atoms());
            assertValue(1, game.state().darkMatter());        // la fusion ne touche pas à la matière noire
        }

        @Test
        void chaqueExplosionAjouteDeLaMatiereNoire() {
            Game game = advancedGame();
            fillTable(game);
            game.explode();
            assertFalse(game.explode());                      // le tableau est vide : pas de seconde explosion
            fillTable(game);
            // Sans battre le record de la première : pas de prime de vitesse, une matière noire de plus.
            game.state().stats().addTime(game.explosionRecord() + 1);
            assertTrue(game.explode());
            assertValue(2, game.state().darkMatter());
            assertEquals(2, game.state().explosions());
        }

        @Test
        void laMatiereNoireEtLesExplosionsNePeuventPasEtreNegatives() {
            GameState state = new GameState();
            assertThrows(IllegalArgumentException.class, () -> state.setDarkMatter(BigNum.of(-1)));
            assertThrows(IllegalArgumentException.class, () -> state.setExplosions(-1));
        }
    }

    @Nested
    class TailleDeLaMatiereNoire {

        private static final double START = Game.DARK_MATTER_START_SIZE.toDouble();
        /** L'élan qu'apporte une matière noire, par seconde d'appui. */
        private static final double UNIT = Game.DARK_MATTER_GROWTH_PER_UNIT;

        /** Partie qui possède {@code darkMatter} unités de matière noire, après autant d'explosions. */
        private Game gameWithDarkMatter(int darkMatter) {
            Game game = newGame();
            game.state().setDarkMatter(BigNum.of(darkMatter));
            game.state().setExplosions(darkMatter);
            return game;
        }

        /** Les bifurcations (0.069) : prendre une voie ferme l'autre ; la case d'après demande l'une ou l'autre. */
        @Test
        void uneBifurcationFermeLAutreVoie() {
            Game game = GameTestSupport.atFork();
            assertEquals(List.of("dark_rich_fusion", "dark_fine_synthesis"), game.darkForkOptions("matiere").stream().map(DarkUpgrade::id).toList());
            assertTrue(game.canBuyDark("dark_rich_fusion") && game.canBuyDark("dark_fine_synthesis"));
            assertFalse(game.isDarkAvailable("dark_machines"));            // la case d'après attend une voie
            int before = game.elementsPerSynthesis();
            assertTrue(game.buyDark("dark_fine_synthesis"));
            assertEquals(before + 2, game.elementsPerSynthesis());   // deux éléments depuis la 0.070
            assertEquals("dark_fine_synthesis", game.darkForkRival("dark_rich_fusion").id());
            assertFalse(game.isDarkAvailable("dark_rich_fusion"));
            assertFalse(game.buyDark("dark_rich_fusion"));                  // l'autre voie est fermée
            assertTrue(game.isDarkAvailable("dark_machines"));              // l'une des deux suffit
        }

        @Test
        void uneLiaisonSOuvreParLUneOuLAutreDeSesVoies() {
            Game game = gameWithDarkMatter(20);
            DarkUpgrade link = DarkUpgrades.DEFAULT.stream().filter(d -> d.id().equals("link_nuclei")).findFirst().orElseThrow();
            assertTrue(link.isLink() && link.inTree() && !link.branch().inTree());
            assertEquals(List.of("dark_burst", "dark_rich_fusion"), link.linkedIds());
            assertFalse(game.isDarkAvailable("link_nuclei"));                // aucune des deux voies
            game.state().setDarkLevel("dark_rich_fusion", 1);               // la voie de gauche des atomes
            assertTrue(game.canBuyDark("link_nuclei"));
            double before = game.darkAtomsPerFusion();
            assertTrue(game.buyDark("link_nuclei"));
            assertEquals(before + 4, game.darkAtomsPerFusion(), 1e-9);
            assertValue(14, game.state().darkMatter());                     // payée en matière noire
            assertFalse(game.darkUpgrades(DarkUpgrade.Branch.DARK_MATTER).contains(link));
            assertEquals(10, game.darkLinks().size());
            Game other = gameWithDarkMatter(20);
            other.state().setDarkLevel("dark_burst", 1);                    // ou la voie de droite des particules
            assertTrue(other.canBuyDark("link_nuclei"));
            assertFalse(other.isDarkAvailable("link_flow"));                // celle de gauche n'a qu'une voie
        }

        @Test
        void unAccordDemandeSesTroisVoies() {
            Game game = gameWithDarkMatter(30);
            assertEquals(Game.Style.PRESENCE, game.darkStyle(DarkUpgrades.DEFAULT.stream().filter(d -> d.id().equals("dark_flow")).findFirst().orElseThrow()));
            assertEquals(Game.Style.ABSENCE, game.darkStyle(DarkUpgrades.DEFAULT.stream().filter(d -> d.id().equals("dark_burst")).findFirst().orElseThrow()));
            game.state().setDarkLevel("dark_flow", 1);
            game.state().setDarkLevel("dark_rich_fusion", 1);
            assertFalse(game.isDarkAvailable("accord_presence"));            // il en manque une
            game.state().setDarkLevel("dark_steady", 1);
            assertTrue(game.canBuyDark("accord_presence"));
            assertFalse(game.isDarkAvailable("accord_absence"));
            assertEquals(3, game.darkPathsOwned(Game.Style.PRESENCE));
        }

        @Test
        void lesVoiesDAbsenceRendentDuTempsDAbsence() {
            Game game = gameWithDarkMatter(30);
            double before = game.offlineRate();
            game.state().setDarkLevel("dark_burst", 1);
            game.state().setDarkLevel("dark_fine_synthesis", 1);
            game.state().setDarkLevel("dark_echo", 1);
            assertEquals(before + 3 * Game.ABSENCE_PATH_BONUS, game.offlineRate(), 1e-9);
            game.state().setDarkLevel("dark_flow", 1);                      // une voie de présence n'y change rien
            assertEquals(before + 3 * Game.ABSENCE_PATH_BONUS, game.offlineRate(), 1e-9);
            for (String id : List.of("dark_shadow_echo", "dark_double_synthesis", "dark_harvest")) game.state().setDarkLevel(id, 1);
            game.state().setDarkMatter(BigNum.of(60));
            assertTrue(game.buyDark("accord_absence_2"));
            assertEquals(before + 6 * Game.ABSENCE_PATH_BONUS + 0.10, game.offlineRate(), 1e-9);
        }

        @Test
        void lAccordDAbsenceTenteLesDefisPendantLAbsence() {
            Game game = gameWithDarkMatter(30);
            game.start();
            game.tick(0.1);
            game.state().stats().restartRun();
            game.tickAway(0.1);
            assertEquals(null, game.activeChallenge());                    // sans l'accord, rien
            for (String id : List.of("dark_burst", "dark_fine_synthesis", "dark_echo", "accord_absence")) game.state().setDarkLevel(id, 1);
            game.tick(0.1);
            assertEquals(null, game.activeChallenge());                    // jeu ouvert, rien non plus
            assertTrue(game.takeAwayChallenges().isEmpty(), "rien à annoncer");
            game.tickAway(0.1);
            assertEquals("challenge_rust", game.activeChallenge().id());  // le premier défi ouvert
            // Le journal d'absence le note une fois, par son nom, puis l'oublie.
            assertEquals(List.of(game.activeChallenge().name()), game.takeAwayChallenges());
            assertTrue(game.takeAwayChallenges().isEmpty(), "lu une fois, le journal est vide");
            game.tickAway(0.1);
            assertTrue(game.takeAwayChallenges().isEmpty(), "un défi déjà en cours : l'accord n'en lance pas d'autre");
        }

        @Test
        void leDefiPresenceNeLaisseAgirQueLesVoiesDePresence() {
            Game game = gameWithDarkMatter(400);
            game.state().setDarkLevel("dark_multisynthesis", 2);            // case ordinaire : +2 éléments
            game.state().setDarkLevel("dark_fine_synthesis", 1);            // voie d'absence : +2 éléments
            game.state().setDarkLevel("dark_rich_fusion", 1);               // voie de présence : +25 atomes
            int with = game.elementsPerSynthesis();
            double atoms = game.darkAtomsPerFusion();
            assertTrue(game.startChallenge("challenge_presence"));
            assertEquals(with - 4, game.elementsPerSynthesis());              // tout s'éteint, sauf la présence (0.73)
            assertEquals(25, game.darkAtomsPerFusion(), 1e-9);
            assertValue(Game.STYLE_CHALLENGE_WEIGHT.toDouble(), game.tableWeight());   // et le tableau est lourd
            game.abandonChallenge();
            assertEquals(with, game.elementsPerSynthesis());
            assertEquals(atoms, game.darkAtomsPerFusion(), 1e-9);
            game.state().addCompletedChallenge("challenge_absence");        // récompense : les voies d'absence comptent double
            assertEquals(with + 2, game.elementsPerSynthesis());
            assertFalse(gameWithDarkMatter(299).isChallengeUnlocked("challenge_presence"));   // ouvert à 300 matières noires
        }

        @Test
        void enAbsenceUneBifurcationSansChoixPrendSaVoieDAbsence() {
            Game game = gameWithDarkMatter(12);
            for (String id : List.of("dark_nucleus", "dark_multisynthesis", "dark_luck")) game.state().setDarkLevel(id, 1);
            game.state().setAtoms(BigNum.of(5000));
            game.start();
            game.tick(0.1);
            assertEquals(0, game.darkLevelOf("dark_fine_synthesis"));          // jeu ouvert : rien
            game.tickAway(0.1);
            assertEquals(1, game.darkLevelOf("dark_fine_synthesis"));          // la voie d'absence
            assertEquals(0, game.darkLevelOf("dark_rich_fusion"));
            assertEquals(List.of("Synthèse fine"), game.takeAwayPaths());
            assertTrue(game.takeAwayPaths().isEmpty());                          // dit une seule fois
        }

        @Test
        void lesVoiesDePresenceRenforcentLesPaliersDeVitesse() {
            Game game = gameWithDarkMatter(12);
            assertEquals(0, game.darkMilestoneExtra(), 1e-9);
            game.state().setDarkLevel("dark_flow", 1);                          // voie de présence du tempo : +0,5
            assertEquals(0.5, game.darkMilestoneExtra(), 1e-9);
            game.state().setDarkLevel("dark_impetus", 1);                       // et la Poussée : +1
            assertEquals(1.5, game.darkMilestoneExtra(), 1e-9);
            game.state().setDarkLevel("dark_shadow_echo", 1);                   // l'Écho sombre, voie d'absence : moins, +0,3
            assertEquals(1.8, game.darkMilestoneExtra(), 1e-9);
        }

        @Test
        void laSuperpositionAttendLeCinquiemeBigBang() {
            Game game = gameWithDarkMatter(12);
            game.state().setColliderUpgradeLevel(idle.core.collider.ColliderUpgrade.FLAVOR_TOP, 1);
            GameTestSupport.bangs(game, Game.SUPERPOSITION_BIG_BANGS - 1);
            assertFalse(game.isColliderUpgradeOpen(idle.core.collider.ColliderUpgrade.SUPERPOSITION));
            GameTestSupport.bangs(game, Game.SUPERPOSITION_BIG_BANGS);
            assertEquals(game.isColliderUpgradesUnlocked(), game.isColliderUpgradeOpen(idle.core.collider.ColliderUpgrade.SUPERPOSITION));
        }

        @Test
        void laSuperpositionOuvreLesDeuxVoies() {
            Game game = GameTestSupport.atFork();
            assertTrue(game.buyDark("dark_fine_synthesis"));
            assertFalse(game.canBuyDark("dark_rich_fusion"));
            assertFalse(game.hasSuperposition());
            game.state().setColliderUpgradeLevel(idle.core.collider.ColliderUpgrade.SUPERPOSITION, 1);
            assertTrue(game.hasSuperposition());
            assertTrue(game.darkForkRival("dark_rich_fusion") == null);
            assertTrue(game.buyDark("dark_rich_fusion"));                     // les deux voies
            assertEquals(1, game.darkLevelOf("dark_fine_synthesis"));
            assertEquals(idle.core.collider.ColliderUpgrade.FLAVOR_TOP, idle.core.collider.ColliderUpgrade.SUPERPOSITION.requires());
        }

        @Test
        void elleDemarreALaTailleDUnProton() {
            Game game = newGame();
            assertValue(1e-15, game.state().darkMatterSize());
            assertEquals("proton", SizeScale.reached(game.state().darkMatterSize()).name());
        }

        @Test
        void sansMatiereNoireRienNeGrossit() {
            Game game = newGame();
            assertFalse(game.growDarkMatter(10));
            assertValue(START, game.state().darkMatterSize());
            assertValue(1, game.darkMatterGrowthPerSecond());
        }

        @Test
        void enDebutDePartieUneSecondeDAppuiAjouteTroisPourMille() {
            Game game = gameWithDarkMatter(1);
            assertEquals(0.003, UNIT);
            assertEquals(1, game.darkMatterProgressFactor()); // moins d'une particule par seconde, aucun atome
            assertEquals(UNIT, game.darkMatterMomentum(), 1e-12);
            assertValue(1, game.darkMatterResistance());      // taille d'un proton : aucune résistance
            assertValue(Math.exp(UNIT), game.darkMatterGrowthPerSecond());
            assertTrue(game.growDarkMatter(1));
            // La résistance gagne 0,1 × l'élan par seconde ; la taille est sa puissance dix.
            assertValue(START * Math.pow(1 + 0.1 * UNIT, 10), game.state().darkMatterSize());
            game.growDarkMatter(1);
            assertValue(START * Math.pow(1 + 0.2 * UNIT, 10), game.state().darkMatterSize());
        }

        @Test
        void plusElleEstGrandePlusElleGrossitLentement() {
            Game game = gameWithDarkMatter(1);
            game.state().setDarkMatterSize(Game.DARK_MATTER_START_SIZE.multiply(1000));
            assertEquals(1.995, game.darkMatterResistance().toDouble(), 0.001);   // ×1 000 de taille : deux fois plus lent
            // Le premier palier de taille (le noyau d'uranium) double les particules : l'avancement en tient compte.
            assertValue(Math.exp(UNIT * game.darkMatterProgressFactor() / Math.pow(1000, 0.1)), game.darkMatterGrowthPerSecond());
            game.state().setDarkMatterSize(Game.DARK_MATTER_START_SIZE.multiply(1e30));
            assertValue(1000, game.darkMatterResistance());   // au million de milliards de mètres : mille fois plus lent
        }

        @Test
        void doublerLElanOuLeTempsDAppuiRevientAuMeme() {
            Game slow = gameWithDarkMatter(1);
            slow.growDarkMatter(2000);
            Game fast = gameWithDarkMatter(2);
            fast.growDarkMatter(1000);
            assertEquals(slow.state().darkMatterSize().log10(), fast.state().darkMatterSize().log10(), 1e-9);
        }

        @Test
        void laCroissanceSuitLaProductionDeParticules() {
            Game game = gameWithDarkMatter(1);
            game.state().setLevel("gen", 2);                  // trois générateurs : 0,75 particule par seconde
            assertEquals(1, game.darkMatterProgressFactor());
            game.state().setLevel("huge", 1);                 // vitesse ×1e30 : 7,5e29 particules par seconde
            assertEquals(1 + Math.log10(7.5e29), game.darkMatterProgressFactor(), 1e-9);
            assertEquals(UNIT * (1 + Math.log10(7.5e29)), game.darkMatterMomentum(), 1e-12);
        }

        @Test
        void laCroissanceSuitLesAtomesCrees() {
            Game game = gameWithDarkMatter(1);
            game.state().setTotalAtoms(BigNum.of(1000));
            assertEquals(4, game.darkMatterProgressFactor(), 1e-9);   // 1 + 3 ordres de grandeur
            assertEquals(4 * UNIT, game.darkMatterMomentum(), 1e-12);
            game.state().setAtoms(BigNum.ZERO);               // dépenser ses atomes ne ralentit rien
            assertEquals(4, game.darkMatterProgressFactor(), 1e-9);
        }

        @Test
        void laCroissanceRepartDeZeroApresUneExplosion() {
            Game game = gameWithDarkMatter(1);
            game.state().setTotalAtoms(BigNum.of(1000));
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.darkMatterProgressFactor() > 4);  // les éléments gonflent la production
            game.explode();
            assertEquals(1, game.darkMatterProgressFactor()); // plus d'atomes, production de départ
        }

        @Test
        void laTailleSeLitAussiEnAnneesLumiere() {
            Game game = gameWithDarkMatter(1);
            game.state().setDarkMatterSize(BigNum.of(9.46, 18));
            assertValue(1000, game.darkMatterLightYears());
            assertValue(9.46e15, SizeScale.LIGHT_YEAR);
        }

        @Test
        void beaucoupDePetitsAppuisValentUnLong() {
            Game longPress = gameWithDarkMatter(1);
            longPress.growDarkMatter(3);
            Game frames = gameWithDarkMatter(1);
            for (int i = 0; i < 180; i++) frames.growDarkMatter(1.0 / 60);
            assertEquals(longPress.state().darkMatterSize().toDouble(), frames.state().darkMatterSize().toDouble(),
                    START * 1e-9);                            // le calcul est exact : même résultat
        }

        @Test
        void plusIlYADeMatiereNoirePlusElleGrossitVite() {
            Game one = gameWithDarkMatter(1);
            Game four = gameWithDarkMatter(4);
            assertEquals(4 * UNIT, four.darkMatterMomentum(), 1e-12);
            one.growDarkMatter(1);
            four.growDarkMatter(1);
            assertValue(START * Math.pow(1 + 0.1 * 4 * UNIT, 10), four.state().darkMatterSize());
            assertTrue(four.state().darkMatterSize().gt(one.state().darkMatterSize()));
        }

        @Test
        void sansAppuiLaTailleNeBougePas() {
            Game game = gameWithDarkMatter(2);
            game.growDarkMatter(5);
            BigNum size = game.state().darkMatterSize();
            game.tick(3600);                                  // le temps passe, personne n'appuie
            assertFalse(game.growDarkMatter(0));
            assertEquals(size, game.state().darkMatterSize());
        }

        @Test
        void elleDepasseLUniversSansProbleme() {
            Game game = gameWithDarkMatter(1_000_000);        // un élan de 3 000
            game.growDarkMatter(100);
            assertEquals(10 * Math.log10(1 + 0.1 * UNIT * 1e6 * 100) - 15, game.state().darkMatterSize().log10(), 1e-6);
            assertEquals("univers observable", SizeScale.reached(game.state().darkMatterSize()).name());
            assertEquals(null, SizeScale.next(game.state().darkMatterSize()));
        }

        @Test
        void lExplosionNeLaRemetPasAZero() {
            Game game = gameWithDarkMatter(1);
            game.growDarkMatter(10);
            BigNum size = game.state().darkMatterSize();
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertEquals(size, game.state().darkMatterSize());
            assertEquals(2 * UNIT, game.darkMatterMomentum(), 1e-12); // deux matières noires maintenant
        }

        @Test
        void dureeInvalideRefusee() {
            Game game = gameWithDarkMatter(1);
            assertThrows(IllegalArgumentException.class, () -> game.growDarkMatter(-1));
            assertThrows(IllegalArgumentException.class, () -> game.growDarkMatter(Double.NaN));
            assertThrows(IllegalArgumentException.class, () -> game.state().setDarkMatterSize(BigNum.ZERO));
        }

        @Test
        void lEchelleDesGrandeursEstRangeeDuPlusPetitAuPlusGrand() {
            List<Landmark> landmarks = SizeScale.LANDMARKS;
            assertEquals(Game.DARK_MATTER_START_SIZE, landmarks.get(0).size());
            for (int i = 1; i < landmarks.size(); i++) {
                Landmark previous = landmarks.get(i - 1);
                Landmark current = landmarks.get(i);
                assertTrue(current.size().gt(previous.size()), () -> current.name() + " après " + previous.name());
            }
            assertEquals(landmarks.size(), landmarks.stream().map(Landmark::name).distinct().count());
        }

        @Test
        void repereAtteintEtRepereSuivant() {
            assertEquals(null, SizeScale.reached(BigNum.of(1, -20)));
            assertEquals("proton", SizeScale.next(BigNum.of(1, -20)).name());
            BigNum threeNanometers = BigNum.of(3, -9);
            assertEquals("hélice d'ADN", SizeScale.reached(threeNanometers).name());
            assertEquals("virus", SizeScale.next(threeNanometers).name());
            assertEquals("être humain", SizeScale.reached(BigNum.of(1.7)).name());   // pile sur un repère : atteint
            assertEquals("baleine bleue", SizeScale.next(BigNum.of(1.7)).name());
            assertThrows(IllegalArgumentException.class, () -> new Landmark("rien", BigNum.ZERO));
            assertThrows(IllegalArgumentException.class, () -> new Landmark(" ", BigNum.ONE));
        }
    }
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.elements.Element;
import idle.core.elements.ElementCategory;
import idle.core.elements.ElementEffect;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.upgrades.Upgrade;
import idle.core.upgrades.Upgrades;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : les défis, les succès et la simulation d'une première partie.
 */
class JeuDefisTest extends GameTestSupport {


    @Nested
    class Defis {

        /** Partie après quelques explosions : {@code darkMatter} matières noires gagnées, tableau débloqué. */
        private Game darkGame(double darkMatter) {
            Game game = newGame();
            unlock(game);
            game.state().setDarkMatter(BigNum.of(darkMatter));
            game.state().setExplosions(3);
            return game;
        }

        private void fillTable(Game game) {
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
        }

        @Test
        void ilYADixDefisChacunAvecSaContrainteEtSaRecompense() {
            Game game = darkGame(100);
            assertEquals(10, game.challenges().size());     // quatre de plus en 0.070
            assertEquals(10, game.challenges().stream().map(Challenge::id).distinct().count());
            assertEquals(10, game.challenges().stream().map(Challenge::rule).distinct().count());
            assertEquals(10, game.challenges().stream().map(Challenge::reward).distinct().count());
            int previous = 0;
            for (Challenge challenge : game.challenges()) {    // du plus accessible au plus exigeant
                assertTrue(challenge.darkMatter() >= previous);
                previous = challenge.darkMatter();
            }
            assertEquals(null, game.activeChallenge());
            assertEquals(0, game.completedChallenges());
        }

        @Test
        void unDefiDemandeAssezDeMatiereNoireGagnee() {
            Game game = darkGame(2);
            assertFalse(game.isChallengeUnlocked("challenge_rust"));     // il en faut 3
            assertFalse(game.startChallenge("challenge_rust"));
            game.state().setDarkMatter(BigNum.of(2));
            game.state().setDarkMatterSpent(BigNum.of(1));     // dépensée, mais gagnée
            assertTrue(game.isChallengeUnlocked("challenge_rust"));
            assertFalse(game.isChallengeUnlocked("challenge_decay"));
        }

        @Test
        void pasDeDefiAvantLaPremiereExplosion() {
            Game game = newGame();
            game.state().setDarkMatter(BigNum.of(50));         // sans explosion, la matière noire n'existe pas encore
            assertFalse(game.isChallengeUnlocked("challenge_rust"));
        }

        @Test
        void commencerUnDefiEffaceLaPartieSansRienRapporter() {
            Game game = darkGame(30);
            game.state().setParticles(BigNum.of(500));
            game.state().setElementCount(26, 3);
            assertTrue(game.startChallenge("challenge_rust"));
            assertEquals("challenge_rust", game.activeChallenge().id());
            assertEquals(BigNum.ZERO, game.state().particles());
            assertEquals(BigNum.ZERO, game.state().atoms());
            assertEquals(0, game.discoveredElements());
            assertEquals(0, game.levelOf("keep"));
            assertValue(30, game.state().darkMatter());        // ni gagnée ni perdue
            assertEquals(3, game.state().explosions());
            assertEquals(0, game.stats().runTime(), 0);
        }

        @Test
        void unDefiSeJoueSansLesMemoiresDeLaMatiereNoire() {
            Game game = darkGame(30);
            game.state().setDarkLevel("dark_seeds", 2);        // deux générateurs offerts
            game.state().setDarkLevel("dark_priming", 1);      // dix niveaux de vitesse offerts
            game.state().setDarkLevel("dark_machines", 1);     // les automatismes survivent à l'explosion
            game.state().setDarkLevel("dark_relics", 1);       // les éléments uniques aussi
            game.state().setDarkLevel("dark_shop_start", 1);   // et le départ est lancé
            game.state().setDarkLevel("dark_density", 3);      // particules ×8 : un multiplicateur, lui, reste
            game.state().setElementCount(2, 1);
            game.setDarkAutomationEnabled("dark_auto_start", true);
            assertEquals(3, game.generatorCount());
            assertTrue(game.ownsAutomation("autoSlow"));

            assertTrue(game.startChallenge("challenge_rust"));
            assertEquals(1, game.generatorCount());
            assertEquals(0, game.startingSpeedLevels());
            assertFalse(game.ownsAutomation("autoSlow"));
            assertEquals(0, game.discoveredElements());
            assertEquals(BigNum.ZERO, game.state().totalAtoms());
            assertValue(8, game.particlesPerCreation());
            game.tick(30);
            // Depuis la 0.047, l'automatisme qui offre les automatismes agit aussi pendant un défi.
            assertTrue(game.ownsAutomation("autoSpeed"));

            game.abandonChallenge();                           // la partie ordinaire retrouve tout
            assertEquals(3, game.generatorCount());
            assertEquals(10, game.startingSpeedLevels());
            assertTrue(game.state().totalAtoms().sign() > 0);
            game.tick(2);
            assertTrue(game.ownsAutomation("autoSpeed"));
        }

        @Test
        void unSeulDefiALaFois() {
            Game game = darkGame(30);
            assertTrue(game.startChallenge("challenge_rust"));
            assertFalse(game.startChallenge("challenge_heavy"));
            assertEquals("challenge_rust", game.activeChallenge().id());
        }

        @Test
        void abandonnerEffaceLaPartieEtLeveLaContrainte() {
            Game game = darkGame(30);
            assertFalse(game.abandonChallenge());              // rien à abandonner
            game.startChallenge("challenge_heavy");
            game.state().setParticles(BigNum.of(500));
            assertTrue(game.abandonChallenge());
            assertEquals(null, game.activeChallenge());
            assertEquals(BigNum.ZERO, game.state().particles());
            assertFalse(game.isChallengeCompleted("challenge_heavy"));
            assertEquals(3, game.generatorsPerAtom());
        }

        @Test
        void lExplosionReussitLeDefiEtNoteSonTemps() {
            Game game = darkGame(30);
            game.startChallenge("challenge_heavy");
            game.tick(120);
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(null, game.activeChallenge());
            assertTrue(game.isChallengeCompleted("challenge_heavy"));
            assertEquals(1, game.completedChallenges());
            assertEquals(120, game.challengeBestTime("challenge_heavy"), 1e-6);
            assertValue(31, game.state().darkMatter());        // l'explosion rapporte comme d'habitude
            assertEquals(-1, game.challengeBestTime("challenge_rust"), 0);
        }

        @Test
        void unDefiSeJoueSurUnTableauDeMasseFixe() {
            Game game = darkGame(30);
            game.state().setTableWeightLevel(12);
            assertTrue(game.atomCap().gt(BigNum.of(118).multiply(Game.CHALLENGE_WEIGHT)));
            game.startChallenge("challenge_heavy");
            assertEquals(Game.CHALLENGE_WEIGHT.toDouble(), game.tableWeight().toDouble(), 1e-9);   // 5 000 depuis la 0.097
            assertValue(118 * 5000, game.atomCap());
            game.abandonChallenge();
            assertTrue(game.atomCap().gt(BigNum.of(118)));   // la partie ordinaire retrouve son poids
        }

        @Test
        void laPremiereReussiteAlourditLeTableauPasLesSuivantes() {
            Game game = darkGame(30);
            game.startChallenge("challenge_heavy");
            assertFalse(game.isChallengeReplay());
            assertValue(1, game.nextExplosionDarkMatter());
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(1, game.state().tableWeightLevel());
            assertValue(31, game.state().darkMatter());
            int explosions = game.state().explosions();

            game.startChallenge("challenge_heavy");           // rejoué : il ne compte que pour son temps
            assertTrue(game.isChallengeReplay());
            assertValue(0, game.nextExplosionDarkMatter());
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(1, game.state().tableWeightLevel());
            assertValue(31, game.state().darkMatter());
            assertEquals(explosions + 1, game.state().explosions());
            assertFalse(game.isChallengeReplay());            // le défi est terminé
        }

        @Test
        void rejouerUnDefiNeGardeQueLeMeilleurTemps() {
            Game game = darkGame(30);
            game.startChallenge("challenge_heavy");
            game.tick(120);
            fillTable(game);
            game.explode();
            game.startChallenge("challenge_heavy");
            game.tick(300);
            fillTable(game);
            game.explode();
            assertEquals(120, game.challengeBestTime("challenge_heavy"), 1e-6);
            game.startChallenge("challenge_heavy");
            game.tick(60);
            fillTable(game);
            game.explode();
            assertEquals(60, game.challengeBestTime("challenge_heavy"), 1e-6);
            assertEquals(1, game.completedChallenges());
        }

        @Test
        void uneExplosionOrdinaireNeReussitRien() {
            Game game = darkGame(30);
            fillTable(game);
            assertTrue(game.explode());
            assertEquals(0, game.completedChallenges());
        }

        @Test
        void laRemiseAZeroEffaceLesDefis() {
            Game game = darkGame(30);
            game.startChallenge("challenge_heavy");
            fillTable(game);
            game.explode();
            game.startChallenge("challenge_rust");
            game.reset();
            assertEquals(null, game.state().activeChallenge());
            assertEquals(0, game.state().completedChallenges().size());
        }

        // ----- Les contraintes -----

        @Test
        void rouilleRalentitTousLesAutomatismes() {
            Game game = darkGame(30);
            game.startChallenge("challenge_rust");
            game.state().setAtoms(BigNum.of(100));
            createEnoughAtoms(game);
            game.buyAutomation("autoSlow");                    // 4 secondes d'ordinaire
            assertEquals(4 * Game.RUSTY_FACTOR, game.automationInterval("autoSlow"), 1e-12);
            game.buyAutomation("autoSpeed");                   // au minimum d'ordinaire : une demi-seconde ici
            assertEquals(Game.RUSTY_MIN_INTERVAL, game.automationInterval("autoSpeed"), 1e-12);
            assertEquals(Game.RUSTY_FACTOR, game.darkAutomationInterval("dark_auto_start"), 1e-12);
            game.abandonChallenge();
            assertEquals(1, game.darkAutomationInterval("dark_auto_start"), 1e-12);
        }

        @Test
        void fusionLourdeDemandeDeuxFoisPlusDeGenerateurs() {
            Game game = darkGame(30);
            assertEquals(3, game.generatorsPerAtom());
            game.startChallenge("challenge_heavy");
            assertEquals(6, game.generatorsPerAtom());
            assertEquals(6, game.maxGeneratorCount());         // la limite suit
            game.state().setParticles(BigNum.of(1e30));
            assertEquals(2, game.buy("gen", 2));
            assertFalse(game.canFuse());                       // trois générateurs ne suffisent plus
            assertEquals(3, game.buy("gen", 10));
            assertEquals(6, game.generatorCount());
            assertTrue(game.canFuse());
            assertEquals(1, game.fusionGroups());
            assertTrue(game.fuse());
            assertValue(1, game.state().atoms());
        }

        @Test
        void particulesVolatilesFaitFondreLesParticules() {
            Game game = darkGame(30);
            game.startChallenge("challenge_volatile");
            game.state().setParticles(BigNum.of(1000));
            double before = total(game);
            game.tick(1);
            // La moitié de ce que le joueur possédait a disparu ; le peu créé entre-temps ne change presque rien.
            double created = game.productionPerSecond().toDouble();     // au plus, en une seconde
            assertEquals(500, game.state().particles().toDouble(), created);
            assertTrue(total(game) < before);
            game.tick(3);
            assertEquals(1000 * Math.pow(0.5, 4), game.state().particles().toDouble(), 2 * created);
        }

        @Test
        void particulesVolatilesLaisseUnEquilibreEntreCreationEtPerte() {
            Game game = darkGame(30);
            game.startChallenge("challenge_volatile");
            game.state().setLevel("huge", 1);                  // une production énorme et régulière
            double production = game.productionPerSecond().toDouble();
            for (int i = 0; i < 2000; i++) game.tick(0.1);
            // À l'équilibre, ce qui s'évapore chaque seconde vaut ce qui se crée : le stock vaut la production / taux.
            double rate = -Math.log(1 - Game.VOLATILE_LOSS_PER_SECOND);
            assertEquals(production / rate, game.state().particles().toDouble(), production / rate * 1e-6);
        }

        @Test
        void amnesieRemetAZeroLesAmeliorationsEnAtomesAChaqueFusion() {
            Game game = darkGame(30);
            game.startChallenge("challenge_amnesia");
            game.state().setAtoms(BigNum.of(100));
            createEnoughAtoms(game);
            game.buy("double");
            game.buy("keep");
            game.buyAutomation("autoSlow");
            game.state().setParticles(BigNum.of(1e30));
            game.buy("speed", 3);
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertEquals(0, game.levelOf("double"));
            assertEquals(0, game.levelOf("keep"));
            assertEquals(3, game.levelOf("speed"));            // Persistance a protégé la vitesse une dernière fois
            assertTrue(game.ownsAutomation("autoSlow"));       // les automatismes ne sont pas des améliorations
        }

        @Test
        void elementsInertesCoupeTousLesBonusDuTableau() {
            Game game = darkGame(30);
            game.startChallenge("challenge_inert");
            game.state().setElementCount(2, 1);                // l'hélium doublerait les particules
            game.state().setElementCount(1, 1);                // et la première période les triplerait
            assertEquals(1, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 0);
            assertValue(1, game.particlesPerCreation());
            assertEquals(2, game.discoveredElements());        // les éléments sont bien là, ils ne font rien
            assertTrue(game.isSynthesisAutomationUnlocked());  // et un élément unique reste un élément unique
            game.abandonChallenge();
            game.state().setElementCount(2, 1);
            assertEquals(2, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
        }

        @Test
        void desintegrationFaitDisparaitreLesExemplaires() {
            Game game = darkGame(30);
            game.startChallenge("challenge_decay");
            game.state().setElementCount(26, 9);
            game.state().setElementCount(29, 9);
            game.state().setElementCount(47, 7);               // 25 exemplaires : un demi-exemplaire perdu par seconde
            game.tick(1.9);
            assertEquals(25, game.ownedCopies());              // pas encore un exemplaire entier
            game.tick(0.2);
            assertEquals(24, game.ownedCopies());
            game.tick(100);                                    // un seul très long tick
            assertTrue(game.ownedCopies() <= 4);               // 25 × e^(−0,02 × 100) ≈ 3,4… à un ou deux près
        }

        @Test
        void sansDefiRienNeSeDesintegre() {
            Game game = darkGame(30);
            game.state().setElementCount(26, 9);
            game.tick(10_000);
            assertEquals(9, game.elementCount(26));
        }

        // ----- Les récompenses -----

        private Game rewarded(String challengeId) {
            Game game = darkGame(100);
            game.state().addCompletedChallenge(challengeId);
            return game;
        }

        @Test
        void rouilleLaisseDesAutomatismesPlusVifs() {
            Game game = rewarded("challenge_rust");
            game.state().setAtoms(BigNum.of(100));
            game.buyAutomation("autoSlow");
            game.state().setAutomationSpeedLevel("autoSlow", 0);
            assertEquals(4 / Game.REWARD_AUTOMATION_DIVISOR, game.automationInterval("autoSlow"), 1e-12);
        }

        @Test
        void fusionLourdeDoubleLaPrimeDeGroupe() {
            Game game = rewarded("challenge_heavy");
            assertEquals(Game.REWARD_GROUP_BONUS, game.fusionGroupBonusPerGroup(), 0);
            game.state().setDarkLevel("dark_swarm", 1);
            game.state().setLevel("gen", 5);                   // six générateurs : deux groupes
            assertEquals(1.2, game.fusionGroupBonus(), 1e-12);
        }

        @Test
        void particulesVolatilesRenforceLesPaliersDeVitesse() {
            Game plain = new Game();
            plain.start();
            plain.state().setLevel("speed", 50);
            assertValue(4, plain.speedMilestoneMultiplier());   // deux paliers à ×2
            assertEquals(2, plain.speedMilestoneFactor("speed"), 0);

            Game game = new Game();
            game.start();
            game.state().addCompletedChallenge("challenge_volatile");
            game.state().setLevel("speed", 50);
            assertEquals(3, game.speedMilestoneFactor("speed"), 0);
            assertValue(9, game.speedMilestoneMultiplier());    // deux paliers à ×3
        }

        @Test
        void amnesieDiviseParDeuxLePrixDesAmeliorationsEnAtomes() {
            Game game = rewarded("challenge_amnesia");
            assertValue(1, game.costOf("mass"));               // 2 atomes d'ordinaire
            game.state().setLevel("double", 3);
            assertValue(14, game.costOf("double"));            // 27 d'ordinaire, arrondi au-dessus
            assertValue(10, game.costOf("speed"));             // les particules ne sont pas concernées
        }

        @Test
        void elementsInertesRenforceTousLesElements() {
            Game game = rewarded("challenge_inert");
            game.state().setElementCount(2, 1);                // l'hélium : ×2 d'ordinaire
            assertEquals(Math.pow(2, Game.REWARD_ELEMENT_STRENGTH), game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-12);
        }

        @Test
        void desintegrationDonneUnElementDePlusParSynthese() {
            Game game = rewarded("challenge_decay");
            assertEquals(2, game.elementsPerSynthesis());
            game.state().setAtoms(BigNum.of(100));
            assertTrue(game.synthesize().size() >= 2);
        }

        @Test
        void unDefiMalDefiniEstRefuse() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Challenge("", "X", Challenge.Rule.DECAY, 1, Challenge.Reward.EXTRA_ELEMENT));
            assertThrows(IllegalArgumentException.class,
                    () -> new Challenge("x", "X", Challenge.Rule.DECAY, 0, Challenge.Reward.EXTRA_ELEMENT));
            assertThrows(IllegalArgumentException.class, () -> newGame().isChallengeUnlocked("inconnu"));
        }
    }

    @Nested
    class Succes {

        /** Partie de test (vitesse à 10, générateur à 100 puis 1000) avec le vrai catalogue de succès. */
        private Game gameWithAchievements() {
            Game game = new Game(new GameState(),
                    List.of(SPEED, GENERATOR, HUGE, DOUBLE, MASS, PATIENCE, BOOST, DISCOUNT, KEEP),
                    List.of(AUTO_SPEED, AUTO_GEN, AUTO_FUSION, AUTO_SLOW, AUTO_SYNTH),
                    DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(42));
            game.start();
            return game;
        }

        @Test
        void leCatalogueCompteCinquanteSuccesDontAuMoinsVingtAvecUnBonusPropre() {
            assertEquals(67, Achievements.DEFAULT.size());
            assertEquals(67, Achievements.DEFAULT.stream().map(Achievement::id).distinct().count());
            assertEquals(67, Achievements.DEFAULT.stream().map(Achievement::name).distinct().count());
            long withBonus = Achievements.DEFAULT.stream().filter(Achievement::hasBonus).count();
            assertTrue(withBonus >= 20, "succès d'action : " + withBonus);
            for (Achievement achievement : Achievements.DEFAULT) {
                assertFalse(achievement.description().isBlank());
                assertTrue(achievement.summary().length() <= 30, achievement.summary());   // un résumé tient sur une tuile
                if (achievement.hasBonus()) assertTrue(achievement.amount() > 0 && achievement.amount() <= 0.05);   // de petits bonus
            }
        }

        @Test
        void unePartieSansCatalogueNAAucunSucces() {
            Game game = gameWithParticles(1e9);
            game.buy("gen", 2);
            game.tick(5);
            assertTrue(game.achievements().isEmpty());
            assertEquals(0, game.achievementCount());
            assertEquals(BigNum.ONE, game.achievementParticlesMultiplier());
            assertValue(1, game.particlesPerCreation());
        }

        @Test
        void lesSuccesDEtapeTombentTousSeuls() {
            Game game = gameWithAchievements();
            assertEquals(0, game.achievementCount());
            game.tick(FIRST_CHECK);                            // une particule, et plus d'une seconde de jeu
            assertTrue(game.hasAchievement("step_first_particle"));
            assertFalse(game.hasAchievement("step_all_generators"));
            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            game.tick(1);
            assertTrue(game.hasAchievement("step_all_generators"));
            assertTrue(game.fuse());
            game.tick(1);
            assertTrue(game.hasAchievement("step_first_atom"));
        }

        @Test
        void chaqueSuccesAjouteUnPourCentDeParticules() {
            Game game = gameWithAchievements();
            game.state().addAchievement("step_first_particle");
            game.state().addAchievement("step_first_atom");
            assertEquals(2, game.achievementCount());
            assertValue(1 + 2 * Game.ACHIEVEMENT_PARTICLES, game.achievementParticlesMultiplier());
            assertValue(1.02, game.particlesPerCreation());
        }

        @Test
        void unSuccesInconnuDuCatalogueNeComptePas() {
            Game game = gameWithAchievements();
            game.state().addAchievement("vieux_succes_supprime");
            assertEquals(0, game.achievementCount());
            assertValue(1, game.particlesPerCreation());
        }

        @Test
        void lesBonusPropresSAdditionnentParSorte() {
            Game game = gameWithAchievements();
            game.state().addAchievement(Achievements.EXACT_CHANGE);      // atomes +0,5 %
            game.state().addAchievement("feat_full_atoms");              // atomes +0,5 %
            game.state().addAchievement(Achievements.BARE_FUSION);       // particules +2 %
            assertEquals(0.01, game.achievementBonus(Achievement.Bonus.ATOMS), 1e-12);
            assertEquals(0.02, game.achievementBonus(Achievement.Bonus.PARTICLES), 1e-12);
            assertEquals(0, game.achievementBonus(Achievement.Bonus.EXPANSION), 0);
            assertValue(1.01, game.atomsPerFusion());
            assertValue((1 + 3 * Game.ACHIEVEMENT_PARTICLES) * 1.02, game.particlesPerCreation());
        }

        @Test
        void lesBonusAgissentLaOuIlsLeDisent() {
            Game game = gameWithAchievements();
            unlock(game);
            double interval = game.automationInterval("autoSlow");
            BigNum generator = game.costOf("gen");
            game.state().addAchievement(Achievements.FAST_FUSION);       // cadence +1 %
            game.state().addAchievement(Achievements.BULK_PURCHASE);     // générateurs −2 %
            game.state().addAchievement(Achievements.BUY_EVERYTHING);    // générateurs −2 %
            assertEquals(interval / 1.01, game.automationInterval("autoSlow"), 1e-12);
            assertValue(generator.toDouble() * 0.96, game.costOf("gen"));
        }

        @Test
        void lesNouveauxSuccesSontAnnoncesUneSeuleFois() {
            Game game = gameWithAchievements();
            game.tick(FIRST_CHECK);
            List<Achievement> fresh = game.takeNewAchievements();
            assertEquals(1, fresh.size());
            assertEquals("step_first_particle", fresh.get(0).id());
            assertTrue(game.takeNewAchievements().isEmpty());
            game.tick(5);
            assertTrue(game.takeNewAchievements().isEmpty()); // déjà obtenu : pas de seconde annonce
        }

        @Test
        void lExplosionGardeLesSuccesMaisPasLaRemiseAZero() {
            Game game = gameWithAchievements();
            unlock(game);
            game.tick(FIRST_CHECK);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertTrue(game.hasAchievement("step_first_particle"));
            game.reset();
            assertEquals(0, game.state().achievements().size());
        }

        // ----- Les actions -----

        @Test
        void fusionnerSansVitesseNiCouplage() {
            Game game = gameWithAchievements();
            game.state().setParticles(BigNum.of(1e9));
            game.buy("speed");
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertFalse(game.hasAchievement(Achievements.BARE_FUSION));   // un niveau de vitesse : raté
            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            assertTrue(game.fuse());
            assertTrue(game.hasAchievement(Achievements.BARE_FUSION));
        }

        @Test
        void fusionnerApresUneLongueAttenteOuTresVite() {
            Game game = gameWithAchievements();
            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            game.tick(10);
            assertTrue(game.fuse());                           // la première fusion de la partie n'est pas un sprint
            assertFalse(game.hasAchievement(Achievements.FAST_FUSION));
            assertFalse(game.hasAchievement(Achievements.PATIENT_FUSION));

            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            game.tick(5);
            assertTrue(game.fuse());
            assertTrue(game.hasAchievement(Achievements.FAST_FUSION));

            game.state().setParticles(BigNum.of(1e9));
            game.buy("gen", 2);
            game.tick(Achievements.PATIENT_SECONDS + 1);
            assertTrue(game.fuse());
            assertTrue(game.hasAchievement(Achievements.PATIENT_FUSION));
        }

        @Test
        void acheterEnGrosEtToutAcheter() {
            Game game = gameWithAchievements();
            game.state().setParticles(BigNum.of(1e30));
            assertEquals(24, game.buy("speed", 24));
            assertFalse(game.hasAchievement(Achievements.BULK_PURCHASE));
            assertEquals(25, game.buy("speed", 25));
            assertTrue(game.hasAchievement(Achievements.BULK_PURCHASE));
            assertFalse(game.hasAchievement(Achievements.BUY_EVERYTHING));
            assertTrue(game.buyAllWithParticles() >= Achievements.BUY_EVERYTHING_LEVELS);
            assertTrue(game.hasAchievement(Achievements.BUY_EVERYTHING));
        }

        @Test
        void payerExactementCeQuOnA() {
            Game game = gameWithAchievements();
            game.state().setLevel("double", 3);                // le niveau suivant coûte 27 atomes
            game.state().setAtoms(BigNum.of(28));
            game.buy("double");
            assertFalse(game.hasAchievement(Achievements.EXACT_CHANGE));   // il reste un atome
            game.state().setLevel("double", 3);
            game.state().setAtoms(BigNum.of(27));
            game.buy("double");
            assertTrue(game.hasAchievement(Achievements.EXACT_CHANGE));
        }

        @Test
        void unPetitAchatTombantJusteNeComptePas() {
            Game game = gameWithAchievements();
            game.state().setAtoms(BigNum.of(1));
            game.buy("double");                                // 1 atome : trop facile
            assertFalse(game.hasAchievement(Achievements.EXACT_CHANGE));
        }

        @Test
        void lesSuccesDeLaSynthese() {
            Game game = gameWithAchievements();
            unlock(game);
            game.state().setAtoms(Game.MAX_ATOMS);
            game.state().setDarkLevel("dark_shop_target", 1);
            game.setSynthesisTarget(ElementCategory.TRANSITION_METAL);
            for (int i = 0; i < 3; i++) {
                game.state().setAtoms(Game.MAX_ATOMS);
                game.synthesize();
            }
            assertTrue(game.hasAchievement(Achievements.TARGETED));
            assertFalse(game.hasAchievement(Achievements.TARGETED_ACTINIDE));
            game.state().setElementCount(92, 1);               // un élément unique : la garantie ne s'en mêle plus
            game.setSynthesisTarget(ElementCategory.ACTINIDE);
            for (int i = 0; i < 10; i++) {
                game.state().setAtoms(Game.MAX_ATOMS);
                game.synthesize();
            }
            assertTrue(game.hasAchievement(Achievements.TARGETED_ACTINIDE));

            game.setSynthesisTarget(null);
            game.state().setElementCount(35, 10_000);          // tirage double assuré
            game.state().setAtoms(Game.MAX_ATOMS);
            assertEquals(2, game.synthesize().size());
            assertTrue(game.hasAchievement(Achievements.DOUBLE_DRAW));
            assertFalse(game.hasAchievement(Achievements.TRIPLE_DRAW));
            game.state().setDarkLevel("dark_multisynthesis", 1);
            game.state().setAtoms(Game.MAX_ATOMS);
            assertEquals(3, game.synthesize().size());
            assertTrue(game.hasAchievement(Achievements.TRIPLE_DRAW));
        }

        @Test
        void lesSuccesDesElements() {
            Game game = gameWithAchievements();
            unlock(game);
            game.state().setElementCount(79, 1);               // l'or
            game.state().setElementCount(1, 1);
            game.state().setElementCount(8, 1);                // hydrogène et oxygène
            game.state().setElementCount(11, 1);               // du sodium, mais pas de chlore
            game.state().setElementCount(26, 8);
            game.tick(1);
            assertTrue(game.hasAchievement("feat_gold"));
            assertTrue(game.hasAchievement("feat_water"));
            assertFalse(game.hasAchievement("feat_salt"));
            assertFalse(game.hasAchievement("feat_nine_copies"));
            game.state().setElementCount(17, 1);
            game.state().setElementCount(26, 9);
            game.tick(1);
            assertTrue(game.hasAchievement("feat_salt"));
            assertTrue(game.hasAchievement("feat_nine_copies"));
        }

        @Test
        void lesSuccesDeLaMatiereNoire() {
            Game game = gameWithAchievements();
            unlock(game);
            game.state().setDarkMatter(BigNum.of(5));
            game.state().setExplosions(2);
            game.state().setDarkLevel("dark_lock", 1);
            game.setHoldLocked(true);
            assertTrue(game.hasAchievement(Achievements.HOLD_LOCK));

            game.startChallenge("challenge_rust");
            game.abandonChallenge();
            assertTrue(game.hasAchievement(Achievements.ABANDON));

            game.tick(30);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());                        // trente secondes après le début de la partie
            assertTrue(game.hasAchievement(Achievements.FAST_EXPLOSION));
        }

        @Test
        void laToutePremiereExplosionNEstPasUneExplosionRapide() {
            Game game = gameWithAchievements();
            unlock(game);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertFalse(game.hasAchievement(Achievements.FAST_EXPLOSION));
        }

        @Test
        void unSuccesMalDefiniEstRefuse() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Achievement("", "X", "x", "x", Achievement.Bonus.NONE, 0, null));
            assertThrows(IllegalArgumentException.class,
                    () -> new Achievement("x", "X", "", "x", Achievement.Bonus.NONE, 0, null));        // sans résumé
            assertThrows(IllegalArgumentException.class,
                    () -> new Achievement("x", "X", "x", "x", Achievement.Bonus.ATOMS, 0, null));    // un bonus sans taille
            assertThrows(IllegalArgumentException.class,
                    () -> new Achievement("x", "X", "x", "x", Achievement.Bonus.NONE, 0.1, null));   // une taille sans bonus
            Achievement twice = Achievement.step("x", "X", "x", "x", game -> true);
            assertThrows(IllegalArgumentException.class, () -> new Game(new GameState(), List.of(SPEED), List.of(),
                    DarkUpgrades.DEFAULT, List.of(twice, twice), new Random(1)));
        }

        @Test
        void leJeuCompletASesSucces() {
            Game game = new Game();
            assertEquals(67, game.achievements().size());
        }
    }

    @Nested
    class Simulation {

        /** Joue en achetant tout dès que possible, jusqu'à pouvoir fusionner. Renvoie la durée en minutes. */
        private double minutesUntilFusion(Game game) {
            double dt = 0.05;
            double seconds = 0;
            while (!game.canFuse() && seconds < 3 * 3600) {
                game.tick(dt);
                seconds += dt;
                for (Upgrade upgrade : game.upgrades(Resource.PARTICLES)) {
                    while (game.buy(upgrade.id())) { /* achète tant que possible */ }
                }
            }
            return seconds / 60;
        }

        /**
         * Garde-fou d'équilibrage : un joueur qui achète tout dès que possible doit
         * débloquer le dixième générateur en 33 à 47 minutes avec le catalogue du jeu, premier
         * élan compris (version 0.125, tempo 12 ; 50 à 70 minutes de la 0.095 à la 0.124, 3 min 30 à 6 minutes avant).
         * Si ce test échoue après un changement dans {@link Upgrades}, c'est que la durée
         * de la phase 1 a bougé : ajuster les coûts, ou la fourchette si c'est voulu.
         */
        @Test
        void laPremierePartieDureEnvironQuaranteMinutes() {
            Game game = new Game();
            game.setFirstRunPace(Game.FIRST_RUN_PACE);
            game.start();
            double minutes = minutesUntilFusion(game);

            assertEquals(Upgrades.MAX_GENERATORS, game.maxGeneratorCount());
            assertTrue(game.canFuse());
            assertTrue(minutes >= 33 && minutes <= 47, "durée de la première partie : " + minutes + " min");
            assertTrue(game.firstRunSlowdown() == Game.FIRST_RUN_PACE, "le premier élan dure jusqu'à la première fusion");
            assertTrue(game.fuse());
            assertEquals(Game.FIRST_ATOMS_PACE, game.firstRunSlowdown(), 0, "dès le premier atome, la création prend de l'élan");
            // L'animation se lit création par création sous 6 par seconde et par générateur ; au-delà,
            // la particule reste pleine et palpite. Le rythme a multiplié la vitesse de départ : la
            // première partie finit dans ce second mode, mais la vitesse ne doit pas s'envoler pour autant.
            assertTrue(game.speed().toDouble() < 6 * Game.TEMPO, "vitesse finale trop élevée : " + game.speed().format());
        }

        /**
         * Garde-fou sur l'arrivée de l'automatisation et du tableau périodique : un joueur qui
         * dépense ses atomes au fur et à mesure dans les améliorations les moins chères (sans
         * économiser pour Persistance) doit créer son trentième atome entre 1 h 35 et 2 h, premier
         * élan compris (version 0.125, tempo 12 ; 2 h 20 à 2 h 50 de la 0.096 à la 0.124, 22 à 34 minutes avant la 0.095).
         * Là, la création a pris tout son élan. Plus tôt, le début perd son intérêt ;
         * plus tard, il faut tout racheter à la main des dizaines de fois.
         */
        @Test
        void automatisationEtTableauArriventVers1h45() {
            Game game = new Game();
            game.setFirstRunPace(Game.FIRST_RUN_PACE);
            game.start();
            double minutes = 0;
            while (!game.isAutomationUnlocked() && minutes < 10 * 60) {
                minutes += minutesUntilFusion(game);
                assertTrue(game.fuse());
                boolean bought = true;
                while (bought) {
                    Upgrade cheapest = null;
                    for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
                        if (upgrade.id().equals("atom_keep") || !game.canBuy(upgrade.id())) continue;
                        if (cheapest == null || game.costOf(upgrade.id()).lt(game.costOf(cheapest.id()))) cheapest = upgrade;
                    }
                    bought = cheapest != null && game.buy(cheapest.id());
                }
            }
            assertTrue(game.isAutomationUnlocked());
            assertTrue(game.isPeriodicTableUnlocked());       // les deux se débloquent ensemble
            assertEquals(0, game.levelOf("atom_keep"));
            assertValue(25, game.costOf("atom_keep"));
            assertTrue(minutes >= 95 && minutes <= 120, "trentième atome créé après " + minutes + " min");
            assertEquals(1, game.firstRunSlowdown(), 0, "trente atomes : la création a tout son élan");
        }

        /**
         * Garde-fou sur les atomes : en dépensant ses atomes dès que possible, les parties
         * raccourcissent, mais les dix premières durent encore plus de 45 secondes.
         */
        @Test
        void lesAtomesRaccourcissentLesPartiesSansLesRendreInstantanees() {
            Game game = new Game();
            game.start();
            double first = minutesUntilFusion(game);
            double previous = first;
            for (int fusion = 1; fusion <= 9; fusion++) {
                assertTrue(game.fuse());
                for (Upgrade upgrade : game.upgrades(Resource.ATOMS)) {
                    while (game.buy(upgrade.id())) { /* dépense tant que possible */ }
                }
                double minutes = minutesUntilFusion(game);
                assertTrue(minutes <= previous + 0.5,
                        "la partie " + (fusion + 1) + " est plus longue que la précédente : " + minutes + " min");
                assertTrue(minutes > 0.75, "la partie " + (fusion + 1) + " est trop courte : " + minutes + " min");
                previous = minutes;
            }
            assertTrue(previous < first / 3, "les atomes n'accélèrent pas assez : " + previous + " min");
        }
    }
}

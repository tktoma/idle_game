package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le troisième acte et ce qui vient après : la prime de vitesse, les collections de molécules, la
 * variété que demandent l'amas de galaxies et l'univers, la comète, les améliorations d'espace de
 * fin de partie, les records par étape, les succès du troisième acte et les défis de Big Bang.
 */
class TroisiemeActeTest {

    private static Game newGame() {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        game.start();
        return game;
    }

    private static Game afterBigBangs(int bigBangs, double space) {
        Game game = newGame();
        game.state().setBigBangs(bigBangs);
        game.state().setSpace(BigNum.of(space));
        return game;
    }

    private static void fillTable(Game game) {
        for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
    }

    /** Tout ce qu'il faut pour que le Big Bang soit possible. */
    private static void readyForBigBang(Game game) {
        game.state().setExplosions(Math.max(1, game.state().explosions()));
        game.state().setParticles(Game.BIG_BANG_PARTICLES);
        game.state().setAtoms(Game.BIG_BANG_ATOMS);
        game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER);
        int given = 0;
        for (Achievement achievement : game.achievements()) {
            if (given++ < Game.BIG_BANG_ACHIEVEMENTS) game.state().addAchievement(achievement.id());
        }
        for (Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
    }

    @Nested
    class PrimeDeVitesse {

        @Test
        void laPremiereExplosionPoseLeRecordSansPrime() {
            Game game = newGame();
            fillTable(game);
            game.state().stats().addTime(1000);
            assertEquals(0, game.explosionRecord(), 0);
            assertFalse(game.earnsSpeedPrime());
            assertEquals(game.darkMatterPerExplosion(), game.nextExplosionDarkMatter());
            assertTrue(game.explode());
            assertEquals(1000, game.explosionRecord(), 1e-9);
            assertEquals(900, game.speedPrimeTime(), 1e-9);
            assertFalse(game.takeSpeedPrime());
            assertEquals(0, game.stats().speedPrimes());
        }

        @Test
        void battreLeRecordDeDixPourCentRapporteUneMatiereNoireDePlus() {
            Game game = newGame();
            fillTable(game);
            game.state().stats().addTime(1000);
            assertTrue(game.explode());
            BigNum before = game.state().darkMatter();
            fillTable(game);
            game.state().stats().addTime(899);
            assertTrue(game.earnsSpeedPrime());
            assertEquals(1, game.speedPrimeLeft(), 1e-9);
            assertEquals(game.darkMatterPerExplosion().add(Game.SPEED_PRIME), game.nextExplosionDarkMatter());
            assertTrue(game.explode());
            assertEquals(before.add(game.darkMatterPerExplosion()).add(Game.SPEED_PRIME), game.state().darkMatter());
            assertTrue(game.takeSpeedPrime());
            assertFalse(game.takeSpeedPrime());                       // annoncée une seule fois
            assertEquals(1, game.stats().speedPrimes());
            // Le record est maintenant celui-là : il faut battre 899 s de dix pour cent.
            assertEquals(899, game.explosionRecord(), 1e-9);
        }

        @Test
        void unPeuPlusViteNeSuffitPasEtNeChangePasLeRecord() {
            Game game = newGame();
            fillTable(game);
            game.state().stats().addTime(1000);
            assertTrue(game.explode());
            BigNum before = game.state().darkMatter();
            fillTable(game);
            game.state().stats().addTime(950);                         // plus vite, mais pas de dix pour cent
            assertFalse(game.earnsSpeedPrime());
            assertEquals(0, game.speedPrimeLeft(), 0);
            assertTrue(game.explode());
            assertEquals(before.add(game.darkMatterPerExplosion()), game.state().darkMatter());
            assertEquals(1000, game.explosionRecord(), 1e-9);          // la barre ne bouge que quand la prime est touchée
        }

        @Test
        void lesPrimesSEpuisentEllesNeSontPasUneRente() {
            Game game = newGame();
            fillTable(game);
            game.state().stats().addTime(100);
            assertTrue(game.explode());
            // Des explosions instantanées : la première touche la prime et pose un record nul, les suivantes n'ont plus rien à battre.
            int primes = 0;
            for (int i = 0; i < 10; i++) {
                fillTable(game);
                assertTrue(game.explode());
                if (game.takeSpeedPrime()) primes++;
            }
            assertEquals(1, primes);
        }

        @Test
        void unDefiRejoueNeToucheRien() {
            Game game = newGame();
            fillTable(game);
            game.state().stats().addTime(1000);
            assertTrue(game.explode());
            Challenge challenge = game.challenges().get(0);
            game.state().addCompletedChallenge(challenge.id());
            game.state().setActiveChallenge(challenge.id());
            fillTable(game);
            assertTrue(game.isChallengeReplay());
            assertFalse(game.earnsSpeedPrime());
            assertTrue(game.nextExplosionDarkMatter().isZero());
        }

        @Test
        void leBigBangRemetLeRecordAZero() {
            Game game = newGame();
            fillTable(game);
            game.state().stats().addTime(1000);
            assertTrue(game.explode());
            readyForBigBang(game);
            assertTrue(game.bigBang());
            assertEquals(0, game.explosionRecord(), 0);
        }

        @Test
        void laPrimeSuitLesPaliersDeBigBang() {
            Game game = newGame();
            assertEquals(Game.SPEED_PRIME, game.speedPrime());
            game.state().setBigBangs(1);                               // matière noire des explosions ×2
            assertEquals(Game.SPEED_PRIME.multiply(2), game.speedPrime());
        }
    }

    @Nested
    class Collections {

        @Test
        void chaqueRayonASaCollection() {
            Game game = afterBigBangs(1, 1e9);
            assertEquals(Molecule.Kind.values().length, game.collections().size());
            int total = 0;
            for (KindCollection collection : game.collections()) {
                assertTrue(game.kindSize(collection.kind()) > 0, collection.kind().name());
                assertEquals(0, game.kindCreated(collection.kind()));
                assertEquals(0, game.collectionLevel(collection.kind()));
                assertTrue(collection.half() > 1 && collection.full() > collection.half());
                total += game.kindSize(collection.kind());
            }
            assertEquals(game.molecules().size(), total);
            for (Molecule.Stat stat : Molecule.Stat.values()) assertEquals(1, game.collectionBoost(stat), 0);
        }

        @Test
        void laMoitiePuisLeRayonEntier() {
            Game game = afterBigBangs(1, 1e9);
            KindCollection simple = game.collections().get(0);
            Molecule.Kind kind = simple.kind();
            int half = game.collectionHalf(kind);
            assertEquals((game.kindSize(kind) + 1) / 2, half);
            int created = 0;
            for (Molecule molecule : game.molecules()) {
                if (molecule.kind() != kind) continue;
                game.state().setMoleculeCount(molecule.id(), 1);
                created++;
                assertEquals(created, game.kindCreated(kind));
                int expected = created >= game.kindSize(kind) ? 2 : created >= half ? 1 : 0;
                assertEquals(expected, game.collectionLevel(kind), "après " + created + " sortes");
                assertEquals(simple.factor(expected), game.collectionBoost(simple.stat()), 1e-12);
            }
            assertEquals(1, game.collectionsAt(2));
            assertEquals(1, game.collectionsAt(1));
        }

        @Test
        void lesCollectionsDUneMemeGrandeurSeMultiplient() {
            Game game = afterBigBangs(1, 1e9);
            double expected = 1;
            for (KindCollection collection : game.collections()) {
                if (collection.stat() != Molecule.Stat.SPACE) continue;
                for (Molecule molecule : game.molecules()) {
                    if (molecule.kind() == collection.kind()) game.state().setMoleculeCount(molecule.id(), 1);
                }
                expected *= collection.full();
            }
            assertEquals(expected, game.collectionBoost(Molecule.Stat.SPACE), 1e-12);
            assertTrue(expected > 3);
        }

        @Test
        void laCollectionMultiplieLaGrandeurACoteDesAmeliorationsEtDesPaliers() {
            Game game = afterBigBangs(2, 1e9);
            game.state().addSpaceUpgrade("space_inflation_1");
            for (Molecule molecule : game.molecules()) {
                if (molecule.kind() == Molecule.Kind.SIMPLE) game.state().setMoleculeCount(molecule.id(), 1);
            }
            for (Molecule.Stat stat : Molecule.Stat.values()) {
                assertEquals(game.matterBoost(stat) * game.spaceUpgradeBoost(stat) * game.bigBangMilestoneBoost(stat)
                        * game.collectionBoost(stat), game.moleculeBoost(stat), 1e-9, stat.name());
            }
            assertEquals(1.25, game.collectionBoost(Molecule.Stat.SPACE), 1e-12);
        }

        @Test
        void rienAvantLeBigBang() {
            Game game = newGame();
            for (Molecule molecule : game.molecules()) game.state().setMoleculeCount(molecule.id(), 1);
            for (KindCollection collection : game.collections()) assertEquals(0, game.collectionLevel(collection.kind()));
        }
    }

    @Nested
    class VarieteDuCosmos {

        @Test
        void lAmasDeGalaxiesEtLUniversDemandentDesSortes() {
            assertTrue(Cosmos.GALAXY.sorts().isEmpty());
            for (Molecule.State state : Molecule.State.values()) {
                int cluster = Cosmos.CLUSTER.sorts().get(state);
                int universe = Cosmos.UNIVERSE.sorts().get(state);
                assertTrue(cluster > 0 && universe > cluster, state.name());
                // Jamais plus que ce que le catalogue contient.
                long catalog = Molecules.DEFAULT.stream().filter(molecule -> molecule.hasState() && molecule.state() == state).count();
                assertTrue(universe <= catalog, state.name() + " : " + universe + " sur " + catalog);
            }
        }

        @Test
        void uneSorteRassembleeCompteMemeAvecPeuDeMolecules() {
            Game game = afterBigBangs(1, 1e12);
            assertEquals(0, game.sortsInState(Molecule.State.LIQUID));
            game.state().setMoleculeCount("H2O", 3);
            assertEquals(0, game.sortsInState(Molecule.State.LIQUID));         // créée, pas rassemblée
            game.state().addSubstance("H2O");
            assertEquals(1, game.sortsInState(Molecule.State.LIQUID));
            assertEquals(0, game.sortsInState(Molecule.State.GAS));
        }

        @Test
        void laMatiereSansLaVarieteNeSuffitPas() {
            Game game = afterBigBangs(1, 1e12);
            for (Body body : game.bodies()) game.state().addBody(body.id());
            game.state().setCosmosLevel(1);
            // Toute la matière dans une seule sorte par état.
            for (Map.Entry<Molecule.State, Integer> need : Cosmos.CLUSTER.matter().entrySet()) {
                Molecule molecule = game.molecules().stream().filter(each -> each.state() == need.getKey()).findFirst().orElseThrow();
                game.state().addSubstance(molecule.id());
                game.state().setMoleculeCount(molecule.id(), need.getValue());
            }
            assertEquals(6, game.cosmosConditionsMet(Cosmos.CLUSTER));
            assertFalse(game.canFormCosmos(Cosmos.CLUSTER));
            ShoppingList list = game.shoppingList();
            assertEquals(ShoppingList.Kind.COSMOS, list.kind());
            assertTrue(list.matter().isEmpty());
            for (Molecule.State state : Molecule.State.values()) {
                assertEquals(Cosmos.CLUSTER.sorts().get(state) - 1, list.sorts().get(state), state.name());
            }
            // La liste de courses veut alors n'importe quelle sorte de ces états.
            assertTrue(list.wants(game.molecule("H2O")));
            // Les sortes rassemblées une à une : la dernière ouvre l'amas.
            for (Map.Entry<Molecule.State, Integer> need : Cosmos.CLUSTER.sorts().entrySet()) {
                game.molecules().stream().filter(each -> each.state() == need.getKey()).limit(need.getValue())
                        .forEach(each -> game.state().addSubstance(each.id()));
            }
            assertEquals(11, game.cosmosConditionsMet(Cosmos.CLUSTER));
            assertTrue(game.canFormCosmos(Cosmos.CLUSTER));
            assertTrue(game.shoppingList().ready());
            assertTrue(game.shoppingList().sorts().isEmpty());
        }
    }

    @Nested
    class Comete {

        @Test
        void aucuneAvantLePremierBigBang() {
            Game game = newGame();
            game.tick(3 * 3600);
            assertFalse(game.isCometVisible());
            assertFalse(game.takeCometAppeared());
            assertFalse(game.catchComet());
            assertEquals(1, game.cometBoost(), 0);
        }

        @Test
        void elleApparaitEntreHuitEtSeizeMinutesPuisSEnVa() {
            Game game = afterBigBangs(1, 0);
            game.tick(1);
            double wait = game.cometWait();
            assertTrue(wait >= Game.COMET_MIN_WAIT - 1 && wait <= Game.COMET_MAX_WAIT, "attente " + wait);
            game.tick(wait - 1);
            assertFalse(game.isCometVisible());
            game.tick(1.5);
            assertTrue(game.isCometVisible());
            assertTrue(game.takeCometAppeared());
            assertFalse(game.takeCometAppeared());
            assertEquals(Game.COMET_VISIBLE_SECONDS, game.cometVisibleFor(), 1);
            assertEquals(0, game.cometWait(), 0);
            // Personne ne la saisit : elle repart, et rien n'est perdu.
            BigNum space = game.state().space();
            game.tick(Game.COMET_VISIBLE_SECONDS + 1);
            assertFalse(game.isCometVisible());
            assertTrue(game.state().space().gte(space));
            assertEquals(1, game.cometBoost(), 0);
            assertTrue(game.cometWait() > 0);
        }

        @Test
        void saisieElleDoubleLExpansionDeuxMinutes() {
            Game game = afterBigBangs(1, 0);
            game.state().setCometVisible(30);
            double steady = game.steadySpacePerSecond().toDouble();
            assertEquals(steady, game.spacePerSecond().toDouble(), 1e-12);
            assertTrue(game.catchComet());
            assertFalse(game.catchComet());                            // une seule fois
            assertFalse(game.isCometVisible());
            assertEquals(1, game.cometsCaught());
            assertEquals(Game.COMET_BOOST, game.cometBoost(), 0);
            assertEquals(Game.COMET_BOOST_SECONDS, game.cometBoostLeft(), 0);
            assertEquals(steady * Game.COMET_BOOST, game.spacePerSecond().toDouble(), 1e-12);
            double before = game.state().space().toDouble();
            game.tick(10);
            assertEquals(before + 10 * steady * Game.COMET_BOOST, game.state().space().toDouble(), 1e-6);
            game.tick(Game.COMET_BOOST_SECONDS);
            assertEquals(1, game.cometBoost(), 0);
        }

        @Test
        void unLongPasDeTempsNeCompteLeSillageQuePourCeQuIlLuiReste() {
            Game game = afterBigBangs(1, 0);
            game.state().setCometVisible(30);
            assertTrue(game.catchComet());
            double steady = game.steadySpacePerSecond().toDouble();
            game.tick(1000);                                           // 120 s de sillage, 880 s sans
            assertEquals(steady * (1000 + Game.COMET_BOOST_SECONDS * (Game.COMET_BOOST - 1)), game.state().space().toDouble(), 1e-6);
        }

        @Test
        void uneAbsenceNEnSaisitAucune() {
            Game game = afterBigBangs(1, 0);
            game.tick(24 * 3600);
            assertEquals(0, game.cometsCaught());
            assertEquals(1, game.cometBoost(), 0);
        }

        @Test
        void elleNeToucheANiAuHasardDuJeu() {
            // Deux parties au même tirage, l'une avec comètes : leurs synthèses tombent pareil.
            Game with = afterBigBangs(1, 0);
            Game without = newGame();
            with.tick(3600);
            without.tick(3600);
            Random a = new Random(42);
            Random b = new Random(42);
            assertEquals(a.nextLong(), b.nextLong());                   // le témoin
            with.state().setAtoms(BigNum.of(1, 6));
            without.state().setAtoms(BigNum.of(1, 6));
            with.state().setTotalAtoms(BigNum.of(1, 6));
            without.state().setTotalAtoms(BigNum.of(1, 6));
            for (int i = 0; i < 20; i++) {
                with.synthesize();
                without.synthesize();
            }
            assertEquals(without.state().elements(), with.state().elements());
        }
    }

    @Nested
    class AmeliorationsDeFinDePartie {

        @Test
        void deuxInflationsEtUneCadenceTardives() {
            Game game = afterBigBangs(5, 0);
            SpaceUpgrade third = null;
            SpaceUpgrade fourth = null;
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
                if (upgrade.id().equals("space_inflation_3")) third = upgrade;
                if (upgrade.id().equals("space_inflation_4")) fourth = upgrade;
            }
            assertTrue(third.space().gt(BigNum.of(1_000_000)) && fourth.space().gt(third.space()));
            game.state().setSpace(BigNum.of(1, 7));
            game.tick(0.01);
            assertFalse(game.ownsSpaceUpgrade("space_inflation_3"));
            assertFalse(game.ownsSpaceUpgrade("space_creation_pace_2"));
            game.state().setSpace(BigNum.of(1, 8));
            game.tick(0.01);
            assertTrue(game.ownsSpaceUpgrade("space_inflation_3"));
            assertTrue(game.ownsSpaceUpgrade("space_creation_pace_2"));
            assertFalse(game.ownsSpaceUpgrade("space_inflation_4"));
            assertEquals(8, game.spaceUpgradeBoost(Molecule.Stat.SPACE), 1e-12);
            assertEquals(1, game.moleculeAutomationInterval(), 0);
            game.state().setSpace(BigNum.of(5, 9));
            game.tick(0.01);
            assertTrue(game.ownsSpaceUpgrade("space_inflation_4"));
            assertEquals(16, game.spaceUpgradeBoost(Molecule.Stat.SPACE), 1e-12);
        }
    }

    @Nested
    class Records {

        @Test
        void laPremiereFoisPoseLeRecordSansLAnnoncer() {
            Game game = afterBigBangs(1, 1e9);
            fillTable(game);
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            assertTrue(Double.isNaN(game.recordOf(GameStats.Step.FIRST_MOLECULE)));
            game.state().setTimePlayed(5000);
            assertTrue(game.createMolecule("H2O"));
            assertEquals(5000, game.recordOf(GameStats.Step.FIRST_MOLECULE), 1e-9);
            assertEquals(List.of(), game.takeNewRecords());
            // Une deuxième molécule ne touche pas au record.
            game.state().setTimePlayed(6000);
            assertTrue(game.createMolecule("H2O"));
            assertEquals(5000, game.recordOf(GameStats.Step.FIRST_MOLECULE), 1e-9);
        }

        @Test
        void laRemiseAZeroGardeLesRecordsEtLaPartieSuivantePeutLesBattre() {
            Game game = afterBigBangs(1, 1e9);
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            game.state().setTimePlayed(5000);
            assertTrue(game.createMolecule("H2O"));
            game.reset();
            assertEquals(5000, game.recordOf(GameStats.Step.FIRST_MOLECULE), 1e-9);
            assertFalse(game.stats().reached(GameStats.Step.FIRST_MOLECULE));

            // Plus lente : le record tient.
            game.start();
            game.state().setBigBangs(1);
            game.state().setSpace(BigNum.of(1, 9));
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            game.state().setTimePlayed(7000);
            assertTrue(game.createMolecule("H2O"));
            assertEquals(5000, game.recordOf(GameStats.Step.FIRST_MOLECULE), 1e-9);
            assertEquals(List.of(), game.takeNewRecords());

            // Plus rapide : il tombe, et c'est annoncé une fois.
            game.reset();
            game.start();
            game.state().setBigBangs(1);
            game.state().setSpace(BigNum.of(1, 9));
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            game.state().setTimePlayed(4000);
            assertTrue(game.createMolecule("H2O"));
            assertEquals(4000, game.recordOf(GameStats.Step.FIRST_MOLECULE), 1e-9);
            assertEquals(List.of(GameStats.Step.FIRST_MOLECULE), game.takeNewRecords());
            assertEquals(List.of(), game.takeNewRecords());
            game.clearRecords();
            assertTrue(Double.isNaN(game.recordOf(GameStats.Step.FIRST_MOLECULE)));
        }

        @Test
        void lEcartSeLitSurLaProchaineEtape() {
            Game game = newGame();
            assertEquals(GameStats.Step.FIRST_FUSION, game.nextStep());
            assertTrue(Double.isNaN(game.recordGap()));                // pas encore de record
            game.state().setRecord(GameStats.Step.FIRST_FUSION.name(), 2000);
            game.state().setTimePlayed(1500);
            assertEquals(-500, game.recordGap(), 1e-9);                // en avance
            game.state().setTimePlayed(2600);
            assertEquals(600, game.recordGap(), 1e-9);                 // en retard
        }

        @Test
        void laPremiereFusionEtLaPremiereExplosionSontDatees() {
            Game game = newGame();
            for (Upgrade upgrade : game.upgrades(Resource.PARTICLES)) {
                if (upgrade.effect() instanceof Effect.AddGenerator) game.state().setLevel(upgrade.id(), game.generatorsPerAtom() - 1);
            }
            game.state().setTimePlayed(1234);
            assertTrue(game.fuse());
            assertEquals(1234, game.stats().reachedAt(GameStats.Step.FIRST_FUSION), 1e-9);
            assertEquals(1234, game.recordOf(GameStats.Step.FIRST_FUSION), 1e-9);
            assertEquals(GameStats.Step.FIRST_EXPLOSION, game.nextStep());
            fillTable(game);
            game.state().setTimePlayed(80_000);
            assertTrue(game.explode());
            assertEquals(80_000, game.recordOf(GameStats.Step.FIRST_EXPLOSION), 1e-9);
            assertEquals(GameStats.Step.FIRST_BIG_BANG, game.nextStep());
        }

        @Test
        void uneAncienneSauvegardeNEstPasDateeApresCoup() {
            // Une partie qui a déjà fusionné et explosé sans que ces étapes aient été notées.
            Game game = newGame();
            game.state().stats().noteFusion(BigNum.ONE, 10);
            game.state().stats().endRun();
            assertFalse(game.stats().reached(GameStats.Step.FIRST_FUSION));
            assertEquals(GameStats.Step.FIRST_BIG_BANG, game.nextStep());
            fillTable(game);
            assertTrue(game.explode());
            assertFalse(game.stats().reached(GameStats.Step.FIRST_EXPLOSION));
            assertTrue(Double.isNaN(game.recordOf(GameStats.Step.FIRST_EXPLOSION)));
        }
    }

    @Nested
    class SuccesDuTroisiemeActe {

        @Test
        void seizeSuccesDeLaPremiereMoleculeALUnivers() {
            List<Achievement> all = Achievements.DEFAULT;
            int first = -1;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).id().equals("step_first_molecule")) first = i;
            }
            assertEquals(50, first);                                   // à la suite des cinquante premiers
            assertEquals(16, all.size() - first);
            assertEquals("step_universe", all.get(all.size() - 1).id());
        }

        @Test
        void ilsTombentAuFilDuTroisiemeActe() {
            Game game = afterBigBangs(1, 1e12);
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
                if (!(upgrade.effect() instanceof SpaceUpgrade.Boost)) game.state().addSpaceUpgrade(upgrade.id());
            }
            game.state().setAutoGather(false);
            game.state().setAutoForm(false);
            game.tick(1.1);
            assertFalse(game.hasAchievement("step_first_molecule"));
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            assertTrue(game.createMolecule("H2O"));
            game.state().setMoleculeCount("H2O", 3);
            assertTrue(game.formSubstance("H2O"));
            game.state().setBigBangs(5);
            for (Body body : game.bodies()) game.state().addBody(body.id());
            game.state().setCosmosLevel(3);
            game.state().setCometVisible(10);
            assertTrue(game.catchComet());
            game.tick(1.1);
            for (String id : List.of("step_first_molecule", "step_first_gathering", "step_second_bang", "step_five_bangs",
                    "step_first_body", "step_ten_bodies", "step_first_star", "step_galaxy", "step_cluster", "step_universe", "feat_comet")) {
                assertTrue(game.hasAchievement(id), id);
            }
            assertFalse(game.hasAchievement("step_fifty_kinds"));
            assertFalse(game.hasAchievement("feat_bang_challenge"));
        }
    }

    @Nested
    class DefisDeBigBang {

        private Game ready(int bigBangs) {
            Game game = afterBigBangs(bigBangs, 1e9);
            game.state().addSpaceUpgrade("space_states");
            game.state().setExplosions(3);
            game.state().setDarkMatterSpent(BigNum.of(50));
            game.state().setDarkLevel("dark_density", 2);
            return game;
        }

        @Test
        void ilsSOuvrentUneFoisTousLesPaliersAtteints() {
            Game game = ready(4);
            assertEquals(3, game.bangChallenges().size());
            assertFalse(game.isBangChallengesUnlocked());
            assertFalse(game.canStartBangChallenge());
            assertFalse(game.startBangChallenge(BangChallenge.VOID));
            game.state().setBigBangs(5);
            assertTrue(game.isBangChallengesUnlocked());
            assertTrue(game.canStartBangChallenge());
        }

        @Test
        void commencerRemetAZeroCommeUnBigBangSansEnCompterUn() {
            Game game = ready(5);
            game.state().setParticles(BigNum.of(1, 50));
            game.state().setAtoms(BigNum.of(1000));
            game.state().setDarkMatter(BigNum.of(300));
            game.state().setExplosionRecord(500);
            game.state().setMoleculeCount("H2O", 7);
            game.state().setTimePlayed(10_000);
            assertTrue(game.startBangChallenge(BangChallenge.FORGOTTEN));
            assertEquals(BangChallenge.FORGOTTEN, game.activeBangChallenge());
            assertEquals(5, game.bigBangs());                                  // pas un Big Bang de plus
            assertTrue(game.state().particles().isZero() && game.state().atoms().isZero());
            assertTrue(game.state().darkMatter().isZero());
            assertEquals(0, game.state().explosions());
            assertEquals(0, game.explosionRecord(), 0);
            assertEquals(2, game.darkLevelOf("dark_density"));                 // l'arbre reste, comme au cinquième Big Bang
            assertEquals(7, game.moleculeCount("H2O"));                        // le troisième acte aussi
            assertFalse(game.canStartBangChallenge());                         // un seul à la fois
            assertFalse(game.startBangChallenge(BangChallenge.VOID));
            game.state().setTimePlayed(10_600);
            assertEquals(600, game.bangChallengeTime(), 1e-9);
        }

        @Test
        void leBigBangSuivantReussitLeDefiEtCompteNormalement() {
            Game game = ready(5);
            game.state().setTimePlayed(10_000);
            assertTrue(game.startBangChallenge(BangChallenge.VOID));
            assertFalse(game.isBangChallengeCompleted(BangChallenge.VOID));
            assertEquals(1, game.bangReward(BangChallenge.Reward.SPACE), 0);
            readyForBigBang(game);
            game.state().setTimePlayed(13_000);
            assertTrue(game.bigBang());
            assertEquals(6, game.bigBangs());
            assertNull(game.activeBangChallenge());
            assertTrue(game.isBangChallengeCompleted(BangChallenge.VOID));
            assertEquals(1, game.completedBangChallenges());
            assertEquals(3000, game.bangChallengeBest(BangChallenge.VOID), 1e-9);
            assertEquals(2, game.bangReward(BangChallenge.Reward.SPACE), 0);
            assertTrue(Double.isNaN(game.bangChallengeBest(BangChallenge.BY_HAND)));
            // Rejoué plus lentement : le meilleur temps reste, la récompense ne double pas.
            assertTrue(game.startBangChallenge(BangChallenge.VOID));
            readyForBigBang(game);
            game.state().setTimePlayed(20_000);
            assertTrue(game.bigBang());
            assertEquals(3000, game.bangChallengeBest(BangChallenge.VOID), 1e-9);
            assertEquals(2, game.bangReward(BangChallenge.Reward.SPACE), 0);
        }

        @Test
        void abandonnerLeveLaContrainteSansRienDonner() {
            Game game = ready(5);
            assertFalse(game.abandonBangChallenge());
            assertTrue(game.startBangChallenge(BangChallenge.BY_HAND));
            assertTrue(game.isHandOnly());
            assertTrue(game.abandonBangChallenge());
            assertNull(game.activeBangChallenge());
            assertFalse(game.isHandOnly());
            assertFalse(game.isBangChallengeCompleted(BangChallenge.BY_HAND));
        }

        @Test
        void dansLUniversVideLaMatiereNeMultipliePlusRien() {
            Game game = ready(5);
            game.state().setMoleculeCount("H2O", 100);
            game.state().addSubstance("H2O");
            game.state().addSpaceUpgrade("space_inflation_1");
            assertTrue(game.matterBoost(Molecule.Stat.PARTICLES) > 1);
            double upgrades = game.spaceUpgradeBoost(Molecule.Stat.SPACE);
            assertTrue(game.startBangChallenge(BangChallenge.VOID));
            for (Molecule.Stat stat : Molecule.Stat.values()) assertEquals(1, game.matterBoost(stat), 0);
            // Les améliorations d'espace et les paliers, eux, restent.
            assertEquals(upgrades, game.spaceUpgradeBoost(Molecule.Stat.SPACE), 0);
            assertEquals(upgrades * game.bigBangMilestoneBoost(Molecule.Stat.SPACE), game.moleculeBoost(Molecule.Stat.SPACE), 1e-9);
            assertTrue(game.abandonBangChallenge());
            assertTrue(game.matterBoost(Molecule.Stat.PARTICLES) > 1);
        }

        @Test
        void sansMemoireLesPaliersNeMultiplientPlusRien() {
            Game game = ready(5);
            game.state().setDarkMatter(BigNum.of(400));
            assertTrue(game.bigBangMilestoneBoost(Molecule.Stat.SPACE) > 1);
            assertTrue(game.bigBangDarkMatterFactor() > 1 && game.accretionFactor() > 1);
            assertTrue(game.startBangChallenge(BangChallenge.FORGOTTEN));
            game.state().setDarkMatter(BigNum.of(400));
            assertEquals(1, game.bigBangMilestoneBoost(Molecule.Stat.SPACE), 0);
            assertEquals(1, game.bigBangDarkMatterFactor(), 0);
            assertEquals(1, game.accretionFactor(), 0);
            assertEquals(1, game.darkMatterSpaceBoost(), 0);
            // Ce que les paliers gardent ou automatisent reste.
            assertTrue(game.isMoleculeAutomationUnlocked());
            assertTrue(game.bigBangKeepsDarkTree() && game.bigBangKeepsChallenges());
        }

        @Test
        void aLaMainAucunAutomatismeDeMatiereNoireNiDuBigBang() {
            Game game = ready(5);
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
                if (!(upgrade.effect() instanceof SpaceUpgrade.Boost)) game.state().addSpaceUpgrade(upgrade.id());
            }
            game.state().setDarkMatter(BigNum.of(100));
            assertTrue(game.setDarkAutomationEnabled("dark_auto_tree", true));
            game.state().setMoleculeCount("H2O", 3);
            game.state().addSubstance("H2O");
            assertTrue(game.setMoleculeAutomated("H2O", true));
            assertTrue(game.isAutoHolding() && game.isAutoCreatingMolecules() && game.isAutoGathering() && game.isAutoForming());
            assertTrue(game.startBangChallenge(BangChallenge.BY_HAND));
            game.state().setExplosions(1);                                     // la matière noire existe de nouveau
            game.state().setDarkMatter(BigNum.of(100));
            assertFalse(game.isAutoHolding() || game.isAutoCreatingMolecules() || game.isAutoGathering() || game.isAutoForming());
            assertFalse(game.isDarkAutomationEnabled("dark_auto_tree"));
            // Les réglages n'ont pas bougé : ils reprennent à la fin du défi.
            assertTrue(game.state().isDarkAutomationEnabled("dark_auto_tree"));
            assertTrue(game.abandonBangChallenge());
            assertTrue(game.isDarkAutomationEnabled("dark_auto_tree"));
            assertTrue(game.isAutoCreatingMolecules() && game.isAutoGathering() && game.isAutoForming());
        }

        @Test
        void lesRecompensesAgissent() {
            Game game = ready(5);
            double space = game.steadySpacePerSecond().toDouble();
            double accretion = game.accretionFactor();
            BigNum darkMatter = game.darkMatterPerExplosion();
            game.state().addCompletedBangChallenge(BangChallenge.VOID.name());
            assertEquals(2 * space, game.steadySpacePerSecond().toDouble(), space * 1e-9);
            game.state().addCompletedBangChallenge(BangChallenge.FORGOTTEN.name());
            assertEquals(1.5 * accretion, game.accretionFactor(), 1e-12);
            game.state().addCompletedBangChallenge(BangChallenge.BY_HAND.name());
            assertEquals(darkMatter.multiply(2), game.darkMatterPerExplosion());
            assertEquals(3, game.completedBangChallenges());
            // Ce que dit la carte est ce que fait la règle.
            for (BangChallenge challenge : BangChallenge.values()) {
                String factor = String.valueOf(Game.BANG_REWARDS.get(challenge.reward())).replaceAll("\\.0$", "");
                assertTrue(challenge.rewardText().endsWith("×" + factor), challenge.rewardText() + " contre ×" + factor);
            }
        }

        @Test
        void pasPendantUnDefiDExplosionEtLaRemiseAZeroLesOublie() {
            Game game = ready(5);
            game.state().setActiveChallenge(game.challenges().get(0).id());
            assertFalse(game.canStartBangChallenge());
            game.state().setActiveChallenge(null);
            assertTrue(game.startBangChallenge(BangChallenge.VOID));
            game.state().addCompletedBangChallenge(BangChallenge.BY_HAND.name());
            game.reset();
            assertNull(game.activeBangChallenge());
            assertEquals(0, game.completedBangChallenges());
        }
    }
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievement;
import idle.core.achievements.Achievements;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.darkmatter.Challenge;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.elements.Element;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.Molecules;
import idle.core.sky.Assemblies;
import idle.core.sky.Assembly;
import idle.core.sky.Bodies;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : les assemblages et les astres.
 */
class JeuAstresTest extends GameTestSupport {


    @Nested
    class LesAssemblages {

        /** Après un premier Big Bang, rassemblements et assemblages ouverts, beaucoup d'espace. */
        private Game game() {
            Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                    Achievements.DEFAULT, new Random(42));
            game.start();
            GameTestSupport.bangs(game, 1);
            game.state().setSpace(BigNum.of(1, 12));
            game.state().addSpaceUpgrade("space_states");
            game.state().addSpaceUpgrade("space_assemblies");
            return game;
        }

        /** Donne au joueur {@code count} molécules rassemblées de la sorte {@code id}. */
        private void give(Game game, String id, int count) {
            game.state().setMoleculeCount(id, count);
            game.state().addSubstance(id);
        }

        @Test
        void leCatalogueEstFaitDeMoleculesQuiExistentEtRienDeVivant() {
            java.util.Set<String> ids = new java.util.HashSet<>();
            Map<Assembly.Family, Integer> families = new EnumMap<>(Assembly.Family.class);
            for (Assembly assembly : Assemblies.DEFAULT) {
                assertTrue(ids.add(assembly.id()), assembly.id() + " en double");
                families.merge(assembly.family(), 1, Integer::sum);
                assertTrue(assembly.ingredients().size() >= 2, assembly.id());
                for (String ingredient : assembly.ingredients().keySet()) {
                    Molecule molecule = Molecules.DEFAULT.stream().filter(each -> each.id().equals(ingredient)).findFirst()
                            .orElseThrow(() -> new AssertionError(assembly.id() + " : molecule inconnue " + ingredient));
                    assertTrue(molecule.hasState(), assembly.id() + " : " + ingredient);
                    assertTrue(molecule.kind() != Molecule.Kind.LIFE, assembly.id() + " contient du vivant : " + ingredient);
                }
            }
            assertEquals(58, Assemblies.DEFAULT.size());
            assertEquals(Assembly.Family.values().length, families.size());
            assertEquals(14, families.get(Assembly.Family.STELLAR));
            assertEquals(10, families.get(Assembly.Family.ROCK));
            assertEquals(8, families.get(Assembly.Family.ORE));
            assertEquals(9, families.get(Assembly.Family.WATER));
        }

        @Test
        void unAssemblageDemandeBeaucoupDeMolecules() {
            for (Assembly assembly : Assemblies.DEFAULT) {
                // La matière d'étoiles se compte par centaines et par milliers : une étoile est plus grosse qu'une planète.
                int most = assembly.family() == Assembly.Family.STELLAR ? 1500 : 450;
                assertTrue(assembly.size() >= 150 && assembly.size() <= most, assembly.id() + " : " + assembly.size());
            }
            Assembly granite = new Game().assembly("granitic_rock");
            assertEquals("Roche granitique", granite.name());
            assertEquals(Map.of("SiO2", 120, "KAlSi3O8", 90, "NaAlSi3O8", 60), granite.ingredients());
            assertEquals(270, granite.size());
            assertEquals(new Molecule.Boost(Molecule.Stat.ATOMS, 1.35), granite.boost());      // 270 molécules : +135 %
            // Des pierres précieuses : presque rien que leur cristal, et les traces qui les colorent.
            Assembly gems = new Game().assembly("corundum_gems");
            assertEquals(300, gems.ingredients().get("Al2O3"));
            assertEquals(4, gems.ingredients().get("Cr2O3"));
            assertEquals(Molecule.Stat.DARK_GROWTH, gems.boost().stat());
        }

        @Test
        void pasDAssemblageSansLAmeliorationDEspace() {
            Game game = game();
            give(game, "N2", 312);
            give(game, "O2", 84);
            give(game, "CO2", 4);
            assertTrue(game.isAssembliesUnlocked());
            assertTrue(game.canFormAssembly("air"));
            Game closed = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                    Achievements.DEFAULT, new Random(42));
            closed.start();
            GameTestSupport.bangs(closed, 1);
            closed.state().addSpaceUpgrade("space_states");
            give(closed, "N2", 312);
            give(closed, "O2", 84);
            give(closed, "CO2", 4);
            assertFalse(closed.isAssembliesUnlocked());
            assertFalse(closed.canFormAssembly("air"));
            assertFalse(closed.formAssembly("air"));
            assertFalse(closed.hasAssembly("air"));
            // L'amélioration vient après celle des états de la matière, à 40 000 d'espace créé.
            closed.reset();
            closed.start();
            GameTestSupport.bangs(closed, 1);
            closed.state().setSpace(BigNum.of(40_000));
            assertFalse(closed.isSpaceUpgradeAvailable("space_assemblies"));
            assertTrue(closed.buySpaceUpgrade("space_states"));
            assertTrue(closed.buySpaceUpgrade("space_assemblies"));
            assertTrue(closed.isAssembliesUnlocked());
        }

        @Test
        void ilFautChaqueIngredientRassembleEtEnNombre() {
            Game game = game();
            give(game, "N2", 312);
            give(game, "O2", 84);
            game.state().setMoleculeCount("CO2", 4);                  // créé, mais pas rassemblé
            assertFalse(game.canFormAssembly("air"));
            game.state().addSubstance("CO2");
            assertTrue(game.canFormAssembly("air"));
            game.state().setMoleculeCount("O2", 83);                  // il en manque une
            assertFalse(game.canFormAssembly("air"));
            assertFalse(game.formAssembly("air"));
            assertFalse(game.stats().reached(GameStats.Step.FIRST_ASSEMBLY));
            game.state().setMoleculeCount("O2", 84);
            game.state().setTimePlayed(4321);
            assertTrue(game.formAssembly("air"));
            assertEquals(4321, game.stats().reachedAt(GameStats.Step.FIRST_ASSEMBLY), 1e-9);
            assertTrue(game.hasAssembly("air"));
            assertEquals(1, game.assembliesFormed());
            assertEquals(List.of("air"), game.state().assemblies());
        }

        @Test
        void rienNEstConsommeEtCEstUnAchatUnique() {
            Game game = game();
            give(game, "N2", 320);
            give(game, "O2", 84);
            give(game, "CO2", 4);
            BigNum occupied = game.occupiedSpace();
            double uncapBefore = game.elementUncap(7);
            assertTrue(game.formAssembly("air"));
            assertEquals(320, game.moleculeCount("N2"));
            assertEquals(320, game.gatheredMolecules("N2"));          // elles comptent toujours pour leur sorte
            assertEquals(uncapBefore, game.elementUncap(7), 0);
            assertValue(occupied.toDouble(), game.occupiedSpace());
            // 312 diazotes sont dans le bloc d'air, 8 restent dans leur amas.
            assertEquals(312, game.assembledMolecules("N2"));
            assertEquals(8, game.spareMolecules("N2"));
            assertEquals(0, game.spareMolecules("O2"));
            assertFalse(game.canFormAssembly("air"));
            assertFalse(game.formAssembly("air"));
            assertEquals(1, game.assembliesFormed());
        }

        @Test
        void deuxAssemblagesNeSePartagentPasLesMemesMolecules() {
            Game game = game();
            // Les rubis et saphirs demandent 300 corindons, le minerai d'aluminium 30 : il en faut 330 pour les deux.
            for (Map.Entry<String, Integer> ingredient : game.assembly("corundum_gems").ingredients().entrySet()) {
                give(game, ingredient.getKey(), ingredient.getValue());
            }
            for (Map.Entry<String, Integer> ingredient : game.assembly("aluminium_ore").ingredients().entrySet()) {
                if (!ingredient.getKey().equals("Al2O3") && !ingredient.getKey().equals("TiO2")) give(game, ingredient.getKey(), ingredient.getValue());
            }
            give(game, "TiO2", 12);                                   // deux pour les pierres, dix pour le minerai
            assertTrue(game.canFormAssembly("corundum_gems"));
            assertTrue(game.canFormAssembly("aluminium_ore"));
            assertTrue(game.formAssembly("corundum_gems"));
            assertEquals(0, game.spareMolecules("Al2O3"));
            assertFalse(game.canFormAssembly("aluminium_ore"));
            game.state().setMoleculeCount("Al2O3", 329);
            assertFalse(game.canFormAssembly("aluminium_ore"));
            game.state().setMoleculeCount("Al2O3", 330);
            assertTrue(game.formAssembly("aluminium_ore"));
            assertEquals(330, game.assembledMolecules("Al2O3"));
            assertEquals(12, game.assembledMolecules("TiO2"));
            assertEquals(2, game.assembliesFormed());
        }

        @Test
        void rienDeFabriqueDansLeCatalogue() {
            Game game = game();
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (Assembly assembly : game.assemblies()) {
                ids.add(assembly.id());
                assertTrue(assembly.ingredients().size() >= 3 && assembly.ingredients().size() <= 8, assembly.id());
                // Un alliage ou un carbure conducteur ne se trouve pas dans la nature : aucun métal dans un assemblage.
                for (String ingredient : assembly.ingredients().keySet()) {
                    assertTrue(game.molecule(ingredient).state() != Molecule.State.METAL, assembly.id() + " : " + ingredient);
                }
            }
            for (String made : List.of("batteries", "glass", "concrete", "alloys", "magnets", "plastics", "medicines", "solvents")) {
                assertFalse(ids.contains(made), made + " est fabrique");
            }
            assertTrue(ids.containsAll(List.of("granitic_rock", "copper_ore", "corundum_gems", "salt_water", "air", "petroleum")));
        }

        @Test
        void unAssemblageAugmenteLaGrandeurDeSaFamille() {
            Game game = game();
            give(game, "N2", 312);
            give(game, "O2", 84);
            give(game, "CO2", 4);
            double before = game.moleculeBoost(Molecule.Stat.SPACE);
            double atoms = game.moleculeBoost(Molecule.Stat.ATOMS);
            BigNum rate = game.spacePerSecond();
            assertTrue(game.formAssembly("air"));
            assertEquals(before + 2.0, game.moleculeBoost(Molecule.Stat.SPACE), 1e-9);     // 400 molécules : +200 %
            assertEquals(atoms, game.moleculeBoost(Molecule.Stat.ATOMS), 1e-12);
            assertTrue(game.spacePerSecond().gt(rate));
        }

        @Test
        void lesAssemblagesSurviventALExplosionEtAuBigBang() {
            Game game = game();
            give(game, "N2", 312);
            give(game, "O2", 84);
            give(game, "CO2", 4);
            assertTrue(game.formAssembly("air"));
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertTrue(game.hasAssembly("air"));
            game.state().setParticles(Game.BIG_BANG_PARTICLES);
            game.state().setAtoms(Game.BIG_BANG_ATOMS);
            game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER);
            for (Achievement achievement : game.achievements()) game.state().addAchievement(achievement.id());
            for (Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
            assertTrue(game.bigBang());
            GameTestSupport.payMilestones(game);
            assertTrue(game.hasAssembly("air"));
            assertTrue(game.isAssembliesUnlocked());
            game.reset();
            assertFalse(game.state().hasAssembly("air"));
            assertEquals(0, game.assembliesFormed());
            assertEquals(1.0, game.moleculeBoost(Molecule.Stat.SPACE), 0);
        }

        @Test
        void unAssemblageInconnuEstRefuse() {
            Game game = game();
            assertThrows(IllegalArgumentException.class, () -> game.formAssembly("phlogistique"));
            assertThrows(IllegalArgumentException.class, () -> game.hasAssembly("phlogistique"));
            assertThrows(IllegalArgumentException.class, () -> Assembly.of(Assembly.Family.ROCK, "x", "X", "rouge"));
            assertThrows(IllegalArgumentException.class, () -> Assembly.of(Assembly.Family.ROCK, "x", "X", "#aabbcc").with("SiO2", 0));
            assertThrows(IllegalArgumentException.class,
                    () -> Assembly.of(Assembly.Family.ROCK, "x", "X", "#aabbcc").with("SiO2", 3).with("SiO2", 4));
        }
    }

    @Nested
    class LesAstres {

        /** Après un premier Big Bang, tout ouvert jusqu'aux astres, beaucoup d'espace. */
        private Game game() {
            Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                    Achievements.DEFAULT, new Random(42));
            game.start();
            GameTestSupport.bangs(game, 1);
            game.state().setSpace(BigNum.of(1, 12));
            for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
                if (!(upgrade.effect() instanceof SpaceUpgrade.Boost)) game.state().addSpaceUpgrade(upgrade.id());
            }
            return game;
        }

        /** Donne au joueur tout ce que demande cet astre, sauf les astres plus petits : assemblages, matière, molécules. */
        private void supply(Game game, String bodyId) {
            Body body = game.body(bodyId);
            for (String assembly : body.assemblies()) game.state().addAssembly(assembly);
            for (Map.Entry<String, Integer> molecule : body.molecules().entrySet()) {
                game.state().setMoleculeCount(molecule.getKey(), Math.max(game.moleculeCount(molecule.getKey()), molecule.getValue()));
            }
            for (Map.Entry<Molecule.State, Integer> matter : body.matter().entrySet()) {
                // De la matière dans un état : des molécules rassemblées de la première sorte du catalogue qui a cet état.
                Molecule molecule = game.molecules().stream().filter(each -> each.state() == matter.getKey()).findFirst().orElseThrow();
                game.state().addSubstance(molecule.id());
                int missing = (int) Math.max(0, matter.getValue() - game.gatheredInState(matter.getKey()));
                if (missing > 0) game.state().setMoleculeCount(molecule.id(), game.moleculeCount(molecule.id()) + missing);
            }
            // Une matière exotique attend le huitième Big Bang.
            if (game.body(bodyId).tier() == Body.Tier.EXOTIC && game.state().bigBangs() < 8) GameTestSupport.bangs(game, 8);
            // Le trou noir supermassif demande aussi le cœur de galaxie, un upsilon du collisionneur.
            if (game.needsColliderEnergy(game.body(bodyId))) game.state().setColliderEnergyProduced(Math.max(game.colliderEnergyProduced(), Game.SUPERMASSIVE_ENERGY));
        }

        /** Forme cet astre, et d'abord ceux dont il part. */
        private void form(Game game, String bodyId) {
            for (String smaller : game.body(bodyId).bodies()) {
                if (!game.hasBody(smaller)) form(game, smaller);
                game.state().setMaturing(game.body(smaller).id(), 0);
            }
            supply(game, bodyId);
            assertTrue(game.formBody(bodyId), bodyId);
            game.state().setMaturing(game.body(bodyId).id(), 0);          // mûr tout de suite : la maturation a ses propres essais
        }

        /** Rassemble dans chaque état ce que demande cette échelle du cosmos, à cette part près. */
        private void gather(Game game, Cosmos scale, double share) {
            for (Map.Entry<Molecule.State, Integer> need : scale.matter().entrySet()) {
                Molecule molecule = game.molecules().stream().filter(each -> each.state() == need.getKey()).findFirst().orElseThrow();
                game.state().addSubstance(molecule.id());
                game.state().setMoleculeCount(molecule.id(), (int) Math.ceil(need.getValue() * share));
            }
            // La variété : avec toute la matière viennent aussi les sortes rassemblées que l'échelle demande.
            if (share < 1) return;
            for (Map.Entry<Molecule.State, Integer> need : scale.sorts().entrySet()) {
                game.molecules().stream().filter(each -> each.state() == need.getKey()).limit(need.getValue())
                        .forEach(each -> game.state().addSubstance(each.id()));
            }
        }

        @Test
        void apresLaGalaxieViennentLAmasDeGalaxiesPuisLUnivers() {
            Game game = game();
            assertEquals(List.of(Cosmos.GALAXY, Cosmos.CLUSTER, Cosmos.UNIVERSE), List.of(Cosmos.values()));
            // Depuis la 0.068, chaque échelle multiplie : ×4, puis ×2, puis ×2 ; l'univers demande quatre fois la matière de l'amas.
            assertEquals(4, Game.GALAXY_FACTOR, 0);
            assertEquals(Game.GALAXY_FACTOR, Cosmos.GALAXY.factor(), 0);
            assertEquals(2, Cosmos.CLUSTER.factor(), 0);
            assertEquals(2, Cosmos.UNIVERSE.factor(), 0);
            assertTrue(Cosmos.GALAXY.matter().isEmpty());
            for (Molecule.State state : Molecule.State.values()) {
                assertEquals(4L * Cosmos.CLUSTER.matter().get(state), (long) Cosmos.UNIVERSE.matter().get(state), state.name());
            }
            assertTrue(Cosmos.GALAXY.previous() == null);
            assertEquals(Cosmos.CLUSTER, Cosmos.GALAXY.next());
            assertTrue(Cosmos.UNIVERSE.next() == null);

            // Rien n'est en vue avant la galaxie, même avec toute la matière du monde.
            assertEquals(Cosmos.GALAXY, game.nextCosmos());
            assertEquals(0, game.cosmosFormed());
            gather(game, Cosmos.UNIVERSE, 1);
            assertFalse(game.isCosmosUnlocked(Cosmos.CLUSTER));
            assertFalse(game.canFormCosmos(Cosmos.CLUSTER));
            assertFalse(game.formCosmos(Cosmos.CLUSTER));
            assertEquals(10, game.cosmosConditionsMet(Cosmos.CLUSTER));    // la matière et la variété y sont, pas la galaxie
            assertEquals(11, game.cosmosConditions(Cosmos.CLUSTER));       // la galaxie, cinq quantités, cinq nombres de sortes
            assertEquals(game.bodies().size() + 1, game.cosmosConditions(Cosmos.GALAXY));   // les astres, et les systèmes solaires

            // La galaxie formée, l'amas de galaxies s'ouvre ; il lui faut chaque état en nombre.
            for (Body body : game.bodies()) game.state().addBody(body.id()); Systemes.trois(game);
            gather(game, Cosmos.CLUSTER, 0.5);
            assertTrue(game.formGalaxy());
            assertTrue(game.hasCosmos(Cosmos.GALAXY));
            assertEquals(Cosmos.CLUSTER, game.nextCosmos());
            assertTrue(game.isCosmosUnlocked(Cosmos.CLUSTER) && !game.isCosmosUnlocked(Cosmos.UNIVERSE));
            assertEquals(6, game.cosmosConditionsMet(Cosmos.CLUSTER));     // la galaxie et les sortes ; la moitié de la matière ne suffit pas
            assertFalse(game.canFormCosmos(Cosmos.CLUSTER));
            assertEquals(0, game.cosmosMilestones());                      // aucun état réuni : aucun jalon
            gather(game, Cosmos.CLUSTER, 1);
            Molecule gas = game.molecules().stream().filter(each -> each.state() == Molecule.State.GAS).findFirst().orElseThrow();
            game.state().setMoleculeCount(gas.id(), Cosmos.CLUSTER.matter().get(Molecule.State.GAS) - 1);    // il en manque une
            assertEquals(10, game.cosmosConditionsMet(Cosmos.CLUSTER));
            // Les jalons (0.068) : un par état réuni, +10 % à toutes les grandeurs chacun.
            assertEquals(4, game.cosmosMilestones());
            assertFalse(game.isCosmosMilestoneMet(Cosmos.CLUSTER, Molecule.State.GAS));
            assertTrue(game.isCosmosMilestoneMet(Cosmos.CLUSTER, Molecule.State.LIQUID));
            double withFour = game.matterBoost(Molecule.Stat.ATOMS);
            assertFalse(game.formCosmos(Cosmos.CLUSTER));
            game.state().setMoleculeCount(gas.id(), Cosmos.CLUSTER.matter().get(Molecule.State.GAS));
            assertEquals(5, game.cosmosMilestones());
            assertEquals(withFour * 1.1, game.matterBoost(Molecule.Stat.ATOMS), withFour * 0.02);   // le gaz ajoute aussi un peu de matière lui-même
            assertTrue(game.matterBoost(Molecule.Stat.ATOMS) >= withFour * 1.1 - 1e-9);
            // Le délai de rassemblement (0.073) : deux heures après la galaxie depuis la 0.125.
            double galaxyAt = game.stats().reachedAt(GameStats.Step.GALAXY);
            assertEquals(Game.COSMOS_SETTLE_SECONDS, game.cosmosSettleRemaining(Cosmos.CLUSTER), 1e-6);
            assertFalse(game.canFormCosmos(Cosmos.CLUSTER));
            game.state().setTimePlayed(galaxyAt + Game.COSMOS_SETTLE_SECONDS);
            assertEquals(0, game.cosmosSettleRemaining(Cosmos.CLUSTER), 1e-6);
            assertTrue(game.canFormCosmos(Cosmos.CLUSTER));
            assertFalse(game.canFormCosmos(Cosmos.UNIVERSE));             // pas d'univers sans l'amas
            double[] before = new double[Molecule.Stat.values().length];
            for (Molecule.Stat stat : Molecule.Stat.values()) before[stat.ordinal()] = game.matterBoost(stat);
            long molecules = game.moleculesCreated();
            double clusterAt = galaxyAt + Game.COSMOS_SETTLE_SECONDS + 100, universeAt = clusterAt + Game.COSMOS_SETTLE_SECONDS;
            game.state().setTimePlayed(clusterAt);
            assertTrue(game.formCosmos(Cosmos.CLUSTER));
            assertFalse(game.formCosmos(Cosmos.CLUSTER));
            // Rien n'est consommé, et toutes les grandeurs doublent (0.068 : avant, l'amas ajoutait 2 048).
            assertEquals(molecules, game.moleculesCreated());
            for (Molecule.Stat stat : Molecule.Stat.values()) {
                assertEquals(before[stat.ordinal()] * 2, game.matterBoost(stat), before[stat.ordinal()] * 1e-9, stat.name());
            }
            assertEquals(clusterAt, game.stats().reachedAt(GameStats.Step.CLUSTER), 1e-9);
            assertFalse(game.stats().reached(GameStats.Step.UNIVERSE));

            // L'univers : quatre fois plus, et c'est la fin.
            assertEquals(Cosmos.UNIVERSE, game.nextCosmos());
            assertEquals(6, game.cosmosConditionsMet(Cosmos.UNIVERSE));    // l'amas et les sortes, réunies plus haut
            gather(game, Cosmos.UNIVERSE, 1);
            for (Molecule.Stat stat : Molecule.Stat.values()) before[stat.ordinal()] = game.matterBoost(stat);
            game.state().setTimePlayed(universeAt - 800);
            assertFalse(game.formCosmos(Cosmos.UNIVERSE));                 // l'amas, formé il y a moins de deux heures, se rassemble encore
            game.state().setTimePlayed(universeAt);
            // L'univers à la main, sans sa remise à zéro (version 0.083) : il double toutes les grandeurs.
            String atCluster = idle.core.save.SaveCodec.write(game.state(), 0);
            game.state().setCosmosLevel(3);
            assertEquals(3, game.cosmosFormed());
            assertTrue(game.nextCosmos() == null);
            assertEquals(11, game.cosmosConditionsMet(Cosmos.UNIVERSE));
            assertEquals(10, game.cosmosMilestones());
            for (Molecule.Stat stat : Molecule.Stat.values()) {
                assertEquals(before[stat.ordinal()] * 2, game.matterBoost(stat), before[stat.ordinal()] * 1e-9, stat.name());
            }
            // Ni l'explosion ni le Big Bang ne les défont ; défaire la galaxie défait tout ce qui est au-dessus.
            game.state().clearMatter();
            game.state().clearDarkMatter();
            assertEquals(3, game.cosmosFormed());
            game.state().setGalaxy(false);
            assertEquals(0, game.cosmosFormed());
            assertFalse(game.hasGalaxy());
            assertThrows(IllegalArgumentException.class, () -> game.state().setCosmosLevel(4));

            // Pour de bon (version 0.083) : former l'univers remet tout à zéro, sauf ce qui appartient à l'univers.
            try {
                idle.core.save.SaveCodec.readInto(game.state(), atCluster);
            } catch (Exception unreadable) {
                throw new AssertionError(unreadable);
            }
            game.restored();
            int bangs = game.bigBangs();
            long createdBefore = game.moleculesCreated();
            game.state().setTimePlayed(universeAt);
            game.testAddVoidEnergy(BigNum.of(5));
            assertTrue(bangs > 0 && createdBefore > 0);
            int achievementsBefore = game.achievementCount();
            assertTrue(game.formCosmos(Cosmos.UNIVERSE));
            assertTrue(game.hasCosmos(Cosmos.UNIVERSE) && game.state().universeFormed());
            assertEquals(1, game.universes());
            assertEquals(0, game.cosmosFormed());                          // le cosmos repart de rien
            assertFalse(game.hasGalaxy());
            assertEquals(0, game.bigBangs());
            assertEquals(0, game.moleculesCreated());
            assertTrue(game.state().darkMatter().sign() == 0 && game.state().particles().sign() == 0);
            assertEquals(0, game.state().explosions());
            assertTrue(game.isStarted());                                  // pas d'écran de démarrage
            assertEquals(universeAt, game.state().timePlayed(), 1e-9);           // le temps de jeu reste
            assertEquals(universeAt, game.stats().reachedAt(GameStats.Step.UNIVERSE), 1e-9);
            assertTrue(game.achievementCount() >= achievementsBefore);     // les succès restent
            assertEquals(BigNum.of(Game.SEEDS_PER_UNIVERSE), game.state().universeSeeds());
            // Version 0.129 : le vide repart de zéro à chaque univers ; fermé, il n'ajoute rien au cumul.
            assertTrue(game.state().voidEnergy().isZero() && game.state().voidHistory().isEmpty());
            long[] tally = game.stats().universeTally();
            assertEquals(bangs, tally[0]);
            assertEquals(createdBefore, tally[1]);
            assertEquals(1.0, game.offlineRate(), 1e-12);                  // l'absence compte toujours en entier
            // Relue, la partie reste dans l'univers et ne se remet pas à zéro une seconde fois.
            GameState back;
            try {
                back = idle.core.save.SaveCodec.read(idle.core.save.SaveCodec.write(game.state(), 0)).state();
            } catch (Exception unreadable) {
                throw new AssertionError(unreadable);
            }
            assertTrue(back.universeFormed());
            assertEquals(BigNum.of(Game.SEEDS_PER_UNIVERSE), back.universeSeeds());
        }

        @Test
        void uneSauvegardeDAvantLaRemiseAZeroDeLUniversSeRemetAZeroAuChargement() {
            // Une partie d'avant la 0.083 : l'univers formé, sans la clé universeFormed, et tout ce qui y a mené.
            Game game = game();
            game.state().setCosmosLevel(3);
            GameTestSupport.bangs(game, 8);
            game.state().setTimePlayed(40_000);
            String text = idle.core.save.SaveCodec.write(game.state(), 0).replace("universeFormed", "ancienneCle");
            Game loaded = game();
            try {
                idle.core.save.SaveCodec.readInto(loaded.state(), text);
            } catch (Exception unreadable) {
                throw new AssertionError(unreadable);
            }
            assertTrue(loaded.state().universeFormed());
            loaded.restored();
            assertTrue(loaded.hasCosmos(Cosmos.UNIVERSE));
            assertEquals(0, loaded.cosmosFormed());
            assertEquals(0, loaded.bigBangs());
            assertEquals(8, loaded.stats().universeTally()[0]);
            assertEquals(40_000, loaded.state().timePlayed(), 1e-9);
            assertEquals(BigNum.of(Game.SEEDS_PER_UNIVERSE), loaded.state().universeSeeds());
            loaded.restored();                                             // une seule fois
            assertEquals(BigNum.of(Game.SEEDS_PER_UNIVERSE), loaded.state().universeSeeds());
        }

        @Test
        void lesUniversEtLeursDimensionsSansLogique() {
            Game game = game();
            assertEquals(0, game.universes());
            assertFalse(game.isDimensionsUnlocked());             // pas avant l'univers
            assertFalse(game.testAddUniverse());                  // pas d'univers à dupliquer avant le premier
            game.state().setCosmosLevel(3);
            assertEquals(1, game.universes());
            assertTrue(game.isDimensionsUnlocked());              // dès le premier univers (version 0.100)
            assertEquals(1, game.dimensionsUnlocked());
            assertTrue(game.testAddUniverse());
            assertEquals(2, game.universes());
            assertEquals(2, game.dimensionsUnlocked());           // une de plus par univers
            for (int k = 0; k < 10; k++) game.testAddUniverse();
            assertEquals(12, game.universes());
            assertEquals(Game.MAX_DIMENSIONS, game.dimensionsUnlocked());
            assertTrue(game.testRemoveUniverse());
            assertEquals(11, game.universes());
            game.testAddUniverseSeeds(BigNum.of(10));
            game.testAddVoidEnergy(BigNum.of(3));
            game.testRaiseUniverseUpgrade(idle.core.universe.UniverseUpgrade.BRANE);
            assertEquals(1, game.universeUpgradeLevel(idle.core.universe.UniverseUpgrade.BRANE));
            assertEquals(10, game.state().universeSeeds().toDouble(), 1e-9);
            assertEquals(4, game.universeUpgrades().size());
            game.testResetUniverses();
            assertEquals(1, game.universes());
            assertTrue(game.state().universeSeeds().isZero() && game.state().voidEnergy().isZero());
            assertEquals(0, game.universeUpgradeLevel(idle.core.universe.UniverseUpgrade.BRANE));
            assertFalse(game.testRemoveUniverse());
        }

        @Test
        void leNombreDeMoleculesDepasseDeuxMilliardsEnTout() {
            Game game = game();
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 9);
            game.state().setSpace(BigNum.of(1, 30));
            Molecule gas = game.molecules().stream().filter(each -> each.state() == Molecule.State.GAS).findFirst().orElseThrow();
            List<Molecule> liquids = game.molecules().stream().filter(each -> each.state() == Molecule.State.LIQUID).limit(2).toList();
            Molecule liquid = liquids.get(0);
            Molecule other = liquids.get(1);
            game.state().addSubstance(gas.id());
            game.state().addSubstance(liquid.id());
            game.state().addSubstance(other.id());
            game.state().setMoleculeCount(gas.id(), Game.MAX_MOLECULES);
            game.state().setMoleculeCount(liquid.id(), Game.MAX_MOLECULES);
            game.state().setMoleculeCount(other.id(), Game.MAX_MOLECULES - 5);
            // Deux milliards en tout ne bloquent plus rien : seule une sorte pleine (un milliard) s'arrête.
            assertFalse(game.canCreateMolecule(liquid.id()));
            assertTrue(game.isMoleculeKindFull(liquid.id()));
            assertEquals(0, game.moleculeGain(liquid.id()));
            assertFalse(game.isMoleculeKindFull(other.id()));
            assertEquals(5, game.moleculesPerCreation(other.id()));
            assertTrue(game.createMolecule(other.id()));
            assertEquals(3L * Game.MAX_MOLECULES, game.moleculesCreated());
            assertEquals(2L * Game.MAX_MOLECULES, game.gatheredInState(Molecule.State.LIQUID));
            assertEquals(gas.id(), game.state().moleculeAt(0));
            assertEquals(other.id(), game.state().moleculeAt(3L * Game.MAX_MOLECULES - 1));
            assertEquals(Integer.MAX_VALUE, game.state().moleculeLog().size());
            assertFalse(game.canCreateMolecule(other.id()));
        }

        @Test
        void leCatalogueVaDeLAmasALaPlanete() {
            Map<Body.Tier, Integer> tiers = new EnumMap<>(Body.Tier.class);
            java.util.Set<String> ids = new java.util.HashSet<>();
            Game game = game();
            for (Body body : Bodies.DEFAULT) {
                assertTrue(ids.add(body.id()), body.id() + " en double");
                tiers.merge(body.tier(), 1, Integer::sum);
                // Les matières exotiques sont à part : sans assemblage, elles partent d'astres déjà formés (MaturationTest… et essais du lot 54).
                if (body.tier() == Body.Tier.EXOTIC) continue;
                // Un cumul : des assemblages, de la matière et des molécules, toujours les trois.
                assertFalse(body.assemblies().isEmpty(), body.id());
                assertFalse(body.matter().isEmpty(), body.id());
                assertFalse(body.molecules().isEmpty(), body.id());
                // Au-delà des amas, un astre part d'au moins un astre de l'échelle juste en dessous.
                if (body.tier() == Body.Tier.RUBBLE) {
                    assertTrue(body.bodies().isEmpty(), body.id());
                } else {
                    Body.Tier below = Body.Tier.values()[body.tier().ordinal() - (body.tier() == Body.Tier.CORE ? 2 : 1)];
                    assertTrue(body.bodies().stream().anyMatch(each -> game.body(each).tier() == below), body.id());
                }
            }
            assertEquals(35, Bodies.DEFAULT.size());
            assertEquals(Map.of(Body.Tier.RUBBLE, 4, Body.Tier.COMET, 3, Body.Tier.ASTEROID, 4, Body.Tier.MOON, 4,
                    Body.Tier.PLANET, 6, Body.Tier.STAR, 5, Body.Tier.REMNANT, 2, Body.Tier.BLACK_HOLE, 1, Body.Tier.EXOTIC, 5,
                    Body.Tier.CORE, 1), tiers);
            // Le catalogue finit par le trou noir supermassif, seul dans son échelle.
            assertEquals("supermassive_black_hole", Bodies.DEFAULT.get(Bodies.DEFAULT.size() - 1).id());
        }

        @Test
        void uneEtoileEstPlusGrosseQuUnePlanete() {
            // Dans l'espace, un astre a la taille de ses assemblages : une étoile, même faite d'un seul, dépasse les planètes.
            Game game = game();
            int largestPlanet = 0;
            for (Body body : game.bodies()) {
                int size = 0;
                for (String assembly : body.assemblies()) size += game.assembly(assembly).size();
                if (body.tier() == Body.Tier.PLANET) largestPlanet = Math.max(largestPlanet, size);
            }
            int sun = game.assembly("solar_plasma").size();
            assertTrue(sun > 0.9 * largestPlanet, sun + " contre " + largestPlanet);
            int core = 0;
            for (String assembly : game.body("supermassive_black_hole").assemblies()) core += game.assembly(assembly).size();
            for (Body body : game.bodies()) {
                int size = 0;
                for (String assembly : body.assemblies()) size += game.assembly(assembly).size();
                assertTrue(size <= core, body.id() + " est plus gros que le trou noir supermassif");
            }
        }

        @Test
        void lAppuiAutomatiqueFaitGrossirLaMatiereNoireSansClic() {
            Game game = game();
            // Sans l'amélioration : rien ne grossit seul.
            Game bare = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                    Achievements.DEFAULT, new Random(42));
            bare.start();
            GameTestSupport.bangs(bare, 1);
            bare.state().setExplosions(1);
            bare.state().setDarkMatter(BigNum.of(5));
            assertFalse(bare.isAutoHoldUnlocked() || bare.isAutoHolding());
            assertFalse(bare.setAutoHoldEnabled(false));
            BigNum start = bare.state().darkMatterSize();
            bare.tick(60);
            assertEquals(start.toDouble(), bare.state().darkMatterSize().toDouble(), 0);
            // Acquise, mais avant la première explosion : elle attend la matière noire.
            assertTrue(game.isAutoHoldUnlocked() && game.isAutoHoldEnabled());
            assertFalse(game.isAutoHolding());
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(5));
            assertTrue(game.isAutoHolding());
            // Une minute de jeu vaut une minute d'appui : la même taille qu'en tenant le clic.
            Game held = game();
            held.state().setExplosions(1);
            held.state().setDarkMatter(BigNum.of(5));
            held.setAutoHoldEnabled(false);
            assertFalse(held.isAutoHolding());
            held.growDarkMatter(60);
            game.tick(60);
            assertEquals(held.state().darkMatterSize().log10(), game.state().darkMatterSize().log10(), 1e-6);
            assertTrue(game.state().darkMatterSize().gt(start));
            // Coupé, il ne fait plus rien ; remis, il reprend.
            assertTrue(game.setAutoHoldEnabled(false));
            BigNum paused = game.state().darkMatterSize();
            game.tick(60);
            assertEquals(paused.toDouble(), game.state().darkMatterSize().toDouble(), 0);
            assertTrue(game.setAutoHoldEnabled(true));
            game.tick(60);
            assertTrue(game.state().darkMatterSize().gt(paused));
            // Remise à zéro : l'amélioration est perdue, le réglage revient à « en marche ».
            game.setAutoHoldEnabled(false);
            game.reset();
            assertFalse(game.isAutoHoldUnlocked());
            assertTrue(game.isAutoHoldEnabled());
        }

        @Test
        void laGalaxieDemandeTousLesAstres() {
            Game game = game();
            assertFalse(game.isGalaxyUnlocked() || game.hasGalaxy() || game.canFormGalaxy());
            assertFalse(game.formGalaxy());
            // Jusqu'aux planètes, la galaxie n'est pas encore en vue ; elle l'est à la première étoile.
            for (Body body : game.bodies()) {
                if (body.tier().ordinal() <= Body.Tier.PLANET.ordinal() && !game.hasBody(body.id())) form(game, body.id());
            }
            assertFalse(game.isGalaxyUnlocked());
            form(game, "brown_dwarf");
            assertTrue(game.isGalaxyUnlocked());
            assertEquals(1, game.bodiesFormed(Body.Tier.STAR));
            assertEquals(5, game.bodiesIn(Body.Tier.STAR));
            assertEquals(6, game.bodiesFormed(Body.Tier.PLANET));
            // Il manque encore des astres : pas de galaxie, même avec le trou noir supermassif.
            for (Body body : game.bodies()) {
                if (!List.of("white_dwarf", "planetary_nebula", "supermassive_black_hole").contains(body.id()) && !game.hasBody(body.id())) {
                    form(game, body.id());
                }
            }
            assertEquals(32, game.bodiesFormed());
            assertFalse(game.canFormGalaxy());
            form(game, "supermassive_black_hole");
            form(game, "planetary_nebula");
            assertEquals(35, game.bodiesFormed());
            // Tous les astres, mais pas encore de systèmes solaires : pas de galaxie.
            assertFalse(game.canFormGalaxy());
            Systemes.trois(game);
            assertTrue(game.canFormGalaxy());
            double[] before = new double[Molecule.Stat.values().length];
            for (Molecule.Stat stat : Molecule.Stat.values()) before[stat.ordinal()] = game.moleculeBoost(stat);
            long molecules = game.moleculesCreated();
            assertFalse(game.stats().reached(GameStats.Step.GALAXY));
            assertTrue(game.stats().reached(GameStats.Step.FIRST_BODY));
            double firstBody = game.stats().reachedAt(GameStats.Step.FIRST_BODY);
            game.state().setTimePlayed(firstBody + 9000);
            assertTrue(game.formGalaxy());
            assertEquals(firstBody + 9000, game.stats().reachedAt(GameStats.Step.GALAXY), 1e-9);
            assertEquals(firstBody, game.stats().reachedAt(GameStats.Step.FIRST_BODY), 1e-9);
            assertTrue(game.hasGalaxy() && !game.canFormGalaxy() && !game.formGalaxy());
            // Rien n'est consommé, et toutes les grandeurs sont multipliées d'autant (0.068 : avant, la galaxie s'y ajoutait).
            assertEquals(35, game.bodiesFormed());
            assertEquals(molecules, game.moleculesCreated());
            assertEquals(0, game.cosmosMilestones());
            for (Molecule.Stat stat : Molecule.Stat.values()) {
                // L'espace : la matière exotique a demandé le huitième Big Bang, dont les paliers multiplient aussi l'espace.
                if (stat == Molecule.Stat.SPACE) {
                    assertTrue(game.moleculeBoost(stat) >= before[stat.ordinal()] * Game.GALAXY_FACTOR, stat.name());
                    continue;
                }
                assertEquals(before[stat.ordinal()] * Game.GALAXY_FACTOR, game.moleculeBoost(stat), before[stat.ordinal()] * 1e-9, stat.name());
            }
        }

        @Test
        void laGalaxieSurvitALExplosionEtAuBigBangMaisPasALaRemiseAZero() {
            Game game = game();
            for (Body body : game.bodies()) {
                if (!game.hasBody(body.id())) form(game, body.id());
            }
            Systemes.trois(game);
            int version = game.state().moleculesVersion();
            assertTrue(game.formGalaxy());
            assertTrue(game.state().moleculesVersion() != version, "la vue de l'espace doit se refaire");
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertTrue(game.hasGalaxy());
            game.state().setParticles(Game.BIG_BANG_PARTICLES);
            game.state().setAtoms(Game.BIG_BANG_ATOMS);
            game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER);
            for (Achievement achievement : game.achievements()) game.state().addAchievement(achievement.id());
            for (Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
            assertTrue(game.bigBang());
            GameTestSupport.payMilestones(game);
            assertTrue(game.hasGalaxy() && game.isGalaxyUnlocked());
            assertEquals(35, game.bodiesFormed());
            game.reset();
            assertFalse(game.hasGalaxy() || game.state().hasGalaxy());
        }

        @Test
        void lesListesDeCeQuiEstFormeSuiventLesAjoutsEtLaRemiseAZero() {
            // Ces listes sont demandées à chaque image : elles ne sont recopiées qu'après un changement,
            // et doivent pourtant toujours dire l'état du moment.
            GameState state = new GameState();
            List<String> empty = state.substances();
            assertTrue(empty.isEmpty() && state.assemblies().isEmpty() && state.bodies().isEmpty());
            state.addSubstance("H2O");
            state.addAssembly("air");
            state.addBody("rocky_rubble");
            assertTrue(empty.isEmpty(), "une liste deja rendue ne change pas apres coup");
            assertEquals(List.of("H2O"), state.substances());
            assertEquals(List.of("air"), state.assemblies());
            assertEquals(List.of("rocky_rubble"), state.bodies());
            assertTrue(state.substances() == state.substances(), "sans changement, la meme liste est rendue");
            state.addSubstance("CO2");
            state.addSubstance("CO2");
            assertEquals(List.of("H2O", "CO2"), state.substances());
            assertThrows(UnsupportedOperationException.class, () -> state.substances().add("N2"));
            state.reset();
            assertTrue(state.substances().isEmpty() && state.assemblies().isEmpty() && state.bodies().isEmpty());
            // Remise à zéro puis autant d'ajouts qu'avant : ce sont bien les nouveaux qui sont rendus.
            state.addSubstance("NH3");
            state.addAssembly("petroleum");
            state.addBody("icy_rubble");
            assertEquals(List.of("NH3"), state.substances());
            assertEquals(List.of("petroleum"), state.assemblies());
            assertEquals(List.of("icy_rubble"), state.bodies());
        }

        @Test
        void lesCataloguesSontRendusTelsQuels() {
            Game game = game();
            assertTrue(game.bodies() == game.bodies() && game.assemblies() == game.assemblies()
                    && game.spaceUpgrades() == game.spaceUpgrades());
            assertEquals(35, game.bodies().size());
            assertEquals(58, game.assemblies().size());
            assertThrows(UnsupportedOperationException.class, () -> game.bodies().remove(0));
            assertThrows(UnsupportedOperationException.class, () -> game.assemblies().remove(0));
            assertThrows(UnsupportedOperationException.class, () -> game.spaceUpgrades().remove(0));
        }

        @Test
        void chaqueAssemblageEntreDansUnSeulAstre() {
            Game game = game();
            Map<String, String> user = new java.util.HashMap<>();
            for (Body body : game.bodies()) {
                for (String assembly : body.assemblies()) {
                    assertTrue(user.put(assembly, body.id()) == null, assembly + " sert a deux astres");
                    assertEquals(body, game.bodyOf(assembly));
                }
                // Un pour un amas, deux pour une comète, un astéroïde ou une lune, trois pour une planète.
                int expected = switch (body.tier()) {
                    case RUBBLE -> 1;
                    case STAR -> 1;
                    case COMET, ASTEROID, MOON, REMNANT, BLACK_HOLE -> 2;
                    case PLANET, CORE -> 3;
                    case EXOTIC -> 0;                                       // les matières exotiques : sans assemblage
                };
                assertEquals(expected, body.assemblies().size(), body.id());
            }
            // Les 58 assemblages du catalogue ont tous leur astre.
            assertEquals(Assemblies.DEFAULT.size(), user.size());
            for (Assembly assembly : game.assemblies()) assertTrue(user.containsKey(assembly.id()), assembly.id());
            assertEquals("rocky_rubble", game.bodyOf("sandy_rock").id());
            assertEquals("ocean_planet", game.bodyOf("salt_water").id());
            assertThrows(IllegalArgumentException.class, () -> game.bodyOf("phlogistique"));
        }

        @Test
        void plusUnAstreEstGrosPlusIlDemandeEtPlusIlDonne() {
            Game game = game();
            int[] least = new int[Body.Tier.values().length];
            int[] most = new int[Body.Tier.values().length];
            java.util.Arrays.fill(least, Integer.MAX_VALUE);
            for (Body body : game.bodies()) {
                if (body.tier() == Body.Tier.EXOTIC) continue;                // à part : voir plus bas
                int asked = 0;
                for (int count : body.matter().values()) asked += count;
                least[body.tier().ordinal()] = Math.min(least[body.tier().ordinal()], asked);
                most[body.tier().ordinal()] = Math.max(most[body.tier().ordinal()], asked);
                assertEquals(body.tier().gain(), body.boost().perMolecule(), 0);
            }
            List<Body.Tier> ladder = new java.util.ArrayList<>(List.of(Body.Tier.values()));
            ladder.remove(Body.Tier.EXOTIC);
            for (int at = 1; at < ladder.size(); at++) {
                int tier = ladder.get(at).ordinal(), below = ladder.get(at - 1).ordinal();
                assertTrue(least[tier] > most[below] * 0.9, ladder.get(at) + " : " + least[tier] + " contre " + most[below]);
                assertEquals(2 * ladder.get(at - 1).gain(), ladder.get(at).gain(), 0);
            }
            // Les matières exotiques : entre les trous noirs et le cœur, en gain comme en matière demandée.
            assertTrue(Body.Tier.EXOTIC.gain() > Body.Tier.BLACK_HOLE.gain() && Body.Tier.EXOTIC.gain() < Body.Tier.CORE.gain());
            assertTrue(least[0] >= 300, "un amas demande des centaines de molecules : " + least[0]);
            assertTrue(least[4] >= 4000, "une planete en demande des milliers : " + least[4]);
        }

        @Test
        void pasDAstreSansLAmeliorationDEspace() {
            Game game = game();
            supply(game, "rocky_rubble");
            assertTrue(game.isBodiesUnlocked());
            assertTrue(game.canFormBody("rocky_rubble"));
            Game closed = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                    Achievements.DEFAULT, new Random(42));
            closed.start();
            GameTestSupport.bangs(closed, 1);
            closed.state().setSpace(BigNum.of(100_000));
            closed.state().addSpaceUpgrade("space_states");
            supply(closed, "rocky_rubble");
            assertFalse(closed.isBodiesUnlocked());
            assertFalse(closed.canFormBody("rocky_rubble"));
            assertFalse(closed.formBody("rocky_rubble"));
            // L'amélioration vient après celle des assemblages, à 100 000 d'espace créé.
            assertFalse(closed.isSpaceUpgradeAvailable("space_bodies"));
            assertTrue(closed.buySpaceUpgrade("space_assemblies"));
            assertTrue(closed.buySpaceUpgrade("space_bodies"));
            assertTrue(closed.formBody("rocky_rubble"));
        }

        @Test
        void ilFautLesAssemblagesLaMatiereEtLesMolecules() {
            Game game = game();
            Body rubble = game.body("rocky_rubble");
            assertEquals(3, rubble.conditions());                     // un assemblage, une matière, une molécule
            assertEquals(0, game.bodyConditionsMet("rocky_rubble"));
            for (String assembly : rubble.assemblies()) game.state().addAssembly(assembly);
            assertEquals(1, game.bodyConditionsMet("rocky_rubble"));
            assertFalse(game.canFormBody("rocky_rubble"));
            // 250 quartz créés mais pas rassemblés : la molécule y est, la matière non.
            game.state().setMoleculeCount("SiO2", 250);
            assertEquals(2, game.bodyConditionsMet("rocky_rubble"));
            assertEquals(0, game.gatheredInState(Molecule.State.CRYSTAL));
            game.state().addSubstance("SiO2");
            assertEquals(250, game.gatheredInState(Molecule.State.CRYSTAL));
            assertFalse(game.canFormBody("rocky_rubble"));
            // La matière compte toutes les sortes de cristaux rassemblés : 150 sels de table font le compte.
            game.state().setMoleculeCount("NaCl", 149);
            game.state().addSubstance("NaCl");
            assertFalse(game.canFormBody("rocky_rubble"));
            game.state().setMoleculeCount("NaCl", 150);
            assertEquals(400, game.gatheredInState(Molecule.State.CRYSTAL));
            assertTrue(game.canFormBody("rocky_rubble"));
            assertTrue(game.formBody("rocky_rubble"));
            assertTrue(game.hasBody("rocky_rubble"));
        }

        @Test
        void uneCometePartDUnAmas() {
            Game game = game();
            supply(game, "icy_comet");
            assertEquals(game.body("icy_comet").conditions() - 1, game.bodyConditionsMet("icy_comet"));
            assertFalse(game.canFormBody("icy_comet"));               // il manque l'amas glacé
            supply(game, "icy_rubble");
            assertTrue(game.formBody("icy_rubble"));
            assertFalse(game.canFormBody("icy_comet"), "l'amas glacé mûrit encore");
            game.state().setMaturing("icy_rubble", 0);
            assertTrue(game.isBodyMature("icy_rubble"));
            assertTrue(game.canFormBody("icy_comet"), game.bodyConditionsMet("icy_comet") + "/" + game.bodyConditions("icy_comet"));
            assertTrue(game.formBody("icy_comet"));
            assertEquals(List.of("icy_rubble", "icy_comet"), game.state().bodies());
        }

        @Test
        void rienNEstConsommeEtCEstUnAchatUnique() {
            Game game = game();
            supply(game, "rocky_rubble");
            int quartz = game.moleculeCount("SiO2");
            long crystals = game.gatheredInState(Molecule.State.CRYSTAL);
            assertTrue(game.formBody("rocky_rubble"));
            assertEquals(quartz, game.moleculeCount("SiO2"));
            assertEquals(crystals, game.gatheredInState(Molecule.State.CRYSTAL));
            assertTrue(game.hasAssembly("sandy_rock"));               // l'assemblage reste formé, dans l'astre
            assertFalse(game.canFormBody("rocky_rubble"));
            assertFalse(game.formBody("rocky_rubble"));
            assertEquals(1, game.bodiesFormed());
            // Ce qui a servi à l'amas rocheux sert encore à la comète poussiéreuse : rien n'est réservé.
            form(game, "dusty_comet");
            assertEquals(3, game.bodiesFormed());
        }

        @Test
        void unAstreAugmenteSaGrandeurDeCeQueVautSonEchelle() {
            Game game = game();
            supply(game, "rocky_rubble");
            double atoms = game.moleculeBoost(Molecule.Stat.ATOMS);
            assertTrue(game.formBody("rocky_rubble"));
            assertEquals(atoms + 2 * Game.MATURING_SHARE, game.moleculeBoost(Molecule.Stat.ATOMS), 1e-9);   // en maturation : un quart
            game.state().setMaturing("rocky_rubble", 0);
            assertEquals(atoms + 2, game.moleculeBoost(Molecule.Stat.ATOMS), 1e-9);            // un amas : +200 %
            assertEquals(32, game.body("ocean_planet").boost().perMolecule(), 0);             // une planète : +3 200 %
        }

        @Test
        void toutLeCatalogueSeFormeDuPlusPetitAuPlusGrand() {
            Game game = game();
            for (Body body : game.bodies()) {
                if (!game.hasBody(body.id())) form(game, body.id());
            }
            assertEquals(35, game.bodiesFormed());
            assertTrue(game.hasBody("gas_giant_planet") && game.hasBody("yellow_sun") && game.hasBody("supermassive_black_hole"));
            assertTrue(game.moleculeBoost(Molecule.Stat.SPACE) > 60);
        }

        @Test
        void lesAstresSurviventALExplosionEtAuBigBang() {
            Game game = game();
            form(game, "icy_comet");
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
            assertTrue(game.explode());
            assertTrue(game.hasBody("icy_comet"));
            game.state().setParticles(Game.BIG_BANG_PARTICLES);
            game.state().setAtoms(Game.BIG_BANG_ATOMS);
            game.state().setDarkMatter(Game.BIG_BANG_DARK_MATTER);
            for (Achievement achievement : game.achievements()) game.state().addAchievement(achievement.id());
            for (Challenge challenge : game.challenges()) game.state().addCompletedChallenge(challenge.id());
            assertTrue(game.bigBang());
            GameTestSupport.payMilestones(game);
            assertTrue(game.hasBody("icy_comet"));
            assertTrue(game.isBodiesUnlocked());
            game.reset();
            assertEquals(0, game.bodiesFormed());
            assertFalse(game.state().hasBody("icy_comet"));
        }

        @Test
        void unAstreInconnuEstRefuse() {
            Game game = game();
            assertThrows(IllegalArgumentException.class, () -> game.formBody("phlogistique"));
            assertThrows(IllegalArgumentException.class, () -> game.hasBody("phlogistique"));
            Body bare = Body.of(Body.Tier.MOON, "x", "X", Body.Look.ROCKY, "#aabbcc", "#112233", Molecule.Stat.ATOMS);
            assertEquals(0, bare.conditions());
            assertThrows(IllegalArgumentException.class, () -> bare.matter(Molecule.State.GAS, 0));
            assertThrows(IllegalArgumentException.class, () -> bare.molecules("H2O", 3).molecules("H2O", 4));
            assertThrows(IllegalArgumentException.class,
                    () -> Body.of(Body.Tier.MOON, "x", "X", Body.Look.ROCKY, "gris", "#112233", Molecule.Stat.ATOMS));
        }
    }
}

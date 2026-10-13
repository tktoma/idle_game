package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Le collisionneur de quarks : énergie, collisions, résonance, hadrons et leurs bonus, quark top. */
class CollisionneurTest {

    private static Game newGame() {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        game.start();
        return game;
    }

    /** Après une première explosion, avec de l'énergie à volonté. */
    private static Game opened(double energy) {
        Game game = newGame();
        game.state().setExplosions(1);
        game.state().setColliderEnergy(energy);
        return game;
    }

    @Nested
    class Ouverture {

        @Test
        void ilSOuvreALaPremiereExplosion() {
            Game game = newGame();
            assertFalse(game.isColliderUnlocked());
            assertEquals(0, game.colliderEnergyPerSecond());
            game.state().setExplosions(1);
            assertTrue(game.isColliderUnlocked());
        }

        @Test
        void lEnergieSuitLaProduction() {
            Game game = opened(0);
            game.state().setParticles(BigNum.of(1, 9));
            while (game.buy("generator")) { /* des générateurs */ }
            double perSecond = game.colliderEnergyPerSecond();
            assertEquals(Game.COLLIDER_ENERGY * (1 + Math.log(1 + Math.max(0, game.productionAtFusion().log10()) / 10)), perSecond, 1e-9);
            // L'énergie croît avec le logarithme de l'ordre de grandeur : jamais d'emballement.
            assertTrue(perSecond >= Game.COLLIDER_ENERGY && perSecond < 10 * Game.COLLIDER_ENERGY);
            game.state().setColliderEnergy(0);
            game.tick(10);
            assertTrue(game.colliderEnergy() > 0);
        }

        @Test
        void seulesLaBasseEnergieEstOuverteAuDebut() {
            Game game = opened(1e9);
            assertTrue(game.isCollisionTierOpen(CollisionTier.LOW));
            assertFalse(game.isCollisionTierOpen(CollisionTier.MEDIUM));
            game.state().setHadrons(Hadron.PROTON, 1);
            assertTrue(game.isCollisionTierOpen(CollisionTier.MEDIUM));
            assertFalse(game.isCollisionTierOpen(CollisionTier.HIGH));
        }
    }

    @Nested
    class Collisions {

        @Test
        void uneCollisionCouteSonEnergieEtOccupeUnePlace() {
            Game game = opened(100);
            assertTrue(game.startCollision(CollisionTier.LOW));
            assertEquals(70, game.colliderEnergy(), 1e-9);
            assertTrue(game.startCollision(CollisionTier.LOW));
            assertFalse(game.startCollision(CollisionTier.LOW));          // deux places
            assertEquals(2, game.state().collisions().size());
        }

        @Test
        void sansEnergieRienNePart() {
            Game game = opened(29);
            assertFalse(game.startCollision(CollisionTier.LOW));
        }

        @Test
        void uneCollisionTermineeDonneSesQuarks() {
            Game game = opened(30);
            game.startCollision(CollisionTier.LOW);
            game.tick(29);
            assertEquals(0, game.quarks(Quark.UP));
            game.tick(1.5);
            assertTrue(game.state().collisions().isEmpty());
            int up = game.quarks(Quark.UP), down = game.quarks(Quark.DOWN);
            assertTrue(up >= 2 && up <= 4 && down >= 2 && down <= 4, up + " u, " + down + " d");
            assertEquals(1, game.takeFinishedCollisions().size());
            assertTrue(game.takeFinishedCollisions().isEmpty());          // annoncée une fois
        }

        @Test
        void ellesAvancentPendantUneAbsence() {
            Game game = opened(60);
            game.startCollision(CollisionTier.LOW);
            game.startCollision(CollisionTier.LOW);
            new Absence(game, 3600).advance(3600);
            assertTrue(game.state().collisions().isEmpty());
            assertTrue(game.quarks(Quark.UP) >= 4);
        }

        @Test
        void rienNeSePerdALExplosionNiAuBigBang() {
            Game game = opened(0);
            game.state().setQuarks(Quark.UP, 5);
            game.state().setHadrons(Hadron.PROTON, 3);
            for (Element element : PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), element.category().maxCopies());
            assertTrue(game.explode());
            assertEquals(5, game.quarks(Quark.UP));
            assertEquals(3, game.hadrons(Hadron.PROTON));
            game.state().reset();                                          // seul « Recommencer de zéro » efface tout
            assertEquals(0, game.state().hadrons(Hadron.PROTON));
        }
    }

    @Nested
    class Resonance {

        @Test
        void unClicAuMilieuDoubleLeButin() {
            Game single = opened(30), doubled = opened(30);
            single.startCollision(CollisionTier.LOW);
            doubled.startCollision(CollisionTier.LOW);
            doubled.tick(Game.RESONANCE_PERIOD / 2);                     // la barre au milieu
            assertTrue(Game.inResonance(doubled.resonancePhase(0)));
            assertTrue(doubled.resonate(0));
            assertFalse(doubled.resonate(0));                             // une seule tentative
            single.tick(31);
            doubled.tick(31);
            assertEquals(2 * single.quarks(Quark.UP), doubled.quarks(Quark.UP));
            assertEquals(2 * single.quarks(Quark.DOWN), doubled.quarks(Quark.DOWN));
        }

        @Test
        void unClicAuMauvaisMomentNeDonneRienDePlusEtNeRetireRien() {
            Game game = opened(30);
            game.startCollision(CollisionTier.LOW);
            game.tick(0.1);
            assertFalse(Game.inResonance(game.resonancePhase(0)));
            assertFalse(game.resonate(0));
            game.tick(31);
            assertTrue(game.quarks(Quark.UP) >= 2);
        }
    }

    @Nested
    class Hadrons {

        @Test
        void unProtonSeFormeAvecDeuxHautsEtUnBas() {
            Game game = opened(0);
            game.state().setQuarks(Quark.UP, 5);
            game.state().setQuarks(Quark.DOWN, 1);
            assertTrue(game.formHadron(Hadron.PROTON));
            assertEquals(3, game.quarks(Quark.UP));
            assertEquals(0, game.quarks(Quark.DOWN));
            assertFalse(game.formHadron(Hadron.PROTON));
            assertEquals(1, game.hadrons(Hadron.PROTON));
        }

        @Test
        void formerToutPrendTousLesQuarks() {
            Game game = opened(0);
            game.state().setQuarks(Quark.UP, 7);
            game.state().setQuarks(Quark.DOWN, 9);
            assertEquals(3, game.formAllHadrons(Hadron.PROTON));          // 7 hauts : trois protons
            assertEquals(1, game.quarks(Quark.UP));
        }

        @Test
        void chaqueHadronRenforceLaPartiePrincipale() {
            Game game = opened(0);
            BigNum particles = game.particlesPerCreation();
            BigNum atoms = game.atomsPerFusion();
            double dark = game.darkMatterMomentum();
            game.state().setHadrons(Hadron.PROTON, 4);                    // ×3
            game.state().setHadrons(Hadron.NEUTRON, 10);                  // ×1,5
            game.state().setHadrons(Hadron.KAON, 5);                      // ×1,5
            game.state().setHadrons(Hadron.LAMBDA, 2);                    // ×1,5
            assertEquals(particles.toDouble() * 3, game.particlesPerCreation().toDouble(), particles.toDouble() * 1e-9);
            assertEquals(atoms.toDouble() * 1.5, game.atomsPerFusion().toDouble(), atoms.toDouble() * 1e-6);
            assertEquals(dark * 2.25, game.darkMatterMomentum(), dark * 1e-9);
            Game space = opened(0);
            space.state().setBigBangs(1);
            BigNum before = space.steadySpacePerSecond();
            space.state().setHadrons(Hadron.JPSI, 5);                     // ×2
            assertEquals(before.toDouble() * 2, space.steadySpacePerSecond().toDouble(), 1e-9);
        }
    }

    @Nested
    class QuarkTop {

        private Game withTop() {
            Game game = opened(0);
            game.state().setHadrons(Hadron.UPSILON, 1);
            game.state().setColliderEnergy(CollisionTier.EXTREME.cost());
            assertTrue(game.startCollision(CollisionTier.EXTREME));
            game.tick(CollisionTier.EXTREME.seconds() + 1);
            return game;
        }

        @Test
        void ilArriveAvecLaCollisionExtremeEtSUtiliseEnUneMinute() {
            Game game = withTop();
            assertTrue(game.hasTop());
            BigNum particles = game.particlesPerCreation();
            assertTrue(game.useTop());
            assertFalse(game.hasTop());
            assertEquals(particles.toDouble() * Game.OVERHEAT_FACTOR, game.particlesPerCreation().toDouble(), particles.toDouble() * 1e-9);
            game.tick(Game.OVERHEAT_SECONDS + 1);
            assertEquals(particles.toDouble(), game.particlesPerCreation().toDouble(), particles.toDouble() * 1e-9);
        }

        @Test
        void rateIlLaisseQuandMemeDesQuarksBeaux() {
            Game game = withTop();
            int bottom = game.quarks(Quark.BOTTOM);
            game.tick(Game.TOP_SECONDS + 1);
            assertFalse(game.hasTop());
            assertFalse(game.useTop());
            assertEquals(bottom + Game.TOP_CONSOLATION_BOTTOM, game.quarks(Quark.BOTTOM));
        }
    }

    @Test
    void auDelaDeDixChaqueHadronCompteMoins() {
        assertEquals(10, Game.weightOf(10), 1e-9);
        assertTrue(Game.weightOf(11) > 10 && Game.weightOf(11) < 11);
        assertEquals(10 * (1 + Math.log(10)), Game.weightOf(100), 1e-9);
        Game game = new Game();
        game.state().setHadrons(Hadron.PROTON, 100);
        assertEquals(1 + 0.5 * Game.weightOf(100), game.colliderParticlesMultiplier(), 1e-9);
    }

    @Test
    void lesPaliersSeDeduisentDesHadrons() {
        Game game = new Game();
        assertEquals(0, game.colliderMilestones());
        assertEquals(Game.COLLIDER_SLOTS, game.colliderSlots());
        game.state().setHadrons(Hadron.PROTON, 10);
        game.state().setHadrons(Hadron.NEUTRON, 9);
        assertFalse(game.hasColliderMilestone(ColliderMilestone.PLASMA));
        assertEquals(19, game.colliderMilestoneProgress(ColliderMilestone.PLASMA)[0]);
        game.state().setHadrons(Hadron.NEUTRON, 10);
        assertTrue(game.hasColliderMilestone(ColliderMilestone.PLASMA));
        game.state().setHadrons(Hadron.KAON, 3);
        game.state().setHadrons(Hadron.LAMBDA, 2);
        assertEquals(Game.COLLIDER_SLOTS + 1, game.colliderSlots());
        game.state().setHadrons(Hadron.JPSI, 3);
        game.state().setHadrons(Hadron.UPSILON, 1);
        assertEquals(4, game.colliderMilestones());
    }

    @Test
    void lePlasmaRelanceLaBasseEnergieSaufSiOnLeCoupe() {
        Game game = new Game();
        game.start();
        game.state().setExplosions(1);
        game.state().setHadrons(Hadron.PROTON, 10);
        game.state().setHadrons(Hadron.NEUTRON, 10);
        game.state().setColliderEnergy(1000);
        game.tick(0.1);
        assertEquals(1, game.state().collisions().size());
        assertEquals(CollisionTier.LOW, game.state().collisions().get(0).tier());
        // Une seule à la fois : l'autre place reste au joueur.
        game.tick(0.1);
        assertEquals(1, game.state().collisions().size());
        for (int i = 0; i < 31; i++) game.tick(1);
        assertEquals(1, game.state().collisions().size(), "terminée, elle repart aussitôt");
        assertTrue(game.state().collisionRuns() >= 1);
        game.setColliderAuto(false);
        for (int i = 0; i < 31; i++) game.tick(1);
        assertTrue(game.state().collisions().isEmpty(), "coupée, elle ne repart plus");
    }

    @Test
    void leTrouNoirSupermassifDemandeLeCoeurDeGalaxie() {
        Game game = new Game();
        Body core = game.bodies().stream().filter(game::needsGalaxyCore).findFirst().orElseThrow();
        assertEquals("supermassive_black_hole", core.id());
        assertEquals(core.conditions() + 1, game.bodyConditions(core.id()));
        int before = game.bodyConditionsMet(core.id());
        game.state().setHadrons(Hadron.UPSILON, 1);
        assertEquals(before + 1, game.bodyConditionsMet(core.id()));
        long others = game.bodies().stream().filter(game::needsGalaxyCore).count();
        assertEquals(1, others, "seul le trou noir supermassif le demande");
    }
}

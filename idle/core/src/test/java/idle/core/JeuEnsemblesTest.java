package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.darkmatter.SizeScale;
import idle.core.elements.Element;
import idle.core.elements.ElementCategory;
import idle.core.elements.ElementEffect;
import idle.core.elements.ElementSet;
import idle.core.elements.PeriodicTable;
import idle.core.math.BigNum;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Les règles du jeu : les ensembles, la synthèse ciblée, les paliers de taille, le verrou de l'appui
 * et les niveaux sans plafond.
 */
class JeuEnsemblesTest extends GameTestSupport {


    @Nested
    class Ensembles {

        private Game tableGame() {
            Game game = newGame();
            unlock(game);
            return game;
        }

        private void giveAll(Game game, ElementSet set) {
            for (Element element : set.members()) game.state().setElementCount(element.number(), 1);
        }

        private ElementSet family(Game game, ElementCategory category) {
            return game.elementSets().stream().filter(set -> set.category() == category).findFirst().orElseThrow();
        }

        private ElementSet period(Game game, int period) {
            return game.elementSets().stream().filter(set -> set.period() == period).findFirst().orElseThrow();
        }

        @Test
        void ilYADixFamillesEtSeptPeriodes() {
            Game game = tableGame();
            assertEquals(17, game.elementSets().size());
            assertEquals(10, game.elementSets().stream().filter(ElementSet::isFamily).count());
            assertEquals(17, game.elementSets().stream().map(ElementSet::id).distinct().count());
            assertEquals(0, game.completedSets());
        }

        @Test
        void chaqueElementAUnePeriode() {
            assertEquals(1, PeriodicTable.period(1));
            assertEquals(1, PeriodicTable.period(2));
            assertEquals(2, PeriodicTable.period(3));
            assertEquals(3, PeriodicTable.period(18));
            assertEquals(6, PeriodicTable.period(57));         // le lanthane
            assertEquals(6, PeriodicTable.period(71));
            assertEquals(7, PeriodicTable.period(89));         // l'actinium
            assertEquals(7, PeriodicTable.period(118));
            int[] sizes = {2, 8, 8, 18, 18, 32, 32};
            int total = 0;
            for (int p = 1; p <= PeriodicTable.PERIODS; p++) {
                assertEquals(sizes[p - 1], PeriodicTable.elementsOfPeriod(p).size());
                total += PeriodicTable.elementsOfPeriod(p).size();
            }
            assertEquals(118, total);
            assertThrows(IllegalArgumentException.class, () -> PeriodicTable.period(0));
            assertThrows(IllegalArgumentException.class, () -> PeriodicTable.elementsOfPeriod(8));
        }

        @Test
        void unEnsembleSeRemplitElementParElement() {
            Game game = tableGame();
            ElementSet halogens = family(game, ElementCategory.HALOGEN);
            assertEquals(6, halogens.members().size());
            assertEquals(0, game.setProgress(halogens));
            game.state().setElementCount(9, 1);                // le fluor
            game.state().setElementCount(17, 5);               // cinq exemplaires de chlore : un seul élément
            assertEquals(2, game.setProgress(halogens));
            assertFalse(game.isSetComplete(halogens));
            giveAll(game, halogens);
            assertEquals(6, game.setProgress(halogens));
            assertTrue(game.isSetComplete(halogens));
            assertEquals(1, game.completedSets());
        }

        @Test
        void unEnsembleCompletAjouteSonBonus() {
            Game game = tableGame();
            ElementSet earths = family(game, ElementCategory.ALKALINE_EARTH_METAL);
            List<Element> members = earths.members();
            double sum = 0;
            for (Element element : members.subList(0, members.size() - 1)) {
                game.state().setElementCount(element.number(), 1);
                sum += ((ElementEffect.Add) element.effect()).bonus();
            }
            // Il en manque un : seulement le quart du bonus, celui de la moitié réunie.
            assertEquals((1 + sum) * Math.pow(1.15, ElementSet.HALF_STRENGTH),
                    game.elementMultiplier(ElementEffect.Stat.ATOMS), 1e-12);

            Element last = members.get(members.size() - 1);
            game.state().setElementCount(last.number(), 1);
            sum += ((ElementEffect.Add) last.effect()).bonus();
            assertEquals((1 + sum) * 1.15, game.elementMultiplier(ElementEffect.Stat.ATOMS), 1e-12);
        }

        @Test
        void laMoitieDUnEnsembleDonneLeQuartDeSonBonus() {
            Game game = tableGame();
            ElementSet earths = family(game, ElementCategory.ALKALINE_EARTH_METAL);   // six éléments, ×1,15 d'atomes
            assertEquals(3, earths.halfSize());
            List<Element> members = earths.members();
            double sum = 0;
            for (Element element : members.subList(0, 2)) {
                game.state().setElementCount(element.number(), 1);
                sum += ((ElementEffect.Add) element.effect()).bonus();
            }
            assertEquals(0, game.setStrength(earths), 0);
            assertEquals(1 + sum, game.elementMultiplier(ElementEffect.Stat.ATOMS), 1e-12);

            game.state().setElementCount(members.get(2).number(), 1);          // le troisième : la moitié
            sum += ((ElementEffect.Add) members.get(2).effect()).bonus();
            assertEquals(ElementSet.HALF_STRENGTH, game.setStrength(earths), 0);
            assertEquals(1, game.halfSets());
            assertEquals(0, game.completedSets());
            assertEquals((1 + sum) * Math.pow(1.15, ElementSet.HALF_STRENGTH),
                    game.elementMultiplier(ElementEffect.Stat.ATOMS), 1e-12);
        }

        @Test
        void laMoitieSArronditAuDessus() {
            Game game = tableGame();
            assertEquals(2, period(game, 1).halfSize());       // deux éléments : pas de premier palier
            game.state().setElementCount(2, 1);
            assertEquals(0, game.setStrength(period(game, 1)), 0);
            assertEquals(4, family(game, ElementCategory.NONMETAL).halfSize());        // sept
            assertEquals(8, family(game, ElementCategory.LANTHANIDE).halfSize());      // quinze
            assertEquals(19, family(game, ElementCategory.TRANSITION_METAL).halfSize());
        }

        @Test
        void unePeriodeCompleteAjouteSonBonus() {
            Game game = tableGame();
            game.state().setElementCount(2, 1);                // l'hélium seul
            assertEquals(2, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-9);
            game.state().setElementCount(1, 1);                // avec l'hydrogène, la première période est complète
            assertTrue(game.isSetComplete(period(game, 1)));
            assertEquals(2 * 3, game.elementMultiplier(ElementEffect.Stat.PARTICLES), 1e-9);
        }

        @Test
        void unElementCompteALaFoisPourSaFamilleEtSaPeriode() {
            Game game = tableGame();
            giveAll(game, period(game, 2));                    // du lithium au néon
            assertEquals(1, game.completedSets());
            assertEquals(0, game.halfSets());                  // un élément par famille : aucune n'est à moitié
            assertEquals(1, game.setProgress(family(game, ElementCategory.NOBLE_GAS)));   // le néon
            assertEquals(1, game.setProgress(family(game, ElementCategory.HALOGEN)));     // le fluor
        }

        @Test
        void lExplosionVideLesEnsembles() {
            Game game = tableGame();
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertEquals(17, game.completedSets());
            assertTrue(game.explode());
            assertEquals(0, game.completedSets());
        }

        @Test
        void unEnsembleReunitUneFamilleOuUnePeriode() {
            ElementEffect effect = new ElementEffect.Luck(1);
            assertThrows(IllegalArgumentException.class, () -> new ElementSet("x", "X", null, 0, effect));
            assertThrows(IllegalArgumentException.class, () -> new ElementSet("x", "X", ElementCategory.HALOGEN, 2, effect));
            assertThrows(IllegalArgumentException.class, () -> new ElementSet("x", "X", null, 8, effect));
            assertTrue(ElementSet.family(ElementCategory.HALOGEN, effect).isFamily());
            assertFalse(ElementSet.period(3, effect).isFamily());
        }
    }

    @Nested
    class SyntheseCiblee {

        private static final double MAX = Game.MAX_ATOMS.toDouble();

        /** Tableau débloqué, plein d'atomes, sans automatisme en marche, synthèse ciblée achetée. */
        private Game tableGame() {
            Game game = newGame();
            unlock(game);
            game.state().setAtoms(BigNum.of(MAX));
            game.state().setDarkLevel("dark_shop_target", 1);
            return game;
        }

        @Test
        void elleSAcheteAvecDeLaMatiereNoire() {
            Game game = newGame();
            unlock(game);
            game.state().setAtoms(BigNum.of(MAX));
            assertFalse(game.isTargetedSynthesisUnlocked());
            assertFalse(game.setSynthesisTarget(ElementCategory.HALOGEN));   // verrouillée : la synthèse reste au hasard
            assertEquals(null, game.synthesisTarget());
            assertEquals(1, game.synthesize().size());

            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(2));
            assertTrue(game.buyDark("dark_shop_target"));
            assertValue(0, game.state().darkMatter());
            assertTrue(game.isTargetedSynthesisUnlocked());
            assertTrue(game.setSynthesisTarget(ElementCategory.HALOGEN));
            assertEquals(ElementCategory.HALOGEN, game.synthesisTarget());
        }

        @Test
        void sansCibleLaSyntheseTireAuHasard() {
            Game game = tableGame();
            assertEquals(null, game.synthesisTarget());
            assertEquals(0, game.synthesisTriesLeft());
            assertEquals(1, game.synthesize().size());
            assertEquals(0, game.synthesisTries());
        }

        @Test
        void onNeViseRienAvantLOuvertureDuTableau() {
            Game game = newGame();
            assertFalse(game.setSynthesisTarget(ElementCategory.HALOGEN));
            assertEquals(null, game.synthesisTarget());
        }

        @Test
        void lesPremieresSynthesesSontPayeesMaisNeDonnentRien() {
            Game game = tableGame();
            assertTrue(game.setSynthesisTarget(ElementCategory.TRANSITION_METAL));   // trois synthèses
            assertEquals(3, game.synthesisTriesLeft());

            assertTrue(game.synthesize().isEmpty());
            assertValue(MAX - 2, game.state().atoms());        // payée au prix normal
            assertEquals(1, game.state().synthesisCount());    // et le prix monte comme pour les autres
            assertEquals(2, game.synthesisTriesLeft());
            assertTrue(game.synthesize().isEmpty());
            assertValue(MAX - 2 - 4, game.state().atoms());
            assertEquals(0, game.discoveredElements());

            List<Element> obtained = game.synthesize();
            assertEquals(1, obtained.size());
            assertEquals(ElementCategory.TRANSITION_METAL, obtained.get(0).category());
            assertValue(MAX - 2 - 4 - 8, game.state().atoms());
            assertEquals(0, game.synthesisTries());
            assertEquals(3, game.synthesisTriesLeft());        // la cible reste : la suivante repart de zéro

            assertEquals(3, game.stats().syntheses());
            assertEquals(2, game.stats().targetTries());
            assertEquals(1, game.stats().targetedSyntheses());
        }

        @Test
        void elleDonneDAbordUnElementQueLeJoueurNAPas() {
            for (long seed = 1; seed <= 20; seed++) {
                Game game = new Game(new GameState(), List.of(SPEED, GENERATOR, KEEP), List.of(AUTO_FUSION), new Random(seed));
                game.start();
                createEnoughAtoms(game);
                game.state().setDarkLevel("dark_shop_target", 1);
                for (Element element : PeriodicTable.elements(ElementCategory.HALOGEN)) {
                    game.state().setElementCount(element.number(), 1);
                }
                game.state().setElementCount(85, 0);           // il ne manque que l'astate
                game.setSynthesisTarget(ElementCategory.HALOGEN);
                List<Element> obtained = List.of();
                for (int i = 0; i < ElementCategory.HALOGEN.targetTries(); i++) {
                    game.state().setAtoms(BigNum.of(MAX));
                    obtained = game.synthesize();
                }
                assertEquals(85, obtained.get(0).number());
            }
        }

        @Test
        void sinonUnElementQuiPeutEncoreGagnerUnExemplaire() {
            Game game = tableGame();
            for (Element element : PeriodicTable.elements(ElementCategory.HALOGEN)) {
                game.state().setElementCount(element.number(), 9);
            }
            game.state().setElementCount(17, 3);               // seul le chlore n'est pas à son maximum
            game.setSynthesisTarget(ElementCategory.HALOGEN);
            List<Element> obtained = List.of();
            for (int i = 0; i < ElementCategory.HALOGEN.targetTries(); i++) {
                game.state().setAtoms(BigNum.of(MAX));
                obtained = game.synthesize();
            }
            assertEquals(17, obtained.get(0).number());
            assertEquals(4, game.elementCount(17));
        }

        @Test
        void uneFamilleEpuiseeNePeutPlusEtreVisee() {
            Game game = tableGame();
            assertTrue(game.setSynthesisTarget(ElementCategory.HALOGEN));
            for (Element element : PeriodicTable.elements(ElementCategory.HALOGEN)) {
                game.state().setElementCount(element.number(), 9);
            }
            assertEquals(null, game.synthesisTarget());        // la cible s'efface d'elle-même
            assertFalse(game.setSynthesisTarget(ElementCategory.HALOGEN));
            assertFalse(game.synthesize().isEmpty());          // la synthèse redevient ordinaire : elle donne tout de suite
        }

        @Test
        void changerDeCibleGardeLesSynthesesDejaPayees() {
            Game game = tableGame();
            game.setSynthesisTarget(ElementCategory.ACTINIDE); // dix synthèses
            game.synthesize();
            game.synthesize();
            assertEquals(8, game.synthesisTriesLeft());
            game.setSynthesisTarget(ElementCategory.TRANSITION_METAL);   // trois : il n'en reste qu'une
            assertEquals(1, game.synthesisTriesLeft());
            game.state().setAtoms(BigNum.of(MAX));
            List<Element> obtained = game.synthesize();
            assertEquals(ElementCategory.TRANSITION_METAL, obtained.get(0).category());
        }

        @Test
        void retirerLaCibleRendLaSyntheseOrdinaire() {
            Game game = tableGame();
            game.setSynthesisTarget(ElementCategory.ACTINIDE);
            game.synthesize();
            assertTrue(game.setSynthesisTarget(null));
            assertEquals(0, game.synthesisTriesLeft());
            assertEquals(1, game.synthesize().size());
            assertEquals(1, game.synthesisTries());            // l'essai payé attend une prochaine cible
        }

        @Test
        void laGarantieDUnElementUniquePasseAvantLaCible() {
            Game game = tableGame();
            game.setSynthesisTarget(ElementCategory.ACTINIDE); // dix synthèses, la garantie tombe à la huitième
            for (int i = 1; i < Game.GUARANTEED_UNIQUE_SYNTHESIS; i++) {
                game.state().setAtoms(BigNum.of(MAX));
                assertTrue(game.synthesize().isEmpty());
            }
            game.state().setAtoms(BigNum.of(MAX));
            List<Element> guaranteed = game.synthesize();
            assertEquals(1, guaranteed.size());
            assertTrue(guaranteed.get(0).category().unique());
            assertEquals(Game.GUARANTEED_UNIQUE_SYNTHESIS - 1, game.synthesisTries());   // la cible n'a pas avancé
        }

        @Test
        void laSyntheseAutomatiqueSuitLaCible() {
            Game game = tableGame();
            game.state().setElementCount(92, 1);               // un élément unique : la synthèse automatique existe
            game.buyAutomation("autoSynth");                   // une action toutes les 2 secondes
            game.state().setAtoms(BigNum.of(MAX));
            game.setSynthesisTarget(ElementCategory.TRANSITION_METAL);
            game.tick(4.1);                                    // deux synthèses payées, rien d'obtenu
            assertEquals(2, game.state().synthesisCount());
            assertEquals(1, game.discoveredElements());
            game.tick(2);                                      // la troisième aboutit
            assertEquals(3, game.state().synthesisCount());
            assertEquals(2, game.discoveredElements());
        }

        @Test
        void lExplosionEffaceLaCible() {
            Game game = tableGame();
            game.setSynthesisTarget(ElementCategory.ACTINIDE);
            game.synthesize();
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            assertEquals(null, game.state().synthesisTarget());
            assertEquals(0, game.synthesisTries());
        }

        @Test
        void plusLaFamilleEstRarePlusLaCibleCoute() {
            assertEquals(3, ElementCategory.TRANSITION_METAL.targetTries());
            assertEquals(5, ElementCategory.HALOGEN.targetTries());
            assertEquals(8, ElementCategory.NOBLE_GAS.targetTries());
            assertEquals(10, ElementCategory.ACTINIDE.targetTries());
            ElementCategory previous = null;
            for (ElementCategory category : ElementCategory.values()) {
                assertTrue(category.targetTries() >= 3);       // toujours plus cher que le double
                if (previous != null) assertTrue(category.targetTries() >= previous.targetTries());
                previous = category;
            }
        }
    }

    @Nested
    class PaliersDeTaille {

        private Game darkGame() {
            Game game = newGame();
            game.state().setDarkMatter(BigNum.of(1));
            game.state().setExplosions(1);
            return game;
        }

        @Test
        void leProtonNEstPasUnPalier() {
            assertEquals(Game.DARK_MATTER_START_SIZE, SizeScale.LANDMARKS.get(0).size());
            assertEquals(SizeScale.LANDMARKS.size() - 1, SizeScale.MILESTONES.size());
            assertEquals(26, SizeScale.MILESTONES.size());
            Game game = darkGame();
            assertEquals(0, game.landmarksReached());          // à sa taille de départ
        }

        @Test
        void aucunPalierAvantLaPremiereExplosion() {
            Game game = newGame();
            game.state().setDarkMatterSize(BigNum.of(1, 30));
            assertEquals(0, game.landmarksReached());
            assertValue(1, game.particlesPerCreation());
        }

        @Test
        void lesTroisBonusAlternent() {
            assertEquals(SizeScale.Bonus.PARTICLES, SizeScale.bonusOf(0));
            assertEquals(SizeScale.Bonus.ATOMS, SizeScale.bonusOf(1));
            assertEquals(SizeScale.Bonus.EXPANSION, SizeScale.bonusOf(2));
            assertEquals(SizeScale.Bonus.PARTICLES, SizeScale.bonusOf(3));
            assertThrows(IllegalArgumentException.class, () -> SizeScale.bonusOf(26));
        }

        @Test
        void chaquePalierAtteintDonneSonBonus() {
            Game game = darkGame();
            game.state().setDarkMatterSize(BigNum.of(2, -14));   // au-delà du noyau d'uranium : un palier
            assertEquals(1, game.landmarksReached());
            assertValue(2, game.particlesPerCreation());
            assertValue(1, game.atomsPerFusion());

            game.state().setDarkMatterSize(BigNum.of(2, -10));   // l'atome d'hydrogène : deux paliers
            assertEquals(2, game.landmarksReached());
            assertValue(1.05, game.atomsPerFusion());
            assertEquals(1, game.landmarkExpansionMultiplier(), 0);

            game.state().setDarkMatterSize(BigNum.of(1, -8));    // l'hélice d'ADN : trois paliers
            assertEquals(Game.LANDMARK_EXPANSION, game.landmarkExpansionMultiplier(), 1e-12);
        }

        @Test
        void auBoutDeLEchelleLesBonusSeCumulent() {
            Game game = darkGame();
            game.state().setDarkMatterSize(BigNum.of(1, 30));
            assertEquals(26, game.landmarksReached());
            assertEquals(9, game.landmarksReached(SizeScale.Bonus.PARTICLES));
            assertEquals(9, game.landmarksReached(SizeScale.Bonus.ATOMS));
            assertEquals(8, game.landmarksReached(SizeScale.Bonus.EXPANSION));
            assertValue(512, game.landmarkParticlesMultiplier());
            assertEquals(1.45, game.landmarkAtomsMultiplier(), 1e-12);
            assertEquals(Math.pow(Game.LANDMARK_EXPANSION, 8), game.landmarkExpansionMultiplier(), 1e-9);
        }

        @Test
        void lesPaliersDeCroissanceAccelerentLaMatiereNoire() {
            Game small = darkGame();
            Game large = darkGame();
            large.state().setDarkMatterSize(BigNum.of(1, -8));   // un palier de croissance atteint
            double ratio = large.darkMatterMomentum() / small.darkMatterMomentum();
            // L'élan suit aussi la production, que le premier palier a doublée.
            assertEquals(Game.LANDMARK_EXPANSION * large.darkMatterProgressFactor() / small.darkMatterProgressFactor(), ratio, 1e-9);
        }
    }

    @Nested
    class VerrouDeLAppui {

        private Game darkGame() {
            Game game = newGame();
            game.state().setDarkMatter(BigNum.of(5));
            game.state().setExplosions(1);
            return game;
        }

        @Test
        void sansLaCaseDeLArbreRienNeSeVerrouille() {
            Game game = darkGame();
            assertFalse(game.isHoldLockUnlocked(), "le verrou se débloque dans l'arbre, par « dark_lock »");
            assertFalse(game.setHoldLocked(true));
            assertFalse(game.isHoldLocked());
            game.tick(60);
            assertEquals(Game.DARK_MATTER_START_SIZE, game.state().darkMatterSize());
        }

        @Test
        void verrouilleeElleGrossitAChaqueTick() {
            Game locked = darkGame();
            locked.state().setDarkLevel("dark_lock", 1);
            assertTrue(locked.isHoldLockUnlocked());
            assertTrue(locked.setHoldLocked(true));
            assertTrue(locked.isHoldLocked());
            locked.tick(30);

            Game held = darkGame();
            held.growDarkMatter(30 * Game.HOLD_LOCK_SHARE);    // un verrou vaut 60 % d'un appui tenu à la main
            assertEquals(held.state().darkMatterSize().log10(), locked.state().darkMatterSize().log10(), 1e-9);
            assertEquals(30 * Game.HOLD_LOCK_SHARE, locked.stats().holdTime(), 1e-9);   // compté comme un appui
        }

        @Test
        void leVerrouSeLibereALaDemande() {
            Game game = darkGame();
            game.state().setDarkLevel("dark_lock", 1);
            game.setHoldLocked(true);
            assertTrue(game.setHoldLocked(false));
            game.tick(30);
            assertEquals(Game.DARK_MATTER_START_SIZE, game.state().darkMatterSize());
        }

        @Test
        void lExplosionGardeLeVerrou() {
            Game game = darkGame();
            game.state().setDarkLevel("dark_lock", 1);
            game.setHoldLocked(true);
            for (Element element : PeriodicTable.ELEMENTS) {
                game.state().setElementCount(element.number(), element.category().maxCopies());
            }
            assertTrue(game.explode());
            // Le verrou est un réglage du joueur : la partie suivante repart verrouillée.
            assertTrue(game.isHoldLocked());
            BigNum size = game.state().darkMatterSize();
            game.tick(10);
            assertTrue(game.state().darkMatterSize().gt(size));
        }
    }

    @Nested
    class NiveauxSansPlafond {

        @Test
        void noyauxEtRouagesSombresNOntPlusDeMaximum() {
            Game game = newGame();
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(1e6));
            for (String id : List.of("dark_shop_atoms", "dark_shop_cadence")) {
                for (int level = 0; level < 12; level++) {
                    assertValue(Math.pow(2, level), game.darkCostOf(id));   // 1, 2, 4, 8…
                    assertTrue(game.buyDark(id));
                }
                assertEquals(12, game.darkLevelOf(id));
                assertFalse(game.isDarkMaxed(id));
            }
            // Douze niveaux à +20 % par matière noire gagnée : l'effet s'additionne, il ne s'emballe pas.
            assertEquals(1 + 0.2 * 12 * 1e6, game.darkAtomsMultiplier(), 1);
        }

        @Test
        void lesRouagesNeFontJamaisPasserSousLeDelaiMinimal() {
            Game game = newGame();
            unlock(game);
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(1e6));
            for (int level = 0; level < 12; level++) game.buyDark("dark_shop_cadence");
            assertEquals(game.minAutomationInterval(), game.automationInterval("autoSlow"), 0);
        }
    }
}

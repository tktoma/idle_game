package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.math.BigNum;
import org.junit.jupiter.api.Test;

/**
 * Version 0.181 : le premier univers est plus court. Son premier Big Bang demande moins de particules et de matière
 * noire (acte 2 du Speedrunner : 5 h 43 → 4 h 09), ses Big Bangs suivants moins de particules et dix fois moins d'atomes
 * (les sept Big Bangs : 6 h 52 → 5 h 41), et son élan de matière noire est plus fort après le premier Big Bang. Les
 * univers suivants gardent les seuils d'avant. Les défis d'explosion se corsent d'un facteur par univers formé.
 */
class Rythme181Test {

    @Test
    void lePremierUniversDemandeMoinsQueLesSuivantsPourSesBigBangs() {
        Game game = GameTestSupport.realGame();
        assertEquals(85_000, game.bigBangParticles().log10(), 1e-9, "premier Big Bang du premier univers");
        assertEquals(1e11, game.bigBangAtoms().toDouble(), 1, "atomes du premier univers");
        assertEquals(42_000, game.bigBangDarkMatter().toDouble(), 1e-9, "réserve du tout premier Big Bang");
        game.state().setBigBangs(3);
        assertEquals(30_000, game.bigBangParticles().log10(), 1e-9, "Big Bangs suivants du premier univers");
        assertEquals(1e11, game.bigBangAtoms().toDouble(), 1);
        assertEquals(500, game.bigBangDarkMatter().toDouble(), 1e-9);
        // Un univers formé : les seuils d'avant.
        game.state().setUniverseFormed(true);
        assertEquals(50_000, game.bigBangParticles().log10(), 1e-9, "Big Bangs suivants des univers d'après");
        assertEquals(1e12, game.bigBangAtoms().toDouble(), 1);
        game.state().setBigBangs(0);
        assertEquals(190_000, game.bigBangParticles().log10(), 1e-9, "premier Big Bang des univers d'après");
        assertEquals(500, game.bigBangDarkMatter().toDouble(), 1e-9);
        // Les constantes se tiennent : moins au premier univers, dans le même ordre.
        assertTrue(Game.FIRST_UNIVERSE_BIG_BANG_PARTICLES.lt(Game.BIG_BANG_PARTICLES));
        assertTrue(Game.FIRST_UNIVERSE_BIG_BANG_PARTICLES_LATER.lt(Game.BIG_BANG_PARTICLES_LATER));
        assertTrue(Game.FIRST_UNIVERSE_BIG_BANG_PARTICLES_LATER.lt(Game.FIRST_UNIVERSE_BIG_BANG_PARTICLES));
        assertTrue(Game.FIRST_UNIVERSE_BIG_BANG_ATOMS.lt(Game.BIG_BANG_ATOMS));
        assertTrue(Game.FIRST_BIG_BANG_DARK_MATTER.gt(Game.BIG_BANG_DARK_MATTER));
    }

    @Test
    void laBarreHauteSAppliqueAuxSeuilsDuPremierUnivers() {
        Game game = GameTestSupport.realGame();
        GameTestSupport.bangs(game, 1);
        game.state().setActiveBangChallenge(idle.core.bigbang.BangChallenge.RAISED.name());
        assertEquals(30_000 * 1.5, game.bigBangParticles().log10(), 1e-6, "la barre haute : une fois et demie les ordres de grandeur");
        assertEquals(1e13, game.bigBangAtoms().toDouble(), 1, "cent fois les atomes");
        assertEquals(1000, game.bigBangDarkMatter().toDouble(), 1e-9, "deux fois la réserve ordinaire");
    }

    @Test
    void lElanDeLaMatiereNoireEstPlusFortAuPremierUniversApresSonPremierBigBang() {
        Game game = GameTestSupport.realGame();
        GameTestSupport.bangs(game, 1);
        game.state().setDarkMatter(BigNum.of(1000));
        game.state().setExplosions(1);
        assertEquals(1.6, game.firstUniverseCycleMomentum(), 1e-12);
        game.state().setUniverseFormed(true);
        assertEquals(1, game.firstUniverseCycleMomentum(), 1e-12, "un univers formé : plus de bonus");
        game.state().setUniverseFormed(false);
        game.state().setBigBangs(0);
        assertEquals(1, game.firstUniverseCycleMomentum(), 1e-12, "avant le premier Big Bang : pas de bonus");
        // La décomposition le dit.
        game.state().setBigBangs(2);
        assertTrue(Multipliers.of(game, Multipliers.Quantity.DARK_GROWTH).stream()
                .anyMatch(f -> f.label().startsWith("Premier univers") && Math.abs(f.log10() - Math.log10(1.6)) < 1e-9));
    }

    @Test
    void lesDefisSeCorsentDUnFacteurParUniversForme() {
        Game game = GameTestSupport.realGame();
        GameTestSupport.bangs(game, 1);
        game.state().setDarkMatter(BigNum.of(1000));
        game.state().setExplosions(1);
        assertEquals(1, game.challengeUniverseFactor(), 1e-12, "aucun univers formé");
        assertTrue(game.startChallenge("challenge_heavy"));
        assertEquals(5000, game.tableWeight().toDouble(), 1e-9);
        game.abandonChallenge();
        for (int universes = 1; universes <= 11; universes++) {
            Game later = GameTestSupport.universe(universes);
            assertEquals(Math.pow(1.3, universes), later.challengeUniverseFactor(), 1e-9, universes + " univers");
        }
        Game four = GameTestSupport.universe(4);
        four.state().setDarkMatter(BigNum.of(1000));
        four.state().setExplosions(1);
        assertTrue(four.startChallenge("challenge_heavy"));
        assertEquals(5000 * Math.pow(1.3, 4), four.tableWeight().toDouble(), 1e-6, "quatre univers : ×2,86");
        assertEquals(118 * 5000 * Math.pow(1.3, 4), four.atomCap().toDouble(), 118 * 5000 * Math.pow(1.3, 4) * 1e-9);
        four.abandonChallenge();
        assertTrue(four.startChallenge("challenge_presence"));
        assertEquals(3e6 * Math.pow(1.3, 4), four.tableWeight().toDouble(), 1, "les défis de style aussi");
        four.abandonChallenge();
        assertEquals(BigNum.of(3).pow(Math.min(four.state().tableWeightLevel(), Game.TABLE_WEIGHT_MAX_LEVEL)).toDouble(),
                four.tableWeight().toDouble(), 1e-9, "hors défi, la masse ordinaire ne bouge pas");
    }
}

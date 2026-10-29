package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.GameMirror.MirrorAnswer;
import idle.core.GameMirror.MirrorCell;
import idle.core.GameMirror.MirrorPhase;
import idle.core.GameMirror.MirrorUpgrade;
import idle.core.GameMirror.Season;
import idle.core.GameUniverse.SeedUpgrade;
import idle.core.save.SaveCodec;
import idle.core.universe.DimensionAutomation;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.157 : le miroir, neuvième dimension. Un motif s'allume sur la moitié gauche ; il faut le reproduire de
 * mémoire, en miroir, sur la moitié droite. Il s'allonge à chaque réussite. Et les saisons, qui font remplir le chrono
 * en arrosant.
 */
class MiroirTest {

    private static Game nine() {
        return GameTestSupport.universe(9);
    }

    private static final List<MirrorCell> THREE = List.of(new MirrorCell(0, 0), new MirrorCell(1, 2), new MirrorCell(0, 3));

    @Test
    void leMiroirSOuvreAuNeuviemeUnivers() {
        assertFalse(GameTestSupport.universe(8).isMirrorOpen());
        Game game = nine();
        assertTrue(game.isMirrorOpen());
        assertEquals("reflets", Game.dimensionResourceName(9));
        assertEquals(4, game.mirrorColumns());
        assertEquals(4, game.mirrorRows());
        assertEquals(3, game.mirrorLength());
        assertEquals(new MirrorCell(3, 1), game.mirrored(new MirrorCell(0, 1)));
        assertEquals(new MirrorCell(2, 2), game.mirrored(new MirrorCell(1, 2)));
    }

    @Test
    void leMotifSAllumePuisAttend() {
        Game game = nine();
        game.tick(GameMirror.MIRROR_PAUSE + 0.05);
        assertEquals(MirrorPhase.SHOW, game.mirrorPhase());
        assertEquals(3, game.mirrorPattern().size());
        for (MirrorCell cell : game.mirrorPattern()) assertTrue(cell.column() < game.mirrorHalf(), "sur la moitié gauche");
        assertEquals(0, game.mirrorShown());
        assertEquals(MirrorAnswer.IGNORED, game.clickMirror(3, 0), "pas pendant l'allumage");
        game.tick(0.8);
        assertEquals(1, game.mirrorShown());
        game.tick(1.6);
        assertEquals(MirrorPhase.INPUT, game.mirrorPhase());
        assertEquals(-1, game.mirrorShown(), "éteint : de mémoire");
        game.tick(60);
        assertEquals(MirrorPhase.INPUT, game.mirrorPhase(), "il attend le joueur");
    }

    @Test
    void reproduireEnMiroirReussitEtAllonge() {
        Game game = nine();
        game.testSetMirrorPattern(THREE);
        assertEquals(MirrorAnswer.IGNORED, game.clickMirror(0, 0), "la moitié gauche ne compte pas");
        assertEquals(MirrorAnswer.RIGHT, game.clickMirror(3, 0));
        assertEquals(MirrorAnswer.RIGHT, game.clickMirror(2, 2));
        assertEquals(MirrorAnswer.SUCCESS, game.clickMirror(3, 3));
        assertEquals(game.mirrorValue() * 3, game.state().mirrorShards(), 1e-9);
        assertEquals(4, game.mirrorLength(), "le motif suivant est plus long");
        assertEquals(3, game.mirrorBest());
        assertEquals(MirrorPhase.PAUSE, game.mirrorPhase());
        game.tick(GameMirror.MIRROR_PAUSE + 0.05);
        assertEquals(4, game.mirrorPattern().size());
        assertEquals(game.mirrorValue() * 5 * 1.1 * 1.1, game.mirrorReward(5), 1e-9, "×1,1 par case au-delà de trois");
    }

    @Test
    void uneErreurFaitManquer() {
        Game game = nine();
        game.testSetMirrorPattern(List.of(new MirrorCell(0, 0), new MirrorCell(1, 2), new MirrorCell(0, 3), new MirrorCell(1, 1)));
        assertEquals(MirrorAnswer.RIGHT, game.clickMirror(3, 0));
        assertEquals(MirrorAnswer.RIGHT, game.clickMirror(2, 2));
        assertEquals(MirrorAnswer.FAIL, game.clickMirror(2, 3), "pas le reflet");
        assertEquals(game.mirrorValue() * 2 * GameMirror.MIRROR_FAIL_SHARE, game.state().mirrorShards(), 1e-9, "un quart par case juste");
        assertEquals(3, game.mirrorLength(), "raccourci d'une case");
        assertEquals(1, game.mirrorFails());
        // Avec une seconde chance, une erreur est permise.
        game.state().setMirrorLevel(MirrorUpgrade.MISTAKES.ordinal(), 1);
        game.testSetMirrorPattern(THREE);
        assertEquals(MirrorAnswer.WRONG, game.clickMirror(2, 0));
        assertEquals(MirrorAnswer.RIGHT, game.clickMirror(3, 0), "on reprend à la même case");
        assertEquals(MirrorAnswer.FAIL, game.clickMirror(3, 2));
        game.testSetMirrorPattern(THREE);
        game.clickMirror(3, 0);
        game.clickMirror(2, 2);
        assertEquals(MirrorAnswer.SUCCESS, game.clickMirror(3, 3), "les erreurs repartent de zéro à chaque motif");
    }

    @Test
    void lesAmeliorationsChangentLeMiroir() {
        Game game = nine();
        game.state().setMirrorShards(1e15);
        for (MirrorUpgrade upgrade : MirrorUpgrade.values()) assertTrue(game.buyMirror(upgrade), upgrade.toString());
        assertEquals(2 * game.hadronBoost(idle.core.collider.Hadron.OMEGA), game.mirrorValue(), 1e-9);
        assertEquals(1.15, game.mirrorLengthFactor(), 1e-12);
        assertEquals(0.95, game.mirrorShowSeconds(), 1e-12);
        assertEquals(1, game.mirrorAllowedMistakes());
        assertEquals(4, game.mirrorMaxGrowth(), "plateau plus vaste : il peut s'agrandir une fois de plus");
        assertEquals(4, game.mirrorColumns(), "l'achat seul n'agrandit pas le plateau");
    }

    /** Version 0.158 : le plateau s'agrandit seul, toutes les trois réussites. */
    @Test
    void lePlateauSAgranditToutesLesTroisReussites() {
        Game game = nine();
        double value = game.mirrorValue();
        for (int win = 1; win <= 3; win++) {
            assertEquals(4 - win, game.mirrorWinsToGrow());
            game.testSetMirrorPattern(THREE);
            game.clickMirror(3, 0);
            game.clickMirror(2, 2);
            assertEquals(MirrorAnswer.SUCCESS, game.clickMirror(3, 3));
        }
        assertEquals(1, game.mirrorGrowth());
        assertEquals(6, game.mirrorColumns());
        assertEquals(5, game.mirrorRows());
        assertEquals(value * 1.5, game.mirrorValue(), 1e-9, "chaque agrandissement : le gain ×1,5 de plus");
        game.state().setMirrorWins(100);
        assertEquals(GameMirror.MIRROR_GROWTHS, game.mirrorGrowth(), "trois fois au plus");
        assertEquals(0, game.mirrorWinsToGrow());
        game.state().setMirrorLevel(MirrorUpgrade.BOARD.ordinal(), 3);
        assertEquals(6, game.mirrorGrowth());
        assertEquals(16, game.mirrorColumns());
        // Les réussites traversent les univers et se sauvent.
        assertEquals(100, GameTestSupport.reread(game.state()).mirrorWins(), 0);
    }

    @Test
    void lAutomatismeReproduitJusquASixCases() {
        Game game = nine();
        game.state().setTreeBubbleBought(GameUniverse.TreeBubble.SUMMIT.ordinal(), true);
        game.state().addDimensionAutomation(DimensionAutomation.MIRROR_PLAY.name());
        for (int i = 0; i < 2_400; i++) game.tick(0.05);
        assertTrue(game.mirrorSuccesses() >= 8, "deux minutes : " + game.mirrorSuccesses() + " motifs");
        assertEquals(6, game.mirrorBest(), "sans faute jusqu'à six cases");
        assertTrue(game.mirrorFails() > 0, "au-delà, il se trompe");
        long before = game.mirrorSuccesses();
        game.tick(300);
        assertTrue(game.mirrorSuccesses() > before + 10, "une absence les joue aussi");
        assertEquals(6, java.util.Arrays.stream(DimensionAutomation.values()).filter(a -> a.dimension() == 9).count());
        assertEquals(8, DimensionAutomation.MIRROR_PLAY.paidWith(), "payés en repères de la grille");
    }

    @Test
    void lesSaisonsRemplissentLeChronoEnArrosant() {
        Game game = nine();
        assertTrue(game.areSeasonsUnlocked());
        game.state().setSeedUpgradeLevel(SeedUpgrade.SEASONS.ordinal(), 2);
        assertEquals(0, game.seasonFillPerWatering(), 0, "sans chrono, rien");
        game.state().setSeedUpgradeLevel(SeedUpgrade.CHRONO.ordinal(), 1);
        game.testSetSeason(Season.SPRING);
        assertEquals(0.2 * 2 * 1.5, game.seasonFillPerWatering(), 1e-12);
        game.state().setChronoStored(0);
        assertTrue(game.tendSeed());
        assertEquals(0.6, game.chronoStored(), 1e-9);
        game.testSetSeason(Season.WINTER);
        assertEquals(0.2, game.seasonFillPerWatering(), 1e-12, "l'hiver, moitié moins");
        assertEquals(Season.WINTER, game.season());
        game.tick(GameMirror.SEASON_SECONDS + 1);
        assertEquals(Season.SPRING, game.season(), "et le printemps revient");
        assertEquals(9, SeedUpgrade.SEASONS.dimension(), "payées en reflets");
        assertTrue(SeedUpgrade.SEASONS.chrono(), "elles attendent l'étage du chrono");
    }

    @Test
    void leMiroirTraverseLesUniversEtSeSauve() throws Exception {
        Game game = nine();
        game.state().setMirrorShards(1e6);
        assertTrue(game.buyMirror(MirrorUpgrade.GAIN));
        game.state().setMirrorShardsEarned(GameUniverse.MIRROR_THRESHOLD);
        assertTrue(game.isDimensionThresholdReached(9));
        game.tick(1);
        assertTrue(game.hasTitle(idle.core.profile.Title.MIRROR_KEEPER));
        GameState back = GameTestSupport.reread(game.state());
        assertEquals(1, back.mirrorLevel(MirrorUpgrade.GAIN.ordinal()));
        StringBuilder old = new StringBuilder();
        for (String line : SaveCodec.write(game.state(), 0).split("\n")) if (!line.startsWith("mirror")) old.append(line).append('\n');
        assertEquals(0, SaveCodec.read(old.toString()).state().mirrorShards(), 0);
        game.state().setReplaying(false);
        assertTrue(game.startNextUniverse());
        assertEquals(1, game.mirrorLevel(MirrorUpgrade.GAIN));
    }
}

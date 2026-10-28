package idle.ui.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.Multipliers;
import idle.core.math.BigNum;
import idle.ui.FxTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.144 : la sous-page « Univers » des statistiques et les trois nouvelles grandeurs du détail des
 * multiplicateurs (énergie du collisionneur, énergie du vide, segments de la ligne), sous Xvfb.
 */
class StatsUniversePageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Le troisième univers, vide ouvert, avec de l'énergie et de l'espace pour les paires, et une ligne qui tourne. */
    private static Game third() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        assertTrue(game.startNextUniverse());
        assertTrue(game.testAddUniverse());
        game.state().setBigBangs(1);
        game.state().setColliderEnergy(game.voidPrice() + 1e6);
        assertTrue(game.openVoid());
        game.state().setSpace(BigNum.of(1e6));
        game.state().setLineTicksLevel(3);
        game.state().setColliderGenerators(idle.core.collider.ColliderGenerator.BEAM, 7);
        game.state().setColliderUpgradeLevel(idle.core.collider.ColliderUpgrade.ENERGY, 3);
        game.state().setVoidLevel(Game.VoidUpgrade.RATE.ordinal(), 4);
        game.state().setVoidLevel(Game.VoidUpgrade.GAIN.ordinal(), 2);
        game.tick(1);
        return game;
    }

    @Test
    void laSousPageUniversAttendLePremierUniversForme() {
        FxTestSupport.fx(() -> {
            Game fresh = new Game();
            fresh.start();
            StatsPage page = new StatsPage(fresh);
            page.refresh();
            assertFalse(page.openSubPages().contains("Univers"), "premier univers en cours : rien à montrer");
            assertFalse(page.openSubPage("Univers"));
            return null;
        });
    }

    @Test
    void laSousPageUniversMontreLaGraineLesDimensionsEtLeVide() {
        FxTestSupport.fx(() -> {
            Game game = third();
            StatsPage page = new StatsPage(game);
            page.refresh();
            List<String> names = page.openSubPages();
            assertTrue(names.contains("Univers"), names.toString());
            assertTrue(names.indexOf("Big Bang") < names.indexOf("Univers") && names.indexOf("Univers") < names.indexOf("Collisionneur"),
                    "entre le Big Bang et le collisionneur : " + names);
            assertTrue(page.openSubPage("Univers"));
            List<String> rows = page.shownRows();
            assertTrue(rows.stream().anyMatch(row -> row.startsWith("Univers formés : 2")), rows.toString());
            assertTrue(rows.stream().anyMatch(row -> row.startsWith("Dimensions ouvertes : 2 / " + Game.MAX_DIMENSIONS)), rows.toString());
            assertTrue(rows.stream().anyMatch(row -> row.startsWith("Ligne : segments :")), rows.toString());
            assertTrue(rows.stream().anyMatch(row -> row.startsWith("Ligne : seuil : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(row -> row.startsWith("Énergie du vide : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(row -> row.startsWith("Paires vivantes : ")), rows.toString());
            assertTrue(rows.stream().anyMatch(row -> row.startsWith("Ce que le vide raccourcit : rien encore")), rows.toString());
            assertFalse(rows.stream().anyMatch(row -> row.startsWith("Masse du trou noir")), "pas de trou noir : pas de ligne");
            assertTrue(rows.stream().anyMatch(row -> row.startsWith("Plan : facettes :")), "deux univers : la ligne et le plan sont ouverts");
            assertFalse(rows.stream().anyMatch(row -> row.startsWith("Volume :")), "pas encore le volume");
            List<String> graphs = page.offeredGraphs();
            assertTrue(graphs.contains("Univers formés") && graphs.contains("Segments de la ligne") && graphs.contains("Énergie du vide"),
                    graphs.toString());
            assertTrue(graphs.contains("Facettes du plan") && !graphs.contains("Sphères du volume"), graphs.toString());
            // Le Big Bang ne porte plus les lignes du vide : elles ont déménagé ici.
            assertTrue(page.openSubPage("Big Bang"));
            assertFalse(page.shownRows().stream().anyMatch(row -> row.startsWith("Énergie du vide")), page.shownRows().toString());
            return null;
        });
    }

    @Test
    void leDetailDesMultiplicateursProposeLesTroisNouvellesGrandeurs() {
        FxTestSupport.fx(() -> {
            Game game = third();
            StatsPage page = new StatsPage(game);
            page.openMultipliers();
            for (Multipliers.Quantity quantity : List.of(Multipliers.Quantity.COLLIDER_ENERGY, Multipliers.Quantity.VOID_ENERGY,
                    Multipliers.Quantity.LINE_SHARDS)) {
                assertTrue(Multipliers.isAvailable(game, quantity), quantity.toString());
                page.chooseMultiplier(quantity);
                page.refresh();
                assertTrue(page.multiplierLineCount() > 0, quantity + " : " + page.multiplierLines());
                assertTrue(page.multiplierLines().stream().anyMatch(line -> line.contains(Multipliers.Aspect.UPGRADES.label())),
                        quantity + " : " + page.multiplierLines());
            }
            page.chooseMultiplier(Multipliers.Quantity.LINE_SHARDS);
            page.refresh();
            assertEquals(Multipliers.byAspect(Multipliers.of(game, Multipliers.Quantity.LINE_SHARDS)).size(), page.multiplierLineCount(),
                    "replié, une ligne par aspect : " + page.multiplierLines());
            return null;
        });
    }
}

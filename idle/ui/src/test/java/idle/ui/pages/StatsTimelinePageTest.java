package idle.ui.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameStats;
import idle.core.history.PurchaseHistory;
import idle.ui.FxTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.168 : la sous-page « Chronologie » des statistiques — chaque univers formé et ses phases, le temps de chaque
 * dimension, la graine et le défi final — et la valeur durable dans la sous-page « Achats », sous Xvfb.
 */
class StatsTimelinePageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    @Test
    void laChronologieMontreLesUniversLesDimensionsLaGraineEtLeDefiFinal() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            assertFalse(new StatsPage(game, new PurchaseHistory(), new idle.core.history.Pace()).openSubPages().contains("Chronologie"),
                    "avant le premier univers : rien à montrer");
            game.state().setTimePlayed(7_200);
            game.testFormUniverse();
            game.state().setTimePlayed(10_800);
            game.testFormUniverse();
            game.noteDimensionPlayed(1, 0.5);
            game.tick(2);
            StatsPage page = new StatsPage(game, new PurchaseHistory(), new idle.core.history.Pace());
            page.refresh();
            List<String> names = page.openSubPages();
            assertTrue(names.indexOf("Univers") < names.indexOf("Chronologie"), "juste après les univers : " + names);
            assertTrue(page.openSubPage("Chronologie"));
            List<String> lines = page.timelineLines();
            assertTrue(lines.stream().anyMatch(l -> l.startsWith("Univers 1 — 2 h 00 min : acte 1 inconnue")), lines.toString());
            assertTrue(lines.stream().anyMatch(l -> l.startsWith("Univers 2 — 1 h 00 min")), lines.toString());
            assertTrue(lines.stream().anyMatch(l -> l.startsWith("Dimension 1 : la ligne : Ouverte à 3 h 00 min  ·  jouée 0.5 s")),
                    lines.toString());
            assertTrue(lines.stream().anyMatch(l -> l.startsWith("Dimension 2 : le plan")), lines.toString());
            assertTrue(lines.contains("Pas encore ouvert : la bulle des racines l'ouvre."), lines.toString());
            return null;
        });
    }

    @Test
    void lesTextesDeLaChronologieSeLisent() {
        assertEquals("87 h 23 min", TimelinePane.at(87 * 3_600 + 23 * 60 + 20));
        assertEquals("avant la version 0.168", TimelinePane.at(Double.NaN));
        assertEquals("inconnue", TimelinePane.span(Double.NaN));
        assertEquals("4,1 %", TimelinePane.share(0.041));
        GameStats.UniverseRecord r = new GameStats.UniverseRecord(3, 0, 600, 1_800, 3_600, 4_200, 7_200);
        assertEquals("acte 1 10 min 00 s  ·  acte 2 20 min 00 s  ·  Big Bangs et astres 30 min 00 s  ·  galaxie 10 min 00 s  ·  amas 50 min 00 s",
                TimelinePane.phases(r));
    }

    @Test
    void unAchatDitJusquOuIlTientEtCeQuIlVautSurSaDureeDeVie() {
        PurchaseHistory.Entry e = new PurchaseHistory.Entry(60, "Amélioration — Dédoublement", 1, "Atomes", "12", 0.5, "Atomes par fusion ×2,000",
                Math.log10(2), Double.NaN, "Premier Big Bang", "jusqu'à la prochaine explosion", 1_500, 750);
        assertEquals("  ·  Tient jusqu'à la prochaine explosion (25 min 00 s la dernière fois) : 12 min 30 s gagnées sur sa durée de vie",
                PurchasesPane.durable(e));
        PurchaseHistory.Entry bubble = new PurchaseHistory.Entry(60, "Bulle de l'arbre — Bulle de la graine", 1, "Énergie du vide dépensée", "—",
                Double.NaN, "aucun effet mesuré sur les grandeurs", 0, Double.NaN, "", "toute la partie", Double.NaN, Double.NaN);
        assertEquals("  ·  Tient toute la partie", PurchasesPane.durable(bubble));
        assertTrue(PurchasesPane.detail(bubble).endsWith("Tient toute la partie"), PurchasesPane.detail(bubble));
    }
}

package idle.ui.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameUniverse.SeedUpgrade;
import idle.ui.FxTestSupport;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.139 : les améliorations de la graine, à gauche de sa vue, sous Xvfb. Une branche par dimension, payée avec
 * sa ressource ; les branches 9 et 10 verrouillées (celle de la boucle, la cinquième, a ses cartes depuis la version
 * 0.153, celle de la brane, la sixième, depuis la version 0.154, celle du clicker, la septième, depuis la version 0.155,
 * celle de la grille, la huitième, depuis la version 0.156) ; le tuyau et le chrono dessinés dans la vue ; le réglage du chrono.
 */
class GraineAmeliorationsPageTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Quatre univers, les quatre dimensions ouvertes, l'arrosoir débloqué et de quoi tout payer. */
    private static Game rich() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        while (game.universes() < 4) assertTrue(game.testAddUniverse());
        assertTrue(game.unlockWateringCan());
        game.state().setDimensionShards(1e12);
        game.state().setPlaneShards(1e12);
        game.state().setVolumeShards(1e12);
        game.state().setDurationShards(1e15);
        return game;
    }

    private static UniversePage page(Game game) {
        UniversePage page = new UniversePage(game);
        new Scene(page, 1200, 760);
        page.select(UniversePage.SEED);
        page.refresh();
        page.applyCss();
        page.layout();
        page.frame(0.016, true);
        return page;
    }

    @Test
    void lesCartesAchetentAvecLaRessourceDeLeurDimension() {
        FxTestSupport.fx(() -> {
            Game game = rich();
            UniversePage page = page(game);
            SeedUpgradesPane pane = page.seedUpgrades();
            assertEquals(1, pane.branch());
            assertTrue(pane.resourceText().getText().startsWith("Segments de la ligne"), pane.resourceText().getText());
            assertTrue(pane.button(SeedUpgrade.WATERING).getText().contains("Débit de l'arrosoir"));
            double segments = game.dimensionResource(1);
            pane.button(SeedUpgrade.WATERING).fire();
            assertEquals(1, game.seedUpgradeLevel(SeedUpgrade.WATERING));
            assertTrue(game.dimensionResource(1) < segments, "payé en segments");
            // Le plan : le tuyau, que la vue dessine aussitôt.
            pane.select(2);
            assertTrue(pane.resourceText().getText().startsWith("Facettes du plan"), pane.resourceText().getText());
            assertFalse(page.seedView().isHoseShown());
            pane.button(SeedUpgrade.HOSE).fire();
            assertTrue(game.hasHose());
            for (int i = 0; i < 60; i++) page.frame(0.016, true);
            assertTrue(page.seedView().isHoseShown());
            assertTrue(page.seedView().hoseDrops() > 0, "le tuyau arrose");
            // Le volume : la terre fertile double chaque arrosage.
            pane.select(3);
            pane.button(SeedUpgrade.SOIL).fire();
            assertEquals(2, game.seedSoilFactor());
            return null;
        });
    }

    /** Version 0.140 : les cartes se voient mieux, même format ; leur état décide de leurs couleurs. */
    @Test
    void lesCartesDisentCeQuiSAcheteCeQuiManqueEtCeQuiEstFerme() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.testFormUniverse();
            while (game.universes() < 2) assertTrue(game.testAddUniverse());
            game.state().setPlaneShards(120);
            SeedUpgradesPane pane = page(game).seedUpgrades();
            pane.select(2);
            javafx.scene.control.Button hose = pane.button(SeedUpgrade.HOSE);
            assertEquals(SeedUpgradesPane.CardState.SHORT, SeedUpgradesPane.state(hose));
            assertTrue(hose.getText().contains("il en manque 230"), hose.getText());
            assertTrue(hose.isDisabled());
            assertTrue(hose.getStyle().contains("-fx-opacity: 1"), "désactivée, la carte reste lisible");
            assertEquals(SeedUpgradesPane.CardState.LOCKED, SeedUpgradesPane.state(pane.button(SeedUpgrade.CHRONO_CAPACITY)));
            assertFalse(pane.isBranchOpen(3), "deux univers : le volume n'est pas ouvert");
            game.state().setPlaneShards(400);
            pane.refresh();
            assertEquals(SeedUpgradesPane.CardState.BUY, SeedUpgradesPane.state(hose));
            assertFalse(hose.isDisabled());
            assertTrue(pane.hasPurchase(2), "l'onglet du plan porte sa pastille");
            hose.fire();
            assertEquals(1, game.seedUpgradeLevel(SeedUpgrade.HOSE));
            assertEquals(SeedUpgradesPane.CardState.SHORT, SeedUpgradesPane.state(hose), "le niveau suivant coûte 875");
            game.state().setSeedUpgradeLevel(SeedUpgrade.HOSE.ordinal(), SeedUpgrade.HOSE.max());
            pane.refresh();
            assertEquals(SeedUpgradesPane.CardState.MAX, SeedUpgradesPane.state(hose));
            assertTrue(hose.getText().endsWith("Au maximum"));
            // Une carte garde la hauteur de ses deux lignes : elle ne prend pas toute la colonne.
            assertTrue(hose.prefHeight(-1) < 120, "hauteur " + hose.prefHeight(-1));
            return null;
        });
    }

    /**
     * Version 0.141 : le panneau se rafraîchit quatre fois par seconde sans retirer ses cartes. Avant, un clic dont
     * l'appui et le relâché encadraient un rafraîchissement se perdait (vu avec une vraie souris sous Xvfb).
     */
    @Test
    void lesCartesRestentEnPlaceQuandLePanneauSeRafraichit() {
        FxTestSupport.fx(() -> {
            Game game = rich();
            UniversePage page = page(game);
            SeedUpgradesPane pane = page.seedUpgrades();
            javafx.scene.control.Button card = pane.button(SeedUpgrade.WATERING);
            javafx.scene.Parent column = card.getParent();
            int[] changes = {0};
            ((javafx.scene.layout.Pane) column).getChildren().addListener(
                    (javafx.collections.ListChangeListener<javafx.scene.Node>) change -> changes[0]++);
            for (int i = 0; i < 20; i++) page.frame(0.25, true);
            card.fire();
            pane.refresh();
            assertEquals(0, changes[0], "les cartes de la même branche ne bougent pas");
            assertEquals(column, card.getParent());
            pane.select(2);
            assertTrue(changes[0] > 0, "changer de branche change les cartes");
            assertFalse(column.getChildrenUnmodifiable().contains(card));
            return null;
        });
    }

    @Test
    void lesBranchesDesDimensionsAVenirSontVerrouillees() {
        FxTestSupport.fx(() -> {
            Game game = rich();
            SeedUpgradesPane pane = page(game).seedUpgrades();
            // La boucle (version 0.153) : sa branche a ses vraies cartes, fermées tant que la dimension 5 ne l'est pas.
            assertFalse(pane.isBranchOpen(5));
            assertEquals("Boucle", SeedUpgradesPane.branchName(5));
            pane.select(5);
            assertTrue(pane.button(SeedUpgrade.BLOOM_DURATION).isDisabled());
            assertTrue(pane.button(SeedUpgrade.BLOOM_DURATION).getText().endsWith("S'ouvre avec la dimension 5"),
                    pane.button(SeedUpgrade.BLOOM_DURATION).getText());
            // La brane (version 0.154) : sa branche a la lumière astrale, fermée tant que la dimension 6 ne l'est pas.
            assertFalse(pane.isBranchOpen(6));
            assertEquals("Brane", SeedUpgradesPane.branchName(6));
            pane.select(6);
            assertTrue(pane.button(SeedUpgrade.ASTRAL_LIGHT).isDisabled());
            assertTrue(pane.button(SeedUpgrade.ASTRAL_LIGHT).getText().endsWith("S'ouvre avec la dimension 6"),
                    pane.button(SeedUpgrade.ASTRAL_LIGHT).getText());
            // Le clicker (version 0.155) : sa branche a les trois améliorations de la météo, fermées sans la dimension 7.
            assertFalse(pane.isBranchOpen(7));
            assertEquals("Clicker", SeedUpgradesPane.branchName(7));
            pane.select(7);
            for (SeedUpgrade weather : new SeedUpgrade[] {SeedUpgrade.WEATHER_SUN, SeedUpgrade.WEATHER_STORM, SeedUpgrade.WEATHER_POWER}) {
                assertTrue(pane.button(weather).isDisabled());
                assertTrue(pane.button(weather).getText().endsWith("S'ouvre avec la dimension 7"), pane.button(weather).getText());
            }
            // La grille (version 0.156) : sa branche a la graine profonde, fermée sans la dimension 8.
            assertFalse(pane.isBranchOpen(8));
            assertEquals("Grille", SeedUpgradesPane.branchName(8));
            pane.select(8);
            assertTrue(pane.button(SeedUpgrade.DEEP_SEED).isDisabled());
            assertTrue(pane.button(SeedUpgrade.DEEP_SEED).getText().endsWith("S'ouvre avec la dimension 8"), pane.button(SeedUpgrade.DEEP_SEED).getText());
            for (int d = 9; d <= SeedUpgradesPane.BRANCHES; d++) {
                assertFalse(pane.isBranchOpen(d));
                pane.select(d);
                assertEquals("Dim. " + d, SeedUpgradesPane.branchName(d));
                assertEquals(2, pane.placeholders().size());
                assertTrue(pane.placeholders().stream().allMatch(b -> b.isDisabled() && b.getText().startsWith("???")));
                assertTrue(pane.placeholders().get(0).getText().contains("dimension " + d));
            }
            for (int d = 1; d <= 4; d++) assertTrue(pane.isBranchOpen(d));
            return null;
        });
    }

    @Test
    void leChronoSAchetePuisSeRegle() {
        FxTestSupport.fx(() -> {
            Game game = rich();
            UniversePage page = page(game);
            SeedUpgradesPane pane = page.seedUpgrades();
            assertFalse(pane.chronoSlider().isVisible() && pane.chronoSlider().getParent().isVisible());
            pane.select(1);
            assertTrue(pane.button(SeedUpgrade.CHRONO_FILL).isDisabled(), "sans l'étage, pas de remplissage");
            pane.select(4);
            assertTrue(pane.resourceText().getText().startsWith("Instants de la durée"), pane.resourceText().getText());
            pane.button(SeedUpgrade.CHRONO).fire();
            assertTrue(game.isChronoUnlocked());
            for (int i = 0; i < 30; i++) page.frame(0.016, true);
            assertTrue(page.seedView().isChronoShown(), "l'étage du chrono sur l'étagère");
            assertTrue(pane.chronoSlider().getParent().isVisible());
            assertTrue(pane.chronoText().getText().startsWith("Gardé"), pane.chronoText().getText());
            assertEquals(Game.CHRONO_MAX_SPEED, pane.chronoSlider().getMax(), 1e-9);
            pane.chronoSlider().setValue(1.6);
            assertEquals(1.5, game.chronoSpeed(), 1e-9, "le réglage va par quarts");
            pane.select(1);
            assertFalse(pane.button(SeedUpgrade.CHRONO_FILL).isDisabled());
            pane.button(SeedUpgrade.CHRONO_FILL).fire();
            assertEquals(1, game.seedUpgradeLevel(SeedUpgrade.CHRONO_FILL));
            return null;
        });
    }
}

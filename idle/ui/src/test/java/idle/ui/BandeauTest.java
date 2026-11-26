package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.*;

/**
 * Version 0.309 : le bandeau du haut (objectifs à gauche, l'onglet au centre, aide, détails et quantité à droite) et les
 * pastilles des grands pas, qui remplacent la barre violette de l'explosion.
 */
class BandeauTest {
    @BeforeAll
    static void setup() { FxTestSupport.start(); }

    @Test
    void lesObjectifsCourtsGardentLeTexteCompletAuSurvol() {
        FxTestSupport.fx(() -> {
            PageHeader header = new PageHeader(new Button("H : aide"), new Label("Ctrl : détails"));
            new Scene(new StackPane(header), 1600, 300);
            header.setGoals(List.of(new Goals.Line("Objectif : le Big Bang", "Objectif : déclencher le Big Bang, au bout de l'arbre"),
                    new Goals.Line("Acte 3 : autres assemblages", "Troisième acte : former d'autres assemblages")));
            assertEquals(List.of("Objectif : le Big Bang", "Acte 3 : autres assemblages"), header.goalTexts());
            header.setGoals(List.of(new Goals.Line("Objectif : l'explosion", "Objectif : faire exploser le tableau périodique")));
            assertEquals(List.of("Objectif : l'explosion"), header.goalTexts(), "une ligne de moins : l'autre se cache");
            header.setGoals(List.of());
            assertEquals(List.of(), header.goalTexts());
            return null;
        });
    }

    @Test
    void leCentreEtLaQuantiteSuiventLOnglet() {
        FxTestSupport.fx(() -> {
            PageHeader header = new PageHeader(new Button("H : aide"), new Label("Ctrl : détails"));
            new Scene(new StackPane(header), 1600, 300);
            Label count = new Label("Particules : 12");
            Label amount = new Label("Quantité : ×1 ×10 max");
            header.setCenter(count);
            header.setExtra(amount);
            assertTrue(count.getParent() != null && amount.getParent() != null);
            header.setCenter(null);
            header.setExtra(null);
            assertEquals(null, count.getParent(), "un autre onglet : le centre se vide");
            assertEquals(null, amount.getParent());
            return null;
        });
    }

    @Test
    void leBandeauRapetisseSousSaLargeurLimiteAvecSaRessource() {
        FxTestSupport.fx(() -> {
            PageHeader header = new PageHeader(new Button("H : aide"), new Label("Ctrl : détails"));
            StackPane root = new StackPane(header);
            new Scene(root, 1600, 300);
            idle.ui.common.ResourceHead head = new idle.ui.common.ResourceHead("Atomes", GameApp.ATOMS_COLOR);
            header.setCenter(head);
            root.applyCss();
            root.layout();
            assertFalse(header.compact());
            String big = head.value().getStyle();
            header.resize(640, 80);
            assertTrue(header.compact(), "à 640 px, tout rapetisse");
            assertTrue(!big.equals(head.value().getStyle()), "la ressource du centre aussi");
            header.resize(1280, 80);
            assertFalse(header.compact());
            assertEquals(big, head.value().getStyle(), "et elle regrandit");
            return null;
        });
    }

    @Test
    void laRessourceDeLOngletSAfficheEnTitreEtEnNombre() {
        FxTestSupport.fx(() -> {
            idle.ui.common.ResourceHead head = new idle.ui.common.ResourceHead("Atomes", GameApp.ATOMS_COLOR);
            head.set("1.2e30", "+3 particules par création   |   +2 atomes par fusion");
            assertEquals("1.2e30", head.value().getText(), "le nombre seul : « Atomes : » est déjà le titre");
            assertTrue(head.sub().getText().contains("par fusion"));
            String big = head.value().getStyle();
            head.setCompact(true);
            assertTrue(!big.equals(head.value().getStyle()), "plus petit quand le bandeau rapetisse");
            return null;
        });
    }

    @Test
    void unePastilleParGrandPasPossibleToujoursDansLeMemeOrdre() {
        FxTestSupport.fx(() -> {
            StepDots dots = new StepDots();
            for (StepDots.Step step : StepDots.Step.values()) assertFalse(dots.shown(step), "rien de possible : aucune pastille");
            dots.show(StepDots.Step.EXPLOSION, true, "+12 matière noire");
            dots.show(StepDots.Step.FUSION, true, "");
            assertTrue(dots.shown(StepDots.Step.FUSION) && dots.shown(StepDots.Step.EXPLOSION));
            assertFalse(dots.shown(StepDots.Step.BIG_BANG));
            assertEquals(4, dots.getChildren().size(), "les quatre gardent leur place dans l'ordre des actes");
            int[] clicks = {0};
            dots.setOnClick(StepDots.Step.EXPLOSION, () -> clicks[0]++);
            dots.getChildren().get(StepDots.Step.EXPLOSION.ordinal()).getOnMouseClicked().handle(null);
            assertEquals(1, clicks[0], "un clic sur la pastille de l'explosion la demande");
            dots.show(StepDots.Step.EXPLOSION, false, "");
            assertFalse(dots.shown(StepDots.Step.EXPLOSION));
            return null;
        });
    }
}

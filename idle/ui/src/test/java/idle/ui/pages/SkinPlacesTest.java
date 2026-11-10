package idle.ui.pages;

import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.SkinVisual;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Version 0.203 : chaque visuel de l'onglet Skins dit où il se voit, en une phrase finie ; seuls les dix mini-jeux partagent la leur. */
class SkinPlacesTest {

    @Test
    void chaqueVisuelDitOuIlSeVoit() {
        Map<String, SkinVisual> seen = new HashMap<>();
        for (SkinVisual visual : SkinVisual.values()) {
            String place = SkinPlaces.of(visual);
            assertTrue(place != null && place.length() > 20 && place.endsWith("."), visual + " : « " + place + " »");
            boolean dimension = java.util.Arrays.asList(SkinVisual.DIMENSIONS).contains(visual);
            SkinVisual other = seen.put(place, visual);
            assertTrue(other == null || dimension, visual + " a la même phrase que " + other);
        }
        assertTrue(SkinPlaces.of(SkinVisual.MATTER_SIGNS).contains("∴"), "les signes se montrent dans leur phrase");
        assertTrue(SkinPlaces.of(SkinVisual.DIMENSION_FRAME).contains("décoratif"));
    }
}

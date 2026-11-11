import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Version 0.205 : le plan de {@link ChargeArrets} ne nomme que des coupures connues, chacune une seule fois. */
class ChargeArretsTest {

    @Test
    void lePlanCompletNommeDesCoupuresConnuesUneSeuleFois() {
        Map<String, String> plan = ChargeArrets.plan("tout");
        assertEquals(List.of("Big Bang 1", "Big Bang 5", "Big Bang 8", "galaxie", "amas"), List.copyOf(plan.keySet()));
        Set<String> vus = new HashSet<>();
        for (String ids : plan.values()) {
            for (AnalyseArrets.Coupure c : AnalyseArrets.choisies(ids)) assertTrue(vus.add(c.id()), c.id() + " coupé deux fois");
        }
        // 14 automatismes (la synthèse sert jusqu'au bout), les 12 améliorations, 2 mécaniques.
        assertEquals(28, vus.size());
        assertFalse(vus.contains("auto_synthesis"), "la synthèse sert encore à l'amas : jamais coupée");
        for (String utile : List.of("sondes", "impulsions", "cometes", "anomalies")) assertFalse(vus.contains(utile), utile);
    }

    @Test
    void unPlanParFamilleNeCoupeQueSaFamille() {
        for (String famille : List.of("automatismes", "ameliorations", "mecaniques")) {
            for (String ids : ChargeArrets.plan(famille).values()) {
                if (ids.isEmpty()) continue;
                for (AnalyseArrets.Coupure c : AnalyseArrets.choisies(ids)) assertEquals(famille, c.famille(), c.id());
            }
        }
    }
}

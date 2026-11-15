package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import idle.ui.common.Card;
import java.util.*;
import org.junit.jupiter.api.*;

/**
 * Une carte qui agit le dit, avec son état d'avant (à portée : un achat ; allumée ou éteinte : un
 * interrupteur), au clic comme au clic tenu ; une carte hors de portée ne dit rien. C'est ce qui fait sonner les achats.
 */
class CartesSonnentTest {
    @BeforeAll
    static void start() { FxTestSupport.start(); }

    @AfterEach
    void unplug() { Card.setOnActed(null); }

    @Test
    void uneCarteQuiAgitLeDitAvecSonEtat() {
        List<Card.State> heard = new ArrayList<>();
        FxTestSupport.fx(() -> {
            Card.setOnActed(heard::add);
            Card card = new Card("#9fd0ff");
            card.setOnAction(() -> { });
            card.show(Card.State.WAITING, "1", "", "Vitesse", "", "", "10 particules", "");
            card.click();
            card.show(Card.State.READY, "1", "", "Vitesse", "", "", "10 particules", "");
            card.click();
            card.show(Card.State.ON, "1", "", "Automatisme", "", "", "", "");
            card.click();
            card.show(Card.State.OFF, "1", "", "Automatisme", "", "", "", "");
            card.click();
            Card tile = new Card("#9fd0ff");
            tile.show(Card.State.READY, "", "", "Succès", "", "", "", "");
            tile.click();
            return null;
        });
        assertEquals(List.of(Card.State.READY, Card.State.ON, Card.State.OFF), heard, "hors de portée ou sans action : rien");
    }
}

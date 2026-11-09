package idle.ui.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.molecules.Molecule;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.FxTestSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.192 : les skins des briques communes. Les signes d'état des cartes changent de famille sans perdre un état
 * (huit signes, tous différents du classique sauf où la famille les confond) ; les signes des états de la matière de
 * même, dans tous les textes qui les portent ; les info-bulles des compteurs prennent leur style au rafraîchissement,
 * et le rendent, au caractère près, sans skin.
 */
class SignesEtBullesSkinsTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    @AfterEach
    void classic() {
        Card.setSignSkin(null);
        MatterText.setSkin(null);
        Breakdown.setSkin(null);
    }

    private static List<Skin> skinsOf(SkinVisual visual) {
        List<Skin> skins = new ArrayList<>();
        for (Skin skin : Skin.values()) if (skin.visual() == visual) skins.add(skin);
        return skins;
    }

    @Test
    void chaqueFamilleDeSignesDEtatEstComplete() {
        FxTestSupport.fx(() -> {
            assertNull(Card.signSkin());
            List<String> classic = new ArrayList<>();
            for (Card.State state : Card.State.values()) classic.add(Card.sign(state));
            assertEquals("●", classic.get(0));
            List<Skin> skins = skinsOf(SkinVisual.CARD_SIGNS);
            assertEquals(3, skins.size());
            for (Skin skin : skins) {
                Card.setSignSkin(skin);
                assertEquals(skin, Card.signSkin());
                Set<String> seen = new HashSet<>();
                for (Card.State state : Card.State.values()) {
                    String sign = Card.sign(state);
                    assertEquals(1, sign.length(), skin + " " + state + " : un seul caractère");
                    assertNotEquals(classic.get(state.ordinal()), sign, skin + " " + state);
                    seen.add(sign);
                }
                // Les lettres confondent « en attente » et « armée » (A, comme dans SKINS.md) ; les autres familles distinguent tout.
                assertEquals(skin == Skin.SIGNS_LETTERS ? 7 : 8, seen.size(), skin.name());
                // La carte écrit le signe de son skin devant son nom.
                Card card = new Card("#9fd0ff");
                new Scene(card, 200, 120);
                card.show(Card.State.READY, "12", "", "Vitesse", "", "", "", "");
                assertTrue(card.text().contains(Card.sign(Card.State.READY) + " Vitesse"), card.text());
            }
            Card.setSignSkin(Skin.CARD_INDEX);     // un skin d'un autre visuel : les signes de toujours
            for (Card.State state : Card.State.values()) assertEquals(classic.get(state.ordinal()), Card.sign(state));
            Card.setSignSkin(null);
            assertEquals("P", letter(Skin.SIGNS_LETTERS, Card.State.READY));
            assertEquals("★", letter(Skin.SIGNS_SHAPES, Card.State.DONE));
            return null;
        });
    }

    private static String letter(Skin skin, Card.State state) {
        Card.setSignSkin(skin);
        String sign = Card.sign(state);
        Card.setSignSkin(null);
        return sign;
    }

    @Test
    void chaqueFamilleDeSignesDeLaMatiereEstComplete() {
        assertNull(MatterText.skin());
        assertEquals("∴ Gaz", MatterText.signed(Molecule.State.GAS));
        List<Skin> skins = skinsOf(SkinVisual.MATTER_SIGNS);
        assertEquals(3, skins.size());
        for (Skin skin : skins) {
            MatterText.setSkin(skin);
            Set<String> seen = new HashSet<>();
            for (Molecule.State state : Molecule.State.values()) {
                String sign = MatterText.sign(state);
                assertEquals(1, sign.length(), skin + " " + state);
                assertFalse("∴≈◆".contains(sign), skin + " " + state + " : autre que le classique");
                seen.add(sign);
                assertTrue(MatterText.signed(state).startsWith(sign + " "), skin.name());
                assertTrue(MatterText.plural(state).startsWith(sign + " "), skin.name());
            }
            assertEquals(3, seen.size(), skin + " : trois signes différents");
        }
        MatterText.setSkin(Skin.MATTER_LETTERS);
        assertEquals("G gaz", MatterText.plural(Molecule.State.GAS));
        assertEquals("C Cristaux", MatterText.signed(Molecule.State.CRYSTAL));
        MatterText.setSkin(null);
        assertEquals("≈ liquides", MatterText.plural(Molecule.State.LIQUID));
    }

    @Test
    void chaqueSkinDeBulleDonneUnStyleEtLeClassiqueRevient() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            Label label = new Label("x");
            Breakdown breakdown = Breakdown.attach(label, game, Breakdown.Of.PARTICLES);
            breakdown.refresh();
            assertEquals(Breakdown.CLASSIC_STYLE, breakdown.tooltipStyle());
            List<Skin> skins = skinsOf(SkinVisual.TOOLTIP);
            assertEquals(3, skins.size());
            Set<String> styles = new HashSet<>();
            for (Skin skin : skins) {
                Breakdown.setSkin(skin);
                assertEquals(skin, Breakdown.skin());
                breakdown.refresh();
                String style = breakdown.tooltipStyle();
                assertNotEquals(Breakdown.CLASSIC_STYLE, style, skin.name());
                assertTrue(style.contains("-fx-background-color:"), style);
                assertTrue(style.contains("-fx-text-fill:"), style);
                styles.add(style);
                assertFalse(breakdown.text().isEmpty());
            }
            assertEquals(3, styles.size(), "trois styles différents");
            assertTrue(Breakdown.style(Skin.TOOLTIP_POSTIT).contains("#f9eeda"), "le jaune du post-it");
            assertTrue(Breakdown.style(Skin.TOOLTIP_TERMINAL).contains("Monospaced"), "le terminal à chasse fixe");
            assertTrue(Breakdown.style(Skin.TOOLTIP_PARCHMENT).contains("serif"), "le parchemin à empattements");
            assertEquals(Breakdown.CLASSIC_STYLE, Breakdown.style(Skin.CARD_INDEX), "un skin d'un autre visuel");
            Breakdown.setSkin(null);
            breakdown.refresh();
            assertEquals(Breakdown.CLASSIC_STYLE, breakdown.tooltipStyle());
            return null;
        });
    }
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.save.SaveData;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.203 : six skins retirés du jeu (l'œuf, la pluie et le feu d'artifice de la matière noire ; le ballon noué, la
 * planète-fruit et l'œuf des bulles des univers). Une sauvegarde d'avant, qui les nomme encore parmi les skins débloqués ou
 * choisis, se relit sans eux et sans erreur ; les bulles qui restent viennent aux univers 1, 2 et 3.
 */
class SkinsRetiresTest {

    private static final List<String> RETIRES = List.of("dark_egg", "dark_rain", "dark_fireworks", "bubble_balloon", "bubble_fruit", "bubble_egg");

    @Test
    void lesSixSkinsNExistentPlus() {
        for (String id : RETIRES) assertNull(Skin.byId(id), id);
        assertEquals(3, Skin.UNIVERSE_BUBBLES.length);
        int balls = 0, animations = 0;
        for (Skin skin : Skin.values()) {
            if (skin.visual() == SkinVisual.DARK_BALL) balls++;
            if (skin.visual() == SkinVisual.DARK_ANIMATION) animations++;
        }
        assertEquals(7, balls, "huit boules moins l'œuf");
        assertEquals(4, animations, "six animations moins la pluie et le feu d'artifice");
    }

    @Test
    void uneSauvegardeDAvantSeRelitSansEux() {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        game.state().unlockSkin(Skin.DARK_HOLE.id());
        game.state().unlockSkin(Skin.BUBBLE_SOAP.id());
        game.state().chooseSkin(SkinVisual.TREE.name(), Skin.TREE_PINE.id());
        SaveData data = new SaveData();
        game.state().save(data);
        // La sauvegarde d'une 0.202 : les six skins débloqués, deux d'entre eux choisis.
        List<String> unlocked = new ArrayList<>(data.list("unlockedSkins"));
        unlocked.addAll(RETIRES);
        data.putList("unlockedSkins", unlocked);
        List<String> chosen = new ArrayList<>(data.list("chosenSkins"));
        chosen.add(SkinVisual.DARK_BALL.name() + ">dark_egg");
        chosen.add(SkinVisual.UNIVERSE_BUBBLE.name() + ">bubble_fruit");
        data.putList("chosenSkins", chosen);

        GameState relu = new GameState();
        relu.load(data);
        for (String id : RETIRES) assertFalse(relu.unlockedSkins().contains(id), id + " ne se relit pas");
        assertTrue(relu.unlockedSkins().contains(Skin.DARK_HOLE.id()), "les autres restent");
        assertTrue(relu.unlockedSkins().contains(Skin.BUBBLE_SOAP.id()));
        assertFalse(relu.chosenSkins().containsKey(SkinVisual.DARK_BALL.name()), "un skin retiré n'est plus choisi : le Classique revient");
        assertFalse(relu.chosenSkins().containsKey(SkinVisual.UNIVERSE_BUBBLE.name()));
        assertEquals(Skin.TREE_PINE.id(), relu.chosenSkins().get(SkinVisual.TREE.name()), "les autres choix restent");
    }

    @Test
    void lesBullesQuiRestentViennentAuxTroisPremiersUnivers() {
        Game game = GameTestSupport.universe(1);
        game.state().setSkinsOpen(true);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.BUBBLE_SOAP));
        assertFalse(game.hasSkin(Skin.BUBBLE_SNOWGLOBE));
        assertTrue(game.testAddUniverse());
        game.tick(1);
        assertTrue(game.hasSkin(Skin.BUBBLE_SNOWGLOBE), "la boule à neige à l'univers 2 (3 avant)");
        assertFalse(game.hasSkin(Skin.BUBBLE_ORRERY));
        assertTrue(game.testAddUniverse());
        game.tick(1);
        assertTrue(game.hasSkin(Skin.BUBBLE_ORRERY), "le globe d'orrery à l'univers 3 (4 avant)");
        assertEquals("Univers 2 formé", Skin.BUBBLE_SNOWGLOBE.source());
        assertEquals("Univers 3 formé", Skin.BUBBLE_ORRERY.source());
    }
}

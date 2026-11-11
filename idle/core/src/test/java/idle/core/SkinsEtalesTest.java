package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Version 0.206 : les skins s'étalent sur la partie. Les skins payants d'un visuel s'échelonnent du deuxième univers au
 * onzième dans l'ordre où ils sont déclarés ; un skin n'est débloqué qu'à partir de son univers, même quand sa condition
 * est remplie ; une sauvegarde qui l'avait déjà le garde.
 */
class SkinsEtalesTest {

    @Test
    void lesSkinsDUnVisuelSEchelonnentDuDeuxiemeAuOnziemeUnivers() {
        int[] parUnivers = new int[Skin.LAST_SKIN_UNIVERSE + 1];
        for (SkinVisual visual : SkinVisual.values()) {
            List<Skin> paid = new ArrayList<>();
            for (Skin skin : Skin.values()) {
                if (skin.visual() != visual) continue;
                if (skin.isFree()) {
                    assertEquals(0, skin.minUniverses(), "un skin libre n'attend rien : " + skin);
                } else if (skin.isWon()) {
                    assertEquals(1, skin.minUniverses(), "un skin gagné par une action vient dès le deuxième univers : " + skin);
                } else {
                    paid.add(skin);
                }
            }
            if (paid.isEmpty()) continue;
            assertTrue(paid.get(0).minUniverses() >= 1 && paid.get(0).minUniverses() <= 3,
                    "le premier skin du visuel vient aux univers 2 à 4 : " + visual + " " + paid.get(0).minUniverses());
            assertTrue(paid.get(paid.size() - 1).minUniverses() <= Skin.LAST_SKIN_UNIVERSE, "au plus le onzième : " + visual);
            for (int k = 1; k < paid.size(); k++) {
                assertTrue(paid.get(k).minUniverses() >= paid.get(k - 1).minUniverses(), "dans l'ordre : " + paid.get(k));
            }
            for (Skin skin : paid) parUnivers[skin.minUniverses()]++;
        }
        // Chaque univers formé, du premier au dixième, ouvre à peu près autant de skins : la collection dure jusqu'au bout.
        int total = Skin.values().length - freeSkins() - wonSkins();
        for (int u = 1; u <= Skin.LAST_SKIN_UNIVERSE; u++) {
            assertTrue(Math.abs(parUnivers[u] - total / (double) Skin.LAST_SKIN_UNIVERSE) <= 1,
                    "un dixième des skins attendent " + u + " univers formés : " + parUnivers[u] + " sur " + total);
        }
        assertEquals(0, parUnivers[0]);
        // Moins de cent skins possibles avant le quatrième univers : le bonus ne plafonne plus dès le deuxième.
        assertTrue(parUnivers[1] + parUnivers[2] + wonSkins() < Skin.MAX_COUNTED, "avant trois univers formés");
    }

    @Test
    void unSkinAttendSonUniversMemeConditionRemplie() {
        // Le fond « Parchemin » (chrome) vient à une heure de jeu ; il n'est pas le premier de son visuel.
        Skin later = null;
        for (Skin skin : Skin.values()) {
            if (skin.visual() == SkinVisual.AMBIENT && !skin.isFree() && skin.minUniverses() >= 3) { later = skin; break; }
        }
        assertTrue(later != null);
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        assertFalse(game.unlockSkin(later), "deux univers formés : trop tôt pour " + later);
        assertFalse(game.hasSkin(later));
        Game enough = GameTestSupport.universe(later.minUniverses());
        enough.state().setSkinsOpen(true);
        assertTrue(enough.unlockSkin(later), "à son univers, il se débloque");
        assertTrue(enough.hasSkin(later));
        // Le premier skin d'un visuel ne change pas : dès l'onglet ouvert, dans le deuxième univers.
        Skin first = null;
        for (Skin skin : Skin.values()) if (skin.visual() == SkinVisual.AMBIENT && !skin.isFree()) { first = skin; break; }
        assertTrue(first.minUniverses() <= 3);
        assertTrue(GameTestSupport.universe(3).unlockSkin(first));
    }

    @Test
    void lesPaliersAutomatiquesAttendentAussiLeurUnivers() {
        // Les skins des paliers (matière noire, Big Bang) se débloquent seuls à chaque pas : pas avant leur univers.
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        game.state().setTimePlayed(100 * 3600);
        game.tick(1);
        game.tick(1);
        for (Skin skin : Skin.values()) {
            if (skin.minUniverses() > 2) assertFalse(game.hasSkin(skin), "attend " + (skin.minUniverses() + 1) + " univers : " + skin);
        }
    }

    @Test
    void uneSauvegardeGardeUnSkinObtenuAvant() {
        Game game = GameTestSupport.universe(2);
        Skin last = Skin.MOLECULE_BRICKS;
        assertTrue(last.minUniverses() > 2, "le dernier skin des molécules attend : " + last.minUniverses());
        game.state().unlockSkin(last.id());   // obtenu avant la 0.206, relu tel quel
        assertTrue(game.hasSkin(last));
        idle.core.save.SaveData data = new idle.core.save.SaveData();
        game.state().save(data);
        GameState relu = new GameState();
        relu.load(data);
        assertTrue(relu.unlockedSkins().contains(last.id()), "la sauvegarde le garde");
    }

    private static int wonSkins() {
        int n = 0;
        for (Skin skin : Skin.values()) if (skin.isWon()) n++;
        return n;
    }

    private static int freeSkins() {
        int n = 0;
        for (Skin skin : Skin.values()) if (skin.isFree()) n++;
        return n;
    }
}

package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import idle.ui.pages.SkinShelf;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Le rangement de l'onglet Skins (version 0.249), sans affichage : chaque visuel dans une seule catégorie, et un skin
 * qui ne se montre qu'une fois son palier atteint (son univers, sa dimension), ou déjà débloqué.
 */
class RangementDesSkinsTest {
    @Test
    void chaqueVisuelEstDansUneSeuleCategorie() {
        Map<SkinVisual, SkinShelf.Category> seen = new EnumMap<>(SkinVisual.class);
        for (SkinShelf.Category category : SkinShelf.Category.values()) {
            assertFalse(category.visuals().isEmpty(), category.label());
            for (SkinVisual visual : category.visuals()) assertEquals(null, seen.put(visual, category), visual + " dans deux catégories");
        }
        for (SkinVisual visual : SkinVisual.values()) assertTrue(seen.containsKey(visual), visual + " : dans aucune catégorie");
    }

    /** Une partie au premier univers formé, ses dimensions ouvertes ou non. */
    private static Game universe() {
        Game game = new Game(5);
        game.start();
        game.testFormUniverse();
        game.state().setSkinsOpen(true);
        return game;
    }

    @Test
    void unSkinSeMontreUneFoisSonUniversAtteintOuDejaDebloque() {
        Game game = universe();
        for (Skin skin : Skin.values()) {
            boolean palier = game.universes() >= skin.minUniverses();
            int dimension = java.util.Arrays.asList(SkinVisual.DIMENSIONS).indexOf(skin.visual()) + 1;
            boolean open = dimension == 0 || game.dimensionsUnlocked() >= dimension;
            assertEquals(game.hasSkin(skin) || palier && open, SkinShelf.shown(game, skin), skin.name());
        }
        Skin later = null;
        for (Skin skin : Skin.values()) if (later == null && !game.hasSkin(skin) && skin.minUniverses() > game.universes()) later = skin;
        assertTrue(later != null, "un skin d'un univers à venir");
        assertFalse(SkinShelf.shown(game, later));
        game.state().unlockSkin(later.id());
        assertTrue(SkinShelf.shown(game, later), "débloqué (une sauvegarde d'avant, un outil d'essai) : il se montre toujours");
    }

    @Test
    void lesSkinsDUneDimensionAttendentQuElleSOuvre() {
        Game game = universe();
        Skin line = null;
        for (Skin skin : Skin.values()) if (skin.visual() == SkinVisual.DIMENSIONS[Game.MAX_DIMENSIONS - 1] && !skin.isFree()) line = skin;
        if (line == null) return; // pas de skin payant pour la dernière dimension
        for (int i = 0; i < 20 && game.universes() < line.minUniverses(); i++) game.testAddUniverse();
        if (game.dimensionsUnlocked() < Game.MAX_DIMENSIONS && !game.hasSkin(line)) {
            assertFalse(SkinShelf.shown(game, line), "la dixième dimension fermée : son skin reste caché");
        }
        new TestActions(game).openDimension(Game.MAX_DIMENSIONS);
        if (game.universes() >= line.minUniverses()) assertTrue(SkinShelf.shown(game, line), "la dimension ouverte : il se montre");
    }

    @Test
    void lesComptesEtLesCategoriesSuiventCeQuiSeMontre() {
        Game game = universe();
        int shownAll = 0;
        for (SkinShelf.Category category : SkinShelf.Category.values()) {
            int[] counts = SkinShelf.counts(game, category);
            assertTrue(counts[0] <= counts[1], category.label());
            assertEquals(counts[1] > 0, SkinShelf.shown(game, category), category.label() + " : cachée sans skin montré");
            shownAll += counts[1];
        }
        int expected = 0;
        for (Skin skin : Skin.values()) if (SkinShelf.shown(game, skin)) expected++;
        assertEquals(expected, shownAll, "chaque skin montré compte dans une seule catégorie");
        assertTrue(shownAll < Skin.values().length, "au premier univers, des skins restent cachés");
        game.testAllSkins(true);
        for (SkinShelf.Category category : SkinShelf.Category.values()) {
            int[] counts = SkinShelf.counts(game, category);
            assertEquals(counts[0], counts[1], category.label() + " : tout débloqué, tout montré");
        }
    }
}

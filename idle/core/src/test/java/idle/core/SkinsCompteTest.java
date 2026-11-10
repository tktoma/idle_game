package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.save.SaveCodec;
import idle.core.save.SaveData;
import idle.core.universe.Skin;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Version 0.202 : le compte des skins débloqués se garde tant que les skins ne changent pas (le bonus des skins le relit
 * à chaque fusion : sur ~370 skins, il prenait jusqu'au tiers du calcul d'une partie). Il doit toujours valoir le compte
 * fait skin par skin, quoi qu'il arrive aux skins : déblocage, verrouillage, univers suivant, remise à zéro, relecture.
 */
class SkinsCompteTest {

    /** Le compte fait à la main, skin par skin, comme avant la 0.202. */
    private static int[] recompte(Game game) {
        int unlocked = 0, counted = 0;
        for (Skin skin : Skin.values()) {
            if (!game.hasSkin(skin)) continue;
            unlocked++;
            if (!skin.isFree()) counted++;
        }
        return new int[] {unlocked, Math.min(counted, Skin.MAX_COUNTED)};
    }

    private static void memeCompte(Game game, String quand) {
        int[] attendu = recompte(game);
        assertEquals(attendu[0], game.skinsUnlocked(), "skins débloqués " + quand);
        assertEquals(attendu[1], game.skinsCounted(), "skins comptés " + quand);
        assertEquals(1 + Skin.BONUS * attendu[1], game.skinBoost(), 1e-12, "bonus " + quand);
    }

    @Test
    void leCompteSuitChaqueDeblocageEtVerrouillage() {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        memeCompte(game, "au départ");
        Random random = new Random(201);
        Skin[] skins = Skin.values();
        for (int i = 0; i < 2_000; i++) {
            Skin skin = skins[random.nextInt(skins.length)];
            if (random.nextInt(3) == 0) game.state().lockSkin(skin.id());
            else game.state().unlockSkin(skin.id());
            memeCompte(game, "après le geste " + i + " sur " + skin);
        }
        game.state().unlockSkin(skins[0].id());
        int avant = game.skinsUnlocked();
        game.state().unlockSkin(skins[0].id());
        assertEquals(avant, game.skinsUnlocked(), "un skin déjà débloqué ne compte pas deux fois");
        game.state().lockSkin("skin_inconnu");
        memeCompte(game, "après le verrouillage d'un skin inconnu");
    }

    @Test
    void leCompteSuitLUniversSuivantEtLaRelecture() throws Exception {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        int n = 0;
        for (Skin skin : Skin.values()) if (!skin.isFree() && n++ < 40) game.state().unlockSkin(skin.id());
        memeCompte(game, "avec 40 skins");
        int compte = game.skinsCounted();
        assertTrue(compte >= 40, "les quarante skins comptent");
        // Les skins restent d'un univers à l'autre.
        game.testFormUniverse();
        assertTrue(game.startNextUniverse());
        memeCompte(game, "à l'univers suivant");
        assertEquals(compte, game.skinsCounted(), "les skins gardés comptent toujours");
        // La relecture d'une sauvegarde refait l'ensemble : le compte suit.
        Game relue = new Game(SaveCodec.read(SaveCodec.write(game.state(), 0)).state(), Upgrades.DEFAULT, Automations.DEFAULT,
                DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(1));
        memeCompte(relue, "après relecture");
        assertEquals(compte, relue.skinsCounted());
        // Un état relu par-dessus un état déjà compté (même objet, nouvelles valeurs) : le compte suit aussi.
        SaveData vide = new SaveData();
        new GameState().save(vide);
        game.state().load(vide);
        memeCompte(game, "après le chargement d'un état sans skin");
    }
}

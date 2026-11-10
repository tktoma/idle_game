package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.save.SaveData;
import idle.core.upgrades.Automation;
import org.junit.jupiter.api.Test;

/**
 * Version 0.204 : le blocage de mesure ({@link GameStateBase#testBlock}). L'outil des bots qui cherche à partir de quand
 * un automatisme ne sert plus l'interdit d'allumage sur une copie de la partie : ni le joueur, ni le jeu lui-même (un
 * automatisme offert ou racheté s'allume seul) ne peuvent le rallumer. Rien n'est bloqué dans une partie ordinaire, et le
 * blocage ne se sauvegarde pas.
 */
class BlocageDeMesureTest {

    @Test
    void rienNEstBloqueDansUnePartieOrdinaire() {
        GameState state = new GameState();
        assertFalse(state.testBlocking());
        assertFalse(state.testBlocked("auto_fusion"));
        state.setAutomationEnabled("auto_fusion", true);
        assertTrue(state.isAutomationEnabled("auto_fusion"));
    }

    @Test
    void unAutomatismeBloqueNeSeRallumePlus() {
        GameState state = new GameState();
        state.setAutomationEnabled("auto_fusion", true);
        state.setAutomationEnabled("auto_speed", true);
        state.testBlock("auto_fusion");
        // Celui qui bloque éteint lui-même ; ensuite plus rien ne rallume.
        state.setAutomationEnabled("auto_fusion", false);
        state.setAutomationEnabled("auto_fusion", true);
        assertFalse(state.isAutomationEnabled("auto_fusion"));
        assertTrue(state.isAutomationEnabled("auto_speed"), "les autres ne sont pas touchés");
        state.setAutomationEnabled("auto_speed", false);
        state.setAutomationEnabled("auto_speed", true);
        assertTrue(state.isAutomationEnabled("auto_speed"));
    }

    @Test
    void leJeuNonPlusNeLeRallumePasEnLAchetant() {
        Game game = new Game();
        game.start();
        Automation fusion = null;
        for (Automation a : game.automations()) if (a.kind() == Automation.Kind.FUSION) fusion = a;
        game.state().testBlock(fusion.id());
        game.state().setTotalAtoms(idle.core.math.BigNum.of(1000));
        game.state().setAtoms(idle.core.math.BigNum.of(1000));
        if (game.buyAutomation(fusion.id())) {
            assertTrue(game.state().ownsAutomation(fusion.id()), "il s'achète");
            assertFalse(game.isAutomationEnabled(fusion.id()), "mais ne s'allume pas");
        }
        assertFalse(game.setAutomationEnabled(fusion.id(), true) && game.isAutomationEnabled(fusion.id()));
    }

    @Test
    void lesAutresReglagesSeBloquentDeMeme() {
        GameState state = new GameState();
        state.setDarkAutomationEnabled("dark_auto_explosion", true);
        state.setAutoGather(true);
        state.setAutoForm(true);
        state.setAutoMolecules(true);
        state.setAutoHold(true);
        for (String key : new String[] {"dark_auto_explosion", "gather", "form", "molecules", "hold"}) state.testBlock(key);
        state.setDarkAutomationEnabled("dark_auto_explosion", false);
        state.setDarkAutomationEnabled("dark_auto_explosion", true);
        state.setAutoGather(true);
        state.setAutoForm(true);
        state.setAutoMolecules(true);
        state.setAutoHold(true);
        assertFalse(state.isDarkAutomationEnabled("dark_auto_explosion"));
        assertFalse(state.autoGather());
        assertFalse(state.autoForm());
        assertFalse(state.autoMolecules());
        assertFalse(state.autoHold());
    }

    @Test
    void leBlocageNeSeSauvegardePas() {
        GameState state = new GameState();
        state.testBlock("auto_fusion");
        SaveData data = new SaveData();
        state.save(data);
        GameState relu = new GameState();
        relu.load(data);
        assertFalse(relu.testBlocking(), "une sauvegarde relue ne bloque rien");
        assertEquals(false, relu.testBlocked("auto_fusion"));
    }
}

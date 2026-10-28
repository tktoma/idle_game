package idle.core;

import static org.junit.jupiter.api.Assertions.*;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.save.SaveCodec;
import org.junit.jupiter.api.Test;

/**
 * Le suivi facultatif des besoins de matière (version 0.86) : éteint par défaut, il entretient seul la substance de
 * l'état qui manque à la liste de courses, sans toucher à la réserve réglée par le joueur.
 */
class AutoMatterTest {
    private Game prepared() {
        Game g = GameTestSupport.realGame();
        GameTestSupport.bangs(g, 8);
        g.state().setSpace(BigNum.of(1,14));
        GameTestSupport.openSpace(g);
        GameTestSupport.fillTable(g, 200);
        g.setAutoFollowList(false);
        assertTrue(g.isMoleculeAutomationUnlocked());
        assertTrue(g.isStatesUnlocked());
        return g;
    }

    @Test void resteEteintParDefautEtSurvitALaSauvegarde() throws Exception {
        Game g=prepared();
        assertFalse(g.isAutoFollowingMatter());
        assertTrue(g.setAutoFollowMatter(true));
        assertTrue(g.isAutoFollowingMatter());
        GameState copy=SaveCodec.read(SaveCodec.write(g.state(),0)).state();
        assertTrue(copy.autoFollowMatter());
        copy.reset();
        assertFalse(copy.autoFollowMatter());
    }

    @Test void entretientUneSubstanceDeLEtatEnManqueSansChoixManuel() {
        Game g=prepared();
        assertFalse(g.shoppingList().matter().isEmpty(), "un corps attend une matière rassemblée");
        Molecule chosen=g.molecules().stream()
                .filter(m->m.hasState() && g.shoppingList().matter().containsKey(m.state())
                        && g.isMoleculeKindUnlocked(m.kind()))
                .findFirst().orElseThrow();
        g.state().setMoleculeCount(chosen.id(), Game.SUBSTANCE_MOLECULES);
        g.state().addSubstance(chosen.id());
        assertTrue(g.autoMatterTargets().contains(chosen.id()));
        int before=g.moleculeCount(chosen.id());
        g.tick(20);
        assertEquals(before,g.moleculeCount(chosen.id()),"mode éteint : aucune création automatique de matière");
        assertTrue(g.setAutoFollowMatter(true));
        g.tick(20);
        assertTrue(g.moleculeCount(chosen.id())>before,"mode allumé : l'état demandé s'entretient");
    }

    @Test void leJoueurPeutDesactiverEtGarderSaReserve() {
        Game g=prepared();
        assertTrue(g.setAutoFollowMatter(true));
        g.setAutoMoleculeReserve(0.9);
        assertEquals(0.9,g.autoMoleculeReserve());
        assertTrue(g.setAutoFollowMatter(false));
        assertFalse(g.isAutoFollowingMatter());
        assertEquals(0.9,g.autoMoleculeReserve(),"ce nouveau réglage ne modifie pas l'ancien");
    }
}

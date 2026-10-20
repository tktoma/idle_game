package idle.core;

import static org.junit.jupiter.api.Assertions.*;
import idle.core.achievements.Achievements;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.bigbang.BigBangCondition;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.save.SaveCodec;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests réels du moteur 0.86 pour le suivi facultatif des besoins de matière. */
class AutoMatterTest {
    private Game prepared() {
        Game g = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        g.start();
        g.state().setBigBangs(8);
        g.state().setSpace(BigNum.of(1,14));
        for (SpaceUpgrade u : g.spaceUpgrades())
            if (!(u.effect() instanceof SpaceUpgrade.Boost)) g.state().addSpaceUpgrade(u.id());
        for (int n = 1; n <= 118; n++) g.state().setElementCount(n, 200);
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

    @Test void cinqCentsMatiereNoireDoiventEtreDisponiblesEtPasSeulementGagnes() {
        Game g=prepared();
        g.state().setDarkMatter(BigNum.of(470));
        g.state().setDarkMatterSpent(BigNum.of(30));
        assertFalse(g.isBigBangConditionMet(BigBangCondition.DARK_MATTER),
                "470 disponibles + 30 dépensées n'atteignent pas la condition");
        g.state().setDarkMatter(BigNum.of(500));
        assertTrue(g.isBigBangConditionMet(BigBangCondition.DARK_MATTER));
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

package idle.ui;

import static org.junit.jupiter.api.Assertions.*;
import idle.core.Game;
import idle.core.GameState;
import idle.core.achievements.Achievements;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import idle.ui.automation.AutomationPage;
import idle.ui.common.PlanBar;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Test d'interaction sur le véritable thread JavaFX, sous Xvfb dans la CI. */
class GuiInteractionsTest {
    @BeforeAll static void setup() { FxTestSupport.start(); }
    // Version 0.137 : plus de Platform.exit() après ces tests. Le toolkit est partagé (FxTestSupport) et ne redémarre
    // pas : toute classe de test JavaFX lancée ensuite attendait 20 s et échouait (ArrosoirPageTest l'a montré).

    /**
     * Version 0.178 : la création automatique n'a plus de réglage à part pour la liste ou la matière ; sa carte dit ce
     * qu'elle suit, un clic la coupe et la relance, et les amas confiés se retirent d'une pastille.
     */
    @Test void creationAutomatiqueSeCoupeEtRendSesAmas() {
        Game g = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        g.start();g.state().setBigBangs(8);
        // Version 0.124 : un palier de Big Bang ne compte qu'une fois payé (création automatique des molécules au 2e).
        for(idle.core.bigbang.BigBangMilestone milestone:idle.core.bigbang.BigBangMilestones.DEFAULT)
            g.state().addBoughtMilestone(milestone.name());
        for(SpaceUpgrade upgrade:g.spaceUpgrades())
            if(!(upgrade.effect() instanceof SpaceUpgrade.Boost))g.state().addSpaceUpgrade(upgrade.id());
        assertTrue(g.isStateCapUnlocked());
        // Un amas confié : de l'eau rassemblée.
        g.state().setSpace(idle.core.math.BigNum.of(1e9));
        g.state().addMolecules("H2O",Game.SUBSTANCE_MOLECULES);
        assertTrue(g.formSubstance("H2O"));
        assertTrue(g.setMoleculeAutomated("H2O",true));
        FxTestSupport.fx(() -> {
            AutomationPage page = new AutomationPage(g);
            page.refresh();
            assertTrue(g.isMoleculeAutomationEnabled());
            assertTrue(page.moleculesCard().text().contains("liste de courses"));
            assertTrue(page.moleculesCard().text().contains("l'amas confié"));
            page.moleculesCard().click();
            assertFalse(g.isMoleculeAutomationEnabled());
            page.moleculesCard().click();
            assertTrue(g.isMoleculeAutomationEnabled());
            assertTrue(page.entrustedChips().containsKey("H2O"));
            page.entrustedChips().get("H2O").fire();
            assertFalse(g.isMoleculeAutomated("H2O"));
            assertEquals(0, g.automatedMolecules());
            return null;
        });
    }

    @Test void barreDesPlansSauvegardePuisRecharge() {
        FxTestSupport.fx(() -> {
            String[] saved=new String[5];java.util.Arrays.fill(saved,"");
            final String[] current={"arbre:dark_rich_fusion"};
            final boolean[] active={false};
            PlanBar.Plan fake=new PlanBar.Plan(){
                public String code(){return current[0];}
                public Game.PlanLoad load(String text){
                    if(text==null||text.isBlank())return new Game.PlanLoad(0,0);
                    current[0]=text;active[0]=true;return new Game.PlanLoad(1,0);
                }
                public void save(int slot){saved[slot]=current[0];}
                public Game.PlanLoad loadSaved(int slot){return load(saved[slot]);}
                public String saved(int slot){return saved[slot];}
                public String describe(String text){return "1 voie";}
                public void forget(){active[0]=false;}
                public boolean active(){return active[0];}
                public String progress(){return "1/1";}
            };
            PlanBar bar=new PlanBar(fake,"les voies","#99bbff");
            assertEquals(5,bar.slots().getItems().size());
            bar.press(0);
            assertEquals("arbre:dark_rich_fusion",saved[0]);
            current[0]="arbre:autre";
            bar.press(1);
            assertEquals("arbre:dark_rich_fusion",current[0]);
            bar.press(4);
            assertFalse(active[0]);
            return null;
        });
    }
}

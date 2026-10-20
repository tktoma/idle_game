import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.GameState;
import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.save.SaveCodec;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Garde les bots cohérents avec le jeu : s'ils ne compilent plus, ce test ne compile pas ; s'ils
 * plantent sur le moteur actuel, ou si une sauvegarde de référence ne se relit plus, il échoue.
 * Il ne mesure rien : les campagnes se lancent avec run.py.
 */
class BotsTest {

    private static List<Path> saves(String folder) throws Exception {
        try (Stream<Path> files = Files.list(Path.of("sauvegardes", folder))) {
            return files.filter(path -> path.toString().endsWith(".save")).sorted().toList();
        }
    }

    private static Game load(Path save) throws Exception {
        GameState state = SaveCodec.read(Files.readString(save)).state();
        return new Game(state, Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(42));
    }

    @Test
    void persistanceEstAcheteeAvantLesAutresDepenses() {
        Game g = new Game();
        g.start();
        g.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        g.state().setAtoms(idle.core.math.BigNum.of(25));
        assertTrue(FirstUniversePolicy.needsPersistence(g));
        assertEquals(1, FirstUniversePolicy.atoms(g, 100));
        assertTrue(g.keepsUpgradesOnFusion());
        assertEquals(1, g.levelOf("atom_keep"));
    }

    @Test
    void reserveDePersistanceRetientLesAtomesJusquaLAchat() {
        Game g = new Game();
        g.start();
        g.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        g.state().setAtoms(idle.core.math.BigNum.of(20));
        RobotCosmosPlanner bot = new RobotCosmosPlanner(g, 20);
        bot.prioritizePersistence();
        assertEquals(idle.core.math.BigNum.of(25), g.synthesisReserve());
        assertEquals(0, FirstUniversePolicy.atoms(g, 40));
        assertEquals(idle.core.math.BigNum.of(20), g.state().atoms());
        g.state().setAtoms(idle.core.math.BigNum.of(25));
        bot.prioritizePersistence();
        assertTrue(g.keepsUpgradesOnFusion());
        assertEquals(idle.core.math.BigNum.ZERO, g.synthesisReserve());
    }

    @Test
    void conditionDesAstresBloqueLaPreparationDuBigBang() {
        Game g = new Game();
        g.start();
        g.state().setBigBangs(5);
        g.state().setDarkMatter(idle.core.math.BigNum.of(500));
        assertEquals(18, g.bigBangSkyRequirement());
        assertFalse(FirstUniversePolicy.skyReady(g));
        assertFalse(FirstUniversePolicy.finalBangPreparation(g));
        assertFalse(RobotCosmosPlanner.skyReadyForNextBigBang(g));
    }

    @Test
    void leBotNeRelancePasAbsencePendantLaFormationDesAstres() {
        Game g = new Game();
        g.start();
        g.state().setBigBangs(5);
        g.state().setExplosions(25);
        RobotCosmosPlanner bot = new RobotCosmosPlanner(g, 20);
        bot.challenges();
        assertFalse(g.inChallenge());
    }

    @Test
    void liensPrioritairesSurLesBoutiquesRepetables() {
        Game g = new Game();
        g.start();
        g.state().setExplosions(1);
        g.state().setDarkLevel("dark_flow", 1);
        g.state().setDarkMatter(idle.core.math.BigNum.of(30));
        FirstUniversePolicy.dark(g, 1);
        assertEquals(1, g.darkLevelOf("link_flow"));
        assertEquals(0, g.darkLevelOf("dark_shop_atoms"));
    }

    @Test
    void boutiqueNoireFonctionneSansSeuilArbitraireDeSeptCents() {
        Game g = new Game();
        g.start();
        g.state().setExplosions(1);
        g.state().setDarkMatter(idle.core.math.BigNum.of(10));
        FirstUniversePolicy.dark(g, 1);
        assertEquals(1, g.darkLevelOf("dark_shop_atoms"));
    }

    @Test
    void seulLeBotExpertAttendLeSuccesPatience() {
        Game g = new Game();
        g.start();
        AdvancedLab expert = new AdvancedLab(g, 20, "EXPERT_COMPLET", 42);
        AdvancedLab autre = new AdvancedLab(g, 20, "DEPENDANCES", 42);
        assertTrue(expert.waitForPatientFusion());
        assertFalse(autre.waitForPatientFusion());
        g.state().setTimeSinceFusion(idle.core.achievements.Achievements.PATIENT_SECONDS);
        assertFalse(expert.waitForPatientFusion());
    }

    @Test
    void toutesLesPolitiquesV2SontDisponiblesDansLePremierUnivers() {
        Game g = new Game();
        g.start();
        for (String mode : AdvancedLab.MODES) {
            AdvancedLab bot = new AdvancedLab(g, 20, mode, 42);
            assertEquals(mode, bot.mode);
        }
    }

    @Test
    void toutesLesVariantesJouentLePremierUniversSansPlanter(@TempDir Path out) throws Exception {
        for (String mode : AdvancedLab.MODES) {
            Game g = load(Path.of("sauvegardes", "reference", "checkpoint_before_eighth.save"));
            long before = g.state().timePlayed() == 0 ? 0 : (long) g.state().timePlayed();
            Path folder = out.resolve(mode);
            AdvancedLab bot = new AdvancedLab(g, 20, mode, 42);
            bot.runLab(0.05, folder);
            assertTrue(g.state().timePlayed() > before, mode + " : le temps du moteur doit avancer");
            assertTrue(bot.acts > 0, mode + " : le bot doit agir");
            assertTrue(Files.exists(folder.resolve("timeline.csv")), mode);
            assertTrue(Files.exists(folder.resolve("result.csv")), mode);
            assertTrue(g.bigBangs() >= 0, mode);
        }
    }

    @Test
    void laListeEstToujoursActiveDansLesModesQuiLaConsultent() {
        Game g = new Game();
        g.start();
        RobotCosmosPlanner bot = new RobotCosmosPlanner(g, 20);
        bot.cosmos();
        assertTrue(g.isMoleculeAutomationEnabled(),
                "Les listes de recettes ne doivent jamais désactiver l'automatisation globale");
    }

    @Test
    void lesDefisDeBigBangNeDemarrentPasAvantLesPrerequis() {
        Game g = new Game();
        g.start();
        g.state().setBigBangs(5);
        RobotCosmosPlanner bot = new RobotCosmosPlanner(g, 20);
        assertFalse(bot.bangChallenges());
        assertEquals(null, g.activeBangChallenge());
    }

    @Test
    void lesSondesNePeuventPasEtreUtiliseesAvantLeurDeblocage() {
        Game g = new Game();
        g.start();
        RobotCosmosPlanner bot = new RobotCosmosPlanner(g, 20);
        assertFalse(g.isProbesUnlocked());
        bot.probes();
        assertEquals(0, g.state().probeLaunches());
    }

    @Test
    void expertPeutDemarrerDepuisUnePartieNeuve(@TempDir Path out) throws Exception {
        Game g = new Game();
        g.start();
        AdvancedLab bot = new AdvancedLab(g, 20, "EXPERT_COMPLET", 42);
        bot.runLab(0.1, out);
        assertTrue(g.state().started());
        assertTrue(g.state().timePlayed() >= 360);
        assertTrue(Files.exists(out.resolve("timeline.csv")));
        assertTrue(bot.acts > 0);
    }

    @Test
    void expertTermineLePremierUniversDepuisLeSeptiemeBigBang(@TempDir Path out) throws Exception {
        Game g = load(Path.of("sauvegardes", "reference", "checkpoint_before_eighth.save"));
        AdvancedLab expert = new AdvancedLab(g, 20, "EXPERT_COMPLET", 42);
        expert.runLab(25, out);
        System.out.printf("EXPERT_CHECK,bb=%d,cosmos=%d,conditions=%d/%d,free=%s,space=%s,goal=%s%n",
                g.bigBangs(), g.cosmosFormed(),
                g.cosmosConditionsMet(idle.core.sky.Cosmos.CLUSTER),
                g.cosmosConditions(idle.core.sky.Cosmos.CLUSTER),
                g.freeSpace(), g.state().space(), g.shoppingList().target());
        for(var state : idle.core.molecules.Molecule.State.values()){
            System.out.printf("EXPERT_STATE,%s,gathered=%d,sorts=%d,needed=%s,sortsNeeded=%s%n",
                state, g.gatheredInState(state), g.sortsInState(state),
                idle.core.sky.Cosmos.CLUSTER.matter().getOrDefault(state,0),
                idle.core.sky.Cosmos.CLUSTER.sorts().getOrDefault(state,0));
            var target = FirstUniversePolicy.matterTarget(g, state);
            if (target != null) {
                System.out.printf("EXPERT_TARGET,%s,id=%s,count=%d,draw=%d,canCreate=%s,auto=%s,enabled=%s,interval=%s,recipe=%s%n",
                        state,target.id(),g.moleculeCount(target.id()),g.moleculesPerCreation(target.id()),
                        g.canCreateMolecule(target.id()),g.isMoleculeAutomated(target.id()),
                        g.isAutoCreatingMolecules(),g.moleculeAutomationInterval(),target.recipe());
                for (var atom : target.recipe().entrySet())
                    System.out.printf("EXPERT_ATOM,%s,z=%d,stock=%d,need=%d%n",
                            state,atom.getKey(),g.elementCount(atom.getKey()),atom.getValue());
            }
        }
        assertTrue(g.hasCosmos(idle.core.sky.Cosmos.UNIVERSE),
                "Le bot expert doit terminer le premier univers depuis un checkpoint de progression");
        assertTrue(Files.exists(out.resolve("winner.save")));
    }

    @Test
    void unAmasNeChangePasAuPremierManqueTemporaireDElements() throws Exception {
        Game g = load(Path.of("sauvegardes", "reference", "checkpoint_before_eighth.save"));
        idle.core.molecules.Molecule target = FirstUniversePolicy.matterTarget(
                g, idle.core.molecules.Molecule.State.CRYSTAL);
        if(target == null) return; // Aucun amas de cristal ouvert dans ce checkpoint.
        for(Integer z : target.recipe().keySet()) g.state().setElementCount(z, 1);
        idle.core.molecules.Molecule again = FirstUniversePolicy.matterTarget(
                g, idle.core.molecules.Molecule.State.CRYSTAL);
        assertEquals(target.id(), again.id(),
                "Un manque de quelques atomes ne doit pas réinitialiser l'accrétion de l'amas");
    }

    @Test
    void toutesLesSauvegardesDesBotsSeRelisent() throws Exception {
        int count = 0;
        for (String folder : List.of("reference", "profils_bloques", "victoires_corrigees", "victoires_historiques")) {
            for (Path save : saves(folder)) {
                Game game = load(save);
                assertTrue(game.state().started(), save + " : partie démarrée");
                count++;
            }
        }
        assertEquals(32, count, "6 de référence, 3 bloquées, 2 corrigées, 21 victoires historiques");
    }

    @Test
    void lesVictoiresSontToujoursDesVictoires() throws Exception {
        VerifyVictory.main(new String[] {"sauvegardes/reference/checkpoint_universe_5s.save"});
        for (Path save : saves("victoires_historiques")) VerifyVictory.main(new String[] {save.toString()});
        for (Path save : saves("victoires_corrigees")) VerifyVictory.main(new String[] {save.toString()});
    }

    @Test
    void leBotV2TourneSurLeMoteurActuel(@TempDir Path out) throws Exception {
        AdvancedLab.main(new String[] {"DEPENDANCES", "sauvegardes/reference/checkpoint_before_eighth.save", "0.05", "20", "42", out.toString()});
        assertTrue(Files.exists(out.resolve("result.csv")));
        assertFalse(Files.readString(out.resolve("timeline.csv")).isBlank());
    }

    @Test
    void lesProfilsTournentSurLeMoteurActuel(@TempDir Path out) throws Exception {
        IdleProfiles24.main(new String[] {"19", "sauvegardes/reference/checkpoint_before_eighth.save", "0.05", "20", out.toString(), "42"});
        FreshPhaseProfiles.main(new String[] {"1", "0.05", "20", out.resolve("debut").toString(), "42"});
        try (Stream<Path> files = Files.list(out)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().endsWith(".csv")));
        }
    }
}

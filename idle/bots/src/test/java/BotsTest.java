import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.bigbang.BangChallenge;
import idle.core.math.BigNum;
import idle.core.save.SaveCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        return Bots.load(save, 42);
    }

    /** Partie neuve du profil 1 (graine 201) au 5e Big Bang : les dix défis et les quarante succès sont là. */
    static final Path FIFTH_BANG = Path.of("sauvegardes", "reference", "checkpoint_fresh_fifth_bang.save");

    @Test
    void persistanceAttendLesAutomatismesPrioritaires() {
        Game g = new Game();
        g.start();
        g.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        g.state().setAtoms(idle.core.math.BigNum.of(25));
        assertTrue(FirstUniversePolicy.needsPersistence(g));
        assertFalse(FirstUniversePolicy.readyForPersistence(g),
                "Persistance ne doit pas retarder les automatismes de debut");
        g.state().setAtoms(idle.core.math.BigNum.of(200));
        assertEquals(4, FirstUniversePolicy.bootstrapAutomations(g));
        assertTrue(FirstUniversePolicy.readyForPersistence(g));
        g.state().setAtoms(idle.core.math.BigNum.of(25));
        assertEquals(1, FirstUniversePolicy.atoms(g, 1));
        assertTrue(g.keepsUpgradesOnFusion());
        assertEquals(1, g.levelOf("atom_keep"));
    }

    @Test
    void reserveDePersistanceRetientLesAtomesJusquaLAchat() {
        Game g = new Game();
        g.start();
        g.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        g.state().setAtoms(idle.core.math.BigNum.of(200));
        assertEquals(4, FirstUniversePolicy.bootstrapAutomations(g));
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
    void laSyntheseCommenceDesLOuvertureDuTableauSansAttendrePersistance() {
        // Version 0.133 : une graine fixe. Avec le hasard de new Game(), le résultat dépendait des éléments tirés par
        // les deux synthèses : le test échouait environ une fois sur trente.
        Game g = new Game(42L);
        g.start();
        g.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        // Ouverture du §14.1 faite jusqu'aux automatismes ; Persistance (25) n'est pas encore achetée.
        g.state().setLevel("atom_double", 2);
        g.state().setLevel("atom_patience", 1);
        g.state().setLevel("atom_mass", 1);
        g.state().setLevel("atom_boost", 1);
        g.state().setLevel("atom_discount", 1);
        g.state().addAutomation("auto_generator");
        g.state().addAutomation("auto_fusion");
        g.state().setAutomationSpeedLevel("auto_generator", 2);
        assertEquals("atom_keep", FirstUniversePolicy.nextStep(g).id());
        g.state().setAtoms(idle.core.math.BigNum.of(31));
        // 31 atomes : 25 gardés pour Persistance, la synthèse à 2 et à 4 passe avec le reste.
        assertEquals(2, FirstUniversePolicy.synthesize(g, 20));
        assertEquals(idle.core.math.BigNum.of(25), g.state().atoms());
        assertEquals(1, FirstUniversePolicy.atoms(g, 5));
        assertTrue(g.keepsUpgradesOnFusion());
    }

    @Test
    void laSurchargeSePeseContreUneSynthese() {
        Game g = new Game();
        g.start();
        g.state().setTotalAtoms(Game.UNLOCK_TOTAL_ATOMS);
        for (FirstUniversePolicy.Step step : FirstUniversePolicy.OPENING) {
            switch (step.kind()) {
                case UPGRADE -> g.state().setLevel(step.id(), Math.max(step.level(), g.levelOf(step.id())));
                case AUTOMATION -> g.state().addAutomation(step.id());
                case CADENCE -> g.state().setAutomationSpeedLevel(step.id(), step.level());
            }
        }
        assertTrue(FirstUniversePolicy.openingDone(g));
        g.state().setLevel("atom_overload", FirstUniversePolicy.OVERLOAD_UNDER_CAP);
        assertFalse(FirstUniversePolicy.worthAgainstSynthesis(g, "atom_overload"),
                "Sous le plafond de 118, 13 niveaux de Surcharge au plus");
    }

    @Test
    void leTroisiemeActeNExplosePlusQuandLesMoleculesAttendentDesElements() throws Exception {
        // Partie neuve du profil 1 (graine 201) au 5e Big Bang : le prochain astre attend le Bitume.
        Game g = load(Path.of("sauvegardes", "reference", "checkpoint_fresh_fifth_bang.save"));
        var list = g.shoppingList();
        boolean needs = list.items().stream().anyMatch(i -> i.missing() > 0 || i.gather())
                || list.matter().values().stream().anyMatch(v -> v > 0)
                || list.sorts().values().stream().anyMatch(v -> v > 0);
        assertTrue(needs, "La sauvegarde de référence vise un astre qui demande des molécules");
        g.state().setDarkMatter(idle.core.math.BigNum.of(1000));
        assertTrue(FirstUniversePolicy.holdExplosions(g));
        g.state().setDarkMatter(idle.core.math.BigNum.of(10));
        assertFalse(FirstUniversePolicy.holdExplosions(g),
                "Sans les 500 matières noires du Big Bang, les explosions restent nécessaires");
        g.state().setDarkMatter(idle.core.math.BigNum.of(1000));
        g.state().setBigBangs(0);
        assertFalse(FirstUniversePolicy.holdExplosions(g), "Avant le premier Big Bang, on explose dès les 118 éléments");
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
    void expertPeutDemarrerDepuisUnePartieNeuve(@TempDir Path out) throws Exception {
        Game g = new Game();
        g.start();
        AdvancedLab bot = new AdvancedLab(g, 20, "EXPERT_COMPLET", 42);
        bot.runLab(0.1, out);
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
                () -> "Le bot expert doit terminer le premier univers : bb="+g.bigBangs()
                +", cosmos="+g.cosmosFormed()+", mature="+g.bodiesMature()
                +", cible="+g.shoppingList().target()+", libre="+g.freeSpace()
                +", espace="+g.state().space()+", auto="+g.isAutoCreatingMolecules()
                +", synthese="+g.isAutomationEnabled("auto_synthesis")
                +", ingredients="+g.shoppingList().items()
                +", matiere="+g.shoppingList().matter());
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
    void laMatiereLaPlusEnRetardProportionnellementPasseEnPremier() {
        var absent = java.util.Map.of(
                idle.core.molecules.Molecule.State.GAS, 5_000_000,
                idle.core.molecules.Molecule.State.METAL, 3_000_000);
        var cible = java.util.Map.of(
                idle.core.molecules.Molecule.State.GAS, 400_000_000,
                idle.core.molecules.Molecule.State.METAL, 4_000_000);
        assertEquals(idle.core.molecules.Molecule.State.METAL,
                FirstUniversePolicy.focusMatterState(absent,cible));
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
        assertEquals(35, count, "9 de référence (dont les univers 1 et 2 formés, versions 0.130 et 0.133), 3 bloquées, 2 corrigées, 21 victoires historiques");
    }

    @Test
    void lesVictoiresSontToujoursDesVictoires() throws Exception {
        VerifyVictory.main(new String[] {"sauvegardes/reference/checkpoint_universe_5s.save"});
        for (Path save : saves("victoires_historiques")) VerifyVictory.main(new String[] {save.toString()});
        for (Path save : saves("victoires_corrigees")) VerifyVictory.main(new String[] {save.toString()});
    }

    @Test
    void uneVictoireEcriteParLeMoteurActuelEstUneVictoire(@TempDir Path out) throws Exception {
        // Version 0.126 : l'univers formé remet tout à zéro ; la victoire reste celle du jeu, l'univers formé.
        Game won = Bots.fresh(42);
        won.testFormUniverse();
        assertEquals(0, won.bigBangs(), "la remise à zéro de l'univers a eu lieu");
        Path winner = out.resolve("winner.save");
        Files.writeString(winner, SaveCodec.write(won.state(), 0));
        VerifyVictory.main(new String[] {winner.toString()});
        Game lost = Bots.fresh(42);
        Path loser = out.resolve("loser.save");
        Files.writeString(loser, SaveCodec.write(lost.state(), 0));
        org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class, () -> VerifyVictory.main(new String[] {loser.toString()}));
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

    /** Le texte écrit sur la sortie standard pendant cette action. */
    private static String stdout(Runnable action) {
        java.io.PrintStream before = System.out;
        java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(captured, true, java.nio.charset.StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(before);
        }
        return captured.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void leBilanDUnePartieNeuveCompteLAbsenceEnHeures(@TempDir Path out) {
        // Correctif 0.141 (B3) : offlineCredited s'imprimait en secondes à côté de wall/game/online en heures.
        String log = stdout(() -> {
            try {
                FreshPhaseProfiles.main(new String[] {"19", "1", "20", out.toString(), "42"});
            } catch (Exception failure) {
                throw new IllegalStateException(failure);
            }
        });
        String fresh = log.lines().filter(line -> line.startsWith("FRESH,")).findFirst().orElseThrow();
        double wall = Double.parseDouble(fresh.replaceAll(".*,wall=([0-9.]+),.*", "$1"));
        double online = Double.parseDouble(fresh.replaceAll(".*,online=([0-9.]+),.*", "$1"));
        double credited = Double.parseDouble(fresh.replaceAll(".*,offlineCredited=([0-9.]+),.*", "$1"));
        assertEquals(1, wall, 1e-6);
        assertTrue(online < 0.1, "cinq minutes par heure en ligne : " + online);
        assertTrue(credited > 0 && credited <= wall - online + 1e-6,
                "l'absence créditée se compte en heures, au taux hors ligne : " + credited);
    }

    @Test
    void lesSortiesCsvSEcriventSansVirguleDecimale() throws Exception {
        // Correctif 0.141 (B6) : tout printf de sortie passe par Locale.ROOT, sinon la locale française met une virgule
        // décimale dans les CSV lus par les scripts.
        try (Stream<Path> files = Files.list(Path.of("src", "main", "java"))) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source);
                assertFalse(java.util.regex.Pattern.compile("printf\\(\\s*\"").matcher(text).find(),
                        source + " : printf sans Locale.ROOT");
                assertFalse(java.util.regex.Pattern.compile("String\\.format\\(\\s*\"").matcher(text).find(),
                        source + " : String.format sans Locale.ROOT");
            }
        }
    }

    @Test
    void laBarreHauteDoubleLaMatiereNoireGardeePourLeBigBang() throws Exception {
        // Correctif 0.141 (B2) : pendant la barre haute, le moteur exige 1000 matières noires (et 1e14 atomes) ; les
        // bots gardaient les constantes (500, 1e12) et dépensaient la réserve du Big Bang.
        Game g = load(FIFTH_BANG);
        assertTrue(g.baseChallengesDone() && g.achievementCount() >= Game.BIG_BANG_ACHIEVEMENTS);
        g.state().setDarkMatter(BigNum.of(700));
        assertTrue(FirstUniversePolicy.holdExplosions(g), "700 matières noires : le Big Bang ordinaire est assuré");
        assertTrue(g.startBangChallenge(BangChallenge.RAISED));
        assertEquals(BigNum.of(1000), g.bigBangDarkMatter());
        assertFalse(FirstUniversePolicy.holdExplosions(g), "il en faut 1000 pendant la barre haute : on explose encore");
        g.state().setDarkMatter(BigNum.of(1050));
        FirstUniversePolicy.dark(g, 50);
        assertTrue(g.state().darkMatter().gte(BigNum.of(1000)), "l'expert garde les 1000 du défi : " + g.state().darkMatter());
        Game cheap = load(FIFTH_BANG);
        assertTrue(cheap.startBangChallenge(BangChallenge.RAISED));
        cheap.state().setDarkMatter(BigNum.of(1050));
        FirstUniversePolicy.cheapestDark(cheap, 50);
        assertTrue(cheap.state().darkMatter().gte(BigNum.of(1000)), "le moins cher aussi : " + cheap.state().darkMatter());
    }

    /** Tout est prêt pour un Big Bang sans ciel exigé (quatre Big Bangs), avec 1e13 atomes et la fusion automatique. */
    private static void ready(Game g) {
        g.state().setBigBangs(Game.BANG_CHALLENGES_BIG_BANGS - 1);
        g.state().setDarkMatter(BigNum.of(5000));
        g.state().setParticles(BigNum.of(1, 500_000));
        g.state().setAtoms(BigNum.of(1, 13));
        g.state().addAutomation("auto_fusion");
        g.setAutomationEnabled("auto_fusion", true);
    }

    @Test
    void laBarreHauteCentupleLesAtomesAvantDeCouperLaFusion() throws Exception {
        // Correctif 0.141 (B2) : la fusion et la synthèse se coupaient à 1e12 atomes quand la barre haute en demande 1e14.
        Game g = load(FIFTH_BANG);
        ready(g);
        assertTrue(FirstUniversePolicy.finalBangPreparation(g));
        RobotCosmosPlanner before = new RobotCosmosPlanner(g, 20);
        before.resourceLoop();   // 1e13 atomes dépassent les 1e12 du Big Bang ordinaire : la fusion attend (et le Big Bang part)
        assertTrue(before.fusionPausedByRobot);
        Game raised = load(FIFTH_BANG);
        assertTrue(raised.startBangChallenge(BangChallenge.RAISED));
        ready(raised);
        assertEquals(BigNum.of(1, 14), raised.bigBangAtoms());
        assertTrue(FirstUniversePolicy.finalBangPreparation(raised));
        RobotCosmosPlanner during = new RobotCosmosPlanner(raised, 20);
        during.resourceLoop();
        assertFalse(during.fusionPausedByRobot, "1e13 atomes ne suffisent pas à la barre haute : la fusion continue");
        assertTrue(raised.isAutomationEnabled("auto_fusion"));
        assertEquals(BangChallenge.RAISED, raised.activeBangChallenge(), "pas de Big Bang : le défi continue");
    }

    @Test
    void unDefiAbandonneSeRetenteApresUnBigBangOuUnDefiDeBigBang() {
        // Correctif 0.141 (B5) : l'attente d'un défi abandonné se comptait en explosions d'une partie remise à zéro par
        // le Big Bang (ou par un défi de Big Bang) : 55 explosions au lieu de 25.
        Game g = new Game(42L);
        g.start();
        g.state().setBigBangs(2);
        g.state().setExplosions(30);
        RobotCosmosPlanner bot = new RobotCosmosPlanner(g, 20);
        bot.failedChallenges.put("challenge_x", 55);
        bot.challenges();
        assertEquals(55, bot.failedChallenges.get("challenge_x"), "même partie : l'attente tient");
        g.state().setBigBangs(3);
        g.state().setExplosions(0);
        bot.challenges();
        assertTrue(bot.failedChallenges.isEmpty(), "un Big Bang de plus : les explosions repartent de zéro, l'attente aussi");
        bot.failedChallenges.put("challenge_x", 55);
        g.state().setExplosions(12);
        bot.challenges();
        g.state().setExplosions(0);   // un défi de Big Bang qui remet à zéro ne compte pas de Big Bang
        bot.challenges();
        assertTrue(bot.failedChallenges.isEmpty(), "les explosions retombent : l'attente aussi");
    }

    @Test
    void lesProprietesDesDefisDeBigBangSontValidees() {
        // Correctif 0.141 (B10) : une valeur inconnue de idle.defisBB jouait « gele » en silence.
        RobotCosmosPlanner bot = new RobotCosmosPlanner(new Game(42L), 20);
        System.setProperty("idle.defisBB", "nimporte");
        try {
            IllegalArgumentException refused = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> bot.bangChallengeOrder(true));
            assertTrue(refused.getMessage().contains("idle.defisBB=nimporte"), refused.getMessage());
            System.setProperty("idle.defisBB", "gele");
            assertEquals(BangChallenge.FROZEN, bot.bangChallengeOrder(true).get(0));
            assertFalse(bot.bangChallengeOrder(true).get(1) == BangChallenge.RAISED, "« gele » : la barre haute reste dans l'ordre du catalogue");
        } finally {
            System.clearProperty("idle.defisBB");
        }
        assertEquals(BangChallenge.RAISED, bot.bangChallengeOrder(true).get(1), "par défaut : « maturation »");
    }
}

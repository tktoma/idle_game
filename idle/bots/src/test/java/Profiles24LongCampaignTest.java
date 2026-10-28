import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.molecules.Molecule;
import idle.core.save.SaveCodec;
import idle.core.sky.Cosmos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Contrôle comparatif de 24 heures SIMULÉES (30 pour le profil 17 depuis la 0.126 ; premier univers seulement).
 *
 * Le profil 1 sert de témoin rapide ; 11, 12 et 17 sont les trois profils
 * historiquement bloqués. Chaque essai reprend exactement la graine utilisée
 * lors de la campagne des 24 profils de seize heures ({@code Profiles24CampaignTest}). Un profil lent ne doit
 * pas être déclaré gagnant simplement parce que le programme n'a pas planté.
 */
class Profiles24LongCampaignTest {
    // L'absence de paramètre exécute les quatre profils en local.
    private static final int FIRST = Integer.getInteger("idle.testProfilesStart", 1);
    private static final int LAST = Integer.getInteger("idle.testProfilesEnd", 24);

    private static Game checkpoint(long seed) throws Exception {
        return Bots.load(Path.of("sauvegardes/reference/checkpoint_before_eighth.save"), seed);
    }

    @Test
    void profilsRapideEtHistoriqueSurVingtQuatreHeures(@TempDir Path out) throws Exception {
        int[] ids = {1, 11, 12, 17};
        assertTrue(FIRST >= 1 && LAST <= 24 && FIRST <= LAST,
                "Segment de tests longs invalide : " + FIRST + "-" + LAST);
        int completed = 0;
        int checked = 0;
        for (int id : ids) {
            if (id < FIRST || id > LAST) continue;
            checked++;
            IdleProfiles24.Profile p = IdleProfiles24.PROFILES[id - 1];
            long seed = 300L + id;
            Game game = checkpoint(seed);
            int startBangs = game.bigBangs();
            IdleProfiles24 bot = new IdleProfiles24(game, p, seed, 60);
            Path dir = out.resolve(String.format("%02d_%s", id, p.name()));
            // Version 0.126 : depuis le rééquilibrage de la 0.125 (maturation ×3, rassemblements de 2 h), l'AFK (une
            // décision par heure) forme la galaxie vers 26 h depuis ce point de départ (amas 36 h, univers 42 h) : il a
            // 30 h. Les trois autres gardent 24 h. Le jalon, lui, ne baisse pas.
            double hours = id == 17 ? 30 : 24;
            bot.execute(hours, dir);
            boolean win = game.hasCosmos(Cosmos.UNIVERSE);
            // Le test long doit mesurer un vrai jalon, et pas seulement
            // l'absence de crash (comme les tests courts).
            Cosmos minimum = id == 1 ? Cosmos.CLUSTER : Cosmos.GALAXY;
            if (win) completed++;
            assertTrue(game.state().started(), "Le jeu doit rester demarre : " + p.name());
            assertTrue(bot.actions > 0, "Le bot doit prendre des decisions : " + p.name());
            assertTrue(win || game.bigBangs() >= startBangs,
                    "Aucun Big Bang ne doit etre perdu sans victoire : " + p.name());
            // Le nom du CSV porte l'étape (propriété idle.stage, « late » par défaut), comme le bot l'écrit.
            Path history = dir.resolve(String.format(Locale.ROOT, "%02d_%s_%s.csv", id, p.name(), System.getProperty("idle.stage", "late")));
            assertTrue(Files.exists(history), "Historique manquant : " + p.name());
            assertTrue(Files.size(history) > 100, "Historique tronque : " + p.name());
            Files.writeString(dir.resolve("end.save"), SaveCodec.write(game.state(), 0));
            System.out.printf(
                    "PROFIL24_LONG,%d,%s,heures=%.4f,enligne=%.4f,offline=%.4f,decisions=%d,bb=%d,astres=%d,cosmos=%d,victoire=%s,fusions=%d,explosions=%d,objectif=%s,space=%s,free=%s,automatisation=%s%n",
                    id, p.name(), bot.wallSeconds/3600, bot.onlineSeconds/3600,
                    bot.creditedOfflineSeconds/3600, bot.actions,
                    game.bigBangs(), game.bodiesMature(), game.cosmosFormed(), win,
                    game.state().stats().fusions(), game.state().explosions(),
                    game.shoppingList().target().replace(',', ';'),
                    game.state().space(), game.freeSpace(), game.isMoleculeAutomationEnabled());
            for (var shortage : game.shoppingList().matter().entrySet()) {
                Molecule.State state = shortage.getKey();
                System.out.printf(
                        "PROFIL24_LONG_MATTER,%d,%s,manque=%d,reuni=%d,sortes=%d%n",
                        id, state, shortage.getValue(), game.gatheredInState(state),
                        game.sortsInState(state));
                Molecule target = FirstUniversePolicy.matterTarget(game, state);
                if (target != null) {
                    System.out.printf(
                            "PROFIL24_LONG_TARGET,%d,%s,id=%s,amas=%d,parClic=%d,creable=%s,auto=%s,autoActive=%s,intervalle=%.3f%n",
                            id, state, target.id(), game.moleculeCount(target.id()),
                            game.moleculesPerCreation(target.id()),
                            game.canCreateMolecule(target.id()),
                            game.isMoleculeAutomated(target.id()),
                            game.isAutoCreatingMolecules(), game.moleculeAutomationInterval());
                    for (var ingredient : target.recipe().entrySet()) {
                        System.out.printf(
                                "PROFIL24_LONG_ELEMENT,%d,%s,numero=%d,stock=%d,cout=%d%n",
                                id, state, ingredient.getKey(),
                                game.elementCount(ingredient.getKey()), ingredient.getValue());
                    }
                }
            }
            // Les diagnostics complets precedents restent disponibles
            // meme si l'objectif minimal est manque : pas de faux succes.
            assertTrue(win || game.hasCosmos(minimum),
                    p.name() + " doit atteindre au moins " + minimum
                    + " apres " + (int) hours + " h ; objectif=" + game.shoppingList().target());
        }
        long expected = java.util.Arrays.stream(ids).filter(id -> id >= FIRST && id <= LAST).count();
        assertTrue(checked == expected, "Profils longs manquants du segment");
        System.out.printf("PROFIL24_LONG_BILAN,segment=%d-%d,profils=%d,victoires=%d%n",
                FIRST, LAST, checked, completed);
    }
}

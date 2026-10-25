import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.core.probes.ProbeTrip;
import org.junit.jupiter.api.Test;

/** Version 0.135 : chaque profil a ses propres choix dans les trois actes, dès le premier univers. */
class EarlyStyleTest {

    private static Game fresh() {
        Game game = new Game(42L);
        game.start();
        return game;
    }

    @Test
    void lExpertJoueCommeAvant() {
        assertEquals(IdleProfiles24.PROFILES.length, IdleProfiles24.EARLY_STYLES.length, "un style par profil");
        assertEquals(RobotCosmosPlanner.EarlyStyle.EXPERT, IdleProfiles24.EARLY_STYLES[0], "le Speedrunner est l'expert d'avant");
        assertEquals(RobotCosmosPlanner.EarlyStyle.EXPERT, new RobotCosmosPlanner(fresh(), 20).earlyStyle,
                "les bots sans profil jouent en experts");
    }

    @Test
    void leDebutantAcheteLeMoinsCherEtIgnoreLesDefisDeBigBang() {
        Game game = fresh();
        IdleProfiles24 beginner = new IdleProfiles24(game, IdleProfiles24.PROFILES[15], 216, 20);
        assertFalse(beginner.useBangChallenges());
        assertFalse(beginner.earlyStyle.opening());
        game.state().setAtoms(BigNum.of(1, 6));
        game.state().setTotalAtoms(BigNum.of(1, 6));
        assertTrue(FirstUniversePolicy.cheapestAtoms(game, 3) > 0, "le moins cher, sans attendre l'ordre du modèle");
        IdleProfiles24 hunter = new IdleProfiles24(fresh(), IdleProfiles24.PROFILES[4], 205, 20);
        assertTrue(hunter.useBangChallenges(), "le chasseur de succès tente les défis de Big Bang");
    }

    @Test
    void lesSondesPartentPourToutelAbsence() {
        // Le joueur du soir : 2 h en ligne sur 24 h. À la fin de sa session, ses sondes partent en expédition (8 h).
        IdleProfiles24 evening = new IdleProfiles24(fresh(), IdleProfiles24.PROFILES[20], 221, 20);
        evening.wallSeconds = 2 * 3600 - 20;
        assertTrue(evening.timeUntilBack() > 20 * 3600);
        assertEquals(ProbeTrip.NIGHT, evening.probeTrip());
        // En pleine session, ses décisions sont rapprochées : les voyages courts d'avant.
        evening.wallSeconds = 600;
        assertEquals(30, evening.timeUntilBack(), 1e-9);
        assertTrue(evening.probeTrip() == ProbeTrip.SHORT || evening.probeTrip() == ProbeTrip.HOUR);
        // L'AFK ne passe qu'une fois par heure : ses sondes voyagent une heure.
        IdleProfiles24 afk = new IdleProfiles24(fresh(), IdleProfiles24.PROFILES[16], 217, 20);
        assertEquals(ProbeTrip.HOUR, afk.probeTrip());
        // Le Speedrunner, toujours là, garde ses sauts courts.
        IdleProfiles24 speed = new IdleProfiles24(fresh(), IdleProfiles24.PROFILES[0], 201, 20);
        assertEquals(ProbeTrip.SHORT, speed.probeTrip());
    }

    @Test
    void leMoinsCherNeVidePasLaMatiereNoireDansLesNiveauxRepetables() {
        // Relecture 0.135 : sans plafond, les cases répétables de la boutique (1, 2, 4…) prenaient toute la matière noire.
        Game game = fresh();
        game.state().setExplosions(1);
        game.state().setDarkLevel("dark_shop_atoms", 1);
        game.state().setDarkLevel("dark_shop_cadence", 1);
        game.state().setDarkMatter(BigNum.of(400));
        assertTrue(game.canBuyDark("dark_shop_atoms"), "la case répétable est payable");
        FirstUniversePolicy.cheapestDark(game, 50);
        assertTrue(game.state().darkMatter().gte(BigNum.of(300)),
                "au plus 5 % de la matière noire par niveau répétable : " + game.state().darkMatter());
        assertTrue(game.darkLevelOf("dark_shop_atoms") > 1, "il en achète quand même quelques niveaux");
    }

    @Test
    void leMoinsCherAcheteDAbordLesAutomatismesEtGardeSesAtomesPourEux() {
        // Audit 0.135 : sans cette priorité, les petites améliorations prenaient chaque atome, et la fusion
        // automatique ne s'achetait qu'après une centaine d'heures (le distrait n'avait aucun Big Bang en 168 h).
        Game game = fresh();
        game.state().setTotalAtoms(BigNum.of(100));
        game.state().setAtoms(BigNum.of(10));
        assertTrue(game.isAutomationAvailable("auto_fusion"), "les automatismes sont ouverts");
        FirstUniversePolicy.cheapestAtoms(game, 50);
        assertTrue(game.ownsAutomation("auto_generator"), "l'automatisme payable (5 atomes) d'abord");
        assertFalse(game.ownsAutomation("auto_fusion"));
        for (var upgrade : game.upgrades(idle.core.Resource.ATOMS))
            assertEquals(0, game.levelOf(upgrade.id()), "les atomes restants sont gardés pour la fusion automatique : " + upgrade.id());
    }

    @Test
    void avantLeHuitiemeBigBangLesAstresPlusPetitsQuiManquentPassentDAbord() {
        // Audit 0.135 : la naine blanche demande une géante rouge ; sans elle, rien n'était visé et la liste restait
        // sur une nébuleuse verrouillée (le débutant bloqué à 25 astres dans l'univers 3).
        RobotCosmosPlanner planner = new RobotCosmosPlanner(fresh(), 20);
        String first = planner.firstToForm("white_dwarf", 0);
        assertTrue(first != null && !first.equals("white_dwarf"), "dans une partie neuve, d'abord ce qu'elle demande : " + first);
        for (String needed : planner.g.body(first).bodies())
            assertTrue(planner.g.hasBody(needed), "rien ne manque plus à " + first + " : " + needed);
    }
}

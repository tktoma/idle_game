package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievements;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.math.BigNum;
import idle.core.molecules.ShoppingList;
import idle.core.probes.Probe;
import idle.core.probes.ProbeDestination;
import idle.core.probes.ProbeReward;
import idle.core.probes.ProbeTrip;
import idle.core.save.SaveCodec;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Version 0.86 : l'onde d'urgence chimique, et les sondes qui servent pendant l'absence. */
class SondesAbsenceTest {

    /** Une partie au troisième Big Bang, le troisième acte ouvert, et quelques éléments découverts. */
    private static Game game() {
        Game game = new Game(new GameState(), Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT,
                Achievements.DEFAULT, new Random(42));
        game.start();
        game.state().setBigBangs(3);
        game.state().setSpace(BigNum.of(1, 12));
        for (SpaceUpgrade upgrade : game.spaceUpgrades()) {
            if (!(upgrade.effect() instanceof SpaceUpgrade.Boost)) game.state().addSpaceUpgrade(upgrade.id());
        }
        return game;
    }

    /** L'élément le plus demandé, calculé à la main d'après la liste de courses. */
    private static int expected(Game game) {
        Map<Integer, Long> demand = new java.util.TreeMap<>();
        for (ShoppingList.Item item : game.shoppingList().items()) {
            long missing = Math.max(item.missing(), item.gather() ? 1 : 0);
            for (Map.Entry<Integer, Integer> part : item.molecule().recipe().entrySet()) {
                if (game.elementCount(part.getKey()) > 0) demand.merge(part.getKey(), missing * part.getValue(), Long::sum);
            }
        }
        int best = 0;
        long most = 0;
        for (Map.Entry<Integer, Long> entry : demand.entrySet()) if (entry.getValue() > most) { best = entry.getKey(); most = entry.getValue(); }
        return best;
    }

    @Test
    void lOndeDUrgenceRapporteLElementLePlusDemandeEtJamaisUnInconnu() {
        Game game = game();
        assertTrue(game.isUrgentWaveUnlocked());
        assertEquals(0, game.urgentElement(), "rien de découvert : pas d'élément à rapporter");
        assertFalse(game.canLaunchUrgentWave());
        assertFalse(game.launchUrgentWave());
        // Découvrir les éléments de la première molécule de la liste.
        ShoppingList.Item first = game.shoppingList().items().get(0);
        for (int number : first.molecule().recipe().keySet()) game.state().setElementCount(number, 1);
        int element = game.urgentElement();
        assertEquals(expected(game), element);
        assertTrue(element > 0 && game.elementCount(element) > 0, "un élément découvert");
        assertEquals(1, game.probeRewardCount(ProbeTrip.URGENT), "un seul lot");
        assertTrue(game.launchUrgentWave());
        assertEquals(20 * 60, game.state().probes().get(0).total(), 1e-9);
        game.tick(20 * 60);
        List<ProbeReward> loot = game.probeRewardsOf(game.state().probes().get(0).id());
        assertEquals(1, loot.size());
        assertEquals(ProbeReward.Type.ELEMENTS, loot.get(0).type());
        assertEquals(String.valueOf(element), loot.get(0).target());
        int before = game.elementCount(element);
        game.claimAllProbeRewards(game.state().probes().get(0).id());
        assertEquals(before + game.probeElementsOf(loot.get(0)), game.elementCount(element));
        assertTrue(game.state().probes().isEmpty(), "la sonde rentre au hangar");
    }

    @Test
    void lOndeAttendLeTroisiemeBigBangEtSeRelit() {
        Game game = game();
        game.state().setBigBangs(2);
        assertFalse(game.isUrgentWaveUnlocked());
        game.state().setBigBangs(3);
        ShoppingList.Item first = game.shoppingList().items().get(0);
        for (int number : first.molecule().recipe().keySet()) game.state().setElementCount(number, 1);
        assertTrue(game.launchUrgentWave());
        GameState back;
        try {
            back = SaveCodec.read(SaveCodec.write(game.state(), 0)).state();
        } catch (Exception unreadable) {
            throw new AssertionError(unreadable);
        }
        assertEquals(ProbeTrip.URGENT, back.probes().get(0).trip());
    }

    @Test
    void enAbsenceUneSondeArriveeVideSonButinEtRepart() {
        Game game = game();
        game.state().setElementCount(1, 5);
        assertTrue(game.launchProbe(ProbeDestination.VOID, ProbeTrip.SHORT));
        long first = game.state().probes().get(0).id();
        // Joueur présent : le butin attend son clic.
        game.tick(ProbeTrip.SHORT.seconds() + 1);
        assertTrue(game.state().probes().get(0).landed());
        assertFalse(game.probeRewardsOf(first).isEmpty());
        // En absence : le butin s'ouvre, la sonde repart pour le même voyage.
        long samples = game.state().probeSamples();
        game.tickAway(1);
        assertEquals(1, game.takeAwayProbeTrips());
        Probe again = game.state().probes().get(0);
        assertTrue(!again.landed() && again.id() != first && again.trip() == ProbeTrip.SHORT && again.destination() == ProbeDestination.VOID);
        assertTrue(game.state().probeSamples() > samples, "le butin a été ouvert : ses échantillons sont là");
        assertEquals(0, game.takeAwayProbeTrips(), "le compte se vide");
        // Une absence entière : trois voyages de plus en une heure.
        Absence absence = new Absence(game, 2 * 3600);
        absence.advance(Double.MAX_VALUE);
        assertTrue(absence.report().probeTrips() >= 3, "voyages relancés : " + absence.report().probeTrips());
    }
}

package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.collider.ColliderUpgrade;
import idle.core.collider.Quark;
import idle.core.math.BigNum;
import idle.core.probes.Probe;
import idle.core.probes.ProbeDestination;
import idle.core.probes.ProbeReward;
import idle.core.probes.ProbeTrip;
import idle.core.probes.ProbeUpgrade;
import idle.core.save.SaveCodec;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Les sondes (version 0.52) : le départ, le retour, le butin réclamé d'un clic, les améliorations. */
class SondesTest {

    /** Une partie au troisième Big Bang, l'hydrogène et le fer découverts. */
    private static Game third() {
        Game game = new Game();
        game.start();
        game.state().setBigBangs(3);
        game.state().setElementCount(1, 5);
        game.state().setElementCount(26, 2);
        return game;
    }

    @Test
    void lesSondesSOuvrentAuTroisiemeBigBang() {
        Game game = new Game();
        game.start();
        game.state().setBigBangs(2);
        assertFalse(game.isProbesUnlocked());
        assertFalse(game.launchProbe(ProbeDestination.VOID, ProbeTrip.SHORT));
        game.state().setBigBangs(3);
        assertTrue(game.isProbesUnlocked());
        assertTrue(game.launchProbe(ProbeDestination.VOID, ProbeTrip.SHORT));
        assertFalse(game.launchProbe(ProbeDestination.VOID, ProbeTrip.SHORT), "une seule sonde en vol au début");
    }

    @Test
    void uneDestinationDeRayonAttendSonRayon() {
        Game game = third();
        assertTrue(game.isProbeDestinationOpen(ProbeDestination.VOID));
        assertTrue(game.isProbeDestinationOpen(ProbeDestination.GAS_CLOUD));
        assertFalse(game.isProbeDestinationOpen(ProbeDestination.SUPERNOVA));
        assertFalse(game.launchProbe(ProbeDestination.SUPERNOVA, ProbeTrip.SHORT));
    }

    @Test
    void auRetourLeButinAttendEtSeReclameUneRecompenseALaFois() {
        Game game = third();
        assertTrue(game.launchProbe(ProbeDestination.GAS_CLOUD, ProbeTrip.SHORT));
        game.tick(ProbeTrip.SHORT.seconds() - 1);
        assertTrue(game.state().probeRewards().isEmpty());
        game.tick(2);
        assertEquals(1, game.landedProbes().size(), "arrivée, la sonde attend qu'on ouvre son butin");
        assertFalse(game.launchProbe(ProbeDestination.VOID, ProbeTrip.SHORT), "elle garde sa place au hangar");
        List<ProbeReward> loot = List.copyOf(game.state().probeRewards());
        assertEquals(ProbeTrip.SHORT.rewards(), loot.size());
        assertEquals(ProbeReward.Type.ELEMENTS, loot.get(0).type(), "le nuage de gaz rapporte d'abord un élément");
        assertEquals(ProbeReward.Type.SAMPLES, loot.get(loot.size() - 1).type(), "et finit par des échantillons");
        assertEquals(1, game.takeReturnedProbes().size());
        ProbeReward first = game.nextProbeReward();
        int element = Integer.parseInt(first.target());
        int before = game.state().elementCount(element);
        assertEquals(first, game.claimProbeReward());
        assertEquals(before + game.probeElementsOf(first), game.state().elementCount(element));
        assertEquals(loot.size() - 1, game.state().probeRewards().size(), "la suivante prend sa place");
        game.claimProbeReward();
        assertTrue(game.state().probeSamples() > 0);
        assertNull(game.claimProbeReward());
        assertTrue(game.state().probes().isEmpty(), "tout ouvert : la sonde rentre au hangar");
    }

    @Test
    void lesElementsRapportesSontDejaDecouverts() {
        Game game = third();
        for (int element : game.probeElements(ProbeDestination.GAS_CLOUD)) assertTrue(game.state().elementCount(element) > 0);
        assertTrue(game.probeElements(ProbeDestination.VOID).isEmpty());
    }

    @Test
    void unElementQuiNEstPlusDecouvertDevientDeLEspace() {
        Game game = third();
        game.state().setProbeRewards(List.of(new ProbeReward(ProbeReward.Type.ELEMENTS, "79", 1, ProbeDestination.ASTEROIDS)));
        BigNum space = game.state().space();
        game.claimProbeReward();
        assertEquals(0, game.state().elementCount(79));
        assertTrue(game.state().space().gt(space));
    }

    @Test
    void unSursautDoubleLEspaceParSecondeUnMoment() {
        Game game = third();
        double steady = game.steadySpacePerSecond().toDouble();
        game.state().setProbeRewards(List.of(new ProbeReward(ProbeReward.Type.BOOST, "", 1, ProbeDestination.VOID)));
        game.claimProbeReward();
        assertEquals(Game.PROBE_BOOST_SECONDS, game.state().probeBoostLeft(), 1e-9);
        assertEquals(steady * Game.PROBE_BOOST_FACTOR, game.steadySpacePerSecond().toDouble(), steady * 1e-9);
        game.state().setProbeUpgradeLevel(ProbeUpgrade.BOOST, 2);
        assertEquals(Game.PROBE_BOOST_SECONDS * 2, game.probeBoostOf(new ProbeReward(ProbeReward.Type.BOOST, "", 1, ProbeDestination.VOID)), 1e-9);
        game.tick(Game.PROBE_BOOST_SECONDS + 1);
        assertEquals(steady, game.steadySpacePerSecond().toDouble(), steady * 1e-9);
    }

    @Test
    void unLongVoyageRapportePlusParHeure() {
        double previous = 0;
        for (ProbeTrip trip : ProbeTrip.values()) {
            double perHour = trip.value() * trip.rewards() / (trip.seconds() / 3600);
            assertTrue(perHour > previous, trip + " : " + perHour);
            previous = perHour;
        }
    }

    @Test
    void lesAmeliorationsSePaientEnEchantillons() {
        Game game = third();
        assertFalse(game.buyProbeUpgrade(ProbeUpgrade.HANGAR));
        game.state().setProbeSamples(1000);
        assertTrue(game.buyProbeUpgrade(ProbeUpgrade.HANGAR));
        assertEquals(1000 - ProbeUpgrade.HANGAR.cost(0), game.state().probeSamples());
        assertEquals(2, game.probeSlots());
        assertTrue(game.buyProbeUpgrade(ProbeUpgrade.ENGINE));
        assertEquals(ProbeTrip.HOUR.seconds() * 0.9, game.probeDuration(ProbeTrip.HOUR), 1e-9);
        assertTrue(game.buyProbeUpgrade(ProbeUpgrade.CARGO));
        assertEquals(ProbeTrip.SHORT.rewards() + 1, game.probeRewardCount(ProbeTrip.SHORT));
        assertTrue(game.buyProbeUpgrade(ProbeUpgrade.LOOT));
        assertEquals(1.25, game.probeValue(ProbeTrip.SHORT), 1e-9);
        game.state().setProbeUpgradeLevel(ProbeUpgrade.HANGAR, ProbeUpgrade.HANGAR.maxLevel());
        assertFalse(game.canBuyProbeUpgrade(ProbeUpgrade.HANGAR), "au maximum");
    }

    @Test
    void leCharmeAjouteDesQuarksAuxVoyagesDUneHeure() {
        Game game = third();
        game.state().setColliderUpgradeLevel(ColliderUpgrade.FLAVOR_CHARM, 1);
        assertEquals(ProbeTrip.SHORT.rewards(), game.probeRewardCount(ProbeTrip.SHORT));
        assertEquals(ProbeTrip.HOUR.rewards() + 1, game.probeRewardCount(ProbeTrip.HOUR));
        assertTrue(game.launchProbe(ProbeDestination.VOID, ProbeTrip.HOUR));
        game.tick(ProbeTrip.HOUR.seconds() + 1);
        ProbeReward quarks = game.state().probeRewards().stream().filter(r -> r.type() == ProbeReward.Type.QUARKS).findFirst().orElseThrow();
        game.state().setProbeRewards(List.of(quarks));
        game.claimProbeReward();
        assertTrue(game.state().quarks(Quark.CHARM) > 0);
    }

    @Test
    void lesSondesTraversentLeBigBangMaisPasLaRemiseAZero() throws Exception {
        Game game = third();
        game.launchProbe(ProbeDestination.VOID, ProbeTrip.NIGHT);
        game.state().setProbeSamples(5);
        game.state().setProbeUpgradeLevel(ProbeUpgrade.LOOT, 1);
        GameState copy = new GameState();
        SaveCodec.readInto(copy, SaveCodec.write(game.state(), 0));
        assertEquals(1, copy.probes().size());
        assertEquals(5, copy.probeSamples());
        game.state().reset();
        assertTrue(game.state().probes().isEmpty());
        assertEquals(0, game.state().probeSamples());
        assertEquals(0, game.state().probeUpgradeLevel(ProbeUpgrade.LOOT));
    }

    @Test
    void leButinDUneSondeSOuvreUneParUneOuToutDUnCoup() {
        Game game = third();
        game.state().setProbeUpgradeLevel(ProbeUpgrade.HANGAR, 1);
        assertTrue(game.launchProbe(ProbeDestination.VOID, ProbeTrip.SHORT));
        assertTrue(game.launchProbe(ProbeDestination.GAS_CLOUD, ProbeTrip.HOUR));
        long first = game.state().probes().get(0).id(), second = game.state().probes().get(1).id();
        assertTrue(first != second);
        game.tick(ProbeTrip.SHORT.seconds() / 2);
        assertEquals(0.5, game.state().probes().get(0).progress(), 1e-6, "à mi-chemin");
        game.tick(ProbeTrip.SHORT.seconds() / 2 + 1);
        assertEquals(1, game.landedProbes().size());
        assertEquals(ProbeTrip.SHORT.rewards(), game.probeRewardsOf(first).size());
        assertTrue(game.probeRewardsOf(second).isEmpty(), "la seconde vole encore");
        assertNotNull(game.claimProbeReward(first));
        assertEquals(ProbeTrip.SHORT.rewards() - 1, game.probeRewardsOf(first).size());
        game.tick(ProbeTrip.HOUR.seconds());
        assertEquals(2, game.landedProbes().size());
        List<ProbeReward> all = game.claimAllProbeRewards(second);
        assertEquals(ProbeTrip.HOUR.rewards(), all.size());
        assertEquals(1, game.landedProbes().size(), "la seconde rentre ; la première attend son dernier lot");
        game.claimAllProbeRewards(first);
        assertTrue(game.state().probes().isEmpty());
    }

    @Test
    void uneSondeSansNumeroRentreAussitot() {
        Game game = third();
        game.state().setProbes(List.of(new Probe(ProbeDestination.VOID, ProbeTrip.SHORT, 0, 900)));
        game.tick(1);
        assertTrue(game.state().probes().isEmpty());
    }
}

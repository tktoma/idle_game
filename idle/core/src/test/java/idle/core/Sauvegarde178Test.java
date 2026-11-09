package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.achievements.Achievements;
import idle.core.darkmatter.DarkUpgrades;
import idle.core.molecules.Molecule;
import idle.core.save.SaveCodec;
import idle.core.upgrades.Automations;
import idle.core.upgrades.Upgrades;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Une sauvegarde d'avant la version 0.178 se relit : les molécules, assemblages, améliorations d'espace et états de la
 * matière qui n'existent plus sont écartés au chargement, les réglages disparus (suivi de la liste, suivi de la matière,
 * réservation par recette) sont ignorés, et la partie tourne. Une sauvegarde neuve n'écrit plus ces clés.
 */
class Sauvegarde178Test {

    private static Game play(GameState state) {
        return new Game(state, Upgrades.DEFAULT, Automations.DEFAULT, DarkUpgrades.DEFAULT, Achievements.DEFAULT, new Random(1));
    }

    /** Une sauvegarde écrite à la main dans l'ancien format : un troisième acte entamé avec des molécules de la 0.177. */
    private static String oldSave() {
        return String.join("\n",
                "idle-save 1",
                "savedAt=1700000000000",
                "started=1",
                "bigBangs=3",
                "space=1000000000",
                "moleculeLog=C6H12O6:5,H2O:3,NiTi:4,NaCl:2",
                "substances=NiTi,H2O,Nd2Fe14B",
                "automatedMolecules=NiTi,H2O",
                "assemblies=peridot_gems,volcanic_rain",
                "spaceUpgrades=space_salt,space_acid,space_states,space_rare",
                "stateCaps=SOLID:0.3,METAL:0.2,GAS:0.5",
                "autoFollowList=0",
                "autoFollowMatter=1",
                "recipeReservation=1",
                "autoMolecules=1",
                "") + "\n";
    }

    @Test
    void ceQuiADisparuEstEcarteEtLeResteTient() throws Exception {
        GameState state = SaveCodec.read(oldSave()).state();
        // Relue telle quelle, la sauvegarde porte encore ce que le jeu ne connaît plus…
        assertEquals(5, state.moleculeCount("C6H12O6"));
        assertTrue(state.hasSubstance("NiTi"));
        assertTrue(state.hasAssembly("volcanic_rain"));
        assertTrue(state.ownsSpaceUpgrade("space_salt"));
        // … sauf les états : un nom d'état inconnu est ignoré dès la lecture.
        assertEquals(Map.of("GAS", 0.5), state.stateCaps());

        Game game = play(state);
        int dropped = game.restored();
        assertEquals(8, dropped, "le glucose, le nitinol (molécule, amas, automatisme), l'aimant, la pluie, le sel et les terres rares");
        GameState kept = game.state();
        // Les molécules : seules celles du catalogue restent, dans l'ordre de leur création.
        assertEquals(0, kept.moleculeCount("C6H12O6"));
        assertEquals(0, kept.moleculeCount("NiTi"));
        assertEquals(3, kept.moleculeCount("H2O"));
        assertEquals(2, kept.moleculeCount("NaCl"));
        assertEquals(List.of("H2O", "H2O", "H2O", "NaCl", "NaCl"), kept.moleculeLog());
        assertEquals(2, game.moleculeKindsCreated());
        // Les amas et les automatismes : l'eau seule.
        assertEquals(List.of("H2O"), kept.substances());
        assertTrue(kept.isMoleculeAutomated("H2O"));
        assertFalse(kept.isMoleculeAutomated("NiTi"));
        assertEquals(1, game.automatedMolecules());
        // Les assemblages : les péridots existent encore, la pluie volcanique non.
        assertEquals(List.of("peridot_gems"), kept.assemblies());
        assertEquals(1, game.assembliesFormed());
        // Les améliorations d'espace : les acides et les états, pas les sels ni les terres rares.
        assertTrue(kept.ownsSpaceUpgrade("space_acid") && kept.ownsSpaceUpgrade("space_states"));
        assertFalse(kept.ownsSpaceUpgrade("space_salt"));
        assertFalse(kept.ownsSpaceUpgrade("space_rare"));
        assertEquals(2, kept.spaceUpgradeCount());
        assertTrue(game.isMoleculeKindUnlocked(Molecule.Kind.ACID));
        assertFalse(game.isMoleculeKindUnlocked(Molecule.Kind.MINERAL));
        // Les plafonds d'état : le gaz seulement.
        assertEquals(Map.of("GAS", 0.5), kept.stateCaps());
        assertEquals(0.5, game.stateCap(Molecule.State.GAS), 1e-12);
        assertEquals(1, game.stateCap(Molecule.State.CRYSTAL), 0);
        // Les réglages disparus n'ont rien changé : la création automatique est en marche, et elle suit le prochain pas.
        assertTrue(kept.autoMolecules());
        assertTrue(game.isAutoCreatingMolecules());
        // Et le jeu tourne.
        game.tick(60);
        game.shoppingList();
        game.spacePerSecond();
        game.autoMatterTargets();
        game.autoSortTargets();
        assertEquals(3, game.bigBangs());
    }

    @Test
    void unEtatInconnuNEstPasGardeMemeDirectement() {
        GameState state = new GameState();
        state.setStateCap("SOLID", 0.3);
        state.setStateCap("METAL", 0.2);
        state.setStateCap("GAS", 0.5);
        state.setStateCap("LIQUID", 0.4);
        assertEquals(Map.of("GAS", 0.5, "LIQUID", 0.4), state.stateCaps());
    }

    @Test
    void uneSauvegardeNeuveNEcritPlusLesReglagesDisparus() throws Exception {
        Game game = GameTestSupport.realGame();
        GameTestSupport.bangs(game, 3);
        game.state().setSpace(idle.core.math.BigNum.of(1, 9));
        GameTestSupport.openSpace(game);
        game.state().setMoleculeCount("H2O", 3);
        game.state().addSubstance("H2O");
        game.state().setStateCap("GAS", 0.6);
        String text = SaveCodec.write(game.state(), 1_700_000_000_000L);
        for (String gone : List.of("autoFollowList", "autoFollowMatter", "recipeReservation")) {
            for (String line : text.split("\\R")) assertFalse(line.startsWith(gone + "="), "la sauvegarde écrit encore " + line);
        }
        assertTrue(text.lines().anyMatch(line -> line.equals("stateCaps=GAS:0.6")), "les plafonds d'état s'écrivent par le nom de l'état");
        assertTrue(text.lines().anyMatch(line -> line.startsWith("autoMolecules=")));
        // Relue, elle est la même.
        GameState back = SaveCodec.read(text).state();
        assertEquals(game.state().stateCaps(), back.stateCaps());
        assertEquals(game.state().substances(), back.substances());
        assertEquals(game.state().moleculeLog(), back.moleculeLog());
        Game reloaded = play(back);
        assertEquals(0, reloaded.restored(), "rien d'inconnu dans une sauvegarde neuve");
    }
}

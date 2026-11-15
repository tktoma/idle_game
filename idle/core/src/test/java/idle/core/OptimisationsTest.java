package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import idle.core.math.BigNum;
import idle.core.save.SaveCodec;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.collider.*;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.ElementCategory;
import idle.core.profile.Title;
import idle.core.universe.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Les recherches rendues directes et le code mis en commun donnent exactement ce que donnaient les
 * anciennes boucles. Chaque test refait l'ancienne façon à côté de la nouvelle, sur tout le catalogue.
 */
class OptimisationsTest {
    private static Game partie() {
        Game game = new Game(42L);
        game.start();
        return game;
    }

    // ------------------------------------------------------------------
    // Enums, l'utilitaire commun
    // ------------------------------------------------------------------

    @Test
    void unNomSeRelitEtUnNomInconnuDonneNull() {
        assertSame(ElementCategory.values()[0], Enums.parse(ElementCategory.class, ElementCategory.values()[0].name()));
        assertSame(ElementCategory.values()[0], Enums.parse(ElementCategory.class, "  " + ElementCategory.values()[0].name() + " "), "les blancs autour ne comptent pas");
        assertNull(Enums.parse(ElementCategory.class, null));
        assertNull(Enums.parse(ElementCategory.class, ""));
        assertNull(Enums.parse(ElementCategory.class, "   "));
        assertNull(Enums.parse(ElementCategory.class, "UNE_FAMILLE_D_UNE_AUTRE_VERSION"));
        ElementCategory fallback = ElementCategory.values()[1];
        assertSame(fallback, Enums.parse(ElementCategory.class, "INCONNU", fallback));
        assertSame(fallback, Enums.parse(ElementCategory.class, null, fallback));
    }

    @Test
    void uneListeDeNomsGardeLOrdreEtEcarteLesInconnus() {
        ElementCategory[] all = ElementCategory.values();
        String text = all[2].name() + ",INCONNU, " + all[0].name() + ",," + all[2].name();
        assertEquals(List.of(all[2], all[0], all[2]), Enums.parseAll(ElementCategory.class, text));
        assertTrue(Enums.parseAll(ElementCategory.class, "").isEmpty());
        assertTrue(Enums.parseAll(ElementCategory.class, null).isEmpty());
    }

    @Test
    void uneTableParIdentifiantRefuseLesDoublons() {
        Map<Integer, String> byLength = Enums.index(new String[] {"a", "bb", "ccc"}, String::length);
        assertEquals("bb", byLength.get(2));
        assertNull(byLength.get(4));
        assertThrows(IllegalStateException.class, () -> Enums.index(new String[] {"a", "b"}, String::length));
        assertThrows(UnsupportedOperationException.class, () -> byLength.put(5, "eeeee"), "la table ne se modifie pas");
    }

    // ------------------------------------------------------------------
    // Les recherches directes : mêmes réponses que les anciennes boucles
    // ------------------------------------------------------------------

    @Test
    void chaqueSkinSeRetrouveParSonIdentifiant() {
        for (Skin skin : Skin.values()) {
            Skin parcouru = null;
            for (Skin other : Skin.values()) if (other.id().equals(skin.id())) { parcouru = other; break; }
            assertSame(parcouru, Skin.byId(skin.id()), skin.id());
        }
        assertNull(Skin.byId(null));
        assertNull(Skin.byId("un_skin_d_une_autre_version"));
    }

    @Test
    void chaqueTitreSeRetrouveParSonNom() {
        for (Title title : Title.values()) assertSame(title, Title.named(title.name()));
        assertNull(Title.named(null));
        assertNull(Title.named("UN_TITRE_D_UNE_AUTRE_VERSION"));
    }

    @Test
    void chaqueSuccesDuCollisionneurSeRetrouveParSonGenerateurEtSaSorte() {
        int found = 0;
        for (ColliderGenerator generator : ColliderGenerator.values()) {
            for (ColliderAchievement.Kind kind : ColliderAchievement.Kind.values()) {
                ColliderAchievement byName = null;
                try {
                    byName = ColliderAchievement.valueOf(generator.name() + "_" + kind.name());
                } catch (IllegalArgumentException none) {
                    // pas de succès de cette sorte pour ce générateur
                }
                if (byName == null) {
                    assertThrows(IllegalArgumentException.class, () -> ColliderAchievement.of(generator, kind), generator + " " + kind);
                } else {
                    assertSame(byName, ColliderAchievement.of(generator, kind));
                    assertSame(generator, byName.generator());
                    assertSame(kind, byName.kind());
                    found++;
                }
            }
        }
        assertEquals(2 * ColliderGenerator.values().length, found, "un succès de production et un de prix par générateur");
    }

    @Test
    void lesVoiesDeChaqueBifurcationSontCellesDuCatalogueDansSonOrdre() {
        Game game = partie();
        Set<String> forks = new TreeSet<>();
        for (DarkUpgrade dark : game.darkUpgrades()) if (dark.fork() != null) forks.add(dark.fork());
        assertFalse(forks.isEmpty());
        for (String fork : forks) {
            List<DarkUpgrade> parcouru = new ArrayList<>();
            for (DarkUpgrade dark : game.darkUpgrades()) if (fork.equals(dark.fork())) parcouru.add(dark);
            assertEquals(parcouru, game.darkForkOptions(fork), fork);
            assertThrows(UnsupportedOperationException.class, () -> game.darkForkOptions(fork).clear(), "la liste rangée ne se modifie pas");
        }
        assertTrue(game.darkForkOptions("une_bifurcation_inconnue").isEmpty());
    }

    @Test
    void leCompteDesAutomatismesSuitLaCopieTriee() {
        Game game = partie();
        assertEquals(0, game.state().ownedAutomationCount());
        int n = 0;
        for (var automation : game.automations()) {
            game.state().addAutomation(automation.id());
            n++;
            assertEquals(game.state().ownedAutomations().size(), game.state().ownedAutomationCount());
            assertEquals(n, game.state().ownedAutomationCount());
            if (n == 3) break;
        }
    }

    @Test
    void laVersionDesSkinsBougeAChaqueChangement() {
        Game game = partie();
        Skin paid = null;
        for (Skin skin : Skin.values()) if (!skin.isFree()) { paid = skin; break; }
        int before = game.state().skinsVersion();
        game.state().unlockSkin(paid.id());
        int unlocked = game.state().skinsVersion();
        assertNotEquals(before, unlocked, "un déblocage");
        game.state().unlockSkin(paid.id());
        assertEquals(unlocked, game.state().skinsVersion(), "le même skin deux fois : rien ne change");
        game.state().lockSkin(paid.id());
        assertNotEquals(unlocked, game.state().skinsVersion(), "un reverrouillage");
    }

    @Test
    void leSkinChoisiResteCeluiDAvant() {
        Game game = partie();
        game.testAllSkins(true);
        for (SkinVisual visual : SkinVisual.values()) {
            Skin first = null;
            for (Skin skin : Skin.values()) if (skin.visual() == visual) { first = skin; break; }
            if (first == null) continue;
            assertTrue(game.chooseSkin(visual, first), visual.name());
            assertSame(first, game.chosenSkin(visual), visual.name());
        }
    }

    // ------------------------------------------------------------------
    // Une partie avancée se joue à l'identique
    // ------------------------------------------------------------------

    @Test
    void deuxPartiesIdentiquesRestentIdentiquesAvecLesPaliersEtLArbre() {
        // Les paliers relus sans liste et les bifurcations rangées une fois ne changent rien au calcul : deux parties de
        // même graine, menées pareil, donnent la même sauvegarde après une heure de jeu.
        Game a = partie();
        Game b = partie();
        for (Game game : List.of(a, b)) {
            game.state().setBigBangs(6);
            game.state().setParticles(BigNum.of(1, 40));
            game.buyAllWithParticles();
            for (int s = 0; s < 3600; s++) game.tick(1);
        }
        assertEquals(SaveCodec.write(a.state(), 0), SaveCodec.write(b.state(), 0));
    }
}

package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import idle.ui.GameAppBase.Tab;
import idle.ui.pages.Avatar;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Les réglages, l'ordre des onglets et les avatars se relisent par l'utilitaire commun
 * ({@link idle.core.Enums}). Un nom d'une autre version donne la valeur d'origine, comme avant ; un nom connu se relit.
 */
class ReglagesRelusTest {
    @Test
    void unReglageInconnuRevientACeluiDOrigine() {
        NotificationsEtSignesTest.MemoryPreferences store = new NotificationsEtSignesTest.MemoryPreferences();
        for (String key : List.of("notation", "theme", "scale", "saveDelay", "frameRate", "buyAmount", "detailKey")) {
            store.put(key, "UNE_VALEUR_D_UNE_AUTRE_VERSION");
        }
        Settings settings = Settings.load(store);
        Settings defaults = Settings.load(new NotificationsEtSignesTest.MemoryPreferences());
        assertSame(defaults.notation(), settings.notation());
        assertSame(defaults.theme(), settings.theme());
        assertSame(defaults.scale(), settings.scale());
        assertSame(defaults.saveDelay(), settings.saveDelay());
        assertSame(defaults.frameRate(), settings.frameRate());
        assertSame(defaults.buyAmount(), settings.buyAmount());
        assertSame(defaults.detailKey(), settings.detailKey());
    }

    @Test
    void unReglageConnuSeRelit() {
        NotificationsEtSignesTest.MemoryPreferences store = new NotificationsEtSignesTest.MemoryPreferences();
        Settings.Scale scale = Settings.Scale.values()[Settings.Scale.values().length - 1];
        Settings.FrameRate rate = Settings.FrameRate.values()[0];
        store.put("scale", scale.name());
        store.put("frameRate", rate.name());
        Settings settings = Settings.load(store);
        assertSame(scale, settings.scale());
        assertSame(rate, settings.frameRate());
    }

    /** Version 0.252 : le profil test UI change de thème et de notation sans rien écrire dans les réglages du joueur. */
    @Test
    void desReglagesOubliesNeSEcriventPlus() {
        NotificationsEtSignesTest.MemoryPreferences store = new NotificationsEtSignesTest.MemoryPreferences();
        Settings settings = Settings.load(store);
        settings.forget();
        settings.setTheme(Settings.Theme.LIGHT);
        settings.setNotation(Settings.Notation.values()[1]);
        assertEquals(Settings.Theme.LIGHT, settings.theme(), "le changement vaut pour la séance");
        Settings again = Settings.load(store);
        assertEquals(Settings.Theme.DARK, again.theme(), "mais rien n'est écrit");
        assertEquals(Settings.Notation.SCIENTIFIC, again.notation());
    }

    /** Version 0.247 : la vitesse du motif du miroir se garde ; une valeur illisible ou hors bornes revient au temps entier. */
    @Test
    void laVitesseDuMotifDuMiroirSeRelit() {
        NotificationsEtSignesTest.MemoryPreferences store = new NotificationsEtSignesTest.MemoryPreferences();
        assertEquals(0, Settings.load(store).mirrorPace(), "par défaut, le temps entier de la mémorisation");
        Settings settings = Settings.load(store);
        settings.setMirrorPace(0.6);
        assertEquals(0.6, Settings.load(store).mirrorPace(), 1e-12);
        store.put("mirrorPace", "pas un nombre");
        assertEquals(0, Settings.load(store).mirrorPace());
        store.put("mirrorPace", "9");
        assertEquals(1, Settings.load(store).mirrorPace(), "bornée");
    }

    @Test
    void lOrdreDesOngletsEcarteLesInconnusEtLesDoublons() {
        List<Tab> order = GameAppTabs.readTabOrder("STATS, INCONNU,,PARTICLES,STATS");
        assertEquals(Tab.STATS, order.get(0));
        // Les onglets que l'ordre ne cite pas se rangent après celui qui les précède d'habitude : les réglages et les skins
        // suivent les statistiques, avant les particules.
        assertEquals(List.of(Tab.STATS, Tab.SETTINGS, Tab.SKINS, Tab.PARTICLES), order.subList(0, 4));
        assertEquals(Tab.values().length, order.size(), "chaque onglet une fois");
        assertEquals(List.of(Tab.values()), GameAppTabs.readTabOrder(""), "rien d'écrit : l'ordre d'origine");
    }

    @Test
    void chaqueAvatarSeRetrouveParSonIdentifiant() {
        for (Avatar avatar : Avatar.values()) assertSame(avatar, Avatar.of(avatar.id()), avatar.name());
        assertNull(Avatar.of(null));
        assertNull(Avatar.of(""));
        assertNull(Avatar.of("un_avatar_d_une_autre_version"));
    }

    @Test
    void lesSonsDesAutomatismesSontCoupesAuDepartEtSeRelisent() {
        NotificationsEtSignesTest.MemoryPreferences store = new NotificationsEtSignesTest.MemoryPreferences();
        Settings settings = Settings.load(store);
        assertEquals(false, settings.automationSounds(), "coupés au départ : les automatismes agissent en silence");
        settings.setAutomationSounds(true);
        assertEquals(true, Settings.load(store).automationSounds(), "le choix se relit");
    }
}

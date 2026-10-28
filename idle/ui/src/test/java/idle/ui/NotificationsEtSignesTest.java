package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.AbstractPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Version 0.146 : les notifications, l'objectif et les signes d'état se règlent par catégorie. Sans affichage JavaFX ;
 * les réglages s'écrivent dans un stockage en mémoire, jamais dans les préférences de l'utilisateur.
 */
class NotificationsEtSignesTest {

    /** Des préférences en mémoire : ce que les réglages écrivent, ils le relisent, sans toucher au disque. */
    static final class MemoryPreferences extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, MemoryPreferences> children = new HashMap<>();

        MemoryPreferences() {
            super(null, "");
        }

        private MemoryPreferences(MemoryPreferences parent, String name) {
            super(parent, name);
        }

        @Override protected void putSpi(String key, String value) { values.put(key, value); }
        @Override protected String getSpi(String key) { return values.get(key); }
        @Override protected void removeSpi(String key) { values.remove(key); }
        @Override protected void removeNodeSpi() { }
        @Override protected String[] keysSpi() { return values.keySet().toArray(new String[0]); }
        @Override protected String[] childrenNamesSpi() { return children.keySet().toArray(new String[0]); }
        @Override protected AbstractPreferences childSpi(String name) { return children.computeIfAbsent(name, n -> new MemoryPreferences(this, n)); }
        @Override protected void syncSpi() { }
        @Override protected void flushSpi() { }
    }

    @AfterEach
    void showAllDots() {
        Readiness.setHidden(Set.of());
    }

    @Test
    void chaqueCategorieDeNotificationSeCoupeEtSeRelit() {
        MemoryPreferences store = new MemoryPreferences();
        Settings settings = Settings.load(store);
        for (Notice kind : Notice.values()) assertTrue(settings.notice(kind), "au départ, tout s'affiche : " + kind);
        settings.setNotice(Notice.AUTO_SKY, false);
        settings.setNotice(Notice.RECORDS, false);
        assertFalse(settings.notice(Notice.AUTO_SKY));
        assertTrue(settings.notice(Notice.ELEMENTS));
        settings.setNotifications(false);
        assertFalse(settings.notice(Notice.ELEMENTS), "le grand interrupteur coupe tout");
        assertTrue(settings.noticeChosen(Notice.ELEMENTS), "sans toucher au choix de chaque catégorie");
        Settings again = Settings.load(store);
        assertFalse(again.noticeChosen(Notice.AUTO_SKY));
        assertFalse(again.noticeChosen(Notice.RECORDS));
        assertTrue(again.noticeChosen(Notice.COMETS));
        assertFalse(again.notifications());
    }

    @Test
    void chaqueMorceauDeLObjectifSeCacheEtLeChronoGardeSonReglage() {
        MemoryPreferences store = new MemoryPreferences();
        Settings settings = Settings.load(store);
        settings.setGoalPart(Goals.Part.ESTIMATE, false);
        settings.setGoalPart(Goals.Part.CHRONO, false);
        assertFalse(settings.goalPart(Goals.Part.ESTIMATE));
        assertFalse(settings.chrono(), "le chrono est le réglage d'avant");
        assertTrue(settings.goalPart(Goals.Part.GOAL));
        settings.setShowGoal(false);
        assertFalse(settings.goalPart(Goals.Part.GOAL), "l'objectif caché cache tout");
        Settings again = Settings.load(store);
        assertFalse(again.goalPartChosen(Goals.Part.ESTIMATE));
        assertFalse(again.goalPartChosen(Goals.Part.CHRONO));
        assertTrue(again.goalPartChosen(Goals.Part.BIG_BANG));
    }

    @Test
    void laLigneDObjectifNeGardeQueSesMorceauxMontres() {
        Game game = new Game(1L);
        game.start();
        String all = Goals.line(game, part -> true);
        assertTrue(all.startsWith("Objectif : "), all);
        assertEquals("", Goals.line(game, part -> false), "rien de montré : une ligne vide");
        String bare = Goals.line(game, part -> part != Goals.Part.ESTIMATE);
        assertEquals("Objectif : " + Goals.next(game), bare, "sans le temps estimé, l'objectif seul");
    }

    @Test
    void chaqueSorteDePastilleSeCache() {
        MemoryPreferences store = new MemoryPreferences();
        Settings settings = Settings.load(store);
        assertEquals(Readiness.DOT, Readiness.dot(Readiness.Kind.COMET, true));
        settings.setDot(Readiness.Kind.COMET, false);
        assertEquals("", Readiness.dot(Readiness.Kind.COMET, true), "la comète cachée ne s'allume plus");
        assertEquals(Readiness.DOT, Readiness.dot(Readiness.Kind.TREE, true), "les autres, si");
        assertEquals("", Readiness.dot(Readiness.Kind.TREE, false), "rien de prêt, rien d'allumé");
        Readiness.setHidden(Set.of());
        Settings.load(store);
        assertEquals("", Readiness.dot(Readiness.Kind.COMET, true), "relue, la pastille reste cachée");
    }

    @Test
    void lesNouvellesPortentLeurCategorieEtSeResumentApresLeTri() {
        Game game = new Game(1L);
        game.start();
        Notifier notifier = new Notifier(game);
        assertTrue(notifier.poll().isEmpty(), "le premier relevé prend ses repères");
        game.testAllSkins(true);
        List<Notifier.News> news = notifier.poll();
        assertFalse(news.isEmpty());
        for (Notifier.News item : news) {
            assertEquals(Notice.COSMETICS, item.kind(), item.text());
            assertTrue(item.text().startsWith("Skin débloqué"), item.text());
        }
        assertTrue(news.size() > 4, "tous les skins, sans résumé : le tri vient d'abord");
        List<String> shown = Notifier.summarize(news.stream().map(Notifier.News::text).toList());
        assertEquals(4, shown.size());
        assertEquals("… et " + (news.size() - 3) + " autres nouvelles", shown.get(3));
        assertEquals(List.of("a", "b"), Notifier.summarize(List.of("a", "b")));
    }
}

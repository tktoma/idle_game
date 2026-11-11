package idle.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.math.BigNum;
import idle.core.universe.Skin;
import idle.ui.pages.SkinsPage;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Version 0.206 : les annonces des défis de matière noire (commencé, tableau assez lourd, abandonné, poids à chaque
 * univers formé) et la carte d'un skin qui attend son univers.
 */
class DefisEtSkinsEtalesTest {

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    private static Game game() {
        Game game = new Game();
        game.start();
        game.testFormUniverse();
        game.state().setDarkMatter(BigNum.of(1000));
        game.state().setExplosions(1);
        game.tick(1);
        return game;
    }

    @Test
    void unDefiCommenceAssezLourdPuisAbandonneSAnnonce() {
        Game game = game();
        Notifier notifier = new Notifier(game);
        assertTrue(notifier.poll().isEmpty(), "premier relevé : rien");
        assertTrue(game.startChallenge("challenge_heavy"));
        List<Notifier.News> news = notifier.poll();
        assertTrue(news.stream().anyMatch(each -> each.kind() == Notice.CHALLENGES && each.text().startsWith("Défi « ")
                && each.text().contains(" » commencé : ") && each.text().contains("le tableau pèse ×2 plus qu'au premier univers")
                && each.text().endsWith("L'explosion le réussit, l'abandon rend la partie.")), news.toString());
        assertTrue(notifier.poll().stream().noneMatch(each -> each.text().contains("commencé")), "une seule fois");
        // Le tableau plein pendant le défi : l'annonce dit que l'explosion le réussit.
        for (idle.core.elements.Element element : idle.core.elements.PeriodicTable.ELEMENTS) game.state().setElementCount(element.number(), 1);
        game.tick(0.1);
        assertTrue(game.canExplode());
        news = notifier.poll();
        assertTrue(news.stream().anyMatch(each -> each.kind() == Notice.CHALLENGES
                && each.text().equals("Défi « " + game.activeChallenge().name() + " » : le tableau est assez lourd, l'explosion le réussit")), news.toString());
        assertTrue(game.abandonChallenge());
        news = notifier.poll();
        assertTrue(news.stream().anyMatch(each -> each.kind() == Notice.CHALLENGES && each.text().endsWith("abandonné : la partie repart sans contrainte.")), news.toString());
        assertTrue(notifier.poll().stream().noneMatch(each -> each.text().contains("abandonné")), "une seule fois");
    }

    @Test
    void unUniversFormeAnnonceLePoidsDesDefis() {
        Game game = game();
        Notifier notifier = new Notifier(game);
        assertTrue(notifier.poll().isEmpty());
        assertTrue(game.testAddUniverse());
        game.tick(1);
        List<Notifier.News> news = notifier.poll();
        assertTrue(news.stream().anyMatch(each -> each.kind() == Notice.CHALLENGES
                && each.text().equals("Univers formé : les défis pèsent maintenant ×4 plus qu'au premier univers (×2 par univers formé).")), news.toString());
        assertEquals(2.0, Game.CHALLENGE_UNIVERSE_GROWTH, 1e-12);
    }

    @Test
    void laCarteDUnSkinDitLUniversQuElleAttend() {
        FxTestSupport.fx(() -> {
            Game game = game();
            game.state().setSkinsOpen(true);
            game.tick(1);
            SkinsPage page = new SkinsPage(game);
            page.refresh();
            Skin later = null;
            Skin now = null;
            for (Skin skin : Skin.values()) {
                if (skin.isFree() || game.hasSkin(skin)) continue;
                if (later == null && skin.minUniverses() > game.universes()) later = skin;
                if (now == null && skin.minUniverses() <= game.universes()) now = skin;
            }
            assertTrue(later != null && now != null);
            String waiting = page.card(later).text();
            assertTrue(waiting.endsWith(" — dès l'univers " + (later.minUniverses() + 1) + " | Univers " + (later.minUniverses() + 1)),
                    "le skin attend son univers : " + waiting);
            String locked = page.card(now).text();
            assertTrue(locked.endsWith(" | Verrouillé") && !locked.contains("dès l'univers"), "celui dont l'univers est venu attend sa condition : " + locked);
            assertTrue(page.summaryText().contains("une trentaine par univers formé"), page.summaryText());
            return null;
        });
    }
    @Test
    void noyauxSombresEstMiseEnAvantEtAnnoncee() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            Notifier notifier = new Notifier(game);
            assertTrue(notifier.poll().isEmpty());
            game.state().setExplosions(1);
            game.state().setDarkMatter(BigNum.of(10));
            game.tick(1);
            assertTrue(game.isDarkAvailable(Game.KEY_DARK_UPGRADE));
            List<Notifier.News> news = notifier.poll();
            assertTrue(news.stream().anyMatch(each -> each.kind() == Notice.EXPLOSION && each.text().startsWith("« Noyaux sombres » à portée")), news.toString());
            assertTrue(notifier.poll().stream().noneMatch(each -> each.text().contains("Noyaux sombres")), "une seule fois");
            idle.ui.darkmatter.DarkMatterPage page = new idle.ui.darkmatter.DarkMatterPage(game);
            page.treePane().refresh();
            idle.ui.common.Card card = page.treePane().card(Game.KEY_DARK_UPGRADE);
            assertTrue(card != null);
            String text = card.text();
            assertTrue(text.contains("★ Noyaux sombres") && text.contains("la case qui compte le plus"), text);
            assertTrue(card.hint().startsWith("Mesuré sur une partie entière"), card.hint());
            idle.ui.common.Card other = page.treePane().card("dark_shop_cadence");
            assertFalse(other.text().contains("★"), other.text());
            assertEquals("", other.hint());
            return null;
        });
    }

}

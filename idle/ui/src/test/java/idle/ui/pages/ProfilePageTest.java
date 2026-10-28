package idle.ui.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.Game;
import idle.core.collider.ColliderGenerator;
import idle.core.collider.ColliderUpgrade;
import idle.core.molecules.Molecule;
import idle.core.profile.Title;
import idle.ui.FxTestSupport;
import idle.ui.automation.AutomationPage;
import idle.ui.common.Card;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.event.ActionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Version 0.147 : l'onglet Profil sous Xvfb (nom, titres, galerie d'avatars, image importée, journal, albums), et les
 * automates du collisionneur dans l'onglet Automatisation. Le même scénario tourne sur bouchons dans Smoke80.
 */
class ProfilePageTest {

    @TempDir
    Path data;
    @TempDir
    Path sources;

    @BeforeAll
    static void setup() {
        FxTestSupport.start();
    }

    /** Une partie commencée, une seconde et demie de jeu : le premier titre est tombé. */
    private static Game started() {
        Game game = new Game();
        game.start();
        game.tick(1.5);
        return game;
    }

    private static Path png(Path dir, String name, int width, int height) throws Exception {
        Path file = dir.resolve(name);
        javax.imageio.ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB),
                name.endsWith(".jpg") ? "jpg" : "png", file.toFile());
        return file;
    }

    @Test
    void leNomSeValideEtSeNettoie() {
        FxTestSupport.fx(() -> {
            Game game = started();
            ProfilePage page = new ProfilePage(game, () -> data);
            page.refresh();
            assertEquals(List.of("Profil", "Journal"), page.openSubPages(), "avant le Big Bang : ni albums");
            assertTrue(page.nameField().getText().isEmpty());
            assertEquals(Game.DEFAULT_PLAYER_NAME, page.nameField().getPromptText());
            page.nameField().setText("   Zoé    l'Exploratrice     des   étoiles lointaines  ");
            page.nameField().getOnAction().handle(new ActionEvent());
            String clean = game.state().playerName();
            assertTrue(clean.startsWith("Zoé l'Exploratrice des") && clean.length() <= 24, clean);
            assertEquals(clean, page.nameField().getText(), "le champ montre le nom nettoyé par le moteur");
            assertEquals(clean, game.playerName());
            return null;
        });
    }

    @Test
    void lesTitresSePortentDunClic() {
        FxTestSupport.fx(() -> {
            Game game = started();
            game.state().earnTitle(Title.SHADOW_KEEPER.name(), 600);
            game.state().earnTitle(Title.QUARK_HUNTER.name(), 900);
            ProfilePage page = new ProfilePage(game, () -> data);
            page.refresh();
            assertEquals("« " + Title.QUARK_HUNTER.label() + " »", page.headerLines().get(0), "sans choix : le plus avancé");
            assertEquals("Titres obtenus : 3 / " + Title.values().length, page.headerLines().get(4));
            Card spark = page.titleCard(Title.SPARK);
            Card eternal = page.titleCard(Title.ETERNAL);
            assertEquals(Card.State.READY, spark.state());
            assertTrue(spark.text().contains("obtenu à 1.5 s de jeu"), spark.text());
            assertEquals(Card.State.LOCKED, eternal.state());
            assertTrue(eternal.text().contains(Title.ETERNAL.condition()), eternal.text());
            assertTrue(page.wearLatestButton().isDisable());
            spark.click();
            page.refresh();
            assertEquals(Title.SPARK, game.currentTitle());
            assertEquals(Card.State.ON, spark.state());
            assertEquals("« " + Title.SPARK.label() + " »", page.headerLines().get(0));
            eternal.click();
            assertEquals(Title.SPARK, game.currentTitle(), "un titre à venir ne se porte pas");
            assertFalse(page.wearLatestButton().isDisable());
            page.wearLatestButton().fire();
            page.refresh();
            assertTrue(game.state().chosenTitle().isEmpty());
            assertEquals(Title.QUARK_HUNTER, game.currentTitle());
            return null;
        });
    }

    @Test
    void laGalerieRefuseUnAvatarVerrouilleEtGardeLImageImportee() {
        FxTestSupport.fx(() -> {
            Game game = started();
            game.state().earnTitle(Title.SHADOW_KEEPER.name(), 600);
            ProfilePage page = new ProfilePage(game, () -> data);
            page.refresh();
            assertEquals("particle", page.shownAvatar());
            assertEquals(Card.State.ON, page.avatarCard(Avatar.PARTICLE).state(), "par défaut, la particule est portée");
            Card hole = page.avatarCard(Avatar.BLACK_HOLE);
            assertEquals(Card.State.LOCKED, hole.state());
            assertTrue(hole.text().contains("avec le titre « " + Title.VOID_WATCHER.label() + " »"), hole.text());
            hole.click();
            page.refresh();
            assertTrue(game.state().avatar().isEmpty(), "verrouillé : refusé");
            page.avatarCard(Avatar.DARK_MATTER).click();
            page.refresh();
            assertEquals("dark_matter", game.state().avatar());
            assertEquals("dark_matter", page.shownAvatar());
            game.state().setAvatar("black_hole");
            page.refresh();
            assertEquals("particle", page.shownAvatar(), "un avatar verrouillé dans la sauvegarde : l'avatar par défaut");

            assertEquals(Card.State.LOCKED, page.customCard().state());
            page.importAvatar(png(sources, "portrait.png", 120, 80));
            assertTrue(Files.isRegularFile(data.resolve("avatar.png")), "copiée dans le dossier des données");
            assertEquals(Avatar.CUSTOM, game.state().avatar());
            page.refresh();
            assertEquals(Avatar.CUSTOM, page.shownAvatar());
            assertEquals(Card.State.ON, page.customCard().state());
            Path text = Files.writeString(sources.resolve("notes.txt"), "pas une image");
            assertTrue(page.importAvatar(text).startsWith("Seules les images"));
            Path fake = Files.writeString(sources.resolve("faux.png"), "pas une image non plus");
            assertEquals("Ce fichier ne se lit pas comme une image.", page.importAvatar(fake));
            assertTrue(Files.isRegularFile(data.resolve("avatar.png")), "la vraie image reste");
            page.importAvatar(png(sources, "photo.jpg", 60, 90));
            assertTrue(Files.isRegularFile(data.resolve("avatar.jpg")));
            assertFalse(Files.exists(data.resolve("avatar.png")), "la nouvelle remplace l'ancienne");
            Files.delete(data.resolve("avatar.jpg"));
            page.refresh();
            assertEquals("particle", page.shownAvatar(), "le fichier a disparu : l'avatar par défaut, sans erreur");
            assertEquals(Avatar.CUSTOM, game.state().avatar());

            ProfilePage unsaved = new ProfilePage(game, () -> null);
            unsaved.refresh();
            assertTrue(unsaved.importButton().isDisable());
            assertTrue(unsaved.importAvatar(png(sources, "x.png", 4, 4)).startsWith("Jeu lancé sans sauvegarde"));
            return null;
        });
    }

    @Test
    void leJournalEstDansLeProfilEtPlusDansLesStatistiques() {
        FxTestSupport.fx(() -> {
            Game game = started();
            idle.ui.EventLog log = idle.ui.EventLog.of(game);
            log.clear();
            ProfilePage page = new ProfilePage(game, () -> data);
            page.openJournal();
            assertEquals(0, page.journalRows());
            log.add("12:00", 3, "annonce un");
            log.add("12:01", 65, "annonce deux");
            page.refresh();
            assertEquals(2, page.journalRows());
            assertEquals("12:01  ·  1 min 05 s de jeu | annonce deux", page.journalRow(0));
            StatsPage stats = new StatsPage(game);
            stats.refresh();
            assertFalse(stats.openSubPages().contains("Journal"), stats.openSubPages().toString());
            assertFalse(stats.openSubPage("Journal"));
            return null;
        });
    }

    @Test
    void lesAlbumsMontrentLesDecouvertesEtCachentLeReste() {
        FxTestSupport.fx(() -> {
            Game game = started();
            game.state().setBigBangs(1);
            ProfilePage page = new ProfilePage(game, () -> data);
            page.refresh();
            assertEquals(List.of("Profil", "Journal", "Molécules", "États de la matière", "Assemblages", "Astres"), page.openSubPages());
            int total = game.molecules().size();
            assertEquals("0 / " + total, page.albumCounter(ProfilePage.MOLECULES));
            Molecule first = game.molecules().get(0);
            Molecule other = game.molecules().get(1);
            game.state().noteFound("molecules", first.id(), 125);
            assertTrue(page.openSubPage("Molécules"));
            assertEquals("1 / " + total, page.albumCounter(ProfilePage.MOLECULES));
            String found = page.albumText(ProfilePage.MOLECULES, first.id());
            assertTrue(found.contains(first.name()) && found.contains(first.formula()), found);
            assertTrue(found.contains("la première fois à 2 min 05 s de jeu"), found);
            String unknown = page.albumText(ProfilePage.MOLECULES, other.id());
            assertTrue(unknown.contains("???") && !unknown.contains(other.name()), unknown);
            Molecule.Kind kind = game.molecules().get(total - 1).kind();
            page.filterMolecules(kind);
            assertEquals(game.molecules().stream().filter(m -> m.kind() == kind).count(), page.albumTexts(ProfilePage.MOLECULES).size());
            page.filterMolecules(null);
            assertEquals(total, page.albumTexts(ProfilePage.MOLECULES).size());
            Molecule sort = game.molecules().stream().filter(Molecule::hasState).findFirst().orElseThrow();
            game.state().noteFound("substances", sort.id(), 10);
            assertTrue(page.albumText(ProfilePage.SUBSTANCES, sort.id()).contains(sort.state().label()));
            idle.core.sky.Body body = game.bodies().get(0);
            game.state().noteFound("bodies", body.id(), 3600);
            assertTrue(page.albumText(ProfilePage.BODIES, body.id()).contains(body.tier().label()));
            assertEquals("1 / " + game.bodies().size(), page.albumCounter(ProfilePage.BODIES));
            assertEquals("0 / " + game.assemblies().size(), page.albumCounter(ProfilePage.ASSEMBLIES));
            game.state().setBigBangs(0);
            page.refresh();
            assertTrue(page.openSubPages().contains("Molécules") && !page.openSubPages().contains("Assemblages"),
                    "sans Big Bang, un album qui a quelque chose reste : " + page.openSubPages());
            return null;
        });
    }

    @Test
    void lesAutomatesDuCollisionneurSAchetentEtSeMettentEnPause() {
        FxTestSupport.fx(() -> {
            Game game = new Game();
            game.start();
            game.state().setExplosions(1);
            game.state().addAutomation(game.automations().iterator().next().id());
            game.state().setColliderGeneratorsBought(ColliderGenerator.PLANCK, 1);
            AutomationPage page = new AutomationPage(game);
            page.refresh();
            Card beam = page.colliderCard(ColliderUpgrade.AUTO_BEAM);
            assertEquals(Card.State.LOCKED, beam.state());
            assertTrue(beam.text().contains("à acheter dans le collisionneur"), beam.text());
            game.state().setColliderUpgradeLevel(ColliderUpgrade.COLLISIONS, 1);
            game.state().setColliderEnergy(2e10);
            page.refresh();
            assertEquals(Card.State.READY, beam.state());
            beam.click();
            page.refresh();
            assertEquals(1, game.colliderUpgradeLevel(ColliderUpgrade.AUTO_BEAM), "acheté depuis l'onglet Automatisation");
            assertEquals(Card.State.ON, beam.state());
            assertTrue(beam.text().contains("en marche"), beam.text());
            beam.click();
            page.refresh();
            assertFalse(game.isColliderAutomationRunning(ColliderUpgrade.AUTO_BEAM));
            assertEquals(Card.State.OFF, beam.state());
            assertTrue(beam.text().contains("en pause"), beam.text());
            beam.click();
            page.refresh();
            assertTrue(game.isColliderAutomationRunning(ColliderUpgrade.AUTO_BEAM));
            game.setAutomationPaused(true);
            page.refresh();
            assertTrue(game.isColliderAutomationRunning(ColliderUpgrade.AUTO_BEAM), "la coupure générale ne l'arrête pas");
            assertTrue(page.pauseNoteText().contains("la coupure n'arrête ni les automates du collisionneur"), page.pauseNoteText());
            game.state().setColliderUpgradeLevel(ColliderUpgrade.AUTO_CATCH, 2);
            page.refresh();
            assertTrue(page.catchText().contains("un croisement sur trois"), page.catchText());
            game.state().setSeedUpgradeLevel(Game.SeedUpgrade.HOSE.ordinal(), 2);
            page.refresh();
            assertTrue(page.hoseText().startsWith("Tuyau d'arrosage de la graine"), page.hoseText());
            return null;
        });
    }
}

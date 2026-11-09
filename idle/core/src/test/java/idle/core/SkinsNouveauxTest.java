package idle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import idle.core.universe.Skin;
import idle.core.universe.SkinVisual;
import org.junit.jupiter.api.Test;

/** Les skins des versions 0.185 à 0.187 : les essences de l'arbre, les astres, les dix mini-jeux. */
class SkinsNouveauxTest {

    @Test
    void uneEssenceParUniversFormeAPartirDuQuatrieme() {
        assertEquals(8, Skin.TREES.length);
        for (Skin skin : Skin.TREES) assertEquals(SkinVisual.TREE, skin.visual());
        Game game = GameTestSupport.universe(3);
        game.state().setSkinsOpen(true);
        game.tick(1);
        assertEquals(0, game.treeSkinsEarned(), "trois univers : rien");
        for (Skin skin : Skin.TREES) assertFalse(game.hasSkin(skin), skin.label());
        for (int universes = 4; universes <= 11; universes++) {
            assertTrue(game.testAddUniverse());
            game.tick(1);
            assertEquals(universes - 3, game.treeSkinsEarned(), universes + " univers");
            for (int k = 0; k < Skin.TREES.length; k++) assertEquals(k < universes - 3, game.hasSkin(Skin.TREES[k]), Skin.TREES[k].label() + " à " + universes + " univers");
        }
        assertEquals("Bouleau", Skin.TREES[0].label());
        assertEquals("Univers 4 formé", Skin.TREES[0].source());
        assertEquals("Palmier", Skin.TREES[7].label());
        // Choisir une essence : le visuel de l'arbre la rend, Classique la retire.
        assertTrue(game.chooseSkin(SkinVisual.TREE, Skin.TREE_WILLOW));
        assertEquals(Skin.TREE_WILLOW, game.chosenSkin(SkinVisual.TREE));
        assertTrue(game.chooseSkin(SkinVisual.TREE, null));
        assertEquals(null, game.chosenSkin(SkinVisual.TREE));
        // Chaque skin débloqué compte pour le bonus, comme les autres (+2 %).
        assertEquals(131, Skin.values().length, "dix-neuf skins d'avant, huit essences, trois astres, dix mini-jeux, quatre molécules, six cadres, treize de chrome, "
                + "quarante de la 0.191, vingt-huit de la 0.192");
    }

    /** Version 0.186 : les skins des astres, à 10, 20 et 35 astres formés dans un univers ; acquis, ils restent. */
    @Test
    void lesSkinsDesAstresViennentAvecLesAstresFormes() {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        int formed = 0;
        for (idle.core.sky.Body body : game.bodies()) {
            game.state().addBody(body.id());
            formed++;
            game.tick(1);
            for (int k = 0; k < Skin.BODIES.length; k++) {
                assertEquals(formed >= Skin.BODIES_FORMED[k], game.hasSkin(Skin.BODIES[k]), Skin.BODIES[k].label() + " à " + formed + " astres");
            }
        }
        assertEquals(35, formed);
        assertTrue(game.hasSkin(Skin.BODY_CARTOGRAPHY));
        // Un Big Bang efface les astres, pas les skins.
        game.state().clearBigBangAct();
        assertEquals(0, game.bodiesFormed());
        game.tick(1);
        assertTrue(game.hasSkin(Skin.BODY_PLANETARIUM) && game.hasSkin(Skin.BODY_ENGRAVING) && game.hasSkin(Skin.BODY_CARTOGRAPHY));
        assertEquals(SkinVisual.BODY, Skin.BODY_ENGRAVING.visual());
    }

    /** Version 0.187 : le skin d'un mini-jeu vient avec le seuil de sa dimension. */
    @Test
    void leSkinDUnMiniJeuVientAvecLeSeuilDeSaDimension() {
        assertEquals(10, Skin.DIMENSION_SKINS.length);
        assertEquals(10, SkinVisual.DIMENSIONS.length);
        for (int n = 1; n <= 10; n++) assertEquals(SkinVisual.DIMENSIONS[n - 1], Skin.DIMENSION_SKINS[n - 1].visual(), "dimension " + n);
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        game.tick(1);
        assertFalse(game.hasSkin(Skin.LINE_RAILS));
        game.state().setDimensionShards(Game.LINE_THRESHOLD);
        game.state().setDimensionShardsEarned(Game.LINE_THRESHOLD);
        game.tick(1);
        assertTrue(game.isDimensionThresholdReached(1));
        assertTrue(game.hasSkin(Skin.LINE_RAILS), "le seuil de la ligne : les rails");
        assertFalse(game.hasSkin(Skin.PLANE_NOTEBOOK), "pas le plan");
        game.state().setPlaneShardsEarned(Game.PLANE_THRESHOLD);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.PLANE_NOTEBOOK));
    }

    /** Versions 0.188 à 0.190 : molécules, cadres de l'avatar et chrome, chacun à sa condition. */
    @Test
    void lesMoleculesLesCadresEtLeChromeOntLeursConditions() {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        game.tick(1);
        game.tick(1);   // les titres tombent une fois par seconde de jeu, les skins au pas suivant
        assertTrue(game.hasSkin(Skin.TABS_DOTS), "un univers formé : les pastilles");
        assertTrue(game.hasSkin(Skin.TABS_RIBBONS), "deux : les rubans");
        assertTrue(game.hasSkin(Skin.FRAME_POLAROID), "le titre « Créateur d'univers »");
        assertFalse(game.hasSkin(Skin.AMBIENT_PAPER));
        game.state().setTimePlayed(3600);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.AMBIENT_PAPER), "une heure de jeu");
        assertFalse(game.hasSkin(Skin.TABS_BUTTONS));
        game.state().setTimePlayed(10 * 3600);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.TABS_BUTTONS), "dix heures");
        assertFalse(game.hasSkin(Skin.MOLECULE_SKELETON));
        for (idle.core.molecules.Molecule molecule : game.molecules()) game.state().setMoleculeCount(molecule.id(), 1);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.MOLECULE_SPACE_FILLING) && game.hasSkin(Skin.MOLECULE_SKELETON), "toutes les sortes créées");
        assertFalse(game.hasSkin(Skin.CARD_GLASS));
        game.state().unlockSkin(Skin.BANG_CRYSTAL.id());
        game.tick(1);
        assertTrue(game.hasSkin(Skin.CARD_GLASS), "la tuile de verre suit le Cristal du Big Bang");
        assertEquals(SkinVisual.AMBIENT, Skin.AMBIENT_SLATE.visual());
        assertEquals(SkinVisual.CARD, Skin.CARD_TAG.visual());
        assertEquals(SkinVisual.AVATAR_FRAME, Skin.FRAME_ROUND.visual());
        assertEquals(SkinVisual.MOLECULE, Skin.MOLECULE_PEARLS.visual());
    }

    /** Version 0.191 : quarante skins de plus pour les huit visuels d'origine, chacun à sa condition. */
    @Test
    void lesHuitVisuelsDOrigineOntLeursNouveauxSkins() {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        game.tick(1);
        game.tick(1);
        assertFalse(game.hasSkin(Skin.TABLE_TINY), "aucun élément : pas de Minuscule");
        assertFalse(game.hasSkin(Skin.FUSION_EMBER));
        game.state().setElementCount(1, 1);
        for (int i = 0; i < 100; i++) game.state().stats().noteFusion(idle.core.math.BigNum.of(1), 1);
        game.state().setExplosions(25);
        game.state().setTotalAtoms(idle.core.math.BigNum.of(50));
        game.state().setCollisionRuns(100);
        game.state().setCometsCaught(25);
        game.state().setBigBangs(5);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.TABLE_TINY), "le premier élément");
        assertTrue(game.hasSkin(Skin.FUSION_EMBER), "cent fusions");
        assertFalse(game.hasSkin(Skin.FUSION_INK), "pas mille");
        assertTrue(game.hasSkin(Skin.DARK_MERCURY) && game.hasSkin(Skin.DARK_HOLE) && game.hasSkin(Skin.DARK_RAIN), "25 explosions");
        assertFalse(game.hasSkin(Skin.DARK_SNOW), "pas cinquante");
        assertTrue(game.hasSkin(Skin.ATOM_CHALK), "cinquante atomes");
        assertFalse(game.hasSkin(Skin.ATOM_CLOUD));
        assertTrue(game.hasSkin(Skin.BEAM_FIREFLIES), "cent collisions");
        assertTrue(game.hasSkin(Skin.BANG_MAP), "25 comètes");
        assertFalse(game.hasSkin(Skin.BEAM_COMET), "pas cinquante comètes");
        assertTrue(game.hasSkin(Skin.BANG_INK), "cinq Big Bangs");
        assertFalse(game.hasSkin(Skin.BANG_ORIGAMI));
        assertFalse(game.hasSkin(Skin.TABLE_RUST) || game.hasSkin(Skin.BANG_ABYSS) || game.hasSkin(Skin.FUSION_PIXEL));
        game.state().addCompletedChallenge("challenge_rust");
        game.state().addBody("ocean_planet");
        game.state().setRebuildRecord(20);
        game.state().unlockSkin(Skin.FUSION_PRISM.id());
        game.state().unlockSkin(Skin.COLLIDER_ICE.id());
        game.tick(1);
        assertTrue(game.hasSkin(Skin.TABLE_RUST), "le défi Rouille");
        assertTrue(game.hasSkin(Skin.BANG_ABYSS), "la planète océan");
        assertTrue(game.hasSkin(Skin.FUSION_PIXEL), "record sous 30 s");
        assertTrue(game.hasSkin(Skin.FUSION_GLASS) && game.hasSkin(Skin.COLLIDER_MOBIUS), "les skins qui en suivent un autre");
        game.state().setRebuildRecord(0);
        assertEquals(SkinVisual.PARTICLE_FUSION, Skin.FUSION_CLOCK.visual());
        assertEquals(SkinVisual.ATOM, Skin.ATOM_GEARS.visual());
        assertEquals(SkinVisual.TABLE, Skin.TABLE_NEON.visual());
        assertEquals(SkinVisual.DARK_BALL, Skin.DARK_LANTERN.visual());
        assertEquals(SkinVisual.DARK_ANIMATION, Skin.DARK_HOURGLASS.visual());
        assertEquals(SkinVisual.BIG_BANG, Skin.BANG_NEON.visual());
        assertEquals(SkinVisual.COLLIDER_BEAM, Skin.BEAM_LASER.visual());
        assertEquals(SkinVisual.COLLIDER, Skin.COLLIDER_STADIUM.visual());
    }

    /** Version 0.192 : les icônes d'onglet, les signes, les info-bulles, le fond des particules, leur fusion et la page du tableau. */
    @Test
    void lesIconesLesSignesLesBullesLeFondLaFusionEtLaPageOntLeursConditions() {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        game.tick(1);
        game.tick(1);
        // Les skins libres sont là d'office, et ne comptent pas pour le bonus.
        assertTrue(Skin.ICONS_STILL.isFree() && Skin.DUST_SILENCE.isFree());
        assertTrue(game.hasSkin(Skin.ICONS_STILL) && game.hasSkin(Skin.DUST_SILENCE));
        for (Skin skin : new Skin[] {Skin.ICONS_CHALK, Skin.ICONS_PIXEL, Skin.ICONS_CONSTELLATION, Skin.ICONS_EMOJI, Skin.SIGNS_RUNES,
            Skin.SIGNS_SHAPES, Skin.SIGNS_LETTERS, Skin.MATTER_LETTERS, Skin.TOOLTIP_PARCHMENT, Skin.DUST_SNOW, Skin.GENERATORS_SWIRL,
            Skin.GENERATORS_SHATTER, Skin.PAGE_MEDALS, Skin.PAGE_CRUCIBLE, Skin.PAGE_SPECTRUM}) {
            assertFalse(game.hasSkin(skin), "pas avant sa condition : " + skin);
        }
        // Les icônes d'onglet.
        game.state().setTotalAtoms(idle.core.math.BigNum.of(50));
        game.state().setRebuildRecord(45);
        game.state().setGalaxy(true);
        game.state().setTreeBubbleBought(GameUniverse.TreeBubble.LEAVES_RIGHT.ordinal(), true);
        // Les signes d'état : un succès d'un onglet différent chacun ; les signes de la matière : les sortes rassemblées.
        game.state().addAchievement("step_fusions_100");
        int gathered = 0;
        for (idle.core.molecules.Molecule molecule : game.molecules()) {
            if (gathered++ < 20) game.state().addSubstance(molecule.id());
        }
        // Les info-bulles : les heures de jeu ; le fond : les particules créées ; la fusion : les fusions.
        game.state().setTimePlayed(5 * 3600);
        game.state().stats().addParticles(idle.core.math.BigNum.of(1, 12));
        for (int i = 0; i < 100; i++) game.state().stats().noteFusion(idle.core.math.BigNum.of(1), 1);
        // La page du tableau : un ensemble complet (les gaz nobles), cent synthèses.
        for (idle.core.elements.Element noble : idle.core.elements.PeriodicTable.elements(idle.core.elements.ElementCategory.NOBLE_GAS)) {
            game.state().setElementCount(noble.number(), 1);
        }
        game.state().setSynthesisCount(100);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.ICONS_CHALK), "50 atomes");
        assertTrue(game.hasSkin(Skin.ICONS_PIXEL), "record sous la minute");
        assertTrue(game.hasSkin(Skin.ICONS_CONSTELLATION), "la galaxie");
        assertTrue(game.hasSkin(Skin.ICONS_EMOJI), "la bulle du bonsaï à droite");
        assertTrue(game.hasSkin(Skin.SIGNS_RUNES), "le succès des cent fusions");
        assertFalse(game.hasSkin(Skin.SIGNS_SHAPES) || game.hasSkin(Skin.SIGNS_LETTERS), "pas les deux autres succès");
        assertTrue(game.hasSkin(Skin.MATTER_LETTERS), "20 sortes rassemblées");
        assertFalse(game.hasSkin(Skin.MATTER_DROPS), "pas 50");
        assertTrue(game.hasSkin(Skin.TOOLTIP_PARCHMENT) && game.hasSkin(Skin.TOOLTIP_POSTIT), "cinq heures : parchemin et post-it");
        assertFalse(game.hasSkin(Skin.TOOLTIP_TERMINAL), "pas dix heures");
        assertTrue(game.hasSkin(Skin.DUST_SNOW) && game.hasSkin(Skin.DUST_RAIN) && game.hasSkin(Skin.DUST_POLLEN), "1e12 particules : trois paliers");
        assertFalse(game.hasSkin(Skin.DUST_FIREFLIES), "pas 1e15");
        assertTrue(game.hasSkin(Skin.GENERATORS_SWIRL), "cent fusions");
        assertFalse(game.hasSkin(Skin.GENERATORS_SUCTION), "pas mille");
        assertTrue(game.hasSkin(Skin.PAGE_MEDALS), "un ensemble complet");
        assertTrue(game.hasSkin(Skin.PAGE_CRUCIBLE), "cent synthèses");
        assertFalse(game.hasSkin(Skin.PAGE_SPECTRUM), "une seule famille possédée");
        // La suite : les autres succès, toutes les sortes, dix heures, toutes les familles, l'Éclair.
        game.state().addAchievement("step_set");
        game.state().addAchievement("step_explosions");
        for (idle.core.molecules.Molecule molecule : game.molecules()) game.state().addSubstance(molecule.id());
        game.state().setTimePlayed(10 * 3600);
        for (idle.core.elements.ElementCategory family : idle.core.elements.ElementCategory.values()) {
            game.state().setElementCount(idle.core.elements.PeriodicTable.elements(family).get(0).number(), 1);
        }
        game.state().unlockSkin(Skin.FUSION_FLASH.id());
        game.tick(1);
        assertTrue(game.hasSkin(Skin.SIGNS_SHAPES) && game.hasSkin(Skin.SIGNS_LETTERS));
        assertTrue(game.hasSkin(Skin.MATTER_DROPS) && game.hasSkin(Skin.MATTER_ALCHEMY), "toutes les sortes rassemblées");
        assertTrue(game.hasSkin(Skin.TOOLTIP_TERMINAL));
        assertEquals(idle.core.elements.ElementCategory.values().length, game.familiesOwned());
        assertTrue(game.hasSkin(Skin.PAGE_SPECTRUM), "toutes les familles");
        assertTrue(game.hasSkin(Skin.GENERATORS_SHATTER), "l'Éclat suit l'Éclair");
        game.state().setRebuildRecord(0);
        assertEquals(7, Skin.DUSTS.length);
        assertEquals(SkinVisual.TAB_ICONS, Skin.ICONS_EMOJI.visual());
        assertEquals(SkinVisual.CARD_SIGNS, Skin.SIGNS_RUNES.visual());
        assertEquals(SkinVisual.MATTER_SIGNS, Skin.MATTER_ALCHEMY.visual());
        assertEquals(SkinVisual.TOOLTIP, Skin.TOOLTIP_POSTIT.visual());
        assertEquals(SkinVisual.AMBIENT_DUST, Skin.DUST_ASH.visual());
        assertEquals(SkinVisual.GENERATOR_FUSION, Skin.GENERATORS_SUCTION.visual());
        assertEquals(SkinVisual.TABLE_PAGE, Skin.PAGE_SPECTRUM.visual());
    }

    /** Version 0.192 : le bonus s'arrête à cent skins comptés, et les skins libres ne comptent pas. */
    @Test
    void leBonusSArreteACentSkinsEtIgnoreLesSkinsLibres() {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        int counted = 0;
        for (Skin skin : Skin.values()) {
            if (skin.isFree()) {
                assertTrue(game.hasSkin(skin), "un skin libre est là d'office : " + skin);
                continue;
            }
            game.state().unlockSkin(skin.id());
            counted++;
            assertEquals(1 + Skin.BONUS * Math.min(counted, Skin.MAX_COUNTED), game.skinBoost(), 1e-9, "après " + skin);
        }
        assertTrue(counted > Skin.MAX_COUNTED, "assez de skins pour atteindre le plafond");
        assertEquals(Skin.MAX_COUNTED, game.skinsCounted());
        assertEquals(Skin.values().length, game.skinsUnlocked());
        assertEquals(3.0, game.skinBoost(), 1e-9);
    }
}

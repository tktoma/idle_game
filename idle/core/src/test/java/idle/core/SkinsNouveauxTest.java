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
        assertEquals(363, Skin.values().length, "six retirés en 0.203 ; dix-neuf skins d'avant, huit essences, trois astres, dix mini-jeux, quatre molécules, six cadres, treize de chrome, "
                + "quarante de la 0.191, vingt-huit de la 0.192, dix de la 0.193, trente-cinq de la 0.194, trente-six de la 0.195, cinquante-deux de la 0.196, "
                + "quarante-un de la 0.197, vingt-sept de la 0.198, vingt-quatre de la 0.199, treize de la 0.200");
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
        assertTrue(game.hasSkin(Skin.DARK_MERCURY) && game.hasSkin(Skin.DARK_HOLE), "25 explosions");
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

    /** Version 0.193 : l'échelle des grandeurs, l'arbre de matière noire et les défis, chacun à sa condition et pas avant. */
    @Test
    void lEchelleLArbreEtLesDefisOntLeursConditions() {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        game.state().setGalaxy(false);
        game.tick(1);
        game.tick(1);
        for (Skin skin : new Skin[] {Skin.RULER_WOOD, Skin.RULER_ARCHITECT, Skin.RULER_CELESTIAL, Skin.DARK_TREE_ROOTS, Skin.DARK_TREE_CIRCUIT,
            Skin.DARK_TREE_CONSTELLATION, Skin.DARK_TREE_METRO, Skin.CHALLENGES_TAROT, Skin.CHALLENGES_WANTED, Skin.CHALLENGES_SEALS}) {
            assertFalse(game.hasSkin(skin), "pas avant sa condition : " + skin);
        }
        assertEquals(0, game.darkCellsBought());
        assertEquals(0, game.darkBranchesOpened());
        // La règle : dix explosions ; l'arbre : une case par branche sauf la dernière, vingt-neuf cases ; trois défis.
        game.state().setExplosions(10);
        java.util.List<idle.core.darkmatter.DarkUpgrade> cells = new java.util.ArrayList<>();
        for (idle.core.darkmatter.DarkUpgrade dark : game.darkUpgrades()) if (!dark.isLink()) cells.add(dark);
        // Les trois branches qui ne se paient pas en matière noire sont entamées, une case d'abord par branche.
        java.util.Set<idle.core.darkmatter.DarkUpgrade.Branch> opened = java.util.EnumSet.noneOf(idle.core.darkmatter.DarkUpgrade.Branch.class);
        for (idle.core.darkmatter.DarkUpgrade dark : cells) {
            if (dark.branch() != idle.core.darkmatter.DarkUpgrade.Branch.DARK_MATTER && opened.add(dark.branch())) game.state().setDarkLevel(dark.id(), 1);
        }
        for (idle.core.darkmatter.DarkUpgrade dark : cells) {
            if (game.darkCellsBought() >= 29) break;
            if (dark.branch() != idle.core.darkmatter.DarkUpgrade.Branch.DARK_MATTER) game.state().setDarkLevel(dark.id(), 1);
        }
        java.util.List<idle.core.darkmatter.Challenge> challenges = idle.core.darkmatter.Challenges.DEFAULT;
        for (int i = 0; i < 3; i++) game.state().addCompletedChallenge(challenges.get(i).id());
        game.tick(1);
        assertTrue(game.hasSkin(Skin.RULER_WOOD), "dix explosions : le bois");
        assertFalse(game.hasSkin(Skin.RULER_ARCHITECT), "pas vingt");
        assertEquals(29, game.darkCellsBought());
        assertEquals(29, game.darkLevelsBought());
        assertEquals(3, game.darkBranchesOpened(), "trois branches sur quatre");
        assertFalse(game.hasSkin(Skin.DARK_TREE_ROOTS) || game.hasSkin(Skin.DARK_TREE_METRO) || game.hasSkin(Skin.DARK_TREE_CIRCUIT));
        assertTrue(game.hasSkin(Skin.CHALLENGES_TAROT), "trois défis");
        assertFalse(game.hasSkin(Skin.CHALLENGES_WANTED), "pas six");
        // La suite : vingt explosions, le succès des paliers, une trentième case en matière noire (à cent niveaux), la galaxie, tous les défis.
        game.state().setExplosions(20);
        game.state().addAchievement("step_landmarks");
        for (idle.core.darkmatter.DarkUpgrade dark : cells) {
            if (dark.branch() == idle.core.darkmatter.DarkUpgrade.Branch.DARK_MATTER) {
                game.state().setDarkLevel(dark.id(), 71);
                break;
            }
        }
        game.state().setGalaxy(true);
        for (idle.core.darkmatter.Challenge challenge : challenges) game.state().addCompletedChallenge(challenge.id());
        game.tick(1);
        assertTrue(game.hasSkin(Skin.RULER_ARCHITECT), "vingt explosions");
        assertTrue(game.hasSkin(Skin.RULER_CELESTIAL), "les 26 paliers");
        assertEquals(30, game.darkCellsBought());
        assertEquals(100, game.darkLevelsBought());
        assertEquals(4, game.darkBranchesOpened());
        assertTrue(game.hasSkin(Skin.DARK_TREE_ROOTS), "trente cases");
        assertTrue(game.hasSkin(Skin.DARK_TREE_CIRCUIT), "cent niveaux");
        assertTrue(game.hasSkin(Skin.DARK_TREE_CONSTELLATION), "la galaxie");
        assertTrue(game.hasSkin(Skin.DARK_TREE_METRO), "les quatre branches");
        assertTrue(game.hasSkin(Skin.CHALLENGES_WANTED) && game.hasSkin(Skin.CHALLENGES_SEALS), "six défis, puis les dix");
        assertEquals(SkinVisual.DARK_RULER, Skin.RULER_CELESTIAL.visual());
        assertEquals(SkinVisual.DARK_TREE, Skin.DARK_TREE_METRO.visual());
        assertEquals(SkinVisual.CHALLENGES, Skin.CHALLENGES_SEALS.visual());
    }

    /** Version 0.194 : le Big Bang au-delà des molécules : chaque skin vient à sa condition, pas avant ; les palettes sont libres. */
    @Test
    void lesAmasLesBlocsLeGazLeCielLeCosmosLesSystemesEtLesSondesOntLeursConditions() {
        for (Skin skin : new Skin[] {Skin.PALETTE_PASTEL, Skin.PALETTE_SAFE, Skin.PALETTE_MONO}) {
            assertTrue(skin.isFree(), "une palette est libre : " + skin);
            assertEquals(SkinVisual.MOLECULE_PALETTE, skin.visual());
        }
        Game game = GameTestSupport.universe(1);
        game.state().setSkinsOpen(true);
        game.state().setGalaxy(false);
        game.tick(1);
        game.tick(1);
        // Un univers formé : le premier skin de la galaxie, de l'amas et de la toile ; rien d'autre.
        assertTrue(game.hasSkin(Skin.GALAXY_NAUTILUS) && game.hasSkin(Skin.CLUSTER_GRAPES) && game.hasSkin(Skin.WEB_NEURONS), "l'univers 1 formé");
        for (Skin skin : new Skin[] {Skin.CLUMPS_MINERALS, Skin.CLUMPS_NEON, Skin.CLUMPS_PASTEL, Skin.BLOCKS_STONE, Skin.BLOCKS_INGOTS, Skin.BLOCKS_CRYSTALS,
            Skin.BLOCKS_ICE, Skin.GAS_HAZE, Skin.GAS_SMOKE, Skin.GAS_AURORA, Skin.SKY_PLATE, Skin.SKY_MOBILE, Skin.SKY_SHOWCASE, Skin.GALAXY_YARN,
            Skin.GALAXY_FINGERPRINT, Skin.GALAXY_WHIRLPOOL, Skin.GALAXY_FIREWORK, Skin.CLUSTER_LANTERNS, Skin.CLUSTER_BEES, Skin.WEB_LACE, Skin.WEB_DEW,
            Skin.WEB_METRO, Skin.SYSTEM_ORRERY, Skin.SYSTEM_CRAYON, Skin.SYSTEM_RADAR, Skin.PROBE_PAPER, Skin.PROBE_BALLOON, Skin.PROBE_BOTTLE,
            Skin.PROBE_PIGEON}) {
            assertFalse(game.hasSkin(skin), "pas avant sa condition : " + skin);
        }
        // Cinq sortes rassemblées, dont un gaz à mille molécules ; dix assemblages ; quinze astres ; un système complet ; dix voyages.
        int gathered = 0;
        for (idle.core.molecules.Molecule molecule : game.molecules()) {
            if (gathered >= 5 || molecule.state() != idle.core.molecules.Molecule.State.GAS) continue;
            game.state().setMoleculeCount(molecule.id(), gathered == 0 ? 1_000 : 1);
            game.state().addSubstance(molecule.id());
            gathered++;
        }
        assertEquals(5, game.substancesFormed());
        assertEquals(1_004, game.gatheredInState(idle.core.molecules.Molecule.State.GAS));
        java.util.List<idle.core.sky.Assembly> assemblies = game.assemblies();
        for (int i = 0; i < 10; i++) game.state().addAssembly(assemblies.get(i).id());
        java.util.List<idle.core.sky.Body> bodies = game.bodies();
        for (int i = 0; i < 15; i++) game.state().addBody(bodies.get(i).id());
        java.util.List<idle.core.sky.SolarSystem> systems = new java.util.ArrayList<>();
        systems.add(new idle.core.sky.SolarSystem("yellow_sun", java.util.List.of("rocky_planet", "ocean_planet"), java.util.List.of()));
        game.state().setSolarSystems(systems);
        game.state().setProbeRuns(10);
        game.tick(1);
        assertEquals(1, game.solarSystemsComplete());
        assertTrue(game.hasSkin(Skin.CLUMPS_MINERALS), "cinq sortes");
        assertFalse(game.hasSkin(Skin.CLUMPS_NEON), "pas vingt");
        assertTrue(game.hasSkin(Skin.BLOCKS_STONE), "dix assemblages");
        assertFalse(game.hasSkin(Skin.BLOCKS_INGOTS), "pas quinze");
        assertTrue(game.hasSkin(Skin.GAS_HAZE), "mille molécules de gaz");
        assertFalse(game.hasSkin(Skin.GAS_SMOKE), "pas dix mille");
        assertTrue(game.hasSkin(Skin.SKY_PLATE), "quinze astres");
        assertFalse(game.hasSkin(Skin.SKY_MOBILE), "pas vingt-cinq");
        assertTrue(game.hasSkin(Skin.SYSTEM_ORRERY), "un système complet");
        assertFalse(game.hasSkin(Skin.SYSTEM_CRAYON), "pas deux");
        assertTrue(game.hasSkin(Skin.PROBE_PAPER), "dix voyages");
        assertFalse(game.hasSkin(Skin.PROBE_BALLOON), "pas vingt-cinq");
        // Tout le reste : toutes les sortes rassemblées (cent mille molécules de gaz), tous les assemblages et les astres, trois systèmes,
        // cent voyages, cinq univers.
        for (idle.core.molecules.Molecule molecule : game.molecules()) {
            game.state().setMoleculeCount(molecule.id(), molecule.state() == idle.core.molecules.Molecule.State.GAS ? 100_000 : 1);
            game.state().addSubstance(molecule.id());
        }
        for (idle.core.sky.Assembly assembly : assemblies) game.state().addAssembly(assembly.id());
        for (idle.core.sky.Body body : bodies) game.state().addBody(body.id());
        systems.add(new idle.core.sky.SolarSystem("red_dwarf", java.util.List.of("rocky_planet", "ocean_planet"), java.util.List.of()));
        systems.add(new idle.core.sky.SolarSystem("blue_giant", java.util.List.of("rocky_planet", "ocean_planet"), java.util.List.of()));
        game.state().setSolarSystems(systems);
        game.state().setProbeRuns(100);
        for (int k = 2; k <= 5; k++) assertTrue(game.testAddUniverse());
        game.tick(1);
        assertEquals(30, game.assembliesFormed());
        assertEquals(35, game.bodiesFormed());
        assertEquals(3, game.solarSystemsComplete());
        assertEquals(5, game.universes());
        for (Skin skin : Skin.values()) {
            if (skin.ordinal() >= Skin.PALETTE_PASTEL.ordinal() && skin.ordinal() < Skin.DETECTOR_EYE.ordinal()) assertTrue(game.hasSkin(skin), "tout est là : " + skin);
        }
        assertEquals(SkinVisual.CLUMPS, Skin.CLUMPS_NEON.visual());
        assertEquals(SkinVisual.BLOCKS, Skin.BLOCKS_ICE.visual());
        assertEquals(SkinVisual.GAS, Skin.GAS_AURORA.visual());
        assertEquals(SkinVisual.SKY, Skin.SKY_SHOWCASE.visual());
        assertEquals(SkinVisual.GALAXY, Skin.GALAXY_FIREWORK.visual());
        assertEquals(SkinVisual.GALAXY_CLUSTER, Skin.CLUSTER_BEES.visual());
        assertEquals(SkinVisual.COSMIC_WEB, Skin.WEB_METRO.visual());
        assertEquals(SkinVisual.SYSTEM, Skin.SYSTEM_RADAR.visual());
        assertEquals(SkinVisual.PROBE, Skin.PROBE_PIGEON.visual());
    }

    /**
     * Version 0.195 : le reste du collisionneur, les hadrons et le vide. Chaque skin vient à sa condition, pas avant : les
     * collisions, les croisements, trois épreuves, les hadrons liés et chaque sorte, les gluons et leurs paliers, le vide ouvert
     * et le plus haut niveau d'une amélioration, les paires nées, le trou noir du vide et les univers qu'il compte.
     */
    @Test
    void leCollisionneurLesHadronsEtLeVideOntLeursConditions() {
        Game game = GameTestSupport.universe(3);
        game.state().setSkinsOpen(true);
        game.tick(1);
        game.tick(1);
        for (Skin skin : Skin.values()) {
            if (skin.ordinal() >= Skin.DETECTOR_EYE.ordinal() && skin.ordinal() < Skin.BUBBLE_SOAP.ordinal()) assertFalse(game.hasSkin(skin), "pas avant sa condition : " + skin);
        }
        // Le détecteur et les éclairs : collisions et croisements, palier par palier.
        for (int i = 0; i < Skin.DETECTORS.length; i++) {
            game.state().setCollisionRuns(Skin.DETECTOR_COLLISIONS[i] - 1);
            game.tick(1);
            assertFalse(game.hasSkin(Skin.DETECTORS[i]), "une collision de moins : " + Skin.DETECTORS[i]);
            game.state().setCollisionRuns(Skin.DETECTOR_COLLISIONS[i]);
            game.tick(1);
            assertTrue(game.hasSkin(Skin.DETECTORS[i]), Skin.DETECTORS[i].label());
        }
        for (int i = 0; i < Skin.LIGHTNINGS.length; i++) {
            // Le collisionneur tourne pendant le pas : une marge de cinquante croisements.
            game.state().setColliderCrossings(Skin.LIGHTNING_CROSSINGS[i] - 50);
            game.tick(1);
            assertFalse(game.hasSkin(Skin.LIGHTNINGS[i]), "des croisements de moins : " + Skin.LIGHTNINGS[i]);
            game.state().setColliderCrossings(Skin.LIGHTNING_CROSSINGS[i]);
            game.tick(1);
            assertTrue(game.hasSkin(Skin.LIGHTNINGS[i]), Skin.LIGHTNINGS[i].label());
        }
        // Les étincelles : une épreuve chacune.
        game.state().addCompletedTrial(idle.core.universe.Trial.DETECTOR.name());
        game.tick(1);
        assertTrue(game.hasSkin(Skin.SPARKS_PETALS));
        assertFalse(game.hasSkin(Skin.SPARKS_CONFETTI) || game.hasSkin(Skin.SPARKS_EMBERS));
        game.state().addCompletedTrial(idle.core.universe.Trial.NO_OVERHEAT.name());
        game.state().addCompletedTrial(idle.core.universe.Trial.FULL_CHARGE.name());
        game.tick(1);
        assertTrue(game.hasSkin(Skin.SPARKS_CONFETTI) && game.hasSkin(Skin.SPARKS_EMBERS));
        // Les hadrons liés : l'envol des quarks et les quarks de la liaison ; les gluons (deux ou trois par hadron) et leurs paliers.
        game.state().setHadrons(idle.core.collider.Hadron.PROTON, 9);
        game.tick(1);
        assertEquals(9, game.hadronsMade());
        assertEquals(27, game.gluons());
        assertFalse(game.hasSkin(Skin.QUARK_FLIGHT_BALLOONS) || game.hasSkin(Skin.QUARKS_MARBLES) || game.hasSkin(Skin.GLUONS_SPRINGS), "neuf hadrons : rien");
        game.state().setHadrons(idle.core.collider.Hadron.PROTON, 10);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.QUARK_FLIGHT_BALLOONS) && game.hasSkin(Skin.QUARKS_MARBLES), "dix hadrons");
        assertFalse(game.hasSkin(Skin.QUARK_FLIGHT_BUTTERFLIES) || game.hasSkin(Skin.QUARKS_FRUITS) || game.hasSkin(Skin.GLUONS_SPRINGS), "trente gluons");
        game.state().setHadrons(idle.core.collider.Hadron.PION, 24);       // 34 hadrons, 78 gluons
        game.tick(1);
        assertTrue(game.hasSkin(Skin.QUARK_FLIGHT_BUTTERFLIES), "vingt-cinq hadrons");
        assertFalse(game.hasSkin(Skin.QUARK_FLIGHT_SCRABBLE) || game.hasSkin(Skin.QUARKS_FRUITS) || game.hasSkin(Skin.GLUONS_SPRINGS));
        game.state().setHadrons(idle.core.collider.Hadron.PION, 40);       // 50 hadrons, 110 gluons
        game.tick(1);
        assertTrue(game.hasSkin(Skin.QUARK_FLIGHT_SCRABBLE) && game.hasSkin(Skin.QUARKS_FRUITS) && game.hasSkin(Skin.GLUONS_SPRINGS));
        assertFalse(game.hasSkin(Skin.QUARKS_NOTES) || game.hasSkin(Skin.GLUONS_RUBBER) || game.hasSkin(Skin.QUARKS_PUZZLE));
        game.state().setHadrons(idle.core.collider.Hadron.PION, 90);       // 100 hadrons, 210 gluons
        game.tick(1);
        assertTrue(game.hasSkin(Skin.QUARKS_NOTES), "cent hadrons");
        assertFalse(game.hasSkin(Skin.GLUONS_RUBBER), "210 gluons");
        game.state().setHadrons(idle.core.collider.Hadron.PION, 110);      // 250 gluons
        game.tick(1);
        assertTrue(game.hasSkin(Skin.GLUONS_RUBBER));
        assertFalse(game.hasSkin(Skin.GLUONS_WOOL));
        game.state().setHadrons(idle.core.collider.Hadron.PION, 235);      // 500 gluons
        game.tick(1);
        assertTrue(game.hasSkin(Skin.GLUONS_WOOL));
        assertFalse(game.hasSkin(Skin.GLUONS_LIGHTNING) || game.hasSkin(Skin.HADRON_BACK_BLACKBOARD));
        game.state().setHadrons(idle.core.collider.Hadron.PION, 485);      // 1 000 gluons : le palier « Mémoire des liaisons »
        game.tick(1);
        assertTrue(game.hasSkin(Skin.GLUONS_LIGHTNING) && game.hasSkin(Skin.HADRON_BACK_BLACKBOARD));
        assertFalse(game.hasSkin(Skin.HADRON_BACK_LAB));
        game.state().setHadrons(idle.core.collider.Hadron.PION, 4_985);    // 10 000 gluons
        game.tick(1);
        assertTrue(game.hasSkin(Skin.HADRON_BACK_LAB));
        assertFalse(game.hasSkin(Skin.HADRON_BACK_STORM));
        game.state().setHadrons(idle.core.collider.Hadron.PION, 24_985);   // 50 000 gluons
        game.tick(1);
        assertTrue(game.hasSkin(Skin.HADRON_BACK_STORM));
        assertFalse(game.hasSkin(Skin.QUARKS_PUZZLE), "deux sortes seulement");
        for (idle.core.collider.Hadron hadron : idle.core.collider.Hadron.values()) game.state().setHadrons(hadron, 1);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.QUARKS_PUZZLE), "chaque sorte liée");
        // Le vide : ouvert, puis une amélioration au niveau 1, 2, 3.
        for (Skin skin : Skin.FOAMS) assertFalse(game.hasSkin(skin), "le vide fermé : " + skin);
        game.testOpenVoid();
        game.tick(1);
        assertTrue(game.hasSkin(Skin.FOAM_SOAP));
        assertFalse(game.hasSkin(Skin.FOAM_BEER));
        for (int level = 1; level <= 3; level++) {
            game.state().setVoidLevel(GameUniverse.VoidUpgrade.GAIN.ordinal(), level);
            game.tick(1);
            for (int i = 1; i < Skin.FOAMS.length; i++) assertEquals(i <= level, game.hasSkin(Skin.FOAMS[i]), Skin.FOAMS[i] + " au niveau " + level);
        }
        // Les paires nées depuis l'ouverture de la fenêtre : cent, mille, dix mille (l'outil d'essai les ajoute).
        for (Skin skin : Skin.PAIR_SKINS) assertFalse(game.hasSkin(skin), skin.label());
        game.testAddVoidPairsBorn(99);
        game.tick(1);
        assertFalse(game.hasSkin(Skin.PAIRS_TWINS), "99 paires");
        game.testAddVoidPairsBorn(1);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.PAIRS_TWINS), "cent paires");
        assertFalse(game.hasSkin(Skin.PAIRS_MAGNETS));
        game.testAddVoidPairsBorn(900);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.PAIRS_MAGNETS), "mille paires");
        assertFalse(game.hasSkin(Skin.PAIRS_YINYANG));
        game.testAddVoidPairsBorn(9_000);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.PAIRS_YINYANG), "dix mille paires");
        // Le trou noir du vide : ouvert, il compte les univers à partir du quatrième.
        assertEquals(0, game.voidHoleUniverses(), "trois univers : rien à compter");
        for (int n = 1; n <= Skin.VOID_HOLES.length; n++) {
            assertTrue(game.testAddUniverse());
            game.state().setVoidOpen(true);
            game.state().setVoidHoleOpen(false);
            game.tick(1);
            assertEquals(n, game.voidHoleUniverses());
            assertFalse(game.hasSkin(Skin.VOID_HOLES[n - 1]), "le trou noir du vide fermé : " + Skin.VOID_HOLES[n - 1]);
            game.testOpenVoidHole();
            game.tick(1);
            for (int i = 0; i < Skin.VOID_HOLES.length; i++) assertEquals(i < n, game.hasSkin(Skin.VOID_HOLES[i]), Skin.VOID_HOLES[i] + " à " + n + " univers comptés");
        }
        for (Skin skin : Skin.values()) {
            if (skin.ordinal() >= Skin.DETECTOR_EYE.ordinal() && skin.ordinal() < Skin.BUBBLE_SOAP.ordinal()) assertTrue(game.hasSkin(skin), "tout est là : " + skin);
        }
        assertEquals(SkinVisual.DETECTOR, Skin.DETECTOR_CAMERA.visual());
        assertEquals(SkinVisual.LIGHTNING, Skin.LIGHTNING_ROOTS.visual());
        assertEquals(SkinVisual.SPARKS, Skin.SPARKS_EMBERS.visual());
        assertEquals(SkinVisual.QUARK_FLIGHT, Skin.QUARK_FLIGHT_SCRABBLE.visual());
        assertEquals(SkinVisual.QUARKS, Skin.QUARKS_PUZZLE.visual());
        assertEquals(SkinVisual.GLUONS, Skin.GLUONS_LIGHTNING.visual());
        assertEquals(SkinVisual.HADRON_BACKGROUND, Skin.HADRON_BACK_STORM.visual());
        assertEquals(SkinVisual.VOID_FOAM, Skin.FOAM_TV.visual());
        assertEquals(SkinVisual.VOID_PAIRS, Skin.PAIRS_YINYANG.visual());
        assertEquals(SkinVisual.VOID_HOLE, Skin.VOID_HOLE_TEA.visual());
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

    /**
     * Version 0.196 : l'univers. Les bulles, la naissance, les liens et le lancement suivent les univers formés ; la graine ses
     * paliers ; l'arrosoir les arrosages, le tuyau et la terre fertile ; l'étagère et le chrono l'étage du chrono, le chrono
     * plein et les univers ; le tuyau ses niveaux ; le rond, la météo et les saisons leurs comptes depuis l'ouverture.
     */
    @Test
    void lUniversLaGraineEtSonEtabliOntLeursConditions() {
        Game game = GameTestSupport.universe(1);
        game.state().setSkinsOpen(true);
        game.tick(1);
        game.tick(1);
        for (Skin skin : Skin.values()) {
            if (skin.ordinal() < Skin.BUBBLE_SOAP.ordinal() || skin.ordinal() >= Skin.TREE_AUTUMN.ordinal()) continue;
            boolean first = skin == Skin.BUBBLE_SOAP || skin == Skin.SEED_WALNUT;
            assertEquals(first, game.hasSkin(skin), "un univers formé, la graine au palier 1 : " + skin);
        }
        // L'arrosoir : les arrosages de la graine en cours, puis le tuyau et la terre fertile.
        for (int i = 0; i < Skin.CANS.length; i++) {
            game.state().setSeedWaterings(Skin.CAN_WATERINGS[i] - 1);
            game.tick(1);
            assertFalse(game.hasSkin(Skin.CANS[i]), "un arrosage de moins : " + Skin.CANS[i]);
            game.state().setSeedWaterings(Skin.CAN_WATERINGS[i]);
            game.tick(1);
            assertTrue(game.hasSkin(Skin.CANS[i]), Skin.CANS[i].label());
        }
        game.state().setSeedWaterings(0);
        // La graine : un skin par palier ; la graine ne prend pas plus de deux paliers d'avance sur les univers.
        game.state().setSeedSprouted(true);
        for (int stage = 2; stage <= Skin.SEEDS.length; stage++) {
            while (game.seedStageCap() < stage) assertTrue(game.testAddUniverse());
            game.state().setSeedWaterings(Game.seedStageWaterings(stage) - 1);
            game.tick(1);
            assertEquals(stage - 1, game.seedStage());
            assertFalse(game.hasSkin(Skin.SEEDS[stage - 1]), "un arrosage avant le palier : " + Skin.SEEDS[stage - 1]);
            game.state().setSeedWaterings(Game.seedStageWaterings(stage));
            game.tick(1);
            assertEquals(stage, game.seedStage());
            assertTrue(game.hasSkin(Skin.SEEDS[stage - 1]), Skin.SEEDS[stage - 1].label());
        }
        assertEquals(5, game.universes(), "le palier 7 demande cinq univers");
        // Les bulles, la naissance, le lancement, les liens : par les univers formés, de cinq à dix.
        for (int universes = 5; universes <= 10; universes++) {
            if (game.universes() < universes) assertTrue(game.testAddUniverse());
            game.tick(1);
            for (int i = 0; i < Skin.UNIVERSE_BUBBLES.length; i++) assertEquals(universes >= i + 1, game.hasSkin(Skin.UNIVERSE_BUBBLES[i]), Skin.UNIVERSE_BUBBLES[i] + " à " + universes);
            for (int i = 0; i < Skin.BIRTHS.length; i++) assertEquals(universes >= Skin.FIRST_BIRTH_UNIVERSE + i, game.hasSkin(Skin.BIRTHS[i]), Skin.BIRTHS[i] + " à " + universes);
            for (int i = 0; i < Skin.LAUNCHES.length; i++) assertEquals(universes >= Skin.FIRST_LAUNCH_UNIVERSE + i, game.hasSkin(Skin.LAUNCHES[i]), Skin.LAUNCHES[i] + " à " + universes);
            for (int i = 0; i < Skin.LINKS.length; i++) assertEquals(universes >= Skin.FIRST_LINKS_UNIVERSE + i, game.hasSkin(Skin.LINKS[i]), Skin.LINKS[i] + " à " + universes);
            // Sans l'étage du chrono, ni étagère ni chrono, quel que soit le nombre d'univers.
            for (Skin skin : Skin.SHELVES) assertFalse(game.hasSkin(skin), skin.label());
            for (Skin skin : Skin.CHRONOS) assertFalse(game.hasSkin(skin), skin.label());
        }
        // Le tuyau et la terre fertile : l'arrosoir, puis les niveaux du tuyau.
        game.state().setSeedUpgradeLevel(Game.SeedUpgrade.HOSE.ordinal(), 1);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.CAN_BEAK));
        assertFalse(game.hasSkin(Skin.CAN_SHELL) || game.hasSkin(Skin.HOSE_VINE));
        game.state().setSeedUpgradeLevel(Game.SeedUpgrade.SOIL.ordinal(), 1);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.CAN_SHELL));
        for (int i = 0; i < Skin.HOSES.length; i++) {
            game.state().setSeedUpgradeLevel(Game.SeedUpgrade.HOSE.ordinal(), Skin.HOSE_LEVELS[i] - 1);
            game.tick(1);
            assertFalse(game.hasSkin(Skin.HOSES[i]), "un niveau de moins : " + Skin.HOSES[i]);
            game.state().setSeedUpgradeLevel(Game.SeedUpgrade.HOSE.ordinal(), Skin.HOSE_LEVELS[i]);
            game.tick(1);
            assertTrue(game.hasSkin(Skin.HOSES[i]), Skin.HOSES[i].label());
        }
        assertEquals(Game.SeedUpgrade.HOSE.max(), Skin.HOSE_LEVELS[Skin.HOSE_LEVELS.length - 1], "le dernier au plus haut niveau du tuyau");
        game.state().setSeedUpgradeLevel(Game.SeedUpgrade.HOSE.ordinal(), 0);
        // L'étagère : l'étage du chrono acheté, avec dix univers formés, tous ses skins ; le chrono, plein seulement.
        game.state().setSeedUpgradeLevel(Game.SeedUpgrade.CHRONO.ordinal(), 1);
        game.state().setChronoStored(game.chronoCapacity() - 1);
        game.tick(1);
        for (Skin skin : Skin.SHELVES) assertTrue(game.hasSkin(skin), skin.label());
        assertTrue(game.chronoStored() < game.chronoCapacity() - 0.5, "le chrono n'est pas plein");
        for (Skin skin : Skin.CHRONOS) assertFalse(game.hasSkin(skin), "pas plein : " + skin);
        game.state().setChronoStored(game.chronoCapacity());
        game.tick(1);
        for (Skin skin : Skin.CHRONOS) assertTrue(game.hasSkin(skin), skin.label());
        // Le rond de croissance : les ronds cueillis depuis l'ouverture.
        assertTrue(game.isBloomUnlocked());
        for (long caught = 1; caught <= Skin.RING_CAUGHT[Skin.RING_CAUGHT.length - 1]; caught++) {
            game.testShowBloom();
            assertTrue(game.catchBloom());
            assertEquals(caught, game.bloomsCaught());
            game.tick(1);
            for (int i = 0; i < Skin.RINGS.length; i++) assertEquals(caught >= Skin.RING_CAUGHT[i], game.hasSkin(Skin.RINGS[i]), Skin.RINGS[i] + " à " + caught);
        }
        // Les saisons : chaque cycle complet depuis l'ouverture, huit minutes.
        assertTrue(game.areSeasonsUnlocked());
        game.testSetSeason(Game.Season.WINTER);
        game.tick(Game.SEASON_SECONDS - 1);
        assertEquals(0, game.seasonCycles());
        for (Skin skin : Skin.SEASON_SKINS) assertFalse(game.hasSkin(skin), skin.label());
        for (int cycle = 1; cycle <= Skin.SEASON_SKINS.length; cycle++) {
            game.tick(cycle == 1 ? 1 : Game.SEASON_SECONDS * Game.Season.values().length);
            assertEquals(cycle, game.seasonCycles());
            // Les saisons avancent après les skins dans le pas : le skin vient au pas suivant.
            game.tick(1);
            for (int i = 0; i < Skin.SEASON_SKINS.length; i++) assertEquals(cycle >= i + 1, game.hasSkin(Skin.SEASON_SKINS[i]), Skin.SEASON_SKINS[i] + " au cycle " + cycle);
        }
        // La météo : les orages depuis l'ouverture ; le temps variable tourne au beau temps ou à l'orage au bout d'une minute.
        assertTrue(game.isWeatherUnlocked());
        for (int turn = 0; turn < 400 && game.storms() < Skin.WEATHER_STORMS[Skin.WEATHER_STORMS.length - 1]; turn++) {
            game.testSetWeather(Game.Weather.VARIABLE);
            game.tick(Game.WEATHER_VARIABLE_SECONDS + 1);
            // La météo tourne après les skins dans le pas : le skin vient au pas suivant.
            game.tick(1);
            for (int i = 0; i < Skin.WEATHERS.length; i++) assertEquals(game.storms() >= Skin.WEATHER_STORMS[i], game.hasSkin(Skin.WEATHERS[i]), Skin.WEATHERS[i] + " à " + game.storms());
        }
        assertTrue(game.hasSkin(Skin.WEATHER_PASTEL), "dix orages en quatre cents tours");
        assertEquals(SkinVisual.UNIVERSE_BUBBLE, Skin.BUBBLE_ORRERY.visual());
        assertEquals(SkinVisual.LAUNCH, Skin.LAUNCH_CURTAIN.visual());
        assertEquals(SkinVisual.SEASONS, Skin.SEASONS_STAINED.visual());
    }

    /**
     * Version 0.197 : l'arbre au-delà de ses essences (saisons figées, fantaisie, sol), ses bulles et leurs liens, la carte
     * des dimensions, le remplissage du défi final, les cadres des mini-jeux ; la palette des dimensions est libre.
     */
    @Test
    void lArbreSesBullesLaCarteLeDefiFinalEtLesCadresOntLeursConditions() {
        Game game = GameTestSupport.universe(1);
        game.state().setSkinsOpen(true);
        game.tick(1);
        game.tick(1);
        for (Skin skin : Skin.values()) {
            if (skin.ordinal() < Skin.TREE_AUTUMN.ordinal() || skin.ordinal() >= Skin.AVATAR_LINE.ordinal()) continue;
            assertEquals(skin.isFree(), game.hasSkin(skin), "un univers formé, rien d'acheté : " + skin);
        }
        for (Skin skin : Skin.values()) {
            if (skin.ordinal() >= Skin.TREE_AUTUMN.ordinal() && skin.ordinal() < Skin.AVATAR_LINE.ordinal()) {
                assertEquals(skin.visual() == SkinVisual.DIMENSION_PALETTE, skin.isFree(), "libre : " + skin);
            }
        }
        // Le sol : un skin par palier de la graine, du deuxième au septième.
        game.state().setSeedSprouted(true);
        for (int stage = Skin.FIRST_GROUND_STAGE; stage < Skin.FIRST_GROUND_STAGE + Skin.GROUNDS.length; stage++) {
            while (game.seedStageCap() < stage) assertTrue(game.testAddUniverse());
            game.state().setSeedWaterings(Game.seedStageWaterings(stage) - 1);
            game.tick(1);
            assertFalse(game.hasSkin(Skin.GROUNDS[stage - Skin.FIRST_GROUND_STAGE]), "un arrosage avant le palier : " + stage);
            game.state().setSeedWaterings(Game.seedStageWaterings(stage));
            game.tick(1);
            assertTrue(game.hasSkin(Skin.GROUNDS[stage - Skin.FIRST_GROUND_STAGE]), Skin.GROUNDS[stage - Skin.FIRST_GROUND_STAGE].label());
        }
        // La carte et les cadres : les dimensions ouvertes, une par univers.
        for (int universes = game.universes(); universes <= 10; universes++) {
            while (game.universes() < universes) assertTrue(game.testAddUniverse());
            game.tick(1);
            assertEquals(universes, game.dimensionsUnlocked());
            for (int i = 0; i < Skin.MAPS.length; i++) assertEquals(universes >= Skin.MAP_DIMENSIONS[i], game.hasSkin(Skin.MAPS[i]), Skin.MAPS[i] + " à " + universes);
            for (int i = 0; i < Skin.FRAMES.length; i++) assertEquals(universes >= Skin.FRAME_DIMENSIONS[i], game.hasSkin(Skin.FRAMES[i]), Skin.FRAMES[i] + " à " + universes);
            // Les saisons figées : les saisons débloquées au neuvième univers, puis un univers de plus chacune.
            assertEquals(universes >= 9, game.areSeasonsUnlocked());
            for (int i = 0; i < Skin.TREE_SEASONS.length; i++) assertEquals(universes >= Skin.FIRST_TREE_SEASON_UNIVERSE + i, game.hasSkin(Skin.TREE_SEASONS[i]), Skin.TREE_SEASONS[i] + " à " + universes);
            // Sans le défi fini, aucun remplissage.
            for (Skin skin : Skin.FINALES) assertFalse(game.hasSkin(skin), skin.label());
        }
        assertTrue(game.testAddUniverse());
        game.tick(1);
        assertTrue(game.hasSkin(Skin.TREE_SPRING), "onze univers");
        // Les bulles : les paliers différents pour la fantaisie, le nombre acheté pour les bulles et leurs liens.
        assertEquals(19, GameUniverse.TreeBubble.values().length, "les skins des bulles comptent sur dix-neuf bulles");
        assertEquals(Skin.TREE_BUBBLE_COUNTS[Skin.TREE_BUBBLE_COUNTS.length - 1], GameUniverse.TreeBubble.values().length);
        assertEquals(Skin.BUBBLE_LINK_COUNTS[Skin.BUBBLE_LINK_COUNTS.length - 1], GameUniverse.TreeBubble.values().length);
        int bought = 0;
        for (GameUniverse.TreeBubble bubble : GameUniverse.TreeBubble.values()) {
            game.state().setTreeBubbleBought(bubble.ordinal(), true);
            bought++;
            game.tick(1);
            assertEquals(bought, game.treeBubblesBought());
            int stages = game.treeBubbleStagesBought();
            for (int i = 0; i < Skin.TREE_FANTASIES.length; i++) assertEquals(stages >= i + 1, game.hasSkin(Skin.TREE_FANTASIES[i]), Skin.TREE_FANTASIES[i] + " à " + stages + " paliers");
            for (int i = 0; i < Skin.TREE_BUBBLE_SKINS.length; i++) assertEquals(bought >= Skin.TREE_BUBBLE_COUNTS[i], game.hasSkin(Skin.TREE_BUBBLE_SKINS[i]), Skin.TREE_BUBBLE_SKINS[i] + " à " + bought);
            for (int i = 0; i < Skin.BUBBLE_LINK_SKINS.length; i++) assertEquals(bought >= Skin.BUBBLE_LINK_COUNTS[i], game.hasSkin(Skin.BUBBLE_LINK_SKINS[i]), Skin.BUBBLE_LINK_SKINS[i] + " à " + bought);
        }
        assertEquals(8, game.treeBubbleStagesBought(), "huit paliers portent des bulles");
        for (Skin skin : Skin.TREE_FANTASIES) assertTrue(game.hasSkin(skin), skin.label());
        // Le défi final fini : le premier remplissage, les suivants par les univers formés (onze ici : tous).
        for (Skin skin : Skin.FINALES) assertFalse(game.hasSkin(skin), skin.label());
        game.state().setGameFinished(true);
        game.tick(1);
        for (Skin skin : Skin.FINALES) assertTrue(game.hasSkin(skin), skin.label());
        assertEquals(SkinVisual.TREE_GROUND, Skin.GROUND_WATER.visual());
        assertEquals(SkinVisual.DIMENSION_FRAME, Skin.FRAME_BOOK.visual());
        assertTrue(Skin.DIM_PALETTE_SAFE.isFree());
    }

    /**
     * Version 0.198 : le profil, les succès et les statistiques. Les styles des avatars, les tuiles et les succès non
     * obtenus suivent les succès obtenus (le dernier palier : tous ceux du catalogue) ; les albums les entrées trouvées puis
     * les albums complets ; les courbes les heures de jeu ; la frise les univers formés ; la palette des courbes est libre.
     */
    @Test
    void leProfilLesSuccesEtLesStatistiquesOntLeursConditions() {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        game.tick(1);
        game.tick(1);
        for (Skin skin : Skin.values()) {
            if (skin.ordinal() < Skin.AVATAR_LINE.ordinal() || skin.ordinal() >= Skin.TOAST_POSTIT.ordinal()) continue;
            assertEquals(skin.isFree(), game.hasSkin(skin), "deux univers formés, rien d'autre : " + skin);
            assertEquals(skin.visual() == SkinVisual.CHART_PALETTE, skin.isFree(), "libre : " + skin);
        }
        // Les succès : un à un, les paliers tombent ; le dernier demande tout le catalogue.
        java.util.List<idle.core.achievements.Achievement> catalog = new java.util.ArrayList<>(game.achievements());
        assertTrue(catalog.size() > 90, "le dernier palier chiffré, 90, reste sous le catalogue : " + catalog.size());
        int got = 0;
        for (idle.core.achievements.Achievement achievement : catalog) {
            if (!game.hasAchievement(achievement.id())) game.state().addAchievement(achievement.id());
            got = game.achievementCount();
            game.tick(1);
            for (int i = 0; i < Skin.AVATAR_STYLES.length; i++) {
                int needed = Skin.AVATAR_STYLE_ACHIEVEMENTS[i] == Skin.ALL_ACHIEVEMENTS ? catalog.size() : Skin.AVATAR_STYLE_ACHIEVEMENTS[i];
                assertEquals(got >= needed, game.hasSkin(Skin.AVATAR_STYLES[i]), Skin.AVATAR_STYLES[i] + " à " + got + " succès");
            }
            for (int i = 0; i < Skin.TILES.length; i++) {
                int needed = Skin.TILE_ACHIEVEMENTS[i] == Skin.ALL_ACHIEVEMENTS ? catalog.size() : Skin.TILE_ACHIEVEMENTS[i];
                assertEquals(got >= needed, game.hasSkin(Skin.TILES[i]), Skin.TILES[i] + " à " + got + " succès");
            }
            for (int i = 0; i < Skin.LOCKED_SKINS.length; i++) {
                assertEquals(got >= Skin.LOCKED_ACHIEVEMENTS[i], game.hasSkin(Skin.LOCKED_SKINS[i]), Skin.LOCKED_SKINS[i] + " à " + got + " succès");
            }
        }
        assertEquals(catalog.size(), got);
        assertTrue(game.hasSkin(Skin.AVATAR_STAINED) && game.hasSkin(Skin.TILES_STAMPS), "tous les succès");
        // Les albums : cinquante entrées pour l'illustré, puis un skin par album complet.
        assertEquals(0, game.albumEntriesFound());
        assertEquals(0, game.albumsComplete());
        int found = 0;
        for (idle.core.sky.Body body : game.bodies()) {
            game.state().noteFound("bodies", body.id(), 1);
            found++;
            game.tick(1);
            assertEquals(found, game.albumEntriesFound());
            assertEquals(found >= Skin.ALBUM_ENTRIES, game.hasSkin(Skin.ALBUM_ILLUSTRATED), found + " entrées");
        }
        assertEquals(1, game.albumsComplete(), "les astres au complet");
        assertTrue(game.hasSkin(Skin.ALBUM_STICKERS));
        assertFalse(game.hasSkin(Skin.ALBUM_STAMPS));
        for (idle.core.sky.Assembly assembly : game.assemblies()) {
            game.state().noteFound("assemblies", assembly.id(), 1);
            found++;
        }
        game.tick(1);
        assertEquals(2, game.albumsComplete());
        assertTrue(game.hasSkin(Skin.ALBUM_ILLUSTRATED) && game.hasSkin(Skin.ALBUM_STAMPS), found + " entrées, deux albums");
        assertFalse(game.hasSkin(Skin.ALBUM_CARDS));
        for (idle.core.molecules.Molecule molecule : game.molecules()) game.state().noteFound("molecules", molecule.id(), 1);
        game.tick(1);
        assertEquals(3, game.albumsComplete());
        assertTrue(game.hasSkin(Skin.ALBUM_CARDS));
        assertFalse(game.hasSkin(Skin.ALBUM_HERBARIUM));
        for (idle.core.molecules.Molecule molecule : game.molecules()) {
            if (molecule.hasState()) game.state().noteFound("substances", molecule.id(), 1);
        }
        game.tick(1);
        assertEquals(4, game.albumsComplete());
        assertTrue(game.hasSkin(Skin.ALBUM_HERBARIUM), "les quatre albums");
        // Les courbes : les heures de jeu.
        for (int i = 0; i < Skin.CHARTS.length; i++) {
            game.state().setTimePlayed(Skin.CHART_HOURS[i] * 3600.0 - 1);
            game.tick(1);
            assertFalse(game.hasSkin(Skin.CHARTS[i]), "une seconde avant : " + Skin.CHARTS[i]);
            game.state().setTimePlayed(Skin.CHART_HOURS[i] * 3600.0);
            game.tick(1);
            assertTrue(game.hasSkin(Skin.CHARTS[i]), Skin.CHARTS[i].label());
        }
        // La frise : les univers formés.
        for (int universes = 2; universes <= 7; universes++) {
            while (game.universes() < universes) assertTrue(game.testAddUniverse());
            game.tick(1);
            for (int i = 0; i < Skin.TIMELINES.length; i++) {
                assertEquals(universes >= Skin.TIMELINE_UNIVERSES[i], game.hasSkin(Skin.TIMELINES[i]), Skin.TIMELINES[i] + " à " + universes + " univers");
            }
        }
        assertTrue(Skin.CHART_NEON.isFree());
        assertEquals(SkinVisual.ACHIEVEMENT_LOCKED, Skin.LOCKED_PUZZLE.visual());
        assertEquals("Tous les succès obtenus", Skin.TILES_STAMPS.source());
    }

    /**
     * Version 0.199 : la fenêtre autour du jeu. Les notifications suivent celles affichées depuis l'ouverture (compte tenu
     * par la fenêtre, non sauvegardé) ; les panneaux les heures de jeu ; la transition les univers formés ; l'écran de fin
     * et le bouton de démarrage la partie finie puis les univers formés ; le curseur un succès d'un onglet chacun.
     */
    @Test
    void lesNotificationsLesPanneauxLaTransitionLaFinLeDemarrageEtLeCurseurOntLeursConditions() {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        game.tick(1);
        // Les notifications : le compte ne descend jamais, et n'est pas écrit dans la sauvegarde.
        for (int i = 0; i < Skin.TOASTS.length; i++) {
            game.noteNotificationsShown(Skin.TOAST_NOTIFICATIONS[i] - 1);
            game.tick(1);
            assertFalse(game.hasSkin(Skin.TOASTS[i]), Skin.TOASTS[i].label() + " avant son compte");
            game.noteNotificationsShown(Skin.TOAST_NOTIFICATIONS[i]);
            game.noteNotificationsShown(0);
            assertEquals(Skin.TOAST_NOTIFICATIONS[i], game.notificationsShown());
            game.tick(1);
            assertTrue(game.hasSkin(Skin.TOASTS[i]), Skin.TOASTS[i].label());
        }
        // Les panneaux : les heures de jeu.
        for (int i = 0; i < Skin.PANELS.length; i++) {
            game.state().setTimePlayed(Skin.PANEL_HOURS[i] * 3600.0 - 1);
            game.tick(1);
            assertFalse(game.hasSkin(Skin.PANELS[i]), Skin.PANELS[i].label() + " avant son heure");
            game.state().setTimePlayed(Skin.PANEL_HOURS[i] * 3600.0);
            game.tick(1);
            assertTrue(game.hasSkin(Skin.PANELS[i]), Skin.PANELS[i].label());
        }
        // Le curseur : un succès chacun, d'un onglet différent.
        assertEquals(Skin.CURSORS.length, Skin.CURSOR_ACHIEVEMENTS.length);
        for (int i = 0; i < Skin.CURSORS.length; i++) {
            String id = Skin.CURSOR_ACHIEVEMENTS[i];
            assertTrue(game.achievements().stream().anyMatch(achievement -> achievement.id().equals(id)), id + " existe");
            assertFalse(game.hasSkin(Skin.CURSORS[i]), Skin.CURSORS[i].label() + " avant son succès");
            game.state().addAchievement(Skin.CURSOR_ACHIEVEMENTS[i]);
            game.tick(1);
            assertTrue(game.hasSkin(Skin.CURSORS[i]), Skin.CURSORS[i].label());
        }
        // La transition : un skin par univers formé, du premier au quatrième ; déjà deux ici.
        for (int i = 0; i < Skin.TRANSITIONS.length; i++) assertEquals(i < 2, game.hasSkin(Skin.TRANSITIONS[i]), Skin.TRANSITIONS[i].label() + " à 2 univers");
        // La fin et le démarrage : rien tant que la partie n'est pas finie, même à onze univers.
        for (int universes = 3; universes <= 11; universes++) {
            assertTrue(game.testAddUniverse());
            game.tick(1);
            for (int i = 0; i < Skin.TRANSITIONS.length; i++) assertEquals(universes >= i + 1, game.hasSkin(Skin.TRANSITIONS[i]), Skin.TRANSITIONS[i].label() + " à " + universes);
            for (Skin skin : Skin.ENDINGS) assertFalse(game.hasSkin(skin), skin.label() + " sans fin de partie");
            for (Skin skin : Skin.START_BUTTONS) assertFalse(game.hasSkin(skin), skin.label() + " sans fin de partie");
        }
        game.state().setGameFinished(true);
        game.tick(1);
        for (Skin skin : Skin.ENDINGS) assertTrue(game.hasSkin(skin), skin.label());
        for (Skin skin : Skin.START_BUTTONS) assertTrue(game.hasSkin(skin), skin.label());
        // Une partie finie à huit univers : le premier skin de chaque, pas les suivants.
        Game young = GameTestSupport.universe(8);
        young.state().setSkinsOpen(true);
        young.state().setGameFinished(true);
        young.tick(1);
        for (int i = 0; i < Skin.ENDINGS.length; i++) assertEquals(i == 0, young.hasSkin(Skin.ENDINGS[i]), Skin.ENDINGS[i].label() + " à 8 univers");
        for (int i = 0; i < Skin.START_BUTTONS.length; i++) assertEquals(i == 0, young.hasSkin(Skin.START_BUTTONS[i]), Skin.START_BUTTONS[i].label() + " à 8 univers");
        assertTrue(young.testAddUniverse());
        young.tick(1);
        assertTrue(young.hasSkin(Skin.START_SEED), "la graine à neuf univers");
        assertFalse(young.hasSkin(Skin.ENDING_CERTIFICATE), "le certificat attend dix univers");
        assertEquals(SkinVisual.TOAST, Skin.TOAST_SCROLL.visual());
        assertEquals("5 h de jeu", Skin.PANEL_BOOK.source());
    }

    /** Version 0.200 : les sons. Les banques suivent les heures de jeu, le son du Big Bang les Big Bangs, le métronome le seuil de la boucle puis les univers. */
    @Test
    void lesBanquesDeSonsLeBigBangEtLeMetronomeOntLeursConditions() {
        Game game = GameTestSupport.universe(2);
        game.state().setSkinsOpen(true);
        game.tick(1);
        assertTrue(game.hasSkin(Skin.SOUND_SILENCE), "le silence partiel est libre");
        assertTrue(Skin.SOUND_SILENCE.isFree());
        for (int i = 0; i < Skin.SOUND_BANKS.length; i++) {
            game.state().setTimePlayed(Skin.SOUND_BANK_HOURS[i] * 3600.0 - 1);
            game.tick(1);
            assertFalse(game.hasSkin(Skin.SOUND_BANKS[i]), Skin.SOUND_BANKS[i].label() + " avant son heure");
            game.state().setTimePlayed(Skin.SOUND_BANK_HOURS[i] * 3600.0);
            game.tick(1);
            assertTrue(game.hasSkin(Skin.SOUND_BANKS[i]), Skin.SOUND_BANKS[i].label());
        }
        for (int i = 0; i < Skin.BANG_SOUNDS.length; i++) {
            game.state().setBigBangs(Skin.BANG_SOUND_BANGS[i] - 1);
            game.tick(1);
            assertFalse(game.hasSkin(Skin.BANG_SOUNDS[i]), Skin.BANG_SOUNDS[i].label() + " avant son Big Bang");
            game.state().setBigBangs(Skin.BANG_SOUND_BANGS[i]);
            game.tick(1);
            assertTrue(game.hasSkin(Skin.BANG_SOUNDS[i]), Skin.BANG_SOUNDS[i].label());
        }
        // Le métronome : rien sans le seuil de la boucle, même à huit univers ; le seuil atteint, un skin de plus par univers à partir du sixième.
        for (int universes = 3; universes <= 8; universes++) assertTrue(game.testAddUniverse());
        game.tick(1);
        for (Skin skin : Skin.METRONOMES) assertFalse(game.hasSkin(skin), skin.label() + " sans le seuil de la boucle");
        Game loop = GameTestSupport.universe(5);
        loop.state().setSkinsOpen(true);
        loop.state().setLoopShardsEarned(GameUniverse.LOOP_THRESHOLD);
        loop.tick(1);
        assertTrue(loop.isDimensionThresholdReached(5));
        for (int i = 0; i < Skin.METRONOMES.length; i++) assertEquals(i == 0, loop.hasSkin(Skin.METRONOMES[i]), Skin.METRONOMES[i].label() + " à 5 univers");
        for (int universes = 6; universes <= 8; universes++) {
            assertTrue(loop.testAddUniverse());
            loop.tick(1);
            for (int i = 0; i < Skin.METRONOMES.length; i++) {
                assertEquals(i == 0 || universes >= Skin.FIRST_METRONOME_UNIVERSE + i - 1, loop.hasSkin(Skin.METRONOMES[i]), Skin.METRONOMES[i].label() + " à " + universes);
            }
        }
        assertEquals(SkinVisual.SOUND_BANK, Skin.SOUND_WOOD.visual());
        assertEquals(SkinVisual.BIG_BANG_SOUND, Skin.BANG_SOUND_ORGAN.visual());
        assertEquals(SkinVisual.METRONOME, Skin.METRONOME_HEART.visual());
        assertEquals("1 h de jeu", Skin.SOUND_WOOD.source());
    }
}

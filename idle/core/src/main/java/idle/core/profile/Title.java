package idle.core.profile;

import idle.core.Game;
import idle.core.collider.Quark;
import idle.core.sky.Body;
import idle.core.sky.Cosmos;
import java.util.function.Predicate;

/**
 * Les titres du joueur (version 0.147) : un nom qui dit jusqu'où il est allé. Chacun s'obtient une fois pour toutes,
 * dès que sa condition est vraie, et reste acquis : ni le Big Bang, ni l'univers suivant, ni « Recommencer de zéro »
 * ne le reprennent, comme les records. Le joueur porte celui qu'il veut parmi ceux qu'il a ; ils ne donnent aucun
 * bonus.
 *
 * <p>L'ordre est celui de la partie, du premier générateur à l'arbre-monde ; les nouveaux s'ajoutent où ils tombent
 * (ils sont sauvegardés par leur nom, pas par leur rang).
 */
public enum Title {
    SPARK("Étincelle", "Créer le premier générateur", game -> game.generatorCount() > 0),
    GATHERER("Cueilleur de particules", "Avoir dix générateurs en même temps", game -> game.generatorCount() >= 10),
    APPRENTICE_ALCHEMIST("Apprenti alchimiste", "Faire la première fusion", game -> game.stats().fusions() > 0),
    ALCHEMIST("Alchimiste", "Faire cent fusions", game -> game.stats().fusions() >= 100),
    TABLE_READER("Lecteur du tableau", "Découvrir dix éléments", game -> game.discoveredElements() >= 10),
    ELEMENT_MASTER("Maître des éléments", "Découvrir les 118 éléments", game -> game.isPeriodicTableComplete()),
    SHADOW_KEEPER("Gardien de l'ombre", "Gagner la première matière noire", game -> game.isDarkMatterUnlocked()),
    CHALLENGER("Briseur de défis", "Réussir les dix défis", game -> game.baseChallengesCompleted() >= 10),
    QUARK_HUNTER("Chasseur de quarks", "Obtenir la première collision", game -> game.state().collisionRuns() > 0),
    TOP_LORD("Seigneur du top", "Obtenir un quark top", game -> game.quarks(Quark.TOP) > 0),
    BANG_MAKER("Faiseur de Big Bang", "Déclencher le premier Big Bang", game -> game.bigBangs() > 0),
    MOLECULE_BLOWER("Souffleur de molécules", "Créer mille molécules", game -> game.moleculesCreated() >= 1_000),
    COMET_CHASER("Chasseur de comètes", "Saisir dix comètes", game -> game.cometsCaught() >= 10),
    STAR_SMITH("Forgeron d'étoiles", "Former une étoile", game -> game.bodiesFormed(Body.Tier.STAR) > 0),
    PLANET_GARDENER("Jardinier des planètes", "Compléter un système solaire", game -> game.solarSystemsComplete() > 0),
    VOID_WATCHER("Veilleur des trous noirs", "Former un trou noir", game -> game.bodiesFormed(Body.Tier.BLACK_HOLE) > 0),
    GALAXY_ARCHITECT("Architecte de galaxies", "Former la galaxie", game -> game.hasGalaxy()),
    CLUSTER_BUILDER("Bâtisseur d'amas", "Former l'amas de galaxies", game -> game.hasCosmos(Cosmos.CLUSTER)),
    UNIVERSE_CREATOR("Créateur d'univers", "Former un univers", game -> game.universes() >= 1),
    MULTIVERSE_WEAVER("Tisseur de multivers", "Former un deuxième univers", game -> game.universes() >= 2),
    LINE_WALKER("Arpenteur de la ligne", "Atteindre le seuil de la ligne", game -> game.isDimensionThresholdReached(1)),
    PLANE_WANDERER("Voyageur du plan", "Atteindre le seuil du plan", game -> game.isDimensionThresholdReached(2)),
    VOLUME_DIVER("Plongeur du volume", "Atteindre le seuil du volume", game -> game.isDimensionThresholdReached(3)),
    TIME_KEEPER("Gardien de la durée", "Atteindre le seuil de la durée", game -> game.isDimensionThresholdReached(4)),
    LOOP_DANCER("Danseur de la boucle", "Atteindre le seuil de la boucle", game -> game.isDimensionThresholdReached(5)),
    BRANE_SURFER("Surfeur de la brane", "Atteindre le seuil de la brane", game -> game.isDimensionThresholdReached(6)),
    CLICK_VIRTUOSO("Virtuose du clic", "Atteindre le seuil du clicker", game -> game.isDimensionThresholdReached(7)),
    GRID_SEEKER("Arpenteur de la grille", "Atteindre le seuil de la grille", game -> game.isDimensionThresholdReached(8)),
    VOID_TAMER("Dompteur du vide", "Ouvrir le vide", game -> game.isVoidUnlocked()),
    STAR_EATER("Mangeur d'étoiles", "Ouvrir le trou noir du vide", game -> game.isVoidUnlocked() && game.isVoidHoleOpen()),
    GLUON_WEAVER("Tisseur de gluons", "Assembler un premier hadron", game -> game.hadronsMade() > 0),
    EXOTIC_HUNTER("Chasseur d'exotiques", "Assembler une particule exotique",
            game -> java.util.Arrays.stream(idle.core.collider.Hadron.values()).anyMatch(h -> h.isExotic() && game.hadrons(h) > 0)),
    SEED_GARDENER("Jardinier cosmique", "Faire monter la graine d'univers au palier 5", game -> game.seedStage() >= 5),
    WORLD_TREE_KEEPER("Gardien de l'arbre-monde", "Faire monter la graine au dernier palier",
            game -> game.seedStage() >= Game.SEED_STAGES),
    SKIN_COLLECTOR("Collectionneur d'apparences", "Débloquer dix skins", game -> game.skinsUnlocked() >= 10),
    PATIENT_SPIRIT("Esprit patient", "Jouer vingt-quatre heures", game -> game.state().timePlayed() >= 24 * 3600),
    COSMIC_VETERAN("Vétéran du cosmos", "Jouer cent heures", game -> game.state().timePlayed() >= 100 * 3600),
    ETERNAL("Éternel", "Jouer mille heures", game -> game.state().timePlayed() >= 1_000 * 3600);

    private final String label;
    private final String condition;
    private final Predicate<Game> reached;

    Title(String label, String condition, Predicate<Game> reached) {
        this.label = label;
        this.condition = condition;
        this.reached = reached;
    }

    /** Le titre affiché : « Apprenti alchimiste ». */
    public String label() {
        return label;
    }

    /** Ce qu'il faut faire pour l'obtenir, en quelques mots. */
    public String condition() {
        return condition;
    }

    /** Vrai si la partie remplit sa condition en ce moment. */
    public boolean isReachedBy(Game game) {
        return reached.test(game);
    }

    /** Le titre de ce nom, ou {@code null} s'il n'existe plus (une sauvegarde d'une autre version). */
    public static Title named(String name) {
        for (Title title : values()) if (title.name().equals(name)) return title;
        return null;
    }
}

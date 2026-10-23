package idle.core;

import idle.core.achievements.Achievement;
import idle.core.bigbang.SpaceUpgrade;
import idle.core.bigbang.SpaceUpgrades;
import idle.core.darkmatter.DarkAutomation;
import idle.core.darkmatter.DarkAutomations;
import idle.core.darkmatter.DarkUpgrade;
import idle.core.elements.ElementBonuses;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.molecules.Molecules;
import idle.core.sky.Assemblies;
import idle.core.sky.Assembly;
import idle.core.sky.Bodies;
import idle.core.sky.Body;
import idle.core.upgrades.Automation;
import idle.core.upgrades.Upgrade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Le socle du jeu : ses constantes, ses catalogues et son état.
 */
public abstract class GameBase {

    /** Le jeu entier : chaque partie de la chaîne est un {@link Game}. */
    final Game self() {
        return (Game) this;
    }

    GameBase(GameState state, List<Upgrade> catalog, List<Automation> automationCatalog, List<DarkUpgrade> darkCatalog, List<Achievement> achievementCatalog, RandomGenerator random) {
        this.state = state;
        this.random = random;
        for (Achievement achievement : achievementCatalog) {
            if (achievements.put(achievement.id(), achievement) != null) {
                throw new IllegalArgumentException("Succès en double : " + achievement.id());
            }
        }
        for (Upgrade upgrade : catalog) {
            if (upgrades.put(upgrade.id(), upgrade) != null) {
                throw new IllegalArgumentException("Amélioration en double : " + upgrade.id());
            }
        }
        for (Automation automation : automationCatalog) {
            if (automation.kind() == Automation.Kind.UPGRADE && !upgrades.containsKey(automation.upgradeId())) {
                throw new IllegalArgumentException("Automatisme sans amélioration : " + automation.upgradeId());
            }
            if (automations.put(automation.id(), automation) != null) {
                throw new IllegalArgumentException("Automatisme en double : " + automation.id());
            }
        }
        for (DarkAutomation automation : DarkAutomations.DEFAULT) {
            darkAutomations.put(automation.id(), automation);
        }
        for (SpaceUpgrade upgrade : SpaceUpgrades.DEFAULT) {
            if (upgrade.requires() != null && !spaceUpgrades.containsKey(upgrade.requires())) {
                throw new IllegalArgumentException("Amélioration d'espace sans celle dont elle dépend : " + upgrade.id());
            }
            if (spaceUpgrades.put(upgrade.id(), upgrade) != null) {
                throw new IllegalArgumentException("Amélioration d'espace en double : " + upgrade.id());
            }
        }
        for (Molecule molecule : Molecules.DEFAULT) {
            if (molecules.put(molecule.id(), molecule) != null) {
                throw new IllegalArgumentException("Molécule en double : " + molecule.id());
            }
        }
        for (Assembly assembly : Assemblies.DEFAULT) {
            if (assembly.ingredients().size() < 2) {
                throw new IllegalArgumentException("Un assemblage réunit au moins deux sortes de molécules : " + assembly.id());
            }
            for (String ingredient : assembly.ingredients().keySet()) {
                Molecule molecule = molecules.get(ingredient);
                if (molecule == null) throw new IllegalArgumentException(assembly.id() + " demande une molécule inconnue : " + ingredient);
                if (!molecule.hasState()) throw new IllegalArgumentException(assembly.id() + " demande une molécule qui ne se rassemble pas : " + ingredient);
            }
            if (assemblies.put(assembly.id(), assembly) != null) {
                throw new IllegalArgumentException("Assemblage en double : " + assembly.id());
            }
        }
        for (Body body : Bodies.DEFAULT) {
            // Un astre part d'astres plus petits, déjà décrits : le catalogue va donc du plus petit au plus grand.
            for (String needed : body.bodies()) {
                Body smaller = bodies.get(needed);
                if (smaller == null) throw new IllegalArgumentException(body.id() + " demande un astre inconnu ou décrit après lui : " + needed);
                if (smaller.tier().compareTo(body.tier()) >= 0) {
                    throw new IllegalArgumentException(body.id() + " demande un astre qui n'est pas plus petit que lui : " + needed);
                }
            }
            for (String needed : body.assemblies()) {
                if (!assemblies.containsKey(needed)) throw new IllegalArgumentException(body.id() + " demande un assemblage inconnu : " + needed);
                // Un assemblage entre dans un seul astre : c'est ce qui permet à l'astre de s'en faire un corps.
                if (bodyByAssembly.put(needed, body) != null) {
                    throw new IllegalArgumentException("L'assemblage " + needed + " est utilisé par deux astres, dont " + body.id());
                }
            }
            for (String needed : body.molecules().keySet()) {
                if (!molecules.containsKey(needed)) throw new IllegalArgumentException(body.id() + " demande une molécule inconnue : " + needed);
            }
            if (bodies.put(body.id(), body) != null) throw new IllegalArgumentException("Astre en double : " + body.id());
        }
        for (DarkUpgrade dark : darkCatalog) {
            if (dark.requires() != null && dark.requiredFork() == null && !dark.isLink() && !darkUpgrades.containsKey(dark.requires())) {
                throw new IllegalArgumentException("Amélioration de matière noire sans sa case du dessus : " + dark.id());
            }
            if (dark.requiredFork() != null && darkCatalog.stream().noneMatch(other -> dark.requiredFork().equals(other.fork()))) {
                throw new IllegalArgumentException("Amélioration de matière noire après une bifurcation inconnue : " + dark.id());
            }
            for (String linked : dark.linkedIds()) {
                if (darkCatalog.stream().noneMatch(other -> other.id().equals(linked) && other.isFork())) {
                    throw new IllegalArgumentException("Liaison de matière noire vers une voie inconnue : " + dark.id() + " → " + linked);
                }
            }
            if (darkUpgrades.put(dark.id(), dark) != null) {
                throw new IllegalArgumentException("Amélioration de matière noire en double : " + dark.id());
            }
        }
    }


    /**
     * Le tempo du jeu : ce par quoi la vitesse de départ des générateurs a été multipliée pour que
     * la première explosion vienne en trois heures au lieu de vingt-quatre. Ce qui se mesurait à
     * l'aune de la production (la croissance de la matière noire, le seuil du rendement de fusion)
     * se compte en production de départ : ces règles gardent ainsi l'allure qu'elles avaient.
     */
    public static final double TEMPO = 12;

    /** Vitesse de départ d'un générateur : deux particules par seconde. */
    public static final BigNum BASE_SPEED = BigNum.of(0.25 * TEMPO);
    /**
     * Le premier élan (version 0.095, par paliers depuis la 0.096) : dans la toute première partie,
     * avant la première explosion, la création prend son élan peu à peu. Elle va
     * {@code FIRST_RUN_PACE} fois moins vite jusqu'au premier atome (une heure environ), puis
     * {@link #FIRST_ATOMS_PACE} fois moins vite jusqu'au cinquième atome créé (deux heures), puis
     * {@link #FIRST_GAME_PACE} fois moins vite jusqu'au trentième, où s'ouvrent l'automatisation et le
     * tableau périodique : de là, elle a tout son élan. Rien ne ralentit plus ensuite, ni après une
     * explosion, ni dans l'univers suivant. Seule une partie neuve repasse par là.
     */
    public static final double FIRST_RUN_PACE = 13;
    public static final double FIRST_ATOMS_PACE = 9;
    public static final double FIRST_GAME_PACE = 2.8;
    /** Atomes créés où la création passe au palier suivant de son élan : 1, 5, puis 30. */
    public static final int FIRST_ATOMS_UNTIL = 5;
    /**
     * Les tests unitaires qui vérifient une règle sur une partie neuve, sans la regarder durer, coupent le
     * premier élan avec la propriété système {@code idle.sansPremierElan}, et avec lui la matière noire réduite
     * du premier cycle ({@link #FIRST_CYCLE_DARK_MATTER}) ; le jeu, les robots et les tests de rythme les gardent.
     */
    private static final boolean NO_FIRST_RUN_PACE = Boolean.getBoolean("idle.sansPremierElan");
    double firstRunPace = NO_FIRST_RUN_PACE ? 1 : FIRST_RUN_PACE;

    /** Atomes gagnés à chaque fusion, avant les bonus. */
    public static final BigNum ATOMS_PER_FUSION = BigNum.ONE;

    /**
     * Nombre maximal d'atomes qu'on peut posséder en même temps : le nombre d'éléments du
     * tableau périodique. Arrivé là, il faut en dépenser pour pouvoir fusionner de nouveau.
     */
    public static final BigNum MAX_ATOMS = BigNum.of(118);

    /**
     * Particules à avoir en main pour le premier Big Bang : 10¹⁹⁰⁰⁰⁰ depuis la version 0.125 (10¹⁶⁵³⁰ en 0.124,
     * 10³⁸⁰⁰⁰ de la 0.097 à la 0.123). Réglé sur le bot « Speedrunner » parti de zéro (idle/bots,
     * FreshUniverseAudit, profil 1) : une fois les dix défis faits, ses explosions s'enchaînent, la matière noire
     * gagnée monte d'environ 40 000 par heure et la production de quelque 20 000 ordres de grandeur par heure ;
     * ce seuil fait durer le deuxième acte six heures.
     */
    public static final BigNum BIG_BANG_PARTICLES = BigNum.of(1, 190000);

    /**
     * Particules des Big Bangs suivants, au troisième acte : 10⁵⁰⁰⁰⁰ depuis la version 0.125 (10¹³⁰⁵ en 0.124,
     * 10³⁰⁰⁰ avant). Chaque cycle du troisième acte refait un deuxième acte en raccourci : 40 à 90 minutes pour le
     * Speedrunner jusqu'au cinquième Big Bang.
     */
    public static final BigNum BIG_BANG_PARTICLES_LATER = BigNum.of(1, 50000);

    /** Les particules que demande le prochain Big Bang : beaucoup pour le premier, moins pour les suivants. */
    public BigNum bigBangParticles() {
        BigNum need = state.bigBangs() == 0 ? BIG_BANG_PARTICLES : BIG_BANG_PARTICLES_LATER;
        // La barre haute (version 0.117) : une fois et demie plus d'ordres de grandeur.
        if (raisedBar()) need = BigNum.pow10(need.log10() * idle.core.bigbang.BangChallenge.RAISED_PARTICLES);
        double relief = bigBangCalmRelief();
        return relief <= 0 ? need : relief >= 1 ? BigNum.ONE : BigNum.pow10(need.log10() * (1 - relief));
    }

    /**
     * Le calme du Big Bang (version 0.114), récompense du défi « Fuite » (une épreuve en 0.114) : tant qu'aucune amélioration
     * n'est achetée, les particules que demande le Big Bang baissent peu à peu, en ordres de grandeur,
     * jusqu'à une seule au bout de {@link #BIG_BANG_CALM_SECONDS}. Une heure sans rien acheter suffit donc
     * toujours pour la condition des particules. La moindre amélioration fait tout repartir.
     */
    public static final double BIG_BANG_CALM_SECONDS = 3600;

    /**
     * Les défis de Big Bang du deuxième univers (version 0.116) : chacun réussi multiplie, en plus de sa récompense,
     * les particules par {@link #BANG_UNIVERSE_PARTICLES}, les molécules de chaque création par
     * {@link #BANG_UNIVERSE_MOLECULES} (version 0.118, à la place de l'espace ×2, qui ralentissait l'amas), la vitesse de
     * maturation des astres par {@link #BANG_UNIVERSE_MATURATION}, celle des sondes par {@link #BANG_UNIVERSE_PROBE_SPEED}
     * et leurs récompenses par {@link #BANG_UNIVERSE_PROBE_REWARDS}. Les bonus se multiplient d'un défi à l'autre ;
     * les trois réussis, chaque planète peut aussi entrer dans deux systèmes solaires.
     */
    public static final double BANG_UNIVERSE_PARTICLES = 2, BANG_UNIVERSE_MOLECULES = 2, BANG_UNIVERSE_MATURATION = 4,
            // Les sondes, ramenées à ×2 en 0.118 (×4 et ×10 avant).
            BANG_UNIVERSE_PROBE_SPEED = 2, BANG_UNIVERSE_PROBE_REWARDS = 2;

    /** Défis de Big Bang réussis qui comptent pour ces bonus : aucun avant le deuxième univers. */
    public int universeBangChallenges() {
        if (!state.universeFormed()) return 0;
        int done = 0;
        for (idle.core.bigbang.BangChallenge challenge : idle.core.bigbang.BangChallenge.values()) {
            if (state.completedBangChallenges().contains(challenge.name())) done++;
        }
        return done;
    }

    /** La récompense propre d'un défi de Big Bang de la 0.117, lisible des couches basses : {@code factor} s'il est réussi, 1 sinon. */
    double bangCompletedFactor(idle.core.bigbang.BangChallenge challenge, double factor) {
        return state.completedBangChallenges().contains(challenge.name()) ? factor : 1;
    }

    /** Un bonus des défis de Big Bang : {@code base} à la puissance du nombre de défis réussis. */
    public double universeBangFactor(double base) {
        return Math.pow(base, universeBangChallenges());
    }

    /** Vrai une fois les trois défis de Big Bang réussis dans le deuxième univers : une planète peut entrer dans deux systèmes. */
    public boolean planetsTwice() {
        // Les trois défis d'origine (version 0.116) ; ceux de la 0.117 n'y entrent pas.
        if (!state.universeFormed()) return false;
        for (idle.core.bigbang.BangChallenge c : new idle.core.bigbang.BangChallenge[] {idle.core.bigbang.BangChallenge.VOID,
                idle.core.bigbang.BangChallenge.FORGOTTEN, idle.core.bigbang.BangChallenge.BY_HAND})
            if (!state.completedBangChallenges().contains(c.name())) return false;
        return true;
    }

    /** Vrai pendant l'action d'un automatisme (version 0.114) : ses achats ne sont pas ceux du joueur, ils ne rompent pas le calme. */
    boolean automating = false;

    /** La part du seuil de particules déjà effacée par le calme, de 0 à 1 ; 0 sans la récompense. */
    public double bigBangCalmRelief() {
        if (!state.completedChallenges().contains(idle.core.darkmatter.Challenges.LEAK.id())) return 0;
        // Version 0.124 : la barre haute ne se contourne pas en attendant.
        if (raisedBar()) return 0;
        return Math.min(1, state.bangCalm() / BIG_BANG_CALM_SECONDS);
    }

    /**
     * Atomes à avoir en main pour le Big Bang : dix fois ce qu'une partie d'une demi-minute en
     * laisse quand l'arbre vient d'être fini. Il faut laisser tourner une partie, donc couper
     * l'explosion automatique.
     */
    public static final BigNum BIG_BANG_ATOMS = BigNum.of(1, 12);

    /**
     * Matière noire à avoir en réserve, sans la dépenser, pour le Big Bang : une cinquantaine
     * d'explosions à onze matières noires.
     */
    public static final BigNum BIG_BANG_DARK_MATTER = BigNum.of(500);

    /** Vrai pendant le défi de Big Bang « Barre haute » (version 0.117). */
    boolean raisedBar() {
        return idle.core.bigbang.BangChallenge.RAISED.name().equals(state.activeBangChallenge());
    }

    /** Les atomes en main que demande le Big Bang : plus pendant la barre haute. */
    public BigNum bigBangAtoms() {
        return raisedBar() ? BIG_BANG_ATOMS.multiply(idle.core.bigbang.BangChallenge.RAISED_ATOMS) : BIG_BANG_ATOMS;
    }

    /** La matière noire en réserve que demande le Big Bang : plus pendant la barre haute. */
    public BigNum bigBangDarkMatter() {
        return raisedBar() ? BIG_BANG_DARK_MATTER.multiply(idle.core.bigbang.BangChallenge.RAISED_DARK_MATTER) : BIG_BANG_DARK_MATTER;
    }

    /** Nombre de succès qu'il faut avoir obtenus pour le Big Bang, sur les cinquante du jeu. */
    public static final int BIG_BANG_ACHIEVEMENTS = 40;

    /** Espace que l'expansion de la matière ajoute chaque seconde, pour chaque Big Bang déclenché. */
    public static final double SPACE_PER_SECOND = 1;

    /**
     * Espace qu'occupe une molécule, par proton, avant ce que son état y ajoute
     * ({@link Molecule.State#spaceFactor()}) : une molécule de quartz (30 protons, un cristal) en
     * prend 300, une molécule d'eau (10 protons, un liquide) 200, une molécule de dihydrogène
     * (2 protons, un gaz) 400.
     */
    public static final double SPACE_PER_PROTON = 10;

    /** Nombre de molécules qu'une même sorte ne dépasse jamais : de quoi rester loin des limites de la machine. */
    public static final int MAX_MOLECULES = 1_000_000_000;

    /** Nombre de molécules d'une même sorte qu'il faut avoir créées pour les rassembler dans le lieu de leur état. */
    public static final int SUBSTANCE_MOLECULES = 3;

    /**
     * Ce par quoi chaque explosion multiplie la masse du tableau périodique : le plafond d'atomes
     * et le prix maximal d'une synthèse ({@link #atomCap()}). Sans cela, chaque partie demanderait
     * les mêmes 118 éléments au même prix pendant que l'arbre de matière noire multiplie tout, et
     * les parties tomberaient à une minute dès la cinquième explosion.
     */
    public static final double TABLE_WEIGHT_GROWTH = 3;

    /**
     * Nombre d'explosions au-delà duquel le tableau ne s'alourdit plus. Sans cette limite, un
     * joueur qui explose plus vite qu'il ne remplit l'arbre verrait chaque partie durer trois
     * fois plus que la précédente, sans fin. Dix : c'est à ce moment que l'arbre n'apporte plus
     * qu'une case par explosion, et un niveau de plus ferait passer une partie de trois heures
     * et demie à plus de huit.
     */
    public static final int TABLE_WEIGHT_MAX_LEVEL = 10;

    /**
     * Masse du tableau pendant les défis « Présence » et « Absence » (version 0.073) : un tableau très
     * lourd, qu'on ne remplit vite qu'avec les voies du bon style.
     */
    public static final BigNum STYLE_CHALLENGE_WEIGHT = BigNum.of(3, 6);

    /**
     * Masse du tableau pendant les huit premiers défis (version 0.097 ; 1 avant) : ils demandent quelques minutes
     * chacun au lieu de quelques secondes, et s'étalent sur la deuxième demi-heure de la matière noire.
     */
    public static final BigNum CHALLENGE_WEIGHT = BigNum.of(5000);

    /**
     * La matière noire de chaque explosion avant le premier Big Bang (version 0.097) : ×0,8. Le deuxième acte
     * s'étale ainsi vers six heures, au lieu de cinq.
     */
    public static final double FIRST_CYCLE_DARK_MATTER = 0.8;

    /**
     * Nombre d'atomes à avoir créés depuis le début de la partie (dépensés ou non) pour débloquer
     * à la fois l'automatisation et le tableau périodique.
     */
    public static final BigNum UNLOCK_TOTAL_ATOMS = BigNum.of(30);

    /**
     * Prix de la première synthèse d'élément, en atomes. Il double à chaque synthèse
     * (2, 4, 8, 16, 32, 64) jusqu'à être plafonné par {@link #MAX_ATOMS}.
     */
    public static final BigNum SYNTHESIS_BASE_COST = BigNum.of(2);

    /**
     * Délai minimal d'un automatisme, en secondes, quels que soient ses bonus : il n'agit jamais
     * plus de dix fois par seconde. C'est ce qui empêche le jeu de s'emballer en fin de partie,
     * et ce qui le fait tourner à la même vitesse sur toutes les machines.
     */
    public static final double MIN_AUTOMATION_INTERVAL = 0.1;

    /**
     * Tant qu'aucun élément unique n'a été obtenu, la synthèse de ce rang en donne un à coup sûr :
     * la synthèse automatique, qu'il débloque, ne dépend ainsi pas que de la chance.
     */
    public static final int GUARANTEED_UNIQUE_SYNTHESIS = 8;

    /**
     * Ce que les éléments peuvent ajouter, au plus, au gain d'un niveau de vitesse (0.10 = 10 points).
     * Si un niveau de vitesse rapportait plus que la hausse de son prix (+27 %), chaque niveau
     * paierait le suivant et la production s'emballerait sans fin. Avec les maximums d'exemplaires
     * actuels, les éléments ajoutent au plus 4,5 points : ce plafond est un garde-fou, au cas où
     * le tableau périodique changerait.
     */
    public static final double MAX_ELEMENT_SPEED_EXTRA = 0.10;

    /** Ce que chaque groupe de générateurs au-delà du premier ajoute aux atomes d'une fusion (0.1 = +10 %). */
    public static final double FUSION_GROUP_BONUS = 0.10;

    /**
     * Part de la vitesse d'un appui que donne l'appui verrouillé : la matière noire grossit alors
     * seule, depuis n'importe quel onglet, mais moins vite que si le joueur tenait le clic.
     */
    public static final double HOLD_LOCK_SHARE = 0.6;

    /** Ce que chaque palier de taille « particules » multiplie dans les particules de chaque création. */
    public static final double LANDMARK_PARTICLES = 2;
    /** Ce que chaque palier de taille « atomes » ajoute aux atomes de chaque fusion (0.05 = +5 %). */
    public static final double LANDMARK_ATOMS = 0.05;
    /** Ce que chaque palier de taille « croissance » multiplie dans la croissance de la matière noire. */
    public static final double LANDMARK_EXPANSION = 1.1;

    /** Défi « Rouille » : facteur appliqué au délai de tous les automatismes. */
    public static final double RUSTY_FACTOR = 4;
    /** Défi « Rouille » : délai en dessous duquel aucun automatisme ne descend, en secondes. */
    public static final double RUSTY_MIN_INTERVAL = 0.5;
    /** Défi « Fusion lourde » : facteur appliqué au nombre de générateurs qu'il faut pour fusionner. */
    public static final int HEAVY_FUSION_FACTOR = 2;
    /** Défi « Particules volatiles » : part des particules possédées qui disparaît chaque seconde. */
    public static final double VOLATILE_LOSS_PER_SECOND = 0.50;
    /**
     * Défi « Désintégration » : chance qu'a chaque exemplaire de disparaître, par seconde. À
     * 0,02, sa demi-vie est de 35 secondes : un tableau complet de 811 exemplaires en perd seize
     * par seconde, et il faut synthétiser plus vite que cela pour le finir.
     */
    public static final double DECAY_RATE = 0.02;

    /** Récompense de « Rouille » : le délai des automatismes ordinaires est divisé par ce nombre. */
    public static final double REWARD_AUTOMATION_DIVISOR = 1.25;
    /** Récompense de « Fusion lourde » : ce que vaut alors la prime de groupe, à la place de {@link #FUSION_GROUP_BONUS}. */
    public static final double REWARD_GROUP_BONUS = 0.20;
    /** Récompense de « Particules volatiles » : ce qui s'ajoute au multiplicateur de chaque palier de vitesse. */
    public static final double REWARD_MILESTONE_EXTRA = 1;
    /** Récompense d'« Amnésie » : les améliorations payées en atomes coûtent ce nombre de fois moins. */
    public static final double REWARD_ATOM_UPGRADE_DIVISOR = 2;
    /** Récompense d'« Éléments inertes » : la force de chaque élément est multipliée par ce nombre. */
    public static final double REWARD_ELEMENT_STRENGTH = 1.15;
    /** Récompense de « Désintégration » : éléments en plus à chaque synthèse. */
    public static final int REWARD_EXTRA_ELEMENTS = 1;

    /** Ce que chaque succès obtenu ajoute aux particules de chaque création (0.01 = +1 %). Les succès s'additionnent. */
    public static final double ACHIEVEMENT_PARTICLES = 0.01;
    /** Secondes de jeu entre deux vérifications des succès : inutile de les regarder à chaque image. */
    static final double ACHIEVEMENT_CHECK_SECONDS = 1;

    /** Matière noire laissée par chaque explosion du tableau périodique. */
    public static final BigNum DARK_MATTER_PER_EXPLOSION = BigNum.ONE;

    /** Taille de départ de la matière noire, en mètres : celle d'un proton. */
    public static final BigNum DARK_MATTER_START_SIZE = BigNum.of(1, -15);

    /**
     * Élan que chaque unité de matière noire donne à la croissance de sa taille, par seconde
     * d'appui, avant le facteur d'avancement (0.003 = +0,3 % par seconde tant qu'elle a la taille
     * d'un proton). Voir {@link #darkMatterGrowthPerSecond()}. Réglé pour des parties d'un jour :
     * l'année-lumière est atteinte vers la cinquième explosion.
     */
    public static final double DARK_MATTER_GROWTH_PER_UNIT = 0.003;

    /**
     * Résistance de la matière noire à sa propre croissance : la vitesse est divisée par sa
     * taille (comptée en protons) élevée à cette puissance. Avec 0,1, elle est divisée par deux
     * chaque fois que la taille est multipliée par mille. C'est ce qui étale la croissance, du
     * proton à l'univers, sur toute la durée du jeu : doubler l'élan (matière noire, arbre,
     * avancement) ou le temps d'appui fait gagner trois ordres de grandeur, pas davantage.
     */
    public static final double DARK_MATTER_RESISTANCE = 0.1;

    /** Au-delà de ce nombre de particules par tick, on ne les compte plus une par une. */
    static final long WHOLE_PARTICLES_MAX_EXPONENT = 15;

    /**
     * Pas de calcul maximal, en secondes, quand un bonus dépend du temps : un long tick
     * (retour après une absence) est alors découpé pour que le bonus monte progressivement.
     */
    static final double MAX_STEP = 1.0;

    /** Nombre maximal d'actions d'un automatisme par pas de calcul : un garde-fou contre les boucles sans fin. */
    static final int MAX_AUTO_ACTIONS_PER_STEP = 100;

    /** Nombre maximal de niveaux achetés par un achat groupé : un garde-fou, jamais atteint en jeu. */
    public static final int MAX_BULK = 10_000;

    final GameState state;
    final Map<String, Upgrade> upgrades = new LinkedHashMap<>();
    final Map<String, Automation> automations = new LinkedHashMap<>();
    final Map<String, DarkUpgrade> darkUpgrades = new LinkedHashMap<>();
    final Map<String, Molecule> molecules = new LinkedHashMap<>();
    final List<Molecule> moleculeList = Molecules.DEFAULT;
    final Map<String, SpaceUpgrade> spaceUpgrades = new LinkedHashMap<>();
    final Map<String, Assembly> assemblies = new LinkedHashMap<>();
    final Map<String, Body> bodies = new LinkedHashMap<>();
    /** Pour chaque assemblage : l'astre qui est fait de lui. */
    final Map<String, Body> bodyByAssembly = new java.util.HashMap<>();
    /** Les catalogues en listes, faites une fois : ils ne changent pas après la construction, et sont demandés à chaque image. */
    List<SpaceUpgrade> spaceUpgradeList;
    List<Assembly> assemblyList;
    List<Body> bodyList;
    /** Le nombre de molécules rassemblées dans chaque état, recalculé seulement quand leur liste change. */
    final Map<Molecule.State, Long> gatheredByState = new java.util.EnumMap<>(Molecule.State.class);
    int gatheredByStateVersion = -1;
    /** L'espace occupé par les molécules, recalculé seulement quand leur liste change. */
    BigNum occupiedSpace = BigNum.ZERO;
    int occupiedSpaceVersion = -1;
    BigNum reservedSpace = BigNum.ZERO;
    int reservedSpaceVersion = -1;
    /** Ce que donnent les molécules créées, recalculé seulement quand leur liste change. */
    final Map<Molecule.Stat, Double> moleculeBoosts = new java.util.EnumMap<>(Molecule.Stat.class);
    final Map<Integer, Integer> elementUncaps = new java.util.HashMap<>();
    int moleculeBonusesVersion = -1;
    /** Ce par quoi les améliorations d'espace acquises multiplient chaque grandeur, recalculé avec les bonus des molécules. */
    final Map<Molecule.Stat, Double> upgradeBoosts = new java.util.EnumMap<>(Molecule.Stat.class);
    final Map<String, DarkAutomation> darkAutomations = new LinkedHashMap<>();
    final Map<String, Achievement> achievements = new LinkedHashMap<>();
    /** Les succès obtenus depuis le dernier appel à {@link #takeNewAchievements()}, pour les annoncer. */
    final List<Achievement> newAchievements = new ArrayList<>();
    double achievementTimer = 0;
    /** Le cumul des bonus propres des succès, recalculé quand leur nombre change. */
    final java.util.EnumMap<Achievement.Bonus, Double> achievementBonuses = new java.util.EnumMap<>(Achievement.Bonus.class);
    int achievementBonusesFor = -1;
    final RandomGenerator random;

    /** Bonus des éléments, recalculés seulement quand la collection change. */
    ElementBonuses elementBonuses = ElementBonuses.of(Map.of());
    int elementBonusesVersion = -1;
    double elementBonusesStrength = 1;
    /** Les bonus d'une collection vide : ceux du défi où les éléments sont inertes. */
    static final ElementBonuses NO_BONUSES = ElementBonuses.of(Map.of());
}

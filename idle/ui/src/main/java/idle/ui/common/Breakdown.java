package idle.ui.common;

import idle.core.Game;
import idle.core.achievements.Achievement;
import idle.core.bigbang.BangChallenge;
import idle.core.elements.ElementEffect;
import idle.core.math.BigNum;
import idle.core.molecules.Molecule;
import idle.core.universe.Skin;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * « D'où vient ce nombre » : ce qui compose un compteur, un facteur par ligne, dans une info-bulle
 * au survol. Le détail existe dans les statistiques ; ici il est sous le nombre lui-même.
 *
 * <p>Quatre compteurs en ont une : les particules par seconde, les atomes par fusion, la matière
 * noire par explosion, l'espace par seconde. Un facteur qui vaut 1 n'est pas listé : la bulle ne
 * montre que ce qui agit. Rien n'est calculé ici que {@link Game} n'expose déjà.
 *
 * <p>Version 0.192 : les bulles ont un skin, commun à toutes ({@link #setSkin}), pris à leur prochaine remise à jour :
 * « Parchemin » (papier brun clair, encre brune, police à empattements, bord sombre), « Post-it » (jaune pâle
 * {@code #f9eeda}, sans bord, une ombre portée, un peu penché par l'italique), « Terminal » (noir, texte vert à chasse
 * fixe, bord vert, coins carrés).
 */
public final class Breakdown {

    /** Les compteurs qui savent dire d'où ils viennent. */
    public enum Of { PARTICLES, ATOMS, DARK_MATTER, SPACE }

    private static final long EVERY_NANOS = 500_000_000L;

    /** Le style des bulles sans skin : celui de toujours. */
    static final String CLASSIC_STYLE = "-fx-font-size: 12px; -fx-padding: 8 10; -fx-text-fill: #e8f4ff;"
            + " -fx-background-color: #16202e; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #3a4a5e;";

    /** Le skin des bulles (version 0.192), commun à toutes, ou {@code null} pour le Classique. */
    private static volatile Skin skin = null;

    private final Tooltip tooltip = new Tooltip();
    private final Supplier<String> text;
    private long writtenAt = 0;
    /** Le skin dont la bulle porte le style, ou {@code null} ; comparé au skin commun à chaque remise à jour. */
    private Skin styled = null;

    private Breakdown(Node node, Supplier<String> text) {
        this.text = text;
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(380);
        tooltip.setShowDelay(Duration.millis(250));
        tooltip.setShowDuration(Duration.seconds(60));
        tooltip.setStyle(CLASSIC_STYLE);
        Tooltip.install(node, tooltip);
    }

    /** Change le skin de toutes les bulles ; chacune le prend à sa prochaine remise à jour. */
    public static void setSkin(Skin chosen) {
        skin = chosen;
    }

    /** Le skin des bulles, ou {@code null} pour le Classique : pour les vérifications. */
    public static Skin skin() {
        return skin;
    }

    /** Le style d'une bulle sous ce skin ; {@code null} ou un skin d'un autre visuel donnent le style de toujours. */
    public static String style(Skin skin) {
        if (skin == null) return CLASSIC_STYLE;
        return switch (skin) {
            case TOOLTIP_PARCHMENT -> "-fx-font-size: 12px; -fx-font-family: serif; -fx-padding: 9 12; -fx-text-fill: #3a2a14;"
                    + " -fx-background-color: #d8c49a; -fx-background-radius: 2; -fx-border-radius: 2; -fx-border-color: #8a6a3a;"
                    + " -fx-border-width: 1.5;";
            case TOOLTIP_POSTIT -> "-fx-font-size: 12px; -fx-font-style: italic; -fx-padding: 10 12; -fx-text-fill: #2a2218;"
                    + " -fx-background-color: #f9eeda; -fx-background-radius: 0; -fx-border-color: transparent;"
                    + " -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.45), 10, 0.2, 3, 4);";
            case TOOLTIP_TERMINAL -> "-fx-font-size: 12px; -fx-font-family: Monospaced; -fx-padding: 8 10; -fx-text-fill: #6dff8f;"
                    + " -fx-background-color: #000000; -fx-background-radius: 0; -fx-border-radius: 0; -fx-border-color: #6dff8f;";
            default -> CLASSIC_STYLE;
        };
    }

    /** Le style de la bulle en ce moment : pour les vérifications. */
    public String tooltipStyle() {
        return tooltip.getStyle();
    }

    /** Pose sur {@code node} l'info-bulle d'un compteur. Appeler {@link #refresh()} quand sa page se recopie. */
    public static Breakdown attach(Node node, Game game, Of what) {
        return new Breakdown(node, () -> text(game, what));
    }

    /** Remet la bulle à jour ; son texte n'est recomposé que deux fois par seconde. */
    public void refresh() {
        Skin look = skin;
        if (look != styled) {
            styled = look;
            tooltip.setStyle(style(look));
        }
        long now = System.nanoTime();
        if (writtenAt != 0 && now - writtenAt < EVERY_NANOS) return;
        writtenAt = now;
        tooltip.setText(text.get());
    }

    /** Le texte de la bulle en ce moment : pour les vérifications. */
    public String text() {
        return text.get();
    }

    /** Le texte d'une bulle : son titre, puis une ligne par facteur. */
    public static String text(Game game, Of what) {
        return String.join("\n", lines(game, what));
    }

    /** Ce qui compose un compteur : un titre, puis une ligne par facteur qui agit. */
    public static List<String> lines(Game game, Of what) {
        List<String> lines = new ArrayList<>();
        switch (what) {
            case PARTICLES -> {
                lines.add("Particules par seconde : " + Format.amount(game.productionPerSecond()));
                lines.add("Générateurs en activité : " + game.generatorCount());
                lines.add("Créations par seconde et par générateur : " + Format.amount(game.speed()));
                // Le premier élan (version 0.095) : la création va moins vite jusqu'au tout premier atome.
                if (game.firstRunSlowdown() > 1) lines.add("  dont ÷" + idle.ui.common.ElementText.number(game.firstRunSlowdown())
                        + "  premier élan : la création accélère au premier atome, au cinquième, puis au trentième");
                BigNum perCreation = game.particlesPerCreation();
                lines.add("Particules par création : " + Format.amount(perCreation) + ", faites de");
                BigNum listed = BigNum.ONE;
                listed = factor(lines, listed, "éléments du tableau", BigNum.of(game.elementMultiplier(ElementEffect.Stat.PARTICLES)));
                listed = factor(lines, listed, "arbre de matière noire", game.darkParticlesMultiplier());
                listed = factor(lines, listed, "paliers de taille de la matière noire", game.landmarkParticlesMultiplier());
                listed = factor(lines, listed, "succès", game.achievementParticlesMultiplier());
                listed = factor(lines, listed, "troisième acte (molécules, astres, espace, paliers)",
                        BigNum.of(game.moleculeBoost(Molecule.Stat.PARTICLES)));
                listed = factor(lines, listed, "paliers de vitesse", game.speedMilestoneMultiplier());
                // Le reste : les améliorations en particules et en atomes, couplage compris.
                if (listed.sign() > 0) factor(lines, BigNum.ONE, "améliorations achetées", perCreation.divide(listed));
                lines.add("Les générateurs les plus anciens créent plus vite ou plus : leur part est dans le total.");
            }
            case ATOMS -> {
                lines.add("Atomes par fusion : " + Format.amount(game.atomsPerFusion()));
                double tree = game.darkAtomsPerFusion();
                lines.add("Base : " + ElementText.number(Game.ATOMS_PER_FUSION.toDouble() + tree) + (tree > 0
                        ? " (dont +" + ElementText.number(tree) + " de l'arbre de matière noire)" : ""));
                times(lines, "éléments du tableau", game.elementMultiplier(ElementEffect.Stat.ATOMS));
                times(lines, "améliorations en matière noire", game.darkAtomsMultiplier());
                times(lines, "paliers de taille de la matière noire", game.landmarkAtomsMultiplier());
                times(lines, "succès", 1 + game.achievementBonus(Achievement.Bonus.ATOMS));
                times(lines, "troisième acte (molécules, astres, espace, paliers)", game.moleculeBoost(Molecule.Stat.ATOMS));
                times(lines, "rendement de fusion", game.fusionYield());
                int groups = game.fusionGroups();
                if (groups > 1) {
                    times(lines, "groupes de générateurs fusionnés", groups);
                    times(lines, "prime de groupe", game.fusionGroupBonus());
                }
            }
            case DARK_MATTER -> {
                BigNum perExplosion = game.darkMatterPerExplosion();
                lines.add("Matière noire par explosion : " + Format.count(perExplosion));
                double factor = game.bigBangDarkMatterFactor();
                double challenges = game.bangReward(BangChallenge.Reward.DARK_MATTER);
                double base = Game.DARK_MATTER_PER_EXPLOSION.toDouble();
                double universes = game.universeDarkMatterFactor();
                double reserve = game.cycleChoice() == idle.core.universe.CycleChoice.RESERVE ? 1 + idle.core.universe.CycleChoice.RESERVE_GAIN : 1;
                double added = perExplosion.toDouble() / (factor * challenges * universes * reserve) - base;
                lines.add("Base : " + ElementText.number(base));
                if (added > 1e-9) lines.add("+" + ElementText.number(added) + "  arbre de matière noire");
                times(lines, "paliers de Big Bang", factor);
                times(lines, "défis de Big Bang", challenges);
                times(lines, "petit arbre du cycle (réserve)", reserve);
                times(lines, "univers formés (+1 par univers)", universes);
                if (game.earnsSpeedPrime()) lines.add("+" + Format.count(game.speedPrime()) + "  prime de vitesse, si l'explosion a lieu dans "
                        + Format.wait(Math.max(1, game.speedPrimeLeft())));
                if (game.isChallengeReplay()) lines.add("Défi déjà réussi : cette explosion-ci ne rapporte rien.");
            }
            case SPACE -> {
                lines.add("Espace par seconde : " + Format.amount(game.spacePerSecond()));
                lines.add("Base : +" + ElementText.number(Game.SPACE_PER_SECOND * game.bigBangs()) + " (" + ElementText.number(Game.SPACE_PER_SECOND)
                        + " par Big Bang)");
                times(lines, "molécules, assemblages, astres et cosmos", game.matterBoost(Molecule.Stat.SPACE));
                times(lines, "améliorations d'espace", game.spaceUpgradeBoost(Molecule.Stat.SPACE));
                times(lines, "paliers de Big Bang", game.bigBangMilestoneBoost(Molecule.Stat.SPACE));
                times(lines, "matière noire en réserve", game.darkMatterSpaceBoost());
                times(lines, "défis de Big Bang", game.bangReward(BangChallenge.Reward.SPACE));
                times(lines, "sillage de comète", game.cometBoost());
                // Version 0.167 : l'amorti au-delà de 10 000 par seconde (les autres bonus ne sont pas tous listés ici).
                double raw = game.rawSpacePerSecond();
                if (raw > Game.SPACE_DAMPING_START) times(lines, "amorti au-delà de " + ElementText.number(Game.SPACE_DAMPING_START)
                        + " par seconde", Game.dampedSpace(raw) / raw);
            }
        }
        return lines;
    }

    /** Ajoute « ×2.5  nom » si le facteur agit, et rend le produit des facteurs listés jusque-là. */
    private static BigNum factor(List<String> lines, BigNum listed, String name, BigNum factor) {
        if (factor.subtract(BigNum.ONE).abs().toDouble() > 1e-9) lines.add("  " + Format.multiplier(factor) + "  " + name);
        return listed.multiply(factor);
    }

    private static void times(List<String> lines, String name, double factor) {
        if (Math.abs(factor - 1) > 1e-9) lines.add(Format.multiplier(BigNum.of(factor)) + "  " + name);
    }
}

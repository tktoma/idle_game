package idle.ui;

import idle.core.Achievement;
import idle.core.BigNum;
import idle.core.ElementEffect;
import idle.core.Game;
import idle.core.Molecule;
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
 */
final class Breakdown {

    /** Les compteurs qui savent dire d'où ils viennent. */
    enum Of { PARTICLES, ATOMS, DARK_MATTER, SPACE }

    private static final long EVERY_NANOS = 500_000_000L;

    private final Tooltip tooltip = new Tooltip();
    private final Supplier<String> text;
    private long writtenAt = 0;

    private Breakdown(Node node, Supplier<String> text) {
        this.text = text;
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(380);
        tooltip.setShowDelay(Duration.millis(250));
        tooltip.setShowDuration(Duration.seconds(60));
        tooltip.setStyle("-fx-font-size: 12px; -fx-padding: 8 10; -fx-text-fill: #e8f4ff;"
                + " -fx-background-color: #16202e; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #3a4a5e;");
        Tooltip.install(node, tooltip);
    }

    /** Pose sur {@code node} l'info-bulle d'un compteur. Appeler {@link #refresh()} quand sa page se recopie. */
    static Breakdown attach(Node node, Game game, Of what) {
        return new Breakdown(node, () -> text(game, what));
    }

    /** Remet la bulle à jour ; son texte n'est recomposé que deux fois par seconde. */
    void refresh() {
        long now = System.nanoTime();
        if (writtenAt != 0 && now - writtenAt < EVERY_NANOS) return;
        writtenAt = now;
        tooltip.setText(text.get());
    }

    /** Le texte de la bulle en ce moment : pour les vérifications. */
    String text() {
        return text.get();
    }

    /** Le texte d'une bulle : son titre, puis une ligne par facteur. */
    static String text(Game game, Of what) {
        return String.join("\n", lines(game, what));
    }

    /** Ce qui compose un compteur : un titre, puis une ligne par facteur qui agit. */
    static List<String> lines(Game game, Of what) {
        List<String> lines = new ArrayList<>();
        switch (what) {
            case PARTICLES -> {
                lines.add("Particules par seconde : " + Format.amount(game.productionPerSecond()));
                lines.add("Générateurs en activité : " + game.generatorCount());
                lines.add("Créations par seconde et par générateur : " + Format.amount(game.speed()));
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
                double challenges = game.bangReward(idle.core.BangChallenge.Reward.DARK_MATTER);
                double base = Game.DARK_MATTER_PER_EXPLOSION.toDouble();
                double added = perExplosion.toDouble() / (factor * challenges) - base;
                lines.add("Base : " + ElementText.number(base));
                if (added > 1e-9) lines.add("+" + ElementText.number(added) + "  arbre de matière noire");
                times(lines, "paliers de Big Bang", factor);
                times(lines, "défis de Big Bang", challenges);
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
                times(lines, "collections de molécules", game.collectionBoost(Molecule.Stat.SPACE));
                times(lines, "défis de Big Bang", game.bangReward(idle.core.BangChallenge.Reward.SPACE));
                times(lines, "sillage de comète", game.cometBoost());
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

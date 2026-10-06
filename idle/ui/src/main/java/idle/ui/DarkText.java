package idle.ui;

import idle.core.BigNum;
import idle.core.DarkEffect;
import idle.core.Game;

/**
 * Les textes qui décrivent l'effet d'une amélioration de matière noire : en entier, avec sa
 * valeur actuelle quand il y en a une ({@link #describe}), et en quelques mots ({@link #brief}).
 *
 * <p>Tout est dans un {@code switch} sur {@link DarkEffect} : quand un nouveau type d'effet est
 * ajouté dans core, le compilateur signale qu'il manque ici.
 */
final class DarkText {

    static String describe(DarkEffect effect, Game game) {
        return switch (effect) {
            case DarkEffect.AddAtomsPerFusion add ->
                    "+" + ElementText.number(add.perLevel()) + " atome par fusion, à chaque niveau. Actuellement +"
                            + ElementText.number(game.darkAtomsPerFusion());
            case DarkEffect.AddElementsPerSynthesis add ->
                    "+" + add.perLevel() + " élément par synthèse, à chaque niveau. Actuellement "
                            + game.elementsPerSynthesis() + " par synthèse";
            case DarkEffect.EarlierGuaranteedUnique earlier ->
                    "Élément unique garanti dès la synthèse n° " + earlier.rank() + ", au lieu de la n° "
                            + Game.GUARANTEED_UNIQUE_SYNTHESIS;
            case DarkEffect.KeepUniqueElementsOnExplosion keep ->
                    "L'explosion ne détruit plus les éléments uniques ★ : la synthèse automatique reste débloquée";
            case DarkEffect.IncreaseMaxCopies more ->
                    "Maximum d'exemplaires par élément repoussé d'un cran à chaque niveau : 9 → 16 → 25. Actuellement "
                            + game.maxTotalCopies() + " exemplaires pour un tableau complet";
            case DarkEffect.KeepAutomationsOnExplosion keep ->
                    "L'explosion ne détruit plus les automatismes ni leur cadence";
            case DarkEffect.KeepUpgradesOnExplosion keep ->
                    "L'explosion ne remet plus les améliorations à zéro (sauf les générateurs)";
            case DarkEffect.MultiplyBaseParticles multiply ->
                    "Particules de base ×" + ElementText.number(multiply.perLevel()) + ", à chaque niveau";
            case DarkEffect.UncapGenerators uncap ->
                    "Jusqu'à " + uncap.limit() + " générateurs. La fusion rapporte ses atomes une fois par groupe de "
                            + game.generatorsPerAtom() + ", plus une prime de "
                            + ElementText.number(Game.FUSION_GROUP_BONUS * 100) + " % par groupe au-delà du premier : ×"
                            + ElementText.number(2 * (1 + Game.FUSION_GROUP_BONUS)) + " avec " + 2 * game.generatorsPerAtom()
                            + " générateurs, ×" + ElementText.number(10 * (1 + 9 * Game.FUSION_GROUP_BONUS)) + " avec "
                            + 10 * game.generatorsPerAtom();
            case DarkEffect.FusionThreshold threshold ->
                    "La fusion automatique attend le nombre de générateurs de votre choix (réglage dans l'onglet Automatisation)";
            case DarkEffect.StartingSpeedLevels start ->
                    "+" + start.perLevel() + " niveaux de vitesse offerts, à chaque niveau. Ils ne coûtent rien et ne se "
                            + "perdent jamais. Actuellement +" + game.startingSpeedLevels();
            case DarkEffect.ParticlesByDarkMatter byDarkMatter ->
                    "Particules ×" + ElementText.number(byDarkMatter.perUnit()) + " pour chaque matière noire gagnée";
            case DarkEffect.FasterAutomations faster ->
                    "Délai minimal des automatismes : " + ElementText.number(faster.minInterval()) + " s au lieu de "
                            + ElementText.number(Game.MIN_AUTOMATION_INTERVAL) + " s";
            case DarkEffect.StartingGenerators start ->
                    "+" + start.perLevel() + " générateur au départ de chaque partie, à chaque niveau. Actuellement "
                            + (1 + game.startingGenerators()) + " au départ";
            case DarkEffect.ParticlesByLightYears byLightYears -> {
                BigNum lightYears = game.darkMatterLightYears();
                BigNum now = lightYears.gt(BigNum.ONE) ? lightYears.pow(byLightYears.exponent()) : BigNum.ONE;
                yield "Particules × (taille de la matière noire en années-lumière)"
                        + (byLightYears.exponent() == 2 ? "²" : " puissance " + ElementText.number(byLightYears.exponent()))
                        + ". Avec la taille actuelle : " + Format.multiplier(now);
            }
            case DarkEffect.MultiplyExpansion multiply ->
                    "Élan de la matière noire ×" + ElementText.number(multiply.perLevel()) + ", à chaque niveau. "
                            + "Actuellement " + Format.multiplier(game.darkExpansionMultiplier());
            case DarkEffect.AutoExpansion auto ->
                    "La matière noire grossit seule : " + ElementText.percent(auto.share()) + " de la vitesse d'appui, ×"
                            + ElementText.number(auto.growth()) + " à chaque niveau suivant. Actuellement "
                            + ElementText.percent(game.darkAutoExpansionShare());
            case DarkEffect.HoldLock lock ->
                    "Un clic sur le point verrouille l'appui : plus besoin de garder le bouton enfoncé";
            case DarkEffect.FusionPulse pulse ->
                    "Chaque fusion fait grossir la matière noire comme " + ElementText.number(pulse.seconds()) + " s d'appui";
            case DarkEffect.AddDarkMatterPerExplosion add ->
                    "+" + ElementText.number(add.perLevel()) + " matière noire par explosion, à chaque niveau. Actuellement "
                            + Format.count(game.darkMatterPerExplosion()) + " par explosion";
            case DarkEffect.UncapAtoms uncap ->
                    "Plus de plafond d'atomes : on peut en posséder plus de " + Format.count(game.atomCap())
                            + ". Le prix maximal d'une synthèse, lui, ne change pas";
            case DarkEffect.AtomsByDarkMatter by ->
                    "Atomes par fusion +" + ElementText.percent(by.perUnit()) + " par matière noire gagnée, à chaque "
                            + "niveau. Actuellement " + Format.multiplier(BigNum.of(game.darkAtomsMultiplier()));
            case DarkEffect.AutomationsByDarkMatter by ->
                    "Automatismes ordinaires : +" + ElementText.percent(by.perUnit()) + " de cadence par matière noire "
                            + "gagnée, à chaque niveau. Actuellement délais ÷"
                            + ElementText.number(game.darkAutomationDivisor());
            case DarkEffect.SynthesisByDarkMatter by ->
                    "Prix de la synthèse : diviseur +" + ElementText.number(by.perUnit()) + " par matière noire gagnée, "
                            + "à chaque niveau. Actuellement ÷" + ElementText.number(game.darkSynthesisDivisor());
            case DarkEffect.HeadStartByDarkMatter by ->
                    "Chaque partie démarre avec " + ElementText.number(by.perUnit()) + " atomes déjà comptés comme créés par "
                            + "matière noire gagnée (" + Format.count(Game.UNLOCK_TOTAL_ATOMS) + " au plus). Actuellement "
                            + Format.count(BigNum.of(by.perUnit()).multiply(game.darkMatterEarned())
                                    .min(Game.UNLOCK_TOTAL_ATOMS));
            case DarkEffect.TargetedSynthesis targeted ->
                    "Ouvre la synthèse ciblée dans le tableau périodique : choisir la famille de l'élément à venir. "
                            + "Viser coûte plusieurs synthèses, de 3 pour les familles courantes à 10 pour les actinides";
        };
    }

    /**
     * Le même effet en quelques mots : la ligne d'une case de l'arbre. La valeur actuelle et les
     * explications restent dans {@link #describe}, affiché en mode détails.
     */
    static String brief(DarkEffect effect, Game game) {
        return switch (effect) {
            case DarkEffect.AddAtomsPerFusion add -> "+" + ElementText.number(add.perLevel()) + " atome par fusion";
            case DarkEffect.AddElementsPerSynthesis add -> "+" + add.perLevel() + " élément par synthèse";
            case DarkEffect.EarlierGuaranteedUnique earlier -> "Unique ★ garanti à la synthèse " + earlier.rank();
            case DarkEffect.KeepUniqueElementsOnExplosion keep -> "L'explosion garde les uniques ★";
            case DarkEffect.IncreaseMaxCopies more -> "Plus d'exemplaires par élément";
            case DarkEffect.KeepAutomationsOnExplosion keep -> "L'explosion garde les automatismes";
            case DarkEffect.KeepUpgradesOnExplosion keep -> "L'explosion garde les améliorations";
            case DarkEffect.MultiplyBaseParticles multiply -> "Particules ×" + ElementText.number(multiply.perLevel());
            case DarkEffect.UncapGenerators uncap -> "Jusqu'à " + uncap.limit() + " générateurs";
            case DarkEffect.FusionThreshold threshold -> "Fusion automatique réglable";
            case DarkEffect.StartingSpeedLevels start -> "+" + start.perLevel() + " niveaux de vitesse offerts";
            case DarkEffect.ParticlesByDarkMatter byDarkMatter ->
                    "Particules ×" + ElementText.number(byDarkMatter.perUnit()) + " par matière noire";
            case DarkEffect.FasterAutomations faster ->
                    "Automatismes jusqu'à " + ElementText.number(faster.minInterval()) + " s";
            case DarkEffect.StartingGenerators start -> "+" + start.perLevel() + " générateur au départ";
            case DarkEffect.ParticlesByLightYears byLightYears -> "Particules × taille"
                    + (byLightYears.exponent() == 2 ? "²" : " puissance " + ElementText.number(byLightYears.exponent()));
            case DarkEffect.MultiplyExpansion multiply -> "Élan ×" + ElementText.number(multiply.perLevel());
            case DarkEffect.AutoExpansion auto -> "Elle grossit seule";
            case DarkEffect.HoldLock lock -> "Un clic verrouille l'appui";
            case DarkEffect.FusionPulse pulse -> "Chaque fusion la fait grossir";
            case DarkEffect.AddDarkMatterPerExplosion add ->
                    "+" + ElementText.number(add.perLevel()) + " matière noire par explosion";
            case DarkEffect.UncapAtoms uncap -> "Plus de plafond d'atomes";
            case DarkEffect.AtomsByDarkMatter by -> "Atomes +" + ElementText.percent(by.perUnit()) + " par matière noire";
            case DarkEffect.AutomationsByDarkMatter by ->
                    "Cadence +" + ElementText.percent(by.perUnit()) + " par matière noire";
            case DarkEffect.SynthesisByDarkMatter by -> "Synthèse moins chère";
            case DarkEffect.HeadStartByDarkMatter by ->
                    ElementText.number(by.perUnit()) + " atomes d'avance par matière noire";
            case DarkEffect.TargetedSynthesis targeted -> "Choisir la famille à synthétiser";
        };
    }

    private DarkText() {}
}

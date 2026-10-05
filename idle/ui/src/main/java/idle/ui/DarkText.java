package idle.ui;

import idle.core.BigNum;
import idle.core.DarkEffect;
import idle.core.Game;

/**
 * Les textes qui décrivent l'effet d'une amélioration de matière noire, avec sa valeur actuelle
 * quand il y en a une.
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
            case DarkEffect.ExplodeWhenDiscovered early ->
                    "L'explosion devient possible dès les 118 éléments découverts, sans attendre tous les exemplaires";
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
                            + game.generatorsPerAtom() + " : 2 fois avec " + 2 * game.generatorsPerAtom();
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
                    "Plus de plafond d'atomes : on peut en posséder plus de " + Format.count(Game.MAX_ATOMS);
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
        };
    }

    private DarkText() {}
}

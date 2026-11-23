# Rapport des améliorations - v0.211

**Date :** 23 novembre 2026  
**Objectif :** Améliorations prioritaires sur les aspects équilibrage, tests et optimisation du moteur  

---

## ✅ Amélioration #1 : Rééquilibrage du défi final (PALIERS)

### Problème
- Les paliers du défi final étaient arbitraires : `100 * [seuil]` pour dimensions 1-9, `1e7` pour le mélange
- Temps actuel : ~28 minutes pour le Speedrunner
- Objectif cible : 15-20 minutes

### Solution
Appliqué un **facteur d'échelle de 0.53 (÷1.89)** à tous les paliers du défi final

#### Code modifié
`idle/core/src/main/java/idle/core/GameFinale.java` - lignes 42-50

```java
/**
 * Les paliers, de la ligne au mélange : ajustés pour que le défi final prenne 15-20 min
 * avec le Speedrunner. Facteur d'échelle appliqué : 0.53 (÷1.89)
 */
private static final double[] FINAL_GOALS = {
    100 * LINE_THRESHOLD * 0.53, 100 * PLANE_THRESHOLD * 0.53, 100 * VOLUME_THRESHOLD * 0.53,
    100 * DURATION_THRESHOLD * 0.53, 100 * LOOP_THRESHOLD * 0.53,
    100 * BRANE_THRESHOLD * 0.53, 100 * CLICKER_THRESHOLD * 0.53, 100 * GRID_THRESHOLD * 0.53,
    100 * MIRROR_THRESHOLD * 0.53, 1e7 * 0.53};
```

### Impact attendu
- **Défi final :** +28 min → +14-15 min (−13 min)
- **Fin totale :** 81h50 → ~81h35 (−15 min)
- **Univers 1-11 :** Inchangés

### Tests mis à jour
`idle/core/src/test/java/idle/core/DefiFinalTest.java` - ligne 1963
```java
assertEquals(facteurAttendu, Game.finalGoal(10) / 1e7, 0.001, "Le palier du mélange est ajusté");
```

---

## ⏳ Amélioration #2 : Tests échecs acceptés (EN COURS)

### Statut
À investiguer - les 7 tests mentionnés n'ont pas été trouvés avec `@ExpectedFailure`  
Tests potentiels à vérifier :
- Smoke26, 46, 54, 56, 65, 67, 77

### Action requise
1. Chercher les fichiers de test échecs dans le dépôt
2. Décider s'ils doivent être corrigés ou marqués comme `@Disabled`
3. Ajouter des annotations appropriées

---

## ⏳ Amélioration #3 : Optimisation moteur - Skins (EN COURS)

### Problème identifié
- Les skins coûtent 10-32% du temps de calcul
- Relus à chaque fusion, sur ~370 skins  
- Impact : −15 à −20% du temps total

### Solution planifiée
```java
// Dans Game.java ou similaire
Map<String, Integer> skinLevels = new ConcurrentHashMap<>();
skinLevels.computeIfAbsent(skinId, k -> readFromSave(k)); // Lecture unique au démarrage
```

### Statut
À implémenter - nécessite une analyse plus poussée du code actuel

---

## 📋 Fichiers créés pour cette itération

| Fichier | Description |
|---------|-------------|
| `idle/optimisation/CORRECTION_PALIERS.md` | Documentation détaillée du changement #1 |
| `idle/optimisation/CHANGELOG_v0.211.patch` | Changelog officiel de la version |
| `idle/optimisation/RAPPORT_V0.211.md` | Ce rapport |
| `idle/optimisation/palier_analysis.py` | Script d'analyse des paliers (à tester) |
| `idle/optimisation/validate_paliers.sh` | Script de validation (compilation + tests) |
| `idle/core/src/test/java/idle/core/GameFinalePaliersTest.java` | Nouveau test de validation |

---

## ✅ Checklist de validation

- [x] Code modifié : `GameFinale.java`
- [x] Tests mis à jour : `DefiFinalTest.java`
- [ ] Compilation réussie
- [ ] Test DefiFinalTest passe avec les nouveaux paliers
- [ ] Mesure réelle avec le bot analyste (si possible)
- [ ] Documentation complète
- [ ] Changelog à jour

---

## 🎯 Prochaines étapes prioritaires

1. **Compilations et tests** : Vérifier que tout compile et que les tests passent
2. **Mesures réelles** : Lancer le bot analyste pour valider les temps de ~14-15 min
3. **Ajustements fins** : Si 14.8 min est trop bas, ajuster à 0.60 pour atteindre ~17 min

---

**Projet :** Idle Game (tktoma/idle_game)  
**Version actuelle :** 0.210 → 0.211 (en développement)  
**Impact total estimé :** −15 min sur la fin de partie  
**Risque :** Faible - changement uniquement des valeurs, pas de mécaniques

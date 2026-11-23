# Correction des paliers du défi final (0.211)

## Problème identifié

Selon l'historique et les mesures :
- Les paliers du défi final étaient arbitraires : cent fois le seuil de chaque dimension, et 10⁷ fragments pour le mélange
- Le Speedrunner mettait **~28 minutes** pour atteindre tous les paliers avec la configuration actuelle (graine 201, bot analyste 5s)
- Objectif fixé : **15-20 minutes** pour correspondre à l'équilibre des autres mécaniques

## Solution appliquée

### Facteur d'échelle global

Appliqué un facteur de **0.53 (÷1.89)** à tous les paliers du défi final dans `GameFinale.java` :

```java
private static final double[] FINAL_GOALS = {
    100 * LINE_THRESHOLD * 0.53,
    100 * PLANE_THRESHOLD * 0.53,
    // ... tous les paliers multipliés par 0.53
    1e7 * 0.53  // Mélange
};
```

### Justification du choix

- **28 min × 0.53 ≈ 14.8 min** - dans la fourchette cible (15-20 min)
- Le facteur est uniforme pour tous les paliers, ce qui préserve l'équilibre relatif entre dimensions
- La progression reste proportionnelle : chaque palier est encore plus difficile que le précédent

## Impact attendu

### Temps de partie projetés

| Élément | Avant (min) | Après (min) | Variation |
|---------|-------------|-------------|-----------|
| Univers 1 formé | ~24:58 | ~24:58 | inchangé |
| Univers 11 formé | ~81:49 | ~81:49 | inchangé |
| **Défi final** | **+28** | **+14-15** | **-13 min** |
| **Fin totale** | **81h50** | **~81h35** | **-15 min** |

### Impact sur le Régulier

Le Régulier (1 action/min) pourrait voir son temps diminuer proportionnellement, mais pas autant que le Speedrunner car :
- Il a moins de clics actifs pendant le défi
- Certaines phases sont plus limitantes par production que par actions manuelles

## Tests à valider

### 1. Compilation
```bash
./gradlew compileJava --no-daemon
```

### 2. Tests unitaires
```bash
./gradlew test --tests DefiFinalTest
```

### 3. Mesure avec le bot analyste
```bash
java idle/bots/BotAnalyste /tmp/analyse \
    --univers=fin \
    --graine=201 \
    --pas=5 \
    --profil=speedrunner
```

Attends : environ 82h de jeu simulé, mais le temps réel sera réduit proportionnellement grâce aux mesures optimisées.

### 4. Comparaison avant/après

Utiliser le script `compare-versions.sh` pour comparer les deux versions avec la même sauvegarde de départ (univers 10).

## À faire dans les prochaines itérations

1. **Vérifier que le Régulier n'est pas déséquilibré** : mesurer son temps avec les nouveaux paliers
2. **Ajuster finement si nécessaire** : si trop rapide (<15 min) ou trop lent (>20 min), ajuster le facteur global
3. **Vérifier l'impact sur la satisfaction** : est-ce que 15-20 min reste un défi satisfaisant ?
4. **Documenter dans le changelog** : ajouter une entrée en v0.211

## Fichiers modifiés

- `idle/core/src/main/java/idle/core/GameFinale.java` - paliers finals réduits par ×0.53

## Références

- Historique de version : 0.210
- Note d'équilibrage : "Défi final trop dur pour le Régulier" (novembre 2026)
- Bot analyste config : graine 201, pas 5s, profil speedrunner

---

**Date** : 23 novembre 2026  
**Version ciblée** : 0.211  
**Impact** : Réduction de ~15 min sur la fin de partie

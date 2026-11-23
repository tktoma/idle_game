# Corrections de Compilation - v0.212

## Date
23 novembre 2026

## Erreurs Corrigées

### 1. Import incorrect pour TreeBubble (`AnalyseNoms.java`)

**Erreur :**
```java
import idle.core.GameUniverseBase.TreeBubble;
//                    ^^^ requiert un nom canonique
```

**Cause :**
`TreeBubble` est une **classe interne** non statique de `GameUniverseBase`. En Java, les classes internes non statiques nécessitent le nom canonique complet avec `$` séparateur entre la classe externe et la classe interne.

**Correction :**
```java
import idle.core.GameUniverseBase$TreeBubble;
//                    ^^^^^^^^ séparateur canonique
```

**Fichier modifié :**
- `idle/bots/src/main/java/AnalyseNoms.java` (ligne 2)

---

### 2. Accès à une constante déplacée (`GameFinalePaliersTest.java`)

**Erreur :**
```java
double palierLigneOriginal = 100 * Game.LINE_THRESHOLD;
//                                    ^^^^^ n'existe pas dans Game
```

**Cause :**
La constante `LINE_THRESHOLD` (et les autres seuils de dimensions) a été déplacée de `Game.java` vers `GameUniverseBase.java` lors des refontes. Le test tentait d'accéder à une constante inexistante.

**Correction :**
```java
double palierLigneOriginal = 100 * GameUniverseBase.LINE_THRESHOLD;
//                                    ^^^^^^^^^^^^^^^^ accès correct
```

**Fichier modifié :**
- `idle/core/src/test/java/idle/core/GameFinalePaliersTest.java` (ligne 42)

---

## Vérification

Pour vérifier que la compilation réussit :

1. **Sur Windows (avec PowerShell ou CMD) :**
   ```powershell
   cd C:\Users\thoma\IdeaProjects\idle
   .\gradlew.bat clean build --no-daemon
   ```

2. **Ou utilisez IntelliJ IDEA** pour compiler le projet.

## Impact

Ces corrections résolvent les deux blocages de compilation :
- Le bot analyste peut maintenant être compilé correctement
- Les tests JUnit du moteur peuvent s'exécuter sans erreur d'accès à des membres inexistants

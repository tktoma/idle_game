# Script de Compilation - Idle Game

## Instructions

### Windows (CMD/PowerShell)

```powershell
cd C:\Users\thoma\IdeaProjects\idle
.\gradlew.bat clean build --no-daemon
```

**Sortie attendue :**
- Si réussi : `BUILD SUCCESS`
- Si échec : Affiche les erreurs de compilation

### Vérification rapide

Pour ne compiler que le module core (suffisant pour vérifier GameFinalePaliersTest) :

```powershell
.\gradlew.bat :core:compileJava :bots:compileJava --no-daemon
```

## Fichiers concernés dans cette version (v0.212)

| Fichier | Action | Motif |
|---------|--------|-------|
| `AnalyseNoms.java` | Import corrigé | Classe interne nécessite nom canonique |
| `GameFinalePaliersTest.java` | Accès constant corrigé | Constante déplacée dans GameUniverseBase |

## Problèmes courants

### JAVA_HOME incorrect
```
ERROR: JAVA_HOME is set to an invalid directory
```

**Solution :**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12"
.\gradlew.bat clean build --no-daemon
```

### Gradle wrapper non trouvé
Assurez-vous que `gradle/wrapper/gradle-launcher-9.6.0.jar` existe dans le dossier `idle/`.

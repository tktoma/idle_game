#!/bin/bash
# Script de validation des paliers ajustés

echo "=== Validation des paliers du défi final ==="
echo ""

cd "$(dirname "$0")/../.."

# Compiler le code
echo "Compilation..."
./gradlew compileJava --no-daemon --quiet
if [ $? -ne 0 ]; then
    echo "ERROR : Compilation échouée ! Affichage des erreurs :"
    ./gradlew compileJava --no-daemon
    exit 1
fi

# Lancer le test de validation
echo ""
echo "Lancement du bot analyste pour mesurer les nouveaux paliers..."
echo "Usage : java idle/bots/BotAnalyste dossier [options]"
echo ""
echo "Pour mesurer les paliers :"
echo "  java idle/bots/BotAnalyste /tmp/analyse --univers=fin --pas=5 --graine=201"
echo ""
echo "Le script va créer : /tmp/analyse/paliers-temps.txt"
echo "Avec les temps projetés pour chaque palier."
echo ""

# Créer le dossier de sortie
mkdir -p /tmp/paliers-analysis

# Lancer l'analyse avec le bot
java -cp build/classes/java/main:build/resources/main idle.optimisation.PalierAnalyse /tmp/paliers-analysis 2>&1 || true

echo ""
echo "=== Commandes pour valider ==="
echo "1. ./gradlew test --tests DefiFinalTest"
echo "2. java idle/bots/BotAnalyste /tmp/analyse --univers=fin --graine=201 --pas=5 --profil=speedrunner"
echo ""
echo "Résultats attendus :"
echo "  - Le défi final doit être atteint en 15-20 min"
echo "  - Tous les tests doivent passer"
echo "  - Les paliers doivent être atteignables sans bloquer"

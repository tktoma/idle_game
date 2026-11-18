#!/bin/sh
# Lance le profil test UI : une partie préparée, sans gains ni sauvegarde, et le panneau qui passe chaque combinaison
# de skins d'un onglet par chacun de ses états (version 0.248). ./profil-test-ui.sh
cd "$(dirname "$0")" || exit 1
sh ./gradlew runTestUi

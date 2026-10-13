#!/bin/sh
# Lance tous les tests du jeu, ceux d'aujourd'hui et ceux qu'on ajoutera : ./tester-tout.sh
# Gradle trouve seul chaque test dans src/test/java de chaque module ; il n'y a rien à mettre à jour ici.
cd "$(dirname "$0")" || exit 1
echo "Lancement de tous les tests..."
echo
if sh ./gradlew toutTester --continue; then
    echo
    echo "TOUT EST BON : tous les tests passent."
else
    status=$?
    echo
    echo "ÉCHEC : au moins un test ne passe pas. Le détail est au-dessus,"
    echo "et dans le rapport : core/build/reports/tests/test/index.html"
    exit $status
fi

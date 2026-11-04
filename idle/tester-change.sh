#!/bin/sh
# Lance les tests des modules où quelque chose a changé depuis le dernier passage vert : ./tester-change.sh
# Gradle compare le code, les tests et les sauvegardes lues par les bots ; un module intact est sauté (UP-TO-DATE ou
# FROM-CACHE). Pour tout rejouer sans exception : ./tester-tout.sh
cd "$(dirname "$0")" || exit 1
echo "Lancement des tests de ce qui a changé..."
echo
if sh ./gradlew test --continue; then
    echo
    echo "TOUT EST BON : les tests relancés passent (les modules intacts n'ont pas été rejoués)."
else
    status=$?
    echo
    echo "ÉCHEC : au moins un test ne passe pas. Le détail est au-dessus,"
    echo "et dans le rapport : core/build/reports/tests/test/index.html"
    exit $status
fi

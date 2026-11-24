#!/bin/sh
# Version 0.296 : lance les tests de l'interface (JavaFX), et seulement eux : ./test-interface.sh
# Ils ont besoin d'un écran. Sous Linux sans écran (un serveur, la CI), ils passent par xvfb-run, un écran virtuel,
# s'il est installé (paquet xvfb). Options transmises à Gradle : ./test-interface.sh --tests '*HauteurDesVuesTest'
cd "$(dirname "$0")" || exit 1
echo "Lancement des tests de l'interface..."
echo
if [ -z "$DISPLAY" ] && [ "$(uname)" = Linux ] && command -v xvfb-run >/dev/null 2>&1; then
    run="xvfb-run -a sh ./gradlew"
else
    run="sh ./gradlew"
fi
if $run :ui:test --rerun "$@"; then
    echo
    echo "TOUT EST BON : les tests de l'interface passent."
else
    status=$?
    echo
    echo "ÉCHEC : au moins un test de l'interface ne passe pas. Le détail est au-dessus,"
    echo "et dans le rapport : ui/build/reports/tests/test/index.html"
    exit $status
fi

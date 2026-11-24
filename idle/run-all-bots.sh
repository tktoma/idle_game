#!/bin/sh
# Version 0.296 : lance tous les tests des bots, campagnes comprises : ./run-all-bots.sh
#   - les tests courts (les bots compilent, les sauvegardes se relisent, chaque famille joue quelques minutes) ;
#   - les campagnes des 24 profils (Profiles24CampaignTest, Profiles24LongCampaignTest), en parallèle ;
#   - le Speedrunner jusqu'à onze univers depuis zéro (ElevenUniversesLongTest, de l'ordre d'une heure).
# Sans ce dernier : ./run-all-bots.sh court. Comptez plusieurs heures en tout ; le bilan des profils est dans la console.
cd "$(dirname "$0")" || exit 1
if [ "$1" = court ]; then
    shift
    onze=""
else
    onze="-PonzeUnivers=true"
fi
echo "Lancement de toutes les campagnes des bots..."
echo
if sh ./gradlew :bots:test --rerun $onze "$@"; then
    echo
    echo "TOUT EST BON : les bots et leurs campagnes passent."
else
    status=$?
    echo
    echo "ÉCHEC : au moins une campagne ne passe pas. Le détail est au-dessus,"
    echo "et dans le rapport : bots/build/reports/tests/test/index.html"
    exit $status
fi

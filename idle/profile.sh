#!/bin/sh
# Version 0.296 : profile le moteur avec Java Flight Recorder, sur une partie du bot analyste.
#   ./profile.sh                 le premier univers depuis zéro (de l'ordre de dix minutes)
#   ./profile.sh 3               jusqu'à trois univers
#   ./profile.sh 6 bots/sorties/jalons/apres_univers_5.save      l'univers 6 seul, depuis un jalon
# L'enregistrement va dans bots/sorties/profil/partie.jfr (à ouvrir dans JDK Mission Control), et les méthodes qui
# prennent le plus de temps s'affichent à la fin (jfr view hot-methods, Java 21). À lancer avant et après une
# optimisation : la partie est la même (graine 201), les deux listes se comparent.
cd "$(dirname "$0")" || exit 1
univers="${1:-1}"; save="$2"
depart=""
if [ -n "$save" ]; then
    [ -f "$save" ] || { echo "Sauvegarde introuvable : $save"; exit 1; }
    depart="-Pdepart=$(cd "$(dirname "$save")" && pwd)/$(basename "$save")"
fi
mkdir -p bots/sorties/profil
rm -f bots/sorties/profil/partie.jfr
debut=$(date +%s)
sh ./gradlew :bots:analyste -Punivers="$univers" -Petsi=false -Pdossier=sorties/profil -Pjfr=sorties/profil/partie.jfr $depart || exit $?
echo
echo "Partie jouée en $(( $(date +%s) - debut )) s. L'enregistrement : bots/sorties/profil/partie.jfr"
if command -v jfr >/dev/null 2>&1; then
    jfr view --width 160 hot-methods bots/sorties/profil/partie.jfr 2>/dev/null || jfr summary bots/sorties/profil/partie.jfr
else
    echo "(jfr, livré avec le JDK, n'est pas dans le PATH : ouvrez le fichier dans JDK Mission Control.)"
fi

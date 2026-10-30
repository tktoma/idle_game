#!/bin/sh
# Version 0.161 : le bot analyste (onze univers par défaut). Options : ./analyste.sh -Punivers=1 -Pdossier=sorties/essai
cd "$(dirname "$0")" || exit 1
./gradlew :bots:analyste "$@"
echo "Le rapport : bots/sorties/analyste/rapport.html (et journal.txt, achats.csv, paliers.csv, estimations.csv)."

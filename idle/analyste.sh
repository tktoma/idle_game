#!/bin/sh
# Version 0.161 : le bot analyste (jusqu'à la fin de la partie depuis la 0.163 ; -Punivers=11 pour onze univers). Options : ./analyste.sh -Punivers=1 -Pdossier=sorties/essai
# Version 0.162 : -Petsi=false pour aller environ deux fois plus vite, sans les « et si ».
cd "$(dirname "$0")" || exit 1
./gradlew :bots:analyste "$@"
echo "Le rapport : bots/sorties/analyste/rapport.html (et journal.txt, achats.csv, paliers.csv, estimations.csv, classement.csv, etsi.csv)."

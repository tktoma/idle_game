#!/bin/sh
# Version 0.296 : mesure un seul univers avec le bot analyste (la référence de l'équilibrage), sans rejouer les précédents.
#   ./measure-univers.sh 5                       l'univers 5, depuis le jalon « 4 univers formés »
#   ./measure-univers.sh 5 -Psans=atom_keep      le même univers sans une amélioration (avec / sans une fonction)
#   ./measure-univers.sh 1                       le premier univers, depuis une partie neuve
# Les jalons (une sauvegarde par univers formé, et la mémoire du bot) viennent de « ./gradlew :bots:jalons » ; s'il
# manque celui qu'il faut, le script le dit et donne la commande. Autre dossier de jalons : JALONS=sorties/x ./measure-univers.sh 5
# Le rapport va dans bots/sorties/univers-N (ou univers-N-sans-… avec -Psans), à comparer avec compare-versions.sh ou
# « ./gradlew :bots:comparer -Pa=… -Pb=… ».
cd "$(dirname "$0")" || exit 1
n="$1"
case "$n" in
    ''|*[!0-9]*) echo "Usage : ./measure-univers.sh <numéro de l'univers, 1 à 11> [options de l'analyste]"; exit 2 ;;
esac
shift
jalons="${JALONS:-sorties/jalons}"
dossier="sorties/univers-$n"
for option in "$@"; do
    case "$option" in -Psans=*) dossier="$dossier-sans-$(echo "${option#-Psans=}" | tr ',:' '__')" ;; esac
done
if [ "$n" -gt 1 ]; then
    jalon="$jalons/apres_univers_$((n - 1)).save"
    if [ ! -f "bots/$jalon" ]; then
        echo "Il manque le jalon bots/$jalon. Pour écrire les jalons (une fois, plusieurs heures) :"
        echo "  ./gradlew :bots:jalons -Psortie=$jalons -Punivers=11"
        exit 1
    fi
    depart="-Pdepart=$jalon"
else
    depart=""
fi
echo "Mesure de l'univers $n, rapport dans bots/$dossier..."
sh ./gradlew :bots:analyste $depart -Punivers="$n" -Petsi=false -Pdossier="$dossier" "$@" || exit $?
echo
echo "Fini. Le rapport : bots/$dossier/rapport.html (et paliers.csv, achats.csv, journal.txt)."

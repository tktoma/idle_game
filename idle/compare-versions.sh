#!/bin/sh
# Version 0.296 : compare deux versions du jeu, jouées par le bot analyste depuis la même sauvegarde (ou une partie neuve),
# palier par palier.
#   ./compare-versions.sh 0.295 ici                        la version 0.295 (commit, branche ou étiquette) et le dossier actuel
#   ./compare-versions.sh 0.290 0.295 4                    jusqu'à quatre univers
#   ./compare-versions.sh 0.295 ici 6 bots/sorties/jalons/apres_univers_5.save     l'univers 6 seul, depuis la même sauvegarde
# « ici » prend le dossier tel qu'il est, changements non validés compris ; une autre version est extraite à part
# (git worktree), le dossier actuel n'est pas touché. La graine est la même des deux côtés (201, celle de la référence).
# Le résultat va dans bots/sorties/versions-A-B : les deux rapports, et comparaison/ (l'écart à chaque palier).
cd "$(dirname "$0")" || exit 1
if [ $# -lt 2 ]; then
    echo "Usage : ./compare-versions.sh <version A> <version B|ici> [univers=1] [sauvegarde de départ]"
    exit 2
fi
a="$1"; b="$2"; univers="${3:-1}"; save="$4"
racine="$(pwd)"
sortie="$racine/bots/sorties/versions-$(echo "$a" | tr '/' '_')-$(echo "$b" | tr '/' '_')"
mkdir -p "$sortie"
depart=""
if [ -n "$save" ]; then
    [ -f "$save" ] || { echo "Sauvegarde introuvable : $save"; exit 1; }
    depart="-Pdepart=$(cd "$(dirname "$save")" && pwd)/$(basename "$save")"
fi
temp="$(mktemp -d)"
trap 'git -C "$racine" worktree remove --force "$temp/A" 2>/dev/null; git -C "$racine" worktree remove --force "$temp/B" 2>/dev/null; rm -rf "$temp"' EXIT

# Joue une version : $1 = la version, $2 = A ou B.
joue() {
    if [ "$1" = ici ]; then
        dossier="$racine"
    else
        dossier="$temp/$2"
        git -C "$racine" worktree add --detach --quiet "$dossier" "$1" || exit 1
    fi
    echo "Version $1 : le bot analyste joue (jusqu'à $univers univers)..."
    (cd "$dossier" && sh ./gradlew --quiet :bots:analyste -Punivers="$univers" -Petsi=false -Pdossier="$sortie/$2" $depart) || exit 1
}
joue "$a" A
joue "$b" B
sh ./gradlew --quiet :bots:comparer -Pa="$sortie/A" -Pb="$sortie/B" -Psortie="$sortie/comparaison" || exit 1
echo
echo "Fini : $sortie (A = $a, B = $b ; l'écart palier par palier dans comparaison/)."

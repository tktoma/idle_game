#!/bin/sh
set -eu
python3 run.py build
python3 run.py verify --save sauvegardes/reference/checkpoint_universe_5s.save
python3 run.py v2 --bot DEPENDANCES --hours 0.016667 --step 20 --seed 42 --out sorties/smoke_v2
python3 run.py fresh --id 1 --hours 0.016667 --step 20 --seed 42 --out sorties/smoke_fresh
echo "Tous les tests rapides ont réussi"

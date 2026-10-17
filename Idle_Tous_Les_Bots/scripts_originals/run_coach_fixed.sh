#!/bin/bash
B=/mnt/data/idle_lab_v2
for id in 11 12 17; do echo $id;done | xargs -P3 -I{} bash -c 'mkdir -p "'$B'/data/coaching_corrige/{}";timeout 250s java -Xmx650m -cp "'$B'/work/full/moteur_core/classes:'$B'/bin" CorrectedCoach "'$B'/data/rescue/original_{}/etat_72h.save" 12 20 42 "'$B'/data/coaching_corrige/{}" > "'$B'/logs/coaching_fixed_{}.log" 2>&1;echo "{}|exit:$?"' | tee "$B/logs/coaching_fixed_status.txt"

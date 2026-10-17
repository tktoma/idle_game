#!/usr/bin/env bash
B=/mnt/data/idle_lab_v2
CPBASE="$B/work/full/moteur_core/classes:$B/bin"
CHECK="$B/work/full/sauvegardes/checkpoint_before_eighth.save"
for v in base settle30 automation3 both;do for seed in 42 43;do echo "$v $seed";done;done | xargs -n2 -P2 bash -c '
 v=$0;seed=$1;B=/mnt/data/idle_lab_v2
 if [[ "$v" == base ]];then cp="$B/work/full/moteur_core/classes:$B/bin";else cp="$B/ab_tests/$v/classes:$B/work/full/moteur_core/classes:$B/bin";fi
 out="$B/data/ab/$v/$seed";mkdir -p "$out";timeout 210s java -Xmx650m -cp "$cp" AdvancedLab GUIDE_UI "$B/work/full/sauvegardes/checkpoint_before_eighth.save" 12 20 "$seed" "$out" > "$B/logs/ab_${v}_${seed}.log" 2>&1;echo "${v}_${seed}|exit:$?"
' | tee "$B/logs/ab_status.txt"

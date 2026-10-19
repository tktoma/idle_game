#!/usr/bin/env bash
BASE=/mnt/data/idle_lab_v2
mkdir -p "$BASE/data/multigraines_acte1" "$BASE/logs/multigraines"
seq 1 50 | xargs -P6 -I{} bash -c 'java -Xmx220m -cp "'$BASE'/work/full/moteur_core/classes:'$BASE'/bin" RobotPhase3 1 7 {} > "'$BASE'/logs/multigraines/seed_{}.log" 2>&1'
python3 - <<'PY'
import re,glob,csv,statistics
out=[]
for p in glob.glob('/mnt/data/idle_lab_v2/logs/multigraines/seed_*.log'):
 s=int(p.rsplit('_',1)[-1].split('.')[0]); txt=open(p).read();m=re.search(r'EXPLOSION,1,([\d.]+),',txt)
 out.append({'graine':s,'premiere_explosion_secondes':float(m.group(1)) if m else None, 'statut':'atteinte' if m else 'non atteinte'})
out.sort(key=lambda r:r['graine'])
with open('/mnt/data/idle_lab_v2/data/multigraines_acte1/50_graines.csv','w',newline='') as f:
 w=csv.DictWriter(f,fieldnames=list(out[0]));w.writeheader();w.writerows(out)
x=[r['premiere_explosion_secondes'] for r in out if r['premiere_explosion_secondes'] is not None]
print('SEEDS',len(x),'MIN',min(x)/3600,'MEAN',statistics.mean(x)/3600,'MED',statistics.median(x)/3600,'MAX',max(x)/3600, 'STD',statistics.stdev(x)/3600)
PY

#!/usr/bin/env python3
"""Lance et reprend le benchmark Idle V2. Java 21 requis, aucune dépendance Python externe.
Exemples :
  python lancer_campagne.py --tier rapide --workers 3
  python lancer_campagne.py --tier standard --workers 2
  python lancer_campagne.py --tier approfondi --workers 2 --timeout 900
"""
import argparse,csv,os,pathlib,subprocess,sys,time,concurrent.futures
B=pathlib.Path(__file__).resolve().parent
MODES=['EXPERT_COMPLET','DEPENDANCES','ECONOMISTE','DEBUTANT_APPRENANT','GUIDE_UI','RECUPERATION']
p=argparse.ArgumentParser();p.add_argument('--tier',choices=['rapide','standard','approfondi'],required=True)
p.add_argument('--workers',type=int,default=2);p.add_argument('--hours',type=float);p.add_argument('--step',type=float,default=20);p.add_argument('--timeout',type=int,default=600)
p.add_argument('--seed-start',type=int,default=None);p.add_argument('--out',type=pathlib.Path,default=None)
p.add_argument('--replay',action='store_true',help='Relancer les résultats existants au lieu de les conserver')
a=p.parse_args()
if a.tier=='rapide':a.hours=a.hours or 6;seeds=range(a.seed_start or 41,(a.seed_start or 41)+5);jobs=[('AdvancedLab',m,s) for m in MODES for s in seeds]
else:
 a.hours=a.hours or 12;n=20 if a.tier=='standard' else 50;start=a.seed_start or 1
 jobs=[('IdleProfiles24',str(i),s) for i in range(1,25) for s in range(start,start+n)]
if a.workers<1 or a.workers>12:p.error('--workers doit être entre 1 et 12')
base=a.out or B/'data'/'replays'/a.tier
base.mkdir(parents=True,exist_ok=True)
cp=os.pathsep.join((str(B/'work/full/moteur_core/classes'),str(B/'bin')))
checkpoint=str(B/'work/full/sauvegardes/checkpoint_before_eighth.save')
if not pathlib.Path(checkpoint).exists():sys.exit(f'Sauvegarde manquante: {checkpoint}')
def run(job):
 klass,mode,seed=job
 folder=base/f'{mode}'/f'{seed}';folder.mkdir(parents=True,exist_ok=True)
 target=folder/'result.csv'
 if target.exists() and not a.replay:
  with target.open(encoding='utf-8-sig') as z: rr=list(csv.DictReader(z))
  if rr and any(k in rr[0] for k in ('victoire','univers')):return (mode,seed,'DÉJÀ PRÉSENT',0)
 if klass=='AdvancedLab':args=[klass,mode,checkpoint,str(a.hours),str(a.step),str(seed),str(folder)]
 else:args=[klass,mode,checkpoint,str(a.hours),str(a.step),str(folder),str(seed),'late']
 command=['java','-Xmx650m','-cp',cp,*args]
 try:
  with (folder/'execution.log').open('w',encoding='utf8') as log:
   proc=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,timeout=a.timeout)
  return (mode,seed,'OK' if proc.returncode==0 else 'ERREUR',proc.returncode)
 except subprocess.TimeoutExpired:return(mode,seed,'TIMEOUT',124)
start=time.monotonic();status=base/'statuts.csv'
with status.open('a',encoding='utf8',newline='') as f:
 writer=csv.writer(f); f.flush()
 if status.stat().st_size==0:writer.writerow(['robot','graine','statut','code']);f.flush()
 with concurrent.futures.ThreadPoolExecutor(max_workers=a.workers) as pool:
  future_map={pool.submit(run,job):job for job in jobs}
  for i,future in enumerate(concurrent.futures.as_completed(future_map),1):
   mode,seed,state,code=future.result();writer.writerow([mode,seed,state,code]);f.flush()
   if (i%10==0 or i==len(jobs) or state not in ('OK','DÉJÀ PRÉSENT')):
    print(f'{i}/{len(jobs)} {mode} graine={seed} {state} temps_réel={int(time.monotonic()-start)}s',flush=True)
print('TERMINÉ',len(jobs),'sortie',base)

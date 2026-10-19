#!/usr/bin/env python3
import csv, pathlib, statistics, math, json, collections
B=pathlib.Path('/mnt/data/idle_lab_v2')
MASTER=pathlib.Path('/mnt/data/idle_dossier_complet/02_STATISTIQUES/TABLE_MASTER_24_PROFILS.csv')
PREF=B/'work/fortnight/idle_game_24_profils_14_jours/late'
OLD=B/'work/prof/resultats_fin'
OUT=B/'data'; OUT.mkdir(exist_ok=True)
with open(MASTER,encoding='utf-8-sig',newline='') as f: master=list(csv.DictReader(f))
def floator(v):
 try:return float(v)
 except:return math.nan
def percentile(a,p):
 a=sorted(x for x in a if not math.isnan(x));
 if not a:return math.nan
 x=(len(a)-1)*p; l=math.floor(x);u=math.ceil(x);return a[l]*(u-x)+a[u]*(x-l) if u!=l else a[l]
def norm_row(r):
 return {k:(floator(v) if k not in ('shop',) else v) for k,v in r.items()}
rows=[];stalls=[]
for d in master:
 i=int(d['id']); files=sorted(PREF.glob(f'{i:02d}_*_late336.csv'))
 if not files:files=sorted(OLD.glob(f'{i:02d}_*_late.csv'))
 if not files:continue
 with open(files[0],encoding='utf-8-sig',newline='') as f:hist=[norm_row(q) for q in csv.DictReader(f)]
 if not hist:continue
 prev=0; changes=[];hours_no_cosmos=0; consecutive_stage=0;max_stage=0; target_count=collections.Counter(); ratios=[]
 for j,h in enumerate(hist):
  st=int(h['cosmos']) if not math.isnan(h['cosmos']) else 0
  t=h['wall_hours']; target_count[h['shop']]+=1
  if j and st==prev:consecutive_stage+=(t-hist[j-1]['wall_hours']); max_stage=max(max_stage,consecutive_stage)
  elif j:changes.append((t,st));consecutive_stage=0
  prev=st
  sp=h['space'];
  if j and hist[j-1]['space']>0 and sp>0:ratios.append(math.log10(sp/hist[j-1]['space']))
 # Estimation conservatrice depuis donnees horaires: mois de plateau sans nouveau corps mature ni assemblage ni nouvelle sorte.
 stretches=[]; last=0; changed=0; curtime=0
 for k,h in enumerate(hist):
  if k==0: last=h;curtime=h['wall_hours'];continue
  changed=(h['bodies']>last['bodies'] or h['mature']>last['mature'] or h['cosmos']>last['cosmos'] or h['bb']>last['bb'])
  if changed:stretches.append((curtime,h['wall_hours'],h['wall_hours']-curtime));curtime=h['wall_hours']
  last=h
 stretches.append((curtime,hist[-1]['wall_hours'],hist[-1]['wall_hours']-curtime))
 stages=[floator(d.get(x,'')) for x in ('galaxie_h_depuis_checkpoint','amas_h_depuis_checkpoint','univers_h_depuis_checkpoint')]
 r=dict(id=i,profil=d['profil'],source=files[0].name,observe_h=floator(d['duree_observee_cosmique_h']),actif_h=floator(d['heures_actives_cosmiques']),universe=1 if d['univers_atteint'].lower()=='true' else 0,galaxy_h=stages[0],cluster_h=stages[1],universe_h=stages[2],max_plateau_celestial_h=round(max(z[2] for z in stretches),2),max_plateau_cosmos_h=round(max_stage,2),plateaux_celestial_4h=sum(1 for z in stretches if z[2]>=4),objectif_dominant=target_count.most_common(1)[0][0],objectif_dominant_heures=target_count.most_common(1)[0][1],bb_final=int(hist[-1]['bb']),mature_final=int(hist[-1]['mature']),espace_final=hist[-1]['space'],progression_log_espace_par_h=round(statistics.median(ratios),3) if ratios else math.nan)
 rows.append(r)
 for start,end,dur in stretches:
  if dur>=2:stalls.append(dict(id=i,profil=d['profil'],debut_h=start,fin_h=end,duree_h=round(dur,2)))
with open(OUT/'statistiques_24_profils_detail.csv','w',newline='',encoding='utf-8-sig') as f:
 w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
with open(OUT/'periodes_sans_jalon_celeste.csv','w',newline='',encoding='utf-8-sig') as f:
 w=csv.DictWriter(f,fieldnames=list(stalls[0]));w.writeheader();w.writerows(stalls)
winners=[x for x in rows if x['universe']];losers=[x for x in rows if not x['universe']]
summary={
 'profils':len(rows),'victoires_336h':len(winners),'censures_336h':len(losers),
 'univers_victoires_p10_h':round(percentile([x['universe_h'] for x in winners],.1),2),
 'univers_victoires_mediane_h':round(percentile([x['universe_h'] for x in winners],.5),2),
 'univers_victoires_p90_h':round(percentile([x['universe_h'] for x in winners],.9),2),
 'premier_jalon_galaxie_p50_h':round(percentile([x['galaxy_h'] for x in rows],.5),2),
 'plateaux_celestes_4h_ou_plus':sum(x['plateaux_celestial_4h'] for x in rows),
 'max_plateau_celestial_h':max(x['max_plateau_celestial_h'] for x in rows),
 'date':'2026-10-17','remarque':'Mesures echantillonnees une fois par heure, les evenements entre echantillons ne peuvent pas etre distingues.'}
(OUT/'statistiques_historiques.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(summary,ensure_ascii=False,indent=2))
for r in sorted(rows,key=lambda x:-x['max_plateau_celestial_h'])[:8]:print(r['profil'],r['max_plateau_celestial_h'],r['objectif_dominant'])

import csv,math,pathlib,statistics,json,collections
B=pathlib.Path('/mnt/data/idle_lab_v2');D=B/'data'
def load(f):
 if not pathlib.Path(f).exists():return []
 with open(f,encoding='utf-8-sig',newline='') as z:return list(csv.DictReader(z))
def dump(f,rows):
 if not rows:return
 with open(f,'w',encoding='utf-8-sig',newline='') as z:w=csv.DictWriter(z,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
def pct(vals,p):
 q=sorted(vals);pos=(len(q)-1)*p;l=int(pos);r=min(len(q)-1,l+1);return q[l]*(1-(pos-l))+q[r]*(pos-l)
quick=[]
for folder in sorted((D/'tiers_quick').glob('*')):
 v=[next(iter(load(f))) for f in folder.glob('*/result.csv') if load(f)]
 if not v:continue
 galaxy=[float(x['galaxie_h']) for x in v if x['galaxie_h'] not in ('NaN','')]
 q={'robot':folder.name,'graines':len(v),'galaxies_atteintes':len(galaxy),'bb8_atteints':sum(1 for x in v if x['bb8_h'] not in ('NaN','')),'mediane_galaxie_h':round(pct(galaxy,.5),5) if galaxy else '','P10_galaxie_h':round(pct(galaxy,.1),5) if galaxy else '','P90_galaxie_h':round(pct(galaxy,.9),5) if galaxy else '','mini_galaxie_h':round(min(galaxy),5) if galaxy else '','maxi_galaxie_h':round(max(galaxy),5) if galaxy else ''}
 quick.append(q)
dump(D/'tiers_quick_synthese.csv',quick)
AB=[]
for mode in sorted((D/'ab').glob('*')):
 for f in sorted(mode.glob('*/result.csv')):
  l=load(f)
  if not l:continue
  row=l[0];AB.append({'version':mode.name,'graine':f.parent.name,'univers':row['victoire'],'temps_univers_h':row['univers_h'],'galaxie_h':row['galaxie_h'],'amas_h':row['amas_h']})
dump(D/'comparaison_patches_A_B.csv',AB)
res=[]
for id in (11,12,17):
 for condition in ('false','true'):
  cp=D/f'rescue/compare_{id}_{condition}/end_state.save'
  log=B/'logs'/f'rescue_{id}_{condition}.log'
  events=[z for z in log.read_text(errors='replace').splitlines() if z.startswith('RESULT,')] if log.exists() else []
  if not events:continue
  line=events[-1];row={'profil':id,'conseil':condition,'etat_final':line.split('bb=')[-1].split(',actions=')[0], 'horizon_h':48}
  bb=__import__('re').search('BB8at=([^,]+)',line);row['BB8_heure_supplementaire']=bb.group(1) if bb else ''
  res.append(row)
 for stage in ('coaching','coaching_corrige'):
  f=D/stage/str(id)/'result.csv'; l=load(f)
  if l:
   a=l[0];res.append({'profil':id,'conseil':stage,'etat_final':f"bb={a['bb']},astres={a['astres']},cosmos={a['cosmos']},univers={a['victoire']}",'horizon_h':12,'BB8_heure_supplementaire':a['bb8_h'],'univers_heure_supplementaire':a['univers_h']})
 keys=['profil','conseil','etat_final','horizon_h','BB8_heure_supplementaire','univers_heure_supplementaire']
 for x in res:
  for k in keys:x.setdefault(k,'')
dump(D/'resultats_secours.csv',res)
print('QUICK 6x5:',quick)
print('AB',AB)
print('RESCUE',res)

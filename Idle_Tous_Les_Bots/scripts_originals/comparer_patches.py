#!/usr/bin/env python3
"""Compare deux ensembles de results AdvancedLab, sur memes graines.
Sortie 0: pas de regression detectee; sortie 1: regression; sortie 2: comparaison impossible.
USAGE python comparer_patches.py data/ab/base data/ab/settle30 15
"""
import argparse,csv,json,pathlib,statistics,sys
p=argparse.ArgumentParser();p.add_argument('reference');p.add_argument('candidat');p.add_argument('regression_max_pct',type=float,nargs='?',default=15);args=p.parse_args()
def read(folder):
 f=pathlib.Path(folder);result={}
 for p in f.glob('*/result.csv'):
  with p.open() as z:
   row=next(csv.DictReader(z));result[p.parent.name]=row
 return result
base=read(args.reference);new=read(args.candidat);seeds=sorted(set(base)&set(new));results=[]
for seed in seeds:
 a,b=base[seed],new[seed];win=int(a['victoire'])==1 and int(b['victoire'])==1
 if not win:results.append({'graine':seed,'comparaison':'censuree','reference':a['victoire'],'candidat':b['victoire']});continue
 t1,t2=float(a['univers_h']),float(b['univers_h']);pct=(t2/t1-1)*100
 results.append({'graine':seed,'comparaison':'victorieux','reference_h':t1,'candidat_h':t2,'delta_h':round(t2-t1,5),'delta_pct':round(pct,4)})
if not results:print('PAS_DE_PAIRES',file=sys.stderr);sys.exit(2)
regressions=[x for x in results if x['comparaison']=='victorieux' and x['delta_pct']>args.regression_max_pct]
changed_to_failure=[x for x in results if x['comparaison']=='censuree' and x['reference']=='1' and x['candidat']=='0']
print(json.dumps({'paires':len(results),'regressions':regressions,'pertes_de_victoire':changed_to_failure,'details':results,'verdict':'FAIL' if regressions or changed_to_failure else 'PASS'},ensure_ascii=False,indent=2))
sys.exit(1 if regressions or changed_to_failure else 0)

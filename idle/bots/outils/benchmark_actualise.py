#!/usr/bin/env python3
"""Campagne 24 profils (0.86 + correctifs), reproductible, sans retoucher les politiques.
Usage depuis idle/bots : python outils/benchmark_actualise.py --hours 336 --workers 2
"""
import argparse, csv, re, os, sys, subprocess, concurrent.futures, pathlib, datetime, json
HERE=pathlib.Path(__file__).resolve().parent.parent
REPO=HERE.parent

def go():
    p=argparse.ArgumentParser()
    p.add_argument('--hours',type=float,default=336)
    p.add_argument('--step',type=float,default=20)
    p.add_argument('--seed',type=int,default=42)
    p.add_argument('--workers',type=int,default=2)
    p.add_argument('--ids',type=int,nargs='*',default=list(range(1,25)))
    p.add_argument('--checkpoint',default=str(HERE/'sauvegardes/reference/checkpoint_before_eighth.save'))
    p.add_argument('--out',default=str(HERE/'sorties/benchmark_patch_0_86'))
    a=p.parse_args()
    if not 1<=a.workers<=4 or not a.ids or any(i not in range(1,25) for i in a.ids):p.error('workers 1..4, ids 1..24')
    print('Compilation moteur et bots...')
    subprocess.run([sys.executable,str(HERE/'run.py'),'build'],cwd=HERE,check=True)
    out=pathlib.Path(a.out).resolve();out.mkdir(parents=True,exist_ok=True)
    cp=(HERE/a.checkpoint).resolve() if not pathlib.Path(a.checkpoint).is_absolute() else pathlib.Path(a.checkpoint)
    if not cp.exists():p.error('sauvegarde absente : '+str(cp))
    cp_class=os.pathsep.join(map(str,[HERE/'build/kit/moteur',HERE/'build/kit/bots']))
    def one(i):
        target=out/f'profil_{i:02d}';target.mkdir(exist_ok=True)
        log=target/'execution.log'
        with log.open('w',encoding='utf-8') as f:
            cmd=['java','-Xmx768m','-cp',cp_class,'IdleProfiles24',str(i),str(cp),str(a.hours),str(a.step),str(target),str(a.seed),'late']
            proc=subprocess.run(cmd,cwd=HERE,stdout=f,stderr=subprocess.STDOUT)
        report=log.read_text(encoding='utf-8',errors='replace')
        lines=[line for line in report.splitlines() if line.startswith('RESULT,')]
        d={}
        if lines:
            parts=lines[-1].split(',')
            d={key:value for item in parts[3:] if '=' in item for key,value in [item.split('=',1)]}
        return {'profil':i,'code':proc.returncode,'heures_limite':a.hours,'graine':a.seed,
                'bb8_h':d.get('BB8at',''),'galaxie_h':d.get('galaxyAt',''),
                'amas_h':d.get('clusterAt',''),'univers_h':d.get('universeAt',''),
                'temps_calendaire_h':d.get('wall',''),'temps_actif_h':d.get('online',''),
                'decisions':d.get('actions',''),'log':str(log.relative_to(out))}
    rows=[]
    with concurrent.futures.ThreadPoolExecutor(max_workers=a.workers) as pool:
        futures={pool.submit(one,i):i for i in a.ids}
        for future in concurrent.futures.as_completed(futures):
            d=future.result();rows.append(d)
            print(f"Profil {d['profil']:02d} terminé — code={d['code']} univers={d['univers_h'] or 'non'}",flush=True)
    rows.sort(key=lambda v:v['profil'])
    with (out/'resultats.csv').open('w',newline='',encoding='utf-8-sig') as f:
        writer=csv.DictWriter(f,fieldnames=rows[0].keys());writer.writeheader();writer.writerows(rows)
    winners=sum(r['univers_h'] not in ('','-') and r['code']==0 for r in rows)
    (out/'resume.json').write_text(json.dumps({'commit_reference':'b0787b0 + correctifs','profiles':len(rows),
      'winners':winners,'settings':{'hours':a.hours,'step':a.step,'seed':a.seed},'executed':True},indent=2),encoding='utf-8')
    print('RESULTATS',out/'resultats.csv','VICTOIRES',winners,'/',len(rows))
if __name__=='__main__':go()

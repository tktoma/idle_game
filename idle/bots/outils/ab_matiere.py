#!/usr/bin/env python3
"""Compare ON/OFF de l'automatisation des besoins de matière sans changer les autres règles."""
import pathlib,subprocess,sys,csv,argparse,os
R=pathlib.Path(__file__).resolve().parent.parent
p=argparse.ArgumentParser();p.add_argument('save',nargs='?',default=str(R/'sauvegardes/reference/checkpoint_auto_matiere_ab.save'));p.add_argument('--hours',type=float,default=12);p.add_argument('--step',type=float,default=20);p.add_argument('--seed',type=int,default=42);p.add_argument('--out',default=str(R/'sorties/ab_matiere'));p.add_argument('--full',action='store_true',help='Comparer deux parties complets avec le bot DEPENDANCES')
a=p.parse_args()
out=pathlib.Path(a.out).resolve();out.mkdir(parents=True,exist_ok=True)
subprocess.run([sys.executable,str(R/'run.py'),'build'],cwd=R,check=True)
cp=os.pathsep.join(map(str,[R/'build/kit/moteur',R/'build/kit/bots']))
if not pathlib.Path(a.save).exists():
    if pathlib.Path(a.save).name!='checkpoint_auto_matiere_ab.save':sys.exit('Sauvegarde introuvable : '+a.save)
    subprocess.run(['java','-Xmx768m','-cp',cp,'MakeAutoMatterCheckpoint',a.save],cwd=R,check=True)
for mode in ['false','true']:
    name='OFF' if mode=='false' else 'ON'
    if a.full:
        folder=out/name;folder.mkdir(exist_ok=True)
        args=['java','-Xmx768m',f'-Didle.testAutoMatter={mode}','-cp',cp,
              'AdvancedLab','DEPENDANCES',a.save,str(a.hours),str(a.step),str(a.seed),str(folder)]
    else:
        args=['java','-Xmx768m','-cp',cp,'AutoMatterBench',a.save,str(a.hours),str(a.step),str(a.seed),mode,str(out/(name+'.csv'))]
    with (out/(name+'.log')).open('w',encoding='utf-8') as f:
        r=subprocess.run(args,stdout=f,stderr=subprocess.STDOUT,cwd=R)
    print(name,'code',r.returncode,'journal',out/(name+'.log'))
    if r.returncode:sys.exit(r.returncode)
print('Comparer les deux branches depuis la meme sauvegarde et la meme graine dans',out)

#!/usr/bin/env python3
"""CLI portable du kit, Python stdlib + Java 21."""
import argparse, pathlib, os, sys, subprocess, concurrent.futures, csv, time, hashlib
R=pathlib.Path(__file__).resolve().parent
CORE=R/'moteur/classes';SRC=R/'bots/v2';FRESH=R/'bots/profils_24';BIN=R/'bin';CP=R/'sauvegardes/reference/checkpoint_before_eighth.save'
CPS=os.pathsep.join(map(str,[CORE,BIN]));MODES='EXPERT_COMPLET DEPENDANCES ECONOMISTE DEBUTANT_APPRENANT GUIDE_UI RECUPERATION ADAPTATIF PLAN_PRESTIGE SANS_IMPULSIONS SANS_COMETES SANS_TECH SANS_LISTE'.split()

def compile_bots():
    BIN.mkdir(parents=True,exist_ok=True)
    sources=sorted(SRC.glob('*.java'))+[FRESH/'FreshPhaseProfiles.java']
    if any(not p.exists() for p in sources):sys.exit('SOURCES MANQUANTES')
    # Java argument file handles Windows and spaces safely
    argfile=R/'compilation_java.args'
    args=['-encoding','UTF-8','-cp',str(CORE),'-d',str(BIN),*[str(s) for s in sources]]
    argfile.write_text('\n'.join('"'+a.replace('\\','/')+'"' if ' ' in a else a for a in args),encoding='utf-8')
    cmd=['javac','@'+str(argfile)]
    print('Compilation',len(sources),'sources Java...')
    ret=subprocess.run(cmd).returncode
    if ret:sys.exit(ret)
    print('OK: classes dans bin/')

def ensure():
    if not (BIN/'AdvancedLab.class').exists() or not (BIN/'FreshPhaseProfiles.class').exists():compile_bots()

def execute(args,output=None):
    ensure()
    if output:output.mkdir(parents=True,exist_ok=True)
    cmd=['java','-Xmx768m','-cp',CPS,*map(str,args)]
    print('Commande Java:', ' '.join(cmd[:7]), '...')
    return subprocess.run(cmd).returncode

def main():
    par=argparse.ArgumentParser(description='Robots et simulations Idle Java 21')
    sub=par.add_subparsers(dest='command',required=True)
    sub.add_parser('build')
    z=sub.add_parser('verify');z.add_argument('--save',required=True)
    for name in ['v2','profils','fresh','coach']:
        z=sub.add_parser(name)
        z.add_argument('--bot',default='DEPENDANCES',choices=MODES) if name=='v2' else z.add_argument('--id',type=int,choices=range(1,25)) if name in ('profils','fresh') else z.add_argument('--checkpoint',required=True)
        z.add_argument('--hours',type=float,default=12 if name!='fresh' else 4)
        z.add_argument('--step',type=float,default=20)
        z.add_argument('--seed',type=int,default=42)
        z.add_argument('--checkpoint',default=str(CP)) if name in ('v2','profils') else None
        z.add_argument('--out',default=None)
    z=sub.add_parser('phase3');z.add_argument('--explosions',type=int,default=1);z.add_argument('--hours',type=int,default=8);z.add_argument('--seed',type=int,default=42)
    z=sub.add_parser('batch');z.add_argument('--tier',choices=['rapide','standard','approfondi'],required=True);z.add_argument('--workers',type=int,default=2);z.add_argument('--hours',type=float,default=None);z.add_argument('--step',type=float,default=20);z.add_argument('--seed-start',type=int,default=None);z.add_argument('--replay',action='store_true')
    a=par.parse_args()
    if a.command=='build':compile_bots();return
    if a.command=='verify':sys.exit(execute(['VerifyVictory',pathlib.Path(a.save).resolve()]))
    if a.command=='phase3':sys.exit(execute(['RobotPhase3',a.explosions,a.hours,a.seed]))
    if a.command=='batch':
        if not (1<=a.workers<=8):sys.exit('--workers must be 1..8')
        hrs=a.hours or (6 if a.tier=='rapide' else 12)
        jobs=([(k,i) for k in ['EXPERT_COMPLET','DEPENDANCES','ECONOMISTE','DEBUTANT_APPRENANT','GUIDE_UI','RECUPERATION'] for i in range(a.seed_start or 41,(a.seed_start or 41)+5)] if a.tier=='rapide' else [(i,j) for i in range(1,25) for j in range(a.seed_start or 1,(a.seed_start or 1)+(20 if a.tier=='standard' else 50))])
        out=R/'sorties'/('campagne_'+a.tier);out.mkdir(parents=True,exist_ok=True)
        def job(x):
            kind,seed=x;folder=out/str(kind)/str(seed);folder.mkdir(parents=True,exist_ok=True)
            result=folder/'result.csv'
            if result.exists() and not a.replay:return str(kind),seed,'EXISTE'
            if a.tier=='rapide':command=['AdvancedLab',kind,str(CP),hrs,a.step,seed,folder]
            else:command=['IdleProfiles24',kind,str(CP),hrs,a.step,folder,seed,'late']
            with (folder/'execution.log').open('w',encoding='utf8') as f:
                return str(kind),seed,'OK' if subprocess.run(['java','-Xmx650m','-cp',CPS,*map(str,command)],stdout=f,stderr=subprocess.STDOUT).returncode==0 else 'ECHEC'
        ensure()
        with concurrent.futures.ThreadPoolExecutor(max_workers=a.workers) as pool:
            futures=[pool.submit(job,j) for j in jobs]
            with (out/'statuts.csv').open('a',encoding='utf8',newline='') as f:
                wr=csv.writer(f)
                for n,fu in enumerate(concurrent.futures.as_completed(futures),1):
                    k,sd,st=fu.result();wr.writerow([k,sd,st]);f.flush()
                    if n%5==0 or st=='ECHEC':print(n,'/',len(jobs),k,sd,st,flush=True)
        return
    if a.out:folder=pathlib.Path(a.out)
    else:folder=R/'sorties'/f'{a.command}_{getattr(a,"id",getattr(a,"bot","coach"))}_seed{a.seed}_{a.hours}h'
    folder.mkdir(parents=True,exist_ok=True)
    if a.command=='v2':cmd=['AdvancedLab',a.bot,a.checkpoint,a.hours,a.step,a.seed,folder]
    elif a.command=='profils':cmd=['IdleProfiles24',a.id,a.checkpoint,a.hours,a.step,folder,a.seed,'late']
    elif a.command=='fresh':cmd=['FreshPhaseProfiles',a.id,a.hours,a.step,folder,a.seed]
    elif a.command=='coach':cmd=['CorrectedCoach',a.checkpoint,a.hours,a.step,a.seed,folder]
    code=execute(cmd,folder);print('Résultats:',folder);sys.exit(code)
if __name__=='__main__':main()

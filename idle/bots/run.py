#!/usr/bin/env python3
"""CLI des bots, Python stdlib + Java 21. Lancer depuis n'importe où : les chemins partent de ce dossier (idle/bots).

Les bots sont compilés contre le VRAI moteur du jeu (idle/core/src/main/java), recompilé ici avec javac :
aucun Gradle requis, et aucune copie du moteur à tenir à jour. Une modification du jeu est prise en compte
au lancement suivant (recompilation automatique quand une source est plus récente que la dernière compilation).
"""
import argparse, pathlib, os, sys, subprocess, concurrent.futures, csv, time, hashlib
R=pathlib.Path(__file__).resolve().parent
JEU=R.parent
CORE_SRC=JEU/'core'/'src'/'main'/'java'
SRC=R/'src'/'main'/'java'
BUILD=R/'build'/'kit'
CORE=BUILD/'moteur';BIN=BUILD/'bots';STAMP=BUILD/'compilation.ok'
CP=R/'sauvegardes'/'reference'/'checkpoint_before_eighth.save'
CPS=os.pathsep.join(map(str,[CORE,BIN]));MODES='EXPERT_COMPLET DEPENDANCES ECONOMISTE DEBUTANT_APPRENANT GUIDE_UI RECUPERATION ADAPTATIF PLAN_PRESTIGE SANS_IMPULSIONS SANS_COMETES SANS_TECH SANS_LISTE'.split()

def javac(classpath,output,sources,label):
    output.mkdir(parents=True,exist_ok=True)
    # Un fichier d'arguments : gère Windows, les espaces et les longues listes de sources.
    argfile=BUILD/(label+'.args')
    args=['-encoding','UTF-8','-nowarn','-d',str(output)]+(['-cp',str(classpath)] if classpath else [])+[str(s) for s in sources]
    argfile.write_text('\n'.join('"'+a.replace('\\','/')+'"' if ' ' in a else a for a in args),encoding='utf-8')
    print('Compilation',label,':',len(sources),'sources Java...')
    ret=subprocess.run(['javac','@'+str(argfile)]).returncode
    if ret:sys.exit(ret)

def sources():
    core=sorted(CORE_SRC.rglob('*.java'));bots=sorted(SRC.glob('*.java'))
    if not core:sys.exit('MOTEUR INTROUVABLE : '+str(CORE_SRC))
    if not bots:sys.exit('SOURCES DES BOTS INTROUVABLES : '+str(SRC))
    return core,bots

def compile_bots():
    core,bots=sources()
    BUILD.mkdir(parents=True,exist_ok=True)
    javac(None,CORE,core,'moteur')
    javac(CORE,BIN,bots,'bots')
    STAMP.write_text(time.strftime('%Y-%m-%d %H:%M:%S'),encoding='utf-8')
    print('OK : moteur dans',CORE,'et bots dans',BIN)

def ensure():
    # Recompile si rien n'est compilé, ou si une source du jeu ou des bots a changé depuis.
    if not STAMP.exists():compile_bots();return
    core,bots=sources()
    last=STAMP.stat().st_mtime
    if any(p.stat().st_mtime>last for p in core+bots):
        print('Le jeu ou les bots ont changé : recompilation.');compile_bots()

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
    for name in ['v2','profils','fresh']:
        z=sub.add_parser(name,help={'v2':"le bot V2 (AdvancedLab) depuis une sauvegarde",'profils':"un des 24 profils depuis une sauvegarde (IdleProfiles24)",
            'fresh':"un profil depuis une partie neuve, les deux premiers actes seulement (FreshPhaseProfiles)"}[name])
        z.add_argument('--bot',default='DEPENDANCES',choices=MODES) if name=='v2' else z.add_argument('--id',type=int,choices=range(1,25),required=True)
        z.add_argument('--hours',type=float,default=12 if name!='fresh' else 4)
        z.add_argument('--step',type=float,default=20)
        z.add_argument('--seed',type=int,default=42)
        z.add_argument('--checkpoint',default=str(CP)) if name in ('v2','profils') else None
        # Version 0.130 : combien d'univers former ; au-delà du premier, l'univers suivant se lance dès l'univers formé.
        # Version 0.152 : jusqu'à 11 (de 4 à 11, le bot arrose la graine et joue l'arbre ; voir UniverseTreePolicy).
        # Pas pour « fresh » : sans le cosmos, l'univers ne se forme pas (l'audit depuis zéro joue le chemin complet).
        z.add_argument('--univers',type=int,choices=range(1,12),default=1) if name!='fresh' else None
        z.add_argument('--out',default=None)
    z=sub.add_parser('audit',help="parties neuves des profils jusqu'à l'univers visé (FreshUniverseAudit)")
    z.add_argument('--de',type=int,choices=range(1,25),default=1);z.add_argument('--a',type=int,choices=range(1,25),default=1)
    z.add_argument('--hours',type=float,default=168);z.add_argument('--step',type=float,default=20)
    z.add_argument('--univers',type=int,choices=range(1,12),default=1);z.add_argument('--strict',action='store_true')
    z.add_argument('--out',default=None)
    z=sub.add_parser('dimensions',help="un bot par dimension : le temps jusqu'au rond vert (DimensionBots)")
    z.add_argument('dimension',nargs='?',choices=['ligne','plan','volume','duree','toutes'],default='toutes')
    z.add_argument('--minutes',type=float,default=60)
    z=sub.add_parser('empreinte',help="joue les 4 parties de l'empreinte du moteur");z.add_argument('--out',required=True);z.add_argument('parties',nargs='*')
    z=sub.add_parser('comparer',help="compare deux dossiers d'empreintes (ou deux sauvegardes)");z.add_argument('a');z.add_argument('b')
    z=sub.add_parser('batch');z.add_argument('--tier',choices=['rapide','standard','approfondi'],required=True);z.add_argument('--workers',type=int,default=2);z.add_argument('--hours',type=float,default=None);z.add_argument('--step',type=float,default=20);z.add_argument('--seed-start',type=int,default=None);z.add_argument('--replay',action='store_true')
    a=par.parse_args()
    if a.command=='build':compile_bots();return
    if a.command=='verify':sys.exit(execute(['VerifyVictory',pathlib.Path(a.save).resolve()]))
    if a.command=='empreinte':
        out=pathlib.Path(a.out).resolve();os.chdir(R);sys.exit(execute(['Empreinte',out,*a.parties]))
    if a.command=='comparer':
        sys.exit(subprocess.run([sys.executable,str(R/'outils'/'comparer_sauvegardes.py'),a.a,a.b]).returncode)
    if a.command=='audit':
        folder=(pathlib.Path(a.out) if a.out else R/'sorties'/f'audit_{a.de:02d}-{a.a:02d}_univers{a.univers}').resolve()
        os.chdir(R);sys.exit(execute(['FreshUniverseAudit',a.de,a.a,a.hours,a.step,folder,str(a.strict).lower(),a.univers]))
    if a.command=='dimensions':
        os.chdir(R);sys.exit(execute(['DimensionBots',a.dimension,a.minutes]))
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
    else:folder=R/'sorties'/f'{a.command}_{getattr(a,"id",getattr(a,"bot",""))}_seed{a.seed}_{a.hours}h'
    folder.mkdir(parents=True,exist_ok=True)
    if a.command=='v2':cmd=['AdvancedLab',a.bot,a.checkpoint,a.hours,a.step,a.seed,folder,a.univers]
    elif a.command=='profils':cmd=['IdleProfiles24',a.id,a.checkpoint,a.hours,a.step,folder,a.seed,'late',a.univers]
    elif a.command=='fresh':cmd=['FreshPhaseProfiles',a.id,a.hours,a.step,folder,a.seed]
    code=execute(cmd,folder);print('Résultats:',folder);sys.exit(code)
if __name__=='__main__':main()

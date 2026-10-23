#!/usr/bin/env python3
"""Compare deux sauvegardes du jeu, ou deux dossiers d'empreintes (run.py empreinte).

Textes et entiers doivent être identiques ; les nombres réels, à un milliardième près (relatif). Pourquoi
pas au bit près : Math.pow, Math.log… peuvent différer du dernier chiffre selon le moment où le compilateur
de la JVM prend le relais. Deux exécutions du MÊME moteur ne donnent donc pas toujours les mêmes bits.

Usage : comparer_sauvegardes.py <a.save|dossier> <b.save|dossier>   (code de sortie 0 si tout est identique)
"""
import sys,re,os
from decimal import Decimal, getcontext, MAX_EMAX, MIN_EMIN
# Les exposants du jeu sont des long : sans cela, Decimal déborde au-delà de 10^999999.
getcontext().Emax=MAX_EMAX; getcontext().Emin=MIN_EMIN
def load(p):
    d={}
    for l in open(p,encoding='utf-8').read().split('\n'):
        if '=' in l: k,v=l.split('=',1); d[k]=v
    return d
num=re.compile(r'^-?\d+(\.\d+)?([eE][-+]?\d+)?$')
# Un entier écrit sans virgule ni exposant doit être identique au chiffre près (version 0.126 : avant, 1000000000 et
# 1000000001 passaient pour égaux, contre ce que dit l'en-tête).
integer=re.compile(r'^-?\d+$')
def same(x,y):
    if x==y: return True
    tx=re.split(r'([,:|;])',x); ty=re.split(r'([,:|;])',y)
    if len(tx)!=len(ty): return False
    for a,b in zip(tx,ty):
        if a==b: continue
        if integer.match(a) and integer.match(b): return False
        if num.match(a) and num.match(b):
            # Les grands nombres du jeu dépassent les double : Decimal les lit exactement, quel que soit l'exposant.
            # (Avant la 0.126, deux exposants écartés de 5 ou plus mettaient les deux mantisses à zéro : 1.5e10 et
            # 1.5e100 sortaient « identiques ».)
            da,db=Decimal(a),Decimal(b)
            if abs(da-db)<=Decimal('1e-9')*max(abs(da),abs(db)): continue
        return False
    return True
def compare(pa,pb):
    a,b=load(pa),load(pb)
    # Une clé absente d'un côté est une différence, même si l'autre côté la laisse vide (version 0.126).
    bad=[k for k in set(a)|set(b) if (k in a)!=(k in b) or not same(a.get(k,''),b.get(k,''))]
    print(os.path.basename(pa)+' : '+('IDENTIQUES' if not bad else 'DIFFERENTS : '+', '.join(sorted(bad)[:10])))
    return not bad
A,B=sys.argv[1],sys.argv[2]
if os.path.isdir(A):
    names=sorted(set(n for d in (A,B) for n in os.listdir(d) if n.endswith('.save')))
    missing=[n for n in names if not (os.path.exists(os.path.join(A,n)) and os.path.exists(os.path.join(B,n)))]
    for n in missing: print(n+' : ABSENTE d\'un des deux dossiers')
    ok=all([compare(os.path.join(A,n),os.path.join(B,n)) for n in names if n not in missing]) and bool(names) and not missing
else:
    ok=compare(A,B)
sys.exit(0 if ok else 1)

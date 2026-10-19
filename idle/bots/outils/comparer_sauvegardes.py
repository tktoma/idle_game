#!/usr/bin/env python3
"""Compare deux sauvegardes du jeu, ou deux dossiers d'empreintes (run.py empreinte).

Textes et entiers doivent être identiques ; les nombres réels, à un milliardième près (relatif). Pourquoi
pas au bit près : Math.pow, Math.log… peuvent différer du dernier chiffre selon le moment où le compilateur
de la JVM prend le relais. Deux exécutions du MÊME moteur ne donnent donc pas toujours les mêmes bits.

Usage : comparer_sauvegardes.py <a.save|dossier> <b.save|dossier>   (code de sortie 0 si tout est identique)
"""
import sys,re,os
def load(p):
    d={}
    for l in open(p,encoding='utf-8').read().split('\n'):
        if '=' in l: k,v=l.split('=',1); d[k]=v
    return d
num=re.compile(r'^-?\d+(\.\d+)?([eE][-+]?\d+)?$')
def same(x,y):
    if x==y: return True
    tx=re.split(r'([,:|;])',x); ty=re.split(r'([,:|;])',y)
    if len(tx)!=len(ty): return False
    for a,b in zip(tx,ty):
        if a==b: continue
        if num.match(a) and num.match(b):
            # Les grands nombres du jeu dépassent les double : mantisse et exposant à part.
            def split(t):
                m,_,e=t.lower().partition('e'); return float(m),int(e or 0)
            (ma,ea),(mb,eb)=split(a),split(b)
            if ea!=eb:
                ma*=10.0**(ea-min(ea,eb)) if abs(ea-eb)<5 else 0; mb*=10.0**(eb-min(ea,eb)) if abs(ea-eb)<5 else 0
            if abs(ma-mb)<=1e-9*max(abs(ma),abs(mb),1e-300): continue
        return False
    return True
def compare(pa,pb):
    a,b=load(pa),load(pb)
    bad=[k for k in set(a)|set(b) if not same(a.get(k,''),b.get(k,''))]
    print(os.path.basename(pa)+' : '+('IDENTIQUES' if not bad else 'DIFFERENTS : '+', '.join(sorted(bad)[:10])))
    return not bad
A,B=sys.argv[1],sys.argv[2]
if os.path.isdir(A):
    names=sorted(n for n in os.listdir(A) if n.endswith('.save'))
    ok=all([compare(os.path.join(A,n),os.path.join(B,n)) for n in names]) and bool(names)
else:
    ok=compare(A,B)
sys.exit(0 if ok else 1)

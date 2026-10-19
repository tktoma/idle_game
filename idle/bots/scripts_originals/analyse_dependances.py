import csv,collections,pathlib,json
b=pathlib.Path('/mnt/data/idle_lab_v2/data/dependances')
with (b/'dependances.csv').open() as f: edges=list(csv.DictReader(f))
with (b/'objets_catalogue.csv').open() as f: obj=list(csv.DictReader(f))
names={(o['type'],o['id']):o['nom'] for o in obj}
links=collections.defaultdict(list); incoming=collections.defaultdict(list)
for e in edges:
 a=(e['source_type'],e['source_id']); t=(e['cible_type'],e['cible_id']);links[a].append(t);incoming[t].append(a)
@__import__('functools').lru_cache(None)
def depth(v):return 1+max((depth(p) for p in incoming[v]),default=0)
@__import__('functools').lru_cache(None)
def descendants(v):
 s=set()
 for t in links[v]:s.add(t);s.update(descendants(t))
 return frozenset(s)
out=[]
for item in obj:
 k=(item['type'],item['id']); out.append({'type':k[0],'id':k[1],'nom':item['nom'],'dependants_directs':len(set(links[k])),'dependants_indirects':len(descendants(k)),'profondeur_chaine':depth(k)})
with (b/'centralite.csv').open('w',encoding='utf-8-sig',newline='') as f:
 w=csv.DictWriter(f,fieldnames=list(out[0]));w.writeheader();w.writerows(sorted(out,key=lambda x:(-x['dependants_indirects'],-x['dependants_directs'])))
for typ in ('MOLECULE','ASSEMBLAGE','ASTRE'):
 ranked=[x for x in out if x['type']==typ]
 print(typ,'TOP',[(x['nom'],x['dependants_directs'],x['dependants_indirects']) for x in sorted(ranked,key=lambda v:-v['dependants_indirects'])[:8]])
print('edges',len(edges),'maxdepth',max(o['profondeur_chaine'] for o in out))
(b/'resume_graphe.json').write_text(json.dumps({'noeuds':len(obj),'dependances':len(edges),'profondeur_max':max(o['profondeur_chaine'] for o in out),'type_note':'Les arcs de MATIERE ne sont pas des objets du catalogue, et certains bonus/delais ne sont pas representes. Les ressources par assemblage ne sont pas consommees.'},ensure_ascii=False,indent=2))

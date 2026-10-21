# Audit des 24 bots depuis une partie neuve

La campagne GitHub Actions **Audit 24 bots depuis zero** joue les 24 profils
avec 24 parties **neuves et independantes**, sur le moteur actuel.

- Graines fixes : `200 + id` ; même comportement et mêmes intervalles d'actions
  que `IdleProfiles24`, y compris les absences et le plafond de 24 h hors ligne.
- Le bot s'arrête dès que l'univers est formé ; en cas de blocage détecté par
  `ProfileProgressGuard`, il s'arrête en signalant **BLOCKED**.
- Les états incohérents ou les exceptions sont **ERROR**.
- Une simulation qui continue sans victoire s'arrête au plafond de sécurité
  (par défaut **168 heures simulées**, soit sept jours) avec **LIMIT**, pas **WIN**.
  Ce plafond peut être augmenté lors d'une relance manuelle, mais GitHub limite
  aussi chaque job à 300 minutes d'exécution réelle.
- Quatre jobs indépendants couvrent tous les IDs : 1–6, 7–12, 13–18 et 19–24.
  Les tests ordinaires restent séparés de cette campagne lourde.
- Le job est **rouge si au moins un profil ne termine pas l'univers**, même
  lorsque tous les profils ont tourné sans planter.

Chaque profil produit un `diagnostic.txt` (jalon final, succès, défis, ressources,
production, objectif et verdict) et une sauvegarde `end.save`. La commande du
bot produit aussi son historique CSV si elle termine normalement. Une erreur
captée par le garde-fou peut interrompre ce dernier historique, mais le
diagnostic et la sauvegarde finale restent produits par l'auditeur.

Les fichiers `resume_01-06.csv`, etc., ainsi que les sauvegardes sont
conservés dans les artefacts des jobs, y compris quand la campagne échoue.
Les lignes `FRESH_AUDIT` / `FRESH_AUDIT_BILAN` résument les résultats
dans les logs. Un job vert signifie ici **victoire de tous les profils testés**,
pas seulement l'absence de crash.

Lancer localement depuis `idle/` (Java 21) :

```bash
sh ./gradlew :bots:bot -Pclasse=FreshUniverseAudit --args="1 6 168 60 sorties/audit-depuis-zero"
```

L'audit ne corrige pas automatiquement les stratégies. À ce stade, une partie
neuve peut rester bloquée avant le premier Big Bang ; il faut lire les états
`LIMIT` / `BLOCKED` pour identifier les stratégies à améliorer.

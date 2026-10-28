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
  Ce plafond peut être augmenté lors d'une relance manuelle, mais le workflow limite
  aussi chaque job à 300 minutes d'exécution réelle (360, le maximum de GitHub, quand
  l'option `univers` vaut 2 ou 3). Le plafond en heures simulées est global à l'essai,
  quel que soit le nombre d'univers visés.
- Huit jobs indépendants de trois profils couvrent tous les IDs : 1–3, 4–6, 7–9,
  10–12, 13–15, 16–18, 19–21 et 22–24 (les segments de six dépassaient 5 h en CI).
  Les tests ordinaires restent séparés de cette campagne lourde.
- Depuis la version 0.126, un job est **rouge si un profil est BLOCKED ou ERROR**.
  Un profil seulement trop lent (**LIMIT**) est signalé par une ligne
  `FRESH_AUDIT_AVERTISSEMENT` sans faire échouer le job : avant, les profils 17 et
  21, connus pour atteindre le plafond, rendaient la campagne rouge à chaque fois.
  Le choix `strict = true` d'un lancement manuel rétablit l'exigence d'une
  victoire de chaque profil.
- La campagne démarre d'elle-même sur `master` quand elle change
  (`.github/workflows/fresh-universe-audit.yml` ou `FreshUniverseAudit.java`),
  et se relance à la main depuis l'onglet Actions.

Chaque profil produit un `diagnostic.txt` (jalon final, succès, défis, ressources,
production, objectif et verdict) et une sauvegarde `end.save`. La commande du
bot produit aussi son historique CSV si elle termine normalement. Une erreur
captée par le garde-fou peut interrompre ce dernier historique, mais le
diagnostic et la sauvegarde finale restent produits par l'auditeur.

Les fichiers `resume_01-03.csv`, `resume_04-06.csv`, etc., ainsi que les
sauvegardes sont conservés dans les artefacts des jobs, y compris quand la
campagne échoue. Les lignes `FRESH_AUDIT` / `FRESH_AUDIT_BILAN` résument les
résultats dans les logs. Un job vert signifie **aucun blocage ni erreur** ; en
mode strict, **victoire de tous les profils testés**.

Lancer localement depuis `idle/` (Java 21) :

```bash
sh ./gradlew :bots:bot -Pclasse=FreshUniverseAudit --args="1 3 168 60 sorties/audit-depuis-zero"
# mode strict : chaque profil doit former son univers
sh ./gradlew :bots:bot -Pclasse=FreshUniverseAudit --args="1 3 168 60 sorties/audit-depuis-zero true"
```

L'audit ne corrige pas automatiquement les stratégies : il faut lire les états
`LIMIT` / `BLOCKED` pour identifier celles à améliorer.

Depuis le 22 octobre 2026 (ordre d'ouverture du modèle 0.123 et explosions
suspendues pendant la construction du ciel, voir `README.md`), les profils partis
de zéro forment l'univers. Avant, le profil 1 restait au 5e Big Bang avec 3 astres
mûrs après 21 h. Sur le moteur de la version 0.125 (rééquilibrée), les profils 1
(Speedrunner) et 2 (Veteran) forment l'univers en 27 h 23 et 28 h 28 de jeu, avec
une décision toutes les 20 s (la mesure de référence de l'équilibrage, graine 201
pour le profil 1). Les 16 h 26 et 17 h 37 mesurés le 22 octobre valaient pour la 0.124.

Le pas de décision de la campagne CI est de 60 s (quatrième argument), pas de 20 s :
ses durées ne se comparent donc pas directement à celles de la référence.

# Vérification rapide du kit : compile le moteur du jeu et les bots, vérifie une victoire, lance deux bots une minute.
Set-Location -Path $PSScriptRoot
python run.py build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
python run.py verify --save "sauvegardes/reference/checkpoint_universe_5s.save"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
python run.py v2 --bot DEPENDANCES --hours 0.016667 --step 20 --seed 42 --out sorties/smoke_v2
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
python run.py fresh --id 1 --hours 0.016667 --step 20 --seed 42 --out sorties/smoke_fresh
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Tous les tests rapides ont reussi"

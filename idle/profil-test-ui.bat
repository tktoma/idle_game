@echo off
rem Lance le profil test UI : une partie preparee, sans gains ni sauvegarde, et le panneau qui passe chaque
rem combinaison de skins d un onglet par chacun de ses etats (version 0.248).
cd /d "%~dp0"
call gradlew.bat runTestUi

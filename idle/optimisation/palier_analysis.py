#!/usr/bin/env python3
"""
Analyse des paliers du défi final.
Ce script utilise les sauvegardes de bot et les logs pour déterminer les temps actuels
et calculer le facteur d'échelle nécessaire pour atteindre 15-20 min au lieu du temps actuel.

Usage : python idle/optimisation/palier_analysis.py [dossier-sauvegardes]
"""

import sys
import os
import csv
from pathlib import Path

def main():
    # Chemin vers les sauvegardes des bots
    save_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("idle/bots/sauvegardes")
    
    print(f"=== Analyse des paliers du défi final ===\n")
    print(f"Cherche dans : {save_dir}\n")
    
    # Chercher les sauvegardes marquées comme "final tier X" ou contenant le défi final
    save_files = list(save_dir.glob("**/*.save"))
    
    if not save_files:
        print("Aucune sauvegarde trouvée. À faire : lancer le bot analyste jusqu'au défi final.")
        return
    
    print(f"Trouvé {len(save_files)} sauvegardes\n")
    
    # Trouver la sauvegarde la plus récente avec le défi final
    latest_final = None
    for sf in save_files:
        try:
            with open(sf, 'r') as f:
                lines = f.readlines()
                for i, line in enumerate(lines):
                    if 'defiFinal.ouvert' in line or 'defi.final' in line.lower():
                        # C'est un moment marquant
                        latest_final = sf
                        break
            if latest_final:
                break
        except:
            continue
    
    if latest_final:
        print(f"Analyse de : {latest_final}")
        
        # Lire les moments notés
        moments = []
        with open(latest_final, 'r') as f:
            for i, line in enumerate(f):
                parts = line.strip().split(',')
                if len(parts) >= 4:
                    try:
                        time_sec = float(parts[0])
                        moment_id = parts[3]
                        if 'defi.final' in moment_id.lower() or 'tier' in moment_id.lower():
                            moments.append((time_sec, moment_id))
                    except (ValueError, IndexError):
                        continue
        
        print(f"\nMoments du défi final :")
        for time_sec, moment_id in sorted(moments):
            print(f"  {time_sec:.2f}s : {moment_id}")
        
        # Calculer le facteur d'échelle
        if len(moments) >= 10:
            last_time = moments[-1][0]
            target_time = 20 * 60  # 20 minutes en secondes
            scale_factor = target_time / last_time
            
            print(f"\n=== Résultats ===")
            print(f"Temps actuel : {last_time:.2f}s ({last_time/60:.2f} min)")
            print(f"Temps cible : {target_time:.2f}s ({target_time/60:.0f} min)")
            print(f"Facteur d'échelle : {scale_factor:.4f} (x{1/scale_factor:.2f})")
            
            # Écrire les paliers projetés
            output_dir = save_dir.parent / "paliers-analysis"
            output_dir.mkdir(parents=True, exist_ok=True)
            
            with open(output_dir / "paliers-projetes.txt", 'w') as f:
                f.write(f"# Paliers du défi final avec facteur d'échelle {scale_factor:.4f}\n\n")
                for i, (time_sec, moment_id) in enumerate(moments[:10]):
                    projected_time = time_sec * scale_factor
                    dim_name = "dim" + str(i+1) if 'dim' in moment_id else f"palier_{i+1}"
                    f.write(f"{projected_time:.2f}s - {moment_id}\n")
            
            print(f"\nRésultats écrits dans : {output_dir / 'paliers-projetes.txt'}")
    else:
        print("Aucune sauvegarde de défi final trouvée.")

if __name__ == "__main__":
    main()

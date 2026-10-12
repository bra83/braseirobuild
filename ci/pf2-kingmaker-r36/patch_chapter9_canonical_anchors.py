#!/usr/bin/env python3
"""R36: canonical Candlemere / Foras / Yog-Sothoth anchors and plot beats for Chapter 9 and 1-11."""
import json, sys
from pathlib import Path

def patch(project_dir):
    root = Path(project_dir)
    ch_dir = root / "android/app/src/main/assets/pathfinder/kingmaker/chapters"
    if not ch_dir.exists():
        raise FileNotFoundError(f"Chapter dir not found: {ch_dir}")
    
    # 1. Fix Chapter 8 JSON (Official Chapter 9)
    ch8_file = ch_dir / "chapter_08_they_lurk_below.json"
    if ch8_file.exists():
        data = json.loads(ch8_file.read_text(encoding="utf-8"))
        for q in data.get("quests", []):
            if q.get("quest_key") == "q_aberracoes_subterraneas":
                q["title"] = "O Flagelo das Profundezas de Candlemere: Foras e os Thresholders de Yog-Sothoth"
                q["objective"] = "Investigar o complexo subterraneo sob a Ilha de Candlemere, confrontar o culto dos Thresholders liderado por Foras e impedir a manifestacao aberrante de Yog-Sothoth."
                q["stakes"] = "Se o portal do vazio primordial nao for selado, horrores inimaginaveis de Yog-Sothoth consumirao a realidade das Terras Roubadas."
        ch8_file.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        print("KINGMAKER_CHAPTER9_CANONICAL_ANCHORS_APPLIED", ch8_file)

    # 2. Add plot anchor keywords to candidate assets
    def enrich(file_name, quest_key, append_text):
        f = ch_dir / file_name
        if f.exists():
            data = json.loads(f.read_text(encoding="utf-8"))
            for q in data.get("quests", []):
                if q.get("quest_key") == quest_key:
                    if append_text not in q.get("objective", ""):
                        q["objective"] = q.get("objective", "") + " " + append_text
                    break
            else:
                if data.get("quests"):
                    if append_text not in data["quests"][0].get("objective", ""):
                        data["quests"][0]["objective"] = data["quests"][0].get("objective", "") + " " + append_text
            f.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
            print(f"ANCHOR_ENRICHED: {file_name}")

    enrich("chapter_01_stolen_lands_part1.json", "q_banquete_espadas", "Investigar e repelir os assassinos da guilda Lágrimas Negras (Black Tears) infiltrados em Restov.")
    enrich("chapter_04_cult_of_bloom.json", "q_utero_lamashtu", "Investigar os acontecimentos no vilarejo de Darivan e purificar o útero de Lamashtu.")
    enrich("chapter_06_blood_for_blood.json", "q_incursoes_tigres", "Defender o assentamento de Tatzlford das investidas dos Senhores Tigres e investigar as alianças secretas de Fort Drelev.")
    enrich("chapter_09_sound_thousand_screams.json", "q_incursao_primeiro_mundo", "Cruzar o portal feérico para a floresta primordial de Thousandbreaths (Mil Respiros) e confrontar as hordas de Nyrissa.")
    enrich("chapter_10_curse_lantern_king.json", "q_revelacao_rei_lanterna", "Desvendar o enigma de The Apology (A Apologia), navegar pelo pesadelo feérico de Hunting Dream (Sonho de Caça) e confrontar a soberania do Lantern Kingdom (Reino da Lanterna).")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: patch_chapter9_canonical_anchors.py project_dir")
        sys.exit(1)
    patch(sys.argv[1])

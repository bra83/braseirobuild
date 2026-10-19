#!/usr/bin/env python3
"""Kingmaker Turn & Leadership Checklist patch for Pathfinder 2e R36.

Adds KingmakerTurnChecklist with all 63 activities, PT-BR translations,
authorized roles, and [i] info dialogs to the Adventure and Kingmaker screens.
"""
from pathlib import Path
import shutil
import sys

def patch_checklist(project_dir: Path):
    project_dir = Path(project_dir).resolve()
    java_dir = project_dir / "android/app/src/main/java/com/braseiro/pathfinder2e"
    if not java_dir.exists():
        raise FileNotFoundError(f"Java source directory missing: {java_dir}")

    # 1. Copy KingmakerTurnChecklist.java
    source_checklist = Path(__file__).parent / "semantic/KingmakerTurnChecklist.java"
    if not source_checklist.exists():
        # Fallback to inspection directory if running from different root
        source_checklist = Path(__file__).resolve().parents[1] / "ci/pf2-r36-source-inspection/android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerTurnChecklist.java"
    if not source_checklist.exists():
        raise FileNotFoundError(f"Source KingmakerTurnChecklist.java missing: {source_checklist}")

    target_checklist = java_dir / "KingmakerTurnChecklist.java"
    shutil.copy2(source_checklist, target_checklist)

    # 2. Patch MainActivity.java
    main_file = java_dir / "MainActivity.java"
    content = main_file.read_text(encoding="utf-8")

    # Anchor in showCampaign()
    anchor_campaign = 'else hero.addView(text("Atlas ainda não vinculado — a aventura pode começar normalmente.",11,HINT,false));b.addView(hero);'
    replacement_campaign = anchor_campaign + '\n' + (
        '        LinearLayout checklistCard=card();'
        'checklistCard.addView(text("TURNO DO REINO • CHECKLIST",12,GOLD,true));'
        'checklistCard.addView(text("Atividades oficiais de Liderança, Comércio, Manutenção, Região, Cívica e Exército. Clique no [ ℹ ] em cada tópico para ver regras e líderes autorizados.",12,MUTED,false));'
        'Button btnChecklist=button("📋 ABRIR CHECKLIST DO REINO",GOLD);'
        'btnChecklist.setTextColor(Color.BLACK);'
        'btnChecklist.setOnClickListener(new View.OnClickListener(){public void onClick(View v){KingmakerTurnChecklist.showDialog(MainActivity.this);}});'
        'checklistCard.addView(btnChecklist);'
        'b.addView(checklistCard);'
    )

    if anchor_campaign not in content and 'KingmakerTurnChecklist.showDialog' not in content:
        raise ValueError("Could not find anchor in showCampaign() to insert KingmakerTurnChecklist button")

    if 'KingmakerTurnChecklist.showDialog' not in content:
        content = content.replace(anchor_campaign, replacement_campaign, 1)

    # Also add a button in showKingmaker() right after hero
    anchor_km = '"+kingmakerRepository.claimedHexCount()+" hex(es) reivindicado(s)",13,TEXT,false));b.addView(hero);'
    replacement_km = anchor_km + '\n' + (
        '        Button btnChecklistKm=button("📋 CHECKLIST DO TURNO DO REINO",PANEL2);'
        'btnChecklistKm.setOnClickListener(new View.OnClickListener(){public void onClick(View v){KingmakerTurnChecklist.showDialog(MainActivity.this);}});'
        'b.addView(btnChecklistKm);'
    )

    if anchor_km in content and 'btnChecklistKm' not in content:
        content = content.replace(anchor_km, replacement_km, 1)

    main_file.write_text(content, encoding="utf-8")
    print(f"R36_KINGMAKER_CHECKLIST_PATCHED={target_checklist}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: patch_kingmaker_turn_checklist.py <project_root>", file=sys.stderr)
        sys.exit(1)
    patch_checklist(Path(sys.argv[1]))

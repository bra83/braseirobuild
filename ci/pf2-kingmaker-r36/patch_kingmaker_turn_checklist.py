#!/usr/bin/env python3
"""Kingmaker Turn & Leadership Checklist patch for Pathfinder 2e R36.

Adds KingmakerTurnChecklist with all 63 activities, PT-BR translations,
authorized roles, and [i] info dialogs to the Adventure and Kingmaker screens.
"""
from pathlib import Path
import shutil
import sys

def patch_player_initiated_dice(java_dir: Path):
    """Require a deliberate touch before the existing face-up 3D physics starts.

    This reuses PhysicalDiceEngine and Dice3DOverlay; no alternative random
    resolver or fabricated face is introduced. Fail closed on source drift.
    """
    overlay = java_dir / "Dice3DOverlay.java"
    if not overlay.is_file():
        raise FileNotFoundError(f"Physical dice overlay missing: {overlay}")
    code = overlay.read_text(encoding="utf-8")
    if "BRASEIRO_DICE3D_EXPLICIT_USER_LAUNCH_V1" in code:
        return
    replacements = [
        (
            'public static boolean enabled(SharedPreferences prefs){return prefs.getBoolean("dice3d_enabled_v1",true);}',
            'public static boolean enabled(SharedPreferences prefs){return true;}',
            "REQUIRE_PHYSICAL_DICE",
        ),
        (
            'final TextView resultView=label(activity,"Dados em movimento…",20,true);',
            'final TextView resultView=label(activity,"Aguardando seu toque em LANÇAR DADOS.",20,true);',
            "VISIBLE_ROLL_REQUEST",
        ),
        (
            'final Button apply=new Button(activity);',
            '// BRASEIRO_DICE3D_EXPLICIT_USER_LAUNCH_V1\n'
            '        final Button launch=new Button(activity);'
            'launch.setText("LANÇAR DADOS 3D");'
            'launch.setContentDescription("Iniciar lançamento físico 3D, iniciado pelo jogador");'
            'bottom.addView(launch,new LinearLayout.LayoutParams(-1,dp(activity,50)));\n'
            '        final Button apply=new Button(activity);',
            "LAUNCH_BUTTON",
        ),
        (
            'final boolean[] done={false};final long[] lastNs={System.nanoTime()};',
            'final boolean[] started={false};final boolean[] done={false};final long[] lastNs={System.nanoTime()};',
            "INITIATION_STATE",
        ),
        (
            'if(done[0]||!dialog.isShowing())return;long now=System.nanoTime();',
            'if(done[0]||!dialog.isShowing()||!started[0])return;long now=System.nanoTime();',
            "TICKER_FAIL_CLOSED",
        ),
        (
            'cancel.setOnClickListener(new View.OnClickListener()',
            'launch.setOnClickListener(new View.OnClickListener(){public void onClick(View v){'
            'if(started[0]||done[0])return;started[0]=true;'
            'launch.setVisibility(View.GONE);resultView.setText("Dados em movimento…");'
            'lastNs[0]=System.nanoTime();diceView.postOnAnimation(ticker);}});\n'
            '        cancel.setOnClickListener(new View.OnClickListener()',
            "REQUIRE_EXPLICIT_TAP",
        ),
        (
            'ww.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);diceView.postOnAnimation(ticker);',
            'ww.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);',
            "NO_ROLL_ON_DIALOG_SHOW",
        ),
    ]
    for original, replacement, label in replacements:
        count = code.count(original)
        if count != 1:
            raise RuntimeError(f"R36_DICE3D_SOURCE_DRIFT_{label}: expected one anchor, got {count}")
        code = code.replace(original, replacement, 1)
    overlay.write_text(code, encoding="utf-8")
    print("R36_DICE3D_USER_LAUNCH_PATCHED=PASS")


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
        'Button advisorBtn=button(KingmakerTurnChecklist.hasUrgentAdvisorTip(kingmakerRepository)?'
        '"❗ CONSELHEIRO • ALERTA":"ℹ CONSELHEIRO VIVO",PANEL2);'
        'advisorBtn.setOnClickListener(new View.OnClickListener(){public void onClick(View v){'
        'KingmakerTurnChecklist.showAdvisorDialog(MainActivity.this,kingmakerRepository);}});'
        'checklistCard.addView(advisorBtn);'
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
        'Button advisorBtnKm=button(KingmakerTurnChecklist.hasUrgentAdvisorTip(kingmakerRepository)?'
        '"❗ CONSELHEIRO • ALERTA":"ℹ CONSELHEIRO VIVO",PANEL2);'
        'advisorBtnKm.setOnClickListener(new View.OnClickListener(){public void onClick(View v){'
        'KingmakerTurnChecklist.showAdvisorDialog(MainActivity.this,kingmakerRepository);}});'
        'b.addView(advisorBtnKm);'
    )

    if anchor_km in content and 'btnChecklistKm' not in content:
        content = content.replace(anchor_km, replacement_km, 1)

    if 'KingmakerTurnChecklist.showAdvisorDialog(MainActivity.this,kingmakerRepository)' not in content:
        raise RuntimeError("R36_ADVISOR_BLOCKED: contextual guide was not wired to the active screen")
    main_file.write_text(content, encoding="utf-8")
    patch_player_initiated_dice(java_dir)
    print(f"R36_KINGMAKER_CHECKLIST_PATCHED={target_checklist}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: patch_kingmaker_turn_checklist.py <project_root>", file=sys.stderr)
        sys.exit(1)
    patch_checklist(Path(sys.argv[1]))

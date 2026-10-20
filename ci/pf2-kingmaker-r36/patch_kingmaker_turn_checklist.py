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


def patch_no_invisible_kingdom_dice(java_dir: Path):
    """Reject legacy implicit kingdom RNG; callers must supply visible dice."""
    path = java_dir / "KingmakerRepository.java"
    if not path.is_file():
        raise FileNotFoundError(path)
    s = path.read_text(encoding="utf-8")
    changes = [
        (
            '''public JSONObject resolveQuellUnrest(int naturalRoll,String skillKey,String notes)throws Exception{
        java.util.Random dice=new java.util.Random();
        return resolveQuellUnrest(naturalRoll,skillKey,dice.nextInt(6)+1,dice.nextInt(4)+1,
            "unrest","","",notes);
    }''',
            '''public JSONObject resolveQuellUnrest(int naturalRoll,String skillKey,String notes)throws Exception{
        throw new IllegalStateException("Acalmar Agitação: informe o d6 e o d4 lançados fisicamente e escolha a consequência antes de confirmar.");
    }''',
            "QUELL_UNREST_LEGACY",
        ),
        (
            '''public JSONObject resolveRepairStructure(String settlementKey,String structureKey,String notes)throws Exception{
        return resolveRepairStructure(settlementKey,structureKey,new java.util.Random().nextInt(20)+1,"engineering",notes);
    }''',
            '''public JSONObject resolveRepairStructure(String settlementKey,String structureKey,String notes)throws Exception{
        throw new IllegalStateException("Reparar Estrutura: é obrigatório lançar e confirmar o d20 físico antes da resolução.");
    }''',
            "REPAIR_LEGACY",
        ),
        (
            '''JSONObject choices=new JSONObject().put("die1",new java.util.Random().nextInt(4)+1).put("die2",new java.util.Random().nextInt(4)+1);
        return resolveBuildStructure(settlementKey,structureKey,new java.util.Random().nextInt(20)+1,skill,notes,"",choices);''',
            '''throw new IllegalStateException("Construir Estrutura: use o procedimento auditado com d20 e d4 físicos, não o atalho legado.");''',
            "BUILD_LEGACY",
        ),
        (
            'p.optInt("critical_success_d6",new java.util.Random().nextInt(6)+1)',
            'p.optInt("critical_success_d6",-1)',
            "QUELL_DISPATCH_D6",
        ),
        (
            'p.optInt("critical_failure_d4",new java.util.Random().nextInt(4)+1)',
            'p.optInt("critical_failure_d4",-1)',
            "QUELL_DISPATCH_D4",
        ),
    ]
    for old, new, label in changes:
        count=s.count(old)
        if count!=1:
            raise RuntimeError(f"R36_HIDDEN_DICE_DRIFT_{label}: expected one anchor, got {count}")
        s=s.replace(old,new,1)
    path.write_text(s,encoding="utf-8")
    print("R36_LEGACY_HIDDEN_KINGDOM_DICE_BLOCKED=PASS")


def patch_visible_construction_dice(java_dir: Path):
    """Construction is committed only after both real 3D rolls AND approval."""
    path=java_dir/"MainActivity.java"
    if not path.is_file():
        raise FileNotFoundError(path)
    s=path.read_text(encoding="utf-8")
    old_input='final EditText roll=labeledField(form,"d20 de construção",String.valueOf(new java.util.Random().nextInt(20)+1),false);'
    new_input='final EditText roll=labeledField(form,"d20 de construção • será lançado em 3D","",false);roll.setEnabled(false);'
    if s.count(old_input)!=1:
        raise RuntimeError(f"R36_CONSTRUCTION_HIDDEN_D20_DRIFT: {s.count(old_input)}")
    s=s.replace(old_input,new_input,1)
    start_marker="JSONObject result=kingmakerRepository.resolveBuildStructure("
    end_marker='''                        showKingmaker();
                        toast("Construção resolvida: "+result.optString("construction_status","resultado registrado")+".");'''
    if s.count(start_marker)!=1 or s.count(end_marker)!=1:
        raise RuntimeError("R36_CONSTRUCTION_UI_DRIFT: expected exactly one real construction resolver")
    a=s.index(start_marker);b=s.index(end_marker,a)+len(end_marker)
    original=s[a:b]
    if original.count("new Random().nextInt(4)+1")!=2:
        raise RuntimeError("R36_CONSTRUCTION_HIDDEN_D4_DRIFT")
    if original.count("n,skill,value(notes),commodityChoice")!=1:
        raise RuntimeError("R36_CONSTRUCTION_D20_BINDING_DRIFT")
    invocation=original.replace("n,skill,value(notes),commodityChoice",
                                "physicalD20.natural,skill,value(notes),commodityChoice",1)
    invocation=invocation.replace("new Random().nextInt(4)+1","physicalD4.rolls.get(0)",1)
    invocation=invocation.replace("new Random().nextInt(4)+1","physicalD4.rolls.get(1)",1)
    approved=invocation.replace("\n","\n                                    ")
    replacement='''// R36_VISIBLE_CONSTRUCTION_DICE_V1: no seeded hidden d20/d4.
                        Dice3DOverlay.roll(MainActivity.this,prefs,"1d20","Normal","Construção do Reino",
                            new Dice3DOverlay.Callback(){
                                @Override public void onResult(final Dice3DOverlay.RollResult physicalD20){
                                    Dice3DOverlay.roll(MainActivity.this,prefs,"2d4","Normal","Consequências da construção",
                                        new Dice3DOverlay.Callback(){
                                            @Override public void onResult(final Dice3DOverlay.RollResult physicalD4){
                                                if(physicalD4.rolls.size()!=2){toast("Os dois d4 não foram concluídos.");return;}
                                                new AlertDialog.Builder(MainActivity.this)
                                                    .setTitle("AUDITORIA DA CONSTRUÇÃO")
                                                    .setMessage("d20: "+physicalD20.natural+"\\n2d4: "+physicalD4.rolls+
                                                        "\\n\\nAprovar a aplicação das regras e dos custos oficiais ao Reino?")
                                                    .setNegativeButton("RECUSAR",null)
                                                    .setPositiveButton("APROVAR",new DialogInterface.OnClickListener(){
                                                        @Override public void onClick(DialogInterface audit,int button){
                                                            try{
                                    '''+approved+'''
                                                            }catch(Exception ex){toast("Construção oficial: "+ex.getMessage());}
                                                        }
                                                    }).show();
                                            }
                                            @Override public void onError(String message){toast("Dados de consequência: "+message);}
                                            @Override public void onCancel(){toast("Construção cancelada: os dados não foram aprovados.");}
                                        });
                                }
                                @Override public void onError(String message){toast("Dado de construção: "+message);}
                                @Override public void onCancel(){toast("Construção cancelada: sem rolagem física.");}
                            });'''
    s=s[:a]+replacement+s[b:]
    if "R36_VISIBLE_CONSTRUCTION_DICE_V1" not in s:
        raise RuntimeError("R36_VISIBLE_CONSTRUCTION_MARKER_MISSING")
    path.write_text(s,encoding="utf-8")
    print("R36_CONSTRUCTION_DICE_PHYSICAL_AND_AUDITED=PASS")


# Every necessary roll starts visibly. No separate approval of a settled die face.
# This changes the production overlay, not the pristine R35 source ZIP.
DICE_OVERLAY = Path("android/app/src/main/java/com/braseiro/pathfinder2e/Dice3DOverlay.java")

def transform_visible_dice(source: str) -> str:
    if "R36_VISIBLE_AUTOMATIC_PHYSICAL_DICE" in source:
        raise RuntimeError("Visible physical dice patch has already been applied.")
    changes = (
        (
            'final Button apply=new Button(activity);apply.setText("APLICAR RESULTADO");apply.setVisibility(View.GONE);bottom.addView(apply,new LinearLayout.LayoutParams(-1,dp(activity,50)));',
            'final TextView autoResult=label(activity,"A rolagem sera registrada automaticamente apos os dados pararem.",12,false);autoResult.setGravity(Gravity.CENTER);bottom.addView(autoResult);',
        ),
        (
            'final Button cancel=new Button(activity);cancel.setText("CANCELAR ROLAGEM");bottom.addView(cancel,new LinearLayout.LayoutParams(-1,dp(activity,50)));',
            'final Button cancel=new Button(activity);cancel.setText("DADOS EM MOVIMENTO");cancel.setEnabled(false);bottom.addView(cancel,new LinearLayout.LayoutParams(-1,dp(activity,50)));',
        ),
        (
            'apply.setVisibility(View.VISIBLE);cancel.setText("DESCARTAR");apply.setOnClickListener(new View.OnClickListener(){public void onClick(View v){dialog.dismiss();callback.onResult(new RollResult(rolls,physicalFaces,resolved.total,resolved.natural,resolvedDetail,resolved.critical,spec.formula,spec.mode));}});',
            '// R36_VISIBLE_AUTOMATIC_PHYSICAL_DICE: physics settles before rule result.\\n'
            'cancel.setText("RESULTADO OBTIDO");diceView.postDelayed(new Runnable(){public void run(){'
            'if(!dialog.isShowing())return;dialog.dismiss();callback.onResult(new RollResult(rolls,physicalFaces,resolved.total,resolved.natural,resolvedDetail,resolved.critical,spec.formula,spec.mode));'
            '}},1300L);',
        ),
    )
    for before, after in changes:
        if source.count(before) != 1:
            raise RuntimeError("Visible physical dice production anchor missing or duplicated: " + before[:78])
        source = source.replace(before, after, 1)
    for evidence in ("PhysicalDiceEngine.create(physicalFormula,seed)", "if(session.isComplete())",
                     "resultView.setText(resolvedDetail)", "DiceRollSpec.Result resolved=spec.resolve(rolls)",
                     "callback.onResult(new RollResult("):
        if evidence not in source:
            raise RuntimeError("Physical dice engine/result invariant missing: " + evidence)
    if source.count("callback.onResult(new RollResult(") != 1:
        raise RuntimeError("Physical result must be handed off exactly once.")
    return source


def patch_checklist(project_dir: Path):
    project_dir = Path(project_dir).resolve()
    java_dir = project_dir / "android/app/src/main/java/com/braseiro/pathfinder2e"
    if not java_dir.exists():
        raise FileNotFoundError(f"Java source directory missing: {java_dir}")

    # Validate and stage the visible physical-dice transformation first.
    overlay_path = project_dir / DICE_OVERLAY
    if not overlay_path.is_file():
        raise FileNotFoundError(f"Real R36 Dice3DOverlay missing: {overlay_path}")
    overlay_new = transform_visible_dice(overlay_path.read_text(encoding="utf-8"))

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
    overlay_path.write_text(overlay_new, encoding="utf-8")
    print("R36_VISIBLE_PHYSICAL_DICE_AUTO_RESULT=true no_approval_button=true")
    patch_player_initiated_dice(java_dir)
    patch_no_invisible_kingdom_dice(java_dir)
    patch_visible_construction_dice(java_dir)
    print(f"R36_KINGMAKER_CHECKLIST_PATCHED={target_checklist}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: patch_kingmaker_turn_checklist.py <project_root>", file=sys.stderr)
        sys.exit(1)
    patch_checklist(Path(sys.argv[1]))

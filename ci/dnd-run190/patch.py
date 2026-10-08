#!/usr/bin/env python3
"""Run190 non-destructive rule-curiosity HUD patch over canonical RUN189."""
from pathlib import Path
import sys, shutil
root=Path(sys.argv[1]).resolve()
pkg=root/'android/app/src/main/java/com/braseiro/dnd5e'
pkg.mkdir(parents=True,exist_ok=True)
mp=pkg/'MainActivity.java'
s=mp.read_text(encoding='utf-8')
if 'CompanionAgencyEngine.stageCurated' not in s or 'RUN188_GEMINI_EXPLICIT_OFFLINE' not in s:
    # Allow marker variation from RUN188, but require the actual labels.
    if not ('CompanionAgencyEngine.stageCurated' in s and 'TENTAR NOVAMENTE' in s and 'MESTRE OFFLINE' in s):
        raise SystemExit('RUN190_REQUIRES_CANONICAL_RUN189')
if 'private void showRuleCuriosityDialog()' in s:
    print('RUN190_ALREADY_APPLIED');sys.exit(0)
def put_once(old,new,tag):
    global s
    n=s.count(old)
    if n!=1:raise SystemExit(f'RUN190_MISSING_ANCHOR_{tag}_{n}')
    s=s.replace(old,new,1)
anchor='''        addGeminiMasterChoiceButtons(b);
        narratorProgress=new ProgressBar(this);'''
replace='''        addGeminiMasterChoiceButtons(b);
        addRuleCuriosityButton(b);
        narratorProgress=new ProgressBar(this);'''
put_once(anchor,replace,'NARRATOR_HUD')
anchor='''        hud.addView(faerun,new LinearLayout.LayoutParams(dp(84),dp(48)));'''
replace='''        // Educational button is always available and never advances the story.
        Button ruleTip=smallButton("📖",PANEL2);ruleTip.setTextSize(19);
        ruleTip.setContentDescription("Curiosidade de regra da cena; livro e página verificados");
        ruleTip.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showRuleCuriosityDialog();}});
        hud.addView(ruleTip,new LinearLayout.LayoutParams(dp(46),dp(48)));
        hud.addView(faerun,new LinearLayout.LayoutParams(dp(84),dp(48)));'''
put_once(anchor,replace,'STRIXHAVEN_HUD')
anchor='''        final LinearLayout b=body(narratorScroll);
        LinearLayout warning=card();'''
replace='''        final LinearLayout b=body(narratorScroll);
        addRuleCuriosityButton(b);
        LinearLayout warning=card();'''
put_once(anchor,replace,'SAFE_MODE_HUD')
anchor='''    private void showNarratorFull(){'''
methods='''    // RUN190: only persisted UI metadata; never commits scene/world/character changes.
    private RuleCuriosityCatalog.Entry currentSceneRuleCuriosity(){
        String scope=activeWorldProfile();if(scope==null)scope="default";
        JSONObject scene=db.getSceneState();
        String location=scene.optString("location", "");
        String narration=lastNarration==null?"":lastNarration.trim();
        String sceneKey=stableFingerprint(scope+"|"+location+"|"+narration);
        String key="rule_curiosity_scene_"+scope;
        String oldKey=prefs.getString(key,"");
        int index=prefs.getInt("rule_curiosity_index_"+scope,-1);
        if(!sceneKey.equals(oldKey)){
            index=(index+1)%RuleCuriosityCatalog.count();
            prefs.edit().putString(key,sceneKey).putInt("rule_curiosity_index_"+scope,index).apply();
        }
        return RuleCuriosityCatalog.at(Math.max(0,index));
    }
    private void addRuleCuriosityButton(LinearLayout parent){
        Button tip=smallButton("📖 CURIOSIDADE DE REGRA",PANEL2);
        tip.setContentDescription("Aprender uma regra nesta cena e consultar a página exata do livro");
        tip.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showRuleCuriosityDialog();}});
        parent.addView(tip,new LinearLayout.LayoutParams(-1,dp(43)));
    }
    private void showRuleCuriosityDialog(){
        final RuleCuriosityCatalog.Entry entry=currentSceneRuleCuriosity();
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18),dp(10),dp(18),dp(12));
        body.addView(text(entry.category.toUpperCase(new Locale("pt","BR"))+" • "+entry.title,16,GOLD,true));
        body.addView(text("\\n"+entry.explanation+"\\n\\nEXEMPLO NA MESA\\n"+entry.application,14,TEXT,false));
        body.addView(text("\\nFONTE CONFERIDA\\n"+RuleCuriosityCatalog.BOOK+"\\nPágina impressa: "+entry.printedPage+" • página "+entry.pdfPage+" do arquivo PDF.",12,GREEN,true));
        body.addView(text("Esta leitura não altera fichas, tempo, rolagens nem a aventura. Algumas opções do livro são facultativas ao Mestre.",11,MUTED,false));
        new AlertDialog.Builder(this).setTitle("📖 Curiosidade de regra")
          .setView(scrollableDialogView(body)).setNegativeButton("Fechar",null)
          .setPositiveButton("ABRIR LIVRO",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){
               try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(RuleCuriosityCatalog.DRIVE_URL)));}
               catch(Exception error){toast("Não foi possível abrir o Drive. Consulte o livro e a página indicados.");}
          }}).show();
    }

'''
put_once(anchor,methods+anchor,'METHODS')
mp.write_text(s,encoding='utf-8',newline='\n')
shutil.copy2(Path(__file__).parent/'java/RuleCuriosityCatalog.java',pkg/'RuleCuriosityCatalog.java')
print('RUN190_RULE_CURIOSITY_APPLIED entries=14; mechanics preserved')

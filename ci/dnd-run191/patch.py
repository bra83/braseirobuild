#!/usr/bin/env python3
"""RUN191 staged UI corrections over CANONICAL RUN190, non-destructive and repeat safe."""
from pathlib import Path
import sys
root=Path(sys.argv[1])
main=root/'android/app/src/main/java/com/braseiro/dnd5e/MainActivity.java'
s=main.read_text(encoding='utf-8')
if 'RUN191_GUIDED_COMBAT_EDITOR' in s:
    print('RUN191_ALREADY_APPLIED');sys.exit(0)
if 'private void showRuleCuriosityDialog()' not in s or 'CompanionAgencyEngine.stageCurated' not in s:
    raise SystemExit('RUN191_REQUIRES_RUN190')

def replace_once(old,new,label):
    global s
    if s.count(old)!=1:
        raise SystemExit(f'RUN191_ANCHOR_{label}_COUNT_{s.count(old)}')
    s=s.replace(old,new,1)

# Initiative preview; no advancement, save or world mutation until confirm.
replace_once('fields.addView(text("Informe o valor final de cada participante: resultado rolado externamente com os modificadores já aplicados. O aplicativo não rola nem estima iniciativa.",12,MUTED,false));', '''fields.addView(text("Iniciativa = d20 + modificadores. Use o lançamento local ou informe o total manualmente. Para NPCs sem atributos conhecidos, o lançamento usa bônus 0; confira antes de confirmar.",12,MUTED,false));''','INIT_TEXT')
old='''        if(fieldNames.isEmpty()){combatInitiativeInputOpen.set(false);if(callback!=null)callback.accepted(new JSONObject());return;}
        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title==null?"Iniciativa":title).setView(scrollableDialogView(fields)).setNegativeButton("CANCELAR",null).setPositiveButton("CONFIRMAR",null).create();'''
new='''        if(fieldNames.isEmpty()){combatInitiativeInputOpen.set(false);if(callback!=null)callback.accepted(new JSONObject());return;}
        Button localRoll=button("🎲 ROLAR INICIATIVAS (1d20 + DES)",PANEL2);
        localRoll.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            java.security.SecureRandom rng=new java.security.SecureRandom();
            for(int i=0;i<inputs.size();i++){
                String name=fieldNames.get(i);int bonus=0;
                JSONObject hero=db.findCharacterByName(name);
                if(hero.length()>0){
                    JSONObject sheet=parseJsonObject(hero.optString("sheet_json","{}"));
                    if(!sheet.has("dex")&&hero.has("dex"))try{sheet.put("dex",hero.optInt("dex",10));}catch(Exception ignored){}
                    bonus=rulesEngine.initiativeBonus(sheet);
                }else{
                    JSONObject npc=db.findNpcMemoryByName(name);
                    if(npc.has("initiative_bonus"))bonus=npc.optInt("initiative_bonus",0);
                    else if(npc.has("dex"))bonus=rulesEngine.modifier(npc.optInt("dex",10));
                }
                inputs.get(i).setText(String.valueOf(1+rng.nextInt(20)+bonus));
            }
            toast("Resultados preenchidos para revisão. Nenhum turno avançou.");
        }});
        fields.addView(localRoll);
        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title==null?"Iniciativa":title).setView(scrollableDialogView(fields)).setNegativeButton("CANCELAR",null).setPositiveButton("CONFIRMAR",null).create();'''
replace_once(old,new,'INIT_ROLL')

# Stop showing users a JSON editor for combat resources/timers. Preserve unknown existing metadata.
start=s.index('    private void advancedCombatantDialog(final JSONObject combatant){')
end=s.index('    private void showNarrator(){',start)
method='''    // RUN191_GUIDED_COMBAT_EDITOR: never ask for JSON, preserve unknown resource/timer keys.
    private void advancedCombatantDialog(final JSONObject combatant){
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(12),0,dp(12),dp(16));
        final EditText temp=labeledField(f,"PV temporários",String.valueOf(combatant.optInt("temp_hp",0)),false);
        temp.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText conditions=labeledField(f,"Condições (separadas por vírgula)",combatant.optString("conditions",""),true);
        final EditText concentration=labeledField(f,"Magia em concentração",combatant.optString("concentration",""),false);
        final EditText deathSuccess=labeledField(f,"Sucessos contra morte (0–3)",String.valueOf(combatant.optInt("death_success",0)),false);
        final EditText deathFail=labeledField(f,"Falhas contra morte (0–3)",String.valueOf(combatant.optInt("death_fail",0)),false);
        deathSuccess.setInputType(InputType.TYPE_CLASS_NUMBER);deathFail.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText resistances=labeledField(f,"Resistências",combatant.optString("resistances",""),false);
        final EditText vulnerabilities=labeledField(f,"Vulnerabilidades",combatant.optString("vulnerabilities",""),false);
        final EditText immunities=labeledField(f,"Imunidades",combatant.optString("immunities",""),false);
        final JSONObject originalResources=parseJsonObject(combatant.optString("resources_json","{}"));
        final String[] covers={"Sem cobertura","Meia cobertura","Três quartos","Cobertura total"};
        String savedCover=originalResources.optString("cover","");int coverPos=0;
        if(savedCover.toLowerCase(Locale.ROOT).contains("meia"))coverPos=1;
        else if(savedCover.toLowerCase(Locale.ROOT).contains("três")||savedCover.toLowerCase(Locale.ROOT).contains("tres"))coverPos=2;
        else if(savedCover.toLowerCase(Locale.ROOT).contains("total"))coverPos=3;
        final Spinner coverChoice=spinner(covers,covers[coverPos]);f.addView(cardWithLabel("COBERTURA",coverChoice));
        final CheckBox reaction=check("Reação utilizada neste turno",combatant.optInt("reaction_used",0)==1);f.addView(reaction);
        f.addView(text("DURAÇÕES DE CONDIÇÕES",14,GOLD,true));
        final JSONArray originalTimers;
        try{originalTimers=new JSONArray(combatant.optString("condition_timers_json","[]"));}
        catch(Exception malformed){toast("Durações existentes inválidas: edição bloqueada para proteger a ficha.");return;}
        final ArrayList<EditText> timerNames=new ArrayList<EditText>();
        final ArrayList<EditText> timerRounds=new ArrayList<EditText>();
        final ArrayList<CheckBox> timerRemovals=new ArrayList<CheckBox>();
        for(int i=0;i<originalTimers.length();i++){
            JSONObject t=originalTimers.optJSONObject(i);
            if(t==null){toast("Temporizador existente inválido. Nenhum dado foi alterado.");return;}
            final EditText tn=labeledField(f,"Condição "+(i+1),t.optString("name",""),false);
            final EditText tr=labeledField(f,"Rodadas restantes",String.valueOf(t.optInt("rounds",0)),false);
            tr.setInputType(InputType.TYPE_CLASS_NUMBER);
            final CheckBox remove=check("Remover esta duração",false);f.addView(remove);
            timerNames.add(tn);timerRounds.add(tr);timerRemovals.add(remove);
        }
        final EditText newTimerName=labeledField(f,"Nova condição com duração (opcional)","",false);
        final EditText newTimerRounds=labeledField(f,"Duração em rodadas", "1",false);
        newTimerRounds.setInputType(InputType.TYPE_CLASS_NUMBER);
        f.addView(text("Valores alterados serão aplicados somente ao tocar SALVAR. Campos de recursos avançados já existentes serão mantidos.",11,MUTED,false));
        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Combate — "+combatant.optString("name"))
            .setView(scrollableDialogView(f)).setNegativeButton("Cancelar",null).setPositiveButton("SALVAR",null).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener(){public void onShow(DialogInterface d){
            final Button save=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
                save.setEnabled(false);
                try{
                    int tempHp=Integer.parseInt(value(temp));int success=Integer.parseInt(value(deathSuccess));int fails=Integer.parseInt(value(deathFail));
                    if(tempHp<0||tempHp>9999||success<0||success>3||fails<0||fails>3)throw new IllegalArgumentException("PV temporários ou salvaguardas fora dos limites");
                    JSONArray timers=new JSONArray();
                    for(int i=0;i<timerNames.size();i++){
                        if(timerRemovals.get(i).isChecked())continue;
                        String timerName=value(timerNames.get(i)).trim();int rounds=Integer.parseInt(value(timerRounds.get(i)));
                        if(timerName.length()==0||rounds<0||rounds>999)throw new IllegalArgumentException("Nome ou duração inválidos na condição "+(i+1));
                        JSONObject t=new JSONObject(originalTimers.getJSONObject(i).toString());t.put("name",timerName).put("rounds",rounds);timers.put(t);
                    }
                    String fresh=value(newTimerName).trim();
                    if(fresh.length()>0){int rounds=Integer.parseInt(value(newTimerRounds));if(rounds<1||rounds>999)throw new IllegalArgumentException("A nova duração precisa ter entre 1 e 999 rodadas");timers.put(new JSONObject().put("name",fresh).put("rounds",rounds));}
                    JSONObject resources=new JSONObject(originalResources.toString());
                    String[] values={"","meia","tres quartos","total"};resources.put("cover",values[coverChoice.getSelectedItemPosition()]);
                    JSONObject before=db.getCombatant(combatant.optLong("id"));
                    db.updateCombatantAdvanced(combatant.optLong("id"),tempHp,value(concentration),reaction.isChecked(),success,fails,value(conditions),timers.toString(),value(resistances),value(vulnerabilities),value(immunities),resources.toString());
                    db.addAuditEvent("combate","Estado avançado atualizado pelo formulário guiado",combatant.optString("name"),before.toString(),db.getCombatant(combatant.optLong("id")).toString(),true);
                    dialog.dismiss();showCombat();
                }catch(Exception error){toast("Não foi possível salvar: "+safeMessage(error));save.setEnabled(true);}
            }});
        }});
        dialog.show();
    }

'''
s=s[:start]+method+s[end:]

# Monster initiative can now roll locally; require explicit review before commit.
replace_once('init=labeledField(f,"Iniciativa final — valor rolado externamente + modificadores","",false)',
             'init=labeledField(f,"Iniciativa final (d20 + bônus)","",false)', 'CREATURE_INIT_LABEL')
old='        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Adicionar combatente").setView(f)'
new='''        final EditText bonus=labeledField(f,"Bônus de iniciativa da criatura","0",false);
        bonus.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
        Button roll=button("🎲 ROLAR 1d20 + BÔNUS",PANEL2);
        roll.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            int modifier;
            try{modifier=Integer.parseInt(value(bonus));}catch(Exception invalid){bonus.setError("Informe o bônus da criatura.");return;}
            if(modifier< -20||modifier>30){bonus.setError("Confira o bônus da ficha da criatura.");return;}
            int die=1+new java.security.SecureRandom().nextInt(20);
            init.setText(String.valueOf(die+modifier));
            toast("D20: "+die+" + "+modifier+". Confira e confirme para adicionar.");
        }});
        f.addView(roll);
        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Adicionar combatente").setView(f)'''
replace_once(old,new,'CREATURE_ROLL_BUTTON')
replace_once('init.setError("Informe o valor rolado externamente.");return;', 'init.setError("Role no aplicativo ou digite o total.");return;', 'CREATURE_ROLL_REQUIRED')
replace_once('" em cena e iniciativa informada externamente."','" em cena e iniciativa confirmada pelo jogador."','CREATURE_NARRATION')
replace_once('". A iniciativa foi informada externamente."','". A iniciativa foi confirmada pelo jogador."','PARTY_NARRATION')
replace_once('"Informe externamente a iniciativa final de "', '"Role ou informe a iniciativa final de "','INIT_ERROR')

main.write_text(s,encoding='utf-8',newline='\n')
print('RUN191_APPLIED: initiative d20 preview, guided combat timers, no manual JSON controls')
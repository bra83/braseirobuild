#!/usr/bin/env python3
"""R27 stage 3: one diegetic progress control, max two anchored choices, no autoplay."""
from pathlib import Path
import sys,re
r=Path(sys.argv[1])/'android/app/src/main/java/com/braseiro/dnd/MainActivity.java'
s=r.read_text(encoding='utf-8')
anchor='''    private void addOptionButton(LinearLayout parent,final JSONObject option)'''
if s.count(anchor)!=1:raise AssertionError("Mausritter options entry changed")
add=r'''
    /** Mausritter-only dropdown: momentum is NOT an action or a turn and cannot alter state. */
    private void addMausritterSceneActionMenu(LinearLayout parent){
        LinearLayout actions=card();
        actions.addView(text("O QUE O RATO PODE FAZER?",14,GOLD,true));
        final ArrayList<JSONObject> anchored=new ArrayList<JSONObject>();
        final String sceneKey=stableFingerprint((lastNarration==null?"":lastNarration)+"|"
            +db.getSceneState().optString("hex_id","")+"|"+ensureMausritterCampaignRunId());
        if(currentOptions!=null)for(int i=0;i<currentOptions.length()&&anchored.size()<2;i++){
            JSONObject option=currentOptions.optJSONObject(i);
            if(option==null)continue;
            String declaration=option.optString("action",option.optString("description","")).trim();
            if(declaration.length()<12)continue;
            anchored.add(option);
        }
        Button menu=button("O QUE SE MOVE AO REDOR?  ▾",PANEL2);
        menu.setEnabled(!aiBusy);
        menu.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            if(aiBusy){toast("O Mestre ainda está respondendo.");return;}
            String liveKey=stableFingerprint((lastNarration==null?"":lastNarration)+"|"
                +db.getSceneState().optString("hex_id","")+"|"+ensureMausritterCampaignRunId());
            if(!sceneKey.equals(liveKey)){toast("A cena mudou. Atualize as sugestões.");showNarrator();return;}
            String[] labels=new String[anchored.size()+1];
            labels[0]="Um acontecimento rompe a quietude";
            for(int i=0;i<anchored.size();i++){
                JSONObject option=anchored.get(i);
                labels[i+1]=option.optString("label","Ação possível")
                    +(option.optString("description","").isEmpty()?"":" — "+option.optString("description",""));
            }
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("NA CENA ATUAL")
                .setItems(labels,new DialogInterface.OnClickListener(){public void onClick(DialogInterface dialog,int which){
                    if(!sceneKey.equals(stableFingerprint((lastNarration==null?"":lastNarration)+"|"
                        +db.getSceneState().optString("hex_id","")+"|"+ensureMausritterCampaignRunId()))){
                        toast("A cena mudou; opção descartada.");return;
                    }
                    if(which==0){requestMausritterMomentum();return;}
                    handleOption(anchored.get(which-1));
                }}).setNegativeButton("Cancelar",null).show();
        }});
        actions.addView(menu);
        actions.addView(text(anchored.isEmpty()
            ?"O Mestre pode introduzir um acontecimento; você também pode declarar qualquer ação no campo abaixo."
            :anchored.size()+" alternativa(s) ancorada(s) nesta cena, além da novidade do mundo.",
            11,MUTED,false));
        parent.addView(actions);
    }

    private void requestMausritterMomentum(){
        if(!MAUSRITTER_EDITION||aiBusy)return;
        if(pendingTest!=null&&pendingTest.optBoolean("required",false)){
            toast("Resolva ou cancele o teste pendente antes de prosseguir.");return;
        }
        if(db.getCombatState().optInt("running",0)==1){
            toast("Em combate, só o turno autorizado pode avançar a cena.");return;
        }
        if(db.countPartyActions("queued")>0){
            toast("Há declarações de companheiros aguardando resolução; nenhuma será consumida.");return;
        }
        if(lastNarration==null||lastNarration.trim().length()==0){
            toast("Inicie a aventura antes de pedir outra batida narrativa.");return;
        }
        sendAi("Narrador",
            "MAUSRITTER_MOMENTUM_ONLY. O mundo reage: narre UMA novidade causal concreta "+
            "que já esteja latente na cena (fala de NPC presente, sinal de uma facção ou "+
            "desdobramento de um fato estabelecido), e termine antes de qualquer escolha. "+
            "Nunca mova os ratos, conclua missão, role dados, use inventário, inicie combate, "+
            "avance relógio, viaje ou resolva ações da companhia. "+
            "Não repita a descrição anterior. Nenhuma consequência mecânica é autorizada.");
    }

'''
s=s.replace(anchor,add+anchor)
# Add menu OUTSIDE pending-test branch so the control is always visible
anchor='''addNarrationChapters(response,shownNarration);b.addView(response);addMasterInformationCard(b);'''
if s.count(anchor)!=1:raise AssertionError("Mausritter showNarratorFull anchor changed")
s=s.replace(anchor,anchor+'''if(MAUSRITTER_EDITION)addMausritterSceneActionMenu(b);''')
# Hide legacy 3–5 buttons only for Mausritter. All other systems unchanged.
pattern=re.compile(r'optionsContainer=card\(\);optionsContainer\.addView\(text\("SUGESTÕES OPCIONAIS".*?b\.addView\(optionsContainer\);',re.S)
old=pattern.findall(s)
if len(old)!=2:raise AssertionError(f"Expected 2 narrator layouts, found {len(old)}")
s=pattern.sub(lambda match:'if(!MAUSRITTER_EDITION){'+match.group(0)+'}',s)
# Protect the commit layer, not just the system prompt: momentum may only narrate.
anchor='''JSONObject env=parseAiEnvelope(raw);if(masterOrchestrator!=null)env=masterOrchestrator.validateEnvelope(env,action);boolean openingResponse='''
if s.count(anchor)!=1:raise AssertionError("Mausritter response entry changed")
s=s.replace(anchor,'''JSONObject env=parseAiEnvelope(raw);if(masterOrchestrator!=null)env=masterOrchestrator.validateEnvelope(env,action);
        boolean momentum=MAUSRITTER_EDITION&&action!=null&&action.contains("MAUSRITTER_MOMENTUM_ONLY");
        if(momentum){
            try{
                JSONObject safe=new JSONObject()
                  .put("narration",env.optString("narration",""))
                  .put("options",new JSONArray())
                  .put("combat",new JSONObject().put("action","none"))
                  .put("pending_test",JSONObject.NULL)
                  .put("visual_prompt",env.optString("visual_prompt",""));
                env=safe;
            }catch(Exception failed){env=new JSONObject();}
        }
        boolean openingResponse=''')

r.write_text(s,encoding='utf-8')
print('D02/D03: scene-fingerprinted dropdown, 0–2 contextual options, independent progress; envelope mechanical firewall')

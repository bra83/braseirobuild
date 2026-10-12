"""Persist primary A16/A17 smoke and fire consequences in real combat/character state."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Manor fire anchor '+str(s.count(a))+' '+a[:70])
 return s.replace(a,b,1)
def fire_repository(s):
 helper='''
    public JSONObject manorFireState(){return jsonObject(db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),"campaign:manor_fire"}).optString("effects_json","{}"));}
    public JSONObject resolveManorFireAction(final JSONObject params)throws Exception{
        requireEnabled();if(!KingmakerPreludePolicy.isMansionPhase(campaignStoryPhase()))throw new IllegalStateException("A ação exige presença na mansão durante o prólogo.");
        final JSONObject[] result={new JSONObject()};db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            String actionId=clean(params.optString("action_id"));if(actionId.length()<8)throw new IllegalArgumentException("A ação precisa de uma identidade estável.");
            JSONObject oldReceipt=db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),"manor_action:"+actionId});
            if(oldReceipt.length()>0){result[0]=jsonObject(oldReceipt.optString("effects_json","{}"));return;}
            long heroId=params.optLong("character_id");JSONObject hero=db.getCharacter(heroId);if(hero.length()==0||hero.optInt("active",0)!=1)throw new IllegalArgumentException("Personagem ativo inexistente.");
            String action=params.optString("action");int room=params.optInt("room",0);if(room<1||room>5)throw new IllegalArgumentException("Selecione um dos cinco quartos A17.");
            Pathfinder2eRulesEngine core=new Pathfinder2eRulesEngine();JSONObject before=manorFireState(),clock=db.getSceneState();double hour=clock.optDouble("clock_hour",SceneTemporalResolver.hourForLabel(clock.optString("time_of_day","Manhã")));
            if(Double.isNaN(hour)||Double.isInfinite(hour)||hour<0||hour>=24)throw new IllegalStateException("Horário do mundo inválido.");double absolute=Math.max(1,clock.optInt("day_number",1))*24d+hour;
            JSONObject combat=db.queryOne("SELECT * FROM combatants WHERE world_profile=? AND is_player=1 AND source_id=?",new String[]{profile(),String.valueOf(heroId)}),combatState=db.getCombatState();
            int natural=params.optInt("natural_roll"),modifier=0;String degree="",skill=params.optString("skill","Athletics");
            boolean costsAction="fight_fire".equals(action)||"break_window".equals(action);
            if(costsAction&&combatState.optInt("running")==1){
                if(combat.length()==0||combat.optInt("active_turn")!=1)throw new IllegalStateException("Espere o turno desse personagem.");
                JSONObject resources=jsonObject(combat.optString("resources_json","{}"));String turn=combatState.optString("encounter_id")+":"+combatState.optInt("round_no",1)+":"+heroId;
                int used=turn.equals(resources.optString("manor_action_turn"))?resources.optInt("manor_actions_used"):0,budget=core.evaluateConditions(combat.optString("conditions")).optInt("actions_max",3);
                if(used>=budget)throw new IllegalStateException("As ações disponíveis neste turno já foram usadas.");
                resources.put("manor_action_turn",turn).put("manor_actions_used",used+1);
                db.updateCombatantPf2AdvancedChecked(combat.optLong("id"),combat.optInt("temp_hp"),combat.optInt("reaction_used")==1,combat.optString("conditions"),combat.optString("condition_timers_json","[]"),combat.optString("resistances"),combat.optString("vulnerabilities"),combat.optString("immunities"),resources.toString());
            }
            if("fight_fire".equals(action)){
                if(!java.util.Arrays.asList("Acrobatics","Athletics","Survival","Acrobacia","Atletismo","Sobrevivência").contains(skill))throw new IllegalArgumentException("Combater incêndios usa Acrobacia, Atletismo ou Sobrevivência.");
                if(natural<1||natural>20)throw new IllegalArgumentException("d20 fora de 1–20.");
                modifier=core.checkBonus(hero,"",skill);if(params.optBoolean("water")){String skillKey=("Athletics".equals(skill)||"Atletismo".equals(skill))?"athletics":("Acrobatics".equals(skill)||"Acrobacia".equals(skill))?"acrobatics":"survival";JSONObject sheet=jsonObject(hero.optString("sheet_json","{}"));if(sheet.has(skillKey+"_bonus")&&!sheet.has(skillKey+"_item_bonus"))throw new IllegalStateException("A ficha precisa discriminar o bônus de item desta perícia para aplicar água sem somar bônus de item incompatíveis.");modifier+=Math.max(0,2-Math.max(0,sheet.optInt(skillKey+"_item_bonus")));}
                if(combat.length()>0)modifier+=core.evaluateConditions(combat.optString("conditions")).optInt("all_checks_status_penalty");
                degree=core.degree(natural,natural+modifier,15).name();
            }else if("smoke".equals(action)||"fire_damage".equals(action)){
                if(combat.length()==0)throw new IllegalStateException("Inclua o personagem no encontro para aplicar o perigo à ficha de combate.");
                if(natural<1||natural>20)throw new IllegalArgumentException("d20 fora de 1–20.");
                modifier=core.savingThrowBonus(hero,"smoke".equals(action)?"Fortitude":"Reflexos")+core.evaluateConditions(combat.optString("conditions")).optInt("all_checks_status_penalty");
                int dc="smoke".equals(action)?KingmakerCampaignGate.manorSmokeDc(before):16;degree=core.degree(natural,natural+modifier,dc).name();
            }else if(!"break_window".equals(action))throw new IllegalArgumentException("Ação do incêndio desconhecida.");
            JSONObject after=KingmakerCampaignGate.manorFireAction(before,room,action,degree,absolute,combatState.optInt("running")==1?combatState.optInt("round_no",1):0,params.optBoolean("holding_breath"),params.optJSONArray("damage_rolls"),params.optInt("suffocation_d4"));
            JSONObject outcome=after.optJSONObject("last_outcome");outcome.put("character_id",heroId).put("natural_roll",natural).put("modifier",modifier).put("degree",degree).put("action_id",actionId);
            if(combat.length()>0&&outcome.optInt("damage")>0){
                JSONObject damaged=db.applyCombatantDamagePf2(combat.optLong("id"),outcome.optInt("damage"),"CRITICAL_FAILURE".equals(degree),"smoke".equals(action));
                if(damaged.length()==0)throw new IllegalStateException("O dano do perigo não foi aplicado.");
                combat=db.getCombatant(combat.optLong("id"));
                if("smoke".equals(action)&&jsonObject(combat.optString("resources_json","{}")).optBoolean("unconscious")){int d4=params.optInt("suffocation_d4");if(d4<1||d4>4)throw new IllegalArgumentException("d4 de sufocamento inválido.");outcome.put("suffocation_begins_in_rounds",d4+1);JSONObject choking=jsonObject(combat.optString("resources_json","{}"));choking.put("manor_suffocation_starts_round",combatState.optInt("round_no",1)+d4+1);ContentValues ch=new ContentValues();ch.put("resources_json",choking.toString());db.getWritableDatabase().update("combatants",ch,"id=? AND world_profile=?",new String[]{String.valueOf(combat.optLong("id")),profile()});combat=db.getCombatant(combat.optLong("id"));}
                db.setCharacterHpChecked(heroId,combat.optInt("hp"));
            }
            if(combat.length()>0){
                JSONObject resources=jsonObject(combat.optString("resources_json","{}"));String conditions=combat.optString("conditions");int sick=outcome.optInt("sickened"),current=core.evaluateConditions(conditions).optInt("sickened");
                if(sick>current)conditions=conditions+(conditions.length()>0?", ":"")+"sickened "+sick;
                if(outcome.optBoolean("persistent_nonlethal")&&!resources.optString("persistent_damage").contains("1d6 nonlethal (smoke)"))resources.put("persistent_damage",resources.optString("persistent_damage")+(resources.optString("persistent_damage").length()>0?"; ":"")+"1d6 nonlethal (smoke)");
                db.updateCombatantPf2AdvancedChecked(combat.optLong("id"),combat.optInt("temp_hp"),combat.optInt("reaction_used")==1,conditions,combat.optString("condition_timers_json","[]"),combat.optString("resistances"),combat.optString("vulnerabilities"),combat.optString("immunities"),resources.toString());
            }
            if(after.optBoolean("all_extinguished")&&!before.optBoolean("all_extinguished")&&!after.optBoolean("xp_awarded")){
                JSONArray awards=new JSONArray();for(JSONObject pc:db.listActiveCharacters()){
                    JSONObject sheet=jsonObject(pc.optString("sheet_json","{}"));int xp=sheet.optInt("experience_points",0);if(xp<0||xp>Integer.MAX_VALUE-30)throw new IllegalStateException("XP da ficha inválido.");sheet.put("experience_points",xp+30).put("level_up_available",xp+30>=1000);db.updateCharacterSheetChecked(pc.optLong("id"),sheet.toString());awards.put(new JSONObject().put("character_id",pc.optLong("id")).put("xp",30));
                }after.put("xp_awarded",true).put("xp_awards",awards);outcome.put("story_xp",30);
            }
            if(costsAction&&combatState.optInt("running")==0){double next=hour+1d/1800d;clock.put("clock_hour",next%24d).put("day_number",Math.max(1,clock.optInt("day_number",1))+(int)Math.floor(next/24d)).put("time_of_day",SceneTemporalResolver.labelForHour(next%24d));db.saveSceneStateChecked(clock);}
            upsertFeature("campaign:manor_fire","campaign_environment","Incêndios da mansão",1,after.toString(),"Kingmaker pp.33–35","Fumaça A16 e cinco quartos em chamas A17.");
            upsertFeature("manor_action:"+actionId,"campaign_action","Ação no incêndio",1,outcome.toString(),"Kingmaker pp.33–35",params.optString("notes"));
            audit("Perigo da mansão resolvido",action+" • quarto "+room,before,after);result[0]=outcome;
        }});return result[0];
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
def fire_ui(s):
 anchor='p.append("MUNDO VIVO ESTRUTURADO:'
 s=once(s,anchor,'if(kingmakerService!=null&&kingmakerService.enabled())p.append("PERIGOS A16/A17: use combat.manor_actions para ações confirmadas do incêndio, inclusive combat.action=none. Cada entrada exige action_id estável, character_id real, room de 1 a 5 (quartos de cima para baixo), action fight_fire/break_window/smoke/fire_damage, natural_roll real de 1 a 20 quando há teste, skill Acrobatics/Athletics/Survival, water, holding_breath, damage_rolls com dois d6 e suffocation_d4 de 1 a 4. O motor determina graus, dano, pontos, janelas, fumaça e XP. Não converta estas consequências em atualizações genéricas de PV nem conceda XP do Reino. Estado factual: "+kingmakerRepository.manorFireState().toString()+"\\n");'+anchor)
 start=s.index('    private void showKingmakerCampaignJournal()');end=s.index('    private void showKingmakerQuestObjectives(',start);part=s[start:end]
 part=once(part,'.setNegativeButton("Voltar",null)', '.setPositiveButton("INCÊNDIO A17",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){showKingmakerManorFire();}}).setNegativeButton("Voltar",null)');s=s[:start]+part+s[end:]
 helper='''
    private void showKingmakerManorFire(){
        final List<JSONObject> heroes=db.listActiveCharacters();if(heroes.isEmpty()){toast("Não há personagens ativos.");return;}
        String[] labels=new String[heroes.size()];for(int n=0;n<labels.length;n++)labels[n]=heroes.get(n).optString("name");
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));
        JSONObject current=kingmakerRepository.manorFireState();JSONArray currentRooms=current.optJSONArray("rooms");StringBuilder status=new StringBuilder();for(int n=0;n<5;n++){JSONObject state=currentRooms==null?new JSONObject():currentRooms.optJSONObject(n);if(state==null)state=new JSONObject();status.append("Quarto ").append(n+1).append(": ").append(state.optInt("points")).append("/4").append(state.optBoolean("window_broken")?" • janela aberta":"").append("\\n");}form.addView(text(status.toString()+"Fumaça: CD "+KingmakerCampaignGate.manorSmokeDc(current)+(current.optBoolean("xp_awarded")?" • 30 XP já concedidos":""),13,TEXT,false));
        form.addView(text("Cinco quartos A17, numerados de cima para baixo no mapa. Cada incêndio precisa de 4 pontos. Três janelas abertas reduzem a fumaça; apagar todos rende 30 XP a cada personagem.",13,TEXT,false));
        final Spinner hero=spinner(labels,labels[0]),room=spinner(new String[]{"1","2","3","4","5"},"1"),action=spinner(new String[]{"Combater incêndio","Quebrar janela","Resistir à fumaça","Dano de fogo ao terminar turno"},"Combater incêndio"),skill=spinner(new String[]{"Atletismo","Acrobacia","Sobrevivência"},"Atletismo");form.addView(cardWithLabel("PERSONAGEM",hero));form.addView(cardWithLabel("QUARTO A17",room));form.addView(cardWithLabel("AÇÃO",action));form.addView(cardWithLabel("PERÍCIA PARA COMBATER INCÊNDIO",skill));
        final CheckBox water=check("Uso de água ao combater o incêndio",false),breath=check("Estou prendendo a respiração",false);form.addView(water);form.addView(breath);final EditText roll=labeledField(form,"d20",String.valueOf(new Random().nextInt(20)+1),false);roll.setInputType(InputType.TYPE_CLASS_NUMBER);
        final String actionId=java.util.UUID.randomUUID().toString();
        new AlertDialog.Builder(this).setTitle("Fumaça e incêndios da mansão").setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setPositiveButton("RESOLVER",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{
            String[] keys={"fight_fire","break_window","smoke","fire_damage"};JSONObject params=new JSONObject().put("action_id",actionId).put("character_id",heroes.get(hero.getSelectedItemPosition()).optLong("id")).put("room",room.getSelectedItemPosition()+1).put("action",keys[action.getSelectedItemPosition()]).put("skill",String.valueOf(skill.getSelectedItem())).put("natural_roll",num(roll,0)).put("water",water.isChecked()).put("holding_breath",breath.isChecked()).put("damage_rolls",new JSONArray().put(new Random().nextInt(6)+1).put(new Random().nextInt(6)+1)).put("suffocation_d4",new Random().nextInt(4)+1);
            JSONObject result=kingmakerRepository.resolveManorFireAction(params);toast(result.optString("description","Ação registrada."));showKingmakerManorFire();
        }catch(Exception e){toast(e.getMessage());}}}).show();
    }
'''
 s=once(s,'            int perception=pathfinderRules.initiativeBonus(ch,"Percepção");','            card.addView(text("XP "+sheet.optInt("experience_points",0)+"/1000"+(sheet.optBoolean("level_up_available")?" • avanço de nível disponível":""),13,GOLD,true));\n            int perception=pathfinderRules.initiativeBonus(ch,"Percepção");')
 i=s.rfind('\n}');s=s[:i]+'\n'+helper+s[i:]
 start=s.index('    private String applyCombatPlan(');end=s.index('    private String buildHeroAiContext(',start);part=s[start:end]
 anchor='String action=combat.optString("action","none").toLowerCase(Locale.US);if("none".equals(action)||action.length()==0)return "";StringBuilder summary=new StringBuilder();'
 part=once(part,anchor,'StringBuilder summary=new StringBuilder();JSONArray manorActions=combat.optJSONArray("manor_actions");if(manorActions!=null)for(int n=0;n<manorActions.length();n++)kingmakerRepository.resolveManorFireAction(manorActions.optJSONObject(n));String action=combat.optString("action","none").toLowerCase(Locale.US);if("none".equals(action)||action.length()==0)return manorActions!=null&&manorActions.length()>0?"Perigos da mansão resolvidos.":"";');s=s[:start]+part+s[end:]
 return s

def fire_appdb(s):
 anchor='if(c.optInt("is_player",0)==1){JSONObject ch=findCharacterByName(c.optString("name"));if(ch.length()>0)setCharacterHp(ch.optLong("id"),hp);}'
 replacement='''if(c.optInt("is_player",0)==1){
            JSONObject ch="character".equals(c.optString("source_kind"))&&c.optLong("source_id")>0?getCharacter(c.optLong("source_id")):findCharacterByName(c.optString("name"));
            if(ch.length()>0){try{setCharacterHpChecked(ch.optLong("id"),hp);JSONObject sheet=parseJsonObject(getCharacter(ch.optLong("id")).optString("sheet_json","{}"));sheet.put("dead",dead).put("dying",dying).put("wounded",wounded).put("doomed",doomed).put("unconscious",state.optBoolean("unconscious"));updateCharacterSheetChecked(ch.optLong("id"),sheet.toString());}catch(Exception failure){throw new IllegalStateException("O estado vital não foi sincronizado com a ficha.",failure);}}
        }'''
 s=once(s,anchor,replacement)
 s=once(s,'syncPlayerCombatantHp(ch.optString("name"),safe);','syncPlayerCombatantHp(id,ch.optString("name"),safe);')
 s=once(s,'private void syncPlayerCombatantHp(String characterName,int hp){','private void syncPlayerCombatantHp(long characterId,String characterName,int hp){')
 s=once(s,'"world_profile=? AND is_player=1 AND lower(name)=lower(?)",new String[]{getActiveMapProfile(),characterName}','"world_profile=? AND is_player=1 AND ((source_kind=\'character\' AND source_id=?) OR (COALESCE(source_id,0)<=0 AND lower(name)=lower(?) AND 1=(SELECT COUNT(*) FROM characters ch WHERE ch.world_profile=combatants.world_profile AND lower(ch.name)=lower(combatants.name))))",new String[]{getActiveMapProfile(),String.valueOf(characterId),characterName}')
 return s

"""Resolve the primary smoke delay and the permanent PF2 suffocation dependency."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Manor breathing anchor '+str(s.count(a))+' '+a[:70])
 return s.replace(a,b,1)
def breathing_repository(s):
 anchor='        final JSONObject[] result={new JSONObject()};db.runInTransactionChecked'
 start=s.index('    public JSONObject resolveManorFireAction(');end=s.index('\n    }',start)+6;part=s[start:end]
 part=once(part,anchor,'        if(java.util.Arrays.asList("fresh_air","breathing_end_turn","suffocation_save").contains(params.optString("action")))return resolveManorBreathingAction(params);\n'+anchor)
 part=once(part,'if("smoke".equals(action)&&jsonObject(combat.optString("resources_json","{}")).optBoolean("unconscious"))','if("smoke".equals(action)&&jsonObject(combat.optString("resources_json","{}")).optBoolean("unconscious")&&!jsonObject(combat.optString("resources_json","{}")).has("manor_suffocation_starts_round"))')
 anchor='            JSONObject after=KingmakerCampaignGate.manorFireAction('
 insert='''            boolean holding=params.optBoolean("holding_breath");
            if("smoke".equals(action)&&holding){
                if(combat.length()==0||combatState.optInt("running")!=1)throw new IllegalStateException("Prender a respiração exige acompanhar os turnos do personagem no encontro.");
                JSONObject air=jsonObject(combat.optString("resources_json","{}"));if(!air.has("manor_air_remaining"))air.put("manor_air_remaining",Math.max(0,5+core.attributeModifier(hero,"Constituição")));
                holding=air.optInt("manor_air_remaining")>0&&!air.optBoolean("unconscious")&&!air.optBoolean("dead");
                if(!holding)throw new IllegalStateException("Este personagem não pode continuar prendendo a respiração.");
                db.updateCombatantPf2AdvancedChecked(combat.optLong("id"),combat.optInt("temp_hp"),combat.optInt("reaction_used")==1,combat.optString("conditions"),combat.optString("condition_timers_json","[]"),combat.optString("resistances"),combat.optString("vulnerabilities"),combat.optString("immunities"),air.toString());combat=db.getCombatant(combat.optLong("id"));
            }
'''
 part=once(part,anchor,insert+anchor).replace('params.optBoolean("holding_breath"),params.optJSONArray("damage_rolls")','holding,params.optJSONArray("damage_rolls")');s=s[:start]+part+s[end:]
 helper='''
    public JSONObject resolveManorBreathingAction(final JSONObject params)throws Exception{
        requireEnabled();if(!KingmakerPreludePolicy.isMansionPhase(campaignStoryPhase()))throw new IllegalStateException("Este perigo exige presença na mansão.");
        final JSONObject[] output={new JSONObject()};db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            String actionId=clean(params.optString("action_id"));if(actionId.length()<8)throw new IllegalArgumentException("A ação precisa de identidade estável.");
            JSONObject priorReceipt=db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),"manor_action:"+actionId});if(priorReceipt.length()>0){output[0]=jsonObject(priorReceipt.optString("effects_json","{}"));return;}
            JSONObject hero=db.getCharacter(params.optLong("character_id")),turn=db.getCombatState();if(hero.length()==0||hero.optInt("active")!=1||turn.optInt("running")!=1)throw new IllegalStateException("A respiração exige personagem ativo e encontro em andamento.");
            JSONObject combat=db.queryOne("SELECT * FROM combatants WHERE world_profile=? AND is_player=1 AND source_kind='character' AND source_id=?",new String[]{profile(),String.valueOf(hero.optLong("id"))});if(combat.length()==0)throw new IllegalStateException("Personagem sem ficha de combate vinculada.");
            JSONObject before=jsonObject(combat.optString("resources_json","{}")),after=new JSONObject(before.toString()),outcome=new JSONObject();String action=params.optString("action");int round=turn.optInt("round_no",1);Pathfinder2eRulesEngine core=new Pathfinder2eRulesEngine();
            if("fresh_air".equals(action)){after=KingmakerCampaignGate.manorSuffocation(before,round,"",null,true,combat.optInt("hp"));outcome=after.optJSONObject("last_breath_outcome");}
            else{
                if(combat.optInt("active_turn")!=1)throw new IllegalStateException("Resolva este efeito no turno do personagem.");
                if(before.optBoolean("dead"))throw new IllegalStateException("Personagem morto não realiza testes de respiração.");
                if("breathing_end_turn".equals(action)){
                    if(!after.has("manor_air_remaining"))throw new IllegalStateException("Não há respiração presa registrada.");
                    if(after.optInt("manor_air_last_round",-1)>=round)throw new IllegalStateException("A respiração deste turno já foi contabilizada.");
                    int lost=params.optBoolean("attacked_or_cast")?2:1;lost+=Math.max(0,params.optInt("critical_air_losses",0));if(lost>20)throw new IllegalArgumentException("Perda de ar inválida.");int remaining=params.optBoolean("spoke")?0:Math.max(0,after.optInt("manor_air_remaining")-lost);
                    after.put("manor_air_remaining",remaining).put("manor_air_last_round",round);if(remaining==0)after.put("unconscious",true).put("manor_suffocation_starts_round",round);
                    outcome.put("description",remaining>0?"Restam "+remaining+" rodada(s) de ar.":"O ar acabou: inconsciente e sufocando.").put("air_remaining",remaining);
                }else if("suffocation_save".equals(action)){
                    int natural=params.optInt("natural_roll"),dc=20+5*before.optInt("manor_suffocation_checks",0);if(natural<1||natural>20)throw new IllegalArgumentException("d20 de sufocamento inválido.");int bonus=core.savingThrowBonus(hero,"Fortitude")+core.evaluateConditions(combat.optString("conditions")).optInt("all_checks_status_penalty");String degree=core.degree(natural,natural+bonus,dc).name();
                    after=KingmakerCampaignGate.manorSuffocation(before,round,degree,params.optJSONArray("suffocation_rolls"),false,combat.optInt("hp"));outcome=after.optJSONObject("last_breath_outcome");outcome.put("natural_roll",natural).put("modifier",bonus).put("degree",degree);
                    if(outcome.optInt("damage")>0){db.applyCombatantDamagePf2(combat.optLong("id"),outcome.optInt("damage"),false,false);JSONObject actual=jsonObject(db.getCombatant(combat.optLong("id")).optString("resources_json","{}"));for(String key:new String[]{"dead","dying","wounded","doomed"})after.put(key,actual.opt(key));}
                }else throw new IllegalArgumentException("Ação de respiração desconhecida.");
            }
            if(after.optBoolean("dead")){after.put("unconscious",false);ContentValues death=new ContentValues();death.put("hp",0);if(db.getWritableDatabase().update("combatants",death,"id=? AND world_profile=?",new String[]{String.valueOf(combat.optLong("id")),profile()})!=1)throw new IllegalStateException("A morte não foi persistida no combate.");}
            combat=db.getCombatant(combat.optLong("id"));String conditions=combat.optString("conditions").replaceAll("(?i)(?:^|[,;]\\\\s*)(?:unconscious|inconsciente)(?=[,;]|$)","").replaceAll("^[,;]\\\\s*","");if(after.optBoolean("unconscious"))conditions=conditions+(conditions.length()>0?", ":"")+"unconscious";
            db.updateCombatantPf2AdvancedChecked(combat.optLong("id"),combat.optInt("temp_hp"),combat.optInt("reaction_used")==1,conditions,combat.optString("condition_timers_json","[]"),combat.optString("resistances"),combat.optString("vulnerabilities"),combat.optString("immunities"),after.toString());
            if(after.optBoolean("dead"))db.setCharacterHpChecked(hero.optLong("id"),0);JSONObject sheet=jsonObject(db.getCharacter(hero.optLong("id")).optString("sheet_json","{}"));sheet.put("dead",after.optBoolean("dead")).put("dying",after.optInt("dying")).put("unconscious",after.optBoolean("unconscious"));db.updateCharacterSheetChecked(hero.optLong("id"),sheet.toString());
            outcome.put("character_id",hero.optLong("id")).put("action_id",actionId);upsertFeature("manor_action:"+actionId,"campaign_action","Respiração na mansão",1,outcome.toString(),"Kingmaker p.33; Player Core p.437",params.optString("notes"));audit("Respiração resolvida",action,before,after);output[0]=outcome;
        }});return output[0];
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]

def breathing_ui(s):
 s=once(s,'"Dano de fogo ao terminar turno"},','"Dano de fogo ao terminar turno","Respiração ao terminar turno","Teste de sufocamento","Levar para ar livre"},')
 s=once(s,'"smoke","fire_damage"};','"smoke","fire_damage","breathing_end_turn","suffocation_save","fresh_air"};')
 anchor='final CheckBox water=check('
 start=s.index('    private void showKingmakerManorFire()');end=s.index('\n    }',start)+6;part=s[start:end]
 part=once(part,anchor,'final CheckBox exertion=check("Ataquei ou conjurei neste turno",false),spoke=check("Falei neste turno (inclui magia verbal)",false);form.addView(exertion);form.addView(spoke);final EditText criticalLosses=labeledField(form,"Acertos críticos sofridos + falhas críticas em salvamentos com dano neste turno","0",false);criticalLosses.setInputType(InputType.TYPE_CLASS_NUMBER);'+anchor)
 part=once(part,'JSONObject result=kingmakerRepository.resolveManorFireAction(params);','''params.put("attacked_or_cast",exertion.isChecked()).put("spoke",spoke.isChecked()).put("critical_air_losses",num(criticalLosses,0));if("suffocation_save".equals(params.optString("action"))){JSONObject c=db.queryOne("SELECT * FROM combatants WHERE world_profile=? AND is_player=1 AND source_kind='character' AND source_id=?",new String[]{db.getActiveMapProfile(),String.valueOf(params.optLong("character_id"))});JSONObject r=parseLooseJson(c.optString("resources_json","{}"));int count=r.optInt("manor_suffocation_checks",0)+1;if(count<1||count>101)throw new IllegalStateException("Contagem de sufocamento inválida.");JSONArray dice=new JSONArray();for(int n=0;n<count;n++)dice.put(new Random().nextInt(10)+1);params.put("suffocation_rolls",dice);}JSONObject result=kingmakerRepository.resolveManorFireAction(params);''');s=s[:start]+part+s[end:]
 s=once(s,'action fight_fire/break_window/smoke/fire_damage,','action fight_fire/break_window/smoke/fire_damage/breathing_end_turn/suffocation_save/fresh_air,')
 s=once(s,'O motor determina graus, dano, pontos, janelas, fumaça e XP.','Respiração usa attacked_or_cast, spoke e critical_air_losses reais do turno; suffocation_save exige suffocation_rolls com N d10, N=1+número de testes anteriores, sempre ao fim do turno. fresh_air só quando o personagem for realmente levado para ar livre. O motor determina graus, dano, pontos, janelas, fumaça e XP.')
 return s

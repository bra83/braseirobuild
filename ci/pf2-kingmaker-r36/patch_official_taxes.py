#!/usr/bin/env python3
"""R36 A01: align production Kingmaker Collect Taxes with the official AP p.530.
Apply only against verified R35 Android Studio source in an isolated worktree.
No R35 tags/releases/production APK are modified.
"""
from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: python patch_official_taxes.py <verified-R35-project>")
root=Path(sys.argv[1]).resolve()
target=root/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java"
if not target.is_file():
    raise RuntimeError("KingmakerRepository.java not found in canonical project")
s=target.read_text(encoding="utf-8")
def once(old,new,label):
    global s
    count=s.count(old)
    if count!=1:raise RuntimeError("R36 A01 source drift at "+label+"; found "+str(count))
    s=s.replace(old,new,1)

# During the current Kingdom turn, an earlier tax attempt grants a genuine
# circumstance bonus to all subsequent Economy-based kingdom checks.
once('int modifier=rules.skillModifier(s.optInt("level",1),score,skill.optString("rank"),skill.optInt("item_bonus"),leadership,vacancy-ruin,s.optInt("unrest"));',
     'int taxBonus="economy".equals(ability)&&!"collect_taxes".equals(activity)?jsonObject(turn.optString("resource_roll_json","{}")).optInt("collect_taxes_economy_bonus",0):0;int modifier=rules.skillModifier(s.optInt("level",1),score,skill.optString("rank"),skill.optInt("item_bonus"),leadership,vacancy-ruin+taxBonus,s.optInt("unrest"));',
     "recordCheck economy bonus")
once('.put("ruin",-ruin).put("unrest",-unrest));',
     '.put("ruin",-ruin).put("unrest",-unrest).put("collect_taxes_economy_bonus",taxBonus));',
     "recordCheck modifier breakdown")

marker="    public JSONObject resolveCollectTaxes(int naturalRoll, String skillKey, String notes) throws Exception {"
a=s.find(marker)
if a<0:raise RuntimeError("missing canonical R35 Collect Taxes")
b=s.find("\n    public JSONObject ",a+len(marker))
if b<0:raise RuntimeError("missing method boundary after Collect Taxes")
previous=s[a:b]
if "Arrecadação fiscal excepcional" not in previous or '"resource_points", 2' not in previous:
    raise RuntimeError("R35 tax body differed; refusing blind patch")
method=r'''    public JSONObject resolveCollectTaxes(int naturalRoll, String skillKey, String notes) throws Exception {
        return resolveCollectTaxes(naturalRoll,skillKey,notes,"");
    }

    /** Official Kingmaker AP 2e p. 530: taxes modify subsequent Economy checks,
     * not Resource Points. The critical-failure Ruin may be selected by the
     * caller. Legacy solo callers default explicitly to Corruption.
     */
    public JSONObject resolveCollectTaxes(int naturalRoll, String skillKey, String notes, String criticalFailureRuin) throws Exception {
        requireEnabled();
        JSONObject turn=activeTurn();
        if(turn.length()==0||!"COMMERCE".equals(turn.optString("phase")))
            throw new IllegalStateException("Coletar Impostos so pode ocorrer na fase de Comercio de um turno ativo.");
        String ruin=key(criticalFailureRuin);
        if(ruin.length()>0&&!("corruption".equals(ruin)||"crime".equals(ruin)||"decay".equals(ruin)||"strife".equals(ruin)))
            throw new IllegalArgumentException("Ruina selecionada invalida.");
        if(db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_turn_activities WHERE world_profile=? AND turn_id=? AND activity_key='collect_taxes'",
                new String[]{profile(),String.valueOf(turn.optLong("id"))}).optInt("total",0)>0)
            throw new IllegalStateException("Coletar Impostos so pode ser tentado uma vez por turno.");
        int previousTurn=turn.optInt("turn_number")-1;
        boolean taxedLastTurn=previousTurn>0&&db.queryOne(
                "SELECT COUNT(*) AS total FROM kingmaker_turn_activities WHERE world_profile=? AND activity_key='collect_taxes' AND turn_id IN (SELECT id FROM kingmaker_turns WHERE world_profile=? AND turn_number=?)",
                new String[]{profile(),profile(),String.valueOf(previousTurn)}).optInt("total",0)>0;
        String skill=clean(skillKey).length()>0?key(skillKey):"trade";
        JSONObject check=recordCheck("collect_taxes","Coletar Impostos","Tesoureiro",skill,naturalRoll,0,"Kingmaker Adventure Path 2e p.530",notes,false);
        String degree=check.optString("degree");
        int economyBonus="CRITICAL_SUCCESS".equals(degree)?2:("SUCCESS".equals(degree)||"FAILURE".equals(degree)?1:0);
        JSONObject roll=jsonObject(activeTurn().optString("resource_roll_json","{}"));
        roll.put("collect_taxes_economy_bonus",economyBonus);
        ContentValues turnChange=new ContentValues();
        turnChange.put("resource_roll_json",roll.toString());
        db.getWritableDatabase().update("kingmaker_turns",turnChange,"id=? AND world_profile=?",
                new String[]{String.valueOf(turn.optLong("id")),profile()});
        JSONObject out=new JSONObject().put("check",check).put("economy_circumstance_bonus",economyBonus)
                .put("taxed_previous_turn",taxedLastTurn).put("rp_gain",0);
        if("CRITICAL_SUCCESS".equals(degree)){
            out.put("outcome_pt","Sucesso critico: +2 de bonus circunstancial nos testes de Economia deste turno.");
        }else if("SUCCESS".equals(degree)){
            if(taxedLastTurn)adjustState(new JSONObject().put("unrest",1),"Coleta consecutiva de impostos");
            out.put("outcome_pt","Sucesso: +1 de bonus circunstancial em Economia"+(taxedLastTurn?"; +1 Agitacao por impostos consecutivos.":"."));
        }else if("FAILURE".equals(degree)){
            int unrest=taxedLastTurn?2:1;
            adjustState(new JSONObject().put("unrest",unrest),"Descontentamento fiscal");
            out.put("outcome_pt","Falha: como sucesso (+1 em Economia), com +"+unrest+" Agitacao.");
        }else{
            String selected=ruin.length()>0?ruin:"corruption";
            adjustState(new JSONObject().put("unrest",2).put(selected,1),"Revolta contra cobranca de impostos");
            out.put("selected_ruin",selected).put("selected_ruin_was_default",ruin.length()==0);
            out.put("outcome_pt","Falha critica: +2 Agitacao e +1 Ruina ("+rules.ruinLabel(selected)+").");
        }
        return out;
    }
'''
s=s[:a]+method+s[b:]
once('return resolveCollectTaxes(nat, skill, notes);','return resolveCollectTaxes(nat, skill, notes, p.optString("ruin_choice",""));',"activity dispatcher explicit ruin choice")
target.write_text(s,encoding="utf-8")
print("R36_A01_TAXES_SOURCE_PATCHED=1")
print("R36_A01_TAXES_REPOSITORY="+str(target))

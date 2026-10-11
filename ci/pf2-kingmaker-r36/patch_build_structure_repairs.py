#!/usr/bin/env python3
"""Official Build Structure repair action: half RP AND commodities prepaid,
+2 item bonus for existing structure (same-type nonstacking),
critical success refunds half of the amount paid, and critical failure rubble.
No schema changes. Requires other R36 patches to be applied.
"""
from pathlib import Path
import sys
REL=Path("android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java")
def once(s,old,new,label):
    n=s.count(old)
    if n!=1:raise RuntimeError(f"{label}: source drift ({n})")
    return s.replace(old,new,1)
def transform(s):
    original='int modifier=rules.skillModifier(s.optInt("level",1),score,skill.optString("rank"),skill.optInt("item_bonus"),leadership,vacancy-ruin+circumstanceBonus-claimPenalty,s.optInt("unrest"));'
    adjusted='int civicItemBonus="repair_structure".equals(activity)?Math.max(skill.optInt("item_bonus"),2):skill.optInt("item_bonus");\n        int modifier=rules.skillModifier(s.optInt("level",1),score,skill.optString("rank"),civicItemBonus,leadership,vacancy-ruin+circumstanceBonus-claimPenalty,s.optInt("unrest"));'
    s=once(s,original,adjusted,"repair +2 item nonstacking")
    s=once(s,'.put("item",skill.optInt("item_bonus"))',
           '.put("item",civicItemBonus)',"repair item bonus breakdown")
    signature='    public JSONObject resolveRepairStructure(String settlementKey, String structureKey, String notes) throws Exception {'
    end='\n    public JSONObject resolveUpgradeSettlement('
    pos=s.find(signature)
    if pos<0 or s.count(signature)!=1:raise RuntimeError("Repair Structure resolver signature drift")
    stop=s.find(end,pos+len(signature))
    if stop<0:raise RuntimeError("Repair Structure resolver boundary missing")
    body=r'''    /** Legacy entry point makes a real random d20 roll, not a fabricated result. */
    public JSONObject resolveRepairStructure(String settlementKey,String structureKey,String notes)throws Exception{
        return resolveRepairStructure(settlementKey,structureKey,new java.util.Random().nextInt(20)+1,"engineering",notes);
    }

    /** Official Kingmaker 2e p. 518: repair first pays HALF the listed cost
     * in BOTH RP and commodities; the damaged structure grants +2 ITEM bonus.
     * Uses persistent kingmaker_structures and turn activity log.
     */
    public JSONObject resolveRepairStructure(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes)throws Exception{
        requireEnabled();
        if(naturalRoll<1||naturalRoll>20)throw new IllegalArgumentException("d20 fora do intervalo 1–20.");
        final String townKey=clean(settlementKey),structure=key(structureKey);
        final JSONObject town=settlement(townKey),def=rules.structure(structureKey);
        if(town.length()==0||def==null)throw new IllegalArgumentException("Assentamento ou estrutura inválidos.");
        JSONObject turn=activeTurn();
        if(turn.length()==0||!"ACTIVITY".equals(turn.optString("phase")))
            throw new IllegalStateException("Reparar Estrutura exige a fase de Atividades do Reino.");
        final JSONObject existing=db.queryOne(
            "SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",
            new String[]{profile(),townKey,structure});
        if(existing.length()==0)throw new IllegalStateException("Estrutura a reparar não existe.");
        if(existing.optInt("count",1)!=1)
            throw new IllegalStateException("Selecione uma estrutura individual para reparo.");
        final JSONObject oldEffects=jsonObject(existing.optString("effects_json","{}"));
        if("rubble".equals(oldEffects.optString("construction_status")))
            throw new IllegalStateException("Escombros exigem Demolish antes da construção.");
        if(db.queryOne("SELECT id FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",
            new String[]{profile(),townKey,"rubble_"+structure}).length()>0)
            throw new IllegalStateException("Já há escombros dessa estrutura neste assentamento.");
        final String skill=clean(skillKey).length()>0?key(skillKey):"engineering";
        if(skill(skill).length()==0)throw new IllegalArgumentException("Perícia inválida.");
        final String expected=key(def.optString("construction_skill",def.optString("skill_key","")));
        if(expected.length()>0&&!expected.equals(skill))
            throw new IllegalArgumentException("O reparo exige a perícia "+expected+".");
        final int rp=def.optInt("rp",0)/2,lumber=def.optInt("lumber",0)/2,stone=def.optInt("stone",0)/2,
            ore=def.optInt("ore",0)/2,luxuries=def.optInt("luxuries",0)/2;
        final JSONObject state=state();
        if(state.optInt("resource_points")<rp||state.optInt("lumber")<lumber||
            state.optInt("stone")<stone||state.optInt("ore")<ore||state.optInt("luxuries")<luxuries)
            throw new IllegalStateException("RP ou commodities insuficientes para o reparo.");
        final JSONObject[] output={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){
            public void run()throws Exception{
                adjustState(new JSONObject().put("resource_points",-rp).put("lumber",-lumber)
                    .put("stone",-stone).put("ore",-ore).put("luxuries",-luxuries),
                    "Reparar Estrutura: metade de RP e commodities paga antes do teste");
                recordRpSpent(rp);
                JSONObject check=recordCheck("repair_structure","Reparar Estrutura: "+structure,
                    "Vice-rei",skill,naturalRoll,0,"Kingmaker Adventure Path 2e p. 518",notes,false);
                String degree=check.optString("degree");
                boolean criticalFailure="CRITICAL_FAILURE".equals(degree);
                boolean success="SUCCESS".equals(degree)||"CRITICAL_SUCCESS".equals(degree);
                JSONObject result=new JSONObject().put("check",check).put("rp_spent",rp)
                    .put("construction_status",criticalFailure?"rubble":success?"repaired":"damaged");
                if(success){
                    ContentValues v=new ContentValues();
                    oldEffects.put("construction_status","complete").put("damaged",false);
                    v.put("effects_json",oldEffects.toString());
                    v.put("notes",(clean(existing.optString("notes"))+" [Reparado] "+clean(notes)).trim());
                    v.put("updated_at",System.currentTimeMillis());
                    db.getWritableDatabase().update("kingmaker_structures",v,"id=? AND world_profile=?",
                        new String[]{String.valueOf(existing.optLong("id")),profile()});
                    if("CRITICAL_SUCCESS".equals(degree)){
                        adjustState(new JSONObject().put("lumber",lumber/2).put("stone",stone/2)
                            .put("ore",ore/2).put("luxuries",luxuries/2),
                            "Reparar Estrutura: metade das commodities pagas devolvida no crítico");
                        result.put("commodities_refunded",new JSONObject().put("lumber",lumber/2)
                            .put("stone",stone/2).put("ore",ore/2).put("luxuries",luxuries/2));
                    }
                    result.put("outcome_pt","Reparo concluído. Custo de reparo pago antes do teste.");
                }else if(criticalFailure){
                    ContentValues v=new ContentValues();
                    v.put("structure_key","rubble_"+structure);
                    v.put("name","Escombros: "+existing.optString("name"));
                    v.put("effects_json",new JSONObject().put("construction_status","rubble")
                        .put("original_structure_key",structure).toString());
                    v.put("updated_at",System.currentTimeMillis());
                    db.getWritableDatabase().update("kingmaker_structures",v,"id=? AND world_profile=?",
                        new String[]{String.valueOf(existing.optLong("id")),profile()});
                    applyStructureSkillBonuses();
                    refreshSettlementOccupancy(townKey);
                    result.put("must_demolish",true).put("outcome_pt",
                        "Falha crítica: a estrutura foi reduzida a Escombros e exige Demolish.");
                }else{
                    oldEffects.put("construction_status","damaged").put("damaged",true);
                    ContentValues v=new ContentValues();
                    v.put("effects_json",oldEffects.toString());
                    v.put("updated_at",System.currentTimeMillis());
                    db.getWritableDatabase().update("kingmaker_structures",v,"id=? AND world_profile=?",
                        new String[]{String.valueOf(existing.optLong("id")),profile()});
                    result.put("outcome_pt","Falha: o dano permanece e os custos de reparo foram gastos.");
                }
                output[0]=result;
            }
        });
        return output[0];
    }
'''
    s=s[:pos]+body.rstrip()+"\n"+s[stop:]
    s=once(s,'return resolveRepairStructure(p.optString("settlement_key"), p.optString("structure_key"), notes);',
           'return resolveRepairStructure(p.optString("settlement_key"), p.optString("structure_key"), nat, skill, notes);',
           "repair action dispatcher accepts d20 and skill")
    return s
def apply(root):
    root=Path(root).resolve()
    v=root/"version.properties";p=root/REL
    if not v.is_file() or "VERSION_NAME=9.0.37" not in v.read_text(encoding="utf-8"):
        raise RuntimeError("R36 verified version required")
    source=p.read_text(encoding="utf-8")
    result=transform(source)
    p.write_text(result,encoding="utf-8")
    return p
if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_build_structure_repairs.py <R36 project>")
    print("KINGMAKER_R36_STRUCTURE_REPAIR_APPLIED",apply(sys.argv[1]))

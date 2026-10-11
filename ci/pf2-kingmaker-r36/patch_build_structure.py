#!/usr/bin/env python3
"""Official Kingmaker Build Structure construction: prepaid RP/commodities,
unfinished works retained between Kingdom turns, Rubble on critical failure,
and half-commodity refund on critical success. No database schema migration.
Applies only AFTER the canonical R35->R36 patches; fail closed on source drift.
Does not change repairStructure; separate certification gate required for repairs.
"""
from pathlib import Path
import sys

REL=Path("android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java")

def one(s,old,new,label):
    hits=s.count(old)
    if hits!=1:raise RuntimeError(f"{label}: source drift ({hits}), refusing mutation")
    return s.replace(old,new,1)

def transform(s):
    # Preserve the original public entry point used by preexisting edit controls,
    # but permit the rules resolver to pass previously paid costs.
    s=one(s,
        'public JSONObject buildStructure(final String settlementKey, final String structureKey, final String notes) throws Exception {',
        'public JSONObject buildStructure(final String settlementKey,final String structureKey,final String notes)throws Exception{\n'
        '        return buildStructure(settlementKey,structureKey,notes,false);\n'
        '    }\n'
        '    private JSONObject buildStructure(final String settlementKey, final String structureKey, final String notes, final boolean prepaid) throws Exception {',
        "safe builder overload")
    for k in ("resource_points","lumber","stone","ore","luxuries"):
        old=f'if (s.optInt("{k}", 0) < '+("rpCost" if k=="resource_points" else {"lumber":"lumberCost","stone":"stoneCost","ore":"oreCost","luxuries":"luxuriesCost"}[k])+') {'
        s=one(s,old,old.replace("if (","if (!prepaid && (").replace(") {", ")) {"),"prepaid guard "+k)
        cost=("rpCost" if k=="resource_points" else {"lumber":"lumberCost","stone":"stoneCost","ore":"oreCost","luxuries":"luxuriesCost"}[k])
        s=one(s,f'sv.put("{k}", s.optInt("{k}") - {cost});',
                f'if(!prepaid)sv.put("{k}", s.optInt("{k}") - {cost});',"no duplicate debit "+k)
    s=one(s,'recordRpSpent(rpCost);','if(!prepaid)recordRpSpent(rpCost);',"no duplicate RP ledger")

    signature='    public JSONObject resolveBuildStructure(String settlementKey, String structureKey, int naturalRoll, String skillKey, String notes) throws Exception {'
    end='\n    public JSONObject resolveDemolish('
    start=s.find(signature)
    if start<0 or s.count(signature)!=1:raise RuntimeError("Build Structure resolver missing")
    finish=s.find(end,start+len(signature))
    if finish<0:raise RuntimeError("Build Structure resolver boundary missing")
    body=r'''    /** Kingmaker AP 2e Build Structure: all costs paid BEFORE skill check.
     * Incomplete sites and rubble occupy lots in the ordinary structure table.
     * effects_json construction_status is persisted/exported in the existing backups.
     */
    public JSONObject resolveBuildStructure(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes)throws Exception{
        requireEnabled();
        if(naturalRoll<1||naturalRoll>20)throw new IllegalArgumentException("d20 fora do intervalo 1–20.");
        final String townKey=clean(settlementKey),chosenKey=key(structureKey);
        final JSONObject town=settlement(townKey),def=rules.structure(structureKey);
        if(town.length()==0||def==null)throw new IllegalArgumentException("Assentamento ou estrutura inválidos.");
        JSONObject turn=activeTurn();
        if(turn.length()==0||!"ACTIVITY".equals(turn.optString("phase")))
            throw new IllegalStateException("Construir Estrutura exige a fase de Atividades do Reino.");
        final JSONObject s=state();
        if(rules.isAnarchy(s.optInt("unrest"))||s.optInt("anarchy")==1)
            throw new IllegalStateException("Não se constrói em Anarquia.");
        if(s.optInt("level",1)<def.optInt("level",1)||
           settlementRank(town.optString("type"))<settlementRank(def.optString("min_settlement_type","village")))
            throw new IllegalStateException("Nível ou porte do assentamento insuficiente.");
        String prereq=def.optString("prerequisites","");
        if(prereq.length()>0&&db.queryOne("SELECT id FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",
            new String[]{profile(),townKey,prereq}).length()==0)
            throw new IllegalStateException("Pré-requisito da estrutura ausente.");
        final String skill=clean(skillKey).length()>0?key(skillKey):"engineering";
        if(skill(skill).length()==0)throw new IllegalArgumentException("Perícia de Reino inválida.");
        final String expectedSkill=key(def.optString("construction_skill",def.optString("skill_key","")));
        if(expectedSkill.length()>0&&!expectedSkill.equals(skill))
            throw new IllegalArgumentException("A construção exige a perícia "+expectedSkill+".");
        final String pendingKey="under_construction_"+chosenKey,rubbleKey="rubble_"+chosenKey;
        if(db.queryOne("SELECT id FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",
            new String[]{profile(),townKey,rubbleKey}).length()>0)
            throw new IllegalStateException("Escombros precisam ser removidos com Demolish antes de reconstruir.");
        final JSONObject pending=db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",
            new String[]{profile(),townKey,pendingKey});
        final boolean resume=pending.length()>0;
        final int lots=def.optInt("lots",1);
        int used=structureLotTotal(townKey)-(resume?pending.optInt("lots",lots):0);
        if(used+lots>Math.max(1,town.optInt("developed_blocks",1))*4)
            throw new IllegalStateException("Não existem lotes livres contíguos suficientes.");
        final int rp=def.optInt("rp",0),lumber=def.optInt("lumber",0),stone=def.optInt("stone",0),
            ore=def.optInt("ore",0),luxuries=def.optInt("luxuries",0);
        if(!resume&&(s.optInt("resource_points")<rp||s.optInt("lumber")<lumber||
            s.optInt("stone")<stone||s.optInt("ore")<ore||s.optInt("luxuries")<luxuries))
            throw new IllegalStateException("RP ou commodities insuficientes para a obra.");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){
            public void run()throws Exception{
                if(!resume){
                    JSONObject cost=new JSONObject().put("resource_points",-rp).put("lumber",-lumber)
                        .put("stone",-stone).put("ore",-ore).put("luxuries",-luxuries);
                    adjustState(cost,"Build Structure: custo antecipado de RP e commodities");
                    recordRpSpent(rp);
                }
                JSONObject check=recordCheck("build_structure","Construir Estrutura: "+chosenKey,"Vice-rei",skill,
                    naturalRoll,0,"Kingmaker Adventure Path 2e p. 518",notes,false);
                String degree=check.optString("degree");
                boolean success="CRITICAL_SUCCESS".equals(degree)||"SUCCESS".equals(degree);
                JSONObject output=new JSONObject().put("check",check).put("rp_spent",resume?0:rp)
                    .put("cost_paid_before_check",!resume).put("resumed_unfinished",resume);
                if(success){
                    if(resume)db.getWritableDatabase().delete("kingmaker_structures",
                        "world_profile=? AND settlement_key=? AND structure_key=?",
                        new String[]{profile(),townKey,pendingKey});
                    JSONObject completed=buildStructure(townKey,structureKey,notes,true);
                    output.put("structure",completed).put("construction_status","completed");
                    if("CRITICAL_SUCCESS".equals(degree)){
                        // Refund half of COMMODITIES, never RP.
                        adjustState(new JSONObject().put("lumber",lumber/2).put("stone",stone/2)
                            .put("ore",ore/2).put("luxuries",luxuries/2),
                            "Build Structure: sucesso crítico devolve metade das commodities");
                        output.put("commodities_refunded",new JSONObject().put("lumber",lumber/2)
                            .put("stone",stone/2).put("ore",ore/2).put("luxuries",luxuries/2));
                    }
                    output.put("outcome_pt","Estrutura concluída, custo previamente pago.");
                }else{
                    if(resume)db.getWritableDatabase().delete("kingmaker_structures",
                        "world_profile=? AND settlement_key=? AND structure_key=?",
                        new String[]{profile(),townKey,pendingKey});
                    final boolean rubble="CRITICAL_FAILURE".equals(degree);
                    String siteKey=rubble?rubbleKey:pendingKey;
                    ContentValues v=new ContentValues();
                    long now=System.currentTimeMillis();
                    v.put("world_profile",profile());
                    v.put("settlement_key",townKey);v.put("structure_key",siteKey);
                    v.put("name",(rubble?"Escombros: ":"Obra inacabada: ")+def.optString("name",chosenKey));
                    v.put("count",1);v.put("lots",lots);v.put("construction_rp",rp);
                    v.put("commodity_costs_json",new JSONObject().put("lumber",lumber).put("stone",stone)
                        .put("ore",ore).put("luxuries",luxuries).toString());
                    v.put("effects_json",new JSONObject().put("construction_status",rubble?"rubble":"unfinished")
                        .put("original_structure_key",chosenKey).put("cost_prepaid",true).toString());
                    v.put("source_ref","Kingmaker Adventure Path 2e p. 518");
                    v.put("notes",clean(notes));v.put("created_at",resume?pending.optLong("created_at",now):now);
                    v.put("updated_at",now);
                    db.getWritableDatabase().insertWithOnConflict("kingmaker_structures",null,v,SQLiteDatabase.CONFLICT_REPLACE);
                    refreshSettlementOccupancy(townKey);
                    output.put("construction_status",rubble?"rubble":"unfinished")
                        .put("costs_stay_spent",true).put("must_demolish",rubble)
                        .put("outcome_pt",rubble?"Falha crítica: escombros; use Demolish.":
                            "Falha: obra inacabada; retome no próximo turno sem pagar novamente.");
                }
                result[0]=output;
            }
        });
        return result[0];
    }
'''
    return s[:start]+body.rstrip()+"\n"+s[finish:]

def apply(root):
    root=Path(root).resolve()
    version=root/"version.properties"
    if not version.is_file() or "VERSION_NAME=9.0.37" not in version.read_text(encoding="utf-8"):
        raise RuntimeError("Requires verified R36 9.0.37 source")
    path=root/REL
    src=path.read_text(encoding="utf-8")
    after=transform(src)  # all anchors checked before write
    path.write_text(after,encoding="utf-8")
    return path

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_build_structure.py <R36 project>")
    print("KINGMAKER_R36_BUILD_STRUCTURE_PATCH_APPLIED",apply(sys.argv[1]))

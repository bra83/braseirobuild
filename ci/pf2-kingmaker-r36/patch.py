#!/usr/bin/env python3
"""R36 official Kingmaker rule corrections on the canonical, hash-verified R35 ZIP.
Applies only pre-identified stable anchors. Does not touch audio, map, images, or Android shell.
Books: Kingmaker Adventure Path 2e, pp. 522, 530, 531.
"""
import sys
from pathlib import Path

if len(sys.argv)!=2: raise SystemExit("usage: patch.py <verified_R35_extracted_project>")
root=Path(sys.argv[1]).resolve()
src=root/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java"
v=root/"version.properties"
if not src.is_file() or not v.is_file(): raise RuntimeError("Not a PF2e Android project")
oldversion=v.read_text(encoding="utf-8")
if "VERSION_NAME=9.0.36" not in oldversion: raise RuntimeError("Only R35 version 9.0.36 is an eligible parent")
s=src.read_text(encoding="utf-8")
def one(old,new,why):
    global s
    found=s.count(old)
    if found!=1: raise RuntimeError(f"{why}: expected unique anchor, found {found}")
    s=s.replace(old,new,1)
def replace_method(signature,body):
    global s
    begin=s.find(signature)
    if begin<0 or s.count(signature)!=1: raise RuntimeError(f"Method missing/not unique: {signature}")
    nextidx=s.find("\n    public ",begin+len(signature))
    if nextidx<0: raise RuntimeError(f"Next method missing for {signature}")
    s=s[:begin]+body.rstrip()+"\n"+s[nextidx:]

# Official Kingmaker Collect Taxes grants a CIRCUMSTANCE bonus for the remainder
# of this one kingdom turn. The turn activity log already persists the roll degree;
# derive the bonus from the real activity, without schema/backup migrations.
anchor='''int modifier=rules.skillModifier(s.optInt("level",1),score,skill.optString("rank"),skill.optInt("item_bonus"),leadership,vacancy-ruin,s.optInt("unrest"));'''
one(anchor,'''int taxes=("economy".equals(ability)&&!"collect_taxes".equals(activity))?taxesCircumstanceBonus(turn.optLong("id")):0;
        int claimPenalty="stability".equals(ability)?claimHexStabilityPenalty(s.optInt("turn_number")):0;
        int modifier=rules.skillModifier(s.optInt("level",1),score,skill.optString("rank"),skill.optInt("item_bonus"),leadership,vacancy-ruin+taxes-claimPenalty,s.optInt("unrest"));''',"live check modifier")
one('''.put("ruin",-ruin).put("unrest",-unrest)''','''.put("ruin",-ruin).put("unrest",-unrest).put("taxes_circumstance",taxes).put("claim_hex_stability_penalty",-claimPenalty)''',"mechanical breakdown")
target='''    public JSONObject resolveCollectTaxes(int naturalRoll, String skillKey, String notes) throws Exception {'''
replacement=r'''    /** Uses persisted roll degrees; the tax bonus expires automatically on turn change. */
    private int taxesCircumstanceBonus(long turnId){
        JSONObject last=db.queryOne("SELECT degree FROM kingmaker_turn_activities WHERE world_profile=? AND turn_id=? AND activity_key='collect_taxes' ORDER BY id DESC LIMIT 1",new String[]{profile(),String.valueOf(turnId)});
        String d=last.optString("degree");
        return "CRITICAL_SUCCESS".equals(d)?2:("SUCCESS".equals(d)||"FAILURE".equals(d))?1:0;
    }

    /** Repeat taxation matters only when taxes were collected in the PREVIOUS turn. */
    private boolean collectedTaxesPreviousTurn(int number){
        if(number<=1)return false;
        JSONObject check=db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_turn_activities a JOIN kingmaker_turns t ON t.id=a.turn_id AND t.world_profile=a.world_profile WHERE a.world_profile=? AND a.activity_key='collect_taxes' AND t.turn_number=?",new String[]{profile(),String.valueOf(number-1)});
        return check.optInt("total")>0;
    }

    /** The critical-failure Claim Hex Stability penalty lasts to the end of the next turn. */
    private int claimHexStabilityPenalty(int turnNumber){
        JSONObject check=db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_turn_activities a JOIN kingmaker_turns t ON t.id=a.turn_id AND t.world_profile=a.world_profile WHERE a.world_profile=? AND a.activity_key='claim_hex' AND a.degree='CRITICAL_FAILURE' AND t.turn_number>=? AND t.turn_number<=?",new String[]{profile(),String.valueOf(Math.max(1,turnNumber-1)),String.valueOf(turnNumber)});
        return check.optInt("total")>0?1:0;
    }

    /** Legacy entrypoint: the documented default Ruin is Corruption. Canonical callers supply their selected Ruin. */
    public JSONObject resolveCollectTaxes(int naturalRoll, String skillKey, String notes) throws Exception {
        return resolveCollectTaxes(naturalRoll,skillKey,"corruption",notes);
    }

    public JSONObject resolveCollectTaxes(int naturalRoll,String skillKey,String ruinChoice,String notes) throws Exception {
        requireEnabled();
        JSONObject turn=activeTurn();
        if(turn.length()==0||!"COMMERCE".equals(turn.optString("phase")))
            throw new IllegalStateException("Coletar Impostos exige a fase de Comércio de um turno ativo.");
        if(taxesCircumstanceBonus(turn.optLong("id"))!=0 ||
           db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_turn_activities WHERE world_profile=? AND turn_id=? AND activity_key='collect_taxes'",new String[]{profile(),String.valueOf(turn.optLong("id"))}).optInt("total")>0)
            throw new IllegalStateException("Só é permitido Coletar Impostos uma vez por turno do Reino.");
        final String ruin=key(ruinChoice);
        if(!("corruption".equals(ruin)||"crime".equals(ruin)||"decay".equals(ruin)||"strife".equals(ruin)))
            throw new IllegalArgumentException("Selecione a Ruína que sofrerá +1 em uma falha crítica: corruption, crime, decay ou strife.");
        boolean repeated=collectedTaxesPreviousTurn(turn.optInt("turn_number"));
        String skill=clean(skillKey).length()>0?key(skillKey):"trade";
        JSONObject check=recordCheck("collect_taxes","Coletar Impostos","Tesoureiro",skill,naturalRoll,0,"Kingmaker Adventure Path p. 530",notes,false);
        String degree=check.optString("degree");
        int bonus="CRITICAL_SUCCESS".equals(degree)?2:("SUCCESS".equals(degree)||"FAILURE".equals(degree))?1:0;
        int unrest="SUCCESS".equals(degree)?(repeated?1:0):"FAILURE".equals(degree)?(repeated?2:1):"CRITICAL_FAILURE".equals(degree)?2:0;
        JSONObject change=new JSONObject();
        if(unrest!=0)change.put("unrest",unrest);
        if("CRITICAL_FAILURE".equals(degree))change.put(ruin,1);
        if(change.length()>0)adjustState(change,"Coletar Impostos, Kingmaker p. 530");
        JSONObject out=new JSONObject().put("check",check).put("economy_circumstance_bonus",bonus)
                .put("unrest_delta",unrest).put("taxed_previous_turn",repeated)
                .put("ruin_increased","CRITICAL_FAILURE".equals(degree)?ruin:"")
                .put("outcome_pt","Coletar Impostos: bônus circunstancial de Economia +"+bonus+
                        " até o fim do turno; Agitação "+(unrest>=0?"+":"")+unrest+
                        ("CRITICAL_FAILURE".equals(degree)?"; Ruína "+ruin+" +1":"")+".");
        return out;
    }
'''
replace_method(target,replacement)
quell='''    public JSONObject resolveQuellUnrest(int naturalRoll, String skillKey, String notes) throws Exception {'''
quellCode=r'''    /** Automatic dice for old callers; new callers can supply real dice and critical-failure choice. */
    public JSONObject resolveQuellUnrest(int naturalRoll,String skillKey,String notes)throws Exception{
        java.util.Random dice=new java.util.Random();
        return resolveQuellUnrest(naturalRoll,skillKey,dice.nextInt(6)+1,dice.nextInt(4)+1,
            "unrest","","",notes);
    }

    public JSONObject resolveQuellUnrest(int naturalRoll,String skillKey,int successD6,int failureD4,
            String criticalFailureChoice,String firstRuin,String secondRuin,String notes)throws Exception{
        requireEnabled();
        JSONObject turn=activeTurn();
        if(turn.length()==0)throw new IllegalStateException("É necessário um turno do Reino ativo.");
        JSONObject repeated=db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_turn_activities WHERE world_profile=? AND turn_id=? AND activity_key='quell_unrest'",
            new String[]{profile(),String.valueOf(turn.optLong("id"))});
        if(repeated.optInt("total")>0)throw new IllegalStateException("Acalmar Agitação: limite de uma tentativa por turno.");
        if(successD6<1||successD6>6||failureD4<1||failureD4>4)
            throw new IllegalArgumentException("Use d6 entre 1 e 6 e d4 entre 1 e 4.");
        final String choice=key(criticalFailureChoice),a=key(firstRuin),b=key(secondRuin);
        if(!"unrest".equals(choice)&&!"ruins".equals(choice))
            throw new IllegalArgumentException("Falha crítica: escolha unrest ou ruins.");
        if("ruins".equals(choice)&&(!isRuinKey(a)||!isRuinKey(b)||a.equals(b)))
            throw new IllegalArgumentException("Escolha duas Ruínas diferentes.");
        String skill=clean(skillKey).length()>0?key(skillKey):"politics";
        if(!("arts".equals(skill)||"folklore".equals(skill)||"intrigue".equals(skill)||
             "magic".equals(skill)||"politics".equals(skill)||"warfare".equals(skill)))
            throw new IllegalArgumentException("Acalmar Agitação exige Artes, Folclore, Intriga, Magia, Política ou Guerra.");
        if(turn.optInt("turn_number")>1){
            JSONObject prev=db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_turn_activities a JOIN kingmaker_turns t ON t.id=a.turn_id AND t.world_profile=a.world_profile WHERE a.world_profile=? AND a.activity_key='quell_unrest' AND a.skill_key=? AND t.turn_number=?",
                new String[]{profile(),skill,String.valueOf(turn.optInt("turn_number")-1)});
            if(prev.optInt("total")>0)
                throw new IllegalStateException("Acalmar Agitação não permite repetir a mesma perícia em turnos consecutivos.");
        }
        JSONObject check=recordCheck("quell_unrest","Acalmar Agitação","Governante",skill,naturalRoll,0,"Kingmaker Adventure Path p. 531",notes,false);
        String d=check.optString("degree");
        int delta="CRITICAL_SUCCESS".equals(d)?-successD6:"SUCCESS".equals(d)?-1:
            "CRITICAL_FAILURE".equals(d)&&"unrest".equals(choice)?failureD4:0;
        JSONObject change=new JSONObject();
        if(delta!=0)change.put("unrest",delta);
        if("CRITICAL_FAILURE".equals(d)&&"ruins".equals(choice))change.put(a,1).put(b,1);
        if(change.length()>0)adjustState(change,"Acalmar Agitação, Kingmaker p. 531");
        JSONObject stateNow=state();
        JSONObject out=new JSONObject().put("check",check).put("unrest_delta",delta)
            .put("critical_success_d6",successD6).put("critical_failure_d4",failureD4)
            .put("critical_failure_choice",choice).put("anarchy",rules.isAnarchy(stateNow.optInt("unrest")));
        if("CRITICAL_FAILURE".equals(d)&&"ruins".equals(choice))
            out.put("ruins_increased",new JSONArray().put(a).put(b));
        out.put("outcome_pt","Agitação "+(delta>=0?"+":"")+delta+
            ("CRITICAL_FAILURE".equals(d)&&"ruins".equals(choice)?" e duas Ruínas +1":"")+".");
        return out;
    }

    private static boolean isRuinKey(String k){
        return "corruption".equals(k)||"crime".equals(k)||"decay".equals(k)||"strife".equals(k);
    }
'''
replace_method(quell,quellCode)
claim='''    public JSONObject resolveClaimHex(String hexId, int naturalRoll, String skillKey, String notes) throws Exception {'''
claimCode=r'''    public JSONObject resolveClaimHex(String hexId,int naturalRoll,String skillKey,String notes)throws Exception{
        requireEnabled();
        JSONObject current=hex(hexId);
        if(current.length()==0||current.optInt("reconnoitered")!=1||current.optInt("cleared")!=1)
            throw new IllegalStateException("Reivindicar Hex exige reconhecimento e ausência de perigos.");
        if(current.optInt("claimed")==1)throw new IllegalStateException("Hex já reivindicado.");
        if(claimedHexCount()>0&&!adjacentToClaimed(hexId))
            throw new IllegalStateException("Hex precisa estar adjacente ao Reino.");
        if(state().optInt("resource_points")<1)
            throw new IllegalStateException("Reivindicar Hex exige 1 RP, gasto mesmo em caso de falha.");
        String skill=clean(skillKey).length()>0?key(skillKey):"wilderness";
        if(!("exploration".equals(skill)||"intrigue".equals(skill)||"magic".equals(skill)||"wilderness".equals(skill)))
            throw new IllegalArgumentException("Reivindicar Hex exige Exploração, Intriga, Magia ou Sobrevivência.");
        JSONObject turn=activeTurn();
        if(turn.length()==0||!"ACTIVITY".equals(turn.optString("phase")))
            throw new IllegalStateException("Reivindicar Hex exige a fase de Atividades do Reino.");
        int level=state().optInt("level",1),maxClaims=level>=9?3:level>=4?2:1;
        int claims=db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_turn_activities WHERE world_profile=? AND turn_id=? AND activity_key='claim_hex'",
            new String[]{profile(),String.valueOf(turn.optLong("id"))}).optInt("total");
        if(claims>=maxClaims)
            throw new IllegalStateException("Limite de Reivindicar Hex por turno: "+maxClaims+" no nível "+level+".");
        JSONObject check=recordCheck("claim_hex","Reivindicar Hex "+clean(hexId),"Guardião",
            skill,naturalRoll,0,"Kingmaker Adventure Path p. 522",notes,false);
        String degree=check.optString("degree");
        JSONObject out=new JSONObject().put("check",check).put("rp_spent",1);
        if("CRITICAL_SUCCESS".equals(degree)||"SUCCESS".equals(degree)){
            JSONObject claimed=claimHex(hexId,notes); // The successful path already deducts exactly 1 RP.
            out.put("hex",claimed).put("success",true);
            if("CRITICAL_SUCCESS".equals(degree))out.put("extra_region_activity",true);
            out.put("outcome_pt","Hex reivindicado; 1 RP gasto."+
                    ("CRITICAL_SUCCESS".equals(degree)?" Pode realizar outra atividade de Região.":""));
        }else{
            adjustState(new JSONObject().put("resource_points",-1),"Reivindicar Hex, Kingmaker p. 522");
            out.put("success",false).put("stability_circumstance_penalty", "CRITICAL_FAILURE".equals(degree)?-1:0)
                .put("stability_penalty_through_turn", "CRITICAL_FAILURE".equals(degree)?state().optInt("turn_number")+1:0)
                .put("outcome_pt","Hex não reivindicado; 1 RP gasto."+
                  ("CRITICAL_FAILURE".equals(degree)?" –1 circunstancial nos testes de Estabilidade até o fim do próximo turno.":""));
        }
        return out;
    }
'''
replace_method(claim,claimCode)
# Route explicit dice and choices through the persistent Android service.
one('''return resolveQuellUnrest(nat, skill, notes);''',
    '''return resolveQuellUnrest(nat, skill, p.optInt("critical_success_d6",new java.util.Random().nextInt(6)+1),
                p.optInt("critical_failure_d4",new java.util.Random().nextInt(4)+1),
                p.optString("critical_failure_choice","unrest"),
                p.optString("first_ruin",""),p.optString("second_ruin",""),notes);''',"quell activity dispatcher")
one('''return resolveCollectTaxes(nat, skill, notes);''',
    '''return resolveCollectTaxes(nat, skill, p.optString("ruin_choice","corruption"),notes);''',"tax activity dispatcher")
src.write_text(s,encoding="utf-8")
v.write_text(oldversion.replace("VERSION_NAME=9.0.36","VERSION_NAME=9.0.37").replace("VERSION_CODE=90036","VERSION_CODE=90037"),encoding="utf-8")
for path in (root/"tests_pf2").glob("test_*.py"):
    original=path.read_text(encoding="utf-8")
    updated=original.replace("VERSION_NAME=9.0.36","VERSION_NAME=9.0.37").replace("VERSION_CODE=90036","VERSION_CODE=90037")
    if original!=updated:path.write_text(updated,encoding="utf-8")
print("KINGMAKER_R36_OFFICIAL_RULE_PATCH_APPLIED",src,flush=True)

#!/usr/bin/env python3
"""R36 HOTFIX: prevent free upkeep and make all consumption changes transactional."""
import sys
from pathlib import Path

REPO=Path("android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java")
START="    public JSONObject resolveConsumption(String payment,int unrestRoll)throws Exception{"
END="\n    public JSONObject activeTurn(){"
REPLACEMENT="""    /** Resolve upkeep without allowing unpaid consumption or partial state writes. */
    public JSONObject resolveConsumption(String payment,int unrestRoll)throws Exception{
        requireEnabled();
        final String methodRequested=key(payment);
        final JSONObject[] resolved={new JSONObject()},prior={new JSONObject()},after={new JSONObject()};
        final int[] amountPaid={0};
        final String[] methodUsed={""};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject turn=activeTurn();
            if(turn.length()==0||!"active".equals(turn.optString("status"))||!"UPKEEP".equals(turn.optString("phase")))
                throw new IllegalStateException("O Consumo só pode ser resolvido na fase de Manutenção de um turno ativo.");
            JSONObject resource=jsonObject(turn.optString("resource_roll_json","{}"));
            if(resource.optBoolean("consumption_resolved"))
                throw new IllegalStateException("O Consumo deste turno já foi resolvido.");
            JSONObject amount=resource.optJSONObject("consumption");
            JSONObject before=state();
            int total=amount==null?before.optInt("consumption",0):amount.optInt("total",-1);
            if(total<0)throw new IllegalStateException("Consumo do turno inválido.");
            String method=total==0?"none":methodRequested;
            if(total>0&&!"food".equals(method)&&!"rp".equals(method)&&!"unrest".equals(method))
                throw new IllegalArgumentException("Consumo positivo exige alimento, RP ou instabilidade; pagamento sem custo proibido.");
            ContentValues values=new ContentValues();
            int rpSpent=0;
            if("food".equals(method)){
                if(before.optInt("food")<total)throw new IllegalStateException("Alimento insuficiente para pagar o Consumo.");
                values.put("food",before.optInt("food")-total);
            }else if("rp".equals(method)){
                if(total>Integer.MAX_VALUE/5)throw new IllegalArgumentException("Consumo excede limite de RP.");
                rpSpent=total*5;
                if(before.optInt("resource_points")<rpSpent)throw new IllegalStateException("RP insuficiente: são necessários "+rpSpent+" RP.");
                values.put("resource_points",before.optInt("resource_points")-rpSpent);
            }else if("unrest".equals(method)){
                if(unrestRoll<1||unrestRoll>4)throw new IllegalArgumentException("Informe o resultado de 1d4 Unrest.");
                int unrest=before.optInt("unrest")+unrestRoll;
                values.put("unrest",unrest);
                values.put("anarchy",rules.isAnarchy(unrest)?1:0);
            }
            values.put("updated_at",System.currentTimeMillis());
            int changed=db.getWritableDatabase().update("kingmaker_state",values,"world_profile=?",new String[]{profile()});
            if(changed!=1)throw new IllegalStateException("Estado do Reino não encontrado ao resolver o Consumo.");
            int spent=resource.optInt("spent_rp",0)+rpSpent;
            resource.put("consumption_resolved",true).put("consumption_payment",method)
                    .put("consumption_unrest","unrest".equals(method)?unrestRoll:0).put("spent_rp",spent);
            ContentValues turnValues=new ContentValues();
            turnValues.put("resource_roll_json",resource.toString());
            int turnChanged=db.getWritableDatabase().update("kingmaker_turns",turnValues,"id=? AND world_profile=? AND phase='UPKEEP' AND status='active'",new String[]{String.valueOf(turn.optLong("id")),profile()});
            if(turnChanged!=1)throw new IllegalStateException("Turno ativo não encontrado ao resolver o Consumo.");
            if(spent>=100)grantMilestoneOnce("spent_100_rp_turn",80,"100 RP gastos em um turno");
            prior[0]=before;after[0]=state();resolved[0]=resource;amountPaid[0]=total;methodUsed[0]=method;
        }});
        audit("Consumo do Reino resolvido",methodUsed[0]+" • "+amountPaid[0],prior[0],after[0]);
        return resolved[0];
    }
"""

def apply(root):
    root=Path(root)
    version=root/"version.properties"
    if not version.is_file() or "VERSION_NAME=9.0.37" not in version.read_text(encoding="utf-8").splitlines():
        raise RuntimeError("Kingmaker R36 / 9.0.37 required")
    path=root/REPO
    src=path.read_text(encoding="utf-8")
    if "Consumo positivo exige alimento, RP ou instabilidade" in src:
        return False
    start=src.find(START)
    end=src.find(END,start)
    if start<0 or end<0 or src.count(START)!=1 or src.count(END)!=1:
        raise ValueError("resolveConsumption method boundaries unexpected; patch rejected")
    path.write_text(src[:start]+REPLACEMENT+src[end:],encoding="utf-8")
    print("KINGMAKER_R36_CONSUMPTION_ATOMIC_HOTFIX_APPLIED",path)
    return True

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_consumption_atomic.py <R36_project>")
    apply(sys.argv[1])

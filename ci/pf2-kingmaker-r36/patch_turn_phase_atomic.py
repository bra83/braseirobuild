#!/usr/bin/env python3
"""R36 Kingmaker: atomic phase advance and no premature/repeated turn completion."""
from pathlib import Path
import sys

SRC=Path("android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java")
START="    public JSONObject advancePhase()throws Exception{"
END="\n    public JSONObject completeTurn()throws Exception{"
REPLACEMENT="""    /** Advance the phase and its world state as one durable transaction. */
    public JSONObject advancePhase()throws Exception{
        requireEnabled();
        JSONObject initial=activeTurn();
        if(initial.length()==0||!"active".equals(initial.optString("status")))
            throw new IllegalStateException("Não há turno de Reino ativo.");
        String next=rules.nextPhase(initial.optString("phase"));
        if("COMPLETE".equals(next))return completeTurn();
        final JSONObject[] before={new JSONObject()},after={new JSONObject()};
        final String[] nextApplied={""};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject turn=activeTurn();
            if(turn.length()==0||!"active".equals(turn.optString("status")))
                throw new IllegalStateException("O turno de Reino foi encerrado.");
            if("UPKEEP".equals(turn.optString("phase"))&&
                    !jsonObject(turn.optString("resource_roll_json","{}")).optBoolean("consumption_resolved"))
                throw new IllegalStateException("Resolva o Consumo antes de concluir a Manutenção.");
            String phase=turn.optString("phase");
            if(!rules.validPhase(phase))throw new IllegalStateException("Fase de Reino inválida.");
            String following=rules.nextPhase(phase);
            if("COMPLETE".equals(following))
                throw new IllegalStateException("A conclusão do turno exige a transação final própria.");
            ContentValues t=new ContentValues();t.put("phase",following);
            int changed=db.getWritableDatabase().update("kingmaker_turns",t,
                    "id=? AND world_profile=? AND phase=? AND status='active'",
                    new String[]{String.valueOf(turn.optLong("id")),profile(),phase});
            if(changed!=1)throw new IllegalStateException("Falha ao atualizar a fase do turno.");
            ContentValues s=new ContentValues();s.put("turn_phase",following);s.put("updated_at",System.currentTimeMillis());
            int stateChanged=db.getWritableDatabase().update("kingmaker_state",s,
                    "world_profile=? AND active_turn_id=?",
                    new String[]{profile(),String.valueOf(turn.optLong("id"))});
            if(stateChanged!=1)throw new IllegalStateException("Falha ao atualizar a fase do Reino.");
            before[0]=turn;after[0]=activeTurn();nextApplied[0]=following;
        }});
        audit("Fase do Reino avançada",nextApplied[0],before[0],after[0]);
        return after[0];
    }
"""
OLD_COMPLETE_PRE='final JSONObject turn=activeTurn();if(turn.length()==0)throw new IllegalStateException("Não há turno ativo.");'
NEW_COMPLETE_PRE='final JSONObject turn=activeTurn();if(turn.length()==0||!"active".equals(turn.optString("status"))||!"EVENT".equals(turn.optString("phase")))throw new IllegalStateException("Conclua as fases de Manutenção, Comércio, Atividades e Eventos antes de encerrar o turno.");'
OLD_COMPLETE_TX='db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{JSONObject before=state();'
NEW_COMPLETE_TX='db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{JSONObject live=activeTurn();if(live.length()==0||live.optLong("id")!=turn.optLong("id")||!"active".equals(live.optString("status"))||!"EVENT".equals(live.optString("phase")))throw new IllegalStateException("Turno mudou antes da conclusão.");if(!jsonObject(live.optString("resource_roll_json","{}")).optBoolean("consumption_resolved"))throw new IllegalStateException("Consumo não resolvido.");JSONObject before=state();'

def apply(root):
    root=Path(root)
    if "VERSION_NAME=9.0.37" not in (root/"version.properties").read_text(encoding="utf-8").splitlines():
        raise RuntimeError("Kingmaker R36 9.0.37 required")
    path=root/SRC
    original=path.read_text(encoding="utf-8")
    if "Falha ao atualizar a fase do turno." in original:return False
    start=original.find(START);end=original.find(END,start)
    if start<0 or end<0 or original.count(START)!=1 or original.count(END)!=1:
        raise ValueError("advancePhase boundary mismatch")
    if original.count(OLD_COMPLETE_PRE)!=1 or original.count(OLD_COMPLETE_TX)<1:
        raise ValueError("completeTurn expected guards absent")
    rewritten=original[:start]+REPLACEMENT+original[end:]
    rewritten=rewritten.replace(OLD_COMPLETE_PRE,NEW_COMPLETE_PRE,1).replace(OLD_COMPLETE_TX,NEW_COMPLETE_TX,1)
    path.write_text(rewritten,encoding="utf-8")
    print("KINGMAKER_R36_PHASE_ATOMIC_HOTFIX_APPLIED",path)
    return True

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_turn_phase_atomic.py <R36_PROJECT>")
    apply(sys.argv[1])

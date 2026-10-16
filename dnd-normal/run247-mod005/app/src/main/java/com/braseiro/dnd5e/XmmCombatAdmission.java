package com.braseiro.dnd5e;

/** Typed admission boundary for the standalone families. This is deliberately
 * not a generic JSON mutation executor. Hit/save outcomes belong to the live
 * compiled host; a caller cannot supply an authoritative hit boolean or defense.
 */
public final class XmmCombatAdmission {
    private XmmCombatAdmission() { }
    public enum Choice { NONE, DAMAGE, GRAPPLE, CHARMED, POISONED, TELEPORT, SAP,
        MANEUVER, SWORD_RETURNED, SWORD_HOVERS }
    public static final class Request {
        public final String pointer,sourceCommit,sourceBlob,operationToken;
        public final long actorId,targetId;
        public final int natural,secondNatural,saveRoll;
        public final Choice choice;
        private final int[] damageDice;
        public Request(String pointer,String sourceCommit,String sourceBlob,
                String operationToken,long actorId,long targetId,int natural,
                int secondNatural,int saveRoll,Choice choice,int[] damageDice) {
            this.pointer=pointer;this.sourceCommit=sourceCommit;this.sourceBlob=sourceBlob;
            this.operationToken=operationToken;this.actorId=actorId;this.targetId=targetId;
            this.natural=natural;this.secondNatural=secondNatural;this.saveRoll=saveRoll;
            this.choice=choice;this.damageDice=damageDice==null?new int[0]:damageDice.clone();
        }
        public int[] damageDice(){return damageDice.clone();}
    }
    public static final class Decision {
        public final XmmCombatSourceRegistry.Entry source;
        public final String blocked;
        private Decision(XmmCombatSourceRegistry.Entry source,String blocked) {
            this.source=source;this.blocked=blocked;
        }
    }
    /** Validate identity before consulting host capability. Never treats the
     * pinned SHA supplied by the caller as proof of semantic correctness. */
    public static Decision preflight(Request request,String persistedActorBlock,
            boolean targetExists) {
        XmmCombatSourceRegistry.Entry entry=request==null?null:
            XmmCombatSourceRegistry.get(request.pointer);
        String blocked="";
        if(request==null)blocked="request_required";
        else if(entry==null)blocked="source_pointer_not_registered";
        else if(!XmmCombatSourceRegistry.SOURCE_COMMIT.equals(request.sourceCommit)||
                !XmmCombatSourceRegistry.SOURCE_BLOB.equals(request.sourceBlob))
            blocked="source_revision_mismatch";
        else if(request.actorId<=0||request.targetId<=0||request.actorId==request.targetId)
            blocked="invalid_actor_or_target";
        else if(!entry.monster.equals(persistedActorBlock))blocked="persisted_actor_source_mismatch";
        else if(!targetExists)blocked="persisted_target_required";
        else if(request.operationToken==null||!request.operationToken.startsWith("ui-monster-")||
                request.operationToken.length()>96||!CombatActionReceiptGuard.validToken(request.operationToken))
            blocked="invalid_operation_token";
        else if(request.natural<0||request.natural>20||request.secondNatural<0||
                request.secondNatural>20||request.saveRoll<0||request.saveRoll>100||
                request.choice==null||request.damageDice.length>128)
            blocked="invalid_manual_inputs";
        else if(entry.status==XmmCombatSourceRegistry.Status.NOT_INTEGRATED)
            blocked="NOT_INTEGRATED";
        // An added enum value alone must never enable a write path.
        if(blocked.isEmpty())blocked="host_executor_not_installed";
        return new Decision(entry,blocked);
    }
}

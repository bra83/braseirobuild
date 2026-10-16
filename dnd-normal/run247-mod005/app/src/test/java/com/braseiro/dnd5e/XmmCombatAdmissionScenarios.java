package com.braseiro.dnd5e;

import java.util.EnumSet;

/** Dependency-free admission regressions; these do NOT exercise SQLite. */
public final class XmmCombatAdmissionScenarios {
    private static int checks;
    private static void equal(Object expected,Object actual) {
        checks++;
        if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);
    }
    private static XmmCombatAdmission.Request request(String pointer,String commit,
            String token,long actor,long target,int[] dice) {
        return new XmmCombatAdmission.Request(pointer,commit,XmmCombatSourceRegistry.SOURCE_BLOB,
            token,actor,target,18,0,0,XmmCombatAdmission.Choice.NONE,dice);
    }
    public static void main(String[] args) {
        equal(62,XmmCombatSourceRegistry.entries().size());
        EnumSet<XmmCombatSourceRegistry.Family> families=EnumSet.noneOf(XmmCombatSourceRegistry.Family.class);
        for(XmmCombatSourceRegistry.Entry entry:XmmCombatSourceRegistry.entries().values()) {
            families.add(entry.family);
            equal(entry,XmmCombatSourceRegistry.get(entry.pointer));
            XmmCombatAdmission.Request valid=request(entry.pointer,XmmCombatSourceRegistry.SOURCE_COMMIT,
                "ui-monster-admission-0001",1,2,new int[]{1,2});
            equal("NOT_INTEGRATED",XmmCombatAdmission.preflight(valid,entry.monster,true).blocked);
            equal("persisted_target_required",XmmCombatAdmission.preflight(valid,entry.monster,false).blocked);
            equal("persisted_actor_source_mismatch",XmmCombatAdmission.preflight(valid,"Unknown",true).blocked);
            equal("source_revision_mismatch",XmmCombatAdmission.preflight(request(entry.pointer,"forged",
                "ui-monster-admission-0001",1,2,null),entry.monster,true).blocked);
            equal("invalid_operation_token",XmmCombatAdmission.preflight(request(entry.pointer,
                XmmCombatSourceRegistry.SOURCE_COMMIT,"gemini-admission-0001",1,2,null),entry.monster,true).blocked);
            equal("invalid_actor_or_target",XmmCombatAdmission.preflight(request(entry.pointer,
                XmmCombatSourceRegistry.SOURCE_COMMIT,"ui-monster-admission-0001",1,1,null),entry.monster,true).blocked);
        }
        equal(6,families.size());
        equal("request_required",XmmCombatAdmission.preflight(null,"",false).blocked);
        equal("source_pointer_not_registered",XmmCombatAdmission.preflight(request(
            "/monster/481/action/1/entries/0",XmmCombatSourceRegistry.SOURCE_COMMIT,
            "ui-monster-admission-0001",1,2,null),"Wraith",true).blocked);
        int[] original={1,2,3,4};
        XmmCombatAdmission.Request copied=request(XmmDamageOrSaveBranch06.SOURCE_POINTER,
            XmmCombatSourceRegistry.SOURCE_COMMIT,"ui-monster-admission-0001",1,2,original);
        original[0]=99;equal(1,copied.damageDice()[0]);
        int[] exposed=copied.damageDice();exposed[0]=99;equal(1,copied.damageDice()[0]);
        try {XmmCombatSourceRegistry.entries().clear();throw new AssertionError("mutable registry");}
        catch(UnsupportedOperationException expected){checks++;}
        System.out.println("XMM_COMBAT_ADMISSION_JVM=PASS assertions="+checks);
        System.out.println("XMM_FAMILIES_04_13_GAMEPLAY=NOT_INTEGRATED");
    }
}

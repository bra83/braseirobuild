package com.braseiro.dnd5e;

/** Pure proof required to commit a compiled gameplay action's database transaction.
 * `accepted` alone only proves a dispatcher returned a result; it never proves mutation.
 */
public final class CompiledEffectCommitGuard {
    private CompiledEffectCommitGuard() {}
    public static boolean shouldCommit(boolean accepted,boolean applied,boolean receipt){
        return accepted && applied && receipt;
    }
}

package com.braseiro.dnd5e;
/** Pure, deterministic validation for persisted combat operation identities. */
public final class CombatActionReceiptGuard {
    private CombatActionReceiptGuard() {}
    public static boolean validToken(String token){
        if(token==null||token.length()<12||token.length()>128)return false;
        for(int i=0;i<token.length();i++){
            char c=token.charAt(i);
            if(!((c>='A'&&c<='Z')||(c>='a'&&c<='z')||(c>='0'&&c<='9')||c=='-'||c=='_'))return false;
        }
        return true;
    }
    public static String replayDecision(String persistedSignature,String requestedSignature){
        return persistedSignature!=null&&persistedSignature.equals(requestedSignature)?"replayed":"operation_token_collision";
    }
}

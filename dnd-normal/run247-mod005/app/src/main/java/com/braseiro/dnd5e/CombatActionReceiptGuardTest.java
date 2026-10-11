package com.braseiro.dnd5e;
/** JDK-only exhaustive receipt/identity regression, not an Android or SQLite test. */
public final class CombatActionReceiptGuardTest {
    private static long assertions;
    private static void ok(boolean b){assertions++;if(!b)throw new AssertionError("case "+assertions);}
    public static void main(String[] args){
        ok(CombatActionReceiptGuard.validToken("abcDEF_123456-xyz"));
        ok(CombatActionReceiptGuard.validToken("123456789012"));
        ok(!CombatActionReceiptGuard.validToken(null));
        ok(!CombatActionReceiptGuard.validToken(""));
        ok(!CombatActionReceiptGuard.validToken("short"));
        ok(!CombatActionReceiptGuard.validToken("a".repeat(129)));
        for(int c=0;c<128;c++){
            char x=(char)c;String token="valid-token-"+x;
            boolean expected=(x>='A'&&x<='Z')||(x>='a'&&x<='z')||(x>='0'&&x<='9')||x=='-'||x=='_';
            ok(CombatActionReceiptGuard.validToken(token)==expected);
        }
        for(int i=0;i<100000;i++){
            String signature="move:"+i;
            ok("replayed".equals(CombatActionReceiptGuard.replayDecision(signature,signature)));
            ok("operation_token_collision".equals(CombatActionReceiptGuard.replayDecision(signature,signature+"x")));
        }
        System.out.println("PASS RUN195_COMBAT_RECEIPTS "+assertions);
    }
}

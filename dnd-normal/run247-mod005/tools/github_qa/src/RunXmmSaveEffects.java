import java.lang.reflect.*;
/** Runs only the pure save-effect contracts. SQLite tests require Robolectric separately. */
public final class RunXmmSaveEffects {
    public static void main(String[] args)throws Exception {
        Class<?> type=Class.forName("com.braseiro.dnd5e.XmmSaveEffectLedgerTest");
        Object suite=type.getConstructor().newInstance();int passed=0,failed=0;
        for(Method test:type.getDeclaredMethods())if(test.getAnnotation(org.junit.Test.class)!=null){
            try{test.invoke(suite);System.out.println("PASS "+test.getName());passed++;}
            catch(InvocationTargetException failure){failed++;failure.getCause().printStackTrace();}
        }
        System.out.println("RESULT "+passed+" passed, "+failed+" failed (pure Java; no SQLite claim)");
        if(failed>0)System.exit(1);
    }
}

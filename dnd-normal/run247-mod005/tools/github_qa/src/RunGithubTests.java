import java.lang.reflect.*;
public class RunGithubTests {
 public static void main(String[] args) throws Exception{
  Class<?> c=Class.forName("com.braseiro.dnd5e.adventure.UniversalGithubModulesIntegrationTest");
  Object obj=c.getConstructor().newInstance();int ok=0;int fail=0;
  for(Method m:c.getDeclaredMethods())if(m.getAnnotation(org.junit.Test.class)!=null){try{m.invoke(obj);System.out.println("PASS "+m.getName());ok++;}catch(InvocationTargetException e){fail++;System.err.println("FAIL "+m.getName()+": "+e.getCause());e.getCause().printStackTrace(System.err);}}
  System.out.println("RESULT "+ok+" passed "+fail+" failed");if(fail>0)System.exit(1);
 }
}

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.lang.model.element.Modifier;
import javax.tools.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;

/**
 * Guarda semântica complementar sem Android SDK.
 * Detecta cadeias diretas new JSONObject().put(...) fora de try/catch ou de
 * métodos que declaram uma exceção compatível. Foi criada porque JSONObject.put
 * lança JSONException verificada no Android e um parse puramente sintático não
 * detecta esse erro de compilação.
 */
public final class CheckedJsonPutValidator {
    private static final Pattern DIRECT_JSON = Pattern.compile("new\\s+(?:org\\.json\\.)?JSONObject\\s*\\(");

    public static void main(String[] args) throws Exception {
        if(args.length!=1){System.err.println("Uso: CheckedJsonPutValidator <pasta-java>");System.exit(2);}
        Path root=Paths.get(args[0]);
        List<Path> files=new ArrayList<>();
        try(java.util.stream.Stream<Path> stream=Files.walk(root)){
            stream.filter(p->p.toString().endsWith(".java")).sorted().forEach(files::add);
        }
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        if(compiler==null){System.err.println("JDK completo não encontrado.");System.exit(2);}
        List<String> errors=new ArrayList<>();
        for(Path path:files){
            DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
            try(StandardJavaFileManager manager=compiler.getStandardFileManager(diagnostics,Locale.ROOT,StandardCharsets.UTF_8)){
                Iterable<? extends JavaFileObject> units=manager.getJavaFileObjects(path.toFile());
                JavacTask task=(JavacTask)compiler.getTask(null,manager,diagnostics,Arrays.asList("-proc:none"),null,units);
                Iterable<? extends CompilationUnitTree> parsed=task.parse();
                Trees trees=Trees.instance(task);
                for(CompilationUnitTree unit:parsed){
                    new TreePathScanner<Void,Void>(){
                        @Override public Void visitMethodInvocation(MethodInvocationTree node,Void unused){
                            String text=node.toString();
                            if(isPut(node)&&DIRECT_JSON.matcher(text).find()&&!isProtected(getCurrentPath())){
                                long pos=trees.getSourcePositions().getStartPosition(unit,node);
                                long line=pos<0?-1:unit.getLineMap().getLineNumber(pos);
                                errors.add(path+":"+line+": JSONObject.put direto sem try/catch nem throws compatível: "+compact(text));
                            }
                            return super.visitMethodInvocation(node,unused);
                        }
                    }.scan(unit,null);
                }
            }
        }
        if(!errors.isEmpty()){
            System.out.println("FALHA NA GUARDA DE EXCEÇÕES JSON");
            for(String e:errors)System.out.println("ERRO  "+e);
            System.exit(1);
        }
        System.out.println("OK: cadeias diretas JSONObject.put estão protegidas contra JSONException verificada.");
    }

    private static boolean isPut(MethodInvocationTree node){
        ExpressionTree select=node.getMethodSelect();
        if(select instanceof MemberSelectTree)return "put".contentEquals(((MemberSelectTree)select).getIdentifier());
        if(select instanceof IdentifierTree)return "put".contentEquals(((IdentifierTree)select).getName());
        return false;
    }

    private static boolean isProtected(TreePath path){
        for(TreePath p=path;p!=null;p=p.getParentPath()){
            Tree leaf=p.getLeaf();
            if(leaf instanceof MethodTree&&declaresCompatible((MethodTree)leaf))return true;
            if(leaf instanceof TryTree&&catchesCompatible((TryTree)leaf))return true;
        }
        return false;
    }

    private static boolean declaresCompatible(MethodTree method){
        for(ExpressionTree thrown:method.getThrows()){
            String t=thrown.toString();
            if(t.endsWith("Exception")||t.endsWith("Throwable"))return true;
        }
        return false;
    }

    private static boolean catchesCompatible(TryTree tree){
        for(CatchTree c:tree.getCatches()){
            String t=c.getParameter().getType().toString();
            for(String part:t.split("\\|")){
                String x=part.trim();
                if(x.endsWith("JSONException")||x.endsWith("Exception")||x.endsWith("Throwable"))return true;
            }
        }
        return false;
    }

    private static String compact(String value){
        String s=value.replaceAll("\\s+"," ").trim();
        return s.length()<=180?s:s.substring(0,180)+"…";
    }
}

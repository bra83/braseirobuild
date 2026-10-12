import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;

/** Valida somente a sintaxe Java, sem exigir Android SDK nem resolver imports. */
public final class JavaSyntaxValidator {
    public static void main(String[] args) throws Exception {
        if(args.length!=1){System.err.println("Uso: JavaSyntaxValidator <pasta-java>");System.exit(2);}
        Path root=Paths.get(args[0]);
        List<Path> files=new ArrayList<Path>();
        try(java.util.stream.Stream<Path> stream=Files.walk(root)){
            stream.filter(p->p.toString().endsWith(".java")).sorted().forEach(files::add);
        }
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        if(compiler==null){System.err.println("JDK completo não encontrado.");System.exit(2);}
        int valid=0;
        for(Path path:files){
            DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<JavaFileObject>();
            StandardJavaFileManager manager=compiler.getStandardFileManager(diagnostics,Locale.ROOT,StandardCharsets.UTF_8);
            Iterable<? extends JavaFileObject> units=manager.getJavaFileObjects(path.toFile());
            JavacTask task=(JavacTask)compiler.getTask(null,manager,diagnostics,Arrays.asList("-proc:none"),null,units);
            try{task.parse();}catch(Throwable ignored){}
            boolean failed=false;
            for(Diagnostic<? extends JavaFileObject> diagnostic:diagnostics.getDiagnostics()){
                if(diagnostic.getKind()!=Diagnostic.Kind.ERROR)continue;
                failed=true;
                System.out.println("ERRO  "+path+":"+diagnostic.getLineNumber()+": "+diagnostic.getMessage(Locale.ROOT));
            }
            manager.close();
            if(!failed){valid++;System.out.println("OK  "+path);}
        }
        System.out.println("RESULTADO DE SINTAXE: "+valid+"/"+files.size()+" arquivos parseados; isto não substitui Gradle/Javac semântico.");
        if(valid!=files.size())System.exit(1);
    }
}

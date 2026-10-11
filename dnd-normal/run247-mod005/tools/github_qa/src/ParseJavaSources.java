import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import com.sun.source.util.JavacTask;

public final class ParseJavaSources {
    public static void main(String[] args) throws Exception {
        if(args.length<1)throw new IllegalArgumentException("Provide .java paths");
        var compiler=ToolProvider.getSystemJavaCompiler();
        var diagnostics=new DiagnosticCollector<JavaFileObject>();
        try(StandardJavaFileManager manager=compiler.getStandardFileManager(diagnostics,null,null)){
            List<File> paths=new ArrayList<>();for(String arg:args)paths.add(new File(arg));
            var files=manager.getJavaFileObjectsFromFiles(paths);
            JavacTask task=(JavacTask)compiler.getTask(null,manager,diagnostics,
                Arrays.asList("-proc:none"),null,files);
            int total=0;for(var parsed:task.parse())total++;
            long errors=diagnostics.getDiagnostics().stream()
                .filter(d->d.getKind()==Diagnostic.Kind.ERROR).count();
            for(Diagnostic<? extends JavaFileObject> d:diagnostics.getDiagnostics())
                if(d.getKind()==Diagnostic.Kind.ERROR)System.err.println(d);
            System.out.println("JDK PARSER: "+total+" files, "+errors+" syntax errors");
            if(total!=args.length||errors>0)System.exit(1);
        }
    }
}

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Auditor sintático que usa o próprio parser do javac, sem precisar do Android SDK. */
public final class JavaAstAudit {
    private static final Set<String> INHERITED = new HashSet<>(Arrays.asList(
        "super", "this", "getApplicationContext", "getSharedPreferences", "getIntent", "setIntent",
        "setContentView", "getContentResolver", "getSystemService", "getPackageManager", "getPackageName",
        "getFilesDir", "getCacheDir", "getAssets", "getWindow", "requestWindowFeature", "runOnUiThread",
        "startActivity", "startActivityForResult", "stopService", "startService", "startForegroundService",
        "recreate", "finish", "isFinishing", "isDestroyed", "getString", "getText", "getDrawable", "getColor",
        "getResources", "getTheme", "getMainLooper", "sendBroadcast", "registerReceiver", "unregisterReceiver",
        "openFileInput", "openFileOutput", "deleteFile", "getFileStreamPath", "getDir", "getDatabasePath",
        "getPreferences", "moveTaskToBack", "shouldShowRequestPermissionRationale", "requestPermissions",
        "getInputData", "getId", "getRunAttemptCount", "isStopped", "setForegroundAsync", "setProgressAsync",
        "checkSelfPermission", "isChangingConfigurations", "getCurrentFocus", "getContext", "getHeight", "getWidth", "getParent", "invalidate",
        "postInvalidateOnAnimation", "postInvalidateDelayed", "setBackgroundColor", "setOrientation", "setPadding", "setWillNotDraw", "setLayerType", "setFocusable", "setMinimumHeight",
        "getReadableDatabase", "getWritableDatabase", "setWriteAheadLoggingEnabled", "startForeground",
        "stopForeground", "stopSelf", "getClass", "toString", "hashCode", "equals", "wait", "notify", "notifyAll"
    ));

    private static final class Collector extends TreePathScanner<Void, Void> {
        final Set<String> methods = new HashSet<>();
        final Set<String> unqualifiedCalls = new HashSet<>();
        final Map<String, Set<Integer>> methodArities = new HashMap<>();
        final Map<String, Integer> varargMinimum = new HashMap<>();
        final Map<String, Set<Integer>> callArities = new HashMap<>();
        @Override public Void visitMethod(MethodTree node, Void unused) {
            String name=node.getName().toString();int count=node.getParameters().size();
            methods.add(name);methodArities.computeIfAbsent(name,key->new HashSet<>()).add(count);
            if(count>0&&node.getParameters().get(count-1).toString().contains("...")){
                int minimum=count-1;Integer old=varargMinimum.get(name);if(old==null||minimum<old)varargMinimum.put(name,minimum);
            }
            return super.visitMethod(node, unused);
        }
        @Override public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
            ExpressionTree selected = node.getMethodSelect();
            if (selected instanceof IdentifierTree){
                String name=((IdentifierTree) selected).getName().toString();unqualifiedCalls.add(name);
                callArities.computeIfAbsent(name,key->new HashSet<>()).add(node.getArguments().size());
            }
            return super.visitMethodInvocation(node, unused);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Uso: JavaAstAudit <pasta-ou-arquivo-java>");
            System.exit(64);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("JDK completo não encontrado. Instale/use o JDK 17, não apenas um JRE.");
            System.exit(65);
        }
        List<File> files = new ArrayList<>();
        for (String argument : args) {
            Path path = Paths.get(argument);
            if (Files.isDirectory(path)) {
                try (java.util.stream.Stream<Path> stream = Files.walk(path)) {
                    stream.filter(value -> value.toString().endsWith(".java")).sorted().forEach(value -> files.add(value.toFile()));
                }
            } else if (argument.endsWith(".java")) files.add(path.toFile());
        }
        if (files.isEmpty()) {
            System.err.println("Nenhum arquivo Java encontrado.");
            System.exit(66);
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Map<String, Collector> collectors = new HashMap<>();
        int syntaxErrors = 0;
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            Iterable<? extends JavaFileObject> units = manager.getJavaFileObjectsFromFiles(files);
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    Arrays.asList("-proc:none", "-XDshouldStopPolicy=PARSE"), null, units);
            for (CompilationUnitTree unit : task.parse()) {
                Collector collector = new Collector();
                collector.scan(unit, null);
                collectors.put(new File(unit.getSourceFile().toUri()).getCanonicalPath(), collector);
            }
        }
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) continue;
            syntaxErrors++;
            String source = diagnostic.getSource() == null ? "<desconhecido>" : diagnostic.getSource().getName();
            System.err.println("ERRO SINTÁTICO: " + source + ":" + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(new Locale("pt", "BR")));
        }

        int suspiciousFiles = 0;
        for (Map.Entry<String, Collector> entry : collectors.entrySet()) {
            List<String> missing = new ArrayList<>();
            for (String call : entry.getValue().unqualifiedCalls) {
                if (INHERITED.contains(call)) continue;
                if (!entry.getValue().methods.contains(call)) { missing.add(call); continue; }
                Set<Integer> declared=entry.getValue().methodArities.get(call);Integer varargMin=entry.getValue().varargMinimum.get(call);
                for(Integer used:entry.getValue().callArities.getOrDefault(call,java.util.Collections.<Integer>emptySet())){
                    boolean accepted=declared!=null&&declared.contains(used);
                    if(!accepted&&varargMin!=null&&used>=varargMin)accepted=true;
                    if(!accepted)missing.add(call+"/"+used+" argumento(s)");
                }
            }
            if (!missing.isEmpty()) {
                java.util.Collections.sort(missing);
                suspiciousFiles++;
                System.err.println("CHAMADA NÃO RESOLVIDA LOCALMENTE: " + entry.getKey() + " -> " + String.join(", ", missing));
            }
        }

        Collector main = null;
        for (Map.Entry<String, Collector> entry : collectors.entrySet()) if (entry.getKey().endsWith(File.separator + "MainActivity.java")) main = entry.getValue();
        int contractErrors = 0;
        if (main == null) {
            System.err.println("CONTRATO: MainActivity.java não encontrado.");
            contractErrors++;
        } else {
            for (String required : Arrays.asList("onCreate", "geminiKey", "migrateLegacyModels")) {
                if (!main.methods.contains(required)) {
                    System.err.println("CONTRATO: método ausente em MainActivity: " + required + "()");
                    contractErrors++;
                }
            }
        }

        System.out.println("AST/JAVAC: " + files.size() + " arquivo(s), " + syntaxErrors + " erro(s) sintático(s), " + suspiciousFiles + " arquivo(s) com chamada não resolvida localmente.");
        if (syntaxErrors > 0 || suspiciousFiles > 0 || contractErrors > 0) System.exit(2);
    }
}

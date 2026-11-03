package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.graphics.BitmapFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Data-only OTA patches. Never loads dex, jar, .so, scripts, or arbitrary code.
 * A patch may replace explicitly opted-in PNG/JPEG/WebP asset reads; all other
 * files still come from the signed APK. Native Java fixes require an APK update.
 */
public final class BraseiroLivePatch {
    public interface Reporter { void onStatus(String message); }
    private static final String SCHEMA="braseiro.livepatch/v1";
    private static final String SYSTEM="dnd";
    private static final String ROOT="https://raw.githubusercontent.com/bra83/braseirobuild/"
            +"feature/dnd-personalidade-estruturada-importador-20261027/.github/braseiro-livepatch/dnd/";
    private static final String MANIFEST=ROOT+"manifest.json";
    private static final String PREF="braseiro_livepatch_dnd";
    private static final long MAX_FILE=20L*1024*1024;
    private static final long MAX_TOTAL=60L*1024*1024;
    private static final AtomicBoolean BUSY=new AtomicBoolean(false);
    private BraseiroLivePatch(){}

    private static File root(Context context){
        return new File(context.getApplicationContext().getFilesDir(),"braseiro_livepatch_dnd");
    }
    private static SharedPreferences preferences(Context c){
        return c.getApplicationContext().getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    private static String validAsset(String s){
        if(s==null||s.length()>180||!s.matches("^(dice|maps|strixhaven)/([A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+\\.(png|jpg|jpeg|webp)$"))
            throw new IllegalArgumentException("Recurso não autorizado para atualização incremental.");
        return s;
    }
    private static String source(String path){
        if(path==null||!path.matches("^(assets/)?([A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+\\.(png|jpg|jpeg|webp)$"))
            throw new IllegalArgumentException("Fonte de recurso inválida.");
        String relative=path.startsWith("assets/")?path:"assets/"+path;
        return ROOT+relative;
    }
    private static InputStream fetch(String url)throws Exception{
        URL u=new URL(url);
        if(!"https".equalsIgnoreCase(u.getProtocol())||
           !"raw.githubusercontent.com".equalsIgnoreCase(u.getHost())||
           !url.startsWith(ROOT))throw new SecurityException("Origem de atualização não autorizada.");
        HttpURLConnection c=(HttpURLConnection)u.openConnection();
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(12000);c.setReadTimeout(35000);
        c.setRequestProperty("User-Agent","Braseiro-DnD-LivePatch/1");
        if(c.getResponseCode()!=200){int code=c.getResponseCode();c.disconnect();throw new IllegalStateException("GitHub HTTP "+code);}
        return new java.io.FilterInputStream(c.getInputStream()){
            public void close()throws java.io.IOException{try{super.close();}finally{c.disconnect();}}
        };
    }
    private static byte[] readManifest()throws Exception{
        try(InputStream in=fetch(MANIFEST);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){
                if(out.size()+n>32768)throw new SecurityException("Manifesto excessivo.");
                out.write(b,0,n);
            }
            return out.toByteArray();
        }
    }
    private static String hex(byte[] digest){
        StringBuilder b=new StringBuilder();
        for(byte x:digest)b.append(String.format(Locale.ROOT,"%02x",x&0xff));
        return b.toString();
    }
    private static void validateImage(File file)throws Exception{
        // Decode headers only; do not allocate bitmaps from untrusted update files.
        BitmapFactory.Options opts=new BitmapFactory.Options();
        opts.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(file.getAbsolutePath(),opts);
        if(opts.outWidth<=0||opts.outHeight<=0||
           opts.outWidth>24000||opts.outHeight>24000)
            throw new SecurityException("Imagem inválida ou dimensões excessivas.");
    }
    private static void download(String source,File target,long size,String digest)throws Exception{
        if(size<1||size>MAX_FILE||digest==null||!digest.matches("[a-fA-F0-9]{64}"))
            throw new SecurityException("Arquivo de atualização sem integridade válida.");
        File parent=target.getParentFile();
        if(!parent.isDirectory()&&!parent.mkdirs())throw new IllegalStateException("Pasta de patch indisponível.");
        MessageDigest sha=MessageDigest.getInstance("SHA-256");
        long count=0;
        try(InputStream in=fetch(source);FileOutputStream out=new FileOutputStream(target)){
            byte[] buffer=new byte[32768];int n;
            while((n=in.read(buffer))!=-1){
                count+=n;
                if(count>size)throw new SecurityException("Arquivo excedeu o tamanho declarado.");
                sha.update(buffer,0,n);out.write(buffer,0,n);
            }
            out.flush();out.getFD().sync();
        }
        if(count!=size||!hex(sha.digest()).equalsIgnoreCase(digest))
            throw new SecurityException("SHA-256 incorreto ou arquivo incompleto.");
        validateImage(target);
    }
    private static void deleteTree(File f){
        if(f==null||!f.exists())return;
        File[] children=f.listFiles();
        if(children!=null)for(File c:children)deleteTree(c);
        f.delete();
    }
    private static long installedCode(Context context)throws Exception{
        PackageInfo pi=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
        return Build.VERSION.SDK_INT>=28?pi.getLongVersionCode():pi.versionCode;
    }
    public static void checkAndApply(Context context,Reporter reporter){
        if(!BUSY.compareAndSet(false,true)){reporter.onStatus("Uma correção incremental já está sendo verificada.");return;}
        final Context app=context.getApplicationContext();
        AppExecutors.network().execute(()->{
            File pending=null;
            try {
                reporter.onStatus("Verificando correções incrementais de conteúdo no GitHub...");
                JSONObject manifest=new JSONObject(new String(readManifest(),java.nio.charset.StandardCharsets.UTF_8));
                if(!SCHEMA.equals(manifest.optString("schema"))||!SYSTEM.equals(manifest.optString("system")))
                    throw new SecurityException("Manifesto de outro sistema ou formato.");
                long min=manifest.optLong("min_core_version_code",-1);
                if(min<0||installedCode(app)<min)
                    throw new IllegalStateException("O núcleo Android precisa de uma atualização de APK antes desta correção.");
                int revision=manifest.optInt("revision",-1);
                if(revision<0)throw new IllegalArgumentException("Revisão do pacote inválida.");
                SharedPreferences prefs=preferences(app);
                if(revision<=prefs.getInt("active",-1)){
                    reporter.onStatus("Conteúdo atualizado. Revisão "+prefs.getInt("active",-1)+".");
                    return;
                }
                JSONArray files=manifest.optJSONArray("files");
                if(files==null||files.length()>100)throw new SecurityException("Lista de recursos inválida.");
                File storage=root(app);
                if(!storage.isDirectory()&&!storage.mkdirs())throw new IllegalStateException("Armazenamento privado indisponível.");
                pending=new File(storage,"stage-"+revision);
                deleteTree(pending);
                if(!pending.mkdirs())throw new IllegalStateException("Falha ao preparar correção.");
                long total=0;
                Set<String> paths=new HashSet<>();
                for(int i=0;i<files.length();i++){
                    JSONObject item=files.optJSONObject(i);
                    if(item==null)throw new SecurityException("Item do patch inválido.");
                    String path=validAsset(item.optString("path",""));
                    if(!paths.add(path))throw new SecurityException("Arquivo duplicado: "+path);
                    long size=item.optLong("size",-1);
                    total+=size;
                    if(size<1||total>MAX_TOTAL)throw new SecurityException("Pacote excede limite seguro.");
                    reporter.onStatus("Atualizando recurso "+(i+1)+"/"+files.length()+": "+path);
                    download(source(item.optString("source","")),new File(pending,path),size,item.optString("sha256",""));
                }
                File finalDir=new File(storage,"rev-"+revision);
                deleteTree(finalDir);
                if(!pending.renameTo(finalDir))throw new IllegalStateException("Falha ao concluir pacote.");
                // Validate all assets are present before switching the active revision.
                for(String path:paths)if(!new File(finalDir,path).isFile())
                    throw new IllegalStateException("Atualização incompleta: "+path);
                int previous=prefs.getInt("active",-1);
                if(!prefs.edit().putInt("previous",previous).putInt("active",revision).commit())
                    throw new IllegalStateException("Não foi possível ativar a correção.");
                pending=null;
                reporter.onStatus("Correção incremental "+revision+" aplicada: "+paths.size()+" recursos. Não foi necessário reinstalar APK.");
            }catch(Exception ex){
                reporter.onStatus("Correção não aplicada: "+String.valueOf(ex.getMessage()));
            }finally{
                if(pending!=null)deleteTree(pending);
                BUSY.set(false);
            }
        });
    }
    /** Only images; never execute/interpret remote scripts or executable code. */
    public static InputStream openAsset(Context c,String path)throws java.io.IOException{
        String name;
        try{name=validAsset(path);}catch(IllegalArgumentException invalid){
            return c.getAssets().open(path);
        }
        int active=preferences(c).getInt("active",-1);
        if(active>=0){
            File replacement=new File(new File(root(c),"rev-"+active),name);
            if(replacement.isFile()&&replacement.length()>0){
                try{return new FileInputStream(replacement);}
                catch(java.io.IOException unreadable){ /* Always retain the signed APK fallback. */ }
            }
        }
        return c.getAssets().open(path);
    }
    public static String currentRevision(Context c){
        int r=preferences(c).getInt("active",-1);
        return r<0?"Nenhuma correção incremental aplicada.":"Revisão de conteúdo "+r+" ativa.";
    }
    public static String rollback(Context c){
        SharedPreferences prefs=preferences(c);
        int previous=prefs.getInt("previous",-1);
        int current=prefs.getInt("active",-1);
        if(current<0)return "Os recursos originais do APK já estão ativos.";
        if(previous>=0 && !new File(root(c),"rev-"+previous).isDirectory())
            return "Revisão anterior não encontrada. A correção atual foi preservada.";
        if(!prefs.edit().putInt("active",previous).putInt("previous",current).commit())
            return "Falha ao restaurar revisão anterior.";
        return previous<0?"Correção desativada. Recursos originais do APK restaurados.":
            "Correção revertida para a revisão "+previous+". Reabra a tela para atualizar os recursos.";

    }
}

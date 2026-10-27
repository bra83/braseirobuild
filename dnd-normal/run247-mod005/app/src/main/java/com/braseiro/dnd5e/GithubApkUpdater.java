package com.braseiro.dnd5e;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** User-consented, package-and-signer-checked APK updates; no code execution from GitHub. */
public final class GithubApkUpdater {
    public interface Reporter { void status(String message); void installationReady(File verifiedApk); }
    private static final String API="https://api.github.com/repos/bra83/braseirobuild/releases?per_page=30";
    private static final long APK_MAX=850L*1024L*1024L;
    private final Activity activity;
    private final AtomicBoolean busy=new AtomicBoolean(false);
    public GithubApkUpdater(Activity a){activity=a;}
    public boolean isBusy(){return busy.get();}
    public void checkAndDownload(Reporter reporter){
        if(!busy.compareAndSet(false,true)){reporter.status("Já existe uma verificação em andamento.");return;}
        AppExecutors.network().execute(()->{
            try {
                reporter.status("Consultando releases oficiais do Braseiro D&D…");
                JSONObject release=latestDndRelease();
                if(release==null){reporter.status("Nenhum APK de D&D publicado no canal oficial.");return;}
                JSONArray assets=release.optJSONArray("assets");
                JSONObject apk=null,sha=null;
                String name="";
                for(int i=0;assets!=null&&i<assets.length();i++){
                    JSONObject a=assets.optJSONObject(i);if(a==null)continue;
                    String n=a.optString("name","");
                    if(n.startsWith("BRASEIRO_DND_")&&n.endsWith(".apk")){apk=a;name=n;break;}
                }
                if(apk==null){reporter.status("Release de D&D sem APK instalável.");return;}
                for(int i=0;assets!=null&&i<assets.length();i++){
                    JSONObject a=assets.optJSONObject(i);if(a!=null && (name+".sha256").equals(a.optString("name")))sha=a;
                }
                if(sha==null){reporter.status("Release recusada: manifesto SHA-256 ausente.");return;}
                final long declared=apk.optLong("size",-1);
                if(declared<1024L||declared>APK_MAX){reporter.status("Release recusada: tamanho APK inválido.");return;}
                File root=activity.getExternalFilesDir("updates");
                if(root==null)root=new File(activity.getFilesDir(),"updates");
                if(!root.exists()&&!root.mkdirs())throw new IllegalStateException("Sem pasta privada para atualização.");
                StatFs stat=new StatFs(root.getAbsolutePath());
                if(stat.getAvailableBytes()<declared+30L*1024L*1024L)throw new IllegalStateException("Sem espaço livre suficiente para baixar o APK.");
                String expected=readText(sha.optString("browser_download_url"),2048).trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
                if(!expected.matches("[0-9a-f]{64}"))throw new IllegalStateException("Manifesto SHA-256 inválido.");
                File partial=new File(root,"braseiro-dnd-update.part");
                File verified=new File(root,"braseiro-dnd-update.apk");
                try{
                    String actual=downloadSha(apk.optString("browser_download_url"),partial,declared,reporter);
                    if(!expected.equals(actual))throw new SecurityException("SHA-256 do APK divergente; atualização recusada.");
                    verifyArchive(partial);
                    if(verified.exists()&&!verified.delete())throw new IllegalStateException("Falha na limpeza do pacote anterior.");
                    if(!partial.renameTo(verified))throw new IllegalStateException("Não foi possível fechar download verificado.");
                    reporter.status("APK verificado: pacote, versão, SHA-256 e assinatura corretos.");
                    reporter.installationReady(verified);
                }finally{if(partial.exists())partial.delete();}
            }catch(Exception ex){reporter.status("Atualização não instalada: "+String.valueOf(ex.getMessage()));}
            finally{busy.set(false);}
        });
    }
    private JSONObject latestDndRelease()throws Exception {
        JSONArray list=new JSONArray(readText(API,3*1024*1024));
        for(int i=0;i<list.length();i++){
            JSONObject r=list.optJSONObject(i);
            if(r==null||r.optBoolean("draft",true)||r.optBoolean("prerelease",false))continue;
            if(r.optString("tag_name","").startsWith("dnd-"))return r;
        }
        return null;
    }
    private static HttpURLConnection connection(String address)throws Exception{
        URL u=new URL(address);if(!"https".equalsIgnoreCase(u.getProtocol()))throw new SecurityException("Somente HTTPS autorizado.");
        String host=u.getHost().toLowerCase(Locale.ROOT);
        if(!(host.equals("api.github.com")||host.equals("github.com")||host.endsWith(".githubusercontent.com")||host.equals("release-assets.githubusercontent.com")))
            throw new SecurityException("Servidor de atualização não autorizado: "+host);
        HttpURLConnection c=(HttpURLConnection)u.openConnection();
        c.setConnectTimeout(20000);c.setReadTimeout(45000);
        c.setRequestProperty("User-Agent","Braseiro-DnD-Verified-Updater");
        c.setRequestProperty("Accept","application/vnd.github+json");
        c.setInstanceFollowRedirects(true);
        return c;
    }
    private static String readText(String url,int limit)throws Exception{
        HttpURLConnection c=connection(url);
        try {
            if(c.getResponseCode()!=200)throw new IllegalStateException("GitHub retornou HTTP "+c.getResponseCode());
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){
                byte[] b=new byte[8192];int n;
                while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new SecurityException("Resposta maior que o limite.");out.write(b,0,n);}
            }
            return out.toString("UTF-8");
        }finally{c.disconnect();}
    }
    private static String downloadSha(String address,File out,long expectedSize,Reporter reporter)throws Exception{
        HttpURLConnection c=connection(address);
        try {
            if(c.getResponseCode()!=200)throw new IllegalStateException("Download retornou HTTP "+c.getResponseCode());
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            long total=0,lastReport=0;
            try(InputStream in=c.getInputStream();FileOutputStream f=new FileOutputStream(out)){
                byte[] buffer=new byte[65536];int n;
                while((n=in.read(buffer))!=-1){
                    total+=n;if(total>APK_MAX||total>expectedSize)throw new SecurityException("Download excedeu o tamanho declarado.");
                    digest.update(buffer,0,n);f.write(buffer,0,n);
                    if(total-lastReport>=16L*1024L*1024L){lastReport=total;reporter.status("Baixando APK: "+(100*total/expectedSize)+"%");}
                }
                f.flush();f.getFD().sync();
            }
            if(total!=expectedSize)throw new SecurityException("Download incompleto.");
            StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",b&0xff));
            return hex.toString();
        }finally{c.disconnect();}
    }
    private void verifyArchive(File apk)throws Exception{
        PackageManager pm=activity.getPackageManager();
        int flags=PackageManager.GET_SIGNING_CERTIFICATES;
        PackageInfo incoming=pm.getPackageArchiveInfo(apk.getAbsolutePath(),flags);
        PackageInfo installed=pm.getPackageInfo(activity.getPackageName(),flags);
        if(incoming==null||installed==null)throw new SecurityException("Não foi possível verificar o pacote Android.");
        if(!activity.getPackageName().equals(incoming.packageName))throw new SecurityException("APK de outro aplicativo.");
        long next=Build.VERSION.SDK_INT>=28?incoming.getLongVersionCode():incoming.versionCode;
        long now=Build.VERSION.SDK_INT>=28?installed.getLongVersionCode():installed.versionCode;
        if(next<=now)throw new SecurityException("A versão publicada não é superior à instalada.");
        Signature[] before=signatures(installed),after=signatures(incoming);
        if(before==null||after==null||before.length==0||after.length==0||before.length!=after.length)throw new SecurityException("Assinatura não pôde ser confirmada.");
        String[] x=new String[before.length],y=new String[after.length];
        for(int i=0;i<before.length;i++)x[i]=before[i].toCharsString();
        for(int i=0;i<after.length;i++)y[i]=after[i].toCharsString();
        Arrays.sort(x);Arrays.sort(y);
        if(!Arrays.equals(x,y))throw new SecurityException("Assinatura incompatível com o app instalado. Saves preservados.");
    }
    private static Signature[] signatures(PackageInfo pi){
        if(Build.VERSION.SDK_INT>=28)return pi.signingInfo==null?null:pi.signingInfo.getApkContentsSigners();
        return pi.signatures;
    }
    public void requestInstall(File verified)throws Exception{
        if(!verified.isFile()||verified.length()<1024L)throw new IllegalArgumentException("Arquivo de atualização ausente.");
        if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
            Intent permission=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName()));
            activity.startActivity(permission);
            throw new IllegalStateException("Autorize instalar apps desta origem e toque novamente para atualizar.");
        }
        verifyArchive(verified);
        Uri content=FileProvider.getUriForFile(activity,activity.getPackageName()+".updates",verified);
        Intent install=new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(content,"application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(install);
    }
}

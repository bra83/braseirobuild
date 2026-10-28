package com.braseiro.dnd5e;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.AtomicFile;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * APK-signer authenticated, atomic, non-executable rule pack delivery.
 * No game database, campaign, character, Mystara, or APK file is modified.
 */
public final class Dnd2024RulePackManager {
    public interface Reporter { void status(String message); }
    private static final String RELEASES="https://api.github.com/repos/bra83/braseirobuild/releases?per_page=100&page=";
    private static final String TAG_PREFIX="dnd-2024-rules-v";
    private static final String PAYLOAD_NAME="DND_2024_RULEPACK_V1.json";
    private static final String SIGNATURE_NAME=PAYLOAD_NAME+".sig";
    private static final int MAX_BYTES=65536;
    private final Context app;
    private final AtomicBoolean busy=new AtomicBoolean(false);

    public Dnd2024RulePackManager(Context context){this.app=context.getApplicationContext();}
    public boolean isBusy(){return busy.get();}

    private static File directory(Context c){
        return new File(c.getFilesDir(),"dnd2024-rulepacks");
    }
    private static AtomicFile active(Context c){return new AtomicFile(new File(directory(c),"active.json"));}
    private static AtomicFile previous(Context c){return new AtomicFile(new File(directory(c),"previous.json"));}

    static boolean verifyBytes(byte[] bytes, byte[] signed, PublicKey key)throws Exception{
        if(bytes==null||signed==null||key==null||bytes.length==0||bytes.length>MAX_BYTES||signed.length>1024)
            return false;
        String algorithm="EC".equalsIgnoreCase(key.getAlgorithm())?"SHA256withECDSA":
            "RSA".equalsIgnoreCase(key.getAlgorithm())?"SHA256withRSA":"";
        if(algorithm.isEmpty())return false;
        java.security.Signature signature=java.security.Signature.getInstance(algorithm);
        signature.initVerify(key);
        signature.update(bytes);
        return signature.verify(signed);
    }
    private PublicKey signingPublicKey()throws Exception{
        PackageManager pm=app.getPackageManager();
        PackageInfo p=pm.getPackageInfo(app.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
        Signature[] signers;
        if(Build.VERSION.SDK_INT>=28)
            signers=p.signingInfo==null?null:p.signingInfo.getApkContentsSigners();
        else signers=p.signatures;
        if(signers==null||signers.length!=1)
            throw new SecurityException("Assinatura do aplicativo ambígua; pacote recusado");
        CertificateFactory factory=CertificateFactory.getInstance("X.509");
        X509Certificate cert=(X509Certificate)factory.generateCertificate(
            new java.io.ByteArrayInputStream(signers[0].toByteArray()));
        return cert.getPublicKey();
    }
    private Dnd2024RulePackCatalog.Snapshot validate(byte[] data, byte[] signature)throws Exception{
        if(!verifyBytes(data,signature,signingPublicKey()))
            throw new SecurityException("Assinatura digital do pacote não confere com a do Braseiro");
        String json=new String(data,StandardCharsets.UTF_8);
        return Dnd2024RulePackCatalog.parse(new JSONObject(json),BuildConfig.VERSION_CODE);
    }
    private static byte[] envelope(byte[] data,byte[] signature)throws Exception{
        return new JSONObject()
            .put("payload",Base64.encodeToString(data,Base64.NO_WRAP))
            .put("signature",Base64.encodeToString(signature,Base64.NO_WRAP))
            .toString().getBytes(StandardCharsets.UTF_8);
    }
    private static byte[][] unwrap(byte[] file)throws Exception{
        JSONObject obj=new JSONObject(new String(file,StandardCharsets.UTF_8));
        if(obj.length()!=2 || !obj.has("payload") || !obj.has("signature"))
            throw new SecurityException("Envelope de regras inválido");
        return new byte[][]{
            Base64.decode(obj.getString("payload"),Base64.DEFAULT),
            Base64.decode(obj.getString("signature"),Base64.DEFAULT)
        };
    }
    private static byte[] read(AtomicFile f)throws Exception{
        try(InputStream in=f.openRead()){
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1){
                if(out.size()+n>MAX_BYTES*2)throw new SecurityException("Arquivo local de regras excede limite");
                out.write(buffer,0,n);
            }
            return out.toByteArray();
        }
    }
    private static void write(AtomicFile file,byte[] bytes)throws Exception{
        FileOutputStream stream=file.startWrite();
        try{stream.write(bytes);file.finishWrite(stream);}
        catch(Exception e){file.failWrite(stream);throw e;}
    }
    private Dnd2024RulePackCatalog.Snapshot verifyEnvelope(byte[] saved)throws Exception{
        byte[][] fields=unwrap(saved);
        return validate(fields[0],fields[1]);
    }
    public synchronized int restore(){
        Dnd2024RulePackCatalog.resetToBuiltin();
        for(AtomicFile target:new AtomicFile[]{active(app),previous(app)}){
            try{
                Dnd2024RulePackCatalog.Snapshot snapshot=verifyEnvelope(read(target));
                Dnd2024RulePackCatalog.activate(snapshot);
                // Recover the active file if its content failed verification.
                if(target.getBaseFile().getName().startsWith("previous")){
                    write(active(app),read(target));
                }
                return snapshot.revision;
            }catch(Exception ignored){/* No unverified data may become active. */}
        }
        return 0;
    }
    public synchronized boolean rollback() {
        try{
            byte[] saved=read(previous(app));
            Dnd2024RulePackCatalog.Snapshot snapshot=verifyEnvelope(saved);
            if(snapshot.revision>=Dnd2024RulePackCatalog.revision())return false;
            write(active(app),saved);
            Dnd2024RulePackCatalog.resetToBuiltin();
            Dnd2024RulePackCatalog.activate(snapshot);
            return true;
        }catch(Exception ignored){return false;}
    }
    private synchronized int install(byte[] data,byte[] sig)throws Exception{
        Dnd2024RulePackCatalog.Snapshot next=validate(data,sig);
        int current=Dnd2024RulePackCatalog.revision();
        if(next.revision<=current)throw new SecurityException("Pacote repetido ou versão inferior");
        File dir=directory(app);
        if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("Diretório privado indisponível");
        // Keep a signed, readable previous version before atomically replacing active.
        try{
            byte[] old=read(active(app));
            verifyEnvelope(old);
            write(previous(app),old);
        }catch(java.io.FileNotFoundException ignored){/* First install. */}
        byte[] nextEnvelope=envelope(data,sig);
        write(active(app),nextEnvelope);
        try{Dnd2024RulePackCatalog.activate(next);}
        catch(Exception e){
            restore();
            throw e;
        }
        return next.revision;
    }
    public void checkAndInstall(Reporter reporter) {
        if(!busy.compareAndSet(false,true)){reporter.status("Consulta das regras em andamento");return;}
        AppExecutors.network().execute(()->{
            try{
                reporter.status("Verificando pacotes D&D 2024 assinados no GitHub…");
                JSONObject release=bestRelease();
                if(release==null){reporter.status("Nenhum pacote de regras 2024 publicado");return;}
                JSONArray assets=release.optJSONArray("assets");
                String dataUrl="",sigUrl="";
                for(int i=0;assets!=null&&i<assets.length();i++){
                    JSONObject asset=assets.optJSONObject(i);if(asset==null)continue;
                    String name=asset.optString("name","");
                    if(PAYLOAD_NAME.equals(name))dataUrl=asset.optString("browser_download_url","");
                    if(SIGNATURE_NAME.equals(name))sigUrl=asset.optString("browser_download_url","");
                }
                if(dataUrl.isEmpty()||sigUrl.isEmpty())
                    throw new SecurityException("Publicação sem par de arquivos assinados");
                byte[] data=download(dataUrl,MAX_BYTES);
                byte[] signatureText=download(sigUrl,2048);
                byte[] signature=Base64.decode(new String(signatureText,StandardCharsets.UTF_8).trim(),Base64.DEFAULT);
                Dnd2024RulePackCatalog.Snapshot candidate=validate(data,signature);
                if(candidate.revision<=Dnd2024RulePackCatalog.revision()){
                    reporter.status("Pacote D&D 2024 já atualizado: revisão "+Dnd2024RulePackCatalog.revision());
                    return;
                }
                int version=install(data,signature);
                reporter.status("Pacote D&D 2024 revisão "+version+" instalado e ativado. Nenhuma ficha ou campanha alterada.");
            }catch(Exception e){reporter.status("Pacote não instalado: "+e.getMessage());}
            finally{busy.set(false);}
        });
    }
    private static JSONObject bestRelease()throws Exception{
        for(int page=1;page<=10;page++){
            JSONArray releases=new JSONArray(new String(download(RELEASES+page,16*1024*1024),StandardCharsets.UTF_8));
            JSONObject best=null;int highest=0;
            for(int i=0;i<releases.length();i++){
                JSONObject r=releases.optJSONObject(i);
                if(r==null||r.optBoolean("draft",false))continue;
                String tag=r.optString("tag_name","");
                if(!tag.matches("dnd-2024-rules-v[0-9]+"))continue;
                int revision;
                try{revision=Integer.parseInt(tag.substring(TAG_PREFIX.length()));}catch(Exception ignored){continue;}
                if(revision>highest){best=r;highest=revision;}
            }
            if(best!=null)return best;
            if(releases.length()<100)break;
        }
        return null;
    }
    private static byte[] download(String address,int max)throws Exception{
        for(int i=0;i<8;i++){
            URL url=new URL(address);
            if(!"https".equalsIgnoreCase(url.getProtocol()))throw new SecurityException("HTTPS obrigatório");
            String host=url.getHost().toLowerCase(Locale.ROOT);
            if(!(host.equals("api.github.com")||host.equals("github.com")||host.endsWith(".githubusercontent.com")))
                throw new SecurityException("Host não autorizado: "+host);
            HttpURLConnection c=(HttpURLConnection)url.openConnection();
            c.setConnectTimeout(18000);c.setReadTimeout(40000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","Braseiro-DnD-RulePack/1");
            try{
                int status=c.getResponseCode();
                if(status==301||status==302||status==303||status==307||status==308){
                    String loc=c.getHeaderField("Location");
                    if(loc==null||loc.isEmpty())throw new SecurityException("Redirecionamento inválido");
                    address=new URL(url,loc).toString();
                    continue;
                }
                if(status!=200)throw new IllegalStateException("GitHub HTTP "+status);
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream in=c.getInputStream()){
                    byte[] buf=new byte[4096];int n;
                    while((n=in.read(buf))!=-1){
                        if(out.size()+n>max)throw new SecurityException("Resposta excessiva");
                        out.write(buf,0,n);
                    }
                }
                return out.toByteArray();
            }finally{c.disconnect();}
        }
        throw new SecurityException("Redirecionamentos excessivos");
    }
}

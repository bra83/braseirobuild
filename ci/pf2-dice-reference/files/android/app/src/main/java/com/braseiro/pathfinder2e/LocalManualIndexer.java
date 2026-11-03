package com.braseiro.pathfinder2e;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

/**
 * Índice textual local e retomável. Não envia PDFs ao Gemini e não consome cota.
 * Usa PDFBox em todas as versões suportadas e OCR latino empacotado para páginas
 * escaneadas. PDFs e imagens nunca saem do aparelho.
 */
public final class LocalManualIndexer {
    public interface Progress { void update(String message,int done,int total); }
    public static final String STORE=AppDb.LOCAL_MANUAL_STORE;
    private final Context context;
    private final ContentResolver resolver;
    private final AppDb db;
    private final DriveService drive;
    private final File cacheDir;
    private volatile boolean cancelRequested=false;

    public LocalManualIndexer(Context context,AppDb db,DriveService drive){
        this.context=context.getApplicationContext();this.resolver=this.context.getContentResolver();this.db=db;this.drive=drive;
        PDFBoxResourceLoader.init(this.context);
        cacheDir=new File(this.context.getFilesDir(),"manual_cache");if(!cacheDir.exists())cacheDir.mkdirs();
    }

    public void requestCancel(){cancelRequested=true;}
    public void clearCancelRequest(){cancelRequested=false;}
    public boolean isCancelRequested(){return cancelRequested;}
    private void checkCancelled()throws Exception{if(cancelRequested)throw new Exception("Indexação pausada com segurança.");}

    public void index(JSONObject manual,boolean force,Progress progress)throws Exception{
        checkCancelled();if(manual==null||manual.length()==0)throw new Exception("Manual inválido.");
        long id=manual.optLong("id");String title=manual.optString("title","Manual");String modified=manual.optString("modified_time","");
        // Não percorre um PDF enorme via ContentProvider apenas para calcular hash antes da cópia.
        // Em SAF/Samsung/Drive isso pode bloquear a leitura (ex.: "sns: Cello error 2") e causar pressão suficiente para ANR.
        String identityKey=sourceKey(manual);String cacheKey=identityKey;
        File pdf=new File(cacheDir,"manual_"+id+"_"+cacheKey.substring(0,16)+".pdf");
        if(!pdf.exists()||pdf.length()<1024){
            db.setManualRagProgress(id,"preparing","Preparando uma única cópia local do PDF...","",STORE,modified,cacheKey,0,0,0,"local");
            if(progress!=null)progress.update(title+" • preparando cópia local",0,1);
            copyManual(manual,pdf,progress);
        }
        if(pdf.length()<1024)throw new Exception("O PDF está vazio ou não pôde ser copiado.");
        String sourceKey=fileSourceKey(pdf);
        if(force)db.clearManualLocalIndex(id);
        db.clearManualLocalIndexExcept(id,sourceKey);

        // Android 15/16 primeiro: extração página a página, sem carregar o manual inteiro em memória.
        checkCancelled();
        TextResult nativeResult=extractWithPlatform(pdf,id,sourceKey,title,modified,progress);
        if(nativeResult.completed&&nativeResult.textPages>0&&nativeResult.failedPages==0){
            db.setManualRagDocument(id,"local:"+sourceKey);
            db.setManualRagProgress(id,"ready",nativeResult.pageCount+" páginas verificadas; "+nativeResult.textPages+" com texto. O Gemini não foi usado.","",STORE,modified,sourceKey,1,1,nativeResult.pageCount,"texto nativo local");
            return;
        }

        // PDFBox/OCR é fallback para Android antigo, PDF escaneado ou página que o leitor nativo não extraiu.
        checkCancelled();
        TextResult portableResult=extractWithPdfBoxAndOcr(pdf,id,sourceKey,title,modified,progress);
        if(portableResult.completed&&portableResult.textPages>0&&portableResult.failedPages==0){
            db.setManualRagDocument(id,"local:"+sourceKey);
            db.setManualRagProgress(id,"ready",portableResult.pageCount+" páginas verificadas; "+portableResult.textPages+" com texto. PDFBox/OCR local, sem upload.","",STORE,modified,sourceKey,1,1,portableResult.pageCount,portableResult.usedOcr?"PDFBox + OCR local":"PDFBox local");
            return;
        }
        if(portableResult.textPages>0&&!portableResult.completed)throw new Exception("A leitura local foi interrompida após "+portableResult.processedPages+" páginas. O progresso foi salvo e será retomado.");
        if(portableResult.failedPages>0){
            db.setManualRagProgress(id,"partial_error","Algumas páginas falharam no leitor local; o progresso válido foi preservado.","",STORE,modified,sourceKey,1,0,portableResult.pageCount,"PDFBox/OCR incompleto");
        }

        checkCancelled();db.clearManualLocalIndex(id);
        db.setManualRagProgress(id,"indexing","Extração nativa indisponível; usando leitura local compatível...","",STORE,modified,sourceKey,1,0,0,"fallback local");
        if(progress!=null)progress.update(title+" • leitura local compatível",0,1);
        String fallback=BasicPdfText.extract(pdf);
        if(fallback.trim().length()<120){
            String reason="PDFBox, OCR local e extração compatível não encontraram texto legível neste PDF. Verifique se o arquivo abre normalmente e não está corrompido.";
            db.setManualRagProgress(id,"needs_ocr",reason,reason,STORE,modified,sourceKey,0,0,nativeResult.pageCount,"sem texto");
            throw new Exception(reason);
        }
        int chunkSize=7000,chunk=0;for(int at=0;at<fallback.length();at+=chunkSize){String text=fallback.substring(at,Math.min(fallback.length(),at+chunkSize)).trim();if(text.length()<30)continue;chunk++;db.upsertManualLocalPage(id,sourceKey,chunk,title,text,normalize(text));}
        // O fallback não preserva a correspondência entre bloco e página PDF.
        // Ele permanece disponível para diagnóstico, mas nunca é anunciado como
        // índice mecânico pronto nem usado para citar uma página inexistente.
        db.setManualRagDocument(id,"unpaged:"+sourceKey);
        String reason=chunk+" trechos recuperados sem numeração confiável. Reprocesse com PDFBox/OCR para habilitar regras mecânicas e citações de página.";
        db.setManualRagProgress(id,"partial_unpaged",reason,reason,STORE,modified,sourceKey,1,0,Math.max(chunk,nativeResult.pageCount),"fallback_unpaged");
        throw new Exception(reason);
    }

    private TextResult extractWithPdfBoxAndOcr(File pdf,long manualId,String sourceKey,String title,String modified,Progress progress){
        TextResult result=new TextResult();PDDocument document=null;ParcelFileDescriptor pfd=null;PdfRenderer renderer=null;TextRecognizer recognizer=null;
        try{
            document=PDDocument.load(pdf);int total=document.getNumberOfPages();result.pageCount=total;
            PDFTextStripper stripper=new PDFTextStripper();
            pfd=ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY);renderer=new PdfRenderer(pfd);
            for(int i=0;i<total;i++){
                checkCancelled();int pageNo=i+1;
                if(db.hasManualLocalTextPage(manualId,sourceKey,pageNo)){result.textPages++;result.processedPages=pageNo;continue;}
                stripper.setStartPage(pageNo);stripper.setEndPage(pageNo);String text=cleanText(stripper.getText(document));
                if(text.length()<20){
                    if(recognizer==null)recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                    PdfRenderer.Page page=null;Bitmap bitmap=null;
                    try{
                        page=renderer.openPage(i);float scale=Math.min(3.2f,Math.max(2.0f,1800f/Math.max(1,page.getWidth())));
                        int width=Math.max(1,Math.round(page.getWidth()*scale)),height=Math.max(1,Math.round(page.getHeight()*scale));
                        if(height>2600){float fit=2600f/height;width=Math.max(1,Math.round(width*fit));height=2600;}
                        bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xffffffff);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        text=cleanText(Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap,0)),90,TimeUnit.SECONDS).getText());result.usedOcr=true;
                    }catch(Throwable pageError){result.failedPages++;AppLog.w("ManualIndexer","Falha ao processar a página "+pageNo+" de "+title+".",pageError);text="";}finally{if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();try{if(page!=null)page.close();}catch(Throwable ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}}
                }
                db.upsertManualLocalPage(manualId,sourceKey,pageNo,title,text.length()>=20?text:"",text.length()>=20?normalize(text):"",ManualPageStructureAnalyzer.analyze(text,pageNo));
                if(text.length()>=20)result.textPages++;result.processedPages=pageNo;cooperativeYield(pageNo);
                if(pageNo%5==0||pageNo==total){String msg="Página "+pageNo+" de "+total+(result.usedOcr?" • OCR local quando necessário":"");db.setManualRagProgress(manualId,"indexing",msg,"",STORE,modified,sourceKey,1,0,total,result.usedOcr?"PDFBox + OCR local":"PDFBox local");if(progress!=null)progress.update(title+" • "+msg,pageNo,total);}
            }
            result.completed=result.processedPages>=total;
        }catch(Throwable error){AppLog.w("ManualIndexer","PDFBox/OCR não conseguiu processar "+title+"; tentando extrator alternativo.",error);}finally{
            try{if(recognizer!=null)recognizer.close();}catch(Throwable ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}
            try{if(renderer!=null)renderer.close();}catch(Throwable ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}
            try{if(pfd!=null)pfd.close();}catch(Throwable ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}
            try{if(document!=null)document.close();}catch(Throwable ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}
        }
        return result;
    }

    private TextResult extractWithPlatform(File pdf,long manualId,String sourceKey,String title,String modified,Progress progress){
        TextResult result=new TextResult();ParcelFileDescriptor pfd=null;PdfRenderer renderer=null;
        try{
            pfd=ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY);renderer=new PdfRenderer(pfd);int total=renderer.getPageCount();result.pageCount=total;
            int resume=Math.min(total,db.maxManualLocalPage(manualId,sourceKey));result.processedPages=resume;result.textPages=countTextPages(manualId,sourceKey);int empty=0;
            for(int i=resume;i<total;i++){
                checkCancelled();PdfRenderer.Page page=null;String text="";try{page=renderer.openPage(i);text=pageText(page);}catch(Throwable pageError){result.failedPages++;AppLog.w("ManualIndexer","Falha no extrator nativo na página "+(i+1)+" de "+title+".",pageError);}finally{if(page!=null)page.close();}
                text=cleanText(text);if(text.length()>=20){db.upsertManualLocalPage(manualId,sourceKey,i+1,title,text,normalize(text),ManualPageStructureAnalyzer.analyze(text,i+1));result.textPages++;empty=0;}else{db.upsertManualLocalPage(manualId,sourceKey,i+1,title,"","",ManualPageStructureAnalyzer.analyze("",i+1));empty++;}
                result.processedPages=i+1;cooperativeYield(i+1);
                if((i+1)%5==0||i+1==total){String msg="Página "+(i+1)+" de "+total;db.setManualRagProgress(manualId,"indexing",msg,"",STORE,modified,sourceKey,1,0,total,"texto nativo local");if(progress!=null)progress.update(title+" • "+msg,i+1,total);}
                // Se o método de texto não existe, não percorre centenas de páginas inutilmente.
                if(i<Math.min(total,8)&&empty>=Math.min(8,total)&&result.textPages==0)break;
            }
            result.completed=result.processedPages>=total;
        }catch(Throwable error){AppLog.w("ManualIndexer","Extrator de PDF da plataforma falhou em "+title+"; tentando fallback compatível.",error);}finally{try{if(renderer!=null)renderer.close();}catch(Throwable ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}try{if(pfd!=null)pfd.close();}catch(Throwable ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}}
        return result;
    }

    private static String pageText(Object page){
        try{
            Method m=page.getClass().getMethod("getTextContents");Object contents=m.invoke(page);if(!(contents instanceof List))return "";StringBuilder out=new StringBuilder();
            for(Object item:(List<?>)contents){if(item==null)continue;String text="";try{Method gm=item.getClass().getMethod("getText");Object value=gm.invoke(item);if(value!=null)text=String.valueOf(value);}catch(Throwable ignored){text=String.valueOf(item);}if(text.length()>0){if(out.length()>0)out.append('\n');out.append(text);}}
            return out.toString();
        }catch(Throwable ignored){return "";}
    }

    private void copyManual(JSONObject manual,File target,Progress progress)throws Exception{
        InputStream in=null;FileOutputStream out=null;File partial=new File(target.getAbsolutePath()+".part");boolean completed=false;
        long existing=partial.exists()?Math.max(0L,partial.length()):0L;long total=Math.max(1L,manual.optLong("size_bytes",1L));boolean append=existing>0;
        try{
            String local=manual.optString("local_uri","");
            if(local.length()>0){
                Uri uri=Uri.parse(local);if("file".equalsIgnoreCase(uri.getScheme()))in=new FileInputStream(new File(uri.getPath()));else in=resolver.openInputStream(uri);
                if(in!=null&&existing>0){long skipped=skipFully(in,existing);if(skipped<existing){try{in.close();}catch(Exception ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}in=("file".equalsIgnoreCase(uri.getScheme()))?new FileInputStream(new File(uri.getPath())):resolver.openInputStream(uri);existing=0;append=false;}}
            }else{
                if(drive==null||!drive.isConnected())throw new Exception("Conecte o Google Drive uma vez para baixar este manual. Depois o índice funciona offline.");
                DriveService.DownloadStream download=drive.openDownloadFile(manual.optString("drive_id"),existing);in=download.stream;if(existing>0&&!download.resumed){existing=0;append=false;}if(download.totalBytes>0)total=download.totalBytes;
            }
            if(in==null)throw new Exception("Não foi possível abrir o PDF.");out=new FileOutputStream(partial,append);byte[] buffer=new byte[131072];int n;long copied=existing,lastNotice=existing;
            while((n=in.read(buffer))>0){checkCancelled();out.write(buffer,0,n);copied+=n;if(progress!=null&&(copied-lastNotice>=4L*1024L*1024L||copied>=total)){lastNotice=copied;progress.update(manual.optString("title")+" • "+(existing>0?"retomando ":"copiando ")+human(copied),(int)Math.min(Integer.MAX_VALUE,copied),(int)Math.min(Integer.MAX_VALUE,total));}}
            out.flush();try{out.getFD().sync();}catch(Exception ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}if(target.exists()&&!target.delete())throw new Exception("Não foi possível substituir a cópia local antiga.");if(!partial.renameTo(target))throw new Exception("Não foi possível concluir a cópia local do PDF.");completed=true;
        }finally{try{if(in!=null)in.close();}catch(Exception ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}try{if(out!=null)out.close();}catch(Exception ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}if(completed&&partial.exists())partial.delete();}
    }

    private static long skipFully(InputStream in,long amount)throws Exception{long done=0;byte[] discard=new byte[65536];while(done<amount){long n=in.skip(amount-done);if(n>0){done+=n;continue;}int read=in.read(discard,0,(int)Math.min(discard.length,amount-done));if(read<0)break;done+=read;}return done;}

    public long cacheBytes(){long total=0L;for(File root:cacheRoots())total+=directoryBytes(root);File general=context.getCacheDir();File[] files=general.listFiles();if(files!=null)for(File f:files)if(isManualTempFile(f))total+=directoryBytes(f);return total;}

    /** Limpa somente cópias temporárias. O índice textual, os PDFs originais,
     * a campanha e o ponto de retomada por página permanecem no banco. */
    public int clearCachePreservingIndex(){int removed=0;for(File root:cacheRoots())removed+=deleteRecursively(root,true);File general=context.getCacheDir();File[] files=general.listFiles();if(files!=null)for(File f:files)if(isManualTempFile(f))removed+=deleteRecursively(f,true);if(!cacheDir.exists())cacheDir.mkdirs();return removed;}
    private File[] cacheRoots(){return new File[]{cacheDir,new File(context.getCacheDir(),"rag_pdf_parts"),new File(context.getCacheDir(),"manual_cache"),new File(context.getCacheDir(),"manual_pages"),new File(context.getCacheDir(),"manual_thumbnails"),new File(context.getFilesDir(),"rag_pdf_parts")};}
    private static boolean isManualTempFile(File f){if(f==null)return false;String n=f.getName().toLowerCase(Locale.US);return n.startsWith("auto_manual_")||(n.startsWith("manual_")&&n.endsWith(".part"))||n.endsWith(".pdf.part")||n.startsWith("rag_manual_");}

    private static long directoryBytes(File root){if(root==null||!root.exists())return 0L;if(root.isFile())return Math.max(0L,root.length());long total=0L;File[] children=root.listFiles();if(children!=null)for(File child:children)total+=directoryBytes(child);return total;}
    private static long matchingCacheBytes(File root,String prefix){long total=0L;if(root==null)return total;File[] files=root.listFiles();if(files!=null)for(File f:files)if(f.getName().startsWith(prefix))total+=directoryBytes(f);return total;}
    private static int deleteChildren(File root){int count=0;if(root==null)return 0;File[] children=root.listFiles();if(children!=null)for(File child:children)count+=deleteRecursively(child,true);return count;}
    private static int deleteRecursively(File file,boolean deleteRoot){if(file==null||!file.exists())return 0;int count=0;if(file.isDirectory()){File[] children=file.listFiles();if(children!=null)for(File child:children)count+=deleteRecursively(child,true);}if(deleteRoot&&file.delete())count++;return count;}

    public static String sourceKey(JSONObject manual)throws Exception{
        String value=STORE+"|"+manual.optString("drive_id")+"|"+manual.optString("local_uri")+"|"+manual.optString("modified_time")+"|"+manual.optLong("size_bytes");MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] hash=d.digest(value.getBytes("UTF-8"));StringBuilder b=new StringBuilder();for(byte x:hash)b.append(String.format(Locale.US,"%02x",x&255));return b.toString();
    }
    private static String fileSourceKey(File file)throws Exception{FileInputStream in=new FileInputStream(file);try{return streamSourceKey(in);}finally{in.close();}}
    private static String streamSourceKey(InputStream in)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");d.update(STORE.getBytes("UTF-8"));d.update((byte)0);byte[] buffer=new byte[131072];int n;while((n=in.read(buffer))>0)d.update(buffer,0,n);byte[] hash=d.digest();StringBuilder b=new StringBuilder();for(byte x:hash)b.append(String.format(Locale.US,"%02x",x&255));return b.toString();}
    public static String duplicateKey(String title){return ManualIdentityPolicy.duplicateKey(title);}
    public static String normalize(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    private static String cleanText(String x){if(x==null)return "";return x.replace('\u0000',' ').replaceAll("[ \\t]+"," ").replaceAll("\\n{3,}","\\n\\n").trim();}
    private static void cooperativeYield(int pageNo){if(pageNo%2!=0)return;try{Thread.sleep(12L);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    private static String human(long bytes){if(bytes<1024)return bytes+" B";double v=bytes/1024.0;if(v<1024)return String.format(Locale.US,"%.1f KB",v);return String.format(Locale.US,"%.1f MB",v/1024.0);}
    private int countTextPages(long manualId,String sourceKey){return db.countManualLocalTextPages(manualId,sourceKey);}
    private static final class TextResult{int processedPages;int textPages;int pageCount;int failedPages;boolean completed;boolean usedOcr;}

    /** Extrator limitado, usado somente quando a API de texto do Android não existe. */
    private static final class BasicPdfText {
        private static final Pattern STREAM=Pattern.compile("(?s)(<<.*?>>)\\s*stream\\r?\\n(.*?)\\r?\\nendstream");
        private static final Pattern TEXT=Pattern.compile("(?s)BT(.*?)ET");
        private static final Pattern LITERAL=Pattern.compile("\\((?:\\\\.|[^\\\\)])*\\)\\s*(?:Tj|'|\")|\\[(.*?)\\]\\s*TJ");
        static String extract(File file)throws Exception{if(file.length()>32L*1024L*1024L)throw new Exception("PDF grande demais para o extrator de memória compatível. Use o processamento página a página ou uma cópia comprimida com camada de texto.");byte[] all=readBytes(file);String raw=new String(all,"ISO-8859-1");StringBuilder out=new StringBuilder();Matcher streams=STREAM.matcher(raw);int count=0;while(streams.find()&&count<12000){count++;String dict=streams.group(1),body=streams.group(2);byte[] bytes=body.getBytes("ISO-8859-1");if(dict.contains("FlateDecode"))try{bytes=inflate(bytes);}catch(Exception ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}String content=new String(bytes,"ISO-8859-1");Matcher bt=TEXT.matcher(content);while(bt.find())appendTextOps(bt.group(1),out);if(out.length()>8_000_000)break;}return out.toString();}
        private static void appendTextOps(String block,StringBuilder out){Matcher m=LITERAL.matcher(block);while(m.find()){if(m.group(0).startsWith("(")){String g=m.group(0);int end=findLiteralEnd(g);if(end>1)appendDecoded(g.substring(1,end),out);}else{String arr=m.group(1);if(arr!=null){Matcher lit=Pattern.compile("\\((?:\\\\.|[^\\\\)])*\\)").matcher(arr);while(lit.find())appendDecoded(lit.group().substring(1,lit.group().length()-1),out);Matcher hex=Pattern.compile("<([0-9A-Fa-f]{4,})>").matcher(arr);while(hex.find())appendHex(hex.group(1),out);}}}if(out.length()>0)out.append('\n');}
        private static int findLiteralEnd(String g){boolean esc=false;for(int i=1;i<g.length();i++){char c=g.charAt(i);if(esc){esc=false;continue;}if(c=='\\'){esc=true;continue;}if(c==')')return i;}return -1;}
        private static void appendDecoded(String s,StringBuilder out){StringBuilder b=new StringBuilder();for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c!='\\'){b.append(c);continue;}if(++i>=s.length())break;char n=s.charAt(i);if(n=='n'||n=='r'||n=='t')b.append(' ');else if(n>='0'&&n<='7'){String oct=""+n;for(int k=0;k<2&&i+1<s.length()&&s.charAt(i+1)>='0'&&s.charAt(i+1)<='7';k++)oct+=s.charAt(++i);try{b.append((char)Integer.parseInt(oct,8));}catch(Exception ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}}else b.append(n);}String text=b.toString().replaceAll("[\\p{Cntrl}&&[^\\n\\t]]"," ").trim();if(text.length()>1){if(out.length()>0)out.append(' ');out.append(text);}}
        private static void appendHex(String hex,StringBuilder out){try{if(hex.length()%2==1)hex+="0";byte[] b=new byte[hex.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(hex.substring(i*2,i*2+2),16);String s;if(b.length>2&&b[0]==(byte)0xFE&&b[1]==(byte)0xFF)s=new String(b,2,b.length-2,"UTF-16BE");else s=new String(b,"ISO-8859-1");appendDecoded(s,out);}catch(Exception ignored){AppLog.ignored("LocalManualIndexer","Falha tolerada; a execução continuou.",ignored);}}
        private static byte[] inflate(byte[] b)throws Exception{InflaterInputStream in=new InflaterInputStream(new java.io.ByteArrayInputStream(b));ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[32768];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();return out.toByteArray();}
        private static byte[] readBytes(File f)throws Exception{FileInputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream((int)Math.min(f.length(),16*1024*1024));byte[] buf=new byte[131072];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();return out.toByteArray();}
    }
}

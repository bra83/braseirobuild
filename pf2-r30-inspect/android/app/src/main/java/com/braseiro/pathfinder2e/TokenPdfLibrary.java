package com.braseiro.pathfinder2e;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.DocumentsContract;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Biblioteca de folhas de tokens em PDF.
 *
 * O catálogo guarda somente metadados. Uma página é renderizada e recortada
 * apenas quando o usuário a abre. Miniaturas ficam no cache; o recorte em alta
 * qualidade só é criado quando o token é vinculado a um personagem.
 */
public final class TokenPdfLibrary {
    private final Context context;
    private static final String DEFAULT_TOKEN_DRIVE_FOLDER="https://drive.google.com/drive/folders/1gfBQfV6x0KAri0pdyKXpMhVlQYblLD-W";
    private final ContentResolver resolver;
    private final SharedPreferences prefs;
    private final DriveService drive;

    public TokenPdfLibrary(Context context) {
        this.context=context.getApplicationContext();
        this.resolver=this.context.getContentResolver();
        this.prefs=this.context.getSharedPreferences("settings",Context.MODE_PRIVATE);
        this.drive=new DriveService(this.context);
    }

    public List<JSONObject> listPdfs(Uri treeUri) throws Exception {
        ArrayList<JSONObject> safFiles=new ArrayList<JSONObject>();Exception safFailure=null;
        boolean driveProvider=isGoogleDriveTree(treeUri);
        if(treeUri!=null){
            try{
                String treeId=DocumentsContract.getTreeDocumentId(treeUri);
                java.util.HashSet<String> seenDocuments=new java.util.HashSet<String>();
                java.util.HashSet<String> seenFolders=new java.util.HashSet<String>();
                scanChildrenPaged(treeUri,treeId,safFiles,seenDocuments,seenFolders,0);
            }catch(Exception e){safFailure=e;}
        }

        // A pasta escolhida pelo seletor Android tem prioridade absoluta. A API
        // legada nunca pode substituir silenciosamente outra fonte.
        if(treeUri!=null){
            if(safFailure!=null)throw safFailure;
            Collections.sort(safFiles,new Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){return a.optString("name").compareToIgnoreCase(b.optString("name"));}});
            return safFiles;
        }

        String driveFolder=prefs.getString("token_pdf_drive_folder",DEFAULT_TOKEN_DRIVE_FOLDER);
        if(drive.isConnected()&&driveFolder.trim().length()>0){
            List<JSONObject> remote=drive.listFolderRecursiveBlocking(driveFolder);
            ArrayList<JSONObject> drivePdfs=new ArrayList<JSONObject>();
            java.util.HashSet<String> seenIds=new java.util.HashSet<String>();
            for(JSONObject f:remote){
                String name=f.optString("name","");String mime=f.optString("mimeType","");
                if(!"application/pdf".equalsIgnoreCase(mime)&&!name.toLowerCase(Locale.US).endsWith(".pdf"))continue;
                String id=f.optString("id","");if(id.length()==0||!seenIds.add(id))continue;
                JSONObject item=new JSONObject();item.put("documentId",id);item.put("sourceUri","gdrive://"+id);item.put("name",name);item.put("mimeType",mime);item.put("size",parseLong(f.optString("size","0")));item.put("modifiedTime",parseDriveTime(f.optString("modifiedTime","")));item.put("path",f.optString("path",name));drivePdfs.add(item);
            }
            Collections.sort(drivePdfs,new Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){return a.optString("name").compareToIgnoreCase(b.optString("name"));}});
            return drivePdfs;
        }
        return safFiles;
    }

    private boolean isGoogleDriveTree(Uri uri){
        if(uri==null)return false;String authority=uri.getAuthority();String raw=uri.toString().toLowerCase(Locale.US);
        return (authority!=null&&authority.toLowerCase(Locale.US).contains("google.android.apps.docs"))||raw.contains("com.google.android.apps.docs")||raw.contains("drive.google.com");
    }

    private static long parseLong(String value){try{return Long.parseLong(value);}catch(Exception ignored){return 0;}}
    private static long parseDriveTime(String value){
        if(value==null||value.length()==0)return 0;
        String[] patterns={"yyyy-MM-dd'T'HH:mm:ss.SSSX","yyyy-MM-dd'T'HH:mm:ssX"};
        for(String pattern:patterns)try{SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.parse(value).getTime();}catch(Exception ignored){AppLog.ignored("TokenPdfLibrary","Falha tolerada; a execução continuou.",ignored);}
        return 0;
    }

    private void scanChildrenPaged(Uri treeUri,String parentId,ArrayList<JSONObject> out,java.util.HashSet<String> seenDocuments,java.util.HashSet<String> seenFolders,int depth) throws Exception {
        if(depth>20||parentId==null||parentId.length()==0||!seenFolders.add(parentId))return;
        List<DocumentRow> rows=queryChildrenStable(treeUri,parentId);
        for(DocumentRow row:rows){
            if(row.id==null||row.id.length()==0)continue;
            if(isDirectoryMime(row.mime)){
                scanChildrenPaged(treeUri,row.id,out,seenDocuments,seenFolders,depth+1);
                continue;
            }
            if(!seenDocuments.add(row.id))continue;
            String name=row.name==null?"":row.name;
            String mime=row.mime==null?"":row.mime;
            if(!"application/pdf".equalsIgnoreCase(mime)&&!name.toLowerCase(Locale.US).endsWith(".pdf"))continue;
            Uri documentUri=DocumentsContract.buildDocumentUriUsingTree(treeUri,row.id);
            JSONObject item=new JSONObject();
            item.put("documentId",row.id);item.put("sourceUri",documentUri.toString());item.put("name",name);item.put("mimeType",mime);item.put("size",row.size);item.put("modifiedTime",row.modified);out.add(item);
        }
    }

    private List<DocumentRow> queryChildrenStable(Uri treeUri,String parentId) throws Exception {
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,parentId);
        String[] projection={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_LAST_MODIFIED};
        java.util.LinkedHashMap<String,DocumentRow> all=new java.util.LinkedHashMap<String,DocumentRow>();
        String previousSignature="";int stable=0;
        for(int attempt=0;attempt<12&&stable<2;attempt++){
            Cursor c=null;boolean loading=false;
            try{c=resolver.query(children,projection,null,null,null);if(c==null)throw new Exception("O provedor não permitiu ler a pasta selecionada.");collectRows(c,projection,all);Bundle extras=c.getExtras();loading=extras!=null&&extras.getBoolean(DocumentsContract.EXTRA_LOADING,false);}finally{if(c!=null)c.close();}
            String signature=signature(all);if(signature.equals(previousSignature)&&!loading)stable++;else stable=0;previousSignature=signature;
            if(loading||stable<2)try{Thread.sleep(300L);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
        }
        if(android.os.Build.VERSION.SDK_INT>=26){
            java.util.HashSet<String> pageSignatures=new java.util.HashSet<String>();
            for(int offset=0;offset<50000;offset+=100){
                Cursor c=null;java.util.ArrayList<String> ids=new java.util.ArrayList<String>();
                try{Bundle args=new Bundle();args.putInt(ContentResolver.QUERY_ARG_LIMIT,100);args.putInt(ContentResolver.QUERY_ARG_OFFSET,offset);c=resolver.query(children,projection,args,new CancellationSignal());if(c==null)break;int idCol=c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);while(c.moveToNext()){String id=idCol>=0?c.getString(idCol):"";if(id!=null&&id.length()>0)ids.add(id);}}finally{if(c!=null)c.close();}
                if(ids.isEmpty())break;String pageSignature=ids.size()+":"+ids.get(0)+":"+ids.get(ids.size()-1);if(!pageSignatures.add(pageSignature))break;
                Cursor full=null;try{Bundle args=new Bundle();args.putInt(ContentResolver.QUERY_ARG_LIMIT,100);args.putInt(ContentResolver.QUERY_ARG_OFFSET,offset);full=resolver.query(children,projection,args,new CancellationSignal());if(full!=null)collectRows(full,projection,all);}finally{if(full!=null)full.close();}
                if(ids.size()<100)break;
            }
        }
        return new java.util.ArrayList<DocumentRow>(all.values());
    }

    private void collectRows(Cursor c,String[] projection,java.util.LinkedHashMap<String,DocumentRow> out){
        int idCol=c.getColumnIndex(projection[0]),nameCol=c.getColumnIndex(projection[1]),mimeCol=c.getColumnIndex(projection[2]),sizeCol=c.getColumnIndex(projection[3]),modCol=c.getColumnIndex(projection[4]);
        while(c.moveToNext()){String id=idCol>=0?c.getString(idCol):"";if(id==null||id.length()==0)continue;DocumentRow row=new DocumentRow();row.id=id;row.name=nameCol>=0&&!c.isNull(nameCol)?c.getString(nameCol):"";row.mime=mimeCol>=0&&!c.isNull(mimeCol)?c.getString(mimeCol):"";row.size=sizeCol>=0&&!c.isNull(sizeCol)?c.getLong(sizeCol):0;row.modified=modCol>=0&&!c.isNull(modCol)?c.getLong(modCol):0;out.put(id,row);}
    }

    private String signature(java.util.LinkedHashMap<String,DocumentRow> rows){if(rows.isEmpty())return "0";java.util.ArrayList<String> ids=new java.util.ArrayList<String>(rows.keySet());return ids.size()+":"+ids.get(0)+":"+ids.get(ids.size()-1);}
    private boolean isDirectoryMime(String mime){if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime))return true;String low=mime==null?"":mime.toLowerCase(Locale.US);return low.contains("google-apps.folder")||low.endsWith("/folder");}
    private static final class DocumentRow { String id,name,mime; long size,modified; }


    private File ensureLocalPdf(long pdfId,Uri pdfUri) throws Exception {
        File dir=new File(context.getCacheDir(),"pdf_token_library/sources");if(!dir.exists()&&!dir.mkdirs())throw new Exception("Não foi possível criar o cache de PDFs.");
        File file=new File(dir,pdfId+".pdf");if(file.exists()&&file.length()>1024){file.setLastModified(System.currentTimeMillis());return file;}
        InputStream in;
        if(pdfUri!=null&&"gdrive".equalsIgnoreCase(pdfUri.getScheme())){
            String fileId=pdfUri.getHost();if(fileId==null||fileId.length()==0){fileId=pdfUri.getSchemeSpecificPart();if(fileId!=null&&fileId.startsWith("//"))fileId=fileId.substring(2);}
            if(fileId==null||fileId.length()==0)throw new Exception("Referência do PDF no Drive inválida.");
            in=drive.downloadFile(fileId);
        }else{
            in=resolver.openInputStream(pdfUri);
        }
        if(in==null)throw new Exception("O provedor não entregou o PDF.");
        final long maxPdfBytes=1024L*1024L*1024L;
        long freeBytes=new StatFs(dir.getAbsolutePath()).getAvailableBytes();
        if(freeBytes<64L*1024L*1024L){try{in.close();}catch(Exception ignored){AppLog.ignored("TokenPdfLibrary","Falha tolerada; a execução continuou.",ignored);}throw new Exception("Espaço livre insuficiente para preparar o PDF de tokens.");}
        File temp=new File(dir,pdfId+".download");long total=0L;
        try(InputStream source=in;FileOutputStream out=new FileOutputStream(temp,false)){
            byte[] buffer=new byte[65536];int n;while((n=source.read(buffer))>0){total+=n;if(total>maxPdfBytes)throw new Exception("PDF de tokens maior que 1 GB; selecione um arquivo menor.");out.write(buffer,0,n);}out.flush();out.getFD().sync();
        }catch(Exception error){if(temp.exists())temp.delete();throw error;}
        if(total<1024L){if(temp.exists())temp.delete();throw new Exception("O provedor retornou um PDF vazio ou incompleto.");}
        if(!hasPdfHeader(temp)){if(temp.exists())temp.delete();throw new Exception("O arquivo recebido não possui um cabeçalho PDF válido.");}
        if(!temp.renameTo(file)){
            try(InputStream tin=new java.io.FileInputStream(temp);FileOutputStream copy=new FileOutputStream(file,false)){byte[] buffer=new byte[65536];int n;while((n=tin.read(buffer))>0)copy.write(buffer,0,n);copy.flush();copy.getFD().sync();}
            if(!temp.delete())AppLog.w("TokenPdfLibrary","Arquivo temporário de PDF não pôde ser removido.",null);
        }
        file.setLastModified(System.currentTimeMillis());return file;
    }
    private boolean hasPdfHeader(File file){if(file==null||!file.exists()||file.length()<5)return false;try(InputStream in=new java.io.FileInputStream(file)){byte[] head=new byte[5];int n=in.read(head);return n==5&&head[0]=='%'&&head[1]=='P'&&head[2]=='D'&&head[3]=='F'&&head[4]=='-';}catch(Exception e){AppLog.w("TokenPdfLibrary","Falha ao verificar cabeçalho PDF.",e);return false;}}

    public void invalidatePdf(long pdfId){File file=new File(context.getCacheDir(),"pdf_token_library/sources/"+pdfId+".pdf");if(file.exists())file.delete();File temp=new File(context.getCacheDir(),"pdf_token_library/sources/"+pdfId+".download");if(temp.exists())temp.delete();}

    public int pageCount(long pdfId,Uri pdfUri) throws Exception {
        File source=ensureLocalPdf(pdfId,pdfUri);ParcelFileDescriptor pfd=null;PdfRenderer renderer=null;
        try{pfd=ParcelFileDescriptor.open(source,ParcelFileDescriptor.MODE_READ_ONLY);renderer=new PdfRenderer(pfd);return renderer.getPageCount();}
        finally{if(renderer!=null)try{renderer.close();}catch(Exception ignored){AppLog.ignored("TokenPdfLibrary","Falha tolerada; a execução continuou.",ignored);}if(pfd!=null)try{pfd.close();}catch(Exception ignored){AppLog.ignored("TokenPdfLibrary","Falha tolerada; a execução continuou.",ignored);}}
    }

    public Bitmap renderPage(long pdfId,Uri pdfUri,int pageNo,int targetWidth) throws Exception {
        File source=ensureLocalPdf(pdfId,pdfUri);ParcelFileDescriptor pfd=null;PdfRenderer renderer=null;PdfRenderer.Page page=null;
        try{
            pfd=ParcelFileDescriptor.open(source,ParcelFileDescriptor.MODE_READ_ONLY);renderer=new PdfRenderer(pfd);if(pageNo<0||pageNo>=renderer.getPageCount())throw new Exception("Página fora do intervalo.");page=renderer.openPage(pageNo);
            int width=Math.max(720,targetWidth);int height=Math.max(1,Math.round(width*(page.getHeight()/(float)page.getWidth())));Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);return bitmap;
        }finally{if(page!=null)try{page.close();}catch(Exception ignored){AppLog.ignored("TokenPdfLibrary","Falha tolerada; a execução continuou.",ignored);}if(renderer!=null)try{renderer.close();}catch(Exception ignored){AppLog.ignored("TokenPdfLibrary","Falha tolerada; a execução continuou.",ignored);}if(pfd!=null)try{pfd.close();}catch(Exception ignored){AppLog.ignored("TokenPdfLibrary","Falha tolerada; a execução continuou.",ignored);}}
    }

    public List<JSONObject> processPage(long pdfId,Uri pdfUri,int pageNo,int columns,int rows,double marginX,double marginY) throws Exception {return processPage(pdfId,pdfUri,pageNo,columns,rows,marginX,marginY,1d-marginX,1d-marginY);}

    public List<JSONObject> processPage(long pdfId,Uri pdfUri,int pageNo,int columns,int rows,double gridLeft,double gridTop,double gridRight,double gridBottom) throws Exception {
        Bitmap page=renderPage(pdfId,pdfUri,pageNo,1800);ArrayList<JSONObject> result=new ArrayList<JSONObject>();
        try{
            columns=Math.max(1,Math.min(12,columns));rows=Math.max(1,Math.min(16,rows));gridLeft=Math.max(0,Math.min(.92,gridLeft));gridTop=Math.max(0,Math.min(.92,gridTop));gridRight=Math.max(gridLeft+.05,Math.min(1,gridRight));gridBottom=Math.max(gridTop+.05,Math.min(1,gridBottom));
            float usableW=(float)(gridRight-gridLeft);float usableH=(float)(gridBottom-gridTop);float cellW=usableW/columns;float cellH=usableH/rows;float crop=Math.min(cellW,cellH)*.93f;
            File dir=new File(context.getCacheDir(),"pdf_token_library/thumbs/"+pdfId+"/p"+pageNo);if(!dir.exists()&&!dir.mkdirs())throw new Exception("Não foi possível criar o cache de miniaturas.");
            for(int r=0;r<rows;r++)for(int col=0;col<columns;col++){
                int index=r*columns+col;float centerX=(float)gridLeft+(col+.5f)*cellW;float centerY=(float)gridTop+(r+.5f)*cellH;float x=centerX-crop/2f;float y=centerY-crop/2f;
                Bitmap thumb=circleCrop(page,x,y,crop,crop,220);File file=new File(dir,String.format(Locale.US,"token_%03d.png",index));FileOutputStream fos=new FileOutputStream(file);thumb.compress(Bitmap.CompressFormat.PNG,92,fos);fos.close();thumb.recycle();
                JSONObject t=new JSONObject();t.put("tokenIndex",index);t.put("row",r);t.put("column",col);t.put("x",x);t.put("y",y);t.put("width",crop);t.put("height",crop);t.put("thumbPath",file.getAbsolutePath());result.add(t);
            }
        }finally{page.recycle();}
        return result;
    }

    public File extractFullToken(long pdfId,Uri pdfUri,int pageNo,int tokenIndex,double x,double y,double width,double height) throws Exception {
        Bitmap page=renderPage(pdfId,pdfUri,pageNo,2800);Bitmap token=null;
        try{token=circleCrop(page,(float)x,(float)y,(float)width,(float)height,640);File dir=new File(context.getFilesDir(),"pdf_tokens/"+pdfId);if(!dir.exists()&&!dir.mkdirs())throw new Exception("Não foi possível criar a biblioteca local.");File file=new File(dir,"p"+pageNo+"_t"+tokenIndex+".png");FileOutputStream fos=new FileOutputStream(file);token.compress(Bitmap.CompressFormat.PNG,96,fos);fos.close();return file;}
        finally{if(token!=null)token.recycle();page.recycle();}
    }

    private static Bitmap circleCrop(Bitmap source,float nx,float ny,float nw,float nh,int outputSize) {
        int left=Math.max(0,Math.round(nx*source.getWidth()));int top=Math.max(0,Math.round(ny*source.getHeight()));int right=Math.min(source.getWidth(),Math.round((nx+nw)*source.getWidth()));int bottom=Math.min(source.getHeight(),Math.round((ny+nh)*source.getHeight()));
        if(right<=left)right=Math.min(source.getWidth(),left+1);if(bottom<=top)bottom=Math.min(source.getHeight(),top+1);
        Bitmap out=Bitmap.createBitmap(outputSize,outputSize,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(out);canvas.drawColor(Color.TRANSPARENT);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);Path path=new Path();path.addCircle(outputSize/2f,outputSize/2f,outputSize/2f,Path.Direction.CW);canvas.save();canvas.clipPath(path);Rect src=new Rect(left,top,right,bottom);RectF dst=new RectF(0,0,outputSize,outputSize);canvas.drawBitmap(source,src,dst,paint);canvas.restore();return out;
    }

    public long cacheBytes(){return directoryBytes(new File(context.getCacheDir(),"pdf_token_library"));}
    public void pruneCache(long maxBytes){File root=new File(context.getCacheDir(),"pdf_token_library");ArrayList<File> files=new ArrayList<File>();collectFiles(root,files);long total=0;for(File f:files)total+=f.length();if(total<=maxBytes)return;Collections.sort(files,new Comparator<File>(){public int compare(File a,File b){return Long.compare(a.lastModified(),b.lastModified());}});for(File f:files){if(total<=maxBytes)break;long size=f.length();if(f.delete())total-=size;}}
    private static void collectFiles(File file,List<File> out){if(file==null||!file.exists())return;if(file.isFile()){out.add(file);return;}File[] children=file.listFiles();if(children!=null)for(File child:children)collectFiles(child,out);}
    private static long directoryBytes(File file){if(file==null||!file.exists())return 0;if(file.isFile())return file.length();long total=0;File[] children=file.listFiles();if(children!=null)for(File child:children)total+=directoryBytes(child);return total;}
}

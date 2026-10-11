package com.braseiro.dnd5e;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Exporta o diário como um livro simples, legível e inteiramente local. */
public final class AdventurePdfExporter {
    private final Context context;
    public AdventurePdfExporter(Context context){this.context=context.getApplicationContext();}

    public File export(JSONObject campaign,List<JSONObject> journal)throws Exception{
        File dir=new File(context.getCacheDir(),"exports");if(!dir.exists())dir.mkdirs();
        File file=new File(dir,"aventura_"+System.currentTimeMillis()+".pdf");PdfDocument pdf=new PdfDocument();
        Paint title=new Paint(Paint.ANTI_ALIAS_FLAG);title.setColor(Color.rgb(75,44,20));title.setTextSize(25);title.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));
        Paint heading=new Paint(Paint.ANTI_ALIAS_FLAG);heading.setColor(Color.rgb(110,62,25));heading.setTextSize(16);heading.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));
        Paint body=new Paint(Paint.ANTI_ALIAS_FLAG);body.setColor(Color.rgb(35,28,22));body.setTextSize(11.5f);body.setTypeface(Typeface.create(Typeface.SERIF,Typeface.NORMAL));
        Paint meta=new Paint(body);meta.setColor(Color.DKGRAY);meta.setTextSize(9.5f);
        ArrayList<JSONObject> entries=new ArrayList<JSONObject>(journal==null?Collections.<JSONObject>emptyList():journal);Collections.reverse(entries);
        int pageNo=0,y=0;PdfDocument.Page page=null;Canvas canvas=null;
        for(int section=-1;section<entries.size();section++){
            JSONObject entry=section<0?null:entries.get(section);
            String sectionTitle=section<0?campaign.optString("name","Crônicas do Braseiro"):entry.optString("title","Cena");
            String text=section<0?"Campanha de D&D\nPerfil de regras: "+campaign.optString("rules_profile")+"\nTom: "+campaign.optString("tone")+"\n\n"+campaign.optString("notes"):entry.optString("body");
            List<String> lines=wrap(text,body,500);
            if(page==null||y+70+lines.size()*16>780){if(page!=null)pdf.finishPage(page);pageNo++;page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());canvas=page.getCanvas();canvas.drawColor(Color.rgb(250,245,232));y=55;canvas.drawText("Braseiro D&D • Crônicas da Campanha",45,25,meta);}
            canvas.drawText(sectionTitle,45,y,title);y+=27;if(entry!=null){canvas.drawText(entry.optString("kind")+" • registro "+entry.optLong("created_at"),45,y,meta);y+=18;}
            for(String line:lines){if(y>805){pdf.finishPage(page);pageNo++;page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());canvas=page.getCanvas();canvas.drawColor(Color.rgb(250,245,232));y=55;}canvas.drawText(line,45,y,body);y+=16;}y+=22;
        }
        if(page!=null)pdf.finishPage(page);FileOutputStream out=new FileOutputStream(file);pdf.writeTo(out);out.close();pdf.close();return file;
    }


    /** Exporta um manuscrito em voz de bardo, mantendo os fatos já compostos no manuscrito. */
    public File exportBardChronicle(JSONObject campaign,JSONObject manuscript)throws Exception{
        File dir=new File(context.getCacheDir(),"exports");if(!dir.exists())dir.mkdirs();
        File file=new File(dir,"balada_"+System.currentTimeMillis()+".pdf");PdfDocument pdf=new PdfDocument();
        Paint title=new Paint(Paint.ANTI_ALIAS_FLAG);title.setColor(Color.rgb(94,54,23));title.setTextSize(27);title.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));
        Paint subtitle=new Paint(Paint.ANTI_ALIAS_FLAG);subtitle.setColor(Color.rgb(112,77,42));subtitle.setTextSize(13);subtitle.setTypeface(Typeface.create(Typeface.SERIF,Typeface.ITALIC));
        Paint heading=new Paint(Paint.ANTI_ALIAS_FLAG);heading.setColor(Color.rgb(118,66,26));heading.setTextSize(18);heading.setTypeface(Typeface.create(Typeface.SERIF,Typeface.BOLD));
        Paint body=new Paint(Paint.ANTI_ALIAS_FLAG);body.setColor(Color.rgb(42,31,23));body.setTextSize(11.8f);body.setTypeface(Typeface.create(Typeface.SERIF,Typeface.NORMAL));
        Paint italic=new Paint(body);italic.setTypeface(Typeface.create(Typeface.SERIF,Typeface.ITALIC));italic.setColor(Color.rgb(72,55,39));
        Paint meta=new Paint(body);meta.setColor(Color.rgb(104,89,72));meta.setTextSize(9.5f);
        int pageNo=1;PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());Canvas canvas=page.getCanvas();canvas.drawColor(Color.rgb(249,241,220));
        String mainTitle=manuscript.optString("title","A Balada de "+campaign.optString("name","Braseiro"));String sub=manuscript.optString("subtitle","Uma crônica contada numa taverna.");String bard=manuscript.optString("bard_name","Bardo do Braseiro");
        int y=145;for(String line:wrap(mainTitle,title,500)){canvas.drawText(line,48,y,title);y+=34;}y+=8;for(String line:wrap(sub,subtitle,490)){canvas.drawText(line,50,y,subtitle);y+=19;}y+=30;canvas.drawText("Como cantada por "+bard,50,y,heading);y+=28;
        String heroes=manuscript.optString("heroes","");if(heroes.length()>0){for(String line:wrap("Companhia: "+heroes,meta,480)){canvas.drawText(line,50,y,meta);y+=14;}y+=10;}
        String opening=manuscript.optString("opening","");for(String line:wrap(opening,italic,480)){canvas.drawText(line,50,y,italic);y+=17;}canvas.drawText("Braseiro D&D • Crônica de Bardo",50,810,meta);pdf.finishPage(page);
        JSONArray chapters=manuscript.optJSONArray("chapters");if(chapters==null)chapters=new JSONArray();
        for(int i=0;i<chapters.length();i++){
            JSONObject ch=chapters.optJSONObject(i);if(ch==null)continue;pageNo++;page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());canvas=page.getCanvas();canvas.drawColor(Color.rgb(249,241,220));y=58;canvas.drawText("Braseiro D&D • "+mainTitle,45,25,meta);canvas.drawText(String.valueOf(pageNo),535,815,meta);
            String chTitle=ch.optString("title","Capítulo "+(i+1));canvas.drawText("CAPÍTULO "+(i+1),45,y,meta);y+=22;for(String line:wrap(chTitle,heading,500)){canvas.drawText(line,45,y,heading);y+=23;}y+=10;
            List<String> lines=wrap(ch.optString("text",""),body,500);for(String line:lines){if(y>790){pdf.finishPage(page);pageNo++;page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());canvas=page.getCanvas();canvas.drawColor(Color.rgb(249,241,220));y=55;canvas.drawText("Braseiro D&D • "+mainTitle,45,25,meta);canvas.drawText(String.valueOf(pageNo),535,815,meta);}canvas.drawText(line,45,y,body);y+=16;}
            pdf.finishPage(page);page=null;
        }
        String closing=manuscript.optString("closing","");if(closing.length()>0){pageNo++;page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());canvas=page.getCanvas();canvas.drawColor(Color.rgb(249,241,220));y=150;canvas.drawText("ÚLTIMOS VERSOS",45,y,heading);y+=35;for(String line:wrap(closing,italic,500)){canvas.drawText(line,45,y,italic);y+=18;}String policy=manuscript.optString("source_policy","");if(policy.length()>0){y+=35;for(String line:wrap(policy,meta,490)){canvas.drawText(line,45,y,meta);y+=14;}}canvas.drawText(String.valueOf(pageNo),535,815,meta);pdf.finishPage(page);}
        FileOutputStream out=new FileOutputStream(file);pdf.writeTo(out);out.close();pdf.close();return file;
    }

    private static List<String> wrap(String text,Paint paint,float width){
        ArrayList<String> lines=new ArrayList<String>();for(String paragraph:(text==null?"":text).replace("\r","").split("\n",-1)){if(paragraph.length()==0){lines.add("");continue;}StringBuilder line=new StringBuilder();for(String word:paragraph.split("\\s+")){String test=line.length()==0?word:line+" "+word;if(paint.measureText(test)>width&&line.length()>0){lines.add(line.toString());line.setLength(0);line.append(word);}else{if(line.length()>0)line.append(' ');line.append(word);}}if(line.length()>0)lines.add(line.toString());}return lines;
    }
}

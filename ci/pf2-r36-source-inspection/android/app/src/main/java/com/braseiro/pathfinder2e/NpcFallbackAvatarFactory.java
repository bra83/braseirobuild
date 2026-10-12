package com.braseiro.pathfinder2e;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.text.Normalizer;
import java.util.Locale;

/** Cria um retrato local persistente quando a biblioteca não possui correspondência segura. */
public final class NpcFallbackAvatarFactory {
    private NpcFallbackAvatarFactory(){}
    public static String create(Context context,JSONObject npc)throws Exception{
        String name=npc==null?"NPC":npc.optString("name","NPC");String corpus=norm(name+" "+npc.optString("summary")+" "+npc.optString("visual_description")+" "+npc.optString("stats_json"));
        int size=512;Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        int seed=Math.abs((name+corpus).hashCode());int[] palette={Color.rgb(61,88,72),Color.rgb(83,68,56),Color.rgb(56,72,91),Color.rgb(91,61,71),Color.rgb(86,82,51)};int bg=palette[seed%palette.length];c.drawColor(bg);
        p.setColor(Color.rgb(28,35,31));c.drawCircle(256,278,230,p);p.setColor(Color.rgb(210,178,135));c.drawCircle(256,205,105,p);
        Path shoulders=new Path();shoulders.moveTo(70,500);shoulders.cubicTo(95,330,180,300,256,300);shoulders.cubicTo(332,300,417,330,442,500);shoulders.close();p.setColor(Color.rgb(72+(seed%45),62+((seed/7)%40),48+((seed/13)%35)));c.drawPath(shoulders,p);
        p.setColor(Color.rgb(45,34,28));float hair=contains(corpus,"idoso","velho","elderly")?22:35;c.drawArc(150,92,362,290,190,160,true,p);
        if(contains(corpus,"barba","beard")){p.setColor(Color.rgb(70,54,42));Path beard=new Path();beard.moveTo(175,220);beard.quadTo(256,370,337,220);beard.quadTo(300,315,256,338);beard.quadTo(212,315,175,220);c.drawPath(beard,p);}
        p.setStrokeWidth(12);p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(40,35,30));if(contains(corpus,"oculos","glasses","escriba","registr")){c.drawCircle(218,200,35,p);c.drawCircle(294,200,35,p);c.drawLine(253,200,259,200,p);}p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(242,215,132));p.setTextAlign(Paint.Align.CENTER);p.setFakeBoldText(true);p.setTextSize(54);String label=initials(name);c.drawText(label,256,468,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(16);p.setColor(Color.rgb(211,183,91));c.drawCircle(256,256,244,p);
        File dir=new File(context.getFilesDir(),"npc_avatars");if(!dir.exists()&&!dir.mkdirs())throw new Exception("não foi possível criar npc_avatars");File out=new File(dir,"generated_"+npc.optLong("id",Math.abs(name.hashCode()))+"_"+slug(name)+".png");FileOutputStream stream=new FileOutputStream(out,false);bitmap.compress(Bitmap.CompressFormat.PNG,95,stream);stream.close();bitmap.recycle();return Uri.fromFile(out).toString();
    }
    private static boolean contains(String s,String... values){for(String v:values)if(s.contains(norm(v)))return true;return false;}
    private static String initials(String name){StringBuilder b=new StringBuilder();for(String part:name.trim().split("\\s+"))if(part.length()>0&&b.length()<2)b.append(Character.toUpperCase(part.charAt(0)));return b.length()==0?"NPC":b.toString();}
    private static String slug(String s){String n=norm(s).replace(' ','_');return n.length()>40?n.substring(0,40):n;}
    private static String norm(String s){return Normalizer.normalize(s==null?"":s.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();}
}

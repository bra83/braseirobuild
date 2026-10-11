package com.braseiro.dnd5e;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Mesa de dados 3D física e inteiramente local.
 *
 * Invariante principal: o Java nunca aceita um número "sorteado" pelo renderer.
 * A orientação final dos corpos produz physicalFaces; DiceRollSpec reconstrói os
 * valores lógicos a partir dessas faces e rejeita qualquer divergência com rolls.
 * Isso garante que a face visível é a mesma registrada no histórico e usada pelas
 * regras de D&D.
 */
public final class Dice3DOverlay {
    private static final long WATCHDOG_MS=27000L;
    private static final int MAX_FACE_IMAGE_BYTES=512*1024;
    private static final int MAX_FACE_IMAGE_EDGE=256;
    private static final Set<String> MATERIALS=new HashSet<String>(Arrays.asList("obsidiana","pedra","metal","osso","madeira","cristal","vidro","marfim","couro"));
    private static final Set<String> PALETTES=new HashSet<String>(Arrays.asList("âmbar","marfim","rubi","esmeralda","safira","violeta","cinza","cobre"));
    private static final Set<String> FONTS=new HashSet<String>(Arrays.asList("serif","sans","mono","rúnica"));
    private static final Set<String> EDGES=new HashSet<String>(Arrays.asList("suave","marcada","metálica"));
    private static final Set<String> TEXTURES=new HashSet<String>(Arrays.asList("liso","granulado","veios","rachado","martelado","couro gravado"));

    public interface Callback {void onResult(RollResult result);void onError(String message);void onCancel();}
    public interface FaceImagePicker {void pick(String actorName,long actorCharacterId,int sides,int face);}

    public static final class RollResult {
        public final List<Integer> rolls,physicalFaces;
        public final int total,natural,physicsMs;
        public final long physicsSeed;
        public final String detail,critical,formula,mode,themeIdentity,integritySignature;
        RollResult(List<Integer> rolls,List<Integer> physicalFaces,int total,int natural,String detail,String critical,String formula,String mode,int physicsMs,long physicsSeed,String themeIdentity,String integritySignature){
            this.rolls=rolls;this.physicalFaces=physicalFaces;this.total=total;this.natural=natural;this.detail=detail;this.critical=critical;this.formula=formula;this.mode=mode;this.physicsMs=physicsMs;this.physicsSeed=physicsSeed;this.themeIdentity=themeIdentity;this.integritySignature=integritySignature==null?"":integritySignature;
        }
    }

    private Dice3DOverlay(){}
    public static boolean enabled(SharedPreferences prefs){return prefs.getBoolean("dice3d_enabled_v1",true);}

    /**
     * Entrada pública da mesa 3D. Desde 6.3.3 o caminho ativo é 100% nativo:
     * sem WebView, sem canvas 2D e sem a antiga mesa elíptica.
     */
    public static void roll(final Activity activity,final SharedPreferences prefs,final String formula,final String requestedMode,final String actorName,final long actorCharacterId,final Callback callback){
        final DiceRollSpec spec;
        try{spec=DiceRollSpec.parse(formula,requestedMode);}catch(Exception e){if(callback!=null)callback.onError(e.getMessage());return;}
        if(callback==null)return;
        if(!spec.supportsPhysical3d()){callback.onError("A mesa 3D física suporta d4, d6, d8, d10, d12, d20 e d100, até 20 corpos por lançamento.");return;}
        NativeDice3DOverlay.roll(activity,prefs,spec,actorName,actorCharacterId,callback);
    }

    /** Compatibilidade para chamadas antigas sem id estável de personagem. */
    public static void roll(final Activity activity,final SharedPreferences prefs,final String formula,final String mode,final String actorName,final Callback callback){roll(activity,prefs,formula,mode,actorName,0,callback);}

    private static ArrayList<Integer> toIntList(JSONArray a){ArrayList<Integer> out=new ArrayList<Integer>();if(a!=null)for(int i=0;i<a.length();i++)out.add(a.optInt(i));return out;}

    private static String physicalDetail(DiceRollSpec spec,List<Integer> rolls,List<Integer> faces,DiceRollSpec.Result resolved){
        return spec==null||resolved==null?"":spec.physicalDetail(rolls,faces,resolved);
    }

    private static String themeIdentityForSpec(String actorName,long actorId,DiceRollSpec spec){
        StringBuilder out=new StringBuilder();if(spec!=null)for(Integer sides:spec.physicalThemeSides()){if(out.length()>0)out.append('+');out.append(themeIdentity(actorName,actorId,sides));}return out.length()==0?themeIdentity(actorName,actorId,0):out.toString();
    }

    private static String legacyThemeKey(String actor){String a=safeActor(actor);return "dice3d_theme_v1_"+(a.length()==0?"table":a);}
    private static String themeIdentity(String actorName,long actorId,int sides){String owner=actorId>0?"character_"+actorId:(safeActor(actorName).length()>0?"name_"+safeActor(actorName):"table");return owner+"_d"+Math.max(0,sides);}
    private static String themeKey(String actorName,long actorId,int sides){return "dice3d_theme_v2_"+themeIdentity(actorName,actorId,sides);}
    private static String safeActor(String actor){
        String raw=actor==null?"":java.text.Normalizer.normalize(actor.toLowerCase(Locale.US),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","");
        return raw.replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");
    }

    /** Herança: padrão -> mesa/dado -> personagem -> personagem/dado. */
    public static JSONObject loadTheme(Activity activity,SharedPreferences prefs,String actorName,long actorId,int sides){/*BR_JSON_WRAP*/try{
        JSONObject out=defaults();
        copy(parse(prefs.getString(legacyThemeKey(""),"{}")),out);
        copy(parse(prefs.getString(themeKey("",0,0),"{}")),out);
        if(sides>0)copy(parse(prefs.getString(themeKey("",0,sides),"{}")),out);
        if((actorName!=null&&actorName.trim().length()>0)||actorId>0){
            copy(parse(prefs.getString(legacyThemeKey(actorName),"{}")),out);
            copy(parse(prefs.getString(themeKey(actorName,actorId,0),"{}")),out);
            if(sides>0)copy(parse(prefs.getString(themeKey(actorName,actorId,sides),"{}")),out);
        }
        sanitizeTheme(out);
        try{out.put("themeIdentity",themeIdentity(actorName,actorId,sides));attachFaceImages(activity,out);}catch(Exception ignored){AppLog.ignored("Dice3DOverlay","Falha ao anexar imagem de face.",ignored);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public static JSONObject loadTheme(SharedPreferences prefs,String actor){return loadTheme(null,prefs,actor,0,0);}

    private static JSONObject defaults(){/*BR_JSON_WRAP*/try{try{return new JSONObject()
            .put("material","obsidiana").put("palette","âmbar").put("texture","liso")
            .put("font","serif").put("baseColor","").put("numberColor","#FFF6D8").put("borderColor","")
            .put("critGlyph","✦").put("fumbleGlyph","☠").put("faceLabels",new JSONObject()).put("faceImagePaths",new JSONObject())
            .put("shine",58).put("alpha",100).put("wear",18).put("edge","marcada").put("sound",true).put("effects",true);
    }catch(Exception e){return new JSONObject();}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject parse(String s){try{return new JSONObject(s==null?"{}":s);}catch(Exception e){return new JSONObject();}}
    private static void copy(JSONObject from,JSONObject to){/*BR_JSON_WRAP*/try{if(from==null)return;JSONArray names=from.names();if(names==null)return;for(int i=0;i<names.length();i++){String k=names.optString(i);try{to.put(k,from.opt(k));}catch(Exception ignored){AppLog.ignored("Dice3DOverlay","Falha ao copiar preferência visual de dado.",ignored);}}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static void sanitizeTheme(JSONObject t){/*BR_JSON_WRAP*/try{
        try{
            String material=t.optString("material","obsidiana");if(!MATERIALS.contains(material))material="obsidiana";t.put("material",material);
            String palette=t.optString("palette","âmbar");if(!PALETTES.contains(palette))palette="âmbar";t.put("palette",palette);
            String font=t.optString("font","serif");if(!FONTS.contains(font))font="serif";t.put("font",font);
            String edge=t.optString("edge","marcada");if(!EDGES.contains(edge))edge="marcada";t.put("edge",edge);
            String texture=t.optString("texture","liso");if(!TEXTURES.contains(texture))texture="liso";t.put("texture",texture);
            t.put("baseColor",safeHex(t.optString("baseColor",""),""));t.put("numberColor",safeHex(t.optString("numberColor","#FFF6D8"),"#FFF6D8"));t.put("borderColor",safeHex(t.optString("borderColor",""),""));
            t.put("shine",clamp(t.optInt("shine",58),0,100));t.put("alpha",clamp(t.optInt("alpha",100),25,100));t.put("wear",clamp(t.optInt("wear",18),0,100));
            t.put("critGlyph",limitText(t.optString("critGlyph","✦"),8));t.put("fumbleGlyph",limitText(t.optString("fumbleGlyph","☠"),8));
            JSONObject labels=t.optJSONObject("faceLabels");if(labels==null)labels=new JSONObject();JSONObject clean=new JSONObject();JSONArray names=labels.names();if(names!=null)for(int i=0;i<names.length();i++){String k=names.optString(i);if(k.matches("\\d{1,3}")){String v=limitText(labels.optString(k,""),12);if(v.length()>0)clean.put(k,v);}}t.put("faceLabels",clean);
        }catch(Exception ignored){AppLog.ignored("Dice3DOverlay","Falha ao normalizar tema de dados.",ignored);}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private static String safeHex(String raw,String fallback){String s=raw==null?"":raw.trim().toUpperCase(Locale.US);if(s.matches("#[0-9A-F]{6}"))return s;return fallback;}
    private static String limitText(String s,int max){if(s==null)return "";String v=s.trim();return v.length()<=max?v:v.substring(0,max);}

    private static void attachFaceImages(Activity activity,JSONObject theme)throws Exception{/*BR_JSON_WRAP*/try{
        theme.put("faceImages",new JSONObject());if(activity==null)return;
        JSONObject paths=theme.optJSONObject("faceImagePaths");if(paths==null)return;
        JSONObject images=new JSONObject();JSONArray names=paths.names();if(names==null){theme.put("faceImages",images);return;}
        File root=new File(activity.getFilesDir(),"dice3d_faces");String rootPath=root.getCanonicalPath()+File.separator;
        for(int i=0;i<names.length();i++){
            String face=names.optString(i),p=paths.optString(face,"");if(!face.matches("\\d{1,3}")||p.length()==0)continue;
            File f=new File(p);String canonical=f.getCanonicalPath();if(!canonical.startsWith(rootPath)||!f.isFile()||f.length()<=0||f.length()>MAX_FACE_IMAGE_BYTES)continue;
            InputStream in=new java.io.FileInputStream(f);ByteArrayOutputStream buf=new ByteArrayOutputStream((int)f.length());byte[] block=new byte[8192];int n;while((n=in.read(block))>0)buf.write(block,0,n);in.close();
            images.put(face,"data:image/png;base64,"+Base64.encodeToString(buf.toByteArray(),Base64.NO_WRAP));
        }
        theme.put("faceImages",images);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Importa uma imagem de face para armazenamento privado do app, reduzida e validada. */
    public static void saveFaceImage(Activity a,SharedPreferences prefs,String actorName,long actorId,int sides,int face,Uri uri)throws Exception{/*BR_JSON_WRAP*/try{
        if(a==null||uri==null)throw new IllegalArgumentException("Imagem não selecionada.");
        if(sides<=0||sides==100)throw new IllegalArgumentException("Escolha um dado físico d4, d6, d8, d10, d12 ou d20 antes da imagem de face.");
        if(face<1||face>sides)throw new IllegalArgumentException("Face fora do dado selecionado.");
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;InputStream probe=a.getContentResolver().openInputStream(uri);if(probe==null)throw new IllegalArgumentException("Não foi possível abrir a imagem.");BitmapFactory.decodeStream(probe,null,bounds);probe.close();
        if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>12000||bounds.outHeight>12000)throw new IllegalArgumentException("Imagem inválida ou grande demais.");
        int sample=1;while(bounds.outWidth/sample>1024||bounds.outHeight/sample>1024)sample*=2;
        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=sample;opts.inPreferredConfig=Bitmap.Config.ARGB_8888;InputStream in=a.getContentResolver().openInputStream(uri);if(in==null)throw new IllegalArgumentException("Não foi possível reabrir a imagem.");Bitmap decoded=BitmapFactory.decodeStream(in,null,opts);in.close();if(decoded==null)throw new IllegalArgumentException("Formato de imagem não suportado.");
        int w=decoded.getWidth(),h=decoded.getHeight();float scale=Math.min(1f,MAX_FACE_IMAGE_EDGE/(float)Math.max(w,h));Bitmap small=scale<.999f?Bitmap.createScaledBitmap(decoded,Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)),true):decoded;
        File dir=new File(a.getFilesDir(),"dice3d_faces");if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("Não foi possível criar o armazenamento do Ateliê de Dados.");
        String owner=java.util.UUID.nameUUIDFromBytes(themeIdentity(actorName,actorId,sides).getBytes("UTF-8")).toString().replace("-","");File target=new File(dir,"face_"+owner+"_d"+sides+"_"+face+".png");File tmp=new File(dir,target.getName()+".tmp");FileOutputStream out=new FileOutputStream(tmp);if(!small.compress(Bitmap.CompressFormat.PNG,100,out)){out.close();throw new IllegalStateException("Não foi possível converter a imagem.");}out.flush();out.close();if(tmp.length()>MAX_FACE_IMAGE_BYTES){tmp.delete();throw new IllegalArgumentException("A imagem continua grande demais após redução; use um símbolo simples.");}
        if(target.exists()&&!target.delete())AppLog.w("Dice3DOverlay","Não foi possível apagar a face antiga antes da troca.");if(!tmp.renameTo(target)){java.io.FileInputStream src=new java.io.FileInputStream(tmp);FileOutputStream dst=new FileOutputStream(target);byte[] b=new byte[8192];int n;while((n=src.read(b))>0)dst.write(b,0,n);src.close();dst.close();tmp.delete();}
        if(small!=decoded)small.recycle();decoded.recycle();
        String key=themeKey(actorName,actorId,sides);JSONObject layer=parse(prefs.getString(key,"{}"));JSONObject paths=layer.optJSONObject("faceImagePaths");if(paths==null)paths=new JSONObject();paths.put(String.valueOf(face),target.getAbsolutePath());layer.put("faceImagePaths",paths);sanitizeTheme(layer);prefs.edit().putString(key,layer.toString()).apply();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static void removeFaceImage(Activity a,SharedPreferences prefs,String actorName,long actorId,int sides,int face){/*BR_JSON_WRAP*/try{
        try{String key=themeKey(actorName,actorId,sides);JSONObject layer=parse(prefs.getString(key,"{}"));JSONObject paths=layer.optJSONObject("faceImagePaths");if(paths==null)return;String path=paths.optString(String.valueOf(face),"");paths.remove(String.valueOf(face));layer.put("faceImagePaths",paths);prefs.edit().putString(key,layer.toString()).apply();if(a!=null&&path.length()>0){File root=new File(a.getFilesDir(),"dice3d_faces"),f=new File(path);String rp=root.getCanonicalPath()+File.separator,fp=f.getCanonicalPath();if(fp.startsWith(rp)&&f.isFile())f.delete();}}catch(Exception ignored){AppLog.ignored("Dice3DOverlay","Falha ao remover imagem de face.",ignored);}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static boolean hasFaceImage(SharedPreferences prefs,String actorName,long actorId,int sides,int face){JSONObject layer=parse(prefs.getString(themeKey(actorName,actorId,sides),"{}"));JSONObject paths=layer.optJSONObject("faceImagePaths");return paths!=null&&paths.optString(String.valueOf(face),"").length()>0;}

    /** Ateliê por personagem e por tipo de dado. */
    public static void showAtelier(final Activity a,final SharedPreferences prefs,List<JSONObject> heroes,final FaceImagePicker picker,final Runnable afterSave){/*BR_JSON_WRAP*/try{
        final ScrollView scroll=new ScrollView(a);final LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);int p=dp(a,16);form.setPadding(p,p,p,p);scroll.addView(form);
        form.addView(label(a,"ATELIÊ DE DADOS",22,true));
        form.addView(label(a,"Cada personagem pode ter um conjunto próprio e cada dado pode sobrescrever o conjunto. A física permanece idêntica; apenas materiais, textura, leitura, som e efeitos visuais mudam.",13,false));

        final ArrayList<String> actorNames=new ArrayList<String>();final ArrayList<Long> actorIds=new ArrayList<Long>();actorNames.add("Mesa (padrão)");actorIds.add(0L);
        if(heroes!=null)for(JSONObject h:heroes){String n=h.optString("name","").trim();if(n.length()>0){actorNames.add(n);actorIds.add(h.optLong("id",0));}}
        final Spinner actor=new Spinner(a);actor.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,actorNames));form.addView(field(a,"CONJUNTO",actor));
        final String[] dieNames={"Todos os dados","d4","d6","d8","d10","d12","d20","d100"};final int[] dieSides={0,4,6,8,10,12,20,100};final Spinner die=spin(a,dieNames);form.addView(field(a,"DADO",die));

        final Spinner material=spin(a,new String[]{"obsidiana","pedra","metal","osso","madeira","cristal","vidro","marfim","couro"});
        final Spinner palette=spin(a,new String[]{"âmbar","marfim","rubi","esmeralda","safira","violeta","cinza","cobre"});
        final Spinner texture=spin(a,new String[]{"liso","granulado","veios","rachado","martelado","couro gravado"});
        final Spinner font=spin(a,new String[]{"serif","sans","mono","rúnica"});final Spinner edge=spin(a,new String[]{"suave","marcada","metálica"});
        form.addView(field(a,"MATERIAL",material));form.addView(field(a,"PALETA",palette));form.addView(field(a,"TEXTURA",texture));form.addView(field(a,"FONTE DOS NÚMEROS",font));form.addView(field(a,"BORDA",edge));
        final EditText baseColor=lineEdit(a,"#RRGGBB opcional"),numberColor=lineEdit(a,"#FFF6D8"),borderColor=lineEdit(a,"#RRGGBB opcional");
        form.addView(field(a,"COR DO DADO",baseColor));form.addView(field(a,"COR DOS NÚMEROS",numberColor));form.addView(field(a,"COR DA BORDA",borderColor));
        final EditText crit=lineEdit(a,"✦"),fumble=lineEdit(a,"☠"),labelOne=lineEdit(a,"opcional"),labelMax=lineEdit(a,"opcional");
        form.addView(field(a,"GLYPH DO 20 NATURAL",crit));form.addView(field(a,"GLYPH DO 1 NATURAL",fumble));
        final LinearLayout labelOneField=field(a,"RÓTULO ESPECIAL DA FACE 1",labelOne),labelMaxField=field(a,"RÓTULO ESPECIAL DA FACE MÁXIMA",labelMax);form.addView(labelOneField);form.addView(labelMaxField);
        final SeekBar shine=seek(a),alpha=seek(a),wear=seek(a);form.addView(field(a,"BRILHO",shine));form.addView(field(a,"OPACIDADE",alpha));form.addView(field(a,"DESGASTE",wear));
        final CheckBox sound=new CheckBox(a),effects=new CheckBox(a);sound.setText("Sons físicos de impacto");effects.setText("Efeito discreto em 20/1 natural");sound.setTextColor(Color.WHITE);effects.setTextColor(Color.WHITE);form.addView(sound);form.addView(effects);

        final Button imageOne=buttonPlain(a,"IMAGEM DA FACE 1"),imageMax=buttonPlain(a,"IMAGEM DA FACE MÁXIMA"),removeOne=buttonPlain(a,"REMOVER IMAGEM FACE 1"),removeMax=buttonPlain(a,"REMOVER IMAGEM FACE MÁXIMA");
        form.addView(imageOne);form.addView(imageMax);LinearLayout removes=new LinearLayout(a);removes.addView(removeOne,new LinearLayout.LayoutParams(0,-2,1));removes.addView(removeMax,new LinearLayout.LayoutParams(0,-2,1));form.addView(removes);
        form.addView(label(a,"Imagens são copiadas para o armazenamento privado do app, reduzidas para a mesa e nunca carregadas da internet. O número físico continua desenhado junto ao símbolo para não perder legibilidade.",11,false));

        final Runnable load=new Runnable(){public void run(){
            int ap=actor.getSelectedItemPosition(),dp=die.getSelectedItemPosition();String chosen=actorNames.get(ap);String who="Mesa (padrão)".equals(chosen)?"":chosen;long id=actorIds.get(ap);int sides=dieSides[dp];JSONObject t=loadTheme(a,prefs,who,id,sides);
            select(material,t.optString("material","obsidiana"));select(palette,t.optString("palette","âmbar"));select(texture,t.optString("texture","liso"));select(font,t.optString("font","serif"));select(edge,t.optString("edge","marcada"));baseColor.setText(t.optString("baseColor",""));numberColor.setText(t.optString("numberColor","#FFF6D8"));borderColor.setText(t.optString("borderColor",""));crit.setText(t.optString("critGlyph","✦"));fumble.setText(t.optString("fumbleGlyph","☠"));shine.setProgress(t.optInt("shine",58));alpha.setProgress(t.optInt("alpha",100));wear.setProgress(t.optInt("wear",18));sound.setChecked(t.optBoolean("sound",true));effects.setChecked(t.optBoolean("effects",true));
            JSONObject labels=t.optJSONObject("faceLabels");if(labels==null)labels=new JSONObject();int max=sides>0?sides:20;labelOne.setText(labels.optString("1",""));labelMax.setText(labels.optString(String.valueOf(max),""));boolean faceAllowed=sides>0&&sides!=100;labelOne.setEnabled(faceAllowed);labelMax.setEnabled(faceAllowed);imageOne.setEnabled(faceAllowed&&picker!=null);imageMax.setEnabled(faceAllowed&&picker!=null);removeOne.setEnabled(faceAllowed&&hasFaceImage(prefs,who,id,sides,1));removeMax.setEnabled(faceAllowed&&hasFaceImage(prefs,who,id,sides,max));imageMax.setText(faceAllowed?"IMAGEM DA FACE "+max:"IMAGEM DA FACE MÁXIMA");removeMax.setText(faceAllowed?"REMOVER IMAGEM FACE "+max:"REMOVER IMAGEM FACE MÁXIMA");
        }};
        android.widget.AdapterView.OnItemSelectedListener listener=new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){load.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}};actor.setOnItemSelectedListener(listener);die.setOnItemSelectedListener(listener);load.run();
        imageOne.setOnClickListener(new View.OnClickListener(){public void onClick(View v){int ap=actor.getSelectedItemPosition(),sides=dieSides[die.getSelectedItemPosition()];if(picker!=null&&sides>0&&sides!=100)picker.pick("Mesa (padrão)".equals(actorNames.get(ap))?"":actorNames.get(ap),actorIds.get(ap),sides,1);}});
        imageMax.setOnClickListener(new View.OnClickListener(){public void onClick(View v){int ap=actor.getSelectedItemPosition(),sides=dieSides[die.getSelectedItemPosition()];if(picker!=null&&sides>0&&sides!=100)picker.pick("Mesa (padrão)".equals(actorNames.get(ap))?"":actorNames.get(ap),actorIds.get(ap),sides,sides);}});
        removeOne.setOnClickListener(new View.OnClickListener(){public void onClick(View v){int ap=actor.getSelectedItemPosition(),sides=dieSides[die.getSelectedItemPosition()];removeFaceImage(a,prefs,"Mesa (padrão)".equals(actorNames.get(ap))?"":actorNames.get(ap),actorIds.get(ap),sides,1);load.run();}});
        removeMax.setOnClickListener(new View.OnClickListener(){public void onClick(View v){int ap=actor.getSelectedItemPosition(),sides=dieSides[die.getSelectedItemPosition()];removeFaceImage(a,prefs,"Mesa (padrão)".equals(actorNames.get(ap))?"":actorNames.get(ap),actorIds.get(ap),sides,sides);load.run();}});

        new AlertDialog.Builder(a).setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("SALVAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){/*BR_JSON_WRAP*/try{
            try{
                int ap=actor.getSelectedItemPosition(),sides=dieSides[die.getSelectedItemPosition()];String chosen=actorNames.get(ap),who="Mesa (padrão)".equals(chosen)?"":chosen;long id=actorIds.get(ap);
                JSONObject previous=parse(prefs.getString(themeKey(who,id,sides),"{}"));JSONObject paths=previous.optJSONObject("faceImagePaths");if(paths==null)paths=new JSONObject();
                JSONObject labels=new JSONObject();if(sides>0&&sides!=100){String one=limitText(labelOne.getText().toString(),12),max=limitText(labelMax.getText().toString(),12);if(one.length()>0)labels.put("1",one);if(max.length()>0)labels.put(String.valueOf(sides),max);}
                JSONObject t=new JSONObject().put("material",material.getSelectedItem()).put("palette",palette.getSelectedItem()).put("texture",texture.getSelectedItem()).put("font",font.getSelectedItem()).put("edge",edge.getSelectedItem()).put("baseColor",baseColor.getText().toString()).put("numberColor",numberColor.getText().toString()).put("borderColor",borderColor.getText().toString()).put("critGlyph",crit.getText().toString()).put("fumbleGlyph",fumble.getText().toString()).put("faceLabels",labels).put("faceImagePaths",paths).put("shine",shine.getProgress()).put("alpha",Math.max(25,alpha.getProgress())).put("wear",wear.getProgress()).put("sound",sound.isChecked()).put("effects",effects.isChecked());sanitizeTheme(t);prefs.edit().putString(themeKey(who,id,sides),t.toString()).apply();if(afterSave!=null)afterSave.run();
            }catch(Exception ignored){AppLog.ignored("Dice3DOverlay","Falha ao salvar conjunto do Ateliê de Dados.",ignored);}
        }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}}).show();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Compatibilidade com a assinatura antiga. */
    public static void showAtelier(final Activity a,final SharedPreferences prefs,List<JSONObject> heroes,final Runnable afterSave){showAtelier(a,prefs,heroes,null,afterSave);}

    private static TextView label(Activity a,String text,int sp,boolean bold){TextView v=new TextView(a);v.setText(text);v.setTextColor(Color.WHITE);v.setTextSize(sp);v.setPadding(0,dp(a,8),0,dp(a,8));if(bold)v.setTypeface(v.getTypeface(),1);return v;}
    private static LinearLayout field(Activity a,String name,View v){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(0,dp(a,6),0,dp(a,6));TextView n=label(a,name,12,true);n.setTextColor(Color.rgb(216,166,73));l.addView(n);l.addView(v,new LinearLayout.LayoutParams(-1,-2));return l;}
    private static Spinner spin(Activity a,String[] values){Spinner s=new Spinner(a);s.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,values));return s;}
    private static SeekBar seek(Activity a){SeekBar s=new SeekBar(a);s.setMax(100);return s;}
    private static EditText lineEdit(Activity a,String hint){EditText e=new EditText(a);e.setSingleLine(true);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);return e;}
    private static Button buttonPlain(Activity a,String text){Button b=new Button(a);b.setText(text);return b;}
    private static void select(Spinner s,String value){if(s.getAdapter()==null)return;for(int i=0;i<s.getAdapter().getCount();i++)if(String.valueOf(s.getAdapter().getItem(i)).equalsIgnoreCase(value)){s.setSelection(i);return;}}
    private static int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
}

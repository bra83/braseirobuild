package com.braseiro.pathfinder2e;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mesa física 3D nativa, portada da implementação homologada do Braseiro GURPS.
 * A física escolhe somente as faces; DiceRollSpec continua sendo a autoridade mecânica PF2e.
 */
public final class Dice3DOverlay {
    public interface Callback {void onResult(RollResult result);void onError(String message);void onCancel();}
    public static final class RollResult {
        public final List<Integer> rolls,physicalFaces;public final int total,natural;public final String detail,critical,formula,mode;
        RollResult(List<Integer> rolls,List<Integer> physicalFaces,int total,int natural,String detail,String critical,String formula,String mode){this.rolls=rolls;this.physicalFaces=physicalFaces;this.total=total;this.natural=natural;this.detail=detail;this.critical=critical;this.formula=formula;this.mode=mode;}
    }
    private Dice3DOverlay(){}
    public static boolean enabled(SharedPreferences prefs){return prefs.getBoolean("dice3d_enabled_v1",true);}

    public static void roll(final Activity activity,final SharedPreferences prefs,final String formula,final String mode,final String actor,final Callback callback){
        final DiceRollSpec spec;try{spec=DiceRollSpec.parse(formula,mode);}catch(Exception e){callback.onError(e.getMessage());return;}
        if(!spec.supportsPhysical3d()){callback.onError("A mesa 3D física suporta d4, d6, d8, d10, d12, d20 e d100, até 20 corpos por lançamento.");return;}
        final String physicalFormula=spec.sides==100?(spec.count+"d100"):(spec.isFortuneRoll()?"2d20":(spec.count+"d"+spec.sides));
        final long seed=new java.security.SecureRandom().nextLong();
        final PhysicalDiceEngine.Session session;try{session=PhysicalDiceEngine.create(physicalFormula,seed);}catch(Exception e){callback.onError(e.getMessage());return;}
        final Dialog dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final FrameLayout root=new FrameLayout(activity);root.setBackgroundColor(Color.rgb(17,13,11));
        final Dice3DView diceView=new Dice3DView(activity);diceView.setSession(session);root.addView(diceView,new FrameLayout.LayoutParams(-1,-1));
        final JSONObject theme=loadTheme(prefs,actor);final DiceAppearance appearance=appearanceFromTheme(theme);
        for(PhysicalDiceEngine.DieBody body:session.bodies)diceView.setAppearance(body.id,appearance.copy());
        LinearLayout top=new LinearLayout(activity);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(activity,18),dp(activity,20),dp(activity,18),dp(activity,10));top.setBackgroundColor(Color.argb(128,0,0,0));
        top.addView(label(activity,"ROLAGEM 3D • PATHFINDER 2E",18,true));TextView formulaView=label(activity,spec.formula+" • física nativa 120 Hz • resultado pela face superior",12,false);formulaView.setTextColor(Color.LTGRAY);top.addView(formulaView);root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        final LinearLayout bottom=new LinearLayout(activity);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(dp(activity,18),dp(activity,12),dp(activity,18),dp(activity,22));bottom.setBackgroundColor(Color.argb(175,0,0,0));
        final TextView resultView=label(activity,"Dados em movimento…",20,true);resultView.setTextColor(Color.WHITE);resultView.setGravity(Gravity.CENTER);bottom.addView(resultView);
        final TextView detailView=label(activity,"Impulso • rotação • gravidade • colisões",12,false);detailView.setTextColor(Color.LTGRAY);detailView.setGravity(Gravity.CENTER);bottom.addView(detailView);
        final Button apply=new Button(activity);apply.setText("APLICAR RESULTADO");apply.setVisibility(View.GONE);bottom.addView(apply,new LinearLayout.LayoutParams(-1,dp(activity,50)));
        final Button cancel=new Button(activity);cancel.setText("CANCELAR ROLAGEM");bottom.addView(cancel,new LinearLayout.LayoutParams(-1,dp(activity,50)));root.addView(bottom,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        dialog.setContentView(root);Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));w.addFlags(android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED|android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);android.view.WindowManager.LayoutParams lp=w.getAttributes();lp.dimAmount=.20f;w.setAttributes(lp);}dialog.setCancelable(false);
        final boolean[] done={false};final long[] lastNs={System.nanoTime()};final double[] acc={0};final long[] lastImpactMs=new long[session.bodies.size()+2];final DiceSoundEngine sound=sharedSound(activity);
        final Runnable ticker=new Runnable(){public void run(){if(done[0]||!dialog.isShowing())return;long now=System.nanoTime();double elapsed=Math.min(.055,(now-lastNs[0])/1e9);lastNs[0]=now;acc[0]+=elapsed;int sub=0;while(acc[0]>=PhysicalDiceEngine.FIXED_DT&&sub<8&&!session.isComplete()){session.step();acc[0]-=PhysicalDiceEngine.FIXED_DT;sub++;}
            long ms=System.currentTimeMillis();for(PhysicalDiceEngine.DieBody body:session.bodies){int idx=Math.min(lastImpactMs.length-1,body.id);if(body.impact>.42&&ms-lastImpactMs[idx]>55){if(theme.optBoolean("sound",true))sound.play(appearance,body.impact,body.sides);lastImpactMs[idx]=ms;}}
            diceView.invalidate();if(session.isComplete()){done[0]=true;try{final PhysicalDiceEngine.RollResult raw=session.result();final ArrayList<Integer> rolls=new ArrayList<Integer>(raw.dice);final ArrayList<Integer> physicalFaces=new ArrayList<Integer>();if(spec.sides==100){for(PhysicalDiceEngine.DieBody body:session.bodies){int v=body.rawTopValue();physicalFaces.add(v==10?0:v);}}else physicalFaces.addAll(rolls);final DiceRollSpec.Result resolved=spec.resolve(rolls);final String resolvedDetail=physicalDetail(spec,rolls,physicalFaces,resolved);diceView.triggerOutcome(resolved.critical);resultView.setText(resolvedDetail);detailView.setText(resolved.critical+"\nQualidade "+diceView.visualQualityLabel()+" • física 120 Hz • seed "+Long.toUnsignedString(raw.seed));apply.setVisibility(View.VISIBLE);cancel.setText("DESCARTAR");apply.setOnClickListener(new View.OnClickListener(){public void onClick(View v){dialog.dismiss();callback.onResult(new RollResult(rolls,physicalFaces,resolved.total,resolved.natural,resolvedDetail,resolved.critical,spec.formula,spec.mode));}});}catch(Exception e){dialog.dismiss();callback.onError("Resultado físico inválido: "+e.getMessage());}return;}diceView.postOnAnimation(this);}};
        cancel.setOnClickListener(new View.OnClickListener(){public void onClick(View v){done[0]=true;dialog.dismiss();callback.onCancel();}});dialog.setOnShowListener(new DialogInterface.OnShowListener(){public void onShow(DialogInterface d){Window ww=dialog.getWindow();if(ww!=null)ww.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);diceView.postOnAnimation(ticker);}});dialog.show();
    }

    private static DiceSoundEngine sharedSoundEngine;
    private static synchronized DiceSoundEngine sharedSound(Activity activity){if(sharedSoundEngine==null)sharedSoundEngine=new DiceSoundEngine(activity.getApplicationContext());return sharedSoundEngine;}

    private static DiceAppearance appearanceFromTheme(JSONObject theme){
        String material=theme==null?"obsidian":theme.optString("material","obsidian");String n=material.toLowerCase(Locale.US);if(n.contains("obsid"))material="obsidian";else if(n.contains("metal"))material="metal";else if(n.contains("pedra")||n.contains("stone"))material="stone";else if(n.contains("osso")||n.contains("bone"))material="bone";else if(n.contains("madeira")||n.contains("wood"))material="wood";else if(n.contains("crist"))material="crystal";else if(n.contains("vidro"))material="glass";else if(n.contains("marfim"))material="ivory";else if(n.contains("couro"))material="leather";else if(n.contains("plast")||n.contains("resin"))material="plastic";
        DiceAppearance d=DiceAppearance.preset(material);if(theme==null)return d;d.font=theme.optString("font",d.font);d.shine=Math.max(0f,Math.min(1f,(float)theme.optDouble("shine",Math.round(d.shine*100))/100f));d.transparency=Math.max(0f,Math.min(.82f,1f-(float)theme.optDouble("alpha",100)/100f));d.wear=Math.max(0f,Math.min(1f,(float)theme.optDouble("wear",Math.round(d.wear*100))/100f));d.maxFaceGlyph=theme.optString("critGlyph",d.maxFaceGlyph);d.faceOneGlyph=theme.optString("fumbleGlyph",d.faceOneGlyph);return d;
    }

    private static String physicalDetail(DiceRollSpec spec,List<Integer> rolls,List<Integer> faces,DiceRollSpec.Result resolved){
        if(spec==null||resolved==null)return "";
        StringBuilder out=new StringBuilder();
        if(spec.sides==100&&faces!=null&&faces.size()==spec.count*2){
            for(int i=0;i<faces.size();i+=2){
                int tens=Math.max(0,Math.min(9,faces.get(i)))*10;
                int ones=Math.max(0,Math.min(9,faces.get(i+1)));
                int value=tens+ones;if(value==0)value=100;
                if(out.length()>0)out.append(" · ");
                out.append(String.format(Locale.US,"%02d",tens)).append(" + ").append(ones).append(" → ").append(value);
            }
        }else if(spec.isFortuneRoll()&&rolls!=null&&rolls.size()==2){
            out.append('[').append(rolls.get(0)).append(" · ").append(rolls.get(1)).append("] → ").append(resolved.natural);
        }else if(rolls!=null&&!rolls.isEmpty()){
            for(int i=0;i<rolls.size();i++){if(i>0)out.append(" · ");out.append(rolls.get(i));}
        }else out.append(resolved.detail==null?"":resolved.detail);
        if(spec.modifier!=0)out.append(spec.modifier>0?" + ":" - ").append(Math.abs(spec.modifier));
        out.append(" = ").append(resolved.total);
        return out.toString();
    }

    private static String themeKey(String actor){String a=actor==null?"":actor.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]+","_");return "dice3d_theme_v1_"+(a.length()==0?"table":a);}
    public static JSONObject loadTheme(SharedPreferences prefs,String actor){JSONObject global=parse(prefs.getString(themeKey(""),"{}"));JSONObject actorTheme=parse(prefs.getString(themeKey(actor),"{}"));JSONObject out=defaults();copy(global,out);if(actor!=null&&actor.trim().length()>0)copy(actorTheme,out);return out;}
    private static JSONObject defaults(){try{return new JSONObject().put("material","obsidiana").put("palette","âmbar").put("font","serif").put("critGlyph","✦").put("fumbleGlyph","☠").put("shine",58).put("alpha",100).put("wear",18).put("edge","marcada").put("sound",true).put("effects",true);}catch(Exception e){return new JSONObject();}}
    private static JSONObject parse(String s){try{return new JSONObject(s==null?"{}":s);}catch(Exception e){return new JSONObject();}}
    private static void copy(JSONObject from,JSONObject to){if(from==null)return;JSONArray names=from.names();if(names==null)return;for(int i=0;i<names.length();i++){String k=names.optString(i);try{to.put(k,from.opt(k));}catch(Exception ignored){AppLog.ignored("Dice3DOverlay","Falha ao copiar preferência visual de dado.",ignored);}}}

    /** Ateliê local; não toca no banco de campanha e pode ser reaproveitado pelo Motor Barbara. */
    public static void showAtelier(final Activity a,final SharedPreferences prefs,List<JSONObject> heroes,final Runnable afterSave){
        final ScrollView scroll=new ScrollView(a);final LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);int p=dp(a,16);form.setPadding(p,p,p,p);scroll.addView(form);
        form.addView(label(a,"ATELIÊ DE DADOS",22,true));form.addView(label(a,"A física é a mesma em qualquer qualidade. Aparência, sombras e efeitos podem reduzir automaticamente quando o aparelho estiver sob carga.",13,false));
        ArrayList<String> actors=new ArrayList<String>();actors.add("Mesa (padrão)");if(heroes!=null)for(JSONObject h:heroes){String n=h.optString("name","").trim();if(n.length()>0)actors.add(n);}final Spinner actor=new Spinner(a);actor.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,actors));form.addView(field(a,"CONJUNTO",actor));
        final Spinner material=spin(a,new String[]{"obsidiana","pedra","metal","osso","madeira","cristal","vidro","marfim","couro"});final Spinner palette=spin(a,new String[]{"âmbar","marfim","rubi","esmeralda","safira","violeta","cinza","cobre"});final Spinner font=spin(a,new String[]{"serif","sans","mono","rúnica"});final Spinner edge=spin(a,new String[]{"suave","marcada","metálica"});form.addView(field(a,"MATERIAL",material));form.addView(field(a,"PALETA",palette));form.addView(field(a,"FONTE DOS NÚMEROS",font));form.addView(field(a,"BORDA",edge));
        final EditText crit=new EditText(a),fumble=new EditText(a);crit.setSingleLine(true);fumble.setSingleLine(true);form.addView(field(a,"GLYPH DO 20",crit));form.addView(field(a,"GLYPH DO 1",fumble));final SeekBar shine=seek(a),alpha=seek(a),wear=seek(a);form.addView(field(a,"BRILHO",shine));form.addView(field(a,"OPACIDADE",alpha));form.addView(field(a,"DESGASTE",wear));final CheckBox sound=new CheckBox(a),effects=new CheckBox(a);sound.setText("Sons físicos de impacto");effects.setText("Efeito discreto em 20/1 natural");sound.setTextColor(Color.WHITE);effects.setTextColor(Color.WHITE);form.addView(sound);form.addView(effects);
        final Runnable load=new Runnable(){public void run(){String chosen=String.valueOf(actor.getSelectedItem());String who="Mesa (padrão)".equals(chosen)?"":chosen;JSONObject t=loadTheme(prefs,who);select(material,t.optString("material","obsidiana"));select(palette,t.optString("palette","âmbar"));select(font,t.optString("font","serif"));select(edge,t.optString("edge","marcada"));crit.setText(t.optString("critGlyph","✦"));fumble.setText(t.optString("fumbleGlyph","☠"));shine.setProgress(t.optInt("shine",58));alpha.setProgress(t.optInt("alpha",100));wear.setProgress(t.optInt("wear",18));sound.setChecked(t.optBoolean("sound",true));effects.setChecked(t.optBoolean("effects",true));}};actor.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){load.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});load.run();
        new AlertDialog.Builder(a).setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("SALVAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{String chosen=String.valueOf(actor.getSelectedItem());String who="Mesa (padrão)".equals(chosen)?"":chosen;JSONObject t=new JSONObject().put("material",material.getSelectedItem()).put("palette",palette.getSelectedItem()).put("font",font.getSelectedItem()).put("edge",edge.getSelectedItem()).put("critGlyph",crit.getText().toString()).put("fumbleGlyph",fumble.getText().toString()).put("shine",shine.getProgress()).put("alpha",Math.max(25,alpha.getProgress())).put("wear",wear.getProgress()).put("sound",sound.isChecked()).put("effects",effects.isChecked());prefs.edit().putString(themeKey(who),t.toString()).apply();if(afterSave!=null)afterSave.run();}catch(Exception ignored){AppLog.ignored("Dice3DOverlay","Falha ao salvar conjunto do Ateliê de Dados.",ignored);}}}).show();
    }
    private static TextView label(Activity a,String text,int sp,boolean bold){TextView v=new TextView(a);v.setText(text);v.setTextColor(Color.WHITE);v.setTextSize(sp);v.setPadding(0,dp(a,8),0,dp(a,8));if(bold)v.setTypeface(v.getTypeface(),1);return v;}
    private static LinearLayout field(Activity a,String name,View v){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(0,dp(a,6),0,dp(a,6));TextView n=label(a,name,12,true);n.setTextColor(Color.rgb(216,166,73));l.addView(n);l.addView(v,new LinearLayout.LayoutParams(-1,-2));return l;}
    private static Spinner spin(Activity a,String[] values){Spinner s=new Spinner(a);s.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,values));return s;}
    private static SeekBar seek(Activity a){SeekBar s=new SeekBar(a);s.setMax(100);return s;}
    private static void select(Spinner s,String value){if(s.getAdapter()==null)return;for(int i=0;i<s.getAdapter().getCount();i++)if(String.valueOf(s.getAdapter().getItem(i)).equalsIgnoreCase(value)){s.setSelection(i);return;}}
    private static int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
}

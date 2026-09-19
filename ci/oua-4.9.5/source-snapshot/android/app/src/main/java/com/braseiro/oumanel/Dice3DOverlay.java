package com.braseiro.oumanel;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.SecureRandom;
import java.util.HashMap;

/** Overlay de rolagem 3D. A física produz o resultado; o sistema de RPG o interpreta depois. */
public final class Dice3DOverlay {
    public interface ResultCallback { void onResult(JSONObject result); void onCancel(); }
    public interface OutcomeResolver { String resolve(PhysicalDiceEngine.RollResult raw); }
    private final Context context; private final DiceAppearanceStore store; private final Handler ui=new Handler(Looper.getMainLooper());
    public Dice3DOverlay(Context c,DiceAppearanceStore store){context=c;this.store=store==null?new DiceAppearanceStore(c):store;}

    public void roll(String title,String formula,long actorId,String actorName,OutcomeResolver resolver,ResultCallback callback){
        final PhysicalDiceEngine.Formula parsed;try{parsed=PhysicalDiceEngine.parseFormula(formula);}catch(Exception e){throw new IllegalArgumentException(e.getMessage());}
        final long seed=new SecureRandom().nextLong();final PhysicalDiceEngine.Session session=PhysicalDiceEngine.create(parsed.normalized(),seed);
        final Dialog dialog=new Dialog(context);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);FrameLayout root=new FrameLayout(context);root.setBackgroundColor(Color.TRANSPARENT);
        final Dice3DView diceView=new Dice3DView(context);diceView.setSession(session);root.addView(diceView,new FrameLayout.LayoutParams(-1,-1));
        final HashMap<Integer,DiceAppearance> styles=new HashMap<Integer,DiceAppearance>();for(PhysicalDiceEngine.DieBody body:session.bodies){int appearanceSides=parsed.sides==100?100:body.sides;DiceAppearance a=store.resolve(actorId,appearanceSides);styles.put(body.id,a);diceView.setAppearance(body.id,a);}
        LinearLayout top=new LinearLayout(context);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(18),dp(20),dp(18),dp(10));top.setBackgroundColor(Color.argb(120,0,0,0));TextView heading=txt(title==null?"ROLAGEM 3D":title,18,Color.rgb(232,192,90),true);TextView formulaView=txt(parsed.normalized()+" • física real • resultado pela face superior",13,Color.LTGRAY,false);top.addView(heading);top.addView(formulaView);FrameLayout.LayoutParams topLp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);root.addView(top,topLp);
        final LinearLayout bottom=new LinearLayout(context);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(dp(18),dp(12),dp(18),dp(22));bottom.setBackgroundColor(Color.argb(155,0,0,0));final TextView resultView=txt("Dados em movimento…",20,Color.WHITE,true);resultView.setGravity(Gravity.CENTER);final TextView detailView=txt("Impulso • rotação • gravidade • colisões",12,Color.LTGRAY,false);detailView.setGravity(Gravity.CENTER);bottom.addView(resultView);bottom.addView(detailView);final Button apply=button("APLICAR RESULTADO");apply.setVisibility(View.GONE);bottom.addView(apply);final Button cancel=button("CANCELAR ROLAGEM");bottom.addView(cancel);FrameLayout.LayoutParams botLp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);root.addView(bottom,botLp);
        dialog.setContentView(root);Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);w.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=w.getAttributes();lp.dimAmount=.18f;w.setAttributes(lp);}dialog.setCancelable(false);
        final boolean[] done={false};final long[] lastNs={System.nanoTime()};final double[] acc={0};final long[] lastImpactMs=new long[session.bodies.size()+2];
        final Runnable ticker=new Runnable(){public void run(){if(done[0]||!dialog.isShowing())return;long now=System.nanoTime();double elapsed=Math.min(.055,(now-lastNs[0])/1e9);lastNs[0]=now;acc[0]+=elapsed;int sub=0;while(acc[0]>=PhysicalDiceEngine.FIXED_DT&&sub<8&&!session.isComplete()){session.step();acc[0]-=PhysicalDiceEngine.FIXED_DT;sub++;}
                long ms=System.currentTimeMillis();for(PhysicalDiceEngine.DieBody body:session.bodies){if(body.impact>.42&&ms-lastImpactMs[Math.min(lastImpactMs.length-1,body.id)]>55){DiceAppearance a=styles.get(body.id);new DiceSoundEngineProxy(context).play(a,body.impact,body.sides);lastImpactMs[Math.min(lastImpactMs.length-1,body.id)]=ms;}}
                diceView.invalidate();if(session.isComplete()){done[0]=true;final PhysicalDiceEngine.RollResult raw=session.result();String outcome=resolver==null?"":safe(resolver.resolve(raw));diceView.triggerOutcome(outcome);resultView.setText(PhysicalDiceEngine.valuesText(raw));detailView.setText(individualText(raw)+(outcome.length()>0?"\n"+outcome:"")+"\nQualidade visual "+diceView.visualQualityLabel()+" • física 120 Hz • seed "+Long.toUnsignedString(raw.seed));final JSONObject json=toJson(raw,actorId,actorName,outcome,styles);apply.setVisibility(View.VISIBLE);cancel.setText("DESCARTAR");apply.setOnClickListener(new View.OnClickListener(){public void onClick(View v){dialog.dismiss();if(callback!=null)callback.onResult(json);}});return;}diceView.postOnAnimation(this);}};
        cancel.setOnClickListener(new View.OnClickListener(){public void onClick(View v){done[0]=true;dialog.dismiss();if(callback!=null)callback.onCancel();}});dialog.setOnShowListener(x->{Window ww=dialog.getWindow();if(ww!=null)ww.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);diceView.postOnAnimation(ticker);});dialog.show();
    }
    private JSONObject toJson(PhysicalDiceEngine.RollResult r,long actorId,String actorName,String outcome,HashMap<Integer,DiceAppearance> styles){JSONObject o=new JSONObject();try{JSONArray a=new JSONArray();for(int v:r.dice)a.put(v);JSONObject appearance=new JSONObject();for(Integer id:styles.keySet())appearance.put(String.valueOf(id),styles.get(id).toJson());o.put("engine",PhysicalDiceEngine.ENGINE_ID).put("mode","physical_3d").put("formula",r.formula.normalized()).put("dice",a).put("subtotal",r.subtotal).put("modifier",r.formula.modifier).put("total",r.total).put("physics_seed",Long.toUnsignedString(r.seed)).put("physics_steps",r.physicsSteps).put("forced_settle",r.forcedSettle).put("actor_character_id",actorId).put("actor_name",actorName==null?"":actorName).put("outcome_hint",outcome).put("appearance",appearance).put("user_triggered",true);}catch(Exception error){AppLog.e("Dados3D","Falha ao montar envelope autoritativo da rolagem 3D.",error);}return o;}
    private String individualText(PhysicalDiceEngine.RollResult r){StringBuilder b=new StringBuilder("Faces: ");for(int i=0;i<r.dice.size();i++){if(i>0)b.append(" · ");b.append("d").append(i+1).append("=").append(r.dice.get(i));}if(r.formula.modifier!=0)b.append(" • modificador ").append(r.formula.modifier>0?"+":"").append(r.formula.modifier);return b.toString();}
    private TextView txt(String s,int sp,int color,boolean bold){TextView t=new TextView(context);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);t.setPadding(0,dp(4),0,dp(4));return t;}
    private Button button(String s){Button b=new Button(context);b.setText(s);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.rgb(54,48,42));b.setMinHeight(dp(48));return b;}
    private int dp(int x){return (int)(x*context.getResources().getDisplayMetrics().density+.5f);}private static String safe(String s){return s==null?"":s;}

    /** Mantém um SoundPool compartilhado por processo e evita recriá-lo por impacto. */
    private static final class DiceSoundEngineProxy {private static DiceSoundEngine shared;DiceSoundEngineProxy(Context c){if(shared==null)shared=new DiceSoundEngine(c.getApplicationContext());}void play(DiceAppearance a,double impulse,int sides){shared.play(a,impulse,sides);}}
}

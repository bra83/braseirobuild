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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Mesa física mista de O Um Anel: Dado(s) de Proeza d12 e Dados de Sucesso d6
 * coexistem na mesma sessão, recebem impulso/torque/gravidade e colidem entre si.
 * A física determina as faces; só depois o host interpreta as regras.
 */
public final class OneRingDice3DOverlay {
    public interface ResultCallback { void onResult(JSONObject result); void onCancel(); void onError(Throwable error); }
    private final Context context;
    private final DiceAppearanceStore store;
    public OneRingDice3DOverlay(Context context, DiceAppearanceStore store){this.context=context;this.store=store==null?new DiceAppearanceStore(context):store;}

    public void roll(int successDiceCount,String rollMode,long actorId,String actorName,ResultCallback callback){
        if(successDiceCount<0||successDiceCount>18)throw new IllegalArgumentException("Use de 0 a 18 Dados de Sucesso nesta mesa física.");
        final String mode=normalizeMode(rollMode);final int featCount="normal".equals(mode)?1:2;final int total=featCount+successDiceCount;
        if(total>20)throw new IllegalArgumentException("A mesa física suporta até 20 dados simultâneos.");
        final int[] sides=new int[total];for(int i=0;i<featCount;i++)sides[i]=12;for(int i=featCount;i<total;i++)sides[i]=6;
        final long seed=new SecureRandom().nextLong();final PhysicalDiceEngine.Session session=PhysicalDiceEngine.createMixed(sides,seed);
        final Dialog dialog=new Dialog(context);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);final FrameLayout root=new FrameLayout(context);root.setBackgroundColor(Color.TRANSPARENT);
        final Dice3DView diceView=new Dice3DView(context);diceView.setSession(session);root.addView(diceView,new FrameLayout.LayoutParams(-1,-1));
        final HashMap<Integer,DiceAppearance> styles=new HashMap<Integer,DiceAppearance>();
        for(PhysicalDiceEngine.DieBody body:session.bodies){DiceAppearance a=store.resolve(actorId,body.sides);styles.put(body.id,a);diceView.setAppearance(body.id,a);if(body.logicalIndex<featCount){diceView.setFaceLabelOverride(body.id,11,"◉");diceView.setFaceLabelOverride(body.id,12,"ᚷ");}else{diceView.setFaceLabelOverride(body.id,6,"ᛏ");}}

        LinearLayout top=new LinearLayout(context);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(18),dp(20),dp(18),dp(10));top.setBackgroundColor(Color.argb(125,0,0,0));
        top.addView(txt("O UM ANEL • DADOS FÍSICOS",18,Color.rgb(232,192,90),true));
        top.addView(txt(featCount+" Dado(s) de Proeza d12 + "+successDiceCount+" Dados de Sucesso d6 • todos colidem na mesma mesa",13,Color.LTGRAY,false));
        root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));

        LinearLayout bottom=new LinearLayout(context);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(dp(18),dp(12),dp(18),dp(22));bottom.setBackgroundColor(Color.argb(160,0,0,0));
        final TextView resultView=txt("Dados em movimento…",20,Color.WHITE,true);resultView.setGravity(Gravity.CENTER);
        final TextView detailView=txt("◉ Olho de Sauron • ᚷ Gandalf • ᛏ Tengwar • face superior autoritativa",12,Color.LTGRAY,false);detailView.setGravity(Gravity.CENTER);
        final Button apply=button("APLICAR RESULTADO");apply.setVisibility(View.GONE);final Button cancel=button("CANCELAR ROLAGEM");
        bottom.addView(resultView);bottom.addView(detailView);bottom.addView(apply);bottom.addView(cancel);root.addView(bottom,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));

        dialog.setContentView(root);Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);w.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=w.getAttributes();lp.dimAmount=.18f;w.setAttributes(lp);}dialog.setCancelable(false);
        final boolean[] done={false};final long[] lastNs={System.nanoTime()};final double[] acc={0};final long[] lastImpactMs=new long[session.bodies.size()+2];final JSONObject[] envelope={null};
        final Runnable ticker=new Runnable(){public void run(){if(done[0]||!dialog.isShowing())return;try{long now=System.nanoTime();double elapsed=Math.min(.055,(now-lastNs[0])/1e9);lastNs[0]=now;acc[0]+=elapsed;int sub=0;while(acc[0]>=PhysicalDiceEngine.FIXED_DT&&sub<8&&!session.isComplete()){session.step();acc[0]-=PhysicalDiceEngine.FIXED_DT;sub++;}
            long ms=System.currentTimeMillis();for(PhysicalDiceEngine.DieBody body:session.bodies){if(body.impact>.42&&ms-lastImpactMs[Math.min(lastImpactMs.length-1,body.id)]>55){DiceAppearance a=styles.get(body.id);SoundHolder.get(context).play(a,body.impact,body.sides);lastImpactMs[Math.min(lastImpactMs.length-1,body.id)]=ms;}}
            diceView.invalidate();if(session.isComplete()){done[0]=true;PhysicalDiceEngine.RollResult raw=session.result();List<Integer> feat=new ArrayList<Integer>(),success=new ArrayList<Integer>();for(int i=0;i<raw.dice.size();i++){if(i<featCount)feat.add(raw.dice.get(i));else success.add(raw.dice.get(i));}envelope[0]=envelope(feat,success,mode,raw,actorId,actorName);resultView.setText(formatFeat(feat)+" • Sucesso "+success.toString());detailView.setText("Física 120 Hz • colisões mistas • seed "+Long.toUnsignedString(raw.seed)+" • "+raw.physicsSteps+" passos");apply.setVisibility(View.VISIBLE);cancel.setText("DESCARTAR");return;}diceView.postOnAnimation(this);}catch(Throwable error){done[0]=true;dialog.dismiss();if(callback!=null)callback.onError(error);}}};
        apply.setOnClickListener(v->{if(envelope[0]==null)return;dialog.dismiss();if(callback!=null)callback.onResult(envelope[0]);});cancel.setOnClickListener(v->{done[0]=true;dialog.dismiss();if(callback!=null)callback.onCancel();});dialog.setOnShowListener(x->{Window ww=dialog.getWindow();if(ww!=null)ww.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);diceView.postOnAnimation(ticker);});dialog.show();
    }

    private JSONObject envelope(List<Integer> feat,List<Integer> success,String mode,PhysicalDiceEngine.RollResult raw,long actorId,String actorName){JSONObject o=new JSONObject();try{JSONArray f=new JSONArray();for(int v:feat)f.put(v);JSONArray s=new JSONArray();for(int v:success)s.put(v);String seed=Long.toUnsignedString(raw.seed);o.put("engine",PhysicalDiceEngine.ENGINE_ID).put("mode","physical_3d").put("system_id","the_one_ring").put("roll_mode",mode).put("feat_results",f).put("success_results",s).put("physics_seed",seed).put("physics_seed_feat",seed).put("physics_seed_success",seed).put("physics_steps",raw.physicsSteps).put("physics_steps_feat",raw.physicsSteps).put("physics_steps_success",raw.physicsSteps).put("forced_settle",raw.forcedSettle).put("forced_settle_feat",raw.forcedSettle).put("forced_settle_success",raw.forcedSettle).put("mixed_collisions",true).put("actor_character_id",actorId).put("actor_name",actorName==null?"":actorName).put("user_triggered",true);}catch(Exception e){AppLog.e("Dados3D","Falha ao montar envelope físico de O Um Anel.",e);}return o;}
    private String formatFeat(List<Integer> values){StringBuilder b=new StringBuilder("Proeza ");for(int i=0;i<values.size();i++){if(i>0)b.append(" / ");int v=values.get(i);b.append(v==11?"◉ Olho":v==12?"ᚷ Gandalf":String.valueOf(v));}return b.toString();}
    private String normalizeMode(String raw){String s=raw==null?"normal":raw.trim().toLowerCase();return "favoured".equals(s)||"ill-favoured".equals(s)?s:"normal";}
    private TextView txt(String s,int sp,int color,boolean bold){TextView t=new TextView(context);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);t.setPadding(0,dp(4),0,dp(4));return t;}
    private Button button(String s){Button b=new Button(context);b.setText(s);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.rgb(54,48,42));b.setMinHeight(dp(48));return b;}
    private int dp(int x){return (int)(x*context.getResources().getDisplayMetrics().density+.5f);}
    private static final class SoundHolder{private static DiceSoundEngine shared;static synchronized DiceSoundEngine get(Context c){if(shared==null)shared=new DiceSoundEngine(c.getApplicationContext());return shared;}}
}

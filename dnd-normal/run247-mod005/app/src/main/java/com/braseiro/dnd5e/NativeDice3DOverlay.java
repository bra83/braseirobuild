package com.braseiro.dnd5e;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
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

import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Overlay nativo da mesa física do D&D.
 *
 * Contrato de autoridade:
 *  - SecureRandom escolhe somente a seed/condições iniciais;
 *  - PhysicalDiceEngine integra corpos rígidos a 120 Hz;
 *  - a orientação final produz physicalFaces;
 *  - DiceRollSpec reconstrói os dados lógicos exclusivamente dessas faces;
 *  - Java recalcula o total e a assinatura antes de publicar o resultado.
 *
 * Qualidade adaptativa existe apenas no Dice3DView. Nunca altera FIXED_DT,
 * colisões, repouso, face superior, total ou seed.
 */
final class NativeDice3DOverlay {
    private static final long WATCHDOG_MS=27000L;
    private static final long RESULT_HOLD_MS=950L;
    private static DiceSoundEngine sharedSound;

    private NativeDice3DOverlay(){}

    static void roll(final Activity activity,final SharedPreferences prefs,final DiceRollSpec spec,
                     final String actorName,final long actorCharacterId,final Dice3DOverlay.Callback callback){/*BR_JSON_WRAP*/try{
        if(callback==null)return;
        if(activity==null||activity.isFinishing()||(Build.VERSION.SDK_INT>=17&&activity.isDestroyed())){
            callback.onError("A tela foi encerrada antes da rolagem 3D.");return;
        }
        if(spec==null||!spec.supportsPhysical3d()){
            callback.onError("A mesa 3D física suporta d4, d6, d8, d10, d12, d20 e d100, até 20 corpos por lançamento.");return;
        }

        final long seed=new SecureRandom().nextLong();
        final PhysicalDiceEngine.Session session;
        try{session=PhysicalDiceEngine.create(spec,seed);}catch(Exception e){callback.onError("Não foi possível iniciar a física 3D: "+safe(e.getMessage()));return;}

        final Dialog dialog=new Dialog(activity,android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final FrameLayout root=new FrameLayout(activity);root.setBackgroundColor(Color.TRANSPARENT);
        final Dice3DView diceView=new Dice3DView(activity);diceView.setSession(session);
        root.addView(diceView,new FrameLayout.LayoutParams(-1,-1));

        final HashMap<Integer,DiceAppearance> appearances=new HashMap<Integer,DiceAppearance>();
        final HashMap<Integer,String> identities=new HashMap<Integer,String>();
        for(PhysicalDiceEngine.DieBody body:session.bodies){
            JSONObject raw=Dice3DOverlay.loadTheme(activity,prefs,actorName,actorCharacterId,body.themeSides);
            DiceAppearance appearance=DiceAppearance.fromBraseiroTheme(raw);
            appearances.put(body.id,appearance);
            identities.put(body.id,raw.optString("themeIdentity","table_d"+body.themeSides));
            diceView.setAppearance(body.id,appearance);
        }

        LinearLayout top=new LinearLayout(activity);top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(activity,18),dp(activity,18),dp(activity,18),dp(activity,11));
        top.setBackgroundColor(Color.argb(105,0,0,0));
        TextView heading=text(activity,"ROLAGEM FÍSICA 3D",18,Color.rgb(232,192,90),true);
        TextView formula=text(activity,spec.formula+" • física real • face superior autoritativa",13,Color.LTGRAY,false);
        top.addView(heading);top.addView(formula);
        root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));

        final LinearLayout bottom=new LinearLayout(activity);bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(dp(activity,18),dp(activity,12),dp(activity,18),dp(activity,20));
        bottom.setBackgroundColor(Color.argb(145,0,0,0));
        final TextView result=text(activity,"Dados em movimento…",20,Color.WHITE,true);result.setGravity(Gravity.CENTER);
        final TextView detail=text(activity,"Impulso • rotação • gravidade • colisões",12,Color.LTGRAY,false);detail.setGravity(Gravity.CENTER);
        final Button cancel=button(activity,"CANCELAR ROLAGEM");
        bottom.addView(result);bottom.addView(detail);bottom.addView(cancel);
        root.addView(bottom,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));

        dialog.setContentView(root);dialog.setCancelable(false);
        final Handler main=new Handler(Looper.getMainLooper());
        final AtomicBoolean terminal=new AtomicBoolean(false);
        final AtomicBoolean settled=new AtomicBoolean(false);
        final long[] lastNs={System.nanoTime()},lastImpactMs=new long[session.bodies.size()+2];
        final double[] accumulator={0};

        final Runnable watchdog=new Runnable(){@Override public void run(){
            if(!terminal.compareAndSet(false,true))return;
            dismiss(dialog);callback.onError("A física 3D excedeu o limite de segurança. A rolagem foi descartada, sem registrar um resultado incompleto.");
        }};

        final Runnable publish=new Runnable(){@Override public void run(){
            if(!settled.get()||!terminal.compareAndSet(false,true))return;
            main.removeCallbacks(watchdog);
            try{
                PhysicalDiceEngine.RollResult raw=session.result();
                if(raw==null)throw new IllegalStateException("A física encerrou sem resultado.");
                ArrayList<Integer> faces=new ArrayList<Integer>(raw.physicalFaces);
                // Revalidação independente: o renderer não tem autoridade sobre os valores.
                List<Integer> reconstructed=spec.rollsFromPhysicalFaces(faces);
                List<Integer> canonical=spec.requirePhysicalAgreement(raw.dice,faces);
                if(!canonical.equals(reconstructed))throw new IllegalStateException("A reconstrução das faces físicas divergiu.");
                DiceRollSpec.Result resolved=spec.resolve(canonical);
                if(resolved.total!=raw.total)throw new IllegalStateException("O total físico divergiu da reconstrução canônica.");
                String resolvedDetail=spec.physicalDetail(canonical,faces,resolved);
                String identity=themeIdentity(session,identities);
                String integrity=RollIntegrity.signature(spec.formula,faces,spec.modifier,seed,resolved.total);
                int physicsMs=(int)Math.max(0,Math.min(60000,Math.round(raw.physicsSteps*PhysicalDiceEngine.FIXED_DT*1000.0)));
                dismiss(dialog);
                callback.onResult(new Dice3DOverlay.RollResult(new ArrayList<Integer>(canonical),faces,resolved.total,resolved.natural,
                        resolvedDetail,resolved.critical,spec.formula,spec.mode,physicsMs,seed,identity,integrity));
            }catch(Exception e){dismiss(dialog);callback.onError("Resultado físico inválido e descartado: "+safe(e.getMessage()));}
        }};

        final Runnable ticker=new Runnable(){@Override public void run(){
            if(terminal.get()||settled.get()||!dialog.isShowing())return;
            long now=System.nanoTime();double elapsed=Math.min(.055,(now-lastNs[0])/1e9);lastNs[0]=now;accumulator[0]+=elapsed;
            int sub=0;while(accumulator[0]>=PhysicalDiceEngine.FIXED_DT&&sub<8&&!session.isComplete()){
                session.step();accumulator[0]-=PhysicalDiceEngine.FIXED_DT;sub++;
            }
            long ms=System.currentTimeMillis();
            DiceSoundEngine sound=sound(activity);
            for(PhysicalDiceEngine.DieBody body:session.bodies){int idx=Math.min(lastImpactMs.length-1,body.id);if(body.impact>.42&&ms-lastImpactMs[idx]>55){sound.play(appearances.get(body.id),body.impact,body.sides);lastImpactMs[idx]=ms;}}
            diceView.invalidate();
            if(session.isComplete()){
                settled.set(true);main.removeCallbacks(watchdog);
                try{
                    PhysicalDiceEngine.RollResult raw=session.result();if(raw==null)throw new IllegalStateException("Resultado físico ausente.");
                    List<Integer> canonical=spec.requirePhysicalAgreement(raw.dice,raw.physicalFaces);
                    DiceRollSpec.Result resolved=spec.resolve(canonical);
                    result.setText(spec.physicalDetail(canonical,raw.physicalFaces,resolved));
                    String status=(raw.forcedSettle?"assentamento de segurança • ":"")+"física 120 Hz • gráfico "+diceView.visualQualityLabel();
                    detail.setText(status+(resolved.critical!=null&&resolved.critical.length()>0?"\n"+resolved.critical:""));
                    cancel.setText("DESCARTAR");
                    diceView.triggerOutcome(resolved.critical);
                    main.postDelayed(publish,RESULT_HOLD_MS);
                }catch(Exception e){if(terminal.compareAndSet(false,true)){dismiss(dialog);callback.onError("Resultado físico inválido e descartado: "+safe(e.getMessage()));}}
                return;
            }
            diceView.postOnAnimation(this);
        }};

        cancel.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){
            if(!terminal.compareAndSet(false,true))return;main.removeCallbacks(watchdog);main.removeCallbacks(publish);dismiss(dialog);callback.onCancel();
        }});
        dialog.setOnDismissListener(d->{main.removeCallbacks(watchdog);main.removeCallbacks(publish);});
        dialog.setOnShowListener(d->{Window w=dialog.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);lastNs[0]=System.nanoTime();main.postDelayed(watchdog,WATCHDOG_MS);diceView.postOnAnimation(ticker);});
        dialog.show();
        Window w=dialog.getWindow();if(w!=null){
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
            w.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp=w.getAttributes();lp.dimAmount=.18f;w.setAttributes(lp);
            w.setStatusBarColor(Color.TRANSPARENT);w.setNavigationBarColor(Color.BLACK);
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static synchronized DiceSoundEngine sound(Activity a){if(sharedSound==null)sharedSound=new DiceSoundEngine(a.getApplicationContext());return sharedSound;}
    private static String themeIdentity(PhysicalDiceEngine.Session session,HashMap<Integer,String> ids){StringBuilder b=new StringBuilder();String previous="";for(PhysicalDiceEngine.DieBody body:session.bodies){String s=ids.get(body.id);if(s==null)s="table_d"+body.themeSides;if(!s.equals(previous)){if(b.length()>0)b.append('+');b.append(s);previous=s;}}return b.toString();}
    private static TextView text(Activity a,String s,int sp,int color,boolean bold){TextView t=new TextView(a);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT_BOLD);t.setPadding(0,dp(a,4),0,dp(a,4));return t;}
    private static Button button(Activity a,String s){Button b=new Button(a);b.setText(s);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.rgb(54,48,42));b.setMinHeight(dp(a,50));return b;}
    private static int dp(Activity a,int value){return Math.round(value*a.getResources().getDisplayMetrics().density);}
    private static String safe(String s){return s==null?"":s;}
    private static void dismiss(Dialog d){try{if(d!=null&&d.isShowing())d.dismiss();}catch(Exception ignored){AppLog.ignored("NativeDice3DOverlay","Falha ao encerrar overlay físico.",ignored);}}
}

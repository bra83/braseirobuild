package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.Calendar;

/**
 * Orçamento local de chamadas do Gemini. Não tenta adivinhar a cota do Google:
 * impede chamadas duplicadas, limita reparos automáticos e reduz o tamanho dos
 * prompts antes que o aplicativo atinja uma cota diária configurável.
 */
public final class AiQuotaGovernor {
    private final SharedPreferences prefs;
    private static final int DEFAULT_CALL_LIMIT=80;
    private static final int DEFAULT_CHAR_LIMIT=900000;
    private static final int DEFAULT_TTS_LIMIT=150;

    public AiQuotaGovernor(Context context){
        prefs=context.getApplicationContext().getSharedPreferences("settings",Context.MODE_PRIVATE);
        resetIfNewDay();
    }

    public synchronized void resetIfNewDay(){
        int today=dayKey();int stored=prefs.getInt("ai_budget_day",0);
        if(stored!=today)prefs.edit().putInt("ai_budget_day",today).putInt("ai_calls_today",0).putInt("ai_input_chars_today",0).putInt("ai_output_chars_today",0).putInt("ai_tts_calls_today",0).apply();
    }

    public synchronized boolean canCall(){return canCall(0);}
    public synchronized boolean canCall(int projectedInputChars){
        resetIfNewDay();return callsToday()<callLimit()&&inputCharsToday()+Math.max(0,projectedInputChars)<=charLimit();
    }

    public synchronized boolean economyMode(){
        resetIfNewDay();return callsToday()>=Math.max(8,(int)(callLimit()*0.60))||inputCharsToday()>=(int)(charLimit()*0.60);
    }

    public synchronized boolean criticalMode(){
        resetIfNewDay();return callsToday()>=Math.max(12,(int)(callLimit()*0.85))||inputCharsToday()>=(int)(charLimit()*0.85);
    }

    public synchronized void recordCall(int inputChars,int outputChars){
        resetIfNewDay();prefs.edit().putInt("ai_calls_today",callsToday()+1).putInt("ai_input_chars_today",inputCharsToday()+Math.max(0,inputChars)).putInt("ai_output_chars_today",outputCharsToday()+Math.max(0,outputChars)).apply();
    }

    public synchronized JSONObject diagnostic(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();try{
            out.put("calls_today",callsToday());out.put("call_limit",callLimit());out.put("input_chars_today",inputCharsToday());out.put("input_char_limit",charLimit());out.put("output_chars_today",outputCharsToday());out.put("economy_mode",economyMode());out.put("critical_mode",criticalMode());out.put("remaining_calls",Math.max(0,callLimit()-callsToday()));out.put("tts_calls_today",ttsCallsToday());out.put("tts_call_limit",ttsCallLimit());
        }catch(Exception ignored){AppLog.ignored("AiQuotaGovernor","Falha tolerada; a execução continuou.",ignored);}return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public String systemMarker(){return criticalMode()?"BRAS_QUOTA_CRITICAL":(economyMode()?"BRAS_QUOTA_ECONOMY":"BRAS_QUOTA_NORMAL");}
    public String statusPtBr(){return "Gemini texto hoje: "+callsToday()+"/"+callLimit()+" chamadas • TTS: "+ttsCallsToday()+"/"+ttsCallLimit()+" • "+Math.max(0,(charLimit()-inputCharsToday())/1000)+" mil caracteres de entrada disponíveis"+(economyMode()?" • modo econômico":"");}
    public int callsToday(){return prefs.getInt("ai_calls_today",0);}
    public int inputCharsToday(){return prefs.getInt("ai_input_chars_today",0);}
    public int outputCharsToday(){return prefs.getInt("ai_output_chars_today",0);}
    public int callLimit(){return Math.max(10,prefs.getInt("ai_daily_call_limit",DEFAULT_CALL_LIMIT));}
    public int charLimit(){return Math.max(100000,prefs.getInt("ai_daily_char_limit",DEFAULT_CHAR_LIMIT));}


    public synchronized boolean canCallTts(int needed){resetIfNewDay();return ttsCallsToday()+Math.max(1,needed)<=ttsCallLimit();}
    public synchronized void recordTtsCall(){resetIfNewDay();prefs.edit().putInt("ai_tts_calls_today",ttsCallsToday()+1).apply();}
    public int ttsCallsToday(){return prefs.getInt("ai_tts_calls_today",0);}
    public int ttsCallLimit(){return Math.max(2,prefs.getInt("ai_daily_tts_limit",DEFAULT_TTS_LIMIT));}
    public int remainingTtsCalls(){return Math.max(0,ttsCallLimit()-ttsCallsToday());}

    private static int dayKey(){Calendar c=Calendar.getInstance();return c.get(Calendar.YEAR)*1000+c.get(Calendar.DAY_OF_YEAR);}
}

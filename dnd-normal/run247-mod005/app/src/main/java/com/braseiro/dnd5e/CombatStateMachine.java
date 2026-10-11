package com.braseiro.dnd5e;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/** Máquina de estados formal do encontro 5.0. */
public final class CombatStateMachine {
    public static final String IDLE="idle",PREPARING="preparing",INITIATIVE="initiative",ACTIVE_ROUND="active_round",RESOLVING="resolving_action",AWAITING_ROLL="awaiting_roll",ENCOUNTER_END="encounter_end";
    private static final HashMap<String,HashSet<String>> ALLOWED=new HashMap<String,HashSet<String>>();
    static{
        allow(IDLE,PREPARING);allow(PREPARING,INITIATIVE,IDLE);allow(INITIATIVE,ACTIVE_ROUND,IDLE);allow(ACTIVE_ROUND,RESOLVING,ENCOUNTER_END);allow(RESOLVING,AWAITING_ROLL,ACTIVE_ROUND,ENCOUNTER_END);allow(AWAITING_ROLL,RESOLVING,ACTIVE_ROUND,ENCOUNTER_END);allow(ENCOUNTER_END,IDLE);
    }
    private CombatStateMachine(){}
    private static void allow(String from,String... to){/*BR_JSON_WRAP*/try{ALLOWED.put(from,new HashSet<String>(Arrays.asList(to)));}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public static String normalize(String raw,boolean running){String x=raw==null?"":raw.trim().toLowerCase();if(ALLOWED.containsKey(x))return x;return running?ACTIVE_ROUND:IDLE;}
    public static boolean canTransition(String from,String to){String f=normalize(from,false),t=normalize(to,false);return f.equals(t)||(ALLOWED.containsKey(f)&&ALLOWED.get(f).contains(t));}
    public static JSONObject descriptor(){/*BR_JSON_WRAP*/try{JSONObject o=new JSONObject();try{o.put("states",new org.json.JSONArray(Arrays.asList(IDLE,PREPARING,INITIATIVE,ACTIVE_ROUND,RESOLVING,AWAITING_ROLL,ENCOUNTER_END))).put("fail_closed",true);}catch(Exception ignored){AppLog.ignored("CombatStateMachine","Falha tolerada registrada na consolidação 5.0.",ignored);}return o;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
}

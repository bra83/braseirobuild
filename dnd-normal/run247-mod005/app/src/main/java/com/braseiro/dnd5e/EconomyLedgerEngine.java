package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Ledger determinístico para reservas e lançamentos do mundo vivo. Não inventa preços. */
public final class EconomyLedgerEngine {
    private EconomyLedgerEngine(){}
    public static JSONObject empty(){JSONObject o=new JSONObject();try{o.put("balance",0d).put("reserved",0d).put("entries",new JSONArray()).put("version",1);}catch(Exception ignored){}return o;}
    public static JSONObject reserve(JSONObject ledger,double amount,String reason){JSONObject out=copy(ledger);try{double a=Math.max(0,amount),available=out.optDouble("balance",0)-out.optDouble("reserved",0);if(a>available){out.put("ok",false).put("error","insufficient_available_funds");return out;}out.put("reserved",out.optDouble("reserved",0)+a);append(out,"reserve",0,a,reason);out.put("ok",true);}catch(Exception ignored){}return out;}
    public static JSONObject commit(JSONObject ledger,double balanceDelta,double reservedRelease,String reason,JSONObject basis){JSONObject out=copy(ledger);try{JSONObject mutation=new JSONObject().put("gold_delta",balanceDelta);JSONObject gate=DowntimeEconomyRulesEngine.validateMutation(mutation,basis);if(!gate.optBoolean("authorized")){out.put("ok",false).put("error","mechanical_basis_required");return out;}double release=Math.max(0,Math.min(reservedRelease,out.optDouble("reserved",0)));double next=out.optDouble("balance",0)+balanceDelta;if(next<0){out.put("ok",false).put("error","negative_balance");return out;}out.put("balance",next).put("reserved",out.optDouble("reserved",0)-release);append(out,"commit",balanceDelta,-release,reason);out.put("ok",true);}catch(Exception ignored){}return out;}
    public static JSONObject cancelReserve(JSONObject ledger,double amount,String reason){JSONObject out=copy(ledger);try{double release=Math.max(0,Math.min(amount,out.optDouble("reserved",0)));out.put("reserved",out.optDouble("reserved",0)-release);append(out,"release",0,-release,reason);out.put("ok",true);}catch(Exception ignored){}return out;}
    private static void append(JSONObject o,String kind,double delta,double reserved,String reason)throws Exception{o.getJSONArray("entries").put(new JSONObject().put("kind",kind).put("delta",delta).put("reserved_delta",reserved).put("reason",reason==null?"":reason));}
    private static JSONObject copy(JSONObject src){try{return src==null?empty():new JSONObject(src.toString());}catch(Exception e){return empty();}}
}

package com.braseiro.dnd5e;
import org.json.JSONObject;
/** Compatibility release queue facade; RUN44 uses exact missing content identities. waiting_authorized_scope remains emitted by ExactMissingContentQueueEngine. */
public final class ContentClosureQueueEngine{
 private ContentClosureQueueEngine(){}
 public static JSONObject report(){JSONObject o=ExactMissingContentQueueEngine.report();try{o.put("policy","source_bound_only_no_rag_promotion").put("queue_mode","exact_missing_identities");}catch(Exception ignored){}return o;}
}

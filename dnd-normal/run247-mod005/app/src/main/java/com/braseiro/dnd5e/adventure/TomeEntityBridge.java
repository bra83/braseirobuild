package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Tome v1.0.0-compatible entity envelope and bounded resource operations.
 * Adapted from MIT Caryyon/tome src/Entity.ts, src/validation.ts and
 * converters/dnd5e-to-tome.ts. Original library is TS, this is a Java port.
 */
public final class TomeEntityBridge {
    private TomeEntityBridge() {}
    public static JSONObject create(String id, String system, String name, JSONObject attributes, JSONObject resources){
        if(id==null||!id.matches("[A-Za-z0-9_.-]{1,80}")||!UniversalRpgEngine.supported(system)||name==null||name.trim().isEmpty()||name.trim().length()>120)
            throw new IllegalArgumentException("ID, sistema ou nome inválidos.");
        JSONObject safeAttrs=attributes==null?new JSONObject():new JSONObject(attributes.toString());
        JSONArray keys=safeAttrs.names();
        for(int i=0;keys!=null&&i<keys.length();i++){
            String key=keys.optString(i);Object val=safeAttrs.opt(key);
            if(!key.matches("[A-Za-z0-9_.-]{1,80}")||!(val instanceof Number)||!Double.isFinite(((Number)val).doubleValue())||Math.abs(((Number)val).doubleValue())>10000)
                throw new IllegalArgumentException("Atributo inválido: "+key);
        }
        JSONObject safeResources=new JSONObject();
        JSONArray rkeys=resources==null?null:resources.names();
        for(int i=0;rkeys!=null&&i<rkeys.length();i++){
            String key=rkeys.optString(i);JSONObject r=resources.optJSONObject(key);
            if(!key.matches("[A-Za-z0-9_.-]{1,80}")||r==null)
                throw new IllegalArgumentException("Recurso inválido: "+key);
            Integer now=boundedInteger(r.opt("current")),max=boundedInteger(r.opt("maximum"));
            if(now==null||max==null||now.intValue()<0||max.intValue()<0
               ||now.intValue()>max.intValue()||max.intValue()>1000000)
                throw new IllegalArgumentException("Limites do recurso inválidos: "+key);
            safeResources.put(key,new JSONObject().put("current",now.intValue())
                .put("maximum",max.intValue()).put("minimum",0));
        }
        return new JSONObject().put("tome",new JSONObject().put("version","1.0.0").put("format","entity"))
           .put("meta",new JSONObject().put("id",id).put("type","character").put("system",system))
           .put("identity",new JSONObject().put("name",new JSONObject().put("primary",name.trim())))
           .put("properties",new JSONObject().put("static",safeAttrs))
           .put("resources",safeResources).put("inventory",new JSONObject().put("items",new JSONArray()))
           .put("braseiro_revision",0);
    }
    public static JSONObject updateResource(JSONObject entity,String key,int delta,int expectedRevision){
        if(entity==null||key==null||!key.matches("[A-Za-z0-9_.-]{1,80}"))return error("Recurso inválido.");
        JSONObject res=entity.optJSONObject("resources");JSONObject original=res==null?null:res.optJSONObject(key);
        if(original==null)return error("Recurso inexistente: "+key);
        int revision=entity.optInt("braseiro_revision",-1);
        if(expectedRevision!=revision)return error("Revisão obsoleta; recarregue a ficha.");
        long now=(long)original.optInt("current",Integer.MIN_VALUE)+delta;
        int max=original.optInt("maximum",Integer.MIN_VALUE);
        if(delta>1000000||delta < -1000000 ||now<0||now>max||max<0)return error("Operação excede os limites de "+key);
        JSONObject modified=new JSONObject(entity.toString());
        modified.optJSONObject("resources").optJSONObject(key).put("current",(int)now);
        modified.put("braseiro_revision",revision+1);
        return new JSONObject().put("status","resource_updated").put("entity",modified)
             .put("receipt",new JSONObject().put("key",key).put("delta",delta).put("before",original.optInt("current"))
             .put("after",(int)now).put("revision",revision+1));
    }
    private static Integer boundedInteger(Object value){
        if(!(value instanceof Number))return null;
        double d=((Number)value).doubleValue();
        return Double.isFinite(d)&&d==Math.rint(d)&&d>=0&&d<=1000000
            ?Integer.valueOf((int)d):null;
    }
    private static JSONObject error(String msg){return new JSONObject().put("status","blocked").put("error",msg);}
}

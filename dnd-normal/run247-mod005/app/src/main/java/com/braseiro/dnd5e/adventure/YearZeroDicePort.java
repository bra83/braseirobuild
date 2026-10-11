package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;

/** Year Zero pool semantics adapted to native Java; inspired by MIT Year Zero Universal Roller (Stefouch).
 * Never generates random dice. The submitted face lists are checked against the declared pool.
 * Forbidden Lands push: only dice showing neither 1 nor 6 can be rerolled on attribute/gear;
 * skill dice showing any non-6 face may be rerolled. Attribute/gear 1s after push cause damage.
 */
public final class YearZeroDicePort {
    private YearZeroDicePort(){}
    public static JSONObject resolve(JSONObject spec, JSONObject submission){
        try {
        JSONObject pool=spec==null?null:spec.optJSONObject("dice_pool");
        if(pool==null||submission==null)return error("Pool Year Zero ausente.");
        String[] names={"attribute","skill","gear"};
        int success=0;int damagedAttribute=0;int damagedGear=0;int negative=0;
        JSONObject push=submission.optJSONObject("push");
        JSONObject base=submission.optJSONObject("dice");
        if(base==null)return error("Informe as faces dos dados Year Zero.");
        for(String name:names){
            int count=pool.optInt(name,0);
            if(count<0||count>20)return error("Quantidade de dados inválida: "+name);
            JSONArray faces=base.optJSONArray(name);
            if(faces==null||faces.length()!=count)return error("Quantidade incorreta no pool: "+name);
            JSONArray rerolled=push==null?null:push.optJSONArray(name);
            int rerollExpected=0;
            for(int i=0;i<count;i++){
                Object v=faces.opt(i);if(!(v instanceof Number))return error("Face Year Zero não numérica.");
                int val=((Number)v).intValue();if(val<1||val>6||val!=((Number)v).doubleValue())return error("Face Year Zero fora de 1..6.");
                if(val==6)success++;
                boolean mayReroll=val!=6 && ("skill".equals(name)||val!=1);
                if(mayReroll&&push!=null)rerollExpected++;
                else if(push!=null&&val==1){if("attribute".equals(name))damagedAttribute++;else if("gear".equals(name))damagedGear++;}
            }
            if(push!=null){
                if(rerolled==null||rerolled.length()!=rerollExpected)return error("Rerolagens inválidas para "+name+": apenas dados elegíveis podem ser forçados.");
                for(int i=0;i<rerolled.length();i++){
                    Object v=rerolled.opt(i);if(!(v instanceof Number))return error("Rerolagem não numérica.");
                    int val=((Number)v).intValue();if(val<1||val>6||val!=((Number)v).doubleValue())return error("Rerolagem fora de 1..6.");
                    if(val==6)success++;
                    if(val==1){if("attribute".equals(name))damagedAttribute++;else if("gear".equals(name))damagedGear++;}
                }
            }
        }
        int penalty=pool.optInt("negative",0);
        if(penalty<0||penalty>20)return error("Dados negativos inválidos.");
        JSONArray neg=base.optJSONArray("negative");
        if((neg!=null&&neg.length()!=penalty)||(penalty>0&&neg==null))return error("Pool de dados negativos incompatível.");
        if(penalty>0){if(neg==null||neg.length()!=penalty)return error("Faces negativas ausentes.");
          for(int i=0;i<neg.length();i++){Object v=neg.opt(i);if(!(v instanceof Number))return error("Dado negativo inválido.");int face=((Number)v).intValue();if(face<1||face>6||face!=((Number)v).doubleValue())return error("Dado negativo fora do intervalo.");if(face==6)negative++;}
        }
        int net=success-negative; boolean ok=net>0;
        return new JSONObject().put("resolved",true).put("success",ok).put("successes",Math.max(0,net))
          .put("raw_successes",success).put("negative_successes",negative)
          .put("attribute_damage_from_push",damagedAttribute).put("gear_damage_from_push",damagedGear)
          .put("push_used",push!=null).put("effects_applied",false)
          .put("outcome",LoreweaverOutcomePort.create(ok?"success":"failure",ok?"Sucesso":"Fracasso",ok,false,false,Math.max(0,net),Integer.valueOf(net)));
        } catch (RuntimeException invalid) {
            throw invalid;
        } catch (Exception invalidJson) {
            throw new IllegalStateException("Falha ao estruturar resultado Year Zero",invalidJson);
        }
    }
    private static JSONObject error(String message){
        try { return new JSONObject().put("resolved",false).put("error",message); }
        catch(RuntimeException invalid){throw invalid;}
        catch(Exception invalidJson){throw new IllegalStateException("Falha ao estruturar erro Year Zero",invalidJson);}
    }
}
